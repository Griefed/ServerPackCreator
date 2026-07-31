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

### B13 — the fish and PowerShell template changes are unverified by execution
Phase 1 and its follow-up changed three decisions in **all three** start-script templates (Forge launcher era, the
NeoForge 1.20 installer coordinate, and dropping `-Djava.security.manager=allow` on Java 24+). Only **bash** was
verified by running the extracted functions; `.fish` and `.ps1` were changed by analogy and are covered only at the
content level. `fish -n` did not even run here — `ScriptTemplateContentTest` skips it when no fish binary is present,
which was the case. The thing that would actually execute them is `ScriptTemplateMatrixIT` (gated
`GRINDER_TEMPLATE_IT=1`, needs the `spc-grinder-templates` image), and it has **not** been re-run since those changes.
Precedent for why this matters: the matrix IT's first real run caught a fish-only bug (`string split` keeping empty
tokens) that content assertions had missed entirely. Run it, one shell at a time if need be —
`SPC_GRINDER_TEMPLATE_WORKERS` must stay at 1 (parallel cells starve the host and produce spurious `Killed` failures).

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
