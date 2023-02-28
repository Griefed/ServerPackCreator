package de.griefed.serverpackcreator.api

import com.electronwill.nightconfig.core.CommentedConfig
import net.lingala.zip4j.ZipFile
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File
import java.io.IOException
import java.nio.file.Paths
import java.util.*

internal class ConfigurationHandlerTest {
    private val applicationProperties =
        ApiWrapper.api(File("src/jvmTest/resources/serverpackcreator.properties")).apiProperties
    private val configurationHandler =
        ApiWrapper.api(File("src/jvmTest/resources/serverpackcreator.properties")).configurationHandler!!
    private val versionMeta =
        ApiWrapper.api(File("src/jvmTest/resources/serverpackcreator.properties")).versionMeta!!
    private val projectDir = applicationProperties.homeDirectory.parentFile.parentFile

    @Test
    fun checkConfigFileTest() {
        Assertions.assertFalse(configurationHandler.checkConfiguration(File("src/jvmTest/resources/testresources/spcconfs/serverpackcreator.conf")))
    }

    @Test
    fun isDirTestCopyDirs() {
        @Suppress("SpellCheckingInspection")
        Assertions.assertTrue(configurationHandler.checkConfiguration(File("src/jvmTest/resources/testresources/spcconfs/serverpackcreator_copydirs.conf")))
    }

    @Test
    fun isDirTestJavaPath() {
        @Suppress("SpellCheckingInspection")
        Assertions.assertFalse(configurationHandler.checkConfiguration(File("src/jvmTest/resources/testresources/spcconfs/serverpackcreator_javapath.conf")))
    }

    @Test
    fun isDirTestMinecraftVersion() {
        @Suppress("SpellCheckingInspection")
        Assertions.assertTrue(configurationHandler.checkConfiguration(File("src/jvmTest/resources/testresources/spcconfs/serverpackcreator_minecraftversion.conf")))
    }

    @Test
    fun isModLoaderLegacyFabric() {
        Assertions.assertFalse(configurationHandler.checkConfiguration(File("src/jvmTest/resources/testresources/spcconfs/serverpackcreator_legacyfabric.conf")))
    }

    @Test
    fun isModLoaderQuilt() {
        Assertions.assertFalse(configurationHandler.checkConfiguration(File("src/jvmTest/resources/testresources/spcconfs/serverpackcreator_quilt.conf")))
    }

    @Test
    fun isDirTestModLoaderFalse() {
        @Suppress("SpellCheckingInspection")
        Assertions.assertTrue(configurationHandler.checkConfiguration(File("src/jvmTest/resources/testresources/spcconfs/serverpackcreator_modloaderfalse.conf")))
    }

    @Test
    fun isDirTestModLoaderVersion() {
        @Suppress("SpellCheckingInspection")
        Assertions.assertTrue(configurationHandler.checkConfiguration(File("src/jvmTest/resources/testresources/spcconfs/serverpackcreator_modloaderversion.conf")))
    }

    @Test
    fun checkModpackDirTest() {
        val modpackDirCorrect = "src/jvmTest/resources/forge_tests"
        Assertions.assertTrue(configurationHandler.checkModpackDir(modpackDirCorrect, ArrayList(100)))
    }

    @Test
    fun checkModpackDirTestFalse() {
        Assertions.assertFalse(configurationHandler.checkModpackDir("modpackDir", ArrayList(100)))
    }

    @Test
    fun checkCopyDirsTest() {
        val modpackDir = "src/jvmTest/resources/forge_tests"
        val copyDirs = arrayListOf("config", "mods", "scripts", "seeds", "defaultconfigs")
        Assertions.assertTrue(configurationHandler.checkCopyDirs(copyDirs, modpackDir, ArrayList(100)))
    }

    @Suppress("SpellCheckingInspection")
    @Test
    fun checkCopyDirsTestFalse() {
        val modpackDir = "src/jvmTest/resources/forge_tests"
        val copyDirsInvalid = arrayListOf("configs", "modss", "scriptss", "seedss", "defaultconfigss")
        Assertions.assertFalse(configurationHandler.checkCopyDirs(copyDirsInvalid, modpackDir, ArrayList(100)))
    }

