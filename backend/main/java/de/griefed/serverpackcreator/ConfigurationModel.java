package de.griefed.serverpackcreator;

import java.util.List;

public class ConfigurationModel {

    private List<String>
            clientMods,
            copyDirs;

    private String
            modpackDir,
            javaPath,
            minecraftVersion,
            modLoader,
            modLoaderVersion,
            javaArgs;

    private String serverPackSuffix = "";

    private Boolean
            includeServerInstallation,
            includeServerIcon,
            includeServerProperties,
            includeStartScripts,
            includeZipCreation;

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
    List<String> getClientMods() {
        return clientMods;
    }

    /**
     * Setter for the list of clientside-only mods to exclude from server pack.
     * @author Griefed
     * @param newClientMods The new list of clientside-only mods to store.
     */
    void setClientMods(List<String> newClientMods) {
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
    void setCopyDirs(List<String> newCopyDirs) {
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
    void setModpackDir(String newModpackDir) {
        newModpackDir = newModpackDir.replace("\\","/");
        this.modpackDir = newModpackDir;
    }

    /**
     * Getter for the path to the Java executable/binary.
     * @author Griefed
     * @return String. Returns the path to the Java executable/binary.
     */
    String getJavaPath() {
        return javaPath;
    }

    /**
     * Setter for the path to the Java executable/binary. Replaces any occurrences of \ with /.
     * @author Griefed
     * @param newJavaPath The new Java path to store.
     */
    void setJavaPath(String newJavaPath) {
        newJavaPath = newJavaPath.replace("\\", "/");
        this.javaPath = newJavaPath;
    }

    /**
     * Getter for the version of Minecraft used by the modpack.
     * @author Griefed
     * @return String. Returns the Minecraft version used in the modpack.
     */
    String getMinecraftVersion() {
        return minecraftVersion;
    }

    /**
     * Setter for the Minecraft version used by the modpack.
     * @author Griefed
     * @param newMinecraftVersion The new Minecraft version to store.
     */
    void setMinecraftVersion(String newMinecraftVersion) {
        this.minecraftVersion = newMinecraftVersion;
    }

    /**
     * Getter for the modloader used by the modpack.
     * @author Griefed
     * @return String. Returns the modloader used by the modpack.
     */
    String getModLoader() {
        return modLoader;
    }

    /**
     * Setter for the modloader used by the modpack.
     * @author Griefed
     * @param newModLoader The new modloader to store.
     */
    void setModLoader(String newModLoader) {
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
    String getModLoaderVersion() {
        return modLoaderVersion;
    }

    /**
     * Setter for the version of the modloader used by the modpack.
     * @author Griefed
     * @param newModLoaderVersion The new modloader version to store.
     */
    void setModLoaderVersion(String newModLoaderVersion) {
        this.modLoaderVersion = newModLoaderVersion;
    }

    /**
     * Getter for whether the modloader server installation should be included.
     * @author Griefed
     * @return Boolean. Returns whether the server installation should be included.
     */
    boolean getIncludeServerInstallation() {
        return includeServerInstallation;
    }

    /**
     * Setter for whether the modloader server installation should be included.
     * @author Griefed
     * @param newIncludeServerInstallation The new boolean to store.
     */
    void setIncludeServerInstallation(boolean newIncludeServerInstallation) {
        this.includeServerInstallation = newIncludeServerInstallation;
    }

    /**
     * Getter for whether the server-icon.png should be included in the server pack.
     * @author Griefed
     * @return Boolean. Returns whether the server-icon.png should be included in the server pack.
     */
    boolean getIncludeServerIcon() {
        return includeServerIcon;
    }

    /**
     * Setter for whether the server-icon.png should be included in the server pack.
     * @author Griefed
     * @param newIncludeServerIcon The new boolean to store.
     */
    void setIncludeServerIcon(boolean newIncludeServerIcon) {
        this.includeServerIcon = newIncludeServerIcon;
    }

    /**
     * Getter for whether the server.properties should be included in the server pack.
     * @author Griefed
     * @return Boolean. Returns whether the server.properties should be included in the server pack.
     */
    boolean getIncludeServerProperties() {
        return includeServerProperties;
    }

    /**
     * Setter for whether the server.properties should be included in the server pack.
     * @author Griefed
     * @param newIncludeServerProperties The new boolean to store.
     */
    void setIncludeServerProperties(boolean newIncludeServerProperties) {
        this.includeServerProperties = newIncludeServerProperties;
    }

    /**
     * Getter for whether the start scripts should be included in the server pack.
     * @author Griefed
     * @return Boolean. Returns the whether the start scripts should be included in the server pack.
     */
    boolean getIncludeStartScripts() {
        return includeStartScripts;
    }

    /**
     * Setter for whether the start scripts should be included in the server pack.
     * @author Griefed
     * @param newIncludeStartScripts The new boolean to store.
     */
    void setIncludeStartScripts(boolean newIncludeStartScripts) {
        this.includeStartScripts = newIncludeStartScripts;
    }

    /**
     * Getter for whether a ZIP-archive of the server pack should be created.
     * @author Griefed
     * @return Boolean. Returns whether a ZIP-archive of the server pack should be created.
     */
    boolean getIncludeZipCreation() {
        return includeZipCreation;
    }

    /**
     * Setter for whether a ZIP-archive of the server pack should be created.
     * @author Griefed
     * @param newIncludeZipCreation The new boolean to store.
     */
    void setIncludeZipCreation(boolean newIncludeZipCreation) {
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
                ", includeServerInstallation=" + includeServerInstallation +
                ", includeServerIcon=" + includeServerIcon +
                ", includeServerProperties=" + includeServerProperties +
                ", includeStartScripts=" + includeStartScripts +
                ", includeZipCreation=" + includeZipCreation +
                '}';
    }
}
