# Refactor Audit — unpushed `develop` (`origin/develop..HEAD`)

**Range audited:** `origin/develop..HEAD` — **102 commits**
**Base:** `origin/develop` (local `develop` is 0 behind it, 102 ahead)
**Head:** `a09138a9b`
**Mode:** READ-ONLY. No source modified.
**Suite at HEAD:** api ✅ · clientside ✅ · grinder ✅ · app ✅ · frontend ✅ (31)
— verified after `b28d131ea`; the only commit since (`a09138a9b`) touches `claude-docs/BACKLOG.md` alone.

> Third report at this path. Supersedes the 81-commit version (1 HIGH / 1 MEDIUM / 2 LOW), whose findings are
> **carried forward with updated status** rather than re-argued.

**Range composition** — audit effort was directed at the segment never examined before:

| Segment | Commits | Status |
|---|---|---|
| `origin/develop..8be2913f8` — earlier sessions | 37 | Audited in report 2 |
| `8be2913f8..bc2bfe7fa` — the merged branch | 44 | Audited in report 1 |
| `bc2bfe7fa..HEAD` — the 2026-07-31 plan, Phases 1–6 | **21** | **Audited here for the first time** |

**New findings: 1 HIGH · 2 MEDIUM · 0 LOW** (L-C withdrawn on inspection — see below). The 21 new commits are the best-disciplined segment in the range
— small, single-purpose, no new `!!`, no stray debug, no cross-module sprawl except where noted. Every finding
below is about *labelling and recording* a change, not about a change being wrong: nothing here is a defect in
shipped behaviour, and one is fixed by adding a table row.

---

## HIGH

### H-B — `7815d5960` changes what an exported API call returns, and it is not in the compatibility table
**Commit:** `7815d5960` *"refactor(api): ship variables.txt as a template instead of a string literal"*
**Files:** `serverpackcreator-api/src/main/kotlin/.../serverpack/ServerPackProvisioner.kt:56-63` (+27/−94),
`settings/PathsConfig.kt:694`, `ApiProperties.kt:862`, new resource `server_files/variables.txt` (+91)
**Rule broken:** *Changed plugin-API contract* (rubric HIGH), against the project's own policy:
*"Source-compatible is not the same as behaviour-compatible… a change that keeps every signature but alters
what an exported call returns is still a contract change for embedders, and belongs in the release notes."*
(`CLAUDE.md:88-90`)
**Severity:** HIGH — same class as the previous audit's **H2**, which was resolved by documenting it.

`ServerPackProvisioner.variables` is reachable from plugins and embedders as
`apiWrapper.serverPackHandler.variables`. Before this commit it returned a compiled-in constant; now it reads
`apiProperties.defaultVariablesTemplate` from disk, falling back to the bundled copy. Two new exported members
land with it (`PathsConfig.defaultVariablesTemplate`, `ApiProperties.defaultVariablesTemplate`).

For a default installation the value is unchanged — the commit states the extracted resource is byte-identical
to the literal, and `ServerPackHandlerTest.forgeTest` (which already generated and asserted `variables.txt`
before the move) stayed green. But the observable contract is now different in kind: an operator who edits or
deletes that file changes what every embedder's generation emits, which was previously impossible.

`CLAUDE.md:93-94` tables exactly two such changes (`PathsConfig.homeDirectory`, `resolvePreferencesNode`).
This one is absent, so the policy that was written *for this situation* was not applied to it.

**Remediation:** **DONE** (`70cfe7fd9`) — the compatibility table now carries the row, naming both new members and
the "generation reflects an on-disk file" effect. No code change was needed; the behaviour is wanted.

---

## MEDIUM

### M-B — every code commit in the plan bundles its test with the production change
**Commits:** `2a9a03473`, `30f6cbded`, `c571e2d7f`, `07a647f01`, `aa2d27f7f`, `91ac0e1a9`, `1f92f585c`,
`5caa6833f` — **8 of 8** code commits in the segment
**Rule broken:** *One concern per commit. Keep "add tests", "refactor (no behavior change)" and "change
behavior" in separate commits.*

