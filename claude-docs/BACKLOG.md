# Backlog — deferred, agreed-to work

Items consciously deferred, with the reason and enough context to pick them up cold. Newest section first.
Not a wish-list: everything here was looked at, judged worth doing, and postponed for a stated reason.
When an item lands, delete it here and record it in `REFACTOR-LOG.md`.

## Deferred 2026-07-30 — while the catalog sweep is the critical path

These were ranked below sweep-output quality: none of them change what the grinder produces, which is why they
waited. Griefed asked for them to be recorded and tackled afterwards.

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

### B24 — every test run wipes `<module>/tests`, so cached version metadata never survives a run
Found 2026-07-31 while removing the build's shared-Preferences writes. `java-conventions`' `cleanup()` runs in
`doFirst` of both `test` and `clean` and deletes everything under `<module>/tests` except `.gitkeep`. Since that
directory is now also each module's SPC **home**, the deletion takes the version manifests and the per-version
`mcserver/*.json` cache with it, every run.

**This explains B20's disappearing metadata.** `26.2.json` had to be seeded by hand to get the Forge proof, and it
vanished again between runs — not deleted by `VersionMeta`, but by the build's own pre-test cleanup. It also means
`ScriptTemplateMatrixIT`'s first run after any `clean`/`test` starts from an empty metadata cache, which is when the
newest Minecraft versions are most likely to resolve as `REQUIREMENT_UNKNOWN`.

It further contradicts the root `CLAUDE.md` claim that the api suite needs **no live network** because "version
manifests are cached": with the cache wiped before every run, the suite re-fetches them. Worth measuring — run the
api suite with networking blocked and see what fails — before deciding whether the clean-slate guarantee or the
offline guarantee is the one to keep. Both are defensible; they cannot both be true as written.
