/* Copyright (C) 2021  Griefed
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 * USA
 *
 * The full license can be found at https:github.com/Griefed/ServerPackCreator/blob/main/LICENSE
 */
package de.griefed.serverpackcreator;

import de.griefed.serverpackcreator.curseforge.CurseCreateModpack;
import de.griefed.serverpackcreator.curseforge.CurseModpack;
import de.griefed.serverpackcreator.i18n.LocalizationManager;
import de.griefed.serverpackcreator.utilities.VersionLister;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * <strong>Table of tests</strong><p>
 * 1. {@link #ConfigurationTest()}<br>
 * 2. {@link #getOldConfigFileTest()}<br>
 * 3. {@link #getConfigFileTest()}<br>
 * 4. {@link #getsetConfigTest()}<br>
 * 5. {@link #getFallbackModsListTest()}<br>
 * 6. {@link #getFallbackModsListTestEquals()}<br>
 * 7. {@link #getsetClientModsTest()}<br>
 * 8. {@link #getsetClientModsTextNotNull()}<br>
 * 9. {@link #getsetCopyDirsTest()}<br>
 * 10.{@link #getsetCopyDirsTestNotNull()}<br>
 * 11.{@link #getsetCopyDirsTestFalse()}<br>
 * 12.{@link #getsetModpackDirTest()}<br>
 * 13.{@link #getsetModpackDirTestNotNull()}<br>
 * 14.{@link #getsetModpackDirTestBackslash()}<br>
 * 15.{@link #getsetModpackDirTestBackslashFalse()}<br>
 * 16.{@link #getsetModpackDirTestBackslashNotNull()}<br>
 * 17.{@link #getsetJavaPathTest()}<br>
 * 18.{@link #getsetJavaPathTestNotNull()}<br>
 * 19.{@link #getsetJavaPathTestBackslash()}<br>
 * 20.{@link #getsetJavaPathTestBackslashNotNull()}<br>
 * 21.{@link #getsetJavaPathTestBackslashNotEquals()}<br>
 * 22.{@link #getsetJavaPathTestBackslashFalse()}<br>
 * 23.{@link #getsetMinecraftVersionTest()}<br>
 * 24.{@link #getsetModLoaderTest()}<br>
 * 25.{@link #getsetModLoaderTestNotNull()}<br>
 * 26.{@link #getsetModLoaderTestNotEquals()}<br>
 * 27.{@link #getsetModLoaderVersionTest()}<br>
 * 28.{@link #getsetModLoaderVersionTestNotNull()}<br>
 * 29.{@link #getsetIncludeServerInstallationTest()}<br>
 * 30.{@link #getsetIncludeServerInstallationTestFalse()}<br>
 * 31.{@link #getsetIncludeServerIconTest()}<br>
 * 32.{@link #getsetIncludeServerIconTestFalse()}<br>
 * 33.{@link #getsetIncludeServerPropertiesTest()}<br>
 * 34.{@link #getsetIncludeServerPropertiesTestFalse()}<br>
 * 35.{@link #getsetIncludeStartScriptsTest()}<br>
 * 36.{@link #getsetIncludeStartScriptsTestFalse()}<br>
 * 37.{@link #getsetIncludeZipCreationTest()}<br>
 * 38.{@link #getsetIncludeZipCreationTestFalse()}<br>
 * 39.{@link #getsetProjectIDTest()}<br>
 * 40.{@link #getsetProjectFileIDTest()}<br>
 * 41.{@link #checkConfigFileTest()}<br>
 * 42.{@link #isDirTest()}<br>
 * 43.{@link #isDirTestCopyDirs()}<br>
 * 44.{@link #isDirTestJavaPath()}<br>
 * 45.{@link #isDirTestMinecraftVersion()}<br>
 * 46.{@link #isDirTestModLoader()}<br>
 * 47.{@link #isDirTestModLoaderFalse()}<br>
 * 48.{@link #isDirTestModLoaderVersion()}<br>
 * 49.{@link #isCurseTest()}<br>
 * 50.{@link #isCurseTestProjectIDFalse()}<br>
 * 51.{@link #isCurseTestProjectFileIDFalse()}<br>
 * 52.{@link #containsFabricTest()}<br>
 * 53.{@link #containsFabricTestFalse()}<br>
 * 54.{@link #suggestCopyDirsTest()}<br>
 * 55.{@link #suggestCopyDirsTestFalse()}<br>
 * 56.{@link #checkCurseForgeTest()}<br>
 * 57.{@link #checkCurseForgeTestFalse()}<br>
 * 58.{@link #convertToBooleanTestTrue()}<br>
 * 59.{@link #convertToBooleanTestFalse()}<br>
 * 60.{@link #printConfigTest()}<br>
 * 61.{@link #checkModpackDirTest()}<br>
 * 62.{@link #checkModpackDirTestFalse()}<br>
 * 63.{@link #checkCopyDirsTest()}<br>
 * 64.{@link #checkCopyDirsTestFalse()}<br>
 * 65.{@link #checkCopyDirsTestFiles()}<br>
 * 66.{@link #checkCopyDirsTestFilesFalse()}<br>
 * 67.{@link #checkJavaPathTest()}<br>
 * 68.{@link #checkModloaderTestForge()}<br>
 * 69.{@link #checkModloaderTestForgeCase()}<br>
 * 70.{@link #checkModloaderTestFabric()}<br>
 * 71.{@link #checkModloaderTestFabricCase()}<br>
 * 72.{@link #checkModLoaderTestFalse()}<br>
 * 73.{@link #setModLoaderCaseTestForge()}<br>
 * 74.{@link #setModLoaderCaseTestFabric()}<br>
 * 75.{@link #setModLoaderCaseTestForgeCorrected()}<br>
 * 76.{@link #setModLoaderCaseTestFabricCorrected()}<br>
 * 77.{@link #checkModloaderVersionTestForge()}<br>
 * 78.{@link #checkModloaderVersionTestForgeFalse()}<br>
 * 79.{@link #checkModloaderVersionTestFabric()}<br>
 * 80.{@link #checkModloaderVersionTestFabricFalse()}<br>
 * 81.{@link #isMinecraftVersionCorrectTest()}<br>
 * 82.{@link #isMinecraftVersionCorrectTestFalse()}<br>
 * 83.{@link #isFabricVersionCorrectTest()}<br>
 * 84.{@link #isFabricVersionCorrectTestFalse()}<br>
 * 85.{@link #isForgeVersionCorrectTest()}<br>
 * 86.{@link #isForgeVersionCorrectTestFalse()}<br>
 * 87.{@link #buildStringTest()}<br>
 * 88.{@link #writeConfigToFileTestForge()}<br>
 * 89.{@link #writeConfigToFileTestFabric()}<br>
 * 90.{@link #checkConfigModelTest()}<br>
 * 91.{@link #encapsulateListElementsTest()}
 */
class ConfigurationTest {

    private final ConfigurationHandler CONFIGURATIONHANDLER;
    private final CurseCreateModpack CURSECREATEMODPACK;
    private final LocalizationManager LOCALIZATIONMANAGER;
    private final DefaultFiles DEFAULTFILES;
    private final VersionLister VERSIONLISTER;
    private Properties serverpackcreatorproperties;

    ConfigurationTest() {
        try {
            FileUtils.copyFile(new File("backend/main/resources/serverpackcreator.properties"),new File("serverpackcreator.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (InputStream inputStream = new FileInputStream("serverpackcreator.properties")) {
            this.serverpackcreatorproperties = new Properties();
            this.serverpackcreatorproperties.load(inputStream);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        LOCALIZATIONMANAGER = new LocalizationManager();
        LOCALIZATIONMANAGER.init();
        DEFAULTFILES = new DefaultFiles(LOCALIZATIONMANAGER);
        VERSIONLISTER = new VersionLister();
        CURSECREATEMODPACK = new CurseCreateModpack(LOCALIZATIONMANAGER);
        CONFIGURATIONHANDLER = new ConfigurationHandler(LOCALIZATIONMANAGER, CURSECREATEMODPACK, VERSIONLISTER, serverpackcreatorproperties);
    }

    @Test
    void getOldConfigFileTest() {
        File file = CONFIGURATIONHANDLER.getOldConfigFile();
        Assertions.assertNotNull(file);
    }

    @Test
    void getConfigFileTest() {
        File file = CONFIGURATIONHANDLER.getConfigFile();
        Assertions.assertNotNull(file);
    }

    @Test
    void getsetConfigTest() {
        File file = new File("backend/test/resources/testresources/serverpackcreator.conf");
        CONFIGURATIONHANDLER.setConfig(file);
        Assertions.assertNotNull(CONFIGURATIONHANDLER.getConfig());
    }

    @Test
    void getFallbackModsListTest() {
        Assertions.assertNotNull(CONFIGURATIONHANDLER.getFallbackModsList());
    }

    @Test
    void getFallbackModsListTestEquals() {
        List<String> fallbackMods = new ArrayList<>(Arrays.asList(
                "AmbientSounds",
                "BackTools",
                "BetterAdvancement",
                "BetterFoliage",
                "BetterPing",
                "BetterPlacement",
                "Blur",
                "cherished",
                "ClientTweaks",
                "Controlling",
                "CTM",
                "customdiscordrpc",
                "CustomMainMenu",
                "DefaultOptions",
                "durability",
                "DynamicSurroundings",
                "EiraMoticons",
                "FullscreenWindowed",
                "itemzoom",
                "itlt",
                "jeiintegration",
                "jei-professions",
                "just-enough-harvestcraft",
                "JustEnoughResources",
                "keywizard",
                "modnametooltip",
                "MouseTweaks",
                "multihotbar-",
                "Neat",
                "OldJavaWarning",
                "PackMenu",
                "preciseblockplacing",
                "ResourceLoader",
                "SimpleDiscordRichPresence",
                "SpawnerFix",
                "timestamps",
                "TipTheScales",
                "WorldNameRandomizer"
        ));
        Assertions.assertEquals(fallbackMods, CONFIGURATIONHANDLER.getFallbackModsList());
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
    void getsetIncludeStartScriptsTest() {
        ConfigurationModel configurationModel = new ConfigurationModel();
        configurationModel.setIncludeStartScripts(true);
        Assertions.assertTrue(configurationModel.getIncludeStartScripts());
    }

    @Test
    void getsetIncludeStartScriptsTestFalse() {
        ConfigurationModel configurationModel = new ConfigurationModel();
        configurationModel.setIncludeStartScripts(false);
        Assertions.assertFalse(configurationModel.getIncludeStartScripts());
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
        CONFIGURATIONHANDLER.setProjectID(projectID);
        Assertions.assertEquals(projectID, CONFIGURATIONHANDLER.getProjectID());
    }

    @Test
    void getsetProjectFileIDTest() {
        int fileID = 123456;
        CONFIGURATIONHANDLER.setProjectFileID(fileID);
        Assertions.assertEquals(fileID, CONFIGURATIONHANDLER.getProjectFileID());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void checkConfigFileTest() throws IOException {
        Files.copy(Paths.get("./backend/test/resources/testresources/serverpackcreator.conf"), Paths.get("./serverpackcreator.conf"), REPLACE_EXISTING);
        CONFIGURATIONHANDLER.setConfig(new File("./serverpackcreator.conf"));
        ConfigurationModel configurationModel = new ConfigurationModel();
        boolean result = CONFIGURATIONHANDLER.checkConfiguration(CONFIGURATIONHANDLER.getConfigFile(), true, configurationModel);
        Assertions.assertFalse(result);
        new File("./serverpackcreator.conf").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void isDirTest() throws IOException {
        Files.copy(Paths.get("./backend/test/resources/testresources/serverpackcreator.conf"), Paths.get("./serverpackcreator.conf"), REPLACE_EXISTING);
        CONFIGURATIONHANDLER.setConfig(new File("./serverpackcreator.conf"));
        ConfigurationModel configurationModel = new ConfigurationModel();
        configurationModel.setIncludeServerInstallation(true);
        Assertions.assertFalse(CONFIGURATIONHANDLER.isDir("./backend/test/resources/forge_tests", configurationModel));
        new File("./serverpackcreator.conf").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void isDirTestCopyDirs() throws IOException {
        Files.copy(Paths.get("./backend/test/resources/testresources/serverpackcreator_copydirs.conf"), Paths.get("./serverpackcreator.conf"), REPLACE_EXISTING);
        CONFIGURATIONHANDLER.setConfig(new File("./serverpackcreator.conf"));
        ConfigurationModel configurationModel = new ConfigurationModel();
        configurationModel.setIncludeServerInstallation(true);
        Assertions.assertTrue(CONFIGURATIONHANDLER.isDir("./backend/test/resources/forge_tests", configurationModel));
        new File("./serverpackcreator.conf").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void isDirTestJavaPath() throws IOException {
        Files.copy(Paths.get("./backend/test/resources/testresources/serverpackcreator_javapath.conf"), Paths.get("./serverpackcreator.conf"), REPLACE_EXISTING);
        CONFIGURATIONHANDLER.setConfig(new File("./serverpackcreator.conf"));
        ConfigurationModel configurationModel = new ConfigurationModel();
        configurationModel.setIncludeServerInstallation(true);
        Assertions.assertFalse(CONFIGURATIONHANDLER.isDir("./backend/test/resources/forge_tests", configurationModel));
        new File("./serverpackcreator.conf").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void isDirTestMinecraftVersion() throws IOException {
        Files.copy(Paths.get("./backend/test/resources/testresources/serverpackcreator_minecraftversion.conf"), Paths.get("./serverpackcreator.conf"), REPLACE_EXISTING);
        CONFIGURATIONHANDLER.setConfig(new File("./serverpackcreator.conf"));
        ConfigurationModel configurationModel = new ConfigurationModel();
        configurationModel.setIncludeServerInstallation(true);
        Assertions.assertTrue(CONFIGURATIONHANDLER.isDir("./backend/test/resources/forge_tests", configurationModel));
        new File("./serverpackcreator.conf").delete();

    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void isDirTestModLoader() throws IOException {
        Files.copy(Paths.get("./backend/test/resources/testresources/serverpackcreator_modloader.conf"), Paths.get("./serverpackcreator.conf"), REPLACE_EXISTING);
        CONFIGURATIONHANDLER.setConfig(new File("./serverpackcreator.conf"));
        ConfigurationModel configurationModel = new ConfigurationModel();
        configurationModel.setIncludeServerInstallation(true);
        Assertions.assertFalse(CONFIGURATIONHANDLER.isDir("./backend/test/resources/forge_tests", configurationModel));
        new File("./serverpackcreator.conf").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void isDirTestModLoaderFalse() throws IOException {
        Files.copy(Paths.get("./backend/test/resources/testresources/serverpackcreator_modloaderfalse.conf"), Paths.get("./serverpackcreator.conf"), REPLACE_EXISTING);
        CONFIGURATIONHANDLER.setConfig(new File("./serverpackcreator.conf"));
        ConfigurationModel configurationModel = new ConfigurationModel();
        configurationModel.setIncludeServerInstallation(true);
        Assertions.assertTrue(CONFIGURATIONHANDLER.isDir("./backend/test/resources/forge_tests", configurationModel));
        new File("./serverpackcreator.conf").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void isDirTestModLoaderVersion() throws IOException {
        Files.copy(Paths.get("./backend/test/resources/testresources/serverpackcreator_modloaderversion.conf"), Paths.get("./serverpackcreator.conf"), REPLACE_EXISTING);
        CONFIGURATIONHANDLER.setConfig(new File("./serverpackcreator.conf"));
        ConfigurationModel configurationModel = new ConfigurationModel();
        configurationModel.setIncludeServerInstallation(true);
        Assertions.assertTrue(CONFIGURATIONHANDLER.isDir("./backend/test/resources/forge_tests", configurationModel));
        new File("./serverpackcreator.conf").delete();
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    @Test
    void isCurseTest() throws IOException {
        Files.copy(Paths.get("./backend/test/resources/testresources/serverpackcreator_curseforge.conf"), Paths.get("./serverpackcreator.conf"), REPLACE_EXISTING);
        ConfigurationModel configurationModel = new ConfigurationModel();
        CONFIGURATIONHANDLER.setConfig(new File("./serverpackcreator.conf"));
        configurationModel.setJavaPath(CONFIGURATIONHANDLER.checkJavaPath(""));
        configurationModel.setIncludeServerInstallation(true);
        configurationModel.setIncludeServerIcon(true);
        configurationModel.setIncludeServerProperties(true);
        configurationModel.setIncludeStartScripts(true);
        configurationModel.setIncludeZipCreation(true);
        configurationModel.setJavaArgs("empty");
        CONFIGURATIONHANDLER.checkCurseForge("238298,3174854");
        configurationModel.setClientMods(CONFIGURATIONHANDLER.getFallbackModsList());

        Assertions.assertFalse(CONFIGURATIONHANDLER.isCurse(configurationModel));
        new File("./serverpackcreator.conf").delete();
        String deleteFolder = "Vanilla Forge";
        if (new File(deleteFolder).isDirectory()) {
            Path pathToBeDeleted = Paths.get(deleteFolder);
            Files.walk(pathToBeDeleted)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    @Test
    void isCurseTestProjectIDFalse() throws IOException {
        Files.copy(Paths.get("./backend/test/resources/testresources/serverpackcreator_curseforgefalse.conf"), Paths.get("./serverpackcreator.conf"), REPLACE_EXISTING);
        ConfigurationModel configurationModel = new ConfigurationModel();
        CONFIGURATIONHANDLER.setConfig(new File("./serverpackcreator.conf"));
        configurationModel.setIncludeServerInstallation(true);
        configurationModel.setIncludeServerIcon(true);
        configurationModel.setIncludeServerProperties(true);
        configurationModel.setIncludeStartScripts(true);
        configurationModel.setIncludeZipCreation(true);
        CONFIGURATIONHANDLER.checkCurseForge("999999,3174854");
        configurationModel.setClientMods(CONFIGURATIONHANDLER.getFallbackModsList());
        Assertions.assertTrue(CONFIGURATIONHANDLER.isCurse(configurationModel));
        new File("./serverpackcreator.conf").delete();
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    @Test
    void isCurseTestProjectFileIDFalse() throws IOException {
        Files.copy(Paths.get("./backend/test/resources/testresources/serverpackcreator_curseforgefilefalse.conf"), Paths.get("./serverpackcreator.conf"), REPLACE_EXISTING);
        CONFIGURATIONHANDLER.setConfig(new File("./serverpackcreator.conf"));
        CONFIGURATIONHANDLER.setProjectID(238298);
        CONFIGURATIONHANDLER.setProjectFileID(999999);
        ConfigurationModel configurationModel = new ConfigurationModel();
        Assertions.assertTrue(CONFIGURATIONHANDLER.isCurse(configurationModel));
        new File("./serverpackcreator.conf").delete();
    }

    @Test
    void containsFabricTest() throws IOException {
        byte[] fabricJsonData = Files.readAllBytes(Paths.get("backend/test/resources/testresources/fabric_manifest.json"));
        CurseModpack fabricModpack = CONFIGURATIONHANDLER.getObjectMapper().readValue(fabricJsonData, CurseModpack.class);
        Assertions.assertTrue(CONFIGURATIONHANDLER.containsFabric(fabricModpack));
    }

    @Test
    void containsFabricTestFalse() throws IOException {
        byte[] forgeJsonData = Files.readAllBytes(Paths.get("backend/test/resources/testresources/manifest.json"));
        CurseModpack forgeModpack = CONFIGURATIONHANDLER.getObjectMapper().readValue(forgeJsonData, CurseModpack.class);
        Assertions.assertFalse(CONFIGURATIONHANDLER.containsFabric(forgeModpack));
    }

    @Test
    void suggestCopyDirsTest() {
        String modpackDir = "backend/test/resources/forge_tests";
        Assertions.assertFalse(CONFIGURATIONHANDLER.suggestCopyDirs(modpackDir).contains("server_pack"));
        Assertions.assertTrue(CONFIGURATIONHANDLER.suggestCopyDirs(modpackDir).contains("config"));
        Assertions.assertTrue(CONFIGURATIONHANDLER.suggestCopyDirs(modpackDir).contains("defaultconfigs"));
        Assertions.assertTrue(CONFIGURATIONHANDLER.suggestCopyDirs(modpackDir).contains("mods"));
        Assertions.assertTrue(CONFIGURATIONHANDLER.suggestCopyDirs(modpackDir).contains("scripts"));
        Assertions.assertTrue(CONFIGURATIONHANDLER.suggestCopyDirs(modpackDir).contains("seeds"));

    }

    @Test
    void suggestCopyDirsTestFalse() {
        String modpackDir = "backend/test/resources/forge_tests";
        Assertions.assertFalse(CONFIGURATIONHANDLER.suggestCopyDirs(modpackDir).contains("server_pack"));
        Assertions.assertFalse(CONFIGURATIONHANDLER.suggestCopyDirs(modpackDir).contains("saves"));
        Assertions.assertFalse(CONFIGURATIONHANDLER.suggestCopyDirs(modpackDir).contains("logs"));
        Assertions.assertFalse(CONFIGURATIONHANDLER.suggestCopyDirs(modpackDir).contains("resourcepacks"));
        Assertions.assertTrue(CONFIGURATIONHANDLER.suggestCopyDirs(modpackDir).contains("scripts"));
        Assertions.assertTrue(CONFIGURATIONHANDLER.suggestCopyDirs(modpackDir).contains("seeds"));
    }

    @Test
    void checkCurseForgeTest() {
        String valid = "430517,3266321";
        Assertions.assertTrue(CONFIGURATIONHANDLER.checkCurseForge(valid));
    }

    @Test
    void checkCurseForgeTestFalse() {
        String invalid = "1,1234";
        Assertions.assertFalse(CONFIGURATIONHANDLER.checkCurseForge(invalid));
    }

    @Test
    void convertToBooleanTestTrue() {
        Assertions.assertTrue(CONFIGURATIONHANDLER.convertToBoolean("True"));
        Assertions.assertTrue(CONFIGURATIONHANDLER.convertToBoolean("true"));
        Assertions.assertTrue(CONFIGURATIONHANDLER.convertToBoolean("1"));
        Assertions.assertTrue(CONFIGURATIONHANDLER.convertToBoolean("Yes"));
        Assertions.assertTrue(CONFIGURATIONHANDLER.convertToBoolean("yes"));
        Assertions.assertTrue(CONFIGURATIONHANDLER.convertToBoolean("Y"));
        Assertions.assertTrue(CONFIGURATIONHANDLER.convertToBoolean("y"));
    }

    @Test
    void convertToBooleanTestFalse() {
        Assertions.assertFalse(CONFIGURATIONHANDLER.convertToBoolean("False"));
        Assertions.assertFalse(CONFIGURATIONHANDLER.convertToBoolean("false"));
        Assertions.assertFalse(CONFIGURATIONHANDLER.convertToBoolean("0"));
        Assertions.assertFalse(CONFIGURATIONHANDLER.convertToBoolean("No"));
        Assertions.assertFalse(CONFIGURATIONHANDLER.convertToBoolean("no"));
        Assertions.assertFalse(CONFIGURATIONHANDLER.convertToBoolean("N"));
        Assertions.assertFalse(CONFIGURATIONHANDLER.convertToBoolean("n"));
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    void printConfigTest() {
        String modpackDir = "backend/test/resources/forge_tests";
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
        boolean includeServerInstallation = true;
        String javaPath = "/usr/bin/java";
        String minecraftVersion = "1.16.5";
        String modLoader = "Forge";
        String modLoaderVersion = "36.1.2";
        String javaArgs = "tf3g4jz89agz843fag8z49a3zg8ap3jg8zap9vagv3z8j";
        boolean includeServerIcon = true;
        boolean includeServerProperties = true;
        boolean includeStartScripts = true;
        boolean includeZipCreation = true;
        CONFIGURATIONHANDLER.printConfig(
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
                includeStartScripts,
                includeZipCreation,
                javaArgs);
    }

    @Test
    void checkModpackDirTest() {
        String modpackDirCorrect = "./backend/test/resources/forge_tests";
        Assertions.assertTrue(CONFIGURATIONHANDLER.checkModpackDir(modpackDirCorrect));
    }

    @Test
    void checkModpackDirTestFalse() {
        Assertions.assertFalse(CONFIGURATIONHANDLER.checkModpackDir("modpackDir"));
    }

    @Test
    void checkCopyDirsTest() {
        String modpackDir = "backend/test/resources/forge_tests";
        List<String> copyDirs = new ArrayList<>(Arrays.asList(
                "config",
                "mods",
                "scripts",
                "seeds",
                "defaultconfigs"
        ));
        Assertions.assertTrue(CONFIGURATIONHANDLER.checkCopyDirs(copyDirs, modpackDir));
    }

    @Test
    void checkCopyDirsTestFalse() {
        String modpackDir = "backend/test/resources/forge_tests";
        List<String> copyDirsInvalid = new ArrayList<>(Arrays.asList(
                "configs",
                "modss",
                "scriptss",
                "seedss",
                "defaultconfigss"
        ));
        Assertions.assertFalse(CONFIGURATIONHANDLER.checkCopyDirs(copyDirsInvalid, modpackDir));
    }

    @Test
    void checkCopyDirsTestFiles() {
        String modpackDir = "backend/test/resources/forge_tests";
        List<String> copyDirsAndFiles = new ArrayList<>(Arrays.asList(
                "config",
                "mods",
                "scripts",
                "seeds",
                "defaultconfigs",
                "test.txt;test.txt",
                "test2.txt;test2.txt"
        ));
        Assertions.assertTrue(CONFIGURATIONHANDLER.checkCopyDirs(copyDirsAndFiles, modpackDir));
    }

    @Test
    void checkCopyDirsTestFilesFalse() {
        String modpackDir = "backend/test/resources/forge_tests";
        List<String> copyDirsAndFilesFalse = new ArrayList<>(Arrays.asList(
                "configs",
                "modss",
                "scriptss",
                "seedss",
                "defaultconfigss",
                "READMEee.md;README.md",
                "LICENSEee;LICENSE",
                "LICENSEee;test/LICENSE",
                "LICENSEee;test/license.md"
        ));
        Assertions.assertFalse(CONFIGURATIONHANDLER.checkCopyDirs(copyDirsAndFilesFalse, modpackDir));
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
        Assertions.assertNotNull(CONFIGURATIONHANDLER.checkJavaPath(javaPath));
        Assertions.assertTrue(new File(CONFIGURATIONHANDLER.checkJavaPath(javaPath)).exists());
    }

    @Test
    void checkModloaderTestForge() {
        Assertions.assertTrue(CONFIGURATIONHANDLER.checkModloader("Forge"));
    }

    @Test
    void checkModloaderTestForgeCase() {
        Assertions.assertTrue(CONFIGURATIONHANDLER.checkModloader("fOrGe"));
    }

    @Test
    void checkModloaderTestFabric() {
        Assertions.assertTrue(CONFIGURATIONHANDLER.checkModloader("Fabric"));
    }

    @Test
    void checkModloaderTestFabricCase() {
        Assertions.assertTrue(CONFIGURATIONHANDLER.checkModloader("fAbRiC"));
    }

    @Test
    void checkModLoaderTestFalse() {
        Assertions.assertFalse(CONFIGURATIONHANDLER.checkModloader("modloader"));
    }

    @Test
    void setModLoaderCaseTestForge() {
        Assertions.assertEquals("Forge", CONFIGURATIONHANDLER.setModLoaderCase("fOrGe"));
    }

    @Test
    void setModLoaderCaseTestFabric() {
        Assertions.assertEquals("Fabric", CONFIGURATIONHANDLER.setModLoaderCase("fAbRiC"));
    }

    @Test
    void setModLoaderCaseTestForgeCorrected() {
        Assertions.assertEquals("Forge", CONFIGURATIONHANDLER.setModLoaderCase("eeeeefOrGeeeeee"));
    }

    @Test
    void setModLoaderCaseTestFabricCorrected() {
        Assertions.assertEquals("Fabric", CONFIGURATIONHANDLER.setModLoaderCase("hufwhafasfabricfagrsg"));
    }

    @Test
    void checkModloaderVersionTestForge() {
        Assertions.assertTrue(CONFIGURATIONHANDLER.checkModloaderVersion("Forge", "36.1.2", "1.16.5"));
    }

    @Test
    void checkModloaderVersionTestForgeFalse() {
        Assertions.assertFalse(CONFIGURATIONHANDLER.checkModloaderVersion("Forge", "90.0.0", "1.16.5"));
    }

    @Test
    void checkModloaderVersionTestFabric() {
        Assertions.assertTrue(CONFIGURATIONHANDLER.checkModloaderVersion("Fabric", "0.11.3", "1.16.5"));
    }

    @Test
    void checkModloaderVersionTestFabricFalse() {
        Assertions.assertFalse(CONFIGURATIONHANDLER.checkModloaderVersion("Fabric", "0.90.3", "1.16.5"));
    }

    @Test
    void isMinecraftVersionCorrectTest() {
        Assertions.assertTrue(CONFIGURATIONHANDLER.isMinecraftVersionCorrect("1.16.5"));
    }

    @Test
    void isMinecraftVersionCorrectTestFalse() {
        Assertions.assertFalse(CONFIGURATIONHANDLER.isMinecraftVersionCorrect("1.99.5"));
    }

    @Test
    void isFabricVersionCorrectTest() {
        Assertions.assertTrue(CONFIGURATIONHANDLER.isFabricVersionCorrect("0.11.3"));
    }

    @Test
    void isFabricVersionCorrectTestFalse() {
        Assertions.assertFalse(CONFIGURATIONHANDLER.isFabricVersionCorrect("0.90.3"));
    }

    @Test
    void isForgeVersionCorrectTest() {
        Assertions.assertTrue(CONFIGURATIONHANDLER.isForgeVersionCorrect("36.1.2", "1.16.5"));
    }

    @Test
    void isForgeVersionCorrectTestFalse() {
        Assertions.assertFalse(CONFIGURATIONHANDLER.isForgeVersionCorrect("99.0.0","1.16.5"));
    }

    @Test
    void buildStringTest() {
        List<String> args = new ArrayList<>(Arrays.asList(
                "config",
                "mods",
                "scripts",
                "seeds",
                "defaultconfigs"
        ));
        String result = CONFIGURATIONHANDLER.buildString(args.toString());
        Assertions.assertEquals(args.toString(), String.format("[%s]",result));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void writeConfigToFileTestForge() {
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

        String javaPath;
        String autoJavaPath = System.getProperty("java.home").replace("\\", "/") + "/bin/java";

        if (autoJavaPath.startsWith("C:")) {autoJavaPath = String.format("%s.exe", autoJavaPath);}
        if (new File("/usr/bin/java").exists()) {javaPath = "/usr/bin/java";} else {javaPath = autoJavaPath;}

        String javaArgs = "tf3g4jz89agz843fag8z49a3zg8ap3jg8zap9vagv3z8j";

        Assertions.assertTrue(CONFIGURATIONHANDLER.writeConfigToFile(
                "./backend/test/resources/forge_tests",
                clientMods,
                copyDirs,
                true,
                javaPath,
                "1.16.5",
                "Forge",
                "36.1.2",
                true,
                true,
                true,
                true,
                javaArgs,
                new File("./serverpackcreatorforge.conf"),
                false
        ));
        Assertions.assertTrue(new File("./serverpackcreatorforge.conf").exists());
        new File("./serverpackcreatorforge.conf").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void writeConfigToFileTestFabric() {
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

        String javaPath;
        String autoJavaPath = System.getProperty("java.home").replace("\\", "/") + "/bin/java";

        if (autoJavaPath.startsWith("C:")) {autoJavaPath = String.format("%s.exe", autoJavaPath);}
        if (new File("/usr/bin/java").exists()) {javaPath = "/usr/bin/java";} else {javaPath = autoJavaPath;}

        String javaArgs = "tf3g4jz89agz843fag8z49a3zg8ap3jg8zap9vagv3z8j";

        Assertions.assertTrue(CONFIGURATIONHANDLER.writeConfigToFile(
                "./backend/test/resources/fabric_tests",
                clientMods,
                copyDirs,
                true,
                javaPath,
                "1.16.5",
                "Fabric",
                "0.11.3",
                true,
                true,
                true,
                true,
                javaArgs,
                new File("./serverpackcreatorfabric.conf"),
                false
        ));
        Assertions.assertTrue(new File("./serverpackcreatorfabric.conf").exists());
        new File("./serverpackcreatorfabric.conf").delete();
    }

    @Test
    void checkConfigModelTest() {
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
        ConfigurationModel configurationModel = new ConfigurationModel();
        configurationModel.setModpackDir("./backend/test/resources/forge_tests");
        configurationModel.setClientMods(clientMods);
        configurationModel.setCopyDirs(copyDirs);
        configurationModel.setJavaPath("");
        configurationModel.setIncludeServerInstallation(true);
        configurationModel.setIncludeServerIcon(true);
        configurationModel.setIncludeServerProperties(true);
        configurationModel.setIncludeStartScripts(true);
        configurationModel.setIncludeZipCreation(true);
        configurationModel.setModLoader("Forge");
        configurationModel.setModLoaderVersion("36.1.2");
        configurationModel.setMinecraftVersion("1.16.5");
        Assertions.assertFalse(CONFIGURATIONHANDLER.checkConfiguration(false, configurationModel));
    }

    @Test
    void encapsulateListElementsTest() {
        List<String> clientMods = new ArrayList<>(Arrays.asList(
                "A[mbient]Sounds",
                "Back[Tools",
                "Bett[er[][]Advancement",
                "Bett   erPing",
                "cheri[ ]shed",
                "ClientT&/$weaks",
                "Control§!%(?=)ling",
                "Defau/()&=?ltOptions",
                "durabi!§/&?lity",
                "DynamicS[]urroundings",
                "itemz\\oom",
                "jei-/($?professions",
                "jeiinteg}][ration",
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
        Assertions.assertNotNull(CONFIGURATIONHANDLER.encapsulateListElements(clientMods));
    }
}
