package de.griefed.ServerPackCreator;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

class ServerSetupTest {
    @Mock
    Logger appLogger;
    @Mock
    Logger installerLogger;
    @InjectMocks
    ServerSetup serverSetup;
    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testDeleteClientMods() throws IOException {
        String modpackDir = "./src/test/resources/forge_tests";
        Files.createDirectories(Paths.get(String.format("%s/server_pack",modpackDir)));
        Files.createDirectories(Paths.get(String.format("%s/server_pack/mods",modpackDir)));
        List<String> clientMods = Arrays.asList("AmbientSounds","BackTools","BetterAdvancement","BetterPing","cherished","ClientTweaks","Controlling","DefaultOptions","durability","DynamicSurroundings","itemzoom","jei-professions","jeiintegration","JustEnoughResources","MouseTweaks","Neat","OldJavaWarning","PackMenu","preciseblockplacing","SimpleDiscordRichPresence","SpawnerFix","TipTheScales","WorldNameRandomizer");
        for (int i = 0; i < clientMods.toArray().length; i++) {
            File file = new File(String.format("%s/server_pack/mods/%s.jar",modpackDir,clientMods.get(i)));
            file.createNewFile();
        }
        ServerSetup.deleteClientMods(modpackDir, clientMods);
    }
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testInstallServerFabric() {
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
        } else if (new File("/opt/hostedtoolcache/jdk/8.0.282/x64/bin/java").exists()) {
            javaPath = "/opt/hostedtoolcache/jdk/8.0.282/x64/bin/java";
        } else {
            javaPath = autoJavaPath;
        }
        ServerUtilities.downloadFabricJar(modpackDir);
        ServerSetup.installServer(modLoader, modpackDir, minecraftVersion, modLoaderVersion, javaPath);
        Assertions.assertTrue(new File(String.format("%s/server_pack/fabric-server-launch.jar",modpackDir)).exists());
        Assertions.assertTrue(new File(String.format("%s/server_pack/server.jar",modpackDir)).exists());
        new File(String.format("%s/server_pack/fabric-server-launch.jar",modpackDir)).delete();
        new File(String.format("%s/server_pack/server.jar",modpackDir)).delete();
    }
    /*
    TODO: Figure out how to run this test on GitHub infrastructure.
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testInstallServerForge() throws IOException {
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
        } else if (new File("/opt/hostedtoolcache/jdk/8.0.282/x64/bin/java").exists()) {
            javaPath = "/opt/hostedtoolcache/jdk/8.0.282/x64/bin/java";
        } else {
            javaPath = autoJavaPath;
        }
        ServerUtilities.downloadForgeJar(minecraftVersion, modLoaderVersion, modpackDir);
        ServerSetup.installServer(modLoader, modpackDir, minecraftVersion, modLoaderVersion, javaPath);
        Assertions.assertTrue(new File(String.format("%s/server_pack/1.16.5.json",modpackDir)).exists());
        Assertions.assertTrue(new File(String.format("%s/server_pack/forge.jar", modpackDir)).exists());
        Assertions.assertTrue(new File(String.format("%s/server_pack/minecraft_server.1.16.5.jar", modpackDir)).exists());
        Assertions.assertTrue(new File(String.format("%s/server_pack/download_minecraft-server.jar_forge.bat",modpackDir)).exists());
        Assertions.assertTrue(new File(String.format("%s/server_pack/download_minecraft-server.jar_forge.sh",modpackDir)).exists());
        new File(String.format("%s/server_pack/1.16.5.json", modpackDir)).delete();
        new File(String.format("%s/server_pack/forge.jar", modpackDir)).delete();
        new File(String.format("%s/server_pack/forge-installer.jar", modpackDir)).delete();
        new File(String.format("%s/server_pack/forge-installer.jar.log", modpackDir)).delete();
        new File(String.format("%s/server_pack/minecraft_server.1.16.5.jar", modpackDir)).delete();
        new File(String.format("%s/server_pack/download_minecraft-server.jar_forge.bat",modpackDir)).delete();
        new File(String.format("%s/server_pack/download_minecraft-server.jar_forge.sh",modpackDir)).delete();
        String delete = String.format("%s/server_pack/libraries", modpackDir);
        if (new File(delete).isDirectory()) {
            Path pathToBeDeleted = Paths.get(delete);
            Files.walk(pathToBeDeleted).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
    }
     */

    /*
    TODO: Figure out how to run this test on GitHub infrastructure.
    @Test
    void testZipBuilderFabric() {
        String modLoader = "Fabric";
        String modpackDir = "./src/test/resources/fabric_tests";
        ServerSetup.zipBuilder(modpackDir, modLoader, Boolean.TRUE);
    }
     */

    /*
    TODO: Figure out how to run this test on GitHub infrastructure.
    @Test
    void testZipBuilderForge() {
        String modLoader = "Forge";
        String modpackDir = "./src/test/resources/forge_tests";
        ServerSetup.zipBuilder(modpackDir, modLoader, Boolean.TRUE);
    }
     */
}