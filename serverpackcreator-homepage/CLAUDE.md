# serverpackcreator-homepage — Claude Code context

The Quasar/Vue single-page site served at **serverpackcreator.de**, plus the versioned
documentation tree it hosts under `/help/<version>/` and `/api/<version>/`.

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

## Where each fact already lives — read it there, don't restate it here

- **How the image is built and why it fails without docs** → `Dockerfile`. Two stages: a Node
  toolchain builds the SPA, an LSIO nginx serves it from `/config/www`, and a final `RUN` fails the
  build outright if `help/latest/config.json` or `api/latest/index.html` is missing.
- **How the versioned documentation tree is assembled** → `docs/assemble-docs.sh`, which is a
  script rather than Dockerfile steps so it can be run against a scratch directory and the result
  clicked. It must run *before* `docker build`; the Dockerfile only `COPY`s `docs-build/`.
- **Which SPC versions the site serves docs for, and what a version costs** → `docs/versions.json`.
  A version listed there needs a published *release*, not merely a tag — its API reference is a
  release asset.
- **How CI wires the two together** → the `homepage-build` and `homepage-image` jobs in
  `.forgejo/workflows/docs.yml`.

## Local development

```
npm install && npx quasar dev     # dev server
npx quasar build                  # production build into dist/spa
npm run lint                      # eslint
```

`npm test` is a stub that echoes and exits 0 — there is no test suite here, and nothing in SPC's
build runs one for this module. Treat a change to `src/` as verified only by looking at it.
