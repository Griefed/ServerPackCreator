package de.griefed.serverpackcreator;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import de.griefed.serverpackcreator.spring.serverpack.ServerPackModel;
import de.griefed.serverpackcreator.versionmeta.VersionMeta;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import javax.xml.parsers.ParserConfigurationException;
import net.lingala.zip4j.ZipFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

class ServerPackHandlerTest {

  private static final Logger LOG = LogManager.getLogger(ServerPackHandlerTest.class);

  static {
    try {
      FileUtils.copyFile(
          new File("backend/test/resources/custom_template.ps1"),
          new File("tests/server_files/custom_template.ps1"));
    } catch (IOException e) {
      LOG.error("Error copying file.", e);
    }
    try {
      FileUtils.copyFile(
          new File("backend/test/resources/custom_template.sh"),
          new File("tests/server_files/custom_template.sh"));
    } catch (IOException e) {
      LOG.error("Error copying file.", e);
    }
  }

  private final ConfigurationHandler configurationHandler;
  private final ServerPackHandler serverPackHandler;
  private final VersionMeta versionMeta;
  private final ApplicationProperties applicationProperties;
  String[] args = new String[]{"--setup", "backend/test/resources/serverpackcreator.properties"};

  ServerPackHandlerTest() throws IOException, ParserConfigurationException, SAXException {
    versionMeta = ServerPackCreator.getInstance(args).getVersionMeta();
    configurationHandler = ServerPackCreator.getInstance(args).getConfigurationHandler();
    serverPackHandler = ServerPackCreator.getInstance(args).getServerPackHandler();
    applicationProperties = ServerPackCreator.getInstance(args).getApplicationProperties();
  }

