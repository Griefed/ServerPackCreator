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
package de.griefed.serverpackcreator.api.versionmeta.neoforge

import de.griefed.serverpackcreator.api.versionmeta.minecraft.MinecraftClient
import java.net.URL
import java.util.*

interface NeoForgeInstance {
    val minecraftVersion: String
    val neoForgeVersion: String
    val installerUrl: URL

    /**
     * Get this Forge instances corresponding Minecraft client instance, wrapped in an
     * [Optional]
     *
     * @return Client wrapped in an [Optional].
     * @author Griefed
     */
    fun minecraftClient(): Optional<MinecraftClient>
}