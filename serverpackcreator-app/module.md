# Module serverpackcreator-app

**Four applications in one module**, all built on top of `serverpackcreator-api`. Nothing here
contains core server-pack logic — these are the *user-interfaces* (and one piece of maintainer
tooling) wrapped around the API.

## ELI5: what's in this module?

The API is a library; on its own it has no front-end. This module is every way a human (or a CI
job) actually *uses* ServerPackCreator:

- **`cli`** — the text/command-line interface for people who live in a terminal or script SPC.
- **`gui`** — the desktop app (Java Swing): windows, buttons, the config editor, log viewers.
- **`web`** — the Spring Boot backend that powers the web UI (the Vue frontend talks to this).
- **`updater`** — checks GitHub/GitLab for newer releases and runs version-to-version migrations.
- **`clientside`** — maintainer tooling that auto-triages "please mark this mod client-only"
  requests (not a user-facing app; see its package doc below).

[ServerPackCreator][de.griefed.serverpackcreator.app.ServerPackCreator] is the entry point;
[Mode][de.griefed.serverpackcreator.app.Mode] decides which of the four runs, and
[CommandlineParser][de.griefed.serverpackcreator.app.CommandlineParser] reads the arguments that
make that decision.

# Package de.griefed.serverpackcreator.app

**The launcher.** This top package decides *which* application to start and gets it going.

- `ServerPackCreator` — the **boot sequence**: creates the API instance and the pieces each mode
  needs, then hands off to the chosen mode. The thing `main` ultimately drives.
- `Mode` — the list of run-modes and the CLI arguments that select them (GUI, CLI, web, the
  clientside verbs, etc.). The map from "what the user typed" to "what to run".
- `CommandlineParser` — reads the command-line arguments and resolves them to a `Mode`.
  **Order-sensitive:** some modes must be matched before others because of substring overlaps (see
  the module `CLAUDE.md`).

# Package de.griefed.serverpackcreator.app.cli

**The interactive command-line interface.** `InteractiveCommandLine` is the text menu a user drives
when SPC runs in CLI mode — it prompts for choices and calls the API to configure and generate a
server pack without any GUI.

# Package de.griefed.serverpackcreator.app.cli.commands

**The individual CLI verbs** (picocli `Command`s). Each class is one sub-command; `Command` is the
shared base. They split into the everyday commands and the maintainer "clientside" tooling.

Everyday commands:
- `RunHeadlessCommand` — generate a server pack non-interactively from an existing config.
- `ConfigGenCommand` — create/generate a configuration. `SetupCommand` — first-run setup.
- `HomeDirCommand` / `LanguageCommand` — set the home directory / language.
- `UpdateCommand` — run the update check. `HelpCommand` — print help.

Clientside-tooling commands (thin CLI wrappers over the `clientside` package — see its doc):
- `ScanCommand` (`-scan`) — print the declared sideness of local jars as JSON.
- `ClientsideReportCommand` (`-clientsidereport`) — metadata-only verification report.
- `VerifyClientsideCommand` (`-verifyclientside`) — report **plus** the server-boot test.
- `ClientsideApplyCommand` (`-clientsideapply`) — insert accepted entries into the fallback lists.

# Package de.griefed.serverpackcreator.app.clientside

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
([Confidence][de.griefed.serverpackcreator.app.clientside.Confidence]):

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
- [ClientsideModels][de.griefed.serverpackcreator.app.clientside.ModFile] —
  `Sideness` (REQUIRED/OPTIONAL/UNSUPPORTED/UNKNOWN), `ModFile` (one downloadable file, normalized
  across platforms; `locked` = author forbade direct download), and `ProjectFiles` (a whole resolved
  project: its files + declared sideness). This is the platform-agnostic shape everything else
  speaks.
- [ClientsideReport][de.griefed.serverpackcreator.app.clientside.ClientsideReport] —
  `Confidence`, `JarScan`, `LoaderVerdict` (the verdict for one loader) and `ClientsideReport` (the
  whole machine-readable answer). The output of the investigation.

**Talking to the hosting platforms (turn a URL into `ProjectFiles`):**
- [ModPlatform][de.griefed.serverpackcreator.app.clientside.ModPlatform] — the interface
  ("can you handle this link? then resolve it"), plus `supportedPlatforms()` (Modrinth always,
  CurseForge only with an API-key) and `HttpFetcher` (a tiny HTTP seam so tests use canned JSON, no
  network).
