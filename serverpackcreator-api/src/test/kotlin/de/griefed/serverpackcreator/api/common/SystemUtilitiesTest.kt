package de.griefed.serverpackcreator.api.common

import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.api.utilities.common.SystemUtilities
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

class SystemUtilitiesTest internal constructor() {
    private var systemUtilities: SystemUtilities =
        ApiWrapper.api(File("src/test/resources/serverpackcreator.properties")).systemUtilities

    @Test
    fun acquireJavaPathFromSystemTest() {
        Assertions.assertNotNull(systemUtilities.acquireJavaPathFromSystem())
        Assertions.assertTrue(File(systemUtilities.acquireJavaPathFromSystem()).exists())
        Assertions.assertTrue(File(systemUtilities.acquireJavaPathFromSystem()).isFile)
        Assertions.assertTrue(
            systemUtilities.acquireJavaPathFromSystem().replace("\\", "/").contains("bin/java")
        )
    }
}