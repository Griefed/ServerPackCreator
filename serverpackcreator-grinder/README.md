# serverpackcreator-grinder

A **fire-and-forget service** that boot-verifies Minecraft mods at scale to find clientside-only ones, and
serves the results as a sortable web table with CSV export.

For each candidate mod it generates a server pack, boots it in an **isolated, network-less Docker
container**, and records a verdict. It reuses [`serverpackcreator-clientside`](../serverpackcreator-clientside/README.md)
for the actual verification — the grinder adds containers, parallelism, persistence and a report.

Why containers: a candidate mod is untrusted code. Every mod-boot runs with `--network none`, a read-only
root filesystem, all capabilities dropped, no-new-privileges, as non-root, under memory/CPU/PID caps.

---

## 1. Prerequisites

| Requirement          | Notes                                                                        |
|----------------------|------------------------------------------------------------------------------|
| Docker               | A running daemon the current user can talk to. Verified against Docker 29.x  |
| JDK 21+              | To build and run the service                                                 |
| Disk                 | The runtime image is ~2 GB; each cached loader install adds a few hundred MB |
| RAM                  | ~3 GB **per parallel worker** (each worker holds a booting Minecraft server) |
| `CURSEFORGE_API_KEY` | Optional. Without it the grinder uses Modrinth only                          |

---

## 2. Build the runtime image (once)

The container that boots the packs. It carries the shell tooling SPC's `start.sh` needs plus Temurin JDK
**8, 17, 21 and 25**, so a pack always boots on the Java its Minecraft version actually requires — with no
Java download, which is what keeps mod-boots runnable offline.

```bash
docker build -t spc-grinder-runtime:latest serverpackcreator-grinder/docker
```

Rebuild it whenever a new Minecraft release needs a newer JDK. See [`docker/README.md`](docker/README.md);
the bundled JDK set must stay in step with `ImageJavaRuntimes.bundledMajors`.

---

## 3. Grind a specific mod (one-shot)

Best first run: it proves your Docker setup end-to-end in a few minutes.

```bash
./gradlew :serverpackcreator-grinder:run --args="https://modrinth.com/mod/modmenu"
```

Pass any number of project URLs. The grinder resolves each, boots it per modloader, records verdicts, then
**keeps the report server up** until you Ctrl-C.

---

## 4. Run it as a service (continuous)

With **no arguments** it enters fire-and-forget mode:

```bash
./gradlew :serverpackcreator-grinder:run
```

Each pass takes the **next slice** of every platform's catalog — most-downloaded first — and grinds the
projects that need it. The crawl position is saved per platform (`SPC_GRINDER_CURSORS`), so passes keep
working *forward* through the catalog and a restart resumes mid-catalog instead of starting over at the
popular mods. When a platform runs out, the crawl wraps to the top and starts a new sweep.

A project is **skipped while its verdict is fresh** and **re-verified once stale**
(`SPC_GRINDER_REVERIFY_TTL_DAYS`), so a completed sweep keeps the catalog current — evolving mods, new loader
versions and newly supported Minecraft releases get picked up — while an unfinished one keeps extending
coverage. Verdicts are written after every result, so a restart never redoes finished work.

**The loop waits only when there is nothing to get on with:** it moves straight to the next slice while
projects keep needing verification, pauses `SPC_GRINDER_SCAN_DELAY` while merely scanning past fresh ones,
and idles `SPC_GRINDER_INTERVAL` once a full sweep found nothing due. So the service goes as fast as your
host allows and settles down once it is caught up. Leave it running and check the report whenever.

### How long is "eventually"?

Sweep time is `catalog ÷ batch × pass duration`, and a pass is dominated by real container boots (minutes
each, `SPC_GRINDER_WORKERS` in parallel). Modrinth carries ~71 000 mod projects, so with `SPC_GRINDER_BATCH`
at the default 25 that is ~2 850 passes for one sweep — plan on raising the batch, the workers, or both if
you want a full sweep in weeks rather than months, and give it a `SPC_GRINDER_REVERIFY_TTL_DAYS` longer than
a sweep takes (otherwise verdicts go stale faster than the crawl advances and it never reaches the tail).

**CurseForge is crawled in partitions**, because its `/mods/search` refuses an `index` beyond 10 000 — one
query can only ever show you the 10 000 most-downloaded mods. So a sweep walks a sequence of bounded queries:

1. the unfiltered catalog (its top 10 000 by downloads);
2. then every Minecraft version, newest first — **135 of them** (the API's version list also carries 7 200
   modloader and non-Minecraft version strings, which are filtered out). A version reporting fewer than
   10 000 mods is done in one slice;
3. a version at the cap is re-crawled **per modloader** and **per category** — both, because CurseForge tags a
   mod with a loader only if it has one, and its own submission docs disagree on whether a category is
   mandatory, so running both axes means a mod is reachable if it has *either*;
