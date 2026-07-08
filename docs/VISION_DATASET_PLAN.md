# TidyPilot Vision Dataset Plan

TidyPilot needs enough image coverage to move from heuristic scan suggestions to
true on-device computer vision. No runtime network upload is approved. These
datasets are candidates for offline research, model training, and validation.

## Best Dataset Mix

| Dataset | Scale | Best use for TidyPilot | Notes |
| --- | ---: | --- | --- |
| Places365 | ~1.8M training images across 365 scene categories | Room type recognition and broad bedroom/kitchen/bathroom/living-room variation | Strong room-scene coverage, but not mess-level labels by default. |
| ADE20K | 27K+ densely annotated scene images, 3K+ object categories | Pixel-level object/region understanding: floor, bed, cabinet, table, sink, shelves | Useful for zone detection and segmentation-style mess scoring. |
| Open Images V7 | ~9M images with labels, boxes, masks, relationships, narratives | Large-scale household object detection: dishes, clothes-like items, bags, bottles, furniture | Huge, but needs filtering and license/URL handling. |
| COCO | 328K/330K images, object detection and segmentation annotations | Common object detection baseline: chair, couch, bed, bottle, bowl, cup, backpack, handbag | Useful for object classes, weaker for room mess level. |
| MIT Indoor Scenes | 15K+ images across 67 indoor categories | Indoor room classification baseline | Good supplemental room-type dataset. |
| HomeObjects-3K | ~3K natural home environment object-detection images | Household object detection in real home contexts | Good bridge dataset before training a custom detector. |
| OCID Object Clutter Indoor Dataset | Cluttered indoor object scenes with pixel-wise/structured annotations | Clutter segmentation concepts | More tabletop/robotics than whole-room cleaning, but useful for clutter logic. |
| Kaggle Messy vs Clean Room | Smaller clean/messy room classification dataset | First-pass binary mess classifier | Must verify license and exact contents before training. |
| Roboflow Clean vs Messy Room | ~100 labeled images | Smoke-test classification benchmark | Too small alone, useful as a validation sanity check. |
| Indoor clutter / CIR-rated academic datasets | ~1,800-image clutter-rating references reported in literature | Best conceptual match for severity scoring | Availability/licensing may require author or dataset access checks. |

## Recommended Training Strategy

1. Build a room-type classifier from Places365/MIT Indoor Scenes:
   - bedroom
   - bathroom
   - kitchen
   - living room
   - laundry / utility
   - basement / storage / garage
   - office

2. Build object/zone detection from ADE20K, Open Images, COCO, and HomeObjects:
   - floor path
   - table/counter/surface
   - bed
   - sink
   - shelf/storage
   - couch/chair
   - laundry/clothes-like objects
   - dishes/cups/bowls
   - bags/boxes/trash-like objects

3. Build mess-level classifier with a TidyPilot-specific labeled subset:
   - Looks mostly clear
   - Quick reset
   - Needs attention
   - Bigger reset

4. Keep a separate validation set with real household examples:
   - at least 250 images per mess level
   - balanced across room types
   - include low light, blur, glare, cropped photos, and phone camera angles

## Target Dataset Size

For a practical v1 on-device model:

- Prototype: 500-1,000 labeled room photos
- Usable closed-test model: 3,000-5,000 labeled room photos
- Stronger general model: 10,000+ labeled room photos

The large public datasets supply room/object pretraining. TidyPilot still needs
its own mess-level labels because "messy" is a subjective household state, not a
standard object class.

## Local Storage

Downloaded dataset samples should stay out of git:

```text
.local-scan-fixtures/
.local-vision-datasets/
```

Only commit manifests, labels, scripts, and evaluation summaries unless a
license review explicitly approves image redistribution.

## Candidate Sources

