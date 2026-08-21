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

> **UNVERIFIED: whether this file's `paths:` scoping actually saves anything.** The move was justified
> by a character count on disk, which answers "how big is this file" and not "does this load".
> [claude-code#16299](https://github.com/anthropics/claude-code/issues/16299) — path-scoped rules in
> `.claude/rules/` loading globally regardless of `paths:` — is **open**, with a repro and no maintainer
> response. If that is still live, this file loads every session anyway and the split bought nothing.
> **Run `/memory` in a fresh session to settle it.** The correctness risk is the smaller one: the two
> known bugs make path-scoped rules load *globally* (#16299) or *never* but only under `~/.claude/rules/`
> ([#22170](https://github.com/anthropics/claude-code/issues/22170)) — these are project-level, which is
> that issue's documented workaround, so the landmines below are not at risk of silently vanishing.

**CI lives in `.forgejo/workflows`. Forgejo (`git.griefed.de`) is the canonical CI and the origin of
every release.** `.gitlab-ci.yml` is gone. **LANDMINE:** `.forgejo/workflows` is *all-or-nothing* — once
it exists, Forgejo ignores `.github/workflows` entirely
([forgejo#9203](https://codeberg.org/forgejo/forgejo/issues/9203)), so anything Forgejo must do belongs
there and nowhere else. `.github/workflows` keeps a **smoke test** plus the four
`clientside-*` workflows, which are GitHub-native (three `issues:`-triggered, one `workflow_call:`
helper); releases are created on Forgejo and mirrored outward
by `release-build.yml`'s `mirror` job, because Forgejo push-mirrors replicate refs but **not** releases.
Two GitLab capabilities were **deliberately not carried over**: `Build Release` uploaded the app jar to
GitLab's *generic package registry* and then created a release asset *link* to it (Forgejo attaches
assets to the release directly, so a consumer with a hard-coded `/packages/generic/...` URL loses it),
and `release_job` created a release whose description merely linked changelogs on three forges (the
Forgejo release now carries the changelog section itself).
