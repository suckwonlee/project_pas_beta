$ErrorActionPreference = 'Stop'
$projectDir = 'C:/Users/USER/AndroidStudioProjects/project_pas'
$hh = "$projectDir/output/imagegen/hero-hunter-style-review-20260904"
$cw = "$projectDir/output/imagegen/cleric-wizard-20260904"
$items = @()
foreach ($item in (Get-Content "$hh/review.json" -Raw -Encoding UTF8 | ConvertFrom-Json)) {
    $items += [pscustomobject]@{id=$item.id; source="$hh/final/$($item.final)"; filename=$item.final}
}
foreach ($item in (Get-Content "$cw/manifest.json" -Raw -Encoding UTF8 | ConvertFrom-Json)) {
    $items += [pscustomobject]@{id=$item.id; source="$cw/$($item.filename)"; filename=$item.filename}
}
if ($items.Count -ne 74 -or @($items | Group-Object id | Where-Object Count -gt 1).Count) { throw 'Invalid icon mapping' }
$lines = @('package com.pas.game.ui;', '', 'import com.pas.game.R;', '', '/** Approved skill artwork shared by selection and battle screens. */', 'public final class ApprovedSkillIcons {', '    private ApprovedSkillIcons() {}', '', '    public static int find(String id) {', '        if (id == null) return 0;', '        switch (id) {')
foreach ($item in $items) {
    $target = "$projectDir/app/src/main/res/drawable-nodpi/$($item.filename)"
    if (-not (Test-Path -LiteralPath $target) -or (Get-FileHash $item.source).Hash -ne (Get-FileHash $target).Hash) {
        Copy-Item -LiteralPath $item.source -Destination $target -Force
    }
    if ((Get-FileHash $item.source).Hash -ne (Get-FileHash $target).Hash) { throw "Copy mismatch: $($item.id)" }
    $resource = [IO.Path]::GetFileNameWithoutExtension($item.filename)
    $lines += '            case "' + $item.id + '": return R.drawable.' + $resource + ';'
}
$lines += @('            default: return 0;', '        }', '    }', '}')
[IO.File]::WriteAllLines("$projectDir/app/src/main/java/com/pas/game/ui/ApprovedSkillIcons.java", $lines, [Text.UTF8Encoding]::new($false))
$activityPath = "$projectDir/app/src/main/java/com/pas/game/ui/activity/MainActivity.java"
$activity = [IO.File]::ReadAllText($activityPath)
$needle = 'private int skillIcon(SkillData skill){String id=skill.getId();'
if (-not $activity.Contains($needle)) { throw 'skillIcon entry not found' }
$activity = $activity.Replace($needle, 'private int skillIcon(SkillData skill){String id=skill.getId();int approvedIcon=com.pas.game.ui.ApprovedSkillIcons.find(id);if(approvedIcon!=0)return approvedIcon;')
$needle = 'input.setHint("쿠폰 코드");'
if (-not $activity.Contains($needle)) { throw 'Coupon input not found' }
$activity = $activity.Replace($needle, $needle + "`n        // TODO: 다음 작업 때 삭제할 것 - 실제 핸드폰 아이콘 확인용 디버그 코드 자동 입력.`n        if(com.pas.game.BuildConfig.DEBUG){input.setText(DebugOptions.DEVELOPER_COUPON);}`n        ")
[IO.File]::WriteAllText($activityPath, $activity, [Text.UTF8Encoding]::new($false))
$items | ConvertTo-Json | Set-Content "$projectDir/output/imagegen/connected-icons.json" -Encoding UTF8
"Connected 74 icons; all source/resource hashes match."
