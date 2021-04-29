package de.griefed.serverpackcreator;

import com.typesafe.config.ConfigFactory;
import de.griefed.serverpackcreator.curseforgemodpack.CurseCreateModpack;
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
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

class ConfigCheckTest {
    @Mock
    Logger appLogger;

    private Configuration configCheck;
    private CurseCreateModpack curseCreateModpack;
    private LocalizationManager localizationManager;

    ConfigCheckTest() {
        localizationManager = new LocalizationManager();
        curseCreateModpack = new CurseCreateModpack(localizationManager);
        configCheck = new Configuration(localizationManager, curseCreateModpack);
    }

    @BeforeEach
    void setUp() {
        localizationManager.checkLocaleFile();
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testBuildString() {
        List<String> args = Arrays.asList(
                "config",
                "mods",
                "scripts",
                "seeds",
                "defaultconfigs"
        );
        String result = configCheck.buildString(args.toString());
        Assertions.assertEquals(args.toString(), String.format("[%s]",result));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testCheckConfig() throws IOException {
        Files.copy(Paths.get("./src/test/resources/testresources/serverpackcreator.conf"), Paths.get("./serverpackcreator.conf"), REPLACE_EXISTING);
        configCheck.setConfig(ConfigFactory.parseFile(configCheck.getConfigFile()));
        boolean result = configCheck.checkConfigFile(configCheck.getConfigFile());
        Assertions.assertFalse(result);
        new File("./serverpackcreator.conf").delete();
    }

    @Test
    void testCheckCurseForgeCorrect() {
        String modpackDir = "238298,3174854";
        boolean result = configCheck.checkCurseForge(modpackDir);
        Assertions.assertTrue(result);
    }

    @Test
    void testCheckCurseForgeFalse() {
        String modpackDir = "1,1234";
        boolean result = configCheck.checkCurseForge(modpackDir);
        Assertions.assertFalse(result);
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    void testPrintConfig() {
        String modpackDir = "src/test/resources/forge_tests";
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
        boolean includeServerInstallation = true;
        String javaPath = "/usr/bin/java";
        String minecraftVersion = "1.16.5";
        String modLoader = "Forge";
        String modLoaderVersion = "36.1.2";
        boolean includeServerIcon = true;
        boolean includeServerProperties = true;
        boolean includeStartScripts = true;
        boolean includeZipCreation = true;
        configCheck.printConfig(
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
    void testCheckModpackDirCorrect() {
        String modpackDir = "./src/test/resources/forge_tests";
        boolean result = configCheck.checkModpackDir(modpackDir);
        Assertions.assertTrue(result);
    }

    @Test
    void testCheckModpackDirFalse() {
        boolean result = configCheck.checkModpackDir("modpackDir");
        Assertions.assertFalse(result);
    }

    @Test
    void testCheckCopyDirsCorrect() {
        String modpackDir = "./src/test/resources/forge_tests";
        List<String> copyDirs = Arrays.asList(
                "config",
                "mods",
                "scripts",
                "seeds",
                "defaultconfigs"
        );
        boolean result = configCheck.checkCopyDirs(copyDirs, modpackDir);
        Assertions.assertTrue(result);
    }

    @Test
    void testCheckCopyDirsFalse() {
        String modpackDir = "./src/test/resources/forge_tests";
        List<String> copyDirs = Arrays.asList(
                "configs",
                "modss",
                "scriptss",
                "seedss",
                "defaultconfigss"
        );
        boolean result = configCheck.checkCopyDirs(copyDirs, modpackDir);
        Assertions.assertFalse(result);
    }

    @Test
    void testGetJavaPath() {
        String result = configCheck.getJavaPath("");
        String autoJavaPath = System.getProperty("java.home").replace("\\", "/") + "/bin/java";
        if (autoJavaPath.startsWith("C:")) {
            autoJavaPath = String.format("%s.exe", autoJavaPath);
        }
        Assertions.assertEquals(autoJavaPath, result);
    }

    @Test
    void testCheckJavaPathCorrect() {
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
        boolean result = configCheck.checkJavaPath(javaPath);
        Assertions.assertTrue(result);
    }

    @Test
    void testCheckModloaderFabric() {
        String modLoader = "Fabric";
        boolean result = configCheck.checkModloader(modLoader);
        Assertions.assertTrue(result);
    }

    @Test
    void testCheckModloaderForge() {
        String modLoader = "Forge";
        boolean result = configCheck.checkModloader(modLoader);
        Assertions.assertTrue(result);
    }

    @Test
    void testCheckModloaderFalse() {
        boolean result = configCheck.checkModloader("modloader");
        Assertions.assertFalse(result);
    }

    @Test
    void testSetModloaderFabric() {
        String result = configCheck.setModloader("fAbRiC");
        Assertions.assertEquals("Fabric", result);
    }

    @Test
    void testSetModloaderForge() {
        String result = configCheck.setModloader("fOrGe");
        Assertions.assertEquals("Forge", result);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testCheckModloaderVersionFabric() {
        String modLoader = "Fabric";
        String modLoaderVersion = "0.11.3";
        boolean result = configCheck.checkModloaderVersion(modLoader, modLoaderVersion);
        Assertions.assertTrue(result);
        new File("fabric-manifest.xml").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testCheckModloaderVersionFabricIncorrect() {
        String modLoader = "Fabric";
        String modLoaderVersion = "0.90.3";
        boolean result = configCheck.checkModloaderVersion(modLoader, modLoaderVersion);
        Assertions.assertFalse(result);
        new File("fabric-manifest.xml").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testCheckModloaderVersionForge() {
        String modLoader = "Forge";
        String modLoaderVersion = "36.1.2";
        boolean result = configCheck.checkModloaderVersion(modLoader, modLoaderVersion);
        Assertions.assertTrue(result);
        new File("forge-manifest.json").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testCheckModloaderVersionForgeIncorrect() {
        String modLoader = "Forge";
        String modLoaderVersion = "90.0.0";
        boolean result = configCheck.checkModloaderVersion(modLoader, modLoaderVersion);
        Assertions.assertFalse(result);
        new File("forge-manifest.json").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testIsMinecraftVersionCorrect() {
        String minecraftVersion = "1.16.5";
        boolean result = configCheck.isMinecraftVersionCorrect(minecraftVersion);
        Assertions.assertTrue(result);
        new File("mcmanifest.json").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testIsMinecraftVersionFalse() {
        String minecraftVersion = "1.99.5";
        boolean result = configCheck.isMinecraftVersionCorrect(minecraftVersion);
        Assertions.assertFalse(result);
        new File("mcmanifest.json").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testIsFabricVersionCorrect() {
        String fabricVersion = "0.11.3";
        boolean result = configCheck.isFabricVersionCorrect(fabricVersion);
        Assertions.assertTrue(result);
        new File("fabric-manifest.xml").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testIsFabricVersionFalse() {
        String fabricVersion = "0.90.3";
        boolean result = configCheck.isFabricVersionCorrect(fabricVersion);
        Assertions.assertFalse(result);
        new File("fabric-manifest.xml").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testIsForgeVersionCorrect() {
        String forgeVersion = "36.1.2";
        boolean result = configCheck.isForgeVersionCorrect(forgeVersion);
        Assertions.assertTrue(result);
        new File("forge-manifest.json").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testIsForgeVersionFalse() {
        String forgeVersion = "99.0.0";
        boolean result = configCheck.isForgeVersionCorrect(forgeVersion);
        Assertions.assertFalse(result);
        new File("forge-manifest.json").delete();
    }
}
