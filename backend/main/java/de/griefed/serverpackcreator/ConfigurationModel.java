/* Copyright (C) 2021  Griefed
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
import de.griefed.serverpackcreator.curseforge.CurseCreateModpack;

import java.util.ArrayList;
import java.util.List;

/**
 * Class containing all fields and therefore all information either gathered from a configuration file, stored by the creation
 * of a modpack from CurseForge or passed otherwise.
 * @author Griefed
 */
public class ConfigurationModel {

    /**
     * Constructor for our ConfigurationModel.
     * @author Griefed
     */
    public ConfigurationModel() {

    }

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

    private JsonNode curseModpack;

    private Boolean includeServerInstallation = true;
    private Boolean includeServerIcon = true;
    private Boolean includeServerProperties = true;
    private Boolean includeZipCreation = true;

    private String projectName;
    private String fileName;
    private String fileDiskName;

    private int projectID;
    private int fileID;

    /**
     * Getter for the suffix of the server pack to be generated.
     * @author Griefed
     * @return String. Returns the suffix for the server pack to be generated.
     */
    public String getServerPackSuffix() {
        return serverPackSuffix;
    }

    /**
     * Setter for the suffix of the server pack to be generated
     * @author Griefed
     * @param serverPackSuffix String. The suffix of the server pack to be generated.
     */
    public void setServerPackSuffix(String serverPackSuffix) {
        this.serverPackSuffix = serverPackSuffix;
    }

    /**
     * Getter for a list of clientside-only mods to exclude from server pack.
     * @author Griefed
     * @return List String. Returns the list of clientside-only mods.
     */
    public List<String> getClientMods() {
        return clientMods;
    }

    /**
     * Setter for the list of clientside-only mods to exclude from server pack.
     * @author Griefed
     * @param newClientMods The new list of clientside-only mods to store.
     */
    public void setClientMods(List<String> newClientMods) {
        this.clientMods = newClientMods;
    }

    /**
     * Getter for the list of directories in the modpack to copy to the server pack.
     * @author Griefed
     * @return List String. Returns the list of directories to copy to the server pack.
     */
    public List<String> getCopyDirs() {
        return copyDirs;
    }

    /**
     * Setter for the list of directories in the modpack to copy to the server pack.
     * @author Griefed
     * @param newCopyDirs The new list of directories to include in server pack to store.
     */
    public void setCopyDirs(List<String> newCopyDirs) {
        for (int i = 0; i < newCopyDirs.size(); i++) {
            newCopyDirs.removeIf(n -> (n.equalsIgnoreCase("server_pack")));
            newCopyDirs.replaceAll(n -> n.replace("\\","/"));
        }
        this.copyDirs = newCopyDirs;
    }

    /**
     * Getter for the path to the modpack directory.
     * @author Griefed
     * @return String. Returns the path to the modpack directory.
     */
    public String getModpackDir() {
        return modpackDir;
    }

    /**
     * Setter for the path to the modpack directory. Replaces any occurrences of \ with /.
     * @author Griefed
     * @param newModpackDir The new modpack directory path to store.
     */
    public void setModpackDir(String newModpackDir) {
        newModpackDir = newModpackDir.replace("\\","/");
        this.modpackDir = newModpackDir;
    }

    /**
     * Getter for the path to the Java executable/binary.
     * @author Griefed
     * @return String. Returns the path to the Java executable/binary.
     */
    public String getJavaPath() {
        return javaPath;
    }

    /**
     * Setter for the path to the Java executable/binary. Replaces any occurrences of \ with /.
     * @author Griefed
     * @param newJavaPath The new Java path to store.
     */
    public void setJavaPath(String newJavaPath) {
        newJavaPath = newJavaPath.replace("\\", "/");
        this.javaPath = newJavaPath;
    }

    /**
     * Getter for the version of Minecraft used by the modpack.
     * @author Griefed
     * @return String. Returns the Minecraft version used in the modpack.
     */
    public String getMinecraftVersion() {
        return minecraftVersion;
    }

    /**
     * Setter for the Minecraft version used by the modpack.
     * @author Griefed
     * @param newMinecraftVersion The new Minecraft version to store.
     */
    public void setMinecraftVersion(String newMinecraftVersion) {
        this.minecraftVersion = newMinecraftVersion;
    }

