# Java version references — everything a version bump has to touch

ServerPackCreator targets **Java 21**. Gradle reads that from one place; nothing else can, so this file
is the map of everything else. **Open it before bumping, and work down it.**

Every link is relative to the repository root, so it resolves on any host and in any editor.
Declarations are named rather than line-numbered — line numbers go stale with the next edit, names do
not.

---

## A — The one entry Gradle reads (no edit needed below it)

`java = "21"` in **[`gradle/libs.versions.toml`](../gradle/libs.versions.toml)**. Five Gradle sites read
it, by two different routes, and none of them carries a version of its own:

| File | Reads it as | Sets |
|---|---|---|
| [`buildSrc/build.gradle.kts`](../buildSrc/build.gradle.kts) | `libs.versions.java` | buildSrc's own toolchain and `jvmTarget` |
| [`build.gradle.kts`](../build.gradle.kts) | `libs.versions.java` | the IDEA language level |
| [`serverpackcreator.java-conventions.gradle.kts`](../buildSrc/src/main/kotlin/serverpackcreator.java-conventions.gradle.kts) | `VersionCatalogsExtension` | the Gradle toolchain for every module |
| [`serverpackcreator.kotlin-conventions.gradle.kts`](../buildSrc/src/main/kotlin/serverpackcreator.kotlin-conventions.gradle.kts) | `VersionCatalogsExtension` | the Kotlin `jvmTarget` |
| [`serverpackcreator.dokka-conventions.gradle.kts`](../buildSrc/src/main/kotlin/serverpackcreator.dokka-conventions.gradle.kts) | `VersionCatalogsExtension` | Dokka's `jdkVersion` external-link base |

**Why two routes.** A precompiled script plugin cannot use the type-safe `libs` accessor — that fails
`:buildSrc:compilePluginsBlocks` with `Unresolved reference: libs`, the landmine in
[`.claude/rules/build-layout.md`](../.claude/rules/build-layout.md) — but it *can* read the catalog of
the project it is applied to. `buildSrc/build.gradle.kts` and the root script are real build scripts, so
they use the accessor.

**Measured, 2026-10-03, by changing that one entry:**

| `java =` | Outcome |
|---|---|
| `"21"` | green; `serverpackcreator-api` classes carry class-file major **65** |
| `"17"` | fails — *"this component declares a component, compatible with Java 21 and the consumer needed a component, compatible with Java 17"*. buildSrc's own plugin dependencies require 21, so **downwards is blocked regardless** |
| `"25"` | fails — *"Dependency requires at least JVM runtime version 25. This build uses a Java 21 JVM."* |

**That last line is the real prerequisite for an LTS bump and it is not in any file:** the Gradle daemon
must itself run on the new JDK, because buildSrc's output is loaded by the daemon. Locally that means
`org.gradle.java.home` or `JAVA_HOME`; in CI it is section B, which must move **first**.

### Not a Java version, despite the name

`org.siouan.frontend-jdk21` in [`gradle/libs.versions.toml`](../gradle/libs.versions.toml) and
[`serverpackcreator.quasar-conventions.gradle.kts`](../buildSrc/src/main/kotlin/serverpackcreator.quasar-conventions.gradle.kts)
is a **plugin id**. The suffix names the minimum JDK the plugin itself runs on, not what SPC targets.
It does not move on a bump, and the `-jdk21` variant runs fine on newer JDKs.

---

## B — CI runner JDKs — **bump these first**

Twelve `setup-java` steps, every one `distribution: 'zulu'` and `java-version: '21'`. The daemon runs on
whatever these install, so section A cannot move until these have.

- [`.github/workflows/test.yml`](../.github/workflows/test.yml),
  [`pages.yml`](../.github/workflows/pages.yml),
  [`clientside-accept.yml`](../.github/workflows/clientside-accept.yml),
  [`clientside-report-reusable.yml`](../.github/workflows/clientside-report-reusable.yml) — one each
