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
>   communication style) → `~/.claude/CLAUDE.md` (user level, applies to all projects), so they
>   don't ship in this public repo's shared file. **See that file before advising.**
>
> **Engineering principles (binding):** KISS, MVC, TDD, SOLID. Operational naming, documentation,
> module-boundary and Kotlin rules are in **## Conventions** below — follow them for all code.

---

## What is ServerPackCreator?

ServerPackCreator creates a server pack from any given Forge, Fabric, Quilt, LegacyFabric and
NeoForge Minecraft-modpack.

It is a Kotlin application and API: the API lives in `serverpackcreator-api`, the application in
`serverpackcreator-app`, the SPA in `serverpackcreator-web-frontend`.

---

## Module map

Each in-build module has its own `CLAUDE.md` with the details — the entries below are the map only.

- **serverpackcreator-api** — core library, published to Maven Central. Packages: `config`,
  `serverpack`, `modscanning`, `versionmeta`, `plugins` (pf4j API), `utilities`, plus `ApiWrapper`
  (composition root), `ApiProperties`, `ApiPlugins`. **Plugins compile against this module — its
  public surface is a compatibility constraint.** See `serverpackcreator-api/CLAUDE.md`.
- **serverpackcreator-clientside** — the clientside-mod verification engine (platforms, metadata +
  server-boot signals, downloaders, fallback-list editor). Depends only on `-api`; **not** published
  to Maven, so it churns freely. Reused by the app's CLI verbs and the planned grinder service. See
  `serverpackcreator-clientside/CLAUDE.md`.
- **serverpackcreator-app** — four apps in one module under `de.griefed.serverpackcreator.app`:
  `cli`, `gui` (Swing), `web` (Spring Boot 4 backend), `updater`. Entry point `ServerPackCreator.kt`
    + `Mode.kt`. See `serverpackcreator-app/CLAUDE.md`.
- **serverpackcreator-plugin-example** — pf4j example plugin exercising every extension point.
  Documentation-by-example: must always reflect current API idiom. See
  `serverpackcreator-plugin-example/CLAUDE.md`.
- **serverpackcreator-web-frontend** — Quasar 2 / Vue 3 SPA, JavaScript (TS migration planned),
  Pinia stores, built into the app's web backend via the org.siouan frontend Gradle plugin. See
  `serverpackcreator-web-frontend/CLAUDE.md`.
- **serverpackcreator-grinder** — standalone fire-and-forget service that boot-verifies mods at scale
  in isolated, network-less **Docker containers** (docker-java). Depends on `-clientside` (+ `-api`);
  no Spring/Swing; not published. Foundation stage: the container-backed `ServerRunner`. See
  `serverpackcreator-grinder/CLAUDE.md`.
- Not in the Gradle build: `serverpackcreator-help` (docs), `buildSrc`, `docker`, `misc`.
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
- **KISS + MVC + TDD + SOLID** — always.
- **No shortcuts:** fix bugs when found, don't defer.
- **No assumptions:** read the code, check the docs before advising.
- **git:** never push yourself; let the user push. One branch per feature/fix; group related work;
  prefix Claude-created branches with `claude-`.

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

**Current status (2026-08-31):**

