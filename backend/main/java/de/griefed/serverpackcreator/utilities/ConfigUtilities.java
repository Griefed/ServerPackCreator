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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.griefed.serverpackcreator.ApplicationProperties;
import de.griefed.serverpackcreator.ConfigurationModel;
import de.griefed.serverpackcreator.utilities.common.Utilities;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.ProviderNotFoundException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import net.lingala.zip4j.ZipFile;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Utility-class revolving around configuration utilities.
 *
 * @author Griefed
 */
@Component
public class ConfigUtilities {
  // TODO move class back to configurationhandler...why did I make all this stuff a separate class
  // anyway?
  private static final Logger LOG = LogManager.getLogger(ConfigUtilities.class);

  private final Utilities UTILITIES;
  private final ApplicationProperties APPLICATIONPROPERTIES;
  private final ObjectMapper OBJECT_MAPPER;

  @Autowired
  public ConfigUtilities(
      Utilities injectedUtilities,
      ApplicationProperties injectedApplicationProperties,
      ObjectMapper objectMapper) {

    this.APPLICATIONPROPERTIES = injectedApplicationProperties;
    this.UTILITIES = injectedUtilities;
    this.OBJECT_MAPPER = objectMapper;
  }

  /**
   * Ensures the modloader is normalized to first letter upper case and rest lower case. Basically
   * allows the user to input Forge or Fabric in any combination of upper- and lowercase and
   * ServerPackCreator will still be able to work with the users input.
   *
   * @param modloader String. The String to check for case-insensitive cases of either Forge or
   *     Fabric.
   * @return String. Returns a normalized String of the specified modloader.
   * @author Griefed
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
   * Convenience method to write a new configuration file with the {@link ConfigurationModel} passed
   * to it. If the given file already exists, it is replaced.
   *
   * @param configurationModel Instance of {@link ConfigurationModel} to write to a file.
   * @param fileName The file to write to.
   * @return Boolean. Returns true if the configuration file has been successfully written and old
   *     ones replaced.
   * @author Griefed
   */
  public boolean writeConfigToFile(ConfigurationModel configurationModel, File fileName) {

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
        fileName);
  }

  /**
   * Writes a new configuration file with the parameters passed to it. If the given file already
   * exists, it is replaced.
   *
   * @param modpackDir String. The path to the modpack.
   * @param clientMods List, String. List of clientside-only mods.
   * @param copyDirs List, String. List of directories to include in server pack.
   * @param serverIconPath String. The path to the custom server-icon.png to include in the server
   *     pack.
   * @param serverPropertiesPath String. The path to the custom server.properties to include in the
   *     server pack.
   * @param includeServer Boolean. Whether the modloader server software should be installed.
   * @param javaPath String. Path to the java executable/binary.
   * @param minecraftVersion String. Minecraft version used by the modpack and server pack.
   * @param modLoader String. Modloader used by the modpack and server pack. Ether Forge or Fabric.
   * @param modLoaderVersion String. Modloader version used by the modpack and server pack.
   * @param includeIcon Boolean. Whether to include a server-icon in the server pack.
   * @param includeProperties Boolean. Whether to include a properties file in the server pack.
   * @param includeZip Boolean. Whether to create a ZIP-archive of the server pack, excluding
   *     Mojang's Minecraft server JAR.
   * @param javaArgs String. Java arguments to write the start-scripts with.
   * @param serverPackSuffix String. Suffix to append to the server pack to be generated.
   * @param fileName The name under which to write the new configuration file.
   * @return Boolean. Returns true if the configuration file has been successfully written and old
   *     ones replaced.
   * @author whitebear60
   * @author Griefed
   */
  public boolean writeConfigToFile(
      String modpackDir,
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
      File fileName) {

    boolean configWritten = false;

    // Griefed: What the fuck. This reads like someone having a stroke. What have I created here?
    String configString =
        String.format(
            "%s\nmodpackDir = \"%s\"\n\n"
                + "%s\nclientMods = %s\n\n"
                + "%s\ncopyDirs = %s\n\n"
                + "%s\nserverIconPath = \"%s\"\n\n"
                + "%s\nserverPropertiesPath = \"%s\"\n\n"
                + "%s\nincludeServerInstallation = %b\n\n"
                + "%s\njavaPath = \"%s\"\n\n"
                + "%s\nminecraftVersion = \"%s\"\n\n"
                + "%s\nmodLoader = \"%s\"\n\n"
                + "%s\nmodLoaderVersion = \"%s\"\n\n"
                + "%s\nincludeServerIcon = %b\n\n"
                + "%s\nincludeServerProperties = %b\n\n"
                + "%s\nincludeZipCreation = %b\n\n"
                + "%s\njavaArgs = \"%s\"\n\n"
                + "%s\nserverPackSuffix = \"%s\"",
            "# Path to your modpack. Can be either relative or absolute.\n# Example: \"./Some Modpack\" or \"C:/Minecraft/Some Modpack\"",
            modpackDir.replace("\\", "/"),
            "# List of client-only mods to delete from serverpack.\n# No need to include version specifics. Must be the filenames of the mods, not their project names on CurseForge!\n# Example: [AmbientSounds-,ClientTweaks-,PackMenu-,BetterAdvancement-,jeiintegration-]",
            UTILITIES.ListUtils()
                .encapsulateListElements(UTILITIES.ListUtils().cleanList(clientMods)),
            "# Name of directories or files to include in serverpack.\n# When specifying \"saves/world_name\", \"world_name\" will be copied to the base directory of the serverpack\n# for immediate use with the server. Automatically set when projectID,fileID for modpackDir has been specified.\n# Example: [config,mods,scripts]",
            UTILITIES.ListUtils()
                .encapsulateListElements(UTILITIES.ListUtils().cleanList(copyDirs)),
            "# Path to a custom server-icon.png-file to include in the server pack.",
            serverIconPath,
            "# Path to a custom server.properties-file to include in the server pack.",
            serverPropertiesPath,
            "# Whether to install a Forge/Fabric/Quilt server for the serverpack. Must be true or false.\n# Default value is true.",
            includeServer,
            "# Path to the Java executable. On Linux systems it would be something like \"/usr/bin/java\".\n# Only needed if includeServerInstallation is true.",
            javaPath.replace("\\", "/"),
            "# Which Minecraft version to use. Example: \"1.16.5\".\n# Automatically set when projectID,fileID for modpackDir has been specified.\n# Only needed if includeServerInstallation is true.",
            minecraftVersion,
            "# Which modloader to install. Must be either \"Forge\", \"Fabric\" or \"Quilt\".\n# Automatically set when projectID,fileID for modpackDir has been specified.\n# Only needed if includeServerInstallation is true.",
            modLoader,
            "# The version of the modloader you want to install. Example for Fabric=\"0.7.3\", example for Forge=\"36.0.15\".\n# Automatically set when projectID,fileID for modpackDir has been specified.\n# Only needed if includeServerInstallation is true.",
            modLoaderVersion,
            "# Include a server-icon.png in your serverpack. Must be true or false.\n# Customize server-icon.png in ./server_files.\n# Dimensions must be 64x64!\n# Default value is true.",
            includeIcon,
            "# Include a server.properties in your serverpack. Must be true or false.\n# Customize server.properties in ./server_files.\n# If no server.properties is provided but is set to true, a default one will be provided.\n# Default value is true.",
            includeProperties,
            "# Create zip-archive of serverpack. Must be true or false.\n# Default value is true.",
            includeZip,
            "# Java arguments to set in the start-scripts for the generated server pack. Default value is \"empty\".\n# Leave as \"empty\" to not have Java arguments in your start-scripts.",
            javaArgs,
            "# Suffix to append to the server pack to be generated. Can be left blank/empty.",
            serverPackSuffix);

    // Overwrite any existing config by deleting already existing one.
    if (fileName.exists()) {
      FileUtils.deleteQuietly(fileName);
    }

    try {
      BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
      writer.write(configString);
      writer.close();
      configWritten = true;
      /* This log is meant to be read by the user, therefore we allow translation. */
      LOG.info("Successfully written new configuration file.");
    } catch (IOException ex) {
      /* This log is meant to be read by the user, therefore we allow translation. */
      LOG.error("Couldn't write serverpackcreator.conf.", ex);
    }

    return configWritten;
  }

  /**
   * Convenience method which passes the important fields from an instance of {@link
   * ConfigurationModel} to {@link #printConfigurationModel(String, List, List, boolean, String,
   * String, String, String, boolean, boolean, boolean, String, String, String, String)}
   *
   * @param configurationModel Instance of {@link ConfigurationModel} to print to console and logs.
   * @author Griefed
   */
  public void printConfigurationModel(ConfigurationModel configurationModel) {
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
        configurationModel.getServerPropertiesPath());
  }

  /**
   * Prints all passed fields to the console and serverpackcreator.log. Used to show the user the
   * configuration before ServerPackCreator starts the generation of the server pack or, if checks
   * failed, to show the user their last configuration, so they can more easily identify problems
   * with said configuration.<br>
   * Should a user report an issue on GitHub and include their logs (which I hope they do....), this
   * would also help me help them. Logging is good. People should use more logging.
   *
   * @param modpackDirectory String. The used modpackDir field either from a configuration file or
   *     from configuration setup.
   * @param clientsideMods String List. List of clientside-only mods to exclude from the server
   *     pack...
   * @param copyDirectories String List. List of directories in the modpack which are to be included
   *     in the server pack.
   * @param installServer Boolean. Whether to install the modloader server in the server pack.
   * @param javaInstallPath String. Path to the Java executable/binary needed for installing the
   *     modloader server in the server pack.
   * @param minecraftVer String. The Minecraft version the modpack uses.
   * @param modloader String. The modloader the modpack uses.
   * @param modloaderVersion String. The version of the modloader the modpack uses.
   * @param includeIcon Boolean. Whether to include the server-icon.png in the server pack.
   * @param includeProperties Boolean. Whether to include the server.properties in the server pack.
   * @param includeZip Boolean. Whether to create a zip-archive of the server pack, excluding the
   *     Minecraft server JAR according to Mojang's TOS and EULA.
   * @param javaArgs String. Java arguments to write the start-scripts with.
   * @param serverPackSuffix String. Suffix to append to name of the server pack to be generated.
   * @param serverIconPath String. The path to the custom server-icon.png to be used in the server
   *     pack.
   * @param serverPropertiesPath String. The path to the custom server.properties to be used in the
   *     server pack.
   * @author Griefed
   */
  public void printConfigurationModel(
      String modpackDirectory,
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

    LOG.info("Your configuration is:");
    LOG.info("Modpack directory: " + modpackDirectory);

    if (clientsideMods.isEmpty()) {
      /* This log is meant to be read by the user, therefore we allow translation. */
      LOG.warn("No client mods specified.");
    } else {

      /* This log is meant to be read by the user, therefore we allow translation. */
      LOG.info("Client mods specified. Client mods are:");
      for (String mod : clientsideMods) {
        LOG.info(String.format("    %s", mod));
      }
    }

    /* This log is meant to be read by the user, therefore we allow translation. */
    LOG.info("Directories to copy:");

    if (copyDirectories != null) {

      for (String directory : copyDirectories) {
        LOG.info(String.format("    %s", directory));
      }

    } else {
      /* This log is meant to be read by the user, therefore we allow translation. */
      LOG.error("List of directories to copy is empty.");
    }

    /* This log is meant to be read by the user, therefore we allow translation. */
    LOG.info("Include server installation:      " + installServer);
    LOG.info("Java Installation path:           " + javaInstallPath);
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
  }

  /**
   * <strong><code>instance.json</code></strong>
   *
   * <p>Update the given ConfigurationModel with values gathered from a ATLauncher manifest.
   *
   * @param configurationModel {@link ConfigurationModel} The model to update.
   * @param manifest {@link File} The manifest file.
   * @throws IOException when the instance.json-file could not be parsed.
   * @author Griefed
   */
  public void updateConfigModelFromATLauncherInstance(
      ConfigurationModel configurationModel, File manifest) throws IOException {

    configurationModel.setModpackJson(getJson(manifest));

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
   * <strong><code>modrinth.index.json</code></strong>
   *
   * <p>Update the given ConfigurationModel with values gathered from a Modrinth <code>
   * modrinth.index.json</code>-manifest.
   *
   * @param configurationModel {@link ConfigurationModel} The model to update.
   * @param manifest {@link File} The manifest file.
   * @throws IOException when the modrinth.index.json-file could not be parsed.
   * @author Griefed
   */
  public void updateConfigModelFromModrinthManifest(
      ConfigurationModel configurationModel, File manifest) throws IOException {

    configurationModel.setModpackJson(getJson(manifest));

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
   * <strong><code>manifest.json</code></strong>
   *
   * <p>Update the given ConfigurationModel with values gathered from the downloaded CurseForge
   * modpack. A manifest.json-file is usually created when a modpack is exported through launchers
   * like Overwolf's CurseForge or GDLauncher.
   *
   * @param configurationModel {@link ConfigurationModel}. An instance containing a configuration
   *     for a modpack from which to create a server pack.
   * @param manifest File. The CurseForge manifest.json-file of the modpack to read.
   * @throws IOException when the manifest.json-file could not be parsed.
   * @author Griefed
   */
  public void updateConfigModelFromCurseManifest(
      ConfigurationModel configurationModel, File manifest) throws IOException {

    configurationModel.setModpackJson(getJson(manifest));

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
   * <strong><code>minecraftinstance.json</code></strong>
   *
   * <p>Update the given ConfigurationModel with values gathered from the minecraftinstance.json of
   * the modpack. A minecraftinstance.json is usually created by Overwolf's CurseForge launcher.
   *
   * @param configurationModel {@link ConfigurationModel}. An instance containing a configuration
   *     for a modpack from which to create a server pack.
   * @param minecraftInstance File. The minecraftinstance.json-file of the modpack to read.
   * @throws IOException when the minecraftinstance.json-file could not be parsed.
   * @author Griefed
   */
  public void updateConfigModelFromMinecraftInstance(
      ConfigurationModel configurationModel, File minecraftInstance) throws IOException {

    configurationModel.setModpackJson(getJson(minecraftInstance));

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
   * <strong><code>config.json</code></strong>
   *
   * <p>Update the given ConfigurationModel with values gathered from the modpacks config.json. A
   * config.json is usually created by GDLauncher.
   *
   * @param configurationModel {@link ConfigurationModel}. An instance containing a configuration
   *     for a modpack from which to create a server pack.
   * @param config {@link File}. The config.json-file of the modpack to read.
   * @throws IOException when the config.json-file could not be parsed.
   * @author Griefed
   */
  public void updateConfigModelFromConfigJson(ConfigurationModel configurationModel, File config)
      throws IOException {

    configurationModel.setModpackJson(getJson(config));

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
   * <strong><code>mmc-pack.json</code></strong>
   *
   * <p>Update the given ConfigurationModel with values gathered from the modpacks mmc-pack.json. A
   * mmc-pack.json is usually created by the MultiMC launcher.
   *
   * @param configurationModel {@link ConfigurationModel}. An instance containing a configuration
   *     for a modpack from which to create a server pack.
   * @param mmcPack {@link File}. The config.json-file of the modpack to read.
   * @throws IOException when the mmc-pack.json-file could not be parsed.
   * @author Griefed
   */
  public void updateConfigModelFromMMCPack(ConfigurationModel configurationModel, File mmcPack)
      throws IOException {

    configurationModel.setModpackJson(getJson(mmcPack));

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
   * <strong><code>instance.cfg</code></strong>
   *
   * <p>Acquire the name of the modpack/instance of a MultiMC modpack from the modpacks
   * instance.cfg, which is usually created by the MultiMC launcher.
   *
   * @param instanceCfg {@link File}. The config.json-file of the modpack to read.
   * @return {@link String} Returns the instance name.
   * @throws IOException when the file could not be found or the properties not be loaded from the
   *     file.
   * @author Griefed
   */
  public String updateDestinationFromInstanceCfg(File instanceCfg) throws IOException {

    String name;

    try (InputStream inputStream = Files.newInputStream(instanceCfg.toPath())) {

      Properties properties = new Properties();
      properties.load(inputStream);

      name = properties.getProperty("name", null);
    }

    return name;
  }

  /**
   * Acquire a {@link JsonNode} from the given json file.
   *
   * @param jsonFile {@link File}. The file to read.
   * @return {@link JsonNode} containing the files json data.
   * @throws IOException when the file could not be parsed/read into a {@link JsonNode}.
   * @author Griefed
   */
  private JsonNode getJson(File jsonFile) throws IOException {
    return OBJECT_MAPPER.readTree(
        Files.readAllBytes(Paths.get(jsonFile.getAbsolutePath().replace("\\", "/"))));
  }

  /**
   * Creates a list of suggested directories to include in server pack which is later on written to
   * a new configuration file. The list of directories to include in the server pack which is
   * generated by this method excludes well know directories which would not be needed by a server
   * pack. If you have suggestions to this list, open a feature request issue on <a
   * href="https://github.com/Griefed/ServerPackCreator/issues/new/choose">GitHub</a>
   *
   * @param modpackDir String. The directory for which to gather a list of directories to copy to
   *     the server pack.
   * @return List, String. Returns a list of directories inside the modpack, excluding well known
   *     client-side only directories.
   * @author Griefed
   */
  public List<String> suggestCopyDirs(String modpackDir) {
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
   * Checks whether the projectID for the Jumploader mod is present in the list of mods required by
   * the CurseForge modpack. If Jumploader is found, the modloader for the new configuration-file
   * will be set to Fabric. If <code>modLoaders</code> in the manifest specifies Fabric, use that to
   * set the modloader and its version.
   *
   * @param modpackJson JSonNode. JsonNode containing all information about the CurseForge modpack.
   * @return Boolean. Returns true if Jumploader is found.
   * @author Griefed
   * @deprecated Regular checks in {@link #updateConfigModelFromCurseManifest(ConfigurationModel,
   *     File)} detect Fabric and the version. Iterating through the mods list is no longer
   *     necessary.
   */
  @Deprecated
  public boolean checkCurseForgeJsonForFabric(JsonNode modpackJson) {

    for (int i = 0; i < modpackJson.get("files").size(); i++) {

      LOG.debug(
          String.format("Mod ID: %s", modpackJson.get("files").get(i).get("projectID").asText()));
      LOG.debug(
          String.format("File ID: %s", modpackJson.get("files").get(i).get("fileID").asText()));

      if (modpackJson.get("files").get(i).get("projectID").asText().equalsIgnoreCase("361988")
          || modpackJson.get("files").get(i).get("fileID").asText().equalsIgnoreCase("306612")) {

        /* This log is meant to be read by the user, therefore we allow translation. */
        LOG.info("Fabric detected. Setting modloader to \"Fabric\".");
        return true;
      }
    }

    String[] modloaderAndVersion =
        modpackJson.get("minecraft").get("modLoaders").get(0).get("id").asText().split("-");

    return modloaderAndVersion[0].equalsIgnoreCase("fabric");
  }

  /**
   * Acquire a list of directories in the base-directory of a ZIP-file.
   *
   * @param zipFile {@link java.nio.file.Path} The ZIP-archive to get the list of files from.
   * @return {@link List} {@link String} A list of all directories in the base-directory of the
   *     ZIP-file.
   * @throws IllegalArgumentException if the pre-conditions for the uri parameter are not met, or
   *     the env parameter does not contain properties required by the provider, or a property value
   *     is invalid.
   * @throws FileSystemAlreadyExistsException if the file system has already been created.
   * @throws ProviderNotFoundException if a provider supporting the URI scheme is not installed.
   * @throws IOException if an I/O error occurs creating the file system.
   * @throws SecurityException if a security manager is installed, and it denies an unspecified
   *     permission required by the file system provider implementation.
   * @author Griefed
   */
  public List<String> getDirectoriesInModpackZipBaseDirectory(ZipFile zipFile)
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
   * Acquire a list of all files in a ZIP-file.
   *
   * @param zipFile {@link java.nio.file.Path} The ZIP-archive to get the list of files from.
   * @return {@link List} {@link String} A list of all files in the ZIP-file.
   * @throws IllegalArgumentException if the pre-conditions for the uri parameter are not met, or
   *     the env parameter does not contain properties required by the provider, or a property value
   *     is invalid.
   * @throws FileSystemAlreadyExistsException if the file system has already been created.
   * @throws ProviderNotFoundException if a provider supporting the URI scheme is not installed.
   * @throws IOException if an I/O error occurs creating the file system.
   * @throws SecurityException if a security manager is installed, and it denies an unspecified
   *     permission required by the file system provider implementation.
   * @author Griefed
   */
  public List<String> getFilesInModpackZip(ZipFile zipFile)
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

  /**
   * Acquire a list of all directories in a ZIP-file.
   *
   * @param zipFile {@link java.nio.file.Path} The ZIP-archive to get the list of files from.
   * @return {@link List} {@link String} A list of all directories in the ZIP-file.
   * @throws IllegalArgumentException if the pre-conditions for the uri parameter are not met, or
   *     the env parameter does not contain properties required by the provider, or a property value
   *     is invalid.
   * @throws FileSystemAlreadyExistsException if the file system has already been created.
   * @throws ProviderNotFoundException if a provider supporting the URI scheme is not installed.
   * @throws IOException if an I/O error occurs creating the file system.
   * @throws SecurityException if a security manager is installed, and it denies an unspecified
   *     permission required by the file system provider implementation.
   * @author Griefed
   */
  public List<String> getDirectoriesInModpackZip(ZipFile zipFile)
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
   * Acquire a list of all files and directories in a ZIP-file.
   *
   * @param zipFile {@link java.nio.file.Path} The ZIP-archive to get the list of files from.
   * @return {@link List} {@link String} A list of all files and directories in the ZIP-file.
   * @throws IllegalArgumentException if the pre-conditions for the uri parameter are not met, or
   *     the env parameter does not contain properties required by the provider, or a property value
   *     is invalid.
   * @throws FileSystemAlreadyExistsException if the file system has already been created.
   * @throws ProviderNotFoundException if a provider supporting the URI scheme is not installed.
   * @throws IOException if an I/O error occurs creating the file system.
   * @throws SecurityException if a security manager is installed, and it denies an unspecified
   *     permission required by the file system provider implementation.
   * @author Griefed
   */
  public List<String> getAllFilesAndDirectoriesInModpackZip(ZipFile zipFile)
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
}
