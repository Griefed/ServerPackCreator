# Refactor audit — `claude-phase4-frontend`

**Scope:** `git log develop..HEAD` — 14 commits (base `ff1daab4d`). Read-only audit against the
Refactoring Conventions. No source was modified.

**Commits audited (oldest → newest):**

| Commit | Subject | Kind |
|---|---|---|
| c56c8f825 | Phase 4a: stand up Vitest; test+clean settings store | tests + cleanup |
| 477737d49 | docs: restructure CLAUDE.md + REFACTOR-LOG | docs |
| a4fce41f5 | Phase 4b: decouple settings store from `$q` | bugfix + test |
| bed873110 | Phase 4c-1: TS infrastructure | tooling |
| 0c1f06587 | Phase 4c-2: boot/router/store/i18n → TS | refactor |
| 3c3efcd78 | Phase 4c-3: settings store → TS | refactor |
| 922a18e53 | Phase 4c-4: display components → TS | refactor |
| 5a2232d47 | Phase 4c-5: layout + pages → TS | refactor |
| a26c57828 | Phase 4c-6: SubmitModPackForm → TS | refactor + bugfix |
| 1e13c7011 | docs: mark 4c complete | docs |
| 68fd279d5 | docs(refactor-log): 4b/4c | docs |
| 90ae913da | Phase 4d: component test harness | tests |
| 786494bc8 | Phase 4d: ErrorsCard test | tests |
| 42c596eb6 | docs: mark 4d done | docs |

Phase 4 is frontend-only — **no `serverpackcreator-api` / plugin-API surface, Swing, or Spring code
was touched**, so there are no module-boundary or plugin-contract (HIGH-class) violations of that
kind on this branch.

---

## HIGH

### H1 — Bugfix mixed into a refactor commit (`a26c57828`, Phase 4c-6)
**File:** `serverpackcreator-web-frontend/src/components/SubmitModPackForm.vue:773-778`
**Rule:** "If you find a bug while refactoring, surface it explicitly and propose a fix in its own
commit." + "Never mix a refactor with a feature or bugfix."

The "convert SubmitModPackForm to TypeScript" commit also fixes a genuine runtime bug in
`onRejected`: the old `rejectedEntry.name` was always `undefined` (QFile `@rejected` emits a
`QRejectedEntry[]`, not a single entry), so the rejection toast read *"undefined is not a
ZIP-file"*. The fix changes observable behavior — it now reads `rejectedEntries[0]?.file.name` and
shows the real filename.

The bug **was surfaced** (called out in the commit message), which is good and avoids the
"silently worked around" trap. But the convention requires the fix in **its own commit**; here it
rides inside a ~135-line pure-refactor commit, so a reviewer bisecting the TS conversion cannot
separate "rename + type" from "changed the rejection message." Same commit also carries the
`Map → Record` data-structure swap (see L2) — another behavior-adjacent change folded into the
"refactor" label.

**Recommendation:** in future, land the `onRejected` fix as a standalone `fix:` commit (ideally
preceded by a test pinning the old/new message) before or after the TS conversion. No code change
requested now — flagged for process.

---

## MEDIUM

### M1 — Components refactored without characterization tests first (cross-commit)
**Commits:** `0c1f06587` (4c-2), `922a18e53` (4c-4), `5a2232d47` (4c-5), `a26c57828` (4c-6)
**Rule:** "Before refactoring any unit, ensure characterization tests exist that pin its current
behavior… Never refactor untested code blind."

The only unit with a behavior-pinning test before it was refactored is the **settings store**
(tested in 4a/4b, converted in 4c-3 — that one is the model: existing assertions stayed green
unchanged). Everything else — all 21 SFCs plus the boot/router/i18n scaffolding — was converted to
TypeScript with **no tests pinning prior behavior**. The component test harness and the first two
component tests (`AboutItem`, `ErrorsCard`) only arrived in 4d, *after* the conversions, and cover
two presentational components. The units that received the most behavior-touching edits —
`SubmitModPackForm` (H1, L2), `ModPackDownload` / `ServerPackDownload` (L1, L3) — remain untested.

