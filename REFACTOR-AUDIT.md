# Refactor Audit — `claude-audit-backlog-cleanup` (`develop..HEAD`)

**Range audited:** `develop..HEAD` — **22 commits**, `fce7e610a` … `2d9dfe62d`
**Base:** `develop` — verified ancestor, clean linear range
**Mode:** READ-ONLY. No source modified.
**Suite at HEAD:** api 272 (1 skip) · clientside 87 · grinder 224 (19 skip) · app 76 — all green on `--rerun-tasks`

> Fourth report at this path. Supersedes the 102-commit audit of unpushed `develop` (1 HIGH / 2 MEDIUM / 0 LOW,
> L-C withdrawn), whose text is in git at `f1368b30d`. Its findings are resolved or carried forward below.
>
> **This audit's subject is the work that closed the previous audit's findings** — so it is deliberately harder on
> itself than on inherited history. Two of the four MEDIUMs below are about rules written *on this branch*.

**Findings: 0 HIGH · 4 MEDIUM · 3 LOW.**

**Status 2026-07-31 — remediated on this branch.** M-2 (`bb6115ade`), M-3 (`c8a8f7e66`), M-4 (`4767786c9`), L-1 and
L-3 (`8693ff3c6`) are **closed**; M-1 and L-2 are historical and not remediable without rewriting
landed history. Fixing M-3 additionally uncovered a live bug — three build scripts were rewriting the **shared**
`ServerPackCreator` Preferences node, relocating the developer's own GUI home into the repository on every `test` or
`clean` — fixed in `4f53aa889`, which also answers the open question `grinder/CLAUDE.md` had recorded and explains
B20's disappearing metadata (recorded as B24).

The structural discipline held. Every one of the six code changes landed as a **red `test(...)` commit followed by
its fix** — the M-B rule this branch introduced, applied to itself: `203a32534`→`f6c23e972`,
`b00ba1ea8`→`bee9e3187`, `3fe798fd7`→`b6b778b82`→`b6ed10227`, `4c941c7a5`→`32bfc2ae3`,
`9a797824a`→`5aa12dbfc`, `cd07859bb`→`2a2a2df10`. Each red state is recorded in its commit message with the actual
failure text. No new `!!`, no stray debug, no module-boundary violation, and **no HIGH** — every `refactor:` label
was checked against its diff rather than taken on trust.

---

## MEDIUM

### M-1 — `bee9e3187` bundles three concerns across two modules
**Commit:** `bee9e3187` *"fix(grinder,api): tell a metadata gap apart from an unsupported Minecraft version"*
**Files:** `grinder/loader/ImageJavaRuntimes.kt` (new enum + `supportFor`), `api/.../MinecraftServer.kt` (logging),
`grinder/.../ScriptTemplateMatrixIT.kt` (consumer change **and** a default-dimension change)
**Rule broken:** *One concern per commit.*

Three separable changes in one commit:

| Concern | Where | Could have shipped alone? |
|---|---|---|
| Tri-state support API + its consumer | `ImageJavaRuntimes.kt`, `ScriptTemplateMatrixIT.kt:173-198` | yes — this is what the red pin covered |
| Logging the swallowed metadata failure | `MinecraftServer.kt:79`, `:116` (`-api`) | yes — different module, different defect, no test |
| `26.2` added to the IT's default Minecraft axis | `ScriptTemplateMatrixIT.kt:77` | yes — pure test-scope coverage change |

The third is the clearest: putting a new Minecraft version into the default axis is a coverage decision with nothing
to do with the tri-state, and it is the one change here that alters what a *future* run does by default. The second
is in a different module and carries no test at all (see **M-2**). The red pin (`b00ba1ea8`) covered only the first,
so two of the three arrived unpinned inside a commit whose message claims the guard turned green.

### M-2 — `bee9e3187` adds embedder-visible logging to an exported `-api` path, uncached and unrecorded
**Commit:** `bee9e3187`
**File:** `serverpackcreator-api/src/main/kotlin/.../versionmeta/minecraft/MinecraftServer.kt:79`, `:116`
**Rule broken:** *If you find a bug while refactoring, surface it explicitly* — and the project's own policy that
behaviour-visible changes to exported calls belong in the compatibility table (`CLAUDE.md:87-95`).

