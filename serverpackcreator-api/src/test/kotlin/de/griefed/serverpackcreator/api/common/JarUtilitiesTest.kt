package de.griefed.serverpackcreator.api.common

import de.griefed.serverpackcreator.api.utilities.common.JarUtilities
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

class JarUtilitiesTest internal constructor() {

    @Test
    fun copyFileFromJarTest() {
        JarUtilities.copyFileFromJar(
            "banner.txt", JarUtilitiesTest::class.java,
            File("tests").absolutePath
        )
        Assertions.assertTrue(File("tests/banner.txt").isFile)
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