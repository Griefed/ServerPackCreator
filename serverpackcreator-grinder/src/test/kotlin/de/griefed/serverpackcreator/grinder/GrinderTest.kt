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
import java.util.concurrent.atomic.AtomicInteger

/**
 * Pins the grind orchestration with a fake [CandidateVerifier] (no containers): one verdict recorded
 * per loader, projects with a *fresh* verdict skipped while *stale* ones are re-verified, a thrown
 * verification swallowed (not propagated), and the pool draining every candidate across workers
 * most-popular-first.
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
        Grinder(verifier, store).grind(candidate("jei"))

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

        Grinder(verifier, store, reverifyTtl = Duration.ofDays(30), clock = { now }).grind(candidate("jei"))

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
        Grinder(verifier, store).grind(candidate("doomed"))

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

        GrindPool(Grinder(verifier, store), workerCount = 4).grindAll(candidates)

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

        pool.grindAll((1..20).map { candidate("mod$it", it.toLong()) })

        Assertions.assertEquals(1, processed.size, "only the in-flight candidate completes; the queue is dropped")
    }

    @Test
    fun poolRejectsANonPositiveWorkerCount() {
        val grinder = Grinder({ clientsideReport("x", emptyList()) }, InMemoryVerdictStore())
        assertThrows<IllegalArgumentException> { GrindPool(grinder, workerCount = 0) }
    }
}
