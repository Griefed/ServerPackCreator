package de.griefed.serverpackcreator;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

class CreateServerPackTest {
    @Mock
    Logger appLogger;

    private CreateServerPack createServerPack;
    private FilesSetup filesSetup;
    private LocalizationManager localizationManager;
    private Configuration configCheck;

    CreateServerPackTest() {
        localizationManager = new LocalizationManager();
        configCheck = new Configuration(localizationManager);
        createServerPack = new CreateServerPack(localizationManager, configCheck);
        filesSetup = new FilesSetup(localizationManager);
    }

    @BeforeEach
    void setUp() {
        localizationManager.checkLocaleFile();
        MockitoAnnotations.openMocks(this);
    }

    @Mock
    Logger installerLogger;


    @Test
    void testCleanupEnvironment() throws IOException {
        //TODO: Figure out how to run this test on GitHub Runners
        if (!new File("/home/runner").isDirectory()) {
            String modpackDir = "./src/test/resources/cleanup_tests";
            Files.copy(Paths.get("./src/test/resources/testresources/server_pack.zip"), Paths.get("./src/test/resources/cleanup_tests/server_pack.zip"), REPLACE_EXISTING);
            Files.createDirectories(Paths.get(String.format("%s/server_pack", modpackDir)));
            createServerPack.cleanupEnvironment(modpackDir);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testCopyStartScriptsFabric() throws IOException {
        //TODO: Figure out how to run this test on GitHub Runners
        if (!new File("/home/runner").isDirectory()) {
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
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testCopyStartScriptsForge() throws IOException {
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
    void testCopyFiles() throws IOException {
        String modpackDir = "./src/test/resources/forge_tests";
        List<String> clientMods = Arrays.asList(
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
        );
        List<String> copyDirs = Arrays.asList(
                "config",
                "mods",
                "scripts",
                "seeds",
                "defaultconfigs"
        );
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
    void testCopyFilesEmptyClients() throws IOException {
        String modpackDir = "./src/test/resources/forge_tests";
        List<String> clientMods = new ArrayList<>();
        List<String> copyDirs = Arrays.asList(
                "config",
                "mods",
                "scripts",
                "seeds",
                "defaultconfigs"
        );
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
    void testCopyIcon() throws IOException {
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
    void testCopyProperties() throws IOException {
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
    void testInstallServerFabric() {
        //TODO: Figure out how to run this test on GitHub Runners
        if (!new File("/home/runner").isDirectory()) {
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
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testInstallServerForge() throws IOException {
        //TODO: Figure out how to run this test on GitHub Runners
        if (!new File("/home/runner").isDirectory()) {
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
    }

    @Test
    void testZipBuilderFabric() throws IOException {
        //TODO: Figure out how to run this test on GitHub Runners
        if (!new File("/home/runner").isDirectory()) {
            Files.copy(Paths.get("./src/test/resources/testresources/server_pack.zip"), Paths.get("./src/test/resources/fabric_tests/server_pack.zip"), REPLACE_EXISTING);
            String minecraftVersion = "1.16.5";
            String modLoader = "Fabric";
            String modpackDir = "src/test/resources/fabric_tests";
            createServerPack.zipBuilder(modpackDir, modLoader, Boolean.TRUE, minecraftVersion);
        }
    }

    @Test
    void testZipBuilderForge() throws IOException {
        //TODO: Figure out how to run this test on GitHub Runners
        if (!new File("/home/runner").isDirectory()) {
            Files.copy(Paths.get("./src/test/resources/testresources/server_pack.zip"), Paths.get("./src/test/resources/forge_tests/server_pack.zip"), REPLACE_EXISTING);
            String minecraftVersion = "1.16.5";
            String modLoader = "Forge";
            String modpackDir = "./src/test/resources/forge_tests";
            createServerPack.zipBuilder(modpackDir, modLoader, Boolean.TRUE, minecraftVersion);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testGenerateDownloadScriptsFabric() {
        //TODO: Figure out how to run this test on GitHub Runners
        if (!new File("/home/runner").isDirectory()) {
            String modLoader = "Fabric";
            String modpackDir = "./src/test/resources/fabric_tests";
            String minecraftVersion = "1.16.5";
            createServerPack.generateDownloadScripts(modLoader, modpackDir, minecraftVersion);
            Assertions.assertTrue(new File(String.format("%s/server_pack/download_minecraft-server.jar_fabric.bat", modpackDir)).exists());
            Assertions.assertTrue(new File(String.format("%s/server_pack/download_minecraft-server.jar_fabric.sh", modpackDir)).exists());
            new File(String.format("%s/server_pack/download_minecraft-server.jar_fabric.bat", modpackDir)).delete();
            new File(String.format("%s/server_pack/download_minecraft-server.jar_fabric.sh", modpackDir)).delete();
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testGenerateDownloadScriptsForge() {
        //TODO: Figure out how to run this test on GitHub Runners
        if (!new File("/home/runner").isDirectory()) {
            String modLoader = "Forge";
            String modpackDir = "./src/test/resources/forge_tests";
            String minecraftVersion = "1.16.5";
            createServerPack.generateDownloadScripts(modLoader, modpackDir, minecraftVersion);
            Assertions.assertTrue(new File(String.format("%s/server_pack/download_minecraft-server.jar_forge.bat", modpackDir)).exists());
            Assertions.assertTrue(new File(String.format("%s/server_pack/download_minecraft-server.jar_forge.sh", modpackDir)).exists());
            new File(String.format("%s/server_pack/download_minecraft-server.jar_forge.bat", modpackDir)).delete();
            new File(String.format("%s/server_pack/download_minecraft-server.jar_forge.sh", modpackDir)).delete();
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testDownloadFabricJar() {
        //TODO: Figure out how to run this test on GitHub Runners
        if (!new File("/home/runner").isDirectory()) {
            String modpackDir = "./src/test/resources/fabric_tests";
            boolean result = createServerPack.downloadFabricJar(modpackDir);
            Assertions.assertTrue(result);
            Assertions.assertTrue(new File(String.format("%s/server_pack/fabric-installer.jar", modpackDir)).exists());
            new File(String.format("%s/server_pack/fabric-installer.jar", modpackDir)).delete();
            new File(String.format("%s/server_pack/fabric-installer.xml", modpackDir)).delete();
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testDownloadForgeJar() {
        //TODO: Figure out how to run this test on GitHub Runners
        if (!new File("/home/runner").isDirectory()) {
            String modLoaderVersion = "36.1.2";
            String modpackDir = "./src/test/resources/forge_tests";
            String minecraftVersion = "1.16.5";
            boolean result = createServerPack.downloadForgeJar(minecraftVersion, modLoaderVersion, modpackDir);
            Assertions.assertTrue(result);
            Assertions.assertTrue(new File(String.format("%s/server_pack/forge-installer.jar", modpackDir)).exists());
            new File(String.format("%s/server_pack/forge-installer.jar", modpackDir)).delete();
        }
    }

    @Test
    void testDeleteMinecraftJarFabric() throws IOException {
        Files.copy(Paths.get("./src/test/resources/testresources/server_pack.zip"), Paths.get("./src/test/resources/fabric_tests/server_pack.zip"), REPLACE_EXISTING);
        String minecraftVersion = "1.16.5";
        String modLoader = "Fabric";
        String modpackDir = "./src/test/resources/fabric_tests";
        createServerPack.deleteMinecraftJar(modLoader, modpackDir, minecraftVersion);
    }

    @Test
    void testDeleteMinecraftJarForge() throws IOException {
        Files.copy(Paths.get("./src/test/resources/testresources/server_pack.zip"), Paths.get("./src/test/resources/forge_tests/server_pack.zip"), REPLACE_EXISTING);
        String minecraftVersion = "1.16.5";
        String modLoader = "Forge";
        String modpackDir = "./src/test/resources/forge_tests";
        createServerPack.deleteMinecraftJar(modLoader, modpackDir, minecraftVersion);
    }

    @Test
    void testCleanUpServerPackForge() {
        String modLoader = "Forge";
        String modpackDir = "./src/test/resources/forge_tests";
        String minecraftVersion = "1.16.5";
        String modLoaderVersion = "36.1.2";
        createServerPack.cleanUpServerPack(
                new File(String.format("%s/server_pack/fabric-installer.jar", modpackDir)),
                new File(String.format("%s/server_pack/forge-%s-%s.jar", modpackDir, minecraftVersion, modLoaderVersion)),
                modLoader,
                modpackDir,
                minecraftVersion,
                modLoaderVersion);
    }

    @Test
    void testCleanUpServerPackFabric() {
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
    }
}
