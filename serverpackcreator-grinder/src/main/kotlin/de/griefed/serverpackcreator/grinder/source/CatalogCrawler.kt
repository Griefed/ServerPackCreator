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
 * **Two-phase by design:** [nextBatch] hands out candidates without moving anything, and [commit] advances each
 * source only past the pages whose candidates were actually ground. An interrupted pass therefore re-hands what
 * it never reached instead of silently skipping it.
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

    /**
     * The next slice of every source's catalog. **Does not move any cursor** — the position advances only in
     * [commit], once the caller reports which candidates were actually reached. Calling this twice without a
     * commit in between therefore hands out the same slice twice.
     */
    fun nextBatch(): CandidateBatch {
        val candidates = ArrayList<GrindCandidate>(batchSize * sources.size.coerceAtLeast(1))
        val pages = ArrayList<CrawledPage>(sources.size + 1)
        var sweepCompleted = false
        for (source in sources) {
            val cursor = cursors.cursor(source.platform)
            // The partition token is the source's own business — carried through verbatim, never interpreted.
            val page = pageOrNull(source, cursor.offset, cursor.partition) ?: continue // failed: position kept
            candidates.addAll(page.candidates)
            pages.add(CrawledPage(source.platform, cursor, page.candidates, page.endOfCatalog, page.nextOffset, page.nextPartition))
            if (!page.endOfCatalog) {
                continue
            }
            sweepCompleted = true
            wrapAround(source, cursor, page)?.let { head ->
                candidates.addAll(head.candidates)
                pages.add(head)
            }
        }
        return CandidateBatch(candidates, sweepCompleted, pages)
    }

    /**
     * Move each source's cursor past the pages of [batch] whose candidates were **all** reached, where "reached"
     * means the grind loop actually got to them (verified, skipped as fresh, or attempted and failed — all three
     * are done with; only never-processed candidates are not).
     *
     * A page is committed whole or not at all, and the first page that was not fully reached stops that source:
     * its cursor stays where that page began, so every candidate in it is handed out again next pass. That is the
     * property the design turns on — **a pass abandoned half-way (shutdown, `requestStop`) must not advance the
     * crawl past projects nothing ever ground**, or they would not be revisited until the next full sweep, weeks
     * or months later.
     *
     * Whole-page granularity is deliberate rather than per-candidate: a partitioned source can cross partitions
     * *inside* one page, so a candidate's exact catalog position is not recoverable from the outside. Re-handing
     * a whole page costs almost nothing, because everything already ground in it now has a fresh verdict and is
     * skipped in microseconds.
     */
    fun commit(batch: CandidateBatch, reached: Set<GrindCandidate>) {
        for ((platform, pages) in batch.pages.groupBy { it.platform }) {
            for (page in pages) {
                if (!reached.containsAll(page.candidates)) {
                    // Not fully ground: rewind to where this page started and re-hand it next pass.
                    cursors.store(platform, page.cursorAtStart)
                    val missed = page.candidates.count { it !in reached }
                    log.info(
                        "$platform: $missed of ${page.candidates.size} candidate(s) were not reached — " +
                            "holding the cursor at offset ${page.cursorAtStart.offset}" +
                            (page.cursorAtStart.partition?.let { " of partition $it" } ?: "") + " so they are not skipped."
                    )
                    break
                }
                cursors.store(platform, page.committedCursor())
            }
        }
    }

    /**
     * Start [source]'s next sweep at the top of its catalog, returning the head page when one was fetched. When
     * the ended slice came back *empty* — the position already sat past the end — the head slice is fetched right
     * away so the pass still has work to do; that retry happens at most once per pass (and not at all when the
     * position was already 0), so an empty catalog cannot spin. Nothing is stored here: the wrap is expressed as
     * a page whose `cursorAtStart` is already the new sweep's start, so [commit] applies it only if the page was
     * fully ground.
     */
    private fun wrapAround(source: CandidateSource, cursor: CatalogCursor, ended: CandidatePage): CrawledPage? {
        // Both offset and partition reset: a new sweep starts at the beginning of the source's plan.
        val wrapped = CatalogCursor(offset = 0, sweeps = cursor.sweeps + 1, partition = null)
        log.info(
            "${source.platform}: reached the end of the catalog at offset ${cursor.offset}" +
                (cursor.partition?.let { " of partition $it" } ?: "") +
                " — sweep #${wrapped.sweeps} complete, starting the next one at the top."
        )
        if (ended.candidates.isEmpty() && (cursor.offset > 0 || cursor.partition != null)) {
            val head = pageOrNull(source, offset = 0, partition = null)
            if (head != null) {
                return CrawledPage(source.platform, wrapped, head.candidates, head.endOfCatalog, head.nextOffset, head.nextPartition)
            }
        }
        return null
    }

    /** One slice from [source], or `null` when the source failed outright (logged, never propagated). */
    private fun pageOrNull(source: CandidateSource, offset: Int, partition: String?): CandidatePage? =
        runCatching { source.page(offset, batchSize, partition) }
            .onFailure { log.warn("Could not read ${source.platform} at offset $offset (partition $partition): ${it.message}") }
            .getOrNull()
}

/**
 * One page handed out by one source in a batch, carrying everything [CatalogCrawler.commit] needs to either
 * advance past it or re-hand it: where the source stood *before* the page, the candidates it produced, and where
 * the source said to continue. Internal because it is crawl bookkeeping, not part of the grind vocabulary.
 *
 * @author Griefed
 */
data class CrawledPage(
    /** Which source produced this page, so the cursor is committed against the right platform. */
    val platform: String,
    /** The cursor this page was fetched at — kept so a failed pass can be retried from where it began. */
    val cursorAtStart: CatalogCursor,
    /** The candidates on this page, already mapped out of the platform's response. */
    val candidates: List<GrindCandidate>,
    /** Whether this page ran off the end of the catalogue, which is what completes a sweep and rewinds the offset. */
    val endedCatalog: Boolean,
    /** Where the next page starts. Watch it against the source's offset ceiling — past it, the tail looks like the end. */
    val nextOffset: Int,
    /** The next partition token for a partitioned crawl (CurseForge), or `null` for a flat one (Modrinth). */
    val nextPartition: String?
) {
    /** Where the cursor belongs once every candidate in this page has been ground. */
    fun committedCursor(): CatalogCursor =
        if (endedCatalog) CatalogCursor(offset = 0, sweeps = cursorAtStart.sweeps + 1, partition = null)
        else cursorAtStart.copy(offset = nextOffset, partition = nextPartition)
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
    /** Everything this pass will grind, across all sources, ranked so the most-downloaded go first. */
    val candidates: List<GrindCandidate>,
    /** Whether a source finished its catalogue this pass, which is what `GrindPacing` treats as "nothing left due". */
    val sweepCompleted: Boolean,
    /** The pages this batch came from, so cursors can be committed per source only after the work is done. */
    val pages: List<CrawledPage> = emptyList()
)
