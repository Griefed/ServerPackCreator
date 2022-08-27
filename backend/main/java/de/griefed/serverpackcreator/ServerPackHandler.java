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

import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import de.griefed.serverpackcreator.i18n.I18n;
import de.griefed.serverpackcreator.modscanning.ModScanner;
import de.griefed.serverpackcreator.spring.serverpack.ServerPackModel;
import de.griefed.serverpackcreator.utilities.common.Utilities;
import de.griefed.serverpackcreator.versionmeta.VersionMeta;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkPermission;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ExcludeFileFilter;
import net.lingala.zip4j.model.ZipParameters;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Create a server pack from a modpack by copying all specified or required files from the modpack
 * to the server pack as well as installing the modloader server for the specified modloader,
 * modloader version and Minecraft version. Create a ZIP-archive of the server pack, excluding the
 * Minecraft server JAR, for immediate upload to CurseForge or other platforms.
 *
 * @author Griefed
 */
@Component
public final class ServerPackHandler {

  private static final Logger LOG = LogManager.getLogger(ServerPackHandler.class);
  private static final Logger LOG_ADDONS = LogManager.getLogger("AddonsLogger");
  private static final Logger LOG_INSTALLER = LogManager.getLogger("InstallerLogger");

  private final VersionMeta VERSIONMETA;
  private final ModScanner MODSCANNER;
  private final ApplicationProperties APPLICATIONPROPERTIES;
  private final Utilities UTILITIES;
  private final ApplicationPlugins APPLICATIONPLUGINS;
  private final StopWatch STOPWATCH_SCANS = new StopWatch();
  private final String[] MOD_FILE_ENDINGS = new String[]{"jar", "disabled"};

  /**
   * <strong>Constructor</strong>
   *
   * <p>Used for Dependency Injection.
   *
   * <p>Receives an instance of {@link I18n} or creates one if the received one is null. Required
   * for use of localization.
   *
   * <p>
   *
   * @param injectedApplicationProperties Instance of {@link Properties} required for various
   *                                      different things.
   * @param injectedVersionMeta           Instance of {@link VersionMeta} required for everything
   *                                      version related.
   * @param injectedUtilities             Instance of {@link Utilities}.
   * @param injectedApplicationPlugins    Instance of {@link ApplicationPlugins}.
   * @param injectedModScanner            Instance of {@link ModScanner} required to determine
   *                                      sideness of mods.
   * @throws IOException if the {@link VersionMeta} could not be instantiated.
   * @author Griefed
   */
  @Autowired
  public ServerPackHandler(
      ApplicationProperties injectedApplicationProperties,
      VersionMeta injectedVersionMeta,
      Utilities injectedUtilities,
      ApplicationPlugins injectedApplicationPlugins,
      ModScanner injectedModScanner)
      throws IOException {

    this.APPLICATIONPROPERTIES = injectedApplicationProperties;
    this.VERSIONMETA = injectedVersionMeta;
    this.UTILITIES = injectedUtilities;
    this.APPLICATIONPLUGINS = injectedApplicationPlugins;
    this.MODSCANNER = injectedModScanner;
  }

  /**
   * Create a server pack from a given instance of {@link ServerPackModel} via webUI.
   *
   * @param serverPackModel An instance of {@link ServerPackModel} which contains the configuration
   *                        of the modpack.
   * @return Returns the passed {@link ServerPackModel} which got altered during the creation of
   * said server pack.
   * @author Griefed
   */
  public ServerPackModel run(@NotNull ServerPackModel serverPackModel) {

    String destination = getServerPackDestination(serverPackModel);

    run((ConfigurationModel) serverPackModel);

    cleanupEnvironment(false, destination);

    serverPackModel.setStatus("Available");
    serverPackModel.setSize(
        Double.parseDouble(
            String.valueOf(
                FileUtils.sizeOfAsBigInteger(new File(destination + "_server_pack.zip"))
                    .divide(BigInteger.valueOf(1048576)))));
    serverPackModel.setPath(destination + "_server_pack.zip");

    return serverPackModel;
  }

  /**
   * Acquire the destination directory in which the server pack will be generated. The directory in
   * which the server pack will be created has all its spaces replaces with underscores, so <code>
   * Survive Create Prosper 4 - 5.0.1</code> would become <code>Survive_Create_Prosper_4_-_5.0.1
   * </code> Even though it is the year 2022, spaces in paths can and do still cause trouble. Such
   * as for Powershell scripts. Powershell throws a complete fit if the path contains spaces....
   *
   * @param configurationModel Model containing the modpack directory of the modpack from which the
   *                           server pack will be generated.
   * @return The complete path to the directory in which the server pack will be generated.
   * @author Griefed
   */
  public String getServerPackDestination(ConfigurationModel configurationModel) {

    String serverPackToBe =
        configurationModel
            .getModpackDir()
            .substring(configurationModel.getModpackDir().lastIndexOf("/") + 1)
            + configurationModel.getServerPackSuffix();

    serverPackToBe = serverPackToBe.replace(" ", "_");

    return new File(
        String.format("%s/%s", APPLICATIONPROPERTIES.getDirectoryServerPacks(), serverPackToBe))
        .getAbsolutePath()
        .replace("\\", "/");
  }

