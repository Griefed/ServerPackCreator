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

/**
 * <strong>Table of methods</strong><p>
 * 1. {@link #Configuration(LocalizationManager, CurseCreateModpack)}<br>
 * 2. {@link #getOldConfigFile()}<br>
 * 3. {@link #getConfigFile()}<br>
 * 4. {@link #getMinecraftManifestUrl()}<br>
 * 5. {@link #getForgeManifestUrl()}<br>
 * 6. {@link #getFabricManifestUrl()}<br>
 * 7. {@link #getConfig()}<br>
 * 8. {@link #setConfig(File)}<br>
 * 9. {@link #getFallbackModsList()}<br>
 * 10.{@link #getClientMods()}<br>
 * 11.{@link #setClientMods(List)}<br>
 * 12.{@link #getCopyDirs()}<br>
 * 13.{@link #setCopyDirs(List)}<br>
 * 14.{@link #getModpackDir()}<br>
 * 15.{@link #setModpackDir(String)}<br>
 * 16.{@link #getJavaPath()}<br>
 * 17.{@link #setJavaPath(String)}<br>
 * 18.{@link #getMinecraftVersion()}<br>
 * 19.{@link #setMinecraftVersion(String)}<br>
 * 20.{@link #getModLoader()}<br>
 * 21.{@link #setModLoader(String)}<br>
 * 22.{@link #getModLoaderVersion()}<br>
 * 23.{@link #setModLoaderVersion(String)}<br>
 * 24.{@link #getIncludeServerInstallation()}<br>
 * 25.{@link #setIncludeServerInstallation(boolean)}<br>
 * 26.{@link #getIncludeServerIcon()}<br>
 * 27.{@link #setIncludeServerIcon(boolean)}<br>
 * 28.{@link #getIncludeServerProperties()}<br>
 * 29.{@link #setIncludeServerProperties(boolean)}<br>
 * 30.{@link #getIncludeStartScripts()}<br>
 * 31.{@link #setIncludeStartScripts(boolean)}<br>
 * 32.{@link #getIncludeZipCreation()}<br>
 * 33.{@link #setIncludeZipCreation(boolean)}<br>
 * 34.{@link #getProjectID()}<br>
 * 35.{@link #setProjectID(int)}<br>
 * 36.{@link #getProjectFileID()}<br>
 * 37.{@link #setProjectFileID(int)}<br>
 * 38.{@link #checkConfigFile(File, boolean)}<br>
 * 39.{@link #isDir(String)}<br>
 * 40.{@link #isCurse()}<br>
 * 41.{@link #containsFabric(CurseModpack)}<br>
 * 42.{@link #suggestCopyDirs(String)}<br>
 * 43.{@link #checkCurseForge(String)}<br>
 * 44.{@link #convertToBoolean(String)}<br>
 * 45.{@link #printConfig(String, List, List, boolean, String, String, String, String, boolean, boolean, boolean, boolean)}<br>
 * 46.{@link #checkModpackDir(String)}<br>
 * 47.{@link #checkCopyDirs(List, String)}<br>
 * 48.{@link #getJavaPathFromSystem(String)}<br>
 * 49.{@link #checkJavaPath(String)}<br>
 * 50.{@link #checkModloader(String)}<br>
 * 51.{@link #setModLoaderCase(String)}<br>
 * 52.{@link #checkModloaderVersion(String, String)}<br>
 * 53.{@link #isMinecraftVersionCorrect(String)}<br>
 * 54.{@link #isFabricVersionCorrect(String)}<br>
 * 55.{@link #isForgeVersionCorrect(String)}<br>
 * 56.{@link #latestFabricLoader(String)}<br>
 * 57.{@link #createConfigurationFile()}<br>
 * 58.{@link #readStringArray()}<br>
 * 59.{@link #buildString(String...)}<br>
 * 60.{@link #readBoolean()}<br>
 * 61.{@link #writeConfigToFile(String, String, String, boolean, String, String, String, String, boolean, boolean, boolean, boolean, File, boolean)}
 * <p>
 * Requires an instance of {@link CurseCreateModpack} in order to create a modpack from scratch should {@link #modpackDir}
 * be a combination of a CurseForge projectID and fileID.<p>
 * Requires an instance of {@link LocalizationManager} for use of localization, but creates one if injected one is null.<p>
 * Loads a configuration from a serverpackcreator.conf-file in the same directory in which ServerPackCreator resides in.
 */
public class Configuration {
    private static final Logger LOG = LogManager.getLogger(Configuration.class);

    private final LocalizationManager LOCALIZATIONMANAGER;
    private final CurseCreateModpack CURSECREATEMODPACK;

    /**
     * <strong>Constructor</strong><p>
     * Used for Dependency Injection.<p>
     * Receives an instance of {@link LocalizationManager} or creates one if the received
     * one is null. Required for use of localization.<p>
     * Receives an instance of {@link CurseCreateModpack} in case the modpack has to be created from a combination of
     * CurseForge projectID and fileID, from which to <em>then</em> create the server pack.
     * @param injectedLocalizationManager Instance of {@link LocalizationManager} required for localized log messages.
     * @param injectedCurseCreateModpack Instance of {@link CurseCreateModpack} in case the modpack has to be created from a combination of
     * CurseForge projectID and fileID, from which to <em>then</em> create the server pack.
     */
    public Configuration(LocalizationManager injectedLocalizationManager, CurseCreateModpack injectedCurseCreateModpack) {
        if (injectedLocalizationManager == null) {
            this.LOCALIZATIONMANAGER = new LocalizationManager();
        } else {
            this.LOCALIZATIONMANAGER = injectedLocalizationManager;
        }

        if (injectedCurseCreateModpack == null) {
            this.CURSECREATEMODPACK = new CurseCreateModpack(LOCALIZATIONMANAGER);
        } else {
            this.CURSECREATEMODPACK = injectedCurseCreateModpack;
        }
    }

    private final File FILE_CONFIG_OLD = new File("creator.conf");
    private final File FILE_CONFIG = new File("serverpackcreator.conf");

    // If you wish to expand this list, fork this repository, make your changes, and submit a PR.
    private final List<String> FALLBACKMODSLIST = Arrays.asList(
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

    public ObjectMapper getObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        return objectMapper;
    }

    /**
     * Getter for creator.conf.
     * @return File. Returns the creator.conf-file for use in {@link #writeConfigToFile(String, String, String, boolean, String, String, String, String, boolean, boolean, boolean, boolean, File, boolean)}
     */
    public File getOldConfigFile() {
        return FILE_CONFIG_OLD;
    }

    /**
     * Getter for serverpackcreator.conf.
     * @return File. Returns the serverpackcreator.conf-file for use in <br>
     * {@link #isCurse()},<br>
     * {@link #createConfigurationFile()},<br>
     * {@link #writeConfigToFile(String, String, String, boolean, String, String, String, String, boolean, boolean, boolean, boolean, File, boolean)}
     */
    public File getConfigFile() {
        return FILE_CONFIG;
    }

    /**
     * Getter for Mojang's Minecraft version-manifest.
     * @return String. Returns the URL to the JSON-file for use in {@link #isMinecraftVersionCorrect(String)}
     */
    URL getMinecraftManifestUrl() {
        URL minecraftURL = null;
        String minecraftManifest = "https://launchermeta.mojang.com/mc/game/version_manifest.json";
        try {
            minecraftURL = new URL(minecraftManifest); }
        catch (IOException ex) { LOG.error(ex); }
        return minecraftURL;
    }

    /**
     * Getter for Forge's version-manifest.
     * @return String. Returns the URL to the JSON-file for use in {@link #isForgeVersionCorrect(String)}
     */
    URL getForgeManifestUrl() {
        URL forgeURL = null;
        String forgeManifest = "https://files.minecraftforge.net/net/minecraftforge/forge/maven-metadata.json";
        try {
            forgeURL = new URL(forgeManifest); }
        catch (IOException ex) { LOG.error(ex); }
        return forgeURL;
    }

