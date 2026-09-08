$ErrorActionPreference = 'Stop'
$assetDir = $PSScriptRoot
$edits = @(Get-Content -LiteralPath "$assetDir/targeted-edits-v3-prompts.json" -Raw -Encoding UTF8 | ConvertFrom-Json)
$review = @(Get-Content -LiteralPath "$assetDir/review.json" -Raw -Encoding UTF8 | ConvertFrom-Json)
$prompts = @(Get-Content -LiteralPath "$assetDir/prompts.json" -Raw -Encoding UTF8 | ConvertFrom-Json)
$gallery = Get-Content -LiteralPath "$assetDir/index.html" -Raw -Encoding UTF8
New-Item -ItemType Directory -Path "$assetDir/previous-v2" -Force | Out-Null
foreach ($edit in $edits) {
    if (-not (Test-Path -LiteralPath "$assetDir/final/$($edit.filename)")) { throw "Missing revised image: $($edit.filename)" }
    $row = $review | Where-Object { $_.character -eq 'hero' -and $_.id -eq $edit.id }
    $oldReason = $row.reason
    $row.final = $edit.filename
    $row.reason = $edit.reason
    $prompt = $prompts | Where-Object { $_.character -eq 'hero' -and $_.id -eq $edit.id }
    $prompt.filename = $edit.filename
    $prompt.prompt = $edit.prompt
    $prompt.reason = $edit.reason
    $prompt.scene = $edit.reason
    $prompt | Add-Member -NotePropertyName userRequestedCharacterException -NotePropertyValue ($edit.id -ne 'salvation_vow') -Force
    $pattern = '<article[^>]*>(?:(?!</article>).)*final/' + [regex]::Escape($edit.old) + '(?:(?!</article>).)*</article>'
    $gallery = [regex]::Replace($gallery,$pattern,{param($m) $m.Value.Replace($edit.old,$edit.filename).Replace($oldReason,$edit.reason)})
    if (Test-Path -LiteralPath "$assetDir/final/$($edit.old)") { Move-Item -LiteralPath "$assetDir/final/$($edit.old)" -Destination "$assetDir/previous-v2/$($edit.old)" -Force }
}
$gallery = $gallery.Replace('일반·비범·마법: 무기·사물·효과 중심 / 서사·전설·이계: 해당 캐릭터 포함','일반·비범·마법: 무기·사물·효과 중심 / 서사·전설·이계: 해당 캐릭터 포함<br>사용자 지정 예외: 전투의 함성·수호의 오라는 인물 중심. 구원의 맹세는 갑옷 십자가 제거.')
$review | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath "$assetDir/review.json" -Encoding UTF8
$prompts | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath "$assetDir/prompts.json" -Encoding UTF8
Set-Content -LiteralPath "$assetDir/index.html" -Value $gallery -Encoding UTF8
$readme = Get-Content -LiteralPath "$assetDir/README.md" -Raw -Encoding UTF8
$readme = $readme.Replace('- 일반·비범·마법: 캐릭터 없이 무기·사물·효과로 표현합니다.','- 일반·비범·마법: 캐릭터 없이 무기·사물·효과로 표현합니다. 사용자 지정 예외로 전투의 함성·수호의 오라는 인물 중심입니다.')
$readme += "`n최신 v3 수정: 전투의 함성은 직접 외치는 인물, 수호의 오라는 인물 중심 오라, 구원의 맹세는 갑옷 십자가 장식 제거. 내장 image_gen 사용, 상세 프롬프트는 targeted-edits-v3-prompts.json에 있습니다.`n"
Set-Content -LiteralPath "$assetDir/README.md" -Value $readme -Encoding UTF8
& "$assetDir/verify.ps1"
$missing = @([regex]::Matches($gallery,'(?:src|href)="((?:before|final)/[^"]+)"') | ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique | Where-Object { -not (Test-Path -LiteralPath (Join-Path $assetDir $_)) })
if ($missing.Count) { throw "Missing gallery images: $missing" }
"Final files: $(@(Get-ChildItem -LiteralPath "$assetDir/final" -File).Count)"
"V3 image references: $([regex]::Matches($gallery,'src="final/skill_hero_[^"]+_v3.png').Count)"
$zipItems = @('before','final','index.html','review.json','manifest.json','prompts.json','targeted-edits-v3-prompts.json','README.md','qa-final-low.jpg','qa-final-high.jpg') | ForEach-Object { Join-Path $assetDir $_ }
Compress-Archive -LiteralPath $zipItems -DestinationPath "$assetDir.zip" -Force
Get-Item -LiteralPath "$assetDir.zip" | Select-Object FullName,Length
