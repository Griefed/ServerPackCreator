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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Pins the decision that answers *"do the jars in this pack satisfy each other?"* — pure, so the whole
 * behaviour is testable without a platform, a download or a container.
 *
 * The live case throughout is `Modrinth/zoomify` on Quilt / Minecraft 1.20.5, 2026-09-06:
 * `yet_another_config_lib_v3-3.6.6+1.20.6-fabric.jar` is tagged for 1.20.5, declares `"minecraft": "~1.20.5"`
 * — so neither selection nor the descriptor gate objects — and demands `"fabric-api": ">=0.100.0+1.20.6"`
 * while the newest Fabric API published for 1.20.5 is `0.97.8+1.20.5`.
 */
internal class DependencyBacktrackTest {

    private val yaclNeedsANewerFabricApi = DependencyBacktrack.Requirement(
        requiringFileName = "yet_another_config_lib_v3-3.6.6+1.20.6-fabric.jar",
        requiringIsCandidate = false,
        requiredModId = "fabric-api",
        versionConstraint = ">=0.100.0+1.20.6"
    )

    /** The live failure: a staged dependency demanding a version of another staged jar that is not there. */
    @Test
    fun aStagedVersionOutsideARequiredRangeIsAConflict() {
        val conflicts = DependencyBacktrack.conflicts(
            listOf(yaclNeedsANewerFabricApi),
            mapOf("fabric-api" to "0.97.8+1.20.5")
        )

        Assertions.assertEquals(1, conflicts.size, "the staged Fabric API is older than the range demands")
        Assertions.assertEquals("fabric-api", conflicts.single().requiredModId)
        Assertions.assertEquals("0.97.8+1.20.5", conflicts.single().stagedVersion)
    }

    /** The ordinary case, which must stay silent or every boot pays for a re-stage it does not need. */
    @Test
    fun aSatisfiedRequirementIsNotAConflict() {
        Assertions.assertEquals(
            emptyList<DependencyBacktrack.Conflict>(),
            DependencyBacktrack.conflicts(
                listOf(yaclNeedsANewerFabricApi),
                mapOf("fabric-api" to "0.110.5+1.20.6")
            )
        )
    }

    /**
     * A requirement naming something that was never staged belongs to `refuseForMissingDependencies`, not
     * here. Demoting a jar over a gap that dropping it cannot close would spend the whole backtrack budget
     * and change nothing.
     */
    @Test
    fun aRequirementForSomethingNotStagedIsNotAConflict() {
        Assertions.assertEquals(
            emptyList<DependencyBacktrack.Conflict>(),
            DependencyBacktrack.conflicts(listOf(yaclNeedsANewerFabricApi), mapOf("something-else" to "1.0.0"))
        )
    }

    /**
     * **The safety property.** `VersionConstraint` accepts everything it cannot read, deliberately, so a
     * grammar gap can never mass-refuse. It must not mass-*demote* either: a constraint nobody can parse is
     * doubt, and doubt boots.
     */
    @Test
    fun anUnreadableConstraintNeverConflicts() {
        val nonsense = listOf("", "   ", "*", "whatever", "not-a-version", "[", ">=", "~")

        for (constraint in nonsense) {
            Assertions.assertEquals(
                emptyList<DependencyBacktrack.Conflict>(),
                DependencyBacktrack.conflicts(
                    listOf(yaclNeedsANewerFabricApi.copy(versionConstraint = constraint)),
                    mapOf("fabric-api" to "0.97.8+1.20.5")
                ),
                "'$constraint' is unreadable, so it must proceed rather than demote"
            )
        }
    }

    /** Ids are matched case-insensitively: a descriptor's spelling is the author's, not a protocol. */
    @Test
    fun modIdsAreMatchedRegardlessOfCase() {
        Assertions.assertEquals(
            1,
            DependencyBacktrack.conflicts(
                listOf(yaclNeedsANewerFabricApi.copy(requiredModId = "Fabric-API")),
                mapOf("fabric-api" to "0.97.8+1.20.5")
            ).size
        )
    }

    /** The demotion is the point: the offending *dependency* file is what gets dropped a build. */
    @Test
    fun theOffendingDependencyIsWhatGetsDemoted() {
        val conflicts = DependencyBacktrack.conflicts(
            listOf(yaclNeedsANewerFabricApi),
            mapOf("fabric-api" to "0.97.8+1.20.5")
        )

        Assertions.assertEquals(
            "yet_another_config_lib_v3-3.6.6+1.20.6-fabric.jar",
            DependencyBacktrack.fileToDemote(conflicts)
        )
    }

    /**
     * **The candidate is never demoted.** It is the subject of the experiment; swapping it for an older
     * build would answer a question about a different mod, which is precisely the confusion the
     * other-version crash re-check exists to keep separate from a candidate's own verdict.
     */
    @Test
    fun theCandidateIsNeverDemoted() {
        val conflicts = DependencyBacktrack.conflicts(
            listOf(yaclNeedsANewerFabricApi.copy(requiringFileName = "Zoomify-2.13.3.jar", requiringIsCandidate = true)),
            mapOf("fabric-api" to "0.97.8+1.20.5")
        )

        Assertions.assertEquals(1, conflicts.size, "the conflict is real and worth logging")
        Assertions.assertNull(DependencyBacktrack.fileToDemote(conflicts), "but nothing may be dropped for it")
    }

    /** With both kinds present the dependency is still the one dropped, whatever order they arrived in. */
    @Test
    fun aCandidateConflictNeverShadowsADemotableOne() {
        val conflicts = DependencyBacktrack.conflicts(
            listOf(
                yaclNeedsANewerFabricApi.copy(requiringFileName = "Zoomify-2.13.3.jar", requiringIsCandidate = true),
                yaclNeedsANewerFabricApi
            ),
            mapOf("fabric-api" to "0.97.8+1.20.5")
        )

        Assertions.assertEquals(
            "yet_another_config_lib_v3-3.6.6+1.20.6-fabric.jar",
            DependencyBacktrack.fileToDemote(conflicts)
        )
    }

    /** Nothing wrong, nothing to do. */
    @Test
    fun noConflictsDemoteNothing() {
        Assertions.assertNull(DependencyBacktrack.fileToDemote(emptyList()))
    }
}
