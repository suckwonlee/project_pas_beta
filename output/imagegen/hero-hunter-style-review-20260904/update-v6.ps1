$ErrorActionPreference = 'Stop'
$assetDir = $PSScriptRoot
$edit = Get-Content -LiteralPath "$assetDir/targeted-edits-v6-prompts.json" -Raw -Encoding UTF8 | ConvertFrom-Json
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
New-Item -ItemType Directory -Path "$assetDir/previous-v5" -Force | Out-Null
Move-Item -LiteralPath "$assetDir/final/$($edit.old)" -Destination "$assetDir/previous-v5/$($edit.old)" -Force
$review | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath "$assetDir/review.json" -Encoding UTF8
$prompts | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath "$assetDir/prompts.json" -Encoding UTF8
Set-Content -LiteralPath "$assetDir/index.html" -Value $gallery -Encoding UTF8
$readme = Get-Content -LiteralPath "$assetDir/README.md" -Raw -Encoding UTF8
$readme += "`n최신 v6 수정: v5의 과장된 검 구도를 유지하고 모든 형태를 굵은 저해상도 도트 클러스터로 재작업. 상세 프롬프트는 targeted-edits-v6-prompts.json에 있습니다.`n"
Set-Content -LiteralPath "$assetDir/README.md" -Value $readme -Encoding UTF8
& "$assetDir/verify.ps1"
$missing = @([regex]::Matches($gallery,'(?:src|href)="((?:before|final)/[^"]+)"') | ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique | Where-Object { -not (Test-Path -LiteralPath (Join-Path $assetDir $_)) })
if ($missing.Count) { throw "Missing gallery image: $missing" }
"Final images: $(@(Get-ChildItem -LiteralPath "$assetDir/final" -File).Count)"
"V6 references: $([regex]::Matches($gallery,'src="final/[^\"]+_v6.png').Count)"
$zipItems = @('before','final','index.html','review.json','manifest.json','prompts.json','targeted-edits-v3-prompts.json','targeted-edits-v4-prompts.json','targeted-edits-v5-prompts.json','targeted-edits-v6-prompts.json','README.md','qa-final-low.jpg','qa-final-high.jpg') | ForEach-Object { Join-Path $assetDir $_ }
Compress-Archive -LiteralPath $zipItems -DestinationPath "$assetDir.zip" -Force
Get-Item -LiteralPath "$assetDir.zip" | Select-Object FullName,Length