  /**
   * Create a server pack from a given instance of {@link ConfigurationModel}.
   *
   * @param configurationModel An instance of {@link ConfigurationModel} which contains the
   *                           configuration of the modpack from which the server pack is to be
   *                           created.
   * @return Boolean. Returns true if the server pack was successfully generated.
   * @author Griefed
   */
  public boolean run(@NotNull ConfigurationModel configurationModel) {

    String destination = getServerPackDestination(configurationModel);

    /*
     * Check whether the server pack for the specified modpack already exists and whether overwrite is disabled.
     * If the server pack exists and overwrite is disabled, no new server pack will be generated.
     */
    if (!APPLICATIONPROPERTIES.isServerPacksOverwriteEnabled()
        && new File(destination).exists()) {

      LOG.info("Server pack already exists and overwrite disabled.");

    } else {

      // Make sure no files from previously generated server packs interrupt us.
      cleanupEnvironment(true, destination);

      try {
        Files.createDirectories(Paths.get(destination));
      } catch (IOException ignored) {

      }

      if (!APPLICATIONPLUGINS.pluginsPreGenExtension().isEmpty()) {
        LOG_ADDONS.info("Executing PreGenExtension addons.");
        APPLICATIONPLUGINS
            .pluginsPreGenExtension()
            .forEach(
                plugin -> {
                  LOG_ADDONS.info("Executing addon " + plugin.getName());

                  try {
                    plugin.run(APPLICATIONPROPERTIES, configurationModel, destination);
                  } catch (Exception | Error ex) {
                    LOG_ADDONS.error("Addon " + plugin.getName() + " encountered an error.", ex);
                  }
                });
      } else {
        LOG.info("No PreGenExtension addons to execute.");
      }

      // Recursively copy all specified directories and files, excluding clientside-only mods, to
      // server pack.
      copyFiles(configurationModel);

      // Create the start scripts for this server pack.
      createStartScripts(configurationModel);

      // If modloader is fabric, try and replace the old server-launch.jar with the new and improved
      // one which also downloads the Minecraft server.
      if (configurationModel.getModLoader().equalsIgnoreCase("Fabric")) {
        provideImprovedFabricServerLauncher(configurationModel);
      }

      // If true, copy the server-icon.png from server_files to the server pack.
      if (configurationModel.getIncludeServerIcon()) {
        copyIcon(configurationModel);
      } else {

        LOG.info("Not including servericon.");
      }

      // If true, copy the server.properties from server_files to the server pack.
      if (configurationModel.getIncludeServerProperties()) {
        copyProperties(configurationModel);
      } else {

        LOG.info("Not including server.properties.");
      }

      if (!APPLICATIONPLUGINS.pluginsPreZipExtension().isEmpty()) {
        LOG_ADDONS.info("Executing PreZipExtension addons.");
        APPLICATIONPLUGINS
            .pluginsPreZipExtension()
            .forEach(
                plugin -> {
                  LOG_ADDONS.info("Executing addon " + plugin.getName());

                  try {
                    plugin.run(APPLICATIONPROPERTIES, configurationModel, destination);
                  } catch (Exception | Error ex) {
                    LOG_ADDONS.error("Addon " + plugin.getName() + " encountered an error.", ex);
                  }
                });
      } else {
        LOG.info("No PreZipExtension addons to execute.");
      }

      // If true, create a ZIP-archive excluding the Minecraft server JAR of the server pack.
      if (configurationModel.getIncludeZipCreation()) {
        zipBuilder(configurationModel);
      } else {

        LOG.info("Not creating zip archive of serverpack.");
      }

      // If true, Install the modloader software for the specified Minecraft version, modloader,
      // modloader version
      if (configurationModel.getIncludeServerInstallation()) {
        installServer(configurationModel);
      } else {

        LOG.info("Not installing modded server.");
      }

      // Inform user about location of newly generated server pack.
      LOG.info("Server pack available at: " + destination);
      LOG.info("Server pack archive available at: " + destination + "_server_pack.zip");
      LOG.info("Done!");

      if (!APPLICATIONPLUGINS.pluginsPostGenExtension().isEmpty()) {
        LOG_ADDONS.info("Executing PostGenExtension addons.");
        APPLICATIONPLUGINS
            .pluginsPostGenExtension()
            .forEach(
                plugin -> {
                  LOG_ADDONS.info("Executing addon " + plugin.getName());

                  try {
                    plugin.run(APPLICATIONPROPERTIES, configurationModel, destination);
                  } catch (Exception | Error ex) {
                    LOG_ADDONS.error("Addon " + plugin.getName() + " encountered an error.", ex);
                  }
                });
      } else {
        LOG.info("No PostGenExtension addons to execute.");
      }
    }
    return true;
  }

  /**
   * Download and provide the improved Fabric Server Launcher, if it is available for the given
   * Minecraft and Fabric version.
   *
   * @param configurationModel ConfigurationModel containing the Minecraft and Fabric version for
   *                           which to acquire the improved Fabric Server Launcher.
   * @author Griefed
   */
  public void provideImprovedFabricServerLauncher(ConfigurationModel configurationModel) {
    provideImprovedFabricServerLauncher(configurationModel.getMinecraftVersion(),
        configurationModel.getModLoaderVersion(), getServerPackDestination(configurationModel));
  }

  /**
   * Download and provide the improved Fabric Server Launcher, if it is available for the given
   * Minecraft and Fabric version.
   *
   * @param minecraftVersion The Minecraft version the modpack uses and the Fabric Server Launcher
   *                         should be downloaded for.
   * @param fabricVersion    The modloader version the modpack uses and the Fabric Server Launcher
   *                         should be downloaded for.
   * @param destination      The destination of the server pack.
   * @author Griefed
   */
  public void provideImprovedFabricServerLauncher(
      String minecraftVersion, String fabricVersion, String destination) {
    String fileDestination = String.format("%s/fabric-server-launcher.jar", destination);

    if (VERSIONMETA.fabric().improvedLauncherUrl(minecraftVersion, fabricVersion).isPresent()
        && UTILITIES.WebUtils()
        .downloadFile(
            fileDestination,
            VERSIONMETA.fabric().improvedLauncherUrl(minecraftVersion, fabricVersion).get())) {

      LOG.info("Successfully provided improved Fabric Server Launcher.");

      try (BufferedWriter writer =
          new BufferedWriter(
              new FileWriter(
                  String.valueOf(
                      Paths.get(String.format("%s/SERVER_PACK_INFO.txt", destination)))))) {

        // Improved Fabric server launcher info
        writer.write(
            "If you are using this server pack on a managed server, meaning you can not execute scripts, please use the fabric-server-launcher.jar instead of the fabric-server-launch.jar. Note the extra \"er\" at the end of \"launcher\".\n");
        writer.write(
            "This is the improved Fabric Server Launcher, which will take care of downloading and installing the Minecraft server and any and all libraries needed for running the Fabric server.\n");
        writer.write("\n");
        writer.write(
            "The downside of this method is the occasional incompatibility of mods with the Fabric version, as the new Fabric Server Launcher always uses the latest available Fabric version.\n");
        writer.write(
            "If a mod is incompatible with said latest Fabric version, contact the mod-author and ask them to remedy the situation.\n");
        writer.write("The official Fabric Discord had the following to add to this:\n");
        writer.write(
            "    Fabric loader however is cross version, so unless there is a mod incompatibility (which usually involves the mod being broken / using non-api internals)\n");
        writer.write(
            "    there is no good reason to use anything but the latest. I.e. the latest loader on any Minecraft version works with the new server launcher.");

      } catch (Exception ex) {
        LOG.error(
            "Error downloading the improved Fabric server launcher. Maybe it doesn't exist for the specified Minecraft and Fabric version?",
            ex);
      }
    }
  }

  /**
   * Deletes all files, directories and ZIP-archives of previously generated server packs to ensure
   * newly generated server pack is as clean as possible.
   *
   * @param deleteZip          Whether to delete the server pack ZIP-archive.
   * @param configurationModel ConfigurationModel containing the modpack directory from which the
   *                           destination of the server pack is acquired.
   * @author Griefed
   */
  private void cleanupEnvironment(boolean deleteZip, ConfigurationModel configurationModel) {
    cleanupEnvironment(deleteZip, getServerPackDestination(configurationModel));
  }

  /**
   * Deletes all files, directories and ZIP-archives of previously generated server packs to ensure
   * newly generated server pack is as clean as possible.
   *
   * @param deleteZip   Whether to delete the server pack ZIP-archive.
   * @param destination The destination at which to clean up in.
   * @author Griefed
   */
  private void cleanupEnvironment(boolean deleteZip, String destination) {

    LOG.info("Found old server_pack. Cleaning up...");

    FileUtils.deleteQuietly(new File(destination));

    if (deleteZip) {

      LOG.info("Found old server_pack.zip. Cleaning up...");

      FileUtils.deleteQuietly(new File(String.format("%s_server_pack.zip", destination)));
    }
  }

