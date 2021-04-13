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
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

class FilesSetupTest {
    @Mock
    Logger appLogger;

    @InjectMocks
    FilesSetup filesSetup;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testRenameOldConfig() throws IOException {
        File oldConfigFile = new File("creator.conf");
        oldConfigFile.createNewFile();
        Assertions.assertFalse(Reference.filesSetup.checkForConfig());
        Assertions.assertTrue(new File("serverpackcreator.conf").exists());
        new File("serverpackcreator.conf").delete();
        new File("creator.conf").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testCheckForConfig() throws IOException {
        File configFile = new File("serverpackcreator.conf");
        configFile.createNewFile();
        Assertions.assertFalse(Reference.filesSetup.checkForConfig());
        configFile.delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testCheckForConfigNew() {
        File configFile = new File("serverpackcreator.conf");
        Assertions.assertTrue(Reference.filesSetup.checkForConfig());
        configFile.delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testCheckForFabricLinux() throws IOException {
        File fabricLinux = new File("start-fabric.sh");
        fabricLinux.createNewFile();
        Assertions.assertFalse(Reference.filesSetup.checkForFabricLinux());
        fabricLinux.delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testCheckForFabricLinuxNew() {
        File fabricLinux = new File("start-fabric.sh");
        Assertions.assertTrue(Reference.filesSetup.checkForFabricLinux());
        fabricLinux.delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testCheckForFabricWindows() throws IOException {
        File fabricWindows = new File("start-fabric.bat");
        fabricWindows.createNewFile();
        Assertions.assertFalse(Reference.filesSetup.checkForFabricWindows());
        fabricWindows.delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testCheckForFabricWindowsNew() {
        File fabricWindows = new File("start-fabric.bat");
        Assertions.assertTrue(Reference.filesSetup.checkForFabricWindows());
        fabricWindows.delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testCheckForForgeLinux() throws IOException {
        File forgeLinux = new File("start-forge.sh");
        forgeLinux.createNewFile();
        Assertions.assertFalse(Reference.filesSetup.checkForForgeLinux());
        forgeLinux.delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testCheckForForgeLinuxNew() {
        File forgeLinux = new File("start-forge.sh");
        Assertions.assertTrue(Reference.filesSetup.checkForForgeLinux());
        forgeLinux.delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testCheckForForgeWindows() throws IOException {
        File forgeWindows = new File("start-forge.bat");
        forgeWindows.createNewFile();
        Assertions.assertFalse(Reference.filesSetup.checkForForgeWindows());
        forgeWindows.delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testCheckForForgeWindowsNew() {
        File forgeWindows = new File("start-forge.bat");
        Assertions.assertTrue(Reference.filesSetup.checkForForgeWindows());
        forgeWindows.delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testCheckForProperties() throws IOException {
        File properties = new File("server.properties");
        properties.createNewFile();
        Assertions.assertFalse(Reference.filesSetup.checkForProperties());
        properties.delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testCheckForPropertiesNew() {
        File properties = new File("server.properties");
        Assertions.assertTrue(Reference.filesSetup.checkForProperties());
        properties.delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testCheckForIcon() throws IOException {
        File icon = new File("server-icon.png");
        icon.createNewFile();
        Assertions.assertFalse(Reference.filesSetup.checkForIcon());
        icon.delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testCheckForIconNew() {
        File icon = new File("server-icon.png");
        Assertions.assertTrue(Reference.filesSetup.checkForIcon());
        icon.delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testFilesSetup() throws IOException {
        Reference.filesSetup.filesSetup();
        Assertions.assertTrue(new File("./server_files").isDirectory());
        Assertions.assertTrue(new File("./server_files/server.properties").exists());
        Assertions.assertTrue(new File("./server_files/server-icon.png").exists());
        Assertions.assertTrue(new File("./server_files/start-fabric.bat").exists());
        Assertions.assertTrue(new File("./server_files/start-fabric.sh").exists());
        Assertions.assertTrue(new File("./server_files/start-forge.bat").exists());
        Assertions.assertTrue(new File("./server_files/start-forge.sh").exists());
        Assertions.assertTrue(new File("./serverpackcreator.conf").exists());
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
    void testWriteConfigToFileFabric() {
        String modpackDir = "./src/test/resources/fabric_tests";
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
        String minecraftVersion = "1.16.5";
        String modLoader = "Fabric";
        String modLoaderVersion = "0.11.3";
        boolean result = Reference.filesSetup.writeConfigToFile(
                modpackDir,
                Reference.cliSetup.buildString(clientMods.toString()),
                Reference.cliSetup.buildString(copyDirs.toString()),
                true,
                javaPath,
                minecraftVersion,
                modLoader,
                modLoaderVersion,
                true,
                true,
                true,
                true
        );
        Assertions.assertTrue(result);
        Assertions.assertTrue(new File("./serverpackcreator.conf").exists());
        new File("./serverpackcreator.conf").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testWriteConfigToFileForge() {
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
        String javaPath = "/use/bin/java";
        String minecraftVersion = "1.16.5";
        String modLoader = "Forge";
        String modLoaderVersion = "36.1.2";
        boolean result = Reference.filesSetup.writeConfigToFile(
                modpackDir,
                Reference.cliSetup.buildString(clientMods.toString()),
                Reference.cliSetup.buildString(copyDirs.toString()),
                true,
                javaPath,
                minecraftVersion,
                modLoader,
                modLoaderVersion,
                true,
                true,
                true,
                true
        );
        Assertions.assertTrue(result);
        Assertions.assertTrue(new File("./serverpackcreator.conf").exists());
        new File("./serverpackcreator.conf").delete();
    }
}