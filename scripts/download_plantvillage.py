#!/usr/bin/env python3
"""
Download PlantVillage images and export ImageFolder layout for LeafRust training.

The Hugging Face card `mohanty/PlantVillage` only exposes path lists via
`load_dataset` — the real images are in LFS file `data.zip` (~2.1 GB).

Usage:
  pip install -r requirements-train.txt
  python download_plantvillage.py --out ../data/plantvillage
  # smoke test:
  python download_plantvillage.py --out ../data/plantvillage_small --max-per-class 50
"""

from __future__ import annotations

import argparse
import shutil
import zipfile
from collections import Counter
from pathlib import Path

from huggingface_hub import hf_hub_download

try:
    from hf_auth import load_hf_token, login_hf
except ImportError:  # running as script from other cwd
    load_hf_token = None  # type: ignore
    login_hf = None  # type: ignore

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


def find_variant_root(extract_root: Path, variant: str) -> Path:
    """Locate raw/<variant> inside the extracted archive."""
    candidates = [
        extract_root / "raw" / variant,
        extract_root / variant,
        extract_root / "PlantVillage-Dataset" / "raw" / variant,
        extract_root / "data" / "raw" / variant,
    ]
    for c in candidates:
        if c.is_dir():
            return c
    # deep search
    matches = list(extract_root.rglob(variant))
    for m in matches:
        if m.is_dir() and any(m.iterdir()):
            # prefer .../raw/color
            if m.parent.name == "raw" or m.name == variant:
                return m
    raise SystemExit(
        f"Could not find '{variant}/' inside extracted archive under {extract_root}. "
        f"Top-level entries: {[p.name for p in extract_root.iterdir()][:20]}"
    )


def download_and_extract(cache_dir: Path, force: bool) -> Path:
    """Download data.zip from Hugging Face and extract once."""
    cache_dir.mkdir(parents=True, exist_ok=True)
    extract_root = cache_dir / "extracted"
    marker = extract_root / ".ok"

    if marker.exists() and not force:
        print(f"Using cached extract: {extract_root}")
        return extract_root

    print("Downloading data.zip from Hugging Face (~2.1 GB, one-time) …")
    token = None
    if load_hf_token is not None:
        token = login_hf(load_hf_token()) if login_hf else load_hf_token()
    zip_path = Path(
        hf_hub_download(
            repo_id="mohanty/PlantVillage",
            filename="data.zip",
            repo_type="dataset",
            token=token,
        )
    )
    print(f"Archive: {zip_path} ({zip_path.stat().st_size / 1e9:.2f} GB)")

    if extract_root.exists():
        shutil.rmtree(extract_root)
    extract_root.mkdir(parents=True)

    print("Extracting (this can take several minutes) …")
    with zipfile.ZipFile(zip_path, "r") as zf:
        zf.extractall(extract_root)
    marker.write_text("ok\n", encoding="utf-8")
    print(f"Extracted to {extract_root}")
    return extract_root


def export_imagefolder(
    src_root: Path,
    out_dir: Path,
    max_per_class: int | None,
) -> None:
    if out_dir.exists():
        shutil.rmtree(out_dir)
    out_dir.mkdir(parents=True)

    for c in LEAFRUST_CLASSES:
        (out_dir / c).mkdir(parents=True, exist_ok=True)

    counts: Counter[str] = Counter()
    skipped: Counter[str] = Counter()
    exts = {".jpg", ".jpeg", ".png", ".JPG", ".JPEG", ".PNG"}

    class_dirs = [p for p in src_root.iterdir() if p.is_dir()]
    print(f"Found {len(class_dirs)} class folders in {src_root}")

    for src_dir in sorted(class_dirs):
        label = normalize_label(src_dir.name)
        if label not in LEAFRUST_CLASSES:
            skipped[src_dir.name] += 1
            continue
        dest = out_dir / label
        n = 0
        for img in sorted(src_dir.rglob("*")):
            if img.suffix not in exts and img.suffix.lower() not in {".jpg", ".jpeg", ".png"}:
                continue
            if max_per_class is not None and n >= max_per_class:
                break
            n += 1
            counts[label] += 1
            target = dest / f"{n:06d}{img.suffix.lower()}"
            shutil.copy2(img, target)

    bg = out_dir / "Background"
    bg.mkdir(exist_ok=True)
    if counts["Background"] == 0:
        # ImageFolder cannot load empty class dirs — seed synthetic non-leaf images
        from PIL import Image

        print("Seeding synthetic Background images (no real Background samples in PlantVillage)")
        rng = __import__("numpy").random.default_rng(0)
        for i in range(64):
            arr = rng.integers(0, 256, size=(224, 224, 3), dtype="uint8")
            if i % 3 == 0:
                arr[:] = rng.integers(0, 256, size=(1, 1, 3), dtype="uint8")
            Image.fromarray(arr).save(bg / f"synth_{i:04d}.jpg", quality=85)
            counts["Background"] += 1

    print("Exported class counts:")
    for c in LEAFRUST_CLASSES:
        print(f"  {c}: {counts[c]}")
    if skipped:
        print("Skipped unknown folders:")
        for k, v in skipped.most_common():
            print(f"  {k}")


def export_from_imagefolder(src: Path, out_dir: Path, max_per_class: int | None) -> None:
    export_imagefolder(src, out_dir, max_per_class)


def main() -> None:
    root = Path(__file__).resolve().parents[1]
    parser = argparse.ArgumentParser(description="Prepare PlantVillage for LeafRust")
    parser.add_argument(
        "--out",
        type=Path,
        default=root / "data" / "plantvillage",
        help="Output ImageFolder directory",
    )
    parser.add_argument(
        "--source",
        choices=("hf", "folder"),
        default="hf",
        help="hf = download data.zip from HuggingFace; folder = local raw/color tree",
    )
    parser.add_argument(
        "--folder",
        type=Path,
        help="Local PlantVillage color/ directory (with --source folder)",
    )
    parser.add_argument(
        "--variant",
        default="color",
        choices=("color", "grayscale", "segmented"),
        help="Which images inside data.zip to use",
    )
    parser.add_argument(
        "--cache-dir",
        type=Path,
        default=root / "data" / "plantvillage_cache",
        help="Where to keep extracted data.zip contents",
    )
    parser.add_argument(
        "--force-download",
        action="store_true",
        help="Re-extract archive even if cache exists",
    )
    parser.add_argument(
        "--max-per-class",
        type=int,
        default=None,
        help="Optional cap per class (faster smoke tests)",
    )
    # Back-compat with older --config flag
    parser.add_argument("--config", default=None, help=argparse.SUPPRESS)
    args = parser.parse_args()

    if args.config:
        args.variant = args.config

    if args.source == "hf":
        extract_root = download_and_extract(args.cache_dir, args.force_download)
        src = find_variant_root(extract_root, args.variant)
        print(f"Using images from {src}")
        export_imagefolder(src, args.out, args.max_per_class)
    else:
        if not args.folder or not args.folder.is_dir():
            raise SystemExit("--folder path required for --source folder")
        export_from_imagefolder(args.folder, args.out, args.max_per_class)

    print(f"\nDone. Train with:\n  python train_mobilenet.py --data {args.out}")


if __name__ == "__main__":
    main()
