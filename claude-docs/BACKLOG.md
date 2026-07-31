# Backlog — deferred, agreed-to work

Items consciously deferred, with the reason and enough context to pick them up cold. Newest section first.
Not a wish-list: everything here was looked at, judged worth doing, and postponed for a stated reason.
When an item lands, delete it here and record it in `REFACTOR-LOG.md`.

## Deferred 2026-07-30 — while the catalog sweep is the critical path

These were ranked below sweep-output quality: none of them change what the grinder produces, which is why they
waited. Griefed asked for them to be recorded and tackled afterwards.

### B4 — `LarsonScanner.kt` (2,217 lines)
Self-contained Swing widget, no dependants beyond the GUI, no known defects. Left alone deliberately: splitting it
buys nothing a reader needs today. `ConfigEditor.kt` (1,369) was assessed and closed — its two pure pieces are
already extracted and the rest is legitimate view code.

### B5 — Verdict store dedup is `slug` + `platform`, not project identity
A project that changes its slug on a platform is re-ground as a new project and its old verdicts linger. Fine today;
would want a stable project id if the store is ever published as a long-lived dataset.

## Existing TODO markers in the codebase (recorded 2026-07-31)

Every `TODO` presently in SPC's sources, so they are tracked somewhere other than a grep. (A fourth apparent hit,
`Translations_pt_BR.properties:636`, is a false positive — `TODO` is Portuguese for "all".)

### B11 — `installCorepackLatest` is a workaround for an upstream Corepack bug
`buildSrc/.../serverpackcreator.quasar-conventions.gradle.kts:31` — *"Remove once the error, which caused this task to
exist in the first place, is fixed in NodeJS/Corepack."* Tracks
[nodejs/corepack#612](https://github.com/nodejs/corepack/issues/612#issuecomment-2631491212). The task globally
installs `corepack@latest` before `installQuasar`, adding a network round-trip to every frontend build. Re-check the
upstream issue periodically; when fixed, drop the task and the `dependsOn`.

## Found while executing the 2026-07-31 plan — not planned, not yet done

Each of these was observed and verified during the plan's phases but fell outside their scope. Newest concern first.

### B21 — eight template paths are captured at construction and do not follow a changed home directory
Found while withdrawing audit finding L-C (2026-07-31). `PathsConfig` resolves 31 of its properties through a
re-deriving getter (`var x = …; get() { field = …; return field }; private set`) precisely so they track a home
directory that changes at runtime — `serverFilesDirectory` (`:573-578`) does this on top of `homeDirectory`,
which **this branch made re-resolve on every access**.

The eight script-template properties do not (`PathsConfig.kt:586`, `:594`, `:603`, `:611`, `:619`, `:627`,
`:635`, `:643`):

```kotlin
val defaultShellScriptTemplate = File(serverFilesDirectory, "default_template.sh")   // evaluated once
```

They are plain `val`s evaluated at construction, so after a home change (`--home`, the `-D` override, or the
GUI's settings panel) they still point into the **old** home while everything around them has moved. These feed
`defaultStartScriptTemplates()` / `defaultJavaScriptTemplates()`, so the consequence is generation reading
templates from a directory the user has left behind — silent, and it looks like "my template edits do nothing".

Not fixed with the audit finding because it predates this range, spans eight properties, and needs its own pin:
a test that changes the home mid-instance and asserts the template paths follow. The fix is either the
re-deriving getter the other 31 use, or a computed `val … get() =`, which is the cleaner Kotlin and behaviour-
identical (the field write in that pattern is dead — the getter recomputes unconditionally).

### B22 — the grinder writes `serverpackcreator.properties` and `log4j2.xml` into the repository root on every start
Observed 2026-07-31 while restoring the sweep. Both files appear untracked in the repo root seconds after the
daemon starts, and stay gone when it is stopped — verified by stopping it, deleting them, and waiting: nothing
reappears, so the daemon is the writer.

Neither obvious lever changes it:

- its working directory was **verified** to be `~/.spc-grinder` (via `lsof -d cwd`), so this is not the
  CWD-relative default of `ApiWrapper.api()` (`GrinderApplication.kt:85`);
- pointing `SPC_GRINDER_SPC_PROPERTIES` at `~/.spc-grinder/serverpackcreator.properties` (the path
  `GrinderApplication.kt:84` honours) makes it *read* from there but it still writes the pair into the repo root.

**The test suite is ruled out as the producer**, which is the natural first suspicion since it *was* the cause of
this class of pollution before 2026-07-31: a full module test run now leaves the repository root untouched,
because `serverpackcreator.java-conventions.gradle.kts:44` pins every module's test home to `<module>/tests`.
Confirmed by running a suite with the root clean and re-checking `git status`. Only the daemon reproduces it, and
only at startup — the pair can be deleted while it runs and does not come back until the next start.

Same class as the test-suite pollution fixed on 2026-07-31 (`ef3280e4c`/`e7cce83fb`) and the reason that one was
worth fixing: artifacts landing outside the home a process was told to use. Consequence today is a permanently
dirty `git status` while a sweep runs, which is how a genuinely unexpected file gets overlooked. Worth checking
whether `ApiProperties`' log4j `ConfigurationFactory` role writes `log4j2.xml` relative to something captured at
class-load rather than to the resolved home, since `log4j2.xml` is the more surprising of the two.

### B23 — a failed Minecraft server-manifest fetch is never remembered, so every lookup retries it
Found by the fourth audit (2026-07-31) while reducing the log volume that finding introduced.
`MinecraftServer.setServerJson()` re-downloads whenever `manifestFile` is absent and, on failure, leaves
`serverJson` null — so the next call tries again. `MinecraftMeta.getServer` (`:166`) then evaluates
`server.url().isPresent && server.javaVersion().isPresent`, and **both** call it: one `requiredJavaVersion`
lookup on a version whose manifest cannot be fetched costs **two** download attempts.

That lookup is hot: `ImageJavaRuntimes.requiredJavaMajor` reaches it from `supportFor`, `javaPath` and
`installerJavaPathFor` — per candidate in `ContainerCandidateVerifier`, per cell in `ScriptTemplateMatrixIT`, and
from the GUI on every version selection (`ConfigEditor.kt:681`). So a single unfetchable version can generate
network attempts in proportion to catalogue size, silently.

The logging is now `debug` and message-only, so the *symptom* is gone; the retry is not. The fix is to remember the
failure per instance (a resolved-or-null cache, so a miss is answered from memory) and it wants a pin — which needs
a download seam `MinecraftServer` does not currently have, since it constructs its own fetch through `utilities`.
That seam is the actual work, and the reason this is an entry rather than a same-day fix. **Note the exported
`Optional` contract must not change:** callers read empty as "no server available", and B20's consumer-side
distinction (`ImageSupport.REQUIREMENT_UNKNOWN`) already depends on that shape.
