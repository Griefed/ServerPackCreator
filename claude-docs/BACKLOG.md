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

### B25 — the shipped per-version manifest snapshot lags its own parent manifest
Found 2026-07-31 while closing B24, which had the mechanism wrong: the version manifests **are** shipped as
resources (`serverpackcreator-api/src/main/resources/de/griefed/resources/manifests`, `mcserver/` included) and
`ApiWrapper.setup()` seeds them from the jar, so the offline guarantee has real backing. The defect is narrower and
it is a *data* problem: the shipped set is internally inconsistent.

`minecraft-manifest.json` lists **26.2** as the newest release, while `mcserver/` — 643 per-version files — has no
`26.2.json`, no `1.21.11.json` and no `1.21.1.json`; its newest is around the 1.20.1 era. So a fresh clone, or CI,
asks for a version the shipped manifest advertises and must fetch it. When that fetch fails or is slow, the answer
is "required Java unknown", which is what left the newest Minecraft versions out of the template matrix.

The project already has the refresh mechanism: `updateManifests` (`serverpackcreator-api/build.gradle.kts:134`)
copies `serverpackcreator-app/tests/manifests` into the shipped resources, and since B24 that directory now
*accumulates* newly-fetched versions instead of being wiped each run — so a run followed by `updateManifests`
genuinely advances the snapshot. Left as a maintainer decision because it is a large data commit (hundreds of
files) that needs network and the app suite, not a code change.

