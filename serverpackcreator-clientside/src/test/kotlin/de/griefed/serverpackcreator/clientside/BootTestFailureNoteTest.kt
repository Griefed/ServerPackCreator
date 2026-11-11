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
import de.griefed.serverpackcreator.clientside.boot.BootVerifier
import de.griefed.serverpackcreator.clientside.metadata.MetadataScanner
import de.griefed.serverpackcreator.clientside.platform.DeclaredSupport
import de.griefed.serverpackcreator.clientside.platform.JarDownloader
import de.griefed.serverpackcreator.clientside.platform.ModFile
import de.griefed.serverpackcreator.clientside.platform.ModPlatform
import de.griefed.serverpackcreator.clientside.platform.ProjectFiles
import de.griefed.serverpackcreator.clientside.selection.BootCandidateSelector
import de.griefed.serverpackcreator.clientside.verdict.Verdict
import de.griefed.serverpackcreator.clientside.version.LoaderVersionResolver
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * A boot-test that throws is an `ERROR` row, and an `ERROR` is only actionable when it says what went wrong.
 * Driven through the real [ClientsideVerifier] and [BootVerifier]; the throw comes from the downloader, which
 * staging calls without a guard of its own.
 */
internal class BootTestFailureNoteTest {

    private val apiWrapper = ApiWrapper.api(File("build/resources/test/serverpackcreator.properties"))
    private val resolver = LoaderVersionResolver(apiWrapper.versionMeta)

    private val forgeRelease = apiWrapper.versionMeta.minecraft.serverReleases()
        .map { it.minecraftVersion }
        .filter { resolver.latest("Forge", it) != null }
        .maxWithOrNull(BootCandidateSelector.minecraftComparator)

    private fun platform(release: String) = object : ModPlatform {
        override val name: String = "CurseForge"
        override fun handles(projectUrl: String): Boolean = true
        override fun resolve(projectUrl: String): ProjectFiles = ProjectFiles(
            platform = "CurseForge",
            slug = "themod",
            projectUrl = projectUrl,
            clientSide = DeclaredSupport.UNKNOWN,
            serverSide = DeclaredSupport.UNKNOWN,
            files = listOf(ModFile("themod-$release-1.0.0.jar", setOf("Forge"), setOf(release), "https://cdn/themod.jar", null, emptyList()))
        )
        override fun resolveDependency(nativeRef: String, minecraftVersion: String?): ProjectFiles? = null
    }

    @Test
    fun aBootTestThatThrowsNamesTheFailureInItsNote(@TempDir workDir: File) {
        val release = forgeRelease
        Assumptions.assumeTrue(release != null, "the shipped manifest must offer a Forge-capable release")
        val failure = "Illegal character in path at index 23: https://cdn/the mod.jar"

        val report = ClientsideVerifier(
            platforms = listOf(platform(release!!)),
            metadataScanner = MetadataScanner(apiWrapper.modScanner),
            jarDownloader = JarDownloader { _, _ -> null },
            workDirectory = workDir,
            bootVerifierFactory = { modPlatform ->
                BootVerifier(
                    apiWrapper = apiWrapper,
                    platform = modPlatform,
                    httpDownloader = { _, _ -> throw IllegalArgumentException(failure) },
                    loaderVersionPolicy = resolver,
                    workDirectory = workDir
                )
            }
        ).report("https://www.curseforge.com/minecraft/mc-mods/themod")

        val row = report.perTarget.single()
        Assertions.assertEquals(Verdict.ERROR, row.verdict)
        Assertions.assertTrue(row.note.orEmpty().contains(failure), "the note must say why: ${row.note}")
    }
}
