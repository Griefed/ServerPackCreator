package de.griefed.serverpackcreator;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import de.griefed.serverpackcreator.i18n.LocalizationManager;
import de.griefed.serverpackcreator.plugins.ApplicationPlugins;
import de.griefed.serverpackcreator.spring.serverpack.ServerPackModel;
import de.griefed.serverpackcreator.utilities.ConfigUtilities;
import de.griefed.serverpackcreator.utilities.common.Utilities;
import de.griefed.serverpackcreator.versionmeta.VersionMeta;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ServerPackHandlerTest {

  private final ServerPackHandler SERVERPACKHANDLER;
  private final ConfigurationHandler CONFIGURATIONHANDLER;

  ServerPackHandlerTest() throws IOException {
    try {
      FileUtils.copyFile(
          new File("backend/main/resources/serverpackcreator.properties"),
          new File("serverpackcreator.properties"));
    } catch (IOException e) {
      e.printStackTrace();
    }

    ApplicationProperties APPLICATIONPROPERTIES = new ApplicationProperties();
    LocalizationManager LOCALIZATIONMANAGER = new LocalizationManager(APPLICATIONPROPERTIES);
    ServerPackCreator SERVER_PACK_CREATOR = new ServerPackCreator(new String[] {"--setup"});
    SERVER_PACK_CREATOR.run(CommandlineParser.Mode.SETUP);
    VersionMeta VERSIONMETA =
        new VersionMeta(
            APPLICATIONPROPERTIES.MINECRAFT_VERSION_MANIFEST_LOCATION(),
            APPLICATIONPROPERTIES.FORGE_VERSION_MANIFEST_LOCATION(),
            APPLICATIONPROPERTIES.FABRIC_VERSION_MANIFEST_LOCATION(),
            APPLICATIONPROPERTIES.FABRIC_INSTALLER_VERSION_MANIFEST_LOCATION(),
            APPLICATIONPROPERTIES.QUILT_VERSION_MANIFEST_LOCATION(),
            APPLICATIONPROPERTIES.QUILT_INSTALLER_VERSION_MANIFEST_LOCATION());
    Utilities UTILITIES = new Utilities(LOCALIZATIONMANAGER, APPLICATIONPROPERTIES);
    ConfigUtilities CONFIGUTILITIES =
        new ConfigUtilities(LOCALIZATIONMANAGER, UTILITIES, APPLICATIONPROPERTIES, VERSIONMETA);
    this.CONFIGURATIONHANDLER =
        new ConfigurationHandler(
            LOCALIZATIONMANAGER, VERSIONMETA, APPLICATIONPROPERTIES, UTILITIES, CONFIGUTILITIES);
    ApplicationPlugins PLUGINMANAGER = new ApplicationPlugins();
    this.SERVERPACKHANDLER =
        new ServerPackHandler(
            LOCALIZATIONMANAGER, APPLICATIONPROPERTIES, VERSIONMETA, UTILITIES, PLUGINMANAGER);
  }

  @Test
  void runTest() {
    ConfigurationModel configurationModel = new ConfigurationModel();
    CONFIGURATIONHANDLER.checkConfiguration(
        new File("./backend/test/resources/testresources/serverpackcreator.conf"),
        configurationModel,
        true);
    Assertions.assertTrue(SERVERPACKHANDLER.run(configurationModel));
    Assertions.assertTrue(new File("server-packs/forge_tests/libraries").isDirectory());
    Assertions.assertTrue(new File("server-packs/forge_tests/config").isDirectory());
    Assertions.assertTrue(new File("server-packs/forge_tests/defaultconfigs").isDirectory());
    Assertions.assertTrue(new File("server-packs/forge_tests/mods").isDirectory());
    Assertions.assertTrue(new File("server-packs/forge_tests/scripts").isDirectory());
    Assertions.assertTrue(new File("server-packs/forge_tests/seeds").isDirectory());
    Assertions.assertTrue(
        new File("server-packs/forge_tests/minecraft_server.1.16.5.jar").exists());
    Assertions.assertTrue(new File("server-packs/forge_tests/forge.jar").exists());
    Assertions.assertTrue(new File("server-packs/forge_tests/server.properties").exists());
    Assertions.assertTrue(new File("server-packs/forge_tests/server-icon.png").exists());
    Assertions.assertTrue(new File("server-packs/forge_tests/start.bat").exists());
    Assertions.assertTrue(new File("server-packs/forge_tests/start.sh").exists());
    try {
      Files.copy(
          Paths.get("./backend/test/resources/testresources/server_pack.zip"),
          Paths.get("server-packs/forge_tests_server_pack.zip"),
          REPLACE_EXISTING);
    } catch (Exception ignored) {
    }

    try (InputStream inputStream =
        Files.newInputStream(Paths.get("server-packs/forge_tests/server.properties"))) {
      Properties properties = new Properties();
      properties.load(inputStream);
      Assertions.assertTrue(Boolean.parseBoolean(properties.getProperty("allow-flight")));
      Assertions.assertTrue(Boolean.parseBoolean(properties.getProperty("allow-nether")));
      Assertions.assertTrue(
          Boolean.parseBoolean(properties.getProperty("broadcast-console-to-ops")));
      Assertions.assertTrue(Boolean.parseBoolean(properties.getProperty("broadcast-rcon-to-ops")));
      Assertions.assertEquals("easy", properties.getProperty("difficulty"));
      Assertions.assertTrue(Boolean.parseBoolean(properties.getProperty("enable-command-block")));
      Assertions.assertFalse(Boolean.parseBoolean(properties.getProperty("enable-jmx-monitoring")));
      Assertions.assertFalse(Boolean.parseBoolean(properties.getProperty("enable-query")));
      Assertions.assertFalse(Boolean.parseBoolean(properties.getProperty("enable-rcon")));
      Assertions.assertTrue(Boolean.parseBoolean(properties.getProperty("enable-status")));
      Assertions.assertTrue(Boolean.parseBoolean(properties.getProperty("enforce-whitelist")));
      Assertions.assertEquals(
          100, Integer.parseInt(properties.getProperty("entity-broadcast-range-percentage")));
      Assertions.assertFalse(Boolean.parseBoolean(properties.getProperty("force-gamemode")));
      Assertions.assertEquals(
          2, Integer.parseInt(properties.getProperty("function-permission-level")));
      Assertions.assertEquals("survival", properties.getProperty("gamemode"));
      Assertions.assertTrue(Boolean.parseBoolean(properties.getProperty("generate-structures")));
      Assertions.assertFalse(Boolean.parseBoolean(properties.getProperty("hardcore")));
      Assertions.assertEquals("world", properties.getProperty("level-name"));
      Assertions.assertEquals(
          "skyblockbuilder:custom_skyblock", properties.getProperty("level-type"));
      Assertions.assertEquals(256, Integer.parseInt(properties.getProperty("max-build-height")));
      Assertions.assertEquals(10, Integer.parseInt(properties.getProperty("max-players")));
      Assertions.assertEquals(120000, Integer.parseInt(properties.getProperty("max-tick-time")));
      Assertions.assertEquals(29999984, Integer.parseInt(properties.getProperty("max-world-size")));
      Assertions.assertEquals("A Minecraft Server", properties.getProperty("motd"));
      Assertions.assertEquals(
          256, Integer.parseInt(properties.getProperty("network-compression-threshold")));
      Assertions.assertTrue(Boolean.parseBoolean(properties.getProperty("online-mode")));
      Assertions.assertEquals(4, Integer.parseInt(properties.getProperty("op-permission-level")));
      Assertions.assertEquals(0, Integer.parseInt(properties.getProperty("player-idle-timeout")));
      Assertions.assertFalse(
          Boolean.parseBoolean(properties.getProperty("prevent-proxy-connections")));
      Assertions.assertTrue(Boolean.parseBoolean(properties.getProperty("pvp")));
      Assertions.assertEquals(25565, Integer.parseInt(properties.getProperty("query.port")));
      Assertions.assertEquals(0, Integer.parseInt(properties.getProperty("rate-limit")));
      Assertions.assertEquals(25575, Integer.parseInt(properties.getProperty("rcon.port")));
      Assertions.assertEquals(25565, Integer.parseInt(properties.getProperty("server-port")));
      Assertions.assertFalse(Boolean.parseBoolean(properties.getProperty("snooper-enabled")));
      Assertions.assertTrue(Boolean.parseBoolean(properties.getProperty("spawn-animals")));
      Assertions.assertTrue(Boolean.parseBoolean(properties.getProperty("spawn-monsters")));
      Assertions.assertTrue(Boolean.parseBoolean(properties.getProperty("spawn-npcs")));
      Assertions.assertEquals(16, Integer.parseInt(properties.getProperty("spawn-protection")));
      Assertions.assertTrue(Boolean.parseBoolean(properties.getProperty("sync-chunk-writes")));
      Assertions.assertTrue(Boolean.parseBoolean(properties.getProperty("use-native-transport")));
      Assertions.assertEquals(10, Integer.parseInt(properties.getProperty("view-distance")));
      Assertions.assertFalse(Boolean.parseBoolean(properties.getProperty("white-list")));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    CONFIGURATIONHANDLER.checkConfiguration(
        new File("./backend/test/resources/testresources/serverpackcreator_quilt.conf"),
        configurationModel,
        true);
    Assertions.assertTrue(SERVERPACKHANDLER.run(configurationModel));

    CONFIGURATIONHANDLER.checkConfiguration(
        new File("./backend/test/resources/testresources/serverpackcreator_fabric.conf"),
        configurationModel,
        true);
    Assertions.assertTrue(SERVERPACKHANDLER.run(configurationModel));
  }

  @Test
  void runServerPackTest() {
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
    ServerPackModel serverPackModel = new ServerPackModel();
    try {
      FileUtils.copyDirectory(
          new File("./backend/test/resources/fabric_tests"),
          new File("./backend/test/resources/fabric_tests_copy"));
    } catch (Exception ignored) {
    }
    serverPackModel.setModpackDir("./backend/test/resources/fabric_tests_copy");
    serverPackModel.setClientMods(clientMods);
    serverPackModel.setCopyDirs(copyDirs);
    serverPackModel.setJavaPath("");
    serverPackModel.setIncludeServerInstallation(true);
    serverPackModel.setIncludeServerIcon(true);
    serverPackModel.setIncludeServerProperties(true);
    serverPackModel.setIncludeZipCreation(true);
    serverPackModel.setModLoader("Fabric");
    serverPackModel.setModLoaderVersion("0.14.6");
    serverPackModel.setMinecraftVersion("1.18.2");
    serverPackModel.setJavaArgs("");
    CONFIGURATIONHANDLER.checkConfiguration(serverPackModel, false);
    Assertions.assertNotNull(SERVERPACKHANDLER.run(serverPackModel));
    Assertions.assertTrue(new File("server-packs/fabric_tests_copy_server_pack.zip").isFile());

    try {
      FileUtils.copyDirectory(
          new File("./backend/test/resources/quilt_tests"),
          new File("./backend/test/resources/quilt_tests_copy"));
    } catch (Exception ignored) {
    }
    serverPackModel.setModpackDir("./backend/test/resources/quilt_tests_copy");
    serverPackModel.setClientMods(clientMods);
    serverPackModel.setCopyDirs(copyDirs);
    serverPackModel.setJavaPath("");
    serverPackModel.setIncludeServerInstallation(true);
    serverPackModel.setIncludeServerIcon(true);
    serverPackModel.setIncludeServerProperties(true);
    serverPackModel.setIncludeZipCreation(true);
    serverPackModel.setModLoader("Quilt");
    serverPackModel.setModLoaderVersion("0.16.1");
    serverPackModel.setMinecraftVersion("1.18.2");
    serverPackModel.setJavaArgs("");
    CONFIGURATIONHANDLER.checkConfiguration(serverPackModel, false);
    Assertions.assertNotNull(SERVERPACKHANDLER.run(serverPackModel));
    Assertions.assertTrue(new File("server-packs/quilt_tests_copy_server_pack.zip").isFile());

    serverPackModel = new ServerPackModel();
    try {
      FileUtils.copyDirectory(
          new File("./backend/test/resources/forge_tests"),
          new File("./backend/test/resources/forge_tests_copy"));
    } catch (Exception ignored) {
    }
    serverPackModel.setModpackDir("./backend/test/resources/forge_tests_copy");
    serverPackModel.setClientMods(clientMods);
    serverPackModel.setCopyDirs(copyDirs);
    serverPackModel.setJavaPath("");
    serverPackModel.setIncludeServerInstallation(true);
    serverPackModel.setIncludeServerIcon(true);
    serverPackModel.setIncludeServerProperties(true);
    serverPackModel.setIncludeZipCreation(true);
    serverPackModel.setModLoader("Forge");
    serverPackModel.setModLoaderVersion("14.23.5.2855");
    serverPackModel.setMinecraftVersion("1.12.2");
    serverPackModel.setJavaArgs("");
    CONFIGURATIONHANDLER.checkConfiguration(serverPackModel, false);
    Assertions.assertNotNull(SERVERPACKHANDLER.run(serverPackModel));
    Assertions.assertTrue(new File("server-packs/forge_tests_copy_server_pack.zip").isFile());
  }

  @Test
  void zipBuilderFabricTest() {
    try {
      Files.createDirectories(Paths.get("server-packs/fabric_tests"));
    } catch (Exception ignored) {
    }
    String minecraftVersion = "1.16.5";
    String modpackDir = "./backend/test/resources/fabric_tests";
    SERVERPACKHANDLER.zipBuilder(minecraftVersion, true, modpackDir);
    Assertions.assertTrue(new File(modpackDir + "_server_pack.zip").exists());
    try {
      Files.copy(
          Paths.get("./backend/test/resources/testresources/server_pack.zip"),
          Paths.get("./backend/test/resources/fabric_tests/server_pack.zip"),
          REPLACE_EXISTING);
    } catch (Exception ignored) {
    }
  }

  @Test
  void zipBuilderForgeTest() {
    try {
      Files.createDirectories(Paths.get("server-packs/forge_tests"));
    } catch (Exception ignored) {
    }
    String minecraftVersion = "1.16.5";
    String modpackDir = "./backend/test/resources/forge_tests";
    SERVERPACKHANDLER.zipBuilder(minecraftVersion, true, modpackDir);
    Assertions.assertTrue(new File(modpackDir + "_server_pack.zip").exists());
    try {
      Files.copy(
          Paths.get("./backend/test/resources/testresources/server_pack.zip"),
          Paths.get("./backend/test/resources/forge_tests/server_pack.zip"),
          REPLACE_EXISTING);
    } catch (Exception ignored) {
    }
  }

  @Test
  void zipBuilderQuiltTest() {
    try {
      Files.createDirectories(Paths.get("server-packs/quilt_tests"));
    } catch (Exception ignored) {
    }
    String minecraftVersion = "1.16.5";
    String modpackDir = "./backend/test/resources/quilt_tests";
    SERVERPACKHANDLER.zipBuilder(minecraftVersion, true, modpackDir);
    Assertions.assertTrue(new File(modpackDir + "_server_pack.zip").exists());
    try {
      Files.copy(
          Paths.get("./backend/test/resources/testresources/server_pack.zip"),
          Paths.get("./backend/test/resources/quilt_tests/server_pack.zip"),
          REPLACE_EXISTING);
    } catch (Exception ignored) {
    }
  }
}
