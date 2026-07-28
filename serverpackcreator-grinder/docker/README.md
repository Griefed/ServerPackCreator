# Grinder runtime image

The container image the grinder boots SPC-generated server packs in. One container per candidate mod,
isolated and resource-capped (see `ContainerServerRunner` / `ContainerSpec`).

## What it is (and isn't)

SPC's generated `start.sh` is **self-contained**: at first boot it installs the modloader server and
the Minecraft server itself. So this image is loader-agnostic — it only ships the **shell tooling**
(`bash`, `curl`/`wget`, `gawk`, `tar`/`gzip`, `ca-certificates`) and **JDKs**. No loader logic lives
here.

How each loader installs (all driven by `start.sh`, not by this image):

| Loader | Installer used by `start.sh` |
|---|---|
| Forge / NeoForge (1.17+) | **neoforged ServerStarterJar** (`server.jar`) |
| Forge (older) | `forge-installer.jar --installServer` |
| Fabric | `fabric-installer` / `fabric-server-launch(er).jar` |
| Quilt | `quilt-installer.jar` |
| LegacyFabric | LegacyFabric installer |

So the **ServerStarterJar is Forge/NeoForge only** — the other loaders never touch it.

## Java per Minecraft version

A single JDK can't boot every Minecraft version. The image bundles Temurin **8, 17, 21, 25** at stable
paths (`/opt/java-8|17|21|25`) and the grinder sets `$JAVA` (via the pack's `variables.txt`) to the right
one — resolved from SPC's own declared required-Java (`MinecraftMeta.requiredJavaVersion`), so **no Java
is ever downloaded**, which is what lets the actual mod-boots run under `--network none`:

| Minecraft | Java |
|---|---|
| ≤ 1.16.5 | 8 |
| 1.17 – 1.20.4 | 17 |
| 1.20.5 – 1.21.x | 21 |
| 26.2 (current release) | 25 |

The bundled set **must mirror `ImageJavaRuntimes.bundledMajors`** — a Minecraft version whose required
Java isn't bundled is skipped, never booted on the wrong JDK. Java 26 (snapshots only) is not bundled;
the release-gate skips snapshots.

## Build

```sh
docker build -t spc-grinder-runtime:latest serverpackcreator-grinder/docker
```

## Template-test image (`Dockerfile.templates`)

A **separate derived image** for the script-template matrix IT (`ScriptTemplateMatrixIT`), which boots
the generated `start.sh` / `start.fish` / `start.ps1` to prove the templates install + boot. It extends
the runtime image with `fish` (the new `.fish` templates) and PowerShell `pwsh` (the `.ps1` templates,
installed from the GitHub release tarball so it works on amd64 **and** arm64). Kept separate so the
production grind image stays bash-only and lean; the grinder never uses it. `.ps1` on Linux `pwsh`
validates script logic/Java-path/install flow, not Windows-cmdlet fidelity.

```sh
docker build -t spc-grinder-runtime:latest serverpackcreator-grinder/docker            # base first
docker build -t spc-grinder-templates:latest -f serverpackcreator-grinder/docker/Dockerfile.templates serverpackcreator-grinder/docker
```

Verified: `fish 3.6.0`, `pwsh 7.4.6`, JDK 8/17/21/25 + bash intact, non-root uid 1000, ~2.48 GB.

## Network: the `--network none` relationship

`start.sh` downloads the loader + Minecraft server + libraries **on first boot** (needs network). That
download happens **once per `(loader, loaderVersion, minecraftVersion)`** during the `LoaderCache`
pre-bake (a setup boot run *with* network), whose result is snapshotted. Every subsequent mod-boot
mounts the cached base and runs offline (`--network none`). Do **not** wire the candidate-mod boot to
run with network — that defeats the isolation (see `serverpackcreator-grinder/CLAUDE.md`).

## Known limitations (draft)

- **Size:** 4 bundled JDKs make this a large image (~2.08 GB, verified). A leaner variant could ship only JDK 21
  and let the pack's `install_java.sh` fetch others during pre-bake (more pre-bake network, larger
  cache) — a deliberate tradeoff, not done here.
- **uid alignment:** runs as `1000:1000`; the host-side bind-mounted pack dir must be writable by that
  uid (matches `ContainerServerRunner`'s `--user` default).
- **arch:** the Temurin install paths are resolved by glob, so amd64/arm64 both work; the bundled JDK
  set assumes one of those two.
