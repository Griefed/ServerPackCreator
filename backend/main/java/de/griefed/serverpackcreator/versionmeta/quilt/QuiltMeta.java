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
package de.griefed.serverpackcreator.versionmeta.quilt;

import com.google.common.collect.Lists;
import de.griefed.serverpackcreator.versionmeta.Type;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
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
 * Quilt meta containing information about available Quilt releases and installers.
 *
 * @author Griefed
 */
public class QuiltMeta {

  private static final Logger LOG = LogManager.getLogger(QuiltMeta.class);

  private final File QUILT_MANIFEST;
  private final File QUILT_INSTALLER_MANIFEST;
  private final QuiltLoader QUILT_LOADER;
  private final QuiltInstaller QUILT_INSTALLER;

  /**
   * Create a new Quilt Meta instance.
   *
   * @param quiltManifest          Quilt manifest file..
   * @param quiltInstallerManifest Quilt-installer manifest file..
   * @author Griefed
   */
  public QuiltMeta(File quiltManifest, File quiltInstallerManifest) {
    this.QUILT_MANIFEST = quiltManifest;
    this.QUILT_INSTALLER_MANIFEST = quiltInstallerManifest;
    this.QUILT_LOADER = new QuiltLoader(getXml(this.QUILT_MANIFEST));
    this.QUILT_INSTALLER = new QuiltInstaller(getXml(this.QUILT_INSTALLER_MANIFEST));
  }

  /**
   * Update the {@link QuiltLoader} and {@link QuiltInstaller} information.
   *
   * @throws MalformedURLException if a URL could not be constructed
   * @author Griefed
   */
  public void update() throws MalformedURLException {
    this.QUILT_LOADER.update(getXml(this.QUILT_MANIFEST));
    this.QUILT_INSTALLER.update(getXml(this.QUILT_INSTALLER_MANIFEST));
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
   * Get the latest Quilt loader version.
   *
   * @return The latest version of the Quilt loader.
   * @author Griefed
   */
  public String latestLoaderVersion() {
    return QUILT_LOADER.latestLoaderVersion();
  }

  /**
   * Get the release Quilt loader version.
   *
   * @return The release version of the Quilt loader.
   * @author Griefed
   */
  public String releaseLoaderVersion() {
    return QUILT_LOADER.releaseLoaderVersion();
  }

  /**
   * Get a list of available Quilt loader versions, in {@link Type#ASCENDING} order.
   *
   * @return List of available Quilt loader versions, in {@link Type#ASCENDING} order.
   * @author Griefed
   */
  public List<String> loaderVersionsAscending() {
    return QUILT_LOADER.loaders();
  }

  /**
   * Get a list of available Quilt loader versions, in {@link Type#DESCENDING} order.
   *
   * @return List of available Quilt loader versions, in {@link Type#DESCENDING} order.
   * @author Griefed
   */
  public List<String> loaderVersionsDescending() {
    return Lists.reverse(QUILT_LOADER.loaders());
  }

  /**
   * Get an array of available Quilt loader versions, in {@link Type#ASCENDING} order.
   *
   * @return Array of available Quilt loader versions, in {@link Type#ASCENDING} order.
   * @author Griefed
   */
  public String[] loaderVersionsArrayAscending() {
    return QUILT_LOADER.loaders().toArray(new String[0]);
  }

  /**
   * Get an array of available Quilt loader versions, in {@link Type#DESCENDING} order.
   *
   * @return Array of available Quilt loader versions, in {@link Type#DESCENDING} order.
   * @author Griefed
   */
  public String[] loaderVersionsArrayDescending() {
    return Lists.reverse(QUILT_LOADER.loaders()).toArray(new String[0]);
  }

  /**
   * Get the latest Quilt installer version.
   *
   * @return The latest Quilt installer version.
   * @author Griefed
   */
  public String latestInstallerVersion() {
    return QUILT_INSTALLER.latestInstallerVersion();
  }

  /**
   * Get the release Quilt installer version.
   *
   * @return The release Quilt installer version.
   * @author Griefed
   */
  public String releaseInstallerVersion() {
    return QUILT_INSTALLER.releaseInstallerVersion();
  }

  /**
   * Get the list of available Quilt installer version, in {@link Type#ASCENDING} order.
   *
   * @return List of available Quilt installer version, in {@link Type#ASCENDING} order.
   * @author Griefed
   */
  public List<String> installerVersionsAscending() {
    return QUILT_INSTALLER.installers();
  }

  /**
   * Get the list of available Quilt installer version, in {@link Type#DESCENDING} order.
   *
   * @return List of available Quilt installer version, in {@link Type#DESCENDING} order.
   * @author Griefed
   */
  public List<String> installerVersionsDescending() {
    return Lists.reverse(QUILT_INSTALLER.installers());
  }

  /**
   * Get the array of available Quilt installer version, in {@link Type#ASCENDING} order.
   *
   * @return Array of available Quilt installer version, in {@link Type#ASCENDING} order.
   * @author Griefed
   */
  public String[] installerVersionsArrayAscending() {
    return QUILT_INSTALLER.installers().toArray(new String[0]);
  }

  /**
   * Get the array of available Quilt installer version, in {@link Type#DESCENDING} order.
   *
   * @return Array of available Quilt installer version, in {@link Type#DESCENDING} order.
   * @author Griefed
   */
  public String[] installerVersionsArrayDescending() {
    return Lists.reverse(QUILT_INSTALLER.installers()).toArray(new String[0]);
  }

  /**
   * Get the {@link URL} to the latest Quilt installer.
   *
   * @return URL to the latest Quilt installer.
   * @author Griefed
   */
  public URL latestInstallerUrl() {
    return QUILT_INSTALLER.latestInstallerUrl();
  }

  /**
   * Get the {@link URL} to the release Quilt installer.
   *
   * @return URL to the release Quilt installer.
   * @author Griefed
   */
  public URL releaseInstallerUrl() {
    return QUILT_INSTALLER.releaseInstallerUrl();
  }

  /**
   * Check whether a {@link URL} to the specified Quilt installer version is available.
   *
   * @param quiltVersion Quilt version.
   * @return <code>true</code> if a {@link URL} to the specified Quilt installer
   * version is available.
   * @author Griefed
   */
  public boolean isInstallerUrlAvailable(String quiltVersion) {
    return Optional.ofNullable(QUILT_INSTALLER.meta().get(quiltVersion)).isPresent();
  }

  /**
   * Get the {@link URL} to the Quilt installer for the specified version.
   *
   * @param quiltVersion Quilt version.
   * @return URL to the Quilt installer for the specified version.
   * @author Griefed
   */
  public Optional<URL> installerUrl(String quiltVersion) {
    return Optional.ofNullable(QUILT_INSTALLER.meta().get(quiltVersion));
  }

  /**
   * Check whether the specified Quilt version is available/correct/valid.
   *
   * @param quiltVersion Quilt version.
   * @return <code>true</code> if the specified version is available/correct/valid.
   * @author Griefed
   */
  public boolean checkQuiltVersion(String quiltVersion) {
    return QUILT_LOADER.loaders().contains(quiltVersion);
  }
}
