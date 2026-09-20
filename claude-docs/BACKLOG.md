# Backlog — deferred, agreed-to work

Items consciously deferred, with the reason and enough context to pick them up cold. Newest section first.
Not a wish-list: everything here was looked at, judged worth doing, and postponed for a stated reason.
When an item lands, delete it here and record it in `REFACTOR-LOG.md`.

> **Numbering continues from the highest ID ever issued — it does not restart, and an ID is never reused.**
> A gap in this file means an item landed or was dropped, not that the counter reset; most IDs ever issued have
> no entry here any more, and some are still cited by name elsewhere in the repo, so reusing one silently
> repoints someone else's citation at the wrong item. `git log -S'B<n> —' -- claude-docs/BACKLOG.md` recovers
> what any past ID meant, and is also how to find the highest one rather than trusting a number written here.

Add the next item under a dated section, starting at **B40**, with the reason it waited and enough context to pick
it up cold. **B36 is issued and gone** — Sinytra Connector as a boot strategy, dropped 2026-09-12 when the
per-line axis made the shim cost a whole Minecraft line and the placeholder was redirected to Fabric instead;
see `REFACTOR-LOG.md`.

## 2026-09-20 — from the Qodana run-817 triage

Both items need a Qodana run to confirm, and none was possible on 2026-09-20: Docker Desktop's VM is
capped at 2 GB on the development machine and the JVM linter is OOM-killed (exit 137) during the Gradle
import. CI runs the scan on every push, so both are verifiable there the moment they land.

### B38 — teach `qodana.yaml` the verdicts, and give the scan a baseline

**What:** encode the standing "not a finding here" decisions in `qodana.yaml` so the tool stops
reporting them, and add a baseline so a run reports what is *new* rather than everything. Run 817
reported 49 problems of which **12 were triaged won't-fix**, and every one of those verdicts currently
lives in a chat log:

| Rule | Count | Where | Verdict |
|---|---|---|---|
| `UnusedSymbol` | 2 | `web/index/IndexStore.kt`, `web/migration/MigrationStore.kt` | False positive. `MongoIndexStore` and `MongoMigrationStore` are `@Component` beans injected by interface; QDJVM **Community** has no Spring plugin and cannot see the wiring. `WebServiceContextTest` asserts both resolve from the context. |
| `UnstableApiUsage` | 4 | `settings.gradle.kts:11-12` | Won't fix. `dependencyResolutionManagement { repositoriesMode = FAIL_ON_PROJECT_REPOS }` is Gradle `@Incubating` with no stable alternative. |
| `ConvertLongToDuration` | 1 | `VersionMeta.awaitManifestRefresh` | **Must not fix.** `-api` is published to Maven Central; changing the parameter type breaks source compatibility for embedders for a cosmetic gain. |
| `RedundantIf` | 5 | `ServerPackUpdater`, `ConfigEditor`, `BootVerifier`, `VersionConstraint`, `BootLogStore` | Won't fix. Each is an early-return guard carrying a comment that explains it; collapsing to `return !cond && expr` makes five explained predicates worse. |

The remaining 16 are style noise (`ConvertCallChainIntoSequence`, `DestructuringDeclaration`,
`RemoveExplicitTypeArguments`, `UnnecessaryVariable`, `RemoveCurlyBracesFromTemplate`, …) and are the
case for the baseline rather than for per-rule configuration.

**Why it waited.** A `qodana.yaml` change that cannot be re-scanned is a guess: the exclusion syntax is
per-rule-and-path and an over-broad entry silences a rule everywhere, which is worse than the noise it
removes. Writing prose about the verdicts and *then* letting the tool keep reporting them is the
failure this entry exists to prevent, so the table above is the interim record, not the fix.

**Pick it up when** a Qodana run can be observed — the next CI run will do. Confirm each exclusion by
the count dropping by exactly the number in the table, not by the total going down.

### B39 — run the code generation before the scan, or keep reading a report that cannot see `-app`

**What:** `.forgejo/workflows/qodana.yml` runs `qodana scan` against a raw checkout. i18n4k generates
the `Translations` object at build time, so it does not exist during analysis: run 817 carries **35
sanity failures** — 30 × `Unresolved reference Translations`, 5 × `Unresolved reference Example` — plus
6 unresolved `kaptGeneratedClasses` roots in both plugin modules. **85 source files reference
`Translations`, 79 of them in `-app`.** A file with an unresolved core symbol is analysed with
inspections degraded, so "3 findings in `-app`" is not evidence that `-app` is clean — it is evidence
that the scanner could not read it.

**Why it waited.** The fix is a Gradle invocation before the scan step (`generateI18n4kFiles` exists and
runs in `-api`; the plugin modules need their `kapt` outputs too), which adds a full dependency
resolution to a job that currently needs none — worth it only if it actually moves the sanity-failure
count, and that cannot be measured from here.

**Pick it up when** you next touch that workflow. The measurement is the point: record the sanity-failure
count and the total problem count before and after, in the commit message, per the build-logic rule in
the root `CLAUDE.md`. If the count does not move, revert it rather than keeping a slower job.

## 2026-09-11 — from the UNVERIFIABLE pass

### B37 — search-then-confirm for a mod id no registry resolves

**What:** when a required manifest id resolves nowhere, search Modrinth's `/v2/search` faceted by loader and
game version, download the best candidate, read its **own** mod id, and accept it only if it matches —
feeding `LearnedModIds` so the cost amortises across candidates.

**Why it waited.** Three cheaper routes landed first and cover the measured rows: the fork table
(`create` → `create-fabric`, `tacz` → `timeless-and-classics-guns`), the cross-platform fallback, and
`askLinkedProjects`, which already does "probe, learn, re-plan" for an id the *page* links. What is left for
a search is an id that is declared by a jar, linked by nobody, absent from the registry and absent from both
platforms' slug namespaces — a set this pass produced no instance of. Building the machinery for it now
would be a guess at demand, and a name-only match that skipped the confirm step would stage the wrong mod.

**Pick it up when** a refusal names an id that none of the four routes above reach. The confirm step is not
optional: `askLinkedProjects` is the template — never trust a name, download and read the id.
