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
 * Pins that an unmet dependency says **why** it is unmet, because three unrelated failures publish one
 * sentence today and an operator cannot tell them apart.
 *
 * `refuseForMissingDependencies` renders every entry of `unsatisfied` the same way, and
 * `unsatisfiedLabel` distinguishes only two of the five ways a dependency gets in there: an unresolved ref
 * and a distribution-locked file. The other three — *the project publishes nothing usable*, *we picked a
 * file and the download died*, and *staging dropped every usable build itself while backtracking* — all
 * print the bare slug, so the verdict reads `Required dependency unavailable … balm` in each case.
 *
 * **This is not cosmetic, and the live store is the argument.** On 2026-09-07 the grinder published 47
 * such rows. Diagnosing them took a CurseForge API probe (`misc/cf-dependency-probe.sh`, which proved the
 * files were all there and correctly tagged) and a log grep on the daemon host (`1014` re-stagings against
 * `4` staging failures, which is what finally identified the backtrack). Both were needed only because the
 * verdict itself named no evidence — the standard this module holds every *boot* verdict to
 * (`BootDecision.decidedBy`) and does not yet hold a *staging* refusal to.
 *
 * The third case below is the one that cost the most: the backtrack demoting a dependency until nothing is
 * left reports the project as publishing nothing, which is the opposite of true — it published builds, and
 * we excluded them.
 *
 * Harness shape (fake platform, real jars on disk, generation deliberately unreachable) is
 * [DependencyBacktrackStagingTest]'s; see its doc for why the loader version is unbootable.
 *
 * @author Griefed
 */
internal class UnmetDependencyReasonTest {

    private val apiWrapper = ApiWrapper.api(File("build/resources/test/serverpackcreator.properties"))
    private val resolver = LoaderVersionResolver(apiWrapper.versionMeta)

    /** A real Fabric-capable server release from the cached metadata, so the test stays version-agnostic. */
    private val fabricRelease = apiWrapper.versionMeta.minecraft.serverReleases()
        .map { it.minecraftVersion }
        .first { resolver.latest("Fabric", it) != null }

    /** Selection passes, generation cannot: every assertion here is about what happens before it. */
    private val unbootableLoaderVersion = object : LoaderVersionPolicy {
        override fun preferredVersion(loader: String, minecraftVersion: String) = "0.0.0-no-such-build"
        override fun latestVersion(loader: String, minecraftVersion: String) = "0.0.0-no-such-build"
    }

    private fun fabricFile(
        fileName: String,
        version: String,
        dependencies: List<String> = emptyList(),
        downloadUrl: String? = "https://cdn/$fileName"
    ) = ModFile(fileName, setOf("Fabric"), setOf(fabricRelease), downloadUrl, null, dependencies, version)

    private val candidate = ProjectFiles(
        platform = "Modrinth",
        slug = "zoomify",
        projectUrl = "https://modrinth.com/mod/zoomify",
        clientSide = DeclaredSupport.REQUIRED,
        serverSide = DeclaredSupport.UNSUPPORTED,
        files = listOf(fabricFile("Zoomify-2.13.3.jar", "2.13.3", listOf("yacl")))
    )

    private val fabricApi = ProjectFiles(
        platform = "Modrinth", slug = "fabric-api", projectUrl = "https://modrinth.com/mod/fabric-api",
        clientSide = DeclaredSupport.UNKNOWN, serverSide = DeclaredSupport.UNKNOWN,
        files = listOf(fabricFile("fabric-api-0.97.8.jar", "0.97.8+1.20.5"))
    )

