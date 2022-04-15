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
package de.griefed.serverpackcreator.versionmeta.fabric;

import org.w3c.dom.Document;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/**
 * Information about the Fabric installer.
 * @author Griefed
 */
public class FabricInstaller {

    private final String URL_TEMPLATE_INSTALLER = "https://maven.fabricmc.net/net/fabricmc/fabric-installer/%s/fabric-installer-%s.jar";
    private final List<String> installers = new ArrayList<>();
    private String latestInstaller;
    private String releaseInstaller;
    private URL latestInstallerUrl;
    private URL releaseInstallerUrl;
    private HashMap<String, URL> installerUrlMeta;

    /**
     * Constructor
     * @author Griefed
     * @param installerManifest {@link Document} containing Fabric installer information
     */
    protected FabricInstaller(Document installerManifest) {
        this.latestInstaller = installerManifest.getElementsByTagName("latest").item(0).getChildNodes().item(0).getNodeValue();
        this.releaseInstaller = installerManifest.getElementsByTagName("release").item(0).getChildNodes().item(0).getNodeValue();
        try {
            this.latestInstallerUrl = new URL(String.format(URL_TEMPLATE_INSTALLER, this.latestInstaller, this.latestInstaller));
        } catch (MalformedURLException ignored) {

        }
        try {
            this.releaseInstallerUrl = new URL(String.format(URL_TEMPLATE_INSTALLER, this.releaseInstaller, this.releaseInstaller));
        } catch (MalformedURLException ignored) {

        }
        this.installers.clear();
        for (int i = 0; i < installerManifest.getElementsByTagName("version").getLength(); i++) {
            installers.add(installerManifest.getElementsByTagName("version").item(i).getChildNodes().item(0).getNodeValue());
        }
        this.installerUrlMeta = new HashMap<>();
        this.installers.forEach(version -> {
            try {
                this.installerUrlMeta.put(version, installerUrl(version));
            } catch (MalformedURLException ignored) {

            }
        });
    }

    /**
     * Update this {@link FabricInstaller} with information from the given {@link Document}.
     * @author Griefed
     * @param installerManifest {@link Document} containing new installer information.
     * @return This instance of {@link FabricInstaller}.
     */
    protected FabricInstaller update(Document installerManifest) {
        this.latestInstaller = installerManifest.getElementsByTagName("latest").item(0).getChildNodes().item(0).getNodeValue();
        this.releaseInstaller = installerManifest.getElementsByTagName("release").item(0).getChildNodes().item(0).getNodeValue();
        try {
            this.latestInstallerUrl = new URL(String.format(URL_TEMPLATE_INSTALLER, this.latestInstaller, this.latestInstaller));
        } catch (MalformedURLException ignored) {

        }
        try {
            this.releaseInstallerUrl = new URL(String.format(URL_TEMPLATE_INSTALLER, this.releaseInstaller, this.releaseInstaller));
        } catch (MalformedURLException ignored) {

        }
        this.installers.clear();
        for (int i = 0; i < installerManifest.getElementsByTagName("version").getLength(); i++) {
            installers.add(installerManifest.getElementsByTagName("version").item(i).getChildNodes().item(0).getNodeValue());
        }
        this.installerUrlMeta = new HashMap<>();
        this.installers.forEach(version -> {
            try {
                this.installerUrlMeta.put(version, installerUrl(version));
            } catch (MalformedURLException ignored) {

            }
        });

        return this;
    }

    /**
     * Acquire the URL for the given Fabric version.
     * @author Griefed
     * @param fabricVersion {@link String} Fabric version.
     * @return {@link URL} to the installer for the given Fabric version.
     * @throws MalformedURLException if the URL could not be formed.
     */
    private URL installerUrl(String fabricVersion) throws MalformedURLException {
        return new URL(String.format(URL_TEMPLATE_INSTALLER, fabricVersion, fabricVersion));
    }

    /**
     * Get a list of available installer versions for Fabric.
     * @author Griefed
     * @return {@link String}-list of available Fabric installer versions.
     */
    protected List<String> installers() {
        return installers;
    }

    /**
     * Meta for the Fabric-Version-to-Installer-URL.<br>
     * key: {@link String} Fabric version.<br>
     * value: {@link URL} Fabric installer URL.
     * @author Griefed
     * @return {@link HashMap} with the Fabric-Version-to-Installer-URL.
     */
    protected HashMap<String, URL> meta() {
        return installerUrlMeta;
    }

    /**
     * Get the latest Fabric installer version.
     * @author Griefed
     * @return {@link String} The latest Fabric installer version.
     */
    protected String latestInstallerVersion() {
        return latestInstaller;
    }

    /**
     * Get the release Fabric installer version.
     * @author Griefed
     * @return {@link String} The release Fabric installer version.
     */
    protected String releaseInstallerVersion() {
        return releaseInstaller;
    }

    /**
     * Get the {@link URL} to the latest Fabric installer.
     * @author Griefed
     * @return {@link URL} to the latest Fabric installer.
     */
    protected URL latestInstallerUrl() {
        return latestInstallerUrl;
    }

    /**
     * Get the {@link URL} to the release Fabric installer.
     * @author Griefed
     * @return {@link URL} to the release Fabric installer.
     */
    protected URL releaseInstallerUrl() {
        return releaseInstallerUrl;
    }

    /**
     * Get the {@link URL} to the improved Fabric launcher for the given Minecraft and Fabric versions.
     * @author Griefed
     * @param minecraftVersion {@link String} Minecraft version.
     * @param fabricVersion {@link String} Fabric version.
     * @return {@link URL} to the improved Fabric launcher, wrapped in an {@link Optional}.
     */
    protected Optional<URL> improvedLauncherUrl(String minecraftVersion, String fabricVersion) {
        try {
            return Optional.of(new URL(
                    String.format("https://meta.fabricmc.net/v2/versions/loader/%s/%s/%s/server/jar",
                            minecraftVersion,
                            fabricVersion,
                            releaseInstaller
                    )
            ));
        } catch (MalformedURLException ex) {
            return Optional.empty();
        }
    }
}
