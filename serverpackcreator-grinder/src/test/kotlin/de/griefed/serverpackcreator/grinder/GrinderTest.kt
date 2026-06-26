/* Copyright (C) 2025 Griefed
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
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger

/**
 * Pins the grind orchestration with a fake [CandidateVerifier] (no containers): one verdict recorded
 * per loader, already-ground projects skipped, a thrown verification swallowed (not propagated), and
 * the pool draining every candidate across workers most-popular-first.
 */
internal class GrinderTest {

    private fun candidate(slug: String, popularity: Long = 1) =
        GrindCandidate("https://modrinth.com/mod/$slug", slug, popularity)

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
    fun skipsProjectsThatAlreadyHaveAVerdict() {
        val store = InMemoryVerdictStore()
        store.record(grindVerdict("jei", "Forge"))
        val calls = AtomicInteger(0)
        val verifier = CandidateVerifier { c -> calls.incrementAndGet(); clientsideReport(c.slug, listOf(loaderVerdict("Forge", "jei-", Confidence.HIGH))) }

        Grinder(verifier, store).grind(candidate("jei"))

        Assertions.assertEquals(0, calls.get(), "an already-ground project must not be re-verified")
        Assertions.assertEquals(1, store.all().size)
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

    @Test
    fun poolRejectsANonPositiveWorkerCount() {
        val grinder = Grinder({ clientsideReport("x", emptyList()) }, InMemoryVerdictStore())
        assertThrows<IllegalArgumentException> { GrindPool(grinder, workerCount = 0) }
    }
}
