# Сторонний репозиторий F-Droid

## URL для клиента

```
https://reinethernal.github.io/leafrust/fdroid/repo?fingerprint=9FEAF2FA0148A741D9A3BEEF5C3CB0F3FA8F1DE13874CB9FEA71CA27950D360E
```

Индекс лежит в `docs/fdroid/repo/` (публикуется на GitHub Pages).

## Pages (обязательно)

https://github.com/reinethernal/leafrust/settings/pages

- Source: **Deploy from a branch**
- Branch: **main**
- Folder: **/docs** → Save

Пока выбран корень `main` (без `/docs`), F-Droid получит 404 (`NotFoundException`).

## Проверка в браузере

Должны открываться (не 404):

- https://reinethernal.github.io/leafrust/fdroid/repo/index.xml
- https://reinethernal.github.io/leafrust/fdroid/repo/index-v1.jar

## Обновление репо

Локально (после `assembleRelease`):

```bash
python scripts/publish_fdroid_docs.py
git add docs && git commit -m "Update F-Droid repo" && git push
```

Или Actions → **F-Droid third-party repo** (нужны secrets из `scripts/print_fdroid_secrets.py`).
