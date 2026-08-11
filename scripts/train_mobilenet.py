"""
Train / fine-tune a MobileNetV3-Small classifier on PlantVillage-style folders
and export TensorFlow Lite weights for LeafRust.

Expected data layout (ImageFolder) — prepare with download_plantvillage.py:
  data/plantvillage/
    Apple___Apple_scab/
    Apple___healthy/
    ...

Usage:
  pip install -r requirements-train.txt
  python download_plantvillage.py --out ../data/plantvillage
  python train_mobilenet.py --data ../data/plantvillage --epochs 8 \\
    --out ../android/app/src/main/assets/models/plantvillage_mobilenet.tflite

Fine-tune after first run (unfreeze last layers):
  python train_mobilenet.py --data ../data/plantvillage --epochs 6 --finetune \\
    --checkpoint ../data/checkpoints/best.keras \\
    --out ../android/app/src/main/assets/models/plantvillage_mobilenet.tflite
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path

import tensorflow as tf


def build_model(num_classes: int, image_size: int = 224) -> tf.keras.Model:
    base = tf.keras.applications.MobileNetV3Small(
        input_shape=(image_size, image_size, 3),
        include_top=False,
        weights="imagenet",
    )
    base.trainable = False
    inputs = tf.keras.Input(shape=(image_size, image_size, 3))
    x = tf.keras.applications.mobilenet_v3.preprocess_input(inputs)
    x = base(x, training=False)
    x = tf.keras.layers.GlobalAveragePooling2D()(x)
    x = tf.keras.layers.Dropout(0.2)(x)
    outputs = tf.keras.layers.Dense(num_classes, activation="softmax")(x)
    return tf.keras.Model(inputs, outputs)


def unfreeze_for_finetune(model: tf.keras.Model, n_layers: int = 40) -> None:
    base = next(
        (layer for layer in model.layers if isinstance(layer, tf.keras.Model)),
        None,
    )
    if base is None:
        return
    base.trainable = True
    for layer in base.layers[:-n_layers]:
        layer.trainable = False


def export_tflite(model: tf.keras.Model, out_path: Path) -> None:
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    tflite_model = converter.convert()
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_bytes(tflite_model)
    print(f"Wrote {out_path} ({out_path.stat().st_size / 1024:.1f} KB)")


def write_labels_txt(class_names: list[str], tflite_out: Path) -> None:
    """Write labels.txt next to the tflite (and into assets/models if path matches)."""
    labels_txt = tflite_out.with_name("labels.txt")
    labels_txt.write_text("\n".join(class_names) + "\n", encoding="utf-8")
    print(f"Wrote {labels_txt} ({len(class_names)} classes)")
    assets_labels = (
        Path(__file__).resolve().parents[1]
        / "android"
        / "app"
        / "src"
        / "main"
        / "assets"
        / "models"
        / "labels.txt"
    )
    if tflite_out.resolve() != assets_labels.with_name(tflite_out.name).resolve():
        # still useful when exporting elsewhere
        pass


def main() -> None:
    parser = argparse.ArgumentParser(description="Train MobileNetV3-Small for LeafRust")
    parser.add_argument("--data", required=True, help="Path to ImageFolder dataset")
    parser.add_argument(
        "--out",
        default="../android/app/src/main/assets/models/plantvillage_mobilenet.tflite",
        help="Output .tflite path",
    )
    parser.add_argument("--epochs", type=int, default=8)
    parser.add_argument("--batch", type=int, default=32)
    parser.add_argument("--image-size", type=int, default=224)
    parser.add_argument(
        "--finetune",
        action="store_true",
        help="Unfreeze top MobileNet layers for fine-tuning",
    )
    parser.add_argument(
        "--checkpoint",
        type=Path,
        default=None,
        help="Optional .keras checkpoint to continue from",
    )
    parser.add_argument(
        "--checkpoint-dir",
        type=Path,
        default=Path("../data/checkpoints"),
        help="Where to save best.keras",
    )
    args = parser.parse_args()

    data_dir = Path(args.data)
    if not data_dir.exists():
        raise SystemExit(f"Dataset not found: {data_dir}")

    # Drop empty class folders (e.g. Background without images)
    nonempty = [p for p in data_dir.iterdir() if p.is_dir() and any(p.iterdir())]
    if len(nonempty) < 2:
        raise SystemExit("Need at least 2 non-empty class folders")

    train_ds = tf.keras.utils.image_dataset_from_directory(
        data_dir,
        validation_split=0.15,
        subset="training",
        seed=42,
        image_size=(args.image_size, args.image_size),
        batch_size=args.batch,
    )
    val_ds = tf.keras.utils.image_dataset_from_directory(
        data_dir,
        validation_split=0.15,
        subset="validation",
        seed=42,
        image_size=(args.image_size, args.image_size),
        batch_size=args.batch,
    )

    class_names = list(train_ds.class_names)
    out_path = Path(args.out)
    labels_json = out_path.with_suffix(".labels.json")
    labels_json.write_text(json.dumps(class_names, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Classes ({len(class_names)}) -> {labels_json}")
    if "Background" not in class_names:
        print(
            "WARNING: Background class missing — Android expects 39 labels including Background. "
            "Add photos to Background/ or keep the shipped labels.txt when exporting a compatible head."
        )

    autotune = tf.data.AUTOTUNE
    train_ds = train_ds.shuffle(1000).prefetch(autotune)
    val_ds = val_ds.prefetch(autotune)

    if args.checkpoint and args.checkpoint.is_file():
        print("Loading checkpoint", args.checkpoint)
        model = tf.keras.models.load_model(args.checkpoint)
    else:
        model = build_model(len(class_names), args.image_size)

    if args.finetune:
        unfreeze_for_finetune(model)
        lr = 1e-4
    else:
        lr = 1e-3

    model.compile(
        optimizer=tf.keras.optimizers.Adam(lr),
        loss="sparse_categorical_crossentropy",
        metrics=["accuracy"],
    )

    args.checkpoint_dir.mkdir(parents=True, exist_ok=True)
    ckpt_path = args.checkpoint_dir / "best.keras"
    callbacks = [
        tf.keras.callbacks.ModelCheckpoint(
            ckpt_path, monitor="val_accuracy", save_best_only=True, verbose=1
        ),
        tf.keras.callbacks.EarlyStopping(
            monitor="val_accuracy", patience=3, restore_best_weights=True
        ),
    ]

    model.fit(
        train_ds,
        validation_data=val_ds,
        epochs=args.epochs,
        callbacks=callbacks,
    )
    export_tflite(model, out_path)
    write_labels_txt(class_names, out_path)
    print(f"Best checkpoint: {ckpt_path}")
    print("Publish to the app repo CDN with: python publish_model.py")


if __name__ == "__main__":
    main()
