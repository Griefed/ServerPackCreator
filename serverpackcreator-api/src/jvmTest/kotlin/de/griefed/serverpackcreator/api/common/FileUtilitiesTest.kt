package de.griefed.serverpackcreator.api.common

import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.api.utilities.common.FileUtilities
import de.griefed.serverpackcreator.api.utilities.common.deleteQuietly
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File
import java.io.IOException

class FileUtilitiesTest internal constructor() {
    private var fileUtilities: FileUtilities =
        ApiWrapper.api(File("src/jvmTest/resources/serverpackcreator.properties")).fileUtilities

    @Test
    fun unzipArchiveTest() {
        val modpackDir = "src/jvmTest/resources/curseforge_tests"
        val zipFile = "src/jvmTest/resources/curseforge_tests/modpack.zip"
        fileUtilities.unzipArchive(zipFile, modpackDir)
        Assertions.assertTrue(
            File("src/jvmTest/resources/curseforge_tests/manifest.json").exists()
        )
        Assertions.assertTrue(
            File("src/jvmTest/resources/curseforge_tests/modlist.html").exists()
        )
        Assertions.assertTrue(
            File("src/jvmTest/resources/curseforge_tests/overrides").isDirectory
        )
        File("src/jvmTest/resources/curseforge_tests/manifest.json").deleteQuietly()
        File("src/jvmTest/resources/curseforge_tests/modlist.html").deleteQuietly()
        File("src/jvmTest/resources/curseforge_tests/overrides").deleteQuietly()
    }

    @Test
    @Throws(IOException::class)
    fun replaceFileTest() {
        val source = File("source.file")
        val destination = File("destination.file")
        source.createNewFile()
        destination.createNewFile()
        fileUtilities.replaceFile(source, destination)
        Assertions.assertFalse(source.exists())
        Assertions.assertTrue(destination.exists())
        destination.deleteQuietly()
    }
}