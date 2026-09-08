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
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

/**
 * Pins that a mod id supplied by a **nested (jar-in-jar) library** counts as staged, so a requirement that
 * contradicts *its* version is a conflict `DependencyBacktrack` can act on.
 *
 * **The blind spot.** `BootVerifier.dependencyToDemote` built its "what is really on the classpath" map from
 * two sources that both miss nested jars: `modsDir.listFiles()`, which only sees top-level files, and
 * `InjectedDependency.version`, which only exists for something the platform published. A library the loader
 * loads out of another mod's `META-INF/jars/` is therefore in neither — and `DependencyBacktrack.conflicts`
 * deliberately skips a requirement naming something not staged, because that case belongs to
 * `refuseForMissingDependencies`. So the pack's real incoherence was invisible and the boot went ahead.
 *
 * **Measured live, `CurseForge/createaddition` on NeoForge 21.1.250 / Minecraft 1.21.1, 2026-09-07:**
 *
 * ```
 * Missing or unsupported mandatory dependencies:
 * Mod ID: 'ponder', Requested by: 'create', Expected range: '[1.0.82,)', Actual version: '1.0.64'
 * ```
 *
 * `ponder` appears in none of that verdict's four `stagedDependencies` — it arrived inside another jar. The
 * boot burned its container and the *candidate* wore the INCONCLUSIVE, which is the "never got a fair run"
 * shape every other guard in this module exists to prevent. Rare, but it is the direction that publishes a
 * wrong verdict rather than merely wasting time, so it is worth closing.
 *
 * Harness shape is [DependencyBacktrackStagingTest]'s, with a downloader that can nest a jar inside a jar.
 *
 * @author Griefed
 */
internal class NestedDependencyConflictTest {

    private val apiWrapper = ApiWrapper.api(File("build/resources/test/serverpackcreator.properties"))
    private val resolver = LoaderVersionResolver(apiWrapper.versionMeta)

    private val fabricRelease = apiWrapper.versionMeta.minecraft.serverReleases()
        .map { it.minecraftVersion }
        .first { resolver.latest("Fabric", it) != null }

    /** Selection passes, generation cannot: everything asserted here happens before it. */
    private val unbootableLoaderVersion = object : LoaderVersionPolicy {
        override fun preferredVersion(loader: String, minecraftVersion: String) = "0.0.0-no-such-build"
        override fun latestVersion(loader: String, minecraftVersion: String) = "0.0.0-no-such-build"
    }

    private fun fabricFile(fileName: String, version: String, dependencies: List<String> = emptyList()) =
        ModFile(fileName, setOf("Fabric"), setOf(fabricRelease), "https://cdn/$fileName", null, dependencies, version)

    /** The candidate pulls in both the library that demands and the mod that ships the demanded thing. */
    private val candidate = ProjectFiles(
        platform = "Modrinth",
        slug = "createaddition",
        projectUrl = "https://modrinth.com/mod/createaddition",
        clientSide = DeclaredSupport.UNKNOWN,
        serverSide = DeclaredSupport.UNKNOWN,
        files = listOf(fabricFile("createaddition-1.3.0.jar", "1.3.0", listOf("create", "ponderjs")))
    )

    /** Newest first, as both platforms answer — so the build demanding too much is picked first. */
    private val create = ProjectFiles(
        platform = "Modrinth", slug = "create", projectUrl = "https://modrinth.com/mod/create",
        clientSide = DeclaredSupport.UNKNOWN, serverSide = DeclaredSupport.UNKNOWN,
        files = listOf(
            fabricFile("create-6.0.10.jar", "6.0.10"),
            fabricFile("create-6.0.8.jar", "6.0.8")
        )
    )

    /** One build, and the only place `ponder` comes from: inside it. */
    private val ponderJs = ProjectFiles(
        platform = "Modrinth", slug = "ponderjs", projectUrl = "https://modrinth.com/mod/ponderjs",
        clientSide = DeclaredSupport.UNKNOWN, serverSide = DeclaredSupport.UNKNOWN,
        files = listOf(fabricFile("ponderjs-2.2.0.jar", "2.2.0"))
    )

    private val platform = object : ModPlatform {
        override val name: String = "Modrinth"
        override fun handles(projectUrl: String): Boolean = true
        override fun resolve(projectUrl: String): ProjectFiles = candidate
        override fun resolveDependency(nativeRef: String, minecraftVersion: String?): ProjectFiles? =
            when (nativeRef) {
                "create" -> create
                "ponderjs" -> ponderJs
                else -> null
            }
    }

