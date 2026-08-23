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
     * The sample the other-version crash re-check boots: the newest bootable file of each Minecraft version
     * *other than* [bootedMinecraftVersion], most recent Minecraft first, capped at [limit] — the same
     * [loaderVersionAvailable] gate as [pickBootableCandidate], since a version the loader cannot install can
     * never be staged either.
     *
     * One candidate per Minecraft version, never two builds of the same one: two rebuilds for one Minecraft
     * are near-identical code, so a different version line buys far more per boot spent. That relies on
     * [files] arriving newest-first, which both platforms do and the stable sort preserves, so the file kept
     * for a version is that version's latest.
     */
    fun pickRecheckCandidates(
        files: List<ModFile>,
        loader: String,
        bootedMinecraftVersion: String,
        limit: Int,
        loaderVersionAvailable: (minecraftVersion: String) -> Boolean
    ): List<Pair<ModFile, String>> {
        if (limit <= 0) {
            return emptyList()
        }
        return files.filter { loader in it.loaders }
            .flatMap { file -> file.minecraftVersions.map { file to it } }
            .filter { (_, minecraftVersion) ->
                minecraftVersion != bootedMinecraftVersion && loaderVersionAvailable(minecraftVersion)
            }
            .sortedWith { left, right -> minecraftComparator.compare(right.second, left.second) }
            .distinctBy { it.second }
            .take(limit)
    }

    /**
     * Pick a dependency-file from [files] for the same [loader], preferring an exact
     * [minecraftVersion] match and falling back to any file for that loader.
     */
    fun pickDependencyFile(files: List<ModFile>, loader: String, minecraftVersion: String): ModFile? =
        pickForLoader(files, loader, minecraftVersion)
            ?: fallbackLoaders[loader]?.let { pickForLoader(files, it, minecraftVersion) }

    /** Newest file carrying [loader], preferring one that also lists [minecraftVersion]. */
    private fun pickForLoader(files: List<ModFile>, loader: String, minecraftVersion: String): ModFile? {
        val forLoader = files.filter { loader in it.loaders }
        return forLoader.firstOrNull { minecraftVersion in it.minecraftVersions } ?: forLoader.firstOrNull()
    }

    /**
     * Loaders that can run another loader's mods, used **only** when a dependency publishes nothing for the loader
     * being booted. Quilt deliberately runs Fabric mods — which is precisely why the canonical dependency of a Quilt
     * mod is Fabric API, a project that ships only Fabric-tagged files. Without this, every such dependency was
     * silently dropped and the mod hard-failed with "requires fabric-api", wasting the whole boot: measured
     * 2026-07-30, 210 dropped dependencies, all but 44 of them on Quilt.
     *
     * Deliberately not symmetric and deliberately minimal: Fabric cannot load Quilt mods, and NeoForge only loads
     * Forge mods for a narrow band of Minecraft versions, so guessing there would stage a jar the loader cannot use.
     */
    private val fallbackLoaders = mapOf("Quilt" to "Fabric")
}
