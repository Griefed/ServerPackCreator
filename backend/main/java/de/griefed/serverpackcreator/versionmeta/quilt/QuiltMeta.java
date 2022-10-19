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
import de.griefed.serverpackcreator.utilities.common.Utilities;
import de.griefed.serverpackcreator.versionmeta.Meta;
import de.griefed.serverpackcreator.versionmeta.fabric.FabricIntermediaries;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import javax.xml.parsers.ParserConfigurationException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.SAXException;

/**
 * Quilt meta containing information about available Quilt versions and installers.
 *
 * @author Griefed
 */
public final class QuiltMeta implements Meta {

  private final QuiltLoader QUILT_LOADER;
  private final QuiltInstaller QUILT_INSTALLER;
  private final FabricIntermediaries FABRIC_INTERMEDIARIES;

  /**
   * Create a new Quilt Meta instance, giving you access to available loader and installer versions,
   * as well as URLs to installer for further processing.
   *
   * @param quiltManifest                Quilt manifest file.
   * @param quiltInstallerManifest       Quilt-installer manifest file.
   * @param injectedFabricIntermediaries Fabric-Intermediaries for further compatibility tests.
   * @param utilities                    Commonly used utilities across ServerPackCreator.
   * @author Griefed
   */
  public QuiltMeta(@NotNull File quiltManifest,
                   @NotNull File quiltInstallerManifest,
                   @NotNull FabricIntermediaries injectedFabricIntermediaries,
                   @NotNull Utilities utilities) {

    QUILT_LOADER = new QuiltLoader(quiltManifest, utilities);
    QUILT_INSTALLER = new QuiltInstaller(quiltInstallerManifest, utilities);
    FABRIC_INTERMEDIARIES = injectedFabricIntermediaries;
  }

  @Override
  public void update() throws IOException, ParserConfigurationException, SAXException {
    QUILT_LOADER.update();
    QUILT_INSTALLER.update();
  }

  @Contract(pure = true)
  @Override
  public @NotNull String latestLoader() {
    return QUILT_LOADER.latestLoaderVersion();
  }

  @Contract(pure = true)
  @Override
  public @NotNull String releaseLoader() {
    return QUILT_LOADER.releaseLoaderVersion();
  }

  @Contract(pure = true)
  @Override
  public @NotNull String latestInstaller() {
    return QUILT_INSTALLER.latestInstallerVersion();
  }

  @Contract(pure = true)
  @Override
  public @NotNull String releaseInstaller() {
    return QUILT_INSTALLER.releaseInstallerVersion();
  }

  @Contract(pure = true)
  @Override
  public @NotNull List<String> loaderVersionsListAscending() {
    return QUILT_LOADER.loaders();
  }

  @Override
  public @NotNull List<String> loaderVersionsListDescending() {
    return Lists.reverse(QUILT_LOADER.loaders());
  }

  @Override
  public String @NotNull [] loaderVersionsArrayAscending() {
    return QUILT_LOADER.loaders().toArray(new String[0]);
  }

  @Override
  public String @NotNull [] loaderVersionsArrayDescending() {
    return Lists.reverse(QUILT_LOADER.loaders()).toArray(new String[0]);
  }

  @Contract(pure = true)
  @Override
  public @NotNull List<String> installerVersionsListAscending() {
    return QUILT_INSTALLER.installers();
  }

  @Override
  public @NotNull List<String> installerVersionsListDescending() {
    return Lists.reverse(QUILT_INSTALLER.installers());
  }

  @Override
  public String @NotNull [] installerVersionsArrayAscending() {
    return QUILT_INSTALLER.installers().toArray(new String[0]);
  }

  @Override
  public String @NotNull [] installerVersionsArrayDescending() {
    return Lists.reverse(QUILT_INSTALLER.installers()).toArray(new String[0]);
  }

  @Contract(pure = true)
  @Override
  public @NotNull URL latestInstallerUrl() {
    return QUILT_INSTALLER.latestInstallerUrl();
  }

  @Contract(pure = true)
  @Override
  public @NotNull URL releaseInstallerUrl() {
    return QUILT_INSTALLER.releaseInstallerUrl();
  }

  @Override
  public boolean isInstallerUrlAvailable(String quiltVersion) {
    return Optional.ofNullable(QUILT_INSTALLER.meta().get(quiltVersion)).isPresent();
  }

  @Override
  public @NotNull Optional<URL> getInstallerUrl(@NotNull String quiltVersion) {
    return Optional.ofNullable(QUILT_INSTALLER.meta().get(quiltVersion));
  }

  @Override
  public boolean isVersionValid(@NotNull String quiltVersion) {
    return QUILT_LOADER.loaders().contains(quiltVersion);
  }

  @Override
  public boolean isMinecraftSupported(@NotNull String minecraftVersion) {
    return FABRIC_INTERMEDIARIES.isIntermediariesPresent(minecraftVersion);
  }
}