  /**
   * Create start-scripts for the generated server pack.
   *
   * @param configurationModel Configuration model containing modpack specific values. keys to be
   *                           replaced with their respective values in the start scripts, as well
   *                           as the modpack directory from which the destination of the server
   *                           pack is acquired.
   * @author Griefed
   */
  public void createStartScripts(ConfigurationModel configurationModel) {
    createStartScripts(configurationModel.getScriptSettings(),
        getServerPackDestination(configurationModel));
  }

  /**
   * Create start-scripts for the generated server pack.
   *
   * @param scriptSettings Key-value pairs to replace in the script. A given key in the script is
   *                       replaced with its value.
   * @param destination    The destination where the scripts should be created in.
   * @author Griefed
   */
  public void createStartScripts(HashMap<String, String> scriptSettings, String destination) {
    for (File template : APPLICATIONPROPERTIES.scriptTemplates()) {

      try {
        String fileEnding = template.toString().substring(template.toString().lastIndexOf(".") + 1);
        File destinationScript = new File(destination + "/start." + fileEnding);

        String scriptContent = FileUtils.readFileToString(template, StandardCharsets.UTF_8);

        for (Map.Entry<String, String> entry : scriptSettings.entrySet()) {
          scriptContent = scriptContent.replace(entry.getKey(), entry.getValue());
        }

        FileUtils.writeStringToFile(destinationScript, scriptContent, StandardCharsets.ISO_8859_1);

      } catch (Exception ex) {
        LOG.error("File not accessible: " + template + ".", ex);
      }
    }
  }

  /**
   * Copies all specified directories and mods, excluding clientside-only mods, from the modpack
   * directory into the server pack directory. If a <code>source/file;destination/file</code>
   * -combination is provided, the specified source-file is copied to the specified
   * destination-file.
   *
   * @param configurationModel ConfigurationModel containing the modpack directory, list of
   *                           directories and files to copy, list of clientside-only mods to
   *                           exclude, the Minecraft version used by the modpack and server pack,
   *                           and the modloader used by the modpack and server pack.
   * @author Griefed
   */
  private void copyFiles(ConfigurationModel configurationModel) {
    copyFiles(configurationModel.getModpackDir(), configurationModel.getCopyDirs(),
        configurationModel.getClientMods(), configurationModel.getMinecraftVersion(),
        getServerPackDestination(configurationModel), configurationModel.getModLoader());
  }

  /**
   * Copies all specified directories and mods, excluding clientside-only mods, from the modpack
   * directory into the server pack directory. If a <code>source/file;destination/file</code>
   * -combination is provided, the specified source-file is copied to the specified
   * destination-file.
   *
   * @param modpackDir        Files and directories are copied into the server_pack directory inside
   *                          the modpack directory.
   * @param directoriesToCopy All directories and files therein to copy to the server pack.
   * @param clientMods        List of clientside-only mods to exclude from the server pack.
   * @param minecraftVersion  The Minecraft version the modpack uses.
   * @param destination       The destination where the files should be copied to.
   * @author Griefed
   */
  private void copyFiles(
      String modpackDir,
      List<String> directoriesToCopy,
      List<String> clientMods,
      String minecraftVersion,
      String destination,
      String modloader) {

    try {

      Files.createDirectories(Paths.get(destination));

    } catch (IOException ex) {

      LOG.error(String.format("Failed to create directory %s", destination));
    }

    if (directoriesToCopy.size() == 1 && directoriesToCopy.get(0).equals("lazy_mode")) {

      LOG.warn("!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!");
      LOG.warn(
          "Lazy mode specified. This will copy the WHOLE modpack to the server pack. No exceptions.");
      LOG.warn("You will not receive any support for a server pack generated this way.");
      LOG.warn(
          "Do not open an issue on GitHub if this configuration errors or results in a broken server pack.");
      LOG.warn("!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!");

      try {

        FileUtils.copyDirectory(new File(modpackDir), new File(destination));

      } catch (IOException ex) {
        LOG.error("An error occurred copying the modpack to the server pack in lazy mode.", ex);
      }

    } else {

      TreeSet<String> exclusions = new TreeSet<>(APPLICATIONPROPERTIES.getDirectoriesToExclude());

      directoriesToCopy.removeIf(exclude -> {

        if (exclude.startsWith("!")) {

          exclusions.add(exclude.substring(1).replace("\\", "/"));
          return true;

        } else {
          return false;
        }
      });

      List<ServerPackFile> serverPackFiles = new ArrayList<>(100000);

      for (String directory : directoriesToCopy) {

        String clientDir = String.format("%s/%s", modpackDir, directory).replace("\\", "/");
        String serverDir = String.format("%s/%s", destination, directory).replace("\\", "/");

        LOG.info("Gathering " + directory + " file(s) and folder(s).");

        if (directory.contains(";")) {

          /*
           * If a semicolon is found, it means a user specified a source/path/to_file.foo;destination/path/to_file.bar-combination
           * for a file they specifically want to include in their server pack.
           */
          serverPackFiles.addAll(getExplicitFiles(directory.split(";"), modpackDir, destination));

        } else if (directory.startsWith("saves/")) {

          /*
           * Check whether the entry starts with saves, and if it does, change the destination path to NOT include
           * saves in it, so when a world is specified inside the saves-directory, it is copied to the base-directory
           * of the server pack, instead of a saves-directory inside the modpack.
           */
          serverPackFiles.addAll(getSaveFiles(clientDir, directory, destination));

        } else if (directory.startsWith("mods")) {

          /*
           * If the entry starts with mods, we need to run our checks for clientside-only mods as well as exclude any
           * user-specified clientside-only mods from the list of mods in the mods-directory.
           */

          try {
            Files.createDirectories(Paths.get(serverDir));
          } catch (IOException ignored) {

          }

          for (File mod : getModsToInclude(clientDir, clientMods, minecraftVersion, modloader)) {

            serverPackFiles.add(
                new ServerPackFile(
                    mod.getAbsolutePath(), String.format("%s/%s", serverDir, mod.getName())));
          }

        } else if (new File(directory).isFile()) {

          serverPackFiles.add(
              new ServerPackFile(
                  directory, String.format("%s/%s", destination, new File(directory).getName())));

        } else if (new File(directory).isDirectory()) {

          serverPackFiles.addAll(getDirectoryFiles(directory, destination));

        } else {

          serverPackFiles.addAll(getDirectoryFiles(clientDir, destination));
        }
      }

      LOG.info("Ensuring files and/or directories are properly excluded.");

      serverPackFiles.removeIf(
          serverPackFile -> {
            if (excludeFileOrDirectory(
                serverPackFile.SOURCE_PATH.toString().replace("\\", "/"), exclusions)) {
              LOG.debug("Excluding file/directory: " + serverPackFile.SOURCE_PATH);
              return true;
            } else {
              return false;
            }
          });

      LOG.info("Copying files to the server pack. This may take a while...");

      serverPackFiles.forEach(
          serverPackFile -> {
            try {
              serverPackFile.copy();
            } catch (IOException ex) {
              LOG.error(
                  "An error occurred trying to copy "
                      + serverPackFile.source()
                      + " to "
                      + serverPackFile.destination()
                      + ".",
                  ex);
            }
          });
    }
  }

