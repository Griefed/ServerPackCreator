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

### B36 — the published HIGH verdicts were produced by the buggy engine and need re-grinding

**What.** 174 of the 455 published HIGH verdicts that still have a crash log (**38%**) are false positives
under the classifier and dependency-staging fixes merged on 2026-08-29. HIGH is the only confidence that
reaches `/as-properties`, which SPC instances poll as their `fallback.updateurl` — so each false positive is
a working mod being stripped from real users' server packs right now.

Measured by fetching the live `/export.csv` (38 532 rows: 31 425 LOW, 5 481 MEDIUM, 1 160 INCONCLUSIVE,
**466 HIGH**), matching the 466 HIGH rows to the 609 published crash logs (455 matched), and re-classifying
every one of those logs with the fixed `BootLogClassifier`:

| | count |
|---|---:|
| still CRASHED | 281 |
| **now excused** | **174** |

Broken down — by loader: **Quilt 118**, Forge 52, NeoForge 4. By cause: dependency/version/mixin 148,
network denied 26. The Quilt concentration is the `pickForLoader` bug: a `+26.3` Fabric API staged into packs
as old as 1.19.2, so Quilt Loader refused the pack and the candidate wore the verdict.

**Why it waited.** It cannot be done from here. The deployed daemon still runs pre-branch code (its `/status`
carries no `bootRules`), so the fixes have to be **deployed first** — re-grinding before that just reproduces
the same verdicts. It is also hours-to-days of the host's time, which is Griefed's call to schedule, not
something to start unattended.

**Pick it up with.** After deploying the merge, requeue the published HIGH set — the lane exists for exactly
this ("a defect in the engine gets un-published"):

```
spc-grinder --requeue $(cat requeue-high-verdicts.txt | tr '\n' ' ')
```

`requeue-high-verdicts.txt` (428 distinct project URLs, uncommitted, generated 2026-08-29) is regenerable at
any time — it is just the distinct `Project` column of `/export.csv` where `Confidence` is HIGH. Prefer
regenerating over reusing the stale copy, since the set moves as the crawl runs.

`--requeue-before <ISO-8601 instant>` re-grinds everything verified before a point in time and is the blunter
option; it covers the LOW/MEDIUM rows too, but that is a full re-sweep of 38 k verdicts rather than the 466
that are actually published.

**Verify it worked** by re-running the audit above against the new store: the excused count should approach
zero, and `/as-properties` should shrink by roughly the number of entries that were false.

**Do not** treat a shrinking `/as-properties` as pure loss — those entries were wrong, and the two true
positives the audit re-confirmed (`arcane-vortex`, `avm-mod`) show the decisive marker still fires.
