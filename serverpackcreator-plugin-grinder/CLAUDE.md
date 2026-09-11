# serverpackcreator-plugin-grinder — module context

> A pf4j plugin that bridges a ServerPackCreator instance to a **grinder** daemon: browse its verdicts
> in a GUI tab, tick the mods you want excluded, and have those ticks folded into the clientside-mod
> exclusion list of every server pack. Package `de.griefed.serverpackcreator.plugin.grinder`.
> Depends on `serverpackcreator-api` **only**. Not published to Maven.

## Why it exists

The grinder already publishes findings, through `/as-properties` — but all-or-nothing: *every*
`CONFIRMED` verdict, folded into `serverpackcreator.properties`, with no way to disagree with one and
nothing at all about the other three verdict classes. This plugin is the pick-and-choose alternative,
plus a native view of the daemon's own dashboard.

## Layout

| Package | What lives there |
|---|---|
| *(root)* | `GrinderPlugin` (the `ServerPackCreatorPlugin`, stateless), `GrinderTabExtension`, `GrinderPreGenExtension` — the two pf4j extension points |
| `core` | Everything testable without Swing: `GrinderUrl`, `GrinderVerdict`, `GrinderClient` (+ `FetchResult`), `SelectionStore` (+ `SelectionPane`), `ClientsideEntryInjector`, `SelectionAttribution` |
| `gui` | `GrinderTab` (the one tab, holding a nested `JTabbedPane`), `VerdictListPane`, `VerdictTableModel`, `DashboardPane`, `SettingsPane`, plus two pinned non-view units: `PlainTextRendering` and `StatusFormatting` |

## How the pieces connect (read this before changing any of them)

- **`ApiPlugins` hands the *same* `CommentedConfig` instance to the tab and to the pre-generation
  extension** (`getPluginConfig(pluginId)` reads one `HashMap`). That is the entire mechanism: a tick
  made in the GUI reaches a generation started seconds later **without a save and without a restart**,
  and `ExtensionTab.saveConfiguration()` is what carries it across a restart. `SelectionStore` is a
  typed view over that object and holds no state of its own, so constructing one wherever a config is
  in hand is free and always current.
- **`ServerPackHandler.run` calls `runPreGenExtensions(packConfig, …)` immediately before it reads
  `packConfig.clientMods`** to compile the mod list. Appending there is what excludes a mod, and
  because the hook is inside `run` it fires for the GUI, the CLI *and* the web backend — so a headless
  run honours ticks made in the GUI. Verified 2026-09-06 end-to-end: with `creativecore-`, `jei-` and
  `iceberg-` ticked, a CLI generation over a five-jar fixture produced a pack containing only
  `bookshelf` (a `CLEAR` verdict, unticked) and `keepme` (unknown to the grinder); with nothing ticked,
  all five survived and the plugin logged nothing.
- **`config.toml`'s keys are the on-disk format**, and the plugin id `grinder` is the file's name.
  `ApiPlugins.extractPluginConfigs` extracts the shipped `config.toml` only `if (!pluginConfigFile.exists())`,
  so a renamed key or id **orphans every user's saved selection** rather than migrating it.

## Landmines & decisions (do not relearn)

- **`suggestedEntry` is the only field that becomes an exclusion entry.** That is what the daemon's own
  `FallbackPropertiesRenderer` publishes, so the two agree. `fileName` — the sampled artifact's own name,
  called `filenamePattern` until 2026-09-10 — is deliberately **not** a fallback: it names one *file*, and
  SPC's default exclusion filter matches a prefix rather than a whole name, so offering it would exclude
  that one build and nothing else. A row with no `suggestedEntry` is therefore not tickable —
  and `VerdictTableModel` makes its checkbox non-editable rather than rendering an unticked box that
  does nothing when clicked.
- **A blank entry is refused in two places, and both are needed.** An empty string matches *every* mod
  name under SPC's `startsWith`/`contains` filters, so one stored by accident empties a server pack's
  mods directory. `SelectionStore` drops blanks on write; `ClientsideEntryInjector` drops them again,
  because the config file is hand-editable and reaches the injector without passing the store's setter.
