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
import java.io.ByteArrayOutputStream
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

/**
 * Pins that Quilt's **`unless`** clause is honoured: a requirement naming an alternative is satisfied by
 * that alternative, so a mod declaring *"QSL, unless Fabric API is here"* boots with Fabric API.
 *
 * **This is Quilt's own mechanism and the loader implements it.** A `quilt.mod.json` entry may carry
 * `unless`, and Quilt Loader then treats the requirement as met when the named id is present. Read from the
 * live jars on 2026-09-10, four of the five refused Quilt rows declare exactly this:
 *
 * ```json
 * { "id": "quilt_resource_loader", "versions": "*", "unless": "fabric-resource-loader-v0" }
 * ```
 *
 * `geophilic`, `terralith`, `trek` and `true-ending` all ship it, and `QuiltScanner` read `id` and
 * `versions` and dropped `unless` — so the requirement looked hard, `quilt_resource_loader` resolved to
 * QSL, and QSL publishes **nothing** past Minecraft 1.21 (`qsl` for Quilt 1.21.1 → 0 versions, measured
 * against the live Modrinth API) while `fabric-api` for 1.21.1 → 36 versions. Mods that run everywhere were
 * refused everywhere.
 *
 * `fabric-resource-loader-v0` is a Fabric API *module*, which `KnownModIds` already resolves to `fabric-api`
 * by shape — so the alternative is not merely expressible, it is already resolvable.
 *
 * **What this does not cover, deliberately:** a requirement with *no* `unless`. `OctoLib-QUILT-0.5.0.1.jar`
 * (`shatterbyte-lib`, `notenoughrecipebook`) hard-requires `quilt_base`, and that jar genuinely targets a
 * Quilt+QSL combination which does not exist for Minecraft 1.21.1. `UNVERIFIABLE` is the correct verdict
 * there, and the last guard below keeps it that way.
 *
 * Driven through real staging, exactly as `DependencyBacktrackStagingTest` and
 * `DependencyMinecraftRangeTest` drive it: a fake platform, a downloader that writes real jars, and a loader
 * version no config check accepts so generation fails and the test stays offline.
 *
 * @author Griefed
 */
internal class QuiltUnlessClauseTest {

    private val apiWrapper = ApiWrapper.api(File("build/resources/test/serverpackcreator.properties"))
    private val resolver = LoaderVersionResolver(apiWrapper.versionMeta)

    /** A real Quilt-capable server release from the cached metadata, so the test stays version-agnostic. */
    private val quiltRelease = apiWrapper.versionMeta.minecraft.serverReleases()
        .map { it.minecraftVersion }
        .first { resolver.latest("Quilt", it) != null }

    /** Selection passes and generation does not, so staging is exercised and no server is ever launched. */
    private val unbootableLoaderVersion = object : LoaderVersionPolicy {
        override fun preferredVersion(loader: String, minecraftVersion: String) = "0.0.0-no-such-build"
        override fun latestVersion(loader: String, minecraftVersion: String) = "0.0.0-no-such-build"
    }

    private fun file(fileName: String, loaders: Set<String>, mcVersion: String = quiltRelease) =
        ModFile(fileName, loaders, setOf(mcVersion), "https://cdn/$fileName", null, emptyList())

    /** `terralith`'s shape: a Quilt-tagged jar, no platform-declared dependencies at all. */
    private val candidate = ProjectFiles(
        platform = "Modrinth", slug = "terralith", projectUrl = "https://modrinth.com/mod/terralith",
        clientSide = DeclaredSupport.REQUIRED, serverSide = DeclaredSupport.REQUIRED,
        files = listOf(file("Terralith_1.21.x_v2.5.14.jar", setOf("Quilt", "Fabric")))
    )

    /**
     * QSL as it really is: published, but nothing for the Minecraft being booted. The project resolves, so
     * this is not an "unresolved ref" — it is the shape that makes the requirement look hard and unmeetable.
     */
    private val qsl = ProjectFiles(
        platform = "Modrinth", slug = "qsl", projectUrl = "https://modrinth.com/mod/qsl",
        clientSide = DeclaredSupport.UNKNOWN, serverSide = DeclaredSupport.UNKNOWN,
        files = listOf(file("qsl-7.0.0+1.21.jar", setOf("Quilt"), mcVersion = "1.21"))
    )

