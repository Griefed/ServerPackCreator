package de.griefed.serverpackcreator.utilities;

import de.griefed.serverpackcreator.ApplicationProperties;
import de.griefed.serverpackcreator.ConfigurationModel;
import de.griefed.serverpackcreator.i18n.LocalizationManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConfigUtilitiesTest {

    private final LocalizationManager LOCALIZATIONMANAGER;
    private final ApplicationProperties APPLICATIONPROPERTIES;
    private final BooleanUtilities BOOLEANUTILITIES;
    private final ListUtilities LISTUTILITIES;
    private final StringUtilities STRINGUTILITIES;
    private final ConfigUtilities CONFIGUTILITIES;

    ConfigUtilitiesTest() {
        this.LOCALIZATIONMANAGER = new LocalizationManager();
        this.APPLICATIONPROPERTIES = new ApplicationProperties();
        this.BOOLEANUTILITIES = new BooleanUtilities(LOCALIZATIONMANAGER, APPLICATIONPROPERTIES);
        this.LISTUTILITIES = new ListUtilities();
        this.STRINGUTILITIES = new StringUtilities();
        this.CONFIGUTILITIES = new ConfigUtilities(LOCALIZATIONMANAGER, BOOLEANUTILITIES, LISTUTILITIES, APPLICATIONPROPERTIES, STRINGUTILITIES);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void writeConfigToFileTestFabric() {
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

        if (autoJavaPath.startsWith("C:")) {autoJavaPath = String.format("%s.exe", autoJavaPath);}
        if (new File("/usr/bin/java").exists()) {javaPath = "/usr/bin/java";} else {javaPath = autoJavaPath;}

        String javaArgs = "tf3g4jz89agz843fag8z49a3zg8ap3jg8zap9vagv3z8j";

        Assertions.assertTrue(CONFIGUTILITIES.writeConfigToFile(
                "./backend/test/resources/fabric_tests",
                clientMods,
                copyDirs,
                "",
                "",
                true,
                javaPath,
                "1.16.5",
                "Fabric",
                "0.11.3",
                true,
                true,
                true,
                javaArgs,
                "",
                new File("./serverpackcreatorfabric.conf"),
                false
        ));
        Assertions.assertTrue(new File("./serverpackcreatorfabric.conf").exists());
        new File("./serverpackcreatorfabric.conf").delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void writeConfigToFileTestForge() {
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

        if (autoJavaPath.startsWith("C:")) {autoJavaPath = String.format("%s.exe", autoJavaPath);}
        if (new File("/usr/bin/java").exists()) {javaPath = "/usr/bin/java";} else {javaPath = autoJavaPath;}

        String javaArgs = "tf3g4jz89agz843fag8z49a3zg8ap3jg8zap9vagv3z8j";

        Assertions.assertTrue(CONFIGUTILITIES.writeConfigToFile(
                "./backend/test/resources/forge_tests",
                clientMods,
                copyDirs,
                "",
                "",
                true,
                javaPath,
                "1.16.5",
                "Forge",
                "36.1.2",
                true,
                true,
                true,
                javaArgs,
                "",
                new File("./serverpackcreatorforge.conf"),
                false
        ));
        Assertions.assertTrue(new File("./serverpackcreatorforge.conf").exists());
        new File("./serverpackcreatorforge.conf").delete();
    }

    @Test
    void writeConfigToFileModelTest() {
        ConfigurationModel configurationModel = new ConfigurationModel();
        configurationModel.setModpackDir("backend/test/resources/forge_tests");
        configurationModel.setClientMods(new ArrayList<>(Arrays.asList(
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
        )));
        configurationModel.setCopyDirs(new ArrayList<>(Arrays.asList(
                "config",
                "mods",
                "scripts",
                "seeds",
                "defaultconfigs"
        )));
        configurationModel.setIncludeServerInstallation(true);
        configurationModel.setIncludeServerIcon(true);
        configurationModel.setIncludeServerProperties(true);
        configurationModel.setIncludeZipCreation(true);
        configurationModel.setJavaPath("/usr/bin/java");
        configurationModel.setMinecraftVersion("1.16.5");
        configurationModel.setModLoader("Forge");
        configurationModel.setModLoaderVersion("36.1.2");
        configurationModel.setJavaArgs("tf3g4jz89agz843fag8z49a3zg8ap3jg8zap9vagv3z8j");
        Assertions.assertTrue(CONFIGUTILITIES.writeConfigToFile(configurationModel, new File("somefile.conf"),false));
        Assertions.assertTrue(new File("somefile.conf").exists());
    }

    @Test
    void setModLoaderCaseTestForge() {
        Assertions.assertEquals("Forge", CONFIGUTILITIES.getModLoaderCase("fOrGe"));
    }

    @Test
    void setModLoaderCaseTestFabric() {
        Assertions.assertEquals("Fabric", CONFIGUTILITIES.getModLoaderCase("fAbRiC"));
    }

    @Test
    void setModLoaderCaseTestForgeCorrected() {
        Assertions.assertEquals("Forge", CONFIGUTILITIES.getModLoaderCase("eeeeefOrGeeeeee"));
    }

    @Test
    void setModLoaderCaseTestFabricCorrected() {
        Assertions.assertEquals("Fabric", CONFIGUTILITIES.getModLoaderCase("hufwhafasfabricfagrsg"));
    }

    @Test
    void printConfigTest() {
        String modpackDir = "backend/test/resources/forge_tests";
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
        String javaArgs = "tf3g4jz89agz843fag8z49a3zg8ap3jg8zap9vagv3z8j";
        boolean includeServerIcon = true;
        boolean includeServerProperties = true;
        boolean includeZipCreation = true;

        CONFIGUTILITIES.printConfigurationModel(
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
                includeZipCreation,
                javaArgs,
                "","","");
    }

    @Test
    void printConfigModelTest() {
        ConfigurationModel configurationModel = new ConfigurationModel();
        configurationModel.setModpackDir("backend/test/resources/forge_tests");
        configurationModel.setClientMods(new ArrayList<>(Arrays.asList(
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
        )));
        configurationModel.setCopyDirs(new ArrayList<>(Arrays.asList(
                "config",
                "mods",
                "scripts",
                "seeds",
                "defaultconfigs"
        )));
        configurationModel.setIncludeServerInstallation(true);
        configurationModel.setIncludeServerIcon(true);
        configurationModel.setIncludeServerProperties(true);
        configurationModel.setIncludeZipCreation(true);
        configurationModel.setJavaPath("/usr/bin/java");
        configurationModel.setMinecraftVersion("1.16.5");
        configurationModel.setModLoader("Forge");
        configurationModel.setModLoaderVersion("36.1.2");
        configurationModel.setJavaArgs("tf3g4jz89agz843fag8z49a3zg8ap3jg8zap9vagv3z8j");
        CONFIGUTILITIES.printConfigurationModel(configurationModel);
    }

    @Test
    void getConfigurationAsListTest() {
        ConfigurationModel configurationModel = new ConfigurationModel();
        configurationModel.setModpackDir("backend/test/resources/forge_tests");
        configurationModel.setClientMods(new ArrayList<>(Arrays.asList(
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
        )));
        configurationModel.setCopyDirs(new ArrayList<>(Arrays.asList(
                "config",
                "mods",
                "scripts",
                "seeds",
                "defaultconfigs"
        )));
        configurationModel.setIncludeServerInstallation(true);
        configurationModel.setIncludeServerIcon(true);
        configurationModel.setIncludeServerProperties(true);
        configurationModel.setIncludeZipCreation(true);
        configurationModel.setJavaPath("/usr/bin/java");
        configurationModel.setMinecraftVersion("1.16.5");
        configurationModel.setModLoader("Forge");
        configurationModel.setModLoaderVersion("36.1.2");
        configurationModel.setJavaArgs("tf3g4jz89agz843fag8z49a3zg8ap3jg8zap9vagv3z8j");
        List<String> config = new ArrayList<>(100);
        Assertions.assertTrue(CONFIGUTILITIES.getConfigurationAsList(configurationModel).size() > 0);
    }
}