The change is right in intent: a swallowed exception became indistinguishable from "declares no required Java", and
that is what produced the benign `[N/A] SKIPPED`. But three properties of the surrounding code were not considered:

- **The failure is never cached.** `setServerJson()` re-downloads whenever `manifestFile` is absent, and on failure
  `serverJson` stays `null`, so every subsequent call retries.
- **One lookup costs two attempts.** `MinecraftMeta.getServer` (`:166`) evaluates `server.url().isPresent &&
  server.javaVersion().isPresent`, and both call `setServerJson()`. So a single `requiredJavaVersion` on a broken
  version now emits **up to two `log.warn` calls with full stack traces**.
- **That path is hot.** `ImageJavaRuntimes.requiredJavaMajor` is reached from `supportFor`, `javaPath` *and*
  `installerJavaPathFor` — per candidate in `ContainerCandidateVerifier`, per cell in the matrix IT, and from the
  GUI on version selection (`ConfigEditor.kt:681`). A sweep hitting one unfetchable manifest can therefore log the
  same warning, with a stack trace, once per candidate.

Same *class* as the previous audit's **H-B** (embedder-visible behaviour changed on an exported call, not recorded in
the compatibility table) at lower severity, since nothing a caller *returns* changed — only log volume. That it
recurred one commit after H-B was closed is the point worth recording. **Options:** log at `debug`, drop the stack
trace, or cache the failure per version so the retry storm goes with it.

### M-3 — `8f1b3a76f` changes ignore behaviour with no automated guard, and the repo has precedent for one
**Commit:** `8f1b3a76f` *"fix: stop .gitignore hiding the shipped server_files resources"*
**File:** `.gitignore:365-371`
**Rule broken:** *Ensure characterization tests exist … never refactor untested code blind.*

Verified by hand — thoroughly, including the `--no-index` correction that showed the first reading was wrong — but
nothing pins it. The failure mode is silent and exactly the one being fixed: someone re-broadens the rule, a shipped
resource stops being tracked, and it surfaces as a file missing from a release. The repo already does this kind of
config pinning (`ReadmeConfigurationTest` pins the documented env vars), so a test asserting `git check-ignore
--no-index` on the shipped resource path **and** on `<module>/tests/server_files` is both feasible and consistent
with existing practice.

### M-4 — the `refactor:` rule written in `70cfe7fd9` is too absolute, and `b6b778b82` is the counter-example
**Commits:** `70cfe7fd9` (the rule), `b6b778b82` (the commit it misjudges)
**File:** `CLAUDE.md` — *"If an **existing** test has to change, the label is already wrong — that is the
stop-and-flag signal, not a formality."*

`b6b778b82` is labelled `refactor:` and modifies an existing test, which by that rule's letter makes it mislabelled.
It is not. The whole diff to `SuspendGapTest.kt` is the receiver symbol moving module:

```
- DockerJavaContainerEngine.isSuspendGap(gap, poll),
+ SuspendAwareDeadline.isSuspendGap(gap, poll),
```

Every assertion, argument, and message is byte-identical, and the production change was verified line by line as
behaviour-preserving (`!deadline.hasTimeLeft()` ≡ `System.currentTimeMillis() >= deadline`, same initial
`lastTick`). A Strangler-Fig move across a module boundary *necessarily* updates references, tests included — so as
written the rule forbids the very refactor the conventions ask for, and the honest response to it would be to
mislabel a clean move as `fix:`.

**The rule needs the carve-out it lacks:** a reference-only update (imports, receivers, renames) with unchanged
assertions is not the signal; a changed *assertion, argument or expected value* is. Worth fixing in `CLAUDE.md`
before it misfires on someone else — a convention that cries wolf gets ignored wholesale.

---

## LOW

### L-1 — `escapeForProperties` is public in a precompiled script plugin
**Commit:** `2a2a2df10` · **File:** `buildSrc/src/main/kotlin/serverpackcreator.java-conventions.gradle.kts` (last line)

Declared as a top-level `fun`, so it enters the scope of every build script applying this convention plugin — a
name nobody outside the file needs. `private fun` keeps it local. Also sits after the `signing` block at the very
end, physically distant from its only caller in `processTestResources`.

