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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

/**
 * Pins that a mod id served by **two projects** — the original and a separate cross-loader fork — reaches
 * whichever of them publishes for the loader being booted.
 *
 * **The measured case, read from the live Modrinth API on 2026-09-10 and again on 2026-09-11.** `create`
 * publishes `loaders = [forge, neoforge]` and **nothing** for Fabric; the Fabric port is a distinct project,
 * `create-fabric`, publishing `[fabric, quilt]`. Both declare the mod id `create`, because a port keeps the
 * original's id on purpose — that is what makes it a drop-in. So a Fabric mod declaring `create` resolved to
 * a project with no Fabric build, and the boot was refused for a dependency that exists.
 *
 * `LearnedModIds` already learned this shape from jars it staged, and deliberately keeps **every** prover
 * rather than the first, for exactly this reason. What it cannot do is help the first time: nothing has
 * proved the fork yet, so the very candidate that would teach it is the one refused. The registry is where
 * an *observed* fork is seeded, and `KnownModIds`' own doc is the constraint on that — ids seen failing,
 * never a speculative table.
 *
 * **Why the fork is an alternative and not a replacement.** The table has no loader dimension and must not
 * grow one: mapping `create` to `create-fabric` outright would send every Forge and NeoForge boot to a
 * project with no Forge build, turning three broken rows into many. The primary is tried first and the fork
 * only when it answers nothing — the same shape `LearnedModIds.mappingsFor` already produces, and the
 * reason `pickDependencyFile`'s loader filter is what actually decides.
 *
 * Driven through real staging with a recording platform, so the *request sequence* is asserted: "it found
 * the fork" and "it did not pay for the fork when the primary answered" are the same guard.
 *
 * @author Griefed
 */
internal class ForkedProjectDependencyTest {

    private val apiWrapper = ApiWrapper.api(File("build/resources/test/serverpackcreator.properties"))
    private val resolver = LoaderVersionResolver(apiWrapper.versionMeta)

    /** A real Fabric-capable server release from the cached metadata, so the test states no version. */
    private val fabricRelease = apiWrapper.versionMeta.minecraft.serverReleases()
        .map { it.minecraftVersion }
        .first { resolver.latest("Fabric", it) != null }

    /** Selection passes and generation does not, so staging is exercised and no server is ever launched. */
    private val unbootableLoaderVersion = object : LoaderVersionPolicy {
        override fun preferredVersion(loader: String, minecraftVersion: String) = "0.0.0-no-such-build"
        override fun latestVersion(loader: String, minecraftVersion: String) = "0.0.0-no-such-build"
    }

    private fun fileFor(fileName: String, loaders: Set<String>) =
        ModFile(fileName, loaders, setOf(fabricRelease), "https://cdn/$fileName", null, emptyList())

    private fun projectOf(slug: String, files: List<ModFile>) = ProjectFiles(
        platform = "Modrinth", slug = slug, projectUrl = "https://modrinth.com/mod/$slug",
        clientSide = DeclaredSupport.UNKNOWN, serverSide = DeclaredSupport.UNKNOWN, files = files
    )

    /** The candidate declares `create` in its manifest and nowhere else, which is the route under test. */
    private val candidate = projectOf("some-addon", listOf(fileFor("some-addon-1.0.0.jar", setOf("Fabric"))))

    /** Every ref the staging actually asked the platform about, in order. */
    private val asked = mutableListOf<String>()

    /**
     * Modrinth as it really answers: `create` is a Forge/NeoForge project, `create-fabric` a separate one.
     * [createPublishesFabric] flips the original into publishing a Fabric build, which is how the
     * "don't pay for the fork when the primary answers" direction is exercised.
     */
    private fun platform(createPublishesFabric: Boolean) = object : ModPlatform {
        override val name: String = "Modrinth"
        override fun handles(projectUrl: String): Boolean = true
        override fun resolve(projectUrl: String): ProjectFiles = candidate

        override fun resolveDependency(nativeRef: String, minecraftVersion: String?): ProjectFiles? {
            asked.add(nativeRef)
            return when (nativeRef) {
                "create" -> projectOf(
                    "create",
                    listOf(
                        fileFor("create-1.0.0.jar", if (createPublishesFabric) setOf("Fabric") else setOf("Forge"))
                    )
                )

                "create-fabric" -> projectOf("create-fabric", listOf(fileFor("create-fabric-1.0.0.jar", setOf("Fabric"))))
                else -> null
            }
        }
    }

    /** Writes the real jars the scanners read, so the manifest route is exercised rather than mocked. */
    private val downloader = JarDownloader { file, targetDirectory ->
        targetDirectory.mkdirs()
        File(targetDirectory, file.fileName).also { jar ->
            JarOutputStream(jar.outputStream()).use { out ->
                out.putNextEntry(JarEntry("fabric.mod.json"))
                val body = if (file.fileName.startsWith("some-addon")) {
                    """"id":"someaddon","depends":{"create":"*"}"""
                } else {
                    """"id":"create""""
                }
                out.write("""{"schemaVersion":1,$body}""".toByteArray())
                out.closeEntry()
            }
        }
    }

