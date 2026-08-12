#!/usr/bin/env python3
"""
Fine-tune MobileNetV3-Small on PlantVillage using NVIDIA GPU (PyTorch CUDA).

Native TensorFlow pip on Windows is CPU-only — this script requires CUDA.

Install:
  pip uninstall -y tensorflow  # optional; keep if you need onnx2tf
  pip install -r requirements-train-gpu.txt --index-url https://download.pytorch.org/whl/cu124
  pip install -r requirements-train-gpu.txt   # rest from PyPI (onnx, etc.)

Run:
  python train_mobilenet_torch.py --data ../data/plantvillage \\
    --out ../android/app/src/main/assets/models/plantvillage_mobilenet.tflite
"""

from __future__ import annotations

import argparse
import json
import platform
import random
import subprocess
import sys
from pathlib import Path

import numpy as np
import torch
import torch.nn as nn
from torch.utils.data import DataLoader, Subset
from torchvision import datasets, models, transforms


IMAGENET_MEAN = (0.485, 0.456, 0.406)
IMAGENET_STD = (0.229, 0.224, 0.225)


def ensure_background_images(data_dir: Path, n: int = 64) -> None:
    """ImageFolder rejects empty class folders — seed Background/ if needed."""
    from PIL import Image

    bg = data_dir / "Background"
    bg.mkdir(parents=True, exist_ok=True)
    exts = {".jpg", ".jpeg", ".png", ".webp", ".bmp"}
    existing = [p for p in bg.iterdir() if p.suffix.lower() in exts]
    if len(existing) >= 8:
        return
    print(f"Seeding {n} synthetic Background images in {bg}")
    rng = np.random.default_rng(0)
    for i in range(n):
        arr = rng.integers(0, 256, size=(224, 224, 3), dtype=np.uint8)
        # Mix flat colors + noise so the class is "not a leaf"
        if i % 3 == 0:
            arr[:] = rng.integers(0, 256, size=(1, 1, 3), dtype=np.uint8)
        Image.fromarray(arr).save(bg / f"synth_{i:04d}.jpg", quality=85)


def require_cuda() -> torch.device:
    if not torch.cuda.is_available():
        raise SystemExit(
            "CUDA GPU not available to PyTorch.\n"
            "Install GPU build:\n"
            "  pip install torch torchvision --index-url https://download.pytorch.org/whl/cu124\n"
            f"torch={torch.__version__}, cuda_built={torch.version.cuda}"
        )
    device = torch.device("cuda")
    name = torch.cuda.get_device_name(0)
    mem = torch.cuda.get_device_properties(0).total_memory / (1024**3)
    print(f"Using GPU: {name} ({mem:.1f} GiB), torch {torch.__version__}, cuda {torch.version.cuda}")
    return device


def build_model(num_classes: int, pretrained: bool = True) -> nn.Module:
    weights = models.MobileNet_V3_Small_Weights.IMAGENET1K_V1 if pretrained else None
    model = models.mobilenet_v3_small(weights=weights)
    in_features = model.classifier[-1].in_features
    model.classifier[-1] = nn.Linear(in_features, num_classes)
    return model


def set_backbone_trainable(model: nn.Module, train_backbone: bool, unfreeze_last_blocks: int = 0) -> None:
    """Freeze/unfreeze MobileNet features for fine-tuning."""
    for p in model.features.parameters():
        p.requires_grad = False
    if not train_backbone:
        return
    # Unfreeze last N inverted residual blocks (children of features)
    blocks = list(model.features.children())
    n = max(0, min(unfreeze_last_blocks, len(blocks)))
    for block in blocks[-n:] if n else []:
        for p in block.parameters():
            p.requires_grad = True
    for p in model.classifier.parameters():
        p.requires_grad = True


def accuracy(logits: torch.Tensor, y: torch.Tensor) -> float:
    pred = logits.argmax(dim=1)
    return (pred == y).float().mean().item()


def run_epoch(
    model: nn.Module,
    loader: DataLoader,
    device: torch.device,
    optimizer: torch.optim.Optimizer | None,
    scaler: torch.amp.GradScaler | None,
) -> tuple[float, float]:
    train = optimizer is not None
    model.train(train)
    total_loss = 0.0
    total_acc = 0.0
    n = 0
    loss_fn = nn.CrossEntropyLoss()
    for x, y in loader:
        x = x.to(device, non_blocking=True)
        y = y.to(device, non_blocking=True)
        with torch.set_grad_enabled(train):
            with torch.amp.autocast("cuda", enabled=scaler is not None):
                logits = model(x)
                loss = loss_fn(logits, y)
            if train:
                optimizer.zero_grad(set_to_none=True)
                if scaler is not None:
                    scaler.scale(loss).backward()
                    scaler.step(optimizer)
                    scaler.update()
                else:
                    loss.backward()
                    optimizer.step()
        bs = y.size(0)
        total_loss += loss.item() * bs
        total_acc += accuracy(logits.detach(), y) * bs
        n += bs
    return total_loss / max(n, 1), total_acc / max(n, 1)