- [ModrinthPlatform][de.griefed.serverpackcreator.app.clientside.ModrinthPlatform] — resolves
  `modrinth.com` links; the *only* platform that declares sideness; its files are never locked.
- [CurseForgePlatform][de.griefed.serverpackcreator.app.clientside.CurseForgePlatform] — resolves
  `curseforge.com` links via the keyed REST API; no sideness field (always `UNKNOWN`); files can be
  distribution-`locked`.

**Working out the list-entry:**
- [FilenameStemDeriver][de.griefed.serverpackcreator.app.clientside.FilenameStemDeriver] — finds the
  longest leading part of a project's file-names that's stable across versions
  (`jei-1.20.1-15.2.jar` → `jei-`). That stem *is* the suggested list-entry. It's a *suggestion* a
  human confirms — file-naming isn't standardized.

**The metadata signal:**
- [MetadataScanner][de.griefed.serverpackcreator.app.clientside.MetadataScanner] — runs SPC's own
  per-loader scanners over a jar to read the sideness it *declares*. Dispatch mirrors the real
  generation path so the answer matches what a real server pack would do.

**Getting the jar (so it can be scanned / booted):**
- [JarDownloader][de.griefed.serverpackcreator.app.clientside.JarDownloader] — the download
  interface, `selectDownloader()` (route by whether the file is locked), and `HttpJarDownloader`
  (the easy case: download straight from the platform URL).
- [BrowserDownloader][de.griefed.serverpackcreator.app.clientside.BrowserDownloader] — the hard
  case: `locked` CurseForge files have no download-URL, so it drives the project's website
  download-flow with a headless browser (Playwright), launched *lazily* only when a locked file
  actually shows up.

**The boot signal (only the maintainer-triggered Phase 2):**
- [BootVerifier][de.griefed.serverpackcreator.app.clientside.BootVerifier] — the orchestrator:
  force-include the mod + its required deps, generate a real server pack, boot it via the
  ServerStarterJar, watch for the ready-line vs. a crash.
- [BootCandidateSelector][de.griefed.serverpackcreator.app.clientside.BootCandidateSelector] — pure
  picking logic (which file + Minecraft-version to boot, which dependency-file to pull alongside),
  split out so it's unit-testable without a real server.
- [LoaderVersionResolver][de.griefed.serverpackcreator.app.clientside.LoaderVersionResolver] — picks
  the loader-version to install for the boot, from SPC's cached version metadata.
- [BootLogClassifier][de.griefed.serverpackcreator.app.clientside.BootLogClassifier] — reads a
  finished boot's console output + exit-code into a `BootResult` (SURVIVED / CRASHED / INCONCLUSIVE),
  with no I/O of its own. `BootLogExcerpt` snips the relevant slice of a crash log for the comment.

**Putting it together and reporting:**
- [ClientsideVerifier][de.griefed.serverpackcreator.app.clientside.ClientsideVerifier] — the
  top-level conductor for Phase 1 (and Phase 2 when a boot-verifier is supplied): pick the platform,
  resolve files, derive the entry, scan, optionally boot, and *aggregate* every signal into a
  per-loader `Confidence`.
- [ClientsideReportRenderer][de.griefed.serverpackcreator.app.clientside.ClientsideReportRenderer] —
  turns the report into the Markdown comment posted on the issue. It carries a hidden marker (so the
  comment can be updated in place) and a hidden JSON copy of the data (so the accept-workflow reads
  the suggested entries back instead of re-deriving them).

**Acting on approval:**
- [ClientsideListEditor][de.griefed.serverpackcreator.app.clientside.ClientsideListEditor] — once a
  maintainer labels the issue accepted, inserts the confirmed entries into the **two** files that
  ship the fallback list — the `fallbackMods` block in `GenerationConfig.kt` and the
  `fallbackmodslist` value in `serverpackcreator.properties` — in sorted position, skipping
  duplicates, with a minimal diff. Pure string transforms (no I/O), so it's unit-tested.

## How a maintainer actually invokes this (the four CLI verbs)

The investigation is reachable from the command-line (and the GitHub workflows call these):

- `-scan <dir> --loader <L> --minecraft <V>` — read the declared sideness of local jars to JSON.
- `-clientsidereport <url> [--output <f>]` — the **metadata-only** report (Phase 1).
- `-verifyclientside <url> [--output <f>]` — metadata **and** the server-boot test (Phase 1 + 2).
- `-clientsideapply --report <json>` — insert the accepted entries into the list files.

