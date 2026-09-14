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

import de.griefed.serverpackcreator.api.config.SupportedModloaders
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Pins [BootCandidateSelector.pickGrindTargets] — one grind per Minecraft version-line, each under a single
 * loader chosen by [BootCandidateSelector.LOADER_PRIORITY].
 *
 * The fixture is `CurseForge/aether`, read from the live API on 2026-09-11, because every property worth
 * guarding is visible in that one project: a file tagged for two loaders at once, a stable old build
 * alongside beta newer ones, and three Minecraft lines that were previously ground as three *loaders* all
 * landing on the newest two.
 *
 * @author Griefed
 */
internal class GrindTargetSelectionTest {

    /** Everything boots; the availability gate has its own guards below. */
    private val anythingBoots: (String, String) -> Boolean = { _, _ -> true }

    /**
     * `CurseForge/aether`'s real shape, newest-upload first as the platform returns it.
     *
     * `aether-1.20.1-1.5.2-neoforge.jar` really is tagged `['NeoForge', '1.20.1', 'Forge']` — one file, two
     * loaders — and the 1.20.1 Fabric builds really are betas while 1.21.1 and 1.12.2 are releases.
     */
    private val aether = listOf(
        ModFile("aether-1.21.1-1.5.10-neoforge.jar", setOf("NeoForge"), setOf("1.21.1"), "https://cdn/a", null, emptyList()),
        ModFile("aether-1.21.1-1.5.11-fabric.jar", setOf("Fabric"), setOf("1.21.1"), "https://cdn/b", null, emptyList()),
        ModFile("aether-1.12.2-v1.5.4.1.jar", setOf("Forge"), setOf("1.12.2"), "https://cdn/c", null, emptyList()),
        ModFile(
            "aether-1.20.1-1.5.2-beta.9-fabric.jar", setOf("Fabric"), setOf("1.20.1"), "https://cdn/d", null,
            emptyList(), channel = ReleaseChannel.BETA
        ),
        ModFile(
            "aether-1.20.1-1.5.2-neoforge.jar", setOf("NeoForge", "Forge"), setOf("1.20.1"), "https://cdn/e", null,
            emptyList()
        )
    )

    /** A readable `(line, loader, file, version)` rendering, so a failure says what actually differed. */
    private fun render(targets: List<BootCandidateSelector.GrindTarget>) =
        targets.map { "${it.minecraftLine} ${it.loader} ${it.file.fileName} @ ${it.minecraftVersion}" }

    /**
     * **The reported case, end to end.** Under the loader axis aether cost three boots — Fabric and NeoForge
     * both on 1.21.1, Forge on 1.20.1 — and its 1.12.2 build, a wholly separate codebase, was never booted.
     * Under the line axis the same project costs three boots that ask three different questions.
     */
    @Test
    fun aetherIsGroundOncePerMinecraftLineUnderOneLoaderEach() {
        Assertions.assertEquals(
            listOf(
                "1.21 NeoForge aether-1.21.1-1.5.10-neoforge.jar @ 1.21.1",
                "1.20 NeoForge aether-1.20.1-1.5.2-neoforge.jar @ 1.20.1",
                "1.12 Forge aether-1.12.2-v1.5.4.1.jar @ 1.12.2"
            ),
            render(BootCandidateSelector.pickGrindTargets(aether, MinecraftLinePolicy(), anythingBoots))
        )
    }

    /**
     * The priority is applied **per line**, not once per project: 1.20 has a NeoForge build and 1.12 does
     * not, so the same project is ground under two different loaders.
     */
    @Test
    fun theLoaderIsChosenPerLineAndNotOncePerProject() {
        val targets = BootCandidateSelector.pickGrindTargets(aether, MinecraftLinePolicy(), anythingBoots)
        Assertions.assertEquals(
            mapOf("1.21" to "NeoForge", "1.20" to "NeoForge", "1.12" to "Forge"),
            targets.associate { it.minecraftLine to it.loader }
        )
    }

    /** The whole order, asserted on one line whose files carry every loader. */
    @Test
    fun theLoaderOrderIsNeoForgeThenForgeThenFabricThenQuiltThenLegacyFabric() {
        val everyLoader = BootCandidateSelector.LOADER_PRIORITY.map {
            ModFile("mod-$it.jar", setOf(it), setOf("1.20.1"), "https://cdn/$it", null, emptyList())
        }
        var remaining = everyLoader
        val chosen = mutableListOf<String>()
        while (remaining.isNotEmpty()) {
            val target = BootCandidateSelector.pickGrindTargets(remaining, MinecraftLinePolicy(), anythingBoots).single()
            chosen.add(target.loader)
            remaining = remaining.filterNot { it.fileName == target.file.fileName }
        }
        Assertions.assertEquals(BootCandidateSelector.LOADER_PRIORITY, chosen)
    }

    /**
     * **Every supported modloader must carry a priority.** One missing is silently never ground — the same
     * failure shape as a verdict missing from the grinder's rank, which sorts behind everything without
     * saying so. Asserted against SPC's own list rather than a copy of it.
     */
    @Test
    fun everySupportedModloaderHasAPriority() {
        Assertions.assertEquals(
            SupportedModloaders.names.toSortedSet(),
            BootCandidateSelector.LOADER_PRIORITY.toSortedSet(),
            "a loader SPC supports but this order omits is never ground, and nothing reports it"
        )
    }

