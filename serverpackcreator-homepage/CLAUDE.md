# serverpackcreator-homepage — Claude Code context

The Quasar/Vue single-page site served at **serverpackcreator.de**, plus the documentation it
hosts at `/help/latest` and `/api/latest`.

Not a Gradle module — `settings.gradle.kts` does not mention it, and `./gradlew build` never
touches it. It is built by `npm`/`quasar` and shipped as its own container image.

## This directory is a git subtree, and that governs how you update it

Its upstream is **https://git.griefed.de/Griefed/serverpackcreator-de**, which still holds the
homepage's own history and its `1.x` release tags. This repository holds only a squashed import —
`chore(homepage): import serverpackcreator.de as a subtree, squashed`, which carries the
`git-subtree-dir` and `git-subtree-split` trailers that `git subtree` reads.

**Pull with `--squash`, always:**

```
git subtree pull --squash --prefix=serverpackcreator-homepage \
  https://git.griefed.de/Griefed/serverpackcreator-de master
```

A plain, non-squash `git subtree pull` merges the upstream tip into this history and makes every
homepage commit an ancestor of `develop` again. That matters because SPC's `.releaserc.yml` uses a
bare `tagFormat: '${version}'` and semantic-release analyses *every* commit since the last SPC tag:
the homepage's conventional-commit subjects are read as SPC's own. The original `git subtree add`
did exactly this and put 39 website commits into SPC's next changelog, one of them a `breaking:`
that would have released 10.0.0 instead of 9.3.0. Since the squash, a non-squash pull refuses with
*"refusing to merge unrelated histories"* rather than doing it silently — do not reach for
`--allow-unrelated-histories` to get past that; it is the guard working.

**Check for upstream tags after every pull.** `git subtree` fetches with plain
`git fetch <url> <ref>`, and git auto-follows any tag pointing into the history it downloads — which
is how `1.0.0`–`1.3.1` arrived here in the first place. They collide with SPC's own release
namespace (`tagFormat` is a bare `'${version}'`, and SPC itself owns `1.5.1`–`1.6.1` from its early
days), so a `git push --tags` would publish phantom SPC releases. After pulling:

```
git tag -l '1.[0-4].*'          # anything listed is the homepage's, not SPC's
git tag -d $(git tag -l '1.[0-4].*')
```

Nothing is lost by deleting them — the upstream repository is where those tags are looked up, and
`git subtree pull --squash` re-fetches the recorded split by hash when it needs it.

### Griefed's call, 2026-10-24: this directory is now authored HERE, and upstream will conflict

The webpack → Vite migration, the TypeScript conversion and the download counter were all written in
ServerPackCreator rather than upstream, deliberately, and over B49's own recommendation that a change
of that size "belongs in the website's own repository, pulled back as one squashed commit". The reason
the recommendation lost: the same work also changes `.forgejo/workflows/docs.yml` and
`claude-docs/BACKLOG.md`, which are **not** in the subtree, so an upstream-first route cannot verify
the migration against this repository's CI or its Dockerfile — and those are the two build paths it
had to be proven against.

**The cost is real and was accepted:** `Griefed/serverpackcreator-de` still holds a webpack tree that
no longer reflects the deployed site, so the next `git subtree pull --squash` will conflict across
roughly fifteen files. Treat this directory as the source of truth and replay outward
(`git subtree push`) rather than pulling inward, or retire the upstream repository. **Do not resolve
that conflict by taking upstream's side** — it is three major versions of toolchain behind.

## The addons overview is a PAGE here, not a project

`src/pages/AddonIndex.vue` is the curated plugin listing, reachable at `/addons`. It used to be its
own Quasar repository (`Griefed/ServerPackCreator-Addons-Overview`, archived) and was briefly imported
as a second subtree before being folded in here instead — the whole thing is one object literal of
three entries and makes no HTTP request, so a second npm project, lockfile, CLI and CI job were all
cost with no return. Adding a plugin is editing that literal.

**`/addons` works only because `public/addons/index.html` exists.** The router is in `hash` mode, so
the route itself is `/#/addons` and nginx would answer a bare `/addons` with a 404. That stub is a
static file rather than an nginx rule for the reason `docs/assemble-docs.sh` already records about
`/help/latest`: the LSIO base image declares `/config` a VOLUME and seeds its site-conf onto it once,
so a redirect baked into the image works on a fresh volume and silently does nothing on the live host.
Quasar copies `public/` to the site root, which is what puts the stub beside `help/` and `api/`.

Two constraints on editing the page itself: `MainLayout.vue` already renders `<router-view/>` inside a
`q-page`, so `AddonIndex.vue`'s root is a plain `<div>` and must stay one; and its info dialog needs
the `Notify` plugin, which `quasar.conf.js` already enables.

## Where each fact already lives — read it there, don't restate it here

