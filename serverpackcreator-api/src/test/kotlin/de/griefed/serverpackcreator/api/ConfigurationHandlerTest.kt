package de.griefed.serverpackcreator.api

import com.electronwill.nightconfig.core.CommentedConfig
import de.griefed.serverpackcreator.api.config.InclusionSpecification
import de.griefed.serverpackcreator.api.config.PackConfig
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File
import java.io.IOException
import java.nio.file.Paths

internal class ConfigurationHandlerTest {
    private val apiProperties =
        ApiWrapper.api(File("src/test/resources/serverpackcreator.properties")).apiProperties
    private val configurationHandler =
        ApiWrapper.api(File("src/test/resources/serverpackcreator.properties")).configurationHandler
    private val versionMeta =
        ApiWrapper.api(File("src/test/resources/serverpackcreator.properties")).versionMeta
    private val projectDir = apiProperties.homeDirectory.parentFile.parentFile

    @Test
    fun checkConfigFileTest() {
        val check = configurationHandler.checkConfiguration(File("src/test/resources/testresources/spcconfs/serverpackcreator.conf"))
        Assertions.assertTrue(check.allChecksPassed)
        Assertions.assertTrue(check.modloaderChecksPassed)
        Assertions.assertTrue(check.modpackChecksPassed)
        Assertions.assertTrue(check.inclusionsChecksPassed)
        Assertions.assertTrue(check.minecraftVersionChecksPassed)
        Assertions.assertTrue(check.configChecksPassed)
        Assertions.assertTrue(check.otherChecksPassed)
        Assertions.assertTrue(check.serverIconChecksPassed)
        Assertions.assertTrue(check.serverPropertiesChecksPassed)
        Assertions.assertTrue(check.modloaderVersionChecksPassed)
    }

    @Test
    fun isDirTestCopyDirs() {
        val check = configurationHandler.checkConfiguration(File("src/test/resources/testresources/spcconfs/serverpackcreator_copydirs.conf"))
        Assertions.assertTrue(check.modpackChecksPassed)
        Assertions.assertTrue(check.configChecksPassed)
        Assertions.assertTrue(check.otherChecksPassed)
        Assertions.assertTrue(check.serverIconChecksPassed)
        Assertions.assertTrue(check.serverPropertiesChecksPassed)
        Assertions.assertFalse(check.minecraftVersionChecksPassed)
        Assertions.assertFalse(check.modloaderVersionChecksPassed)
        Assertions.assertFalse(check.modloaderChecksPassed)
        Assertions.assertFalse(check.inclusionsChecksPassed)
        Assertions.assertFalse(check.allChecksPassed)
    }

    @Test
    fun isDirTestMinecraftVersionFalse() {
        val check = configurationHandler.checkConfiguration(File("src/test/resources/testresources/spcconfs/serverpackcreator_minecraftversion.conf"))
        Assertions.assertTrue(check.modloaderChecksPassed)
        Assertions.assertTrue(check.modpackChecksPassed)
        Assertions.assertTrue(check.inclusionsChecksPassed)
        Assertions.assertTrue(check.configChecksPassed)
        Assertions.assertTrue(check.otherChecksPassed)
        Assertions.assertTrue(check.serverIconChecksPassed)
        Assertions.assertTrue(check.serverPropertiesChecksPassed)
        Assertions.assertFalse(check.modloaderVersionChecksPassed)
        Assertions.assertFalse(check.minecraftVersionChecksPassed)
        Assertions.assertFalse(check.allChecksPassed)
    }

    @Test
    fun isModLoaderLegacyFabric() {
        val check = configurationHandler.checkConfiguration(File("src/test/resources/testresources/spcconfs/serverpackcreator_legacyfabric.conf"))
        Assertions.assertTrue(check.modloaderChecksPassed)
        Assertions.assertTrue(check.modpackChecksPassed)
        Assertions.assertTrue(check.inclusionsChecksPassed)
        Assertions.assertTrue(check.minecraftVersionChecksPassed)
        Assertions.assertTrue(check.configChecksPassed)
        Assertions.assertTrue(check.otherChecksPassed)
        Assertions.assertTrue(check.serverIconChecksPassed)
        Assertions.assertTrue(check.serverPropertiesChecksPassed)
        Assertions.assertTrue(check.modloaderVersionChecksPassed)
    }

    @Test
    fun isModLoaderQuilt() {
        val check = configurationHandler.checkConfiguration(File("src/test/resources/testresources/spcconfs/serverpackcreator_quilt.conf"))
        Assertions.assertTrue(check.modloaderChecksPassed)
        Assertions.assertTrue(check.modpackChecksPassed)
        Assertions.assertTrue(check.inclusionsChecksPassed)
        Assertions.assertTrue(check.minecraftVersionChecksPassed)
        Assertions.assertTrue(check.configChecksPassed)
        Assertions.assertTrue(check.otherChecksPassed)
        Assertions.assertTrue(check.serverIconChecksPassed)
        Assertions.assertTrue(check.serverPropertiesChecksPassed)
        Assertions.assertTrue(check.modloaderVersionChecksPassed)
    }

    @Test
    fun isDirTestModLoaderFalse() {
        val check = configurationHandler.checkConfiguration(File("src/test/resources/testresources/spcconfs/serverpackcreator_modloaderfalse.conf"))
        Assertions.assertTrue(check.modpackChecksPassed)
        Assertions.assertTrue(check.inclusionsChecksPassed)
        Assertions.assertTrue(check.minecraftVersionChecksPassed)
        Assertions.assertTrue(check.configChecksPassed)
        Assertions.assertTrue(check.otherChecksPassed)
        Assertions.assertTrue(check.serverIconChecksPassed)
        Assertions.assertTrue(check.serverPropertiesChecksPassed)
        Assertions.assertFalse(check.modloaderChecksPassed)
        Assertions.assertFalse(check.modloaderVersionChecksPassed)
    }