    /**
     * The availability gate is the loader's, not the project's: NeoForge publishes nothing for 1.12.2, so
     * the line falls through to Forge even though NeoForge outranks it.
     */
    @Test
    fun aLoaderWithNoBuildForTheLineFallsThroughToTheNext() {
        val bothLoaders = listOf(
            ModFile("mod-forge.jar", setOf("Forge"), setOf("1.12.2"), "https://cdn/f", null, emptyList()),
            ModFile("mod-neo.jar", setOf("NeoForge"), setOf("1.12.2"), "https://cdn/n", null, emptyList())
        )
        val target = BootCandidateSelector.pickGrindTargets(bothLoaders, MinecraftLinePolicy()) { loader, _ ->
            loader != "NeoForge"
        }.single()
        Assertions.assertEquals("Forge" to "mod-forge.jar", target.loader to target.file.fileName)
    }

    /** A line nothing can boot is dropped, not reported — there is no run to have a verdict about. */
    @Test
    fun aLineNoLoaderCanBootIsDropped() {
        val onlyOldLine = listOf(
            ModFile("mod.jar", setOf("Fabric"), setOf("1.7.10"), "https://cdn/o", null, emptyList()),
            ModFile("mod-new.jar", setOf("Fabric"), setOf("1.21.1"), "https://cdn/p", null, emptyList())
        )
        val targets = BootCandidateSelector.pickGrindTargets(
            onlyOldLine, MinecraftLinePolicy(newestCount = 5, anchors = emptySet())
        ) { _, minecraftVersion -> minecraftVersion != "1.7.10" }
        Assertions.assertEquals(listOf("1.21 Fabric mod-new.jar @ 1.21.1"), render(targets))
    }

    /**
     * **A file tagged across lines must not boot outside the line it was selected for.** One published file
     * routinely carries several Minecraft versions, and the pick takes the newest it is *shown* — so handing
     * the whole set to a 1.20 line would boot it at 1.21, which is precisely what a per-line axis exists to
     * prevent.
     */
    @Test
    fun aFileTaggedAcrossLinesBootsAtTheLinesOwnVersion() {
        val spanning = listOf(
            ModFile("mod-wide.jar", setOf("Forge"), setOf("1.20.1", "1.21.1"), "https://cdn/w", null, emptyList())
        )
        Assertions.assertEquals(
            listOf("1.21 Forge mod-wide.jar @ 1.21.1", "1.20 Forge mod-wide.jar @ 1.20.1"),
            render(BootCandidateSelector.pickGrindTargets(spanning, MinecraftLinePolicy(), anythingBoots))
        )
    }

    /**
     * **A stated loader wins across loaders, not only within one.** An untagged file (CurseForge published
     * no modloader facet before Minecraft 1.13) matches every loader, so a single pass down the priority
     * order would hand it to NeoForge while Forge had a file its author actually tagged — a jar staged for a
     * loader that will ignore it, which can boot cleanly and publish a false `CLEAR`.
     */
    @Test
    fun aTaggedFileBeatsAnUntaggedOneEvenForALowerPriorityLoader() {
        val mixed = listOf(
            ModFile("untagged.jar", emptySet(), setOf("1.20.1"), "https://cdn/u", null, emptyList()),
            ModFile("tagged-forge.jar", setOf("Forge"), setOf("1.20.1"), "https://cdn/t", null, emptyList())
        )
        val target = BootCandidateSelector.pickGrindTargets(mixed, MinecraftLinePolicy(), anythingBoots).single()
        Assertions.assertEquals(
            "Forge" to "tagged-forge.jar", target.loader to target.file.fileName,
            "an empty loader set is the absence of a statement, not a claim on every loader"
        )
    }

    /**
     * With nothing tagged there is still a grind — that is what the second pass is for, and it is what made
     * an all-untagged project such as mtlib grindable at all.
     */
    @Test
    fun anAllUntaggedLineIsStillGround() {
        val untagged = listOf(ModFile("mtlib.jar", emptySet(), setOf("1.12.2"), "https://cdn/m", null, emptyList()))
        val target = BootCandidateSelector.pickGrindTargets(untagged, MinecraftLinePolicy()) { loader, _ ->
            loader == "Forge"
        }.single()
        Assertions.assertEquals("Forge" to "mtlib.jar", target.loader to target.file.fileName)
    }

    /**
     * The channel preference survives the narrowing: within a line a stable build still beats a beta, which
     * is the rule `ReleaseChannelPreferenceTest` owns and this must not quietly undo.
     */
    @Test
    fun aStableBuildStillBeatsABetaInsideALine() {
        val line = listOf(
            ModFile(
                "beta-1.20.2.jar", setOf("Forge"), setOf("1.20.2"), "https://cdn/b", null, emptyList(),
                channel = ReleaseChannel.BETA
            ),
            ModFile("stable-1.20.1.jar", setOf("Forge"), setOf("1.20.1"), "https://cdn/s", null, emptyList())
        )
        Assertions.assertEquals(
            listOf("1.20 Forge stable-1.20.1.jar @ 1.20.1"),
            render(BootCandidateSelector.pickGrindTargets(line, MinecraftLinePolicy(), anythingBoots))
        )
    }

    /** A project publishing nothing yields nothing. */
    @Test
    fun aProjectWithNoFilesYieldsNoTargets() {
        Assertions.assertEquals(
            emptyList<BootCandidateSelector.GrindTarget>(),
            BootCandidateSelector.pickGrindTargets(emptyList(), MinecraftLinePolicy(), anythingBoots)
        )
    }

    /** The policy decides how many lines, and the selection honours it rather than grinding everything. */
    @Test
    fun thePolicyBoundsHowManyLinesAreGround() {
        Assertions.assertEquals(
            listOf("1.21 NeoForge aether-1.21.1-1.5.10-neoforge.jar @ 1.21.1"),
            render(
                BootCandidateSelector.pickGrindTargets(
                    aether, MinecraftLinePolicy(newestCount = 1, anchors = emptySet()), anythingBoots
                )
            )
        )
    }
}
