# Backlog — deferred, agreed-to work

Items consciously deferred, with the reason and enough context to pick them up cold. Newest section first.
Not a wish-list: everything here was looked at, judged worth doing, and postponed for a stated reason.
When an item lands, delete it here and record it in `REFACTOR-LOG.md`.

## Deferred 2026-07-30 — while the catalog sweep is the critical path

These were ranked below sweep-output quality: none of them change what the grinder produces, which is why they
waited. Griefed asked for them to be recorded and tackled afterwards.

### B1 — `-app`'s five hard-coded `Preferences` node call-sites
`CommandlineParser.kt:98`, `ServerPackCreator.kt:72` and `:99`, `HomeDirCommand.kt:59`, `GuiProps.kt:504` still use
`Preferences.userRoot().node("ServerPackCreator")` literally. `-api` now resolves the node through
`ApiProperties.resolvePreferencesNode()`, and the grinder + test suites each claim their own, so the grinder is safe;
what remains is that a **test-suite run can still move a developer's own GUI installation's home directory**, and the
shared node currently holds a repo test path on Griefed's machine. Mechanical fix: route these through the same
resolver. Watch out: `GuiProps` is GUI state (window geometry etc.), so moving it to a different node **loses a
user's saved layout** — that one needs a deliberate decision, not a blind sed.

### B2 — NeoForge builds whose installer artifact is missing upstream
`21.1.247` is listed in `maven.neoforged.net`'s version index but `neoforge-21.1.247-installer.jar` **404s** (verified
2026-07-30), so SPC's metadata is right and the artifact simply is not there. Observed effect is smaller than first
assumed — that build still produced `SURVIVED` verdicts (17 of them), because a cached install layer can already
exist — but a cold cache wastes an install attempt per candidate. Option: have `LoaderCache` fall back to the
next-newest *installable* build on a download failure. **Careful:** `latestVersion` alone drives the support gate and
the crash re-check (`serverpackcreator-clientside/CLAUDE.md`), so a fallback must not quietly redefine "newest".

### B3 — Frontend component-test breadth
Phase 4 is complete (Vitest, full TS migration, all cards + nav SFCs, suite at 23). Tables are untested **by
design** — trivial format-lambda logic vs. brittle QTable rendering. Only worth extending if a real bug appears there.

### B4 — `LarsonScanner.kt` (2,217 lines)
Self-contained Swing widget, no dependants beyond the GUI, no known defects. Left alone deliberately: splitting it
buys nothing a reader needs today. `ConfigEditor.kt` (1,369) was assessed and closed — its two pure pieces are
already extracted and the rest is legitimate view code.

### B5 — Verdict store dedup is `slug` + `platform`, not project identity
A project that changes its slug on a platform is re-ground as a new project and its old verdicts linger. Fine today;
would want a stable project id if the store is ever published as a long-lived dataset.

### B6 — Modrinth's offset ceiling
Measured at 99,999 against ~71,000 `project_type:mod` projects, so the whole catalog is reachable **today**. If it
ever exceeds 100,000 the tail silently looks like the end of the catalog and the crawl wraps early, losing coverage
with no error. Worth a guard that logs when `offset` approaches the ceiling.

### B7 — Worker sizing for the production host
The grinder will run on a machine with **~80 GB free memory** (Griefed, 2026-07-30), not on the dev box whose Docker
VM is deliberately capped at 1.93 GiB. Sizing rule: `SPC_GRINDER_WORKERS ≈ (memory available to Docker − overhead) /
per-boot cap`, with the per-boot cap being `ContainerResources.memoryBytes` (3 GiB default) — so ~20+ workers there,
versus **1** on the dev box. The Docker VM's memory must exceed `workers × cap`, or boots are OOM-killed rather than
capped (which is what made the killed/OOM classifier guard necessary). Worth putting in `README.md` §5 as explicit
guidance rather than leaving operators to infer it.

## Found 2026-07-30 during the sweep — next dominant cause

### B8 — every Forge boot on newer Minecraft fails to launch (`Unable to access jarfile forge.jar`)
**24 boot logs, all Forge**, never started the server at all: the generated `start.sh` reports
`Launcher JAR: forge.jar` / `Run Command: … -jar forge.jar nogui` — the template's **legacy** Forge branch — while
no `forge.jar` exists in the pack or anywhere under the loader cache. Modern Forge installs use
`@libraries/net/minecraftforge/forge/<mc>-<ver>/unix_args.txt` instead (`default_template.sh` has that branch too, at
the `SERVER_RUN_COMMAND="@user_jvm_args.txt @libraries/…/unix_args.txt nogui"` line), so either the branch condition
misfires for these Minecraft versions — worth checking against the new `26.x` versioning, which breaks any `1.x`
numeric assumption — or the grinder's pre-baked install layer does not contain what the chosen branch expects.
Affected examples: `balm`, `better-advancements`, `biomes-o-plenty`, `cherished-worlds`, `collective`,
`cubes-without-borders`, `cyclops-core`, `euphoria-patches`, `forge-config-api-port`, `geckolib`, `iceberg`,
`inventory-profiles-next`.

