package de.griefed.serverPackCreator;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

public class Main {
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
    static Config conf;

    private static final Logger appLogger = LogManager.getLogger("Main");
    /** Warn user about WIP status. Get configuration from serverpackcreator.conf. Check configuration. Print configuration. Make calls according to configuration.
     * Basically, the main class makes the calls to every other class where the actual magic is happening. The main class of ServerPackCreator should never contain code which does work on the serverpack itself.
     * @param args
     */
    public static void main(String[] args) {
        appLogger.warn("################################################################");
        appLogger.warn("#         WORK IN PROGRESS! CONSIDER THIS ALPHA-STATE!         #");
        appLogger.warn("#  USE AT YOUR OWN RISK! BE AWARE THAT DATA LOSS IS POSSIBLE!  #");
        appLogger.warn("#         I CAN NOT BE HELD RESPONSIBLE FOR DATA LOSS!         #");
        appLogger.warn("#                    YOU HAVE BEEN WARNED!                     #");
        appLogger.warn("################################################################");
        try {
            String jarPath = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            appLogger.info("JAR Path: " + jarPath);
            String jarName = jarPath.substring(jarPath.lastIndexOf("/") + 1);
            appLogger.info("JAR Name: " + jarName);

        } catch (URISyntaxException ex) {
            appLogger.error("Error getting jar name.", ex);
        }
        FilesSetup.filesSetup();
        appLogger.info("Getting configuration...");
        conf = ConfigFactory.parseFile(FilesSetup.configFile);
        modpackDir = conf.getString("modpackDir");
        clientMods = conf.getStringList("clientMods");
        copyDirs = conf.getStringList("copyDirs");
        includeServerInstallation = conf.getBoolean("includeServerInstallation");
        if (includeServerInstallation && new File(conf.getString("javaPath")).exists()) {
            if (conf.getString("javaPath").endsWith(".exe")) {
                javaPath = conf.getString("javaPath").substring(0, conf.getString("javaPath").length() - 4);
            } else {
                javaPath = conf.getString("javaPath");
            }
        } else if (includeServerInstallation && !(new File(conf.getString("javaPath")).exists())) {
            appLogger.error("Java could not be found. Check your configuration.");
            appLogger.error("Your configuration for javaPath was: " + conf.getString("javaPath"));
            System.exit(0);
        } else if (!includeServerInstallation) {
            appLogger.info("Server installation disabled. ");
        } else {
            appLogger.error("Server installation and/or Java path incorrect. Please check.");
            appLogger.error("Your configuration for javaPath was: " + conf.getString("javaPath"));
            appLogger.error("Your configuration for includeServerInstallation was: " + conf.getString("includeServerInstallation"));
            System.exit(0);
        }

        minecraftVersion = conf.getString("minecraftVersion");
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
        appLogger.info("Java Installation path:           " + javaPath);
        appLogger.info("Minecraft version:                " + minecraftVersion);
        appLogger.info("Modloader:                        " + modLoader);
        appLogger.info("Modloader Version:                " + modLoaderVersion);
        appLogger.info("Include server icon:              " + includeServerIcon.toString());
        appLogger.info("Include server properties:        " + includeServerProperties.toString());
        appLogger.info("Include start scripts:            " + includeStartScripts.toString());
        appLogger.info("Create zip-archive of serverpack: " + includeZipCreation.toString());

        CopyFiles.cleanupEnvironment(modpackDir);

        try {
            CopyFiles.copyFiles(modpackDir, copyDirs);
        } catch (IOException ex) {
            appLogger.error("There was an error calling the copyFiles method.", ex);
        }

        try {
            ServerSetup.deleteClientMods(modpackDir, clientMods);
        } catch (IOException ex) {
            appLogger.error("There was an error calling the deleteClientMods method.", ex);
        }

        CopyFiles.copyStartScripts(modpackDir, modLoader, includeStartScripts);

        if (includeServerInstallation) {
            ServerSetup.installServer(modLoader, modpackDir, minecraftVersion, modLoaderVersion, javaPath);
        } else {
            appLogger.info("Not installing modded server.");
        }

        if (includeServerIcon) {
            CopyFiles.copyIcon(modpackDir);
        } else {
            appLogger.info("Not including servericon.");
        }

        if (includeServerProperties) {
            CopyFiles.copyProperties(modpackDir);
        } else {
            appLogger.info("Not including server.properties.");
        }

        if (includeZipCreation) {
            ServerSetup.zipBuilder(modpackDir, modLoader, minecraftVersion);
        } else {
            appLogger.info("Not creating zip archive of serverpack.");
        }
        appLogger.info("Serverpack available at: " + modpackDir + "/serverpack");
        appLogger.info("Done!");
    }
}