4. any slice still at the cap is crawled from both ends (`sortOrder` desc *and* asc, reaching 20 000), and a
   category slice still capped after both is narrowed by modloader as a last resort.

A CurseForge sweep is therefore much longer than a Modrinth one — 135 versions, plus ~58 more requests for
each version big enough to need splitting, plus paging. Mods supporting many versions turn up in several
partitions, which costs nothing but a store lookup because fresh verdicts are skipped.

One residual gap remains, and it is **logged with a count** rather than left to look like completeness: a
single (version, category, modloader) slice holding more than 20 000 mods loses its middle. That is the
narrowest slice this API can express, so covering it would need a filter CurseForge does not offer. A mod
carrying *neither* a loader tag nor a category is likewise only reachable while its version fits under
10 000 — that one cannot be detected from outside, so it cannot be logged.

> **Verified against the live API on 2026-07-30**, which is worth knowing because the design was originally
> built from CurseForge's documentation alone and the documentation was not enough. The real API showed that
> `pagination.totalCount` **saturates at 10 000** (a version holding 200 000 mods reports the same number as one
> holding exactly 10 000), so the split conditions had to key off saturation rather than a size comparison —
> as written from the docs they could never have fired, and the crawl would silently have covered only the top
> 10 000 of each version. The version list, the modloader filter (Forge 1 … NeoForge 6), the category list, the
> paging cap and the crawl's own advance-and-resume behaviour are all now pinned by a gated live test.
>
> Still unproven: a *full* sweep has never run — that is weeks of work and a large slice of an API key's quota.

Modrinth needs no partitioning: its offset is usable to 99 999, comfortably past today's ~71 000 mod projects.

For a real deployment, build a start script instead of using Gradle:

```bash
./gradlew :serverpackcreator-grinder:installDist
./serverpackcreator-grinder/build/install/serverpackcreator-grinder/bin/serverpackcreator-grinder
```

`Ctrl-C` / `SIGTERM` stops cleanly: it stops taking new candidates and removes any container still
mid-boot, so no Minecraft server is left running.

**Disk:** each cached loader install — one per `(Minecraft, loader, loader-version)` — is ~150 MB, and loaders
keep shipping builds, so the cache would grow without limit over a long sweep. After every pass the grinder
deletes cached installs nothing has booted for `SPC_GRINDER_CACHE_TTL_DAYS` (default 7). Retention is measured
from **last use**, not from install time: a tuple the sweep still boots is stamped fresh on every cache hit and
never evicted, and a re-install costs one networked setup boot if it comes back. Set `0` to keep everything.

---

## 5. Configuration (environment variables)

| Variable                        | Default                        | Meaning                                                                      |
|---------------------------------|--------------------------------|------------------------------------------------------------------------------|
| `SPC_GRINDER_IMAGE`             | `spc-grinder-runtime:latest`   | Image used for the boots                                                     |
| `SPC_GRINDER_WORK`              | `~/.spc-grinder/work`          | Scratch space for generated packs                                            |
| `SPC_GRINDER_CACHE`             | `~/.spc-grinder/cache`         | Cached loader installs, one per loader/version/Minecraft                     |
| `SPC_GRINDER_STORE`             | `~/.spc-grinder/verdicts.json` | Verdict store — delete to start fresh                                        |
| `SPC_GRINDER_CURSORS`           | `~/.spc-grinder/cursors.json`  | Crawl position per platform — delete to re-sweep from the most-downloaded    |
| `SPC_GRINDER_PORT`              | `8757`                         | Report server port                                                           |
| `SPC_GRINDER_WORKERS`           | `2`                            | Parallel boots. **Budget ~3 GB RAM each**                                    |
| `SPC_GRINDER_BATCH`             | `25`                           | Projects taken from **each** platform per pass — the sweep-speed lever       |
| `SPC_GRINDER_INTERVAL`          | `21600` (6 h)                  | Seconds to idle after a full sweep found nothing due                         |
| `SPC_GRINDER_SCAN_DELAY`        | `15`                           | Seconds between passes that only scanned past fresh verdicts                 |
| `SPC_GRINDER_REVERIFY_TTL_DAYS` | `30`                           | How long a verdict stays fresh before re-verification                        |
| `SPC_GRINDER_CACHE_TTL_DAYS`    | `7`                            | Delete cached loader installs unused this long (~150 MB each). `0` = never   |
| `SPC_GRINDER_SPC_PROPERTIES`    | *(unset)*                      | Point SPC at a specific `serverpackcreator.properties` for reproducible runs |
| `CURSEFORGE_API_KEY`            | *(unset)*                      | Enables the CurseForge candidate source                                      |

```bash
export SPC_GRINDER_WORKERS=4
export SPC_GRINDER_BATCH=100
export SPC_GRINDER_PORT=8757
./gradlew :serverpackcreator-grinder:run
```

---

## 6. Read the results

While the service runs:

- **Table:** `http://localhost:8757/` — sortable by any column (name, project, name pattern, confidence, loader)
- **CSV:** `http://localhost:8757/export.csv`

