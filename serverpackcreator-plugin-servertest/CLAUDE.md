# serverpackcreator-plugin-servertest — module context

> A pf4j plugin that **launches a generated server pack from inside ServerPackCreator**, through the
> pack's own start scripts, and hands the user that server's console plus a line into its standard
> input. Package `de.griefed.serverpackcreator.plugin.servertest`.
> Depends on `serverpackcreator-api` **only**. Not published to Maven.

## Why it exists

Testing a generated pack meant leaving the application: find the directory, open a terminal,
`bash start.sh`, answer Mojang's EULA, wait out the modloader install, then start a client. Nothing in
the GUI ran a server at all — a grep for `ProcessBuilder` under `serverpackcreator-app/**/gui/**`
returns nothing. This makes it one click plus connecting a client.

**Launching through the start script is the requirement, not an implementation choice.** The scripts
install the modloader, pick a Java version, handle the ServerStarterJar and clean up after a version
change. A launcher that built its own `java` command would not be testing what ships.

## Layout

| Package | What lives there |
|---|---|
| *(root)* | `ServerTestPlugin` (the `ServerPackCreatorPlugin`, stateless) and the two pf4j extension points: `ServerTestTabExtension` (the GUI tab) and `ServerTestPostGenExtension` (refreshes the list when a pack is generated) |
| `core` | Everything testable without Swing: `Platform`/`StartScriptSelector`/`StartScriptSelection`, `ServerPackCatalog`/`LaunchablePack`, `PortAllocator`, `ServerPropertiesPatch`, `PackVariables`, `ServerSession`/`SessionState`, `SessionRegistry`, `ServerTestSettings`, **`ServerLauncher`/`LaunchOutcome`** — the launch sequence itself — and `GenerationNotifier`/`Subscription` |
| `gui` | `ServerTestTab` (the one tab, holding a nested `JTabbedPane`), `PackListPane`, `PackTableModel`/`PackRow`, `ConsolePane`, plus three pinned non-view units: `ConsoleHints`, `PlainTextRendering` and `Dialogs` |

## Zero API changes, and what that rests on

Nothing in `serverpackcreator-api` was added or altered for this plugin. Everything it needs was
already exported: `TabExtension`/`ExtensionTab`, `ApiProperties.serverPacksDirectory`,
`ServerPackManifest.inside`, and `utilities.jsonUtilities.objectMapper`. That was the constraint
Griefed set — the beta is feature-complete — and it held.

**`-clientside` is the tempting dependency and is deliberately not taken.** Its
`HostProcessServerRunner` already spawns `bash start.sh`, but its contract is the opposite of this
plugin's at all three points that matter: stdin is `/dev/null`, it writes `eula.txt` itself, and it
force-kills the moment the server is ready. This plugin needs an open stdin, the script's own EULA
prompt, and a server that stays up. It is also unpublished and churns freely, which the grinder
plugin's build file already records as a reason.

## Landmines & decisions (do not relearn)

- **The scripts on offer come from `ApiProperties.startScriptTemplates`, not from a list here.** Those
  keys are what `ServerPackProvisioner` generates packs from — one per script type, each written out as
  `start.<key>` — so reading the same setting is the only thing that stops the dropdown offering a script
  no generation produces, or hiding one an operator added. **`StartScripts.forKey` must keep agreeing
  with `ServerPackProvisioner.startScriptName` (`"start.$key"`)**; disagree and every row reports a
  missing script that is sitting right there in the pack.
- **A key this plugin has no interpreter for is still offered, and executed directly** (`./start.zsh`).
  ServerPackCreator marks every generated start script executable, so a custom template carrying a
  shebang runs on its own — and inventing an interpreter for an undocumented key would repeat the
  mistake the old platform fallback made.
- **The order is imposed because `startScriptTemplates` is a `HashMap`.** Known types first in the order
  a user wants them, an operator's additions after, alphabetically. Without it the dropdown reshuffles
  between reads.
