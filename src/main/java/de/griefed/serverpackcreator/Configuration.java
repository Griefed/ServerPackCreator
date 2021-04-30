/* Copyright (C) 2021  Griefed
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 * USA
 *
 * The full license can be found at https:github.com/Griefed/ServerPackCreator/blob/main/LICENSE
 */
//TODO: Write table of contents
package de.griefed.serverpackcreator;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.therandomlabs.curseapi.CurseAPI;
import com.therandomlabs.curseapi.CurseException;
import com.typesafe.config.*;
import de.griefed.serverpackcreator.curseforgemodpack.CurseCreateModpack;
import de.griefed.serverpackcreator.curseforgemodpack.CurseModpack;
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
//TODO: Write docs for class
public class Configuration {

    private static final Logger appLogger = LogManager.getLogger(Configuration.class);
    private final File oldConfigFile = new File("creator.conf");
    private final File configFile = new File("serverpackcreator.conf");
    private LocalizationManager localizationManager;
    private CurseCreateModpack curseCreateModpack;

    public Configuration(LocalizationManager injectedLocalizationManager, CurseCreateModpack injectedCurseCreateModpack) {
        if (injectedLocalizationManager == null) {
            this.localizationManager = new LocalizationManager();
        } else {
            this.localizationManager = injectedLocalizationManager;
        }

        if (injectedCurseCreateModpack == null) {
            this.curseCreateModpack = new CurseCreateModpack(localizationManager);
        } else {
            this.curseCreateModpack = injectedCurseCreateModpack;
        }
    }

    //-- If you wish to expand this list, fork this repository, make your changes, and submit a PR -------------------------
    private final List<String> fallbackModsList = Arrays.asList(
            "AmbientSounds",
            "BackTools",
            "BetterAdvancement",
            "BetterPing",
            "cherished",
            "ClientTweaks",
            "Controlling",
            "DefaultOptions",
            "durability",
            "DynamicSurroundings",
            "itemzoom",
            "jei-professions",
            "jeiintegration",
            "JustEnoughResources",
            "MouseTweaks",
            "Neat",
            "OldJavaWarning",
            "PackMenu",
            "preciseblockplacing",
            "SimpleDiscordRichPresence",
            "SpawnerFix",
            "TipTheScales",
            "WorldNameRandomizer"
    );

    private List<String>
            clientMods,
            copyDirs;

    private String
            modpackDir,
            javaPath,
            minecraftVersion,
            modLoader,
            modLoaderVersion;

    private Boolean
            includeServerInstallation,
            includeServerIcon,
            includeServerProperties,
            includeStartScripts,
            includeZipCreation;

    private int
            projectID,
            projectFileID;

    private Config config;

    public File getOldConfigFile() {
        return oldConfigFile;
    }

    public File getConfigFile() {
        return configFile;
    }

    String getMinecraftManifestUrl() {
        return "https://launchermeta.mojang.com/mc/game/version_manifest.json";
    }

    String getForgeManifestUrl() {
        return "https://files.minecraftforge.net/net/minecraftforge/forge/maven-metadata.json";
    }

    String getFabricManifestUrl() {
        return "https://maven.fabricmc.net/net/fabricmc/fabric-loader/maven-metadata.xml";
    }

    public com.typesafe.config.Config getConfig() {
        return config;
    }
    public void setConfig(com.typesafe.config.Config newConfig) {
        this.config = newConfig;
    }

    List<String> getFallbackModsList() {
        return fallbackModsList;
    }

    List<String> getClientMods() {
        return clientMods;
    }
    void setClientMods(List<String> newClientMods) {
        this.clientMods = newClientMods;
    }

    List<String> getCopyDirs() {
        return copyDirs;
    }
    void setCopyDirs(List<String> newCopyDirs) {
        this.copyDirs = newCopyDirs;
    }

    String getModpackDir() {
        return modpackDir;
    }
    void setModpackDir(String newModpackDir) {
        newModpackDir = newModpackDir.replace("\\","/");
        this.modpackDir = newModpackDir;
    }

    String getJavaPath() {
        return javaPath;
    }
    void setJavaPath(String newJavaPath) {
        newJavaPath = newJavaPath.replace("\\", "/");
        this.javaPath = newJavaPath;
    }

    String getMinecraftVersion() {
        return minecraftVersion;
    }
    void setMinecraftVersion(String newMinecraftVersion) {
        this.minecraftVersion = newMinecraftVersion;
    }

    String getModLoader() {
        return modLoader;
    }
    void setModLoader(String newModLoader) {
        this.modLoader = newModLoader;
    }

    String getModLoaderVersion() {
        return modLoaderVersion;
    }
    void setModLoaderVersion(String newModLoaderVersion) {
        this.modLoaderVersion = newModLoaderVersion;
    }

    boolean getIncludeServerInstallation() {
        return includeServerInstallation;
    }
    void setIncludeServerInstallation(boolean newIncludeServerInstallation) {
        this.includeServerInstallation = newIncludeServerInstallation;
    }

    boolean getIncludeServerIcon() {
        return includeServerIcon;
    }
    void setIncludeServerIcon(boolean newIncludeServerIcon) {
        this.includeServerIcon = newIncludeServerIcon;
    }

    boolean getIncludeServerProperties() {
        return includeServerProperties;
    }
    void setIncludeServerProperties(boolean newIncludeServerProperties) {
        this.includeServerProperties = newIncludeServerProperties;
    }

