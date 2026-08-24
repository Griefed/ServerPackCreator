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
package de.griefed.serverpackcreator.app.web.task

import de.griefed.serverpackcreator.api.config.PackConfig
import de.griefed.serverpackcreator.app.web.modpack.ModPack
import de.griefed.serverpackcreator.app.web.serverpack.ServerPack
import de.griefed.serverpackcreator.app.web.serverpack.customizing.RunConfiguration
import java.io.File

class TaskDetail(val modpack: ModPack) {

    /** The server pack being produced, once it has been created. */
    var serverPack: ServerPack? = null
    /** The API-level configuration the generation runs with, derived from [runConfiguration]. */
    var packConfig: PackConfig? = null
    /** The stored configuration this task was submitted with. */
    var runConfiguration: RunConfiguration? = null

    /** Where the finished archive landed on disk. */
    var serverPackFile: File? = null
    /** The uploaded archive this task reads from. */
    var modPackFile: File? = null

    /** Compares the modpack, server pack, configuration and run configuration — the four things that identify the work, not the files it happens to have produced yet. */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TaskDetail

        if (modpack != other.modpack) return false
        if (serverPack != other.serverPack) return false
        if (packConfig != other.packConfig) return false
        if (runConfiguration != other.runConfiguration) return false

        return true
    }

    /** Hashes the same four fields [equals] compares. */
    override fun hashCode(): Int {
        var result = modpack.hashCode()
        result = 31 * result + (serverPack.hashCode())
        result = 31 * result + (packConfig.hashCode())
        result = 31 * result + (runConfiguration.hashCode())
        return result
    }

    /** Every field, for a log line. */
    override fun toString(): String {
        return "TaskDetail(modpack=$modpack, serverPack=$serverPack, packConfig=$packConfig, runConfiguration=$runConfiguration)"
    }
}