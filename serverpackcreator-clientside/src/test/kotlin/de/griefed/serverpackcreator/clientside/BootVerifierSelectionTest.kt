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

/**
 * Characterizes [BootVerifier.prepareBootPack]'s candidate selection against a real (offline)
 * `ApiWrapper`: the `minecraftAcceptable` gate (the grinder's image-supported-Java hook) and the
 * release-only filter are both AND-ed into the choice. Staging stops at the download step here — the
 * injected downloaders return `null` (no network) — so these tests pin *which combination is selected*
 * without ever booting a server. The boot/verdict half is covered by `BootVerifierRunPreparedTest`.
 */
internal class BootVerifierSelectionTest {
    private val apiWrapper = ApiWrapper.api(File("src/test/resources/serverpackcreator.properties"))
    private val resolver = LoaderVersionResolver(apiWrapper.versionMeta)

    /** A real Forge-capable server release from the cached metadata, so the test stays version-agnostic. */
    private val forgeRelease = apiWrapper.versionMeta.minecraft.serverReleases()
        .map { it.minecraftVersion }
        .first { resolver.latest("Forge", it) != null }

    /** Downloaders that never fetch — selection happens before download, and we want download to fail fast. */
    private val noNetworkDownloader = JarDownloader { _, _ -> null }

    /** The platform is only consulted for dependency resolution, which these tests never reach. */
    private val unusedPlatform = object : ModPlatform {
        override fun handles(projectUrl: String): Boolean = false
        override fun resolve(projectUrl: String): ProjectFiles = error("resolve must not be called")
        override fun resolveDependency(nativeRef: String): ProjectFiles? = error("resolveDependency must not be called")
    }

    private fun verifier(workDir: File, minecraftAcceptable: (String) -> Boolean) = BootVerifier(
        apiWrapper = apiWrapper,
        platform = unusedPlatform,
        httpDownloader = noNetworkDownloader,
        browserDownloader = noNetworkDownloader,
        loaderVersionPolicy = resolver,
        workDirectory = workDir,
        minecraftAcceptable = minecraftAcceptable
    )

    /** A single-file Forge project targeting the given Minecraft versions. */
    private fun forgeProject(vararg minecraftVersions: String) = ProjectFiles(
        platform = "Modrinth",
        slug = "testmod",
        projectUrl = "https://modrinth.com/mod/testmod",
        clientSide = Sideness.UNKNOWN,
        serverSide = Sideness.UNKNOWN,
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
}
