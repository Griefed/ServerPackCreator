package de.griefed.ServerPackCreator;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

class ConfigCheck {
    static Config conf;
    private static final Logger appLogger = LogManager.getLogger("ApplicationLogger");
    /** Check the config file for configuration errors. If an error is found, the log file will tell the user where the error is, so they can fix their config.
     * @return Return true if error is found in user's configuration. If an error is found, the application will exit in main.
     */
    static boolean checkConfig() {
        boolean configHasError = false;
        appLogger.info("Checking configuration...");
        try {
            conf = ConfigFactory.parseFile(Reference.configFile);
        } catch (ConfigException ex) {
            appLogger.error("Couldn't parse config file. Consider checking your config file and fixing empty values. If the value needs to be an empty string, leave its value to \"\".");
        }
        if (checkModpackDir(conf.getString("modpackDir"))) {
            Reference.modpackDir = conf.getString("modpackDir");
        } else { configHasError = true; }
        Reference.clientMods = conf.getStringList("clientMods");
        if (checkCopyDirs(conf.getStringList("copyDirs"), conf.getString("modpackDir"))) {
            Reference.copyDirs = conf.getStringList("copyDirs");
        } else { configHasError = true; }
        Reference.includeServerInstallation = convertToBoolean(conf.getString("includeServerInstallation"));
        if (Reference.includeServerInstallation) {
            if (checkJavaPath(conf.getString("javaPath"))) {
                Reference.javaPath = conf.getString("javaPath");
            } else {
                String tmpJavaPath = getJavaPath(conf.getString("javaPath"));
                if (checkJavaPath(tmpJavaPath)) {
                    Reference.javaPath = tmpJavaPath;
                } else {
                    configHasError = true;
                }
            }
            if (isMinecraftVersionCorrect(conf.getString("minecraftVersion"))) {
                Reference.minecraftVersion = conf.getString("minecraftVersion");
            } else { configHasError = true; }
            if (checkModloader(conf.getString("modLoader"))) {
                Reference.modLoader = setModloader(conf.getString("modLoader"));
            } else { configHasError = true; }
            if (checkModloaderVersion(Reference.modLoader, conf.getString("modLoaderVersion"), Reference.minecraftVersion)) {
                Reference.modLoaderVersion = conf.getString("modLoaderVersion");
            } else { configHasError = true; }
        } else {
            appLogger.info("Server installation disabled. Skipping check of:");
            appLogger.info("    Java path");
            appLogger.info("    Minecraft version");
            appLogger.info("    Modloader");
            appLogger.info("    Modloader version");
        }
        Reference.includeServerIcon = convertToBoolean(conf.getString("includeServerIcon"));
        Reference.includeServerProperties = convertToBoolean(conf.getString("includeServerProperties"));
        Reference.includeStartScripts = convertToBoolean(conf.getString("includeStartScripts"));
        Reference.includeZipCreation = convertToBoolean(conf.getString("includeZipCreation"));
        printConfig(Reference.modpackDir, Reference.clientMods, Reference.copyDirs, Reference.includeServerInstallation, Reference.javaPath, Reference.minecraftVersion, Reference.modLoader, Reference.modLoaderVersion, Reference.includeServerIcon, Reference.includeServerProperties, Reference.includeStartScripts, Reference.includeZipCreation);
        if (!configHasError) {
            appLogger.info("Config check successful. No errors encountered.");
        } else {
            appLogger.error("Config check not successful. Check your config for errors.");
        }
        return configHasError;
    }

