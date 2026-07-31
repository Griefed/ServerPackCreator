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
