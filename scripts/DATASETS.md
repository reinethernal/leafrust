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

```bash
cd scripts
pip install -r requirements-train.txt
python download_plantvillage.py --out ../data/plantvillage
# опционально быстрее:  --max-per-class 200
```

Структура на выходе — ImageFolder:

```
data/plantvillage/
  Apple___Apple_scab/
  Tomato___healthy/
  Background/          # пустая; можно добавить «не лист»
  ...
```

### Обучить / дообучить

```bash
# с нуля (голова на ImageNet-backbone)
python train_mobilenet.py --data ../data/plantvillage --epochs 8 \
  --out ../android/app/src/main/assets/models/plantvillage_mobilenet.tflite

# дообучение (разморозка верхних слоёв)
python train_mobilenet.py --data ../data/plantvillage --epochs 6 --finetune \
  --checkpoint ../data/checkpoints/best.keras \
  --out ../android/app/src/main/assets/models/plantvillage_mobilenet.tflite
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
