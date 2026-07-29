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

import java.util.concurrent.ConcurrentHashMap

/**
 * How far a crawl has walked into one platform's catalog: the [offset] the next slice starts at, plus how
 * many complete [sweeps] of that catalog are behind it. The sweep count is not used for control flow — it
 * exists so an operator reading the log or the cursor file can see whether the grinder is still on its first
 * pass over a platform or already re-checking, which is the difference between "incomplete" and "current".
 *
 * @author Griefed
 */
data class CatalogCursor(val offset: Int, val sweeps: Int) {
    init {
        require(offset >= 0) { "offset must be >= 0, was $offset" }
        require(sweeps >= 0) { "sweeps must be >= 0, was $sweeps" }
    }

    companion object {
        /** The start of a catalog — what an unseen source reports. */
        val START = CatalogCursor(offset = 0, sweeps = 0)
    }
}

/**
 * Remembers each [CandidateSource]'s crawl position between passes. The production implementation persists
 * ([JsonCursorStore]), because the position is what makes unattended coverage possible at all: without it
 * every restart — and every pass — begins at the most-downloaded mods and the catalog's tail is never
 * reached. Keyed by [CandidateSource.platform].
 *
 * @author Griefed
 */
interface CursorStore {
    /** The crawl position for [source], or [CatalogCursor.START] when that source has never been crawled. */
    fun cursor(source: String): CatalogCursor

    /** Persist [cursor] as [source]'s new crawl position, replacing any previous one. */
    fun store(source: String, cursor: CatalogCursor)
}

/**
 * In-memory [CursorStore] — the test double, and the fallback for a run that deliberately keeps no state.
 * Backed by a [ConcurrentHashMap]; a restart starts every catalog over from the top.
 *
 * @author Griefed
 */
class InMemoryCursorStore : CursorStore {
    private val cursors = ConcurrentHashMap<String, CatalogCursor>()

    override fun cursor(source: String): CatalogCursor = cursors[source] ?: CatalogCursor.START

    override fun store(source: String, cursor: CatalogCursor) {
        cursors[source] = cursor
    }
}
