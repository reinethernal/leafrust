# Сторонний репозиторий F-Droid (добавить в приложение)

GitHub с исходниками **нельзя** добавить в F-Droid → Repositories.  
Нужен **опубликованный индекс** (`index-v1.jar` + APK) по HTTPS.

После настройки CI URL будет таким:

```
https://reinethernal.github.io/leafrust/fdroid/repo?fingerprint=9FEAF2FA0148A741D9A3BEEF5C3CB0F3FA8F1DE13874CB9FEA71CA27950D360E
```

(отпечаток уже сгенерирован локально в `.secrets/`; тот же ключ нужно залить в GitHub Secrets.)

## Разовая настройка

1. **Pages:** GitHub → Settings → Pages → Source = **GitHub Actions**.
2. **Секреты:** Settings → Secrets and variables → Actions → New repository secret:

   | Secret | Откуда |
   |--------|--------|
   | `FDROID_KEYSTORE_BASE64` | вывод скрипта |
   | `FDROID_KEYSTORE_PASSWORD` | то же |
   | `FDROID_KEY_ALIAS` | `leafrust` |
   | `FDROID_KEY_PASSWORD` | то же, что password |

   Локально (ключи уже в `.secrets/`, в git не попадают):

   ```bash
   python scripts/print_fdroid_secrets.py
   ```

3. **Запуск:** Actions → **F-Droid third-party repo** → Run workflow  
   (или push тега `v*`).
4. Откройте https://reinethernal.github.io/leafrust/ — там кнопка/URL для клиента.
5. В F-Droid: Settings → Repositories → **+** → вставьте URL с `?fingerprint=...`.

## Что в репозитории

| Путь | Назначение |
|------|------------|
| `.github/workflows/fdroid-repo.yml` | Сборка signed APK + `fdroid update` → GitHub Pages |
| `fdroid/metadata/com.leafrust.yml` | Описание приложения в стороннем репо |
| `fdroid/site-index.html` | Лендинг с URL для добавления |
| `scripts/generate_fdroid_keys.py` | Создать keystore (если нужно заново) |
| `scripts/print_fdroid_secrets.py` | Печать значений для GitHub Secrets |
| `metadata/com.leafrust.yml` | Рецепт для **официального** fdroiddata (отдельно) |

## Важно про ключи

- Один keystore подписывает и APK, и индекс репозитория.
- Не коммитьте `.secrets/` и не ротируйте ключ без нужды — иначе у пользователей сменится fingerprint.
- Обновления в F-Droid-клиенте работают только пока APK подписан **тем же** ключом.

## Официальный каталог F-Droid

Стороннее репо ≠ попадание в f-droid.org. Для официального каталога нужен MR в  
[fdroiddata](https://gitlab.com/fdroid/fdroiddata) (файл `metadata/com.leafrust.yml` в корне этого проекта).