    /**
     * Getter for Fabric's version-manifest.
     * @return String. Returns the URL to the JSON-file for use in {@link #isFabricVersionCorrect(String)}
     */
    URL getFabricManifestUrl() {
        URL fabricURL = null;
        String fabricManifest = "https://maven.fabricmc.net/net/fabricmc/fabric-loader/maven-metadata.xml";
        try {
            fabricURL = new URL(fabricManifest); }
        catch (IOException ex) { LOG.error(ex); }
        return fabricURL;
    }

    /**
     * Getter for a {@link Config} containing a parsed configuration-file.
     * @return Config. Returns parsed serverpackcreator.conf for use in<br>
     * {@link #checkConfigFile(File, boolean)}<br>
     * {@link #isDir(String)}<br>
     * {@link #isCurse()}
     */
     Config getConfig() {
        return config;
    }

    /**
     * Setter for a {@link Config} containing a parsed configuration-file.
     * For use in {@link #checkConfigFile(File, boolean)}
     * @param newConfig The new file of which to store a Typesafe Config.
     */
    void setConfig(File newConfig) {
        try {
            this.config = ConfigFactory.parseFile(newConfig);
        } catch (ConfigException ex) {
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.error.checkconfig.start"));
        }
    }

    /**
     * Getter for the fallback clientside-only mods-list, in case no customized one is provided by the user.
     * @return List String. Returns the fallback clientside-only mods-list for use in {@link #checkConfigFile(File, boolean)}
     */
    List<String> getFallbackModsList() {
        return FALLBACKMODSLIST;
    }

    /**
     * Getter for a list of clientside-only mods to exclude from server pack.
     * @return List String. Returns the list of clientside-only mods for use in<br>
     * {@link #checkConfigFile(File, boolean)}<br>
     * {@link #isCurse()}
     */
    List<String> getClientMods() {
        return clientMods;
    }

    /**
     * Setter for the list of clientside-only mods to exclude from server pack.
     * For use in {@link #checkConfigFile(File, boolean)}
     * @param newClientMods The new list of clientside-only mods to store.
     */
    void setClientMods(List<String> newClientMods) {
        this.clientMods = newClientMods;
    }

    /**
     * Getter for the list of directories in the modpack to copy to the server pack.
     * @return List String. Returns the list of directories to copy to the server pack for use in<br>
     * {@link #checkConfigFile(File, boolean)}<br>
     * {@link #isCurse()}
     */
    public List<String> getCopyDirs() {
        return copyDirs;
    }

    /**
     * Setter for the list of directories in the modpack to copy to the server pack.
     * For use in {@link #isDir(String)} and {@link #isCurse()}
     * @param newCopyDirs The new list of directories to include in server pack to store.
     */
    void setCopyDirs(List<String> newCopyDirs) {
        for (int i = 0; i < newCopyDirs.size(); i++) {
            newCopyDirs.removeIf(n -> (n.equalsIgnoreCase("server_pack")));
        }
        this.copyDirs = newCopyDirs;
    }

    /**
     * Getter for the path to the modpack directory.
     * @return String. Returns the path to the modpack directory for use in<br>
     * {@link #checkConfigFile(File, boolean)}<br>
     * {@link #isDir(String)}<br>
     * {@link #isCurse()}
     */
    public String getModpackDir() {
        return modpackDir;
    }

    /**
     * Setter for the path to the modpack directory. Replaces any occurrences of \ with /.
     * For use in {@link #isDir(String)} and {@link #isCurse()}
     * @param newModpackDir The new modpack directory path to store.
     */
    void setModpackDir(String newModpackDir) {
        newModpackDir = newModpackDir.replace("\\","/");
        this.modpackDir = newModpackDir;
    }

    /**
     * Getter for the path to the Java executable/binary.
     * @return String. Returns the path to the Java executable/binary for use in {@link #checkConfigFile(File, boolean)} and {@link #isCurse()}
     */
    String getJavaPath() {
        return javaPath;
    }

    /**
     * Setter for the path to the Java executable/binary. Replaces any occurrences of \ with /.
     * For use in {@link #isDir(String)} and {@link #isCurse()}
     * @param newJavaPath The new Java path to store.
     */
    void setJavaPath(String newJavaPath) {
        newJavaPath = newJavaPath.replace("\\", "/");
        this.javaPath = newJavaPath;
    }

    /**
     * Getter for the version of Minecraft used by the modpack.
     * @return String. Returns the  for use in {@link #checkConfigFile(File, boolean)} and {@link #isCurse()}
     */
    String getMinecraftVersion() {
        return minecraftVersion;
    }

    /**
     * Setter for the Minecraft version used by the modpack.
     * For use in {@link #isDir(String)} and {@link #isCurse()}
     * @param newMinecraftVersion The new Minecraft version to store.
     */
    void setMinecraftVersion(String newMinecraftVersion) {
        this.minecraftVersion = newMinecraftVersion;
    }

    /**
     * Getter for the modloader used by the modpack.
     * @return String. Returns the modloader used by the modpack for use in<br>
     * {@link #checkConfigFile(File, boolean)}<br>
     * {@link #isDir(String)}<br>
     * {@link #isCurse()}
     */
    String getModLoader() {
        return modLoader;
    }

    /**
     * Setter for the modloader used by the modpack.
     * For use in {@link #isDir(String)} and {@link #isCurse()}
     * @param newModLoader The new modloader to store.
     */
    void setModLoader(String newModLoader) {
        this.modLoader = setModLoaderCase(newModLoader);
    }

    /**
     * Getter for the version of the modloader used by the modpack.
     * @return String. Returns the version of the modloader used by the modpack for use in {@link #checkConfigFile(File, boolean)} and {@link #isCurse()}
     */
    String getModLoaderVersion() {
        return modLoaderVersion;
    }

    /**
     * Setter for the version of the modloader used by the modpack.
     * For use in {@link #isDir(String)} and {@link #isCurse()}
     * @param newModLoaderVersion The new modloader version to store.
     */
    void setModLoaderVersion(String newModLoaderVersion) {
        this.modLoaderVersion = newModLoaderVersion;
    }

    /**
     * Getter for whether the modloader server installation should be included.
     * @return Boolean. Returns whether the server installation should be included, for use in<br>
     * {@link #checkConfigFile(File, boolean)}<br>
     * {@link #isDir(String)}<br>
     * {@link #isCurse()}
     */
    boolean getIncludeServerInstallation() {
        return includeServerInstallation;
    }

    /**
     * Setter for whether the modloader server installation should be included.
     * For use in {@link #checkConfigFile(File, boolean)}
     * @param newIncludeServerInstallation The new boolean to store.
     */
    void setIncludeServerInstallation(boolean newIncludeServerInstallation) {
        this.includeServerInstallation = newIncludeServerInstallation;
    }

    /**
     * Getter for whether the server-icon.png should be included in the server pack.
     * @return Boolean. Returns whether the server-icon.png should be included in the server pack, for use in {@link #checkConfigFile(File, boolean)}
     * and {@link #isCurse()}
     */
    boolean getIncludeServerIcon() {
        return includeServerIcon;
    }

    /**
     * Setter for whether the server-icon.png should be included in the server pack.
     * For use in {@link #checkConfigFile(File, boolean)}
     * @param newIncludeServerIcon The new boolean to store.
     */
    void setIncludeServerIcon(boolean newIncludeServerIcon) {
        this.includeServerIcon = newIncludeServerIcon;
    }

    /**
     * Getter for whether the server.properties should be included in the server pack.
     * @return Boolean. Returns whether the server.properties should be included in the server pack, for use in
     * {@link #checkConfigFile(File, boolean)} and {@link #isCurse()}
     */
    boolean getIncludeServerProperties() {
        return includeServerProperties;
    }

