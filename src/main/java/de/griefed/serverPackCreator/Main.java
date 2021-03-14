package de.griefed.serverPackCreator;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.List;
import java.util.Scanner;

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
    /** Warn user about WIP status. Get configuration from serverpackcreator.conf. Check configuration. Print configuration. Make calls according to configuration.
     * Basically, the main class makes the calls to every other class where the actual magic is happening. The main class of ServerPackCreator should never contain code which does work on the serverpack itself.
     *
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
        try {
            conf = ConfigFactory.parseFile(FilesSetup.configFile);
        } catch (ConfigException ex) {
            appLogger.error("One of your config values is empty. Consider checking your config file and fixing empty values. If the value needs to be an empty string, leave its value to \"\".");
        }
        modpackDir = conf.getString("modpackDir");
        clientMods = conf.getStringList("clientMods");
        copyDirs = conf.getStringList("copyDirs");
        try {
            includeServerInstallation = conf.getBoolean("includeServerInstallation");
        } catch (ConfigException ex) {
            configHasError = true;
            appLogger.error("Error: Wrong configuration for includeServerInstallation. Must be true or false.");
        }
        minecraftVersion = conf.getString("minecraftVersion");
        modLoader = conf.getString("modLoader");
        modLoaderVersion = conf.getString("modLoaderVersion");
        try {
            includeServerIcon = conf.getBoolean("includeServerIcon");
        } catch (ConfigException ex) {
            configHasError = true;
            appLogger.error("Error: Wrong configuration for includeServerIcon. Must be true or false.");
        }
        try {
            includeServerProperties = conf.getBoolean("includeServerProperties");
        } catch (ConfigException ex) {
            configHasError = true;
            appLogger.error("Error: Wrong configuration for includeServerProperties. Must be true or false.");
        }
        try {
            includeStartScripts = conf.getBoolean("includeStartScripts");
        } catch (ConfigException ex) {
            configHasError = true;
            appLogger.error("Error: Wrong configuration for includeStartScripts. Must be true or false.");
        }
        try {
            includeZipCreation = conf.getBoolean("includeZipCreation");
        } catch (ConfigException ex) {
            configHasError = true;
            appLogger.error("Error: Wrong configuration for includeZipCreation. Must be true or false.");
        }

        if (modpackDir.equalsIgnoreCase("")) {
            configHasError = true;
            appLogger.error("Error: Modpack directory not specified.");
        } else if (!(new File(modpackDir).isDirectory())) {
            configHasError = true;
            appLogger.error("Error: Modpack directory doesn't exist.");
        }

        if (includeServerInstallation && new File(conf.getString("javaPath")).exists()) {
            if (conf.getString("javaPath").endsWith(".exe")) {
                javaPath = conf.getString("javaPath").substring(0, conf.getString("javaPath").length() - 4);
            } else {
                javaPath = conf.getString("javaPath");
            }
            if (minecraftVersion.equals("")) {
                configHasError = true;
                appLogger.error("Error: Minecraft version is not specified.");
            } else {
                try {
                    File manifestJSONFile = new File(modpackDir + "/manifest.json");
                    manifestJSONFile.getParentFile().createNewFile();
                    appLogger.info("Created temp file: Minecraft Manifest JSON");
                } catch (IOException ex) {
                    appLogger.error("Error: Couldn't create Minecraft Manifest JSON", ex);
                }
                if (!isMinecraftVersionCorrect()) {
                    configHasError = true;
                    appLogger.error("Error: Invalid Minecraft version specified.");
                }
            }
            if (!modLoader.equals("Forge") && !modLoader.equals("Fabric")) {
                configHasError = true;
                appLogger.error("Error: Incorrect mod loader specified.");
            }
            if (modLoaderVersion.equals("")) {
                configHasError = true;
                appLogger.error("Error: Mod loader version is not specified.");
            }
        } else if (includeServerInstallation && !(new File(conf.getString("javaPath")).exists())) {
            configHasError = true;
            appLogger.error("Java could not be found. Check your configuration.");
            appLogger.error("Your configuration for javaPath was: " + conf.getString("javaPath"));
        } else if (!includeServerInstallation) {
            javaPath = conf.getString("javaPath");
            appLogger.info("Server installation disabled. Skipping check of:");
            appLogger.info("    Java path");
            appLogger.info("    Minecraft version");
            appLogger.info("    Modloader");
            appLogger.info("    Modloader version");
        } else {
            configHasError = true;
            appLogger.error("Server installation and/or Java path incorrect. Please check.");
            appLogger.error("Your configuration for javaPath was: " + conf.getString("javaPath"));
            appLogger.error("Your configuration for includeServerInstallation was: " + conf.getString("includeServerInstallation"));
        }

        if (copyDirs.isEmpty()) {
            configHasError = true;
            appLogger.error("Error: No directories specified for copying. This would result in an empty serverpack.");
        }

        if (!configHasError) {
            appLogger.info("Your configuration is:");
            appLogger.info("Modpack directory: " + modpackDir);
            clientModsExist = clientMods.toArray().length != 0;
            if (!clientModsExist) {
                appLogger.warn("Client mods do not exist in the modpack.");
            } else {
                appLogger.info("Client mods specified. Client mods are:");
                for (int i = 0; i < clientMods.toArray().length; i++) {
                    appLogger.info("    " + clientMods.get(i));
                }
            }
            appLogger.info("Directories to copy:");
            for (int i = 0; i < copyDirs.toArray().length; i++) {
                appLogger.info("    " + copyDirs.get(i));
            }
            appLogger.info("Include server installation:      " + includeServerInstallation.toString());
            appLogger.info("Java Installation path:           " + conf.getString("javaPath"));
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
            if (clientModsExist) {
                try {
                    ServerSetup.deleteClientMods(modpackDir, clientMods);
                } catch (IOException ex) {
                    appLogger.error("There was an error calling the deleteClientMods method.", ex);
                }
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
                ServerSetup.zipBuilder(modpackDir, modLoader, minecraftVersion, includeServerInstallation);
            } else {
                appLogger.info("Not creating zip archive of serverpack.");
            }
            appLogger.info("Serverpack available at: " + modpackDir + "/serverpack");
            appLogger.info("Done!");
        } else {
            appLogger.error("Config file has errors. Consider editing serverpackcreator.conf file that is located in directory with SPC.");
            System.exit(1);
        }
    }
    /** Optional. Check the specified Minecraft version against Mojang's version manifest to validate the version.
     *
     * @return Returns boolean depending on whether the specified Minecraft version could be found in Mojang#s manifest.
     */
    private static boolean isMinecraftVersionCorrect() {
        try {
            URL manifestJsonURL = new URL("https://launchermeta.mojang.com/mc/game/version_manifest.json");
            ReadableByteChannel readableByteChannel = Channels.newChannel(manifestJsonURL.openStream());
            FileOutputStream downloadManifestOutputStream = null;
            try {
                downloadManifestOutputStream = new FileOutputStream("mcmanifest.json");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                File file = new File("mcmanifest.json");
                if (!file.exists()){
                    appLogger.info("Manifest JSON File does not exist, creating...");
                    boolean jsonCreated = file.createNewFile();
                    if (jsonCreated) {
                        appLogger.info("Manifest JSON File created");
                    } else {
                        appLogger.error("Error. Could not create Manifest JSON File.");
                    }
                }
                downloadManifestOutputStream = new FileOutputStream("mcmanifest.json");
            }
            FileChannel downloadManifestOutputStreamChannel = downloadManifestOutputStream.getChannel();
            downloadManifestOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
            downloadManifestOutputStream.flush();
            downloadManifestOutputStream.close();
            File manifestJsonFile = new File("mcmanifest.json");
            Scanner myReader = new Scanner(manifestJsonFile);
            String data = myReader.nextLine();
            myReader.close();
            data = data.replaceAll("\\s", "");
            boolean contains = data.trim().contains(String.format("\"id\":\"%s\"", minecraftVersion));
            manifestJsonFile.deleteOnExit();
            return contains;
        } catch (Exception ex) {
            appLogger.error("An error occurred during Minecraft version validation.", ex);
            return false;
        }
    }
}