  @Test
  void runTest() throws IOException {
    ConfigurationModel configurationModel = new ConfigurationModel();
    configurationHandler.checkConfiguration(
        new File("backend/test/resources/testresources/spcconfs/serverpackcreator.conf"),
        configurationModel,
        true);
    Assertions.assertTrue(serverPackHandler.run(configurationModel));
    Assertions.assertTrue(new File(applicationProperties.serverPacksDirectory(),
        "forge_tests/libraries").isDirectory());
    Assertions.assertTrue(
        new File(applicationProperties.serverPacksDirectory(), "forge_tests/config").isDirectory());
    Assertions.assertTrue(new File(applicationProperties.serverPacksDirectory(),
        "forge_tests/defaultconfigs").isDirectory());
    Assertions.assertTrue(
        new File(applicationProperties.serverPacksDirectory(), "forge_tests/mods").isDirectory());
    Assertions.assertTrue(new File(applicationProperties.serverPacksDirectory(),
        "forge_tests/scripts").isDirectory());
    Assertions.assertTrue(
        new File(applicationProperties.serverPacksDirectory(), "forge_tests/seeds").isDirectory());
    Assertions.assertTrue(
        new File(applicationProperties.serverPacksDirectory(),
            "forge_tests/minecraft_server.1.16.5.jar").exists());
    Assertions.assertTrue(
        new File(applicationProperties.serverPacksDirectory(), "forge_tests/forge.jar").exists());
    Assertions.assertTrue(new File(applicationProperties.serverPacksDirectory(),
        "forge_tests/server.properties").exists());
    Assertions.assertTrue(new File(applicationProperties.serverPacksDirectory(),
        "forge_tests/server-icon.png").exists());
    Assertions.assertTrue(
        new File(applicationProperties.serverPacksDirectory(), "forge_tests/start.ps1").exists());
    Assertions.assertTrue(
        new File(applicationProperties.serverPacksDirectory(), "forge_tests/start.sh").exists());
    Assertions.assertTrue(
        new File(applicationProperties.serverPacksDirectory(), "forge_tests/exclude_me").exists());
    Assertions.assertTrue(new File(
        applicationProperties.serverPacksDirectory(),
        "forge_tests/exclude_me/exclude_me_some_more/ICANSEEMYHOUSEFROMHEEEEEEEEEEEEERE").exists());
    Assertions.assertTrue(new File(applicationProperties.serverPacksDirectory(),
        "forge_tests_server_pack.zip").exists());

    ZipFile zip = new ZipFile(
        new File(applicationProperties.serverPacksDirectory(), "forge_tests_server_pack.zip"));

    Assertions.assertTrue(IOUtils.toString(zip.getInputStream(zip.getFileHeader("start.sh")),
        StandardCharsets.UTF_8).contains("JAVA=\"java\""), "Default Java setting not present!");

    Assertions.assertTrue(IOUtils.toString(zip.getInputStream(zip.getFileHeader("start.ps1")),
            StandardCharsets.UTF_8).contains("$Java = \"java\""),
        "Default Java setting not present!");

    zip.close();

    String ps1 = FileUtils.readFileToString(
        new File(applicationProperties.serverPacksDirectory(), "forge_tests/start.ps1"),
        StandardCharsets.UTF_8);
    Assertions.assertTrue(ps1.contains("$JavaArgs = \"" + configurationModel.getJavaArgs()));
    Assertions.assertTrue(
        ps1.contains("$MinecraftVersion = \"" + configurationModel.getMinecraftVersion()));
    Assertions.assertTrue(ps1.contains("$ModLoader = \"" + configurationModel.getModLoader()));
    Assertions.assertTrue(
        ps1.contains("$ModLoaderVersion = \"" + configurationModel.getModLoaderVersion()));
    Assertions.assertTrue(
        ps1.contains("$Java = \"C:\\Program Files\\Java\\jdk1.8.0_301\\bin\\java.exe\""));
    Assertions.assertTrue(
        ps1.contains("$FabricInstallerVersion = \"" + versionMeta.fabric().releaseInstaller()));
    Assertions.assertTrue(ps1.contains(
        "$LegacyFabricInstallerVersion = \"" + versionMeta.legacyFabric().releaseInstaller()));
    Assertions.assertTrue(
        ps1.contains("$QuiltInstallerVersion = \"" + versionMeta.quilt().releaseInstaller()));
    Assertions.assertTrue(ps1.contains("# Start script generated by ServerPackCreator dev."));
    Assertions.assertTrue(ps1.contains("$MinecraftServerUrl = \"" + versionMeta.minecraft()
        .getServer(configurationModel.getMinecraftVersion()).get().url().get()));

    String shell = FileUtils.readFileToString(
        new File(applicationProperties.serverPacksDirectory(), "forge_tests/start.sh"),
        StandardCharsets.UTF_8);
    Assertions.assertTrue(shell.contains("ARGS=\"" + configurationModel.getJavaArgs()));
    Assertions.assertTrue(
        shell.contains("MINECRAFT_VERSION=\"" + configurationModel.getMinecraftVersion()));
    Assertions.assertTrue(shell.contains("MODLOADER=\"" + configurationModel.getModLoader()));
    Assertions.assertTrue(
        shell.contains("MODLOADER_VERSION=\"" + configurationModel.getModLoaderVersion()));
    Assertions.assertTrue(
        shell.contains("JAVA=\"C:\\Program Files\\Java\\jdk1.8.0_301\\bin\\java.exe\""));
    Assertions.assertTrue(
        shell.contains("FABRIC_INSTALLER_VERSION=\"" + versionMeta.fabric().releaseInstaller()));
    Assertions.assertTrue(shell.contains(
        "LEGACYFABRIC_INSTALLER_VERSION=\"" + versionMeta.legacyFabric().releaseInstaller()));
    Assertions.assertTrue(
        shell.contains("QUILT_INSTALLER_VERSION=\"" + versionMeta.quilt().releaseInstaller()));
    Assertions.assertTrue(shell.contains("# Start script generated by ServerPackCreator dev."));
    Assertions.assertTrue(shell.contains("MINECRAFT_SERVER_URL=\"" + versionMeta.minecraft()
        .getServer(configurationModel.getMinecraftVersion()).get().url().get()));

    Assertions.assertTrue(ps1.contains("Flynn = \"Now that's a big door\""),
        "Custom script settings not present!");
    Assertions.assertTrue(ps1.contains("SomeValue = \"something\""),
        "Custom script settings not present!");
    Assertions.assertTrue(ps1.contains("PraiseTheLamb = \"Kannema jajaja kannema\""),
        "Custom script settings not present!");
    Assertions.assertTrue(ps1.contains("AnotherValue = \"another\""),
        "Custom script settings not present!");
    Assertions.assertTrue(ps1.contains("Hello = \"Is it me you are looking foooooor\""),
        "Custom script settings not present!");

    Assertions.assertTrue(shell.contains("FLYNN=\"Now that's a big door\""),
        "Custom script settings not present!");
    Assertions.assertTrue(shell.contains("SOME_VALUE=\"something\""),
        "Custom script settings not present!");
    Assertions.assertTrue(shell.contains("PRAISE_THE_LAMB=\"Kannema jajaja kannema\""),
        "Custom script settings not present!");
    Assertions.assertTrue(shell.contains("ANOTHER_VALUE=\"another\""),
        "Custom script settings not present!");
    Assertions.assertTrue(shell.contains("HELLO=\"Is it me you are looking foooooor\""),
        "Custom script settings not present!");

    Assertions.assertFalse(
        new File(applicationProperties.serverPacksDirectory(),
            "forge_tests/exclude_me/I_dont_want_to_be_included.file").exists());
    Assertions.assertFalse(new File(
        applicationProperties.serverPacksDirectory(),
        "forge_tests/exclude_me/exclude_me_some_more/I_dont_want_to_be_included.file").exists());
    Assertions.assertFalse(new File(
        applicationProperties.serverPacksDirectory(),
        "forge_tests/exclude_me/exclude_me_some_more/some_more_dirs_to_exclude").exists());
    Assertions.assertFalse(new File(
        applicationProperties.serverPacksDirectory(),
        "forge_tests/exclude_me/exclude_me_some_more/some_more_dirs_to_exclude/I_dont_want_to_be_included.file").exists());
    Assertions.assertFalse(new File(
        applicationProperties.serverPacksDirectory(),
        "forge_tests/exclude_me/exclude_me_some_more/dont_include_me_either.ogg").exists());

    try {
      Files.copy(
          Paths.get("./backend/test/resources/testresources/server_pack.zip"),
          new File(applicationProperties.serverPacksDirectory(),
              "forge_tests_server_pack.zip").toPath(),
          REPLACE_EXISTING);
    } catch (Exception ignored) {
    }

    try (InputStream inputStream =
        Files.newInputStream(new File(applicationProperties.serverPacksDirectory(),
            "forge_tests/server.properties").toPath())) {
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
        new File("backend/test/resources/testresources/spcconfs/serverpackcreator_quilt.conf"),
        configurationModel,
        true);
    Assertions.assertTrue(serverPackHandler.run(configurationModel));

    configurationHandler.checkConfiguration(
        new File("backend/test/resources/testresources/spcconfs/serverpackcreator_fabric.conf"),
        configurationModel,
        true);
    Assertions.assertTrue(serverPackHandler.run(configurationModel));

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

    try {
      FileUtils.copyDirectory(
          new File("./backend/test/resources/legacyfabric_tests"),
          new File(applicationProperties.modpacksDirectory(), "legacyfabric_tests_copy"));
    } catch (Exception ignored) {
    }
    configurationModel.setModpackDir(
        applicationProperties.modpacksDirectory() + File.separator + "legacyfabric_tests_copy");
    configurationModel.setModLoader("LegacyFabric");
    configurationModel.setModLoaderVersion("0.13.3");
    configurationModel.setMinecraftVersion("1.12.2");
    configurationModel.setJavaArgs("");
    configurationHandler.checkConfiguration(configurationModel, false);
    serverPackHandler.run(configurationModel);
    Assertions.assertTrue(
        new File(applicationProperties.serverPacksDirectory(),
            "legacyfabric_tests_copy_server_pack.zip").isFile());
    Assertions.assertTrue(new File(applicationProperties.serverPacksDirectory(),
        "legacyfabric_tests_copy/server.jar").isFile());
    Assertions.assertTrue(
        new File(applicationProperties.serverPacksDirectory(),
            "legacyfabric_tests_copy/fabric-server-launch.jar").isFile());
    Assertions.assertTrue(new File(applicationProperties.serverPacksDirectory(),
        "legacyfabric_tests_copy/start.ps1").isFile());
    Assertions.assertTrue(new File(applicationProperties.serverPacksDirectory(),
        "legacyfabric_tests_copy/start.sh").isFile());

    try {
      FileUtils.copyDirectory(
          new File("./backend/test/resources/quilt_tests"),
          new File(applicationProperties.modpacksDirectory(), "quilt_tests_copy"));
    } catch (Exception ignored) {
    }
    configurationModel.setModpackDir(
        applicationProperties.modpacksDirectory() + File.separator + "quilt_tests_copy");
    configurationModel.setClientMods(clientMods);
    configurationModel.setCopyDirs(copyDirs);
    configurationModel.setIncludeServerInstallation(true);
    configurationModel.setIncludeServerIcon(true);
    configurationModel.setIncludeServerProperties(true);
    configurationModel.setIncludeZipCreation(true);
    configurationModel.setModLoader("Quilt");
    configurationModel.setModLoaderVersion("0.16.1");
    configurationModel.setMinecraftVersion("1.18.2");
    configurationModel.setJavaArgs("");
    configurationHandler.checkConfiguration(configurationModel, false);
    serverPackHandler.run(configurationModel);
    Assertions.assertTrue(new File(applicationProperties.serverPacksDirectory(),
        "quilt_tests_copy_server_pack.zip").isFile());
    Assertions.assertTrue(new File(applicationProperties.serverPacksDirectory(),
        "quilt_tests_copy/server.jar").isFile());
    Assertions.assertTrue(
        new File(applicationProperties.serverPacksDirectory(),
            "quilt_tests_copy/quilt-server-launch.jar").isFile());
    Assertions.assertTrue(new File(applicationProperties.serverPacksDirectory(),
        "quilt_tests_copy/start.ps1").isFile());
    Assertions.assertTrue(new File(applicationProperties.serverPacksDirectory(),
        "quilt_tests_copy/start.sh").isFile());

    try {
      FileUtils.copyDirectory(
          new File("./backend/test/resources/fabric_tests"),
          new File(applicationProperties.modpacksDirectory(), "fabric_tests_copy"));
    } catch (Exception ignored) {
    }
    configurationModel.setModpackDir(
        applicationProperties.modpacksDirectory() + File.separator + "fabric_tests_copy");
    configurationModel.setClientMods(clientMods);
    configurationModel.setCopyDirs(copyDirs);
    configurationModel.setIncludeServerInstallation(true);
    configurationModel.setIncludeServerIcon(true);
    configurationModel.setIncludeServerProperties(true);
    configurationModel.setIncludeZipCreation(true);
    configurationModel.setModLoader("Fabric");
    configurationModel.setModLoaderVersion("0.14.6");
    configurationModel.setMinecraftVersion("1.18.2");
    configurationModel.setJavaArgs("");
    configurationHandler.checkConfiguration(configurationModel, false);
    serverPackHandler.run(configurationModel);
    Assertions.assertTrue(new File(applicationProperties.serverPacksDirectory(),
        "fabric_tests_copy_server_pack.zip").isFile());
    Assertions.assertTrue(new File(applicationProperties.serverPacksDirectory(),
        "fabric_tests_copy/server.jar").isFile());
    Assertions.assertTrue(
        new File(applicationProperties.serverPacksDirectory(),
            "fabric_tests_copy/fabric-server-launch.jar").isFile());
    Assertions.assertTrue(
        new File(applicationProperties.serverPacksDirectory(),
            "fabric_tests_copy/fabric-server-launcher.jar").isFile());
    Assertions.assertTrue(new File(applicationProperties.serverPacksDirectory(),
        "fabric_tests_copy/start.ps1").isFile());
    Assertions.assertTrue(new File(applicationProperties.serverPacksDirectory(),
        "fabric_tests_copy/start.sh").isFile());
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
          new File(applicationProperties.modpacksDirectory(), "fabric_tests_copy"));
    } catch (Exception ignored) {
    }
    serverPackModel.setModpackDir(applicationProperties.modpacksDirectory() + File.separator + "fabric_tests_copy");
    serverPackModel.setClientMods(clientMods);
    serverPackModel.setCopyDirs(copyDirs);
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
    Assertions.assertTrue(new File(applicationProperties.serverPacksDirectory(),
        "fabric_tests_copy_server_pack.zip").isFile());
  }
}
