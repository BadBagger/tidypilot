param(
    [string]$OutputDir = ".local-vision-datasets\metadata"
)

$ErrorActionPreference = "Stop"

function Resolve-RepoPath([string]$Path) {
    if ([System.IO.Path]::IsPathRooted($Path)) { return $Path }
    return Join-Path (Get-Location) $Path
}

function Download-FirstWorkingUrl([string[]]$Urls, [string]$OutputPath) {
    foreach ($url in $Urls) {
        try {
            Invoke-WebRequest -Uri $url -OutFile $OutputPath -UseBasicParsing -TimeoutSec 60
            Write-Output "Downloaded $url"
            return $true
        } catch {
            Write-Warning "Could not download ${url}: $($_.Exception.Message)"
        }
    }
    return $false
}

$metadataDir = Resolve-RepoPath $OutputDir
New-Item -ItemType Directory -Force -Path $metadataDir | Out-Null

$placesCategoriesPath = Join-Path $metadataDir "places365_categories.txt"
$placesIoPath = Join-Path $metadataDir "places365_io.txt"
$openImagesClassPath = Join-Path $metadataDir "open_images_class_descriptions.csv"

Download-FirstWorkingUrl @(
    "https://raw.githubusercontent.com/csailvision/places365/master/categories_places365.txt"
) $placesCategoriesPath | Out-Null

Download-FirstWorkingUrl @(
    "https://raw.githubusercontent.com/csailvision/places365/master/IO_places365.txt"
) $placesIoPath | Out-Null

Download-FirstWorkingUrl @(
    "https://storage.googleapis.com/openimages/v7/oidv7-class-descriptions-boxable.csv",
    "https://storage.googleapis.com/openimages/v6/oidv6-class-descriptions-boxable.csv",
    "https://storage.googleapis.com/openimages/v5/class-descriptions-boxable.csv",
    "https://storage.googleapis.com/openimages/v7/oidv7-class-descriptions.csv",
    "https://storage.googleapis.com/openimages/v6/oidv6-class-descriptions.csv",
    "https://storage.googleapis.com/openimages/v5/class-descriptions.csv",
    "https://storage.googleapis.com/openimages/2018_04/class-descriptions.csv"
) $openImagesClassPath | Out-Null

$targetRoomTerms = @(
    "bedroom", "bathroom", "kitchen", "living_room", "dining_room",
    "laundry", "utility", "basement", "garage", "home_office", "office",
    "closet", "pantry", "storage", "dorm_room", "childs_room", "nursery"
)

$targetObjectMap = @{
    "bed" = "unmade_bed"
    "couch" = "living_surface_clutter"
    "chair" = "household_object"
    "table" = "cluttered_surface"
    "desk" = "office_desk_clutter"
    "sink" = "sink_full"
    "kitchen & dining room table" = "cluttered_surface"
    "coffee table" = "living_surface_clutter"
    "toilet" = "bathroom_counter_mess"
    "bathtub" = "shower_reset_needed"
    "shower" = "shower_reset_needed"
    "washing machine" = "laundry_machine_reset"
    "dishwasher" = "dishes_visible"
    "microwave oven" = "cluttered_surface"
    "refrigerator" = "cluttered_surface"
    "wardrobe" = "closet_or_box_clutter"
    "cabinetry" = "cluttered_surface"
    "bookcase" = "cluttered_surface"
    "drawer" = "cluttered_surface"
    "clothing" = "laundry_visible"
    "footwear" = "floor_clutter"
    "backpack" = "floor_clutter"
    "handbag" = "floor_clutter"
    "suitcase" = "closet_or_box_clutter"
    "bottle" = "trash_visible"
    "bowl" = "dishes_visible"
    "cup" = "dishes_visible"
    "plate" = "dishes_visible"
    "spoon" = "dishes_visible"
    "fork" = "dishes_visible"
    "book" = "cluttered_surface"
    "box" = "closet_or_box_clutter"
    "waste container" = "trash_visible"
    "toy" = "floor_clutter"
    "towel" = "laundry_visible"
    "pillow" = "unmade_bed"
    "blanket" = "unmade_bed"
}

$placesTargets = @()
if (Test-Path -LiteralPath $placesCategoriesPath) {
    $placesLines = Get-Content -LiteralPath $placesCategoriesPath
    foreach ($line in $placesLines) {
        $parts = $line -split "\s+"
        if ($parts.Count -lt 2) { continue }
        $rawCategory = $parts[0]
        $index = $parts[1]
        $category = $rawCategory.TrimStart("/")
        $matches = $targetRoomTerms | Where-Object { $category -like "*$_*" }
        if ($matches) {
            $placesTargets += [PSCustomObject]@{
                source_id = "places365"
                class_id = $index
                category = $category
                matched_terms = ($matches -join "|")
                suggested_room_type = ($matches | Select-Object -First 1)
                use = "room_type_classifier"
            }
        }
    }
}

$openImageTargets = @()
if (Test-Path -LiteralPath $openImagesClassPath) {
    $classRows = Import-Csv -LiteralPath $openImagesClassPath -Header "mid","label"
    foreach ($row in $classRows) {
        $label = $row.label.Trim()
        $key = $label.ToLowerInvariant()
        if ($targetObjectMap.ContainsKey($key)) {
            $openImageTargets += [PSCustomObject]@{
                source_id = "open_images_v7"
                class_id = $row.mid
                label = $label
                matched_terms = $key
                suggested_issue = $targetObjectMap[$key]
                use = "household_object_detection"
            }
        }
    }
}

$placesOut = Join-Path $metadataDir "places365_target_categories.csv"
$openImagesOut = Join-Path $metadataDir "open_images_target_classes.csv"
$summaryOut = Join-Path $metadataDir "vision_metadata_summary.txt"

$placesTargets | Export-Csv -LiteralPath $placesOut -NoTypeInformation -Encoding UTF8
$openImageTargets | Export-Csv -LiteralPath $openImagesOut -NoTypeInformation -Encoding UTF8

@(
    "TidyPilot vision metadata harvest",
    "Generated: $(Get-Date -Format o)",
    "Places365 target categories: $(@($placesTargets).Count)",
    "Open Images target classes: $(@($openImageTargets).Count)",
    "",
    "Outputs:",
    $placesOut,
    $openImagesOut
) | Set-Content -LiteralPath $summaryOut -Encoding UTF8

Get-Content -LiteralPath $summaryOut