  /**
   * Gather a list of all files from an explicit source;destination-combination. If the source is a
   * file, a singular {@link ServerPackFile} is returned. If the source is a directory, then
   * {@link ServerPackFile}s for all files in said directory are returned.
   *
   * @param combination        Array containing a source-file/directory;destination-file/directory
   *                           combination.
   * @param configurationModel ConfigurationModel containing the modpack directory so the
   *                           destination of the server pack can be acquired.
   * @return List of {@link ServerPackFile}.
   * @author Griefed
   */
  private List<ServerPackFile> getExplicitFiles(String[] combination,
      ConfigurationModel configurationModel) {
    return getExplicitFiles(combination, configurationModel.getModpackDir(),
        getServerPackDestination(configurationModel));
  }

  /**
   * Gather a list of all files from an explicit source;destination-combination. If the source is a
   * file, a singular {@link ServerPackFile} is returned. If the source is a directory, then
   * {@link ServerPackFile}s for all files in said directory are returned.
   *
   * @param combination Array containing a source-file/directory;destination-file/directory
   *                    combination.
   * @param modpackDir  The modpack-directory.
   * @param destination The destination, normally the server pack-directory.
   * @return List of {@link ServerPackFile}.
   * @author Griefed
   */
  private List<ServerPackFile> getExplicitFiles(
      String[] combination, String modpackDir, String destination) {
    List<ServerPackFile> serverPackFiles = new ArrayList<>();

    if (new File(String.format("%s/%s", modpackDir, combination[0])).isFile()) {

      serverPackFiles.add(
          new ServerPackFile(
              String.format("%s/%s", modpackDir, combination[0]),
              String.format("%s/%s", destination, combination[1])));

    } else if (new File(String.format("%s/%s", modpackDir, combination[0])).isDirectory()) {

      serverPackFiles.addAll(
          getDirectoryFiles(String.format("%s/%s", modpackDir, combination[0]), destination));

    } else if (new File(combination[0]).isFile()) {

      serverPackFiles.add(
          new ServerPackFile(combination[0], String.format("%s/%s", destination, combination[1])));

    } else if (new File(combination[0]).isDirectory()) {

      serverPackFiles.addAll(getDirectoryFiles(combination[0], destination));
    }

    return serverPackFiles;
  }

  /**
   * Gather {@link ServerPackFile}s for a given directory, recursively.
   *
   * @param source      The source-directory.
   * @param destination The server pack-directory.
   * @return List of files and folders of the server pack.
   * @author Griefed
   */
  private List<ServerPackFile> getDirectoryFiles(String source, String destination) {
    List<ServerPackFile> serverPackFiles = new ArrayList<>();
    try (Stream<Path> files = Files.walk(Paths.get(source))) {

      files.forEach(
          file -> {
            try {

              serverPackFiles.add(
                  new ServerPackFile(
                      file,
                      Paths.get(String.format("%s/%s", destination, new File(source).getName()))
                          .resolve(Paths.get(source).relativize(file))));
            } catch (UnsupportedOperationException ex) {

              LOG.error("Couldn't gather file " + file + " from directory " + source + ".", ex);
            }
          });

    } catch (IOException ex) {

      LOG.error("An error occurred gathering files to copy to the server pack.", ex);
    }

    return serverPackFiles;
  }

  /**
   * Gather all files in the specified save-directory and create {@link ServerPackFile}s from it.
   *
   * @param clientDir   Target directory in the server pack. Usually the name of the world.
   * @param directory   The save-directory.
   * @param destination The destination of the server pack.
   * @return List of {@link ServerPackFile}.
   * @author Griefed
   */
  private List<ServerPackFile> getSaveFiles(
      String clientDir, String directory, String destination) {
    List<ServerPackFile> serverPackFiles = new ArrayList<>();

    try (Stream<Path> files = Files.walk(Paths.get(clientDir))) {

      files.forEach(
          file -> {
            try {

              serverPackFiles.add(
                  new ServerPackFile(
                      file,
                      Paths.get(String.format("%s/%s", destination, directory.substring(6)))
                          .resolve(Paths.get(clientDir).relativize(file))));
            } catch (UnsupportedOperationException ex) {
              LOG.error("Couldn't gather file " + file + " from directory " + clientDir + ".", ex);
            }
          });

    } catch (IOException ex) {

      LOG.error("An error occurred during the copy-procedure to the server pack.", ex);
    }

    return serverPackFiles;
  }

  /**
   * Generates a list of all mods to include in the server pack. If the user specified
   * clientside-mods to exclude, and/or if the automatic exclusion of clientside-only mods is
   * active, they will be excluded, too.
   *
   * @param configurationModel The configurationModel containing the modpack directory, list of
   *                           clientside-only mods to exclude, Minecraft version used by the
   *                           modpack and server pack and the modloader used by the modpack and
   *                           server pack.
   * @return A list of all mods to include in the server pack.
   * @author Griefed
   */
  public List<File> getModsToInclude(ConfigurationModel configurationModel) {
    return getModsToInclude(configurationModel.getModpackDir() + "/mods",
        configurationModel.getClientMods(), configurationModel.getMinecraftVersion(),
        configurationModel.getModLoader());
  }

