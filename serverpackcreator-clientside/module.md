# Module serverpackcreator-clientside

**The clientside-mod verification engine** — maintainer tooling that auto-triages "this mod is
client-only, please exclude it from server packs" requests. It depends only on
`serverpackcreator-api`; the `serverpackcreator-app` CLI verbs and the standalone grinder service are
both thin callers over it, so the investigation logic lives here once.

# Package de.griefed.serverpackcreator.clientside

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

It never just guesses — it gathers *evidence* and reports how confident it is
([Confidence][de.griefed.serverpackcreator.clientside.Confidence]):

1. **Metadata signal (Phase 1 — cheap, no server boot).** Ask two sources what the mod *claims*:
   - the hosting platform (Modrinth literally declares `client_side` / `server_side`; CurseForge
     has *no such field*, so its answer is always `UNKNOWN`), and
   - the mod's own jar metadata (`fabric.mod.json`, `mods.toml`, …), read with SPC's real scanners.
   This tops out at **MEDIUM** confidence — a claim is not proof.

2. **Boot signal (Phase 2 — expensive, actually runs a server).** Force the mod (plus its required
   dependencies) into a freshly generated server pack and **boot it**. The asymmetry is the whole
   point: *a crash is strong proof it's client-only* (→ **HIGH**, and it even catches a mod that
   *lies* by declaring server-support yet crashing), but *a clean boot proves nothing* — plenty of
   client mods start up quietly.

## ELI5: the pipeline, end to end

```
issue link ──▶ pick platform ──▶ resolve to ProjectFiles ──▶ derive file-name stem (= list entry)
                                          │
                                          ├─▶ download a sample jar ──▶ scan declared sideness
                                          │                              (metadata signal)
                                          └─▶ [Phase 2] build server pack + boot it
                                                                         (boot signal)
                                          ▼
                              aggregate per-loader Confidence ──▶ ClientsideReport
                                          ▼
              render Markdown issue-comment  ·  (if approved) edit list files + open PR
```

## ELI5: what each class does (grouped by job)

**The shared vocabulary (plain data, no logic):**
- [ClientsideModels][de.griefed.serverpackcreator.clientside.ModFile] —
  `Sideness` (REQUIRED/OPTIONAL/UNSUPPORTED/UNKNOWN), `ModFile` (one downloadable file, normalized
  across platforms; `locked` = author forbade direct download), and `ProjectFiles` (a whole resolved
  project: its files + declared sideness). This is the platform-agnostic shape everything else
  speaks.
- [ClientsideReport][de.griefed.serverpackcreator.clientside.ClientsideReport] —
  `Confidence`, `JarScan`, `LoaderVerdict` (the verdict for one loader) and `ClientsideReport` (the
  whole machine-readable answer). The output of the investigation.

**Talking to the hosting platforms (turn a URL into `ProjectFiles`):**
- [ModPlatform][de.griefed.serverpackcreator.clientside.ModPlatform] — the interface
  ("can you handle this link? then resolve it"), plus `supportedPlatforms()` (Modrinth always,
  CurseForge only with an API-key) and `HttpFetcher` (a tiny HTTP seam so tests use canned JSON, no
  network).
- [ModrinthPlatform][de.griefed.serverpackcreator.clientside.ModrinthPlatform] — resolves
  `modrinth.com` links; the *only* platform that declares sideness; its files are never locked.
- [CurseForgePlatform][de.griefed.serverpackcreator.clientside.CurseForgePlatform] — resolves
  `curseforge.com` links via the keyed REST API; no sideness field (always `UNKNOWN`); files can be
  distribution-`locked`.

**Working out the list-entry:**
- [FilenameStemDeriver][de.griefed.serverpackcreator.clientside.FilenameStemDeriver] — finds the
  longest leading part of a project's file-names that's stable across versions
  (`jei-1.20.1-15.2.jar` → `jei-`). That stem *is* the suggested list-entry. It's a *suggestion* a
  human confirms — file-naming isn't standardized.

**The metadata signal:**
- [MetadataScanner][de.griefed.serverpackcreator.clientside.MetadataScanner] — runs SPC's own
  per-loader scanners over a jar to read the sideness it *declares*. Dispatch mirrors the real
  generation path so the answer matches what a real server pack would do.

**Getting the jar (so it can be scanned / booted):**
- [JarDownloader][de.griefed.serverpackcreator.clientside.JarDownloader] — the download
  interface, `selectDownloader()` (route by whether the file is locked), and `HttpJarDownloader`
  (the easy case: download straight from the platform URL).
- [BrowserDownloader][de.griefed.serverpackcreator.clientside.BrowserDownloader] — the hard
  case: `locked` CurseForge files have no download-URL, so it drives the project's website
  download-flow with a headless browser (Playwright), launched *lazily* only when a locked file
  actually shows up.

**The boot signal (only the maintainer-triggered Phase 2):**
- [BootVerifier][de.griefed.serverpackcreator.clientside.BootVerifier] — the orchestrator:
  force-include the mod + its required deps, generate a real server pack, boot it via the
  ServerStarterJar, watch for the ready-line vs. a crash.
- [BootCandidateSelector][de.griefed.serverpackcreator.clientside.BootCandidateSelector] — pure
  picking logic (which file + Minecraft-version to boot, which dependency-file to pull alongside),
  split out so it's unit-testable without a real server.
- [LoaderVersionResolver][de.griefed.serverpackcreator.clientside.LoaderVersionResolver] — picks
  the loader-version to install for the boot, from SPC's cached version metadata.
- [BootLogClassifier][de.griefed.serverpackcreator.clientside.BootLogClassifier] — reads a
  finished boot's console output + exit-code into a `BootResult` (SURVIVED / CRASHED / INCONCLUSIVE),
  with no I/O of its own. `BootLogExcerpt` snips the relevant slice of a crash log for the comment.

**Putting it together and reporting:**
- [ClientsideVerifier][de.griefed.serverpackcreator.clientside.ClientsideVerifier] — the
  top-level conductor for Phase 1 (and Phase 2 when a boot-verifier is supplied): pick the platform,
  resolve files, derive the entry, scan, optionally boot, and *aggregate* every signal into a
  per-loader `Confidence`.
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
