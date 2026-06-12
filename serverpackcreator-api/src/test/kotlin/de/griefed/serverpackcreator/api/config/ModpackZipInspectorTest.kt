package de.griefed.serverpackcreator.api.config

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Tests for [ModpackZipInspector], extracted from ConfigurationHandler in refactor Phase 1c.
 * Pins the content-listing of modpack ZIP-archives against the valid modpack-fixture; the
 * validity-checks themselves are pinned by ConfigurationHandlerCharacterizationTest through
 * the facade.
 */
internal class ModpackZipInspectorTest {
    private val validModpackZip = File("src/test/resources/testresources/Survive_Create_Prosper_4_valid.zip")
    private val zipInspector = ModpackZipInspector()

    /**
     * Pins that base-directory listing yields the well-known modpack-directories with trailing
     * slashes.
     */
    @Test
    fun baseDirectoriesAreListedWithTrailingSlash() {
        val baseDirectories = zipInspector.getDirectoriesInModpackZipBaseDirectory(validModpackZip)
        Assertions.assertTrue(baseDirectories.contains("mods/"), "mods/ expected in $baseDirectories")
        Assertions.assertTrue(baseDirectories.contains("config/"), "config/ expected in $baseDirectories")
    }

    /**
     * Pins that directory- and file-listings are disjoint and together form the full listing.
     */
    @Test
    fun directoryAndFileListingsAreDisjointAndComplete() {
        val directories = zipInspector.getDirectoriesInModpackZip(validModpackZip)
        val files = zipInspector.getFilesInModpackZip(validModpackZip)
        val everything = zipInspector.getAllFilesAndDirectoriesInModpackZip(validModpackZip)
        Assertions.assertTrue(directories.isNotEmpty())
        Assertions.assertTrue(files.isNotEmpty())
        Assertions.assertTrue(directories.all { entry -> entry.endsWith("/") })
        Assertions.assertTrue(files.none { entry -> entry.endsWith("/") })
        Assertions.assertEquals(directories.size + files.size, everything.size)
    }
}
