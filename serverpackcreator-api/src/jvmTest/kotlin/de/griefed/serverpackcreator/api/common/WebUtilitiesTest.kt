package de.griefed.serverpackcreator.api.common

import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.api.utilities.common.WebUtilities
import de.griefed.serverpackcreator.api.utilities.common.deleteQuietly
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File
import java.net.URI

class WebUtilitiesTest internal constructor() {
    private var webUtilities: WebUtilities =
        ApiWrapper.api(File("src/jvmTest/resources/serverpackcreator.properties")).webUtilities

    @Suppress("SpellCheckingInspection")
    @Test
    fun downloadFileTest() {
        val file = File("tests/Fabric-Server-Launcher.jar")
        try {
            webUtilities.downloadFile(
                file,
                URI("https://meta.fabricmc.net/v2/versions/loader/1.18.1/0.12.12/0.10.2/server/jar").toURL()
            )
        } catch (ignored: Exception) {
        }
        Assertions.assertTrue(file.exists())
        file.deleteQuietly()
        val nested = File("tests/some_foooooolder/foooobar/Fabric-Server-Launcher.jar")
        try {
            webUtilities.downloadFile(
                nested,
                URI("https://meta.fabricmc.net/v2/versions/loader/1.18.1/0.12.12/0.10.2/server/jar").toURL()
            )
        } catch (ignored: Exception) {
        }
        Assertions.assertTrue(nested.exists())
        nested.deleteQuietly()
    }
}