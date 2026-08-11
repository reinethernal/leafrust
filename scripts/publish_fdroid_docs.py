#!/usr/bin/env python3
"""Build signed APK + F-Droid repo index into docs/ for GitHub Pages."""
from __future__ import annotations

import hashlib
import json
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

BINARY_SUFFIXES = {".apk", ".idsig", ".jar", ".png", ".jpg", ".jpeg", ".webp", ".gif"}


def to_lf_bytes(data: bytes) -> bytes:
    return data.replace(b"\r\n", b"\n").replace(b"\r", b"\n")


def normalize_repo_lf(repo: Path) -> None:
    """Force LF endings so GitHub Pages hash matches entry.json (Windows CRLF break)."""
    for path in repo.rglob("*"):
        if not path.is_file() or path.suffix.lower() in BINARY_SUFFIXES:
            continue
        data = path.read_bytes()
        lf = to_lf_bytes(data)
        if lf != data:
            path.write_bytes(lf)
            print("LF normalized", path.name)


def write_jar(jar_path: Path, inner_name: str, payload: bytes) -> None:
    jar_path.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(jar_path, "w", compression=zipfile.ZIP_DEFLATED) as zf:
        # ZIP stores dates; keep deterministic-ish content
        info = zipfile.ZipInfo(inner_name)
        info.compress_type = zipfile.ZIP_DEFLATED
        zf.writestr(info, payload)


def jarsign(jar: Path, jks: Path, alias: str, password: str) -> None:
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
            str(jar),
            alias,
        ]
    )
    print("signed", jar.name)


def rebuild_signed_indexes(repo: Path, jks: Path, alias: str, password: str) -> None:
    """Rebuild index JARs from LF-normalized loose files and sign with SHA-256."""
    normalize_repo_lf(repo)

    v2_path = repo / "index-v2.json"
    v2 = v2_path.read_bytes()
    v2_hash = hashlib.sha256(v2).hexdigest()
    entry = {
        "timestamp": json.loads(v2.decode("utf-8"))["repo"]["timestamp"],
        "version": 20002,
        "index": {
            "name": "/index-v2.json",
            "sha256": v2_hash,
            "size": len(v2),
            "numPackages": 1,
        },
        "diffs": {},
    }
    # Match fdroid pretty JSON style (2-space indent, trailing newline)
    entry_bytes = (json.dumps(entry, indent=2) + "\n").encode("utf-8")
    (repo / "entry.json").write_bytes(entry_bytes)
    print("entry.json hash ->", v2_hash, "size", len(v2))

    # index-v1.json may exist — keep LF and pack into jar
    v1_path = repo / "index-v1.json"
    if v1_path.is_file():
        write_jar(repo / "index-v1.jar", "index-v1.json", v1_path.read_bytes())
        jarsign(repo / "index-v1.jar", jks, alias, password)

    # legacy index.jar from index.xml if present
    xml_path = repo / "index.xml"
    if xml_path.is_file():
        write_jar(repo / "index.jar", "index.xml", xml_path.read_bytes())
        jarsign(repo / "index.jar", jks, alias, password)

    write_jar(repo / "entry.jar", "entry.json", entry_bytes)
    jarsign(repo / "entry.jar", jks, alias, password)


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

    (work / "config.yml").write_text(
        "\n".join(
            [
                "repo_url: https://reinethernal.github.io/leafrust/docs/fdroid/repo",
                "repo_name: LeafRust",
                "repo_description: Official LeafRust third-party F-Droid repository",
                "archive_older: 0",
                f"sdk_path: {(ROOT / '.build-tools/android-sdk').as_posix()}",
                f"apksigner: {APKSIGNER.as_posix()}",
                f"keystore: {JKS.as_posix()}",
                f"repo_keyalias: {ALIAS}",
                f"keystorepass: {password}",
                f"keypass: {password}",
                "",
            ]
        ),
        encoding="utf-8",
        newline="\n",
    )

    env = os.environ.copy()
    scripts = Path(os.environ["APPDATA"]) / "Python/Python310/Scripts"
    env["PATH"] = str(scripts) + os.pathsep + env.get("PATH", "")
    subprocess.check_call(
        ["fdroid", "update", "--create-metadata", "--pretty", "--delete-unknown"],
        cwd=str(work),
        env=env,
    )

    rebuild_signed_indexes(repo, JKS, ALIAS, password)

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
    fp = hashlib.sha256(der).hexdigest().lower()
    print("FINGERPRINT", fp)

    # Verify local consistency before publish
    v2 = (repo / "index-v2.json").read_bytes()
    entry = json.loads((repo / "entry.json").read_text(encoding="utf-8"))
    got = hashlib.sha256(v2).hexdigest()
    if got != entry["index"]["sha256"] or len(v2) != entry["index"]["size"]:
        raise SystemExit(f"hash mismatch before publish: {got} vs {entry['index']}")
    if b"\r" in v2:
        raise SystemExit("index-v2.json still has CR — abort")

    docs = ROOT / "docs"
    if docs.exists():
        shutil.rmtree(docs)
    docs.mkdir()
    (docs / ".nojekyll").write_bytes(b"")
    (ROOT / ".nojekyll").write_bytes(b"")
    shutil.copytree(repo, docs / "fdroid" / "repo")
    html = (ROOT / "fdroid/site-index.html").read_text(encoding="utf-8").replace(
        "@FINGERPRINT@", fp
    )
    (docs / "index.html").write_bytes(html.replace("\r\n", "\n").encode("utf-8"))
    (docs / "add-repo.txt").write_bytes(
        f"https://reinethernal.github.io/leafrust/docs/fdroid/repo?fingerprint={fp}\n".encode(
            "ascii"
        )
    )
    (SECRETS / "fingerprint.txt").write_bytes(fp.encode("ascii") + b"\n")

    print("Published to", docs)
    for p in sorted(docs.rglob("*")):
        if p.is_file():
            print(f"  {p.relative_to(docs)} ({p.stat().st_size} bytes)")


if __name__ == "__main__":
    main()
