package de.griefed.serverpackcreator.api

import de.griefed.serverpackcreator.api.utilities.common.readText
import net.lingala.zip4j.ZipFile
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.*

internal class ServerPackHandlerTest {
    private val configurationHandler =
        ApiWrapper.api(File("src/jvmTest/resources/serverpackcreator.properties")).configurationHandler!!
    private val serverPackHandler =
        ApiWrapper.api(File("src/jvmTest/resources/serverpackcreator.properties")).serverPackHandler!!
    private val versionMeta =
        ApiWrapper.api(File("src/jvmTest/resources/serverpackcreator.properties")).versionMeta!!
    private val applicationProperties =
        ApiWrapper.api(File("src/jvmTest/resources/serverpackcreator.properties")).apiProperties

    init {
        File("src/jvmTest/resources/custom_template.ps1").copyTo(
            File("tests/server_files/custom_template.ps1"),
            overwrite = true
        )
        File("src/jvmTest/resources/custom_template.sh").copyTo(
            File("tests/server_files/custom_template.sh"),
            overwrite = true
        )
    }

    @Suppress("SpellCheckingInspection")
    @Test
    fun forgeTest() {
        val packConfig = PackConfig()
        val ps1 = File(applicationProperties.serverPacksDirectory, "forge_tests/start.ps1")
        val shell = File(applicationProperties.serverPacksDirectory, "forge_tests/start.sh")
        val vars = File(applicationProperties.serverPacksDirectory, "forge_tests/variables.txt")
        val forgeZip = ZipFile(File(applicationProperties.serverPacksDirectory, "forge_tests_server_pack.zip"))
        val forgeDir = File(applicationProperties.serverPacksDirectory, "forge_tests")
        val props = File(forgeDir, "server.properties")
        configurationHandler.checkConfiguration(
            File("src/jvmTest/resources/testresources/spcconfs/serverpackcreator.conf"),
            packConfig
        )
        Assertions.assertTrue(serverPackHandler.run(packConfig))
        Assertions.assertTrue(File(forgeDir, "libraries").isDirectory)
        Assertions.assertTrue(File(forgeDir, "config").isDirectory)
        Assertions.assertTrue(File(forgeDir, "defaultconfigs").isDirectory)
        Assertions.assertTrue(File(forgeDir, "mods").isDirectory)
        Assertions.assertTrue(File(forgeDir, "scripts").isDirectory)
        Assertions.assertTrue(File(forgeDir, "seeds").isDirectory)
        Assertions.assertTrue(File(forgeDir, "minecraft_server.1.16.5.jar").isFile)
        Assertions.assertTrue(File(forgeDir, "forge.jar").isFile)
        Assertions.assertTrue(File(forgeDir, "server.properties").isFile)
        Assertions.assertTrue(File(forgeDir, "server-icon.png").isFile)
        Assertions.assertTrue(File(forgeDir, "start.ps1").isFile)
        Assertions.assertTrue(File(forgeDir, "start.sh").isFile)
        Assertions.assertTrue(File(forgeDir, "exclude_me").isDirectory)
        Assertions.assertTrue(
            File(
                forgeDir,
                "exclude_me/exclude_me_some_more/ICANSEEMYHOUSEFROMHEEEEEEEEEEEEERE"
            ).isFile
        )
        Assertions.assertTrue(File(applicationProperties.serverPacksDirectory, "forge_tests_server_pack.zip").isFile)
        Assertions.assertTrue(
            forgeZip.getInputStream(forgeZip.getFileHeader("start.sh")).readText().contains("JAVA=\"java\""),
            "Default Java setting not present!"
        )
        Assertions.assertTrue(
            forgeZip.getInputStream(forgeZip.getFileHeader("start.ps1")).readText().contains("\$Java = \"java\""),
            "Default Java setting not present!"
        )
        forgeZip.close()
        val variables = vars.readText()
        Assertions.assertTrue(variables.contains("MINECRAFT_VERSION=${packConfig.minecraftVersion}"))
        Assertions.assertTrue(variables.contains("MODLOADER=${packConfig.modloader}"))
        Assertions.assertTrue(variables.contains("MODLOADER_VERSION=${packConfig.modloaderVersion}"))
        Assertions.assertTrue(variables.contains("LEGACYFABRIC_INSTALLER_VERSION=${versionMeta.legacyFabric.releaseInstaller()}"))
        Assertions.assertTrue(variables.contains("FABRIC_INSTALLER_VERSION=${versionMeta.fabric.releaseInstaller()}"))
        Assertions.assertTrue(variables.contains("QUILT_INSTALLER_VERSION=${versionMeta.quilt.releaseInstaller()}"))
        Assertions.assertTrue(
            variables.contains(
                "MINECRAFT_SERVER_URL=${
                    versionMeta.minecraft.getServer(packConfig.minecraftVersion).get().url().get()
                }"
            )
        )
        Assertions.assertTrue(variables.contains("JAVA_ARGS=${packConfig.javaArgs}"))
        Assertions.assertTrue(variables.contains("JAVA=\"C\\:\\\\Program Files\\\\Java\\\\jdk1.8.0_301\\\\bin\\\\java.exe\""))

