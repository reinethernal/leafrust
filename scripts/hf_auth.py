#!/usr/bin/env python3
"""Load Hugging Face token from env or a file next to the training scripts."""

from __future__ import annotations

import os
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parent
REPO = SCRIPTS.parent

# Prefer scripts/ next to the notebook; also accept .secrets/
TOKEN_CANDIDATES = [
    SCRIPTS / "huggingface.token",
    SCRIPTS / ".hf_token",
    SCRIPTS / "hf_token.txt",
    REPO / ".secrets" / "huggingface.token",
    REPO / ".secrets" / "hf_token.txt",
]

_last_source: str | None = None


def describe_token_source() -> str:
    return _last_source or "(none)"


def load_hf_token(explicit: str | Path | None = None) -> str | None:
    """Return HF token or None. Sets HF_TOKEN / HUGGING_FACE_HUB_TOKEN in os.environ when found."""
    global _last_source

    if explicit:
        p = Path(explicit)
        if p.is_file():
            tok = _read_token_file(p)
            if tok:
                _last_source = str(p)
                _export(tok)
                return tok
        elif isinstance(explicit, str) and explicit.startswith(("hf_", "HF_")):
            _last_source = "explicit"
            _export(explicit.strip())
            return explicit.strip()

    for key in ("HF_TOKEN", "HUGGING_FACE_HUB_TOKEN", "HUGGINGFACE_TOKEN"):
        val = (os.environ.get(key) or "").strip()
        if val and not val.startswith("hf_xxx") and "YOUR_" not in val.upper():
            _last_source = f"env:{key}"
            _export(val)
            return val

    for path in TOKEN_CANDIDATES:
        if path.is_file():
            tok = _read_token_file(path)
            if tok:
                _last_source = str(path)
                _export(tok)
                return tok

    _last_source = None
    return None


def login_hf(token: str | None = None) -> str | None:
    """Apply token for huggingface_hub (and optional huggingface_hub.login)."""
    tok = token if token is not None else load_hf_token()
    if not tok:
        return None
    _export(tok)
    try:
        from huggingface_hub import login

        login(token=tok, add_to_git_credential=False)
    except Exception as exc:
        # Token still in env — hf_hub_download will pick it up
        print(f"huggingface_hub.login skipped: {exc}")
    return tok


def _read_token_file(path: Path) -> str | None:
    text = path.read_text(encoding="utf-8").strip()
    if not text:
        return None
    # Allow KEY=value or bare token; ignore comments
    for line in text.splitlines():
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        if "=" in line and not line.startswith("hf_"):
            _, _, rhs = line.partition("=")
            line = rhs.strip().strip('"').strip("'")
        if line and "YOUR_" not in line.upper() and "paste" not in line.lower():
            if set(line.replace("hf_", "")) <= set("Xx_") or "XXX" in line:
                continue
            return line
    return None


def _export(tok: str) -> None:
    os.environ["HF_TOKEN"] = tok
    os.environ["HUGGING_FACE_HUB_TOKEN"] = tok