    /**
     * Setter for whether the server.properties should be included in the server pack.
     * For use in {@link #checkConfigFile(File, boolean)}
     * @param newIncludeServerProperties The new boolean to store.
     */
    void setIncludeServerProperties(boolean newIncludeServerProperties) {
        this.includeServerProperties = newIncludeServerProperties;
    }

    /**
     * Getter for whether the start scripts should be included in the server pack.
     * @return Boolean. Returns the whether the start scripts should be included in the server pack, for use in {@link #checkConfigFile(File, boolean)} and {@link #isCurse()}
     */
    boolean getIncludeStartScripts() {
        return includeStartScripts;
    }

    /**
     * Setter for whether the start scripts should be included in the server pack.
     * For use in {@link #checkConfigFile(File, boolean)}
     * @param newIncludeStartScripts The new boolean to store.
     */
    void setIncludeStartScripts(boolean newIncludeStartScripts) {
        this.includeStartScripts = newIncludeStartScripts;
    }

    /**
     * Getter for whether a ZIP-archive of the server pack should be created.
     * @return Boolean. Returns whether a ZIP-archive of the server pack should be created, for use in {@link #checkConfigFile(File, boolean)} and {@link #isCurse()}
     */
    boolean getIncludeZipCreation() {
        return includeZipCreation;
    }

    /**
     * Setter for whether a ZIP-archive of the server pack should be created.
     * For use in {@link #checkConfigFile(File, boolean)}
     * @param newIncludeZipCreation The new boolean to store.
     */
    void setIncludeZipCreation(boolean newIncludeZipCreation) {
        this.includeZipCreation = newIncludeZipCreation;
    }

    /**
     * Getter for the CurseForge projectID of a modpack, which will be created by {@link CurseCreateModpack#curseForgeModpack(String, Integer, Integer)}.
     * @return Integer. Returns the CurseForge projectID of a modpack, for use in {@link CurseCreateModpack#curseForgeModpack(String, Integer, Integer)} and {@link #checkCurseForge(String)}
     */
    int getProjectID() {
        return projectID;
    }

    /**
     * Setter for the CurseForge projectID of a modpack, which will be created by {@link CurseCreateModpack#curseForgeModpack(String, Integer, Integer)}.
     * For use in {@link #checkCurseForge(String)}
     * @param newProjectID The new projectID to store.
     */
    void setProjectID(int newProjectID) {
        this.projectID = newProjectID;
    }

    /**
     * Getter for the CurseForge file of a modpack, which will be created by {@link CurseCreateModpack#curseForgeModpack(String, Integer, Integer)}.
     * @return Integer. Returns the CurseForge fileID of a modpack, for use in {@link #isCurse()} and {@link #checkCurseForge(String)}
     */
    int getProjectFileID() {
        return projectFileID;
    }

    /**
     * Setter for the CurseForge file of a modpack, which will be created by {@link CurseCreateModpack#curseForgeModpack(String, Integer, Integer)}.
     * For use in {@link #checkCurseForge(String)}
     * @param newProjectFileID The new projectFileID to store.
     */
    void setProjectFileID(int newProjectFileID) {
        this.projectFileID = newProjectFileID;
    }

    /**
     * Sets {@link #setConfig(File)} and calls checks for the provided configuration-file. If any check returns <code>true</code>
     * then the server pack will not be created. In order to find out which check failed, the user has to check their
     * serverpackcreator.log in the logs directory. Calls<br>
     * {@link #setConfig(File)}<br>
     * {@link #getConfig()}<br>
     * {@link #setClientMods(List)}<br>
     * {@link #getFallbackModsList()}<br>
     * {@link #setIncludeServerInstallation(boolean)}<br>
     * {@link #setIncludeServerIcon(boolean)}<br>
     * {@link #setIncludeServerProperties(boolean)}<br>
     * {@link #setIncludeStartScripts(boolean)}<br>
     * {@link #setIncludeZipCreation(boolean)}<br>
     * {@link #checkModpackDir(String)}<br>
     * {@link #checkCurseForge(String)}<br>
     * {@link #isDir(String)}<br>
     * {@link #isCurse()}<br>
     * {@link #printConfig(String, List, List, boolean, String, String, String, String, boolean, boolean, boolean, boolean)}
     * @param configFile File. The configuration file to check. Must be a valid configuration file for serverpackcreator to work.
     * @param shouldModpackBeCreated Boolean. Whether the CurseForge modpack should be downloaded and created.
     * @return Boolean. Returns <code>false</code> if all checks are passed.
     */
    public boolean checkConfigFile(File configFile, boolean shouldModpackBeCreated) {
        boolean configHasError = false;
        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.info.checkconfig.start"));
        try {
            setConfig(configFile);
        } catch (ConfigException ex) {
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.error.checkconfig.start"));
        }

        if (getConfig().getStringList("clientMods").isEmpty()) {
            LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.warn.checkconfig.clientmods"));
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
            if (shouldModpackBeCreated) {
                configHasError = isCurse();
            } else {
                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.info.checkconfig.skipmodpackcreation"));
            }
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
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.info.checkconfig.success"));
        } else {
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.error.checkconfig.failure"));
        }
        return configHasError;
    }

    /**
     * If the in the configuration specified modpack dir is an existing directory, checks are made for valid configuration
     * of: directories to copy to server pack,<br>
     * if includeServerInstallation is <code>true</code>) path to Java executable/binary, Minecraft version, modloader and modloader version.
     * Calls<br>
     * {@link #setModpackDir(String)}<br>
     * {@link #getModpackDir()}<br>
     * {@link #checkCopyDirs(List, String)}<br>
     * {@link #setCopyDirs(List)}<br>
     * {@link #getIncludeServerInstallation()}<br>
     * {@link #checkJavaPath(String)}<br>
     * {@link #setJavaPath(String)}<br>
     * {@link #getJavaPathFromSystem(String)}<br>
     * {@link #isMinecraftVersionCorrect(String)}<br>
     * {@link #setMinecraftVersion(String)}<br>
     * {@link #checkModloader(String)}<br>
     * {@link #setModLoader(String)}<br>
     * {@link #setModLoaderCase(String)}<br>
     * {@link #checkModloaderVersion(String, String)}<br>
     * {@link #setModLoaderVersion(String)}<br>
     * @param modpackDir String. Should an existing modpack be specified, all configurations are read from the provided
     *                   configuration file and checks are made in this directory.
     * @return Boolean. Returns true if an error is found during configuration check.
     */
    boolean isDir(String modpackDir) {
        boolean configHasError = false;
        setModpackDir(modpackDir);

        if (checkCopyDirs(getConfig().getStringList("copyDirs"), getModpackDir())) {

            setCopyDirs(getConfig().getStringList("copyDirs"));

        } else { configHasError = true; }

        if (getIncludeServerInstallation()) {

            if (checkJavaPath(getConfig().getString("javaPath"))) {

                setJavaPath(getConfig().getString("javaPath"));

            } else {

                String tmpJavaPath = getJavaPathFromSystem(getConfig().getString("javaPath"));

                if (checkJavaPath(tmpJavaPath)) {

                    setJavaPath(tmpJavaPath);

                } else { configHasError = true; } }



            if (isMinecraftVersionCorrect(getConfig().getString("minecraftVersion"))) {

                setMinecraftVersion(getConfig().getString("minecraftVersion"));

            } else { configHasError = true; }

            if (checkModloader(getConfig().getString("modLoader"))) {

                setModLoader(setModLoaderCase(getConfig().getString("modLoader")));

            } else { configHasError = true; }

            if (checkModloaderVersion(getModLoader(), getConfig().getString("modLoaderVersion"))) {

                setModLoaderVersion(getConfig().getString("modLoaderVersion"));

            } else { configHasError = true; }

        } else {
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.info.checkconfig.skipstart"));
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.info.checkconfig.skipjava"));
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.info.checkconfig.skipminecraft"));
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.info.checkconfig.skipmodlaoder"));
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.info.checkconfig.skipmodloaderversion"));
        }
        return configHasError;
    }

