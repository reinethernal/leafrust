#!/usr/bin/env python3
"""
Download PlantVillage and export ImageFolder layout for LeafRust training.

Primary source (recommended):
  Hugging Face  mohanty/PlantVillage  (color) — ~54k images, Mohanty et al.

Also documented:
  GitHub        spMohanty/PlantVillage-Dataset
  Kaggle        abdallahalidev/plantvillage-dataset

Usage:
  pip install -r requirements-train.txt
  python download_plantvillage.py --out ../data/plantvillage

Then train:
  python train_mobilenet.py --data ../data/plantvillage --epochs 8 \\
    --out ../android/app/src/main/assets/models/plantvillage_mobilenet.tflite
"""

from __future__ import annotations

import argparse
import shutil
from collections import Counter
from pathlib import Path


# Order expected by the Android app (PlantAi / PlantLabels). Index 4 = Background.
LEAFRUST_CLASSES = [
    "Apple___Apple_scab",
    "Apple___Black_rot",
    "Apple___Cedar_apple_rust",
    "Apple___healthy",
    "Background",
    "Blueberry___healthy",
    "Cherry___Powdery_mildew",
    "Cherry___healthy",
    "Corn___Cercospora_leaf_spot Gray_leaf_spot",
    "Corn___Common_rust",
    "Corn___Northern_Leaf_Blight",
    "Corn___healthy",
    "Grape___Black_rot",
    "Grape___Esca_(Black_Measles)",
    "Grape___Leaf_blight_(Isariopsis_Leaf_Spot)",
    "Grape___healthy",
    "Orange___Haunglongbing_(Citrus_greening)",
    "Peach___Bacterial_spot",
    "Peach___healthy",
    "Pepper___Bacterial_spot",
    "Pepper___healthy",
    "Potato___Early_blight",
    "Potato___Late_blight",
    "Potato___healthy",
    "Raspberry___healthy",
    "Soybean___healthy",
    "Squash___Powdery_mildew",
    "Strawberry___Leaf_scorch",
    "Strawberry___healthy",
    "Tomato___Bacterial_spot",
    "Tomato___Early_blight",
    "Tomato___Late_blight",
    "Tomato___Leaf_Mold",
    "Tomato___Septoria_leaf_spot",
    "Tomato___Spider_mites Two-spotted_spider_mite",
    "Tomato___Target_Spot",
    "Tomato___Tomato_Yellow_Leaf_Curl_Virus",
    "Tomato___Tomato_mosaic_virus",
    "Tomato___healthy",
]


def normalize_label(name: str) -> str:
    """Map HF / folder variants onto LeafRust class ids."""
    n = name.strip().replace("/", "___")
    aliases = {
        "Cherry_(including_sour)___Powdery_mildew": "Cherry___Powdery_mildew",
        "Cherry_(including_sour)___healthy": "Cherry___healthy",
        "Corn_(maize)___Cercospora_leaf_spot Gray_leaf_spot": "Corn___Cercospora_leaf_spot Gray_leaf_spot",
        "Corn_(maize)___Common_rust_": "Corn___Common_rust",
        "Corn_(maize)___Common_rust": "Corn___Common_rust",
        "Corn_(maize)___Northern_Leaf_Blight": "Corn___Northern_Leaf_Blight",
        "Corn_(maize)___healthy": "Corn___healthy",
        "Pepper,_bell___Bacterial_spot": "Pepper___Bacterial_spot",
        "Pepper,_bell___healthy": "Pepper___healthy",
    }
    return aliases.get(n, n)


