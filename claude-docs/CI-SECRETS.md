# Forgejo Actions — secrets and variables reference

Operator-facing companion to `.forgejo/workflows/`. It answers three questions that are painful to
reconstruct while a release is failing: **what is this secret**, **where do I get one**, and **what
breaks if it is missing**.

Create these as *repository* secrets on `git.griefed.de/Griefed/ServerPackCreator` (Settings →
Actions → Secrets), or at user level if you want them shared across repositories.

---

## The naming rule that shapes half this list

Forgejo validates Actions secret **and variable** names against

```
^(?!FORGEJO_|GITEA_|GITHUB_)[a-zA-Z_][a-zA-Z0-9_]*$
```

so all three of those prefixes are rejected outright
([forgejo#10682](https://codeberg.org/forgejo/forgejo/pulls/10682)). That is why the credentials here
are `FJ_*` and `GH_*` rather than the obvious `FORGEJO_*` and `GITHUB_*`.

`FORGEJO_TOKEN` and `GITHUB_TOKEN` are worse than merely unavailable: both are documented names for
the token Forgejo mints **per job**. A reference to either resolves to that automatic token instead
of failing, and it is repo-scoped, expires with the job, cannot write to the package registry, and
cannot push past branch protection — so the symptom is a permissions error a long way from the cause.

**The env-var names inside a step are unrestricted**, which is why `release-build.yml` maps
`FORGEJO_ACTOR: ${{ secrets.FJ_ACTOR }}`. That mismatch is deliberate:
`serverpackcreator.publishing-conventions` reads `FORGEJO_ACTOR`/`FORGEJO_TOKEN` with `System.getenv`,
and renaming the secrets must not force a buildSrc change. Do not "tidy" the two halves into
agreement.

Nothing in `.forgejo/workflows` uses the automatic job token.

---

## The secrets

| Secret | What it is / where to get one | Consumed by |
|---|---|---|
| `FJ_ACTOR` | Forgejo username (`Griefed`) | release-generate, release-build, devbuild, update-readme |
| `FJ_TOKEN` | Forgejo access token — Settings → Applications → Access Tokens. Scopes: **`write:package`** for the maven registry, **`write:repository`** to create releases, upload assets and push the `RELEASE:` commit and tags | the same four |
| `GH_ACTOR` | GitHub username; used as the GitHub Packages maven username | release-build |
| `GH_TOKEN` | GitHub **classic** PAT. Scopes: `write:packages` (covers both `maven.pkg.github.com` and the `ghcr.io` login) and `repo` (mirror the release, delete the stale `continuous` release). Fine-grained tokens do not cover ghcr/maven packages cleanly — use a classic one | release-build, devbuild, docs |
| `GITLABCOM_TOKEN` | gitlab.com PAT, scope **`api`** — one scope covers both the `Private-Token` maven upload to project `32677538` and the release API | release-build |
| `SIGNING_KEY` | ASCII-armoured PGP **private** key: `gpg --armor --export-secret-keys <KEYID>`, the entire block including BEGIN/END lines and newlines | release-build |
| `SIGNING_PASSWORD` | that key's passphrase | release-build |
| `SONATYPE_USERNAME` / `SONATYPE_PASSWORD` | Central Portal **user token** pair — central.sonatype.com → View Account → Generate User Token. Not your portal login | release-build |
| `DOCKERHUB_USER` | Docker Hub username, lowercase. Also the `ghcr.io` login user *and* the namespace in both tag sets — see the traps below | release-build, docs |
| `DOCKERHUB_TOKEN` | Docker Hub → Personal access tokens, Read/Write | release-build, docs |
| `DOCKERHUB_REPO` | image name, e.g. `serverpackcreator`. The help image's name is hardcoded in `docs.yml` and does not read this | release-build |
| `SPCUPLOAD_HOST` | SSH host for the devbuild artifact upload | devbuild |
| `SPCUPLOAD_USERNAME` | SSH user | devbuild |
| `SPCUPLOAD_KEY` | OpenSSH **private** key, **no passphrase** — the action cannot answer a prompt | devbuild |
| `SPCUPLOAD_TARGET` | remote directory the `continuous` folder is copied into | devbuild |
| `GIT_USER` / `GIT_MAIL` | committer identity for the automated commits (semantic-release's `RELEASE:` + changelog, and the sponsors/contributors refresh). Not credentials, but passed via `env:` rather than interpolated into a command, like every other value | release-generate, update-readme |
| `WEBHOOK_URL` | Discord webhook for the Qodana result post. **Optional**: the step exits cleanly when unset | qodana |
| `VT_API_KEY` | VirusTotal API key — profile → API key | release-build |
| `INSTALL4J_LICENSE` | install4j license key, for the major version the workflows pin (`version:` on the setup-install4j step, kept in step with `install4j` in `gradle/libs.versions.toml`). **a major-version bump needs a key valid for that version** — ej-technologies issues an upgraded key, free if the release falls in your support period — or both jobs fail at the media step | release-build, devbuild |

---

## What breaks without what

Useful when you want a partial setup working rather than all of it at once.

| Workflow | Needs | Degrades to |
|---|---|---|
| `test.yml`, `docker-test.yml` | **nothing** | — |
| `qodana.yml` | **nothing required**; `WEBHOOK_URL` optional | silently skips the Discord post without the webhook |
| `release-generate.yml` | `FJ_ACTOR`, `FJ_TOKEN`, `GIT_USER`, `GIT_MAIL` | nothing releases — this is the workflow that cuts the version and pushes the tag |
| `release-build.yml` | everything else | see below — several jobs fail independently |
| `devbuild.yml` | `FJ_*`, `GH_TOKEN`, `INSTALL4J_LICENSE`, `SPCUPLOAD_*` | no nightly `continuous` build |
| `docs.yml` | `GH_TOKEN`, `DOCKERHUB_USER`, `DOCKERHUB_TOKEN` | no help image |
| `update-readme.yml` | `FJ_*`, `GH_TOKEN`, `GIT_USER`, `GIT_MAIL` | sponsors/contributors stop refreshing |

Inside `release-build.yml` the jobs fail independently, so a missing secret usually costs one job
rather than the release:

| Job | Secrets it reads |
|---|---|
| `prepare` | none |
| `assets` | `INSTALL4J_LICENSE` |
| `release` | `FJ_TOKEN` |
| `maven` | `SIGNING_KEY`, `SIGNING_PASSWORD`, and then whichever registries you want: `FJ_*`, `GH_*`, `GITLABCOM_TOKEN`, `SONATYPE_*` |
| `docker` | `DOCKERHUB_USER`, `DOCKERHUB_TOKEN`, `DOCKERHUB_REPO`, `GH_TOKEN` (ghcr login) |
| `virustotal` | `VT_API_KEY`, `FJ_TOKEN` (appends the scan links to the release) |
| `mirror` | `GH_TOKEN`, `GITLABCOM_TOKEN`, `FJ_TOKEN` (reads the release notes back off Forgejo) |

`FJ_TOKEN` is therefore the one secret four separate jobs depend on — it is the first thing to check
when a release half-happens.

---

## Traps

**`SIGNING_KEY` must arrive as `ORG_GRADLE_PROJECT_signingKey`, never as `-PsigningKey=`.** A
multi-line value truncates at the first newline in an argument list, so `useInMemoryPgpKeys` receives
a fragment and signing fails misleadingly. Passing it by environment also keeps the private key out
of the process argument list, where anything able to read `/proc/<pid>/cmdline` could see it. This is
how GitLab supplied it too, and `release-build.yml` carries the reasoning inline.

**`DOCKERHUB_USER` is used for three different things** — the Docker Hub login, the `ghcr.io` login,
and the namespace in *both* `ghcr.io/$USER/$REPO` and `index.docker.io/$USER/$REPO`. This is faithful
to the retired `.gitlab-ci.yml`, which ran `docker login ghcr.io -u "$DOCKERHUB_USER"` in all four
image jobs, so it is pre-existing behaviour rather than a migration artefact. It works only because
the Docker Hub and GitHub account names coincide. If they ever diverge, ghcr pushes break and the fix
is a separate secret for the ghcr namespace.

**Qodana takes no token, and that is not an oversight.** `QODANA_TOKEN` is required only for the paid
linters and for uploading to Qodana Cloud; it is optional for the Community linters, and `qodana.yml`
runs `qodana-jvm-community`. There is no free Cloud tier and this project does not subscribe, so the
job is self-contained: it counts problems out of the SARIF it produced, attaches the HTML report as a
workflow artifact, and has Discord link the run. An empty token env would imply a Cloud setup that
does not exist, so there isn't one.

**`FJ_TOKEN` must be able to push to `main`, `alpha` and `beta`.** semantic-release pushes the
changelog commit and the tag itself. Under branch protection the token's account has to be an allowed
pusher, or the run ends with a tag that was never created and therefore a release that never builds.

**A token in a URL is a finding, not a shortcut.** Both workflows that push authenticate with a
host-scoped `http.<host>.extraheader` carrying HTTP Basic, and leave `origin` a bare URL. A credential
embedded in the remote URL is written into `.git/config` and then handed back by `git remote -v`, by
git's own error text on a failed push, and by anything later in the job that prints the remote.
`WORKFLOW-AUDIT.md` raised that as H-class against the pre-migration workflows; keep new push steps
on the header pattern.

---

## Verifying a setup without cutting a release

```bash
# Forgejo token: can it see the repo, and does it carry package scope?
curl -H "Authorization: token $FJ_TOKEN" \
  https://git.griefed.de/api/v1/repos/Griefed/ServerPackCreator | jq .permissions

# The maven registry, end to end, from a working tree:
FORGEJO_ACTOR=Griefed FORGEJO_TOKEN=$FJ_TOKEN \
  ./gradlew :serverpackcreator-api:publishMavenJavaPublicationToGitGriefedRepository
curl https://git.griefed.de/api/v1/packages/Griefed | jq '.[].name'

# GitHub PAT: does it actually carry write:packages?
curl -sI -H "Authorization: token $GH_TOKEN" https://api.github.com/user | grep -i x-oauth-scopes
```

The full end-to-end check is a prerelease on `alpha` — push a `feat:` and confirm the version is cut,
all release assets attach, the maven package appears, the images tag, and the GitHub release mirrors
with no duplicate tag.
