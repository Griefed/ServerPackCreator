# Module serverpackcreator-plugin-grinder

**A pf4j plugin that bridges a ServerPackCreator instance to a [grinder](../serverpackcreator-grinder)
daemon.** It adds one GUI tab in which the daemon's verdicts can be browsed, ticked and searched, and a
pre-generation extension that folds every ticked entry into the clientside-mod exclusion list of every
server pack — GUI, CLI and web alike. Depends on `serverpackcreator-api` **only**: everything it needs
from the grinder arrives over HTTP as JSON, so the jar is never tied to the unpublished modules that
churn.

## ELI5: what problem does this solve?

The grinder boot-verifies mods at scale and publishes what it found. Folding those findings into
ServerPackCreator was previously all-or-nothing — its `/as-properties` endpoint emits *every*
`CONFIRMED` verdict as a `serverpackcreator.properties` block, with no way to disagree with a single
row and nothing at all about the other verdict classes. This plugin is the pick-and-choose
alternative: a table of everything the daemon knows, a checkbox per row, and the selection applied at
generation time.

## ELI5: how a tick reaches a server pack

`ApiPlugins` hands the *same* `CommentedConfig` instance to the tab and to the pre-generation
extension, so a tick made in the GUI reaches a generation started seconds later without a save and
without a restart; `ExtensionTab.saveConfiguration()` is what carries it across a restart.
`ServerPackHandler.run` calls the pre-generation extensions immediately before it reads
`packConfig.clientMods`, which is why appending there excludes a mod for headless runs too.

```
grinder /verdicts.json ──▶ GrinderClient ──▶ VerdictTableModel ──▶ [tick]
                                                                    │
                                            SelectionStore (config.toml) ◀┘
                                                                    │
              ServerPackHandler.run ──▶ GrinderPreGenExtension ──▶ packConfig.clientMods
```

## ELI5: what each class does (grouped by job)

**The two pf4j extension points** (root package)
- `GrinderPlugin` — the `ServerPackCreatorPlugin` entry class pf4j instantiates. Stateless.
- `GrinderTabExtension` — the `TabExtension` that contributes the tab.
- `GrinderPreGenExtension` — the `PreGenExtension` that appends the ticked entries at generation time.

**Everything testable without Swing** (`core`)
- `GrinderUrl` — the single rule turning an operator-typed address into a requestable base URL, plus
  the `/verdicts.json` and `/status` endpoints derived from it.
- `GrinderVerdict` — one row of the daemon's feed, parsed as a JSON *tree* rather than bound to a
  class, so a field this build has never heard of is ignorable instead of a parse failure.
- `GrinderClient` — the HTTP calls and their `FetchResult`. Nothing here throws: it runs on a Swing
  worker and on the generation path, where an escaped exception is a dead tab or an aborted pack.
- `SelectionStore` — a typed view over the plugin's `CommentedConfig`, holding no state of its own.
- `ClientsideEntryInjector` — the pure transform appending a selection to a mod list.
- `SelectionAttribution` — decides which pane an entry is filed under; shown wins over stored, and an
  entry neither pane shows keeps the pane it was saved under.

**The Swing surface** (`gui`)
- `GrinderTab` — the one tab, holding a nested `JTabbedPane`: Confirmed, Other Verdicts, Dashboard,
  Settings.
- `VerdictListPane` / `VerdictTableModel` — the table and the checkbox-to-selection logic.
- `DashboardPane` — polls the daemon's `/status` on a Swing `Timer`, stopped in `removeNotify`.
- `SettingsPane` — the grinder address and the poll interval.
- `PlainTextRendering` / `StatusFormatting` — the two non-view units pinned by tests. Every component
  showing text from outside this plugin goes through `PlainTextRendering`, because Swing's `JLabel`
  and default table renderer install an HTML view for any string starting with `<html>` and that
  subset loads remote images.

# Package de.griefed.serverpackcreator.plugin.grinder

The plugin's root: `GrinderPlugin`, the entry class pf4j instantiates, and the two extensions SPC
discovers — `GrinderTabExtension` (contributes the GUI tab) and `GrinderPreGenExtension` (appends the
ticked entries to `packConfig.clientMods` immediately before the mod list is compiled).

# Package de.griefed.serverpackcreator.plugin.grinder.core

Everything that works without a display: the address rule (`GrinderUrl`), the verdict row
(`GrinderVerdict`), the HTTP client and its results (`GrinderClient`), the typed view over the plugin
configuration (`SelectionStore`), the mod-list transform (`ClientsideEntryInjector`) and the rule
deciding which pane an entry belongs to (`SelectionAttribution`).

# Package de.griefed.serverpackcreator.plugin.grinder.gui

The Swing surface: `GrinderTab` and its four panes (`VerdictListPane` twice, `DashboardPane`,
`SettingsPane`), the table model carrying the selection (`VerdictTableModel`), and the two units the
panes delegate to rather than formatting inline — `PlainTextRendering` (HTML-proofing untrusted text)
and `StatusFormatting`.
