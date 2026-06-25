# GitHub Actions workflow audit (2026-06-25)

**Scope:** the six pre-existing workflows in `.github/workflows/`, **excluding** the newly added
`clientside-*` workflows: `devbuild.yml`, `github-prerelease.yml`, `github_release.yml`, `test.yml`,
`update_readme.yml`, `virustotal.yml`. Read-only audit — **no workflow was modified**.

Audited for: supply-chain exposure (action pinning, untrusted triggers), `GITHUB_TOKEN` least
privilege, secret handling, script-injection, correctness, and deprecations. Severity:
**HIGH** = secret/credential exposure or a broken/dangerous release path · **MEDIUM** = real
correctness bug, over-broad privilege, or risky pattern · **LOW** = hardening / deprecation / hygiene.

> One workflow already does it right: **`virustotal.yml`** sets a top-level `permissions: contents: read`
> and elevates to `contents: write` only on the job that needs it. It's the model the others should follow.

---

## Remediation status (2026-06-25, branch `claude-workflow-audit`)

**Fixed:** H1, H2, H3, M1, M2, M4, M5, M6, L1 (third-party), L2, L3. **Accepted (not changed):** M3.

- **H1** — `tj-actions/branch-names` removed from all three files; version now comes from
  `${{ github.ref_name }}` (the pushed tag/branch). The `preparations` jobs no longer checkout.
- **H2** — every workflow now has a top-level `permissions: contents: read`; jobs that create
  releases/tags/pages elevate to `contents: write` (`devbuild`→`continuous`, both release files’
  `release`/`prerelease`, `github_release`→`pages`). `virustotal` was already correct.
- **H3 / L1** — **all third-party actions SHA-pinned** (with a `# vX` comment), including the four
  `nogsantos/scp-deploy@master` → `…@48b9ca0 # v1.3.0` and `actions/checkout@master` → `@v6`.
  First-party `actions/*` and `gradle/*` deliberately kept on major-version tags (accepted norm;
  SHA-pinning them is churn with little security gain).
- **M1** — `github_release.yml` `pages` job given `needs: [preparations]` (was building with
  `-Pversion=""`).
- **M2** — removed the stray `${{ steps.preinfo.outputs.content }}` reference from `github_release.yml`’s
  Append Info step (that job has no `preinfo` step).
- **M4** — both `::set-output` usages replaced with `>> "$GITHUB_OUTPUT"`.
- **M5** — `update_readme.yml`: GitLab token moved out of the push URL into an `http.extraheader`
  Authorization header, all secrets passed via `env:`, and the needless `apt-get install git` removed.
- **M6** — the `curl | execute` of `discord.sh@master` is now pinned to commit `f274ed6` and
  **checksum-verified** (`sha256sum -c`) before execution, in both release files.
- **L2** — `devbuild.yml` "Determine version" passes the dispatch input via `env:` instead of
  interpolating `${{ inputs.version }}` into the shell.
- **L3** — `test.yml` gained `concurrency` (`cancel-in-progress`) and dropped tj-actions.
- **M3 — ACCEPTED (not changed).** The `if: ${{ always() }}` on the release/prerelease jobs is
  **intentional** (per maintainer): the GitHub release with source archives + notes is the priority, and
  assets can be attached even if a build/upload step hiccups. Left as-is, with a clarifying comment
  added in the workflow.

> **Untested by nature:** these workflows only run on tag-push / release / dispatch, so the changes
> can't be exercised locally — only YAML-validated. The release/deploy paths (install4j media, scp
> deploy, GitLab push, Discord webhook) should be watched on the next real run.

---

## HIGH

### H1 — `tj-actions/branch-names` (compromised publisher), pinned to a mutable tag
`test.yml:17`, `github-prerelease.yml:28`, `github_release.yml:26` — all `tj-actions/branch-names@v9.0.2`.

`tj-actions` was the subject of a **major supply-chain compromise in March 2025** (CVE-2025-30066):
`tj-actions/changed-files` tags were retroactively repointed to a malicious commit that dumped runner
memory — including secrets — into the build logs. `branch-names` is the **same publisher**. Two
problems compound: (a) trusting that publisher at all, and (b) pinning to a **tag** (`@v9.0.2`), which
a compromised maintainer can move to point at new code that runs with whatever secrets the job holds.
In the release workflows that job sits next to `GITLAB_TOKEN`, `SPCUPLOAD_KEY`, `INSTALL4J_LICENSE`.