The matching GitHub workflows (`clientside-verify`, `clientside-boot`, `clientside-accept`) run the
metadata pass when an issue is opened, the boot pass on demand, and — once the `accepted` label is
set — run `-clientsideapply` and open a PR against `develop` automatically.

# Package de.griefed.serverpackcreator.app.updater

**Staying up to date.** Checks for newer ServerPackCreator releases and migrates settings between
versions.

- `UpdateChecker` — points at SPC's GitHub and GitLab repositories and tells you whether a newer
  release exists.
- `MigrationManager` — works out the ordered list of migration-steps between the user's old version
  and the new one, and runs them (so settings/files keep working across upgrades).

# Package de.griefed.serverpackcreator.app.updater.versionchecker

**The reusable update-checking engine** behind `UpdateChecker` (works against any GitHub/GitLab
repo). `VersionChecker` is the base class with the semantic-version comparison logic; `GitHubChecker`
and `GitLabChecker` fetch releases from each host. `Update` describes an available newer release and
gives access to its `ReleaseAsset`s, `Source` archives and `ArchiveType`s.

# Package de.griefed.serverpackcreator.app.gui

**The desktop application (Java Swing).** The GUI holds little to no server-pack logic itself — it's
a window onto the API, letting a user configure and generate packs by clicking instead of typing. The
top package is the shell.

- `MainWindow` — assembles and shows the whole desktop application.
- `GuiProps` — shared GUI properties/resources (fonts, colours, icons, sizing) the widgets read from.

# Package de.griefed.serverpackcreator.app.gui.components

**Reusable Swing building-blocks** used all over the GUI — the project's own widget toolkit. Custom
inputs (`ActionCheckBox`, `ActionComboBox`, `ActionSlider`, `ScrollTextField`, `ScrollTextArea`,
`ScrollTextFileField`), file pickers (`BaseFileChooser`, `FileFIeldDropType`), layout/structure
(`CollapsiblePanel`, `TabPanel`, `TabTitle`, `ConvenientJTable`), and visual bits (`StatusIcon`,
`CompoundIcon`, `TextIcon`, `ElementLabel`, `BalloonTipButton`, `ThemedBalloonTip`,
`DocumentChangeListener`).

# Package de.griefed.serverpackcreator.app.gui.splash

**The start-up splash screen** shown while SPC boots. `SplashScreen` is the window; `BackgroundPanel`,
`Progress` and `Version` draw its contents; `Splashes`/`Reticulation`/`By` provide the rotating
splash texts and artwork; `ExitButton` lets you bail early; `SplashProps` holds its settings.

# Package de.griefed.serverpackcreator.app.gui.themes

**Look-and-feel.** `ThemeManager` loads and applies the available UI themes; `ThemeInfo` describes
one theme.

# Package de.griefed.serverpackcreator.app.gui.tips

**"Tip of the day".** `TipOfTheDayManager` shows a helpful tip on start-up; `CustomTip` and
`CustomTipOfTheDayUI` render the dialog.

# Package de.griefed.serverpackcreator.app.gui.utilities

**GUI helper utilities.** `DialogUtilities` (common dialogs/message-boxes), `ImageUtilities` (image
loading/scaling), and `ComponentCoroutineScope` — the **lifecycle-bound coroutine scope** every GUI
background task launches on, so work is cancelled when its component goes away (replaces the old
`GlobalScope.launch` anti-pattern; see the module `CLAUDE.md`).

# Package de.griefed.serverpackcreator.app.gui.window

**The main application frame** and the scaffolding that holds the tabs together. `MainFrame` is the
top-level window; `MainPanel` is its content with the tabbed areas; `KeyComboManager` wires keyboard
shortcuts; `UpdateDialogs` shows the "an update is available" prompts.

# Package de.griefed.serverpackcreator.app.gui.window.configs

**The config-editor tab — the heart of the GUI**, where a user builds a `PackConfig`.

- `TabbedConfigsTab` — hosts one editor per open configuration (multiple packs at once).
- `ConfigEditor` — one configuration's editor: all the Swing fields, combo-boxes and status icons
  wired to a `PackConfig`. (Mostly legitimate view code; its domain logic lives in the view-model.)
