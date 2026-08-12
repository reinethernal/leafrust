"""
DEPRECATED for Windows GPU: use train_mobilenet_torch.py instead.

Native TensorFlow pip wheels on Windows are CPU-only. This machine has an
NVIDIA GPU — fine-tuning must use PyTorch CUDA:

  pip install torch torchvision --index-url https://download.pytorch.org/whl/cu124
  pip install -r requirements-train-gpu.txt
  python train_mobilenet_torch.py --data ../data/plantvillage \\
    --out ../android/app/src/main/assets/models/plantvillage_mobilenet.tflite
"""

from __future__ import annotations

import sys


def main() -> None:
    print(
        "TensorFlow training on native Windows cannot use your NVIDIA GPU.\n"
        "Use the CUDA fine-tune script instead:\n\n"
        "  pip install torch torchvision --index-url https://download.pytorch.org/whl/cu124\n"
        "  pip install onnx onnx2tf onnxruntime sng4onnx tensorflow\n"
        "  python train_mobilenet_torch.py --data ../data/plantvillage "
        "--out ../android/app/src/main/assets/models/plantvillage_mobilenet.tflite\n",
        file=sys.stderr,
    )
    raise SystemExit(2)


if __name__ == "__main__":
    main()
