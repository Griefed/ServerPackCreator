# Module serverpackcreator-clientside

**The clientside-mod verification engine** — maintainer tooling that auto-triages "this mod is
client-only, please exclude it from server packs" requests. It depends only on
`serverpackcreator-api`; the `serverpackcreator-app` CLI verbs and the standalone grinder service are
both thin callers over it, so the investigation logic lives here once.

# Package de.griefed.serverpackcreator.clientside

**The engine's entry point and its output surface** — `ClientsideVerifier`, `ClientsideReportRenderer`
and `ClientsideListEditor` — plus, below, the tour of the whole module, which is kept here because
this is the package a reader arrives at.

**Automated triage for "this mod is client-only, please exclude it from server packs" requests.**

## ELI5: what problem does this package solve?

ServerPackCreator builds a *server* pack out of a *modpack*. Some mods only make sense on the
client (minimaps, shaders, fancy menus) and will, at best, do nothing on a server and, at worst,
crash it. So SPC keeps a **fallback list of client-only mods** and leaves them out of every server
pack. A list-entry is just the *front part of a mod's file-name* — SPC excludes a file when
`fileName.startsWith(entry)` (e.g. the entry `jei-` excludes `jei-1.20.1-15.2.jar`).

People open GitHub issues asking "please add mod X to that list". Deciding whether that's actually
correct used to be manual and tedious: find the mod, look at what it claims, maybe download it,
maybe even boot a server with it to see if it explodes. **This package automates that whole
investigation** and turns it into a report a maintainer can approve with one click — after which the
tooling edits the list and opens the pull-request itself.

## ELI5: how does it decide? Two signals, two phases

It never just guesses — it gathers *evidence* and publishes one
[Verdict][de.griefed.serverpackcreator.clientside.verdict.Verdict] per Minecraft version-line:

1. **Metadata signal (Phase 1 — cheap, no server boot).** Ask two sources what the mod *claims*:
   - the hosting platform (Modrinth literally declares `client_side` / `server_side`; CurseForge
     has *no such field*, so its answer is always `UNKNOWN`), and
   - the mod's own jar metadata (`fabric.mod.json`, `mods.toml`, …), read with SPC's real scanners.
   A claim only ever *declares*: a metadata rule sets `declares` and may not carry a verdict, because a
   declaration never stands in for a boot.

2. **Boot signal (Phase 2 — expensive, actually runs a server).** Force the mod (plus its required
   dependencies) into a freshly generated server pack and **boot it**. The console is what decides:
   a rule matching it is the only route to `CONFIRMED` — which is what catches a mod that *lies* by
   declaring server-support yet crashing — and a boot that ran cleanly and matched nothing is the
   opposite proof, `CLEAR`. A crash nothing recognised is neither: the bare exit-code rung means
   "exited non-zero, nothing said why" and stays `INCONCLUSIVE`, raw material for the next rule.

## ELI5: the pipeline, end to end

```
issue link ──▶ pick platform ──▶ resolve to ProjectFiles ──▶ derive file-name stem (= list entry)
                                          │
                                          ├─▶ download a sample jar ──▶ scan declared sideness
                                          │                              (metadata signal)
                                          └─▶ [Phase 2] build server pack + boot it
                                                                         (boot signal)
                                          ▼
                        one Verdict per Minecraft version-line ──▶ ClientsideReport
                                          ▼
              render Markdown issue-comment  ·  (if approved) edit list files + open PR
```

## ELI5: what each class does (grouped by job)

**The shared vocabulary (plain data, no logic):**
- [ClientsideModels][de.griefed.serverpackcreator.clientside.platform.ModFile] —
  `DeclaredSupport` (REQUIRED/OPTIONAL/UNSUPPORTED/UNKNOWN — the platform's self-report about one side,
  deliberately *not* the API's `Sideness` verdict), `ModFile` (one downloadable file, normalized
  across platforms; `locked` = author forbade direct download), and `ProjectFiles` (a whole resolved
  project: its files + declared sideness). This is the platform-agnostic shape everything else
  speaks.
