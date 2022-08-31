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
package de.griefed.serverpackcreator.versionmeta;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

public interface Meta {

  /**
   * Update the meta-information for this modloader-meta, updating the available loader and
   * installer versions, thus giving you access to version-checks, URLs etc.
   *
   * @throws ParserConfigurationException indicates a serious configuration error.
   * @throws IOException                  if any IO errors occur.
   * @throws SAXException                 if any parse errors occur.
   * @author Griefed
   */
  void update() throws IOException, ParserConfigurationException, SAXException;

  /**
   * Get the latest loader version.
   *
   * @return The latest version.
   * @author Griefed
   */
  String latestLoader();

  /**
   * Get the release loader version.
   *
   * @return The release loader version.
   * @author Griefed
   */
  String releaseLoader();

  /**
   * Get the latest installer version.
   *
   * @return The latest installer version.
   * @author Griefed
   */
  String latestInstaller();

  /**
   * Get the release installer version.
   *
   * @return The release installer version.
   * @author Griefed
   */
  String releaseInstaller();

  /**
   * List of available loader versions in ascending order.
   *
   * @return Available loader versions in ascending order.
   * @author Griefed
   */
  List<String> loaderVersionsListAscending();

  /**
   * List of available loader versions in descending order.
   *
   * @return Available loader versions in descending order.
   * @author Griefed
   */
  List<String> loaderVersionsListDescending();

  /**
   * Array of available loader versions in ascending order.
   *
   * @return Available loader versions in ascending order.
   * @author Griefed
   */
  String[] loaderVersionsArrayAscending();

  /**
   * Array of available loader versions in descending order.
   *
   * @return Available loader versions in descending order.
   * @author Griefed
   */
  String[] loaderVersionsArrayDescending();

  /**
   * List of available installer version in ascending order.
   *
   * @return Available installer version in ascending order.
   * @author Griefed
   */
  List<String> installerVersionsListAscending();

  /**
   * List of available installer version in descending order.
   *
   * @return Available installer version in descending order.
   * @author Griefed
   */
  List<String> installerVersionsListDescending();

  /**
   * Array of available installer version in ascending order.
   *
   * @return Available installer version in ascending order.
   * @author Griefed
   */
  String[] installerVersionsArrayAscending();

  /**
   * Array of available installer version in descending order.
   *
   * @return Available installer version in descending order.
   * @author Griefed
   */
  String[] installerVersionsArrayDescending();

  /**
   * Get the URL to the latest installer.
   *
   * @return URL to the latest installer.
   * @throws MalformedURLException when the URL could not be created.
   * @author Griefed
   */
  URL latestInstallerUrl() throws MalformedURLException;

  /**
   * Get the URL to the release installer.
   *
   * @return URL to the release installer.
   * @throws MalformedURLException when the URL could not be created.
   * @author Griefed
   */
  URL releaseInstallerUrl() throws MalformedURLException;

  /**
   * Check whether a URL to an installer is available for the specified version.
   *
   * @param version The modloader version for which to check for installer availability.
   * @return <code>true</code> if available.
   * @author Griefed
   */
  boolean isInstallerUrlAvailable(String version);

  /**
   * Get the URL to the installer for the specified version, wrapped in an Optional.
   *
   * @param version The modloader version for which to get the installer.
   * @return The URL to the installer, wrapped in an Optional.
   * @throws MalformedURLException when the URL could not be created.
   * @author Griefed
   */
  Optional<URL> getInstallerUrl(String version) throws MalformedURLException;

  /**
   * Check whether the specified version is available/correct/valid.
   *
   * @param version The version to check.
   * @return <code>true</code> if the specified version is available/correct/valid.
   * @author Griefed
   */
  boolean isVersionValid(String version);

  /**
   * Check whether the given Minecraft version is supported by this modloader.
   *
   * @param minecraftVersion The Minecraft version for which to check for support.
   * @return <code>true</code> if the specified Minecraft version is supported.
   * @author Griefed
   */
  boolean isMinecraftSupported(String minecraftVersion);
}