    @Test
    fun isDirTestModLoaderVersion() {
        val check = configurationHandler.checkConfiguration(File("src/test/resources/testresources/spcconfs/serverpackcreator_modloaderversion.conf"))
        Assertions.assertTrue(check.modloaderChecksPassed)
        Assertions.assertTrue(check.modpackChecksPassed)
        Assertions.assertTrue(check.inclusionsChecksPassed)
        Assertions.assertTrue(check.minecraftVersionChecksPassed)
        Assertions.assertTrue(check.configChecksPassed)
        Assertions.assertTrue(check.otherChecksPassed)
        Assertions.assertTrue(check.serverIconChecksPassed)
        Assertions.assertTrue(check.serverPropertiesChecksPassed)
        Assertions.assertFalse(check.modloaderVersionChecksPassed)
    }

    @Test
    fun checkModpackDirTest() {
        val modpackDirCorrect = "src/test/resources/forge_tests"
        val check = configurationHandler.checkModpackDir(modpackDirCorrect)
        Assertions.assertTrue(check.modpackChecksPassed)
    }

    @Test
    fun checkModpackDirTestFalse() {
        val check = configurationHandler.checkModpackDir("modpackDir")
        Assertions.assertFalse(check.modpackChecksPassed)
    }

    @Test
    fun checkInclusionsTest() {
        val modpackDir = "src/test/resources/forge_tests"
        val inclusions = mutableListOf<InclusionSpecification>()
        inclusions.add(InclusionSpecification("config"))
        inclusions.add(InclusionSpecification("mods"))
        inclusions.add(InclusionSpecification("scripts"))
        inclusions.add(InclusionSpecification("seeds"))
        inclusions.add(InclusionSpecification("defaultconfigs"))
        val check = configurationHandler.checkInclusions(inclusions, modpackDir)
        Assertions.assertTrue(check.modpackChecksPassed)
        Assertions.assertTrue(check.inclusionsChecksPassed)
    }

    @Suppress("SpellCheckingInspection")
    @Test
    fun checkInclusionsTestFalse() {
        val modpackDir = "src/test/resources/forge_tests"
        val inclusions = mutableListOf<InclusionSpecification>()
        inclusions.add(InclusionSpecification("config"))
        inclusions.add(InclusionSpecification("modss"))
        inclusions.add(InclusionSpecification("scriptss"))
        inclusions.add(InclusionSpecification("seedss"))
        inclusions.add(InclusionSpecification("defaultconfigss"))
        val check = configurationHandler.checkInclusions(inclusions, modpackDir)
        Assertions.assertTrue(check.modpackChecksPassed)
        Assertions.assertFalse(check.inclusionsChecksPassed)
    }

    @Test
    fun checkInclusionsTestFiles() {
        val modpackDir = "src/test/resources/forge_tests"
        val inclusions = mutableListOf<InclusionSpecification>()
        inclusions.add(InclusionSpecification("config"))
        inclusions.add(InclusionSpecification("mods"))
        inclusions.add(InclusionSpecification("scripts"))
        inclusions.add(InclusionSpecification("seeds"))
        inclusions.add(InclusionSpecification("defaultconfigs"))
        inclusions.add(InclusionSpecification("test.txt","test.txt"))
        inclusions.add(InclusionSpecification("test2.txt","test2.txt"))
        val check = configurationHandler.checkInclusions(inclusions, modpackDir)
        Assertions.assertTrue(check.modpackChecksPassed)
        Assertions.assertTrue(check.inclusionsChecksPassed)
    }

    @Suppress("SpellCheckingInspection")
    @Test
    fun checkInclusionsTestFilesFalse() {
        val modpackDir = "src/test/resources/forge_tests"
        val inclusions = mutableListOf<InclusionSpecification>()
        inclusions.add(InclusionSpecification("config"))
        inclusions.add(InclusionSpecification("mods"))
        inclusions.add(InclusionSpecification("scripts"))
        inclusions.add(InclusionSpecification("seeds"))
        inclusions.add(InclusionSpecification("defaultconfigs"))
        inclusions.add(InclusionSpecification("READMEee.md","README.md"))
        inclusions.add(InclusionSpecification("LICENSEee","LICENSE"))
        inclusions.add(InclusionSpecification("LICENSEee","test/LICENSE"))
        inclusions.add(InclusionSpecification("LICENSEee","test/license.md"))
        val check = configurationHandler.checkInclusions(inclusions, modpackDir)
        Assertions.assertTrue(check.modpackChecksPassed)
        Assertions.assertFalse(check.inclusionsChecksPassed)
    }

    @Test
    fun checkModloaderTestForge() {
        var check = configurationHandler.checkModloader("Forge")
        Assertions.assertTrue(check.modloaderChecksPassed)
        check = configurationHandler.checkModloader("fOrGe")
        Assertions.assertTrue(check.modloaderChecksPassed)
        check = configurationHandler.checkModloader("Fabric")
        Assertions.assertTrue(check.modloaderChecksPassed)
        check = configurationHandler.checkModloader("fAbRiC")
        Assertions.assertTrue(check.modloaderChecksPassed)
        check = configurationHandler.checkModloader("Quilt")
        Assertions.assertTrue(check.modloaderChecksPassed)
        check = configurationHandler.checkModloader("qUiLt")
        Assertions.assertTrue(check.modloaderChecksPassed)
        check = configurationHandler.checkModloader("lEgAcYfAbRiC")
        Assertions.assertTrue(check.modloaderChecksPassed)
        check = configurationHandler.checkModloader("LegacyFabric")
        Assertions.assertTrue(check.modloaderChecksPassed)
        check = configurationHandler.checkModloader("nEoFoRgE")
        Assertions.assertTrue(check.modloaderChecksPassed)
        check = configurationHandler.checkModloader("NeoForge")
        Assertions.assertTrue(check.modloaderChecksPassed)
        check = configurationHandler.checkModloader("modloader")
        Assertions.assertFalse(check.modloaderChecksPassed)
    }

    @Test
    fun checkModloaderVersionTestForge() {
        var check = configurationHandler.checkModloaderVersion("Forge", "36.1.2", "1.16.5")
        Assertions.assertTrue(check.modloaderChecksPassed)
        Assertions.assertTrue(check.modloaderVersionChecksPassed)
        check = configurationHandler.checkModloaderVersion("Forge", "90.0.0", "1.16.5")
        Assertions.assertTrue(check.modloaderChecksPassed)
        Assertions.assertFalse(check.modloaderVersionChecksPassed)
    }