def export_onnx(model: nn.Module, onnx_path: Path, image_size: int = 224) -> None:
    model.eval()
    model.cpu()
    dummy = torch.randn(1, 3, image_size, image_size)
    onnx_path.parent.mkdir(parents=True, exist_ok=True)
    torch.onnx.export(
        model,
        dummy,
        str(onnx_path),
        input_names=["input"],
        output_names=["logits"],
        opset_version=17,
        dynamo=False,
    )
    print(f"Wrote {onnx_path}")


def onnx_to_tflite(onnx_path: Path, tflite_path: Path) -> None:
    """Convert ONNX → TFLite via onnx2tf (runs converter on CPU TF)."""
    out_dir = tflite_path.parent / "_onnx2tf_out"
    if out_dir.exists():
        import shutil

        shutil.rmtree(out_dir)
    out_dir.mkdir(parents=True)
    cmd = [
        sys.executable,
        "-m",
        "onnx2tf",
        "-i",
        str(onnx_path),
        "-o",
        str(out_dir),
        "-b",
        "1",
        "--non_verbose",
    ]
    print("Running:", " ".join(cmd))
    subprocess.check_call(cmd)

    # Prefer float32 saved_model → tflite with default optimize
    import tensorflow as tf

    saved = out_dir / "saved_model"
    if not saved.exists():
        # onnx2tf may write directly
        candidates = list(out_dir.rglob("*.tflite"))
        if candidates:
            tflite_path.write_bytes(candidates[0].read_bytes())
            print(f"Wrote {tflite_path} (from onnx2tf artifact)")
            return
        raise SystemExit(f"onnx2tf produced no saved_model/tflite under {out_dir}")

    converter = tf.lite.TFLiteConverter.from_saved_model(str(saved))
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    data = converter.convert()
    tflite_path.parent.mkdir(parents=True, exist_ok=True)
    tflite_path.write_bytes(data)
    print(f"Wrote {tflite_path} ({len(data) / 1024:.1f} KB)")


class SoftmaxWrapper(nn.Module):
    """Android applies softmax in post-process; export raw logits (preferred)."""

    def __init__(self, backbone: nn.Module):
        super().__init__()
        self.backbone = backbone

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        return self.backbone(x)


