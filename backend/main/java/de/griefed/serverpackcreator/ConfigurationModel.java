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

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.core.file.NoFormatFoundException;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.fasterxml.jackson.databind.JsonNode;
import de.griefed.serverpackcreator.utilities.common.Utilities;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class containing all fields and therefore all information either gathered from a configuration
 * file, stored by the creation of a modpack from CurseForge or passed otherwise.
 *
 * @author Griefed
 */
public class ConfigurationModel {

  private final List<String> clientMods = new ArrayList<>();
  private final List<String> copyDirs = new ArrayList<>();
  private final HashMap<String, String> scriptSettings = new HashMap<>();
  private final HashMap<String, ArrayList<CommentedConfig>> addonsConfigs = new HashMap<>();
  private String modpackDir = "";
  private String javaPath = "";
  private String minecraftVersion = "";
  private String modLoader = "";
  private String modLoaderVersion = "";
  private String javaArgs = "";
  private String serverPackSuffix = "";
  private String serverIconPath = "";
  private String serverPropertiesPath = "";
  private Boolean includeServerInstallation = true;
  private Boolean includeServerIcon = true;
  private Boolean includeServerProperties = true;
  private Boolean includeZipCreation = true;
  private JsonNode modpackJson = null;
  private String projectName;
  private String fileName;
  private String fileDiskName;

  /**
   * Constructor for our ConfigurationModel.
   *
   * @author Griefed
   */
  public ConfigurationModel() {
  }

  /**
   * Construct a new configuration model with custom values.
   *
   * @param clientMods                List of clientside mods to exclude from the server pack.
   * @param copyDirs                  List of directories and/or files to include in the server
   *                                  pack.
   * @param modpackDir                The path to the modpack.
   * @param javaPath                  The path to the java installation used for modloader server
   *                                  installation.
   * @param minecraftVersion          The Minecraft version the modpack uses.
   * @param modLoader                 The modloader the modpack uses. Either
   *                                  <code>Forge</code>,
   *                                  <code>Fabric</code> or <code>Quilt</code>.
   * @param modLoaderVersion          The modloader version the modpack uses.
   * @param javaArgs                  JVM flags to create the start scripts with.
   * @param serverPackSuffix          Suffix to create the server pack with.
   * @param serverIconPath            Path to the icon to use in the server pack.
   * @param serverPropertiesPath      Path to the server.properties to create the server pack with.
   * @param includeServerInstallation Whether to install the modloader server in the server pack.
   * @param includeServerIcon         Whether to include the server-icon.png in the server pack.
   * @param includeServerProperties   Whether to include the server.properties in the server pack.
   * @param includeZipCreation        Whether to create a ZIP-archive of the server pack.
   * @param scriptSettings            Map containing key-value pairs to be used in start script
   *                                  creation.
   * @param addonsConfigs             Configuration for any and all addons used by this
   *                                  configuration.
   * @author Griefed
   */
  public ConfigurationModel(
      List<String> clientMods,
      List<String> copyDirs,
      String modpackDir,
      String javaPath,
      String minecraftVersion,
      String modLoader,
      String modLoaderVersion,
      String javaArgs,
      String serverPackSuffix,
      String serverIconPath,
      String serverPropertiesPath,
      boolean includeServerInstallation,
      boolean includeServerIcon,
      boolean includeServerProperties,
      boolean includeZipCreation,
      HashMap<String, String> scriptSettings,
      HashMap<String, ArrayList<CommentedConfig>> addonsConfigs) {

    this.clientMods.addAll(clientMods);
    this.copyDirs.addAll(copyDirs);
    this.modpackDir = modpackDir;
    this.javaPath = javaPath;
    this.minecraftVersion = minecraftVersion;
    this.modLoader = modLoader;
    this.modLoaderVersion = modLoaderVersion;
    this.javaArgs = javaArgs;
    this.serverPackSuffix = serverPackSuffix;
    this.serverIconPath = serverIconPath;
    this.serverPropertiesPath = serverPropertiesPath;
    this.includeServerInstallation = includeServerInstallation;
    this.includeServerIcon = includeServerIcon;
    this.includeServerProperties = includeServerProperties;
    this.includeZipCreation = includeZipCreation;
    this.scriptSettings.putAll(scriptSettings);
    this.addonsConfigs.putAll(addonsConfigs);
  }

