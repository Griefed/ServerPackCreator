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

import javax.validation.constraints.NotEmpty;
import java.io.File;
import java.util.List;

@Service
public class ConfigurationService {

    @NotEmpty
    private List<String> clientMods;

    @NotEmpty
    private List<String> copyDirs;

    @NotEmpty
    private String modpackDir;

    private String javaPath;

    @NotEmpty
    private String minecraftVersion;

    @NotEmpty
    private String modLoader;

    @NotEmpty
    private String modLoaderVersion;

    @NotEmpty
    private String includeServerInstallation;

    @NotEmpty
    private String includeServerIcon;

    @NotEmpty
    private String includeServerProperties;

    @NotEmpty
    private String includeStartScripts;

    @NotEmpty
    private String includeZipCreation;

    private final Configuration CONFIGURATION;

    @Autowired
    ConfigurationService(Configuration injectedConfiguration) {
        this.CONFIGURATION = injectedConfiguration;
    }

    public void setConfig(File configurationFile) {
        Config config = ConfigFactory.parseFile(configurationFile);
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
        if (!CONFIGURATION.checkJavaPath(config.getString("javaPath"))) {
            this.javaPath = CONFIGURATION.getJavaPathFromSystem("");
        } else {
            this.javaPath = config.getString("javaPath");
        }
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
