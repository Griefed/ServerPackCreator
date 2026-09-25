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
    ): LaunchOutcome = LaunchOutcome.Refused("Launching is not implemented yet.")
}
