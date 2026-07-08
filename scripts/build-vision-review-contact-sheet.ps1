param(
    [string]$FeaturePath = ".local-vision-datasets\features\tidypilot_combined_features.csv",
    [string]$OutputPath = ".local-vision-datasets\review\tidypilot_review_contact_sheet.html",
    [int]$Limit = 60,
    [switch]$IncludeOk
)

$ErrorActionPreference = "Stop"

$argsList = @(
    "scripts\build-vision-review-contact-sheet.py",
    "--features", $FeaturePath,
    "--output", $OutputPath,
    "--limit", "$Limit"
)

if ($IncludeOk) {
    $argsList += "--include-ok"
}

python @argsList
