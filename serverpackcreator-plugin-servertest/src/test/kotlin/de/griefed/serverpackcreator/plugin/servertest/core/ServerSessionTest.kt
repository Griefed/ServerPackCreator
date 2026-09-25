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

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.Collections
import java.util.concurrent.TimeUnit

/**
 * Pins the session against a **real** child process, not a mock.
 *
 * What is under test is process behaviour: does a line the user typed actually reach the child's standard
 * input, does output arrive while the process is still running rather than only at its exit, does killing the
 * launcher also kill what the launcher started. A mock answers none of that honestly — it agrees with
 * whatever the implementation happens to do. Same reasoning as the grinder plugin testing its client against
 * a real loopback `HttpServer`.
 *
 * The stand-in is a shell script rather than a Minecraft server for the obvious reason, but it is the same
 * *shape*: it prints as it goes, reads commands from stdin, and exits with a status of its own choosing.
 */
internal class ServerSessionTest {

    /** Sessions started by a test, torn down afterwards so a failing assertion cannot leak a process. */
    private val started = Collections.synchronizedList(mutableListOf<ServerSession>())

    /** A PID the stand-in reported, killed afterwards so a red run does not leak one per re-run. */
    private var spawnedChildPid: Long? = null

    @AfterEach
    fun tearDownEverythingStarted() {
        started.forEach { runCatching { it.kill() } }
        spawnedChildPid?.let { pid -> ProcessHandle.of(pid).ifPresent { it.destroyForcibly() } }
    }