**Fix:** the action is only used to get the branch/tag name — replace with native context, no action
needed: tag → `${GITHUB_REF#refs/tags/}` (or `${{ github.ref_name }}`), branch → `${{ github.ref_name }}`.
`test.yml`'s `-Pversion="${{ steps.branch-name.outputs.current_branch }}"` becomes
`-Pversion="${{ github.ref_name }}"`.

### H2 — No `permissions:` block in 5 of 6 workflows → over-privileged `GITHUB_TOKEN`
Missing in `devbuild.yml`, `github-prerelease.yml`, `github_release.yml`, `test.yml`, `update_readme.yml`
(present only in `virustotal.yml`).

With no `permissions:` declared, every job runs with the **repository/org default** token scope. If
that default is read-write, the token handed to *every* third-party action in these files (several of
them unpinned — see H3) can push code, move tags, edit releases, etc. If the default is read-only
(GitHub's newer default), the release/tag steps (`ncipollo/release-action`, `richardsimko/update-tag`,
`softprops/action-gh-release`) will instead **silently lack `contents: write`** and fail. Either way,
permissions must be explicit.

**Fix:** top-level `permissions: contents: read` in each file; elevate per-job to `contents: write`
only where a release/tag is created. Mirror `virustotal.yml`.

### H3 — Third-party actions pinned to `@master` (and low-popularity actions handling deploy keys)
`nogsantos/scp-deploy@master` — `devbuild.yml:349`, `github_release.yml:199` & `:217`,
`github-prerelease.yml:207`. `actions/checkout@master` — `github_release.yml:262`.

`@master` runs whatever is at that branch's HEAD **at run time** — the maintainer (or anyone who
compromises that repo) can change what executes, with no review on your side. `scp-deploy` is the worst
case: it is handed `secrets.SPCUPLOAD_KEY` (your deploy SSH private key) and `SPCUPLOAD_HOST/TARGET`, so
a malicious update gets your server credentials. `nogsantos/scp-deploy` and `tiyee/action-ssh`
(`@v1.0.1`) are also low-popularity third-party actions in the secret-handling path.

**Fix:** pin **every** third-party action to a full commit SHA (not a tag/branch). Most urgent: the four
`scp-deploy@master` and `checkout@master`. Consider replacing `scp-deploy`/`action-ssh` with a pinned
`appleboy/scp-action`/`ssh-action` or a plain `rsync`/`ssh` step using the key from a step `env:`.

---

## MEDIUM

### M1 — `github_release.yml` `pages` job is missing `needs:` → builds with an empty version
`github_release.yml:257` (`pages:` job) has no `needs:`, yet line 276 uses
`-Pversion="${{ needs.preparations.outputs.version }}"`. Without `needs: [preparations]`, the `needs`
context is empty, so Dokka builds with `-Pversion=""`, **and** the job runs in parallel with
`preparations` instead of after it. (The `devbuild`/`prerelease` jobs wire `needs` correctly.)
**Fix:** add `needs: [preparations]` (and likely `release`) to the `pages` job.

### M2 — `github_release.yml` "Append Info" references a non-existent step
`github_release.yml:175` interpolates `${{ steps.preinfo.outputs.content }}`, but this file's `release`
job has no `preinfo` step (only `info`). The `preinfo` / `PRE-INFO.md` step exists only in
`github-prerelease.yml`. The reference resolves to empty — a copy-paste leftover that silently drops
content. **Fix:** remove the stray `preinfo` line (or add the step if PRE-INFO was intended here).

### M3 — Release aggregation jobs run with `if: ${{ always() }}` → can publish a broken release
`github_release.yml:122` and `github-prerelease.yml:124` (`release`/`prerelease` jobs) use
`if: ${{ always() }}` with `needs: [preparations, jar-and-media]`. `always()` makes the release proceed
**even if `jar-and-media` failed**; combined with `fail_on_unmatched_files: false`, a failed build can
still cut a GitHub release with missing/partial artifacts. **Fix:** gate on success —
`if: ${{ needs.jar-and-media.result == 'success' }}` (or `if: success()`), keeping `always()` only where
genuinely needed.