- **No templates configured is a real state, not a defensive one** — the setting is user-editable and can
  be emptied, which makes generations produce no start scripts at all. The choice is therefore nullable,
  the dropdown disables itself, and the Status column carries `StartScripts.NO_SCRIPTS_CONFIGURED`.
- **The user picks; the platform only supplies the default** (`bat` on Windows, `sh` elsewhere, falling
  back to whatever *is* configured). The plugin *deciding* is the one failure with no workaround: a guess
  landing on a script the host cannot run leaves somebody unable to start a pack at all. An entry this
  host has no interpreter for is deliberately still selectable — that launch fails on the console with
  the interpreter's own error, which beats a disabled control explaining nothing. **A pack missing the
  chosen script is refused by name, never substituted**; substituting is what the old platform fallback
  did, and it hid that the user asked for `start.bat` and got `start.ps1`.
- **What a pack *carries* is independent of what is *configured*.** The catalog records every `start.*`
  regular file it finds, so a pack generated under a different template set still reports itself
  honestly; the current configuration is applied at the dropdown, not at discovery.
  `StartScriptSelector.selectFor(pack, script)` is pure, so changing the choice re-decides every row
  without going back to disk.
- **The port can only be set through `server.properties`, and that file is the user's.** `start.sh`
  interpolates `ADDITIONAL_ARGS` *before* `-jar`, in JVM-argument position, so Minecraft's `--port`
  never reaches the server; `SERVER_RUN_COMMAND` always ends in `nogui` with no hook for a program
  argument. And `server.properties` is in `GenerationConfig.fallbackUpdateProtectedPaths` precisely
  because SPC must never take it. So `ServerPropertiesPatch` **borrows** it: back it up byte-for-byte,
  rewrite only the port lines, restore on the way out — including restoring to *absent* when the pack
  shipped without one. **The backup's presence is the crash marker**, the same shape as `manifest.json`
  being SPC's "did I produce this?" marker: the next `borrow` restores before it takes, so a killed
  ServerPackCreator cannot leave its borrowed port to be backed up as though it were the user's.
- **A zero-length backup would be ambiguous with a genuinely empty properties file**, and that
  distinction decides whether `restore` writes bytes back or deletes the file — hence `ABSENT_MARKER`.
- **`rcon.port` moves only when `enable-rcon=true`.** It is off in SPC's shipped properties, and when
  on it is a second real listening socket needing its own allocated port.
- **The bind probe sets `reuseAddress = false` on purpose.** With it on, a port in `TIME_WAIT` binds
  successfully, and reporting that as free hands a server a port it may still lose.
- **The configured port range is clamped, never validated.** It comes from a hand-editable
  `config.toml` and the allocator is built while the tab is — see the next entry.
- **`ApiPlugins.addTabExtensionTabs` calls `getTab` OUTSIDE its own try-block.** An exception in
  `ServerTestTab`'s constructor escapes into GUI assembly rather than being logged as a plugin error,
  taking every later plugin's tab with it. Everything reachable from that constructor therefore reports
  rather than throws: an unreadable server-packs directory, a corrupt manifest, a damaged setting.
- **`ServerSession` knows nothing about Swing.** Callbacks arrive on its reader thread and each pane
  marshals with `SwingUtilities.invokeLater` — which is also what lets all of `core` be tested headless
  against real processes. SPC's own `LogTailer` appends from the tailer's thread; that is not a pattern
  to copy.
- **Threading is `SwingWorker` and daemon reader threads, never coroutines.** A plugin cannot reach
  `-app`'s lifecycle-cancelled `ComponentCoroutineScope`, and `GlobalScope` is the anti-pattern this
  project spent a sprint removing from its own GUI.
- **`kill()` collects descendants BEFORE killing anything.** `start.sh` is a launcher and the server is
  its child; once the shell dies its children are reparented and stop being its descendants, so
  collecting afterwards finds nothing. This is the same defect the branch fixed in `-clientside`'s
  `HostProcessServerRunner`, pinned here separately so the plugin cannot reintroduce it.
- **One session per pack directory, keyed on the canonical path.** Two servers over one `world/`
  corrupt it, and two spellings of one directory must not smuggle a second server onto the same world.
