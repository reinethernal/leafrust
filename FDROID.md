# Сторонний репозиторий F-Droid

## URL (скопируйте целиком одной строкой)

```
https://reinethernal.github.io/leafrust/docs/fdroid/repo?fingerprint=9feaf2fa0148a741d9a3beef5c3cb0f3fa8f1de13874cb9fea71ca27950d360e
```

Тот же текст: https://reinethernal.github.io/leafrust/docs/add-repo.txt  
QR: https://reinethernal.github.io/leafrust/docs/fdroid-repo-qr.png  
Лендинг: https://reinethernal.github.io/leafrust/docs/

## Если пишет «неверный отпечаток»

1. В F-Droid → Repositories **удалите** прошлую попытку (leafrust / LeafRust / github.io).
2. Добавьте репозиторий заново по URL выше (**без** пробелов и переносов).
3. Не подставляйте отпечаток HTTPS-сертификата сайта — нужен отпечаток **ключа индекса** (уже в URL).

Проверка индекса в браузере:  
https://reinethernal.github.io/leafrust/docs/fdroid/repo/index-v1.jar

## Обновление репо

```bash
python scripts/publish_fdroid_docs.py
git add docs .nojekyll && git commit -m "Update F-Droid repo" && git push
```
