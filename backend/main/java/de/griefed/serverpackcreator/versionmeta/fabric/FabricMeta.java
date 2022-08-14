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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import de.griefed.serverpackcreator.versionmeta.Type;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Fabric meta containing information about available Fabric releases and installers.
 *
 * @author Griefed
 */
public class FabricMeta {

  private static final Logger LOG = LogManager.getLogger(FabricMeta.class);

  private final File FABRIC_MANIFEST;
  private final File FABRIC_INSTALLER_MANIFEST;
  private final FabricLoader FABRIC_LOADER;
  private final FabricLoaderDetails FABRIC_LOADER_DETAILS;
  private final FabricInstaller FABRIC_INSTALLER;
  private final FabricIntermediaries FABRIC_INTERMEDIARIES;
  private final HashMap<String, FabricDetails> LOADER_DETAILS = new HashMap<>();

  /**
   * Create a new Fabric Meta instance.
   *
   * @param fabricManifest             Fabric manifest file.
   * @param fabricInstallerManifest    Fabric-installer manifest file.
   * @param fabricIntermediaryManifest Fabric Intermediary manifest file.
   * @param objectMapper               Object mapper for JSON parsing.
   * @throws IOException when the Intermediaries manifest could not be parsed.
   * @author Griefed
   */
  public FabricMeta(
      File fabricManifest,
      File fabricInstallerManifest,
      File fabricIntermediaryManifest,
      ObjectMapper objectMapper)
      throws IOException {

    this.FABRIC_LOADER_DETAILS = new FabricLoaderDetails(objectMapper);
    this.FABRIC_MANIFEST = fabricManifest;
    this.FABRIC_INSTALLER_MANIFEST = fabricInstallerManifest;
    this.FABRIC_LOADER = new FabricLoader(getXml(this.FABRIC_MANIFEST));
    this.FABRIC_INTERMEDIARIES = new FabricIntermediaries(fabricIntermediaryManifest, objectMapper);
    this.FABRIC_INSTALLER = new FabricInstaller(getXml(this.FABRIC_INSTALLER_MANIFEST));
  }

  /**
   * Update the {@link FabricLoader} and {@link FabricInstaller} information.
   *
   * @author Griefed
   */
  public void update() {
    this.FABRIC_LOADER.update(getXml(this.FABRIC_MANIFEST));
    this.FABRIC_INSTALLER.update(getXml(this.FABRIC_INSTALLER_MANIFEST));
  }

  private Document getXml(File manifest) {
    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder documentBuilder = null;
    Document xml = null;

    try {

      documentBuilder = documentBuilderFactory.newDocumentBuilder();

    } catch (ParserConfigurationException ex) {
      LOG.error("Couldn't read document.", ex);
    }

    try {

      assert documentBuilder != null;
      xml = documentBuilder.parse(manifest);

    } catch (SAXException | IOException ex) {
      LOG.error("Couldn't read document.", ex);
    }

    assert xml != null;
    xml.normalize();
    return xml;
  }

  /**
   * Get the latest Fabric loader version.
   *
   * @return The latest version of the Fabric loader.
   * @author Griefed
   */
  public String latestLoaderVersion() {
    return FABRIC_LOADER.latestLoaderVersion();
  }

  /**
   * Get the release Fabric loader version.
   *
   * @return The release version of the Fabric loader.
   * @author Griefed
   */
  public String releaseLoaderVersion() {
    return FABRIC_LOADER.releaseLoaderVersion();
  }

  /**
   * Get a list of available Fabric loader versions, in {@link Type#ASCENDING} order.
   *
   * @return List of available Fabric loader versions, in {@link Type#ASCENDING} order.
   * @author Griefed
   */
  public List<String> loaderVersionsAscending() {
    return FABRIC_LOADER.loaders();
  }

  /**
   * Get a list of available Fabric loader versions, in {@link Type#DESCENDING} order.
   *
   * @return List of available Fabric loader versions, in {@link Type#DESCENDING} order.
   * @author Griefed
   */
  public List<String> loaderVersionsDescending() {
    return Lists.reverse(FABRIC_LOADER.loaders());
  }

  /**
   * Get an array of available Fabric loader versions, in {@link Type#ASCENDING} order.
   *
   * @return Array of available Fabric loader versions, in {@link Type#ASCENDING} order.
   * @author Griefed
   */
  public String[] loaderVersionsArrayAscending() {
    return FABRIC_LOADER.loaders().toArray(new String[0]);
  }

  /**
   * Get an array of available Fabric loader versions, in {@link Type#DESCENDING} order.
   *
   * @return Array of available Fabric loader versions, in {@link Type#DESCENDING} order.
   * @author Griefed
   */
  public String[] loaderVersionsArrayDescending() {
    return Lists.reverse(FABRIC_LOADER.loaders()).toArray(new String[0]);
  }

  /**
   * Get the latest Fabric installer version.
   *
   * @return The latest Fabric installer version.
   * @author Griefed
   */
  public String latestInstallerVersion() {
    return FABRIC_INSTALLER.latestInstallerVersion();
  }

  /**
   * Get the release Fabric installer version.
   *
   * @return The release Fabric installer version.
   * @author Griefed
   */
  public String releaseInstallerVersion() {
    return FABRIC_INSTALLER.releaseInstallerVersion();
  }