    /** A `fabric.mod.json` body (no braces) per staged file, plus what each file nests. */
    private val descriptors = mapOf(
        "createaddition-1.3.0.jar" to """"id":"createaddition","version":"1.3.0"""",
        // The live shape: Create's newest build demands a Ponder newer than the one in the pack.
        "create-6.0.10.jar" to """"id":"create","version":"6.0.10","depends":{"ponder":">=1.0.82"}""",
        "create-6.0.8.jar" to """"id":"create","version":"6.0.8","depends":{"ponder":">=1.0.60"}""",
        "ponderjs-2.2.0.jar" to
            """"id":"ponderjs","version":"2.2.0","jars":[{"file":"META-INF/jars/ponder-1.0.64.jar"}]"""
    )

    /** What each staged file carries inside `META-INF/jars/`: path to that nested jar's descriptor body. */
    private val nested = mapOf(
        "ponderjs-2.2.0.jar" to ("META-INF/jars/ponder-1.0.64.jar" to """"id":"ponder","version":"1.0.64"""")
    )

    /** A real jar, with a real nested jar inside it where the fixture says so. */
    private fun downloader() = JarDownloader { modFile, targetDirectory ->
        val body = descriptors[modFile.fileName] ?: return@JarDownloader null
        targetDirectory.mkdirs()
        File(targetDirectory, modFile.fileName).also { jar ->
            JarOutputStream(jar.outputStream()).use { out ->
                out.putNextEntry(JarEntry("fabric.mod.json"))
                out.write("""{"schemaVersion":1,$body}""".toByteArray())
                out.closeEntry()
                nested[modFile.fileName]?.let { (path, nestedBody) ->
                    out.putNextEntry(JarEntry(path))
                    out.write(jarBytes(nestedBody))
                    out.closeEntry()
                }
            }
        }
    }

    /** The bytes of a one-descriptor jar, to be written as an entry of another jar. */
    private fun jarBytes(body: String): ByteArray {
        val buffer = ByteArrayOutputStream()
        JarOutputStream(buffer).use { out ->
            out.putNextEntry(JarEntry("fabric.mod.json"))
            out.write("""{"schemaVersion":1,$body}""".toByteArray())
            out.closeEntry()
        }
        return buffer.toByteArray()
    }

    private fun stagedMods(workDir: File): List<String> =
        File(workDir, AttemptDirectory.nameFor("Modrinth", "createaddition", "Fabric") + "/modpack/mods")
            .listFiles()?.map { it.name }?.sorted() ?: emptyList()

    private fun stage(workDir: File) = BootVerifier(
        apiWrapper = apiWrapper,
        platform = platform,
        httpDownloader = downloader(),
        loaderVersionPolicy = unbootableLoaderVersion,
        workDirectory = workDir
    ).prepareBootPack(candidate, "Fabric")

    /**
     * **The blind spot itself.** `ponder 1.0.64` exists only inside `ponderjs`, so nothing saw it, the
     * conflict went unnoticed and `create-6.0.10` was staged into a pack the loader refuses.
     */
    @Test
    fun aNestedLibraryVersionIsVisibleToTheConflictCheck(@TempDir workDir: File) {
        stage(workDir)

        Assertions.assertEquals(
            listOf("createaddition-1.3.0.jar", "create-6.0.8.jar", "ponderjs-2.2.0.jar").sorted(),
            stagedMods(workDir),
            "create 6.0.10 demands ponder >=1.0.82 and the pack holds the 1.0.64 nested in ponderjs, " +
                "so the older Create — which asks for >=1.0.60 — is what makes this pack coherent"
        )
    }

    /**
     * And the counterweight, or the fix would read as "always take the older dependency": a nested version
     * that *satisfies* the requirement demotes nothing.
     */
    @Test
    fun aSatisfiedNestedRequirementDemotesNothing(@TempDir workDir: File) {
        val satisfied = descriptors + mapOf(
            "create-6.0.10.jar" to """"id":"create","version":"6.0.10","depends":{"ponder":">=1.0.60"}"""
        )
        val verifier = BootVerifier(
            apiWrapper = apiWrapper,
            platform = platform,
            httpDownloader = JarDownloader { modFile, targetDirectory ->
                val body = satisfied[modFile.fileName] ?: return@JarDownloader null
                targetDirectory.mkdirs()
                File(targetDirectory, modFile.fileName).also { jar ->
                    JarOutputStream(jar.outputStream()).use { out ->
                        out.putNextEntry(JarEntry("fabric.mod.json"))
                        out.write("""{"schemaVersion":1,$body}""".toByteArray())
                        out.closeEntry()
                        nested[modFile.fileName]?.let { (path, nestedBody) ->
                            out.putNextEntry(JarEntry(path))
                            out.write(jarBytes(nestedBody))
                            out.closeEntry()
                        }
                    }
                }
            },
            loaderVersionPolicy = unbootableLoaderVersion,
            workDirectory = workDir
        )

        verifier.prepareBootPack(candidate, "Fabric")

        Assertions.assertTrue(
            stagedMods(workDir).contains("create-6.0.10.jar"),
            "1.0.64 satisfies >=1.0.60, so nothing contradicts anything: ${stagedMods(workDir)}"
        )
    }
}
