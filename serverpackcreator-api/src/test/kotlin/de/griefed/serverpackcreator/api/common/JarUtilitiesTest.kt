package de.griefed.serverpackcreator.api.common

import de.griefed.serverpackcreator.api.utilities.common.JarAccessException
import de.griefed.serverpackcreator.api.utilities.common.JarUtilities
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files

class JarUtilitiesTest internal constructor() {

    /**
     * Copying a resource this module really ships must produce the resource, byte for byte.
     *
     * It used to ask for `banner.txt`, which lives in `serverpackcreator-app` and has never been on
     * this module's classpath, and then asserted only that the destination `isFile`. That was true
     * because the broken `copyFileFromJar` created the file before it went looking for the resource -
     * so the test passed against the defect, on an empty file, for as long as the defect existed.
     * Asserting the *content* is what makes it a copy test rather than a file-exists test.
     */
    @Test
    fun copyFileFromJarTest() {
        val destination = File(Files.createTempDirectory("spc-copy-from-jar").toFile(), "log4j2.xml")
        destination.deleteOnExit()
        val expected = JarUtilitiesTest::class.java.getResourceAsStream("/log4j2.xml")!!
            .bufferedReader().use { it.readText() }

        Assertions.assertTrue(
            JarUtilities.copyFileFromJar("log4j2.xml", destination, JarUtilitiesTest::class.java),
            "copying a resource that is in the jar must report success"
        )
        Assertions.assertEquals(expected, destination.readText(), "the copy must be the resource")
    }

    /**
     * A resource that is not in the jar must fail loudly and leave nothing behind.
     *
     * It used to do neither. `copyFileFromJar` created the destination *before* resolving the stream
     * and then wrote it with `it?.transferTo(out)`, so a missing resource was swallowed by the safe
     * call, the file existed, and the function reported success. `ApiWrapper.stageOne()` staged
     * `default_java_template.bat` that way - a resource that has never existed - and recreated a
     * 0-byte file on every single launch, in every test home, with nothing logged.
     */
    @Test
    fun copyingAResourceThatIsNotInTheJarFailsAndLeavesNoFileBehind() {
        val destination = File(Files.createTempDirectory("spc-missing-resource").toFile(), "not-in-the-jar.bat")
        destination.deleteOnExit()

        Assertions.assertThrows(JarAccessException::class.java) {
            JarUtilities.copyFileFromJar(
                "de/griefed/resources/server_files/this_resource_has_never_existed.bat",
                destination,
                JarUtilitiesTest::class.java
            )
        }
        Assertions.assertFalse(
            destination.exists(),
            "a resource that could not be read must not leave an empty file at $destination"
        )
    }

    /**
     * The directory-taking overload has to answer the same way, because it had the same hole.
     *
     * Both spellings of `copyFileFromJar` created the destination before resolving the stream and wrote
     * it with `it?.transferTo(out)`. Fixing the one `ApiWrapper` happens to call would have left the one
     * the GUI's delete-watcher calls still handing back an empty file and a `true`.
     */
    @Test
    fun copyingAResourceThatIsNotInTheJarIntoADirectoryFailsAndLeavesNoFileBehind() {
        val directory = Files.createTempDirectory("spc-missing-resource-directory").toFile()
        directory.deleteOnExit()

        Assertions.assertThrows(JarAccessException::class.java) {
            JarUtilities.copyFileFromJar(
                "this_resource_has_never_existed.bat",
                JarUtilitiesTest::class.java,
                directory.absolutePath
            )
        }
        Assertions.assertEquals(
            emptyList<String>(),
            directory.list()?.toList() ?: emptyList<String>(),
            "a resource that could not be read must not leave an empty file in $directory"
        )
    }

    @Test
    fun systemInformationTest() {
        val system: HashMap<String, String> = JarUtilities.jarInformation(JarUtilitiesTest::class.java)
        Assertions.assertNotNull(system)
        Assertions.assertNotNull(system["jarPath"])
        Assertions.assertTrue(system["jarPath"]!!.isNotEmpty())
        Assertions.assertNotNull(system["jarName"])
        Assertions.assertTrue(system["jarName"]!!.isNotEmpty())
        Assertions.assertNotNull(system["javaVersion"])
        Assertions.assertTrue(system["javaVersion"]!!.isNotEmpty())
        Assertions.assertNotNull(system["osArch"])
        Assertions.assertTrue(system["osArch"]!!.isNotEmpty())
        Assertions.assertNotNull(system["osName"])
        Assertions.assertTrue(system["osName"]!!.isNotEmpty())
        Assertions.assertNotNull(system["osVersion"])
        Assertions.assertTrue(system["osVersion"]!!.isNotEmpty())
    }

    @Test
    fun copyFolderFromJarTest() {
        try {
            JarUtilities.copyFolderFromJar(
                JarUtilitiesTest::class.java,
                "/de/griefed/resources/manifests",
                "tests/manifestTest",
                "",
                ".*\\.(xml|json)".toRegex()
            )
        } catch (ignored: Exception) {
        }
        Assertions.assertTrue(File("tests/manifestTest").isDirectory)
        Assertions.assertTrue(File("tests/manifestTest/fabric-installer-manifest.xml").isFile)
        Assertions.assertTrue(File("tests/manifestTest/mcserver").isDirectory)
        Assertions.assertTrue(File("tests/manifestTest/mcserver/1.8.2.json").isFile)
    }
}