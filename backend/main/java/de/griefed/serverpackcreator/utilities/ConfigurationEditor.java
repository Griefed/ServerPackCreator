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
package de.griefed.serverpackcreator.utilities;

import com.electronwill.nightconfig.core.file.NoFormatFoundException;
import de.griefed.serverpackcreator.ApplicationProperties;
import de.griefed.serverpackcreator.ConfigurationHandler;
import de.griefed.serverpackcreator.ConfigurationModel;
import de.griefed.serverpackcreator.utilities.common.Utilities;
import de.griefed.serverpackcreator.versionmeta.VersionMeta;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Create and edit a serverpackcreator.conf file via CLI. Allows loading of a custom file for
 * editing purposes and saving any config file under a different name.
 *
 * @author Griefed
 */
public final class ConfigurationEditor {

  private static final Logger LOG = LogManager.getLogger(ConfigurationEditor.class);

  private final ConfigurationHandler CONFIGURATIONHANDLER;
  private final ApplicationProperties APPLICATIONPROPERTIES;
  private final VersionMeta VERSIONMETA;
  private final Utilities UTILITIES;
  private final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(
      "yyyy-MM-dd_HH-mm-ss");
  private File logFile;

  public ConfigurationEditor(
      @NotNull ConfigurationHandler injectedConfigurationHandler,
      @NotNull ApplicationProperties injectedApplicationProperties,
      @NotNull Utilities injectedUtilities,
      @NotNull VersionMeta injectedVersionMeta) {

    APPLICATIONPROPERTIES = injectedApplicationProperties;
    VERSIONMETA = injectedVersionMeta;
    UTILITIES = injectedUtilities;
    CONFIGURATIONHANDLER = injectedConfigurationHandler;
  }

  /**
   * Present the user with a menu, asking them what they would like to do.
   *
   * @author Griefed
   */
  public void continuedRunOptions() {
    LocalDateTime currentTime = LocalDateTime.now();
    logFile = new File(
        "logs/configurationCreator-" + DATE_TIME_FORMATTER.format(currentTime) + ".log");
    checkLogFile();

    Scanner scanner = new Scanner(System.in);
    int selection;

    do {

      printMenu();
      selection = getDecision(scanner, 0, 2);

      switch (selection) {
        case 1:
          createConfigurationFile(scanner);
          break;

        case 2:
          loadAndEdit(scanner);
          break;

        case 0:
          printToFileAndConsole("Exiting Configuration Editor.");
          break;
      }

    } while (selection != 0);

    scanner.close();
  }

  /**
   * Print the text-menu so the user may decide what they would like to do next.
   *
   * @author Griefed
   */
  private void printMenu() {
    printToFileAndConsole();
    printToFileAndConsole("What would you like to do?");
    printToFileAndConsole("(1) : Create new configuration");
    printToFileAndConsole("(2) : Load and edit configuration");
    printToFileAndConsole("(0) : Exit");
    printToFileAndConsole("-------------------------------------------");
    printToFileAndConsole("Enter the number of your selection: ", false);
  }