def main() -> None:
    parser = argparse.ArgumentParser(description="GPU fine-tune MobileNetV3 for LeafRust")
    parser.add_argument("--data", required=True, type=Path)
    parser.add_argument(
        "--out",
        type=Path,
        default=Path("../android/app/src/main/assets/models/plantvillage_mobilenet.tflite"),
    )
    parser.add_argument("--batch", type=int, default=64)
    parser.add_argument(
        "--workers",
        type=int,
        default=0 if platform.system() == "Windows" else 4,
        help="DataLoader workers (0 on Windows avoids pagefile/spawn CUDA DLL crashes)",
    )
    parser.add_argument("--image-size", type=int, default=224)
    parser.add_argument("--head-epochs", type=int, default=3)
    parser.add_argument("--ft-epochs", type=int, default=5)
    parser.add_argument("--unfreeze-blocks", type=int, default=6)
    parser.add_argument("--lr-head", type=float, default=1e-3)
    parser.add_argument("--lr-ft", type=float, default=1e-4)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--val-split", type=float, default=0.15)
    parser.add_argument("--no-amp", action="store_true", help="Disable mixed precision")
    parser.add_argument("--skip-tflite", action="store_true", help="Only save .pt + .onnx")
    parser.add_argument(
        "--checkpoint-dir",
        type=Path,
        default=Path("../data/checkpoints"),
    )
    args = parser.parse_args()

    device = require_cuda()
    random.seed(args.seed)
    np.random.seed(args.seed)
    torch.manual_seed(args.seed)
    torch.cuda.manual_seed_all(args.seed)

    if not args.data.is_dir():
        raise SystemExit(f"Dataset not found: {args.data}")

    ensure_background_images(args.data)

    train_tf = transforms.Compose(
        [
            transforms.Resize((args.image_size, args.image_size)),
            transforms.RandomHorizontalFlip(),
            transforms.ColorJitter(0.1, 0.1, 0.1, 0.05),
            transforms.ToTensor(),
            transforms.Normalize(IMAGENET_MEAN, IMAGENET_STD),
        ]
    )
    val_tf = transforms.Compose(
        [
            transforms.Resize((args.image_size, args.image_size)),
            transforms.ToTensor(),
            transforms.Normalize(IMAGENET_MEAN, IMAGENET_STD),
        ]
    )

    full = datasets.ImageFolder(args.data, transform=train_tf)
    class_names = list(full.classes)
    n = len(full)
    indices = list(range(n))
    random.shuffle(indices)
    n_val = max(1, int(n * args.val_split))
    val_idx, train_idx = indices[:n_val], indices[n_val:]

    # Separate transform for val via dataset copy
    full_val = datasets.ImageFolder(args.data, transform=val_tf)
    train_set = Subset(full, train_idx)
    val_set = Subset(full_val, val_idx)

    train_loader = DataLoader(
        train_set,
        batch_size=args.batch,
        shuffle=True,
        num_workers=args.workers,
        pin_memory=True,
        persistent_workers=args.workers > 0,
    )
    val_loader = DataLoader(
        val_set,
        batch_size=args.batch,
        shuffle=False,
        num_workers=args.workers,
        pin_memory=True,
        persistent_workers=args.workers > 0,
    )

    print(
        f"Classes={len(class_names)} train={len(train_idx)} val={len(val_idx)} "
        f"batch={args.batch} workers={args.workers}"
    )

    model = build_model(len(class_names), pretrained=True).to(device)
    args.checkpoint_dir.mkdir(parents=True, exist_ok=True)
    best_path = args.checkpoint_dir / "best_torch.pt"
    use_amp = not args.no_amp
    scaler = torch.amp.GradScaler("cuda", enabled=use_amp) if use_amp else None

    def train_phase(epochs: int, lr: float, train_backbone: bool, tag: str) -> float:
        set_backbone_trainable(model, train_backbone, args.unfreeze_blocks if train_backbone else 0)
        params = [p for p in model.parameters() if p.requires_grad]
        opt = torch.optim.AdamW(params, lr=lr, weight_decay=1e-4)
        best_acc = -1.0
        print(f"=== {tag}: epochs={epochs} lr={lr} trainable={sum(p.numel() for p in params):,} ===")
        for epoch in range(1, epochs + 1):
            tr_loss, tr_acc = run_epoch(model, train_loader, device, opt, scaler)
            va_loss, va_acc = run_epoch(model, val_loader, device, None, None)
            print(
                f"{tag} {epoch}/{epochs}  "
                f"train loss={tr_loss:.4f} acc={tr_acc:.3f}  "
                f"val loss={va_loss:.4f} acc={va_acc:.3f}"
            )
            if va_acc > best_acc:
                best_acc = va_acc
                torch.save(
                    {
                        "model": model.state_dict(),
                        "classes": class_names,
                        "val_acc": va_acc,
                    },
                    best_path,
                )
                print(f"  saved {best_path} (val_acc={va_acc:.3f})")
        return best_acc

    # Phase 1: head only
    if args.head_epochs > 0:
        train_phase(args.head_epochs, args.lr_head, train_backbone=False, tag="head")

    # Phase 2: fine-tune top blocks
    if args.ft_epochs > 0:
        train_phase(args.ft_epochs, args.lr_ft, train_backbone=True, tag="finetune")

    # Load best
    ckpt = torch.load(best_path, map_location="cpu", weights_only=False)
    model.load_state_dict(ckpt["model"])
    model.eval()

    out = args.out
    out.parent.mkdir(parents=True, exist_ok=True)
    labels_txt = out.with_name("labels.txt")
    labels_txt.write_text("\n".join(class_names) + "\n", encoding="utf-8")
    out.with_suffix(".labels.json").write_text(
        json.dumps(class_names, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    print(f"Wrote {labels_txt}")

    onnx_path = args.checkpoint_dir / "leafrust_mobilenet_v3.onnx"
    export_onnx(SoftmaxWrapper(model), onnx_path, args.image_size)

    if args.skip_tflite:
        print("Skipped TFLite conversion. ONNX at", onnx_path)
        return

    try:
        onnx_to_tflite(onnx_path, out)
    except Exception as exc:
        print("TFLite conversion failed:", exc)
        print("ONNX kept at", onnx_path)
        raise

    print("Done. Publish with: python publish_model.py")


if __name__ == "__main__":
    main()
