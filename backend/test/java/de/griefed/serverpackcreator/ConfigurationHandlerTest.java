package de.griefed.serverpackcreator;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.griefed.serverpackcreator.i18n.I18n;
import de.griefed.serverpackcreator.utilities.ConfigUtilities;
import de.griefed.serverpackcreator.utilities.common.Utilities;
import de.griefed.serverpackcreator.versionmeta.VersionMeta;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

class ConfigurationHandlerTest {

  private final ApplicationProperties applicationProperties;
  private final ConfigurationHandler configurationHandler;
  private final VersionMeta versionMeta;

  ConfigurationHandlerTest() throws IOException, ParserConfigurationException, SAXException {
    try {
      FileUtils.copyFile(
          new File("backend/main/resources/serverpackcreator.properties"),
          new File("serverpackcreator.properties"));
    } catch (IOException e) {
      e.printStackTrace();
    }
    applicationProperties = new ApplicationProperties();
    I18n i18N = new I18n(applicationProperties);
    ObjectMapper objectMapper =
        new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
    Utilities utilities = new Utilities(applicationProperties);
    versionMeta =
        new VersionMeta(
            applicationProperties.MINECRAFT_VERSION_MANIFEST(),
            applicationProperties.FORGE_VERSION_MANIFEST(),
            applicationProperties.FABRIC_VERSION_MANIFEST(),
            applicationProperties.FABRIC_INSTALLER_VERSION_MANIFEST(),
            applicationProperties.FABRIC_INTERMEDIARIES_MANIFEST_LOCATION(),
            applicationProperties.QUILT_VERSION_MANIFEST(),
            applicationProperties.QUILT_INSTALLER_VERSION_MANIFEST(),
            applicationProperties.LEGACY_FABRIC_GAME_MANIFEST_LOCATION(),
            applicationProperties.LEGACY_FABRIC_LOADER_MANIFEST_LOCATION(),
            applicationProperties.LEGACY_FABRIC_INSTALLER_MANIFEST_LOCATION(),
            objectMapper);
    configurationHandler =
        new ConfigurationHandler(
            i18N,
            versionMeta,
            applicationProperties,
            utilities,
            new ConfigUtilities(utilities, applicationProperties, objectMapper));

  }

  @Test
  void checkConfigFileTest() {
    Assertions.assertFalse(
        configurationHandler.checkConfiguration(
            new File("./backend/test/resources/testresources/serverpackcreator.conf"), false));
  }

  @Test
  void isDirTestCopyDirs() {
    Assertions.assertTrue(
        configurationHandler.checkConfiguration(
            new File("./backend/test/resources/testresources/serverpackcreator_copydirs.conf"),
            false));
  }

  @Test
  void isDirTestJavaPath() {
    Assertions.assertFalse(
        configurationHandler.checkConfiguration(
            new File("./backend/test/resources/testresources/serverpackcreator_javapath.conf"),
            false));
  }

  @Test
  void isDirTestMinecraftVersion() {
    Assertions.assertTrue(
        configurationHandler.checkConfiguration(
            new File(
                "./backend/test/resources/testresources/serverpackcreator_minecraftversion.conf"),
            false));
  }

  @Test
  void isModLoaderLegacyFabric() {
    Assertions.assertFalse(
        configurationHandler.checkConfiguration(
            new File("./backend/test/resources/testresources/serverpackcreator_legacyfabric.conf"),
            false));
  }

  @Test
  void isModLoaderQuilt() {
    Assertions.assertFalse(
        configurationHandler.checkConfiguration(
            new File("./backend/test/resources/testresources/serverpackcreator_quilt.conf"),
            false));
  }

  @Test
  void isDirTestModLoaderFalse() {
    Assertions.assertTrue(
        configurationHandler.checkConfiguration(
            new File(
                "./backend/test/resources/testresources/serverpackcreator_modloaderfalse.conf"),
            false));
  }

  @Test
  void isDirTestModLoaderVersion() {
    Assertions.assertTrue(
        configurationHandler.checkConfiguration(
            new File(
                "./backend/test/resources/testresources/serverpackcreator_modloaderversion.conf"),
            false));
  }

  @Test
  void checkModpackDirTest() {
    String modpackDirCorrect = "./backend/test/resources/forge_tests";
    Assertions.assertTrue(
        configurationHandler.checkModpackDir(modpackDirCorrect, new ArrayList<>(100)));
  }

  @Test
  void checkModpackDirTestFalse() {
    Assertions.assertFalse(
        configurationHandler.checkModpackDir("modpackDir", new ArrayList<>(100)));
  }

