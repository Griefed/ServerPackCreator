/*
 * Copyright (C) 2024 Griefed.
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
package de.griefed.serverpackcreator.api.versionmeta

import de.griefed.serverpackcreator.api.utilities.URL
import de.griefed.serverpackcreator.api.versionmeta.fabric.FabricMeta
import de.griefed.serverpackcreator.api.versionmeta.forge.ForgeMeta
import de.griefed.serverpackcreator.api.versionmeta.legacyfabric.LegacyFabricMeta
import de.griefed.serverpackcreator.api.versionmeta.minecraft.MinecraftMeta
import de.griefed.serverpackcreator.api.versionmeta.neoforge.NeoForgeMeta
import de.griefed.serverpackcreator.api.versionmeta.quilt.QuiltMeta

/**
 * VersionMeta containing available versions and important details for Minecraft, Fabric and Forge.
 *
 * @author Griefed
 */
expect class VersionMeta {
    @Suppress("MemberVisibilityCanBePrivate")
    val legacyFabricUrlGame: URL

    @Suppress("MemberVisibilityCanBePrivate")
    val legacyFabricUrlLoader: URL

    @Suppress("MemberVisibilityCanBePrivate")
    val legacyfabricUrlManifest: URL

    @Suppress("MemberVisibilityCanBePrivate")
    val minecraftUrlManifest: URL

    @Suppress("MemberVisibilityCanBePrivate")
    val forgeUrlManifest: URL

    @Suppress("MemberVisibilityCanBePrivate")
    val neoForgeUrlManifest: URL

    @Suppress("MemberVisibilityCanBePrivate")
    val fabricUrlManifest: URL

    @Suppress("MemberVisibilityCanBePrivate")
    val fabricUrlIntermediariesManifest: URL

    @Suppress("MemberVisibilityCanBePrivate")
    val fabricUrlInstallerManifest: URL

    @Suppress("MemberVisibilityCanBePrivate")
    val quiltUrlManifest: URL

    @Suppress("MemberVisibilityCanBePrivate")
    val quiltUrlInstallerManifest: URL

    /**
     * The MinecraftMeta instance for working with Minecraft versions and information about them.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    val minecraft: MinecraftMeta

    /**
     * The QuiltMeta-instance for working with Fabric versions and information about them.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    val fabric: FabricMeta

    /**
     * The ForgeMeta-instance for working with Forge versions and information about them.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    val forge: ForgeMeta

    /**
     * The NeoForgeMeta-instance for working with NeoForge versions and information about them.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    val neoForge: NeoForgeMeta

    /**
     * The QuiltMeta-instance for working with Quilt versions and information about them.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    val quilt: QuiltMeta

    /**
     * The LegacyFabric-instance for working with Legacy Fabric versions and information about them.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    val legacyFabric: LegacyFabricMeta
}