---
description: How CI is wired on Forgejo, and the all-or-nothing landmine that governs .forgejo/workflows
paths:
  - ".forgejo/**"
  - ".github/**"
  - ".releaserc.yml"
---

# CI / release pipeline

Moved out of the root `CLAUDE.md` on 2026-08-21 so it loads when you touch a workflow rather than in
every session. Operator-facing secret detail lives in `claude-docs/CI-SECRETS.md`.

**CI lives in `.forgejo/workflows`. Forgejo (`git.griefed.de`) is the canonical CI and the origin of
every release.** `.gitlab-ci.yml` is gone. **LANDMINE:** `.forgejo/workflows` is *all-or-nothing* — once
it exists, Forgejo ignores `.github/workflows` entirely
([forgejo#9203](https://codeberg.org/forgejo/forgejo/issues/9203)), so anything Forgejo must do belongs
there and nowhere else. `.github/workflows` keeps a **smoke test** plus the four issue-driven
`clientside-*` workflows, which are GitHub-native; releases are created on Forgejo and mirrored outward
by `release-build.yml`'s `mirror` job, because Forgejo push-mirrors replicate refs but **not** releases.
Two GitLab capabilities were **deliberately not carried over**: `Build Release` uploaded the app jar to
GitLab's *generic package registry* and then created a release asset *link* to it (Forgejo attaches
assets to the release directly, so a consumer with a hard-coded `/packages/generic/...` URL loses it),
and `release_job` created a release whose description merely linked changelogs on three forges (the
Forgejo release now carries the changelog section itself).
