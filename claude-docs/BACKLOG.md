# Backlog — deferred, agreed-to work

Items consciously deferred, with the reason and enough context to pick them up cold. Newest section first.
Not a wish-list: everything here was looked at, judged worth doing, and postponed for a stated reason.
When an item lands, delete it here and record it in `REFACTOR-LOG.md`.

## Empty

Every recorded item has landed. The last one, **B11** (the `installCorepackLatest` Corepack workaround), went when
Griefed's Node 24.18.1 bump made it unnecessary — verified by building the frontend with the `dependsOn` disabled:
`installQuasar` succeeded, `installCorepack` was skipped, and `installFrontend`/`assembleFrontend` both completed.

Add the next item under a dated section, with the reason it waited and enough context to pick it up cold.
