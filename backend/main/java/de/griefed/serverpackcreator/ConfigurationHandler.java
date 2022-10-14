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

import com.fasterxml.jackson.databind.JsonNode;
import de.griefed.serverpackcreator.i18n.I18n;
import de.griefed.serverpackcreator.utilities.common.InvalidFileTypeException;
import de.griefed.serverpackcreator.utilities.common.Utilities;
import de.griefed.serverpackcreator.versionmeta.VersionMeta;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.ProviderNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;
import net.lingala.zip4j.ZipFile;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Check any given {@link ConfigurationModel} for errors and, if so desired, add them to a passed
 * list of errors, so you may display them in a GUI, CLI or website. The most important method is
 * {@link #checkConfiguration(ConfigurationModel, boolean)} and all of its variants which will check
 * your passed configuration model for errors, indicating whether it is safe to use for further
 * operations. Running your model through the checks also ensures that the default script settings
 * are present and set according to your pack's environment.
 *
 * @author Griefed
 */
@SuppressWarnings("unused")
@Component
public final class ConfigurationHandler {

  private static final Logger LOG = LogManager.getLogger(ConfigurationHandler.class);

  private final I18n I18N;
  private final VersionMeta VERSIONMETA;
  private final ApplicationProperties APPLICATIONPROPERTIES;
  private final Utilities UTILITIES;
  private final ApplicationAddons APPLICATIONADDONS;

  /**
   * Construct a new ConfigurationHandler giving you access to various config check methods.
   *
   * @param injectedI18n                  Used to add localized error messages during configuration
   *                                      checks.
   * @param injectedApplicationProperties Base settings of SPC used just about everywhere.
   * @param injectedVersionMeta           Meta used for Minecraft and modloader version checks and
   *                                      verification.
   * @param injectedUtilities             Common utilities used all across SPC.
   * @param injectedApplicationAddons     Addons and extensions added by external addons which can
   *                                      add additional checks to a given configuration check.
   * @author Griefed
   */
  @Autowired
  public ConfigurationHandler(
      I18n injectedI18n,
      VersionMeta injectedVersionMeta,
      ApplicationProperties injectedApplicationProperties,
      Utilities injectedUtilities,
      ApplicationAddons injectedApplicationAddons) {

    APPLICATIONPROPERTIES = injectedApplicationProperties;
    I18N = injectedI18n;
    VERSIONMETA = injectedVersionMeta;
    UTILITIES = injectedUtilities;
    APPLICATIONADDONS = injectedApplicationAddons;
  }

  /**
   * Check the passed configuration-file. If any check returns {@code true} then the server pack
   * will not be created. In order to find out which check failed, the user has to check their
   * serverpackcreator.log in the logs-directory.<br> Does not create a modpack if a CurseForge
   * project and file is specified.
   *
   * @param configFile        The configuration file to check. Must either be an existing file to
   *                          load a configuration from or null if you want to use the passed
   *                          configuration model.
   * @param encounteredErrors A list of errors encountered during configuration checks which gets
   *                          printed to the console and log after all checks have run. Gives the
   *                          user more detail on what went wrong at which part of their
   *                          configuration. Can be used to display the errors, if any were
   *                          encountered, in a UI or be printed into the console or whatever have
   *                          you.
   * @param quietCheck        Whether the configuration should be printed to the console and logs.
   *                          Pass false to quietly check the configuration.
   * @return {@code false} if the configuration has passed all tests.
   * @author Griefed
   */
  public boolean checkConfiguration(
      @NotNull final File configFile,
      @NotNull final List<String> encounteredErrors,
      boolean quietCheck) {

    ConfigurationModel configurationModel = new ConfigurationModel();

    return checkConfiguration(configFile, configurationModel, encounteredErrors, quietCheck);
  }

  /**
   * Check the passed configuration-file. If any check returns {@code true} then the server pack
   * will not be created. In order to find out which check failed, the user has to check their
   * serverpackcreator.log in the logs-directory.
   *
   * @param configFile         The configuration file to check. Must either be an existing file to
   *                           load a configuration from or null if you want to use the passed
   *                           configuration model.
   * @param configurationModel Instance of a configuration of a modpack. Can be used to further
   *                           display or use any information within, as it may be changed or
   *                           otherwise altered by this method.
   * @param encounteredErrors  A list of errors encountered during configuration checks which gets
   *                           printed to the console and log after all checks have run. Gives the
   *                           user more detail on what went wrong at which part of their
   *                           configuration. Can be used to display the errors, if any were
   *                           encountered, in a UI or be printed into the console or whatever have
   *                           you.
   * @param quietCheck         Whether the configuration should be printed to the console and logs.
   *                           Pass false to quietly check the configuration.
   * @return {@code false} if the configuration has passed all tests.
   * @author Griefed
   */
  public boolean checkConfiguration(
      @NotNull final File configFile,
      @NotNull final ConfigurationModel configurationModel,
      @NotNull final List<String> encounteredErrors,
      boolean quietCheck) {

    try {
      ConfigurationModel fileConf = new ConfigurationModel(UTILITIES, configFile);
      configurationModel.setClientMods(fileConf.getClientMods());
      configurationModel.setCopyDirs(fileConf.getCopyDirs());
      configurationModel.setModpackDir(fileConf.getModpackDir());
      configurationModel.setMinecraftVersion(fileConf.getMinecraftVersion());
      configurationModel.setModLoader(fileConf.getModLoader());
      configurationModel.setModLoaderVersion(fileConf.getModLoaderVersion());
      configurationModel.setJavaArgs(fileConf.getJavaArgs());
      configurationModel.setServerPackSuffix(fileConf.getServerPackSuffix());
      configurationModel.setServerIconPath(fileConf.getServerIconPath());
      configurationModel.setServerPropertiesPath(fileConf.getServerPropertiesPath());
      configurationModel.setIncludeServerInstallation(fileConf.getIncludeServerInstallation());
      configurationModel.setIncludeServerIcon(fileConf.getIncludeServerIcon());
      configurationModel.setIncludeServerProperties(fileConf.getIncludeServerProperties());
      configurationModel.setIncludeZipCreation(fileConf.getIncludeZipCreation());
      configurationModel.setScriptSettings(fileConf.getScriptSettings());
      configurationModel.setAddonsConfigs(fileConf.getAddonsConfigs());
    } catch (Exception ex) {

      LOG.error(
          "Couldn't parse config file. Consider checking your config file and fixing empty values. If the value needs to be an empty string, leave its value to \"\".");

      /* This log is meant to be read by the user, therefore we allow translation. */
      encounteredErrors.add(I18N.getMessage("configuration.log.error.checkconfig.start"));
    }

    return checkConfiguration(configurationModel, encounteredErrors, quietCheck);
  }

  /**
   * Check the passed {@link ConfigurationModel}. If any check returns {@code true} then the server
   * pack will not be created. In order to find out which check failed, the user has to check their
   * serverpackcreator.log in the logs-directory.<br> The passed String List
   * {@code encounteredErrors} can be used to display the errors, if any were encountered, in a UI
   * or be printed into the console or whatever have you.<br> The passed {@link ConfigurationModel}
   * can be used to further display or use any information within, as it may be changed or otherwise
   * altered by this method.
   *
   * @param configurationModel Instance of a configuration of a modpack. Can be used to further
   *                           display or use any information within, as it may be changed or
   *                           otherwise altered by this method.
   * @param encounteredErrors  A list of errors encountered during configuration checks which gets
   *                           printed to the console and log after all checks have run. Gives the
   *                           user more detail on what went wrong at which part of their
   *                           configuration. Can be used to display the errors, if any were
   *                           encountered, in a UI or be printed into the console or whatever have
   *                           you.
   * @param quietCheck         Whether the configuration should be printed to the console and logs.
   *                           Pass false to quietly check the configuration.
   * @return {@code false} if all checks are passed.
   * @author Griefed
   */
  public boolean checkConfiguration(
      @NotNull final ConfigurationModel configurationModel,
      @NotNull final List<String> encounteredErrors,
      boolean quietCheck) {

    boolean configHasError = false;

    sanitizeLinks(configurationModel);

    LOG.info("Checking configuration...");

    if (configurationModel.getClientMods().isEmpty()) {

      LOG.warn("No clientside-only mods specified. Using fallback list.");
      configurationModel.setClientMods(APPLICATIONPROPERTIES.getListFallbackMods());
    }

    if (!checkIconAndProperties(configurationModel.getServerIconPath())) {

      configHasError = true;

      LOG.error(
          "The specified server-icon does not exist: " + configurationModel.getServerIconPath());

      /* This log is meant to be read by the user, therefore we allow translation. */
      encounteredErrors.add(
          String.format(
              I18N.getMessage("configuration.log.error.servericon"),
              configurationModel.getServerIconPath()));

    } else if (!configurationModel.getServerIconPath().isEmpty()
        && new File(configurationModel.getServerIconPath()).exists()
        && !UTILITIES.FileUtils().checkReadPermission(configurationModel.getServerIconPath())) {

      configHasError = true;

      LOG.error("No read-permission for " + configurationModel.getServerIconPath());

      /* This log is meant to be read by the user, therefore we allow translation. */
      encounteredErrors.add(
          String.format(
              I18N.getMessage("configuration.log.error.checkcopydirs.read"),
              configurationModel.getServerIconPath()));
    }

    if (!checkIconAndProperties(configurationModel.getServerPropertiesPath())) {

      configHasError = true;

      LOG.error(
          "The specified server.properties does not exist: "
              + configurationModel.getServerPropertiesPath());

      /* This log is meant to be read by the user, therefore we allow translation. */
      encounteredErrors.add(
          String.format(
              I18N.getMessage("configuration.log.error.serverproperties"),
              configurationModel.getServerPropertiesPath()));

    } else if (!configurationModel.getServerPropertiesPath().isEmpty()
        && new File(configurationModel.getServerPropertiesPath()).exists()
        && !UTILITIES.FileUtils()
                     .checkReadPermission(configurationModel.getServerPropertiesPath())) {

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

      if (isDir(configurationModel, encounteredErrors)) {
        configHasError = true;
      }

    } else if (modpack.isFile() && modpack.getName().endsWith("zip")) {

      try {
        if (isZip(configurationModel, encounteredErrors)) {
          configHasError = true;
        }
      } catch (IOException ex) {
        configHasError = true;
        LOG.error("An error occurred whilst working with the ZIP-archive.", ex);
      }

    } else {
      configHasError = true;
      LOG.error("Modpack directory not specified. Please specify an existing directory. Specified: "
                    + configurationModel.getModpackDir());

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
            configurationModel.getMinecraftVersion(),
            encounteredErrors)) {

          LOG.debug("modLoaderVersion setting check passed.");

        } else {

          configHasError = true;

          LOG.error("There's something wrong with your modloader version setting.");

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

      LOG.error("There's something wrong with your modloader or modloader version setting.");

      /* This log is meant to be read by the user, therefore we allow translation. */
      encounteredErrors.add(I18N.getMessage("configuration.log.error.checkmodloader"));
    }

    if (APPLICATIONADDONS.runConfigCheckExtensions(configurationModel, encounteredErrors)) {
      configHasError = true;
    }

    if (quietCheck) {
      printConfigurationModel(configurationModel);
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
   * Sanitize any and all links in a given instance of {@link ConfigurationModel} modpack-directory,
   * server-icon path, server-properties path, Java path and copy-directories entries.
   *
   * @param configurationModel Instance of {@link ConfigurationModel} in which to sanitize links to
   *                           their respective destinations.
   * @author Griefed
   */
  public void sanitizeLinks(@NotNull final ConfigurationModel configurationModel) {

    LOG.info("Checking configuration for links...");

    if (!configurationModel.getModpackDir().isEmpty()
        && UTILITIES.FileUtils().isLink(configurationModel.getModpackDir())) {
      try {
        configurationModel.setModpackDir(
            UTILITIES.FileUtils().resolveLink(configurationModel.getModpackDir()));

        LOG.info("Resolved modpack directory link to: " + configurationModel.getModpackDir());

      } catch (InvalidFileTypeException | IOException ex) {
        LOG.error("Couldn't resolve link for modpack directory.", ex);
      }
    }

    if (!configurationModel.getServerIconPath().isEmpty()
        && UTILITIES.FileUtils().isLink(configurationModel.getServerIconPath())) {
      try {
        configurationModel.setServerIconPath(
            UTILITIES.FileUtils().resolveLink(configurationModel.getServerIconPath()));

        LOG.info("Resolved server-icon link to: " + configurationModel.getServerIconPath());

      } catch (InvalidFileTypeException | IOException ex) {
        LOG.error("Couldn't resolve link for server-icon.", ex);
      }
    }

    if (!configurationModel.getServerPropertiesPath().isEmpty()
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

    if (!configurationModel.getCopyDirs().isEmpty()) {
      List<String> copyDirs = new ArrayList<>(configurationModel.getCopyDirs());
      boolean copyDirChanges = false;

      for (int i = 0; i < copyDirs.size(); i++) {

        if (!copyDirs.get(i).startsWith("==") && copyDirs.get(i).contains("==")
            && copyDirs.get(i).split("==").length == 2) {

          String[] entries = copyDirs.get(i).split("==");

          if (UTILITIES.FileUtils().isLink(entries[0])) {
            try {
              copyDirs.set(i, UTILITIES.FileUtils().resolveLink(entries[0]) + "==" + entries[1]);

              LOG.info("Resolved regex-directory link to: " + copyDirs.get(i));
              copyDirChanges = true;

            } catch (InvalidFileTypeException | IOException ex) {
              LOG.error("Couldn't resolve link for copy-directories entry: " + copyDirs.get(i), ex);
            }
          }

        } else if (copyDirs.get(i).contains(";")) {
          // Source;Destination-combination

          String[] entries = copyDirs.get(i).split(";");

          if (UTILITIES.FileUtils().isLink(entries[0])) {
            try {
              copyDirs.set(i, UTILITIES.FileUtils().resolveLink(entries[0]) + ";" + entries[1]);

              LOG.info("Resolved copy-directories link to: " + copyDirs.get(i));
              copyDirChanges = true;

            } catch (InvalidFileTypeException | IOException ex) {
              LOG.error("Couldn't resolve link for copy-directories entry: " + copyDirs.get(i), ex);
            }

          } else if (UTILITIES.FileUtils()
                              .isLink(configurationModel.getModpackDir() + File.separator
                                          + entries[0])) {
            try {
              copyDirs.set(
                  i,
                  UTILITIES.FileUtils()
                           .resolveLink(
                               configurationModel.getModpackDir() + File.separator + entries[0])
                      + ";"
                      + entries[1]);

              LOG.info("Resolved copy-directories link to: " + copyDirs.get(i));
              copyDirChanges = true;

            } catch (InvalidFileTypeException | IOException ex) {
              LOG.error("Couldn't resolve link for copy-directories entry: " + copyDirs.get(i), ex);
            }
          }

        } else if (copyDirs.get(i).startsWith("!")) {

          if (copyDirs.get(i).contains("==")
              && copyDirs.get(i).substring(1).split("==").length == 2) {

            String[] entries = copyDirs.get(i).split("==");

            if (UTILITIES.FileUtils().isLink(entries[0].substring(1))) {
              try {
                copyDirs.set(i,
                             "!" + UTILITIES.FileUtils().resolveLink(entries[0].substring(1)) + "=="
                                 + entries[1]);

                LOG.info("Resolved regex-directory link to: " + copyDirs.get(i));
                copyDirChanges = true;

              } catch (InvalidFileTypeException | IOException ex) {
                LOG.error("Couldn't resolve link for copy-directories entry: " + copyDirs.get(i),
                          ex);
              }
            }

          } else if (UTILITIES.FileUtils().isLink(copyDirs.get(i).substring(1))) {

            try {
              copyDirs.set(
                  i, "!" + UTILITIES.FileUtils().resolveLink(copyDirs.get(i).substring(1)));

              LOG.info("Resolved copy-directories link to: " + copyDirs.get(i));
              copyDirChanges = true;

            } catch (InvalidFileTypeException | IOException ex) {
              LOG.error("Couldn't resolve link for copy-directories entry: " + copyDirs.get(i), ex);
            }

          } else if (UTILITIES.FileUtils()
                              .isLink(configurationModel.getModpackDir() + File.separator
                                          + copyDirs.get(i)
                                                    .substring(1))) {

            try {
              copyDirs.set(
                  i,
                  UTILITIES.FileUtils()
                           .resolveLink(
                               "!"
                                   + configurationModel.getModpackDir()
                                   + File.separator
                                   + copyDirs.get(i).substring(1)));

              LOG.info("Resolved copy-directories link to: " + copyDirs.get(i));
              copyDirChanges = true;

            } catch (InvalidFileTypeException | IOException ex) {
              LOG.error("Couldn't resolve link for copy-directories entry: " + copyDirs.get(i), ex);
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
                            .isLink(
                                configurationModel.getModpackDir() + File.separator + copyDirs.get(
                                    i))) {
          try {
            copyDirs.set(
                i,
                UTILITIES.FileUtils()
                         .resolveLink(
                             configurationModel.getModpackDir() + File.separator + copyDirs.get(
                                 i)));

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
   * Checks the passed String whether it is an existing file. If the passed String is empty, then
   * ServerPackCreator will treat it as the user being fine with the default files and return the
   * corresponding boolean.
   *
   * @param iconOrPropertiesPath The path to the custom server-icon.png or server.properties file to
   *                             check.
   * @return Boolean. True if the file exists or an empty String was passed, false if a file was
   * specified, but the file was not found.
   * @author Griefed
   */
  public boolean checkIconAndProperties(@NotNull final String iconOrPropertiesPath) {

    if (iconOrPropertiesPath.isEmpty()) {

      return true;

    } else {

      return new File(iconOrPropertiesPath).exists();
    }
  }

  /**
   * If the in the configuration specified modpack dir is an existing directory, checks are made for
   * valid configuration of: directories to copy to server pack, if includeServerInstallation is
   * {@code true} path to Java executable/binary, Minecraft version, modloader and modloader
   * version.
   *
   * @param configurationModel An instance of {@link ConfigurationModel} which contains the
   *                           configuration of the modpack.
   * @param encounteredErrors  A list to which all encountered errors are saved to.
   * @return {@code true} if an error is found during configuration check.
   * @author Griefed
   */
  public boolean isDir(@NotNull final ConfigurationModel configurationModel,
                       @NotNull final List<String> encounteredErrors) {
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
   * considered valid, it needs to contain the {@code mods} and {@code config} folders at minimum.
   * If any of {@code manifest.json}, {@code minecraftinstance.json} or {@code config.json} are
   * available, gather as much information from them as possible.
   *
   * @param configurationModel Instance of {@link ConfigurationModel} with a server pack
   *                           configuration.
   * @param encounteredErrors  A list of errors encountered during configuration checks.
   * @return {@code false} when no errors were encountered.
   * @throws IOException if an error occurred trying to move the server pack directory.
   * @author Griefed
   */
  public boolean isZip(@NotNull final ConfigurationModel configurationModel,
                       @NotNull final List<String> encounteredErrors)
      throws IOException {
    boolean configHasError = false;

    // modpackDir points at a ZIP-file. Get the path to the would be modpack directory.
    String modpackName = new File(configurationModel.getModpackDir()).getName()
                                                                     .replace("\\.[Zz][Ii][Pp]",
                                                                              "");
    String destination = APPLICATIONPROPERTIES.modpacksDirectory() + File.separator + modpackName;

    if (checkZipArchive(Paths.get(configurationModel.getModpackDir()), encounteredErrors)) {
      return true;
    }

    // Does the modpack extracted from the ZIP-archive already exist?
    destination = acquireDestination(destination);

    // Extract the archive to the modpack directory.
    UTILITIES.FileUtils().unzipArchive(configurationModel.getModpackDir(), destination);

    // Expand the already set copyDirs with suggestions from extracted ZIP-archive.
    List<String> newCopyDirs = suggestCopyDirs(destination);
    for (String entry : configurationModel.getCopyDirs()) {
      if (!newCopyDirs.contains(entry)) {
        newCopyDirs.add(entry);
      }
    }
    configurationModel.setCopyDirs(newCopyDirs);

    // If various manifests exist, gather as much information as possible.
    // Check CurseForge manifest available if a modpack was exported through a client like
    // Overwolf's CurseForge or through GDLauncher.

    int amountOfErrors = encounteredErrors.size();

    String packName = checkManifests(destination, configurationModel, encounteredErrors);

    if (encounteredErrors.size() > amountOfErrors) {
      configHasError = true;
    }

    // If no json was read from the modpack, we must sadly use the ZIP-files name as the new
    // destination. Sadface.
    if (packName == null) {
      packName = destination;
    }

    packName = new File(UTILITIES.StringUtils().pathSecureTextAlternative(packName)).getPath();

    // Get the path to the would-be-server-pack with the new destination.
    String wouldBeServerPack =
        new File(APPLICATIONPROPERTIES.serverPacksDirectory(), new File(packName).getName()
            + configurationModel.getServerPackSuffix())
            .getCanonicalPath();

    // Check whether a server pack for the new destination already exists.
    // If it does, we need to change it to avoid overwriting any existing files.
    packName = packName + "_" + getIncrementation(packName, wouldBeServerPack);

    // Finally, move to new destination to avoid overwriting of server pack
    FileUtils.moveDirectory(new File(destination),
                            new File(APPLICATIONPROPERTIES.modpacksDirectory(),
                                     new File(packName).getName()));

    // Last but not least, use the newly acquired packname as the modpack directory.
    configurationModel.setModpackDir(packName);

    // Does the modpack contain a server-icon or server.properties? If so, include
    // them in the server pack.
    File file = new File(packName, "server-icon.png");
    if (file.exists()) {
      configurationModel.setServerIconPath(file.getAbsolutePath());
    }
    file = new File(packName, "server.properties");
    if (file.exists()) {
      configurationModel.setServerPropertiesPath(file.getAbsolutePath());
    }

    return configHasError;
  }

  /**
   * Checks whether either Forge or Fabric were specified as the modloader.
   *
   * @param modloader Check as case-insensitive for Forge or Fabric.
   * @return Boolean. Returns true if the specified modloader is either Forge or Fabric. False if
   * neither.
   * @author Griefed
   */
  public boolean checkModloader(@NotNull final String modloader) {

    if (modloader.toLowerCase().matches("^forge$")
        || modloader.toLowerCase().matches("^fabric$")
        || modloader.toLowerCase().matches("^quilt$")
        || modloader.toLowerCase().matches("^legacyfabric$")) {

      return true;

    } else {

      LOG.error("Invalid modloader specified. Modloader must be either Forge, Fabric or Quilt.");

      return false;
    }
  }

  /**
   * Check the given Minecraft and modloader versions for the specified modloader and update the
   * passed error-list should any error be encountered.
   *
   * @param modloader         String. The passed modloader which determines whether the check for
   *                          Forge or Fabric is called.
   * @param modloaderVersion  String. The version of the modloader which is checked against the
   *                          corresponding modloaders manifest.
   * @param minecraftVersion  String. The version of Minecraft used for checking the Forge version.
   * @param encounteredErrors List of encountered errors to add to in case of errors.
   * @return {@code true} if the specified modloader version was found in the corresponding
   * manifest.
   * @author Griefed
   */
  public boolean checkModloaderVersion(
      @NotNull final String modloader,
      @NotNull final String modloaderVersion,
      @NotNull final String minecraftVersion,
      @NotNull final List<String> encounteredErrors) {

    switch (modloader) {
      case "Forge":
        if (VERSIONMETA.forge().checkForgeAndMinecraftVersion(minecraftVersion, modloaderVersion)) {

          return true;

        } else {

          encounteredErrors.add(
              String.format(
                  I18N.getMessage("configuration.log.error.checkmodloaderandversion"),
                  minecraftVersion,
                  modloader,
                  modloaderVersion));

          return false;
        }

      case "Fabric":
        if (VERSIONMETA.fabric().isVersionValid(modloaderVersion)
            && VERSIONMETA
            .fabric()
            .getLoaderDetails(minecraftVersion, modloaderVersion)
            .isPresent()) {

          return true;

        } else {
          encounteredErrors.add(
              String.format(
                  I18N.getMessage("configuration.log.error.checkmodloaderandversion"),
                  minecraftVersion,
                  modloader,
                  modloaderVersion));

          return false;
        }

      case "Quilt":
        if (VERSIONMETA.quilt().isVersionValid(modloaderVersion)
            && VERSIONMETA.fabric().isMinecraftSupported(minecraftVersion)) {

          return true;

        } else {
          encounteredErrors.add(
              String.format(
                  I18N.getMessage("configuration.log.error.checkmodloaderandversion"),
                  minecraftVersion,
                  modloader,
                  modloaderVersion));

          return false;
        }

      case "LegacyFabric":
        if (VERSIONMETA.legacyFabric().isVersionValid(modloaderVersion)
            && VERSIONMETA.legacyFabric().isMinecraftSupported(minecraftVersion)) {

          return true;

        } else {
          encounteredErrors.add(
              String.format(
                  I18N.getMessage("configuration.log.error.checkmodloaderandversion"),
                  minecraftVersion,
                  modloader,
                  modloaderVersion));

          return false;
        }

      default:
        LOG.error(
            "Specified incorrect modloader version. Please check your modpack for the correct version and enter again.");

        return false;
    }
  }

  /**
   * Convenience method which passes the important fields from an instance of
   * {@link ConfigurationModel} to
   * {@link #printConfigurationModel(String, List, List, boolean, String, String, String, boolean,
   * boolean, boolean, String, String, String, String, HashMap)}
   *
   * @param configurationModel Instance of {@link ConfigurationModel} to print to console and logs.
   * @author Griefed
   */
  public void printConfigurationModel(@NotNull final ConfigurationModel configurationModel) {
    printConfigurationModel(
        configurationModel.getModpackDir(),
        configurationModel.getClientMods(),
        configurationModel.getCopyDirs(),
        configurationModel.getIncludeServerInstallation(),
        configurationModel.getMinecraftVersion(),
        configurationModel.getModLoader(),
        configurationModel.getModLoaderVersion(),
        configurationModel.getIncludeServerIcon(),
        configurationModel.getIncludeServerProperties(),
        configurationModel.getIncludeZipCreation(),
        configurationModel.getJavaArgs(),
        configurationModel.getServerPackSuffix(),
        configurationModel.getServerIconPath(),
        configurationModel.getServerPropertiesPath(),
        configurationModel.getScriptSettings());
  }

  /**
   * Print all encountered errors to logs.
   *
   * @param encounteredErrors List String. A list of all errors which were encountered during a
   *                          configuration check.
   * @author Griefed
   */
  private void printEncounteredErrors(@NotNull final List<String> encounteredErrors) {

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
   * Update the script settings and ensure the default keys, with values gathered from the passed
   * {@link ConfigurationModel}, are present:
   *
   * <ol>
   *   <li>{@code SPC_SERVERPACKCREATOR_VERSION_SPC} : {@code
   *       ServerPackCreator version with which the scripts were created}
   *   <li>{@code SPC_MINECRAFT_VERSION_SPC} : {@code Minecraft version of the modpack}
   *   <li>{@code SPC_MINECRAFT_SERVER_URL_SPC} : {@code Download-URL to the Minecraft server
   *       }
   *   <li>{@code SPC_MODLOADER_SPC} : {@code The modloader of the modpack}
   *   <li>{@code SPC_MODLOADER_VERSION_SPC} : {@code The modloader version of the modpack
   *       }
   *   <li>{@code SPC_JAVA_ARGS_SPC} : {@code The JVM args to be used to run the server}
   *   <li>{@code SPC_JAVA_SPC} : {@code
   *       Path to the java installation to be used to run the server}
   *   <li>{@code SPC_FABRIC_INSTALLER_VERSION_SPC} : {@code
   *       Most recent version of the Fabric installer at the time of creating the scripts}
   *   <li>{@code SPC_QUILT_INSTALLER_VERSION_SPC} : {@code
   *       Most recent version of the Quilt installer at the time of creating the scripts}
   * </ol>
   *
   * @param configurationModel Model in which to ensure the default key-value pairs are present.
   * @author Griefed
   */
  public void ensureScriptSettingsDefaults(@NotNull final ConfigurationModel configurationModel) {

    if (!VERSIONMETA.minecraft()
                    .getServer(configurationModel.getMinecraftVersion()).isPresent()
        || !VERSIONMETA.minecraft()
                       .getServer(configurationModel.getMinecraftVersion()).get().url().isPresent()
    ) {

      configurationModel.getScriptSettings().put(
          "SPC_MINECRAFT_SERVER_URL_SPC",
          "");

    } else {

      configurationModel.getScriptSettings().put(
          "SPC_MINECRAFT_SERVER_URL_SPC",
          VERSIONMETA
              .minecraft().getServer(configurationModel.getMinecraftVersion()).get()
              .url().get().toString());
    }

    configurationModel.getScriptSettings().put(
        "SPC_SERVERPACKCREATOR_VERSION_SPC",
        APPLICATIONPROPERTIES.serverPackCreatorVersion());

    configurationModel.getScriptSettings().put(
        "SPC_MINECRAFT_VERSION_SPC",
        configurationModel.getMinecraftVersion());

    configurationModel.getScriptSettings().put(
        "SPC_MODLOADER_SPC",
        configurationModel.getModLoader());

    configurationModel.getScriptSettings().put(
        "SPC_MODLOADER_VERSION_SPC",
        configurationModel.getModLoaderVersion());

    configurationModel.getScriptSettings().put(
        "SPC_JAVA_ARGS_SPC",
        configurationModel.getJavaArgs());

    if (!configurationModel.getScriptSettings().containsKey("SPC_JAVA_SPC")) {

      configurationModel.getScriptSettings().put("SPC_JAVA_SPC", "java");
    }

    configurationModel.getScriptSettings().put(
        "SPC_FABRIC_INSTALLER_VERSION_SPC",
        VERSIONMETA.fabric().releaseInstaller());

    configurationModel.getScriptSettings().put(
        "SPC_QUILT_INSTALLER_VERSION_SPC",
        VERSIONMETA.quilt().releaseInstaller());

    configurationModel.getScriptSettings().put(
        "SPC_LEGACYFABRIC_INSTALLER_VERSION_SPC",
        VERSIONMETA.legacyFabric().releaseInstaller());
  }

  /**
   * Checks whether the passed list of directories which are supposed to be in the modpack directory
   * is empty and prints a message to the console and serverpackcreator.log if it is.<br> Checks
   * whether all directories in the list exist in the modpack directory and prints a message to the
   * console and serverpackcreator.log if any one of the directories could not be found. If the user
   * specified a {@code source/file;destination/file}-combination, it is checked whether the
   * specified source-file exists on the host.
   *
   * @param directoriesToCopy The list of directories, or
   *                          {@code source/file;destination/file}-combinations, to check for
   *                          existence. {@code  source/file;destination/file}-combinations must be
   *                          absolute paths to the source-file.
   * @param modpackDir        The path to the modpack directory in which to check for existence of
   *                          the passed list of directories.
   * @param encounteredErrors A list to which all encountered errors are saved to.
   * @return Boolean. Returns true if every directory was found in the modpack directory. If any
   * single one was not found, false is returned.
   * @author Griefed
   */
  public boolean checkCopyDirs(
      @NotNull final List<String> directoriesToCopy,
      @NotNull final String modpackDir,
      @NotNull final List<String> encounteredErrors) {
    boolean configCorrect = true;

    directoriesToCopy.removeIf(entry -> entry.matches("^\\s+$") || entry.isEmpty());

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

      if (directoriesToCopy.size() > 1 && directoriesToCopy.contains("lazy_mode")) {
        LOG.warn(
            "You specified lazy mode in your configuration, but your copyDirs configuration contains other entries. To use the lazy mode, only specify \"lazy_mode\" and nothing else. Ignoring lazy mode.");
      }

      directoriesToCopy.removeIf(entry -> entry.equals("lazy_mode"));

      for (String directory : directoriesToCopy) {

        // Check whether the user specified a source;destination-combination
        if (directory.contains(";")) {

          String[] sourceFileDestinationFileCombination = directory.split(";");

          File sourceFileToCheck =
              new File(modpackDir, sourceFileDestinationFileCombination[0]);

          if (!new File(modpackDir, sourceFileDestinationFileCombination[0])
              .isFile()
              && !new File(
              modpackDir, sourceFileDestinationFileCombination[0])
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

            if (new File(modpackDir, sourceFileDestinationFileCombination[0]).exists()
                && !UTILITIES.FileUtils()
                             .checkReadPermission(
                                 modpackDir + File.separator
                                     + sourceFileDestinationFileCombination[0])) {

              configCorrect = false;

              LOG.error(
                  "No read-permission for " + modpackDir + File.separator
                      + sourceFileDestinationFileCombination[0]);

              /* This log is meant to be read by the user, therefore we allow translation. */
              encounteredErrors.add(
                  String.format(
                      I18N.getMessage("configuration.log.error.checkcopydirs.read"),
                      modpackDir + File.separator + sourceFileDestinationFileCombination[0]));

            } else if (new File(modpackDir, sourceFileDestinationFileCombination[0])
                .exists()
                && !UTILITIES.FileUtils()
                             .checkReadPermission(
                                 modpackDir + File.separator
                                     + sourceFileDestinationFileCombination[0])) {

              configCorrect = false;

              LOG.error(
                  "No read-permission for " + modpackDir + File.separator
                      + sourceFileDestinationFileCombination[0]);

              /* This log is meant to be read by the user, therefore we allow translation. */
              encounteredErrors.add(
                  String.format(
                      I18N.getMessage("configuration.log.error.checkcopydirs.read"),
                      modpackDir + File.separator + sourceFileDestinationFileCombination[0]));

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

            if (new File(modpackDir, sourceFileDestinationFileCombination[0])
                .isDirectory()) {

              //noinspection ConstantConditions
              for (File file :
                  new File(modpackDir, sourceFileDestinationFileCombination[0])
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

          if (directory.substring(1).contains("==")) {

            if (!checkRegex(modpackDir, directory, true, encounteredErrors)) {
              configCorrect = false;
            }

          } else {

            File fileOrDirectory = new File(modpackDir, directory.substring(1));

            if (fileOrDirectory.isFile()) {

              LOG.warn("File " + directory.substring(1) + " will be ignored.");

            } else if (fileOrDirectory.isDirectory()) {

              LOG.warn("Directory " + directory.substring(1) + " will be ignored.");

            } else {

              LOG.debug("What? " + fileOrDirectory + " is neither a file nor directory.");
            }
          }

        } else if (directory.contains("==")) {

          if (!checkRegex(modpackDir, directory, false, encounteredErrors)) {
            configCorrect = false;
          }

          // Check if the entry exists
        } else {

          File dirToCheck = new File(modpackDir, directory);

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
   * Check the given entry for valid regex. In order for an entry to be valid, it must
   * <ol>
   *   <li>Contain {@code ==}</li>
   *   <li>Optionally start with {@code ==}. When starting with {@code ==}</li>
   *   <li><ul>
   *     <li>The left side of {@code ==} must specify an existing directory</li>
   *     <li>The right side of {@code ==} must be the regex to match files and/or directories</li>
   *   </ul></li>
   * </ol>
   *
   * @param modpackDir The modpacks directory which will be checked when the entry starts with
   *                   {@code ==}
   * @param entry      The regex, or file/directory and regex, combination.
   * @param exclusion  Whether the checks are for exclusions ({@code true}) or files and/or
   *                   directories, or inclusions ({@code false}).
   * @return {@code true} when no errors were encountered.
   * @author Griefed
   */
  public boolean checkRegex(@NotNull final String modpackDir,
                            @NotNull final String entry,
                            boolean exclusion) {
    return checkRegex(modpackDir, entry, exclusion, new ArrayList<>(1));
  }

  /**
   * Check the given entry for valid regex. In order for an entry to be valid, it must
   * <ul>
   *   <li>Contain {@code ==}</li>
   *   <li>Optionally start with {@code ==}. When starting with {@code ==}</li>
   *   <li><ul>
   *     <li>The left side of {@code ==} must specify an existing directory</li>
   *     <li>The right side of {@code ==} must be the regex to match files and/or directories</li>
   *   </ul></li>
   * </ul>
   *
   * @param modpackDir        The modpacks directory which will be checked when the entry starts
   *                          with {@code ==}
   * @param entry             The regex, or file/directory and regex, combination.
   * @param exclusion         Whether the checks are for exclusions ({@code true}) or files and/or
   *                          directories, or inclusions ({@code false}).
   * @param encounteredErrors A list to which all encountered errors are saved to.
   * @return {@code true} when no errors were encountered.
   * @author Griefed
   */
  public boolean checkRegex(@NotNull final String modpackDir,
                            @NotNull final String entry,
                            boolean exclusion,
                            @NotNull final List<String> encounteredErrors) {
    try {
      if (exclusion) {
        return exclusionRegexCheck(modpackDir, entry, encounteredErrors);
      } else {
        return inclusionRegexCheck(modpackDir, entry, encounteredErrors);
      }
    } catch (PatternSyntaxException ex) {
      LOG.error(
          "Invalid regex specified: " + entry + ". Error near regex-index " + ex.getIndex() + ".");
      encounteredErrors.add(I18N.getMessage(String.format(
          I18N.getMessage(""),
          entry,
          ex.getIndex())));
      return false;
    }
  }

  /**
   * Exclusion regex checks when the entry is prefixed with an {@code !}.
   *
   * @param modpackDir        The modpacks directory which will be checked when the entry starts
   *                          with {@code ==}
   * @param entry             The regex, or file/directory and regex, combination.
   * @param encounteredErrors A list to which all encountered errors are saved to.
   * @return {@code true} if the specified entry matched existing file(s) or directories.
   * @author Griefed
   */
  private boolean exclusionRegexCheck(@NotNull final String modpackDir,
                                      @NotNull final String entry,
                                      @NotNull final List<String> encounteredErrors) {
    /*
     * Check for matches in modpack directory
     */
    if (entry.startsWith("!==") && entry.length() > 3) {

      File source = new File(modpackDir);
      regexWalk(source, entry);
      return true;

    } else if (entry.contains("==") && entry.split("==").length == 2) {

      String[] sourceRegex = entry.split("==");

      /*
       * Matches inside modpack-directory
       */
      if (new File(modpackDir, sourceRegex[0].substring(1)).isDirectory()) {

        File source = new File(modpackDir, sourceRegex[0].substring(1));
        regexWalk(source, sourceRegex[1]);
        return true;

        /*
         * Matches inside directory outside modpack-directory
         */
      } else if (new File(sourceRegex[0].substring(1)).isDirectory()) {

        File source = new File(sourceRegex[0].substring(1));
        regexWalk(source, sourceRegex[1]);
        return true;

      } else {

        encounteredErrors.add(String.format(
            I18N.getMessage("configuration.log.error.checkcopydirs.checkforregex"),
            sourceRegex[0]));
        return false;
      }

    } else {

      encounteredErrors.add(
          I18N.getMessage("configuration.log.error.checkcopydirs.checkforregex.invalid"));
      return false;
    }
  }

  /**
   * Inclusion regex checks.
   *
   * @param modpackDir        The modpacks directory which will be checked when the entry starts
   *                          with {@code ==}
   * @param entry             The regex, or file/directory and regex, combination.
   * @param encounteredErrors A list to which all encountered errors are saved to.
   * @return {@code true} if the specified entry matched existing file(s) or directories.
   * @author Griefed
   */
  private boolean inclusionRegexCheck(@NotNull final String modpackDir,
                                      @NotNull final String entry,
                                      @NotNull final List<String> encounteredErrors) {
    AtomicInteger counter = new AtomicInteger();
    AtomicReference<String> toMatch = new AtomicReference<>();

    /*
     * Check for matches in modpack directory
     */
    if (entry.startsWith("==") && entry.length() > 2) {

      File source = new File(modpackDir);
      regexWalk(source, entry);
      return true;

      /*
       * Check for matches in the specified directory
       */
    } else if (entry.contains("==") && entry.split("==").length == 2) {

      String[] sourceRegex = entry.split("==");

      /*
       * Matches inside modpack-directory
       */
      if (new File(modpackDir, sourceRegex[0]).isDirectory()) {

        File source = new File(modpackDir, sourceRegex[0]);
        regexWalk(source, sourceRegex[1]);
        return true;

        /*
         * Matches inside directory outside modpack-directory
         */
      } else if (new File(sourceRegex[0]).isDirectory()) {

        File source = new File(sourceRegex[0]);
        regexWalk(source, sourceRegex[1]);
        return true;


      } else {

        encounteredErrors.add(String.format(
            I18N.getMessage("configuration.log.error.checkcopydirs.checkforregex"),
            sourceRegex[0]));
        return false;
      }

    } else {

      encounteredErrors.add(
          I18N.getMessage("configuration.log.error.checkcopydirs.checkforregex.invalid"));
      return false;
    }
  }

  /**
   * Walk through each file in the specified source-directory and perform regex-matches using the
   * specified regex. The number of matches found is printed to the logs.
   *
   * @param source The source directory to walk through and perform regex-matches on.
   * @param regex  The regex to use for finding matches within the specified source-directory.
   * @author Griefed
   */
  private void regexWalk(@NotNull final File source,
                         @NotNull final String regex) {
    AtomicInteger counter = new AtomicInteger();
    AtomicReference<String> toMatch = new AtomicReference<>();

    try (Stream<Path> files = Files.walk(source.toPath())) {
      files.forEach(
          file -> {

            toMatch.set(file.toFile().getAbsolutePath().replace(
                source.getAbsolutePath(),
                ""));

            if (toMatch.get().startsWith(File.separator)) {
              toMatch.set(toMatch.get().substring(1));
            }

            if (toMatch.get().matches(regex)) {
              counter.addAndGet(1);
            }
          }
      );
    } catch (IOException ex) {
      LOG.error("Could not check your regex entry \""
                    + regex
                    + "\" in directory \""
                    + source, ex);
    }

    LOG.info("Regex \""
                 + regex
                 + "\" matched "
                 + counter + " files/folders.");
  }

  /**
   * Check a given ZIP-archives contents. If the ZIP-archive only contains one directory, or if it
   * contains neither the mods nor the config directories, consider it invalid.
   *
   * @param pathToZip         Path to the ZIP-file to check.
   * @param encounteredErrors List of encountered errors for further processing, like printing to
   *                          logs or display in GUI or whatever you want, really.
   * @return Boolean. {@code false} if the ZIP-archive is considered valid.
   * @author Griefed
   */
  public boolean checkZipArchive(@NotNull final Path pathToZip,
                                 @NotNull final List<String> encounteredErrors) {

    ZipFile modpackZip;

    try (ZipFile zipFile = new ZipFile(pathToZip.toString())) {

      if (!zipFile.isValidZipFile()) {

        return true;

      } else {

        modpackZip = zipFile;
      }

    } catch (IOException ex) {
      LOG.error("Could not validate ZIP-file " + pathToZip + ".", ex);
      return true;
    }

    try {
      List<String> foldersInModpackZip =
          getDirectoriesInModpackZipBaseDirectory(modpackZip);

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
      } else if (!foldersInModpackZip.contains("mods/")
          || !foldersInModpackZip.contains("config/")) {

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
   * Update the destination to which the ZIP-archive will the extracted to, based on whether a
   * directory of the same name already exists.
   *
   * @param destination The destination to where the ZIP-archive was about to be extracted to.
   * @return The destination where the ZIP-archive will be extracted to.
   * @author Griefed
   */
  private @NotNull String acquireDestination(@NotNull String destination) {
    // Does the modpack extracted from the ZIP-archive already exist?
    if (new File(destination).isDirectory()) {

      int incrementation = 0;

      // Has there been a previous getIncrementation?
      if (destination.matches(".*_\\d")) {

        incrementation = Integer.parseInt(destination.substring(destination.length() - 1));

        while (new File(destination.substring(0, destination.length() - 1) + "_" + incrementation)
            .isDirectory()) {
          incrementation++;
        }

        destination = destination.substring(0, destination.length() - 1) + "_" + incrementation;

        // No previous getIncrementation, but it exists. Append _0 to prevent overwrite.
      } else {
        while (new File(destination + "_" + incrementation).isDirectory()) {
          incrementation++;
        }

        destination = destination + "_" + incrementation;
      }
    }
    return new File(destination).getPath();
  }

  /**
   * Creates a list of suggested directories to include in server pack which is later on written to
   * a new configuration file. The list of directories to include in the server pack which is
   * generated by this method excludes well know directories which would not be needed by a server
   * pack. If you have suggestions to this list, open a feature request issue on <a
   * href="https://github.com/Griefed/ServerPackCreator/issues/new/choose">GitHub</a>
   *
   * @param modpackDir The directory for which to gather a list of directories to copy to the server
   *                   pack.
   * @return Directories inside the modpack, excluding well known client-side only directories.
   * @author Griefed
   */
  public @NotNull List<String> suggestCopyDirs(@NotNull final String modpackDir) {
    /* This log is meant to be read by the user, therefore we allow translation. */
    LOG.info("Preparing a list of directories to include in server pack...");

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
      LOG.error(
          "Error: Something went wrong during the setup of the modpack. Copy dirs should never be empty. Please check the logs for errors and open an issue on https://github.com/Griefed/ServerPackCreator/issues.",
          np);
    }

    for (int idirs = 0; idirs < APPLICATIONPROPERTIES.getDirectoriesToExclude().size(); idirs++) {

      int i = idirs;

      dirsInModpack.removeIf(
          n -> (n.contains(APPLICATIONPROPERTIES.getDirectoriesToExclude().get(i))));
    }

    LOG.info(
        "Modpack directory checked. Suggested directories for copyDirs-setting are: "
            + dirsInModpack);

    return dirsInModpack;
  }

  /**
   * Check whether various manifests from various launchers exist and use them to update our
   * ConfigurationModel and pack name.
   *
   * @param destination        The destination in which the manifests are.
   * @param configurationModel The ConfigurationModel to update.
   * @param encounteredErrors  A list of errors encountered during configuration checks, to be added
   *                           to in case an error is encountered during manifest checks.
   * @return The name of the modpack currently being checked. {@code null} if the name could not be
   * acquired.
   * @author Griefed
   */
  public @Nullable String checkManifests(@NotNull final String destination,
                                         @NotNull final ConfigurationModel configurationModel,
                                         @NotNull final List<String> encounteredErrors) {
    String packName = null;

    if (new File(destination, "manifest.json").exists()) {

      try {

        updateConfigModelFromCurseManifest(
            configurationModel, new File(destination, "manifest.json"));

        packName = updatePackName(configurationModel, "name");

      } catch (IOException ex) {

        LOG.error("Error parsing CurseForge manifest.json from ZIP-file.", ex);

        /* This log is meant to be read by the user, therefore we allow translation. */
        encounteredErrors.add(I18N.getMessage("configuration.log.error.zip.manifest"));
      }

      // Check minecraftinstance.json usually created by Overwolf's CurseForge launcher.
    } else if (new File(destination, "minecraftinstance.json").exists()) {

      try {
        updateConfigModelFromMinecraftInstance(
            configurationModel, new File(destination, "minecraftinstance.json"));

        packName = updatePackName(configurationModel, "name");

      } catch (IOException ex) {

        LOG.error("Error parsing minecraftinstance.json from ZIP-file.", ex);

        /* This log is meant to be read by the user, therefore we allow translation. */
        encounteredErrors.add(I18N.getMessage("configuration.log.error.zip.instance"));
      }

      // Check modrinth.index.json usually available if the modpack is from Modrinth
    } else if (new File(destination, "modrinth.index.json").exists()) {

      try {
        updateConfigModelFromModrinthManifest(
            configurationModel, new File(destination, "modrinth.index.json"));

        packName = updatePackName(configurationModel, "name");

      } catch (IOException ex) {

        LOG.error("Error parsing modrinth.index.json from ZIP-file.", ex);

        /* This log is meant to be read by the user, therefore we allow translation. */
        encounteredErrors.add(I18N.getMessage("configuration.log.error.zip.config"));
      }

      // Check instance.json usually created by ATLauncher
    } else if (new File(destination, "instance.json").exists()) {

      try {
        updateConfigModelFromATLauncherInstance(
            configurationModel, new File(destination, "instance.json"));

        // If JSON was acquired, get the name of the modpack and overwrite newDestination using
        // modpack name.
        if (UTILITIES.JsonUtilities()
                     .getNestedText(configurationModel.getModpackJson(), "launcher", "name")
            == null) {
          packName = UTILITIES.JsonUtilities()
                              .getNestedText(configurationModel.getModpackJson(), "launcher",
                                             "pack");
        } else {
          packName = UTILITIES.JsonUtilities()
                              .getNestedText(configurationModel.getModpackJson(), "launcher",
                                             "name");
        }

      } catch (IOException ex) {

        LOG.error("Error parsing config.json from ZIP-file.", ex);

        /* This log is meant to be read by the user, therefore we allow translation. */
        encounteredErrors.add(I18N.getMessage("configuration.log.error.zip.config"));
      }

      // Check the config.json usually created by GDLauncher.
    } else if (new File(destination, "config.json").exists()) {

      try {
        updateConfigModelFromConfigJson(
            configurationModel, new File(destination, "config.json"));

        // If JSON was acquired, get the name of the modpack and overwrite newDestination using
        // modpack name.
        packName = UTILITIES.JsonUtilities()
                            .getNestedText(configurationModel.getModpackJson(), "loader",
                                           "sourceName");

      } catch (IOException ex) {

        LOG.error("Error parsing config.json from ZIP-file.", ex);

        /* This log is meant to be read by the user, therefore we allow translation. */
        encounteredErrors.add(I18N.getMessage("configuration.log.error.zip.config"));
      }

      // Check mmc-pack.json usually created by MultiMC.
    } else if (new File(destination, "mmc-pack.json").exists()) {

      try {
        updateConfigModelFromMMCPack(
            configurationModel, new File(destination, "mmc-pack.json"));

      } catch (IOException ex) {

        LOG.error("Error parsing mmc-pack.json from ZIP-file.", ex);

        /* This log is meant to be read by the user, therefore we allow translation. */
        encounteredErrors.add(I18N.getMessage("configuration.log.error.zip.mmcpack"));
      }

      try {

        if (new File(destination, "instance.cfg").exists()) {

          packName = updateDestinationFromInstanceCfg(
              new File(destination, "instance.cfg"));
        }

      } catch (IOException ex) {
        LOG.error("Couldn't read instance.cfg.", ex);
      }
    }

    return packName;
  }

  /**
   * Check whether a server pack for the given destination already exists and get an incrementor
   * based on whether one exists, how many, or none exist. Think if this as the incrementation
   * Windows does when a file of the same name is copied. {@code foo.bar} becomes
   * {@code foo (1).bar} etc.
   *
   * @param packName          The name of the modpack.
   * @param wouldBeServerPack The name of the server pack about to be generated.
   * @return An incremented number, based on whether a server pack of the same name already exists.
   * @author Griefed
   */
  private int getIncrementation(@NotNull final String packName,
                                @NotNull final String wouldBeServerPack) {
    // Check whether a server pack for the new destination already exists.
    // If it does, we need to change it to avoid overwriting any existing files.
    int incrementation = 0;

    // If no name was acquired from the ZIP-archive, has there been a previous getIncrementation of the
    // pack?
    if (packName.matches(".*_\\d")) {

      // Has there been a previous getIncrementation of the would-be-server-pack?
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

        // No previous getIncrementation of the would-be-server-pack.
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

      // Has there been a previous getIncrementation of the would-be-server-pack?
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

        // No previous getIncrementation of the would-be-server-pack.
      } else {

        // Increment until both a free would-be-server-pack and packname are found.
        while (new File(wouldBeServerPack + "_" + incrementation).isDirectory()
            || new File(packName + "_" + incrementation).isDirectory()) {

          incrementation++;
        }
      }
    }
    return incrementation;
  }

  /**
   * Prints all passed fields to the console and serverpackcreator.log. Used to show the user the
   * configuration before ServerPackCreator starts the generation of the server pack or, if checks
   * failed, to show the user their last configuration, so they can more easily identify problems
   * with said configuration.<br> Should a user report an issue on GitHub and include their logs
   * (which I hope they do....), this would also help me help them. Logging is good. People should
   * use more logging.
   *
   * @param modpackDirectory     The used modpackDir field either from a configuration file or from
   *                             configuration setup.
   * @param clientsideMods       List of clientside-only mods to exclude from the server pack...
   * @param copyDirectories      List of directories in the modpack which are to be included in the
   *                             server pack.
   * @param installServer        Whether to install the modloader server in the server pack.
   * @param minecraftVer         The Minecraft version the modpack uses.
   * @param modloader            The modloader the modpack uses.
   * @param modloaderVersion     The version of the modloader the modpack uses.
   * @param includeIcon          Whether to include the server-icon.png in the server pack.
   * @param includeProperties    Whether to include the server.properties in the server pack.
   * @param includeZip           Whether to create a zip-archive of the server pack, excluding the
   *                             Minecraft server JAR according to Mojang's TOS and EULA.
   * @param javaArgs             Java arguments to write the start-scripts with.
   * @param serverPackSuffix     Suffix to append to name of the server pack to be generated.
   * @param serverIconPath       The path to the custom server-icon.png to be used in the server
   *                             pack.
   * @param serverPropertiesPath The path to the custom server.properties to be used in the server
   *                             pack.
   * @param scriptSettings       Custom settings for start script creation. {@code KEY}s are the
   *                             placeholder, {@code VALUE}s are the values with which the
   *                             placeholders are to be replaced.
   * @author Griefed
   */
  public void printConfigurationModel(
      @NotNull final String modpackDirectory,
      @NotNull final List<String> clientsideMods,
      @NotNull final List<String> copyDirectories,
      boolean installServer,
      @NotNull final String minecraftVer,
      @NotNull final String modloader,
      @NotNull final String modloaderVersion,
      boolean includeIcon,
      boolean includeProperties,
      boolean includeZip,
      @NotNull final String javaArgs,
      @NotNull final String serverPackSuffix,
      @NotNull final String serverIconPath,
      @NotNull final String serverPropertiesPath,
      @NotNull final HashMap<String, String> scriptSettings) {

    LOG.info("Your configuration is:");
    LOG.info("Modpack directory: " + modpackDirectory);

    if (clientsideMods.isEmpty()) {
      /* This log is meant to be read by the user, therefore we allow translation. */
      LOG.warn("No client mods specified.");
    } else {

      /* This log is meant to be read by the user, therefore we allow translation. */
      LOG.info("Client mods specified. Client mods are:");
      UTILITIES.ListUtils().printListToLogChunked(clientsideMods, 5, "    ", true);
    }

    /* This log is meant to be read by the user, therefore we allow translation. */
    LOG.info("Directories to copy:");

    for (String directory : copyDirectories) {
      LOG.info(String.format("    %s", directory));
    }

    /* This log is meant to be read by the user, therefore we allow translation. */
    LOG.info("Include server installation:      " + installServer);
    LOG.info("Minecraft version:                " + minecraftVer);
    LOG.info("Modloader:                        " + modloader);
    LOG.info("Modloader Version:                " + modloaderVersion);
    LOG.info("Include server icon:              " + includeIcon);
    LOG.info("Include server properties:        " + includeProperties);
    LOG.info("Create zip-archive of serverpack: " + includeZip);
    LOG.info("Java arguments for start-scripts: " + javaArgs);
    LOG.info("Server pack suffix:               " + serverPackSuffix);
    LOG.info("Path to custom server-icon:       " + serverIconPath);
    LOG.info("Path to custom server.properties: " + serverPropertiesPath);
    LOG.info("Script settings:");
    for (Map.Entry<String, String> entry : scriptSettings.entrySet()) {
      LOG.info("  Placeholder: " + entry.getKey());
      LOG.info("        Value: " + entry.getValue());
    }
  }

  /**
   * Acquire a list of directories in the base-directory of a ZIP-file.
   *
   * @param zipFile The ZIP-archive to get the list of files from.
   * @return All directories in the base-directory of the ZIP-file.
   * @throws IllegalArgumentException         if the pre-conditions for the uri parameter are not
   *                                          met, or the env parameter does not contain properties
   *                                          required by the provider, or a property value is
   *                                          invalid.
   * @throws FileSystemAlreadyExistsException if the file system has already been created.
   * @throws ProviderNotFoundException        if a provider supporting the URI scheme is not
   *                                          installed.
   * @throws IOException                      if an I/O error occurs creating the file system.
   * @throws SecurityException                if a security manager is installed, and it denies an
   *                                          unspecified permission required by the file system
   *                                          provider implementation.
   * @author Griefed
   */
  public @NotNull List<String> getDirectoriesInModpackZipBaseDirectory(@NotNull final ZipFile zipFile)
      throws IllegalArgumentException, FileSystemAlreadyExistsException, ProviderNotFoundException,
      IOException, SecurityException {

    List<String> baseDirectories = new ArrayList<>(100);

    zipFile
        .getFileHeaders()
        .forEach(
            fileHeader -> {
              if (fileHeader.getFileName().matches("^\\w+[/\\\\]$")) {
                baseDirectories.add(fileHeader.getFileName());
              }
            });

    return baseDirectories;
  }

  /**
   * <strong>{@code manifest.json}</strong>
   *
   * <p>Update the given ConfigurationModel with values gathered from the downloaded CurseForge
   * modpack. A manifest.json-file is usually created when a modpack is exported through launchers
   * like Overwolf's CurseForge or GDLauncher.
   *
   * @param configurationModel An instance containing a configuration for a modpack from which to
   *                           create a server pack.
   * @param manifest           File. The CurseForge manifest.json-file of the modpack to read.
   * @throws IOException when the manifest.json-file could not be parsed.
   * @author Griefed
   */
  public void updateConfigModelFromCurseManifest(
      @NotNull final ConfigurationModel configurationModel,
      @NotNull final File manifest) throws IOException {

    configurationModel.setModpackJson(UTILITIES.JsonUtilities().getJson(manifest));

    String[] modloaderAndVersion =
        configurationModel
            .getModpackJson()
            .get("minecraft")
            .get("modLoaders")
            .get(0)
            .get("id")
            .asText()
            .split("-");

    configurationModel.setMinecraftVersion(
        configurationModel.getModpackJson().get("minecraft").get("version").asText());

    configurationModel.setModLoader(modloaderAndVersion[0]);

    configurationModel.setModLoaderVersion(modloaderAndVersion[1]);
  }

  /**
   * Acquire the modpacks name from the JSON previously acquired and stored in the
   * ConfigurationModel.
   *
   * @param configurationModel The ConfigurationModel containing the JsonNode from which to acquire
   *                           the modpacks name.
   * @param childNodes         The child nodes, in order, which contain the requested packname.
   * @return The new name of the modpack.
   * @author Griefed
   */
  private @Nullable String updatePackName(@NotNull final ConfigurationModel configurationModel,
                                          @NotNull final String... childNodes) {
    try {

      return APPLICATIONPROPERTIES.modpacksDirectory()
          + File.separator
          + UTILITIES.JsonUtilities()
                     .getNestedText(
                         configurationModel.getModpackJson(),
                         childNodes);

    } catch (NullPointerException npe) {

      return null;
    }
  }

  /**
   * <strong>{@code minecraftinstance.json}</strong>
   *
   * <p>Update the given ConfigurationModel with values gathered from the minecraftinstance.json of
   * the modpack. A minecraftinstance.json is usually created by Overwolf's CurseForge launcher.
   *
   * @param configurationModel An instance containing a configuration for a modpack from which to
   *                           create a server pack.
   * @param minecraftInstance  File. The minecraftinstance.json-file of the modpack to read.
   * @throws IOException when the minecraftinstance.json-file could not be parsed.
   * @author Griefed
   */
  public void updateConfigModelFromMinecraftInstance(
      @NotNull final ConfigurationModel configurationModel,
      @NotNull final File minecraftInstance) throws IOException {

    configurationModel.setModpackJson(UTILITIES.JsonUtilities().getJson(minecraftInstance));

    configurationModel.setModLoader(
        getModLoaderCase(
            configurationModel
                .getModpackJson()
                .get("baseModLoader")
                .get("name")
                .asText()
                .split("-")[0]));

    configurationModel.setModLoaderVersion(
        configurationModel.getModpackJson().get("baseModLoader").get("forgeVersion").asText());

    configurationModel.setMinecraftVersion(
        configurationModel.getModpackJson().get("baseModLoader").get("minecraftVersion").asText());
  }

  /**
   * <strong>{@code modrinth.index.json}</strong>
   *
   * <p>Update the given ConfigurationModel with values gathered from a Modrinth {@code
   * modrinth.index.json}-manifest.
   *
   * @param configurationModel The model to update.
   * @param manifest           The manifest file.
   * @throws IOException when the modrinth.index.json-file could not be parsed.
   * @author Griefed
   */
  public void updateConfigModelFromModrinthManifest(
      @NotNull final ConfigurationModel configurationModel,
      @NotNull final File manifest) throws IOException {

    configurationModel.setModpackJson(UTILITIES.JsonUtilities().getJson(manifest));

    configurationModel.setMinecraftVersion(
        configurationModel.getModpackJson().get("dependencies").get("minecraft").asText());

    for (Iterator<Entry<String, JsonNode>> it =
        configurationModel.getModpackJson().get("dependencies").fields();
        it.hasNext(); ) {
      Entry<String, JsonNode> dependencyEntry = it.next();

      switch (dependencyEntry.getKey()) {
        case "fabric-loader":
          configurationModel.setModLoader("Fabric");
          configurationModel.setModLoaderVersion(dependencyEntry.getValue().asText());
          break;

        case "quilt-loader":
          configurationModel.setModLoader("Quilt");
          configurationModel.setModLoaderVersion(dependencyEntry.getValue().asText());
          break;

        case "forge":
          configurationModel.setModLoader("Forge");
          configurationModel.setModLoaderVersion(dependencyEntry.getValue().asText());
          break;
      }
    }
  }

  /**
   * <strong>{@code instance.json}</strong>
   *
   * <p>Update the given ConfigurationModel with values gathered from a ATLauncher manifest.
   *
   * @param configurationModel The model to update.
   * @param manifest           The manifest file.
   * @throws IOException when the instance.json-file could not be parsed.
   * @author Griefed
   */
  public void updateConfigModelFromATLauncherInstance(
      @NotNull final ConfigurationModel configurationModel,
      @NotNull final File manifest) throws IOException {

    configurationModel.setModpackJson(UTILITIES.JsonUtilities().getJson(manifest));

    configurationModel.setMinecraftVersion(configurationModel.getModpackJson().get("id").asText());

    configurationModel.setModLoader(
        configurationModel
            .getModpackJson()
            .get("launcher")
            .get("loaderVersion")
            .get("type")
            .asText());

    configurationModel.setModLoaderVersion(
        configurationModel
            .getModpackJson()
            .get("launcher")
            .get("loaderVersion")
            .get("version")
            .asText());
  }

  /**
   * <strong>{@code config.json}</strong>
   *
   * <p>Update the given ConfigurationModel with values gathered from the modpacks config.json. A
   * config.json is usually created by GDLauncher.
   *
   * @param configurationModel An instance containing a configuration for a modpack from which to
   *                           create a server pack.
   * @param config             The config.json-file of the modpack to read.
   * @throws IOException when the config.json-file could not be parsed.
   * @author Griefed
   */
  public void updateConfigModelFromConfigJson(@NotNull final ConfigurationModel configurationModel,
                                              @NotNull final File config)
      throws IOException {

    configurationModel.setModpackJson(UTILITIES.JsonUtilities().getJson(config));

    configurationModel.setModLoader(
        getModLoaderCase(
            configurationModel.getModpackJson().get("loader").get("loaderType").asText()));

    configurationModel.setMinecraftVersion(
        configurationModel.getModpackJson().get("loader").get("mcVersion").asText());

    configurationModel.setModLoaderVersion(
        configurationModel
            .getModpackJson()
            .get("loader")
            .get("loaderVersion")
            .asText()
            .replace(configurationModel.getMinecraftVersion() + "-", ""));
  }

  /**
   * <strong>{@code mmc-pack.json}</strong>
   *
   * <p>Update the given ConfigurationModel with values gathered from the modpacks mmc-pack.json. A
   * mmc-pack.json is usually created by the MultiMC launcher.
   *
   * @param configurationModel An instance containing a configuration for a modpack from which to
   *                           create a server pack.
   * @param mmcPack            The config.json-file of the modpack to read.
   * @throws IOException when the mmc-pack.json-file could not be parsed.
   * @author Griefed
   */
  public void updateConfigModelFromMMCPack(@NotNull final ConfigurationModel configurationModel,
                                           @NotNull final File mmcPack)
      throws IOException {

    configurationModel.setModpackJson(UTILITIES.JsonUtilities().getJson(mmcPack));

    for (JsonNode jsonNode : configurationModel.getModpackJson().get("components")) {

      switch (jsonNode.get("uid").asText()) {
        case "net.minecraft":
          configurationModel.setMinecraftVersion(jsonNode.get("version").asText());
          break;

        case "net.minecraftforge":
          configurationModel.setModLoader("Forge");
          configurationModel.setModLoaderVersion(jsonNode.get("version").asText());
          break;

        case "net.fabricmc.fabric-loader":
          configurationModel.setModLoader("Fabric");
          configurationModel.setModLoaderVersion(jsonNode.get("version").asText());
          break;

        case "org.quiltmc.quilt-loader":
          configurationModel.setModLoader("Quilt");
          configurationModel.setModLoaderVersion(jsonNode.get("version").asText());
          break;
      }
    }
  }

  /**
   * <strong>{@code instance.cfg}</strong>
   *
   * <p>Acquire the name of the modpack/instance of a MultiMC modpack from the modpacks
   * instance.cfg, which is usually created by the MultiMC launcher.
   *
   * @param instanceCfg The config.json-file of the modpack to read.
   * @return The instance name.
   * @throws IOException when the file could not be found or the properties not be loaded from the
   *                     file.
   * @author Griefed
   */
  public @NotNull String updateDestinationFromInstanceCfg(@NotNull final File instanceCfg)
      throws IOException {

    String name;

    try (InputStream inputStream = Files.newInputStream(instanceCfg.toPath())) {

      Properties properties = new Properties();
      properties.load(inputStream);

      name = properties.getProperty("name", null);
    }

    return name;
  }

  /**
   * Ensures the modloader is normalized to first letter upper case and rest lower case. Basically
   * allows the user to input Forge or Fabric in any combination of upper- and lowercase and
   * ServerPackCreator will still be able to work with the users input.
   *
   * @param modloader Modloader String-representation to normalize.
   * @return A normalized String of the specified modloader.
   * @author Griefed
   */
  public @NotNull String getModLoaderCase(@NotNull final String modloader) {

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
   * Check the passed configuration-file. If any check returns {@code true} then the server pack
   * will not be created. In order to find out which check failed, the user has to check their
   * serverpackcreator.log in the logs-directory.
   *
   * @param configFile The configuration file to check. Must either be an existing file to load a
   *                   configuration from or null if you want to use the passed configuration
   *                   model.
   * @param quietCheck Whether the configuration should be printed to the console and logs. Pass
   *                   false to quietly check the configuration.
   * @return {@code false} if the configuration has passed all tests.
   * @author Griefed
   */
  public boolean checkConfiguration(@NotNull final File configFile,
                                    boolean quietCheck) {

    List<String> encounteredErrors = new ArrayList<>(100);

    ConfigurationModel configurationModel = new ConfigurationModel();

    return checkConfiguration(configFile, configurationModel, encounteredErrors, quietCheck);
  }

  /**
   * Check the passed configuration-file. If any check returns {@code true} then the server pack
   * will not be created. In order to find out which check failed, the user has to check their
   * serverpackcreator.log in the logs-directory.
   *
   * @param configFile         The configuration file to check. Must either be an existing file to
   *                           load a configuration from or null if you want to use the passed
   *                           configuration model.
   * @param configurationModel Instance of a configuration of a modpack. Can be used to further
   *                           display or use any information within, as it may be changed or
   *                           otherwise altered by this method.
   * @param quietCheck         Whether the configuration should be printed to the console and logs.
   *                           Pass false to quietly check the configuration.
   * @return {@code false} if the configuration has passed all tests.
   * @author Griefed
   */
  public boolean checkConfiguration(
      @NotNull final File configFile,
      @NotNull final ConfigurationModel configurationModel,
      boolean quietCheck) {

    List<String> encounteredErrors = new ArrayList<>(100);

    return checkConfiguration(configFile, configurationModel, encounteredErrors, quietCheck);
  }

  /**
   * Check the passed {@link ConfigurationModel}. If any check returns {@code true} then the server
   * pack will not be created. In order to find out which check failed, the user has to check their
   * serverpackcreator.log in the logs-directory.
   *
   * @param configurationModel Instance of a configuration of a modpack. Can be used to further
   *                           display or use any information within, as it may be changed or
   *                           otherwise altered by this method.
   * @param quietCheck         Whether the configuration should be printed to the console and logs.
   *                           Pass false to quietly check the configuration.
   * @return {@code false} if the configuration has passed all tests.
   * @author Griefed
   */
  public boolean checkConfiguration(
      @NotNull final ConfigurationModel configurationModel,
      boolean quietCheck) {

    List<String> encounteredErrors = new ArrayList<>(100);

    return checkConfiguration(configurationModel, encounteredErrors, quietCheck);
  }

  /**
   * Check the passed directory for existence and whether it is a directory, rather than a file.
   *
   * @param modpackDir The modpack directory.
   * @return {@code true} if the directory exists.
   * @author Griefed
   */
  public boolean checkModpackDir(@NotNull final String modpackDir) {
    return checkModpackDir(modpackDir, new ArrayList<>(5));
  }

  /**
   * Checks whether the passed String is empty and if it is empty, prints the corresponding message
   * to the console and serverpackcreator.log so the user knows what went wrong.<br> Checks whether
   * the passed String is a directory and if it is not, prints the corresponding message to the
   * console and serverpackcreator.log so the user knows what went wrong.
   *
   * @param modpackDir        Path to the modpack directory to check whether it is empty and whether
   *                          it is a directory.
   * @param encounteredErrors List to which all encountered errors are added to.
   * @return {@code true} if the directory exists.
   * @author Griefed
   */
  public boolean checkModpackDir(@NotNull final String modpackDir,
                                 @NotNull final List<String> encounteredErrors) {
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
   * specified a {@code source/file;destination/file}-combination, it is checked whether the
   * specified source-file exists on the host.
   *
   * @param directoriesToCopy Directories, or {@code source/file;destination/file}-combinations, to
   *                          check for existence.
   *                          {@code  source/file;destination/file}-combinations must be absolute
   *                          paths to the source-file.
   * @param modpackDir        Path to the modpack directory in which to check for existence of the
   *                          passed list of directories.
   * @return {@code true} if every directory was found in the modpack directory. If any single one
   * was not found, false is returned.
   * @author Griefed
   */
  public boolean checkCopyDirs(@NotNull final List<String> directoriesToCopy,
                               @NotNull final String modpackDir) {
    return checkCopyDirs(directoriesToCopy, modpackDir, new ArrayList<>(5));
  }

  /**
   * Check the given Minecraft and modloader versions for the specified modloader.
   *
   * @param modloader        The passed modloader which determines whether the check for Forge or
   *                         Fabric is called.
   * @param modloaderVersion The version of the modloader which is checked against the corresponding
   *                         modloaders manifest.
   * @param minecraftVersion The version of Minecraft used for checking the Forge version.
   * @return {@code true} if the specified modloader version was found in the corresponding
   * manifest.
   * @author Griefed
   */
  public boolean checkModloaderVersion(
      @NotNull final String modloader,
      @NotNull final String modloaderVersion,
      @NotNull final String minecraftVersion) {

    return checkModloaderVersion(modloader, modloaderVersion, minecraftVersion, new ArrayList<>(5));
  }

  /**
   * Acquire a list of all files and directories in a ZIP-file.
   *
   * @param zipFile The ZIP-archive to get the list of files from.
   * @return All files and directories in the ZIP-file.
   * @throws IllegalArgumentException         if the pre-conditions for the uri parameter are not
   *                                          met, or the env parameter does not contain properties
   *                                          required by the provider, or a property value is
   *                                          invalid.
   * @throws FileSystemAlreadyExistsException if the file system has already been created.
   * @throws ProviderNotFoundException        if a provider supporting the URI scheme is not
   *                                          installed.
   * @throws IOException                      if an I/O error occurs creating the file system.
   * @throws SecurityException                if a security manager is installed, and it denies an
   *                                          unspecified permission required by the file system
   *                                          provider implementation.
   * @author Griefed
   */
  public @NotNull List<String> getAllFilesAndDirectoriesInModpackZip(@NotNull final ZipFile zipFile)
      throws IllegalArgumentException, FileSystemAlreadyExistsException, ProviderNotFoundException,
      IOException, SecurityException {

    List<String> filesAndDirectories = new ArrayList<>(100);

    zipFile
        .getFileHeaders()
        .forEach(
            fileHeader -> {
              try {
                filesAndDirectories.addAll(getDirectoriesInModpackZip(zipFile));
              } catch (IOException ex) {
                LOG.error("Could not acquire file or directory from ZIP-archive.", ex);
              }
              try {
                filesAndDirectories.addAll(getFilesInModpackZip(zipFile));
              } catch (IOException ex) {
                LOG.error("Could not acquire file or directory from ZIP-archive.", ex);
              }
            });

    return filesAndDirectories;
  }

  /**
   * Acquire a list of all directories in a ZIP-file. The resulting list excludes files.
   *
   * @param zipFile The ZIP-archive to get the list of files from.
   * @return All directories in the ZIP-file.
   * @throws IllegalArgumentException         if the pre-conditions for the uri parameter are not
   *                                          met, or the env parameter does not contain properties
   *                                          required by the provider, or a property value is
   *                                          invalid.
   * @throws FileSystemAlreadyExistsException if the file system has already been created.
   * @throws ProviderNotFoundException        if a provider supporting the URI scheme is not
   *                                          installed.
   * @throws IOException                      if an I/O error occurs creating the file system.
   * @throws SecurityException                if a security manager is installed, and it denies an
   *                                          unspecified permission required by the file system
   *                                          provider implementation.
   * @author Griefed
   */
  public @NotNull List<String> getDirectoriesInModpackZip(@NotNull final ZipFile zipFile)
      throws IllegalArgumentException, FileSystemAlreadyExistsException, ProviderNotFoundException,
      IOException, SecurityException {

    List<String> directories = new ArrayList<>(100);

    zipFile
        .getFileHeaders()
        .forEach(
            fileHeader -> {
              if (fileHeader.isDirectory()) {
                directories.add(fileHeader.getFileName());
              }
            });

    return directories;
  }

  /**
   * Acquire a list of all files in a ZIP-file. The resulting list excludes directories.
   *
   * @param zipFile The ZIP-archive to get the list of files from.
   * @return All files in the ZIP-file.
   * @throws IllegalArgumentException         if the pre-conditions for the uri parameter are not
   *                                          met, or the env parameter does not contain properties
   *                                          required by the provider, or a property value is
   *                                          invalid.
   * @throws FileSystemAlreadyExistsException if the file system has already been created.
   * @throws ProviderNotFoundException        if a provider supporting the URI scheme is not
   *                                          installed.
   * @throws IOException                      if an I/O error occurs creating the file system.
   * @throws SecurityException                if a security manager is installed, and it denies an
   *                                          unspecified permission required by the file system
   *                                          provider implementation.
   * @author Griefed
   */
  public @NotNull List<String> getFilesInModpackZip(@NotNull final ZipFile zipFile)
      throws IllegalArgumentException, FileSystemAlreadyExistsException, ProviderNotFoundException,
      IOException, SecurityException {

    List<String> files = new ArrayList<>(100);

    zipFile
        .getFileHeaders()
        .forEach(
            fileHeader -> {
              if (!fileHeader.isDirectory()) {
                files.add(fileHeader.getFileName());
              }
            });

    return files;
  }
}
