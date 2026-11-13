# serverpackcreator-clientside — module context

> The **clientside-mod verification engine**, reused by the app's CLI verbs and by `serverpackcreator-grinder`.
> Package `de.griefed.serverpackcreator.clientside`. **Depends only on `serverpackcreator-api`** (plus
> jackson-module-kotlin) — it must never gain a dependency on `-app`, Spring, or Swing. Not published to Maven
> Central, so it can churn freely without compatibility lock-in.

> **History and evidence:** `claude-docs/CLIENTSIDE-HISTORY.md` holds the unabridged text of this file — the live
> cases, measurements and dated decisions behind each rule below. Read it when a rule's reason is unclear, or
> before re-opening a recorded decision.

## What it does

Given a Modrinth/CurseForge project-link: pick the platform, resolve the project's files, derive the
clientside-list file-name stem(s), and combine signals into one `Verdict` **per Minecraft version-line** (see
*The grind axis* below). Driven by the app's four CLI verbs (`-scan`, `-clientsidereport`, `-verifyclientside`,
`-clientsideapply`) and the `clientside-*.yml` workflows — see `serverpackcreator-app/CLAUDE.md` for the verb
wiring and CI.

## Package layout

Organized by **business feature**. The package graph is **acyclic** — that is the property to preserve, and the
one a careless import silently destroys.

| Package | Role | May depend on |
|---|---|---|
| *(root)* | API and composition: `ClientsideVerifier` (entry point), `ClientsideReportRenderer`, `ClientsideListEditor` | everything |
| `boot` | stage a pack, run a server, keep the evidence (incl. `PackVariables`/`ImageJavaRuntimes`, here so the CLI boots packs like the grinder) | all of the below |
| `verdict` | evidence → conclusion | `platform` |
| `selection` | which builds get ground | `platform`, `version` |
| `platform` | Modrinth/CurseForge, HTTP, shared model (`ModFile`, `ProjectFiles`, …) | `metadata` |
| `metadata` | what a jar says about itself | `version` |
| `dependency` | `DependencyAttribution`, `DependencyBacktrack` | `version` |
| `version` | version and loader-compatibility arithmetic — the one cross-cutting package | nothing |

- **Some imports exist only so a KDoc `[Link]` resolves** and create no bytecode dependency. Re-derive the real
  graph from *code* references, not from the import list.
- **LANDMINE — `platform.textOrNull(field)` and `BundledJars.nonBlankStringOrNull()` are NOT duplicates. Do not
  merge them; both directions fail tests.** Keeping them separate is **Griefed's call, 2026-10-18** — treat it
  as settled. `textOrNull` must stringify non-strings (CurseForge serves `modId` as a number); the bundled one
  must refuse anything not a non-blank string (a non-string Fabric/Quilt descriptor field is not an id or a
  range). One shared name meaning two predicates is the defect; the distinct names keep either from being
  reached by accident, and keep `metadata` → `platform` out of the graph.
- **Tests mirror the package of their subject only where the subject is unambiguous.** Behaviour tests spanning
  several features stay beside `ClientsideVerifier` in the root test package, which is what they drive.

## The grind axis is the Minecraft version-line

`ClientsideVerifier.report` asks `BootCandidateSelector.pickGrindTargets` for one `GrindTarget` per Minecraft
version-line `MinecraftLinePolicy` selects, each under the first of `LOADER_PRIORITY`
(`NeoForge, Forge, Fabric, Quilt, LegacyFabric`) that line publishes a bootable build for. **Why:** sideness is a
property of a *build*; a per-loader axis re-asks the same era and never boots old, separate codebases.

- **`pickGrindTargets` runs the priority order twice, and the second pass is load-bearing.** An untagged file
  matches *every* loader, so one pass would hand it to NeoForge while Forge had a tagged file — a jar the loader
  ignores, which can boot cleanly and publish a false `CLEAR`. Hence `pickBootableCandidate`'s
  `untaggedFallback`: off in pass one, on in pass two.
- **A file's *versions* are narrowed to the line, not just its files** — files are tagged across lines, and
  handing the whole set lets a 1.20 row boot at 1.21.
- **`targetDisprovingTheCrash` asks for another *row*, not another loader** (`other !== verdict`): two rows may
  share a loader and differ by era.
- **`suggestedEntry` is deliberately the stem over the *loader's whole history*.** `/as-properties` matches it
  with `startsWith`; narrowing it would miss builds, and it is what makes the cross-row disproof reachable.
- **LANDMINE — `AttemptDirectory` is `<platform>-<slug>-<loader>-<line>` and `ownerOf` cuts TWO parts.** One
  loader wins several lines; a shorter name lets one target wipe another's pack mid-run. Keep `nameFor`'s part
  count and `SUFFIX_PARTS` in lockstep.
- **The crash guard the axis needs.** `shouldRecheckAgainstOtherVersions` also arms on a **decisive** crash
  (in practice `OPERATOR_RULE`, which nothing else cross-checks), and `pickRecheckCandidates` spends its
  **first** attempt on the crashing era's other loader — every other line is already a verdict the report
  reconciles against for free.
