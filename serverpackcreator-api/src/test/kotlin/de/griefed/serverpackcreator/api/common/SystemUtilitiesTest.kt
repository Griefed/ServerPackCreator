package de.griefed.serverpackcreator.api.common

import de.griefed.serverpackcreator.api.utilities.common.SystemUtilities
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

class SystemUtilitiesTest internal constructor() {
    @Test
    fun acquireJavaPathFromSystemTest() {
        Assertions.assertNotNull(SystemUtilities.acquireJavaPathFromSystem())
        Assertions.assertTrue(File(SystemUtilities.acquireJavaPathFromSystem()).exists())
        Assertions.assertTrue(File(SystemUtilities.acquireJavaPathFromSystem()).isFile)
        Assertions.assertTrue(
            SystemUtilities.acquireJavaPathFromSystem().replace("\\", "/").contains("bin/java")
        )
    }
}