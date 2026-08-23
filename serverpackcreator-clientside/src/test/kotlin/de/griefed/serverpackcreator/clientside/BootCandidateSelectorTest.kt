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
 * Pins the boot file/version-selection that drives which combination the boot-test installs, free of
 * any [de.griefed.serverpackcreator.api.ApiWrapper] or running server.
 */
internal class BootCandidateSelectorTest {

    private fun file(name: String, loaders: Set<String>, mcVersions: Set<String>) =
        ModFile(name, loaders, mcVersions, "https://cdn/$name", null, emptyList())

    @Test
    fun minecraftComparatorOrdersNumericallyNotLexically() {
        Assertions.assertTrue(BootCandidateSelector.minecraftComparator.compare("1.20", "1.9") > 0)
        Assertions.assertTrue(BootCandidateSelector.minecraftComparator.compare("1.20.1", "1.20") > 0)
    }

    @Test
    fun picksNewestMinecraftVersionForLoader() {
        val files = listOf(
            file("mod-1.19.2.jar", setOf("Forge"), setOf("1.19.2")),
            file("mod-1.20.1.jar", setOf("Forge"), setOf("1.20.1"))
        )
        val candidate = BootCandidateSelector.pickBootableCandidate(files, "Forge") { true }
        Assertions.assertEquals("1.20.1", candidate?.second)
    }

    @Test
    fun skipsMinecraftVersionsWithoutAnAvailableLoaderVersion() {
        val files = listOf(
            file("mod-1.21.jar", setOf("Forge"), setOf("1.21")),
            file("mod-1.20.1.jar", setOf("Forge"), setOf("1.20.1"))
        )
        // The newest (1.21) has no loader version yet; selection falls back to 1.20.1.
        val candidate = BootCandidateSelector.pickBootableCandidate(files, "Forge") { it != "1.21" }
        Assertions.assertEquals("1.20.1", candidate?.second)
    }

    @Test
    fun returnsNullWhenNoLoaderVersionIsEverAvailable() {
        val files = listOf(file("mod-1.20.1.jar", setOf("Fabric"), setOf("1.20.1")))
        Assertions.assertNull(BootCandidateSelector.pickBootableCandidate(files, "Fabric") { false })
        // also null for a loader the project does not ship
        Assertions.assertNull(BootCandidateSelector.pickBootableCandidate(files, "Forge") { true })
    }

    // --- the sample the other-version crash re-check boots ------------------------------------------

    /**
     * What "other versions of the mod" means: the newest file of each *other* Minecraft version, most recent
     * Minecraft first. One per version, never two builds of the same one — two rebuilds for one Minecraft are
     * near-identical code, so the second boot buys far less than a different version line does — and never a
     * file the booted version already covered, which is the whole point of re-checking somewhere else.
     */
    @Test
    fun recheckCandidatesAreTheNewestFileOfEachOtherMinecraftVersion() {
        val files = listOf(
            file("mod-1.20.2-2.jar", setOf("Forge"), setOf("1.20.2")),
            file("mod-1.20.2-1.jar", setOf("Forge"), setOf("1.20.2")),
            file("mod-1.20.1-2.jar", setOf("Forge"), setOf("1.20.1")),
            file("mod-1.20.1-1.jar", setOf("Forge"), setOf("1.20.1")),
            file("mod-1.19.2.jar", setOf("Forge"), setOf("1.19.2")),
            file("mod-fabric-1.18.2.jar", setOf("Fabric"), setOf("1.18.2"))
        )

        val picked = BootCandidateSelector.pickRecheckCandidates(files, "Forge", "1.20.2", limit = 5) { true }

        Assertions.assertEquals(
            listOf("mod-1.20.1-2.jar" to "1.20.1", "mod-1.19.2.jar" to "1.19.2"),
            picked.map { it.first.fileName to it.second },
            "expected the newest Forge file of 1.20.1 then 1.19.2 — no 1.20.2 sibling, no Fabric file"
        )
    }

    /** Each re-check is a full boot, so the limit is a hard budget, taken from the most recent end. */
    @Test
    fun recheckCandidatesStopAtTheLimit() {
        val files = listOf(
            file("mod-1.20.2.jar", setOf("Forge"), setOf("1.20.2")),
            file("mod-1.20.1.jar", setOf("Forge"), setOf("1.20.1")),
            file("mod-1.19.2.jar", setOf("Forge"), setOf("1.19.2"))
        )

        Assertions.assertEquals(
            listOf("1.20.1"),
            BootCandidateSelector.pickRecheckCandidates(files, "Forge", "1.20.2", limit = 1) { true }.map { it.second }
        )
        Assertions.assertTrue(
            BootCandidateSelector.pickRecheckCandidates(files, "Forge", "1.20.2", limit = 0) { true }.isEmpty(),
            "a zero budget must buy no boots at all"
        )
    }

