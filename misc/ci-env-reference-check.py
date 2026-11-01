#!/usr/bin/env python3
"""Fail when a step's `env:` block and its script disagree about a name.

The defect it pins, by its own shape: `release-build.yml`'s `news` step declared
`WEBHOOK_URL: ${{ secrets.NEWS_WEBHOOK_URL }}` while its script read `$NEWS_WEBHOOK_URL`. The two
names never met, `${NEWS_WEBHOOK_URL:-}` was unconditionally empty, and the guard that exists to
tolerate an unconfigured webhook swallowed every release announcement for twelve days -- GREEN, with
a log line that read like a deliberate configuration choice.

Nothing about that is specific to a webhook. An `env:` key is one string and the name a script reads
is another, and a shell resolves an undefined name to the empty string rather than failing, so the
mismatch has no symptom at all until somebody notices the thing that was supposed to happen did not.
So both directions are reported:

  UNUSED    a step declares an `env:` key its own `run:` script never mentions
  UNDEFINED a script reads a name that is neither declared, nor assigned in the script, nor
            provided by the runner

Each finding is keyed `<file>::<job>::<step>::<NAME>` and can be exempted in
`ci-env-reference-allowlist.yml` with a reason. The common honest exemption is a name consumed by a
TOOL rather than by the script -- `GH_TOKEN` read by the `gh` CLI, `JAVA_HOME` read by Gradle.

Usage: python3 misc/ci-env-reference-check.py [--list] [path ...]
       --list prints every finding as a ready-to-paste allowlist key.
"""

import re
import sys
from pathlib import Path

import yaml

REPO = Path(__file__).resolve().parent.parent
DEFAULT_PATHS = [REPO / ".forgejo" / "workflows", REPO / ".github" / "workflows", REPO / ".forgejo" / "actions"]
ALLOWLIST = Path(__file__).resolve().parent / "ci-env-reference-allowlist.yml"

# Names the runner puts in every step's environment. Prefixes rather than a list, because the
# `GITHUB_*` family alone is thirty-odd names and a new one appearing is not this check's business.
RUNTIME_PREFIXES = ("GITHUB_", "RUNNER_", "FORGEJO_", "ACT_", "INPUT_", "npm_")
RUNTIME_NAMES = {
    "CI", "HOME", "PATH", "PWD", "OLDPWD", "USER", "LOGNAME", "SHELL", "TERM", "TMPDIR", "TMP",
    "HOSTNAME", "LANG", "LC_ALL", "IFS", "RANDOM", "SECONDS", "LINENO", "BASH_SOURCE", "FUNCNAME",
    "OSTYPE", "HOSTTYPE", "MACHTYPE", "SHLVL", "PS1", "PS2", "EDITOR", "PAGER",
}

# `<<'DELIM'` quotes the heredoc, so the shell expands nothing inside it -- that body is python or
# awk source, and a `$name` in it is that language's, not a shell reference. `<<DELIM` unquoted does
# expand, so those bodies stay.
QUOTED_HEREDOC = re.compile(r"<<-?\s*(['\"])([A-Za-z_]\w*)\1(.*?)^\s*\2\s*$", re.S | re.M)
# A quoted heredoc is not shell, but its body is usually a program that reads the very `env:` keys the
# step declares -- which is this repository's standard way of keeping a value out of the shell's
# argument list. Those lookups are references, so the UNUSED half has to see them or it reports the
# correct pattern and stays blind to the defect.
EMBEDDED_ENV = re.compile(
    r"""(?:os\.environ(?:\.get)?\[?\(?|os\.getenv\(|ENV\[|System\.getenv\(|process\.env\.)"""
    r"""\s*['\"]?([A-Za-z_]\w*)""")
EXPRESSION = re.compile(r"\$\{\{.*?\}\}", re.S)
FULL_LINE_COMMENT = re.compile(r"^\s*#.*$", re.M)
TRAILING_COMMENT = re.compile(r"(?<=\s)#[^'\"]*$", re.M)
SINGLE_QUOTED = re.compile(r"'[^']*'", re.S)

BRACED_REF = re.compile(r"\$\{([A-Za-z_]\w*)[:#%/\[\}\-]")
BARE_REF = re.compile(r"\$([A-Za-z_]\w*)")
PWSH_REF = re.compile(r"\$env:([A-Za-z_]\w*)", re.I)

# Any `NAME=` that is not a comparison and not the tail of a flag -- so it catches `A=1`, the
# leading-assignment run `A=1 B=2 cmd`, and the two forms that an anchored pattern misses and that
# this repository actually writes: `U="x"; R="y"` on one line, and `then VERB="x"; else VERB="y"`.
ASSIGNMENT = re.compile(r"(?<![\w$\-/.])([A-Za-z_]\w*)=(?![=~])")
LOOP_VAR = re.compile(r"\bfor\s+([A-Za-z_]\w*)\s+in\b")
READ_VAR = re.compile(r"\bread\s+(?:-\w+\s+)*([A-Za-z_]\w*)")


def is_runtime(name: str) -> bool:
    """Whether the runner supplies this name, so neither direction is a finding for it."""
    return name in RUNTIME_NAMES or name.startswith(RUNTIME_PREFIXES)


