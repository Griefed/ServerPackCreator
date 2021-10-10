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
 * 1. {@link #ServerPackHandlerTest()}<br>
 * 2. {@link #getPropertiesFileTest()}<br>
 * 3. {@link #getIconFileTest()}<br>
 * 4. {@link #getWindowsFileTest()}<br>
 * 5. {@link #getLinuxFileTest()}<br>
 * 6. {@link #getObjectMapperTest()}<br>
 * 7. {@link #runTest()}<br>
 * 8. {@link #cleanupEnvironmentTest()}<br>
 * 9. {@link #createStartScriptsFabricTest()}<br>
 * 10.{@link #createStartScriptsForgeTest()}<br>
 * 11.{@link #copyFilesTest()}<br>
 * 12.{@link #copyFilesEmptyClientsTest()}<br>
 * 13.{@link #excludeClientModsTest()}<br>
 * 14.{@link #copyIconTest()}<br>
 * 15.{@link #copyPropertiesTest()}<br
 * 16.{@link #installServerFabricTest()}<br>
 * 17.{@link #installServerForgeTest()}<br>
 * 18.{@link #zipBuilderFabricTest()}<br>
 * 19.{@link #zipBuilderForgeTest()}<br>
 * 20.{@link #downloadFabricJarTest()}<br>
 * 21.{@link #downloadForgeJarTest()}<br>
 * 22.{@link #cleanUpServerPackForgeTest()}<br>
 * 23.{@link #cleanUpServerPackFabricTest()}<br>
 * 24.{@link #runConfigModelTest()}
 */
class ServerPackHandlerTest {

    private final ServerPackHandler SERVERPACKHANDLER;
    private final DefaultFiles DEFAULTFILES;
    private final CurseCreateModpack CURSECREATEMODPACK;
    private final LocalizationManager LOCALIZATIONMANAGER;
    private final ConfigurationHandler CONFIGURATIONHANDLER;
    private final AddonsHandler ADDONSHANDLER;
    private final VersionLister VERSIONLISTER;
    private Properties serverPackCreatorProperties;

    ServerPackHandlerTest() {
        try {
            FileUtils.copyFile(new File("backend/main/resources/serverpackcreator.properties"),new File("serverpackcreator.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (InputStream inputStream = new FileInputStream("serverpackcreator.properties")) {
            this.serverPackCreatorProperties = new Properties();
            this.serverPackCreatorProperties.load(inputStream);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        LOCALIZATIONMANAGER = new LocalizationManager(serverPackCreatorProperties);
        LOCALIZATIONMANAGER.init();
        DEFAULTFILES = new DefaultFiles(LOCALIZATIONMANAGER, serverPackCreatorProperties);
        VERSIONLISTER = new VersionLister(serverPackCreatorProperties);
        ADDONSHANDLER = new AddonsHandler(LOCALIZATIONMANAGER, serverPackCreatorProperties);
        CURSECREATEMODPACK = new CurseCreateModpack(LOCALIZATIONMANAGER, serverPackCreatorProperties);
        CONFIGURATIONHANDLER = new ConfigurationHandler(LOCALIZATIONMANAGER, CURSECREATEMODPACK, VERSIONLISTER, serverPackCreatorProperties);
        SERVERPACKHANDLER = new ServerPackHandler(LOCALIZATIONMANAGER, CURSECREATEMODPACK, ADDONSHANDLER, CONFIGURATIONHANDLER, serverPackCreatorProperties, VERSIONLISTER);

    }

    @Test
    void getPropertiesFileTest() {
        File file = SERVERPACKHANDLER.getPropertiesFile();
        Assertions.assertNotNull(file);
    }

    @Test
    void getIconFileTest() {
        File file = SERVERPACKHANDLER.getIconFile();
        Assertions.assertNotNull(file);
    }

    @Test
    void getWindowsFileTest() {
        File file = SERVERPACKHANDLER.getWindowsFile();
        Assertions.assertNotNull(file);
    }

    @Test
    void getLinuxFileTest() {
        File file = SERVERPACKHANDLER.getLinuxFile();
        Assertions.assertNotNull(file);
    }

    @Test
    void getObjectMapperTest() {
        Assertions.assertNotNull(SERVERPACKHANDLER.getObjectMapper());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void runTest() throws IOException {
        DEFAULTFILES.filesSetup();
        ADDONSHANDLER.initializeAddons();
        Files.copy(Paths.get("./backend/test/resources/testresources/serverpackcreator.conf"), Paths.get("serverpackcreator.conf"), REPLACE_EXISTING);
        ConfigurationModel configurationModel = new ConfigurationModel();
        SERVERPACKHANDLER.run(CONFIGURATIONHANDLER.getConfigFile(), configurationModel);
        Assertions.assertTrue(new File("server-packs/forge_tests/libraries").isDirectory());
        Assertions.assertTrue(new File("server-packs/forge_tests/config").isDirectory());
        Assertions.assertTrue(new File("server-packs/forge_tests/defaultconfigs").isDirectory());
        Assertions.assertTrue(new File("server-packs/forge_tests/mods").isDirectory());
        Assertions.assertTrue(new File("server-packs/forge_tests/scripts").isDirectory());
        Assertions.assertTrue(new File("server-packs/forge_tests/seeds").isDirectory());

        Assertions.assertTrue(new File("server-packs/forge_tests/1.16.5.json").exists());
        Assertions.assertTrue(new File("server-packs/forge_tests/minecraft_server.1.16.5.jar").exists());
        Assertions.assertTrue(new File("server-packs/forge_tests/forge.jar").exists());
        Assertions.assertTrue(new File("server-packs/forge_tests/server.properties").exists());
        Assertions.assertTrue(new File("server-packs/forge_tests/server-icon.png").exists());
        Assertions.assertTrue(new File("server-packs/forge_tests/start.bat").exists());
        Assertions.assertTrue(new File("server-packs/forge_tests/start.sh").exists());

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
        new File("server-packs/forge_tests/forge.jar").delete();
        new File("server-packs/forge_tests/server.properties").delete();
        new File("server-packs/forge_tests/server-icon.png").delete();
        new File("server-packs/forge_tests/start.bat").delete();
        new File("server-packs/forge_tests/start.sh").delete();
        new File("./serverpackcreator.conf").delete();
        Files.copy(Paths.get("./backend/test/resources/testresources/server_pack.zip"), Paths.get("server-packs/forge_tests_server_pack.zip"), REPLACE_EXISTING);
    }

    @Test
    void cleanupEnvironmentTest() throws IOException {
        Files.createDirectories(Paths.get("server-packs/forge_tests"));
        String modpackDir = "server-packs/forge_tests";
        SERVERPACKHANDLER.setServerPackDestination(modpackDir, "");
        Files.copy(Paths.get("./backend/test/resources/testresources/server_pack.zip"), Paths.get("server-packs/forge_tests_server_pack.zip"), REPLACE_EXISTING);
        SERVERPACKHANDLER.cleanupEnvironment();
        Assertions.assertFalse(new File("\"server-packs/forge_tests_server_pack.zip\"").exists());
        Assertions.assertFalse(new File("server-packs/forge_tests").isDirectory());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void createStartScriptsFabricTest() throws IOException {
        DEFAULTFILES.filesSetup();
        String modpackDir = "./backend/test/resources/fabric_tests";
        String destination = modpackDir.substring(modpackDir.lastIndexOf("/") + 1);
        String modLoader = "Fabric";
        DEFAULTFILES.filesSetup();
        SERVERPACKHANDLER.setServerPackDestination(modpackDir, "");
        SERVERPACKHANDLER.createStartScripts(modLoader, "empty", "1.16.5",  "0.12.1");
        Assertions.assertTrue(new File(String.format("server-packs/%s/start.bat", SERVERPACKHANDLER.getServerPackDestination())).exists());
        Assertions.assertTrue(new File(String.format("server-packs/%s/start.sh", destination)).exists());
        new File(String.format("server-packs/%s/start.bat", SERVERPACKHANDLER.getServerPackDestination())).delete();
        new File(String.format("server-packs/%s/start.sh", SERVERPACKHANDLER.getServerPackDestination())).delete();
        String delete = "server_files";
        if (new File(delete).isDirectory()) {
            Path pathToBeDeleted = Paths.get(delete);
            Files.walk(pathToBeDeleted).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
        new File("./serverpackcreator.conf").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void createStartScriptsForgeTest() throws IOException {
        DEFAULTFILES.filesSetup();
        String modpackDir = "./backend/test/resources/forge_tests";
        String modLoader = "Forge";
        DEFAULTFILES.filesSetup();
        SERVERPACKHANDLER.setServerPackDestination(modpackDir, "");
        SERVERPACKHANDLER.createStartScripts(modLoader, "empty", "1.16.5","36.2.4");
        Assertions.assertTrue(new File(String.format("server-packs/%s/start.bat", SERVERPACKHANDLER.getServerPackDestination())).exists());
        Assertions.assertTrue(new File(String.format("server-packs/%s/start.sh", SERVERPACKHANDLER.getServerPackDestination())).exists());
        new File(String.format("server-packs/%s/start.bat", SERVERPACKHANDLER.getServerPackDestination())).delete();
        new File(String.format("server-packs/%s/start.sh", SERVERPACKHANDLER.getServerPackDestination())).delete();
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
                "defaultconfigs",
                "!bla",
                "!fancymenu"
        ));
        SERVERPACKHANDLER.setServerPackDestination(modpackDir, "");
        SERVERPACKHANDLER.copyFiles(modpackDir, copyDirs, clientMods, "1.16.5");
        Assertions.assertTrue(new File(String.format("server-packs/%s/config", SERVERPACKHANDLER.getServerPackDestination())).isDirectory());
        Assertions.assertTrue(new File(String.format("server-packs/%s/mods", SERVERPACKHANDLER.getServerPackDestination())).isDirectory());
        Assertions.assertTrue(new File(String.format("server-packs/%s/scripts", SERVERPACKHANDLER.getServerPackDestination())).isDirectory());
        Assertions.assertTrue(new File(String.format("server-packs/%s/seeds", SERVERPACKHANDLER.getServerPackDestination())).isDirectory());
        Assertions.assertTrue(new File(String.format("server-packs/%s/defaultconfigs", SERVERPACKHANDLER.getServerPackDestination())).isDirectory());
        Assertions.assertTrue(new File(String.format("server-packs/%s/config/testfile.txt", SERVERPACKHANDLER.getServerPackDestination())).exists());
        Assertions.assertTrue(new File(String.format("server-packs/%s/defaultconfigs/testfile.txt", SERVERPACKHANDLER.getServerPackDestination())).exists());
        Assertions.assertTrue(new File(String.format("server-packs/%s/mods/testmod.jar", SERVERPACKHANDLER.getServerPackDestination())).exists());
        Assertions.assertTrue(new File(String.format("server-packs/%s/scripts/testscript.zs", SERVERPACKHANDLER.getServerPackDestination())).exists());
        Assertions.assertTrue(new File(String.format("server-packs/%s/seeds/testjson.json", SERVERPACKHANDLER.getServerPackDestination())).exists());
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
        List<String> clientMods = new ArrayList<>();
        List<String> copyDirs = new ArrayList<>(Arrays.asList(
                "config",
                "mods",
                "scripts",
                "seeds",
                "defaultconfigs"
        ));
        SERVERPACKHANDLER.setServerPackDestination(modpackDir, "");
        SERVERPACKHANDLER.copyFiles(modpackDir, copyDirs, clientMods, "1.16.5");
        Assertions.assertTrue(new File(String.format("server-packs/%s/config", SERVERPACKHANDLER.getServerPackDestination())).isDirectory());
        Assertions.assertTrue(new File(String.format("server-packs/%s/mods", SERVERPACKHANDLER.getServerPackDestination())).isDirectory());
        Assertions.assertTrue(new File(String.format("server-packs/%s/scripts", SERVERPACKHANDLER.getServerPackDestination())).isDirectory());
        Assertions.assertTrue(new File(String.format("server-packs/%s/seeds", SERVERPACKHANDLER.getServerPackDestination())).isDirectory());
        Assertions.assertTrue(new File(String.format("server-packs/%s/defaultconfigs", SERVERPACKHANDLER.getServerPackDestination())).isDirectory());
        Assertions.assertTrue(new File(String.format("server-packs/%s/config/testfile.txt", SERVERPACKHANDLER.getServerPackDestination())).exists());
        Assertions.assertTrue(new File(String.format("server-packs/%s/defaultconfigs/testfile.txt", SERVERPACKHANDLER.getServerPackDestination())).exists());
        Assertions.assertTrue(new File(String.format("server-packs/%s/mods/testmod.jar", SERVERPACKHANDLER.getServerPackDestination())).exists());
        Assertions.assertTrue(new File(String.format("server-packs/%s/scripts/testscript.zs", SERVERPACKHANDLER.getServerPackDestination())).exists());
        Assertions.assertTrue(new File(String.format("server-packs/%s/seeds/testjson.json", SERVERPACKHANDLER.getServerPackDestination())).exists());
        for (String dir : copyDirs) {
            String deleteMe = (String.format("server-packs/%s/%s", SERVERPACKHANDLER.getServerPackDestination(), dir));
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
        List<String> result = SERVERPACKHANDLER.excludeClientMods("backend/test/resources/forge_tests/mods", clientMods, "1.16.5");
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
        DEFAULTFILES.filesSetup();
        String modpackDir = "./backend/test/resources/forge_tests";
        SERVERPACKHANDLER.setServerPackDestination(modpackDir, "");
        SERVERPACKHANDLER.copyIcon();
        Assertions.assertTrue(new File(String.format("server-packs/%s/server-icon.png", SERVERPACKHANDLER.getServerPackDestination())).exists());
        new File(String.format("server-packs/%s/server-icon.png", SERVERPACKHANDLER.getServerPackDestination())).delete();
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
        DEFAULTFILES.filesSetup();
        String modpackDir = "./backend/test/resources/forge_tests";
        SERVERPACKHANDLER.setServerPackDestination(modpackDir, "");
        SERVERPACKHANDLER.copyProperties();
        Assertions.assertTrue(new File(String.format("server-packs/%s/server.properties", SERVERPACKHANDLER.getServerPackDestination()  )).exists());
        new File(String.format("server-packs/%s/server.properties", SERVERPACKHANDLER.getServerPackDestination())).delete();
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
        SERVERPACKHANDLER.setServerPackDestination(modpackDir, "");
        SERVERPACKHANDLER.installServer(modLoader, minecraftVersion, modLoaderVersion, javaPath);
        Assertions.assertTrue(new File(String.format("server-packs/%s/fabric-server-launch.jar", SERVERPACKHANDLER.getServerPackDestination())).exists());
        Assertions.assertTrue(new File(String.format("server-packs/%s/server.jar", SERVERPACKHANDLER.getServerPackDestination())).exists());
        new File(String.format("server-packs/%s/fabric-server-launch.jar", SERVERPACKHANDLER.getServerPackDestination())).delete();
        new File(String.format("server-packs/%s/server.jar", SERVERPACKHANDLER.getServerPackDestination())).delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void installServerForgeTest() throws IOException {
        Files.createDirectories(Paths.get("server-packs/forge_tests"));
        String modLoader = "Forge";
        String modpackDir = "./backend/test/resources/forge_tests";
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
        SERVERPACKHANDLER.setServerPackDestination(modpackDir, "");
        SERVERPACKHANDLER.installServer(modLoader, minecraftVersion, modLoaderVersion, javaPath);
        Assertions.assertTrue(new File(String.format("server-packs/%s/1.16.5.json", SERVERPACKHANDLER.getServerPackDestination())).exists());
        Assertions.assertTrue(new File(String.format("server-packs/%s/forge.jar", SERVERPACKHANDLER.getServerPackDestination())).exists());
        Assertions.assertTrue(new File(String.format("server-packs/%s/minecraft_server.1.16.5.jar", SERVERPACKHANDLER.getServerPackDestination())).exists());
        new File(String.format("server-packs/%s/1.16.5.json", SERVERPACKHANDLER.getServerPackDestination())).delete();
        new File(String.format("server-packs/%s/forge.jar", SERVERPACKHANDLER.getServerPackDestination())).delete();
        new File(String.format("server-packs/%s/forge-installer.jar", SERVERPACKHANDLER.getServerPackDestination())).delete();
        new File(String.format("server-packs/%s/forge-installer.jar.log", SERVERPACKHANDLER.getServerPackDestination())).delete();
        new File(String.format("server-packs/%s/minecraft_server.1.16.5.jar", SERVERPACKHANDLER.getServerPackDestination())).delete();
        String delete = String.format("server-packs/%s/libraries", SERVERPACKHANDLER.getServerPackDestination());
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
        SERVERPACKHANDLER.setServerPackDestination(modpackDir, "");
        SERVERPACKHANDLER.zipBuilder(minecraftVersion, Boolean.TRUE);
        Assertions.assertTrue(new File("server-packs/fabric_tests_server_pack.zip").exists());
        Files.copy(Paths.get("./backend/test/resources/testresources/server_pack.zip"), Paths.get("./backend/test/resources/fabric_tests/server_pack.zip"), REPLACE_EXISTING);
    }

    @Test
    void zipBuilderForgeTest() throws IOException {
        Files.createDirectories(Paths.get("server-packs/forge_tests"));
        String minecraftVersion = "1.16.5";
        String modpackDir = "./backend/test/resources/forge_tests";
        SERVERPACKHANDLER.setServerPackDestination(modpackDir, "");
        SERVERPACKHANDLER.zipBuilder(minecraftVersion, Boolean.TRUE);
        Assertions.assertTrue(new File("server-packs/forge_tests_server_pack.zip").exists());
        Files.copy(Paths.get("./backend/test/resources/testresources/server_pack.zip"), Paths.get("./backend/test/resources/forge_tests/server_pack.zip"), REPLACE_EXISTING);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void downloadFabricJarTest() throws IOException {
        String modpackDir = "./backend/test/resources/fabric_tests";
        Files.createDirectories(Paths.get("server-packs/fabric_tests"));
        SERVERPACKHANDLER.setServerPackDestination(modpackDir, "");
        Assertions.assertTrue(SERVERPACKHANDLER.downloadFabricJar());
        Assertions.assertTrue(new File(String.format("server-packs/%s/fabric-installer.jar", SERVERPACKHANDLER.getServerPackDestination())).exists());
        new File(String.format("server-packs/%s/fabric-installer.jar", SERVERPACKHANDLER.getServerPackDestination())).delete();
        new File(String.format("server-packs/%s/fabric-installer.xml", SERVERPACKHANDLER.getServerPackDestination())).delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void downloadForgeJarTest() throws IOException {
        String modLoaderVersion = "36.1.2";
        String modpackDir = "./backend/test/resources/forge_tests";
        Files.createDirectories(Paths.get("server-packs/forge_tests"));
        String minecraftVersion = "1.16.5";
        SERVERPACKHANDLER.setServerPackDestination(modpackDir, "");
        SERVERPACKHANDLER.setServerPackDestination(modpackDir, "");
        Assertions.assertTrue(SERVERPACKHANDLER.downloadForgeJar(minecraftVersion, modLoaderVersion));
        Assertions.assertTrue(new File(String.format("server-packs/%s/forge-installer.jar", SERVERPACKHANDLER.getServerPackDestination())).exists());
        new File(String.format("server-packs/%s/forge-installer.jar", SERVERPACKHANDLER.getServerPackDestination())).delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void cleanUpServerPackForgeTest() throws IOException {
        String modLoader = "Forge";
        String modpackDir = "./backend/test/resources/forge_tests";
        String minecraftVersion = "1.16.5";
        String modLoaderVersion = "36.1.2";
        new File("server-packs/forge_tests/forge-1.16.5-36.1.2.jar").createNewFile();
        SERVERPACKHANDLER.setServerPackDestination(modpackDir, "");
        SERVERPACKHANDLER.cleanUpServerPack(
                new File(String.format("server-packs/%s/forge-installer.jar", SERVERPACKHANDLER.getServerPackDestination())),
                new File(String.format("server-packs/%s/forge-%s-%s.jar", SERVERPACKHANDLER.getServerPackDestination(), minecraftVersion, modLoaderVersion)),
                modLoader,
                minecraftVersion,
                modLoaderVersion);
        Assertions.assertFalse(new File("server-packs/forge_tests/forge-1.16.5-36.1.2.jar").exists());
        Assertions.assertFalse(new File("server-packs/forge_tests/forge-installer.jar").exists());
    }

    @Test
    void cleanUpServerPackFabricTest() {
        String modLoader = "Fabric";
        String modpackDir = "./backend/test/resources/fabric_tests";
        String minecraftVersion = "1.16.5";
        String modLoaderVersion = "36.1.2";
        SERVERPACKHANDLER.setServerPackDestination(modpackDir, "");
        SERVERPACKHANDLER.cleanUpServerPack(
                new File(String.format("server-packs/%s/fabric-installer.jar", SERVERPACKHANDLER.getServerPackDestination())),
                new File(String.format("server-packs/%s/forge-%s-%s.jar", SERVERPACKHANDLER.getServerPackDestination(), minecraftVersion, modLoaderVersion)),
                modLoader,
                minecraftVersion,
                modLoaderVersion);
        Assertions.assertFalse(new File("server-packs/forge_tests/fabric-installer.jar").exists());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void runConfigModelTest() throws IOException {
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
        configurationModel.setIncludeZipCreation(true);
        configurationModel.setModLoader("Forge");
        configurationModel.setModLoaderVersion("36.1.2");
        configurationModel.setMinecraftVersion("1.16.5");
        configurationModel.setJavaArgs("");
        DEFAULTFILES.filesSetup();
        ADDONSHANDLER.initializeAddons();
        SERVERPACKHANDLER.run(configurationModel);
        Assertions.assertTrue(new File("server-packs/forge_tests/libraries").isDirectory());
        Assertions.assertTrue(new File("server-packs/forge_tests/config").isDirectory());
        Assertions.assertTrue(new File("server-packs/forge_tests/defaultconfigs").isDirectory());
        Assertions.assertTrue(new File("server-packs/forge_tests/mods").isDirectory());
        Assertions.assertTrue(new File("server-packs/forge_tests/scripts").isDirectory());
        Assertions.assertTrue(new File("server-packs/forge_tests/seeds").isDirectory());

        Assertions.assertTrue(new File("server-packs/forge_tests/1.16.5.json").exists());
        Assertions.assertTrue(new File("server-packs/forge_tests/minecraft_server.1.16.5.jar").exists());
        Assertions.assertTrue(new File("server-packs/forge_tests/forge.jar").exists());
        Assertions.assertTrue(new File("server-packs/forge_tests/server.properties").exists());
        Assertions.assertTrue(new File("server-packs/forge_tests/server-icon.png").exists());
        Assertions.assertTrue(new File("server-packs/forge_tests/start.bat").exists());
        Assertions.assertTrue(new File("server-packs/forge_tests/start.sh").exists());

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
        new File("server-packs/forge_tests/forge.jar").delete();
        new File("server-packs/forge_tests/server.properties").delete();
        new File("server-packs/forge_tests/server-icon.png").delete();
        new File("server-packs/forge_tests/start.bat").delete();
        new File("server-packs/forge_tests/start.sh").delete();
        new File("./serverpackcreator.conf").delete();
        Files.copy(Paths.get("./backend/test/resources/testresources/server_pack.zip"), Paths.get("server-packs/forge_tests_server_pack.zip"), REPLACE_EXISTING);
    }
}