    @Test
    fun checkCopyDirsTestFiles() {
        val modpackDir = "src/jvmTest/resources/forge_tests"
        val copyDirsAndFiles = arrayListOf(
            "config",
            "mods",
            "scripts",
            "seeds",
            "defaultconfigs",
            "test.txt;test.txt",
            "test2.txt;test2.txt"
        )
        Assertions.assertTrue(configurationHandler.checkCopyDirs(copyDirsAndFiles, modpackDir, ArrayList(100)))
    }

    @Suppress("SpellCheckingInspection")
    @Test
    fun checkCopyDirsTestFilesFalse() {
        val modpackDir = "src/jvmTest/resources/forge_tests"
        val copyDirsAndFilesFalse = arrayListOf(
            "configs",
            "modss",
            "scriptss",
            "seedss",
            "defaultconfigss",
            "READMEee.md;README.md",
            "LICENSEee;LICENSE",
            "LICENSEee;test/LICENSE",
            "LICENSEee;test/license.md"
        )
        Assertions.assertFalse(configurationHandler.checkCopyDirs(copyDirsAndFilesFalse, modpackDir, ArrayList(100)))
    }

    @Test
    fun checkModloaderTestForge() {
        Assertions.assertTrue(configurationHandler.checkModloader("Forge"))
        Assertions.assertTrue(configurationHandler.checkModloader("fOrGe"))
        Assertions.assertTrue(configurationHandler.checkModloader("Fabric"))
        Assertions.assertTrue(configurationHandler.checkModloader("fAbRiC"))
        Assertions.assertTrue(configurationHandler.checkModloader("Quilt"))
        Assertions.assertTrue(configurationHandler.checkModloader("qUiLt"))
        Assertions.assertTrue(configurationHandler.checkModloader("lEgAcYfAbRiC"))
        Assertions.assertTrue(configurationHandler.checkModloader("LegacyFabric"))
        Assertions.assertFalse(configurationHandler.checkModloader("modloader"))
    }

    @Test
    fun checkModloaderVersionTestForge() {
        Assertions.assertTrue(configurationHandler.checkModloaderVersion("Forge", "36.1.2", "1.16.5"))
        Assertions.assertFalse(configurationHandler.checkModloaderVersion("Forge", "90.0.0", "1.16.5"))
    }

    @Test
    fun checkModloaderVersionTestFabric() {
        Assertions.assertTrue(configurationHandler.checkModloaderVersion("Fabric", "0.11.3", "1.16.5"))
        Assertions.assertFalse(
            configurationHandler.checkModloaderVersion("Fabric", "0.90.3", "1.16.5")
        )
    }

    @Test
    fun checkModloaderVersionTestQuilt() {
        Assertions.assertTrue(configurationHandler.checkModloaderVersion("Quilt", "0.16.1", "1.16.5"))
        Assertions.assertFalse(configurationHandler.checkModloaderVersion("Quilt", "0.90.3", "1.16.5"))
    }

    @Test
    fun isLegacyFabricVersionCorrectTest() {
        Assertions.assertTrue(
            configurationHandler.checkModloaderVersion("LegacyFabric", "0.13.3", "1.12.2")
        )
        Assertions.assertFalse(
            configurationHandler.checkModloaderVersion("LegacyFabric", "0.999.3", "1.12.2")
        )
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

        val copyDirs = arrayListOf("config", "mods", "scripts", "seeds", "defaultconfigs")
        val packConfig = PackConfig()
        packConfig.modpackDir = "src/jvmTest/resources/forge_tests"
        packConfig.setClientMods(clientMods)
        packConfig.setCopyDirs(copyDirs)
        packConfig.isServerInstallationDesired = true
        packConfig.isServerIconInclusionDesired = true
        packConfig.isServerPropertiesInclusionDesired = true
        packConfig.isZipCreationDesired = true
        packConfig.modloader = "Forge"
        packConfig.modloaderVersion = "36.1.2"
        packConfig.minecraftVersion = "1.16.5"
        Assertions.assertFalse(configurationHandler.checkConfiguration(packConfig))
        Assertions.assertTrue(
            packConfig.scriptSettings.containsKey("SPC_MINECRAFT_SERVER_URL_SPC")
        )
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
            versionMeta.minecraft.getServer(packConfig.minecraftVersion).get().url()
                .get().toString(),
            packConfig.scriptSettings["SPC_MINECRAFT_SERVER_URL_SPC"]
        )
        Assertions.assertEquals(
            applicationProperties.apiVersion,
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
        packConfig.modpackDir = "src/jvmTest/resources/testresources/Survive_Create_Prosper_4_valid.zip"
        Assertions.assertFalse(configurationHandler.checkConfiguration(packConfig))
        packConfig = PackConfig()
        packConfig.modpackDir = "src/jvmTest/resources/testresources/Survive_Create_Prosper_4_invalid.zip"
        Assertions.assertTrue(configurationHandler.checkConfiguration(packConfig))
    }

