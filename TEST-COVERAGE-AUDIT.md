# Test Coverage Audit — Config Checks & Server Pack Generation

> Branch: `claude-test-coverage-audit`. Read-only analysis; no production code changed.
> Goal: raise coverage as far as practical, with **priority on configuration checking
> (`config` package) and server pack generation (`serverpack` package)**, where the bulk
> of the branching, edge cases and "many ways to configure" complexity lives.

---

## 1. Method & baseline

- Source inventory: enumerated every public method in the `config` and `serverpack` packages.
- Existing-test inventory: enumerated every `@Test`/`@ParameterizedTest` and mapped it to the
  method/branch it exercises.
- Gap = a public method, or a distinct branch/edge case within one, with **no** direct assertion.
- A Kover baseline has **not** been captured yet — see §6. This audit is branch-level by reading,
  which is more precise than line% for "which edge cases are missing".

### Current relevant test footprint

| Test file | Tests | Targets |
|---|---|---|
| `ConfigurationHandlerTest` | 43 | happy-path + some negatives across the handler facade |
| `ConfigurationHandlerCharacterizationTest` | 16 | manifest parsing, zip, suggest, modloader-case (pinned) |
| `ServerPackHandlerTest` | 5 | full e2e generation, one per loader |
| `ServerPackHandlerCharacterizationTest` | 13 | destination, cleanup, explicit/save/regex, placeholders, icon/props |
| `PackConfigTest` | 2 | modloader get/set, script settings |
| `config/ValidatorsTest` | 4 | inclusions + modpack-dir validators (subset) |
| `config/InclusionSpecificationTest` | 4 | equals/hashCode/value-equality |
| `config/ModpackZipInspectorTest` | 2 | base-dir listing, disjoint listings |
| `modscanning/ModScannerTest` | 4 | toml/fabric/quilt/annotation scanners |

**Overall reading:** the *happy paths* and the *refactor characterization pins* are solid. The
gaps are concentrated in (a) **error/negative branches**, (b) **the combinatorial config matrix**
(loader × MC-version × source-type), and (c) **`ModListCompiler` exclusion logic**, which is the
single most branch-dense untested unit in the codebase.

---

## 2. Config-package gaps (priority: HIGH)

### 2.1 `ConfigurationHandler.checkConfiguration` — orchestration branches
The all-in-one check has many independent failure branches; tests mostly assert the all-pass case.
Missing, per branch:

- **Empty client-mods → fallback list applied** (`clientMods.isEmpty()`): assert the fallback from
  `apiProperties.clientSideMods()` is injected into the model. Same for **empty whitelist**.
- **Server-icon path branches** (4 outcomes): nonexistent file → `serverIconErrors`; existing but
  **no read permission** → distinct error; empty path → pass; valid → pass. Only "valid" is hit today.
- **Server-properties path branches**: same four outcomes, none asserted on the negative side.
- **Modpack neither dir nor zip nor `.zip`** → `modpackErrors` (`configuration_log_error_checkmodpackdir`).
- **Modpack is a `.zip` that throws `IOException`** during `isZip` → the catch adds a modpack error.
- **`checkConfiguration(File, …)` parse-failure path**: pass a malformed/empty-value config file →
  the `catch` adds `configuration_log_error_checkconfig_start`. Currently only the success load is tested.
- **`quietCheck=true`** path (calls `printConfigurationModel`) — never exercised with the flag set.
- **Plugin config-check extension contributes an error** → ends up in `pluginsErrors` and flips
  `allChecksPassed`. (Ties into `ApiPlugins`; can use the example-plugin jar already in test-resources.)

### 2.2 `sanitizeLinks` — currently untested
Symlink resolution for modpackDir, serverIconPath, serverPropertiesPath, and inclusion sources
(both the `startsWith(modpackDir)` branch and the modpack-relative branch). Needs symlink fixtures
(JUnit `@TempDir` + `Files.createSymbolicLink`, skip-on-Windows guard). Zero coverage today.

### 2.3 `ModloaderValidator` (covered partially via handler)
- `checkModloader`: tested for Forge/Fabric/Quilt/LegacyFabric/NeoForge validity, but **not** the
  *invalid/unknown* loader → `modloaderErrors` branch, nor case-insensitivity (`"FORGE"`, `"fAbRiC"`).
- `checkModloaderVersion`: the **`else` branch** (modloader string not one of the five exact-cased
  names) is untested. Also the *invalid version for valid loader* negative for each of the 5 loaders
  — only some positives exist. Worth a `@ParameterizedTest` matrix (loader × {valid, invalid} version).

