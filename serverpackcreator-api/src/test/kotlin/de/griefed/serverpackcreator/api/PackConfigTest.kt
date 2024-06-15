package de.griefed.serverpackcreator.api

import com.electronwill.nightconfig.core.CommentedConfig
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File
import java.io.FileNotFoundException
import javax.xml.parsers.ParserConfigurationException

class PackConfigTest internal constructor() {

    @Test
    fun getSetModLoaderTest() {
        var modloader = "FoRgE"
        val packConfig = PackConfig()
        packConfig.modloader = modloader
        Assertions.assertEquals("Forge", packConfig.modloader)
        Assertions.assertNotEquals(modloader, packConfig.modloader)
        modloader = "fAbRiC"
        packConfig.modloader = modloader
        Assertions.assertEquals("Fabric", packConfig.modloader)
        Assertions.assertNotEquals(modloader, packConfig.modloader)
        modloader = "qUiLt"
        packConfig.modloader = modloader
        Assertions.assertEquals("Quilt", packConfig.modloader)
        Assertions.assertNotEquals(modloader, packConfig.modloader)
        modloader = "lEgAcYfAbRiC"
        packConfig.modloader = modloader
        Assertions.assertEquals("LegacyFabric", packConfig.modloader)
        Assertions.assertNotEquals(modloader, packConfig.modloader)
        modloader = "nEoFoRgE"
        packConfig.modloader = modloader
        Assertions.assertEquals("NeoForge", packConfig.modloader)
        Assertions.assertNotEquals(modloader, packConfig.modloader)
    }

