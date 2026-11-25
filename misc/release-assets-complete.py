#!/usr/bin/env python3
"""Exit 0 when a mirrored release carries every asset of the canonical one, printing how many; else exit 1.

Usage: release-assets-complete.py <canonical-release.json> <mirror-release.json>

Both arguments are release objects as Forgejo and GitHub return them (`{"assets": [{"name": ...}]}`). The
expected set is the canonical release's, so nothing here hard-codes how many assets a release has. A
canonical release with no assets fails too: it cannot vouch for anything.
"""

import json
import sys


def asset_names(path: str) -> set:
    with open(path, encoding="utf-8") as handle:
        return {asset["name"] for asset in json.load(handle).get("assets") or []}


def main() -> int:
    expected = asset_names(sys.argv[1])
    present = asset_names(sys.argv[2])
    if not expected:
        print("The canonical release lists no assets, so there is nothing to compare against.", file=sys.stderr)
        return 1
    missing = sorted(expected - present)
    if missing:
        print(f"The mirrored release lacks {len(missing)} of {len(expected)} assets: {', '.join(missing)}",
              file=sys.stderr)
        return 1
    print(len(expected))
    return 0


if __name__ == "__main__":
    sys.exit(main())