def strip_unexpanded(script: str) -> str:
    """Remove the parts of a script in which a `$name` is not a shell reference.

    Quoted heredocs, `${{ }}` workflow expressions, comments and single-quoted strings all carry
    `$name` sequences the shell never resolves -- an `awk '$0 ~ x'` program being the loudest. Left
    in, they make every one of them look like an undefined reference.
    """
    text = EXPRESSION.sub(" ", script)
    text = QUOTED_HEREDOC.sub(" ", text)
    text = FULL_LINE_COMMENT.sub("", text)
    text = TRAILING_COMMENT.sub("", text)
    return SINGLE_QUOTED.sub(" ", text)


def referenced_names(script: str, shell: str) -> set:
    """Every name the script reads, after the unexpanded parts are removed.

    PowerShell is scanned for `$env:NAME` only: its bare `$name` is a local variable, so treating one
    as an environment reference would report every loop counter.
    """
    embedded = set()
    for _, _, body in QUOTED_HEREDOC.findall(script):
        embedded.update(EMBEDDED_ENV.findall(body))
    text = strip_unexpanded(script)
    if shell in ("pwsh", "powershell"):
        return set(PWSH_REF.findall(text)) | embedded
    return set(BRACED_REF.findall(text)) | set(BARE_REF.findall(text)) | embedded


def assigned_names(script: str, shell: str) -> set:
    """Every name the script defines for itself, which is not a reference to anything outside it."""
    if shell in ("pwsh", "powershell"):
        return set()
    text = strip_unexpanded(script)
    names = set(ASSIGNMENT.findall(text))
    names.update(LOOP_VAR.findall(text))
    names.update(READ_VAR.findall(text))
    return names


def steps_of(document: dict):
    """Yield `(job, step_index, step, inherited_env)` for every step in a workflow or action.

    A composite action has one flat `runs.steps` and no jobs, so it is reported under the job name
    `runs` -- which is what its YAML calls it.
    """
    workflow_env = document.get("env") or {}
    if "runs" in document and isinstance(document["runs"], dict):
        for index, step in enumerate(document["runs"].get("steps") or []):
            yield "runs", index, step, dict(workflow_env)
        return
    for job_name, job in (document.get("jobs") or {}).items():
        if not isinstance(job, dict):
            continue
        inherited = dict(workflow_env)
        inherited.update(job.get("env") or {})
        for index, step in enumerate(job.get("steps") or []):
            yield job_name, index, step, inherited


def check(paths) -> list:
    """Collect every finding across the given workflow/action files, as `(key, kind, detail)`."""
    findings = []
    files = []
    for path in paths:
        path = Path(path)
        if path.is_dir():
            files.extend(sorted(path.rglob("*.yml")))
            files.extend(sorted(path.rglob("*.yaml")))
        elif path.is_file():
            files.append(path)
    for file in files:
        document = yaml.safe_load(file.read_text())
        if not isinstance(document, dict):
            continue
        try:
            rel = file.relative_to(REPO)
        except ValueError:
            # A path outside the repository -- a scratch copy under test. Report it as given.
            rel = file
        for job_name, index, step, inherited in steps_of(document):
            if not isinstance(step, dict):
                continue
            script = step.get("run")
            step_env = step.get("env") or {}
            label = step.get("name") or step.get("id") or f"#{index}"
            # A `uses:` step's `env:` is read by the action, not by any script here, so neither
            # direction can be judged from this file.
            if script is None:
                continue
            shell = (step.get("shell") or "bash").lower()
            referenced = referenced_names(script, shell)
            assigned = assigned_names(script, shell)
            declared = set(step_env) | set(inherited)
            for name in sorted(set(step_env) - referenced):
                findings.append((f"{rel}::{job_name}::{label}::{name}", "UNUSED",
                                 f"declared in `env:` but never read by the script"))
            for name in sorted(referenced - declared - assigned):
                if is_runtime(name):
                    continue
                findings.append((f"{rel}::{job_name}::{label}::{name}", "UNDEFINED",
                                 f"read by the script but neither declared nor assigned"))
    return findings


def main() -> int:
    """Report every finding not carrying an allowlist entry; exit 1 when any remain."""
    argv = [a for a in sys.argv[1:] if a != "--list"]
    listing = "--list" in sys.argv[1:]
    findings = check(argv or DEFAULT_PATHS)
    allowed = {}
    if ALLOWLIST.exists():
        allowed = yaml.safe_load(ALLOWLIST.read_text()) or {}
    if listing:
        for key, kind, detail in findings:
            print(f"{key!r}: {kind} - {detail}")
        return 0
    remaining = [f for f in findings if f[0] not in allowed]
    for key, kind, detail in remaining:
        print(f"{kind}: {key}\n    {detail}")
    if remaining:
        print(f"\n{len(remaining)} finding(s). Fix the mismatch, or add the key to "
              f"{ALLOWLIST.relative_to(REPO)} with the reason it is correct.")
        return 1
    print(f"ci-env-reference-check: clean ({len(allowed)} allowlisted)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
