param(
    [string]$SeedManifest = "datasets\vision_seed_manifest.csv",
    [string]$OutputPath = ".local-vision-datasets\manifests\tidypilot_vision_manifest.csv",
    [switch]$DownloadSmallFixtures
)

$ErrorActionPreference = "Stop"

function Resolve-RepoPath([string]$Path) {
    if ([System.IO.Path]::IsPathRooted($Path)) { return $Path }
    return Join-Path (Get-Location) $Path
}

$seedPath = Resolve-RepoPath $SeedManifest
$output = Resolve-RepoPath $OutputPath
$outputDir = Split-Path -Parent $output
New-Item -ItemType Directory -Force -Path $outputDir | Out-Null

if (-not (Test-Path -LiteralPath $seedPath)) {
    throw "Seed manifest not found: $seedPath"
}

$rows = Import-Csv -LiteralPath $seedPath

if ($DownloadSmallFixtures) {
    $photoDir = Resolve-RepoPath ".local-scan-fixtures\photos"
    New-Item -ItemType Directory -Force -Path $photoDir | Out-Null
    foreach ($row in $rows) {
        if ($row.source_id -ne "wikimedia_commons") { continue }
        if ([string]::IsNullOrWhiteSpace($row.local_path)) { continue }
        $localPath = Resolve-RepoPath $row.local_path
        if (Test-Path -LiteralPath $localPath) { continue }
        if ($row.source_url -notmatch "/wiki/File:") { continue }
        $fileName = ($row.source_url -replace '^.*File:', '') -replace ' ', '_'
        $downloadUrl = "https://commons.wikimedia.org/wiki/Special:FilePath/$fileName"
        try {
            Invoke-WebRequest -Uri $downloadUrl -OutFile $localPath -UseBasicParsing -TimeoutSec 45
            Write-Output "Downloaded $($row.image_id) -> $($row.local_path)"
        } catch {
            Write-Warning "Could not download $($row.image_id): $($_.Exception.Message)"
        }
    }
}

$normalized = foreach ($row in $rows) {
    $exists = $false
    if (-not [string]::IsNullOrWhiteSpace($row.local_path)) {
        $exists = Test-Path -LiteralPath (Resolve-RepoPath $row.local_path)
    }
    [PSCustomObject]@{
        image_id = $row.image_id
        source_id = $row.source_id
        source_url = $row.source_url
        local_path = $row.local_path
        local_exists = $exists
        room_type = $row.room_type
        mess_level = $row.mess_level
        visible_issues = $row.visible_issues
        confidence = $row.confidence
        license = $row.license
        attribution = $row.attribution
        split = $row.split
        notes = $row.notes
    }
}

$normalized | Export-Csv -LiteralPath $output -NoTypeInformation -Encoding UTF8

$total = @($normalized).Count
$available = @($normalized | Where-Object { $_.local_exists }).Count
$train = @($normalized | Where-Object { $_.split -eq "train" }).Count
$validation = @($normalized | Where-Object { $_.split -eq "validation" }).Count

Write-Output "Wrote $output"
Write-Output "Rows: $total | Local files present: $available | Train seeds: $train | Validation seeds: $validation"
