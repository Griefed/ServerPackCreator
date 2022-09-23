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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import de.griefed.serverpackcreator.versionmeta.ManifestParser;
import de.griefed.serverpackcreator.versionmeta.Meta;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

public final class LegacyFabricMeta extends ManifestParser implements Meta {

  private final LegacyFabricGame GAME_VERSIONS;
  private final LegacyFabricLoader LOADER_VERSIONS;
  private final LegacyFabricInstaller INSTALLER_VERSIONS;

  public LegacyFabricMeta(File gameVersionsManifest, File loaderVersionsManifest,
      File installerVersionsManifest, ObjectMapper mapper) {

    GAME_VERSIONS = new LegacyFabricGame(gameVersionsManifest, mapper);
    LOADER_VERSIONS = new LegacyFabricLoader(loaderVersionsManifest, mapper);
    INSTALLER_VERSIONS = new LegacyFabricInstaller(installerVersionsManifest);
  }

  @Override
  public void update() throws IOException, ParserConfigurationException, SAXException {
    GAME_VERSIONS.update();
    LOADER_VERSIONS.update();
    INSTALLER_VERSIONS.update();
  }

  @Override
  public String latestLoader() {
    return LOADER_VERSIONS.all().get(0);
  }

  @Override
  public String releaseLoader() {
    return LOADER_VERSIONS.releases().get(0);
  }

  @Override
  public String latestInstaller() {
    return INSTALLER_VERSIONS.latest();
  }

  @Override
  public String releaseInstaller() {
    return INSTALLER_VERSIONS.release();
  }

  @Override
  public List<String> loaderVersionsListAscending() {
    return LOADER_VERSIONS.all();
  }

  @Override
  public List<String> loaderVersionsListDescending() {
    return Lists.reverse(LOADER_VERSIONS.all());
  }

  @Override
  public String[] loaderVersionsArrayAscending() {
    return loaderVersionsListAscending().toArray(new String[0]);
  }

  @Override
  public String[] loaderVersionsArrayDescending() {
    return loaderVersionsListDescending().toArray(new String[0]);
  }

  @Override
  public List<String> installerVersionsListAscending() {
    return INSTALLER_VERSIONS.all();
  }

  @Override
  public List<String> installerVersionsListDescending() {
    return Lists.reverse(INSTALLER_VERSIONS.all());
  }

  @Override
  public String[] installerVersionsArrayAscending() {
    return installerVersionsListAscending().toArray(new String[0]);
  }

  @Override
  public String[] installerVersionsArrayDescending() {
    return installerVersionsListDescending().toArray(new String[0]);
  }

  @Override
  public URL latestInstallerUrl() throws MalformedURLException {
    return INSTALLER_VERSIONS.latestURL();
  }

  @Override
  public URL releaseInstallerUrl() throws MalformedURLException {
    return INSTALLER_VERSIONS.releaseURL();
  }

  @Override
  public boolean isInstallerUrlAvailable(String version) {
    try {
      return INSTALLER_VERSIONS.specificURL(version).isPresent();
    } catch (MalformedURLException e) {
      return false;
    }
  }

  @Override
  public Optional<URL> getInstallerUrl(String version) throws MalformedURLException {
    return INSTALLER_VERSIONS.specificURL(version);
  }

  @Override
  public boolean isVersionValid(String version) {
    return LOADER_VERSIONS.all().contains(version);
  }

  @Override
  public boolean isMinecraftSupported(String minecraftVersion) {
    return GAME_VERSIONS.all().contains(minecraftVersion);
  }

  /**
   * All Legacy Fabric supported Minecraft versions.
   *
   * @return All Legacy Fabric supported Minecraft versions.
   * @author Griefed
   */
  public List<String> supportedMinecraftVersions() {
    return GAME_VERSIONS.all();
  }
}
