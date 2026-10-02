# Runner host — registry caching

Operator-facing companion to `.forgejo/workflows/` and to the *ghcr's 429 is a burst limiter on the
host* section of `.claude/rules/ci-workflows.md`. That section explains why the retry landed; this file
explains how to stop needing it.

**Nothing here is a repository change.** Everything in "Option 1" onwards is configuration on the
machine that runs `forgejo-runner`, plus one workflow edit that must not land until the host side is up.

---

## The problem, measured

`docker-test.yml` has lost three of 57 completed runs to a ghcr `429` — 676 and 680 on 2026-09-27,
739 on 2026-09-28. Every one of them carried the same two numbers:

```
toomanyrequests: retry-after: 933.17µs, allowed: 44000/minute
```

An allowance of 44,000/minute is not something one build can exhaust, and a retry window under a
millisecond is not a quota — it is a **token bucket keyed on the requesting host**. Run 739 proves an
account-scoped credential does not lift it: that job logged `Login Succeeded`, buildkit emitted its
`[auth] … token for ghcr.io` vertex, and the blob `GET` still came back 429.

So the lever is *requests per minute leaving this host*, and the fix is to stop making most of them.

## Who actually pulls, and from where

Four consumers, and they do **not** share a cache:

| Consumer | Pulls | Configured by |
|---|---|---|
| The runner's daemon | `ghcr.io/catthehacker/ubuntu:runner-latest` (job container), service images | `forgejo-runner` `config.yaml`, `/etc/docker/daemon.json` |
| The daemon, for tests | `busybox`, `alpine`, `mcr.microsoft.com/powershell` (`DockerJavaContainerEngineIT`, grinder ITs) | `/etc/docker/daemon.json` |
| BuildKit, in the buildx builder container | `ghcr.io/linuxserver/baseimage-ubuntu:noble`, `docker.io/library/eclipse-temurin:21-jdk-jammy` | `buildkitd.toml`, via `setup-buildx-action` |
| The grinder | its own server images | the grinder's own engine config |

**Only the third one has ever failed**, and it is also the only one that can be mirrored per-registry.

## The constraint that decides the shape of every option

**Docker Engine's `registry-mirrors` mirrors Docker Hub and nothing else.** Point it at a cache for
`ghcr.io` and the daemon silently ignores it — upstream says *"it's currently not possible to mirror
another private registry; only the central Hub can be mirrored"*, and a pull of
`ghcr.io/anything` never consults the mirror list.

**BuildKit has no such limit.** `[registry."ghcr.io"] mirrors = […]` works, and so does a mirror with a
path (`harbor.example/proxy.ghcr.io`), which the daemon also rejects.

**Fallback is built in, and that is verified from source rather than from the docs** — the buildkitd
reference does not mention it. `NewRegistryConfig` in `util/resolver/resolver.go` builds the host list by
appending each mirror first and then **appending the upstream host last**, and containerd's resolver
walks that list in order. A cache that is down or 404s costs a failed attempt, not a failed build. The
mirror entries also carry `HostCapabilityPull | HostCapabilityResolve` only — **pushes always go
upstream**, so `release-build.yml`'s push to ghcr and Docker Hub is untouched by any of this.

---

## Option 0 — a persistent builder (cheapest, repo-side, no host services)

`docker/setup-buildx-action` creates a *fresh* builder per job and deletes it at the end, so every
docker-test run pulls both base images from scratch. On a persistent self-hosted runner that is pure
waste, and the action has inputs for exactly this:

```yaml
      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@e468171a9de216ec08956ac3ada2f0791b6bd435 # v3.11.1
        with:
          name: spc-builder     # a fixed name is reused when it already exists
          keep-state: true      # "only useful on persistent self-hosted runners" -- its own description
          cleanup: false
```

What it buys: the ~160 MB of `eclipse-temurin` layers and the ghcr base image stay in the builder's
content store, so the **blob** fetches that killed runs 680 and 739 stop happening entirely. It also
removes the 1m41s `Set up Docker Buildx` step from every run.

What it does **not** buy: buildkit still resolves `:noble` and `:21-jdk-jammy` to a digest on every
build, which is one registry request per `FROM` — and a manifest resolve is exactly what killed run
**676**. So this shrinks the exposure, it does not close it.

Costs to accept before taking it:
- **Unbounded disk.** Nothing prunes a builder that is never deleted. Pair it with a timer running
  `docker buildx prune --builder spc-builder --keep-storage 20GB -f`, or a `[worker.oci] gc = true`
  block in `buildkitd-config-inline`.
