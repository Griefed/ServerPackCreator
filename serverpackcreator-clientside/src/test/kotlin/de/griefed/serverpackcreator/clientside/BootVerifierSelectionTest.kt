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
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

/**
 * Characterizes [BootVerifier.prepareBootPack]'s candidate selection against a real (offline)
 * `ApiWrapper`: the `minecraftAcceptable` gate (the grinder's image-supported-Java hook) and the
 * release-only filter are both AND-ed into the choice. Staging stops at the download step here — the
 * injected downloaders return `null` (no network) — so these tests pin *which combination is selected*
 * without ever booting a server. The boot/verdict half is covered by `BootVerifierRunPreparedTest`.
 */
internal class BootVerifierSelectionTest {
    private val apiWrapper = ApiWrapper.api(File("build/resources/test/serverpackcreator.properties"))
    private val resolver = LoaderVersionResolver(apiWrapper.versionMeta)

    /** A real Forge-capable server release from the cached metadata, so the test stays version-agnostic. */
    private val forgeRelease = apiWrapper.versionMeta.minecraft.serverReleases()
        .map { it.minecraftVersion }
        .first { resolver.latest("Forge", it) != null }

    /** Downloaders that never fetch — selection happens before download, and we want download to fail fast. */
    private val noNetworkDownloader = JarDownloader { _, _ -> null }

    /** The platform is only consulted for dependency resolution, which these tests never reach. */
    private val unusedPlatform = object : ModPlatform {
        // Deliberately not a real platform's spelling: `platformRefFor` maps a manifest mod-id per platform,
        // and a name no platform uses makes it resolve nothing — which is what "never reached" should mean.
        override val name: String = "unused"
        override fun handles(projectUrl: String): Boolean = false
        override fun resolve(projectUrl: String): ProjectFiles = error("resolve must not be called")
        override fun resolveDependency(nativeRef: String, minecraftVersion: String?): ProjectFiles? =
            error("resolveDependency must not be called")
    }

    private fun verifier(workDir: File, minecraftAcceptable: (String) -> Boolean) = BootVerifier(
        apiWrapper = apiWrapper,
        platform = unusedPlatform,
        httpDownloader = noNetworkDownloader,
        loaderVersionPolicy = resolver,
        workDirectory = workDir,
        minecraftAcceptable = minecraftAcceptable
    )

    /** A single-file Forge project targeting the given Minecraft versions. */
    private fun forgeProject(vararg minecraftVersions: String) = ProjectFiles(
        platform = "Modrinth",
        slug = "testmod",
        projectUrl = "https://modrinth.com/mod/testmod",
        clientSide = DeclaredSupport.UNKNOWN,
        serverSide = DeclaredSupport.UNKNOWN,
        files = listOf(ModFile("testmod.jar", setOf("Forge"), minecraftVersions.toSet(), "https://cdn/testmod.jar", null, emptyList()))
    )

    /**
     * The image-supported-Java gate is AND-ed in: even though [forgeRelease] passes the release filter
     * and has an available loader, a gate that rejects every version leaves no bootable combination.
     */
    @Test
    fun rejectsWhenTheMinecraftAcceptableGateRejectsEveryCandidate(@TempDir workDir: File) {
        val prepared = verifier(workDir) { false }.prepareBootPack(forgeProject(forgeRelease), "Forge")
        Assertions.assertTrue(prepared is BootVerifier.Prepared.Failed)
        Assertions.assertTrue(
            (prepared as BootVerifier.Prepared.Failed).detail.contains("No bootable"),
            "the gate must short-circuit selection, was: ${prepared.detail}"
        )
    }

    /**
     * With an accept-all gate a real release passes selection, so staging advances **past** selection to
     * the download — proven by the failure being the download message rather than "No bootable …".
     */
    @Test
    fun acceptsARealReleaseAndAdvancesToDownload(@TempDir workDir: File) {
        val prepared = verifier(workDir) { true }.prepareBootPack(forgeProject(forgeRelease), "Forge")
        Assertions.assertTrue(prepared is BootVerifier.Prepared.Failed)
        val detail = (prepared as BootVerifier.Prepared.Failed).detail
        Assertions.assertTrue(detail.contains("Could not download"), "selection should have passed; failed with: $detail")
    }

