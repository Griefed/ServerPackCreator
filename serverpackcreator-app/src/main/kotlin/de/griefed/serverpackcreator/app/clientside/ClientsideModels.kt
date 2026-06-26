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

import de.griefed.serverpackcreator.app.clientside.Sideness.UNKNOWN


/**
 * Platform-declared support-level of a mod for a given side (client or server). Mirrors Modrinth's
 * `client_side`/`server_side` values; CurseForge exposes no such field and therefore always reports
 * [UNKNOWN].
 *
 * @author Griefed
 */
enum class Sideness {
    REQUIRED,
    OPTIONAL,
    UNSUPPORTED,
    UNKNOWN;

    companion object {
        /**
         * Parse a platform-provided sideness-string into a [Sideness], defaulting to [UNKNOWN] for
         * anything unrecognized or absent so callers never have to null-check.
         */
        fun fromString(value: String?): Sideness = when (value?.lowercase()) {
            "required" -> REQUIRED
            "optional" -> OPTIONAL
            "unsupported" -> UNSUPPORTED
            else -> UNKNOWN
        }
    }
}

/**
 * A single downloadable mod-file as listed by a hosting platform, normalized across Modrinth and
 * CurseForge. [downloadUrl] is `null` when the author forbade third-party distribution
 * (CurseForge `allowModDistribution=false`); such [locked] files can only be fetched through the
 * project's website download-flow (see the browser-downloader in the boot phase).
 *
 * @param fileName             The exact file-name as published, case preserved (the clientside-list
 *                             matches file-names with `startsWith`, so case matters).
 * @param loaders              Canonical SPC loader-names this file targets (Forge, NeoForge, Fabric,
 *                             Quilt, LegacyFabric).
 * @param minecraftVersions    Minecraft versions this file targets.
 * @param downloadUrl          Direct download-URL, or `null` when distribution is locked.
 * @param pageUrl              The project/file webpage, used by the browser-downloader for locked files.
 * @param requiredDependencies Platform-native references (ids/slugs) of mods required by this file.
 * @author Griefed
 */
data class ModFile(
    val fileName: String,
    val loaders: Set<String>,
    val minecraftVersions: Set<String>,
    val downloadUrl: String?,
    val pageUrl: String?,
    val requiredDependencies: List<String>
) {
    /** Whether this file cannot be downloaded via the API and needs the browser download-flow. */
    val locked: Boolean
        get() = downloadUrl == null
}

/**
 * The resolved view of a hosting-platform project: its declared sideness (Modrinth only) and the
 * complete list of its [files] across every loader and Minecraft version.
 *
 * @param platform    Human-readable platform name ("Modrinth" or "CurseForge").
 * @param slug        The project-slug taken from the issue-link.
 * @param projectUrl  The original project-link from the issue.
 * @param clientSide  Platform-declared client support; [Sideness.UNKNOWN] for CurseForge.
 * @param serverSide  Platform-declared server support; [Sideness.UNKNOWN] for CurseForge.
 * @param files       Every published file of the project.
 * @author Griefed
 */
data class ProjectFiles(
    val platform: String,
    val slug: String,
    val projectUrl: String,
    val clientSide: Sideness,
    val serverSide: Sideness,
    val files: List<ModFile>
) {
    /** All distinct canonical loader-names this project ships for. */
    val loaders: Set<String>
        get() = files.flatMap { it.loaders }.toSortedSet()

    /** Every distinct file-name of the project. */
    val fileNames: List<String>
        get() = files.map { it.fileName }.distinct()

    /** The file-names of this project that target the given canonical [loader]. */
    fun fileNamesForLoader(loader: String): List<String> =
        files.filter { loader in it.loaders }.map { it.fileName }.distinct()
}
