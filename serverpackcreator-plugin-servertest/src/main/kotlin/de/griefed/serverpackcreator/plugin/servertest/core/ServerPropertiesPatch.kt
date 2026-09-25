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

/**
 * Borrows a server pack's `server.properties` for the duration of a test run, and gives it back.
 *
 * The port a test server listens on can only be set here. The start scripts interpolate `ADDITIONAL_ARGS`
 * *before* `-jar`, in JVM-argument position, so Minecraft's own `--port` never reaches the server — which
 * leaves the properties file as the only way in.
 *
 * That file is on ServerPackCreator's own protected-paths list
 * ([GenerationConfig.updateProtectedPaths][de.griefed.serverpackcreator.api.ApiProperties.updateProtectedPaths])
 * precisely because it is the user's hand-tuned file that a regeneration must never take. So this borrows it
 * rather than owning it: the original is copied aside, only the port lines are rewritten, and [restore] puts
 * the original back byte for byte — including putting it back to *absent*, when there was no file to start
 * with.
 *
 * **The backup's presence is the crash marker.** A ServerPackCreator killed mid-run leaves the patched file
 * and the backup beside it; the next [borrow] restores from that backup before doing anything else, so a
 * user's real port can never be overwritten by a previous run's borrowed one.
 *
 * @param packDirectory The server pack whose properties are being borrowed.
 * @author Griefed
 */
class ServerPropertiesPatch(private val packDirectory: File) {

    /** The pack's properties file, whether or not it exists yet. */
    val propertiesFile: File = File(packDirectory, PROPERTIES_NAME)

    /** Where the original is kept while it is borrowed. Its presence means a borrow is outstanding. */
    val backupFile: File = File(packDirectory, BACKUP_NAME)

    /**
     * Whether this pack has RCON switched on, read from the file as it stands right now.
     *
     * Asked before [borrow] so the caller knows whether to allocate a second port: `rcon.port` is a real
     * listening socket when enabled, and two test servers sharing one collide exactly as their game ports
     * would. Off in ServerPackCreator's shipped properties, so most packs never need the second port.
     */
    fun rconEnabled(): Boolean = false

    /**
     * Take the file, pointing the server at [serverPort] (and RCON at [rconPort] when one is supplied).
     *
     * Restores any outstanding backup first, so a crashed previous run cannot leave its borrowed port to be
     * backed up as though it were the user's.
     */
    fun borrow(serverPort: Int, rconPort: Int? = null) {
    }

    /**
     * Give the file back exactly as it was, and forget the borrow.
     *
     * Idempotent: with no backup outstanding there is nothing to give back, which is also what makes it safe
     * to call from both the normal stop path and a shutdown hook.
     */
    fun restore() {
    }

    companion object {
        /** Minecraft's own settings file, and the only place a server's port can be set from here. */
        const val PROPERTIES_NAME = "server.properties"

        /** The borrowed original. Named for this plugin so it is obvious who left it behind after a crash. */
        const val BACKUP_NAME = "server.properties.servertest-backup"

        /** The key naming the port the game itself listens on. */
        const val SERVER_PORT_KEY = "server-port"

        /** The key naming the UDP query port, which vanilla keeps equal to the game port. */
        const val QUERY_PORT_KEY = "query.port"

        /** The key naming the RCON port, only a real socket when `enable-rcon` is on. */
        const val RCON_PORT_KEY = "rcon.port"

        /** The switch deciding whether [RCON_PORT_KEY] is listened on at all. */
        const val ENABLE_RCON_KEY = "enable-rcon"
    }
}