| Module         | Tests         | Notes                                                                                |
|----------------|---------------|--------------------------------------------------------------------------------------|
| api            | 383 (1 skip)  | Phase 1 **complete**. Counts in this column are re-derivable from `<module>/build/test-results/test/*.xml` after a full build — confirm the files came from that run before trusting a total. Guard style worth knowing before adding one: manifest and generation work is pinned by *request*, *read* and *open counts* against loopback servers and injected openers, never by wall-clock; shipped shell templates are pinned by **executing** them — and since 2026-08-23 the two shells that cannot be executed everywhere are covered by driving the extracted function in a container instead, which is what proved bash, fish and PowerShell agree on the Forge launch path across both versioning schemes. |
| clientside     | 267           | Extracted from `-app`; `BootVerifier` split + `packPostProcessor` hook; selection (MC-support gate) + setup-abort classification pinned; `MetadataScanner` dispatches through `ModScanner.scannerFor`; the locked-file browser download treats an aborted navigation as the download starting, which is the only way CurseForge's `/download` ever succeeds. A crash now has to survive **three** checks before it counts as clientside: the newest loader build, other versions of the mod (when it contradicts a declared server support), and — after every loader is in — another loader that booted a server with the *same* list-entry. One build's crash and a clientside mod used to be indistinguishable evidence, and the published entry is a loader-agnostic stem, so one loader's crash was stripping another loader's proven-bootable build (2026-08-23). The other-version re-check now spends its budget on a *diverse* sample — a new Minecraft version-line and a new loader per pick — rather than the two versions either side of the crashing one, and a Modrinth version's non-primary files (source jars) are no longer mod files at all; both were needed to stop `creativecore` publishing HIGH (2026-08-23). Per-attempt scratch space is owned by `(platform, slug, loader)` via `AttemptDirectory`, not by `(slug, loader)` — the same slug on two platforms is two candidates the grinder runs in parallel, and the shared directory was being wiped out from under a running container (2026-08-23). A verdict now records **which loader actually booted** (`bootedLoader`), because a cross-loader re-check can decide one loader's verdict from another's clean boot — and only a loader's *own* boot may disprove another's crash (2026-08-23). A **modloader that never bootstrapped** is INCONCLUSIVE, not CRASHED: `Could not find parent layer for module` is the ServerStarterJar and Forge's `SecureModuleClassLoader` disagreeing about module layers, so the server dies before FML exists and no mod is loaded — `ars-nouveau` was headed for a HIGH off it. The JVM's `Error: could not open` for an unreadable `@argfile` joined the launch-failure guard at the same time, since Forge now boots from one (2026-08-23) **A crash is only evidence when the harness gave the mod a fair run** (2026-08-29, from a census of 200 of the 609 crash logs the live grinder publishes; 130 of them carried no client-side evidence at all yet every one scored CRASHED). Two fixes came out of it. `BootCandidateSelector` now fixes the **Minecraft version across both loader attempts** instead of preferring it inside each: a Quilt-tagged Fabric API for the wrong version used to satisfy the first attempt, so the Quilt-to-Fabric fallback holding the right version was never reached, and 20 of the 35 sampled Quilt boots were handed a `+26.3` Fabric API for packs as old as 1.19.2 — Quilt Loader refused the pack and the *candidate* wore the verdict. A dependency matching no file for the pack's Minecraft version is now **not staged at all**, because a near-miss dependency only manufactures a conflict to blame on the mod, whereas a near-miss *candidate* still tests the candidate. `BootLogClassifier` gained `sandboxNetworkMarkers` and widened `dependencyFailureMarkers` (Quilt's `requires version [x, y) of z`, mixin `ClassMetadataNotFoundException`, the legacy `MixinTweaker` CNFE) — both in the band **below** `clientOnlyClassMarker`, which is the whole design: an excuse may never outrank decisive client-only evidence. Boots run `--network none`, so a mod whose loader phones home cannot pass here and fails nowhere else (OneConfig fetches its stage1 from `api.polyfrost.org`, falls back to a Swing dialog — the `Fontconfig error: No writable cache directories` tail — and exits). Re-classifying the real logs: the 21 Griefed sent went 21 CRASHED → 11 CRASHED/10 INCONCLUSIVE, the 200-log sample 200 → 113/87, with both true positives (`arcane-vortex` invalid-dist, `avm-mod` `class_746`) retained. **Known gap, deliberately unfixed:** `clientOnlyClassMarker` misses Fabric intermediary names — `class_NNNN` is intermediary for *every* class, not only client ones, so a pattern would trade these false negatives for false positives; it needs a version-specific ID list or nothing. A staged dependency is counted **by file, not by ref** (2026-08-30): a project is reachable under the platform ref an author linked (`P7dR8mSH`) and the mod id its manifest declares (`fabric` → slug `fabric-api`), which are different strings for one project, so `stageableRequirements`' ref-level dedupe cannot see it — B6's gate caught `amblekit` recording one jar twice. Cosmetic in the report, but it also double-counted toward `MAX_INJECTED_DEPENDENCIES`, refusing packs that were within the cap and scoring them INCONCLUSIVE. **The clientside test tree also did not compile from clean** between `18f59b4bf` and 2026-08-30: an abstract `ModPlatform.name` was added without updating two anonymous implementations in tests, and Gradle's incremental compilation never recompiled them — every green build in between was green without ever compiling those files. `--rerun-tasks` is what exposes that class of thing — a *plain* build of the same commit says `BUILD SUCCESSFUL in 5s` off the build cache (`org.gradle.caching=true`), which is exactly why it survived local full builds. **CI caught it and nobody looked**: `test.yml` runs `./gradlew build` on every push, and runs #301 (`592f1ce21`) and #306 (`388b6e3a2`) both went **failure** on `develop` and stayed unactioned for about a week. The gate existed; the response did not. Check the pipeline after a push rather than assuming green. **A SURVIVED boot is evidence, and `aggregate` was throwing it away** (2026-09-01, from the live store's INCONCLUSIVE rows): the fold consulted `bootResult` only for CRASHED, so a clean boot fell through to the metadata — and with the jar scan errored and the platform declaring nothing, which is *every* CurseForge project, there was no metadata to fall through to and the most expensive signal the engine produces read "we learned nothing". SURVIVED now yields LOW, ranked below `metadataClient` so a clean boot still cannot overturn a client-only declaration; `better-stats`, `tcdcommons` and `yacl` are the measured rows. In the same pass, a staging refusal stopped saying only `Could not download <file>` — 21 live verdicts read exactly that, all CurseForge, all distribution-locked, which is a sentence a 404 and *a host with no Chromium* produce identically. |
| app            | 149           | Phase 2 largely complete; clientside engine extracted out, CLI verbs stay. GUI hot paths are pinned by *call counts* and set identity, never wall-clock; the web module's persistence declarations are pinned against Spring Data's own machinery (`PartTree`, `MongoMappingContext`, `MongoPersistentEntityIndexResolver`) so none of them needs a database. |
| plugin-example | 3 (from 0)    | Phase 3 **complete**                                                                  |
| web-frontend   | 32 (from 0)   | Phase 4a–4e done: Vitest, `$q` decoupling, **full TS migration**, component coverage; `types/api.ts` mod-lists are `string[]` since the web module embedded them (2026-08-17); `RunConfigurationCard` asserts the *rendered* lists, not the props it passed in — the pass-through version stayed green with the card reverted to the pre-branch object shape (2026-08-18) |
| grinder        | 434 (29 skip) | Continuous fire-and-forget boot-verification in network-less containers, with a persisted catalog crawl cursor so coverage accumulates instead of re-checking the top N. Core loop e2e-verified on current MC; script-template matrix green across bash/fish. **The CurseForge crawl's two design-killers are LANDMINE #1 and #2 in `serverpackcreator-grinder/src/main/kotlin/de/griefed/serverpackcreator/grinder/source/CLAUDE.md`** — read those before touching the partition plan. Runs as a systemd service since 2026-08-22, which took a home-resolution fix in `-api` and a startup-ordering fix here — both landmined in the module files. The report server binds **loopback** unless `SPC_GRINDER_HOST` says otherwise (2026-08-23) and carries no authentication — the landmine in the module file has the reverse-proxy consequence. Its two bind-address guards need a real non-loopback IPv4 and skip without one, which is why this row's skip count is a range rather than a number. `deploy/` carries an example systemd unit and an installer; `SystemdUnitConfigurationTest` fails the build if a knob is added to the service and not to the unit. Per-container CPU and memory are capped by `SPC_GRINDER_CPUS` (cores, default `2`) and `SPC_GRINDER_MEMORY_GIB` (GiB, default `3`), both defaults being the values hardcoded before the knobs existed; only an exact `0` means uncapped, decided on the input rather than on the rounded value. **The memory cap carries a don't-touch warning for a measured reason:** the grinder's packs pass no `-Xmx`, so the JVM derives each server's heap from the cgroup limit at 25% (3 GiB → a 768 MiB heap), and the cap is also the divisor in the worker-sizing formula — so moving it starves boots or over-subscribes the host, and either way the kills are scored INCONCLUSIVE and read as mods that hang. a unit-level `CPUQuota=` can only ever bound the daemon's own host-side work, for the same cgroup reason that makes the shutdown hook load-bearing. Containers run as the **owner of the directory they mount**, not the image's `USER 1000:1000` — the systemd migration broke every install with that mismatch, and the landmine in the module file has the reason the failure names the wrong subsystem (2026-08-23). `/as-properties` publishes the fallback clientside-mod list (shipped list + crash-proven findings) for an SPC instance's `fallback.updateurl` to poll. The console of every **crashed** boot is copied out of staging into `<home>/crash-logs` before the reaper runs, because staging is wiped by the next re-grind of the same tuple and a crash is the only outcome that reaches HIGH; the report links each one per row and indexes them at `/crash-logs`, whose `?name=` lookup treats the name as untrusted (2026-08-23). The overview now carries a button for every endpoint beside "Download CSV". An **immediate re-grind queue** (`RequeueStore`, `--requeue` / `--requeue-before`, drained forced at the start of every pass) is how a defect in the *engine* gets un-published, since the crawl and the TTL only bring a project round eventually (2026-08-23). Stopping the service signals containers and workers and kills them after a 15s window; containers live in the **docker daemon's** cgroup, not the unit's, so the shutdown hook is the only thing that can stop them and a label-based startup reap is the only recovery from a SIGKILL — both landmined in the module file. Boot containers carry a **fixed, resolvable hostname** (`spc-grinder`, mapped to loopback via `--add-host`): `--network none` gives the daemon no address to write into `/etc/hosts`, so a container's own name did not resolve and every boot opened with three `UnknownHostException` stacktraces from log4j's `getLocalHost()` (2026-08-23). Its packs leave **`USE_SSJ` alone** on purpose: the *templates* now bypass the ServerStarterJar for exactly the Minecraft versions it cannot launch (1.20.2/1.20.3 — the range was measured, not read off the securemodules source, which would have condemned every modern Forge), so the grinder keeps booting the same path a user's pack does and stays able to notice that path breaking (2026-08-23). The boot tmpfs is mounted **`exec`** — a decided weakening (2026-08-24), because `noexec` was silently costing every boot Netty's native epoll transport, not merely producing JNA noise in crash reports; `nosuid`/`nodev` still apply. The overview and the CSV both carry a **`Scanned` date** (`YEAR/MM/DD`, UTC, one shared `ScanDate`), and the report serves its own bundled favicon rather than any external asset (2026-08-23). Verdict-store writes are **coalesced** since 2026-08-29 (B35, closed): `record()` buffers and a daemon flusher persists every `SPC_GRINDER_STORE_FLUSH_SECONDS` (default 30), with the shutdown hook flushing last, after the workers stop. `persist()` serialises the *whole* store under the lock on a grind worker's thread, so write-through cost 20.8 ms per verdict at 1 k rows, 94.7 ms at 10 k and **1242.5 ms** at 100 k — against a deployed store of 38,258 verdicts that only grows. Coalesced it is **2.6 µs and flat**, which is the point: it no longer scales with the store. Dropping the pretty-printer was measured and rejected as insufficient (707 → 361 ms at 100 k), and the append-log was therefore never needed. The default is deliberately write-through (`Duration.ZERO`) so coalescing is opted into at the composition root; a hard kill can lose at most one interval, re-derived by the re-verify TTL. The report's **Logs column is sortable** since 2026-08-29 — it was exempted from sorting as well as filtering on the reasoning that it is not verdict-derived, which was wrong: *not every entry has logs* (artifacts are kept only for boots that did not survive, and the reaper drops the oldest past the budget), so `?sort=logs&dir=desc` is how a maintainer finds the rows with anything to read. The sort key is its own sealed type (`SortKey.Column` / `SortKey.Logs`) rather than a `VerdictField`, because the sortable columns and the verdict-derived ones are not the same set; `VerdictField` is untouched and Logs stays exempt from filtering, searching and the CSV. The count the sort uses and the links the cell renders come from **one** per-request snapshot via `ReportServer.logNamesFor` — two lookups is how a row sorts as having logs and then renders an em-dash, which is why the wiring is pinned through the real handler and not only as a pure unit. **`/dashboard` renders `/status` for a human** since 2026-09-01 — a polling, framework-free page (JDK HTTP server, vanilla JS, nothing fetched off the network) showing pass, workers, crawl, cache and rule errors with human durations. `/status` itself is untouched: it is scripted against, so this is a second route rather than content negotiation. The page is a *constant*, which is how a server with no authentication displays internet-supplied slugs without an escaping bug — every value is written client-side with `textContent`. Its guards are the two a string constant otherwise cannot have: `READ_FIELDS` cross-checked against a document a real `ReportServer` serves, because `statusJson()`'s keys are string literals no compiler checks (renaming `"loaderCache"` was verified to be the only failure in all 434 tests), and the script *executed* under node when present. |

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