  /**
   * Allow the user to load and edit an existing configuration.
   *
   * @param scanner Used for reading the users input.
   * @author Griefed
   */
  private void loadAndEdit(@NotNull Scanner scanner) {

    ConfigurationModel configurationModel = new ConfigurationModel();
    String fileName;
    boolean configLoaded = false;

    do {
      try {

        printToFileAndConsole("Please enter the path to the configuration file you want to edit:");
        fileName = getNextLine(scanner);
        configurationModel = new ConfigurationModel(UTILITIES, new File(fileName));
        configLoaded = true;

      } catch (FileNotFoundException | NoFormatFoundException e) {
        printToFileAndConsole("The specified file could not be loaded. " + e.getMessage());
      }

    } while (!configLoaded);

    printToFileAndConsole("Configuration successfully loaded.");
    printToFileAndConsole();

    int selection;

    do {

      CONFIGURATIONHANDLER.printConfigurationModel(configurationModel);
      printEditMenu();

      selection = getDecision(scanner, 0, 18);

      switch (selection) {
        case 1:
          configurationModel.setModpackDir(getModpackDirectory(scanner));
          break;

        case 2:
          getClientSideMods(scanner, configurationModel.getClientMods());
          break;

        case 3:

          getDirsAndFilesToCopy(scanner, configurationModel.getModpackDir(),
                                configurationModel.getCopyDirs());
          break;

        case 4:
          configurationModel.setServerIconPath(getServerIcon(scanner));
          break;

        case 5:
          configurationModel.setServerPropertiesPath(getServerProperties(scanner));
          break;

        case 6:
          configurationModel.setIncludeServerInstallation(installModloaderServer(scanner));
          break;

        case 7:
          configurationModel.setMinecraftVersion(getMinecraftVersion(scanner));
          break;

        case 8:
          configurationModel.setModLoader(getModloader(scanner));
          break;

        case 9:
          configurationModel.setModLoaderVersion(
              getModloaderVersion(scanner, configurationModel.getMinecraftVersion(),
                                  configurationModel.getModLoader()));
          break;

        case 10:
          configurationModel.setIncludeServerIcon(includeServerIcon(scanner));
          break;

        case 11:
          configurationModel.setIncludeServerProperties(includeServerProperties(scanner));
          break;

        case 12:
          configurationModel.setIncludeZipCreation(includeZipCreation(scanner));
          break;

        case 13:
          configurationModel.setJavaArgs(getJavaArgs(scanner));
          break;

        case 14:
          configurationModel.setServerPackSuffix(getServerPackSuffix(scanner));
          break;

        case 15:
          saveConfiguration(scanner, configurationModel);
          break;

        case 16:
          CONFIGURATIONHANDLER.printConfigurationModel(configurationModel);
          break;

        case 17:
          checkConfig(configurationModel);
          break;

        case 0:
          printToFileAndConsole("Exiting Configuration Editor.");
          break;
      }

    } while (selection != 0);
  }

  /**
   * Check the configuration model for errors. If any errors are encountered, they are written to
   * the console as well as the config-editor log.
   *
   * @param configurationModel The ConfigurationModel to check.
   * @author Griefed
   */
  private void checkConfig(@NotNull ConfigurationModel configurationModel) {
    List<String> errors = new ArrayList<>(10);
    CONFIGURATIONHANDLER.checkConfiguration(configurationModel, errors, false);
    if (!errors.isEmpty()) {
      printToFileAndConsole("Encountered the following errors:");
      for (int i = 0; i < errors.size(); i++) {
        printToFileAndConsole("  (" + i + "): " + errors.get(i));
      }
    } else {
      printToFileAndConsole("No errors encountered.");
    }
  }

  /**
   * Print the text-menu so the user may decide what they would like to do next.
   *
   * @author Griefed
   */
  private void printEditMenu() {
    printToFileAndConsole();
    printToFileAndConsole("What would you like to edit?");
    printToFileAndConsole("(1)  : Path to the modpack directory");
    printToFileAndConsole("(2)  : List of clientside-only mods");
    printToFileAndConsole("(3)  : List of files and/or folders to include in the server pack");
    printToFileAndConsole("(4)  : Path to a custom server-icon.png");
    printToFileAndConsole("(5)  : Path to a custom server.properties");
    printToFileAndConsole(
        "(6)  : Whether to install the modloader server during server pack generation");
    printToFileAndConsole("(7)  : Minecraft version");
    printToFileAndConsole("(8)  : Modloader");
    printToFileAndConsole("(9)  : Modloader version");
    printToFileAndConsole("(10) : Path to Java installation for modloader server installation");
    printToFileAndConsole("     - Only relevant if you set (6) to yes/true");
    printToFileAndConsole("(11) : Whether to include a server-icon.png in the server pack");
    printToFileAndConsole("     - Only relevant if you set (4) to a valid path");
    printToFileAndConsole("(12) : Whether to include a server.properties in the server pack");
    printToFileAndConsole("     - Only relevant if you set (5) to a valid path");
    printToFileAndConsole("(13) : Whether to create a ZIP-archive of the generated server pack");
    printToFileAndConsole(
        "(14) : JVM flags / Java Args to run the server of the generated server pack with");
    printToFileAndConsole("     - These will be used by the start scripts");
    printToFileAndConsole("(15) : Server pack suffix");
    printToFileAndConsole("(16) : Save config to file");
    printToFileAndConsole("(17) : Print current config");
    printToFileAndConsole("(18) : Check configuration");
    printToFileAndConsole("(0)  : Exit");
    printToFileAndConsole(
        "---------------------------------------------------------------------------------");
    printToFileAndConsole("Enter the number of your selection: ", false);
  }