  @Test
  void checkCopyDirsTest() {
    String modpackDir = "backend/test/resources/forge_tests";
    List<String> copyDirs =
        new ArrayList<>(Arrays.asList("config", "mods", "scripts", "seeds", "defaultconfigs"));
    Assertions.assertTrue(
        configurationHandler.checkCopyDirs(copyDirs, modpackDir, new ArrayList<>(100)));
  }

  @Test
  void checkCopyDirsTestFalse() {
    String modpackDir = "backend/test/resources/forge_tests";
    List<String> copyDirsInvalid =
        new ArrayList<>(Arrays.asList("configs", "modss", "scriptss", "seedss", "defaultconfigss"));
    Assertions.assertFalse(
        configurationHandler.checkCopyDirs(copyDirsInvalid, modpackDir, new ArrayList<>(100)));
  }

  @Test
  void checkCopyDirsTestFiles() {
    String modpackDir = "backend/test/resources/forge_tests";
    List<String> copyDirsAndFiles =
        new ArrayList<>(
            Arrays.asList(
                "config",
                "mods",
                "scripts",
                "seeds",
                "defaultconfigs",
                "test.txt;test.txt",
                "test2.txt;test2.txt"));
    Assertions.assertTrue(
        configurationHandler.checkCopyDirs(copyDirsAndFiles, modpackDir, new ArrayList<>(100)));
  }

  @Test
  void checkCopyDirsTestFilesFalse() {
    String modpackDir = "backend/test/resources/forge_tests";
    List<String> copyDirsAndFilesFalse =
        new ArrayList<>(
            Arrays.asList(
                "configs",
                "modss",
                "scriptss",
                "seedss",
                "defaultconfigss",
                "READMEee.md;README.md",
                "LICENSEee;LICENSE",
                "LICENSEee;test/LICENSE",
                "LICENSEee;test/license.md"));
    Assertions.assertFalse(
        configurationHandler.checkCopyDirs(
            copyDirsAndFilesFalse, modpackDir, new ArrayList<>(100)));
  }

  @Test
  void checkJavaPathTest() {
    String javaPath;
    String autoJavaPath = System.getProperty("java.home").replace("\\", "/") + "/bin/java";
    if (autoJavaPath.startsWith("C:")) {
      autoJavaPath = String.format("%s.exe", autoJavaPath);
    }
    if (new File("/usr/bin/java").exists()) {
      javaPath = "/usr/bin/java";
    } else if (new File("/opt/hostedtoolcache/jdk/8.0.282/x64/bin/java").exists()) {
      javaPath = "/opt/hostedtoolcache/jdk/8.0.282/x64/bin/java";
    } else {
      javaPath = autoJavaPath;
    }
    Assertions.assertNotNull(configurationHandler.getJavaPath(javaPath));
    Assertions.assertTrue(new File(configurationHandler.getJavaPath(javaPath)).exists());
  }

  @Test
  void checkModloaderTestForge() {
    Assertions.assertTrue(configurationHandler.checkModloader("Forge"));
    Assertions.assertTrue(configurationHandler.checkModloader("fOrGe"));
    Assertions.assertTrue(configurationHandler.checkModloader("Fabric"));
    Assertions.assertTrue(configurationHandler.checkModloader("fAbRiC"));
    Assertions.assertTrue(configurationHandler.checkModloader("Quilt"));
    Assertions.assertTrue(configurationHandler.checkModloader("qUiLt"));
    Assertions.assertTrue(configurationHandler.checkModloader("lEgAcYfAbRiC"));
    Assertions.assertTrue(configurationHandler.checkModloader("LegacyFabric"));

    Assertions.assertFalse(configurationHandler.checkModloader("modloader"));
  }

  @Test
  void checkModloaderVersionTestForge() {
    Assertions.assertTrue(configurationHandler.checkModloaderVersion("Forge", "36.1.2", "1.16.5"));
    Assertions.assertFalse(configurationHandler.checkModloaderVersion("Forge", "90.0.0", "1.16.5"));
  }

  @Test
  void checkModloaderVersionTestFabric() {
    Assertions.assertTrue(configurationHandler.checkModloaderVersion("Fabric", "0.11.3", "1.16.5"));
    Assertions.assertFalse(configurationHandler.checkModloaderVersion("Fabric", "0.90.3", "1.16.5"));
  }

  @Test
  void checkModloaderVersionTestQuilt() {
    Assertions.assertTrue(configurationHandler.checkModloaderVersion("Quilt", "0.16.1", "1.16.5"));
    Assertions.assertFalse(configurationHandler.checkModloaderVersion("Quilt", "0.90.3", "1.16.5"));
  }

  @Test
  void isLegacyFabricVersionCorrectTest() {
    Assertions.assertTrue(configurationHandler.checkModloaderVersion("LegacyFabric","0.13.3","1.12.2"));
    Assertions.assertFalse(configurationHandler.checkModloaderVersion("LegacyFabric","0.999.3","1.12.2"));
  }

