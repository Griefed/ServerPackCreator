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
     * What "other versions of the mod" means: a *diverse* sample, not the next-newest builds. Each pick
     * introduces a Minecraft version-line and a loader no earlier pick used, most recent Minecraft first
     * — so the crashing combination's own line is skipped before its neighbours are considered, and the
     * budget is not spent twice on the same loader.
     *
     * **Why:** measured 2026-08-23 on `creativecore`, both re-checks landed on the same loader, the same
     * loader version (Fabric 0.19.3) and the two Minecraft versions adjacent to the crashing one — near
     * identical code in a near-identical environment, and both came back INCONCLUSIVE while another loader
     * of the same project had booted a server cleanly.
     */
    @Test
    fun recheckCandidatesSpanOtherMinecraftLinesAndOtherLoaders() {
        val files = listOf(
            file("mod-1.20.2-2.jar", setOf("Forge"), setOf("1.20.2")),
            file("mod-1.20.2-1.jar", setOf("Forge"), setOf("1.20.2")),
            file("mod-1.20.1-2.jar", setOf("Forge"), setOf("1.20.1")),
            file("mod-1.20.1-1.jar", setOf("Forge"), setOf("1.20.1")),
            file("mod-1.19.2.jar", setOf("Forge"), setOf("1.19.2")),
            file("mod-fabric-1.18.2.jar", setOf("Fabric"), setOf("1.18.2"))
        )

        val picked = BootCandidateSelector.pickRecheckCandidates(files, "Forge", "1.20.2", limit = 5) { _, _ -> true }

        Assertions.assertEquals(
            listOf(
                Triple("mod-1.19.2.jar", "Forge", "1.19.2"),
                Triple("mod-fabric-1.18.2.jar", "Fabric", "1.18.2"),
                Triple("mod-1.20.1-2.jar", "Forge", "1.20.1")
            ),
            picked.map { Triple(it.file.fileName, it.loader, it.minecraftVersion) },
            "expected a new line first (1.19.2 over the booted line's 1.20.1), then a new loader, and only " +
                "then the booted line's sibling — newest file of each Minecraft version throughout"
        )
    }

    /**
     * The shape that prompted the change, in miniature: `creativecore` crashed on Fabric / Minecraft 26.2
     * while NeoForge booted a server, and the two re-checks it spent both went to Fabric 26.1.2 and 26.1.
     * The diverse sample keeps one same-loader answer and spends the other on a different loader *and* a
     * different Minecraft line.
     */
    @Test
    fun aCrashIsReCheckedOnAnotherLoaderRatherThanTwiceOnItsOwn() {
        val files = listOf(
            file("CreativeCore_FABRIC_v2.14.16_mc26.2.jar", setOf("Fabric"), setOf("26.2")),
            file("CreativeCore_NEOFORGE_v2.14.16_mc26.2.jar", setOf("NeoForge"), setOf("26.2")),
            file("CreativeCore_FABRIC_v2.14.16_mc26.1.2.jar", setOf("Fabric"), setOf("26.1.2")),
            file("CreativeCore_NEOFORGE_v2.14.16_mc26.1.2.jar", setOf("NeoForge"), setOf("26.1.2")),
            file("CreativeCore_FABRIC_v2.14.13_mc26.1.jar", setOf("Fabric"), setOf("26.1")),
            file("CreativeCore_FABRIC_v2.13.39_mc1.21.1.jar", setOf("Fabric"), setOf("1.21.1")),
            file("CreativeCore_NEOFORGE_v2.13.39_mc1.21.1.jar", setOf("NeoForge"), setOf("1.21.1")),
            file("CreativeCore_FORGE_v2.12.39_mc1.20.1.jar", setOf("Forge"), setOf("1.20.1"))
        )

        val picked = BootCandidateSelector.pickRecheckCandidates(files, "Fabric", "26.2", limit = 2) { _, _ -> true }

        Assertions.assertEquals(
            listOf(
                Triple("CreativeCore_FABRIC_v2.14.16_mc26.1.2.jar", "Fabric", "26.1.2"),
                Triple("CreativeCore_NEOFORGE_v2.13.39_mc1.21.1.jar", "NeoForge", "1.21.1")
            ),
            picked.map { Triple(it.file.fileName, it.loader, it.minecraftVersion) },
            "expected the 26.1 line on the crashing loader, then a different loader on a different line — " +
                "never 26.1.2 and 26.1, which are the same line"
        )
    }

    /**
     * Two builds of the same Minecraft version are near-identical code, so only the newest is ever a
     * candidate — but a project that publishes a single Minecraft *line* must still spend its budget, and
     * the relaxation that lets it is what keeps that case as well-sampled as it was before diversity
     * became a preference.
     */
    @Test
    fun aSingleMinecraftLineStillSpendsTheWholeBudget() {
        val files = listOf(
            file("mod-1.20.4.jar", setOf("Forge"), setOf("1.20.4")),
            file("mod-1.20.2-b.jar", setOf("Forge"), setOf("1.20.2")),
            file("mod-1.20.2-a.jar", setOf("Forge"), setOf("1.20.2")),
            file("mod-1.20.1.jar", setOf("Forge"), setOf("1.20.1"))
        )

        Assertions.assertEquals(
            listOf("mod-1.20.2-b.jar" to "1.20.2", "mod-1.20.1.jar" to "1.20.1"),
            BootCandidateSelector.pickRecheckCandidates(files, "Forge", "1.20.4", limit = 2) { _, _ -> true }
                .map { it.file.fileName to it.minecraftVersion }
        )
    }

    /** Each re-check is a full boot, so the limit is a hard budget. */
    @Test
    fun recheckCandidatesStopAtTheLimit() {
        val files = listOf(
            file("mod-1.20.2.jar", setOf("Forge"), setOf("1.20.2")),
            file("mod-1.20.1.jar", setOf("Forge"), setOf("1.20.1")),
            file("mod-1.19.2.jar", setOf("Forge"), setOf("1.19.2"))
        )

        Assertions.assertEquals(
            listOf("1.19.2"),
            BootCandidateSelector.pickRecheckCandidates(files, "Forge", "1.20.2", limit = 1) { _, _ -> true }
                .map { it.minecraftVersion },
            "the single boot goes to the other Minecraft line, not to the crashing line's neighbour"
        )
        Assertions.assertTrue(
            BootCandidateSelector.pickRecheckCandidates(files, "Forge", "1.20.2", limit = 0) { _, _ -> true }.isEmpty(),
            "a zero budget must buy no boots at all"
        )
    }

    /**
     * Same gate as selection, now asked per *loader* as well: a combination the loader has no build for can
     * never be staged, and a cross-loader candidate makes the loader half of that question a real one.
     */
    @Test
    fun recheckCandidatesSkipCombinationsWithoutAnAvailableLoaderVersion() {
        val files = listOf(
            file("mod-1.20.2.jar", setOf("Forge"), setOf("1.20.2")),
            file("mod-1.20.1.jar", setOf("Forge"), setOf("1.20.1")),
            file("mod-1.19.2.jar", setOf("Forge"), setOf("1.19.2")),
            file("mod-fabric-1.19.2.jar", setOf("Fabric"), setOf("1.19.2"))
        )

        Assertions.assertEquals(
            listOf("Forge" to "1.19.2", "Forge" to "1.20.1"),
            BootCandidateSelector.pickRecheckCandidates(files, "Forge", "1.20.2", limit = 5) { loader, _ ->
                loader != "Fabric"
            }.map { it.loader to it.minecraftVersion },
            "the Fabric build is gated out, so the sample falls back to the crashing loader's own versions"
        )
    }

    /** A mod published for exactly one loader and one Minecraft version has nothing to be re-checked against. */
    @Test
    fun aProjectWithNoOtherCombinationYieldsNoRecheckCandidates() {
        val files = listOf(
            file("mod-1.20.2-2.jar", setOf("Forge"), setOf("1.20.2")),
            file("mod-1.20.2-1.jar", setOf("Forge"), setOf("1.20.2"))
        )

        Assertions.assertTrue(
            BootCandidateSelector.pickRecheckCandidates(files, "Forge", "1.20.2", limit = 5) { _, _ -> true }.isEmpty()
        )
    }

    @Test
    fun dependencyFileTakesTheExactMinecraftMatch() {
        val files = listOf(
            file("dep-1.19.2.jar", setOf("Forge"), setOf("1.19.2")),
            file("dep-1.20.1.jar", setOf("Forge"), setOf("1.20.1"))
        )
        Assertions.assertEquals("dep-1.20.1.jar", BootCandidateSelector.pickDependencyFile(files, "Forge", "1.20.1")?.fileName)
        // No file for 1.21 -> nothing is staged. This assertion used to expect `dep-1.19.2.jar`, and that
        // expectation is the bug: see `aDependencyIsNeverStagedForADifferentMinecraftVersion`.
        Assertions.assertNull(BootCandidateSelector.pickDependencyFile(files, "Forge", "1.21"))
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
     * An exact **Minecraft** match must beat a nearer *loader* match. Fabric API tags only its recent files as
     * Quilt-compatible on CurseForge, so a Quilt boot found a Quilt-tagged file for the wrong Minecraft version
     * and stopped before ever trying the Fabric fallback that had the right one.
     *
     * Measured 2026-08-29 over 200 published crash logs: **20 of the 35** boots that staged a Fabric API did so
     * for the wrong Minecraft version — every one of them Quilt, every one of them the newest `+26.3` build,
     * dropped into packs as old as 1.19.2. Quilt Loader then refused the pack with "Fabric API requires version
     * ... of fabricloader/minecraft/java", the boot died, and the *candidate* was scored CRASHED for it.
     */
    @Test
    fun anExactMinecraftMatchBeatsANearerLoaderMatch() {
        val fabricApi = listOf(
            file("fabric-api-0.158.3+26.3.jar", setOf("Fabric", "Quilt"), setOf("26.3")),
            file("fabric-api-0.92.11+1.20.1.jar", setOf("Fabric"), setOf("1.20.1"))
        )

        Assertions.assertEquals(
            "fabric-api-0.92.11+1.20.1.jar",
            BootCandidateSelector.pickDependencyFile(fabricApi, "Quilt", "1.20.1")?.fileName,
            "a Quilt-tagged build for the wrong Minecraft version must not win over the Fabric build for the right one"
        )
    }

    /**
     * A dependency published for no matching Minecraft version is **not** staged at all.
     *
     * The opposite of the candidate rule, and deliberately so: booting the candidate on a near-miss version still
     * tests the candidate, but injecting a wrong-version *dependency* guarantees a loader-level version conflict
     * that kills the boot and is then blamed on the mod under test. `refuseForMissingDependencies` scores the
     * refusal INCONCLUSIVE, which is the honest answer — the mod was never given a fair run.
     */
    @Test
    fun aDependencyIsNeverStagedForADifferentMinecraftVersion() {
        val files = listOf(
            file("dep-1.19.2.jar", setOf("Forge"), setOf("1.19.2")),
            file("dep-1.20.1.jar", setOf("Forge"), setOf("1.20.1"))
        )

        Assertions.assertNull(
            BootCandidateSelector.pickDependencyFile(files, "Forge", "1.21"),
            "staging a 1.19.2 dependency into a 1.21 pack cannot help the boot and can only break it"
        )
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

    /**
     * A declared constraint **narrows** the choice; it must never empty it.
     *
     * That direction is the whole safety property of constraint-aware selection: preferring a satisfying
     * file is an improvement, but returning `null` where the old code returned a file would turn a
     * bootable candidate into a refusal — and `refuseForMissingDependencies` scores a refusal INCONCLUSIVE,
     * so the mod would silently stop being verified at all.
     */
    @Test
    fun aConstraintNarrowsTheChoiceButNeverEmptiesIt() {
        val older = modFile("api-0.91.0.jar", version = "0.91.0")
        val newer = modFile("api-0.92.5.jar", version = "0.92.5")
        val files = listOf(newer, older)

        Assertions.assertEquals(
            newer, BootCandidateSelector.pickDependencyFile(files, "Fabric", "1.20.1", ">=0.92.0"),
            "the satisfying file is preferred"
        )
        Assertions.assertEquals(
            older, BootCandidateSelector.pickDependencyFile(listOf(older), "Fabric", "1.20.1", ">=0.92.0"),
            "when nothing satisfies, the answer must still be what it was without a constraint"
        )
        Assertions.assertEquals(
            BootCandidateSelector.pickDependencyFile(files, "Fabric", "1.20.1"),
            BootCandidateSelector.pickDependencyFile(files, "Fabric", "1.20.1", null),
            "no constraint must behave exactly as before constraints existed"
        )
    }

    /** A file whose version the platform never reported cannot be judged, so it stays eligible. */
    @Test
    fun aFileWithNoKnownVersionIsStillEligible() {
        val unversioned = modFile("api-mystery.jar", version = null)

        Assertions.assertEquals(
            unversioned,
            BootCandidateSelector.pickDependencyFile(listOf(unversioned), "Fabric", "1.20.1", ">=0.92.0")
        )
    }

    /** The Quilt-to-Fabric fallback still applies with a constraint in play — Fabric API is the case. */
    @Test
    fun theQuiltFallbackStillAppliesWithAConstraint() {
        val fabricOnly = modFile("fabric-api-0.92.5.jar", version = "0.92.5", loaders = setOf("Fabric"))

        Assertions.assertEquals(
            fabricOnly,
            BootCandidateSelector.pickDependencyFile(listOf(fabricOnly), "Quilt", "1.20.1", ">=0.92.0")
        )
    }

    /** Shared builder for the constraint guards above. */
    private fun modFile(
        fileName: String,
        version: String?,
        loaders: Set<String> = setOf("Fabric"),
        minecraftVersions: Set<String> = setOf("1.20.1")
    ) = ModFile(fileName, loaders, minecraftVersions, "https://example.invalid/$fileName", null, emptyList(), version)
}