**Cost:** a full boot (~70 s) per Forge candidate, learning nothing — Forge coverage is effectively zero on those
versions. It is no longer *dangerous* (these now classify INCONCLUSIVE via `launchFailureMarkers` rather than being
promoted to a false clientside HIGH), which is why it is backlog and not an emergency, but it is the largest remaining
waste and the biggest blind spot in the deliverable. Reproduce with a one-shot on any of the mods above and read
`work/verify/boot/<slug>-Forge/boot.log`.

### B9 — boot deadlines are wall-clock, so a host suspend writes off good boots
`BootVerifier`'s `bootTimeout` (12 min in the grinder) is measured against wall-clock, not against time the boot was
actually allowed to run. A host that suspends mid-boot therefore blows the deadline while the server is frozen, and the
run is recorded INCONCLUSIVE even though it succeeded. Measured 2026-07-31: the dev box idle-slept in a repeating
~16-minute cycle overnight, and **19 of 153 verdicts** came back `timed out` — including several reading
`SURVIVED (timed out)`, whose console shows the server reaching `Done (6.572s)!` seconds after launch. The wake times
in `pmset -g log` line up with the grinder's log gaps to the second.

Mitigated operationally by launching under `caffeinate -ims` (assertions are held by a child of the grinder JVM, so
they expire with it) — note `PreventSystemSleep` only binds on AC, and closing the lid sleeps regardless. Irrelevant on
the ~80 GB production host, which does not suspend, which is why this is backlog rather than a fix. If it is ever worth
closing properly: measure the deadline against a monotonic clock **and** detect a suspend (a jump between successive
log-line timestamps far larger than the poll interval) so the boot can be re-run rather than scored, since a frozen JVM
resumes into a world where its own timers already expired.

## Existing TODO markers in the codebase (recorded 2026-07-31)

Every `TODO` presently in SPC's sources, so they are tracked somewhere other than a grep. (A fourth apparent hit,
`Translations_pt_BR.properties:636`, is a false positive — `TODO` is Portuguese for "all".)

### B10 — `ServerPackProvisioner`'s `variables.txt` content is a Kotlin string literal
`serverpackcreator-api/.../serverpack/ServerPackProvisioner.kt:53` — *"move to template file, just like the scripts."*
The whole `variables.txt` body, comments and escaping guidance included, is a multi-line string constant in Kotlin, so
changing operator-facing documentation means editing and recompiling the API. The start scripts already live in
`src/main/resources/de/griefed/resources/server_files/` and are copied into SPC's home for users to adjust; this should
follow the same route. **Worth knowing before touching it:** those templates are copied into the SPC home directory and
are then read from *there*, not from the jar — a change to the shipped file does not reach an installation whose home
already exists (see `serverpackcreator-grinder/CLAUDE.md`). Any move must decide what happens to an existing
`variables.txt` on upgrade.

### B11 — `installCorepackLatest` is a workaround for an upstream Corepack bug
`buildSrc/.../serverpackcreator.quasar-conventions.gradle.kts:31` — *"Remove once the error, which caused this task to
exist in the first place, is fixed in NodeJS/Corepack."* Tracks
[nodejs/corepack#612](https://github.com/nodejs/corepack/issues/612#issuecomment-2631491212). The task globally
installs `corepack@latest` before `installQuasar`, adding a network round-trip to every frontend build. Re-check the
upstream issue periodically; when fixed, drop the task and the `dependsOn`.

### B12 — `ForgeLoader.forgeVersionFrom` has no length guard
`serverpackcreator-api/.../versionmeta/forge/ForgeLoader.kt:147`. Low priority and **not** the obvious fix — see the
TODO itself. Measured against the real manifest, all 5025 entries across 77 Minecraft keys carry their own key as a
prefix, so the only unhandled shape is an entry equal to its key with nothing after it, which throws
`StringIndexOutOfBoundsException`; `update()` catches only `MalformedURLException` and `NoSuchElementException`, so it
would abort the whole Forge load rather than cost one version. A length check closes it. Do **not** use
`startsWith("$minecraftVersion-")`: entries carry the raw manifest key while the Minecraft version may be reconciled
(`1.7.10_pre4` → `1.7.10-pre4`), so that guard would reject a legitimate entry. Behaviour is pinned by
`ForgeVersionMappingTest`, which must be updated alongside any fix.