  /**
   * Generates a list of all mods to include in the server pack. If the user specified
   * clientside-mods to exclude, and/or if the automatic exclusion of clientside-only mods is
   * active, they will be excluded, too.
   *
   * @param modsDir                 String. The mods-directory of the modpack of which to generate a
   *                                list of all its contents.
   * @param userSpecifiedClientMods List String. A list of all clientside-only mods.
   * @param minecraftVersion        String. The Minecraft version the modpack uses. When the
   *                                modloader is Forge, this determines whether Annotations or Tomls
   *                                are scanned.
   * @param modloader               {@link String} The modloader the modpack uses.
   * @return A list of all mods to include in the server pack.
   * @author Griefed
   */
  public List<File> getModsToInclude(
      String modsDir,
      List<String> userSpecifiedClientMods,
      String minecraftVersion,
      String modloader) {

    LOG.info("Preparing a list of mods to include in server pack...");

    Collection<File> filesInModsDir =
        new ArrayList<>(FileUtils.listFiles(new File(modsDir), MOD_FILE_ENDINGS, true));
    TreeSet<File> modsInModpack = new TreeSet<>(filesInModsDir);
    List<File> autodiscoveredClientMods = new ArrayList<>();

    // Check whether scanning mods for sideness is activated.
    if (APPLICATIONPROPERTIES.isAutoExcludingModsEnabled()) {

      STOPWATCH_SCANS.start();
      // If Minecraft version is 1.12 or newer, scan Tomls, else scan annotations.

      switch (modloader) {
        case "LegacyFabric":
        case "Fabric":
          autodiscoveredClientMods.addAll(MODSCANNER.fabric().scan(filesInModsDir));
          break;

        case "Forge":
          if (Integer.parseInt(minecraftVersion.split("\\.")[1]) > 12) {
            autodiscoveredClientMods.addAll(MODSCANNER.tomls().scan(filesInModsDir));
          } else {
            autodiscoveredClientMods.addAll(MODSCANNER.annotations().scan(filesInModsDir));
          }
          break;

        case "Quilt":
          TreeSet<File> discoMods = new TreeSet<>();
          discoMods.addAll(MODSCANNER.fabric().scan(filesInModsDir));
          discoMods.addAll(MODSCANNER.quilt().scan(filesInModsDir));
          autodiscoveredClientMods.addAll(discoMods);
          discoMods.clear();
      }

      // Exclude scanned mods from copying if said functionality is enabled.
      excludeMods(autodiscoveredClientMods, modsInModpack);

      STOPWATCH_SCANS.stop();
      LOG.debug(
          "Scanning and excluding of " + filesInModsDir.size() + " mods took " + STOPWATCH_SCANS);
      STOPWATCH_SCANS.reset();

    } else {
      LOG.info("Automatic clientside-only mod detection disabled.");
    }

    // Exclude user-specified mods from copying.
    excludeUserSpecifiedMod(userSpecifiedClientMods, modsInModpack);

    return new ArrayList<>(modsInModpack);
  }

  /**
   * Exclude every automatically discovered clientside-only mod from the list of mods in the
   * modpack.
   *
   * @param autodiscoveredClientMods Automatically discovered clientside-only mods in the modpack.
   * @param modsInModpack            All mods in the modpack.
   * @author Griefed
   */
  private void excludeMods(List<File> autodiscoveredClientMods,
      TreeSet<File> modsInModpack) {

    if (autodiscoveredClientMods.size() > 0) {

      LOG.info("Automatically detected mods: " + autodiscoveredClientMods.size());

      for (File discoveredMod : autodiscoveredClientMods) {
        modsInModpack.removeIf(
            mod -> {
              if (mod.getName().contains(discoveredMod.getName())) {
                LOG.warn("Automatically excluding mod: " + discoveredMod.getName());
                return true;
              } else {
                return false;
              }
            });
      }
    } else {
      LOG.info("No clientside-only mods detected.");
    }
  }

  /**
   * Exclude user-specified mods from the server pack.
   *
   * @param userSpecifiedExclusions User-specified clientside-only mods to exclude from the server
   *                                pack.
   * @param modsInModpack           Every mod ending with <code>jar</code> or <code>disabled</code>
   *                                in the modpack.
   * @author Griefed
   */
  private void excludeUserSpecifiedMod(List<String> userSpecifiedExclusions,
      TreeSet<File> modsInModpack) {

    if (userSpecifiedExclusions.size() > 0) {

      LOG.info("Performing " + APPLICATIONPROPERTIES.exclusionFilter()
          + "-type checks for user-specified clientside-only mod exclusion.");
      for (String userSpecifiedExclusion : userSpecifiedExclusions) {
        exclude(userSpecifiedExclusion, modsInModpack);
      }

    } else {
      LOG.warn("User specified no clientside-only mods.");
    }
  }

  /**
   * Go through the mods in the modpack and exclude any of the user-specified clientside-only mods
   * according to the filter method set in the serverpackcreator.properties.
   *
   * @param userSpecifiedExclusion The client mod to check whether it needs to be excluded.
   * @param modsInModpack          All mods in the modpack.
   */
  private void exclude(String userSpecifiedExclusion, TreeSet<File> modsInModpack) {
    modsInModpack.removeIf(
        mod -> {
          boolean excluded;
          String check = mod.getName();

          switch (APPLICATIONPROPERTIES.exclusionFilter()) {

            case END:
              excluded = check.endsWith(userSpecifiedExclusion);
              break;

            case CONTAIN:
              excluded = check.contains(userSpecifiedExclusion);
              break;

            case REGEX:
              excluded = check.matches(userSpecifiedExclusion);
              break;

            case EITHER:
              excluded = check.startsWith(userSpecifiedExclusion) || check.endsWith(
                  userSpecifiedExclusion)
                  || check.contains(userSpecifiedExclusion) || check.matches(
                  userSpecifiedExclusion);
              break;

            case START:
            default:
              excluded = check.startsWith(userSpecifiedExclusion);
          }

          if (excluded) {
            LOG.debug(
                "Removed " + mod.getName() + " as per user-specified check: "
                    + userSpecifiedExclusion);
          }
          return excluded;
        });
  }

  /**
   * Check whether the given file is present in the list of directories to exclude from the server
   * pack.
   *
   * @param fileToCheckFor The string to check for.
   * @return Boolean. Returns true if the file is found in the list of directories to exclude, false
   * if not.
   * @author Griefed
   */
  private boolean excludeFileOrDirectory(String fileToCheckFor, TreeSet<String> exclusions) {
    boolean isPresentInList = false;
    for (String entry : exclusions) {
      if (fileToCheckFor.replace("\\", "/").contains(entry)) {
        isPresentInList = true;
        break;
      }
    }
    return isPresentInList;
  }

  /**
   * Copies the server-icon.png into server_pack.
   *
   * @param configurationModel Containing the modpack directory to acquire the destination of the
   *                           server pack and the path to the server icon to copy.
   * @author Griefed
   */
  private void copyIcon(ConfigurationModel configurationModel) {
    copyIcon(getServerPackDestination(configurationModel), configurationModel.getServerIconPath());
  }

  /**
   * Copies the server-icon.png into server_pack.
   *
   * @param destination      The destination where the icon should be copied to.
   * @param pathToServerIcon The path to the custom server-icon.
   * @author Griefed
   */
  private void copyIcon(String destination, String pathToServerIcon) {

    LOG.info("Copying server-icon.png...");

    File iconFile =
        new File(String.format("%s/%s", destination, APPLICATIONPROPERTIES.DEFAULT_SERVER_ICON()));

    if (new File(pathToServerIcon).exists()) {

      BufferedImage originalImage;
      //noinspection UnusedAssignment
      Image scaledImage = null;

      try {

        originalImage = ImageIO.read(new File(pathToServerIcon));

        if (originalImage.getHeight() == 64 && originalImage.getWidth() == 64) {

          try {

            FileUtils.copyFile(new File(pathToServerIcon), iconFile);

          } catch (IOException e) {
            LOG.error("An error occurred trying to copy the server-icon.", e);
          }

        } else {

          // Scale our image to 64x64
          scaledImage = originalImage.getScaledInstance(64, 64, Image.SCALE_SMOOTH);
          BufferedImage outputImage =
              new BufferedImage(
                  scaledImage.getWidth(null),
                  scaledImage.getHeight(null),
                  BufferedImage.TYPE_INT_ARGB);
          outputImage.getGraphics().drawImage(scaledImage, 0, 0, null);

          // Save our scaled image to disk.
          try {
            ImageIO.write(outputImage, "png", iconFile);

          } catch (IOException ex) {

            LOG.error("Error scaling image.", ex);
          }
        }

      } catch (Exception ex) {
        LOG.error("Error reading server-icon image.", ex);
      }

    } else if (pathToServerIcon.length() == 0) {

      LOG.info("No custom icon specified or the file doesn't exist.");

      try {

        FileUtils.copyFile(
            new File(String.format("server_files/%s", APPLICATIONPROPERTIES.DEFAULT_SERVER_ICON())),
            iconFile);

      } catch (IOException ex) {
        LOG.error("An error occurred trying to copy the server-icon.", ex);
      }

    } else {

      LOG.error("The specified server-icon does not exist: " + pathToServerIcon);
    }
  }

