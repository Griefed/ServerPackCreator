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
package de.griefed.serverpackcreator.api.utilities.common

import de.griefed.serverpackcreator.api.ApiProperties
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Guards that outbound HTTP calls give up instead of waiting forever.
 *
 * These are *hang* guards, not latency assertions: they point the utilities at a loopback server
 * which accepts the connection and then never writes a byte, which is what a black-holing firewall
 * or a stalled upstream looks like from the client side. Without a read-timeout the JDK waits
 * indefinitely — and twelve such calls sit on the blocking GUI startup path
 * (`ServerPackCreator` -> `ApiWrapper.stageTwo()` -> `VersionMeta.checkManifests`), so a stall there
 * leaves the splash screen stuck with no recovery but killing the process.
 *
 * The bound is deliberately orders of magnitude above any configured timeout: the point is to
 * distinguish "gave up" from "hung", not to measure how fast it gave up, so the guard cannot go
 * flaky on a loaded machine.
 */
internal class WebUtilitiesTimeoutTest {

    /** How long a probe may take before the test calls it a hang. Only an *unbounded* wait trips it. */
    private val hangThresholdSeconds = 15L

    /** Timeout handed to the utilities under test, in milliseconds. Short, so a stall resolves fast. */
    private val configuredTimeout = 250

    /**
     * An [ApiProperties] reporting [configuredTimeout] for every network timeout.
     *
     * **The explicit stubs are load-bearing — do not replace this with a bare
     * `mockk(relaxed = true)`.** A relaxed mock answers `0` for an `Int`, and `0` is the JDK's
     * "wait forever", so a relaxed mock silently reproduces the very defect these guards exist to
     * catch: both stall-guards failed against the *fixed* code until these stubs were added.
     */
    private fun timedProperties(): ApiProperties {
        val apiProperties = mockk<ApiProperties>(relaxed = true)
        every { apiProperties.networkConnectTimeout } returns configuredTimeout
        every { apiProperties.networkReadTimeout } returns configuredTimeout
        every { apiProperties.networkDownloadReadTimeout } returns configuredTimeout
        return apiProperties
    }

    /**
     * Runs [probe] on a daemon thread and fails, naming [what], if it has not finished within
     * [hangThresholdSeconds]; returns the probe's value once it does.
     *
     * A bounded wait rather than a direct call, because the defect being guarded is an *infinite*
     * wait — calling it inline would hang the entire suite instead of failing this one test.
     */
    private fun <T> withinBound(what: String, probe: () -> T): T {
        val task = FutureTask(probe)
        Thread(task, "timeout-guard").apply { isDaemon = true }.start()
        return try {
            task.get(hangThresholdSeconds, TimeUnit.SECONDS)
        } catch (_: TimeoutException) {
            Assertions.fail(
                "$what did not return within ${hangThresholdSeconds}s. The connection carries no " +
                        "read-timeout, so a host which accepts and never responds hangs the caller forever."
            )
        }
    }

    /**
     * Opens a loopback server which accepts connections and never answers, hands [test] a URL
     * pointing at it, and cleans up afterwards. Accepted sockets are held open on purpose — closing
     * them would hand the client an EOF and let it proceed, which is the opposite of the stall being
     * simulated.
     */
    private fun withStalledServer(test: (URI) -> Unit) {
        ServerSocket(0, 8, InetAddress.getLoopbackAddress()).use { stalled ->
            val accepted = mutableListOf<Socket>()
            val acceptor = Thread {
                try {
                    while (!stalled.isClosed) {
                        accepted.add(stalled.accept())
                    }
                } catch (_: Exception) {
                    // The socket was closed as the test finished; nothing left to accept.
                }
            }
            acceptor.isDaemon = true
            acceptor.start()
            try {
                test(URI("http://127.0.0.1:${stalled.localPort}/stalled-manifest.json"))
            } finally {
                accepted.forEach { runCatching { it.close() } }
            }
        }
    }

    /**
     * Pins that a reachability check gives up on a server which accepts and never responds, rather
     * than blocking its caller indefinitely.
     */
    @Test
    fun isReachableGivesUpOnAServerThatAcceptsButNeverResponds() {
        withStalledServer { url ->
            val webUtilities = WebUtilities(timedProperties())
            val reachable = withinBound("isReachable") { webUtilities.isReachable(url.toURL()) }
            Assertions.assertFalse(reachable, "A server which never responds is not reachable")
        }
    }

    /**
     * Pins the same for a download: a stalled host must fail the download rather than hang, and must
     * not leave a partial file behind.
     */
    @Test
    fun downloadFileGivesUpOnAServerThatAcceptsButNeverResponds() {
        withStalledServer { url ->
            val webUtilities = WebUtilities(timedProperties())
            val destination = File.createTempFile("spc-timeout-guard", ".json").apply { delete() }
            try {
                val downloaded = withinBound("downloadFile") {
                    webUtilities.downloadFile(destination, url.toURL())
                }
                Assertions.assertFalse(downloaded, "Nothing can be downloaded from a stalled host")
                Assertions.assertFalse(destination.isFile, "A failed download must not leave a file behind")
            } finally {
                destination.delete()
            }
        }
    }
}
