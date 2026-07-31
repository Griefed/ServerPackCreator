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
package de.griefed.serverpackcreator.grinder.report

import de.griefed.serverpackcreator.clientside.Confidence
import de.griefed.serverpackcreator.grinder.ModPlatforms.CURSEFORGE
import de.griefed.serverpackcreator.grinder.ModPlatforms.MODRINTH
import de.griefed.serverpackcreator.grinder.grindVerdict
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Pins the in-memory verdict store: a re-verified `(platform, slug, loader)` replaces rather than
 * duplicates, distinct loaders of one project coexist, **the same slug on two platforms stays two
 * projects**, and [VerdictStore.newestVerification] returns the freshest timestamp across a project's loaders —
 * both scoped to the platform. **[VerdictStore.hasVerdictFor] does *not* drive production's skip check** (an
 * earlier version of this comment claimed it did): `Grinder.grind` compares `newestVerification` against the
 * re-verify TTL, because "seen at all" and "seen recently enough" are different questions. `hasVerdictFor` is kept
 * as the readable predicate for tests and for callers that only need the former.
 */
internal class VerdictStoreTest {

    @Test
    fun reVerifyingSameProjectAndLoaderReplacesTheVerdict() {
        val store = InMemoryVerdictStore()
        store.record(grindVerdict("jei", "Forge", confidence = Confidence.MEDIUM))
        store.record(grindVerdict("jei", "Forge", confidence = Confidence.HIGH))

        Assertions.assertEquals(1, store.all().size)
        Assertions.assertEquals(Confidence.HIGH, store.all().single().confidence)
    }

    @Test
    fun distinctLoadersOfOneProjectCoexist() {
        val store = InMemoryVerdictStore()
        store.record(grindVerdict("sodium", "Fabric"))
        store.record(grindVerdict("sodium", "Quilt"))

        Assertions.assertEquals(2, store.all().size)
        Assertions.assertTrue(store.hasVerdictFor(MODRINTH, "sodium"))
    }

    @Test
    fun hasVerdictForIsFalseUntilSomethingIsRecorded() {
        val store = InMemoryVerdictStore()
        Assertions.assertFalse(store.hasVerdictFor(MODRINTH, "rubidium"))
        store.record(grindVerdict("rubidium", "Forge"))
        Assertions.assertTrue(store.hasVerdictFor(MODRINTH, "rubidium"))
        Assertions.assertFalse(store.hasVerdictFor(MODRINTH, "something-else"))
    }

    /**
     * The dedup identity includes the platform: slugs are not globally unique (`jei` ships on Modrinth
     * *and* CurseForge), so the same slug+loader on two platforms must be two rows. Before this, one
     * platform's verdict overwrote the other's and suppressed grinding it at all.
     */
    @Test
    fun theSameSlugOnTwoPlatformsIsTwoProjects() {
        val store = InMemoryVerdictStore()
        store.record(grindVerdict("jei", "Forge", platform = MODRINTH, confidence = Confidence.LOW))
        store.record(grindVerdict("jei", "Forge", platform = CURSEFORGE, confidence = Confidence.HIGH))

        Assertions.assertEquals(2, store.all().size, "one row per platform, not one overwriting the other")
        Assertions.assertEquals(Confidence.LOW, store.all().single { it.platform == MODRINTH }.confidence)
        Assertions.assertEquals(Confidence.HIGH, store.all().single { it.platform == CURSEFORGE }.confidence)
    }

    /** A verdict on one platform must not make the *other* platform's project look already-ground. */
    @Test
    fun freshnessAndPresenceAreScopedToThePlatform() {
        val store = InMemoryVerdictStore()
        val recorded = Instant.parse("2026-06-01T00:00:00Z")
        store.record(grindVerdict("jei", "Forge", platform = MODRINTH, verifiedAt = recorded))

        Assertions.assertTrue(store.hasVerdictFor(MODRINTH, "jei"))
        Assertions.assertFalse(store.hasVerdictFor(CURSEFORGE, "jei"), "CurseForge's jei is still un-ground")
        Assertions.assertEquals(recorded, store.newestVerification(MODRINTH, "jei"))
        Assertions.assertNull(store.newestVerification(CURSEFORGE, "jei"), "must not inherit the other platform's timestamp")
    }

    @Test
    fun newestVerificationReturnsTheFreshestTimestampAcrossLoaders() {
        val store = InMemoryVerdictStore()
        val older = Instant.parse("2026-01-01T00:00:00Z")
        val newer = Instant.parse("2026-06-01T00:00:00Z")
        store.record(grindVerdict("sodium", "Fabric", verifiedAt = older))
        store.record(grindVerdict("sodium", "Quilt", verifiedAt = newer))

        Assertions.assertEquals(newer, store.newestVerification(MODRINTH, "sodium"))
        Assertions.assertNull(store.newestVerification(MODRINTH, "never-ground"))
    }
}
