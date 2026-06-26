# Module serverpackcreator-plugin-example

**A working example plugin that exercises every ServerPackCreator extension point.** It exists as
*documentation-by-example*: if you want to write your own SPC plugin, this is the reference to copy
from, and it is kept in sync with the current plugin API on purpose.

> Note: this module is built as a pf4j plugin jar; it is not part of the Dokka publication, so this
> `module.md` serves as in-repo documentation rather than rendered API docs.

## ELI5: what is a ServerPackCreator plugin?

ServerPackCreator lets third-party code hook into well-defined moments of its work via the
`de.griefed.serverpackcreator.api.plugins` API. A plugin is a jar that SPC discovers at start-up
(through pf4j) and whose extensions SPC then calls at the right time — to add config-checks, run
tasks during generation, or add tabs/panels to the GUI. This example implements **one of each kind**
so you can see exactly how.

## ELI5: the six extension points, by package

**The plugin itself**
- `Example` (root package) — the plugin's entry class. It extends `ServerPackCreatorPlugin`, which
  handles the pf4j boilerplate, so this class stays tiny. Every plugin needs one of these.

**Hooking into the configuration check** (`configcheck`)
- `ConfigurationCheck` — a `ConfigCheckExtension`: runs alongside SPC's own checks when a server-pack
  configuration is validated, letting a plugin add its own rules and error messages.

**Hooking into server-pack generation** (`serverpack`) — three moments in the generation lifecycle:
- `PreGeneration` — a `PreGenExtension`: runs *before* generation starts.
- `PreZipArchive` — a `PreZipExtension`: runs *after* the files are gathered but *before* the ZIP is
  created.
- `PostGeneration` — a `PostGenExtension`: runs *after* the server pack is finished.

**Hooking into the Swing GUI** (`gui`)
- `gui.tab.Tab` — a `TabExtension`: tells SPC to add a whole new tab, returning `TetrisTab`.
- `gui.tab.TetrisTab` — the tab's contents (an `ExtensionTab`). To prove a tab can host anything, it
  embeds a playable **Tetris** game (`gui.tab.Tetris`, a self-contained game engine adapted from Per
  Cederberg's classic) plus some global plugin-configuration controls.
- `gui.panel.Panel` — a `ConfigPanelExtension`: tells SPC to add a panel *into* the config editor,
  returning `ConfigurationPanel`.
- `gui.panel.ConfigurationPanel` — the panel's contents (an `ExtensionConfigPanel`): a place for a
  plugin to expose its own custom configuration values to the user.

# Package de.griefed.example.kotlin

The plugin's root: `Example`, the `ServerPackCreatorPlugin` entry class pf4j instantiates. Keep your
own plugin's entry class as thin as this — the base class does the heavy lifting.

# Package de.griefed.example.kotlin.configcheck

The configuration-check hook. `ConfigurationCheck` (a `ConfigCheckExtension`) adds plugin-specific
validation that runs as part of SPC's normal config check.

# Package de.griefed.example.kotlin.serverpack

The generation-lifecycle hooks: `PreGeneration` (before), `PreZipArchive` (before zipping) and
`PostGeneration` (after) — one class per `PreGen`/`PreZip`/`PostGen` extension point.

# Package de.griefed.example.kotlin.gui

The GUI-extension hooks (Swing). This package and its sub-packages show how a plugin contributes both
a top-level tab and a panel inside the config editor.

# Package de.griefed.example.kotlin.gui.tab

Adds a whole new GUI tab. `Tab` is the `TabExtension` SPC discovers; it returns `TetrisTab`, the
actual tab content (`ExtensionTab`). `Tetris` is the embedded game engine the tab hosts — included to
demonstrate that an extension tab can contain arbitrary, fully-interactive UI.

# Package de.griefed.example.kotlin.gui.panel

Adds a panel inside SPC's config editor. `Panel` is the `ConfigPanelExtension` SPC discovers; it
returns `ConfigurationPanel`, an `ExtensionConfigPanel` where a plugin surfaces its own configuration
values to the user.
