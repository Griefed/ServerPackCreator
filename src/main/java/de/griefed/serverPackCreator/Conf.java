package de.griefed.serverPackCreator;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.IOException;
import java.util.List;

public class Conf {
    public static String modpackDir;
    public static List<String> clientMods;
    public static List<String> copyDirs;
    public static Boolean includeForgeInstallation;
    public static String forgeVersion;
    public static Boolean includeServerIcon;
    public static Boolean includeServerProperties;
    public static Boolean includeStartScripts;
    public static Boolean includeZipCreation;
    public static Config conf;

    // Save all config values from config file to variables
    public static void loadConfig() {
        conf = ConfigFactory.parseFile(Copy.configFile);
        modpackDir = conf.getString("modpackDir");
        clientMods = conf.getStringList("clientMods");
        copyDirs = conf.getStringList("copyDirs");
        includeForgeInstallation = conf.getBoolean("includeForgeInstallation");
        forgeVersion = conf.getString("forgeVersion");
        includeServerIcon = conf.getBoolean("includeServerIcon");
        includeServerProperties = conf.getBoolean("includeServerProperties");
        includeStartScripts = conf.getBoolean("includeStartScripts");
        includeZipCreation = conf.getBoolean("includeZipCreation");

        // Copy all specified directories from modpack to serverpack.
        try {
            Copy.copyFiles(modpackDir, copyDirs);
        } catch (IOException e) {
            //e.printStackTrace();
        }

        // Delete client-side mods from serverpack.
        try {
            Server.deleteClientMods(modpackDir, clientMods);
        } catch (IOException e) {
            //e.printStackTrace();
        }

        // If true, copy server-icon to serverpack.
        if (includeServerIcon) {
            Copy.copyIcon(modpackDir);
        }

        // If true, copy server.properties to serverpack.
        if (includeServerProperties) {
            Copy.copyProperties(modpackDir);
        }

        // If true, copy start scripts to serverpack.
        if (includeStartScripts) {
            Copy.copyStartScripts(modpackDir);
        }

        // If true, create zip archive of serverpack.
        if (includeZipCreation) {
            Server.zipBuilder(modpackDir);
        }
    }
}
