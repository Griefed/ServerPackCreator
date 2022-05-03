package de.griefed.serverpackcreator;

import com.therandomlabs.curseapi.CurseException;
import de.griefed.serverpackcreator.curseforge.CurseCreateModpack;
import de.griefed.serverpackcreator.curseforge.InvalidFileException;
import de.griefed.serverpackcreator.curseforge.InvalidModpackException;
import de.griefed.serverpackcreator.i18n.LocalizationManager;
import de.griefed.serverpackcreator.utilities.ConfigUtilities;
import de.griefed.serverpackcreator.utilities.commonutilities.Utilities;
import de.griefed.serverpackcreator.versionmeta.VersionMeta;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class ConfigurationHandlerTest {

    private final ConfigurationHandler CONFIGURATIONHANDLER;
    private final VersionMeta VERSIONMETA;

    ConfigurationHandlerTest() throws IOException {
        try {
            FileUtils.copyFile(new File("backend/main/resources/serverpackcreator.properties"),new File("serverpackcreator.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        ApplicationProperties APPLICATIONPROPERTIES = new ApplicationProperties();
        LocalizationManager LOCALIZATIONMANAGER = new LocalizationManager(APPLICATIONPROPERTIES);
        ServerPackCreator SERVER_PACK_CREATOR = new ServerPackCreator(new String[]{"--setup"});
        SERVER_PACK_CREATOR.run(CommandlineParser.Mode.SETUP);
        this.VERSIONMETA = new VersionMeta(
                APPLICATIONPROPERTIES.PATH_FILE_MANIFEST_MINECRAFT,
                APPLICATIONPROPERTIES.PATH_FILE_MANIFEST_FORGE,
                APPLICATIONPROPERTIES.PATH_FILE_MANIFEST_FABRIC,
                APPLICATIONPROPERTIES.PATH_FILE_MANIFEST_FABRIC_INSTALLER
        );
        Utilities UTILITIES = new Utilities(LOCALIZATIONMANAGER, APPLICATIONPROPERTIES);
        ConfigUtilities CONFIGUTILITIES = new ConfigUtilities(LOCALIZATIONMANAGER, UTILITIES, APPLICATIONPROPERTIES, VERSIONMETA);
        CurseCreateModpack CURSECREATEMODPACK = new CurseCreateModpack(LOCALIZATIONMANAGER, APPLICATIONPROPERTIES, VERSIONMETA, UTILITIES,CONFIGUTILITIES);
        this.CONFIGURATIONHANDLER = new ConfigurationHandler(LOCALIZATIONMANAGER, CURSECREATEMODPACK, VERSIONMETA, APPLICATIONPROPERTIES, UTILITIES, CONFIGUTILITIES);

    }

    @Test
    void checkConfigFileTest() {
        Assertions.assertFalse(CONFIGURATIONHANDLER.checkConfiguration(new File("./backend/test/resources/testresources/serverpackcreator.conf"), false));
    }

    @Test
    void isDirTestCopyDirs() {
        Assertions.assertTrue(CONFIGURATIONHANDLER.checkConfiguration(new File("./backend/test/resources/testresources/serverpackcreator_copydirs.conf"), false));
    }

    @Test
    void isDirTestJavaPath() {
        Assertions.assertFalse(CONFIGURATIONHANDLER.checkConfiguration(new File("./backend/test/resources/testresources/serverpackcreator_javapath.conf"), false));
    }

    @Test
    void isDirTestMinecraftVersion() {
        Assertions.assertTrue(CONFIGURATIONHANDLER.checkConfiguration(new File("./backend/test/resources/testresources/serverpackcreator_minecraftversion.conf"), false));

    }

    @Test
    void isDirTestModLoader() {
        Assertions.assertFalse(CONFIGURATIONHANDLER.checkConfiguration(new File("./backend/test/resources/testresources/serverpackcreator_modloader.conf"), false));
    }

    @Test
    void isDirTestModLoaderFalse() {
        Assertions.assertTrue(CONFIGURATIONHANDLER.checkConfiguration(new File("./backend/test/resources/testresources/serverpackcreator_modloaderfalse.conf"), false));
    }

    @Test
    void isDirTestModLoaderVersion() {
        Assertions.assertTrue(CONFIGURATIONHANDLER.checkConfiguration(new File("./backend/test/resources/testresources/serverpackcreator_modloaderversion.conf"), false));
    }

    @Disabled
    @Test
    void isCurseTest() {
        // TODO: Check config when re-activating CurseForge module
        Assertions.assertTrue(CONFIGURATIONHANDLER.checkConfiguration(new File("./backend/test/resources/testresources/serverpackcreator_curseforge.conf"), false));
    }

    @Disabled
    @Test
    void isCurseTestProjectIDFalse() {
        // TODO: Check config when re-activating CurseForge module
        Assertions.assertTrue(CONFIGURATIONHANDLER.checkConfiguration(new File("./backend/test/resources/testresources/serverpackcreator_curseforgefalse.conf"), false));
    }

    @Disabled
    @Test
    void isCurseTestProjectFileIDFalse() {
        // TODO: Check config when re-activating CurseForge module
        Assertions.assertTrue(CONFIGURATIONHANDLER.checkConfiguration(new File("./backend/test/resources/testresources/serverpackcreator_curseforgefilefalse.conf"), false));
    }


    @Disabled
    @Test
    void checkCurseForgeTest() throws InvalidModpackException, InvalidFileException, CurseException {
        String valid = "430517,3266321";
        ConfigurationModel configurationModel = new ConfigurationModel();
        Assertions.assertTrue(CONFIGURATIONHANDLER.checkCurseForge(valid, configurationModel, new ArrayList<>(100)));
    }

    @Disabled
    @Test
    void checkCurseForgeTestFalse() throws InvalidModpackException, InvalidFileException, CurseException {
        String invalid = "1,1234";
        ConfigurationModel configurationModel = new ConfigurationModel();
        Assertions.assertFalse(CONFIGURATIONHANDLER.checkCurseForge(invalid, configurationModel, new ArrayList<>(100)));
    }

    @Disabled
    @Test
    void checkCurseForgeTestNotMinecraft() {
        String invalid = "10,60018";
        ConfigurationModel configurationModel = new ConfigurationModel();
        Assertions.assertThrows(InvalidModpackException.class, () -> CONFIGURATIONHANDLER.checkCurseForge(invalid, configurationModel, new ArrayList<>(100)));
    }

    @Test
    void checkModpackDirTest() {
        String modpackDirCorrect = "./backend/test/resources/forge_tests";
        Assertions.assertTrue(CONFIGURATIONHANDLER.checkModpackDir(modpackDirCorrect, new ArrayList<>(100)));
    }

    @Test
    void checkModpackDirTestFalse() {
        Assertions.assertFalse(CONFIGURATIONHANDLER.checkModpackDir("modpackDir", new ArrayList<>(100)));
    }

    @Test
    void checkCopyDirsTest() {
        String modpackDir = "backend/test/resources/forge_tests";
        List<String> copyDirs = new ArrayList<>(Arrays.asList(
                "config",
                "mods",
                "scripts",
                "seeds",
                "defaultconfigs"
        ));
        Assertions.assertTrue(CONFIGURATIONHANDLER.checkCopyDirs(copyDirs, modpackDir, new ArrayList<>(100)));
    }

    @Test
    void checkCopyDirsTestFalse() {
        String modpackDir = "backend/test/resources/forge_tests";
        List<String> copyDirsInvalid = new ArrayList<>(Arrays.asList(
                "configs",
                "modss",
                "scriptss",
                "seedss",
                "defaultconfigss"
        ));
        Assertions.assertFalse(CONFIGURATIONHANDLER.checkCopyDirs(copyDirsInvalid, modpackDir, new ArrayList<>(100)));
    }

    @Test
    void checkCopyDirsTestFiles() {
        String modpackDir = "backend/test/resources/forge_tests";
        List<String> copyDirsAndFiles = new ArrayList<>(Arrays.asList(
                "config",
                "mods",
                "scripts",
                "seeds",
                "defaultconfigs",
                "test.txt;test.txt",
                "test2.txt;test2.txt"
        ));
        Assertions.assertTrue(CONFIGURATIONHANDLER.checkCopyDirs(copyDirsAndFiles, modpackDir, new ArrayList<>(100)));
    }

    @Test
    void checkCopyDirsTestFilesFalse() {
        String modpackDir = "backend/test/resources/forge_tests";
        List<String> copyDirsAndFilesFalse = new ArrayList<>(Arrays.asList(
                "configs",
                "modss",
                "scriptss",
                "seedss",
                "defaultconfigss",
                "READMEee.md;README.md",
                "LICENSEee;LICENSE",
                "LICENSEee;test/LICENSE",
                "LICENSEee;test/license.md"
        ));
        Assertions.assertFalse(CONFIGURATIONHANDLER.checkCopyDirs(copyDirsAndFilesFalse, modpackDir, new ArrayList<>(100)));
    }

    @Test
    void checkJavaPathTest() {
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
        Assertions.assertNotNull(CONFIGURATIONHANDLER.getJavaPath(javaPath));
        Assertions.assertTrue(new File(CONFIGURATIONHANDLER.getJavaPath(javaPath)).exists());
    }

    @Test
    void checkModloaderTestForge() {
        Assertions.assertTrue(CONFIGURATIONHANDLER.checkModloader("Forge"));
    }

    @Test
    void checkModloaderTestForgeCase() {
        Assertions.assertTrue(CONFIGURATIONHANDLER.checkModloader("fOrGe"));
    }

    @Test
    void checkModloaderTestFabric() {
        Assertions.assertTrue(CONFIGURATIONHANDLER.checkModloader("Fabric"));
    }

    @Test
    void checkModloaderTestFabricCase() {
        Assertions.assertTrue(CONFIGURATIONHANDLER.checkModloader("fAbRiC"));
    }

    @Test
    void checkModLoaderTestFalse() {
        Assertions.assertFalse(CONFIGURATIONHANDLER.checkModloader("modloader"));
    }

    @Test
    void checkModloaderVersionTestForge() {
        Assertions.assertTrue(CONFIGURATIONHANDLER.checkModloaderVersion("Forge", "36.1.2", "1.16.5"));
    }

    @Test
    void checkModloaderVersionTestForgeFalse() {
        Assertions.assertFalse(CONFIGURATIONHANDLER.checkModloaderVersion("Forge", "90.0.0", "1.16.5"));
    }

    @Test
    void checkModloaderVersionTestFabric() {
        Assertions.assertTrue(CONFIGURATIONHANDLER.checkModloaderVersion("Fabric", "0.11.3", "1.16.5"));
    }

    @Test
    void checkModloaderVersionTestFabricFalse() {
        Assertions.assertFalse(CONFIGURATIONHANDLER.checkModloaderVersion("Fabric", "0.90.3", "1.16.5"));
    }

    @Test
    void isMinecraftVersionCorrectTest() {
        Assertions.assertTrue(VERSIONMETA.minecraft().checkMinecraftVersion("1.16.5"));
    }

    @Test
    void isMinecraftVersionCorrectTestFalse() {
        Assertions.assertFalse(VERSIONMETA.minecraft().checkMinecraftVersion("1.99.5"));
    }

    @Test
    void isFabricVersionCorrectTest() {
        Assertions.assertTrue(VERSIONMETA.fabric().checkFabricVersion("0.11.3"));
    }

    @Test
    void isFabricVersionCorrectTestFalse() {
        Assertions.assertFalse(VERSIONMETA.fabric().checkFabricVersion("0.90.3"));
    }

    @Test
    void isForgeVersionCorrectTest() {
        Assertions.assertTrue(VERSIONMETA.forge().checkForgeAndMinecraftVersion("1.16.5","36.1.2"));
    }

    @Test
    void isForgeVersionCorrectTestFalse() {
        Assertions.assertFalse(VERSIONMETA.forge().checkForgeAndMinecraftVersion("1.16.5","99.0.0"));
    }

    @Test
    void checkConfigModelTest() {
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
        Assertions.assertFalse(CONFIGURATIONHANDLER.checkConfiguration(configurationModel, false));
    }

    @Test
    void zipArchiveTest() {
        ConfigurationModel configurationModel = new ConfigurationModel();
        configurationModel.setModpackDir("backend/test/resources/testresources/Survive_Create_Prosper_4_valid.zip");
        Assertions.assertFalse(CONFIGURATIONHANDLER.checkConfiguration(configurationModel,true));
        configurationModel = new ConfigurationModel();
        configurationModel.setModpackDir("backend/test/resources/testresources/Survive_Create_Prosper_4_invalid.zip");
        Assertions.assertTrue(CONFIGURATIONHANDLER.checkConfiguration(configurationModel,true));
    }

    @Test
    void checkConfigurationFileTest() {
        Assertions.assertFalse(CONFIGURATIONHANDLER.checkConfiguration(new File("backend/test/resources/testresources/serverpackcreator.conf"), new ArrayList<>(),true));
    }

    @Test
    void checkConfigurationFileNoDownloadTest() {
        Assertions.assertFalse(CONFIGURATIONHANDLER.checkConfiguration(new File("backend/test/resources/testresources/serverpackcreator.conf"),false,true));
    }

    @Test
    void checkConfigurationFileAndModelTest() {
        ConfigurationModel configurationModel = new ConfigurationModel();
        Assertions.assertFalse(CONFIGURATIONHANDLER.checkConfiguration(new File("backend/test/resources/testresources/serverpackcreator.conf"), configurationModel, false,true));
        Assertions.assertEquals("./backend/test/resources/forge_tests", configurationModel.getModpackDir());
        Assertions.assertEquals("1.16.5",configurationModel.getMinecraftVersion());
        Assertions.assertEquals("Forge",configurationModel.getModLoader());
        Assertions.assertEquals("36.1.2",configurationModel.getModLoaderVersion());
    }

    @Test
    void checkConfigurationFileModelExtendedParamsTest() {
        ConfigurationModel configurationModel = new ConfigurationModel();
        Assertions.assertFalse(CONFIGURATIONHANDLER.checkConfiguration(new File("backend/test/resources/testresources/serverpackcreator.conf"), configurationModel,new ArrayList<>(),false,false));
        Assertions.assertEquals("./backend/test/resources/forge_tests", configurationModel.getModpackDir());
        Assertions.assertEquals("1.16.5",configurationModel.getMinecraftVersion());
        Assertions.assertEquals("Forge",configurationModel.getModLoader());
        Assertions.assertEquals("36.1.2",configurationModel.getModLoaderVersion());
    }

    @Test
    void checkConfigurationNoFileTest() {
        ConfigurationModel configurationModel = new ConfigurationModel();
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
        Assertions.assertFalse(CONFIGURATIONHANDLER.checkConfiguration(configurationModel,new ArrayList<>(),false,true));
        Assertions.assertEquals("./backend/test/resources/forge_tests", configurationModel.getModpackDir());
        Assertions.assertEquals("1.16.5",configurationModel.getMinecraftVersion());
        Assertions.assertEquals("Forge",configurationModel.getModLoader());
        Assertions.assertEquals("36.1.2",configurationModel.getModLoaderVersion());
    }

    @Test
    void checkIconAndPropertiesTest() {
        Assertions.assertTrue(CONFIGURATIONHANDLER.checkIconAndProperties(""));
        Assertions.assertFalse(CONFIGURATIONHANDLER.checkIconAndProperties("/some/path"));
        Assertions.assertTrue(CONFIGURATIONHANDLER.checkIconAndProperties("img/prosper.png"));
    }
}
