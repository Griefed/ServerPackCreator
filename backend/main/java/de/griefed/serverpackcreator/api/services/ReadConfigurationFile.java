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
package de.griefed.serverpackcreator.api.services;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import de.griefed.serverpackcreator.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

@Service
public class ReadConfigurationFile {

    private List<String> clientMods;
    private List<String> copyDirs;
    private String modpackDir;
    private String javaPath;
    private String minecraftVersion;
    private String modLoader;
    private String modLoaderVersion;
    private String includeServerInstallation;
    private String includeServerIcon;
    private String includeServerProperties;
    private String includeStartScripts;
    private String includeZipCreation;

    private final Configuration CONFIGURATION;

    private final File defaultConfigurationFile = new File("serverpackcreator.conf");

    @Autowired
    ReadConfigurationFile(Configuration newCONFIGURATION) {
        this.CONFIGURATION = newCONFIGURATION;
    }

    public void setConfigFromSpecificFile(File configFileToRead) {
        Config localConfig = ConfigFactory.parseFile(configFileToRead);
        setConfig(localConfig);
    }

    public void setConfigDefault() {
        Config specificConfig = ConfigFactory.parseFile(defaultConfigurationFile);
        setConfig(specificConfig);
    }

    private void setConfig(Config config) {
        setClientMods(config);
        setCopyDirs(config);
        setModpackDir(config);
        setJavaPath(config);
        setMinecraftVersion(config);
        setModLoader(config);
        setModLoaderVersion(config);
        setIncludeServerInstallation(config);
        setIncludeServerIcon(config);
        setIncludeServerProperties(config);
        setIncludeStartScripts(config);
        setIncludeZipCreation(config);
    }

    public List<String> getClientMods() {
        return clientMods;
    }

    private void setClientMods(Config config) {
        if (config.getStringList("clientMods").isEmpty()) {
            this.clientMods = CONFIGURATION.getFallbackModsList();
        } else {
            this.clientMods = config.getStringList("clientMods");
        }
    }

    public List<String> getCopyDirs() {
        return copyDirs;
    }

    private void setCopyDirs(Config config) {
        this.copyDirs = config.getStringList("copyDirs");
    }

    public String getModpackDir() {
        return modpackDir;
    }

    private void setModpackDir(Config config) {
        this.modpackDir = config.getString("modpackDir");
    }

    public String getJavaPath() {
        return javaPath;
    }

    private void setJavaPath(Config config) {
        this.javaPath = config.getString("javaPath");
    }

    public String getMinecraftVersion() {
        return minecraftVersion;
    }

    private void setMinecraftVersion(Config config) {
        this.minecraftVersion = config.getString("minecraftVersion");
    }

    public String getModLoader() {
        return modLoader;
    }

    private void setModLoader(Config config) {
        this.modLoader = config.getString("modLoader");
    }

    public String getModLoaderVersion() {
        return modLoaderVersion;
    }

    private void setModLoaderVersion(Config config) {
        this.modLoaderVersion = config.getString("modLoaderVersion");
    }

    public String getIncludeServerInstallation() {
        return includeServerInstallation;
    }

    private void setIncludeServerInstallation(Config config) {
        this.includeServerInstallation = config.getString("includeServerInstallation");
    }

    public String getIncludeServerIcon() {
        return includeServerIcon;
    }

    private void setIncludeServerIcon(Config config) {
        this.includeServerIcon = config.getString("includeServerIcon");
    }

    public String getIncludeServerProperties() {
        return includeServerProperties;
    }

    private void setIncludeServerProperties(Config config) {
        this.includeServerProperties = config.getString("includeServerProperties");
    }

    public String getIncludeStartScripts() {
        return includeStartScripts;
    }

    private void setIncludeStartScripts(Config config) {
        this.includeStartScripts = config.getString("includeStartScripts");
    }

    public String getIncludeZipCreation() {
        return includeZipCreation;
    }

    private void setIncludeZipCreation(Config config) {
        this.includeZipCreation = config.getString("includeZipCreation");
    }

    @Override
    public String toString() {
        return "LocalConfigurationFile{" +
                "clientMods=" + clientMods +
                ", copyDirs=" + copyDirs +
                ", modpackDir='" + modpackDir + '\'' +
                ", javaPath='" + javaPath + '\'' +
                ", minecraftVersion='" + minecraftVersion + '\'' +
                ", modLoader='" + modLoader + '\'' +
                ", modLoaderVersion='" + modLoaderVersion + '\'' +
                ", includeServerInstallation='" + includeServerInstallation + '\'' +
                ", includeServerIcon='" + includeServerIcon + '\'' +
                ", includeServerProperties='" + includeServerProperties + '\'' +
                ", includeStartScripts='" + includeStartScripts + '\'' +
                ", includeZipCreation='" + includeZipCreation + '\'' +
                '}';
    }
}
