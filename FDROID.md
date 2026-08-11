# Сторонний репозиторий F-Droid

## URL для клиента (рабочий)

```
https://reinethernal.github.io/leafrust/docs/fdroid/repo?fingerprint=9FEAF2FA0148A741D9A3BEEF5C3CB0F3FA8F1DE13874CB9FEA71CA27950D360E
```

Проверка в браузере (должно быть **не** 404):  
https://reinethernal.github.io/leafrust/docs/fdroid/repo/index-v1.jar

## Почему был NotFoundException

Pages отдавал корень `main` (README), а индекса `/fdroid/repo/` не было.  
Сейчас индекс лежит в `docs/fdroid/repo/` → в URL нужен сегмент **`/docs/`**.

## Опционально: короткий URL без /docs

Settings → Pages → Branch **main** → folder **/docs** → Save.  
Тогда можно будет использовать  
`https://reinethernal.github.io/leafrust/fdroid/repo?fingerprint=...`  
(после смены папки нужно снова поправить `repo_url` и пересобрать индекс).

## Обновление

```bash
python scripts/publish_fdroid_docs.py
git add docs && git commit -m "Update F-Droid repo" && git push
```
