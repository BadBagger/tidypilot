# TidyPilot Vision Dataset Scripts

## Build the starter manifest

```powershell
.\scripts\build-vision-manifest.ps1
```

Output is written to:

```text
.local-vision-datasets\manifests\tidypilot_vision_manifest.csv
```

That folder is ignored by git.

## Try downloading small public fixtures

```powershell
.\scripts\build-vision-manifest.ps1 -DownloadSmallFixtures
```

This only attempts direct Wikimedia file downloads from the seed manifest. Large
datasets such as Places365, ADE20K, Open Images, COCO, Kaggle, and Roboflow
should be downloaded through their official tools and license terms.

## Dataset rule

Commit manifests, scripts, labels, and evaluation summaries. Do not commit large
datasets, downloaded photos, Kaggle credentials, Roboflow keys, or license-
restricted media.

## Summarize the local manifest

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\summarize-vision-manifest.ps1
```

## Fetch public metadata

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\fetch-vision-metadata.ps1
```

This downloads small metadata files such as Places365 categories and Open Images
class descriptions into `.local-vision-datasets\metadata`, then writes filtered
target CSVs for TidyPilot room/object classes.

## Import Kaggle clean/messy room data

Requires local Kaggle CLI and either `%USERPROFILE%\.kaggle\access_token` or
the legacy `%USERPROFILE%\.kaggle\kaggle.json`. Keep credentials outside the
repo.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\import-kaggle-clean-messy.ps1
```

The images and generated manifest stay under `.local-vision-datasets`.

## Merge available local manifests

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\merge-vision-manifests.ps1
```

This writes:

```text
.local-vision-datasets\manifests\tidypilot_combined_manifest.csv
```

The combined manifest normalizes mess labels to TidyPilot's app-level buckets:
`clear`, `light_reset`, `moderate_mess`, `heavy_reset`, and `needs_review`.

## Extract offline image features

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\build-vision-feature-table.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\summarize-vision-features.ps1
```

This creates an ignored local CSV with simple pixel measurements such as
brightness, contrast, edge/detail density, entropy, and a rough experimental
pixel mess score. It is research tooling only; it does not claim perfect
computer vision.

## Build a local review contact sheet

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\build-vision-review-contact-sheet.ps1
```

This writes an ignored HTML contact sheet under `.local-vision-datasets\review`
so possible label mismatches can be reviewed without uploading photos anywhere.

## Prepare an Open Images household sample

First fetch public metadata:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\fetch-vision-metadata.ps1
```

Then create a validation-split sample manifest and official downloader image
list:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\prepare-open-images-sample.ps1 -MaxPerClass 5
```

To download the selected images through the official Open Images downloader:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\prepare-open-images-sample.ps1 -MaxPerClass 5 -DownloadImages -InstallPythonDeps
```
