#!/usr/bin/env python
"""Create a local HTML contact sheet for TidyPilot dataset review."""

from __future__ import annotations

import argparse
import base64
import csv
from html import escape
from io import BytesIO
from pathlib import Path

from PIL import Image


def repo_path(path: str | Path) -> Path:
    value = Path(path)
    if value.is_absolute():
        return value
    return Path.cwd() / value


def thumbnail_data_uri(path: Path, size: int = 220) -> str:
    with Image.open(path) as image:
        image = image.convert("RGB")
        image.thumbnail((size, size))
        buffer = BytesIO()
        image.save(buffer, format="JPEG", quality=78)
    encoded = base64.b64encode(buffer.getvalue()).decode("ascii")
    return f"data:image/jpeg;base64,{encoded}"


def main() -> int:
    parser = argparse.ArgumentParser(description="Build a local image review contact sheet.")
    parser.add_argument("--features", default=".local-vision-datasets/features/tidypilot_combined_features.csv")
    parser.add_argument("--output", default=".local-vision-datasets/review/tidypilot_review_contact_sheet.html")
    parser.add_argument("--limit", type=int, default=60)
    parser.add_argument("--include-ok", action="store_true")
    args = parser.parse_args()

    feature_path = repo_path(args.features)
    output_path = repo_path(args.output)
    output_path.parent.mkdir(parents=True, exist_ok=True)

    with feature_path.open(newline="", encoding="utf-8-sig") as handle:
        rows = list(csv.DictReader(handle))

    if not args.include_ok:
        rows = [row for row in rows if row.get("review_priority") != "ok"]

    rows.sort(key=lambda row: (row.get("review_priority") == "ok", -int(row.get("pixel_mess_score") or 0)))
    rows = rows[: args.limit]

    cards: list[str] = []
    for row in rows:
        local_path = repo_path(row.get("local_path", ""))
        if not local_path.exists():
            continue
        thumb = thumbnail_data_uri(local_path)
        cards.append(
            f"""
            <article class="card">
              <img src="{thumb}" alt="{escape(row.get('image_id', 'image'))}">
              <div class="meta">
                <strong>{escape(row.get('mess_level', 'needs_review'))}</strong>
                <span>pixel score {escape(row.get('pixel_mess_score', ''))}</span>
                <span>{escape(row.get('review_priority', ''))}</span>
                <small>{escape(row.get('local_path', ''))}</small>
              </div>
            </article>
            """
        )

    html = f"""<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>TidyPilot Vision Review</title>
  <style>
    :root {{
      color-scheme: light dark;
      font-family: Arial, sans-serif;
      background: #f7f3ea;
      color: #26231f;
    }}
    body {{
      margin: 0;
      padding: 24px;
    }}
    h1 {{
      margin: 0 0 8px;
      font-size: 28px;
    }}
    p {{
      margin: 0 0 24px;
      color: #625b51;
    }}
    .grid {{
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
      gap: 16px;
    }}
    .card {{
      background: #fffaf1;
      border: 1px solid #e2d8c8;
      border-radius: 12px;
      overflow: hidden;
      box-shadow: 0 6px 18px rgba(35, 28, 22, .08);
    }}
    img {{
      display: block;
      width: 100%;
      height: 180px;
      object-fit: cover;
      background: #ddd;
    }}
    .meta {{
      display: grid;
      gap: 6px;
      padding: 12px;
      font-size: 14px;
    }}
    small {{
      overflow-wrap: anywhere;
      color: #746b60;
    }}
    @media (prefers-color-scheme: dark) {{
      :root {{
        background: #171512;
        color: #f4eee4;
      }}
      p, small {{
        color: #cfc4b7;
      }}
      .card {{
        background: #24211d;
        border-color: #3a332b;
      }}
    }}
  </style>
</head>
<body>
  <h1>TidyPilot Vision Review</h1>
  <p>Local-only contact sheet for reviewing possible label mismatches and unlabeled scan fixtures.</p>
  <section class="grid">
    {''.join(cards)}
  </section>
</body>
</html>
"""
    output_path.write_text(html, encoding="utf-8")
    print(f"Wrote {output_path}")
    print(f"Review cards: {len(cards)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
