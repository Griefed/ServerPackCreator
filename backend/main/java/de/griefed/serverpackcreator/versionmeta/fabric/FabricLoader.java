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
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Information about releases of the Fabric loader.
 *
 * @author Griefed
 */
final class FabricLoader {

  private final Utilities UTILITIES;
  private final List<String> loaders = new ArrayList<>(100);
  private final File MANIFEST;
  private String latest;
  private String release;

  /**
   * Create a new instance of the Fabric Loader.
   *
   * @param loaderManifest The manifest used when updating available versions.
   * @param utilities      Commonly used utilities across ServerPackCreator.
   * @author Griefed
   */
  FabricLoader(File loaderManifest,
               Utilities utilities) {
    MANIFEST = loaderManifest;
    UTILITIES = utilities;
  }

  /**
   * Update the Fabric loader versions by parsing the Fabric loader manifest.
   *
   * @author Griefed
   */
  void update() throws ParserConfigurationException, IOException, SAXException {
    Document document = UTILITIES.XmlUtilities().getXml(MANIFEST);
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
   * Get the latest Fabric loader version.
   *
   * @return The latest Fabric loader version.
   * @author Griefed
   */
  String latestLoaderVersion() {
    return latest;
  }

  /**
   * Get the release Fabric loader version.
   *
   * @return The release Fabric loader version.
   * @author Griefed
   */
  String releaseLoaderVersion() {
    return release;
  }

  /**
   * Get the list of available Fabric loader versions.
   *
   * @return List of the available Fabric loader versions.
   * @author Griefed
   */
  List<String> loaders() {
    return loaders;
  }
}
