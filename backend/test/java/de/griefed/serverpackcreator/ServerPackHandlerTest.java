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
        CONFIGUTILITIES = new ConfigUtilities(LOCALIZATIONMANAGER, BOOLEANUTILITIES, LISTUTILITIES, APPLICATIONPROPERTIES, STRINGUTILITIES);
        CURSECREATEMODPACK = new CurseCreateModpack(LOCALIZATIONMANAGER, APPLICATIONPROPERTIES, VERSIONLISTER, BOOLEANUTILITIES, LISTUTILITIES, STRINGUTILITIES, CONFIGUTILITIES);
        CONFIGURATIONHANDLER = new ConfigurationHandler(LOCALIZATIONMANAGER, CURSECREATEMODPACK, VERSIONLISTER, APPLICATIONPROPERTIES, BOOLEANUTILITIES, LISTUTILITIES, STRINGUTILITIES, SYSTEMUTILITIES, CONFIGUTILITIES);
        PLUGINMANAGER = new ApplicationPlugins();
        SERVERPACKHANDLER = new ServerPackHandler(LOCALIZATIONMANAGER, CURSECREATEMODPACK, CONFIGURATIONHANDLER, APPLICATIONPROPERTIES, VERSIONLISTER, BOOLEANUTILITIES, LISTUTILITIES, STRINGUTILITIES, SYSTEMUTILITIES, CONFIGUTILITIES, PLUGINMANAGER);

    }

    @Test
    void runTest() throws IOException {
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

        FileUtils.deleteQuietly(new File("server-packs/forge_tests/libraries"));
        FileUtils.deleteQuietly(new File("server-packs/forge_tests/config"));
        FileUtils.deleteQuietly(new File("server-packs/forge_tests/defaultconfigs"));
        FileUtils.deleteQuietly(new File("server-packs/forge_tests/mods"));
        FileUtils.deleteQuietly(new File("server-packs/forge_tests/scripts"));
        FileUtils.deleteQuietly(new File("server-packs/forge_tests/seeds"));
        FileUtils.deleteQuietly(new File("server_files"));
        FileUtils.deleteQuietly(new File("server-packs/forge_tests/1.16.5.json"));
        FileUtils.deleteQuietly(new File("server-packs/forge_tests/minecraft_server.1.16.5.jar"));
        FileUtils.deleteQuietly(new File("server-packs/forge_tests/forge.jar"));
        FileUtils.deleteQuietly(new File("server-packs/forge_tests/server.properties"));
        FileUtils.deleteQuietly(new File("server-packs/forge_tests/server-icon.png"));
        FileUtils.deleteQuietly(new File("server-packs/forge_tests/start.bat"));
        FileUtils.deleteQuietly(new File("server-packs/forge_tests/start.sh"));
        FileUtils.deleteQuietly(new File("./serverpackcreator.conf"));
        Files.copy(Paths.get("./backend/test/resources/testresources/server_pack.zip"), Paths.get("server-packs/forge_tests_server_pack.zip"), REPLACE_EXISTING);
    }

    @Test
    void zipBuilderFabricTest() throws IOException {
        Files.createDirectories(Paths.get("server-packs/fabric_tests"));
        String minecraftVersion = "1.16.5";
        String modpackDir = "./backend/test/resources/fabric_tests";
        SERVERPACKHANDLER.zipBuilder(minecraftVersion, true, modpackDir);
        Assertions.assertTrue(new File(modpackDir + "_server_pack.zip").exists());
        FileUtils.deleteQuietly(new File(modpackDir + "_server_pack.zip"));
        Files.copy(Paths.get("./backend/test/resources/testresources/server_pack.zip"), Paths.get("./backend/test/resources/fabric_tests/server_pack.zip"), REPLACE_EXISTING);
    }

    @Test
    void zipBuilderForgeTest() throws IOException {
        Files.createDirectories(Paths.get("server-packs/forge_tests"));
        String minecraftVersion = "1.16.5";
        String modpackDir = "./backend/test/resources/forge_tests";
        SERVERPACKHANDLER.zipBuilder(minecraftVersion, true, modpackDir);
        Assertions.assertTrue(new File(modpackDir + "_server_pack.zip").exists());
        FileUtils.deleteQuietly(new File(modpackDir + "_server_pack.zip"));
        Files.copy(Paths.get("./backend/test/resources/testresources/server_pack.zip"), Paths.get("./backend/test/resources/forge_tests/server_pack.zip"), REPLACE_EXISTING);
    }

    @Test
    void runServerPackTest() throws IOException {
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
        FileUtils.copyDirectory(new File("./backend/test/resources/forge_tests"), new File("./backend/test/resources/forge_tests_copy"));
        serverPack.setModpackDir("./backend/test/resources/forge_tests_copy");
        serverPack.setClientMods(clientMods);
        serverPack.setCopyDirs(copyDirs);
        serverPack.setJavaPath("");
        serverPack.setIncludeServerInstallation(true);
        serverPack.setIncludeServerIcon(true);
        serverPack.setIncludeServerProperties(true);
        serverPack.setIncludeZipCreation(true);
        serverPack.setModLoader("Forge");
        serverPack.setModLoaderVersion("36.1.2");
        serverPack.setMinecraftVersion("1.16.5");
        serverPack.setJavaArgs("");
        DEFAULTFILES.filesSetup();
        CONFIGURATIONHANDLER.checkConfiguration(serverPack, false);
        SERVERPACKHANDLER.run(serverPack);
        Assertions.assertFalse(new File("server-packs/forge_tests_copy/libraries").isDirectory());
        Assertions.assertFalse(new File("server-packs/forge_tests_copy/config").isDirectory());
        Assertions.assertFalse(new File("server-packs/forge_tests_copy/defaultconfigs").isDirectory());
        Assertions.assertFalse(new File("server-packs/forge_tests_copy/mods").isDirectory());
        Assertions.assertFalse(new File("server-packs/forge_tests_copy/scripts").isDirectory());
        Assertions.assertFalse(new File("server-packs/forge_tests_copy/seeds").isDirectory());

        Assertions.assertFalse(new File("server-packs/forge_tests_copy/1.16.5.json").exists());
        Assertions.assertFalse(new File("server-packs/forge_tests_copy/minecraft_server.1.16.5.jar").exists());
        Assertions.assertFalse(new File("server-packs/forge_tests_copy/forge.jar").exists());
        Assertions.assertFalse(new File("server-packs/forge_tests_copy/server.properties").exists());
        Assertions.assertFalse(new File("server-packs/forge_tests_copy/server-icon.png").exists());
        Assertions.assertFalse(new File("server-packs/forge_tests_copy/start.bat").exists());
        Assertions.assertFalse(new File("server-packs/forge_tests_copy/start.sh").exists());

        FileUtils.deleteQuietly(new File("server-packs/forge_tests_copy/libraries"));
        FileUtils.deleteQuietly(new File("server-packs/forge_tests_copy/config"));
        FileUtils.deleteQuietly(new File("server-packs/forge_tests_copy/defaultconfigs"));
        FileUtils.deleteQuietly(new File("server-packs/forge_tests_copy/mods"));
        FileUtils.deleteQuietly(new File("server-packs/forge_tests_copy/scripts"));
        FileUtils.deleteQuietly(new File("server-packs/forge_tests_copy/seeds"));
        FileUtils.deleteQuietly(new File("server-packs/forge_tests_copy/1.16.5.json"));
        FileUtils.deleteQuietly(new File("server-packs/forge_tests_copy/minecraft_server.1.16.5.jar"));
        FileUtils.deleteQuietly(new File("server-packs/forge_tests_copy/forge.jar"));
        FileUtils.deleteQuietly(new File("server-packs/forge_tests_copy/server.properties"));
        FileUtils.deleteQuietly(new File("server-packs/forge_tests_copy/server-icon.png"));
        FileUtils.deleteQuietly(new File("server-packs/forge_tests_copy/start.bat"));
        FileUtils.deleteQuietly(new File("server-packs/forge_tests_copy/start.sh"));
        FileUtils.deleteQuietly(new File("./serverpackcreator.conf"));

        Files.copy(Paths.get("./backend/test/resources/testresources/server_pack.zip"), Paths.get("server-packs/forge_tests_server_pack.zip"), REPLACE_EXISTING);
    }
}
