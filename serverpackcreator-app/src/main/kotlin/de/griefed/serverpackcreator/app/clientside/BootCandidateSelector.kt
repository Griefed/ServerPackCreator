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

/**
 * Pure file/version-selection logic for the boot-test, factored out of [BootVerifier] so it is
 * unit-testable without an [de.griefed.serverpackcreator.api.ApiWrapper] or a running server: which
 * file+Minecraft-version to boot for a loader, and which dependency-file to pull alongside it.
 *
 * @author Griefed
 */
object BootCandidateSelector {

    /** Numeric Minecraft-version ordering (so `1.20` sorts above `1.9`, unlike a string compare). */
    val minecraftComparator = Comparator<String> { left, right ->
        val leftParts = left.split('.').map { it.toIntOrNull() ?: 0 }
        val rightParts = right.split('.').map { it.toIntOrNull() ?: 0 }
        for (index in 0 until maxOf(leftParts.size, rightParts.size)) {
            val difference = leftParts.getOrElse(index) { 0 }.compareTo(rightParts.getOrElse(index) { 0 })
            if (difference != 0) return@Comparator difference
        }
        0
    }

    /**
     * Choose the newest (file, Minecraft-version) pair among [files] for [loader] for which
     * [loaderVersionAvailable] holds, so the boot uses a combination that can actually install.
     * Returns `null` when no such combination exists.
     */
    fun pickBootableCandidate(
        files: List<ModFile>,
        loader: String,
        loaderVersionAvailable: (minecraftVersion: String) -> Boolean
    ): Pair<ModFile, String>? =
        files.filter { loader in it.loaders }
            .flatMap { file -> file.minecraftVersions.map { file to it } }
            .sortedWith { left, right -> minecraftComparator.compare(right.second, left.second) }
            .firstOrNull { loaderVersionAvailable(it.second) }

    /**
     * Pick a dependency-file from [files] for the same [loader], preferring an exact
     * [minecraftVersion] match and falling back to any file for that loader.
     */
    fun pickDependencyFile(files: List<ModFile>, loader: String, minecraftVersion: String): ModFile? {
        val forLoader = files.filter { loader in it.loaders }
        return forLoader.firstOrNull { minecraftVersion in it.minecraftVersions } ?: forLoader.firstOrNull()
    }
}