  @Test
  void checkConfigModelTest() {
    List<String> clientMods =
        new ArrayList<>(
            Arrays.asList(
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
                "WorldNameRandomizer"));
    List<String> copyDirs =
        new ArrayList<>(Arrays.asList("config", "mods", "scripts", "seeds", "defaultconfigs"));
    ConfigurationModel configurationModel = new ConfigurationModel();
    configurationModel.setModpackDir("./backend/test/resources/forge_tests");
    configurationModel.setClientMods(clientMods);
    configurationModel.setCopyDirs(copyDirs);
    configurationModel.setJavaPath("");
    configurationModel.setIncludeServerInstallation(true);
    configurationModel.setIncludeServerIcon(true);
    configurationModel.setIncludeServerProperties(true);
    configurationModel.setIncludeZipCreation(true);
    configurationModel.setModLoader("Forge");
    configurationModel.setModLoaderVersion("36.1.2");
    configurationModel.setMinecraftVersion("1.16.5");
    Assertions.assertFalse(configurationHandler.checkConfiguration(configurationModel, false));

    Assertions.assertTrue(configurationModel.getScriptSettings().containsKey("SPC_MINECRAFT_SERVER_URL_SPC"));
    Assertions.assertTrue(configurationModel.getScriptSettings().containsKey("SPC_SERVERPACKCREATOR_VERSION_SPC"));
    Assertions.assertTrue(configurationModel.getScriptSettings().containsKey("SPC_MINECRAFT_VERSION_SPC"));
    Assertions.assertTrue(configurationModel.getScriptSettings().containsKey("SPC_MODLOADER_SPC"));
    Assertions.assertTrue(configurationModel.getScriptSettings().containsKey("SPC_MODLOADER_VERSION_SPC"));
    Assertions.assertTrue(configurationModel.getScriptSettings().containsKey("SPC_JAVA_ARGS_SPC"));
    Assertions.assertTrue(configurationModel.getScriptSettings().containsKey("SPC_JAVA_SPC"));
    Assertions.assertTrue(configurationModel.getScriptSettings().containsKey("SPC_FABRIC_INSTALLER_VERSION_SPC"));
    Assertions.assertTrue(configurationModel.getScriptSettings().containsKey("SPC_LEGACYFABRIC_INSTALLER_VERSION_SPC"));
    Assertions.assertTrue(configurationModel.getScriptSettings().containsKey("SPC_QUILT_INSTALLER_VERSION_SPC"));

    Assertions.assertEquals(configurationModel.getMinecraftVersion(),configurationModel.getScriptSettings().get("SPC_MINECRAFT_VERSION_SPC"));
    Assertions.assertEquals(configurationModel.getModLoader(),configurationModel.getScriptSettings().get("SPC_MODLOADER_SPC"));
    Assertions.assertEquals(configurationModel.getModLoaderVersion(),configurationModel.getScriptSettings().get("SPC_MODLOADER_VERSION_SPC"));
    Assertions.assertEquals(configurationModel.getJavaArgs(),configurationModel.getScriptSettings().get("SPC_JAVA_ARGS_SPC"));
    Assertions.assertEquals("java",configurationModel.getScriptSettings().get("SPC_JAVA_SPC"));
    Assertions.assertEquals(versionMeta.minecraft().getServer(configurationModel.getMinecraftVersion()).get().url().get().toString(),configurationModel.getScriptSettings().get("SPC_MINECRAFT_SERVER_URL_SPC"));
    Assertions.assertEquals(applicationProperties.SERVERPACKCREATOR_VERSION(),configurationModel.getScriptSettings().get("SPC_SERVERPACKCREATOR_VERSION_SPC"));
    Assertions.assertEquals(versionMeta.fabric().releaseInstaller(),configurationModel.getScriptSettings().get("SPC_FABRIC_INSTALLER_VERSION_SPC"));
    Assertions.assertEquals(versionMeta.legacyFabric().releaseInstaller(),configurationModel.getScriptSettings().get("SPC_LEGACYFABRIC_INSTALLER_VERSION_SPC"));
    Assertions.assertEquals(versionMeta.quilt().releaseInstaller(),configurationModel.getScriptSettings().get("SPC_QUILT_INSTALLER_VERSION_SPC"));
  }

  @Test
  void zipArchiveTest() {
    ConfigurationModel configurationModel = new ConfigurationModel();
    configurationModel.setModpackDir(
        "backend/test/resources/testresources/Survive_Create_Prosper_4_valid.zip");
    Assertions.assertFalse(configurationHandler.checkConfiguration(configurationModel, true));
    configurationModel = new ConfigurationModel();
    configurationModel.setModpackDir(
        "backend/test/resources/testresources/Survive_Create_Prosper_4_invalid.zip");
    Assertions.assertTrue(configurationHandler.checkConfiguration(configurationModel, true));
  }

