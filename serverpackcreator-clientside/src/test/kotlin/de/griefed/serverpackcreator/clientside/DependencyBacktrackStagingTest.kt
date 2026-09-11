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
 * Drives the **whole staging join** — resolve, download, scan, judge the set, demote, re-stage — against a
 * fake platform and a downloader that writes real jars, so the decision `DependencyBacktrackTest` pins in
 * isolation is proven to be wired to something.
 *
 * **The case is `Modrinth/zoomify` on Quilt / Minecraft 1.20.5, 2026-09-06**, reproduced in miniature:
 * a dependency whose newest build demands a version of Fabric API that does not exist for the pack's
 * Minecraft, and an older build of the same dependency that demands nothing.
 *
 * Generation is deliberately made to fail — the injected [LoaderVersionPolicy] answers a loader version no
 * config check accepts — so the test stays offline and fast. Everything this asserts happens *before*
 * generation, and the staged `mods` directory is what carries the answer.
 */
internal class DependencyBacktrackStagingTest {

    private val apiWrapper = ApiWrapper.api(File("build/resources/test/serverpackcreator.properties"))
    private val resolver = LoaderVersionResolver(apiWrapper.versionMeta)

    /** A real Fabric-capable server release from the cached metadata, so the test stays version-agnostic. */
    private val fabricRelease = apiWrapper.versionMeta.minecraft.serverReleases()
        .map { it.minecraftVersion }
        .first { resolver.latest("Fabric", it) != null }

    /**
     * Selection must pass but generation must not: the support gate only asks for a non-`null` answer, while
     * the config check rejects a version no loader ever published.
     */
    private val unbootableLoaderVersion = object : LoaderVersionPolicy {
        override fun preferredVersion(loader: String, minecraftVersion: String) = "0.0.0-no-such-build"
        override fun latestVersion(loader: String, minecraftVersion: String) = "0.0.0-no-such-build"
    }

    private fun fabricFile(fileName: String, version: String, dependencies: List<String> = emptyList()) =
        ModFile(fileName, setOf("Fabric"), setOf(fabricRelease), "https://cdn/$fileName", null, dependencies, version)

    /** The mod under test, needing the config library by platform ref. */
    private val candidate = ProjectFiles(
        platform = "Modrinth",
        slug = "zoomify",
        projectUrl = "https://modrinth.com/mod/zoomify",
        clientSide = DeclaredSupport.REQUIRED,
        serverSide = DeclaredSupport.UNSUPPORTED,
        files = listOf(fabricFile("Zoomify-2.13.3.jar", "2.13.3", listOf("yacl")))
    )

    /** Newest first, exactly as both platforms answer — so the demand-too-much build is picked first. */
    private val configLibrary = ProjectFiles(
        platform = "Modrinth", slug = "yacl", projectUrl = "https://modrinth.com/mod/yacl",
        clientSide = DeclaredSupport.UNKNOWN, serverSide = DeclaredSupport.UNKNOWN,
        files = listOf(
            fabricFile("yet_another_config_lib_v3-3.6.6.jar", "3.6.6+1.20.6"),
            fabricFile("YetAnotherConfigLib-3.4.2.jar", "3.4.2+1.20.5")
        )
    )

    private val fabricApi = ProjectFiles(
        platform = "Modrinth", slug = "fabric-api", projectUrl = "https://modrinth.com/mod/fabric-api",
        clientSide = DeclaredSupport.UNKNOWN, serverSide = DeclaredSupport.UNKNOWN,
        files = listOf(fabricFile("fabric-api-0.97.8.jar", "0.97.8+1.20.5"))
    )

    private val platform = object : ModPlatform {
        override val name: String = "Modrinth"
        override fun handles(projectUrl: String): Boolean = true
        override fun resolve(projectUrl: String): ProjectFiles = candidate
        override fun resolveDependency(nativeRef: String, minecraftVersion: String?): ProjectFiles? =
            when (nativeRef) {
                "yacl" -> configLibrary
                "fabric-api" -> fabricApi
                else -> null
            }
    }