    boolean getIncludeStartScripts() {
        return includeStartScripts;
    }
    void setIncludeStartScripts(boolean newIncludeStartScripts) {
        this.includeStartScripts = newIncludeStartScripts;
    }

    boolean getIncludeZipCreation() {
        return includeZipCreation;
    }
    void setIncludeZipCreation(boolean newIncludeZipCreation) {
        this.includeZipCreation = newIncludeZipCreation;
    }

    int getProjectID() {
        return projectID;
    }
    void setProjectID(int newProjectID) {
        this.projectID = newProjectID;
    }

    int getProjectFileID() {
        return projectFileID;
    }
    void setProjectFileID(int newProjectFileID) {
        this.projectFileID = newProjectFileID;
    }

    /** Check the config file for configuration errors. If an error is found, the log file will tell the user where the error is, so they can fix their config.
     * @param configFile The configuration file to check. Must be a valid configuration file for serverpackcreator to work.
     * @return Return true if error is found in user's configuration. If an error is found, the application will exit in main.
     */
    public boolean checkConfigFile(File configFile) {
        boolean configHasError;
        appLogger.info(localizationManager.getLocalizedString("configcheck.log.info.checkconfig.start"));
        try {
            setConfig(ConfigFactory.parseFile(configFile));
        } catch (ConfigException ex) {
            appLogger.error(localizationManager.getLocalizedString("configcheck.log.error.checkconfig.start"));
        }

        if (getConfig().getStringList("clientMods").isEmpty()) {
            appLogger.warn(localizationManager.getLocalizedString("configcheck.log.warn.checkconfig.clientmods"));
            setClientMods(getFallbackModsList());
        } else {
            setClientMods(getConfig().getStringList("clientMods"));
        }
        setIncludeServerInstallation(convertToBoolean(getConfig().getString("includeServerInstallation")));
        setIncludeServerIcon(convertToBoolean(getConfig().getString("includeServerIcon")));
        setIncludeServerProperties(convertToBoolean(getConfig().getString("includeServerProperties")));
        setIncludeStartScripts(convertToBoolean(getConfig().getString("includeStartScripts")));
        setIncludeZipCreation(convertToBoolean(getConfig().getString("includeZipCreation")));

        if (checkModpackDir(getConfig().getString("modpackDir").replace("\\","/"))) {
            configHasError = isDir(getConfig().getString("modpackDir").replace("\\","/"));
        } else if (checkCurseForge(getConfig().getString("modpackDir").replace("\\","/"))) {
            configHasError = isCurse();
        } else {
            configHasError = true;
        }

        printConfig(getModpackDir(),
                getClientMods(),
                getCopyDirs(),
                getIncludeServerInstallation(),
                getJavaPath(),
                getMinecraftVersion(),
                getModLoader(),
                getModLoaderVersion(),
                getIncludeServerIcon(),
                getIncludeServerProperties(),
                getIncludeStartScripts(),
                getIncludeZipCreation());

        if (!configHasError) {
            appLogger.info(localizationManager.getLocalizedString("configcheck.log.info.checkconfig.success"));
        } else {
            appLogger.error(localizationManager.getLocalizedString("configcheck.log.error.checkconfig.failure"));
        }
        return configHasError;
    }

    /** Checks whether the specified modpack exists. If it does, the config file is checked for errors. Should any error be found, it will return true so the configCheck method informs the user about an invalid configuration.
     * @param modpackDir String. Should an existing modpack be specified, all configurations are read from local file and the server pack is created, if config is correct.
     * @return Boolean. Returns true if an error is found during configuration check. False if the configuration is deemed valid.
     */
    private boolean isDir(String modpackDir) {
        boolean configHasError = false;
        setModpackDir(modpackDir);

        if (checkCopyDirs(getConfig().getStringList("copyDirs"), getModpackDir())) {
            setCopyDirs(getConfig().getStringList("copyDirs"));
        } else { configHasError = true; }

        if (getIncludeServerInstallation()) {
            if (checkJavaPath(getConfig().getString("javaPath"))) {
                setJavaPath(getConfig().getString("javaPath"));
            } else {
                String tmpJavaPath = getJavaPath(getConfig().getString("javaPath"));
                if (checkJavaPath(tmpJavaPath)) {
                    setJavaPath(tmpJavaPath);
                } else { configHasError = true; } }

            if (isMinecraftVersionCorrect(getConfig().getString("minecraftVersion"))) {
                setMinecraftVersion(getConfig().getString("minecraftVersion"));
            } else { configHasError = true; }

            if (checkModloader(getConfig().getString("modLoader"))) {
                setModLoader(setModloader(getConfig().getString("modLoader")));
            } else { configHasError = true; }

            if (checkModloaderVersion(getModLoader(), getConfig().getString("modLoaderVersion"))) {
                setModLoaderVersion(getConfig().getString("modLoaderVersion"));
            } else { configHasError = true; }

        } else {
            appLogger.info(localizationManager.getLocalizedString("configcheck.log.info.checkconfig.skipstart"));
            appLogger.info(localizationManager.getLocalizedString("configcheck.log.info.checkconfig.skipjava"));
            appLogger.info(localizationManager.getLocalizedString("configcheck.log.info.checkconfig.skipminecraft"));
            appLogger.info(localizationManager.getLocalizedString("configcheck.log.info.checkconfig.skipmodlaoder"));
            appLogger.info(localizationManager.getLocalizedString("configcheck.log.info.checkconfig.skipmodloaderversion"));
        }
        return configHasError;
    }

