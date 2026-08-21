# ServerPackCreator — Claude Code context

> **Purpose of this file:** the durable, *current-state* context for Claude Code sessions on
> ServerPackCreator. Read it before touching code.
>
> - Per-sprint **narrative** history → `git log` and `claude-docs/REFACTOR-LOG.md`.
> - **Deferred-but-agreed work** → `claude-docs/BACKLOG.md` (why it waited + context to pick it up cold).
> - **Behaviour changes on the published API** → `claude-docs/API-BEHAVIOUR-CHANGES.md` (one row per
>   change, what an embedder sees). The *policy* stays below; that file is its evidence.
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
- **CI lives in `.forgejo/workflows` — Forgejo (`git.griefed.de`) is the canonical CI and the origin of
  every release.** `.gitlab-ci.yml` is gone. **LANDMINE:** `.forgejo/workflows` is *all-or-nothing* — once
  it exists, Forgejo ignores `.github/workflows` entirely
  ([forgejo#9203](https://codeberg.org/forgejo/forgejo/issues/9203)), so anything Forgejo must do belongs
  there and nowhere else. `.github/workflows` keeps a **smoke test** plus the four issue-driven
  `clientside-*` workflows, which are GitHub-native; releases are created on Forgejo and mirrored outward
  by `release-build.yml`'s `mirror` job, because Forgejo push-mirrors replicate refs but **not** releases.
  Two GitLab capabilities were **deliberately not carried over**: `Build Release` uploaded the app jar to
  GitLab's *generic package registry* and then created a release asset *link* to it (Forgejo attaches
  assets to the release directly, so a consumer with a hard-coded `/packages/generic/...` URL loses it),
  and `release_job` created a release whose description merely linked changelogs on three forges (the
  Forgejo release now carries the changelog section itself).
  **`serverpackcreator-help/Writerside/api-docs.yaml` is GENERATED, not hand-maintained** — springdoc
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
  costs one fetch of its `mcserver/<version>.json`. The snapshot currently lags its own parent manifest (backlog
  B25). The test home is wiped before each run **except** `manifests/`, so that cache persists and accumulates.
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

### Build layout (durable — where things are declared)

- **Repositories are declared once**, in `settings.gradle.kts` under `dependencyResolutionManagement`,
  with `RepositoriesMode.FAIL_ON_PROJECT_REPOS` — a project-level `repositories { }` is a build
  failure, not a silent override. They were previously in 13 places. `buildSrc/build.gradle.kts` keeps
  its own because it is a **separate build** and cannot read the root settings; it deliberately does
  **not** list `mavenLocal()`, which used to be first there and let a stale `~/.m2` artifact shadow the
  real one.
- **The foojay toolchain resolver is declared TWICE, differently, and both are required.**
  `settings.gradle.kts` has it `version "0.8.0"`; `buildSrc/settings.gradle.kts` has it **without** a
  version. buildSrc is a separate build and does **not** inherit the root's toolchain repositories
  (verified — it fails with *"Toolchain download repositories have not been configured"*), yet by the
  time its settings evaluate the plugin is already on the classpath, so requesting a version there
  fails with *"already on the classpath with an unknown version"*. Don't "tidy" either one away.
- **Versions live in `gradle/libs.versions.toml`** — `[versions]`, `[libraries]` (49) and `[plugins]`
  (12). Do not re-add a hardcoded coordinate to a module build file.
  **Plugins are consumed by two different routes, and only one of them works everywhere:**
  - a *real* build script (`build.gradle.kts`, a module's own) uses `plugins { alias(libs.plugins.x) }`;
  - a **precompiled script plugin** (`buildSrc/src/main/kotlin/*.gradle.kts`) **cannot** — `alias(...)`
    there fails at `:buildSrc:compilePluginsBlocks` with `Unresolved reference: libs`. Verified by
    trying it, not assumed. Those apply a versionless `id("...")`, and the version arrives from the
    plugin **marker** (`<id>:<id>.gradle.plugin:<version>`) that `buildSrc/build.gradle.kts` puts on
    its own compile classpath via `libs.plugins.x.marker()`.

    Either route reads this one file, so a plugin's id and version are declared exactly once. Before
    2026-08-16 buildSrc depended on plugin *implementation* artifacts under `[libraries]`
    (`kotlinGradlePlugin`, `dokka`, …) while the convention plugins named the plugin *id* — two
    unlinked strings per plugin. Converting to markers is behaviour-preserving; measured, the
    flattened buildSrc compile classpath gained only the marker POMs and **lost
    `org.jetbrains.dokka:javadoc-plugin`**, which the `org.jetbrains.dokka-javadoc` marker does not
    depend on. That artifact turned out to be unnecessary: a from-scratch `dokkaJavadocJar` still
    produces 467 files / 356 HTML pages. Check that jar if you touch dokka wiring — `-api`'s javadoc
    is **published to Maven Central**, and the task reports success either way.
  - `settings.gradle.kts` cannot use the catalog in its own `plugins { }` block (it is evaluated
    before the catalog exists), which is why the foojay resolver keeps a literal version there. `buildSrc/settings.gradle.kts` points at the same file
  explicitly: buildSrc does **not** inherit the root catalog (verified on Gradle 8.14.4 — removing the
  block fails with `Unresolved reference: libs`).
  **Everything Kotlin is ONE `kotlin` entry (2.4.10) — keep it that way.** The compiler plugin, the
  allopen/jpa/spring compiler plugins and the stdlib/reflect/test libraries all read `version.ref =
  "kotlin"`. JetBrains versions these together, so a split only ever produces skew: until 2026-08-16
  this was four entries (`kotlin`, `kotlinAllOpen`, `kotlinJpa` on 2.3.20; `kotlinLibs` on 2.4.10),
  which meant `-api` compiled with a 2.3.20 compiler against a 2.4.10 stdlib. That combination did
  work — but it is the same *shape* as the coroutines failure below: a compiler reading metadata from
  a newer library fails hard with *"binary version of its metadata is X, expected Y"*, and nothing
  warns you as the gap widens. Do not re-split it to bump libraries without the compiler.
  Unifying was measured, not assumed: compiler warnings **243 before, 243 after**, the only delta
  being one warning the newer compiler rewords in place (`ServerPackCreator.kt:164:95`, elvis
  operator); 741 tests green; `bootJar`, `dokkaJavadocJar` (356 HTML pages), `sourcesJar` and
  `generateLicenseReport` all still succeed. Note the compiler version binds **Gradle** only —
  IntelliJ analyses with its own bundled Kotlin plugin, so an IDE older than the catalog can report
  metadata errors the command line does not.
- **Only `-api` publishes.** `serverpackcreator.publishing-conventions` is applied by that module
  alone, matching CI (`.forgejo/workflows/release-build.yml`'s `maven` job runs four
  `:serverpackcreator-api:publish...` invocations and nothing else). Non-api modules produce no sources/javadoc jar and run no `signing`. Do not move this
  back into `java-conventions`.
- **Convention plugin graph:** `java-conventions` (toolchain, test isolation, jar manifest) ←
  `kotlin-conventions` (Kotlin + Kover) ← `application-conventions` (= kotlin + spring);
  `spring-conventions`, `dokka-conventions`, `quasar-conventions` and `publishing-conventions` are
  applied on top as needed.
- **No cross-project configuration in the root build.** `allprojects { }`,
  `evaluationDependsOnChildren()` and `project("x").tasks.y.get()` are gone. A module that needs to run
  after another declares it itself, by task **path** (`-app`'s
  `mustRunAfter(":generateLicenseReport", ":serverpackcreator-web-frontend:build")`) — a string path
  resolves lazily, reaching into another project's task container forces it to be evaluated. The
  example-plugin jar is consumed as an artifact (`pluginArtifact`, a consumable configuration on
  `-plugin-example`) rather than dug out of `childProjects[...]`, which is what removed the build's last
  `!!`. Do not re-introduce any of the four.
- **Configuration cache is NOT enabled, and step 5 above is not what is blocking it** — measured, because
  this was claimed and was wrong: `build --dry-run --configuration-cache` reported the *same* 20 problems
  (13 unique) before and after the cross-project work, and configuration time was ~4.95 s either way.
  Those constructs block project **isolation**, a different feature. The 20 problems are:
  - `:generateLicenseReport` holds a `Project` reference — **third-party** (jk1 gradle-license-report),
    not fixable here.
  - every module's `test` and `processTestResources` "cannot serialize Gradle script object references" —
    **ours**: the `filter { }` in `processTestResources` and the `doFirst { cleanup() }` in `test`, both in
    `java-conventions`, capture the enclosing script; `-app`'s `test.doFirst` additionally captures
    `projectDir`.
  So the ceiling without excluding `generateLicenseReport` is "fewer problems", not zero. Fixing our own is
  a real, separate piece of work; do not start it expecting the cache to switch on at the end of it.
- **LANDMINE — Boot's BOM is a `platform()`, never `io.spring.dependency-management`. Do not "restore"
  that plugin.** Boot's BOM manages far more than Spring — verified in 4.0.2's BOM: `kotlin.version`
  2.2.21, `kotlin-coroutines.version` 1.10.2, `log4j2.version` 2.25.3, `jackson-2-bom.version` 2.20.2,
  `jackson-bom.version` 3.0.4, `junit-jupiter.version` 6.0.2, `mongodb.version` 5.6.2, i.e. most of what
  this project pins for itself. `io.spring.dependency-management` applies those as **forced** versions
  that beat every transitive request, so each catalog bump upgraded the other modules and was silently
  reverted in `-app`. That is not a warning and not a build failure — it surfaces as a
  `NoSuchMethodError` the first time the newer API is *called*. It cost 16 app tests on the coroutines
  1.11.0 bump (`BuildersKt.runBlockingK`, renamed in 1.11.0, absent from the 1.10.2 the BOM forced),
  while `./gradlew compileKotlin` was green in every module.
  Since 2026-08-16 `serverpackcreator.spring-conventions` imports the BOM as a Gradle `platform()`,
  whose versions are ordinary constraints that lose to a higher request — the catalog wins, Boot still
  versions everything we do not pin. Measured `-api` vs `-app` on shared coordinates:

  | Configuration | Differing before | Differing after |
  |---|---|---|
  | `runtimeClasspath` | 13 of 79 | **0 of 79** |
  | `testRuntimeClasspath` | 31 of 101 | **3 of 102** |

  The three survivors are `-app` resolving *higher* (byte-buddy 1.18.10, asm 9.7.1) from test
  dependencies `-api` lacks — correct conflict resolution, not drift. **Two related traps:**
  - The BOM coordinate comes from the catalog's `springBoot`, **not** `SpringBootPlugin.BOM_COORDINATES`,
    which is the *Gradle plugin's* version (`springGradle`). Those had drifted to 4.0.2 vs 4.1.0, leaving
    Boot internally inconsistent — `spring-boot` at 4.0.2 while `spring-boot-starter-web` was 4.1.0.
  - A platform only out-ranks what the module actually *requests*. `-app` got mockk only transitively
    from springmockk (1.14.6), so the catalog's 1.14.11 never applied and `-api`'s comment claiming the
    build is mockk-single-versioned was false. `-app` now declares `libs.mockk` explicitly. Bumping a
    library that reaches a module **only transitively** still needs an explicit declaration there.
- **LANDMINE — never do filesystem work in a task's configuration block.** `-api` shipped its
  root-level documents with fifteen bare `copy { }` calls inside `tasks.processResources { }`, so they
  ran when the task was *configured* — including on runs where `processResources` was UP-TO-DATE and did
  nothing — with no inputs, no outputs and no caching, writing into two source trees. They are now the
  `shipRootDocuments` / `shipWritersideDocuments` / `shipWritersideImages` Copy tasks. Making them
  visible immediately surfaced a real undeclared dependency (`sourcesJar` packages what
  `shipRootDocuments` writes), which had been ordering by luck.

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

- **Cite names, not snapshots.** Three consecutive audits of the performance branches found the same
  defect class and nothing else: a fact quoted in prose going stale the moment the code moved — 54 commit
  hashes killed by a rebase, a landmine still describing a flaw that had been fixed, a line number shifted
  by the very commit that cited it, and suite counts left behind by the tests that were just added. Prefer
  the **commit subject** over its hash (subjects survive rebase, cherry-pick and squash), the **symbol name**
  over `File.kt:123`, and "what the guard asserts" over "how many tests exist". Where a number genuinely
  earns its place — a measurement, a byte count — say what produced it, so a reader can re-run it instead of
  trusting it.
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
  (`REFACTOR-AUDIT.md` iteration 7): **490 pre-existing guards, zero failures**, with exactly two files
  uncompilable — one adapted by adding two constructor arguments and *no* assertion edits (7 guards green), one
  legitimately unadaptable because it asserted behaviour the branch removed. Also check *which* changed classes
  the base's tests actually name, so the residual risk is stated rather than assumed; a class-name grep
  under-reports, since `QuiltPackScanner` is exercised only through `ModScannerSidenessTest`.
- **What only a real runtime can answer, ask a real runtime.** Anything whose point is what an external system
  does — an index, a data migration, a REST response shape — is not verified by a mocked test, however good.
  Iteration 7 ran the actual `bootJar` in `-web` mode against MongoDB 8.0.5 in Docker, seeded with pre-branch
  shaped documents, and that is what confirmed the `sha256` index really exists, the migration really converts
  legacy documents and really skips already-migrated ones, and `/api/v2/runconfigs/all` really returns the
  documented shape. It also surfaced B33, which no test could have. Cost: about fifteen minutes.
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

**Current status (2026-08-21):**

| Module         | Tests         | Notes                                                                                |
|----------------|---------------|--------------------------------------------------------------------------------------|
| api            | 343 (1 skip)  | Phase 1 **complete**. Guard style worth knowing before adding one: manifest and generation work is pinned by *request*, *read* and *open counts* against loopback servers and injected openers, never by wall-clock; shipped shell templates are pinned by **executing** them. |
| clientside     | 88            | Extracted from `-app`; `BootVerifier` split + `packPostProcessor` hook; selection (MC-support gate) + setup-abort classification pinned; `MetadataScanner` dispatches through `ModScanner.scannerFor` |
| app            | 149           | Phase 2 largely complete; clientside engine extracted out, CLI verbs stay. GUI hot paths are pinned by *call counts* and set identity, never wall-clock; the web module's persistence declarations are pinned against Spring Data's own machinery (`PartTree`, `MongoMappingContext`, `MongoPersistentEntityIndexResolver`) so none of them needs a database. |
| plugin-example | 3 (from 0)    | Phase 3 **complete**                                                                  |
| web-frontend   | 32 (from 0)   | Phase 4a–4e done: Vitest, `$q` decoupling, **full TS migration**, component coverage; `types/api.ts` mod-lists are `string[]` since the web module embedded them (2026-08-17); `RunConfigurationCard` asserts the *rendered* lists, not the props it passed in — the pass-through version stayed green with the card reverted to the pre-branch object shape (2026-08-18) |
| grinder        | 233 (19 skip) | Continuous fire-and-forget boot-verification in network-less containers, with a persisted catalog crawl cursor so coverage accumulates instead of re-checking the top N. Core loop e2e-verified on current MC; script-template matrix green across bash/fish. **The CurseForge crawl's two design-killers are LANDMINE #1 and #2 in `serverpackcreator-grinder/src/main/kotlin/de/griefed/serverpackcreator/grinder/source/CLAUDE.md`** — read those before touching the partition plan. |

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
