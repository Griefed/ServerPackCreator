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
package de.griefed.serverpackcreator.grinder.source

/**
 * One bounded slice of the CurseForge catalog: a `/mods/search` query narrowed by game version and modloader,
 * walked in one sort direction. Partitions exist because CurseForge refuses `index + pageSize > 10 000`, so
 * the catalog cannot be enumerated as one sequence — it is covered as many slices that each fit under that
 * cap (see [CurseForgePartitions] for the traversal).
 *
 * @param gameVersion    `gameVersion` filter, or `null` for the unfiltered catalog.
 * @param modLoaderType  `modLoaderType` filter (see [CurseForgePartitions.FORGE] etc.), or `null` for all.
 * @param ascending      `true` to sort least-downloaded first, reaching the *bottom* of a slice that is too
 *                       big to page through from the top.
 * @author Griefed
 */
data class CurseForgePartition(
    val gameVersion: String?,
    val modLoaderType: Int?,
    val ascending: Boolean
) {
    /**
     * The stable token persisted in the crawl cursor, `version|loader|direction` with `*` for "no filter"
     * (e.g. `*|*|desc`, `1.20.1|1|asc`). Deliberately readable: an operator inspecting `cursors.json` can see
     * which part of the catalog the crawl is in.
     */
    val key: String
        get() = "${gameVersion ?: NONE}|${modLoaderType ?: NONE}|${if (ascending) ASCENDING else DESCENDING}"

    companion object {
        private const val NONE = "*"
        private const val ASCENDING = "asc"
        private const val DESCENDING = "desc"

        /**
         * Read a [key] back, falling back to [CurseForgePartitions.FIRST] for anything unreadable — a cursor
         * file written by an older build, hand-edited, or truncated must restart the sweep, never crash the
         * daemon or leave it crawling nothing.
         */
        fun parse(key: String): CurseForgePartition {
            val parts = key.split('|')
            if (parts.size != 3) {
                return CurseForgePartitions.FIRST
            }
            val (rawVersion, rawLoader, rawDirection) = parts
            val ascending = when (rawDirection) {
                ASCENDING -> true
                DESCENDING -> false
                else -> return CurseForgePartitions.FIRST
            }
            val loader = if (rawLoader == NONE) null else rawLoader.toIntOrNull() ?: return CurseForgePartitions.FIRST
            return CurseForgePartition(
                gameVersion = rawVersion.takeUnless { it == NONE || it.isBlank() },
                modLoaderType = loader,
                ascending = ascending
            )
        }
    }
}

/**
 * The CurseForge crawl plan: which slice to walk next so that, over one sweep, as much of the catalog as the
 * API can reach is covered.
 *
 * The shape of the plan follows from two documented facts. `/mods/search` rejects `index + pageSize > 10 000`,
 * so any single query exposes at most 10 000 mods; and every response carries `pagination.totalCount`, so a
 * slice's true size is known for free as soon as it is queried. [next] therefore splits **adaptively**:
 *
 * 1. the unfiltered catalog, most-downloaded first — the top 10 000 mods on CurseForge, which is what the
 *    grinder cares about most and what it crawled before partitioning existed;
 * 2. then each game version, newest first. A version whose `totalCount` fits under the cap is done in one
 *    slice;
 * 3. a version over the cap is re-crawled once per modloader, which is what actually reaches past 10 000;
 * 4. a *loader* slice still over the cap is crawled from the bottom too (`sortOrder=asc`), covering up to
 *    20 000 mods in that slice.
 *
 * Splitting only where a count demands it keeps a sweep to roughly one request per version rather than one per
 * version *and* loader. **Residual gap, deliberately accepted:** a single (version, loader) slice holding more
 * than 20 000 mods loses its middle, and a mod that carries no modloader tag is only reachable while its
 * version fits under the cap. [CurseForgeCandidateSource] logs both cases rather than letting a sweep look
 * complete; adding a third axis (`categoryId`) is the remedy if they ever show up in the wild.
 *
 * @author Griefed
 */
object CurseForgePartitions {
    /**
     * CurseForge's `ModLoaderType` values. The REST docs render the enum numerically without names; the
     * mapping is corroborated against PrismLauncher's Flame integration (`case ModPlatform::Forge: return 1;
     * … Cauldron: 2, LiteLoader: 3, Fabric: 4, Quilt: 5, NeoForge: 6`), the same source that corroborates
     * [CurseForgeCandidateSource.SORT_FIELD_TOTAL_DOWNLOADS]. `0` means "any" and is expressed as `null` here.
     */
    const val FORGE = 1
    const val CAULDRON = 2
    const val LITELOADER = 3
    const val FABRIC = 4
    const val QUILT = 5
    const val NEOFORGE = 6

