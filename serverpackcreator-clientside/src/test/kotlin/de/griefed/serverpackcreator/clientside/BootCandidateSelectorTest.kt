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

    /**
     * **The shape this suite never had, and the one that produced a live false positive.** A Modrinth version
     * tagged `loaders: [forge, neoforge]` with two primary jars gives *both* `ModFile`s the *same* two-loader
     * set — the loaders are read from the version node and applied to every file of it. CurseForge does the
     * same from one flat `gameVersions` array.
     *
     * The selector then legitimately offers either jar for either loader, and being a stable sort it takes
     * whichever the platform listed first. That is how `DamageVignette-2.0.2-forge+mc1.20.jar` came to be
     * booted under NeoForge 20.4.251 on 2026-08-31, dying on `Missing language javafml version [46,)`.
     *
     * This is characterization, not a complaint: the selector cannot tell the two jars apart from metadata
     * that describes them identically. `JarSelfDeclaration` is what refuses the pick afterwards, from the
     * jar's own descriptor — see `JarSelfDeclarationTest.aForgeJarIsRefusedForANeoForgeBoot`.
     */
    @Test
    fun aFileClaimingTwoLoadersIsOfferedForBoth() {
        val bothTagged = setOf("Forge", "NeoForge")
        val files = listOf(
            file("DamageVignette-2.0.2-forge+mc1.20.jar", bothTagged, setOf("1.20.4")),
            file("DamageVignette-2.0.2-neoforge+mc1.20.jar", bothTagged, setOf("1.20.4"))
        )

        val forFabricless = BootCandidateSelector.pickBootableCandidate(files, "NeoForge") { true }

        Assertions.assertNotNull(forFabricless)
        Assertions.assertEquals(
            "DamageVignette-2.0.2-forge+mc1.20.jar", forFabricless?.first?.fileName,
            "the stable sort takes whichever the platform listed first — a FORGE jar for a NeoForge boot, " +
                "which only the post-stage descriptor gate can catch"
        )
    }

    /**
     * The Minecraft twin of the same problem. A platform's declared version set is what an author ticked, and
     * `ModrinthPlatform.filesOf` applies a version node's whole `game_versions` list to every file of it
     * without filtering — so one jar becomes one candidate *per version*, and the newest wins.
     *
     * Measured 2026-08-31: `create_ltab` booted on Minecraft 1.20.6 against older mappings, dying with
     * `@Inject … could not find any targets matching 'Lnet/minecraft/class_4317;method_20807'`. Nothing here
     * asks what the jar was compiled for, because nothing here can — the descriptor gate does.
     */
    @Test
    fun aFileClaimingSeveralMinecraftVersionsIsBootedOnTheNewest() {
        val files = listOf(file("create_ltab-2.1.2.jar", setOf("Fabric"), setOf("1.20.4", "1.20.5", "1.20.6")))

        val candidate = BootCandidateSelector.pickBootableCandidate(files, "Fabric") { true }

        Assertions.assertEquals("1.20.6", candidate?.second, "one jar, three candidate versions, newest taken")
        Assertions.assertEquals("create_ltab-2.1.2.jar", candidate?.first?.fileName)
    }

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
     * The fallback is deliberately not symmetric, and the NeoForge half of it is deliberately narrow: Fabric
     * cannot load Quilt mods at all, and NeoForge loads Forge mods on Minecraft 1.20.1 only — see
     * [theNeoForgeFallbackToForgeAppliesOnMinecraft1201Only].
     *
     * **The Forge fixture used to be tagged 1.20.1 and asked for at 1.21.1**, so it answered `null` because no
     * file carried the version at all — whatever the loader rule said. The message spoke about cross-loading
     * while the assertion could not see it; both fixtures are now asked at the version they carry.
     */
    @Test
    fun theFallbackDoesNotApplyToOtherLoaders() {
        val quiltOnly = listOf(file("dep-quilt.jar", setOf("Quilt"), setOf("1.20.1")))
        val forgeOnly = listOf(file("dep-forge.jar", setOf("Forge"), setOf("1.21.1")))

        Assertions.assertNull(
            BootCandidateSelector.pickDependencyFile(quiltOnly, "Fabric", "1.20.1"),
            "Fabric cannot load a Quilt mod"
        )
        Assertions.assertNull(
            BootCandidateSelector.pickDependencyFile(forgeOnly, "NeoForge", "1.21.1"),
            "NeoForge renamed its packages away from Forge's at 1.20.2 — a Forge build is not one here"
        )
    }

    /**
     * NeoForge 20.1.x is Forge 47 under another name — same `net.minecraftforge` packages, same `javafml`,
     * same `META-INF/mods.toml` — so a dependency publishing only Forge files really is stageable for a
     * NeoForge boot on Minecraft 1.20.1. Refusing it costs the entire boot, because
     * `BootVerifier.refuseForMissingDependencies` scores an unstageable requirement INCONCLUSIVE.
     *
     * One Minecraft version wide, not a range: 1.20.2 renamed the packages and ended the compatibility.
     */
    @Test
    fun theNeoForgeFallbackToForgeAppliesOnMinecraft1201Only() {
        val forgeOnly = listOf(file("dep-forge.jar", setOf("Forge"), setOf("1.20.1", "1.20.2")))

        Assertions.assertEquals(
            "dep-forge.jar",
            BootCandidateSelector.pickDependencyFile(forgeOnly, "NeoForge", "1.20.1")?.fileName,
            "NeoForge 20.1.x loads a Forge 1.20.1 mod unchanged"
        )
        Assertions.assertNull(
            BootCandidateSelector.pickDependencyFile(forgeOnly, "NeoForge", "1.20.2"),
            "the band is exactly 1.20.1"
        )
    }

    /** A real NeoForge build still wins where the dependency publishes one, exactly as Quilt's fallback does. */
    @Test
    fun aRealNeoForgeBuildIsPreferredOverTheForgeFallback() {
        val files = listOf(
            file("dep-forge.jar", setOf("Forge"), setOf("1.20.1")),
            file("dep-neoforge.jar", setOf("NeoForge"), setOf("1.20.1"))
        )

        Assertions.assertEquals(
            "dep-neoforge.jar",
            BootCandidateSelector.pickDependencyFile(files, "NeoForge", "1.20.1")?.fileName
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

    /**
     * **JEI's live shape, and the reason this function exists.** `jei-1.21.1-forge-19.52.0.422.jar` is
     * tagged on both platforms for Minecraft 1.21 *and* 1.21.1, while its own `META-INF/mods.toml`
     * declares `versionRange="[1.21, 1.21.1)"` — a Maven range whose `)` excludes the very version the
     * file is named after. JEI's `gradle.properties` builds it as `[start, thisVersion)` where it should
     * be `[start, nextVersion)`, so the descriptor is upstream-wrong, not misread.
     *
     * [BootCandidateSelector.pickBootableCandidate] takes the newest and lands on 1.21.1, which the
     * descriptor gate then vetoes — and the boot is lost even though **1.21 satisfies both the platform
     * and the jar**. Picking the newest version the jar itself accepts is what turns that refusal back
     * into a verification.
     */
    @Test
    fun theNewestVersionTheJarItselfAcceptsIsPickedWhenTheNewestTaggedOneIsExcluded() {
        val jei = file("jei-1.21.1-forge-19.52.0.422.jar", setOf("Forge"), setOf("1.21", "1.21.1"))

        Assertions.assertEquals(
            "1.21",
            BootCandidateSelector.newestVersionSatisfying(jei, "[1.21, 1.21.1)") { true },
            "1.21 is tagged by the platform and accepted by the jar, so the boot is not lost"
        )
    }

    /** The gate still applies: a version the jar accepts but the host cannot boot is not a way out. */
    @Test
    fun aVersionTheJarAcceptsButTheHostCannotBootIsNotPicked() {
        val jei = file("jei-1.21.1-forge-19.52.0.422.jar", setOf("Forge"), setOf("1.21", "1.21.1"))

        Assertions.assertNull(
            BootCandidateSelector.newestVersionSatisfying(jei, "[1.21, 1.21.1)") { false }
        )
    }

    /**
     * Nothing to fall back to must stay `null` rather than quietly returning the excluded version — the
     * caller keeps the original refusal, which is the honest outcome when jar and platform truly disagree.
     */
    @Test
    fun aJarThatAcceptsNoTaggedVersionYieldsNothing() {
        val stale = file("stale.jar", setOf("Forge"), setOf("1.21.1"))

        Assertions.assertNull(BootCandidateSelector.newestVersionSatisfying(stale, "[1.20, 1.20.1)") { true })
    }

    /** A file whose author opted out of third-party distribution: CurseForge publishes no URL for it. */
    private fun lockedFile(name: String, loaders: Set<String>, mcVersions: Set<String>) =
        ModFile(name, loaders, mcVersions, null, null, emptyList())

    /**
     * **A locked file cannot be staged, so it must not be preferred over one that can.**
     *
     * `pickForLoader` took the first match by loader and Minecraft version and never asked whether the file
     * was obtainable. A distribution-locked newest build therefore beat an obtainable older one, the
     * download returned `null`, and the dependency was reported unmet — the shape behind
     * *"Required dependency unavailable for Quilt / Minecraft 1.20.4: 306612"*, CurseForge's Fabric API.
     */
    @Test
    fun anObtainableDependencyBeatsALockedNewerOne() {
        val files = listOf(
            lockedFile("fabric-api-0.97.jar", setOf("Fabric"), setOf("1.20.4")),
            file("fabric-api-0.96.jar", setOf("Fabric"), setOf("1.20.4"))
        )

        Assertions.assertEquals(
            "fabric-api-0.96.jar",
            BootCandidateSelector.pickDependencyFile(files, "Fabric", "1.20.4")?.fileName,
            "the newest file has no download URL; picking it guarantees a refusal"
        )
    }

    /**
     * **And obtainability outranks the loader preference**, which is the half that actually explains the
     * Quilt report. Quilt runs Fabric mods, so an obtainable Fabric build is a working dependency while a
     * locked Quilt build is nothing at all — the fallback exists precisely to be used here.
     */
    @Test
    fun anObtainableFabricBuildBeatsALockedQuiltOne() {
        val files = listOf(
            lockedFile("lib-quilt.jar", setOf("Quilt"), setOf("1.20.4")),
            file("lib-fabric.jar", setOf("Fabric"), setOf("1.20.4"))
        )

        Assertions.assertEquals(
            "lib-fabric.jar",
            BootCandidateSelector.pickDependencyFile(files, "Quilt", "1.20.4")?.fileName,
            "an exact-loader match that cannot be downloaded is worse than a usable fallback"
        )
    }

    /**
     * When **everything** is locked, still return a file rather than `null`. The refusal then reads
     * "distribution-locked", which is true and actionable; `null` would read "publishes no Quilt file for
     * Minecraft 1.20.4", which is false. Preference, never filter — the rule this function already follows
     * for version constraints.
     */
    @Test
    fun anAllLockedProjectStillYieldsAFileSoTheRefusalCanBeHonest() {
        val files = listOf(lockedFile("lib-quilt.jar", setOf("Quilt"), setOf("1.20.4")))

        Assertions.assertEquals(
            "lib-quilt.jar",
            BootCandidateSelector.pickDependencyFile(files, "Quilt", "1.20.4")?.fileName
        )
    }

    /** The established preferences survive: an exact loader still wins when both are obtainable. */
    @Test
    fun anObtainableExactLoaderStillBeatsAnObtainableFallback() {
        val files = listOf(
            file("lib-fabric.jar", setOf("Fabric"), setOf("1.20.4")),
            file("lib-quilt.jar", setOf("Quilt"), setOf("1.20.4"))
        )

        Assertions.assertEquals(
            "lib-quilt.jar",
            BootCandidateSelector.pickDependencyFile(files, "Quilt", "1.20.4")?.fileName
        )
    }

    /** And the version constraint still narrows among obtainable files. */
    @Test
    fun theVersionConstraintStillNarrowsAmongObtainableFiles() {
        val files = listOf(
            ModFile("lib-2.0.jar", setOf("Fabric"), setOf("1.20.4"), "https://cdn/2", null, emptyList(), "2.0.0"),
            ModFile("lib-1.0.jar", setOf("Fabric"), setOf("1.20.4"), "https://cdn/1", null, emptyList(), "1.0.0")
        )

        Assertions.assertEquals(
            "lib-1.0.jar",
            BootCandidateSelector.pickDependencyFile(files, "Fabric", "1.20.4", ">=1.0.0 <2.0.0")?.fileName
        )
    }

    /** A file the author opted out of distributing, carrying a version so a constraint can select it. */
    private fun lockedAtVersion(name: String, version: String) =
        ModFile(name, setOf("Fabric"), setOf("1.20.1"), null, null, emptyList(), version)

    /** An ordinary, fetchable file at a given version. */
    private fun obtainableAtVersion(name: String, version: String) =
        ModFile(name, setOf("Fabric"), setOf("1.20.1"), "https://example.invalid/$name", null, emptyList(), version)

    /**
     * **Obtainability outranks the version constraint** — the one pair of preference arms this suite never
     * separated.
     *
     * `pickDependencyFile` narrows four times: satisfying-and-obtainable, then obtainable, then satisfying,
     * then anything. The middle two are the interesting pair, and every existing test varied one axis at a
     * time: locked-versus-obtainable with no constraint in play, and constraint-narrowing with nothing
     * locked. Swapping arms 2 and 3 therefore passed.
     *
     * It must prefer the obtainable file even though the locked one is the only version the constraint
     * accepts, because a locked file has no `downloadUrl` at all: picking it guarantees the dependency is
     * reported unmet, while a version the constraint dislikes at least stages and boots. This is the
     * `306612` / Fabric-API refusal fixed on 2026-09-04, one layer down — and a staging refusal publishes
     * `ERROR` over whatever decisive verdict the store held.
     */
    @Test
    fun anObtainableFileBeatsALockedOneThatSatisfiesTheConstraint() {
        val files = listOf(
            lockedAtVersion("lib-1.5.0.jar", "1.5.0"),
            obtainableAtVersion("lib-0.9.0.jar", "0.9.0")
        )

        Assertions.assertEquals(
            "lib-0.9.0.jar",
            BootCandidateSelector.pickDependencyFile(files, "Fabric", "1.20.1", ">=1.0")?.fileName,
            "the locked file satisfies the constraint but cannot be fetched; an unmet dependency refuses the boot"
        )
    }

    /** With both obtainable, the constraint decides again — obtainability narrows, it does not override. */
    @Test
    fun betweenTwoObtainableFilesTheConstraintStillDecides() {
        val files = listOf(
            obtainableAtVersion("lib-0.9.0.jar", "0.9.0"),
            obtainableAtVersion("lib-1.5.0.jar", "1.5.0")
        )

        Assertions.assertEquals(
            "lib-1.5.0.jar",
            BootCandidateSelector.pickDependencyFile(files, "Fabric", "1.20.1", ">=1.0")?.fileName,
            "obtainability is the stronger preference, not the only one"
        )
    }

    /**
     * When **no tagged version** satisfies the jar, fall back to the newest real Minecraft **release** the
     * jar's own descriptor accepts, and boot there.
     *
     * Griefed's call, 2026-09-06, from the live grinder's ERROR rows: `moonlight-1.20.4-2.9.9-forge.jar` is
     * tagged 1.20.4 and only 1.20.4, while its descriptor declares `[1.20,1.20.2)`. Platform and jar share
     * no version at all, so `newestVersionSatisfying` returns null and the candidate was refused outright —
     * "bump the version to the one specced in the JAR, then run the grind."
     *
     * The jar is the better authority here, and not merely a different one: the *loader* enforces this
     * range at runtime, so booting inside it is what actually gets the mod loaded, while booting at a
     * version the author only ticked on a web form gets it rejected by FML before it runs. The file itself
     * is unchanged — only the pack's Minecraft version moves.
     */
    @Test
    fun fallsBackToTheNewestReleaseTheJarItselfAccepts() {
        val releases = listOf("1.21.1", "1.20.6", "1.20.4", "1.20.1", "1.20", "1.19.2")

        Assertions.assertEquals(
            "1.20.1",
            BootCandidateSelector.newestReleaseSatisfying("[1.20,1.20.2)", releases) { true },
            "1.20.1 is the newest release inside the range moonlight's descriptor declares"
        )
    }

    /** The loader gate still applies — a version no loader build exists for is not a place to boot. */
    @Test
    fun skipsAReleaseTheLoaderCannotBootAndTakesTheNextOne() {
        val releases = listOf("1.21.1", "1.20.6", "1.20.4", "1.20.1", "1.20")

        Assertions.assertEquals(
            "1.20",
            BootCandidateSelector.newestReleaseSatisfying("[1.20,1.20.2)", releases) { it != "1.20.1" }
        )
    }

    /** Nothing in the range means nothing to bump to, and the caller keeps its honest refusal. */
    @Test
    fun answersNothingWhenNoReleaseSatisfiesTheJar() {
        Assertions.assertNull(
            BootCandidateSelector.newestReleaseSatisfying("[1.16,1.17)", listOf("1.21.1", "1.20.4")) { true }
        )
    }

    /**
     * A constraint that accepts everything must not silently relocate a pack to the newest Minecraft in
     * existence. `VersionConstraint` deliberately accepts anything it cannot read, so an unreadable
     * descriptor would otherwise bump every candidate to the top of the release list.
     */
    @Test
    fun refusesToBumpOnAConstraintThatConstrainsNothing() {
        val releases = listOf("1.21.1", "1.20.4")

        for (constraint in listOf("", "   ", "*", "not a version at all")) {
            Assertions.assertNull(
                BootCandidateSelector.newestReleaseSatisfying(constraint, releases) { true },
                "'$constraint' bounds nothing and must not move the pack"
            )
        }
    }

    /**
     * **A file carrying no loader tag is *unknown*, not incompatible.**
     *
     * CurseForge had no modloader facet before Minecraft 1.13 — everything was Forge, so nothing was
     * tagged — and `pickForLoader` requires `loader in it.loaders`, which no empty set satisfies. The
     * dependency was therefore unpickable and the boot refused.
     *
     * Measured against the live CurseForge API on 2026-09-06 with Griefed's key, which is what found this:
     * `modtweaker` declares dependency `253211`, that resolves to **mtlib**, and all **7** of its obtainable
     * 1.12.2 files carry `loaders=[]`. `pickDependencyFile(files, "Forge", "1.12.2")` returned nothing, and
     * the refusal read *"Required dependency unavailable for Forge / Minecraft 1.12.2: mtlib"* — the exact
     * line Griefed reported. It is not rare: of the projects sampled, `mtlib` is 15/15 untagged,
     * `iron-chests` 106/138, `waystones` 70/494 and `crafttweaker` 28/500, essentially all pre-1.13, plus a
     * handful of modern stragglers (`journeymap`, 5 files at 26.1.2).
     *
     * Untagged is the **last** resort, after the exact loader and the Quilt-to-Fabric fallback, so a
     * properly tagged file always wins and this can only add a pick where there was none.
     */
    @Test
    fun fallsBackToAnUntaggedFileWhenNothingCarriesTheLoader() {
        val mtlib = listOf(
            file("MTLib-3.0.7.jar", emptySet(), setOf("1.12.2")),
            file("MTLib-3.0.6.jar", emptySet(), setOf("1.12.2"))
        )

        Assertions.assertEquals(
            "MTLib-3.0.7.jar",
            BootCandidateSelector.pickDependencyFile(mtlib, "Forge", "1.12.2")?.fileName,
            "every file mtlib publishes for 1.12.2 is untagged; refusing them refuses the boot"
        )
    }

    /** The Minecraft version is still exact — untagged excuses the loader, never the version. */
    @Test
    fun anUntaggedFileStillHasToMatchTheMinecraftVersion() {
        val wrongVersion = listOf(file("MTLib-3.0.7.jar", emptySet(), setOf("1.12.2")))

        Assertions.assertNull(BootCandidateSelector.pickDependencyFile(wrongVersion, "Forge", "1.20.1"))
    }

    /** A tagged file wins over an untagged one, so this only ever adds a pick where there was none. */
    @Test
    fun prefersATaggedFileOverAnUntaggedOne() {
        val mixed = listOf(
            file("untagged.jar", emptySet(), setOf("1.20.1")),
            file("tagged-forge.jar", setOf("Forge"), setOf("1.20.1"))
        )

        Assertions.assertEquals(
            "tagged-forge.jar",
            BootCandidateSelector.pickDependencyFile(mixed, "Forge", "1.20.1")?.fileName
        )
    }

    /**
     * And a file tagged for a *different* loader is still refused — untagged means "the author told us
     * nothing", which is not the same as "the author told us this is Fabric".
     */
    @Test
    fun stillRefusesAFileTaggedForAnotherLoader() {
        val fabricOnly = listOf(file("something-fabric.jar", setOf("Fabric"), setOf("1.20.1")))

        Assertions.assertNull(
            BootCandidateSelector.pickDependencyFile(fabricOnly, "Forge", "1.20.1"),
            "a Fabric-tagged jar is a statement, and it says this is not a Forge file"
        )
    }

    /**
     * **The candidate half of the untagged-loader rule** (Griefed's call, 2026-09-06). A project whose files
     * are *all* untagged was never selected as a candidate at all, so it was never ground under any loader.
     *
     * Measured against the live CurseForge API: `pickBootableCandidate(mtlib.files, "Forge")` returned
     * **nothing** — all 15 of its files carry `loaders=[]`, because CurseForge had no modloader facet before
     * Minecraft 1.13. `iron-chests` and `waystones` escaped only because their *newer* files are tagged.
     *
     * **Why widening it here is safe, and it is a different argument than for a dependency.** Picking an
     * untagged file for the wrong loader could in principle stage a jar that loader ignores, boot cleanly
     * and publish a false `CLEAR` — "proven server-safe" for a mod that never loaded. Two things prevent it.
     * `loaderVersionAvailable` gates the dominant case: untagged files are overwhelmingly pre-1.13, where
     * Fabric and Quilt have no builds at all, so only Forge is reachable and untagged *means* Forge. And for
     * anything newer, `BootVerifier.refuseForSelfDeclaration` reads the downloaded jar's own descriptor
     * before the boat is launched and refuses a jar carrying only another loader's descriptor — the
     * *"carries only Forge descriptor(s), so it is not a NeoForge mod"* refusal already visible in the live
     * store. The cost of being wrong is therefore a refused attempt, not a wrong verdict.
     */
    @Test
    fun grindsAProjectWhoseFilesAreAllUntagged() {
        val mtlib = listOf(
            file("MTLib-3.0.7.jar", emptySet(), setOf("1.12.2")),
            file("MTLib-3.0.5.jar", emptySet(), setOf("1.12", "1.12.1", "1.12.2"))
        )

        val candidate = BootCandidateSelector.pickBootableCandidate(mtlib, "Forge") { true }

        Assertions.assertNotNull(candidate, "an all-untagged project must still be ground")
        Assertions.assertEquals("MTLib-3.0.7.jar", candidate!!.first.fileName)
        Assertions.assertEquals("1.12.2", candidate.second, "and still on the newest version it publishes")
    }

    /** A tagged file wins, so this can only add a candidate where there was none. */
    @Test
    fun prefersATaggedCandidateOverAnUntaggedOne() {
        val mixed = listOf(
            file("untagged-newer.jar", emptySet(), setOf("1.21.1")),
            file("tagged-older.jar", setOf("Forge"), setOf("1.20.1"))
        )

        val candidate = BootCandidateSelector.pickBootableCandidate(mixed, "Forge") { true }

        Assertions.assertEquals(
            "tagged-older.jar", candidate?.first?.fileName,
            "a stated loader outranks a newer file that states nothing"
        )
    }

    /** A file tagged for another loader is still never a candidate — an empty set is not a Fabric tag. */
    @Test
    fun stillRefusesACandidateTaggedForAnotherLoader() {
        val fabricOnly = listOf(file("something-fabric.jar", setOf("Fabric"), setOf("1.20.1")))

        Assertions.assertNull(BootCandidateSelector.pickBootableCandidate(fabricOnly, "Forge") { true })
    }

    /**
     * The loader-availability gate still applies to the untagged fallback, and it is what keeps a pre-1.13
     * untagged file — which is a Forge file — from being ground under Fabric, which has no build there.
     */
    @Test
    fun theLoaderGateStillAppliesToAnUntaggedCandidate() {
        val ancient = listOf(file("MTLib-3.0.7.jar", emptySet(), setOf("1.12.2")))

        Assertions.assertNull(
            BootCandidateSelector.pickBootableCandidate(ancient, "Fabric") { false },
            "Fabric has no build for 1.12.2, so there is nothing to boot"
        )
    }
}
