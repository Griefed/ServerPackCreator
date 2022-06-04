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

import java.util.ArrayList;
import java.util.List;

/**
 * Class containing all fields and therefore all information either gathered from a configuration file, stored by the creation
 * of a modpack from CurseForge or passed otherwise.
 *
 * @author Griefed
 */
public class ConfigurationModel {

    private List<String> clientMods = new ArrayList<>();
    private List<String> copyDirs = new ArrayList<>();
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
    private JsonNode curseModpack = null;
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
     * @param serverPackSuffix String. The suffix of the server pack to be generated.
     * @author Griefed
     */
    public void setServerPackSuffix(String serverPackSuffix) {
        this.serverPackSuffix = serverPackSuffix;
    }

    /**
     * Getter for a list of clientside-only mods to exclude from server pack.
     *
     * @return List String. Returns the list of clientside-only mods.
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
        newClientMods.removeIf(entry -> entry.matches("\\s+") || entry.length() == 0);
        this.clientMods = newClientMods;
    }

    /**
     * Getter for the list of directories in the modpack to copy to the server pack.
     *
     * @return List String. Returns the list of directories to copy to the server pack.
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
        newCopyDirs.removeIf(entry -> entry.equalsIgnoreCase("server_pack") || entry.matches("\\s+") || entry.length() == 0);
        newCopyDirs.replaceAll(entry -> entry.replace("\\", "/"));
        this.copyDirs = newCopyDirs;
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
     * @param javaArgs String. Sets the Java arguments with which the start-scripts will be generated.
     * @author Griefed
     */
    public void setJavaArgs(String javaArgs) {
        this.javaArgs = javaArgs;
    }

    /**
     * Getter for the JsonNode containing all information about the CurseForge modpack.
     *
     * @return JsonNode. The JsonNode containing all information about the CurseForge modpack.
     * @author Griefed
     */
    public JsonNode getCurseModpack() {
        return curseModpack;
    }

    /**
     * Setter for the JsonNode containing all information about the CurseForge modpack.
     *
     * @param curseModpack JsonNode. The JsonNode containing all information about the CurseForge modpack.
     * @author Griefed
     */
    public void setCurseModpack(JsonNode curseModpack) {
        this.curseModpack = curseModpack;
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
     * @param projectName String. The name of the CurseForge project.
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
     * @param fileName String. The name of the CurseForge project file.
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
     * @param fileName String. The disk-name of the CurseForge project file.
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
     * @param serverIconPath String. The path to the server-icon.png to include in the server pack.
     * @author Griefed
     */
    public void setServerIconPath(String serverIconPath) {
        this.serverIconPath = serverIconPath.replace("\\", "/");
    }

    /**
     * Getter for the path of the server.properties to include in the server pack.
     *
     * @return String. Returns the path to the server.properties to include in the server pack.
     * @author Griefed
     */
    public String getServerPropertiesPath() {
        return serverPropertiesPath;
    }

    /**
     * Setter for the path of the server.properties to include in the server pack.
     *
     * @param serverPropertiesPath String. The path to the server.properties to include in the server pack.
     * @author Griefed
     */
    public void setServerPropertiesPath(String serverPropertiesPath) {
        this.serverPropertiesPath = serverPropertiesPath.replace("\\", "/");
    }

    @Override
    public String toString() {
        return "ConfigurationModel{" +
                "clientMods=" + clientMods +
                ", copyDirs=" + copyDirs +
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
                ", curseModpack=" + curseModpack +
                ", projectName='" + projectName + '\'' +
                ", fileName='" + fileName + '\'' +
                ", fileDiskName='" + fileDiskName + '\'' +
                '}';
    }
}
