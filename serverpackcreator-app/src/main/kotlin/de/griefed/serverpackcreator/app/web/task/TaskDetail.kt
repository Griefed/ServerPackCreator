/* Copyright (C) 2025 Griefed
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

    var serverPack: ServerPack? = null
    var packConfig: PackConfig? = null
    var runConfiguration: RunConfiguration? = null

    var serverPackFile: File? = null
    var modPackFile: File? = null

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

    override fun hashCode(): Int {
        var result = modpack.hashCode()
        result = 31 * result + (serverPack?.hashCode() ?: 0)
        result = 31 * result + (packConfig?.hashCode() ?: 0)
        result = 31 * result + (runConfiguration?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "TaskDetail(modpack=$modpack, serverPack=$serverPack, packConfig=$packConfig, runConfiguration=$runConfiguration)"
    }
}