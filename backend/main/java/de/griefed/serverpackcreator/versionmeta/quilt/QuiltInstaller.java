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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.w3c.dom.Document;

/**
 * Information about the Quilt installer.
 *
 * @author Griefed
 */
public class QuiltInstaller {

  private final String URL_TEMPLATE_INSTALLER =
      "https://maven.quiltmc.org/repository/release/org/quiltmc/quilt-installer/%s/quilt-installer-%s.jar";
  private final List<String> installers = new ArrayList<>();
  private String latestInstaller;
  private String releaseInstaller;
  private URL latestInstallerUrl;
  private URL releaseInstallerUrl;
  private HashMap<String, URL> installerUrlMeta;

  /**
   * Create a new Quilt Installer instance.
   *
   * @param installerManifest {@link Document} containing Quilt installer information
   * @author Griefed
   */
  protected QuiltInstaller(Document installerManifest) {
    this.latestInstaller =
        installerManifest
            .getElementsByTagName("latest")
            .item(0)
            .getChildNodes()
            .item(0)
            .getNodeValue();
    this.releaseInstaller =
        installerManifest
            .getElementsByTagName("release")
            .item(0)
            .getChildNodes()
            .item(0)
            .getNodeValue();
    try {
      this.latestInstallerUrl =
          new URL(
              String.format(URL_TEMPLATE_INSTALLER, this.latestInstaller, this.latestInstaller));
    } catch (MalformedURLException ignored) {

    }
    try {
      this.releaseInstallerUrl =
          new URL(
              String.format(URL_TEMPLATE_INSTALLER, this.releaseInstaller, this.releaseInstaller));
    } catch (MalformedURLException ignored) {

    }
    this.installers.clear();
    for (int i = 0; i < installerManifest.getElementsByTagName("version").getLength(); i++) {
      installers.add(
          installerManifest
              .getElementsByTagName("version")
              .item(i)
              .getChildNodes()
              .item(0)
              .getNodeValue());
    }
    this.installerUrlMeta = new HashMap<>();
    this.installers.forEach(
        version -> {
          try {
            this.installerUrlMeta.put(version, installerUrl(version));
          } catch (MalformedURLException ignored) {

          }
        });
  }

  /**
   * Update this {@link QuiltInstaller} with information from the given {@link Document}.
   *
   * @param installerManifest {@link Document} containing new installer information.
   * @author Griefed
   */
  protected void update(Document installerManifest) {
    this.latestInstaller =
        installerManifest
            .getElementsByTagName("latest")
            .item(0)
            .getChildNodes()
            .item(0)
            .getNodeValue();
    this.releaseInstaller =
        installerManifest
            .getElementsByTagName("release")
            .item(0)
            .getChildNodes()
            .item(0)
            .getNodeValue();
    try {
      this.latestInstallerUrl =
          new URL(
              String.format(URL_TEMPLATE_INSTALLER, this.latestInstaller, this.latestInstaller));
    } catch (MalformedURLException ignored) {

    }
    try {
      this.releaseInstallerUrl =
          new URL(
              String.format(URL_TEMPLATE_INSTALLER, this.releaseInstaller, this.releaseInstaller));
    } catch (MalformedURLException ignored) {

    }
    this.installers.clear();
    for (int i = 0; i < installerManifest.getElementsByTagName("version").getLength(); i++) {
      installers.add(
          installerManifest
              .getElementsByTagName("version")
              .item(i)
              .getChildNodes()
              .item(0)
              .getNodeValue());
    }
    this.installerUrlMeta = new HashMap<>();
    this.installers.forEach(
        version -> {
          try {
            this.installerUrlMeta.put(version, installerUrl(version));
          } catch (MalformedURLException ignored) {

          }
        });
  }

  /**
   * Acquire the URL for the given Quilt version.
   *
   * @param quiltInstallerVersion {@link String} Quilt version.
   * @return {@link URL} to the installer for the given Quilt version.
   * @throws MalformedURLException if the URL could not be formed.
   * @author Griefed
   */
  private URL installerUrl(String quiltInstallerVersion) throws MalformedURLException {
    return new URL(
        String.format(URL_TEMPLATE_INSTALLER, quiltInstallerVersion, quiltInstallerVersion));
  }

  /**
   * Get a list of available installer versions for Quilt.
   *
   * @return {@link String}-list of available Quilt installer versions.
   * @author Griefed
   */
  protected List<String> installers() {
    return installers;
  }

  /**
   * Meta for the Quilt-Version-to-Installer-URL.<br>
   * key: {@link String} Quilt version.<br>
   * value: {@link URL} Quilt installer URL.
   *
   * @return {@link HashMap} with the Quilt-Version-to-Installer-URL.
   * @author Griefed
   */
  protected HashMap<String, URL> meta() {
    return installerUrlMeta;
  }

  /**
   * Get the latest Quilt installer version.
   *
   * @return {@link String} The latest Quilt installer version.
   * @author Griefed
   */
  protected String latestInstallerVersion() {
    return latestInstaller;
  }

  /**
   * Get the release Quilt installer version.
   *
   * @return {@link String} The release Quilt installer version.
   * @author Griefed
   */
  protected String releaseInstallerVersion() {
    return releaseInstaller;
  }

  /**
   * Get the {@link URL} to the latest Quilt installer.
   *
   * @return {@link URL} to the latest Quilt installer.
   * @author Griefed
   */
  protected URL latestInstallerUrl() {
    return latestInstallerUrl;
  }

  /**
   * Get the {@link URL} to the release Quilt installer.
   *
   * @return {@link URL} to the release Quilt installer.
   * @author Griefed
   */
  protected URL releaseInstallerUrl() {
    return releaseInstallerUrl;
  }
}