### M4 — Deprecated `::set-output` workflow command
`github_release.yml:233`, `github-prerelease.yml:223`: `echo "::set-output name=today::..."`. GitHub
**deprecated and disabled** `::set-output` (mid-2023); it now warns and is slated to stop working.
**Fix:** `echo "today=$(date -u +'%Y-%m-%dT%H:%M:%S')" >> "$GITHUB_OUTPUT"`.

### M5 — `update_readme.yml`: token embedded in the push URL + `curl | execute` patterns elsewhere
`update_readme.yml:61` pushes to the GitLab mirror via `https://${GIT_USER}:${GITLAB_TOKEN}@${CI_SERVER_HOST}/...`.
Interpolating a secret into a command string risks it surfacing in error traces (Actions masks known
secret *values*, but constructed URLs in stack traces are a known leak vector). It also `apt-get install
git` as root on a runner that already has git. No `permissions:` block, and it runs several unpinned
third-party actions (`release-kit/hash-files@v1`, `JamesIves/github-sponsors-readme-action@v1`,
`actions-cool/contributor-helper@v1.2.1`) that receive `GITHUB_TOKEN`. **Fix:** use a credential helper
or `actions/checkout` with a token + `git push` to a remote configured without inlining the secret;
drop the `apt-get install git`; add `permissions:` and SHA-pin.

### M6 — `news` jobs `curl | chmod +x | execute` a remote script
`github_release.yml:237`, `github-prerelease.yml:227`: download `discord.sh` from
`raw.githubusercontent.com/ChaoticWeg/discord.sh/master/...` and execute it. Running an unpinned remote
script (its `master`) is remote-code-execution-by-trust; blast radius is limited to the `news` job
(only `WEBHOOK_URL`), hence MEDIUM not HIGH. **Fix:** vendor the script into the repo (pinned), or pin
to a specific commit and verify a checksum.

---

## LOW

- **L1 — Floating major-tag pins everywhere** (`@v1`, `@v2`, `@v5`, `@v6`, `@v7`, `@v8`), including
  `actions/*`. Major-tag pinning of first-party `actions/*` is widely accepted, but for defense in
  depth SHA-pin all actions. (The worst offenders — `@master` — are H3.)
- **L2 — `devbuild.yml:39-46` interpolates `${{ inputs.version }}` straight into a `run:` shell.** Only
  write-access users can `workflow_dispatch`, so injection risk is low, but the pattern is the script-
  injection antipattern; pass via `env: VERSION: ${{ inputs.version }}` and use `"$VERSION"`.
- **L3 — `test.yml` runs on every `push` to any branch with no `concurrency`.** Rapid pushes stack full
  `./gradlew build` runs. Add `concurrency: { group: test-${{ github.ref }}, cancel-in-progress: true }`
  and consider path filters.
- **L4 — Low-popularity token-handling actions** (`richardsimko/update-tag`, `tiyee/action-ssh`,
  `release-kit/hash-files`, `actions-cool/contributor-helper`). Prefer well-maintained equivalents or
  the `gh` CLI, and SHA-pin regardless.

---

## Summary table

| ID | Severity | Where | One-line |
|----|----------|-------|----------|
| H1 | HIGH | test, github-prerelease, github_release | `tj-actions/branch-names` (compromised publisher), tag-pinned — replace with native context |
| H2 | HIGH | all except virustotal | no `permissions:` → over-privileged (or failing) `GITHUB_TOKEN` |
| H3 | HIGH | devbuild, both release files | `scp-deploy@master` / `checkout@master` handling deploy keys — SHA-pin |
| M1 | MEDIUM | github_release `pages` | missing `needs:` → `-Pversion=""` |
| M2 | MEDIUM | github_release `release` | references non-existent `steps.preinfo` |
| M3 | MEDIUM | both release files | `if: always()` can publish a broken release |
| M4 | MEDIUM | both release files | deprecated `::set-output` |
| M5 | MEDIUM | update_readme | token-in-URL push + unpinned actions, no permissions |
| M6 | MEDIUM | both release files | `curl \| execute` remote `discord.sh@master` |
| L1–L4 | LOW | various | tag-pinning, run-injection pattern, no test concurrency, low-popularity actions |

**Recommended order if remediating:** H2 (permissions) + H1/H3 (pinning) first — they're the credential
blast radius and are mechanical, low-risk edits. Then M1/M2/M3 (release correctness). M4–M6 and the
LOWs as hygiene. None require touching application code.
