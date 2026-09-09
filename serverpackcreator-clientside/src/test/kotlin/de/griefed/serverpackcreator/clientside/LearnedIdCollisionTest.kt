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
 * Pins that **one mod id can be served by more than one project**, and that a learned mapping which cannot
 * stage does not end the search.
 *
 * `LearnedModIds` recorded one ref per id and kept whichever project proved it first. That is the right call
 * about *overwriting* — grind order must not decide the answer — and the wrong one about *forgetting*: the
 * cross-loader forks and unofficial ports that fill the ecosystem deliberately keep the original's mod id,
 * so whichever of them is ground first owns the id for every loader afterwards. And because a learned
 * mapping is an `Alias` rather than a `Guess`, the wrong project's "publishes nothing for this loader and
 * Minecraft version" carries the right to **refuse the boot**.
 *
 * **Measured on `grinder.serverpackcreator.de`, 2026-09-09.** `chefs-delight` on Forge / Minecraft 1.20.1 is
 * published `ERROR` for `farmersdelight`. That refusal can only have come from the manifest route — the
 * platform route labels an unmet dependency with the resolved project's *slug*, and Farmer's Delight's
 * Modrinth slug is `farmers-delight`, not `farmersdelight` — and the manifest route refuses only on a
 * confident mapping, which `KnownModIds.mappingFor("farmersdelight", "Modrinth")` is not: it is a `Guess`.
 * So the alias came from the learned map. Verified against the live API the same day, the real project
 * publishes `FarmersDelight-1.20.1-1.3.4.jar` for Forge 1.20.1 — so the ref that "publishes nothing" is a
 * different project answering to the same id, and its Fabric port is the obvious candidate.
 *
 * The same collision explains `create` (Create ↔ Create Fabric) and `sophisticatedcore` (Sophisticated Core
 * ↔ its unofficial Fabric port); those are additionally covered by [ProvidedDependencyTest], because the
 * project is in the pack there and this one's is not.
 *
 * **Trying each learned ref is what makes this loader-aware without a loader dimension**, since
 * `BootCandidateSelector.pickDependencyFile` already filters by loader and Minecraft version. Whichever
 * project has a build for the boot in hand is the one that stages.
 *
 * @author Griefed
 */
internal class LearnedIdCollisionTest {

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

    private fun modFile(fileName: String, loaders: Set<String>) =
        ModFile(fileName, loaders, setOf(fabricRelease), "https://cdn/$fileName", null, emptyList())

    private fun project(slug: String, vararg files: ModFile) = ProjectFiles(
        "Modrinth", slug, "https://modrinth.com/mod/$slug",
        DeclaredSupport.UNKNOWN, DeclaredSupport.UNKNOWN, files.toList()
    )

    /**
     * `chefs-delight`'s shape: **no platform-declared dependency at all**, so the only route to Farmer's
     * Delight is the mod id in the jar. The loaders are swapped against the live row (a Fabric boot needing
     * the Fabric port, where the row is a Forge boot needing the Forge original) because the staging harness
     * is proven on `fabric.mod.json`; the collision is the same one either way.
     */
    private val candidate = project("chefs-delight", modFile("chefsdelight-1.0.4.jar", setOf("Fabric")))

    /** The Forge original, which proved the id first and publishes nothing this boot can use. */
    private val forgeOriginal = project("farmers-delight", modFile("FarmersDelight-1.3.4.jar", setOf("Forge")))

    /** The Fabric port, which proved the same id second and is the one that fits. */
    private val fabricPort =
        project("farmers-delight-fabric", modFile("farmersdelight-fabric-1.3.4.jar", setOf("Fabric")))

    private val platform = object : ModPlatform {
        override val name: String = "Modrinth"
        override fun handles(projectUrl: String): Boolean = true
        override fun resolve(projectUrl: String): ProjectFiles = candidate
        override fun resolveDependency(nativeRef: String, minecraftVersion: String?): ProjectFiles? =
            when (nativeRef) {
                "farmers-delight" -> forgeOriginal
                "farmers-delight-fabric" -> fabricPort
                else -> null
            }
    }

    private val descriptors = mapOf(
        "chefsdelight-1.0.4.jar" to """"id":"chefsdelight","depends":{"farmersdelight":"*"}""",
        "farmersdelight-fabric-1.3.4.jar" to """"id":"farmersdelight"""",
        "FarmersDelight-1.3.4.jar" to """"id":"farmersdelight""""
    )

    /** Writes the real jar the scanners will read, so nothing here is mocked below the descriptor. */
    private fun downloaderFor(bodies: Map<String, String>) = JarDownloader { file, targetDirectory ->
        val body = bodies[file.fileName] ?: return@JarDownloader null
        targetDirectory.mkdirs()
        File(targetDirectory, file.fileName).also { jar ->
            JarOutputStream(jar.outputStream()).use { out ->
                out.putNextEntry(JarEntry("fabric.mod.json"))
                out.write("""{"schemaVersion":1,$body}""".toByteArray())
                out.closeEntry()
            }
        }
    }

    private fun stagedMods(workDir: File): List<String> =
        File(workDir, AttemptDirectory.nameFor("Modrinth", "chefs-delight", "Fabric") + "/modpack/mods")
            .listFiles()?.map { it.name }?.sorted() ?: emptyList()

    /**
     * **The whole point.** Two projects have proved `farmersdelight`; the one that proved it first has no
     * build for this boot and the other does. Staging must reach the second rather than refusing on the
     * first, because the first is not the answer to the question being asked.
     */
    @Test
    fun aSecondProjectIsTriedWhenTheFirstCannotStage(@TempDir workDir: File) {
        val learned = LearnedModIds().apply {
            learn("Modrinth", "farmers-delight", setOf("farmersdelight"))
            learn("Modrinth", "farmers-delight-fabric", setOf("farmersdelight"))
        }
        val verifier = BootVerifier(
            apiWrapper = apiWrapper,
            platform = platform,
            httpDownloader = downloaderFor(descriptors),
            loaderVersionPolicy = unbootableLoaderVersion,
            workDirectory = workDir,
            learnedModIds = learned
        )

        val prepared = verifier.prepareBootPack(candidate, "Fabric")

        Assertions.assertEquals(
            listOf("chefsdelight-1.0.4.jar", "farmersdelight-fabric-1.3.4.jar"),
            stagedMods(workDir),
            "the Fabric port is what this pack needs, and something has proved it serves the id"
        )
        val detail = (prepared as? BootVerifier.Prepared.Failed)?.detail.orEmpty()
        Assertions.assertFalse(
            detail.contains("Required dependency"),
            "one project having no build for this loader is not the dependency being unavailable ($detail)"
        )
    }

    /**
     * And where **no** project that proved the id can stage it, the refusal stands and still names the id:
     * the alias's right to refuse is what stops a genuinely-missing library reaching a container.
     */
    @Test
    fun anIdNoProvenProjectCanStageStillRefuses(@TempDir workDir: File) {
        val learned = LearnedModIds().apply { learn("Modrinth", "farmers-delight", setOf("farmersdelight")) }
        val verifier = BootVerifier(
            apiWrapper = apiWrapper,
            platform = platform,
            httpDownloader = downloaderFor(descriptors),
            loaderVersionPolicy = unbootableLoaderVersion,
            workDirectory = workDir,
            learnedModIds = learned
        )

        val prepared = verifier.prepareBootPack(candidate, "Fabric")

        Assertions.assertTrue(
            (prepared as? BootVerifier.Prepared.Failed)?.detail?.contains("farmersdelight") == true,
            "only the Forge original has proved this id, and it publishes no Fabric build"
        )
    }
}
