# serverpackcreator-clientside — module context

> The **clientside-mod verification engine**, extracted out of `serverpackcreator-app` so it can be
> reused by both the app's CLI verbs and the planned standalone Docker "grinder" service. Package
> `de.griefed.serverpackcreator.clientside`. **Depends only on `serverpackcreator-api`** (plus
> Playwright + jackson-module-kotlin) — it must never gain a dependency on `-app`, Spring, or Swing.
> Not published to Maven Central (unlike `-api`), so it can churn freely without compatibility lock-in.

## What it does

Given a Modrinth/CurseForge project-link: pick the platform, resolve the project's files, derive the
clientside-list file-name stem(s), and combine signals into a per-loader `Confidence`. Driven by the
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
  into a `Confidence` **precisely because the platform's claim is unreliable** — which is the whole reason
  the expensive boot-test exists. The domains are bridged deliberately at that one call-site, and it takes
  *two* `DeclaredSupport` values to derive one client/server leaning. Merging the enums would collapse the
  distinction the model is built on, and would push a third party's field vocabulary into `-api`, which is
  published to Maven and is a plugin-compatibility constraint. The rename removed a real hazard, not just
  an aesthetic one: `MetadataScanner` sits in *this* package and imports the API's `Sideness`, which
  silently shadowed the package-local type — so one file's `Sideness` meant the opposite of its neighbours'.
- **Boot signal** (`BootVerifier`): force-includes the mod (auto-exclude off, empty clientside-list) +
  its recursively-resolved required deps, generates a server pack and boots it via the ServerStarterJar.
  `BootLogClassifier` reads the `Done (…)! For help` ready-line vs a non-zero exit (pure, unit-tested).
  **Asymmetry baked into the confidence model:** only a CRASH is decisive (→ HIGH, incl. the
  "declares server/both yet crashes" lie); a clean boot does not prove server-safe. **The declared
  sideness is a self-report and is unreliable** — that asymmetry is *why* the expensive boot exists.
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
  Forge from `@libraries/.../unix_args.txt`. The ladder is **fourteen** rungs, and
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
  exactly `CLIENT_ONLY_CLASS` and `OPERATOR_RULE` (a rule reaching CRASHED stated it deliberately), and the
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
- **Dependencies come from BOTH the platform and the jar manifest, and the two are trusted differently.**
  `downloadWithDependencies` resolves `ModFile.requiredDependencies` as before, then scans each staged jar and
  resolves what its manifest declares and the platform never mentioned — the case Fabric API most often falls
  into. `KnownModIds` bridges the vocabularies (a manifest says `fabric`, Modrinth wants `fabric-api`,
  CurseForge wants `306612`); `VersionConstraint` matches a declared range against `ModFile.version`.
  - **LANDMINE — the refusal split is the whole safety property, and it is structural.** A platform ref is a
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
- **Quilt dependencies fall back to the Fabric build** (`BootCandidateSelector.fallbackLoaders`). Quilt deliberately
  runs Fabric mods, which is why the canonical dependency of a Quilt mod is **Fabric API — a project publishing only
  Fabric-tagged files**. Strict loader matching dropped it silently: measured 2026-07-30, **210** dropped
  dependencies, all but 44 on Quilt, `P7dR8mSH`/`306612` (Fabric API) the most-dropped ref. The map is deliberately
  one-way and minimal — Fabric cannot load Quilt mods, and NeoForge/Forge cross-loading is version-dependent, so
  guessing there would stage a jar the loader cannot use.
- **`allowModDistribution=false`** CurseForge files arrive with `downloadUrl=null` (`ModFile.locked`);
  routed (`selectDownloader`) to the **Playwright** headless-browser `BrowserDownloader` (lazy; only
  launched for locked files), everything else to `HttpJarDownloader`. Playwright is declared in **this**
  module's build (`com.microsoft.playwright:playwright`), exported `api` so `-app` gets it transitively.
  **LANDMINE — the `/download` navigation is *supposed* to fail.** CurseForge answers it with a file transfer,
  and Chromium aborts a navigation that becomes a download, so Playwright throws `net::ERR_ABORTED`. That throw
  used to escape the `waitForDownload` callback and tear the wait down, discarding a download that had already
  started (observed 2026-08-23 on bwncr-neoforge, tombstone-neoforge, Structory). `isDownloadAbort` swallows
  exactly that and nothing else — a timeout or a DNS failure must still fail, or the downloader returns `null`
  forever in silence. Both navigations also wait for `DOMCONTENTLOADED`, never the default `load`: an ad-laden
  project page keeps fetching long after it is usable, and the whole 30s default budget was being spent on it.
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

- 138 tests, all offline. Most build jars in-memory (`java.util.jar`) or feed canned
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
