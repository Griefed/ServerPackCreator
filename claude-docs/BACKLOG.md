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
*Waited because:* it has to run in CI once before it can be read. Every line ends in `|| true`, so leaving it costs
correctness nothing, only noise.
*Updated 2026-08-21:* the block now also prints `CI_RUNNER_ID` / `_DESCRIPTION` / `_TAGS`, because without them one
pipeline's output cannot answer B27 — see the runner note there. **One more pipeline is needed before it can be
deleted**, and what to capture is the whole `**** Docker endpoint in use ****` block, not just `DOCKER_HOST`.
Attempted and failed from here: `git.griefed.de`'s API answers `404` unauthenticated, and there is no `glab`, `gh`
or token on this machine, so the 2026-08-04 output cannot be read from the repository.

**B27 — decide whether `.dockerized` should exist at all.** The dind service **never starts**: `dockerd` dies on
`can't create unix socket /var/run/docker.sock: device or resource busy`, because something is already mounted at
that path in the service container. Yet the Docker jobs build and push, so they reach a daemon another way.
`358675fbf` narrowed the service from 20 jobs to 7 rather than deleting it, deliberately conservative. Read B26's
output: `DOCKER_HOST` unset + a live socket ⇒ the jobs use the **host** daemon and `.dockerized` can go entirely
(7 more jobs stop paying the 30 s health-check wait); `DOCKER_HOST=tcp://docker:2375|2376` ⇒ the service is the
intended endpoint and B28 is the real bug.

*Verified locally 2026-08-21, which narrows this but does not close it:*
- **`.gitlab-ci.yml` sets neither `DOCKER_HOST` nor `DOCKER_TLS_CERTDIR` anywhere** — the only mentions in the whole
  file are the diagnostic echoing them. The CLI therefore uses its default `unix:///var/run/docker.sock`, so the
  `tcp://docker:2375|2376` branch above is **excluded by configuration**: nothing points at the service.
- **The `docker` alias the service publishes is never used as an endpoint** by any job.
- So on the runner that produced the green 2026-08-04 pipeline, the jobs reach a daemon at the default socket path
  that the *service* did not create — it died trying. Removing a service nothing connects to cannot remove that
  daemon.

*Still waited because of one thing, and it is not the endpoint question:* **this file declares no `tags:`**, so
nothing pins these jobs to a particular runner. The reasoning above holds for the runner that ran that pipeline; if
Griefed has more than one and any lacks the `/var/run/docker.sock` mount, dind is the only daemon *there* and
dropping `.dockerized` breaks the release pipeline on that runner. B26 now prints the runner identity so one
pipeline settles it. Seven jobs still extend `.dockerized`: `Docker Test`, `Build Docker Release`,
`Build Docker PreRelease`, `Writerside Docker`, `Writerside Docker Latest`, `Writerside Docker Prerelease`,
`Update README:on-schedule`.

**B28 — the dind socket collision itself.** Root cause is the runner's `config.toml` bind-mounting
`/var/run/docker.sock` into containers, which is **not in this repo** and is Griefed's to change. Either fix the
runner config (drop the mount, or give dind its own socket path / `DOCKER_TLS_CERTDIR`) or accept the service is
dead and close it via B27. *Waited because:* not actionable from the repository. *Note 2026-08-21:* if the runner
config is the first thing you reach for, the useful question is whether **every** runner that accepts these untagged
jobs carries that mount — the same fact B27 is blocked on, which B26 now prints.

**B29 — record the first green 2026.2 Qodana problem count.** `CLAUDE.md`'s 2026-08-02 entry left it "unverified"
because the local run OOM-killed at 1.93 GiB. The 2026-08-04 pipeline is green, so the number is now readable off
the report; the 2025.1 baseline for comparison was 54 problems, 18 of them phantom `KotlinUnreachableCode`.
*Checked 2026-08-21:* nothing committed carries the count — the repository has `qodana.yaml`, no SARIF and no
baseline — so it exists **only** in that job's artefact. The job runs `jetbrains/qodana-jvm-community:2026.2`
(`.gitlab-ci.yml`, `Qodana`), and reading its artefact needs pipeline access this machine does not have.

Add the next item under a dated section, with the reason it waited and enough context to pick it up cold.
