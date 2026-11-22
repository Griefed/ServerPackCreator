#!/usr/bin/env python3
"""Fail when an image pinned by digest is no longer served by its registry.

A digest pin outlives its tag: a registry that drops the manifest turns every build or `compose up` of the
pinned file into a pull failure, and nothing in a normal build notices until then. This asks the registry
about each `<image>:<tag>@sha256:<digest>` in the given files (default: every Dockerfile and compose file
that pins one) and reports the ones it does not answer for.

Usage: python3 misc/pinned-digests-check.py [--inspector CMD] [file ...]
       --inspector replaces `docker manifest inspect` (a client-side registry call; no daemon needed).
"""

import re
import subprocess
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
DEFAULT_FILES = [
    "docker/Dockerfile",
    "docker/docker-compose-dev.yml",
    "docker/docker-compose.yml",
    "serverpackcreator-grinder/docker/Dockerfile",
    "serverpackcreator-homepage/Dockerfile",
]
PINNED = re.compile(r"([a-z0-9][a-z0-9._/-]*(?::[A-Za-z0-9._-]+)?@sha256:[0-9a-f]{64})")


def pinned_references(text: str) -> list:
    """Every digest-pinned image reference in the text, in order, duplicates kept once."""
    seen = []
    for reference in PINNED.findall(text):
        if reference not in seen:
            seen.append(reference)
    return seen


def by_digest(reference: str) -> str:
    """The reference without its tag. Given `name:tag@digest`, `docker manifest inspect` resolves the TAG and
    then fails verification whenever the tag has moved on, which says nothing about the digest itself."""
    name, digest = reference.split("@", 1)
    head, _, last = name.rpartition("/")
    return f"{head + '/' if head else ''}{last.split(':', 1)[0]}@{digest}"


def main() -> int:
    args = sys.argv[1:]
    inspector = ["docker", "manifest", "inspect"]
    if "--inspector" in args:
        at = args.index("--inspector")
        inspector = [args[at + 1]]
        del args[at:at + 2]
    files = [Path(f) for f in args] or [REPO / f for f in DEFAULT_FILES]
    references = []
    for file in files:
        for reference in pinned_references(file.read_text()):
            if reference not in references:
                references.append(reference)
    missing = []
    for reference in references:
        result = subprocess.run(inspector + [by_digest(reference)], capture_output=True, text=True)
        if result.returncode != 0:
            missing.append(reference)
            print(f"MISSING {reference}\n    {(result.stderr or result.stdout).strip()[:300]}")
    if missing:
        print(f"\n{len(missing)} of {len(references)} pinned digests are not served. Re-pin them from the "
              f"registry index: `docker buildx imagetools inspect <image>:<tag>`.")
        return 1
    print(f"pinned-digests-check: {len(references)} pinned digests resolve")
    return 0


if __name__ == "__main__":
    sys.exit(main())
