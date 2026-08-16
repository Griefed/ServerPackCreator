# Refactor audit — session work merged into `develop` (fifth pass)

**Base:** `a7717e8a9` (pre-session `develop` tip) · **Head:** `c1c3430a2` · **Commits in range:** 23
**Date:** 2026-08-16

**Scope change from previous passes.** There is no feature branch left to audit —
`claude-coroutines-1.11-fallout` and `claude-mongo-version-doc` are merged and deleted, and the three
`backup-*` branches are deleted after verifying their content is fully superseded. This pass audits
the merged range on `develop`, which includes two merge commits and one commit authored directly by
Griefed.

> **The work is PUSHED.** `origin/develop` contains everything through the merge `3bcb62af7`. Every
> earlier pass assumed history rewriting was free because nothing had left the machine; that is no
> longer true. Findings below can only be fixed *forward*.

**Verdict: no HIGH. One MEDIUM (new, already fixed by Griefed), one MEDIUM (recurring, fixed by this
pass). All pass-four findings closed.**

---

## Range

| Commit | Subject | Note |
|---|---|---|
| `e55ba8947` … `57ba3f256` | 16 commits | audited in pass four — all clean |
| `9560ec0a0` | `docs: audit the coroutines/catalog branch, fourth pass` | docs |
| `dbcb80caf` | `fix(build): declare the Java compilations on dokka's HTML publication too` | **clean** |
| `8f3a51f0f` | `docs: close L3 in the audit after fixing it` | docs |
| `3bcb62af7` | `Merge branch 'claude-coroutines-1.11-fallout' into develop` | `--no-ff`, matches repo convention |
| `2611aed00` | `chore: Update-To-Date license agreement` | **Griefed's own — see M5** |
| `e3289e3bf` | `docs(app): re-verify the Mongo URI landmine at the versions now resolved` | docs |
| `c1c3430a2` | `Merge branch 'claude-mongo-version-doc' into develop` | `--no-ff` |

**Merge integrity verified**, not assumed: `git diff 8f3a51f0f develop` over every branch-touched
path is empty, and `git diff 2611aed00 develop` over both license artifacts is empty. Neither merge
dropped a contribution from either parent.

---

## MEDIUM

### M5 (NEW) — the branch changed the dependency set but never regenerated the tracked license report

**Rule broken:** Boy Scout / completeness — a change is not finished while a tracked artifact it
invalidates is left stale.
**Files:** `licenses/LICENSE-AGREEMENT.txt`,
`serverpackcreator-app/src/main/resources/de/griefed/resources/gui/LICENSE-AGREEMENT`

This project **tracks the generated license report in VCS** and ships a copy inside the app's
resources. The branch altered the resolved dependency set of every module — Spring Boot 4.0.6 →
4.1.0, Kotlin 2.3.20/2.4.10 → 2.4.10 everywhere, coroutines 1.10.2 → 1.11.0, jackson, log4j, junit,
mockk, the Mongo driver 5.6.2 → 5.8.0 — and **touched neither artifact in any of its 19 commits**:

```
git log a7717e8a9..8f3a51f0f -- licenses  …/gui/LICENSE-AGREEMENT   →  (empty)
```

Griefed regenerated and committed them himself in `2611aed00`, whose diff is unambiguously the
consequence of the catalog work (`kotlin-bom` and `kotlin-stdlib` entries dropped,
`kotlinx-coroutines-*` and `kotlinx-datetime` reordered).

**Why this is MEDIUM and not LOW:** the stale copy is *shipped to users* in the app resources. Had
Griefed not caught it, the released application would have displayed a license agreement that
misstates its own dependencies — a compliance-adjacent inaccuracy, not a tidiness one. The signal was
visible throughout the session: `git status` showed both files dirty after every `./gradlew build`,
and that was repeatedly dismissed as "regenerated build outputs" and discarded with `git checkout --`
rather than recognised as *the branch's own output that needed committing*.

**Status: FIXED by `2611aed00`** — by the maintainer, which is precisely the problem. Confirmed clean
now: a full `./gradlew build` at `c1c3430a2` leaves the working tree spotless, because the committed
report finally matches the resolved dependencies.

**Preventive note for the next dependency change:** if a bump alters resolution anywhere, run
`./gradlew generateLicenseReport` and commit both artifacts in the same change.

### M4 (RECURRING) — the committed audit was stale again

`REFACTOR-AUDIT.md` stated `Head: 57ba3f256 · Commits: 16` while `develop` stood at `c1c3430a2` with
23 commits in range, omitting the L3 fix, both merges and Griefed's license chore.

This is the third pass in a row to raise it, and the cause is structural rather than careless: the
file is a snapshot committed *into* the history it describes, so it is stale the moment anything
lands after it. Fixed by this pass. If it keeps mattering, the durable answer is to stop pinning a
`Head:` SHA in the document and describe the range instead.