  /**
   * Copies the server.properties into server_pack.
   *
   * @param configurationModel Containing the modpack directory to acquire the destination of the
   *                           server pack and the path to the server properties to copy.
   * @author Griefed
   */
  private void copyProperties(ConfigurationModel configurationModel) {
    copyProperties(getServerPackDestination(configurationModel),
        configurationModel.getServerPropertiesPath());
  }

  /**
   * Copies the server.properties into server_pack.
   *
   * @param destination            The destination where the properties should be copied to.
   * @param pathToServerProperties The path to the custom server.properties.
   * @author Griefed
   */
  private void copyProperties(String destination, String pathToServerProperties) {

    LOG.info("Copying server.properties...");

    File defaultProperties =
        new File(
            String.format("%s/%s", destination, APPLICATIONPROPERTIES.DEFAULT_SERVER_PROPERTIES()));

    if (new File(pathToServerProperties).exists()) {
      try {

        FileUtils.copyFile(new File(pathToServerProperties), defaultProperties);

      } catch (IOException ex) {
        LOG.error("An error occurred trying to copy the server.properties-file.", ex);
      }

    } else if (pathToServerProperties.length() == 0) {

      LOG.info("No custom properties specified or the file doesn't exist.");

      try {

        FileUtils.copyFile(
            new File(
                String.format(
                    "server_files/%s", APPLICATIONPROPERTIES.DEFAULT_SERVER_PROPERTIES())),
            defaultProperties);

      } catch (IOException ex) {
        LOG.error("An error occurred trying to copy the server.properties-file.", ex);
      }

    } else {

      LOG.error("The specified server.properties does not exist: " + pathToServerProperties);
    }
  }

  /**
   * Installs the modloader server for the specified modloader, modloader version and Minecraft
   * version.
   *
   * @param configurationModel Contains the used modloader, Minecraft version, modloader version,
   *                           path to the java executable/binary and modpack directory in order to
   *                           acquire the destination at which to install the server.
   * @author Griefed
   */
  public void installServer(ConfigurationModel configurationModel) {
    installServer(configurationModel.getModLoader(), configurationModel.getMinecraftVersion(),
        configurationModel.getModLoaderVersion(),
        configurationModel.getJavaPath(), getServerPackDestination(configurationModel));
  }

  /**
   * Installs the modloader server for the specified modloader, modloader version and Minecraft
   * version.
   *
   * @param modLoader        The modloader for which to install the server software. Either Forge or
   *                         Fabric.
   * @param minecraftVersion The Minecraft version for which to install the modloader and Minecraft
   *                         server.
   * @param modLoaderVersion The modloader version for which to install the modloader and Minecraft
   *                         server.
   * @param javaPath         The path to the Java executable/binary which is needed to execute the
   *                         Forge/Fabric installersList.
   * @param destination      The destination where the modloader server should be installed into.
   * @author Griefed
   */
  public void installServer(
      String modLoader,
      String minecraftVersion,
      String modLoaderVersion,
      String javaPath,
      String destination) {

    List<String> commandArguments = new ArrayList<>();

    Process process = null;
    BufferedReader bufferedReader = null;

    switch (modLoader) {
      case "Fabric":
        LOG_INSTALLER.info("Starting Fabric installation.");

        if (UTILITIES.WebUtils()
            .downloadFile(String.format("%s/fabric-installer.jar", destination),
                VERSIONMETA.fabric().releaseInstallerUrl())) {

          LOG.info("Fabric installer successfully downloaded.");

          commandArguments.add(javaPath);
          commandArguments.add("-jar");
          commandArguments.add("fabric-installer.jar");
          commandArguments.add("server");
          commandArguments.add("-mcversion");
          commandArguments.add(minecraftVersion);
          commandArguments.add("-loader");
          commandArguments.add(modLoaderVersion);
          commandArguments.add("-downloadMinecraft");

        } else {

          LOG.error(
              "Something went wrong during the installation of Fabric. Maybe the Fabric servers are down or unreachable? Skipping...");
        }
        break;

      case "Forge":
        LOG_INSTALLER.info("Starting Forge installation.");

        if (VERSIONMETA.forge().getForgeInstance(minecraftVersion, modLoaderVersion).isPresent()
            && UTILITIES.WebUtils()
            .downloadFile(
                String.format("%s/forge-installer.jar", destination),
                VERSIONMETA
                    .forge()
                    .getForgeInstance(minecraftVersion, modLoaderVersion)
                    .get()
                    .installerUrl())) {

          LOG.info("Forge installer successfully downloaded.");

          commandArguments.add(javaPath);
          commandArguments.add("-jar");
          commandArguments.add("forge-installer.jar");
          commandArguments.add("--installServer");

        } else {

          LOG.error(
              "Something went wrong during the installation of Forge. Maybe the Forge servers are down or unreachable? Skipping...");
        }
        break;

      case "Quilt":
        LOG_INSTALLER.info("Starting Quilt installation.");

        if (UTILITIES.WebUtils()
            .downloadFile(String.format("%s/quilt-installer.jar", destination),
                VERSIONMETA.quilt().releaseInstallerUrl())) {

          LOG.info("Quilt installer successfully downloaded.");

          commandArguments.add(javaPath);
          commandArguments.add("-jar");
          commandArguments.add("quilt-installer.jar");
          commandArguments.add("install");
          commandArguments.add("server");
          commandArguments.add(minecraftVersion);
          commandArguments.add("--download-server");
          commandArguments.add("--install-dir=.");

        } else {

          LOG.error(
              "Something went wrong during the installation of Quilt. Maybe the Quilt servers are down or unreachable? Skipping...");
        }
        break;

      case "LegacyFabric":
        LOG_INSTALLER.info("Starting Legacy Fabric installation.");

        try {
          if (UTILITIES.WebUtils()
              .downloadFile(String.format("%s/legacyfabric-installer.jar", destination),
                  VERSIONMETA.legacyFabric().releaseInstallerUrl())) {

            LOG.info("LegacyFabric installer successfully downloaded.");

            commandArguments.add(javaPath);
            commandArguments.add("-jar");
            commandArguments.add("legacyfabric-installer.jar");
            commandArguments.add("server");
            commandArguments.add("-mcversion");
            commandArguments.add(minecraftVersion);
            commandArguments.add("-loader");
            commandArguments.add(modLoaderVersion);
            commandArguments.add("-downloadMinecraft");

          } else {

            LOG.error(
                "Something went wrong during the installation of LegacyFabric. Maybe the LegacyFabric servers are down or unreachable? Skipping...");
          }
        } catch (MalformedURLException ex) {
          LOG.error("Couldn't acquire LegacyFabric installer URL.", ex);
        }
        break;

      default:
        LOG.error(
            "Invalid modloader specified. Modloader must be either Forge, Fabric or Quilt. Specified: "
                + modLoader);
    }

    try {
      LOG.info(
          "Starting server installation for Minecraft "
              + minecraftVersion
              + ", "
              + modLoader
              + " "
              + modLoaderVersion
              + ".");

      ProcessBuilder processBuilder =
          new ProcessBuilder(commandArguments).directory(new File(destination));

      LOG.debug("ProcessBuilder command: " + processBuilder.command());

      processBuilder.redirectErrorStream(true);
      process = processBuilder.start();

      bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
      String line;

      while (true) {
        line = bufferedReader.readLine();
        if (line == null) {
          break;
        }
        LOG_INSTALLER.info(line);
      }

      LOG_INSTALLER.info(
          "Server for Minecraft "
              + minecraftVersion
              + ", "
              + modLoader
              + " "
              + modLoaderVersion
              + " installed.");

      LOG.info(
          "Server for Minecraft "
              + minecraftVersion
              + ", "
              + modLoader
              + " "
              + modLoaderVersion
              + " installed.");

      LOG.info(
          "For details regarding the installation of this modloader server, see logs/modloader_installer.log.");

    } catch (IOException ex) {

      LOG.error(
          "Something went wrong during the installation of Forge. Maybe the Forge servers are down or unreachable? Skipping...",
          ex);

    } finally {

      try {
        //noinspection ConstantConditions
        bufferedReader.close();
      } catch (Exception ignored) {

      }

      try {
        //noinspection ConstantConditions
        process.destroy();
      } catch (Exception ignored) {

      }

      commandArguments.clear();
    }

    if (APPLICATIONPROPERTIES.isServerPackCleanupEnabled()) {
      cleanUpServerPack(minecraftVersion, modLoaderVersion, destination);
    } else {
      LOG.info("Server pack cleanup disabled.");
    }
  }