    /** Same gate as selection: a Minecraft version the loader has no build for can never be staged. */
    @Test
    fun recheckCandidatesSkipMinecraftVersionsWithoutAnAvailableLoaderVersion() {
        val files = listOf(
            file("mod-1.20.2.jar", setOf("Forge"), setOf("1.20.2")),
            file("mod-1.20.1.jar", setOf("Forge"), setOf("1.20.1")),
            file("mod-1.19.2.jar", setOf("Forge"), setOf("1.19.2"))
        )

        Assertions.assertEquals(
            listOf("1.19.2"),
            BootCandidateSelector.pickRecheckCandidates(files, "Forge", "1.20.2", limit = 5) { it != "1.20.1" }
                .map { it.second }
        )
    }

    /** A mod published for exactly one Minecraft version has nothing to be re-checked against. */
    @Test
    fun aProjectWithNoOtherMinecraftVersionYieldsNoRecheckCandidates() {
        val files = listOf(
            file("mod-1.20.2-2.jar", setOf("Forge"), setOf("1.20.2")),
            file("mod-1.20.2-1.jar", setOf("Forge"), setOf("1.20.2"))
        )

        Assertions.assertTrue(
            BootCandidateSelector.pickRecheckCandidates(files, "Forge", "1.20.2", limit = 5) { true }.isEmpty()
        )
    }

    @Test
    fun dependencyFilePrefersExactMinecraftMatchThenFallsBack() {
        val files = listOf(
            file("dep-1.19.2.jar", setOf("Forge"), setOf("1.19.2")),
            file("dep-1.20.1.jar", setOf("Forge"), setOf("1.20.1"))
        )
        Assertions.assertEquals("dep-1.20.1.jar", BootCandidateSelector.pickDependencyFile(files, "Forge", "1.20.1")?.fileName)
        // no exact match -> first file for the loader
        Assertions.assertEquals("dep-1.19.2.jar", BootCandidateSelector.pickDependencyFile(files, "Forge", "1.21")?.fileName)
        Assertions.assertNull(BootCandidateSelector.pickDependencyFile(files, "Fabric", "1.20.1"))
    }

    /**
     * A Quilt boot must accept a **Fabric**-tagged dependency file, because Quilt deliberately runs Fabric mods —
     * which is why the canonical dependency of a Quilt mod is Fabric API, a project that publishes only Fabric files.
     *
     * Measured live on 2026-07-30: strict loader matching silently dropped **210** dependencies, 210 of them on
     * Quilt, and the single most-dropped ref was `P7dR8mSH` — Fabric API (CurseForge `306612`) — 27 times in one
     * sweep. The mod then hard-failed with "requires fabric-api" and the whole boot was wasted, which is the largest
     * failure class in the kept boot logs.
     */
    @Test
    fun aQuiltDependencyFallsBackToTheFabricBuild() {
        val fabricApi = listOf(
            file("fabric-api-0.100.0+1.20.1.jar", setOf("Fabric"), setOf("1.20.1")),
            file("fabric-api-0.92.0+1.19.2.jar", setOf("Fabric"), setOf("1.19.2"))
        )

        Assertions.assertEquals(
            "fabric-api-0.100.0+1.20.1.jar",
            BootCandidateSelector.pickDependencyFile(fabricApi, "Quilt", "1.20.1")?.fileName,
            "Quilt loads Fabric mods; refusing the Fabric build leaves the mod without its required dependency"
        )
    }

    /** A real Quilt build is still preferred over the Fabric fallback when the dependency publishes both. */
    @Test
    fun aQuiltBuildWinsOverTheFabricFallback() {
        val files = listOf(
            file("dep-fabric.jar", setOf("Fabric"), setOf("1.20.1")),
            file("dep-quilt.jar", setOf("Quilt"), setOf("1.20.1"))
        )

        Assertions.assertEquals("dep-quilt.jar", BootCandidateSelector.pickDependencyFile(files, "Quilt", "1.20.1")?.fileName)
    }

    /**
     * The fallback is Quilt-only and deliberately not symmetric. Fabric cannot load Quilt mods, and NeoForge only
     * loads Forge mods for a narrow range of Minecraft versions — guessing there would stage a jar the loader cannot
     * use and turn a clean signal into noise.
     */
    @Test
    fun theFallbackDoesNotApplyToOtherLoaders() {
        val quiltOnly = listOf(file("dep-quilt.jar", setOf("Quilt"), setOf("1.20.1")))
        val forgeOnly = listOf(file("dep-forge.jar", setOf("Forge"), setOf("1.20.1")))

        Assertions.assertNull(
            BootCandidateSelector.pickDependencyFile(quiltOnly, "Fabric", "1.20.1"),
            "Fabric cannot load a Quilt mod"
        )
        Assertions.assertNull(
            BootCandidateSelector.pickDependencyFile(forgeOnly, "NeoForge", "1.21.1"),
            "NeoForge/Forge cross-loading is version-dependent — do not guess"
        )
    }
}