    /**
     * Every loader a version is split across, in crawl order. **All six are included on purpose:** the legacy
     * ones (Cauldron, LiteLoader) cost one request each on the old versions where they exist, while leaving
     * one out would make its mods unreachable — the opposite trade.
     */
    val LOADERS = listOf(FORGE, CAULDRON, LITELOADER, FABRIC, QUILT, NEOFORGE)

    /** The slice every sweep opens with: the whole catalog, most-downloaded first. */
    val FIRST = CurseForgePartition(gameVersion = null, modLoaderType = null, ascending = false)

    /**
     * The slice to crawl after [current], given that slice's [totalCount] and the platform's [versions]
     * (newest first, as ordered by [orderVersions]). `null` means the plan is finished — the crawler treats
     * that as the end of the catalog, wraps, and starts a fresh sweep.
     */
    fun next(current: CurseForgePartition, totalCount: Int, versions: List<String>): CurseForgePartition? {
        val cap = CurseForgeCandidateSource.MAX_INDEX
        // The unfiltered slice is always far bigger than the cap; splitting it by loader would still be
        // capped, so the per-version slices are what reach deeper.
        if (current.gameVersion == null) {
            return versions.firstOrNull()?.let { CurseForgePartition(it, null, ascending = false) }
        }
        if (current.modLoaderType == null) {
            return if (totalCount > cap) {
                CurseForgePartition(current.gameVersion, LOADERS.first(), ascending = false)
            } else {
                nextVersionAfter(current.gameVersion, versions)
            }
        }
        // A loader slice that overflows the cap is worth a second pass from the bottom; after that pass (or a
        // slice that fits) the plan moves on, so a slice bigger than two caps loses its middle.
        if (!current.ascending && totalCount > cap) {
            return current.copy(ascending = true)
        }
        val followingLoader = LOADERS.getOrNull(LOADERS.indexOf(current.modLoaderType) + 1)
        return if (followingLoader != null) {
            CurseForgePartition(current.gameVersion, followingLoader, ascending = false)
        } else {
            nextVersionAfter(current.gameVersion, versions)
        }
    }

    /**
     * The version slice following [version], or `null` at the end of the list. A version that is no longer in
     * [versions] (the platform dropped it, or the cursor is older than the list) falls forward to the newest
     * one instead of dead-ending the crawl.
     */
    private fun nextVersionAfter(version: String, versions: List<String>): CurseForgePartition? {
        val position = versions.indexOf(version)
        val following = if (position < 0) versions.firstOrNull() else versions.getOrNull(position + 1)
        return following?.let { CurseForgePartition(it, null, ascending = false) }
    }

    /**
     * Order raw platform version strings newest-first, comparing dotted components **numerically** so
     * `1.21.11` sorts above `1.21.2`. Strings that are not dotted numbers (CurseForge mixes in labels) keep a
     * stable position at the end rather than being dropped — an odd entry costs one wasted request, whereas
     * dropping a real version would cost coverage. Blank entries are dropped.
     */
    fun orderVersions(rawVersions: List<String>): List<String> =
        rawVersions.filter { it.isNotBlank() }
            .sortedWith(
                compareBy<String> { if (numericComponentsOf(it) == null) 1 else 0 }
                    .thenComparator { left, right -> compareNumeric(numericComponentsOf(left), numericComponentsOf(right)) }
                    .thenBy { it }
            )

    /** Dotted numeric components of [version] (`1.21.11` → `[1, 21, 11]`), or `null` when it is not one. */
    private fun numericComponentsOf(version: String): List<Int>? {
        val parts = version.trim().split('.')
        val numbers = parts.map { it.toIntOrNull() ?: return null }
        return numbers.takeIf { it.isNotEmpty() }
    }

    /** Compare component lists descending (newest first); `null` (unparsable) sorts as equal to `null`. */
    private fun compareNumeric(left: List<Int>?, right: List<Int>?): Int {
        if (left == null || right == null) {
            return 0
        }
        for (position in 0 until maxOf(left.size, right.size)) {
            val comparison = (right.getOrElse(position) { 0 }).compareTo(left.getOrElse(position) { 0 })
            if (comparison != 0) {
                return comparison
            }
        }
        return 0
    }
}
