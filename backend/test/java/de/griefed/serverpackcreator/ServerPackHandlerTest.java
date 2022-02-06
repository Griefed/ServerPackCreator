package de.griefed.serverpackcreator;

import de.griefed.serverpackcreator.curseforge.CurseCreateModpack;
import de.griefed.serverpackcreator.i18n.LocalizationManager;
import de.griefed.serverpackcreator.spring.models.ServerPack;
import de.griefed.serverpackcreator.utilities.*;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

class ServerPackHandlerTest {

    private final ServerPackHandler SERVERPACKHANDLER;
    private final DefaultFiles DEFAULTFILES;
    private final CurseCreateModpack CURSECREATEMODPACK;
    private final LocalizationManager LOCALIZATIONMANAGER;
    private final ConfigurationHandler CONFIGURATIONHANDLER;
    private final VersionLister VERSIONLISTER;
    private final BooleanUtilities BOOLEANUTILITIES;
    private final ListUtilities LISTUTILITIES;
    private final StringUtilities STRINGUTILITIES;
    private final ConfigUtilities CONFIGUTILITIES;
    private final SystemUtilities SYSTEMUTILITIES;
    private final ApplicationProperties APPLICATIONPROPERTIES;
    private final ApplicationPlugins PLUGINMANAGER;