    /** Fabric API, which does publish for the boot — and which Quilt loads. */
    private val fabricApi = ProjectFiles(
        platform = "Modrinth", slug = "fabric-api", projectUrl = "https://modrinth.com/mod/fabric-api",
        clientSide = DeclaredSupport.UNKNOWN, serverSide = DeclaredSupport.UNKNOWN,
        files = listOf(file("fabric-api-0.116.17.jar", setOf("Fabric")))
    )

    private val platform = object : ModPlatform {
        override val name: String = "Modrinth"
        override fun handles(projectUrl: String): Boolean = true
        override fun resolve(projectUrl: String): ProjectFiles = candidate
        override fun resolveDependency(nativeRef: String, minecraftVersion: String?): ProjectFiles? =
            when (nativeRef) {
                "qsl" -> qsl
                "fabric-api" -> fabricApi
                else -> null
            }
    }

    /** Writes the real jar the scanners read, so nothing here is mocked below the descriptor. */
    private fun downloaderFor(bodies: Map<String, Pair<String, String>>) =
        JarDownloader { modFile, targetDirectory ->
            val (descriptor, body) = bodies[modFile.fileName] ?: return@JarDownloader null
            targetDirectory.mkdirs()
            File(targetDirectory, modFile.fileName).also { jar ->
                JarOutputStream(jar.outputStream()).use { out ->
                    out.putNextEntry(JarEntry(descriptor))
                    out.write(body.toByteArray())
                    out.closeEntry()
                }
            }
        }

    /** `terralith`'s real `quilt.mod.json`, with the `unless` naming the Fabric API module. */
    private fun quiltCandidate(unless: String?) = "quilt.mod.json" to """
        {"schema_version":1,"quilt_loader":{"id":"terralith","version":"2.5.14","depends":[
          {"id":"quilt_resource_loader","versions":"*"${unless?.let { ""","unless":"$it"""" } ?: ""}}
        ]}}
    """.trimIndent()

    private val descriptors = mapOf(
        "Terralith_1.21.x_v2.5.14.jar" to quiltCandidate("fabric-resource-loader-v0"),
        "qsl-7.0.0+1.21.jar" to ("quilt.mod.json" to """{"schema_version":1,"quilt_loader":{"id":"qsl"}}"""),
        "fabric-api-0.116.17.jar" to
            ("fabric.mod.json" to """{"schemaVersion":1,"id":"fabric-api","provides":["fabric"]}""")
    )

    /**
     * `terralith`'s descriptor with the same `unless`, plus a **declared nested jar** carrying the
     * alternative — `sodium`'s shape, which declares nine of them, all Fabric API modules.
     */
    private val bundlingCandidate = "quilt.mod.json" to """
        {"schema_version":1,"quilt_loader":{"id":"terralith","version":"2.5.14",
          "jars":["META-INF/jars/fabric-resource-loader-v0-0.116.17.jar"],
          "depends":[{"id":"quilt_resource_loader","versions":"*","unless":"fabric-resource-loader-v0"}]}}
    """.trimIndent()

    /** The nested module as a real jar-in-jar, so `BundledJars` reads an archive rather than a stub. */
    private fun nestedResourceLoader(): ByteArray {
        val bytes = ByteArrayOutputStream()
        JarOutputStream(bytes).use { out ->
            out.putNextEntry(JarEntry("fabric.mod.json"))
            out.write("""{"schemaVersion":1,"id":"fabric-resource-loader-v0","version":"0.116.17"}""".toByteArray())
            out.closeEntry()
        }
        return bytes.toByteArray()
    }

    /**
     * [downloaderFor] plus the nested jar inside the candidate, and it **records every file it fetched** —
     * which is the only way to see that a dropped requirement costs no download.
     */
    private fun bundlingDownloader(bodies: Map<String, Pair<String, String>>, fetched: MutableList<String>) =
        JarDownloader { modFile, targetDirectory ->
            fetched.add(modFile.fileName)
            val (descriptor, body) = bodies[modFile.fileName] ?: return@JarDownloader null
            targetDirectory.mkdirs()
            File(targetDirectory, modFile.fileName).also { jar ->
                JarOutputStream(jar.outputStream()).use { out ->
                    out.putNextEntry(JarEntry(descriptor))
                    out.write(body.toByteArray())
                    out.closeEntry()
                    if (descriptor == "quilt.mod.json" && body.contains("META-INF/jars/")) {
                        out.putNextEntry(JarEntry("META-INF/jars/fabric-resource-loader-v0-0.116.17.jar"))
                        out.write(nestedResourceLoader())
                        out.closeEntry()
                    }
                }
            }
        }

