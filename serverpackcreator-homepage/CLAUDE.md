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
npm install && npx quasar dev     # dev server
npx quasar build                  # production build into dist/spa
npm run lint                      # eslint
```

`npm test` runs `docs/assemble-docs.test.sh` and nothing else: the documentation assembly is the
part of this module with behaviour worth pinning. **There is still no test for `src/`** — nothing in
SPC's Gradle build reaches this module either, so treat a change to a `.vue` file as verified only by
looking at it in `quasar dev`.

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
