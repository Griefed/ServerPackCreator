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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

class ConfigurationTest {
    @Mock
    Logger appLogger;

    private Configuration configuration;
    private CurseCreateModpack curseCreateModpack;
    private LocalizationManager localizationManager;

    ConfigurationTest() {
        localizationManager = new LocalizationManager();
        curseCreateModpack = new CurseCreateModpack(localizationManager);
        configuration = new Configuration(localizationManager, curseCreateModpack);
    }

    @BeforeEach
    void setUp() {
        localizationManager.checkLocaleFile();
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSetCopyDirs() {
        List<String> testList = new ArrayList<>(Arrays.asList(
                "config",
                "mods",
                "scripts",
                "seeds",
                "defaultconfigs",
                "server_pack"
        ));
        configuration.setCopyDirs(testList);
        Assertions.assertFalse(configuration.getCopyDirs().contains("server_pack"));
    }

    @Test
    void testBuildString() {
        List<String> args = new ArrayList<>(Arrays.asList(
                "config",
                "mods",
                "scripts",
                "seeds",
                "defaultconfigs"
        ));
        String result = configuration.buildString(args.toString());
        Assertions.assertEquals(args.toString(), String.format("[%s]",result));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testCheckConfig() throws IOException {
        Files.copy(Paths.get("./src/test/resources/testresources/serverpackcreator.conf"), Paths.get("./serverpackcreator.conf"), REPLACE_EXISTING);
        configuration.setConfig(ConfigFactory.parseFile(configuration.getConfigFile()));
        boolean result = configuration.checkConfigFile(configuration.getConfigFile(), true);
        Assertions.assertFalse(result);
        new File("./serverpackcreator.conf").delete();
    }

    @Test
    void testCheckCurseForgeCorrect() {
        String modpackDir = "238298,3174854";
        boolean result = configuration.checkCurseForge(modpackDir);
        Assertions.assertTrue(result);
    }

    @Test
    void testCheckCurseForgeFalse() {
        String modpackDir = "1,1234";
        boolean result = configuration.checkCurseForge(modpackDir);
        Assertions.assertFalse(result);
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    void testPrintConfig() {
        String modpackDir = "src/test/resources/forge_tests";
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
        boolean includeServerInstallation = true;
        String javaPath = "/usr/bin/java";
        String minecraftVersion = "1.16.5";
        String modLoader = "Forge";
        String modLoaderVersion = "36.1.2";
        boolean includeServerIcon = true;
        boolean includeServerProperties = true;
        boolean includeStartScripts = true;
        boolean includeZipCreation = true;
        configuration.printConfig(
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
        boolean result = configuration.checkModpackDir(modpackDir);
        Assertions.assertTrue(result);
    }

    @Test
    void testCheckModpackDirFalse() {
        boolean result = configuration.checkModpackDir("modpackDir");
        Assertions.assertFalse(result);
    }

    @Test
    void testCheckCopyDirsCorrect() {
        String modpackDir = "./src/test/resources/forge_tests";
        List<String> copyDirs = new ArrayList<>(Arrays.asList(
                "config",
                "mods",
                "scripts",
                "seeds",
                "defaultconfigs"
        ));
        boolean result = configuration.checkCopyDirs(copyDirs, modpackDir);
        Assertions.assertTrue(result);
    }

    @Test
    void testCheckCopyDirsFalse() {
        String modpackDir = "./src/test/resources/forge_tests";
        List<String> copyDirs = new ArrayList<>(Arrays.asList(
                "configs",
                "modss",
                "scriptss",
                "seedss",
                "defaultconfigss"
        ));
        boolean result = configuration.checkCopyDirs(copyDirs, modpackDir);
        Assertions.assertFalse(result);
    }

    @Test
    void testGetJavaPath() {
        String result = configuration.getJavaPathFromSystem("");
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
        boolean result = configuration.checkJavaPath(javaPath);
        Assertions.assertTrue(result);
    }

    @Test
    void testCheckModloaderFabric() {
        String modLoader = "Fabric";
        boolean result = configuration.checkModloader(modLoader);
        Assertions.assertTrue(result);
    }

    @Test
    void testCheckModloaderForge() {
        String modLoader = "Forge";
        boolean result = configuration.checkModloader(modLoader);
        Assertions.assertTrue(result);
    }

    @Test
    void testCheckModloaderFalse() {
        boolean result = configuration.checkModloader("modloader");
        Assertions.assertFalse(result);
    }

    @Test
    void testSetModloaderFabric() {
        String result = configuration.setModLoaderCase("fAbRiC");
        Assertions.assertEquals("Fabric", result);
    }

    @Test
    void testSetModloaderForge() {
        String result = configuration.setModLoaderCase("fOrGe");
        Assertions.assertEquals("Forge", result);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testCheckModloaderVersionFabric() {
        String modLoader = "Fabric";
        String modLoaderVersion = "0.11.3";
        boolean result = configuration.checkModloaderVersion(modLoader, modLoaderVersion);
        Assertions.assertTrue(result);
        new File("fabric-manifest.xml").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testCheckModloaderVersionFabricIncorrect() {
        String modLoader = "Fabric";
        String modLoaderVersion = "0.90.3";
        boolean result = configuration.checkModloaderVersion(modLoader, modLoaderVersion);
        Assertions.assertFalse(result);
        new File("fabric-manifest.xml").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testCheckModloaderVersionForge() {
        String modLoader = "Forge";
        String modLoaderVersion = "36.1.2";
        boolean result = configuration.checkModloaderVersion(modLoader, modLoaderVersion);
        Assertions.assertTrue(result);
        new File("forge-manifest.json").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testCheckModloaderVersionForgeIncorrect() {
        String modLoader = "Forge";
        String modLoaderVersion = "90.0.0";
        boolean result = configuration.checkModloaderVersion(modLoader, modLoaderVersion);
        Assertions.assertFalse(result);
        new File("forge-manifest.json").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testIsMinecraftVersionCorrect() {
        String minecraftVersion = "1.16.5";
        boolean result = configuration.isMinecraftVersionCorrect(minecraftVersion);
        Assertions.assertTrue(result);
        new File("mcmanifest.json").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testIsMinecraftVersionFalse() {
        String minecraftVersion = "1.99.5";
        boolean result = configuration.isMinecraftVersionCorrect(minecraftVersion);
        Assertions.assertFalse(result);
        new File("mcmanifest.json").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testIsFabricVersionCorrect() {
        String fabricVersion = "0.11.3";
        boolean result = configuration.isFabricVersionCorrect(fabricVersion);
        Assertions.assertTrue(result);
        new File("fabric-manifest.xml").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testIsFabricVersionFalse() {
        String fabricVersion = "0.90.3";
        boolean result = configuration.isFabricVersionCorrect(fabricVersion);
        Assertions.assertFalse(result);
        new File("fabric-manifest.xml").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testIsForgeVersionCorrect() {
        String forgeVersion = "36.1.2";
        boolean result = configuration.isForgeVersionCorrect(forgeVersion);
        Assertions.assertTrue(result);
        new File("forge-manifest.json").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testIsForgeVersionFalse() {
        String forgeVersion = "99.0.0";
        boolean result = configuration.isForgeVersionCorrect(forgeVersion);
        Assertions.assertFalse(result);
        new File("forge-manifest.json").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void writeConfigToFileTestFabric() {
        String modpackDir = "./src/test/resources/fabric_tests";
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
        boolean result = configuration.writeConfigToFile(
                modpackDir,
                configuration.buildString(clientMods.toString()),
                configuration.buildString(copyDirs.toString()),
                true,
                javaPath,
                minecraftVersion,
                modLoader,
                modLoaderVersion,
                true,
                true,
                true,
                true,
                configuration.getConfigFile(),
                false
        );
        Assertions.assertTrue(result);
        Assertions.assertTrue(new File("./serverpackcreator.conf").exists());
        new File("./serverpackcreator.conf").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testWriteConfigToFileForge() {
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
        String javaPath = "/use/bin/java";
        String minecraftVersion = "1.16.5";
        String modLoader = "Forge";
        String modLoaderVersion = "36.1.2";
        boolean result = configuration.writeConfigToFile(
                modpackDir,
                configuration.buildString(clientMods.toString()),
                configuration.buildString(copyDirs.toString()),
                true,
                javaPath,
                minecraftVersion,
                modLoader,
                modLoaderVersion,
                true,
                true,
                true,
                true,
                configuration.getConfigFile(),
                false
        );
        Assertions.assertTrue(result);
        Assertions.assertTrue(new File("./serverpackcreator.conf").exists());
        new File("./serverpackcreator.conf").delete();
    }
}
