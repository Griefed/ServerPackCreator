# serverpackcreator-clientside — module context

> The **clientside-mod verification engine**, extracted out of `serverpackcreator-app` so it can be
> reused by both the app's CLI verbs and the planned standalone Docker "grinder" service. Package
> `de.griefed.serverpackcreator.clientside`. **Depends only on `serverpackcreator-api`** (plus
> jackson-module-kotlin; Playwright was dropped 2026-09-02) — it must never gain a dependency on `-app`, Spring, or Swing.
> Not published to Maven Central (unlike `-api`), so it can churn freely without compatibility lock-in.

## What it does

Given a Modrinth/CurseForge project-link: pick the platform, resolve the project's files, derive the
clientside-list file-name stem(s), and combine signals into a per-loader `Verdict`. Driven by the
app's four CLI verbs (`-scan`, `-clientsidereport`, `-verifyclientside`, `-clientsideapply`) and the
`clientside-*.yml` workflows — see `serverpackcreator-app/CLAUDE.md` for the verb wiring and CI.

## Engine details & landmines (durable)

- **`MetadataScanner` no longer mirrors `ModListCompiler` — since 2026-08-15 both dispatch through
  `ModScanner.scannerFor(modloader, minecraftVersion)` in `-api`.** It *is* shared code now; do not
  re-add a local `when (loader)` over the concrete scanners. The old "kept in sync deliberately" note
  is why this entry exists: the two copies had already drifted, and both carried the same Forge
  era bug (Minecraft's `YY.x.y` scheme read as `1.x` — see the versioning-scheme landmine in
  `serverpackcreator-api/CLAUDE.md`), so the metadata signal was degraded on every modern Forge mod.
  A loader `scannerFor` does not know yields `null`, which this class reads as `SERVER_OR_BOTH`:
  nothing was read, so nothing declared the mod client-only.
- **Platform layer**: `ModPlatform` (Modrinth/CurseForge) over an injectable `HttpFetcher` so tests use
  canned JSON (no live network). **CurseForge has no sideness field** → `DeclaredSupport.UNKNOWN`; only
  Modrinth declares `client_side`/`server_side`. Use `JsonNode.textOrNull` for nullable URL fields —
  `asText(null)` returns the literal `"null"` for a JSON-null and would defeat `ModFile.locked`.
- **LANDMINE — a Modrinth version's `files[]` is not a list of mods.** Authors attach source jars, and
  Modrinth flags the real one `"primary": true`; `ModrinthPlatform.modFilesOf` keeps only those, falling
  back to *every* file of a version that flags none (3 of `creativecore`'s 300 versions genuinely do, and
  dropping them would lose real builds). Without it a source jar is a candidate for everything a `ModFile`
  feeds: the published list-entry, the jar-scan sample, and — order permitting — the boot itself.
  **What it cost, measured against the live API on 2026-08-23:** `creativecore`'s Fabric group holds 143
  files, one being the stray `CreativeCore-sources.jar`. That name shares no delimited prefix with the
  `CreativeCore_FABRIC_v*.jar` builds, so `FilenameStemDeriver` fell back to stripping the version off the
  *shortest* name and published `CreativeCore-sources` — an entry matching nothing the project ships. It
  also cost the mod its cross-loader disproof: `loaderDisprovingTheCrash` compares entries, and that stem
  matched neither other loader's `CreativeCore_`, so a Fabric crash stood as HIGH while NeoForge had booted
  a server in the same run. **CurseForge has no equivalent flag** — its file list is plain uploads, so an
  author who publishes a source jar as a normal file there is still unfiltered; nothing has been seen doing
  it, and there is no signal to act on if one does.
- **CurseForge file resolution pages; a dependency's deliberately does not.** `resolve` walks
  `/mods/{id}/files` with `index` until `totalCount` is reached, capped at `MAX_FILE_PAGES` (10 × 50) with a
  warning when it truncates. **Why:** the newest 50 files are 50 files *across all loaders*, so a project that
  migrated Forge → NeoForge keeps publishing NeoForge builds until its older Forge builds fall out of the
  window — leaving the crash re-check nothing of that loader to try, precisely for the projects that produce a
  false clientside verdict. Most projects still cost one call. `resolveDependency` stays single-page on
  purpose: it needs *a* usable file for one loader/Minecraft pair, not a history, and paging every dependency
  of every candidate would multiply what a catalog sweep spends of the API key's quota.
