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
import org.apache.logging.log4j.kotlin.cachedLoggerOf

/**
 * Hands the grind loop the *next* slice of every platform's catalog and remembers where it got to, so an
 * unattended grinder walks a whole catalog over successive passes instead of re-checking the most-downloaded
 * mods forever. Each [nextBatch] resumes each source at its persisted [CatalogCursor], advances it by what
 * was actually handed out, and wraps around to the top once a source reports the end of its catalog — at
 * which point the verdict TTL takes over and the new sweep re-verifies only what has gone stale.
 *
 * A source whose request failed keeps its position (retried next pass) and one that throws is skipped
 * entirely, so neither a flaky platform nor a broken source can stall or sink the crawl.
 *
 * Not thread-safe by design: the daemon loop calls it once per pass, then fans the batch out to the workers.
 *
 * @param sources   The platforms to crawl, each identified by its [CandidateSource.platform].
 * @param cursors   Where crawl positions are kept (persistent in production).
 * @param batchSize How many projects to take from each source per pass — size it to what the host can
 *                  actually grind in one pass; a bigger batch sweeps the catalog faster.
 * @author Griefed
 */
class CatalogCrawler(
    private val sources: List<CandidateSource>,
    private val cursors: CursorStore,
    private val batchSize: Int
) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

    init {
        require(batchSize >= 1) { "batchSize must be at least 1, was $batchSize" }
    }

    /** The next slice of every source's catalog, with each source's position advanced accordingly. */
    fun nextBatch(): CandidateBatch {
        val candidates = ArrayList<GrindCandidate>(batchSize * sources.size.coerceAtLeast(1))
        var sweepCompleted = false
        for (source in sources) {
            val cursor = cursors.cursor(source.platform)
            val page = pageOrNull(source, cursor.offset) ?: continue // failed: position untouched, retried later
            candidates.addAll(page.candidates)
            if (!page.endOfCatalog) {
                cursors.store(source.platform, cursor.copy(offset = page.nextOffset))
                continue
            }
            sweepCompleted = true
            candidates.addAll(wrapAround(source, cursor, page))
        }
        return CandidateBatch(candidates, sweepCompleted)
    }

    /**
     * Start [source]'s next sweep at the top of its catalog and return whatever extra candidates that yields.
     * When the ended slice came back *empty* — the position already sat past the end — the head slice is
     * fetched right away so the pass still has work to do; that retry happens at most once per pass (and not
     * at all when the position was already 0), so an empty catalog cannot spin.
     */
    private fun wrapAround(source: CandidateSource, cursor: CatalogCursor, ended: CandidatePage): List<GrindCandidate> {
        val wrapped = CatalogCursor(offset = 0, sweeps = cursor.sweeps + 1)
        log.info(
            "${source.platform}: reached the end of the catalog at offset ${cursor.offset} — " +
                "sweep #${wrapped.sweeps} complete, starting the next one at the top."
        )
        if (ended.candidates.isEmpty() && cursor.offset > 0) {
            val head = pageOrNull(source, offset = 0)
            if (head != null) {
                cursors.store(source.platform, wrapped.copy(offset = head.nextOffset))
                return head.candidates
            }
        }
        cursors.store(source.platform, wrapped)
        return emptyList()
    }

    /** One slice from [source], or `null` when the source failed outright (logged, never propagated). */
    private fun pageOrNull(source: CandidateSource, offset: Int): CandidatePage? =
        runCatching { source.page(offset, batchSize) }
            .onFailure { log.warn("Could not read ${source.platform} at offset $offset: ${it.message}") }
            .getOrNull()
}

/**
 * One pass' worth of candidates plus whether any source finished a sweep of its catalog while producing it.
 * [sweepCompleted] is the daemon's "we have been all the way round" signal: combined with a pass that found
 * nothing left to verify, it means the reachable catalog is covered and current, so the loop can idle for a
 * long interval instead of paging ahead (see `GrindPacing`).
 *
 * @author Griefed
 */
data class CandidateBatch(
    val candidates: List<GrindCandidate>,
    val sweepCompleted: Boolean
)