Mitigation present: `vue-tsc`, `eslint`, and a full `quasar build` gated every commit, and the
edits are individually small/disclosed. But compile-time checks do not pin render/runtime behavior,
so "behavior-preserving" here rests on inspection, not on a green characterization suite.

**Recommendation:** add component tests for `SubmitModPackForm` (esp. `onRejected`, the modloader
dropdown wiring, the picker dictionaries) and the download pages before further changes.

### M2 — Multiple concerns in one commit (`c56c8f825`, Phase 4a)
**File:** `serverpackcreator-web-frontend/src/stores/setting-store.js` (now `.ts:51`)
**Rule:** "One concern per commit. Keep 'add tests', 'refactor (no behavior change)', and 'change
behavior' in separate commits."

This single commit bundles three concerns: (a) the Vitest toolchain + first tests, (b) dead-code
removal (the `doubleCount` getter referencing a non-existent `counter`), and (c) a contract change
— `refresh()` now `return`s its promise. (c) is a behavior/contract change (made so tests can await
it); (b) is a refactor. Per the convention these belong in separate commits from "add tests."
Low blast radius (the sole caller didn't await, dead getter was unreachable), hence MEDIUM not HIGH.

---

## LOW

### L1 — Behavior-adjacent value edits inside "convert to TS" commits (4c-4, 4c-5, 4c-6)
Disclosed in the commit messages and plausibly behavior-equivalent, but they change values/semantics
inside commits labelled as pure refactors, and are unverified by tests:
- `opacity: 0.75 → '0.75'`, `0.2 → '0.2'` (number→string) — `ErrorsCard.vue:46,54`,
  `RunConfigurationCard.vue`, `SubmissionPage.vue` (4c-4/4c-5).
- Added `field: 'download'` to slot-rendered action columns — `ServerPacksTable.vue:130`,
  `ModpacksTable.vue:101` (4c-4). Inert at runtime (slot overrides), but it is a data edit.
- `?? ''` fallbacks, `String(...)` coercions, and template `this.x → x` rewrites change edge-case
  evaluation (empty array → `''` vs previous `undefined`) — `ModPackDownload.vue`,
  `ServerPackDownload.vue`, `SubmitModPackForm.vue` (4c-5/4c-6).

### L2 — `Map → Record` data-structure swap folded into the TS refactor (`a26c57828`, 4c-6)
**File:** `SubmitModPackForm.vue:586,600-601`
`ref(new Map)` → `ref<Record<…>>({})` for `forgeVersions`/`neoForgeVersions`/`modPacks`/
`runConfigurations`. The reasoning (they were only ever bracket-accessed; the version maps are
overwritten by plain JSON) is sound and almost certainly behavior-identical, but it is a structural
change shipped under the "convert to TypeScript" label, untested. Ideally its own
"refactor (no behavior change)" commit.

### L3 — Debug `console.log` removal bundled into the TS conversion (`5a2232d47`, 4c-5)
**File:** `serverpackcreator-web-frontend/src/pages/ModPackDownload.vue` (`current()`)
Removing the stray `console.log(this.$route)` is a fine Boy-Scout cleanup and was disclosed, but it
is a (trivial) behavior change living in a "convert layout/pages to TS" commit rather than its own
cleanup commit.

### L4 — Latent config bug introduced then fixed across commits (`bed873110` → `0c1f06587`)
4c-1 shipped `tsconfig.json` with a `baseUrl: "."` that mis-rebased the `.quasar` path aliases; it
passed only because no `.ts` exercised the aliases yet. The fix (dropping `baseUrl`) then landed
inside the 4c-2 "pure refactor" commit. The infra commit was not self-correct, and the correction
rode in a refactor rather than a dedicated `fix:`. Minor.

### L5 — Existing test file edited inside a refactor commit (`0c1f06587`, 4c-2)
**File:** `serverpackcreator-web-frontend/test/stores/setting-store.test.js`
The "convert scaffolding to TS" commit edits the store test. Inspection confirms this is **only an
import-specifier update** (`boot/axios.js` → `boot/axios` + the matching `vi.mock` path) forced by
the `axios.js → axios.ts` rename — **no assertion changed**, so it does not trip the
"a test had to change ⇒ not behavior-preserving" rule. Noted for completeness; a co-located,
necessary edit rather than a real violation.

---

## What was done well (not violations)

- **Incremental / Strangler-Fig:** the TS migration was staged leaf-first across 4c-1…4c-6, each
  commit independently green (vue-tsc + eslint + vitest). No big-bang module rewrite — the largest
  single-file change (`SubmitModPackForm`) is one SFC behind its own interface.
- **4c-3 is the model pure-refactor:** the settings store was converted *after* it had tests, and
  those existing assertions stayed green **unchanged**.
- **4b** is a self-contained, explicitly-surfaced bugfix (the broken `$q` error path) in its own
  commit — the convention's preferred handling. Its only nit (test shipped with the fix rather than
  a separate "add tests" commit) is borderline and not separately logged.
