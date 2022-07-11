package de.griefed.serverpackcreator;

import de.griefed.serverpackcreator.i18n.I18n;
import de.griefed.serverpackcreator.utilities.ConfigUtilities;
import de.griefed.serverpackcreator.utilities.common.Utilities;
import de.griefed.serverpackcreator.versionmeta.VersionMeta;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ConfigurationHandlerTest {

  private final ConfigurationHandler CONFIGURATIONHANDLER;
  private final VersionMeta VERSIONMETA;

  ConfigurationHandlerTest() throws IOException {
    try {
      FileUtils.copyFile(
          new File("backend/main/resources/serverpackcreator.properties"),
          new File("serverpackcreator.properties"));
    } catch (IOException e) {
      e.printStackTrace();
    }

    ApplicationProperties APPLICATIONPROPERTIES = new ApplicationProperties();
    I18n I18N = new I18n(APPLICATIONPROPERTIES);
    ServerPackCreator SERVER_PACK_CREATOR = new ServerPackCreator(new String[] {"--setup"});
    SERVER_PACK_CREATOR.run(ServerPackCreator.CommandlineParser.Mode.SETUP);
    this.VERSIONMETA =
        new VersionMeta(
            APPLICATIONPROPERTIES.MINECRAFT_VERSION_MANIFEST_LOCATION(),
            APPLICATIONPROPERTIES.FORGE_VERSION_MANIFEST_LOCATION(),
            APPLICATIONPROPERTIES.FABRIC_VERSION_MANIFEST_LOCATION(),
            APPLICATIONPROPERTIES.FABRIC_INSTALLER_VERSION_MANIFEST_LOCATION(),
            APPLICATIONPROPERTIES.QUILT_VERSION_MANIFEST_LOCATION(),
            APPLICATIONPROPERTIES.QUILT_INSTALLER_VERSION_MANIFEST_LOCATION());
    Utilities UTILITIES = new Utilities(I18N, APPLICATIONPROPERTIES);
    ConfigUtilities CONFIGUTILITIES =
        new ConfigUtilities(I18N, UTILITIES, APPLICATIONPROPERTIES, VERSIONMETA);
    this.CONFIGURATIONHANDLER =
        new ConfigurationHandler(
            I18N, VERSIONMETA, APPLICATIONPROPERTIES, UTILITIES, CONFIGUTILITIES);
  }

  @Test
  void checkConfigFileTest() {
    Assertions.assertFalse(
        CONFIGURATIONHANDLER.checkConfiguration(
            new File("./backend/test/resources/testresources/serverpackcreator.conf"), false));
  }

  @Test
  void isDirTestCopyDirs() {
    Assertions.assertTrue(
        CONFIGURATIONHANDLER.checkConfiguration(
            new File("./backend/test/resources/testresources/serverpackcreator_copydirs.conf"),
            false));
  }

  @Test
  void isDirTestJavaPath() {
    Assertions.assertFalse(
        CONFIGURATIONHANDLER.checkConfiguration(
            new File("./backend/test/resources/testresources/serverpackcreator_javapath.conf"),
            false));
  }

  @Test
  void isDirTestMinecraftVersion() {
    Assertions.assertTrue(
        CONFIGURATIONHANDLER.checkConfiguration(
            new File(
                "./backend/test/resources/testresources/serverpackcreator_minecraftversion.conf"),
            false));
  }

  @Test
  void isDirTestModLoader() {
    Assertions.assertFalse(
        CONFIGURATIONHANDLER.checkConfiguration(
            new File("./backend/test/resources/testresources/serverpackcreator_modloader.conf"),
            false));
  }

  @Test
  void isDirTestModLoaderFalse() {
    Assertions.assertTrue(
        CONFIGURATIONHANDLER.checkConfiguration(
            new File(
                "./backend/test/resources/testresources/serverpackcreator_modloaderfalse.conf"),
            false));
  }

  @Test
  void isDirTestModLoaderVersion() {
    Assertions.assertTrue(
        CONFIGURATIONHANDLER.checkConfiguration(
            new File(
                "./backend/test/resources/testresources/serverpackcreator_modloaderversion.conf"),
            false));
  }

  @Test
  void checkModpackDirTest() {
    String modpackDirCorrect = "./backend/test/resources/forge_tests";
    Assertions.assertTrue(
        CONFIGURATIONHANDLER.checkModpackDir(modpackDirCorrect, new ArrayList<>(100)));
  }

  @Test
  void checkModpackDirTestFalse() {
    Assertions.assertFalse(
        CONFIGURATIONHANDLER.checkModpackDir("modpackDir", new ArrayList<>(100)));
  }

  @Test
  void checkCopyDirsTest() {
    String modpackDir = "backend/test/resources/forge_tests";
    List<String> copyDirs =
        new ArrayList<>(Arrays.asList("config", "mods", "scripts", "seeds", "defaultconfigs"));
    Assertions.assertTrue(
        CONFIGURATIONHANDLER.checkCopyDirs(copyDirs, modpackDir, new ArrayList<>(100)));
  }

  @Test
  void checkCopyDirsTestFalse() {
    String modpackDir = "backend/test/resources/forge_tests";
    List<String> copyDirsInvalid =
        new ArrayList<>(Arrays.asList("configs", "modss", "scriptss", "seedss", "defaultconfigss"));
    Assertions.assertFalse(
        CONFIGURATIONHANDLER.checkCopyDirs(copyDirsInvalid, modpackDir, new ArrayList<>(100)));
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
        CONFIGURATIONHANDLER.checkCopyDirs(copyDirsAndFiles, modpackDir, new ArrayList<>(100)));
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
        CONFIGURATIONHANDLER.checkCopyDirs(
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
    Assertions.assertNotNull(CONFIGURATIONHANDLER.getJavaPath(javaPath));
    Assertions.assertTrue(new File(CONFIGURATIONHANDLER.getJavaPath(javaPath)).exists());
  }

  @Test
  void checkModloaderTestForge() {
    Assertions.assertTrue(CONFIGURATIONHANDLER.checkModloader("Forge"));
    Assertions.assertTrue(CONFIGURATIONHANDLER.checkModloader("fOrGe"));
    Assertions.assertTrue(CONFIGURATIONHANDLER.checkModloader("Fabric"));
    Assertions.assertTrue(CONFIGURATIONHANDLER.checkModloader("fAbRiC"));
    Assertions.assertTrue(CONFIGURATIONHANDLER.checkModloader("Quilt"));
    Assertions.assertTrue(CONFIGURATIONHANDLER.checkModloader("qUiLt"));

    Assertions.assertFalse(CONFIGURATIONHANDLER.checkModloader("modloader"));
  }

  @Test
  void checkModloaderVersionTestForge() {
    Assertions.assertTrue(CONFIGURATIONHANDLER.checkModloaderVersion("Forge", "36.1.2", "1.16.5"));
    Assertions.assertFalse(CONFIGURATIONHANDLER.checkModloaderVersion("Forge", "90.0.0", "1.16.5"));
  }

  @Test
  void checkModloaderVersionTestFabric() {
    Assertions.assertTrue(CONFIGURATIONHANDLER.checkModloaderVersion("Fabric", "0.11.3", "1.16.5"));
    Assertions.assertFalse(
        CONFIGURATIONHANDLER.checkModloaderVersion("Fabric", "0.90.3", "1.16.5"));
  }

  @Test
  void checkModloaderVersionTestQuilt() {
    Assertions.assertTrue(CONFIGURATIONHANDLER.checkModloaderVersion("Quilt", "0.16.1", "1.16.5"));
    Assertions.assertFalse(CONFIGURATIONHANDLER.checkModloaderVersion("Quilt", "0.90.3", "1.16.5"));
  }

  @Test
  void isMinecraftVersionCorrectTest() {
    Assertions.assertTrue(VERSIONMETA.minecraft().checkMinecraftVersion("1.16.5"));
    Assertions.assertFalse(VERSIONMETA.minecraft().checkMinecraftVersion("1.99.5"));
  }

  @Test
  void isFabricVersionCorrectTest() {
    Assertions.assertTrue(VERSIONMETA.fabric().checkFabricVersion("0.11.3"));
    Assertions.assertFalse(VERSIONMETA.fabric().checkFabricVersion("0.90.3"));
  }

  @Test
  void isForgeVersionCorrectTest() {
    Assertions.assertTrue(VERSIONMETA.forge().checkForgeAndMinecraftVersion("1.16.5", "36.1.2"));
    Assertions.assertFalse(VERSIONMETA.forge().checkForgeAndMinecraftVersion("1.16.5", "99.0.0"));
  }

  @Test
  void isQuiltVersionCorrectTest() {
    Assertions.assertTrue(VERSIONMETA.quilt().checkQuiltVersion("0.16.1"));
    Assertions.assertFalse(VERSIONMETA.quilt().checkQuiltVersion("0.90.3"));
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
    Assertions.assertFalse(CONFIGURATIONHANDLER.checkConfiguration(configurationModel, false));
  }

  @Test
  void zipArchiveTest() {
    ConfigurationModel configurationModel = new ConfigurationModel();
    configurationModel.setModpackDir(
        "backend/test/resources/testresources/Survive_Create_Prosper_4_valid.zip");
    Assertions.assertFalse(CONFIGURATIONHANDLER.checkConfiguration(configurationModel, true));
    configurationModel = new ConfigurationModel();
    configurationModel.setModpackDir(
        "backend/test/resources/testresources/Survive_Create_Prosper_4_invalid.zip");
    Assertions.assertTrue(CONFIGURATIONHANDLER.checkConfiguration(configurationModel, true));
  }

  @Test
  void checkConfigurationFileTest() {
    Assertions.assertFalse(
        CONFIGURATIONHANDLER.checkConfiguration(
            new File("backend/test/resources/testresources/serverpackcreator.conf"), true));
  }

  @Test
  void checkConfigurationFileAndModelTest() {
    ConfigurationModel configurationModel = new ConfigurationModel();
    Assertions.assertFalse(
        CONFIGURATIONHANDLER.checkConfiguration(
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
        CONFIGURATIONHANDLER.checkConfiguration(
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
        CONFIGURATIONHANDLER.checkConfiguration(configurationModel, new ArrayList<>(), true));
    Assertions.assertEquals(
        "./backend/test/resources/forge_tests", configurationModel.getModpackDir());
    Assertions.assertEquals("1.16.5", configurationModel.getMinecraftVersion());
    Assertions.assertEquals("Forge", configurationModel.getModLoader());
    Assertions.assertEquals("36.1.2", configurationModel.getModLoaderVersion());

    configurationModel.setModLoader("Fabric");
    configurationModel.setModLoaderVersion("0.14.6");
    Assertions.assertFalse(
        CONFIGURATIONHANDLER.checkConfiguration(configurationModel, new ArrayList<>(), true));
    Assertions.assertEquals("Fabric", configurationModel.getModLoader());
    Assertions.assertEquals("0.14.6", configurationModel.getModLoaderVersion());

    configurationModel.setModLoader("Quilt");
    configurationModel.setModLoaderVersion("0.16.1");
    Assertions.assertFalse(
        CONFIGURATIONHANDLER.checkConfiguration(configurationModel, new ArrayList<>(), true));
    Assertions.assertEquals("Quilt", configurationModel.getModLoader());
    Assertions.assertEquals("0.16.1", configurationModel.getModLoaderVersion());
  }

  @Test
  void checkIconAndPropertiesTest() {
    Assertions.assertTrue(CONFIGURATIONHANDLER.checkIconAndProperties(""));
    Assertions.assertFalse(CONFIGURATIONHANDLER.checkIconAndProperties("/some/path"));
    Assertions.assertTrue(CONFIGURATIONHANDLER.checkIconAndProperties("img/prosper.png"));
  }
}