    @Test
    fun checkConfigurationFileTest() {
        Assertions.assertFalse(configurationHandler.checkConfiguration(File("src/jvmTest/resources/testresources/spcconfs/serverpackcreator.conf")))
    }

    @Test
    fun checkConfigurationFileAndModelTest() {
        var packConfig = PackConfig()
        Assertions.assertFalse(
            configurationHandler.checkConfiguration(
                File("src/jvmTest/resources/testresources/spcconfs/serverpackcreator.conf"),
                packConfig
            )
        )
        Assertions.assertEquals("src/jvmTest/resources/forge_tests", packConfig.modpackDir)
        Assertions.assertEquals("1.16.5", packConfig.minecraftVersion)
        Assertions.assertEquals("Forge", packConfig.modloader)
        Assertions.assertEquals("36.1.2", packConfig.modloaderVersion)
        packConfig = PackConfig()
        Assertions.assertFalse(
            configurationHandler.checkConfiguration(
                File("src/jvmTest/resources/testresources/spcconfs/serverpackcreator.conf"),
                packConfig,
                ArrayList(5),
                false
            )
        )
        Assertions.assertEquals("src/jvmTest/resources/forge_tests", packConfig.modpackDir)
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
        val copyDirs = arrayListOf("config", "mods", "scripts", "seeds", "defaultconfigs")
        packConfig.modpackDir = "src/jvmTest/resources/forge_tests"
        packConfig.setClientMods(clientMods)
        packConfig.setCopyDirs(copyDirs)
        packConfig.isServerIconInclusionDesired = true
        packConfig.isServerIconInclusionDesired = true
        packConfig.isServerPropertiesInclusionDesired = true
        packConfig.isZipCreationDesired = true
        packConfig.modloader = "Forge"
        packConfig.modloaderVersion = "36.1.2"
        packConfig.minecraftVersion = "1.16.5"
        Assertions.assertFalse(
            configurationHandler.checkConfiguration(packConfig, ArrayList(5), true)
        )
        Assertions.assertEquals(
            "src/jvmTest/resources/forge_tests", packConfig.modpackDir
        )
        Assertions.assertEquals("1.16.5", packConfig.minecraftVersion)
        Assertions.assertEquals("Forge", packConfig.modloader)
        Assertions.assertEquals("36.1.2", packConfig.modloaderVersion)
        packConfig.modloader = "Fabric"
        packConfig.modloaderVersion = "0.14.6"
        Assertions.assertFalse(
            configurationHandler.checkConfiguration(packConfig, ArrayList(5), true)
        )
        Assertions.assertEquals("Fabric", packConfig.modloader)
        Assertions.assertEquals("0.14.6", packConfig.modloaderVersion)
        packConfig.modloader = "Quilt"
        packConfig.modloaderVersion = "0.16.1"
        Assertions.assertFalse(
            configurationHandler.checkConfiguration(packConfig, ArrayList(5), true)
        )
        Assertions.assertEquals("Quilt", packConfig.modloader)
        Assertions.assertEquals("0.16.1", packConfig.modloaderVersion)
    }

    @Test
    fun checkIconAndPropertiesTest() {
        Assertions.assertTrue(configurationHandler.checkIconAndProperties(""))
        Assertions.assertFalse(configurationHandler.checkIconAndProperties("/some/path"))
        Assertions.assertTrue(
            configurationHandler.checkIconAndProperties(
                File(
                    projectDir.path,
                    "/img/prosper.png"
                ).path
            )
        )
    }

