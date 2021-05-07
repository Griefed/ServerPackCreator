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
import de.griefed.serverpackcreator.i18n.LocalizationManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * <strong>Table of tests</strong><p>
 * 1. {@link #CreateServerPackTest()}<br>
 * 2. {@link #getPropertiesFileTest()}<br>
 * 3. {@link #getIconFileTest()}<br>
 * 4. {@link #getForgeWindowsFileTest()}<br>
 * 5. {@link #getForgeLinuxFileTest()}<br>
 * 6. {@link #getFabricWindowsFileTest()}<br>
 * 7. {@link #getFabricLinuxFileTest()}<br>
 * 8. {@link #runTest()}<br>
 * 9. {@link #cleanupEnvironmentTest()}<br>
 * 10.{@link #copyStartScriptsFabricTest()}<br>
 * 11.{@link #copyStartScriptsForgeTest()}<br>
 * 12.{@link #copyFilesTest()}<br>
 * 13.{@link #copyFilesEmptyClientsTest()}<br>
 * 14.{@link #excludeClientModsTest()}<br>
 * 15.{@link #copyIconTest()}<br>
 * 16.{@link #copyPropertiesTest()}<br
 * 17.{@link #installServerFabricTest()}<br>
 * 18.{@link #installServerForgeTest()}<br>
 * 19.{@link #zipBuilderFabricTest()}<br>
 * 20.{@link #zipBuilderForgeTest()}<br>
 * 21.{@link #generateDownloadScriptsFabricTest()}<br>
 * 22.{@link #generateDownloadScriptsForgeTest()}<br>
 * 23.{@link #fabricShellTest()}<br>
 * 24.{@link #fabricBatchTest()}<br>
 * 25.{@link #forgeShellTest()}<br>
 * 26.{@link #forgeBatchTest()}<br>
 * 27.{@link #downloadFabricJarTest()}<br>
 * 28.{@link #latestFabricInstallerTest()}<br>
 * 29.{@link #downloadForgeJarTest()}<br>
 * 30.{@link #deleteMinecraftJarFabricTest()}<br>
 * 31.{@link #deleteMinecraftJarForgeTest()}<br>
 * 32.{@link #cleanUpServerPackForgeTest()}
 * 33.{@link #cleanUpServerPackFabricTest()}
 */
class CreateServerPackTest {

    private CreateServerPack createServerPack;
    private FilesSetup filesSetup;
    private CurseCreateModpack curseCreateModpack;
    private LocalizationManager localizationManager;
    private Configuration configuration;

    CreateServerPackTest() {
        localizationManager = new LocalizationManager();
        curseCreateModpack = new CurseCreateModpack(localizationManager);
        configuration = new Configuration(localizationManager, curseCreateModpack);
        createServerPack = new CreateServerPack(localizationManager, configuration, curseCreateModpack);
        filesSetup = new FilesSetup(localizationManager);
    }

    @BeforeEach
    void setUp() {
        localizationManager.checkLocaleFile();
        filesSetup.filesSetup();
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getPropertiesFileTest() {
        File file = createServerPack.getPropertiesFile();
        Assertions.assertNotNull(file);
    }

    @Test
    void getIconFileTest() {
        File file = createServerPack.getIconFile();
        Assertions.assertNotNull(file);
    }

    @Test
    void getForgeWindowsFileTest() {
        File file = createServerPack.getForgeWindowsFile();
        Assertions.assertNotNull(file);
    }

    @Test
    void getForgeLinuxFileTest() {
        File file = createServerPack.getForgeLinuxFile();
        Assertions.assertNotNull(file);
    }

    @Test
    void getFabricWindowsFileTest() {
        File file = createServerPack.getFabricWindowsFile();
        Assertions.assertNotNull(file);
    }

    @Test
    void getFabricLinuxFileTest() {
        File file = createServerPack.getFabricLinuxFile();
        Assertions.assertNotNull(file);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void runTest() throws IOException {
        filesSetup.filesSetup();
        Files.copy(Paths.get("./src/test/resources/testresources/serverpackcreator.conf"), Paths.get("./serverpackcreator.conf"), REPLACE_EXISTING);
        createServerPack.run();
        Assertions.assertTrue(new File("./src/test/resources/forge_tests/server_pack/libraries").isDirectory());
        Assertions.assertTrue(new File("./src/test/resources/forge_tests/server_pack/config").isDirectory());
        Assertions.assertTrue(new File("./src/test/resources/forge_tests/server_pack/defaultconfigs").isDirectory());
        Assertions.assertTrue(new File("./src/test/resources/forge_tests/server_pack/mods").isDirectory());
        Assertions.assertTrue(new File("./src/test/resources/forge_tests/server_pack/scripts").isDirectory());
        Assertions.assertTrue(new File("./src/test/resources/forge_tests/server_pack/seeds").isDirectory());

        Assertions.assertTrue(new File("./src/test/resources/forge_tests/server_pack/1.16.5.json").exists());
        Assertions.assertTrue(new File("./src/test/resources/forge_tests/server_pack/minecraft_server.1.16.5.jar").exists());
        Assertions.assertTrue(new File("./src/test/resources/forge_tests/server_pack/download_minecraft-server.jar_forge.bat").exists());
        Assertions.assertTrue(new File("./src/test/resources/forge_tests/server_pack/download_minecraft-server.jar_forge.sh").exists());
        Assertions.assertTrue(new File("./src/test/resources/forge_tests/server_pack/forge.jar").exists());
        Assertions.assertTrue(new File("./src/test/resources/forge_tests/server_pack/server.properties").exists());
        Assertions.assertTrue(new File("./src/test/resources/forge_tests/server_pack/server-icon.png").exists());
        Assertions.assertTrue(new File("./src/test/resources/forge_tests/server_pack/start-forge.bat").exists());
        Assertions.assertTrue(new File("./src/test/resources/forge_tests/server_pack/start-forge.sh").exists());

        if (new File("./src/test/resources/forge_tests/server_pack/libraries").isDirectory()) {
            Path pathToBeDeleted = Paths.get("./src/test/resources/forge_tests/server_pack/libraries");
            Files.walk(pathToBeDeleted).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
        if (new File("./src/test/resources/forge_tests/server_pack/config").isDirectory()) {
            Path pathToBeDeleted = Paths.get("./src/test/resources/forge_tests/server_pack/config");
            Files.walk(pathToBeDeleted).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
        if (new File("./src/test/resources/forge_tests/server_pack/defaultconfigs").isDirectory()) {
            Path pathToBeDeleted = Paths.get("./src/test/resources/forge_tests/server_pack/defaultconfigs");
            Files.walk(pathToBeDeleted).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
        if (new File("./src/test/resources/forge_tests/server_pack/mods").isDirectory()) {
            Path pathToBeDeleted = Paths.get("./src/test/resources/forge_tests/server_pack/mods");
            Files.walk(pathToBeDeleted).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
        if (new File("./src/test/resources/forge_tests/server_pack/scripts").isDirectory()) {
            Path pathToBeDeleted = Paths.get("./src/test/resources/forge_tests/server_pack/scripts");
            Files.walk(pathToBeDeleted).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
        if (new File("./src/test/resources/forge_tests/server_pack/seeds").isDirectory()) {
            Path pathToBeDeleted = Paths.get("./src/test/resources/forge_tests/server_pack/seeds");
            Files.walk(pathToBeDeleted).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
        if (new File("./server_files").isDirectory()) {
            Path pathToBeDeleted = Paths.get("./server_files");
            Files.walk(pathToBeDeleted).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
        new File("./src/test/resources/forge_tests/server_pack/1.16.5.json").delete();
        new File("./src/test/resources/forge_tests/server_pack/minecraft_server.1.16.5.jar").delete();
        new File("./src/test/resources/forge_tests/server_pack/download_minecraft-server.jar_forge.bat").delete();
        new File("./src/test/resources/forge_tests/server_pack/download_minecraft-server.jar_forge.sh").delete();
        new File("./src/test/resources/forge_tests/server_pack/forge.jar").delete();
        new File("./src/test/resources/forge_tests/server_pack/server.properties").delete();
        new File("./src/test/resources/forge_tests/server_pack/server-icon.png").delete();
        new File("./src/test/resources/forge_tests/server_pack/start-forge.bat").delete();
        new File("./src/test/resources/forge_tests/server_pack/start-forge.sh").delete();
        new File("./serverpackcreator.conf").delete();
        Files.copy(Paths.get("./src/test/resources/testresources/server_pack.zip"), Paths.get("./src/test/resources/forge_tests/server_pack.zip"), REPLACE_EXISTING);
    }

    @Test
    void cleanupEnvironmentTest() throws IOException {
        Files.createDirectories(Paths.get("./src/test/resources/cleanup_tests/server_pack"));
        String modpackDir = "./src/test/resources/cleanup_tests";
        Files.copy(Paths.get("./src/test/resources/testresources/server_pack.zip"), Paths.get("./src/test/resources/cleanup_tests/server_pack.zip"), REPLACE_EXISTING);
        createServerPack.cleanupEnvironment(modpackDir);
        Assertions.assertFalse(new File("\"./src/test/resources/cleanup_tests/server_pack.zip\"").exists());
        Assertions.assertFalse(new File("./src/test/resources/cleanup_tests/server_pack").isDirectory());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void copyStartScriptsFabricTest() throws IOException {
        filesSetup.filesSetup();
        String modpackDir = "./src/test/resources/fabric_tests";
        String modLoader = "Fabric";
        filesSetup.filesSetup();
        createServerPack.copyStartScripts(modpackDir, modLoader, true);
        Assertions.assertTrue(new File(String.format("%s/server_pack/start-fabric.bat", modpackDir)).exists());
        Assertions.assertTrue(new File(String.format("%s/server_pack/start-fabric.sh", modpackDir)).exists());
        new File(String.format("%s/server_pack/start-fabric.bat", modpackDir)).delete();
        new File(String.format("%s/server_pack/start-fabric.sh", modpackDir)).delete();
        String delete = "./server_files";
        if (new File(delete).isDirectory()) {
            Path pathToBeDeleted = Paths.get(delete);
            Files.walk(pathToBeDeleted).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
        new File("./serverpackcreator.conf").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void copyStartScriptsForgeTest() throws IOException {
        filesSetup.filesSetup();
        String modpackDir = "./src/test/resources/forge_tests";
        String modLoader = "Forge";
        filesSetup.filesSetup();
        createServerPack.copyStartScripts(modpackDir, modLoader, true);
        Assertions.assertTrue(new File(String.format("%s/server_pack/start-forge.bat",modpackDir)).exists());
        Assertions.assertTrue(new File(String.format("%s/server_pack/start-forge.sh",modpackDir)).exists());
        new File(String.format("%s/server_pack/start-forge.bat",modpackDir)).delete();
        new File(String.format("%s/server_pack/start-forge.sh",modpackDir)).delete();
        String delete = "./server_files";
        if (new File(delete).isDirectory()) {
            Path pathToBeDeleted = Paths.get(delete);
            Files.walk(pathToBeDeleted)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
        new File("./serverpackcreator.conf").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void copyFilesTest() throws IOException {
        String modpackDir = "./src/test/resources/forge_tests";
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
        createServerPack.copyFiles(modpackDir, copyDirs, clientMods);
        Assertions.assertTrue(new File(String.format("%s/server_pack/config",modpackDir)).isDirectory());
        Assertions.assertTrue(new File(String.format("%s/server_pack/mods",modpackDir)).isDirectory());
        Assertions.assertTrue(new File(String.format("%s/server_pack/scripts",modpackDir)).isDirectory());
        Assertions.assertTrue(new File(String.format("%s/server_pack/seeds",modpackDir)).isDirectory());
        Assertions.assertTrue(new File(String.format("%s/server_pack/defaultconfigs",modpackDir)).isDirectory());
        Assertions.assertTrue(new File(String.format("%s/server_pack/config/testfile.txt",modpackDir)).exists());
        Assertions.assertTrue(new File(String.format("%s/server_pack/defaultconfigs/testfile.txt",modpackDir)).exists());
        Assertions.assertTrue(new File(String.format("%s/server_pack/mods/testmod.jar",modpackDir)).exists());
        Assertions.assertTrue(new File(String.format("%s/server_pack/scripts/testscript.zs",modpackDir)).exists());
        Assertions.assertTrue(new File(String.format("%s/server_pack/seeds/testjson.json",modpackDir)).exists());
        for (String s : copyDirs) {
            String deleteMe = (String.format("%s/server_pack/%s",modpackDir,s));
            if (new File(deleteMe).isDirectory()) {
                Path pathToBeDeleted = Paths.get(deleteMe);
                Files.walk(pathToBeDeleted)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void copyFilesEmptyClientsTest() throws IOException {
        String modpackDir = "./src/test/resources/forge_tests";
        List<String> clientMods = new ArrayList<>();
        List<String> copyDirs = new ArrayList<>(Arrays.asList(
                "config",
                "mods",
                "scripts",
                "seeds",
                "defaultconfigs"
        ));
        createServerPack.copyFiles(modpackDir, copyDirs, clientMods);
        Assertions.assertTrue(new File(String.format("%s/server_pack/config",modpackDir)).isDirectory());
        Assertions.assertTrue(new File(String.format("%s/server_pack/mods",modpackDir)).isDirectory());
        Assertions.assertTrue(new File(String.format("%s/server_pack/scripts",modpackDir)).isDirectory());
        Assertions.assertTrue(new File(String.format("%s/server_pack/seeds",modpackDir)).isDirectory());
        Assertions.assertTrue(new File(String.format("%s/server_pack/defaultconfigs",modpackDir)).isDirectory());
        Assertions.assertTrue(new File(String.format("%s/server_pack/config/testfile.txt",modpackDir)).exists());
        Assertions.assertTrue(new File(String.format("%s/server_pack/defaultconfigs/testfile.txt",modpackDir)).exists());
        Assertions.assertTrue(new File(String.format("%s/server_pack/mods/testmod.jar",modpackDir)).exists());
        Assertions.assertTrue(new File(String.format("%s/server_pack/scripts/testscript.zs",modpackDir)).exists());
        Assertions.assertTrue(new File(String.format("%s/server_pack/seeds/testjson.json",modpackDir)).exists());
        for (String s : copyDirs) {
            String deleteMe = (String.format("%s/server_pack/%s",modpackDir,s));
            if (new File(deleteMe).isDirectory()) {
                Path pathToBeDeleted = Paths.get(deleteMe);
                Files.walk(pathToBeDeleted)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        }
    }

    @Test
    void excludeClientModsTest() {
        List<String> clientMods = Arrays.asList(
                "aaaaa","bbbbb","ccccc","fffff","ggggg","hhhhh","iiiii","jjjjj","kkkkk","lllll",
                "nnnnn","ppppp","qqqqq","rrrrr","uuuuu","vvvvv","wwwww","xxxxx","yyyyy"
        );
        List<String> result = createServerPack.excludeClientMods("src/test/resources/forge_tests/mods", clientMods);
        System.out.println(result);
        Assertions.assertFalse(result.toString().contains("aaaaa"));
        Assertions.assertFalse(result.toString().contains("bbbbb"));
        Assertions.assertFalse(result.toString().contains("ccccc"));
        Assertions.assertFalse(result.toString().contains("fffff"));
        Assertions.assertFalse(result.toString().contains("ggggg"));
        Assertions.assertFalse(result.toString().contains("hhhhh"));
        Assertions.assertFalse(result.toString().contains("iiiii"));
        Assertions.assertFalse(result.toString().contains("jjjjj"));
        Assertions.assertFalse(result.toString().contains("kkkkk"));
        Assertions.assertFalse(result.toString().contains("lllll"));
        Assertions.assertFalse(result.toString().contains("nnnnn"));
        Assertions.assertFalse(result.toString().contains("ppppp"));
        Assertions.assertFalse(result.toString().contains("qqqqq"));
        Assertions.assertFalse(result.toString().contains("rrrrr"));
        Assertions.assertFalse(result.toString().contains("uuuuu"));
        Assertions.assertFalse(result.toString().contains("vvvvv"));
        Assertions.assertFalse(result.toString().contains("wwwww"));
        Assertions.assertFalse(result.toString().contains("xxxxx"));
        Assertions.assertFalse(result.toString().contains("yyyyy"));
        Assertions.assertTrue(result.toString().contains("zzzzz"));

        Assertions.assertTrue(result.toString().contains("testmod"));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void copyIconTest() throws IOException {
        filesSetup.filesSetup();
        String modpackDir = "./src/test/resources/forge_tests";
        createServerPack.copyIcon(modpackDir);
        Assertions.assertTrue(new File(String.format("%s/server_pack/server-icon.png",modpackDir)).exists());
        new File(String.format("%s/server_pack/server-icon.png",modpackDir)).delete();
        String delete = "./server_files";
        if (new File(delete).isDirectory()) {
            Path pathToBeDeleted = Paths.get(delete);
            Files.walk(pathToBeDeleted)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
        new File("./serverpackcreator.conf").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void copyPropertiesTest() throws IOException {
        filesSetup.filesSetup();
        String modpackDir = "./src/test/resources/forge_tests";
        createServerPack.copyProperties(modpackDir);
        Assertions.assertTrue(new File(String.format("%s/server_pack/server.properties",modpackDir)).exists());
        new File(String.format("%s/server_pack/server.properties",modpackDir)).delete();
        String delete = "./server_files";
        if (new File(delete).isDirectory()) {
            Path pathToBeDeleted = Paths.get(delete);
            Files.walk(pathToBeDeleted)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
        new File("./serverpackcreator.conf").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void installServerFabricTest() throws IOException {
        Files.createDirectories(Paths.get("./src/test/resources/fabric_tests/server_pack"));
        String modLoader = "Fabric";
        String modpackDir = "./src/test/resources/fabric_tests";
        String minecraftVersion = "1.16.5";
        String modLoaderVersion = "0.7.3";
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
        createServerPack.downloadFabricJar(modpackDir);
        createServerPack.installServer(modLoader, modpackDir, minecraftVersion, modLoaderVersion, javaPath);
        Assertions.assertTrue(new File(String.format("%s/server_pack/fabric-server-launch.jar", modpackDir)).exists());
        Assertions.assertTrue(new File(String.format("%s/server_pack/server.jar", modpackDir)).exists());
        new File(String.format("%s/server_pack/fabric-server-launch.jar", modpackDir)).delete();
        new File(String.format("%s/server_pack/server.jar", modpackDir)).delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void installServerForgeTest() throws IOException {
        Files.createDirectories(Paths.get("./src/test/resources/forge_tests/server_pack"));
        String modLoader = "Forge";
        String modpackDir = "./src/test/resources/forge_tests";
        String minecraftVersion = "1.16.5";
        String modLoaderVersion = "36.1.2";
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
        createServerPack.downloadForgeJar(minecraftVersion, modLoaderVersion, modpackDir);
        createServerPack.installServer(modLoader, modpackDir, minecraftVersion, modLoaderVersion, javaPath);
        Assertions.assertTrue(new File(String.format("%s/server_pack/1.16.5.json", modpackDir)).exists());
        Assertions.assertTrue(new File(String.format("%s/server_pack/forge.jar", modpackDir)).exists());
        Assertions.assertTrue(new File(String.format("%s/server_pack/minecraft_server.1.16.5.jar", modpackDir)).exists());
        Assertions.assertTrue(new File(String.format("%s/server_pack/download_minecraft-server.jar_forge.bat", modpackDir)).exists());
        Assertions.assertTrue(new File(String.format("%s/server_pack/download_minecraft-server.jar_forge.sh", modpackDir)).exists());
        new File(String.format("%s/server_pack/1.16.5.json", modpackDir)).delete();
        new File(String.format("%s/server_pack/forge.jar", modpackDir)).delete();
        new File(String.format("%s/server_pack/forge-installer.jar", modpackDir)).delete();
        new File(String.format("%s/server_pack/forge-installer.jar.log", modpackDir)).delete();
        new File(String.format("%s/server_pack/minecraft_server.1.16.5.jar", modpackDir)).delete();
        new File(String.format("%s/server_pack/download_minecraft-server.jar_forge.bat", modpackDir)).delete();
        new File(String.format("%s/server_pack/download_minecraft-server.jar_forge.sh", modpackDir)).delete();
        String delete = String.format("%s/server_pack/libraries", modpackDir);
        if (new File(delete).isDirectory()) {
            Path pathToBeDeleted = Paths.get(delete);
            Files.walk(pathToBeDeleted).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
    }

    @Test
    void zipBuilderFabricTest() throws IOException {
        Files.createDirectories(Paths.get("./src/test/resources/fabric_tests/server_pack"));
        String minecraftVersion = "1.16.5";
        String modLoader = "Fabric";
        String modpackDir = "src/test/resources/fabric_tests";
        createServerPack.zipBuilder(modpackDir, modLoader, Boolean.FALSE, minecraftVersion);
        Assertions.assertTrue(new File("src/test/resources/fabric_tests/server_pack.zip").exists());
        Files.copy(Paths.get("./src/test/resources/testresources/server_pack.zip"), Paths.get("./src/test/resources/fabric_tests/server_pack.zip"), REPLACE_EXISTING);
    }

    @Test
    void zipBuilderForgeTest() throws IOException {
        Files.createDirectories(Paths.get("./src/test/resources/forge_tests/server_pack"));
        String minecraftVersion = "1.16.5";
        String modLoader = "Forge";
        String modpackDir = "./src/test/resources/forge_tests";
        createServerPack.zipBuilder(modpackDir, modLoader, Boolean.FALSE, minecraftVersion);
        Assertions.assertTrue(new File("./src/test/resources/forge_tests/server_pack.zip").exists());
        Files.copy(Paths.get("./src/test/resources/testresources/server_pack.zip"), Paths.get("./src/test/resources/forge_tests/server_pack.zip"), REPLACE_EXISTING);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void generateDownloadScriptsFabricTest() throws IOException {
        Files.createDirectories(Paths.get("./src/test/resources/fabric_tests/server_pack"));
        String modLoader = "Fabric";
        String modpackDir = "./src/test/resources/fabric_tests";
        String minecraftVersion = "1.16.5";
        createServerPack.generateDownloadScripts(modLoader, modpackDir, minecraftVersion);
        Assertions.assertTrue(new File(String.format("%s/server_pack/download_minecraft-server.jar_fabric.bat", modpackDir)).exists());
        Assertions.assertTrue(new File(String.format("%s/server_pack/download_minecraft-server.jar_fabric.sh", modpackDir)).exists());
        new File(String.format("%s/server_pack/download_minecraft-server.jar_fabric.bat", modpackDir)).delete();
        new File(String.format("%s/server_pack/download_minecraft-server.jar_fabric.sh", modpackDir)).delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void generateDownloadScriptsForgeTest() throws IOException {
        Files.createDirectories(Paths.get("./src/test/resources/forge_tests/server_pack"));
        String modLoader = "Forge";
        String modpackDir = "./src/test/resources/forge_tests";
        String minecraftVersion = "1.16.5";
        createServerPack.generateDownloadScripts(modLoader, modpackDir, minecraftVersion);
        Assertions.assertTrue(new File(String.format("%s/server_pack/download_minecraft-server.jar_forge.bat", modpackDir)).exists());
        Assertions.assertTrue(new File(String.format("%s/server_pack/download_minecraft-server.jar_forge.sh", modpackDir)).exists());
        new File(String.format("%s/server_pack/download_minecraft-server.jar_forge.bat", modpackDir)).delete();
        new File(String.format("%s/server_pack/download_minecraft-server.jar_forge.sh", modpackDir)).delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void fabricShellTest() throws IOException {
        filesSetup.filesSetup();
        String modpackDir = "./src/test/resources/forge_tests";
        Files.createDirectories(Paths.get("./src/test/resources/forge_tests/server_pack"));
        String minecraftVersion = "1.16.5";
        createServerPack.fabricShell(modpackDir, minecraftVersion);
        Assertions.assertTrue(new File("./src/test/resources/forge_tests/server_pack/download_minecraft-server.jar_fabric.sh").exists());
        new File("./src/test/resources/forge_tests/server_pack/download_minecraft-server.jar_fabric.sh").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void fabricBatchTest() throws IOException {
        filesSetup.filesSetup();
        String modpackDir = "./src/test/resources/forge_tests";
        Files.createDirectories(Paths.get("./src/test/resources/forge_tests/server_pack"));
        String minecraftVersion = "1.16.5";
        createServerPack.fabricBatch(modpackDir, minecraftVersion);
        Assertions.assertTrue(new File("./src/test/resources/forge_tests/server_pack/download_minecraft-server.jar_fabric.bat").exists());
        new File("./src/test/resources/forge_tests/server_pack/download_minecraft-server.jar_fabric.bat").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void forgeShellTest() throws IOException {
        filesSetup.filesSetup();
        String modpackDir = "./src/test/resources/forge_tests";
        Files.createDirectories(Paths.get("./src/test/resources/forge_tests/server_pack"));
        String minecraftVersion = "1.16.5";
        createServerPack.forgeShell(modpackDir, minecraftVersion);
        Assertions.assertTrue(new File("./src/test/resources/forge_tests/server_pack/download_minecraft-server.jar_forge.sh").exists());
        new File("./src/test/resources/forge_tests/server_pack/download_minecraft-server.jar_forge.sh").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void forgeBatchTest() throws IOException {
        filesSetup.filesSetup();
        String modpackDir = "./src/test/resources/forge_tests";
        Files.createDirectories(Paths.get("./src/test/resources/forge_tests/server_pack"));
        String minecraftVersion = "1.16.5";
        createServerPack.forgeBatch(modpackDir, minecraftVersion);
        Assertions.assertTrue(new File("./src/test/resources/forge_tests/server_pack/download_minecraft-server.jar_forge.bat").exists());
        new File("./src/test/resources/forge_tests/server_pack/download_minecraft-server.jar_forge.bat").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void downloadFabricJarTest() throws IOException {
        String modpackDir = "./src/test/resources/fabric_tests";
        Files.createDirectories(Paths.get("./src/test/resources/fabric_tests/server_pack"));
        boolean result = createServerPack.downloadFabricJar(modpackDir);
        Assertions.assertTrue(result);
        Assertions.assertTrue(new File(String.format("%s/server_pack/fabric-installer.jar", modpackDir)).exists());
        new File(String.format("%s/server_pack/fabric-installer.jar", modpackDir)).delete();
        new File(String.format("%s/server_pack/fabric-installer.xml", modpackDir)).delete();
    }

    @Test
    void latestFabricInstallerTest() {
        String modpackDir = "./src/test/resources/forge_tests";
        String result = createServerPack.latestFabricInstaller(modpackDir);
        Assertions.assertNotNull(result);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void downloadForgeJarTest() throws IOException {
        String modLoaderVersion = "36.1.2";
        String modpackDir = "./src/test/resources/forge_tests";
        Files.createDirectories(Paths.get("./src/test/resources/forge_tests/server_pack"));
        String minecraftVersion = "1.16.5";
        boolean result = createServerPack.downloadForgeJar(minecraftVersion, modLoaderVersion, modpackDir);
        Assertions.assertTrue(result);
        Assertions.assertTrue(new File(String.format("%s/server_pack/forge-installer.jar", modpackDir)).exists());
        new File(String.format("%s/server_pack/forge-installer.jar", modpackDir)).delete();
    }

    @Test
    void deleteMinecraftJarFabricTest() throws IOException {
        Files.copy(Paths.get("./src/test/resources/testresources/server_pack.zip"), Paths.get("./src/test/resources/fabric_tests/server_pack.zip"), REPLACE_EXISTING);
        String minecraftVersion = "1.16.5";
        String modLoader = "Fabric";
        String modpackDir = "./src/test/resources/fabric_tests";
        createServerPack.deleteMinecraftJar(modLoader, modpackDir, minecraftVersion);
    }

    @Test
    void deleteMinecraftJarForgeTest() throws IOException {
        Files.copy(Paths.get("./src/test/resources/testresources/server_pack.zip"), Paths.get("./src/test/resources/forge_tests/server_pack.zip"), REPLACE_EXISTING);
        String minecraftVersion = "1.16.5";
        String modLoader = "Forge";
        String modpackDir = "./src/test/resources/forge_tests";
        createServerPack.deleteMinecraftJar(modLoader, modpackDir, minecraftVersion);
    }

    @Test
    void cleanUpServerPackForgeTest() throws IOException {
        String modLoader = "Forge";
        String modpackDir = "./src/test/resources/forge_tests";
        String minecraftVersion = "1.16.5";
        String modLoaderVersion = "36.1.2";
        new File("./src/test/resources/forge_tests/server_pack/forge-1.16.5-36.1.2.jar").createNewFile();
        createServerPack.cleanUpServerPack(
                new File(String.format("%s/server_pack/forge-installer.jar", modpackDir)),
                new File(String.format("%s/server_pack/forge-%s-%s.jar", modpackDir, minecraftVersion, modLoaderVersion)),
                modLoader,
                modpackDir,
                minecraftVersion,
                modLoaderVersion);
        Assertions.assertFalse(new File("./src/test/resources/forge_tests/forge-1.16.5-36.1.2.jar").exists());
        Assertions.assertFalse(new File("./src/test/resources/forge_tests/forge-installer.jar").exists());
    }

    @Test
    void cleanUpServerPackFabricTest() {
        String modLoader = "Fabric";
        String modpackDir = "./src/test/resources/fabric_tests";
        String minecraftVersion = "1.16.5";
        String modLoaderVersion = "36.1.2";
        createServerPack.cleanUpServerPack(
                new File(String.format("%s/server_pack/fabric-installer.jar", modpackDir)),
                new File(String.format("%s/server_pack/forge-%s-%s.jar", modpackDir, minecraftVersion, modLoaderVersion)),
                modLoader,
                modpackDir,
                minecraftVersion,
                modLoaderVersion);
        Assertions.assertFalse(new File("./src/test/resources/forge_tests/fabric-installer.xml").exists());
        Assertions.assertFalse(new File("./src/test/resources/forge_tests/fabric-installer.jar").exists());
    }
}
