<!-- Loads when Claude works with files in this package. Module-wide context (package map, the
cross-cutting landmines, remaining work) lives in serverpackcreator-grinder/CLAUDE.md. -->

# grinder.source — candidate discovery and the crawl position

What gets ground, in what order, and how the position survives restarts. The CurseForge landmines below
were each *measured* against the live API after the documentation proved insufficient.

- **Candidate sources** (`CandidateSource` — `platform` + `page(offset, limit, partition): CandidatePage`,
  most-downloaded first): `ModrinthCandidateSource` (keyless Modrinth search, one offset sequence, ignores
  `partition`) and `CurseForgeCandidateSource` (CF `/mods/search` sorted by `sortField=6` TotalDownloads,
  `x-api-key`, `index`/`pageSize≤50`, `index+pageSize≤10000`; project link = `links.websiteUrl`; **crawled in
  partitions**, see below). Both paginate behind the clientside `HttpFetcher` seam (unit-tested with canned
  JSON). `GrinderApplication` wires Modrinth always and CurseForge **only when `CURSEFORGE_API_KEY` is set**;
  `GrindPool` orders each batch round-robin across platforms (see below). Store dedup is by `slug`, so a mod
  on both platforms is treated as one project (accepted for now).
- **CurseForge partitioned crawl** (`CurseForgePartitions`, pure + unit-tested — the *only* place that decides
  what CF gets crawled): one query can never expose more than 10 000 mods (`index+pageSize` cap), so a sweep
  walks a **sequence** of bounded queries: (1) the unfiltered catalog, (2) each game version newest-first from
  `/games/{gameId}/versions`, (3) a version whose `pagination.totalCount` exceeds the cap re-crawled **per
  modloader and then per category** (`/categories?classId=6`) — *this* is what reaches past 10 000 — (4) any
  slice still over the cap also crawled `sortOrder=asc` (bottom 10 000 ⇒ ≤20 000 covered), and a *category*
  slice past even that narrowed by modloader (version × category × loader, the deepest the API allows).
  **Both axes on purpose, not redundancy:** a mod carries a loader tag only if it has one, and CF's own docs
  disagree on whether a category is mandatory (submission guide says the main category is required; the
  project-creation page lists only the class) — so neither axis is provably total, and running both means a mod
  is reachable if it has *either*. ~6 extra requests per over-cap version buys removal of a silent hole.
  Splitting only where a count demands it keeps a sweep at ~1 request per version; `totalCount` rides along on
  every response, so sizing is free (the lone probe case is a slice resuming exactly at the cap). All six
  documented loaders are crawled incl. legacy Cauldron/LiteLoader, and **every** category incl. children —
  measured live: a parent category does **not** reliably include its children (3 of 6 sampled child-category
  mods were invisible under their parent), so crawling parents only would lose them.
  **LANDMINE #1 — `pagination.totalCount` SATURATES at `MAX_INDEX`** (measured 2026-07-30: the whole catalog,
  `gameVersion=1.12.2` and a 200 000-mod slice all report exactly `10 000`; only a slice genuinely below the cap
  reports its true size). Therefore **every split condition is `>= CAP`, never `> CAP`** — the `> CAP` /
  `> 2 × CAP` rules written from the docs could never fire, which made the entire partition plan inert (it would
  have crawled the top 10 000 of each version and nothing else) and left `warnIfSliceIsUnreachable` dead too.
  Do not "tidy" these back into size comparisons.
  **LANDMINE #2 — the version axis must be filtered by version *type*.** `/games/432/versions` returns **7 339**
  strings across 36 types, including Forge version families (`47.0.42`) and types named `Server Side`,
  `Shader Loader`, `Addons`, `DO NOT USE - Grouped MC Versions`. Keeping only types whose name starts with
  `Minecraft ` (via `/games/432/version-types`) leaves **135** real versions — a 54× smaller axis. Unfiltered,
  a sweep would burn 7 200 requests on partitions that can hold no mods.
  **LANDMINE #3 — sort order is approximate.** `desc` only *trends* by downloads (a live 10-mod page had one
  adjacent inversion: 385 316 073 before 386 940 279), and `asc` is not ordered at all though it does reach the
  tail (counts in the hundreds). `warnIfMisordered` therefore checks the descending **trend** (first vs last)
  and skips ascending entirely; checking adjacent pairs would warn on ordinary pages.
  **Axis lists refresh at sweep start *and whenever missing*** — the second condition is not an optimisation
  but a **restart-correctness fix**: a resumed cursor arrives with a partition token and an empty in-memory
  list, and without re-reading it the plan finds no next partition, reports the catalog finished and **throws
  the resumed position away** (found by `theLoaderStageHandsOverToTheCategoryStageWithinOneSlice`, pinned by
  `resumingMidSweepFetchesTheAxisListsItHasNotGotYet`). A failed version fetch degrades to the unfiltered top
  10 000; a failed category fetch leaves the loader stage working.
  **Residual gap (logged with a count, not hidden):** a (version, category, loader) slice >20 000 loses its
  middle — no narrower filter exists. A mod with *neither* a loader nor a category is unreachable beyond its
  version's cap and cannot be detected from outside. **Landmine:** `warnIfMisordered` must follow the
  partition's direction — an ascending slice is *supposed* to come back least-downloaded first, so the old
  descending-only check would have cried wolf on every bottom-up slice.
  **`CandidatePage.endOfCatalog` is load-bearing, don't collapse it:** it is `true` only when the platform
  genuinely ran out — for a partitioned source, when the *last* partition ran out — never on a failed request
  or a failed size probe. The crawler wraps to the start of the plan on it, so treating a transient 503 as
  "the end" would silently reset a deep crawl to the popular head.
  **The partition token is opaque outside its source.** `CatalogCursor.partition` / `CandidatePage.nextPartition`
  are carried and persisted verbatim by the crawler; only `CurseForgeCandidateSource` parses them
  (`CurseForgePartition.parse`, which falls back to the start of the sweep for anything unreadable, so an old
  or hand-edited `cursors.json` can't crash the daemon). Don't teach the crawler what a partition means.

- **Catalog crawl (the coverage mechanism)**: `CatalogCrawler.nextBatch()` resumes each source at its
  persisted `CatalogCursor` and on `endOfCatalog` wraps to the
  top and counts a sweep (the verdict TTL then decides what the new sweep re-grinds). A failed page keeps its
  position (retried next pass); a *throwing* source is skipped, not fatal. A position already past the end
  wraps **and** takes the head slice in the same pass — guarded by `cursor.offset > 0 || partition != null` so
  an empty catalog can't spin. `JsonCursorStore` persists offset+sweeps+partition per platform
  (temp-then-atomic-move, corrupt → start of catalog) — **this file is the difference between eventual full coverage and re-checking the top N
  forever**; deleting it costs one re-sweep (fresh verdicts are skipped), not correctness.

- **Two-phase commit — the crawl advances on work done, not on hand-out.** `nextBatch()` moves nothing;
  `commit(batch, reached)` advances each source past the pages whose candidates were **all** reached, and stops
  that source at the first page that was not (leaving its cursor where that page began, so the whole page is
  re-handed). `GrindPool.grindAll` returns a `GrindPass(reached, verified)` for exactly this: *reached* means
  `grind` returned for it — verified, skipped-as-fresh, **or attempted and failed** (all three are done with;
  otherwise one poison candidate would stall the sweep forever).
  **Why:** the cursor used to advance at hand-out, so restarting the daemon mid-pass skipped whatever the
  abandoned batch never ground — invisible, and not revisited until the next full sweep (weeks at real
  throughput). Observed live on 2026-07-30 after two restarts.
  **Granularity is per page, deliberately:** a partitioned source can cross partitions *inside* one page, so a
  candidate's exact catalog position isn't recoverable from outside. Re-handing a whole page is nearly free —
  everything already ground in it has a fresh verdict and is skipped in microseconds. A sweep is likewise only
  counted when the page that ended the catalog was itself fully ground.

- **Cross-platform ordering is round-robin, never a global popularity sort** (`GrindPool.interleaveByPlatform`).
  The counts are not comparable: CurseForge counts file downloads across every version, Modrinth counts
  differently, so `jei` at 602 M vs `fabric-api` at 218 M says nothing about relative importance. Sorting the
  union by `popularity` therefore put **every** CurseForge candidate ahead of **every** Modrinth one — measured
  live on 2026-07-30: a two-hour pass produced 108 CurseForge verdicts and **zero** Modrinth ones, and any
  interruption shorter than a full pass meant Modrinth never progressed at all. Within a platform the ranking is
  kept, because there it is meaningful. This is *fairness*, a separate concern from the two-phase commit above,
  which is *correctness* — the commit stops un-ground candidates being skipped, but on its own would have left
  Modrinth starved forever.
