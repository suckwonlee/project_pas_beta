param([string]$Serial = 'emulator-5554')
$ErrorActionPreference = 'Stop'
$adb = 'C:\Users\USER\AppData\Local\Android\Sdk\platform-tools\adb.exe'
function Find-Node([string]$Text, [bool]$Prefix = $false) {
    for ($attempt = 0; $attempt -lt 5; $attempt++) {
        $null = & $adb -s $Serial shell uiautomator dump /sdcard/pas-network-window.xml
        [xml]$tree = (& $adb -s $Serial shell cat /sdcard/pas-network-window.xml)
        $match = $tree.SelectNodes('//node') | Where-Object {
            if ($Prefix) { $_.text.StartsWith($Text) } else { $_.text -eq $Text }
        } | Select-Object -First 1
        if ($match) { return $match }
        Start-Sleep -Milliseconds 300
    }
    throw "Expected UI not found: $Text"
}
function Tap-Node([string]$Text, [bool]$Long = $false, [bool]$Prefix = $false) {
    $node = Find-Node $Text $Prefix
    $coords = [regex]::Matches($node.bounds, '\d+') | ForEach-Object { [int]$_.Value }
    $x = [int](($coords[0] + $coords[2]) / 2)
    $y = [int](($coords[1] + $coords[3]) / 2)
    if ($Long) { $null = & $adb -s $Serial shell input swipe $x $y $x $y 1200 }
    else { $null = & $adb -s $Serial shell input tap $x $y }
}
$null = & $adb -s $Serial reverse tcp:8090 tcp:8090
$null = & $adb -s $Serial shell am force-stop com.pas.game
$null = & $adb -s $Serial shell am start -W -n com.pas.game/.ui.activity.MainActivity
Tap-Node 'Project-PAS' $true
Tap-Node '온라인 서버 연결 테스트'
Tap-Node '서버 주소 (' $false $true
$null = & $adb -s $Serial shell input keycombination 113 29
$null = & $adb -s $Serial shell input text http://127.0.0.1:8090
$null = & $adb -s $Serial shell input keyevent 4
Tap-Node '모드: 2인 협동'
Tap-Node '1인 2캐릭터'
Tap-Node '방 만들기'
Tap-Node '구성 저장 후 준비 완료'
$state = Find-Node '라운드 1' $true
Write-Output $state.text
Tap-Node '턴 종료'
$state = Find-Node '라운드 1 · 용사후보 P2' $true
Write-Output $state.text
Tap-Node '턴 종료'
$state = Find-Node '라운드 2 · 용사후보 P1' $true
Write-Output $state.text
$null = & $adb -s $Serial shell screencap -p /sdcard/pas-online-final.png
& $adb -s $Serial pull /sdcard/pas-online-final.png (Join-Path $PSScriptRoot '..\build\pas-online-final.png')
& $adb -s $Serial logcat -d -s AndroidRuntime:E
