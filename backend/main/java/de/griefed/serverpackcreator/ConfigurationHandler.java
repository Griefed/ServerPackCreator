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

import com.electronwill.nightconfig.core.file.FileConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.therandomlabs.curseapi.CurseAPI;
import com.therandomlabs.curseapi.CurseException;
import com.therandomlabs.curseapi.project.CurseProject;
import com.typesafe.config.*;
import de.griefed.serverpackcreator.curseforge.CurseCreateModpack;
import de.griefed.serverpackcreator.curseforge.InvalidFileException;
import de.griefed.serverpackcreator.curseforge.InvalidModpackException;
import de.griefed.serverpackcreator.i18n.LocalizationManager;
import de.griefed.serverpackcreator.spring.models.ServerPack;
import de.griefed.serverpackcreator.utilities.VersionLister;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.*;
import java.util.*;

/**
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
    private final VersionLister VERSIONLISTER;
    private final ApplicationProperties APPLICATIONPROPERTIES;

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
     * @param injectedApplicationProperties Instance of {@link Properties} required for various different things.
     * @param injectedVersionLister Instance of {@link VersionLister} required for everything version-related.
     */
    @Autowired
    public ConfigurationHandler(LocalizationManager injectedLocalizationManager, CurseCreateModpack injectedCurseCreateModpack, VersionLister injectedVersionLister, ApplicationProperties injectedApplicationProperties) {
        if (injectedApplicationProperties == null) {
            this.APPLICATIONPROPERTIES = new ApplicationProperties();
        } else {
            this.APPLICATIONPROPERTIES = injectedApplicationProperties;
        }

        if (injectedLocalizationManager == null) {
            this.LOCALIZATIONMANAGER = new LocalizationManager(APPLICATIONPROPERTIES);
        } else {
            this.LOCALIZATIONMANAGER = injectedLocalizationManager;
        }

        if (injectedCurseCreateModpack == null) {
            this.CURSECREATEMODPACK = new CurseCreateModpack(LOCALIZATIONMANAGER, APPLICATIONPROPERTIES);
        } else {
            this.CURSECREATEMODPACK = injectedCurseCreateModpack;
        }

        if (injectedVersionLister == null) {
            this.VERSIONLISTER = new VersionLister(APPLICATIONPROPERTIES);
        } else {
            this.VERSIONLISTER = injectedVersionLister;
        }
    }

    /**
     * Check the passed {@link ConfigurationModel}. If any check returns <code>true</code>
     * then the server pack will not be created. In order to find out which check failed, the user has to check their
     * serverpackcreator.log in the logs-directory.<br>
     * Does not create a modpack if a CurseForge project and file is specified.
     * @author Griefed
     * @param configurationModel ConfigurationModel. Instance of a configuration of a modpack. Can be used to further display or use any information within, as it may be changed
     * or otherwise altered by this method.
     * @param quietCheck Boolean. Whether the configuration should be printed to the console and logs. Pass false to quietly check the configuration.
     * @return Boolean. Returns <code>false</code> if the configuration has passed all tests.
     */
    public boolean checkConfiguration(@NotNull ConfigurationModel configurationModel, boolean quietCheck) {

        List<String> encounteredErrors = new ArrayList<>(100);

        return checkConfiguration(configurationModel, encounteredErrors, false, quietCheck);
    }

    /**
     * Check the passed configuration-file. If any check returns <code>true</code>
     * then the server pack will not be created. In order to find out which check failed, the user has to check their
     * serverpackcreator.log in the logs-directory.<br>
     * Does not create a modpack if a CurseForge project and file is specified.
     * @author Griefed
     * @param configFile File. The configuration file to check. Must either be an existing file to load a configuration
     *        from or null if you want to use the passed configuration model.
     * @param quietCheck Boolean. Whether the configuration should be printed to the console and logs. Pass false to quietly check the configuration.
     * @return Boolean. Returns <code>false</code> if the configuration has passed all tests.
     */
    public boolean checkConfiguration(@NotNull File configFile, boolean quietCheck) {

        List<String> encounteredErrors = new ArrayList<>(100);

        ConfigurationModel configurationModel = new ConfigurationModel();

        return checkConfiguration(configFile, configurationModel, encounteredErrors, false, quietCheck);
    }

    /**
     * Check the passed configuration-file. If any check returns <code>true</code>
     * then the server pack will not be created. In order to find out which check failed, the user has to check their
     * serverpackcreator.log in the logs-directory.<br>
     * Does not create a modpack if a CurseForge project and file is specified.
     * @author Griefed
     * @param configFile File. The configuration file to check. Must either be an existing file to load a configuration
     *        from or null if you want to use the passed configuration model.
     * @param encounteredErrors List String. A list of errors encountered during configuration checks which gets printed
     *                          to the console and log after all checks have run. Gives the user more detail on what
     *                          went wrong at which part of their configuration. Can be used to display the errors, if any were encountered,
     *                          in a UI or be printed into the console or whatever have you.
     * @param quietCheck Boolean. Whether the configuration should be printed to the console and logs. Pass false to quietly check the configuration.
     * @return Boolean. Returns <code>false</code> if the configuration has passed all tests.
     */
    public boolean checkConfiguration(@NotNull File configFile, @NotNull List<String> encounteredErrors, boolean quietCheck) {

        ConfigurationModel configurationModel = new ConfigurationModel();

        return checkConfiguration(configFile, configurationModel, encounteredErrors, false, quietCheck);
    }

    /**
     * Check the passed configuration-file. If any check returns <code>true</code>
     * then the server pack will not be created. In order to find out which check failed, the user has to check their
     * serverpackcreator.log in the logs-directory.<br>
     * Does not create a modpack if a CurseForge project and file is specified.
     * @author Griefed
     * @param configFile File. The configuration file to check. Must either be an existing file to load a configuration
     *        from or null if you want to use the passed configuration model.
     * @param configurationModel ConfigurationModel. Instance of a configuration of a modpack. Can be used to further display or use any information within, as it may be changed
     * or otherwise altered by this method.
     * @param quietCheck Boolean. Whether the configuration should be printed to the console and logs. Pass false to quietly check the configuration.
     * @return Boolean. Returns <code>false</code> if the configuration has passed all tests.
     */
    public boolean checkConfiguration(@NotNull File configFile, @NotNull ConfigurationModel configurationModel, boolean quietCheck) {

        List<String> encounteredErrors = new ArrayList<>(100);

        return checkConfiguration(configFile, configurationModel, encounteredErrors, false, quietCheck);
    }

    /**
     * Check the passed configuration-file. If any check returns <code>true</code>
     * then the server pack will not be created. In order to find out which check failed, the user has to check their
     * serverpackcreator.log in the logs-directory.
     * @author Griefed
     * @param configFile File. The configuration file to check. Must either be an existing file to load a configuration
     *        from or null if you want to use the passed configuration model.
     * @param downloadAndCreateModpack Boolean. Whether the CurseForge modpack should be downloaded and created.
     * @param quietCheck Boolean. Whether the configuration should be printed to the console and logs. Pass false to quietly check the configuration.
     * @return Boolean. Returns <code>false</code> if the configuration has passed all tests.
     */
    public boolean checkConfiguration(@NotNull File configFile, boolean downloadAndCreateModpack, boolean quietCheck) {

        List<String> encounteredErrors = new ArrayList<>(100);

        ConfigurationModel configurationModel = new ConfigurationModel();

        return checkConfiguration(configFile, configurationModel, encounteredErrors, downloadAndCreateModpack, quietCheck);
    }

    /**
     * Check the passed configuration-file. If any check returns <code>true</code>
     * then the server pack will not be created. In order to find out which check failed, the user has to check their
     * serverpackcreator.log in the logs-directory.
     * @author Griefed
     * @param configFile File. The configuration file to check. Must either be an existing file to load a configuration
     *        from or null if you want to use the passed configuration model.
     * @param configurationModel ConfigurationModel. Instance of a configuration of a modpack. Can be used to further display or use any information within, as it may be changed
     * or otherwise altered by this method.
     * @param downloadAndCreateModpack Boolean. Whether the CurseForge modpack should be downloaded and created.
     * @param quietCheck Boolean. Whether the configuration should be printed to the console and logs. Pass false to quietly check the configuration.
     * @return Boolean. Returns <code>false</code> if the configuration has passed all tests.
     */
    public boolean checkConfiguration(@NotNull File configFile, @NotNull ConfigurationModel configurationModel, boolean downloadAndCreateModpack, boolean quietCheck) {

        List<String> encounteredErrors = new ArrayList<>(100);

        return checkConfiguration(configFile, configurationModel, encounteredErrors, downloadAndCreateModpack, quietCheck);
    }

    /**
     * Check the passed {@link ConfigurationModel}. If any check returns <code>true</code>
     * then the server pack will not be created. In order to find out which check failed, the user has to check their
     * serverpackcreator.log in the logs-directory.
     * @author Griefed
     * @param configurationModel ConfigurationModel. Instance of a configuration of a modpack. Can be used to further display or use any information within, as it may be changed
     * or otherwise altered by this method.
     * @param downloadAndCreateModpack Boolean. Whether the CurseForge modpack should be downloaded and created.
     * @param quietCheck Boolean. Whether the configuration should be printed to the console and logs. Pass false to quietly check the configuration.
     * @return Boolean. Returns <code>false</code> if the configuration has passed all tests.
     */
    public boolean checkConfiguration(@NotNull ConfigurationModel configurationModel, boolean downloadAndCreateModpack, boolean quietCheck) {

        List<String> encounteredErrors = new ArrayList<>(100);

        return checkConfiguration(configurationModel, encounteredErrors, downloadAndCreateModpack, quietCheck);
    }

    /**
     * Check the passed configuration-file. If any check returns <code>true</code>
     * then the server pack will not be created. In order to find out which check failed, the user has to check their
     * serverpackcreator.log in the logs-directory.
     * @author Griefed
     * @param configFile File. The configuration file to check. Must either be an existing file to load a configuration
     *                   from or null if you want to use the passed configuration model.
     * @param configurationModel ConfigurationModel. Instance of a configuration of a modpack. Can be used to further display or use any information within, as it may be changed
     * or otherwise altered by this method.
     * @param encounteredErrors List String. A list of errors encountered during configuration checks which gets printed
     *                          to the console and log after all checks have run. Gives the user more detail on what
     *                          went wrong at which part of their configuration. Can be used to display the errors, if any were encountered,
     *                          in a UI or be printed into the console or whatever have you.
     * @param downloadAndCreateModpack Boolean. Whether the CurseForge modpack should be downloaded and created.
     * @param quietCheck Boolean. Whether the configuration should be printed to the console and logs. Pass false to quietly check the configuration.
     * @return Boolean. Returns <code>false</code> if the configuration has passed all tests.
     */
    public boolean checkConfiguration(@NotNull File configFile, @NotNull ConfigurationModel configurationModel, @NotNull List<String> encounteredErrors, boolean downloadAndCreateModpack, boolean quietCheck) {
        /*
         * TODO: Rewrite so configs from file are passed to checkConfiguration(boolean shouldModpackBeCreated, ConfigurationModel configurationModel) Makes future refactorings easier
         */

        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.checkconfig.start"));

        FileConfig config = null;

        try {
            config = FileConfig.of(configFile);
        } catch (ConfigException ex) {
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.checkconfig.start"));
        }

        if (config != null) {

            try {
                config.load();
            } catch (ConfigException ex) {
                /* This log is meant to be read by the user, therefore we allow translation. */
                LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.checkconfig.start"));
                encounteredErrors.add(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.checkconfig.start"));
            }

            configurationModel.setClientMods(config.getOrElse("clientMods", Collections.singletonList("")));
            configurationModel.setCopyDirs(config.getOrElse("copyDirs",Collections.singletonList("")));
            configurationModel.setModpackDir(config.getOrElse("modpackDir", "").replace("\\", "/"));
            configurationModel.setJavaPath(config.getOrElse("javaPath", "").replace("\\", "/"));

            configurationModel.setMinecraftVersion(config.getOrElse("minecraftVersion",""));
            configurationModel.setModLoader(config.getOrElse("modLoader",""));
            configurationModel.setModLoaderVersion(config.getOrElse("modLoaderVersion",""));
            configurationModel.setJavaArgs(config.getOrElse("javaArgs","empty"));

            configurationModel.setServerPackSuffix(config.getOrElse("serverPackSuffix",""));
            configurationModel.setServerIconPath(config.getOrElse("serverIconPath",""));
            configurationModel.setServerPropertiesPath(config.getOrElse("serverPropertiesPath",""));

            configurationModel.setIncludeServerInstallation(convertToBoolean(String.valueOf(config.getOrElse("includeServerInstallation","False"))));
            configurationModel.setIncludeServerIcon(convertToBoolean(String.valueOf(config.getOrElse("includeServerIcon", "False"))));
            configurationModel.setIncludeServerProperties(convertToBoolean(String.valueOf(config.getOrElse("includeServerProperties", "False"))));
            configurationModel.setIncludeZipCreation(convertToBoolean(String.valueOf(config.getOrElse("includeZipCreation","False"))));

        } else {

            encounteredErrors.add(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.checkconfig.start"));

        }

        return checkConfiguration(configurationModel, encounteredErrors, downloadAndCreateModpack, quietCheck);

    }

    /**
     * Check the passed {@link ConfigurationModel}. If any check returns <code>true</code>
     * then the server pack will not be created. In order to find out which check failed, the user has to check their
     * serverpackcreator.log in the logs-directory.<br>
     * The passed String List <code>encounteredErrors</code> can be used to display the errors, if any were encountered,
     * in a UI or be printed into the console or whatever have you.<br>
     * The passed {@link ConfigurationModel} can be used to further display or use any information within, as it may be changed
     * or otherwise altered by this method.
     * @author Griefed
     * @param configurationModel ConfigurationModel. Instance of a configuration of a modpack. Can be used to further display or use any information within, as it may be changed
     * or otherwise altered by this method.
     * @param encounteredErrors List String. A list of errors encountered during configuration checks which gets printed
     *                          to the console and log after all checks have run. Gives the user more detail on what
     *                          went wrong at which part of their configuration. Can be used to display the errors, if any were encountered,
     *                          in a UI or be printed into the console or whatever have you.
     * @param downloadAndCreateModpack Boolean. Whether the CurseForge modpack, if specified, should be downloaded and created.
     * @param quietCheck Boolean. Whether the configuration should be printed to the console and logs. Pass false to quietly check the configuration.
     * @return Boolean. Returns <code>false</code> if all checks are passed.
     */
    public boolean checkConfiguration(@NotNull ConfigurationModel configurationModel, @NotNull List<String> encounteredErrors, boolean downloadAndCreateModpack, boolean quietCheck) {
        boolean configHasError = false;

        /* This log is meant to be read by the user, therefore we allow translation. */
        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.checkconfig.start"));

        if (configurationModel.getClientMods().isEmpty()) {
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.warn.checkconfig.clientmods"));
            configurationModel.setClientMods(APPLICATIONPROPERTIES.getListFallbackMods());
        } else {
            configurationModel.setClientMods(configurationModel.getClientMods());
        }

        configurationModel.setJavaPath(checkJavaPath(configurationModel.getJavaPath().replace("\\", "/")));

        if (!checkIconAndProperties(configurationModel.getServerIconPath())) {

            configHasError = true;

            // TODO: Replace with lang key
            encounteredErrors.add("The specified server-icon does not exist: " + configurationModel.getServerIconPath());

        }

        if (!checkIconAndProperties(configurationModel.getServerPropertiesPath())) {

            configHasError = true;

            // TODO: Replace with lang key
            encounteredErrors.add("The specified server.properties does not exist: " + configurationModel.getServerPropertiesPath());

        }

        try {

            if (configurationModel.getModpackDir().equals("")) {

                configHasError = true;
                LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.checkmodpackdir"));
                encounteredErrors.add(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.checkmodpackdir"));

            } else if (new File(configurationModel.getModpackDir()).isDirectory()) {

                configHasError = isDir(configurationModel, encounteredErrors);

            } else if (checkCurseForge(configurationModel.getModpackDir(), configurationModel, encounteredErrors)) {

                if (downloadAndCreateModpack) {

                    configHasError = isCurse(configurationModel, encounteredErrors);

                } else {
                    /* This log is meant to be read by the user, therefore we allow translation. */
                    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.checkconfig.skipmodpackcreation"));
                }

            }


        } catch (InvalidModpackException ex) {
            LOG.error("The specified project is not a valid Minecraft modpack!", ex);
        } catch (InvalidFileException ex) {
            LOG.error("The specified file is not a file of this project.", ex);
        } catch (CurseException ex) {
            LOG.error("The specified project does not exist.", ex);
        }


        if (quietCheck) {
            printConfigurationModel(configurationModel);
        }

        if (!configHasError) {

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.checkconfig.success"));

        } else {

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.checkconfig.failure"));

            printEncounteredErrors(encounteredErrors);

        }

        return configHasError;
    }

    /**
     * If the in the configuration specified modpack dir is an existing directory, checks are made for valid configuration
     * of: directories to copy to server pack,<br>
     * if includeServerInstallation is <code>true</code>) path to Java executable/binary, Minecraft version, modloader and modloader version.
     * @author Griefed
     * @param configurationModel An instance of {@link ConfigurationModel} which contains the configuration of the modpack.
     * @param encounteredErrors List String. A list to which all encountered errors are saved to.
     * @return Boolean. Returns true if an error is found during configuration check.
     */
    private boolean isDir(ConfigurationModel configurationModel, List<String> encounteredErrors) {
        boolean configHasError = false;

        if (checkCopyDirs(configurationModel.getCopyDirs(), configurationModel.getModpackDir(), encounteredErrors)) {

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.debug(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.debug.isdir.copydirs"));

        } else {

            configHasError = true;
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.isdir.copydir"));
            encounteredErrors.add(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.isdir.copydir"));

        }

        if (checkModloader(configurationModel.getModLoader())) {

            if (isMinecraftVersionCorrect(configurationModel.getMinecraftVersion())) {

                /* This log is meant to be read by the user, therefore we allow translation. */
                LOG.debug(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.debug.isdir.minecraftversion"));

                /* This log is meant to be read by the user, therefore we allow translation. */
                LOG.debug(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.debug.isdir.modloader"));

                if (checkModloaderVersion(configurationModel.getModLoader(), configurationModel.getModLoaderVersion(), configurationModel.getMinecraftVersion())) {

                    /* This log is meant to be read by the user, therefore we allow translation. */
                    LOG.debug(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.debug.isdir.modloaderversion"));

                } else {

                    configHasError = true;

                    /* This log is meant to be read by the user, therefore we allow translation. */
                    LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.isdir.modloaderversion"));
                    encounteredErrors.add(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.checkmodloaderversion"));

                }

            } else {

                configHasError = true;
                /* This log is meant to be read by the user, therefore we allow translation. */
                LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.isdir.minecraftversion"));
                // TODO: Replace with lang key
                encounteredErrors.add("Incorrect Minecraft version specified.");

            }


        } else {

            configHasError = true;

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.isdir.modloader"));
            encounteredErrors.add(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.checkmodloader"));

        }

        return configHasError;
    }

    /**
     * If modpackDir in the configuration file is a CurseForge projectID,fileID combination, then the modpack is first
     * created from said combination, using {@link CurseCreateModpack#curseForgeModpack(ConfigurationModel, Integer, Integer)},
     * before proceeding to checking the rest of the configuration. If everything passes and the modpack was created,
     * a new configuration file is created, replacing the one used to create the modpack in the first place, with the
     * modpackDir field pointing to the newly created modpack.
     * @author Griefed
     * @param configurationModel An instance of {@link ConfigurationModel} which contains the configuration of the modpack.
     * @param encounteredErrors List String. A list to which all encountered errors are saved to.
     * @return Boolean. Returns false unless an error was encountered during either the acquisition of the CurseForge
     * project name and displayname, or when the creation of the modpack fails.
     */
    private boolean isCurse(ConfigurationModel configurationModel, List<String> encounteredErrors) {
        boolean configHasError = false;
        try {
            if (CurseAPI.project(configurationModel.getProjectID()).isPresent() && CurseAPI.file(configurationModel.getProjectID(), configurationModel.getFileID()).isPresent()) {

                CURSECREATEMODPACK.curseForgeModpack(configurationModel, configurationModel.getProjectID(), configurationModel.getFileID());

                configurationModel.setMinecraftVersion(configurationModel.getCurseModpack().get("minecraft").get("version").asText());

                if (checkModpackForFabric(configurationModel.getCurseModpack())) {

                    if (configurationModel.getCurseModpack().get("minecraft").get("modLoaders").get(0).get("id").asText().contains("fabric")) {

                        configurationModel.setModLoader("Fabric");
                        configurationModel.setModLoaderVersion(configurationModel.getCurseModpack().get("minecraft").get("modLoaders").get(0).get("id").asText().substring(7));

                    } else {

                        /* This log is meant to be read by the user, therefore we allow translation. */
                        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.iscurse.fabric"));
                        LOG.debug("Setting modloader to Fabric.");

                        configurationModel.setModLoader("Fabric");
                        configurationModel.setModLoaderVersion(VERSIONLISTER.getFabricReleaseVersion());

                    }

                } else {

                    // TODO: Replace with lang key
                    /* This log is meant to be read by the user, therefore we allow translation. */
                    LOG.debug("Setting modloader to Forge.");

                    configurationModel.setModLoader("Forge");
                    configurationModel.setModLoaderVersion(configurationModel.getCurseModpack().get("minecraft").get("modLoaders").get(0).get("id").asText().substring(6));

                }

                configurationModel.setCopyDirs(suggestCopyDirs(configurationModel.getModpackDir()));

                /* This log is meant to be read by the user, therefore we allow translation. */
                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.iscurse.replace"));

                writeConfigToFile(configurationModel, APPLICATIONPROPERTIES.FILE_CONFIG,false);

            } else {

                configHasError = true;
                /* This log is meant to be read by the user, therefore we allow translation. */
                LOG.error(LOCALIZATIONMANAGER.getLocalizedString("cursecreatemodpack.log.error.notfound"));

                encounteredErrors.add(LOCALIZATIONMANAGER.getLocalizedString("cursecreatemodpack.log.error.notfound"));

            }

        } catch (CurseException | IllegalArgumentException ex) {

            configHasError = true;
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.iscurse.project"), configurationModel.getProjectID()), ex);

            encounteredErrors.add(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.iscurse.project"), configurationModel.getProjectID()));

        }

        return configHasError;
    }

    /**
     * Checks whether the projectID for the Jumploader mod is present in the list of mods required by the CurseForge modpack.
     * If Jumploader is found, the modloader for the new configuration-file will be set to Fabric.
     * If <code>modLoaders</code> in the manifest specifies Fabric, use that to set the modloader and its version.
     * @author Griefed
     * @param modpackJson JSonNode. JsonNode containing all information about the CurseForge modpack.
     * @return Boolean. Returns true if Jumploader is found.
     */
    private boolean checkModpackForFabric(JsonNode modpackJson) {

        // TODO: Move to CurseCreateModpack and set modloader in there

        for (int i = 0; i < modpackJson.get("files").size(); i++) {

            LOG.debug(String.format("Mod ID: %s", modpackJson.get("files").get(i).get("projectID").asText()));
            LOG.debug(String.format("File ID: %s", modpackJson.get("files").get(i).get("fileID").asText()));

            if (modpackJson.get("files").get(i).get("projectID").asText().equalsIgnoreCase("361988") || modpackJson.get("files").get(i).get("fileID").asText().equalsIgnoreCase("306612")) {

                /* This log is meant to be read by the user, therefore we allow translation. */
                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.containsfabric"));
                return true;
            }
        }

        String[] modloaderAndVersion = modpackJson.get("minecraft").get("modLoaders").get(0).get("id").asText().split("-");

        return modloaderAndVersion[0].equalsIgnoreCase("fabric");
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
    public List<String> suggestCopyDirs(String modpackDir) {
        /* This log is meant to be read by the user, therefore we allow translation. */
        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.suggestcopydirs.start"));

        File[] listDirectoriesInModpack = new File(modpackDir).listFiles();

        List<String> dirsInModpack = new ArrayList<>(100);

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

        for (int idirs = 0; idirs < APPLICATIONPROPERTIES.getListOfDirectoriesToExclude().size(); idirs++) {

            int i = idirs;

            dirsInModpack.removeIf(n -> (n.contains(APPLICATIONPROPERTIES.getListOfDirectoriesToExclude().get(i))));
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
     * @param configurationModel or {@link ServerPack}. Instance containing
     *                           all the information about our server pack.
     * @param encounteredErrors List String. A list to which all encountered errors are saved to.
     * @throws InvalidModpackException Thrown if the specified IDs do not match a CurseForge modpack.
     * @throws InvalidFileException Thrown if the specified fileID does not match a file for the projectID.
     * @throws CurseException Thrown if an error occurs working with the CurseForgeAPI.
     * @return Boolean. Returns true if the combination is deemed valid, false if not.
     */
    public boolean checkCurseForge(String modpackDir, ConfigurationModel configurationModel, List<String> encounteredErrors) throws InvalidModpackException, InvalidFileException, CurseException {

        boolean configCorrect = false;

        if (modpackDir.matches("[0-9]{2,},[0-9]{5,}")) {

            String[] curseForgeIDCombination;
            curseForgeIDCombination = modpackDir.split(",");

            int curseProjectID = Integer.parseInt(curseForgeIDCombination[0]);
            int curseFileID = Integer.parseInt(curseForgeIDCombination[1]);

            CurseProject curseProject = null;

            try {

                if (CurseAPI.project(curseProjectID).isPresent()) {

                    curseProject = CurseAPI.project(curseProjectID).get();

                    if (!curseProject.game().name().equalsIgnoreCase("Minecraft")) {
                        LOG.debug("CurseForge game for " + curseProject.name() + " (id: " + curseProjectID + ") is: " + curseProject.game().name());
                        throw new InvalidModpackException(curseProjectID, curseProject.name());
                    }

                    if (!curseProject.categorySection().name().equalsIgnoreCase("Modpacks")) {
                        LOG.debug("CurseForge game for " + curseProject.name() + " (id: " + curseProjectID + ") is: " + curseProject.game().name());
                        throw new InvalidModpackException(curseProjectID, curseProject.name());
                    }

                    //noinspection UnusedAssignment
                    configCorrect = true;

                    configurationModel.setProjectID(curseProjectID);
                    configurationModel.setProjectName(CURSECREATEMODPACK.retrieveProjectName(curseProjectID));
                }

            } catch (NoSuchElementException ex) {

                //noinspection UnusedAssignment
                configCorrect = false;

                /* This log is meant to be read by the user, therefore we allow translation. */
                LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.iscurse.project"), curseProjectID), ex);
                encounteredErrors.add(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.iscurse.project"), curseProjectID));

            }

            try {
                if (curseProject != null) {

                    if (curseProject.refreshFiles().fileWithID(curseFileID) != null) {

                        configurationModel.setFileID(curseFileID);
                        configurationModel.setFileName(CURSECREATEMODPACK.retrieveFileName(curseProjectID, curseFileID));
                        configurationModel.setFileDiskName(CURSECREATEMODPACK.retrieveFileDiskName(curseProjectID,curseFileID));

                        configCorrect = true;

                    } else {

                        encounteredErrors.add(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.iscurse.file"), curseFileID));
                        throw new InvalidFileException(curseFileID);
                    }
                } else {

                    encounteredErrors.add("Specified CurseForge project " + curseProjectID + " could not be found.");
                    throw new CurseException("Project was null. Does the specified project " + curseProjectID + " exist?");
                }

            } catch (CurseException | NoSuchElementException ex) {

                configCorrect = false;

                /* This log is meant to be read by the user, therefore we allow translation. */
                LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.iscurse.file"), curseFileID), ex);
                encounteredErrors.add(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.iscurse.file"), curseFileID));

            }

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.checkcurseforge.info"));
            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.checkcurseforge.return"), configurationModel.getProjectID(), configurationModel.getFileID()));
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

        // TODO: Move to class Utilities.ConfigUtilities

        if (stringBoolean.matches("[Tt]rue")    ||
                stringBoolean.matches("1")      ||
                stringBoolean.matches("[Yy]es") ||
                stringBoolean.matches("[Yy]")   ||
                stringBoolean.matches(LOCALIZATIONMANAGER.getLocalizedString("cli.input.true")) ||
                stringBoolean.matches(LOCALIZATIONMANAGER.getLocalizedString("cli.input.yes"))  ||
                stringBoolean.matches(LOCALIZATIONMANAGER.getLocalizedString("cli.input.yes.short"))
        ){
            return true;

        } else if (stringBoolean.matches("[Ff]alse") ||
                stringBoolean.matches("0")           ||
                stringBoolean.matches("[Nn]o")       ||
                stringBoolean.matches("[Nn]" )       ||
                stringBoolean.matches(LOCALIZATIONMANAGER.getLocalizedString("cli.input.false")) ||
                stringBoolean.matches(LOCALIZATIONMANAGER.getLocalizedString("cli.input.no"))    ||
                stringBoolean.matches(LOCALIZATIONMANAGER.getLocalizedString("cli.input.no.short"))
        ){
            return false;

        } else {
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.warn.converttoboolean.warn"));
            return false;
        }
    }

    /**
     * Print all encountered errors to logs.
     * @author Griefed
     * @param encounteredErrors List String. A list of all errors which were encountered during a configuration check.
     */
    private void printEncounteredErrors(List<String> encounteredErrors) {
        // TODO: Replace with lang key
        LOG.error("Encountered " + encounteredErrors.size() + " errors during the configuration check.");

        //noinspection UnusedAssignment
        int encounteredErrorNumber = 0;

        for (int i = 0; i < encounteredErrors.size(); i++) {

            encounteredErrorNumber = i + 1;

            LOG.error("Error " + encounteredErrorNumber + ": " + encounteredErrors.get(i));
        }
    }

    /**
     * Convenience method which passes the important fields from an instance of {@link ConfigurationModel} to {@link #printConfigurationModel(String, List, List, boolean, String, String, String, String, boolean, boolean, boolean, String, String, String, String)}
     * @author Griefed
     * @param configurationModel Instance of {@link ConfigurationModel} to print to console and logs.
     */
    public void printConfigurationModel(ConfigurationModel configurationModel) {
        // TODO: Move to class Utilities.ConfigUtilities
        printConfigurationModel(
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
                configurationModel.getIncludeZipCreation(),
                configurationModel.getJavaArgs(),
                configurationModel.getServerPackSuffix(),
                configurationModel.getServerIconPath(),
                configurationModel.getServerPropertiesPath()
        );
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
     * @param includeZip Boolean. Whether to create a zip-archive of the server pack, excluding the Minecraft server JAR according to Mojang's TOS and EULA.
     * @param javaArgs String. Java arguments to write the start-scripts with.
     * @param serverPackSuffix String. Suffix to append to name of the server pack to be generated.
     * @param serverIconPath String. The path to the custom server-icon.png to be used in the server pack.
     * @param serverPropertiesPath String. The path to the custom server.properties to be used in the server pack.
     */
    void printConfigurationModel(String modpackDirectory,
                                 List<String> clientsideMods,
                                 List<String> copyDirectories,
                                 boolean installServer,
                                 String javaInstallPath,
                                 String minecraftVer,
                                 String modloader,
                                 String modloaderVersion,
                                 boolean includeIcon,
                                 boolean includeProperties,
                                 boolean includeZip,
                                 String javaArgs,
                                 String serverPackSuffix,
                                 String serverIconPath,
                                 String serverPropertiesPath) {

        // TODO: Move to class Utilities.ConfigUtilities

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
        LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.printconfig.zip"), includeZip));
        LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.printconfig.javaargs"), javaArgs));
        LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.printconfig.serverpacksuffix"), serverPackSuffix));
        // TODO: Replace with lang keys
        LOG.info("Path to custom server-icon:       " + serverIconPath);
        LOG.info("Path to custom server.properties: " + serverPropertiesPath);
    }

    /**
     * Checks whether the passed String is empty and if it is empty, prints the corresponding message to the console and
     * serverpackcreator.log so the user knows what went wrong.<br>
     * Checks whether the passed String is a directory and if it is not, prints the corresponding message to the console
     * and serverpackcreator.log so the user knows what went wrong.
     * @author Griefed
     * @param modpackDir String. The path to the modpack directory to check whether it is empty and whether it is a directory.
     * @param encounteredErrors List String. A list to which all encountered errors are saved to.
     * @return Boolean. Returns true if the directory exists.
     */
    public boolean checkModpackDir(String modpackDir, List<String> encounteredErrors) {
        boolean configCorrect = false;

        if (modpackDir.equals("")) {

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.checkmodpackdir"));
            encounteredErrors.add(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.checkmodpackdir"));

        } else if (!(new File(modpackDir).isDirectory())) {

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.warn(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.warn.checkmodpackdir"), modpackDir));

            // TODO: Replace with lang key
            encounteredErrors.add(String.format("Modpack directory %s could not be found.", modpackDir));

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
     * @param encounteredErrors List String. A list to which all encountered errors are saved to.
     * @return Boolean. Returns true if every directory was found in the modpack directory. If any single one was not found,
     * false is returned.
     */
    public boolean checkCopyDirs(List<String> directoriesToCopy, String modpackDir, List<String> encounteredErrors) {
        boolean configCorrect = true;

        if (directoriesToCopy.isEmpty()) {

            configCorrect = false;

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.checkcopydirs.empty"));

            encounteredErrors.add(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.checkcopydirs.empty"));

        } else {

            for (String directory : directoriesToCopy) {

                // Check whether the user specified a source;destination-combination
                if (directory.contains(";")) {

                    String[] sourceFileDestinationFileCombination = directory.split(";");

                    File sourceFileToCheck = new File(String.format("%s/%s", modpackDir, sourceFileDestinationFileCombination[0]));

                    if (!sourceFileToCheck.exists()) {

                        configCorrect = false;

                        /* This log is meant to be read by the user, therefore we allow translation. */
                        LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.checkcopydirs.filenotfound"), sourceFileToCheck));

                        encounteredErrors.add(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.checkcopydirs.filenotfound"), sourceFileToCheck));

                    }

                // Add an entry to the list of directories/files to exclude if it starts with !
                } else if (directory.startsWith("!")) {

                    APPLICATIONPROPERTIES.addToListOfDirectoriesToExclude(directory.substring(directory.lastIndexOf("!") + 1));

                // Check if the entry exists
                } else {

                    File dirToCheck = new File(String.format("%s/%s", modpackDir, directory));

                    if (!dirToCheck.exists()) {

                        configCorrect = false;

                        /* This log is meant to be read by the user, therefore we allow translation. */
                        LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.checkcopydirs.notfound"), dirToCheck.getAbsolutePath()));

                        encounteredErrors.add(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.checkcopydirs.notfound"), dirToCheck.getAbsolutePath()));

                    }
                }
            }
        }
        return configCorrect;
    }

    /**
     * Checks the passed String whether it is an existing file. If the passed String is empty, then ServerPackCreator will
     * treat it as the user being fine with the default files and return the corresponding boolean.
     * @author Griefed
     * @param iconOrPropertiesPath String. The path to the custom server-icon.png or server.properties file to check.
     * @return Boolean. True if the file exists or an empty String was passed, false if a file was specified, but the file was not found.
     */
    public boolean checkIconAndProperties(String iconOrPropertiesPath) {

        if (iconOrPropertiesPath.equals("")) {

            return true;

        } else return new File(iconOrPropertiesPath).exists();
    }

    /**
     * Checks whether the passed String ends with <code>java.exe</code> or <code>java</code> and whether the files exist.
     * @author Griefed
     * @param pathToJava String. The path to check for java.exe and java.
     * @return String. Returns the path to the java installation. If user input was incorrect, SPC will try to acquire the path automatically.
     */
    public String checkJavaPath(String pathToJava) {

        //noinspection UnusedAssignment
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
                checkedJavaPath = acquireJavaPathFromSystem();

                LOG.debug("Automatically acquired path to Java installation: " + checkedJavaPath);
            }

        } catch (NullPointerException ex) {

            checkedJavaPath = acquireJavaPathFromSystem();

            LOG.debug("Automatically acquired path to Java installation: " + checkedJavaPath);

        }

        return checkedJavaPath;
    }

    /**
     * Automatically acquire the path to the systems default Java installation.
     * @author Griefed
     * @return String. The path to the systems default Java installation.
     */
    public String acquireJavaPathFromSystem() {

        // TODO: Move to class Utilities.ConfigUtilities

        LOG.debug("Acquiring path to Java installation from system properties...");
        String javaPath = String.format("%s/bin/java",System.getProperty("java.home").replace("\\", "/"));

        // TODO: Move to class wide field after move to Utilities.ConfigUtilities
        List<String> letters = new ArrayList<>(Arrays.asList("A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z"));

        for (String letter : letters) {
            if (javaPath.startsWith(letter + ":")) {

                LOG.debug("We're running on Windows. Ensuring javaPath ends with .exe");
                javaPath = String.format("%s.exe", javaPath);
            }
        }

        return javaPath;
    }

    /**
     * Checks whether either Forge or Fabric were specified as the modloader.
     * @author Griefed
     * @param modloader String. Check as case-insensitive for Forge or Fabric.
     * @return Boolean. Returns true if the specified modloader is either Forge or Fabric. False if neither.
     */
    public boolean checkModloader(String modloader) {

        if (modloader.toLowerCase().contains("forge") || modloader.toLowerCase().contains("fabric")) {

            return true;

        }

        /* This log is meant to be read by the user, therefore we allow translation. */
        LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.checkmodloader"));

        return false;

    }

    /**
     * Ensures the modloader is normalized to first letter upper case and rest lower case. Basically allows the user to
     * input Forge or Fabric in any combination of upper- and lowercase and ServerPackCreator will still be able to
     * work with the users input.
     * @author Griefed
     * @param modloader String. The String to check for case-insensitive cases of either Forge or Fabric.
     * @return String. Returns a normalized String of the specified modloader.
     */
    public String getModLoaderCase(String modloader) {

        if (modloader.equalsIgnoreCase("Forge")) {

            return "Forge";

        } else if (modloader.equalsIgnoreCase("Fabric")) {

            return "Fabric";

        } else if (modloader.toLowerCase().contains("forge")) {

            return "Forge";

        } else if (modloader.toLowerCase().contains("fabric")) {

            return "Fabric";

        } else {

            return "Forge";
        }
    }

    /**
     * Depending on whether Forge or Fabric was specified as the modloader, this will call the corresponding version check
     * to verify that the user correctly set their modloader version.<br>
     * If the user specified Forge as their modloader, {@link #isForgeVersionCorrect(String, String)} is called and the version
     * the user specified is checked against Forge's version manifest..<br>
     * If the user specified Fabric as their modloader, {@link #isFabricVersionCorrect(String)} is called and the version
     * the user specified is checked against Fabric's version manifest.
     * @author Griefed
     * @param modloader String. The passed modloader which determines whether the check for Forge or Fabric is called.
     * @param modloaderVersion String. The version of the modloader which is checked against the corresponding modloaders manifest.
     * @param minecraftVersion String. The version of Minecraft used for checking the Forge version.
     * @return Boolean. Returns true if the specified modloader version was found in the corresponding manifest.
     */
    public boolean checkModloaderVersion(String modloader, String modloaderVersion, String minecraftVersion) {

        if (modloader.equalsIgnoreCase("Forge") && isForgeVersionCorrect(modloaderVersion, minecraftVersion)) {

            return true;

        } else if (modloader.equalsIgnoreCase("Fabric") && isFabricVersionCorrect(modloaderVersion)) {

            return true;

        } else {

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.checkmodloaderversion"));



            return false;

        }
    }

    /**
     * Check whether the specified Minecraft version is correct by searching the version list of the Minecraft manifest
     * for the specified version.
     * @author Griefed
     * @param minecraftVersion String. The version to check for in Minecraft's version manifest.
     * @return Boolean. Returns true if the specified Minecraft version could be found in Mojang's manifest.
     */
    public boolean isMinecraftVersionCorrect(String minecraftVersion) {

        if (!minecraftVersion.equals("")) {

            return VERSIONLISTER.getMinecraftReleaseVersions().contains(minecraftVersion);

        } else {

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.isminecraftversioncorrect.empty"));

            return false;
        }
    }

    /**
     * Check whether the specified Fabric version is correct by searching the version list of the Fabric manifest
     * for the specified version.
     * @author Griefed
     * @param fabricVersion String. The version to check for in Fabric's version manifest.
     * @return Boolean. Returns true if the specified fabric version could be found in Fabric's manifest.
     */
    public boolean isFabricVersionCorrect(String fabricVersion) {

        if (!fabricVersion.equals("")) {

            return VERSIONLISTER.getFabricVersions().contains(fabricVersion);

        } else {

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.isfabricversioncorrect.empty"));

            return false;
        }
    }

    /**
     * Check whether the specified Forge version is correct by searching the version list of the Forge manifest
     * for the specified version.
     * @author Griefed
     * @param forgeVersion String. The version to check for in Forge's version manifest.
     * @param minecraftVersion String. The minecraft version to check the Forge version with.
     * @return Boolean. Returns true if the specified Forge version could be found in Forge's manifest.
     */
    public boolean isForgeVersionCorrect(String forgeVersion, String minecraftVersion) {

        if (!forgeVersion.equals("")) {

            try {

                for (String version : VERSIONLISTER.getForgeMeta().get(minecraftVersion)) {

                    if (version.equals(forgeVersion)) {

                        return true;

                    }

                }

            } catch (NullPointerException ignored) {

                return false;

            }

            return false;

        } else {

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.isforgeversioncorrect.empty"));
            return false;
        }
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
    public void createConfigurationFile() {
        // TODO: Move to class Utilities.ConfigUtilities
        List<String> clientMods, copyDirs, encounteredErrors;

        clientMods = new ArrayList<>(100);
        copyDirs = new ArrayList<>(100);
        encounteredErrors = new ArrayList<>(100);

        String[] tmpClientMods, tmpCopyDirs;

        boolean includeServerInstallation,
                includeServerIcon,
                includeServerProperties,
                includeZipCreation;

        String modpackDir,
                javaPath,
                minecraftVersion,
                modLoader,
                modLoaderVersion,
                serverIconPath,
                serverPropertiesPath,
                tmpModpackDir,
                tmpServerIcon,
                tmpServerProperties,
                javaArgs,
                serverPackSuffix;

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
                } while (!checkModpackDir(tmpModpackDir, encounteredErrors));

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
                    clientMods = APPLICATIONPROPERTIES.getListFallbackMods();

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

                } while (!checkCopyDirs(copyDirs, modpackDir, encounteredErrors));

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

//---------------------------------------------------------------------------PATH TO THE CUSTOM SERVER-ICON.PNG---------
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.custom.icon.enter"));

            do {

                do {
                    /* This log is meant to be read by the user, therefore we allow translation. */
                    System.out.print(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.custom.icon.path"));
                    tmpServerIcon = reader.nextLine();

                } while (!checkIconAndProperties(tmpServerIcon));

                /* This log is meant to be read by the user, therefore we allow translation. */
                LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.checkreturn"), tmpServerIcon));
                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.custom.icon.end"));

                System.out.print(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.answer") + " ");

            } while (!readBoolean());

            serverIconPath = tmpServerIcon.replace("\\", "/");
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.checkreturn"), serverIconPath));
            System.out.println();

//-------------------------------------------------------------------------PATH TO THE CUSTOM SERVER.PROPERTIES---------

            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.custom.properties.enter"));

            do {

                do {
                    /* This log is meant to be read by the user, therefore we allow translation. */
                    System.out.print(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.custom.properties.path"));
                    tmpServerProperties = reader.nextLine();
                } while (!checkIconAndProperties(tmpServerProperties));

                /* This log is meant to be read by the user, therefore we allow translation. */
                LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.checkreturn"), tmpServerProperties));
                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.custom.properties.end"));

                System.out.print(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.answer") + " ");

            } while (!readBoolean());

            serverPropertiesPath = tmpServerProperties.replace("\\", "/");
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.checkreturn"), serverPropertiesPath));
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

            modLoader = getModLoaderCase(modLoader);

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.checkreturn"), modLoader));
            System.out.println();

//----------------------------------------------------------------------------VERSION OF MODLOADER MODPACK USES---------
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.modloaderversion.enter"), modLoader));

            do {
                System.out.print(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.modloaderversion.cli") + " ");
                modLoaderVersion = reader.nextLine();

            } while (!checkModloaderVersion(modLoader, modLoaderVersion, minecraftVersion));

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

            //noinspection RedundantStringFormatCall
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

//------------------------------------------------------------------------------SUFFIX TO APPEND TO SERVER PACK---------

            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.serverpacksuffix.cli"));

            System.out.print(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.serverpacksuffix.enter"));
            serverPackSuffix = reader.nextLine();

//------------------------------------------------------------------------------PRINT CONFIG TO CONSOLE AND LOG---------
            printConfigurationModel(modpackDir,
                    clientMods,
                    copyDirs,
                    includeServerInstallation,
                    javaPath,
                    minecraftVersion,
                    modLoader,
                    modLoaderVersion,
                    includeServerIcon,
                    includeServerProperties,
                    includeZipCreation,
                    javaArgs,
                    serverPackSuffix,
                    serverIconPath,
                    serverPropertiesPath);

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
                serverIconPath,
                serverPropertiesPath,
                includeServerInstallation,
                javaPath,
                minecraftVersion,
                modLoader,
                modLoaderVersion,
                includeServerIcon,
                includeServerProperties,
                includeZipCreation,
                javaArgs,
                serverPackSuffix,
                APPLICATIONPROPERTIES.FILE_CONFIG,
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
    public List<String> readStringArray() {

        // TODO: Move to class Utilities.ConfigUtilities

        Scanner readerArray = new Scanner(System.in);

        ArrayList<String> result = new ArrayList<>(100);

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

        // TODO: Move to class Utilities.StringUtilities

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

        // TODO: Move to class Utilities.ListUtilities

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
    public boolean readBoolean() {

        // TODO: Move to class Utilities.BooleanUtilities

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

    /**
     * Convenience method to write a new configuration file with the {@link ConfigurationModel} passed to it. Passes all
     * important fields from an instance of {@link ConfigurationModel} to {@link #writeConfigToFile(String, List, List, String, String, boolean, String, String, String, String, boolean, boolean, boolean, String, String, File, boolean)}.
     * @author Griefed
     * @param configurationModel Instance of {@link ConfigurationModel} to write to a file.
     * @param fileName The file to write to.
     * @param isTemporary Whether the file is temporary.
     * @return Boolean. Returns true if the configuration file has been successfully written and old ones replaced.
     */
    public boolean writeConfigToFile(ConfigurationModel configurationModel, File fileName, boolean isTemporary) {

        // TODO: Move to class Utilities.ConfigUtilities

        return writeConfigToFile(
                configurationModel.getModpackDir(),
                configurationModel.getClientMods(),
                configurationModel.getCopyDirs(),
                configurationModel.getServerIconPath(),
                configurationModel.getServerPropertiesPath(),
                configurationModel.getIncludeServerInstallation(),
                configurationModel.getJavaPath(),
                configurationModel.getMinecraftVersion(),
                configurationModel.getModLoader(),
                configurationModel.getModLoaderVersion(),
                configurationModel.getIncludeServerIcon(),
                configurationModel.getIncludeServerProperties(),
                configurationModel.getIncludeZipCreation(),
                configurationModel.getJavaArgs(),
                configurationModel.getServerPackSuffix(),
                fileName,
                isTemporary
        );
    }

    /** Writes a new configuration file with the parameters passed to it.
     * @author whitebear60
     * @author Griefed
     * @param modpackDir String. The path to the modpack.
     * @param clientMods List, String. List of clientside-only mods.
     * @param copyDirs List, String. List of directories to include in server pack.
     * @param serverIconPath String. The path to the custom server-icon.png to include in the server pack.
     * @param serverPropertiesPath String. The path to the custom server.properties to include in the server pack.
     * @param includeServer Boolean. Whether the modloader server software should be installed.
     * @param javaPath String. Path to the java executable/binary.
     * @param minecraftVersion String. Minecraft version used by the modpack and server pack.
     * @param modLoader String. Modloader used by the modpack and server pack. Ether Forge or Fabric.
     * @param modLoaderVersion String. Modloader version used by the modpack and server pack.
     * @param includeIcon Boolean. Whether to include a server-icon in the server pack.
     * @param includeProperties Boolean. Whether to include a properties file in the server pack.
     * @param includeZip Boolean. Whether to create a ZIP-archive of the server pack, excluding Mojang's Minecraft server JAR.
     * @param javaArgs String. Java arguments to write the start-scripts with.
     * @param serverPackSuffix String. Suffix to append to the server pack to be generated.
     * @param fileName The name under which to write the new configuration file.
     * @param isTemporary Decides whether to delete existing config-file. If isTemporary is false, existing config files will be deleted before writing the new file.
     * @return Boolean. Returns true if the configuration file has been successfully written and old ones replaced.
     */
    public boolean writeConfigToFile(String modpackDir,
                                     List<String> clientMods,
                                     List<String> copyDirs,
                                     String serverIconPath,
                                     String serverPropertiesPath,
                                     boolean includeServer,
                                     String javaPath,
                                     String minecraftVersion,
                                     String modLoader,
                                     String modLoaderVersion,
                                     boolean includeIcon,
                                     boolean includeProperties,
                                     boolean includeZip,
                                     String javaArgs,
                                     String serverPackSuffix,
                                     File fileName,
                                     boolean isTemporary) {

        // TODO: Move to class Utilities.ConfigUtilities

        boolean configWritten = false;

        if (javaArgs.equals("")) {
            javaArgs = "empty";
        }

        //Griefed: What the fuck. This reads like someone having a stroke. What have I created here?
        String configString = String.format(
                        "%s\nmodpackDir = \"%s\"\n\n" +
                        "%s\nclientMods = %s\n\n" +
                        "%s\ncopyDirs = %s\n\n" +
                        "%s\nserverIconPath = \"%s\"\n\n" +
                        "%s\nserverPropertiesPath = \"%s\"\n\n" +
                        "%s\nincludeServerInstallation = %b\n\n" +
                        "%s\njavaPath = \"%s\"\n\n" +
                        "%s\nminecraftVersion = \"%s\"\n\n" +
                        "%s\nmodLoader = \"%s\"\n\n" +
                        "%s\nmodLoaderVersion = \"%s\"\n\n" +
                        "%s\nincludeServerIcon = %b\n\n" +
                        "%s\nincludeServerProperties = %b\n\n" +
                        "%s\nincludeZipCreation = %b\n\n" +
                        "%s\njavaArgs = \"%s\"\n\n" +
                        "%s\nserverPackSuffix = \"%s\"",
                LOCALIZATIONMANAGER.getLocalizedString("configuration.writeconfigtofile.modpackdir"), modpackDir.replace("\\","/"),
                LOCALIZATIONMANAGER.getLocalizedString("configuration.writeconfigtofile.clientmods"), encapsulateListElements(clientMods),
                LOCALIZATIONMANAGER.getLocalizedString("configuration.writeconfigtofile.copydirs"), encapsulateListElements(copyDirs),
                LOCALIZATIONMANAGER.getLocalizedString("configuration.writeconfigtofile.custom.icon"), serverIconPath,
                LOCALIZATIONMANAGER.getLocalizedString("configuration.writeconfigtofile.custom.properties"), serverPropertiesPath,
                LOCALIZATIONMANAGER.getLocalizedString("configuration.writeconfigtofile.includeserverinstallation"), includeServer,
                LOCALIZATIONMANAGER.getLocalizedString("configuration.writeconfigtofile.javapath"), javaPath.replace("\\","/"),
                LOCALIZATIONMANAGER.getLocalizedString("configuration.writeconfigtofile.minecraftversion"), minecraftVersion,
                LOCALIZATIONMANAGER.getLocalizedString("configuration.writeconfigtofile.modloader"), modLoader,
                LOCALIZATIONMANAGER.getLocalizedString("configuration.writeconfigtofile.modloaderversion"), modLoaderVersion,
                LOCALIZATIONMANAGER.getLocalizedString("configuration.writeconfigtofile.includeservericon"), includeIcon,
                LOCALIZATIONMANAGER.getLocalizedString("configuration.writeconfigtofile.includeserverproperties"), includeProperties,
                LOCALIZATIONMANAGER.getLocalizedString("configuration.writeconfigtofile.includezipcreation"), includeZip,
                LOCALIZATIONMANAGER.getLocalizedString("configuration.writeconfigtofile.javaargs"), javaArgs,
                LOCALIZATIONMANAGER.getLocalizedString("configuration.writeconfigtofile.serverpacksuffix"), serverPackSuffix
        );

        if (!isTemporary) {
            if (APPLICATIONPROPERTIES.FILE_CONFIG.exists()) {
                boolean delConf = APPLICATIONPROPERTIES.FILE_CONFIG.delete();
                if (delConf) {
                    /* This log is meant to be read by the user, therefore we allow translation. */
                    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.info.writeconfigtofile.config"));
                } else {
                    /* This log is meant to be read by the user, therefore we allow translation. */
                    LOG.error(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.error.writeconfigtofile.config"));
                }
            }
            if (APPLICATIONPROPERTIES.FILE_CONFIG_OLD.exists()) {
                boolean delOldConf = APPLICATIONPROPERTIES.FILE_CONFIG_OLD.delete();
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

        // TODO: Move to class Utilities.ConfigUtilities

        List<String> configurationAsList = new ArrayList<>(100);

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
        configurationAsList.add(String.valueOf(configurationModel.getIncludeZipCreation()));

        LOG.debug(String.format("Configuration to pass to addons is: %s", configurationAsList));

        return configurationAsList;
    }
}