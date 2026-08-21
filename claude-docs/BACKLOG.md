# Backlog — deferred, agreed-to work

Items consciously deferred, with the reason and enough context to pick them up cold. Newest section first.
Not a wish-list: everything here was looked at, judged worth doing, and postponed for a stated reason.
When an item lands, delete it here and record it in `REFACTOR-LOG.md`.

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
