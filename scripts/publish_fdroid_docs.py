#!/usr/bin/env python3
"""Build signed APK + F-Droid repo index into docs/ for GitHub Pages."""
from __future__ import annotations

import hashlib
import os
import shutil
import subprocess
import tempfile
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SECRETS = ROOT / ".secrets"
JKS = SECRETS / "leafrust-fdroid.jks"
ALIAS = "leafrust"
UNSIGNED = ROOT / "android/app/build/outputs/apk/release/app-release-unsigned.apk"
APKSIGNER = ROOT / ".build-tools/android-sdk/build-tools/35.0.0/apksigner.bat"


def resign_index_jars(repo: Path, jks: Path, alias: str, password: str) -> None:
    """Re-sign index JARs with SHA-256 (fdroidserver still uses SHA1 for v1 jars)."""
    for name in ("index.jar", "index-v1.jar", "entry.jar"):
        jar = repo / name
        if not jar.is_file():
            continue
        # Drop old signature block so jarsigner can re-sign cleanly.
        with tempfile.TemporaryDirectory() as tmp:
            tmp_path = Path(tmp)
            unsigned = tmp_path / name
            with zipfile.ZipFile(jar, "r") as zin, zipfile.ZipFile(
                unsigned, "w", compression=zipfile.ZIP_DEFLATED
            ) as zout:
                for item in zin.infolist():
                    if item.filename.startswith("META-INF/"):
                        continue
                    zout.writestr(item, zin.read(item.filename))
            subprocess.check_call(
                [
                    "jarsigner",
                    "-keystore",
                    str(jks),
                    "-storepass",
                    password,
                    "-keypass",
                    password,
                    "-digestalg",
                    "SHA-256",
                    "-sigalg",
                    "SHA256withRSA",
                    "-signedjar",
                    str(jar),
                    str(unsigned),
                    alias,
                ]
            )
            print("re-signed", name)


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
                "repo_url: https://reinethernal.github.io/leafrust/docs/fdroid/repo",
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
    resign_index_jars(repo, JKS, ALIAS, password)

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
    (ROOT / ".nojekyll").write_text("", encoding="utf-8")
    shutil.copytree(repo, docs / "fdroid" / "repo")
    # Prefer lowercase fingerprint in URLs (F-Droid accepts both).
    fp_url = fp.lower()
    html = (ROOT / "fdroid/site-index.html").read_text(encoding="utf-8").replace(
        "@FINGERPRINT@", fp_url
    )
    (docs / "index.html").write_text(html, encoding="utf-8")
    (docs / "add-repo.txt").write_text(
        f"https://reinethernal.github.io/leafrust/docs/fdroid/repo?fingerprint={fp_url}\n",
        encoding="ascii",
    )
    (SECRETS / "fingerprint.txt").write_text(fp_url, encoding="ascii")

    print("Published to", docs)
    for p in sorted(docs.rglob("*")):
        if p.is_file():
            print(f"  {p.relative_to(docs)} ({p.stat().st_size} bytes)")


if __name__ == "__main__":
    main()
