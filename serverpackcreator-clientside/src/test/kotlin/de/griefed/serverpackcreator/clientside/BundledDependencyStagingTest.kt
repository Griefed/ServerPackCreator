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
 * Pins that a **bundled** library's own demands reach staging — through `prepareBootPack`, the call the
 * daemon actually makes, rather than through `BundledJars` alone.
 *
 * `BundledDemandTest` pins the reader. This pins the *decision*, which is the distinction this module has
 * paid for three times in two days: a correct unit no caller reaches changes nothing, and a marker-based
 * assertion passes against the broken code. Only "did it download the library?" separates the two.
 *
 * **The live failure**, read off the public grinder on 2026-09-11 and verified by opening the published jar:
 * `Modrinth/highlight` declares `depends: { "resourcefullib": "*" }` and ships
 * `META-INF/jars/resourcefullib-fabric-26.2-5.0.3.jar`, so the requirement was rightly dropped — the library
 * is already inside. The bundled jar then declares `depends: { "fabric-api": "*" }`, which nothing read, so
 * Fabric API was never staged. The boot died with *"Resourceful Lib requires any version of fabric-api,
 * which is missing"*, and the INCONCLUSIVE was charged to `highlight`, whose `stagedDependencies` was empty.
 *
 * @author Griefed
 */
internal class BundledDependencyStagingTest {

    private val apiWrapper = ApiWrapper.api(File("build/resources/test/serverpackcreator.properties"))
    private val resolver = LoaderVersionResolver(apiWrapper.versionMeta)

    /** A real Fabric-capable server release from the cached metadata, so the test states an era. */
    private val fabricRelease = apiWrapper.versionMeta.minecraft.serverReleases()
        .map { it.minecraftVersion }
        .first { resolver.latest("Fabric", it) != null }

    /** Selection must pass and generation must not, so nothing here ever launches a server. */
    private val unbootableLoaderVersion = object : LoaderVersionPolicy {
        override fun preferredVersion(loader: String, minecraftVersion: String) = "0.0.0-no-such-build"
        override fun latestVersion(loader: String, minecraftVersion: String) = "0.0.0-no-such-build"
    }

    private fun fabricFile(fileName: String) =
        ModFile(fileName, setOf("Fabric"), setOf(fabricRelease), "https://cdn/$fileName", null, emptyList())

    /** `highlight`, which declares its library and then ships it. */
    private val candidate = ProjectFiles(
        platform = "Modrinth", slug = "highlight", projectUrl = "https://modrinth.com/mod/highlight",
        clientSide = DeclaredSupport.UNKNOWN, serverSide = DeclaredSupport.UNKNOWN,
        files = listOf(fabricFile("hightlight-26.2-4.2.0.jar"))
    )

    private val fabricApi = ProjectFiles(
        platform = "Modrinth", slug = "fabric-api", projectUrl = "https://modrinth.com/mod/fabric-api",
        clientSide = DeclaredSupport.UNKNOWN, serverSide = DeclaredSupport.UNKNOWN,
        files = listOf(fabricFile("fabric-api-0.160.0.jar"))
    )

    private val platform = object : ModPlatform {
        override val name: String = "Modrinth"
        override fun handles(projectUrl: String): Boolean = true
        override fun resolve(projectUrl: String): ProjectFiles = candidate
        override fun resolveDependency(nativeRef: String, minecraftVersion: String?): ProjectFiles? =
            fabricApi.takeIf { nativeRef == "fabric-api" }
    }

    /** The bytes of a one-descriptor jar, to be written as an entry of another jar. */
    private fun nestedJarBytes(body: String): ByteArray {
        val buffer = ByteArrayOutputStream()
        JarOutputStream(buffer).use { out ->
            out.putNextEntry(JarEntry("fabric.mod.json"))
            out.write("""{"schemaVersion":1,$body}""".toByteArray())
            out.closeEntry()
        }
        return buffer.toByteArray()
    }

    /** Writes the real jars the scanners read, so nothing below the descriptor is mocked. */
    private fun downloader(requested: MutableList<String>) = JarDownloader { modFile, targetDirectory ->
        requested.add(modFile.fileName)
        val nested = modFile.fileName.startsWith("hightlight")
        val body = if (nested) {
            """"id":"highlight","depends":{"resourcefullib":"*"},""" +
                """"jars":[{"file":"META-INF/jars/resourcefullib-fabric-26.2-5.0.3.jar"}]"""
        } else {
            """"id":"fabric-api","provides":["fabric"]"""
        }
        targetDirectory.mkdirs()
        File(targetDirectory, modFile.fileName).also { jar ->
            JarOutputStream(jar.outputStream()).use { out ->
                out.putNextEntry(JarEntry("fabric.mod.json"))
                out.write("""{"schemaVersion":1,$body}""".toByteArray())
                out.closeEntry()
                if (nested) {
                    out.putNextEntry(JarEntry("META-INF/jars/resourcefullib-fabric-26.2-5.0.3.jar"))
                    out.write(
                        nestedJarBytes(
                            """"id":"resourcefullib","version":"5.0.3","depends":{"fabric-api":"*"}"""
                        )
                    )
                    out.closeEntry()
                }
            }
        }
    }

    /**
     * **The pin.** The candidate asks for nothing the platform can supply and nothing its own descriptor
     * names beyond what it ships — so the only route to Fabric API is the bundled library's own `depends`.
     */
    @Test
    fun aLibraryDemandedOnlyByABundledJarIsStillStaged(@TempDir workDir: File) {
        val requested = mutableListOf<String>()

        BootVerifier(
            apiWrapper = apiWrapper,
            platform = platform,
            httpDownloader = downloader(requested),
            loaderVersionPolicy = unbootableLoaderVersion,
            workDirectory = workDir
        ).prepareBootPack(candidate, "Fabric")

        Assertions.assertEquals(
            listOf("hightlight-26.2-4.2.0.jar", "fabric-api-0.160.0.jar"), requested,
            "the bundled resourcefullib demands fabric-api, and nothing else in this fixture does"
        )
    }
}
