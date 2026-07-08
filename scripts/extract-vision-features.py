#!/usr/bin/env python
"""Extract simple offline image features for TidyPilot scan research.

This is not a production model. It creates a repeatable feature table from
locally downloaded room photos so scanner work can move from anecdotes to
measured evidence before an on-device model is trained.
"""

from __future__ import annotations

import argparse
import csv
import math
from pathlib import Path
from statistics import mean, pstdev

from PIL import Image, ImageFilter, ImageStat


MESS_LABELS = {
    "clear": 0,
    "light_reset": 1,
    "moderate_mess": 2,
    "heavy_reset": 3,
}


def repo_path(path: str | Path) -> Path:
    value = Path(path)
    if value.is_absolute():
        return value
    return Path.cwd() / value


def histogram_entropy(values: list[int]) -> float:
    total = sum(values)
    if total == 0:
        return 0.0
    entropy = 0.0
    for count in values:
        if count:
            p = count / total
            entropy -= p * math.log2(p)
    return entropy


def grid_stddev(gray: Image.Image, cells: int = 4) -> tuple[float, int]:
    width, height = gray.size
    scores: list[float] = []
    for y in range(cells):
        for x in range(cells):
            box = (
                int(width * x / cells),
                int(height * y / cells),
                int(width * (x + 1) / cells),
                int(height * (y + 1) / cells),
            )
            crop = gray.crop(box)
            scores.append(ImageStat.Stat(crop).stddev[0])
    return mean(scores), sum(1 for score in scores if score >= 45)


def feature_score(
    edge_density: float,
    entropy: float,
    contrast: float,
    saturation_mean: float,
    busy_cells: int,
) -> int:
    # Experimental proxy: cluttered scenes usually have more edges, more local
    # brightness variation, and more visually busy regions. This score is used
    # only for research/evaluation, not as a claim of perfect detection.
    score = (
        edge_density * 0.35
        + min(entropy / 8.0, 1.0) * 0.18
        + min(contrast / 85.0, 1.0) * 0.20
        + min(saturation_mean / 120.0, 1.0) * 0.08
        + min(busy_cells / 16.0, 1.0) * 0.19
    )
    return round(max(0.0, min(score, 1.0)) * 100)


def quality_label(width: int, height: int, brightness: float, contrast: float, edge_density: float) -> str:
    if width < 240 or height < 240:
        return "low_resolution"
    if brightness < 45:
        return "too_dark"
    if brightness > 220:
        return "too_bright"
    if contrast < 22 and edge_density < 0.12:
        return "possibly_blurry"
    return "usable"


def extract_features(path: Path) -> dict[str, str]:
    with Image.open(path) as image:
        image = image.convert("RGB")
        width, height = image.size
        resized = image.resize((160, 160))
        gray = resized.convert("L")
        hsv = resized.convert("HSV")
        saturation = hsv.getchannel("S")
        edges = gray.filter(ImageFilter.FIND_EDGES)

        gray_stat = ImageStat.Stat(gray)
        edge_values = list(image_values(edges))
        edge_density = sum(1 for value in edge_values if value >= 32) / len(edge_values)
        brightness = gray_stat.mean[0]
        contrast = gray_stat.stddev[0]
        entropy = histogram_entropy(gray.histogram())
        grid_variation, busy_cells = grid_stddev(gray)
        saturation_mean = ImageStat.Stat(saturation).mean[0]
        gray_values = list(image_values(gray))
        dark_ratio = sum(1 for value in gray_values if value < 35) / (160 * 160)
        bright_ratio = sum(1 for value in gray_values if value > 230) / (160 * 160)

        pixel_score = feature_score(edge_density, entropy, contrast, saturation_mean, busy_cells)
        return {
            "width": str(width),
            "height": str(height),
            "brightness_mean": f"{brightness:.2f}",
            "contrast_stddev": f"{contrast:.2f}",
            "entropy": f"{entropy:.3f}",
            "edge_density": f"{edge_density:.4f}",
            "saturation_mean": f"{saturation_mean:.2f}",
            "grid_variation": f"{grid_variation:.2f}",
            "busy_grid_cells": str(busy_cells),
            "dark_ratio": f"{dark_ratio:.4f}",
            "bright_ratio": f"{bright_ratio:.4f}",
            "pixel_mess_score": str(pixel_score),
            "photo_quality": quality_label(width, height, brightness, contrast, edge_density),
        }


def image_values(image: Image.Image):
    if hasattr(image, "get_flattened_data"):
        return image.get_flattened_data()
    return image.getdata()


def main() -> int:
    parser = argparse.ArgumentParser(description="Build a TidyPilot offline image feature CSV.")
    parser.add_argument("--manifest", default=".local-vision-datasets/manifests/tidypilot_combined_manifest.csv")
    parser.add_argument("--output", default=".local-vision-datasets/features/tidypilot_combined_features.csv")
    parser.add_argument("--max-images", type=int, default=0, help="Optional cap for quick smoke runs.")
    args = parser.parse_args()

    manifest_path = repo_path(args.manifest)
    output_path = repo_path(args.output)
    output_path.parent.mkdir(parents=True, exist_ok=True)

    rows: list[dict[str, str]] = []
    with manifest_path.open(newline="", encoding="utf-8-sig") as handle:
        reader = csv.DictReader(handle)
        for row in reader:
            local_path = row.get("local_path", "")
            if not local_path:
                continue
            image_path = repo_path(local_path)
            if not image_path.exists():
                continue
            features = extract_features(image_path)
            mess_level = row.get("mess_level", "needs_review")
            label_numeric = MESS_LABELS.get(mess_level, "")
            pixel_score = int(features["pixel_mess_score"])
            review_priority = "review" if mess_level == "needs_review" else (
                "possible_label_mismatch"
                if label_numeric != "" and abs(pixel_score - (int(label_numeric) * 33)) >= 45
                else "ok"
            )
            rows.append(
                {
                    "image_id": row.get("image_id", ""),
                    "source_id": row.get("source_id", ""),
                    "local_path": local_path,
                    "room_type": row.get("room_type", ""),
                    "mess_level": mess_level,
                    "label_numeric": str(label_numeric),
                    **features,
                    "review_priority": review_priority,
                }
            )
            if args.max_images and len(rows) >= args.max_images:
                break

    fieldnames = [
        "image_id",
        "source_id",
        "local_path",
        "room_type",
        "mess_level",
        "label_numeric",
        "width",
        "height",
        "brightness_mean",
        "contrast_stddev",
        "entropy",
        "edge_density",
        "saturation_mean",
        "grid_variation",
        "busy_grid_cells",
        "dark_ratio",
        "bright_ratio",
        "pixel_mess_score",
        "photo_quality",
        "review_priority",
    ]
    with output_path.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(rows)

    print(f"Wrote {output_path}")
    print(f"Images with features: {len(rows)}")
    if rows:
        usable = sum(1 for row in rows if row["photo_quality"] == "usable")
        review = sum(1 for row in rows if row["review_priority"] != "ok")
        avg_score = mean(int(row["pixel_mess_score"]) for row in rows)
        print(f"Usable photos: {usable}")
        print(f"Needs review: {review}")
        print(f"Average pixel mess score: {avg_score:.1f}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
