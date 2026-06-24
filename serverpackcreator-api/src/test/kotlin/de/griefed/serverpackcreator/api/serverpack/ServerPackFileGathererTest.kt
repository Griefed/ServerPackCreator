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
package de.griefed.serverpackcreator.api.serverpack

import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.api.config.InclusionSpecification
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Direct unit tests for [ServerPackFileGatherer], whose `getServerFiles` is a nine-arm `when` that
 * decides — per inclusion-specification — what ends up in the server pack. Existing characterization
 * tests cover explicit-files and regex-walking; this class exercises the remaining arms (global
 * filter, destination sub-branches, the dedicated "mods" arm, directory/file sources without a
 * destination), the inclusion/exclusion-filter logic in `runFilters`, the global
 * `excludeFileOrDirectory` check, and the lazy-mode whole-pack copy in `copyFiles`.
 */
internal class ServerPackFileGathererTest {

    private val api = ApiWrapper.api(File("src/test/resources/serverpackcreator.properties"))
    private val apiProperties = api.apiProperties
    private val gatherer = ServerPackFileGatherer(ModListCompiler(apiProperties, api.modScanner))

    private var originalAutoExclude = false

    @BeforeEach
    fun snapshotAutoExclude() {
        originalAutoExclude = apiProperties.isAutoExcludingModsEnabled
    }

    @AfterEach
    fun restoreAutoExclude() {
        apiProperties.isAutoExcludingModsEnabled = originalAutoExclude
    }

    /**
     * Builds a small modpack tree under [parent]: a top-level file, a config directory with two
     * files, and a mods directory with two jars. Returns the modpack directory.
     */
    private fun buildModpack(parent: File): File {
        val modpack = File(parent, "modpack")
        File(modpack, "config").mkdirs()
        File(modpack, "mods").mkdirs()
        File(modpack, "options.txt").writeText("option")
        File(modpack, "config/include-me.txt").writeText("keep")
        File(modpack, "config/exclude-me.txt").writeText("drop")
        File(modpack, "mods/keepme.jar").writeText("dummy")
        File(modpack, "mods/client.jar").writeText("dummy")
        return modpack
    }

    /**
     * Invokes the gatherer for a single inclusion with empty client/whitelist defaults.
     */
    private fun gather(
        inclusion: InclusionSpecification,
        modpackDir: String,
        destination: String,
        exclusions: MutableList<Regex> = mutableListOf()
    ): List<ServerPackFile> = gatherer.getServerFiles(
        inclusion, modpackDir, destination, exclusions, emptyList(), emptyList(), "1.20.1", "Forge"
    )

    /**
     * A global exclusion-filter (blank source + exclusion filter) gathers no files of its own but
     * registers its regex in the shared exclusions list for later filtering.
     */
    @Test
    fun globalExclusionFilterRegistersRegexAndGathersNothing(@TempDir tempDir: File) {
        val modpack = buildModpack(tempDir)
        val exclusions = mutableListOf<Regex>()
        val inclusion = InclusionSpecification("", null, null, "config")

        val files = gather(inclusion, modpack.absolutePath, File(tempDir, "out").absolutePath, exclusions)

        Assertions.assertTrue(files.isEmpty(), "A global filter must not gather files itself")
        Assertions.assertEquals(1, exclusions.size, "The exclusion regex must be registered")
    }

    /**
     * A global filter whose regex is invalid is swallowed: nothing is gathered and nothing is
     * registered.
     */
    @Test
    fun globalFilterWithInvalidRegexIsIgnored(@TempDir tempDir: File) {
        val modpack = buildModpack(tempDir)
        val exclusions = mutableListOf<Regex>()
        val inclusion = InclusionSpecification("", null, null, "[")

        val files = gather(inclusion, modpack.absolutePath, File(tempDir, "out").absolutePath, exclusions)

        Assertions.assertTrue(files.isEmpty())
        Assertions.assertTrue(exclusions.isEmpty(), "An invalid regex must not be registered")
    }

    /**
     * A file-source with a destination is copied as a single, renamed file into the destination.
     */
    @Test
    fun destinationWithFileSourceProducesSingleRenamedFile(@TempDir tempDir: File) {
        val modpack = buildModpack(tempDir)
        val destination = File(tempDir, "out").absolutePath
        val inclusion = InclusionSpecification("options.txt", "renamed.txt")

        val files = gather(inclusion, modpack.absolutePath, destination)

        Assertions.assertEquals(1, files.size)
        Assertions.assertEquals(File(destination, "renamed.txt"), files.first().destinationFile)
    }

    /**
     * A directory-source with a destination gathers the directory's files under that destination.
     */
    @Test
    fun destinationWithDirectorySourceGathersFilesUnderDestination(@TempDir tempDir: File) {
        val modpack = buildModpack(tempDir)
        val destination = File(tempDir, "out").absolutePath
        val inclusion = InclusionSpecification("config", "cfg")

        val files = gather(inclusion, modpack.absolutePath, destination)

        Assertions.assertTrue(files.any { it.sourceFile.name == "include-me.txt" })
        Assertions.assertTrue(
            files.all { it.destinationFile.absolutePath.contains(File.separator + "cfg") },
            "Gathered files must be destined under the specified destination 'cfg'"
        )
    }

    /**
     * A destination-bearing inclusion whose source does not exist anywhere still yields a single
     * (best-effort) entry rather than silently dropping the inclusion.
     */
    @Test
    fun destinationWithNonexistentSourceStillProducesEntry(@TempDir tempDir: File) {
        val modpack = buildModpack(tempDir)
        val destination = File(tempDir, "out").absolutePath
        val inclusion = InclusionSpecification("does-not-exist.txt", "ghost.txt")

        val files = gather(inclusion, modpack.absolutePath, destination)

        Assertions.assertEquals(1, files.size)
        Assertions.assertEquals(File(destination, "ghost.txt"), files.first().destinationFile)
    }

