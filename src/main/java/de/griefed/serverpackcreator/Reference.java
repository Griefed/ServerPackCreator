package de.griefed.serverpackcreator;

import com.typesafe.config.Config;
import de.griefed.serverpackcreator.curseforgemodpack.CreateModpack;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class Reference {
    private static final String MINECRAFT_MANIFEST_URL = "https://launchermeta.mojang.com/mc/game/version_manifest.json";
    private static final String FORGE_MANIFEST_URL     = "https://files.minecraftforge.net/net/minecraftforge/forge/maven-metadata.json";
    private static final String FABRIC_MANIFEST_URL    = "https://maven.fabricmc.net/net/fabricmc/fabric-loader/maven-metadata.xml";

    private static final String LANG_ARGUMENT       = "-lang";
    private static final String CONFIG_GEN_ARGUMENT = "-cgen";
    private static final String RUN_CLI_ARGUMENT    = "-cli";

    private static final File configFile        = new File("serverpackcreator.conf");
    private static final File oldConfigFile     = new File("creator.conf");


    private static final File propertiesFile    = new File("server.properties");
    private static final File iconFile          = new File("server-icon.png");
    private static final File forgeWindowsFile  = new File("start-forge.bat");
    private static final File forgeLinuxFile    = new File("start-forge.sh");
    private static final File fabricWindowsFile = new File("start-fabric.bat");
    private static final File fabricLinuxFile   = new File("start-fabric.sh");

    private static final File langPropertiesFile = new File("lang.properties");

    //-- If you wish to expand this list, fork this repository, make your changes, and submit a PR -------------------------
    private static final String[] SUPPORTED_LANGUAGES = {
            "en_us",
            "uk_ua",
            "de_de"
    };

    public static final CreateModpack createModpack = new CreateModpack();

    public static final FilesSetup      filesSetup      = new FilesSetup();
    public static final CLISetup        cliSetup        = new CLISetup();
    public static final ConfigCheck     configCheck     = new ConfigCheck();
    static final CopyFiles       copyFiles       = new CopyFiles();
    static final ServerSetup     serverSetup     = new ServerSetup();
    static final ServerUtilities serverUtilities = new ServerUtilities();

    public static Config config;

    //-- If you wish to expand this list, fork this repository, make your changes, and submit a PR -------------------------
    private static final List<String> fallbackModsList = Arrays.asList(
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

    private static List<String>
            clientMods,
            copyDirs;

    private static String
            modpackDir,
            javaPath,
            minecraftVersion,
            modLoader,
            modLoaderVersion;

    private static Boolean
            includeServerInstallation,
            includeServerIcon,
            includeServerProperties,
            includeStartScripts,
            includeZipCreation;

    private static int
            projectID,
            projectFileID;

    public static String getMinecraftManifestUrl() {
        return MINECRAFT_MANIFEST_URL;
    }


    public static String getForgeManifestUrl() {
        return FORGE_MANIFEST_URL;
    }


    public static String getFabricManifestUrl() {
        return FABRIC_MANIFEST_URL;
    }


    public static String getLangArgument() {
        return LANG_ARGUMENT;
    }


    public static String getConfigGenArgument() {
        return CONFIG_GEN_ARGUMENT;
    }


    public static String getRunCliArgument() {
        return RUN_CLI_ARGUMENT;
    }


    public static File getConfigFile() {
        return configFile;
    }


    public static File getOldConfigFile() {
        return oldConfigFile;
    }


    public static File getPropertiesFile() {
        return propertiesFile;
    }


    public static File getIconFile() {
        return iconFile;
    }


    public static File getForgeWindowsFile() {
        return forgeWindowsFile;
    }


    public static File getForgeLinuxFile() {
        return forgeLinuxFile;
    }


    public static File getFabricWindowsFile() {
        return fabricWindowsFile;
    }


    public static File getFabricLinuxFile() {
        return fabricLinuxFile;
    }


    public static File getLangPropertiesFile() {
        return langPropertiesFile;
    }


    public static String[] getSupportedLanguages() {
        return SUPPORTED_LANGUAGES;
    }


    public static List<String> getFallbackModsList() {
        return fallbackModsList;
    }


    public static List<String> getClientMods() {
        return clientMods;
    }
    public static void setClientMods(List<String> newClientMods) {
        clientMods = newClientMods;
    }


    public static List<String> getCopyDirs() {
        return copyDirs;
    }
    public static void setCopyDirs(List<String> newCopyDirs) {
        copyDirs = newCopyDirs;
    }


    public static String getModpackDir() {
        return modpackDir;
    }
    public static void setModpackDir(String newModpackDir) {
        newModpackDir = newModpackDir.replace("\\","/");
        modpackDir = newModpackDir;
    }


    public static String getJavaPath() {
        return javaPath;
    }
    public static void setJavaPath(String newJavaPath) {
        newJavaPath = newJavaPath.replace("\\", "/");
        javaPath = newJavaPath;
    }


    public static String getMinecraftVersion() {
        return minecraftVersion;
    }
    public static void setMinecraftVersion(String newMinecraftVersion) {
        minecraftVersion = newMinecraftVersion;
    }


    public static String getModLoader() {
        return modLoader;
    }
    public static void setModLoader(String newModLoader) {
        modLoader = newModLoader;
    }


    public static String getModLoaderVersion() {
        return modLoaderVersion;
    }
    public static void setModLoaderVersion(String newModLoaderVersion) {
        modLoaderVersion = newModLoaderVersion;
    }


    public static boolean getIncludeServerInstallation() {
        return includeServerInstallation;
    }
    public static void setIncludeServerInstallation(boolean newIncludeServerInstallation) {
        includeServerInstallation = newIncludeServerInstallation;
    }


    public static boolean getIncludeServerIcon() {
        return includeServerIcon;
    }
    public static void setIncludeServerIcon(boolean newIncludeServerIcon) {
        includeServerIcon = newIncludeServerIcon;
    }


    public static boolean getIncludeServerProperties() {
        return includeServerProperties;
    }
    public static void setIncludeServerProperties(boolean newIncludeServerProperties) {
        includeServerProperties = newIncludeServerProperties;
    }


    public static boolean getIncludeStartScripts() {
        return includeStartScripts;
    }
    public static void setIncludeStartScripts(boolean newIncludeStartScripts) {
        includeStartScripts = newIncludeStartScripts;
    }


    public static boolean getIncludeZipCreation() {
        return includeZipCreation;
    }
    public static void setIncludeZipCreation(boolean newIncludeZipCreation) {
        includeZipCreation = newIncludeZipCreation;
    }


    public static int getProjectID() {
        return projectID;
    }
    public static void setProjectID(int newProjectID) {
        projectID = newProjectID;
    }


    public static int getProjectFileID() {
        return projectFileID;
    }
    public static void setProjectFileID(int newProjectFileID) {
        projectFileID = newProjectFileID;
    }
}