  /**
   * Walk the user through the generation of a new ServerPackCreator configuration file by asking
   * them for input, step-by-step, regarding their modpack. At the end of this method a fully
   * configured serverpackcreator.conf file is saved and any previously existing configuration file
   * replaced by the new one.<br> After every input, said input is displayed to the user, and
   * they're asked whether they are satisfied with said input. The user can then decide whether they
   * would like to restart the entry of the field they just configured, or agree and move to the
   * next one.<br> At the end of this method, the user will have a newly configured and created
   * configuration file for ServerPackCreator.<br>
   * <br>
   * Most user-input is checked after entry to ensure the configuration is already in
   * working-condition after completion of this method.
   *
   * @param scanner Used for reading the users input.
   * @author whitebear60
   * @author Griefed
   */
  public void createConfigurationFile(@NotNull Scanner scanner) {

    ConfigurationModel configurationModel = new ConfigurationModel();

    do {
      // -----------------------------------------------------------------MODPACK DIRECTORY---------

      configurationModel.setModpackDir(getModpackDirectory(scanner));

      // --------------------------------------------------------------CLIENTSIDE-ONLY MODS---------

      getClientSideMods(scanner, configurationModel.getClientMods());

      // ---------------------------------------DIRECTORIES OR FILES TO COPY TO SERVER PACK---------

      getDirsAndFilesToCopy(scanner, configurationModel.getModpackDir(),
                            configurationModel.getCopyDirs());

      // ------------------------------------------------PATH TO THE CUSTOM SERVER-ICON.PNG---------

      configurationModel.setServerIconPath(getServerIcon(scanner));

      // -----------------------------------------------PATH TO THE CUSTOM SERVER.PROPERTIES--------

      configurationModel.setServerPropertiesPath(getServerProperties(scanner));

      // ----------------------------------WHETHER TO INCLUDE MODLOADER SERVER INSTALLATION---------

      configurationModel.setIncludeServerInstallation(installModloaderServer(scanner));

      // ----------------------------------------------------MINECRAFT VERSION MODPACK USES---------

      configurationModel.setMinecraftVersion(getMinecraftVersion(scanner));

      // ------------------------------------------------------------MODLOADER MODPACK USES---------

      configurationModel.setModLoader(getModloader(scanner));

      // -------------------------------------------------VERSION OF MODLOADER MODPACK USES---------

      configurationModel.setModLoaderVersion(
          getModloaderVersion(scanner, configurationModel.getMinecraftVersion(),
                              configurationModel.getModLoader()));

      // ---------------------------------WHETHER TO INCLUDE SERVER-ICON.PNG IN SERVER PACK---------

      if (new File(configurationModel.getServerIconPath()).isFile()) {
        configurationModel.setIncludeServerIcon(includeServerIcon(scanner));
      } else {
        printToFileAndConsole(
            "You did not specify a server-icon. Setting server-icon inclusion to false.");
      }

      // -------------------------------WHETHER TO INCLUDE SERVER.PROPERTIES IN SERVER PACK---------

      if (new File(configurationModel.getServerPropertiesPath()).isFile()) {
        configurationModel.setIncludeServerProperties(includeServerProperties(scanner));
      } else {
        printToFileAndConsole(
            "You did not specify the server.properties. Setting server.properties inclusion to false.");
      }

      // -------------------------WHETHER TO INCLUDE CREATION OF ZIP-ARCHIVE OF SERVER PACK---------

      configurationModel.setIncludeZipCreation(includeZipCreation(scanner));

      // ----------------------------------------------JAVA ARGS TO EXECUTE THE SERVER WITH---------

      configurationModel.setJavaArgs(getJavaArgs(scanner));

      // ---------------------------------------------------SUFFIX TO APPEND TO SERVER PACK---------

      configurationModel.setServerPackSuffix(getServerPackSuffix(scanner));

      // ---------------------------------------------------PRINT CONFIG TO CONSOLE AND LOG---------
      CONFIGURATIONHANDLER.printConfigurationModel(configurationModel);

      printToFileAndConsole("Are you satisfied with this config?");
      printToFileAndConsole("Answer: ", false);

    } while (!UTILITIES.BooleanUtils().readBoolean(scanner));

    // ----------------------------------------------------------------CHECK CONFIGURATION----------

    printToFileAndConsole("Would you like to check your new configuration for errors?");
    printToFileAndConsole("Answer: ", false);
    if (UTILITIES.BooleanUtils().readBoolean(scanner)) {
      checkConfig(configurationModel);
    }

    // ----------------------------------------------------------------WRITE CONFIG TO FILE---------

    saveConfiguration(scanner, configurationModel);
  }

