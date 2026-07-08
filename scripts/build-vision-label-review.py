#!/usr/bin/env python
"""Create a local CSV for manually reviewing TidyPilot vision labels."""

from __future__ import annotations

import argparse
import csv
from pathlib import Path


MESS_LEVELS = ["clear", "light_reset", "moderate_mess", "heavy_reset", "needs_review"]


def repo_path(path: str | Path) -> Path:
    value = Path(path)
    if value.is_absolute():
        return value
    return Path.cwd() / value


def suggested_review(row: dict[str, str]) -> str:
    level = row.get("mess_level", "needs_review")
    score = int(row.get("pixel_mess_score") or 0)
    if level == "needs_review":
        return "label_missing"
    if level == "clear" and score >= 58:
        return "clear_label_but_visually_busy"
    if level == "heavy_reset" and score <= 45:
        return "heavy_label_but_visually_simple"
    return "spot_check"


def main() -> int:
    parser = argparse.ArgumentParser(description="Build a manual label-review CSV.")
    parser.add_argument("--features", default=".local-vision-datasets/features/tidypilot_combined_features.csv")
    parser.add_argument("--output", default=".local-vision-datasets/review/tidypilot_label_review.csv")
    parser.add_argument("--limit", type=int, default=120)
    parser.add_argument("--include-ok", action="store_true")
    args = parser.parse_args()

    feature_path = repo_path(args.features)
    output_path = repo_path(args.output)
    output_path.parent.mkdir(parents=True, exist_ok=True)

    with feature_path.open(newline="", encoding="utf-8-sig") as handle:
        rows = list(csv.DictReader(handle))

    if not args.include_ok:
        rows = [row for row in rows if row.get("review_priority") != "ok"]

    rows.sort(
        key=lambda row: (
            row.get("review_priority") == "ok",
            row.get("mess_level") != "needs_review",
            -int(row.get("pixel_mess_score") or 0),
        )
    )
    rows = rows[: args.limit]

    fieldnames = [
        "image_id",
        "local_path",
        "source_id",
        "current_room_type",
        "current_mess_level",
        "pixel_mess_score",
        "photo_quality",
        "review_priority",
        "suggested_review",
        "reviewer_mess_level",
        "reviewer_room_type",
        "reviewer_visible_issues",
        "reviewer_notes",
        "allowed_mess_levels",
    ]

    with output_path.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=fieldnames)
        writer.writeheader()
        for row in rows:
            writer.writerow(
                {
                    "image_id": row.get("image_id", ""),
                    "local_path": row.get("local_path", ""),
                    "source_id": row.get("source_id", ""),
                    "current_room_type": row.get("room_type", ""),
                    "current_mess_level": row.get("mess_level", ""),
                    "pixel_mess_score": row.get("pixel_mess_score", ""),
                    "photo_quality": row.get("photo_quality", ""),
                    "review_priority": row.get("review_priority", ""),
                    "suggested_review": suggested_review(row),
                    "reviewer_mess_level": "",
                    "reviewer_room_type": "",
                    "reviewer_visible_issues": "",
                    "reviewer_notes": "",
                    "allowed_mess_levels": "|".join(MESS_LEVELS),
                }
            )

    print(f"Wrote {output_path}")
    print(f"Review rows: {len(rows)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
