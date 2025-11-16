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
package de.griefed.serverpackcreator.api.versionmeta.forge

import de.griefed.serverpackcreator.api.versionmeta.minecraft.MinecraftMeta
import java.net.URI
import java.net.URL
import java.util.*

/**
 * An instance of a complete Forge combination, containing a Minecraft version, related Forge
 * version and the URL to the server installer.
 *
 * @param minecraftVersion Minecraft version.
 * @param forgeVersion     Forge version.
 * @param minecraftMeta    The corresponding Minecraft client for this Forge version.
 * @throws [java.net.MalformedURLException] if the URL to the download of the Forge server installer could not be created.
 *
 * @author Griefed
 */
@Suppress("unused")
class ForgeInstance(
    val minecraftVersion: String,
    val forgeVersion: String,
    private val minecraftMeta: MinecraftMeta
) {
    val installerUrl: URL =
        URI("https://files.minecraftforge.net/maven/net/minecraftforge/forge/$minecraftVersion-$forgeVersion/forge-$minecraftVersion-$forgeVersion-installer.jar").toURL() // TODO Move URL to property

    /**
     * Get this Forge instances corresponding Minecraft client instance, wrapped in an
     * [Optional]
     *
     * @return Client wrapped in an [Optional].
     * @author Griefed
     */
    fun minecraftClient() = minecraftMeta.getClient(minecraftVersion)
}