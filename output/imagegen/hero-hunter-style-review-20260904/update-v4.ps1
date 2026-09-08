$ErrorActionPreference = 'Stop'
$assetDir = $PSScriptRoot
$edits = @(Get-Content -LiteralPath "$assetDir/targeted-edits-v4-prompts.json" -Raw -Encoding UTF8 | ConvertFrom-Json)
$review = @(Get-Content -LiteralPath "$assetDir/review.json" -Raw -Encoding UTF8 | ConvertFrom-Json)
$prompts = [System.Collections.ArrayList]@(@(Get-Content -LiteralPath "$assetDir/prompts.json" -Raw -Encoding UTF8 | ConvertFrom-Json))
$gallery = Get-Content -LiteralPath "$assetDir/index.html" -Raw -Encoding UTF8
New-Item -ItemType Directory -Path "$assetDir/previous-v3" -Force | Out-Null
foreach ($edit in $edits) {
    if (-not (Test-Path -LiteralPath "$assetDir/final/$($edit.filename)")) { throw "Missing image: $($edit.filename)" }
    $row = $review | Where-Object { $_.id -eq $edit.id }
    $oldReason = $row.reason
    $wasRetained = $row.decision -eq 'retained'
    $row.decision = 'revised'
    $row.final = $edit.filename
    $row.reason = $edit.reason
    $prompt = $prompts | Where-Object { $_.id -eq $edit.id }
    if ($null -eq $prompt) {
        [void]$prompts.Add([pscustomobject]@{character=$row.character; id=$edit.id; name=$edit.name; grade=$row.grade; original=$row.original; reason=$edit.reason; filename=$edit.filename; prompt=$edit.prompt; references=$edit.refs})
    } else {
        $prompt.filename = $edit.filename
        $prompt.prompt = $edit.prompt
        $prompt.reason = $edit.reason
    }
    $pattern = '<article[^>]*>(?:(?!</article>).)*final/' + [regex]::Escape($edit.old) + '(?:(?!</article>).)*</article>'
    $gallery = [regex]::Replace($gallery,$pattern,{param($m)
        $article = $m.Value.Replace($edit.old,$edit.filename).Replace($oldReason,$edit.reason)
        if ($wasRetained) { $article = $article.Replace('card retained','card revised').Replace(' · 유지',' · 수정').Replace('<figcaption>유지</figcaption>','<figcaption>수정본</figcaption>') }
        $article
    })
    if (Test-Path -LiteralPath "$assetDir/final/$($edit.old)") { Move-Item -LiteralPath "$assetDir/final/$($edit.old)" -Destination "$assetDir/previous-v3/$($edit.old)" -Force }
}
$gallery = $gallery.Replace('32종 수정 · 8종 유지','33종 수정 · 7종 유지')
$review | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath "$assetDir/review.json" -Encoding UTF8
$prompts | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath "$assetDir/prompts.json" -Encoding UTF8
Set-Content -LiteralPath "$assetDir/index.html" -Value $gallery -Encoding UTF8
$readme = Get-Content -LiteralPath "$assetDir/README.md" -Raw -Encoding UTF8
$readme = $readme.Replace('수정 32종, 유지 8종','수정 33종, 유지 7종').Replace('수정 32종의','수정 33종의').Replace('용사후보 2종(용사 검법, 정의의 심판)','용사후보 1종(정의의 심판)')
$readme += "`n최신 v4 수정: 용사 검법은 용자검법 1초식의 플루크형 원근 구도, 구원의 맹세는 눈을 감은 맹세 자세, 극독의 저주는 화살을 겨누지 않는 손 시전 구도. 내장 image_gen 사용, 상세 프롬프트는 targeted-edits-v4-prompts.json에 있습니다.`n"
Set-Content -LiteralPath "$assetDir/README.md" -Value $readme -Encoding UTF8
& "$assetDir/verify.ps1"
$missing = @([regex]::Matches($gallery,'(?:src|href)="((?:before|final)/[^"]+)"') | ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique | Where-Object { -not (Test-Path -LiteralPath (Join-Path $assetDir $_)) })
if ($missing.Count) { throw "Missing gallery image: $missing" }
"Final images: $(@(Get-ChildItem -LiteralPath "$assetDir/final" -File).Count)"
"Revised cards: $([regex]::Matches($gallery,'card revised').Count)"
"Retained cards: $([regex]::Matches($gallery,'card retained').Count)"
$zipItems = @('before','final','index.html','review.json','manifest.json','prompts.json','targeted-edits-v3-prompts.json','targeted-edits-v4-prompts.json','README.md','qa-final-low.jpg','qa-final-high.jpg') | ForEach-Object { Join-Path $assetDir $_ }
Compress-Archive -LiteralPath $zipItems -DestinationPath "$assetDir.zip" -Force
Get-Item -LiteralPath "$assetDir.zip" | Select-Object FullName,Length