- `ConfigEditorViewModel` — the **testable brain** behind the editor: the dirty-check (has the user
  changed anything?) and the required-Java-version lookup, kept out of the Swing code so they can be
  unit-tested.

# Package de.griefed.serverpackcreator.app.gui.window.configs.components

**The widgets that make up the config editor.** Pickers (`ModpackChooser`, `ServerIconChooser`,
`ServerPropertiesChooser`, `QuickSelect`), the icon preview (`IconPreview`), the plugins panel
(`PluginsSettingsPanel`), auto-complete (`SuggestionProvider`), the title bar (`ConfigEditorTitle`),
resize helpers (`ComponentResizer`, `ResizeIndicatorScrollPane`), and `ConfigCheckTimer` — which
periodically re-runs the API's config-check so the status icons stay live as you type.

# Package de.griefed.serverpackcreator.app.gui.window.configs.components.advanced

**The "advanced settings" section** of the config editor. `AdvancedSettingsPanel` is the container;
`ClientModsChooser` and `WhitelistChooser` edit the client-only/whitelist mod lists;
`AikarsFlagsButton` inserts the recommended JVM flags; `ScriptKVPairs` edits the script-variable
key/value pairs.

# Package de.griefed.serverpackcreator.app.gui.window.configs.components.inclusions

**The include/exclude editor** — the UI for `InclusionSpecification`s. `InclusionsEditor` is the list
editor; `InclusionSourceChooser` picks a source; `SelectedInclusionDetails` edits the selected rule's
destination and filters; `InclusionSpecificationRenderer` draws each rule in the list.

# Package de.griefed.serverpackcreator.app.gui.window.control

**The bottom control bar** — where you actually press "generate". `ControlPanel` holds the generate
button and overall status; `StatusPanel` shows progress/messages during a run.

# Package de.griefed.serverpackcreator.app.gui.window.control.components

**The control-bar widgets.** `GenerationButton` (start a generation), `ServerPacksButton` (open the
output folder), `StatusLabel` (current status text), and `LarsonScanner` — the Knight-Rider-style
animated progress indicator (a large self-contained custom widget).

# Package de.griefed.serverpackcreator.app.gui.window.logs

**The log-viewer tab.** `TabbedLogsTab` hosts the log panes; `ServerPackCreatorLog`, `PluginsLog`
show SPC's and the plugins' logs; `LogTailer` streams a growing log file into the view live.

# Package de.griefed.serverpackcreator.app.gui.window.logs.components

A single helper: `SmartScroller` keeps a log view pinned to the bottom as new lines arrive — but
stops auto-scrolling once the user scrolls up to read.

# Package de.griefed.serverpackcreator.app.gui.window.menu

The application's menu bar. `MainMenuBar` assembles the File/Edit/View/About menus from the
sub-packages below.

# Package de.griefed.serverpackcreator.app.gui.window.menu.file

The **File** menu: new/load/save configurations (`NewConfigItem`, `LoadConfigItem`, `SaveConfigItem`,
`SaveConfigAsItem`, `SaveAllConfigsItem`, `ConfigChooser`), share logs/configs to HasteBin
(`ConfigToHasteBinItem`, `MainLogToHasteBinItem`, `HasteBinMenuItem`), and `ExitItem`.

# Package de.griefed.serverpackcreator.app.gui.window.menu.edit

The **Edit** menu: edit the server-icon (`EditIconItem`) and `serverpackcreator.properties`
(`EditPropertiesItem`), open the modpack (`OpenModpackItem`), and refresh the bundled client-only mod
list (`UpdateDefaultModslistItem`).

# Package de.griefed.serverpackcreator.app.gui.window.menu.view

The **View** menu: quick "open this directory" / "open this log" shortcuts (home, configs, server
files, server packs, icons, properties, themes, plugins dirs, and the various log files).

# Package de.griefed.serverpackcreator.app.gui.window.menu.about

The **About/Help** menu: links out to Discord, GitHub (page/issues/releases), donations, the wiki,
the step-by-step guide, third-party notices, migration info, "tip of the day" and the update check.

# Package de.griefed.serverpackcreator.app.gui.window.settings

**The settings tab** (the GUI counterpart to `ApiProperties`). `SettingsEditorsTab` hosts the
panels; `GlobalSettings`, `GuiSettings`, `WebserviceSettings` are the panels themselves;
`SettingsHandling` loads/saves them (and **must reload after saving** so the dirty-check settles —
see the module `CLAUDE.md`).