Each commit contains the change and its guard together. `2a9a03473` is representative: three template files
(+17/−4) plus `ScriptTemplateContentTest.kt` (+76/−0) in one commit. The approved plan was explicit for this
phase — *"Test first (fails). … **Commit alone, test only.**"* — and that boundary was not kept in any of the eight.

Two mitigations, stated because they change what this finding means:

- The tests **were** written first and observed failing in-session; only the commit boundary collapsed. This is
  not the fix-then-pin pattern of M-A/M2, where no test existed at fix time.
- `b28d131ea` then wrote the pin-first rule into `CLAUDE.md` — so the segment codified the discipline it was
  simultaneously not following at the commit level.

The cost is real but narrow: the history cannot *demonstrate* any pin failing. Nobody can check out
`2a9a03473^` and watch the guard go red, which is precisely the evidence the new rule asks for. Worth noting
that in-session verification of teeth **silently passed twice** this session (a mis-indented edit meant the
"broken" run was unmodified code), which is the argument for the separate commit rather than against it.

### M-C — two behaviour changes are labelled `refactor:`
**Commits:** `5f138ef8a` *"refactor(app): route the stored home directory through one place"*,
`7815d5960` *"refactor(api): ship variables.txt as a template instead of a string literal"*
**Rule broken:** *A pure refactor commit must keep the suite green with the existing assertions. If a test must
change for a "refactor", that is a signal the change is NOT behavior-preserving — stop and flag it.*

`5f138ef8a` is the clearer case. It changes four call-sites from a hard-coded `Preferences` node to the
resolved one, so a host claiming its own node (the grinder daemon, every test JVM) now reads and writes a
different location than before — the intended fix for B1, and a behaviour change. The signal fired exactly as
the rule predicts: an **existing** test had to change (`CommandlineParserTest.kt` +15/−13, replacing a literal
node lookup with `HomeDirectoryPreference.stored()`). Inspected — the assertion semantics are equivalent and
the edit is plumbing, not a weakened guard — but under a `refactor:` label the rule says stop and flag, and it
was neither stopped nor flagged.

`7815d5960` additionally reaches outside its module: `serverpackcreator-app/.../ServerPackCreator.kt:355-359`
gains a delete-watcher branch so a removed template is restored. That is app-side behaviour in a commit
labelled `refactor(api)`.

Both are single-concern and both messages describe the behaviour change explicitly, so nothing is concealed
from a reviewer — which is why this is MEDIUM, not HIGH: there is no independent refactor riding along to be
confused with the behaviour change. `fix:` and `feat:` respectively would have been the honest labels.

---

## LOW

### L-C — ~~`var` where every sibling is `val`, and nothing assigns it~~ **WITHDRAWN — the finding was wrong**
**Commit:** `7815d5960`
**File:** `serverpackcreator-api/src/main/kotlin/.../settings/PathsConfig.kt:694-699`

Retracted on inspection before the change was made. The declaration continues past the line the audit quoted:

```kotlin
var defaultVariablesTemplate: File = File(serverFilesDirectory, "variables.txt").absoluteFile   // :694
    get() {
        field = File(serverFilesDirectory, "variables.txt").absoluteFile                        // re-derives
        return field
    }
    private set                                                                                 // :699
```

Two things the original finding missed. The setter is **`private`**, so this is not exported mutable state and
`val` would not narrow the public surface. And the `var` is **load-bearing**: the getter assigns the backing
field so the path re-derives on every access, which is how it tracks a home directory that changed — and
`serverFilesDirectory` (`:573-578`) does exactly the same on top of `homeDirectory`, which this very branch
made re-resolve per access. **31** properties in this file use that pattern; `defaultVariablesTemplate`
follows it correctly.

The two plain `val`s the audit held up as the standard (`:586`, `:603`) are the exception, not the rule — and
they are the ones with the latent problem: captured once at construction, they do **not** follow a home that
changes afterwards. Recorded as backlog **B21** rather than fixed here, since it predates this range and needs
its own pin.

---

## Carried forward

Re-verified at this HEAD; not re-argued.