### 2.4 `InclusionsValidator` (`ValidatorsTest` covers 3 of ~7 branches)
Covered: missing source, invalid filter regex, lazy-alone. Missing:
- **Empty inclusions list** → `configuration_log_error_checkcopydirs_empty`.
- **`lazy_mode` + other entries** → warning branch, lazy stripped, others still validated.
- **Invalid destination** (`checkForInvalidPathCharacters` false) → destination nulled + error added.
- **Global filter entry** (`isGlobalFilter()`) → skipped from source-existence check (`continue`).
- **Invalid inclusion-filter vs invalid exclusion-filter** separately (only one is hit now).

### 2.5 `ModpackZipInspector` / `checkZipArchive`
`ModpackZipInspectorTest` covers listings. Missing the **validity** rules in `checkZipArchive`:
- ZIP with only a top-level wrapper directory (the `zipCheck` `^\w+[/\\]$` case).
- ZIP missing `mods`/`config` (the documented minimum) → modpack error.
- Corrupt/non-zip file handed to the inspector.
- `unzipDestination` increment-on-collision is pinned in characterization; keep it.

### 2.6 `ModpackManifestParser` (manifest matrix)
Characterization pins Curse/GD/AT/MMC/instance.cfg/Modrinth happy paths. Missing:
- **Modrinth** `updateConfigModelFromModrinthManifest` negative/partial-manifest (missing fields).
- **`checkManifests` dispatch precedence** when *multiple* manifests coexist in one modpack.
- **Malformed JSON** for each parser → exception handling / no-crash.
- **`getModLoaderCase`** for every alias incl. the default-to-Forge fallback for an unknown string.

### 2.7 `PackConfig` (only 2 tests)
- **`save(destination, apiProperties)`** round-trip: save then reload, assert all fields equal
  (loaders, inclusions, script settings, icon/props flags, suffix). High value, currently untested.
- Constructor-from-`File` parsing of each field type incl. malformed/empty values (the conf files
  under `testresources/spcconfs` are ready fixtures).
- `setInclusions` / `getPluginConfigs` / `setScriptSettings` boundary behavior.
- The **silent default-to-Forge** modloader setter quirk for unknown values (pinned in char test
  for the handler path, but assert it directly on `PackConfig` too).

---

## 3. Server-pack-generation gaps (priority: HIGH)

### 3.1 `ModListCompiler` — the biggest single gap
Branch-dense (auto-scan on/off, 5 loader scan paths, Forge MC `>12` vs `<=12`, NeoForge
`>=1.20.5` vs older, whitelist match by START/END/CONTAIN/REGEX/EITHER, dependency-keep logic,
disabled-mod renaming). Today it is only exercised **indirectly** via full e2e + one
characterization test (`compileModListExcludesClientsideAndKeepsWhitelisted`). Proposed direct tests:

- Auto-exclusion **disabled** vs **enabled** (`apiProperties.isAutoExcludingModsEnabled`).
- One test per **`ExclusionFilter`** mode (START/END/CONTAIN/REGEX/EITHER) for user-specified exclusion.
- **Whitelist overrides exclusion** for each filter mode.
- **Dependency mod is kept** even though it matches a clientside entry (`isDependencyMod` branch).
- **Forge annotation vs toml** selection by Minecraft minor version boundary (`12`/`13`).
- **NeoForge scanner vs Forge scanner** selection at the `1.20.5` boundary.
- **Quilt** = union of fabric + quilt scanner results.
- `.disabled` rename for already-disabled vs enabled mod files (the `second` of the returned pair).

These can use the existing `*_tests/mods` fixtures and a `ModScanner` against real fixture jars.

### 3.2 `ServerPackFileGatherer` — copy/filter matrix
`getServerFiles` is a 9-way `when`. Characterization touches explicit-files and regex-walk; the
rest of the `when` arms are untested:
- **Global filter** inclusion (adds to `exclusions`, copies nothing itself).
- **hasDestination** × {clientDir is dir, clientDir is file, absolute source dir, absolute source
  file, nonexistent} — five sub-branches.
- **`source == "mods"`** delegating to the compiler and renaming disabled mods.
- **directory vs file vs absolute** fallthrough arms.
- `runFilters`: inclusion-only, exclusion-only, both, and **invalid regex** (logged, treated as null).
- `copyFiles` **lazy_mode** whole-pack copy branch (warned, returns early).
- `excludeFileOrDirectory` true/false against a global exclusion regex.