- [`.forgejo/workflows/test.yml`](../.forgejo/workflows/test.yml),
  [`docs.yml`](../.forgejo/workflows/docs.yml),
  [`qodana.yml`](../.forgejo/workflows/qodana.yml),
  [`grinder-container-it.yml`](../.forgejo/workflows/grinder-container-it.yml) — one each
- [`.forgejo/workflows/devbuild.yml`](../.forgejo/workflows/devbuild.yml) and
  [`release-build.yml`](../.forgejo/workflows/release-build.yml) — **two each**

The step names say `Set up JDK 21`, so grep for the name as well as the value.

**Zulu is the right distribution to stay on.** Adoptium publishes no `windows/aarch64` JDK 25 GA (only
`-ea-beta`); Zulu and the Microsoft build publish it for both 21 and 25. Measured 2026-10-03.

---

## C — Qodana

[`qodana.yaml`](../qodana.yaml) — `projectJDK: "21"`, and `linter: jetbrains/qodana-jvm-community:<tag>`.
The same image tag is repeated at two more places in
[`.forgejo/workflows/qodana.yml`](../.forgejo/workflows/qodana.yml).

**The linter has to support the target before `projectJDK` can move.** This has blocked the project
before — the changelog records *"Qodana does not support Java 21 yet"*. Check the image, then bump.

---

## D — Bundled and documented runtimes

These ship a JDK to a user, or tell a user which one to install. None of them reads section A.

| What | Where | Note |
|---|---|---|
| AppImage JDK | [`misc/build-appimage.sh`](build-appimage.sh) | `JDK_VERSION` **and** the pinned `JDK_RELEASE`; `JDK_MAX_GLIBC` beside them is the portability floor, and `misc/AppImage-Portability.md` says what it costs to raise it |
| install4j minimum | [`spc.install4j`](../spc.install4j) | `javaMinVersion` on `<application>` |
| install4j bundled JRE | [`spc.install4j`](../spc.install4j) | `<jreBundles jdkProviderId="Adoptium" release="21/jdk-21.0.4+7">`; the three per-media `<jreBundle>` elements inherit it |
| Docker build stage | [`docker/Dockerfile`](../docker/Dockerfile) | `FROM eclipse-temurin:21-jdk-jammy AS builder` |
| Docker runtime stage | [`docker/Dockerfile`](../docker/Dockerfile) | `openjdk-21-jdk-headless` |
| Documented run command | [`README.md`](../README.md) | `eclipse-temurin:21-jre` |

**Two of these are real constraints rather than string swaps:**

- The Docker **runtime** stage is an Ubuntu *noble* apt package on
  `ghcr.io/linuxserver/baseimage-ubuntu:noble`. `openjdk-25-jdk-headless` is **not in noble's archive**,
  so that line needs a different base, a PPA, or a tarball install — decide it before starting.
- install4j's JRE bundles come from **Adoptium**, which publishes JDK 25 GA for aix/ppc64,
  alpine-linux/{aarch64,x64}, linux/{aarch64,ppc64le,riscv64,s390x,x64}, mac/{aarch64,x64} and
  **windows/x64** — covering every media set [`spc.install4j`](../spc.install4j) defines today, but not
  `windows/aarch64` should one ever be added. Measured 2026-10-03.

### Paired with D: the AppImage JDK cache keys

[`.forgejo/workflows/devbuild.yml`](../.forgejo/workflows/devbuild.yml) caches the AppImage JDK under
`jdk-21-x86_64` and `jdk-21-aarch64`, as both `path:` and `key:`, hard-coded — **not** derived from
`build-appimage.sh`. [`release-build.yml`](../.forgejo/workflows/release-build.yml) names the same
directories in prose. Bump the script alone and the cache points at a directory that is never produced:
a silent miss every run, never a failure.

---

## E — Host prerequisites for the grinder deployment

- [`serverpackcreator-grinder/deploy/install-grinder.sh`](../serverpackcreator-grinder/deploy/install-grinder.sh)
  — `openjdk-21-jdk`, twice (one comment, one live `apt install`)