  /**
   * Acquire the path to the modpack directory from the user.
   *
   * @param scanner Used for reading the users input.
   * @return The path to the modpack directory.
   * @author Griefed
   */
  private @NotNull String getModpackDirectory(@NotNull Scanner scanner) {
    String modpackDir;

    printToFileAndConsole(
        "Please enter your modpack path. This path can be relative to ServerPackCreator, or absolute.");
    printToFileAndConsole("Example: \"./Some Modpack\" or \"C:/Minecraft/Some Modpack\"");

    do {

      do {
        printToFileAndConsole("Path to modpack directory: ", false);
        modpackDir = getNextLine(scanner);
      } while (!CONFIGURATIONHANDLER.checkModpackDir(modpackDir));

      printToFileAndConsole("You entered: " + modpackDir);
      printToFileAndConsole("Are you satisfied with this modpack directory?");
      printToFileAndConsole("Answer: ", false);

    } while (!UTILITIES.BooleanUtils().readBoolean(scanner));

    printToFileAndConsole("You entered: " + modpackDir);
    printToFileAndConsole();

    return modpackDir;
  }

  /**
   * Acquire a list of clientside-only modslist from the user.
   *
   * @param clientMods List of clientside-only mods to either overwrite or edit.
   * @author Griefed
   */
  private void getClientSideMods(@NotNull Scanner scanner,
                                 @NotNull List<String> clientMods) {

    int selection = 2;

    do {
      if (!clientMods.isEmpty()) {

        printToFileAndConsole("You have entries in your list of clientside-only mods.");
        printToFileAndConsole("Would you like to edit (1) that list, or start over (2)?");
        selection = getDecision(scanner, 1, 2);

      }

      switch (selection) {
        case 1:
          editList(scanner, clientMods);
          break;

        case 2:
          clientMods.clear();
          clientMods.addAll(newClientModsList());
          break;
      }

      printToFileAndConsole("Are you satisfied with this list?");
      printToFileAndConsole("Answer: ", false);

    } while (!UTILITIES.BooleanUtils().readBoolean(scanner));

    printToFileAndConsole("Your list:");
    UTILITIES.ListUtils().printListToConsoleChunked(clientMods, 5, "    ", false);
    printToFileAndConsole();

  }

  /**
   * Get a decision from the user, between and including the min and max values specified.
   *
   * @param scanner Used for reading the users input.
   * @param min     The minimum value allowed to pick.
   * @param max     The maximum value allowed to pick.
   * @return The users decision.
   * @author Griefed
   */
  private int getDecision(@NotNull Scanner scanner,
                          int min,
                          int max) {
    int selection;
    do {
      try {
        selection = Integer.parseInt(getNextLine(scanner));
      } catch (Exception ex) {
        printToFileAndConsole(
            "Not a valid number. Please pick between " + min + " and " + max + ".");
        selection = min - 1;
      }
    } while (selection < min && selection > max);
    return selection;
  }

