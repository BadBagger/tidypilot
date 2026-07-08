param(
    [ValidateSet("validation", "test", "train")]
    [string]$Split = "validation",
    [int]$MaxPerClass = 5,
    [string]$TargetClassesPath = ".local-vision-datasets\metadata\open_images_target_classes.csv",
    [string]$OutputDir = ".local-vision-datasets\open_images",
    [switch]$DownloadImages,
    [switch]$InstallPythonDeps
)

$ErrorActionPreference = "Stop"

function Resolve-RepoPath([string]$Path) {
    if ([System.IO.Path]::IsPathRooted($Path)) { return $Path }
    return Join-Path (Get-Location) $Path
}

if ($MaxPerClass -lt 1) {
    throw "MaxPerClass must be at least 1."
}

$targetPath = Resolve-RepoPath $TargetClassesPath
if (-not (Test-Path -LiteralPath $targetPath)) {
    throw "Target classes not found. Run scripts\fetch-vision-metadata.ps1 first."
}

$root = Resolve-RepoPath $OutputDir
$metadataDir = Join-Path $root "metadata"
$manifestDir = Join-Path $root "manifests"
$imageDir = Join-Path $root "images\$Split"
New-Item -ItemType Directory -Force -Path $metadataDir, $manifestDir, $imageDir | Out-Null

$bboxFile = Join-Path $metadataDir "open_images_${Split}_bbox.csv"
$bboxUrl = "https://storage.googleapis.com/openimages/v5/${Split}-annotations-bbox.csv"
if (-not (Test-Path -LiteralPath $bboxFile)) {
    Write-Output "Downloading $bboxUrl"
    Invoke-WebRequest -Uri $bboxUrl -OutFile $bboxFile -UseBasicParsing -TimeoutSec 180
}

$targets = Import-Csv -LiteralPath $targetPath
$targetByMid = @{}
foreach ($target in $targets) {
    $targetByMid[$target.class_id] = $target
}

$selectedByClass = @{}
foreach ($target in $targets) {
    $selectedByClass[$target.class_id] = New-Object System.Collections.Generic.List[object]
}

Import-Csv -LiteralPath $bboxFile | ForEach-Object {
    $target = $targetByMid[$_.LabelName]
    if (-not $target) { return }
    $bucket = $selectedByClass[$_.LabelName]
    if ($bucket.Count -ge $MaxPerClass) { return }
    $bucket.Add([PSCustomObject]@{
        image_id = $_.ImageID
        source_id = "open_images_v7"
        source_url = "https://storage.googleapis.com/openimages/web/download_v7.html"
        local_path = ".local-vision-datasets/open_images/images/$Split/$($_.ImageID).jpg"
        room_type = "mixed"
        mess_level = "needs_review"
        visible_issues = $target.suggested_issue
        confidence = "medium"
        license = "verify_image_source_license"
        attribution = "Open Images $Split / $($target.label)"
        split = "train"
        notes = "Open Images bbox class $($target.label); review image before mess-level training."
        open_images_split = $Split
        class_id = $_.LabelName
        class_label = $target.label
        suggested_issue = $target.suggested_issue
        xmin = $_.XMin
        xmax = $_.XMax
        ymin = $_.YMin
        ymax = $_.YMax
    })
}

$rows = foreach ($key in $selectedByClass.Keys) {
    $selectedByClass[$key]
}

$manifestPath = Join-Path $manifestDir "open_images_${Split}_sample_manifest.csv"
$imageListPath = Join-Path $manifestDir "open_images_${Split}_image_list.txt"
$rows | Sort-Object class_label, image_id | Export-Csv -LiteralPath $manifestPath -NoTypeInformation -Encoding UTF8
$rows | Select-Object -ExpandProperty image_id -Unique | Sort-Object | ForEach-Object { "$Split/$_" } | Set-Content -LiteralPath $imageListPath -Encoding ASCII

Write-Output "Wrote $manifestPath"
Write-Output "Wrote $imageListPath"
Write-Output "Selected annotations: $(@($rows).Count)"
Write-Output "Unique images: $(@($rows | Select-Object -ExpandProperty image_id -Unique).Count)"

if ($DownloadImages) {
    if ($InstallPythonDeps) {
        python -m pip install --user boto3 tqdm
    }
    $hasBoto = python -c "import importlib.util; raise SystemExit(0 if importlib.util.find_spec('boto3') and importlib.util.find_spec('tqdm') else 1)"
    if ($LASTEXITCODE -ne 0) {
        throw "Python dependencies missing. Rerun with -InstallPythonDeps or install boto3 and tqdm."
    }
    $downloaderPath = Join-Path $metadataDir "open_images_downloader.py"
    if (-not (Test-Path -LiteralPath $downloaderPath)) {
        Invoke-WebRequest -Uri "https://raw.githubusercontent.com/openimages/dataset/master/downloader.py" -OutFile $downloaderPath -UseBasicParsing -TimeoutSec 60
    }
    python $downloaderPath $imageListPath --download_folder=$imageDir --num_processes=5
}
