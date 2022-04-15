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
import de.griefed.serverpackcreator.spring.serverpack.ServerPackModel;
import de.griefed.serverpackcreator.utilities.ConfigUtilities;
import de.griefed.serverpackcreator.utilities.commonutilities.Utilities;
import de.griefed.serverpackcreator.versionmeta.VersionMeta;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private final VersionMeta VERSIONMETA;
    private final ApplicationProperties APPLICATIONPROPERTIES;
    private final Utilities UTILITIES;
    private final ConfigUtilities CONFIGUTILITIES;

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
     * @param injectedVersionMeta Instance of {@link VersionMeta} required for everything version-related.
     * @param injectedUtilities Instance of {@link Utilities}.
     * @param injectedConfigUtilities Instance of {@link ConfigUtilities}.
     * @throws IOException if the {@link VersionMeta} could not be instantiated.
     */
    @Autowired
    public ConfigurationHandler(LocalizationManager injectedLocalizationManager, CurseCreateModpack injectedCurseCreateModpack,
                                VersionMeta injectedVersionMeta, ApplicationProperties injectedApplicationProperties,
                                Utilities injectedUtilities, ConfigUtilities injectedConfigUtilities) throws IOException {

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

        if (injectedVersionMeta == null) {
            this.VERSIONMETA = new VersionMeta(APPLICATIONPROPERTIES);
        } else {
            this.VERSIONMETA = injectedVersionMeta;
        }

        if (injectedUtilities == null) {
            this.UTILITIES = new Utilities(LOCALIZATIONMANAGER, APPLICATIONPROPERTIES);
        } else {
            this.UTILITIES = injectedUtilities;
        }

        if (injectedConfigUtilities == null) {
            this.CONFIGUTILITIES = new ConfigUtilities(LOCALIZATIONMANAGER, UTILITIES, APPLICATIONPROPERTIES, VERSIONMETA);
        } else {
            this.CONFIGUTILITIES = injectedConfigUtilities;
        }

        if (injectedCurseCreateModpack == null) {
            this.CURSECREATEMODPACK = new CurseCreateModpack(LOCALIZATIONMANAGER, APPLICATIONPROPERTIES, VERSIONMETA, UTILITIES, CONFIGUTILITIES);
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

            configurationModel.setServerPackSuffix(UTILITIES.StringUtils().pathSecureText(config.getOrElse("serverPackSuffix","")));
            configurationModel.setServerIconPath(config.getOrElse("serverIconPath","").replace("\\", "/"));
            configurationModel.setServerPropertiesPath(config.getOrElse("serverPropertiesPath","").replace("\\", "/"));

            configurationModel.setIncludeServerInstallation(UTILITIES.BooleanUtils().convertToBoolean(String.valueOf(config.getOrElse("includeServerInstallation","False"))));
            configurationModel.setIncludeServerIcon(UTILITIES.BooleanUtils().convertToBoolean(String.valueOf(config.getOrElse("includeServerIcon", "False"))));
            configurationModel.setIncludeServerProperties(UTILITIES.BooleanUtils().convertToBoolean(String.valueOf(config.getOrElse("includeServerProperties", "False"))));
            configurationModel.setIncludeZipCreation(UTILITIES.BooleanUtils().convertToBoolean(String.valueOf(config.getOrElse("includeZipCreation","False"))));

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


        /*
         * Run checks on the specified modpack directory.
         * Depending on the setting, various configurations can be acquired automatically.
         *
         * 1. If the modpackDir is an actual directory, every check needs to run. Nothing can be acquired automatically, or rather, we want the user to set everything accordingly.
         * 2. If CurseForge is activated and the specified modpackDir is a CurseForge projectID and fileID combination, create the modpack and gather information from said CurseForge modpack.
         * 3. If the modpackDir is a ZIP-archive, check it and if it is found to be valid, extract it, gather as much information as possible.
         *
         * Last but by no means least: Run final checks.
         */
        if (new File(configurationModel.getModpackDir()).isDirectory()) {

            configHasError = isDir(configurationModel, encounteredErrors);

        } else if (APPLICATIONPROPERTIES.isCurseForgeActivated()) {

            try {

                if (checkCurseForge(configurationModel.getModpackDir(), configurationModel, encounteredErrors) && downloadAndCreateModpack) {
                    configHasError = isCurse(configurationModel, encounteredErrors);
                } else {
                    /* This log is meant to be read by the user, therefore we allow translation. */
                    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.checkconfig.skipmodpackcreation"));
                }

            } catch (InvalidModpackException ex) {

                configHasError = true;
                encounteredErrors.add("The specified project is not a valid Minecraft modpack!");
                LOG.error("The specified project is not a valid Minecraft modpack!", ex);

            } catch (InvalidFileException ex) {

                configHasError = true;
                encounteredErrors.add("The specified file is not a file of this project.");
                LOG.error("The specified file is not a file of this project.", ex);

            } catch (CurseException ex) {

                configHasError = true;
                encounteredErrors.add("The specified project does not exist.");
                LOG.error("The specified project does not exist.", ex);

            }

        } else if (new File(configurationModel.getModpackDir()).isFile() &&
                configurationModel.getModpackDir().substring(configurationModel.getModpackDir().length() - 3).equalsIgnoreCase("zip")) {

            try {
                configHasError = isZip(configurationModel, encounteredErrors);
            } catch (IOException ex) {
                configHasError = true;
                LOG.error("An error occurred whilst working with the ZIP-archive.",ex);
            }

        } else {
            configHasError = true;
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.checkmodpackdir"));
            encounteredErrors.add(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.checkmodpackdir"));
        }

        if (checkModloader(configurationModel.getModLoader())) {

            if (VERSIONMETA.minecraft().checkMinecraftVersion(configurationModel.getMinecraftVersion())) {

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

        return configHasError;
    }

    /**
     * Checks the specified ZIP-archive for validity. In order for a modpack ZIP-archive to be considered valid,
     * it needs to contain the <code>mods</code> and <code>config</code> folders at minimum. If any of <code>manifest.json</code>,
     * <code>minecraftinstance.json</code> or <code>config.json</code> are available, gather as much information from them
     * as possible.
     * @author Griefed
     * @param configurationModel Instance of {@link ConfigurationModel} with a server pack configuration.
     * @param encounteredErrors String List. A list of errors encountered during configuration checks.
     * @return Boolean. Returns false when no errors were encountered.
     */
    private boolean isZip(ConfigurationModel configurationModel, List<String> encounteredErrors) throws IOException {
        boolean configHasError = false;

        // modpackDir points at a ZIP-file. Get the path to the would be modpack directory.
        String destination = String.format("./work/modpacks/%s",
                configurationModel.getModpackDir().substring(
                                configurationModel.getModpackDir().lastIndexOf("/") + 1)
                        .substring(
                                0,
                                configurationModel.getModpackDir().substring(configurationModel.getModpackDir().lastIndexOf("/") + 1).length() - 4
                        )
        );


        if (checkZipArchive(Paths.get(configurationModel.getModpackDir()), encounteredErrors)) return true;

        // Does the modpack extracted from the ZIP-archive already exist?
        if (new File(destination).isDirectory()) {

            int incrementation = 0;

            // Has there been a previous incrementation?
            if (destination.matches(".*_\\d")) {

                incrementation = Integer.parseInt(destination.substring(destination.length() - 1));

                while (new File(destination.substring(0, destination.length()-1) + "_" + incrementation).isDirectory()) {
                    incrementation++;
                }

                destination = destination.substring(0, destination.length()-1) + "_" + incrementation;

            // No previous incrementation, but it exists. Append _0 to prevent overwrite.
            } else {
                while (new File(destination + "_" + incrementation).isDirectory()) {
                    incrementation++;
                }

                destination = destination + "_" + incrementation;
            }

        }

        // Extract the archive to the modpack directory.
        UTILITIES.FileUtils().unzipArchive(configurationModel.getModpackDir(), destination);

        // Expand the already set copyDirs with suggestions from extracted ZIP-archive.
        List<String> newCopyDirs = CONFIGUTILITIES.suggestCopyDirs(destination);
        for (String entry : configurationModel.getCopyDirs()) {
            if (!newCopyDirs.contains(entry)) newCopyDirs.add(entry);
        }
        configurationModel.setCopyDirs(newCopyDirs);

        String packName = null;

        // If various manifests exist, gather as much information as possible.
        // Check CurseForge manifest available if a modpack was exported through a client like Overwolf's CurseForge or through GDLauncher.
        if (new File(String.format("%s/manifest.json", destination)).exists()) {

            try {
                CONFIGUTILITIES.updateConfigModelFromCurseManifest(configurationModel, new File(String.format("%s/manifest.json", destination)));

                // If JSON was acquired, get the name of the modpack and overwrite newDestination using modpack name.
                try {
                    packName = String.format("./work/modpacks/%s",configurationModel.getCurseModpack().get("name").asText());
                } catch (NullPointerException npe) {
                    packName = null;
                }

            } catch (IOException ex) {

                LOG.error("Error parsing CurseForge manifest.json from ZIP-file.",ex);
                encounteredErrors.add(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.zip.manifest"));

                configHasError = true;
            }

        // Check minecraftinstance.json usually created by Overwolf's CurseForge launcher.
        } else if (new File(String.format("%s/minecraftinstance.json", destination)).exists()) {

            try {
                CONFIGUTILITIES.updateConfigModelFromMinecraftInstance(configurationModel, new File(String.format("%s/minecraftinstance.json", destination)));

                // If JSON was acquired, get the name of the modpack and overwrite newDestination using modpack name.
                try {
                    packName = String.format("./work/modpacks/%s",configurationModel.getCurseModpack().get("name").asText());
                } catch (NullPointerException npe) {
                    packName = null;
                }

            } catch (IOException ex) {

                LOG.error("Error parsing minecraftinstance.json from ZIP-file.",ex);
                encounteredErrors.add(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.zip.instance"));

                configHasError = true;
            }

        // Check the config.json usually created by GDLauncher.
        } else if (new File(String.format("%s/config.json", destination)).exists()) {

            try {
                CONFIGUTILITIES.updateConfigModelFromConfigJson(configurationModel, new File(String.format("%s/config.json",destination)));

                // If JSON was acquired, get the name of the modpack and overwrite newDestination using modpack name.
                try {
                    packName = String.format("./work/modpacks/%s",configurationModel.getCurseModpack().get("loader").get("sourceName").asText());
                } catch (NullPointerException npe) {
                    packName = null;
                }

            } catch (IOException ex) {

                LOG.error("Error parsing config.json from ZIP-file.",ex);
                encounteredErrors.add(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.zip.config"));

                configHasError = true;
            }

        // Check mmc-pack.json usually created by MultiMC.
        } else if (new File(String.format("%s/mmc-pack.json", destination)).exists()) {

            try {
                CONFIGUTILITIES.updateConfigModelFromMMCPack(configurationModel, new File(String.format("%s/mmc-pack.json",destination)));

            } catch (IOException ex) {

                LOG.error("Error parsing mmc-pack.json from ZIP-file.",ex);
                encounteredErrors.add(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.zip.mmcpack"));

                configHasError = true;
            }

            try {

                if (new File(String.format("%s/instance.cfg",destination)).exists()) {

                    String name = CONFIGUTILITIES.updateDestinationFromInstanceCfg(
                            new File(
                                    String.format(
                                            "%s/instance.cfg",
                                            destination
                                    )
                            )
                    );

                    if (name != null) packName = name;

                }

            } catch (IOException ex) {
                LOG.error("Couldn't read instance.cfg.",ex);
            }


        }

        // If no json was read from the modpack, we must sadly use the ZIP-files name as the new destination. Sadface.
        if (packName == null) packName = destination;

        // Get the path to the would-be-server-pack with the new destination.
        String wouldBeServerPack = new File(
                String.format(
                        "%s/%s",
                        APPLICATIONPROPERTIES.getDirectoryServerPacks(),
                        packName.substring(
                                packName.lastIndexOf("/") + 1
                        ) + configurationModel.getServerPackSuffix()
                )
        ).getAbsolutePath()
                .replace("\\", "/");

        // Check whether a server pack for the new destination already exists.
        // If it does, we need to change it to avoid overwriting any existing files.
        int incrementation = 0;

        // If no name was acquired from the ZIP-archive, has there been a previous incrementation of the pack?
        if (packName.matches(".*_\\d")) {

            // Has there been a previous incrementation of the would-be-server-pack?
            if (wouldBeServerPack.matches(".*_\\d")) {

                // Increment until both a free would-be-server-pack and packname are found.
                while (new File(wouldBeServerPack.substring(0, wouldBeServerPack.length()-1) + "_" + incrementation).isDirectory() ||
                        new File(packName.substring(0,packName.length() - 1) + "_" + incrementation).isDirectory()) {

                    incrementation++;
                }

                packName = packName + "_" + incrementation;

            // No previous incrementation of the would-be-server-pack.
            } else {

                // Increment until both a free would-be-server-pack and packname are found.
                while (new File(wouldBeServerPack + "_" + incrementation).isDirectory() ||
                        new File(packName.substring(0,packName.length() - 1) + "_" + incrementation).isDirectory()) {

                    incrementation++;
                }

                packName = packName + "_" + incrementation;

            }

        // the modpack has not been extracted yet by a previous run. FREEDOOOOOOM!
        } else {

            // Has there been a previous incrementation of the would-be-server-pack?
            if (wouldBeServerPack.matches(".*_\\d")) {

                // Increment until both a free would-be-server-pack and packname are found.
                while (new File(wouldBeServerPack.substring(0, wouldBeServerPack.length()-1) + "_" + incrementation).isDirectory() ||
                        new File(packName + "_" + incrementation).isDirectory()) {

                    incrementation++;
                }

                packName = packName + "_" + incrementation;

            // No previous incrementation of the would-be-server-pack.
            } else {

                // Increment until both a free would-be-server-pack and packname are found.
                while (new File(wouldBeServerPack + "_" + incrementation).isDirectory() ||
                        new File(packName + "_" + incrementation).isDirectory()) {

                    incrementation++;
                }

                packName = packName + "_" + incrementation;

            }
        }

        // Finally, move to new destination to avoid overwriting of server pack
        FileUtils.moveDirectory(
                new File(destination),
                new File(packName));

        // Last but not least, use the newly acquired packname as the modpack directory.
        configurationModel.setModpackDir(packName);

        // Almost forgot. Does the modpack contain a server-icon or server.properties? If so, include them in the server pack.
        if (new File(packName + "/server-icon.png").exists()) configurationModel.setServerIconPath(packName + "/server-icon.png");
        if (new File(packName + "/server.properties").exists()) configurationModel.setServerPropertiesPath(packName + "/server.properties");

        return configHasError;
    }

    /**
     * Check a given ZIP-archives contents. If the ZIP-archive only contains one directory, or if it contains neither the
     * mods nor the config directories, consider it invalid.
     * @author Griefed
     * @param pathToZip Path to the ZIP-file to check.
     * @param encounteredErrors String List. List of encountered errors for further processing, like printing to logs or
     *                          display in GUI or whatever you want, really.
     * @return Boolean. Returns false if the ZIP-archive is considered valid.
     */
    public boolean checkZipArchive(Path pathToZip, List<String> encounteredErrors) {
        try {
            List<String> foldersInModpackZip = CONFIGUTILITIES.directoriesInModpackZip(pathToZip);

            // If the ZIP-file only contains one directory, assume it is overrides and return true to indicate invalid configuration.
            if (foldersInModpackZip.size() == 1) {

                LOG.error(String.format(
                        LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.zip.overrides"),
                        foldersInModpackZip.get(0)
                ));
                encounteredErrors.add(
                        String.format(
                                LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.zip.overrides"),
                                foldersInModpackZip.get(0)
                        )
                );

                return true;

            // If the ZIP-file does not contain the mods or config directories, consider it invalid.
            } else if (!foldersInModpackZip.contains("mods") || !foldersInModpackZip.contains("config")) {

                LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.zip.modsorconfig"));
                encounteredErrors.add(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.zip.modsorconfig"));

                return true;
            }

        } catch (IOException ex) {

            LOG.error("Couldn't acquire directories in ZIP-file.",ex);
            encounteredErrors.add(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.zip.directories"));

            return true;
        }

        return false;
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
     * @param configurationModel or {@link ServerPackModel}. Instance containing
     *                           all the information about our server pack.
     * @param encounteredErrors List String. A list to which all encountered errors are saved to.
     * @throws InvalidModpackException Thrown if the specified IDs do not match a CurseForge modpack.
     * @throws InvalidFileException Thrown if the specified fileID does not match a file for the projectID.
     * @throws CurseException Thrown if an error occurs working with the CurseForgeAPI.
     * @return Boolean. Returns true if the combination is deemed valid, false if not.
     */
    public boolean checkCurseForge(String modpackDir, ConfigurationModel configurationModel, List<String> encounteredErrors) throws InvalidModpackException, InvalidFileException, CurseException {

        boolean configCorrect = false;

        if (modpackDir.matches("[0-9]{2,},[0-9]{5,}") &&
                Integer.parseInt(modpackDir.split(",")[0]) >= 10 &&
                Integer.parseInt(modpackDir.split(",")[1]) >= 60018) {

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

        directoriesToCopy.removeIf(entry -> entry.matches("^\\s+$") || entry.length() == 0);

        if (directoriesToCopy.isEmpty()) {

            configCorrect = false;

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.checkcopydirs.empty"));

            encounteredErrors.add(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.checkcopydirs.empty"));

        } else if (directoriesToCopy.size() == 1 && directoriesToCopy.get(0).equals("lazy_mode")) {

            LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.warn.checkconfig.copydirs.lazymode0"));
            LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.warn.checkconfig.copydirs.lazymode1"));
            LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.warn.checkconfig.copydirs.lazymode2"));
            LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.warn.checkconfig.copydirs.lazymode3"));
            LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.warn.checkconfig.copydirs.lazymode0"));

        } else {

            if (directoriesToCopy.size() > 1 && directoriesToCopy.contains("lazy_mode")) LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.warn.checkconfig.copydirs.lazymode.ignore"));

            directoriesToCopy.removeIf(entry -> entry.equals("lazy_mode"));

            for (String directory : directoriesToCopy) {

                // Check whether the user specified a source;destination-combination
                if (directory.contains(";")) {

                    String[] sourceFileDestinationFileCombination = directory.split(";");

                    File sourceFileToCheck = new File(String.format("%s/%s", modpackDir, sourceFileDestinationFileCombination[0]));

                    if (!new File(String.format("%s/%s", modpackDir, sourceFileDestinationFileCombination[0])).isFile() &&
                            !new File(String.format("%s/%s", modpackDir, sourceFileDestinationFileCombination[0])).isDirectory() &&
                            !new File(sourceFileDestinationFileCombination[0]).isFile() &&
                            !new File(sourceFileDestinationFileCombination[0]).isDirectory()
                    ) {

                        configCorrect = false;

                        /* This log is meant to be read by the user, therefore we allow translation. */
                        LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.checkcopydirs.filenotfound"), sourceFileToCheck));

                        encounteredErrors.add(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.checkcopydirs.filenotfound"), sourceFileToCheck));

                    }

                // Add an entry to the list of directories/files to exclude if it starts with !
                } else if (directory.startsWith("!")) {

                    File fileOrDirectory = new File(String.format("%s/%s",modpackDir, directory.substring(1)));

                    if (fileOrDirectory.isFile()) {
                        LOG.warn("File " + directory.substring(1) + " will be ignored.");
                    } else if (fileOrDirectory.isDirectory()) {
                        LOG.warn("Directory " + directory.substring(1) + " will be ignored.");
                    } else {
                        LOG.debug("What? " + fileOrDirectory + " is neither a file nor directory.");
                    }

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
                checkedJavaPath = UTILITIES.SystemUtils().acquireJavaPathFromSystem();

                LOG.debug("Automatically acquired path to Java installation: " + checkedJavaPath);
            }

        } catch (NullPointerException ex) {

            checkedJavaPath = UTILITIES.SystemUtils().acquireJavaPathFromSystem();

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
     * Check the given Minecraft and modloader versions for the specified modloader.
     * @author Griefed
     * @param modloader String. The passed modloader which determines whether the check for Forge or Fabric is called.
     * @param modloaderVersion String. The version of the modloader which is checked against the corresponding modloaders manifest.
     * @param minecraftVersion String. The version of Minecraft used for checking the Forge version.
     * @return Boolean. Returns true if the specified modloader version was found in the corresponding manifest.
     */
    public boolean checkModloaderVersion(String modloader, String modloaderVersion, String minecraftVersion) {

        if (modloader.equalsIgnoreCase("Forge") && VERSIONMETA.forge().checkForgeAndMinecraftVersion(minecraftVersion,modloaderVersion)) {

            return true;

        } else if (modloader.equalsIgnoreCase("Fabric") && VERSIONMETA.fabric().checkFabricVersion(modloaderVersion)) {

            return true;

        } else {

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.checkmodloaderversion"));

            return false;

        }
    }
}