- **LANDMINE — every component showing text from outside this plugin must come from `PlainTextRendering`.**
  `JLabel` and `DefaultTableCellRenderer` install an HTML view for any string starting with `<html>`, and
  Swing's HTML subset loads remote images — so a mod name was enough to make a user's window fetch a URL.
  A verdict's `slug` and `detail` are scraped mod metadata and the `/status` strings are the daemon's, and
  that daemon is unauthenticated. Measured headless: `JLabel` parses it, the table's default renderer
  parses it, `putClientProperty("html.disable", true)` stops both. Only labels holding a literal written
  *here* may use `JLabel` directly. The grinder's own web report was hardened against this same input
  class; this surface had reintroduced it in a different renderer.
- **Which pane an entry is filed under is `SelectionAttribution`'s decision, and it is not "whichever pane
  shows it".** Shown wins over stored, so a re-ground verdict moves lists — but an entry **neither** pane
  shows keeps the pane it was *saved* under. That is not an edge case: the never-prune rule below
  guarantees such entries accumulate, and filing them all as CONFIRMED (which the first version did, from
  a bare `partition`) silently reclassifies what the user accepted at their own risk as a proven finding.
  Generation is unaffected either way — it reads the union — which is exactly why it went unnoticed.
- **A wrong-shaped 200 is a failure, not an empty list.** `GrinderClient.readVerdicts` returns `null` for
  a document that is neither an array nor an object carrying a `verdicts` array, and the caller turns that
  into `Failed`. Reported as "no verdicts found" it is indistinguishable from a grinder that has genuinely
  ground nothing. An empty `verdicts` array still reaches the success branch.
- **The dashboard `Timer` is stopped in `removeNotify` and resumed in `addNotify`.** A Swing `Timer` holds
  its listener and fires for the life of the JVM otherwise, so an unattended ServerPackCreator would poll
  its grinder every few seconds forever.
- **`getColumnClass` returns `Boolean::class.javaObjectType`, not `Boolean::class.java`.** The latter is
  the primitive `boolean.class`, which `JTable` has no renderer for — the column would fall back to the
  string renderer and show "true"/"false" instead of a checkbox. The `java.lang.Boolean::class.java`
  spelling picks the right class but raises a compiler warning.
- **The table shows the conclusion and the two readings behind it** (2026-09-08). Columns are
  `[tick] Name · Entry · Verdict · Declared · JAR sideness · Loader · Platform · Scanned · Detail`, with
  the two evidence columns immediately after `Verdict` because that is the order a reader needs them in.
  They are worth the width: on the live feed, **161 of 2057** rows are `CONTRADICTORY` — the platform's
  declaration and the jar's own descriptor disagreeing about the same mod — and that is exactly the row a
  maintainer wants to check by hand rather than trust. `declared` may be **absent** (18 of 2057) and both
  render as an empty cell rather than the word "null", the same conflation `textOrNull` exists for.
  **Consequence worth knowing:** the search box filters with a column-less `RowFilter.regexFilter`, so it
  now matches these values too — typing `CONTRADICTORY` or `SERVER_OR_BOTH` filters the table. That is
  useful and it was not designed, it is inherited from the filter being column-less.
  **Both evidence columns carry a preferred width** (`VerdictTableModel.DECLARED_COLUMN` /
  `JAR_SIDENESS_COLUMN`, sized in `VerdictListPane`, which builds *both* panes). At an equal share of the
  table their widest values clip: `SERVER_OR_BOTH` (1718 of 2057 rows) and `CONTRADICTORY` (161) — the
  second being the exact value these columns were added to surface, so `CONTRADICTO...` defeats the point
  of having them. **Both were found by running the GUI against the live daemon and looking**, one per
  screenshot, and neither is reachable from a unit test: the model reports strings, the width is the
  table's. A *preferred* width only, so the columns still shrink with the window. The constants are
  guarded against drifting from the column list, because the failure mode of a bare index is silently
  sizing a *different* column.
- **A selection is never pruned — only the user unticks.** An entry the grinder has stopped reporting
  (crawl moved on, store reset, daemon down) stays ticked. The alternative is that a mod the user
  deliberately excluded silently reappears in their next server pack, which is the one failure nobody
  notices until the pack is wrong. Same rule in `SelectionStore` and in `VerdictTableModel.setRows`.
- **Deselect-all clears only the rows *that* table shows.** Both panes render one shared selection set,
  so clearing Confirmed must not untick anything in Other Verdicts.
- **Assigning `VerdictTableModel.selection` must not fire `onSelectionChanged`.** That assignment is how
  a saved configuration is *loaded*; treating it as a user edit would have the tab writing its config on
  every refresh. User edits go through `setValueAt` / `selectAll` / `deselectAll`.