    @Test
    fun checkZipArchiveTest() {
        Assertions.assertFalse(
            configurationHandler.checkZipArchive(
                Paths.get("src/jvmTest/resources/testresources/Survive_Create_Prosper_4_valid.zip").toAbsolutePath().toString(),
                ArrayList(5)
            )
        )
        Assertions.assertTrue(
            configurationHandler.checkZipArchive(
                Paths.get("src/jvmTest/resources/testresources/Survive_Create_Prosper_4_invalid.zip").toAbsolutePath().toString(),
                ArrayList(5)
            )
        )
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

        val copyDirs = arrayListOf("config", "mods", "scripts", "seeds", "defaultconfigs")
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
                copyDirs,
                "src/jvmTest/resources/fabric_tests",
                javaPath,
                "1.16.5",
                "Fabric",
                "0.11.3",
                javaArgs,
                "",
                "",
                includeServerInstallation = true,
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

        val copyDirs = arrayListOf("config", "mods", "scripts", "seeds", "defaultconfigs")
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
                copyDirs,
                "src/jvmTest/resources/forge_tests",
                javaPath,
                "1.16.5",
                "Forge",
                "36.1.2",
                javaArgs,
                "",
                "",
                includeServerInstallation = true,
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
        packConfig.modpackDir = "src/jvmTest/resources/forge_tests"
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
        packConfig.setCopyDirs(arrayListOf("config", "mods", "scripts", "seeds", "defaultconfigs"))
        packConfig.isServerInstallationDesired = true
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
        packConfig.modpackDir = "src/jvmTest/resources/forge_tests"
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
        packConfig.setCopyDirs(arrayListOf("config", "mods", "scripts", "seeds", "defaultconfigs"))
        packConfig.isServerInstallationDesired = true
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
            File("src/jvmTest/resources/testresources/modrinth/forge_modrinth.index.json")
        )
        Assertions.assertEquals(packConfig.minecraftVersion, "1.18.2")
        Assertions.assertEquals(packConfig.modloader, "Forge")
        Assertions.assertEquals(packConfig.modloaderVersion, "40.1.48")
        configurationHandler.updateConfigModelFromModrinthManifest(
            packConfig,
            File("src/jvmTest/resources/testresources/modrinth/fabric_modrinth.index.json")
        )
        Assertions.assertEquals(packConfig.minecraftVersion, "1.19")
        Assertions.assertEquals(packConfig.modloader, "Fabric")
        Assertions.assertEquals(packConfig.modloaderVersion, "0.14.8")
        configurationHandler.updateConfigModelFromModrinthManifest(
            packConfig,
            File("src/jvmTest/resources/testresources/modrinth/quilt_modrinth.index.json")
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
            File("src/jvmTest/resources/testresources/curseforge/forge_manifest.json")
        )
        Assertions.assertEquals(packConfig.minecraftVersion, "1.16.5")
        Assertions.assertEquals(packConfig.modloader, "Forge")
        Assertions.assertEquals(packConfig.modloaderVersion, "36.0.1")
        configurationHandler.updateConfigModelFromCurseManifest(
            packConfig,
            File("src/jvmTest/resources/testresources/curseforge/fabric_manifest.json")
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
            File("src/jvmTest/resources/testresources/curseforge/forge_minecraftinstance.json")
        )
        Assertions.assertEquals(packConfig.minecraftVersion, "1.16.5")
        Assertions.assertEquals(packConfig.modloader, "Forge")
        Assertions.assertEquals(packConfig.modloaderVersion, "36.2.4")
        configurationHandler.updateConfigModelFromMinecraftInstance(
            packConfig,
            File("src/jvmTest/resources/testresources/curseforge/fabric_minecraftinstance.json")
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
            File("src/jvmTest/resources/testresources/gdlauncher/fabric_config.json")
        )
        Assertions.assertEquals(packConfig.minecraftVersion, "1.18.2")
        Assertions.assertEquals(packConfig.modloader, "Fabric")
        Assertions.assertEquals(packConfig.modloaderVersion, "0.14.8")
        configurationHandler.updateConfigModelFromConfigJson(
            packConfig,
            File("src/jvmTest/resources/testresources/gdlauncher/forge_config.json")
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
            File("src/jvmTest/resources/testresources/multimc/fabric_mmc-pack.json")
        )
        Assertions.assertEquals(packConfig.minecraftVersion, "1.18.1")
        Assertions.assertEquals(packConfig.modloader, "Fabric")
        Assertions.assertEquals(packConfig.modloaderVersion, "0.12.12")
        configurationHandler.updateConfigModelFromMMCPack(
            packConfig,
            File("src/jvmTest/resources/testresources/multimc/forge_mmc-pack.json")
        )
        Assertions.assertEquals(packConfig.minecraftVersion, "1.16.5")
        Assertions.assertEquals(packConfig.modloader, "Forge")
        Assertions.assertEquals(packConfig.modloaderVersion, "36.2.23")
        configurationHandler.updateConfigModelFromMMCPack(
            packConfig,
            File("src/jvmTest/resources/testresources/multimc/quilt_mmc-pack.json")
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
                File("src/jvmTest/resources/testresources/multimc/better_mc_instance.cfg")
            ),
            "Better Minecraft [FABRIC] - 1.18.1"
        )
        Assertions.assertEquals(
            configurationHandler.updateDestinationFromInstanceCfg(
                File("src/jvmTest/resources/testresources/multimc/all_the_mods_instance.cfg")
            ),
            "All the Mods 6 - ATM6 - 1.16.5"
        )
    }

    @Test
    fun suggestCopyDirsTest() {
        val dirs: List<String> = configurationHandler.suggestCopyDirs("src/jvmTest/resources/fabric_tests")
        Assertions.assertTrue(dirs.contains("config"))
        Assertions.assertTrue(dirs.contains("defaultconfigs"))
        Assertions.assertTrue(dirs.contains("mods"))
        Assertions.assertTrue(dirs.contains("scripts"))
        Assertions.assertTrue(dirs.contains("seeds"))
        Assertions.assertFalse(dirs.contains("server_pack"))
    }

    @Test
    @Throws(IOException::class)
    fun directoriesInModpackZipTest() {
        var entries: List<String?> = configurationHandler.getDirectoriesInModpackZipBaseDirectory(
            File(
                "src/jvmTest/resources/testresources/Survive_Create_Prosper_4_invalid.zip"
            )
        )
        Assertions.assertEquals(1, entries.size)
        Assertions.assertTrue(entries.contains("overrides/"))
        entries = configurationHandler.getDirectoriesInModpackZipBaseDirectory(
            File("src/jvmTest/resources/testresources/Survive_Create_Prosper_4_valid.zip")
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
                        "src/jvmTest/resources/testresources/Survive_Create_Prosper_4_invalid.zip"
                    )
                )
                .isNotEmpty()
        )
        Assertions.assertTrue(
            configurationHandler
                .getFilesInModpackZip(
                    File(
                        "src/jvmTest/resources/testresources/Survive_Create_Prosper_4_valid.zip"
                    )
                )
                .isNotEmpty()
        )
        Assertions.assertTrue(
            configurationHandler
                .getDirectoriesInModpackZip(
                    File(
                        "src/jvmTest/resources/testresources/Survive_Create_Prosper_4_invalid.zip"
                    )
                )
                .isNotEmpty()
        )
        Assertions.assertTrue(
            configurationHandler
                .getDirectoriesInModpackZip(
                    File(
                        "src/jvmTest/resources/testresources/Survive_Create_Prosper_4_valid.zip"
                    )
                )
                .isNotEmpty()
        )
        Assertions.assertTrue(
            configurationHandler
                .getAllFilesAndDirectoriesInModpackZip(
                    File(
                        "src/jvmTest/resources/testresources/Survive_Create_Prosper_4_invalid.zip"
                    )
                )
                .isNotEmpty()
        )
        Assertions.assertTrue(
            configurationHandler
                .getAllFilesAndDirectoriesInModpackZip(
                    File(
                        "src/jvmTest/resources/testresources/Survive_Create_Prosper_4_valid.zip"
                    )
                )
                .isNotEmpty()
        )
    }
}