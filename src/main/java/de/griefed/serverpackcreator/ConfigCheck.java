package de.griefed.serverpackcreator;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.therandomlabs.curseapi.CurseAPI;
import com.therandomlabs.curseapi.CurseException;
import com.typesafe.config.*;
import de.griefed.serverpackcreator.curseforgemodpack.Modpack;
import de.griefed.serverpackcreator.i18n.LocalizationManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

class ConfigCheck {
    private static final Logger appLogger = LogManager.getLogger(ConfigCheck.class);

    /** Check the config file for configuration errors. If an error is found, the log file will tell the user where the error is, so they can fix their config.
     * @return Return true if error is found in user's configuration. If an error is found, the application will exit in main.
     */
    public boolean checkConfig() {
        boolean configHasError;
        appLogger.info(LocalizationManager.getLocalizedString("configcheck.log.info.checkconfig.start"));
        try {
            Reference.config = ConfigFactory.parseFile(Reference.configFile);
        } catch (ConfigException ex) {
            appLogger.error(LocalizationManager.getLocalizedString("configcheck.log.error.checkconfig.start"));
        }

        Reference.clientMods = Reference.config.getStringList("clientMods");
        Reference.includeServerInstallation = convertToBoolean(Reference.config.getString("includeServerInstallation"));
        Reference.includeServerIcon = convertToBoolean(Reference.config.getString("includeServerIcon"));
        Reference.includeServerProperties = convertToBoolean(Reference.config.getString("includeServerProperties"));
        Reference.includeStartScripts = convertToBoolean(Reference.config.getString("includeStartScripts"));
        Reference.includeZipCreation = convertToBoolean(Reference.config.getString("includeZipCreation"));

        if (checkModpackDir(Reference.config.getString("modpackDir"))) {
            configHasError = isDir(Reference.config.getString("modpackDir"));
        } else if (checkCurseForge(Reference.config.getString("modpackDir"))) {
            configHasError = isCurse();
        } else {
            configHasError = true;
        }

        printConfig(Reference.modpackDir,
                Reference.clientMods,
                Reference.copyDirs,
                Reference.includeServerInstallation,
                Reference.javaPath,
                Reference.minecraftVersion,
                Reference.modLoader,
                Reference.modLoaderVersion,
                Reference.includeServerIcon,
                Reference.includeServerProperties,
                Reference.includeStartScripts,
                Reference.includeZipCreation);

        if (!configHasError) {
            appLogger.info(LocalizationManager.getLocalizedString("configcheck.log.info.checkconfig.success"));
        } else {
            appLogger.error(LocalizationManager.getLocalizedString("configcheck.log.error.checkconfig.failure"));
        }
        return configHasError;
    }

    /** Checks whether the specified modpack exists. If it does, the config file is checked for errors. Should any error be found, it will return true so the configCheck method informs the user about an invalid configuration.
     * @param modpackDir String. Should an existing modpack be specified, all configurations are read from local file and the server pack is created, if config is correct.
     * @return Boolean. Returns true if an error is found during configuration check. False if the configuration is deemed valid.
     */
    private boolean isDir(String modpackDir) {
        boolean configHasError = false;
        Reference.modpackDir = modpackDir;

        if (checkCopyDirs(Reference.config.getStringList("copyDirs"), Reference.modpackDir)) {
            Reference.copyDirs = Reference.config.getStringList("copyDirs");
        } else { configHasError = true; }

        if (Reference.includeServerInstallation) {
            if (checkJavaPath(Reference.config.getString("javaPath"))) {
                Reference.javaPath = Reference.config.getString("javaPath");
            } else {
                String tmpJavaPath = getJavaPath(Reference.config.getString("javaPath"));
                if (checkJavaPath(tmpJavaPath)) {
                    Reference.javaPath = tmpJavaPath;
                } else { configHasError = true; } }

            if (isMinecraftVersionCorrect(Reference.config.getString("minecraftVersion"))) {
                Reference.minecraftVersion = Reference.config.getString("minecraftVersion");
            } else { configHasError = true; }

            if (checkModloader(Reference.config.getString("modLoader"))) {
                Reference.modLoader = setModloader(Reference.config.getString("modLoader"));
            } else { configHasError = true; }

            if (checkModloaderVersion(Reference.modLoader, Reference.config.getString("modLoaderVersion"), Reference.minecraftVersion)) {
                Reference.modLoaderVersion = Reference.config.getString("modLoaderVersion");
            } else { configHasError = true; }

        } else {
            appLogger.info(LocalizationManager.getLocalizedString("configcheck.log.info.checkconfig.skipstart"));
            appLogger.info(LocalizationManager.getLocalizedString("configcheck.log.info.checkconfig.skipjava"));
            appLogger.info(LocalizationManager.getLocalizedString("configcheck.log.info.checkconfig.skipminecraft"));
            appLogger.info(LocalizationManager.getLocalizedString("configcheck.log.info.checkconfig.skipmodlaoder"));
            appLogger.info(LocalizationManager.getLocalizedString("configcheck.log.info.checkconfig.skipmodloaderversion"));
        }
        return configHasError;
    }