  /**
   * Create a new configuration model from a config file.
   *
   * @param utilities  Instance of our SPC utilities.
   * @param configFile Configuration file to load.
   * @throws FileNotFoundException  if the specified file can not be found.
   * @throws NoFormatFoundException if the configuration format could not be determined by
   *                                Night-Config.
   * @author Griefed
   */
  public ConfigurationModel(Utilities utilities, File configFile)
      throws FileNotFoundException, NoFormatFoundException {

    if (!configFile.exists()) {
      throw new FileNotFoundException("Couldn't find file: " + configFile);
    }

    FileConfig config = FileConfig.of(configFile, TomlFormat.instance());

    config.load();

    setClientMods(config.getOrElse("clientMods", Collections.singletonList("")));
    setCopyDirs(config.getOrElse("copyDirs", Collections.singletonList("")));
    setModpackDir(config.getOrElse("modpackDir", "").replace("\\", "/"));
    setJavaPath(config.getOrElse("javaPath", "").replace("\\", "/"));

    setMinecraftVersion(config.getOrElse("minecraftVersion", ""));
    setModLoader(config.getOrElse("modLoader", ""));
    setModLoaderVersion(config.getOrElse("modLoaderVersion", ""));
    setJavaArgs(config.getOrElse("javaArgs", ""));

    setServerPackSuffix(
        utilities.StringUtils().pathSecureText(config.getOrElse("serverPackSuffix", "")));
    setServerIconPath(config.getOrElse("serverIconPath", "").replace("\\", "/"));
    setServerPropertiesPath(config.getOrElse("serverPropertiesPath", "").replace("\\", "/"));

    setIncludeServerInstallation(config.getOrElse("includeServerInstallation", false));
    setIncludeServerIcon(config.getOrElse("includeServerIcon", false));
    setIncludeServerProperties(config.getOrElse("includeServerProperties", false));
    setIncludeZipCreation(config.getOrElse("includeZipCreation", false));

    try {
      for (Map.Entry<String, Object> entry : ((CommentedConfig) config.get("addons")).valueMap()
          .entrySet()) {
        addonsConfigs.put(entry.getKey(), (ArrayList<CommentedConfig>) entry.getValue());
      }
    } catch (Exception ignored) {

    }

    try {
      for (Map.Entry<String, Object> entry : ((CommentedConfig) config.get("scripts")).valueMap()
          .entrySet()) {
        scriptSettings.put(entry.getKey(), entry.getValue().toString());
      }
    } catch (Exception ignored) {

    }

    config.close();
  }

