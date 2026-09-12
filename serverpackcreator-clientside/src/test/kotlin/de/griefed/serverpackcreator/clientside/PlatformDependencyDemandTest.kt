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
 * Pins that a **platform-declared dependency the jar itself never asks for cannot refuse the boot**.
 *
 * A platform's dependency list is a self-report attached to a project by its author, and CurseForge
 * carries one list per file that authors routinely maintain per *project*. The jar's descriptor is the
 * thing the loader actually enforces. Where the two disagree, the descriptor wins — the same relationship
 * `boot-rules.default.json` already encodes one layer up, where the console decides and the metadata only
 * declares.
 *
 * **The live case, `CurseForge/aether` on Forge / Minecraft 1.20.2, 2026-09-08.** The grinder published
 * *"Required dependency unavailable … owo-lib (nothing published for this loader and Minecraft version)"*.
 * Both halves of that are true — owo-lib publishes no Forge build at all — and the conclusion is still
 * wrong, because **the Forge/NeoForge jar's `mods.toml` does not list owo-lib**; only the Fabric and Quilt
 * builds of Aether need it. CurseForge's relations put it on the file anyway, and the candidate wore an
 * ERROR for a dependency it does not have.
 *
 * **The matching is deliberately fuzzy, and the direction is what makes that safe.** An unstageable project
 * cannot be downloaded, so its declared mod id is unknowable here and the comparison has to run between the
 * project *slug* and the ids the descriptor names. Both mistakes cost at most one container: a false
 * "demanded" refuses a boot that might have run (today's behaviour, so no regression), and a false "not
 * demanded" spends a boot that the loader then refuses, which yields INCONCLUSIVE — never a wrong sideness
 * verdict either way.
 *
 * @author Griefed
 */
internal class PlatformDependencyDemandTest {

    private fun project(slug: String) = ProjectFiles(
        platform = "CurseForge",
        slug = slug,
        projectUrl = "https://www.curseforge.com/minecraft/mc-mods/$slug",
        clientSide = DeclaredSupport.UNKNOWN,
        serverSide = DeclaredSupport.UNKNOWN,
        files = emptyList()
    )

    /** The reported case: Aether's Forge descriptor names its own libraries, and owo-lib is not among them. */
    @Test
    fun aDependencyTheDescriptorNeverNamesIsNotDemanded() {
        Assertions.assertFalse(
            PlatformDependencyDemand.isDemanded(
                declaredIds = setOf("aether", "cumulus_menus", "nitrogen_internals"),
                project = project("owo-lib")
            ),
            "the Forge jar does not ask for owo-lib; only the Fabric build does"
        )
    }

    /** The ordinary case: the descriptor names it outright. */
    @Test
    fun aDependencyTheDescriptorNamesIsDemanded() {
        Assertions.assertTrue(
            PlatformDependencyDemand.isDemanded(setOf("jei", "patchouli"), project("jei"))
        )
    }

    /**
     * **A mod id is not a slug, and the difference is usually a loader suffix.** `kleeslabs` declares
     * `balm-fabric` while the project is published as `balm`; refusing to see that as the same thing would
     * turn every real missing-dependency refusal into a wasted container.
     */
    @Test
    fun aLoaderSuffixedIdStillNamesTheProject() {
        Assertions.assertTrue(
            PlatformDependencyDemand.isDemanded(setOf("balm-fabric"), project("balm"))
        )
        Assertions.assertTrue(
            PlatformDependencyDemand.isDemanded(setOf("ftblibrary"), project("ftb-library-forge")),
            "punctuation differs between the two vocabularies and means nothing"
        )
    }

    /**
     * An id the registry already knows resolves through it rather than through spelling: `fabric` is Fabric
     * API, whatever the project is called on the platform in question.
     */
    @Test
    fun anIdTheRegistryKnowsIsMatchedByItsRef() {
        Assertions.assertTrue(
            PlatformDependencyDemand.isDemanded(
                declaredIds = setOf("fabric"),
                project = project("fabric-api").copy(platform = "Modrinth")
            )
        )
    }

    /**
     * **An unreadable descriptor demands everything**, which is today's behaviour and the conservative
     * direction: with nothing to compare against, the platform's claim is all there is.
     */
    @Test
    fun anUnreadableDescriptorLeavesThePlatformInCharge() {
        Assertions.assertTrue(PlatformDependencyDemand.isDemanded(declaredIds = null, project = project("owo-lib")))
    }

    /**
     * A descriptor that declares no dependencies at all is *read* and says nothing is needed. That is a
     * statement, not an absence — and the cost of believing it is one boot, which then produces real
     * evidence instead of a refusal.
     */
    @Test
    fun aDescriptorDeclaringNothingDemandsNothing() {
        Assertions.assertFalse(PlatformDependencyDemand.isDemanded(declaredIds = emptySet(), project = project("owo-lib")))
    }

    /** Short ids must not match by accident — `owo` inside `owo-lib` is a real pair, `ae` inside `aether` is not. */
    @Test
    fun aTwoLetterIdDoesNotMatchEverything() {
        Assertions.assertFalse(
            PlatformDependencyDemand.isDemanded(setOf("ae"), project("aether")),
            "a fragment that short would claim half the catalogue"
        )
    }

    /**
     * **The range lives in the jar, not in the ref.** `ModFile.requiredDependencies` carries opaque platform
     * ids and no version, so the platform route picked the newest build for the Minecraft version even where
     * the candidate had demanded a specific one. Measured on the public grinder 2026-09-11:
     * `cobblemon-additions` demands `cobblemon >=1.7.1` and was staged `Cobblemon-fabric-1.6.1+1.21.1`.
     */
    @Test
    fun theRangeTheJarDeclaresForAProjectIsFound() {
        val cobblemon = project("cobblemon")

        Assertions.assertEquals(
            ">=1.7.1",
            PlatformDependencyDemand.demandedConstraint(
                listOf(ModDependency("cobblemon", versionConstraint = ">=1.7.1")), cobblemon
            )
        )
    }

    /** Named without a range, or not named at all, is the same answer: nothing to narrow by. */
    @Test
    fun aDependencyNamedWithoutARangeNarrowsNothing() {
        val cobblemon = project("cobblemon")

        Assertions.assertNull(
            PlatformDependencyDemand.demandedConstraint(listOf(ModDependency("cobblemon")), cobblemon)
        )
        Assertions.assertNull(
            PlatformDependencyDemand.demandedConstraint(listOf(ModDependency("something-else", versionConstraint = "1.0")), cobblemon)
        )
        Assertions.assertNull(PlatformDependencyDemand.demandedConstraint(null, cobblemon))
    }

    /**
     * The same fuzzy id-to-slug match `isDemanded` makes, because one matcher is the point: a mod id is
     * routinely the slug plus or minus a loader suffix.
     */
    @Test
    fun theRangeIsFoundThroughTheSameFuzzyMatchAsTheDemandItself() {
        Assertions.assertEquals(
            ">=5.0",
            PlatformDependencyDemand.demandedConstraint(
                listOf(ModDependency("balm-fabric", versionConstraint = ">=5.0")), project("balm")
            )
        )
    }

}