# Package de.griefed.serverpackcreator.app.gui.window.settings.components

**The settings-panel widgets.** `SettingsEditor`/`Editor`/`SettingsTitle` form the base;
directory/file pickers (`HomeDirChooser`, `ServerPackDirChooser`, `PropertiesChooser`,
`TomcatBaseDirChooser`, `TomcatLogDirChooser`, `WritableDirectoryFilter`); script/Java settings
(`JavaPaths`, `ScriptTemplates`, `JavaScriptTemplates`, `ScriptTemplatesChooser`); and
`SettingsCheckTimer`, which keeps the settings' unsaved-changes indicator live.

# Module serverpackcreator-web

Web-API of ServerPackCreator.

# Package de.griefed.serverpackcreator.app.web

**The Spring Boot backend** that serves the web UI and exposes ServerPackCreator over REST. It's a
standard layered web app: controllers handle HTTP and delegate to services; entities are stored in
**MongoDB** (not JPA). The Vue frontend (a separate module) is the client of these endpoints.

- `WebService` — the Spring Boot application entry point.
- `BeanConfiguration` — wires up beans that can't be auto-configured.
- `SettingsController` — exposes this instance's configuration to the frontend.
- `RouteController` / `ErrorRedirect` — make SPA routing and error pages resolve to the Vue app
  (`/#/...`) instead of 404-ing.

# Package de.griefed.serverpackcreator.app.web.modpack

**Modpack uploads and their lifecycle.** `ModPackController` handles the HTTP endpoints,
`ModPackService` the logic; `ModPack`/`ModPackDownload` are the stored entities (with their
Mongo repositories) and `ModPackStatus` tracks where an upload is in processing. `ZipResponse`
streams a packaged result back.

# Package de.griefed.serverpackcreator.app.web.serverpack

**Generated server packs over the web** (download, list, vote, delete). `ServerPackController` is the
`/api/v2/serverpacks` REST surface; `ServerPackService` the logic; `ServerPack`/`ServerPackDownload`
the entities and repositories.

# Package de.griefed.serverpackcreator.app.web.serverpack.customizing

**The persisted pieces of a web run-configuration:** `RunConfiguration` plus the user's chosen client
mods (`ClientMod`), whitelisted mods (`WhitelistedMod`) and JVM start arguments (`StartArgument`),
each with its Mongo repository.

# Package de.griefed.serverpackcreator.app.web.serverpack.runconfiguration

The run-configuration REST surface: `RunConfigurationController` (endpoints),
`RunConfigurationService` (logic) and `RunConfigurationRepository` (storage).

# Package de.griefed.serverpackcreator.app.web.scheduling

**Background cron jobs** that keep the service tidy: `FileCleanupSchedule` (delete old files),
`DatabaseCleanupSchedule` (prune old records) and `VersionRefreshSchedule` (refresh the version
manifests).

# Package de.griefed.serverpackcreator.app.web.task

**The work queue** (a server pack isn't generated inline; it's queued and processed). `QueueEvent`
and `TaskDetail` describe queued work; `TaskExecutionService`/`TaskExecutionServiceImpl` run it;
`EventController`/`EventService` surface progress events to the frontend; `ErrorEntry` records
failures. Each has its Mongo repository.

# Package de.griefed.serverpackcreator.app.web.storage

**File storage abstraction.** `StorageSystem` is the interface, with a `FileSystemStorageService`
(disk) and `DatabaseStorageService` (Mongo/GridFS) implementation; `SavedFile` describes a stored
file and `StorageException` signals failures.

# Package de.griefed.serverpackcreator.app.web.versions

The versions REST surface: `VersionsController` serves all available Minecraft/Forge/Fabric/loader
versions (read from the API's `VersionMeta`) to the frontend; `VersionMetaResponse` is the response
shape.

# Package de.griefed.serverpackcreator.app.web.stats

**Usage statistics for the dashboard.** `StatsController` is the REST surface; the sub-packages
compute the numbers: `stats.creation` (`CreationStatsService`, `AmountPerDate` — packs created over
time), `stats.downloads` (`DownloadStatsService` — download counts), `stats.packs`
(`AmountStatsService`, `AmountStatsData` — totals) and `stats.disk` (`DiskStatsService`,
`DiskStatsData` — disk usage).
