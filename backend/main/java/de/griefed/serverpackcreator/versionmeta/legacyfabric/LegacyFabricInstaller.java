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
package de.griefed.serverpackcreator.versionmeta.legacyfabric;

import de.griefed.serverpackcreator.utilities.common.Utilities;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.xml.parsers.ParserConfigurationException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public final class LegacyFabricInstaller {

  private final Utilities UTILITIES;
  private final File MANIFEST;
  private final List<String> ALL = new ArrayList<>(100);
  private final String INSTALLER_URL_TEMPLATE = "https://maven.legacyfabric.net/net/legacyfabric/fabric-installer/%s/fabric-installer-%s.jar";
  private String latest;
  private String release;

  /**
   * New instance holding information about the LegacyFabric installer and versions.
   *
   * @param installerVersionsManifest Manifest containing information about LegacyFabric installer
   *                                  versions.
   * @param utilities                 Commonly used utilities across ServerPackCreator.
   * @author Griefed
   */
  public LegacyFabricInstaller(@NotNull File installerVersionsManifest,
                               @NotNull Utilities utilities) {
    MANIFEST = installerVersionsManifest;
    UTILITIES = utilities;
  }

  /**
   * Update all lists of available versions with new information gathered from the manifest.
   *
   * @throws IOException when the manifest could not be parsed.
   */
  void update() throws ParserConfigurationException, IOException, SAXException {
    Document installerManifest = UTILITIES.XmlUtilities().getXml(MANIFEST);

    latest =
        installerManifest
            .getElementsByTagName("latest")
            .item(0)
            .getChildNodes()
            .item(0)
            .getNodeValue();

    release =
        installerManifest
            .getElementsByTagName("release")
            .item(0)
            .getChildNodes()
            .item(0)
            .getNodeValue();

    ALL.clear();

    for (int i = 0; i < installerManifest.getElementsByTagName("version").getLength(); i++) {
      ALL.add(
          installerManifest
              .getElementsByTagName("version")
              .item(i)
              .getChildNodes()
              .item(0)
              .getNodeValue());
    }
  }

  /**
   * All available installer versions.
   *
   * @return All available installer versions.
   * @author Griefed
   */
  @Contract(pure = true)
  public @NotNull List<String> all() {
    return ALL;
  }

  /**
   * The latest version of the Legacy Fabric installer.
   *
   * @return Latest Legacy Fabric installer.
   * @author Griefed
   */
  @Contract(pure = true)
  public @NotNull String latest() {
    return latest;
  }

  /**
   * The release version of the Legacy Fabric installer.
   *
   * @return Release version of the Legacy Fabric installer.
   * @author Griefed
   */
  @Contract(pure = true)
  public @NotNull String release() {
    return release;
  }

  /**
   * The URL to the latest installer for Legacy Fabric.
   *
   * @return URL to the latest installer for Legacy Fabric.
   * @throws MalformedURLException when the URL could not be created.
   * @author Griefed
   */
  @Contract(" -> new")
  public @NotNull URL latestURL() throws MalformedURLException {
    return new URL(String.format(INSTALLER_URL_TEMPLATE, latest, latest));
  }

  /**
   * The URL to the release installer for Legacy Fabric.
   *
   * @return URL to the release installer for Legacy Fabric.
   * @throws MalformedURLException when the URL could not be created.
   * @author Griefed
   */
  @Contract(" -> new")
  public @NotNull URL releaseURL() throws MalformedURLException {
    return new URL(String.format(INSTALLER_URL_TEMPLATE, release, latest));
  }

  /**
   * Get the URL for a specific installer version, wrapped in an Optional.
   *
   * @param version The version of the installer for which to get the URL.
   * @return URL to the installer, for the specified version, wrapped in an Optional.
   * @throws MalformedURLException when the URL could not be created.
   * @author Griefed
   */
  public @NotNull Optional<URL> specificURL(@NotNull String version) throws MalformedURLException {
    if (ALL.contains(version)) {
      return Optional.of(new URL(String.format(INSTALLER_URL_TEMPLATE, version, version)));
    } else {
      return Optional.empty();
    }
  }
}