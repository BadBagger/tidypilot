param(
    [string]$ManifestPath = ".local-vision-datasets\manifests\tidypilot_combined_manifest.csv",
    [string]$OutputPath = ".local-vision-datasets\features\tidypilot_combined_features.csv",
    [int]$MaxImages = 0
)

$ErrorActionPreference = "Stop"

$argsList = @(
    "scripts\extract-vision-features.py",
    "--manifest", $ManifestPath,
    "--output", $OutputPath
)

if ($MaxImages -gt 0) {
    $argsList += @("--max-images", "$MaxImages")
}

python @argsList
