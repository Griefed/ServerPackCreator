# Forgejo Actions

Converted from `.gitlab-ci.yml` (removed in the same commit; recoverable via
`git log --follow`). The conversion is a port of the one made for
`Griefed/griefed.de`, which was verified against **Forgejo 16.0.5+gitea-1.22.0**
on `git.griefed.de`.

## Workflows

| Workflow | Trigger | Replaces |
|---|---|---|
| `workflows/ci.yml` | push to any branch, manual | stages `test` (`test quasar`, `test docker`), `release`, and the `pages` job |
| `workflows/build.yml` | push of a `x.y.z` tag, manual | the `build` job |
| `workflows/packages.yml` | daily at 03:00 UTC, manual | `Check Packages:on-schedule` |

`scripts/forgejo-release.mjs` creates the Forgejo release and is invoked by
`@semantic-release/exec` from `.releaserc.yml`.

## Required runner setup

* A runner labelled **`ubuntu-latest`**. Change `runs-on:` in all three
  workflows if your label differs.
* That runner must expose a **Docker daemon** to the job — `setup-buildx-action`
  and `build-push-action` cannot work without one. With the Docker backend this
  means setting `container.docker_host` in the runner's `config.yaml`.
* The runner's **built-in cache server** must be enabled (it is by default) for
  `setup-node`'s `cache: 'npm'` in `ci.yml`.

## Required repository configuration

### Variables (Settings → Actions → Variables)

| Name | Used by | Value |
|---|---|---|
| `DOCKERHUB_USER` | `build.yml` | Docker Hub namespace |
| `DOCKERHUB_REPO` | `build.yml` | image name, e.g. `serverpackcreator-de` |
| `GHCR_USER` | `build.yml` | GitHub namespace. Optional — falls back to `DOCKERHUB_USER`, which is what the GitLab pipeline used |
| `GIT_USER` | `packages.yml` | committer name |
| `GIT_MAIL` | `packages.yml` | committer e-mail |

### Secrets (Settings → Actions → Secrets)

| Name | Used by | Scope needed |
|---|---|---|
| `DOCKERHUB_TOKEN` | `build.yml` | Docker Hub access token |
| `GHCR_TOKEN` | `build.yml` | GitHub PAT with `write:packages` |
| `PACKAGES_TOKEN` | `build.yml` | Forgejo PAT with `write:package` |
| `RELEASE_USER` | `ci.yml`, `packages.yml` | Forgejo username the release commits are pushed as |
| `RELEASE_TOKEN` | `ci.yml`, `packages.yml` | Forgejo PAT with `write:repository` |

## Notes on the conversion

* **`GITHUB_TOKEN` was renamed to `GHCR_TOKEN`.** Forgejo injects a
  `GITHUB_TOKEN` of its own into the `secrets` context, so a repository secret
  of that name cannot be used.
