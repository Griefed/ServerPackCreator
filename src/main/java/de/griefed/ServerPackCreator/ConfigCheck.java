package de.griefed.ServerPackCreator;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

class ConfigCheck {
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
    static Boolean configHasError = false;
    private static final Logger appLogger = LogManager.getLogger("ConfigCheck");
    /** Check the config file for configuration errors. If an error is found, the log file will tell the user where the error is, so they can fix their config.
     *
     * @return Return true if error is found in user's configuration. If an error is found, the application will exit in main.
     */
    static boolean checkConfig() {
        appLogger.info("Checking configuration...");
        try {
            conf = ConfigFactory.parseFile(FilesSetup.configFile);
        } catch (ConfigException ex) {
            appLogger.error("One of your config values is empty. Consider checking your config file and fixing empty values. If the value needs to be an empty string, leave its value to \"\".");
        }
        if (conf.getString("modpackDir").equalsIgnoreCase("")) {
            configHasError = true;
            appLogger.error("Error: Modpack directory not specified.");
        } else if (!(new File(conf.getString("modpackDir")).isDirectory())) {
            configHasError = true;
            appLogger.error("Error: Modpack directory doesn't exist.");
        } else {
            modpackDir = conf.getString("modpackDir");
        }
        clientMods = conf.getStringList("clientMods");
        if (conf.getStringList("copyDirs").isEmpty()) {
            configHasError = true;
            appLogger.error("Error: No directories specified for copying. This would result in an empty serverpack.");
        } else {
            copyDirs = conf.getStringList("copyDirs");
        }
        try {
            includeServerInstallation = conf.getBoolean("includeServerInstallation");
        } catch (ConfigException ex) {
            configHasError = true;
            appLogger.error("Error: Wrong configuration for includeServerInstallation. Must be true or false.");
        }
        if (includeServerInstallation && (new File(conf.getString("javaPath")).exists())) {
            if (conf.getString("javaPath").endsWith(".exe")) {
                javaPath = conf.getString("javaPath").substring(0, conf.getString("javaPath").length() - 4);
            } else {
                javaPath = conf.getString("javaPath");
            }
            if (conf.getString("minecraftVersion").equals("")) {
                configHasError = true;
                appLogger.error("Error: Minecraft version is not specified.");
            } else {
                if (!ConfigCheck.isMinecraftVersionCorrect(conf.getString("minecraftVersion"))) {
                    configHasError = true;
                    appLogger.error("Error: Invalid Minecraft version specified.");
                } else {
                    minecraftVersion = conf.getString("minecraftVersion");
                    if (!conf.getString("modLoader").equals("Forge") && !conf.getString("modLoader").equals("Fabric") && !conf.getString("modLoader").equals("")) {
                        configHasError = true;
                        appLogger.error("Error: Incorrect mod loader specified.");
                    } else if (conf.getString("modLoader").equals("Fabric") && !ConfigCheck.isFabricVersionCorrect(conf.getString("modLoaderVersion"))) {
                        configHasError = true;
                        appLogger.error("Error: Incorrect Fabric version specified.");
                    /* @Whitebear60 Make the call for Forge version check here!
                    } else if (conf.getString("modLoader").equals("Forge") && !ConfigCheck.isForgeVersionCorrect(conf.getString("modLoaderVersion"))) {
                        configHasError = true;
                        appLogger.error("Error: Incorrect Forge version specified.");
                    */
                    } else {
                        if (conf.getString("modLoaderVersion").equals("")) {
                            configHasError = true;
                            appLogger.error("Error: Mod loader version is not specified.");
                        } else {
                            modLoaderVersion = conf.getString("modLoaderVersion");
                            modLoader = conf.getString("modLoader");
                        }
                    }
                }
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
        if (!configHasError) {
            appLogger.info("Config check successful. No errors encountered.");
        }
        appLogger.info("Your configuration is:");
        appLogger.info("Modpack directory: " + modpackDir);
        if (clientMods.toArray().length == 0) {
            appLogger.warn("No client mods specified");
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
        appLogger.info("Java Installation path:           " + ConfigFactory.parseFile(FilesSetup.configFile).getString("javaPath"));
        appLogger.info("Minecraft version:                " + minecraftVersion);
        appLogger.info("Modloader:                        " + modLoader);
        appLogger.info("Modloader Version:                " + modLoaderVersion);
        appLogger.info("Include server icon:              " + includeServerIcon.toString());
        appLogger.info("Include server properties:        " + includeServerProperties.toString());
        appLogger.info("Include start scripts:            " + includeStartScripts.toString());
        appLogger.info("Create zip-archive of serverpack: " + includeZipCreation.toString());
        return configHasError;
    }
    /** Optional. Check the specified Minecraft version against Mojang's version manifest to validate the version.
     *
     * @return Returns boolean depending on whether the specified Minecraft version could be found in Mojang's manifest.
     */
    static boolean isMinecraftVersionCorrect(String mcver) {
        try {
            File manifestJSONFile = new File(conf.getString("modpackDir") + "/manifest.json");
            manifestJSONFile.getParentFile().createNewFile();
        } catch (IOException ex) {
            appLogger.error("Error: Couldn't create Minecraft Manifest JSON", ex);
        }
        try {
            URL manifestJsonURL = new URL("https://launchermeta.mojang.com/mc/game/version_manifest.json");
            ReadableByteChannel readableByteChannel = Channels.newChannel(manifestJsonURL.openStream());
            FileOutputStream downloadManifestOutputStream = null;
            try {
                downloadManifestOutputStream = new FileOutputStream("mcmanifest.json");
            } catch (FileNotFoundException ex) {
                appLogger.debug("Couldn't find mcmanifest.json", ex);
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
            boolean contains = data.trim().contains(String.format("\"id\":\"%s\"", mcver));
            manifestJsonFile.deleteOnExit();
            return contains;
        } catch (Exception ex) {
            appLogger.error("An error occurred during Minecraft version validation.", ex);
            return false;
        }
    }
    /** Optional. Check the specified Minecraft version against Fabric's version manifest to validate the version.
     *
     * @return Returns boolean depending on whether the specified Minecraft version could be found in Fabric's manifest.
     */
    static boolean isFabricVersionCorrect(String version) {
        try {
            URL manifestJsonURL = new URL("https://maven.fabricmc.net/net/fabricmc/fabric-loader/maven-metadata.xml");
            ReadableByteChannel readableByteChannel = Channels.newChannel(manifestJsonURL.openStream());
            FileOutputStream downloadManifestOutputStream = null;
            try {
                downloadManifestOutputStream = new FileOutputStream("fabric-manifest.xml");
            } catch (FileNotFoundException ex) {
                appLogger.error("Error: Couldn't create Fabric Manifest JSON", ex);
                File file = new File("fabric-manifest.xml");
                if (!file.exists()){
                    appLogger.info("Fabric Manifest XML File does not exist, creating...");
                    boolean jsonCreated = file.createNewFile();
                    if (jsonCreated) {
                        appLogger.info("Fabric Manifest XML File created");
                    } else {
                        appLogger.error("Error. Could not create Fabric Manifest XML File.");
                    }
                }
                downloadManifestOutputStream = new FileOutputStream("fabric-manifest.xml");
            }
            FileChannel downloadManifestOutputStreamChannel = downloadManifestOutputStream.getChannel();
            downloadManifestOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
            downloadManifestOutputStream.flush();
            downloadManifestOutputStream.close();
            File manifestXMLFile = new File("fabric-manifest.xml");
            Scanner myReader = new Scanner(manifestXMLFile);
            ArrayList<String> d = new ArrayList<>();
            while (myReader.hasNextLine()) {
                d.add(myReader.nextLine());
            }
            String[] data = new String[d.size()];
            String manifestXML;
            d.toArray(data);
            manifestXML = Arrays.toString(data);
            myReader.close();
            manifestXML = manifestXML.replaceAll("\\s", "");
            boolean contains = manifestXML.trim().contains(String.format("<version>%s</version>", version));
            manifestXMLFile.deleteOnExit();
            return contains;
        } catch (Exception ex) {
            appLogger.error("An error occurred during Minecraft version validation.", ex);
            return false;
        }
    }
    /* @Whitebear60: Replace this with your Forge version check method.
    static boolean isForgeVersionCorrect(String version) {

    }
    */
}