    /**
     * Two candidates sharing a slug across platforms must stage into separate directories. Staging *wipes*
     * the directory it is about to use, and the grinder runs the two platform candidates in parallel, so a
     * shared name lets one run delete the pack another is booting from — see `AttemptStagingIsolationTest`
     * for the `creativecore` evidence. Executed rather than asserted on the name alone: both calls run real
     * staging as far as the download, which is what creates the directory on disk.
     */
    @Test
    fun theSameSlugOnTwoPlatformsStagesIntoSeparateDirectories(@TempDir workDir: File) {
        verifier(workDir) { true }.prepareBootPack(forgeProject(forgeRelease), "Forge")
        verifier(workDir) { true }.prepareBootPack(forgeProject(forgeRelease).copy(platform = "CurseForge"), "Forge")

        Assertions.assertEquals(
            listOf(
                AttemptDirectory.nameFor("CurseForge", "testmod", "Forge"),
                AttemptDirectory.nameFor("Modrinth", "testmod", "Forge")
            ).sorted(),
            workDir.listFiles()?.map { it.name }?.sorted() ?: emptyList<String>(),
            "the second platform's staging must not have wiped and reused the first's directory"
        )
    }

    /**
     * The release-only filter rejects a project that targets only pre-releases — a `-pre`/`-rc` is not in
     * `serverReleases()`, so no bootable combination is found.
     */
    @Test
    fun rejectsAProjectThatTargetsOnlyNonReleaseVersions(@TempDir workDir: File) {
        val prepared = verifier(workDir) { true }.prepareBootPack(forgeProject("1.20.1-pre1", "1.19-rc2"), "Forge")
        Assertions.assertTrue(prepared is BootVerifier.Prepared.Failed)
        Assertions.assertTrue(
            (prepared as BootVerifier.Prepared.Failed).detail.contains("No bootable"),
            "pre-releases must be filtered out, was: ${prepared.detail}"
        )
    }

    /**
     * The two newest Forge-capable releases in the cached metadata, newest first — the stand-ins for
     * JEI's 1.21.1 and 1.21. Derived rather than hardcoded so this stays version-agnostic as the shipped
     * manifest moves.
     */
    private val twoForgeReleases = apiWrapper.versionMeta.minecraft.serverReleases()
        .map { it.minecraftVersion }
        .filter { resolver.latest("Forge", it) != null }
        .sortedWith { left, right -> BootCandidateSelector.minecraftComparator.compare(right, left) }
        .take(2)

    /**
     * A downloader that writes a real jar carrying a real `META-INF/mods.toml` declaring [versionRange]
     * as its `minecraft` dependency, so the *actual* `ForgeTomlScanner` reads it. Nothing is faked past
     * the network boundary: the constraint travels the same path a downloaded JEI jar's would.
     */
    private fun tomlJarDownloader(versionRange: String) = JarDownloader { modFile, targetDirectory ->
        File(targetDirectory, modFile.fileName).also { jar ->
            targetDirectory.mkdirs()
            JarOutputStream(jar.outputStream()).use { out ->
                out.putNextEntry(JarEntry("META-INF/mods.toml"))
                out.write(
                    ("""
                    modLoader="javafml"
                    loaderVersion="[1,)"
                    license="MIT"
                    [[mods]]
                    modId="testmod"
                    version="1.0.0"
                    [[dependencies.testmod]]
                        modId="minecraft"
                        mandatory=true
                        versionRange="$versionRange"
                        ordering="NONE"
                        side="BOTH"
                    """.trimIndent()).toByteArray()
                )
                out.closeEntry()
            }
        }
    }

    /**
     * **The JEI regression, end to end.** A project tagged for both releases, whose jar declares a range
     * excluding the newer one, must not be thrown away: staging picks the newer, the descriptor gate
     * contradicts it, and the verifier has to re-select the newest version the jar *does* accept rather
     * than refuse outright.
     *
     * Refusing costs a `BootResult.INCONCLUSIVE`, which overwrites a decisive verdict — the same harm
     * shape as the missing-runtime-image outage, permanent instead of windowed.
     */
    @Test
    fun aJarWhoseRangeExcludesTheNewestIsStagedOnAVersionItAccepts(@TempDir workDir: File) {
        Assumptions.assumeTrue(twoForgeReleases.size == 2, "needs two Forge-capable releases in the manifest")
        val (newer, older) = twoForgeReleases
        val verifier = BootVerifier(
            apiWrapper = apiWrapper,
            platform = unusedPlatform,
            httpDownloader = tomlJarDownloader("[$older, $newer)"),
            loaderVersionPolicy = resolver,
            workDirectory = workDir,
            minecraftAcceptable = { true }
        )

        val prepared = verifier.prepareBootPack(forgeProject(older, newer), "Forge")

        val detail = (prepared as? BootVerifier.Prepared.Failed)?.detail.orEmpty()
        Assertions.assertFalse(
            detail.contains("Refusing to boot"),
            "the jar accepts $older and the platform tags it, so the boot must not be refused: $detail"
        )
        Assertions.assertTrue(
            prepared is BootVerifier.Prepared.Ready && prepared.minecraftVersion == older ||
                detail.contains(older),
            "staging must have moved to $older, not stopped at $newer; got: ${prepared::class.simpleName} $detail"
        )
    }
}
