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

import com.google.common.collect.Lists;
import de.griefed.serverpackcreator.utilities.common.Utilities;
import de.griefed.serverpackcreator.versionmeta.Meta;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import javax.xml.parsers.ParserConfigurationException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.SAXException;

public final class LegacyFabricMeta implements Meta {

  private final LegacyFabricGame GAME_VERSIONS;
  private final LegacyFabricLoader LOADER_VERSIONS;
  private final LegacyFabricInstaller INSTALLER_VERSIONS;

  /**
   * LegacyFabric meta providing game, loader and installer version information.
   *
   * @param gameVersionsManifest      Game version manifest.
   * @param loaderVersionsManifest    Loader version manifest.
   * @param installerVersionsManifest Installer version manifest.
   * @param utilities                 Commonly used utilities across ServerPackCreator.
   */
  public LegacyFabricMeta(File gameVersionsManifest,
                          File loaderVersionsManifest,
                          File installerVersionsManifest,
                          Utilities utilities) {

    GAME_VERSIONS = new LegacyFabricGame(gameVersionsManifest, utilities);
    LOADER_VERSIONS = new LegacyFabricLoader(loaderVersionsManifest, utilities);
    INSTALLER_VERSIONS = new LegacyFabricInstaller(installerVersionsManifest, utilities);
  }

  @Override
  public void update() throws IOException, ParserConfigurationException, SAXException {
    GAME_VERSIONS.update();
    LOADER_VERSIONS.update();
    INSTALLER_VERSIONS.update();
  }

  @Override
  public @NotNull String latestLoader() {
    return LOADER_VERSIONS.all().get(0);
  }

  @Override
  public @NotNull String releaseLoader() {
    return LOADER_VERSIONS.releases().get(0);
  }

  @Contract(pure = true)
  @Override
  public @NotNull String latestInstaller() {
    return INSTALLER_VERSIONS.latest();
  }

  @Contract(pure = true)
  @Override
  public @NotNull String releaseInstaller() {
    return INSTALLER_VERSIONS.release();
  }

  @Override
  public @NotNull List<String> loaderVersionsListAscending() {
    return LOADER_VERSIONS.all();
  }

  @Override
  public @NotNull List<String> loaderVersionsListDescending() {
    return Lists.reverse(LOADER_VERSIONS.all());
  }

  @Override
  public String @NotNull [] loaderVersionsArrayAscending() {
    return loaderVersionsListAscending().toArray(new String[0]);
  }

  @Override
  public String @NotNull [] loaderVersionsArrayDescending() {
    return loaderVersionsListDescending().toArray(new String[0]);
  }

  @Contract(pure = true)
  @Override
  public @NotNull List<String> installerVersionsListAscending() {
    return INSTALLER_VERSIONS.all();
  }

  @Override
  public @NotNull List<String> installerVersionsListDescending() {
    return Lists.reverse(INSTALLER_VERSIONS.all());
  }

  @Override
  public String @NotNull [] installerVersionsArrayAscending() {
    return installerVersionsListAscending().toArray(new String[0]);
  }

  @Override
  public String @NotNull [] installerVersionsArrayDescending() {
    return installerVersionsListDescending().toArray(new String[0]);
  }

  @Contract(" -> new")
  @Override
  public @NotNull URL latestInstallerUrl() throws MalformedURLException {
    return INSTALLER_VERSIONS.latestURL();
  }

  @Contract(" -> new")
  @Override
  public @NotNull URL releaseInstallerUrl() throws MalformedURLException {
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
  public @NotNull Optional<URL> getInstallerUrl(@NotNull String version) throws MalformedURLException {
    return INSTALLER_VERSIONS.specificURL(version);
  }

  @Override
  public boolean isVersionValid(@NotNull String version) {
    return LOADER_VERSIONS.all().contains(version);
  }

  @Override
  public boolean isMinecraftSupported(@NotNull String minecraftVersion) {
    return GAME_VERSIONS.all().contains(minecraftVersion);
  }

  /**
   * All Legacy Fabric supported Minecraft versions.
   *
   * @return All Legacy Fabric supported Minecraft versions.
   * @author Griefed
   */
  public @NotNull List<String> supportedMinecraftVersions() {
    return GAME_VERSIONS.all();
  }
}