  @Test
  void checkConfigurationFileTest() {
    Assertions.assertFalse(
        configurationHandler.checkConfiguration(
            new File("backend/test/resources/testresources/serverpackcreator.conf"), true));
  }

  @Test
  void checkConfigurationFileAndModelTest() {
    ConfigurationModel configurationModel = new ConfigurationModel();
    Assertions.assertFalse(
        configurationHandler.checkConfiguration(
            new File("backend/test/resources/testresources/serverpackcreator.conf"),
            configurationModel,
            true));
    Assertions.assertEquals(
        "./backend/test/resources/forge_tests", configurationModel.getModpackDir());
    Assertions.assertEquals("1.16.5", configurationModel.getMinecraftVersion());
    Assertions.assertEquals("Forge", configurationModel.getModLoader());
    Assertions.assertEquals("36.1.2", configurationModel.getModLoaderVersion());

    configurationModel = new ConfigurationModel();
    Assertions.assertFalse(
        configurationHandler.checkConfiguration(
            new File("backend/test/resources/testresources/serverpackcreator.conf"),
            configurationModel,
            new ArrayList<>(),
            false));
    Assertions.assertEquals(
        "./backend/test/resources/forge_tests", configurationModel.getModpackDir());
    Assertions.assertEquals("1.16.5", configurationModel.getMinecraftVersion());
    Assertions.assertEquals("Forge", configurationModel.getModLoader());
    Assertions.assertEquals("36.1.2", configurationModel.getModLoaderVersion());
  }

  @Test
  void checkConfigurationNoFileTest() {
    ConfigurationModel configurationModel = new ConfigurationModel();
    List<String> clientMods =
        new ArrayList<>(
            Arrays.asList(
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
                "WorldNameRandomizer"));
    List<String> copyDirs =
        new ArrayList<>(Arrays.asList("config", "mods", "scripts", "seeds", "defaultconfigs"));
    configurationModel.setModpackDir("./backend/test/resources/forge_tests");
    configurationModel.setClientMods(clientMods);
    configurationModel.setCopyDirs(copyDirs);
    configurationModel.setJavaPath("");
    configurationModel.setIncludeServerInstallation(true);
    configurationModel.setIncludeServerIcon(true);
    configurationModel.setIncludeServerProperties(true);
    configurationModel.setIncludeZipCreation(true);
    configurationModel.setModLoader("Forge");
    configurationModel.setModLoaderVersion("36.1.2");
    configurationModel.setMinecraftVersion("1.16.5");
    Assertions.assertFalse(
        configurationHandler.checkConfiguration(configurationModel, new ArrayList<>(), true));
    Assertions.assertEquals(
        "./backend/test/resources/forge_tests", configurationModel.getModpackDir());
    Assertions.assertEquals("1.16.5", configurationModel.getMinecraftVersion());
    Assertions.assertEquals("Forge", configurationModel.getModLoader());
    Assertions.assertEquals("36.1.2", configurationModel.getModLoaderVersion());

    configurationModel.setModLoader("Fabric");
    configurationModel.setModLoaderVersion("0.14.6");
    Assertions.assertFalse(
        configurationHandler.checkConfiguration(configurationModel, new ArrayList<>(), true));
    Assertions.assertEquals("Fabric", configurationModel.getModLoader());
    Assertions.assertEquals("0.14.6", configurationModel.getModLoaderVersion());

    configurationModel.setModLoader("Quilt");
    configurationModel.setModLoaderVersion("0.16.1");
    Assertions.assertFalse(
        configurationHandler.checkConfiguration(configurationModel, new ArrayList<>(), true));
    Assertions.assertEquals("Quilt", configurationModel.getModLoader());
    Assertions.assertEquals("0.16.1", configurationModel.getModLoaderVersion());
  }

  @Test
  void checkIconAndPropertiesTest() {
    Assertions.assertTrue(configurationHandler.checkIconAndProperties(""));
    Assertions.assertFalse(configurationHandler.checkIconAndProperties("/some/path"));
    Assertions.assertTrue(configurationHandler.checkIconAndProperties("img/prosper.png"));
  }

  @Test
  void checkZipArchiveTest() {
    Assertions.assertFalse(
        configurationHandler.checkZipArchive(
            Paths.get("backend/test/resources/testresources/Survive_Create_Prosper_4_valid.zip"),
            new ArrayList<>()));

    Assertions.assertTrue(
        configurationHandler.checkZipArchive(
            Paths.get("backend/test/resources/testresources/Survive_Create_Prosper_4_invalid.zip"),
            new ArrayList<>()));
  }
}