- **A wedged builder persists.** Recovery is `docker buildx rm spc-builder` on the host.
- **Two jobs share it.** `concurrency` is keyed per-ref, so a push to `develop` runs the same commit
  twice in parallel; buildkit serialises concurrent solves safely, and they would now share cache
  rather than duplicate the pull. That is a win, but it is the kind of claim that needs one real run
  to confirm rather than one that can be reasoned into.

---

## Option 1 — pull-through caches on the host (recommended)

CNCF Distribution proxies **one upstream per instance** (*"it's currently possible to mirror only one
upstream registry at a time"*), so two upstreams means two containers. That is fine; they are tiny.

### 0. The topology, because it decides everything below

**Jobs do not talk to the host's Docker daemon.** The runner's `docker_host` is
`unix:///sockets/docker.sock`, and that socket comes from a `docker:dind` service over a shared
volume — which makes it *look* like a host socket mount and is not one:

```yaml
  docker-in-docker:
    image: docker:dind
    container_name: 'docker_dind'
    privileged: 'true'
    volumes: [ dind-storage:/var/lib/docker, dind-socket:/sockets ]
    command: ['dockerd', '-H', 'unix:///sockets/docker.sock', '--tls=false', '-G', '1001', ...]
```

| container | created by | sees |
|---|---|---|
| `docker_dind`, the runner, the caches | the **host** daemon | the host's networks |
| every job container, every buildx builder, every container a test starts | the **dind** daemon | dind's own bridge |

So `docker network inspect runners_default` from a job asks dind, which has never heard of it — the
notice that started this was right about the network being absent and wrong about whose. **Service
names cannot work across that boundary; routing can.** Both halves measured 2026-10-02 against a
reproduction of this exact shape (`docker:dind` on a user-defined network, a `registry:3` beside it):

```
from a container created by the dind daemon:
  wget http://registry-cache-ghcr:5000/v2/   ->  bad address          (DNS: no)
  wget http://172.31.77.6:5000/v2/           ->  200 {}               (routing: yes)
```

That is the whole argument for addressing the caches by **fixed IP**.

### 1. The caches, on their own network with pinned addresses

Pick a subnet nothing else on the host uses — check first, because a collision is a confusing outage:

```
docker network inspect $(docker network ls -q) \
  --format '{{.Name}} {{range .IPAM.Config}}{{.Subnet}}{{end}}'
```

Then add to the **runner's own compose file**, so the caches and dind come up together:

```yaml
networks:
  registry-cache:
    name: registry-cache
    ipam:
      config:
        - subnet: 172.31.77.0/24

volumes:
  ghcr-data:
  dockerhub-data:

services:
  docker-in-docker:
    # ... everything already there, plus:
    networks:
      - default            # LANDMINE: naming any network drops the implicit `default`, and
      - registry-cache     # dind off `runners_default` is a runner that cannot reach its daemon.
    command: ['dockerd', '-H', 'unix:///sockets/docker.sock', '--tls=false', '-G', '1001',
              '--registry-mirror', 'http://172.31.77.5:5000',
              '--dns', '127.0.0.1', '--dns', '185.12.64.2', '--dns', '185.12.64.1']

  registry-cache-dockerhub:
    image: registry:3
    container_name: registry-cache-dockerhub
    restart: unless-stopped
    environment:
      REGISTRY_PROXY_REMOTEURL: "https://registry-1.docker.io"
      REGISTRY_PROXY_TTL: "168h"
      # Optional: an authenticated upstream turns Docker Hub's anonymous 100-pulls/6h into the
      # account's allowance. The cache then serves whatever that account can see, so keep it on this
      # network and unpublished.
      # REGISTRY_PROXY_USERNAME: "…"
      # REGISTRY_PROXY_PASSWORD: "…"
    volumes: [ dockerhub-data:/var/lib/registry ]
    networks:
      registry-cache:
        ipv4_address: 172.31.77.5

  registry-cache-ghcr:
    image: registry:3
    container_name: registry-cache-ghcr
    restart: unless-stopped
    environment:
      REGISTRY_PROXY_REMOTEURL: "https://ghcr.io"
      REGISTRY_PROXY_TTL: "168h"
    volumes: [ ghcr-data:/var/lib/registry ]
    networks:
      registry-cache:
        ipv4_address: 172.31.77.6
```

**No published ports.** Nothing needs them, and an open proxy cache is not something to publish. This
also sidesteps the `INPUT policy DROP` trap that cost this project a 131-second outage diagnosed three
wrong ways (see *the symptom shared by two layers* in `CLAUDE.md`): there is no host-published port to
dial, so the host's filter table never enters the picture.

**`docker compose up -d` will recreate `docker_dind`**, because its networks and command change. That
kills whatever jobs are in flight; do it when the runner is idle. `dind-storage` is a named volume, so
dind's image cache survives the restart.

### 2. Attaching dind to the cache network is load-bearing — verified by removing it

Without it, a job's packets leave dind through its `runners_default` gateway and Docker's
`DOCKER-ISOLATION-STAGE` chains drop them between bridges. The failure is a **timeout**, not a refusal,
which is the shape that reads as "the cache does not work":

```
dind attached to the cache network:    wget http://172.31.77.5:5000/v2/  ->  200
dind detached (nothing else changed):  wget http://172.31.77.5:5000/v2/  ->  download timed out
```

So if the caches ever appear dead, check `docker network inspect registry-cache` for `docker_dind`
before suspecting the registries.

### 3. What this covers, and what it does not

`--registry-mirror` on dind's `dockerd` is **Docker Hub only** — not an oversight, a Docker Engine
limitation: *"it's currently not possible to mirror another private registry; only the central Hub can
be mirrored"*, and a pull of `ghcr.io/anything` never consults the mirror list. BuildKit has no such
limit, which is why ghcr is handled there instead.

| puller | what it pulls | cached by | covered |
|---|---|---|---|
| dind's daemon | `busybox`, `alpine` (the container ITs, `test.yml`'s pre-pull) | `--registry-mirror` | **yes** |
| BuildKit, in the builder | `ghcr.io/linuxserver/baseimage-ubuntu:noble`, `docker.io/library/eclipse-temurin` | the workflows' mirror stanzas | **yes** |
| dind's daemon | `mcr.microsoft.com/powershell` | nothing — not Hub, not BuildKit | **no** |
| dind's daemon | `ghcr.io/catthehacker/ubuntu:runner-latest` (job containers) | nothing — daemon-level ghcr | **no** |

