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

import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * Information about releases of the Fabric loader.
 * @author Griefed
 */
public class FabricLoader {

    private String latest;
    private String release;
    private final List<String> loaders = new ArrayList<>();

    /**
     * Constructor
     * @author Griefed
     * @param loaderManifest {@link Document} containing Fabrics manifest.
     */
    protected FabricLoader(Document loaderManifest) {

        this.latest = loaderManifest.getElementsByTagName("latest").item(0).getChildNodes().item(0).getNodeValue();
        this.release = loaderManifest.getElementsByTagName("release").item(0).getChildNodes().item(0).getNodeValue();
        this.loaders.clear();
        for (int i = 0; i < loaderManifest.getElementsByTagName("version").getLength(); i++) {
            loaders.add(loaderManifest.getElementsByTagName("version").item(i).getChildNodes().item(0).getNodeValue());
        }
    }

    /**
     * Update the latest, release and releases information.
     * @author Griefed
     * @param loaderManifest {@link Document} containing Fabrics manifest.
     * @return This instance of {@link FabricLoader}.
     */
    protected FabricLoader update(Document loaderManifest) {
        this.latest = loaderManifest.getElementsByTagName("latest").item(0).getChildNodes().item(0).getNodeValue();
        this.release = loaderManifest.getElementsByTagName("release").item(0).getChildNodes().item(0).getNodeValue();
        this.loaders.clear();
        for (int i = 0; i < loaderManifest.getElementsByTagName("version").getLength(); i++) {
            loaders.add(loaderManifest.getElementsByTagName("version").item(i).getChildNodes().item(0).getNodeValue());
        }

        return this;
    }

    /**
     * Get the latest Fabric loader version.
     * @author Griefed
     * @return {@link String} The latest Fabric loader version.
     */
    protected String latestLoaderVersion() {
        return latest;
    }

    /**
     * Get the release Fabric loader version.
     * @author Griefed
     * @return {@link String} The release Fabric loader version.
     */
    protected String releaseLoaderVersion() {
        return release;
    }

    /**
     * Get the list of available Fabric loader versions.
     * @author Griefed
     * @return {@link String}-list of the available Fabric loader versions.
     */
    protected List<String> loaders() {
        return loaders;
    }
}