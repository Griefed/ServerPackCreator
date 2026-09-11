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

import de.griefed.serverpackcreator.api.ApiWrapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Pins that `BootVerifier.verify(project, target, …)` boots the combination the **caller** chose, rather
 * than re-running selection and booting whatever is newest.
 *
 * That is the whole point of the target entry point: the per-line axis asks a separate question of each
 * Minecraft era, and an entry point that quietly re-selected would answer every one of them with the newest
 * line — which is exactly the behaviour it replaces.
 *
 * Both are *executed*: staging is observed through the file it asks the downloader for, and the downloader
 * answers `null`, so no server is ever launched.
 *
 * @author Griefed
 */
internal class GrindTargetBootTest {

    private val apiWrapper = ApiWrapper.api(File("build/resources/test/serverpackcreator.properties"))
    private val resolver = LoaderVersionResolver(apiWrapper.versionMeta)

    /** Every real Minecraft release Forge publishes a build for, oldest first, from SPC's own metadata. */
    private val forgeReleases = apiWrapper.versionMeta.minecraft.serverReleases()
        .map { it.minecraftVersion }
        .filter { resolver.latest("Forge", it) != null }
        .sortedWith(BootCandidateSelector.minecraftComparator)

    private val olderRelease = forgeReleases.first()
    private val newerRelease = forgeReleases.last()

    private val olderFile =
        ModFile("themod-$olderRelease.jar", setOf("Forge"), setOf(olderRelease), "https://cdn/old.jar", null, emptyList())
    private val newerFile =
        ModFile("themod-$newerRelease.jar", setOf("Forge"), setOf(newerRelease), "https://cdn/new.jar", null, emptyList())

    private val project = ProjectFiles(
        platform = "Modrinth",
        slug = "themod",
        projectUrl = "https://modrinth.com/mod/themod",
        clientSide = DeclaredSupport.UNKNOWN,
        serverSide = DeclaredSupport.UNKNOWN,
        files = listOf(newerFile, olderFile)
    )

    /** The platform is only consulted for dependency resolution, which these tests never reach. */
    private val unusedPlatform = object : ModPlatform {
        override val name: String = "unused"
        override fun handles(projectUrl: String): Boolean = false
        override fun resolve(projectUrl: String): ProjectFiles = error("resolve must not be called")
        override fun resolveDependency(nativeRef: String, minecraftVersion: String?): ProjectFiles? =
            error("resolveDependency must not be called")
    }

    private fun verifier(workDir: File, requested: MutableList<String>) = BootVerifier(
        apiWrapper = apiWrapper,
        platform = unusedPlatform,
        httpDownloader = { modFile, _ -> requested.add(modFile.fileName); null },
        loaderVersionPolicy = resolver,
        workDirectory = workDir
    )

    private fun assumeTwoLines() = Assumptions.assumeTrue(
        forgeReleases.size > 1 &&
            BootCandidateSelector.minecraftLine(olderRelease) != BootCandidateSelector.minecraftLine(newerRelease),
        "the shipped manifest must offer two Forge-capable releases on different Minecraft lines"
    )

    /**
     * The target is honoured verbatim — the *older* line's file is staged, although a newer one exists and
     * selection would have preferred it.
     */
    @Test
    fun aTargetIsBootedAsChosenRatherThanReSelected(@TempDir workDir: File) {
        assumeTwoLines()
        val requested = mutableListOf<String>()
        val target = BootCandidateSelector.GrindTarget(
            BootCandidateSelector.minecraftLine(olderRelease), "Forge", olderFile, olderRelease
        )

        val outcome = verifier(workDir, requested).verify(project, target)

        Assertions.assertEquals(listOf(olderFile.fileName), requested)
        Assertions.assertTrue(
            outcome.detail?.contains(olderFile.fileName) == true,
            "the refusal has to be about the file the caller asked for, was: ${outcome.detail}"
        )
    }

    /**
     * The loader entry point is **unchanged**, and asserting it beside the target one is what makes the
     * previous test mean something: the two genuinely diverge, so honouring the target is a decision rather
     * than a coincidence of this fixture.
     */
    @Test
    fun theLoaderEntryPointStillSelectsTheNewestItself(@TempDir workDir: File) {
        assumeTwoLines()
        val requested = mutableListOf<String>()

        verifier(workDir, requested).verify(project, "Forge")

        Assertions.assertEquals(listOf(newerFile.fileName), requested)
    }

    /**
     * A target whose Minecraft version no loader build exists for refuses at staging, naming that version —
     * it is not silently re-selected onto one that does. Selection is deliberately not repeated inside
     * `prepareBootPack`, so this is where such a combination surfaces.
     */
    @Test
    fun aTargetWithNoLoaderBuildRefusesNamingItsOwnVersion(@TempDir workDir: File) {
        val requested = mutableListOf<String>()
        val unbootable = ModFile("themod-x.jar", setOf("Forge"), setOf("0.0.1"), "https://cdn/x.jar", null, emptyList())
        val target = BootCandidateSelector.GrindTarget("0.0", "Forge", unbootable, "0.0.1")

        val prepared = verifier(workDir, requested).prepareBootPack(project, target)

        Assertions.assertTrue(prepared is BootVerifier.Prepared.Failed)
        Assertions.assertTrue(
            (prepared as BootVerifier.Prepared.Failed).detail.contains("0.0.1"),
            "the refusal must name the version the caller chose, was: ${prepared.detail}"
        )
        Assertions.assertEquals(
            PreventionCause.UPSTREAM_UNAVAILABLE, prepared.cause,
            "nothing published a loader build there -- nobody's failure, and not a statement about the mod"
        )
    }
}
