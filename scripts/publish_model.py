#!/usr/bin/env python3
"""
Copy trained TFLite + labels into docs/models/ and write model_manifest.json
so the Android app can download updates from the LeafRust GitHub Pages CDN.

Usage (after train_mobilenet.py):
  python publish_model.py
  git add docs/models && git commit -m "Update on-device model" && git push
"""

from __future__ import annotations

import argparse
import hashlib
import json
import shutil
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_SRC = ROOT / "android/app/src/main/assets/models/plantvillage_mobilenet.tflite"
DEFAULT_LABELS = ROOT / "android/app/src/main/assets/models/labels.txt"
OUT_DIR = ROOT / "docs/models"
CDN_BASE = "https://reinethernal.github.io/leafrust/docs/models"
RAW_BASE = "https://raw.githubusercontent.com/reinethernal/leafrust/main/docs/models"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--model", type=Path, default=DEFAULT_SRC)
    parser.add_argument("--labels", type=Path, default=DEFAULT_LABELS)
    parser.add_argument("--version", type=int, default=None, help="Bump modelVersion (int)")
    parser.add_argument(
        "--version-name",
        default=None,
        help="Human version, default timestamp UTC",
    )
    args = parser.parse_args()

    if not args.model.is_file():
        raise SystemExit(f"Model not found: {args.model}")
    if not args.labels.is_file():
        raise SystemExit(f"Labels not found: {args.labels}")

    OUT_DIR.mkdir(parents=True, exist_ok=True)
    model_name = "plantvillage_mobilenet.tflite"
    labels_name = "labels.txt"
    dest_model = OUT_DIR / model_name
    dest_labels = OUT_DIR / labels_name
    shutil.copy2(args.model, dest_model)
    shutil.copy2(args.labels, dest_labels)

    raw = dest_model.read_bytes()
    sha = hashlib.sha256(raw).hexdigest()
    size = len(raw)

    manifest_path = OUT_DIR / "model_manifest.json"
    prev_version = 1
    if manifest_path.is_file():
        try:
            prev_version = int(json.loads(manifest_path.read_text(encoding="utf-8")).get("version", 1))
        except Exception:
            prev_version = 1
    version = args.version if args.version is not None else prev_version + (0 if not manifest_path.exists() else 1)
    if args.version is None and not manifest_path.exists():
        version = 1
    elif args.version is None and manifest_path.exists():
        # bump when content changes
        try:
            old = json.loads(manifest_path.read_text(encoding="utf-8"))
            version = int(old.get("version", 1)) + (0 if old.get("sha256") == sha else 1)
        except Exception:
            version = prev_version + 1

    version_name = args.version_name or datetime.now(timezone.utc).strftime("%Y.%m.%d")
    manifest = {
        "version": version,
        "versionName": version_name,
        "model": model_name,
        "labels": labels_name,
        "sha256": sha,
        "size": size,
        "minAppVersionCode": 1,
        "updatedAt": datetime.now(timezone.utc).isoformat(),
        "urls": {
            "model": [
                f"{CDN_BASE}/{model_name}",
                f"{RAW_BASE}/{model_name}",
            ],
            "labels": [
                f"{CDN_BASE}/{labels_name}",
                f"{RAW_BASE}/{labels_name}",
            ],
            "manifest": [
                f"{CDN_BASE}/model_manifest.json",
                f"{RAW_BASE}/model_manifest.json",
            ],
        },
    }
    text = json.dumps(manifest, indent=2) + "\n"
    manifest_path.write_text(text, encoding="utf-8", newline="\n")
    # Keep a copy next to assets for local reference
    (DEFAULT_SRC.parent / "model_manifest.json").write_text(text, encoding="utf-8", newline="\n")

    print("Published:")
    print(f"  {dest_model} ({size} bytes)")
    print(f"  {dest_labels}")
    print(f"  {manifest_path}")
    print(f"  version={version} sha256={sha}")
    print("Commit & push docs/models so devices can download the update.")


if __name__ == "__main__":
    main()