  /**
   * Creates a ZIP-archive of the server pack previously generated. Depending on
   * {@link ApplicationProperties#isZipFileExclusionEnabled()}, files will be excluded. To customize
   * the files which will be excluded, see
   * {@link ApplicationProperties#getFilesToExcludeFromZipArchive()}
   *
   * @param configurationModel Contains the Minecraft version used by the modpack and server pack,
   *                           whether the modloader server was installed, the modpack directory to
   *                           acquire the destination of the server pack, the modloader used by the
   *                           modpack and server pack and the modloader version.
   * @author Griefed
   */
  public void zipBuilder(ConfigurationModel configurationModel) {
    zipBuilder(configurationModel.getMinecraftVersion(),
        configurationModel.getIncludeServerInstallation(),
        getServerPackDestination(configurationModel),
        configurationModel.getModLoader(), configurationModel.getModLoaderVersion());
  }

  /**
   * Creates a ZIP-archive of the server pack previously generated. Depending on
   * {@link ApplicationProperties#isZipFileExclusionEnabled()}, files will be excluded. To customize
   * the files which will be excluded, see
   * {@link ApplicationProperties#getFilesToExcludeFromZipArchive()}
   *
   * @param minecraftVersion          Determines the name of the Minecraft server JAR to exclude
   *                                  from the ZIP-archive if the modloader is Forge.
   * @param includeServerInstallation Determines whether the Minecraft server JAR info should be
   *                                  printed.
   * @param destination               The destination where the ZIP-archive should be created in.
   * @param modloader                 The modloader the modpack and server pack use.
   * @param modloaderVersion          The modloader version the modpack and server pack use.
   * @author Griefed
   */
  public void zipBuilder(
      String minecraftVersion,
      boolean includeServerInstallation,
      String destination,
      String modloader,
      String modloaderVersion) {

    LOG.info("Creating zip archive of serverpack...");

    ZipParameters zipParameters = new ZipParameters();

    List<File> filesToExclude = new ArrayList<>(100);
    if (APPLICATIONPROPERTIES.isZipFileExclusionEnabled()) {

      APPLICATIONPROPERTIES
          .getFilesToExcludeFromZipArchive()
          .forEach(
              entry ->
                  filesToExclude.add(
                      new File(
                          destination
                              + "/"
                              + entry
                              .replace("MINECRAFT_VERSION", minecraftVersion)
                              .replace("MODLOADER", modloader)
                              .replace("MODLOADER_VERSION", modloaderVersion))));

      ExcludeFileFilter excludeFileFilter = filesToExclude::contains;
      zipParameters.setExcludeFileFilter(excludeFileFilter);

    } else {

      LOG.info("File exclusion from ZIP-archives deactivated.");
    }

    String comment =
        "Server pack made with ServerPackCreator "
            + APPLICATIONPROPERTIES.SERVERPACKCREATOR_VERSION()
            + " by Griefed.";

    zipParameters.setIncludeRootFolder(false);
    zipParameters.setFileComment(comment);

    try (ZipFile zip = new ZipFile(String.format("%s_server_pack.zip", destination))) {

      zip.addFolder(new File(destination), zipParameters);
      zip.setComment(comment);

    } catch (IOException ex) {

      LOG.error("There was an error during zip creation.", ex);
    }

    if (includeServerInstallation) {

      LOG.warn(
          "!!!-------NOTE: The minecraft_server.jar will not be included in the zip-archive.-------!!!");
      LOG.warn(
          "!!!-Mojang strictly prohibits the distribution of their software through third parties.-!!!");
      LOG.warn(
          "!!!---Tell your users to execute the download scripts to get the Minecraft server jar.--!!!");
    }

    LOG.info("Finished creation of zip archive.");
  }

  /**
   * Cleans up the server_pack directory by deleting left-over files from modloader installations
   * and version checking.
   *
   * @param configurationModel Containing the Minecraft version used by the modpack and server pack,
   *                           the modloader version used by the modpack and server pack and the
   *                           modpack directory to acquire the destination of the server pack.
   * @author Griefed
   */
  private void cleanUpServerPack(ConfigurationModel configurationModel) {
    cleanUpServerPack(configurationModel.getMinecraftVersion(),
        configurationModel.getModLoaderVersion(), getServerPackDestination(configurationModel));
  }