    /** Prints the configuration.
     * @param modpackDirectory String. Path to modpack directory.
     * @param clientsideMods String List. List of clientside mods to delete from server pack.
     * @param copyDirectories String List. List of directories to copy to server pack.
     * @param installServer Boolean. Whether to install the modloader server.
     * @param javaInstallPath String. Path to Java installation needed to install modloader server.
     * @param minecraftVer String. Minecraft version the modpack uses.
     * @param modloader String. Modloader the modpack uses.
     * @param modloaderVersion String. Version of the modloader the modpack uses.
     * @param includeIcon Boolean. Whether to include the server-icon.png in the server pack.
     * @param includeProperties Boolean. Whether to include the server.properties in the server pack.
     * @param includeScripts Boolean. Whether to include start scripts for the specified modloader in the server pack.
     * @param includeZip Boolean. Whether to create a zip-archive of the server pack.
     */
    static void printConfig(String modpackDirectory, List<String> clientsideMods, List<String> copyDirectories, boolean installServer, String javaInstallPath, String minecraftVer, String modloader, String modloaderVersion, boolean includeIcon, boolean includeProperties, boolean includeScripts, boolean includeZip) {
        appLogger.info("Your configuration is:");
        appLogger.info("Modpack directory: " + modpackDirectory);
        if (clientsideMods.toArray().length == 0) {
            appLogger.warn("No client mods specified");
        } else {
            appLogger.info("Client mods specified. Client mods are:");
            for (int i = 0; i < clientsideMods.toArray().length; i++) {
                appLogger.info("    " + clientsideMods.get(i));
            }
        }
        appLogger.info("Directories to copy:");
        for (int i = 0; i < copyDirectories.toArray().length; i++) {
            appLogger.info("    " + copyDirectories.get(i));
        }
        appLogger.info("Include server installation:      " + installServer);
        appLogger.info("Java Installation path:           " + javaInstallPath);
        appLogger.info("Minecraft version:                " + minecraftVer);
        appLogger.info("Modloader:                        " + modloader);
        appLogger.info("Modloader Version:                " + modloaderVersion);
        appLogger.info("Include server icon:              " + includeIcon);
        appLogger.info("Include server properties:        " + includeProperties);
        appLogger.info("Include start scripts:            " + includeScripts);
        appLogger.info("Create zip-archive of serverpack: " + includeZip);
    }
    /** Check whether the specified modpack directory exists.
     * @param modpackDir String. The path to the modpack directory.
     * @return Boolean. Returns true if the directory exists. False if not.
     */
    static boolean checkModpackDir(String modpackDir) {
        boolean configCorrect = false;
        if (modpackDir.equals("")) {
            appLogger.error("Error: Modpack directory not specified. Please specify an existing directory.");
        } else if (!(new File(modpackDir).isDirectory())) {
            appLogger.error("Error: Modpack directory doesn't exist.Please specify an existing directory.");
        } else {
            configCorrect = true;
        }
        return configCorrect;
    }
    /** Check whether the specified directories exist in the modpack directory.
     * @param copyDirs String. The directories for which to check.
     * @param modpackDir String. The path to the modpack directory in which to check for directories.
     * @return Boolean. Returns true if all directories exist. False if any one does not.
     */
    static boolean checkCopyDirs(List<String> copyDirs, String modpackDir) {
        boolean configCorrect = true;
        if (copyDirs.isEmpty()) {
            appLogger.error("Error: No directories specified for copying. This would result in an empty serverpack.");
            configCorrect = false;
        } else {
            for (String copyDir : copyDirs) {
                File directory = new File(String.format("%s/%s", modpackDir, copyDir));
                if (!directory.exists() || !directory.isDirectory()) {
                    appLogger.error(String.format("Error: Specified directory %s does not exist. Please specify existing directories.", directory.getAbsolutePath()));
                    configCorrect = false;
                }
            }
        }
        return configCorrect;
    }
    /** Converts various strings to booleans.
     * @param stringBoolean String. The string which should be converted to boolean if it matches certain patterns.
     * @return Boolean. Returns the corresponding boolean if match with pattern was found. If no match is found, assume and return false.
     */
    static boolean convertToBoolean(String stringBoolean) {
        boolean returnBoolean;
        if (stringBoolean.matches("[Tt]rue") || stringBoolean.matches("1") || stringBoolean.matches("[Yy]es") || stringBoolean.matches("[Yy]")) {
            returnBoolean = true;
        } else if (stringBoolean.matches("[Ff]alse") || stringBoolean.matches("0") || stringBoolean.matches("[Nn]o") || stringBoolean.matches("[Nn]" )) {
            returnBoolean = false;
        } else {
            appLogger.warn("Warning. Couldn't parse boolean. Assuming false.");
            returnBoolean = false;
        }
        return returnBoolean;
    }
    /** Automatically set Java path if none is specified
     * @param enteredPath String. The path to check whether it is empty.
     * @return String. Return the entered Java path if it is not empty. Automatically determine path if empty.
     */
    static String getJavaPath(String enteredPath) {
        String autoJavaPath;
        if (enteredPath.equals("")) {
            appLogger.warn("You didn't specify the path to your Java installation. ServerPackCreator will try to determine it for you...");
            autoJavaPath = System.getProperty("java.home").replace("\\", "/") + "/bin/java";
            if (autoJavaPath.startsWith("C:")) {
                autoJavaPath = String.format("%s.exe", autoJavaPath);
            }
            appLogger.warn(String.format("ServerPackCreator set the path to your Java installation to: %s", autoJavaPath));
            return autoJavaPath;
        } else {
            appLogger.info(String.format("ServerPackCreator set the path to your Java installation to: %s", enteredPath));
            return enteredPath;
        }
    }
    /** Checks whether the correct path to the Java installation was set.
     * @param pathToJava String. The path to check for java.exe or java.
     * @return Boolean. Returns true if the path was correctly set. False if not.
     */
    static boolean checkJavaPath(String pathToJava) {
        boolean configCorrect = false;
        if (new File(pathToJava).exists() && pathToJava.endsWith("java.exe")) {
            configCorrect = true;
        } else if (new File(pathToJava).exists() && pathToJava.endsWith("java")) {
            configCorrect = true;
        } else {
            appLogger.error("Incorrect Java path specified.");
        }
        return configCorrect;
    }
    /** Checks whether Forge or Fabric were specified as modloader.
     * @param modloader String. Check case insensitive for Forge or Fabric.
     * @return Boolean. Returns true if the specified modloader is either Forge or Fabric. False if not.
     */
    static boolean checkModloader(String modloader) {
        boolean configCorrect = false;
        if (modloader.equalsIgnoreCase("Forge") || modloader.equalsIgnoreCase("Fabric")) {
            configCorrect = true;
        } else {
            appLogger.error("Error: Invalid modloader specified. Modloader must bei either Forge or Fabric.");
        }
        return configCorrect;
    }
    /** Standardize the specified modloader.
     * @param modloader String. If any case of Forge or Fabric was specified, return "Forge" or "Fabric", so users can enter "forge" or "fabric" or any combination of upper- and lowercase letters..
     * @return String. Returns a standardized String of the specified modloader.
     */
    static String setModloader(String modloader) {
        String returnLoader = null;
        if (modloader.equalsIgnoreCase("Forge")) {
            returnLoader = "Forge";
        } else if (modloader.equalsIgnoreCase("Fabric")) {
            returnLoader = "Fabric";
        }
        return returnLoader;
    }
    /** Determine whether to check for correct Forge or correct Fabric modloader version.
     * @param modloader String. Determines whether the check for Forge or Fabric is called.
     * @param modloaderVersion String. The version of the modloader to check for.
     * @param minecraftVersion String. The version of Minecraft for which to check for if modloader is Forge.
     * @return Boolean. Returns true if the specified modloader version is correct. False if not.
     */
    static boolean checkModloaderVersion(String modloader, String modloaderVersion, String minecraftVersion) {
        boolean isVersionCorrect = false;
        if (modloader.equalsIgnoreCase("Forge") && isForgeVersionCorrect(modloaderVersion, minecraftVersion)) {
            isVersionCorrect = true;
        } else if (modloader.equalsIgnoreCase("Fabric") && isFabricVersionCorrect(modloaderVersion)) {
            isVersionCorrect = true;
        } else {
            appLogger.error("Specified incorrect modloader version. Please check your modpack for the correct version and enter again.");
        }
        return isVersionCorrect;
    }
    /** Check the specified Minecraft version against Mojang's version manifest to validate the version.
     * @param minecraftVersion Minecraft version to check.
     * @return Boolean. Returns true if the specified Minecraft version could be found in Mojang's manifest. False if not.
     */
    static boolean isMinecraftVersionCorrect(String minecraftVersion) {
        if (!minecraftVersion.equals("")) {
            try {
                URL manifestJsonURL = new URL(Reference.MINECRAFT_MANIFEST_URL);
                ReadableByteChannel readableByteChannel = Channels.newChannel(manifestJsonURL.openStream());
                FileOutputStream downloadManifestOutputStream;
                try {
                    downloadManifestOutputStream = new FileOutputStream("mcmanifest.json");
                } catch (FileNotFoundException ex) {
                    appLogger.debug("Couldn't find mcmanifest.json", ex);
                    File file = new File("mcmanifest.json");
                    if (!file.exists()) {
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
        } else {
            appLogger.error("You didn't specify your Minecraft version.");
            return false;
        }
    }
    /** Check the specified Fabric version against Fabric's version manifest to validate the version.
     * @param fabricVersion String. The Fabric version to check.
     * @return Boolean. Returns true if the specified Fabric version could be found in Fabric's manifest. False if not.
     */
    static boolean isFabricVersionCorrect(String fabricVersion) {
        try {
            URL manifestJsonURL = new URL(Reference.FABRIC_MANIFEST_URL);
            ReadableByteChannel readableByteChannel = Channels.newChannel(manifestJsonURL.openStream());
            FileOutputStream downloadManifestOutputStream;
            try {
                downloadManifestOutputStream = new FileOutputStream("fabric-manifest.xml");
            } catch (FileNotFoundException ex) {
                appLogger.debug("Couldn't find fabric-manifest.xml.", ex);
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
            ArrayList<String> dataArray = new ArrayList<>();
            while (myReader.hasNextLine()) {
                dataArray.add(myReader.nextLine());
            }
            String[] data = new String[dataArray.size()];
            String manifestXML;
            dataArray.toArray(data);
            manifestXML = Arrays.toString(data);
            myReader.close();
            manifestXML = manifestXML.replaceAll("\\s", "");
            boolean contains = manifestXML.trim().contains(String.format("<version>%s</version>", fabricVersion));
            manifestXMLFile.deleteOnExit();
            return contains;
        } catch (Exception ex) {
            appLogger.error("An error occurred during Minecraft version validation.", ex);
            return false;
        }
    }
    /** Checks Forge version for errors (basically for its availability in Forge manifest)
     * @param forgeVersion String. The Forge version to check.
     * @param minecraftVersion String. The Minecraft version that the modpack uses. Needed to prevent usage of Forge, for example, from MC version 1.7.10, with 1.12.2.
     * @return Boolean. Returns true if Forge version correct and false if it isn't correct.
     */
    static boolean isForgeVersionCorrect(String forgeVersion, String minecraftVersion) {
        try {
            URL manifestJsonURL = new URL(Reference.FORGE_MANIFEST_URL);
            ReadableByteChannel readableByteChannel = Channels.newChannel(manifestJsonURL.openStream());
            FileOutputStream downloadManifestOutputStream;
            try {
                downloadManifestOutputStream = new FileOutputStream("forge-manifest.json");
            } catch (FileNotFoundException ex) {
                appLogger.debug("Couldn't find forge-manifest.json", ex);
                File file = new File("forge-manifest.json");
                if (!file.exists()){
                    appLogger.info("Forge Manifest JSON File does not exist, creating...");
                    boolean jsonCreated = file.createNewFile();
                    if (jsonCreated) {
                        appLogger.info("Forge Manifest JSON File created");
                    } else {
                        appLogger.error("Error. Could not create Forge Manifest JSON File.");
                    }
                }
                downloadManifestOutputStream = new FileOutputStream("forge-manifest.json");
            }
            FileChannel downloadManifestOutputStreamChannel = downloadManifestOutputStream.getChannel();
            downloadManifestOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
            downloadManifestOutputStream.flush();
            downloadManifestOutputStream.close();
            File manifestJsonFile = new File("forge-manifest.json");
            manifestJsonFile.deleteOnExit();
            Scanner myReader = new Scanner(manifestJsonFile);
            ArrayList<String> d = new ArrayList<>();
            while (myReader.hasNextLine()) {
                d.add(myReader.nextLine());
            }
            String[] data = new String[d.size()];
            String manifestJSON;
            d.toArray(data);
            manifestJSON = Arrays.toString(data);
            myReader.close();
            manifestJSON = manifestJSON.replaceAll("\\s", "");
            return manifestJSON.trim().contains(String.format("%s-%s", minecraftVersion, forgeVersion));
        } catch (Exception ex) {
            appLogger.error("An error occurred during Forge version validation.", ex);
            return false;
        }
    }
}