    /**
     * Each staged file's `fabric.mod.json` **body** (no enclosing braces), keyed by file name — the
     * descriptors that make the set coherent or not.
     */
    private val descriptors = mapOf(
        "Zoomify-2.13.3.jar" to """"id":"zoomify","environment":"client","depends":{"yacl":"*"}""",
        // The live shape: tagged for the pack's Minecraft, accepting it in its own descriptor, and demanding
        // a Fabric API that only exists for the *next* Minecraft version.
        "yet_another_config_lib_v3-3.6.6.jar" to """"id":"yacl","depends":{"fabric-api":">=0.100.0+1.20.6"}""",
        "YetAnotherConfigLib-3.4.2.jar" to """"id":"yacl","depends":{"fabric-resource-loader-v0":"*"}""",
        "fabric-api-0.97.8.jar" to """"id":"fabric-api","provides":["fabric"]"""
    )

    /** Writes the real jar the scanners will read, so nothing here is mocked below the descriptor. */
    private fun downloaderFor(bodies: Map<String, String>) = JarDownloader { modFile, targetDirectory ->
        val body = bodies[modFile.fileName] ?: return@JarDownloader null
        targetDirectory.mkdirs()
        File(targetDirectory, modFile.fileName).also { jar ->
            JarOutputStream(jar.outputStream()).use { out ->
                out.putNextEntry(JarEntry("fabric.mod.json"))
                out.write("""{"schemaVersion":1,$body}""".toByteArray())
                out.closeEntry()
            }
        }
    }

    private fun stagedMods(workDir: File): List<String> =
        File(workDir, AttemptDirectory.nameFor("Modrinth", "zoomify", "Fabric", BootCandidateSelector.minecraftLine(fabricRelease)) + "/modpack/mods")
            .listFiles()?.map { it.name }?.sorted() ?: emptyList()

    /**
     * **The whole point.** The newest config-library build is picked first, its descriptor demands a Fabric
     * API the pack cannot hold, and staging drops it a build rather than booting a pack the loader will
     * refuse — which is what burned ~70 s and published INCONCLUSIVE against the *candidate*.
     */
    @Test
    fun aDependencyDemandingAnUnavailableVersionIsDroppedToAnOlderBuild(@TempDir workDir: File) {
        val verifier = BootVerifier(
            apiWrapper = apiWrapper,
            platform = platform,
            httpDownloader = downloaderFor(descriptors),
            loaderVersionPolicy = unbootableLoaderVersion,
            workDirectory = workDir
        )

        verifier.prepareBootPack(candidate, "Fabric")

        Assertions.assertEquals(
            listOf("Zoomify-2.13.3.jar", "YetAnotherConfigLib-3.4.2.jar", "fabric-api-0.97.8.jar").sorted(),
            stagedMods(workDir),
            "the 3.6.6 build demands fabric-api >=0.100.0+1.20.6 and must not survive staging"
        )
    }

    /**
     * And a coherent set is staged exactly as before: no demotion, no extra download, the newest build kept.
     * Without this the fix would be indistinguishable from "always take the older dependency".
     */
    @Test
    fun aCoherentSetKeepsTheNewestDependency(@TempDir workDir: File) {
        val satisfiable = descriptors + mapOf(
            "yet_another_config_lib_v3-3.6.6.jar" to """"id":"yacl","depends":{"fabric-api":">=0.90.0"}"""
        )
        val verifier = BootVerifier(
            apiWrapper = apiWrapper,
            platform = platform,
            httpDownloader = downloaderFor(satisfiable),
            loaderVersionPolicy = unbootableLoaderVersion,
            workDirectory = workDir
        )

        verifier.prepareBootPack(candidate, "Fabric")

        Assertions.assertEquals(
            listOf("Zoomify-2.13.3.jar", "yet_another_config_lib_v3-3.6.6.jar", "fabric-api-0.97.8.jar").sorted(),
            stagedMods(workDir),
            "nothing contradicts anything, so the newest build stays"
        )
    }
}
