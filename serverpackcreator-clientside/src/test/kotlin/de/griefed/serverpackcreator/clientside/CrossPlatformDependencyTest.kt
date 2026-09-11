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
 * Pins that a required dependency the candidate's **own** platform cannot supply is fetched from the other
 * one (Griefed's call).
 *
 * **The measured row.** `tacz` resolves to `timeless-and-classics-guns`, whose Minecraft 1.21.1 build is
 * published on **CurseForge only** — so a Modrinth candidate needing it was refused for a jar that exists
 * and that any launcher would install. A mod's dependency is a *mod*, not a listing: which site hosts it
 * says nothing about whether the pack can boot with it, and the staged file is just a jar either way.
 *
 * **Why this cannot be done by ref.** A platform ref is that platform's identifier — Modrinth's opaque
 * base62 and CurseForge's numeric id name nothing on the other side — so only the **manifest** route, which
 * knows the mod *id*, can cross. That is also what bounds the cost: it is reached from a requirement that
 * is required, declared by the jar, and already unsatisfiable here, whose only other outcome is a refused
 * boot.
 *
 * **The learned mapping is recorded against the platform that proved it.** A ref learned on CurseForge is
 * meaningless on Modrinth, and filing it under the candidate's platform would teach the id table a mapping
 * that resolves to nothing — which the next candidate would then trust.
 *
 * Driven through real staging with two recording platforms, so what is asserted is the order of requests:
 * the home platform first, the other only after it has nothing.
 *
 * @author Griefed
 */
internal class CrossPlatformDependencyTest {

    private val apiWrapper = ApiWrapper.api(File("build/resources/test/serverpackcreator.properties"))
    private val resolver = LoaderVersionResolver(apiWrapper.versionMeta)

    private val fabricRelease = apiWrapper.versionMeta.minecraft.serverReleases()
        .map { it.minecraftVersion }
        .first { resolver.latest("Fabric", it) != null }

    /** Selection passes and generation does not, so staging is exercised and no server is ever launched. */
    private val unbootableLoaderVersion = object : LoaderVersionPolicy {
        override fun preferredVersion(loader: String, minecraftVersion: String) = "0.0.0-no-such-build"
        override fun latestVersion(loader: String, minecraftVersion: String) = "0.0.0-no-such-build"
    }

    private fun fileFor(fileName: String, loaders: Set<String> = setOf("Fabric")) =
        ModFile(fileName, loaders, setOf(fabricRelease), "https://cdn/$fileName", null, emptyList())

    private fun projectOf(platform: String, slug: String, files: List<ModFile>) = ProjectFiles(
        platform = platform, slug = slug, projectUrl = "https://$platform/$slug",
        clientSide = DeclaredSupport.UNKNOWN, serverSide = DeclaredSupport.UNKNOWN, files = files
    )

    private val candidate = projectOf("Modrinth", "some-addon", listOf(fileFor("some-addon-1.0.0.jar")))

    /** Every `(platform, ref)` the staging asked about, in order. */
    private val asked = mutableListOf<String>()

    /** The home platform: it carries the project, and it publishes no build this boot can use. */
    private val home = object : ModPlatform {
        override val name: String = "Modrinth"
        override fun handles(projectUrl: String): Boolean = true
        override fun resolve(projectUrl: String): ProjectFiles = candidate
        override fun resolveDependency(nativeRef: String, minecraftVersion: String?): ProjectFiles? {
            asked.add("Modrinth/$nativeRef")
            // Tagged Forge: the project exists here, it simply has nothing for the loader being booted.
            return projectOf("Modrinth", nativeRef, listOf(fileFor("somelib-forge-1.0.0.jar", setOf("Forge"))))
        }
    }

    /** The other platform, which publishes the build that is missing here. */
    private fun other(publishes: Boolean) = object : ModPlatform {
        override val name: String = "CurseForge"
        override fun handles(projectUrl: String): Boolean = false
        override fun resolve(projectUrl: String): ProjectFiles = error("the candidate is not resolved here")
        override fun resolveDependency(nativeRef: String, minecraftVersion: String?): ProjectFiles? {
            asked.add("CurseForge/$nativeRef")
            return projectOf("CurseForge", nativeRef, listOf(fileFor("somelib-fabric-1.0.0.jar")))
                .takeIf { publishes }
        }
    }

    private val downloader = JarDownloader { file, targetDirectory ->
        targetDirectory.mkdirs()
        File(targetDirectory, file.fileName).also { jar ->
            JarOutputStream(jar.outputStream()).use { out ->
                out.putNextEntry(JarEntry("fabric.mod.json"))
                val body = if (file.fileName.startsWith("some-addon")) {
                    """"id":"someaddon","depends":{"somelib":"*"}"""
                } else {
                    """"id":"somelib""""
                }
                out.write("""{"schemaVersion":1,$body}""".toByteArray())
                out.closeEntry()
            }
        }
    }

    private val learned = LearnedModIds()

    private fun stage(workDir: File, alternates: List<ModPlatform>): List<String> {
        BootVerifier(
            apiWrapper = apiWrapper,
            platform = home,
            httpDownloader = downloader,
            loaderVersionPolicy = unbootableLoaderVersion,
            workDirectory = workDir,
            alternatePlatforms = alternates,
            learnedModIds = learned
        ).prepareBootPack(candidate, "Fabric")
        return File(workDir, AttemptDirectory.nameFor("Modrinth", "some-addon", "Fabric") + "/modpack/mods")
            .listFiles()?.map { it.name }?.sorted().orEmpty()
    }

    /** **The live row.** The dependency exists, on the other site, and the pack boots with it. */
    @Test
    fun aDependencyOnlyTheOtherPlatformPublishesIsStaged(@TempDir workDir: File) {
        val staged = stage(workDir, listOf(other(publishes = true)))

        Assertions.assertEquals(
            listOf("some-addon-1.0.0.jar", "somelib-fabric-1.0.0.jar"), staged,
            "which site hosts the jar says nothing about whether the pack can boot with it"
        )
        Assertions.assertEquals(
            listOf("Modrinth/somelib", "CurseForge/somelib"), asked,
            "and the candidate's own platform is asked first: the other one is the fallback"
        )
    }

    /** The ref that worked is learned against **CurseForge**, because it means nothing on Modrinth. */
    @Test
    fun theRefIsLearnedAgainstThePlatformThatProvedIt(@TempDir workDir: File) {
        stage(workDir, listOf(other(publishes = true)))

        Assertions.assertEquals(
            listOf("somelib"), learned.refsFor("somelib", "CurseForge"),
            "a CurseForge ref filed under Modrinth would resolve to nothing for the next candidate"
        )
        Assertions.assertTrue(
            learned.refsFor("somelib", "Modrinth").isEmpty(),
            "and nothing on Modrinth proved anything: its project had no usable build"
        )
    }

    /** With no other platform configured — no API key, say — the refusal is exactly what it was. */
    @Test
    fun withNoOtherPlatformTheRefusalIsUnchanged(@TempDir workDir: File) {
        val staged = stage(workDir, emptyList())

        Assertions.assertEquals(listOf("some-addon-1.0.0.jar"), staged)
        Assertions.assertEquals(listOf("Modrinth/somelib"), asked, "nothing else exists to ask")
    }

    /** The other platform having nothing either leaves the original refusal standing, with its own reason. */
    @Test
    fun anUnsatisfiedDependencyStaysUnsatisfiedWhenNeitherPlatformHasIt(@TempDir workDir: File) {
        val staged = stage(workDir, listOf(other(publishes = false)))

        Assertions.assertEquals(listOf("some-addon-1.0.0.jar"), staged)
        Assertions.assertEquals(listOf("Modrinth/somelib", "CurseForge/somelib"), asked)
    }
}
