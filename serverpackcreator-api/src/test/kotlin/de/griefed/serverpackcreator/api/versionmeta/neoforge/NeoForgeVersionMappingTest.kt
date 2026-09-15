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
package de.griefed.serverpackcreator.api.versionmeta.neoforge

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Pins the Minecraft → NeoForge version mapping, the rule that decides which NeoForge builds belong to which
 * Minecraft version.
 *
 * **Why this needs pinning:** the mapping fails *silently*. A wrong pattern does not throw — it produces an empty or
 * mis-assigned NeoForge version list, and everything downstream then behaves as though NeoForge simply has no builds
 * for that Minecraft version. The grinder made the cost visible on 2026-07-30: `LoaderVersionResolver` drops a
 * Minecraft/loader combination whose loader reports no version, so a broken mapping quietly erases NeoForge coverage
 * for whole Minecraft versions rather than failing anywhere a human would notice.
 *
 * The version pairs below are **real**, taken from NeoForge's published manifests, because the whole risk here is a
 * pattern that looks plausible and does not match reality.
 */
internal class NeoForgeVersionMappingTest {

    /** Assert that every one of [neoForgeVersions] is claimed by [minecraftVersion]. */
    private fun assertClaims(minecraftVersion: String, vararg neoForgeVersions: String) {
        val pattern = NeoForgeLoader.neoForgeVersionPatternFor(minecraftVersion)
            ?: Assertions.fail("Minecraft $minecraftVersion yielded no pattern at all")
        for (neoForgeVersion in neoForgeVersions) {
            Assertions.assertTrue(
                neoForgeVersion.matches(pattern),
                "Minecraft $minecraftVersion must claim NeoForge $neoForgeVersion (pattern: $pattern)"
            )
        }
    }

    /** Assert that none of [neoForgeVersions] is claimed by [minecraftVersion]. */
    private fun assertRejects(minecraftVersion: String, vararg neoForgeVersions: String) {
        val pattern = NeoForgeLoader.neoForgeVersionPatternFor(minecraftVersion)
            ?: Assertions.fail("Minecraft $minecraftVersion yielded no pattern at all")
        for (neoForgeVersion in neoForgeVersions) {
            Assertions.assertFalse(
                neoForgeVersion.matches(pattern),
                "Minecraft $minecraftVersion must NOT claim NeoForge $neoForgeVersion (pattern: $pattern)"
            )
        }
    }

    /**
     * The classic scheme: NeoForge drops Minecraft's leading `1.`, so Minecraft `1.21.1` becomes `21.1.x`.
     */
    @Test
    fun classicMinecraftVersionsMapByDroppingTheLeadingOne() {
        assertClaims("1.20.2", "20.2.88", "20.2.86")
        assertClaims("1.20.4", "20.4.238")
        assertClaims("1.21.1", "21.1.247", "21.1.244", "21.1.1")
        assertClaims("1.21.4", "21.4.148")
        assertClaims("1.21.8", "21.8.54")
    }

    /**
     * A two-part Minecraft version means patch `0` in NeoForge's numbering: Minecraft `1.21` is `21.0.x`, **not**
     * `21.x`. Getting this wrong is the difference between finding every build for `1.21` and finding none.
     */
    @Test
    fun aMissingMinecraftPatchBecomesAnExplicitZero() {
        assertClaims("1.21", "21.0.167", "21.0.0")
        assertClaims("1.20", "20.0.1")
    }

    /**
     * The newer `YY.x` Minecraft scheme is kept as-is rather than having a leading component stripped — Minecraft
     * `26.1.2` is NeoForge `26.1.2.x`, and Minecraft `26.2` is `26.2.0.x`. Both were verified against live boots on
     * 2026-07-30 (`NeoForge 26.1.2.93`, `NeoForge 26.2.0.40-beta`).
     */
    @Test
    fun theNewMinecraftVersioningSchemeKeepsItsMajor() {
        assertClaims("26.1.2", "26.1.2.93")
        assertClaims("26.2", "26.2.0.40-beta", "26.2.0.1")
    }

    /** Pre-release suffixes such as `-beta` belong to the build and must not stop it being claimed. */
    @Test
    fun buildSuffixesAreAccepted() {
        assertClaims("26.2", "26.2.0.40-beta")
        assertClaims("1.21.1", "21.1.100-beta", "21.1.100-rc1")
    }

    /**
     * **The bug class this test file exists for.** A pattern must claim its *own* builds and nobody else's: adjacent
     * Minecraft versions share a numeric prefix (`1.21.1` vs `1.21.10`, `1.21` vs `1.21.1`), so a pattern that is even
     * slightly too loose silently steals another version's builds and reports NeoForge versions that cannot run that
     * Minecraft version at all.
     */
    @Test
    fun aVersionNeverClaimsAnAdjacentVersionsBuilds() {
        // 1.21 is 21.0.x only — it must not swallow every 21.y.z
        assertRejects("1.21", "21.1.247", "21.4.148", "21.8.54")
        // 1.21.1 must not reach into 1.21.10's builds, nor into a bare two-part build
        assertRejects("1.21.1", "21.10.5", "21.11.2", "21.2.1")
        assertRejects("1.21.10", "21.1.247")
        // The new scheme must stay inside its own major/minor
        assertRejects("26.2", "26.1.2.93", "27.2.0.1")
        assertRejects("26.1.2", "26.1.3.1", "26.2.0.40-beta")
    }

    /**
     * A NeoForge version must carry at least one build number beyond the Minecraft part, so the bare Minecraft
     * component alone is not a NeoForge version.
     */
    @Test
    fun theMinecraftPartAloneIsNotANeoForgeVersion() {
        assertRejects("1.21.1", "21.1")
        assertRejects("1.21", "21.0")
        assertRejects("26.1.2", "26.1.2")
    }

    /**
     * Snapshots, pre-releases and release candidates never have NeoForge builds, so they yield no pattern and are
     * skipped rather than matched loosely against everything.
     */
    @Test
    fun nonReleaseMinecraftVersionsYieldNoPattern() {
        for (version in listOf("24w14a", "25w03b", "1.21.1-pre1", "1.21-rc1", "1.14.4-pre7", "3D Shareware v1.34", "")) {
            Assertions.assertNull(
                NeoForgeLoader.neoForgeVersionPatternFor(version),
                "Minecraft '$version' cannot have NeoForge builds and must yield no pattern"
            )
        }
    }

    /**
     * Documents a known limitation rather than asserting it is good: the new-scheme branches require a **two-digit**
     * major, so a three-digit major would silently yield no pattern and lose NeoForge coverage entirely. Recorded
     * here so that if Minecraft ever ships one, this test fails and the choice becomes deliberate instead of a
     * silent gap.
     */
    @Test
    fun aThreeDigitMajorIsNotYetSupported() {
        Assertions.assertNull(
            NeoForgeLoader.neoForgeVersionPatternFor("100.1"),
            "if this now returns a pattern, the two-digit assumption was lifted — update the docs with it"
        )
    }

    /**
     * The old `1.x` scheme is unbounded in its minor, so the pattern must keep working as Minecraft's minor grows
     * past two digits — `1.100.1` is structurally fine and must not be dropped.
     */
    @Test
    fun theClassicSchemeHandlesLargeMinors() {
        assertClaims("1.100.1", "100.1.5")
        assertClaims("1.100", "100.0.5")
    }
}
