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
package de.griefed.serverpackcreator.clientside

import de.griefed.serverpackcreator.api.modscanning.ModDependency
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Pins **which unmet dependencies may refuse a boot**, which is the decision that determines whether
 * reading jar-manifest dependencies improves the engine or wrecks it.
 *
 * Platform-declared refs and manifest-declared ids are not equally trustworthy. A platform ref is a
 * project the author explicitly linked; a manifest id is a bare string that may name something bundled
 * inside another jar (`fabric-api-base` ships inside Fabric API), something the loader itself provides, or
 * something optional in practice. `refuseForMissingDependencies` aborts a boot as INCONCLUSIVE, so
 * treating every unresolvable manifest id as a refusal would convert a large share of today's *working*
 * boots into INCONCLUSIVE — a strict regression wearing a feature's clothes.
 *
 * Hence the split, and it is what these guards are for:
 *  - **unsatisfied** — platform-declared misses, and manifest ids the registry *did* map and then failed to
 *    stage. Cases we chose to trust, so failing to honour them is a real gap. These refuse.
 *  - **unmapped** — manifest ids that map to no project at all. Recorded and reported, never fatal.
 */
internal class ManifestDependencyTest {

    private fun requirement(modId: String, constraint: String? = null) =
        ModDependency(modId, versionConstraint = constraint)

    /**
     * **The guard that keeps B from being a regression, and it is structural.** An unmapped manifest id
     * never even reaches [BootVerifier.refuseForMissingDependencies] — that is the whole point of keeping
     * the two collections separate rather than adding a flag. The engine booted without these ids before,
     * and `dependencyFailureMarkers` already catches the case where the loader genuinely rejects the mod.
     */
    @Test
    fun anUnmappedManifestDependencyDoesNotRefuseTheBoot() {
        Assertions.assertNull(
            BootVerifier.refuseForMissingDependencies(emptySet(), "Fabric", "1.20.1"),
            "with nothing in `unsatisfied` there is no refusal, whatever went unmapped"
        )
    }

    /** A dependency we *did* map and then failed to stage is a real gap, and still refuses as it always did. */
    @Test
    fun aMappedDependencyThatCouldNotBeStagedStillRefuses() {
        val refusal = BootVerifier.refuseForMissingDependencies(setOf("fabric-api"), "Fabric", "1.20.1")

        Assertions.assertNotNull(refusal)
        Assertions.assertTrue(refusal!!.detail.contains("fabric-api"), "the reason must name it: ${refusal.detail}")
    }

    /** Unmapped ids are still *reported*, so a coverage gap in the registry is visible rather than silent. */
    @Test
    fun unmappedDependenciesAreNamedEvenWhenTheyDoNotRefuse() {
        val note = BootVerifier.unmappedDependencyNote(setOf("cloth-config", "fabric-api-base"))

        Assertions.assertNotNull(note)
        Assertions.assertTrue(note!!.contains("cloth-config"))
        Assertions.assertTrue(note.contains("fabric-api-base"))
    }

    /** Nothing unmapped means nothing to say — an empty note would be noise on every clean boot. */
    @Test
    fun nothingUnmappedProducesNoNote() {
        Assertions.assertNull(BootVerifier.unmappedDependencyNote(emptySet()))
    }

    /**
     * The manifest's requirements and the platform's are unioned, and the loader's own ids are dropped:
     * `minecraft`, `java` and the loader are provided by the environment, never staged as mods.
     */
    @Test
    fun theEnvironmentsOwnIdsAreNeverTreatedAsDependencies() {
        val requirements = listOf(
            requirement("minecraft", "1.20.1"),
            requirement("java", ">=17"),
            requirement("fabricloader", ">=0.14"),
            requirement("forge"),
            requirement("neoforge"),
            requirement("quilt_loader"),
            requirement("fabric", ">=0.92.0")
        )

        Assertions.assertEquals(
            listOf("fabric"),
            BootVerifier.stageableRequirements(requirements).map { it.modID },
            "only real mods are staged; the environment provides the rest"
        )
    }

    /** A requirement already satisfied by a platform-declared ref must not be staged a second time. */
    @Test
    fun aRequirementAlreadyResolvedByThePlatformIsNotStagedTwice() {
        val requirements = listOf(requirement("fabric", ">=0.92.0"), requirement("cloth-config"))

        Assertions.assertEquals(
            listOf("cloth-config"),
            BootVerifier.stageableRequirements(requirements, alreadyResolved = setOf("fabric-api")) { id ->
                KnownModIds.refFor(id, "Modrinth")
            }.map { it.modID },
            "the platform already staged Fabric API; staging it again would be a wasted download"
        )
    }

    /** A pack cannot grow without bound: beyond the cap, refuse rather than boot a 40-jar pack. */
    @Test
    fun stagingRefusesBeyondTheInjectionCap() {
        val refusal = BootVerifier.refuseForTooManyDependencies(
            (1..BootVerifier.MAX_INJECTED_DEPENDENCIES + 1).map { "dep$it.jar" }, "Forge", "1.20.1"
        )

        Assertions.assertNotNull(refusal, "a pack this large tells you nothing about the candidate")
        Assertions.assertTrue(refusal!!.detail.contains("${BootVerifier.MAX_INJECTED_DEPENDENCIES}"))
    }

    /** At or below the cap, nothing is refused — the cap is a ceiling, not a target. */
    @Test
    fun stagingAtTheCapIsFine() {
        Assertions.assertNull(
            BootVerifier.refuseForTooManyDependencies(
                (1..BootVerifier.MAX_INJECTED_DEPENDENCIES).map { "dep$it.jar" }, "Forge", "1.20.1"
            )
        )
    }
}
