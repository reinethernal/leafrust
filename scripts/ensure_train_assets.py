#!/usr/bin/env python3
"""Ensure PlantVillage ImageFolder + baseline LeafRust models exist (download if missing)."""

from __future__ import annotations

import urllib.request
from pathlib import Path

from hf_auth import load_hf_token, login_hf

# Published CDN / GitHub raw (same as app ModelDownloader)
BASELINE_URLS = {
    "plantvillage_mobilenet.tflite": [
        "https://reinethernal.github.io/leafrust/docs/models/plantvillage_mobilenet.tflite",
        "https://raw.githubusercontent.com/reinethernal/leafrust/main/docs/models/plantvillage_mobilenet.tflite",
    ],
    "labels.txt": [
        "https://reinethernal.github.io/leafrust/docs/models/labels.txt",
        "https://raw.githubusercontent.com/reinethernal/leafrust/main/docs/models/labels.txt",
    ],
    "model_manifest.json": [
        "https://reinethernal.github.io/leafrust/docs/models/model_manifest.json",
        "https://raw.githubusercontent.com/reinethernal/leafrust/main/docs/models/model_manifest.json",
    ],
}


def ensure_plantvillage(
    repo: Path,
    data_dir: Path,
    max_per_class: int | None = None,
    force: bool = False,
) -> Path:
    """Download + export ImageFolder under data_dir if missing/empty."""
    login_hf(load_hf_token())
    data_dir = Path(data_dir)
    if not force and data_dir.is_dir() and any(p.is_dir() for p in data_dir.iterdir()):
        n = sum(1 for p in data_dir.iterdir() if p.is_dir())
        print(f"Dataset present: {data_dir} ({n} class folders)")
        return data_dir

    from download_plantvillage import download_and_extract, export_imagefolder, find_variant_root

    cache = Path(repo) / "data" / "plantvillage_cache"
    print(f"Downloading PlantVillage into {cache} …")
    extract_root = download_and_extract(cache, force=force)
    src = find_variant_root(extract_root, "color")
    export_imagefolder(src, data_dir, max_per_class)
    print(f"Dataset ready: {data_dir}")
    return data_dir


def ensure_torchvision_weights() -> None:
    """Trigger MobileNetV3 ImageNet weight download if not cached."""
    import torch
    from torchvision import models

    print("Ensuring torchvision MobileNetV3-Small ImageNet weights …")
    _ = models.mobilenet_v3_small(weights=models.MobileNet_V3_Small_Weights.IMAGENET1K_V1)
    del _
    if torch.cuda.is_available():
        torch.cuda.empty_cache()
    print("Torchvision weights OK")


def ensure_baseline_models(export_dir: Path, names: list[str] | None = None) -> dict[str, Path]:
    """Download published LeafRust TFLite + labels into export_dir if missing."""
    export_dir = Path(export_dir)
    export_dir.mkdir(parents=True, exist_ok=True)
    wanted = names or list(BASELINE_URLS.keys())
    out: dict[str, Path] = {}
    for name in wanted:
        dest = export_dir / name
        if dest.is_file() and dest.stat().st_size > 0:
            print(f"Baseline present: {dest}")
            out[name] = dest
            continue
        urls = BASELINE_URLS.get(name)
        if not urls:
            print(f"No URL for {name}, skip")
            continue
        last_err: Exception | None = None
        for url in urls:
            try:
                print(f"Downloading {name} <- {url}")
                urllib.request.urlretrieve(url, dest)
                if dest.stat().st_size <= 0:
                    raise RuntimeError("empty download")
                print(f"  saved {dest} ({dest.stat().st_size} bytes)")
                out[name] = dest
                last_err = None
                break
            except Exception as exc:
                last_err = exc
                if dest.exists():
                    dest.unlink(missing_ok=True)
        if last_err is not None and name not in out:
            print(f"WARNING: could not download {name}: {last_err}")
    return out