  /**
   * Edit entries in a list.
   *
   * @param scanner Used for reading the users input.
   * @param list    The list in which to edit its entries.
   * @author Griefed
   */
  private void editList(@NotNull Scanner scanner,
                        @NotNull List<String> list) {
    printToFileAndConsole("Available entries in list:");
    for (int i = 0; i < list.size(); i++) {
      printToFileAndConsole("(" + i + ") : " + list.get(i));
    }

    int max = list.size() - 1;

    do {

      printToFileAndConsole("Which entry would you like to edit?");
      printToFileAndConsole("Enter a number between 0 and " + max);

      int selection = getDecision(scanner, 0, max);

      printToFileAndConsole("(" + selection + ") of " + max + " = " + list.get(selection));

      printToFileAndConsole("Do you want to edit(1) that entry, or delete(2) it?");

      int decision;
      decision = getDecision(scanner, 1, 2);

      switch (decision) {
        case 1:
          printToFileAndConsole("Enter the new text for this entry:", false);
          list.set(selection, getNextLine(scanner));
          break;

        case 2:
          printToFileAndConsole("Deleted " + list.remove(selection) + " from the list.");
          break;
      }

      for (int i = 0; i < list.size(); i++) {
        printToFileAndConsole("(" + i + ") : " + list.get(i));
      }

      printToFileAndConsole(
          "--------------------------------------------------------------------------------");
      printToFileAndConsole("Are you satisfied with this list?");
      printToFileAndConsole("Answer: ", false);

    } while (!UTILITIES.BooleanUtils().readBoolean(scanner));

    UTILITIES.ListUtils().printListToConsoleChunked(list, 5, "    ", false);
    printToFileAndConsole("List successfully edited.");
  }

  /**
   * Create a new list of clientside-only mods.
   *
   * @return A list of clientside-only mods, as per user input.
   * @author Griefed
   */
  private @NotNull List<String> newClientModsList() {

    printToFileAndConsole(
        "Enter filenames of clientside-only mods, one per line. When you are done, simply press enter with empty input.");
    printToFileAndConsole(
        "If you do not specify any clientside-only mods, a default list will be used.");

    List<String> clientMods = newCustomList();

    if (clientMods.isEmpty()) {
      clientMods = APPLICATIONPROPERTIES.getListFallbackMods();

      printToFileAndConsole("No clientside-only mods specified. Using fallback list.");

      for (String mod : clientMods) {
        printToFileAndConsole("    " + mod);
      }
    }
    return clientMods;
  }

  /**
   * Create a new list of filled with user inputs.
   *
   * @return A list of clientside-only mods, as per user input.
   * @author Griefed
   */
  private @NotNull List<String> newCustomList() {

    List<String> custom = new ArrayList<>(UTILITIES.ListUtils().readStringList());

    printToFileAndConsole("You entered:");
    for (int i = 0; i < custom.size(); i++) {
      printToFileAndConsole("  " + i + 1 + ". " + custom.get(i));
    }

    return custom;
  }

  /**
   * Acquire a list of files and directories to include in the server pack from the user.
   *
   * @param modpackDir The path to the modpack directory.
   * @author Griefed
   */
  private void getDirsAndFilesToCopy(@NotNull Scanner scanner,
                                     @NotNull String modpackDir,
                                     @NotNull List<String> copyDirs) {

    printToFileAndConsole(
        "Which directories or files should be copied to the server pack? These are folder- or filenames inside your modpack directory or explicit source/file;destination/file-combinations.");

    listModpackFilesAndFolders(modpackDir);

    int selection = 2;

    do {
      if (!copyDirs.isEmpty()) {

        printToFileAndConsole(
            "You have entries in your list of files/folder to include in the server pack.");
        printToFileAndConsole();
        UTILITIES.ListUtils().printListToConsoleChunked(copyDirs, 1, "    ", true);
        printToFileAndConsole();
        printToFileAndConsole(
            "Would you like to edit (1) that list, start over (2), or list the files in your modpack directory (3) again?");
        selection = getDecision(scanner, 1, 3);

      }

      switch (selection) {
        case 1:
          editList(scanner, copyDirs);
          break;

        case 2:
          copyDirs.clear();
          copyDirs.addAll(newCustomList());
          break;

        case 3:
          listModpackFilesAndFolders(modpackDir);
          break;
      }

      printToFileAndConsole("Are you satisfied with this list?");
      printToFileAndConsole("Answer: ", false);

    } while (!UTILITIES.BooleanUtils().readBoolean(scanner));

    printToFileAndConsole("Your list:");
    for (int i = 0; i < copyDirs.size(); i++) {
      printToFileAndConsole("  " + i + 1 + ". " + copyDirs.get(i));
    }

    printToFileAndConsole();
  }