    @Test
    fun checkModloaderVersionTestFabric() {
        var check = configurationHandler.checkModloaderVersion("Fabric", "0.11.3", "1.16.5")
        Assertions.assertTrue(check.modloaderChecksPassed)
        Assertions.assertTrue(check.modloaderVersionChecksPassed)
        check = configurationHandler.checkModloaderVersion("Fabric", "0.90.3", "1.16.5")
        Assertions.assertTrue(check.modloaderChecksPassed)
        Assertions.assertFalse(check.modloaderVersionChecksPassed)
    }

    @Test
    fun checkModloaderVersionTestQuilt() {
        var check = configurationHandler.checkModloaderVersion("Quilt", "0.16.1", "1.16.5")
        Assertions.assertTrue(check.modloaderChecksPassed)
        Assertions.assertTrue(check.modloaderVersionChecksPassed)
        check = configurationHandler.checkModloaderVersion("Quilt", "0.90.3", "1.16.5")
        Assertions.assertTrue(check.modloaderChecksPassed)
        Assertions.assertFalse(check.modloaderVersionChecksPassed)
    }

    @Test
    fun isLegacyFabricVersionCorrectTest() {
        var check = configurationHandler.checkModloaderVersion("LegacyFabric", "0.13.3", "1.12.2")
        Assertions.assertTrue(check.modloaderChecksPassed)
        Assertions.assertTrue(check.modloaderVersionChecksPassed)
        check = configurationHandler.checkModloaderVersion("LegacyFabric", "0.999.3", "1.12.2")
        Assertions.assertTrue(check.modloaderChecksPassed)
        Assertions.assertFalse(check.modloaderVersionChecksPassed)
    }

    @Test
    fun isNeoForgeVersionCorrectTest() {
        var check = configurationHandler.checkModloaderVersion("NeoForge", "20.6.118", "1.20.6")
        Assertions.assertTrue(check.modloaderChecksPassed)
        Assertions.assertTrue(check.modloaderVersionChecksPassed)
        check = configurationHandler.checkModloaderVersion("NeoForge", "20.5.21-beta", "1.20.5")
        Assertions.assertTrue(check.modloaderChecksPassed)
        Assertions.assertTrue(check.modloaderVersionChecksPassed)
        check = configurationHandler.checkModloaderVersion("NeoForge", "20.4.237", "1.20.4")
        Assertions.assertTrue(check.modloaderChecksPassed)
        Assertions.assertTrue(check.modloaderVersionChecksPassed)
        check = configurationHandler.checkModloaderVersion("NeoForge", "20.3.8-beta", "1.20.3")
        Assertions.assertTrue(check.modloaderChecksPassed)
        Assertions.assertTrue(check.modloaderVersionChecksPassed)
        check = configurationHandler.checkModloaderVersion("NeoForge", "20.2.88", "1.20.2")
        Assertions.assertTrue(check.modloaderChecksPassed)
        Assertions.assertTrue(check.modloaderVersionChecksPassed)
        check = configurationHandler.checkModloaderVersion("NeoForge", "47.1.106", "1.20.1")
        Assertions.assertTrue(check.modloaderChecksPassed)
        Assertions.assertTrue(check.modloaderVersionChecksPassed)
        check = configurationHandler.checkModloaderVersion("NeoForge", "0.999.3", "1.20.6")
        Assertions.assertTrue(check.modloaderChecksPassed)
        Assertions.assertFalse(check.modloaderVersionChecksPassed)
    }

    @Suppress("SpellCheckingInspection")
    @Test
    fun checkConfigModelTest() {
        val clientMods = arrayListOf(
            "AmbientSounds",
            "BackTools",
            "BetterAdvancement",
            "BetterPing",
            "cherished",
            "ClientTweaks",
            "Controlling",
            "DefaultOptions",
            "durability",
            "DynamicSurroundings",
            "itemzoom",
            "jei-professions",
            "jeiintegration",
            "JustEnoughResources",
            "MouseTweaks",
            "Neat",
            "OldJavaWarning",
            "PackMenu",
            "preciseblockplacing",
            "SimpleDiscordRichPresence",
            "SpawnerFix",
            "TipTheScales",
            "WorldNameRandomizer"
        )
        val inclusions = ArrayList<InclusionSpecification>()
        inclusions.add(InclusionSpecification("config"))
        inclusions.add(InclusionSpecification("mods"))
        inclusions.add(InclusionSpecification("scripts"))
        inclusions.add(InclusionSpecification("seeds"))
        inclusions.add(InclusionSpecification("defaultconfigs"))
        val packConfig = PackConfig()
        packConfig.modpackDir = "src/test/resources/forge_tests"
        packConfig.setClientMods(clientMods)
        packConfig.setInclusions(inclusions)
        packConfig.isServerIconInclusionDesired = true
        packConfig.isServerPropertiesInclusionDesired = true
        packConfig.isZipCreationDesired = true
        packConfig.modloader = "Forge"
        packConfig.modloaderVersion = "36.1.2"
        packConfig.minecraftVersion = "1.16.5"
        val check = configurationHandler.checkConfiguration(packConfig)
        Assertions.assertTrue(check.allChecksPassed)
        Assertions.assertTrue(check.modloaderChecksPassed)
        Assertions.assertTrue(check.modpackChecksPassed)
        Assertions.assertTrue(check.inclusionsChecksPassed)
        Assertions.assertTrue(check.minecraftVersionChecksPassed)
        Assertions.assertTrue(check.configChecksPassed)
        Assertions.assertTrue(check.otherChecksPassed)
        Assertions.assertTrue(check.serverIconChecksPassed)
        Assertions.assertTrue(check.serverPropertiesChecksPassed)
        Assertions.assertTrue(check.modloaderVersionChecksPassed)
        Assertions.assertTrue(
            packConfig.scriptSettings.containsKey("SPC_SERVERPACKCREATOR_VERSION_SPC")
        )
        Assertions.assertTrue(
            packConfig.scriptSettings.containsKey("SPC_MINECRAFT_VERSION_SPC")
        )
        Assertions.assertTrue(packConfig.scriptSettings.containsKey("SPC_MODLOADER_SPC"))
        Assertions.assertTrue(
            packConfig.scriptSettings.containsKey("SPC_MODLOADER_VERSION_SPC")
        )
        Assertions.assertTrue(packConfig.scriptSettings.containsKey("SPC_JAVA_ARGS_SPC"))
        Assertions.assertTrue(packConfig.scriptSettings.containsKey("SPC_JAVA_SPC"))
        Assertions.assertTrue(
            packConfig.scriptSettings.containsKey("SPC_FABRIC_INSTALLER_VERSION_SPC")
        )
        Assertions.assertTrue(
            packConfig.scriptSettings
                .containsKey("SPC_LEGACYFABRIC_INSTALLER_VERSION_SPC")
        )
        Assertions.assertTrue(
            packConfig.scriptSettings.containsKey("SPC_QUILT_INSTALLER_VERSION_SPC")
        )
        Assertions.assertEquals(
            packConfig.minecraftVersion,
            packConfig.scriptSettings["SPC_MINECRAFT_VERSION_SPC"]
        )
        Assertions.assertEquals(
            packConfig.modloader,
            packConfig.scriptSettings["SPC_MODLOADER_SPC"]
        )
        Assertions.assertEquals(
            packConfig.modloaderVersion,
            packConfig.scriptSettings["SPC_MODLOADER_VERSION_SPC"]
        )
        Assertions.assertEquals(
            packConfig.javaArgs,
            packConfig.scriptSettings["SPC_JAVA_ARGS_SPC"]
        )
        Assertions.assertEquals("java", packConfig.scriptSettings["SPC_JAVA_SPC"])
        Assertions.assertEquals(
            apiProperties.apiVersion,
            packConfig.scriptSettings["SPC_SERVERPACKCREATOR_VERSION_SPC"]
        )
        Assertions.assertEquals(
            versionMeta.fabric.releaseInstaller(),
            packConfig.scriptSettings["SPC_FABRIC_INSTALLER_VERSION_SPC"]
        )
        Assertions.assertEquals(
            versionMeta.legacyFabric.releaseInstaller(),
            packConfig.scriptSettings["SPC_LEGACYFABRIC_INSTALLER_VERSION_SPC"]
        )
        Assertions.assertEquals(
            versionMeta.quilt.releaseInstaller(),
            packConfig.scriptSettings["SPC_QUILT_INSTALLER_VERSION_SPC"]
        )
    }

