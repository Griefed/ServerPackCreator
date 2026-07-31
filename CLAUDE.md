# ServerPackCreator — Claude Code context

> **Purpose of this file:** the durable, *current-state* context for Claude Code sessions on
> ServerPackCreator. Read it before touching code.
>
> - Per-sprint **narrative** history → `git log` and `claude-docs/REFACTOR-LOG.md`.
> - **Deferred-but-agreed work** → `claude-docs/BACKLOG.md` (why it waited + context to pick it up cold).
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

Gradle multi-project build (`settings.gradle.kts`), Kotlin 2.3.x, JVM 21, version catalog in
`libs.versions.toml`, convention plugins in
`buildSrc/src/main/kotlin/serverpackcreator.*-conventions.gradle.kts`.

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

## Build & test commands

- `./gradlew build` — full build. The app build depends on the frontend build and license report.
- `./gradlew :serverpackcreator-api:test` — API suite (runs against fixture modpacks in
  `serverpackcreator-api/tests/` and `src/test/resources/testresources/`). **Offline for every Minecraft version in
  the shipped manifest snapshot**, which `ApiWrapper.setup()` seeds from the jar; a version newer than that snapshot
  costs one fetch of its `mcserver/<version>.json`. The snapshot currently lags its own parent manifest (backlog
  B25). The test home is wiped before each run **except** `manifests/`, so that cache persists and accumulates.
- `./gradlew :serverpackcreator-app:test` — app suite.
- `./gradlew :<module>:koverHtmlReport` / `koverXmlReport` — coverage (Kover), report under
  `<module>/build/reports/kover/`.
- Frontend: `npm install && npx quasar dev` in `serverpackcreator-web-frontend/` (dev server),
  `npx quasar build` for production build, `npm test` (Vitest).
- Run the app locally: `./gradlew :serverpackcreator-app:run` (GUI by default; CLI/web via args,
  see `Mode.kt` / `CommandlineParser.kt`).
- `media` task needs install4j installed locally — not part of regular dev loop.

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
  release notes even though nothing fails to compile. Recorded because this branch made one:

| Change | Effect on an embedder |
|---|---|
| `PathsConfig.homeDirectory` consults `-Dde.griefed.serverpackcreator.home` **before** the stored preference and the properties file (`PathsConfig.kt:106`) | A host that sets that property now resolves a different home than the same code did before. Additive and opt-in — nothing changes unless the property is set — but every plugin reading `apiProperties.homeDirectory` follows it. |
| `ApiProperties.resolvePreferencesNode` + `PREFERENCES_NODE_PROPERTY` / `PREFERENCES_NODE_ENV` / `DEFAULT_PREFERENCES_NODE` | New exported surface; the default node name is unchanged, so existing installations keep reading their own settings. |
| The eight `default*ScriptTemplate` properties are computed per access (`PathsConfig.kt:586`–`:643`) instead of captured at construction, and the `ApiProperties` facades (`:801`–`:836`) pass that through | For a stable home the value is identical, so nothing changes for a normal embedder. What changes is that the value is no longer a *constant*: a host that moves the home at runtime (`--home`, the `-D` override, the GUI settings panel) now sees the template paths follow it, where before they kept pointing into the old home. Anything caching one of these paths across a home change was reading a stale path and should re-read instead. |
| `MinecraftServer` gains defaulted `downloadCooldown` / `clock` parameters, and a failed manifest **download** is not re-attempted for an hour (`MinecraftServer.kt`, `readServerJson`) | A manifest already on disk is still always read, so a working installation is unchanged. What changes is a *failing* one: an embedder that previously saw a download attempt — and `WebUtilities`' ERROR-with-stack-trace — on every `getServer`/`requiredJavaVersion` call now sees at most one per hour per version. The exported `Optional` shape is unchanged; a caller reading empty as "no server available" still gets that. |
| `ServerPackProvisioner.variables` reads the shipped `server_files/variables.txt` instead of a compiled-in string literal, falling back to the bundled copy (`ServerPackProvisioner.kt:56-63`). New exported members: `PathsConfig.defaultVariablesTemplate`, `ApiProperties.defaultVariablesTemplate` | A default installation gets byte-identical output — but the value is no longer a constant. An operator who edits that file changes what **every** embedder's generation emits, and one who deletes it gets the bundled fallback (the app's delete-watcher restores it). Anything asserting on a fixed `variables` string should read the template instead. |

---

## Conventions

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
- **`refactor:` is a claim about behaviour, not about intent.** Use it only when behaviour is preserved; label
  a behaviour change `fix:` or `feat:` however tidy it looks. If an **existing** test's *assertion, argument or
  expected value* has to change, the label is already wrong — that is the stop-and-flag signal, not a formality.
  **Carve-out: a reference-only update is not the signal.** Moving a symbol between modules necessarily updates
  imports and receivers in its tests, and Strangler-Fig moves are exactly what the conventions ask for — so
  `- Old.isSuspendGap(gap, poll)` / `+ New.isSuspendGap(gap, poll)`, with every assertion byte-identical, stays a
  `refactor:`. Judge the diff, not the file list: if no expectation changed, the test did not change in the sense
  this rule means. (Written after the rule's first draft flagged `b6b778b82`, a clean cross-module move, as
  mislabelled — a convention that cries wolf on legitimate refactors gets ignored wholesale.) Two commits got this
  wrong: `5f138ef8a` (`refactor(app)`) moved four call-sites onto a *resolved* Preferences node, changing where
  any host with its own node reads and writes, and had to edit `CommandlineParserTest`; `7815d5960`
  (`refactor(api)`) added an operator-editable template path plus a delete-watcher branch in `-app`. Both
  described the change honestly in the body — only the type lied.

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