    /**
     * If modpackDir in the configuration file is a CurseForge projectID,fileID combination, then the modpack is first
     * created from said combination, using {@link CurseCreateModpack#curseForgeModpack(String, Integer, Integer)},
     * before proceeding to checking the rest of the configuration. If everything passes and the modpack was created,
     * a new configuration file is created, replacing the one used to create the modpack in the first place, with the
     * modpackDir field pointing to the newly created modpack. Calls<br>
     * {@link CurseAPI} and various methods of it.<br>
     * {@link #setModpackDir(String)}<br>
     * {@link #getModpackDir()}<br>
     * {@link CurseCreateModpack#curseForgeModpack(String, Integer, Integer)}<br>
     * {@link CurseModpack}<br>
     * {@link #containsFabric(CurseModpack)}<br>
     * {@link #setModLoader(String)}<br>
     * {@link #setModLoaderVersion(String)}<br>
     * {@link #setModLoaderCase(String)}<br>
     * {@link #checkJavaPath(String)}<br>
     * {@link #setJavaPath(String)}<br>
     * {@link #getJavaPathFromSystem(String)}<br>
     * {@link #setCopyDirs(List)}<br>
     * {@link #suggestCopyDirs(String)}<br>
     * {@link #writeConfigToFile(String, String, String, boolean, String, String, String, String, boolean, boolean, boolean, boolean, File, boolean)}<br>
     * @return Boolean. Returns false unless an error was encountered during either the acquisition of the CurseForge
     * project name and displayname, or when the creation of the modpack fails.
     */
    boolean isCurse() {
        boolean configHasError = false;
        try {
            if (CurseAPI.project(getProjectID()).isPresent()) {

                String projectName, displayName;
                projectName = displayName = "";

                try { projectName = CurseAPI.project(getProjectID()).get().name();

                    try { displayName = Objects.requireNonNull(CurseAPI.project(getProjectID()).get().files().fileWithID(getProjectFileID())).displayName(); }
                    catch (NullPointerException npe) { LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.info.iscurse.display"));

                        try { displayName = Objects.requireNonNull(CurseAPI.project(getProjectID()).get().files().fileWithID(getProjectFileID())).nameOnDisk(); }
                        catch (NullPointerException npe2) {
                            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.error.iscurse.file"), npe2);
                            displayName = null;
                        }
                    }
                } catch (CurseException cex) { LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.error.iscurse.curseforge")); }


                if (displayName != null && projectName != null) {

                    setModpackDir(String.format("./%s/%s", projectName, displayName));

                    if (CURSECREATEMODPACK.curseForgeModpack(getModpackDir(), getProjectID(), getProjectFileID())) {
                        try {
                            byte[] jsonData = Files.readAllBytes(Paths.get(String.format("%s/manifest.json", getModpackDir())));

                            CurseModpack modpack = getObjectMapper().readValue(jsonData, CurseModpack.class);

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
                                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.info.iscurse.fabric"));

                                setModLoader("Fabric");
                                setModLoaderVersion(latestFabricLoader(getModpackDir()));

                            } else {

                                setModLoader("Forge");
                                setModLoaderVersion(modLoaderVersion[1]);

                            }
                        } catch (IOException ex) {
                            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.error.iscurse.json"), ex);
                        }

                        if (checkJavaPath(getConfig().getString("javaPath").replace("\\", "/"))) {

                            setJavaPath(getConfig().getString("javaPath").replace("\\", "/"));

                        } else {
                            String tmpJavaPath = getJavaPathFromSystem(getConfig().getString("javaPath").replace("\\", "/"));

                            if (checkJavaPath(tmpJavaPath)) {
                                setJavaPath(tmpJavaPath);
                            }

                        }
                        setCopyDirs(suggestCopyDirs(getModpackDir()));

                        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.info.iscurse.replace"));

                        writeConfigToFile(
                                getModpackDir(),
                                buildString(getClientMods().toString()),
                                buildString(getCopyDirs().toString()),
                                getIncludeServerInstallation(),
                                getJavaPath(),
                                getMinecraftVersion(),
                                getModLoader(),
                                getModLoaderVersion(),
                                getIncludeServerIcon(),
                                getIncludeServerProperties(),
                                getIncludeStartScripts(),
                                getIncludeZipCreation(),
                                getConfigFile(),
                                false
                        );

                    } else { configHasError = true; }
                } else { configHasError = true; }
            } else { configHasError = true; }

        } catch (CurseException cex) {
            LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.error.iscurse.project"), getProjectID()), cex);
            configHasError = true;
        }
        return configHasError;
    }

    /**
     * Checks whether the projectID for the Jumploader mod is present in the list of mods required by the CurseForge modpack.
     * If Jumploader is found, the modloader for the new configuration-file will be set to Fabric.
     * @param modpack CurseModpack. Contains information about the CurseForge modpack. Used to get a list of all projects
     *               required by the modpack.
     * @return Boolean. Returns true if Jumploader is found.
     */
    boolean containsFabric(CurseModpack modpack) {
        boolean hasJumploader = false;

        for (int i = 0; i < modpack.getFiles().size(); i++) {

            String[] mods = modpack.getFiles().get(i).toString().split(",");

            if (mods[0].equalsIgnoreCase("361988") || mods[0].equalsIgnoreCase("306612")) {
                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.info.containsfabric"));
                hasJumploader = true;
            }
        }
        return hasJumploader;
    }

    /**
     * Creates a list of suggested directories to include in server pack which is later on written to a new configuration file.
     * The list of directories to include in the server pack which is generated by this method excludes well know directories
     * which would not be needed by a server pack. If you have suggestions to this list, open an issue on
     * <a href=https://github.com/Griefed/ServerPackCreator/issues>GitHub</a>
     * @param modpackDir String. The directory for which to gather a list of directories to copy to the server pack.
     * @return List, String. Returns a list of directories inside the modpack, excluding well known client-side only
     * directories.
     */
    List<String> suggestCopyDirs(String modpackDir) {
        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.info.suggestcopydirs.start"));

        List<String> dirsNotToCopy = new ArrayList<>(Arrays.asList(
                "overrides",
                "packmenu",
                "resourcepacks",
                "server_pack"
        ));

        File[] listDirectoriesInModpack = new File(modpackDir).listFiles();

        List<String> dirsInModpack = new ArrayList<>();

        try {
            assert listDirectoriesInModpack != null;
            for (File dir : listDirectoriesInModpack) {
                if (dir.isDirectory()) {
                    dirsInModpack.add(dir.getName());
                }
            }
        } catch (NullPointerException np) {
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.error.suggestcopydirs"));
        }

        for (int idirs = 0; idirs < dirsNotToCopy.size(); idirs++) {

            int i = idirs;

            dirsInModpack.removeIf(n -> (n.contains(dirsNotToCopy.get(i))));
        }

        LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.info.suggestcopydirs.list"),dirsInModpack));

        return dirsInModpack;
    }