The two uncovered rows need Option 3's MITM proxy, and are worth it only if measurement says they are a
real share. The row that was actually failing — BuildKit's ghcr pulls, three runs lost to 429 — is
covered.

### 3a. The workflow half, which is already in place

`docker-test.yml` and `release-build.yml` probe each cache with `GET /v2/` and emit a
`buildkitd.toml` containing stanzas only for the ones that answered, then pass it as
`buildkitd-config`. Three things about it are deliberate:

- **`driver-opts: network=…` is gone.** With fixed IPs the builder has no network to join, and its
  removal also removes the only way this step could kill a build outright
  (`network runners_default not found`, which is what broke run 797).
- **The probe asks what matters.** "Does this network exist" was a proxy for "can the builder reach the
  cache", and it answered about the wrong daemon. A `GET /v2/` from the job takes the same path the
  builder takes, both being children of the same dind daemon.
- **It fails soft, per registry.** A cache that does not answer is left out; the build goes upstream for
  that registry and through the cache for the other. Measured with one of the two stopped.

The second stanza per registry (`[registry."<host:port>"] http = true`) is not optional and not
obvious: `newMirrorRegistryHost` defaults a mirror to `https`, and `fillInsecureOpts` looks the
mirror's scheme up under a **top-level** section of that name. Without it buildkit dials `https`, fails
the handshake, falls back to upstream — and silently does nothing at all.

**Fallback is verified from source rather than from the docs**, which do not state it: `NewRegistryConfig`
in `util/resolver/resolver.go` appends each mirror first and the upstream host **last**, and containerd's
resolver walks that list in order. The mirror entries carry `HostCapabilityPull | HostCapabilityResolve`
only, so **pushes always go upstream** — `release-build.yml`'s push to ghcr and Docker Hub is untouched.

### 4. Prove it worked, then record the numbers

**First prove each container is a *cache* and not an empty registry.** `REGISTRY_<SECTION>_<KEY>` is
Distribution's documented environment override for `config.yml`, and a **misspelled override is ignored
silently**: the registry comes up as an empty *writable* registry instead of a proxy, then 404s every
pull, which looks exactly like a network fault. A `200` on the second line below can only have come from
upstream through the proxy, so it is the whole test:

```
curl -s -o /dev/null -w '%{http_code}\n' http://172.31.77.6:5000/v2/                      # 200
curl -s -o /dev/null -w '%{http_code}\n' \
  -H 'Accept: application/vnd.oci.image.index.v1+json' \
  http://172.31.77.6:5000/v2/linuxserver/baseimage-ubuntu/manifests/noble                 # 200
docker logs registry-cache-ghcr | grep -c 'GET /v2/'                                      # non-zero
```

