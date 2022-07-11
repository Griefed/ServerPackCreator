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

import de.griefed.serverpackcreator.ApplicationProperties;
import de.griefed.serverpackcreator.ConfigurationHandler;
import de.griefed.serverpackcreator.i18n.I18n;
import de.griefed.serverpackcreator.utilities.common.Utilities;
import de.griefed.serverpackcreator.utilities.misc.Generated;
import de.griefed.serverpackcreator.versionmeta.VersionMeta;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utility-class revolving around the creation of a new configuration file.
 *
 * @author Griefed
 */
@Generated
public class ConfigurationCreator {

  private static final Logger LOG = LogManager.getLogger(ConfigurationCreator.class);

  private final I18n I18N;
  private final ConfigurationHandler CONFIGURATIONHANDLER;
  private final ApplicationProperties APPLICATIONPROPERTIES;
  private final VersionMeta VERSIONMETA;
  private final Utilities UTILITIES;
  private final ConfigUtilities CONFIGUTILITIES;

  public ConfigurationCreator(
      I18n injectedI18n,
      ConfigurationHandler injectedConfigurationHandler,
      ApplicationProperties injectedApplicationProperties,
      Utilities injectedUtilities,
      VersionMeta injectedVersionMeta,
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

    if (injectedConfigurationHandler == null) {
      this.CONFIGURATIONHANDLER =
          new ConfigurationHandler(
              I18N, VERSIONMETA, APPLICATIONPROPERTIES, UTILITIES, CONFIGUTILITIES);
    } else {
      this.CONFIGURATIONHANDLER = injectedConfigurationHandler;
    }
  }