- **LANDMINE — `DeclaredSupport` and `api.modscanning.Sideness` are different concepts; do not merge them.**
  This module's enum was itself called `Sideness` until 2026-08-14, which made them look like duplicates
  of one idea. They are not, and the confidence model depends on the difference:

  | | `api.modscanning.Sideness` | `clientside.DeclaredSupport` |
  |---|---|---|
  | Values | `SERVER`, `CLIENT` | `REQUIRED`, `OPTIONAL`, `UNSUPPORTED`, `UNKNOWN` |
  | Is | SPC's own **verdict** — which side a mod belongs on | a platform's **self-report** about *one* side |
  | Shape | one value = the whole answer | read as a **pair** (`clientSide` + `serverSide`) |
  | Default | `SERVER` when undetermined, so nothing is dropped | `UNKNOWN` when absent (all of CurseForge) |

  `ClientsideVerifier.aggregate` folds this, `JarScan` (where the API's verdict arrives) and `BootResult`
  into a `Verdict` **precisely because the platform's claim is unreliable** — which is the whole reason
  the expensive boot-test exists. The domains are bridged deliberately at that one call-site, and it takes
  *two* `DeclaredSupport` values to derive one client/server leaning. Merging the enums would collapse the
  distinction the model is built on, and would push a third party's field vocabulary into `-api`, which is
  published to Maven and is a plugin-compatibility constraint. The rename removed a real hazard, not just
  an aesthetic one: `MetadataScanner` sits in *this* package and imports the API's `Sideness`, which
  silently shadowed the package-local type — so one file's `Sideness` meant the opposite of its neighbours'.
- **Boot signal** (`BootVerifier`): force-includes the mod (auto-exclude off, empty clientside-list) +
  its recursively-resolved required deps, generates a server pack and boots it via the ServerStarterJar.
  `BootLogClassifier` reads the `Done (…)! For help` ready-line vs a non-zero exit (pure, unit-tested).
  **Asymmetry baked into the model (see the four-verdict entry below for the current rules):** only a
  crash a *decisive rung* explained is evidence (→ CONFIRMED, incl. the
  "declares server/both yet crashes" lie); a clean boot does not prove server-safe. **The declared
  sideness is a self-report and is unreliable** — that asymmetry is *why* the expensive boot exists.
  **But "not decisive" is not "not evidence", and `aggregate` conflated the two until 2026-09-01.** It
  consulted `bootResult` for CRASHED and nothing else, so a SURVIVED boot fell through to the metadata —
  and where there *is* no metadata (jar scan ERROR plus a platform declaring nothing, i.e. every
  CurseForge project) it landed in INCONCLUSIVE, which means "we learned nothing" about a run that
  learned the server started. SURVIVED now yields `LOW`, ranked **below** `metadataClient` so the
  asymmetry above is untouched: a clean boot still cannot overturn a client-only declaration. Measured
  against the live store: `better-stats`, `tcdcommons` and `yacl`, all `JarSideness=ERROR`, all decided
  `READY_LINE`. **Exit 137 on such a row is normal** — `ContainerServerRunner` stops the container at the
  ready-line — so the classifier was right and only the fold disagreed.
  **Landmine — loader-availability & pre-launch aborts (two-layer defense against a false HIGH):**
  a loader lacking a build for a brand-new Minecraft (e.g. Fabric on 26.2) must never be scored a
  clientside crash. (1) *Selection gate* — `LoaderVersionResolver.latest` returns `null` for a
  Minecraft the loader doesn't support: Forge/NeoForge are already MC-specific, and Fabric/Quilt/
  LegacyFabric are gated on `Meta.isMinecraftSupported(mc)` (the intermediary check). A `null` drops
  the combo from `BootVerifier.prepareBootPack`'s candidate selection, so no container is even spun up.
  (2) *Boot backstop* — if a boot still aborts pre-launch, `start.sh`'s `crashServer` messages (loader
  not available, launcher-jar/install download failure, Java/EULA/`variables.txt` setup failure,
  unknown modloader) exit non-zero *before the mod is loaded*; `BootLogClassifier` maps those
  (`setupAbortMarkers`) to **INCONCLUSIVE**, not CRASHED. Both found via a grinder e2e on MC 26.2.
  **(3) Killed-from-outside backstop** — `killedExitCodes` (137 `SIGKILL`, 143 `SIGTERM`) and `outOfMemoryMarkers`
  (`java.lang.OutOfMemoryError`, `insufficient memory for the Java Runtime Environment`, `Cannot allocate memory`,
  the shell's `Killed "$JAVA"`) also map to **INCONCLUSIVE**. Found 2026-07-30 while sizing a catalog sweep: the
  grinder caps a boot at 3 GiB but the host's Docker VM held **1.93 GiB**, so the cap cannot be honoured and a fat
  mod is OOM-killed by the VM — exit 137, no ready-line, and `Killed "$JAVA"` deliberately doesn't match the
  setup-abort markers, leaving `CRASHED` as the only outcome, i.e. **a HIGH-confidence "clientside" produced purely
  by host memory pressure**, systematically, for the biggest mods. `SIGABRT` (134) is deliberately *not* excused: a
  fatal JVM abort is a real failure of the running server. Keep the guard narrow — a genuine mod-load crash
  (`NoClassDefFoundError: net/minecraft/client/…`) must still read CRASHED, and a test pins that.
  **(4) Loader-bootstrap backstop** (`loaderBootstrapFailureMarkers`, 2026-08-23) — the *modloader itself*
  failing before FML exists is also **INCONCLUSIVE**. `ars-nouveau` (Forge 48.1.0 / MC 1.20.2) was headed for a
  HIGH off `IllegalStateException: Could not find parent layer for module`, thrown in
  `BootstrapLauncher.main` with no mod loaded: the ServerStarterJar synthesises a boot layer for the module path
  in Forge's `unix_args.txt`, and Forge's `SecureModuleClassLoader` matches a read module's configuration against
  its **direct** parents only, so `java.base` — one level up in the real boot configuration — is not found.
  cpw's original, which NeoForge runs, falls back to the platform classloader there, and that asymmetry is the
  whole reason the same starter jar launches NeoForge and not Forge. **Match the message, not the module:**
  reproduced locally, the same run named `java.management.rmi` read by `JarJarMetadata` instead — the iteration
  order differs per run. The starter jar's own pre-launch give-ups (`Failed to find run file at`, `Failed to find
  startup arguments using run script path`) sit in the same guard, and the JVM's `Error: could not open` for an
  unreadable `@argfile` joined `launchFailureMarkers`, which became reachable once the grinder started booting
  Forge from `@libraries/.../unix_args.txt`. The ladder is **sixteen** rungs, and
  `theGuardOrderIsPinnedAsAWhole` is what pins the order as a unit — re-derive the count from `classify`
  rather than trusting this sentence, which has been wrong twice.
- **A crash that contradicts the metadata is re-checked on the mod's other versions.**
  `recheckCrashOnOtherModVersions`, over the pure `shouldRecheckAgainstOtherVersions`,
  `reconcileOtherVersionRecheck` and `BootCandidateSelector.pickRecheckCandidates`.
  **Why:** only one build of a project is ever booted, so
  "this build crashes" and "this mod cannot run on a server" produced identical evidence — reported
  2026-08-23, `iron-chests` published `HIGH` off a single crashing `Forge 48.1.0 / Minecraft 1.20.2`. A mod
  that cannot run server-side cannot run server-side in *any* build, so one clean boot on another version
  clears the crash. Stops at the first clean boot.
  **The sample is deliberately diverse, not simply the next-newest builds** (2026-08-23). Each pick
  introduces a Minecraft **version-line** (`minecraftLine` = the first two components, so `26.1.2` and `26.1`
  are one line and `26.2` another — the granularity at which mod source actually differs) *and* a loader that
  no earlier pick used, newest Minecraft first, with the crashing combination's own line marked used from the
  start. **Why:** `creativecore` — a mod whose own description advertises server features — crashed on
  Fabric / MC 26.2 and spent both re-checks on Fabric 26.1.2 and Fabric 26.1, same loader, same loader
  version `0.19.3`. Both INCONCLUSIVE, so the crash published HIGH, while NeoForge 26.1.2.97 booted a server
  for the same project *in the same run* and the CurseForge run minutes earlier booted the very file the 26.1
  re-check gave up on. Two boots that close to the crashing combination re-test its environment, not the mod.
  The same budget now buys Fabric 26.1.2 + NeoForge 1.21.11 — verified by running the real
  `ModrinthPlatform` + `pickRecheckCandidates` over the project's live 300-version response, which is also
  where the `CreativeCore_FABRIC_` stem above was confirmed. **Diversity is a preference, not a filter** — it
  relaxes to a new line, then a new loader, then whatever is left, so a project publishing one loader and one
  Minecraft line samples exactly as deeply as before; `aSingleMinecraftLineStillSpendsTheWholeBudget` pins that
  direction. **Landmine — crossing the loader here is a wider claim than `loaderDisprovingTheCrash` permits**,
  and the difference is the gate: that pass applies to *any* crash, so it insists on a matching entry, while
  this sample is spent only where the crash already contradicts a declared server support, i.e. where one of
  the two signals is known to be wrong. Do not loosen one by pointing at the other. Two consequences worth
  knowing: every attempt's label names its loader (`file.jar (NeoForge, Minecraft 1.21.1)`) because the
  returned outcome may be a boot run under a *different* loader than the verdict is about, and every attempt
  stages into the **crashing** loader's directory (`stageBootPack`'s `attemptDirName`) — staging under the
  candidate's own loader would wipe the pack and console that loader's own verdict is about to be built from.
  **Landmine — the gate is the contradiction, not the crash.** It arms only when
  `ClientsideVerifier.declaresServerSupport` holds, which is the same predicate that prints the "Declared
  server/both but the server crashed" note; keep them sharing it, or the report states a contradiction the
  re-check silently decided did not exist. Where the metadata already leans clientside the crash *confirms*
  it, and in a catalog sweep that agreement is the common case — arming there costs two boots per true
  positive and buys nothing. **CurseForge has no sideness field**, so its claim can only come from the jar
  scan: a gate reading the platform alone never arms for a CurseForge mod, i.e. never for the report that
  prompted this. Conservative in every other direction, like the re-check below: crashes elsewhere
  corroborate, and an attempt that learned nothing leaves the crash standing.
- **A crash cannot outrank another loader's clean boot** (`ClientsideVerifier.reconcileAcrossLoaders`, over the
  pure `loaderDisprovingTheCrash` / `supersededByLoader`). `report()` runs a second pass once every loader is
  in: where one loader CRASHED and another SURVIVED deriving the **same** list-entry, the crash stops counting
  as sideness evidence and the confidence drops to what `aggregate` yields with no boot. **Why the entry and
  not the loader:** the published artefact is a loader-agnostic file-name stem matched with `startsWith`, so
  the crash's entry would strip the surviving build. Where the stems differ nothing is stripped and there is
  no contradiction — a mod really can be client-only on one loader, and this must not become "any survival
  clears any crash". The live case (2026-08-23) had both rows in **one run**, and the disproof was discarded:

  | Loader | Was | Entry | Boot |
  |---|---|---|---|
  | Forge | `HIGH` | `ironchest-` | Forge 48.1.0 / MC 1.20.2 → CRASHED (exit 1) |
  | NeoForge | `LOW` | `ironchest-` | NeoForge 21.11.45 / MC 1.21.11 → SURVIVED (exit 137) |

  **LANDMINE — `bootResult` alone is not enough; check *whose* boot it was.** Since the other-version re-check
  began spanning loaders, `reconcileOtherVersionRecheck` can decide one loader's verdict from another loader's
  clean boot, leaving `bootResult == SURVIVED` on a loader that crashed. `BootOutcome.bootedLoader` (stamped by
  `runPrepared`, carried to `LoaderVerdict.bootedLoader`) records which loader actually ran, and
  `loaderDisprovingTheCrash` requires `other.bootedLoader == other.loader`. Without it the guard fires on
  evidence it does not have: the build that booted belongs to a third loader whose stem may differ, so the
  published entry would strip nothing proven bootable — `embeddium-` (Forge/NeoForge) versus `sodium-fabric-`
  is exactly that shape, and it is the one `FilenameStemDeriver.deriveStems` documents — and the note would
  read "<loader> booted a server" of a loader that did not. The Markdown report's Boot cell says
  `SURVIVED (via NeoForge)` when the two differ, so no row claims a boot it never had.
  **Exit 137 on a SURVIVED row is normal, not a kill to investigate** — `ContainerServerRunner` watches for the
  ready-line and stops the container the moment it appears, so every clean container boot exits 137.
  **The crash is not erased:** `bootResult` and the excerpt stay, because the server did crash and that is
  worth diagnosing; only its *standing* changes. The note is **rebuilt**, not appended to — its old text ended
  in "a strong clientside signal", and bolting a correction onto a false sentence is how prose goes stale.
- **The two crash guards layer, and both cost.** The within-loader other-version re-check runs *during* the
  crashing loader's own boot; cross-loader reconciliation runs after every loader is in. So a crash on a
  project whose other loader survives still pays the two extra boots first — loaders are assessed in sorted
  order (`Fabric, Forge, NeoForge, Quilt`), and nothing looks ahead. Deliberate: the extra boots also produce
  the *within*-loader answer, which is the more specific one.
- **LANDMINE — a slug is not an identity; scratch space is owned by `(platform, slug, loader)`.**
  `AttemptDirectory` (this module) builds `<platform>-<slug>-<loader>` and reads it back to its owner, and
  **both halves live there because three callers must agree**: `ClientsideVerifier` (jar-scan downloads),
  `BootVerifier` (staged packs) and the grinder's `BootWorkspaceReaper`, which decides what to *delete* from
  the name alone. They used to agree only by separate string literals happening to match. **Why the
  platform:** the same slug on Modrinth and on CurseForge is two projects, and in the grinder two candidates
  ground by parallel workers (freshness is keyed `(platform, slug)` for the same reason). Staging *wipes* the
  directory before using it and the reaper deletes it afterwards, so an unqualified name let one candidate
  pull the server pack out from under a container the other was still booting. Measured on `creativecore`,
  2026-08-23, two platform runs finishing 71s apart: **NeoForge 26.2.0.66 / MC 26.2 read SURVIVED (exit 137)
  on one platform and CRASHED (exit 1) on the other** — same loader build, same Minecraft, same mod — a
  Fabric boot exited **127** (a shell that could not find the command it was given, because the pack had
  gone), and two re-checks came back INCONCLUSIVE on a file the other run had booted to a ready-line minutes
  earlier. Every one of those is a boot scored as evidence about a mod when it was evidence about a deleted
  directory, and a crash is the one outcome that reaches HIGH. Cut only the *loader* suffix when parsing —
  slugs nest, and a prefix match would claim `creativecore-extras` for `creativecore`. Directories staged
  before this change match no owner and are cleared by the grinder's startup `reapAll()`.
- **Every rung names itself, and only two are decisive** (`BootDecision`). `CRASHED` is reachable from
  `clientOnlyClassMarker`, which no broken harness can fabricate, *and* from the bare exit-code rung, which
  means only "exited non-zero, nothing recognised why" — and afterwards the two were indistinguishable, so the
  grinder published both alike. `Classification.decidedBy` records the rung; `BootDecision.decisive` marks
  exactly four — `CLIENT_ONLY_CLASS`, `LWJGL_ON_A_DEDICATED_SERVER` and `FML_INVALID_DIST` (none of which a
  broken harness can fabricate), plus `OPERATOR_RULE` (a rule reaching CRASHED stated it deliberately) — and the
  grinder's `/as-properties` gate publishes nothing else. **Measured 2026-08-31 against the deployed
  grinder: four of five published boot logs were decided by the exit-code rung**, and one of those mods was
  already in the served list.
  - Three marker sets exist because of those four logs, all **below** `clientOnlyClassMarker` so a mod
    reaching a client class *through* a mixin still reads CRASHED: `mixinApplyFailureMarkers` (an
    `@Inject`/`@Shadow` that found no target — the jar and its Minecraft disagree),
    `loaderSolverFailureMarkers` (Quilt's `Unhandled solver error` / `(0 valid options, 0 invalid options)`,
    a phrasing sharing *nothing* with Fabric's, so `dependencyFailureMarkers` never reached it), and
    `runtimeMismatchMarkers` (`Missing language javafml version [46,)`, `java.lang.module.ResolutionException`
    — a Forge jar staged for a NeoForge boot).
- **LANDMINE — a ladder rung whose bundled id does not resolve matches NOTHING, and used to do so silently
  (2026-09-05).** `BootLogClassifier` holds the ladder's *order* in code and each rung's *pattern* in
  `boot-rules.default.json`, looked up by id. A renamed or deleted id — or one present carrying a pattern
  `Regex()` cannot compile, since `BootRule.regex` is `runCatching { … }.getOrNull()` — yielded
  `Regex("(?!)")` with no log anywhere. Both directions are silent and both are bad: lose a decisive rung and
  every true positive falls through to the exit-code rung, which cannot publish, so the engine merely looks
  like it found nothing; lose a fair-run guard and host trouble stops being excused, which is the
  memory-starved-VM failure already on this engine's record. Now recorded in `missingRuleIds()` and logged at
  ERROR, and `BundledRuleIdsResolveTest` fails the build — the file ships in our own jar, so an unresolved id
  is a packaging fault and belongs to the build, not to a verdict store read weeks later. A bundled file that
  cannot be read *at all* stays a deliberate degradation to "no console rules".

- **A confirmation names the rule that DECIDED it, never one that merely annotated (2026-09-05).**
  `Classification.firedRule` deliberately carries both — a rule stating no verdict rides along on the
  ladder's own decision so its author can see the pattern matched — and `verdictOf` read
  `firedRule ?: decidedBy?.ruleId`, crediting a rule that had declined to state one. The verdict was never
  wrong (CONFIRMED is gated on `BootDecision.decisive`), but the Rule column sent an operator asking "which
  rule excluded this mod?" to the wrong rule, against this module's own standard that a verdict which cannot
  name its evidence cannot be audited. The rule is credited only when `decidedBy == OPERATOR_RULE`.

- **`theGuardOrderIsPinnedAsAWhole` covers all sixteen rungs, and its doc has been wrong three times.**
  Rungs 9, 10 and 12–15 — the decisive pair below `client-only-class`, and the four excuses below them — were
  asserted nowhere, so reordering any of them passed. Extended green (the code was right; the guard was
  absent) and **mutation-verified**: hoisting `mixin-apply-failure` above `client-only-class` now fails. Its
  doc said "eight ordered guards" while listing fourteen, omitted `lwjgl`/`fml-invalid-dist`, and carried a
  stray fragment of an older ladder. **Re-derive the count from `classify`** — that instruction is now the
  first thing the doc says, and this file's own count was wrong for the same reason.

- **OPEN — there are two rule types, and they should become one.** `ConsoleRule`/`ConsoleRuleSet`
  (`ConsoleRules.kt`, speaking `BootResult`, read from the operator's `SPC_GRINDER_BOOT_RULES` file) and
  `BootRule`/`BootRuleSet` (`BootRule.kt`, speaking `Verdict`, read from the bundled
  `boot-rules.default.json`) are two independent implementations of the same idea, introduced by the
  2026-09-04 redesign as a Strangler-Fig step and *not yet collapsed*. Consequences a reader will hit:
  an operator's file and the shipped ladder use different vocabularies for `verdict`, and two parsers exist
  where one would do. Merging them means migrating the operator file's `CRASHED|SURVIVED|INCONCLUSIVE` to
  the four verdicts, which is a breaking change to a documented operator-facing format — hence deferred
  rather than done quietly. Do not add a third.

- **Operator console rules are rung 7 of the ladder** (`ConsoleRules.kt`: `ConsoleRule`, `ConsoleRuleSet`,
  `ConsoleRuleFile`; `BootLogClassifier.classify(..., rules)` returning a `Classification`). A rule maps console
  text to a `BootResult`, so a newly-observed clientside signature is a file edit rather than a release. **Where
  it sits is the whole design**: below the ready-line and below every guard meaning *the mod never got a fair
  run* (timeout, killed/OOM, setup abort, launch failure, loader bootstrap), so a hand-edited file can never turn
  host trouble into a HIGH — and *above* the client-class marker and the exit code, so a rule can both raise FML's
  `for invalid dist DEDICATED_SERVER` on a **zero** exit (the verified gap: that string was in no guard) and
  excuse a console the marker would crash. The order is pinned as a unit inside
  `theGuardOrderIsPinnedAsAWhole` — do not add a sibling test stating it a second time.
  - **An absent verdict and an unreadable one are different, and resolve differently.** A rule stating *no*
    verdict is **undecided**: `ConsoleRuleSet.undecidedVerdict` decides what that means, and it defaults to
    `null` — the ladder decides and the rule merely names itself, i.e. "if none is specified, determine by
    grinder". `SPC_GRINDER_RULE_FALLBACK=inconclusive` opts into the conservative reading for a run where
    unfinished rules are expected. A **misspelt** verdict is always INCONCLUSIVE regardless of that setting
    and is recorded in `errors`: the author tried to state an intention and failed, and a typo must never be
    honoured as one. The opt-in governs undecided rules **only** — a rule that states a verdict always means
    what it says, or turning the setting on would silently rewrite deliberate CRASHED rules. First match in
    file order wins.
  - **Drops and fallbacks go in opposite directions, deliberately.** No id or no pattern **drops** the rule
    (untraceable, or unmatchable); an unreadable verdict **falls back**. Every fallback is recorded, and a
    broken whole file keeps the last good set — which hides breakage, hence `ConsoleRuleSet.errors` on
    `/status`.
  - Reload is a `(lastModified, length)` pair checked on read, not a `WatchService`: `classify` runs once per
    boot, so the stat is free, while a watcher costs a thread, a platform-specific backend (macOS's JDK default
    is itself a poller) and tests that need sleeps. **Landmine:** mtime is second-granular, so two edits inside
    one second that keep the byte count identical are missed — stated rather than hidden, because closing it
    means hashing the file on every call.
  - **Parameter-order landmine:** `bootArtifactSink` must stay the **last** parameter of `runPrepared` and of
    `BootVerifier`'s constructor. Adding `rules` after it silently re-bound every trailing-lambda call site to
    the wrong parameter.
- **Every attempt's evidence is handed to `bootArtifactSink`, from inside `runPrepared`.** `BootArtifacts.collect`
  reads the staged pack's `logs/` and `crash-reports/` alongside the console and returns them as separate
  entries (they disagree usefully: `logs/latest.log` is log4j's file appender, so it holds entries stdout never
  sees and misses the launcher output stdout has). Retention is `BootArtifacts.worthKeeping` — anything but
  SURVIVED — and it lives here so the CLI verb and the grinder cannot disagree about it. Capped per artifact by
  a **seeking** tail read, never `readText`-then-trim, and everything found is named in an `index.txt` whether
  kept or not, because a silently capped set of logs reads as a complete one.
  - **LANDMINE — the hook fires per *attempt*, and there is exactly one call site.** `runPrepared` runs three
    times per candidate (first boot, newest-build re-check, each other-version re-check) and staging wipes the
    attempt directory before each, so anything read after `verify` returns can only ever see the last one. All
    three sites go through the private `BootVerifier.boot`; `onlyOneCallSiteInvokesRunPrepared` pins that,
    because a hook added at two of three would silently lose exactly the re-check evidence a contested crash
    is argued with.
  - `Prepared.Ready.attemptName` derives the `(platform, slug, loader)` tuple from the log file's parent
    rather than carrying three more fields — and stays correct for the other-version re-check, which
    deliberately stages into the *crashing* loader's directory.
- **Landmine — every attempt for one candidate writes the *same* `boot.log`.** Staging wipes
  `<work>/boot/<platform>-<slug>-<loader>` and re-creates it, so the loader-build re-check and each other-version boot
  overwrite the previous console, while the *reported* verdict is usually the first crash. The grinder's
  reaper keeps exactly that one file, so the log a `HIGH` is diagnosed from would be a different boot's.
  `BootOutcome.console` + `restoreDecisiveConsole` (called at the end of `verify`) put the decided outcome's
  own console back. Any new re-check path must leave that call last.
- **`LoaderVersionPolicy` (seam) + crash re-check.** `BootVerifier` takes a *policy*, not the concrete
  `LoaderVersionResolver`: `preferredVersion` is what gets booted, `latestVersion` is the authoritative newest.
  The default resolver answers both identically. A caller may prefer an **older** build it already has installed
  (the grinder's `CachedLoaderVersions` does, to avoid a ~150 MB install per loader release) — which is only safe
  because of the guard: **`latestVersion` alone drives the support gate**, and a CRASHED outcome on a
  non-newest build is re-booted on the newest one before it may stand (`recheckCrashOnNewestVersion`).
  **Why it must exist:** a mod needing a newer loader than the cached build fails to load, exits non-zero, and
  the classifier reads CRASHED → a server-safe mod published as a **HIGH-confidence clientside mod**. The two
  decisions are pure and unit-tested (`shouldRecheckCrash`, `reconcileRecheck`) because `verify` itself needs an
  `ApiWrapper` + real generation + a running server — same split as `outcomeFor`. **Landmine:** an INCONCLUSIVE
  re-check must never clear the crash (a flaky second boot is not evidence); only a clean boot on the newest may.
- **LANDMINE — `"<Loader> is not available for Minecraft X"` in a boot log does NOT mean the loader lacks a
  build.** `default_template.sh:316` raises it when `FABRIC_AVAILABLE != 200`, and that variable is an HTTP status
  from a `curl`/`wget` probe — which cannot succeed in the grinder's `--network none` boot container. The message
  therefore means *"I could not check"*, not *"unsupported"*. Measured 2026-07-30: a `LoaderSupportMemory` that
  treated it as unsupported marked Fabric unusable for **22 Minecraft versions** (1.19.2 through 26.2 — i.e. all
  of them) within minutes, and was reverted. `0.19.3` is a genuine current Fabric *loader* version, so SPC's
  version plumbing is fine; Fabric installer versions (`1.1.2`…) are a separate series and are not what is passed.
  **FIXED 2026-07-30 in the templates (all three shells):** `setupFabric` now settles the launcher from disk
  (`fabric-server-launcher.jar` / `fabric-server-launch.jar`) *before* probing, so an offline pack that already has
  its launcher never asks the network. **Landmine — the fix has two halves, and the first attempt only had one:**
  the disk branch must **fall through** to the `SERVER_RUN_COMMAND="${JAVA_ARGS} -jar ${LAUNCHER_JAR_LOCATION}
  nogui"` assignment at the end of the function. The first cut `return 0`-ed as soon as it found the jar, jumping
  over that assignment, so the pack launched `java -Dlog4j2... do_not_manually_edit` (the untouched placeholder)
  and died with `Could not find or load main class do_not_manually_edit` — *past* every ordering assertion the
  guard test made. `-api`'s `ScriptTemplateContentTest` now **executes** the extracted bash `setupFabric` against a
  staged launcher jar with network calls stubbed to fail, which is the only assertion that catches this; verified
  to fail when the `return 0` is reinstated.
- **LANDMINE — the exit code is NOT a reliable crash signal; the console decides.** Measured 2026-07-30: NeoForge's
  **ServerStarterJar prints a mod-loading crash in full and then exits `0`**. Because `classify` keyed CRASHED on a
  non-zero exit, `modelfix` — whose console holds a textbook `NoClassDefFoundError: net/minecraft/client/Minecraft` —
  came out INCONCLUSIVE, and **no verdict in a 517-verdict store ever reached HIGH**: the expensive boot was running,
  crashing correctly, and being discarded. `clientOnlyClassMarker` now returns **CRASHED from the console alone**,
  ahead of the exit-code logic, and that is what finally produced the engine's first `HIGH(boot:CRASHED)`. It is safe
  to trust over the exit code precisely because no environment failure can fabricate it — but it stays **subordinate
  to the timeout and killed/OOM guards**, so host trouble can never manufacture a HIGH (tests pin both directions).
  A separate fix propagates the server's real status through the start scripts (`SERVER_EXIT_CODE`, all three
  templates), which is correct and useful for users' service wrappers — it just cannot rescue a loader that reports
  success for a crash, so **never make CRASHED depend on the exit code alone again**.