    /**
     * The dedicated "mods" arm compiles the mod-list: kept mods are gathered as-is, while a
     * clientside-excluded mod is gathered with a ".disabled" extension appended.
     */
    @Test
    fun modsSourceGathersKeptModsAndRenamesDisabled(@TempDir tempDir: File) {
        apiProperties.isAutoExcludingModsEnabled = false
        val modpack = buildModpack(tempDir)
        val destination = File(tempDir, "out").absolutePath
        val inclusion = InclusionSpecification("mods")

        val files = gatherer.getServerFiles(
            inclusion, modpack.absolutePath, destination, mutableListOf(),
            listOf("client"), emptyList(), "1.20.1", "Forge"
        )

        val destNames = files.map { it.destinationFile.name }
        Assertions.assertTrue(destNames.contains("keepme.jar"), "Kept mod must be gathered; got $destNames")
        Assertions.assertTrue(destNames.contains("client.jar.disabled"), "Excluded mod must be renamed to .disabled; got $destNames")
    }

    /**
     * A directory-source without a destination is gathered recursively under a directory named
     * after the source.
     */
    @Test
    fun directorySourceWithoutDestinationGathersRecursively(@TempDir tempDir: File) {
        val modpack = buildModpack(tempDir)
        val destination = File(tempDir, "out").absolutePath
        val inclusion = InclusionSpecification("config")

        val files = gather(inclusion, modpack.absolutePath, destination)

        Assertions.assertTrue(files.any { it.sourceFile.name == "include-me.txt" })
        Assertions.assertTrue(files.any { it.sourceFile.name == "exclude-me.txt" })
    }

    /**
     * A file-source without a destination is copied to a same-named file under the destination.
     */
    @Test
    fun fileSourceWithoutDestinationProducesSingleEntry(@TempDir tempDir: File) {
        val modpack = buildModpack(tempDir)
        val destination = File(tempDir, "out").absolutePath
        val inclusion = InclusionSpecification("options.txt")

        val files = gather(inclusion, modpack.absolutePath, destination)

        Assertions.assertEquals(1, files.size)
        Assertions.assertEquals("options.txt", files.first().sourceFile.name)
    }

    /**
     * An inclusion-filter keeps only the matching files of a directory-source.
     */
    @Test
    fun inclusionFilterKeepsOnlyMatchingFiles(@TempDir tempDir: File) {
        val modpack = buildModpack(tempDir)
        val destination = File(tempDir, "out").absolutePath
        val inclusion = InclusionSpecification("config", null, ".*include-me.*", null)

        val files = gather(inclusion, modpack.absolutePath, destination)

        Assertions.assertTrue(files.any { it.sourceFile.name == "include-me.txt" })
        Assertions.assertFalse(files.any { it.sourceFile.name == "exclude-me.txt" })
    }

    /**
     * An exclusion-filter removes the matching files of a directory-source while keeping the rest.
     */
    @Test
    fun exclusionFilterRemovesMatchingFiles(@TempDir tempDir: File) {
        val modpack = buildModpack(tempDir)
        val destination = File(tempDir, "out").absolutePath
        val inclusion = InclusionSpecification("config", null, null, ".*exclude-me.*")

        val files = gather(inclusion, modpack.absolutePath, destination)

        Assertions.assertTrue(files.any { it.sourceFile.name == "include-me.txt" })
        Assertions.assertFalse(files.any { it.sourceFile.name == "exclude-me.txt" })
    }

    /**
     * An invalid inclusion-filter regex is swallowed and treated as "no filter", so all files are
     * gathered.
     */
    @Test
    fun invalidInclusionFilterFallsBackToGatheringAll(@TempDir tempDir: File) {
        val modpack = buildModpack(tempDir)
        val destination = File(tempDir, "out").absolutePath
        val inclusion = InclusionSpecification("config", null, "[", null)

        val files = gather(inclusion, modpack.absolutePath, destination)

        Assertions.assertTrue(files.any { it.sourceFile.name == "include-me.txt" })
        Assertions.assertTrue(files.any { it.sourceFile.name == "exclude-me.txt" })
    }

    /**
     * The global exclusion check matches a path against the registered regexes relative to the
     * modpack directory.
     */
    @Test
    fun excludeFileOrDirectoryMatchesAgainstRegisteredRegex(@TempDir tempDir: File) {
        val modpack = buildModpack(tempDir)
        Assertions.assertTrue(
            gatherer.excludeFileOrDirectory(modpack.absolutePath, File(modpack, "config"), listOf(Regex("config")))
        )
        Assertions.assertFalse(
            gatherer.excludeFileOrDirectory(modpack.absolutePath, File(modpack, "options.txt"), listOf(Regex("config")))
        )
    }

    /**
     * Lazy-mode copies the entire modpack into the destination and returns an empty copied-files
     * list (no per-file accounting in lazy mode).
     */
    @Test
    fun lazyModeCopiesWholeModpack(@TempDir tempDir: File) {
        val modpack = buildModpack(tempDir)
        val destination = File(tempDir, "out")

        val copied = gatherer.copyFiles(
            modpack.absolutePath, arrayListOf(InclusionSpecification("lazy_mode")),
            emptyList(), emptyList(), "1.20.1", destination.absolutePath, "Forge", true
        )

        Assertions.assertTrue(copied.isEmpty(), "Lazy mode returns no per-file accounting")
        Assertions.assertTrue(File(destination, "options.txt").exists(), "Whole modpack must be copied in lazy mode")
        Assertions.assertTrue(File(destination, "config/include-me.txt").exists())
    }
}