### L-2 — `cd07859bb`'s pin was only one-third red at commit time
**Commit:** `cd07859bb` · **File:** `serverpackcreator-api/src/test/kotlin/.../TestPropertiesTest.kt`

Of its three guards, only `theCommittedTestPropertiesNameNoHost` failed. The other two passed **because this is the
machine whose paths were committed** — they only become load-bearing once the committed values are blanked. It was
disclosed in the commit message at the time, which is why this is LOW rather than a missing-pin finding, but a guard
that passes for an accidental reason is not yet evidence of anything.

### L-3 — `TestPropertiesTest` depends on the working directory being the module root
**Commit:** `cd07859bb` · **File:** same, the `committed` / `generated` fields

Both paths are relative (`src/test/resources/…`, `build/resources/test/…`). Correct under Gradle, whose test
working directory is the project directory, but a runner that starts from the repository root — some IDE
configurations do — fails the test on a path that does not exist rather than on the property it checks. Resolving
against a system property the build already injects would remove the assumption.

---

## Carried forward

| Finding | Severity | Status at this HEAD |
|---|---|---|
| **H-A** `origin/develop` published with a failing `ConfigEditorViewModelTest` | HIGH | **STILL OPEN, and deeper: 102 + 22 = 124 commits now sit behind the unpushed fix `34464832e`.** The only item across four audits that cannot be closed locally. |
| H-B `variables.txt` contract change untabled | HIGH | **Closed** (`70cfe7fd9`). See **M-2** — the same class recurred immediately, milder. |
| M-B tests bundled with production changes | MEDIUM | **Closed** as a rule and **followed** by all six changes here. |
| M-C `refactor:` used for behaviour changes | MEDIUM | **Closed** as a rule; see **M-4**, the rule as written over-fires. |
| L-C `var` in `PathsConfig` | LOW | **Withdrawn** (`b9777ddf9`); inverse defect recorded as backlog B21. |
| H1, M3, M4, L-A, L-B (landed history) | — | Historical; rewriting would be worse than the finding. |

---

## What this branch got right

- **The red pin is real, not ceremonial.** Each `test(...)` commit records the observed failure verbatim — e.g.
  *"on Java 24 SSJ cannot trap the Forge installer's System.exit … expected: `<false>` but was: `<true>`"*. Two
  guards were additionally verified by *inverting the fix* and watching the count fall (`ImageJavaRuntimes`,
  `LoaderCache`), which is the check that silently passed twice in the previous session.
- **A finding was withdrawn rather than defended.** `b9777ddf9` retracts L-C after reading the whole declaration:
  the setter was `private` and the `var` load-bearing. The inverse defect it pointed at became B21.
- **Dependency direction drove a design decision.** `SuspendAwareDeadline` went into `-clientside`, not the grinder,
  because grinder depends on clientside — and the shared arithmetic is now injectable-clock testable where both
  callers are integration-shaped.
- **An expensive default was chosen on evidence.** B15 tolerates absent provenance instead of invalidating, because
  Phase 2's cached Forge tuples were *checked* and found still bootable — the alternative would have re-installed 74
  tuples for nothing.
- **Two measurement artifacts were caught before they became conclusions:** `git check-ignore` masking tracked
  paths without `--no-index`, and `ImageJavaRuntimes`' own "before" reading being suppressed the same way.

---

## Recommended order of action

1. **Push.** **H-A** is four audits old and now 124 commits deep.
2. **M-4 — fix the `refactor:` rule's wording** before it misfires: exempt reference-only updates, keep the signal
   on changed assertions. Cheapest item here and it protects the rule's credibility.
3. **M-2 — decide the logging volume** (`debug`, no stack trace, or cache the failure) and table the change if it
   stays at `warn`.
4. **M-3 — pin the `.gitignore` behaviour** alongside `ReadmeConfigurationTest`.
5. **L-1 → `private`**, then L-2/L-3 as tidy-ups if the test is touched again.

**M-1 is not remediable** without rewriting landed history; recorded so the next change to `MinecraftServer` knows
its logging arrived unpinned.

---

**Report only — no source modified. Awaiting go-ahead.**
