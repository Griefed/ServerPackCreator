package de.griefed.serverpackcreator;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import de.griefed.serverpackcreator.spring.serverpack.ServerPackModel;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ServerPackHandlerTest {

  private final ConfigurationHandler configurationHandler;
  private final ServerPackHandler serverPackHandler;

  ServerPackHandlerTest() throws IOException {
    try {
      FileUtils.copyFile(
          new File("backend/main/resources/serverpackcreator.properties"),
          new File("serverpackcreator.properties"));
    } catch (IOException e) {
      e.printStackTrace();
    }
    ServerPackCreator serverPackCreator = new ServerPackCreator(new String[]{"--setup"});
    serverPackCreator.run();
    configurationHandler = serverPackCreator.getConfigurationHandler();
    serverPackHandler = serverPackCreator.getServerPackHandler();
  }

  @Test
  void runTest() {
    ConfigurationModel configurationModel = new ConfigurationModel();
    configurationHandler.checkConfiguration(
        new File("./backend/test/resources/testresources/serverpackcreator.conf"),
        configurationModel,
        true);
    Assertions.assertTrue(serverPackHandler.run(configurationModel));
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
    Assertions.assertTrue(new File("server-packs/forge_tests/start.ps1").exists());
    Assertions.assertTrue(new File("server-packs/forge_tests/start.sh").exists());
    Assertions.assertTrue(new File("server-packs/forge_tests/exclude_me").exists());
    Assertions.assertTrue(new File(
        "server-packs/forge_tests/exclude_me/exclude_me_some_more/ICANSEEMYHOUSEFROMHEEEEEEEEEEEEERE").exists());

    Assertions.assertFalse(
        new File("server-packs/forge_tests/exclude_me/I_dont_want_to_be_included.file").exists());
    Assertions.assertFalse(new File(
        "server-packs/forge_tests/exclude_me/exclude_me_some_more/I_dont_want_to_be_included.file").exists());
    Assertions.assertFalse(new File(
        "server-packs/forge_tests/exclude_me/exclude_me_some_more/some_more_dirs_to_exclude").exists());
    Assertions.assertFalse(new File(
        "server-packs/forge_tests/exclude_me/exclude_me_some_more/some_more_dirs_to_exclude/I_dont_want_to_be_included.file").exists());
    Assertions.assertFalse(new File(
        "server-packs/forge_tests/exclude_me/exclude_me_some_more/dont_include_me_either.ogg").exists());

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

    configurationHandler.checkConfiguration(
        new File("./backend/test/resources/testresources/serverpackcreator_quilt.conf"),
        configurationModel,
        true);
    Assertions.assertTrue(serverPackHandler.run(configurationModel));

    configurationHandler.checkConfiguration(
        new File("./backend/test/resources/testresources/serverpackcreator_fabric.conf"),
        configurationModel,
        true);
    Assertions.assertTrue(serverPackHandler.run(configurationModel));
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
    configurationHandler.checkConfiguration(serverPackModel, false);
    Assertions.assertNotNull(serverPackHandler.run(serverPackModel));
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
    configurationHandler.checkConfiguration(serverPackModel, false);
    Assertions.assertNotNull(serverPackHandler.run(serverPackModel));
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
    configurationHandler.checkConfiguration(serverPackModel, false);
    Assertions.assertNotNull(serverPackHandler.run(serverPackModel));
    Assertions.assertTrue(new File("server-packs/forge_tests_copy_server_pack.zip").isFile());
  }

  @Test
  void zipBuilderTest() {
    Path path = Paths.get("./backend/test/resources/testresources/server_pack.zip");

    try {
      Files.createDirectories(Paths.get("server-packs/fabric_tests"));
    } catch (Exception ignored) {
    }

    String minecraftVersion = "1.16.5";
    String modpackDir = "./backend/test/resources/fabric_tests";
    serverPackHandler.zipBuilder(minecraftVersion, true, modpackDir, "Forge", "36.2.25");
    Assertions.assertTrue(new File(modpackDir + "_server_pack.zip").exists());
    try {
      Files.copy(
          path,
          Paths.get("./backend/test/resources/fabric_tests/server_pack.zip"),
          REPLACE_EXISTING);
    } catch (Exception ignored) {
    }

    try {
      Files.createDirectories(Paths.get("server-packs/forge_tests"));
    } catch (Exception ignored) {
    }
    minecraftVersion = "1.16.5";
    modpackDir = "./backend/test/resources/forge_tests";
    serverPackHandler.zipBuilder(minecraftVersion, true, modpackDir, "Forge", "36.2.25");
    Assertions.assertTrue(new File(modpackDir + "_server_pack.zip").exists());
    try {
      Files.copy(
          path,
          Paths.get("./backend/test/resources/forge_tests/server_pack.zip"),
          REPLACE_EXISTING);
    } catch (Exception ignored) {
    }

    try {
      Files.createDirectories(Paths.get("server-packs/quilt_tests"));
    } catch (Exception ignored) {
    }
    minecraftVersion = "1.16.5";
    modpackDir = "./backend/test/resources/quilt_tests";
    serverPackHandler.zipBuilder(minecraftVersion, true, modpackDir, "Forge", "36.2.25");
    Assertions.assertTrue(new File(modpackDir + "_server_pack.zip").exists());
    try {
      Files.copy(
          path,
          Paths.get("./backend/test/resources/quilt_tests/server_pack.zip"),
          REPLACE_EXISTING);
    } catch (Exception ignored) {
    }
  }
}
