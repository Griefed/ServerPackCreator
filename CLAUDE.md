# ServerPackCreator — Claude Code context

> **Purpose of this file:** the durable, *current-state* context for Claude Code sessions on
> ServerPackCreator. Read it before touching code.
>
> - Per-sprint **narrative** history → `git log` and `claude-docs/REFACTOR-LOG.md`.
> - **Deferred-but-agreed work** → `claude-docs/BACKLOG.md` (why it waited + context to pick it up cold).
> - **Behaviour changes on the published API** → `claude-docs/API-BEHAVIOUR-CHANGES.md` (one row per
>   change, what an embedder sees). The *policy* stays below; that file is its evidence.
> - **CI secrets — what each one is, its scopes, and which job dies without it** →
>   `claude-docs/CI-SECRETS.md`. Read it before touching a `secrets.*` reference: Forgejo rejects the
>   `FORGEJO_`/`GITEA_`/`GITHUB_` prefixes, so the credentials are `FJ_*`/`GH_*` on purpose.
> - Module-specific facts, patterns and landmines → each module's own `CLAUDE.md`
>   (lazy-loaded by Claude Code when you work in that module).
> - Personal working preferences (general approach, organization, no-shortcuts ethos,
>   communication style) reach a session from the **user's own settings**, not from this repo — they
>   deliberately don't ship in a public shared file. Follow them as given; they outrank style choices here.
>
> **Engineering principles (binding):** KISS, MVC, TDD, SOLID. Operational naming, documentation,
> module-boundary and Kotlin rules are in **## Conventions** below — follow them for all code.

---

## Module map

What each module *is* comes from `settings.gradle.kts`, `README.md` and the module's own `CLAUDE.md`
(lazy-loaded when you work there). What is listed below is only what those cannot tell you: the
constraints.

- **`serverpackcreator-api` is published to Maven Central, so its public surface is a compatibility
  constraint** — plugins compile against it. Governed by the **API compatibility policy** below.
- **Every other module is unpublished and therefore churns freely** (`-clientside`, `-app`,
  `-grinder`, both plugin modules, the frontend). `-clientside` in particular is free to change shape;
  `-plugin-example` is the exception that must always reflect *current* API idiom, because it is
  documentation by example.
- **Dependencies point inward toward `-api`, never outward** — see **Module boundaries** below.
- Not in the Gradle build, so `settings.gradle.kts` will not mention them:
  `serverpackcreator-help` (docs), `buildSrc`, `docker`, `misc`.
- **CI lives in `.forgejo/workflows`** — Forgejo (`git.griefed.de`) is the canonical CI and the origin of
  every release; `.gitlab-ci.yml` is gone. The wiring, the all-or-nothing `.forgejo`/`.github` landmine and
  the two deliberately-dropped GitLab capabilities are in `.claude/rules/ci-workflows.md`, which loads when
  you touch a workflow. Secrets, scopes and which job dies without which → `claude-docs/CI-SECRETS.md`.
- **`serverpackcreator-help/Writerside/api-docs.yaml` is GENERATED, not hand-maintained** — springdoc
  is wired into `-app` as `developmentOnly`, and the regeneration command sits beside that dependency
  in `serverpackcreator-app/build.gradle.kts`. It had drifted to 25 of 44 endpoints while being edited
  by hand, including two schemas for classes that no longer existed. Regenerate it rather than patching
  it, and regenerate it again after any change to a controller or an entity it serialises.

## Build & test commands

- `./gradlew build` — full build. The app build depends on the frontend build and license report, and
  since 2026-08-14 it also **runs the frontend's Vitest suite** (`checkFrontend` → `npm run test`). Before
  that, `checkScript` was unset, so the plugin SKIPped `checkFrontend` and a green `build` had executed
  zero frontend tests while still compiling and bundling the SPA.