    @Test
    fun zipArchiveTest() {
        var packConfig = PackConfig()
        packConfig.modpackDir = "src/test/resources/testresources/Survive_Create_Prosper_4_valid.zip"
        var check = configurationHandler.checkConfiguration(packConfig)
        Assertions.assertTrue(check.allChecksPassed)
        Assertions.assertTrue(check.modloaderChecksPassed)
        Assertions.assertTrue(check.modpackChecksPassed)
        Assertions.assertTrue(check.inclusionsChecksPassed)
        Assertions.assertTrue(check.minecraftVersionChecksPassed)
        Assertions.assertTrue(check.configChecksPassed)
        Assertions.assertTrue(check.otherChecksPassed)
        Assertions.assertTrue(check.serverIconChecksPassed)
        Assertions.assertTrue(check.serverPropertiesChecksPassed)
        Assertions.assertTrue(check.modloaderVersionChecksPassed)
        packConfig = PackConfig()
        packConfig.modpackDir = "src/test/resources/testresources/Survive_Create_Prosper_4_invalid.zip"
        check = configurationHandler.checkConfiguration(packConfig)
        Assertions.assertTrue(check.configChecksPassed)
        Assertions.assertFalse(check.modpackChecksPassed)
        Assertions.assertTrue(check.inclusionsChecksPassed)
        Assertions.assertFalse(check.minecraftVersionChecksPassed)
        Assertions.assertFalse(check.modloaderChecksPassed)
        Assertions.assertFalse(check.modloaderVersionChecksPassed)
        Assertions.assertTrue(check.serverIconChecksPassed)
        Assertions.assertTrue(check.serverPropertiesChecksPassed)
        Assertions.assertTrue(check.otherChecksPassed)
        Assertions.assertFalse(check.allChecksPassed)
    }

    @Test
    fun checkConfigurationFileTest() {
        val check = configurationHandler.checkConfiguration(File("src/test/resources/testresources/spcconfs/serverpackcreator.conf"))
        Assertions.assertTrue(check.allChecksPassed)
        Assertions.assertTrue(check.modloaderChecksPassed)
        Assertions.assertTrue(check.modpackChecksPassed)
        Assertions.assertTrue(check.inclusionsChecksPassed)
        Assertions.assertTrue(check.minecraftVersionChecksPassed)
        Assertions.assertTrue(check.configChecksPassed)
        Assertions.assertTrue(check.otherChecksPassed)
        Assertions.assertTrue(check.serverIconChecksPassed)
        Assertions.assertTrue(check.serverPropertiesChecksPassed)
        Assertions.assertTrue(check.modloaderVersionChecksPassed)
    }

    @Test
    fun checkConfigurationFileAndModelTest() {
        var packConfig = PackConfig()
        var check = configurationHandler.checkConfiguration(
            File("src/test/resources/testresources/spcconfs/serverpackcreator.conf"),
            packConfig
        )
        Assertions.assertTrue(check.allChecksPassed)
        Assertions.assertTrue(check.modloaderChecksPassed)
        Assertions.assertTrue(check.modpackChecksPassed)
        Assertions.assertTrue(check.inclusionsChecksPassed)
        Assertions.assertTrue(check.minecraftVersionChecksPassed)
        Assertions.assertTrue(check.configChecksPassed)
        Assertions.assertTrue(check.otherChecksPassed)
        Assertions.assertTrue(check.serverIconChecksPassed)
        Assertions.assertTrue(check.serverPropertiesChecksPassed)
        Assertions.assertTrue(check.modloaderVersionChecksPassed)
        Assertions.assertEquals("src/test/resources/forge_tests", packConfig.modpackDir)
        Assertions.assertEquals("1.16.5", packConfig.minecraftVersion)
        Assertions.assertEquals("Forge", packConfig.modloader)
        Assertions.assertEquals("36.1.2", packConfig.modloaderVersion)
        packConfig = PackConfig()
        check = configurationHandler.checkConfiguration(
            File("src/test/resources/testresources/spcconfs/serverpackcreator.conf"),
            packConfig
        )
        Assertions.assertTrue(check.allChecksPassed)
        Assertions.assertTrue(check.modloaderChecksPassed)
        Assertions.assertTrue(check.modpackChecksPassed)
        Assertions.assertTrue(check.inclusionsChecksPassed)
        Assertions.assertTrue(check.minecraftVersionChecksPassed)
        Assertions.assertTrue(check.configChecksPassed)
        Assertions.assertTrue(check.otherChecksPassed)
        Assertions.assertTrue(check.serverIconChecksPassed)
        Assertions.assertTrue(check.serverPropertiesChecksPassed)
        Assertions.assertTrue(check.modloaderVersionChecksPassed)
        Assertions.assertEquals("src/test/resources/forge_tests", packConfig.modpackDir)
        Assertions.assertEquals("1.16.5", packConfig.minecraftVersion)
        Assertions.assertEquals("Forge", packConfig.modloader)
        Assertions.assertEquals("36.1.2", packConfig.modloaderVersion)
    }

