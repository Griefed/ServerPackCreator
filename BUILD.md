# Building ServerPackCreator

A map of the build for people who did not write it. If you only want to compile and run the tests,
[Quick start](#quick-start) is enough; the rest explains why the build looks the way it does, so you
can change it without guessing.

---

## Quick start

```bash
git clone -b develop https://github.com/Griefed/ServerPackCreator.git
cd ServerPackCreator
chmod +x gradlew          # Linux/macOS, first time only

./gradlew build           # compile, test and assemble everything
```

That is the whole story for a normal contribution. `build` compiles all six modules, runs every JVM
suite **and** the frontend's Vitest suite, generates the license report and assembles the jars.

Expect the first run to take a few minutes: it downloads dependencies, and the frontend plugin
downloads its own Node.js into `serverpackcreator-web-frontend/node/`.

### The commands you will actually use

| Command | What it does |
|---|---|
| `./gradlew build` | Everything. What CI runs. |
| `./gradlew test` | Every JVM test suite, no assembly. |
| `./gradlew :serverpackcreator-api:test` | One module's suite — much faster while iterating. |
| `./gradlew :serverpackcreator-app:bootRun` | Run the app. GUI by default; pass `--args="-cli"` etc. **Not `run`** — `-app` is a Spring Boot module, not an `application` one. (`:serverpackcreator-grinder:run` *does* exist; the grinder applies `application`.) |
| `./gradlew :serverpackcreator-api:koverHtmlReport` | Coverage, written to `<module>/build/reports/kover/`. |
| `./gradlew test --configuration-cache` | Same tests, ~2× faster configuration. See [below](#configuration-cache). |

Two tasks exist that you almost certainly do not want:

- **`media`** builds the native installers and needs a local install4j installation. Not part of any
  normal loop.
- **`updateManifests`** refreshes the Minecraft version-manifest snapshot shipped inside `-api`. Run it
  deliberately, never casually — see [the test home](#the-test-home).

---

## Prerequisites

**A JDK 21.** Every module targets Java 21 through a Gradle toolchain.

> **Trap:** the [foojay toolchain resolver](https://github.com/gradle/foojay-toolchains) is applied in
> `buildSrc/settings.gradle.kts` but **not** in the root `settings.gradle.kts`. So Gradle can
> auto-download a JDK for the build's *own* code, but not for the modules. If you do not have a JDK 21
> installed, the main build fails with *"No matching toolchains found"* rather than fetching one.
> Install a JDK 21 (Temurin is what CI uses) or add the resolver to the root settings.

**Nothing else.** Node.js is downloaded and managed by the build. Docker is only needed for the
grinder's gated integration tests.

---

## The modules

Six modules, and dependencies point **inward** — toward `serverpackcreator-api`. Nothing depends on
the app.

```
                       serverpackcreator-api          ← the domain core, published to Maven Central
                        ↑        ↑         ↑
     serverpackcreator-clientside │   serverpackcreator-plugin-example
                        ↑         │
     serverpackcreator-grinder    │
                                  │
                       serverpackcreator-app          ← CLI + Swing GUI + Spring web + updater
                                  ↑
                    serverpackcreator-web-frontend    ← Quasar/Vue SPA, bundled into -app
```

| Module | What it is | Published? |
|---|---|---|
| `serverpackcreator-api` | The domain core: config validation, server-pack generation, mod scanning, version metadata, the pf4j plugin API. | **Yes** — Maven Central. Its public surface is a compatibility constraint. |
| `serverpackcreator-clientside` | The clientside-mod verification engine (platform lookups, metadata + boot signals, downloaders). | No |
| `serverpackcreator-grinder` | Standalone service that boot-verifies mods at scale in isolated Docker containers. | No |
| `serverpackcreator-app` | Four applications in one: `cli`, `gui`, `web`, `updater`. | No |
| `serverpackcreator-web-frontend` | The SPA the web backend serves. | No |
| `serverpackcreator-plugin-example` | Example pf4j plugin exercising every extension point. | No |

Each module has its own `CLAUDE.md` with the details and the landmines specific to it. They are worth
reading before changing that module — several document incidents that cost real debugging time.

---

## How the build is assembled

There is very little logic in the module build files. Almost everything lives in **convention
plugins** under `buildSrc/src/main/kotlin/`, applied by name:

```
serverpackcreator.java-conventions          Java toolchain, test setup, jar manifest, test-home isolation
        ↑
serverpackcreator.kotlin-conventions        Kotlin + Kover coverage       ← most modules use this
        ↑
serverpackcreator.application-conventions   = kotlin + spring

serverpackcreator.spring-conventions        Spring Boot plugins and starters
serverpackcreator.dokka-conventions         API documentation
serverpackcreator.quasar-conventions        Node.js + the frontend build
serverpackcreator.publishing-conventions    Maven publishing + signing  ← -api only
```

So `serverpackcreator-clientside/build.gradle.kts` is 25 lines, and what it does not say is inherited
from `kotlin-conventions` → `java-conventions`.

`buildSrc` also holds a little compiled Kotlin under `de/griefed/common/gradle/` —
`LicenseAgreementRenderer`, `SubprojectLicenseFilter`, `TestHome`. Code lands there rather than in a
convention script when a *task action* needs to call it; see [configuration cache](#configuration-cache).

---

## Single sources of truth

These are declared in exactly one place each. Re-declaring them locally is the most common way to
break this build subtly, so each is enforced or documented:

| What | Where | Enforcement |
|---|---|---|
| **Repositories** | `settings.gradle.kts` | `RepositoriesMode.FAIL_ON_PROJECT_REPOS` — a project-level `repositories { }` fails the build. |
| **Dependency versions** | `gradle/libs.versions.toml` | Convention. Use `libs.someLibrary`, never a hardcoded `"group:artifact:version"`. |
| **Java/Kotlin version** | `java-conventions` (toolchain) + `kotlin-conventions` (jvmTarget) | — |
| **Publishing** | `publishing-conventions`, applied by `-api` alone | CI publishes only `:serverpackcreator-api`. |

`buildSrc` is a **separate build** and cannot see the root settings, so it declares its own
repositories and points at the same version catalog explicitly. That duplication is deliberate and
commented; removing it fails with `Unresolved reference: libs`.

---

## Things that will surprise you

### The test home

Every module's tests run against an isolated ServerPackCreator home at `<module>/tests`, injected as
`-Dde.griefed.serverpackcreator.home`. It is wiped before each run — **except `manifests/`**, which is
a cache of Minecraft version metadata. Preserving it is what keeps the suites offline; wiping it makes
every run re-download and quietly discards newly-fetched versions.

Tests also run under a **per-module `Preferences` node**. Without that, a test run relocates the home
directory of your own GUI — and of any running grinder daemon — into the repository, because SPC
resolves its home through a machine-wide per-user preference. This has happened; it is not theoretical.

Both live in `TestHome.kt` and `java-conventions`, with the incidents documented in place.

### Some generated files live in the source tree

`-api`'s `processResources` copies the root `README.md`, `LICENSE`, `CHANGELOG.md` and friends into
`serverpackcreator-api/src/main/resources/` and into the Writerside help sources, because SPC ships
them inside its own jar and writes them into the user's home at runtime. They are **generated** — edit
the copies at the repository root, never the ones under `src/main/resources`.

The same applies to `serverpackcreator-plugin-example/src/main/resources/`.

### The frontend is part of the JVM build

`:serverpackcreator-app:build` depends on the frontend being built. The
[org.siouan frontend plugin](https://siouan.github.io/frontend-gradle-plugin/) downloads Node,
installs npm dependencies, runs `npm run build` and — since 2026-08 — `npm run test`. If you are only
touching Kotlin you will still pay for this on a clean build.

For frontend work, skip Gradle: `cd serverpackcreator-web-frontend && npm install && npx quasar dev`.

### Never do filesystem work in a task's configuration block

A bare `copy { }` inside `tasks.someTask { }` executes when that task is *configured*, not when it
runs — including on builds where the task is `UP-TO-DATE` and does nothing. It has no inputs, no
outputs, no caching, and it breaks the configuration cache. Register a real `Copy` task instead. Both
`-api` and `-plugin-example` used to get this wrong; `shipRootDocuments` and `shipPluginDocuments` are
what they became.

---

## Configuration cache

Not enabled by default, but it works for most entry points:

```bash
./gradlew test --configuration-cache     # entry stored; ~4.75s → ~2.02s configuration time
```

`build` cannot use it, because `:generateLicenseReport` (a third-party plugin) holds a reference to
the Gradle `Project`, which cannot be serialized. That is the only remaining blocker and it is not
fixable from this repository.

If you add build logic, two rules keep it compatible:

1. **A task action must not capture the build script.** Calling *any* function declared in a
   `.gradle.kts` file from inside `doFirst { }`, `doLast { }` or a `filter { }` captures the whole
   script. Put the function in `buildSrc/src/main/kotlin/de/griefed/common/gradle/` and call it there.
2. **A task action must not touch `project`.** No `projectDir`, no `project.mkdir(...)`. Capture what
   you need into a local `File` at configuration time and close over that.

---

## Troubleshooting

**"No matching toolchains found"** — you do not have a JDK 21. See [prerequisites](#prerequisites).

**A test suite deleted files in my repository / my GUI's home moved** — you are running a build from
before the test-home isolation, or you have re-introduced a `Preferences` write. See
[the test home](#the-test-home).

**The frontend build fails on a corporate network** — it downloads Node from nodejs.org. There is no
offline mode; you need to let it through or pre-populate
`serverpackcreator-web-frontend/node/`.

**`./gradlew build` fails only in `:serverpackcreator-app:test`** — the app's tests need `-api`'s test
resources, which is wired as a task dependency. Run `./gradlew :serverpackcreator-api:processTestResources`
first if you are invoking tests in an unusual way.

**Changes to a `buildSrc` convention plugin seem to have no effect** — `buildSrc` is compiled before
the main build; if it fails to compile, Gradle reports that failure and never reaches your module.
Read the *first* error, not the last.

---

## Where to look next

- `CONTRIBUTING.md` — branch and PR workflow, commit-message conventions.
- `CLAUDE.md` (root) — the engineering conventions this project holds itself to, and the current
  refactor state.
- `<module>/CLAUDE.md` — per-module architecture notes and landmines.
- `claude-docs/REFACTOR-LOG.md` — why things are the way they are, in narrative form.