- `./gradlew :serverpackcreator-api:test` — API suite (runs against fixture modpacks in
  `serverpackcreator-api/tests/` and `src/test/resources/testresources/`). **Offline for every Minecraft version in
  the shipped manifest snapshot**, which `ApiWrapper.setup()` seeds from the jar; a version newer than that snapshot
  costs one fetch of its `mcserver/<version>.json`. The snapshot **no longer lags its own parent
  manifest** — the release named in `minecraft-manifest.json`'s `latest.release` now has a matching
  `mcserver/<version>.json`, which is the check worth re-running rather than trusting a file count. That was the open
  deferral B25, closed as a side effect of the `updateManifests` retarget. The test home is wiped before each run **except** `manifests/`, so that cache persists and accumulates.
- `./gradlew :serverpackcreator-app:test` — app suite.
- `./gradlew :<module>:koverHtmlReport` / `koverXmlReport` — coverage (Kover), report under
  `<module>/build/reports/kover/`.
- Frontend: `npm install && npx quasar dev` in `serverpackcreator-web-frontend/` (dev server),
  `npx quasar build` for production build, `npm test` (Vitest).
- Run the app locally: `./gradlew :serverpackcreator-app:bootRun` (GUI by default; CLI/web via args,
  **not `:run`** — `-app` applies the Spring Boot plugin, not `application`, so `run` does not exist there;
  `:serverpackcreator-grinder:run` does, because the grinder applies `application`; see `Mode.kt` /
  `CommandlineParser.kt` for the arguments).
- `media` task needs install4j installed locally — not part of regular dev loop.

### Build layout

**Where every build declaration lives, and the landmines protecting them, are in
`.claude/rules/build-layout.md`** — it loads whenever you touch `build.gradle.kts`,
`settings.gradle.kts`, `buildSrc/`, or the version catalog. Read it before changing any of those:
it is the difference between a two-line bump and re-introducing a failure this project already paid
for. `BUILD.md` is the contributor-facing tour of the same ground.

## Branching & git workflow

- PRs target **`develop`**; `main` is the release branch.
- One branch per feature/fix, prefixed `claude-` when created by Claude. Group related fixes.
- **Never push. Keep all changes local — the user pushes.**

## API compatibility policy (adopted default — Griefed may override)

- The plugin-facing API (everything `serverpackcreator-api` exports, esp. `plugins`,
  `ApiWrapper`, `PackConfig`) stays source-compatible within a major version.
- Refactors keep old entry points as thin deprecated facades (`@Deprecated` with `ReplaceWith`)
  for at least one major release before removal.
- Internal-only types may move/change freely once they are no longer exported.
- **Source-compatible is not the same as behaviour-compatible.** A change that keeps every signature but
  alters what an exported call *returns* is still a contract change for embedders, and belongs in the
  release notes even though nothing fails to compile.

**The behaviour-change record lives in `claude-docs/API-BEHAVIOUR-CHANGES.md`** — one row per change,
with what an embedder actually sees. Append to it whenever you change what an exported call *does*,
and read it before answering "will this break an embedder?". It is out of this file because it is
evidence consulted occasionally, not context every session needs.

---

## Conventions

- **"Make it work, make it right, make it fast." — Kent Beck.** A more detailed variation often cited is
  *"First, make it. Then, make it work. Lastly, if you can, make it pretty."* The sequence exists to head
  off perfectionism and analysis paralysis: functionality comes before form, and the core logic has to be
  solid before anyone spends effort on readability or speed.
  - **Avoiding premature optimization.** Knuth's "root of all evil" — you cannot predict bottlenecks
    without a working system to measure. This project has the receipts: B30 was a real 121,492-byte
    saving per startup that bought **~0 ms**, because the twelve manifest checks run concurrently and the
    slowest one gated the batch. Measured, it was the wrong thing to optimise; the right one (B31, taking
    the refresh off the startup path) was ~392 ms and only visible once something was running.
  - **Managing technical debt.** Shortcuts may be taken first, but the bargain is that you come back and
    polish. Many developers argue "fix it later" is a myth, and that is the risk this convention set
    exists to contain — which is why `claude-docs/BACKLOG.md` demands a *stated reason* per deferral and
    enough context to pick it up cold, rather than a wish-list.
  - **Iterative improvement.** A messy first draft, then refinement.

  **How this squares with TDD and "no shortcuts", which it looks like it contradicts:** the ordering is
  about which *concern* you attack first, not permission to skip pinning. "Make it work" is what the
  characterization test asserts; "make it right" and "make it fast" are the steps the test then protects.
  Read the other way round it licenses exactly the failure this file already documents at length — the
  performance branch whose tests were written by the same pass that changed the code and therefore passed
  by construction. Draft messily, but pin before you refine, and never let "make it fast" arrive before
  there is something whose behaviour is known.