    /**
     * Getter for the modloader used by the modpack.
     * @author Griefed
     * @return String. Returns the modloader used by the modpack.
     */
    public String getModLoader() {
        return modLoader;
    }

    /**
     * Setter for the modloader used by the modpack.
     * @author Griefed
     * @param newModLoader The new modloader to store.
     */
    public void setModLoader(String newModLoader) {
        if (newModLoader.equalsIgnoreCase("Forge")) {

            newModLoader = "Forge";

        } else if (newModLoader.equalsIgnoreCase("Fabric")) {

            newModLoader = "Fabric";

        } else if (newModLoader.toLowerCase().contains("forge")) {

            newModLoader = "Forge";

        } else if (newModLoader.toLowerCase().contains("fabric")) {

            newModLoader = "Fabric";
        }
        this.modLoader = newModLoader;
    }

    /**
     * Getter for the version of the modloader used by the modpack.
     * @author Griefed
     * @return String. Returns the version of the modloader used by the modpack.
     */
    public String getModLoaderVersion() {
        return modLoaderVersion;
    }

    /**
     * Setter for the version of the modloader used by the modpack.
     * @author Griefed
     * @param newModLoaderVersion The new modloader version to store.
     */
    public void setModLoaderVersion(String newModLoaderVersion) {
        this.modLoaderVersion = newModLoaderVersion;
    }

    /**
     * Getter for whether the modloader server installation should be included.
     * @author Griefed
     * @return Boolean. Returns whether the server installation should be included.
     */
    public boolean getIncludeServerInstallation() {
        return includeServerInstallation;
    }

    /**
     * Setter for whether the modloader server installation should be included.
     * @author Griefed
     * @param newIncludeServerInstallation The new boolean to store.
     */
    public void setIncludeServerInstallation(boolean newIncludeServerInstallation) {
        this.includeServerInstallation = newIncludeServerInstallation;
    }

    /**
     * Getter for whether the server-icon.png should be included in the server pack.
     * @author Griefed
     * @return Boolean. Returns whether the server-icon.png should be included in the server pack.
     */
    public boolean getIncludeServerIcon() {
        return includeServerIcon;
    }

    /**
     * Setter for whether the server-icon.png should be included in the server pack.
     * @author Griefed
     * @param newIncludeServerIcon The new boolean to store.
     */
    public void setIncludeServerIcon(boolean newIncludeServerIcon) {
        this.includeServerIcon = newIncludeServerIcon;
    }

    /**
     * Getter for whether the server.properties should be included in the server pack.
     * @author Griefed
     * @return Boolean. Returns whether the server.properties should be included in the server pack.
     */
    public boolean getIncludeServerProperties() {
        return includeServerProperties;
    }

    /**
     * Setter for whether the server.properties should be included in the server pack.
     * @author Griefed
     * @param newIncludeServerProperties The new boolean to store.
     */
    public void setIncludeServerProperties(boolean newIncludeServerProperties) {
        this.includeServerProperties = newIncludeServerProperties;
    }

    /**
     * Getter for whether a ZIP-archive of the server pack should be created.
     * @author Griefed
     * @return Boolean. Returns whether a ZIP-archive of the server pack should be created.
     */
    public boolean getIncludeZipCreation() {
        return includeZipCreation;
    }

    /**
     * Setter for whether a ZIP-archive of the server pack should be created.
     * @author Griefed
     * @param newIncludeZipCreation The new boolean to store.
     */
    public void setIncludeZipCreation(boolean newIncludeZipCreation) {
        this.includeZipCreation = newIncludeZipCreation;
    }

    /**
     * Getter for the Java arguments with which the start-scripts will be generated.
     * @author Griefed
     * @return String. Returns the Java arguments with which the start-scripts will be generated.
     */
    public String getJavaArgs() {
        return javaArgs;
    }

    /**
     * Setter for the Java arguments with which the start-scripts will be generated.
     * @author Griefed
     * @param javaArgs String. Sets the Java arguments with which the start-scripts will be generated.
     */
    public void setJavaArgs(String javaArgs) {
        this.javaArgs = javaArgs;
    }

    /**
     * Getter for the JsonNode containing all information about the CurseForge modpack.
     * @author Griefed
     * @return JsonNode. The JsonNode containing all information about the CurseForge modpack.
     */
    public JsonNode getCurseModpack() {
        return curseModpack;
    }