def export_from_huggingface(out_dir: Path, config: str, max_per_class: int | None) -> None:
    from datasets import load_dataset

    print(f"Loading Hugging Face dataset mohanty/PlantVillage ({config}) …")
    ds = load_dataset("mohanty/PlantVillage", config)
    # Prefer train+test merged for full ImageFolder
    splits = []
    if "train" in ds:
        splits.append(ds["train"])
    if "test" in ds:
        splits.append(ds["test"])
    if not splits and "train" not in ds:
        # single split
        splits = [ds[next(iter(ds.keys()))]]

    out_dir.mkdir(parents=True, exist_ok=True)
    for c in LEAFRUST_CLASSES:
        (out_dir / c).mkdir(parents=True, exist_ok=True)

    counts: Counter[str] = Counter()
    skipped = Counter()
    idx = 0
    for split in splits:
        for row in split:
            label = row.get("label")
            if hasattr(label, "item"):
                # int class id — resolve via feature names if present
                names = split.features["label"].names
                label = names[int(label)]
            label = normalize_label(str(label))
            if label not in LEAFRUST_CLASSES:
                skipped[label] += 1
                continue
            if max_per_class is not None and counts[label] >= max_per_class:
                continue
            image = row["image"]
            counts[label] += 1
            idx += 1
            dest = out_dir / label / f"{idx:06d}.jpg"
            if dest.exists():
                continue
            rgb = image.convert("RGB")
            rgb.save(dest, quality=92)

    # Empty Background folder (optional hard-negatives can be added later)
    bg = out_dir / "Background"
    bg.mkdir(exist_ok=True)
    if counts["Background"] == 0:
        print(
            "Note: no Background images in PlantVillage — folder created empty. "
            "Add non-leaf photos there if you want that class during training."
        )

    print("Exported class counts:")
    for c in LEAFRUST_CLASSES:
        print(f"  {c}: {counts[c]}")
    if skipped:
        print("Skipped unknown labels (top):")
        for k, v in skipped.most_common(15):
            print(f"  {k}: {v}")


def export_from_imagefolder(src: Path, out_dir: Path) -> None:
    """Copy/rename an existing PlantVillage color/ tree into LeafRust ids."""
    if out_dir.exists():
        shutil.rmtree(out_dir)
    out_dir.mkdir(parents=True)
    for src_dir in sorted(p for p in src.iterdir() if p.is_dir()):
        label = normalize_label(src_dir.name)
        if label not in LEAFRUST_CLASSES:
            print("skip", src_dir.name, "->", label)
            continue
        dest = out_dir / label
        dest.mkdir(parents=True, exist_ok=True)
        n = 0
        for img in src_dir.rglob("*"):
            if img.suffix.lower() not in {".jpg", ".jpeg", ".png", ".JPG", ".JPEG", ".PNG"}:
                continue
            n += 1
            shutil.copy2(img, dest / f"{n:06d}{img.suffix.lower()}")
        print(f"{label}: {n}")
    (out_dir / "Background").mkdir(exist_ok=True)


def main() -> None:
    parser = argparse.ArgumentParser(description="Prepare PlantVillage for LeafRust")
    parser.add_argument(
        "--out",
        type=Path,
        default=Path(__file__).resolve().parents[1] / "data" / "plantvillage",
        help="Output ImageFolder directory",
    )
    parser.add_argument(
        "--source",
        choices=("hf", "folder"),
        default="hf",
        help="hf = HuggingFace mohanty/PlantVillage; folder = local color/ tree",
    )
    parser.add_argument(
        "--folder",
        type=Path,
        help="Local PlantVillage color/ directory (with --source folder)",
    )
    parser.add_argument(
        "--config",
        default="color",
        choices=("color", "grayscale", "segmented"),
        help="HuggingFace config name",
    )
    parser.add_argument(
        "--max-per-class",
        type=int,
        default=None,
        help="Optional cap per class (faster smoke tests)",
    )
    args = parser.parse_args()

    if args.source == "hf":
        export_from_huggingface(args.out, args.config, args.max_per_class)
    else:
        if not args.folder or not args.folder.is_dir():
            raise SystemExit("--folder path required for --source folder")
        export_from_imagefolder(args.folder, args.out)

    print(f"\nDone. Train with:\n  python train_mobilenet.py --data {args.out}")


if __name__ == "__main__":
    main()
