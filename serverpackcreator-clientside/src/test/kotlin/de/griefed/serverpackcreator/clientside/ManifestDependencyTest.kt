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

    /**
     * **A mod declares Fabric API several times over, and it must still be staged once.**
     *
     * Fabric API ships as ~45 modules and a descriptor depends on the modules, so a single mod routinely
     * names five or eight of them. Every one now resolves to the same project, which makes this the exact
     * shape B6 caught on `amblekit`: one jar reachable under several names, counted more than once, eating
     * the `MAX_INJECTED_DEPENDENCIES` budget and refusing packs that were within it — scored INCONCLUSIVE,
     * so it reads as a mod that could not be tested rather than as a bookkeeping bug.
     */
    @Test
    fun theManyModulesOfFabricApiCollapseToOneDependency() {
        val requirements = listOf(
            requirement("fabric-resource-loader-v0"),
            requirement("fabric-block-getter-api-v2"),
            requirement("fabric-rendering-fluids-v1"),
            requirement("fabric-networking-api-v1"),
            requirement("fabric-api-base"),
            requirement("cloth-config")
        )

        val stageable = BootVerifier.stageableRequirements(
            requirements, alreadyResolved = setOf("fabric-api")
        ) { id -> KnownModIds.refFor(id, "Modrinth") }

        Assertions.assertEquals(
            listOf("cloth-config"), stageable.map { it.modID },
            "the platform already staged Fabric API, so none of its modules may be staged again"
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
    /**
     * The same jar staged twice must count **once** against the cap.
     *
     * A dependency is reachable under two different-but-equally-valid identifiers — the platform ref the
     * author linked (`P7dR8mSH`) and the mod id its own manifest declares (`fabric`, mapped by
     * `ModIdRegistry` to the slug `fabric-api`). `stageableRequirements` dedupes by *ref*, so it cannot
     * see that those two resolve to one file; only the file name can.
     *
     * Observed in B6's merge gate 2026-08-30: `amblekit/Fabric` recorded
     * `fabric-api-0.100.8+1.20.6.jar` twice. Cosmetic in the report, but not against the cap — double
     * counting refuses a pack that is within it, and `refuseForTooManyDependencies` scores a refusal
     * INCONCLUSIVE, so the mod silently stops being verified.
     */
    @Test
    fun aJarStagedUnderTwoRefsCountsOnceAgainstTheCap() {
        // Seven distinct jars, each recorded twice: fourteen entries, but a seven-dependency pack. The
        // duplicated total is what pushes it past the cap of twelve, which is the whole defect.
        val distinctFiles = (1..7).map { "dep-$it.jar" }
        val everyOneDuplicated = distinctFiles + distinctFiles

        Assertions.assertNull(
            BootVerifier.refuseForTooManyDependencies(everyOneDuplicated, "Fabric", "1.20.6"),
            "14 entries naming only 7 distinct jars is a 7-dependency pack and must not be refused"
        )
    }

    /** The cap still bites on genuinely distinct dependencies, or it would protect nothing. */
    @Test
    fun theCapStillRefusesGenuinelyLargePacks() {
        val tooMany = (1..13).map { "dep-$it.jar" }

        Assertions.assertNotNull(
            BootVerifier.refuseForTooManyDependencies(tooMany, "Fabric", "1.20.6"),
            "13 distinct dependencies exceeds the cap of 12 and must still refuse"
        )
    }
    /** A project carrying exactly [files], enough for the planner to pick from. */
    private fun project(vararg files: ModFile) = ProjectFiles(
        platform = "CurseForge", slug = "fabric-api", projectUrl = "https://example.invalid/fabric-api",
        clientSide = DeclaredSupport.UNKNOWN, serverSide = DeclaredSupport.UNKNOWN, files = files.toList()
    )

    private fun file(name: String, loaders: Set<String>, mc: Set<String>) =
        ModFile(name, loaders, mc, "https://cdn/$name", null, emptyList())

    /**
     * **The bug this exists for.** A manifest id the registry mapped, whose project resolved, and which
     * then had no usable file, must REFUSE the boot — not be filed as a guess that missed.
     *
     * Observed live 2026-08-30 on `CurseForge/attributefix` at Minecraft 1.21.11: its manifest declares
     * `Depends on 'fabric-api' (-∞, ∞)`, nothing was staged for it, and the boot ran anyway. Quilt Loader
     * then refused the pack with "AttributeFix requires any version of fabric-api, which is missing!" and
     * the *candidate* wore the verdict — the exact failure the refusal path exists to prevent.
     *
     * The split this restores is the one this file documents: mapped and resolved, then failed to stage, is
     * a case we chose to trust, so failing to honour it is a real gap.
     */
    @Test
    fun aMappedDependencyWithNoUsableFileRefusesRatherThanBeingFiledAsAGuess() {
        val plan = BootVerifier.planManifestDependency(
            ModDependency("fabric-api"), "Quilt", "1.21.11",
            refFor = { "306612" },
            // Resolves, but publishes nothing for this Minecraft version — the live shape.
            resolveRef = { project(file("fabric-api-0.100.8+1.20.6.jar", setOf("Fabric"), setOf("1.20.6"))) }
        )

        Assertions.assertEquals(
            ManifestDependencyPlan.Unsatisfied("fabric-api"), plan,
            "a dependency we mapped and resolved and then could not stage must refuse the boot"
        )
    }

    /** An id that maps to no project at all stays a guess, and never refuses. */
    @Test
    fun anUnmappableIdIsStillOnlyAGuess() {
        val plan = BootVerifier.planManifestDependency(
            ModDependency("some-bundled-thing"), "Quilt", "1.21.11",
            refFor = { null },
            resolveRef = { error("must not be consulted when nothing maps") }
        )

        Assertions.assertEquals(ManifestDependencyPlan.Unmapped("some-bundled-thing"), plan)
    }

    /** Mapped, but this platform does not carry it: also a guess that missed, so also never fatal. */
    @Test
    fun aMappedIdThePlatformDoesNotCarryIsAGuessToo() {
        val plan = BootVerifier.planManifestDependency(
            ModDependency("fabric-api"), "Quilt", "1.21.11",
            refFor = { "306612" },
            resolveRef = { null }
        )

        Assertions.assertEquals(ManifestDependencyPlan.Unmapped("fabric-api"), plan)
    }

    /** The happy path still picks a file, and picks one that fits the pack. */
    @Test
    fun aResolvableDependencyIsPlannedForStaging() {
        val fits = file("fabric-api-0.141.6+1.21.11.jar", setOf("Fabric"), setOf("1.21.11"))
        val plan = BootVerifier.planManifestDependency(
            ModDependency("fabric-api"), "Quilt", "1.21.11",
            refFor = { "306612" },
            resolveRef = { project(file("fabric-api-0.100.8+1.20.6.jar", setOf("Fabric"), setOf("1.20.6")), fits) }
        )

        Assertions.assertEquals(ManifestDependencyPlan.Stage("306612", fits), plan)
    }
}
