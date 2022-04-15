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

import com.google.common.collect.Lists;
import de.griefed.serverpackcreator.ApplicationProperties;
import de.griefed.serverpackcreator.versionmeta.Type;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

/**
 * Fabric meta containing information about available Fabric releases and installers.
 * @author Griefed
 */
public class FabricMeta {

    private final ApplicationProperties APPLICATIONPROPERTIES;
    private final FabricLoader fabricLoader;
    private final FabricInstaller fabricInstaller;

    /**
     * Constructor
     * @author Griefed
     * @param injectedApplicationProperties instance of {@link ApplicationProperties}.
     * @throws MalformedURLException if the meta could not be updated.
     */
    public FabricMeta(ApplicationProperties injectedApplicationProperties) throws MalformedURLException {

        if (injectedApplicationProperties == null) {
            this.APPLICATIONPROPERTIES = new ApplicationProperties();
        } else {
            this.APPLICATIONPROPERTIES = injectedApplicationProperties;
        }

        this.fabricLoader = new FabricLoader(getXml(APPLICATIONPROPERTIES.PATH_FILE_MANIFEST_FABRIC));
        this.fabricInstaller = new FabricInstaller(getXml(APPLICATIONPROPERTIES.PATH_FILE_MANIFEST_FABRIC_INSTALLER));

    }

    /**
     * Update the {@link FabricLoader} and {@link FabricInstaller} information.
     * @author Griefed
     * @return This instance of {@link FabricMeta}.
     * @throws MalformedURLException if a URL could not be constructed
     */
    public FabricMeta update() throws MalformedURLException {

        this.fabricLoader.update(getXml(APPLICATIONPROPERTIES.PATH_FILE_MANIFEST_FABRIC));
        this.fabricInstaller.update(getXml(APPLICATIONPROPERTIES.PATH_FILE_MANIFEST_FABRIC_INSTALLER));

        return this;
    }