    @Suppress("SpellCheckingInspection")
    @Test
    fun checkConfigurationNoFileTest() {
        val packConfig = PackConfig()
        val clientMods = arrayListOf(
            "AmbientSounds",
            "BackTools",
            "BetterAdvancement",
            "BetterPing",
            "cherished",
            "ClientTweaks",
            "Controlling",
            "DefaultOptions",
            "durability",
            "DynamicSurroundings",
            "itemzoom",
            "jei-professions",
            "jeiintegration",
            "JustEnoughResources",
            "MouseTweaks",
            "Neat",
            "OldJavaWarning",
            "PackMenu",
            "preciseblockplacing",
            "SimpleDiscordRichPresence",
            "SpawnerFix",
            "TipTheScales",
            "WorldNameRandomizer"
        )
        val inclusions = ArrayList<InclusionSpecification>()
        inclusions.add(InclusionSpecification("config"))
        inclusions.add(InclusionSpecification("mods"))
        inclusions.add(InclusionSpecification("scripts"))
        inclusions.add(InclusionSpecification("seeds"))
        inclusions.add(InclusionSpecification("defaultconfigs"))
        packConfig.modpackDir = "src/test/resources/forge_tests"
        packConfig.setClientMods(clientMods)
        packConfig.setInclusions(inclusions)
        packConfig.isServerIconInclusionDesired = true
        packConfig.isServerIconInclusionDesired = true
        packConfig.isServerPropertiesInclusionDesired = true
        packConfig.isZipCreationDesired = true
        packConfig.modloader = "Forge"
        packConfig.modloaderVersion = "36.1.2"
        packConfig.minecraftVersion = "1.16.5"
        var check = configurationHandler.checkConfiguration(packConfig)
        Assertions.assertTrue(check.allChecksPassed)
        Assertions.assertTrue(check.modloaderChecksPassed)
        Assertions.assertTrue(check.modpackChecksPassed)
        Assertions.assertTrue(check.inclusionsChecksPassed)
        Assertions.assertTrue(check.minecraftVersionChecksPassed)
        Assertions.assertTrue(check.configChecksPassed)
        Assertions.assertTrue(check.otherChecksPassed)
        Assertions.assertTrue(check.serverIconChecksPassed)
        Assertions.assertTrue(check.serverPropertiesChecksPassed)
        Assertions.assertTrue(check.modloaderVersionChecksPassed)
        Assertions.assertEquals(
            "src/test/resources/forge_tests", packConfig.modpackDir
        )
        Assertions.assertEquals("1.16.5", packConfig.minecraftVersion)
        Assertions.assertEquals("Forge", packConfig.modloader)
        Assertions.assertEquals("36.1.2", packConfig.modloaderVersion)
        packConfig.modloader = "Fabric"
        packConfig.modloaderVersion = "0.14.6"
        check = configurationHandler.checkConfiguration(packConfig)
        Assertions.assertTrue(check.allChecksPassed)
        Assertions.assertTrue(check.modloaderChecksPassed)
        Assertions.assertTrue(check.modpackChecksPassed)
        Assertions.assertTrue(check.inclusionsChecksPassed)
        Assertions.assertTrue(check.minecraftVersionChecksPassed)
        Assertions.assertTrue(check.configChecksPassed)
        Assertions.assertTrue(check.otherChecksPassed)
        Assertions.assertTrue(check.serverIconChecksPassed)
        Assertions.assertTrue(check.serverPropertiesChecksPassed)
        Assertions.assertTrue(check.modloaderVersionChecksPassed)
        Assertions.assertEquals("Fabric", packConfig.modloader)
        Assertions.assertEquals("0.14.6", packConfig.modloaderVersion)
        packConfig.modloader = "Quilt"
        packConfig.modloaderVersion = "0.16.1"
        check = configurationHandler.checkConfiguration(packConfig)
        Assertions.assertTrue(check.allChecksPassed)
        Assertions.assertTrue(check.modloaderChecksPassed)
        Assertions.assertTrue(check.modpackChecksPassed)
        Assertions.assertTrue(check.inclusionsChecksPassed)
        Assertions.assertTrue(check.minecraftVersionChecksPassed)
        Assertions.assertTrue(check.configChecksPassed)
        Assertions.assertTrue(check.otherChecksPassed)
        Assertions.assertTrue(check.serverIconChecksPassed)
        Assertions.assertTrue(check.serverPropertiesChecksPassed)
        Assertions.assertTrue(check.modloaderVersionChecksPassed)
        Assertions.assertEquals("Quilt", packConfig.modloader)
        Assertions.assertEquals("0.16.1", packConfig.modloaderVersion)
    }

    @Test
    fun checkIconAndPropertiesTest() {
        Assertions.assertTrue(configurationHandler.checkIconAndProperties(""))
        Assertions.assertFalse(configurationHandler.checkIconAndProperties("/some/path"))
        Assertions.assertTrue(configurationHandler.checkIconAndProperties(apiProperties.defaultServerIcon.absolutePath))
        Assertions.assertTrue(configurationHandler.checkIconAndProperties(apiProperties.defaultServerProperties.absolutePath))
    }

