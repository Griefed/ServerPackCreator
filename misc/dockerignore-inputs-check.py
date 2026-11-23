#!/usr/bin/env python3
"""Fail when `.dockerignore` excludes a file a Gradle task declares as an input and the image build runs.

`docker/Dockerfile` runs a full Gradle build on the repository root as its context. A task input that
`.dockerignore` keeps out of that context fails the image build with "Input file does not exist", and the
only other safeguard is a comment. This finds every `inputs.file(...)` in the build scripts, names the task
declaring it, and checks the path against `.dockerignore` with Docker's rules (`**`, a directory pattern
excluding its contents, the last matching line winning, `!` re-including). Inputs of a task the Dockerfile
skips with `-x :project:task` are reported as exempt rather than as findings.

Usage: python3 misc/dockerignore-inputs-check.py [repository-root]
"""

import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
INPUT = re.compile(r"""inputs\.file\(\s*(rootProject\.file|file)\(\s*"([^"]+)"\s*\)""")
TASK_OPENER = re.compile(r"""^\s*tasks\.(?:named(?:<[^>]*>)?\(\s*"([^"]+)"|(\w+))""")
SKIPPED = re.compile(r"-x\s+(:[\w:.-]+)")


def pattern_regex(pattern: str) -> re.Pattern:
    """Docker's filepath.Match plus `**`, as a regex over a slash-separated relative path."""
    out, i = "", 0
    while i < len(pattern):
        if pattern.startswith("**/", i):
            out += "(?:.*/)?"
            i += 3
        elif pattern.startswith("**", i):
            out += ".*"
            i += 2
        elif pattern[i] == "*":
            out += "[^/]*"
            i += 1
        elif pattern[i] == "?":
            out += "[^/]"
            i += 1
        else:
            out += re.escape(pattern[i])
            i += 1
    return re.compile(f"^{out}$")


def parse_dockerignore(text: str) -> list:
    """`(regex, excludes)` per non-comment line, in file order."""
    rules = []
    for raw in text.splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        excludes = not line.startswith("!")
        line = line.lstrip("!").strip().strip("/")
        rules.append((pattern_regex(line), excludes))
    return rules


def is_excluded(path: str, rules: list) -> bool:
    """Whether Docker leaves `path` out of the context: the last rule matching it or a parent decides."""
    parts = path.split("/")
    candidates = ["/".join(parts[:n]) for n in range(1, len(parts) + 1)]
    excluded = False
    for regex, excludes in rules:
        if any(regex.match(candidate) for candidate in candidates):
            excluded = excludes
    return excluded


def declared_inputs(root: Path) -> list:
    """`(task path, repository-relative input path)` for every `inputs.file(...)` in a build script."""
    found = []
    for script in sorted(root.rglob("build.gradle.kts")):
        relative = script.relative_to(root)
        if any(part in ("node_modules", "build") for part in relative.parts):
            continue
        # A directory with its own `.git` is another checkout (a git worktree under .claude/worktrees, say),
        # whose build scripts are not this build's.
        if any((root.joinpath(*relative.parts[:depth]) / ".git").exists() for depth in range(1, len(relative.parts))):
            continue
        module_dir = script.parent
        project = module_dir.relative_to(root).as_posix()
        task = None
        for line in script.read_text().splitlines():
            opener = TASK_OPENER.match(line)
            if opener:
                task = opener.group(1) or opener.group(2)
            for kind, value in INPUT.findall(line):
                base = root if kind == "rootProject.file" else module_dir
                target = (base / value).resolve().relative_to(root.resolve()).as_posix()
                found.append((f":{project}:{task}" if project != "." else f":{task}", target))
    return found


def check(root: Path) -> tuple:
    """`(findings, exempt)`: excluded inputs of tasks the image build runs, and of tasks it skips."""
    rules = parse_dockerignore((root / ".dockerignore").read_text())
    skipped = set(SKIPPED.findall((root / "docker" / "Dockerfile").read_text()))
    findings, exempt = [], []
    for task, path in declared_inputs(root):
        if is_excluded(path, rules):
            (exempt if task in skipped else findings).append((task, path))
    return findings, exempt


def main() -> int:
    root = Path(sys.argv[1]) if len(sys.argv) > 1 else REPO
    findings, exempt = check(root)
    for task, path in exempt:
        print(f"exempt: {path} (input of {task}, which docker/Dockerfile skips with -x)")
    for task, path in findings:
        print(f"EXCLUDED INPUT: {path} is an input of {task}, and .dockerignore keeps it out of the image "
              f"build's context. Re-include it with a `!` line, or skip the task in docker/Dockerfile.")
    if findings:
        return 1
    print(f"dockerignore-inputs-check: clean ({len(declared_inputs(root))} declared inputs, {len(exempt)} exempt)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
