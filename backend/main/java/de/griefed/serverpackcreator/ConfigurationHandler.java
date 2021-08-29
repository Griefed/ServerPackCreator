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
import de.griefed.serverpackcreator.curseforge.CurseCreateModpack;
import de.griefed.serverpackcreator.curseforge.CurseModpack;
import de.griefed.serverpackcreator.i18n.LocalizationManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * <strong>Table of methods</strong><p>
 * 1. {@link #ConfigurationHandler(LocalizationManager, CurseCreateModpack, Properties)}<br>
 * 2. {@link #getOldConfigFile()}<br>
 * 3. {@link #getConfigFile()}<br>
 * 4. {@link #getConfig()}<br>
 * 5. {@link #setConfig(File)}<br>
 * 6. {@link #getFallbackModsList()}<br>
 * 7. {@link #getProjectID()}<br>
 * 8. {@link #setProjectID(int)}<br>
 * 9. {@link #getProjectFileID()}<br>
 * 10.{@link #setProjectFileID(int)}<br>
 * 11.{@link #checkConfiguration(File, boolean, ConfigurationModel)}<br>
 * 12.{@link #isDir(String, ConfigurationModel)}<br>
 * 13.{@link #isCurse(ConfigurationModel)}<br>
 * 14.{@link #containsFabric(CurseModpack)}<br>
 * 15.{@link #suggestCopyDirs(String)}<br>
 * 16.{@link #checkCurseForge(String)}<br>
 * 17.{@link #convertToBoolean(String)}<br>
 * 18.{@link #printConfig(String, List, List, boolean, String, String, String, String, boolean, boolean, boolean, boolean, String)}<br>
 * 19.{@link #checkModpackDir(String)}<br>
 * 20.{@link #checkCopyDirs(List, String)}<br>
 * 21.{@link #checkJavaPath(String)}<br>
 * 22.{@link #checkModloader(String)}<br>
 * 23.{@link #setModLoaderCase(String)}<br>
 * 24.{@link #checkModloaderVersion(String, String)}<br>
 * 25.{@link #isMinecraftVersionCorrect(String)}<br>
 * 26.{@link #isFabricVersionCorrect(String)}<br>
 * 27.{@link #isForgeVersionCorrect(String)}<br>
 * 28.{@link #latestFabricLoader()}<br>
 * 29.{@link #createConfigurationFile()}<br>
 * 30.{@link #readStringArray()}<br>
 * 31.{@link #buildString(String...)}<br>
 * 32.{@link #encapsulateListElements(List)}<br>
 * 33.{@link #readBoolean()}<br>
 * 34.{@link #writeConfigToFile(String, List, List, boolean, String, String, String, String, boolean, boolean, boolean, boolean, String, File, boolean)}<br>
 * 35.{@link #getConfigurationAsList(ConfigurationModel)}
 * <p>
 * Requires an instance of {@link CurseCreateModpack} in order to create a modpack from scratch should the specified modpackDir
 * be a combination of a CurseForge projectID and fileID.<p>
 * Requires an instance of {@link LocalizationManager} for use of localization, but creates one if injected one is null.<p>
 * Loads a configuration from a serverpackcreator.conf-file in the same directory in which ServerPackCreator resides in.
 * @author Griefed
 */
@Component
public class ConfigurationHandler {
    private static final Logger LOG = LogManager.getLogger(ConfigurationHandler.class);

    private final LocalizationManager LOCALIZATIONMANAGER;
    private final CurseCreateModpack CURSECREATEMODPACK;
    private Properties serverpackcreatorproperties;

    /**
     * <strong>Constructor</strong><p>
     * Used for Dependency Injection.<p>
     * Receives an instance of {@link LocalizationManager} or creates one if the received
     * one is null. Required for use of localization.<p>
     * Receives an instance of {@link CurseCreateModpack} in case the modpack has to be created from a combination of
     * CurseForge projectID and fileID, from which to <em>then</em> create the server pack.
     * @author Griefed
     * @param injectedLocalizationManager Instance of {@link LocalizationManager} required for localized log messages.
     * @param injectedCurseCreateModpack Instance of {@link CurseCreateModpack} in case the modpack has to be created from a combination of
     * CurseForge projectID and fileID, from which to <em>then</em> create the server pack.
     * @param injectedServerPackCreatorProperties Instance of {@link Properties} required for various different things.
     */
    @Autowired
    public ConfigurationHandler(LocalizationManager injectedLocalizationManager, CurseCreateModpack injectedCurseCreateModpack, Properties injectedServerPackCreatorProperties) {
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

        this.serverpackcreatorproperties = injectedServerPackCreatorProperties;

        setFALLBACKMODSLIST();

    }

    private final File FILE_CONFIG_OLD = new File("creator.conf");
    private final File FILE_CONFIG = new File("serverpackcreator.conf");

    /**
     * Setter for the fallback modslist in case the users did not specify any clientside-only mods. Reads <code>de.griefed.serverpackcreator.configuration.fallbackmodslist</code>
     * from the serverpackcreator.properties-file and if it doesn't exist in said properties-file, assigns the default value <code>AmbientSounds,BackTools,BetterAdvancement,BetterFoliage,BetterPing,BetterPlacement,Blur,cherished,ClientTweaks,Controlling,CTM,customdiscordrpc,CustomMainMenu,DefaultOptions,durability,DynamicSurroundings,EiraMoticons,FullscreenWindowed,itemzoom,itlt,jeiintegration,jei-professions,just-enough-harvestcraft,JustEnoughResources,keywizard,modnametooltip,MouseTweaks,multihotbar-,Neat,OldJavaWarning,PackMenu,preciseblockplacing,ResourceLoader,SimpleDiscordRichPresence,SpawnerFix,timestamps,TipTheScales,WorldNameRandomizer</code>
     */
    public void setFALLBACKMODSLIST() {
        if (serverpackcreatorproperties.getProperty("de.griefed.serverpackcreator.configuration.fallbackmodslist") == null) {

            this.FALLBACKMODSLIST = new ArrayList<>(Arrays.asList(
                    "AmbientSounds","BackTools","BetterAdvancement","BetterFoliage","BetterPing","BetterPlacement","Blur","cherished",
                            "ClientTweaks","Controlling","CTM","customdiscordrpc","CustomMainMenu","DefaultOptions","durability","DynamicSurroundings",
                            "EiraMoticons","FullscreenWindowed","itemzoom","itlt","jeiintegration","jei-professions","just-enough-harvestcraft",
                            "JustEnoughResources","keywizard","modnametooltip","MouseTweaks","multihotbar-","Neat","OldJavaWarning","PackMenu",
                            "preciseblockplacing","ResourceLoader","SimpleDiscordRichPresence","SpawnerFix","timestamps","TipTheScales",
                            "WorldNameRandomizer"));

            LOG.debug("Fallbackmodslist property null. Using fallback: " + FALLBACKMODSLIST);

        } else if (serverpackcreatorproperties.getProperty("de.griefed.serverpackcreator.configuration.fallbackmodslist").contains(",")) {

            this.FALLBACKMODSLIST = new ArrayList<>(Arrays.asList(serverpackcreatorproperties.getProperty(
                    "de.griefed.serverpackcreator.configuration.fallbackmodslist",
                    "AmbientSounds,BackTools,BetterAdvancement,BetterFoliage,BetterPing,BetterPlacement,Blur,cherished," +
                            "ClientTweaks,Controlling,CTM,customdiscordrpc,CustomMainMenu,DefaultOptions,durability,DynamicSurroundings," +
                            "EiraMoticons,FullscreenWindowed,itemzoom,itlt,jeiintegration,jei-professions,just-enough-harvestcraft," +
                            "JustEnoughResources,keywizard,modnametooltip,MouseTweaks,multihotbar-,Neat,OldJavaWarning,PackMenu," +
                            "preciseblockplacing,ResourceLoader,SimpleDiscordRichPresence,SpawnerFix,timestamps,TipTheScales," +
                            "WorldNameRandomizer").split(",")));

            LOG.debug("Fallbackmodslist set to: " + FALLBACKMODSLIST);

        } else {

            this.FALLBACKMODSLIST = Collections.singletonList((serverpackcreatorproperties.getProperty("de.griefed.serverpackcreator.configuration.fallbackmodslist")));

            LOG.debug("Fallbackmodslist set to: " + FALLBACKMODSLIST);
        }

    }

    /**
     * If you wish to expand this list, open a feature request issue on <a href=https://github.com/Griefed/ServerPackCreator/issues/new/choose>GitHub</a>
     * with the mod(s) you want to see added.
     */
    private List<String> FALLBACKMODSLIST = new ArrayList<>();

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
     * @author Griefed
     * @return File. Returns the creator.conf-file for use in {@link #writeConfigToFile(String, List, List, boolean, String, String, String, String, boolean, boolean, boolean, boolean, String, File, boolean)}
     */
    public File getOldConfigFile() {
        return FILE_CONFIG_OLD;
    }

    /**
     * Getter for serverpackcreator.conf.
     * @author Griefed
     * @return File. Returns the serverpackcreator.conf-file.
     * {@link #writeConfigToFile(String, List, List, boolean, String, String, String, String, boolean, boolean, boolean, boolean, String, File, boolean)}
     */
    public File getConfigFile() {
        return FILE_CONFIG;
    }

    /**
     * Getter for a {@link Config} containing a parsed configuration-file.
     * @author Griefed
     * @return Config. Returns parsed serverpackcreator.conf.
     */
    Config getConfig() {
        return config;
    }

    /**
     * Setter for a {@link Config} containing a parsed configuration-file.
     * @author Griefed
     * @param newConfig The new file of which to store a Typesafe Config.
     */
    void setConfig(File newConfig) {
        try {
            this.config = ConfigFactory.parseFile(newConfig);
        } catch (ConfigException ex) {
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.checkconfig.start"));
        }
    }

    /**
     * Getter for the fallback clientside-only mods-list, in case no customized one is provided by the user.
     * @author Griefed
     * @return List String. Returns the fallback clientside-only mods-list.
     */
    public List<String> getFallbackModsList() {
        return FALLBACKMODSLIST;
    }

    /**
     * Getter for the CurseForge projectID of a modpack, which will be created by {@link CurseCreateModpack#curseForgeModpack(String, Integer, Integer)}.
     * @author Griefed
     * @return Integer. Returns the CurseForge projectID of a modpack, for use in {@link CurseCreateModpack#curseForgeModpack(String, Integer, Integer)} and {@link #checkCurseForge(String)}
     */
    int getProjectID() {
        return projectID;
    }

    /**
     * Setter for the CurseForge projectID of a modpack, which will be created by {@link CurseCreateModpack#curseForgeModpack(String, Integer, Integer)}.
     * For use in {@link #checkCurseForge(String)}
     * @author Griefed
     * @param newProjectID The new projectID to store.
     */
    void setProjectID(int newProjectID) {
        this.projectID = newProjectID;
    }

    /**
     * Getter for the CurseForge file of a modpack, which will be created by {@link CurseCreateModpack#curseForgeModpack(String, Integer, Integer)}.
     * @author Griefed
     * @return Integer. Returns the CurseForge fileID of a modpack.
     */
    int getProjectFileID() {
        return projectFileID;
    }

    /**
     * Setter for the CurseForge file of a modpack, which will be created by {@link CurseCreateModpack#curseForgeModpack(String, Integer, Integer)}.
     * For use in {@link #checkCurseForge(String)}
     * @author Griefed
     * @param newProjectFileID The new projectFileID to store.
     */
    void setProjectFileID(int newProjectFileID) {
        this.projectFileID = newProjectFileID;
    }

    /**
     * Sets {@link #setConfig(File)} and calls checks for the provided configuration-file. If any check returns <code>true</code>
     * then the server pack will not be created. In order to find out which check failed, the user has to check their
     * serverpackcreator.log in the logs-directory.
     * @author Griefed
     * @param configFile File. The configuration file to check. Must be a valid configuration file for serverpackcreator to work.
     * @param shouldModpackBeCreated Boolean. Whether the CurseForge modpack should be downloaded and created.
     * @param configurationModel ConfigurationModel. Instance of a configuration of a modpack.
     * @return Boolean. Returns <code>false</code> if all checks are passed.
     */
    public boolean checkConfiguration(File configFile, boolean shouldModpackBeCreated, ConfigurationModel configurationModel) {
        boolean configHasError = false;

        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.checkconfig.start"));

        try {
            setConfig(configFile);
        } catch (ConfigException ex) {
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.checkconfig.start"));
        }

        if (getConfig().getStringList("clientMods").isEmpty()) {
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.warn.checkconfig.clientmods"));
            configurationModel.setClientMods(getFallbackModsList());
        } else {
            configurationModel.setClientMods(getConfig().getStringList("clientMods"));
        }

        configurationModel.setJavaPath(checkJavaPath(getConfig().getString("javaPath").replace("\\", "/")));

        configurationModel.setIncludeServerInstallation(convertToBoolean(getConfig().getString("includeServerInstallation")));

        configurationModel.setIncludeServerIcon(convertToBoolean(getConfig().getString("includeServerIcon")));

        configurationModel.setIncludeServerProperties(convertToBoolean(getConfig().getString("includeServerProperties")));

        configurationModel.setIncludeStartScripts(convertToBoolean(getConfig().getString("includeStartScripts")));

        configurationModel.setIncludeZipCreation(convertToBoolean(getConfig().getString("includeZipCreation")));

        try {
            configurationModel.setJavaArgs(getConfig().getString("javaArgs"));

        } catch (ConfigException | NullPointerException ex) {

            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.javaargs.notfound"));
            configurationModel.setJavaArgs("empty");
        }

        if (checkModpackDir(getConfig().getString("modpackDir").replace("\\","/"))) {

            configHasError = isDir(getConfig().getString("modpackDir").replace("\\","/"), configurationModel);

        } else if (checkCurseForge(getConfig().getString("modpackDir"))) {

            if (shouldModpackBeCreated) {

                configHasError = isCurse(configurationModel);

            } else {
                /* This log is meant to be read by the user, therefore we allow translation. */
                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.checkconfig.skipmodpackcreation"));
            }

        } else {
            configHasError = true;
        }

        printConfig(configurationModel.getModpackDir(),
                configurationModel.getClientMods(),
                configurationModel.getCopyDirs(),
                configurationModel.getIncludeServerInstallation(),
                configurationModel.getJavaPath(),
                configurationModel.getMinecraftVersion(),
                configurationModel.getModLoader(),
                configurationModel.getModLoaderVersion(),
                configurationModel.getIncludeServerIcon(),
                configurationModel.getIncludeServerProperties(),
                configurationModel.getIncludeStartScripts(),
                configurationModel.getIncludeZipCreation(),
                configurationModel.getJavaArgs());

        if (!configHasError) {
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.checkconfig.success"));
        } else {
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.checkconfig.failure"));
        }

        return configHasError;
    }

    /**
     * Check the passed {@link ConfigurationModel}. If any check returns <code>true</code>
     * then the server pack will not be created. In order to find out which check failed, the user has to check their
     * serverpackcreator.log in the logs-directory.
     * @author Griefed
     * @param shouldModpackBeCreated Boolean. Whether the CurseForge modpack should be downloaded and created.
     * @param configurationModel ConfigurationModel. Instance of a configuration of a modpack.
     * @return Boolean. Returns <code>false</code> if all checks are passed.
     */
    public boolean checkConfiguration(boolean shouldModpackBeCreated, ConfigurationModel configurationModel) {
        boolean configHasError = false;

        /* This log is meant to be read by the user, therefore we allow translation. */
        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.checkconfig.start"));

        if (configurationModel.getClientMods().isEmpty()) {
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.warn.checkconfig.clientmods"));
            configurationModel.setClientMods(getFallbackModsList());
        } else {
            configurationModel.setClientMods(configurationModel.getClientMods());
        }

        configurationModel.setJavaPath(checkJavaPath(configurationModel.getJavaPath().replace("\\", "/")));

        if (checkModpackDir(configurationModel.getModpackDir().replace("\\","/"))) {

            configHasError = isDir(configurationModel);

        } else if (checkCurseForge(configurationModel.getModpackDir())) {

            if (shouldModpackBeCreated) {

                configHasError = isCurse(configurationModel);

            } else {
                /* This log is meant to be read by the user, therefore we allow translation. */
                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.checkconfig.skipmodpackcreation"));
            }

        } else {
            configHasError = true;
        }

        printConfig(configurationModel.getModpackDir(),
                configurationModel.getClientMods(),
                configurationModel.getCopyDirs(),
                configurationModel.getIncludeServerInstallation(),
                configurationModel.getJavaPath(),
                configurationModel.getMinecraftVersion(),
                configurationModel.getModLoader(),
                configurationModel.getModLoaderVersion(),
                configurationModel.getIncludeServerIcon(),
                configurationModel.getIncludeServerProperties(),
                configurationModel.getIncludeStartScripts(),
                configurationModel.getIncludeZipCreation(),
                configurationModel.getJavaArgs());

        if (!configHasError) {
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.checkconfig.success"));
        } else {
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.checkconfig.failure"));
        }

        return configHasError;
    }

    /**
     * If the in the configuration specified modpack dir is an existing directory, checks are made for valid configuration
     * of: directories to copy to server pack,<br>
     * if includeServerInstallation is <code>true</code>) path to Java executable/binary, Minecraft version, modloader and modloader version.
     * @author Griefed
     * @param modpackDir String. Should an existing modpack be specified, all configurations are read from the provided
     *                   configuration file and checks are made in this directory.
     * @param configurationModel An instance of {@link ConfigurationModel} which contains the configuration of the modpack.
     * @return Boolean. Returns true if an error is found during configuration check.
     */
    boolean isDir(String modpackDir, ConfigurationModel configurationModel) {
        boolean configHasError = false;
        configurationModel.setModpackDir(modpackDir);

        if (checkCopyDirs(getConfig().getStringList("copyDirs"), configurationModel.getModpackDir())) {

            configurationModel.setCopyDirs(getConfig().getStringList("copyDirs"));
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.debug(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.debug.isdir.copydirs"));

        } else {
            configHasError = true;
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.isdir.copydir"));
        }

        if (isMinecraftVersionCorrect(getConfig().getString("minecraftVersion"))) {

            configurationModel.setMinecraftVersion(getConfig().getString("minecraftVersion"));
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.debug(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.debug.isdir.minecraftversion"));

        } else {
            configHasError = true;
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.isdir.minecraftversion"));
        }

        if (checkModloader(getConfig().getString("modLoader"))) {

            configurationModel.setModLoader(setModLoaderCase(getConfig().getString("modLoader")));
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.debug(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.debug.isdir.modloader"));

            if (checkModloaderVersion(configurationModel.getModLoader(), getConfig().getString("modLoaderVersion"))) {

                configurationModel.setModLoaderVersion(getConfig().getString("modLoaderVersion"));
                /* This log is meant to be read by the user, therefore we allow translation. */
                LOG.debug(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.debug.isdir.modloaderversion"));

            } else {
                configHasError = true;
                /* This log is meant to be read by the user, therefore we allow translation. */
                LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.isdir.modloaderversion"));
            }

        } else {
            configHasError = true;
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.isdir.modloader"));
        }

        return configHasError;
    }

    /**
     * If the in the configuration specified modpack dir is an existing directory, checks are made for valid configuration
     * of: directories to copy to server pack,<br>
     * if includeServerInstallation is <code>true</code>) path to Java executable/binary, Minecraft version, modloader and modloader version.
     * @author Griefed
     * @param configurationModel An instance of {@link ConfigurationModel} which contains the configuration of the modpack.
     * @return Boolean. Returns true if an error is found during configuration check.
     */
    boolean isDir(ConfigurationModel configurationModel) {
        boolean configHasError = false;

        if (checkCopyDirs(configurationModel.getCopyDirs(), configurationModel.getModpackDir())) {

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.debug(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.debug.isdir.copydirs"));

        } else {
            configHasError = true;
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.isdir.copydir"));
        }

        if (isMinecraftVersionCorrect(configurationModel.getMinecraftVersion())) {

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.debug(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.debug.isdir.minecraftversion"));

        } else {
            configHasError = true;
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.isdir.minecraftversion"));
        }

        if (checkModloader(configurationModel.getModLoader())) {

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.debug(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.debug.isdir.modloader"));

            if (checkModloaderVersion(configurationModel.getModLoader(), configurationModel.getModLoaderVersion())) {

                /* This log is meant to be read by the user, therefore we allow translation. */
                LOG.debug(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.debug.isdir.modloaderversion"));

            } else {
                configHasError = true;
                /* This log is meant to be read by the user, therefore we allow translation. */
                LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.isdir.modloaderversion"));
            }

        } else {
            configHasError = true;
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.isdir.modloader"));
        }

        return configHasError;
    }

    /**
     * If modpackDir in the configuration file is a CurseForge projectID,fileID combination, then the modpack is first
     * created from said combination, using {@link CurseCreateModpack#curseForgeModpack(String, Integer, Integer)},
     * before proceeding to checking the rest of the configuration. If everything passes and the modpack was created,
     * a new configuration file is created, replacing the one used to create the modpack in the first place, with the
     * modpackDir field pointing to the newly created modpack.
     * @author Griefed
     * @param configurationModel An instance of {@link ConfigurationModel} which contains the configuration of the modpack.
     * @return Boolean. Returns false unless an error was encountered during either the acquisition of the CurseForge
     * project name and displayname, or when the creation of the modpack fails.
     */
    boolean isCurse(ConfigurationModel configurationModel) {
        boolean configHasError = false;
        try {
            if (CurseAPI.project(getProjectID()).isPresent()) {

                String projectName, displayName;
                projectName = displayName = "";

                try {
                    projectName = CurseAPI.project(getProjectID()).get().name();

                    try {
                        displayName = Objects.requireNonNull(CurseAPI.project(getProjectID()).get().files().fileWithID(getProjectFileID())).displayName();

                    } catch (NullPointerException npe) {
                        /* This log is meant to be read by the user, therefore we allow translation. */
                        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.iscurse.display"));

                        try {
                            displayName = Objects.requireNonNull(CurseAPI.project(getProjectID()).get().files().fileWithID(getProjectFileID())).nameOnDisk();

                        } catch (NullPointerException npe2) {

                            /* This log is meant to be read by the user, therefore we allow translation. */
                            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.iscurse.file"), npe2);
                            displayName = null;
                        }
                    }

                } catch (CurseException ex) {
                    /* This log is meant to be read by the user, therefore we allow translation. */
                    LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.iscurse.curseforge"), ex);
                }

                if (displayName != null && projectName != null) {

                    configurationModel.setModpackDir(String.format("./%s/%s", projectName, displayName));

                    if (CURSECREATEMODPACK.curseForgeModpack(configurationModel.getModpackDir(), getProjectID(), getProjectFileID())) {
                        try {
                            byte[] jsonData = Files.readAllBytes(Paths.get(String.format("%s/manifest.json", configurationModel.getModpackDir())));

                            CurseModpack modpack = getObjectMapper().readValue(jsonData, CurseModpack.class);

                            String[] minecraftLoaderVersions = modpack
                                    .getMinecraft()
                                    .toString()
                                    .split(",");

                            String[] modLoaderVersion = minecraftLoaderVersions[1]
                                    .replace("[", "")
                                    .replace("]", "")
                                    .split("-");

                            configurationModel.setMinecraftVersion(minecraftLoaderVersions[0]
                                    .replace("[", ""));

                            if (containsFabric(modpack)) {
                                /* This log is meant to be read by the user, therefore we allow translation. */
                                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.iscurse.fabric"));
                                LOG.debug("Setting modloader to Fabric.");

                                configurationModel.setModLoader("Fabric");
                                configurationModel.setModLoaderVersion(latestFabricLoader());

                            } else {
                                /* This log is meant to be read by the user, therefore we allow translation. */
                                LOG.debug("Setting modloader to Forge.");

                                configurationModel.setModLoader("Forge");
                                configurationModel.setModLoaderVersion(modLoaderVersion[1]);

                            }
                        } catch (IOException ex) {
                            LOG.error("Error: There was a fault during json parsing.", ex);
                        }

                        configurationModel.setCopyDirs(suggestCopyDirs(configurationModel.getModpackDir()));

                        /* This log is meant to be read by the user, therefore we allow translation. */
                        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.iscurse.replace"));

                        writeConfigToFile(
                                configurationModel.getModpackDir(),
                                configurationModel.getClientMods(),
                                configurationModel.getCopyDirs(),
                                configurationModel.getIncludeServerInstallation(),
                                configurationModel.getJavaPath(),
                                configurationModel.getMinecraftVersion(),
                                configurationModel.getModLoader(),
                                configurationModel.getModLoaderVersion(),
                                configurationModel.getIncludeServerIcon(),
                                configurationModel.getIncludeServerProperties(),
                                configurationModel.getIncludeStartScripts(),
                                configurationModel.getIncludeZipCreation(),
                                configurationModel.getJavaArgs(),
                                getConfigFile(),
                                false
                        );

                    } else {
                        /* This log is meant to be read by the user, therefore we allow translation. */
                        LOG.error(LOCALIZATIONMANAGER.getLocalizedString("cursecreatemodpack.log.error.create"));
                        configHasError = true;
                    }

                } else {
                    /* This log is meant to be read by the user, therefore we allow translation. */
                    LOG.error(LOCALIZATIONMANAGER.getLocalizedString("cursecreatemodpack.log.error.ids"));
                    configHasError = true;
                }

            } else {
                /* This log is meant to be read by the user, therefore we allow translation. */
                LOG.error(LOCALIZATIONMANAGER.getLocalizedString("cursecreatemodpack.log.error.notfound"));
                configHasError = true;
            }

        } catch (CurseException | IllegalArgumentException ex) {
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.iscurse.project"), getProjectID()), ex);
            configHasError = true;
        }
        return configHasError;
    }

    /**
     * Checks whether the projectID for the Jumploader mod is present in the list of mods required by the CurseForge modpack.
     * If Jumploader is found, the modloader for the new configuration-file will be set to Fabric.
     * @author Griefed
     * @param modpack CurseModpack. Contains information about the CurseForge modpack. Used to get a list of all projects
     *               required by the modpack.
     * @return Boolean. Returns true if Jumploader is found.
     */
    boolean containsFabric(CurseModpack modpack) {
        boolean hasJumploader = false;

        for (int i = 0; i < modpack.getFiles().size(); i++) {

            String[] mods = modpack.getFiles().get(i).toString().split(",");

            LOG.debug(String.format("Mod ID: %s", mods[0]));
            LOG.debug(String.format("File ID: %s", mods[1]));

            if (mods[0].equalsIgnoreCase("361988") || mods[0].equalsIgnoreCase("306612")) {

                /* This log is meant to be read by the user, therefore we allow translation. */
                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.containsfabric"));
                hasJumploader = true;
            }
        }
        
        return hasJumploader;
    }

    /**
     * Creates a list of suggested directories to include in server pack which is later on written to a new configuration file.
     * The list of directories to include in the server pack which is generated by this method excludes well know directories
     * which would not be needed by a server pack. If you have suggestions to this list, open a feature request issue on
     * <a href=https://github.com/Griefed/ServerPackCreator/issues/new/choose>GitHub</a>
     * @author Griefed
     * @param modpackDir String. The directory for which to gather a list of directories to copy to the server pack.
     * @return List, String. Returns a list of directories inside the modpack, excluding well known client-side only
     * directories.
     */
    List<String> suggestCopyDirs(String modpackDir) {
        /* This log is meant to be read by the user, therefore we allow translation. */
        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.suggestcopydirs.start"));

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
            LOG.error("Error: Something went wrong during the setup of the modpack. Copy dirs should never be empty. Please check the logs for errors and open an issue on https://github.com/Griefed/ServerPackCreator/issues.", np);
        }

        for (int idirs = 0; idirs < dirsNotToCopy.size(); idirs++) {

            int i = idirs;

            dirsInModpack.removeIf(n -> (n.contains(dirsNotToCopy.get(i))));
        }

        LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.suggestcopydirs.list"),dirsInModpack));

        return dirsInModpack;
    }

    /**
     * Checks whether the specified modpack directory contains a valid projectID,fileID combination.
     * ProjectIDs must be at least two digits long, fileIDs must be at least 5 digits long.
     * Must be numbers separated by a ",". If modpackDir successfully matched a projectID,fileID combination, CurseForge
     * is then checked for existence of said projectID and fileID. If the project can not be found or the file returns null
     * then false is returned and the check is considered failed.
     * @author Griefed
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

            } catch (CurseException | NoSuchElementException ex) {
                /* This log is meant to be read by the user, therefore we allow translation. */
                LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.iscurse.project"), curseProjectID), ex);
                configCorrect = false;
            }

            try {

                if (CurseAPI.project(curseProjectID).get().files().fileWithID(curseFileID) != null) {
                    setProjectFileID(curseFileID);
                    configCorrect = true;
                }

            } catch (CurseException | NoSuchElementException ex) {
                /* This log is meant to be read by the user, therefore we allow translation. */
                LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.iscurse.file"), curseFileID), ex);
                configCorrect = false;
            }

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.checkcurseforge.info"));
            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.checkcurseforge.return"), getProjectID(), getProjectFileID()));
            LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.warn.checkcurseforge.warn"));


        } else {
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.warn.checkcurseforge.warn2"));
        }

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
     * @author Griefed
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
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.warn.converttoboolean.warn"));
            returnBoolean = false;
        }

        return returnBoolean;
    }

    /**
     * Prints all passed fields to the console and serverpackcreator.log. Used to show the user the configuration before
     * ServerPackCreator starts the generation of the server pack or, if checks failed, to show the user their last
     * configuration, so they can more easily identify problems with said configuration.<br>
     * Should a user report an issue on GitHub and include their logs (which I hope they do....), this would also
     * help me help them. Logging is good. People should use more logging.
     * @author Griefed
     * @param modpackDirectory String. The used modpackDir field either from a configuration file or from configuration setup.
     * @param clientsideMods String List. List of clientside-only mods to exclude from the server pack...
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
     * @param javaArgs String. Java arguments to write the start-scripts with.
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
                     boolean includeZip,
                     String javaArgs) {

        /* This log is meant to be read by the user, therefore we allow translation. */
        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.printconfig.start"));
        LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.printconfig.modpackdir"), modpackDirectory));

        if (clientsideMods.isEmpty()) {
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.warn.printconfig.noclientmods"));
        } else {

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.printconfig.clientmods"));
            for (String mod : clientsideMods) {
                LOG.info(String.format("    %s", mod));
            }

        }

        /* This log is meant to be read by the user, therefore we allow translation. */
        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.printconfig.copydirs"));

        if (copyDirectories != null) {

            for (String directory : copyDirectories) {
                LOG.info(String.format("    %s", directory));
            }

        } else {
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.printconfig.copydirs"));
        }

        /* This log is meant to be read by the user, therefore we allow translation. */
        LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.printconfig.server"), installServer));
        LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.printconfig.javapath"), javaInstallPath));
        LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.printconfig.minecraftversion"), minecraftVer));
        LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.printconfig.modloader"), modloader));
        LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.printconfig.modloaderversion"), modloaderVersion));
        LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.printconfig.icon"), includeIcon));
        LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.printconfig.properties"), includeProperties));
        LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.printconfig.scripts"), includeScripts));
        LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.printconfig.zip"), includeZip));
        LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.printconfig.javaargs"), javaArgs));
    }

    /**
     * Checks whether the passed String is empty and if it is empty, prints the corresponding message to the console and
     * serverpackcreator.log so the user knows what went wrong.<br>
     * Checks whether the passed String is a directory and if it is not, prints the corresponding message to the console
     * and serverpackcreator.log so the user knows what went wrong.
     * @author Griefed
     * @param modpackDir String. The path to the modpack directory to check whether it is empty and whether it is a directory.
     * @return Boolean. Returns true if the directory exists.
     */
    boolean checkModpackDir(String modpackDir) {
        boolean configCorrect = false;

        if (modpackDir.equals("")) {

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.checkmodpackdir"));

        } else if (!(new File(modpackDir).isDirectory())) {

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.warn(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.warn.checkmodpackdir"), modpackDir));

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
     * If the user specified a <code>source/file;destination/file</code>-combination, it is checked whether the specified
     * source-file exists on the host.
     * @author Griefed
     * @param directoriesToCopy List String. The list of directories, or <code>source/file;destination/file</code>-combinations,
     *                         to check for existence. <code>source/file;destination/file</code>-combinations must be
     *                          absolute paths to the source-file.
     * @param modpackDir String. The path to the modpack directory in which to check for existence of the passed list of
     *                  directories.
     * @return Boolean. Returns true if every directory was found in the modpack directory. If any single one was not found,
     * false is returned.
     */
    boolean checkCopyDirs(List<String> directoriesToCopy, String modpackDir) {
        boolean configCorrect = true;

        if (directoriesToCopy.isEmpty()) {

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.checkcopydirs.empty"));
            configCorrect = false;

        } else {

            for (String directory : directoriesToCopy) {

                // Check whether the user explicitly specified a file to include in the server pack.
                if (directory.contains(";")) {

                    String[] sourceFileDestinationFileCombination = directory.split(";");

                    File sourceFileToCheck = new File (String.format("%s/%s", modpackDir,sourceFileDestinationFileCombination[0]));

                    if (!sourceFileToCheck.exists()) {

                        /* This log is meant to be read by the user, therefore we allow translation. */
                        LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.checkcopydirs.filenotfound"), sourceFileToCheck));
                        configCorrect = false;
                    }

                // If user did not explicitly specify a file, check for directory.
                } else {

                    File dirToCheck = new File(String.format("%s/%s", modpackDir, directory));

                    if (!dirToCheck.exists() || !dirToCheck.isDirectory()) {

                        /* This log is meant to be read by the user, therefore we allow translation. */
                        LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.checkcopydirs.notfound"), dirToCheck.getAbsolutePath()));
                        configCorrect = false;
                    }

                }
            }
        }
        return configCorrect;
    }

    /**
     * Checks whether the passed String ends with <code>java.exe</code> or <code>java</code> and whether the files exist.
     * @author Griefed
     * @param pathToJava String. The path to check for java.exe and java.
     * @return String. Returns the path to the java installation. If user input was incorrect, SPC will try to acquire the path automatically.
     */
    public String checkJavaPath(String pathToJava) {
        String checkedJavaPath = null;

        try {
            if (new File(pathToJava).exists() && pathToJava.endsWith("java.exe")) {

                checkedJavaPath = pathToJava;

            } else if (new File(pathToJava).exists() && pathToJava.endsWith("java")) {

                checkedJavaPath = pathToJava;

            } else if (!new File(pathToJava).exists() && new File(pathToJava + ".exe").exists()) {

                checkedJavaPath = pathToJava + ".exe";
                /* This log is meant to be read by the user, therefore we allow translation. */
                LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.checkjavapath.windows"));

            } else {
                LOG.debug("Acquiring path to Java installation from system properties...");
                checkedJavaPath = String.format("%s/bin/java",System.getProperty("java.home").replace("\\", "/"));

                if (checkedJavaPath.startsWith("C:")) {

                    LOG.debug("We're running on Windows. Ensuring javaPath ends with .exe");
                    checkedJavaPath = String.format("%s.exe", checkedJavaPath);
                }

                LOG.debug("Automatically acquired path to Java installation: " + checkedJavaPath);
            }
        } catch (NullPointerException ignored) {
            checkedJavaPath = String.format("%s/bin/java",System.getProperty("java.home").replace("\\", "/"));

            if (checkedJavaPath.startsWith("C:")) {

                LOG.debug("We're running on Windows. Ensuring javaPath ends with .exe.");
                checkedJavaPath = String.format("%s.exe", checkedJavaPath);
            }
        } finally {
            return checkedJavaPath;
        }
    }

    /**
     * Checks whether either Forge or Fabric were specified as the modloader.
     * @author Griefed
     * @param modloader String. Check as case-insensitive for Forge or Fabric.
     * @return Boolean. Returns true if the specified modloader is either Forge or Fabric. False if neither.
     */
    boolean checkModloader(String modloader) {
        boolean configCorrect = false;

        if (modloader.toLowerCase().contains("forge") || modloader.toLowerCase().contains("fabric")) {

            configCorrect = true;

        } else {

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.checkmodloader"));
        }
        return configCorrect;
    }

    /**
     * Ensures the modloader is normalized to first letter upper case and rest lower case. Basically allows the user to
     * input Forge or Fabric in any combination of upper- and lowercase and ServerPackCreator will still be able to
     * work with the users input.
     * @author Griefed
     * @param modloader String. The String to check for case-insensitive cases of either Forge or Fabric.
     * @return String. Returns a normalized String of the specified modloader.
     */
    public String setModLoaderCase(String modloader) {
        String returnLoader = null;

        if (modloader.equalsIgnoreCase("Forge")) {

            returnLoader = "Forge";

        } else if (modloader.equalsIgnoreCase("Fabric")) {

            returnLoader = "Fabric";

        } else if (modloader.toLowerCase().contains("forge")) {

            returnLoader = "Forge";

        } else if (modloader.toLowerCase().contains("fabric")) {

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
     * @author Griefed
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
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.checkmodloaderversion"));
        }
        return isVersionCorrect;
    }

    /**
     * Checks whether the passed String is empty and if it is not. check the String against Mojang's version manifest
     * to validate the version.
     * @author whitebear60
     * @param minecraftVersion String. The version to check for in Mojang's version manifest.
     * @return Boolean. Returns true if the specified Minecraft version could be found in Mojang's manifest.
     */
    boolean isMinecraftVersionCorrect(String minecraftVersion) {
        if (!minecraftVersion.equals("")) {
            try {

                File manifestJsonFile = new File("./work/minecraft-manifest.json");

                Scanner jsonReader = new Scanner(manifestJsonFile);

                String jsonData = jsonReader.nextLine();

                jsonReader.close();

                jsonData = jsonData.replaceAll("\\s", "");

                return jsonData.trim().contains(String.format("\"id\":\"%s\"", minecraftVersion));

            } catch (Exception ex) {

                /* This log is meant to be read by the user, therefore we allow translation. */
                LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.isminecraftversioncorrect.validate"), minecraftVersion), ex);
                return false;
            }
        } else {
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.isminecraftversioncorrect.empty"));
            return false;
        }
    }

    /**
     * Checks whether the passed String is empty and if it is not. check the String against Fabric's version manifest
     * to validate the version.
     * @author whitebear60
     * @param fabricVersion String. The version to check for in Fabric's version manifest.
     * @return Boolean. Returns true if the specified fabric version could be found in Fabric's manifest.
     */
    boolean isFabricVersionCorrect(String fabricVersion) {
        if (!fabricVersion.equals("")) {
            try {

                File manifestXMLFile = new File("./work/fabric-manifest.xml");

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

                return manifestXML.trim().contains(fabricVersion);

            } catch (Exception ex) {
                LOG.error("An error occurred during Fabric version validation.", ex);
                return false;
            }

        } else {
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.isfabricversioncorrect.empty"));
            return false;
        }
    }

    /**
     * Checks whether the passed String is empty and if it is not. check the String against Forge's version manifest
     * to validate the version.
     * @author whitebear60
     * @param forgeVersion String. The version to check for in Forge's version manifest.
     * @return Boolean. Returns true if the specified Forge version could be found in Forge's manifest.
     */
    boolean isForgeVersionCorrect(String forgeVersion) {
        if (!forgeVersion.equals("")) {
            try {

                File manifestJsonFile = new File("./work/forge-manifest.json");

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

                return manifestJSON.trim().contains(forgeVersion);

            } catch (Exception ex) {

                LOG.error("An error occurred during Forge version validation.", ex);
                return false;
            }
        } else {

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.isforgeversioncorrect.empty"));
            return false;
        }
    }

    /**
     * Returns the latest version for the Fabric-loader. If Fabric's version manifest should be unreachable for whatever
     * reason, version 0.11.3 is returned by default.
     * @author whitebear60
     * @return Boolean. Returns true if the download was successful. False if not.
     */
    String latestFabricLoader() {
        String result;
        try {

            DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();

            DocumentBuilder builder = domFactory.newDocumentBuilder();

            Document fabricXml = builder.parse(new File("./work/fabric-manifest.xml"));

            XPathFactory xPathFactory = XPathFactory.newInstance();

            XPath xpath = xPathFactory.newXPath();

            result = (String) xpath.evaluate("/metadata/versioning/release", fabricXml, XPathConstants.STRING);

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.info.latestfabricloader.created"));

        } catch (IOException | ParserConfigurationException | SAXException | XPathExpressionException ex) {
            result = "0.11.6";
            LOG.error("LOCALIZATIONMANAGER.getLocalizedString(\"configuration.log.error.latestfabricloader.parse\")", ex);
        }

        return result;

    }

    /**
     * Walk the user through the generation of a new ServerPackCreator configuration file by asking them for input,
     * step-by-step, regarding their modpack. At the end of this method a fully configured serverpackcreator.conf file
     * is saved and any previously existing configuration file replaced by the new one.<br>
     * After every input, said input is displayed to the user, and they're asked whether they are satisfied with said
     * input. The user can then decide whether they would like to restart the entry of the field they just configured,
     * or agree and move to the next one.<br>
     * At the end of this method, the user will have a newly configured and created configuration file for ServerPackCreator.<br>
     * <br>
     * Most user-input is checked after entry to ensure the configuration is already in working-condition after completion
     * of this method.
     * @author whitebear60
     * @author Griefed
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
                tmpModpackDir,
                javaArgs;

        Scanner reader = new Scanner(System.in);

        /* This log is meant to be read by the user, therefore we allow translation. */
        LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.start"), "-cgen"));
        do {
//--------------------------------------------------------------------------------------------MODPACK DIRECTORY---------
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.modpack.enter"));
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.modpack.example"));

            do {

                do {
                    System.out.print(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.modpack.cli") + " ");
                    tmpModpackDir = reader.nextLine();
                } while (!checkModpackDir(tmpModpackDir));

                /* This log is meant to be read by the user, therefore we allow translation. */
                LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.checkreturn"), tmpModpackDir));
                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.modpack.checkreturninfo"));

                System.out.print(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.answer") + " ");

            } while (!readBoolean());

            modpackDir = tmpModpackDir.replace("\\", "/");
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.checkreturn"), modpackDir));
            System.out.println();

//-----------------------------------------------------------------------------------------CLIENTSIDE-ONLY MODS---------
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.clientmods.enter"));
            do {
                clientMods.clear();

                clientMods.addAll(readStringArray());

                /* This log is meant to be read by the user, therefore we allow translation. */
                LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.checkreturn"), clientMods));

                if (clientMods.isEmpty()) {
                    clientMods = getFallbackModsList();

                    /* This log is meant to be read by the user, therefore we allow translation. */
                    LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.warn.checkconfig.clientmods"));

                    for (String mod : clientMods) {
                        LOG.warn(String.format("    %s", mod));
                    }
                }

                /* This log is meant to be read by the user, therefore we allow translation. */
                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.clientmods.checkreturninfo"));

                System.out.print(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.answer") + " ");

            } while (!readBoolean());

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.checkreturn"), clientMods));

            tmpClientMods = new String[clientMods.size()];
            clientMods.toArray(tmpClientMods);

            System.out.println();

//------------------------------------------------------------------DIRECTORIES OR FILES TO COPY TO SERVER PACK---------
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.copydirs.enter"));

            List<String> dirList = Arrays.asList(Objects.requireNonNull(new File(modpackDir).list((current, name) -> new File(current, name).isDirectory())));

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.copydirs.dirsinmodpack"), dirList.toString().replace("[","").replace("]","")));
            do {
                do {
                    copyDirs.clear();
                    /* This log is meant to be read by the user, therefore we allow translation. */
                    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.copydirs.specify"));
                    copyDirs.addAll(readStringArray());

                } while (!checkCopyDirs(copyDirs, modpackDir));

                /* This log is meant to be read by the user, therefore we allow translation. */
                LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.checkreturn"), copyDirs));
                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.copydirs.checkreturninfo"));

                System.out.print(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.answer") + " ");

            } while (!readBoolean());

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.checkreturn"), copyDirs));

            tmpCopyDirs = new String[copyDirs.size()];
            copyDirs.toArray(tmpCopyDirs);

            System.out.println();

//-------------------------------------------------------------WHETHER TO INCLUDE MODLOADER SERVER INSTALLATION---------
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.server.enter"));

            System.out.print(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.server.include") + " ");
            includeServerInstallation = readBoolean();

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.checkreturn"), includeServerInstallation));

//-------------------------------------------------------------------------------MINECRAFT VERSION MODPACK USES---------
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.minecraft.enter"));

            do {
                System.out.print(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.minecraft.specify") + " ");
                minecraftVersion = reader.nextLine();

            } while (!isMinecraftVersionCorrect(minecraftVersion));

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.checkreturn"), minecraftVersion));
            System.out.println();

//---------------------------------------------------------------------------------------MODLOADER MODPACK USES---------
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.modloader.enter"));

            do {
                System.out.print(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.modloader.cli") + " ");
                modLoader = reader.nextLine();

            } while (!checkModloader(modLoader));

            modLoader = setModLoaderCase(modLoader);

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.checkreturn"), modLoader));
            System.out.println();

//----------------------------------------------------------------------------VERSION OF MODLOADER MODPACK USES---------
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.modloaderversion.enter"), modLoader));

            do {
                System.out.print(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.modloaderversion.cli") + " ");
                modLoaderVersion = reader.nextLine();

            } while (!checkModloaderVersion(modLoader, modLoaderVersion));

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.checkreturn"), modLoaderVersion));
            System.out.println();

//------------------------------------------------------------------------------------PATH TO JAVA INSTALLATION---------
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.java.enter"));
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.java.enter2"));
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.java.example"));

            System.out.print(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.java.cli") + " ");

            javaPath = checkJavaPath(reader.nextLine());

            System.out.println(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.warn.getjavapath.set"), javaPath));



            System.out.println();

//------------------------------------------------------------WHETHER TO INCLUDE SERVER-ICON.PNG IN SERVER PACK---------
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.icon.enter"));

            System.out.print(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.icon.cli") + " ");
            includeServerIcon = readBoolean();

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.checkreturn"), includeServerIcon));
            System.out.println();

//----------------------------------------------------------WHETHER TO INCLUDE SERVER.PROPERTIES IN SERVER PACK---------
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.properties.enter"));

            System.out.print(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.properties.cli") + " ");
            includeServerProperties = readBoolean();

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.checkreturn"), includeServerProperties));
            System.out.println();

//--------------------------------------------------------------WHETHER TO INCLUDE START SCRIPTS IN SERVER PACK---------
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.scripts.enter"));

            System.out.print(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.scripts.cli") + " ");
            includeStartScripts = readBoolean();

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.checkreturn"), includeStartScripts));
            System.out.println();

//----------------------------------------------------WHETHER TO INCLUDE CREATION OF ZIP-ARCHIVE OF SERVER PACK---------
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.zip.enter"));

            System.out.print(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.zip.cli") + " ");
            includeZipCreation = readBoolean();

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.checkreturn"), includeZipCreation));

//-------------------------------------------------------------------------JAVA ARGS TO EXECUTE THE SERVER WITH---------

            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.javaargs.cli"));

            System.out.print(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.javaargs.enter"));
            javaArgs = reader.nextLine();

            if (javaArgs.equals("")) {
                javaArgs = "empty";
            }

            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.printconfig.javaargs"), javaArgs));

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
                    includeZipCreation,
                    javaArgs);

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.config.enter"));

            System.out.print(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.answer") + " ");

        } while (!readBoolean());
        reader.close();

//-----------------------------------------------------------------------------------------WRITE CONFIG TO FILE---------
        if (writeConfigToFile(
                modpackDir,
                Arrays.asList(tmpClientMods),
                Arrays.asList(tmpCopyDirs),
                includeServerInstallation,
                javaPath,
                minecraftVersion,
                modLoader,
                modLoaderVersion,
                includeServerIcon,
                includeServerProperties,
                includeStartScripts,
                includeZipCreation,
                javaArgs,
                getConfigFile(),
                false
        )) {
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.config.written"));
        }
    }

    /**
     * A helper method for {@link #createConfigurationFile()}. Prompts the user to enter the values which will make up
     * a String List in the new configuration file. If the user enters an empty line, the method is exited and the
     * String List returned.
     * @author whitebear60
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
     * @author whitebear60
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
     * Encapsulate every element of the passed String List in quotes. Returns the list as <code>["element1","element2","element3"</code> etc.
     * @author Griefed
     * @param listToEncapsulate The String List of which to encapsulate every element in.
     * @return String. Returns a concatenated String with all elements of the passed list encapsulated.
     */
    public String encapsulateListElements(List<String> listToEncapsulate) {

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("[\"").append(listToEncapsulate.get(0).replace("\\", "/")).append("\"");

        for (int i = 1; i < listToEncapsulate.size(); i++) {
            stringBuilder.append(",\"").append(listToEncapsulate.get(i).replace("\\", "/")).append("\"");
        }

        stringBuilder.append("]");

        return stringBuilder.toString();
    }

    /**
     * A helper method for {@link #createConfigurationFile()}. Prompts the user to enter values which will then be
     * converted to booleans, either <code>TRUE</code> or <code>FALSE</code>. This prevents any non-boolean values
     * from being written to the new configuration file.
     * @author whitebear60
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
                /* This log is meant to be read by the user, therefore we allow translation. */
                LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.answer"));
            }
        }
    }

    /** Writes a new configuration file with the parameters passed to it.
     * @author whitebear60
     * @author Griefed
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
     * @param javaArgs String. Java arguments to write the start-scripts with.
     * @param fileName The name under which to write the new configuration file.
     * @param isTemporary Decides whether to delete existing config-file. If isTemporary is false, existing config files will be deleted before writing the new file.
     * @return Boolean. Returns true if the configuration file has been successfully written and old ones replaced.
     */
    public boolean writeConfigToFile(String modpackDir,
                                     List<String> clientMods,
                                     List<String> copyDirs,
                                     boolean includeServer,
                                     String javaPath,
                                     String minecraftVersion,
                                     String modLoader,
                                     String modLoaderVersion,
                                     boolean includeIcon,
                                     boolean includeProperties,
                                     boolean includeScripts,
                                     boolean includeZip,
                                     String javaArgs,
                                     File fileName,
                                     boolean isTemporary) {

        boolean configWritten = false;

        if (javaArgs.equals("")) {
            javaArgs = "empty";
        }

        //Griefed: What the fuck. This reads like someone having a stroke. What have I created here?
        String configString = String.format(
                        "%s\nmodpackDir = \"%s\"\n\n" +
                        "%s\nclientMods = %s\n\n" +
                        "%s\ncopyDirs = %s\n\n" +
                        "%s\nincludeServerInstallation = %b\n\n" +
                        "%s\njavaPath = \"%s\"\n\n" +
                        "%s\nminecraftVersion = \"%s\"\n\n" +
                        "%s\nmodLoader = \"%s\"\n\n" +
                        "%s\nmodLoaderVersion = \"%s\"\n\n" +
                        "%s\nincludeServerIcon = %b\n\n" +
                        "%s\nincludeServerProperties = %b\n\n" +
                        "%s\nincludeStartScripts = %b\n\n" +
                        "%s\nincludeZipCreation = %b\n\n" +
                        "%s\njavaArgs = \"%s\"\n",
                LOCALIZATIONMANAGER.getLocalizedString("configuration.writeconfigtofile.modpackdir"), modpackDir.replace("\\","/"),
                LOCALIZATIONMANAGER.getLocalizedString("configuration.writeconfigtofile.clientmods"), encapsulateListElements(clientMods),
                LOCALIZATIONMANAGER.getLocalizedString("configuration.writeconfigtofile.copydirs"), encapsulateListElements(copyDirs),
                LOCALIZATIONMANAGER.getLocalizedString("configuration.writeconfigtofile.includeserverinstallation"), includeServer,
                LOCALIZATIONMANAGER.getLocalizedString("configuration.writeconfigtofile.javapath"), javaPath.replace("\\","/"),
                LOCALIZATIONMANAGER.getLocalizedString("configuration.writeconfigtofile.minecraftversion"), minecraftVersion,
                LOCALIZATIONMANAGER.getLocalizedString("configuration.writeconfigtofile.modloader"), modLoader,
                LOCALIZATIONMANAGER.getLocalizedString("configuration.writeconfigtofile.modloaderversion"), modLoaderVersion,
                LOCALIZATIONMANAGER.getLocalizedString("configuration.writeconfigtofile.includeservericon"), includeIcon,
                LOCALIZATIONMANAGER.getLocalizedString("configuration.writeconfigtofile.includeserverproperties"), includeProperties,
                LOCALIZATIONMANAGER.getLocalizedString("configuration.writeconfigtofile.includestartscripts"), includeScripts,
                LOCALIZATIONMANAGER.getLocalizedString("configuration.writeconfigtofile.includezipcreation"), includeZip,
                LOCALIZATIONMANAGER.getLocalizedString("configuration.writeconfigtofile.javaargs"), javaArgs
        );

        if (!isTemporary) {
            if (getConfigFile().exists()) {
                boolean delConf = getConfigFile().delete();
                if (delConf) {
                    /* This log is meant to be read by the user, therefore we allow translation. */
                    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.info.writeconfigtofile.config"));
                } else {
                    /* This log is meant to be read by the user, therefore we allow translation. */
                    LOG.error(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.error.writeconfigtofile.config"));
                }
            }
            if (getOldConfigFile().exists()) {
                boolean delOldConf = getOldConfigFile().delete();
                if (delOldConf) {
                    /* This log is meant to be read by the user, therefore we allow translation. */
                    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.info.writeconfigtofile.old"));
                } else {
                    /* This log is meant to be read by the user, therefore we allow translation. */
                    LOG.error(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.error.writeconfigtofile.old"));
                }
            }
        }

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
            writer.write(configString);
            writer.close();
            configWritten = true;
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.info.writeconfigtofile.confignew"));
        } catch (IOException ex) {
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.error.writeconfigtofile"), ex);
        }

        return configWritten;
    }

    /**
     * Creates a list of all configurations as they appear in the serverpackcreator.conf to pass it to any addon run by
     * ServerPackCreator in {@link AddonsHandler}.<br>
     * Values included in this list are:<br>
     * 1. modpackDir<br>
     * 2. clientMods<br>
     * 3. copyDirs<br>
     * 4. javaPath<br>
     * 5. minecraftVersion<br>
     * 6. modLoader<br>
     * 7. modLoaderVersion<br>
     * 8. includeServerInstallation<br>
     * 9. includeServerIcon<br>
     * 10.includeServerProperties<br>
     * 11.includeStartScripts<br>
     * 12.includeZipCreation
     * @author Griefed
     * @param configurationModel An instance of {@link ConfigurationModel} which contains the configuration of the modpack.
     * @return String List. A list of all configurations as strings.
     */
    public List<String> getConfigurationAsList(ConfigurationModel configurationModel) {
        List<String> configurationAsList = new ArrayList<>();

        configurationAsList.add(configurationModel.getModpackDir());
        configurationAsList.add(buildString(configurationModel.getClientMods().toString()));
        configurationAsList.add(buildString(configurationModel.getCopyDirs().toString()));
        configurationAsList.add(configurationModel.getJavaPath());
        configurationAsList.add(configurationModel.getMinecraftVersion());
        configurationAsList.add(configurationModel.getModLoader());
        configurationAsList.add(configurationModel.getModLoaderVersion());
        configurationAsList.add(String.valueOf(configurationModel.getIncludeServerInstallation()));
        configurationAsList.add(String.valueOf(configurationModel.getIncludeServerIcon()));
        configurationAsList.add(String.valueOf(configurationModel.getIncludeServerProperties()));
        configurationAsList.add(String.valueOf(configurationModel.getIncludeStartScripts()));
        configurationAsList.add(String.valueOf(configurationModel.getIncludeZipCreation()));

        LOG.debug(String.format("Configuration to pass to addons is: %s", configurationAsList));

        return configurationAsList;
    }
}