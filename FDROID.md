# F-Droid packaging for LeafRust

Checklist and notes for publishing LeafRust on [F-Droid](https://f-droid.org/).

## What is already in the repo

| Item | Path |
|------|------|
| Apache-2.0 license | `LICENSE` |
| Third-party / privacy notice | `NOTICE` |
| Fastlane store metadata (en-US, ru-RU) | `fastlane/metadata/android/` |
| fdroiddata recipe template | `metadata/com.leafrust.yml` |
| Unsigned-friendly release build | `android/app/build.gradle.kts` |
| Bundled TFLite model + labels | `android/app/src/main/assets/models/` |
| Offline plant KB (SQLite) | `android/app/src/main/assets/kb/` |

App id: `com.leafrust` · versionName `1.0.0` · versionCode `1`

## Before you submit

1. **Public Git repo** — https://github.com/reinethernal/leafrust (already set in `metadata/com.leafrust.yml`).
2. **Tag a release**: `git tag -a v1.0.0 -m "1.0.0" && git push --tags`
3. **Screenshots** — add portrait PNGs under:
   - `fastlane/metadata/android/en-US/images/phoneScreenshots/`
   - (optional) `fastlane/metadata/android/ru-RU/images/phoneScreenshots/`
4. **Optional icon for the listing** — `fastlane/metadata/android/en-US/images/icon.png` (512×512).

## Local unsigned release (same as F-Droid)

```bash
cd android
./gradlew :app:clean :app:assembleRelease
# APK: app/build/outputs/apk/release/app-release-unsigned.apk
```

Linux/macOS need the Unix `gradlew` script (committed next to `gradlew.bat`). F-Droid runs on Linux and invokes `./gradlew`.

Do **not** set `LEAFRUST_STORE_*` when checking F-Droid compatibility — the CI build must stay unsigned so F-Droid can sign it.

Optional local signing (not used by F-Droid):

```bash
export LEAFRUST_STORE_FILE=/path/to.keystore
export LEAFRUST_STORE_PASSWORD=...
export LEAFRUST_KEY_ALIAS=...
export LEAFRUST_KEY_PASSWORD=...
./gradlew :app:assembleRelease
```

## Submit to fdroiddata

1. Fork [fdroiddata](https://gitlab.com/fdroid/fdroiddata).
2. Copy `metadata/com.leafrust.yml` into that fork (same path).
3. Fix SourceCode / Repo URLs and `commit: v1.0.0` (or the exact commit hash).
4. Open a merge request following [Inclusion How-To](https://f-droid.org/docs/Inclusion_How-To/).
5. Expect review for:
   - reproducible / source build from `subdir: android`
   - `AntiFeatures: NonFreeAssets` (prebuilt `.tflite` weights)
   - INTERNET only for optional model download (weights are also bundled)

## AntiFeatures

- **NonFreeAssets** — PlantVillage-derived TFLite weights are shipped as a binary blob and are not rebuilt in the F-Droid recipe. Documented in `NOTICE` and `android/app/src/main/assets/models/README.md`.

Do not add proprietary SDKs (Play Services, Crashlytics, ads, analytics).

## Build recipe notes

- Gradle project root for F-Droid: `android/` (`subdir: android`).
- Output: `app/build/outputs/apk/release/app-release-unsigned.apk`
- `mobile/` (Expo archive) and `.build-tools/` are removed in the recipe so the scanner does not pick up unrelated JS deps or local SDK installs.
- Fastlane metadata at the **git root** (not under `android/`) is the layout F-Droid expects when the recipe uses the default metadata path relative to the repo.

## Privacy summary (for reviewers)

No accounts, ads, or analytics. Photos and inspection history stay on-device unless the user shares them. Network is used only to optionally refresh the model.
