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
import de.griefed.serverpackcreator.api.modscanning.ModDependency
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

/**
 * Pins that a manifest-declared requirement the staged pack **already provides** never refuses the boot.
 *
 * `stageableRequirements` drops a requirement that is optional, bundled inside the candidate,
 * environment-provided, or whose *platform ref* was already resolved. It does not ask the one question that
 * settles the matter: **is the id already in `mods/`?** The ref dedupe is not that question, because a
 * project is reachable under two equally-valid refs — the one the platform page links, and whatever
 * `LearnedModIds`/`KnownModIds` maps the manifest id to — and when those differ the same id is looked up a
 * second time, against a *different project*.
 *
 * **Measured on `grinder.serverpackcreator.de`, 2026-09-09.** Ten published `ERROR` verdicts are this:
 *
 * | Candidate | Loader / Minecraft | Refused for |
 * |---|---|---|
 * | `copycats`, `create-steam-n-rails`, `createaddition` (both platforms) | Forge 1.20.1, NeoForge 1.21.1 | `create` |
 * | `ends-delight` | Forge 1.20.1, NeoForge 1.21.1 | `farmersdelight` |
 * | `sophisticated-backpacks`/`-storage` (unofficial Fabric ports) | Fabric 1.21.1 | `sophisticatedcore` |
 *
 * Every one of them declares the project as a *platform* dependency too, so the jar was downloaded and
 * staged before the refusal was raised. Verified against the live Modrinth API the same day: `createaddition`
 * requires project `LNytGWDc`, which publishes 17 Forge 1.20.1 files and 11 NeoForge 1.21.1 files — so the
 * project that "publishes nothing for this loader and Minecraft version" is not the one the pack contains.
 *
 * The mod id and the platform ref name **one** mod; refusing a boot over the second lookup of a dependency
 * that is sitting in the pack is the clearest possible case of a verdict that cannot name its own evidence.
 *
 * The fixture is the `sophisticatedcore` row, because it is the one whose two projects are a Fabric port and
 * a Forge original — the collision at its plainest. Staging is driven exactly as
 * `DependencyBacktrackStagingTest` drives it: a fake platform, a downloader that writes real jars, and a
 * loader version no config check accepts, so generation fails and the test stays offline. Everything
 * asserted here happens before generation.
 *
 * @author Griefed
 */
internal class ProvidedDependencyTest {

    /**
     * The pure half: an id a staged jar answers to is not a requirement to go and fetch, exactly as a
     * bundled one is not.
     */
    @Test
    fun aRequirementTheStagedPackAlreadyProvidesIsNotStageable() {
        val requirements = listOf(ModDependency("sophisticatedcore"), ModDependency("balm"))

        Assertions.assertEquals(
            listOf("balm"),
            BootVerifier.stageableRequirements(requirements, providedIds = setOf("sophisticatedcore"))
                .map { it.modID },
            "'sophisticatedcore' is already in mods/; only 'balm' is still worth resolving"
        )
    }

    /** Case is not identity: descriptors spell ids inconsistently and a miss here costs the whole boot. */
    @Test
    fun theProvidedCheckIgnoresCase() {
        Assertions.assertEquals(
            emptyList<String>(),
            BootVerifier.stageableRequirements(
                listOf(ModDependency("SophisticatedCore")),
                providedIds = setOf("sophisticatedcore")
            ).map { it.modID }
        )
    }

    /** And an id nothing provides is still stageable, so the drop cannot be mistaken for "never refuse". */
    @Test
    fun anIdNothingProvidesIsStillStageable() {
        Assertions.assertEquals(
            listOf("farmersdelight"),
            BootVerifier.stageableRequirements(
                listOf(ModDependency("farmersdelight")),
                providedIds = setOf("create")
            ).map { it.modID }
        )
    }

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

    private fun fabricFile(fileName: String, dependencies: List<String> = emptyList()) =
        ModFile(fileName, setOf("Fabric"), setOf(fabricRelease), "https://cdn/$fileName", null, dependencies)

    /** The unofficial Fabric port, requiring its core by the platform ref its Modrinth page links. */
    private val candidate = ProjectFiles(
        platform = "Modrinth",
        slug = "sophisticated-storage",
        projectUrl = "https://modrinth.com/mod/sophisticated-storage",
        clientSide = DeclaredSupport.REQUIRED,
        serverSide = DeclaredSupport.REQUIRED,
        files = listOf(fabricFile("sophisticatedstorage-1.3.7.9.jar", listOf("9jxwkYQL")))
    )

    /** The core's Fabric port — a file for exactly the loader and Minecraft being booted. */
    private val fabricCore = ProjectFiles(
        platform = "Modrinth", slug = "sophisticated-core-fabric",
        projectUrl = "https://modrinth.com/mod/sophisticated-core-fabric",
        clientSide = DeclaredSupport.UNKNOWN, serverSide = DeclaredSupport.UNKNOWN,
        files = listOf(fabricFile("sophisticatedcore-fabric-1.3.7.jar"))
    )

