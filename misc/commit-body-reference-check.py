#!/usr/bin/env python3
"""Fail when a commit message holds a reference the release notes would link as an issue that is not one.

semantic-release writes CHANGELOG.md through conventional-commits-parser 6.4.0 with the
conventionalcommits@9 preset's options, and every reference that parser extracts becomes a
`closes <link>` line in the notes. It does not read markdown, so a hex colour, a commit hash, a
`Class#member` or an `owner/repo#n` from another forge is linked to an issue of this repository that does
not exist, fenced code blocks included. This ports the parser's extraction (`regex.js` and
`CommitParser.parseReferences` of that version) line by line, and reports every extracted reference
except a plain `#<digits>`, which is a genuine issue of this repository. The preset's writer also links
`#[a-z0-9]+` inside the subject with no regard for backticks or URLs, so a non-numeric one there is
reported too.

What keeps a token out of the notes is what the parser cannot match: a reference must be followed by
whitespace, the end of the line or one of `,;)]`, so a closing backtick written directly after it stops
the match; and a line carrying an `http(s)://` URL yields no reference at all.

Usage: python3 misc/commit-body-reference-check.py [--git-dir DIR] [REVISION ...]
       python3 misc/commit-body-reference-check.py [--git-dir DIR] --push BEFORE SHA BRANCH
       REVISION defaults to origin/main..HEAD and is passed to `git log` as is. --push derives the
       range from a push event: BEFORE..SHA, or, for a new branch (all-zero BEFORE) or a BEFORE the
       repository does not have (a force push), the commits of SHA that no other branch holds. An empty
       BEFORE means the run is not a push, and the check is skipped. A shallow repository is refused,
       because its history ends early and every commit before the boundary would look new.
"""

import re
import subprocess
import sys

ACTIONS = "close|closes|closed|fix|fixes|fixed|resolve|resolves|resolved"
# The parser's `getReferencesRegex`: a sentence runs from a closing keyword to the next one. JavaScript's
# `$` without the `m` flag is the end of the input, which is `\Z` here.
SENTENCES = re.compile(rf"({ACTIONS})(?:\s+(.*?))(?=(?:{ACTIONS})|\Z)", re.I | re.A)
# `getReferencePartsRegex` with issuePrefixes ['#']. `[\w-\.\/]` in a non-unicode JavaScript class is the
# word characters plus `-`, `.` and `/`.
REFERENCE = re.compile(r"(?:.*?)??\s*([\w\-./]*?)??(#)([\w-]+)(?=\s|\Z|[,;)\]])", re.I | re.A)
URL = re.compile(r"\b(?:https?)://(?:www\.)?([-a-zA-Z0-9@:%_+.~#?&/=])+\b")
NOTE = re.compile(r"^[\s|*]*(BREAKING CHANGE|BREAKING-CHANGE)[:\s]+(.*)", re.I)
GPG = re.compile(r"^\s*gpg:")
HEADER = re.compile(r"^(\w*)(?:\((.*)\))?!?: (.*)$")
SUBJECT_ISSUE = re.compile(r"#([a-z0-9]+)")
GENUINE = re.compile(r"#\d+")
EMPTY_OBJECT_ID = "0" * 16


def references(line: str) -> list:
    """The references the parser extracts from one line, as `repository#issue` strings."""
    sentences = [match.group(2) or "" for match in SENTENCES.finditer(line)] if SENTENCES.search(line) else [line]
    found = []
    for sentence in sentences:
        if URL.search(sentence):
            continue
        found.extend(f"{match.group(1) or ''}#{match.group(3)}" for match in REFERENCE.finditer(sentence))
    return found


def mislinked(message: str) -> list:
    """Every reference in `message` the release notes would link although it is not an issue number."""
    lines = [line for line in re.split(r"\r?\n", message.strip("\r\n")) if not GPG.match(line)]
    header = HEADER.match(lines[0])
    in_subject = [f"#{issue}" for issue in SUBJECT_ISSUE.findall(header.group(3))] if header else []
    # The writer drops a reference the subject already links, so it is counted once.
    extracted = [token for line in lines if not NOTE.match(line) for token in references(line)]
    found = in_subject + [token for token in extracted if token not in in_subject]
    return [token for token in found if not GENUINE.fullmatch(token)]


def push_revisions(before: str, sha: str, branch: str, has_commit) -> list:
    """The `git log` revisions a push added, or None when there is no push (`before` empty)."""
    if not before:
        return None
    if before.startswith(EMPTY_OBJECT_ID) or not has_commit(before):
        return [sha, "--not", f"--exclude={branch}", "--branches"]
    return [f"{before}..{sha}"]


def main() -> int:
    args = sys.argv[1:]
    git = ["git"]
    if args[:1] == ["--git-dir"]:
        git += ["--git-dir", args[1]]
        args = args[2:]

    def run(*command: str) -> subprocess.CompletedProcess:
        return subprocess.run([*git, *command], capture_output=True, text=True)

    if run("rev-parse", "--is-shallow-repository").stdout.strip() == "true":
        print("commit-body-reference-check: the repository is shallow, so its history ends early and every "
              "commit before the boundary would look new. Fetch the full history (a `--filter=tree:0` clone "
              "carries every commit and costs a few megabytes).")
        return 1
    if args[:1] == ["--push"]:
        before, sha, branch = args[1:4]
        revisions = push_revisions(before, sha, branch,
                                   lambda rev: run("cat-file", "-e", f"{rev}^{{commit}}").returncode == 0)
        if revisions is None:
            print("commit-body-reference-check: skipped, this run is not a push, so there is no pushed range")
            return 0
    else:
        revisions = args or ["origin/main..HEAD"]

    log = run("log", "-z", "--format=%h%n%B", *revisions, "--")
    if log.returncode != 0:
        print(f"commit-body-reference-check: cannot resolve {' '.join(revisions)}\n    {log.stderr.strip()}")
        return 1
    records = [record for record in log.stdout.split("\0") if record.strip()]
    offenders = 0
    for record in records:
        short_hash, _, body = record.partition("\n")
        tokens = mislinked(body)
        if tokens:
            offenders += 1
            print(f"MISLINKED REFERENCE {short_hash} {body.splitlines()[0] if body else ''}\n"
                  f"    {' '.join(tokens)}")
    if offenders:
        print(f"\n{offenders} commit(s) would link a non-issue as an issue in the release notes. Put each "
              "token in backticks with the closing tick directly after it (a subject cannot be fixed that "
              "way; reword it), or link a real issue as a plain #<number> or a URL.")
        return 1
    print(f"commit-body-reference-check: clean ({len(records)} commits in {' '.join(revisions)})")
    return 0


if __name__ == "__main__":
    sys.exit(main())