    private fun stagedMods(workDir: File): List<String> =
        File(workDir, AttemptDirectory.nameFor("Modrinth", "terralith", "Quilt", BootCandidateSelector.minecraftLine(quiltRelease)) + "/modpack/mods")
            .listFiles()?.map { it.name }?.sorted() ?: emptyList()

    private fun stage(bodies: Map<String, Pair<String, String>>, workDir: File): BootVerifier.Prepared =
        BootVerifier(
            apiWrapper = apiWrapper,
            platform = platform,
            httpDownloader = downloaderFor(bodies),
            loaderVersionPolicy = unbootableLoaderVersion,
            workDirectory = workDir
        ).prepareBootPack(candidate, "Quilt")

    /**
     * **The four refused rows.** QSL cannot be staged for this Minecraft, the descriptor says Fabric API
     * will do instead, and Fabric API publishes for it — so the boot happens.
     */
    @Test
    fun anUnlessAlternativeSatisfiesTheRequirement(@TempDir workDir: File) {
        val prepared = stage(descriptors, workDir)

        Assertions.assertEquals(
            listOf("Terralith_1.21.x_v2.5.14.jar", "fabric-api-0.116.17.jar"), stagedMods(workDir),
            "the descriptor's own alternative is what the loader would use, so it is what staging must use"
        )
        val detail = (prepared as? BootVerifier.Prepared.Failed)?.detail.orEmpty()
        Assertions.assertFalse(
            detail.contains("Required dependency"),
            "the requirement names an alternative that is present; refusing on it is the defect ($detail)"
        )
    }

    /**
     * **The cheap path, which is the one that was dead.** `stageableRequirements` is supposed to drop a
     * requirement whose `unless` alternative is already in the pack — and the alternative most often *is*,
     * as a **jar-in-jar**: read from the live `fabric-api-0.116.17+1.21.1.jar`, its descriptor declares
     * `id=fabric-api` and `provides=["fabric"]`, and `fabric-resource-loader-v0` exists only as
     * `META-INF/jars/fabric-resource-loader-v0-0.116.17.jar`. So the id lands in `bundledIds`, never in
     * `providedIds`, and an arm testing only the latter cannot fire for its own documented case.
     *
     * Asserted through a **recording downloader**, because the observable cost is a fetch that should not
     * happen: without the fix the requirement survives, `alternativeFor` resolves Fabric API and stages a
     * second copy of a library the loader already has on the classpath.
     */
    @Test
    fun anUnlessAlternativeAlreadyBundledCostsNoDownload(@TempDir workDir: File) {
        val fetched = mutableListOf<String>()
        val bodies = descriptors + ("Terralith_1.21.x_v2.5.14.jar" to bundlingCandidate)

        BootVerifier(
            apiWrapper = apiWrapper,
            platform = platform,
            httpDownloader = bundlingDownloader(bodies, fetched),
            loaderVersionPolicy = unbootableLoaderVersion,
            workDirectory = workDir
        ).prepareBootPack(candidate, "Quilt")

        Assertions.assertEquals(
            listOf("Terralith_1.21.x_v2.5.14.jar"), fetched,
            "the alternative is inside the candidate; fetching the project that ships it buys nothing"
        )
        Assertions.assertEquals(
            listOf("Terralith_1.21.x_v2.5.14.jar"), stagedMods(workDir),
            "and nothing extra ends up in mods/"
        )
    }

    /**
     * And without an `unless` the refusal stands — `shatterbyte-lib`'s `quilt_base` really is hard, and its
     * jar targets a Quilt+QSL pairing that does not exist for the Minecraft being booted.
     */
    @Test
    fun aRequirementWithoutAnUnlessStillRefuses(@TempDir workDir: File) {
        val hardRequirement = descriptors +
            ("Terralith_1.21.x_v2.5.14.jar" to quiltCandidate(null))

        val prepared = stage(hardRequirement, workDir)

        Assertions.assertTrue(
            (prepared as? BootVerifier.Prepared.Failed)?.detail?.contains("Required dependency") == true,
            "nothing offers an alternative here, so the honest outcome is still a refusal"
        )
    }
}
