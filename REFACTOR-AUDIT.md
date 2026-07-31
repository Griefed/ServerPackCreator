# Refactor Audit — `claude-grinder-catalog-cursor`

**Range audited:** `develop..HEAD` (38 commits, `2babcb443` … `797ed40c3`)
**Base:** `develop` — verified ancestor of HEAD, clean linear range
**Mode:** READ-ONLY. No source modified.
**Suite at HEAD:** api ✅ · clientside ✅ · grinder ✅ · app ✅

> Supersedes the previous report at this path (`claude-module-readmes`, 0 HIGH / 3 MEDIUM / 3 LOW). All three
> of its findings are **closed on `develop`**: the scanning snippet now uses `scan(jarFiles) → ScanResult`
> (M1), `ReadmeExamplesTest` pins §2/§3/§4/§5 and *executes* §8 (M2), and the clientside README documents the
> headless-browser dependencies (M3). Its subject commit `c08d786b1` is on no branch. Ask if you want that
> text restored.

**Summary: 2 HIGH · 6 MEDIUM · 3 LOW.** The branch's engineering outcomes are sound and the suite is green;
the failures are almost entirely in *commit hygiene* — concerns bundled together — and in one case that
bundling let a regression reach a live sweep.

---

## HIGH

### H1 — `aca721725` bundles a refactor with five behaviour changes across two modules
**Commit:** `aca721725` *"fix(clientside): decide CRASHED from the console, and never boot without required dependencies"*
**Rule broken:** *One concern per commit. Keep "add tests", "refactor (no behavior change)" and "change
behavior" in separate commits. Never mix a refactor with a feature or bugfix.*
**Severity:** HIGH — a behaviour change mixed into a refactor.

One commit: 16 files, +489/−19, spanning `-api` and `-clientside`, containing:

| Concern | Where |
|---|---|
| Behaviour — start scripts propagate the server's exit status | `default_template.{sh,fish,ps1}` (`default_template.sh:592`, `:602`) |
| Behaviour — a client-only-class console hit decides CRASHED | `BootLogClassifier.kt:181` |
| Behaviour — dependency markers force INCONCLUSIVE | `BootLogClassifier.kt:185` |
| Behaviour — staging refuses to boot without required deps | `BootVerifier.kt:352` (`refuseForMissingDependencies`) |
| Behaviour — Quilt dependencies fall back to Fabric builds | `BootCandidateSelector.kt:61` |
| Behaviour — boot detail now carries the exit status | `BootVerifier.outcomeFor` |
| **Refactor** — `downloadWithDependencies` gains an `unsatisfied` out-parameter | `BootVerifier.kt:156` |
| Docs | `CLAUDE.md`, `BACKLOG.md`, `REFACTOR-LOG.md`, `clientside/CLAUDE.md` |

The signature change to `downloadWithDependencies` is a pure refactor riding along with five behaviour
changes — exactly what the rule forbids. A reviewer cannot separate *"did the plumbing change break
anything?"* from *"is the new classification correct?"*.

**It also rewrote an existing assertion** (`BootVerifierOutcomeTest.kt:59`):
```
-  assertEquals("Forge 1.0 / Minecraft 1.20.1 → CRASHED", outcome.detail)
+  assertEquals("Forge 1.0 / Minecraft 1.20.1 → CRASHED (exit 1)", outcome.detail)
```
Legitimate in a behaviour-change commit — but it is the signal the convention says to *stop* on, and here it
sat unremarked among five other concerns.

**Consequence, realised.** This commit introduced a regression that reached a live sweep: making the exit
status trustworthy promoted every never-launched boot (`Error: Unable to access jarfile forge.jar`) to a
HIGH-confidence clientside verdict. **Ten of the next fifteen HIGH verdicts were false**, including the
server-side libraries `balm`, `collective` and `geckolib`. Closed two commits later by `8ebb3bbcb`. Landed
alone, the exit-status change would have invited the obvious review question — *"what else now returns
non-zero?"* — before it ever ran.

**Remediation:** none retroactively; do not rewrite landed history. Going forward, split.

