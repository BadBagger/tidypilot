param(
    [string]$FeaturePath = ".local-vision-datasets\features\tidypilot_combined_features.csv"
)

$ErrorActionPreference = "Stop"

function Resolve-RepoPath([string]$Path) {
    if ([System.IO.Path]::IsPathRooted($Path)) { return $Path }
    return Join-Path (Get-Location) $Path
}

$path = Resolve-RepoPath $FeaturePath
if (-not (Test-Path -LiteralPath $path)) {
    throw "Feature table not found: $path"
}

$rows = Import-Csv -LiteralPath $path

Write-Output "Feature table: $path"
Write-Output "Total rows: $(@($rows).Count)"
Write-Output ""
Write-Output "By label:"
$rows | Group-Object mess_level | Sort-Object Name | ForEach-Object { Write-Output "  $($_.Name): $($_.Count)" }
Write-Output ""
Write-Output "By photo quality:"
$rows | Group-Object photo_quality | Sort-Object Name | ForEach-Object { Write-Output "  $($_.Name): $($_.Count)" }
Write-Output ""
Write-Output "By review priority:"
$rows | Group-Object review_priority | Sort-Object Name | ForEach-Object { Write-Output "  $($_.Name): $($_.Count)" }
Write-Output ""
Write-Output "Average pixel mess score by label:"
$rows |
    Group-Object mess_level |
    Sort-Object Name |
    ForEach-Object {
        $scores = @($_.Group | ForEach-Object { [int]$_.pixel_mess_score })
        $avg = if ($scores.Count -gt 0) { [math]::Round(($scores | Measure-Object -Average).Average, 1) } else { 0 }
        Write-Output "  $($_.Name): $avg"
    }
Write-Output ""
Write-Output "Highest review-priority samples:"
$rows |
    Where-Object { $_.review_priority -ne "ok" } |
    Sort-Object @{ Expression = { [int]$_.pixel_mess_score }; Descending = $true } |
    Select-Object -First 12 image_id,mess_level,pixel_mess_score,photo_quality,local_path |
    Format-Table -AutoSize
