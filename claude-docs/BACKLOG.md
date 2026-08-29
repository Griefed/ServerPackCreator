# Backlog — deferred, agreed-to work

Items consciously deferred, with the reason and enough context to pick them up cold. Newest section first.
Not a wish-list: everything here was looked at, judged worth doing, and postponed for a stated reason.
When an item lands, delete it here and record it in `REFACTOR-LOG.md`.

> **Numbering continues from B34 — it does not restart, and an ID is never reused.** This file is empty because
> every item landed or was dropped, not because the counter reset. B1–B34 have all been issued, and some are still
> cited by name elsewhere in the repo after their entries went away, so reusing an ID silently repoints someone
> else's citation at the wrong item. `git log -S'B<n> —' -- claude-docs/BACKLOG.md` recovers what any past ID meant.

Add the next item under a dated section, starting at **B36**, with the reason it waited and enough context to pick
it up cold.

## 2026-08-29

### B35 — `JsonVerdictStore.persist()` rewrites the whole file on every `record()`

**What.** Every single `record()` serialises the entire in-memory map with
`writerWithDefaultPrettyPrinter()` to a temp file and atomically moves it over the store — inside
`@Synchronized`, on a grind worker's thread. Cost is therefore O(store) per verdict, not O(1).

**Why it waited.** It is on the **grind** path, not the report path, and it did not block the boot-log,
rule-engine or dependency work that surrounded it. It also wants a *measurement* before a fix, per this
repo's own rule that build- and performance-shaped changes are justified by numbers rather than by
reasoning — and taking that measurement properly means a store far larger than the 875 rows on hand.

**Pick it up with.** Time `record()` against synthetic stores of 1 k / 10 k / 100 k verdicts and put the
three numbers in the commit message. At 100 k rows the current shape is a multi-megabyte serialise plus an
atomic move *per verdict*, so the interesting question is whether an append-log-plus-periodic-compaction,
or simply dropping the pretty-printer, is enough — the pretty-printing alone may be a large share of it.

**Do not** conflate this with the report's in-memory filter/sort, which was measured as fine (~50–150 ms at
100 k rows for a human-facing page) and is a different concern entirely.
