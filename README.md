# LeafRust

Нативное Android-приложение на **Kotlin + Jetpack Compose**: фото листа → локальный ИИ (болезнь + % поражения) → таблица осмотров → отправка результатов.

## Возможности

- Съёмка / галерея с подгонкой листа в рамку
- Автоопределение: культуры, ягоды, деревья, кустарники, цветы, травы, тропические, комнатные, суккуленты, папоротники, лианы
- **Офлайн-база** растений и болезней (симптомы + лечение) в SQLite
- Оценка % поражения, история осмотров, Share / CSV

## Структура

```
leaf/
  android/     # основное приложение (Kotlin)
  mobile/      # предыдущий Expo-прототип (архив)
  scripts/     # обучение модели + сборка базы знаний
```

## Сборка

Нужны **Android Studio Ladybug+**, JDK 17 и Android SDK.

1. (Опционально) пересобрать базу знаний:
   ```bash
   python scripts/build_plant_kb.py
   # → android/app/src/main/assets/kb/plants_diseases.sqlite
   ```
2. Откройте папку `android/` в Android Studio → Sync → Run.

Из терминала:

```bash
cd android
./gradlew :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

## База знаний

Файл `android/app/src/main/assets/kb/plants_diseases.sqlite` содержит:
- растения и категории;
- типы болезней;
- связки растение–болезнь с симптомами и лечением (RU);
- `class_map` для лейблов PlantVillage и ornamental-ключей.

При анализе тексты симптомов/лечения подставляются из базы по `classId`.

## Модель

Основа: [PlantAi](https://github.com/Nishant1998/PlantAi) / PlantVillage (**39 классов**, включая Background), TFLite.

- В APK веса лежат в assets (офлайн).
- Обновления качаются с CDN репозитория:  
  `https://reinethernal.github.io/leafrust/docs/models/`  
  (манифест `model_manifest.json` + `.tflite`). Кнопка **Ещё → Обновить модель**.

Датасет и дообучение: [`scripts/DATASETS.md`](scripts/DATASETS.md)

```bash
cd scripts
pip install -r requirements-train.txt
python download_plantvillage.py --out ../data/plantvillage
python train_mobilenet.py --data ../data/plantvillage --epochs 8 \
  --out ../android/app/src/main/assets/models/plantvillage_mobilenet.tflite
python publish_model.py
git add docs/models android/app/src/main/assets/models && git commit -m "Update model" && git push
```

## Разрешения

- Камера — съёмка листа
- Галерея — выбор фото
- Интернет — автоскачивание модели

## F-Droid

### Сторонний репозиторий (добавить в приложение F-Droid)

После публикации через GitHub Actions:

```
https://reinethernal.github.io/leafrust/docs/fdroid/repo?fingerprint=9FEAF2FA0148A741D9A3BEEF5C3CB0F3FA8F1DE13874CB9FEA71CA27950D360E
```

Настройка секретов и Pages: [`FDROID.md`](FDROID.md).

### Официальный каталог

Шаблон для fdroiddata: [`metadata/com.leafrust.yml`](metadata/com.leafrust.yml).  
Лицензия: [`LICENSE`](LICENSE), [`NOTICE`](NOTICE).

```bash
cd android
./gradlew :app:assembleRelease
```