  /**
   * Walk the user through the generation of a new ServerPackCreator configuration file by asking
   * them for input, step-by-step, regarding their modpack. At the end of this method a fully
   * configured serverpackcreator.conf file is saved and any previously existing configuration file
   * replaced by the new one.<br>
   * After every input, said input is displayed to the user, and they're asked whether they are
   * satisfied with said input. The user can then decide whether they would like to restart the
   * entry of the field they just configured, or agree and move to the next one.<br>
   * At the end of this method, the user will have a newly configured and created configuration file
   * for ServerPackCreator.<br>
   * <br>
   * Most user-input is checked after entry to ensure the configuration is already in
   * working-condition after completion of this method.
   *
   * @author whitebear60
   * @author Griefed
   */
  public void createConfigurationFile() {

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

    LOG.info(
        "You started ServerPackCreator with the \"-cgen\" argument. Step-by-step generation of config file initiated...");
    do {
      // -----------------------------------------------------------------MODPACK DIRECTORY---------

      LOG.info("Please enter your modpack path. This path can be relative to ServerPackCreator, or absolute.");
      LOG.info("Example: \"./Some Modpack\" or \"C:/Minecraft/Some Modpack\"");

      do {

        do {
          System.out.print("Path to modpack directory: ");
          tmpModpackDir = reader.nextLine();
        } while (!CONFIGURATIONHANDLER.checkModpackDir(tmpModpackDir, encounteredErrors));

        LOG.info("You entered: " + tmpModpackDir);
        LOG.info("If you are satisfied with this setting, enter ture. If not, enter false to restart modpack directory configuration.");

        System.out.print("Answer: ");

      } while (!UTILITIES.BooleanUtils().readBoolean());

      modpackDir = tmpModpackDir.replace("\\", "/");

      LOG.info("You entered: " + modpackDir);
      System.out.println();

      // --------------------------------------------------------------CLIENTSIDE-ONLY MODS---------

      LOG.info("Enter filenames of clientside-only mods, one per line. When you are done, simply press enter with empty input.");
      do {
        clientMods.clear();

        clientMods.addAll(UTILITIES.ListUtils().readStringArray());

        /* This log is meant to be read by the user, therefore we allow translation. */
        LOG.info("You entered: " + clientMods);

        if (clientMods.isEmpty()) {
          clientMods = APPLICATIONPROPERTIES.getListFallbackMods();

          LOG.warn("No clientside-only mods specified. Using fallback list.");

          for (String mod : clientMods) {
            LOG.warn(String.format("    %s", mod));
          }
        }


        LOG.info("If you are satisfied with these values, enter true. If not, enter false to restart clientmod configuration.");

        System.out.print("Answer: ");

      } while (!UTILITIES.BooleanUtils().readBoolean());

      LOG.info("You entered: " + clientMods);

      tmpClientMods = new String[clientMods.size()];
      clientMods.toArray(tmpClientMods);

      System.out.println();

      // ---------------------------------------DIRECTORIES OR FILES TO COPY TO SERVER PACK---------

      LOG.info("Which directories or files should be copied to the server pack? These are folder- or filenames inside your modpack directory or explicit source/file;destination/file-combinations.");

      List<String> dirList =
          Arrays.asList(
              Objects.requireNonNull(
                  new File(modpackDir)
                      .list((current, name) -> new File(current, name).isDirectory())));

      LOG.info("The modpack directory you specified contains the following directories: " + UTILITIES.StringUtils().buildString(dirList));

      do {
        do {
          copyDirs.clear();

          LOG.info("Specify your directories and files you want to be copied:");
          copyDirs.addAll(UTILITIES.ListUtils().readStringArray());

        } while (!CONFIGURATIONHANDLER.checkCopyDirs(copyDirs, modpackDir, encounteredErrors));

        LOG.info("You entered: " + copyDirs);
        LOG.info("If you are satisfied with these values, enter true. If not, enter false to restart clientmod configuration.");

        System.out.print("Answer: ");

      } while (!UTILITIES.BooleanUtils().readBoolean());

      LOG.info("You entered: " + copyDirs);

      tmpCopyDirs = new String[copyDirs.size()];
      copyDirs.toArray(tmpCopyDirs);

      System.out.println();

      // ------------------------------------------------PATH TO THE CUSTOM SERVER-ICON.PNG---------

      LOG.info("Enter the path to your custom server-icon.png-file, if you want to include one. Leave blank if you are fine with the default.");

      do {

        do {
          System.out.print("Path to your server-icon.png: ");
          tmpServerIcon = reader.nextLine();

        } while (!CONFIGURATIONHANDLER.checkIconAndProperties(tmpServerIcon));

        LOG.info("You entered: " + tmpServerIcon);
        LOG.info("If you are satisfied with this setting, enter ture. If not, enter false to restart server-icon.png configuration.");
        System.out.print("Answer: ");

      } while (!UTILITIES.BooleanUtils().readBoolean());

      serverIconPath = tmpServerIcon.replace("\\", "/");

      LOG.info("You entered: " + serverIconPath);
      System.out.println();

      //-----------------------------------------------PATH TO THE CUSTOM SERVER.PROPERTIES---------

      LOG.info("Enter the path to your custom server.properties-file, if you want to include one. Leave blank if you are fine with the default.");

      do {

        do {

          System.out.print("Path to your server.properties: ");
          tmpServerProperties = reader.nextLine();
        } while (!CONFIGURATIONHANDLER.checkIconAndProperties(tmpServerProperties));

        LOG.info("You entered: " + tmpServerProperties);
        LOG.info("If you are satisfied with this setting, enter ture. If not, enter false to restart server-icon.png configuration.");

        System.out.print("Answer: ");

      } while (!UTILITIES.BooleanUtils().readBoolean());

      serverPropertiesPath = tmpServerProperties.replace("\\", "/");

      LOG.info("You entered: " + serverPropertiesPath);
      System.out.println();

      // ----------------------------------WHETHER TO INCLUDE MODLOADER SERVER INSTALLATION---------

      LOG.info("Do you want ServerPackCreator to install the modloader server for your server pack? Must be true or false.");

      System.out.print("Include modloader server installation: ");
      includeServerInstallation = UTILITIES.BooleanUtils().readBoolean();

      LOG.info("You entered: " + includeServerInstallation);

      // ----------------------------------------------------MINECRAFT VERSION MODPACK USES---------
      LOG.info("Which version of Minecraft does your modpack use?");

      do {
        System.out.print("Minecraft version: ");
        minecraftVersion = reader.nextLine();

      } while (!VERSIONMETA.minecraft().checkMinecraftVersion(minecraftVersion));

      LOG.info("You entered: " + minecraftVersion);
      System.out.println();

      // ------------------------------------------------------------MODLOADER MODPACK USES---------

      LOG.info("What modloader does your modpack use?");

      do {
        System.out.print("Modloader: ");
        modLoader = reader.nextLine();

      } while (!CONFIGURATIONHANDLER.checkModloader(modLoader));

      modLoader = CONFIGUTILITIES.getModLoaderCase(modLoader);

      LOG.info("You entered: " + modLoader);
      System.out.println();

      // -------------------------------------------------VERSION OF MODLOADER MODPACK USES---------
      LOG.info("What version of " + modLoader + " does your modpack use?");

      do {
        System.out.print("Modloader version: ");
        modLoaderVersion = reader.nextLine();

      } while (!CONFIGURATIONHANDLER.checkModloaderVersion(
          modLoader, modLoaderVersion, minecraftVersion));

      LOG.info("You entered: " + modLoaderVersion);
      System.out.println();

      // ---------------------------------------------------------PATH TO JAVA INSTALLATION---------
      LOG.info(
          "Specify the path to your Java installation. Must end with \"java\" on Linux, or \"java.exe\" on Windows.");
      LOG.info("If you leave this empty, ServerPackCreator will try to determine the path for you.");
      LOG.info("Example Linux: /usr/bin/java | Example Windows: C:/Program Files/AdoptOpenJDK/jdk-8.0.275.1-hotspot/jre/bin/java.exe");

      System.out.print("Path to your Java installation: ");

      javaPath = CONFIGURATIONHANDLER.getJavaPath(reader.nextLine());

      System.out.println("Automatically acquired path to Java installation: " + javaPath);

      System.out.println();

      // ---------------------------------WHETHER TO INCLUDE SERVER-ICON.PNG IN SERVER PACK---------

      LOG.info("Do you want ServerPackCreator to include a server-icon in your server pack? Must be true or false.");

      System.out.print("Include server-icon.png: ");
      includeServerIcon = UTILITIES.BooleanUtils().readBoolean();

      LOG.info("You entered: " + includeServerIcon);
      System.out.println();

      // -------------------------------WHETHER TO INCLUDE SERVER.PROPERTIES IN SERVER PACK---------

      LOG.info("Do you want ServerPackCreator to include a server.properties in your server pack? Must be true or false.");

      System.out.print("Include server.properties: ");
      includeServerProperties = UTILITIES.BooleanUtils().readBoolean();

      LOG.info("You entered: " + includeServerProperties);
      System.out.println();

      // -------------------------WHETHER TO INCLUDE CREATION OF ZIP-ARCHIVE OF SERVER PACK---------
      LOG.info("Do you want ServerPackCreator to create a ZIP-archive of your server pack? Must be true or false.");

      System.out.print("Create ZIP-archive: ");
      includeZipCreation = UTILITIES.BooleanUtils().readBoolean();

      LOG.info("You entered: " + includeZipCreation);

      // ----------------------------------------------JAVA ARGS TO EXECUTE THE SERVER WITH---------

      LOG.info("Specify the Java arguments, if any, to execute the server with. Can be left blank.");

      System.out.print("Java args: ");
      javaArgs = reader.nextLine();

      if (javaArgs.isEmpty()) {
        javaArgs = "empty";
      }

      LOG.info("Java arguments for start-scripts: " + javaArgs);

      // ---------------------------------------------------SUFFIX TO APPEND TO SERVER PACK---------

      LOG.info("Enter the suffix you want to append to your server pack. Can be left empty.");

      System.out.print("Server pack suffix: ");
      serverPackSuffix = reader.nextLine();

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

      LOG.info("If you are satisfied with these values, enter true. If not, enter false to restart config generation.");

      System.out.print("Answer: ");

    } while (!UTILITIES.BooleanUtils().readBoolean());

    reader.close();

    // ----------------------------------------------------------------WRITE CONFIG TO FILE---------
    if (CONFIGUTILITIES.writeConfigToFile(
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
        APPLICATIONPROPERTIES.DEFAULT_CONFIG())) {

      LOG.info("New config file successfully written. Thanks go to Whitebear60 for initially writing the CLI-Config-Generation.");
    }
  }
}
