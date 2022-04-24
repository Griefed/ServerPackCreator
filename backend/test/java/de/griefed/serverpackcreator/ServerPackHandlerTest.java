package de.griefed.serverpackcreator;

import de.griefed.serverpackcreator.curseforge.CurseCreateModpack;
import de.griefed.serverpackcreator.i18n.LocalizationManager;
import de.griefed.serverpackcreator.plugins.ApplicationPlugins;
import de.griefed.serverpackcreator.spring.serverpack.ServerPackModel;
import de.griefed.serverpackcreator.utilities.ConfigUtilities;
import de.griefed.serverpackcreator.utilities.commonutilities.Utilities;
import de.griefed.serverpackcreator.versionmeta.VersionMeta;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

class ServerPackHandlerTest {

    private final ServerPackHandler SERVERPACKHANDLER;
    private final ConfigurationHandler CONFIGURATIONHANDLER;
    private final ServerPackCreator SERVER_PACK_CREATOR;

    ServerPackHandlerTest() throws IOException {
        try {
            FileUtils.copyFile(new File("backend/main/resources/serverpackcreator.properties"),new File("serverpackcreator.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        ApplicationProperties APPLICATIONPROPERTIES = new ApplicationProperties();
        LocalizationManager LOCALIZATIONMANAGER = new LocalizationManager(APPLICATIONPROPERTIES);
        SERVER_PACK_CREATOR = new ServerPackCreator(new String[]{"--setup"});
        SERVER_PACK_CREATOR.run(CommandlineParser.Mode.SETUP);
        VersionMeta VERSIONMETA = new VersionMeta(
                APPLICATIONPROPERTIES.PATH_FILE_MANIFEST_MINECRAFT,
                APPLICATIONPROPERTIES.PATH_FILE_MANIFEST_FORGE,
                APPLICATIONPROPERTIES.PATH_FILE_MANIFEST_FABRIC,
                APPLICATIONPROPERTIES.PATH_FILE_MANIFEST_FABRIC_INSTALLER
        );
        Utilities UTILITIES = new Utilities(LOCALIZATIONMANAGER, APPLICATIONPROPERTIES);
        ConfigUtilities CONFIGUTILITIES = new ConfigUtilities(LOCALIZATIONMANAGER, UTILITIES, APPLICATIONPROPERTIES, VERSIONMETA);
        CurseCreateModpack CURSECREATEMODPACK = new CurseCreateModpack(LOCALIZATIONMANAGER, APPLICATIONPROPERTIES, VERSIONMETA, UTILITIES, CONFIGUTILITIES);
        this.CONFIGURATIONHANDLER = new ConfigurationHandler(LOCALIZATIONMANAGER, CURSECREATEMODPACK, VERSIONMETA, APPLICATIONPROPERTIES, UTILITIES, CONFIGUTILITIES);
        ApplicationPlugins PLUGINMANAGER = new ApplicationPlugins();
        this.SERVERPACKHANDLER = new ServerPackHandler(LOCALIZATIONMANAGER, CURSECREATEMODPACK, APPLICATIONPROPERTIES, VERSIONMETA, UTILITIES, PLUGINMANAGER, CONFIGUTILITIES);

    }

    @Test
    void runTest() throws IOException {
        SERVER_PACK_CREATOR.run(CommandlineParser.Mode.SETUP);
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
        ServerPackModel serverPackModel = new ServerPackModel();
        try {
            FileUtils.copyDirectory(new File("./backend/test/resources/fabric_tests"), new File("./backend/test/resources/fabric_tests_copy"));
        } catch (Exception ignored) {}
        serverPackModel.setModpackDir("./backend/test/resources/fabric_tests_copy");
        serverPackModel.setClientMods(clientMods);
        serverPackModel.setCopyDirs(copyDirs);
        serverPackModel.setJavaPath("");
        serverPackModel.setIncludeServerInstallation(true);
        serverPackModel.setIncludeServerIcon(true);
        serverPackModel.setIncludeServerProperties(true);
        serverPackModel.setIncludeZipCreation(true);
        serverPackModel.setModLoader("Fabric");
        serverPackModel.setModLoaderVersion("0.13.1");
        serverPackModel.setMinecraftVersion("1.18.1");
        serverPackModel.setJavaArgs("");
        SERVER_PACK_CREATOR.run(CommandlineParser.Mode.SETUP);
        CONFIGURATIONHANDLER.checkConfiguration(serverPackModel, false);
        Assertions.assertNotNull(SERVERPACKHANDLER.run(serverPackModel));
        Assertions.assertTrue(new File("server-packs/fabric_tests_copy_server_pack.zip").isFile());
    }

    @Test
    void runServerPackTestOldMinecraftVersion() throws IOException {
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
        ServerPackModel serverPackModel = new ServerPackModel();
        try {
            FileUtils.copyDirectory(new File("./backend/test/resources/forge_tests"), new File("./backend/test/resources/forge_tests_copy"));
        } catch (Exception ignored) {}
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
        SERVER_PACK_CREATOR.run(CommandlineParser.Mode.SETUP);
        CONFIGURATIONHANDLER.checkConfiguration(serverPackModel, false);
        Assertions.assertNotNull(SERVERPACKHANDLER.run(serverPackModel));
        Assertions.assertTrue(new File("server-packs/forge_tests_copy_server_pack.zip").isFile());
    }
}
