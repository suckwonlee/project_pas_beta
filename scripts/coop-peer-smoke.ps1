param(
    [Parameter(Mandatory = $true)][string]$RoomCode,
    [string]$ServerUrl = 'http://127.0.0.1:8090'
)
$ErrorActionPreference = 'Stop'
if ($ServerUrl -notmatch '^http://(127\.0\.0\.1|localhost):[0-9]+$') {
    throw 'This smoke-test peer is restricted to a local server.'
}
if ($RoomCode -notmatch '^[A-Z2-9]{6}$') { throw 'A six-character room code is required.' }
# Credentials live only in memory. Never output the connection response or headers.
$peer = Invoke-RestMethod "$ServerUrl/api/v1/rooms/$RoomCode/join" -Method Post -ContentType 'application/json' -Body '{"nickname":"Smoke peer"}'
$headers = @{ 'X-PAS-TOKEN' = $peer.accessToken; 'X-PAS-CLIENT' = $peer.clientId }
$body = @{ characters = @(@{ playerSlot = 2; characterId = 'HUNTER' }) } | ConvertTo-Json -Depth 5
$null = Invoke-RestMethod "$ServerUrl/api/v1/rooms/$RoomCode/players/$($peer.clientId)/loadout" -Method Put -ContentType 'application/json' -Headers $headers -Body $body
$null = Invoke-RestMethod "$ServerUrl/api/v1/rooms/$RoomCode/players/$($peer.clientId)/ready" -Method Post -Headers $headers
$socket = [Net.WebSockets.ClientWebSocket]::new()
$socket.Options.SetRequestHeader('X-PAS-TOKEN', $peer.accessToken)
$socket.Options.SetRequestHeader('X-PAS-CLIENT', $peer.clientId)
$timeout = [Threading.CancellationTokenSource]::new([TimeSpan]::FromSeconds(60))
try {
    $url = $ServerUrl.Replace('http://', 'ws://') + "/ws/battle?roomCode=$RoomCode"
    $null = $socket.ConnectAsync([Uri]$url, $timeout.Token).GetAwaiter().GetResult()
    Write-Output 'Peer connected. Ready the Android host, then end its turn within 60 seconds.'
    $sent = $false
    while ($socket.State -eq [Net.WebSockets.WebSocketState]::Open) {
        $message = [IO.MemoryStream]::new()
        try {
            do {
                $bytes = [byte[]]::new(65536)
                $part = $socket.ReceiveAsync([ArraySegment[byte]]::new($bytes), $timeout.Token).GetAwaiter().GetResult()
                if ($part.MessageType -eq [Net.WebSockets.WebSocketMessageType]::Close) { throw 'Server closed the test connection.' }
                $message.Write($bytes, 0, $part.Count)
            } until ($part.EndOfMessage)
            $receipt = [Text.Encoding]::UTF8.GetString($message.ToArray()) | ConvertFrom-Json
        } finally { $message.Dispose() }
        if (-not $receipt.accepted) { throw 'Server rejected the smoke-test command.' }
        $snapshot = $receipt.snapshotJson | ConvertFrom-Json
        Write-Output "Received $($receipt.requestId), revision $($receipt.revision), active $($snapshot.turn.activeUnitId)"
        if ($sent) { Write-Output 'Guest turn accepted; host should receive the same revision.'; break }
        if ($snapshot.turn.activeUnitId -like 'P2_*') {
            $command = @{ protocolVersion = 1; matchId = $RoomCode; clientId = $peer.clientId; requestId = [Guid]::NewGuid().ToString(); expectedRevision = $receipt.revision; commandType = 'END_TURN'; actorUnitId = $snapshot.turn.activeUnitId } | ConvertTo-Json -Compress
            $data = [Text.Encoding]::UTF8.GetBytes($command)
            $null = $socket.SendAsync([ArraySegment[byte]]::new($data), [Net.WebSockets.WebSocketMessageType]::Text, $true, $timeout.Token).GetAwaiter().GetResult()
            $sent = $true
        }
    }
} finally { $socket.Dispose(); $timeout.Dispose() }
