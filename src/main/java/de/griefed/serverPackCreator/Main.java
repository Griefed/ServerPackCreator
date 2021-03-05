package de.griefed.serverPackCreator;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

public class Main {
    static String modpackDir;
    static Boolean clientModsExist;
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
    static Boolean configHasError = false;

    private static final Logger appLogger = LogManager.getLogger("Main");
    private static final Logger errorLogger = LogManager.getLogger("MainError");

    public static void main(String[] args) throws FileNotFoundException{
        appLogger.info("################################################################");
        appLogger.info("#         WORK IN PROGRESS! CONSIDER THIS ALPHA-STATE!         #");
        appLogger.info("#  USE AT YOUR OWN RISK! BE AWARE THAT DATA LOSS IS POSSIBLE!  #");
        appLogger.info("#         I CAN NOT BE HELD RESPONSIBLE FOR DATA LOSS!         #");
        appLogger.info("#                    YOU HAVE BEEN WARNED!                     #");
        appLogger.info("################################################################");
        // Get name of the jar so log states version number. Good for issues on GitHub.
        try {
            // Get path of the JAR file
            String jarPath = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            appLogger.info("JAR Path: " + jarPath);

            // Get name of the JAR file
            String jarName = jarPath.substring(jarPath.lastIndexOf("/") + 1);
            appLogger.info("JAR Name: " + jarName);

        } catch (URISyntaxException ex) {
            errorLogger.error("Error getting jar name.", ex);
        }
        // Generate default files if they do not exist and exit if serverpackcreator.conf was created
        FilesSetup.filesSetup();
        // Setup config variables with config file
        appLogger.info("Getting configuration...");
        conf = ConfigFactory.parseFile(FilesSetup.configFile);
        modpackDir = conf.getString("modpackDir");
        clientModsExist = conf.getBoolean("clientModsExist");
        clientMods = conf.getStringList("clientMods");
        copyDirs = conf.getStringList("copyDirs");
        includeServerInstallation = conf.getBoolean("includeServerInstallation");
        javaPath = conf.getString("javaPath");
        minecraftVersion = conf.getString("minecraftVersion");
        modLoader = conf.getString("modLoader");
        modLoaderVersion = conf.getString("modLoaderVersion");
        includeServerIcon = conf.getBoolean("includeServerIcon");
        includeServerProperties = conf.getBoolean("includeServerProperties");
        includeStartScripts = conf.getBoolean("includeStartScripts");
        includeZipCreation = conf.getBoolean("includeZipCreation");

        if (modpackDir.equalsIgnoreCase("")) {
            configHasError = true;
            errorLogger.error("Error: Modpack directory not specified.");
        }

        if (clientModsExist) {
            if (clientMods.isEmpty()) {
                configHasError = true;
                errorLogger.error("Error: Specified that client-only mods exist in the modpack but don't specified what mods is client-only.");
                errorLogger.info("Consider changing clientModsExist to false or providing a list of client-only mods in clientMods field.");
            }
        }

        if (copyDirs.isEmpty()) {
            configHasError = true;
            errorLogger.error("Error: Don't specified which directories to copy.");
        }

        if (javaPath.equals("") || !javaPath.endsWith("java")) {
            configHasError = true;
            errorLogger.error("Error: Java path is not specified or incorrect.");
            errorLogger.info("Consider specifying a valid path to java executable.");
        }

        if (minecraftVersion.equals("")) {
            configHasError = true;
            errorLogger.error("Error: Minecraft version is not specified.");
        }

        if (!modLoader.equals("Forge") && !modLoader.equals("Fabric")) {
            configHasError = true;
            errorLogger.error("Error: Incorrect mod loader specified.");
        }

        if (modLoaderVersion.equals("")) {
            configHasError = true;
            errorLogger.error("Error: Mod loader version is not specified.");
        }

        if (!configHasError) {
            File serverPackDir = new File(modpackDir);
            if (!serverPackDir.isDirectory()) {
                throw new FileNotFoundException("Modpack directory doesn't exist.");
            }

            // Print current configuration so in case of error, logs are more informative
            appLogger.info("Your configuration is:");
            appLogger.info("Modpack directory: " + modpackDir);
            if (clientModsExist) {
                appLogger.info("Client mods does not exist in the modpack.");
            } else {
                appLogger.info("Client mods are:");
            }
            for (int i = 0; i < clientMods.toArray().length; i++) {
                appLogger.info("    " + clientMods.get(i));
            }
            appLogger.info("Directories to copy:");
            for (int i = 0; i < copyDirs.toArray().length; i++) {
                appLogger.info("    " + copyDirs.get(i));
            }
            appLogger.info("Include server installation:      " + includeServerInstallation.toString());
            appLogger.info("Java Installation path:           " + javaPath);
            appLogger.info("Minecraft version:                " + minecraftVersion);
            appLogger.info("Modloader:                        " + modLoader);
            appLogger.info("Modloader Version:                " + modLoaderVersion);
            appLogger.info("Include server icon:              " + includeServerIcon.toString());
            appLogger.info("Include server properties:        " + includeServerProperties.toString());
            appLogger.info("Include start scripts:            " + includeStartScripts.toString());
            appLogger.info("Create zip-archive of serverpack: " + includeZipCreation.toString());
            // Copy all specified directories from modpack to serverpack.
            try {
                CopyFiles.copyFiles(modpackDir, copyDirs);
            } catch (IOException ex) {
                errorLogger.error("There was an error calling the copyFiles method.", ex);
            }

            if (clientModsExist) {
                // Delete client-side mods from serverpack.
                try {
                    ServerSetup.deleteClientMods(modpackDir, clientMods);
                } catch (IOException ex) {
                    errorLogger.error("There was an error calling the deleteClientMods method.", ex);
                }
            }
            // Generate Forge/Fabric start scripts and copy to serverpack.
            CopyFiles.copyStartScripts(modpackDir, modLoader, includeStartScripts);

            if (includeServerInstallation) {
                ServerSetup.installServer(modLoader, modpackDir, minecraftVersion, modLoaderVersion, javaPath);
            } else {
                appLogger.info("Not installing modded server.");
            }
            // If true, copy server-icon to serverpack.
            if (includeServerIcon) {
                CopyFiles.copyIcon(modpackDir);
            } else {
                appLogger.info("Not including servericon.");
            }
            // If true, copy server.properties to serverpack.
            if (includeServerProperties) {
                CopyFiles.copyProperties(modpackDir);
            } else {
                appLogger.info("Not including server.properties.");
            }
            // If true, create zip archive of serverpack.
            if (includeZipCreation) {
                ServerSetup.zipBuilder(modpackDir, modLoader, minecraftVersion);
            } else {
                appLogger.info("Not creating zip archive of serverpack.");
            }
            appLogger.info("Serverpack available at: " + modpackDir + "/serverpack");
            appLogger.info("Done!");
        } else {
            System.out.println();
            appLogger.info("Config file has errors. Consider editing serverpackcreator.conf file that is located in directory with SPC.");
        }
    }
}