  /**
   * List all files and folders in the provided modpack-directory.
   *
   * @param modpackDir Path to the modpack-directory of which to list the containing files and
   *                   folders.
   * @author Griefed
   */
  private void listModpackFilesAndFolders(@NotNull String modpackDir) {
    try {
      List<File> dirList = new ArrayList<>(FileUtils.listFiles(new File(modpackDir), null, false));

      if (!dirList.isEmpty()) {
        printToFileAndConsole(
            "The modpack directory you specified contains the following directories:");
        for (int i = 0; i < dirList.size(); i++) {
          printToFileAndConsole("  " + (i + 1) + ". " + dirList.get(i));
        }
      } else {
        printToFileAndConsole("No files or directories found in " + modpackDir);
      }
    } catch (Exception ignored) {

    }
  }

  /**
   * Acquire the path to the server-icon to include in the server pack from the user.
   *
   * @param scanner Used for reading the users input.
   * @return The path to the server-icon to include in the server pack, as per the users input.
   * @author Griefed
   */
  private @NotNull String getServerIcon(@NotNull Scanner scanner) {
    String serverIconPath;
    printToFileAndConsole(
        "Enter the path to your custom server-icon.png-file, if you want to include one. Leave blank if you are fine with the default.");

    do {

      do {
        printToFileAndConsole("Path to your server-icon.png: ", false);
        serverIconPath = getNextLine(scanner);

      } while (!CONFIGURATIONHANDLER.checkIconAndProperties(serverIconPath));

      printToFileAndConsole("You entered: " + serverIconPath);

      printToFileAndConsole("Are you satisfied with this setting?");
      printToFileAndConsole("Answer: ", false);

    } while (!UTILITIES.BooleanUtils().readBoolean(scanner));

    printToFileAndConsole("You entered: " + serverIconPath);
    printToFileAndConsole();

    return serverIconPath;
  }

  /**
   * Acquire the path to the server-properties to include in the server pack from the user.
   *
   * @param scanner Used for reading the users input.
   * @return The path to the server-properties to include in the server pack, as per the users
   * input.
   * @author Griefed
   */
  private @NotNull String getServerProperties(@NotNull Scanner scanner) {
    String serverPropertiesPath;
    printToFileAndConsole(
        "Enter the path to your custom server.properties-file, if you want to include one. Leave blank if you are fine with the default.");

    do {

      do {

        printToFileAndConsole("Path to your server.properties: ", false);
        serverPropertiesPath = getNextLine(scanner);
      } while (!CONFIGURATIONHANDLER.checkIconAndProperties(serverPropertiesPath));

      printToFileAndConsole("You entered: " + serverPropertiesPath);

      printToFileAndConsole("Are you satisfied with this setting?");
      printToFileAndConsole("Answer: ", false);

    } while (!UTILITIES.BooleanUtils().readBoolean(scanner));

    printToFileAndConsole("You entered: " + serverPropertiesPath);
    printToFileAndConsole();

    return serverPropertiesPath;
  }

  /**
   * Get the users decision on whether they want to include the modloader server installation.
   *
   * @return {@code true} if the user wants the modloader server to be installed.
   * @author Griefed
   */
  private boolean installModloaderServer(@NotNull Scanner scanner) {
    boolean includeServerInstallation;
    printToFileAndConsole(
        "Do you want ServerPackCreator to install the modloader server for your server pack? Must be true or false.");

    printToFileAndConsole("Include modloader server installation: ", false);
    includeServerInstallation = UTILITIES.BooleanUtils().readBoolean(scanner);

    printToFileAndConsole("You entered: " + includeServerInstallation);
    return includeServerInstallation;
  }

  /**
   * Get the users modpacks Minecraft version.
   *
   * @param scanner Used for reading the users input.
   * @return The Minecraft version the users modpack uses, as per the users input.
   * @author Griefed
   */
  private @NotNull String getMinecraftVersion(@NotNull Scanner scanner) {
    String minecraftVersion;
    printToFileAndConsole("Which version of Minecraft does your modpack use?");

    do {
      printToFileAndConsole("Minecraft version: ", false);
      minecraftVersion = getNextLine(scanner);

    } while (!VERSIONMETA.minecraft().checkMinecraftVersion(minecraftVersion));

    printToFileAndConsole("You entered: " + minecraftVersion);
    printToFileAndConsole();

    return minecraftVersion;
  }