  /**
   * Save this configuration.
   *
   * @param destination The file to store the configuration in.
   * @return The configuration for further operations.
   * @author Griefed
   */
  public ConfigurationModel save(File destination) {

    CommentedConfig conf = TomlFormat.instance().createConfig();
    conf.set("includeServerInstallation",
        getIncludeServerInstallation());
    conf.setComment("includeServerInstallation",
        " Whether to install a Forge/Fabric/Quilt server for the serverpack. Must be true or false.\n Default value is true.");

    conf.setComment("serverIconPath",
        " Path to a custom server-icon.png-file to include in the server pack.");
    conf.set("serverIconPath", getServerIconPath());

    conf.setComment("copyDirs",
        " Name of directories or files to include in serverpack.\n When specifying \"saves/world_name\", \"world_name\" will be copied to the base directory of the serverpack\n for immediate use with the server. Automatically set when projectID,fileID for modpackDir has been specified.\n Example: [config,mods,scripts]");
    conf.set("copyDirs", getCopyDirs());

    conf.setComment("serverPackSuffix",
        " Suffix to append to the server pack to be generated. Can be left blank/empty.");
    conf.set("serverPackSuffix", getServerPackSuffix());

    conf.setComment("clientMods",
        " List of client-only mods to delete from serverpack.\n No need to include version specifics. Must be the filenames of the mods, not their project names on CurseForge!\n Example: [AmbientSounds-,ClientTweaks-,PackMenu-,BetterAdvancement-,jeiintegration-]");
    conf.set("clientMods", getClientMods());

    conf.setComment("serverPropertiesPath",
        " Path to a custom server.properties-file to include in the server pack.");
    conf.set("serverPropertiesPath", getServerPropertiesPath());

    conf.setComment("includeServerProperties",
        " Include a server.properties in your serverpack. Must be true or false.\n If no server.properties is provided but is set to true, a default one will be provided.\n Default value is true.");
    conf.set("includeServerProperties", getIncludeServerProperties());

    conf.setComment("javaArgs",
        " Java arguments to set in the start-scripts for the generated server pack. Default value is \"empty\".\n Leave as \"empty\" to not have Java arguments in your start-scripts.");
    conf.set("javaArgs", getJavaArgs());

    conf.setComment("javaPath",
        " Path to the Java executable. On Linux systems it would be something like \"/usr/bin/java\".\n Only needed if includeServerInstallation is true.");
    conf.set("javaPath", getJavaPath());

    conf.setComment("modpackDir",
        " Path to your modpack. Can be either relative or absolute.\n Example: \"./Some Modpack\" or \"C:/Minecraft/Some Modpack\"");
    conf.set("modpackDir", getModpackDir());

    conf.setComment("includeServerIcon",
        " Include a server-icon.png in your serverpack. Must be true or false\n Default value is true.");
    conf.set("includeServerIcon", getIncludeServerIcon());

    conf.setComment("includeZipCreation",
        " Create zip-archive of serverpack. Must be true or false.\n Default value is true.");
    conf.set("includeZipCreation", getIncludeZipCreation());

    conf.setComment("modLoaderVersion",
        " The version of the modloader you want to install. Example for Fabric=\"0.7.3\", example for Forge=\"36.0.15\".\n Automatically set when projectID,fileID for modpackDir has been specified.\n Only needed if includeServerInstallation is true.");
    conf.set("modLoaderVersion", getModLoaderVersion());

    conf.setComment("minecraftVersion",
        " Which Minecraft version to use. Example: \"1.16.5\".\n Automatically set when projectID,fileID for modpackDir has been specified.\n Only needed if includeServerInstallation is true.");
    conf.set("minecraftVersion", getMinecraftVersion());

    conf.setComment("modLoader",
        " Which modloader to install. Must be either \"Forge\", \"Fabric\", \"Quilt\" or \"LegacyFabric\".\n Automatically set when projectID,fileID for modpackDir has been specified.\n Only needed if includeServerInstallation is true.");
    conf.set("modLoader", getModLoader());

    Config addons = TomlFormat.newConfig();
    addons.valueMap().putAll(getAddonsConfigs());

    conf.setComment("addons",
        " Configurations for any and all addons installed and used by this configuration.");
    conf.setComment("addons"," Settings related to addons. An addon is identified by its ID.");
    conf.set("addons", addons);

    Config scripts = TomlFormat.newConfig();
    for (Map.Entry<String, String> entry : scriptSettings.entrySet()) {
      if (!entry.getKey().equals("SPC_SERVERPACKCREATOR_VERSION_SPC") && !entry.getKey()
          .equals("SPC_MINECRAFT_VERSION_SPC") && !entry.getKey()
          .equals("SPC_MODLOADER_SPC") && !entry.getKey()
          .equals("SPC_MODLOADER_VERSION_SPC") && !entry.getKey()
          .equals("SPC_JAVA_ARGS_SPC") && !entry.getKey()
          .equals("SPC_JAVA_SPC") && !entry.getKey()
          .equals("SPC_FABRIC_INSTALLER_VERSION_SPC") && !entry.getKey()
          .equals("SPC_QUILT_INSTALLER_VERSION_SPC") && !entry.getKey()
          .equals("SPC_LEGACYFABRIC_INSTALLER_VERSION_SPC")) {
        scripts.set(entry.getKey(),entry.getValue());
      }
    }
    conf.setComment("scripts"," Key-value pairs for start scripts. A given key in a start script is replaced with the value.");
    conf.add("scripts",scripts);

    TomlFormat.instance().createWriter()
        .write(conf, destination, WritingMode.REPLACE, StandardCharsets.UTF_8);

    return this;
  }

  /**
   * Get the configurations for various addons.
   *
   * @return A hashmap containing configurations for various addons.
   * @author Griefed
   */
  public HashMap<String, ArrayList<CommentedConfig>> getAddonsConfigs() {
    return addonsConfigs;
  }

  /**
   * Clear and set the configs for addons.
   *
   * @param addonsConfigs The new configurations for various addons.
   * @author Griefed
   */
  public void setAddonsConfigs(HashMap<String, ArrayList<CommentedConfig>> addonsConfigs) {
    this.addonsConfigs.clear();
    this.addonsConfigs.putAll(addonsConfigs);
  }

