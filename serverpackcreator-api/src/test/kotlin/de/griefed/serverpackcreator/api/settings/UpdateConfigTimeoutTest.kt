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
package de.griefed.serverpackcreator.api.settings

import de.griefed.serverpackcreator.api.PropertyStore
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Guards that the fallback-list refresh gives up instead of waiting forever.
 *
 * This one sits **earlier** on the startup path than anything else that reaches the network:
 * `ApiProperties`' own `init` calls `loadProperties`, which calls `updateFallback()`, so merely
 * *constructing* `ApiProperties` fetches the update URL. A host that accepts the connection and then
 * goes silent therefore blocks before `stageOne` has finished — earlier than the version-manifest
 * checks whose timeouts were added first, and with the same "no recovery but killing the process"
 * outcome.
 *
 * A hang guard, not a latency assertion: the bound is far above any configured timeout, so only an
 * *unbounded* wait can trip it.
 */
internal class UpdateConfigTimeoutTest {

    /** How long the refresh may take before the test calls it a hang. */
    private val hangThresholdSeconds = 15L

    /**
     * Runs [probe] on a daemon thread and fails if it has not finished within [hangThresholdSeconds].
     * A bounded wait rather than a direct call, because the defect being guarded is an infinite one —
     * calling it inline would hang the whole suite instead of failing this test.
     */
    private fun <T> withinBound(what: String, probe: () -> T): T {
        val task = FutureTask(probe)
        Thread(task, "update-timeout-guard").apply { isDaemon = true }.start()
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
     * Accepted sockets are held open deliberately — closing them would hand the client an EOF and let
     * it proceed, which is the opposite of the stall being simulated.
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
                test(URI("http://127.0.0.1:${stalled.localPort}/serverpackcreator.properties"))
            } finally {
                accepted.forEach { runCatching { it.close() } }
            }
        }
    }

    /**
     * Pins that a stalled update-host makes the refresh fail rather than block, and that it reports
     * "nothing updated" rather than throwing.
     */
    @Test
    fun theFallbackRefreshGivesUpOnAServerThatAcceptsButNeverResponds() {
        withStalledServer { url ->
            val store = PropertyStore()
            // Short timeouts, so a bounded implementation resolves this well inside the guard's window.
            store.define(NetworkConfig.CONNECT_TIMEOUT_KEY, "250")
            store.define(NetworkConfig.READ_TIMEOUT_KEY, "250")
            val updateConfig = UpdateConfig(store, GenerationConfig(store)) { }
            updateConfig.updateUrl = url.toURL()

            val updated = withinBound("updateFallback") { updateConfig.updateFallback() }

            Assertions.assertFalse(updated, "Nothing can be refreshed from a host which never responds")
        }
    }
}