- [`serverpackcreator-grinder/README.md`](../serverpackcreator-grinder/README.md) — "JDK 21+", four places

---

## F — IDE and developer setup

- [`.runConfigurations/Debug Fat Jar.run.xml`](../.runConfigurations/Debug%20Fat%20Jar.run.xml) —
  `ALTERNATIVE_JRE_PATH` is `temurin-21`
- `.idea/misc.xml` — `languageLevel="JDK_21"`, `project-jdk-name="21"`. **Untracked**, so it is each
  developer's own and no bump commit touches it.

---

## G — Documentation prose

Root files only. The copies under `serverpackcreator-help/Writerside/topics/` are **generated and
gitignored**, and editing them is silently undone by the next build.

- [`BUILD.md`](../BUILD.md) — the prerequisite, the convention-plugin table, and the "No matching
  toolchains found" troubleshooting entry
- [`README.md`](../README.md) — "Using the JAR-file release requires Java 21."
- [`CONTRIBUTING.md`](../CONTRIBUTING.md) — "You need a **JDK 21** installed"

---

## H — DO NOT TOUCH: the generated server pack's Java

**These are not SPC's Java version and must not move with it.** They track Mojang's per-Minecraft-version
`requiredJavaVersion` — 8, 17, 21 and 25 all appear, deliberately — and changing them is a behaviour
change to every server pack SPC produces.

- The start-script templates under
  [`serverpackcreator-api/src/main/resources/de/griefed/resources/server_files/`](../serverpackcreator-api/src/main/resources/de/griefed/resources/server_files/):
  `variables.txt` (`RECOMMENDED_JAVA_VERSION`, `JDK_VENDOR`), the four `default_template.*` and the three
  `default_java_template.*`
- `MinecraftMeta.requiredJavaVersion` in
  [`serverpackcreator-api`](../serverpackcreator-api/src/main/kotlin/de/griefed/serverpackcreator/api/versionmeta/minecraft/MinecraftMeta.kt),
  and the GUI/view-model code that reads it
- [`HELP.md`](../HELP.md) — the `SPC_RECOMMENDED_JAVA_VERSION_SPC` placeholder docs
- The grinder's boot image: [`serverpackcreator-grinder/docker/Dockerfile`](../serverpackcreator-grinder/docker/Dockerfile)
  installs `temurin-{8,17,21,25}-jdk` and symlinks `/opt/java-{8,17,21,25}`, and
  [`ImageJavaRuntimes`](../serverpackcreator-grinder/src/main/kotlin/de/griefed/serverpackcreator/grinder/loader/ImageJavaRuntimes.kt)'s
  `bundledMajors` must list exactly the same set.
  **That pairing is hand-maintained with no build-time check** — the Dockerfile says so in a comment, and
  only `ImageJavaRuntimesTest` guards the behaviour, with its own literal sets.

The grinder image's `ENV JAVA=/opt/java-21` and `ENV PATH=/opt/java-21/bin:...` are the *default* for a
boot, also a Minecraft-driven choice rather than SPC's target.

---

## I — Cosmetic

[`serverpackcreator-api/src/test/resources/testresources/properties/filters/`](../serverpackcreator-api/src/test/resources/testresources/properties/filters/)
— five `.properties` fixtures carry a Windows `jdk-21.0.1.12-hotspot` path in
`de.griefed.serverpackcreator.java`. Fixture data for the filter tests; nothing resolves it.

---

## The order to do it in

1. **C** — confirm the Qodana linter supports the target, or the scan job goes red and stays red.
2. **B** — the runner JDKs, so the Gradle daemon runs on the new version.
3. **A** — one line. The build now compiles to it.
4. **D** — the bundled runtimes and the cache keys paired with them, checking the two constraints above.
5. **E**, **F**, **G** — prerequisites and prose.
6. Leave **H** alone, and say in the commit message that you did.