    @Test
    fun checkZipArchiveTest() {
        var check = configurationHandler.checkZipArchive(Paths.get("src/test/resources/testresources/Survive_Create_Prosper_4_valid.zip").toAbsolutePath().toString())
        Assertions.assertTrue(check.modpackChecksPassed)
        check = configurationHandler.checkZipArchive(Paths.get("src/test/resources/testresources/Survive_Create_Prosper_4_invalid.zip").toAbsolutePath().toString())
        Assertions.assertFalse(check.modpackChecksPassed)
    }

    @Suppress("SpellCheckingInspection")
    @Test
    fun writeConfigToFileTestFabric() {
        val clientMods = arrayListOf(
            "AmbientSounds",
            "BackTools",
            "BetterAdvancement",
            "BetterPing",
            "cherished",
            "ClientTweaks",
            "Controlling",
            "DefaultOptions",
            "durability",
            "DynamicSurroundings",
            "itemzoom",
            "jei-professions",
            "jeiintegration",
            "JustEnoughResources",
            "MouseTweaks",
            "Neat",
            "OldJavaWarning",
            "PackMenu",
            "preciseblockplacing",
            "SimpleDiscordRichPresence",
            "SpawnerFix",
            "TipTheScales",
            "WorldNameRandomizer"
        )
        val whitelist = arrayListOf(
            "Ping-Wheel-"
        )
        val inclusions = ArrayList<InclusionSpecification>()
        inclusions.add(InclusionSpecification("config"))
        inclusions.add(InclusionSpecification("mods"))
        inclusions.add(InclusionSpecification("scripts"))
        inclusions.add(InclusionSpecification("seeds"))
        inclusions.add(InclusionSpecification("defaultconfigs"))
        val javaPath: String
        var autoJavaPath = System.getProperty("java.home").replace("\\", "/") + "/bin/java"
        if (autoJavaPath.startsWith("C:")) {
            autoJavaPath = "%s.exe".format(autoJavaPath)
        }
        javaPath = if (File("/usr/bin/java").exists()) {
            "/usr/bin/java"
        } else {
            autoJavaPath
        }
        val javaArgs = "tf3g4jz89agz843fag8z49a3zg8ap3jg8zap9vagv3z8j"
        PackConfig()
        Assertions.assertNotNull(
            PackConfig(
                clientMods,
                whitelist,
                inclusions,
                "src/test/resources/fabric_tests",
                javaPath,
                "Fabric",
                "1.16.5",
                "0.11.3",
                javaArgs,
                "",
                "",
                includeServerIcon = true,
                includeServerProperties = true,
                includeZipCreation = true,
                scriptSettings = HashMap<String, String>(10),
                pluginsConfigs = HashMap<String, ArrayList<CommentedConfig>>(10)
            ).save(File("tests/serverpackcreatorfabric.conf"))
        )
        Assertions.assertTrue(File("tests/serverpackcreatorfabric.conf").exists())
    }

    @Suppress("SpellCheckingInspection")
    @Test
    fun writeConfigToFileTestForge() {
        val clientMods = arrayListOf(
            "AmbientSounds",
            "BackTools",
            "BetterAdvancement",
            "BetterPing",
            "cherished",
            "ClientTweaks",
            "Controlling",
            "DefaultOptions",
            "durability",
            "DynamicSurroundings",
            "itemzoom",
            "jei-professions",
            "jeiintegration",
            "JustEnoughResources",
            "MouseTweaks",
            "Neat",
            "OldJavaWarning",
            "PackMenu",
            "preciseblockplacing",
            "SimpleDiscordRichPresence",
            "SpawnerFix",
            "TipTheScales",
            "WorldNameRandomizer"
        )
        val whitelist = arrayListOf(
            "Ping-Wheel-"
        )
        val inclusions = ArrayList<InclusionSpecification>()
        inclusions.add(InclusionSpecification("config"))
        inclusions.add(InclusionSpecification("mods"))
        inclusions.add(InclusionSpecification("scripts"))
        inclusions.add(InclusionSpecification("seeds"))
        inclusions.add(InclusionSpecification("defaultconfigs"))
        val javaPath: String
        var autoJavaPath = System.getProperty("java.home").replace("\\", "/") + "/bin/java"
        if (autoJavaPath.startsWith("C:")) {
            autoJavaPath = "%s.exe".format(autoJavaPath)
        }
        javaPath = if (File("/usr/bin/java").exists()) {
            "/usr/bin/java"
        } else {
            autoJavaPath
        }
        val javaArgs = "tf3g4jz89agz843fag8z49a3zg8ap3jg8zap9vagv3z8j"
        Assertions.assertNotNull(
            PackConfig(
                clientMods,
                whitelist,
                inclusions,
                "src/test/resources/forge_tests",
                javaPath,
                "Forge",
                "1.16.5",
                "36.1.2",
                javaArgs,
                "",
                "",
                includeServerIcon = true,
                includeServerProperties = true,
                includeZipCreation = true,
                scriptSettings = HashMap<String, String>(10),
                pluginsConfigs = HashMap<String, ArrayList<CommentedConfig>>(10)
            ).save(File("tests/serverpackcreatorforge.conf"))
        )
        Assertions.assertTrue(File("tests/serverpackcreatorforge.conf").exists())
    }

    @Suppress("SpellCheckingInspection")
    @Test
    fun writeConfigToFileModelTest() {
        val packConfig = PackConfig()
        packConfig.modpackDir = "src/test/resources/forge_tests"
        packConfig.setClientMods(
            arrayListOf(
                "AmbientSounds",
                "BackTools",
                "BetterAdvancement",
                "BetterPing",
                "cherished",
                "ClientTweaks",
                "Controlling",
                "DefaultOptions",
                "durability",
                "DynamicSurroundings",
                "itemzoom",
                "jei-professions",
                "jeiintegration",
                "JustEnoughResources",
                "MouseTweaks",
                "Neat",
                "OldJavaWarning",
                "PackMenu",
                "preciseblockplacing",
                "SimpleDiscordRichPresence",
                "SpawnerFix",
                "TipTheScales",
                "WorldNameRandomizer"
            )
        )
        val inclusions = ArrayList<InclusionSpecification>()
        inclusions.add(InclusionSpecification("config"))
        inclusions.add(InclusionSpecification("mods"))
        inclusions.add(InclusionSpecification("scripts"))
        inclusions.add(InclusionSpecification("seeds"))
        inclusions.add(InclusionSpecification("defaultconfigs"))
        packConfig.setInclusions(inclusions)
        packConfig.isServerIconInclusionDesired = true
        packConfig.isServerPropertiesInclusionDesired = true
        packConfig.isZipCreationDesired = true
        packConfig.minecraftVersion = "1.16.5"
        packConfig.modloader = "Forge"
        packConfig.modloaderVersion = "36.1.2"
        packConfig.javaArgs = "tf3g4jz89agz843fag8z49a3zg8ap3jg8zap9vagv3z8j"
        Assertions.assertNotNull(packConfig.save(File("tests/somefile.conf")))
        Assertions.assertTrue(File("tests/somefile.conf").exists())
    }

