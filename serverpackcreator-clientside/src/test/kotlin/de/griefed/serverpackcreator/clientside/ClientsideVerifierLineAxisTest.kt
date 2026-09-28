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
 * Pins that a report is one verdict per **Minecraft version-line**, not one per modloader — the axis itself,
 * asserted through `ClientsideVerifier.report` rather than through the selector it delegates to.
 *
 * `BootCandidateSelector.pickGrindTargets` has its own guards; this is the join, which is the part no unit
 * test of either side can see. The fixture is `CurseForge/aether`'s real shape, read from the live API on
 * 2026-09-11: under the loader axis it produced three verdicts of which two were about Minecraft 1.21.1,
 * and its 1.12.2 build — a wholly separate codebase — was never looked at under any loader.
 *
 * @author Griefed
 */
internal class ClientsideVerifierLineAxisTest {

    private val apiWrapper = ApiWrapper.api(File("build/resources/test/serverpackcreator.properties"))

    private val aetherFiles = listOf(
        ModFile("aether-1.21.1-1.5.10-neoforge.jar", setOf("NeoForge"), setOf("1.21.1"), "https://cdn/a", null, emptyList()),
        ModFile("aether-1.21.1-1.5.11-fabric.jar", setOf("Fabric"), setOf("1.21.1"), "https://cdn/b", null, emptyList()),
        ModFile("aether-1.12.2-v1.5.4.1.jar", setOf("Forge"), setOf("1.12.2"), "https://cdn/c", null, emptyList()),
        ModFile(
            "aether-1.20.1-1.5.2-neoforge.jar", setOf("NeoForge", "Forge"), setOf("1.20.1"), "https://cdn/e", null,
            emptyList()
        )
    )

    private fun platform(files: List<ModFile>) = object : ModPlatform {
        override val name: String = "CurseForge"
        override fun handles(projectUrl: String): Boolean = true
        override fun resolve(projectUrl: String): ProjectFiles = ProjectFiles(
            platform = name, slug = "aether", projectUrl = projectUrl,
            clientSide = DeclaredSupport.UNKNOWN, serverSide = DeclaredSupport.UNKNOWN, files = files
        )

        override fun resolveDependency(nativeRef: String, minecraftVersion: String?): ProjectFiles? = null
    }

    private fun report(files: List<ModFile>, workDir: File, policy: MinecraftLinePolicy = MinecraftLinePolicy()) =
        ClientsideVerifier(
            platforms = listOf(platform(files)),
            metadataScanner = MetadataScanner(apiWrapper.modScanner),
            // No download, so the scan degrades and only the axis is under test.
            jarDownloader = JarDownloader { _, _ -> null },
            workDirectory = workDir,
            linePolicy = policy
        ).report("https://www.curseforge.com/minecraft/mc-mods/aether")

    /** One row per line, newest line first, each naming the loader that line was ground under. */
    @Test
    fun oneVerdictPerMinecraftLineNotOnePerLoader(@TempDir workDir: File) {
        val perLine = report(aetherFiles, workDir).perTarget

        Assertions.assertEquals(
            listOf("1.21" to "NeoForge", "1.20" to "NeoForge", "1.12" to "Forge"),
            perLine.map { it.minecraftLine to it.loader },
            "aether publishes for three Minecraft lines and three loaders; the rows are the lines"
        )
    }

    /** Each row carries the exact version its pack was staged at, not only the line it belongs to. */
    @Test
    fun everyVerdictNamesTheVersionItWasStagedAt(@TempDir workDir: File) {
        Assertions.assertEquals(
            listOf("1.21.1", "1.20.1", "1.12.2"),
            report(aetherFiles, workDir).perTarget.map { it.minecraftVersion }
        )
    }

    /**
     * **The same loader may hold several of a project's rows**, which is the property that made the attempt
     * directory need the line in its name: two NeoForge targets staging into one directory would wipe each
     * other's pack and console mid-run.
     */
    @Test
    fun oneLoaderCanOwnSeveralRows(@TempDir workDir: File) {
        val neoForgeRows = report(aetherFiles, workDir).perTarget.filter { it.loader == "NeoForge" }

        Assertions.assertEquals(
            listOf("1.21", "1.20"), neoForgeRows.map { it.minecraftLine },
            "one loader, two lines, two rows -- and therefore two scratch directories"
        )
    }

    /**
     * The published entry stays the **loader's whole history**, not the line's. `/as-properties` matches it
     * with `startsWith`, so narrowing it to one line would publish a pattern that misses every build it was
     * not shown — and it is also what lets two lines of one loader disprove each other's crash, since the
     * disproof compares entries.
     */
    @Test
    fun twoLinesOfOneLoaderShareTheirPublishedEntry(@TempDir workDir: File) {
        val entries = report(aetherFiles, workDir).perTarget
            .filter { it.loader == "NeoForge" }
            .map { it.suggestedEntry }

        Assertions.assertEquals(1, entries.distinct().size, "one loader, one published pattern: $entries")
    }

    /** The policy bounds the work: a project publishing for many lines is not ground on all of them. */
    @Test
    fun thePolicyDecidesHowManyRowsAProjectGets(@TempDir workDir: File) {
        val perLine = report(aetherFiles, workDir, MinecraftLinePolicy(newestCount = 1, anchors = emptySet()))

        Assertions.assertEquals(listOf("1.21"), perLine.perTarget.map { it.minecraftLine })
    }

    /** A project publishing nothing bootable yields no rows rather than an empty-looking one. */
    @Test
    fun aProjectWithNoBootableLineYieldsNoVerdicts(@TempDir workDir: File) {
        val noVersions = listOf(
            ModFile("mod.jar", setOf("Forge"), emptySet(), "https://cdn/x", null, emptyList())
        )

        Assertions.assertEquals(emptyList<GrindTargetVerdict>(), report(noVersions, workDir).perTarget)
    }
}