    /** Checks whether the specified projectID,fileID combination is a valid CurseForge project and file and whether the resulting directory exists. If the directory does not exist, make calls to other methods which create the modpack. Parses information gathered from the modpack to later replace the previous configuration file.
     * @return Boolean. Currently always returns true so serverpackcreator does not go straight into server pack creation after the creation of the specified modpack. Gives the user the chance to check their config before actually creating the server pack.
     */
    private boolean isCurse() {
        boolean configHasError = false;
        try {
            if (CurseAPI.project(Reference.projectID).isPresent()) {
                String projectName, displayName;
                projectName = displayName = "";
                try { projectName = CurseAPI.project(Reference.projectID).get().name();

                    try { displayName = Objects.requireNonNull(CurseAPI.project(Reference.projectID).get().files().fileWithID(Reference.projectFileID)).displayName(); }
                        catch (NullPointerException npe) { appLogger.info(LocalizationManager.getLocalizedString("configcheck.log.info.iscurse.display"));

                    try { displayName = Objects.requireNonNull(CurseAPI.project(Reference.projectID).get().files().fileWithID(Reference.projectFileID)).nameOnDisk(); }
                        catch (NullPointerException npe2) { displayName = String.format("%d", Reference.projectFileID); } } }

                catch (CurseException cex) { appLogger.error(LocalizationManager.getLocalizedString("configcheck.log.error.iscurse.curseforge")); }

                Reference.modpackDir = String.format("./%s/%s", projectName, displayName);

                if (Reference.createModpack.curseForgeModpack(Reference.modpackDir, Reference.projectID, Reference.projectFileID)) {
                    try {
                        byte[] jsonData = Files.readAllBytes(Paths.get(String.format("%s/manifest.json", Reference.modpackDir)));
                        ObjectMapper objectMapper = new ObjectMapper();
                        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
                        objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
                        Modpack modpack = objectMapper.readValue(jsonData, Modpack.class);

                        String[] minecraftLoaderVersions = modpack
                                .getMinecraft()
                                .toString()
                                .split(",");
                        String[] modLoaderVersion = minecraftLoaderVersions[1]
                                .replace("[", "")
                                .replace("]", "")
                                .split("-");
                        Reference.minecraftVersion = minecraftLoaderVersions[0]
                                .replace("[", "");

                        if (containsFabric(modpack)) {
                            appLogger.info(LocalizationManager.getLocalizedString("configcheck.log.info.iscurse.fabric"));
                            Reference.modLoader = "Fabric";
                            Reference.modLoaderVersion = latestFabricLoader(Reference.modpackDir);
                        } else {
                            Reference.modLoader = setModloader(modLoaderVersion[0]);
                            Reference.modLoaderVersion = modLoaderVersion[1];
                        }
                    } catch (IOException ex) { appLogger.error(LocalizationManager.getLocalizedString("configcheck.log.error.iscurse.json"), ex); }

                    if (checkJavaPath(Reference.config.getString("javaPath"))) {
                        Reference.javaPath = Reference.config.getString("javaPath");
                    } else {
                        String tmpJavaPath = getJavaPath(Reference.config.getString("javaPath"));
                        if (checkJavaPath(tmpJavaPath)) {
                            Reference.javaPath = tmpJavaPath;
                        }
                    }
                    Reference.copyDirs = suggestCopyDirs(Reference.modpackDir);
                    appLogger.info(LocalizationManager.getLocalizedString("configcheck.log.info.iscurse.replace"));
                    Reference.filesSetup.writeConfigToFile(
                            Reference.modpackDir,
                            Reference.cliSetup.buildString(Reference.clientMods.toString()),
                            Reference.cliSetup.buildString(Reference.copyDirs.toString()),
                            Reference.includeServerInstallation, Reference.javaPath,
                            Reference.minecraftVersion, Reference.modLoader,
                            Reference.modLoaderVersion, Reference.includeServerIcon,
                            Reference.includeServerProperties, Reference.includeStartScripts,
                            Reference.includeZipCreation
                    );
                }
            }
        } catch (CurseException cex) {
            appLogger.error(String.format(LocalizationManager.getLocalizedString("configcheck.log.error.iscurse.project"), Reference.projectID), cex);
            configHasError = true;
        }
        return configHasError;
    }