Run those from the **host**, which is on the `registry-cache` bridge. From a job they are the probe the
workflow already performs.

**Then prove dind is actually using the Hub mirror**, which is a separate claim:

```
docker -H unix:///var/lib/docker/volumes/.../docker.sock info | grep -A2 'Registry Mirrors'
# or simply, after a job has run:
docker logs --since 10m registry-cache-dockerhub | grep -c 'GET /v2/'
```

Per this repo's convention that build and CI changes are verified by measurement rather than by tests,
put before/after in the commit message:

```
docker exec registry-cache-ghcr du -sh /var/lib/registry        # grows on the first build, then plateaus
docker logs --since 10m registry-cache-ghcr | grep -c 'GET /v2/'
```

The number that matters is the `Build image` step's duration on a **second** run of the same commit: if
the mirror is serving, the `FROM` stages drop to roughly local-disk speed and no ghcr blob `GET` leaves
the host at all.

---

## Option 2 — one endpoint for several upstreams (Harbor)

Harbor's proxy-cache projects front many upstreams behind one service, which removes the
one-container-per-registry sprawl. BuildKit takes it directly, because it accepts a path in a mirror
(`mirrors = ["harbor.example/proxy.ghcr.io"]`). **Docker Engine does not** — a daemon mirror URL must be
a domain root with no path — so Harbor replaces the two `registry:3` containers and the workflows'
mirror stanzas, but *not* dind's `--registry-mirror`, which still needs a path-free root. It is the right
answer if the host is going to cache for more than this repository; it is a lot of machinery for two
base images.

## Option 3 — transparent caching for the daemon too (a MITM proxy)

The only way to cache `ghcr.io` for **dockerd** is to stop asking dockerd to do it: run a caching proxy
(`rpardini/docker-registry-proxy` is the usual one) and give the daemon `HTTPS_PROXY` in a systemd
drop-in. It caches every registry, including the runner image, and needs its CA trusted by the daemon.
**This is what would close the two uncovered rows in section 3** — `mcr.microsoft.com/powershell` and the
`ghcr.io/catthehacker/ubuntu:runner-latest` job image. Worth it only once measurement says those are a
real share of the traffic; by default they are not, because `container.force_pull` is `false` and
dind's `dind-storage` volume keeps the runner image across restarts. Check that setting (below) before
building any of this.

## Runner hygiene worth checking first, because it is free

- **`container.force_pull` in the runner's `config.yaml`.** It defaults to `false`, but most
  docker-compose examples for `forgejo-runner` set it to `true`, and several jobs run per push. If it is
  `true` here, the host is pulling `ghcr.io/catthehacker/ubuntu:runner-latest` **once per job** for no
  benefit, and that alone could be the burst. Check it before building anything.
- **One push used to build the same commit twice, which doubled every pull below — fixed 2026-10-02.**
  Three conditions had to coincide, and all three were normal here: a workflow triggering on **both**
  `push:` and `pull_request:`; a long-lived PR open whose head is the branch being pushed (the standing
  `develop` → `beta` one); and a concurrency group carrying `${{ github.ref }}`, which differs between
  `refs/heads/develop` and the PR's ref, so nothing deduplicated them. The per-ref key was the *enabling*
  condition, not the cause — it is what stopped the second run being queued behind the first.

  `test.yml`, `docker-test.yml` and `grinder-container-it.yml` were the three that fired twice, and
  `pull_request:` has been removed from all three: every PR on this instance comes from a branch of this
  repository, which `push:` already builds. `docs.yml`, `qodana.yml` and the release workflows trigger on
  `push:` only and never doubled. Clearest evidence of the old behaviour in the run list: indices **665
  (#678)** and **666 (develop)**, same commit, both started `2026-09-27T15:10:11`; and the batch of nine
  runs queued at `2026-10-01T17:03:00`, which is what made `DockerJavaContainerEngineIT` fail three runs
  in a row on 2026-09-27 and took `test.yml` and `grinder-container-it.yml` down on 2026-10-01. What it
  costs is in each workflow's own header comment: the merge result is no longer built separately, and a
  fork PR gets no Forgejo CI (GitHub's mirrored `test.yml` keeps `pull_request:` and covers that).

## What none of this fixes

A cache reduces requests; it does not make the host immune. The first pull of any *new* tag still goes
upstream and can still meet the limiter — which is why the `continue-on-error` retry in both workflows
stays regardless of what is done here.