  /**
   * Get the list of available Fabric installer version, in {@link Type#ASCENDING} order.
   *
   * @return List of available Fabric installer version, in {@link Type#ASCENDING} order.
   * @author Griefed
   */
  public List<String> installerVersionsAscending() {
    return FABRIC_INSTALLER.installers();
  }

  /**
   * Get the list of available Fabric installer version, in {@link Type#DESCENDING} order.
   *
   * @return List of available Fabric installer version, in {@link Type#DESCENDING} order.
   * @author Griefed
   */
  public List<String> installerVersionsDescending() {
    return Lists.reverse(FABRIC_INSTALLER.installers());
  }

  /**
   * Get the array of available Fabric installer version, in {@link Type#ASCENDING} order.
   *
   * @return Array of available Fabric installer version, in {@link Type#ASCENDING} order.
   * @author Griefed
   */
  public String[] installerVersionsArrayAscending() {
    return FABRIC_INSTALLER.installers().toArray(new String[0]);
  }

  /**
   * Get the array of available Fabric installer version, in {@link Type#DESCENDING} order.
   *
   * @return Array of available Fabric installer version, in {@link Type#DESCENDING} order.
   * @author Griefed
   */
  public String[] installerVersionsArrayDescending() {
    return Lists.reverse(FABRIC_INSTALLER.installers()).toArray(new String[0]);
  }

  /**
   * Get the {@link URL} to the latest Fabric installer.
   *
   * @return URL to the latest Fabric installer.
   * @author Griefed
   */
  public URL latestInstallerUrl() {
    return FABRIC_INSTALLER.latestInstallerUrl();
  }

  /**
   * Get the {@link URL} to the release Fabric installer.
   *
   * @return URL to the release Fabric installer.
   * @author Griefed
   */
  public URL releaseInstallerUrl() {
    return FABRIC_INSTALLER.releaseInstallerUrl();
  }

  /**
   * Check whether a {@link URL} to the specified Fabric installer version is available.
   *
   * @param fabricVersion Fabric version.
   * @return <code>true</code> if a {@link URL} to the specified Fabric installer
   * version is available.
   * @author Griefed
   */
  public boolean isInstallerUrlAvailable(String fabricVersion) {
    return Optional.ofNullable(FABRIC_INSTALLER.meta().get(fabricVersion)).isPresent();
  }

  /**
   * Get the {@link URL} to the Fabric installer for the specified version.
   *
   * @param fabricVersion Fabric version.
   * @return URL to the Fabric installer for the specified version.
   * @author Griefed
   */
  public Optional<URL> installerUrl(String fabricVersion) {
    return Optional.ofNullable(FABRIC_INSTALLER.meta().get(fabricVersion));
  }

  /**
   * Get the {@link URL} to the Fabric launcher for the specified Minecraft and Fabric version.
   *
   * @param minecraftVersion Minecraft version.
   * @param fabricVersion    Fabric version.
   * @return URL to the Fabric launcher for the specified Minecraft and Fabric version.
   * @author Griefed
   */
  public Optional<URL> improvedLauncherUrl(String minecraftVersion, String fabricVersion) {
    return FABRIC_INSTALLER.improvedLauncherUrl(minecraftVersion, fabricVersion);
  }

  /**
   * Check whether the specified Fabric version is available/correct/valid.
   *
   * @param fabricVersion Fabric version.
   * @return <code>true</code> if the specified version is available/correct/valid.
   * @author Griefed
   */
  public boolean checkFabricVersion(String fabricVersion) {
    return FABRIC_LOADER.loaders().contains(fabricVersion);
  }

  /**
   * Get details for a Fabric loader.
   *
   * @param minecraftVersion Minecraft version.
   * @param fabricVersion    Fabric version.
   * @return Details of a Fabric loader for the given Minecraft and Fabric version, wrapped in an
   * {@link Optional}.
   * @author Griefed
   */
  public Optional<FabricDetails> getLoaderDetails(String minecraftVersion, String fabricVersion) {
    String key = minecraftVersion + "-" + fabricVersion;

    if (LOADER_DETAILS.containsKey(key)) {

      return Optional.of(LOADER_DETAILS.get(key));

    } else if (FABRIC_LOADER_DETAILS.getDetails(minecraftVersion, fabricVersion).isPresent()) {

      LOADER_DETAILS.put(
          key, FABRIC_LOADER_DETAILS.getDetails(minecraftVersion, fabricVersion).get());
      return Optional.of(LOADER_DETAILS.get(key));

    } else {

      return Optional.empty();
    }
  }

  /**
   * HashMap of available intermediaries.
   *
   * @return Map of available intermediaries.
   * @author Griefed
   */
  public HashMap<String, FabricIntermediary> getFabricIntermediaries() {
    return FABRIC_INTERMEDIARIES.getIntermediaries();
  }

  /**
   * Get a specific intermediary, wrapped in an {@link Optional}.
   *
   * @param minecraftVersion Minecraft version.
   * @return A specific intermediary, wrapped in an {@link Optional}.
   * @author Griefed
   */
  public Optional<FabricIntermediary> getFabricIntermediary(String minecraftVersion) {
    return FABRIC_INTERMEDIARIES.getIntermediary(minecraftVersion);
  }
}