    /**
     * The **Forge** original, a different project answering to the same mod id `sophisticatedcore` and
     * publishing nothing this boot can use. This is what a learned mapping points at once the Forge project
     * happens to be ground first, since "the first project to prove an id keeps it".
     */
    private val forgeCore = ProjectFiles(
        platform = "Modrinth", slug = "sophisticated-core",
        projectUrl = "https://modrinth.com/mod/sophisticated-core",
        clientSide = DeclaredSupport.UNKNOWN, serverSide = DeclaredSupport.UNKNOWN,
        files = listOf(
            ModFile(
                "sophisticatedcore-forge-1.3.7.jar", setOf("Forge"), setOf(fabricRelease),
                "https://cdn/sophisticatedcore-forge-1.3.7.jar", null, emptyList()
            )
        )
    )

    private val platform = object : ModPlatform {
        override val name: String = "Modrinth"
        override fun handles(projectUrl: String): Boolean = true
        override fun resolve(projectUrl: String): ProjectFiles = candidate
        override fun resolveDependency(nativeRef: String, minecraftVersion: String?): ProjectFiles? =
            when (nativeRef) {
                "9jxwkYQL" -> fabricCore
                "sophisticated-core" -> forgeCore
                else -> null
            }
    }

    /**
     * Each staged file's `fabric.mod.json` **body** (no enclosing braces), keyed by file name. The candidate
     * declares its core by *mod id*, which is the second lookup this test is about.
     */
    private val descriptors = mapOf(
        "sophisticatedstorage-1.3.7.9.jar" to
            """"id":"sophisticatedstorage","depends":{"sophisticatedcore":"*"}""",
        "sophisticatedcore-fabric-1.3.7.jar" to """"id":"sophisticatedcore"""",
        "sophisticatedcore-forge-1.3.7.jar" to """"id":"sophisticatedcore""""
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
        File(workDir, AttemptDirectory.nameFor("Modrinth", "sophisticated-storage", "Fabric", BootCandidateSelector.minecraftLine(fabricRelease)) + "/modpack/mods")
            .listFiles()?.map { it.name }?.sorted() ?: emptyList()

    /** The learned map as the live daemon holds it: one id, pointing at whichever project proved it first. */
    private fun learnedAs(modId: String, ref: String) =
        LearnedModIds().apply { restore(mapOf("Modrinth" to mapOf(modId to listOf(ref)))) }

    /**
     * **The published row verbatim.** The core is staged from the platform ref, the candidate's own
     * descriptor then names `sophisticatedcore`, and the learned map sends that id to the Forge project —
     * which publishes nothing for this boot. The refusal that produced must not happen: the dependency is
     * in the pack.
     */
    @Test
    fun aDependencyAlreadyInThePackDoesNotRefuseTheBoot(@TempDir workDir: File) {
        val verifier = BootVerifier(
            apiWrapper = apiWrapper,
            platform = platform,
            httpDownloader = downloaderFor(descriptors),
            loaderVersionPolicy = unbootableLoaderVersion,
            workDirectory = workDir,
            learnedModIds = learnedAs("sophisticatedcore", "sophisticated-core")
        )

        val prepared = verifier.prepareBootPack(candidate, "Fabric")

        Assertions.assertEquals(
            listOf("sophisticatedcore-fabric-1.3.7.jar", "sophisticatedstorage-1.3.7.9.jar"),
            stagedMods(workDir),
            "the core was staged from the platform ref, so the pack has what the descriptor asks for"
        )
        val detail = (prepared as? BootVerifier.Prepared.Failed)?.detail.orEmpty()
        Assertions.assertFalse(
            detail.contains("Required dependency"),
            "the pack contains 'sophisticatedcore'; refusing it for a missing one is the defect ($detail)"
        )
    }

    /**
     * The other direction, so the fix stays a *dedupe* and not an amnesty: a required manifest id that
     * really is absent from the pack still refuses, and still names itself.
     */
    @Test
    fun aDependencyMissingFromThePackStillRefusesTheBoot(@TempDir workDir: File) {
        val needsSomethingElse = descriptors + mapOf(
            "sophisticatedstorage-1.3.7.9.jar" to
                """"id":"sophisticatedstorage","depends":{"sophisticatedcore":"*","balm":"*"}"""
        )
        val verifier = BootVerifier(
            apiWrapper = apiWrapper,
            platform = platform,
            httpDownloader = downloaderFor(needsSomethingElse),
            loaderVersionPolicy = unbootableLoaderVersion,
            workDirectory = workDir,
            // `balm` maps to a project that publishes only a Forge build, so it resolves and cannot stage.
            learnedModIds = learnedAs("balm", "sophisticated-core")
        )

        val prepared = verifier.prepareBootPack(candidate, "Fabric")

        Assertions.assertTrue(
            (prepared as? BootVerifier.Prepared.Failed)?.detail?.contains("balm") == true,
            "nothing in the pack answers to 'balm', so the refusal is the honest outcome"
        )
    }
}
