#!/usr/bin/env python3
"""Verify every launcher manifest Class-Path entry exists in the desktop bundle.

The packaged application JAR starts through its manifest ``Class-Path``, so a
runtime JAR that is referenced but missing from the distribution ZIP produces a
``NoClassDefFoundError`` only once a user launches the app. Checking the two
lists against each other in CI turns that into a build failure instead.

Usage:
    verify_bundle_manifest.py <manifest-file> <zip-listing-file>

``manifest-file``    raw ``META-INF/MANIFEST.MF`` extracted from the app JAR.
``zip-listing-file`` output of ``unzip -l`` for the desktop bundle ZIP.
"""

from __future__ import annotations

import sys
from pathlib import Path


def unfold(manifest_text: str) -> list[str]:
    """Join JAR-manifest continuation lines (those starting with a space)."""
    lines: list[str] = []
    for line in manifest_text.replace("\r\n", "\n").split("\n"):
        if line.startswith(" ") and lines:
            lines[-1] += line[1:]
        else:
            lines.append(line)
    return lines


def class_path_entries(manifest_text: str) -> list[str]:
    for line in unfold(manifest_text):
        if line.startswith("Class-Path:"):
            return line[len("Class-Path:") :].split()
    return []


def main(argv: list[str]) -> int:
    if len(argv) != 3:
        print(f"usage: {Path(argv[0]).name} <manifest-file> <zip-listing-file>")
        return 2

    manifest_text = Path(argv[1]).read_text(encoding="utf-8", errors="replace")
    listing = Path(argv[2]).read_text(encoding="utf-8", errors="replace")

    entries = class_path_entries(manifest_text)
    if not entries:
        print("::error::Launcher manifest has no Class-Path entry.")
        return 1

    missing = [entry for entry in entries if entry not in listing]
    if missing:
        for entry in missing:
            print(f"::error::Manifest references a JAR absent from the bundle: {entry}")
        return 1

    print(f"All {len(entries)} manifest Class-Path entries are present in the bundle.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