    /** Checks whether the specified projectID,fileID combination is a valid CurseForge project and file and whether the resulting directory exists. If the directory does not exist, make calls to other methods which create the modpack. Parses information gathered from the modpack to later replace the previous configuration file.
     * @return Boolean. Currently always returns true so serverpackcreator does not go straight into server pack creation after the creation of the specified modpack. Gives the user the chance to check their config before actually creating the server pack.
     */
    private boolean isCurse() {
        boolean configHasError = false;
        try {
            if (CurseAPI.project(getProjectID()).isPresent()) {
                String projectName, displayName;
                projectName = displayName = "";
                try { projectName = CurseAPI.project(getProjectID()).get().name();

                    try { displayName = Objects.requireNonNull(CurseAPI.project(getProjectID()).get().files().fileWithID(getProjectFileID())).displayName(); }
                    catch (NullPointerException npe) { appLogger.info(localizationManager.getLocalizedString("configcheck.log.info.iscurse.display"));

                        try { displayName = Objects.requireNonNull(CurseAPI.project(getProjectID()).get().files().fileWithID(getProjectFileID())).nameOnDisk(); }
                        catch (NullPointerException npe2) { displayName = String.format("%d", getProjectFileID()); } } }

                catch (CurseException cex) { appLogger.error(localizationManager.getLocalizedString("configcheck.log.error.iscurse.curseforge")); }

                setModpackDir(String.format("./%s/%s", projectName, displayName));

                if (curseCreateModpack.curseForgeModpack(getModpackDir(), getProjectID(), getProjectFileID())) {
                    try {
                        byte[] jsonData = Files.readAllBytes(Paths.get(String.format("%s/manifest.json", getModpackDir())));
                        ObjectMapper objectMapper = new ObjectMapper();
                        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
                        objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
                        CurseModpack modpack = objectMapper.readValue(jsonData, CurseModpack.class);

                        String[] minecraftLoaderVersions = modpack
                                .getMinecraft()
                                .toString()
                                .split(",");

                        String[] modLoaderVersion = minecraftLoaderVersions[1]
                                .replace("[", "")
                                .replace("]", "")
                                .split("-");
                        setMinecraftVersion(minecraftLoaderVersions[0]
                                .replace("[", ""));

                        if (containsFabric(modpack)) {
                            appLogger.info(localizationManager.getLocalizedString("configcheck.log.info.iscurse.fabric"));

                            setModLoader("Fabric");
                            setModLoaderVersion(latestFabricLoader(getModpackDir()));

                        } else {

                            setModLoader(setModloader(modLoaderVersion[0]));
                            setModLoaderVersion(modLoaderVersion[1]);

                        }
                    } catch (IOException ex) { appLogger.error(localizationManager.getLocalizedString("configcheck.log.error.iscurse.json"), ex); }

                    if (checkJavaPath(getConfig().getString("javaPath").replace("\\","/"))) {

                        setJavaPath(getConfig().getString("javaPath").replace("\\","/"));

                    } else {
                        String tmpJavaPath = getJavaPath(getConfig().getString("javaPath").replace("\\","/"));

                        if (checkJavaPath(tmpJavaPath)) {
                            setJavaPath(tmpJavaPath);
                        }

                    }
                    setCopyDirs(suggestCopyDirs(getModpackDir()));

                    appLogger.info(localizationManager.getLocalizedString("configcheck.log.info.iscurse.replace"));

                    writeConfigToFile(
                            getModpackDir(),
                            buildString(getClientMods().toString()),
                            buildString(getCopyDirs().toString()),
                            getIncludeServerInstallation(), getJavaPath(),
                            getMinecraftVersion(), getModLoader(),
                            getModLoaderVersion(), getIncludeServerIcon(),
                            getIncludeServerProperties(), getIncludeStartScripts(),
                            getIncludeZipCreation(),
                            getConfigFile(),
                            false
                    );
                }
            }
        } catch (CurseException cex) {
            appLogger.error(String.format(localizationManager.getLocalizedString("configcheck.log.error.iscurse.project"), getProjectID()), cex);
            configHasError = true;
        }
        return configHasError;
    }

    /** Checks for the Jumploader mod in the project list of the modpack. If Jumploader is found, the modloader in the configuration will be set to Fabric.
     * @param modpack Object. Contains information about our modpack. Used to get a list of all projects used in the modpack.
     * @return Boolean. Returns true if Jumploader is found, false if not found.
     */
    private boolean containsFabric(CurseModpack modpack) {
        boolean hasJumploader = false;

        for (int i = 0; i < modpack.getFiles().size(); i++) {

            String[] mods = modpack.getFiles().get(i).toString().split(",");

            if (mods[0].equalsIgnoreCase("361988") || mods[0].equalsIgnoreCase("306612")) {
                appLogger.info(localizationManager.getLocalizedString("configcheck.log.info.containsfabric"));
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
        appLogger.info(localizationManager.getLocalizedString("configcheck.log.info.suggestcopydirs.start"));

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

            appLogger.info(String.format(localizationManager.getLocalizedString("configcheck.log.info.suggestcopydirs.list"), dirList));

            return Arrays.asList(copyDirs.clone());

        } else { appLogger.error(localizationManager.getLocalizedString("configcheck.log.error.suggestcopydirs")); }

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

            setProjectID(Integer.parseInt(projectFileIds[0]));

            setProjectFileID(Integer.parseInt(projectFileIds[1]));

            appLogger.info(localizationManager.getLocalizedString("configcheck.log.info.checkcurseforge.info"));
            appLogger.info(String.format(localizationManager.getLocalizedString("configcheck.log.info.checkcurseforge.return"), getProjectID(), getProjectFileID()));
            appLogger.warn(localizationManager.getLocalizedString("configcheck.log.warn.checkcurseforge.warn"));

            configCorrect = true;

        } else { appLogger.warn(localizationManager.getLocalizedString("configcheck.log.warn.checkcurseforge.warn2")); }

        return configCorrect;
    }

