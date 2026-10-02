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
import java.util.concurrent.TimeUnit

/**
 * Pins that [ServerSession.isStillRunning] separates a *running* process from one that has already exited
 * and is only waiting to be reaped.
 *
 * `ProcessHandle.isAlive` is true for a zombie: the process is gone, its parent has merely not called
 * `wait()` yet. An orphan is reparented to PID 1, and a container whose PID 1 is not an init never reaps
 * it — so the zombie is permanent there, which is exactly the CI job container. That is why
 * `SessionRegistryTest.killAllEndsTheServersAndWhatTheySpawned` read a correctly-killed `sleep` as a
 * survivor in CI while the same code stayed green on a developer machine, where launchd reaps.
 *
 * `-clientside` pins the same distinction in `HostProcessLivenessTest`. The knowledge is duplicated rather
 * than shared
 * because `-plugin-servertest` depends on `-api` **only, deliberately** — a plugin compiles against the
 * published API surface, and reaching into `-clientside` for a two-line predicate would buy a module
 * dependency this project has chosen not to have. If a third module needs it, that is the point at which
 * it has earned a home in `-api` instead of a third copy.
 *
 * The zombie is staged without touching PID 1: `bash` backgrounds a child and then `exec`s itself into
 * `sleep`, so the child's parent becomes a process that will never call `wait()`.
 */
internal class SessionLivenessTest {

    /** The `exec`ed parent holding the zombie unreaped; destroyed after each test so nothing leaks. */
    private var keeper: Process? = null

    /**
     * Kill the keeper, which lets the operating system finally reap the zombie staged against it.
     *
     * Without this the child would stay in the process table for as long as the Gradle test JVM lives,
     * which is the very leak the production fix exists to avoid.
     */
    @AfterEach
    fun killKeeper() {
        keeper?.let {
            it.destroyForcibly()
            it.waitFor(10, TimeUnit.SECONDS)
        }
    }

    /**
     * A descendant that has exited but has not been reaped must not count as still running.
     *
     * Both directions are asserted from one fixture: the same handle is checked while the process really
     * is running, and again once it is a zombie. A predicate that answered one of them by always
     * returning the same value would fail the other — which is what makes this a pin and not a
     * restatement of whichever branch happens to be implemented.
     */
    @Test
    fun aDescendantAwaitingReapingIsNotStillRunning(@TempDir workDir: File) {
        Assumptions.assumeTrue(File("/bin/bash").canExecute(), "Needs bash to background a child and exec.")
        val child = stageUnreapableChild(workDir)

        Assertions.assertTrue(
            ServerSession.isStillRunning(child),
            "A process that is genuinely running must count as running, or teardown would skip killing it."
        )

        child.destroy()
        val unreaped = awaitUnreaped(child)
        // Not an assertion: on a platform that reaps the orphan itself the trap cannot be staged at all,
        // and a skip says that honestly where a pass would claim a guard that never ran.
        Assumptions.assumeTrue(
            unreaped && child.isAlive,
            "This platform reaped the orphan itself; the unreaped-descendant trap cannot be staged here."
        )

        Assertions.assertFalse(
            ServerSession.isStillRunning(child),
            "Process ${child.pid()} has exited and is only awaiting reaping, but teardown still counts it " +
                    "as running. `stop()` would wait on it until the graceful budget expires and then " +
                    "force-kill a corpse, and every guard asking whether a session's children survived " +
                    "would read it as one."
        )
    }

    /**
     * Start a child whose parent can never reap it, and return its handle.
     *
     * `bash` backgrounds `sleep`, records its PID, then `exec`s itself into another `sleep` — the PID
     * stays, but the shell that would have reaped is gone, so the recorded child is left unreapable for
     * as long as the keeper lives.
     */
    private fun stageUnreapableChild(workDir: File): ProcessHandle {
        val pidFile = File(workDir, "child.pid")
        File(workDir, "keeper.sh").writeText(
            """
            #!/usr/bin/env bash
            sleep 300 &
            echo ${'$'}! > "${pidFile.absolutePath}"
            exec sleep 300
            """.trimIndent() + "\n"
        )
        keeper = ProcessBuilder("bash", "keeper.sh")
            .directory(workDir)
            .redirectErrorStream(true)
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .start()

        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(FIXTURE_TIMEOUT_SECONDS)
        while (!pidFile.isFile && System.nanoTime() < deadline) {
            Thread.sleep(POLL_MILLIS)
        }
        Assumptions.assumeTrue(pidFile.isFile, "The keeper script never reported a child PID.")
        val pid = pidFile.readText().trim().toLong()
        return ProcessHandle.of(pid).orElseThrow { AssertionError("Child $pid vanished before it could be used.") }
    }

    /**
     * Wait until [child] has exited without being reaped, reporting whether that state was reached.
     *
     * The wait watches `command()` because `onExit()` cannot be used here: it never completes for a
     * process nobody will reap, which is the same trap this test exists to pin — and the same trap
     * `ServerSession.stop()` walks into when it waits on descendants. This is synchronisation only; what
     * the test proves is carried by the two assertions around it.
     */
    private fun awaitUnreaped(child: ProcessHandle): Boolean {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(FIXTURE_TIMEOUT_SECONDS)
        while (child.info().command().isPresent && System.nanoTime() < deadline) {
            Thread.sleep(POLL_MILLIS)
        }
        return child.info().command().isEmpty
    }

    private companion object {
        /** How long the fixture waits for the operating system to catch up before giving up on staging. */
        const val FIXTURE_TIMEOUT_SECONDS = 5L

        /** Cadence for the fixture's two waits; short because both transitions are near-instant. */
        const val POLL_MILLIS = 25L
    }
}