Columns are `Name, Project, NamePattern, Confidence, Loader, Detail`, highest confidence first.

**Interpreting confidence:** only `HIGH` (the server crashed with the mod in place) is decisive. `MEDIUM`
means the server booted — which does *not* prove the mod is server-safe. `INCONCLUSIVE` means nothing was
learned, e.g. the loader has no build for that Minecraft version, so the mod was never actually tested.

The store is plain JSON (`SPC_GRINDER_STORE`), keyed by platform + slug + loader — the same slug on
Modrinth and CurseForge stays two separate projects. How far the crawl has got is in `SPC_GRINDER_CURSORS`:
one entry per platform with the next `offset`, the number of completed `sweeps`, and — for CurseForge — the
`partition` being walked (`gameVersion|modLoaderType|direction`, `*` meaning "no filter"). Read it to tell
"still on the first pass over this platform" from "covered it, now keeping it current".

---

## 7. Running as a systemd service

```ini
[Unit]
Description=ServerPackCreator Grinder
After=docker.service
Requires=docker.service

[Service]
User=grinder
Environment=SPC_GRINDER_WORKERS=4
Environment=SPC_GRINDER_BATCH=100
Environment=SPC_GRINDER_REVERIFY_TTL_DAYS=180
# Environment=CURSEFORGE_API_KEY=...
ExecStart=/opt/spc-grinder/bin/serverpackcreator-grinder
Restart=on-failure
TimeoutStopSec=120

[Install]
WantedBy=multi-user.target
```

Give `TimeoutStopSec` room: on stop the grinder removes in-flight containers before exiting.

---

## 8. Troubleshooting

| Symptom                                  | Cause & fix                                                                                                                   |
|------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------|
| `Cannot connect to the Docker daemon`    | Daemon not running, or your user isn't in the `docker` group                                                                  |
| `No cached loader install for …`         | The one-off install boot failed — it is the only boot allowed network. Check connectivity and the logs above it               |
| Mods on the newest Minecraft are skipped | The image lacks that version's required JDK. Add it to the Dockerfile **and** `ImageJavaRuntimes.bundledMajors`, then rebuild |
| Boots die with `Killed` mid-startup      | Host out of memory — lower `SPC_GRINDER_WORKERS`                                                                              |
| Everything is `INCONCLUSIVE`             | Often the loader genuinely has no build for the selected Minecraft version; check the `Detail` column                         |
| Nothing gets ground on a second run      | Working as intended: verdicts are still fresh. Lower `SPC_GRINDER_REVERIFY_TTL_DAYS` or delete the store                      |
| Passes run but verify nothing for a while | Also expected: the crawl is scanning past projects whose verdicts are fresh, one batch per `SPC_GRINDER_SCAN_DELAY`          |
| It re-grinds popular mods, never the tail | The crawl position was lost (deleted/unwritable `SPC_GRINDER_CURSORS`) or the TTL is shorter than a sweep takes — raise it    |
| `game-version list unavailable`          | CurseForge's `/games/432/versions` failed, so that sweep covers only the unfiltered top 10 000. Check the key and connectivity |
| `category list unavailable`               | CurseForge's `/categories` failed, so an over-cap version is covered by its modloader slices only that sweep                   |
| `holds N mods but only 20000 are reachable` | Even a version × category × modloader slice is too big to page through; its middle is skipped. No further filter exists      |
| A container outlived the process         | Should not happen — shutdown drains them. If it does, `docker ps` and remove it, and please report it                         |

---

## 9. Testing the start-script templates (maintainers)

The module also hosts the harness that boots SPC's generated `start.sh` / `start.fish` across Minecraft
versions and loaders, plus a PowerShell parse/behaviour check. It needs a second image:

```bash
docker build -t spc-grinder-templates:latest \
  -f serverpackcreator-grinder/docker/Dockerfile.templates serverpackcreator-grinder/docker

GRINDER_TEMPLATE_IT=1 ./gradlew :serverpackcreator-grinder:test --tests "*ScriptTemplateMatrixIT"
```

It is slow and network-heavy, and gated behind `GRINDER_TEMPLATE_IT=1` so normal runs skip it. Keep
`SPC_GRINDER_TEMPLATE_WORKERS` at **1** — parallel cells starve the host and produce false failures. The
docker-glue integration test is gated separately behind `GRINDER_DOCKER_IT=1`.

The catalog crawl has its own live-API check — no containers, a handful of search calls — which pins that
Modrinth really serves the offsets the crawl walks (including a deep one) and that the position survives a
restart. Run it after touching paging or the cursor:

```bash
GRINDER_LIVE_IT=1 ./gradlew :serverpackcreator-grinder:test --tests "*CatalogCrawlLiveIT"
```

Internals, design decisions and landmines live in [`CLAUDE.md`](CLAUDE.md) and [`module.md`](module.md).
