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
import com.electronwill.nightconfig.core.file.NoFormatFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import de.griefed.serverpackcreator.utilities.common.Utilities;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

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
      HashMap<String, String> scriptSettings) {

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
  }

  /**
   * Create a new configuration model from a config file.
   *
   * @param utilities  Instance of our SPC utilities.
   * @param configFile Configuration file to load.
   * @throws FileNotFoundException if the specified file can not be found.
   * @author Griefed
   */
  public ConfigurationModel(Utilities utilities, File configFile)
      throws FileNotFoundException, NoFormatFoundException {

    if (!configFile.exists()) {
      throw new FileNotFoundException("Couldn't find file: " + configFile);
    }

    FileConfig config = FileConfig.of(configFile);

    config.load();

    setClientMods(config.getOrElse("clientMods", Collections.singletonList("")));
    setCopyDirs(config.getOrElse("copyDirs", Collections.singletonList("")));
    setModpackDir(config.getOrElse("modpackDir", "").replace("\\", "/"));
    setJavaPath(config.getOrElse("javaPath", "").replace("\\", "/"));

    setMinecraftVersion(config.getOrElse("minecraftVersion", ""));
    setModLoader(config.getOrElse("modLoader", ""));
    setModLoaderVersion(config.getOrElse("modLoaderVersion", ""));
    setJavaArgs(config.getOrElse("javaArgs", "empty"));

    setServerPackSuffix(
        utilities.StringUtils().pathSecureText(config.getOrElse("serverPackSuffix", "")));
    setServerIconPath(config.getOrElse("serverIconPath", "").replace("\\", "/"));
    setServerPropertiesPath(config.getOrElse("serverPropertiesPath", "").replace("\\", "/"));

    setIncludeServerInstallation(
        utilities.BooleanUtils()
            .convert(
                String.valueOf(config.getOrElse("includeServerInstallation", "False"))));
    setIncludeServerIcon(
        utilities.BooleanUtils()
            .convert(String.valueOf(config.getOrElse("includeServerIcon", "False"))));
    setIncludeServerProperties(
        utilities.BooleanUtils()
            .convert(
                String.valueOf(config.getOrElse("includeServerProperties", "False"))));
    setIncludeZipCreation(
        utilities.BooleanUtils()
            .convert(String.valueOf(config.getOrElse("includeZipCreation", "False"))));
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
    if (newModLoader.toLowerCase().contains("forge")) {

      this.modLoader = "Forge";

    } else if (newModLoader.toLowerCase().contains("fabric")) {

      this.modLoader = "Fabric";

    } else if (newModLoader.toLowerCase().contains("quilt")) {

      this.modLoader = "Quilt";
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
   * @param scriptSettings Key-value pairs to be used in script creation.
   * @author Griefed
   */
  public void setScriptSettings(HashMap<String, String> scriptSettings) {
    this.scriptSettings.clear();
    this.scriptSettings.putAll(scriptSettings);
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
