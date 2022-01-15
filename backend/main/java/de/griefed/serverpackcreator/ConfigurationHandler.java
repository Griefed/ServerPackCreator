/* Copyright (C) 2022  Griefed
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
import com.therandomlabs.curseapi.CurseAPI;
import com.therandomlabs.curseapi.CurseException;
import com.typesafe.config.*;
import de.griefed.serverpackcreator.curseforge.CurseCreateModpack;
import de.griefed.serverpackcreator.curseforge.InvalidFileException;
import de.griefed.serverpackcreator.curseforge.InvalidModpackException;
import de.griefed.serverpackcreator.i18n.LocalizationManager;
import de.griefed.serverpackcreator.spring.models.ServerPack;
import de.griefed.serverpackcreator.utilities.*;
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
    private final BooleanUtilities BOOLEANUTILITIES;
    private final ConfigUtilities CONFIGUTILITIES;
    private final ListUtilities LISTUTILITIES;
    private final StringUtilities STRINGUTILITIES;
    private final SystemUtilities SYSTEMUTILITIES;

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
     * @param injectedBooleanUtilities Instance of {@link BooleanUtilities}.
     * @param injectedListUtilities Instance of {@link ListUtilities}.
     * @param injectedStringUtilities Instance of {@link StringUtilities}.
     * @param injectedSystemUtilities Instance of {@link SystemUtilities}.
     * @param injectedConfigUtilities Instance of {@link ConfigUtilities}.
     */
    @Autowired
    public ConfigurationHandler(LocalizationManager injectedLocalizationManager, CurseCreateModpack injectedCurseCreateModpack,
                                VersionLister injectedVersionLister, ApplicationProperties injectedApplicationProperties,
                                BooleanUtilities injectedBooleanUtilities, ListUtilities injectedListUtilities,
                                StringUtilities injectedStringUtilities, SystemUtilities injectedSystemUtilities,
                                ConfigUtilities injectedConfigUtilities) {

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

        if (injectedVersionLister == null) {
            this.VERSIONLISTER = new VersionLister(APPLICATIONPROPERTIES);
        } else {
            this.VERSIONLISTER = injectedVersionLister;
        }


        if (injectedBooleanUtilities == null) {
            this.BOOLEANUTILITIES = new BooleanUtilities(LOCALIZATIONMANAGER, APPLICATIONPROPERTIES);
        } else {
            this.BOOLEANUTILITIES = injectedBooleanUtilities;
        }

        if (injectedListUtilities == null) {
            this.LISTUTILITIES = new ListUtilities();
        } else {
            this.LISTUTILITIES = injectedListUtilities;
        }

        if (injectedStringUtilities == null) {
            this.STRINGUTILITIES = new StringUtilities();
        } else {
            this.STRINGUTILITIES = injectedStringUtilities;
        }

        if (injectedSystemUtilities == null) {
            this.SYSTEMUTILITIES = new SystemUtilities();
        } else {
            this.SYSTEMUTILITIES = injectedSystemUtilities;
        }

        if (injectedConfigUtilities == null) {
            this.CONFIGUTILITIES = new ConfigUtilities(LOCALIZATIONMANAGER, BOOLEANUTILITIES, LISTUTILITIES, APPLICATIONPROPERTIES, STRINGUTILITIES);
        } else {
            this.CONFIGUTILITIES = injectedConfigUtilities;
        }

        if (injectedCurseCreateModpack == null) {
            this.CURSECREATEMODPACK = new CurseCreateModpack(LOCALIZATIONMANAGER, APPLICATIONPROPERTIES, VERSIONLISTER, BOOLEANUTILITIES, LISTUTILITIES, STRINGUTILITIES, CONFIGUTILITIES);
        } else {
            this.CURSECREATEMODPACK = injectedCurseCreateModpack;
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

            configurationModel.setIncludeServerInstallation(BOOLEANUTILITIES.convertToBoolean(String.valueOf(config.getOrElse("includeServerInstallation","False"))));
            configurationModel.setIncludeServerIcon(BOOLEANUTILITIES.convertToBoolean(String.valueOf(config.getOrElse("includeServerIcon", "False"))));
            configurationModel.setIncludeServerProperties(BOOLEANUTILITIES.convertToBoolean(String.valueOf(config.getOrElse("includeServerProperties", "False"))));
            configurationModel.setIncludeZipCreation(BOOLEANUTILITIES.convertToBoolean(String.valueOf(config.getOrElse("includeZipCreation","False"))));

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

            encounteredErrors.add(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.servericon"),configurationModel.getServerIconPath()));

        }

        if (!checkIconAndProperties(configurationModel.getServerPropertiesPath())) {

            configHasError = true;

            encounteredErrors.add(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.serverproperties"),configurationModel.getServerPropertiesPath()));

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
            CONFIGUTILITIES.printConfigurationModel(configurationModel);
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
                encounteredErrors.add(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.minecraft"));

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

            LOG.info("IMPORTANT!!! - Modpack directory matches CurseForge projectID and fileID format. However, the CurseForge module is currently disabled due to CurseForge changing their API and the way one can access it.");
            LOG.info("IMPORTANT!!! - Downloading and installing a modpack is disabled until further notice.");
            // TODO: Reactivate once custom implementation of CurseForgeAPI has been implemented and CurseForge has provided me with an API key
            encounteredErrors.add("IMPORTANT!!! - Modpack directory matches CurseForge projectID and fileID format. However, the CurseForge module is currently disabled due to CurseForge changing their API and the way one can access it.");
            encounteredErrors.add("IMPORTANT!!! - Downloading and installing a modpack is disabled until further notice.");

            return false;

//            String[] curseForgeIDCombination;
//            curseForgeIDCombination = modpackDir.split(",");
//
//            int curseProjectID = Integer.parseInt(curseForgeIDCombination[0]);
//            int curseFileID = Integer.parseInt(curseForgeIDCombination[1]);
//
//            CurseProject curseProject = null;
//
//            try {
//
//                if (CurseAPI.project(curseProjectID).isPresent()) {
//
//                    curseProject = CurseAPI.project(curseProjectID).get();
//
//                    if (!curseProject.game().name().equalsIgnoreCase("Minecraft")) {
//                        LOG.debug("CurseForge game for " + curseProject.name() + " (id: " + curseProjectID + ") is: " + curseProject.game().name());
//                        throw new InvalidModpackException(curseProjectID, curseProject.name());
//                    }
//
//                    if (!curseProject.categorySection().name().equalsIgnoreCase("Modpacks")) {
//                        LOG.debug("CurseForge game for " + curseProject.name() + " (id: " + curseProjectID + ") is: " + curseProject.game().name());
//                        throw new InvalidModpackException(curseProjectID, curseProject.name());
//                    }
//
//                    //noinspection UnusedAssignment
//                    configCorrect = true;
//
//                    configurationModel.setProjectID(curseProjectID);
//                    configurationModel.setProjectName(CURSECREATEMODPACK.retrieveProjectName(curseProjectID));
//                }
//
//            } catch (NoSuchElementException ex) {
//
//                //noinspection UnusedAssignment
//                configCorrect = false;
//
//                /* This log is meant to be read by the user, therefore we allow translation. */
//                LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.iscurse.project"), curseProjectID), ex);
//                encounteredErrors.add(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.iscurse.project"), curseProjectID));
//
//            }
//
//            try {
//                if (curseProject != null) {
//
//                    if (curseProject.refreshFiles().fileWithID(curseFileID) != null) {
//
//                        configurationModel.setFileID(curseFileID);
//                        configurationModel.setFileName(CURSECREATEMODPACK.retrieveFileName(curseProjectID, curseFileID));
//                        configurationModel.setFileDiskName(CURSECREATEMODPACK.retrieveFileDiskName(curseProjectID,curseFileID));
//
//                        configCorrect = true;
//
//                    } else {
//
//                        encounteredErrors.add(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.iscurse.file"), curseFileID));
//                        throw new InvalidFileException(curseFileID);
//                    }
//                } else {
//
//                    encounteredErrors.add("Specified CurseForge project " + curseProjectID + " could not be found.");
//                    throw new CurseException("Project was null. Does the specified project " + curseProjectID + " exist?");
//                }
//
//            } catch (CurseException | NoSuchElementException ex) {
//
//                configCorrect = false;
//
//                /* This log is meant to be read by the user, therefore we allow translation. */
//                LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.iscurse.file"), curseFileID), ex);
//                encounteredErrors.add(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.iscurse.file"), curseFileID));
//
//            }
//
//            /* This log is meant to be read by the user, therefore we allow translation. */
//            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.checkcurseforge.info"));
//            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.checkcurseforge.return"), configurationModel.getProjectID(), configurationModel.getFileID()));
//            LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.warn.checkcurseforge.warn"));

        } else {
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.warn.checkcurseforge.warn2"));
        }

        return configCorrect;
    }

    /**
     * Print all encountered errors to logs.
     * @author Griefed
     * @param encounteredErrors List String. A list of all errors which were encountered during a configuration check.
     */
    private void printEncounteredErrors(List<String> encounteredErrors) {

        LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.encountered"),encounteredErrors.size()));

        //noinspection UnusedAssignment
        int encounteredErrorNumber = 0;

        for (int i = 0; i < encounteredErrors.size(); i++) {

            encounteredErrorNumber = i + 1;

            LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.encountered.specific"), encounteredErrorNumber, encounteredErrors.get(i)));
        }
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

            encounteredErrors.add(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.modpackdirectory"), modpackDir));

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
                checkedJavaPath = SYSTEMUTILITIES.acquireJavaPathFromSystem();

                LOG.debug("Automatically acquired path to Java installation: " + checkedJavaPath);
            }

        } catch (NullPointerException ex) {

            checkedJavaPath = SYSTEMUTILITIES.acquireJavaPathFromSystem();

            LOG.debug("Automatically acquired path to Java installation: " + checkedJavaPath);

        }

        return checkedJavaPath;
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
}