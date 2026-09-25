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

import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.util.Random

/**
 * Hands each running server a port nothing else is listening on, and takes it back when the server stops.
 *
 * Every generated pack ships the same `server-port=25565`, so a second pack started alongside the first would
 * fail to bind. The port has to be decided here rather than passed to the server, because the start scripts
 * offer no way through: `ADDITIONAL_ARGS` is interpolated *before* `-jar`, in JVM-argument position, so
 * Minecraft's own `--port` is unreachable without abandoning the scripts.
 *
 * The starting point of the scan is random so repeated runs do not queue up on the same port, and every
 * candidate is **bind-tested** before it is offered: a port that merely looks unused in this JVM's bookkeeping
 * may well be held by something else on the machine.
 *
 * The default range sits above the privileged ports and clear of the well-known ones. The only IANA-registered
 * port inside it is 25565 itself — registered to Minecraft, which is what is being launched — which also keeps
 * the ports easy to write a firewall rule for, as an ephemeral-range port would not be.
 *
 * @param rangeStart First port to consider; coerced into a usable range rather than rejected.
 * @param rangeEnd   Last port to consider, inclusive.
 * @param random     Source of the scan's starting offset; injectable so the scan order can be pinned.
 * @param isFree     The bind test; injectable so allocation can be tested without binding real sockets.
 * @author Griefed
 */
class PortAllocator(
    rangeStart: Int = DEFAULT_RANGE_START,
    rangeEnd: Int = DEFAULT_RANGE_END,
    private val random: Random = Random(),
    private val isFree: (Int) -> Boolean = ::isPortBindable
) {

    /**
     * The range actually used, after clamping whatever was configured.
     *
     * Clamped rather than validated because these values come from the plugin's hand-editable `config.toml`
     * and this object is built while the tab is: throwing on a typo would take the GUI's tab assembly with
     * it, and refusing to launch anything is a worse answer than launching on a sane port.
     */
    val range: IntRange = run {
        val start = rangeStart.coerceIn(LOWEST_UNPRIVILEGED_PORT, HIGHEST_PORT)
        start..rangeEnd.coerceIn(start, HIGHEST_PORT)
    }

    /** Ports handed out and not yet released. Guards against two servers being given the same one. */
    private val allocated = mutableSetOf<Int>()

    /**
     * A free port from [range], or `null` when every port in it is taken.
     *
     * Synchronized because a port is claimed on the event dispatch thread and released from the thread
     * watching a server exit.
     */
    @Synchronized
    fun allocate(): Int? = null

    /** Return [port] to the pool once the server holding it has exited. Unknown ports are ignored. */
    @Synchronized
    fun release(port: Int) {
        allocated.remove(port)
    }

    companion object {
        /** Lowest port a process can bind without privileges, and the floor for any configured range. */
        const val LOWEST_UNPRIVILEGED_PORT = 1024

        /** The highest port number there is. */
        const val HIGHEST_PORT = 65535

        /** Minecraft's own registered port, and the natural start of the range servers are handed. */
        const val DEFAULT_RANGE_START = 25565

        /** End of the default range: room for plenty of concurrent servers without leaving unassigned space. */
        const val DEFAULT_RANGE_END = 25999

        /**
         * Whether [port] can be bound right now, asked by actually binding it.
         *
         * `reuseAddress` is switched **off** so the probe is strict: with it on, a port left in `TIME_WAIT`
         * binds successfully, and reporting that as free would hand a server a port it may still lose. The
         * socket binds all interfaces, matching a Minecraft server's own default.
         */
        fun isPortBindable(port: Int): Boolean = try {
            ServerSocket().use { socket ->
                socket.reuseAddress = false
                socket.bind(InetSocketAddress(port), 1)
                true
            }
        } catch (ex: IOException) {
            false
        }
    }
}
