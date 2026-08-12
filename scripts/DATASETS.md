# Датасеты для дообучения LeafRust

## Рекомендуемый: PlantVillage (Mohanty et al.)

~54 000 фото листьев, 14 культур, 26 болезней. Совместим с текущими 39 классами приложения (плюс папка `Background`).

| Источник | Ссылка |
|----------|--------|
| **Hugging Face** (удобнее всего) | https://huggingface.co/datasets/mohanty/PlantVillage |
| GitHub (исходные папки) | https://github.com/spMohanty/PlantVillage-Dataset |
| Kaggle | https://www.kaggle.com/datasets/abdallahalidev/plantvillage-dataset |
| Статья | https://www.frontiersin.org/articles/10.3389/fpls.2016.01419 |

### Скачать и разложить под обучение (в этом репо)

`load_dataset("mohanty/PlantVillage")` отдаёт только пути — картинки в **`data.zip` (~2.1 GB)** на Hugging Face.

```bash
cd scripts
pip install -r requirements-train.txt
python download_plantvillage.py --out ../data/plantvillage
# быстрее для проверки:
python download_plantvillage.py --out ../data/plantvillage_small --max-per-class 50
```

Архив скачивается один раз в `data/plantvillage_cache/` (повторный запуск использует кэш).

Структура на выходе — ImageFolder:

```
data/plantvillage/
  Apple___Apple_scab/
  Tomato___healthy/
  Background/          # пустая; можно добавить «не лист»
  ...
```

### Обучить / дообучить (GPU, NVIDIA)

Пакеты и кэши Python перенесены на **X:** (junctions с C:). В новой сессии:

```powershell
. X:\CODE\leaf\scripts\use_x_python.ps1
```

### Jupyter на удалённом GPU-сервере

Ноутбук: `scripts/train_mobilenet.ipynb` — пути через cwd / `LEAFRUST_ROOT`, результат в `data/exports/`.

```bash
cd leafrust && source .venv-gpu/bin/activate
jupyter lab --ip 0.0.0.0 --port 8888 --no-browser
# затем scp data/exports/plantvillage_mobilenet.tflite labels.txt → android/.../assets/models/
```

На Windows официальный TensorFlow **без CUDA**. Для RTX 3060 Ti локально:

```powershell
cd X:\CODE\leaf
.\.venv-gpu\Scripts\Activate.ps1
cd scripts
python train_mobilenet_torch.py --data ../data/plantvillage `
  --out ../android/app/src/main/assets/models/plantvillage_mobilenet.tflite
```

По умолчанию (GPU, batch=64 + AMP):
1. **head** — только классификатор (backbone ImageNet заморожен)
2. **finetune** — разморозка последних блоков MobileNetV3 → ONNX → TFLite

Если не хватает VRAM:

```powershell
python train_mobilenet_torch.py --data ../data/plantvillage --batch 32
```

Установка venv с нуля (уже сделано на диске X:):

```powershell
python -m venv .venv-gpu
.\.venv-gpu\Scripts\Activate.ps1
pip install torch torchvision --index-url https://download.pytorch.org/whl/cu124
pip install -r requirements-train-gpu.txt
```

### Опубликовать модель в CDN репозитория

Приложение качает обновления с GitHub Pages:

`https://reinethernal.github.io/leafrust/docs/models/`

```bash
python publish_model.py
git add docs/models android/app/src/main/assets/models
git commit -m "Update on-device plant disease model"
git push
```

После пуша устройства подтянут веса при следующем запуске (если `version` в `model_manifest.json` выше) или по кнопке **Ещё → Обновить модель**.

## Дополнительно (не для замены 39 классов as-is)

- **PlantDoc** — фото «в поле», полезно для домена: https://github.com/pratikkayal/PlantDoc-Dataset  
- Свои фото: положите в те же папки `Культура___Болезнь/` и переобучите.

`data/` в git не коммитится (см. `.gitignore`).