    private Document getXml(File manifest) {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = null;
        Document xml = null;
        try {
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        try {
            assert documentBuilder != null;
            xml = documentBuilder.parse(manifest);
        } catch (SAXException | IOException e) {
            e.printStackTrace();
        }
        assert xml != null;
        xml.normalize();
        return xml;
    }

    /**
     * Get the latest Fabric loader version.
     * @author Griefed
     * @return {@link String} The latest version of the Fabric loader.
     */
    public String latestLoaderVersion() {
        return fabricLoader.latestLoaderVersion();
    }

    /**
     * Get the release Fabric loader version.
     * @author Griefed
     * @return {@link String} The release version of the Fabric loader.
     */
    public String releaseLoaderVersion() {
        return fabricLoader.releaseLoaderVersion();
    }

    /**
     * Get a list of available Fabric loader versions, in {@link Type#ASCENDING} order.
     * @author Griefed
     * @return {@link String}-list of available Fabric loader versions, in {@link Type#ASCENDING} order.
     */
    public List<String> loaderVersionsAscending() {
        return fabricLoader.loaders();
    }

    /**
     * Get a list of available Fabric loader versions, in {@link Type#DESCENDING} order.
     * @author Griefed
     * @return {@link String}-list of available Fabric loader versions, in {@link Type#DESCENDING} order.
     */
    public List<String> loaderVersionsDescending() {
        return Lists.reverse(fabricLoader.loaders());
    }

    /**
     * Get an array of available Fabric loader versions, in {@link Type#ASCENDING} order.
     * @author Griefed
     * @return {@link String}-array of available Fabric loader versions, in {@link Type#ASCENDING} order.
     */
    public String[] loaderVersionsArrayAscending() {
        return fabricLoader.loaders().toArray(new String[0]);
    }

    /**
     * Get an array of available Fabric loader versions, in {@link Type#DESCENDING} order.
     * @author Griefed
     * @return {@link String}-array of available Fabric loader versions, in {@link Type#DESCENDING} order.
     */
    public String[] loaderVersionsArrayDescending() {
        return Lists.reverse(fabricLoader.loaders()).toArray(new String[0]);
    }

    /**
     * Get the latest Fabric installer version.
     * @author Griefed
     * @return {@link String} The latest Fabric installer version.
     */
    public String latestInstallerVersion() {
        return fabricInstaller.latestInstallerVersion();
    }

    /**
     * Get the release Fabric installer version.
     * @author Griefed
     * @return {@link String} The release Fabric installer version.
     */
    public String releaseInstallerVersion() {
        return fabricInstaller.releaseInstallerVersion();
    }

    /**
     * Get the list of available Fabric installer version, in {@link Type#ASCENDING} order.
     * @author Griefed
     * @return {@link String}-list of available Fabric installer version, in {@link Type#ASCENDING} order.
     */
    public List<String> installerVersionsAscending() {
        return fabricInstaller.installers();
    }

    /**
     * Get the list of available Fabric installer version, in {@link Type#DESCENDING} order.
     * @author Griefed
     * @return {@link String}-list of available Fabric installer version, in {@link Type#DESCENDING} order.
     */
    public List<String> installerVersionsDescending() {
        return Lists.reverse(fabricInstaller.installers());
    }

    /**
     * Get the array of available Fabric installer version, in {@link Type#ASCENDING} order.
     * @author Griefed
     * @return {@link String}-array of available Fabric installer version, in {@link Type#ASCENDING} order.
     */
    public String[] installerVersionsArrayAscending() {
        return fabricInstaller.installers().toArray(new String[0]);
    }

    /**
     * Get the array of available Fabric installer version, in {@link Type#DESCENDING} order.
     * @author Griefed
     * @return {@link String}-array of available Fabric installer version, in {@link Type#DESCENDING} order.
     */
    public String[] installerVersionsArrayDescending() {
        return Lists.reverse(fabricInstaller.installers()).toArray(new String[0]);
    }

    /**
     * Get the {@link URL} to the latest Fabric installer.
     * @author Griefed
     * @return {@link URL} to the latest Fabric installer.
     */
    public URL latestInstallerUrl() {
        return fabricInstaller.latestInstallerUrl();
    }

    /**
     * Get the {@link URL} to the release Fabric installer.
     * @author Griefed
     * @return {@link URL} to the release Fabric installer.
     */
    public URL releaseInstallerUrl() {
        return fabricInstaller.releaseInstallerUrl();
    }

    /**
     * Check whether a {@link URL} to the specified Fabric installer version is available.
     * @author Griefed
     * @param fabricVersion {@link String} Fabric version.
     * @return {@link Boolean} <code>true</code> if a {@link URL} to the specified Fabric installer version is available.
     */
    public boolean isInstallerUrlAvailable(String fabricVersion) {
        return Optional.ofNullable(fabricInstaller.meta().get(fabricVersion)).isPresent();
    }

    /**
     * Get the {@link URL} to the Fabric installer for the specified version.
     * @author Griefed
     * @param fabricVersion {@link String} Fabric version.
     * @return {@link URL} to the Fabric installer for the specified version.
     */
    public Optional<URL> installerUrl(String fabricVersion) {
        return Optional.ofNullable(fabricInstaller.meta().get(fabricVersion));
    }

    /**
     * Get the {@link URL} to the Fabric launcher for the specified Minecraft and Fabric version.
     * @author Griefed
     * @param minecraftVersion {@link String} Minecraft version.
     * @param fabricVersion {@link String} Fabric version.
     * @return {@link URL} to the Fabric launcher for the specified Minecraft and Fabric version.
     */
    public Optional<URL> improvedLauncherUrl(String minecraftVersion, String fabricVersion) {
        return fabricInstaller.improvedLauncherUrl(minecraftVersion, fabricVersion);
    }

    /**
     *  Check whether the specified Fabric version is available/correct/valid.
     * @author Griefed
     * @param fabricVersion {@link String} Fabric version.
     * @return {@link Boolean} <code>true</code> if the specified version is available/correct/valid.
     */
    public boolean checkFabricVersion(String fabricVersion) {
        return fabricLoader.loaders().contains(fabricVersion);
    }
}
