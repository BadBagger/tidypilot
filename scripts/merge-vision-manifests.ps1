param(
    [string[]]$ManifestPaths = @(
        "datasets\vision_seed_manifest.csv",
        ".local-vision-datasets\manifests\tidypilot_vision_manifest.csv",
        ".local-vision-datasets\manifests\kaggle_messy_clean_manifest.csv",
        ".local-vision-datasets\open_images\manifests\open_images_validation_sample_manifest.csv"
    ),
    [string]$OutputPath = ".local-vision-datasets\manifests\tidypilot_combined_manifest.csv"
)

$ErrorActionPreference = "Stop"

function Resolve-RepoPath([string]$Path) {
    if ([System.IO.Path]::IsPathRooted($Path)) { return $Path }
    return Join-Path (Get-Location) $Path
}

function Normalize-MessLevel([string]$MessLevel) {
    if ($null -eq $MessLevel) {
        $level = ""
    } else {
        $level = $MessLevel.Trim().ToLowerInvariant()
    }

    switch ($level) {
        "looks_mostly_clear" { return "clear" }
        "clear" { return "clear" }
        "quick_reset" { return "light_reset" }
        "light_reset" { return "light_reset" }
        "needs_attention" { return "moderate_mess" }
        "moderate_mess" { return "moderate_mess" }
        "bigger_reset" { return "heavy_reset" }
        "heavy_reset" { return "heavy_reset" }
        default { return "needs_review" }
    }
}

function Get-ColumnValue($Row, [string[]]$Names, [string]$Default = "") {
    foreach ($name in $Names) {
        if ($Row.PSObject.Properties.Name -contains $name) {
            $value = $Row.$name
            if ($null -ne $value -and "$value".Length -gt 0) {
                return "$value"
            }
        }
    }
    return $Default
}

$output = Resolve-RepoPath $OutputPath
New-Item -ItemType Directory -Force -Path (Split-Path -Parent $output) | Out-Null

$seen = @{}
$merged = New-Object System.Collections.Generic.List[object]

foreach ($manifestPath in $ManifestPaths) {
    $path = Resolve-RepoPath $manifestPath
    if (-not (Test-Path -LiteralPath $path)) {
        Write-Warning "Skipping missing manifest: $path"
        continue
    }

    $rows = Import-Csv -LiteralPath $path
    foreach ($row in $rows) {
        $imageId = Get-ColumnValue $row @("image_id", "ImageID", "imageId")
        $sourceId = Get-ColumnValue $row @("source_id", "source", "Source") "unknown"
        $localPath = Get-ColumnValue $row @("local_path", "image_path", "path", "OriginalURL")
        if (-not $imageId) {
            $imageId = "$sourceId`_$([System.Guid]::NewGuid().ToString("N").Substring(0, 12))"
        }

        $dedupeKey = if ($localPath) { "$sourceId|$localPath" } else { "$sourceId|$imageId" }
        if ($seen.ContainsKey($dedupeKey)) {
            continue
        }
        $seen[$dedupeKey] = $true

        $localExistsRaw = Get-ColumnValue $row @("local_exists")
        $localExists = if ($localExistsRaw) {
            $localExistsRaw
        } elseif ($localPath -and (Test-Path -LiteralPath (Resolve-RepoPath $localPath))) {
            "True"
        } else {
            "False"
        }

        $merged.Add([PSCustomObject]@{
            image_id = $imageId
            source_id = $sourceId
            source_url = Get-ColumnValue $row @("source_url", "OriginalURL", "sourceUrl")
            local_path = $localPath
            local_exists = $localExists
            room_type = Get-ColumnValue $row @("room_type", "room", "roomType") "mixed"
            mess_level = Normalize-MessLevel (Get-ColumnValue $row @("mess_level", "label", "LabelName") "needs_review")
            visible_issues = Get-ColumnValue $row @("visible_issues", "issues", "tags")
            confidence = Get-ColumnValue $row @("confidence") "low"
            license = Get-ColumnValue $row @("license", "License") "verify_source"
            attribution = Get-ColumnValue $row @("attribution", "source_id") $sourceId
            split = Get-ColumnValue $row @("split", "Split") "train"
            notes = Get-ColumnValue $row @("notes", "Notes")
        })
    }
}

$merged |
    Sort-Object source_id, split, mess_level, image_id |
    Export-Csv -LiteralPath $output -NoTypeInformation -Encoding UTF8

Write-Output "Wrote $output"
Write-Output "Rows: $($merged.Count)"
Write-Output ""
Write-Output "By source:"
$merged | Group-Object source_id | Sort-Object Name | ForEach-Object { Write-Output "  $($_.Name): $($_.Count)" }
Write-Output ""
Write-Output "By mess level:"
$merged | Group-Object mess_level | Sort-Object Name | ForEach-Object { Write-Output "  $($_.Name): $($_.Count)" }
