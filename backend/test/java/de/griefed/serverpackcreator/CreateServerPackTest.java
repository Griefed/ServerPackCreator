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
 * 32.{@link #cleanUpServerPackForgeTest()}<br>
 * 33.{@link #cleanUpServerPackFabricTest()}
 */
class CreateServerPackTest {

    private final CreateServerPack createServerPack;
    private final DefaultFiles filesSetup;
    private final CurseCreateModpack curseCreateModpack;
    private final LocalizationManager localizationManager;
    private final Configuration configuration;
    private final AddonsHandler addonsHandler;

    CreateServerPackTest() {
        localizationManager = new LocalizationManager();
        localizationManager.init();
        addonsHandler = new AddonsHandler(localizationManager);
        curseCreateModpack = new CurseCreateModpack(localizationManager);
        configuration = new Configuration(localizationManager, curseCreateModpack);
        createServerPack = new CreateServerPack(localizationManager, curseCreateModpack, addonsHandler);
        filesSetup = new DefaultFiles(localizationManager);
        filesSetup.filesSetup();
    }

/*    @BeforeEach
    void setUp() {
        localizationManager.checkLocaleFile();
        filesSetup.filesSetup();
        MockitoAnnotations.openMocks(this);
    }*/

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
        Files.copy(Paths.get("backend/test/resources/testresources/ServerPackCreatorExampleAddon-1.1.0.jar"), Paths.get("addons/ServerPackCreatorExampleAddon-1.1.0.jar"), REPLACE_EXISTING);
        addonsHandler.initializeAddons();
        Files.copy(Paths.get("./backend/test/resources/testresources/serverpackcreator.conf"), Paths.get("serverpackcreator.conf"), REPLACE_EXISTING);
        createServerPack.run(configuration.getConfigFile());
        Assertions.assertTrue(new File("server-packs/forge_tests/libraries").isDirectory());
        Assertions.assertTrue(new File("server-packs/forge_tests/config").isDirectory());
        Assertions.assertTrue(new File("server-packs/forge_tests/defaultconfigs").isDirectory());
        Assertions.assertTrue(new File("server-packs/forge_tests/mods").isDirectory());
        Assertions.assertTrue(new File("server-packs/forge_tests/scripts").isDirectory());
        Assertions.assertTrue(new File("server-packs/forge_tests/seeds").isDirectory());

        Assertions.assertTrue(new File("server-packs/forge_tests/1.16.5.json").exists());
        Assertions.assertTrue(new File("server-packs/forge_tests/minecraft_server.1.16.5.jar").exists());
        Assertions.assertTrue(new File("server-packs/forge_tests/download_minecraft-server.jar_forge.bat").exists());
        Assertions.assertTrue(new File("server-packs/forge_tests/download_minecraft-server.jar_forge.sh").exists());
        Assertions.assertTrue(new File("server-packs/forge_tests/forge.jar").exists());
        Assertions.assertTrue(new File("server-packs/forge_tests/server.properties").exists());
        Assertions.assertTrue(new File("server-packs/forge_tests/server-icon.png").exists());
        Assertions.assertTrue(new File("server-packs/forge_tests/start-forge.bat").exists());
        Assertions.assertTrue(new File("server-packs/forge_tests/start-forge.sh").exists());

        if (new File("server-packs/forge_tests/libraries").isDirectory()) {
            Path pathToBeDeleted = Paths.get("server-packs/forge_tests/libraries");
            Files.walk(pathToBeDeleted).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
        if (new File("server-packs/forge_tests/config").isDirectory()) {
            Path pathToBeDeleted = Paths.get("server-packs/forge_tests/config");
            Files.walk(pathToBeDeleted).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
        if (new File("server-packs/forge_tests/defaultconfigs").isDirectory()) {
            Path pathToBeDeleted = Paths.get("server-packs/forge_tests/defaultconfigs");
            Files.walk(pathToBeDeleted).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
        if (new File("server-packs/forge_tests/mods").isDirectory()) {
            Path pathToBeDeleted = Paths.get("server-packs/forge_tests/mods");
            Files.walk(pathToBeDeleted).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
        if (new File("server-packs/forge_tests/scripts").isDirectory()) {
            Path pathToBeDeleted = Paths.get("server-packs/forge_tests/scripts");
            Files.walk(pathToBeDeleted).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
        if (new File("server-packs/forge_tests/seeds").isDirectory()) {
            Path pathToBeDeleted = Paths.get("server-packs/forge_tests/seeds");
            Files.walk(pathToBeDeleted).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
        if (new File("server_files").isDirectory()) {
            Path pathToBeDeleted = Paths.get("server_files");
            Files.walk(pathToBeDeleted).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
        new File("server-packs/forge_tests/1.16.5.json").delete();
        new File("server-packs/forge_tests/minecraft_server.1.16.5.jar").delete();
        new File("server-packs/forge_tests/download_minecraft-server.jar_forge.bat").delete();
        new File("server-packs/forge_tests/download_minecraft-server.jar_forge.sh").delete();
        new File("server-packs/forge_tests/forge.jar").delete();
        new File("server-packs/forge_tests/server.properties").delete();
        new File("server-packs/forge_tests/server-icon.png").delete();
        new File("server-packs/forge_tests/start-forge.bat").delete();
        new File("server-packs/forge_tests/start-forge.sh").delete();
        new File("./serverpackcreator.conf").delete();
        Files.copy(Paths.get("./backend/test/resources/testresources/server_pack.zip"), Paths.get("server-packs/forge_tests_server_pack.zip"), REPLACE_EXISTING);
    }

    @Test
    void cleanupEnvironmentTest() throws IOException {
        Files.createDirectories(Paths.get("server-packs/forge_tests"));
        String modpackDir = "server-packs/forge_tests";
        Files.copy(Paths.get("./backend/test/resources/testresources/server_pack.zip"), Paths.get("server-packs/forge_tests_server_pack.zip"), REPLACE_EXISTING);
        createServerPack.cleanupEnvironment(modpackDir);
        Assertions.assertFalse(new File("\"server-packs/forge_tests_server_pack.zip\"").exists());
        Assertions.assertFalse(new File("server-packs/forge_tests").isDirectory());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void copyStartScriptsFabricTest() throws IOException {
        filesSetup.filesSetup();
        String modpackDir = "./backend/test/resources/fabric_tests";
        String destination = modpackDir.substring(modpackDir.lastIndexOf("/") + 1);
        String modLoader = "Fabric";
        filesSetup.filesSetup();
        createServerPack.copyStartScripts(modpackDir, modLoader, true);
        Assertions.assertTrue(new File(String.format("server-packs/%s/start-fabric.bat", destination)).exists());
        Assertions.assertTrue(new File(String.format("server-packs/%s/start-fabric.sh", destination)).exists());
        new File(String.format("server-packs/%s/start-fabric.bat", destination)).delete();
        new File(String.format("server-packs/%s/start-fabric.sh", destination)).delete();
        String delete = "server_files";
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
        String modpackDir = "./backend/test/resources/forge_tests";
        String destination = modpackDir.substring(modpackDir.lastIndexOf("/") + 1);
        String modLoader = "Forge";
        filesSetup.filesSetup();
        createServerPack.copyStartScripts(modpackDir, modLoader, true);
        Assertions.assertTrue(new File(String.format("server-packs/%s/start-forge.bat", destination)).exists());
        Assertions.assertTrue(new File(String.format("server-packs/%s/start-forge.sh", destination)).exists());
        new File(String.format("server-packs/%s/start-forge.bat", destination)).delete();
        new File(String.format("server-packs/%s/start-forge.sh", destination)).delete();
        String delete = "server_files";
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
        String modpackDir = "./backend/test/resources/forge_tests";
        String destination = modpackDir.substring(modpackDir.lastIndexOf("/") + 1);
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
        Assertions.assertTrue(new File(String.format("server-packs/%s/config", destination)).isDirectory());
        Assertions.assertTrue(new File(String.format("server-packs/%s/mods", destination)).isDirectory());
        Assertions.assertTrue(new File(String.format("server-packs/%s/scripts", destination)).isDirectory());
        Assertions.assertTrue(new File(String.format("server-packs/%s/seeds", destination)).isDirectory());
        Assertions.assertTrue(new File(String.format("server-packs/%s/defaultconfigs", destination)).isDirectory());
        Assertions.assertTrue(new File(String.format("server-packs/%s/config/testfile.txt", destination)).exists());
        Assertions.assertTrue(new File(String.format("server-packs/%s/defaultconfigs/testfile.txt", destination)).exists());
        Assertions.assertTrue(new File(String.format("server-packs/%s/mods/testmod.jar", destination)).exists());
        Assertions.assertTrue(new File(String.format("server-packs/%s/scripts/testscript.zs", destination)).exists());
        Assertions.assertTrue(new File(String.format("server-packs/%s/seeds/testjson.json", destination)).exists());
        for (String dir : copyDirs) {
            String deleteMe = (String.format("server-packs/%s/%s", destination, dir));
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
        String modpackDir = "./backend/test/resources/forge_tests";
        String destination = modpackDir.substring(modpackDir.lastIndexOf("/") + 1);
        List<String> clientMods = new ArrayList<>();
        List<String> copyDirs = new ArrayList<>(Arrays.asList(
                "config",
                "mods",
                "scripts",
                "seeds",
                "defaultconfigs"
        ));
        createServerPack.copyFiles(modpackDir, copyDirs, clientMods);
        Assertions.assertTrue(new File(String.format("server-packs/%s/config", destination)).isDirectory());
        Assertions.assertTrue(new File(String.format("server-packs/%s/mods", destination)).isDirectory());
        Assertions.assertTrue(new File(String.format("server-packs/%s/scripts", destination)).isDirectory());
        Assertions.assertTrue(new File(String.format("server-packs/%s/seeds", destination)).isDirectory());
        Assertions.assertTrue(new File(String.format("server-packs/%s/defaultconfigs", destination)).isDirectory());
        Assertions.assertTrue(new File(String.format("server-packs/%s/config/testfile.txt", destination)).exists());
        Assertions.assertTrue(new File(String.format("server-packs/%s/defaultconfigs/testfile.txt", destination)).exists());
        Assertions.assertTrue(new File(String.format("server-packs/%s/mods/testmod.jar", destination)).exists());
        Assertions.assertTrue(new File(String.format("server-packs/%s/scripts/testscript.zs", destination)).exists());
        Assertions.assertTrue(new File(String.format("server-packs/%s/seeds/testjson.json", destination)).exists());
        for (String dir : copyDirs) {
            String deleteMe = (String.format("server-packs/%s/%s", destination, dir));
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
        List<String> result = createServerPack.excludeClientMods("backend/test/resources/forge_tests/mods", clientMods);
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
        String modpackDir = "./backend/test/resources/forge_tests";
        String destination = modpackDir.substring(modpackDir.lastIndexOf("/") + 1);
        createServerPack.copyIcon(modpackDir);
        Assertions.assertTrue(new File(String.format("server-packs/%s/server-icon.png", destination)).exists());
        new File(String.format("server-packs/%s/server-icon.png", destination)).delete();
        String delete = "server_files";
        if (new File(delete).isDirectory()) {
            Path pathToBeDeleted = Paths.get(delete);
            Files.walk(pathToBeDeleted)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
        new File("serverpackcreator.conf").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void copyPropertiesTest() throws IOException {
        filesSetup.filesSetup();
        String modpackDir = "./backend/test/resources/forge_tests";
        String destination = modpackDir.substring(modpackDir.lastIndexOf("/") + 1);
        createServerPack.copyProperties(modpackDir);
        Assertions.assertTrue(new File(String.format("server-packs/%s/server.properties", destination  )).exists());
        new File(String.format("server-packs/%s/server.properties", destination)).delete();
        String delete = "server_files";
        if (new File(delete).isDirectory()) {
            Path pathToBeDeleted = Paths.get(delete);
            Files.walk(pathToBeDeleted)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
        new File("serverpackcreator.conf").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void installServerFabricTest() throws IOException {
        Files.createDirectories(Paths.get("server-packs/fabric_tests"));
        String modLoader = "Fabric";
        String modpackDir = "./backend/test/resources/fabric_tests";
        String destination = modpackDir.substring(modpackDir.lastIndexOf("/") + 1);
        String minecraftVersion = "1.16.5";
        String modLoaderVersion = "0.11.6";
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
        Assertions.assertTrue(new File(String.format("server-packs/%s/fabric-server-launch.jar", destination)).exists());
        Assertions.assertTrue(new File(String.format("server-packs/%s/server.jar", destination)).exists());
        new File(String.format("server-packs/%s/fabric-server-launch.jar", destination)).delete();
        new File(String.format("server-packs/%s/server.jar", destination)).delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void installServerForgeTest() throws IOException {
        Files.createDirectories(Paths.get("server-packs/forge_tests"));
        String modLoader = "Forge";
        String modpackDir = "./backend/test/resources/forge_tests";
        String destination = modpackDir.substring(modpackDir.lastIndexOf("/") + 1);
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
        Assertions.assertTrue(new File(String.format("server-packs/%s/1.16.5.json", destination)).exists());
        Assertions.assertTrue(new File(String.format("server-packs/%s/forge.jar", destination)).exists());
        Assertions.assertTrue(new File(String.format("server-packs/%s/minecraft_server.1.16.5.jar", destination)).exists());
        Assertions.assertTrue(new File(String.format("server-packs/%s/download_minecraft-server.jar_forge.bat", destination)).exists());
        Assertions.assertTrue(new File(String.format("server-packs/%s/download_minecraft-server.jar_forge.sh", destination)).exists());
        new File(String.format("server-packs/%s/1.16.5.json", destination)).delete();
        new File(String.format("server-packs/%s/forge.jar", destination)).delete();
        new File(String.format("server-packs/%s/forge-installer.jar", destination)).delete();
        new File(String.format("server-packs/%s/forge-installer.jar.log", destination)).delete();
        new File(String.format("server-packs/%s/minecraft_server.1.16.5.jar", destination)).delete();
        new File(String.format("server-packs/%s/download_minecraft-server.jar_forge.bat", destination)).delete();
        new File(String.format("server-packs/%s/download_minecraft-server.jar_forge.sh", destination)).delete();
        String delete = String.format("server-packs/%s/libraries", destination);
        if (new File(delete).isDirectory()) {
            Path pathToBeDeleted = Paths.get(delete);
            Files.walk(pathToBeDeleted).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
    }

    @Test
    void zipBuilderFabricTest() throws IOException {
        Files.createDirectories(Paths.get("server-packs/fabric_tests"));
        String minecraftVersion = "1.16.5";
        String modpackDir = "backend/test/resources/fabric_tests";
        createServerPack.zipBuilder(modpackDir, minecraftVersion, Boolean.TRUE);
        Assertions.assertTrue(new File("server-packs/fabric_tests_server_pack.zip").exists());
        Files.copy(Paths.get("./backend/test/resources/testresources/server_pack.zip"), Paths.get("./backend/test/resources/fabric_tests/server_pack.zip"), REPLACE_EXISTING);
    }

    @Test
    void zipBuilderForgeTest() throws IOException {
        Files.createDirectories(Paths.get("server-packs/forge_tests"));
        String minecraftVersion = "1.16.5";
        String modpackDir = "./backend/test/resources/forge_tests";
        createServerPack.zipBuilder(modpackDir, minecraftVersion, Boolean.TRUE);
        Assertions.assertTrue(new File("server-packs/forge_tests_server_pack.zip").exists());
        Files.copy(Paths.get("./backend/test/resources/testresources/server_pack.zip"), Paths.get("./backend/test/resources/forge_tests/server_pack.zip"), REPLACE_EXISTING);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void generateDownloadScriptsFabricTest() throws IOException {
        Files.createDirectories(Paths.get("server-packs/fabric_tests"));
        String modLoader = "Fabric";
        String modpackDir = "./backend/test/resources/fabric_tests";
        String destination = modpackDir.substring(modpackDir.lastIndexOf("/") + 1);
        String minecraftVersion = "1.16.5";
        createServerPack.generateDownloadScripts(modLoader, modpackDir, minecraftVersion);
        Assertions.assertTrue(new File(String.format("server-packs/%s/download_minecraft-server.jar_fabric.bat", destination)).exists());
        Assertions.assertTrue(new File(String.format("server-packs/%s/download_minecraft-server.jar_fabric.sh", destination)).exists());
        new File(String.format("server-packs/%s/download_minecraft-server.jar_fabric.bat", destination)).delete();
        new File(String.format("server-packs/%s/download_minecraft-server.jar_fabric.sh", destination)).delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void generateDownloadScriptsForgeTest() throws IOException {
        Files.createDirectories(Paths.get("server-packs/forge_tests"));
        String modLoader = "Forge";
        String modpackDir = "./backend/test/resources/forge_tests";
        String destination = modpackDir.substring(modpackDir.lastIndexOf("/") + 1);
        String minecraftVersion = "1.16.5";
        createServerPack.generateDownloadScripts(modLoader, modpackDir, minecraftVersion);
        Assertions.assertTrue(new File(String.format("server-packs/%s/download_minecraft-server.jar_forge.bat", destination)).exists());
        Assertions.assertTrue(new File(String.format("server-packs/%s/download_minecraft-server.jar_forge.sh", destination)).exists());
        new File(String.format("server-packs/%s/download_minecraft-server.jar_forge.bat", destination)).delete();
        new File(String.format("server-packs/%s/download_minecraft-server.jar_forge.sh", destination)).delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void fabricShellTest() throws IOException {
        filesSetup.filesSetup();
        String modpackDir = "./backend/test/resources/forge_tests";
        Files.createDirectories(Paths.get("server-packs/forge_tests"));
        String minecraftVersion = "1.16.5";
        createServerPack.fabricShell(modpackDir, minecraftVersion);
        Assertions.assertTrue(new File("server-packs/forge_tests/download_minecraft-server.jar_fabric.sh").exists());
        new File("server-packs/forge_tests/download_minecraft-server.jar_fabric.sh").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void fabricBatchTest() throws IOException {
        filesSetup.filesSetup();
        String modpackDir = "./backend/test/resources/forge_tests";
        Files.createDirectories(Paths.get("server-packs/forge_tests"));
        String minecraftVersion = "1.16.5";
        createServerPack.fabricBatch(modpackDir, minecraftVersion);
        Assertions.assertTrue(new File("server-packs/forge_tests/download_minecraft-server.jar_fabric.bat").exists());
        new File("server-packs/forge_tests/download_minecraft-server.jar_fabric.bat").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void forgeShellTest() throws IOException {
        filesSetup.filesSetup();
        String modpackDir = "./backend/test/resources/forge_tests";
        Files.createDirectories(Paths.get("server-packs/forge_tests"));
        String minecraftVersion = "1.16.5";
        createServerPack.forgeShell(modpackDir, minecraftVersion);
        Assertions.assertTrue(new File("server-packs/forge_tests/download_minecraft-server.jar_forge.sh").exists());
        new File("server-packs/forge_tests/download_minecraft-server.jar_forge.sh").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void forgeBatchTest() throws IOException {
        filesSetup.filesSetup();
        String modpackDir = "./backend/test/resources/forge_tests";
        Files.createDirectories(Paths.get("server-packs/forge_tests"));
        String minecraftVersion = "1.16.5";
        createServerPack.forgeBatch(modpackDir, minecraftVersion);
        Assertions.assertTrue(new File("server-packs/forge_tests/download_minecraft-server.jar_forge.bat").exists());
        new File("server-packs/forge_tests/download_minecraft-server.jar_forge.bat").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void downloadFabricJarTest() throws IOException {
        String modpackDir = "./backend/test/resources/fabric_tests";
        String destination = modpackDir.substring(modpackDir.lastIndexOf("/") + 1);
        Files.createDirectories(Paths.get("server-packs/fabric_tests"));
        boolean result = createServerPack.downloadFabricJar(modpackDir);
        Assertions.assertTrue(result);
        Assertions.assertTrue(new File(String.format("server-packs/%s/fabric-installer.jar", destination)).exists());
        new File(String.format("server-packs/%s/fabric-installer.jar", destination)).delete();
        new File(String.format("server-packs/%s/fabric-installer.xml", destination)).delete();
    }

    @Test
    void latestFabricInstallerTest() {
        String result = createServerPack.latestFabricInstaller();
        Assertions.assertNotNull(result);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void downloadForgeJarTest() throws IOException {
        String modLoaderVersion = "36.1.2";
        String modpackDir = "./backend/test/resources/forge_tests";
        String destination = modpackDir.substring(modpackDir.lastIndexOf("/") + 1);
        Files.createDirectories(Paths.get("server-packs/forge_tests"));
        String minecraftVersion = "1.16.5";
        boolean result = createServerPack.downloadForgeJar(minecraftVersion, modLoaderVersion, modpackDir);
        Assertions.assertTrue(result);
        Assertions.assertTrue(new File(String.format("server-packs/%s/forge-installer.jar", destination)).exists());
        new File(String.format("server-packs/%s/forge-installer.jar", destination)).delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void cleanUpServerPackForgeTest() throws IOException {
        String modLoader = "Forge";
        String modpackDir = "./backend/test/resources/forge_tests";
        String destination = modpackDir.substring(modpackDir.lastIndexOf("/") + 1);
        String minecraftVersion = "1.16.5";
        String modLoaderVersion = "36.1.2";
        new File("server-packs/forge_tests/forge-1.16.5-36.1.2.jar").createNewFile();
        createServerPack.cleanUpServerPack(
                new File(String.format("server-packs/%s/forge-installer.jar", destination)),
                new File(String.format("server-packs/%s/forge-%s-%s.jar", destination, minecraftVersion, modLoaderVersion)),
                modLoader,
                modpackDir,
                minecraftVersion,
                modLoaderVersion);
        Assertions.assertFalse(new File("server-packs/forge_tests/forge-1.16.5-36.1.2.jar").exists());
        Assertions.assertFalse(new File("server-packs/forge_tests/forge-installer.jar").exists());
    }

    @Test
    void cleanUpServerPackFabricTest() {
        String modLoader = "Fabric";
        String modpackDir = "./backend/test/resources/fabric_tests";
        String destination = modpackDir.substring(modpackDir.lastIndexOf("/") + 1);
        String minecraftVersion = "1.16.5";
        String modLoaderVersion = "36.1.2";
        createServerPack.cleanUpServerPack(
                new File(String.format("server-packs/%s/fabric-installer.jar", destination)),
                new File(String.format("server-packs/%s/forge-%s-%s.jar", destination, minecraftVersion, modLoaderVersion)),
                modLoader,
                modpackDir,
                minecraftVersion,
                modLoaderVersion);
        Assertions.assertFalse(new File("server-packs/forge_tests/fabric-installer.jar").exists());
    }
}