    /** Converts various strings to booleans.
     * @param stringBoolean String. The string which should be converted to boolean if it matches certain patterns.
     * @return Boolean. Returns the corresponding boolean if match with pattern was found. If no match is found, assume and return false.
     */
    public boolean convertToBoolean(String stringBoolean) {
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
            appLogger.warn(localizationManager.getLocalizedString("configcheck.log.warn.converttoboolean.warn"));
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

        appLogger.info(localizationManager.getLocalizedString("configcheck.log.info.printconfig.start"));
        appLogger.info(String.format(localizationManager.getLocalizedString("configcheck.log.info.printconfig.modpackdir"), modpackDirectory));

        if (clientsideMods.size() == 0) {
            appLogger.warn(localizationManager.getLocalizedString("configcheck.log.warn.printconfig.noclientmods"));
        } else {

            appLogger.info(localizationManager.getLocalizedString("configcheck.log.info.printconfig.clientmods"));
            for (int i = 0; i < clientsideMods.size(); i++) {
                appLogger.info(String.format("    %s", clientsideMods.get(i))); }

        }

        appLogger.info(localizationManager.getLocalizedString("configcheck.log.info.printconfig.copydirs"));

        if (copyDirectories != null) {

            for (int i = 0; i < copyDirectories.size(); i++) {
                appLogger.info(String.format("    %s", copyDirectories.get(i))); }

        } else {
            appLogger.error(localizationManager.getLocalizedString("configcheck.log.error.printconfig.copydirs"));
        }

        appLogger.info(String.format(localizationManager.getLocalizedString("configcheck.log.info.printconfig.server"), installServer));
        appLogger.info(String.format(localizationManager.getLocalizedString("configcheck.log.info.printconfig.javapath"), javaInstallPath));
        appLogger.info(String.format(localizationManager.getLocalizedString("configcheck.log.info.printconfig.minecraftversion"), minecraftVer));
        appLogger.info(String.format(localizationManager.getLocalizedString("configcheck.log.info.printconfig.modloader"), modloader));
        appLogger.info(String.format(localizationManager.getLocalizedString("configcheck.log.info.printconfig.modloaderversion"), modloaderVersion));
        appLogger.info(String.format(localizationManager.getLocalizedString("configcheck.log.info.printconfig.icon"), includeIcon));
        appLogger.info(String.format(localizationManager.getLocalizedString("configcheck.log.info.printconfig.properties"), includeProperties));
        appLogger.info(String.format(localizationManager.getLocalizedString("configcheck.log.info.printconfig.scripts"), includeScripts));
        appLogger.info(String.format(localizationManager.getLocalizedString("configcheck.log.info.printconfig.zip"), includeZip));
    }