| Finding | Severity | Status at this HEAD |
|---|---|---|
| **H-A** `origin/develop` published with a failing `ConfigEditorViewModelTest` | HIGH | **STILL OPEN — the only action item that cannot be done locally.** Fix `34464832e` remains unpushed; the backlog behind it has grown from 81 to **102** commits. |
| H1 `aca721725` bundled a refactor with five behaviour changes | HIGH | Open, not remediable — rewriting landed history would be worse. Consequence closed by `8ebb3bbcb`. |
| H2 `dd4fcc935` changed home resolution for every embedder | HIGH | Documented in the compatibility table. **H-B is the same class and is *not* — that table is the remediation pattern.** |
| M-A `2e16bf0c8` fix-then-pin on a template; third instance in two audits | MEDIUM | **Closed** by `b28d131ea` — pin-first for templates/manifests/version-parsing is now a binding rule in `CLAUDE.md`, carrying its evidence (24 wasted boots, 820 mis-attributed NeoForge versions). |
| M1 checked-in test properties rewritten by the suite | MEDIUM | Fixed (`ef3280e4c`, `e7cce83fb`). Second half — the files still carry one machine's absolute paths — recorded as backlog **B16**. |
| M2 `28a786b58` shipped untested | MEDIUM | Closed by `5703b0340`. |
| M3 / M4 commit-shape findings | MEDIUM | Historical. |
| M5 `LoaderSupportMemory` on a false premise | MEDIUM | Reverted (`d8b6cb157`). |
| M6 guard test green while behaviour broken | MEDIUM | Fixed (`e3296a7d2`). |
| L1 new `!!` in refactored code | LOW | Fixed (`60541158a`); **still none introduced** — verified across all 21 new commits. |
| L2 / L3 / L-A / L-B | LOW | Reviewed, accepted, or historical. |

---

## What the 21 new commits got right

Recorded because it is the majority of the picture and the contrast with report 1 is the point.

- **Not a blind refactor.** `7815d5960` moved an unpinned-looking literal, but `ServerPackHandlerTest.forgeTest`
  already generated and asserted `variables.txt`, and the commit verified byte-identity — so the pre-existing
  end-to-end pin stayed green across the move.
- **A documented invariant was respected under pressure.** `91ac0e1a9` adds an installer-fallback that could
  easily have redefined "newest"; instead it supplies `availableVersions` while `latestVersion` still delegates,
  scoped to Forge/NeoForge only, with the reasoning recorded at the function.
- **Clean Kotlin.** No new `!!`; the only new `var`s are loop-local deadline state in the suspend-gap fix
  (legitimate) plus L-C. No `TODO`, `FIXME`, or `println` added anywhere in the segment.
- **Correct separation where it counts.** `1c69fdc62` is test-only; the seven `docs:` commits are docs-only;
  `aa2d27f7f` fixes a bug found *by* the previous commit's test rather than absorbing it.
- **The audit's own process finding was closed** (`b28d131ea`, M-A) instead of being noted again.

---

## Recommended order of action

**Status 2026-07-31, branch `claude-audit-backlog-cleanup`:** H-B, M-B and M-C are **closed** (`70cfe7fd9`);
**L-C is withdrawn** (`b9777ddf9`) — see its section. **H-A remains the only open action and cannot be done
locally.**

1. **Push.** **H-A** is unchanged and now 102 commits deep. Every other finding in this report is either
   historical, already closed, or documentation.
2. **H-B — add the compatibility-table row** for `variables.txt` / `defaultVariablesTemplate`. Cheap, and it is
   the project's own stated policy for exactly this kind of change.
3. **DONE — M-B and M-C are now binding rules** in the root `CLAUDE.md`, and this branch follows them: every code
   change landed as a red `test(...)` commit followed by its fix. **M-B is a boundary habit, not a correctness gap.** The pin-first rule now exists; what the eight commits
   show is that "written first" and "committed first" drifted apart. If the failing-guard evidence matters
   (and the two silently-passing teeth checks argue it does), the rule needs the commit boundary spelled out,
   not just the ordering.
4. **M-C — label behaviour changes `fix:`/`feat:`.** Both commits were honest in the body; only the type was wrong.

---

**Report only — no source modified. Awaiting go-ahead.**