    @Test
    fun setModLoaderCaseTestForge() {
        Assertions.assertEquals("Forge", configurationHandler.getModLoaderCase("fOrGe"))
    }

    @Test
    fun setModLoaderCaseTestFabric() {
        Assertions.assertEquals("Fabric", configurationHandler.getModLoaderCase("fAbRiC"))
    }

    @Test
    fun setModLoaderCaseTestForgeCorrected() {
        @Suppress("SpellCheckingInspection")
        Assertions.assertEquals("Forge", configurationHandler.getModLoaderCase("eeeeefOrGeeeeee"))
    }

    @Test
    fun setModLoaderCaseTestFabricCorrected() {
        @Suppress("SpellCheckingInspection")
        Assertions.assertEquals(
            "Fabric",
            configurationHandler.getModLoaderCase("hufwhafasfabricfagrsg")
        )
    }

    @Suppress("SpellCheckingInspection")
    @Test
    fun printConfigModelTest() {
        val packConfig = PackConfig()
        packConfig.modpackDir = "src/test/resources/forge_tests"
        packConfig.setClientMods(
            arrayListOf(
                "AmbientSounds",
                "BackTools",
                "BetterAdvancement",
                "BetterPing",
                "cherished",
                "ClientTweaks",
                "Controlling",
                "DefaultOptions",
                "durability",
                "DynamicSurroundings",
                "itemzoom",
                "jei-professions",
                "jeiintegration",
                "JustEnoughResources",
                "MouseTweaks",
                "Neat",
                "OldJavaWarning",
                "PackMenu",
                "preciseblockplacing",
                "SimpleDiscordRichPresence",
                "SpawnerFix",
                "TipTheScales",
                "WorldNameRandomizer"
            )
        )
        val inclusions = ArrayList<InclusionSpecification>()
        inclusions.add(InclusionSpecification("config"))
        inclusions.add(InclusionSpecification("mods"))
        inclusions.add(InclusionSpecification("scripts"))
        inclusions.add(InclusionSpecification("seeds"))
        inclusions.add(InclusionSpecification("defaultconfigs"))
        packConfig.setInclusions(inclusions)
        packConfig.isServerIconInclusionDesired = true
        packConfig.isServerPropertiesInclusionDesired = true
        packConfig.isZipCreationDesired = true
        packConfig.minecraftVersion = "1.16.5"
        packConfig.modloader = "Forge"
        packConfig.modloaderVersion = "36.1.2"
        packConfig.javaArgs = "tf3g4jz89agz843fag8z49a3zg8ap3jg8zap9vagv3z8j"
        configurationHandler.printConfigurationModel(packConfig)
    }

    @Test
    @Throws(IOException::class)
    fun updateConfigModelFromModrinthManifestTest() {
        val packConfig = PackConfig()
        configurationHandler.updateConfigModelFromModrinthManifest(
            packConfig,
            File("src/test/resources/testresources/modrinth/forge_modrinth.index.json")
        )
        Assertions.assertEquals(packConfig.minecraftVersion, "1.18.2")
        Assertions.assertEquals(packConfig.modloader, "Forge")
        Assertions.assertEquals(packConfig.modloaderVersion, "40.1.48")
        configurationHandler.updateConfigModelFromModrinthManifest(
            packConfig,
            File("src/test/resources/testresources/modrinth/fabric_modrinth.index.json")
        )
        Assertions.assertEquals(packConfig.minecraftVersion, "1.19")
        Assertions.assertEquals(packConfig.modloader, "Fabric")
        Assertions.assertEquals(packConfig.modloaderVersion, "0.14.8")
        configurationHandler.updateConfigModelFromModrinthManifest(
            packConfig,
            File("src/test/resources/testresources/modrinth/quilt_modrinth.index.json")
        )
        Assertions.assertEquals(packConfig.minecraftVersion, "1.19")
        Assertions.assertEquals(packConfig.modloader, "Quilt")
        Assertions.assertEquals(packConfig.modloaderVersion, "0.17.0")
    }

    @Test
    @Throws(IOException::class)
    fun updateConfigModelFromCurseManifestTest() {
        val packConfig = PackConfig()
        configurationHandler.updateConfigModelFromCurseManifest(
            packConfig,
            File("src/test/resources/testresources/curseforge/forge_manifest.json")
        )
        Assertions.assertEquals(packConfig.minecraftVersion, "1.16.5")
        Assertions.assertEquals(packConfig.modloader, "Forge")
        Assertions.assertEquals(packConfig.modloaderVersion, "36.0.1")
        configurationHandler.updateConfigModelFromCurseManifest(
            packConfig,
            File("src/test/resources/testresources/curseforge/fabric_manifest.json")
        )
        Assertions.assertEquals(packConfig.minecraftVersion, "1.18.2")
        Assertions.assertEquals(packConfig.modloader, "Fabric")
        Assertions.assertEquals(packConfig.modloaderVersion, "0.13.3")
    }

    @Test
    @Throws(IOException::class)
    fun updateConfigModelFromMinecraftInstanceTest() {
        val packConfig = PackConfig()
        configurationHandler.updateConfigModelFromMinecraftInstance(
            packConfig,
            File("src/test/resources/testresources/curseforge/forge_minecraftinstance.json")
        )
        Assertions.assertEquals(packConfig.minecraftVersion, "1.16.5")
        Assertions.assertEquals(packConfig.modloader, "Forge")
        Assertions.assertEquals(packConfig.modloaderVersion, "36.2.4")
        configurationHandler.updateConfigModelFromMinecraftInstance(
            packConfig,
            File("src/test/resources/testresources/curseforge/fabric_minecraftinstance.json")
        )
        Assertions.assertEquals(packConfig.minecraftVersion, "1.18.2")
        Assertions.assertEquals(packConfig.modloader, "Fabric")
        Assertions.assertEquals(packConfig.modloaderVersion, "0.13.3")
    }

