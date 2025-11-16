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
package de.griefed.serverpackcreator.app.web.modpack

class ZipResponse(
    val message: String,
    val success: Boolean,
    val modPackId: String?,
    val runConfigId: String?,
    val serverPackId: String?,
    val status: ModPackStatus?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ZipResponse

        if (message != other.message) return false
        if (success != other.success) return false
        if (modPackId != other.modPackId) return false
        if (runConfigId != other.runConfigId) return false
        if (serverPackId != other.serverPackId) return false
        if (status != other.status) return false

        return true
    }

    override fun hashCode(): Int {
        var result = message.hashCode()
        result = 31 * result + success.hashCode()
        result = 31 * result + modPackId.hashCode()
        result = 31 * result + runConfigId.hashCode()
        result = 31 * result + serverPackId.hashCode()
        result = 31 * result + status.hashCode()
        return result
    }

    override fun toString(): String {
        return "ZipResponse(message='$message', success=$success, modPackId=$modPackId, configId=$runConfigId, serverPackId=$serverPackId, status=$status)"
    }
}