### H2 — `dd4fcc935` changes home resolution for every embedder, and hides its own fallout inside the same commit
**Commit:** `dd4fcc935` *"fix(api): let a host pin its Preferences node and home directory"*
**Files:** `ApiProperties.kt:1310-1345` (new public `DEFAULT_PREFERENCES_NODE`, `PREFERENCES_NODE_PROPERTY`,
`PREFERENCES_NODE_ENV`, `resolvePreferencesNode`); `PathsConfig.kt:106`
**Rule broken:** *changed plugin-API contract* · *one concern per commit*
**Severity:** HIGH — the exported `-api` surface governs plugin compatibility.

The `ApiProperties` additions are additive and source-compatible, so the compatibility policy itself holds.
The contract change is `PathsConfig.kt:106`: a new **highest-precedence** branch in `homeDirectory`
resolution, ahead of both the stored preference and the properties file. Every host embedding `-api` —
including plugins reading `apiProperties.homeDirectory` — resolves its home differently when that property
is set. That warranted its own commit and a line in the compatibility notes.

The commit additionally carries the fix for a regression it created during development: isolating each test
JVM onto its own Preferences node removed the stored home, so resolution fell through to the working
directory and `ApiWrapper.setup()` wrote README/CHANGELOG/`server_files` **into the module source tree**,
overwriting `serverpackcreator-clientside/README.md`'s CLI guide. Shipping cause and cure together beats
shipping the cause alone — but it buries a load-bearing discovery in an unrelated-looking diff.

---

## MEDIUM

### M1 — checked-in test resources are mutated by running the suite, and were committed seven times
**Commits:** `defbded12`, `a44e4a700`, `b4b340254`, `d8b6cb157`, `1a55797df`, `aca721725`, `8ebb3bbcb`
**Files:** `serverpackcreator-api/src/test/resources/serverpackcreator.properties`,
`serverpackcreator-clientside/src/test/resources/serverpackcreator.properties`
**Rule broken:** *Boy Scout rule — stay within the commit's stated scope; don't let churn sprawl.*

Running any suite rewrites these checked-in files, and that churn was swept into seven unrelated commits. It
is **not** merely a timestamp. At HEAD the committed content reads:

```
…/serverpackcreator-api/src/test/resources/serverpackcreator.properties:27
  server.tomcat.basedir=/Users/davidhengstmann/…/serverpackcreator-app/build/spc-test-home
…/serverpackcreator-api/src/test/resources/serverpackcreator.properties:28
  spring.data.mongodb.uri=mongodb\://localhost\:27017/serverpackcreatordb   (was: user\:password@localhost)
```

Two distinct problems: a **build-output path** is now checked in, **machine-specific** to this developer; and
the MongoDB URI silently lost its `user:password@` credentials component. No commit message mentions either.
(Line 11's `de.griefed.serverpackcreator.java=/Users/…/sdkman/…` is machine-specific too, but predates this
branch.) The `build/spc-test-home` value is a direct consequence of **H2** — the Tomcat base directory follows
the home directory — so this is H2 leaking into version control.

**Worth a deliberate decision:** either these files should not be writable by the suite, or they should not
be checked in carrying absolute paths.

### M2 — `28a786b58` changes parsing behaviour with no tests
**Commit:** `28a786b58` *"fix: Correctly parse NeoForge to Minecraft version mappings"* — 2 files, **0 tests**
**File:** `NeoForgeLoader.kt` — the `when` building each Minecraft version's pattern
**Rule broken:** *Ensure characterization tests exist before changing a unit. Never refactor untested code blind.*

This is precisely the logic the rule protects: it fails **silently**, yielding empty or mis-assigned version
lists rather than an error. The change was correct and valuable — measured against the real 1639-version
manifest it removed **820 wrong attributions for Minecraft 1.21 alone** (985 builds claimed where 165 are
right), plus 108 for `1.21.1` — but nothing in the commit demonstrated either the old defect or the new
correctness. Characterization arrived only afterwards, in `5703b0340`.

### M3 — `5703b0340` and `e9faf528c` are labelled `test(...)` but carry production extractions
**Commits:** `5703b0340` (`NeoForgeLoader.kt`, + companion `neoForgeVersionPatternFor`); `e9faf528c`
(`ForgeLoader.kt:136-149`, + companion `forgeVersionFrom`)
**Rule broken:** *Keep "add tests" and "refactor (no behavior change)" in separate commits.*

