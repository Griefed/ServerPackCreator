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

import de.griefed.serverpackcreator.modscanning.ModScanner;
import de.griefed.serverpackcreator.spring.serverpack.ServerPackModel;
import de.griefed.serverpackcreator.utilities.SimpleStopWatch;
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
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ExcludeFileFilter;
import net.lingala.zip4j.model.ZipParameters;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Everything revolving around creating a server pack. The intended workflow is to create a
 * {@link ConfigurationModel} and run it through any of the available
 * {@link ConfigurationHandler#checkConfiguration(ConfigurationModel, boolean)}-variants, and then
 * call {@link #run(ConfigurationModel)} with the previously checked configuration model. You may
 * run with an unchecked configuration model, but no guarantees or promises, yes not even support,
 * is given for running a model without checking it first.<br> This class also gives you access to
 * the methods which are responsible for creating the server pack, in case you want to do things
 * manually.<br>The methods in question are:<br>
 * <ul>
 *   <li>{@link #cleanupEnvironment(boolean, ConfigurationModel)} and {@link #cleanupEnvironment(boolean, String)} </li>
 *   <li>{@link ApplicationAddons#runPreZipExtensions(ConfigurationModel, String)}</li>
 *   <li>{@link #copyFiles(ConfigurationModel)} and {@link #copyFiles(String, List, List, String, String, String)}</li>
 *   <li>{@link #provideImprovedFabricServerLauncher(ConfigurationModel)} and {@link #provideImprovedFabricServerLauncher(String, String, String)} if Fabric is the chosen Modloader</li>
 *   <li>{@link #copyIcon(ConfigurationModel)} and {@link #copyIcon(String, String)}</li>
 *   <li>{@link #copyProperties(ConfigurationModel)} and {@link #copyProperties(String, String)}</li>
 *   <li>{@link ApplicationAddons#runPreZipExtensions(ConfigurationModel, String)}</li>
 *   <li>{@link #zipBuilder(ConfigurationModel)} and {@link #zipBuilder(String, boolean, String, String, String)}</li>
 *   <li>{@link #createStartScripts(ConfigurationModel, boolean)} and {@link #createStartScripts(HashMap, String, boolean)}</li>
 *   <li>{@link #installServer(ConfigurationModel)} and {@link #installServer(String, String, String, String)}</li>
 *   <li>{@link ApplicationAddons#runPostGenExtensions(ConfigurationModel, String)}</li>
 * </ul>
 * <br> If you want to execute extensions, see
 * {@link ApplicationAddons#runPreGenExtensions(ConfigurationModel, String)}},
 * {@link ApplicationAddons#runPreZipExtensions(ConfigurationModel, String)}} and
 * {@link ApplicationAddons#runPostGenExtensions(ConfigurationModel, String)}.
 *
 * @author Griefed
 */
@SuppressWarnings("unused")
@Component
public final class ServerPackHandler {

  private static final Logger LOG = LogManager.getLogger(ServerPackHandler.class);
  private static final Logger LOG_INSTALLER = LogManager.getLogger("InstallerLogger");

  private final VersionMeta VERSIONMETA;
  private final ModScanner MODSCANNER;
  private final ApplicationProperties APPLICATIONPROPERTIES;
  private final Utilities UTILITIES;
  private final ApplicationAddons APPLICATIONADDONS;
  private final SimpleStopWatch STOPWATCH_SCANS = new SimpleStopWatch();
  private final String[] MOD_FILE_ENDINGS = new String[]{"jar", "disabled"};

  /**
   * Create a new instance to create server packs.
   *
   * @param injectedApplicationProperties Base settings of ServerPackCreator needed for server pack
   *                                      generation, such as access to the directories, script
   *                                      templates and so on.
   * @param injectedVersionMeta           Meta for modloader and version specific checks and
   *                                      information gathering, such as modloader installer
   *                                      downloads.
   * @param injectedUtilities             Common utilities used across ServerPackCreator.
   * @param injectedApplicationAddons     Any addons which a user may want to execute during the
   *                                      generation of a server pack.
   * @param injectedModScanner            In case a user enabled automatic sideness detection, this
   *                                      will exclude clientside-only mods from a server pack.
   * @author Griefed
   */
  @Autowired
  public ServerPackHandler(
      @NotNull final ApplicationProperties injectedApplicationProperties,
      @NotNull final VersionMeta injectedVersionMeta,
      @NotNull final Utilities injectedUtilities,
      @NotNull final ApplicationAddons injectedApplicationAddons,
      @NotNull final ModScanner injectedModScanner) {

    APPLICATIONPROPERTIES = injectedApplicationProperties;
    VERSIONMETA = injectedVersionMeta;
    UTILITIES = injectedUtilities;
    APPLICATIONADDONS = injectedApplicationAddons;
    MODSCANNER = injectedModScanner;
  }

  /**
   * Create a server pack from a given instance of {@link ServerPackModel} when running as a
   * webservice.
   *
   * @param serverPackModel An instance of {@link ServerPackModel} which contains the configuration
   *                        of the modpack.
   * @return The passed {@link ServerPackModel} which got altered during the creation of said server
   * pack.
   * @author Griefed
   */
  @Contract("_ -> param1")
  public @NotNull ServerPackModel run(@NotNull final ServerPackModel serverPackModel) {

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
   * which the server pack will be created has all its spaces replaces with underscores, so
   * {@code Survive Create Prosper 4 - 5.0.1} would become {@code Survive_Create_Prosper_4_-_5.0.1 }
   * Even though it is the year 2022, spaces in paths can and do still cause trouble. Such as for
   * Powershell scripts. Powershell throws a complete fit if the path contains spaces....soooooo, we
   * remove them. Better safe than sorry.
   *
   * @param configurationModel Model containing the modpack directory of the modpack from which the
   *                           server pack will be generated.
   * @return The complete path to the directory in which the server pack will be generated.
   * @author Griefed
   */
  public @NotNull String getServerPackDestination(
      @NotNull final ConfigurationModel configurationModel) {

    String serverPackToBe = new File(configurationModel.getModpackDir()).getName()
        + configurationModel.getServerPackSuffix();

    serverPackToBe = UTILITIES.StringUtils().pathSecureText(serverPackToBe.replace(" ", "_"));

    return new File(APPLICATIONPROPERTIES.serverPacksDirectory(), serverPackToBe)
        .getPath();
  }

  /**
   * Create a server pack from a given instance of {@link ConfigurationModel}.
   *
   * @param configurationModel An instance of {@link ConfigurationModel} which contains the
   *                           configuration of the modpack from which the server pack is to be
   *                           created.
   * @return {@code true} if the server pack was successfully generated.
   * @author Griefed
   */
  public boolean run(@NotNull final ConfigurationModel configurationModel) {

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

      APPLICATIONADDONS.runPreGenExtensions(configurationModel, destination);

      // Recursively copy all specified directories and files, excluding clientside-only mods, to
      // server pack.
      copyFiles(configurationModel);

      // If modloader is fabric, try and replace the old server-launch.jar with the new and improved
      // one which also downloads the Minecraft server.
      if (configurationModel.getModLoader().equalsIgnoreCase("Fabric")) {
        provideImprovedFabricServerLauncher(configurationModel);
      }

      // If true, copy the server-icon.png from server_files to the server pack.
      if (configurationModel.isServerIconInclusionDesired()) {
        copyIcon(configurationModel);
      } else {

        LOG.info("Not including servericon.");
      }

      // If true, copy the server.properties from server_files to the server pack.
      if (configurationModel.isServerPropertiesInclusionDesired()) {
        copyProperties(configurationModel);
      } else {

        LOG.info("Not including server.properties.");
      }

      APPLICATIONADDONS.runPreZipExtensions(configurationModel, destination);

      // If true, create a ZIP-archive excluding the Minecraft server JAR of the server pack.
      if (configurationModel.isZipCreationDesired()) {

        /*
         * Create the start scripts for this server pack. Ignores custom SPC_JAVA_SPC setting if one
         * is present. This is because a ZIP-archive, if one is created, is supposed to be uploaded
         * to platforms like CurseForge. We must not have scripts with custom Java paths there.
         */
        createStartScripts(configurationModel, false);

        zipBuilder(configurationModel);
      } else {

        LOG.info("Not creating zip archive of serverpack.");
      }

      /*
       * Create the start scripts for this server pack to be used for local testing.
       * The difference to the previous call is that these scripts respect the SPC_JAVA_SPC
       * placeholder setting, if the user has set one
       */
      createStartScripts(configurationModel, true);

      // If true, Install the modloader software for the specified Minecraft version, modloader,
      // modloader version
      if (configurationModel.isServerInstallationDesired()) {
        installServer(configurationModel);
      } else {

        LOG.info("Not installing modded server.");
      }

      // Inform user about location of newly generated server pack.
      LOG.info("Server pack available at: " + destination);
      LOG.info("Server pack archive available at: " + destination + "_server_pack.zip");
      LOG.info("Done!");

      APPLICATIONADDONS.runPostGenExtensions(configurationModel, destination);
    }
    return true;
  }

  /**
   * Deletes all files, directories and ZIP-archives of previously generated server packs to ensure
   * newly generated server pack is as clean as possible. This will completely empty the server pack
   * directory, so use with caution!
   *
   * @param deleteZip   Whether to delete the server pack ZIP-archive.
   * @param destination The destination at which to clean up in.
   * @author Griefed
   */
  public void cleanupEnvironment(boolean deleteZip,
                                 @NotNull final String destination) {

    LOG.info("Found old server_pack. Cleaning up...");

    FileUtils.deleteQuietly(new File(destination));

    if (deleteZip) {

      LOG.info("Found old server_pack.zip. Cleaning up...");

      FileUtils.deleteQuietly(new File(destination + "_server_pack.zip"));
    }
  }

  /**
   * Copies all specified directories and mods, excluding clientside-only mods, from the modpack
   * directory into the server pack directory. If a {@code source/file;destination/file}
   * -combination is provided, the specified source-file is copied to the specified
   * destination-file. One of the reasons as to why it is recommended to run a given
   * ConfigurationModel through the ConfigurationHandler first, is because the ConfigurationHandler
   * will resolve links to their actual files first before then correcting the given
   * ConfigurationModel.
   *
   * @param configurationModel ConfigurationModel containing the modpack directory, list of
   *                           directories and files to copy, list of clientside-only mods to
   *                           exclude, the Minecraft version used by the modpack and server pack,
   *                           and the modloader used by the modpack and server pack.
   * @author Griefed
   */
  public void copyFiles(@NotNull final ConfigurationModel configurationModel) {
    copyFiles(configurationModel.getModpackDir(), configurationModel.getCopyDirs(),
              configurationModel.getClientMods(), configurationModel.getMinecraftVersion(),
              getServerPackDestination(configurationModel), configurationModel.getModLoader());
  }

  /**
   * Download and provide the improved Fabric Server Launcher, if it is available for the given
   * Minecraft and Fabric version.
   *
   * @param configurationModel ConfigurationModel containing the Minecraft and Fabric version for
   *                           which to acquire the improved Fabric Server Launcher.
   * @author Griefed
   */
  public void provideImprovedFabricServerLauncher(
      @NotNull final ConfigurationModel configurationModel) {

    provideImprovedFabricServerLauncher(configurationModel.getMinecraftVersion(),
                                        configurationModel.getModLoaderVersion(),
                                        getServerPackDestination(configurationModel));
  }

  /**
   * Copies the server-icon.png into server pack. The sever-icon is automatically scaled to a
   * resolution of 64x64 pixels.
   *
   * @param configurationModel Containing the modpack directory to acquire the destination of the
   *                           server pack and the path to the server icon to copy.
   * @author Griefed
   */
  public void copyIcon(@NotNull final ConfigurationModel configurationModel) {
    copyIcon(getServerPackDestination(configurationModel), configurationModel.getServerIconPath());
  }

  /**
   * Copies the server.properties into server pack.
   *
   * @param configurationModel Containing the modpack directory to acquire the destination of the
   *                           server pack and the path to the server properties to copy.
   * @author Griefed
   */
  public void copyProperties(@NotNull final ConfigurationModel configurationModel) {
    copyProperties(getServerPackDestination(configurationModel),
                   configurationModel.getServerPropertiesPath());
  }

  /**
   * Create start-scripts for the generated server pack using the templates the user has defined for
   * their instance of ServerPackCreator in {@link ApplicationProperties#scriptTemplates()}.
   *
   * @param configurationModel Configuration model containing modpack specific values. keys to be
   *                           replaced with their respective values in the start scripts, as well
   *                           as the modpack directory from which the destination of the server
   *                           pack is acquired.
   * @param isLocal            Whether the start scripts should be created for a locally usable
   *                           server pack. Use {@code false} if the start scripts should be created
   *                           for a server pack about to be zipped.
   * @author Griefed
   */
  public void createStartScripts(@NotNull final ConfigurationModel configurationModel,
                                 boolean isLocal) {
    createStartScripts(configurationModel.getScriptSettings(),
                       getServerPackDestination(configurationModel), isLocal);
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
  public void zipBuilder(@NotNull final ConfigurationModel configurationModel) {
    zipBuilder(configurationModel.getMinecraftVersion(),
               configurationModel.isServerInstallationDesired(),
               getServerPackDestination(configurationModel),
               configurationModel.getModLoader(), configurationModel.getModLoaderVersion());
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
  public void installServer(@NotNull final ConfigurationModel configurationModel) {
    installServer(configurationModel.getModLoader(), configurationModel.getMinecraftVersion(),
                  configurationModel.getModLoaderVersion(),
                  getServerPackDestination(configurationModel));
  }

  /**
   * Copies all specified directories and mods, excluding clientside-only mods, from the modpack
   * directory into the server pack directory. If a {@code source/file;destination/file}
   * -combination is provided, the specified source-file is copied to the specified
   * destination-file. One of the reasons as to why it is recommended to run a given
   * ConfigurationModel through the ConfigurationHandler first, is because the ConfigurationHandler
   * will resolve links to their actual files first before then correcting the given
   * ConfigurationModel.
   *
   * @param modpackDir        Files and directories are copied into the server_pack directory inside
   *                          the modpack directory.
   * @param directoriesToCopy All directories and files therein to copy to the server pack.
   * @param clientMods        List of clientside-only mods to exclude from the server pack.
   * @param minecraftVersion  The Minecraft version the modpack uses.
   * @param destination       The destination where the files should be copied to.
   * @param modloader         The modloader used for mod sideness detection.
   * @author Griefed
   */
  public void copyFiles(
      @NotNull final String modpackDir,
      @NotNull final List<String> directoriesToCopy,
      @NotNull final List<String> clientMods,
      @NotNull final String minecraftVersion,
      @NotNull final String destination,
      @NotNull final String modloader) {

    try {
      Files.createDirectories(Paths.get(destination));
    } catch (IOException ex) {
      LOG.error("Failed to create directory " + destination);
    }

    if (directoriesToCopy.size() == 1 && directoriesToCopy.get(0).equals("lazy_mode")) {

      LOG.warn(
          "!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!");
      LOG.warn(
          "Lazy mode specified. This will copy the WHOLE modpack to the server pack. No exceptions.");
      LOG.warn(
          "You will not receive any support for a server pack generated this way.");
      LOG.warn(
          "Do not open an issue on GitHub if this configuration errors or results in a broken server pack.");
      LOG.warn(
          "!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!");

      try {
        FileUtils.copyDirectory(new File(modpackDir), new File(destination));
      } catch (IOException ex) {
        LOG.error("An error occurred copying the modpack to the server pack in lazy mode.", ex);
      }

    } else {

      TreeSet<String> exclusions = new TreeSet<>();
      directoriesToCopy.removeIf(exclude -> {

        if (exclude.startsWith("!")) {
          exclusions.add(exclude.substring(1));
          return true;
        } else {
          return false;
        }
      });

      List<ServerPackFile> serverPackFiles = new ArrayList<>(100000);

      for (String directory : directoriesToCopy) {

        String clientDir = modpackDir + File.separator + directory;
        String serverDir = destination + File.separator + directory;

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
                    mod,
                    new File(serverDir, mod.getName())));
          }

          /*
           * The user wants to add files to the server pack based on a regex-filter.
           * Every match will be added.
           */
        } else if (directory.contains("==")) {

          serverPackFiles.addAll(getRegexMatches(modpackDir, destination, directory));

        } else if (new File(directory).isFile()) {

          serverPackFiles.add(
              new ServerPackFile(
                  new File(directory),
                  new File(destination, new File(directory).getName())));

        } else if (new File(directory).isDirectory()) {

          serverPackFiles.addAll(getDirectoryFiles(directory, destination));

        } else {

          serverPackFiles.addAll(getDirectoryFiles(clientDir, destination));
        }
      }

      LOG.info("Ensuring files and/or directories are properly excluded.");

      serverPackFiles.removeIf(
          serverPackFile ->
              excludeFileOrDirectory(
                  modpackDir,
                  serverPackFile.SOURCE_FILE,
                  exclusions));

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
      @NotNull final String minecraftVersion,
      @NotNull final String fabricVersion,
      @NotNull final String destination) {

    String fileDestination = destination + File.separator + "fabric-server-launcher.jar";

    if (VERSIONMETA.fabric().improvedLauncherUrl(minecraftVersion, fabricVersion).isPresent()
        && UTILITIES.WebUtils()
                    .downloadFile(
                        fileDestination,
                        VERSIONMETA.fabric().improvedLauncherUrl(minecraftVersion, fabricVersion)
                                   .get())) {

      LOG.info("Successfully provided improved Fabric Server Launcher.");

      try (BufferedWriter writer =
          new BufferedWriter(
              new FileWriter(
                  new File(destination, "SERVER_PACK_INFO.txt")))
      ) {

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
   * Copies the server-icon.png into server pack. The sever-icon is automatically scaled to a
   * resolution of 64x64 pixels.
   *
   * @param destination      The destination where the icon should be copied to.
   * @param pathToServerIcon The path to the custom server-icon.
   * @author Griefed
   */
  public void copyIcon(
      @NotNull final String destination,
      @NotNull final String pathToServerIcon) {

    LOG.info("Copying server-icon.png...");

    File customIcon =
        new File(destination, APPLICATIONPROPERTIES.defaultServerIcon().getName());

    if (new File(pathToServerIcon).exists()) {

      BufferedImage originalImage;
      //noinspection UnusedAssignment
      Image scaledImage = null;

      try {
        originalImage = ImageIO.read(new File(pathToServerIcon));

        if (originalImage.getHeight() == 64 && originalImage.getWidth() == 64) {

          try {
            FileUtils.copyFile(new File(pathToServerIcon), customIcon);
          } catch (IOException e) {
            LOG.error("An error occurred trying to copy the server-icon.", e);
          }

        } else {

          // Scale our image to 64x64
          scaledImage = originalImage.getScaledInstance(
              64,
              64,
              Image.SCALE_SMOOTH);

          BufferedImage outputImage =
              new BufferedImage(
                  scaledImage.getWidth(null),
                  scaledImage.getHeight(null),
                  BufferedImage.TYPE_INT_ARGB);

          outputImage.getGraphics().drawImage(
              scaledImage,
              0,
              0,
              null);

          // Save our scaled image to disk.
          try {
            ImageIO.write(outputImage, "png", customIcon);
          } catch (IOException ex) {
            LOG.error("Error scaling image.", ex);
          }
        }

      } catch (Exception ex) {
        LOG.error("Error reading server-icon image.", ex);
      }

    } else if (pathToServerIcon.isEmpty()) {

      LOG.info("No custom icon specified or the file doesn't exist.");

      try {
        FileUtils.copyFile(
            APPLICATIONPROPERTIES.defaultServerIcon(),
            customIcon);
      } catch (IOException ex) {
        LOG.error("An error occurred trying to copy the server-icon.", ex);
      }

    } else {
      LOG.error("The specified server-icon does not exist: " + pathToServerIcon);
    }
  }

  /**
   * Copies the server.properties into server pack.
   *
   * @param destination            The destination where the properties should be copied to.
   * @param pathToServerProperties The path to the custom server.properties.
   * @author Griefed
   */
  public void copyProperties(
      @NotNull final String destination,
      @NotNull final String pathToServerProperties) {

    LOG.info("Copying server.properties...");

    File customProperties =
        new File(
            destination,
            APPLICATIONPROPERTIES.defaultServerProperties().getName());

    if (new File(pathToServerProperties).exists()) {
      try {
        FileUtils.copyFile(new File(pathToServerProperties), customProperties);
      } catch (IOException ex) {
        LOG.error("An error occurred trying to copy the server.properties-file.", ex);
      }

    } else if (pathToServerProperties.isEmpty()) {

      LOG.info("No custom properties specified or the file doesn't exist.");

      try {
        FileUtils.copyFile(APPLICATIONPROPERTIES.defaultServerProperties(),
                           customProperties);
      } catch (IOException ex) {
        LOG.error("An error occurred trying to copy the server.properties-file.", ex);
      }

    } else {
      LOG.error("The specified server.properties does not exist: " + pathToServerProperties);
    }
  }

  /**
   * Create start-scripts for the generated server pack using the templates the user has defined for
   * their instance of ServerPackCreator in {@link ApplicationProperties#scriptTemplates()}.
   *
   * @param scriptSettings Key-value pairs to replace in the script. A given key in the script is
   *                       replaced with its value.
   * @param destination    The destination where the scripts should be created in.
   * @param isLocal        Whether the start scripts should be created for a locally usable server
   *                       pack. Use {@code false} if the start scripts should be created for a
   *                       server pack about to be zipped.
   * @author Griefed
   */
  public void createStartScripts(@NotNull final HashMap<String, String> scriptSettings,
                                 @NotNull final String destination,
                                 boolean isLocal) {
    for (File template : APPLICATIONPROPERTIES.scriptTemplates()) {

      try {
        String fileEnding = template.toString().substring(template.toString().lastIndexOf(".") + 1);
        File destinationScript = new File(destination, "start." + fileEnding);

        String scriptContent = FileUtils.readFileToString(template, StandardCharsets.UTF_8);

        for (Map.Entry<String, String> entry : scriptSettings.entrySet()) {
          if (isLocal && entry.getKey().equals("SPC_JAVA_SPC")) {

            scriptContent = scriptContent.replace(entry.getKey(), entry.getValue());

          } else if (!isLocal && entry.getKey().equals("SPC_JAVA_SPC")) {

            scriptContent = scriptContent.replace(entry.getKey(), "java");

          } else {
            scriptContent = scriptContent.replace(entry.getKey(), entry.getValue());
          }
        }

        FileUtils.writeStringToFile(
            destinationScript,
            scriptContent.replace("\r", ""),
            StandardCharsets.UTF_8);

      } catch (Exception ex) {
        LOG.error("File not accessible: " + template + ".", ex);
      }
    }
  }

  /**
   * Creates a ZIP-archive of specified directory. Depending on
   * {@link ApplicationProperties#isZipFileExclusionEnabled()}, files will be excluded. To customize
   * the files which will be excluded, see
   * {@link ApplicationProperties#getFilesToExcludeFromZipArchive()}. The created ZIP-archive will
   * be stored alongside the specified destination, with {@code _server_pack.zip} appended to its
   * name.
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
      @NotNull final String minecraftVersion,
      boolean includeServerInstallation,
      @NotNull final String destination,
      @NotNull final String modloader,
      @NotNull final String modloaderVersion) {

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
                          destination,
                          entry
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
            + APPLICATIONPROPERTIES.serverPackCreatorVersion()
            + " by Griefed.";

    zipParameters.setIncludeRootFolder(false);
    zipParameters.setFileComment(comment);

    try (ZipFile zip = new ZipFile(destination + "_server_pack.zip")) {

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
   * Installs the modloader server for the specified modloader, modloader version and Minecraft
   * version.
   *
   * @param modLoader        The modloader for which to install the server software. Either Forge or
   *                         Fabric.
   * @param minecraftVersion The Minecraft version for which to install the modloader and Minecraft
   *                         server.
   * @param modLoaderVersion The modloader version for which to install the modloader and Minecraft
   *                         server.
   * @param destination      The destination where the modloader server should be installed into.
   * @author Griefed
   */
  public void installServer(
      @NotNull final String modLoader,
      @NotNull final String minecraftVersion,
      @NotNull final String modLoaderVersion,
      @NotNull final String destination) {

    if (!serverDownloadable(minecraftVersion, modLoader, modLoaderVersion)) {
      LOG.error("The servers for " + minecraftVersion + ", " + modLoader + " " + modLoaderVersion
                    + " are currently unreachable. Skipping server installation.");
    }

    List<String> commandArguments = new ArrayList<>(10);
    commandArguments.add(APPLICATIONPROPERTIES.java());

    commandArguments.add("-jar");
    Process process = null;
    BufferedReader bufferedReader = null;

    switch (modLoader) {
      case "Fabric":
        LOG_INSTALLER.info("Starting Fabric installation.");

        if (UTILITIES.WebUtils()
                     .downloadFile(destination + File.separator + "fabric-installer.jar",
                                   VERSIONMETA.fabric().releaseInstallerUrl())) {

          LOG.info("Fabric installer successfully downloaded.");

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
          return;
        }
        break;

      case "Forge":
        LOG_INSTALLER.info("Starting Forge installation.");

        if (VERSIONMETA.forge().getForgeInstance(minecraftVersion, modLoaderVersion).isPresent()
            && UTILITIES.WebUtils().downloadFile(
            destination + File.separator + "forge-installer.jar",
            VERSIONMETA.forge().getForgeInstance(minecraftVersion, modLoaderVersion).get()
                       .installerUrl())
        ) {

          LOG.info("Forge installer successfully downloaded.");

          commandArguments.add("forge-installer.jar");
          commandArguments.add("--installServer");

        } else {

          LOG.error(
              "Something went wrong during the installation of Forge. Maybe the Forge servers are down or unreachable? Skipping...");
          return;
        }
        break;

      case "Quilt":
        LOG_INSTALLER.info("Starting Quilt installation.");

        if (UTILITIES.WebUtils().downloadFile(destination + File.separator + "quilt-installer.jar",
                                              VERSIONMETA.quilt().releaseInstallerUrl())
        ) {

          LOG.info("Quilt installer successfully downloaded.");

          commandArguments.add("quilt-installer.jar");
          commandArguments.add("install");
          commandArguments.add("server");
          commandArguments.add(minecraftVersion);
          commandArguments.add("--download-server");
          commandArguments.add("--install-dir=" + new File(destination).getAbsolutePath());

        } else {

          LOG.error(
              "Something went wrong during the installation of Quilt. Maybe the Quilt servers are down or unreachable? Skipping...");
          return;
        }
        break;

      case "LegacyFabric":
        LOG_INSTALLER.info("Starting Legacy Fabric installation.");

        try {

          if (UTILITIES.WebUtils()
                       .downloadFile(destination + File.separator + "legacyfabric-installer.jar",
                                     VERSIONMETA.legacyFabric().releaseInstallerUrl())) {

            LOG.info("LegacyFabric installer successfully downloaded.");

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
            return;
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
      LOG.debug("Executing in: " + new File(destination));

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
    }

    if (modLoader.equalsIgnoreCase("Forge")) {
      try {
        Path path =
            Paths.get(
                destination + File.separator + "forge-" + minecraftVersion + "-" + modLoaderVersion
                    + ".jar");

        if (new File(
            destination + File.separator + "forge-" + minecraftVersion + "-" + modLoaderVersion
                + ".jar")
            .exists()) {

          Files.copy(path, Paths.get(destination + File.separator + "forge.jar"), REPLACE_EXISTING);
          FileUtils.deleteQuietly(path.toFile());
        }

      } catch (IOException ex) {
        LOG.error("Could not rename forge-" + minecraftVersion + "-" + modLoaderVersion
                      + ".jar to forge.jar", ex);
      }
    }

    if (APPLICATIONPROPERTIES.isServerPackCleanupEnabled()) {
      cleanUpServerPack(minecraftVersion, modLoaderVersion, destination);
    } else {
      LOG.info("Server pack cleanup disabled.");
    }
  }

  /**
   * Gather a list of all files from an explicit source;destination-combination. If the source is a
   * file, a singular {@link ServerPackFile} is returned. If the source is a directory, then all
   * files in said directory are returned.
   *
   * @param combination Array containing a source-file/directory;destination-file/directory
   *                    combination.
   * @param modpackDir  The modpack-directory.
   * @param destination The destination, normally the server pack-directory.
   * @return List of {@link ServerPackFile}.
   * @author Griefed
   */
  private @NotNull List<ServerPackFile> getExplicitFiles(
      @NotNull final String @NotNull [] combination,
      @NotNull final String modpackDir,
      @NotNull final String destination) {

    List<ServerPackFile> serverPackFiles = new ArrayList<>(100);

    if (new File(modpackDir, combination[0]).isFile()) {

      serverPackFiles.add(
          new ServerPackFile(
              new File(modpackDir, combination[0]),
              new File(destination, combination[1])));

    } else if (new File(modpackDir, combination[0]).isDirectory()) {

      serverPackFiles.addAll(
          getDirectoryFiles(
              modpackDir + File.separator + combination[0],
              destination));

    } else if (new File(combination[0]).isFile()) {

      serverPackFiles.add(
          new ServerPackFile(
              new File(combination[0]),
              new File(destination, combination[1])));

    } else if (new File(combination[0]).isDirectory()) {

      serverPackFiles.addAll(getDirectoryFiles(combination[0], destination));
    }

    return serverPackFiles;
  }

  /**
   * Recursively acquire all files and directories inside the given save-directory as a list of
   * {@link ServerPackFile}.
   *
   * @param clientDir   Target directory in the server pack. Usually the name of the world.
   * @param directory   The save-directory.
   * @param destination The destination of the server pack.
   * @return List of {@link ServerPackFile} which will be included in the server pack.
   * @author Griefed
   */
  private @NotNull List<ServerPackFile> getSaveFiles(
      @NotNull final String clientDir,
      @NotNull final String directory,
      @NotNull final String destination) {

    List<ServerPackFile> serverPackFiles = new ArrayList<>(2000);

    try (Stream<Path> files = Files.walk(Paths.get(clientDir))) {
      files.forEach(
          file -> {
            try {
              serverPackFiles.add(new ServerPackFile(
                  file,
                  Paths.get(destination + File.separator + directory.substring(6))
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
   * @param modsDir                 The mods-directory of the modpack of which to generate a list of
   *                                all its contents.
   * @param userSpecifiedClientMods A list of all clientside-only mods.
   * @param minecraftVersion        The Minecraft version the modpack uses. When the modloader is
   *                                Forge, this determines whether Annotations or Tomls are
   *                                scanned.
   * @param modloader               The modloader the modpack uses.
   * @return A list of all mods to include in the server pack.
   * @author Griefed
   */
  @Contract("_, _, _, _ -> new")
  public @NotNull List<File> getModsToInclude(@NotNull final String modsDir,
                                              @NotNull final List<String> userSpecifiedClientMods,
                                              @NotNull final String minecraftVersion,
                                              @NotNull final String modloader) {

    LOG.info("Preparing a list of mods to include in server pack...");

    Collection<File> filesInModsDir =
        new ArrayList<>(
            FileUtils.listFiles(
                new File(modsDir),
                MOD_FILE_ENDINGS,
                true));

    TreeSet<File> modsInModpack = new TreeSet<>(filesInModsDir);
    List<File> autodiscoveredClientMods = new ArrayList<>(100);

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

      LOG.debug(
          "Scanning and excluding of " + filesInModsDir.size() + " mods took "
              + STOPWATCH_SCANS.stop().getTime());

    } else {
      LOG.info("Automatic clientside-only mod detection disabled.");
    }

    // Exclude user-specified mods from copying.
    excludeUserSpecifiedMod(userSpecifiedClientMods, modsInModpack);

    return new ArrayList<>(modsInModpack);
  }

  /**
   * Acquire all files and directories for the given regex in a list of {@link ServerPackFile} for a
   * given regex-entry from the configuration models copyDirs.
   *
   * @param modpackDir  The path to the modpack directory in which to check for existence of the
   *                    passed list of directories.
   * @param destination The destination where the files should be copied to.
   * @param entry       The regex, or file/directory and regex, combination.
   * @return List of {@link ServerPackFile} which will be included in the server pack.
   * @author Griefed
   */
  private @NotNull List<ServerPackFile> getRegexMatches(
      @NotNull final String modpackDir,
      @NotNull final String destination,
      @NotNull final String entry) {

    List<ServerPackFile> serverPackFiles = new ArrayList<>(100);

    if (entry.startsWith("==") && entry.length() > 2) {

      regexWalk(new File(modpackDir),
                destination,
                entry.substring(2),
                serverPackFiles);

    } else if (entry.contains("==") && entry.split("==").length == 2) {

      String[] regexinclusion = entry.split("==");

      if (new File(modpackDir, regexinclusion[0]).isDirectory()) {

        regexWalk(new File(
                      modpackDir,
                      regexinclusion[0]),
                  destination,
                  regexinclusion[1],
                  serverPackFiles);

      } else if (new File(regexinclusion[0]).isDirectory()) {

        regexWalk(new File(regexinclusion[0]),
                  destination,
                  regexinclusion[1],
                  serverPackFiles);
      }

    }
    return serverPackFiles;
  }

  /**
   * Recursively acquire all files and directories inside the given directory as a list of
   * {@link ServerPackFile}.
   *
   * @param source      The source-directory.
   * @param destination The server pack-directory.
   * @return List of files and folders of the server pack.
   * @author Griefed
   */
  private @NotNull List<ServerPackFile> getDirectoryFiles(
      @NotNull final String source,
      @NotNull final String destination) {

    List<ServerPackFile> serverPackFiles = new ArrayList<>(100);
    try (Stream<Path> files = Files.walk(Paths.get(source))) {

      files.forEach(
          file -> {
            try {
              serverPackFiles.add(
                  new ServerPackFile(
                      file,
                      Paths.get(destination + File.separator + new File(source).getName())
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
   * Check whether the given file or directory should be excluded from the server pack.
   *
   * @param modpackDir     The directory where the modpack resides in. Used to filter out any
   *                       unwanted directories using
   *                       {@link ApplicationProperties#getDirectoriesToExclude()}.
   * @param fileToCheckFor The file or directory to check whether it should be excluded from the
   *                       server pack.
   * @param exclusions     Files or directories determined by ServerPackCreator to be excluded from
   *                       the server pack
   * @return {@code true} if the file or directory was determined to be excluded from the server
   * pack.
   * @author Griefed
   */
  private boolean excludeFileOrDirectory(
      @NotNull final String modpackDir,
      @NotNull final File fileToCheckFor,
      @NotNull final TreeSet<String> exclusions) {

    exclusions.addAll(APPLICATIONPROPERTIES.getDirectoriesToExclude());

    for (String exclusion : exclusions) {

      // Exclude based on regex matches. Scary stuff.
      if (exclusion.contains("==")) {

        // Tell a user to use !==.* and watch the world burn, hehehe.
        if (exclusion.startsWith("==") && fileToCheckFor.getAbsolutePath()
                                                        .matches(exclusion.substring(2))) {

          LOG.debug("Excluding file/directory: " + fileToCheckFor.getAbsolutePath());
          return true;

        } else if (exclusion.split("==").length == 2) {

          String[] regexclusion = exclusion.split("==");
          String toMatch;

          if (new File(modpackDir, regexclusion[0]).isDirectory()) {

            toMatch = fileToCheckFor.getAbsolutePath().replace(
                new File(modpackDir, regexclusion[0]).getAbsolutePath(),
                "");

            if (toMatch.startsWith(File.separator)) {
              toMatch = toMatch.substring(1);
            }

            if (toMatch.matches(regexclusion[1])) {
              LOG.debug("Excluding file/directory: " + fileToCheckFor.getAbsolutePath());
              return true;
            }

          } else if (new File(regexclusion[0]).isDirectory()) {

            toMatch = fileToCheckFor.getAbsolutePath().replace(
                new File(regexclusion[0]).getAbsolutePath(),
                "");

            if (toMatch.startsWith(File.separator)) {
              toMatch = toMatch.substring(1);
            }

            if (toMatch.matches(regexclusion[1])) {
              LOG.debug("Excluding file/directory: " + fileToCheckFor.getAbsolutePath());
              return true;
            }
          }

          if (fileToCheckFor.getAbsolutePath()
                            .startsWith(new File(regexclusion[0]).getAbsolutePath())
              && fileToCheckFor.getAbsolutePath()
                               .replace(new File(regexclusion[0]).getAbsolutePath(), "")
                               .matches(regexclusion[1])) {
            LOG.debug("Excluding file/directory: " + fileToCheckFor.getAbsolutePath());
            return true;
          }
        }

        // Exclude files with a specific file-ending.
      } else if (exclusion.matches("^\\.[0-9a-zA-Z]+$") && fileToCheckFor.getAbsolutePath()
                                                                         .endsWith(exclusion)) {
        LOG.debug("Excluding file/directory: " + fileToCheckFor.getAbsolutePath());
        return true;

        // Exclude specific file/directory inside modpack from server pack
      } else if (fileToCheckFor.getAbsolutePath()
                               .startsWith(new File(modpackDir, exclusion).getAbsolutePath())) {
        LOG.debug("Excluding file/directory: " + fileToCheckFor.getAbsolutePath());
        return true;
      }
    }

    return false;
  }

  /**
   * Check whether the installer for the given combination of Minecraft version, modloader and
   * modloader version is available/reachable.
   *
   * @param mcVersion        The Minecraft version.
   * @param modloader        The modloader.
   * @param modloaderVersion The modloader version.
   * @return {@code true} if the installer can be downloaded.
   * @author Griefed
   */
  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  public boolean serverDownloadable(
      @NotNull final String mcVersion,
      @NotNull final String modloader,
      @NotNull final String modloaderVersion) {

    switch (modloader) {

      case "Fabric":
        return UTILITIES.WebUtils().isReachable(VERSIONMETA.fabric().releaseInstallerUrl());

      case "Forge":
        return VERSIONMETA.forge().getForgeInstance(mcVersion, modloaderVersion).isPresent()
            && UTILITIES.WebUtils().isReachable(
            VERSIONMETA.forge().getForgeInstance(mcVersion, modloaderVersion).get()
                       .installerUrl());

      case "Quilt":
        return UTILITIES.WebUtils().isReachable(VERSIONMETA.quilt().releaseInstallerUrl());

      case "LegacyFabric":
        try {
          return UTILITIES.WebUtils().isReachable(VERSIONMETA.legacyFabric().releaseInstallerUrl());
        } catch (MalformedURLException e) {
          return false;
        }

      default:
        return false;
    }
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
      @NotNull final String minecraftVersion,
      @NotNull final String modLoaderVersion,
      @NotNull final String destination) {

    LOG.info("Cleanup after modloader server installation.");

    FileUtils.deleteQuietly(new File(destination, "fabric-installer.jar"));
    FileUtils.deleteQuietly(new File(destination, "forge-installer.jar"));
    FileUtils.deleteQuietly(new File(destination, "quilt-installer.jar"));
    FileUtils.deleteQuietly(new File(destination, "installer.log"));
    FileUtils.deleteQuietly(new File(destination, "forge-installer.jar.log"));
    FileUtils.deleteQuietly(new File(destination, "legacyfabric-installer.jar"));
    FileUtils.deleteQuietly(new File(destination, "run.bat"));
    FileUtils.deleteQuietly(new File(destination, "run.sh"));
    FileUtils.deleteQuietly(new File(destination, "user_jvm_args.txt"));
  }

  /**
   * Exclude every automatically discovered clientside-only mod from the list of mods in the
   * modpack.
   *
   * @param autodiscoveredClientMods Automatically discovered clientside-only mods in the modpack.
   * @param modsInModpack            All mods in the modpack.
   * @author Griefed
   */
  private void excludeMods(
      @NotNull final List<File> autodiscoveredClientMods,
      @NotNull final TreeSet<File> modsInModpack) {

    if (!autodiscoveredClientMods.isEmpty()) {

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
   * @param modsInModpack           Every mod ending with {@code jar} or {@code disabled} in the
   *                                modpack.
   * @author Griefed
   */
  private void excludeUserSpecifiedMod(
      @NotNull final List<String> userSpecifiedExclusions,
      @NotNull final TreeSet<File> modsInModpack) {

    if (!userSpecifiedExclusions.isEmpty()) {
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
   * Walk through the specified directory and add a {@link ServerPackFile} for every file/folder
   * which matches the given regex.
   *
   * @param source          The source-directory to walk through and perform regex-matches in.
   * @param destination     The destination-directory where a matched file should be copied to,
   *                        usually the server pack directory.
   * @param regex           Regex with which to perform matches against files in the
   *                        source-directory.
   * @param serverPackFiles List of files to copy to the server pack to which any matched file will
   *                        be added to.
   * @author Griefed
   */
  private void regexWalk(
      @NotNull final File source,
      @NotNull final String destination,
      @NotNull final String regex,
      @NotNull final List<ServerPackFile> serverPackFiles) {

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

              Path add = Paths.get(destination + File.separator + source.getName())
                              .resolve(source.toPath().relativize(file));
              serverPackFiles.add(
                  new ServerPackFile(
                      file,
                      add));
              LOG.debug("Including through regex-match:");
              LOG.debug("    SOURCE: " + file);
              LOG.debug("    DESTINATION: " + add);
            }
          }
      );
    } catch (IOException ex) {
      LOG.error("Couldn't gather all files from "
                    + source.getAbsolutePath()
                    + " for filter \""
                    + regex
                    + "\".", ex);
    }
  }

  /**
   * Go through the mods in the modpack and exclude any of the user-specified clientside-only mods
   * according to the filter method set in the serverpackcreator.properties. For available filters,
   * see {@link de.griefed.serverpackcreator.ApplicationProperties.ExclusionFilter}.
   *
   * @param userSpecifiedExclusion The client mod to check whether it needs to be excluded.
   * @param modsInModpack          All mods in the modpack.
   */
  private void exclude(
      @NotNull final String userSpecifiedExclusion,
      @NotNull final TreeSet<File> modsInModpack) {

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
   * Deletes all files, directories and ZIP-archives of previously generated server packs to ensure
   * newly generated server pack is as clean as possible. This will completely empty the server pack
   * directory, so use with caution!
   *
   * @param deleteZip          Whether to delete the server pack ZIP-archive.
   * @param configurationModel ConfigurationModel containing the modpack directory from which the
   *                           destination of the server pack is acquired.
   * @author Griefed
   */
  public void cleanupEnvironment(boolean deleteZip,
                                 @NotNull final ConfigurationModel configurationModel) {
    cleanupEnvironment(deleteZip, getServerPackDestination(configurationModel));
  }

  /**
   * Gather a list of all files from an explicit source;destination-combination. If the source is a
   * file, a singular {@link ServerPackFile} is returned. If the source is a directory, then all
   * files in said directory are returned.
   *
   * @param combination        Array containing a source-file/directory;destination-file/directory
   *                           combination.
   * @param configurationModel ConfigurationModel containing the modpack directory so the
   *                           destination of the server pack can be acquired.
   * @return List of {@link ServerPackFile}.
   * @author Griefed
   */
  private @NotNull List<ServerPackFile> getExplicitFiles(
      @NotNull final String[] combination,
      @NotNull final ConfigurationModel configurationModel) {

    return getExplicitFiles(combination, configurationModel.getModpackDir(),
                            getServerPackDestination(configurationModel));
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
  @Contract("_ -> new")
  public @NotNull List<File> getModsToInclude(
      @NotNull final ConfigurationModel configurationModel) {

    return getModsToInclude(configurationModel.getModpackDir() + File.separator + "mods",
                            configurationModel.getClientMods(),
                            configurationModel.getMinecraftVersion(),
                            configurationModel.getModLoader());
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
  private void cleanUpServerPack(@NotNull final ConfigurationModel configurationModel) {
    cleanUpServerPack(configurationModel.getMinecraftVersion(),
                      configurationModel.getModLoaderVersion(),
                      getServerPackDestination(configurationModel));
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
    public ServerPackFile(@NotNull File sourceFile,
                          @NotNull File destinationFile)
        throws InvalidPathException {

      this.SOURCE_FILE = sourceFile;
      this.SOURCE_PATH = sourceFile.toPath();
      this.DESTINATION_FILE = destinationFile;
      this.DESTINATION_PATH = destinationFile.toPath();
    }

    /**
     * Construct a new ServerPackFile from two {@link Path}-objects, a source and a destination.
     *
     * @param sourcePath      The source file/directory. Usually a file/directory in a modpack.
     * @param destinationPath The destination file/directory in the server pack.
     * @author Griefed
     */
    public ServerPackFile(@NotNull Path sourcePath,
                          @NotNull Path destinationPath)
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
    @Contract(pure = true)
    public File source() {
      return SOURCE_FILE;
    }

    /**
     * The destination-file.
     *
     * @return The destination-file.
     * @author Griefed
     */
    @Contract(pure = true)
    public File destination() {
      return DESTINATION_FILE;
    }

    /**
     * The path to the source-file.
     *
     * @return The path to the source-file.
     * @author Griefed
     */
    @Contract(pure = true)
    public Path sourcePath() {
      return SOURCE_PATH;
    }

    /**
     * The path to the destination-file.
     *
     * @return The path to the destination-file.
     * @author Griefed
     */
    @Contract(pure = true)
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
        try {
          FileUtils.createParentDirectories(DESTINATION_FILE);
        } catch (IOException ignored) {

        }
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
     * separated by a {@code ;}
     *
     * @return This ServerPackFiles source-file and destination-file as a
     * {@link String}-combination, separated by a {@code ;}
     * @author Griefed
     */
    @Contract(pure = true)
    @Override
    public @NotNull String toString() {
      return SOURCE_PATH + ";" + DESTINATION_PATH;
    }
  }
}