---

## Closed since pass four

| ID | Finding | How |
|---|---|---|
| M1 | Kotlin commit mixed compiler bump with pure ref collapse | split into `f0bf0034e` + `2be03f8d0` |
| M2 | `[plugins]` commit mixed alias conversion with buildSrc classpath change | split into `a8f158865` + `6325735c9` |
| M3 | "Three leftovers" bundled pure and behavioural changes | split into `b4e6fe977` + `66c77c053` |
| L1 | Documentation bundling inconsistent | consolidated into `57ba3f256` |
| L2 | `./gradlew build` never run; Kover and frontend unexercised | now the standard; it caught the Kover/KGP break |
| L3 | dokka HTML publication's undeclared task dependency | `dbcb80caf` — 3/3 FAILED → 3/3 SUCCESSFUL |

### `dbcb80caf` reviewed on its own terms — clean

One concern; a genuine `fix:` for a behaviour change; measured before and after (3 of 3 runs each
way, 185 `index.html` produced); fixed in `dokka-conventions` so every consuming module benefits
rather than patching `-api` alone; and it corrected an *asymmetry* — the Javadoc publication already
carried the same `dependsOn`, so this was one bug fixed twice, half at a time. Configuring the two
publications together is the right structural answer.

Scope note: L3 was pre-existing and pass four explicitly placed it out of scope. It was pulled in at
Griefed's direction, which is an authorised scope expansion, not sprawl.

---

## Clean — verified this pass

- **No commit is labelled `refactor:`.** All 23 are `test:`, `build:`, `fix:`, `docs:`, `chore:` or
  merges; every behaviour change is labelled `fix:` or `build:`.
- **No existing test's assertion, argument or expected value was modified** anywhere in the range. The
  only test file touched is `ListUtilitiesTest.kt`, additively.
- **Both merges are `--no-ff`**, matching the repo's existing convention
  (`a7717e8a9 Merge branch 'claude-securitymanager-unknown-java' into develop`), and neither dropped
  content.
- **Branch hygiene:** no dangling work. Both feature branches were merged before deletion; all three
  `backup-*` branches were verified content-superseded (every difference was `develop` being ahead —
  the count-based leak guard, the narrowed imports, the L3 fix, the Mongo doc) before force-deletion.
- **`main` is untouched and 567 commits behind `develop`** — the expected state for a release branch
  sitting at `RELEASE: 8.1.1`.
- **Catalog hygiene:** 49 libraries, 12 plugins, zero unused aliases, zero unreferenced `[versions]`.

---

## Carried forward — now permanent

- **The two deliberately red commits are on `origin/develop`.** `e55ba8947` (parallelMap guards before
  their fix) and `e55ddfe8e` (catalog bump before the platform switch) are pushed. A `git bisect`
  across this range will land on a red commit twice, and that is no longer reversible without
  rewriting published history. The squash-merge option discussed in passes two through four has
  lapsed.
- **`parallelMap`'s published behavioural contract change** — Griefed's explicit decision, recorded in
  the API-compatibility table. An embedder whose lambda mutated shared state without synchronisation
  was previously serialised by accident and can now race. Worth a release-note line.
- **The Mongo driver moved 5.6.2 → 5.8.0** as a side effect of the Spring Boot bump. The
  autoconfiguration contract was re-verified with `javap` (`e3289e3bf`); query and codec behaviour
  were not, and this project runs Mongo in production containers.

---

## Verification at `c1c3430a2`

`./gradlew build` — **BUILD SUCCESSFUL, 91 tasks**, working tree clean afterwards. 741 JVM tests
(api 309, clientside 88, app 108, plugin-example 3, grinder 233; 0 failures, 20 skipped), every Kover
report, `bootJar`, both dokka publications, `sourcesJar`, `generateLicenseReport`, and the frontend
including the Vitest suite. `WebServiceContextTest` passes 4/4 — its `MongoSocketOpenException` trace
is expected and documented; the driver connects lazily and startup continues.

Only install4j's `media` task remains unexercised; it needs a local install4j installation and is
documented as outside the development loop.

---

## Remaining decisions for Griefed

1. **`develop` → `main` is a release cut, not housekeeping.** `main` sits at `RELEASE: 8.1.1`, 567
   commits behind. `.gitlab-ci.yml:221` fires on `main` when the commit title is not `RELEASE:…`, and
   the publish jobs are tag-gated (`:270`, `:290`). Merging would start that pipeline. **Not done** —
   it needs an explicit release decision.
2. **Two commits remain unpushed** on `develop` (`e3289e3bf`, `c1c3430a2`).
3. Nothing else outstanding. No remediation proposed.