- **Missing dependencies must not reach a boot at all.** `downloadWithDependencies` collects every required dependency
  it could not stage (unresolvable ref, no usable file, failed download — the first of which used to be a *silent*
  `continue`), and `refuseForMissingDependencies` then aborts staging with a named reason instead of booting. A loader
  that rejects a mod for missing dependencies never runs the mod's code, so the run cannot speak to sideness; it just
  produces a failure that looks like a crash. Measured across 112 kept boot logs: **36** failed exactly that way, the
  largest single failure class, each burning ~70 s to learn nothing. `BootLogClassifier` keeps a matching backstop
  (`dependencyFailureMarkers` → INCONCLUSIVE) for deps that go missing despite staging.
- **A pack whose own jars contradict each other backtracks instead of booting** (`DependencyBacktrack`,
  2026-09-06). Staging resolved every dependency *alone* — the newest file that project publishes for the
  pack's Minecraft — and never asked whether the resulting **set** was coherent. Where it is not, the loader
  refuses the pack, ~70 s of container is spent, and the *candidate* wears the INCONCLUSIVE: the
  "never got a fair run" shape, one layer earlier than every guard that already covers it.
  **The live case, `Modrinth/zoomify` on Quilt / Minecraft 1.20.5:**
  `yet_another_config_lib_v3-3.6.6+1.20.6-fabric.jar` is tagged for 1.20.5 *and* declares
  `"minecraft": "~1.20.5"` — so neither selection nor the descriptor gate objects — while demanding
  `"fabric-api": ">=0.100.0+1.20.6"`. Verified against the live API: Modrinth publishes exactly **four**
  fabric-api files for 1.20.5, `0.97.5` through `0.97.8`. **Staging *more* cannot fix that pack**; only an
  older YACL can, and `3.4.2+1.20.5` requires nothing but `fabric-resource-loader-v0`.
  - **What it never does, and each omission is load-bearing.** It never demotes the *candidate* (the subject
    of the experiment; swapping it answers a question about a different mod). It never **refuses** — no
    scanner, an unreadable jar, a version the platform never reported, a range `VersionConstraint` cannot
    parse, an exhausted budget: every one of them proceeds to the boot exactly as before, because a gate
    refusing on doubt is the mass-INCONCLUSIVE shape this module has already paid for twice. It ignores
    **optional** dependencies (the loader loads the mod without them, so one being old cannot be why a pack
    is refused) and requirements naming something **not staged at all** (that is
    `refuseForMissingDependencies`' case, and demoting over a gap dropping a jar cannot close burns the
    budget for nothing).
  - **LANDMINE — a backtrack re-stages from scratch, so it re-downloads everything.** `zoomify` needs
    **seven** (YACL ships 3.6.6 down to 3.6.0 tagged for 1.20.5, every one a `+1.20.6` build with the same
    demand), and `MAX_BACKTRACKS` is 10 for that reason. Still cheaper than the wasted boot it replaces;
    skipping files already on disk is an optimisation to make **only if the rate warrants it** — 2 of 250
    live verdicts reached `DEPENDENCY_FAILURE` when this was written. Measure before changing it.
  - The judge reads the staged jars through the same `ModScanner.scannerFor` staging already uses, which on
    a Quilt pack is `QuiltPackScanner` — the one that merges the Fabric descriptor most Quilt mods actually
    ship, so a Fabric-only dependency is not invisible to it. Jars whose descriptor could not be read are
    dropped on `descriptorRead`: `ScannedMod`'s fallback is indistinguishable by value from a mod that
    declared nothing.
  - `InjectedDependency.version` exists for this: a descriptor names a **mod id and a range**, never a file,
    so the judge needs what the platform published each staged file as. Carrying it there avoided threading
    a second accumulator through every level of the staging recursion.
  - **LANDMINE — that version is a platform *release name*, and comparing it as a number invented conflicts
    everywhere (2026-09-07, one day after the backtrack shipped).** CurseForge has no version field, so
    `CurseForgePlatform.toModFile` fills `ModFile.version` with the author-typed `displayName` — documented
    in place as "often decorated". `VersionConstraint.numbersOf` maps a digit-less component to `0`, so
    `Balm 26.2.0.7` read as `[0, 2, 0, 7]` and `balm-fabric-26.2-26.2.0.7.jar` (everything before the first
    `-`) as `[0]`: below almost any range, on essentially every CurseForge dependency. `looksLikeVersion`
    did not catch it — it asks only whether a digit is present, and those hold four.
    **Measured on the live daemon that day: `1014` `re-staging ... without it` lines and `146`
    `publishes no ... file for Minecraft` lines in one day, against `4` real staging failures, ending in 47
    published `ERROR` verdicts** reading *"Required dependency unavailable"* — for files the CurseForge API
    returns on request, correctly loader-tagged (`misc/cf-dependency-probe.sh` is that probe, and its header
    carries the numbers). The demote loop exhausted each project's file list and `withoutExcluded` then left
    `pickDependencyFile` nothing to pick. `readableVersion` now gates `satisfies` on the version side —
    every dot-separated component of the core numeric, `v` prefix allowed — so prose accepts, exactly as an
    unreadable *constraint* always has. **Do not "improve" this by extracting a version out of a release
    name**: `Create 6.0.10 for NeoForge 1.21.1` offers two readings four major versions apart, and guessing
    is the silently-plausible-value trap. A conflict spelled in an unreadable version is missed instead,
    which costs one boot where inventing one costs a published verdict.
  - **A jar-in-jar library counts as staged when the set is judged** (`BundledJars.versionsIn` +
    `BootVerifier.nestedVersions`, 2026-09-08). `dependencyToDemote` read `modsDir.listFiles()` and
    `InjectedDependency.version`, and a nested library is in neither — not a top-level file, never published
    by a platform — so a requirement contradicting *its* version looked like a requirement naming something
    absent, which `DependencyBacktrack.conflicts` skips by design. Live: `CurseForge/createaddition` on
    NeoForge 21.1.250 / MC 1.21.1 logged *"Mod ID: 'ponder', Requested by: 'create', Expected range:
    '[1.0.82,)', Actual version: '1.0.64'"* while `ponder` appears in none of that verdict's four
    `stagedDependencies`. Nested entries sit **under** the top-level ones (a bundled copy fills a gap, never
    overwrites the build staging chose — which is also the one a demotion acts on), and an id bundled at two
    different versions, within a jar or across two, contributes **nothing**: which copy a loader picks is its
    own resolution behaviour, and no opinion costs a missed conflict where a wrong one manufactures a demotion.
  - **A staging refusal names its evidence, like a boot verdict does** (`UnmetReason`, 2026-09-07). Five
    ways a dependency reaches `unsatisfied`, three of which printed the bare slug: an operator could not
    tell *the project publishes nothing usable* from *the download died* from *staging dropped every build
    itself*. `backtrackReason` re-runs the pick over the **unfiltered** list to separate the last one, which
    had been reporting the exact opposite of what happened. **The reason travels beside the name, never
    inside it** — `unsatisfied` is a `Map<name, reason>` so the `waystones` dedupe (one mod missing by both
    routes is one entry) survives the two routes failing differently.
