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

### 1. The caches

`/opt/registry-cache/compose.yaml` on the runner host:

```yaml
name: registry-cache

networks:
  registry-cache:
    name: registry-cache          # explicit, so the buildx builder can join it by this exact name

volumes:
  ghcr-data:
  dockerhub-data:

services:
  ghcr:
    image: registry:3
    container_name: registry-cache-ghcr
    restart: unless-stopped
    environment:
      REGISTRY_PROXY_REMOTEURL: "https://ghcr.io"
      REGISTRY_PROXY_TTL: "168h"
      REGISTRY_STORAGE_FILESYSTEM_ROOTDIRECTORY: /var/lib/registry
    volumes:
      - ghcr-data:/var/lib/registry
    networks: [registry-cache]
    ports:
      - "127.0.0.1:5002:5000"     # loopback only -- an open proxy cache is not something to publish

  dockerhub:
    image: registry:3
    container_name: registry-cache-dockerhub
    restart: unless-stopped
    environment:
      REGISTRY_PROXY_REMOTEURL: "https://registry-1.docker.io"
      REGISTRY_PROXY_TTL: "168h"
      # Optional but worth it: an authenticated upstream turns Docker Hub's anonymous 100-pulls/6h into
      # the account's allowance. If you set these, the cache serves whatever that account can see --
      # keep it bound to loopback and to the runner network.
      # REGISTRY_PROXY_USERNAME: "…"
      # REGISTRY_PROXY_PASSWORD: "…"
    volumes:
      - dockerhub-data:/var/lib/registry
    networks: [registry-cache]
    ports:
      - "127.0.0.1:5001:5000"
```

`REGISTRY_<SECTION>_<KEY>` is Distribution's documented environment override for `config.yml`, so the
four `REGISTRY_PROXY_*` names above are the `proxy:` block. **Prove it before wiring CI to it** — a
misspelled override is ignored silently and the registry comes up as an empty *writable* registry
rather than a cache, which then 404s every pull and looks like a network fault:

```
docker compose -f /opt/registry-cache/compose.yaml up -d
curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1:5002/v2/                     # 200
curl -s -o /dev/null -w '%{http_code}\n' \
  -H 'Accept: application/vnd.oci.image.index.v1+json' \
  http://127.0.0.1:5002/v2/linuxserver/baseimage-ubuntu/manifests/noble                # 200, and
docker logs registry-cache-ghcr | grep -c 'GET /v2/'                                   # non-zero
```

A `200` on the second line is the whole test: it can only have come from ghcr through the proxy.

### 2. The daemon (Docker Hub only — this is the constraint above, not an oversight)

`/etc/docker/daemon.json`:

```json
{
  "registry-mirrors": ["http://127.0.0.1:5001"]
}
```

`127.0.0.0/8` is in Docker's default insecure-registry list, so plain HTTP on loopback needs no
`insecure-registries` entry. `systemctl reload docker`, then confirm with `docker info | grep -A2 'Registry Mirrors'`.
This covers `busybox`, `alpine` and the grinder's Docker Hub pulls. **It does nothing for
`ghcr.io/catthehacker/ubuntu:runner-latest`** — see Option 3 if that turns out to matter.

### 3. BuildKit (this is the one that fixes the observed failure)

In `docker-test.yml` and `release-build.yml`, on the `Set up Docker Buildx` step:

```yaml
        with:
          driver-opts: network=registry-cache
          buildkitd-config-inline: |
            [registry."ghcr.io"]
              mirrors = ["registry-cache-ghcr:5000"]
            [registry."registry-cache-ghcr:5000"]
              http = true
            [registry."docker.io"]
              mirrors = ["registry-cache-dockerhub:5000"]
            [registry."registry-cache-dockerhub:5000"]
              http = true
```

The second and fourth stanzas are not optional and are not obvious: `newMirrorRegistryHost` defaults a
mirror to `https`, and `fillInsecureOpts` looks the mirror's own scheme up under a **top-level
`[registry."<mirror host:port>"]` section**. Without it, buildkit dials `https://registry-cache-ghcr:5000`,
fails the handshake, and falls back to upstream — i.e. it silently does nothing at all.

**`driver-opts: network=registry-cache` is load-bearing, and the reason is written into this repo's
history.** The builder is a container; without joining the cache's network its only route to a
host-published port is the Docker bridge gateway, and this host's `iptables` `INPUT` chain has
`policy DROP`. That configuration has already cost this project a 131-second outage diagnosed three
different wrong ways (see the *symptom shared by two layers* entry in `CLAUDE.md`), and it fails the
same silent way here: the SYN is dropped, buildkit waits out `tcp_syn_retries`, then falls back to
upstream and the mirror appears to be "not working". Joining the network sidesteps the host stack
entirely and gets DNS on the service name for free.

### 4. Prove it worked, then record the numbers

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
a domain root with no path — so Harbor replaces step 1 and step 3 above but not step 2. It is the right
answer if the host is going to cache for more than this repository; it is a lot of machinery for two
base images.

## Option 3 — transparent caching for the daemon too (a MITM proxy)

The only way to cache `ghcr.io` for **dockerd** is to stop asking dockerd to do it: run a caching proxy
(`rpardini/docker-registry-proxy` is the usual one) and give the daemon `HTTPS_PROXY` in a systemd
drop-in. It caches every registry, including the runner image, and needs its CA trusted by the daemon.
Only worth it if measurement shows the runner-image pulls are a real share of the traffic — which they
are not by default:

## Runner hygiene worth checking first, because it is free

- **`container.force_pull` in the runner's `config.yaml`.** It defaults to `false`, but most
  docker-compose examples for `forgejo-runner` set it to `true`, and several jobs run per push. If it is
  `true` here, the host is pulling `ghcr.io/catthehacker/ubuntu:runner-latest` **once per job** for no
  benefit, and that alone could be the burst. Check it before building anything.
- **`concurrency` is keyed per-ref** (`<workflow>-${{ github.ref }}`), so one push to `develop` builds the
  same commit twice, in parallel, against one registry from one address. That is a deliberate trade
  recorded in `.claude/rules/ci-workflows.md`; it is also a x2 on every pull this file is trying to
  reduce.

## What none of this fixes

A cache reduces requests; it does not make the host immune. The first pull of any *new* tag still goes
upstream and can still meet the limiter — which is why the `continue-on-error` retry in both workflows
stays regardless of what is done here.