### 3.3 `ServerPackProvisioner`
- **`copyIcon`** three branches partly covered (default/custom/missing) — add the **non-64×64
  scaling** path explicitly (asserts a 64×64 output is written) and the **unreadable image** catch.
- **`replacePlaceholders`**: `SPC_JAVA_SPC` local (escaped path) vs zipped (`"java"`),
  `SPC_RESTART_SPC` zipped→`"true"`, generic key replacement. Characterization touches two; make the
  table exhaustive.
- **`serverDownloadable`**: the `else → false` (unknown loader) is pinned; the per-loader reachable
  branches are network-dependent — assert via a stubbed `WebUtilities`/`VersionMeta` rather than live.
- **`createServerRunFiles`** local vs zipped: assert the `SPC_JAVA_SPC` difference lands in the
  written start scripts; assert `variables.txt`/HOW-TO-RUN content.
- **`getImprovedFabricLauncher`**: present vs absent launcher (writes `SERVER_PACK_INFO.txt` only
  when present).
- `preInstallationCleanup` / `postInstallCleanup` are pinned — keep.

### 3.4 `ServerPackHandler.run` — orchestration branches
e2e tests cover the 5 loaders with overwrite-on. Missing config-toggle permutations:
- **Overwrite disabled** path (`isServerPacksOverwriteEnabled=false`) → no cleanup, zip deleted only.
- **Update-existing-server-packs enabled** with a stale `manifest.json` → old files pruned.
- **`customDestination` present** vs derived destination.
- **`isZipCreationDesired=false`** → no zip, but local scripts still created.
- **`isServerIconInclusionDesired` / `isServerPropertiesInclusionDesired` = false** → not copied.
- Assert the generated **`manifest.json`** content (relative file list, MC/loader versions).
- `getServerPackDestination`: space→underscore and suffix are pinned; add `pathSecureText` stripping
  of illegal chars.

---

## 4. Prioritized plan

| # | Area | Effort | Value | Why |
|---|---|---|---|---|
| P1 | `ModListCompiler` direct tests (filter modes, whitelist, deps, loader/MC boundaries) | M | **High** | densest untested branch logic; core "many ways to configure" |
| P2 | `checkConfiguration` negative branches (icon/props/modpack-type/parse-fail/quiet) | M | **High** | user-facing validation correctness |
| P3 | `ServerPackFileGatherer.getServerFiles` `when`-arm matrix + `runFilters` | M | **High** | determines what actually ends up in the pack |
| P4 | `ModloaderValidator` + `InclusionsValidator` remaining branches (parameterized) | S | High | cheap, closes clear gaps |
| P5 | `ServerPackHandler.run` config-toggle permutations + manifest assertions | M | High | the headline "various ways a pack is generated" |
| P6 | `PackConfig` save/load round-trip + field parsing | S | Med | serialization regressions are silent |
| P7 | `ServerPackProvisioner` (placeholders table, icon scaling, scripts, downloadable-stubbed) | M | Med | isolate from network via stubs |
| P8 | `sanitizeLinks` symlink resolution (temp-dir, skip-on-Windows) | M | Med | wholly untested, but platform-fiddly |
| P9 | `ModpackManifestParser` malformed/precedence cases | S | Med | robustness against real-world manifests |

Recommended sequencing: **P1 → P3 → P2 → P4 → P5**, then the rest. P1/P3 give the largest branch
coverage jump for the generation core; P2/P4 for the config core.

---

## 5. Fixtures & techniques

- Reuse existing `src/test/resources/{forge,fabric,quilt,neoforge,legacyfabric}_tests` modpacks and
  `testresources/{curseforge,modrinth,atlauncher,gdlauncher,multimc}` manifests — most edge cases
  have a fixture already.
- Use JUnit5 `@TempDir` for write-side tests (copy, scripts, cleanup, symlinks) to avoid polluting
  the repo and to keep tests hermetic/parallel-safe.
- **Decouple from the network**: `serverDownloadable` and installer-reachability must be tested with
  a stubbed `WebUtilities`/`VersionMeta`, not live HTTP — consistent with the module rule "no live
  network; manifests are cached".
- Prefer `@ParameterizedTest` for the matrices (filter modes, loader×version, icon dimensions) to
  keep test count meaningful without copy-paste.
- Keep the refactor **characterization tests green** — these are behavior pins, not throwaways.

---

## 6. Establish the numeric baseline (do first)

Capture a Kover baseline so progress is measurable and gaps are confirmed quantitatively:

```
./gradlew :serverpackcreator-api:koverHtmlReport
# open serverpackcreator-api/build/reports/kover/html/index.html
```

Record per-class line/branch% for the `config` and `serverpack` packages, then re-run after each
priority block to track the delta. This audit predicts the lowest starting numbers in
`ModListCompiler`, `ServerPackFileGatherer.getServerFiles`, and the negative branches of
`ConfigurationHandler.checkConfiguration`.

### Baseline captured (2026-06-24, Kover)

| Class | Line | Branch | Notes |
|---|---|---|---|
| **serverpack.ModListCompiler** | **38.8%** | **20.3%** | worst in repo — confirms P1 |
| serverpack.ServerPackFileGatherer | 68.9% | 59.0% | `getServerFiles` `when`-arms — P3 |
| serverpack.ServerPackProvisioner | 78.7% | 67.1% | P7 |
| serverpack.ServerPackFile | 78.3% | n/a | |
| serverpack.ServerPackHandler | 86.6% | 52.9% | branch gap = run() toggles — P5 |
| serverpack.ServerPackManifest | 92.3% | 25.0% | |
| config.ConfigCheck | 73.3% | n/a | |
| config.ModpackZipInspector | 76.1% | 85.7% | 2.5 |
| config.ModpackManifestParser | 80.2% | 64.0% | P9 |
| config.InclusionsValidator | 81.2% | 68.4% | P4 |
| config.ConfigurationHandler | 82.8% | 74.3% | negatives + sanitizeLinks — P2/P8 |
| config.PackConfig | 86.1% | 80.4% | P6 |
| config.ModloaderValidator | 95.5% | 90.3% | P4 (cheap remainder) |
| config.ModpackDirectoryValidator | 100.0% | 75.0% | |
| config.InclusionSpecification | 100.0% | 82.4% | |

The prediction holds: `ModListCompiler` is by far the lowest (line **and** branch), the negative
branches show up as the branch-vs-line gaps in `ServerPackHandler` (86.6% line / 52.9% branch) and
`InclusionsValidator`, and the manifest/validator classes trail on branch%.

---

## 7. Progress log

### P1 — `ModListCompiler` (done, 2026-06-24)

New `serverpack/ModListCompilerTest.kt` (6 tests, all green; full API suite 161 → **170**):

- `noExclusionsKeepsEveryMod` — auto-discovery off, empty clientside list → all mods kept.
- `exclusionFilterModesExcludeMatchedMod` — all five `ExclusionFilter` modes (START/END/CONTAIN/
  REGEX/EITHER), each crafted to exclude exactly the matched mod.
- `whitelistOverridesUserExclusion` — whitelisted mod kept despite matching a clientside entry.
- `excludedModsAreReportedSeparately` — excluded mod reported in the disabled (second) list.
- `autoDiscoveryReachesScannerBranchPerLoader` — every arm of the scanner-selection `when`
  (Forge annotation ≤1.12 vs toml >1.12; NeoForge Forge-scanner <1.20.5 vs dedicated ≥1.20.5;
  Fabric; LegacyFabric; Quilt union), asserting the included+disabled partition invariant.
- `whitelistRescuesAutoDiscoveredClientsideMod` — a scanner-detected clientside mod is rescued by
  the whitelist (detected name discovered dynamically, so it is fixture-name-independent).

