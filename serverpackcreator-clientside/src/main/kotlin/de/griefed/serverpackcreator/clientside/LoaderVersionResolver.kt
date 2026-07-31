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
package de.griefed.serverpackcreator.clientside

import de.griefed.serverpackcreator.api.versionmeta.VersionMeta

/**
 * Picks the newest known loader-version to boot a server with, for a given loader and Minecraft
 * version, from SPC's [VersionMeta]. Forge/NeoForge are Minecraft-version-specific; Fabric/Quilt/
 * LegacyFabric loader versions are Minecraft-independent, but the loader still has to *support* the
 * Minecraft version (a brand-new Minecraft has no intermediary yet), so their latest is only returned
 * when [de.griefed.serverpackcreator.api.versionmeta.Meta.isMinecraftSupported] holds.
 *
 * @param versionMeta SPC's cached version manifests (from [de.griefed.serverpackcreator.api.ApiWrapper]).
 * @author Griefed
 */
interface LoaderVersionPolicy {
    /**
     * The loader-version to actually boot for this pair, or `null` when none is usable. A caller with a warm
     * install cache may deliberately return an *older* build here to avoid paying for a fresh install of a
     * version that behaves identically — [BootVerifier] guards that choice by re-checking any crash against
     * [latestVersion] before letting it stand.
     */
    fun preferredVersion(loader: String, minecraftVersion: String): String?

    /**
     * The authoritative newest loader-version, independent of any caching preference. Used for two things a
     * preference must never weaken: the support gate that decides whether the loader/Minecraft combination is
     * bootable at all, and the crash re-check.
     */
    fun latestVersion(loader: String, minecraftVersion: String): String?
}

/**
 * The default [LoaderVersionPolicy]: always boot the newest known build, so `preferredVersion` and
 * `latestVersion` are the same answer and [BootVerifier]'s crash re-check never has anything to re-check.
 */
class LoaderVersionResolver(private val versionMeta: VersionMeta) : LoaderVersionPolicy {

    override fun preferredVersion(loader: String, minecraftVersion: String): String? = latest(loader, minecraftVersion)

    override fun latestVersion(loader: String, minecraftVersion: String): String? = latest(loader, minecraftVersion)


    /**
     * Newest loader-version for the [loader]/[minecraftVersion] pair, or `null` when none is known —
     * i.e. the loader has no build for (Forge/NeoForge) or does not yet support (Fabric/Quilt/
     * LegacyFabric) that Minecraft version. A `null` here stops the combo being selected for a boot,
     * so a loader lacking support for a fresh Minecraft is skipped rather than spun up and aborted.
     */
    fun latest(loader: String, minecraftVersion: String): String? = when (loader) {
        "Forge" -> versionMeta.forge.newestForgeVersion(minecraftVersion).orElse(null)
        "NeoForge" -> versionMeta.neoForge.newestNeoForgeVersion(minecraftVersion).orElse(null)
        "Fabric" -> if (versionMeta.fabric.isMinecraftSupported(minecraftVersion)) versionMeta.fabric.latestLoader() else null
        "Quilt" -> if (versionMeta.quilt.isMinecraftSupported(minecraftVersion)) versionMeta.quilt.latestLoader() else null
        "LegacyFabric" -> if (versionMeta.legacyFabric.isMinecraftSupported(minecraftVersion)) versionMeta.legacyFabric.latestLoader() else null
        else -> null
    }
}
