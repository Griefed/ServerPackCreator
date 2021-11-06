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
import de.griefed.serverpackcreator.spring.models.ServerPack;
import de.griefed.serverpackcreator.utilities.VersionLister;
import jdk.nashorn.internal.ir.annotations.Ignore;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

class ServerPackHandlerTest {

    private final ServerPackHandler SERVERPACKHANDLER;
    private final DefaultFiles DEFAULTFILES;
    private final CurseCreateModpack CURSECREATEMODPACK;
    private final LocalizationManager LOCALIZATIONMANAGER;
    private final ConfigurationHandler CONFIGURATIONHANDLER;
    private final AddonsHandler ADDONSHANDLER;
    private final VersionLister VERSIONLISTER;
    private ApplicationProperties serverPackCreatorProperties;

    ServerPackHandlerTest() {
        try {
            FileUtils.copyFile(new File("backend/main/resources/serverpackcreator.properties"),new File("serverpackcreator.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.serverPackCreatorProperties = new ApplicationProperties();

        LOCALIZATIONMANAGER = new LocalizationManager(serverPackCreatorProperties);
        LOCALIZATIONMANAGER.init();
        DEFAULTFILES = new DefaultFiles(LOCALIZATIONMANAGER, serverPackCreatorProperties);
        DEFAULTFILES.filesSetup();
        VERSIONLISTER = new VersionLister(serverPackCreatorProperties);
        ADDONSHANDLER = new AddonsHandler(LOCALIZATIONMANAGER, serverPackCreatorProperties);
        CURSECREATEMODPACK = new CurseCreateModpack(LOCALIZATIONMANAGER, serverPackCreatorProperties);
        CONFIGURATIONHANDLER = new ConfigurationHandler(LOCALIZATIONMANAGER, CURSECREATEMODPACK, VERSIONLISTER, serverPackCreatorProperties);
        SERVERPACKHANDLER = new ServerPackHandler(LOCALIZATIONMANAGER, CURSECREATEMODPACK, ADDONSHANDLER, CONFIGURATIONHANDLER, serverPackCreatorProperties, VERSIONLISTER);

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
        configurationModel.setModpackDir("./backend/test/resources/forge_tests");
        SERVERPACKHANDLER.run(serverPackCreatorProperties.FILE_CONFIG, configurationModel);
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
        String destination = modpackDir.substring(modpackDir.lastIndexOf("/") + 1);
        Files.copy(Paths.get("./backend/test/resources/testresources/server_pack.zip"), Paths.get("server-packs/forge_tests_server_pack.zip"), REPLACE_EXISTING);
        SERVERPACKHANDLER.cleanupEnvironment(true, destination);
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
        SERVERPACKHANDLER.createStartScripts(modLoader, "empty", "1.16.5",  "0.12.1", destination);
        Assertions.assertTrue(new File(String.format("server-packs/%s/start.bat", destination)).exists());
        Assertions.assertTrue(new File(String.format("server-packs/%s/start.sh", destination)).exists());
        new File(String.format("server-packs/%s/start.bat", destination)).delete();
        new File(String.format("server-packs/%s/start.sh", destination)).delete();
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
        String destination = modpackDir.substring(modpackDir.lastIndexOf("/") + 1);
        SERVERPACKHANDLER.createStartScripts(modLoader, "empty", "1.16.5","36.2.4", destination);
        Assertions.assertTrue(new File(String.format("server-packs/%s/start.bat", destination)).exists());
        Assertions.assertTrue(new File(String.format("server-packs/%s/start.sh", destination)).exists());
        new File(String.format("server-packs/%s/start.bat", destination)).delete();
        new File(String.format("server-packs/%s/start.sh", destination)).delete();
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
        SERVERPACKHANDLER.copyFiles(modpackDir, copyDirs, clientMods, "1.16.5", destination);
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
        List<String> clientMods = new ArrayList<>();
        List<String> copyDirs = new ArrayList<>(Arrays.asList(
                "config",
                "mods",
                "scripts",
                "seeds",
                "defaultconfigs"
        ));
        String destination = modpackDir.substring(modpackDir.lastIndexOf("/") + 1);
        SERVERPACKHANDLER.copyFiles(modpackDir, copyDirs, clientMods, "1.16.5", destination);
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
        String destination = modpackDir.substring(modpackDir.lastIndexOf("/") + 1);
        SERVERPACKHANDLER.copyIcon(destination);
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
        DEFAULTFILES.filesSetup();
        String modpackDir = "./backend/test/resources/forge_tests";
        String destination = modpackDir.substring(modpackDir.lastIndexOf("/") + 1);
        SERVERPACKHANDLER.copyProperties(destination);
        Assertions.assertTrue(new File(String.format("server-packs/%s/server.properties", destination)).exists());
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
        String destination = modpackDir.substring(modpackDir.lastIndexOf("/") + 1);
        SERVERPACKHANDLER.installServer(modLoader, minecraftVersion, modLoaderVersion, javaPath, destination);
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
        String destination = modpackDir.substring(modpackDir.lastIndexOf("/") + 1);
        SERVERPACKHANDLER.installServer(modLoader, minecraftVersion, modLoaderVersion, javaPath, destination);
        Assertions.assertTrue(new File(String.format("server-packs/%s/1.16.5.json", destination)).exists());
        Assertions.assertTrue(new File(String.format("server-packs/%s/forge.jar", destination)).exists());
        Assertions.assertTrue(new File(String.format("server-packs/%s/minecraft_server.1.16.5.jar", destination)).exists());
        new File(String.format("server-packs/%s/1.16.5.json", destination)).delete();
        new File(String.format("server-packs/%s/forge.jar", destination)).delete();
        new File(String.format("server-packs/%s/forge-installer.jar", destination)).delete();
        new File(String.format("server-packs/%s/forge-installer.jar.log", destination)).delete();
        new File(String.format("server-packs/%s/minecraft_server.1.16.5.jar", destination)).delete();
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
        String destination = modpackDir.substring(modpackDir.lastIndexOf("/") + 1);
        SERVERPACKHANDLER.zipBuilder(minecraftVersion, true, destination);
        Assertions.assertTrue(new File("server-packs/fabric_tests_server_pack.zip").exists());
        Files.copy(Paths.get("./backend/test/resources/testresources/server_pack.zip"), Paths.get("./backend/test/resources/fabric_tests/server_pack.zip"), REPLACE_EXISTING);
    }

    @Test
    void zipBuilderForgeTest() throws IOException {
        Files.createDirectories(Paths.get("server-packs/forge_tests"));
        String minecraftVersion = "1.16.5";
        String modpackDir = "./backend/test/resources/forge_tests";
        String destination = modpackDir.substring(modpackDir.lastIndexOf("/") + 1);
        SERVERPACKHANDLER.zipBuilder(minecraftVersion, true, destination);
        Assertions.assertTrue(new File("server-packs/forge_tests_server_pack.zip").exists());
        Files.copy(Paths.get("./backend/test/resources/testresources/server_pack.zip"), Paths.get("./backend/test/resources/forge_tests/server_pack.zip"), REPLACE_EXISTING);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void downloadFabricJarTest() throws IOException {
        String modpackDir = "./backend/test/resources/fabric_tests";
        Files.createDirectories(Paths.get("server-packs/fabric_tests"));
        String destination = modpackDir.substring(modpackDir.lastIndexOf("/") + 1);
        Assertions.assertTrue(SERVERPACKHANDLER.downloadFabricJar(destination));
        Assertions.assertTrue(new File(String.format("server-packs/%s/fabric-installer.jar", destination)).exists());
        new File(String.format("server-packs/%s/fabric-installer.jar", destination)).delete();
        new File(String.format("server-packs/%s/fabric-installer.xml", destination)).delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void downloadForgeJarTest() throws IOException {
        String modLoaderVersion = "36.1.2";
        String modpackDir = "./backend/test/resources/forge_tests";
        Files.createDirectories(Paths.get("server-packs/forge_tests"));
        String minecraftVersion = "1.16.5";
        String destination = modpackDir.substring(modpackDir.lastIndexOf("/") + 1);
        Assertions.assertTrue(SERVERPACKHANDLER.downloadForgeJar(minecraftVersion, modLoaderVersion, destination));
        Assertions.assertTrue(new File(String.format("server-packs/%s/forge-installer.jar", destination)).exists());
        new File(String.format("server-packs/%s/forge-installer.jar", destination)).delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void cleanUpServerPackForgeTest() throws IOException {
        String modLoader = "Forge";
        String modpackDir = "./backend/test/resources/forge_tests";
        String minecraftVersion = "1.16.5";
        String modLoaderVersion = "36.1.2";
        new File("server-packs/forge_tests/forge-1.16.5-36.1.2.jar").createNewFile();
        String destination = modpackDir.substring(modpackDir.lastIndexOf("/") + 1);
        SERVERPACKHANDLER.cleanUpServerPack(
                new File(String.format("server-packs/%s/forge-installer.jar", destination)),
                new File(String.format("server-packs/%s/forge-%s-%s.jar", destination, minecraftVersion, modLoaderVersion)),
                modLoader,
                minecraftVersion,
                modLoaderVersion,
                destination);
        Assertions.assertFalse(new File("server-packs/forge_tests/forge-1.16.5-36.1.2.jar").exists());
        Assertions.assertFalse(new File("server-packs/forge_tests/forge-installer.jar").exists());
    }

    @Test
    void cleanUpServerPackFabricTest() {
        String modLoader = "Fabric";
        String modpackDir = "./backend/test/resources/fabric_tests";
        String minecraftVersion = "1.16.5";
        String modLoaderVersion = "36.1.2";
        String destination = modpackDir.substring(modpackDir.lastIndexOf("/") + 1);
        SERVERPACKHANDLER.cleanUpServerPack(
                new File(String.format("server-packs/%s/fabric-installer.jar", destination)),
                new File(String.format("server-packs/%s/forge-%s-%s.jar", destination, minecraftVersion, modLoaderVersion)),
                modLoader,
                minecraftVersion,
                modLoaderVersion,
                destination);
        Assertions.assertFalse(new File("server-packs/forge_tests/fabric-installer.jar").exists());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Ignore
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
        ServerPack serverPack = new ServerPack();
        serverPack.setModpackDir("./backend/test/resources/forge_tests");
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
        ADDONSHANDLER.initializeAddons();
        SERVERPACKHANDLER.run(serverPack);
        Assertions.assertFalse(new File("server-packs/forge_tests/libraries").isDirectory());
        Assertions.assertFalse(new File("server-packs/forge_tests/config").isDirectory());
        Assertions.assertFalse(new File("server-packs/forge_tests/defaultconfigs").isDirectory());
        Assertions.assertFalse(new File("server-packs/forge_tests/mods").isDirectory());
        Assertions.assertFalse(new File("server-packs/forge_tests/scripts").isDirectory());
        Assertions.assertFalse(new File("server-packs/forge_tests/seeds").isDirectory());

        Assertions.assertFalse(new File("server-packs/forge_tests/1.16.5.json").exists());
        Assertions.assertFalse(new File("server-packs/forge_tests/minecraft_server.1.16.5.jar").exists());
        Assertions.assertFalse(new File("server-packs/forge_tests/forge.jar").exists());
        Assertions.assertFalse(new File("server-packs/forge_tests/server.properties").exists());
        Assertions.assertFalse(new File("server-packs/forge_tests/server-icon.png").exists());
        Assertions.assertFalse(new File("server-packs/forge_tests/start.bat").exists());
        Assertions.assertFalse(new File("server-packs/forge_tests/start.sh").exists());

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
