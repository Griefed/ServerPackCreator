---
description: Where build declarations live and the landmines that protect them — repositories, the version catalog, plugin markers, Boot's BOM, publishing scope
paths:
  - "**/build.gradle.kts"
  - "settings.gradle.kts"
  - "buildSrc/**"
  - "gradle/*.toml"
  - "gradle/wrapper/**"
---

# Build layout (durable — where things are declared)

Moved out of the root `CLAUDE.md` on 2026-08-21: this only matters when editing a build file, and it
was ~3.2k tokens of every session. Contributor-facing companion: `BUILD.md` (the "what"); this file is
the "do not tidy that away, here is what it cost last time".

> **UNVERIFIED: whether this file's `paths:` scoping actually saves anything.** The move was justified
> by a character count on disk, which answers "how big is this file" and not "does this load".
> [claude-code#16299](https://github.com/anthropics/claude-code/issues/16299) — path-scoped rules in
> `.claude/rules/` loading globally regardless of `paths:` — is **open**, with a repro and no maintainer
> response. If that is still live, this file loads every session anyway and the split bought nothing.
> **Run `/memory` in a fresh session to settle it.** The correctness risk is the smaller one: the two
> known bugs make path-scoped rules load *globally* (#16299) or *never* but only under `~/.claude/rules/`
> ([#22170](https://github.com/anthropics/claude-code/issues/22170)) — these are project-level, which is
> that issue's documented workaround, so the landmines below are not at risk of silently vanishing.

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

    **LANDMINE — the marker route drags the plugin's jar onto buildSrc's *compile* classpath, so a
    plugin built with a newer Kotlin than Gradle embeds breaks every task in the build.** `Bump
    install4j to 13` moved `install4j-gradle` to 13.1, whose jar carries Kotlin **2.3.0** metadata.
    Precompiled script plugins compile with **Gradle's embedded** Kotlin — 2.0.x on the wrapper's
    8.14.4 — and `:buildSrc:compilePluginsBlocks` refuses to read it: *"binary version of its metadata
    is 2.3.0, expected version is 2.0.0"*. `./gradlew help` fails, so nothing in the build runs at all.
    The catalog's `kotlin` version cannot rescue this: it governs how the **modules** compile, never
    how build logic does.

    The fix was neither downgrading the plugin nor upgrading Gradle. install4j is applied by the
    **root build script**, which is a real one and therefore takes `alias(...)` — so the marker came
    off buildSrc's classpath and the incompatible jar is never compiled against. Measured: `./gradlew
    help` failed in 3 s before and succeeded in 5 s after, with `install4j` and `media` both still
    registered.

    So the rule is narrower than "markers everywhere", but be careful how it is narrowed: **a plugin
    needs the marker when buildSrc needs it at compile time**, which happens two ways — a *precompiled
    script plugin* applies it by versionless `id(...)`, **or** buildSrc's own Kotlin source compiles
    against its API. install4j was neither, which is why `alias` works there.

    Both routes are in use. Applied by a precompiled script plugin: `kotlin("jvm")` in
    `kotlin-conventions`, `kotlin("plugin.spring"/"allopen"/"jpa")` in `spring-conventions`, plus
    dokka, dokka-javadoc, kover and the siouan frontend plugin. Needed by *source*:
    `licenseReport` — `buildSrc/src/main/kotlin/de/griefed/common/gradle/LicenseAgreementRenderer.kt`
    implements jk1's `ReportRenderer` against `ProjectData`/`ModuleData`.

    **Do not "tidy" `licenseReport` onto `alias` — it was tried and it fails.** It looks like the
    identical case (a versionless `id(...)` in the root script, one metadata bump from the same total
    failure), and a grep of every `id(...)` and `kotlin(...)` call in the precompiled script plugins
    supports that reading. It is wrong: dropping the marker fails `:buildSrc:compileKotlin` with
    ~10 `Unresolved reference: jk1` / `ReportRenderer` / `ProjectData` errors, because the renderer is
    ordinary buildSrc source, not a plugin application. Checking applications alone under-reports what
    buildSrc's classpath is for.

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