    private fun stage(workDir: File, createPublishesFabric: Boolean = false): List<String> {
        BootVerifier(
            apiWrapper = apiWrapper,
            platform = platform(createPublishesFabric),
            httpDownloader = downloader,
            loaderVersionPolicy = unbootableLoaderVersion,
            workDirectory = workDir
        ).prepareBootPack(candidate, "Fabric")
        return File(workDir, AttemptDirectory.nameFor("Modrinth", "some-addon", "Fabric", BootCandidateSelector.minecraftLine(fabricRelease)) + "/modpack/mods")
            .listFiles()?.map { it.name }?.sorted().orEmpty()
    }

    /**
     * **The live row.** The original publishes no Fabric build, the fork does, and the mod id is the same —
     * so the dependency is staged from the fork instead of being reported missing.
     */
    @Test
    fun aFabricBootReachesTheFabricFork(@TempDir workDir: File) {
        val staged = stage(workDir)

        Assertions.assertEquals(
            listOf("create-fabric-1.0.0.jar", "some-addon-1.0.0.jar"), staged,
            "`create` publishes forge/neoforge only; the Fabric port is the project `create-fabric`"
        )
        Assertions.assertEquals(
            listOf("create", "create-fabric"), asked,
            "and the original is still asked first: the fork is the fallback, not the mapping"
        )
    }

    /**
     * **The fork costs nothing where the primary answers**, which is what stops a table of alternatives
     * becoming a second request for every dependency in the catalog.
     */
    @Test
    fun theForkIsNotAskedWhenTheOriginalPublishesForTheLoader(@TempDir workDir: File) {
        val staged = stage(workDir, createPublishesFabric = true)

        Assertions.assertEquals(
            listOf("create-1.0.0.jar", "some-addon-1.0.0.jar"), staged,
            "the original answered, so it is what gets staged"
        )
        Assertions.assertEquals(listOf("create"), asked, "and nothing else was asked for")
    }

    /**
     * A renamed project, the other half of the same report: `tacz` is a mod id no Modrinth slug matches
     * (verified 404 on 2026-09-11), and the project is `timeless-and-classics-guns`. That one *is* a
     * mapping rather than an alternative — the bare id names nothing on either platform.
     */
    @Test
    fun aRenamedProjectIsMappedRatherThanGuessed() {
        Assertions.assertEquals(
            listOf("timeless-and-classics-guns"),
            KnownModIds.mappingsFor("tacz", "Modrinth").map { it.ref },
            "the slug guess 404s, so guessing it is a wasted request and a refusal we cannot act on"
        )
    }

    /**
     * **The reason the CurseForge side of that entry is `null`, pinned.** No numeric CurseForge id could be
     * verified for either observed fork, and a wrong one stages somebody else's mod — so a table entry that
     * carries no ref for a platform falls through to that platform's own slug guess instead of resolving to
     * nothing. Without this the entry would have *removed* CurseForge's pre-existing guess, which is a
     * regression rather than a fix.
     */
    @Test
    fun anEntryWithNoRefForAPlatformFallsBackToThatPlatformsGuess() {
        Assertions.assertEquals(
            ModIdMapping.Guess("tacz"), KnownModIds.mappingFor("tacz", "CurseForge"),
            "CurseForge addresses this by whatever its own search resolves, not by Modrinth's slug"
        )
        Assertions.assertEquals(
            ModIdMapping.Alias("timeless-and-classics-guns"), KnownModIds.mappingFor("tacz", "Modrinth"),
            "and the platform the project *was* verified on still gets the mapping"
        )
        Assertions.assertEquals(
            ModIdMapping.None, KnownModIds.mappingFor("tacz", "SomeFuturePlatform"),
            "a platform this registry knows nothing about still gets nothing, not a guess"
        )
    }

    /** A fork that happens to equal the primary contributes nothing — one ref, asked once. */
    @Test
    fun aForkIsNeverOfferedTwice() {
        Assertions.assertEquals(
            listOf("create", "create-fabric"),
            KnownModIds.mappingsFor("create", "Modrinth").map { it.ref },
            "the primary comes first and each ref appears once"
        )
        Assertions.assertEquals(
            listOf("create"), KnownModIds.mappingsFor("create", "CurseForge").map { it.ref },
            "the fork carries no CurseForge ref, so that platform is left with its own guess alone"
        )
    }

    /** Every ordinary id is unchanged: one mapping, exactly as before, so this adds no request anywhere. */
    @Test
    fun anIdWithNoKnownForkStillYieldsOneMapping() {
        Assertions.assertEquals(
            listOf("some-random-mod"),
            KnownModIds.mappingsFor("some-random-mod", "Modrinth").map { it.ref }
        )
        Assertions.assertEquals(
            listOf("fabric-api"),
            KnownModIds.mappingsFor("fabric-resource-loader-v0", "Modrinth").map { it.ref },
            "and a family module still resolves to its one project"
        )
    }
}
