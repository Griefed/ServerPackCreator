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
        val spawned = try {
            ProcessBuilder(command)
                .directory(workingDirectory)
                .redirectErrorStream(true)
                .start()
        } catch (ex: IOException) {
            // Never thrown at the caller: start() is wired to a button, and the console is where a user
            // looks for why nothing happened. A missing bash or an unexecutable script arrives here.
            emit("Could not launch ${command.joinToString(" ")} in ${workingDirectory.absolutePath}: ${ex.message}")
            transitionTo(SessionState.Exited(null))
            close()
            return
        }

        process = spawned
        standardInput = spawned.outputStream.bufferedWriter()
        transitionTo(SessionState.Starting)

        Thread({ pump(spawned) }, "servertest-console-${workingDirectory.name}")
            .apply { isDaemon = true }
            .start()
    }

    /**
     * Read the merged console until end of stream, then reap the process.
     *
     * The ready-line is only looked for while [SessionState.Starting]: after boot the same text can arrive
     * from chat or from a mod echoing the log, and re-entering Ready from Stopping would tell the user a
     * shutting-down server is available.
     */
    private fun pump(spawned: Process) {
        try {
            spawned.inputStream.bufferedReader().forEachLine { line ->
                emit(line)
                if (state == SessionState.Starting && READY_LINE.containsMatchIn(line)) {
                    transitionTo(SessionState.Ready)
                }
            }
        } catch (ex: IOException) {
            emit("Console stream ended unexpectedly: ${ex.message}")
        }

        val exitCode = runCatching { spawned.waitFor() }.getOrNull()
        runCatching { standardInput?.close() }
        transitionTo(SessionState.Exited(exitCode))
        close()
    }

    /**
     * Write [line] to the script's standard input, terminated, and flush.
     *
     * Returns whether it was delivered: a session whose process has gone cannot take input, and the console
     * pane says so rather than silently swallowing what the user typed.
     */
    fun send(line: String): Boolean {
        val writer = standardInput ?: return false
        if (process?.isAlive != true) {
            return false
        }
        return try {
            synchronized(writer) {
                writer.write(line)
                writer.write("\n")
                writer.flush()
            }
            true
        } catch (ex: IOException) {
            // The process exited between the liveness check and the write. Not exceptional, and not worth a
            // console line the user did not cause -- the refusal is the answer.
            false
        }
    }

    /**
     * Ask the server to shut down the way a console operator would, by typing `stop`.
     *
     * This is the *graceful* path and the reason standard input is open at all — the server saves its world
     * and exits on its own. [kill] is the fallback for a server that will not.
     */
    fun stop(): Boolean {
        if (process?.isAlive != true) {
            return false
        }
        transitionTo(SessionState.Stopping)
        return send(STOP_COMMAND)
    }

    /** Kill the script and everything it started, immediately. For a server that will not stop on its own. */
    fun kill() {
        val spawned = process ?: return
        if (!spawned.isAlive) {
            return
        }
        transitionTo(SessionState.Stopping)

        // Descendants are collected BEFORE anything is killed: once the shell dies its children are
        // reparented and stop being its descendants, so collecting afterwards finds nothing. `start.sh` is a
        // launcher and the server is its child, so killing the shell alone would leave the server holding
        // the world directory, the port and its heap.
        val descendants = spawned.toHandle().descendants().toList()
        descendants.forEach { it.destroy() }
        spawned.destroy()

        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(GRACEFUL_TEARDOWN_SECONDS)
        for (descendant in descendants) {
            val remainingNanos = deadline - System.nanoTime()
            if (remainingNanos <= 0) {
                break
            }
            // A process that has already exited is the expected case, not an error.
            runCatching { descendant.onExit().get(remainingNanos, TimeUnit.NANOSECONDS) }
        }

        if (spawned.isAlive) {
            spawned.destroyForcibly()
        }
        descendants.filter { it.isAlive }.forEach { it.destroyForcibly() }
    }

    /** Record and publish a state change. */
    private fun transitionTo(next: SessionState) {
        state = next
        onState(next)
    }

    /** Hand a console line to the listener without letting a throwing listener kill the reader thread. */
    private fun emit(line: String) {
        runCatching { onLine(line) }
    }

    /** Run [onClosed] the first time only, whatever path the session ended by. */
    private fun close() {
        if (closed.compareAndSet(false, true)) {
            runCatching { onClosed() }
        }
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

        /** What a console operator types to shut a Minecraft server down cleanly, world saved. */
        const val STOP_COMMAND = "stop"
    }
}
