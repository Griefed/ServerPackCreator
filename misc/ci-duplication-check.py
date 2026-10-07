#!/usr/bin/env python3
"""Fail when a step is written twice in `.forgejo/workflows/`.

The rule it enforces: an artifact has exactly one definition, and `devbuild` and `release-build`
differ only in the values they pass it. The rule exists because the alternative has already cost a
release -- `release-build.yml`'s Windows job passed `-JarPath` while `devbuild.yml`'s passed nothing,
so the broken one of the two was the one nobody ever ran before the tag.

It compares *normalised* step bodies, not text: comments and blank lines go, trailing whitespace goes,
and a `uses:` step is compared together with its `with:` block. Anything appearing more than once is
reported unless `ci-duplication-allowlist.yml` carries its fingerprint and a reason.

The fingerprint is a hash of the normalised body, so editing an allowlisted step turns this red again
and the exemption has to be re-argued. That is deliberate: these are the steps a release depends on.

Usage: python3 misc/ci-duplication-check.py [--list] [workflow-dir]
       --list prints every fingerprint with its body, which is how allowlist entries get written.
"""

import hashlib
import sys
from pathlib import Path
from typing import Optional

import yaml

# A bare `uses:` with no `with:` -- `actions/checkout@v6` -- is one atomic reference with no body to
# share, so repeating it is not duplication in the sense this check means.
SKIP_BARE_USES = True


def normalise_run(body: str) -> str:
    """Strip comments, blank lines and trailing whitespace from a `run:` block.

    Two steps that differ only in their prose are the same step; this is what lets the check see
    through the comment blocks that sit inside these workflows' scripts.
    """
    lines = []
    for raw in body.splitlines():
        line = raw.rstrip()
        stripped = line.strip()
        if not stripped or stripped.startswith("#"):
            continue
        lines.append(line)
    return "\n".join(lines)


def step_body(step: dict) -> Optional[str]:
    """Render one step as the text that decides whether it duplicates another.

    A `run:` step is its normalised script. A `uses:` step is its reference plus its sorted `with:`
    entries, because the same action with different inputs is a different step. Returns None for
    steps with nothing shareable.
    """
    if "run" in step:
        shell = step.get("shell", "")
        return f"run[{shell}]\n{normalise_run(str(step['run']))}"
    if "uses" in step:
        with_block = step.get("with") or {}
        if SKIP_BARE_USES and not with_block:
            return None
        rendered = "\n".join(
            f"  {key}: {normalise_run(str(value))}" for key, value in sorted(with_block.items())
        )
        return f"uses {step['uses']}\nwith:\n{rendered}"
    return None


def fingerprint(body: str) -> str:
    """Twelve hex characters of the body's SHA-256 -- short enough to paste into the allowlist."""
    return hashlib.sha256(body.encode("utf-8")).hexdigest()[:12]


def collect(workflow_dir: Path) -> dict:
    """Walk every workflow and index each shareable body by fingerprint.

    Job-level `if:` expressions are indexed too: the alpha/beta create-guard is repeated verbatim
    across six jobs and is exactly the kind of copy this check exists to surface.
    """
    found = {}

    def record(body: str, where: str) -> None:
        entry = found.setdefault(fingerprint(body), {"body": body, "where": []})
        entry["where"].append(where)

    for path in sorted(workflow_dir.glob("*.yml")):
        document = yaml.safe_load(path.read_text(encoding="utf-8")) or {}
        for job_id, job in (document.get("jobs") or {}).items():
            if not isinstance(job, dict):
                continue
            if "if" in job:
                record(f"job-if\n{normalise_run(str(job['if']))}", f"{path.name}:{job_id} (if)")
            for index, step in enumerate(job.get("steps") or []):
                if not isinstance(step, dict):
                    continue
                body = step_body(step)
                if body is None:
                    continue
                label = step.get("name") or step.get("uses") or "run"
                record(body, f"{path.name}:{job_id} step {index} ({label})")

    return found


def load_allowlist(path: Path) -> dict:
    """Read fingerprint -> reason. A missing file means nothing is exempt, which is the right default."""
    if not path.exists():
        return {}
    return yaml.safe_load(path.read_text(encoding="utf-8")) or {}


def main() -> int:
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    listing = "--list" in sys.argv[1:]
    workflow_dir = Path(args[0]) if args else Path(".forgejo/workflows")
    allowlist = load_allowlist(Path(__file__).with_name("ci-duplication-allowlist.yml"))

    found = collect(workflow_dir)

    if listing:
        for key, entry in sorted(found.items(), key=lambda kv: -len(kv[1]["where"])):
            print(f"--- {key}  x{len(entry['where'])}")
            for where in entry["where"]:
                print(f"      {where}")
            print("    " + entry["body"].replace("\n", "\n    "))
        return 0

    offenders = {
        key: entry
        for key, entry in found.items()
        if len(entry["where"]) > 1 and key not in allowlist
    }

    stale = sorted(set(allowlist) - set(found))

    for key, entry in sorted(offenders.items(), key=lambda kv: -len(kv[1]["where"])):
        print(f"DUPLICATED x{len(entry['where'])}  fingerprint {key}")
        for where in entry["where"]:
            print(f"    {where}")
        first = entry["body"].splitlines()[:6]
        for line in first:
            print(f"    | {line}")
        if len(entry["body"].splitlines()) > 6:
            print("    | ...")
        print()

    for key in stale:
        print(f"STALE ALLOWLIST ENTRY {key} -- {allowlist[key]}")
        print("    Nothing in the workflows has this fingerprint any more. Remove it.")
        print()

    exempt = len(set(allowlist) & set(found))
    print(
        f"{len(found)} distinct step bodies, {len(offenders)} duplicated and unexplained, "
        f"{exempt} allowlisted."
    )
    if offenders or stale:
        print("Run with --list to see full bodies, then either share the step or explain the repeat")
        print("in misc/ci-duplication-allowlist.yml.")
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
