param(
    [string]$ManifestPath = ".local-vision-datasets\manifests\tidypilot_vision_manifest.csv",
    [int]$MaxLocalFiles = 25
)

$ErrorActionPreference = "Stop"

function Resolve-RepoPath([string]$Path) {
    if ([System.IO.Path]::IsPathRooted($Path)) { return $Path }
    return Join-Path (Get-Location) $Path
}

$path = Resolve-RepoPath $ManifestPath
if (-not (Test-Path -LiteralPath $path)) {
    throw "Manifest not found: $path"
}

$rows = Import-Csv -LiteralPath $path

Write-Output "Manifest: $path"
Write-Output "Total rows: $(@($rows).Count)"
Write-Output ""
Write-Output "By split:"
$rows | Group-Object split | Sort-Object Name | ForEach-Object { Write-Output "  $($_.Name): $($_.Count)" }
Write-Output ""
Write-Output "By room type:"
$rows | Group-Object room_type | Sort-Object Name | ForEach-Object { Write-Output "  $($_.Name): $($_.Count)" }
Write-Output ""
Write-Output "By mess level:"
$rows | Group-Object mess_level | Sort-Object Name | ForEach-Object { Write-Output "  $($_.Name): $($_.Count)" }
Write-Output ""
Write-Output "Local files present:"
$localFiles = @($rows | Where-Object { $_.local_exists -eq "True" })
$localFiles | Select-Object -First $MaxLocalFiles | ForEach-Object { Write-Output "  $($_.image_id) -> $($_.local_path)" }
if ($localFiles.Count -gt $MaxLocalFiles) {
    Write-Output "  ... $($localFiles.Count - $MaxLocalFiles) more local files"
}