- Places365: https://github.com/csailvision/places365
- ADE20K: https://ade20k.csail.mit.edu/
- ADE20K GitHub mirror: https://github.com/CSAILVision/ADE20K
- Open Images V7 facts: https://storage.googleapis.com/openimages/web/factsfigures_v7.html
- Open Images repository/license notes: https://github.com/openimages/dataset
- COCO: https://cocodataset.org/
- MIT Indoor Scenes: https://www.kaggle.com/datasets/itsahmad/indoor-scenes-cvpr-2019
- HomeObjects-3K: https://docs.ultralytics.com/datasets/detect/homeobjects-3k/
- OCID: https://www.acin.tuwien.ac.at/en/vision-for-robotics/software-tools/object-clutter-indoor-dataset/
- Kaggle Messy vs Clean Room: https://www.kaggle.com/datasets/cdawn1/messy-vs-clean-room
- Roboflow Clean vs Messy Room: https://universe.roboflow.com/workspace-zpgzp/clean-vs-messy-room-0lozg

## Immediate Next Step

Create a local dataset manifest builder that can ingest downloaded metadata from
Places365, ADE20K, Open Images, COCO, and smaller clean/messy datasets, then
produce a TidyPilot CSV like:

```csv
image_path,source,room_type,mess_level,visible_issues,license,attribution
```

That manifest becomes the stable bridge between public datasets and the
on-device model training pipeline.

## Scripts Added

- `scripts/build-vision-manifest.ps1` builds the starter local manifest.
- `scripts/summarize-vision-manifest.ps1` reports split, room, mess-level, and
  local-file counts.
- `scripts/fetch-vision-metadata.ps1` downloads small public metadata files and
  filters Places365/Open Images classes into TidyPilot targets.
- `scripts/import-kaggle-clean-messy.ps1` imports the Kaggle messy/clean room
  dataset when Kaggle CLI credentials are available locally.
- `scripts/merge-vision-manifests.ps1` combines starter, Kaggle, Wikimedia, and
  Open Images manifests into one ignored local CSV with normalized TidyPilot mess
  labels.
- `scripts/build-vision-feature-table.ps1` extracts offline pixel features from
  local images so scanner work can be evaluated against brightness, blur,
  edge/detail density, entropy, and rough visual busy-ness.
- `scripts/summarize-vision-features.ps1` summarizes the generated feature table
  and highlights samples needing review.
- `scripts/build-vision-review-contact-sheet.ps1` creates a local HTML contact
  sheet for reviewing possible label mismatches and unlabeled photos.
- `scripts/build-vision-label-review.ps1` creates a local CSV with blank reviewer
  columns so visual review can become durable labels for future training.
- `scripts/prepare-open-images-sample.ps1` filters Open Images bounding-box
  annotations by vetted household object classes and creates a sample manifest
  plus image-ID list for the official Open Images downloader.

## Current Local Import Notes

The Kaggle messy/clean room import is treated as offline training material only.
Folder labels are useful for bootstrapping, but they are not trusted as perfect
room-cleanliness truth. Labeled clean images map to `clear`, labeled messy
images map to `heavy_reset`, and unlabeled test images remain `needs_review`
until manually reviewed.

Current local combined manifest after the first Kaggle/Open Images/Wikimedia
pass, with duplicated Kaggle archive folders removed:

- Total rows: 306
- Train rows: 268
- Validation rows: 28
- Test rows: 10
- Clear: 106
- Heavy reset: 113
- Needs review: 86
- Moderate mess: 1

Current local feature table:

- Local images with extracted features: 224
- Usable photo quality: 224
- Possible label mismatch: 114
- Manual review needed: 10
- Average pixel mess score for clear labels: 48.6
- Average pixel mess score for heavy-reset labels: 56.7

The next useful improvement is not just more downloads; it is balanced labeling
for `light_reset` and `moderate_mess`, plus room-type review so the model learns
bedroom, kitchen, bathroom, living room, laundry, storage, basement, and office
context separately.

The first pixel-feature pass is intentionally modest. It measures image quality
and visual busy-ness from local files so TidyPilot can start comparing scanner
outputs against repeatable evidence before any TensorFlow Lite or ML Kit model is
introduced.

Manual review is the next accuracy gate. The generated label review CSV should
be filled with `clear`, `light_reset`, `moderate_mess`, or `heavy_reset`, plus
room type and visible issue tags. That reviewed table can later feed a small
on-device model or a calibrated local heuristic without weakening the app's
privacy stance.
