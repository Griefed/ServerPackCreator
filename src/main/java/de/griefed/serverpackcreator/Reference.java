package de.griefed.serverpackcreator;

import com.typesafe.config.Config;
import de.griefed.serverpackcreator.curseforgemodpack.CreateModpack;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class Reference {
    static final String MINECRAFT_MANIFEST_URL = "https://launchermeta.mojang.com/mc/game/version_manifest.json";
    static final String FORGE_MANIFEST_URL     = "https://files.minecraftforge.net/net/minecraftforge/forge/maven-metadata.json";
    static final String FABRIC_MANIFEST_URL    = "https://maven.fabricmc.net/net/fabricmc/fabric-loader/maven-metadata.xml";

    static final String LANG_ARGUMENT       = "-lang";
    static final String CONFIG_GEN_ARGUMENT = "-cgen";
    static final String RUN_CLI_ARGUMENT    = "-cli";

    public static final File configFile        = new File("serverpackcreator.conf");
           static final File oldConfigFile     = new File("creator.conf");


    static final File propertiesFile    = new File("server.properties");
    static final File iconFile          = new File("server-icon.png");
    static final File forgeWindowsFile  = new File("start-forge.bat");
    static final File forgeLinuxFile    = new File("start-forge.sh");
    static final File fabricWindowsFile = new File("start-fabric.bat");
    static final File fabricLinuxFile   = new File("start-fabric.sh");

    static final File langPropertiesFile = new File("lang.properties");

//-- If you wish to expand this list, fork this repository, make your changes, and submit a PR -------------------------
    public static final String[] SUPPORTED_LANGUAGES = {
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
    public static final List<String> fallbackModsList = Arrays.asList(
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

    public static List<String>
            clientMods,
            copyDirs;

    public static String
            modpackDir,
            javaPath,
            minecraftVersion,
            modLoader,
            modLoaderVersion;

    public static Boolean
            includeServerInstallation,
            includeServerIcon,
            includeServerProperties,
            includeStartScripts,
            includeZipCreation;

    public static int
            projectID,
            projectFileID;
}
