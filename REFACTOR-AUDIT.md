# Refactor audit — `claude-securitymanager-unknown-java` (second pass)

**Scope:** `git log develop..HEAD` — **4 commits**, `0ff278423` … `d2d155927`.
**Mode:** READ-ONLY. No source was modified while auditing.
**Supersedes** the first pass over this branch, which covered the first two commits and raised M-1,
M-2, L-1 and L-2. **M-1 and L-2 are fixed** by `28d95bf06` and `d2d155927` and are re-verified below.
M-2 and the original L-1 stand — the commits were not rewritten.

**Verdict: no HIGH, two MEDIUM, two LOW.** The two remediation commits do what they claim, and the
new tests were teeth-checked mutation by mutation. Both new findings are commit-hygiene, and **one is
a repeat of a failure mode this project has already been bitten by twice.**

---

## HIGH

None. No Kotlin source is touched anywhere in the branch; the only production changes are the three
shell templates and one shipped `variables.txt` comment block. No module boundary, no plugin API.

---

## MEDIUM

### M-1 · `28d95bf06` sweeps `REFACTOR-AUDIT.md` into a `test(api)` commit — the third instance of this exact mistake

**File:** `REFACTOR-AUDIT.md` (+105/−79, the single largest change in the commit)
**Rule:** *One concern per commit.*

The commit is titled *"pin the Java-resolve and fail-safe guard in all three templates"* and its body
describes only the new test. Its actual contents:

```
REFACTOR-AUDIT.md                          184 ++++++-------
.../api/ScriptTemplateContentTest.kt        67 ++++++
```

The audit report — a *different* document, about the *previous* audit pass — is more than half the
diff and is **not mentioned once** in the commit message.

**This is a repeat.** The earlier `claude-modscanning-generification` audit raised exactly this as
**H-1**: `git add -A` sweeping unrelated files into a commit whose message describes something else.
That was severe enough to warrant rebuilding the branch. It happened again here, from the same cause:
the audit report was sitting uncommitted in the working tree when the remediation was staged with
`git add -A`.

It is MEDIUM rather than HIGH only because the swept file is documentation with no bearing on the
build, where H-1 buried four `-api` production refactors. The *habit* is identical and has now cost
three findings across this session.

**Remedy:** the branch is unpushed, so `REFACTOR-AUDIT.md` can be split out into its own `docs:`
commit exactly as the earlier H-1 was. More usefully: stop using `git add -A` when an audit report is
in flight.

### M-2 · `5f4bce289` bundles two independent fixes *(carried from the first pass, unchanged)*

The version-resolve and the guard inversion are separable, with separate rationales and separate
failure modes — as that commit's own body argues in both directions. Still one commit.

---

## LOW

### L-1 · `d2d155927` is typed `docs(api)` but adds a test and edits a shipped template

**Files:** `ScriptTemplateContentTest.kt` (+81), `variables.txt` (+6), `CLAUDE.md` (+9)

The type badly understates the contents. 81 of its 96 added lines are a **new executing test** —
`theBashTemplateResolvesTheJavaVersionEvenWhenChecksAreSkipped`, which extracts and runs the shipped
Java-check block against a fake Java. That is a `test(api):` commit wearing a `docs:` label.

The `variables.txt` change is also not quite documentation in the ordinary sense: it is a **shipped
template**, so those six comment lines land in the `variables.txt` of every server pack SPC generates
from now on. Comment-only, so nothing functional changes, and `VariablesTemplateTest` (which asserts
the placeholders generation substitutes, not prose) stays green — but "docs" reads as *repository
documentation*, and this ships to users.

Splitting it into `test(api):` + `docs(api):` would have cost nothing, and this branch already
demonstrates the split is natural — `28d95bf06` is a standalone test commit.

### L-2 · The new cross-template assertions are whitespace-exact against template source

**File:** `ScriptTemplateContentTest.kt:415,420,425`

The fail-safe guard is matched as an exact substring, e.g.

```
if [[ ! "${JAVA_VERSION}" =~ ^[0-9]+$ ]] || [[ ${JAVA_VERSION} -ge 24 ]]; then
```

So reformatting the condition — splitting it across lines, changing spacing, swapping `[[ ! x ]] || y`
for an equivalent — fails the test even though behaviour is unchanged. A future maintainer tidying a
shell template gets a red suite and no hint that the *behaviour* is fine.

Accepted as the established trade-off rather than a defect: this is exactly what
`allTemplatesUseAnAlreadyInstalledFabricLauncherBeforeCheckingTheNetwork` already does, and for the
same reason — fish and PowerShell cannot be executed on every machine, so source-level matching is the
only coverage available. The failure message does say what shape is expected, which is what makes a
brittle assertion survivable. Recorded so the brittleness is a known cost, not a surprise.

---

## Verified fixed since the first pass

- **First-pass M-1 (fish and ps1 untested) — FIXED by `28d95bf06`.** Six mutations were run, one per
  shell per property, and all six fail:

  | | sh | fish | ps1 |
  |---|---|---|---|
  | guard un-inverted | FAILS | FAILS | FAILS |
  | resolve call removed | FAILS | FAILS | FAILS |

  The commit body records that the second row did **not** fail on the first attempt — the assertion
  searched backwards from a marker and matched a `getJavaVersion` *inside* the check block. Anchoring
  on the last `installJava` fixed it. Recording a failed teeth-check is what makes the second one
  credible, and it is the difference between this pin and one that quietly asserts nothing.

- **First-pass L-2 (undisclosed `SKIP_JAVA_CHECK` consequence) — FIXED by `d2d155927`**, and fixed the
  right way round: the behaviour was checked against what `variables.txt` actually promises
  ("compatibility check … as well as the automatic installation" — comparing and installing, not
  reading) before being kept. The new test asserts *both* halves — the version resolves **and** the
  install is still skipped — so the promise the setting makes is now guarded, not just the new
  behaviour. Teeth verified by moving the call back inside the conditional.

## Checked and clean

- **Pin-first held for the original fix**: `0ff278423` is test-only, observed red, and quotes a failure
  reproducing the reporter's run command line for line.
- **No `refactor:` commit anywhere in the branch**, so the "a refactor that changes a test isn't a
  refactor" rule cannot be violated.
- **No existing assertion was weakened.** `theBashTemplateDropsTheSecurityManagerFlagOnJavaThatRejectsIt`
  still requires 17/21 → flag passed, 24/25 → dropped, and passes after the inversion — which is what
  proves the guard was inverted rather than loosened.
- **A user's proposed fix was declined with a stated reason**, not silently ignored.
- **A suspected second bug was confirmed with the reporter before being written down** (1.20.1 pulling
  Java 25 turned out to be a hand-edited `RECOMMENDED_JAVA_VERSION`).
- **The verification gap is still disclosed**: fish and pwsh are absent on this machine, and the commits
  say so rather than implying execution.
- `./gradlew clean build` green; api 307 (1 skip).

## Follow-ups, not defects in this range

- Split `REFACTOR-AUDIT.md` out of `28d95bf06` (M-1) while the branch is still unpushed.
- Still open from earlier branches: the tracked
  `serverpackcreator-plugin-example/src/main/resources/CHANGELOG.md` that nothing generates, and the
  configuration cache remaining opt-in because of the third-party `:generateLicenseReport`.
