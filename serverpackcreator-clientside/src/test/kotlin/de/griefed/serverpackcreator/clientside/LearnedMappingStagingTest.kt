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
 * Drives the **learning** through the real staging join and across two candidates, which is the whole
 * claim: an id one grind could not resolve becomes resolvable because an earlier grind downloaded the
 * project and read its descriptor.
 *
 * The library here is deliberately unresolvable by every route that exists without learning — its mod id
 * (`mysterylib_v9`) is nothing like its project ref (`weird-slug`), it is in no alias table, it matches no
 * module shape, and the platform does not answer to the id. The only way to know is to have seen the jar.
 *
 * That is `yet_another_config_lib_v3` in miniature, and the reason it is worth automating: the entry added
 * to `KnownModIds` for YACL today cost a wasted boot, a log grep and two platform lookups, for a fact
 * staging held in its hands the first time anything staged YACL.
 *
 * Harness shape is [DependencyBacktrackStagingTest]'s — fake platform, real jars written to disk,
 * generation made unreachable so every assertion is about staging.
 *
 * @author Griefed
 */
internal class LearnedMappingStagingTest {

    private val apiWrapper = ApiWrapper.api(File("build/resources/test/serverpackcreator.properties"))
    private val resolver = LoaderVersionResolver(apiWrapper.versionMeta)

    private val fabricRelease = apiWrapper.versionMeta.minecraft.serverReleases()
        .map { it.minecraftVersion }
        .first { resolver.latest("Fabric", it) != null }

    private val unbootableLoaderVersion = object : LoaderVersionPolicy {
        override fun preferredVersion(loader: String, minecraftVersion: String) = "0.0.0-no-such-build"
        override fun latestVersion(loader: String, minecraftVersion: String) = "0.0.0-no-such-build"
    }

    private fun fabricFile(fileName: String, version: String, dependencies: List<String> = emptyList()) =
        ModFile(fileName, setOf("Fabric"), setOf(fabricRelease), "https://cdn/$fileName", null, dependencies, version)

    /** Staged by ref from its platform page, which is how the library's id gets seen at all. */
    private val library = ProjectFiles(
        platform = "Modrinth", slug = "weird-slug", projectUrl = "https://modrinth.com/mod/weird-slug",
        clientSide = DeclaredSupport.UNKNOWN, serverSide = DeclaredSupport.UNKNOWN,
        files = listOf(fabricFile("MysteryLib-9.0.0.jar", "9.0.0"))
    )

    /** The first grind: its platform page links the library, so staging fetches and reads it. */
    private val teacher = ProjectFiles(
        platform = "Modrinth", slug = "teacher", projectUrl = "https://modrinth.com/mod/teacher",
        clientSide = DeclaredSupport.UNKNOWN, serverSide = DeclaredSupport.UNKNOWN,
        files = listOf(fabricFile("Teacher-1.0.0.jar", "1.0.0", listOf("weird-slug")))
    )

    /** The second grind: it needs the same library, and only its **mod id** appears anywhere. */
    private val learner = ProjectFiles(
        platform = "Modrinth", slug = "learner", projectUrl = "https://modrinth.com/mod/learner",
        clientSide = DeclaredSupport.UNKNOWN, serverSide = DeclaredSupport.UNKNOWN,
        files = listOf(fabricFile("Learner-1.0.0.jar", "1.0.0"))
    )

    private val descriptors = mapOf(
        "Teacher-1.0.0.jar" to """"id":"teacher","version":"1.0.0"""",
        "MysteryLib-9.0.0.jar" to """"id":"mysterylib_v9","version":"9.0.0"""",
        "Learner-1.0.0.jar" to """"id":"learner","version":"1.0.0","depends":{"mysterylib_v9":"*"}"""
    )

    /** Answers by ref only — asking for the library by its mod id gets nothing, as a real platform would. */
    private val platform = object : ModPlatform {
        override val name: String = "Modrinth"
        override fun handles(projectUrl: String): Boolean = true
        override fun resolve(projectUrl: String): ProjectFiles = teacher
        override fun resolveDependency(nativeRef: String, minecraftVersion: String?): ProjectFiles? =
            when (nativeRef) {
                "weird-slug" -> library
                else -> null
            }
    }

    private val downloader = JarDownloader { modFile, targetDirectory ->
        val body = descriptors[modFile.fileName] ?: return@JarDownloader null
        targetDirectory.mkdirs()
        File(targetDirectory, modFile.fileName).also { jar ->
            JarOutputStream(jar.outputStream()).use { out ->
                out.putNextEntry(JarEntry("fabric.mod.json"))
                out.write("""{"schemaVersion":1,$body}""".toByteArray())
                out.closeEntry()
            }
        }
    }

    private fun verifier(workDir: File, learned: LearnedModIds) = BootVerifier(
        apiWrapper = apiWrapper,
        platform = platform,
        httpDownloader = downloader,
        loaderVersionPolicy = unbootableLoaderVersion,
        workDirectory = workDir,
        learnedModIds = learned
    )

    private fun stagedMods(workDir: File, slug: String): List<String> =
        File(workDir, AttemptDirectory.nameFor("Modrinth", slug, "Fabric") + "/modpack/mods")
            .listFiles()?.map { it.name }?.sorted() ?: emptyList()

    /**
     * **The claim.** Grind the teacher first — its page links the library by ref, so the jar is fetched and
     * its `mysterylib_v9` recorded. Then grind the learner, whose only reference to the same library is that
     * mod id: it now stages, where before it would have been filed as "maps to nothing this platform
     * carries" and the boot spent without it.
     */
    @Test
    fun anIdLearnedFromOneGrindResolvesForTheNext(@TempDir workDir: File) {
        val learned = LearnedModIds()

        verifier(workDir, learned).prepareBootPack(teacher, "Fabric")
        Assertions.assertEquals(
            listOf("MysteryLib-9.0.0.jar", "Teacher-1.0.0.jar"),
            stagedMods(workDir, "teacher"),
            "the teacher's own page links the library by ref, so this much needs no learning"
        )
        Assertions.assertEquals(
            "weird-slug", learned.refFor("mysterylib_v9", "Modrinth"),
            "and reading that jar is what teaches the id"
        )

        verifier(workDir, learned).prepareBootPack(learner, "Fabric")

        Assertions.assertEquals(
            listOf("Learner-1.0.0.jar", "MysteryLib-9.0.0.jar"),
            stagedMods(workDir, "learner"),
            "the learner names only the mod id, which nothing but the earlier grind could resolve"
        )
    }

    /**
     * The counterweight: with nothing learned, the same second grind stages the candidate alone. Without
     * this the test above would pass just as well against a platform that answered to mod ids, and would be
     * proving nothing about learning.
     */
    @Test
    fun withoutTheEarlierGrindTheSameIdResolvesToNothing(@TempDir workDir: File) {
        verifier(workDir, LearnedModIds()).prepareBootPack(learner, "Fabric")

        Assertions.assertEquals(
            listOf("Learner-1.0.0.jar"),
            stagedMods(workDir, "learner"),
            "mysterylib_v9 is in no table, matches no shape, and the platform does not answer to it"
        )
    }
}