  /**
   * Get the users modpacks modloader.
   *
   * @param scanner Used for reading the users input.
   * @return The modloader the users modpack uses, as per the users input.
   * @author Griefed
   */
  private @NotNull String getModloader(@NotNull Scanner scanner) {
    String modLoader;
    printToFileAndConsole("What modloader does your modpack use?");

    do {
      printToFileAndConsole("Modloader: ", false);
      modLoader = getNextLine(scanner);

    } while (!CONFIGURATIONHANDLER.checkModloader(modLoader));

    modLoader = CONFIGURATIONHANDLER.getModLoaderCase(modLoader);

    printToFileAndConsole("You entered: " + modLoader);
    printToFileAndConsole();

    return modLoader;
  }

  /**
   * Get the users modpack modloader version.
   *
   * @param scanner   Used for reading the users input.
   * @param modLoader The modloader the users modpack uses.
   * @return The modloader version the users modpack uses, as per the users input.
   * @author Griefed
   */
  private @NotNull String getModloaderVersion(@NotNull Scanner scanner,
                                              @NotNull String minecraftVersion,
                                              @NotNull String modLoader) {
    String modLoaderVersion;
    printToFileAndConsole("What version of " + modLoader + " does your modpack use?");

    do {
      printToFileAndConsole("Modloader version: ", false);
      modLoaderVersion = getNextLine(scanner);

    } while (!CONFIGURATIONHANDLER.checkModloaderVersion(
        modLoader, modLoaderVersion, minecraftVersion));

    printToFileAndConsole("You entered: " + modLoaderVersion);
    printToFileAndConsole();

    return modLoaderVersion;
  }

  /**
   * Get the users decision on whether they want to include the server-icon.
   *
   * @return {@code true} if the user wants the server-icon to be included.
   * @author Griefed
   */
  private boolean includeServerIcon(@NotNull Scanner scanner) {
    boolean includeServerIcon;
    printToFileAndConsole(
        "Do you want ServerPackCreator to include a server-icon in your server pack? Must be true or false.");

    printToFileAndConsole("Include server-icon.png: ", false);
    includeServerIcon = UTILITIES.BooleanUtils().readBoolean(scanner);

    printToFileAndConsole("You entered: " + includeServerIcon);
    printToFileAndConsole();

    return includeServerIcon;
  }

  /**
   * Get the users decision on whether they want to include the server-properties.
   *
   * @return {@code true} if the user wants the server-properties to be included.
   * @author Griefed
   */
  private boolean includeServerProperties(@NotNull Scanner scanner) {
    boolean includeServerProperties;
    printToFileAndConsole(
        "Do you want ServerPackCreator to include a server.properties in your server pack? Must be true or false.");

    printToFileAndConsole("Include server.properties: ", false);
    includeServerProperties = UTILITIES.BooleanUtils().readBoolean(scanner);

    printToFileAndConsole("You entered: " + includeServerProperties);
    printToFileAndConsole();

    return includeServerProperties;
  }

  /**
   * Get the users decision on whether they want to include the ZIP-archive creation of the server
   * pack.
   *
   * @return {@code true} if the user wants a ZIP-archive of the server pack to be created.
   * @author Griefed
   */
  private boolean includeZipCreation(@NotNull Scanner scanner) {
    boolean includeZipCreation;
    printToFileAndConsole(
        "Do you want ServerPackCreator to create a ZIP-archive of your server pack? Must be true or false.");

    printToFileAndConsole("Create ZIP-archive: ", false);
    includeZipCreation = UTILITIES.BooleanUtils().readBoolean(scanner);

    printToFileAndConsole("You entered: " + includeZipCreation);
    printToFileAndConsole();

    return includeZipCreation;
  }

  /**
   * Get the Java args to be used when starting the server pack.
   *
   * @param scanner Used for reading the users input.
   * @return The Java args to be used when starting the server pack, as per the users input.
   * @author Griefed
   */
  private @NotNull String getJavaArgs(@NotNull Scanner scanner) {
    String javaArgs;
    printToFileAndConsole(
        "Specify the Java arguments, if any, to execute the server with. Can be left blank.");

    printToFileAndConsole("Java args: ", false);
    javaArgs = getNextLine(scanner);

    if (javaArgs.isEmpty()) {
      javaArgs = "";
    }

    printToFileAndConsole("Java arguments for start-scripts: " + javaArgs);
    printToFileAndConsole();

    return javaArgs;
  }