- **The client parses responses as a JSON *tree*, never bound to a class.** The daemon is upgraded on
  the operator's schedule and the plugin on the user's, so a field this build has never heard of must be
  ignorable rather than a parse failure the user reads as "the grinder is broken". `textOrNull` also
  exists because Jackson's `asText()` renders a null node as the string `"null"`, which would both put
  the word in a table cell and make an absent entry look tickable.
- **Nothing in `GrinderClient` throws.** It runs on a Swing worker and on the generation path, where an
  escaped exception is a dead tab or an aborted server pack. An unusable address never reaches the
  network, a non-2xx carries its status into the reason (the one fact separating "daemon down" from
  "something in front of it answered"), and `InterruptedException` restores the flag — a `SwingWorker`
  cancels by interrupting.
- **The Dashboard reads `/status`, not `/dashboard`.** That page is an HTML shell whose numbers arrive
  from JavaScript, and Swing's HTML renderer executes none. The fields are the ones the daemon's
  `StatusDashboardRenderer.READ_FIELDS` names; the worker rows follow its `WorkerSnapshot`
  (`worker`, `platform`, `slug`, `projectUrl`, `busySeconds`) — worth reading rather than guessing, as
  the first draft invented `name`/`subject`.
- **Threading is a plain `SwingWorker`, deliberately.** A plugin cannot reach `-app`'s
  lifecycle-cancelled `ComponentCoroutineScope`, and `GlobalScope` is the anti-pattern this project
  spent a sprint removing from its own GUI. The Swing `Timer` driving the Dashboard fires on the EDT and
  only *starts* the worker, so no request runs there.
- **LANDMINE — inside a `JButton.apply { }` the identifier `model` resolves to the button's own
  `ButtonModel`.** It silently shadows a pane's table model. Caught by the compiler here only because
  the field and `ButtonModel` have no members in common; a name they *did* share would have compiled.
  The bulk-select listeners call a named method instead.
- **The grinder's report server has no authentication** (see the root `CLAUDE.md` and the grinder's
  `README.md`). Fetched entries end up in an exclusion list, so a hostile grinder could suppress mods.
  The mitigation is the feature itself — nothing is ticked by default and the table shows every entry
  verbatim — plus a note saying so in the Settings pane. A malformed entry is already inert:
  `ModListCompiler.FilterMatcher` compiles patterns up front and logs-and-skips the bad ones.

## Build wiring

Modelled on `serverpackcreator-plugin-example`: kotlin + dokka conventions, `kapt(libs.pf4j)` for the
extension index, the `pluginArtifact` consumable configuration, `plugin.toml` expansion through
`processResources`, and the `Plugin-*` jar manifest attributes. No i18n4k (the example uses it only for
demo strings). The root build copies the jar into `serverpackcreator-app/tests/plugins` via
`copyPluginsToApp`.

- **LANDMINE — this jar must NOT be copied into `serverpackcreator-api/src/test/resources/testresources/plugins`.**
  `ApiPluginsTest` loops over every plugin jar it finds there and asserts each one provides **all six**
  extension types; this plugin provides two. The example plugin is the one that exercises every
  extension point, which is why it is the only one `copyPluginsApiUnitTests` takes. There is a comment
  saying so at the point in the root build where somebody would add the second one.

## Testing

`./gradlew :serverpackcreator-plugin-grinder:test` — 69 tests. `GrinderClientTest` runs against a real
loopback `HttpServer` rather than a mock, the same idiom the grinder's own `ReportServerTest` uses:
what is under test is behaviour at a socket (refused connection, a 502 from a proxy, a 200 carrying
HTML), and a mock answers none of it honestly. `SelectionStoreTest` parses the `config.toml` this module
actually ships, through SPC's own `TomlParser`, so a key renamed in one place and not the other fails
there rather than at a user's next generation.

The GUI panes' *rendering* is untested by design (this project's standing stance on tables); the table
**model** is not — that is where the checkbox-to-selection logic lives.

## Adjacent fix this module surfaced

Installing a second plugin exposed `ApiPlugins.getAllExtensionsOfPlugin` ignoring its `plugin` argument,
so every tab was added once per *installed plugin* and every generation extension ran that many times
(`Grinder | Tetris | Grinder | Tetris`). Fixed in `-api`, pinned by `ExtensionScopingTest`, recorded in
`claude-docs/API-BEHAVIOUR-CHANGES.md`. It had been invisible because one plugin times one plugin is one.
