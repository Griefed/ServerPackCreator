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
import de.griefed.serverpackcreator.versionmeta.ManifestParser;
import de.griefed.serverpackcreator.versionmeta.Meta;
import de.griefed.serverpackcreator.versionmeta.fabric.FabricIntermediaries;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
 * Quilt meta containing information about available Quilt versions and installers.
 *
 * @author Griefed
 */
public final class QuiltMeta extends ManifestParser implements Meta {

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
   * @author Griefed
   */
  public QuiltMeta(File quiltManifest, File quiltInstallerManifest,
      FabricIntermediaries injectedFabricIntermediaries) {

    QUILT_LOADER = new QuiltLoader(quiltManifest);
    QUILT_INSTALLER = new QuiltInstaller(quiltInstallerManifest);
    FABRIC_INTERMEDIARIES = injectedFabricIntermediaries;
  }

  @Override
  public void update() throws IOException, ParserConfigurationException, SAXException {
    QUILT_LOADER.update();
    QUILT_INSTALLER.update();
  }

  @Override
  public String latestLoader() {
    return QUILT_LOADER.latestLoaderVersion();
  }

  @Override
  public String releaseLoader() {
    return QUILT_LOADER.releaseLoaderVersion();
  }

  @Override
  public List<String> loaderVersionsListAscending() {
    return QUILT_LOADER.loaders();
  }

  @Override
  public List<String> loaderVersionsListDescending() {
    return Lists.reverse(QUILT_LOADER.loaders());
  }

  @Override
  public String[] loaderVersionsArrayAscending() {
    return QUILT_LOADER.loaders().toArray(new String[0]);
  }

  @Override
  public String[] loaderVersionsArrayDescending() {
    return Lists.reverse(QUILT_LOADER.loaders()).toArray(new String[0]);
  }

  @Override
  public String latestInstaller() {
    return QUILT_INSTALLER.latestInstallerVersion();
  }

  @Override
  public String releaseInstaller() {
    return QUILT_INSTALLER.releaseInstallerVersion();
  }

  @Override
  public List<String> installerVersionsListAscending() {
    return QUILT_INSTALLER.installers();
  }

  @Override
  public List<String> installerVersionsListDescending() {
    return Lists.reverse(QUILT_INSTALLER.installers());
  }

  @Override
  public String[] installerVersionsArrayAscending() {
    return QUILT_INSTALLER.installers().toArray(new String[0]);
  }

  @Override
  public String[] installerVersionsArrayDescending() {
    return Lists.reverse(QUILT_INSTALLER.installers()).toArray(new String[0]);
  }

  @Override
  public URL latestInstallerUrl() {
    return QUILT_INSTALLER.latestInstallerUrl();
  }

  @Override
  public URL releaseInstallerUrl() {
    return QUILT_INSTALLER.releaseInstallerUrl();
  }

  @Override
  public boolean isInstallerUrlAvailable(String quiltVersion) {
    return Optional.ofNullable(QUILT_INSTALLER.meta().get(quiltVersion)).isPresent();
  }

  @Override
  public Optional<URL> getInstallerUrl(String quiltVersion) {
    return Optional.ofNullable(QUILT_INSTALLER.meta().get(quiltVersion));
  }

  @Override
  public boolean isVersionValid(String quiltVersion) {
    return QUILT_LOADER.loaders().contains(quiltVersion);
  }

  @Override
  public boolean isMinecraftSupported(String minecraftVersion) {
    return FABRIC_INTERMEDIARIES.areIntermediariesPresent(minecraftVersion);
  }
}
