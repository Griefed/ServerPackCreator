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

**Measured 2026-08-29** (`StoreWriteBenchTest`, gated behind `SPC_GRINDER_BENCH=1`; Apple silicon, JDK 21,
`-Xmx512m`). Per `record()`, against stores seeded directly on disk:

| rows | per `record()` | file |
|------:|---------------:|------:|
| 1 000 | 18–25 ms | 0.5 MiB |
| 10 000 | 74–83 ms | 4.6 MiB |
| 100 000 | 787–1050 ms | 45.8 MiB |

**The question is answered: dropping the pretty-printer is NOT enough.** Splitting `persist()` at 100 k rows:
sort **40.6 ms**, pretty write **707.5 ms** (45.6 MiB), compact write **360.7 ms** (38.2 MiB). So the printer
is roughly half the write and 16% of the bytes — a 2× win that still leaves ~360 ms *per verdict*. The cost is
the O(n) whole-file rewrite itself, and only changing *how often* or *how much* is written fixes it.

**Live scale, for sizing.** The deployed store held **38 258** verdicts on 2026-08-29 and grows monotonically;
that interpolates to ~150–200 ms per `record()` today. `persist()` is `@Synchronized` on the grind worker's
thread, so at the observed ~4.3 verdicts/second that is already a serialising bottleneck across all workers.

**Pick it up with — two candidates, both still open, and the choice is Griefed's:**
- *Coalesced writes.* `record()` marks dirty; one flusher persists at most every N seconds, plus a synchronous
  flush in the existing shutdown hook. Keeps the file format, the pretty-printing and the atomic move exactly
  as they are — the only thing that changes is durability, bounded to the flush interval. A lost verdict is
  re-derived by the TTL re-grind, so that cost is genuinely low. Cheapest real fix; needs a lifecycle on the
  store, which it currently has none of.
- *Append-log plus periodic compaction.* True O(1) per verdict, but a store-format change with recovery,
  compaction scheduling and dedup-on-load (`supersededLegacyKey`) semantics to get right — on a file holding
  38 k live verdicts. Deliberately **not** attempted at the tail of the dependency-resolution branch.

**Do not** land either without re-running the benchmark above and putting the before/after in the commit
message, per this repo's rule for performance-shaped changes.

**Do not** conflate this with the report's in-memory filter/sort, which was measured as fine (~50–150 ms at
100 k rows for a human-facing page) and is a different concern entirely.
