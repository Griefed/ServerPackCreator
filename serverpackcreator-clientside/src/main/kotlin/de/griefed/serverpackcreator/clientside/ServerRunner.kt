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
package de.griefed.serverpackcreator.clientside

import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Outcome of actually executing a prepared server pack, independent of *how* it was run (host process
 * or — for the grinder — an isolated container). Kept raw on purpose: classification belongs to the
 * caller so every runner is judged by the same [BootLogClassifier].
 *
 * @author Griefed
 */
sealed interface RunResult {
    /** The pack could not be launched at all (e.g. no start script); carries why, for the report. */
    data class NotStarted(val detail: String) : RunResult

    /**
     * The server ran to a terminal state. [lines] is the full console output, [exitCode] the process
     * exit (or `null` if it had to be force-killed), [timedOut] whether the budget elapsed before the
     * ready-line appeared.
     */
    data class Completed(val lines: List<String>, val exitCode: Int?, val timedOut: Boolean) : RunResult
}

/**
 * Runs a generated server pack and returns its raw console output + exit status, force-stopping the
 * server once it is ready or once `timeout` elapses. The host-process implementation spawns `start.sh`
 * directly; the planned grinder supplies a container-backed implementation that mounts the pack into an
 * isolated, network-less container. Both feed the same [BootLogClassifier], so the verdict logic is
 * shared.
 *
 * @author Griefed
 */
fun interface ServerRunner {
    /**
     * Boot [serverPack], stopping once the server is ready or [timeout] elapses. Returns the *raw*
     * console lines + exit status; persisting and classifying them is the caller's concern
     * ([BootVerifier.outcomeFor]).
     *
     * [onLine] is invoked for **every console line as it arrives**, so a caller can persist the boot log while
     * the boot is still running. Without it the output only becomes visible once the run finishes, which makes a
     * hung boot undiagnosable until its timeout fires — the whole point is that an operator can `tail -f` a boot
     * in progress. Implementations must still return the complete line list; the sink is an addition, not a
     * replacement, and must never be allowed to fail the boot (see the callers, which swallow sink errors).
     */
    fun run(serverPack: File, timeout: Duration, onLine: (String) -> Unit): RunResult
}

/**
 * The default [ServerRunner]: boots the pack by spawning its `start.sh` as a host child-process,
 * streaming stdout/stderr while watching for the vanilla ready-line, and force-killing the process once
 * ready or timed out. This is the behaviour the clientside boot-verification has always used; the
 * grinder swaps in a container-backed runner for isolation and parallelism.
 *
 * @author Griefed
 */
class HostProcessServerRunner : ServerRunner {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

    /** The vanilla server's ready-line, watched while streaming the boot to stop early. */
    private val readyLine = Regex("""Done \([^)]*\)! For help""")

    override fun run(serverPack: File, timeout: Duration, onLine: (String) -> Unit): RunResult {
        val startScript = File(serverPack, "start.sh")
        if (!startScript.isFile) {
            return RunResult.NotStarted("No start.sh in the generated server pack.")
        }
        File(serverPack, "eula.txt").writeText("eula=true\n")
        startScript.setExecutable(true)

        log.info("Booting server pack at ${serverPack.absolutePath}")
        val process = ProcessBuilder("bash", startScript.name)
            .directory(serverPack)
            .redirectErrorStream(true)
            .redirectInput(ProcessBuilder.Redirect.from(File("/dev/null")))
            .start()

        val lines = ArrayList<String>()
        val ready = AtomicBoolean(false)
        val readerThread = Thread {
            process.inputStream.bufferedReader().useLines { sequence ->
                for (line in sequence) {
                    synchronized(lines) { lines.add(line) }
                    onLine(line)
                    if (readyLine.containsMatchIn(line)) {
                        ready.set(true)
                    }
                }
            }
        }.apply { isDaemon = true; start() }

        // The budget must not be spent while the host is asleep. A suspend freezes the server mid-boot and a
        // wall-clock deadline then expires on a boot that never got the time — measured in the grinder, a laptop
        // idle-sleeping in ~16-minute cycles produced 19 of 153 verdicts reading `timed out`, several of them
        // `SURVIVED (timed out)` whose console showed the server reaching ready seconds after launch. The container
        // engine was fixed for this first; this path, which the `-verifyclientside` verb uses, had the same hole.
        val deadline = SuspendAwareDeadline(timeout, POLL_INTERVAL_MILLIS) { gapMillis ->
            log.warn(
                "The host appears to have suspended for ~${gapMillis / 1000}s while booting; that time is not counted " +
                    "against the boot's ${timeout.toMinutes()}-minute budget. A boot interrupted this way learns " +
                    "nothing either way, so keep the machine awake (e.g. `caffeinate -ims` on macOS)."
            )
        }
        while (process.isAlive && !ready.get() && deadline.hasTimeLeft()) {
            Thread.sleep(POLL_INTERVAL_MILLIS)
            deadline.tick()
        }
        val timedOut = !ready.get() && !deadline.hasTimeLeft()

        if (process.isAlive) {
            process.destroyForcibly()
            process.waitFor(30, TimeUnit.SECONDS)
        }
        readerThread.join(5_000)
        val exitCode = if (process.isAlive) null else runCatching { process.exitValue() }.getOrNull()

        return RunResult.Completed(synchronized(lines) { ArrayList(lines) }, exitCode, timedOut)
    }

    companion object {
        /** How often the boot's liveness and ready-state are polled; also sets what counts as a suspend gap. */
        internal const val POLL_INTERVAL_MILLIS = 500L
    }
}
