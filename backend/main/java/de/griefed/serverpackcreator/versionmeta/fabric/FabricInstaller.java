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

import de.griefed.serverpackcreator.utilities.common.Utilities;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Information about the Fabric installer.
 *
 * @author Griefed
 */
final class FabricInstaller {

  private final Utilities UTILITIES;
  private final String URL_TEMPLATE_INSTALLER =
      "https://maven.fabricmc.net/net/fabricmc/fabric-installer/%s/fabric-installer-%s.jar";
  private final File MANIFEST;
  private final List<String> installers = new ArrayList<>(100);
  private final HashMap<String, URL> installerUrlMeta = new HashMap<>(100);
  private String latestInstaller;
  private String releaseInstaller;
  private URL latestInstallerUrl;
  private URL releaseInstallerUrl;

  /**
   * Create a new instance of the Fabric Installer.
   *
   * @param manifest  Fabric installer information
   * @param utilities Commonly used utilities across ServerPackCreator.
   * @author Griefed
   */
  FabricInstaller(File manifest,
                  Utilities utilities) {
    MANIFEST = manifest;
    UTILITIES = utilities;
  }

  /**
   * Update the Fabric installer version by parsing the Fabric installer manifest.
   *
   * @author Griefed
   */
  void update() throws ParserConfigurationException, IOException, SAXException {
    Document document = UTILITIES.XmlUtilities().getXml(MANIFEST);

    latestInstaller =
        document
            .getElementsByTagName("latest")
            .item(0)
            .getChildNodes()
            .item(0)
            .getNodeValue();
    releaseInstaller =
        document
            .getElementsByTagName("release")
            .item(0)
            .getChildNodes()
            .item(0)
            .getNodeValue();
    try {
      latestInstallerUrl =
          new URL(
              String.format(URL_TEMPLATE_INSTALLER, latestInstaller, latestInstaller));
    } catch (MalformedURLException ignored) {

    }
    try {
      releaseInstallerUrl =
          new URL(
              String.format(URL_TEMPLATE_INSTALLER, releaseInstaller, releaseInstaller));
    } catch (MalformedURLException ignored) {

    }
    installers.clear();
    for (int i = 0; i < document.getElementsByTagName("version").getLength(); i++) {
      installers.add(
          document
              .getElementsByTagName("version")
              .item(i)
              .getChildNodes()
              .item(0)
              .getNodeValue());
    }
    installerUrlMeta.clear();
    installers.forEach(
        version -> {
          try {
            installerUrlMeta.put(version, installerUrl(version));
          } catch (MalformedURLException ignored) {

          }
        });
  }

  /**
   * Acquire the URL for the given Fabric version.
   *
   * @param fabricInstallerVersion Fabric version.
   * @return URL to the installer for the given Fabric version.
   * @throws MalformedURLException if the URL could not be formed.
   * @author Griefed
   */
  private URL installerUrl(String fabricInstallerVersion) throws MalformedURLException {
    return new URL(
        String.format(URL_TEMPLATE_INSTALLER, fabricInstallerVersion, fabricInstallerVersion));
  }

  /**
   * Get a list of available installer versions for Fabric.
   *
   * @return List of available Fabric installer versions.
   * @author Griefed
   */
  List<String> installers() {
    return installers;
  }

  /**
   * Meta for the Fabric-Version-to-Installer-URL.<br> key: {@link String} Fabric version.<br>
   * value: {@link URL} Fabric installer URL.
   *
   * @return Map with the Fabric-Version-to-Installer-URL.
   * @author Griefed
   */
  HashMap<String, URL> meta() {
    return installerUrlMeta;
  }

  /**
   * Get the latest Fabric installer version.
   *
   * @return The latest Fabric installer version.
   * @author Griefed
   */
  String latestInstallerVersion() {
    return latestInstaller;
  }

  /**
   * Get the release Fabric installer version.
   *
   * @return The release Fabric installer version.
   * @author Griefed
   */
  String releaseInstallerVersion() {
    return releaseInstaller;
  }

  /**
   * Get the {@link URL} to the latest Fabric installer.
   *
   * @return URL to the latest Fabric installer.
   * @author Griefed
   */
  URL latestInstallerUrl() {
    return latestInstallerUrl;
  }

  /**
   * Get the {@link URL} to the release Fabric installer.
   *
   * @return URL to the release Fabric installer.
   * @author Griefed
   */
  URL releaseInstallerUrl() {
    return releaseInstallerUrl;
  }

  /**
   * Get the {@link URL} to the improved Fabric launcher for the given Minecraft and Fabric
   * versions.
   *
   * @param minecraftVersion Minecraft version.
   * @param fabricVersion    Fabric version.
   * @return URL to the improved Fabric launcher, wrapped in an {@link Optional}.
   * @author Griefed
   */
  Optional<URL> improvedLauncherUrl(String minecraftVersion,
                                    String fabricVersion) {
    try {
      return Optional.of(
          new URL(
              String.format(
                  "https://meta.fabricmc.net/v2/versions/loader/%s/%s/%s/server/jar",
                  minecraftVersion, fabricVersion, releaseInstaller)));
    } catch (MalformedURLException ex) {
      return Optional.empty();
    }
  }
}
