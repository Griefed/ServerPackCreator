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

import de.griefed.serverpackcreator.versionmeta.ManifestParser;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Information about releases of the Quilt loader.
 *
 * @author Griefed
 */
final class QuiltLoader extends ManifestParser {

  private final File MANIFEST;
  private final List<String> loaders = new ArrayList<>(100);
  private String latest;
  private String release;

  /**
   * Create a new Quilt Loader instance.
   *
   * @param loaderManifest The manifest used when updating available versions.
   * @author Griefed
   */
  QuiltLoader(File loaderManifest) {
    MANIFEST = loaderManifest;
  }

  /**
   * Update the Quilt loader versions by parsing the Fabric loader manifest.
   *
   * @author Griefed
   */
  void update() throws ParserConfigurationException, IOException, SAXException {
    Document document = getXml(MANIFEST);

    latest =
        document
            .getElementsByTagName("latest")
            .item(0)
            .getChildNodes()
            .item(0)
            .getNodeValue();

    release =
        document
            .getElementsByTagName("release")
            .item(0)
            .getChildNodes()
            .item(0)
            .getNodeValue();
    loaders.clear();

    for (int i = 0; i < document.getElementsByTagName("version").getLength(); i++) {
      loaders.add(
          document
              .getElementsByTagName("version")
              .item(i)
              .getChildNodes()
              .item(0)
              .getNodeValue());
    }
  }

  /**
   * Get the latest Quilt loader version.
   *
   * @return The latest Quilt loader version.
   * @author Griefed
   */
  String latestLoaderVersion() {
    return latest;
  }

  /**
   * Get the release Quilt loader version.
   *
   * @return The release Quilt loader version.
   * @author Griefed
   */
  String releaseLoaderVersion() {
    return release;
  }

  /**
   * Get the list of available Quilt loader versions.
   *
   * @return List of the available Quilt loader versions.
   * @author Griefed
   */
  List<String> loaders() {
    return loaders;
  }
}
