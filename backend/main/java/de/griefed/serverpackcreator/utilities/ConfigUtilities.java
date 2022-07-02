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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.griefed.serverpackcreator.ApplicationProperties;
import de.griefed.serverpackcreator.ConfigurationModel;
import de.griefed.serverpackcreator.i18n.I18n;
import de.griefed.serverpackcreator.utilities.common.Utilities;
import de.griefed.serverpackcreator.versionmeta.VersionMeta;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.ProviderNotFoundException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
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

  private static final Logger LOG = LogManager.getLogger(ConfigUtilities.class);

  private final I18n I18N;
  private final Utilities UTILITIES;
  private final ApplicationProperties APPLICATIONPROPERTIES;
  private final VersionMeta VERSIONMETA;

  @Autowired
  public ConfigUtilities(
      I18n injectedI18n,
      Utilities injectedUtilities,
      ApplicationProperties injectedApplicationProperties,
      VersionMeta injectedVersionMeta)
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

    if (injectedUtilities == null) {
      this.UTILITIES = new Utilities(I18N, APPLICATIONPROPERTIES);
    } else {
      this.UTILITIES = injectedUtilities;
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
            I18N.getLocalizedString("configuration.writeconfigtofile.modpackdir"),
            modpackDir.replace("\\", "/"),
            I18N.getLocalizedString("configuration.writeconfigtofile.clientmods"),
            UTILITIES.ListUtils()
                .encapsulateListElements(UTILITIES.ListUtils().cleanList(clientMods)),
            I18N.getLocalizedString("configuration.writeconfigtofile.copydirs"),
            UTILITIES.ListUtils()
                .encapsulateListElements(UTILITIES.ListUtils().cleanList(copyDirs)),
            I18N.getLocalizedString("configuration.writeconfigtofile.custom.icon"),
            serverIconPath,
            I18N.getLocalizedString(
                "configuration.writeconfigtofile.custom.properties"),
            serverPropertiesPath,
            I18N.getLocalizedString(
                "configuration.writeconfigtofile.includeserverinstallation"),
            includeServer,
            I18N.getLocalizedString("configuration.writeconfigtofile.javapath"),
            javaPath.replace("\\", "/"),
            I18N.getLocalizedString(
                "configuration.writeconfigtofile.minecraftversion"),
            minecraftVersion,
            I18N.getLocalizedString("configuration.writeconfigtofile.modloader"),
            modLoader,
            I18N.getLocalizedString(
                "configuration.writeconfigtofile.modloaderversion"),
            modLoaderVersion,
            I18N.getLocalizedString(
                "configuration.writeconfigtofile.includeservericon"),
            includeIcon,
            I18N.getLocalizedString(
                "configuration.writeconfigtofile.includeserverproperties"),
            includeProperties,
            I18N.getLocalizedString(
                "configuration.writeconfigtofile.includezipcreation"),
            includeZip,
            I18N.getLocalizedString("configuration.writeconfigtofile.javaargs"),
            javaArgs,
            I18N.getLocalizedString(
                "configuration.writeconfigtofile.serverpacksuffix"),
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
      LOG.info(
          I18N.getLocalizedString(
              "defaultfiles.log.info.writeconfigtofile.confignew"));
    } catch (IOException ex) {
      /* This log is meant to be read by the user, therefore we allow translation. */
      LOG.error(
          I18N.getLocalizedString("defaultfiles.log.error.writeconfigtofile"), ex);
    }

    return configWritten;
  }

  /**
   * Creates a list of all configurations as they appear in the serverpackcreator.conf to pass it to
   * any addon that may run. Values included in this list are:<br>
   * 1. modpackDir<br>
   * 2. clientMods<br>
   * 3. copyDirs<br>
   * 4. javaPath<br>
   * 5. minecraftVersion<br>
   * 6. modLoader<br>
   * 7. modLoaderVersion<br>
   * 8. includeServerInstallation<br>
   * 9. includeServerIcon<br>
   * 10.includeServerProperties<br>
   * 11.includeStartScripts<br>
   * 12.includeZipCreation
   *
   * @param configurationModel An instance of {@link ConfigurationModel} which contains the
   *     configuration of the modpack.
   * @return String List. A list of all configurations as strings.
   * @author Griefed
   */
  @Deprecated
  public List<String> getConfigurationAsList(ConfigurationModel configurationModel) {

    List<String> configurationAsList = new ArrayList<>(100);

    configurationAsList.add(configurationModel.getModpackDir());
    configurationAsList.add(
        UTILITIES.StringUtils().buildString(configurationModel.getClientMods()));
    configurationAsList.add(
        UTILITIES.StringUtils().buildString(configurationModel.getCopyDirs()));
    configurationAsList.add(configurationModel.getJavaPath());
    configurationAsList.add(configurationModel.getMinecraftVersion());
    configurationAsList.add(configurationModel.getModLoader());
    configurationAsList.add(configurationModel.getModLoaderVersion());
    configurationAsList.add(String.valueOf(configurationModel.getIncludeServerInstallation()));
    configurationAsList.add(String.valueOf(configurationModel.getIncludeServerIcon()));
    configurationAsList.add(String.valueOf(configurationModel.getIncludeServerProperties()));
    configurationAsList.add(String.valueOf(configurationModel.getIncludeZipCreation()));

    LOG.debug(String.format("Configuration to pass to addons is: %s", configurationAsList));

    return configurationAsList;
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

    /* This log is meant to be read by the user, therefore we allow translation. */
    LOG.info(I18N.getLocalizedString("configuration.log.info.printconfig.start"));
    LOG.info(
        String.format(
            I18N.getLocalizedString("configuration.log.info.printconfig.modpackdir"),
            modpackDirectory));

    if (clientsideMods.isEmpty()) {
      /* This log is meant to be read by the user, therefore we allow translation. */
      LOG.warn(
          I18N.getLocalizedString(
              "configuration.log.warn.printconfig.noclientmods"));
    } else {

      /* This log is meant to be read by the user, therefore we allow translation. */
      LOG.info(
          I18N.getLocalizedString("configuration.log.info.printconfig.clientmods"));
      for (String mod : clientsideMods) {
        LOG.info(String.format("    %s", mod));
      }
    }

    /* This log is meant to be read by the user, therefore we allow translation. */
    LOG.info(I18N.getLocalizedString("configuration.log.info.printconfig.copydirs"));

    if (copyDirectories != null) {

      for (String directory : copyDirectories) {
        LOG.info(String.format("    %s", directory));
      }

    } else {
      /* This log is meant to be read by the user, therefore we allow translation. */
      LOG.error(
          I18N.getLocalizedString("configuration.log.error.printconfig.copydirs"));
    }

    /* This log is meant to be read by the user, therefore we allow translation. */
    LOG.info(
        String.format(
            I18N.getLocalizedString("configuration.log.info.printconfig.server"),
            installServer));
    LOG.info(
        String.format(
            I18N.getLocalizedString("configuration.log.info.printconfig.javapath"),
            javaInstallPath));
    LOG.info(
        String.format(
            I18N.getLocalizedString(
                "configuration.log.info.printconfig.minecraftversion"),
            minecraftVer));
    LOG.info(
        String.format(
            I18N.getLocalizedString("configuration.log.info.printconfig.modloader"),
            modloader));
    LOG.info(
        String.format(
            I18N.getLocalizedString(
                "configuration.log.info.printconfig.modloaderversion"),
            modloaderVersion));
    LOG.info(
        String.format(
            I18N.getLocalizedString("configuration.log.info.printconfig.icon"),
            includeIcon));
    LOG.info(
        String.format(
            I18N.getLocalizedString("configuration.log.info.printconfig.properties"),
            includeProperties));
    LOG.info(
        String.format(
            I18N.getLocalizedString("configuration.log.info.printconfig.zip"),
            includeZip));
    LOG.info(
        String.format(
            I18N.getLocalizedString("configuration.log.info.printconfig.javaargs"),
            javaArgs));
    LOG.info(
        String.format(
            I18N.getLocalizedString(
                "configuration.log.info.printconfig.serverpacksuffix"),
            serverPackSuffix));
    LOG.info(
        String.format(
            I18N.getLocalizedString("utilities.log.info.config.print.servericon"),
            serverIconPath));
    LOG.info(
        String.format(
            I18N.getLocalizedString(
                "utilities.log.info.config.print.serverproperties"),
            serverPropertiesPath));
  }

  /**
   * Update the given ConfigurationModel with values gathered from the downloaded CurseForge
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

    configurationModel.setCurseModpack(getJson(manifest));

    String[] modloaderAndVersion =
        configurationModel
            .getCurseModpack()
            .get("minecraft")
            .get("modLoaders")
            .get(0)
            .get("id")
            .asText()
            .split("-");

    configurationModel.setMinecraftVersion(
        configurationModel.getCurseModpack().get("minecraft").get("version").asText());

    if (checkCurseForgeJsonForFabric(configurationModel.getCurseModpack())) {

      if (modloaderAndVersion[0].equalsIgnoreCase("Fabric")) {

        configurationModel.setModLoader("Fabric");
        configurationModel.setModLoaderVersion(modloaderAndVersion[1]);

      } else {

        /* This log is meant to be read by the user, therefore we allow translation. */
        LOG.info(I18N.getLocalizedString("configuration.log.info.iscurse.fabric"));
        LOG.debug(
            I18N.getLocalizedString("configuration.log.debug.modloader.forge"));

        configurationModel.setModLoader("Fabric");
        configurationModel.setModLoaderVersion(VERSIONMETA.fabric().releaseLoaderVersion());
      }

    } else {

      /* This log is meant to be read by the user, therefore we allow translation. */
      LOG.debug(I18N.getLocalizedString("configuration.log.debug.modloader.forge"));

      configurationModel.setModLoader("Forge");
      configurationModel.setModLoaderVersion(modloaderAndVersion[1]);
    }
  }

  /**
   * Update the given ConfigurationModel with values gathered from the minecraftinstance.json of the
   * modpack. A minecraftinstance.json is usually created by Overwolf's CurseForge launcher.
   *
   * @param configurationModel {@link ConfigurationModel}. An instance containing a configuration
   *     for a modpack from which to create a server pack.
   * @param minecraftInstance File. The minecraftinstance.json-file of the modpack to read.
   * @throws IOException when the minecraftinstance.json-file could not be parsed.
   * @author Griefed
   */
  public void updateConfigModelFromMinecraftInstance(
      ConfigurationModel configurationModel, File minecraftInstance) throws IOException {

    configurationModel.setCurseModpack(getJson(minecraftInstance));

    String[] modLoaderAndVersion =
        configurationModel.getCurseModpack().get("baseModLoader").get("name").asText().split("-");

    configurationModel.setModLoader(getModLoaderCase(modLoaderAndVersion[0]));
    configurationModel.setModLoaderVersion(modLoaderAndVersion[1]);
    configurationModel.setMinecraftVersion(
        configurationModel.getCurseModpack().get("baseModLoader").get("minecraftVersion").asText());
  }

  /**
   * Update the given ConfigurationModel with values gathered from the modpacks config.json. A
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

    configurationModel.setCurseModpack(getJson(config));

    configurationModel.setModLoader(
        getModLoaderCase(
            configurationModel.getCurseModpack().get("loader").get("loaderType").asText()));
    configurationModel.setMinecraftVersion(
        configurationModel.getCurseModpack().get("loader").get("mcVersion").asText());

    if (configurationModel.getModLoader().equalsIgnoreCase("forge")) {

      configurationModel.setModLoaderVersion(
          configurationModel
              .getCurseModpack()
              .get("loader")
              .get("loaderVersion")
              .asText()
              .split("-")[1]);

    } else {

      configurationModel.setModLoaderVersion(
          configurationModel.getCurseModpack().get("loader").get("loaderVersion").asText());
    }
  }

  /**
   * Update the given ConfigurationModel with values gathered from the modpacks mmc-pack.json. A
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

    configurationModel.setCurseModpack(getJson(mmcPack));

    for (JsonNode jsonNode : configurationModel.getCurseModpack().get("components")) {

      if (jsonNode.get("uid").asText().equals("net.minecraft")) {

        configurationModel.setMinecraftVersion(jsonNode.get("version").asText());

      } else if (jsonNode.get("uid").asText().equals("net.minecraftforge")) {

        configurationModel.setModLoader("Forge");
        configurationModel.setModLoaderVersion(jsonNode.get("version").asText());

      } else if (jsonNode.get("uid").asText().equals("net.fabricmc.fabric-loader")) {

        configurationModel.setModLoader("Fabric");
        configurationModel.setModLoaderVersion(jsonNode.get("version").asText());
      }
    }
  }

  /**
   * Acquire the name of the modpack/instance of a MultiMC modpack from the modpacks instance.cfg,
   * which is usually created by the MultiMC launcher.
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
    return getObjectMapper()
        .readTree(Files.readAllBytes(Paths.get(jsonFile.getAbsolutePath().replace("\\", "/"))));
  }

  /**
   * Getter for the object-mapper used for working with JSON-data.
   *
   * @return ObjectMapper. Returns the object-mapper used for working with JSON-data.
   * @author Griefed
   */
  private ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
    return objectMapper;
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
    LOG.info(
        I18N.getLocalizedString("configuration.log.info.suggestcopydirs.start"));

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

    for (int idirs = 0;
        idirs < APPLICATIONPROPERTIES.getDirectoriesToExclude().size();
        idirs++) {

      int i = idirs;

      dirsInModpack.removeIf(
          n -> (n.contains(APPLICATIONPROPERTIES.getDirectoriesToExclude().get(i))));
    }

    LOG.info(
        String.format(
            I18N.getLocalizedString("configuration.log.info.suggestcopydirs.list"),
            dirsInModpack));

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
   */
  public boolean checkCurseForgeJsonForFabric(JsonNode modpackJson) {

    for (int i = 0; i < modpackJson.get("files").size(); i++) {

      LOG.debug(
          String.format("Mod ID: %s", modpackJson.get("files").get(i).get("projectID").asText()));
      LOG.debug(
          String.format("File ID: %s", modpackJson.get("files").get(i).get("fileID").asText()));

      if (modpackJson.get("files").get(i).get("projectID").asText().equalsIgnoreCase("361988")
          || modpackJson.get("files").get(i).get("fileID").asText().equalsIgnoreCase("306612")) {

        /* This log is meant to be read by the user, therefore we allow translation. */
        LOG.info(I18N.getLocalizedString("configuration.log.info.containsfabric"));
        return true;
      }
    }

    String[] modloaderAndVersion =
        modpackJson.get("minecraft").get("modLoaders").get(0).get("id").asText().split("-");

    return modloaderAndVersion[0].equalsIgnoreCase("fabric");
  }

  /**
   * Acquire a list of directories in a ZIP-file.
   *
   * @param zipURI URI to the ZIP-file from which to gather a list of directories within.
   * @return String List. A list of all directories in the ZIP-file.
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
  public List<String> directoriesInModpackZip(Path zipURI)
      throws IllegalArgumentException, FileSystemAlreadyExistsException, ProviderNotFoundException,
          IOException, SecurityException {
    List<String> directories = new ArrayList<>(100);

    LOG.debug("URI: " + zipURI);

    FileSystem fileSystems = FileSystems.newFileSystem(zipURI, null);

    fileSystems
        .getRootDirectories()
        .forEach(
            root -> {
              LOG.debug("root: " + root);

              try (Stream<Path> paths = Files.walk(root)) {

                paths.forEach(
                    path -> {

                      /*
                       * In regular regex: ^[\/\\]\w+[\/\\]?$
                       * What is this madness?
                       */
                      if (path.toString().matches("^[/\\\\]\\w+[/\\\\]?$")) {
                        LOG.debug("Path in ZIP: " + path);
                        directories.add(path.toString().replace("/", ""));
                      }
                    });

              } catch (IOException ex) {
                LOG.error("No root available.", ex);
              }
            });

    if (directories.isEmpty()) {

      Enumeration<? extends ZipEntry> entries;
      try (ZipFile zipFile = new ZipFile(zipURI.toString())) {
        entries = zipFile.entries();
      }

      while (entries.hasMoreElements()) {
        ZipEntry zipEntry = entries.nextElement();
        LOG.debug("ZIP entry: " + zipEntry.getName());

        /*
         * In regular regex: ^[\/\\]\w+[\/\\]?$
         * You have a problem, so you use regex. Now you have two problems.
         */
        if (zipEntry.getName().matches("^[/\\\\]\\w+[/\\\\]?$")) {
          directories.add(zipEntry.getName());
        }
      }
    }

    fileSystems.close();

    return directories;
  }
}