Both lift a pure decision out of a private parse loop so a test can reach it, then add the tests. The
extractions are behaviour-preserving and the suites stayed green on existing assertions — the Strangler-Fig
shape is right — but a commit labelled `test:` that edits `src/main` misleads the log, and it is the same
mixing the convention forbids, in the other direction.

### M4 — three commits are large enough to defeat review
**Rule broken:** *One concern per commit* · *refactor incrementally; no big-bang.*

| Commit | Subject | Scope |
|---|---|---|
| `a44e4a700` | live boot logs, a `/status` endpoint, per-candidate log lines | **11** main, 7 test, 2 modules |
| `defbded12` | reuse cached loader builds, guarded by a crash re-check | 7 main, 4 test, **3** modules |
| `1783eb7c8` | advance the crawl cursor on work done, not on hand-out | 7 main, 3 test, 6 docs |

`a44e4a700` is three independently useful features sharing a theme but not a concern. `defbded12` spans app,
clientside and grinder, adding the `LoaderVersionPolicy` seam, the cache-preferring policy **and** the crash
re-check that makes the policy safe — that last piece carries the entire safety argument and deserved
isolation.

### M5 — `b4b340254` shipped a behaviour change on an unverified premise
**Commits:** `b4b340254` *"stop re-paying for loader combinations that cannot boot"* → reverted by `d8b6cb157`
**Rule broken:** *characterization before behaviour change.*

`LoaderSupportMemory` treated the boot log's `"<Loader> is not available for Minecraft X"` as proof that a
loader has no build for that version. It is not: the message is emitted when an HTTP probe fails, which under
the grinder's `--network none` boots always means *"could not check"*. Within minutes of going live it marked
Fabric unusable for **22 Minecraft versions**. The unit tests passed throughout — they encoded the same wrong
premise.

The revert is exemplary: prompt, total, and its message states the false premise plainly, which is what the
"surface bugs explicitly" rule asks for. The finding is that **no test could have caught this** — the premise
was never checked against a real boot log before the behaviour shipped.

### M6 — a guard test stayed green while the behaviour it guarded was broken
**Commits:** `1a55797df` → `e3296a7d2`
**File:** `ScriptTemplateContentTest.allTemplatesUseAnAlreadyInstalledFabricLauncherBeforeCheckingTheNetwork`
**Rule broken:** *tests must pin behaviour, not shape.*

`1a55797df` made `setupFabric` prefer an on-disk launcher and pinned it with a test asserting only that the
disk check appears *before* the network probe. The fix was half-complete — it `return 0`-ed past the closing
`SERVER_RUN_COMMAND=` assignment, so packs launched `java … do_not_manually_edit` and died — and the ordering
assertion stayed green the whole time. `e3296a7d2` fixed the fall-through and replaced the guard with one that
**executes** the extracted function, verified to fail when the early return is reinstated. Recorded because
"assert positions of substrings" is a test shape worth avoiding, not because the end state is wrong.

---

## LOW

### L1 — new `!!` in refactored code
**Commit:** `defbded12` · **File:** `BootVerifier.kt:143`
```kotlin
return reconcileRecheck(outcome, second, first.loaderVersion, newest!!)
```
**Rule broken:** *No new `!!` in refactored code — handle nullability explicitly.*
Provably safe (`shouldRecheckCrash` returns false when `newest == null`, and the early return above
guarantees it) — but the guarantee lives in another function. `val newestVersion = newest ?: return outcome`
would put it where the reader is. One occurrence in main sources; five in tests, where the convention is
laxer.

### L2 — twelve new class-scope `var`s in main sources
**Rule broken:** *Prefer `val`; immutable data by default.*
Most are legitimately mutable state (`GrinderStatus` counters, cursor and cache bookkeeping, `PathsConfig`'s
recomputing getters). Flagged for a pass, not as defects — worth confirming none are `var` merely because it
was convenient.

### L3 — inconsistent placement of documentation
Some behaviour commits carry their `CLAUDE.md` / `REFACTOR-LOG.md` updates (`aca721725`, `1783eb7c8`,
`3c2bb2759`); others defer them to a following `docs:` commit (`0351f3c79`, `47df8086d`, `e2dd33ca3`,
`85cc20b54`). Either is defensible; alternating makes the log harder to read and leaves `REFACTOR-LOG.md`
occasionally describing a commit that is not yet in the branch.

---

## What the branch got right

An audit listing only faults would misrepresent the work.

