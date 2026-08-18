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
import org.apache.logging.log4j.kotlin.cachedLoggerOf

/**
 * Settings-group for the timeouts applied to every outbound HTTP call ServerPackCreator makes.
 *
 * These exist because the JDK's default is to wait *forever*: an `HttpURLConnection` with no
 * timeouts set never gives up on a host which accepts the connection and then goes silent, which is
 * exactly what a black-holing firewall or a stalled upstream looks like. Twelve manifest checks sit
 * on the blocking startup path, so an unbounded wait there is a hang with no recovery rather than a
 * slow start.
 *
 * Two timeouts are separate on purpose, because they answer different questions:
 * - **connect** bounds reaching the host at all, and can be short — either the TCP handshake
 *   completes quickly or the host is not there.
 * - **read** bounds how long a *single* read may block waiting for bytes. It is **not** a budget for
 *   the whole transfer, which is why a multi-megabyte download does not need a huge value — only a
 *   host that stalls mid-stream for longer than the timeout trips it.
 *
 * [downloadReadTimeout] is nevertheless separate from [readTimeout] because the two are used against
 * different things: a metadata manifest either answers promptly or is broken, while an installer or
 * mod jar is legitimately served by hosts that trickle bytes under load.
 */
class NetworkConfig(private val store: PropertyStore) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

    companion object {
        /**
         * Property-key holding the milliseconds to wait for a connection to be established.
         */
        const val CONNECT_TIMEOUT_KEY = "de.griefed.serverpackcreator.network.timeout.connect"

        /**
         * Property-key holding the milliseconds a single read of a metadata response may block.
         */
        const val READ_TIMEOUT_KEY = "de.griefed.serverpackcreator.network.timeout.read"

        /**
         * Property-key holding the milliseconds a single read of a file-download may block.
         */
        const val DOWNLOAD_READ_TIMEOUT_KEY = "de.griefed.serverpackcreator.network.timeout.download.read"

        /**
         * Shipped default milliseconds to wait for a connection. A constant so callers which have no
         * `PropertyStore` — `VersionChecker` in `-app` is one — can default to the same value instead of
         * repeating the literal, which is how equal-valued copies start drifting.
         */
        const val DEFAULT_CONNECT_TIMEOUT = 5_000

        /** Shipped default milliseconds a single read of a metadata response may block. */
        const val DEFAULT_READ_TIMEOUT = 15_000

        /** Shipped default milliseconds a single read of a file-download may block. */
        const val DEFAULT_DOWNLOAD_READ_TIMEOUT = 60_000
    }

    /**
     * Fallback milliseconds to wait for a connection to be established. Short by design: a
     * reachable host completes the handshake in well under this, so a longer value only prolongs
     * the wait on hosts which are not there.
     */
    val fallbackConnectTimeout = DEFAULT_CONNECT_TIMEOUT

    /**
     * Fallback milliseconds a single read of a metadata response may block.
     */
    val fallbackReadTimeout = DEFAULT_READ_TIMEOUT

    /**
     * Fallback milliseconds a single read of a file-download may block. Larger than
     * [fallbackReadTimeout] because installers and mod jars are served by hosts which trickle bytes
     * under load, where a stalled metadata endpoint is simply broken.
     */
    val fallbackDownloadReadTimeout = DEFAULT_DOWNLOAD_READ_TIMEOUT

    /**
     * Milliseconds to wait for a connection to be established before giving up.
     *
     * See [sanitise] for how invalid values are handled, and note that `0` is a deliberate escape
     * hatch meaning "wait forever" — the JDK's own semantics, and the behaviour SPC had before these
     * settings existed.
     */
    var connectTimeout = fallbackConnectTimeout
        get() {
            field = sanitise(store.getInt(CONNECT_TIMEOUT_KEY, fallbackConnectTimeout), fallbackConnectTimeout, CONNECT_TIMEOUT_KEY)
            return field
        }
        set(value) {
            // Sanitised once, then used for all three: storing one value while keeping and logging
            // another told an operator the opposite of what took effect.
            val sanitised = sanitise(value, fallbackConnectTimeout, CONNECT_TIMEOUT_KEY)
            store.setInt(CONNECT_TIMEOUT_KEY, sanitised)
            field = sanitised
            log.info("Connect-timeout set to: $field ms")
        }

    /**
     * Milliseconds a single read of a metadata response (version manifests, reachability checks) may
     * block before the call gives up.
     */
    var readTimeout = fallbackReadTimeout
        get() {
            field = sanitise(store.getInt(READ_TIMEOUT_KEY, fallbackReadTimeout), fallbackReadTimeout, READ_TIMEOUT_KEY)
            return field
        }
        set(value) {
            // Sanitised once, then used for all three: storing one value while keeping and logging
            // another told an operator the opposite of what took effect.
            val sanitised = sanitise(value, fallbackReadTimeout, READ_TIMEOUT_KEY)
            store.setInt(READ_TIMEOUT_KEY, sanitised)
            field = sanitised
            log.info("Read-timeout set to: $field ms")
        }

    /**
     * Milliseconds a single read of a file-download (installers, mod jars) may block before the
     * download is abandoned.
     */
    var downloadReadTimeout = fallbackDownloadReadTimeout
        get() {
            field = sanitise(
                store.getInt(DOWNLOAD_READ_TIMEOUT_KEY, fallbackDownloadReadTimeout),
                fallbackDownloadReadTimeout,
                DOWNLOAD_READ_TIMEOUT_KEY
            )
            return field
        }
        set(value) {
            val sanitised = sanitise(value, fallbackDownloadReadTimeout, DOWNLOAD_READ_TIMEOUT_KEY)
            store.setInt(DOWNLOAD_READ_TIMEOUT_KEY, sanitised)
            field = sanitised
            log.info("Download read-timeout set to: $field ms")
        }

    /**
     * Returns [timeout] unless it is negative, in which case [fallback] is returned and the bad
     * value logged against [key].
     *
     * Negatives are rejected rather than passed through because `setConnectTimeout`/`setReadTimeout`
     * throw `IllegalArgumentException` on them — a typo in a properties file must not turn every
     * network call into a crash. Zero is left alone; it is the JDK's "no timeout".
     */
    private fun sanitise(timeout: Int, fallback: Int, key: String): Int =
        if (timeout < 0) {
            log.error("Negative timeout $timeout specified for $key. Defaulting to $fallback ms.")
            fallback
        } else {
            timeout
        }
}
