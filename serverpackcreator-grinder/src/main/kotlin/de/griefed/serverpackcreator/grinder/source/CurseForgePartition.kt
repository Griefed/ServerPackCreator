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
 * One bounded slice of the CurseForge catalog: a `/mods/search` query narrowed by game version, category and
 * modloader, walked in one sort direction. Partitions exist because CurseForge refuses
 * `index + pageSize > 10 000`, so the catalog cannot be enumerated as one sequence — it is covered as many
 * slices that each fit under that cap (see [CurseForgePartitions] for the traversal).
 *
 * @param gameVersion    `gameVersion` filter, or `null` for the unfiltered catalog.
 * @param categoryId     `categoryId` filter, or `null` for all categories.
 * @param modLoaderType  `modLoaderType` filter (see [CurseForgePartitions.FORGE] etc.), or `null` for all.
 * @param ascending      `true` to sort least-downloaded first, reaching the *bottom* of a slice that is too
 *                       big to page through from the top.
 * @author Griefed
 */
data class CurseForgePartition(
    val gameVersion: String?,
    val categoryId: Int?,
    val modLoaderType: Int?,
    val ascending: Boolean
) {
    /**
     * The stable token persisted in the crawl cursor, `version|category|loader|direction` with `*` for "no
     * filter" (e.g. `*|*|*|desc`, `1.20.1|426|1|asc`). Deliberately readable: an operator inspecting
     * `cursors.json` can see which part of the catalog the crawl is in.
     */
    val key: String
        get() = listOf(
            gameVersion ?: NONE,
            categoryId?.toString() ?: NONE,
            modLoaderType?.toString() ?: NONE,
            if (ascending) ASCENDING else DESCENDING
        ).joinToString("|")

    companion object {
        private const val NONE = "*"
        private const val ASCENDING = "asc"
        private const val DESCENDING = "desc"
        private const val FIELDS = 4

        /**
         * Read a [key] back, falling back to [CurseForgePartitions.FIRST] for anything unreadable — a cursor
         * file written by an older build (the token gained a field when the category axis was added), hand-edited
         * or truncated must restart the sweep, never crash the daemon or leave it crawling nothing. One extra
         * sweep costs nothing, since fresh verdicts are skipped.
         */
        fun parse(key: String): CurseForgePartition {
            val parts = key.split('|')
            if (parts.size != FIELDS) {
                return CurseForgePartitions.FIRST
            }
            val (rawVersion, rawCategory, rawLoader, rawDirection) = parts
            val ascending = when (rawDirection) {
                ASCENDING -> true
                DESCENDING -> false
                else -> return CurseForgePartitions.FIRST
            }
            val category = optionalNumber(rawCategory) ?: return CurseForgePartitions.FIRST
            val loader = optionalNumber(rawLoader) ?: return CurseForgePartitions.FIRST
            return CurseForgePartition(
                gameVersion = rawVersion.takeUnless { it == NONE || it.isBlank() },
                categoryId = category.value,
                modLoaderType = loader.value,
                ascending = ascending
            )
        }

        /**
         * Read one optional numeric field: `*` means "no filter" (a present box holding `null`), a number means
         * that filter, and anything else means the token is unreadable (`null`). Boxing keeps "absent" and
         * "unreadable" apart, which a bare `Int?` cannot express.
         */
        private fun optionalNumber(raw: String): OptionalFilter? = when {
            raw == NONE -> OptionalFilter(null)
            else -> raw.toIntOrNull()?.let { OptionalFilter(it) }
        }

        /** A parsed optional filter — see [optionalNumber]. */
        private data class OptionalFilter(val value: Int?)
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
 * 3. a version over the cap is re-crawled once per **modloader**, then once per **category** — see below;
 * 4. any slice that is still over the cap is crawled from the bottom too (`sortOrder=asc`), covering up to
 *    20 000 mods in it, and a *category* slice past even that is narrowed by modloader as a last resort.
 *
 * **Why an over-cap version is crawled along two axes rather than one.** Neither tag is guaranteed: a mod
 * carries a modloader only if it has one, and CurseForge's own submission docs disagree on whether a category
 * is mandatory (the submission guide calls a main category required, the project-creation page lists only the
 * class). Crawling both axes means a mod is reachable if it has *either* — deliberately paying ~6 extra
 * requests per over-cap version for the loader stage that the category stage would mostly duplicate, because
 * the alternative is a silent hole. Splitting only where a reported count demands it keeps the cost to the
 * handful of versions that actually need it.
 *
 * **Residual gap, deliberately accepted:** a mod with *neither* a loader nor a category is unreachable beyond
 * its version's top 10 000, and a single (version, category, modloader) slice above 20 000 mods loses its
 * middle. [CurseForgeCandidateSource] logs the latter with a count; the former cannot be observed from
 * outside, and no further axis in the API would fix it.
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
    val FIRST = CurseForgePartition(gameVersion = null, categoryId = null, modLoaderType = null, ascending = false)

    /** Shorthand for the platform's paging cap, the bound every partition has to fit under. */
    private val CAP = CurseForgeCandidateSource.MAX_INDEX

    /**
     * The slice to crawl after [current], given that slice's [totalCount], the platform's [versions] (newest
     * first, as ordered by [orderVersions]) and its mod [categories]. `null` means the plan is finished — the
     * crawler treats that as the end of the catalog, wraps, and starts a fresh sweep.
     *
     * Every transition advances a loader, a category or a version, or flips the sort direction once, so the
     * walk always terminates.
     */
    fun next(
        current: CurseForgePartition,
        totalCount: Int,
        versions: List<String>,
        categories: List<Int>
    ): CurseForgePartition? {
        val version = current.gameVersion
            // The unfiltered slice is always far bigger than the cap; splitting it by loader or category would
            // still be capped, so the per-version slices are what reach deeper.
            ?: return versions.firstOrNull()?.let { versionSlice(it) }

        // A slice too big to page through from the top is worth a second pass from the bottom.
        if (!current.ascending && totalCount > CAP && current != versionSlice(version)) {
            return current.copy(ascending = true)
        }
        return when {
            // The version itself: under the cap it is done; over it, the loader stage opens the split.
            current.categoryId == null && current.modLoaderType == null ->
                if (totalCount > CAP) loaderSlice(version, LOADERS.first()) else versionSliceAfter(version, versions)

            // Loader stage of a version: walk the loaders, then hand over to the category stage.
            current.categoryId == null ->
                loaderAfter(current.modLoaderType!!)?.let { loaderSlice(version, it) }
                    ?: categories.firstOrNull()?.let { categorySlice(version, it) }
                    ?: versionSliceAfter(version, versions)

            // Category stage: a category past *both* sort directions is narrowed by loader as a last resort.
            current.modLoaderType == null ->
                if (totalCount > 2 * CAP) categoryLoaderSlice(version, current.categoryId, LOADERS.first())
                else categorySliceAfter(version, current.categoryId, categories, versions)

            // Deepest slices (category × loader): walk the loaders, then on to the next category.
            else ->
                loaderAfter(current.modLoaderType)?.let { categoryLoaderSlice(version, current.categoryId, it) }
                    ?: categorySliceAfter(version, current.categoryId, categories, versions)
        }
    }

    /** The whole of one game version, most-downloaded first. */
    private fun versionSlice(version: String) =
        CurseForgePartition(version, categoryId = null, modLoaderType = null, ascending = false)

    /** One modloader's mods for a game version, most-downloaded first. */
    private fun loaderSlice(version: String, loader: Int) =
        CurseForgePartition(version, categoryId = null, modLoaderType = loader, ascending = false)

    /** One category's mods for a game version, most-downloaded first. */
    private fun categorySlice(version: String, category: Int) =
        CurseForgePartition(version, categoryId = category, modLoaderType = null, ascending = false)

    /** The deepest narrowing: one category *and* one modloader of a game version. */
    private fun categoryLoaderSlice(version: String, category: Int, loader: Int) =
        CurseForgePartition(version, categoryId = category, modLoaderType = loader, ascending = false)

    /** The loader following [loader] in crawl order, or `null` after the last one. */
    private fun loaderAfter(loader: Int): Int? = LOADERS.getOrNull(LOADERS.indexOf(loader) + 1)

    /**
     * The category slice following [category] for [version], falling through to the next version once its
     * categories are done. A category that is no longer in [categories] (the platform changed them, or the
     * cursor predates the change) falls forward to the first one instead of dead-ending the crawl.
     */
    private fun categorySliceAfter(
        version: String,
        category: Int,
        categories: List<Int>,
        versions: List<String>
    ): CurseForgePartition? {
        val position = categories.indexOf(category)
        val following = if (position < 0) categories.firstOrNull() else categories.getOrNull(position + 1)
        return following?.let { categorySlice(version, it) } ?: versionSliceAfter(version, versions)
    }

    /**
     * The version slice following [version], or `null` at the end of the list. A version that is no longer in
     * [versions] falls forward to the newest one instead of dead-ending the crawl.
     */
    private fun versionSliceAfter(version: String, versions: List<String>): CurseForgePartition? {
        val position = versions.indexOf(version)
        val following = if (position < 0) versions.firstOrNull() else versions.getOrNull(position + 1)
        return following?.let { versionSlice(it) }
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
