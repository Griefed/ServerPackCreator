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

import de.griefed.serverpackcreator.clientside.Confidence
import de.griefed.serverpackcreator.grinder.report.InMemoryVerdictStore
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.time.Instant
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

/**
 * Pins the grind orchestration with a fake [CandidateVerifier] (no containers): one verdict recorded
 * per loader, projects with a *fresh* verdict skipped while *stale* ones are re-verified, a thrown
 * verification swallowed (not propagated), and the pool draining every candidate across workers
 * most-popular-first. Each grind also reports its [GrindOutcome], and the pool counts the verified ones —
 * the daemon paces itself on that count, so it is part of the contract, not a convenience.
 */
internal class GrinderTest {

    private fun candidate(slug: String, popularity: Long = 1) =
        GrindCandidate("https://modrinth.com/mod/$slug", slug, popularity, ModPlatforms.MODRINTH)

    @Test
    fun recordsOneVerdictPerLoaderFromTheReport() {
        val store = InMemoryVerdictStore()
        val verifier = CandidateVerifier { c ->
            clientsideReport(
                c.slug,
                listOf(
                    loaderVerdict("Forge", "${c.slug}-", Confidence.HIGH),
                    loaderVerdict("Fabric", "${c.slug}-fabric-", Confidence.MEDIUM)
                )
            )
        }
        val outcome = Grinder(verifier, store).grind(candidate("jei"))

        Assertions.assertEquals(GrindOutcome.VERIFIED, outcome)
        Assertions.assertEquals(2, store.all().size)
        Assertions.assertEquals(
            setOf("Forge" to Confidence.HIGH, "Fabric" to Confidence.MEDIUM),
            store.all().map { it.loader to it.confidence }.toSet()
        )
        Assertions.assertEquals("jei-", store.all().first { it.loader == "Forge" }.suggestedEntry)
    }

    @Test
    fun skipsProjectsWithAFreshVerdict() {
        val now = Instant.parse("2026-06-01T00:00:00Z")
        val store = InMemoryVerdictStore()
        store.record(grindVerdict("jei", "Forge", verifiedAt = now.minus(Duration.ofDays(5))))
        val calls = AtomicInteger(0)
        val verifier = CandidateVerifier { c -> calls.incrementAndGet(); clientsideReport(c.slug, listOf(loaderVerdict("Forge", "jei-", Confidence.HIGH))) }

        val outcome = Grinder(verifier, store, reverifyTtl = Duration.ofDays(30), clock = { now }).grind(candidate("jei"))

        Assertions.assertEquals(GrindOutcome.SKIPPED_FRESH, outcome)
        Assertions.assertEquals(0, calls.get(), "a verdict younger than the TTL must not be re-verified")
        Assertions.assertEquals(1, store.all().size)
    }

    /**
     * The freshness check is per-platform: a fresh Modrinth verdict for `jei` must not stop CurseForge's
     * `jei` — a different project that happens to share a slug — from being ground.
     */
    @Test
    fun aFreshVerdictOnOnePlatformDoesNotSkipTheSameSlugOnAnother() {
        val now = Instant.parse("2026-06-01T00:00:00Z")
        val store = InMemoryVerdictStore()
        store.record(grindVerdict("jei", "Forge", platform = ModPlatforms.MODRINTH, verifiedAt = now.minus(Duration.ofDays(1))))
        val ground = Collections.synchronizedList(mutableListOf<String>())
        val verifier = CandidateVerifier { c ->
            ground.add(c.platform)
            clientsideReport(c.slug, listOf(loaderVerdict("Forge", "jei-", Confidence.HIGH)), platform = c.platform)
        }
        val grinder = Grinder(verifier, store, reverifyTtl = Duration.ofDays(30), clock = { now })

        grinder.grind(GrindCandidate("https://modrinth.com/mod/jei", "jei", 1, ModPlatforms.MODRINTH))
        grinder.grind(GrindCandidate("https://www.curseforge.com/minecraft/mc-mods/jei", "jei", 1, ModPlatforms.CURSEFORGE))

        Assertions.assertEquals(listOf(ModPlatforms.CURSEFORGE), ground, "only the CurseForge project was due")
        Assertions.assertEquals(
            setOf(ModPlatforms.MODRINTH, ModPlatforms.CURSEFORGE),
            store.all().map { it.platform }.toSet(),
            "both platforms' jei coexist in the store"
        )
    }

