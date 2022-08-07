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
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import org.apache.commons.io.FileUtils;

/**
 * Create and edit a serverpackcreator.conf file via CLI. Allows loading of a custom file for
 * editing purposes and saving any config file under a different name.
 *
 * @author Griefed
 */
public class ConfigurationEditor {

  private final ConfigurationHandler CONFIGURATIONHANDLER;
  private final ApplicationProperties APPLICATIONPROPERTIES;
  private final VersionMeta VERSIONMETA;
  private final Utilities UTILITIES;
  private final ConfigUtilities CONFIGUTILITIES;

  public ConfigurationEditor(
      ConfigurationHandler injectedConfigurationHandler,
      ApplicationProperties injectedApplicationProperties,
      Utilities injectedUtilities,
      VersionMeta injectedVersionMeta,
      ConfigUtilities injectedConfigUtilities) {

    this.APPLICATIONPROPERTIES = injectedApplicationProperties;
    this.VERSIONMETA = injectedVersionMeta;
    this.UTILITIES = injectedUtilities;
    this.CONFIGUTILITIES = injectedConfigUtilities;
    this.CONFIGURATIONHANDLER = injectedConfigurationHandler;
  }

  /**
   * Present the user with a menu, asking them what they would like to do.
   *
   * @author Griefed
   */
  public void continuedRunOptions() {
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
          System.out.println("Exiting Configuration Editor.");
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
    System.out.println();
    System.out.println("What would you like to do?");
    System.out.println("(1) : Create new configuration");
    System.out.println("(2) : Load and edit configuration");
    System.out.println("(0) : Exit");
    System.out.println("-------------------------------------------");
    System.out.print("Enter the number of your selection: ");
  }

  /**
   * Allow the user to load and edit an existing configuration.
   *
   * @param scanner Used for reading the users input.
   * @author Griefed
   */
  private void loadAndEdit(Scanner scanner) {

    ConfigurationModel configurationModel = new ConfigurationModel();
    String fileName;
    boolean configLoaded = false;

    do {
      try {

        System.out.println("Please enter the path to the configuration file you want to edit:");
        fileName = scanner.nextLine();
        configurationModel = new ConfigurationModel(UTILITIES, new File(fileName));
        configLoaded = true;

      } catch (FileNotFoundException | NoFormatFoundException e) {
        System.out.println("The specified file could not be loaded. " + e.getMessage());
      }

    } while (!configLoaded);

    System.out.println("Configuration successfully loaded.");
    System.out.println();

    int selection;

    do {

      CONFIGUTILITIES.printConfigurationModel(configurationModel);
      printEditMenu();

      selection = getDecision(scanner, 0, 17);

      switch (selection) {
        case 1:
          configurationModel.setModpackDir(getModpackDirectory(scanner));
          break;

        case 2:
          configurationModel.setClientMods(
              getClientSideMods(scanner, configurationModel.getClientMods()));
          break;

        case 3:
          configurationModel.setCopyDirs(
              getDirsAndFilesToCopy(scanner, configurationModel.getModpackDir(),
                  configurationModel.getCopyDirs()));
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
          configurationModel.setJavaPath(getJavaPath(scanner));
          break;

        case 11:
          configurationModel.setIncludeServerIcon(includeServerIcon(scanner));
          break;

        case 12:
          configurationModel.setIncludeServerProperties(includeServerProperties(scanner));
          break;

        case 13:
          configurationModel.setIncludeZipCreation(includeZipCreation(scanner));
          break;

        case 14:
          configurationModel.setJavaArgs(getJavaArgs(scanner));
          break;

        case 15:
          configurationModel.setServerPackSuffix(getServerPackSuffix(scanner));
          break;

        case 16:
          saveConfiguration(scanner, configurationModel);
          break;

        case 17:
          CONFIGUTILITIES.printConfigurationModel(configurationModel);
          break;

        case 0:
          System.out.println("Exiting Configuration Editor.");
          break;
      }

    } while (selection != 0);
  }