- **A JVM shutdown hook is the only join point a plugin has.** `addTabExtensionTabs` offers no close
  callback and `MainPanel.closeAndExit` is not reachable from `-api`. It covers a normal quit and
  `Ctrl+C`; it cannot cover `SIGKILL`, and a server surviving that is reachable — the next launch's
  bind test simply hands out a different port.
- **`PackRow.startable` keys on liveness, not on the last state.** Keying it on `state == null` would
  let each pack be tested exactly once per launch of ServerPackCreator, while the row still shows how
  the last run ended.
- **The launch sequence lives in `core`, not in the tab.** Taking ports, borrowing
  `server.properties`, registering before starting and giving everything back is `ServerLauncher`'s
  job; the tab puts a console on screen and starts the session once there is somewhere for its output
  to go. An audit found the whole sequence uncovered while it sat in a Swing view — it is not
  rendering, and treating it as such is what hid it. **Two idempotence guards in there, not one:**
  `giveBack` covers the resources and is shared with the failure path, where the caller must *not* be
  told a session closed because it never got one; a second guard covers the whole close so the caller
  is told exactly once.
- **Three surfaces show a pack's own directory name, and all three had to be proofed separately.**
  `JLabel`, `DefaultTableCellRenderer`, `JOptionPane`'s string messages and `BasicTabbedPaneUI`'s tab
  titles all install an HTML view for anything starting with `<html>`, and Swing's HTML subset loads
  remote images. A directory name is user-controlled and an imported modpack can influence it.
  **LANDMINE — a registration is not a mitigation until something reaches it.** `PackListPane`
  registered an HTML-disabled renderer under `String::class.java` while `PackTableModel` inherited
  `getColumnClass` = `Object`, so `JTable` never consulted it; measured, the resolved renderer was the
  stock one and the rendered component carried an installed HTML view. `PackTableModel.getColumnClass`
  is therefore load-bearing, every dialog goes through `Dialogs`, and every console tab's title is set
  with `setTabComponentAt`. `HtmlProofingTest` asserts what Swing *resolved*, never that a
  registration was made — and includes a structural guard that no view reaches for `JOptionPane`
  itself, which reads code rather than comments after its first version flagged the sentence
  explaining the rule. `PlainTextRendering` is a deliberate second copy of the grinder plugin's: both
  plugins depend on `-api` alone, and promoting eight lines into the published API would be a
  permanent compatibility obligation bought for very little. If a third plugin needs it, that trade
  changes.
- **`ServerTestTab.consoles` is a `ConcurrentHashMap`, and that is not caution.** It is written on the
  event dispatch thread and read on each session's reader thread, which is where `ServerSession`
  documents its callbacks arrive.
- **Closing a *running* server's console tab is refused.** That tab is the only place the server can be
  stopped from, so removing it would strand the process.
- **The console wraps, unlike SPC's own log panes.** Found by rendering the pane and looking: the notes
  it writes are prose and were being cut off mid-sentence behind a horizontal scrollbar. A crash report
  is what a user comes here to read, and hunting for a scrollbar to finish a stack-trace line is worse
  than a wrapped one.
- **Auto-scroll decides whether it was at the bottom BEFORE appending.** Afterwards the maximum has
  already grown and every position looks scrolled-up, which stops the console following the tail from
  its very first line.

## The server runs in its own JVM, and that is the whole point

**Three OS processes, not one.** ServerPackCreator's JVM spawns `bash start.sh` with `ProcessBuilder`, and
the script spawns `java` itself. Nothing about the server runs inside ServerPackCreator's own JVM, so its
heap is not ServerPackCreator's heap and a modpack asking for 12G cannot exhaust a ServerPackCreator
started with 512M.

Measured rather than asserted (2026-09-25): a launching JVM at `maxHeap=512M`, pid 56250, spawned a shell
at pid 56251 whose child JVM reported `MaxHeapSize = 3221225472` — 3 GiB, from its own `-Xmx3G`, while the
launcher stayed at 512M. Separate pids, independent heaps.