    @Test
    fun reVerifiesAProjectWhoseVerdictIsStale() {
        val now = Instant.parse("2026-06-01T00:00:00Z")
        val store = InMemoryVerdictStore()
        store.record(grindVerdict("jei", "Forge", confidence = Confidence.LOW, verifiedAt = now.minus(Duration.ofDays(40))))
        val calls = AtomicInteger(0)
        val verifier = CandidateVerifier { c -> calls.incrementAndGet(); clientsideReport(c.slug, listOf(loaderVerdict("Forge", "jei-", Confidence.HIGH))) }

        Grinder(verifier, store, reverifyTtl = Duration.ofDays(30), clock = { now }).grind(candidate("jei"))

        Assertions.assertEquals(1, calls.get(), "a verdict older than the TTL must be re-verified")
        Assertions.assertEquals(Confidence.HIGH, store.all().single { it.loader == "Forge" }.confidence)
    }

    @Test
    fun swallowsAThrownVerificationWithoutRecordingOrPropagating() {
        val store = InMemoryVerdictStore()
        val verifier = CandidateVerifier { throw IllegalStateException("boot host exploded") }

        // Must not throw — a bad candidate cannot sink the worker.
        val outcome = Grinder(verifier, store).grind(candidate("doomed"))

        Assertions.assertEquals(GrindOutcome.FAILED, outcome, "a failure is not progress — the daemon throttles on it")
        Assertions.assertTrue(store.all().isEmpty())
    }

    @Test
    fun poolDrainsEveryCandidateAcrossWorkers() {
        val store = InMemoryVerdictStore()
        val verifier = CandidateVerifier { c ->
            Thread.sleep(5)
            clientsideReport(c.slug, listOf(loaderVerdict("Forge", "${c.slug}-", Confidence.HIGH)))
        }
        val candidates = (1..30).map { candidate("mod$it", it.toLong()) }

        val pass = GrindPool(Grinder(verifier, store), workerCount = 4).grindAll(candidates)

        Assertions.assertEquals(30, pass.verified, "every candidate was verified")
        Assertions.assertEquals(candidates.toSet(), pass.reached, "and every candidate was reached")
        Assertions.assertEquals(30, store.all().size)
        Assertions.assertEquals((1..30).map { "mod$it" }.toSet(), store.all().map { it.slug }.toSet())
    }

    @Test
    fun poolProcessesMostPopularFirst() {
        val processed = Collections.synchronizedList(mutableListOf<String>())
        val verifier = CandidateVerifier { c ->
            processed.add(c.slug)
            clientsideReport(c.slug, listOf(loaderVerdict("Forge", "${c.slug}-", Confidence.HIGH)))
        }
        val candidates = listOf(candidate("low", 10), candidate("high", 30), candidate("mid", 20))

        // Single worker => deterministic order, so the popularity ranking is observable.
        GrindPool(Grinder(verifier, InMemoryVerdictStore()), workerCount = 1).grindAll(candidates)

        Assertions.assertEquals(listOf("high", "mid", "low"), processed)
    }

