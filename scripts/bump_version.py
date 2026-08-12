#!/usr/bin/env python3
"""Bump LeafRust Android versionCode + SemVer versionName (default: minor)."""

from __future__ import annotations

import argparse
import os
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
GRADLE = ROOT / "android" / "app" / "build.gradle.kts"
META = ROOT / "metadata" / "com.leafrust.yml"


def parse_name(name: str) -> tuple[int, int, int]:
    m = re.fullmatch(r"(\d+)\.(\d+)\.(\d+)", name.strip())
    if not m:
        raise SystemExit(f"Unsupported versionName: {name!r} (want MAJOR.MINOR.PATCH)")
    return int(m.group(1)), int(m.group(2)), int(m.group(3))


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--part",
        choices=("minor", "patch", "major"),
        default="minor",
        help="SemVer component to increment (default: minor → X.(Y+1).0)",
    )
    args = parser.parse_args()

    text = GRADLE.read_text(encoding="utf-8")
    code_m = re.search(r"versionCode\s*=\s*(\d+)", text)
    name_m = re.search(r'versionName\s*=\s*"([^"]+)"', text)
    if not code_m or not name_m:
        raise SystemExit("versionCode/versionName not found in build.gradle.kts")

    old_code = int(code_m.group(1))
    old_name = name_m.group(1)
    major, minor, patch = parse_name(old_name)
    if args.part == "major":
        new_name = f"{major + 1}.0.0"
    elif args.part == "patch":
        new_name = f"{major}.{minor}.{patch + 1}"
    else:
        new_name = f"{major}.{minor + 1}.0"
    new_code = old_code + 1

    text = re.sub(r"versionCode\s*=\s*\d+", f"versionCode = {new_code}", text, count=1)
    text = re.sub(
        r'versionName\s*=\s*"[^"]+"',
        f'versionName = "{new_name}"',
        text,
        count=1,
    )
    GRADLE.write_text(text, encoding="utf-8", newline="\n")

    if META.is_file():
        meta = META.read_text(encoding="utf-8")
        meta = re.sub(r"(?m)^CurrentVersion:\s*.*$", f"CurrentVersion: {new_name}", meta)
        meta = re.sub(
            r"(?m)^CurrentVersionCode:\s*.*$",
            f"CurrentVersionCode: {new_code}",
            meta,
        )
        META.write_text(meta, encoding="utf-8", newline="\n")

    print(f"version {old_name} ({old_code}) -> {new_name} ({new_code})")
    gh_out = os.environ.get("GITHUB_OUTPUT")
    if gh_out:
        with open(gh_out, "a", encoding="utf-8") as f:
            f.write(f"version_name={new_name}\n")
            f.write(f"version_code={new_code}\n")
            f.write(f"tag=v{new_name}\n")


if __name__ == "__main__":
    main()
