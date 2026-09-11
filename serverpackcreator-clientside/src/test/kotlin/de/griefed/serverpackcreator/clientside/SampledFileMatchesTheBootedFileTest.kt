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
 * Pins that a verdict names the artifact it is **about** — the one staging selected — rather than whichever
 * file of that loader the platform happened to list first.
 *
 * **The reported case, measured against the live CurseForge API on 2026-09-11.** `CurseForge/aether`'s Forge
 * row read `Filename = aether-1.12.2-v1.5.4.1.jar` while its detail was a `DEPENDENCY_FAILURE` naming
 * `curios-forge`. Both halves were true of different files:
 *
 * - `aether-1.20.1-1.5.2-neoforge.jar` is tagged `['NeoForge', '1.20.1', 'Forge']`, and
 *   [BootCandidateSelector.pickBootableCandidate] orders newest-Minecraft-first inside a release channel, so
 *   the **Forge** boot staged that jar. Its `META-INF/mods.toml` declares `modId = "curios"`,
 *   `mandatory = true`, `versionRange = "[5.3.1+1.20.1,)"` — so `curios-forge-<v>+1.20.1.jar` was staged
 *   beside it, and `DependencyAttribution` blamed the crash on its stem.
 * - `aether-1.12.2-v1.5.4.1.jar` was re-uploaded in 2025 and is therefore the **newest-uploaded**
 *   Forge-tagged file, which is what `loaderFiles.firstOrNull()` returns. Its `mcmod.info` declares
 *   `"dependencies": []`, and CurseForge lists `curios` against it only as relationType `1`
 *   (`EmbeddedLibrary`), which `CurseForgePlatform` correctly ignores.
 *
 * So the row was internally inconsistent, and a reader checking it against the platform page correctly
 * concluded the dependency resolution had gone wrong — when what had gone wrong was the attribution. A
 * verdict that cannot name its own evidence cannot be audited.
 *
 * Both guards below are *executed*: the pick is observed through the file staging actually asks for, not
 * asserted against a re-implementation of the selection rules.
 *
 * @author Griefed
 */
internal class SampledFileMatchesTheBootedFileTest {

    private val apiWrapper = ApiWrapper.api(File("build/resources/test/serverpackcreator.properties"))
    private val resolver = LoaderVersionResolver(apiWrapper.versionMeta)

    /**
     * Every real Minecraft release Forge publishes a build for, oldest first — taken from SPC's own
     * metadata so the fixture states an *era* rather than a version that upstream can retire.
     */
    private val forgeReleases = apiWrapper.versionMeta.minecraft.serverReleases()
        .map { it.minecraftVersion }
        .filter { resolver.latest("Forge", it) != null }
        .sortedWith(BootCandidateSelector.minecraftComparator)

    /** The aether shape's old build: a release channel file on a Minecraft line nobody would pick today. */
    private val olderRelease = forgeReleases.first()

    /** The build selection should actually reach: same loader, newer Minecraft, same channel. */
    private val newerRelease = forgeReleases.last()

    /** The published name of the old build — first in list order, exactly as CurseForge returned aether's. */
    private val olderFileName = "themod-$olderRelease-v1.5.4.1.jar"

    /** The published name of the newer build, which is the one a Forge boot selects. */
    private val newerFileName = "themod-$newerRelease-1.5.2.jar"

    /**
     * The aether ordering: the **older** Minecraft build is listed first, because the platform orders by
     * upload date and that build was re-uploaded most recently.
     */
    private fun project() = ProjectFiles(
        platform = "CurseForge",
        slug = "themod",
        projectUrl = "https://www.curseforge.com/minecraft/mc-mods/themod",
        clientSide = DeclaredSupport.UNKNOWN,
        serverSide = DeclaredSupport.UNKNOWN,
        files = listOf(
            ModFile(olderFileName, setOf("Forge"), setOf(olderRelease), "https://cdn/old.jar", null, emptyList()),
            ModFile(newerFileName, setOf("Forge"), setOf(newerRelease), "https://cdn/new.jar", null, emptyList())
        )
    )

    /** A platform serving [project] and nothing else; dependency resolution is never reached. */
    private fun platform() = object : ModPlatform {
        override val name: String = "CurseForge"
        override fun handles(projectUrl: String): Boolean = true
        override fun resolve(projectUrl: String): ProjectFiles = project()
        override fun resolveDependency(nativeRef: String, minecraftVersion: String?): ProjectFiles? = null
    }

    /**
     * Without a boot there is still exactly one file the verdict is about — the one a boot *would* stage —
     * and the jar scan reads that same file. Sampling the platform's first entry instead is what produced
     * the aether row.
     */
    @Test
    fun theSampledFileIsTheOneSelectionWouldStage(@TempDir workDir: File) {
        Assumptions.assumeTrue(
            forgeReleases.size > 1 &&
                BootCandidateSelector.minecraftLine(olderRelease) != BootCandidateSelector.minecraftLine(newerRelease),
            "the shipped manifest must offer two Forge-capable releases on different Minecraft lines"
        )

        val verdict = ClientsideVerifier(
            platforms = listOf(platform()),
            metadataScanner = MetadataScanner(apiWrapper.modScanner),
            // No download, so the scan degrades and only the selection is under test.
            jarDownloader = JarDownloader { _, _ -> null },
            workDirectory = workDir
        ).report("https://www.curseforge.com/minecraft/mc-mods/themod").perLoader.single()

        Assertions.assertEquals(
            newerFileName, verdict.sampleFile,
            "the row must name the build a boot would stage, not whichever file the platform listed first"
        )
    }

    /**
     * **The invariant itself**, and the only form of it worth having: the file staging asked the network for
     * and the file the verdict names are the same file. Observed through the downloader rather than
     * asserted against the selection rules, so a future change to either half is caught by the pair
     * disagreeing.
     *
     * Staging stops at that download (the recorder answers `null`), so no server is ever launched and the
     * verdict falls back to the metadata pick — which is precisely the equality being asserted.
     */
    @Test
    fun theStagedFileAndTheReportedFileAreTheSameFile(@TempDir workDir: File) {
        Assumptions.assumeTrue(
            forgeReleases.size > 1 &&
                BootCandidateSelector.minecraftLine(olderRelease) != BootCandidateSelector.minecraftLine(newerRelease),
            "the shipped manifest must offer two Forge-capable releases on different Minecraft lines"
        )
        val requested = mutableListOf<String>()

        val verdict = ClientsideVerifier(
            platforms = listOf(platform()),
            metadataScanner = MetadataScanner(apiWrapper.modScanner),
            jarDownloader = JarDownloader { _, _ -> null },
            workDirectory = workDir,
            bootVerifierFactory = { modPlatform ->
                BootVerifier(
                    apiWrapper = apiWrapper,
                    platform = modPlatform,
                    httpDownloader = { modFile, _ -> requested.add(modFile.fileName); null },
                    loaderVersionPolicy = resolver,
                    workDirectory = workDir
                )
            }
        ).report("https://www.curseforge.com/minecraft/mc-mods/themod").perLoader.single()

        Assertions.assertEquals(
            listOf(newerFileName), requested,
            "staging must have selected the newer Minecraft build -- otherwise this test is not exercising " +
                "the disagreement it exists for"
        )
        Assertions.assertEquals(
            requested.single(), verdict.sampleFile,
            "the verdict has to name the artifact staging actually worked on; anything else attributes one " +
                "file's evidence to another file"
        )
    }
}