- **Bugs surfaced, not buried:** every behavior change (H1, the `$q` fix, the `console.log`) is
  called out in its commit message rather than slipped in.
- **Docs cleanly separated:** all CLAUDE.md / REFACTOR-LOG churn lives in dedicated `docs:` commits.

---

## Summary

| Severity | Count | Items |
|---|---|---|
| HIGH | 1 | H1 (bugfix mixed into refactor — 4c-6) |
| MEDIUM | 2 | M1 (no characterization tests before refactor), M2 (multi-concern 4a) |
| LOW | 5 | L1–L5 |

The dominant theme is **process, not correctness**: behavior-touching edits (one real bugfix, plus
several disclosed behavior-equivalent tweaks) were folded into commits labelled "convert to
TypeScript," and the bulk of the SPA was refactored ahead of any behavior-pinning tests. No broken
module boundary, no changed plugin/API contract, no silently-buried bug.

---

## Remediation (this session)

All findings have been remediated. The commit hashes cited in the findings above are the
**pre-rewrite** hashes (the history as audited); that history was then cleaned by a non-interactive
rebuild (cherry-pick-from-base, since `git rebase -i` is unavailable here). The pre-rewrite tip is
preserved at branch `backup-pre-rewrite`, and the rewrite was verified to leave the final tree
**byte-identical** to it (`git diff backup-pre-rewrite HEAD` is empty) — only commit structure
changed. Tests stayed green (12/12).

**Tests added (forward fix for M1 / H1 coverage):**
- **M1 — RESOLVED.** The previously-untested units that received behavior edits now have
  characterization tests: `SubmitModPackForm` (`test/components/SubmitModPackForm.test.ts` —
  onRejected + modloaderSelected + selectedRunConfiguration incl. the unknown-id guard) and both
  download pages (`test/pages/*.test.ts` — filename derivation). Harness extended: Notify registered
  in `test/install-quasar.ts`, `assets` path-alias added to `vitest.config.js`. Suite 6 → 12.
  (Remaining untested SFCs — boot/router/i18n scaffolding and the presentational cards/tables —
  carry only mechanical typing changes; `ErrorsCard`/`AboutItem` already covered.)

**History cleaned (rewrite):**
- **H1 — RESOLVED.** The `onRejected` bugfix is now its own commit, *"Phase 4c-6 (fix): onRejected
  reads the rejected file from QFile's array"*, placed immediately before *"Phase 4c-6 (refactor):
  convert SubmitModPackForm to TypeScript"* — so the TS-conversion commit is behavior-preserving.
  The fix is additionally pinned by the regression test above.
- **M2 — RESOLVED.** The multi-concern 4a commit is split into *"Phase 4a (refactor): tidy the
  settings store"* (dead `doubleCount` removal + `refresh()` returns its promise) and *"Phase 4a
  (tests): stand up Vitest and characterize the settings store"*.

**LOW — no code defect, left as-is.** L1–L5 are behavior-equivalent edits (opacity-as-string, inert
`field`, `?? ''`/`String()` coercions, `Map → Record`, the removed debug log, the corrected
tsconfig) that are correct in the current tree and several are now exercised by the M1 tests; L2's
`Map → Record` is covered via `selectedRunConfiguration`. Their only sub-ideal aspect was *also*
being commit-hygiene (disclosed boy-scout edits inside refactor commits); these were judged not
worth additional history surgery beyond the HIGH/MEDIUM splits. None require a code change.
