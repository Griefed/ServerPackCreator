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

import de.griefed.serverpackcreator.grinder.GrindCandidate

/**
 * A hosting platform the grinder can enumerate for mods to verify. Implementations paginate their
 * platform's catalog **most-downloaded first**, so a grind that never gets past the first slice still
 * covers the mods most likely to land in a modpack.
 *
 * Paging is **offset-based rather than "top N"**: a continuous grind must walk the *whole* catalog over
 * successive passes, not re-check the same head forever, so the caller keeps the offset (see
 * [CatalogCrawler] and [CursorStore]) and asks for the next slice each time.
 *
 * @author Griefed
 */
interface CandidateSource {
    /**
     * The platform these candidates come from — one of [de.griefed.serverpackcreator.grinder.ModPlatforms].
     * Doubles as the source's identity in the [CursorStore], so it must be stable across restarts.
     */
    val platform: String

    /**
     * One slice of the catalog: up to [limit] projects starting at [offset], most-downloaded first.
     *
     * Implementations may need several API calls to fill one slice (platform page caps are smaller than a
     * useful batch) and must distinguish the two ways a slice can come back short — see
     * [CandidatePage.endOfCatalog].
     */
    fun page(offset: Int, limit: Int): CandidatePage
}

/**
 * One slice of a platform's catalog plus where to continue. [endOfCatalog] is the distinction that makes
 * unattended crawling correct: a slice can come back short because the catalog *ended* (the crawler should
 * wrap around and start a new sweep) or because a request *failed* (the crawler must stay put and retry the
 * same region later). Conflating the two would silently reset a deep crawl to the top of the catalog on any
 * transient HTTP error.
 *
 * @param candidates   The projects in this slice, most-downloaded first.
 * @param nextOffset   Offset the next slice should start at — `offset + candidates.size`, i.e. only what was
 *                     actually handed out is treated as consumed.
 * @param endOfCatalog `true` only when the platform genuinely ran out of results (or the reachable catalog
 *                     ended, as with CurseForge's search-index cap); never on a failed request.
 * @author Griefed
 */
data class CandidatePage(
    val candidates: List<GrindCandidate>,
    val nextOffset: Int,
    val endOfCatalog: Boolean
)