    ServerPackHandlerTest() {
        try {
            FileUtils.copyFile(new File("backend/main/resources/serverpackcreator.properties"),new File("serverpackcreator.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.APPLICATIONPROPERTIES = new ApplicationProperties();

        LOCALIZATIONMANAGER = new LocalizationManager(APPLICATIONPROPERTIES);
        LOCALIZATIONMANAGER.initialize();
        DEFAULTFILES = new DefaultFiles(LOCALIZATIONMANAGER, APPLICATIONPROPERTIES);
        DEFAULTFILES.filesSetup();
        VERSIONLISTER = new VersionLister(APPLICATIONPROPERTIES);
        LISTUTILITIES = new ListUtilities();
        STRINGUTILITIES = new StringUtilities();
        SYSTEMUTILITIES = new SystemUtilities();
        BOOLEANUTILITIES = new BooleanUtilities(LOCALIZATIONMANAGER, APPLICATIONPROPERTIES);
        CONFIGUTILITIES = new ConfigUtilities(LOCALIZATIONMANAGER, BOOLEANUTILITIES, LISTUTILITIES, APPLICATIONPROPERTIES, STRINGUTILITIES, VERSIONLISTER);
        CURSECREATEMODPACK = new CurseCreateModpack(LOCALIZATIONMANAGER, APPLICATIONPROPERTIES, VERSIONLISTER, BOOLEANUTILITIES, LISTUTILITIES, STRINGUTILITIES, CONFIGUTILITIES, SYSTEMUTILITIES);
        CONFIGURATIONHANDLER = new ConfigurationHandler(LOCALIZATIONMANAGER, CURSECREATEMODPACK, VERSIONLISTER, APPLICATIONPROPERTIES, BOOLEANUTILITIES, LISTUTILITIES, STRINGUTILITIES, SYSTEMUTILITIES, CONFIGUTILITIES);
        PLUGINMANAGER = new ApplicationPlugins();
        SERVERPACKHANDLER = new ServerPackHandler(LOCALIZATIONMANAGER, CURSECREATEMODPACK, CONFIGURATIONHANDLER, APPLICATIONPROPERTIES, VERSIONLISTER, BOOLEANUTILITIES, LISTUTILITIES, STRINGUTILITIES, SYSTEMUTILITIES, CONFIGUTILITIES, PLUGINMANAGER);

    }

    @Test
    void runTest() {
        DEFAULTFILES.filesSetup();
        ConfigurationModel configurationModel = new ConfigurationModel();
        CONFIGURATIONHANDLER.checkConfiguration(new File("./backend/test/resources/testresources/serverpackcreator.conf"), configurationModel, true);
        SERVERPACKHANDLER.run(configurationModel);
        Assertions.assertTrue(new File("server-packs/forge_tests/libraries").isDirectory());
        Assertions.assertTrue(new File("server-packs/forge_tests/config").isDirectory());
        Assertions.assertTrue(new File("server-packs/forge_tests/defaultconfigs").isDirectory());
        Assertions.assertTrue(new File("server-packs/forge_tests/mods").isDirectory());
        Assertions.assertTrue(new File("server-packs/forge_tests/scripts").isDirectory());
        Assertions.assertTrue(new File("server-packs/forge_tests/seeds").isDirectory());
        Assertions.assertTrue(new File("server-packs/forge_tests/minecraft_server.1.16.5.jar").exists());
        Assertions.assertTrue(new File("server-packs/forge_tests/forge.jar").exists());
        Assertions.assertTrue(new File("server-packs/forge_tests/server.properties").exists());
        Assertions.assertTrue(new File("server-packs/forge_tests/server-icon.png").exists());
        Assertions.assertTrue(new File("server-packs/forge_tests/start.bat").exists());
        Assertions.assertTrue(new File("server-packs/forge_tests/start.sh").exists());
        try {
            Files.copy(Paths.get("./backend/test/resources/testresources/server_pack.zip"), Paths.get("server-packs/forge_tests_server_pack.zip"), REPLACE_EXISTING);
        } catch (Exception ignored) {}
    }

    @Test
    void zipBuilderFabricTest() {
        try {
            Files.createDirectories(Paths.get("server-packs/fabric_tests"));
        } catch (Exception ignored) {}
        String minecraftVersion = "1.16.5";
        String modpackDir = "./backend/test/resources/fabric_tests";
        SERVERPACKHANDLER.zipBuilder(minecraftVersion, true, modpackDir);
        Assertions.assertTrue(new File(modpackDir + "_server_pack.zip").exists());
        try {
            Files.copy(Paths.get("./backend/test/resources/testresources/server_pack.zip"), Paths.get("./backend/test/resources/fabric_tests/server_pack.zip"), REPLACE_EXISTING);
        } catch (Exception ignored) {}
    }

    @Test
    void zipBuilderForgeTest() {
        try {
            Files.createDirectories(Paths.get("server-packs/forge_tests"));
        } catch (Exception ignored) {}
        String minecraftVersion = "1.16.5";
        String modpackDir = "./backend/test/resources/forge_tests";
        SERVERPACKHANDLER.zipBuilder(minecraftVersion, true, modpackDir);
        Assertions.assertTrue(new File(modpackDir + "_server_pack.zip").exists());
        try {
            Files.copy(Paths.get("./backend/test/resources/testresources/server_pack.zip"), Paths.get("./backend/test/resources/forge_tests/server_pack.zip"), REPLACE_EXISTING);
        } catch (Exception ignored) {}
    }

    @Test
    void runServerPackTest() {
        List<String> clientMods = new ArrayList<>(Arrays.asList(
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
        ));
        List<String> copyDirs = new ArrayList<>(Arrays.asList(
                "config",
                "mods",
                "scripts",
                "seeds",
                "defaultconfigs"
        ));
        ServerPack serverPack = new ServerPack();
        try {
            FileUtils.copyDirectory(new File("./backend/test/resources/fabric_tests"), new File("./backend/test/resources/fabric_tests_copy"));
        } catch (Exception ignored) {}
        serverPack.setModpackDir("./backend/test/resources/fabric_tests_copy");
        serverPack.setClientMods(clientMods);
        serverPack.setCopyDirs(copyDirs);
        serverPack.setJavaPath("");
        serverPack.setIncludeServerInstallation(true);
        serverPack.setIncludeServerIcon(true);
        serverPack.setIncludeServerProperties(true);
        serverPack.setIncludeZipCreation(true);
        serverPack.setModLoader("Fabric");
        serverPack.setModLoaderVersion("0.13.1");
        serverPack.setMinecraftVersion("1.18.1");
        serverPack.setJavaArgs("");
        DEFAULTFILES.filesSetup();
        CONFIGURATIONHANDLER.checkConfiguration(serverPack, false);
        Assertions.assertNotNull(SERVERPACKHANDLER.run(serverPack));
        Assertions.assertTrue(new File("server-packs/fabric_tests_copy_server_pack.zip").isFile());
    }

    @Test
    void runServerPackTestOldMinecraftVersion() {
        List<String> clientMods = new ArrayList<>(Arrays.asList(
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
        ));
        List<String> copyDirs = new ArrayList<>(Arrays.asList(
                "config",
                "mods",
                "scripts",
                "seeds",
                "defaultconfigs"
        ));
        ServerPack serverPack = new ServerPack();
        try {
            FileUtils.copyDirectory(new File("./backend/test/resources/forge_tests"), new File("./backend/test/resources/forge_tests_copy"));
        } catch (Exception ignored) {}
        serverPack.setModpackDir("./backend/test/resources/forge_tests_copy");
        serverPack.setClientMods(clientMods);
        serverPack.setCopyDirs(copyDirs);
        serverPack.setJavaPath("");
        serverPack.setIncludeServerInstallation(true);
        serverPack.setIncludeServerIcon(true);
        serverPack.setIncludeServerProperties(true);
        serverPack.setIncludeZipCreation(true);
        serverPack.setModLoader("Forge");
        serverPack.setModLoaderVersion("14.23.5.2855");
        serverPack.setMinecraftVersion("1.12.2");
        serverPack.setJavaArgs("");
        DEFAULTFILES.filesSetup();
        CONFIGURATIONHANDLER.checkConfiguration(serverPack, false);
        Assertions.assertNotNull(SERVERPACKHANDLER.run(serverPack));
        Assertions.assertTrue(new File("server-packs/forge_tests_copy_server_pack.zip").isFile());
    }
}