- **Dependencies come from BOTH the platform and the jar manifest, and the two are trusted differently.**
  `downloadWithDependencies` resolves `ModFile.requiredDependencies` as before, then scans each staged jar and
  resolves what its manifest declares and the platform never mentioned — the case Fabric API most often falls
  into. `KnownModIds` bridges the vocabularies (a manifest says `fabric`, Modrinth wants `fabric-api`,
  CurseForge wants `306612`); `VersionConstraint` matches a declared range against `ModFile.version`.
  - **LANDMINE — Fabric API is one project shipped as ~45 modules, and descriptors depend on the *modules*.**
    A manifest says `fabric-resource-loader-v0`, `fabric-block-getter-api-v2`, `fabric-rendering-fluids-v1`;
    neither platform publishes a project under any of those names, so Modrinth's slug guess 404s, CurseForge
    refuses to guess, and the single most common dependency in the ecosystem went unstaged — the mod booted
    without it, the loader refused the pack, and the *candidate* wore the INCONCLUSIVE. Reported live
    2026-09-01, and the same shape as the recorded Quilt solver failure `fabric-resource-loader-v0 versions
    [*] (0 valid options, 0 invalid options)`.
    - **A rule, not a table, and that is load-bearing.** The API-version suffix moves between releases: the
      current source tree ships `fabric-resource-loader-v1` and `fabric-block-getter-api-v2`, while the corpus
      is full of older mods declaring `-v0` — ids in no tree today. A list snapshotted from the repository
      would be wrong for exactly the historical mods this fixes. `fabric-<something>-v<digits>` is the stable
      shape; measured against the 46 directories of `FabricMC/fabric` it matches **44**, the two misses being
      `fabric-api-bom` and `fabric-api-catalog` (build artifacts, not runtime modules). `fabric-api-base` and
      `fabric-renderer-indigo` carry no suffix and are listed explicitly.
    - **`notFabricApi` is why a bare pattern is wrong.** lucko's `fabric-permissions-api-v0` (plural) matches
      the shape and is a separate project; Fabric API's own is `fabric-permission-api-v1` (singular), one
      character away, and must still resolve. Verified against lucko's `fabric.mod.json`, 2026-09-01. Keep
      that set to ids **observed** colliding — guessing at more re-creates the un-pinned table `KnownModIds`
      exists to avoid. A `fabric-` prefix alone proves nothing: `fabric-language-kotlin` is its own project.
    - **QSL is the same shape, and is handled by its own rule.** Quilt Standard Libraries also ships as
      many modules, and a Quilt descriptor names them (`quilt_resource_loader`, `quilt_networking`). Verified
      2026-09-01 across all 47 `quilt.mod.json` files in `QuiltMC/quilt-standard-libraries` (branch 1.21.5):
      **33 distinct `quilt_*` ids**, all lowercase-with-underscores and **none** carrying an API-version
      suffix — so `fabric-<x>-v<digits>` matches no QSL id, which is why the Fabric rule left this open
      rather than closing it by coincidence. The QSL rule is `^quilt_[a-z0-9_]+$` → `qsl` / `634179`, with
      `notQsl` holding `quilt_loader` (the loader, not a module).
      **`quilt_base` was the one QSL module both layers called "the platform", and that is fixed** (Griefed's
      call, 2026-09-01, after it was flagged rather than changed). It is QSL's base module shipped by QFAPI —
      `library/core/qsl_base`, and `quilt_base_testmod` depends on `["quilt_loader", "quilt_base"]` — so
      `-api`'s `QuiltScanner.dependencyExclusions` no longer strips it and it is no longer in
      `BootVerifier.environmentProvidedIds`. Only `quilt_loader` is the runtime, which is the same line
      `FabricScanner` draws by excluding `fabricloader` and never `fabric`. Two existing `-api` expectations
      had to change, which is the stop-and-flag signal working as intended: it was raised, decided, and the
      commit is labelled `fix:`. One row in `API-BEHAVIOUR-CHANGES.md` — a Quilt jar's scan now returns one
      more `ModDependency`, and a QFAPI/QSL jar becomes rescuable into a pack that had disabled it.
    - **One mod names several modules, and they must collapse to one download.** `visited` is claimed on the
      *ref*, so the first module resolves Fabric API and the rest short-circuit. That matters beyond the
      wasted fetch: it is the B6 shape, where one jar reachable under several names double-counted toward
      `MAX_INJECTED_DEPENDENCIES` and refused packs that were within the cap.
  - **LANDMINE — the refusal split is the whole safety property, and it is structural.** *(Superseded
    2026-09-06 — it now keys on mapping **confidence** rather than on how far the lookup got; see the entry
    above. The reasoning below is why the split exists at all and still holds.)* A platform ref is a
    project the author linked; a manifest id is a bare string that may name something *bundled inside another
    jar* (`fabric-api-base` ships inside Fabric API), provided by the loader, or optional in practice. Since
    `refuseForMissingDependencies` scores a refusal INCONCLUSIVE, treating every unresolvable manifest id as a
    refusal would convert a large share of *working* boots into INCONCLUSIVE. So `unmapped` never reaches that
    function at all — not via a flag, via a separate collection — while `unsatisfied` (platform misses, and
    manifest ids that mapped and then failed to stage) still refuses.
  - **`VersionConstraint` fails towards ACCEPT, always.** A constraint it cannot parse must never refuse: a
    refusal is indistinguishable from the dependency being genuinely unsatisfiable, so a grammar gap would
    present as a catalog-wide mass-INCONCLUSIVE event. `looksLikeVersion` gates every comparison because
    `numbersOf` maps a digit-less component to `0` — **two** separate branches shipped that bug during
    development (a bare clause, then a bare `.x`), both invisible to inspection, both found by
    `VersionConstraintFuzzTest`, which sweeps 30 malformed shapes and asserts not one refuses. Do not add a
    comparison site without gating it.
  - `BootCandidateSelector.pickDependencyFile` treats a constraint as a **preference**: it narrows, then falls
    back to the whole set. Returning `null` where it used to return a file would turn a bootable candidate into
    a refusal.
  - **Attribution annotates, never downgrades** (`DependencyAttribution`, `BootVerifier.attribute`). A crash
    naming an injected dependency records `blamedDependency` and requeues that project — the candidate still
    crashed a server in the configuration a real pack produces, and downgrading on a string match trades a false
    positive for a *lost true positive*. `attributionNeverChangesTheBootResult` pins it. Blame needs a crash
    marker or stack frame (a name appears in every "loading mod" line) and stands down when the candidate is
    named anywhere in the same crash context.
