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
import com.typesafe.config.ConfigException;
import de.griefed.serverpackcreator.i18n.I18n;
import de.griefed.serverpackcreator.utilities.ConfigUtilities;
import de.griefed.serverpackcreator.utilities.common.FileUtilities;
import de.griefed.serverpackcreator.utilities.common.InvalidFileTypeException;
import de.griefed.serverpackcreator.utilities.common.Utilities;
import de.griefed.serverpackcreator.versionmeta.VersionMeta;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This class revolves around checking and adjusting a given instance of {@link ConfigurationModel}
 * so it can safely be used in {@link ServerPackHandler#run(ConfigurationModel)} later.
 *
 * @author Griefed
 */
@Component
public class ConfigurationHandler {

  private static final Logger LOG = LogManager.getLogger(ConfigurationHandler.class);

  private final I18n I18N;
  private final VersionMeta VERSIONMETA;
  private final ApplicationProperties APPLICATIONPROPERTIES;
  private final Utilities UTILITIES;
  private final ConfigUtilities CONFIGUTILITIES;

  /**
   * <strong>Constructor</strong>
   *
   * <p>Used for Dependency Injection.
   *
   * <p>Receives an instance of {@link I18n} or creates one if the received one is null. Required
   * for use of localization.
   *
   * @param injectedI18n Instance of {@link I18n} required for localized log messages.
   * @param injectedApplicationProperties Instance of {@link Properties} required for various
   *     different things.
   * @param injectedVersionMeta Instance of {@link VersionMeta} required for everything
   *     version-related.
   * @param injectedUtilities Instance of {@link Utilities}.
   * @param injectedConfigUtilities Instance of {@link ConfigUtilities}.
   * @throws IOException if the {@link VersionMeta} could not be instantiated.
   * @author Griefed
   */
  @Autowired
  public ConfigurationHandler(
      I18n injectedI18n,
      VersionMeta injectedVersionMeta,
      ApplicationProperties injectedApplicationProperties,
      Utilities injectedUtilities,
      ConfigUtilities injectedConfigUtilities)
      throws IOException {

    if (injectedApplicationProperties == null) {
      this.APPLICATIONPROPERTIES = new ApplicationProperties();
    } else {
      this.APPLICATIONPROPERTIES = injectedApplicationProperties;
    }

    if (injectedI18n == null) {
      this.I18N = new I18n(APPLICATIONPROPERTIES);
    } else {
      this.I18N = injectedI18n;
    }

    if (injectedVersionMeta == null) {
      this.VERSIONMETA =
          new VersionMeta(
              APPLICATIONPROPERTIES.MINECRAFT_VERSION_MANIFEST_LOCATION(),
              APPLICATIONPROPERTIES.FORGE_VERSION_MANIFEST_LOCATION(),
              APPLICATIONPROPERTIES.FABRIC_VERSION_MANIFEST_LOCATION(),
              APPLICATIONPROPERTIES.FABRIC_INSTALLER_VERSION_MANIFEST_LOCATION(),
              APPLICATIONPROPERTIES.QUILT_VERSION_MANIFEST_LOCATION(),
              APPLICATIONPROPERTIES.QUILT_INSTALLER_VERSION_MANIFEST_LOCATION());
    } else {
      this.VERSIONMETA = injectedVersionMeta;
    }

    if (injectedUtilities == null) {
      this.UTILITIES = new Utilities(I18N, APPLICATIONPROPERTIES);
    } else {
      this.UTILITIES = injectedUtilities;
    }

    if (injectedConfigUtilities == null) {
      this.CONFIGUTILITIES =
          new ConfigUtilities(I18N, UTILITIES, APPLICATIONPROPERTIES, VERSIONMETA);
    } else {
      this.CONFIGUTILITIES = injectedConfigUtilities;
    }
  }

  /**
   * Check the passed configuration-file. If any check returns <code>true</code> then the server
   * pack will not be created. In order to find out which check failed, the user has to check their
   * serverpackcreator.log in the logs-directory.<br>
   * Does not create a modpack if a CurseForge project and file is specified.
   *
   * @param configFile File. The configuration file to check. Must either be an existing file to
   *     load a configuration from or null if you want to use the passed configuration model.
   * @param encounteredErrors List String. A list of errors encountered during configuration checks
   *     which gets printed to the console and log after all checks have run. Gives the user more
   *     detail on what went wrong at which part of their configuration. Can be used to display the
   *     errors, if any were encountered, in a UI or be printed into the console or whatever have
   *     you.
   * @param quietCheck Boolean. Whether the configuration should be printed to the console and logs.
   *     Pass false to quietly check the configuration.
   * @return Boolean. Returns <code>false</code> if the configuration has passed all tests.
   * @author Griefed
   */
  public boolean checkConfiguration(
      @NotNull File configFile, @NotNull List<String> encounteredErrors, boolean quietCheck) {

    ConfigurationModel configurationModel = new ConfigurationModel();

    return checkConfiguration(configFile, configurationModel, encounteredErrors, quietCheck);
  }

  /**
   * Check the passed configuration-file. If any check returns <code>true</code> then the server
   * pack will not be created. In order to find out which check failed, the user has to check their
   * serverpackcreator.log in the logs-directory.
   *
   * @param configFile File. The configuration file to check. Must either be an existing file to
   *     load a configuration from or null if you want to use the passed configuration model.
   * @param quietCheck Boolean. Whether the configuration should be printed to the console and logs.
   *     Pass false to quietly check the configuration.
   * @return Boolean. Returns <code>false</code> if the configuration has passed all tests.
   * @author Griefed
   */
  public boolean checkConfiguration(@NotNull File configFile, boolean quietCheck) {

    List<String> encounteredErrors = new ArrayList<>(100);

    ConfigurationModel configurationModel = new ConfigurationModel();

    return checkConfiguration(configFile, configurationModel, encounteredErrors, quietCheck);
  }

  /**
   * Check the passed configuration-file. If any check returns <code>true</code> then the server
   * pack will not be created. In order to find out which check failed, the user has to check their
   * serverpackcreator.log in the logs-directory.
   *
   * @param configFile File. The configuration file to check. Must either be an existing file to
   *     load a configuration from or null if you want to use the passed configuration model.
   * @param configurationModel ConfigurationModel. Instance of a configuration of a modpack. Can be
   *     used to further display or use any information within, as it may be changed or otherwise
   *     altered by this method.
   * @param quietCheck Boolean. Whether the configuration should be printed to the console and logs.
   *     Pass false to quietly check the configuration.
   * @return Boolean. Returns <code>false</code> if the configuration has passed all tests.
   * @author Griefed
   */
  public boolean checkConfiguration(
      @NotNull File configFile,
      @NotNull ConfigurationModel configurationModel,
      boolean quietCheck) {

    List<String> encounteredErrors = new ArrayList<>(100);

    return checkConfiguration(configFile, configurationModel, encounteredErrors, quietCheck);
  }

  /**
   * Check the passed {@link ConfigurationModel}. If any check returns <code>true</code> then the
   * server pack will not be created. In order to find out which check failed, the user has to check
   * their serverpackcreator.log in the logs-directory.
   *
   * @param configurationModel ConfigurationModel. Instance of a configuration of a modpack. Can be
   *     used to further display or use any information within, as it may be changed or otherwise
   *     altered by this method.
   * @param quietCheck Boolean. Whether the configuration should be printed to the console and logs.
   *     Pass false to quietly check the configuration.
   * @return Boolean. Returns <code>false</code> if the configuration has passed all tests.
   * @author Griefed
   */
  public boolean checkConfiguration(
      @NotNull ConfigurationModel configurationModel, boolean quietCheck) {

    List<String> encounteredErrors = new ArrayList<>(100);

    return checkConfiguration(configurationModel, encounteredErrors, quietCheck);
  }

  /**
   * Check the passed configuration-file. If any check returns <code>true</code> then the server
   * pack will not be created. In order to find out which check failed, the user has to check their
   * serverpackcreator.log in the logs-directory.
   *
   * @param configFile File. The configuration file to check. Must either be an existing file to
   *     load a configuration from or null if you want to use the passed configuration model.
   * @param configurationModel ConfigurationModel. Instance of a configuration of a modpack. Can be
   *     used to further display or use any information within, as it may be changed or otherwise
   *     altered by this method.
   * @param encounteredErrors List String. A list of errors encountered during configuration checks
   *     which gets printed to the console and log after all checks have run. Gives the user more
   *     detail on what went wrong at which part of their configuration. Can be used to display the
   *     errors, if any were encountered, in a UI or be printed into the console or whatever have
   *     you.
   * @param quietCheck Boolean. Whether the configuration should be printed to the console and logs.
   *     Pass false to quietly check the configuration.
   * @return Boolean. Returns <code>false</code> if the configuration has passed all tests.
   * @author Griefed
   */
  public boolean checkConfiguration(
      @NotNull File configFile,
      @NotNull ConfigurationModel configurationModel,
      @NotNull List<String> encounteredErrors,
      boolean quietCheck) {

    FileConfig config = null;

    try {
      config = FileConfig.of(configFile);
    } catch (ConfigException ex) {

      LOG.error(
          "Couldn't parse config file. Consider checking your config file and fixing empty values. If the value needs to be an empty string, leave its value to \"\".");

      /* This log is meant to be read by the user, therefore we allow translation. */
      encounteredErrors.add(I18N.getMessage("configuration.log.error.checkconfig.start"));
    }

    if (config != null) {

      try {
        config.load();
      } catch (ConfigException ex) {

        LOG.error(
            "Couldn't parse config file. Consider checking your config file and fixing empty values. If the value needs to be an empty string, leave its value to \"\".");

        /* This log is meant to be read by the user, therefore we allow translation. */
        encounteredErrors.add(I18N.getMessage("configuration.log.error.checkconfig.start"));
      }

      configurationModel.setClientMods(
          config.getOrElse("clientMods", Collections.singletonList("")));
      configurationModel.setCopyDirs(config.getOrElse("copyDirs", Collections.singletonList("")));
      configurationModel.setModpackDir(config.getOrElse("modpackDir", "").replace("\\", "/"));
      configurationModel.setJavaPath(config.getOrElse("javaPath", "").replace("\\", "/"));

      configurationModel.setMinecraftVersion(config.getOrElse("minecraftVersion", ""));
      configurationModel.setModLoader(config.getOrElse("modLoader", ""));
      configurationModel.setModLoaderVersion(config.getOrElse("modLoaderVersion", ""));
      configurationModel.setJavaArgs(config.getOrElse("javaArgs", ""));

      configurationModel.setServerPackSuffix(
          UTILITIES.StringUtils().pathSecureText(config.getOrElse("serverPackSuffix", "")));
      configurationModel.setServerIconPath(
          config.getOrElse("serverIconPath", "").replace("\\", "/"));
      configurationModel.setServerPropertiesPath(
          config.getOrElse("serverPropertiesPath", "").replace("\\", "/"));

      configurationModel.setIncludeServerInstallation(
          UTILITIES.BooleanUtils()
              .convertToBoolean(
                  String.valueOf(config.getOrElse("includeServerInstallation", "False"))));
      configurationModel.setIncludeServerIcon(
          UTILITIES.BooleanUtils()
              .convertToBoolean(String.valueOf(config.getOrElse("includeServerIcon", "False"))));
      configurationModel.setIncludeServerProperties(
          UTILITIES.BooleanUtils()
              .convertToBoolean(
                  String.valueOf(config.getOrElse("includeServerProperties", "False"))));
      configurationModel.setIncludeZipCreation(
          UTILITIES.BooleanUtils()
              .convertToBoolean(String.valueOf(config.getOrElse("includeZipCreation", "False"))));

    } else {

      LOG.error(
          "Couldn't parse config file. Consider checking your config file and fixing empty values. If the value needs to be an empty string, leave its value to \"\".");

      /* This log is meant to be read by the user, therefore we allow translation. */
      encounteredErrors.add(I18N.getMessage("configuration.log.error.checkconfig.start"));
    }

    return checkConfiguration(configurationModel, encounteredErrors, quietCheck);
  }

  /**
   * Check the passed {@link ConfigurationModel}. If any check returns <code>true</code> then the
   * server pack will not be created. In order to find out which check failed, the user has to check
   * their serverpackcreator.log in the logs-directory.<br>
   * The passed String List <code>encounteredErrors</code> can be used to display the errors, if any
   * were encountered, in a UI or be printed into the console or whatever have you.<br>
   * The passed {@link ConfigurationModel} can be used to further display or use any information
   * within, as it may be changed or otherwise altered by this method.
   *
   * @param configurationModel ConfigurationModel. Instance of a configuration of a modpack. Can be
   *     used to further display or use any information within, as it may be changed or otherwise
   *     altered by this method.
   * @param encounteredErrors List String. A list of errors encountered during configuration checks
   *     which gets printed to the console and log after all checks have run. Gives the user more
   *     detail on what went wrong at which part of their configuration. Can be used to display the
   *     errors, if any were encountered, in a UI or be printed into the console or whatever have
   *     you.
   * @param quietCheck Boolean. Whether the configuration should be printed to the console and logs.
   *     Pass false to quietly check the configuration.
   * @return Boolean. Returns <code>false</code> if all checks are passed.
   * @author Griefed
   */
  public boolean checkConfiguration(
      @NotNull ConfigurationModel configurationModel,
      @NotNull List<String> encounteredErrors,
      boolean quietCheck) {
    boolean configHasError;

    sanitizeLinks(configurationModel);

    LOG.info("Checking configuration...");

    if (configurationModel.getClientMods().isEmpty()) {

      LOG.warn("No clientside-only mods specified. Using fallback list.");
      configurationModel.setClientMods(APPLICATIONPROPERTIES.getListFallbackMods());
    } else {
      configurationModel.setClientMods(configurationModel.getClientMods());
    }

    configurationModel.setJavaPath(
        getJavaPath(configurationModel.getJavaPath().replace("\\", "/")));

    if (!checkIconAndProperties(configurationModel.getServerIconPath())) {

      //noinspection UnusedAssignment
      configHasError = true;

      LOG.error(
          "The specified server-icon does not exist: " + configurationModel.getServerIconPath());

      /* This log is meant to be read by the user, therefore we allow translation. */
      encounteredErrors.add(
          String.format(
              I18N.getMessage("configuration.log.error.servericon"),
              configurationModel.getServerIconPath()));

    } else if (configurationModel.getServerIconPath().length() > 0
        && new File(configurationModel.getServerIconPath()).exists()
        && !UTILITIES.FileUtils().checkReadPermission(configurationModel.getServerIconPath())) {

      //noinspection UnusedAssignment
      configHasError = true;

      LOG.error("No read-permission for " + configurationModel.getServerIconPath());

      /* This log is meant to be read by the user, therefore we allow translation. */
      encounteredErrors.add(
          String.format(
              I18N.getMessage("configuration.log.error.checkcopydirs.read"),
              configurationModel.getServerIconPath()));
    }

    if (!checkIconAndProperties(configurationModel.getServerPropertiesPath())) {

      //noinspection UnusedAssignment
      configHasError = true;

      LOG.error(
          "The specified server.properties does not exist: "
              + configurationModel.getServerPropertiesPath());

      /* This log is meant to be read by the user, therefore we allow translation. */
      encounteredErrors.add(
          String.format(
              I18N.getMessage("configuration.log.error.serverproperties"),
              configurationModel.getServerPropertiesPath()));

    } else if (configurationModel.getServerPropertiesPath().length() > 0
        && new File(configurationModel.getServerPropertiesPath()).exists()
        && !UTILITIES.FileUtils()
            .checkReadPermission(configurationModel.getServerPropertiesPath())) {

      //noinspection UnusedAssignment
      configHasError = true;

      LOG.error("No read-permission for " + configurationModel.getServerPropertiesPath());

      /* This log is meant to be read by the user, therefore we allow translation. */
      encounteredErrors.add(
          String.format(
              I18N.getMessage("configuration.log.error.checkcopydirs.read"),
              configurationModel.getServerPropertiesPath()));
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
    File modpack = new File(configurationModel.getModpackDir());

    if (modpack.isDirectory()) {

      configHasError = isDir(configurationModel, encounteredErrors);

    } else if (modpack.isFile() && modpack.getName().endsWith("zip")) {

      try {
        configHasError = isZip(configurationModel, encounteredErrors);
      } catch (IOException ex) {
        configHasError = true;
        LOG.error("An error occurred whilst working with the ZIP-archive.", ex);
      }

    } else {
      configHasError = true;
      LOG.error("Modpack directory not specified. Please specify an existing directory.");

      /* This log is meant to be read by the user, therefore we allow translation. */
      encounteredErrors.add(I18N.getMessage("configuration.log.error.checkmodpackdir"));
    }

    if (checkModloader(configurationModel.getModLoader())) {

      if (VERSIONMETA.minecraft().checkMinecraftVersion(configurationModel.getMinecraftVersion())) {

        LOG.debug("minecraftVersion setting check passed.");
        LOG.debug("modLoader setting check passed.");

        if (checkModloaderVersion(
            configurationModel.getModLoader(),
            configurationModel.getModLoaderVersion(),
            configurationModel.getMinecraftVersion())) {

          LOG.debug("modLoaderVersion setting check passed.");

        } else {

          configHasError = true;

          LOG.error("There's something wrong with your Modloader version setting.");

          /* This log is meant to be read by the user, therefore we allow translation. */
          encounteredErrors.add(I18N.getMessage("configuration.log.error.checkmodloaderversion"));
        }

      } else {

        configHasError = true;
        LOG.error("There's something wrong with your Minecraft version setting.");

        /* This log is meant to be read by the user, therefore we allow translation. */
        encounteredErrors.add(I18N.getMessage("configuration.log.error.minecraft"));
      }

    } else {

      configHasError = true;

      LOG.error("There's something wrong with your Modloader or Modloader version setting.");

      /* This log is meant to be read by the user, therefore we allow translation. */
      encounteredErrors.add(I18N.getMessage("configuration.log.error.checkmodloader"));
    }

    if (quietCheck) {
      CONFIGUTILITIES.printConfigurationModel(configurationModel);
    }

    if (!configHasError) {

      LOG.info("Config check successful. No errors encountered.");

    } else {

      LOG.error("Config check not successful. Check your config for errors.");
      printEncounteredErrors(encounteredErrors);
    }

    ensureScriptSettingsDefaults(configurationModel);

    return configHasError;
  }

  /**
   * If the in the configuration specified modpack dir is an existing directory, checks are made for
   * valid configuration of: directories to copy to server pack,<br>
   * if includeServerInstallation is <code>true</code>) path to Java executable/binary, Minecraft
   * version, modloader and modloader version.
   *
   * @param configurationModel An instance of {@link ConfigurationModel} which contains the
   *     configuration of the modpack.
   * @param encounteredErrors List String. A list to which all encountered errors are saved to.
   * @return Boolean. Returns true if an error is found during configuration check.
   * @author Griefed
   */
  private boolean isDir(ConfigurationModel configurationModel, List<String> encounteredErrors) {
    boolean configHasError = false;

    if (checkCopyDirs(
        configurationModel.getCopyDirs(), configurationModel.getModpackDir(), encounteredErrors)) {

      LOG.debug("copyDirs setting check passed.");

    } else {

      configHasError = true;
      LOG.error(
          "There's something wrong with your setting of directories to include in your server pack.");

      /* This log is meant to be read by the user, therefore we allow translation. */
      encounteredErrors.add(I18N.getMessage("configuration.log.error.isdir.copydir"));
    }

    return configHasError;
  }

  /**
   * Checks the specified ZIP-archive for validity. In order for a modpack ZIP-archive to be
   * considered valid, it needs to contain the <code>mods</code> and <code>config</code> folders at
   * minimum. If any of <code>manifest.json</code>, <code>minecraftinstance.json</code> or <code>
   * config.json</code> are available, gather as much information from them as possible.
   *
   * @param configurationModel Instance of {@link ConfigurationModel} with a server pack
   *     configuration.
   * @param encounteredErrors String List. A list of errors encountered during configuration checks.
   * @return Boolean. Returns false when no errors were encountered.
   * @author Griefed
   */
  private boolean isZip(ConfigurationModel configurationModel, List<String> encounteredErrors)
      throws IOException {
    boolean configHasError = false;

    // modpackDir points at a ZIP-file. Get the path to the would be modpack directory.
    String destination =
        String.format(
            "./work/modpacks/%s",
            configurationModel
                .getModpackDir()
                .substring(configurationModel.getModpackDir().lastIndexOf("/") + 1)
                .substring(
                    0,
                    configurationModel
                            .getModpackDir()
                            .substring(configurationModel.getModpackDir().lastIndexOf("/") + 1)
                            .length()
                        - 4));

    if (checkZipArchive(Paths.get(configurationModel.getModpackDir()), encounteredErrors)) {
      return true;
    }

    // Does the modpack extracted from the ZIP-archive already exist?
    if (new File(destination).isDirectory()) {

      int incrementation = 0;

      // Has there been a previous incrementation?
      if (destination.matches(".*_\\d")) {

        incrementation = Integer.parseInt(destination.substring(destination.length() - 1));

        while (new File(destination.substring(0, destination.length() - 1) + "_" + incrementation)
            .isDirectory()) {
          incrementation++;
        }

        destination = destination.substring(0, destination.length() - 1) + "_" + incrementation;

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
    // Check CurseForge manifest available if a modpack was exported through a client like
    // Overwolf's CurseForge or through GDLauncher.
    if (new File(String.format("%s/manifest.json", destination)).exists()) {

      try {
        CONFIGUTILITIES.updateConfigModelFromCurseManifest(
            configurationModel, new File(String.format("%s/manifest.json", destination)));

        // If JSON was acquired, get the name of the modpack and overwrite newDestination using
        // modpack name.
        try {

          packName =
              String.format(
                  "./work/modpacks/%s", configurationModel.getCurseModpack().get("name").asText());

        } catch (NullPointerException npe) {

          //noinspection ConstantConditions
          packName = null;
        }

      } catch (IOException ex) {

        LOG.error("Error parsing CurseForge manifest.json from ZIP-file.", ex);

        /* This log is meant to be read by the user, therefore we allow translation. */
        encounteredErrors.add(I18N.getMessage("configuration.log.error.zip.manifest"));

        configHasError = true;
      }

      // Check minecraftinstance.json usually created by Overwolf's CurseForge launcher.
    } else if (new File(String.format("%s/minecraftinstance.json", destination)).exists()) {

      try {
        CONFIGUTILITIES.updateConfigModelFromMinecraftInstance(
            configurationModel, new File(String.format("%s/minecraftinstance.json", destination)));

        // If JSON was acquired, get the name of the modpack and overwrite newDestination using
        // modpack name.
        try {

          packName =
              String.format(
                  "./work/modpacks/%s", configurationModel.getCurseModpack().get("name").asText());

        } catch (NullPointerException npe) {

          //noinspection ConstantConditions
          packName = null;
        }

      } catch (IOException ex) {

        LOG.error("Error parsing minecraftinstance.json from ZIP-file.", ex);

        /* This log is meant to be read by the user, therefore we allow translation. */
        encounteredErrors.add(I18N.getMessage("configuration.log.error.zip.instance"));

        configHasError = true;
      }

      // Check the config.json usually created by GDLauncher.
    } else if (new File(String.format("%s/config.json", destination)).exists()) {

      try {
        CONFIGUTILITIES.updateConfigModelFromConfigJson(
            configurationModel, new File(String.format("%s/config.json", destination)));

        // If JSON was acquired, get the name of the modpack and overwrite newDestination using
        // modpack name.
        try {

          packName =
              String.format(
                  "./work/modpacks/%s",
                  configurationModel.getCurseModpack().get("loader").get("sourceName").asText());

        } catch (NullPointerException npe) {

          //noinspection ConstantConditions
          packName = null;
        }

      } catch (IOException ex) {

        LOG.error("Error parsing config.json from ZIP-file.", ex);

        /* This log is meant to be read by the user, therefore we allow translation. */
        encounteredErrors.add(I18N.getMessage("configuration.log.error.zip.config"));

        configHasError = true;
      }

      // Check mmc-pack.json usually created by MultiMC.
    } else if (new File(String.format("%s/mmc-pack.json", destination)).exists()) {

      try {
        CONFIGUTILITIES.updateConfigModelFromMMCPack(
            configurationModel, new File(String.format("%s/mmc-pack.json", destination)));

      } catch (IOException ex) {

        LOG.error("Error parsing mmc-pack.json from ZIP-file.", ex);

        /* This log is meant to be read by the user, therefore we allow translation. */
        encounteredErrors.add(I18N.getMessage("configuration.log.error.zip.mmcpack"));

        configHasError = true;
      }

      try {

        if (new File(String.format("%s/instance.cfg", destination)).exists()) {

          String name =
              CONFIGUTILITIES.updateDestinationFromInstanceCfg(
                  new File(String.format("%s/instance.cfg", destination)));

          if (name != null) packName = name;
        }

      } catch (IOException ex) {
        LOG.error("Couldn't read instance.cfg.", ex);
      }
    }

    // If no json was read from the modpack, we must sadly use the ZIP-files name as the new
    // destination. Sadface.
    if (packName == null) packName = destination;

    // Get the path to the would-be-server-pack with the new destination.
    String wouldBeServerPack =
        new File(
                String.format(
                    "%s/%s",
                    APPLICATIONPROPERTIES.getDirectoryServerPacks(),
                    packName.substring(packName.lastIndexOf("/") + 1)
                        + configurationModel.getServerPackSuffix()))
            .getAbsolutePath()
            .replace("\\", "/");

    // Check whether a server pack for the new destination already exists.
    // If it does, we need to change it to avoid overwriting any existing files.
    int incrementation = 0;

    // If no name was acquired from the ZIP-archive, has there been a previous incrementation of the
    // pack?
    if (packName.matches(".*_\\d")) {

      // Has there been a previous incrementation of the would-be-server-pack?
      if (wouldBeServerPack.matches(".*_\\d")) {

        // Increment until both a free would-be-server-pack and packname are found.
        while (new File(
                    wouldBeServerPack.substring(0, wouldBeServerPack.length() - 1)
                        + "_"
                        + incrementation)
                .isDirectory()
            || new File(packName.substring(0, packName.length() - 1) + "_" + incrementation)
                .isDirectory()) {

          incrementation++;
        }

        // No previous incrementation of the would-be-server-pack.
      } else {

        // Increment until both a free would-be-server-pack and packname are found.
        while (new File(wouldBeServerPack + "_" + incrementation).isDirectory()
            || new File(packName.substring(0, packName.length() - 1) + "_" + incrementation)
                .isDirectory()) {

          incrementation++;
        }
      }

      // the modpack has not been extracted yet by a previous run. FREEDOOOOOOM!
    } else {

      // Has there been a previous incrementation of the would-be-server-pack?
      if (wouldBeServerPack.matches(".*_\\d")) {

        // Increment until both a free would-be-server-pack and packname are found.
        while (new File(
                    wouldBeServerPack.substring(0, wouldBeServerPack.length() - 1)
                        + "_"
                        + incrementation)
                .isDirectory()
            || new File(packName + "_" + incrementation).isDirectory()) {

          incrementation++;
        }

        // No previous incrementation of the would-be-server-pack.
      } else {

        // Increment until both a free would-be-server-pack and packname are found.
        while (new File(wouldBeServerPack + "_" + incrementation).isDirectory()
            || new File(packName + "_" + incrementation).isDirectory()) {

          incrementation++;
        }
      }
    }

    packName = packName + "_" + incrementation;

    // Finally, move to new destination to avoid overwriting of server pack
    FileUtils.moveDirectory(new File(destination), new File(packName));

    // Last but not least, use the newly acquired packname as the modpack directory.
    configurationModel.setModpackDir(packName);

    // Does the modpack contain a server-icon or server.properties? If so, include
    // them in the server pack.
    if (new File(packName + "/server-icon.png").exists())
      configurationModel.setServerIconPath(packName + "/server-icon.png");
    if (new File(packName + "/server.properties").exists())
      configurationModel.setServerPropertiesPath(packName + "/server.properties");

    return configHasError;
  }

  /**
   * Check a given ZIP-archives contents. If the ZIP-archive only contains one directory, or if it
   * contains neither the mods nor the config directories, consider it invalid.
   *
   * @param pathToZip Path to the ZIP-file to check.
   * @param encounteredErrors String List. List of encountered errors for further processing, like
   *     printing to logs or display in GUI or whatever you want, really.
   * @return Boolean. Returns false if the ZIP-archive is considered valid.
   * @author Griefed
   */
  public boolean checkZipArchive(Path pathToZip, List<String> encounteredErrors) {
    try {
      List<String> foldersInModpackZip = CONFIGUTILITIES.directoriesInModpackZip(pathToZip);

      // If the ZIP-file only contains one directory, assume it is overrides and return true to
      // indicate invalid configuration.
      if (foldersInModpackZip.size() == 1) {

        LOG.error(
            "The ZIP-file you specified only contains one directory: "
                + foldersInModpackZip.get(0)
                + ". ZIP-files for ServerPackCreator must be full modpacks, with all their contents being in the root of the ZIP-file.");

        /* This log is meant to be read by the user, therefore we allow translation. */
        encounteredErrors.add(
            String.format(
                I18N.getMessage("configuration.log.error.zip.overrides"),
                foldersInModpackZip.get(0)));

        return true;

        // If the ZIP-file does not contain the mods or config directories, consider it invalid.
      } else if (!foldersInModpackZip.contains("mods") || !foldersInModpackZip.contains("config")) {

        LOG.error(
            "The ZIP-file you specified does not contain the mods or config directories. What use is a modded server without mods and their configurations?");

        /* This log is meant to be read by the user, therefore we allow translation. */
        encounteredErrors.add(I18N.getMessage("configuration.log.error.zip.modsorconfig"));

        return true;
      }

    } catch (IOException ex) {

      LOG.error("Couldn't acquire directories in ZIP-file.", ex);

      /* This log is meant to be read by the user, therefore we allow translation. */
      encounteredErrors.add(I18N.getMessage("configuration.log.error.zip.directories"));

      return true;
    }

    return false;
  }

  /**
   * Update the script settings and ensure the default keys, with values gathered from the passed
   * {@link ConfigurationModel}, are present:
   *
   * <ol>
   *   <li><code>SPC_SERVERPACKCREATOR_VERSION_SPC</code> : <code>
   *       ServerPackCreator version with which the scripts were created</code>
   *   <li><code>SPC_MINECRAFT_VERSION_SPC</code> : <code>Minecraft version of the modpack</code>
   *   <li><code>SPC_MINECRAFT_SERVER_URL_SPC</code> : <code>Download-URL to the Minecraft server
   *       </code>
   *   <li><code>SPC_MODLOADER_SPC</code> : <code>The modloader of the modpack</code>
   *   <li><code>SPC_MODLOADER_VERSION_SPC</code> : <code>The modloader version of the modpack
   *       </code>
   *   <li><code>SPC_JAVA_ARGS_SPC</code> : <code>The JVM args to be used to run the server</code>
   *   <li><code>SPC_JAVA_SPC</code> : <code>
   *       Path to the java installation to be used to run the server</code>
   *   <li><code>SPC_FABRIC_INSTALLER_VERSION_SPC</code> : <code>
   *       Most recent version of the Fabric installer at the time of creating the scripts</code>
   *   <li><code>SPC_QUILT_INSTALLER_VERSION_SPC</code> : <code>
   *       Most recent version of the Quilt installer at the time of creating the scripts</code>
   * </ol>
   *
   * @param configurationModel {@link ConfigurationModel} in which to ensure the default key-value
   *     pairs are present.
   * @author Griefed
   */
  private void ensureScriptSettingsDefaults(ConfigurationModel configurationModel) {

    HashMap<String, String> scriptSettings = configurationModel.getScriptSettings();

    if (!VERSIONMETA.minecraft().getServer(configurationModel.getMinecraftVersion()).isPresent()
        || !VERSIONMETA
            .minecraft()
            .getServer(configurationModel.getMinecraftVersion())
            .get()
            .url()
            .isPresent()) {

      scriptSettings.put(
          "SPC_MINECRAFT_SERVER_URL_SPC","");

    } else {
      scriptSettings.put(
          "SPC_MINECRAFT_SERVER_URL_SPC",
          VERSIONMETA
              .minecraft()
              .getServer(configurationModel.getMinecraftVersion())
              .get()
              .url()
              .get()
              .toString());
    }

    scriptSettings.put(
        "SPC_SERVERPACKCREATOR_VERSION_SPC", APPLICATIONPROPERTIES.SERVERPACKCREATOR_VERSION());
    scriptSettings.put("SPC_MINECRAFT_VERSION_SPC", configurationModel.getMinecraftVersion());

    scriptSettings.put("SPC_MODLOADER_SPC", configurationModel.getModLoader());
    scriptSettings.put("SPC_MODLOADER_VERSION_SPC", configurationModel.getModLoaderVersion());
    scriptSettings.put("SPC_JAVA_ARGS_SPC", configurationModel.getJavaArgs());

    // To be enhanced in a later milestone
    scriptSettings.put("SPC_JAVA_SPC", "java");

    scriptSettings.put(
        "SPC_FABRIC_INSTALLER_VERSION_SPC", VERSIONMETA.fabric().releaseInstallerVersion());
    scriptSettings.put(
        "SPC_QUILT_INSTALLER_VERSION_SPC", VERSIONMETA.quilt().releaseInstallerVersion());

    configurationModel.setScriptSettings(scriptSettings);
  }

  /**
   * Sanitize any and all links in a given instance of {@link ConfigurationModel} modpack-directory,
   * server-icon path, server-properties path, Java path and copy-directories entries.
   *
   * @param configurationModel Instance of {@link ConfigurationModel} in which to sanitize links to
   *     their respective destinations.
   * @author Griefed
   */
  private void sanitizeLinks(ConfigurationModel configurationModel) {

    LOG.info("Checking configuration for links...");

    if (configurationModel.getModpackDir().length() > 0
        && UTILITIES.FileUtils().isLink(configurationModel.getModpackDir())) {
      try {
        configurationModel.setModpackDir(
            UTILITIES.FileUtils().resolveLink(configurationModel.getModpackDir()));

        LOG.info("Resolved modpack directory link to: " + configurationModel.getModpackDir());

      } catch (InvalidFileTypeException | IOException ex) {
        LOG.error("Couldn't resolve link for modpack directory.", ex);
      }
    }

    if (configurationModel.getServerIconPath().length() > 0
        && UTILITIES.FileUtils().isLink(configurationModel.getServerIconPath())) {
      try {
        configurationModel.setServerIconPath(
            UTILITIES.FileUtils().resolveLink(configurationModel.getServerIconPath()));

        LOG.info("Resolved server-icon link to: " + configurationModel.getServerIconPath());

      } catch (InvalidFileTypeException | IOException ex) {
        LOG.error("Couldn't resolve link for server-icon.", ex);
      }
    }

    if (configurationModel.getServerPropertiesPath().length() > 0
        && UTILITIES.FileUtils().isLink(configurationModel.getServerPropertiesPath())) {
      try {
        configurationModel.setServerPropertiesPath(
            UTILITIES.FileUtils().resolveLink(configurationModel.getServerPropertiesPath()));

        LOG.info(
            "Resolved server-properties link to: " + configurationModel.getServerPropertiesPath());

      } catch (InvalidFileTypeException | IOException ex) {
        LOG.error("Couldn't resolve link for server-properties.", ex);
      }
    }

    if (configurationModel.getJavaPath().length() > 0
        && UTILITIES.FileUtils().isLink(configurationModel.getJavaPath())) {
      try {
        configurationModel.setJavaPath(
            UTILITIES.FileUtils().resolveLink(configurationModel.getJavaPath()));

        LOG.info("Resolved Java link to: " + configurationModel.getJavaPath());

      } catch (InvalidFileTypeException | IOException ex) {
        LOG.error("Couldn't resolve link for Java path.", ex);
      }
    }

    if (!configurationModel.getCopyDirs().isEmpty()) {
      List<String> copyDirs = configurationModel.getCopyDirs();
      boolean copyDirChanges = false;

      for (int i = 0; i < copyDirs.size(); i++) {

        if (copyDirs.get(i).contains(";")) {
          // Source;Destination-combination

          String[] entries = copyDirs.get(i).split(";");

          if (UTILITIES.FileUtils().isLink(entries[0])) {
            try {
              copyDirs.set(i, UTILITIES.FileUtils().resolveLink(entries[0]) + ";" + entries[1]);

              LOG.info("Resolved copy-directories link to: " + copyDirs.get(i));
              copyDirChanges = true;

            } catch (InvalidFileTypeException | IOException ex) {
              LOG.error("Couldn't resolve link for copy-directories entry.", ex);
            }

          } else if (UTILITIES.FileUtils()
              .isLink(configurationModel.getModpackDir() + "/" + entries[0])) {
            try {
              copyDirs.set(
                  i,
                  UTILITIES.FileUtils()
                          .resolveLink(configurationModel.getModpackDir() + "/" + entries[0])
                      + ";"
                      + entries[1]);

              LOG.info("Resolved copy-directories link to: " + copyDirs.get(i));
              copyDirChanges = true;

            } catch (InvalidFileTypeException | IOException ex) {
              LOG.error("Couldn't resolve link for copy-directories entry.", ex);
            }
          }

        } else if (copyDirs.get(i).startsWith("!")) {
          // File exclusion

          if (UTILITIES.FileUtils().isLink(copyDirs.get(i).substring(1))) {

            try {
              copyDirs.set(
                  i, "!" + UTILITIES.FileUtils().resolveLink(copyDirs.get(i).substring(1)));

              LOG.info("Resolved copy-directories link to: " + copyDirs.get(i));
              copyDirChanges = true;

            } catch (InvalidFileTypeException | IOException ex) {
              LOG.error("Couldn't resolve link for copy-directories entry.", ex);
            }

          } else if (UTILITIES.FileUtils()
              .isLink(configurationModel.getModpackDir() + "/" + copyDirs.get(i).substring(1))) {

            try {
              copyDirs.set(
                  i,
                  UTILITIES.FileUtils()
                      .resolveLink(
                          "!"
                              + configurationModel.getModpackDir()
                              + "/"
                              + copyDirs.get(i).substring(1)));

              LOG.info("Resolved copy-directories link to: " + copyDirs.get(i));
              copyDirChanges = true;

            } catch (InvalidFileTypeException | IOException ex) {
              LOG.error("Couldn't resolve link for copy-directories entry.", ex);
            }
          }

        } else if (UTILITIES.FileUtils().isLink(copyDirs.get(i))) {
          // Regular entry, may be absolute path or relative one

          try {
            copyDirs.set(i, UTILITIES.FileUtils().resolveLink(copyDirs.get(i)));

            LOG.info("Resolved to: " + configurationModel.getModpackDir());
            copyDirChanges = true;

          } catch (InvalidFileTypeException | IOException ex) {
            LOG.error("Couldn't resolve link for modpack directory.", ex);
          }

        } else if (UTILITIES.FileUtils()
            .isLink(configurationModel.getModpackDir() + "/" + copyDirs.get(i))) {
          try {
            copyDirs.set(
                i,
                UTILITIES.FileUtils()
                    .resolveLink(configurationModel.getModpackDir() + "/" + copyDirs.get(i)));

            LOG.info("Resolved copy-directories link to: " + copyDirs.get(i));
            copyDirChanges = true;

          } catch (InvalidFileTypeException | IOException ex) {
            LOG.error("Couldn't resolve link for copy-directories entry.", ex);
          }
        }
      }

      if (copyDirChanges) {
        configurationModel.setCopyDirs(copyDirs);
      }
    }
  }

  /**
   * Print all encountered errors to logs.
   *
   * @param encounteredErrors List String. A list of all errors which were encountered during a
   *     configuration check.
   * @author Griefed
   */
  private void printEncounteredErrors(List<String> encounteredErrors) {

    LOG.error(
        "Encountered " + encounteredErrors.size() + " errors during the configuration check.");

    //noinspection UnusedAssignment
    int encounteredErrorNumber = 0;

    for (int i = 0; i < encounteredErrors.size(); i++) {

      encounteredErrorNumber = i + 1;

      LOG.error("Error " + encounteredErrorNumber + ": " + encounteredErrors.get(i));
    }
  }

  /**
   * Check the passed directory for existence and whether it is a directory, rather than a file.
   *
   * @param modpackDir {@link String} The modpack directory.
   * @return Boolean. Returns true if the directory exists.
   * @author Griefed
   */
  public boolean checkModpackDir(String modpackDir) {
    return checkModpackDir(modpackDir, new ArrayList<>());
  }

  /**
   * Checks whether the passed String is empty and if it is empty, prints the corresponding message
   * to the console and serverpackcreator.log so the user knows what went wrong.<br>
   * Checks whether the passed String is a directory and if it is not, prints the corresponding
   * message to the console and serverpackcreator.log so the user knows what went wrong.
   *
   * @param modpackDir String. The path to the modpack directory to check whether it is empty and
   *     whether it is a directory.
   * @param encounteredErrors List String. A list to which all encountered errors are saved to.
   * @return Boolean. Returns true if the directory exists.
   * @author Griefed
   */
  public boolean checkModpackDir(String modpackDir, List<String> encounteredErrors) {
    boolean configCorrect = false;

    if (modpackDir.isEmpty()) {

      LOG.error("Modpack directory not specified. Please specify an existing directory.");

      /* This log is meant to be read by the user, therefore we allow translation. */
      encounteredErrors.add(I18N.getMessage("configuration.log.error.checkmodpackdir"));

    } else if (!(new File(modpackDir).isDirectory())) {

      LOG.warn("Couldn't find directory " + modpackDir + ".");

      /* This log is meant to be read by the user, therefore we allow translation. */
      encounteredErrors.add(
          String.format(I18N.getMessage("configuration.log.error.modpackdirectory"), modpackDir));

    } else {

      configCorrect = true;
    }
    return configCorrect;
  }

  /**
   * Checks whether the passed list of directories which are supposed to be in the modpack directory
   * is empty, or whether all directories in the list exist in the modpack directory. If the user
   * specified a <code>source/file;destination/file</code>-combination, it is checked whether the
   * specified source-file exists on the host.
   *
   * @param directoriesToCopy List String. The list of directories, or <code>
   *     source/file;destination/file</code>-combinations, to check for existence. <code>
   *     source/file;destination/file</code>-combinations must be absolute paths to the source-file.
   * @param modpackDir String. The path to the modpack directory in which to check for existence of
   *     the passed list of directories.
   * @return Boolean. Returns true if every directory was found in the modpack directory. If any
   *     single one was not found, false is returned.
   * @author Griefed
   */
  public boolean checkCopyDirs(List<String> directoriesToCopy, String modpackDir) {
    return checkCopyDirs(directoriesToCopy, modpackDir, new ArrayList<>());
  }

  /**
   * Checks whether the passed list of directories which are supposed to be in the modpack directory
   * is empty and prints a message to the console and serverpackcreator.log if it is.<br>
   * Checks whether all directories in the list exist in the modpack directory and prints a message
   * to the console and serverpackcreator.log if any one of the directories could not be found. If
   * the user specified a <code>source/file;destination/file</code>-combination, it is checked
   * whether the specified source-file exists on the host.
   *
   * @param directoriesToCopy List String. The list of directories, or <code>
   *     source/file;destination/file</code>-combinations, to check for existence. <code>
   *     source/file;destination/file</code>-combinations must be absolute paths to the source-file.
   * @param modpackDir String. The path to the modpack directory in which to check for existence of
   *     the passed list of directories.
   * @param encounteredErrors List String. A list to which all encountered errors are saved to.
   * @return Boolean. Returns true if every directory was found in the modpack directory. If any
   *     single one was not found, false is returned.
   * @author Griefed
   */
  public boolean checkCopyDirs(
      List<String> directoriesToCopy, String modpackDir, List<String> encounteredErrors) {
    boolean configCorrect = true;

    directoriesToCopy.removeIf(entry -> entry.matches("^\\s+$") || entry.length() == 0);

    if (directoriesToCopy.isEmpty()) {

      configCorrect = false;

      LOG.error(
          "No directories or files specified for copying. This would result in an empty serverpack.");

      /* This log is meant to be read by the user, therefore we allow translation. */
      encounteredErrors.add(I18N.getMessage("configuration.log.error.checkcopydirs.empty"));

    } else if (directoriesToCopy.size() == 1 && directoriesToCopy.get(0).equals("lazy_mode")) {

      LOG.warn(
          "!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!");
      LOG.warn(
          "Lazy mode specified. This will copy the WHOLE modpack to the server pack. No exceptions.");
      LOG.warn("You will not receive support from me for a server pack generated this way.");
      LOG.warn(
          "Do not open an issue on GitHub if this configuration errors or results in a broken server pack.");
      LOG.warn(
          "!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!");

    } else {

      if (directoriesToCopy.size() > 1 && directoriesToCopy.contains("lazy_mode"))
        LOG.warn(
            "You specified lazy mode in your configuration, but your copyDirs configuration contains other entries. To use the lazy mode, only specify \"lazy_mode\" and nothing else. Ignoring lazy mode.");

      directoriesToCopy.removeIf(entry -> entry.equals("lazy_mode"));

      for (String directory : directoriesToCopy) {

        // Check whether the user specified a source;destination-combination
        if (directory.contains(";")) {

          String[] sourceFileDestinationFileCombination = directory.split(";");

          File sourceFileToCheck =
              new File(String.format("%s/%s", modpackDir, sourceFileDestinationFileCombination[0]));

          if (!new File(String.format("%s/%s", modpackDir, sourceFileDestinationFileCombination[0]))
                  .isFile()
              && !new File(
                      String.format("%s/%s", modpackDir, sourceFileDestinationFileCombination[0]))
                  .isDirectory()
              && !new File(sourceFileDestinationFileCombination[0]).isFile()
              && !new File(sourceFileDestinationFileCombination[0]).isDirectory()) {

            configCorrect = false;

            LOG.error(
                "Copy-file "
                    + sourceFileToCheck
                    + " does not exist. Please specify existing files.");

            /* This log is meant to be read by the user, therefore we allow translation. */
            encounteredErrors.add(
                String.format(
                    I18N.getMessage("configuration.log.error.checkcopydirs.filenotfound"),
                    sourceFileToCheck));

          } else {

            if (new File(
                        String.format("%s/%s", modpackDir, sourceFileDestinationFileCombination[0]))
                    .exists()
                && !UTILITIES.FileUtils()
                    .checkReadPermission(
                        String.format(
                            "%s/%s", modpackDir, sourceFileDestinationFileCombination[0]))) {

              configCorrect = false;

              LOG.error(
                  "No read-permission for "
                      + String.format(
                          "%s/%s", modpackDir, sourceFileDestinationFileCombination[0]));

              /* This log is meant to be read by the user, therefore we allow translation. */
              encounteredErrors.add(
                  String.format(
                      I18N.getMessage("configuration.log.error.checkcopydirs.read"),
                      String.format("%s/%s", modpackDir, sourceFileDestinationFileCombination[0])));

            } else if (new File(
                        String.format("%s/%s", modpackDir, sourceFileDestinationFileCombination[0]))
                    .exists()
                && !UTILITIES.FileUtils()
                    .checkReadPermission(
                        String.format(
                            "%s/%s", modpackDir, sourceFileDestinationFileCombination[0]))) {

              configCorrect = false;

              LOG.error(
                  "No read-permission for "
                      + String.format(
                          "%s/%s", modpackDir, sourceFileDestinationFileCombination[0]));

              /* This log is meant to be read by the user, therefore we allow translation. */
              encounteredErrors.add(
                  String.format(
                      I18N.getMessage("configuration.log.error.checkcopydirs.read"),
                      String.format("%s/%s", modpackDir, sourceFileDestinationFileCombination[0])));

            } else if (new File(sourceFileDestinationFileCombination[0]).exists()
                && !UTILITIES.FileUtils()
                    .checkReadPermission(sourceFileDestinationFileCombination[0])) {

              configCorrect = false;

              LOG.error("No read-permission for " + sourceFileDestinationFileCombination[0]);

              /* This log is meant to be read by the user, therefore we allow translation. */
              encounteredErrors.add(
                  String.format(
                      I18N.getMessage("configuration.log.error.checkcopydirs.read"),
                      sourceFileDestinationFileCombination[0]));
            }

            if (new File(
                    String.format("%s/%s", modpackDir, sourceFileDestinationFileCombination[0]))
                .isDirectory()) {

              //noinspection ConstantConditions
              for (File file :
                  new File(
                          String.format(
                              "%s/%s", modpackDir, sourceFileDestinationFileCombination[0]))
                      .listFiles()) {
                if (!UTILITIES.FileUtils().checkReadPermission(file)) {
                  configCorrect = false;

                  LOG.error("No read-permission for " + file);

                  /* This log is meant to be read by the user, therefore we allow translation. */
                  encounteredErrors.add(
                      String.format(
                          I18N.getMessage("configuration.log.error.checkcopydirs.read"), file));
                }
              }

            } else if (new File(sourceFileDestinationFileCombination[0]).isDirectory()) {

              //noinspection ConstantConditions
              for (File file : new File(sourceFileDestinationFileCombination[0]).listFiles()) {
                if (!UTILITIES.FileUtils().checkReadPermission(file)) {
                  configCorrect = false;

                  LOG.error("No read-permission for " + file);

                  /* This log is meant to be read by the user, therefore we allow translation. */
                  encounteredErrors.add(
                      String.format(
                          I18N.getMessage("configuration.log.error.checkcopydirs.read"), file));
                }
              }
            }
          }

          // Add an entry to the list of directories/files to exclude if it starts with !
        } else if (directory.startsWith("!")) {

          File fileOrDirectory =
              new File(String.format("%s/%s", modpackDir, directory.substring(1)));

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

          if (!dirToCheck.exists()
              && !new File(directory).exists()
              && !new File(directory).isFile()
              && !new File(directory).isDirectory()) {

            configCorrect = false;

            LOG.error(
                "Copy-file or copy-directory "
                    + directory
                    + " does not exist. Please specify existing directories or files.");

            /* This log is meant to be read by the user, therefore we allow translation. */
            encounteredErrors.add(
                String.format(
                    I18N.getMessage("configuration.log.error.checkcopydirs.notfound"), directory));

          } else {

            if (dirToCheck.exists() && !UTILITIES.FileUtils().checkReadPermission(dirToCheck)) {

              configCorrect = false;

              LOG.error("No read-permission for " + dirToCheck);

              /* This log is meant to be read by the user, therefore we allow translation. */
              encounteredErrors.add(
                  String.format(
                      I18N.getMessage("configuration.log.error.checkcopydirs.read"), dirToCheck));

            } else if (new File(directory).exists()
                && !UTILITIES.FileUtils().checkReadPermission(directory)) {

              configCorrect = false;

              LOG.error("No read-permission for " + directory);

              /* This log is meant to be read by the user, therefore we allow translation. */
              encounteredErrors.add(
                  String.format(
                      I18N.getMessage("configuration.log.error.checkcopydirs.read"), directory));
            }

            if (dirToCheck.isDirectory()) {

              //noinspection ConstantConditions
              for (File file : dirToCheck.listFiles()) {
                if (!UTILITIES.FileUtils().checkReadPermission(file)) {
                  configCorrect = false;

                  LOG.error("No read-permission for " + file);

                  /* This log is meant to be read by the user, therefore we allow translation. */
                  encounteredErrors.add(
                      String.format(
                          I18N.getMessage("configuration.log.error.checkcopydirs.read"), file));
                }
              }

            } else if (new File(directory).isDirectory()) {

              //noinspection ConstantConditions
              for (File file : new File(directory).listFiles()) {
                if (!UTILITIES.FileUtils().checkReadPermission(file)) {
                  configCorrect = false;

                  LOG.error("No read-permission for " + file);

                  /* This log is meant to be read by the user, therefore we allow translation. */
                  encounteredErrors.add(
                      String.format(
                          I18N.getMessage("configuration.log.error.checkcopydirs.read"), file));
                }
              }
            }
          }
        }
      }
    }
    return configCorrect;
  }

  /**
   * Checks the passed String whether it is an existing file. If the passed String is empty, then
   * ServerPackCreator will treat it as the user being fine with the default files and return the
   * corresponding boolean.
   *
   * @param iconOrPropertiesPath String. The path to the custom server-icon.png or server.properties
   *     file to check.
   * @return Boolean. True if the file exists or an empty String was passed, false if a file was
   *     specified, but the file was not found.
   * @author Griefed
   */
  public boolean checkIconAndProperties(String iconOrPropertiesPath) {

    if (iconOrPropertiesPath.isEmpty()) {

      return true;

    } else {

      return new File(iconOrPropertiesPath).exists();
    }
  }

  /**
   * Check whether the given path is a valid Java specification.
   *
   * @param pathToJava {@link String} Path to the Java executable
   * @return Boolean. Returns <code>true</code> if the path is valid.
   * @author Griefed
   */
  public boolean checkJavaPath(String pathToJava) {

    if (pathToJava.length() == 0) {
      return false;
    }

    FileUtilities.FileType type = UTILITIES.FileUtils().checkFileType(pathToJava);

    switch (type) {
      case FILE:
        return testJava(pathToJava);

      case LINK:
      case SYMLINK:
        try {

          return testJava(UTILITIES.FileUtils().resolveLink(new File(pathToJava)));

        } catch (InvalidFileTypeException | IOException ex) {
          LOG.error("Could not read link/symlink.", ex);
        }

        return false;

      case DIRECTORY:
        LOG.error("Directory specified. Path to Java must lead to a lnk, symlink or file.");

      case INVALID:
      default:
        return false;
    }
  }

  /**
   * Test for a valid Java specification by trying to run <code>java -version</code>. If the command
   * goes through without errors, it is considered a correct specification.
   *
   * @param pathToJava {@link String} Path to the java executable/binary.
   * @return {@link Boolean} <code>true</code> if the specified file is a valid Java
   *     executable/binary.
   * @author Griefed
   */
  public boolean testJava(String pathToJava) {
    boolean testSuccessful;
    try {
      ProcessBuilder processBuilder =
          new ProcessBuilder(new ArrayList<>(Arrays.asList(pathToJava, "-version")));

      processBuilder.redirectErrorStream(true);

      Process process = processBuilder.start();

      BufferedReader bufferedReader =
          new BufferedReader(new InputStreamReader(process.getInputStream()));

      while (bufferedReader.readLine() != null && !bufferedReader.readLine().equals("null")) {
        System.out.println(bufferedReader.readLine());
      }

      bufferedReader.close();
      process.destroyForcibly();

      testSuccessful = true;
    } catch (IOException e) {

      LOG.error("Invalid Java specified.");
      testSuccessful = false;
    }

    return testSuccessful;
  }

  /**
   * Check the given path to a Java installation for validity and return it, if it is valid. If the
   * passed path is a UNIX symlink or Windows lnk, it is resolved, then returned. If the passed path
   * is considered invalid, the system default is acquired and returned.
   *
   * @param pathToJava String. The path to check for whether it is a valid Java installation.
   * @return String. Returns the path to the Java installation. If user input was incorrect, SPC
   *     will try to acquire the path automatically.
   * @author Griefed
   */
  public String getJavaPath(String pathToJava) {

    String checkedJavaPath;

    try {

      if (pathToJava.length() > 0) {

        if (checkJavaPath(pathToJava)) {

          return pathToJava;
        }

        if (checkJavaPath(pathToJava + ".exe")) {

          return pathToJava + ".exe";
        }

        if (checkJavaPath(pathToJava + ".lnk")) {

          return UTILITIES.FileUtils().resolveLink(new File(pathToJava + ".lnk"));
        }
      }

      LOG.info("Java setting invalid or otherwise not usable. Using system default.");
      LOG.debug("Acquiring path to Java installation from system properties...");
      checkedJavaPath = UTILITIES.SystemUtils().acquireJavaPathFromSystem();

      LOG.debug("Automatically acquired path to Java installation: " + checkedJavaPath);

    } catch (NullPointerException | InvalidFileTypeException | IOException ex) {

      LOG.info("Java setting invalid or otherwise not usable. using system default.");
      checkedJavaPath = UTILITIES.SystemUtils().acquireJavaPathFromSystem();

      LOG.debug("Automatically acquired path to Java installation: " + checkedJavaPath, ex);
    }

    return checkedJavaPath;
  }

  /**
   * Checks whether either Forge or Fabric were specified as the modloader.
   *
   * @param modloader String. Check as case-insensitive for Forge or Fabric.
   * @return Boolean. Returns true if the specified modloader is either Forge or Fabric. False if
   *     neither.
   * @author Griefed
   */
  public boolean checkModloader(String modloader) {

    if (modloader.toLowerCase().contains("forge")
        || modloader.toLowerCase().contains("fabric")
        || modloader.toLowerCase().contains("quilt")) {

      return true;

    } else {

      LOG.error("Invalid modloader specified. Modloader must be either Forge, Fabric or Quilt.");

      return false;
    }
  }

  /**
   * Check the given Minecraft and modloader versions for the specified modloader.
   *
   * @param modloader String. The passed modloader which determines whether the check for Forge or
   *     Fabric is called.
   * @param modloaderVersion String. The version of the modloader which is checked against the
   *     corresponding modloaders manifest.
   * @param minecraftVersion String. The version of Minecraft used for checking the Forge version.
   * @return Boolean. Returns true if the specified modloader version was found in the corresponding
   *     manifest.
   * @author Griefed
   */
  public boolean checkModloaderVersion(
      String modloader, String modloaderVersion, String minecraftVersion) {

    switch (modloader) {
      case "Forge":
        return VERSIONMETA
            .forge()
            .checkForgeAndMinecraftVersion(minecraftVersion, modloaderVersion);

      case "Fabric":
        return VERSIONMETA.fabric().checkFabricVersion(modloaderVersion);

      case "Quilt":
        return VERSIONMETA.quilt().checkQuiltVersion(modloaderVersion);

      default:
        LOG.error(
            "Specified incorrect modloader version. Please check your modpack for the correct version and enter again.");

        return false;
    }
  }
}
