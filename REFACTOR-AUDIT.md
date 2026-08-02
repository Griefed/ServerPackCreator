# Refactor Audit — `claude-kdoc-coverage` (`develop..HEAD`)

**Range audited:** `develop..HEAD` — **9 commits**, `e670f38c8` … `9bd743611`
**Base:** `develop` (`19b86e7c5`) — verified ancestor, clean linear range
**Mode:** READ-ONLY. No source modified.
**Suites at HEAD:** api 278 (1 skip) · clientside 87 · grinder 233 (19 skip) · app 76 — all green

> Sixth report at this path. Supersedes the `claude-backlog-followups` audit (1 HIGH / 2 MEDIUM / 2 LOW, all
> remediated), whose text is in git at `fce979685`'s successor on that branch.

**Findings: 0 HIGH · 1 MEDIUM · 3 LOW.**

**Status 2026-07-31 — all four addressed.** **M-1** resolved by cherry-picking `e670f38c8` onto `develop`
(`266f4f099`) and replaying this branch's eight documentation commits on top, so the build change is reviewed as
itself and no longer rides a docs merge; the replay was verified content-identical to the pre-rebase branch.
**L-2** corrected in the replayed message ("eight files"). **L-3** addressed by moving the shared contract into the
`ServerPackConfigTab` interface doc and enriching nine accessors. **L-1** needs no code change — reverting the
reshape would delete the per-parameter docs it enables — and is instead recorded as a convention in `CLAUDE.md`,
together with the L-3 lesson, so neither is re-flagged.

This is a documentation-only branch, so most of the conventions have nothing to bite on — and that is worth
stating rather than glossing: **no characterization tests were required because no behaviour changed**, which the
evidence below establishes positively rather than by assumption. The single MEDIUM is about lineage, not code.

---

## MEDIUM

### M-1 — a commit of Griefed's is reachable only from this branch, not from `develop`
**Commit:** `e670f38c8` *"build(deps): Update NodeJS and npm versions"* — **not authored by this session**
**Files:** `buildSrc/.../quasar-conventions.gradle.kts`, `docker/Dockerfile.help`,
`serverpackcreator-web-frontend/package.json`, `package-lock.json`, the frontend plugin's package-manager spec
**Rule broken:** *One concern per commit* — in effect, not by authorship: a documentation branch is carrying an
unrelated build/dependency change.

`develop` is at `19b86e7c5`. `e670f38c8` sits between it and this branch's first documentation commit, because the
branch was cut from the working HEAD at the time rather than from `develop`. Two consequences:

1. **`develop` does not contain it.** The NodeJS/npm bump and the `installCorepackLatest` change (the quasar edit
   handed back after the previous audit's H-1) exist only here. Discarding this branch discards them.
2. **Merging this branch brings a build change in under a documentation merge.** The frontend is not built by any
   of the four Kotlin suites run to green here, so "all suites green" says nothing about it — the same gap the
   previous audit flagged when that file first appeared.

**Remediation:** decide where that commit belongs before merging. Cherry-picking it onto `develop` on its own, then
rebasing this branch, keeps the documentation history honest and gets the dependency bump reviewed as itself. If it
merges as-is, the merge message should name it, and `npx quasar build` should be run first — nothing in this branch
exercises it.

**Note for future branches:** cut from the integration branch, not from whatever HEAD happens to be, or unrelated
work rides along invisibly.

---

## LOW

### L-1 — four declarations were reshaped inside documentation commits
**Commits:** `f0371da56` (`ContainerRunOutput`, `CatalogCursor`), `d386b6153` (`Exclusion`, `Dependency`)
**Rule broken:** *One concern per commit* / *Boy Scout rule — stay within scope.*

Each went from a single-line constructor to a multi-line one:

```
-data class ContainerRunOutput(val lines: List<String>, val exitCode: Int?, val timedOut: Boolean)
+data class ContainerRunOutput(
+    val lines: List<String>,
...
```

Formatting, not behaviour — verified: parameter names, types, order and the one default (`modID: String = "N/A"`)
are all preserved. It is also the *enabling* change, since per-parameter KDoc cannot attach to a parameter on a
shared line, so calling it sprawl would be harsh. LOW, and recorded only because a reader diffing these commits
expecting comments-only will find four source lines that moved.

### L-2 — `dd5cf2ef0`'s message says "nine files"; the commit contains eight
**Commit:** `dd5cf2ef0` *"docs(api): document the version-meta instances, path defaults and scanner matchers"*

The declaration count in that message (31) is right and matches the measured api delta (81 → 50); the file count is
off by one. Trivial in isolation, recorded because this branch's commit messages are the only record of what each
batch covered, and every other count in the range checks out: `d386b6153` "seven files" = 7, `9bd743611` "~30" = 29,
and the per-file deltas (`PackConfig` 38, `ServerPackConfigTab` 39, `ApiProperties` 24) are exact.

### L-3 — a subset of the 299 docs are thin restatements, which the convention discourages
**Commits:** chiefly `aac077a8e`, `dd5cf2ef0`
**Examples:** `getMinecraftVersion` → *"The selected Minecraft version."*; `getServerPackSuffix` → *"The configured
server-pack suffix."*; `IS_MAC` → *"Whether SPC runs on macOS."*
**Rule broken:** the project's own documentation convention — *"State briefly WHAT it's for and HOW it achieves it,
not a restatement of the signature."*

Honest accounting of a real tension: for a pure accessor over an already-named field there is often nothing to add,
and the alternative is silence, which is exactly what dokka flags. The majority of the branch does meet the higher
bar — `spcSSJArgsKeyDefaultValue` carries the JEP 486 warning, `modFileEndings` explains why `disabled` counts,
`NeoForgeInstance.installerUrl` warns that maven lists versions whose installer 404s, `bothServer` records that the
declared side is an unreliable self-report — but perhaps 30–40 of the 299 are labels. Worth knowing before treating
"0 undocumented" as "fully documented".

---

## Verified clean (positive findings, established rather than assumed)

- **No behaviour changed anywhere in the branch.** Filtering the whole `develop..HEAD` diff down to non-comment
  lines yields *exactly* the four reshapes in L-1 and nothing else. No new `!!`, no new `var`, no altered default.
- **`ServerPackConfigTab`'s 39 signatures are byte-identical** to `develop`'s, compared as a sorted signature set.
  That interface body was rewritten wholesale, so this was the check worth doing — and the app and plugin-example,
  its real implementors, both compile.
- **Two documentation claims spot-verified** rather than trusted: `NeoForgeTomlScanner` really does
  `override val modsToml` (`:31`), and `ServerPackHandler` really does read an old manifest back (`:229`), which is
  what makes the `ServerPackManifest.files` rationale true.
- **The three residual dokka warnings are unfixable, not skipped.** `Comparison` is an enum, `SPCGenericListener`
  and `NeoForgeInstance` are interfaces; none has a `companion object` anywhere in its source (grepped: zero hits),
  so dokka is reporting a synthetic declaration with nothing to attach a comment to.
- **Tooling traps caught before they became false claims**, both recorded in commit messages: `-q` suppresses
  dokka's warnings entirely (a first "0 undocumented" reading was meaningless), and `git grep -- '*.kt'`, POSIX ERE
  `\b`, and a missing `re.MULTILINE` each silently produced "0 mapped" while building the worklist.

---

## Carried forward

| Finding | Severity | Status at this HEAD |
|---|---|---|
| **H-A** `origin/develop` published with a failing `ConfigEditorViewModelTest` | HIGH | **STILL OPEN, six audits deep.** `develop` is 149 commits ahead of `origin/develop`; this branch adds 9 more. The only finding across all six that cannot be closed locally. |
| Previous audit's H-1 (`git add -A` swept an unrelated build change) | HIGH | Remediated there; **its subject reappears here as M-1**, now as a commit of Griefed's that only this branch can reach. |
| B11 Corepack workaround | — | Sole backlog entry, and `e670f38c8` touches exactly that mechanism — see M-1. |

---

## Recommended order of action

1. **M-1 — decide where `e670f38c8` belongs** before merging, and run `npx quasar build` against it: no suite in
   this branch touches the frontend.
2. **Push** (**H-A**). Six audits, 149+ commits.
3. **L-2** — correct or ignore; it is one word in a commit message and rewriting history for it is not worth it.
4. **L-3** — no action unless you want the thin accessors revisited; the alternative to a label there is silence.

---

**Report only — no source modified. Awaiting go-ahead.**