- **How the image is built and why it fails without docs** → `Dockerfile`. Two stages: a Node
  toolchain builds the SPA, an LSIO nginx serves it from `/config/www`, and a final `RUN` fails the
  build outright if `help/latest/config.json` or `api/latest/index.html` is missing.
- **How the documentation tree is assembled** → `docs/assemble-docs.sh`, which is a script rather
  than Dockerfile steps so it can be run against a scratch directory and the result clicked. It must
  run *before* `docker build`; the Dockerfile only `COPY`s `docs-build/`. Both archives are **handed
  to it** — `SPC_WEBHELP_ZIP` and `SPC_DOKKA_TARGZ`, each required — so it fetches nothing and needs
  no network, no Docker daemon and no credentials.
- **What it guarantees** → `docs/assemble-docs.test.sh`, which runs it end to end against fixture
  archives. `npm test` is that script, and `docs.yml`'s `homepage-build` job runs it before the
  Quasar build.
- **How CI wires the two together** → the `homepage-build` and `homepage-image` jobs in
  `.forgejo/workflows/docs.yml`.

## Local development

```
npm ci && npx quasar dev          # dev server
npm run build                     # collect-downloads, then quasar build into dist/spa
npm run lint                      # eslint 10, flat config
npm run type-check                # vue-tsc --noEmit
npm run test:unit                 # vitest
npm test                          # docs/assemble-docs.test.sh -- NOT the unit suite
npm run collect-downloads         # writes src/generated/download-counts.json
```

**`npm test` and `npm run test:unit` are deliberately two scripts.** `.forgejo/workflows/docs.yml`'s
*Verify the documentation assembly* step runs `npm test`, and folding Vitest into it — which is what
copying `serverpackcreator-web-frontend`'s `package.json` does — silently drops that gate.

`src/` has a test suite now (Vitest + Vue Test Utils + happy-dom, `test/`), but it covers logic, not
appearance: nothing in it can see a layout regression, and nothing in SPC's Gradle build reaches this
module. **A change to a `.vue` file is still verified by looking at it in `quasar dev`.**

## Toolchain — `@quasar/app-vite` 3, TypeScript, and three landmines

Migrated off `@quasar/app` 3.3.3 (the pre-rename **webpack** CLI) on 2026-10-24, which is what closed
B49 and CVE-2026-73647. All of `src/` is TypeScript. One path alias, `@` → `src/`; in templates an
asset is `~@/assets/x` and in SCSS `url("@/assets/x")`.

- **LANDMINE — `build.vueOptionsAPI: true` in `quasar.config.js` is load-bearing.** app-vite 3
  defaults it to **false**, which compiles `__VUE_OPTIONS_API__ = "false"` and tree-shakes
  `applyOptions` out of Vue. Every SFC here is Options API, so without it every page still renders and
  nothing throws — the page just loses `data`, `methods`, `computed` and every lifecycle hook. That is
  precisely what stalled B49's prototype (`cardImage is not a function` ×9, no flashcard width, blank
  dev page) and it went undiagnosed because the symptoms look like a bundling fault.
  `test/config/quasar-config.test.js` pins the flag, which is all a unit test can reach — Vitest does
  not apply the build's `define`s, so only a production bundle shows the real thing. The same landmine
  is documented at length in `serverpackcreator-web-frontend/CLAUDE.md`; this is the pointer, not a
  second copy.
- **LANDMINE — `process.env.*` does not exist in `src/`.** It is `import.meta.env.QUASAR_*`, a rename
  and not a relocation. A read is a `ReferenceError` under `quasar dev` and **silently `undefined` in a
  build**. `test/config/no-process-env.test.js` pins it for all of `src/`. `QUASAR_SERVER` is
  substituted as a boolean literal — never compare it to `'false'`.
- **LANDMINE — a page's root must not be `<q-layout>`.** `MainLayout.vue` already provides one, and
  Quasar supplies QLayout by injection, so a nested one shadows the outer for its whole subtree.
  `Index.vue` carried this for years and was fixed on 2026-10-24; `AddonIndex.vue` and `Error404.vue`
  are plain `<div>`s and must stay that way.

`@quasar/extras` is pinned at **1.18.0 on purpose**: 2.x's `exports` map has no `fontawesome-v5` or
`mdi-v5`, both of which `quasar.config.js` requests. 1.18.0 already ships the v7 sets, so the
rename-and-bump is available whenever someone wants it — as one change, not as a side effect.

The qflashcard app extension must stay at **3.0.0 or newer**: 2.0.0-alpha.4 refuses app-vite 3
outright (`compatibleWith('@quasar/app-vite', '^1.0.0-alpha.0')`), and its shipped
`dist/types/index.d.ts` contained `import { String | StringArray } from './types'` — three TS *parse*
errors, which neither `skipLibCheck` nor an ambient `declare module` can suppress, because both gate
semantic checking while the file is still resolved and parsed.

## The download counter is a library, and it is counted at BUILD time

