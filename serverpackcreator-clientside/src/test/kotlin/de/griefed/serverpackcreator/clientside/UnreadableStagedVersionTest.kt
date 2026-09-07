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
 * Pins the **mirror** of the rule [VersionConstraintFuzzTest] already covers: an unreadable *constraint*
 * accepts, and so must an unreadable **version**.
 *
 * `numbersOf` reads a version by splitting on `.` and taking each component's leading digits, mapping
 * anything digit-less to `0`. That is right for `0.92.2+1.20.1` and catastrophic for text: `Balm 26.2.0.7`
 * becomes `[0, 2, 0, 7]` and `balm-fabric-26.2-26.2.0.7.jar` — everything before the first `-` — becomes
 * `[0]`. Both then read as *older than almost any range*, and nothing objected, because the
 * fail-toward-accepting guards (`looksLikeVersion`) are applied to the constraint side only.
 *
 * **Why a version is text in the first place:** `ModFile.version` is the platform's own version string, and
 * CurseForge has no version field — `CurseForgePlatform.toModFile` fills it with `displayName`, which is
 * whatever the author typed as the release name and is documented there as "often decorated".
 *
 * **Measured on the live daemon, 2026-09-07**, one day after `DependencyBacktrack` went in: `1014`
 * re-stagings (`re-staging … without it`) and `146` `publishes no … file for Minecraft` lines in a single
 * day, ending in **47 published `ERROR` verdicts** reading *"Required dependency unavailable"* — for files
 * that exist. The CurseForge API, asked directly for the four worst (`misc/cf-dependency-probe.sh`),
 * returns every one of them correctly loader-tagged: `balm-fabric-26.2-26.2.0.7.jar`,
 * `architectury-9.2.14-fabric.jar`, `thermal_foundation-1.20.1-11.0.6.70.jar` and ten Fabric-tagged
 * `create-fabric` 1.20.1 builds. The demote loop simply exhausted the file list first, and
 * `BootVerifier.withoutExcluded` then left `pickDependencyFile` nothing to pick.
 *
 * The direction of the fix is the module's standing rule: a version we cannot read is **not evidence of a
 * conflict**. Missing a real conflict costs one boot; inventing one costs a published verdict.
 *
 * @author Griefed
 */
internal class UnreadableStagedVersionTest {

    /** Balm on CurseForge, as `displayName` spells it — a real release name, not a version string. */
    private val decoratedDisplayNames = listOf(
        "Balm 26.2.0.7",
        "balm-fabric-26.2-26.2.0.7.jar",
        "Create 6.0.10 for NeoForge 1.21.1",
        "JEI 15.2.0.27 for 1.20.1",
        "[1.20.1] Thermal Foundation 11.0.6.70"
    )

    /** The mirror of `noMalformedConstraintEverRefuses`: text where a version was expected must accept. */
    @Test
    fun anUnreadableVersionNeverRefuses() {
        val constraints = listOf(">=26.2.0", "[1.0.82,)", "~1.20.5", "[1,)", ">=0.100.0+1.20.6")

        val refusals = decoratedDisplayNames.flatMap { version ->
            constraints.filterNot { VersionConstraint.satisfies(version, it) }.map { version to it }
        }

        Assertions.assertTrue(
            refusals.isEmpty(),
            "a version that cannot be read must accept, never refuse — these did not: $refusals"
        )
    }

    /**
     * The field case end to end: seven candidates (`crafting-tweaks`, `cooking-for-blockheads`,
     * `kleeslabs`, `netherportalfix`, `trashslot`, `waystones`, `farming-for-blockheads`) published
     * `ERROR` for a Balm that was staged, newer than asked for, and reachable.
     */
    @Test
    fun aDecoratedStagedVersionIsNotAConflict() {
        val requirement = DependencyBacktrack.Requirement(
            requiringFileName = "kleeslabs-fabric-26.2-26.2.0.jar",
            requiringIsCandidate = false,
            requiredModId = "balm",
            versionConstraint = ">=26.2.0"
        )

        Assertions.assertEquals(
            emptyList<DependencyBacktrack.Conflict>(),
            DependencyBacktrack.conflicts(listOf(requirement), mapOf("balm" to "Balm 26.2.0.7")),
            "26.2.0.7 satisfies >=26.2.0; only the decoration made it look older"
        )
    }

    /**
     * The guard against over-correcting: this must **not** turn the backtrack off. A version the parser can
     * genuinely read still conflicts — the `zoomify` case `DependencyBacktrack` was built for.
     */
    @Test
    fun aReadableStagedVersionStillConflicts() {
        val requirement = DependencyBacktrack.Requirement(
            requiringFileName = "yet_another_config_lib_v3-3.6.6+1.20.6-fabric.jar",
            requiringIsCandidate = false,
            requiredModId = "fabric-api",
            versionConstraint = ">=0.100.0+1.20.6"
        )

        Assertions.assertEquals(
            1,
            DependencyBacktrack.conflicts(listOf(requirement), mapOf("fabric-api" to "0.97.8+1.20.5")).size,
            "0.97.8 really is older than 0.100.0, and demoting YACL is what makes that pack boot"
        )
    }

    /** And the comparison itself is untouched for versions that are versions. */
    @Test
    fun aReadableVersionIsStillCompared() {
        Assertions.assertFalse(VersionConstraint.satisfies("0.97.8+1.20.5", ">=0.100.0+1.20.6"))
        Assertions.assertTrue(VersionConstraint.satisfies("26.2.0.7", ">=26.2.0"))
        Assertions.assertTrue(VersionConstraint.satisfies("v2.1", ">=2.0"), "a leading v is decoration, not text")
    }
}