  /**
   * Cleans up the server_pack directory by deleting left-over files from modloader installations
   * and version checking.
   *
   * @param minecraftVersion Needed for renaming the Forge server JAR to work with launch scripts
   *                         provided by ServerPackCreator.
   * @param modLoaderVersion Needed for renaming the Forge server JAR to work with launch scripts
   *                         provided by ServerPackCreator.
   * @param destination      The destination where we should clean up in.
   * @author Griefed
   */
  private void cleanUpServerPack(
      String minecraftVersion, String modLoaderVersion, String destination) {

    LOG.info("Cleanup after modloader server installation.");

    FileUtils.deleteQuietly(new File(destination + "/fabric-installer.jar"));
    FileUtils.deleteQuietly(new File(destination + "/forge-installer.jar"));
    FileUtils.deleteQuietly(new File(destination + "/quilt-installer.jar"));
    FileUtils.deleteQuietly(new File(destination + "/installer.log"));
    FileUtils.deleteQuietly(new File(destination + "/forge-installer.jar.log"));
    FileUtils.deleteQuietly(Paths.get(destination + "/run.bat").toFile());
    FileUtils.deleteQuietly(Paths.get(destination + "/run.sh").toFile());
    FileUtils.deleteQuietly(Paths.get(destination + "/user_jvm_args.txt").toFile());

    try {
      Path path =
          Paths.get(
              String.format(destination + "/forge-%s-%s.jar", minecraftVersion, modLoaderVersion));

      if (new File(
          String.format(destination + "/forge-%s-%s.jar", minecraftVersion, modLoaderVersion))
          .exists()) {

        Files.copy(path, Paths.get(destination + "/forge.jar"), REPLACE_EXISTING);
        FileUtils.deleteQuietly(path.toFile());
      }

    } catch (IOException ignored) {

    }
  }

  /**
   * A ServerPackFile represents a source-destination-combination of two files/directories. The
   * source is the file/directory, usually in the modpack, whilst the destination is the file to
   * which the source is supposed to be copied to in the server pack.
   *
   * @author Griefed
   */
  @SuppressWarnings("InnerClassMayBeStatic")
  private class ServerPackFile {

    private final File SOURCE_FILE;
    private final Path SOURCE_PATH;
    private final File DESTINATION_FILE;
    private final Path DESTINATION_PATH;

    /**
     * Construct a new ServerPackFile from two {@link File}-objects, a source and a destination.
     *
     * @param sourceFile      The source file/directory. Usually a file/directory in a modpack.
     * @param destinationFile The destination file/directory in the server pack.
     * @author Griefed
     */
    public ServerPackFile(File sourceFile, File destinationFile) throws InvalidPathException {
      this.SOURCE_FILE = sourceFile;
      this.SOURCE_PATH = sourceFile.toPath();
      this.DESTINATION_FILE = destinationFile;
      this.DESTINATION_PATH = destinationFile.toPath();
    }

    /**
     * Construct a new ServerPackFile from two {@link String}-objects, a source and a destination.
     *
     * @param sourceFile      The source file/directory. Usually a file/directory in a modpack.
     * @param destinationFile The destination file/directory in the server pack.
     * @author Griefed
     */
    public ServerPackFile(String sourceFile, String destinationFile)
        throws NullPointerException, InvalidPathException {
      this.SOURCE_FILE = new File(sourceFile);
      this.SOURCE_PATH = SOURCE_FILE.toPath();
      this.DESTINATION_FILE = new File(destinationFile);
      this.DESTINATION_PATH = DESTINATION_FILE.toPath();
    }

    /**
     * Construct a new ServerPackFile from two {@link Path}-objects, a source and a destination.
     *
     * @param sourcePath      The source file/directory. Usually a file/directory in a modpack.
     * @param destinationPath The destination file/directory in the server pack.
     * @author Griefed
     */
    public ServerPackFile(Path sourcePath, Path destinationPath)
        throws UnsupportedOperationException {
      this.SOURCE_FILE = sourcePath.toFile();
      this.SOURCE_PATH = sourcePath;
      this.DESTINATION_FILE = destinationPath.toFile();
      this.DESTINATION_PATH = destinationPath;
    }

    /**
     * The source-file.
     *
     * @return The source-file.
     * @author Griefed
     */
    public File source() {
      return SOURCE_FILE;
    }

    /**
     * The destination-file.
     *
     * @return The destination-file.
     * @author Griefed
     */
    public File destination() {
      return DESTINATION_FILE;
    }

    /**
     * The path to the source-file.
     *
     * @return The path to the source-file.
     * @author Griefed
     */
    public Path sourcePath() {
      return SOURCE_PATH;
    }

    /**
     * The path to the destination-file.
     *
     * @return The path to the destination-file.
     * @author Griefed
     */
    public Path destinationPath() {
      return DESTINATION_PATH;
    }

    /**
     * Copy this ServerPackFiles source to the destination. Already existing files are replaced.
     * When the source-file is a directory, then the destination-directory is created as an empty
     * directory. Any contents in the source-directory are NOT copied over to the
     * destination-directory. See {@link Files#copy(Path, Path, CopyOption...)} for an example on
     * how to copy entire directories, or use {@link FileUtils#copyDirectory(File, File)}.<br>
     * <br>
     * This method specifically does NOT copy recursively, because we would potentially copy
     * previously EXCLUDED files, too. We do not want that. At all.
     *
     * @throws SecurityException             In the case of the default provider, and a security
     *                                       manager is installed, the
     *                                       {@link SecurityManager#checkRead(String) checkRead}
     *                                       method is invoked to check read access to the source
     *                                       file, the
     *                                       {@link SecurityManager#checkWrite(String) checkWrite}
     *                                       is invoked to check write access to the target file. If
     *                                       a symbolic link is copied the security manager is
     *                                       invoked to check
     *                                       {@link LinkPermission}{@code ("symbolic")}.
     * @throws UnsupportedOperationException if the array contains a copy option that is not
     *                                       supported.
     * @throws IOException                   if an I/O error occurs
     * @author Griefed
     */
    public void copy() throws SecurityException, UnsupportedOperationException, IOException {
      try {
        if (!SOURCE_FILE.isDirectory()) {

          FileUtils.copyFile(
              SOURCE_FILE, DESTINATION_FILE, true, StandardCopyOption.REPLACE_EXISTING);

        } else {

          Files.copy(SOURCE_PATH, DESTINATION_PATH, REPLACE_EXISTING, COPY_ATTRIBUTES);
        }

        LOG.debug("Successfully copied ServerPackFile");
        LOG.debug("    Source: " + SOURCE_PATH);
        LOG.debug("    Destination: " + DESTINATION_PATH);

      } catch (DirectoryNotEmptyException ignored) {
        // If the directory to be copied already exists we're good.
      }
    }

    /**
     * This ServerPackFiles source-file and destination-file as a {@link String}-combination,
     * separated by a <code>;</code>
     *
     * @return This ServerPackFiles source-file and destination-file as a
     * {@link String}-combination, separated by a <code>;</code>
     * @author Griefed
     */
    @Override
    public String toString() {
      return SOURCE_PATH + ";" + DESTINATION_PATH;
    }
  }
}
