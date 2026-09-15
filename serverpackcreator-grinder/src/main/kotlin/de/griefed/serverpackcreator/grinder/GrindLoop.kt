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

import de.griefed.serverpackcreator.grinder.source.CatalogCrawler
import de.griefed.serverpackcreator.grinder.source.RequeueStore
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.time.Duration

/**
 * The continuous sweep: drain the re-grind queue, take the next catalog slice, grind it, commit the crawl
 * position, evict stale loader installs, then pace before going round again.
 *
 * **Extracted from `main` because it could not otherwise be tested at all.** It is the daemon's central
 * behaviour — the ordering of requeue-before-catalog, committing only what was actually reached, and
 * pausing on the pacing rules — and every one of those decisions lived inside a 300-line function that
 * needs a Docker daemon to run. The collaborators it takes are the ones it genuinely uses; passing them
 * explicitly is what lets a test drive a pass with fakes.
 *
 * @param grinder     Grinds one candidate.
 * @param crawler     Hands out the next catalog slice and records how far it got.
 * @param requeue     The jump-the-crawl lane, drained *before* the slice on every pass.
 * @param evictUnusedInstalls Sweeps loader installs unused past [cacheRetention], returning how many went.
 *        A function rather than the `LoaderCache` itself: the loop uses exactly this one call, and taking
 *        the whole class would drag a Docker-bound installer into every test of the sweep.
 * @param verdictCount How many verdicts the store holds, for the completion line. Likewise narrowed.
 * @param status      Live activity for `/status`.
 * @param workers     Concurrent grinds per pass.
 * @param cacheRetention How long an unused loader install is kept.
 * @param betweenSweeps Pause after a pass that completed a sweep with nothing due.
 * @param whileCrawling Pause after a pass that found nothing due but has catalog left.
 * @param running     Whether to begin another pass; polled between steps so a shutdown lands promptly.
 * @param onPoolChanged Publishes the in-flight pool so the shutdown hook can stop it, and `null` when idle.
 * @param sleeper     How a pause is served. Injected so a test need not actually wait.
 * @author Griefed
 */
internal class GrindLoop(
    private val grinder: Grinder,
    private val crawler: CatalogCrawler,
    private val requeue: RequeueStore,
    private val evictUnusedInstalls: (Duration) -> Int,
    private val verdictCount: () -> Int,
    private val status: GrinderStatus,
    private val workers: Int,
    private val cacheRetention: Duration,
    private val betweenSweeps: Duration,
    private val whileCrawling: Duration,
    private val running: () -> Boolean,
    private val onPoolChanged: (GrindPool?) -> Unit = {},
    private val sleeper: (Long) -> Unit = SLEEP
) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

    private companion object {
        /** The real pause. A named default so the constructor can reference it without a self-reference. */
        val SLEEP: (Long) -> Unit = { Thread.sleep(it) }
    }

    /** Run passes until [running] says otherwise, returning how many completed. */
    fun run(): Int {
        var pass = 0
        while (running()) {
            pass++
            // The immediate lane first, and forced: these are projects somebody decided are wrong, and a
            // wrong verdict is usually a recent one, so an unforced drain would skip every one as fresh.
            // Ground before the catalog slice so a re-grind lands in minutes rather than at the next TTL.
            val requeued = requeue.drain()
            val batch = crawler.nextBatch()
            // Announced before either pool runs, and counting both: /status answers "what is it doing right
            // now?", and a drain of hundreds used to leave it showing the *previous* pass for the duration.
            status.beginPass(pass, requeued.size + batch.candidates.size)
            if (requeued.isNotEmpty()) {
                log.info("Pass #$pass: re-grinding ${requeued.size} requested candidate(s) ahead of the crawl...")
                GrindPool(grinder, workers).also { onPoolChanged(it) }.grindAll(requeued, force = true)
            }
            // LANDMINE: this check is what keeps a *second* pool per pass safe. The shutdown hook holds one
            // handle and reads it once (deliberately -- reading twice could signal one pool and wait on
            // another), so a stop that landed in the drain above has already been signalled, awaited and
            // reported complete. Falling through would start a whole new pool of boots behind it, creating
            // containers after the hook finished and while TimeoutStopSec counts down.
            if (!running()) {
                break
            }
            log.info("Pass #$pass: grinding ${batch.candidates.size} candidate(s)...")
            val pool = GrindPool(grinder, workers).also { onPoolChanged(it) }
            // Named for what it is, and deliberately not `pass`: that shadowed the pass *counter*, so every
            // "Pass #$pass" below it printed this data class instead of the number.
            val catalogPass = pool.grindAll(batch.candidates)
            val verified = catalogPass.verified
            onPoolChanged(null)
            // Advance the crawl only past what was actually ground. An interrupted pass re-hands the rest next
            // time instead of skipping those projects until the next full sweep, weeks or months away.
            crawler.commit(batch, catalogPass.reached)
            log.info("Pass #$pass complete: $verified verified, ${verdictCount()} verdict(s) total.")
            // Bound the loader cache by time. Each tuple costs ~150 MB and loaders keep shipping builds, so an
            // unattended sweep would grow it without limit; a tuple still being booted is stamped as used on
            // every cache hit, so only genuinely idle ones go.
            val evicted = evictUnusedInstalls(cacheRetention)
            if (evicted > 0) {
                log.info("Evicted $evicted loader install(s) unused for over ${cacheRetention.toDays()}d.")
            }
            if (!running()) {
                break
            }
            // Wait only when there is nothing to get on with — a fixed sleep per pass would cap how fast the
            // catalog can be swept, which is the difference between covering it in weeks and never.
            val pause = GrindPacing.pauseAfterPass(verified, batch.sweepCompleted, betweenSweeps, whileCrawling)
            if (pause.isZero) {
                continue
            }
            // Served out in slices rather than one sleep, so a re-grind queued into a dozing daemon starts in
            // seconds instead of waiting out a six-hour inter-sweep pause. See GrindPacing.pollInterval.
            val wakeAt = System.currentTimeMillis() + pause.toMillis()
            try {
                while (running() && System.currentTimeMillis() < wakeAt) {
                    if (requeue.pending() > 0) {
                        log.info("Work was queued for re-grinding; starting the next pass now.")
                        break
                    }
                    val remaining = Duration.ofMillis(wakeAt - System.currentTimeMillis())
                    sleeper(GrindPacing.pollInterval(remaining).toMillis())
                }
            } catch (_: InterruptedException) {
                break // shutdown requested during the inter-pass wait
            }
        }
        log.info("Grinder stopped after $pass pass(es).")
        return pass
    }
}