    @Suppress("SpellCheckingInspection")
    @Test
    @Throws(FileNotFoundException::class, ParserConfigurationException::class)
    fun scriptSettingsTest() {
        val packConfig = PackConfig(
            ApiWrapper.api(File("src/test/resources/serverpackcreator.properties")).utilities,
            File("src/test/resources/testresources/spcconfs/scriptSettings.conf")
        )
        Assertions.assertEquals(
            packConfig.serverIconPath,
            "C:/Minecraft/ServerPackCreator/server_files/server-icon.png"
        )
        Assertions.assertEquals(packConfig.serverPackSuffix, "-4.0.0")
        Assertions.assertEquals(
            packConfig.serverPropertiesPath,
            "C:/Minecraft/ServerPackCreator/server_files/scp3.properties"
        )
        Assertions.assertEquals(
            packConfig.javaArgs,
            "-Xms8G -Xmx8G -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 -XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1 -Dusing.aikars.flags=https://mcflags.emc.gs -Daikars.new.flags=true"
        )
        Assertions.assertEquals(
            packConfig.modpackDir,
            "C:/Minecraft/Game/Instances/Survive Create Prosper 3"
        )
        Assertions.assertEquals(packConfig.modloaderVersion, "14.23.5.2860")
        Assertions.assertEquals(packConfig.minecraftVersion, "1.12.2")
        Assertions.assertEquals(packConfig.modloader, "Forge")
        Assertions.assertTrue(packConfig.isServerPropertiesInclusionDesired)
        Assertions.assertTrue(packConfig.isServerIconInclusionDesired)
        Assertions.assertTrue(packConfig.isZipCreationDesired)
        Assertions.assertTrue(packConfig.scriptSettings.containsKey("SPC_JAVA_SPC"))
        Assertions.assertEquals(
            packConfig.scriptSettings["SPC_JAVA_SPC"],
            "C:\\Program Files\\Java\\jdk1.8.0_301\\bin\\java.exe"
        )
        Assertions.assertTrue(
            packConfig.scriptSettings.containsKey("SPC_FLYNN_LIVES_SPC")
        )
        Assertions.assertEquals(
            packConfig.scriptSettings["SPC_FLYNN_LIVES_SPC"],
            "Now that's a big door"
        )
        Assertions.assertTrue(packConfig.scriptSettings.containsKey("SPC_SOME_VALUE_SPC"))
        Assertions.assertEquals(
            packConfig.scriptSettings["SPC_SOME_VALUE_SPC"],
            "something"
        )
        Assertions.assertTrue(
            packConfig.scriptSettings.containsKey("SPC_PRAISE_THE_LAMB_SPC")
        )
        Assertions.assertEquals(
            packConfig.scriptSettings["SPC_PRAISE_THE_LAMB_SPC"],
            "Kannema jajaja kannema"
        )
        Assertions.assertTrue(
            packConfig.scriptSettings.containsKey("SPC_ANOTHER_VALUE_SPC")
        )
        Assertions.assertEquals(
            packConfig.scriptSettings["SPC_ANOTHER_VALUE_SPC"],
            "another"
        )
        Assertions.assertTrue(packConfig.scriptSettings.containsKey("SPC_HELLO_SPC"))
        Assertions.assertEquals(
            packConfig.scriptSettings["SPC_HELLO_SPC"],
            "Is it me you are looking foooooor"
        )
        Assertions.assertNotNull(packConfig.getPluginConfigs("tetris"))
        Assertions.assertNotNull(packConfig.getPluginConfigs("example"))
        packConfig.pluginsConfigs["tetris"]?.let { Assertions.assertEquals(3, it.size) }
        packConfig.pluginsConfigs["example"]?.let { Assertions.assertEquals(3, it.size) }
        val list = arrayListOf("foo", "bar", "fasel", "blubba")
        for (config in packConfig.getPluginConfigs("tetris")) {
            Assertions.assertTrue((config.get("bool") as Boolean))
            Assertions.assertTrue((config.get("loader") as String).matches("(forge|fabric|quilt)".toRegex()))
            Assertions.assertEquals(config.get("id"), "tetris")
            Assertions.assertEquals((config.get("list") as ArrayList<String>).size, 4)
            Assertions.assertEquals(config.get("list"), list)
        }
        for (config in packConfig.getPluginConfigs("example")) {
            Assertions.assertTrue((config.get("bool") as Boolean))
            Assertions.assertTrue((config.get("loader") as String).matches("(forge|fabric|quilt)".toRegex()))
            Assertions.assertEquals(config.get("id") as String, "example")
            Assertions.assertEquals((config.get("list") as ArrayList<String>).size, 4)
            Assertions.assertEquals(config.get("list") as ArrayList<String>, list)
        }
        val apiProperties = ApiWrapper.api(File("src/test/resources/serverpackcreator.properties")).apiProperties
        val afterFile = File(apiProperties.homeDirectory,"after.conf")
        packConfig.save(afterFile, apiProperties)
        val after = PackConfig(
            ApiWrapper.api(File("src/test/resources/serverpackcreator.properties")).utilities, afterFile
        )
        Assertions.assertEquals(after.serverIconPath, packConfig.serverIconPath)
        Assertions.assertEquals(after.serverPackSuffix, packConfig.serverPackSuffix)
        Assertions.assertEquals(
            after.serverPropertiesPath,
            packConfig.serverPropertiesPath
        )
        Assertions.assertEquals(after.javaArgs, packConfig.javaArgs)
        Assertions.assertEquals(after.modpackDir, packConfig.modpackDir)
        Assertions.assertEquals(after.modloaderVersion, packConfig.modloaderVersion)
        Assertions.assertEquals(after.minecraftVersion, packConfig.minecraftVersion)
        Assertions.assertEquals(after.modloader, packConfig.modloader)
        Assertions.assertTrue(after.isServerPropertiesInclusionDesired)
        Assertions.assertTrue(after.isServerIconInclusionDesired)
        Assertions.assertTrue(after.isZipCreationDesired)
        Assertions.assertTrue(after.scriptSettings.containsKey("SPC_FLYNN_LIVES_SPC"))
        Assertions.assertEquals(
            after.scriptSettings["SPC_FLYNN_LIVES_SPC"],
            "Now that's a big door"
        )
        Assertions.assertTrue(after.scriptSettings.containsKey("SPC_SOME_VALUE_SPC"))
        Assertions.assertEquals(after.scriptSettings["SPC_SOME_VALUE_SPC"], "something")
        Assertions.assertTrue(after.scriptSettings.containsKey("SPC_PRAISE_THE_LAMB_SPC"))
        Assertions.assertEquals(
            after.scriptSettings["SPC_PRAISE_THE_LAMB_SPC"],
            "Kannema jajaja kannema"
        )
        Assertions.assertTrue(after.scriptSettings.containsKey("SPC_ANOTHER_VALUE_SPC"))
        Assertions.assertEquals(after.scriptSettings["SPC_ANOTHER_VALUE_SPC"], "another")
        Assertions.assertTrue(after.scriptSettings.containsKey("SPC_HELLO_SPC"))
        Assertions.assertEquals(
            after.scriptSettings["SPC_HELLO_SPC"],
            "Is it me you are looking foooooor"
        )
        Assertions.assertNotNull(after.getPluginConfigs("tetris"))
        Assertions.assertNotNull(after.getPluginConfigs("example"))
        Assertions.assertEquals(
            (packConfig.pluginsConfigs["tetris"] as ArrayList<CommentedConfig>).size,
            (after.pluginsConfigs["tetris"] as ArrayList<CommentedConfig>).size
        )
        Assertions.assertEquals(
            (packConfig.pluginsConfigs["example"] as ArrayList<CommentedConfig>).size,
            (after.pluginsConfigs["example"] as ArrayList<CommentedConfig>).size
        )
        for (config in after.getPluginConfigs("tetris")) {
            Assertions.assertTrue((config.get("bool") as Boolean))
            Assertions.assertTrue((config.get("loader") as String).matches("(forge|fabric|quilt)".toRegex()))
            Assertions.assertEquals(config.get("id"), "tetris")
            Assertions.assertEquals((config.get("list") as ArrayList<String>).size, 4)
            Assertions.assertEquals(config.get("list") as ArrayList<String>, list)
        }
        for (config in after.getPluginConfigs("example")) {
            Assertions.assertTrue((config.get("bool") as Boolean))
            Assertions.assertTrue((config.get("loader") as String).matches("(forge|fabric|quilt)".toRegex()))
            Assertions.assertEquals(config.get("id"), "example")
            Assertions.assertEquals((config.get("list") as ArrayList<String>).size, 4)
            Assertions.assertEquals(config.get("list") as ArrayList<String>, list)
        }
    }
}