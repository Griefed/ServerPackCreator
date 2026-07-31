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
import java.util.Collections

/**
 * Pins the order the pool grinds a batch in: **round-robin across platforms**, each platform in its own
 * most-downloaded-first order.
 *
 * The old behaviour sorted the whole batch by `popularity`, which starved a platform outright. Observed live on
 * 2026-07-30: CurseForge download counts run several times Modrinth's for equivalent mods (`jei` 602 M vs
 * `fabric-api` 218 M), so *every* CurseForge candidate outranked *every* Modrinth one and a ~2-hour pass ground
 * 108 CurseForge projects and **zero** Modrinth ones. Any interruption shorter than a full pass meant Modrinth
 * made no progress at all, indefinitely.
 *
 * The deeper reason a global sort was wrong: the two counts are not comparable. CurseForge counts file downloads
 * across every version, Modrinth counts differently — ranking them against each other silently promoted one
 * platform for the whole run. Interleaving keeps the meaningful comparison (within a platform) and drops the
 * meaningless one (between platforms).
 */
internal class GrindPoolOrderTest {

    private fun candidate(platform: String, slug: String, popularity: Long) =
        GrindCandidate("https://example.invalid/$slug", slug, popularity, platform)

    /** Records grind order; one worker keeps it deterministic. */
    private fun processedOrder(candidates: List<GrindCandidate>, workers: Int = 1): List<String> {
        val processed = Collections.synchronizedList(mutableListOf<String>())
        val verifier = CandidateVerifier { c ->
            processed.add("${c.platform}:${c.slug}")
            clientsideReport(c.slug, listOf(loaderVerdict("Forge", "${c.slug}-", Confidence.MEDIUM)), platform = c.platform)
        }
        GrindPool(Grinder(verifier, InMemoryVerdictStore()), workerCount = workers).grindAll(candidates)
        return processed
    }

    @Test
    fun alternatesBetweenPlatformsSoNeitherIsStarved() {
        val order = processedOrder(
            listOf(
                candidate("CurseForge", "cf-big", 600_000_000),
                candidate("CurseForge", "cf-mid", 400_000_000),
                candidate("CurseForge", "cf-small", 200_000_000),
                candidate("Modrinth", "mr-big", 200_000_000),
                candidate("Modrinth", "mr-mid", 100_000_000),
                candidate("Modrinth", "mr-small", 50_000_000)
            )
        )

        Assertions.assertEquals(
            listOf(
                "CurseForge:cf-big", "Modrinth:mr-big",
                "CurseForge:cf-mid", "Modrinth:mr-mid",
                "CurseForge:cf-small", "Modrinth:mr-small"
            ),
            order,
            "one from each platform in turn, each platform most-downloaded first"
        )
    }

    /** Within a platform the popularity ranking is unchanged — that comparison is the meaningful one. */
    @Test
    fun keepsEachPlatformInItsOwnPopularityOrder() {
        val order = processedOrder(
            listOf(
                candidate("Modrinth", "low", 10),
                candidate("Modrinth", "high", 30),
                candidate("Modrinth", "mid", 20)
            )
        )

        Assertions.assertEquals(listOf("Modrinth:high", "Modrinth:mid", "Modrinth:low"), order)
    }

    @Test
    fun aPlatformThatRunsOutEarlyDoesNotStallTheOthers() {
        val order = processedOrder(
            listOf(
                candidate("CurseForge", "cf1", 900),
                candidate("CurseForge", "cf2", 800),
                candidate("CurseForge", "cf3", 700),
                candidate("Modrinth", "mr1", 500)
            )
        )

        Assertions.assertEquals(
            listOf("CurseForge:cf1", "Modrinth:mr1", "CurseForge:cf2", "CurseForge:cf3"), order,
            "the shorter platform is exhausted, the longer one simply continues"
        )
    }

    /**
     * The property this change exists for: a pass cut short must still have reached **both** platforms. With a
     * global popularity sort this failed outright — the whole prefix belonged to one platform.
     */
    @Test
    fun anInterruptedPassHasReachedBothPlatforms() {
        val processed = Collections.synchronizedList(mutableListOf<String>())
        lateinit var pool: GrindPool
        val verifier = CandidateVerifier { c ->
            processed.add(c.platform)
            if (processed.size >= 2) {
                pool.requestStop() // cut the pass short, as a shutdown would
            }
            clientsideReport(c.slug, listOf(loaderVerdict("Forge", "${c.slug}-", Confidence.MEDIUM)), platform = c.platform)
        }
        pool = GrindPool(Grinder(verifier, InMemoryVerdictStore()), workerCount = 1)
        val candidates = (1..20).map { candidate("CurseForge", "cf$it", 1_000_000_000L - it) } +
            (1..20).map { candidate("Modrinth", "mr$it", 1_000L - it) }

        val pass = pool.grindAll(candidates)

        Assertions.assertEquals(
            setOf("CurseForge", "Modrinth"), pass.reached.map { it.platform }.toSet(),
            "both platforms must have progressed before the pass was abandoned; got $processed"
        )
    }
}
