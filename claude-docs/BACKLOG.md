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

### B13 — the fish and PowerShell template changes are unverified by execution — **DONE 2026-07-31**
Phase 1 and its follow-up changed three decisions in **all three** start-script templates (Forge launcher era, the
NeoForge 1.20 installer coordinate, and dropping `-Djava.security.manager=allow` on Java 24+). Only **bash** had been
verified by running the extracted functions; `.fish` and `.ps1` were changed by analogy.

Closed by running `ScriptTemplateMatrixIT` over `1.16.1,1.20.1,26.2` x `Forge,NeoForge,Fabric` x `bash,fish`
(`SPC_GRINDER_TEMPLATE_WORKERS=1`, 23 min): **16 of 18 boot cells reached the vanilla ready-line, bash == fish in every
single cell**, plus both PowerShell tests (own-parser parse of both `.ps1` templates, and executing the shipped
`RunInstallerJavaCommand` for the installer-JDK fallback *and* override). The Java-24+ guard was confirmed on a real
Java 25 boot rather than by inspection: `SSJ Forge Args: -Djava.security.manager=allow` is echoed while the emitted
`Run Command` omits it. **Note the default dimensions cover no `26.x` version** — pass `SPC_GRINDER_TEMPLATE_MC`
explicitly, or the new-scheme branch that motivated Phase 1 is never executed.

The two failures were the same cell in both shells and are recorded as B19 (a real Forge-26.x defect) and B20 (the
silent N/A that hid it for a whole run).

### B14 — `HostProcessServerRunner` still spends a boot's budget while the host is asleep
B9 fixed the wall-clock deadline in `DockerJavaContainerEngine`, but `ServerRunner.kt:114-118` has the identical
`System.currentTimeMillis()` deadline for the **host-process** runner — the path the app's `-verifyclientside` CLI verb
uses. A suspend there still writes off a boot that never got its time. The fix is the same shape as B9's
(`isSuspendGap` is already pure and could simply be shared), and the reason it was not done with B9 is only that the
grinder was the observed victim.

### B15 — the loader-cache marker records success, not which template produced it
Phase 1 had to invalidate two cached Forge tuples **by hand**: the install boot runs the same start-script templates, so
those layers had been produced by the buggy legacy branch (no `server.jar`), and `LoaderCache`'s completion marker made
`ensureInstalled` serve them regardless. Any future template change that alters what an install *produces* has the same
problem, and nothing warns. Option: record a hash of the templates (or SPC's version) in the marker and treat a
mismatch as a miss. Cheap to do, and it removes a manual step nobody will remember.

### B16 — the checked-in test properties still carry machine-specific absolute paths
The audit's M1 had two halves. The suite no longer *rewrites*
`serverpackcreator-{api,clientside}/src/test/resources/serverpackcreator.properties` (fixed 2026-07-31), but they are
still committed containing absolute paths from one developer's machine — `de.griefed.serverpackcreator.java=/Users/…/
sdkman/…` and `server.tomcat.basedir=/Users/…`. Harmless while only that machine runs them; misleading for anyone else
and meaningless in CI. Either relativise them or generate them into `build/` at test time.

### B17 — `.gitignore` hides new files under `server_files`
`.gitignore:365` ignores `server_files` wholesale, so the shipped templates are tracked only because they predate the
rule. Adding `variables.txt` in Phase 3 needed `git add -f`, and the next shipped resource will be silently untracked
unless somebody remembers — a packaging bug that only shows up as a file missing from a release. Narrow the rule (ignore
the *home* directory's `server_files`, not the resource path) or add explicit negations for the tracked resources.

### B18 — an install failure's console is destroyed by the next attempt on that tuple
`DockerLoaderInstaller` streams the install console to `work/install/<tuple>/install.log`, which correctly survives the
pack cleanup — but `VanillaPackGenerator.generate` wipes the whole tuple directory at the start of the next attempt, so
the failing run's log is gone exactly when a retry makes you want to compare the two. The failure warning does embed the
container's last 25 lines (which is what made Phase 1's Java 24 diagnosis possible at all), so this is a convenience
gap rather than a blind spot: keep the previous log as `install.log.previous`, or write it under the tuple's cache entry
instead.

### B19 — a fresh Forge pack on Minecraft 26.x installs and exits 0 without launching — **DONE 2026-07-31**
Root cause was not the URL-vs-version argument this entry suspected. ServerStarterJar runs the Forge installer in
its **own JVM** and needs a `SecurityManager` (`SecurityAccess.wrapNoForceExit`) to swallow the `System.exit(0)`
the installer calls on success; JEP 486 makes that impossible from Java 24 and SSJ swallows the failure silently,
so the installer's exit kills the process before launch. Fixed by `203a32534` (red pin) + `f6c23e972`: from Java 24
on, the templates install Forge themselves and launch from the installer's argfile; below 24 the SSJ path is
untouched. Verified fresh-pack, first-invocation on Minecraft 26.2 in bash **and** fish, with Forge 1.20.1 and
NeoForge 26.2 green as regression controls. Full landmine in `serverpackcreator-api/CLAUDE.md`.

### B20 — a missing per-version Minecraft manifest silently becomes "N/A", deleting matrix coverage
`MinecraftServer.javaVersion()` wraps its body in `catch (e: Exception) -> Optional.empty()`, and `getServer()` requires
both `url()` and `javaVersion()` to be present, swallowing anything else. So a per-version json that is absent and fails
to download is indistinguishable from "this version declares no Java". `ImageJavaRuntimes.supports()` maps that to
`false`, and `ScriptTemplateMatrixIT` then reports the cell as `[N/A] SKIPPED` with the reason "loader has no build for
Minecraft X (or its JDK is not bundled)" — which reads as a legitimate exclusion.

Observed on 2026-07-31: the first matrix run skipped **all four 26.2 cells**, including Fabric, which the sweep boots
fine. Cause was first-run staleness — `VersionMeta` refreshed the manifest *during* setup, after the in-memory server
map had been built, so `26.2` was missing from the map while present in the file on disk. The second run picked it up
and all three loaders became valid. A silent N/A on the newest Minecraft version is the worst possible place for this:
the run still reports green while skipping exactly the new-scheme branch the templates were fixed for. At minimum the IT
should distinguish "loader genuinely has no build" from "this Minecraft version's Java requirement is unknown" and be
loud about the second; the swallow in `javaVersion()` should also not hide a failed download.

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
