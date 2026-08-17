# Backlog — deferred, agreed-to work

Items consciously deferred, with the reason and enough context to pick them up cold. Newest section first.
Not a wish-list: everything here was looked at, judged worth doing, and postponed for a stated reason.
When an item lands, delete it here and record it in `REFACTOR-LOG.md`.

## 2026-08-17 — startup / network performance (`claude-perf-network-startup`)

**B30 — `If-None-Match` for the Forge manifest.** After the conditional-GET work, four of twelve
manifests answer `304` to `If-Modified-Since` (Mojang 206,986 B, fabric-intermediaries 56,270 B,
fabric-loader 9,381 B, fabric-installer 2,516 B). `files.minecraftforge.net` **ignores**
`If-Modified-Since` but does honour `If-None-Match` against its weak nginx ETag — verified by hand,
`304/0` — which is another **121,492 B**, i.e. 57 % of the 213,885 B still transferred per startup.
Needs somewhere to persist an ETag per manifest (a sidecar file beside each one, or a property),
kept in sync with the manifest it describes; a stale pairing suppresses a real update.
*Waited because:* **measured, it buys ~0 ms of startup.** The twelve checks run concurrently, so
wall-clock is the slowest one, and that is LegacyFabric at ~330 ms for **498 bytes** — pure latency.
Forge finishes in ~234 ms, below the gate, so making it free does not move the batch. It only starts
to matter below roughly 4 Mbit/s, where 121,492 B (~972 kbit) overtakes the ~330 ms gate. One host
only: NeoForge and LegacyFabric publish no ETag at all, and Quilt's non-standard
`unverified:<sha>` value is not honoured (tested, still `200`). Adding persistent state to the code
path that was just simplified, for one host and no wall-clock gain, is the wrong ratio — but the
bandwidth is real on a metered connection, so it is deferred rather than rejected.

**B31 — take the manifest refresh off the blocking startup path.** This is the *larger* win B30 is
not. Measured after the conditional-GET work: the twelve checks still cost **~392 ms median**
(3 runs; 601 ms before conditional requests) of blocking startup, behind the splash screen at 20 %
— `ServerPackCreator.kt:228` → `ApiWrapper.stageTwo()` → `versionMeta` → `VersionMeta.init` →
`checkManifests()`. It is dominated by one slow, niche host: three LegacyFabric checks at ~330 /
~260 / ~249 ms for 15 KB combined. Since `ApiWrapper.setup()` already seeds every manifest from the
jar, SPC has working data *before* any request is made, so refreshing in the background would take
this off the startup path entirely — ~392 ms → ~0, and it makes the offline launch instant instead
of timeout-bound. *Waited because:* it changes `VersionMeta`'s construction contract. Today
callers may assume the metas hold refreshed data the moment the constructor returns; the grinder and
the web backend's version schedule both need checking against that before the guarantee is weakened.
Not a drive-by.

**B32 — `hasteBinPreChecks` reads a whole file to measure its length.**
`WebUtilities.kt:130-137` calls `fileToCheck.readText().length < 400_000` *after* `fileToCheck.size()`
is already known, materialising up to 10 MB as a String (≈20 MB of `char`) only to count it. Character
count is not byte count, so it is not strictly redundant — a streaming count, or a bound derived from
`size()`, gives the same answer without the allocation. *Waited because:* cold path. It runs only when
a user explicitly uploads a log to HasteBin, once, and the allocation is short-lived.

## 2026-08-04 — CI / dind (`claude-ci-qodana-jbr-cache`, `claude-ci-audit-fixes`)

> **Numbering continues from B25, it does not restart.** When this file was emptied, several items still referenced
> from the `CLAUDE.md` files were not carried over — **B25** (the shipped manifest snapshot lagging its own parent
> manifest, cited at `CLAUDE.md:63` and `serverpackcreator-api/CLAUDE.md:17`) and the **B4, B5, B11, B21, B22**
> named at `CLAUDE.md:254`. Those IDs are therefore in use even though the entries are gone; reusing them would
> break the citations. Reconstructing them is its own task and is not attempted here.

**B26 — remove the Docker-endpoint diagnostic from `Docker Test`.** `.gitlab-ci.yml`, `Docker Test`'s
`before_script` prints `DOCKER_HOST`, `DOCKER_TLS_CERTDIR`, `ls -l /var/run/docker.sock`, `docker context ls` and
`docker info --format …` before the registry logins. It is temporary by construction and exists only to answer B27.
*Waited because:* it has to run in CI once before it can be read. Griefed's 2026-08-04 pipeline was green, so the
output exists — read it off that `Docker Test` job, record the answer in B27, and delete the block. Every line ends
in `|| true`, so leaving it costs correctness nothing, only noise.

**B27 — decide whether `.dockerized` should exist at all.** The dind service **never starts**: `dockerd` dies on
`can't create unix socket /var/run/docker.sock: device or resource busy`, because something is already mounted at
that path in the service container. Yet the Docker jobs build and push, so they reach a daemon another way.
`358675fbf` narrowed the service from 20 jobs to 7 rather than deleting it, deliberately conservative. Read B26's
output: `DOCKER_HOST` unset + a live socket ⇒ the jobs use the **host** daemon and `.dockerized` can go entirely
(7 more jobs stop paying the 30 s health-check wait); `DOCKER_HOST=tcp://docker:2375|2376` ⇒ the service is the
intended endpoint and B28 is the real bug. *Waited because:* it depends on B26, and guessing wrong breaks the
release pipeline's push jobs.

**B28 — the dind socket collision itself.** Root cause is the runner's `config.toml` bind-mounting
`/var/run/docker.sock` into containers, which is **not in this repo** and is Griefed's to change. Either fix the
runner config (drop the mount, or give dind its own socket path / `DOCKER_TLS_CERTDIR`) or accept the service is
dead and close it via B27. *Waited because:* not actionable from the repository.

**B29 — record the first green 2026.2 Qodana problem count.** `CLAUDE.md`'s 2026-08-02 entry left it "unverified"
because the local run OOM-killed at 1.93 GiB. The 2026-08-04 pipeline is green, so the number is now readable off
the report; the 2025.1 baseline for comparison was 54 problems, 18 of them phantom `KotlinUnreachableCode`.

Add the next item under a dated section, with the reason it waited and enough context to pick it up cold.
