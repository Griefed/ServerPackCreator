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
package de.griefed.serverpackcreator.api.config

/**
 * The single source of truth for the modloaders supported by ServerPackCreator: their
 * exact-match regexes for case-insensitive loader-detection and their canonical names.
 * Consolidates the regexes previously duplicated across PackConfig, ConfigurationHandler,
 * ModloaderValidator and ModpackManifestParser (refactor Phase 1e).
 */
object SupportedModloaders {
    /**
     * Exact-match regex for Forge, applied to lowercased input.
     */
    val forge = "^forge$".toRegex()

    /**
     * Exact-match regex for NeoForge, applied to lowercased input.
     */
    val neoForge = "^neoforge$".toRegex()

    /**
     * Exact-match regex for Fabric, applied to lowercased input.
     */
    val fabric = "^fabric$".toRegex()

    /**
     * Exact-match regex for Quilt, applied to lowercased input.
     */
    val quilt = "^quilt$".toRegex()

    /**
     * Exact-match regex for LegacyFabric, applied to lowercased input.
     */
    val legacyFabric = "^legacyfabric$".toRegex()

    /**
     * Canonical names of all supported modloaders.
     */
    val names = arrayOf("Fabric", "Forge", "Quilt", "LegacyFabric", "NeoForge")
}
