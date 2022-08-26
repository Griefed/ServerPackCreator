package de.griefed.serverpackcreator.versionmeta.legacyfabric;

import de.griefed.serverpackcreator.versionmeta.Manifests;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public final class LegacyFabricInstaller extends Manifests {

  private final File MANIFEST;
  private final List<String> ALL = new ArrayList<>();
  private final String INSTALLER_URL_TEMPLATE = "https://maven.legacyfabric.net/net/legacyfabric/fabric-installer/%s/fabric-installer-%s.jar";
  private String latest;
  private String release;

  public LegacyFabricInstaller(File installerVersionsManifest)
      throws ParserConfigurationException, IOException, SAXException {

    MANIFEST = installerVersionsManifest;
    update();
  }

  /**
   * Update all lists of available versions with new information gathered from the manifest.
   *
   * @throws IOException when the manifest could not be parsed.
   */
  void update() throws ParserConfigurationException, IOException, SAXException {
    Document installerManifest = getXml(MANIFEST);

    this.latest =
        installerManifest
            .getElementsByTagName("latest")
            .item(0)
            .getChildNodes()
            .item(0)
            .getNodeValue();

    this.release =
        installerManifest
            .getElementsByTagName("release")
            .item(0)
            .getChildNodes()
            .item(0)
            .getNodeValue();

    this.ALL.clear();

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
  public List<String> all() {
    return ALL;
  }

  /**
   * The latest version of the Legacy Fabric installer.
   *
   * @return Latest Legacy Fabric installer.
   * @author Griefed
   */
  public String latest() {
    return latest;
  }

  /**
   * The release version of the Legacy Fabric installer.
   *
   * @return Release version of the Legacy Fabric installer.
   * @author Griefed
   */
  public String release() {
    return release;
  }

  /**
   * The URL to the latest installer for Legacy Fabric.
   *
   * @return URL to the latest installer for Legacy Fabric.
   * @throws MalformedURLException when the URL could not be created.
   * @author Griefed
   */
  public URL latestURL() throws MalformedURLException {
    return new URL(String.format(INSTALLER_URL_TEMPLATE, latest, latest));
  }

  /**
   * The URL to the release installer for Legacy Fabric.
   *
   * @return URL to the release installer for Legacy Fabric.
   * @throws MalformedURLException when the URL could not be created.
   * @author Griefed
   */
  public URL releaseURL() throws MalformedURLException {
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
  public Optional<URL> specificURL(String version) throws MalformedURLException {
    if (ALL.contains(version)) {
      return Optional.of(new URL(String.format(INSTALLER_URL_TEMPLATE, version, version)));
    } else {
      return Optional.empty();
    }
  }
}