- [ClientsideReport][de.griefed.serverpackcreator.clientside.verdict.ClientsideReport] —
  `JarScan`, `GrindTargetVerdict` (the verdict for one Minecraft version-line) and `ClientsideReport`
  (the whole machine-readable answer). The output of the investigation.
- [Verdict][de.griefed.serverpackcreator.clientside.verdict.Verdict] — the six states the engine publishes:
  `CONFIRMED`, `CLEAR`, `INCONCLUSIVE`, `ERROR`, `LOCKED`, `UNVERIFIABLE`. It replaced a `BootResult` ×
  `Confidence` pairing that conflated *what happened* with *how sure are we* and could not say whether
  the grind ran at all — which `grindRan` now answers directly.

**Talking to the hosting platforms (turn a URL into `ProjectFiles`):**
- [ModPlatform][de.griefed.serverpackcreator.clientside.platform.ModPlatform] — the interface
  ("can you handle this link? then resolve it"), plus `supportedPlatforms()` (Modrinth always,
  CurseForge only with an API-key) and `HttpFetcher` (a tiny HTTP seam so tests use canned JSON, no
  network).
- [ModrinthPlatform][de.griefed.serverpackcreator.clientside.platform.ModrinthPlatform] — resolves
  `modrinth.com` links; the *only* platform that declares sideness; its files are never locked.
- [CurseForgePlatform][de.griefed.serverpackcreator.clientside.platform.CurseForgePlatform] — resolves
  `curseforge.com` links via the keyed REST API; no sideness field (always `UNKNOWN`); files can be
  distribution-`locked`.

**Working out the list-entry:**
- [FilenameStemDeriver][de.griefed.serverpackcreator.clientside.metadata.FilenameStemDeriver] — finds the
  longest leading part of a project's file-names that's stable across versions
  (`jei-1.20.1-15.2.jar` → `jei-`). That stem *is* the suggested list-entry. It's a *suggestion* a
  human confirms — file-naming isn't standardized.

**The metadata signal:**
- [MetadataScanner][de.griefed.serverpackcreator.clientside.metadata.MetadataScanner] — runs SPC's own
  per-loader scanners over a jar to read the sideness it *declares*. Dispatch mirrors the real
  generation path so the answer matches what a real server pack would do.

**Getting the jar (so it can be scanned / booted):**
- [JarDownloader][de.griefed.serverpackcreator.clientside.platform.JarDownloader] — the download
  interface, `selectDownloader()` (route by whether the file is locked), and `HttpJarDownloader`
  (the easy case: download straight from the platform URL).
- Distribution-locked CurseForge files (`allowModDistribution=false`) publish no download URL and
  are reported as unverifiable. The headless-browser downloader that used to fetch them was
  removed in 2026-09; it only circumvented the author's distribution opt-out, and Modrinth
  verification covers those projects instead.
- [BootVerifier][de.griefed.serverpackcreator.clientside.boot.BootVerifier] — the orchestrator:
  force-include the mod + its required deps, generate a real server pack, boot it via the
  ServerStarterJar, watch for the ready-line vs. a crash.
- [BootCandidateSelector][de.griefed.serverpackcreator.clientside.selection.BootCandidateSelector] — pure
  picking logic (which file + Minecraft-version to boot, which dependency-file to pull alongside),
  split out so it's unit-testable without a real server.
- [LoaderVersionResolver][de.griefed.serverpackcreator.clientside.version.LoaderVersionResolver] — picks
  the loader-version to install for the boot, from SPC's cached version metadata.
- [BootLogClassifier][de.griefed.serverpackcreator.clientside.verdict.BootLogClassifier] — reads a
  finished boot's console output + exit-code into a `BootResult` (SURVIVED / CRASHED / INCONCLUSIVE),
  with no I/O of its own. `BootLogExcerpt` snips the relevant slice of a crash log for the comment.

**Putting it together and reporting:**
- [ClientsideVerifier][de.griefed.serverpackcreator.clientside.ClientsideVerifier] — the
  top-level conductor for Phase 1 (and Phase 2 when a boot-verifier is supplied): pick the platform,
  resolve files, derive the entry, scan, optionally boot, and combine every signal into one `Verdict`
  per Minecraft version-line.