  /**
   * Print the text-menu so the user may decide what they would like to do next.
   *
   * @author Griefed
   */
  private void printEditMenu() {
    System.out.println();
    System.out.println("What would you like to edit?");
    System.out.println("(1)  : Path to the modpack directory");
    System.out.println("(2)  : List of clientside-only mods");
    System.out.println("(3)  : List of files and/or folders to include in the server pack");
    System.out.println("(4)  : Path to a custom server-icon.png");
    System.out.println("(5)  : Path to a custom server.properties");
    System.out.println(
        "(6)  : Whether to install the modloader server during server pack generation");
    System.out.println("(7)  : Minecraft version");
    System.out.println("(8)  : Modloader");
    System.out.println("(9)  : Modloader version");
    System.out.println("(10) : Path to Java installation for modloader server installation");
    System.out.println("     - Only relevant if you set (6) to yes/true");
    System.out.println("(11) : Whether to include a server-icon.png in the server pack");
    System.out.println("     - Only relevant if you set (4) to a valid path");
    System.out.println("(12) : Whether to include a server.properties in the server pack");
    System.out.println("     - Only relevant if you set (5) to a valid path");
    System.out.println("(13) : Whether to create a ZIP-archive of the generated server pack");
    System.out.println(
        "(14) : JVM flags / Java Args to run the server of the generated server pack with");
    System.out.println("     - These will be used by the start scripts");
    System.out.println("(15) : Server pack suffix");
    System.out.println("(16) : Save config to file");
    System.out.println("(17) : Print current config");
    System.out.println("(0)  : Exit");
    System.out.println(
        "---------------------------------------------------------------------------------");
    System.out.print("Enter the number of your selection: ");
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
  public void createConfigurationFile(Scanner scanner) {

    List<String> clientMods, copyDirs;

    clientMods = new ArrayList<>(100);
    copyDirs = new ArrayList<>(100);

    boolean includeServerInstallation,
        includeServerIcon = false,
        includeServerProperties = false,
        includeZipCreation;

    String modpackDir,
        javaPath = "",
        minecraftVersion,
        modLoader,
        modLoaderVersion,
        serverIconPath,
        serverPropertiesPath,
        javaArgs,
        serverPackSuffix;

    do {
      // -----------------------------------------------------------------MODPACK DIRECTORY---------

      modpackDir = getModpackDirectory(scanner);

      // --------------------------------------------------------------CLIENTSIDE-ONLY MODS---------

      clientMods.addAll(getClientSideMods(scanner, clientMods));

      // ---------------------------------------DIRECTORIES OR FILES TO COPY TO SERVER PACK---------

      copyDirs.addAll(getDirsAndFilesToCopy(scanner, modpackDir, copyDirs));

      // ------------------------------------------------PATH TO THE CUSTOM SERVER-ICON.PNG---------

      serverIconPath = getServerIcon(scanner);

      // -----------------------------------------------PATH TO THE CUSTOM SERVER.PROPERTIES--------

      serverPropertiesPath = getServerProperties(scanner);

      // ----------------------------------WHETHER TO INCLUDE MODLOADER SERVER INSTALLATION---------

      includeServerInstallation = installModloaderServer(scanner);

      // ----------------------------------------------------MINECRAFT VERSION MODPACK USES---------

      minecraftVersion = getMinecraftVersion(scanner);

      // ------------------------------------------------------------MODLOADER MODPACK USES---------

      modLoader = getModloader(scanner);

      // -------------------------------------------------VERSION OF MODLOADER MODPACK USES---------

      modLoaderVersion = getModloaderVersion(scanner, minecraftVersion, modLoader);

      // ---------------------------------------------------------PATH TO JAVA INSTALLATION---------

      if (includeServerInstallation) {
        javaPath = getJavaPath(scanner);
      } else {
        System.out.println(
            "Skipping Java installation path acquisition, as the modloader server installation is deactivated as per your input.");
      }

      // ---------------------------------WHETHER TO INCLUDE SERVER-ICON.PNG IN SERVER PACK---------

      if (new File(serverIconPath).isFile()) {
        includeServerIcon = includeServerIcon(scanner);
      } else {
        System.out.println(
            "You did not specify a server-icon. Setting server-icon inclusion to false.");
      }

      // -------------------------------WHETHER TO INCLUDE SERVER.PROPERTIES IN SERVER PACK---------

      if (new File(serverPropertiesPath).isFile()) {
        includeServerProperties = includeServerProperties(scanner);
      } else {
        System.out.println(
            "You did not specify the server.properties. Setting server.properties inclusion to false.");
      }

      // -------------------------WHETHER TO INCLUDE CREATION OF ZIP-ARCHIVE OF SERVER PACK---------

      includeZipCreation = includeZipCreation(scanner);

      // ----------------------------------------------JAVA ARGS TO EXECUTE THE SERVER WITH---------

      javaArgs = getJavaArgs(scanner);

      // ---------------------------------------------------SUFFIX TO APPEND TO SERVER PACK---------

      serverPackSuffix = getServerPackSuffix(scanner);

      // ---------------------------------------------------PRINT CONFIG TO CONSOLE AND LOG---------
      CONFIGUTILITIES.printConfigurationModel(
          modpackDir,
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

      System.out.println("Are you satisfied with this config?");
      System.out.print("Answer: ");

    } while (!UTILITIES.BooleanUtils().readBoolean(scanner));

    // ----------------------------------------------------------------WRITE CONFIG TO FILE---------

    saveConfiguration(scanner,
        modpackDir,
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
  }

  /**
   * Acquire the path to the modpack directory from the user.
   *
   * @param scanner Used for reading the users input.
   * @return The path to the modpack directory.
   * @author Griefed
   */
  private String getModpackDirectory(Scanner scanner) {
    String modpackDir;

    System.out.println(
        "Please enter your modpack path. This path can be relative to ServerPackCreator, or absolute.");
    System.out.println("Example: \"./Some Modpack\" or \"C:/Minecraft/Some Modpack\"");

    do {

      do {
        System.out.print("Path to modpack directory: ");
        modpackDir = scanner.nextLine();
      } while (!CONFIGURATIONHANDLER.checkModpackDir(modpackDir));

      System.out.println("You entered: " + modpackDir);
      System.out.println("Are you satisfied with this modpack directory?");
      System.out.print("Answer: ");

    } while (!UTILITIES.BooleanUtils().readBoolean(scanner));

    modpackDir = modpackDir.replace("\\", "/");

    System.out.println("You entered: " + modpackDir);
    System.out.println();

    return modpackDir;
  }

  /**
   * Acquire a list of clientside-only modslist from the user.
   *
   * @param clientMods List of clientside-only mods to either overwrite or edit.
   * @return A list of clientside-only mods as per the users input.
   * @author Griefed
   */
  private List<String> getClientSideMods(Scanner scanner, List<String> clientMods) {

    int selection = 2;

    do {
      if (!clientMods.isEmpty()) {

        System.out.println("You have entries in your list of clientside-only mods.");
        System.out.println("Would you like to edit (1) that list, or start over (2)?");
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

      System.out.println("Are you satisfied with this list?");
      System.out.print("Answer: ");

    } while (!UTILITIES.BooleanUtils().readBoolean(scanner));

    System.out.println("Your list:");
    UTILITIES.ListUtils().printListToConsoleChunked(clientMods, 5, "    ", false);
    System.out.println();

    return clientMods;
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
  private int getDecision(Scanner scanner, int min, int max) {
    int selection;
    do {
      try {
        selection = Integer.parseInt(scanner.nextLine());
      } catch (Exception ex) {
        System.out.println("Not a valid number. Please pick between " + min + " and " + max + ".");
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
  private void editList(Scanner scanner, List<String> list) {
    System.out.println("Available entries in list:");
    for (int i = 0; i < list.size(); i++) {
      System.out.println("(" + i + ") : " + list.get(i));
    }

    int max = list.size() - 1;

    do {

      System.out.println("Which entry would you like to edit?");
      System.out.println("Enter a number between 0 and " + max);

      int selection = getDecision(scanner, 0, max);

      System.out.println("(" + selection + ") of " + max + " = " + list.get(selection));

      System.out.println("Do you want to edit(1) that entry, or delete(2) it?");

      int decision;
      decision = getDecision(scanner, 1, 2);

      switch (decision) {
        case 1:
          System.out.print("Enter the new text for this entry:");
          list.set(selection, scanner.nextLine());
          break;

        case 2:
          System.out.println("Deleted " + list.remove(selection) + " from the list.");
          break;
      }

      for (int i = 0; i < list.size(); i++) {
        System.out.println("(" + i + ") : " + list.get(i));
      }

      System.out.println(
          "--------------------------------------------------------------------------------");
      System.out.println("Are you satisfied with this list?");
      System.out.print("Answer: ");

    } while (!UTILITIES.BooleanUtils().readBoolean(scanner));

    UTILITIES.ListUtils().printListToConsoleChunked(list, 5, "    ", false);
    System.out.println("List successfully edited.");
  }

  /**
   * Create a new list of clientside-only mods.
   *
   * @return A list of clientside-only mods, as per user input.
   * @author Griefed
   */
  private List<String> newClientModsList() {

    System.out.println(
        "Enter filenames of clientside-only mods, one per line. When you are done, simply press enter with empty input.");
    System.out.println(
        "If you do not specify any clientside-only mods, a default list will be used.");

    List<String> clientMods = newCustomList();

    if (clientMods.isEmpty()) {
      clientMods = APPLICATIONPROPERTIES.getListFallbackMods();

      System.out.println("No clientside-only mods specified. Using fallback list.");

      for (String mod : clientMods) {
        System.out.println("    " + mod);
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
  private List<String> newCustomList() {

    List<String> custom = new ArrayList<>(UTILITIES.ListUtils().readStringList());

    System.out.println("You entered:");
    for (int i = 0; i < custom.size(); i++) {
      System.out.println("  " + i + 1 + ". " + custom.get(i));
    }

    return custom;
  }

  /**
   * Acquire a list of files and directories to include in the server pack from the user.
   *
   * @param modpackDir The path to the modpack directory.
   * @return A list of files and directories to include in the server pack as per the users input.
   * @author Griefed
   */
  private List<String> getDirsAndFilesToCopy(Scanner scanner, String modpackDir,
      List<String> copyDirs) {

    System.out.println(
        "Which directories or files should be copied to the server pack? These are folder- or filenames inside your modpack directory or explicit source/file;destination/file-combinations.");

    listModpackFilesAndFolders(modpackDir);

    int selection = 2;

    do {
      if (!copyDirs.isEmpty()) {

        System.out.println(
            "You have entries in your list of files/folder to include in the server pack.");
        System.out.println();
        UTILITIES.ListUtils().printListToConsoleChunked(copyDirs,1,"    ", true);
        System.out.println();
        System.out.println(
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

      System.out.println("Are you satisfied with this list?");
      System.out.print("Answer: ");

    } while (!UTILITIES.BooleanUtils().readBoolean(scanner));

    System.out.println("Your list:");
    for (int i = 0; i < copyDirs.size(); i++) {
      System.out.println("  " + i + 1 + ". " + copyDirs.get(i));
    }

    System.out.println();
    return copyDirs;
  }

  private void listModpackFilesAndFolders(String modpackDir) {
    try {
      List<File> dirList = new ArrayList<>(FileUtils.listFiles(new File(modpackDir), null, false));

      if (dirList.size() > 0) {
        System.out.println(
            "The modpack directory you specified contains the following directories:");
        for (int i = 0; i < dirList.size(); i++) {
          System.out.println("  " + (i + 1) + ". " + dirList.get(i));
        }
      } else {
        System.out.println("No files or directories found in " + modpackDir);
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
  private String getServerIcon(Scanner scanner) {
    String serverIconPath;
    System.out.println(
        "Enter the path to your custom server-icon.png-file, if you want to include one. Leave blank if you are fine with the default.");

    do {

      do {
        System.out.print("Path to your server-icon.png: ");
        serverIconPath = scanner.nextLine();

      } while (!CONFIGURATIONHANDLER.checkIconAndProperties(serverIconPath));

      System.out.println("You entered: " + serverIconPath);

      System.out.println("Are you satisfied with this setting?");
      System.out.print("Answer: ");

    } while (!UTILITIES.BooleanUtils().readBoolean(scanner));

    serverIconPath = serverIconPath.replace("\\", "/");

    System.out.println("You entered: " + serverIconPath);
    System.out.println();

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
  private String getServerProperties(Scanner scanner) {
    String serverPropertiesPath;
    System.out.println(
        "Enter the path to your custom server.properties-file, if you want to include one. Leave blank if you are fine with the default.");

    do {

      do {

        System.out.print("Path to your server.properties: ");
        serverPropertiesPath = scanner.nextLine();
      } while (!CONFIGURATIONHANDLER.checkIconAndProperties(serverPropertiesPath));

      System.out.println("You entered: " + serverPropertiesPath);

      System.out.println("Are you satisfied with this setting?");
      System.out.print("Answer: ");

    } while (!UTILITIES.BooleanUtils().readBoolean(scanner));

    serverPropertiesPath = serverPropertiesPath.replace("\\", "/");

    System.out.println("You entered: " + serverPropertiesPath);
    System.out.println();

    return serverPropertiesPath;
  }

  /**
   * Get the users decision on whether they want to include the modloader server installation.
   *
   * @return <code>true</code> if the user wants the modloader server to be installed.
   * @author Griefed
   */
  private boolean installModloaderServer(Scanner scanner) {
    boolean includeServerInstallation;
    System.out.println(
        "Do you want ServerPackCreator to install the modloader server for your server pack? Must be true or false.");

    System.out.print("Include modloader server installation: ");
    includeServerInstallation = UTILITIES.BooleanUtils().readBoolean(scanner);

    System.out.println("You entered: " + includeServerInstallation);
    return includeServerInstallation;
  }

  /**
   * Get the users modpacks Minecraft version.
   *
   * @param scanner Used for reading the users input.
   * @return The Minecraft version the users modpack uses, as per the users input.
   * @author Griefed
   */
  private String getMinecraftVersion(Scanner scanner) {
    String minecraftVersion;
    System.out.println("Which version of Minecraft does your modpack use?");

    do {
      System.out.print("Minecraft version: ");
      minecraftVersion = scanner.nextLine();

    } while (!VERSIONMETA.minecraft().checkMinecraftVersion(minecraftVersion));

    System.out.println("You entered: " + minecraftVersion);
    System.out.println();

    return minecraftVersion;
  }

  /**
   * Get the users modpacks modloader.
   *
   * @param scanner Used for reading the users input.
   * @return The modloader the users modpack uses, as per the users input.
   * @author Griefed
   */
  private String getModloader(Scanner scanner) {
    String modLoader;
    System.out.println("What modloader does your modpack use?");

    do {
      System.out.print("Modloader: ");
      modLoader = scanner.nextLine();

    } while (!CONFIGURATIONHANDLER.checkModloader(modLoader));

    modLoader = CONFIGUTILITIES.getModLoaderCase(modLoader);

    System.out.println("You entered: " + modLoader);
    System.out.println();

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
  private String getModloaderVersion(Scanner scanner, String minecraftVersion, String modLoader) {
    String modLoaderVersion;
    System.out.println("What version of " + modLoader + " does your modpack use?");

    do {
      System.out.print("Modloader version: ");
      modLoaderVersion = scanner.nextLine();

    } while (!CONFIGURATIONHANDLER.checkModloaderVersion(
        modLoader, modLoaderVersion, minecraftVersion));

    System.out.println("You entered: " + modLoaderVersion);
    System.out.println();

    return modLoaderVersion;
  }

  /**
   * Get the path to the Java installation to use in the modloader server installation.
   *
   * @param scanner Used for reading the users input.
   * @return The path to the Java installation to use during modloader server installation, as per
   * the users input.
   * @author Griefed
   */
  private String getJavaPath(Scanner scanner) {
    String javaPath;
    System.out.println(
        "Specify the path to your Java installation. Must end with \"java\" on Linux, or \"java.exe\" on Windows.");
    System.out.println(
        "If you leave this empty, ServerPackCreator will try to determine the path for you.");
    System.out.println(
        "Example Linux: /usr/bin/java | Example Windows: C:/Program Files/AdoptOpenJDK/jdk-8.0.275.1-hotspot/jre/bin/java.exe");

    System.out.print("Path to your Java installation: ");

    javaPath = CONFIGURATIONHANDLER.getJavaPath(scanner.nextLine());

    System.out.println("Automatically acquired path to Java installation: " + javaPath);
    System.out.println();

    return javaPath;
  }

  /**
   * Get the users decision on whether they want to include the server-icon.
   *
   * @return <code>true</code> if the user wants the server-icon to be included.
   * @author Griefed
   */
  private boolean includeServerIcon(Scanner scanner) {
    boolean includeServerIcon;
    System.out.println(
        "Do you want ServerPackCreator to include a server-icon in your server pack? Must be true or false.");

    System.out.print("Include server-icon.png: ");
    includeServerIcon = UTILITIES.BooleanUtils().readBoolean(scanner);

    System.out.println("You entered: " + includeServerIcon);
    System.out.println();

    return includeServerIcon;
  }

  /**
   * Get the users decision on whether they want to include the server-properties.
   *
   * @return <code>true</code> if the user wants the server-properties to be included.
   * @author Griefed
   */
  private boolean includeServerProperties(Scanner scanner) {
    boolean includeServerProperties;
    System.out.println(
        "Do you want ServerPackCreator to include a server.properties in your server pack? Must be true or false.");

    System.out.print("Include server.properties: ");
    includeServerProperties = UTILITIES.BooleanUtils().readBoolean(scanner);

    System.out.println("You entered: " + includeServerProperties);
    System.out.println();

    return includeServerProperties;
  }

  /**
   * Get the users decision on whether they want to include the ZIP-archive creation of the server
   * pack.
   *
   * @return <code>true</code> if the user wants a ZIP-archive of the server pack to be created.
   * @author Griefed
   */
  private boolean includeZipCreation(Scanner scanner) {
    boolean includeZipCreation;
    System.out.println(
        "Do you want ServerPackCreator to create a ZIP-archive of your server pack? Must be true or false.");

    System.out.print("Create ZIP-archive: ");
    includeZipCreation = UTILITIES.BooleanUtils().readBoolean(scanner);

    System.out.println("You entered: " + includeZipCreation);
    System.out.println();

    return includeZipCreation;
  }

  /**
   * Get the Java args to be used when starting the server pack.
   *
   * @param scanner Used for reading the users input.
   * @return The Java args to be used when starting the server pack, as per the users input.
   * @author Griefed
   */
  private String getJavaArgs(Scanner scanner) {
    String javaArgs;
    System.out.println(
        "Specify the Java arguments, if any, to execute the server with. Can be left blank.");

    System.out.print("Java args: ");
    javaArgs = scanner.nextLine();

    if (javaArgs.isEmpty()) {
      javaArgs = "";
    }

    System.out.println("Java arguments for start-scripts: " + javaArgs);
    System.out.println();

    return javaArgs;
  }

  /**
   * Get the server pack suffix to append to the server pack.
   *
   * @param scanner Used for reading the users input.
   * @return The server pack suffix to append to the server pack, as per the users input.
   * @author Griefed
   */
  private String getServerPackSuffix(Scanner scanner) {
    System.out.println(
        "Enter the suffix you want to append to your server pack. Can be left empty.");

    System.out.print("Server pack suffix: ");

    return scanner.nextLine();
  }

  /**
   * Let the user save the created configuration to a file.
   *
   * @param scanner                   Used for reading the users input.
   * @param modpackDir                Path to the modpack directory.
   * @param clientMods                List of clientside-only mods.
   * @param copyDirs                  List of files and/or folders to include in the server pack.
   * @param includeServerInstallation Whether to install the modloader server during server pack
   *                                  generation.
   * @param javaPath                  The path to the Java installation used for modloader
   *                                  installation during server pack generation.
   * @param minecraftVersion          The Minecraft version of the modpack.
   * @param modLoader                 The modloader used by the modpack.
   * @param modLoaderVersion          The modloader version used by the modpack.
   * @param includeServerIcon         Whether to include a server-icon.png in the server pack.
   * @param includeServerProperties   Whether to include a server-properties in the server pack.
   * @param includeZipCreation        Whether to create a ZIP-archive of the server pack.
   * @param javaArgs                  JVM flags / Java Args to start the server with.
   * @param serverPackSuffix          Suffix to append to the name of the server pack to be
   *                                  generated.
   * @param serverIconPath            Path to the custom server-icon.png
   * @param serverPropertiesPath      Path to the custom server.properties.
   * @author Griefed
   */
  private void saveConfiguration(Scanner scanner,
      String modpackDir,
      List<String> clientMods,
      List<String> copyDirs,
      boolean includeServerInstallation,
      String javaPath,
      String minecraftVersion,
      String modLoader,
      String modLoaderVersion,
      boolean includeServerIcon,
      boolean includeServerProperties,
      boolean includeZipCreation,
      String javaArgs,
      String serverPackSuffix,
      String serverIconPath,
      String serverPropertiesPath) {

    System.out.println(
        "Would you like to save this configuration as an additional, separate, configuration file?");
    if (UTILITIES.BooleanUtils().readBoolean(scanner)) {

      System.out.println(
          "Enter the name under which you want to additionally store the above configuration:");
      File customFileName = new File(UTILITIES.StringUtils().pathSecureText(scanner.nextLine()));

      if (CONFIGUTILITIES.writeConfigToFile(
          modpackDir,
          clientMods,
          copyDirs,
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
          customFileName)) {

        System.out.println(
            "Your configuration has been saved as '" + customFileName + "'.");
        System.out.println(
            "Please note that running ServerPackCreator in CLI mode requires a valid 'serverpackcreator.conf'-file to be present.");
        System.out.println("You may load the previous configuration, saved as '" + customFileName
            + "' and save it as 'serverpackcreator.conf'");
      }

    } else {
      if (CONFIGUTILITIES.writeConfigToFile(
          modpackDir,
          clientMods,
          copyDirs,
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
          APPLICATIONPROPERTIES.DEFAULT_CONFIG())) {

        System.out.println(
            "Your configuration has been saved as 'serverpackcreator.conf'.");
      }
    }
  }

  /**
   * Let the user save the created configuration to a file.
   *
   * @param scanner            Used for reading the users input.
   * @param configurationModel Contains the created/changed/loaded configuration to save.
   * @author Griefed
   */
  private void saveConfiguration(Scanner scanner, ConfigurationModel configurationModel) {
    System.out.println(
        "Would you like to save this configuration under a different file name than 'serverpackcreator.conf'?");

    if (UTILITIES.BooleanUtils().readBoolean(scanner)) {

      System.out.println(
          "Enter the name under which you want to additionally store the above configuration:");
      File customFileName = new File(UTILITIES.StringUtils().pathSecureText(scanner.nextLine()));

      if (CONFIGUTILITIES.writeConfigToFile(
          configurationModel,
          customFileName)) {

        System.out.println(
            "Your configuration has been saved as '" + customFileName + "'.");
        System.out.println(
            "Please note that running ServerPackCreator in CLI mode requires a valid 'serverpackcreator.conf'-file to be present.");
        System.out.println("You may load the previous configuration, saved as '" + customFileName
            + "' and save it as 'serverpackcreator.conf'");
      }

    } else {
      if (CONFIGUTILITIES.writeConfigToFile(
          configurationModel,
          APPLICATIONPROPERTIES.DEFAULT_CONFIG())) {

        System.out.println(
            "Your configuration has been saved as 'serverpackcreator.conf'.");
      }
    }
  }
}