- **LANDMINE — `pickDependencyFile` prefers an *obtainable* file, and obtainability outranks the loader
  match (2026-09-04).** `pickForLoader` was `firstOrNull { loader in it.loaders && mc in it.minecraftVersions }`
  and never asked whether the file could be downloaded, so a distribution-locked build
  (`downloadUrl == null` — the author's opt-out) was picked over an obtainable one and the dependency was
  reported unmet. Reported as *"Required dependency unavailable for Quilt / Minecraft 1.20.4: 306612"*,
  CurseForge's Fabric API.
  **Obtainability beats the exact-loader preference on purpose:** Quilt genuinely runs Fabric mods, so an
  obtainable Fabric build is a working dependency while a locked Quilt build is nothing at all — a locked
  exact match otherwise shadows the very fallback that exists for libraries publishing Fabric-only files.
  **Still a preference, never a filter.** The last arm returns a locked file when every candidate is locked,
  so the refusal reads "distribution-locked" — true and actionable — rather than "publishes no <loader> file
  for Minecraft X", which would be false. Returning `null` where a file exists turns a diagnosable refusal
  into a misleading one, which is the same reason the version constraint is a preference here.

- **Quilt dependencies fall back to the Fabric build** (`LoaderCompatibility.alsoRuns`). Quilt deliberately
  runs Fabric mods, which is why the canonical dependency of a Quilt mod is **Fabric API — a project publishing only
  Fabric-tagged files**. Strict loader matching dropped it silently: measured 2026-07-30, **210** dropped
  dependencies, all but 44 on Quilt, `P7dR8mSH`/`306612` (Fabric API) the most-dropped ref. The map is deliberately
  one-way and minimal — Fabric cannot load Quilt mods, so guessing wider would stage a jar the loader cannot use.
- **NeoForge runs Forge builds on Minecraft 1.20.1, and on nothing else** (`LoaderCompatibility`, 2026-09-06).
  NeoForge 20.1.x is a fork of Forge 47 that kept the `net.minecraftforge` packages, the `javafml` language
  provider and `META-INF/mods.toml`, so there a Forge jar and a NeoForge jar are *the same file*; the package
  rename landed with 1.20.2 and ends it. **State it as the one version, never as a lower bound** — a range
  would boot Forge jars under NeoForge 1.20.2+, where FML rejects them (`Missing language javafml version
  [46,)`, already a `runtimeMismatchMarkers` entry) and the failure is scored against the *mod*.
  - **The fact has one home because it used to have two.** `JarSelfDeclaration.alsoRuns` (the pre-boot
    descriptor gate) and `BootCandidateSelector.fallbackLoaders` (dependency selection) were separate
    `Quilt to Fabric` maps answering the same question, so only one of them could ever have learned this.
    `LoaderCompatibility.alsoRuns(loader, minecraftVersion)` is now both. **It takes the Minecraft version on
    purpose:** the NeoForge claim is meaningless without one, and an overload that omits it would silently
    re-open the gap.
  - **What it cost, live 2026-09-06:** `CurseForge/mantle` published an `ERROR` row — *"Refusing to boot
    NeoForge on Minecraft 1.20.1: `Mantle-1.20.1-1.11.117.jar` carries only Forge descriptor(s), so it is not
    a NeoForge mod"* — for a file CurseForge ticks Forge **and** NeoForge and which had reached a ready-line
    under Forge minutes earlier in the same run. A verdict about the grinder's own descriptor table,
    published as a verdict about the mod. The dependency half was the same gap one step earlier: a dependency
    publishing only Forge files was unpickable for a NeoForge 1.20.1 boot, and `refuseForMissingDependencies`
    scores an unstageable requirement INCONCLUSIVE, losing the whole boot.
  - Both concessions stay one-way: Forge never gained the ability to read `META-INF/neoforge.mods.toml`, and
    a real NeoForge build still beats the Forge fallback wherever a project publishes one.
- **A Sinytra Connector *placeholder* is a Fabric mod, and the Forge scanner reads a stub** (2026-09-06).
  `JarSelfDeclaration.isConnectorPlaceholder` reads `[properties] "connector:placeholder" = true` out of
  `META-INF/mods.toml`, and `MetadataScanner` then scans such a jar as **Fabric**. Read from the live
  `continuity-3.0.0+1.20.1.forge.jar`: the `mods.toml` exists only to get the file past Forge's mod discovery
  (version-less dependency entries on `connectormod` and `fabric_api`), while the `fabric.mod.json` beside it
  holds the real mod — `"environment": "client"` included.
  **Measured live 2026-09-06:** that project's Forge row read `jarScan=SERVER_OR_BOTH` and
  `declared=CONTRADICTORY` against a platform declaring `client_side=REQUIRED`, while the *same project's*
  Fabric row read `CLIENT` off the identical descriptor. The contradiction was manufactured by the scanner
  choice — and `ClientsideVerifier.declaresServerSupport`, the same predicate, is what arms the other-version
  crash re-check, so a false one costs up to three boot budgets (~45 min) per armed candidate.
  - **It substitutes the scanner's *input*, not the dispatch.** The loader→scanner choice still goes through
    `ModScanner.scannerFor`, so the `MetadataScanner`/`ModListCompiler` drift documented at the top of this
    file cannot come back; only the question changes, because a placeholder is not the loader it is tagged for.
  - **Keyed on the marker, never on carrying both descriptors.** A genuine multi-loader jar ships a real
    `mods.toml` beside a real `fabric.mod.json` and each speaks for its own loader; hijacking those would
    answer a Forge question with a Fabric answer.
  - **The boot is still attempted** (Griefed's call): a working Connector setup should still be verified, and
    the row's INCONCLUSIVE then stands on its own evidence rather than on a false contradiction.
  - **Why that boot failed is NOT ours, and the staging was right.** The grinder staged the newest Sinytra
    Connector (`1.0.0-beta.49+1.20.1`) and the newest Forgified Fabric API (`0.92.6+1.11.15+1.20.1`) — the
    only ones Modrinth publishes for 1.20.1 — and Connector under Forge 47.4.23 still logged *"Dependency
    resolution found 0 candidates to load"* and never converted the jar, leaving Forge to read the stub's
    version-less ranges and refuse. Do not "fix" this by staging more dependencies; they were all there.
- **`allowModDistribution=false`** CurseForge files arrive with `downloadUrl=null` (`ModFile.locked`) and
  are **not obtainable** — the author opted out of third-party distribution, so there is nothing to fetch.
  `HttpJarDownloader` returns `null`, `ClientsideVerifier` records `JarScan.DEFERRED`, and the staging
  refusal says so and points at Modrinth, where the same project's files carry a URL.
  - **HISTORY — Playwright and a headless Chromium used to fetch these anyway, and were removed 2026-09-02
    (Griefed's call).** The route existed only to circumvent the distribution block, and by the end it did
    not work at all: CurseForge is behind a Cloudflare challenge the headless browser does not clear, so
    every attempt died on `Timeout 60000ms exceeded` *after* Chromium had launched, while a plain HTTPS
    fetch of the same file page returned **403 with challenge markers on any user agent**. The host was
    never the problem — the installer's launch probe rendered a page as the service account and the cache
    held both `chromium-1234` and `chromium_headless_shell-1234`.
  - **What it cost, measured:** `driver-bundle-1.62.0.jar` is **192.9 MB** of bundled node binaries for five
    platforms, reaching every artifact because this module declared `api(libs.playwright)`. Removing it took
    `serverpackcreator-app-dev.jar` from **274.7 MB to 77.8 MB** — the driver code itself was only 3 MB; the
    bundle was purely node runtimes. Every SPC user carried that for one CLI verb.
  - **Do not reintroduce it.** Regular downloads for non-blocked CurseForge content and all of Modrinth never
    needed a browser, which is why removing this cost no working coverage. Gone with it:
    `BrowserDownloader`, `BrowserRouteBreaker` (a circuit breaker added hours earlier to bound the failing
    route's cost — obsolete once the route went), `selectDownloader` (one route needs no router, so
    `JarDownloader.kt` now emits no `JarDownloaderKt` facade at all), the installer's browser stage and
    `--skip-browser` flag, and the CI job's `playwright install-deps`. `JarDownloaderRoutingTest` asserts
    `BrowserDownloader` is **absent from the classpath**, so a half-revert fails rather than lingering.
- **The descriptor gate is a *filter*, not a veto — a jar's range that excludes the newest tagged version
  re-selects rather than refuses (2026-09-03).** `BootCandidateSelector.pickBootableCandidate` can only see
  platform metadata, because the jar is not downloaded until after selection; `refuseForSelfDeclaration`
  then reads the descriptor and may contradict the pick. Refusing there threw the whole candidate away even
  when a version *both* sources accept was sitting in the same list.
  **The measured case is JEI, and the descriptor is upstream-wrong rather than misread.**
  `jei-1.21.1-forge-19.52.0.422.jar` is tagged on both platforms for Minecraft 1.21 **and** 1.21.1 while its
  own `META-INF/mods.toml` declares `versionRange="[1.21, 1.21.1)"` — a Maven range whose `)` excludes the
  version the file is named after. JEI's `gradle.properties` on its 1.21.1 branch confirms the intent:
  `minecraftVersion=1.21.1` sits beside `minecraftVersionRange=[1.21, 1.21.1)`, i.e. the range is built as
  `[start, thisVersion)` where it should be `[start, nextVersion)`. Both halves of our reading are correct —
  `ForgeTomlScanner.getVersionRange` is verbatim, `VersionConstraint.mavenRangeHolds` trims its bounds exactly
  like Maven's own `parseRestriction` — so **do not "fix" the parser**; this class of range is genuinely
  self-excluding and common, since authors routinely tick `X` and `X.1` while the toml covers only `X`.
  `reselectOnMinecraftContradiction` re-stages once on `BootCandidateSelector.newestVersionSatisfying`, and
  keeps the original refusal when no tagged version satisfies the jar.
  **Why it matters more than one lost boot:** a staging refusal publishes `BootResult.INCONCLUSIVE`, which
  overwrites a decisive verdict — the same harm shape as the missing-runtime-image outage, except permanent
  instead of windowed.
  **Landmine — `Prepared.Failed.declaredMinecraftConstraint` is set only for the Minecraft disagreement**, and
  the predicate is re-asked rather than inferred from `JarSelfDeclaration.contradiction` being non-null: that
  same string also reports *a jar carrying the wrong loader's descriptor*, which no other version can fix. Widen
  it and a NeoForge-tagged Forge jar will re-stage down its whole version list, once per version, learning
  nothing each time. The retry calls `stageBootPack`, never `prepareBootPack`, so a second contradiction
  surfaces instead of looping.
- **A comma is two different separators in a Maven range, and reading it as one made every union
  unsatisfiable (2026-09-06).** Inside a bracketed range it separates lower bound from upper; *between*
  ranges it separates alternatives, which Maven documents (`(,1.0],[1.2,)`) and Forge/NeoForge accept in a
  `versionRange`. `mavenRangeHolds` assumed the first reading always, so `[1.20.3],[1.20.4]` parsed as one
  range from `1.20.3]` to `[1.20.4` — and `numbersOf` maps the bracketed component to `0`, making the upper
  bound `0.20.4`. Executed before it was fixed: that constraint refused **both** versions it lists, `26.2,26.3`
  refused both of its, and Maven's own example refused everything.
  Found in the live grinder's ERROR rows — `distanthorizons` refused for a 1.20.4 pack, `mru` for a 26.2 one.
  `unionMembers` splits on top-level commas by tracking bracket depth, since the two commas are spelled
  identically and only nesting tells them apart; a bare `26.2,26.3` is read as alternatives too. An unbalanced
  string still yields one member, so a malformed constraint reaches `mavenRangeHolds` as before and still
  resolves to accept.
  **It failed safe** — a refused boot publishes nothing, so no wrong exclusion ever reached a user — and the
  cost was coverage. Neither `readsMavenRanges` nor `VersionConstraintFuzzTest` caught it: the fuzz test
  sweeps *malformed* constraints, and this one is well-formed.

- **When the jar and its platform tags share no Minecraft version, the JAR wins and the pack is bumped**
  (Griefed's call, 2026-09-06). `reselectOnMinecraftContradiction` reconsidered only *tagged* versions, which
  rescues JEI (tagged 1.21 and 1.21.1, declaring `[1.21, 1.21.1)`) and does nothing for a file tagged for
  exactly one version its own descriptor excludes — `moonlight-1.20.4-2.9.9-forge.jar` is tagged 1.20.4 and
  declares `[1.20,1.20.2)`, so the candidate was refused outright. It now falls back to
  `BootCandidateSelector.newestReleaseSatisfying` over SPC's real Minecraft release list.
  **The jar is the better authority, not merely a different one:** the loader enforces that range at runtime,
  so booting inside it is what gets the mod loaded, while booting at a version the author ticked on a web form
  gets it rejected by FML before it runs. Only the pack's Minecraft version moves.
  - **LANDMINE — `constrainsAnything` is what stops this relocating every candidate.** `VersionConstraint`
    accepts anything it cannot parse, deliberately, so an empty, wildcard or unreadable descriptor would
    otherwise "satisfy" the newest Minecraft in existence. It is decided by asking whether the constraint
    excludes anything in the set about to be searched — never by re-detecting which shapes the parser
    tolerates, which would be a second copy of that grammar drifting from the first.
  - Still exactly one retry, through `stageBootPack`, so a second contradiction surfaces rather than loops.

- **LANDMINE — a CurseForge file with NO loader tag is *unknown*, not incompatible (2026-09-06).**
  CurseForge had no modloader facet before Minecraft 1.13 — everything was Forge, so nothing was tagged —
  and `pickForLoader` asks `loader in it.loaders`, which no empty set satisfies. Such a dependency was
  therefore unpickable and the boot was refused.
  **Measured against the live API with Griefed's key:** `modtweaker` declares dependency `253211`, which
  resolves to **mtlib** and returns 7 obtainable 1.12.2 files, *all* carrying `loaders=[]`. That is what
  *"Required dependency unavailable for Forge / Minecraft 1.12.2: mtlib"* was. Not rare: mtlib 15/15 files
  untagged, `iron-chests` 106/138, `waystones` 70/494, `crafttweaker` 28/500 — essentially all pre-1.13 —
  plus modern stragglers (`journeymap`, 5 files at 26.1.2). `jei` and `athena` have none.
  `pickUntagged` is the **last** arm of `pickFrom`, so a file whose author stated a loader always wins and
  this can only add a pick where there was none. The Minecraft version stays exact, and a file tagged for a
  *different* loader is still refused — a tag is a statement, an empty set is the absence of one.
  - **The first diagnosis of this report was wrong, and only the live API showed it.** The refusal names a
    *slug* (`unsatisfiedLabel` resolves the ref), which reads like a manifest mod id — but
    `modtweaker-4.0.20.11` declares `253211`, a **numeric** ref, so it came from the platform path and never
    touched the manifest-id mapping. A cause that survives a code read can still be the wrong one.
  - **`pickBootableCandidate` got the same fallback** (Griefed's call, same day), so a project whose files
    are *all* untagged is ground rather than skipped: `mtlib` as a Forge candidate returned **nothing**
    before and `MTLib-3.0.7.jar @ 1.12.2` after. Both arms go through `newestOf`, sharing the ordering and
    the availability gate.
    **The safety argument differs from the dependency half and is worth keeping.** An untagged file picked
    for the wrong loader could stage a jar that loader ignores, boot cleanly and publish a false `CLEAR` —
    the worst outcome this engine has, because it claims proof about a mod that never loaded. Two things
    prevent it: `loaderVersionAvailable` covers the dominant case (untagged is overwhelmingly pre-1.13,
    where Fabric and Quilt have no builds, so only Forge is reachable and untagged *means* Forge), and for
    anything newer `refuseForSelfDeclaration` reads the downloaded jar's descriptor before the boot.
    **Measured consequence, verified live on `iron-chests`:** a Fabric attempt now picks an untagged 1.16.2
    Forge jar and is refused by the descriptor gate, where before it was refused at selection. Same verdict
    class, a more precise reason, one download's worth of extra work — and that project publishes no
    Fabric-tagged file at all, so the attempt was never going to succeed.

- **THE REFUSAL SPLIT KEYS ON MAPPING CONFIDENCE, NOT ON HOW FAR A LOOKUP GOT (2026-09-06).** This
  supersedes the "mapped-then-unstageable refuses, unmappable does not" rule described further down, and it
  is the safety property that rule was reaching for — now stated directly instead of emerging from distance.
  - `KnownModIds.mappingFor` returns a **`ModIdMapping`**: `Alias` (the table, or a recognised Fabric API /
    QSL module shape — a project we *know* the id names), `Guess` (the optimistic slug), or `None`.
    `refFor` is just `mappingFor(...).ref`, for the call sites that only dedupe.
  - **An alias refuses; a guess never does, at any stage** — not when it resolves to a project publishing
    nothing usable, not when the download then fails. `ManifestDependencyPlan.Stage` carries `confident` so
    the download branch obeys the same rule as the plan.
  - **Why: being *almost* resolvable must not be worse than being unknown.** `xaeros-world-map` was refused
    for `xaerolib` because a real Modrinth project of that name exists but publishes nothing tagged Quilt or
    26.2 — the guess hit, so it refused, where an outright miss would have been allowed. That trap is now
    closed by construction.
  - **And it is what makes a guess safe to offer on CurseForge**, which had none precisely *because* a guess
    could refuse. Every manifest-declared dependency of a CurseForge candidate was therefore unresolvable
    unless it was one of four aliases — measured 2026-09-06: `mtlib`, `crafttweaker`, `jei`, `athena`,
    `flywheel` and `xaerolib` all mapped to nothing. Reported by Griefed via `modtweaker` on Forge/1.12.2,
    whose `mtlib` CurseForge publishes under exactly that slug.
  - `CurseForgePlatform.resolveDependency` began with `nativeRef.toLong()`, so any non-numeric ref threw and
    was caught as unresolvable. It now falls back to `modIdForSlug`, whose request is **byte-identical** to
    the one `resolve` has always used (`/mods/search?gameId&classId&slug=`) and reads `data[0].id` the same
    way — so the shape is proven by every CurseForge candidate already resolving through it. Exact-match on
    the slug, never a text search, so it finds the project the id names or finds nothing.
  - **The premise that changed, stated so nobody re-litigates it from the old rationale:** CurseForge was
    given no guess because a guess cost a refusal. It no longer does. The four guards asserting `null` for
    CurseForge were rewritten, three of them intent-preserving (`fabric-permissions-api-v0`,
    `fabric-language-kotlin` and `quilt_loader` must not be claimed for Fabric API or QSL — they are now
    guesses at their own slugs, which refuse nothing).

- **THE RESULT SYSTEM IS FOUR VERDICTS, AND EVERY CLIENTSIDE RULE LIVES IN A FILE (2026-09-04).** Read this
  before touching `BootLogClassifier`, `ClientsideVerifier` or `boot-rules.default.json`.
  - **`Verdict { CONFIRMED, CLEAR, ERROR, INCONCLUSIVE }`** replaced `BootResult` × `Confidence`. The pairing
    conflated *what happened* with *how sure are we*, and could not express whether the grind ran at all.
    **`ERROR` is that missing verdict** — no runtime image, a staging refusal, a failed download — and its
    absence is what let a host-wide outage publish as one INCONCLUSIVE per candidate, overwriting decisive
    verdicts. `CLEAR` is the other half: a clean boot that matched nothing is *proven server-safe*, and
    collapsing it into INCONCLUSIVE throws away the most expensive signal the engine produces.
  - **Only a rule reaches CONFIRMED, and only from a rung `BootDecision.decisive` marks.** The bare exit-code
    rung means "exited non-zero, nothing recognised why" and can no longer publish; it is the rung that had
    27 of 43 published HIGHs resting on no decisive evidence.
  - **`boot-rules.default.json` (in `src/main/resources`) IS the ladder.** The eleven hardcoded marker groups
    live there now; `BootLogClassifier` compiles its patterns back out by rule id via `bundledPattern`, so
    one edit reaches the engine and the two copies cannot drift — the `MetadataScanner`/`ModListCompiler`
    failure one level up. **Order is precedence**: fair-run guards (setup abort, launch failure, loader
    bootstrap, OOM), then `client-only-class`, then the excuses. Both inversions are pinned in
    `DefaultBootRulesTest` — an excuse above the evidence silently discards true positives, the evidence
    above the fair-run guards publishes host trouble as a mod's fault.
  - **LANDMINE — the ladder's *order* is still in code, only its *content* moved.** Re-ordering rungs changes
    judgment, and the killed-exit-code check sits *between* rungs, so file order alone cannot express it.
    Do not "finish the job" by turning `classify` into a bare loop over the file without solving that.
  - **LANDMINE — the console decides, the metadata only declares.** A `RuleSource.METADATA` rule sets
    `declares` (`Declaration { CLIENT, SERVER, CONTRADICTORY }`) and **may not set `verdict`**;
    `ConsoleOutranksMetadataTest.noMetadataRuleCarriesAVerdict` fails the build if one does, because that
    regression is silent — the file would simply start publishing mods that were never booted. A declaration
    never stands in for a boot and never overturns one: **a mod claiming server whose console reaches a
    client-only class is CONFIRMED client**, and that contradiction is the target. An honestly-declared
    client mod is already excludable from its metadata and costs nothing to find; the container is paid for
    the *dishonest* one, which is why the console rules are the ones worth crafting delicately.
  - **Metadata facts render as ONE canonical line** (`MetadataFacts.line`), not a stream per source, because
    a regex matches a line at a time and the platform-vs-jar contradiction is a *conjunction*. With every
    fact on one line a pattern naming two fields is an AND. The field names (`platform_server=`,
    `platform_client=`, `manifest=`) are an interface operators write patterns against and are pinned; a
    rename would present as "nothing is clientside any more" rather than as a break.
  - `VerdictPolicy.decide` takes `declared` and **never consults it** — deliberate, so the signature is
    honest about what it was given rather than about what it used.
  - **`BootOutcome.stagingPrevented` is what `ERROR` derives from, and EVERY path on which no container
    ran must set it.** There are four: the staging refusal, the other-version re-stage refusal, a thrown
    `packPostProcessor`, and `RunResult.NotStarted`. The last two were missed when the verdict was
    introduced and shipped as INCONCLUSIVE (audit iteration 34) — the post-processor one being the worst,
    since in the grinder that hook *is* `overlayLoaderInstall`, so it fails when the loader cache is
    broken and was publishing a broken host as a verdict about every mod that wanted the tuple.
    **If you add a path that returns a `BootOutcome` without a container having run, it belongs in that
    set** — `PreventedGrindTest` is where to pin it, and `aRealBootThatFailedIsNotMarkedPrevented` is the
    counterweight that stops the flag swallowing real crashes.
  - `verdictOf` also produces the report **note**, carrying the two things the verdict alone cannot say: a
    contradicted server claim, and a distribution-locked file that was never readable at all.
  - `Confidence` and `aggregateFor` are **gone**. `BootResult` stays: it is the classifier's per-boot
    reading, not a published verdict, and `VerdictPolicy` consumes it directly.

- **LANDMINE — a platform ref is an identifier, not a name; a refusal must say the slug (2026-09-04).**
  `ModFile.requiredDependencies` holds Modrinth's opaque base62 `project_id` (`MBAkmtvl`) or CurseForge's
  bare numeric id, and `unsatisfied` recorded the ref verbatim — so refusals read as gibberish.
  `architectury-api`, `enchantment-descriptions` and `waystones` were reported that way:
  `enchantment-descriptions` needs `uy4Cnpcm`/`aaRl8GiW` (**bookshelf-lib**, **prickle**), `waystones` needs
  `bi4iCmsw`/`MBAkmtvl` (**shogi**, **balm**).
  **`waystones` shows why this is a defect and not a cosmetic gripe:** its own `neoforge.mods.toml` declares
  `balm` and `shogi` in words, so the *manifest* half of staging already reported them readably while the
  *platform* half reported the same two mods as ids. `BootVerifier.unsatisfiedLabel` resolves a ref to the
  resolved project's slug, and keeps the ref *plus the platform name* only when nothing resolved.
  **The dedupe matters more than the wording:** `unsatisfied` is a `Set<String>`, so a mod missing by both
  routes used to be two entries and is now one. Do not "simplify" this back to adding the raw ref.
  - **There are THREE branches, and the first fix caught two.** `downloadWithDependencies` records an
    unmet dependency when the ref does not resolve, when it resolves but publishes no usable file, and
    when it resolves, a file is picked, and the *download* then fails. The third kept adding the bare ref
    and produced the follow-up report `... Quilt / Minecraft 1.20.4: 306612` — CurseForge's id for Fabric
    API. If you add a fourth, label it there too.
  - **A distribution-locked dependency is not a failed download.** CurseForge publishes no `downloadUrl`
    for an author who opted out, so `JarDownloader` returns `null` and the dependency read as "could not
    be downloaded" — the sentence a 404, a flaky link and a deliberate opt-out all produce. The label now
    says `distribution-locked`, which is the same distinction `downloadFailureDetail` draws for the
    candidate; retrying an opt-out never succeeds.

- **A dependency is resolved by NAME and AT THE VERSION BEING BOOTED — two separate defects, one report
  (2026-09-04).** Both were live on `architectury-api` (Quilt / MC 1.20.4) and `waystones` (Forge /
  MC 1.21.11), each published `ERROR` reading *"Required dependency unavailable … 306612"* / *"… 531761"*.
  - **The name.** `unsatisfiedLabel` names a resolved project by `ProjectFiles.slug` and always did — but
    **both** platforms' `resolveDependency` passed `nativeRef` into that parameter *positionally*, so the
    label resolved the project and read back the ref it started from. The earlier labelling fix only helped
    the branches that *append* something (`(unresolved X project)`, `(distribution-locked on X)`); the plain
    resolved case printed the id. CurseForge's slug was already in the `/mods/{id}` response it fetches for
    `websiteUrl`; Modrinth costs one extra GET, which falls back to the ref rather than losing the project.
  - **LANDMINE — the window. `resolveDependency` reads ONE page of 50 files, and must narrow by
    `gameVersion` or that page is useless for anything but current Minecraft.** CurseForge answers
    newest-first across every loader *and* every Minecraft version, so a library publishing as often as
    Fabric API (1000+ files) has nothing older than current Minecraft in its newest 50. Single-page is still
    correct — a dependency needs *a* usable file, not a history — but only once the query is narrowed.
    Un-narrowed it refuses boots for files that have existed for years, and a staging refusal publishes
    ERROR over whatever the store held.
  - **LANDMINE — `modLoaderType` is supported by the API and must NOT be sent.** Asking CurseForge for
    Quilt returns nothing for Fabric API and re-creates the same refusal one layer down:
    `LoaderCompatibility.alsoRuns` has to *see* the Fabric builds in order to fall back to them,
    and Fabric API is its canonical case. Version narrows the set; loader choice stays in the selector,
    with the obtainability preference. Parameters verified against https://docs.curseforge.com/rest-api/
    (`gameVersion`, `modLoaderType`, `gameVersionTypeId`, `index`, `pageSize`).
  - Modrinth accepts `minecraftVersion` and ignores it: its version endpoint returns a project's whole
    version list in one response, so there is no newest-N window to fall outside of.
  - **The test-boundary lesson, which is the reusable part.** `DependencyLabelTest` proved the labeller
    correct by handing it a `ProjectFiles` the test built with the slug already right — production never
    builds one of those. A unit test that *constructs* the value under test cannot see a producer
    constructing it wrongly. `DependencySlugTest` drives the real `resolveDependency` with canned JSON and
    asserts the composition; it is the only arrangement in which a positional slip in either platform fails.

- **A dependency the candidate *ships* is never fetched and never missing** (`BundledJars`, 2026-09-04).
  Fabric and Quilt load jar-in-jar libraries, so a `depends` naming one is satisfied before staging looks.
  `xaeros-world-map` was refused for `xaerolib` while carrying
  `META-INF/jars/xaerolib-fabric-26.2-1.7.1.jar`.
  **The near-miss is the mechanism, and it is worth understanding before touching the refusal split:** a
  Modrinth project `xaerolib` exists, so the manifest id *mapped* — but it publishes nothing tagged Quilt or
  26.2, so nothing could be staged, and a mapped-then-unstageable id lands in `unsatisfied` (refuses) where an
  unmappable one lands in `unmapped` (does not). Being *almost* resolvable was worse than being unknown.
  Not a quirk: `sodium` declares **nine** nested jars, and they are exactly the Fabric API modules this file
  documents as the most-commonly-missing dependency class.
  - **LANDMINE — only jars the descriptor *declares* count.** Fabric loads the list in `jars` (Quilt:
    `quilt_loader.jars`); a stray file under `META-INF/jars/` is not on the classpath. Treating one as
    satisfied would skip staging something genuinely needed and produce a failure to blame on the mod — the
    one direction where being generous here is dangerous. Unreadable input yields no ids for the same reason.
  - **LANDMINE — read the nested jar as a `ZipInputStream`, never spool it to a temp file.** The first cut
    used `createTempFile(...).apply { deleteOnExit() }`; `deleteOnExit` registers the path in a static set
    that never shrinks, and this runs per staged jar, per boot attempt, for every candidate of a sweep.
  - Ids come from each nested descriptor's own `id` and `provides`, never from its file name — a name like
    `xaerolib-fabric-26.2-1.7.1.jar` carries a version and a loader the id does not.

- **Client-only proof is about the mod, so it crosses builds and loaders** (`BootDecision.provesClientOnly`,
  2026-09-04). `sodium` — a client renderer Modrinth marks `server_side: unsupported` — published
  INCONCLUSIVE: its NeoForge boot crashed reaching LWJGL, and the other-version re-check then sampled a
  Fabric build that booted cleanly, which `reconcileOtherVersionRecheck` treats as replacing the crash.
  For the three rungs that are client-only evidence **by construction** — `CLIENT_ONLY_CLASS`,
  `LWJGL_ON_A_DEDICATED_SERVER`, `FML_INVALID_DIST` — the re-check is not run, a survivor cannot clear it,
  another loader cannot supersede it, and **every loader inherits CONFIRMED**. Features do not change with
  the loader; only the implementation does. It matters concretely because the stems differ
  (`sodium-neoforge-` vs `sodium-fabric-`), so excluding only the proving loader leaves half the project
  shipping into every server pack.
  - **LANDMINE — an *unexplained* crash is still disprovable**, and that guard is why `iron-chests` stopped
    publishing off one bad build. Do not widen `provesClientOnly` to cover `OPERATOR_RULE`: a rule reaching
    CRASHED states that *this console* is a crash, not that the mod is client-only.
  - A superseded `ERROR` keeps its reason in the note. Publishing the entry is right — it comes from platform
    metadata, not from the boot — but the grind still failed and an operator has to see that.

- **LWJGL and FML's invalid-dist are shipped defaults, not examples** (2026-09-04). `iris` scored
  INCONCLUSIVE on `NoClassDefFoundError: org/lwjgl/Version` because both signatures lived only in
  `boot-rules.example.json`, a template the daemon never loads. **Ordering was not the problem** — nothing
  matched the line at all. `fml-invalid-dist` also stops a zero exit hiding a crash, since NeoForge's
  ServerStarterJar prints the refusal in full and exits 0.

- **Two patterns, and only one of them is publishable** (`LoaderVerdict.filenamePattern`, 2026-09-04).
  `suggestedEntry` is the longest common prefix over a project's *whole* history and must stay that way —
  it is what the fallback list matches with `startsWith`, so it has to cover every build ever released.
  The cost is that any project which renamed its files loses whatever the rename dropped:
  `iris` published `iris-` for Fabric and Quilt against `iris-neoforge-` for NeoForge, the difference being
  that its oldest Fabric jars are `iris-mc1.16.5-1.0.0.jar`, from before the loader went into the name,
  while all 42 NeoForge files carry it.
  `filenamePattern` runs the same `FilenameStemDeriver.deriveStem` over the **sampled file alone**, so it
  keeps the token history erodes, and the grinder shows the two side by side.
  - **LANDMINE — never publish the narrow one.** Serving `filenamePattern` from `/as-properties` would stop
    excluding every build the narrow form misses, which for `iris` is its entire pre-2022 history. The two
    are separate fields for that reason and `theFilenamePatternIsNotWhatGetsPublished` fails the build on a
    swap.
  - A **Quilt** row reads `iris-fabric-`, which is correct and not a leak: Quilt boots Fabric builds, and
    this describes the artifact, not the row's label. That is the whole point — it is what a maintainer
    checks the finding against on the platform page.
  - `ClientsideReportRenderer` (the CLI's Markdown report) still shows only `Suggested entry`. Same gap,
    deliberately left: the ask was the grinder's catalog table, where a reader has no other context.

- **`ClientsideListEditor`** (pure, unit-tested) inserts accepted entries into both files that ship the
  fallback-list: the `fallbackMods` `listOf(...)` block in `GenerationConfig.kt` (sorted, aligned
  `//link` comment, Kotlin trailing-comma is fine) and the backslash-continued `fallbackmodslist` in
  `serverpackcreator.properties`. **Landmine — properties continuation:** the *last* entry must NOT end
  in `,\`, or the continuation bleeds into the next property and corrupts it; the editor strips the
  delimiter off the final line and adds one to the previous-last when appending.

## Boot seam (for the grinder — DONE)

`BootVerifier` is split so a container-backed runner can reuse it: **`prepareBootPack()`** does
host-side staging (pick combo → download mod + deps → generate the self-installing pack) and returns a
`Prepared.Ready`; a **`ServerRunner`** runs the pack and returns *raw* `RunResult` lines + exit status
(classification deliberately left to the caller); **`BootVerifier.outcomeFor()`** is the shared verdict
seam (writes the log, then `BootLogClassifier` + `BootLogExcerpt`). The default
`HostProcessServerRunner` spawns `start.sh`; the grinder will supply a `ContainerServerRunner`
(`--network none`, resource-capped) and `BootLogClassifier` is reused verbatim on its streamed logs.
`verify()`'s signature is unchanged (the `serverRunner` ctor param defaults to the host runner).

## Testing patterns

- 257 tests, all offline. Most build jars in-memory (`java.util.jar`) or feed canned
  JSON to a fake `HttpFetcher`. **Four need a resource** — `MetadataScannerTest`, `LoaderVersionResolverTest`,
  `BootVerifierSelectionTest` and `AttemptStagingIsolationTest` each boot an offline `ApiWrapper` from
  `src/test/resources/serverpackcreator.properties` (whose `ModScanner` relies on the API's cached
  version-manifests, hence `test` `dependsOn :serverpackcreator-api:processTestResources`). The count is
  re-derivable with `grep -rl "ApiWrapper.api(" src/test`; it read "`MetadataScannerTest` is the only one"
  while three already did, which is why it is stated as a command rather than a number to trust.
- `BootCandidateSelector`, `BootLogClassifier`, `FilenameStemDeriver`, `ClientsideListEditor`, plus the
  extracted `BootVerifier.outcomeFor` (`BootVerifierOutcomeTest`) and `HostProcessServerRunner`'s
  no-start-script contract are pure/offline-testable without a running server — keep new logic that way.
  Both crash re-checks follow the same split: `verify` needs an `ApiWrapper` and a running server, so what is
  pinned is *when* a re-check happens and *how the attempts reconcile*, in `BootVerifierCrashRecheckTest`.

## Roadmap — the grinder (`serverpackcreator-grinder`, planned)

A standalone fire-and-forget Docker service depending on this module + `docker-java` boots candidate
mods **in parallel, isolated containers** to build a catalog-wide list of suspected-clientside mods.
The boot seam above is in place; remaining to build: the `ContainerServerRunner`, the worker
pool/queue, and a per-`(loader, loaderVer, mcVer)` pre-bake cache of the installed loader+libraries so
each actual mod-boot runs offline.

## Boot deadlines are suspend-aware (shared with the grinder)

`SuspendAwareDeadline` (this module) is what both boot paths poll: `HostProcessServerRunner` here and the grinder's
`DockerJavaContainerEngine`. A plain wall-clock deadline expires on a boot the host froze — measured, a laptop
idle-sleeping in ~16-minute cycles produced 19 of 153 verdicts reading `timed out`, several `SURVIVED (timed out)`
whose console showed the server reaching ready seconds after launch. Any gap between polls too large to be mere
slowness is added back to the budget, so a timeout means *"the boot had this long and did not make it"*.

It lives **here, not in the grinder**, because grinder depends on clientside and not the reverse — the detection was
originally written in the container engine's companion, where this module could not reach it. The clock is injected
so the threshold is testable at all: both real callers are integration-shaped and cannot be made to sleep.
**Landmine:** any new poll/park loop that bounds a boot must use it rather than `System.currentTimeMillis()`, or that
path silently reacquires the bug.

## Refactor state — condensed summary

> Moved here from the root `CLAUDE.md` on 2026-09-05. It lived in that file's always-loaded
> *Refactor state* table, where it cost every session in every part of the repo ~2.9k tokens for
> detail only relevant while working in this module. Kept **verbatim** rather than diffed against the
> sections above, so nothing could be lost in the move — expect it to restate them in condensed form.
> Read it as an index into the detail above; when the two disagree, the sections above are authoritative.

Extracted from `-app`; `BootVerifier` split + `packPostProcessor` hook; selection (MC-support gate) + setup-abort classification pinned; `MetadataScanner` dispatches through `ModScanner.scannerFor`; the locked-file browser download treats an aborted navigation as the download starting, which is the only way CurseForge's `/download` ever succeeds. A crash now has to survive **three** checks before it counts as clientside: the newest loader build, other versions of the mod (when it contradicts a declared server support), and — after every loader is in — another loader that booted a server with the *same* list-entry. One build's crash and a clientside mod used to be indistinguishable evidence, and the published entry is a loader-agnostic stem, so one loader's crash was stripping another loader's proven-bootable build (2026-08-23). The other-version re-check now spends its budget on a *diverse* sample — a new Minecraft version-line and a new loader per pick — rather than the two versions either side of the crashing one, and a Modrinth version's non-primary files (source jars) are no longer mod files at all; both were needed to stop `creativecore` publishing HIGH (2026-08-23). Per-attempt scratch space is owned by `(platform, slug, loader)` via `AttemptDirectory`, not by `(slug, loader)` — the same slug on two platforms is two candidates the grinder runs in parallel, and the shared directory was being wiped out from under a running container (2026-08-23). A verdict now records **which loader actually booted** (`bootedLoader`), because a cross-loader re-check can decide one loader's verdict from another's clean boot — and only a loader's *own* boot may disprove another's crash (2026-08-23). A **modloader that never bootstrapped** is INCONCLUSIVE, not CRASHED: `Could not find parent layer for module` is the ServerStarterJar and Forge's `SecureModuleClassLoader` disagreeing about module layers, so the server dies before FML exists and no mod is loaded — `ars-nouveau` was headed for a HIGH off it. The JVM's `Error: could not open` for an unreadable `@argfile` joined the launch-failure guard at the same time, since Forge now boots from one (2026-08-23) **A crash is only evidence when the harness gave the mod a fair run** (2026-08-29, from a census of 200 of the 609 crash logs the live grinder publishes; 130 of them carried no client-side evidence at all yet every one scored CRASHED). Two fixes came out of it. `BootCandidateSelector` now fixes the **Minecraft version across both loader attempts** instead of preferring it inside each: a Quilt-tagged Fabric API for the wrong version used to satisfy the first attempt, so the Quilt-to-Fabric fallback holding the right version was never reached, and 20 of the 35 sampled Quilt boots were handed a `+26.3` Fabric API for packs as old as 1.19.2 — Quilt Loader refused the pack and the *candidate* wore the verdict. A dependency matching no file for the pack's Minecraft version is now **not staged at all**, because a near-miss dependency only manufactures a conflict to blame on the mod, whereas a near-miss *candidate* still tests the candidate. `BootLogClassifier` gained `sandboxNetworkMarkers` and widened `dependencyFailureMarkers` (Quilt's `requires version [x, y) of z`, mixin `ClassMetadataNotFoundException`, the legacy `MixinTweaker` CNFE) — both in the band **below** `clientOnlyClassMarker`, which is the whole design: an excuse may never outrank decisive client-only evidence. Boots run `--network none`, so a mod whose loader phones home cannot pass here and fails nowhere else (OneConfig fetches its stage1 from `api.polyfrost.org`, falls back to a Swing dialog — the `Fontconfig error: No writable cache directories` tail — and exits). Re-classifying the real logs: the 21 Griefed sent went 21 CRASHED → 11 CRASHED/10 INCONCLUSIVE, the 200-log sample 200 → 113/87, with both true positives (`arcane-vortex` invalid-dist, `avm-mod` `class_746`) retained. **Known gap, deliberately unfixed:** `clientOnlyClassMarker` misses Fabric intermediary names — `class_NNNN` is intermediary for *every* class, not only client ones, so a pattern would trade these false negatives for false positives; it needs a version-specific ID list or nothing. A staged dependency is counted **by file, not by ref** (2026-08-30): a project is reachable under the platform ref an author linked (`P7dR8mSH`) and the mod id its manifest declares (`fabric` → slug `fabric-api`), which are different strings for one project, so `stageableRequirements`' ref-level dedupe cannot see it — B6's gate caught `amblekit` recording one jar twice. Cosmetic in the report, but it also double-counted toward `MAX_INJECTED_DEPENDENCIES`, refusing packs that were within the cap and scoring them INCONCLUSIVE. **The clientside test tree also did not compile from clean** between `18f59b4bf` and 2026-08-30: an abstract `ModPlatform.name` was added without updating two anonymous implementations in tests, and Gradle's incremental compilation never recompiled them — every green build in between was green without ever compiling those files. `--rerun-tasks` is what exposes that class of thing — a *plain* build of the same commit says `BUILD SUCCESSFUL in 5s` off the build cache (`org.gradle.caching=true`), which is exactly why it survived local full builds. **CI caught it and nobody looked**: `test.yml` runs `./gradlew build` on every push, and runs #301 (`592f1ce21`) and #306 (`388b6e3a2`) both went **failure** on `develop` and stayed unactioned for about a week. The gate existed; the response did not. Check the pipeline after a push rather than assuming green. **A SURVIVED boot is evidence, and `aggregate` was throwing it away** (2026-09-01, from the live store's INCONCLUSIVE rows): the fold consulted `bootResult` only for CRASHED, so a clean boot fell through to the metadata — and with the jar scan errored and the platform declaring nothing, which is *every* CurseForge project, there was no metadata to fall through to and the most expensive signal the engine produces read "we learned nothing". SURVIVED yielded LOW, ranked below `metadataClient` so a clean boot could not overturn a client-only declaration; `better-stats`, `tcdcommons` and `yacl` are the measured rows. **That precedence was deliberately reversed by the 2026-09-04 redesign below — the console now decides and a clean boot is `CLEAR` whatever the mod declared — but the finding that produced it still stands:** a boot that reached its ready-line is the most expensive signal this engine produces and must never read as "we learned nothing", which is why `CLEAR` exists as its own verdict rather than folding into INCONCLUSIVE. In the same pass, a staging refusal stopped saying only `Could not download <file>` — 21 live verdicts read exactly that, all CurseForge, all distribution-locked, which is a sentence a 404 and *a host with no Chromium* produce identically. **The descriptor gate became a filter rather than a veto on 2026-09-03**: selection sees only platform metadata (the jar is not downloaded yet), so a jar whose own declared range excludes the newest *tagged* Minecraft version used to cost the whole candidate. JEI is the measured case, and the descriptor is upstream-wrong rather than misread — `jei-1.21.1-forge-19.52.0.422.jar` is tagged for 1.21 **and** 1.21.1 while declaring `versionRange="[1.21, 1.21.1)"`, and JEI's own `gradle.properties` pairs `minecraftVersion=1.21.1` with `minecraftVersionRange=[1.21, 1.21.1)`, building the range as `[start, thisVersion)` where it should be `[start, nextVersion)`. `ForgeTomlScanner.getVersionRange` is verbatim and `VersionConstraint.mavenRangeHolds` trims its bounds exactly like Maven's own `parseRestriction`, so the parser is not the bug and must not be "fixed"; `reselectOnMinecraftContradiction` re-stages once on the newest version the jar accepts and keeps the original refusal when there is none. It matters beyond one lost boot because a staging refusal publishes INCONCLUSIVE, which overwrites a decisive verdict — the missing-runtime-image harm shape, permanent instead of windowed. **An optional dependency stopped being a requirement on 2026-09-04.** Neither `mandatory` (Forge's `mods.toml`) nor `type` (NeoForge's `neoforge.mods.toml`, default `"required"`, also taking `optional`/`incompatible`/`discouraged`) was read anywhere in `-api`, so `ModDependency` could not carry the distinction and every declared entry was a hard requirement. `advancement-plaques` 1.7.2 was refused with *"Required dependency unavailable … prism"* though its own toml marks `prism` and `toastcontrol` `mandatory=false` and Modrinth lists prism `optional` — an INCONCLUSIVE spent on a mod that never required it. Both **platforms** were already correct (`dependency_type == "required"`, `relationType == 3`); only the manifest half was missing. Optional entries are still *recorded* on `ScannedMod` and merely flagged — stripping them would also drop them from `ModListCompiler`'s dependency rescue and could remove mods from a user's server pack, against this module's keep-the-superfluous-mod rule — so the filter lives in `stageableRequirements`, at the boot-staging consumer. Absent or unreadable means **required**, which is NeoForge's own default and the safe direction: a required dependency read as optional boots a mod without what it needs and can publish a *wrong* verdict, while the reverse only refuses a boot. **The result-system was redesigned on 2026-09-04, and the vocabulary is now four verdicts:** `CONFIRMED` / `CLEAR` / `ERROR` / `INCONCLUSIVE`, replacing `BootResult` × `Confidence`. The pairing conflated *what happened* with *how sure are we* and could not say the thing an operator most needed: **whether the grind ran at all**. `ERROR` is that missing verdict — a grind that could not be performed (no runtime image, a staging refusal, a failed download) — and its absence is what let the missing-runtime-image outage publish a host-wide defect as one INCONCLUSIVE per candidate, overwriting decisive verdicts the 30-day TTL would have left alone. `CLEAR` is the other half: a boot that ran clean and matched nothing is *proven server-safe*, which a single INCONCLUSIVE bucket destroys — "we proved it is fine" and "we learned nothing" are different claims. **Only a rule reaches CONFIRMED**, and only from a rung `BootDecision.decisive` marks, so the bare exit-code rung ("exited non-zero, nothing recognised why" — 27 of 43 published HIGHs rested on it) can no longer publish anything. **Every clientside-determining rule now lives in `boot-rules.default.json`**, bundled in the `-clientside` jar and editable: the eleven hardcoded marker groups moved there verbatim, and `BootLogClassifier` compiles its patterns back out of it by rule id, so an operator's edit reaches the engine and the two copies cannot drift. **File order is the ladder** — fair-run guards, then the decisive client-only evidence, then the excuses — and both inversions are pinned (`DefaultBootRulesTest`). **The console decides and the metadata only declares** (`ConsoleOutranksMetadataTest`): a `RuleSource.METADATA` rule sets `declares`, may not set `verdict`, and a mod claiming **server** whose console reaches a client-only class is CONFIRMED **client** — that contradiction is the target, since an honestly-declared client mod is already excludable from its metadata and costs nothing to find. Metadata facts render as **one canonical line** so a pattern naming two fields is an AND, which is the only way the platform-vs-jar contradiction stays expressible.