    /**
     * Checks whether the specified modpack directory contains a valid projectID,fileID combination.
     * ProjectIDs must be at least two digits long, fileIDs must be at least 5 digits long.
     * Must be numbers separated by a ",". If modpackDir successfully matched a projectID,fileID combination, CurseForge
     * is then checked for existence of said projectID and fileID. If the project can not be found or the file returns null
     * then false is returned and the check is considered failed.
     * @param modpackDir String. The string which to check for a valid projectID,fileID combination.
     * @return Boolean. Returns true if the combination is deemed valid, false if not.
     */
    boolean checkCurseForge(String modpackDir) {
        String[] curseForgeIDCombination;
        boolean configCorrect = false;

        if (modpackDir.matches("[0-9]{2,},[0-9]{5,}")) {

            curseForgeIDCombination = modpackDir.split(",");
            int curseProjectID = Integer.parseInt(curseForgeIDCombination[0]);
            int curseFileID= Integer.parseInt(curseForgeIDCombination[1]);

            try {
                if (CurseAPI.project(curseProjectID).isPresent()) {
                    setProjectID(curseProjectID);
                    configCorrect = true;
                }
            } catch (CurseException cex) {
                LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.error.iscurse.project"), curseProjectID),cex);
                configCorrect = false;
            }

            try {
                if (CurseAPI.project(curseProjectID).get().files().fileWithID(curseFileID) != null) {
                    setProjectFileID(curseFileID);
                    configCorrect = true;
                }
            } catch (CurseException cex) {
                LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.error.iscurse.file"), curseFileID), cex);
                configCorrect = false;
            }

            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.info.checkcurseforge.info"));
            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.info.checkcurseforge.return"), getProjectID(), getProjectFileID()));
            LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.warn.checkcurseforge.warn"));


        } else { LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.warn.checkcurseforge.warn2")); }

        return configCorrect;
    }

    /**
     * Converts various strings to booleans, by using regex, to allow for more variations in input.<br>
     * <strong>Converted to <code>TRUE</code> are:<br></strong>
     * <code>[Tt]rue</code><br>
     * <code>1</code><br>
     * <code>[Yy]es</code><br>
     * <code>[Yy]</code><br>
     * Language Key <code>cli.input.true</code><br>
     * Language Key <code>cli.input.yes</code><br>
     * Language Key <code>cli.input.yes.short</code><br>
     * <strong>Converted to <code>FALSE</code> are:<br></strong>
     * <code>[Ff]alse</code><br>
     * <code>0</code><br>
     * <code>[Nn]o</code><br>
     * <code>[Nn]</code><br>
     * Language Key <code>cli.input.false</code><br>
     * Language Key <code>cli.input.no</code><br>
     * Language Key <code>cli.input.no.short</code><br>
     * @param stringBoolean String. The string which should be converted to boolean if it matches certain patterns.
     * @return Boolean. Returns the corresponding boolean if match with pattern was found. If no match is found, assume and return false.
     */
    public boolean convertToBoolean(String stringBoolean) {
        boolean returnBoolean;

        if (stringBoolean.matches("[Tt]rue")    ||
                stringBoolean.matches("1")      ||
                stringBoolean.matches("[Yy]es") ||
                stringBoolean.matches("[Yy]")   ||
                stringBoolean.matches(LOCALIZATIONMANAGER.getLocalizedString("cli.input.true")) ||
                stringBoolean.matches(LOCALIZATIONMANAGER.getLocalizedString("cli.input.yes"))  ||
                stringBoolean.matches(LOCALIZATIONMANAGER.getLocalizedString("cli.input.yes.short"))
        ){
            returnBoolean = true;

        } else if (stringBoolean.matches("[Ff]alse") ||
                stringBoolean.matches("0")           ||
                stringBoolean.matches("[Nn]o")       ||
                stringBoolean.matches("[Nn]" )       ||
                stringBoolean.matches(LOCALIZATIONMANAGER.getLocalizedString("cli.input.false")) ||
                stringBoolean.matches(LOCALIZATIONMANAGER.getLocalizedString("cli.input.no"))    ||
                stringBoolean.matches(LOCALIZATIONMANAGER.getLocalizedString("cli.input.no.short"))
        ){
            returnBoolean = false;

        } else {
            LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.warn.converttoboolean.warn"));
            returnBoolean = false;
        }

        return returnBoolean;
    }

    /**
     * Prints all passed fields to the console and serverpackcreator.log. Used to show the user the configuration before
     * ServerPackCreator starts the generation of the server pack or, if checks failed, to show the user their last
     * configuration so they can more easily identify problems with said configuration.<br>
     * Should a user report an issue on GitHub and include their logs (which I hope they do....), this would also
     * help me help them. Logging is good. People should use more logging.
     * @param modpackDirectory String. The used modpackDir field either from a configuration file or from configuration setup.
     * @param clientsideMods String List. List of clientside-only mods to exclude from the server pack..
     * @param copyDirectories String List. List of directories in the modpack which are to be included in the server pack.
     * @param installServer Boolean. Whether to install the modloader server in the server pack.
     * @param javaInstallPath String. Path to the Java executable/binary needed for installing the modloader server in the server pack.
     * @param minecraftVer String. The Minecraft version the modpack uses.
     * @param modloader String. The modloader the modpack uses.
     * @param modloaderVersion String. The version of the modloader the modpack uses.
     * @param includeIcon Boolean. Whether to include the server-icon.png in the server pack.
     * @param includeProperties Boolean. Whether to include the server.properties in the server pack.
     * @param includeScripts Boolean. Whether to include the start scripts for the specified modloader in the server pack.
     * @param includeZip Boolean. Whether to create a zip-archive of the server pack, excluding the Minecraft server JAR according to Mojang's TOS and EULA.
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

        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.info.printconfig.start"));
        LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.info.printconfig.modpackdir"), modpackDirectory));

        if (clientsideMods.isEmpty()) {
            LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.warn.printconfig.noclientmods"));
        } else {

            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.info.printconfig.clientmods"));
            for (String mod : clientsideMods) {
                LOG.info(String.format("    %s", mod));
            }

        }

        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.info.printconfig.copydirs"));

        if (copyDirectories != null) {

            for (String directory : copyDirectories) {
                LOG.info(String.format("    %s", directory));
            }

        } else {
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.error.printconfig.copydirs"));
        }

        LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.info.printconfig.server"), installServer));
        LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.info.printconfig.javapath"), javaInstallPath));
        LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.info.printconfig.minecraftversion"), minecraftVer));
        LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.info.printconfig.modloader"), modloader));
        LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.info.printconfig.modloaderversion"), modloaderVersion));
        LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.info.printconfig.icon"), includeIcon));
        LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.info.printconfig.properties"), includeProperties));
        LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.info.printconfig.scripts"), includeScripts));
        LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.info.printconfig.zip"), includeZip));
    }

    /**
     * Checks whether the passed String is empty and if it is empty, prints the corresponding message to the console and
     * serverpackcreator.log so the user knows what went wrong.<br>
     * Checks whether the passed String is a directory and if it is not, prints the corresponding message to the console
     * and serverpackcreator.log so the user knows what went wrong.
     * @param modpackDir String. The path to the modpack directory to check whether it is empty and whether it is a directory.
     * @return Boolean. Returns true if the directory exists.
     */
    boolean checkModpackDir(String modpackDir) {
        boolean configCorrect = false;

        if (modpackDir.equals("")) {

            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.error.checkmodpackdir"));

        } else if (!(new File(modpackDir).isDirectory())) {

            LOG.warn(String.format(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.warn.checkmodpackdir"), modpackDir));

        } else {

            configCorrect = true;

        }
        return configCorrect;
    }

    /**
     * Checks whether the passed list of directories which are supposed to be in the modpack directory is empty and
     * prints a message to the console and serverpackcreator.log if it is.<br>
     * Checks whether all directories in the list exist in the modpack directory and prints a message to the console
     * and serverpackcreator.log if any one of the directories could not be found.
     * @param directoriesToCopy List String. The list of directories to check for existence.
     * @param modpackDir String. The path to the modpack directory in which to check for existence of the passed list of
     *                  directories.
     * @return Boolean. Returns true if every directory was found in the modpack directory. If any single one was not found,
     * false is returned.
     */
    boolean checkCopyDirs(List<String> directoriesToCopy, String modpackDir) {
        boolean configCorrect = true;

        if (directoriesToCopy.isEmpty()) {

            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.error.checkcopydirs.empty"));
            configCorrect = false;

        } else {

            for (String directory : directoriesToCopy) {

                File dirToCheck = new File(String.format("%s/%s", modpackDir, directory));

                if (!dirToCheck.exists() || !dirToCheck.isDirectory()) {

                    LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.error.checkcopydirs.notfound"), dirToCheck.getAbsolutePath()));
                    configCorrect = false;
                }
            }
        }
        return configCorrect;
    }

    /**
     * Checks the passed String whether it is empty, and if it is, automatically acquires the path to the users Java
     * installation and appends bin/java.exe or bin/java depending on whether the path to said installation starts with
     * Windows-typical C: prefix.
     * @param enteredPath String. The path to check whether it is empty.
     * @return String. Returns the passed String as is if it is not empty. Returns the automatically acquired path to the
     * Java executable/binary if the passed String was empty.
     */
    String getJavaPathFromSystem(String enteredPath) {
        String autoJavaPath;

        if (enteredPath.equals("")) {

            LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.warn.getjavapath.empty"));
            autoJavaPath = String.format("%s/bin/java",System.getProperty("java.home").replace("\\", "/"));

            if (autoJavaPath.startsWith("C:")) {

                autoJavaPath = String.format("%s.exe", autoJavaPath);
            }

            LOG.warn(String.format(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.warn.getjavapath.set"), autoJavaPath));
            return autoJavaPath;

        } else {

            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.warn.getjavapath.set"), enteredPath));
            return enteredPath;
        }
    }

    /**
     * Checks whether the passed String ends with <code>java.exe</code> or <code>java</code> and whether the files exist.
     * @param pathToJava String. The path to check for java.exe and java.
     * @return Boolean. Returns true if the String ends with java.exe or java, and if either of these files exist.
     */
    boolean checkJavaPath(String pathToJava) {
        boolean configCorrect = false;

        if (new File(pathToJava).exists() && pathToJava.endsWith("java.exe")) {

            configCorrect = true;

        } else if (new File(pathToJava).exists() && pathToJava.endsWith("java")) {

            configCorrect = true;

        } else {
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.error.checkjavapath"));
        }
        return configCorrect;
    }

    /**
     * Checks whether either Forge or Fabric were specified as the modloader.
     * @param modloader String. Check case insensitive for Forge or Fabric.
     * @return Boolean. Returns true if the specified modloader is either Forge or Fabric. False if neither.
     */
    boolean checkModloader(String modloader) {
        boolean configCorrect = false;

        if (modloader.equalsIgnoreCase("Forge") || modloader.equalsIgnoreCase("Fabric")) {

            configCorrect = true;

        } else {

            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.error.checkmodloader"));
        }
        return configCorrect;
    }

    /**
     * Ensures the modloader is normalized to first letter upper case and rest lower case. Basically allows the user to
     * input Forge or Fabric in any combination of upper- and lowercase and ServerPackCreator will still be able to
     * work with the users input.
     * @param modloader String. The String to check for case-insensitive cases of either Forge or Fabric.
     * @return String. Returns a normalized String of the specified modloader.
     */
    String setModLoaderCase(String modloader) {
        String returnLoader = null;

        if (modloader.equalsIgnoreCase("Forge")) {

            returnLoader = "Forge";

        } else if (modloader.equalsIgnoreCase("Fabric")) {

            returnLoader = "Fabric";
        }
        return returnLoader;
    }

    /**
     * Depending on whether Forge or Fabric was specified as the modloader, this will call the corresponding version check
     * to verify that the user correctly set their modloader version.<br>
     * If the user specified Forge as their modloader, {@link #isForgeVersionCorrect(String)} is called and the version
     * the user specified is checked against Forge's version manifest..<br>
     * If the user specified Fabric as their modloader, {@link #isFabricVersionCorrect(String)} is called and the version
     * the user specified is checked against Fabric's version manifest.
     * @param modloader String. The passed modloader which determines whether the check for Forge or Fabric is called.
     * @param modloaderVersion String. The version of the modloader which is checked against the corresponding modloaders manifest.
     * @return Boolean. Returns true if the specified modloader version was found in the corresponding manifest.
     */
    boolean checkModloaderVersion(String modloader, String modloaderVersion) {
        boolean isVersionCorrect = false;

        if (modloader.equalsIgnoreCase("Forge") && isForgeVersionCorrect(modloaderVersion)) {

            isVersionCorrect = true;

        } else if (modloader.equalsIgnoreCase("Fabric") && isFabricVersionCorrect(modloaderVersion)) {

            isVersionCorrect = true;

        } else {
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.error.checkmodloaderversion"));
        }
        return isVersionCorrect;
    }

    /**
     * Checks whether the passed String is empty and if it is not. check the String against Mojang's version manifest
     * to validate the version.
     * @param minecraftVersion String. The version to check for in Mojang's version manifest.
     * @return Boolean. Returns true if the specified Minecraft version could be found in Mojang's manifest.
     */
    boolean isMinecraftVersionCorrect(String minecraftVersion) {
        if (!minecraftVersion.equals("")) {
            try {
                URL manifestJsonURL = getMinecraftManifestUrl();
                ReadableByteChannel readableByteChannel = Channels.newChannel(manifestJsonURL.openStream());
                FileOutputStream downloadManifestOutputStream;

                try {
                    downloadManifestOutputStream = new FileOutputStream("mcmanifest.json");
                } catch (FileNotFoundException ex) {

                    LOG.debug(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.debug.isminecraftversioncorrect"), ex);

                    File file = new File("mcmanifest.json");

                    if (!file.exists()) {

                        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.info.isminecraftversioncorrect.create"));
                        boolean jsonCreated = file.createNewFile();

                        if (jsonCreated) {

                            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.info.isminecraftversioncorrect.created"));

                        } else {

                            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.error.isminecraftversioncorrect.parse"));
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

                LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.error.isminecraftversioncorrect.validate"), minecraftVersion), ex);
                return false;
            }
        } else {
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.error.isminecraftversioncorrect.empty"));
            return false;
        }
    }

    /**
     * Checks whether the passed String is empty and if it is not. check the String against Fabric's version manifest
     * to validate the version.
     * @param fabricVersion String. The version to check for in Fabric's version manifest.
     * @return Boolean. Returns true if the specified fabric version could be found in Fabric's manifest.
     */
    boolean isFabricVersionCorrect(String fabricVersion) {
        if (!fabricVersion.equals("")) {
            try {
                URL manifestJsonURL = getFabricManifestUrl();

                ReadableByteChannel readableByteChannel = Channels.newChannel(manifestJsonURL.openStream());

                FileOutputStream downloadManifestOutputStream;

                try {
                    downloadManifestOutputStream = new FileOutputStream("fabric-manifest.xml");
                } catch (FileNotFoundException ex) {

                    LOG.debug(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.debug.isfabricversioncorrect"), ex);
                    File file = new File("fabric-manifest.xml");

                    if (!file.exists()) {

                        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.info.isfabricversioncorrect.create"));
                        boolean jsonCreated = file.createNewFile();

                        if (jsonCreated) {

                            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.info.isfabricversioncorrect.created"));

                        } else {
                            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.error.isfabricversioncorrect.parse"));
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

                LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.error.isfabricversioncorrect.validate"), ex);
                return false;
            }

        } else {

            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.error.isfabricversioncorrect.empty"));
            return false;
        }
    }

    /**
     * Checks whether the passed String is empty and if it is not. check the String against Forge's version manifest
     * to validate the version.
     * @param forgeVersion String. The version to check for in Forge's version manifest.
     * @return Boolean. Returns true if the specified Forge version could be found in Forge's manifest.
     */
    boolean isForgeVersionCorrect(String forgeVersion) {
        if (!forgeVersion.equals("")) {
            try {
                URL manifestJsonURL = getForgeManifestUrl();
                ReadableByteChannel readableByteChannel = Channels.newChannel(manifestJsonURL.openStream());
                FileOutputStream downloadManifestOutputStream;

                try {

                    downloadManifestOutputStream = new FileOutputStream("forge-manifest.json");

                } catch (FileNotFoundException ex) {

                    LOG.debug(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.debug.isforgeversioncorrect"), ex);
                    File file = new File("forge-manifest.json");

                    if (!file.exists()) {

                        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.info.isforgeversioncorrect.create"));

                        boolean jsonCreated = file.createNewFile();

                        if (jsonCreated) {

                            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.info.isforgeversioncorrect.created"));
                        } else {

                            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.error.isforgeversioncorrect.parse"));
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

                LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.error.isforgeversioncorrect.validate"), ex);
                return false;
            }
        } else {

            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.error.isforgeversioncorrect.empty"));
            return false;
        }
    }

    /**
     * Returns the latest version for the Fabric-loader. If Fabric's version manifest should be unreachable for whatever
     * reason, version 0.11.3 is returned by default.
     * @param modpackDir String. The Fabric-loader manifest will be stored inside the server_pack directory inside the modpack
     *                   directory.
     * @return Boolean. Returns true if the download was successful. False if not.
     */
    String latestFabricLoader(String modpackDir) {
        String result = "0.11.3";

        try {
            URL downloadFabricXml = getFabricManifestUrl();
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

            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.info.latestfabricloader.created"));

        } catch (IOException | ParserConfigurationException | SAXException | XPathExpressionException ex) {

            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.error.latestfabricloader.parse"), ex);
        }

        return result;

    }

    /**
     * Walk the user through the generation of a new ServerPackCreator configuration file by asking them for input,
     * step-by-step, regarding their modpack. At the end of this method a fully configured serverpackcreator.conf file
     * is saved and any previously existing configuration file replaced by the new one.<br>
     * After every input, said input is displayed to the user and they're asked whether they are satisfied with said
     * input. The user can then decide whether they would like to restart the entry of the field they just configured,
     * or agree and move to the next one.<br>
     * At the end of this method, the user will have a newly configured and created configuration file for ServerPackCreator.<br>
     * <br>
     * Most user-input is checked after entry to ensure the configuration is already in working-condition after completion
     * of this method.<br>
     * Calls<br>
     * {@link #checkModpackDir(String)}<br>
     * {@link #readBoolean()}<br>
     * {@link #getFallbackModsList()}<br>
     * {@link #readStringArray()}<br>
     * {@link #checkCopyDirs(List, String)}<br>
     * {@link #isMinecraftVersionCorrect(String)}<br>
     * {@link #checkModloader(String)}<br>
     * {@link #setModLoaderCase(String)}<br>
     * {@link #checkModloaderVersion(String, String)}<br>
     * {@link #getJavaPathFromSystem(String)}<br>
     * {@link #checkJavaPath(String)}<br>
     * {@link #printConfig(String, List, List, boolean, String, String, String, String, boolean, boolean, boolean, boolean)}<br>
     * {@link #writeConfigToFile(String, String, String, boolean, String, String, String, String, boolean, boolean, boolean, boolean, File, boolean)}
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

        LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("clisetup.log.info.start"), "-cgen"));
        do {
//--------------------------------------------------------------------------------------------MODPACK DIRECTORY---------
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("clisetup.log.info.modpack.enter"));
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("clisetup.log.info.modpack.example"));

            do {

                do {
                    System.out.print(LOCALIZATIONMANAGER.getLocalizedString("clisetup.log.info.modpack.cli"));
                    tmpModpackDir = reader.nextLine();
                } while (!checkModpackDir(tmpModpackDir));

                LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("clisetup.log.info.checkreturn"), tmpModpackDir));
                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("clisetup.log.info.modpack.checkreturninfo"));
                System.out.print(LOCALIZATIONMANAGER.getLocalizedString("clisetup.log.info.answer"));

            } while (!readBoolean());

            modpackDir = tmpModpackDir.replace("\\", "/");
            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("clisetup.log.info.checkreturn"), modpackDir));
            System.out.println();

//-----------------------------------------------------------------------------------------CLIENTSIDE-ONLY MODS---------
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("clisetup.log.info.clientmods.enter"));
            do {
                clientMods.addAll(readStringArray());
                LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("clisetup.log.info.checkreturn"), clientMods));

                if (clientMods.isEmpty()) {
                    clientMods = getFallbackModsList();
                    LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("configcheck.log.warn.checkconfig.clientmods"));

                    for (String mod : clientMods) {
                        LOG.warn(String.format("    %s", mod));
                    }
                }

                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("clisetup.log.info.clientmods.checkreturninfo"));
                System.out.print(LOCALIZATIONMANAGER.getLocalizedString("clisetup.log.info.answer"));

            } while (!readBoolean());
            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("clisetup.log.info.checkreturn"), clientMods));
            tmpClientMods = new String[clientMods.size()];
            clientMods.toArray(tmpClientMods);
            System.out.println();

//---------------------------------------------------------------------------DIRECTORIES TO COPY TO SERVER PACK---------
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("clisetup.log.info.copydirs.enter"));
            File directories = new File(modpackDir);
            List<String> dirList = Arrays.asList(Objects.requireNonNull(directories.list((current, name) -> new File(current, name).isDirectory())));
            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("clisetup.log.info.copydirs.dirsinmodpack"), dirList.toString().replace("[","").replace("]","")));
            do {
                do {
                    copyDirs.clear();
                    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("clisetup.log.info.copydirs.specify"));
                    copyDirs.addAll(readStringArray());

                } while (!checkCopyDirs(copyDirs, modpackDir));

                LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("clisetup.log.info.checkreturn"), copyDirs));
                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("clisetup.log.info.copydirs.checkreturninfo"));
                System.out.print(LOCALIZATIONMANAGER.getLocalizedString("clisetup.log.info.answer"));
            } while (!readBoolean());
            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("clisetup.log.info.checkreturn"), copyDirs));
            tmpCopyDirs = new String[copyDirs.size()];
            copyDirs.toArray(tmpCopyDirs);
            System.out.println();

//-------------------------------------------------------------WHETHER TO INCLUDE MODLOADER SERVER INSTALLATION---------
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("clisetup.log.info.server.enter"));
            System.out.print(LOCALIZATIONMANAGER.getLocalizedString("clisetup.log.info.server.include"));
            includeServerInstallation = readBoolean();
            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("clisetup.log.info.checkreturn"), includeServerInstallation));

//-------------------------------------------------------------------------------MINECRAFT VERSION MODPACK USES---------
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("clisetup.log.info.minecraft.enter"));
            do {
                System.out.print(LOCALIZATIONMANAGER.getLocalizedString("clisetup.log.info.minecraft.specify"));
                minecraftVersion = reader.nextLine();
            } while (!isMinecraftVersionCorrect(minecraftVersion));
            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("clisetup.log.info.checkreturn"), minecraftVersion));
            System.out.println();

//---------------------------------------------------------------------------------------MODLOADER MODPACK USES---------
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("clisetup.log.info.modloader.enter"));
            do {
                System.out.print(LOCALIZATIONMANAGER.getLocalizedString("clisetup.log.info.modloader.cli"));
                modLoader = reader.nextLine();
            } while (!checkModloader(modLoader));
            modLoader = setModLoaderCase(modLoader);
            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("clisetup.log.info.checkreturn"), modLoader));
            System.out.println();

//----------------------------------------------------------------------------VERSION OF MODLOADER MODPACK USES---------
            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("clisetup.log.info.modloaderversion.enter"), modLoader));
            do {
                System.out.print(LOCALIZATIONMANAGER.getLocalizedString("clisetup.log.info.modloaderversion.cli"));
                modLoaderVersion = reader.nextLine();
            } while (!checkModloaderVersion(modLoader, modLoaderVersion));
            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("clisetup.log.info.checkreturn"), modLoaderVersion));
            System.out.println();

//------------------------------------------------------------------------------------PATH TO JAVA INSTALLATION---------
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("clisetup.log.info.java.enter"));
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("clisetup.log.info.java.enter2"));
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("clisetup.log.info.java.example"));
            do {
                System.out.print(LOCALIZATIONMANAGER.getLocalizedString("clisetup.log.info.java.cli"));
                String tmpJavaPath = reader.nextLine();
                javaPath = getJavaPathFromSystem(tmpJavaPath);
            } while (!checkJavaPath(javaPath));
            System.out.println();

//------------------------------------------------------------WHETHER TO INCLUDE SERVER-ICON.PNG IN SERVER PACK---------
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("clisetup.log.info.icon.enter"));
            System.out.print(LOCALIZATIONMANAGER.getLocalizedString("clisetup.log.info.icon.cli"));
            includeServerIcon = readBoolean();
            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("clisetup.log.info.checkreturn"), includeServerIcon));
            System.out.println();

//----------------------------------------------------------WHETHER TO INCLUDE SERVER.PROPERTIES IN SERVER PACK---------
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("clisetup.log.info.properties.enter"));
            System.out.print(LOCALIZATIONMANAGER.getLocalizedString("clisetup.log.info.properties.cli"));
            includeServerProperties = readBoolean();
            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("clisetup.log.info.checkreturn"), includeServerProperties));
            System.out.println();

//--------------------------------------------------------------WHETHER TO INCLUDE START SCRIPTS IN SERVER PACK---------
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("clisetup.log.info.scripts.enter"));
            System.out.print(LOCALIZATIONMANAGER.getLocalizedString("clisetup.log.info.scripts.cli"));
            includeStartScripts = readBoolean();
            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("clisetup.log.info.checkreturn"), includeStartScripts));
            System.out.println();

//----------------------------------------------------WHETHER TO INCLUDE CREATION OF ZIP-ARCHIVE OF SERVER PACK---------
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("clisetup.log.info.zip.enter"));
            System.out.print(LOCALIZATIONMANAGER.getLocalizedString("clisetup.log.info.zip.cli"));
            includeZipCreation = readBoolean();
            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("clisetup.log.info.checkreturn"), includeZipCreation));

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
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("clisetup.log.info.config.enter"));
            System.out.print(LOCALIZATIONMANAGER.getLocalizedString("clisetup.log.info.answer"));
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
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("clisetup.log.info.config.written"));
        }
    }

    /**
     * A helper method for {@link #createConfigurationFile()}. Prompts the user to enter the values which will make up
     * a String List in the new configuration file. If the user enters an empty line, the method is exited and the
     * String List returned.
     * @return String List. Returns the list of values entered by the user.
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

    /**
     * Converts a sequence of Strings, for example from a list, into a concatenated String.
     * @param args Strings that will be concatenated into one string
     * @return String. Returns concatenated string that contains all provided values.
     */
    public String buildString(String... args) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(Arrays.toString(args));
        stringBuilder.delete(0, 2).reverse().delete(0,2).reverse();
        return stringBuilder.toString();
    }

    /**
     * A helper method for {@link #createConfigurationFile()}. Prompts the user to enter values which will then be
     * converted to booleans, either <code>TRUE</code> or <code>FALSE</code>. This prevents any non-boolean values
     * from being written to the new configuration file.
     * @return Boolean. True or False, depending on user input.
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
                    boolRead.matches(LOCALIZATIONMANAGER.getLocalizedString("cli.input.true")) ||
                    boolRead.matches(LOCALIZATIONMANAGER.getLocalizedString("cli.input.yes"))  ||
                    boolRead.matches(LOCALIZATIONMANAGER.getLocalizedString("cli.input.yes.short"))) {
                return true;

            } else if (boolRead.matches("[Ff]alse") ||
                    boolRead.matches("[Nn]o")    ||
                    boolRead.matches("[Nn]" )    ||
                    boolRead.matches("0")                                                            ||
                    boolRead.matches(LOCALIZATIONMANAGER.getLocalizedString("cli.input.false")) ||
                    boolRead.matches(LOCALIZATIONMANAGER.getLocalizedString("cli.input.no"))    ||
                    boolRead.matches(LOCALIZATIONMANAGER.getLocalizedString("cli.input.no.short"))) {
                return false;

            } else {
                LOG.error(LOCALIZATIONMANAGER.getLocalizedString("clisetup.log.error.answer"));
            }
        }
    }

    /** Writes a new configuration file with the parameters passed to it.<br>
     * Calls {@link #getConfigFile()}<br>
     * @param modpackDir String. The path to the modpack.
     * @param clientMods List, String. List of clientside-only mods.
     * @param copyDirs List, String. List of directories to include in server pack.
     * @param includeServer Boolean. Whether the modloader server software should be installed.
     * @param javaPath String. Path to the java executable/binary.
     * @param minecraftVersion String. Minecraft version used by the modpack and server pack.
     * @param modLoader String. Modloader used by the modpack and server pack. Ether Forge or Fabric.
     * @param modLoaderVersion String. Modloader version used by the modpack and server pack.
     * @param includeIcon Boolean. Whether to include a server-icon in the server pack.
     * @param includeProperties Boolean. Whether to include a properties file in the server pack.
     * @param includeScripts Boolean. Whether to include start scripts in the server pack.
     * @param includeZip Boolean. Whether to create a ZIP-archive of the server pack, excluding Mojang's Minecraft server JAR.
     * @param fileName The name under which to write the new configuration file.
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
                LOCALIZATIONMANAGER.getLocalizedString("filessetup.writeconfigtofile.modpackdir"), modpackDir.replace("\\","/"),
                LOCALIZATIONMANAGER.getLocalizedString("filessetup.writeconfigtofile.clientmods"), clientMods,
                LOCALIZATIONMANAGER.getLocalizedString("filessetup.writeconfigtofile.copydirs"), copyDirs,
                LOCALIZATIONMANAGER.getLocalizedString("filessetup.writeconfigtofile.includeserverinstallation"), includeServer,
                LOCALIZATIONMANAGER.getLocalizedString("filessetup.writeconfigtofile.javapath"), javaPath.replace("\\","/"),
                LOCALIZATIONMANAGER.getLocalizedString("filessetup.writeconfigtofile.minecraftversion"), minecraftVersion,
                LOCALIZATIONMANAGER.getLocalizedString("filessetup.writeconfigtofile.modloader"), modLoader,
                LOCALIZATIONMANAGER.getLocalizedString("filessetup.writeconfigtofile.modloaderversion"), modLoaderVersion,
                LOCALIZATIONMANAGER.getLocalizedString("filessetup.writeconfigtofile.includeservericon"), includeIcon,
                LOCALIZATIONMANAGER.getLocalizedString("filessetup.writeconfigtofile.includeserverproperties"), includeProperties,
                LOCALIZATIONMANAGER.getLocalizedString("filessetup.writeconfigtofile.includestartscripts"), includeScripts,
                LOCALIZATIONMANAGER.getLocalizedString("filessetup.writeconfigtofile.includezipcreation"), includeZip
        );

        if (!isTemporary) {
            if (getConfigFile().exists()) {
                boolean delConf = getConfigFile().delete();
                if (delConf) {
                    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("filessetup.log.info.writeconfigtofile.config"));
                } else {
                    LOG.error(LOCALIZATIONMANAGER.getLocalizedString("filessetup.log.error.writeconfigtofile.config"));
                }
            }
            if (getOldConfigFile().exists()) {
                boolean delOldConf = getOldConfigFile().delete();
                if (delOldConf) {
                    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("filessetup.log.info.writeconfigtofile.old"));
                } else {
                    LOG.error(LOCALIZATIONMANAGER.getLocalizedString("filessetup.log.error.writeconfigtofile.old"));
                }
            }
        }

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
            writer.write(configString);
            writer.close();
            configWritten = true;
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("filessetup.log.info.writeconfigtofile.confignew"));
        } catch (IOException ex) {
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("filessetup.log.error.writeconfigtofile"), ex);
        }

        return configWritten;
    }
}