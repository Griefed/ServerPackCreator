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

import de.griefed.serverpackcreator.clientside.Verdict
import de.griefed.serverpackcreator.grinder.report.InMemoryVerdictStore
import de.griefed.serverpackcreator.grinder.source.CandidatePage
import de.griefed.serverpackcreator.grinder.source.CandidateSource
import de.griefed.serverpackcreator.grinder.source.CatalogCrawler
import de.griefed.serverpackcreator.grinder.source.InMemoryCursorStore
import de.griefed.serverpackcreator.grinder.source.RequeueStore
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Pins the daemon's central behaviour, which **had no test at all** until it was lifted out of `main`.
 *
 * The ordering of requeue-before-catalog, committing only what was actually reached, and stopping promptly
 * between steps are the decisions a long-running grind depends on — and every one of them lived inside a
 * 300-line function that needs a Docker daemon to run, so the only guard available was grepping its source
 * text for the presence of a call. These drive the real loop with fakes instead.
 */
internal class GrindLoopTest {

    private fun candidate(slug: String) =
        GrindCandidate("https://modrinth.com/mod/$slug", slug, 1, ModPlatforms.MODRINTH)

    /** A source handing out one page then nothing, which is a completed sweep. */
    private class OneShotSource(private val candidates: List<GrindCandidate>) : CandidateSource {
        override val platform = ModPlatforms.MODRINTH
        override fun page(offset: Int, limit: Int, partition: String?): CandidatePage =
            if (offset == 0) {
                CandidatePage(candidates, nextOffset = candidates.size, endOfCatalog = true)
            } else {
                CandidatePage(emptyList(), nextOffset = 0, endOfCatalog = true)
            }
    }

    /** A queue that hands its contents over exactly once. */
    private class FakeRequeue(private var queued: MutableList<GrindCandidate>) : RequeueStore {
        override fun add(candidates: Collection<GrindCandidate>): Int {
            queued.addAll(candidates); return candidates.size
        }

        override fun drain(): List<GrindCandidate> = queued.toList().also { queued.clear() }
        override fun pending(): Int = queued.size
    }

    /**
     * A loop that stops the way the daemon does: on the *pause* between passes.
     *
     * `running` is polled between steps on purpose — that is what makes a stop land promptly rather than
     * after another boot budget — so a counter-based fake would stop the loop mid-pass and test nothing.
     * Flipping the flag from the injected sleeper exercises the real flow: a pass runs to completion, the
     * pacing decides to pause, and the stop arrives there.
     */
    private fun loop(
        ground: MutableList<String>,
        requeue: RequeueStore,
        candidates: List<GrindCandidate>,
        running: AtomicBoolean = AtomicBoolean(true),
        status: GrinderStatus = GrinderStatus(),
        evicted: Int = 0
    ): GrindLoop {
        val verifier = CandidateVerifier { candidate ->
            ground.add(candidate.slug)
            clientsideReport(candidate.slug, listOf(loaderVerdict("Forge", "${candidate.slug}-", verdict = Verdict.ERROR)))
        }
        return GrindLoop(
            grinder = Grinder(verifier, InMemoryVerdictStore()),
            crawler = CatalogCrawler(listOf(OneShotSource(candidates)), InMemoryCursorStore(), batchSize = 10),
            requeue = requeue,
            evictUnusedInstalls = { evicted },
            verdictCount = { 0 },
            status = status,
            workers = 1,
            cacheRetention = Duration.ofDays(7),
            betweenSweeps = Duration.ofSeconds(1),
            whileCrawling = Duration.ofSeconds(1),
            running = running::get,
            sleeper = { running.set(false) }
        )
    }

    /**
     * **The re-grind lane is drained before the catalog slice**, which is the whole point of it: a project
     * is queued because its published verdict is known to be wrong, and waiting for the crawl to come round
     * would leave that verdict being served for a full sweep.
     */
    @Test
    fun theRequeueLaneIsGroundBeforeTheCatalogSlice() {
        val ground = mutableListOf<String>()

        loop(ground, FakeRequeue(mutableListOf(candidate("urgent"))), listOf(candidate("crawled"))).run()

        Assertions.assertEquals(listOf("urgent", "crawled"), ground, "the queued project must be ground first")
    }

    /**
     * A stop arriving *during* the drain must not start the catalog pass. Without this the daemon spends a
     * whole further pass — up to a boot budget per candidate — after being asked to stop, and systemd's
     * `TimeoutStopSec` lands on it mid-boot.
     */
    @Test
    fun theCatalogPassIsNotStartedWhenAStopArrivedDuringTheDrain() {
        val ground = mutableListOf<String>()
        val running = AtomicBoolean(true)
        val requeue = object : RequeueStore {
            override fun add(candidates: Collection<GrindCandidate>) = 0
            override fun drain(): List<GrindCandidate> {
                running.set(false)
                return emptyList()
            }

            override fun pending() = 0
        }

        loop(ground, requeue, listOf(candidate("crawled")), running = running).run()

        Assertions.assertTrue(ground.isEmpty(), "a stop during the drain must not start the catalog pass")
    }

    /**
     * The live pass count covers **both** lanes, or `/status` under-reports what a pass is doing while an
     * operator is watching a re-grind land.
     *
     * Sampled from inside the grind rather than after the loop: the last pass is the empty one that
     * triggers the stop, so a snapshot taken at the end would report zero and prove nothing.
     */
    @Test
    fun theLivePassCountsTheRequeuedCandidatesToo() {
        val status = GrinderStatus()
        val duringFirstGrind = mutableListOf<Int>()
        val ground = mutableListOf<String>()
        val verifier = CandidateVerifier { candidate ->
            duringFirstGrind.add(status.snapshot().passCandidates)
            ground.add(candidate.slug)
            clientsideReport(candidate.slug, listOf(loaderVerdict("Forge", "${candidate.slug}-", verdict = Verdict.ERROR)))
        }
        val running = AtomicBoolean(true)

        GrindLoop(
            grinder = Grinder(verifier, InMemoryVerdictStore()),
            crawler = CatalogCrawler(
                listOf(OneShotSource(listOf(candidate("a"), candidate("b")))), InMemoryCursorStore(), batchSize = 10
            ),
            requeue = FakeRequeue(mutableListOf(candidate("urgent"))),
            evictUnusedInstalls = { 0 },
            verdictCount = { 0 },
            status = status,
            workers = 1,
            cacheRetention = Duration.ofDays(7),
            betweenSweeps = Duration.ofSeconds(1),
            whileCrawling = Duration.ofSeconds(1),
            running = running::get,
            sleeper = { running.set(false) }
        ).run()

        Assertions.assertEquals(listOf("urgent", "a", "b"), ground)
        Assertions.assertTrue(
            duringFirstGrind.all { it == 3 },
            "every grind of the first pass should see 1 requeued + 2 crawled; saw $duringFirstGrind"
        )
    }

    /** Passes are counted and reported, which is what the daemon's final log line states. */
    /**
     * The loop runs until told to stop and reports how many passes it completed — the number the daemon's
     * final log line states. Two here: the first grinds the catalog slice, the second finds nothing due and
     * pauses, which is where the stop arrives.
     */
    @Test
    fun everyCompletedPassIsCounted() {
        val completed = loop(mutableListOf(), FakeRequeue(mutableListOf()), listOf(candidate("a"))).run()

        Assertions.assertEquals(2, completed)
    }
}
