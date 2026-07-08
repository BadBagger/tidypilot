param(
    [string]$Dataset = "cdawn1/messy-vs-clean-room",
    [string]$OutputDir = ".local-vision-datasets\raw\kaggle_messy_clean",
    [string]$ManifestPath = ".local-vision-datasets\manifests\kaggle_messy_clean_manifest.csv"
)

$ErrorActionPreference = "Stop"

function Resolve-RepoPath([string]$Path) {
    if ([System.IO.Path]::IsPathRooted($Path)) { return $Path }
    return Join-Path (Get-Location) $Path
}

$kaggle = Get-Command kaggle -ErrorAction SilentlyContinue
if (-not $kaggle) {
    throw "Kaggle CLI is not installed. Install it locally, then rerun this script."
}

$legacyCredentialPath = Join-Path $env:USERPROFILE ".kaggle\kaggle.json"
$tokenCredentialPath = Join-Path $env:USERPROFILE ".kaggle\access_token"
if (-not (Test-Path -LiteralPath $legacyCredentialPath) -and -not (Test-Path -LiteralPath $tokenCredentialPath)) {
    throw "Kaggle credentials not found. Add ~/.kaggle/access_token or ~/.kaggle/kaggle.json locally; do not commit it."
}

$output = Resolve-RepoPath $OutputDir
$manifest = Resolve-RepoPath $ManifestPath
New-Item -ItemType Directory -Force -Path $output | Out-Null
New-Item -ItemType Directory -Force -Path (Split-Path -Parent $manifest) | Out-Null

kaggle datasets download -d $Dataset -p $output --unzip

$imageExtensions = @(".jpg", ".jpeg", ".png", ".webp")
$rows = Get-ChildItem -LiteralPath $output -Recurse -File |
    Where-Object { $imageExtensions -contains $_.Extension.ToLowerInvariant() } |
    Where-Object {
        $relative = Resolve-Path -LiteralPath $_.FullName -Relative
        $relativeLower = $relative.ToLowerInvariant()
        $relativeLower -notmatch "[\\/]images[\\/]images[\\/]"
    } |
    ForEach-Object {
        $relative = Resolve-Path -LiteralPath $_.FullName -Relative
        $relativeLower = $relative.ToLowerInvariant()
        $safeId = ($relativeLower -replace "^[.][\\/]", "") -replace "[^a-z0-9]+", "_"
        $messLevel = if ($relativeLower -match "[\\/](messy|dirty|clutter)[\\/]") {
            "heavy_reset"
        } elseif ($relativeLower -match "[\\/](clean|clear)[\\/]") {
            "clear"
        } else {
            "needs_review"
        }
        $split = if ($relativeLower -match "[\\/]train[\\/]") {
            "train"
        } elseif ($relativeLower -match "[\\/]val(idation)?[\\/]") {
            "validation"
        } elseif ($relativeLower -match "[\\/]test[\\/]") {
            "test"
        } else {
            "train"
        }
        [PSCustomObject]@{
            image_id = "kaggle_messy_clean_$safeId"
            source_id = "kaggle_messy_clean"
            source_url = "https://www.kaggle.com/datasets/$Dataset"
            local_path = $relative
            local_exists = $true
            room_type = "mixed"
            mess_level = $messLevel
            visible_issues = ""
            confidence = if ($messLevel -eq "needs_review") { "low" } else { "medium" }
            license = "verify_kaggle_dataset_license"
            attribution = $Dataset
            split = $split
            notes = "Imported from Kaggle folder labels; review before model training. Unlabeled test images remain needs_review."
        }
    }

$rows | Export-Csv -LiteralPath $manifest -NoTypeInformation -Encoding UTF8
Write-Output "Wrote $manifest"
Write-Output "Images indexed: $(@($rows).Count)"
