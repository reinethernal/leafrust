"""
Train a lightweight MobileNetV3-Small classifier on PlantVillage-style folders
and export TensorFlow Lite weights for LeafRust.

Expected data layout (ImageFolder):
  dataset/
    Apple___Apple_scab/
    Apple___healthy/
    Tomato___Late_blight/
    ...

Usage:
  pip install -r requirements-train.txt
  python train_mobilenet.py --data /path/to/PlantVillage --out ../android/app/src/main/assets/models/plantvillage_mobilenet.tflite
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


def export_tflite(model: tf.keras.Model, out_path: Path) -> None:
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    tflite_model = converter.convert()
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_bytes(tflite_model)
    print(f"Wrote {out_path} ({out_path.stat().st_size / 1024:.1f} KB)")


def main() -> None:
    parser = argparse.ArgumentParser(description="Train MobileNetV3-Small for LeafRust")
    parser.add_argument("--data", required=True, help="Path to ImageFolder dataset")
    parser.add_argument(
        "--out",
        default="../android/app/src/main/assets/models/plantvillage_mobilenet.tflite",
        help="Output .tflite path",
    )
    parser.add_argument("--epochs", type=int, default=5)
    parser.add_argument("--batch", type=int, default=32)
    parser.add_argument("--image-size", type=int, default=224)
    args = parser.parse_args()

    data_dir = Path(args.data)
    if not data_dir.exists():
        raise SystemExit(f"Dataset not found: {data_dir}")

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
    labels_path = Path(args.out).with_suffix(".labels.json")
    labels_path.write_text(json.dumps(class_names, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Classes ({len(class_names)}) -> {labels_path}")

    autotune = tf.data.AUTOTUNE
    train_ds = train_ds.shuffle(1000).prefetch(autotune)
    val_ds = val_ds.prefetch(autotune)

    model = build_model(len(class_names), args.image_size)
    model.compile(
        optimizer=tf.keras.optimizers.Adam(1e-3),
        loss="sparse_categorical_crossentropy",
        metrics=["accuracy"],
    )
    model.fit(train_ds, validation_data=val_ds, epochs=args.epochs)
    export_tflite(model, Path(args.out))


if __name__ == "__main__":
    main()
