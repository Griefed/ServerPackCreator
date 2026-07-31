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
package de.griefed.serverpackcreator.grinder

import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Live, in-memory record of what the daemon is doing right now, so an operator can answer "what is it working
 * on?" without reading the log. Served as JSON by `ReportServer` at `/status`.
 *
 * The verdict store answers what the grinder has *found*; this answers what it is *doing* — which pass, which
 * candidate each worker holds, how long it has held it, and how the current pass is going. Written from the
 * worker threads and read from the HTTP threads, so every field is concurrent and [snapshot] is a point-in-time
 * copy rather than a live view.
 *
 * @param clock Supplies "now" for uptimes and durations (injectable so tests need no sleeping).
 * @author Griefed
 */
class GrinderStatus(private val clock: () -> Instant = Instant::now) {
    private val startedAt = clock()

    @Volatile
    private var pass = PassState(number = 0, candidates = 0, startedAt = startedAt)

    /** Keyed by worker-thread name, so the map size is the pool size and entries are self-cleaning. */
    private val active = ConcurrentHashMap<String, ActiveCandidate>()

    private val verifiedTotal = AtomicInteger(0)
    private val failedTotal = AtomicInteger(0)
    private val skippedTotal = AtomicInteger(0)

    /** Note that a new pass has begun with [candidates] handed out. */
    fun beginPass(number: Int, candidates: Int) {
        pass = PassState(number, candidates, clock())
    }

    /** Note that the calling worker has started grinding [candidate]. */
    fun beginCandidate(candidate: GrindCandidate) {
        active[Thread.currentThread().name] = ActiveCandidate(
            platform = candidate.platform,
            slug = candidate.slug,
            projectUrl = candidate.projectUrl,
            startedAt = clock()
        )
    }

    /** Note that the calling worker has finished its candidate with [outcome], freeing that worker. */
    fun endCandidate(outcome: GrindOutcome) {
        active.remove(Thread.currentThread().name)
        when (outcome) {
            GrindOutcome.VERIFIED -> verifiedTotal
            GrindOutcome.FAILED -> failedTotal
            GrindOutcome.SKIPPED_FRESH -> skippedTotal
        }.incrementAndGet()
    }

    /** A point-in-time copy, safe to serialize while the workers keep going. */
    fun snapshot(): StatusSnapshot {
        val now = clock()
        val current = pass
        return StatusSnapshot(
            uptimeSeconds = Duration.between(startedAt, now).seconds,
            startedAt = startedAt.toString(),
            pass = current.number,
            passCandidates = current.candidates,
            passRunningSeconds = Duration.between(current.startedAt, now).seconds,
            verified = verifiedTotal.get(),
            failed = failedTotal.get(),
            skippedFresh = skippedTotal.get(),
            workers = active.entries
                .sortedBy { it.key }
                .map { (worker, candidate) ->
                    WorkerSnapshot(
                        worker = worker,
                        platform = candidate.platform,
                        slug = candidate.slug,
                        projectUrl = candidate.projectUrl,
                        busySeconds = Duration.between(candidate.startedAt, now).seconds
                    )
                }
        )
    }

    /** Which pass is running and since when. */
    private data class PassState(val number: Int, val candidates: Int, val startedAt: Instant)

    /** What one worker is holding. */
    private data class ActiveCandidate(
        val platform: String,
        val slug: String,
        val projectUrl: String,
        val startedAt: Instant
    )
}

/**
 * Serializable snapshot of [GrinderStatus] — the body of `/status`. A flat, boring shape on purpose: it is read
 * by humans in a browser and by whatever an operator points at it.
 *
 * @author Griefed
 */
data class StatusSnapshot(
    /** Seconds since the daemon started, so "is it alive?" is answerable without reading a log. */
    val uptimeSeconds: Long,
    /** When the daemon started, ISO-8601. A string rather than an `Instant` so the JSON needs no date module. */
    val startedAt: String,
    /** Which crawl pass is running, counting from 1. Rising while `verified` does not means work is failing. */
    val pass: Int,
    /** How many candidates this pass took from the crawler — the slice size, not the catalogue size. */
    val passCandidates: Int,
    /** Seconds the current pass has been running. Read with [passCandidates] to judge whether it is progressing. */
    val passRunningSeconds: Long,
    /** Candidates this pass carried to a verdict. The one number that means the sweep is doing its job. */
    val verified: Int,
    /**
     * Verifications that failed this pass. Deliberately *not* counted as work by `GrindPacing`: on a broken host
     * every candidate fails, and pacing on that would race the cursor through the catalogue verifying nothing.
     */
    val failed: Int,
    /** Candidates skipped because their verdict is still inside the re-verify TTL — expected, not a problem. */
    val skippedFresh: Int,
    /** The workers currently holding a candidate. Shorter than the pool means the rest are idle or the pass ended. */
    val workers: List<WorkerSnapshot>
)

/**
 * One busy worker: which candidate it holds and for how long. A worker between candidates is simply absent, so
 * `workers` shorter than the pool size means the rest are idle (or the pass is over).
 *
 * @author Griefed
 */
data class WorkerSnapshot(
    /** Thread name, e.g. `grind-worker-1`, matching the thread field in the log pattern so the two can be lined up. */
    val worker: String,
    /** Which platform the held candidate came from — `jei` exists on both, so this disambiguates it. */
    val platform: String,
    /** The held candidate's project slug. */
    val slug: String,
    /** Link to the held candidate, so an operator can look at what is being booted without decoding a pack path. */
    val projectUrl: String,
    /**
     * Seconds this worker has held its candidate. The number that exposes a hung boot: a value far above a normal
     * boot's duration means the container is stuck rather than the pass being slow.
     */
    val busySeconds: Long
)
