package de.griefed.ServerPackCreator;

import com.therandomlabs.curseapi.project.CurseProject;
import com.typesafe.config.Config;

import java.io.File;
import java.util.List;
import java.util.Optional;

class Reference {
    static final String MINECRAFT_MANIFEST_URL = "https://launchermeta.mojang.com/mc/game/version_manifest.json";
    static final String FORGE_MANIFEST_URL = "https://files.minecraftforge.net/maven/net/minecraftforge/forge/maven-metadata.json";
    static final String FABRIC_MANIFEST_URL = "https://maven.fabricmc.net/net/fabricmc/fabric-loader/maven-metadata.xml";
    static final File oldConfigFile = new File("creator.conf");
    static final File configFile = new File("serverpackcreator.conf");
    static final File propertiesFile = new File("server.properties");
    static final File iconFile = new File("server-icon.png");
    static final File forgeWindowsFile = new File("start-forge.bat");
    static final File forgeLinuxFile = new File("start-forge.sh");
    static final File fabricWindowsFile = new File("start-fabric.bat");
    static final File fabricLinuxFile = new File("start-fabric.sh");
    static Config conf;
    static String modpackDir;
    static List<String> clientMods;
    static List<String> copyDirs;
    static Boolean includeServerInstallation;
    static String javaPath;
    static String minecraftVersion;
    static String modLoader;
    static String modLoaderVersion;
    static Boolean includeServerIcon;
    static Boolean includeServerProperties;
    static Boolean includeStartScripts;
    static Boolean includeZipCreation;
    static final String CONFIG_GEN_ARGUMENT = "-cgen";
}