The server's heap comes from **`JAVA_ARGS` in the pack's own `variables.txt`** (which Forge and NeoForge
packs also write into `user_jvm_args.txt`), and `JAVA` there may point at an entirely different Java
installation — a different major version, even. None of that is negotiated with ServerPackCreator.

**What *is* shared is the machine.** The plugin deliberately allows several packs to run at once, so total
RAM is the real constraint: three modpack servers at 8G each will hurt whatever else is running, including
ServerPackCreator. That is a different concern from heap-in-one-JVM, and the honest mitigation is that the
console shows each server's own output and Force stop kills its whole process tree.

**This is also why `kill()` walks `descendants()`.** The server is a *grandchild* — SIGKILL to the shell
alone reparents it to init, still holding the world directory, the port and all of that heap. The same
defect was fixed in `-clientside`'s `HostProcessServerRunner` on this branch.

## Refreshing when a pack is generated

`ServerTestPostGenExtension` hears that a generation finished and publishes to `GenerationNotifier`; the
tab subscribes while it is in the window.

- **A `PostGenExtension`, not an `SPCPostGenListener`**, though both fire on adjacent lines at the end of
  `ServerPackHandler.run`. A listener must be registered by reaching back through `ApiWrapper.api()` from
  plugin `init` — the call the api `CLAUDE.md` records as having caused two unbounded recursions — and
  `ServerPackHandler` has `addEventListener` with **no `removeEventListener`**, so a rebuilt tab would leak
  one permanently. `ApiPlugins.runPostGenExtensions` also already wraps every call, so a throw here cannot
  abort somebody's generation.
- **`GenerationNotifier` exists because the two extensions cannot reach each other.**
  `SingletonExtensionFactory` builds each one separately. It is an `object` but **plugin-scoped, not
  JVM-global** — pf4j gives each plugin its own classloader.
- **Subscribed in `addNotify`, cancelled in `removeNotify`**, the grinder plugin's timer idiom: the
  notifier keeps whatever it is given, so an unowned subscription outlives the tab it served.
- **The generated pack's path is deliberately ignored** and the directory re-read, so the new row's facts
  come from the same `manifest.json` as every other row's rather than by a second route.
- Publishing happens on the generation thread, never the EDT, so the tab marshals for itself.

## The EULA, and why the plugin never writes `eula.txt`

Pure passthrough, by Griefed's decision. The script asks on the console and the user types `I agree`
into the input line. Two reasons: accepting a licence is the user's to do, and letting the script ask is
what exercises that branch of `start.sh` — which is the point of launching through the scripts at all.
Both `HostProcessServerRunner` and the grinder's `ContainerServerRunner` pre-write `eula=true`; they are
unattended verifiers, and this is not.

## `WAIT_FOR_USER_INPUT` — the trap that cost a real boot

`WAIT_FOR_USER_INPUT=true` ends the script on `read -n 1 -s -r -p "Press any key to continue"`, and
**bash writes a `read -p` prompt only when standard input is a terminal.** Over this plugin's pipe there
is no prompt at all, so a finished server is indistinguishable from a hung one.

Observed rather than imagined: on 2026-09-25 a real NeoForge boot reached ready, took `stop`, saved every
dimension, printed `Exiting...` and then went silent for three minutes until the run was abandoned.
`ConsoleHints` now fires a note at that exact line, gated on the pack's own `WAIT_FOR_USER_INPUT` so a
pack that never waits never sees it. The pane also warns up front, but by then it is a modloader install
up the scrollback — which is why the live hint exists as well as the opening note.

`RESTART=true` gets an opening note for the neighbouring reason: the script loops, so `stop` returns the
user to a five-second countdown rather than ending the session, and Force stop is what ends it.

## Testing

`./gradlew :serverpackcreator-plugin-servertest:test` — 106 tests, of which 105 run by default; the
skip is `RealPackBootTest`, which boots a real server and is switched on deliberately (below).