`src/services/downloads/` counts release-asset downloads across **GitHub, Gitea and Forgejo**. It
hard-codes no URL — `countPlatformDownloads(platform, repositoryUrl)` derives everything from the URL,
and `countAllDownloads(owner, repository, { hosts })` falls back to `DEFAULT_HOSTS`. SPC's own
repositories are in `src/config/repositories.ts`, which is site configuration, not library content.

- **LANDMINE — Gitea and Forgejo page with `limit` (max 50), NOT `per_page`, and sending the wrong
  one fails silently.** They ignore an unknown paging parameter and fall back to the instance's
  `DEFAULT_PAGING_NUM` — 10 on gitea.com, 30 on git.griefed.de — returning a short page with no error,
  so the counter stops early and reports a confident fraction. Measured against the live forges:
  gitea **6,385 → 907** and forgejo **8,546 → 2,008** from that one constant.
  `test/services/downloads/gitea-counter.test.js` is the guard.
- **GitLab is not supported and must not be "added".** It publishes no release-asset download counts
  at all — `assets.links[]` carries `id, name, url, direct_asset_url, link_type` and nothing else, and
  [gitlab#327317](https://gitlab.com/gitlab-org/gitlab/-/issues/327317) has been open since 2021-04-09.
  `counterFor('gitlab')` raises `UnknownPlatformError` saying so. Counting it as zero would be
  indistinguishable from a repository nobody downloads.
- **Why build time.** Node has no CORS, so `git.griefed.de` is countable there and **not** from a
  browser: it sends no `Access-Control-Allow-Origin`, which is Forgejo's `[cors] ENABLED = false`
  default (Codeberg, same major, sends `*`). Build-time counting also spares each visitor ~20 requests
  and several MB, and spares GitHub's 60-per-hour-per-IP anonymous ceiling, which the old
  `github-release-stats` chip burned on every page load. `refreshDownloadTotal()` then updates the
  figure in the background at most once a day per visitor.
- **`src/generated/download-counts.json` is gitignored and must stay so.** A committed copy is a second
  place a total is recorded and goes stale — the mistake `docs/versions.json` made. The collector is
  fail-soft and always exits 0: with every forge unreachable it writes `{"total": null}` and the chip
  renders "many", because no forge reachable is a different fact from nobody downloading anything.

To let the browser refresh include Forgejo, git.griefed.de needs CORS enabled. **That is host
configuration and is not done.** Nothing breaks without it; the baked figure already includes Forgejo.

```ini
[cors]
ENABLED = true
; omit ALLOW_DOMAIN to keep the default `*`, which is what gitea.com and codeberg.org send
```

or, in docker-compose, `FORGEJO__cors__ENABLED=true`.

**`ALLOW_DOMAIN` takes ORIGINS, not domains, and a bare hostname silently matches nothing.** The
field is passed straight to go-chi/cors as `AllowedOrigins`
(`routers/api/shared/middleware.go`), and its own declaration carries the admission:
*"this option is from legacy code, it actually works as AllowedOrigins"*
(`modules/setting/cors.go`). An `Origin` header always carries a scheme, so the value must be
`https://serverpackcreator.de`. Restricting it also costs a `Vary: Origin`, which any caching proxy
in front of the instance then has to honour. `SCHEME` and `ALLOW_SUBDOMAIN` no longer exist.

The middleware is registered **only on the API routes** and only behind `if
setting.CORSConfig.Enabled`, so enabling it changes nothing about the web UI.

## One version per image — the site archives nothing

`/help/latest` and `/api/latest` are the only documentation URLs the site has, and they hold what
the build that produced the image rendered. **This image is tagged per release**, so the
documentation inside it is that release's by construction; `latest` cannot drift, because there is
nothing else in there for it to drift away from. An older release's documentation is read from the
image tagged for it, and an older API reference is also a `dokka-html.zip` release asset.

Two things were removed on 2026-10-19 to get here, and both are worth knowing about because their
shapes keep suggesting themselves:

- **A `serverpackcreator-help:<version>` container per release**, which `assemble-docs.sh` extracted
  with `docker create` + `docker cp`. It existed to host the help when GitLab Pages went away and
  Forgejo had no equivalent; once this site served the help it had no reader but the assembly, which
  pulled 62 MB per version to take 4 MB out of it. Docker Hub held **912 tags** of it. Nothing on
  the site ever linked a per-version help URL — `MainLayout.vue` links `/help/latest` and always has.
- **`docs/versions.json`**, a hand-edited manifest of which versions to serve, and the release-asset
  downloads it drove. It was a second place for a release to be recorded, so it could be — and was —
  stale: it still named 9.2.1 as latest after 9.3.0 and 9.3.1 had shipped. Griefed's call: the image
  tag already carries that fact, so nothing should restate it.

**Griefed's call is also why "add it back for just the API reference" is not an open question.** The
asymmetry is tempting — a reference is tied to a compile in a way user documentation is not — and it
was considered and declined: the release asset is where a specific version's reference lives, and
the site is not a second distribution channel for it.
