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

The maintainer "mark this mod client-only" tooling no longer lives here: its engine moved to the
standalone **`serverpackcreator-clientside`** module. The CLI verbs that drive it (`-scan`,
`-clientsidereport`, `-verifyclientside`, `-clientsideapply`) remain in `cli.commands` as thin
wrappers — see that module's doc for the investigation itself.

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

Clientside-tooling commands (thin CLI wrappers over the `serverpackcreator-clientside` module — see
that module's doc):
- `ScanCommand` (`-scan`) — print the declared sideness of local jars as JSON.
- `ClientsideReportCommand` (`-clientsidereport`) — metadata-only verification report.
- `VerifyClientsideCommand` (`-verifyclientside`) — report **plus** the server-boot test.
- `ClientsideApplyCommand` (`-clientsideapply`) — insert accepted entries into the fallback lists.

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