    /** Poll until [condition] holds, so the assertions do not race the reader thread. */
    private fun awaitUntil(what: String, timeoutMillis: Long = 10_000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            if (condition()) {
                return
            }
            Thread.sleep(25)
        }
        Assertions.fail<Unit>("Timed out after ${timeoutMillis}ms waiting for: $what")
    }

    /**
     * A stand-in for a start script: prints, echoes whatever it is told, announces itself ready on request,
     * and exits with a distinctive status on `stop`. The same shape as the real thing, without the download.
     */
    private fun standInScript(packDir: File): File = File(packDir, "start.sh").apply {
        writeText(
            """
            #!/usr/bin/env bash
            echo "booting the stand-in"
            while IFS= read -r line; do
              echo "echoed:${'$'}line"
              if [ "${'$'}line" = "make-ready" ]; then
                echo 'Done (1.234s)! For help, type "help"'
              fi
              if [ "${'$'}line" = "stop" ]; then
                echo "stand-in shutting down"
                exit 7
              fi
            done
            """.trimIndent() + "\n"
        )
    }

    /** Build a session over [packDir]'s stand-in, recording every line and state it reports. */
    private fun sessionOver(
        packDir: File,
        lines: MutableList<String>,
        states: MutableList<SessionState>,
        closedCount: IntArray
    ): ServerSession = ServerSession(
        workingDirectory = packDir,
        command = listOf("bash", "start.sh"),
        onLine = { lines.add(it) },
        onState = { states.add(it) },
        onClosed = { closedCount[0]++ }
    ).also { started.add(it) }

    private fun requireBash() =
        Assumptions.assumeTrue(File("/bin/bash").canExecute(), "Needs bash to run a stand-in start script.")

    /**
     * Output arrives while the process is still running.
     *
     * The distinction matters: a runner that only hands over its console at exit makes a server stuck on the
     * EULA prompt look like a hang with nothing to show, which is the exact failure this feature exists to
     * avoid.
     */
    @Test
    fun streamsConsoleOutputWhileTheProcessIsStillRunning(@TempDir packDir: File) {
        requireBash()
        standInScript(packDir)
        val lines = Collections.synchronizedList(mutableListOf<String>())
        val session = sessionOver(packDir, lines, Collections.synchronizedList(mutableListOf()), IntArray(1))

        session.start()

        awaitUntil("the stand-in's first line") { lines.contains("booting the stand-in") }
        Assertions.assertEquals(
            SessionState.Starting,
            session.state,
            "The process is still running, so it has not exited."
        )
    }

    /**
     * A line the user types reaches the child's standard input.
     *
     * This is the one capability every other runner in this repository lacks — they pin stdin to /dev/null —
     * and without it the EULA prompt cannot be answered and `stop` cannot be sent.
     */
    @Test
    fun deliversATypedLineToTheProcessStandardInput(@TempDir packDir: File) {
        requireBash()
        standInScript(packDir)
        val lines = Collections.synchronizedList(mutableListOf<String>())
        val session = sessionOver(packDir, lines, Collections.synchronizedList(mutableListOf()), IntArray(1))
        session.start()
        awaitUntil("the stand-in to be reading") { lines.isNotEmpty() }

        Assertions.assertTrue(session.send("I agree"), "A running session must accept input.")

        awaitUntil("the stand-in to echo what it was sent") { lines.contains("echoed:I agree") }
    }

    /** The ready-line flips the state, which is what tells the user a client can now connect. */
    @Test
    fun reachesReadyOnTheVanillaReadyLine(@TempDir packDir: File) {
        requireBash()
        standInScript(packDir)
        val states = Collections.synchronizedList(mutableListOf<SessionState>())
        val lines = Collections.synchronizedList(mutableListOf<String>())
        val session = sessionOver(packDir, lines, states, IntArray(1))
        session.start()
        awaitUntil("the stand-in to be reading") { lines.isNotEmpty() }

        session.send("make-ready")

        awaitUntil("the ready state") { session.state == SessionState.Ready }
        Assertions.assertTrue(states.contains(SessionState.Ready), "The transition must be reported, not just held.")
    }

    /**
     * `stop` goes down the same pipe the user types into, and the script's own exit status is reported.
     *
     * The status matters: the start-script template captures the server's exit code deliberately so a crash
     * is distinguishable from a clean shutdown, and throwing that away here would waste it.
     */
    @Test
    fun stopSendsTheConsoleCommandAndReportsTheExitStatus(@TempDir packDir: File) {
        requireBash()
        standInScript(packDir)
        val lines = Collections.synchronizedList(mutableListOf<String>())
        val closedCount = IntArray(1)
        val session = sessionOver(packDir, lines, Collections.synchronizedList(mutableListOf()), closedCount)
        session.start()
        awaitUntil("the stand-in to be reading") { lines.isNotEmpty() }

        Assertions.assertTrue(session.stop(), "A running session must accept a stop.")

        awaitUntil("the stand-in to exit") { session.state is SessionState.Exited }
        Assertions.assertEquals(SessionState.Exited(7), session.state)
        Assertions.assertTrue(lines.contains("stand-in shutting down"), "The shutdown output must reach the console.")
        awaitUntil("the close callback") { closedCount[0] == 1 }
    }

    /**
     * The port and the borrowed `server.properties` are given back however the session ended — including when
     * the process died on its own, which is the case nothing explicitly triggers.
     */
    @Test
    fun runsTheCloseCallbackExactlyOnceWhenTheProcessDiesOnItsOwn(@TempDir packDir: File) {
        requireBash()
        File(packDir, "start.sh").writeText("#!/usr/bin/env bash\necho \"and immediately gone\"\nexit 3\n")
        val closedCount = IntArray(1)
        val session = sessionOver(
            packDir,
            Collections.synchronizedList(mutableListOf()),
            Collections.synchronizedList(mutableListOf()),
            closedCount
        )

        session.start()

        awaitUntil("the exit to be observed") { session.state is SessionState.Exited }
        Assertions.assertEquals(SessionState.Exited(3), session.state)
        Assertions.assertEquals(1, closedCount[0], "The port must be given back exactly once.")

        session.kill()
        session.stop()
        Assertions.assertEquals(1, closedCount[0], "A stop or kill after the fact must not release twice.")
    }

    /** Input to a session that has exited is refused rather than silently swallowed. */
    @Test
    fun refusesInputOnceTheProcessHasGone(@TempDir packDir: File) {
        requireBash()
        File(packDir, "start.sh").writeText("#!/usr/bin/env bash\nexit 0\n")
        val session = sessionOver(
            packDir,
            Collections.synchronizedList(mutableListOf()),
            Collections.synchronizedList(mutableListOf()),
            IntArray(1)
        )
        session.start()
        awaitUntil("the exit to be observed") { session.state is SessionState.Exited }

        Assertions.assertFalse(session.send("too late"), "A dead session cannot take input.")
    }

    /**
     * Killing the session kills the **server**, not only the shell that launched it.
     *
     * The same defect this branch fixed in `-clientside`'s HostProcessServerRunner, pinned here so the plugin
     * cannot reintroduce it: `start.sh` is a launcher, the server is its child, and SIGKILL to the shell
     * alone reparents the server to init with the world directory and the port still held.
     */
    @Test
    fun killEndsTheProcessesTheScriptSpawned(@TempDir packDir: File) {
        requireBash()
        val pidFile = File(packDir, "child.pid")
        File(packDir, "start.sh").writeText(
            """
            #!/usr/bin/env bash
            sleep 300 &
            echo ${'$'}! > "${pidFile.absolutePath}"
            echo "spawned a child"
            wait
            """.trimIndent() + "\n"
        )
        val lines = Collections.synchronizedList(mutableListOf<String>())
        val session = sessionOver(packDir, lines, Collections.synchronizedList(mutableListOf()), IntArray(1))
        session.start()
        awaitUntil("the stand-in to spawn its child") { pidFile.isFile && lines.contains("spawned a child") }
        val childPid = pidFile.readText().trim().toLong().also { spawnedChildPid = it }

        session.kill()

        awaitUntil("the spawned child to be gone", TimeUnit.SECONDS.toMillis(20)) {
            !ProcessHandle.of(childPid).map { it.isAlive }.orElse(false)
        }
    }

    /**
     * A session that was never started refuses input and refuses to stop, rather than throwing.
     *
     * Reachable from the GUI: the Start button builds a session before the process exists, and a user can
     * reach the console's controls in that window.
     */
    @Test
    fun aSessionThatWasNeverStartedRefusesEverythingQuietly(@TempDir packDir: File) {
        val session = sessionOver(
            packDir,
            Collections.synchronizedList(mutableListOf()),
            Collections.synchronizedList(mutableListOf()),
            IntArray(1)
        )

        Assertions.assertFalse(session.send("hello"), "There is no process to send to.")
        Assertions.assertFalse(session.stop(), "There is no process to stop.")
        session.kill()
        Assertions.assertEquals(SessionState.Starting, session.state)
    }

    /**
     * A listener that throws must not kill the reader thread, or one bad line costs the whole console.
     *
     * `emit` wraps the callback for exactly this, and nothing proved it: the output after the throw is the
     * assertion, because a dead reader thread simply stops delivering and looks like a quiet server.
     */
    @Test
    fun aThrowingLineListenerDoesNotKillTheConsole(@TempDir packDir: File) {
        requireBash()
        standInScript(packDir)
        val seen = Collections.synchronizedList(mutableListOf<String>())
        val session = ServerSession(
            workingDirectory = packDir,
            command = listOf("bash", "start.sh"),
            onLine = { line -> seen.add(line); throw IllegalStateException("a listener misbehaving") },
            onState = { },
            onClosed = { }
        ).also { started.add(it) }

        session.start()
        awaitUntil("the stand-in to be reading") { seen.isNotEmpty() }
        session.send("still alive")

        awaitUntil("output to keep arriving after the listener threw") { seen.contains("echoed:still alive") }
    }

    /**
     * `stop()` and `kill()` on a session that has already exited are no-ops that leave its status intact.
     *
     * **What this does not pin, stated so nobody assumes it does.** `transitionTo` also refuses to move off
     * `Exited`, which defends against a *race*: `stop()` checks liveness, the process exits, `pump`
     * publishes `Exited`, and the late `Stopping` overwrites it. Removing that guard leaves this guard
     * green — measured — because once the process is genuinely dead `stop()` and `kill()` return before
     * reaching `transitionTo` at all. The race is real and no deterministic test in this suite reaches it,
     * so the guard in the production code stands on reasoning rather than on this test.
     */
    @Test
    fun stopAndKillAfterExitLeaveTheStatusIntact(@TempDir packDir: File) {
        requireBash()
        File(packDir, "start.sh").writeText("#!/usr/bin/env bash\nexit 5\n")
        val session = sessionOver(
            packDir,
            Collections.synchronizedList(mutableListOf()),
            Collections.synchronizedList(mutableListOf()),
            IntArray(1)
        )
        session.start()
        awaitUntil("the exit to be observed") { session.state is SessionState.Exited }

        session.stop()
        session.kill()

        Assertions.assertEquals(SessionState.Exited(5), session.state, "Exited is terminal.")
    }

    /** A pack whose script cannot be spawned reports an exit rather than throwing into the caller. */
    @Test
    fun anUnlaunchableCommandEndsAsAnExitRatherThanAnException(@TempDir packDir: File) {
        val closedCount = IntArray(1)
        val lines = Collections.synchronizedList(mutableListOf<String>())
        val session = ServerSession(
            workingDirectory = packDir,
            command = listOf("definitely-not-a-real-command-${System.nanoTime()}"),
            onLine = { lines.add(it) },
            onState = { },
            onClosed = { closedCount[0]++ }
        ).also { started.add(it) }

        session.start()

        Assertions.assertInstanceOf(SessionState.Exited::class.java, session.state)
        Assertions.assertEquals(1, closedCount[0], "A launch that never happened must still give the port back.")
        Assertions.assertTrue(lines.isNotEmpty(), "The console must say why nothing started.")
    }
}
