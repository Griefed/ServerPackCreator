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

/**
 * Where a launched server pack is in its life, as the console pane reports it.
 *
 * Deliberately about the *script*, not the game: `start.sh` installs a modloader and may prompt before a
 * server is ever launched, so [Starting] covers everything up to the ready-line and [Exited] is the script's
 * exit, which the template is careful to make the server's own status.
 */
sealed interface SessionState {

    /** Launched, but the ready-line has not appeared. Installing, prompting, or still booting. */
    data object Starting : SessionState

    /** The server announced itself ready. This is the point at which a client can connect. */
    data object Ready : SessionState

    /** A stop has been asked for and the process has not gone yet. */
    data object Stopping : SessionState

    /**
     * The script has exited. [exitCode] is its status, or `null` when it could not be read because the
     * process had to be forced.
     */
    data class Exited(val exitCode: Int?) : SessionState
}
