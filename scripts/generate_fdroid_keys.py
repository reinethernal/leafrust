#!/usr/bin/env python3
"""Generate a dedicated keystore for signing LeafRust APKs + F-Droid repo index."""
from __future__ import annotations

import base64
import hashlib
import secrets
import string
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / ".secrets"
ALIAS = "leafrust"
JKS = OUT / "leafrust-fdroid.jks"


def find_keytool() -> str:
    candidates = [
        ROOT / ".build-tools" / "jdk-17" / "bin" / ("keytool.exe" if sys.platform == "win32" else "keytool"),
        Path("keytool"),
    ]
    for c in candidates:
        if c == Path("keytool"):
            return "keytool"
        if c.is_file():
            return str(c)
    raise SystemExit("keytool not found; install JDK 17")


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    if JKS.exists():
        print(f"Already exists: {JKS}")
        print("Delete it first if you want to rotate keys (will change fingerprint).")
        return

    alphabet = string.ascii_letters + string.digits
    password = "".join(secrets.choice(alphabet) for _ in range(24))
    (OUT / "keystore.password.txt").write_text(password, encoding="utf-8")

    keytool = find_keytool()
    subprocess.check_call(
        [
            keytool,
            "-genkeypair",
            "-v",
            "-keystore",
            str(JKS),
            "-alias",
            ALIAS,
            "-keyalg",
            "RSA",
            "-keysize",
            "2048",
            "-validity",
            "10000",
            "-storepass",
            password,
            "-keypass",
            password,
            "-dname",
            "CN=LeafRust F-Droid Repo, OU=LeafRust, O=reinethernal, L=Internet, ST=NA, C=RU",
        ]
    )

    b64 = base64.b64encode(JKS.read_bytes()).decode("ascii")
    (OUT / "leafrust-fdroid.jks.base64.txt").write_text(b64, encoding="ascii")

    der = subprocess.check_output(
        [
            keytool,
            "-exportcert",
            "-keystore",
            str(JKS),
            "-alias",
            ALIAS,
            "-storepass",
            password,
        ],
        stderr=subprocess.DEVNULL,
    )
    fp = hashlib.sha256(der).hexdigest().lower()
    (OUT / "fingerprint.txt").write_text(fp, encoding="ascii")
    print("Created", JKS)
    print("Fingerprint:", fp.upper())
    print("Next: python scripts/print_fdroid_secrets.py")


if __name__ == "__main__":
    main()