- **A published verdict must rest on decisive evidence, and the report must say which rung produced it.**
  `CRASHED` is reachable both from a marker no broken harness can fabricate and from a bare non-zero exit that
  means only *"nothing recognised why"* — and until `BootDecision` those were indistinguishable downstream, so
  the grinder published both alike. Measured against the live daemon on 2026-08-31: **27 of 43 published
  `HIGH` verdicts rested on no decisive evidence**, and one poisoned loader-cache entry (`NeoForge 21.11.45 /
  MC 1.21.11`) had produced identical failures across all **90** boots against it — a library mod and a
  server-side building mod among them. Two lessons generalise beyond the grinder: **a verdict that cannot name
  its own evidence cannot be audited**, and **an environment defect looks exactly like a subject defect unless
  something distinguishes them**.
- **Question the requirement before you optimise the cost of meeting it.** On 2026-09-02 a circuit breaker
  was designed, pinned with 189 lines of guards, implemented, wired through two modules and documented — and
  deleted 34 minutes later, when Griefed asked whether the thing it protected was needed at all. It was not:
  the route it bounded existed only to circumvent CurseForge's distribution opt-out, had stopped working
  entirely, served under 1% of candidates, and cost 192.9 MB in every artifact. Every commit in that sequence
  was correctly shaped, which is exactly why the shaping did not save it. The evidence to ask the prior
  question was already in hand. This is Knuth's rule one level up: *measure before optimising* presumes the
  thing should exist, so establish that first — "should this code exist?" is cheaper to answer than "how do I
  make its failure cheap?", and one of the two answers deletes the other's work.

- **Cite names, not snapshots.** Three consecutive audits of the performance branches found the same
  defect class and nothing else: a fact quoted in prose going stale the moment the code moved — 54 commit
  hashes killed by a rebase, a landmine still describing a flaw that had been fixed, a line number shifted
  by the very commit that cited it, and suite counts left behind by the tests that were just added. **It
  recurred on 2026-09-01:** that rebase killed 13 more hashes in `claude-docs/REFACTOR-AUDIT.md`, five of
  which are now reachable from no ref at all and will stop resolving entirely once gc runs. That file cites
  hashes at volume, so it is the guaranteed casualty of every history rewrite — write subjects there the
  first time. Prefer the **commit subject** over its hash (subjects survive rebase, cherry-pick and squash),
  the **symbol name** over `File.kt:123`, and "what the guard asserts" over "how many tests exist". Where a
  number genuinely earns its place — a measurement, a byte count — say what produced it, so a reader can
  re-run it instead of trusting it.

