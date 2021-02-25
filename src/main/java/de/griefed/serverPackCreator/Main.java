package de.griefed.serverPackCreator;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;

public class Main {
    public static String modpackDir;
    public static List<String> clientMods;
    public static List<String> copyDirs;
    public static Boolean includeServerInstallation;
    public static String modLoader;
    public static String modLoaderVersion;
    public static Boolean includeServerIcon;
    public static Boolean includeServerProperties;
    public static Boolean includeStartScripts;
    public static Boolean includeZipCreation;
    public static Config conf;
    private static final Logger appLogger = LogManager.getLogger("Main");
    private static final Logger errorLogger = LogManager.getLogger("MainError");

    public static void main(String[] args) throws IOException {
        appLogger.info("WORK IN PROGESS! CONSIDER THIS ALPHA-STATE!");
        appLogger.info("USE AT YOUR OWN RISK! BE AWARE THAT DATA LOSS IS POSSIBLE!");
        appLogger.info("I CAN NOT BE HELD RESPONSIBLE FOR DATA LOSS!");
        appLogger.info("YOU HAVE BEEN WARNED!");
        appLogger.info("----------------------------------------------------------");

        Copy.filesSetup();

        // Setup config variables with config file
        appLogger.info("Getting configuration...");
        conf = ConfigFactory.parseFile(Copy.configFile);
        modpackDir = conf.getString("modpackDir");
        clientMods = conf.getStringList("clientMods");
        copyDirs = conf.getStringList("copyDirs");
        includeServerInstallation = conf.getBoolean("includeServerInstallation");
        modLoader = conf.getString("modLoader");
        modLoaderVersion = conf.getString("modLoaderVersion");
        includeServerIcon = conf.getBoolean("includeServerIcon");
        includeServerProperties = conf.getBoolean("includeServerProperties");
        includeStartScripts = conf.getBoolean("includeStartScripts");
        includeZipCreation = conf.getBoolean("includeZipCreation");

        appLogger.info("Your configuration is:");
        appLogger.info("Modpack directory: " + modpackDir);
        appLogger.info("Client mods are:");
        for (int i = 0; i < clientMods.toArray().length; i++) {appLogger.info("    " + clientMods.get(i));}
        appLogger.info("Directories to copy:");
        for (int i = 0; i < copyDirs.toArray().length; i++) {appLogger.info("    " + copyDirs.get(i));}
        appLogger.info("Include server installation:      " + includeServerInstallation.toString());
        appLogger.info("Modloader:                        " + modLoader);
        appLogger.info("Modloader Version:                " + modLoaderVersion);
        appLogger.info("Include server icon:              " + includeServerIcon.toString());
        appLogger.info("Include server properties:        " + includeServerProperties.toString());
        appLogger.info("Include start scripts:            " + includeStartScripts.toString());
        appLogger.info("Create zip-archive of serverpack: " + includeZipCreation.toString());

        // Copy all specified directories from modpack to serverpack.
        try {
            Copy.copyFiles(modpackDir, copyDirs);
        } catch (IOException ex) {
            errorLogger.error(ex);
        }

        // Delete client-side mods from serverpack.
        try {
            Server.deleteClientMods(modpackDir, clientMods);
        } catch (IOException ex) {
            errorLogger.error(ex);
        }

        // Generate Forge/Fabric start scripts and copy to serverpack.
        Copy.copyStartScripts(modpackDir, modLoader, includeStartScripts);

        // If true, copy server-icon to serverpack.
        if (includeServerIcon) {
            Copy.copyIcon(modpackDir);
        }

        // If true, copy server.properties to serverpack.
        if (includeServerProperties) {
            Copy.copyProperties(modpackDir);
        }

        // If true, create zip archive of serverpack.
        if (includeZipCreation) {
            Server.zipBuilder(modpackDir);
        }
        appLogger.info("Done!");
        appLogger.info("Serverpack available at: " + modpackDir + "/serverpack");
    }
}