        val powerShell = ps1.readText()
        val shellScript = shell.readText()

        Assertions.assertTrue(
            powerShell.contains("Flynn = \"Now that's a big door\""),
            "Custom script settings not present!"
        )
        Assertions.assertTrue(
            powerShell.contains("SomeValue = \"something\""),
            "Custom script settings not present!"
        )
        Assertions.assertTrue(
            powerShell.contains("PraiseTheLamb = \"Kannema jajaja kannema\""),
            "Custom script settings not present!"
        )
        Assertions.assertTrue(
            powerShell.contains("AnotherValue = \"another\""),
            "Custom script settings not present!"
        )
        Assertions.assertTrue(
            powerShell.contains("Hello = \"Is it me you are looking foooooor\""),
            "Custom script settings not present!"
        )
        Assertions.assertTrue(
            shellScript.contains("FLYNN=\"Now that's a big door\""),
            "Custom script settings not present!"
        )
        Assertions.assertTrue(
            shellScript.contains("SOME_VALUE=\"something\""),
            "Custom script settings not present!"
        )
        Assertions.assertTrue(
            shellScript.contains("PRAISE_THE_LAMB=\"Kannema jajaja kannema\""),
            "Custom script settings not present!"
        )
        Assertions.assertTrue(
            shellScript.contains("ANOTHER_VALUE=\"another\""),
            "Custom script settings not present!"
        )
        Assertions.assertTrue(
            shellScript.contains("HELLO=\"Is it me you are looking foooooor\""),
            "Custom script settings not present!"
        )
        Assertions.assertFalse(File(forgeDir, "exclude_me/I_dont_want_to_be_included.file").isFile)
        Assertions.assertFalse(File(forgeDir, "exclude_me/exclude_me_some_more/I_dont_want_to_be_included.file").isFile)
        Assertions.assertFalse(File(forgeDir, "exclude_me/exclude_me_some_more/some_more_dirs_to_exclude").isDirectory)
        Assertions.assertFalse(
            File(
                forgeDir,
                "exclude_me/exclude_me_some_more/some_more_dirs_to_exclude/I_dont_want_to_be_included.file"
            ).isFile
        )
        Assertions.assertFalse(File(forgeDir, "exclude_me/exclude_me_some_more/dont_include_me_either.ogg").isFile)
        try {
            File("src/jvmTest/resources/testresources/server_pack.zip").copyTo(
                File(
                    applicationProperties.serverPacksDirectory,
                    "forge_tests_server_pack.zip"
                ), true
            )
        } catch (ignored: Exception) {
        }
        try {
            Files.newInputStream(props.toPath()).use {
                val properties = Properties()
                properties.load(it)
                Assertions.assertTrue(properties.getProperty("allow-flight").toBoolean())
                Assertions.assertTrue(properties.getProperty("allow-nether").toBoolean())
                Assertions.assertTrue(properties.getProperty("broadcast-console-to-ops").toBoolean())
                Assertions.assertTrue(properties.getProperty("broadcast-rcon-to-ops").toBoolean())
                Assertions.assertEquals("easy", properties.getProperty("difficulty"))
                Assertions.assertTrue(properties.getProperty("enable-command-block").toBoolean())
                Assertions.assertFalse(properties.getProperty("enable-jmx-monitoring").toBoolean())
                Assertions.assertFalse(properties.getProperty("enable-query").toBoolean())
                Assertions.assertFalse(properties.getProperty("enable-rcon").toBoolean())
                Assertions.assertTrue(properties.getProperty("enable-status").toBoolean())
                Assertions.assertTrue(properties.getProperty("enforce-whitelist").toBoolean())
                Assertions.assertEquals(100, properties.getProperty("entity-broadcast-range-percentage").toInt())
                Assertions.assertFalse(properties.getProperty("force-gamemode").toBoolean())
                Assertions.assertEquals(2, properties.getProperty("function-permission-level").toInt())
                Assertions.assertEquals("survival", properties.getProperty("gamemode"))
                Assertions.assertTrue(properties.getProperty("generate-structures").toBoolean())
                Assertions.assertFalse(properties.getProperty("hardcore").toBoolean())
                Assertions.assertEquals("world", properties.getProperty("level-name"))
                Assertions.assertEquals("skyblockbuilder:custom_skyblock", properties.getProperty("level-type"))
                Assertions.assertEquals(256, properties.getProperty("max-build-height").toInt())
                Assertions.assertEquals(10, properties.getProperty("max-players").toInt())
                Assertions.assertEquals(120000, properties.getProperty("max-tick-time").toInt())
                Assertions.assertEquals(29999984, properties.getProperty("max-world-size").toInt())
                Assertions.assertEquals("A Minecraft Server", properties.getProperty("motd"))
                Assertions.assertEquals(256, properties.getProperty("network-compression-threshold").toInt())
                Assertions.assertTrue(properties.getProperty("online-mode").toBoolean())
                Assertions.assertEquals(4, properties.getProperty("op-permission-level").toInt())
                Assertions.assertEquals(0, properties.getProperty("player-idle-timeout").toInt())
                Assertions.assertFalse(properties.getProperty("prevent-proxy-connections").toBoolean())
                Assertions.assertTrue(properties.getProperty("pvp").toBoolean())
                Assertions.assertEquals(25565, properties.getProperty("query.port").toInt())
                Assertions.assertEquals(0, properties.getProperty("rate-limit").toInt())
                Assertions.assertEquals(25575, properties.getProperty("rcon.port").toInt())
                Assertions.assertEquals(25565, properties.getProperty("server-port").toInt())
                Assertions.assertFalse(properties.getProperty("snooper-enabled").toBoolean())
                Assertions.assertTrue(properties.getProperty("spawn-animals").toBoolean())
                Assertions.assertTrue(properties.getProperty("spawn-monsters").toBoolean())
                Assertions.assertTrue(properties.getProperty("spawn-npcs").toBoolean())
                Assertions.assertEquals(16, properties.getProperty("spawn-protection").toInt())
                Assertions.assertTrue(properties.getProperty("sync-chunk-writes").toBoolean())
                Assertions.assertTrue(properties.getProperty("use-native-transport").toBoolean())
                Assertions.assertEquals(10, properties.getProperty("view-distance").toInt())
                Assertions.assertFalse(properties.getProperty("white-list").toBoolean())
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    @Suppress("SpellCheckingInspection")
    @Test
    fun fabricTest() {
        val fabricDir = File(applicationProperties.serverPacksDirectory, "fabric_tests_copy")
        val packConfig = PackConfig()
        configurationHandler.checkConfiguration(
            File("src/jvmTest/resources/testresources/spcconfs/serverpackcreator_fabric.conf"),
            packConfig
        )
        Assertions.assertTrue(serverPackHandler.run(packConfig))
        Assertions.assertTrue(
            File(
                applicationProperties.serverPacksDirectory,
                "fabric_tests_server_pack.zip"
            ).isFile
        )
        Assertions.assertTrue(File(fabricDir, "fabric-server-launch.jar").isFile)
        Assertions.assertTrue(File(fabricDir, "fabric-server-launcher.jar").isFile)
        Assertions.assertTrue(File(fabricDir, "start.ps1").isFile)
        Assertions.assertTrue(File(fabricDir, "start.sh").isFile)
    }

    @Suppress("SpellCheckingInspection")
    @Test
    fun quiltTest() {
        val quiltDir = File(applicationProperties.serverPacksDirectory, "quilt_tests_copy")
        val packConfig = PackConfig()
        configurationHandler.checkConfiguration(
            File("src/jvmTest/resources/testresources/spcconfs/serverpackcreator_quilt.conf"),
            packConfig
        )
        Assertions.assertTrue(serverPackHandler.run(packConfig))
        Assertions.assertTrue(
            File(
                applicationProperties.serverPacksDirectory,
                "legacyfabric_tests_server_pack.zip"
            ).isFile
        )
        Assertions.assertTrue(File(quiltDir, "server.jar").isFile)
        Assertions.assertTrue(File(quiltDir, "quilt-server-launch.jar").isFile)
        Assertions.assertTrue(File(quiltDir, "start.ps1").isFile)
        Assertions.assertTrue(File(quiltDir, "start.sh").isFile)
    }

    @Suppress("SpellCheckingInspection")
    @Test
    fun legacyFabricTest() {
        val legacyFabricDir = File(applicationProperties.serverPacksDirectory, "legacyfabric_tests")
        val packConfig = PackConfig()
        configurationHandler.checkConfiguration(
            File("src/jvmTest/resources/testresources/spcconfs/serverpackcreator_legacyfabric.conf"),
            packConfig
        )
        Assertions.assertTrue(serverPackHandler.run(packConfig))
        Assertions.assertTrue(
            File(
                applicationProperties.serverPacksDirectory,
                "legacyfabric_tests_server_pack.zip"
            ).isFile
        )
        Assertions.assertTrue(File(legacyFabricDir, "server.jar").isFile)
        Assertions.assertTrue(File(legacyFabricDir, "fabric-server-launch.jar").isFile)
        Assertions.assertTrue(File(legacyFabricDir, "start.ps1").isFile)
        Assertions.assertTrue(File(legacyFabricDir, "start.sh").isFile)
    }
}