- **Fix bugs when you find them and read before you advise.** A defect noticed in passing gets surfaced
  and fixed in its own commit, never deferred as "out of scope"; a claim about this codebase gets checked
  against the code or the docs first. (The other two bullets that used to sit here — the principles list and
  the git workflow — are stated in the header and in **## Branching & git workflow**.)

### Module boundaries (architecture — SOLID / MVC)

- `serverpackcreator-api` is the domain core. It must **not** gain compile dependencies on Swing,
  Spring web, or the web frontend. Dependencies point inward toward `-api`, never outward; the app
  and frontend are adapters around it. Flag any inward-pointing violation.
- Domain logic in `-api` must be unit-testable **without** booting a Spring context. A class that
  needs the Spring container to be tested is a design smell — flag it.
- (Plugin-API stability is governed by the **API compatibility policy** above — don't duplicate it.)

### Refactor discipline (behavior-preserving by default)

- One logical concern per commit. Keep "add tests", "pure refactor (no behavior change)", and
  "change behavior" in **separate** commits. Related behavior changes may be grouped.
- Pin current behavior with characterization tests **before** restructuring legacy code; refactor
  in small steps behind source-compatible facades (Strangler-Fig), keep the suite green at every
  commit.
- A pure-refactor commit must keep the **existing** assertions green. If a test must change for a
  "refactor", that's a signal the change is **not** behavior-preserving — stop and flag it.
- Boy-Scout rule: leave touched files cleaner than you found them, but stay within the commit's
  stated scope; don't let cleanup sprawl into unrelated files.
- **Shell templates, manifests and version parsing get their test written and observed failing FIRST.**
  Not because the rule is different there, but because these fail *silently* — a wrong branch or a
  mis-sliced version produces a plausible value, not an error — so they get verified by hand and pinned
  afterwards, if at all. Two audits found three instances (`28a786b58` NeoForge mapping, no test;
  `2e16bf0c8` fish `--no-empty`, test five commits later; `1a55797df` Fabric fall-through, whose guard
  asserted *ordering* and stayed green while the behaviour was still broken). The cost is measurable:
  the Forge launcher-era bug wasted **24 boots** before anything noticed, and 820 NeoForge versions were
  mis-attributed to one Minecraft release.
- **A test that only asserts shape is not a pin.** Prefer *executing* the unit — `ScriptTemplateContentTest`
  extracts a shell function and runs it — over asserting substring positions or that a symbol exists. And
  **confirm the test fails before the fix**: a guard whose teeth were never checked has repeatedly turned
  out to assert nothing (twice in one session, when a mis-indented edit meant the "broken" run was
  actually unmodified code).
- **A performance branch is not done until it is proven equivalent to its base.** Green tests are not that
  proof: they are HEAD's tests, written by the same pass that changed the code, and they pass by construction.
  The check that *is* proof is cheap and repeatable — run the **base branch's unmodified test tree against the
  branch's production code**:

  ```
  git worktree add --detach <tmp> HEAD
  cd <tmp> && rm -rf <module>/src/test && git checkout develop -- <module>/src/test
  ./gradlew :<module>:test --continue
  ```

  Every failure is either a regression or a deliberate change; every *compile* error is a signature change,
  which is a finding in itself and must be enumerated rather than worked around. Done for this branch
  (`claude-docs/REFACTOR-AUDIT.md` iteration 7): **490 pre-existing guards, zero failures**, with exactly two files
  uncompilable — one adapted by adding two constructor arguments and *no* assertion edits (7 guards green), one
  legitimately unadaptable because it asserted behaviour the branch removed. Also check *which* changed classes
  the base's tests actually name, so the residual risk is stated rather than assumed; a class-name grep
  under-reports, since `QuiltPackScanner` is exercised only through `ModScannerSidenessTest`.
- **What only a real runtime can answer, ask a real runtime.** Anything whose point is what an external system
  does — an index, a data migration, a REST response shape — is not verified by a mocked test, however good.
  Iteration 7 ran the actual `bootJar` in `-web` mode against MongoDB 8.0.5 in Docker, seeded with pre-branch
  shaped documents, and that is what confirmed the `sha256` index really exists, the migration really converts
  legacy documents and really skips already-migrated ones, and `/api/v2/runconfigs/all` really returns the
  documented shape. It also surfaced what was filed at the time as B33 — the web application
    writing to MongoDB's default `test` database instead of the configured one, which no test could have caught, and
    which the Spring Boot 4 property-key fix has since closed. Cost: about fifteen minutes.
- **Build logic is verified by measurement, not by tests — and the measurement goes in the commit message.**
  `buildSrc` has no test source set and no Gradle TestKit harness, and we have decided not to add one to pin single
  predicates (a task-wiring change or a one-line filter is not worth a second test framework in the build). So for a
  change to `buildSrc`, a `build.gradle.kts` or task wiring, the standard is: **measure the behaviour before and
  after, and record both numbers in the commit message.** `8b87057cf` did this (a planted marker plus a cached
  manifest; the cache went 643 → 0 before the change and survived after), as did the `updateManifests` retarget (app
  home 643 files vs api home 659, the difference being exactly the 16 releases that could never have been copied).
  Two audits flagged these as missing pins; this is the deliberate ceiling, so state it rather than re-flag it. Where
  a *consequence* is reachable from a normal suite, pin that instead — `ShippedManifestSnapshotTest` guards the
  outcome of the manifest work even though nothing can guard `cleanup()` itself, because `ApiWrapper.setup()`
  re-seeds from the jar and makes a wiped cache indistinguishable from a preserved one at test time.
- **Pin first means *commit* first, not just write first.** The failing test lands in its own `test(...)`
  commit, **red**, and the fix follows in the next one. In-session verification is not a substitute: it leaves
  no evidence, and it is exactly what silently passed twice above. Audited 2026-07-31 — all **eight** code
  commits of that day's plan (`2a9a03473`, `30f6cbded`, `c571e2d7f`, `07a647f01`, `aa2d27f7f`, `91ac0e1a9`,
  `1f92f585c`, `5caa6833f`) bundled guard and change, so nobody can check out `2a9a03473^` and watch the pin
  go red. The tests were written first; only the boundary collapsed, which is the part that costs nothing to
  keep and everything to reconstruct later.
- **Run the pin before you commit it red, and read *why* it failed.** Committing a guard red is only
  evidence if the red is the missing implementation. Twice on 2026-09-01 it was not: a fixture omitted a
  collaborator, so four asserted field paths could not have resolved even against correct code; and a Kotlin
  `${'$'}` escape produced the literal `...-v${'$'}version`, so the guard asserted a string no implementation
  would ever return. Both were fixed in the very commit that was supposed to turn them green, which quietly
  undoes the boundary the previous rule exists to create — `git checkout <fix>^` then shows a failure that is
  partly the guard's own fault. One run before `git commit` distinguishes the two, and costs seconds.

- **`refactor:` is a claim about behaviour, not about intent.** Use it only when behaviour is preserved; label
  a behaviour change `fix:` or `feat:` however tidy it looks. If an **existing** test's *assertion, argument or
  expected value* has to change, the label is already wrong — that is the stop-and-flag signal, not a formality.
  **Carve-out: a reference-only update is not the signal.** Moving a symbol between modules necessarily updates
  imports and receivers in its tests, and Strangler-Fig moves are exactly what the conventions ask for — so
  `- Old.isSuspendGap(gap, poll)` / `+ New.isSuspendGap(gap, poll)`, with every assertion byte-identical, stays a
  `refactor:`. Judge the diff, not the file list: if no expectation changed, the test did not change in the sense
  this rule means. (Written after the rule's first draft flagged `b6b778b82`, a clean cross-module move, as
  mislabelled — a convention that cries wolf on legitimate refactors gets ignored wholesale.) Three commits got
  this wrong: `5f138ef8a` (`refactor(app)`) moved four call-sites onto a *resolved* Preferences node, changing where
  any host with its own node reads and writes, and had to edit `CommandlineParserTest`; `7815d5960`
  (`refactor(api)`) added an operator-editable template path plus a delete-watcher branch in `-app`; `358675fbf`
  (`refactor(ci)`) stopped 13 of the pipeline's 20 jobs from starting a dind service, cutting ~34 s off each. All
  three described the change honestly in the body — only the type lied. **`358675fbf` also shows why the label is
  worth getting right the first time:** it was audited only after being merged into `develop` and `alpha`, at which
  point the honest remedy is this entry, because the alternative is force-pushing two shared branches. The rule is
  cheap before the merge and unfixable after it.

### Kotlin idioms

- Prefer `val` over `var`; immutable data by default.
- `data class` for value types, `sealed class` + exhaustive `when` for state, Kotlin null-safety
  instead of defensive null checks. Don't port Java patterns 1:1.
- No **new** `!!` non-null assertions in refactored code — handle nullability explicitly.
- **Naming — speaking names:** variables, parameters, constants, types and methods get names a
  reader can derive meaning and context from (`configRepo`, `attemptCount`, `tokenOverridesJSON`),
  not single letters. The one carve-out is the throwaway loop counter; it generalises only to a
  small, closed set of established Kotlin idioms whose meaning is universal and whose scope is a few
  lines. The line is "would a newcomer have to scroll up to learn what this is?" — if yes, name it.
- **Documentation — comment everything:** every function, method and exported constant carries a
  doc comment — unexported ones too. State briefly WHAT it's for and HOW it achieves it, not a
  restatement of the signature. One or two sentences is the target; needing more is a sign the unit
  is doing too much (KISS). Keep comments truthful as code changes — a stale comment is worse than
  none.
- **Documenting a single-line constructor means reshaping it, and that reshape is in scope.** Per-parameter KDoc
  cannot attach to a parameter sharing a line with others, so `data class X(val a: A, val b: B)` has to become one
  parameter per line before each can be documented. Four declarations were reshaped that way on 2026-07-31
  (`ContainerRunOutput`, `CatalogCursor`, `Exclusion`, `Dependency`); an audit flagged it as scope creep, which it is
  not — it is the enabling change. Keep it to the declaration being documented, and verify the obvious: parameter
  names, types, order and defaults must survive untouched. The alternative, a class-level `@param` block, leaves the
  properties themselves undocumented as far as dokka is concerned.
- **A doc that only restates the signature is barely better than none — but silence is worse.** For a pure accessor
  over a well-named field there is often nothing to add, and dokka flags the omission either way. Put the shared
  meaning where it belongs: `ServerPackConfigTab`'s accessors were labels until the *interface* doc explained that
  they read live tab state rather than the saved configuration, which is the fact every one of them depends on.
  Prefer one honest paragraph on the type over forty restatements on its members.
- **Errors:** always handle, never ignore with `_` unless intentional (comment why).

## Definition of done (per change)

1. Tests written first and green (`./gradlew :<module>:test`).
2. Doc comments per **## Conventions** on every new/changed unit.
3. No new compiler warnings; stale comments updated.
4. CLAUDE.md "Refactor state" (and the relevant module `CLAUDE.md`) updated when an architectural
   step lands. Append the blow-by-blow to `claude-docs/REFACTOR-LOG.md`, not here.

---

## Refactor state (living — current snapshot only; full history in `claude-docs/REFACTOR-LOG.md`)

**Goal:** KISS/MVC/TDD/SOLID across api → app → plugin-example → web-frontend.
**Phases:** 0 baseline · 1 API · 2 app · 3 plugin-example · 4 frontend.

**Current status (2026-09-11).** Counts are a snapshot and go stale — re-derive them from
`<module>/build/test-results/test/*.xml` after a run rather than trusting the column:

| Module         | Tests         | State — detail and landmines live in the module's own `CLAUDE.md` |
|----------------|---------------|------------------------------------------------------------------|
| api            | 421 (1 skip)  | Phase 1 complete. → `serverpackcreator-api/CLAUDE.md` |
| clientside     | 635           | The clientside-mod verification engine; six verdicts. → `serverpackcreator-clientside/CLAUDE.md` |
| app            | 149           | Phase 2 largely complete; CLI verbs stay, engine extracted out. → `serverpackcreator-app/CLAUDE.md` |
| plugin-example | 3 (from 0)    | Phase 3 complete. → `serverpackcreator-plugin-example/CLAUDE.md` |
| plugin-grinder | 75            | GUI plugin over a grinder daemon. → `serverpackcreator-plugin-grinder/CLAUDE.md` |
| web-frontend   | 32 (from 0)   | Phase 4a-4e complete; full TS migration. → `serverpackcreator-web-frontend/CLAUDE.md` |
| grinder        | 529 (29 skip) | Continuous boot-verification daemon. → `serverpackcreator-grinder/CLAUDE.md` |

Key size reductions (all behind source-compatible facades): `ApiProperties.kt` 3,007 → 1,372;
`ConfigurationHandler.kt` 1,562 → 897; `ServerPackHandler.kt` 1,466 → 490.

**Remaining hotspots:** `ConfigEditor.kt` 1,369 — assessed: the two pure pieces (dirty-check,
Java-version) are extracted; the rest is legitimate Swing view code, not worth mechanically
splitting. `LarsonScanner.kt` 2,217 — self-contained widget, low priority.

**Open issues (details + locations in the relevant module `CLAUDE.md`):**
The GUI `GlobalScope.launch` anti-pattern (app) is **resolved** — all 26 sites now use
`gui.utilities.ComponentCoroutineScope` (lifecycle-cancelled), GUI-verified. The frontend's
settings-store `$q` coupling (4b) and `jsconfig.json`/TS gap (4c) are **resolved**.

**Current phase — 4 (frontend) complete; GUI structured-concurrency done.** Frontend 4a–4e: Vitest,
settings-store `$q` decoupling, full TypeScript migration (all `src/` is TS, verified by
`quasar build`), a Quasar component test harness (Vue Test Utils), and broadened component coverage
(all cards + nav SFCs; suite at 32 across 14 files; tables left untested by design — trivial format-lambda logic vs.
brittle QTable rendering). The GUI `GlobalScope.launch` anti-pattern is resolved (see Open issues),
GUI-verified. **Next (optional):** broaden component-test coverage further.

**Lessons that generalise beyond their incident.** The narratives they came from — the grinder plugin
(2026-09-06), the `ERROR` bucket (2026-09-09) and the `UNVERIFIABLE` bucket (2026-09-10/11) — are in
`claude-docs/REFACTOR-LOG.md`; the module-specific detail is in each module's own `CLAUDE.md`.

- **A defect whose multiplier is the count of something the repo only ever has one of cannot be found by
  testing what the repo ships.** One installed plugin made a per-plugin loop invisible for months.
- **A fixture installed *after* the thing it is meant to exercise has already run is not a fixture.** The
  api suite stayed green for months against an unbounded plugin-loading recursion for exactly that reason.
- **A category defined by its consequence will accumulate everything with that consequence, whatever the
  cause** — so define it by the cause and make the type carry it (`PreventionCause`), not a sentence.
- **A defect whose evidence is a published report can be diagnosed without touching the host.** The
  report's wording is part of the evidence, which is the argument for it being precise.
- **A test can pass against unfixed code because one fixture value is a prefix of another.** Ask why a
  guard *passed*, not only why it failed, whenever fixture values could contain one another.
- **A guard that cannot compile is not a red pin.** Land the seam first as its own behaviour-preserving
  commit, or say in the message that the boundary is missing and quote the mutation that reproduces the red.
- **Duplicated knowledge drifts toward whichever copy is easier to reach** — three instances so far. Delete
  the duplicate rather than correcting it, and ask of any new lookup table which existing one already
  answers it.
- **An axis chosen for how work is *produced* asks the same question repeatedly and never asks the others.**
  The grinder ground a mod once per modloader because that is how a boot is parameterised — and spent 3.06
  boots per project on a mean of 1.6 distinct Minecraft eras, never booting `aether`'s 1.12.2 build at all.
  The axis worth keying on is the one along which the *answer* varies.
- **A report row that names a thing it did not use cannot be audited, and reads as a different bug.** The
  aether row named `aether-1.12.2-v1.5.4.1.jar` while its dependency failure belonged to the 1.20.1 jar
  staging had actually selected; checked against the platform page, that reads as broken dependency
  resolution rather than as broken attribution. Two selections for one row is the defect — make one.
- **Changing a key's shape is not the same as changing its value, and a one-hop migration does not
  generalise.** `supersededLegacyKey` computes the superseded key from fields the new row still carries,
  which works when the mapping is one-to-one and cannot work when several rows collapse into one. Removal by
  *prefix* is the shape that generalises, and it is why a key component worth migrating past needs a marker
  in it.