  /**
   * Get the server pack suffix to append to the server pack.
   *
   * @param scanner Used for reading the users input.
   * @return The server pack suffix to append to the server pack, as per the users input.
   * @author Griefed
   */
  private @NotNull String getServerPackSuffix(@NotNull Scanner scanner) {
    printToFileAndConsole(
        "Enter the suffix you want to append to your server pack. Can be left empty.");

    printToFileAndConsole("Server pack suffix: ", false);

    return getNextLine(scanner);
  }

  /**
   * Let the user save the created configuration to a file.
   *
   * @param scanner            Used for reading the users input.
   * @param configurationModel Configuration to save.
   * @author Griefed
   */
  private void saveConfiguration(@NotNull Scanner scanner,
                                 @NotNull ConfigurationModel configurationModel) {

    printToFileAndConsole(
        "Would you like to save this configuration as an additional, separate, configuration file?");
    if (UTILITIES.BooleanUtils().readBoolean(scanner)) {

      printToFileAndConsole(
          "Enter the name under which you want to additionally store the above configuration:");
      File customFileName = new File(UTILITIES.StringUtils().pathSecureText(getNextLine(scanner)));

      configurationModel.save(customFileName);

      printToFileAndConsole(
          "Your configuration has been saved as '" + customFileName + "'.");
      printToFileAndConsole(
          "Please note that running ServerPackCreator in CLI mode requires a valid 'serverpackcreator.conf'-file to be present.");
      printToFileAndConsole("You may load the previous configuration, saved as '" + customFileName
                                + "' and save it as 'serverpackcreator.conf'");


    } else {

      configurationModel.save(APPLICATIONPROPERTIES.defaultConfig());
      printToFileAndConsole(
          "Your configuration has been saved as 'serverpackcreator.conf'.");

    }
  }

  /**
   * Acquire user input and print that input to our config-editor log.
   *
   * @param scanner Used for reading the users input.
   * @return The text the user entered.
   */
  private @NotNull String getNextLine(@NotNull Scanner scanner) {
    String text = scanner.nextLine();
    printToFile(text, true);
    return text;
  }

  /**
   * Write/print an empty line to a file as well as console. The filename is
   * {@code logs/configurationCreator-CURRENT_DATE_CURRENT_TIME.log}.
   *
   * @author Griefed
   */
  private void printToFileAndConsole() {
    printToFileAndConsole("");
  }

  private void printToFileAndConsole(String text) {
    printToFileAndConsole(text, true);
  }

  /**
   * Write/print text to a file as well as console. The filename is
   * {@code logs/configurationCreator-CURRENT_DATE_CURRENT_TIME.log}.
   *
   * @param text The text to write/print
   * @author Griefed
   */
  private void printToFileAndConsole(@NotNull String text,
                                     boolean newLine) {
    if (newLine) {
      System.out.println(text);
    } else {
      System.out.print(text);
    }
    printToFile(text, newLine);
  }

  /**
   * Append text to our config-editor log.
   *
   * @param text    The text to append to the log.
   * @param newLine Whether to include a newline after the text.
   */
  private void printToFile(@NotNull String text,
                           boolean newLine) {
    if (logFile.exists()) {
      try {
        if (newLine) {
          FileUtils.writeStringToFile(logFile, text + "\n", StandardCharsets.UTF_8, true);
        } else {
          FileUtils.writeStringToFile(logFile, text, StandardCharsets.UTF_8, true);
        }
      } catch (IOException ex) {
        LOG.error("Could not write to logfile " + logFile.getName(), ex);
      }
    } else {
      LOG.error("Logfile " + logFile.getName() + " does not exist.");
    }
  }

  private void checkLogFile() {
    List<File> files = new ArrayList<>(FileUtils.listFiles(new File("logs"), null, false));

    for (File file : files) {
      if (file.getName().contains("configurationCreator")) {
        try {
          FileUtils.moveFile(file, new File("logs/archive/" + file.getName()));
        } catch (IOException ex) {
          LOG.error("Could not move " + file.getName() + " to archive.", ex);
        }
      }
    }

    if (!logFile.exists()) {
      try {
        //noinspection ResultOfMethodCallIgnored
        logFile.createNewFile();
      } catch (IOException ex) {
        LOG.error("Could not create logfile " + logFile.getName(), ex);
      }
    }
  }
}
