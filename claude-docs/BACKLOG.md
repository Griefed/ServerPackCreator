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

### B7 — Worker sizing for the production host
The grinder will run on a machine with **~80 GB free memory** (Griefed, 2026-07-30), not on the dev box whose Docker
VM is deliberately capped at 1.93 GiB. Sizing rule: `SPC_GRINDER_WORKERS ≈ (memory available to Docker − overhead) /
per-boot cap`, with the per-boot cap being `ContainerResources.memoryBytes` (3 GiB default) — so ~20+ workers there,
versus **1** on the dev box. The Docker VM's memory must exceed `workers × cap`, or boots are OOM-killed rather than
capped (which is what made the killed/OOM classifier guard necessary). Worth putting in `README.md` §5 as explicit
guidance rather than leaving operators to infer it.

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