    /** Checks for the Jumploader mod in the project list of the modpack. If Jumploader is found, the modloader in the configuration will be set to Fabric.
     * @param modpack Object. Contains information about our modpack. Used to get a list of all projects used in the modpack.
     * @return Boolean. Returns true if Jumploader is found, false if not found.
     */
    private boolean containsFabric(Modpack modpack) {
        boolean hasJumploader = false;
        for (int i = 0; i < modpack.getFiles().size(); i++) {
            String[] mods;
            mods = modpack.getFiles().get(i).toString().split(",");
            if (mods[0].equalsIgnoreCase("361988") || mods[0].equalsIgnoreCase("306612")) {
                appLogger.info(LocalizationManager.getLocalizedString("configcheck.log.info.containsfabric"));
                hasJumploader = true;
            }
        }
        return hasJumploader;
    }

    /** Creates a list of suggested directories to include in server pack which is later on written to a new configuration file.
     * @param modpackDir String. The directory for which to gather a list of directories.
     * @return List, String. Returns a list of directories inside the modpack, excluding well known client-side only directories which would not be needed by a server pack. If you have suggestions to this list, open an issue on https://github.com/Griefed/ServerPackCreator/issues
     */
    private List<String> suggestCopyDirs(String modpackDir) {
        appLogger.info(LocalizationManager.getLocalizedString("configcheck.log.info.suggestcopydirs.start"));

        String[] dirsNotToCopy = {
                "overrides",
                "packmenu",
                "resourcepacks"
        };
        String[] copyDirs = new String[0];
        List<String> dirList;
        File directories = new File(modpackDir);

        String[] dirArray = directories.list((current, name) -> new File(current, name).isDirectory());

        if (dirArray != null) {
            dirList = new ArrayList<>(Arrays.asList(dirArray));
            List<String> doNotCopyList = new ArrayList<>(Arrays.asList(dirsNotToCopy));
            for (int i = 0; i < dirsNotToCopy.length; i++) {
                dirList.remove(doNotCopyList.get(i));
            }
            copyDirs = dirList.toArray(new String[0]);
            appLogger.info(String.format(LocalizationManager.getLocalizedString("configcheck.log.info.suggestcopydirs.list"), dirList));

            return Arrays.asList(copyDirs.clone());

        } else { appLogger.error(LocalizationManager.getLocalizedString("configcheck.log.error.suggestcopydirs")); }

        return Arrays.asList(copyDirs.clone());
    }

    /** Checks whether the modpackDir contains a valid projectID,fileID combination. ProjectIDs must be at least two digits long, fileIDs must be at least 5 digits long. Must be numbers separated by a ",".
     * @param modpackDir String. The string which to check for a valid projectID,fileID combination.
     * @return Boolean. Returns true if the combination is deemed valid, false if not.
     */
    boolean checkCurseForge(String modpackDir) {
        String[] projectFileIds;
        boolean configCorrect = false;

        if (modpackDir.matches("[0-9]{2,},[0-9]{5,}")) {
            projectFileIds = modpackDir.split(",");
            Reference.projectID = Integer.parseInt(projectFileIds[0]);
            Reference.projectFileID = Integer.parseInt(projectFileIds[1]);

            appLogger.info(LocalizationManager.getLocalizedString("configcheck.log.info.checkcurseforge.info"));
            appLogger.info(String.format(LocalizationManager.getLocalizedString("configcheck.log.info.checkcurseforge.return"), Reference.projectID, Reference.projectFileID));
            appLogger.warn(LocalizationManager.getLocalizedString("configcheck.log.warn.checkcurseforge.warn"));

            configCorrect = true;

        } else { appLogger.warn(LocalizationManager.getLocalizedString("configcheck.log.warn.checkcurseforge.warn2")); }

        return configCorrect;
    }