* **The automatic per-run token cannot push to the package registry**
  ([forgejo#1296](https://codeberg.org/forgejo/forgejo/issues/1296)), which is
  why `PACKAGES_TOKEN` exists rather than reusing `secrets.GITHUB_TOKEN`.
* **`@semantic-release/gitlab` was replaced.** It speaks the GitLab API and has
  no Forgejo mode. `scripts/forgejo-release.mjs` posts to
  `/api/v1/repos/{owner}/{repo}/releases` instead, and the `gitlabUrl` key is
  gone from `.releaserc.yml`.
* **`breaking` and `improv` were added to the changelog preset.** Both are
  release-triggering types in `analyzeCommits` but had no `presetConfig.types`
  entry, so the preset hid them. That is why `1.1.0` — the release the
  `improv: Add ko-fi links` commit triggered — has no notes in `CHANGELOG.md`.
* **The semantic-release toolchain is installed globally in the job, not in
  `package.json`.** semantic-release 25 requires Node `^22.14 || >=24.10` while
  the app is built on Node 20; one dependency tree cannot satisfy both. The
  pinned versions live in the `Install semantic-release` step of `ci.yml`.
* **GitLab Pages has no equivalent.** The built SPA is uploaded as a workflow
  artifact (7 day retention, matching the old `expire_in: 1 week`). The
  `cp -Rf dist/spa/* public/` that the `pages` job did is gone: it existed only
  to give the artifact the directory name GitLab Pages demands, and Quasar
  already copies `public/` into `dist/spa` on its own. The site itself is
  served by the image `build.yml` publishes.
* **The artifact upload uses `forgejo/upload-artifact`, not
  `actions/upload-artifact`.** From v4 on, the upstream action refuses to run
  against anything that is not GitHub.com. Forgejo maintains a fork with that
  check removed, and the mirror of the upstream action carries a warning
  saying so. `actions/cache` and `setup-node`'s `cache:` input need no such
  fork: their `isGhes()` check makes them fall back to cache service v1,
  which is the API forgejo-runner implements.
* **The tag filter `[0-9]+.[0-9]+.[0-9]+` is unchanged from the GitLab rule.**
  Forgejo compiles ref filters to a regular expression, where `+` and `?` are
  quantifiers and every other character is escaped, so the pattern becomes
  `^[0-9]+\.[0-9]+\.[0-9]+$` — exactly what `/^\d+\.\d+\.\d+$/` matched.
* **`build.yml` is guarded with `startsWith(github.ref, 'refs/tags/')`.**
  Without it, a manual `workflow_dispatch` from a branch would publish that
  branch as `:latest` over the released image.
* **`Dockerfile` now builds from the build context.** It used to `git clone`
  `https://$HOSTER/Griefed/serverpackcreator-de.git` inside the builder stage,
  so it built whatever the remote branch pointed at rather than the commit the
  workflow checked out, and required the repository to be anonymously readable.
  The builder stage now `COPY`s the context, so the `BRANCH_OR_TAG` and
  `HOSTER` build args are gone from all workflows, and a `.dockerignore` keeps
  `node_modules`/`dist` out of the context.
* **The Docker Hub login step must not set `registry:`.** Docker Hub is the one
  registry whose credentials are filed under a canonical key,
  `https://index.docker.io/v1/`, rather than under its hostname. `docker login`
  only rewrites the server address to that key when the argument is empty or
  exactly `docker.io`, so `registry: index.docker.io` filed the credential
  under `index.docker.io` and buildkit — which looks Docker Hub up under the
  canonical key for host `registry-1.docker.io` — never found it. The push went
  out anonymously and Docker Hub answered `insufficient_scope: authorization
  failed`, on a step whose login had reported success moments earlier. `ghcr.io`
  and the Forgejo registry are ordinary hosts and do take `registry:`. The tell
  in a build log is a missing `[auth] <repo>:pull,push token for
  registry-1.docker.io` line: if buildx never asked for a Docker Hub token, it
  had no credentials to ask with.
* **Renovate does update the action pins.** The `uses:` references are fully
  qualified (`https://data.forgejo.org/...`) so that they resolve the same way
  regardless of the instance's `DEFAULT_ACTIONS_URL`, and that is the form
  Renovate handles natively. Its `github-actions` manager scans
  `.forgejo/workflows` — the default file pattern is
  `/(^|/)(workflow-templates|\.(?:github|gitea|forgejo)/(?:workflows|actions))/.+\.ya?ml$/`
  — and has parsed the `https://host/owner/repo@ref` form since
  [#24336](https://github.com/renovatebot/renovate/pull/24336)
  (*Support for Gitea and Forgejo actions within `github-actions` manager*,
  2023-09-10). It splits the hostname out of the ref, recognises Forgejo from
  it, and queries the forgejo-tags datasource against that host rather than
  github.com.
  [renovate#31666](https://github.com/renovatebot/renovate/discussions/31666)
  is sometimes read as saying otherwise, but it asks for the opposite case:
  resolving a *bare* `uses: actions/checkout@v5` against the instance's
  `DEFAULT_ACTIONS_URL` instead of assuming github.com. Renovate's parser
  defaults a ref with no hostname to `github.com`, so bare refs would indeed go
  unresolved — full URLs sidestep that entirely.

* **`renovate.json` was modernized alongside this migration.** `config:base` ->
  `config:recommended`; the old name only still resolves through Renovate's
  `removedPresets` compatibility map. The `packageRules` entry was dropped
  entirely -- it re-extended the very preset already applied repo-wide, scoped
  to `package.json`, which did nothing. `renovate-config-validator` previously
  reported `WARN: Config migration necessary` and now passes clean.
* **`.github/workflows/gh-pages.yml` was rebuilt, not migrated.** It is the
  GitHub-Pages fallback for the site and is independent of this pipeline, but
  it had never once succeeded -- all three runs it ever had failed, no
  `gh-pages` branch was created, and Pages was never enabled. It now uses the
  native Pages deployment (`configure-pages` + `upload-pages-artifact` +
  `deploy-pages`) instead of pushing a `gh-pages` branch. Two manual steps are
  still required: set *Settings -> Pages -> Source* to **GitHub Actions**, and
  recreate the push mirror (it used to be driven from GitLab) under
  *Settings -> Repository -> Mirror Settings* in Forgejo, or nothing will ever
  reach GitHub to trigger it.
* **The `gitlabci` Renovate manager has nothing left to do here.**
  `griefed/gitlab-ci-cd` is now referenced only from `Dockerfile`, which the
  `dockerfile` manager covers.

## Known issues inherited from before the migration

* `Dockerfile`'s builder stage still uses `griefed/gitlab-ci-cd:2.8.0`. It is
  only a Node + Quasar toolchain image, so it works, but the name is now
  misleading.
* `ghcr.io/linuxserver/nginx:1.22.1` is the runtime base and is several years
  old. Bumping it is unrelated to this migration and was left alone.
