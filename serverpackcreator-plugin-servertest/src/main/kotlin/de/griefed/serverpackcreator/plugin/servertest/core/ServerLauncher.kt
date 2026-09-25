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
import java.util.concurrent.atomic.AtomicBoolean

/**
 * What came of asking to launch a pack.
 *
 * A sealed pair rather than a nullable session, because a refusal carries a reason the user has to read
 * and a success carries a port the user has to connect to — neither is the absence of the other.
 */
sealed interface LaunchOutcome {

    /**
     * The pack is registered and its session built, **but not started** — the caller starts it once its
     * console is on screen, so no output can arrive before there is anywhere to put it.
     */
    data class Started(
        /** The session to start, already registered so a second Start cannot race into the same world. */
        val session: ServerSession,
        /** The port written into the pack's borrowed `server.properties`, and the one to connect to. */
        val port: Int
    ) : LaunchOutcome

    /** Nothing was launched and nothing was taken. [reason] is written for a user, not a log. */
    data class Refused(
        /** Why, in words a dialog can show. */
        val reason: String
    ) : LaunchOutcome
}

/**
 * Everything that has to happen, in order, to get a server pack running — and everything that has to be
 * given back afterwards.
 *
 * Lives in `core` rather than in the tab because none of it is rendering: refusing a pack that is already
 * running, taking a port, taking a *second* port only when RCON is on, borrowing `server.properties`,
 * registering the session before it starts, and releasing all of that exactly once however the run ends.
 * An audit found the whole sequence untested while it sat inside a Swing view; here every branch is
 * reachable headless.
 *
 * @param allocator  Hands out ports and takes them back.
 * @param registry   Owns the running sessions, and refuses a second one for the same pack.
 * @param patchFor   Builds the `server.properties` borrower for a pack.
 * @param sessionFor Builds the session. Injectable so a guard can hold the close callback this class
 *                   wired and invoke it, which is the only way to reach the give-back without spawning a
 *                   real server — the parameters mirror [ServerSession]'s constructor exactly.
 * @author Griefed
 */
class ServerLauncher(
    private val allocator: PortAllocator,
    private val registry: SessionRegistry,
    private val patchFor: (File) -> ServerPropertiesPatch = { ServerPropertiesPatch(it) },
    private val sessionFor: (
        workingDirectory: File,
        command: List<String>,
        onLine: (String) -> Unit,
        onState: (SessionState) -> Unit,
        onClosed: () -> Unit
    ) -> ServerSession = { directory, command, onLine, onState, onClosed ->
        ServerSession(directory, command, onLine, onState, onClosed)
    }
) {

    /**
     * Take everything [pack] needs and build its session, or refuse and take nothing.
     *
     * [onClosed] runs after the plugin's own give-back, so a caller can drop its console without having to
     * know that a port and a borrowed file were released first.
     */
    fun launch(
        pack: LaunchablePack,
        onLine: (String) -> Unit,
        onState: (SessionState) -> Unit,
        onClosed: () -> Unit
    ): LaunchOutcome {
        val selection = pack.selection as? StartScriptSelection.Available
            ?: return LaunchOutcome.Refused((pack.selection as StartScriptSelection.Missing).reason)

        if (registry.isRunning(pack.directory)) {
            return LaunchOutcome.Refused(
                "${pack.name} is already running. Two servers over one world directory would corrupt it."
            )
        }

        val patch = patchFor(pack.directory)
        val serverPort = allocator.allocate() ?: return LaunchOutcome.Refused(
            "No free port between ${allocator.range.first} and ${allocator.range.last}. Stop a running " +
                    "server, or widen the range in this plugin's config.toml."
        )
        // Only when the pack has RCON switched on: it is a second real listening socket, and two test
        // servers sharing one collide exactly as their game ports would.
        val rconPort = if (patch.rconEnabled()) allocator.allocate() else null

        val givenBack = AtomicBoolean(false)
        val giveBack = {
            // Guarded because it is reachable twice: from the session's close callback, and from the
            // failure path below when the borrow itself fails before any session exists. Releasing twice
            // would hand a live server's port to the next pack.
            if (givenBack.compareAndSet(false, true)) {
                runCatching { patch.restore() }
                allocator.release(serverPort)
                rconPort?.let(allocator::release)
            }
        }

        try {
            patch.borrow(serverPort, rconPort)
        } catch (ex: Exception) {
            giveBack()
            return LaunchOutcome.Refused(
                "Could not set the port in ${pack.name}'s ${ServerPropertiesPatch.PROPERTIES_NAME}: ${ex.message}"
            )
        }

        // A second guard, deliberately not the same one. `giveBack` covers the resources and is shared
        // with the failure path above, where the caller must NOT be told a session closed -- it never got
        // one. This covers the whole close, so a caller is told exactly once even though `ServerSession`
        // already promises that; the promise is one class away and this costs a boolean.
        val closed = AtomicBoolean(false)
        val session = sessionFor(pack.directory, selection.command, onLine, onState) {
            if (closed.compareAndSet(false, true)) {
                giveBack()
                registry.unregister(pack.directory)
                onClosed()
            }
        }

        // Registered before it is handed back, so a second Start pressed in the same instant is refused
        // rather than racing into a second server over the same world.
        if (!registry.register(pack.directory, session)) {
            giveBack()
            return LaunchOutcome.Refused("${pack.name} is already running.")
        }

        return LaunchOutcome.Started(session, serverPort)
    }
}
