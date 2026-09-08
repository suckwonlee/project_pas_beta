$ErrorActionPreference = 'Stop'
$assetDir = $PSScriptRoot
$edit = Get-Content -LiteralPath "$assetDir/targeted-edits-v7-prompts.json" -Raw -Encoding UTF8 | ConvertFrom-Json
$review = @(Get-Content -LiteralPath "$assetDir/review.json" -Raw -Encoding UTF8 | ConvertFrom-Json)
$prompts = @(Get-Content -LiteralPath "$assetDir/prompts.json" -Raw -Encoding UTF8 | ConvertFrom-Json)
$gallery = Get-Content -LiteralPath "$assetDir/index.html" -Raw -Encoding UTF8
$row = $review | Where-Object id -eq $edit.id
$oldReason = $row.reason
$row.final = $edit.filename
$row.reason = $edit.reason
$prompt = $prompts | Where-Object id -eq $edit.id
$prompt.filename = $edit.filename
$prompt.prompt = $edit.prompt
$prompt.reason = $edit.reason
$pattern = '<article[^>]*>(?:(?!</article>).)*final/' + [regex]::Escape($edit.old) + '(?:(?!</article>).)*</article>'
$gallery = [regex]::Replace($gallery,$pattern,{param($m) $m.Value.Replace($edit.old,$edit.filename).Replace($oldReason,$edit.reason)})
New-Item -ItemType Directory -Path "$assetDir/previous-v6" -Force | Out-Null
Move-Item -LiteralPath "$assetDir/final/$($edit.old)" -Destination "$assetDir/previous-v6/$($edit.old)" -Force
$review | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath "$assetDir/review.json" -Encoding UTF8
$prompts | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath "$assetDir/prompts.json" -Encoding UTF8
Set-Content -LiteralPath "$assetDir/index.html" -Value $gallery -Encoding UTF8
& "$assetDir/verify.ps1"
$missing = @([regex]::Matches($gallery,'(?:src|href)="((?:before|final)/[^"]+)"') | ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique | Where-Object { -not (Test-Path -LiteralPath (Join-Path $assetDir $_)) })
if ($missing.Count) { throw "Missing gallery image: $missing" }
$zipItems = @('before','final','index.html','review.json','manifest.json','prompts.json','targeted-edits-v3-prompts.json','targeted-edits-v4-prompts.json','targeted-edits-v5-prompts.json','targeted-edits-v6-prompts.json','targeted-edits-v7-prompts.json','README.md','qa-final-low.jpg','qa-final-high.jpg') | ForEach-Object { Join-Path $assetDir $_ }
Compress-Archive -LiteralPath $zipItems -DestinationPath "$assetDir.zip" -Force
"Final images: $(@(Get-ChildItem -LiteralPath "$assetDir/final" -File).Count)"
"V7 references: $([regex]::Matches($gallery,'src="final/[^\"]+_v7.png').Count)"
