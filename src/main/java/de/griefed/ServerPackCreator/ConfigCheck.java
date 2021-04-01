package de.griefed.ServerPackCreator;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.therandomlabs.curseapi.CurseAPI;
import com.therandomlabs.curseapi.CurseException;
import com.typesafe.config.*;
import de.griefed.ServerPackCreator.CurseForgeModpack.Modpack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static de.griefed.ServerPackCreator.CurseForgeModpack.CreateModpack.curseForgeModpack;

class ConfigCheck {
    private static final Logger appLogger = LogManager.getLogger(ConfigCheck.class);
    /** Check the config file for configuration errors. If an error is found, the log file will tell the user where the error is, so they can fix their config.
     * @return Return true if error is found in user's configuration. If an error is found, the application will exit in main.
     */
    public static boolean checkConfig() {
        boolean configHasError;
        appLogger.info("Checking configuration...");
        try {
            Reference.conf = ConfigFactory.parseFile(Reference.configFile);
        } catch (ConfigException ex) {
            appLogger.error("Couldn't parse config file. Consider checking your config file and fixing empty values. If the value needs to be an empty string, leave its value to \"\".");
        }
        Reference.clientMods = Reference.conf.getStringList("clientMods");
        Reference.includeServerInstallation = convertToBoolean(Reference.conf.getString("includeServerInstallation"));
        Reference.includeServerIcon = convertToBoolean(Reference.conf.getString("includeServerIcon"));
        Reference.includeServerProperties = convertToBoolean(Reference.conf.getString("includeServerProperties"));
        Reference.includeStartScripts = convertToBoolean(Reference.conf.getString("includeStartScripts"));
        Reference.includeZipCreation = convertToBoolean(Reference.conf.getString("includeZipCreation"));
        if (checkModpackDir(Reference.conf.getString("modpackDir"))) {
            configHasError = isDir();
        } else if (checkCurseForge(Reference.conf.getString("modpackDir"))) {
            configHasError = isCurse();
        } else { configHasError = true; }
        printConfig(Reference.modpackDir, Reference.clientMods, Reference.copyDirs, Reference.includeServerInstallation, Reference.javaPath, Reference.minecraftVersion, Reference.modLoader, Reference.modLoaderVersion, Reference.includeServerIcon, Reference.includeServerProperties, Reference.includeStartScripts, Reference.includeZipCreation);
        if (!configHasError) {
            appLogger.info("Config check successful. No errors encountered.");
        } else {
            appLogger.error("Config check not successful. Check your config for errors.");
        }
        return configHasError;
    }
    /** Checks whether the specified modpack exists. If it does, the config file is checked for errors. Should any error be found, it will return true so the configCheck method informs the user about an invalid configuration.
     * @return Boolean. Returns true if an error is found during configuration check. False if the configuration is deemed valid.
     */
    private static boolean isDir() {
        boolean configHasError = false;
        Reference.modpackDir = Reference.conf.getString("modpackDir");
        if (checkCopyDirs(Reference.conf.getStringList("copyDirs"), Reference.conf.getString("modpackDir"))) {
            Reference.copyDirs = Reference.conf.getStringList("copyDirs");
        } else { configHasError = true; }
        if (Reference.includeServerInstallation) {
            if (checkJavaPath(Reference.conf.getString("javaPath"))) {
                Reference.javaPath = Reference.conf.getString("javaPath");
            } else {
                String tmpJavaPath = getJavaPath(Reference.conf.getString("javaPath"));
                if (checkJavaPath(tmpJavaPath)) {
                    Reference.javaPath = tmpJavaPath;
                } else {
                    configHasError = true;
                }
            }
            if (isMinecraftVersionCorrect(Reference.conf.getString("minecraftVersion"))) {
                Reference.minecraftVersion = Reference.conf.getString("minecraftVersion");
            } else { configHasError = true; }
            if (checkModloader(Reference.conf.getString("modLoader"))) {
                Reference.modLoader = setModloader(Reference.conf.getString("modLoader"));
            } else { configHasError = true; }
            if (checkModloaderVersion(Reference.modLoader, Reference.conf.getString("modLoaderVersion"), Reference.minecraftVersion)) {
                Reference.modLoaderVersion = Reference.conf.getString("modLoaderVersion");
            } else { configHasError = true; }
        } else {
            appLogger.info("Server installation disabled. Skipping check of:");
            appLogger.info("    Java path");
            appLogger.info("    Minecraft version");
            appLogger.info("    Modloader");
            appLogger.info("    Modloader version");
        }
        return configHasError;
    }
    /** Checks whether the specified projectID,fileID combination is a valid CurseForge project and file and whether the resulting directory exists. If the directory does not exist, make calls to other methods which create the modpack. Parses information gathered from the modpack to later replace the previous configuration file.
     * @return Boolean. Currently always returns true so ServerPackCreator does not go straight into server pack creation after the creation of the specified modpack. Gives the user the chance to check their config before actually creating the server pack.
     */
    private static boolean isCurse() {
        boolean configHasError = false;
        String[] projectFileIds = splitString(Reference.conf.getString("modpackDir"));
        try {
            if (CurseAPI.project(Integer.parseInt(projectFileIds[0])).isPresent()) {
                String projectName, displayName;
                projectName = displayName = "";
                try {
                    projectName = CurseAPI.project(Integer.parseInt(projectFileIds[0])).get().name();
                    try {
                        displayName = CurseAPI.project(Integer.parseInt(projectFileIds[0])).get().files().fileWithID(Integer.parseInt(projectFileIds[1])).displayName();
                    } catch (NullPointerException npe) {
                        appLogger.info("INFO: Display name not found. Setting display name as file name on disk.");
                        displayName = CurseAPI.project(Integer.parseInt(projectFileIds[0])).get().files().fileWithID(Integer.parseInt(projectFileIds[1])).nameOnDisk();
                    }
                } catch (CurseException cex) {
                    appLogger.error("Error: Could not retrieve CurseForge project and file.");
                }
                Reference.modpackDir = String.format("./%s/%s", projectName, displayName);
                if (curseForgeModpack(Reference.modpackDir, Integer.parseInt(projectFileIds[0]), Integer.parseInt(projectFileIds[1]))) {
                    try {
                        byte[] jsonData = Files.readAllBytes(Paths.get(String.format("%s/manifest.json", Reference.modpackDir)));
                        ObjectMapper objectMapper = new ObjectMapper();
                        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
                        objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
                        Modpack modpack = objectMapper.readValue(jsonData, Modpack.class);
                        String[] minecraftLoaderVersions = modpack.getMinecraft().toString().split(",");
                        String[] modLoaderVersion = minecraftLoaderVersions[1].replace("[", "").replace("]", "").split("-");
                        Reference.minecraftVersion = minecraftLoaderVersions[0].replace("[", "");
                        if (containsJumploader(modpack)) {
                            appLogger.info("Please make sure to check the configuration for the used Fabric version after ServerPackCreator is done setting up the modpack and new config file.");
                            Reference.modLoader = "Fabric";
                            Reference.modLoaderVersion = "0.7.2";
                        } else {
                            Reference.modLoader = setModloader(modLoaderVersion[0]);
                            Reference.modLoaderVersion = modLoaderVersion[1];
                        }
                    } catch (IOException ex) {
                        appLogger.error("Error: There was a fault during json parsing.", ex);
                    }
                    if (checkJavaPath(Reference.conf.getString("javaPath"))) {
                        Reference.javaPath = Reference.conf.getString("javaPath");
                    } else {
                        String tmpJavaPath = getJavaPath(Reference.conf.getString("javaPath"));
                        if (checkJavaPath(tmpJavaPath)) {
                            Reference.javaPath = tmpJavaPath;
                        }
                    }
                    Reference.copyDirs = suggestCopyDirs(Reference.modpackDir);
                    appLogger.info("Your old config file will now be replaced by a new one, with values gathered from the downloaded modpack.");
                    FilesSetup.writeConfigToFile(Reference.modpackDir, CLISetup.buildString(Reference.clientMods.toString()), CLISetup.buildString(Reference.copyDirs.toString()), Reference.includeServerInstallation, Reference.javaPath, Reference.minecraftVersion, Reference.modLoader, Reference.modLoaderVersion, Reference.includeServerIcon, Reference.includeServerProperties, Reference.includeStartScripts, Reference.includeZipCreation);
                }
                configHasError = true;
            }
        } catch (CurseException cex) {
            appLogger.error(String.format("Error: Project with ID %s could not be found", projectFileIds[0]), cex);
            configHasError = true;
        }
        return configHasError;
    }
    /** Checks for the Jumploader mod in the project list of the modpack. If Jumploader is found, the modloader in the configuration will be set to Fabric.
     * @param modpack Object. Contains information about our modpack. Used to get a list of all projects used in the modpack.
     * @return Boolean. Returns true if Jumploader is found, false if not found.
     */
    private static boolean containsJumploader(Modpack modpack) {
        boolean hasJumploader = false;
        for (int i = 0; i < modpack.getFiles().toArray().length; i++) {
            String[] mods;
            mods = modpack.getFiles().get(i).toString().split(",");
            try {
                if (CurseAPI.project(Integer.parseInt(mods[0])).get().name().toLowerCase().contains("jumploader")) {
                    appLogger.info("Jumploader detected. Setting modloader to Fabric.");
                    hasJumploader = true;
                }
            } catch (CurseException cex) {
                appLogger.error("Error: Encountered error during check for Jumploader.", cex);
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException iex) {
                appLogger.debug("Error during interruption event.", iex);
            }
        }
        return hasJumploader;
    }
    /** Creates a list of suggested directories to include in server pack which is later on written to a new configuration file.
     * @param modpackDir String. The directory for which to gather a list of directories.
     * @return List, String. Returns a list of directories inside the modpack, excluding well known client-side only directories which would not be needed by a server pack. If you have suggestions to this list, open an issue on https://github.com/Griefed/ServerPackCreator/issues
     */
    private static List<String> suggestCopyDirs(String modpackDir) {
        appLogger.info("Preparing a list of directories to include in server pack...");
        String[] dirsNotToCopy = {"overrides","packmenu","resourcepacks"};
        File directories = new File(modpackDir);
        String[] dirArray = directories.list((current, name) -> new File(current, name).isDirectory());
        List<String> dirList = new ArrayList<>(Arrays.asList(dirArray));
        List<String> doNotCopyList = new ArrayList<>(Arrays.asList(dirsNotToCopy));
        for (int i = 0; i< dirsNotToCopy.length; i++) {
            dirList.remove(doNotCopyList.get(i));
        }
        String[] copyDirs = dirList.toArray(new String[0]);
        appLogger.info(String.format("Modpack directory checked. Suggested directories for copyDirs-setting are: %s", dirList.toString()));
        return Arrays.asList(copyDirs.clone());
    }
    /** Checks whether the modpackDir contains a valid projectID,fileID combination. ProjectIDs must be at least two digits long, fileIDs must be at least 5 digits long. Must be numbers separated by a ",".
     * @param modpackDir String. The string which to check for a valid projectID,fileID combination.
     * @return Boolean. Returns true if the combination is deemed valid, false if not.
     */
    private static boolean checkCurseForge(String modpackDir) {
        boolean configCorrect = false;
        if (modpackDir.matches("[0-9]{2,},[0-9]{5,}")) {
            appLogger.info("You specified a CurseForge projectID and fileID combination.");
            configCorrect = true;
        } else {
            appLogger.error("Error: You did not specify a CurseForge projectID,fileID combination.");
        }
        return configCorrect;
    }
    /** Splits a string into an array with the separator being ",". Used to get the CurseForge projectID and fileID as separate string so they can later be parsed into integers and used with the CurseForgeAPI make downloads and gather information.
     * @param modpackDir String. The string which to split with the "," separator.
     * @return Array, String. Returns the array consisting of the projectID at 0 and the fileID at 1.
     */
    private static String[] splitString(String modpackDir) {
        String[] projectFileIds;
        projectFileIds = modpackDir.split(",");
        appLogger.info(String.format("You entered: ProjectID %s | FileID %s.", projectFileIds[0], projectFileIds[1]));
        return projectFileIds;
    }
    /** Converts various strings to booleans.
     * @param stringBoolean String. The string which should be converted to boolean if it matches certain patterns.
     * @return Boolean. Returns the corresponding boolean if match with pattern was found. If no match is found, assume and return false.
     */
    private static boolean convertToBoolean(String stringBoolean) {
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
        appLogger.info(String.format("Modpack directory: %s", modpackDirectory));
        if (clientsideMods.toArray().length == 0) {
            appLogger.warn("No client mods specified");
        } else {
            appLogger.info("Client mods specified. Client mods are:");
            for (int i = 0; i < clientsideMods.toArray().length; i++) {
                appLogger.info(String.format("    %s", clientsideMods.get(i)));
            }
        }
        appLogger.info("Directories to copy:");
        for (int i = 0; i < copyDirectories.toArray().length; i++) {
            appLogger.info(String.format("    %s", copyDirectories.get(i)));
        }
        appLogger.info(String.format("Include server installation:      %s", installServer));
        appLogger.info(String.format("Java Installation path:           %s", javaInstallPath));
        appLogger.info(String.format("Minecraft version:                %s", minecraftVer));
        appLogger.info(String.format("Modloader:                        %s", modloader));
        appLogger.info(String.format("Modloader Version:                %s", modloaderVersion));
        appLogger.info(String.format("Include server icon:              %s", includeIcon));
        appLogger.info(String.format("Include server properties:        %s", includeProperties));
        appLogger.info(String.format("Include start scripts:            %s", includeScripts));
        appLogger.info(String.format("Create zip-archive of serverpack: %s", includeZip));
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
            appLogger.warn(String.format("Warning: Couldn't find a directory with setting %s. Checking for CurseForge projectID,fileID next...", modpackDir));
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
                Scanner jsonReader = new Scanner(manifestJsonFile);
                String jsonData = jsonReader.nextLine();
                jsonReader.close();
                jsonData = jsonData.replaceAll("\\s", "");
                boolean contains = jsonData.trim().contains(String.format("\"id\":\"%s\"", minecraftVersion));
                manifestJsonFile.deleteOnExit();
                return contains;
            } catch (Exception ex) {
                appLogger.error(String.format("Error: Could not validate Minecraft version %s.", minecraftVersion), ex);
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
            Scanner xmlReader = new Scanner(manifestXMLFile);
            ArrayList<String> dataList = new ArrayList<>();
            while (xmlReader.hasNextLine()) {
                dataList.add(xmlReader.nextLine());
            }
            String[] dataArray = new String[dataList.size()];
            String manifestXML;
            dataList.toArray(dataArray);
            manifestXML = Arrays.toString(dataArray);
            xmlReader.close();
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
            Scanner jsonReader = new Scanner(manifestJsonFile);
            ArrayList<String> dataList = new ArrayList<>();
            while (jsonReader.hasNextLine()) {
                dataList.add(jsonReader.nextLine());
            }
            String[] dataArray = new String[dataList.size()];
            String manifestJSON;
            dataList.toArray(dataArray);
            manifestJSON = Arrays.toString(dataArray);
            jsonReader.close();
            manifestJSON = manifestJSON.replaceAll("\\s", "");
            return manifestJSON.trim().contains(String.format("%s-%s", minecraftVersion, forgeVersion));
        } catch (Exception ex) {
            appLogger.error("An error occurred during Forge version validation.", ex);
            return false;
        }
    }
}