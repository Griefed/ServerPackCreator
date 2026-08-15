# Refactor audit — `claude-build-docs`

**Scope:** `git log develop..HEAD` — **1 commit**, `fd8d674be`.
**Mode:** READ-ONLY. No source was modified while auditing.
**Supersedes** the previous audit in this file (`claude-webservice-context-test` +
`claude-config-cache`). That one's M-1 — a commit bundling three concerns — was acted on: the commit
was split into three, one part was dropped entirely after measurement disproved its rationale, and all
of it is now merged into `develop`.

**Verdict: no HIGH, no MEDIUM, two LOW.** This is a single documentation commit that touches no
source. Most of these conventions are written for code changes and simply do not apply; rather than
stretch them to produce findings, this report says which ones were checked and what the two real
observations are.

---

## HIGH

None. The commit modifies three Markdown files and nothing else — verified, no `.kt`, `.kts`,
`.properties` or `.toml` in the diff. No behaviour, no module boundary, no plugin API is involved.

## MEDIUM

None.

The characterization-test rule has no purchase on a docs commit, but its *spirit* — do not assert
what you have not verified — is the one that matters here, and it was honoured. Every checkable claim
in `BUILD.md` was run rather than recalled, which is what turned up the three errors the commit fixes
(the missing `./gradlew`, the non-existent `Build All` task, and `:serverpackcreator-app:run`). Spot-
checks during this audit:

| Claim | Result |
|---|---|
| `build` runs the frontend Vitest suite | `checkScript.set("run test")` present in quasar-conventions ✓ |
| configuration time ~4.75s → ~2.02s | re-measured 4.96s → 2.04s ✓ (within noise) |
| `BUILD.md` is not in the shipped document set | 0 mentions in `-api`'s build file ✓ |
| `bootRun` exists, `run` does not, for `-app` | verified against the task graph ✓ |
| foojay resolver absent from root settings | verified ✓ |

---

## LOW

### L-1 · `fd8d674be` fixes a documentation bug found mid-task, in the same commit rather than its own

**File:** `CLAUDE.md:72-75`
**Rule:** *If you find a bug while refactoring, surface it explicitly and propose a fix in its own
commit.*

While verifying `BUILD.md`'s claims, `./gradlew :serverpackcreator-app:run` turned out not to exist —
`-app` applies the Spring Boot plugin, so the task is `bootRun`. The root `CLAUDE.md` carried the same
wrong command and is corrected in this commit rather than a separate one.

It *is* surfaced explicitly — the commit body names it as one of three errors the verification caught,
and the correction adds the reason (`-app` is not an `application` module) plus the contrast with
`:serverpackcreator-grinder:run`, which does exist. So the "do not silently work around it" half of
the rule is satisfied; only the "own commit" half is not.

Defensible as one concern — *the build documentation was wrong in three places, here are the three*.
Recorded because the rule is written without that exception, and because the fix lands in a file that
is not otherwise the subject of the commit.

### L-2 · `fd8d674be` leaves a stated prerequisite gap unresolved by design

**File:** `BUILD.md:49-53`

The commit documents that the foojay toolchain resolver is applied in `buildSrc/settings.gradle.kts`
but not in the root, so a contributor without a local JDK 21 gets *"No matching toolchains found"*
instead of an automatic download — and then explicitly declines to fix it, on the grounds that
changing toolchain provisioning does not belong in a docs commit.

That is the right call under *one concern per commit*, and the trap is now written down where a
newcomer will hit it. Flagged only so it does not disappear: **documenting a papercut is not the same
as fixing it**, and the fix is one line in `settings.gradle.kts`. It should become a follow-up rather
than remain permanently "documented".

---

## Checked and clean

- **No source modified**: the diff is `BUILD.md` (new, 229 lines), `CONTRIBUTING.md` (+14/-6) and
  `CLAUDE.md` (+6/-6).
- **No test was touched or needed to change**, so the "a refactor that changes a test is not a
  refactor" rule is trivially satisfied.
- **Claims were verified rather than recalled**, and the verification is what produced the commit's
  content — three documented errors, each named in the commit body with how it was found.
- **The shipping decision is correct and was checked, not assumed.** `CONTRIBUTING.md` is one of the
  seven documents copied into `-api`'s resources and mirrored into the Writerside topics; `BUILD.md`
  is not, so the link between them is an absolute URL rather than a relative path that would dangle in
  the shipped copies. Confirmed `BUILD.md` appears nowhere in `-api`'s `shippedDocuments` list.
- **`BUILD.md`'s own links resolve**: no broken heading anchors, no broken file links. (An earlier
  draft had two broken anchors; they were caught and fixed before the commit.)
- **Scope did not sprawl.** Three files, all documentation, all about how to build the project.
- **Branch follows the `claude-` naming rule** and has not been pushed.
- `./gradlew build` green.

## Follow-ups, not defects in this range

- **Add the foojay resolver to the root `settings.gradle.kts`** and delete the trap from `BUILD.md`
  (L-2). One line; removes the JDK-21 prerequisite entirely.
- **`serverpackcreator-plugin-example/src/main/resources/CHANGELOG.md`** is a tracked 14 KB file that
  nothing generates and nothing updates, shipped inside the example plugin's jar, whose source file at
  the module root does not exist. Surfaced during the previous branch and deliberately left alone —
  deleting a tracked file that reaches users is a product decision, not a build cleanup.
- The configuration cache remains opt-in (`--configuration-cache`), blocked for `build` by the
  third-party `:generateLicenseReport`. Documented in `BUILD.md` and `CLAUDE.md`.
