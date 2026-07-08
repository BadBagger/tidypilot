param(
    [string]$FeaturePath = ".local-vision-datasets\features\tidypilot_combined_features.csv",
    [string]$OutputPath = ".local-vision-datasets\review\tidypilot_label_review.csv",
    [int]$Limit = 120,
    [switch]$IncludeOk
)

$ErrorActionPreference = "Stop"

$argsList = @(
    "scripts\build-vision-label-review.py",
    "--features", $FeaturePath,
    "--output", $OutputPath,
    "--limit", "$Limit"
)

if ($IncludeOk) {
    $argsList += "--include-ok"
}

python @argsList