    /** Converts various strings to booleans.
     * @param stringBoolean String. The string which should be converted to boolean if it matches certain patterns.
     * @return Boolean. Returns the corresponding boolean if match with pattern was found. If no match is found, assume and return false.
     */
    private boolean convertToBoolean(String stringBoolean) {
        boolean returnBoolean;
        if (stringBoolean.matches("[Tt]rue") ||
            stringBoolean.matches("1")       ||
            stringBoolean.matches("[Yy]es")  ||
            stringBoolean.matches("[Yy]"))    {

            returnBoolean = true;
        } else if (stringBoolean.matches("[Ff]alse") ||
                   stringBoolean.matches("0")        ||
                   stringBoolean.matches("[Nn]o")    ||
                   stringBoolean.matches("[Nn]" ))    {

            returnBoolean = false;
        } else {
            appLogger.warn(LocalizationManager.getLocalizedString("configcheck.log.warn.converttoboolean.warn"));
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
    void printConfig(String modpackDirectory,
                            List<String> clientsideMods,
                            List<String> copyDirectories,
                            boolean installServer,
                            String javaInstallPath,
                            String minecraftVer,
                            String modloader,
                            String modloaderVersion,
                            boolean includeIcon,
                            boolean includeProperties,
                            boolean includeScripts,
                            boolean includeZip) {
        appLogger.info(LocalizationManager.getLocalizedString("configcheck.log.info.printconfig.start"));
        appLogger.info(String.format(LocalizationManager.getLocalizedString("configcheck.log.info.printconfig.modpackdir"), modpackDirectory));
        if (clientsideMods.size() == 0) {
            appLogger.warn(LocalizationManager.getLocalizedString("configcheck.log.warn.printconfig.noclientmods"));
        } else {
            appLogger.info(LocalizationManager.getLocalizedString("configcheck.log.info.printconfig.clientmods"));
            for (int i = 0; i < clientsideMods.size(); i++) {
                appLogger.info(String.format("    %s", clientsideMods.get(i))); }
        }

        appLogger.info(LocalizationManager.getLocalizedString("configcheck.log.info.printconfig.copydirs"));
        if (copyDirectories != null) {
            for (int i = 0; i < copyDirectories.size(); i++) {
                appLogger.info(String.format("    %s", copyDirectories.get(i))); }
        } else { appLogger.error(LocalizationManager.getLocalizedString("configcheck.log.error.printconfig.copydirs")); }

        appLogger.info(String.format(LocalizationManager.getLocalizedString("configcheck.log.info.printconfig.server"), installServer));
        appLogger.info(String.format(LocalizationManager.getLocalizedString("configcheck.log.info.printconfig.javapath"), javaInstallPath));
        appLogger.info(String.format(LocalizationManager.getLocalizedString("configcheck.log.info.printconfig.minecraftversion"), minecraftVer));
        appLogger.info(String.format(LocalizationManager.getLocalizedString("configcheck.log.info.printconfig.modloader"), modloader));
        appLogger.info(String.format(LocalizationManager.getLocalizedString("configcheck.log.info.printconfig.modloaderversion"), modloaderVersion));
        appLogger.info(String.format(LocalizationManager.getLocalizedString("configcheck.log.info.printconfig.icon"), includeIcon));
        appLogger.info(String.format(LocalizationManager.getLocalizedString("configcheck.log.info.printconfig.properties"), includeProperties));
        appLogger.info(String.format(LocalizationManager.getLocalizedString("configcheck.log.info.printconfig.scripts"), includeScripts));
        appLogger.info(String.format(LocalizationManager.getLocalizedString("configcheck.log.info.printconfig.zip"), includeZip));
    }

    /** Check whether the specified modpack directory exists.
     * @param modpackDir String. The path to the modpack directory.
     * @return Boolean. Returns true if the directory exists. False if not.
     */
    boolean checkModpackDir(String modpackDir) {
        boolean configCorrect = false;
        if (modpackDir.equals("")) {
            appLogger.error(LocalizationManager.getLocalizedString("configcheck.log.error.checkmodpackdir"));
        } else if (!(new File(modpackDir).isDirectory())) {
            appLogger.warn(String.format(LocalizationManager.getLocalizedString("configcheck.log.warn.checkmodpackdir"), modpackDir));
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
    boolean checkCopyDirs(List<String> copyDirs, String modpackDir) {
        boolean configCorrect = true;
        if (copyDirs.isEmpty()) {
            appLogger.error(LocalizationManager.getLocalizedString("configcheck.log.error.checkcopydirs.empty"));
            configCorrect = false;
        } else {
            for (int i = 0; i < copyDirs.size(); i++) {
                File directory = new File(String.format("%s/%s", modpackDir, copyDirs.get(i)));
                if (!directory.exists() || !directory.isDirectory()) {
                    appLogger.error(String.format(LocalizationManager.getLocalizedString("configcheck.log.error.checkcopydirs.notfound"), directory.getAbsolutePath()));
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
    String getJavaPath(String enteredPath) {
        String autoJavaPath;
        if (enteredPath.equals("")) {
            appLogger.warn(LocalizationManager.getLocalizedString("configcheck.log.warn.getjavapath.empty"));
            autoJavaPath = String.format("%s/bin/java",System.getProperty("java.home").replace("\\", "/"));
            if (autoJavaPath.startsWith("C:")) {
                autoJavaPath = String.format("%s.exe", autoJavaPath);
            }
            appLogger.warn(String.format(LocalizationManager.getLocalizedString("configcheck.log.warn.getjavapath.set"), autoJavaPath));
            return autoJavaPath;
        } else {
            appLogger.info(String.format(LocalizationManager.getLocalizedString("configcheck.log.warn.getjavapath.set"), enteredPath));
            return enteredPath;
        }
    }

    /** Checks whether the correct path to the Java installation was set.
     * @param pathToJava String. The path to check for java.exe or java.
     * @return Boolean. Returns true if the path was correctly set. False if not.
     */
    boolean checkJavaPath(String pathToJava) {
        boolean configCorrect = false;
        if (new File(pathToJava).exists() && pathToJava.endsWith("java.exe")) {
            configCorrect = true;
        } else if (new File(pathToJava).exists() && pathToJava.endsWith("java")) {
            configCorrect = true;
        } else {
            appLogger.error(LocalizationManager.getLocalizedString("configcheck.log.error.checkjavapath"));
        }
        return configCorrect;
    }

    /** Checks whether Forge or Fabric were specified as modloader.
     * @param modloader String. Check case insensitive for Forge or Fabric.
     * @return Boolean. Returns true if the specified modloader is either Forge or Fabric. False if not.
     */
    boolean checkModloader(String modloader) {
        boolean configCorrect = false;
        if (modloader.equalsIgnoreCase("Forge") || modloader.equalsIgnoreCase("Fabric")) {
            configCorrect = true;
        } else {
            appLogger.error(LocalizationManager.getLocalizedString("configcheck.log.error.checkmodloader"));
        }
        return configCorrect;
    }

    /** Standardize the specified modloader.
     * @param modloader String. If any case of Forge or Fabric was specified, return "Forge" or "Fabric", so users can enter "forge" or "fabric" or any combination of upper- and lowercase letters..
     * @return String. Returns a standardized String of the specified modloader.
     */
    String setModloader(String modloader) {
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
    boolean checkModloaderVersion(String modloader, String modloaderVersion, String minecraftVersion) {
        boolean isVersionCorrect = false;
        if (modloader.equalsIgnoreCase("Forge") && isForgeVersionCorrect(modloaderVersion, minecraftVersion)) {
            isVersionCorrect = true;
        } else if (modloader.equalsIgnoreCase("Fabric") && isFabricVersionCorrect(modloaderVersion)) {
            isVersionCorrect = true;
        } else {
            appLogger.error(LocalizationManager.getLocalizedString("configcheck.log.error.checkmodloaderversion"));
        }
        return isVersionCorrect;
    }

    /** Check the specified Minecraft version against Mojang's version manifest to validate the version.
     * @param minecraftVersion Minecraft version to check.
     * @return Boolean. Returns true if the specified Minecraft version could be found in Mojang's manifest. False if not.
     */
    boolean isMinecraftVersionCorrect(String minecraftVersion) {
        if (!minecraftVersion.equals("")) {
            try {
                URL manifestJsonURL = new URL(Reference.MINECRAFT_MANIFEST_URL);
                ReadableByteChannel readableByteChannel = Channels.newChannel(manifestJsonURL.openStream());
                FileOutputStream downloadManifestOutputStream;

                try {
                    downloadManifestOutputStream = new FileOutputStream("mcmanifest.json");
                } catch (FileNotFoundException ex) {
                    appLogger.debug(LocalizationManager.getLocalizedString("configcheck.log.debug.isminecraftversioncorrect"), ex);
                    File file = new File("mcmanifest.json");
                    if (!file.exists()) {
                        appLogger.info(LocalizationManager.getLocalizedString("configcheck.log.info.isminecraftversioncorrect.create"));
                        boolean jsonCreated = file.createNewFile();
                        if (jsonCreated) {
                            appLogger.info(LocalizationManager.getLocalizedString("configcheck.log.info.isminecraftversioncorrect.created"));
                        } else {
                            appLogger.error(LocalizationManager.getLocalizedString("configcheck.log.error.isminecraftversioncorrect.parse"));
                        }
                    }
                    downloadManifestOutputStream = new FileOutputStream("mcmanifest.json");
                }
                FileChannel downloadManifestOutputStreamChannel = downloadManifestOutputStream.getChannel();
                downloadManifestOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
                downloadManifestOutputStream.flush();
                downloadManifestOutputStream.close();
                readableByteChannel.close();
                downloadManifestOutputStreamChannel.close();

                File manifestJsonFile = new File("mcmanifest.json");
                Scanner jsonReader = new Scanner(manifestJsonFile);
                String jsonData = jsonReader.nextLine();
                jsonReader.close();
                jsonData = jsonData.replaceAll("\\s", "");

                boolean contains = jsonData.trim().contains(String.format("\"id\":\"%s\"", minecraftVersion));
                manifestJsonFile.deleteOnExit();
                return contains;
            } catch (Exception ex) {
                appLogger.error(String.format(LocalizationManager.getLocalizedString("configcheck.log.error.isminecraftversioncorrect.validate"), minecraftVersion), ex);
                return false;
            }
        } else {
            appLogger.error(LocalizationManager.getLocalizedString("configcheck.log.error.isminecraftversioncorrect.empty"));
            return false;
        }
    }

    /** Check the specified Fabric version against Fabric's version manifest to validate the version.
     * @param fabricVersion String. The Fabric version to check.
     * @return Boolean. Returns true if the specified Fabric version could be found in Fabric's manifest. False if not.
     */
    boolean isFabricVersionCorrect(String fabricVersion) {
        try {
            URL manifestJsonURL = new URL(Reference.FABRIC_MANIFEST_URL);
            ReadableByteChannel readableByteChannel = Channels.newChannel(manifestJsonURL.openStream());
            FileOutputStream downloadManifestOutputStream;

            try {
                downloadManifestOutputStream = new FileOutputStream("fabric-manifest.xml");
            } catch (FileNotFoundException ex) {
                appLogger.debug(LocalizationManager.getLocalizedString("configcheck.log.debug.isfabricversioncorrect"), ex);
                File file = new File("fabric-manifest.xml");
                if (!file.exists()){
                    appLogger.info(LocalizationManager.getLocalizedString("configcheck.log.info.isfabricversioncorrect.create"));
                    boolean jsonCreated = file.createNewFile();
                    if (jsonCreated) {
                        appLogger.info(LocalizationManager.getLocalizedString("configcheck.log.info.isfabricversioncorrect.created"));
                    } else {
                        appLogger.error(LocalizationManager.getLocalizedString("configcheck.log.error.isfabricversioncorrect.parse"));
                    }
                }
                downloadManifestOutputStream = new FileOutputStream("fabric-manifest.xml");
            }
            FileChannel downloadManifestOutputStreamChannel = downloadManifestOutputStream.getChannel();
            downloadManifestOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
            downloadManifestOutputStream.flush();
            downloadManifestOutputStream.close();
            readableByteChannel.close();
            downloadManifestOutputStreamChannel.close();

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

            boolean contains = manifestXML.trim().contains(String.format("%s", fabricVersion));
            manifestXMLFile.deleteOnExit();
            return contains;
        } catch (Exception ex) {
            appLogger.error(LocalizationManager.getLocalizedString("configcheck.log.error.isfabricversioncorrect.validate"), ex);
            return false;
        }
    }

    /** Checks Forge version for errors (basically for its availability in Forge manifest)
     * @param forgeVersion String. The Forge version to check.
     * @param minecraftVersion String. The Minecraft version that the modpack uses. Needed to prevent usage of Forge, for example, from MC version 1.7.10, with 1.12.2.
     * @return Boolean. Returns true if Forge version correct and false if it isn't correct.
     */
    boolean isForgeVersionCorrect(String forgeVersion, String minecraftVersion) {
        try {
            URL manifestJsonURL = new URL(Reference.FORGE_MANIFEST_URL);
            ReadableByteChannel readableByteChannel = Channels.newChannel(manifestJsonURL.openStream());
            FileOutputStream downloadManifestOutputStream;

            try {
                downloadManifestOutputStream = new FileOutputStream("forge-manifest.json");
            } catch (FileNotFoundException ex) {
                appLogger.debug(LocalizationManager.getLocalizedString("configcheck.log.debug.isforgeversioncorrect"), ex);
                File file = new File("forge-manifest.json");
                if (!file.exists()){
                    appLogger.info(LocalizationManager.getLocalizedString("configcheck.log.info.isforgeversioncorrect.create"));
                    boolean jsonCreated = file.createNewFile();
                    if (jsonCreated) {
                        appLogger.info(LocalizationManager.getLocalizedString("configcheck.log.info.isforgeversioncorrect.created"));
                    } else {
                        appLogger.error(LocalizationManager.getLocalizedString("configcheck.log.error.isforgeversioncorrect.parse"));
                    }
                }
                downloadManifestOutputStream = new FileOutputStream("forge-manifest.json");
            }
            FileChannel downloadManifestOutputStreamChannel = downloadManifestOutputStream.getChannel();
            downloadManifestOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
            downloadManifestOutputStream.flush();
            downloadManifestOutputStream.close();
            readableByteChannel.close();
            downloadManifestOutputStreamChannel.close();

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
            appLogger.error(LocalizationManager.getLocalizedString("configcheck.log.error.isforgeversioncorrect.validate"), ex);
            return false;
        }
    }

    /** Returns the latest installer version for the Fabric installer to be used in ServerSetup.installServer.
     * @param modpackDir String. /server_pack The directory where the Fabric installer will be placed in.
     * @return Boolean. Returns true if the download was successful. False if not.
     */
    @SuppressWarnings({"ReturnInsideFinallyBlock", "finally"})
    private String latestFabricLoader(String modpackDir) {
        String result = "0.11.3";
        try {
            URL downloadFabricXml = new URL(Reference.FABRIC_MANIFEST_URL);
            ReadableByteChannel downloadFabricXmlReadableByteChannel = Channels.newChannel(downloadFabricXml.openStream());

            FileOutputStream downloadFabricXmlFileOutputStream = new FileOutputStream(String.format("%s/server_pack/fabric-loader.xml", modpackDir));
            FileChannel downloadFabricXmlFileChannel = downloadFabricXmlFileOutputStream.getChannel();
            downloadFabricXmlFileOutputStream.getChannel().transferFrom(downloadFabricXmlReadableByteChannel, 0, Long.MAX_VALUE);

            downloadFabricXmlFileOutputStream.flush();
            downloadFabricXmlFileOutputStream.close();
            downloadFabricXmlReadableByteChannel.close();
            downloadFabricXmlFileChannel.close();

            DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = domFactory.newDocumentBuilder();
            Document fabricXml = builder.parse(new File(String.format("%s/server_pack/fabric-loader.xml",modpackDir)));
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xpath = xPathFactory.newXPath();

            result = (String) xpath.evaluate("/metadata/versioning/release", fabricXml, XPathConstants.STRING);
            appLogger.info(LocalizationManager.getLocalizedString("configcheck.log.info.latestfabricloader.created"));
        } catch (IOException | ParserConfigurationException | SAXException | XPathExpressionException ex) {
            appLogger.error(LocalizationManager.getLocalizedString("configcheck.log.error.latestfabricloader.parse"), ex);
        } finally {
            return result;
        }
    }
}