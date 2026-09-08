$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$assetRoot = $PSScriptRoot
$rows = @(Get-Content -LiteralPath (Join-Path $assetRoot 'review.json') -Raw -Encoding UTF8 | ConvertFrom-Json)
$checks = foreach ($row in $rows) {
    $finalPath = Join-Path $assetRoot ('final/' + $row.final)
    $beforePath = Join-Path $assetRoot ('before/' + $row.original)
    $img = [System.Drawing.Image]::FromFile($finalPath)
    $width = $img.Width
    $height = $img.Height
    $img.Dispose()
    if ($width -ne $height) { throw "Non-square: $finalPath" }
    $hash = (Get-FileHash -LiteralPath $finalPath -Algorithm SHA256).Hash
    if ($row.decision -eq 'retained' -and $hash -ne (Get-FileHash -LiteralPath $beforePath -Algorithm SHA256).Hash) { throw "Retained file differs: $finalPath" }
    [pscustomobject]@{character=$row.character; name=$row.name; grade=$row.grade; decision=$row.decision; file=('final/'+$row.final); width=$width; height=$height; sha256=$hash}
}
if ($checks.Count -ne 40) { throw 'Expected 40 files' }
if (@($checks | Group-Object sha256 | Where-Object Count -gt 1).Count -gt 0) { throw 'Duplicate final images' }
$checks | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $assetRoot 'manifest.json') -Encoding UTF8
foreach ($kind in @('low','high')) {
    $items = @($rows | Where-Object { $isHigh = $_.grade -in @('EPIC','LEGENDARY','OTHERWORLD'); if ($kind -eq 'high') {$isHigh} else {!$isHigh} })
    $canvas = [System.Drawing.Bitmap]::new(1200, ([int][Math]::Ceiling($items.Count / 5.0) * 268))
    $graphics = [System.Drawing.Graphics]::FromImage($canvas)
    $graphics.Clear([System.Drawing.Color]::FromArgb(24,24,28))
    $font = [System.Drawing.Font]::new('Malgun Gothic',11)
    for ($i=0; $i -lt $items.Count; $i++) {
        $item = $items[$i]
        $x = ($i % 5) * 240
        $y = [int][Math]::Floor($i / 5.0) * 268
        $icon = [System.Drawing.Image]::FromFile((Join-Path $assetRoot ('final/'+$item.final)))
        $graphics.DrawImage($icon, $x+4, $y+4, 232, 232)
        $icon.Dispose()
        $graphics.DrawString(($item.name+' / '+$item.grade), $font, [System.Drawing.Brushes]::White, [single]($x+4), [single]($y+238))
    }
    $canvas.Save((Join-Path $assetRoot ('qa-final-'+$kind+'.jpg')), [System.Drawing.Imaging.ImageFormat]::Jpeg)
    $font.Dispose()
    $graphics.Dispose()
    $canvas.Dispose()
}
$checks | Group-Object decision | Select-Object Name,Count
$checks | Group-Object grade | Select-Object Name,Count