- [ClientsideReportRenderer][de.griefed.serverpackcreator.clientside.ClientsideReportRenderer] —
  turns the report into the Markdown comment posted on the issue. It carries a hidden marker (so the
  comment can be updated in place) and a hidden JSON copy of the data (so the accept-workflow reads
  the suggested entries back instead of re-deriving them).

**Acting on approval:**
- [ClientsideListEditor][de.griefed.serverpackcreator.clientside.ClientsideListEditor] — once a
  maintainer labels the issue accepted, inserts the confirmed entries into the **two** files that
  ship the fallback list — the `fallbackMods` block in `GenerationConfig.kt` and the
  `fallbackmodslist` value in `serverpackcreator.properties` — in sorted position, skipping
  duplicates, with a minimal diff. Pure string transforms (no I/O), so it's unit-tested.

## How a maintainer actually invokes this (the four CLI verbs)

The investigation is reachable from the command-line (the `serverpackcreator-app` verbs below wrap
this module, and the GitHub workflows call them):

- `-scan <dir> --loader <L> --minecraft <V>` — read the declared sideness of local jars to JSON.
- `-clientsidereport <url> [--output <f>]` — the **metadata-only** report (Phase 1).
- `-verifyclientside <url> [--output <f>]` — metadata **and** the server-boot test (Phase 1 + 2).
- `-clientsideapply --report <json>` — insert the accepted entries into the list files.

The matching GitHub workflows (`clientside-verify`, `clientside-boot`, `clientside-accept`) run the
metadata pass when an issue is opened, the boot pass on demand, and — once the `accepted` label is
set — run `-clientsideapply` and open a PR against `develop` automatically.

# Package de.griefed.serverpackcreator.clientside.platform

**Where builds come from.** `ModPlatform` and the two implementations behind it, the `HttpFetcher`
seam that lets tests answer with canned JSON, `JarDownloader`, `PlatformDependencyDemand`, and the
platform-agnostic model every other package speaks: `ModFile`, `ProjectFiles`, `DeclaredSupport`,
`ReleaseChannel`.

# Package de.griefed.serverpackcreator.clientside.metadata

**What a jar says about itself.** `MetadataScanner` (SPC's own per-loader scanners), the
Fabric/Quilt descriptor reader `JarSelfDeclaration`, the jar-in-jar reader `BundledJars`, the mod-id
vocabulary `ModIdRegistry` and `LearnedModIds`, and `FilenameStemDeriver`, which works out the
list-entry stem.

# Package de.griefed.serverpackcreator.clientside.version

**Version and loader-compatibility arithmetic**, and the only package here that depends on nothing:
`VersionConstraint`, `LoaderCompatibility`, `LoaderVersionDemand`, `LoaderVersionResolver`.

# Package de.griefed.serverpackcreator.clientside.selection

**Which builds get ground.** `BootCandidateSelector` (and its `GrindTarget`), `MinecraftLinePolicy`
— the Minecraft version-lines a project is ground on — and `VersionOfFile`. Pure picking logic, so
it is unit-testable without a real server.

# Package de.griefed.serverpackcreator.clientside.dependency

**Making a boot's dependencies reachable**, and saying whose fault it was when they are not:
`DependencyAttribution` and `DependencyBacktrack`.

# Package de.griefed.serverpackcreator.clientside.verdict

**Evidence into a conclusion.** `BootLogClassifier` reads a console and an exit code into a
`BootDecision`; `BootRule` and `ConsoleRules` are the rule sets it matches against; `Verdict` and
`VerdictPolicy` are the six states the engine publishes and the gate onto them; `ClientsideReport`
is the machine-readable answer.

# Package de.griefed.serverpackcreator.clientside.boot

**Staging a pack, running a server, keeping the evidence.** `BootVerifier` orchestrates it,
`ServerRunner`/`HostProcessServerRunner` spawns the process, `BootDeadline` and
`SuspendAwareDeadline` bound it, `AttemptDirectory` names its scratch space, and `BootArtifacts` /
`BootArtifactWriter` decide what survives the run.