    @Suppress("SpellCheckingInspection")
    @Test
    @Throws(IOException::class)
    fun updateConfigModelFromConfigJsonTest() {
        val packConfig = PackConfig()
        configurationHandler.updateConfigModelFromConfigJson(
            packConfig,
            File("src/test/resources/testresources/gdlauncher/fabric_config.json")
        )
        Assertions.assertEquals(packConfig.minecraftVersion, "1.18.2")
        Assertions.assertEquals(packConfig.modloader, "Fabric")
        Assertions.assertEquals(packConfig.modloaderVersion, "0.14.8")
        configurationHandler.updateConfigModelFromConfigJson(
            packConfig,
            File("src/test/resources/testresources/gdlauncher/forge_config.json")
        )
        Assertions.assertEquals(packConfig.minecraftVersion, "1.18.2")
        Assertions.assertEquals(packConfig.modloader, "Forge")
        Assertions.assertEquals(packConfig.modloaderVersion, "40.1.52")
    }

    @Suppress("SpellCheckingInspection")
    @Test
    @Throws(IOException::class)
    fun updateConfigModelFromMMCPackTest() {
        val packConfig = PackConfig()
        configurationHandler.updateConfigModelFromMMCPack(
            packConfig,
            File("src/test/resources/testresources/multimc/fabric_mmc-pack.json")
        )
        Assertions.assertEquals(packConfig.minecraftVersion, "1.18.1")
        Assertions.assertEquals(packConfig.modloader, "Fabric")
        Assertions.assertEquals(packConfig.modloaderVersion, "0.12.12")
        configurationHandler.updateConfigModelFromMMCPack(
            packConfig,
            File("src/test/resources/testresources/multimc/forge_mmc-pack.json")
        )
        Assertions.assertEquals(packConfig.minecraftVersion, "1.16.5")
        Assertions.assertEquals(packConfig.modloader, "Forge")
        Assertions.assertEquals(packConfig.modloaderVersion, "36.2.23")
        configurationHandler.updateConfigModelFromMMCPack(
            packConfig,
            File("src/test/resources/testresources/multimc/quilt_mmc-pack.json")
        )
        Assertions.assertEquals(packConfig.minecraftVersion, "1.19")
        Assertions.assertEquals(packConfig.modloader, "Quilt")
        Assertions.assertEquals(packConfig.modloaderVersion, "0.17.0")
    }

    @Suppress("SpellCheckingInspection")
    @Test
    @Throws(IOException::class)
    fun updateDestinationFromInstanceCfgTest() {
        Assertions.assertEquals(
            configurationHandler.updateDestinationFromInstanceCfg(
                File("src/test/resources/testresources/multimc/better_mc_instance.cfg")
            ),
            "Better Minecraft [FABRIC] - 1.18.1"
        )
        Assertions.assertEquals(
            configurationHandler.updateDestinationFromInstanceCfg(
                File("src/test/resources/testresources/multimc/all_the_mods_instance.cfg")
            ),
            "All the Mods 6 - ATM6 - 1.16.5"
        )
    }

    @Test
    fun suggestInclusionsTest() {
        val dirs: List<InclusionSpecification> = configurationHandler.suggestInclusions("src/test/resources/fabric_tests")
        dirs.any { inclusion -> inclusion.source == "config" }
        dirs.any { inclusion -> inclusion.source == "defaultconfigs" }
        dirs.any { inclusion -> inclusion.source == "mods" }
        dirs.any { inclusion -> inclusion.source == "scripts" }
        dirs.any { inclusion -> inclusion.source == "seeds" }
        dirs.any { inclusion -> inclusion.source != "server_pack" }
    }

    @Test
    @Throws(IOException::class)
    fun directoriesInModpackZipTest() {
        var entries: List<String?> = configurationHandler.getDirectoriesInModpackZipBaseDirectory(
            File(
                "src/test/resources/testresources/Survive_Create_Prosper_4_invalid.zip"
            )
        )
        Assertions.assertEquals(1, entries.size)
        Assertions.assertTrue(entries.contains("overrides/"))
        entries = configurationHandler.getDirectoriesInModpackZipBaseDirectory(
            File("src/test/resources/testresources/Survive_Create_Prosper_4_valid.zip")
        )
        Assertions.assertTrue(entries.size > 1)
        Assertions.assertTrue(entries.contains("mods/"))
        Assertions.assertTrue(entries.contains("config/"))
    }

    @Test
    @Throws(IOException::class)
    fun filesAndDirsInZipTest() {
        Assertions.assertTrue(
            configurationHandler
                .getFilesInModpackZip(
                    File(
                        "src/test/resources/testresources/Survive_Create_Prosper_4_invalid.zip"
                    )
                )
                .isNotEmpty()
        )
        Assertions.assertTrue(
            configurationHandler
                .getFilesInModpackZip(
                    File(
                        "src/test/resources/testresources/Survive_Create_Prosper_4_valid.zip"
                    )
                )
                .isNotEmpty()
        )
        Assertions.assertTrue(
            configurationHandler
                .getDirectoriesInModpackZip(
                    File(
                        "src/test/resources/testresources/Survive_Create_Prosper_4_invalid.zip"
                    )
                )
                .isNotEmpty()
        )
        Assertions.assertTrue(
            configurationHandler
                .getDirectoriesInModpackZip(
                    File(
                        "src/test/resources/testresources/Survive_Create_Prosper_4_valid.zip"
                    )
                )
                .isNotEmpty()
        )
        Assertions.assertTrue(
            configurationHandler
                .getAllFilesAndDirectoriesInModpackZip(
                    File(
                        "src/test/resources/testresources/Survive_Create_Prosper_4_invalid.zip"
                    )
                )
                .isNotEmpty()
        )
        Assertions.assertTrue(
            configurationHandler
                .getAllFilesAndDirectoriesInModpackZip(
                    File(
                        "src/test/resources/testresources/Survive_Create_Prosper_4_valid.zip"
                    )
                )
                .isNotEmpty()
        )
    }
}