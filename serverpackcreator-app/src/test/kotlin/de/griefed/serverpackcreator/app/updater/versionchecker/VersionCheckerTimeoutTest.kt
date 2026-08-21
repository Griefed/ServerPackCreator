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
package de.griefed.serverpackcreator.app.updater.versionchecker

import de.griefed.serverpackcreator.api.settings.NetworkConfig
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import java.net.URL
import java.util.Optional
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Guards that the update check gives up instead of waiting forever.
 *
 * `VersionChecker.getResponse` is how both the GitHub and GitLab checkers talk to their APIs, and the
 * GUI runs an update check on startup (`GitHubChecker` logs its version list there). It opened a
 * connection with the JDK's infinite default timeouts, so a host that accepts and then goes silent
 * blocked the check with no bound.
 *
 * A hang guard, not a latency assertion: the bound is far above any realistic timeout, so only an
 * *unbounded* wait can trip it.
 */
internal class VersionCheckerTimeoutTest {

    /**
     * How long a request may take before the test calls it a hang.
     *
     * Comfortably above the shipped default read-timeout (15 s, [NetworkConfig.DEFAULT_READ_TIMEOUT]),
     * because this guard must distinguish "gave up after its configured timeout" from "waited forever" —
     * and a bound *equal* to the timeout races between the two. That costs this one test ~15 s once the
     * request is bounded, which is the price of guarding an unbounded wait without a settable timeout to
     * shorten: `VersionChecker` has none until the fix introduces it, and a guard may not depend on the
     * thing it is guarding.
     */
    private val hangThresholdSeconds = 45L

    /** Exposes the protected request method, which is the unit under test. */
    private class ProbingVersionChecker : VersionChecker() {
        override fun allVersions(): List<String> = listOf("1.0.0")
        override fun refresh(): VersionChecker = this
        override fun latestVersion(checkForPreRelease: Boolean): String = "1.0.0"
        override fun getDownloadUrl(version: String): String = ""
        override fun setRepository() {}
        override fun check(currentVersion: String, checkForPreReleases: Boolean): Optional<Update> =
            Optional.empty()

        /** Calls the protected request method so a test can time it. */
        fun request(url: URL): String = getResponse(url)
    }

    /**
     * Runs [probe] on a daemon thread and fails if it has not finished within [hangThresholdSeconds].
     * A bounded wait rather than a direct call, because the defect being guarded is an infinite one.
     */
    private fun <T> withinBound(what: String, probe: () -> T): T {
        val task = FutureTask(probe)
        Thread(task, "update-check-timeout-guard").apply { isDaemon = true }.start()
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
     * Opens a loopback server which accepts connections and never answers, and hands [test] its URL.
     * Accepted sockets stay open on purpose: closing them would hand the client an EOF and let it
     * proceed, which is the opposite of the stall being simulated.
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
                    // Socket closed as the test finished; nothing left to accept.
                }
            }
            acceptor.isDaemon = true
            acceptor.start()
            try {
                test(URI("http://127.0.0.1:${stalled.localPort}/releases"))
            } finally {
                accepted.forEach { runCatching { it.close() } }
            }
        }
    }

    /**
     * Pins that a stalled API host makes the request fail rather than block. The failure itself is an
     * `IOException`, which is what every caller of `getResponse` already handles.
     */
    @Test
    fun anUpdateCheckGivesUpOnAServerThatAcceptsButNeverResponds() {
        withStalledServer { url ->
            val checker = ProbingVersionChecker()
            val failed = withinBound("getResponse") {
                runCatching { checker.request(url.toURL()) }.exceptionOrNull()
            }
            Assertions.assertNotNull(failed, "A stalled host must fail the request, not answer it")
        }
    }
}
