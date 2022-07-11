package de.griefed.serverpackcreator.utilities;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.griefed.serverpackcreator.ApplicationProperties;
import de.griefed.serverpackcreator.ConfigurationModel;
import de.griefed.serverpackcreator.i18n.I18n;
import de.griefed.serverpackcreator.utilities.common.Utilities;
import de.griefed.serverpackcreator.versionmeta.VersionMeta;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ConfigUtilitiesTest {

  private final ConfigUtilities CONFIGUTILITIES;

  ConfigUtilitiesTest() throws IOException {
    I18n I18N = new I18n();
    ApplicationProperties APPLICATIONPROPERTIES = new ApplicationProperties();
    VersionMeta VERSIONMETA =
        new VersionMeta(
            APPLICATIONPROPERTIES.MINECRAFT_VERSION_MANIFEST_LOCATION(),
            APPLICATIONPROPERTIES.FORGE_VERSION_MANIFEST_LOCATION(),
            APPLICATIONPROPERTIES.FABRIC_VERSION_MANIFEST_LOCATION(),
            APPLICATIONPROPERTIES.FABRIC_INSTALLER_VERSION_MANIFEST_LOCATION(),
            APPLICATIONPROPERTIES.QUILT_VERSION_MANIFEST_LOCATION(),
            APPLICATIONPROPERTIES.QUILT_INSTALLER_VERSION_MANIFEST_LOCATION());
    this.CONFIGUTILITIES =
        new ConfigUtilities(
            I18N,
            new Utilities(I18N, APPLICATIONPROPERTIES),
            APPLICATIONPROPERTIES,
            VERSIONMETA);
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Test
  void writeConfigToFileTestFabric() {
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

    String javaPath;
    String autoJavaPath = System.getProperty("java.home").replace("\\", "/") + "/bin/java";

    if (autoJavaPath.startsWith("C:")) {
      autoJavaPath = String.format("%s.exe", autoJavaPath);
    }
    if (new File("/usr/bin/java").exists()) {
      javaPath = "/usr/bin/java";
    } else {
      javaPath = autoJavaPath;
    }

    String javaArgs = "tf3g4jz89agz843fag8z49a3zg8ap3jg8zap9vagv3z8j";

    Assertions.assertTrue(
        CONFIGUTILITIES.writeConfigToFile(
            "./backend/test/resources/fabric_tests",
            clientMods,
            copyDirs,
            "",
            "",
            true,
            javaPath,
            "1.16.5",
            "Fabric",
            "0.11.3",
            true,
            true,
            true,
            javaArgs,
            "",
            new File("./serverpackcreatorfabric.conf")));
    Assertions.assertTrue(new File("./serverpackcreatorfabric.conf").exists());
    new File("./serverpackcreatorfabric.conf").delete();
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Test
  void writeConfigToFileTestForge() {
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

    String javaPath;
    String autoJavaPath = System.getProperty("java.home").replace("\\", "/") + "/bin/java";

    if (autoJavaPath.startsWith("C:")) {
      autoJavaPath = String.format("%s.exe", autoJavaPath);
    }
    if (new File("/usr/bin/java").exists()) {
      javaPath = "/usr/bin/java";
    } else {
      javaPath = autoJavaPath;
    }

    String javaArgs = "tf3g4jz89agz843fag8z49a3zg8ap3jg8zap9vagv3z8j";

    Assertions.assertTrue(
        CONFIGUTILITIES.writeConfigToFile(
            "./backend/test/resources/forge_tests",
            clientMods,
            copyDirs,
            "",
            "",
            true,
            javaPath,
            "1.16.5",
            "Forge",
            "36.1.2",
            true,
            true,
            true,
            javaArgs,
            "",
            new File("./serverpackcreatorforge.conf")));
    Assertions.assertTrue(new File("./serverpackcreatorforge.conf").exists());
    new File("./serverpackcreatorforge.conf").delete();
  }

  @Test
  void writeConfigToFileModelTest() {
    ConfigurationModel configurationModel = new ConfigurationModel();
    configurationModel.setModpackDir("backend/test/resources/forge_tests");
    configurationModel.setClientMods(
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
                "WorldNameRandomizer")));
    configurationModel.setCopyDirs(
        new ArrayList<>(Arrays.asList("config", "mods", "scripts", "seeds", "defaultconfigs")));
    configurationModel.setIncludeServerInstallation(true);
    configurationModel.setIncludeServerIcon(true);
    configurationModel.setIncludeServerProperties(true);
    configurationModel.setIncludeZipCreation(true);
    configurationModel.setJavaPath("/usr/bin/java");
    configurationModel.setMinecraftVersion("1.16.5");
    configurationModel.setModLoader("Forge");
    configurationModel.setModLoaderVersion("36.1.2");
    configurationModel.setJavaArgs("tf3g4jz89agz843fag8z49a3zg8ap3jg8zap9vagv3z8j");
    Assertions.assertTrue(
        CONFIGUTILITIES.writeConfigToFile(configurationModel, new File("somefile.conf")));
    Assertions.assertTrue(new File("somefile.conf").exists());
  }

  @Test
  void setModLoaderCaseTestForge() {
    Assertions.assertEquals("Forge", CONFIGUTILITIES.getModLoaderCase("fOrGe"));
  }

  @Test
  void setModLoaderCaseTestFabric() {
    Assertions.assertEquals("Fabric", CONFIGUTILITIES.getModLoaderCase("fAbRiC"));
  }

  @Test
  void setModLoaderCaseTestForgeCorrected() {
    Assertions.assertEquals("Forge", CONFIGUTILITIES.getModLoaderCase("eeeeefOrGeeeeee"));
  }

  @Test
  void setModLoaderCaseTestFabricCorrected() {
    Assertions.assertEquals("Fabric", CONFIGUTILITIES.getModLoaderCase("hufwhafasfabricfagrsg"));
  }

  @Test
  void printConfigTest() {
    String modpackDir = "backend/test/resources/forge_tests";
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
    boolean includeServerInstallation = true;
    String javaPath = "/usr/bin/java";
    String minecraftVersion = "1.16.5";
    String modLoader = "Forge";
    String modLoaderVersion = "36.1.2";
    String javaArgs = "tf3g4jz89agz843fag8z49a3zg8ap3jg8zap9vagv3z8j";
    boolean includeServerIcon = true;
    boolean includeServerProperties = true;
    boolean includeZipCreation = true;

    CONFIGUTILITIES.printConfigurationModel(
        modpackDir,
        clientMods,
        copyDirs,
        includeServerInstallation,
        javaPath,
        minecraftVersion,
        modLoader,
        modLoaderVersion,
        includeServerIcon,
        includeServerProperties,
        includeZipCreation,
        javaArgs,
        "",
        "",
        "");
  }

  @Test
  void printConfigModelTest() {
    ConfigurationModel configurationModel = new ConfigurationModel();
    configurationModel.setModpackDir("backend/test/resources/forge_tests");
    configurationModel.setClientMods(
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
                "WorldNameRandomizer")));
    configurationModel.setCopyDirs(
        new ArrayList<>(Arrays.asList("config", "mods", "scripts", "seeds", "defaultconfigs")));
    configurationModel.setIncludeServerInstallation(true);
    configurationModel.setIncludeServerIcon(true);
    configurationModel.setIncludeServerProperties(true);
    configurationModel.setIncludeZipCreation(true);
    configurationModel.setJavaPath("/usr/bin/java");
    configurationModel.setMinecraftVersion("1.16.5");
    configurationModel.setModLoader("Forge");
    configurationModel.setModLoaderVersion("36.1.2");
    configurationModel.setJavaArgs("tf3g4jz89agz843fag8z49a3zg8ap3jg8zap9vagv3z8j");
    CONFIGUTILITIES.printConfigurationModel(configurationModel);
  }

  @Test
  void updateConfigModelFromCurseManifestTest() throws IOException {
    ConfigurationModel configurationModel = new ConfigurationModel();
    CONFIGUTILITIES.updateConfigModelFromCurseManifest(
        configurationModel, new File("backend/test/resources/testresources/manifest.json"));
    Assertions.assertEquals(configurationModel.getMinecraftVersion(), "1.16.5");
    Assertions.assertEquals(configurationModel.getModLoader(), "Forge");
    Assertions.assertEquals(configurationModel.getModLoaderVersion(), "36.0.1");
  }

  @Test
  void updateConfigModelFromMinecraftInstanceTest() throws IOException {
    ConfigurationModel configurationModel = new ConfigurationModel();
    CONFIGUTILITIES.updateConfigModelFromMinecraftInstance(
        configurationModel,
        new File("backend/test/resources/testresources/minecraftinstance.json"));
    Assertions.assertEquals(configurationModel.getMinecraftVersion(), "1.16.5");
    Assertions.assertEquals(configurationModel.getModLoader(), "Forge");
    Assertions.assertEquals(configurationModel.getModLoaderVersion(), "36.2.4");
  }

  @Test
  void updateConfigModelFromConfigJsonTest() throws IOException {
    ConfigurationModel configurationModel = new ConfigurationModel();
    CONFIGUTILITIES.updateConfigModelFromConfigJson(
        configurationModel, new File("backend/test/resources/testresources/config.json"));
    Assertions.assertEquals(configurationModel.getMinecraftVersion(), "1.18.1");
    Assertions.assertEquals(configurationModel.getModLoader(), "Fabric");
    Assertions.assertEquals(configurationModel.getModLoaderVersion(), "0.12.12");
  }

  @Test
  void updateConfigModelFromMMCPackTest() throws IOException {
    ConfigurationModel configurationModel = new ConfigurationModel();
    CONFIGUTILITIES.updateConfigModelFromMMCPack(
        configurationModel, new File("backend/test/resources/testresources/mmc-pack.json"));
    Assertions.assertEquals(configurationModel.getMinecraftVersion(), "1.18.1");
    Assertions.assertEquals(configurationModel.getModLoader(), "Fabric");
    Assertions.assertEquals(configurationModel.getModLoaderVersion(), "0.12.12");
  }

  @Test
  void updateDestinationFromInstanceCfgTest() throws IOException {
    String name =
        CONFIGUTILITIES.updateDestinationFromInstanceCfg(
            new File("backend/test/resources/testresources/instance.cfg"));
    Assertions.assertEquals(name, "Better Minecraft [FABRIC] - 1.18.1");
  }

  @Test
  void suggestCopyDirsTest() {
    List<String> dirs = CONFIGUTILITIES.suggestCopyDirs("backend/test/resources/fabric_tests");
    Assertions.assertTrue(dirs.contains("config"));
    Assertions.assertTrue(dirs.contains("defaultconfigs"));
    Assertions.assertTrue(dirs.contains("mods"));
    Assertions.assertTrue(dirs.contains("scripts"));
    Assertions.assertTrue(dirs.contains("seeds"));
    Assertions.assertFalse(dirs.contains("server_pack"));
  }

  @Test
  void checkCurseForgeJsonForFabricTest() throws IOException {
    Assertions.assertTrue(
        CONFIGUTILITIES.checkCurseForgeJsonForFabric(
            getJson(new File("backend/test/resources/testresources/fabric_manifest.json"))));
  }

  @Test
  void directoriesInModpackZipTest() throws IOException {
    List<String> entries =
        CONFIGUTILITIES.directoriesInModpackZip(
            Paths.get("backend/test/resources/testresources/Survive_Create_Prosper_4_invalid.zip"));
    Assertions.assertEquals(1, entries.size());
    Assertions.assertTrue(entries.contains("overrides"));
    entries =
        CONFIGUTILITIES.directoriesInModpackZip(
            Paths.get("backend/test/resources/testresources/Survive_Create_Prosper_4_valid.zip"));
    Assertions.assertEquals(2, entries.size());
    Assertions.assertTrue(entries.contains("mods"));
    Assertions.assertTrue(entries.contains("config"));
  }

  private ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
    return objectMapper;
  }

  private JsonNode getJson(File jsonFile) throws IOException {
    return getObjectMapper()
        .readTree(Files.readAllBytes(Paths.get(jsonFile.getAbsolutePath().replace("\\", "/"))));
  }
}
