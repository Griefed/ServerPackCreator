/* Copyright (C) 2026 Griefed
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 * USA
 *
 * The full license can be found at https:github.com/Griefed/ServerPackCreator/blob/main/LICENSE
 */
package de.griefed.serverpackcreator.plugin.servertest.core

import java.io.File
import java.io.IOException
import java.io.Writer
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * One launched server pack: the process, its console, and the line into its standard input.
 *
 * Launches through the pack's **own start script** rather than a hand-built `java` command. That is the whole
 * point of the feature: the scripts install the modloader, choose a Java version, handle the ServerStarterJar
 * and clean up after a version change, so a test that bypassed them would not be testing what ships.
 *
 * **Standard input stays open**, which is the one thing every other runner in this repository gets wrong for
 * this purpose. `start.sh` blocks on `read` three times — a spaces-in-path confirmation, Mojang's EULA, and a
 * "press any key" on exit — and the Minecraft server reads its own commands from stdin afterwards. One pipe
 * serves all of it: the user types `I agree` to get past the EULA and `stop` to shut the server down. The
 * plugin deliberately writes no `eula.txt` of its own; accepting a licence is the user's to do, and letting
 * the script ask is what exercises that branch.
 *
 * Knows nothing about Swing. Callbacks arrive on the reader thread, so a GUI caller marshals them to the
 * event dispatch thread itself — which is also what lets this be tested headless against a real script.
 *
 * @param workingDirectory The server pack; the script is run with this as its working directory.
 * @param command          The argv to spawn, from [StartScriptSelector].
 * @param onLine           Called for every console line as it arrives, on the reader thread.
 * @param onState          Called on every state transition, on the reader thread.
 * @param onClosed         Called exactly once after the process is gone, for giving back the port and the
 *                         borrowed `server.properties`. Runs however the session ended, including a crash.
 * @author Griefed
 */
class ServerSession(
    private val workingDirectory: File,
    private val command: List<String>,
    private val onLine: (String) -> Unit,
    private val onState: (SessionState) -> Unit,
    private val onClosed: () -> Unit
) {

    /** The spawned script, or `null` before [start]. */
    private var process: Process? = null

    /** The pipe into the script's standard input, held open for the life of the session. */
    private var standardInput: Writer? = null

    /** Guards [onClosed] so a session that is stopped, killed and then exits still gives its port back once. */
    private val closed = AtomicBoolean(false)

    /** The current state, also pushed to [onState] on every change. */
    @Volatile
    var state: SessionState = SessionState.Starting
        private set

    /**
     * Spawn the start script and begin streaming its console.
     *
     * stderr is merged into stdout so the console shows the same interleaving a terminal would.
     */
    fun start() {
    }

    /**
     * Write [line] to the script's standard input, terminated, and flush.
     *
     * Returns whether it was delivered: a session whose process has gone cannot take input, and the console
     * pane says so rather than silently swallowing what the user typed.
     */
    fun send(line: String): Boolean = false

    /**
     * Ask the server to shut down the way a console operator would, by typing `stop`.
     *
     * This is the *graceful* path and the reason standard input is open at all — the server saves its world
     * and exits on its own. [kill] is the fallback for a server that will not.
     */
    fun stop(): Boolean = false

    /** Kill the script and everything it started, immediately. For a server that will not stop on its own. */
    fun kill() {
    }

    companion object {
        /**
         * The vanilla server's ready-line, and the point at which a client can connect.
         *
         * Same spelling as `HostProcessServerRunner` and `ContainerServerRunner` use, because it is the same
         * fact about the same servers; a plugin cannot reach either of those modules to share the constant.
         */
        val READY_LINE = Regex("""Done \([^)]*\)! For help""")

        /** How long the process tree gets to exit on SIGTERM before it is forced. */
        const val GRACEFUL_TEARDOWN_SECONDS = 5L
    }
}
