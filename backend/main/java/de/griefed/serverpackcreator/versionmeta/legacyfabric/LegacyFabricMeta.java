package de.griefed.serverpackcreator.versionmeta.legacyfabric;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import de.griefed.serverpackcreator.versionmeta.Manifests;
import de.griefed.serverpackcreator.versionmeta.Meta;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

public final class LegacyFabricMeta extends Manifests implements Meta {

  private final LegacyFabricGame GAME_VERSIONS;
  private final LegacyFabricLoader LOADER_VERSIONS;
  private final LegacyFabricInstaller INSTALLER_VERSIONS;

  public LegacyFabricMeta(File gameVersionsManifest, File loaderVersionsManifest,
      File installerVersionsManifest, ObjectMapper mapper)
      throws IOException, ParserConfigurationException, SAXException {

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
