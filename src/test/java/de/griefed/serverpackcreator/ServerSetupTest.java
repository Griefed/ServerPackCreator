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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

class ServerSetupTest {
    @Mock
    Logger appLogger;

    @Mock
    Logger installerLogger;

    @InjectMocks
    ServerSetup serverSetup;

    @BeforeEach
    void setUp() {
        Reference.filesSetup.checkLocaleFile();
        MockitoAnnotations.openMocks(this);
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
            Reference.serverUtilities.downloadFabricJar(modpackDir);
            Reference.serverSetup.installServer(modLoader, modpackDir, minecraftVersion, modLoaderVersion, javaPath);
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
            Reference.serverUtilities.downloadForgeJar(minecraftVersion, modLoaderVersion, modpackDir);
            Reference.serverSetup.installServer(modLoader, modpackDir, minecraftVersion, modLoaderVersion, javaPath);
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
    void testZipBuilderFabric() {
        //TODO: Figure out how to run this test on GitHub Runners
        if (!new File("/home/runner").isDirectory()) {
            String modLoader = "Fabric";
            String modpackDir = "./src/test/resources/fabric_tests";
            Reference.serverSetup.zipBuilder(modpackDir, modLoader, Boolean.TRUE);
        }
    }

    @Test
    void testZipBuilderForge() {
        //TODO: Figure out how to run this test on GitHub Runners
        if (!new File("/home/runner").isDirectory()) {
            String modLoader = "Forge";
            String modpackDir = "./src/test/resources/forge_tests";
            Reference.serverSetup.zipBuilder(modpackDir, modLoader, Boolean.TRUE);
        }
    }
}