    /**
     * Setter for the JsonNode containing all information about the CurseForge modpack.
     * @author Griefed
     * @param curseModpack JsonNode. The JsonNode containing all information about the CurseForge modpack.
     */
    public void setCurseModpack(JsonNode curseModpack) {
        this.curseModpack = curseModpack;
    }

    /**
     * Getter for the CurseForge projectID of a modpack, which will be created by {@link CurseCreateModpack#curseForgeModpack(ConfigurationModel, Integer, Integer)}.
     * @author Griefed
     * @return Integer. Returns the CurseForge projectID of a modpack.
     */
    public int getProjectID() {
        return projectID;
    }

    /**
     * Setter for the CurseForge projectID of a modpack, which will be created by {@link CurseCreateModpack#curseForgeModpack(ConfigurationModel, Integer, Integer)}.
     * @author Griefed
     * @param newProjectID The new projectID to store.
     */
    public void setProjectID(int newProjectID) {
        this.projectID = newProjectID;
    }

    /**
     * Getter for the CurseForge file of a modpack, which will be created by {@link CurseCreateModpack#curseForgeModpack(ConfigurationModel, Integer, Integer)}.
     * @author Griefed
     * @return Integer. Returns the CurseForge fileID of a modpack.
     */
    public int getFileID() {
        return fileID;
    }

    /**
     * Setter for the CurseForge file of a modpack, which will be created by {@link CurseCreateModpack#curseForgeModpack(ConfigurationModel, Integer, Integer)}.
     * @author Griefed
     * @param newFileID The new projectFileID to store.
     */
    public void setFileID(int newFileID) {
        this.fileID = newFileID;
    }

    /**
     * Getter for the name of the CurseForge project.
     * @author Griefed
     * @return String. The name of the CurseForge project.
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     * Setter for the name of the CurseForge project.
     * @author Griefed
     * @param projectName String. The name of the CurseForge project.
     */
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    /**
     * Getter for the name of the CurseForge project file.
     * @author Griefed
     * @return String. The name of the CurseForge project file.
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Setter for the name of the CurseForge project file.
     * @author Griefed
     * @param fileName String. The name of the CurseForge project file.
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Getter for the disk-name of the CurseForge project file.
     * @author Griefed
     * @return String. The disk-name of the CurseForge project file.
     */
    public String getFileDiskName() {
        return fileDiskName;
    }

    /**
     * Setter for the disk-name of the CurseForge project file.
     * @author Griefed
     * @param fileName String. The disk-name of the CurseForge project file.
     */
    public void setFileDiskName(String fileName) {
        this.fileDiskName = fileName;
    }

    /**
     * Getter for the path to the server-icon.png to include in the server pack.
     * @author Griefed
     * @return String. Returns the path to the server-icon.png.
     */
    public String getServerIconPath() {
        return serverIconPath;
    }

    /**
     * Setter for the path of the server-icon.png to include in the server pack.
     * @author Griefed
     * @param serverIconPath String. The path to the server-icon.png to include in the server pack.
     */
    public void setServerIconPath(String serverIconPath) {
        this.serverIconPath = serverIconPath.replace("\\","/");
    }

    /**
     * Getter for the path of the server.properties to include in the server pack.
     * @author Griefed
     * @return String. Returns the path to the server.properties to include in the server pack.
     */
    public String getServerPropertiesPath() {
        return serverPropertiesPath;
    }

    /**
     * Setter for the path of the server.properties to include in the server pack.
     * @author Griefed
     * @param serverPropertiesPath String. The path to the server.properties to include in the server pack.
     */
    public void setServerPropertiesPath(String serverPropertiesPath) {
        this.serverPropertiesPath = serverPropertiesPath.replace("\\", "/");
    }

    /**
     * Concatenates all configuration parameters into a String. Overrides the default toString() method.
     * @author Griefed
     * @return String. A concatenated string of the whole configuration.
     */
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
                ", serverProperties='" + serverPropertiesPath + '\'' +
                ", curseModpack=" + curseModpack +
                ", includeServerInstallation=" + includeServerInstallation +
                ", includeServerIcon=" + includeServerIcon +
                ", includeServerProperties=" + includeServerProperties +
                ", includeZipCreation=" + includeZipCreation +
                ", projectName='" + projectName + '\'' +
                ", fileName='" + fileName + '\'' +
                ", fileDiskName='" + fileDiskName + '\'' +
                ", projectID=" + projectID +
                ", fileID=" + fileID +
                '}';
    }
}