**Coverage delta:** `ModListCompiler` line **38.8% → 79.1%**, branch **20.3% → 59.5%**.
Residual branches are the scanner-supplied **dependency-keep** logic (`isDependencyMod`), which
requires fixture jars carrying real mod-dependency metadata — deferred (needs purpose-built
fixtures, beyond P1's scope).

Note: the exclusion toggles (`exclusionFilter`, `isAutoExcludingModsEnabled`) live on the singleton
`ApiWrapper` graph; the test snapshots and restores them per-test to keep the suite isolated.
No `junit-jupiter-params` on the classpath, so cases are looped inside plain `@Test` methods to
match the existing suite idiom rather than add a dependency.

### P3 — `ServerPackFileGatherer.getServerFiles` matrix (done, 2026-06-24)

New `serverpack/ServerPackFileGathererTest.kt` (13 tests, all green; full API suite 170 → **183**):

- Global exclusion-filter registers its regex and gathers nothing; invalid global regex is swallowed.
- Destination sub-branches: file-source → single renamed file; directory-source → files under the
  destination; nonexistent source → best-effort single entry.
- Dedicated `mods` arm: kept mods gathered as-is, clientside-excluded mod renamed `.disabled`.
- No-destination arms: directory-source gathered recursively; file-source → single same-named entry.
- `runFilters`: inclusion-only keeps matches, exclusion-only removes matches, invalid inclusion
  regex falls back to gathering all.
- `excludeFileOrDirectory`: matches/does-not-match against a registered regex.
- `copyFiles` lazy-mode: whole modpack copied, empty per-file accounting returned.

**Coverage delta:** `ServerPackFileGatherer` line **68.9% → 86.2%**, branch **59.0% → 84.6%**.
Residual is mostly the absolute-path fallthrough arms (source given as an absolute path while the
modpack-relative path does not exist) and a couple of IO-error catch blocks — low value, deferred.

### P2 — `ConfigurationHandler.checkConfiguration` negatives (done, 2026-06-24)

New `ConfigurationHandlerNegativeTest.kt` (6 tests, all green; full API suite 183 → **189**):

- Empty clientside list and empty whitelist receive the configured fallback lists.
- Non-existent server-icon path → server-icon check fails.
- Non-existent server.properties path → server-properties check fails.
- Modpack that is neither a directory nor a ZIP → modpack check fails.
- Unparsable config file → caught and reported as a config error (no exception propagates).
- Quiet-check path (`quietCheck = true`) prints the model and a valid config still passes.

**Coverage delta:** `ConfigurationHandler` line **82.8% → 87.4%**, branch **74.3% → 79.7%**.
Residual: the server-icon/properties **read-permission** branches (need `chmod`, not portable in
CI) and the in-`isZip` `IOException` catch — deferred as platform-fragile / low value. `sanitizeLinks`
remains untested and is tracked separately as **P8**.

### P4 — validators (done, 2026-06-24)

Extended `config/ValidatorsTest.kt` (+6 tests, 4 → 10; full API suite 189 → **195**):

- `InclusionsValidator`: lazy-mode stripped when mixed with other entries (rest still validated);
  invalid destination nulled + reported; global-filter entry skipped from the source-existence
  check; invalid **exclusion**-regex rejected (complements the existing inclusion-regex test).
- `ModloaderValidator` (now tested directly): unknown modloader rejected; check is
  case-insensitive (`"FORGE"` passes); unknown loader name hits the version-check else branch.

**Coverage delta:** `InclusionsValidator` line **81.2% → 100%**, branch **68.4% → 81.6%**;
`ModloaderValidator` line **95.5% → 100%** (branch 90.3%, residual = per-loader invalid-version
negatives, low value). 

> **Latent bug discovered — FIXED** (branch `claude-fix-invalid-path-characters`):
> `StringUtilities.checkForInvalidPathCharacters` was an OR-of-negations
> (`!contains("<") || !contains(">") || …`), so it returned `true` (i.e. "valid") unless the
> destination contained *every single* forbidden character at once — meaning virtually all invalid
> destinations passed validation, contradicting the method's own contract ("`true` if none of these
> characters were found"). Fixed by changing `||` to `&&` so the method returns `true` only when not
> a single forbidden character is present (any one forbidden char now invalidates the path). Both
> call-sites (`InclusionsValidator`, GUI `InclusionsEditor`) treat `true` as "valid", so the
> semantics stay consistent. Added a direct regression test in `StringUtilitiesTest` (every forbidden
> char rejected individually) and simplified the P4 destination test to a single-forbidden-char case.

### P5 — `ServerPackHandler.run` toggle permutations (done, 2026-06-24)

New `ServerPackHandlerRunTest.kt` (5 tests, all green; full API suite 195 → **200**):

- Custom destination is honored (server pack generated at exactly that path).
- ZIP-creation disabled → no ZIP reported/written, local start scripts still created.
- Icon- and properties-inclusion disabled → neither file copied into the pack.
- Written `manifest.json` records relative file paths plus the Minecraft/modloader versions.
- Overwrite-disabled + updating-enabled → files listed in a pre-existing manifest but no longer in
  the modpack are pruned on regeneration.

Each run is redirected into a `@TempDir` via `PackConfig.customDestination`, keeping the shared
server-packs directory clean and the tests isolated; the overwrite/update toggles are
snapshotted/restored per-test.

**Coverage delta:** `ServerPackHandler` line **86.6% → 95.1%**, branch **52.9% → 88.2%**;
`ServerPackManifest` line **92.3% → 100%**. Residual `run` branches are the overwrite-enabled
cleanup path and a couple of empty IO-catch blocks, exercised indirectly by the e2e suite.

### P6 — `PackConfig` save/load round-trip (done, 2026-06-24)

New `config/PackConfigRoundTripTest.kt` (3 tests, all green; full API suite 200 → **203**):

- Full round-trip preserves all scalar fields, mod-lists, script-settings and — newly pinned —
  **inclusion-specifications including destination and both filters** (the existing
  `scriptSettingsTest` round-trips everything *except* inclusions).
- Mod-list setters drop blank / whitespace-only entries.
- `save` appends a `.conf` extension when the destination lacks one.

**Coverage delta:** `PackConfig` line **86.1% → 86.5%**, branch **80.4% → 83.9%** (modest line gain —
the save/load lines were already largely covered; the value is pinning the inclusions round-trip and
the setter filtering directly).

### P7 — `ServerPackProvisioner` (done, 2026-06-24)

New `serverpack/ServerPackProvisionerTest.kt` (4 tests, all green; full API suite 203 → **207**):
existing-custom server.properties copy, already-64x64 icon direct-copy (no scaling), Windows-path
escaping + generic-key placeholder replacement, and zipped-pack plain-`java` placeholder.

**Coverage delta:** `ServerPackProvisioner` **unchanged at 78.7% line / 67.1% branch** — and this is
the honest finding: those branches were *already* covered by the comprehensive e2e `forgeTest`
(which uses a custom 64x64 icon, a custom properties file and a Windows Java path). The new tests
still add value as **fast, isolated, single-purpose regression pins** (the e2e is one giant test;
these localize a failure to one behavior), but they do not raise the number.

The genuine provisioner residual is **network-bound and currently untestable offline**:
`serverDownloadable` per-loader installer-reachability and `getImprovedFabricLauncher`'s
launcher-present branch both hit `WebUtilities`/`VersionMeta` over HTTP, and there is **no mocking
library on the test classpath** to stub them. Closing this would require adding a mock dependency
(e.g. MockK) and injecting stubbed collaborators into a hand-built `ServerPackProvisioner` — a
larger, dependency-adding change deferred for Griefed's decision. Remaining non-network gaps are the
`zipBuilder` zip-exclusion filtering and a few `createServerRunFiles` catch blocks.

### P9 — `ModpackManifestParser` (done, 2026-06-24)

New `config/ModpackManifestParserTest.kt` (3 tests, all green; full API suite 207 → **210**):

- All four Modrinth dependency keys (`fabric-loader`/`quilt-loader`/`forge`/`neoforge`) map to the
  correct normalized modloader and version (previously only one loader was exercised).
- `checkManifests` precedence: a CurseForge `minecraftinstance.json` wins over a co-present
  `manifest.json` (it is the only one recording the CurseForge source + project IDs).
- A malformed manifest is caught and reported as a modpack error rather than propagating.

All JSON is crafted minimally so no network icon-download is triggered.

**Coverage delta:** `ModpackManifestParser` line **80.2% → 82.6%**, branch **64.0% → 66.7%**.
Residual is the per-launcher icon-download (`getAndSetIcon`, network) and several launcher-specific
error/catch blocks — same network constraint as P7.

---

## 8. Summary (P1–P9 complete)

Full API suite **161 → 210 tests (+49), 0 failures**. Behavior-preserving throughout (no production
code changed). Headline classes — config-checking and server-pack generation:

| Class | Line | Branch |
|---|---|---|
| `ModListCompiler` | 38.8% → **79.1%** | 20.3% → **59.5%** |
| `ServerPackFileGatherer` | 68.9% → **86.2%** | 59.0% → **84.6%** |
| `ConfigurationHandler` | 82.8% → **87.4%** | 74.3% → **79.7%** |
| `InclusionsValidator` | 81.2% → **100%** | 68.4% → **81.6%** |
| `ModloaderValidator` | 95.5% → **100%** | 90.3% |
| `ServerPackHandler` | 86.6% → **95.1%** | 52.9% → **88.2%** |
| `ServerPackManifest` | 92.3% → **100%** | 25% → 50% |
| `PackConfig` | 86.1% → **86.5%** | 80.4% → **83.9%** |
| `ServerPackProvisioner` | 78.7% | 67.1% (isolation pins added; network-bound residual) |
| `ModpackManifestParser` | 80.2% → **82.6%** | 64.0% → **66.7%** |

Open decisions: (1) fix the `checkForInvalidPathCharacters` `||`→`&&` latent bug; (2) add MockK to
unlock the network-bound provisioner/manifest branches.
