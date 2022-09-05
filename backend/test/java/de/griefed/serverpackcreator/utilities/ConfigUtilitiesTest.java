package de.griefed.serverpackcreator.utilities;

import de.griefed.serverpackcreator.ConfigurationModel;
import de.griefed.serverpackcreator.ServerPackCreator;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import net.lingala.zip4j.ZipFile;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ConfigUtilitiesTest {
  String[] args = new String[]{"--setup", "backend/test/resources/serverpackcreator.properties"};
  ConfigUtilities configUtilities;

  ConfigUtilitiesTest() {
    configUtilities = ServerPackCreator.getInstance(args).getConfigUtilities();
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

    Assertions.assertNotNull(
        new ConfigurationModel(
            clientMods,
            copyDirs,
            "./backend/test/resources/fabric_tests",
            javaPath,
            "1.16.5",
            "Fabric",
            "0.11.3",
            javaArgs,
            "",
            "",
            "",
            true,
            true,
            true,
            true,
            new HashMap<>(),
            new HashMap<>()
        ).save(new File("./serverpackcreatorfabric.conf")));
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

    Assertions.assertNotNull(
        new ConfigurationModel(
            clientMods,
            copyDirs,
            "./backend/test/resources/forge_tests",
            javaPath,
            "1.16.5",
            "Forge",
            "36.1.2",
            javaArgs,
            "",
            "",
            "",
            true,
            true,
            true,
            true,
            new HashMap<>(),
            new HashMap<>()
        ).save(new File("./serverpackcreatorforge.conf")));
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
    Assertions.assertNotNull(configurationModel.save(new File("somefile.conf")));
    Assertions.assertTrue(new File("somefile.conf").exists());
  }

  @Test
  void setModLoaderCaseTestForge() {
    Assertions.assertEquals("Forge", configUtilities.getModLoaderCase("fOrGe"));
  }

  @Test
  void setModLoaderCaseTestFabric() {
    Assertions.assertEquals("Fabric", configUtilities.getModLoaderCase("fAbRiC"));
  }

  @Test
  void setModLoaderCaseTestForgeCorrected() {
    Assertions.assertEquals("Forge", configUtilities.getModLoaderCase("eeeeefOrGeeeeee"));
  }

  @Test
  void setModLoaderCaseTestFabricCorrected() {
    Assertions.assertEquals("Fabric", configUtilities.getModLoaderCase("hufwhafasfabricfagrsg"));
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

    configUtilities.printConfigurationModel(
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
    configUtilities.printConfigurationModel(configurationModel);
  }

  @Test
  void updateConfigModelFromModrinthManifestTest() throws IOException {
    ConfigurationModel configurationModel = new ConfigurationModel();
    configUtilities.updateConfigModelFromModrinthManifest(
        configurationModel,
        new File("backend/test/resources/testresources/modrinth/forge_modrinth.index.json"));
    Assertions.assertEquals(configurationModel.getMinecraftVersion(), "1.18.2");
    Assertions.assertEquals(configurationModel.getModLoader(), "Forge");
    Assertions.assertEquals(configurationModel.getModLoaderVersion(), "40.1.48");

    configUtilities.updateConfigModelFromModrinthManifest(
        configurationModel,
        new File("backend/test/resources/testresources/modrinth/fabric_modrinth.index.json"));
    Assertions.assertEquals(configurationModel.getMinecraftVersion(), "1.19");
    Assertions.assertEquals(configurationModel.getModLoader(), "Fabric");
    Assertions.assertEquals(configurationModel.getModLoaderVersion(), "0.14.8");

    configUtilities.updateConfigModelFromModrinthManifest(
        configurationModel,
        new File("backend/test/resources/testresources/modrinth/quilt_modrinth.index.json"));
    Assertions.assertEquals(configurationModel.getMinecraftVersion(), "1.19");
    Assertions.assertEquals(configurationModel.getModLoader(), "Quilt");
    Assertions.assertEquals(configurationModel.getModLoaderVersion(), "0.17.0");
  }

  @Test
  void updateConfigModelFromCurseManifestTest() throws IOException {
    ConfigurationModel configurationModel = new ConfigurationModel();
    configUtilities.updateConfigModelFromCurseManifest(
        configurationModel,
        new File("backend/test/resources/testresources/curseforge/forge_manifest.json"));
    Assertions.assertEquals(configurationModel.getMinecraftVersion(), "1.16.5");
    Assertions.assertEquals(configurationModel.getModLoader(), "Forge");
    Assertions.assertEquals(configurationModel.getModLoaderVersion(), "36.0.1");

    configUtilities.updateConfigModelFromCurseManifest(
        configurationModel,
        new File("backend/test/resources/testresources/curseforge/fabric_manifest.json"));
    Assertions.assertEquals(configurationModel.getMinecraftVersion(), "1.18.2");
    Assertions.assertEquals(configurationModel.getModLoader(), "Fabric");
    Assertions.assertEquals(configurationModel.getModLoaderVersion(), "0.13.3");
  }

  @Test
  void updateConfigModelFromMinecraftInstanceTest() throws IOException {
    ConfigurationModel configurationModel = new ConfigurationModel();
    configUtilities.updateConfigModelFromMinecraftInstance(
        configurationModel,
        new File("backend/test/resources/testresources/curseforge/forge_minecraftinstance.json"));
    Assertions.assertEquals(configurationModel.getMinecraftVersion(), "1.16.5");
    Assertions.assertEquals(configurationModel.getModLoader(), "Forge");
    Assertions.assertEquals(configurationModel.getModLoaderVersion(), "36.2.4");

    configUtilities.updateConfigModelFromMinecraftInstance(
        configurationModel,
        new File("backend/test/resources/testresources/curseforge/fabric_minecraftinstance.json"));
    Assertions.assertEquals(configurationModel.getMinecraftVersion(), "1.18.2");
    Assertions.assertEquals(configurationModel.getModLoader(), "Fabric");
    Assertions.assertEquals(configurationModel.getModLoaderVersion(), "0.13.3");
  }

  @Test
  void updateConfigModelFromConfigJsonTest() throws IOException {
    ConfigurationModel configurationModel = new ConfigurationModel();
    configUtilities.updateConfigModelFromConfigJson(
        configurationModel,
        new File("backend/test/resources/testresources/gdlauncher/fabric_config.json"));
    Assertions.assertEquals(configurationModel.getMinecraftVersion(), "1.18.2");
    Assertions.assertEquals(configurationModel.getModLoader(), "Fabric");
    Assertions.assertEquals(configurationModel.getModLoaderVersion(), "0.14.8");

    configUtilities.updateConfigModelFromConfigJson(
        configurationModel,
        new File("backend/test/resources/testresources/gdlauncher/forge_config.json"));
    Assertions.assertEquals(configurationModel.getMinecraftVersion(), "1.18.2");
    Assertions.assertEquals(configurationModel.getModLoader(), "Forge");
    Assertions.assertEquals(configurationModel.getModLoaderVersion(), "40.1.52");
  }

  @Test
  void updateConfigModelFromMMCPackTest() throws IOException {
    ConfigurationModel configurationModel = new ConfigurationModel();
    configUtilities.updateConfigModelFromMMCPack(
        configurationModel,
        new File("backend/test/resources/testresources/multimc/fabric_mmc-pack.json"));
    Assertions.assertEquals(configurationModel.getMinecraftVersion(), "1.18.1");
    Assertions.assertEquals(configurationModel.getModLoader(), "Fabric");
    Assertions.assertEquals(configurationModel.getModLoaderVersion(), "0.12.12");

    configUtilities.updateConfigModelFromMMCPack(
        configurationModel,
        new File("backend/test/resources/testresources/multimc/forge_mmc-pack.json"));
    Assertions.assertEquals(configurationModel.getMinecraftVersion(), "1.16.5");
    Assertions.assertEquals(configurationModel.getModLoader(), "Forge");
    Assertions.assertEquals(configurationModel.getModLoaderVersion(), "36.2.23");

    configUtilities.updateConfigModelFromMMCPack(
        configurationModel,
        new File("backend/test/resources/testresources/multimc/quilt_mmc-pack.json"));
    Assertions.assertEquals(configurationModel.getMinecraftVersion(), "1.19");
    Assertions.assertEquals(configurationModel.getModLoader(), "Quilt");
    Assertions.assertEquals(configurationModel.getModLoaderVersion(), "0.17.0");
  }

  @Test
  void updateDestinationFromInstanceCfgTest() throws IOException {
    Assertions.assertEquals(
        configUtilities.updateDestinationFromInstanceCfg(
            new File("backend/test/resources/testresources/multimc/better_mc_instance.cfg")),
        "Better Minecraft [FABRIC] - 1.18.1");

    Assertions.assertEquals(
        configUtilities.updateDestinationFromInstanceCfg(
            new File("backend/test/resources/testresources/multimc/all_the_mods_instance.cfg")),
        "All the Mods 6 - ATM6 - 1.16.5");
  }

  @Test
  void suggestCopyDirsTest() {
    List<String> dirs = configUtilities.suggestCopyDirs("backend/test/resources/fabric_tests");
    Assertions.assertTrue(dirs.contains("config"));
    Assertions.assertTrue(dirs.contains("defaultconfigs"));
    Assertions.assertTrue(dirs.contains("mods"));
    Assertions.assertTrue(dirs.contains("scripts"));
    Assertions.assertTrue(dirs.contains("seeds"));
    Assertions.assertFalse(dirs.contains("server_pack"));
  }

  @Test
  void directoriesInModpackZipTest() throws IOException {
    List<String> entries =
        configUtilities.getDirectoriesInModpackZipBaseDirectory(
            new ZipFile(
                "backend/test/resources/testresources/Survive_Create_Prosper_4_invalid.zip"));
    Assertions.assertEquals(1, entries.size());
    Assertions.assertTrue(entries.contains("overrides/"));
    entries =
        configUtilities.getDirectoriesInModpackZipBaseDirectory(
            new ZipFile("backend/test/resources/testresources/Survive_Create_Prosper_4_valid.zip"));
    Assertions.assertTrue(entries.size() > 1);
    Assertions.assertTrue(entries.contains("mods/"));
    Assertions.assertTrue(entries.contains("config/"));
  }

  @Test
  void filesAndDirsInZipTest() throws IOException {
    Assertions.assertTrue(
        configUtilities
            .getFilesInModpackZip(
                new ZipFile(
                    "backend/test/resources/testresources/Survive_Create_Prosper_4_invalid.zip"))
            .size()
            > 0);
    Assertions.assertTrue(
        configUtilities
            .getFilesInModpackZip(
                new ZipFile(
                    "backend/test/resources/testresources/Survive_Create_Prosper_4_valid.zip"))
            .size()
            > 0);

    Assertions.assertTrue(
        configUtilities
            .getDirectoriesInModpackZip(
                new ZipFile(
                    "backend/test/resources/testresources/Survive_Create_Prosper_4_invalid.zip"))
            .size()
            > 0);
    Assertions.assertTrue(
        configUtilities
            .getDirectoriesInModpackZip(
                new ZipFile(
                    "backend/test/resources/testresources/Survive_Create_Prosper_4_valid.zip"))
            .size()
            > 0);

    Assertions.assertTrue(
        configUtilities
            .getAllFilesAndDirectoriesInModpackZip(
                new ZipFile(
                    "backend/test/resources/testresources/Survive_Create_Prosper_4_invalid.zip"))
            .size()
            > 0);
    Assertions.assertTrue(
        configUtilities
            .getAllFilesAndDirectoriesInModpackZip(
                new ZipFile(
                    "backend/test/resources/testresources/Survive_Create_Prosper_4_valid.zip"))
            .size()
            > 0);
  }
}
