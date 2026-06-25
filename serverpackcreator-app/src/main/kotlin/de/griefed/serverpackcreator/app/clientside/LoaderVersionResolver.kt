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
package de.griefed.serverpackcreator.app.clientside

import de.griefed.serverpackcreator.api.versionmeta.VersionMeta

/**
 * Picks the newest known loader-version to boot a server with, for a given loader and Minecraft
 * version, from SPC's [VersionMeta]. Forge/NeoForge are Minecraft-version-specific; Fabric/Quilt/
 * LegacyFabric loaders are Minecraft-independent so their latest is used regardless.
 *
 * @param versionMeta SPC's cached version manifests (from [de.griefed.serverpackcreator.api.ApiWrapper]).
 * @author Griefed
 */
class LoaderVersionResolver(private val versionMeta: VersionMeta) {

    /**
     * Newest loader-version for the [loader]/[minecraftVersion] pair, or `null` when none is known
     * (e.g. the Minecraft version is unsupported by that loader).
     */
    fun latest(loader: String, minecraftVersion: String): String? = when (loader) {
        "Forge" -> versionMeta.forge.newestForgeVersion(minecraftVersion).orElse(null)
        "NeoForge" -> versionMeta.neoForge.newestNeoForgeVersion(minecraftVersion).orElse(null)
        "Fabric" -> versionMeta.fabric.latestLoader()
        "Quilt" -> versionMeta.quilt.latestLoader()
        "LegacyFabric" -> versionMeta.legacyFabric.latestLoader()
        else -> null
    }
}
