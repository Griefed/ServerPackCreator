# Module serverpackcreator-plugin-servertest

**A pf4j plugin that runs a generated server pack from inside ServerPackCreator.** It adds one GUI tab
listing every pack in the server-packs directory, launches the selected one **through the pack's own start
script**, streams that server's console into the window and gives the user a line into its stdin. Depends
on `serverpackcreator-api` **only**, so the jar is never tied to the unpublished modules that churn.

## ELI5: what problem does this solve?

Testing a generated pack meant leaving the application: find the directory, open a terminal,
`bash start.sh`, answer Mojang's EULA, wait for the loader to install, then start a client. Nothing in the
GUI ran a server at all. This plugin makes it one click plus connecting a client.

Launching through the start script rather than a hand-built `java` command is the deliberate part: the
scripts install the modloader, pick a Java version, handle the ServerStarterJar and clean up after a
version change. A test that bypassed them would not be testing the thing that ships.

## ELI5: why the console has an input line

The start scripts are genuinely interactive. `start.sh` blocks on `read` in three places — a
spaces-in-path confirmation, Mojang's EULA, and a "press any key" on exit — and the Minecraft server reads
its own commands from stdin after that. One pipe serves all of it: the user types `I agree` to get past the
EULA, and `stop` later to shut the server down. The plugin deliberately does **not** write `eula.txt`
itself; accepting a licence is the user's to do, and letting the script ask is what exercises that branch.

## ELI5: why each server gets its own port

Several packs can run at once, and every generated pack ships the same `server-port=25565`, so the second
one would fail to bind. There is no way to pass a port through the start scripts — `ADDITIONAL_ARGS` lands
in JVM position, before `-jar`, so Minecraft's `--port` is unreachable — which leaves `server.properties`.
That file is on ServerPackCreator's protected-paths list because it is the user's, so the plugin borrows it
rather than owning it: back it up, rewrite only the port lines, restore it when the server stops.

# Package de.griefed.serverpackcreator.plugin.servertest

The plugin's root: `ServerTestPlugin`, the entry class pf4j instantiates, `ServerTestTabExtension`, the
`TabExtension` SPC discovers and asks for the GUI tab, `ServerTestPostGenExtension`, and
`ServerTestSettings` — the typed view over the configuration SPC owns, which lives here because it is a
model the feature packages read rather than orchestration they must not.

# Package de.griefed.serverpackcreator.plugin.servertest.pack

What there is to launch: finding the packs in the server-packs directory, choosing the start script for
the platform, borrowing and returning `server.properties`, and reading a pack's `variables.txt`. Depends
on nothing else in the plugin.

# Package de.griefed.serverpackcreator.plugin.servertest.session

Running one: allocating a free port, launching through the pack's own start script, and owning the live
processes and their state. Depends on `pack`.

# Package de.griefed.serverpackcreator.plugin.servertest.gui

The Swing surface: the tab, the pack list, and one console pane per running server.