  /**
   * Set the configuration list for a specific addon, identified by the addons ID.
   *
   * @param addonId    The ID of the addon.
   * @param configList The list of configurations for the specific addon.
   * @author Griefed
   */
  public void setAddonConfigs(String addonId, ArrayList<CommentedConfig> configList) {
    this.addonsConfigs.put(addonId, configList);
  }

  /**
   * Get the list of configurations for a specific addon. If no configurations are available, an
   * empty list will be returned.
   *
   * @param addonId The ID of the addon.
   * @return A list of configuration for the specified addon. If none are available, an empty list
   * will be returned.
   * @author Griefed
   */
  public ArrayList<CommentedConfig> getAddonConfigs(String addonId) {
    if (addonsConfigs.containsKey(addonId)) {
      return addonsConfigs.get(addonId);
    } else {
      return new ArrayList<>();
    }
  }

  /**
   * Getter for the suffix of the server pack to be generated.
   *
   * @return String. Returns the suffix for the server pack to be generated.
   * @author Griefed
   */
  public String getServerPackSuffix() {
    return serverPackSuffix;
  }

  /**
   * Setter for the suffix of the server pack to be generated
   *
   * @param serverPackSuffix The suffix of the server pack to be generated.
   * @author Griefed
   */
  public void setServerPackSuffix(String serverPackSuffix) {
    this.serverPackSuffix = serverPackSuffix;
  }

  /**
   * Getter for a list of clientside-only mods to exclude from server pack.
   *
   * @return Returns the list of clientside-only mods.
   * @author Griefed
   */
  public List<String> getClientMods() {
    return clientMods;
  }

  /**
   * Setter for the list of clientside-only mods to exclude from server pack.
   *
   * @param newClientMods The new list of clientside-only mods to store.
   * @author Griefed
   */
  public void setClientMods(List<String> newClientMods) {
    this.clientMods.clear();
    newClientMods.removeIf(entry ->
        entry.matches("\\s+") || entry.length() == 0
    );
    this.clientMods.addAll(newClientMods);
  }

  /**
   * Getter for the list of directories in the modpack to copy to the server pack.
   *
   * @return Returns the list of directories to copy to the server pack.
   * @author Griefed
   */
  public List<String> getCopyDirs() {
    return copyDirs;
  }

  /**
   * Setter for the list of directories in the modpack to copy to the server pack.
   *
   * @param newCopyDirs The new list of directories to include in server pack to store.
   * @author Griefed
   */
  public void setCopyDirs(List<String> newCopyDirs) {
    this.copyDirs.clear();
    newCopyDirs.removeIf(
        entry ->
            entry.equalsIgnoreCase("server_pack") || entry.matches("\\s+") || entry.length() == 0);
    newCopyDirs.replaceAll(entry -> entry.replace("\\", "/"));
    this.copyDirs.addAll(newCopyDirs);
  }

  /**
   * Getter for the path to the modpack directory.
   *
   * @return String. Returns the path to the modpack directory.
   * @author Griefed
   */
  public String getModpackDir() {
    return modpackDir;
  }

  /**
   * Setter for the path to the modpack directory. Replaces any occurrences of \ with /.
   *
   * @param newModpackDir The new modpack directory path to store.
   * @author Griefed
   */
  public void setModpackDir(String newModpackDir) {
    this.modpackDir = newModpackDir.replace("\\", "/");
  }

  /**
   * Getter for the path to the Java executable/binary.
   *
   * @return String. Returns the path to the Java executable/binary.
   * @author Griefed
   */
  public String getJavaPath() {
    return javaPath;
  }

  /**
   * Setter for the path to the Java executable/binary. Replaces any occurrences of \ with /.
   *
   * @param newJavaPath The new Java path to store.
   * @author Griefed
   */
  public void setJavaPath(String newJavaPath) {
    this.javaPath = newJavaPath.replace("\\", "/");
  }

  /**
   * Getter for the version of Minecraft used by the modpack.
   *
   * @return String. Returns the Minecraft version used in the modpack.
   * @author Griefed
   */
  public String getMinecraftVersion() {
    return minecraftVersion;
  }

  /**
   * Setter for the Minecraft version used by the modpack.
   *
   * @param newMinecraftVersion The new Minecraft version to store.
   * @author Griefed
   */
  public void setMinecraftVersion(String newMinecraftVersion) {
    this.minecraftVersion = newMinecraftVersion;
  }

  /**
   * Getter for the modloader used by the modpack.
   *
   * @return String. Returns the modloader used by the modpack.
   * @author Griefed
   */
  public String getModLoader() {
    return modLoader;
  }

