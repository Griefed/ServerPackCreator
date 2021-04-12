package de.griefed.ServerPackCreator;

import com.typesafe.config.Config;
import de.griefed.ServerPackCreator.CurseForgeModpack.CreateModpack;
import de.griefed.ServerPackCreator.GUI.TabbedPane;

import java.io.File;
import java.util.List;

public class Reference {
    static final String MINECRAFT_MANIFEST_URL = "https://launchermeta.mojang.com/mc/game/version_manifest.json";
    static final String FORGE_MANIFEST_URL     = "https://files.minecraftforge.net/maven/net/minecraftforge/forge/maven-metadata.json";
    static final String FABRIC_MANIFEST_URL    = "https://maven.fabricmc.net/net/fabricmc/fabric-loader/maven-metadata.xml";

    static final String CONFIG_GEN_ARGUMENT = "-cgen";
    static final String RUN_CLI_ARGUMENT    = "-cli";

    static final File oldConfigFile     = new File("creator.conf");
    static final File configFile        = new File("serverpackcreator.conf");
    static final File propertiesFile    = new File("server.properties");
    static final File iconFile          = new File("server-icon.png");
    static final File forgeWindowsFile  = new File("start-forge.bat");
    static final File forgeLinuxFile    = new File("start-forge.sh");
    static final File fabricWindowsFile = new File("start-fabric.bat");
    static final File fabricLinuxFile   = new File("start-fabric.sh");

    public static final CreateModpack createModpack = new CreateModpack();

    public static final TabbedPane mainGUI = new TabbedPane();

    public static final CLISetup cliSetup               = new CLISetup();
    public static final ConfigCheck configCheck         = new ConfigCheck();
    public static final CopyFiles copyFiles             = new CopyFiles();
    public static final FilesSetup filesSetup           = new FilesSetup();
    public static final ServerSetup serverSetup         = new ServerSetup();
    public static final ServerUtilities serverUtilities = new ServerUtilities();

    static Config config;

    static List<String>
            clientMods,
            copyDirs;

    static String
            modpackDir,
            javaPath,
            minecraftVersion,
            modLoader,
            modLoaderVersion;

    static Boolean
            includeServerInstallation,
            includeServerIcon,
            includeServerProperties,
            includeStartScripts,
            includeZipCreation;

    static int
            projectID,
            projectFileID;
}
