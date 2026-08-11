#!/usr/bin/env python3
"""Build signed APK + F-Droid repo index into docs/ for GitHub Pages."""
from __future__ import annotations

import hashlib
import os
import shutil
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SECRETS = ROOT / ".secrets"
JKS = SECRETS / "leafrust-fdroid.jks"
ALIAS = "leafrust"
UNSIGNED = ROOT / "android/app/build/outputs/apk/release/app-release-unsigned.apk"
APKSIGNER = ROOT / ".build-tools/android-sdk/build-tools/35.0.0/apksigner.bat"


def main() -> None:
    password = (SECRETS / "keystore.password.txt").read_text(encoding="utf-8").strip()
    if not JKS.is_file():
        raise SystemExit(f"Missing {JKS}")
    if not UNSIGNED.is_file():
        raise SystemExit(f"Missing {UNSIGNED} — build release APK first")

    work = Path(tempfile.mkdtemp(prefix="leafrust-fdroid-"))
    repo = work / "repo"
    meta = work / "metadata"
    repo.mkdir()
    meta.mkdir()
    shutil.copy2(ROOT / "fdroid/metadata/com.leafrust.yml", meta / "com.leafrust.yml")

    signed = repo / "com.leafrust.apk"
    subprocess.check_call(
        [
            str(APKSIGNER),
            "sign",
            "--ks",
            str(JKS),
            "--ks-key-alias",
            ALIAS,
            "--ks-pass",
            f"pass:{password}",
            "--key-pass",
            f"pass:{password}",
            "--out",
            str(signed),
            str(UNSIGNED),
        ]
    )
    subprocess.check_call([str(APKSIGNER), "verify", "--verbose", str(signed)])

    apksigner = APKSIGNER.as_posix()
    (work / "config.yml").write_text(
        "\n".join(
            [
                "repo_url: https://reinethernal.github.io/leafrust/fdroid/repo",
                "repo_name: LeafRust",
                "repo_description: Official LeafRust third-party F-Droid repository",
                "archive_older: 0",
                f"sdk_path: {(ROOT / '.build-tools/android-sdk').as_posix()}",
                f"apksigner: {apksigner}",
                f"keystore: {JKS.as_posix()}",
                f"repo_keyalias: {ALIAS}",
                f"keystorepass: {password}",
                f"keypass: {password}",
                "",
            ]
        ),
        encoding="utf-8",
    )

    env = os.environ.copy()
    scripts = Path(os.environ["APPDATA"]) / "Python/Python310/Scripts"
    env["PATH"] = str(scripts) + os.pathsep + env.get("PATH", "")
    subprocess.check_call(
        ["fdroid", "update", "--create-metadata", "--pretty", "--delete-unknown"],
        cwd=str(work),
        env=env,
    )

    der = subprocess.check_output(
        [
            "keytool",
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
    fp = hashlib.sha256(der).hexdigest().upper()
    print("FINGERPRINT", fp)

    docs = ROOT / "docs"
    if docs.exists():
        shutil.rmtree(docs)
    docs.mkdir()
    (docs / ".nojekyll").write_text("", encoding="utf-8")
    shutil.copytree(repo, docs / "fdroid" / "repo")
    html = (ROOT / "fdroid/site-index.html").read_text(encoding="utf-8").replace(
        "@FINGERPRINT@", fp
    )
    (docs / "index.html").write_text(html, encoding="utf-8")
    (SECRETS / "fingerprint.txt").write_text(fp.lower(), encoding="ascii")

    print("Published to", docs)
    for p in sorted(docs.rglob("*")):
        if p.is_file():
            print(f"  {p.relative_to(docs)} ({p.stat().st_size} bytes)")


if __name__ == "__main__":
    main()