- **`core` is tested against real processes, not mocks.** What is under test is process behaviour — does
  a typed line reach the child's stdin, does output arrive while the process runs, does killing the
  launcher kill what it launched — and a mock agrees with whatever the implementation does. Same
  reasoning as the grinder plugin's `GrinderClientTest` using a real loopback `HttpServer`. The stand-in
  is a shell script with the same *shape* as a start script: prints as it goes, reads from stdin, exits
  with a status of its own choosing.
- **`PortAllocatorTest` keeps one test on the shipped bind probe**, holding a real `ServerSocket`,
  because whether a port is free is a question only the OS answers.
- **`RealPackBootTest` is the end-to-end, and it is OFF by default.** It boots a real pack through its
  real start script:
  ```
  ./gradlew :serverpackcreator-plugin-servertest:test --tests "*RealPackBootTest*" \
      -Dservertest.integration=true -Dservertest.pack=/path/to/a/server-pack
  ```
  It copies the pack first and empties the copy's `mods/`: the plugin's launch path is what is under
  test, and a pack whose mods crash the server proves nothing about it either way.
  **LANDMINE — the test task forwards those two switches explicitly.** A bare `-D` reaches the Gradle
  daemon, not the forked test JVM; without the forwarding the test SKIPs however it is invoked and the
  run reports BUILD SUCCESSFUL having booted nothing.
- **Pane rendering is untested by design** (this project's standing stance on Swing views), but
  `PackTableModel`, `ConsoleHints` and everything in `core` are pinned — that is where the phrases, the
  Start-button logic and the hint timing live.
- **Three units were written before their guards** (`ServerTestSettings`, `PackVariables`,
  `SessionRegistry`) and were verified by mutation instead. That run earned its keep: it found
  `ServerTestSettingsTest` **vacuous**, because each shipped default deliberately equals the code's
  fallback, so reading a renamed key returns the fallback and looks identical to reading the right one.
  It now asserts key **presence**. Ask of any settings guard here whether it could tell a live key from a
  dead one.
- **Two guards are known to be weaker than they read, and say so in their own KDoc.** Do not "fix" them
  by strengthening the wording. `stopAndKillAfterExitLeaveTheStatusIntact` stays green when
  `transitionTo`'s terminal check is removed, because once the process is dead `stop()` and `kill()`
  return before reaching it — the race that check defends against is real and no deterministic test here
  reaches it, so that guard stands on reasoning. `onlyOneConcurrentRegistrationWinsAPack` stays green
  when `SessionRegistry.register` loses its `@Synchronized`, because the window is a few instructions
  wide; `concurrentAllocationsNeverCollide` *does* catch its annotation being removed, so the two are
  not equal evidence.
- **Seam-plus-guard is the sanctioned shape here, not a lapse.** Every `test(servertest): …` commit on
  this branch also lands the production seam with a stubbed body, because for new code there is no
  behaviour to preserve and a guard that cannot compile fails on a missing symbol rather than on a wrong
  answer. Griefed sanctioned it on 2026-09-25 and declined to re-split the branch; the rule and its two
  siblings are in the root `CLAUDE.md`. **The obligation that comes with it:** name, in the commit
  message, which assertions are vacuous against the stub — those are the ones that will otherwise be
  mistaken for pins.
- **`RealPackBootTest` goes through `ServerLauncher`**, not through hand-wired collaborators, so the
  end-to-end exercises the sequence the Start button actually runs rather than a copy that could drift.

## Verified end to end (2026-09-25)

Against `serverpackcreator-app/tests/server-packs/NeoForge-1.21`, through the plugin's own code path:
the pack was discovered, a port allocated and written into a borrowed `server.properties`, the real
`start.sh` downloaded the ServerStarterJar, ran the NeoForge installer, prompted for the EULA, took
`I agree` over stdin and wrote its own `eula.txt`, reached `Done (4.772s)! For help, type "help"`, took
`stop`, saved every dimension, and exited. Afterwards: `server.properties` byte-identical to the
original (md5 unchanged, `diff -rq` silent), no backup left behind, and no server JVM outliving the run.

The GUI was verified by rendering the panes and looking at them — which is how the console's wrapping
defect was found, with every guard green.