  /**
   * Setter for the modloader used by the modpack.
   *
   * @param newModLoader The new modloader to store.
   * @author Griefed
   */
  public void setModLoader(String newModLoader) {
    if (newModLoader.toLowerCase().matches("^forge$")) {

      this.modLoader = "Forge";

    } else if (newModLoader.toLowerCase().matches("^fabric$")) {

      this.modLoader = "Fabric";

    } else if (newModLoader.toLowerCase().matches("^quilt$")) {

      this.modLoader = "Quilt";

    } else if (newModLoader.toLowerCase().matches("^legacyfabric$")) {

      this.modLoader = "LegacyFabric";
    }
  }

  /**
   * Getter for the version of the modloader used by the modpack.
   *
   * @return String. Returns the version of the modloader used by the modpack.
   * @author Griefed
   */
  public String getModLoaderVersion() {
    return modLoaderVersion;
  }

  /**
   * Setter for the version of the modloader used by the modpack.
   *
   * @param newModLoaderVersion The new modloader version to store.
   * @author Griefed
   */
  public void setModLoaderVersion(String newModLoaderVersion) {
    this.modLoaderVersion = newModLoaderVersion;
  }

  /**
   * Getter for whether the modloader server installation should be included.
   *
   * @return Boolean. Returns whether the server installation should be included.
   * @author Griefed
   */
  public boolean getIncludeServerInstallation() {
    return includeServerInstallation;
  }

  /**
   * Setter for whether the modloader server installation should be included.
   *
   * @param newIncludeServerInstallation The new boolean to store.
   * @author Griefed
   */
  public void setIncludeServerInstallation(boolean newIncludeServerInstallation) {
    this.includeServerInstallation = newIncludeServerInstallation;
  }

  /**
   * Getter for whether the server-icon.png should be included in the server pack.
   *
   * @return Boolean. Returns whether the server-icon.png should be included in the server pack.
   * @author Griefed
   */
  public boolean getIncludeServerIcon() {
    return includeServerIcon;
  }

  /**
   * Setter for whether the server-icon.png should be included in the server pack.
   *
   * @param newIncludeServerIcon The new boolean to store.
   * @author Griefed
   */
  public void setIncludeServerIcon(boolean newIncludeServerIcon) {
    this.includeServerIcon = newIncludeServerIcon;
  }

  /**
   * Getter for whether the server.properties should be included in the server pack.
   *
   * @return Boolean. Returns whether the server.properties should be included in the server pack.
   * @author Griefed
   */
  public boolean getIncludeServerProperties() {
    return includeServerProperties;
  }

  /**
   * Setter for whether the server.properties should be included in the server pack.
   *
   * @param newIncludeServerProperties The new boolean to store.
   * @author Griefed
   */
  public void setIncludeServerProperties(boolean newIncludeServerProperties) {
    this.includeServerProperties = newIncludeServerProperties;
  }

  /**
   * Getter for whether a ZIP-archive of the server pack should be created.
   *
   * @return Boolean. Returns whether a ZIP-archive of the server pack should be created.
   * @author Griefed
   */
  public boolean getIncludeZipCreation() {
    return includeZipCreation;
  }

  /**
   * Setter for whether a ZIP-archive of the server pack should be created.
   *
   * @param newIncludeZipCreation The new boolean to store.
   * @author Griefed
   */
  public void setIncludeZipCreation(boolean newIncludeZipCreation) {
    this.includeZipCreation = newIncludeZipCreation;
  }

  /**
   * Getter for the Java arguments with which the start-scripts will be generated.
   *
   * @return String. Returns the Java arguments with which the start-scripts will be generated.
   * @author Griefed
   */
  public String getJavaArgs() {
    return javaArgs;
  }

  /**
   * Setter for the Java arguments with which the start-scripts will be generated.
   *
   * @param javaArgs Sets the Java arguments with which the start-scripts will be generated.
   * @author Griefed
   */
  public void setJavaArgs(String javaArgs) {
    this.javaArgs = javaArgs;
  }

  /**
   * Getter for the JsonNode containing all information about the modpack. May be from various *
   * sources, so be careful when using this.
   *
   * @return JsonNode. The JsonNode containing all information about the modpack.
   * @author Griefed
   */
  public JsonNode getModpackJson() {
    return modpackJson;
  }