    /** Check whether the specified modpack directory exists.
     * @param modpackDir String. The path to the modpack directory.
     * @return Boolean. Returns true if the directory exists. False if not.
     */
    boolean checkModpackDir(String modpackDir) {
        boolean configCorrect = false;

        if (modpackDir.equals("")) {

            appLogger.error(localizationManager.getLocalizedString("configcheck.log.error.checkmodpackdir"));

        } else if (!(new File(modpackDir).isDirectory())) {

            appLogger.warn(String.format(localizationManager.getLocalizedString("configcheck.log.warn.checkmodpackdir"), modpackDir));

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

            appLogger.error(localizationManager.getLocalizedString("configcheck.log.error.checkcopydirs.empty"));
            configCorrect = false;

        } else {

            for (int i = 0; i < copyDirs.size(); i++) {

                File directory = new File(String.format("%s/%s", modpackDir, copyDirs.get(i)));

                if (!directory.exists() || !directory.isDirectory()) {

                    appLogger.error(String.format(localizationManager.getLocalizedString("configcheck.log.error.checkcopydirs.notfound"), directory.getAbsolutePath()));
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

            appLogger.warn(localizationManager.getLocalizedString("configcheck.log.warn.getjavapath.empty"));
            autoJavaPath = String.format("%s/bin/java",System.getProperty("java.home").replace("\\", "/"));

            if (autoJavaPath.startsWith("C:")) {

                autoJavaPath = String.format("%s.exe", autoJavaPath);
            }

            appLogger.warn(String.format(localizationManager.getLocalizedString("configcheck.log.warn.getjavapath.set"), autoJavaPath));
            return autoJavaPath;

        } else {

            appLogger.info(String.format(localizationManager.getLocalizedString("configcheck.log.warn.getjavapath.set"), enteredPath));
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
            appLogger.error(localizationManager.getLocalizedString("configcheck.log.error.checkjavapath"));
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

            appLogger.error(localizationManager.getLocalizedString("configcheck.log.error.checkmodloader"));
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
     * @return Boolean. Returns true if the specified modloader version is correct. False if not.
     */
    boolean checkModloaderVersion(String modloader, String modloaderVersion) {
        boolean isVersionCorrect = false;

        if (modloader.equalsIgnoreCase("Forge") && isForgeVersionCorrect(modloaderVersion)) {

            isVersionCorrect = true;

        } else if (modloader.equalsIgnoreCase("Fabric") && isFabricVersionCorrect(modloaderVersion)) {

            isVersionCorrect = true;

        } else {
            appLogger.error(localizationManager.getLocalizedString("configcheck.log.error.checkmodloaderversion"));
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
                URL manifestJsonURL = new URL(getMinecraftManifestUrl());
                ReadableByteChannel readableByteChannel = Channels.newChannel(manifestJsonURL.openStream());
                FileOutputStream downloadManifestOutputStream;

                try {
                    downloadManifestOutputStream = new FileOutputStream("mcmanifest.json");
                } catch (FileNotFoundException ex) {

                    appLogger.debug(localizationManager.getLocalizedString("configcheck.log.debug.isminecraftversioncorrect"), ex);

                    File file = new File("mcmanifest.json");

                    if (!file.exists()) {

                        appLogger.info(localizationManager.getLocalizedString("configcheck.log.info.isminecraftversioncorrect.create"));
                        boolean jsonCreated = file.createNewFile();

                        if (jsonCreated) {

                            appLogger.info(localizationManager.getLocalizedString("configcheck.log.info.isminecraftversioncorrect.created"));

                        } else {

                            appLogger.error(localizationManager.getLocalizedString("configcheck.log.error.isminecraftversioncorrect.parse"));
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

                appLogger.error(String.format(localizationManager.getLocalizedString("configcheck.log.error.isminecraftversioncorrect.validate"), minecraftVersion), ex);
                return false;
            }
        } else {
            appLogger.error(localizationManager.getLocalizedString("configcheck.log.error.isminecraftversioncorrect.empty"));
            return false;
        }
    }

    /** Check the specified Fabric version against Fabric's version manifest to validate the version.
     * @param fabricVersion String. The Fabric version to check.
     * @return Boolean. Returns true if the specified Fabric version could be found in Fabric's manifest. False if not.
     */
    boolean isFabricVersionCorrect(String fabricVersion) {
        try {
            URL manifestJsonURL = new URL(getFabricManifestUrl());

            ReadableByteChannel readableByteChannel = Channels.newChannel(manifestJsonURL.openStream());

            FileOutputStream downloadManifestOutputStream;

            try {
                downloadManifestOutputStream = new FileOutputStream("fabric-manifest.xml");
            } catch (FileNotFoundException ex) {

                appLogger.debug(localizationManager.getLocalizedString("configcheck.log.debug.isfabricversioncorrect"), ex);
                File file = new File("fabric-manifest.xml");

                if (!file.exists()){

                    appLogger.info(localizationManager.getLocalizedString("configcheck.log.info.isfabricversioncorrect.create"));
                    boolean jsonCreated = file.createNewFile();

                    if (jsonCreated) {

                        appLogger.info(localizationManager.getLocalizedString("configcheck.log.info.isfabricversioncorrect.created"));

                    } else {
                        appLogger.error(localizationManager.getLocalizedString("configcheck.log.error.isfabricversioncorrect.parse"));
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

            appLogger.error(localizationManager.getLocalizedString("configcheck.log.error.isfabricversioncorrect.validate"), ex);
            return false;
        }
    }

    /** Checks Forge version for errors (basically for its availability in Forge manifest)
     * @param forgeVersion String. The Forge version to check.
     * @return Boolean. Returns true if Forge version correct and false if it isn't correct.
     */
    boolean isForgeVersionCorrect(String forgeVersion) {
        try {
            URL manifestJsonURL = new URL(getForgeManifestUrl());
            ReadableByteChannel readableByteChannel = Channels.newChannel(manifestJsonURL.openStream());
            FileOutputStream downloadManifestOutputStream;

            try {

                downloadManifestOutputStream = new FileOutputStream("forge-manifest.json");

            } catch (FileNotFoundException ex) {

                appLogger.debug(localizationManager.getLocalizedString("configcheck.log.debug.isforgeversioncorrect"), ex);
                File file = new File("forge-manifest.json");

                if (!file.exists()){

                    appLogger.info(localizationManager.getLocalizedString("configcheck.log.info.isforgeversioncorrect.create"));

                    boolean jsonCreated = file.createNewFile();

                    if (jsonCreated) {

                        appLogger.info(localizationManager.getLocalizedString("configcheck.log.info.isforgeversioncorrect.created"));
                    } else {

                        appLogger.error(localizationManager.getLocalizedString("configcheck.log.error.isforgeversioncorrect.parse"));
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

            return manifestJSON.trim().contains(String.format("%s", forgeVersion));

        } catch (Exception ex) {

            appLogger.error(localizationManager.getLocalizedString("configcheck.log.error.isforgeversioncorrect.validate"), ex);
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
            URL downloadFabricXml = new URL(getFabricManifestUrl());
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
            appLogger.info(localizationManager.getLocalizedString("configcheck.log.info.latestfabricloader.created"));
        } catch (IOException | ParserConfigurationException | SAXException | XPathExpressionException ex) {
            appLogger.error(localizationManager.getLocalizedString("configcheck.log.error.latestfabricloader.parse"), ex);
        } finally {
            return result;
        }
    }

    /**
     * Generate new configuration file from CLI input. Prompts user to enter config file values and then generates a config file with values entered by user.
     */
    void createConfigurationFile() {
        List<String> clientMods, copyDirs;

        clientMods = new ArrayList<>(0);
        copyDirs = new ArrayList<>(0);

        String[] tmpClientMods, tmpCopyDirs;

        boolean includeServerInstallation,
                includeServerIcon,
                includeServerProperties,
                includeStartScripts,
                includeZipCreation;

        String modpackDir,
                javaPath,
                minecraftVersion,
                modLoader,
                modLoaderVersion,
                tmpModpackDir;

        Scanner reader = new Scanner(System.in);

        appLogger.info(String.format(localizationManager.getLocalizedString("clisetup.log.info.start"), "-cgen"));
        do {
//--------------------------------------------------------------------------------------------MODPACK DIRECTORY---------
            appLogger.info(localizationManager.getLocalizedString("clisetup.log.info.modpack.enter"));
            appLogger.info(localizationManager.getLocalizedString("clisetup.log.info.modpack.example"));

            do {
                System.out.print(localizationManager.getLocalizedString("clisetup.log.info.modpack.cli"));
                tmpModpackDir = reader.nextLine();
            } while (!checkModpackDir(tmpModpackDir));

            modpackDir = tmpModpackDir.replace("\\", "/");
            appLogger.info(String.format(localizationManager.getLocalizedString("clisetup.log.info.checkreturn"), modpackDir));
            System.out.println();

//-----------------------------------------------------------------------------------------CLIENTSIDE-ONLY MODS---------
            appLogger.info(localizationManager.getLocalizedString("clisetup.log.info.clientmods.enter"));
            do {
                clientMods.addAll(readStringArray());
                appLogger.info(String.format(localizationManager.getLocalizedString("clisetup.log.info.checkreturn"), clientMods));
                appLogger.info(localizationManager.getLocalizedString("clisetup.log.info.clientmods.checkreturninfo"));
                System.out.print(localizationManager.getLocalizedString("clisetup.log.info.answer"));
            } while (!readBoolean());
            appLogger.info(String.format(localizationManager.getLocalizedString("clisetup.log.info.checkreturn"), clientMods));
            tmpClientMods = new String[clientMods.size()];
            clientMods.toArray(tmpClientMods);
            System.out.println();

//---------------------------------------------------------------------------DIRECTORIES TO COPY TO SERVER PACK---------
            appLogger.info(localizationManager.getLocalizedString("clisetup.log.info.copydirs.enter"));
            do {
                do {
                    copyDirs.clear();
                    appLogger.info(localizationManager.getLocalizedString("clisetup.log.info.copydirs.specify"));
                    copyDirs.addAll(readStringArray());

                } while (!checkCopyDirs(copyDirs, modpackDir));

                appLogger.info(String.format(localizationManager.getLocalizedString("clisetup.log.info.checkreturn"), copyDirs));
                appLogger.info(localizationManager.getLocalizedString("clisetup.log.info.copydirs.checkreturninfo"));
                System.out.print(localizationManager.getLocalizedString("clisetup.log.info.answer"));
            } while (!readBoolean());
            appLogger.info(String.format(localizationManager.getLocalizedString("clisetup.log.info.checkreturn"), copyDirs));
            tmpCopyDirs = new String[copyDirs.size()];
            copyDirs.toArray(tmpCopyDirs);
            System.out.println();

//-------------------------------------------------------------WHETHER TO INCLUDE MODLOADER SERVER INSTALLATION---------
            appLogger.info(localizationManager.getLocalizedString("clisetup.log.info.server.enter"));
            System.out.print(localizationManager.getLocalizedString("clisetup.log.info.server.include"));
            includeServerInstallation = readBoolean();
            appLogger.info(String.format(localizationManager.getLocalizedString("clisetup.log.info.checkreturn"), includeServerInstallation));

//-------------------------------------------------------------------------------MINECRAFT VERSION MODPACK USES---------
            appLogger.info(localizationManager.getLocalizedString("clisetup.log.info.minecraft.enter"));
            do {
                System.out.print(localizationManager.getLocalizedString("clisetup.log.info.minecraft.specify"));
                minecraftVersion = reader.nextLine();
            } while (!isMinecraftVersionCorrect(minecraftVersion));
            appLogger.info(String.format(localizationManager.getLocalizedString("clisetup.log.info.checkreturn"), minecraftVersion));
            System.out.println();

//---------------------------------------------------------------------------------------MODLOADER MODPACK USES---------
            appLogger.info(localizationManager.getLocalizedString("clisetup.log.info.modloader.enter"));
            do {
                System.out.print(localizationManager.getLocalizedString("clisetup.log.info.modloader.cli"));
                modLoader = reader.nextLine();
            } while (!checkModloader(modLoader));
            modLoader = setModloader(modLoader);
            appLogger.info(String.format(localizationManager.getLocalizedString("clisetup.log.info.checkreturn"), modLoader));
            System.out.println();

//----------------------------------------------------------------------------VERSION OF MODLOADER MODPACK USES---------
            appLogger.info(String.format(localizationManager.getLocalizedString("clisetup.log.info.modloaderversion.enter"), modLoader));
            do {
                System.out.print(localizationManager.getLocalizedString("clisetup.log.info.modloaderversion.cli"));
                modLoaderVersion = reader.nextLine();
            } while (!checkModloaderVersion(modLoader, modLoaderVersion));
            appLogger.info(String.format(localizationManager.getLocalizedString("clisetup.log.info.checkreturn"), modLoaderVersion));
            System.out.println();

//------------------------------------------------------------------------------------PATH TO JAVA INSTALLATION---------
            appLogger.info(localizationManager.getLocalizedString("clisetup.log.info.java.enter"));
            appLogger.info(localizationManager.getLocalizedString("clisetup.log.info.java.enter2"));
            appLogger.info(localizationManager.getLocalizedString("clisetup.log.info.java.example"));
            do {
                System.out.print(localizationManager.getLocalizedString("clisetup.log.info.java.cli"));
                String tmpJavaPath = reader.nextLine();
                javaPath = getJavaPath(tmpJavaPath);
            } while (!checkJavaPath(javaPath));
            System.out.println();

//------------------------------------------------------------WHETHER TO INCLUDE SERVER-ICON.PNG IN SERVER PACK---------
            appLogger.info(localizationManager.getLocalizedString("clisetup.log.info.icon.enter"));
            System.out.print(localizationManager.getLocalizedString("clisetup.log.info.icon.cli"));
            includeServerIcon = readBoolean();
            appLogger.info(String.format(localizationManager.getLocalizedString("clisetup.log.info.checkreturn"), includeServerIcon));
            System.out.println();

//----------------------------------------------------------WHETHER TO INCLUDE SERVER.PROPERTIES IN SERVER PACK---------
            appLogger.info(localizationManager.getLocalizedString("clisetup.log.info.properties.enter"));
            System.out.print(localizationManager.getLocalizedString("clisetup.log.info.properties.cli"));
            includeServerProperties = readBoolean();
            appLogger.info(String.format(localizationManager.getLocalizedString("clisetup.log.info.checkreturn"), includeServerProperties));
            System.out.println();

//--------------------------------------------------------------WHETHER TO INCLUDE START SCRIPTS IN SERVER PACK---------
            appLogger.info(localizationManager.getLocalizedString("clisetup.log.info.scripts.enter"));
            System.out.print(localizationManager.getLocalizedString("clisetup.log.info.scripts.cli"));
            includeStartScripts = readBoolean();
            appLogger.info(String.format(localizationManager.getLocalizedString("clisetup.log.info.checkreturn"), includeStartScripts));
            System.out.println();

//----------------------------------------------------WHETHER TO INCLUDE CREATION OF ZIP-ARCHIVE OF SERVER PACK---------
            appLogger.info(localizationManager.getLocalizedString("clisetup.log.info.zip.enter"));
            System.out.print(localizationManager.getLocalizedString("clisetup.log.info.zip.cli"));
            includeZipCreation = readBoolean();
            appLogger.info(String.format(localizationManager.getLocalizedString("clisetup.log.info.checkreturn"), includeZipCreation));

//------------------------------------------------------------------------------PRINT CONFIG TO CONSOLE AND LOG---------
            printConfig(modpackDir,
                    clientMods,
                    copyDirs,
                    includeServerInstallation,
                    javaPath,
                    minecraftVersion,
                    modLoader,
                    modLoaderVersion,
                    includeServerIcon,
                    includeServerProperties,
                    includeStartScripts,
                    includeZipCreation);
            appLogger.info(localizationManager.getLocalizedString("clisetup.log.info.config.enter"));
            System.out.print(localizationManager.getLocalizedString("clisetup.log.info.answer"));
        } while (!readBoolean());
        reader.close();

//-----------------------------------------------------------------------------------------WRITE CONFIG TO FILE---------
        if (writeConfigToFile(
                modpackDir,
                buildString(Arrays.toString(tmpClientMods)),
                buildString(Arrays.toString(tmpCopyDirs)),
                includeServerInstallation,
                javaPath,
                minecraftVersion,
                modLoader,
                modLoaderVersion,
                includeServerIcon,
                includeServerProperties,
                includeStartScripts,
                includeZipCreation,
                getConfigFile(),
                false
        )) {
            appLogger.info(localizationManager.getLocalizedString("clisetup.log.info.config.written"));
        }
    }

    /** A helper method for config setup. Prompts user to enter the values that will be stored in arrays in config.
     * @return String List. Returns list with user input values that will be stored in config.
     */
    private List<String> readStringArray() {
        Scanner readerArray = new Scanner(System.in);
        ArrayList<String> result = new ArrayList<>(1);
        String stringArray;
        while (true) {
            stringArray = readerArray.nextLine();
            if (stringArray.isEmpty()) {
                return result;
            } else {
                result.add(stringArray);
            }
        }
    }

    /** Converts list of strings into concatenated string.
     * @param args Strings that will be concatenated into one string
     * @return String. Returns concatenated string that contains all provided values.
     */
    public String buildString(String... args) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(Arrays.toString(args));
        stringBuilder.delete(0, 2).reverse().delete(0,2).reverse();
        return stringBuilder.toString();
    }

    /** A helper method for config setup. Prompts user to enter boolean values that will be stored in config and checks entered values to prevent storing non-boolean values in boolean variables.
     * @return Boolean. Converts to boolean and returns value entered by user that will be stored in config.
     */
    private boolean readBoolean() {
        Scanner readerBoolean = new Scanner(System.in);
        String boolRead;
        while (true) {
            boolRead = readerBoolean.nextLine();
            if (boolRead.matches("[Tt]rue") ||
                    boolRead.matches("[Yy]es")  ||
                    boolRead.matches("[Yy]")    ||
                    boolRead.matches("1")                                                           ||
                    boolRead.matches(localizationManager.getLocalizedString("cli.input.true")) ||
                    boolRead.matches(localizationManager.getLocalizedString("cli.input.yes"))  ||
                    boolRead.matches(localizationManager.getLocalizedString("cli.input.yes.short"))) {
                return true;

            } else if (boolRead.matches("[Ff]alse") ||
                    boolRead.matches("[Nn]o")    ||
                    boolRead.matches("[Nn]" )    ||
                    boolRead.matches("0")                                                            ||
                    boolRead.matches(localizationManager.getLocalizedString("cli.input.false")) ||
                    boolRead.matches(localizationManager.getLocalizedString("cli.input.no"))    ||
                    boolRead.matches(localizationManager.getLocalizedString("cli.input.no.short"))) {
                return false;

            } else {
                appLogger.error(localizationManager.getLocalizedString("clisetup.log.error.answer"));
            }
        }
    }

    /** Writes a new configuration file with the parameters passed to it.
     * @param modpackDir String. The path to the modpack.
     * @param clientMods List, String. List of clientside-only mods.
     * @param copyDirs List, String. List of directories to include in server pack.
     * @param includeServer Boolean. Whether to include a modloader server installation.
     * @param javaPath String. Path to the java executable.
     * @param minecraftVersion String. Minecraft version used by the modpack and server pack.
     * @param modLoader String. Modloader used by the modpack and server pack. Ether Forge or Fabric.
     * @param modLoaderVersion String. Modloader version used by the modpack and server pack.
     * @param includeIcon Boolean. Whether to include a server-icon in the server pack.
     * @param includeProperties Boolean. Whether to include a properties file in the server pack.
     * @param includeScripts Boolean. Whether to include start scripts in the server pack.
     * @param includeZip Boolean. Whether to create a ZIP-archive of the server pack, excluding Mojang's Minecraft server jar.
     * @param fileName The name under which to write the new file.
     * @param isTemporary Decides whether to delete existing config-file. If isTemporary is false, existing config files will be deleted before writing the new file.
     * @return Boolean. Returns true if the configuration file has been successfully written and old ones replaced.
     */
    public boolean writeConfigToFile(String modpackDir,
                                     String clientMods,
                                     String copyDirs,
                                     boolean includeServer,
                                     String javaPath,
                                     String minecraftVersion,
                                     String modLoader,
                                     String modLoaderVersion,
                                     boolean includeIcon,
                                     boolean includeProperties,
                                     boolean includeScripts,
                                     boolean includeZip,
                                     File fileName,
                                     boolean isTemporary) {

        boolean configWritten = false;

        //Griefed: What the fuck. This reads like someone having a stroke. What have I created here?
        String configString = String.format(
                        "%s\nmodpackDir = \"%s\"\n\n" +
                        "%s\nclientMods = [%s]\n\n" +
                        "%s\ncopyDirs =[%s]\n\n" +
                        "%s\nincludeServerInstallation = %b\n\n" +
                        "%s\njavaPath = \"%s\"\n\n" +
                        "%s\nminecraftVersion = \"%s\"\n\n" +
                        "%s\nmodLoader = \"%s\"\n\n" +
                        "%s\nmodLoaderVersion = \"%s\"\n\n" +
                        "%s\nincludeServerIcon = %b\n\n" +
                        "%s\nincludeServerProperties = %b\n\n" +
                        "%s\nincludeStartScripts = %b\n\n" +
                        "%s\nincludeZipCreation = %b\n",
                localizationManager.getLocalizedString("filessetup.writeconfigtofile.modpackdir"), modpackDir.replace("\\","/"),
                localizationManager.getLocalizedString("filessetup.writeconfigtofile.clientmods"), clientMods,
                localizationManager.getLocalizedString("filessetup.writeconfigtofile.copydirs"), copyDirs,
                localizationManager.getLocalizedString("filessetup.writeconfigtofile.includeserverinstallation"), includeServer,
                localizationManager.getLocalizedString("filessetup.writeconfigtofile.javapath"), javaPath.replace("\\","/"),
                localizationManager.getLocalizedString("filessetup.writeconfigtofile.minecraftversion"), minecraftVersion,
                localizationManager.getLocalizedString("filessetup.writeconfigtofile.modloader"), modLoader,
                localizationManager.getLocalizedString("filessetup.writeconfigtofile.modloaderversion"), modLoaderVersion,
                localizationManager.getLocalizedString("filessetup.writeconfigtofile.includeservericon"), includeIcon,
                localizationManager.getLocalizedString("filessetup.writeconfigtofile.includeserverproperties"), includeProperties,
                localizationManager.getLocalizedString("filessetup.writeconfigtofile.includestartscripts"), includeScripts,
                localizationManager.getLocalizedString("filessetup.writeconfigtofile.includezipcreation"), includeZip
        );

        if (!isTemporary) {
            if (getConfigFile().exists()) {
                boolean delConf = getConfigFile().delete();
                if (delConf) {
                    appLogger.info(localizationManager.getLocalizedString("filessetup.log.info.writeconfigtofile.config"));
                } else {
                    appLogger.error(localizationManager.getLocalizedString("filessetup.log.error.writeconfigtofile.config"));
                }
            }
            if (getOldConfigFile().exists()) {
                boolean delOldConf = getOldConfigFile().delete();
                if (delOldConf) {
                    appLogger.info(localizationManager.getLocalizedString("filessetup.log.info.writeconfigtofile.old"));
                } else {
                    appLogger.error(localizationManager.getLocalizedString("filessetup.log.error.writeconfigtofile.old"));
                }
            }
        }

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
            writer.write(configString);
            writer.close();
            configWritten = true;
        } catch (IOException ex) {
            appLogger.error(localizationManager.getLocalizedString("filessetup.log.error.writeconfigtofile"), ex);
        }

        return configWritten;
    }
}