- **`BootVerifier` has two entry points.** `verify(project, target, …)` boots the caller's choice and never
  re-selects (re-selecting would answer a crash on one era with a boot on another); `verify(project, loader, …)`
  selects itself. `bootableCombination()` is public so callers select with the same gate the boot applies.

## Dependency failures that are ours to prevent

- **A jar-in-jar library's demands bind exactly like a staged jar's.** `BundledJars` reads what nested jars
  *require* as well as provide: a bundled copy's `fabric-api` dependency, or a bundled **Minecraft pin**
  (QFAPI's nested `qsl_base` pins an exact Minecraft). `requirementsIn` and `minecraftDemandsIn` are separate
  because an unmet mod dependency is *staged*, while a wrong-Minecraft bundle can only be answered by dropping
  the jar carrying it.
- **A demand the loader itself must satisfy is handled at three layers, and all three are required.**
  (1) `BundledJars.demandsOn` reads back loader demands that `FabricScanner.dependencyExclusions` correctly
  strips; (2) the `loaderProvides` seam says what a loader provides (the grinder reads it from the installed
  loader jar, never a table); (3) `DependencyBacktrack` judges it. A fix at one layer alone changes nothing.
- **LANDMINE — what we ask the installer for is not always what boots.** `BootLoaderVersion` reads the loader
  build the console announces and states any disagreement in the detail. The newest-build re-check is
  deliberately **not** armed off it: if the installer keeps producing the old build, that costs a boot per row
  and fixes nothing. **Open:** why a Quilt tuple labelled `0.31.0-beta.4` boots quilt-loader 0.30.1.
- **"Untagged means Forge" is false above Minecraft 1.13.** The *dependency* untagged fallback is gated below
  1.13; the **candidate's** is not, because `refuseForSelfDeclaration` reads the downloaded jar and a dependency
  gets no such guard.
- **The version range lives in the jar, never in the platform ref.** `PlatformDependencyDemand.demandedConstraint`
  finds it through the same fuzzy id-to-slug match `isDemanded` uses — one matcher, not two.
- **Never resolve an unknown mod id by platform text search** — it returns other people's mods. Unresolvable ids
  get **verified** `KnownModIds` entries (Modrinth ref checked by reading the id from that project's jar,
  CurseForge id by its file names), never a smarter guess.
- **Known not-ours failures** (upstream-unsatisfiable ranges, the Sinytra Connector placeholder, non-dependency
  failures) are listed in the archive so they are not re-opened.

## A clean boot outranks an inherited client-only proof

`propagateClientOnlyProof` carries one build's client-only proof to the project's other targets (*a mod's
features do not change with the loader*). That inference is invalid when the reaching is one build's defect, so
`contradictsTheProof` blocks propagation onto a target that booted a server **itself** and whose mod is declared
`Declaration.SERVER`. Narrow in three directions, each load-bearing:

- **The claim is `Declaration.SERVER`, never `declaresServerSupport`** — that predicate accepts
  `JarScan.SERVER_OR_BOTH`, which is also what a scan that read nothing returns; `CONTRADICTORY` is not evidence.
- **`sodium`-shaped mods are untouched** (declaring `client_side: required`) — the case propagation exists for,
  since stems differ per loader.
- **The survival must be the target's own** (`bootedLoader == loader`), as in `targetDisprovingTheCrash`.

**The accepted cost:** a mod whose metadata wrongly claims the server and boots cleanly stops inheriting and
ships unused into server packs — cheaper than a false positive stripping a working mod from every pack.

- **LANDMINE — an inherited proof is a *field*, not prose.** `GrindTargetVerdict.inheritedProofFrom` /
  `inheritedProofRule` (the grinder's `Inherited proof` column). Without them `decidedBy` shows the row's own
  `READY_LINE` and `GrinderAuditIT` reads the row as resting on no evidence.
- **`com.mojang.blaze3d` is client-only evidence** alongside `net.minecraft.client` — a dedicated server ships no
  rendering layer, so no environment failure can fabricate it.

## Engine details & landmines (durable)

### Host boots, HTTP and platforms

- **LANDMINE — `start.sh` accepts only the exact required Java major**; otherwise `install_java.sh` prompts, and a
  terminal-less boot (stdin `/dev/null`) can only decline. `-verifyclientside` passes a `HostJavaSelection`
  post-processor writing the JDK into `variables.txt` (`PackVariables.prepareUnattended`), chosen from
  `JAVA_HOME_<major>_<arch>` (`ImageJavaRuntimes.javaHomesIn`) plus the running JVM. A missing major **throws** →
  `ERROR` row (deliberately not the grinder's `minecraftAcceptable` gate, which silently drops the line). The
  prompt refusal is also a `setup-abort` rung. **The GitHub workflow must install every major a release can need**
  (8, 16, 17, 21, 25 — Mojang's per-version requirement, not SPC's target).
- **LANDMINE — `JdkHttpFetcher`'s default client is `SHARED`, one per process; never make it per-instance** (each
  client owns a pool and a `SelectorManager` thread, and `supportedPlatforms()` builds fresh platforms per
  candidate). Safe because the CurseForge key is a per-request header. `JdkHttpFetcherClientSharingTest` pins it by
  identity, and that a caller-supplied client still wins.
- **`MetadataScanner` dispatches through `-api`'s `ModScanner.scannerFor(modloader, minecraftVersion)`.** Never
  re-add a local `when (loader)`. An unknown loader yields `null`, read as `SERVER_OR_BOTH`.
- **Platforms** sit over an injectable `HttpFetcher` (tests use canned JSON). **CurseForge has no sideness field**
  → `DeclaredSupport.UNKNOWN`. Use `JsonNode.textOrNull` for nullable fields — `asText(null)` returns `"null"` and
  defeats `ModFile.locked`.
- **LANDMINE — a Modrinth version's `files[]` is not a list of mods.** `ModrinthPlatform.modFilesOf` keeps
  `"primary": true` files (all files when none is flagged); a source jar otherwise poisons the stem, scan and boot.
  CurseForge has no such flag.
- **CurseForge `resolve` pages** `/mods/{id}/files` up to `MAX_FILE_PAGES` (warning on truncation) — the newest 50
  span all loaders, hiding a migrated project's old loader. `resolveDependency` reads **one page at the booted
  version**, then one per patch neighbour only if needed, to protect the key's quota.
- **LANDMINE — `DeclaredSupport` (a platform's self-report about one side, read as a pair, `UNKNOWN` for
  CurseForge) and `api.modscanning.Sideness` (SPC's own verdict, default `SERVER`) are different concepts; never
  merge them.** `ClientsideVerifier.aggregate` bridges them at one call-site because the claim is unreliable;
  merging would also push a third party's vocabulary into published `-api`.

### Boot signal and the classifier ladder

- **Boot signal** (`BootVerifier`): force-include the mod (auto-exclude off, empty clientside-list) plus its
  recursive required deps, generate a server pack, boot it via the ServerStarterJar; `BootLogClassifier` (pure)
  reads the console. Only a crash a *decisive rung* explains is evidence. SURVIVED yields `CLEAR`, ranked **below**
  `metadataClient`, so a clean boot never overturns a client-only declaration. **Exit 137 on SURVIVED is normal** —
  the container runner stops at the ready-line.
- **LANDMINE — a boot where the mod never got a fair run must never read CRASHED.** (1) Selection gate:
  `LoaderVersionResolver.latest` returns `null` for an unsupported Minecraft (Fabric-family via
  `Meta.isMinecraftSupported`). (2) `setupAbortMarkers` (`start.sh`'s `crashServer`). (3) `killedExitCodes`
  (137, 143) and `outOfMemoryMarkers`; `SIGABRT` (134) is deliberately *not* excused, and a genuine
  `NoClassDefFoundError: net/minecraft/client/…` must still read CRASHED. (4) `loaderBootstrapFailureMarkers` — the
  loader failing before FML exists; **match the message, not the module name** (it varies per run); the starter
  jar's give-ups and the JVM's `Error: could not open` (`@argfile`) are guarded too. All → INCONCLUSIVE.
  **Re-derive the rung count from `classify`**; `theGuardOrderIsPinnedAsAWhole` pins the order as a unit — no
  sibling test restating it.
- **LANDMINE — the console decides, not the exit code.** NeoForge's ServerStarterJar prints a crash and exits `0`.
  `clientOnlyClassMarker` returns CRASHED from the console alone, ahead of exit-code logic but **below the timeout
  and killed/OOM guards**. Never make CRASHED depend on the exit code alone.
- **Only four rungs are decisive** (`BootDecision.decisive`): `CLIENT_ONLY_CLASS`, `LWJGL_ON_A_DEDICATED_SERVER`,
  `FML_INVALID_DIST` (unfabricable by a broken harness) and `OPERATOR_RULE`. `Classification.decidedBy` records the
  rung; `/as-properties` publishes nothing else. `mixinApplyFailureMarkers`, `loaderSolverFailureMarkers` and
  `runtimeMismatchMarkers` sit **below** `clientOnlyClassMarker`, so a client class reached through a mixin still
  reads CRASHED.
- **LANDMINE — a rung whose bundled id does not resolve matches NOTHING.** Order lives in `BootLogClassifier`,
  patterns in `boot-rules.default.json` by id. A missing id or uncompilable pattern goes to `missingRuleIds()`, is
  logged at ERROR and fails `BundledRuleIdsResolveTest`. An unreadable bundled file degrades to "no console rules".
- **A confirmation credits the rule that DECIDED it** — `firedRule` only when `decidedBy == OPERATOR_RULE`.
- **OPEN — two rule types should become one:** `ConsoleRule`/`ConsoleRuleSet` (operator's `SPC_GRINDER_BOOT_RULES`,
  speaks `BootResult`) and `BootRule`/`BootRuleSet` (bundled, speaks `Verdict`). Merging changes the operator file's
  vocabulary — a breaking change, hence deferred. **Do not add a third.**
- **Operator console rules are a rung**: **below** the ready-line and every fair-run guard (a hand-edited file can
  never turn host trouble into a HIGH), **above** the client-class marker and exit code.
  - No verdict = **undecided**: `ConsoleRuleSet.undecidedVerdict`, default `null` (ladder decides);
    `SPC_GRINDER_RULE_FALLBACK=inconclusive` affects undecided rules **only**. A **misspelt** verdict is always
    INCONCLUSIVE and recorded in `errors`. First match in file order wins.
  - No id or no pattern **drops** the rule; an unreadable verdict **falls back**; a broken file keeps the last good
    set, so `ConsoleRuleSet.errors` is on `/status`.
  - Reload is a `(lastModified, length)` stat, not a `WatchService`; two same-size edits within a second are missed.
  - **`bootArtifactSink` must stay the LAST parameter** of `runPrepared` and `BootVerifier`'s constructor, or
    trailing-lambda call sites silently re-bind.

### Boot evidence

- **Every attempt's evidence goes to `bootArtifactSink` from inside `runPrepared`.** `BootArtifacts.collect` returns
  console, `logs/` and `crash-reports/` separately; `BootArtifacts.worthKeeping` (anything but SURVIVED) is shared
  by CLI and grinder. Capped by a **seeking** tail read, never `readText`-then-trim; everything found is listed in
  `index.txt`.
  - **LANDMINE — the hook fires per *attempt*, at exactly one call site** (private `BootVerifier.boot`,
    `onlyOneCallSiteInvokesRunPrepared`); staging wipes the attempt directory between attempts.
  - **The CLI sink writes `<work>/boot-evidence`, a SIBLING of `boot/`** (which is recreated per attempt), as
    `<attemptName>/<n>-<loader>-<loaderVersion>` — the ordinal gives uniqueness. `VerifyClientsideArtifactSinkTest`
    pins the wiring by source.
- **Landmine — every attempt writes the same `boot.log`.** `restoreDecisiveConsole` (end of `verify`) restores the
  decided outcome's console; any new re-check path must leave that call last.

### Crash re-checks and cross-row reconciliation

- **`LoaderVersionPolicy`:** `preferredVersion` boots (the grinder may prefer a cached older build);
  **`latestVersion` alone drives the support gate**, and a CRASHED outcome on a non-newest build is re-booted on the
  newest before it stands (`shouldRecheckCrash`, `reconcileRecheck`). An INCONCLUSIVE re-check never clears a crash.
- **A loader too old for the pack is re-checked on the newest build whatever the verdict** (`LoaderVersionDemand`,
  `shouldRecheckOnNewestBuild`): a demand phrase **and** a runtime-provided loader id on one line, whole-word (or
  `forge` fires inside `forgeconfigapiport`). **Not a `BootRule`** — an operator typo must not cost containers. A
  stale shipped loader manifest causes the same symptom: refresh with
  `./gradlew :serverpackcreator-api:updateManifests`, then re-run the api suite **after** the copy (the task's
  `dependsOn(test)` runs before it).
- **A crash contradicting declared server support is re-checked on other versions**
  (`recheckCrashOnOtherModVersions`; pure `shouldRecheckAgainstOtherVersions`, `reconcileOtherVersionRecheck`,
  `pickRecheckCandidates`). One clean boot clears it.
  - **Diverse sample:** each pick adds a new Minecraft line (`minecraftLine`, first two components) *and* loader,
    newest first, crashing line pre-marked — a **preference**, not a filter.
  - **Landmine — crossing loaders here is wider than `targetDisprovingTheCrash` permits, and the gate is why**; do
    not loosen one by pointing at the other. Every attempt stages into the **crashing** loader's directory.
  - **Landmine — the gate is the contradiction** (`ClientsideVerifier.declaresServerSupport`, shared with the
    "Declared server/both but the server crashed" note — keep them shared).
- **A crash cannot outrank another row's clean boot of the same list-entry** (`reconcileAcrossTargets`;
  `targetDisprovingTheCrash`, `supersededByTarget`). Different stems → no contradiction; never "any survival clears
  any crash".
  - **LANDMINE — check whose boot it was:** require `other.bootedLoader == other.loader` (`BootOutcome.bootedLoader`
    → `GrindTargetVerdict.bootedLoader`); the Markdown cell reads `SURVIVED (via <loader>)` when they differ.
  - The crash keeps `bootResult` and excerpt; the note is **rebuilt**, never appended to.
- Both guards cost: the re-check runs during the crashing boot, reconciliation afterwards with no look-ahead —
  deliberately, for the more specific within-era answer.
- **LANDMINE — scratch space is owned by `(platform, slug, loader, line)`, not the slug.** `AttemptDirectory` builds
  and parses it for `ClientsideVerifier`, `BootVerifier` and the grinder's `BootWorkspaceReaper` (which deletes by
  name); an unqualified name lets parallel workers wipe each other's packs. Parse by cutting exactly `SUFFIX_PARTS`
  trailing segments — slugs nest. Old-format directories are cleared by the grinder's `reapAll()`.

### Dependency staging

- **LANDMINE — `"<Loader> is not available for Minecraft X"` means "could not check"** (an HTTP probe that always
  fails under `--network none`); never feed it into loader-support memory. `setupFabric` (all three shells) settles
  the launcher from disk first, and **that branch must fall through** to the `SERVER_RUN_COMMAND=…` assignment.
  `-api`'s `ScriptTemplateContentTest` **executes** `setupFabric` — the only guard that catches it.
- **Missing dependencies never reach a boot:** `refuseForMissingDependencies` aborts with a named reason;
  `dependencyFailureMarkers` → INCONCLUSIVE is the backstop.
- **`DependencyBacktrack` judges the staged SET and demotes a conflicting dependency.** It never demotes the
  candidate, never **refuses** (every doubt proceeds to the boot), and ignores optional deps and requirements naming
  nothing staged. It re-stages and re-downloads from scratch (`MAX_BACKTRACKS` = 10); skip-on-disk only if a
  measured rate warrants it. Jars not `descriptorRead` are dropped. `InjectedDependency.version` is what the platform
  published each file as.
  - **LANDMINE — that version is a release *name*.** CurseForge's is the author's `displayName`, and `numbersOf`
    maps digit-less components to `0`; `readableVersion` gates `satisfies`, and prose **accepts**. **Never extract a
    version from a release name.**
  - Jar-in-jar libraries count as staged (`BundledJars.versionsIn`, `BootVerifier.nestedVersions`), **under**
    top-level ones; an id bundled at two versions contributes nothing.
  - `UnmetReason` names each refusal; `backtrackReason` re-runs the pick **unfiltered** to tell "dropped" from
    "nothing usable". `unsatisfied` is a `Map<name, reason>` — the reason travels beside the name.
- **A dependency already in the pack never refuses its boot:** a lowercased `provided` set (`id` + `provides` of
  every staged jar) is threaded through staging, since one project is reachable by ref and by a different mapped id.
  Each staged jar's descriptor is read **once** (`scanStagedJar`) for the platform loop, manifest stager and
  `identityIn`.
- **One mod id, several projects:** `LearnedModIds` keeps **every** prover in order (never overwrite, never forget);
  `planManifestDependency` tries each, and only an alias may refuse. `JsonLearnedModIds` still reads the old
  bare-string shape. Only a jar's own `id` + `provides` is learned, never what it bundles.
  `KnownModIds.alternatives` (e.g. `create` → `create-fabric`) is tried **after** the primary. **Never invent
  CurseForge numeric ids.**
- **Neighbouring-patch fallback** (Griefed's call): nearest patch, ties to newer, only published versions, never
  across a line. **Landmine — never widen the loader rule with it**: `pickFrom` takes a separate `compatibleAt`, the
  Minecraft the pack **boots at** (`theNeoForgeFallbackToForgeAppliesOnMinecraft1201Only`). `preferenceLadder`:
  obtainability, then Minecraft version, then declared constraint.
  - **LANDMINE — CurseForge narrows `resolveDependency` by exact `gameVersion`**, so
    `BootVerifier.resolveDependencyAcrossTheLine` asks for the exact version alone, then the line, via a second
    `resolveDependency` overload defaulting to the narrow one. **Keep the narrowing.**
- **A dependency's own Minecraft range is a pre-boot gate:** `outsideThePacksMinecraft` (in `dependencyToDemote`,
  asked **before** version conflicts) demotes a dependency that positively excludes the booted version. Everything
  uncertain accepts; the candidate is excluded (`reselectOnMinecraftContradiction` handles it). Both paths yield
  `UnmetReason.DROPPED_BY_BACKTRACK` → `PreventionCause.UPSTREAM_UNAVAILABLE`. Exhausting backtracks logs and boots.
- **The jar is the authority on what it needs.** A platform-declared dependency the descriptor never names goes to
  `unmapped` and never refuses (`PlatformDependencyDemand`; fuzzy slug-vs-id by necessity). `null` declared-ids
  leaves the platform in charge; an empty set is a read descriptor asking for nothing.
- **`askLinkedProjects`** is the last step for a required, jar-declared id nothing else resolves: probe the page's
  linked projects (required + optional, never `incompatible`/`embedded`), learn everything probed, track a per-jar
  `probed` set, probe outside `mods/` and delete, stage a match via the ordinary path.
  `nothingIsProbedWhileTheIdStillResolves` pins the cost.
- **Manifest dependencies and Fabric API / QSL modules.** `KnownModIds` bridges vocabularies.
  - **LANDMINE — Fabric API modules are mapped by RULE, not table**: `fabric-<x>-v<digits>`, plus `fabric-api-base`
    and `fabric-renderer-indigo`. `notFabricApi` holds only **observed** collisions (lucko's
    `fabric-permissions-api-v0`). A bare `fabric-` prefix proves nothing.
  - QSL: `^quilt_[a-z0-9_]+$` → `qsl`/`634179`, `notQsl` = `quilt_loader`. **`quilt_base` is a QSL module, not the
    platform** (Griefed's call, 2026-09-01) — keep it out of `QuiltScanner.dependencyExclusions` and
    `environmentProvidedIds`.
  - `visited` is claimed on the *ref*, so many modules → one download, one `MAX_INJECTED_DEPENDENCIES` slot.
  - **`VersionConstraint` fails towards ACCEPT, always.** `looksLikeVersion` gates every comparison
    (`VersionConstraintFuzzTest`); **never add an ungated comparison site.** `unionMembers` splits Maven unions on
    top-level commas by bracket depth (`(,1.0],[1.2,)`; bare `26.2,26.3` too).
  - `pickDependencyFile` treats a constraint as a **preference**; never return `null` where a file exists.
  - **Attribution annotates, never downgrades** (`DependencyAttribution`, `attributionNeverChangesTheBootResult`);
    blame needs a crash marker or frame and stands down when the candidate is named in the same context.
- **THE REFUSAL SPLIT KEYS ON MAPPING CONFIDENCE.** `KnownModIds.mappingFor` → `Alias` | `Guess` | `None`. **An
  alias refuses; a guess never does, at any stage** (`ManifestDependencyPlan.Stage.confident`). `unmapped` is a
  separate collection that never reaches `refuseForMissingDependencies`. CurseForge falls back from a numeric ref to
  `modIdForSlug` (exact slug, never text search).
- **LANDMINE — obtainability outranks the loader match in `pickDependencyFile`**, but stays a preference: all-locked
  returns a locked file so the refusal truthfully says "distribution-locked".
- **`LoaderCompatibility.alsoRuns(loader, minecraftVersion)` is the ONE home of cross-loading** (descriptor gate and
  dependency selection); never add an overload without the version. Quilt runs Fabric builds; **NeoForge runs Forge
  builds on 1.20.1 only — the one version, never a lower bound.** Both one-way; a real NeoForge build wins.
- **Only Forge/NeoForge boots can prove clientside**; Fabric skips `environment: "client"` entrypoints and survives.
  **`LOADER_PRIORITY` is the order in which a boot can say something**, not popularity; move a line off the Forge
  family only when that boot could not produce proof anyway. Stated limit: a well-behaved Fabric-only client mod
  cannot reach CONFIRMED, and metadata may not carry a verdict.
- **Carrying a loader's descriptor is not running under it.** `JarSelfDeclaration.demandedLoaderVersion` reads the
  jar's `forge`/`neoforge` `versionRange`; `contradictingLoaders` drops loaders whose **`latestVersion`** cannot
  satisfy it and names those that can (`reselectOnLoaderContradiction`); it never fires when nothing is reachable.
  `platformIdsFor` derives `forge` vs `neoforge` from `alsoRuns`. Read here, not via `ForgeTomlScanner`.
- **A Sinytra Connector placeholder is a Fabric mod, scanned AND booted as one.** `isConnectorPlaceholder` reads
  `"connector:placeholder" = true` from **either** TOML (`PROPERTY_BEARING_DESCRIPTORS`, version-blind). It swaps
  the scanner's *input*, not `scannerFor`; keyed on the marker, never on carrying both descriptors. **Never "fix" a
  Connector boot by staging more dependencies** — Forge reads the stub's empty ranges as matching nothing.
- **`allowModDistribution=false`** files have `downloadUrl=null` (`ModFile.locked`) and are **not obtainable**;
  `JarScan.DEFERRED`, refusal points at Modrinth. **Never reintroduce a browser route** (Griefed's call,
  2026-09-02); `JarDownloaderRoutingTest` asserts `BrowserDownloader` is absent from the classpath.
- **The descriptor gate is a filter, not a veto.** When the jar's range excludes the picked version,
  `reselectOnMinecraftContradiction` re-stages once on `newestVersionSatisfying`; self-excluding ranges
  (`[1.21, 1.21.1)` tagged 1.21.1) are upstream — **do not "fix" the parser**.
  - **Landmine — `Prepared.Failed.declaredMinecraftConstraint` is set only for the Minecraft disagreement**
    (re-asked, not inferred from `contradiction`, which also reports wrong-loader descriptors). Retries call
    `stageBootPack`, never `prepareBootPack`.
  - **When jar and tags share no version, the JAR wins and the pack is bumped** (Griefed's call, 2026-09-06) via
    `newestReleaseSatisfying`. **`constrainsAnything`** (does it exclude anything in the searched set?) stops this
    relocating every candidate — never re-detect parser-tolerated shapes.
  - `Prepared.Failed.declaredLoaders` (the jar outranks the platform's loader tick, Griefed's call) is a **sibling**,
    never a widening. One retry, loader first, gated on that loader having a build, staged into the **requested**
    loader's directory. The channels are exclusive by construction (`aLoaderMismatchLeavesNoRangeToRetryOn`).
- **LANDMINE — an untagged CurseForge file is *unknown*, not incompatible.** `pickUntagged` is the **last** arm of
  `pickFrom` (exact Minecraft; a different loader tag still refuses), and `pickBootableCandidate` shares it via
  `newestOf` (Griefed's call). A wrong-loader candidate could publish a false CLEAR; `loaderVersionAvailable` and
  `refuseForSelfDeclaration` prevent it. A refusal naming a slug may still come from the platform path.
- **Descriptor-per-loader-per-version has one home: `-api`'s `LoaderDescriptors`.** Declaring is not reading: on
  1.20.1 a `neoforge.mods.toml`-only jar is not Forge. On 1.20.2–1.20.4 a `mods.toml`-only jar is accepted for
  NeoForge (indistinguishable; `runtimeMismatchMarkers` catches Forge jars). `LegacyFabric`'s set is **empty**.
- **Quilt's `unless`** (`ModDependency.unlessProvided`): `stageableRequirements` drops a requirement whose
  alternative is present, **checking `bundledIds` as well as `providedIds`**. `quilt_base` with no `unless` stays
  UNVERIFIABLE.
- **Release channel outranks Minecraft recency for the CANDIDATE only** (`ReleaseChannel`, Griefed's rule: *newest
  Release for any loader; Beta or Alpha only when no release exists*), as a preference. **Do not add it** to
  `pickDependencyFile` or `pickRecheckCandidates`.
- **Cross-platform:** a dependency may come from the other platform (manifest route only; `stagedFromPlatform`
  files the learned ref; both `Unmapped` and `Unsatisfied` cross); a candidate never does.
- **Boundary readers:** a Modrinth dependency with `version_id` and null `project_id` resolves via memoised
  `projectBehind` to the *project*; a CurseForge JSON-null `modId` must never become the ref `"null"`.
- **LANDMINE — a refusal names the slug, never the raw ref** (`unsatisfiedLabel`; ref + platform only when
  unresolved; also dedupes). Label all three unmet branches (unresolved, no usable file, download failed) and any
  new one; a locked dependency is labelled `distribution-locked`. Both platforms' `resolveDependency` must put the
  **slug** in `ProjectFiles.slug`.
  - **LANDMINE — CurseForge `resolveDependency` must narrow by `gameVersion` and must NOT send `modLoaderType`**
    (`alsoRuns` must see Fabric builds for Quilt). Parameters per https://docs.curseforge.com/rest-api/. Modrinth
    ignores `minecraftVersion`.
  - A test constructing the value under test cannot see its producer build it wrongly — `DependencySlugTest`
    drives the real `resolveDependency`.
- **A dependency the candidate ships is never fetched** (`BundledJars`). **LANDMINE:** only descriptor-declared
  nested jars count (Fabric `jars`, Quilt `quilt_loader.jars`); **read them as a `ZipInputStream`, never a temp file**
  (`deleteOnExit` leaks); ids come from nested descriptors, never file names.

### Verdicts and prevention causes

- **Six verdicts:** `CONFIRMED`, `CLEAR`, `ERROR` (operator's problem), `INCONCLUSIVE`, `LOCKED` (distribution
  opt-out), `UNVERIFIABLE` (upstream gap). `LOCKED`/`UNVERIFIABLE` keep no logs. Only a rule reaches CONFIRMED, only
  from a decisive rung.
- **Prevented grinds are blamed:** `PreventionCause` (`HOST`/`DISTRIBUTION_LOCKED`/`UPSTREAM_UNAVAILABLE`) on
  `StagingOutcome.Prevented`, `Prepared.Failed` and `BootOutcome.prevention`; `stagingPrevented` is derived.
  `VerdictPolicy.decide` alone maps cause → verdict. `UnmetReason` owns its cause; `preventionCauseFor` folds to the
  most actionable (`HOST` > `DISTRIBUTION_LOCKED` > `UPSTREAM_UNAVAILABLE`). Ask `Verdict.grindRan`, never
  `== ERROR`. **Every default is `HOST`**, so a forgotten cause stays loud. `PreventedGrindBlameTest` must drive the
  real `prepareBootPack`/`refuseForMissingDependencies`, never pass the cause in. The grinder's `VERDICT_RANK` must
  rank every verdict (`everyVerdictHasARank`); `/as-properties` gates on CONFIRMED alone.
- **`boot-rules.default.json` is the ladder's content**, compiled by id via `bundledPattern`. Order is precedence:
  fair-run guards, `client-only-class`, excuses (`DefaultBootRulesTest`). **LANDMINE — order stays in code** (the
  killed-exit check sits between rungs); do not turn `classify` into a bare loop.
- **LANDMINE — the console decides, metadata only declares.** A `RuleSource.METADATA` rule sets `declares` and may
  not set `verdict` (`noMetadataRuleCarriesAVerdict`). Metadata facts render as ONE line (`MetadataFacts.line`) so two
  fields in a pattern are an AND; `platform_server=`, `platform_client=`, `manifest=` are a pinned operator interface.
  `VerdictPolicy.decide` takes `declared` and never consults it, deliberately.
- **`stagingPrevented` must be set on EVERY no-container path**: staging refusal, re-stage refusal, a thrown
  `packPostProcessor` (the grinder's `overlayLoaderInstall`), `RunResult.NotStarted`. Pin new ones in
  `PreventedGrindTest`; `aRealBootThatFailedIsNotMarkedPrevented` is the counterweight.
- `verdictOf` writes the **note** (contradicted server claim, never-readable locked file). `BootResult` stays as the
  per-boot reading.
- **Client-only proof crosses builds and loaders** (`provesClientOnly`: the three unfabricable rungs) — no re-check,
  no supersession, every loader inherits CONFIRMED. **LANDMINE — never widen it to `OPERATOR_RULE`**; an unexplained
  crash must stay disprovable. A superseded `ERROR` keeps its reason in the note.
- LWJGL and `fml-invalid-dist` are **shipped** rules in `boot-rules.default.json`, not only in the never-loaded
  `boot-rules.example.json`.

### Published names and the fallback-list editor

- **Two names, one publishable.** `GrindTargetVerdict.suggestedEntry` is the longest common prefix over the whole
  history (matched with `startsWith`); `sampleFile` is the sampled artifact's verbatim name. **LANDMINE — never
  publish `sampleFile`** (`theSampledFilenameIsNotWhatGetsPublished`). There is no `filenamePattern`; `fileName` is
  the grinder's. `deriveStem` survives for `Prepared.Ready.candidateStem`. A Quilt row's `-fabric-` sample is
  correct. `ClientsideReportRenderer` shows only `Suggested entry`, deliberately.
- **`ClientsideListEditor`** (pure) edits `fallbackMods` in `GenerationConfig.kt` (sorted, aligned `//link`) and
  `fallbackmodslist` in `serverpackcreator.properties`. **Landmine:** the *last* properties entry must NOT end in
  `,\`, or it corrupts the next property.

## Boot seam

`BootVerifier.prepareBootPack()` does host-side staging and returns `Prepared.Ready`; a **`ServerRunner`** runs
the pack and returns *raw* `RunResult` lines + exit status (classification left to the caller);
`BootVerifier.outcomeFor()` is the shared verdict seam (writes the log, then `BootLogClassifier` +
`BootLogExcerpt`). Default `HostProcessServerRunner` spawns `start.sh`; the grinder supplies a container runner
(`--network none`, resource-capped) and reuses `BootLogClassifier` verbatim.

## Testing patterns

- **All offline.** Most build jars in-memory (`java.util.jar`) or feed canned JSON to a fake `HttpFetcher`. Tests
  driving real *staging* boot an offline `ApiWrapper` from `src/test/resources/serverpackcreator.properties`
  (hence `test` `dependsOn :serverpackcreator-api:processTestResources`). **Never write counts here** — derive the
  suite total from `build/test-results/test/*.xml` and the ApiWrapper set from `grep -rl "ApiWrapper.api(" src/test`.
- Keep new logic pure/offline-testable (`BootCandidateSelector`, `BootLogClassifier`, `FilenameStemDeriver`,
  `ClientsideListEditor`, `BootVerifier.outcomeFor`). `verify` needs an `ApiWrapper` and a running server, so
  re-check *timing* and *reconciliation* are pinned as pure decisions in `BootVerifierCrashRecheckTest`.

## Boot deadlines are suspend-aware (shared with the grinder)

`SuspendAwareDeadline` is what both boot paths poll (`HostProcessServerRunner` and the grinder's
`DockerJavaContainerEngine`): any gap between polls too large to be slowness (host sleep/freeze) is added back to
the budget, so a timeout means *"the boot had this long and did not make it"*. It lives here because grinder
depends on clientside, not the reverse; the clock is injected for testability. **Landmine:** any new poll/park
loop bounding a boot must use it, never `System.currentTimeMillis()`.

**LANDMINE — "did it time out?" is a question about the run, never the clock** (`BootDeadline.timedOut`). A wait
ends on ready, on the subject finishing, or on budget spent; only the last is a timeout. `!ready &&
!deadline.hasTimeLeft()` cannot tell the second from the third when setup calls outlast the budget. Ask the end
state — **still running and never ready**. It matters because `classify` branches on timeout second and returns
INCONCLUSIVE, discarding exit code and crash evidence. Both runners must go through this one predicate
(`BootDeadlineTest`).

## Boot teardown kills the tree, and "alive" is not "running"

`start.sh` is a launcher: the server is a `java` child of the spawned `bash`. `HostProcessServerRunner.destroyTree`
collects `ProcessHandle.descendants()` **before** killing anything (orphans reparent and stop being descendants),
asks the whole tree to exit, and forces only what remains after one shared budget.
`HostProcessDescendantTeardownTest` asks the OS, because `RunResult` reports the shell's exit either way.

**Landmine — `ProcessHandle.isAlive` is true for an exited, unreaped process (zombie).** Under a container whose
PID 1 is not an init (the CI job container), zombies are permanent. Liveness has one definition,
`HostProcessServerRunner.isStillRunning` — alive **and** still carrying a command line. Use it, never `isAlive`,
wherever teardown or a guard asks whether a boot's processes survived. It is also why the graceful wait **polls**
rather than awaiting `onExit()`, which never completes for an unreaped process. `HostProcessLivenessTest` pins the
distinction and skips (never passes vacuously) where it cannot stage.