    /**
     * A stop request abandons the rest of the batch instead of draining it, so the daemon's shutdown ends
     * the current pass promptly. The candidate in flight is *not* cancelled — it finishes — which is why
     * the in-flight container is torn down separately by closing the engine.
     */
    @Test
    fun requestStopAbandonsTheRestOfTheBatch() {
        val processed = Collections.synchronizedList(mutableListOf<String>())
        lateinit var pool: GrindPool
        val verifier = CandidateVerifier { c ->
            processed.add(c.slug)
            pool.requestStop() // ask to stop while the very first candidate is still being ground
            clientsideReport(c.slug, listOf(loaderVerdict("Forge", "${c.slug}-", Confidence.HIGH)))
        }
        pool = GrindPool(Grinder(verifier, InMemoryVerdictStore()), workerCount = 1)

        val pass = pool.grindAll((1..20).map { candidate("mod$it", it.toLong()) })

        Assertions.assertEquals(
            setOf(processed.single()), pass.reached.map { it.slug }.toSet(),
            "the abandoned batch reports exactly the one candidate it got to"
        )
        Assertions.assertEquals(1, processed.size, "only the in-flight candidate completes; the queue is dropped")
    }

    /**
     * The pacing input: only verified candidates count. A pass made up of fresh skips and failures reports
     * zero, which is what makes the daemon wait instead of racing its crawl position through the catalog.
     */
    @Test
    fun poolCountsOnlyTheCandidatesItActuallyVerified() {
        val now = Instant.parse("2026-06-01T00:00:00Z")
        val store = InMemoryVerdictStore()
        store.record(grindVerdict("fresh", "Forge", verifiedAt = now.minus(Duration.ofDays(1))))
        val verifier = CandidateVerifier { c ->
            if (c.slug == "doomed") throw IllegalStateException("boot host exploded")
            clientsideReport(c.slug, listOf(loaderVerdict("Forge", "${c.slug}-", Confidence.HIGH)))
        }
        val grinder = Grinder(verifier, store, reverifyTtl = Duration.ofDays(30), clock = { now })

        val candidates = listOf(candidate("fresh"), candidate("doomed"), candidate("due"), candidate("alsoDue"))
        val pass = GrindPool(grinder, workerCount = 2).grindAll(candidates)

        Assertions.assertEquals(2, pass.verified, "one fresh skip and one failure are not work")
        Assertions.assertEquals(
            candidates.toSet(), pass.reached,
            "a fresh skip and a failure are still *reached* — the crawl may advance past them, or a poison " +
                "candidate would stall the sweep forever"
        )
    }

    /**
     * The daemon's shutdown hook interrupts the main thread, which is normally parked in `grindAll` joining
     * its workers. An interrupted join must end the pass, **not** escape as an uncaught `InterruptedException`
     * (observed killing the process with a bare `Exception in thread "main"` on `SIGTERM` mid-pass). The
     * interrupt flag is handed back to the caller so the daemon loop still sees the shutdown.
     */
    @Test
    fun anInterruptedPassStopsInsteadOfThrowing() {
        val firstStarted = CountDownLatch(1)
        val verifier = CandidateVerifier { c ->
            firstStarted.countDown()
            Thread.sleep(50)
            clientsideReport(c.slug, listOf(loaderVerdict("Forge", "${c.slug}-", Confidence.HIGH)))
        }
        val pool = GrindPool(Grinder(verifier, InMemoryVerdictStore()), workerCount = 1)
        val mainThread = Thread.currentThread()
        Thread { firstStarted.await(); mainThread.interrupt() }.apply { isDaemon = true; start() }

        // Must return rather than throw, even though the caller is interrupted mid-join.
        val pass = pool.grindAll((1..40).map { candidate("mod$it", it.toLong()) })

        Assertions.assertTrue(Thread.interrupted(), "the caller's interrupt flag must be restored (and is cleared here)")
        Assertions.assertTrue(pass.verified < 40, "the pass was abandoned, not drained; verified=${pass.verified}")
        Assertions.assertTrue(
            pass.reached.size < 40,
            "an abandoned pass must report the candidates it never reached, so the crawl does not skip them"
        )
    }

    @Test
    fun poolRejectsANonPositiveWorkerCount() {
        val grinder = Grinder({ clientsideReport("x", emptyList()) }, InMemoryVerdictStore())
        assertThrows<IllegalArgumentException> { GrindPool(grinder, workerCount = 0) }
    }
}