    /** A platform answering [configLibrary] for `yacl`, so each case varies only what that project ships. */
    private fun platformServing(configLibrary: ProjectFiles) = object : ModPlatform {
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

    private val descriptors = mapOf(
        "Zoomify-2.13.3.jar" to """"id":"zoomify","environment":"client","depends":{"yacl":"*"}""",
        "yet_another_config_lib_v3-3.6.6.jar" to """"id":"yacl","depends":{"fabric-api":">=0.100.0+1.20.6"}""",
        "fabric-api-0.97.8.jar" to """"id":"fabric-api","provides":["fabric"]"""
    )

    /**
     * Writes the real jar the scanners read; a file with no body here fails to download, as CDNs do.
     *
     * **Mirrors `HttpJarDownloader`'s first line**: a file with no `downloadUrl` is distribution-locked and
     * cannot be fetched at all. A fake that ignored that would make the locked path untestable — and did,
     * until this test was written against it and came back reporting a backtrack instead.
     */
    private fun downloaderFor(bodies: Map<String, String>) = JarDownloader { modFile, targetDirectory ->
        if (modFile.downloadUrl == null) return@JarDownloader null
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

    private fun refusalFor(
        configLibrary: ProjectFiles,
        bodies: Map<String, String>,
        workDir: File
    ): String {
        val prepared = BootVerifier(
            apiWrapper = apiWrapper,
            platform = platformServing(configLibrary),
            httpDownloader = downloaderFor(bodies),
            loaderVersionPolicy = unbootableLoaderVersion,
            workDirectory = workDir
        ).prepareBootPack(candidate, "Fabric")

        return Assertions.assertInstanceOf(BootVerifier.Prepared.Failed::class.java, prepared).detail
    }

    /** Nothing usable published: the only build is tagged for a loader this boot cannot run. */
    @Test
    fun aProjectPublishingNothingUsableSaysSo(@TempDir workDir: File) {
        val forgeOnly = ProjectFiles(
            platform = "Modrinth", slug = "yacl", projectUrl = "https://modrinth.com/mod/yacl",
            clientSide = DeclaredSupport.UNKNOWN, serverSide = DeclaredSupport.UNKNOWN,
            files = listOf(
                ModFile(
                    "yacl-forge-3.6.6.jar", setOf("Forge"), setOf(fabricRelease),
                    "https://cdn/yacl-forge-3.6.6.jar", null, emptyList(), "3.6.6"
                )
            )
        )

        val detail = refusalFor(forgeOnly, descriptors, workDir)

        Assertions.assertTrue(
            detail.contains("yacl (nothing published for this loader and Minecraft version)"),
            "the project really does publish nothing usable, and the refusal should say that: $detail"
        )
    }

    /** A file was picked and the download died — a transient failure, not an absent build. */
    @Test
    fun aFailedDownloadIsNotAnAbsentBuild(@TempDir workDir: File) {
        val configLibrary = ProjectFiles(
            platform = "Modrinth", slug = "yacl", projectUrl = "https://modrinth.com/mod/yacl",
            clientSide = DeclaredSupport.UNKNOWN, serverSide = DeclaredSupport.UNKNOWN,
            files = listOf(fabricFile("yet_another_config_lib_v3-3.6.6.jar", "3.6.6+1.20.6"))
        )

        // The candidate downloads; its dependency does not, because no body is staged for that file name.
        val detail = refusalFor(configLibrary, descriptors - "yet_another_config_lib_v3-3.6.6.jar", workDir)

        Assertions.assertTrue(
            detail.contains("yacl (download failed)"),
            "the build exists and was picked; retrying might work, and the refusal must not blame the project: $detail"
        )
    }

    /**
     * A file was picked and its author opted out of third-party distribution: there is no URL to fetch, and
     * no amount of retrying produces one. Distinguishing this from a transient failure is the same call
     * `downloadFailureDetail` makes for the candidate — and until now it was asserted only where the string
     * is rendered, never where the reason is *chosen* (A-5, `claude-docs/ANALYSIS-AUDIT.md`).
     */
    @Test
    fun aDistributionLockedDependencyIsNotAFailedDownload(@TempDir workDir: File) {
        val lockedOnly = ProjectFiles(
            platform = "Modrinth", slug = "yacl", projectUrl = "https://modrinth.com/mod/yacl",
            clientSide = DeclaredSupport.UNKNOWN, serverSide = DeclaredSupport.UNKNOWN,
            files = listOf(
                fabricFile("yet_another_config_lib_v3-3.6.6.jar", "3.6.6+1.20.6", downloadUrl = null)
            )
        )

        val detail = refusalFor(lockedOnly, descriptors, workDir)

        Assertions.assertTrue(
            detail.contains("yacl (distribution-locked on Modrinth)"),
            "an author's opt-out must not read as a transient failure: $detail"
        )
    }

    /**
     * A ref the platform does not carry at all. The label already says so, so the reason adds no second
     * sentence — but it must be *chosen*, and that choice was likewise asserted nowhere.
     */
    @Test
    fun anUnresolvedRefSaysWhichPlatformItBelongsTo(@TempDir workDir: File) {
        val carriesNothing = object : ModPlatform {
            override val name: String = "Modrinth"
            override fun handles(projectUrl: String): Boolean = true
            override fun resolve(projectUrl: String): ProjectFiles = candidate
            override fun resolveDependency(nativeRef: String, minecraftVersion: String?): ProjectFiles? = null
        }
        val prepared = BootVerifier(
            apiWrapper = apiWrapper,
            platform = carriesNothing,
            httpDownloader = downloaderFor(descriptors),
            loaderVersionPolicy = unbootableLoaderVersion,
            workDirectory = workDir
        ).prepareBootPack(candidate, "Fabric")

        val detail = Assertions.assertInstanceOf(BootVerifier.Prepared.Failed::class.java, prepared).detail
        Assertions.assertTrue(
            detail.contains("yacl (unresolved Modrinth project)"),
            "an opaque ref must say where to look it up, and must not gain a second explanation: $detail"
        )
    }

    /**
     * **The `aether` case, end to end.** The platform attributes a dependency to this file that the jar's
     * own descriptor never names, and the project publishes nothing usable — staging must boot anyway
     * rather than publish an ERROR about a dependency the loader will not ask for.
     *
     * Asserted through the real staging join rather than on `PlatformDependencyDemand` alone, because a
     * pure predicate proved correct in isolation says nothing about whether production consults it — the
     * lesson `DependencySlugTest` was written for.
     */
    @Test
    fun aPlatformDependencyTheJarNeverNamesDoesNotRefuseTheBoot(@TempDir workDir: File) {
        // The candidate's platform page claims it needs yacl; its descriptor asks for nothing at all.
        val aetherShaped = candidate.copy(
            files = listOf(fabricFile("Zoomify-2.13.3.jar", "2.13.3", listOf("yacl")))
        )
        val forgeOnly = ProjectFiles(
            platform = "Modrinth", slug = "yacl", projectUrl = "https://modrinth.com/mod/yacl",
            clientSide = DeclaredSupport.UNKNOWN, serverSide = DeclaredSupport.UNKNOWN,
            files = listOf(
                ModFile(
                    "yacl-forge-3.6.6.jar", setOf("Forge"), setOf(fabricRelease),
                    "https://cdn/yacl-forge-3.6.6.jar", null, emptyList(), "3.6.6"
                )
            )
        )
        val declaresNothing = descriptors + mapOf("Zoomify-2.13.3.jar" to """"id":"zoomify","environment":"client"""")

        val prepared = BootVerifier(
            apiWrapper = apiWrapper,
            platform = platformServing(forgeOnly),
            httpDownloader = downloaderFor(declaresNothing),
            loaderVersionPolicy = unbootableLoaderVersion,
            workDirectory = workDir
        ).prepareBootPack(aetherShaped, "Fabric")

        val detail = Assertions.assertInstanceOf(BootVerifier.Prepared.Failed::class.java, prepared).detail
        Assertions.assertFalse(
            detail.contains("Required dependency unavailable"),
            "the jar does not ask for yacl, so its absence must not refuse the boot: $detail"
        )
        Assertions.assertTrue(
            detail.contains("Server-pack generation failed"),
            "staging should have got as far as generation, which this harness makes fail: $detail"
        )
    }

    // --- the reason predicate itself, directly ------------------------------------------------------

    /** Nothing excluded: whatever went wrong, staging did not do it. Short-circuits without a second pick. */
    @Test
    fun nothingExcludedIsAlwaysTheProjectsOwnGap() {
        val project = ProjectFiles(
            platform = "Modrinth", slug = "yacl", projectUrl = "https://modrinth.com/mod/yacl",
            clientSide = DeclaredSupport.UNKNOWN, serverSide = DeclaredSupport.UNKNOWN,
            files = listOf(fabricFile("yacl-3.6.6.jar", "3.6.6"))
        )

        Assertions.assertEquals(
            UnmetReason.NO_USABLE_FILE,
            BootVerifier.backtrackReason(project, emptySet(), "Fabric", fabricRelease)
        )
    }

    /**
     * Exclusions exist, but the project publishes nothing this boot could have used **anyway** — so they are
     * not what stood in the way, and the refusal must still blame the project.
     *
     * Note the precondition this respects: `backtrackReason` is asked only once the *filtered* pick has
     * already failed. Handing it a project that would still have yielded a file asks about a state it is
     * never called in, and the answer is meaningless rather than wrong.
     */
    @Test
    fun exclusionsDoNotGetTheBlameWhenNothingWasUsableEither() {
        val forgeOnly = ProjectFiles(
            platform = "Modrinth", slug = "yacl", projectUrl = "https://modrinth.com/mod/yacl",
            clientSide = DeclaredSupport.UNKNOWN, serverSide = DeclaredSupport.UNKNOWN,
            files = listOf(
                ModFile(
                    "yacl-forge-3.6.6.jar", setOf("Forge"), setOf(fabricRelease),
                    "https://cdn/yacl-forge-3.6.6.jar", null, emptyList(), "3.6.6"
                )
            )
        )

        Assertions.assertEquals(
            UnmetReason.NO_USABLE_FILE,
            BootVerifier.backtrackReason(forgeOnly, setOf("yacl-3.4.2.jar"), "Fabric", fabricRelease),
            "no Fabric build exists with or without the exclusions"
        )
    }

    /** And the case the reason exists for: the exclusions are the only thing standing in the way. */
    @Test
    fun anExclusionThatRemovesEveryUsableBuildIsOurOwnDoing() {
        val project = ProjectFiles(
            platform = "Modrinth", slug = "yacl", projectUrl = "https://modrinth.com/mod/yacl",
            clientSide = DeclaredSupport.UNKNOWN, serverSide = DeclaredSupport.UNKNOWN,
            files = listOf(fabricFile("yacl-3.6.6.jar", "3.6.6"))
        )

        Assertions.assertEquals(
            UnmetReason.DROPPED_BY_BACKTRACK,
            BootVerifier.backtrackReason(project, setOf("yacl-3.6.6.jar"), "Fabric", fabricRelease)
        )
    }

    /**
     * **The expensive one.** The project's only build demands a Fabric API the pack cannot hold, so
     * `DependencyBacktrack` demotes it — and there is nothing older to fall back to. Staging then refuses
     * with the *same* sentence as a project that published nothing, which is how 1014 re-stagings a day
     * hid behind 47 verdicts reading "Required dependency unavailable".
     */
    @Test
    fun aDependencyDroppedByBacktrackingDoesNotReadAsAnAbsentBuild(@TempDir workDir: File) {
        val onlyTheDemandingBuild = ProjectFiles(
            platform = "Modrinth", slug = "yacl", projectUrl = "https://modrinth.com/mod/yacl",
            clientSide = DeclaredSupport.UNKNOWN, serverSide = DeclaredSupport.UNKNOWN,
            files = listOf(fabricFile("yet_another_config_lib_v3-3.6.6.jar", "3.6.6+1.20.6"))
        )

        val detail = refusalFor(onlyTheDemandingBuild, descriptors, workDir)

        Assertions.assertTrue(
            detail.contains("yacl (every usable build was dropped resolving a version conflict)"),
            "staging excluded the build itself, so reporting the project as publishing nothing is false: $detail"
        )
    }
}