**Current status (2026-07-31):**

| Module         | Tests         | Notes                                                                                |
|----------------|---------------|--------------------------------------------------------------------------------------|
| api            | 272 (1 skip)  | Phase 1 **complete**; + `MinecraftMetaTest` (`requiredJavaVersion`) and `ScriptTemplateContentTest` (non-gated guard for the shipped templates; skips its `fish -n` case where fish is absent, and **executes** the bash `setupFabric` to pin the offline launcher path). |
| clientside     | 87            | Extracted from `-app`; `BootVerifier` split + `packPostProcessor` hook; selection (MC-support gate) + setup-abort classification pinned |
| app            | 76            | Phase 2 largely complete; clientside engine extracted out, CLI verbs stay             |
| plugin-example | 3 (from 0)    | Phase 3 **complete**                                                                  |
| web-frontend   | 23 (from 0)   | Phase 4a–4e done: Vitest, `$q` decoupling, **full TS migration**, component coverage  |
| grinder        | 224 (19 skip) | + `BootWorkspaceReaper` — staging reclamation; the work tree grew unbounded at ~23 GB/h (98 GB measured) before it. Core loop **e2e-verified on current MC** (26.2/Quilt boots offline on JDK 25); **continuous fire-and-forget** with a **persisted catalog crawl cursor** (each pass takes the next slice, so coverage accumulates instead of re-checking the top N) + work-driven pacing; Modrinth + CurseForge sources; **script-template matrix IT** (bash/fish/pwsh — caught + fixed a real `.fish` bug); container/loader/report/source subpackages; MC selection bounded to image-supported Java. 94 run + 8 gated (3 engine IT, 3 live-crawl IT, 2 template-matrix). Template matrix fully green: 5 MC x 5 loaders x bash/fish, bash == fish everywhere. CurseForge is crawled **in partitions** (135 Minecraft versions × modloader × category, both sort directions) to get past its 10 000-result API cap — **now live-verified with a real API key** (`CurseForgeCrawlLiveIT`), which caught two silent design-killers the docs had hidden: `totalCount` saturates at the cap (so no split could ever fire) and the version list is 98 % non-Minecraft strings |

Key size reductions (all behind source-compatible facades): `ApiProperties.kt` 3,007 → 1,372;
`ConfigurationHandler.kt` 1,562 → 897; `ServerPackHandler.kt` 1,466 → 490.

**Remaining hotspots:** `ConfigEditor.kt` 1,369 — assessed: the two pure pieces (dirty-check,
Java-version) are extracted; the rest is legitimate Swing view code, not worth mechanically
splitting. `LarsonScanner.kt` 2,217 — self-contained widget, low priority.

**Open issues (details + locations in the relevant module `CLAUDE.md`):**
The GUI `GlobalScope.launch` anti-pattern (app) is **resolved** — all 26 sites now use
`gui.utilities.ComponentCoroutineScope` (lifecycle-cancelled), GUI-verified. The frontend's
settings-store `$q` coupling (4b) and `jsconfig.json`/TS gap (4c) are **resolved**.

**2026-07-31 — audit + backlog cleanup (`claude-audit-backlog-cleanup`).** Three audit findings closed (the
`variables.txt` contract row, and pin-first/`refactor:`-labelling as binding rules); one **withdrawn** as wrong on
inspection, its inverse recorded as backlog B21. Backlog B13–B20 closed: a fresh Forge pack on Minecraft 26.x
installed and exited **0** without launching (ServerStarterJar needs a `SecurityManager` to swallow the Forge
installer's `System.exit`, which JEP 486 removed in Java 24 — so from 24 the templates install Forge themselves and
launch from the installer's argfile); the "not applicable" skip that hid it now fails loudly; both boot paths share
one suspend-aware deadline; the loader cache records which templates produced an install; test properties are
generated rather than committed with one machine's paths; and `.gitignore` no longer hides shipped `server_files`
resources. Remaining backlog: B4, B5, B11 (deliberate) plus B21, B22.

**Current phase — 4 (frontend) complete; GUI structured-concurrency done.** Frontend 4a–4e: Vitest,
settings-store `$q` decoupling, full TypeScript migration (all `src/` is TS, verified by
`quasar build`), a Quasar component test harness (Vue Test Utils), and broadened component coverage
(all cards + nav SFCs; suite at 23; tables left untested by design — trivial format-lambda logic vs.
brittle QTable rendering). The GUI `GlobalScope.launch` anti-pattern is resolved (see Open issues),
GUI-verified. **Next (optional):** broaden component-test coverage further.
