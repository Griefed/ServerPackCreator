package de.griefed.serverpackcreator;

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
import java.nio.file.Paths;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

class ServerUtilitiesTest {
    @Mock
    Logger appLogger;

    @InjectMocks
    ServerUtilities serverUtilities;

    @BeforeEach
    void setUp() {
        Reference.filesSetup.checkLocaleFile();
        MockitoAnnotations.openMocks(this);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testGenerateDownloadScriptsFabric() {
        //TODO: Figure out how to run this test on GitHub Runners
        if (!new File("/home/runner").isDirectory()) {
            String modLoader = "Fabric";
            String modpackDir = "./src/test/resources/fabric_tests";
            String minecraftVersion = "1.16.5";
            Reference.serverUtilities.generateDownloadScripts(modLoader, modpackDir, minecraftVersion);
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
            Reference.serverUtilities.generateDownloadScripts(modLoader, modpackDir, minecraftVersion);
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
            boolean result = Reference.serverUtilities.downloadFabricJar(modpackDir);
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
            boolean result = Reference.serverUtilities.downloadForgeJar(minecraftVersion, modLoaderVersion, modpackDir);
            Assertions.assertTrue(result);
            Assertions.assertTrue(new File(String.format("%s/server_pack/forge-installer.jar", modpackDir)).exists());
            new File(String.format("%s/server_pack/forge-installer.jar", modpackDir)).delete();
        }
    }

    @Test
    void testDeleteMinecraftJarFabric() throws IOException {
        Files.copy(Paths.get("./src/test/resources/testresources/server_pack.zip"), Paths.get("./src/test/resources/fabric_tests/server_pack.zip"), REPLACE_EXISTING);
        String modLoader = "Fabric";
        String modpackDir = "./src/test/resources/forge_tests";
        Reference.serverUtilities.deleteMinecraftJar(modLoader, modpackDir);
    }

    @Test
    void testDeleteMinecraftJarForge() throws IOException {
        Files.copy(Paths.get("./src/test/resources/testresources/server_pack.zip"), Paths.get("./src/test/resources/forge_tests/server_pack.zip"), REPLACE_EXISTING);
        String modLoader = "Forge";
        String modpackDir = "./src/test/resources/forge_tests";
        Reference.serverUtilities.deleteMinecraftJar(modLoader, modpackDir);
    }

    @Test
    void testCleanUpServerPackForge() {
        String modLoader = "Forge";
        String modpackDir = "./src/test/resources/forge_tests";
        String minecraftVersion = "1.16.5";
        String modLoaderVersion = "36.1.2";
        Reference.serverUtilities.cleanUpServerPack(
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
        Reference.serverUtilities.cleanUpServerPack(
                new File(String.format("%s/server_pack/fabric-installer.jar", modpackDir)),
                new File(String.format("%s/server_pack/forge-%s-%s.jar", modpackDir, minecraftVersion, modLoaderVersion)),
                modLoader,
                modpackDir,
                minecraftVersion,
                modLoaderVersion);
    }
}