- **Bugs were surfaced, never silently worked around.** Every mid-stream defect got an explicit record:
  B8–B12 in `claude-docs/BACKLOG.md`, landmine sections in the clientside and grinder `CLAUDE.md`s, and a
  correction paragraph in `REFACTOR-LOG.md` for each hypothesis that proved wrong — both the "missing
  dependencies cause false HIGHs" theory and the NeoForge `21.1.247` theory were checked against the store
  and retracted rather than quietly dropped.
- **The revert (`d8b6cb157`) is textbook** — fast, total, honest about the premise.
- **Guards were verified by breaking them.** The exit-status test, the killed/OOM guard, the Fabric
  fall-through and the launch-failure guard were each confirmed to *fail* when the fix was reverted, rather
  than merely observed passing.
- **Extractions stayed behaviour-preserving** with existing suites green: `neoForgeVersionPatternFor`,
  `forgeVersionFrom`, `refuseForMissingDependencies`, `BootWorkspaceReaper`.
- **19 new mapping tests** now cover logic that previously had none, and the suite is green at HEAD across
  all four modules.

---

## One thing this audit could not settle

`BootLogClassifier.classify` (`BootLogClassifier.kt:159-190`) is now **seven ordered guards** — ready-line →
timeout → setup-abort → launch-failure → killed/OOM → client-only-class → dependency-failure → exit code —
and its correctness rests entirely on that order. It accreted across four commits (`34b5cbb6e`, `aca721725`,
`8ebb3bbcb`, and the original), each added in reaction to a live false positive. Every individual ordering
constraint is tested, but **no test asserts the ordering as a whole**, and no single commit ever presented
the finished decision table for review. It is the highest-risk unit on the branch and the one most deserving
a hostile read by someone who did not write it.

---

## Remediation — applied 2026-07-31 on Griefed's go-ahead

| Finding | Status | Commit |
|---|---|---|
| **M1** checked-in test properties rewritten by the suite | **Fixed** — both files restored to `develop` content; all 37 call sites across 28 test files now read `build/resources/test/serverpackcreator.properties`, the copy `processTestResources` already produces, so writes land in build output. Proven by running all four suites and confirming no unstaged change to the source files. | `ef3280e4c` |
| **L1** new `!!` in refactored code | **Fixed** — explicit `newest == null ||` in the same condition smart-casts it away; behaviour-preserving, existing assertions unchanged. No `!!` remains in `-clientside` main sources. | `60541158a` |
| *Open item* — `classify`'s guard order untested as a whole | **Fixed** — each case pairs a higher-priority signal with a lower-priority one and asserts the higher wins, the only shape that makes a reorder fail. Verified by moving the client-class guard above the launch-failure guard: 2 tests fail. | `6ea977087` |
| **H2** plugin-visible behaviour change unrecorded | **Documented** — the API compatibility policy in `CLAUDE.md` now states that source-compatible is not behaviour-compatible, and tables the two changes this branch made. | see below |
| **L2** twelve new class-scope `var`s | **Reviewed, no change** — all are loop-local accumulators or deliberately mutable state (`GrinderStatus.pass` is `@Volatile`; the two `private var` lists in `CurseForgeCandidateSource` are lazily-populated caches). No `val` candidates. |  |
| **H1** `aca721725` bundled a refactor with five behaviour changes | **Not remediable retroactively** — rewriting landed history would be worse than the finding. Its *consequence* was already closed by `8ebb3bbcb`. Process finding: split. |  |
| **M2** `28a786b58` shipped untested | **Closed by follow-up** — characterization landed in `5703b0340`; 19 mapping tests now cover it. |  |
| **M3** `test(...)` commits carrying extractions | **Historical** — labelling only; the extractions are behaviour-preserving and green. |  |
| **M4** three oversized commits | **Historical** — process finding, nothing to change in the tree. |  |
| **M5** `LoaderSupportMemory` on a false premise | **Already reverted** by `d8b6cb157`. |  |
| **M6** guard test green while behaviour broken | **Already fixed** by `e3296a7d2`, which replaced the ordering assertion with one that executes the function. |  |
| **L3** inconsistent doc placement | **Accepted** — both patterns are defensible; not worth churning history over. |  |

**Suite after remediation:** api ✅ · clientside ✅ · grinder ✅ · app ✅

**Report complete.**
