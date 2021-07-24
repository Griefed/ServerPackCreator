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

import com.therandomlabs.curseapi.CurseAPI;
import com.therandomlabs.curseapi.CurseException;
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
import java.net.URL;
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
 * 6. {@link #getsetClientModsTest()}<br>
 * 7. {@link #getsetCopyDirsTest()}<br>
 * 8. {@link #getsetModpackDirTest()}<br>
 * 9. {@link #getsetJavaPathTest()}<br>
 * 10.{@link #getsetMinecraftVersionTest()}<br>
 * 11.{@link #getsetModLoaderTest()}<br>
 * 12.{@link #getsetModLoaderVersionTest()}<br>
 * 13.{@link #getsetIncludeServerInstallationTest()}<br>
 * 14.{@link #getsetIncludeServerIconTest()}<br>
 * 15.{@link #getsetIncludeServerPropertiesTest()}<br>
 * 16.{@link #getsetIncludeStartScriptsTest()}<br>
 * 17.{@link #getsetIncludeZipCreationTest()}<br>
 * 18.{@link #getsetProjectIDTest()}<br>
 * 19.{@link #getsetProjectFileIDTest()}<br>
 * 20.{@link #checkConfigFileTest()}<br>
 * 21.{@link #isDirTest()}<br>
 * 22.{@link #isCurseTest()}<br>
 * 23.{@link #containsFabricTest()}<br>
 * 24.{@link #suggestCopyDirsTest()}<br>
 * 25.{@link #checkCurseForgeTest()}<br>
 * 26.{@link #convertToBooleanTest()}<br>
 * 27.{@link #printConfigTest()}<br>
 * 28.{@link #checkModpackDirTest()}<br>
 * 29.{@link #checkCopyDirsTest()}<br>
 * 30.{@link #getJavaPathFromSystemTest()}<br>
 * 31.{@link #checkJavaPathTest()}<br>
 * 32.{@link #checkModloaderTest()}<br>
 * 33.{@link #setModLoaderCaseTest()}<br>
 * 34.{@link #checkModloaderVersionTest()}<br>
 * 35.{@link #isMinecraftVersionCorrectTest()}<br>
 * 36.{@link #isFabricVersionCorrectTest()}<br>
 * 37.{@link #isForgeVersionCorrectTest()}<br>
 * 38.{@link #latestFabricLoaderTest()}<br>
 * 39.{@link #buildStringTest()}<br>
 * 40.{@link #writeConfigToFileTest()}
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
        Assertions.assertNotNull(configuration.getClientMods());
        Assertions.assertEquals(clientMods, configuration.getClientMods());
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
        Assertions.assertNotNull(configuration.getCopyDirs());
        Assertions.assertEquals(getList, configuration.getCopyDirs());
        Assertions.assertFalse(configuration.getCopyDirs().contains("server_pack"));
    }

    @Test
    void getsetModpackDirTest() {
        String modpackDir = "backend/test/resources/forge_tests";
        configuration.setModpackDir(modpackDir);
        Assertions.assertNotNull(configuration.getModpackDir());
        Assertions.assertEquals(modpackDir, configuration.getModpackDir());

        String modpackDir2 = "backend\\test\\resources\\forge_tests";
        configuration.setModpackDir(modpackDir2);
        Assertions.assertNotNull(configuration.getModpackDir());
        Assertions.assertNotEquals(modpackDir2, configuration.getModpackDir());
        Assertions.assertEquals(modpackDir2.replace("\\","/"),configuration.getModpackDir());
        Assertions.assertFalse(configuration.getModpackDir().contains("\\"));
    }

    @Test
    void getsetJavaPathTest() {
        String javaPath = "backend/test/resources/forge_tests";
        configuration.setJavaPath(javaPath);
        Assertions.assertNotNull(configuration.getJavaPath());
        Assertions.assertEquals(javaPath, configuration.getJavaPath());

        String javaPath2 = "backend\\test\\resources\\forge_tests";
        configuration.setModpackDir(javaPath2);
        Assertions.assertNotNull(configuration.getJavaPath());
        Assertions.assertNotEquals(javaPath2, configuration.getJavaPath());
        Assertions.assertEquals(javaPath2.replace("\\","/"),configuration.getJavaPath());
        Assertions.assertFalse(configuration.getJavaPath().contains("\\"));
    }

    @Test
    void getsetMinecraftVersionTest() {
        String minecraftVersion = "1.16.5";
        configuration.setMinecraftVersion(minecraftVersion);
        Assertions.assertNotNull(configuration.getMinecraftVersion());
        Assertions.assertEquals(minecraftVersion, configuration.getMinecraftVersion());
    }

    @Test
    void getsetModLoaderTest() {
        String modloader = "FoRgE";
        configuration.setModLoader(modloader);
        Assertions.assertNotNull(configuration.getModLoader());
        Assertions.assertNotEquals(modloader, configuration.getModLoader());
        Assertions.assertEquals("Forge", configuration.getModLoader());
    }

    @Test
    void getsetModLoaderVersionTest() {
        String modloaderVersion = "36.1.2";
        configuration.setModLoaderVersion(modloaderVersion);
        Assertions.assertNotNull(configuration.getModLoaderVersion());
        Assertions.assertEquals(modloaderVersion, configuration.getModLoaderVersion());
    }

    @Test
    void getsetIncludeServerInstallationTest() {
        configuration.setIncludeServerInstallation(true);
        Assertions.assertTrue(configuration.getIncludeServerInstallation());
        configuration.setIncludeServerInstallation(false);
        Assertions.assertFalse(configuration.getIncludeServerInstallation());
    }

    @Test
    void getsetIncludeServerIconTest() {
        configuration.setIncludeServerIcon(true);
        Assertions.assertTrue(configuration.getIncludeServerIcon());
        configuration.setIncludeServerIcon(false);
        Assertions.assertFalse(configuration.getIncludeServerIcon());
    }

    @Test
    void getsetIncludeServerPropertiesTest() {
        configuration.setIncludeServerProperties(true);
        Assertions.assertTrue(configuration.getIncludeServerProperties());
        configuration.setIncludeServerProperties(false);
        Assertions.assertFalse(configuration.getIncludeServerProperties());
    }

    @Test
    void getsetIncludeStartScriptsTest() {
        configuration.setIncludeStartScripts(true);
        Assertions.assertTrue(configuration.getIncludeStartScripts());
        configuration.setIncludeStartScripts(false);
        Assertions.assertFalse(configuration.getIncludeStartScripts());
    }

    @Test
    void getsetIncludeZipCreationTest() {
        configuration.setIncludeZipCreation(true);
        Assertions.assertTrue(configuration.getIncludeZipCreation());
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

        Files.copy(Paths.get("./backend/test/resources/testresources/serverpackcreator_copydirs.conf"), Paths.get("./serverpackcreator.conf"), REPLACE_EXISTING);
        configuration.setConfig(new File("./serverpackcreator.conf"));
        configuration.setIncludeServerInstallation(true);
        Assertions.assertTrue(configuration.isDir("./backend/test/resources/forge_tests"));
        new File("./serverpackcreator.conf").delete();

        Files.copy(Paths.get("./backend/test/resources/testresources/serverpackcreator_javapath.conf"), Paths.get("./serverpackcreator.conf"), REPLACE_EXISTING);
        configuration.setConfig(new File("./serverpackcreator.conf"));
        configuration.setIncludeServerInstallation(true);
        Assertions.assertTrue(configuration.isDir("./backend/test/resources/forge_tests"));
        new File("./serverpackcreator.conf").delete();

        Files.copy(Paths.get("./backend/test/resources/testresources/serverpackcreator_minecraftversion.conf"), Paths.get("./serverpackcreator.conf"), REPLACE_EXISTING);
        configuration.setConfig(new File("./serverpackcreator.conf"));
        configuration.setIncludeServerInstallation(true);
        Assertions.assertTrue(configuration.isDir("./backend/test/resources/forge_tests"));
        new File("./serverpackcreator.conf").delete();

        Files.copy(Paths.get("./backend/test/resources/testresources/serverpackcreator_modloader.conf"), Paths.get("./serverpackcreator.conf"), REPLACE_EXISTING);
        configuration.setConfig(new File("./serverpackcreator.conf"));
        configuration.setIncludeServerInstallation(true);
        Assertions.assertTrue(configuration.isDir("./backend/test/resources/forge_tests"));
        new File("./serverpackcreator.conf").delete();

        Files.copy(Paths.get("./backend/test/resources/testresources/serverpackcreator_modloaderversion.conf"), Paths.get("./serverpackcreator.conf"), REPLACE_EXISTING);
        configuration.setConfig(new File("./serverpackcreator.conf"));
        configuration.setIncludeServerInstallation(true);
        Assertions.assertTrue(configuration.isDir("./backend/test/resources/forge_tests"));
        new File("./serverpackcreator.conf").delete();
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored", "OptionalGetWithoutIsPresent"})
    @Test
    void isCurseTest() throws CurseException, IOException {
        Files.copy(Paths.get("./backend/test/resources/testresources/serverpackcreator.conf"), Paths.get("./serverpackcreator.conf"), REPLACE_EXISTING);
        configuration.setConfig(new File("./serverpackcreator.conf"));
        configuration.setProjectID(238298);
        configuration.setProjectFileID(3174854);
        String projectName, displayName;
        projectName = CurseAPI.project(238298).get().name();
        displayName = CurseAPI.project(238298).get().files().fileWithID(3174854).displayName();
        configuration.setClientMods(configuration.getFallbackModsList());
        configuration.setIncludeServerInstallation(true);
        configuration.setIncludeServerIcon(true);
        configuration.setIncludeServerProperties(true);
        configuration.setIncludeStartScripts(true);
        configuration.setIncludeZipCreation(true);
        Assertions.assertFalse(configuration.isCurse());
        if (new File(String.format("./%s/%s",projectName,displayName)).isDirectory()) {
            Path pathToBeDeleted = Paths.get(String.format("./%s/%s",projectName,displayName));
            Files.walk(pathToBeDeleted).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
        new File("./serverpackcreator.conf").delete();

        configuration.setProjectID(999999);
        Assertions.assertTrue(configuration.isCurse());
        new File("./serverpackcreator.conf").delete();

        Files.copy(Paths.get("./backend/test/resources/testresources/serverpackcreator.conf"), Paths.get("./serverpackcreator.conf"), REPLACE_EXISTING);
        configuration.setConfig(new File("./serverpackcreator.conf"));
        configuration.setProjectID(238298);
        configuration.setProjectFileID(999999);
        Assertions.assertTrue(configuration.isCurse());

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

    @Test
    void containsFabricTest() throws IOException {
        byte[] fabricJsonData = Files.readAllBytes(Paths.get("backend/test/resources/testresources/fabric_manifest.json"));
        CurseModpack fabricModpack = configuration.getObjectMapper().readValue(fabricJsonData, CurseModpack.class);
        Assertions.assertTrue(configuration.containsFabric(fabricModpack));

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
    void checkCurseForgeTest() {
        String valid = "430517,3266321";
        Assertions.assertTrue(configuration.checkCurseForge(valid));
        String invalid = "1,1234";
        Assertions.assertFalse(configuration.checkCurseForge(invalid));
    }

    @Test
    void convertToBooleanTest() {
        Assertions.assertTrue(configuration.convertToBoolean("True"));
        Assertions.assertTrue(configuration.convertToBoolean("true"));
        Assertions.assertTrue(configuration.convertToBoolean("1"));
        Assertions.assertTrue(configuration.convertToBoolean("Yes"));
        Assertions.assertTrue(configuration.convertToBoolean("yes"));
        Assertions.assertTrue(configuration.convertToBoolean("Y"));
        Assertions.assertTrue(configuration.convertToBoolean("y"));

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
        Assertions.assertFalse(configuration.checkModpackDir("modpackDir"));
    }


    @Test
    void checkCopyDirsTest() {
        //Assert true
        String modpackDir = "backend/test/resources/forge_tests";
        List<String> copyDirs = new ArrayList<>(Arrays.asList(
                "config",
                "mods",
                "scripts",
                "seeds",
                "defaultconfigs"
        ));
        Assertions.assertTrue(configuration.checkCopyDirs(copyDirs, modpackDir));

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


        //Assert false
        List<String> copyDirsInvalid = new ArrayList<>(Arrays.asList(
                "configs",
                "modss",
                "scriptss",
                "seedss",
                "defaultconfigss"
        ));
        Assertions.assertFalse(configuration.checkCopyDirs(copyDirsInvalid, modpackDir));

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
        String result = configuration.getJavaPathFromSystem("");
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
    void checkModloaderTest() {
        String Fabric = "Fabric";
        Assertions.assertTrue(configuration.checkModloader(Fabric));
        String Forge = "Forge";
        Assertions.assertTrue(configuration.checkModloader(Forge));
        Assertions.assertFalse(configuration.checkModloader("modloader"));
        Assertions.assertEquals("Fabric", configuration.setModLoaderCase("fAbRiC"));
        Assertions.assertEquals("Forge", configuration.setModLoaderCase("fOrGe"));
    }

    @Test
    void setModLoaderCaseTest() {
        String forge = "fOrGe";
        String fabric = "fAbRiC";
        Assertions.assertEquals("Forge", configuration.setModLoaderCase(forge));
        Assertions.assertEquals("Fabric", configuration.setModLoaderCase(fabric));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void checkModloaderVersionTest() {
        Assertions.assertTrue(configuration.checkModloaderVersion("Fabric", "0.11.3"));
        Assertions.assertFalse(configuration.checkModloaderVersion("Fabric", "0.90.3"));
        new File("fabric-manifest.xml").delete();

        Assertions.assertTrue(configuration.checkModloaderVersion("Forge", "36.1.2"));
        Assertions.assertFalse(configuration.checkModloaderVersion("Forge", "90.0.0"));
        new File("forge-manifest.json").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void isMinecraftVersionCorrectTest() {
        Assertions.assertTrue(configuration.isMinecraftVersionCorrect("1.16.5"));
        Assertions.assertFalse(configuration.isMinecraftVersionCorrect("1.99.5"));
        new File("mcmanifest.json").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void isFabricVersionCorrectTest() {
        Assertions.assertTrue(configuration.isFabricVersionCorrect("0.11.3"));
        Assertions.assertFalse(configuration.isFabricVersionCorrect("0.90.3"));
        new File("fabric-manifest.xml").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void isForgeVersionCorrectTest() {
        Assertions.assertTrue(configuration.isForgeVersionCorrect("36.1.2"));
        Assertions.assertFalse(configuration.isForgeVersionCorrect("99.0.0"));
        new File("forge-manifest.json").delete();
    }

    @Test
    void latestFabricLoaderTest() {
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
    void writeConfigToFileTest() {
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

        if (autoJavaPath.startsWith("C:")) {
            autoJavaPath = String.format("%s.exe", autoJavaPath);
        }
        if (new File("/usr/bin/java").exists()) {
            javaPath = "/usr/bin/java";
        } else {
            javaPath = autoJavaPath;
        }

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

}
