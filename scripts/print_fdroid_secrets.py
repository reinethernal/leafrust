#!/usr/bin/env python3
"""Print GitHub Actions secret values from .secrets/ (local only, never commit)."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
sec = root / ".secrets"
jks = sec / "leafrust-fdroid.jks"
b64_path = sec / "leafrust-fdroid.jks.base64.txt"
pass_path = sec / "keystore.password.txt"
fp_path = sec / "fingerprint.txt"

if not jks.is_file() or not pass_path.is_file():
    raise SystemExit(
        "Missing .secrets/leafrust-fdroid.jks — generate with scripts/generate_fdroid_keys.py first."
    )

b64 = b64_path.read_text(encoding="ascii").strip() if b64_path.is_file() else None
if not b64:
    import base64

    b64 = base64.b64encode(jks.read_bytes()).decode("ascii")
    b64_path.write_text(b64, encoding="ascii")

password = pass_path.read_text(encoding="utf-8").strip()
fp = fp_path.read_text(encoding="ascii").strip().upper() if fp_path.is_file() else "(run generate script)"

print("Add these as GitHub -> Settings -> Secrets and variables -> Actions:\n")
print("FDROID_KEYSTORE_BASE64=")
print(b64)
print()
print(f"FDROID_KEYSTORE_PASSWORD={password}")
print("FDROID_KEY_ALIAS=leafrust")
print(f"FDROID_KEY_PASSWORD={password}")
print()
print("After Pages deploy, add this URL in the F-Droid app:")
print(f"https://reinethernal.github.io/leafrust/fdroid/repo?fingerprint={fp}")
