package de.griefed.serverpackcreator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConfigurationModelTest {

    ConfigurationModelTest() {

    }

    @Test
    void getsetClientModsTest() {
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
        ConfigurationModel configurationModel = new ConfigurationModel();
        configurationModel.setClientMods(clientMods);
        Assertions.assertEquals(clientMods, configurationModel.getClientMods());
    }

    @Test
    void getsetClientModsTextNotNull() {
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
        ConfigurationModel configurationModel = new ConfigurationModel();
        configurationModel.setClientMods(clientMods);
        Assertions.assertNotNull(configurationModel.getClientMods());
    }

    @Test
    void getsetCopyDirsTest() {
        List<String> testList = new ArrayList<>(Arrays.asList(
                "config",
                "mods",
                "scripts",
                "seeds",
                "defaultconfigs",
                "server_pack"
        ));
        List<String> getList = new ArrayList<>(Arrays.asList(
                "config",
                "mods",
                "scripts",
                "seeds",
                "defaultconfigs"
        ));
        ConfigurationModel configurationModel = new ConfigurationModel();
        configurationModel.setCopyDirs(testList);
        Assertions.assertEquals(getList, configurationModel.getCopyDirs());
    }

    @Test
    void getsetCopyDirsTestNotNull() {
        List<String> testList = new ArrayList<>(Arrays.asList(
                "config",
                "mods",
                "scripts",
                "seeds",
                "defaultconfigs",
                "server_pack"
        ));
        ConfigurationModel configurationModel = new ConfigurationModel();
        configurationModel.setCopyDirs(testList);
        Assertions.assertNotNull(configurationModel.getCopyDirs());
    }

    @Test
    void getsetCopyDirsTestFalse() {
        List<String> testList = new ArrayList<>(Arrays.asList(
                "config",
                "mods",
                "scripts",
                "seeds",
                "defaultconfigs",
                "server_pack"
        ));
        ConfigurationModel configurationModel = new ConfigurationModel();
        configurationModel.setCopyDirs(testList);
        Assertions.assertFalse(configurationModel.getCopyDirs().contains("server_pack"));
    }

    @Test
    void getsetModpackDirTest() {
        String modpackDir = "backend/test/resources/forge_tests";
        ConfigurationModel configurationModel = new ConfigurationModel();
        configurationModel.setModpackDir(modpackDir);
        Assertions.assertEquals(modpackDir, configurationModel.getModpackDir());
    }

    @Test
    void getsetModpackDirTestNotNull() {
        String modpackDir = "backend/test/resources/forge_tests";
        ConfigurationModel configurationModel = new ConfigurationModel();
        configurationModel.setModpackDir(modpackDir);
        Assertions.assertNotNull(configurationModel.getModpackDir());
    }

    @Test
    void getsetModpackDirTestBackslash() {
        String modpackDir = "backend\\test\\resources\\forge_tests";
        ConfigurationModel configurationModel = new ConfigurationModel();
        configurationModel.setModpackDir(modpackDir);
        Assertions.assertEquals(modpackDir.replace("\\","/"),configurationModel.getModpackDir());
    }

    @Test
    void getsetModpackDirTestBackslashFalse() {
        String modpackDir = "backend\\test\\resources\\forge_tests";
        ConfigurationModel configurationModel = new ConfigurationModel();
        configurationModel.setModpackDir(modpackDir);
        Assertions.assertFalse(configurationModel.getModpackDir().contains("\\"));
    }

    @Test
    void getsetModpackDirTestBackslashNotNull() {
        String modpackDir = "backend\\test\\resources\\forge_tests";
        ConfigurationModel configurationModel = new ConfigurationModel();
        configurationModel.setModpackDir(modpackDir);
        Assertions.assertNotNull(configurationModel.getModpackDir());
    }

    @Test
    void getsetJavaPathTest() {
        String javaPath = "backend/test/resources/forge_tests";
        ConfigurationModel configurationModel = new ConfigurationModel();
        configurationModel.setJavaPath(javaPath);
        Assertions.assertEquals(javaPath, configurationModel.getJavaPath());
    }

    @Test
    void getsetJavaPathTestNotNull() {
        String javaPath = "backend/test/resources/forge_tests";
        ConfigurationModel configurationModel = new ConfigurationModel();
        configurationModel.setJavaPath(javaPath);
        Assertions.assertNotNull(configurationModel.getJavaPath());
    }

    @Test
    void getsetJavaPathTestBackslash() {
        String javaPath = "backend\\test\\resources\\forge_tests";
        ConfigurationModel configurationModel = new ConfigurationModel();
        configurationModel.setJavaPath(javaPath);
        Assertions.assertEquals(javaPath.replace("\\","/"),configurationModel.getJavaPath());
    }

    @Test
    void getsetJavaPathTestBackslashNotNull() {
        String javaPath = "backend\\test\\resources\\forge_tests";
        ConfigurationModel configurationModel = new ConfigurationModel();
        configurationModel.setJavaPath(javaPath);
        Assertions.assertNotNull(configurationModel.getJavaPath());
    }

    @Test
    void getsetJavaPathTestBackslashNotEquals() {
        String javaPath = "backend\\test\\resources\\forge_tests";
        ConfigurationModel configurationModel = new ConfigurationModel();
        configurationModel.setJavaPath(javaPath);
        Assertions.assertNotEquals(javaPath, configurationModel.getJavaPath());
    }

    @Test
    void getsetJavaPathTestBackslashFalse() {
        String javaPath = "backend\\test\\resources\\forge_tests";
        ConfigurationModel configurationModel = new ConfigurationModel();
        configurationModel.setJavaPath(javaPath);
        Assertions.assertFalse(configurationModel.getJavaPath().contains("\\"));
    }

    @Test
    void getsetMinecraftVersionTest() {
        String minecraftVersion = "1.16.5";
        ConfigurationModel configurationModel = new ConfigurationModel();
        configurationModel.setMinecraftVersion(minecraftVersion);
        Assertions.assertEquals(minecraftVersion, configurationModel.getMinecraftVersion());
    }

    @Test
    void getsetMinecraftVersionTestNotNull() {
        String minecraftVersion = "1.16.5";
        ConfigurationModel configurationModel = new ConfigurationModel();
        configurationModel.setMinecraftVersion(minecraftVersion);
        Assertions.assertNotNull(configurationModel.getMinecraftVersion());
    }

    @Test
    void getsetModLoaderTest() {
        String modloader = "FoRgE";
        ConfigurationModel configurationModel = new ConfigurationModel();
        configurationModel.setModLoader(modloader);
        Assertions.assertEquals("Forge", configurationModel.getModLoader());
    }

    @Test
    void getsetModLoaderTestNotNull() {
        String modloader = "FoRgE";
        ConfigurationModel configurationModel = new ConfigurationModel();
        configurationModel.setModLoader(modloader);
        Assertions.assertNotNull(configurationModel.getModLoader());
    }

    @Test
    void getsetModLoaderTestNotEquals() {
        String modloader = "FoRgE";
        ConfigurationModel configurationModel = new ConfigurationModel();
        configurationModel.setModLoader(modloader);
        Assertions.assertNotEquals(modloader, configurationModel.getModLoader());
    }

    @Test
    void getsetModLoaderVersionTest() {
        String modloaderVersion = "36.1.2";
        ConfigurationModel configurationModel = new ConfigurationModel();
        configurationModel.setModLoaderVersion(modloaderVersion);
        Assertions.assertEquals(modloaderVersion, configurationModel.getModLoaderVersion());
    }

    @Test
    void getsetModLoaderVersionTestNotNull() {
        String modloaderVersion = "36.1.2";
        ConfigurationModel configurationModel = new ConfigurationModel();
        configurationModel.setModLoaderVersion(modloaderVersion);
        Assertions.assertNotNull(configurationModel.getModLoaderVersion());
    }

    @Test
    void getsetIncludeServerInstallationTest() {
        ConfigurationModel configurationModel = new ConfigurationModel();
        configurationModel.setIncludeServerInstallation(true);
        Assertions.assertTrue(configurationModel.getIncludeServerInstallation());
    }

    @Test
    void getsetIncludeServerInstallationTestFalse() {
        ConfigurationModel configurationModel = new ConfigurationModel();
        configurationModel.setIncludeServerInstallation(false);
        Assertions.assertFalse(configurationModel.getIncludeServerInstallation());
    }

    @Test
    void getsetIncludeServerIconTest() {
        ConfigurationModel configurationModel = new ConfigurationModel();
        configurationModel.setIncludeServerIcon(true);
        Assertions.assertTrue(configurationModel.getIncludeServerIcon());
    }

    @Test
    void getsetIncludeServerIconTestFalse() {
        ConfigurationModel configurationModel = new ConfigurationModel();
        configurationModel.setIncludeServerIcon(false);
        Assertions.assertFalse(configurationModel.getIncludeServerIcon());
    }

    @Test
    void getsetIncludeServerPropertiesTest() {
        ConfigurationModel configurationModel = new ConfigurationModel();
        configurationModel.setIncludeServerProperties(true);
        Assertions.assertTrue(configurationModel.getIncludeServerProperties());
    }

    @Test
    void getsetIncludeServerPropertiesTestFalse() {
        ConfigurationModel configurationModel = new ConfigurationModel();
        configurationModel.setIncludeServerProperties(false);
        Assertions.assertFalse(configurationModel.getIncludeServerProperties());
    }

    @Test
    void getsetIncludeZipCreationTest() {
        ConfigurationModel configurationModel = new ConfigurationModel();
        configurationModel.setIncludeZipCreation(true);
        Assertions.assertTrue(configurationModel.getIncludeZipCreation());
    }

    @Test
    void getsetIncludeZipCreationTestFalse() {
        ConfigurationModel configurationModel = new ConfigurationModel();
        configurationModel.setIncludeZipCreation(false);
        Assertions.assertFalse(configurationModel.getIncludeZipCreation());
    }

    @Test
    void getsetProjectIDTest() {
        int projectID = 123456;
        ConfigurationModel configurationModel = new ConfigurationModel();
        configurationModel.setProjectID(projectID);
        Assertions.assertEquals(projectID, configurationModel.getProjectID());
    }

    @Test
    void getsetProjectFileIDTest() {
        int fileID = 123456;
        ConfigurationModel configurationModel = new ConfigurationModel();
        configurationModel.setFileID(fileID);
        Assertions.assertEquals(fileID, configurationModel.getFileID());
    }
}
