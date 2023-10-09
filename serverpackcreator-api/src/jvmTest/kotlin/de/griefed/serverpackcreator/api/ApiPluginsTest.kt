package de.griefed.serverpackcreator.api

import de.griefed.serverpackcreator.api.plugins.configurationhandler.ConfigCheckExtension
import de.griefed.serverpackcreator.api.plugins.serverpackhandler.PostGenExtension
import de.griefed.serverpackcreator.api.plugins.serverpackhandler.PreGenExtension
import de.griefed.serverpackcreator.api.plugins.serverpackhandler.PreZipExtension
import de.griefed.serverpackcreator.api.plugins.swinggui.ConfigPanelExtension
import de.griefed.serverpackcreator.api.plugins.swinggui.TabExtension
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.xml.sax.SAXException
import java.io.File
import java.io.IOException
import javax.xml.parsers.ParserConfigurationException

class ApiPluginsTest {
    private val apiPlugins: ApiPlugins =
        ApiWrapper.api(File("src/jvmTest/resources/serverpackcreator.properties")).apiPlugins!!

    @Test
    @Throws(IOException::class, ParserConfigurationException::class, SAXException::class)
    fun test() {
        try {
            File("src/jvmTest/resources/testresources/plugins").copyRecursively(File("tests/plugins"), true)
        } catch (ignored: Exception) {
        }
        Assertions.assertNotNull(apiPlugins)
        apiPlugins.loadPlugins()
        apiPlugins.startPlugins()
        for (plugin in apiPlugins.plugins) {
            Assertions.assertTrue(
                apiPlugins.getAllExtensionsOfPlugin(plugin, PostGenExtension::class.java).isNotEmpty()
            )
            Assertions.assertTrue(apiPlugins.getAllExtensionsOfPlugin(plugin, TabExtension::class.java).isNotEmpty())
            Assertions.assertTrue(apiPlugins.getAllExtensionsOfPlugin(plugin, PreGenExtension::class.java).isNotEmpty())
            Assertions.assertTrue(apiPlugins.getAllExtensionsOfPlugin(plugin, PreZipExtension::class.java).isNotEmpty())
            Assertions.assertTrue(
                apiPlugins.getAllExtensionsOfPlugin(plugin, ConfigCheckExtension::class.java).isNotEmpty()
            )
            Assertions.assertTrue(
                apiPlugins.getAllExtensionsOfPlugin(plugin, ConfigPanelExtension::class.java).isNotEmpty()
            )
        }
    }
}