package de.griefed.serverpackcreator.api.common

import de.griefed.serverpackcreator.api.utilities.common.FileUtilities
import de.griefed.serverpackcreator.api.utilities.common.deleteQuietly
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File
import java.io.IOException

class FileUtilitiesTest internal constructor() {

    @Test
    fun unzipArchiveTest() {
        val modpackDir = "src/test/resources/curseforge_tests"
        val zipFile = "src/test/resources/curseforge_tests/modpack.zip"
        FileUtilities.unzipArchive(zipFile, modpackDir)
        Assertions.assertTrue(
            File("src/test/resources/curseforge_tests/manifest.json").exists()
        )
        Assertions.assertTrue(
            File("src/test/resources/curseforge_tests/modlist.html").exists()
        )
        Assertions.assertTrue(
            File("src/test/resources/curseforge_tests/overrides").isDirectory
        )
        File("src/test/resources/curseforge_tests/manifest.json").deleteQuietly()
        File("src/test/resources/curseforge_tests/modlist.html").deleteQuietly()
        File("src/test/resources/curseforge_tests/overrides").deleteQuietly()
    }

    @Test
    @Throws(IOException::class)
    fun replaceFileTest() {
        val source = File("source.file")
        val destination = File("destination.file")
        source.createNewFile()
        destination.createNewFile()
        FileUtilities.replaceFile(source, destination)
        Assertions.assertFalse(source.exists())
        Assertions.assertTrue(destination.exists())
        destination.deleteQuietly()
    }

    /**
     * Pins that `unzipArchive` declares no checked exception to Java.
     *
     * It briefly carried `@Throws(IOException::class)`, which Kotlin callers cannot notice — Kotlin has no
     * checked exceptions — while `javap` showed it emitting
     * `throws java.io.IOException`, so every existing **Java** caller of this published method stopped
     * compiling. That is a source-incompatible change inside a major version, and nothing in the suite
     * saw it: the behaviour the annotation was added for (letting an extraction failure surface) works
     * without it, so every functional guard stayed green.
     *
     * Asserted through reflection rather than by reading the source, because the annotation is exactly the
     * kind of thing that gets re-added as a tidy-up.
     */
    @Test
    fun unzipArchiveDeclaresNoCheckedExceptionToJavaCallers() {
        val method = FileUtilities.Companion::class.java
            .getMethod("unzipArchive", String::class.java, String::class.java)

        Assertions.assertArrayEquals(
            emptyArray<Class<*>>(), method.exceptionTypes,
            "unzipArchive declares ${method.exceptionTypes.map { it.simpleName }} to Java; a checked " +
                    "exception here breaks every existing Java caller of a published method"
        )
    }
}
