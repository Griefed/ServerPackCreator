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

import de.griefed.serverpackcreator.curseforgemodpack.CurseCreateModpack;
import de.griefed.serverpackcreator.curseforgemodpack.CurseModpack;
import de.griefed.serverpackcreator.i18n.LocalizationManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.io.File;
import java.io.IOException;
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
 * 67.{@link #getJavaPathFromSystemTest()}<br>
 * 68.{@link #checkJavaPathTest()}<br>
 * 69.{@link #checkModloaderTestForge()}<br>
 * 70.{@link #checkModloaderTestForgeCase()}<br>
 * 71.{@link #checkModloaderTestFabric()}<br>
 * 72.{@link #checkModloaderTestFabricCase()}<br>
 * 73.{@link #checkModLoaderTestFalse()}<br>
 * 74.{@link #setModLoaderCaseTestForge()}<br>
 * 75.{@link #setModLoaderCaseTestFabric()}<br>
 * 76.{@link #setModLoaderCaseTestForgeCorrected()}<br>
 * 77.{@link #setModLoaderCaseTestFabricCorrected()}<br>
 * 78.{@link #checkModloaderVersionTestForge()}<br>
 * 79.{@link #checkModloaderVersionTestForgeFalse()}<br>
 * 80.{@link #checkModloaderVersionTestFabric()}<br>
 * 81.{@link #checkModloaderVersionTestFabricFalse()}<br>
 * 82.{@link #isMinecraftVersionCorrectTest()}<br>
 * 83.{@link #isMinecraftVersionCorrectTestFalse()}<br>
 * 84.{@link #isFabricVersionCorrectTest()}<br>
 * 85.{@link #isFabricVersionCorrectTestFalse()}<br>
 * 86.{@link #isForgeVersionCorrectTest()}<br>
 * 87.{@link #isForgeVersionCorrectTestFalse()}<br>
 * 88.{@link #latestFabricLoaderTest()}<br>
 * 89.{@link #latestFabricLoaderTestNotNull()}<br>
 * 90.{@link #buildStringTest()}<br>
 * 91.{@link #writeConfigToFileTestForge()}<br>
 * 92.{@link #writeConfigToFileTestFabric()}
 */
class ConfigurationTest {
    @Mock
    Logger appLogger;

    private final Configuration configuration;
    private final CurseCreateModpack curseCreateModpack;
    private final LocalizationManager localizationManager;
    private final DefaultFiles defaultFiles;

    ConfigurationTest() {
        localizationManager = new LocalizationManager();
        defaultFiles = new DefaultFiles(localizationManager);
        curseCreateModpack = new CurseCreateModpack(localizationManager);
        configuration = new Configuration(localizationManager, curseCreateModpack);
    }

    @BeforeEach
    void setUp() {
        localizationManager.checkLocaleFile();
        defaultFiles.filesSetup();
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getOldConfigFileTest() {
        File file = configuration.getOldConfigFile();
        Assertions.assertNotNull(file);
    }

    @Test
    void getConfigFileTest() {
        File file = configuration.getConfigFile();
        Assertions.assertNotNull(file);
    }

    @Test
    void getsetConfigTest() {
        File file = new File("backend/test/resources/testresources/serverpackcreator.conf");
        configuration.setConfig(file);
        Assertions.assertNotNull(configuration.getConfig());
    }

    @Test
    void getFallbackModsListTest() {
        Assertions.assertNotNull(configuration.getFallbackModsList());
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
        Assertions.assertEquals(fallbackMods, configuration.getFallbackModsList());
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
        configuration.setClientMods(clientMods);
        Assertions.assertEquals(clientMods, configuration.getClientMods());
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
        configuration.setClientMods(clientMods);
        Assertions.assertNotNull(configuration.getClientMods());
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
        configuration.setCopyDirs(testList);
        Assertions.assertEquals(getList, configuration.getCopyDirs());
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
        configuration.setCopyDirs(testList);
        Assertions.assertNotNull(configuration.getCopyDirs());
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
        configuration.setCopyDirs(testList);
        Assertions.assertFalse(configuration.getCopyDirs().contains("server_pack"));
    }

    @Test
    void getsetModpackDirTest() {
        String modpackDir = "backend/test/resources/forge_tests";
        configuration.setModpackDir(modpackDir);
        Assertions.assertEquals(modpackDir, configuration.getModpackDir());
    }

    @Test
    void getsetModpackDirTestNotNull() {
        String modpackDir = "backend/test/resources/forge_tests";
        configuration.setModpackDir(modpackDir);
        Assertions.assertNotNull(configuration.getModpackDir());
    }

    @Test
    void getsetModpackDirTestBackslash() {
        String modpackDir = "backend\\test\\resources\\forge_tests";
        configuration.setModpackDir(modpackDir);
        Assertions.assertEquals(modpackDir.replace("\\","/"),configuration.getModpackDir());
    }

    @Test
    void getsetModpackDirTestBackslashFalse() {
        String modpackDir = "backend\\test\\resources\\forge_tests";
        configuration.setModpackDir(modpackDir);
        Assertions.assertFalse(configuration.getModpackDir().contains("\\"));
    }

    @Test
    void getsetModpackDirTestBackslashNotNull() {
        String modpackDir = "backend\\test\\resources\\forge_tests";
        configuration.setModpackDir(modpackDir);
        Assertions.assertNotNull(configuration.getModpackDir());
    }

    @Test
    void getsetJavaPathTest() {
        String javaPath = "backend/test/resources/forge_tests";
        configuration.setJavaPath(javaPath);
        Assertions.assertEquals(javaPath, configuration.getJavaPath());
    }

    @Test
    void getsetJavaPathTestNotNull() {
        String javaPath = "backend/test/resources/forge_tests";
        configuration.setJavaPath(javaPath);
        Assertions.assertNotNull(configuration.getJavaPath());
    }

    @Test
    void getsetJavaPathTestBackslash() {
        String javaPath = "backend\\test\\resources\\forge_tests";
        configuration.setJavaPath(javaPath);
        Assertions.assertEquals(javaPath.replace("\\","/"),configuration.getJavaPath());
    }

    @Test
    void getsetJavaPathTestBackslashNotNull() {
        String javaPath = "backend\\test\\resources\\forge_tests";
        configuration.setJavaPath(javaPath);
        Assertions.assertNotNull(configuration.getJavaPath());
    }

    @Test
    void getsetJavaPathTestBackslashNotEquals() {
        String javaPath = "backend\\test\\resources\\forge_tests";
        configuration.setJavaPath(javaPath);
        Assertions.assertNotEquals(javaPath, configuration.getJavaPath());
    }

    @Test
    void getsetJavaPathTestBackslashFalse() {
        String javaPath = "backend\\test\\resources\\forge_tests";
        configuration.setJavaPath(javaPath);
        Assertions.assertFalse(configuration.getJavaPath().contains("\\"));
    }

    @Test
    void getsetMinecraftVersionTest() {
        String minecraftVersion = "1.16.5";
        configuration.setMinecraftVersion(minecraftVersion);
        Assertions.assertEquals(minecraftVersion, configuration.getMinecraftVersion());
    }

    @Test
    void getsetMinecraftVersionTestNotNull() {
        String minecraftVersion = "1.16.5";
        configuration.setMinecraftVersion(minecraftVersion);
        Assertions.assertNotNull(configuration.getMinecraftVersion());
    }

    @Test
    void getsetModLoaderTest() {
        String modloader = "FoRgE";
        configuration.setModLoader(modloader);
        Assertions.assertEquals("Forge", configuration.getModLoader());
    }

    @Test
    void getsetModLoaderTestNotNull() {
        String modloader = "FoRgE";
        configuration.setModLoader(modloader);
        Assertions.assertNotNull(configuration.getModLoader());
    }

    @Test
    void getsetModLoaderTestNotEquals() {
        String modloader = "FoRgE";
        configuration.setModLoader(modloader);
        Assertions.assertNotEquals(modloader, configuration.getModLoader());
    }

    @Test
    void getsetModLoaderVersionTest() {
        String modloaderVersion = "36.1.2";
        configuration.setModLoaderVersion(modloaderVersion);
        Assertions.assertEquals(modloaderVersion, configuration.getModLoaderVersion());
    }

    @Test
    void getsetModLoaderVersionTestNotNull() {
        String modloaderVersion = "36.1.2";
        configuration.setModLoaderVersion(modloaderVersion);
        Assertions.assertNotNull(configuration.getModLoaderVersion());
    }

    @Test
    void getsetIncludeServerInstallationTest() {
        configuration.setIncludeServerInstallation(true);
        Assertions.assertTrue(configuration.getIncludeServerInstallation());
    }

    @Test
    void getsetIncludeServerInstallationTestFalse() {
        configuration.setIncludeServerInstallation(false);
        Assertions.assertFalse(configuration.getIncludeServerInstallation());
    }

    @Test
    void getsetIncludeServerIconTest() {
        configuration.setIncludeServerIcon(true);
        Assertions.assertTrue(configuration.getIncludeServerIcon());
    }

    @Test
    void getsetIncludeServerIconTestFalse() {
        configuration.setIncludeServerIcon(false);
        Assertions.assertFalse(configuration.getIncludeServerIcon());
    }

    @Test
    void getsetIncludeServerPropertiesTest() {
        configuration.setIncludeServerProperties(true);
        Assertions.assertTrue(configuration.getIncludeServerProperties());
    }

    @Test
    void getsetIncludeServerPropertiesTestFalse() {
        configuration.setIncludeServerProperties(false);
        Assertions.assertFalse(configuration.getIncludeServerProperties());
    }

    @Test
    void getsetIncludeStartScriptsTest() {
        configuration.setIncludeStartScripts(true);
        Assertions.assertTrue(configuration.getIncludeStartScripts());
    }

    @Test
    void getsetIncludeStartScriptsTestFalse() {
        configuration.setIncludeStartScripts(false);
        Assertions.assertFalse(configuration.getIncludeStartScripts());
    }

    @Test
    void getsetIncludeZipCreationTest() {
        configuration.setIncludeZipCreation(true);
        Assertions.assertTrue(configuration.getIncludeZipCreation());
    }

    @Test
    void getsetIncludeZipCreationTestFalse() {
        configuration.setIncludeZipCreation(false);
        Assertions.assertFalse(configuration.getIncludeZipCreation());
    }

    @Test
    void getsetProjectIDTest() {
        int projectID = 123456;
        configuration.setProjectID(projectID);
        Assertions.assertEquals(projectID, configuration.getProjectID());
    }

    @Test
    void getsetProjectFileIDTest() {
        int fileID = 123456;
        configuration.setProjectFileID(fileID);
        Assertions.assertEquals(fileID, configuration.getProjectFileID());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void checkConfigFileTest() throws IOException {
        Files.copy(Paths.get("./backend/test/resources/testresources/serverpackcreator.conf"), Paths.get("./serverpackcreator.conf"), REPLACE_EXISTING);
        configuration.setConfig(new File("./serverpackcreator.conf"));
        boolean result = configuration.checkConfigFile(configuration.getConfigFile(), true);
        Assertions.assertFalse(result);
        new File("./serverpackcreator.conf").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void isDirTest() throws IOException {
        Files.copy(Paths.get("./backend/test/resources/testresources/serverpackcreator.conf"), Paths.get("./serverpackcreator.conf"), REPLACE_EXISTING);
        configuration.setConfig(new File("./serverpackcreator.conf"));
        configuration.setIncludeServerInstallation(true);
        Assertions.assertFalse(configuration.isDir("./backend/test/resources/forge_tests"));
        new File("./serverpackcreator.conf").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void isDirTestCopyDirs() throws IOException {
        Files.copy(Paths.get("./backend/test/resources/testresources/serverpackcreator_copydirs.conf"), Paths.get("./serverpackcreator.conf"), REPLACE_EXISTING);
        configuration.setConfig(new File("./serverpackcreator.conf"));
        configuration.setIncludeServerInstallation(true);
        Assertions.assertTrue(configuration.isDir("./backend/test/resources/forge_tests"));
        new File("./serverpackcreator.conf").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void isDirTestJavaPath() throws IOException {
        Files.copy(Paths.get("./backend/test/resources/testresources/serverpackcreator_javapath.conf"), Paths.get("./serverpackcreator.conf"), REPLACE_EXISTING);
        configuration.setConfig(new File("./serverpackcreator.conf"));
        configuration.setIncludeServerInstallation(true);
        Assertions.assertFalse(configuration.isDir("./backend/test/resources/forge_tests"));
        new File("./serverpackcreator.conf").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void isDirTestMinecraftVersion() throws IOException {
        Files.copy(Paths.get("./backend/test/resources/testresources/serverpackcreator_minecraftversion.conf"), Paths.get("./serverpackcreator.conf"), REPLACE_EXISTING);
        configuration.setConfig(new File("./serverpackcreator.conf"));
        configuration.setIncludeServerInstallation(true);
        Assertions.assertTrue(configuration.isDir("./backend/test/resources/forge_tests"));
        new File("./serverpackcreator.conf").delete();

    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void isDirTestModLoader() throws IOException {
        Files.copy(Paths.get("./backend/test/resources/testresources/serverpackcreator_modloader.conf"), Paths.get("./serverpackcreator.conf"), REPLACE_EXISTING);
        configuration.setConfig(new File("./serverpackcreator.conf"));
        configuration.setIncludeServerInstallation(true);
        Assertions.assertFalse(configuration.isDir("./backend/test/resources/forge_tests"));
        new File("./serverpackcreator.conf").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void isDirTestModLoaderFalse() throws IOException {
        Files.copy(Paths.get("./backend/test/resources/testresources/serverpackcreator_modloaderfalse.conf"), Paths.get("./serverpackcreator.conf"), REPLACE_EXISTING);
        configuration.setConfig(new File("./serverpackcreator.conf"));
        configuration.setIncludeServerInstallation(true);
        Assertions.assertTrue(configuration.isDir("./backend/test/resources/forge_tests"));
        new File("./serverpackcreator.conf").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void isDirTestModLoaderVersion() throws IOException {
        Files.copy(Paths.get("./backend/test/resources/testresources/serverpackcreator_modloaderversion.conf"), Paths.get("./serverpackcreator.conf"), REPLACE_EXISTING);
        configuration.setConfig(new File("./serverpackcreator.conf"));
        configuration.setIncludeServerInstallation(true);
        Assertions.assertTrue(configuration.isDir("./backend/test/resources/forge_tests"));
        new File("./serverpackcreator.conf").delete();
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    @Test
    void isCurseTest() throws IOException {
        Files.copy(Paths.get("./backend/test/resources/testresources/serverpackcreator_curseforge.conf"), Paths.get("./serverpackcreator.conf"), REPLACE_EXISTING);
        configuration.setConfig(new File("./serverpackcreator.conf"));
        configuration.setIncludeServerInstallation(true);
        configuration.setIncludeServerIcon(true);
        configuration.setIncludeServerProperties(true);
        configuration.setIncludeStartScripts(true);
        configuration.setIncludeZipCreation(true);
        configuration.checkCurseForge("238298,3174854");
        configuration.setClientMods(configuration.getFallbackModsList());
        Assertions.assertFalse(configuration.isCurse());
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
        configuration.setConfig(new File("./serverpackcreator.conf"));
        configuration.setIncludeServerInstallation(true);
        configuration.setIncludeServerIcon(true);
        configuration.setIncludeServerProperties(true);
        configuration.setIncludeStartScripts(true);
        configuration.setIncludeZipCreation(true);
        configuration.checkCurseForge("999999,3174854");
        configuration.setClientMods(configuration.getFallbackModsList());
        Assertions.assertTrue(configuration.isCurse());
        new File("./serverpackcreator.conf").delete();
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    @Test
    void isCurseTestProjectFileIDFalse() throws IOException {
        Files.copy(Paths.get("./backend/test/resources/testresources/serverpackcreator_curseforgefilefalse.conf"), Paths.get("./serverpackcreator.conf"), REPLACE_EXISTING);
        configuration.setConfig(new File("./serverpackcreator.conf"));
        configuration.setProjectID(238298);
        configuration.setProjectFileID(999999);
        Assertions.assertTrue(configuration.isCurse());
        new File("./serverpackcreator.conf").delete();
    }

    @Test
    void containsFabricTest() throws IOException {
        byte[] fabricJsonData = Files.readAllBytes(Paths.get("backend/test/resources/testresources/fabric_manifest.json"));
        CurseModpack fabricModpack = configuration.getObjectMapper().readValue(fabricJsonData, CurseModpack.class);
        Assertions.assertTrue(configuration.containsFabric(fabricModpack));
    }

    @Test
    void containsFabricTestFalse() throws IOException {
        byte[] forgeJsonData = Files.readAllBytes(Paths.get("backend/test/resources/testresources/manifest.json"));
        CurseModpack forgeModpack = configuration.getObjectMapper().readValue(forgeJsonData, CurseModpack.class);
        Assertions.assertFalse(configuration.containsFabric(forgeModpack));
    }

    @Test
    void suggestCopyDirsTest() {
        String modpackDir = "backend/test/resources/forge_tests";
        Assertions.assertFalse(configuration.suggestCopyDirs(modpackDir).contains("server_pack"));
        Assertions.assertTrue(configuration.suggestCopyDirs(modpackDir).contains("config"));
        Assertions.assertTrue(configuration.suggestCopyDirs(modpackDir).contains("defaultconfigs"));
        Assertions.assertTrue(configuration.suggestCopyDirs(modpackDir).contains("mods"));
        Assertions.assertTrue(configuration.suggestCopyDirs(modpackDir).contains("scripts"));
        Assertions.assertTrue(configuration.suggestCopyDirs(modpackDir).contains("seeds"));

    }

    @Test
    void suggestCopyDirsTestFalse() {
        String modpackDir = "backend/test/resources/forge_tests";
        Assertions.assertFalse(configuration.suggestCopyDirs(modpackDir).contains("server_pack"));
        Assertions.assertFalse(configuration.suggestCopyDirs(modpackDir).contains("saves"));
        Assertions.assertFalse(configuration.suggestCopyDirs(modpackDir).contains("logs"));
        Assertions.assertFalse(configuration.suggestCopyDirs(modpackDir).contains("resourcepacks"));
        Assertions.assertTrue(configuration.suggestCopyDirs(modpackDir).contains("scripts"));
        Assertions.assertTrue(configuration.suggestCopyDirs(modpackDir).contains("seeds"));
    }

    @Test
    void checkCurseForgeTest() {
        String valid = "430517,3266321";
        Assertions.assertTrue(configuration.checkCurseForge(valid));
    }

    @Test
    void checkCurseForgeTestFalse() {
        String invalid = "1,1234";
        Assertions.assertFalse(configuration.checkCurseForge(invalid));
    }

    @Test
    void convertToBooleanTestTrue() {
        Assertions.assertTrue(configuration.convertToBoolean("True"));
        Assertions.assertTrue(configuration.convertToBoolean("true"));
        Assertions.assertTrue(configuration.convertToBoolean("1"));
        Assertions.assertTrue(configuration.convertToBoolean("Yes"));
        Assertions.assertTrue(configuration.convertToBoolean("yes"));
        Assertions.assertTrue(configuration.convertToBoolean("Y"));
        Assertions.assertTrue(configuration.convertToBoolean("y"));
    }

    @Test
    void convertToBooleanTestFalse() {
        Assertions.assertFalse(configuration.convertToBoolean("False"));
        Assertions.assertFalse(configuration.convertToBoolean("false"));
        Assertions.assertFalse(configuration.convertToBoolean("0"));
        Assertions.assertFalse(configuration.convertToBoolean("No"));
        Assertions.assertFalse(configuration.convertToBoolean("no"));
        Assertions.assertFalse(configuration.convertToBoolean("N"));
        Assertions.assertFalse(configuration.convertToBoolean("n"));
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
        boolean includeServerIcon = true;
        boolean includeServerProperties = true;
        boolean includeStartScripts = true;
        boolean includeZipCreation = true;
        configuration.printConfig(
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
                includeZipCreation);
    }

    @Test
    void checkModpackDirTest() {
        String modpackDirCorrect = "./backend/test/resources/forge_tests";
        Assertions.assertTrue(configuration.checkModpackDir(modpackDirCorrect));
    }

    @Test
    void checkModpackDirTestFalse() {
        Assertions.assertFalse(configuration.checkModpackDir("modpackDir"));
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
        Assertions.assertTrue(configuration.checkCopyDirs(copyDirs, modpackDir));
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
        Assertions.assertFalse(configuration.checkCopyDirs(copyDirsInvalid, modpackDir));
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
        Assertions.assertTrue(configuration.checkCopyDirs(copyDirsAndFiles, modpackDir));
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
        Assertions.assertFalse(configuration.checkCopyDirs(copyDirsAndFilesFalse, modpackDir));
    }

    @Test
    void getJavaPathFromSystemTest() {
        String result = configuration.getJavaPathFromSystem();
        String autoJavaPath = System.getProperty("java.home").replace("\\", "/") + "/bin/java";
        if (autoJavaPath.startsWith("C:")) {
            autoJavaPath = String.format("%s.exe", autoJavaPath);
        }
        Assertions.assertEquals(autoJavaPath, result);
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
        Assertions.assertTrue(configuration.checkJavaPath(javaPath));
    }

    @Test
    void checkModloaderTestForge() {
        Assertions.assertTrue(configuration.checkModloader("Forge"));
    }

    @Test
    void checkModloaderTestForgeCase() {
        Assertions.assertTrue(configuration.checkModloader("fOrGe"));
    }

    @Test
    void checkModloaderTestFabric() {
        Assertions.assertTrue(configuration.checkModloader("Fabric"));
    }

    @Test
    void checkModloaderTestFabricCase() {
        Assertions.assertTrue(configuration.checkModloader("fAbRiC"));
    }

    @Test
    void checkModLoaderTestFalse() {
        Assertions.assertFalse(configuration.checkModloader("modloader"));
    }

    @Test
    void setModLoaderCaseTestForge() {
        Assertions.assertEquals("Forge", configuration.setModLoaderCase("fOrGe"));
    }

    @Test
    void setModLoaderCaseTestFabric() {
        Assertions.assertEquals("Fabric", configuration.setModLoaderCase("fAbRiC"));
    }

    @Test
    void setModLoaderCaseTestForgeCorrected() {
        Assertions.assertEquals("Forge", configuration.setModLoaderCase("eeeeefOrGeeeeee"));
    }

    @Test
    void setModLoaderCaseTestFabricCorrected() {
        Assertions.assertEquals("Fabric", configuration.setModLoaderCase("hufwhafasfabricfagrsg"));
    }

    @Test
    void checkModloaderVersionTestForge() {
        Assertions.assertTrue(configuration.checkModloaderVersion("Forge", "36.1.2"));
    }

    @Test
    void checkModloaderVersionTestForgeFalse() {
        Assertions.assertFalse(configuration.checkModloaderVersion("Forge", "90.0.0"));
    }

    @Test
    void checkModloaderVersionTestFabric() {
        Assertions.assertTrue(configuration.checkModloaderVersion("Fabric", "0.11.3"));
    }

    @Test
    void checkModloaderVersionTestFabricFalse() {
        Assertions.assertFalse(configuration.checkModloaderVersion("Fabric", "0.90.3"));
    }

    @Test
    void isMinecraftVersionCorrectTest() {
        Assertions.assertTrue(configuration.isMinecraftVersionCorrect("1.16.5"));
    }

    @Test
    void isMinecraftVersionCorrectTestFalse() {
        Assertions.assertFalse(configuration.isMinecraftVersionCorrect("1.99.5"));
    }

    @Test
    void isFabricVersionCorrectTest() {
        Assertions.assertTrue(configuration.isFabricVersionCorrect("0.11.3"));
    }

    @Test
    void isFabricVersionCorrectTestFalse() {
        Assertions.assertFalse(configuration.isFabricVersionCorrect("0.90.3"));
    }

    @Test
    void isForgeVersionCorrectTest() {
        Assertions.assertTrue(configuration.isForgeVersionCorrect("36.1.2"));
    }

    @Test
    void isForgeVersionCorrectTestFalse() {
        Assertions.assertFalse(configuration.isForgeVersionCorrect("99.0.0"));
    }

    @Test
    void latestFabricLoaderTest() {
        Assertions.assertTrue(configuration.latestFabricLoader().matches("\\d+\\.\\d+\\.\\d+"));
    }

    @Test
    void latestFabricLoaderTestNotNull() {
        Assertions.assertNotNull(configuration.latestFabricLoader());
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
        String result = configuration.buildString(args.toString());
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

        Assertions.assertTrue(configuration.writeConfigToFile(
                "./backend/test/resources/forge_tests",
                configuration.buildString(clientMods.toString()),
                configuration.buildString(copyDirs.toString()),
                true,
                javaPath,
                "1.16.5",
                "Forge",
                "36.1.2",
                true,
                true,
                true,
                true,
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

        Assertions.assertTrue(configuration.writeConfigToFile(
                "./backend/test/resources/fabric_tests",
                configuration.buildString(clientMods.toString()),
                configuration.buildString(copyDirs.toString()),
                true,
                javaPath,
                "1.16.5",
                "Fabric",
                "0.11.3",
                true,
                true,
                true,
                true,
                new File("./serverpackcreatorfabric.conf"),
                false
        ));
        Assertions.assertTrue(new File("./serverpackcreatorfabric.conf").exists());
        new File("./serverpackcreatorfabric.conf").delete();
    }
}
