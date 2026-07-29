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

Each pass pulls the most-downloaded mods, grinds the ones that need it, then sleeps. A project is **skipped
while its verdict is fresh** and **re-verified once stale**, so evolving mods, new loader versions and newly
supported Minecraft releases get picked up over time. Verdicts are written after every result, so a restart
resumes instead of starting over.

For a real deployment, build a start script instead of using Gradle:

```bash
./gradlew :serverpackcreator-grinder:installDist
./serverpackcreator-grinder/build/install/serverpackcreator-grinder/bin/serverpackcreator-grinder
```

`Ctrl-C` / `SIGTERM` stops cleanly: it stops taking new candidates and removes any container still
mid-boot, so no Minecraft server is left running.

---

## 5. Configuration (environment variables)

| Variable                        | Default                        | Meaning                                                                      |
|---------------------------------|--------------------------------|------------------------------------------------------------------------------|
| `SPC_GRINDER_IMAGE`             | `spc-grinder-runtime:latest`   | Image used for the boots                                                     |
| `SPC_GRINDER_WORK`              | `~/.spc-grinder/work`          | Scratch space for generated packs                                            |
| `SPC_GRINDER_CACHE`             | `~/.spc-grinder/cache`         | Cached loader installs, one per loader/version/Minecraft                     |
| `SPC_GRINDER_STORE`             | `~/.spc-grinder/verdicts.json` | Verdict store — delete to start fresh                                        |
| `SPC_GRINDER_PORT`              | `8757`                         | Report server port                                                           |
| `SPC_GRINDER_WORKERS`           | `2`                            | Parallel boots. **Budget ~3 GB RAM each**                                    |
| `SPC_GRINDER_INTERVAL`          | `21600` (6 h)                  | Seconds between passes (continuous mode only)                                |
| `SPC_GRINDER_REVERIFY_TTL_DAYS` | `30`                           | How long a verdict stays fresh before re-verification                        |
| `SPC_GRINDER_MODRINTH_LIMIT`    | `25`                           | Modrinth projects pulled per pass                                            |
| `SPC_GRINDER_CF_LIMIT`          | `25`                           | CurseForge projects pulled per pass (needs the key)                          |
| `SPC_GRINDER_SPC_PROPERTIES`    | *(unset)*                      | Point SPC at a specific `serverpackcreator.properties` for reproducible runs |
| `CURSEFORGE_API_KEY`            | *(unset)*                      | Enables the CurseForge candidate source                                      |

```bash
export SPC_GRINDER_WORKERS=4
export SPC_GRINDER_INTERVAL=3600
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
Modrinth and CurseForge stays two separate projects.

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
Environment=SPC_GRINDER_INTERVAL=21600
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

Internals, design decisions and landmines live in [`CLAUDE.md`](CLAUDE.md) and [`module.md`](module.md).