  /**
   * Setter for the JsonNode containing all information about the modpack. May be from various
   * sources, so be careful when using this.
   *
   * @param modpackJson JsonNode. The JsonNode containing all information about the modpack.
   * @author Griefed
   */
  public void setModpackJson(JsonNode modpackJson) {
    this.modpackJson = modpackJson;
  }

  /**
   * Getter for the name of the CurseForge project.
   *
   * @return String. The name of the CurseForge project.
   * @author Griefed
   */
  public String getProjectName() {
    return projectName;
  }

  /**
   * Setter for the name of the CurseForge project.
   *
   * @param projectName The name of the CurseForge project.
   * @author Griefed
   */
  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  /**
   * Getter for the name of the CurseForge project file.
   *
   * @return String. The name of the CurseForge project file.
   * @author Griefed
   */
  public String getFileName() {
    return fileName;
  }

  /**
   * Setter for the name of the CurseForge project file.
   *
   * @param fileName The name of the CurseForge project file.
   * @author Griefed
   */
  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  /**
   * Getter for the disk-name of the CurseForge project file.
   *
   * @return String. The disk-name of the CurseForge project file.
   * @author Griefed
   */
  public String getFileDiskName() {
    return fileDiskName;
  }

  /**
   * Setter for the disk-name of the CurseForge project file.
   *
   * @param fileName The disk-name of the CurseForge project file.
   * @author Griefed
   */
  public void setFileDiskName(String fileName) {
    this.fileDiskName = fileName;
  }

  /**
   * Getter for the path to the server-icon.png to include in the server pack.
   *
   * @return String. Returns the path to the server-icon.png.
   * @author Griefed
   */
  public String getServerIconPath() {
    return serverIconPath;
  }

  /**
   * Setter for the path of the server-icon.png to include in the server pack.
   *
   * @param serverIconPath The path to the server-icon.png to include in the server pack.
   * @author Griefed
   */
  public void setServerIconPath(String serverIconPath) {
    this.serverIconPath = serverIconPath.replace("\\", "/");
  }

  /**
   * Getter for the path of the server.properties to include in the server pack.
   *
   * @return Returns the path to the server.properties to include in the server pack.
   * @author Griefed
   */
  public String getServerPropertiesPath() {
    return serverPropertiesPath;
  }

  /**
   * Setter for the path of the server.properties to include in the server pack.
   *
   * @param serverPropertiesPath The path to the server.properties to include in the server pack.
   * @author Griefed
   */
  public void setServerPropertiesPath(String serverPropertiesPath) {
    this.serverPropertiesPath = serverPropertiesPath.replace("\\", "/");
  }

  /**
   * Getter for the script settings used during script creation.
   *
   * @return Map of all script settings and their values.
   * @author Griefed
   */
  public HashMap<String, String> getScriptSettings() {
    return scriptSettings;
  }

  /**
   * Putter for the script settings used during script creation. All key-value pairs from the passed
   * hashmap are put into this models hashmap.
   *
   * @param settings Key-value pairs to be used in script creation.
   * @author Griefed
   */
  public void setScriptSettings(HashMap<String, String> settings) {
    this.scriptSettings.clear();
    this.scriptSettings.putAll(settings);
  }

  @Override
  public String toString() {
    return "ConfigurationModel{" +
        "clientMods=" + clientMods +
        ", copyDirs=" + copyDirs +
        ", scriptSettings=" + scriptSettings +
        ", modpackDir='" + modpackDir + '\'' +
        ", javaPath='" + javaPath + '\'' +
        ", minecraftVersion='" + minecraftVersion + '\'' +
        ", modLoader='" + modLoader + '\'' +
        ", modLoaderVersion='" + modLoaderVersion + '\'' +
        ", javaArgs='" + javaArgs + '\'' +
        ", serverPackSuffix='" + serverPackSuffix + '\'' +
        ", serverIconPath='" + serverIconPath + '\'' +
        ", serverPropertiesPath='" + serverPropertiesPath + '\'' +
        ", includeServerInstallation=" + includeServerInstallation +
        ", includeServerIcon=" + includeServerIcon +
        ", includeServerProperties=" + includeServerProperties +
        ", includeZipCreation=" + includeZipCreation +
        ", modpackJson=" + modpackJson +
        ", projectName='" + projectName + '\'' +
        ", fileName='" + fileName + '\'' +
        ", fileDiskName='" + fileDiskName + '\'' +
        '}';
  }
}
