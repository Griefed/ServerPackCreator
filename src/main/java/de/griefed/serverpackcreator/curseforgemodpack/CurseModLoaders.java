/* Copyright (C) 2021  Griefed
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
package de.griefed.serverpackcreator.curseforgemodpack;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 *
 */
public class CurseModLoaders {
    /**
     * Ignore unknown values/object. We only want to gather specific information and disregard the rest. Setting this
     * property allows us setup the class to only gather the information we want, so we don't have to worry about any
     * additions to the source-data being made, which would otherwise cause an "Unknown property" exception.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private String id;

    /**
     * Getter for the modloader and modloader version used by the CurseForge modpack. It's a single string in the
     * manifest.json which contains the modloader and the version of said modloader, for example <code>"id": "forge-36.1.4"</code>
     * @return String. Returns the id of the modpack acquired from CurseForge containing information about the modloader
     * and the version of said modloader.
     */
    public String getId() {
        return id;
    }

    /**
     * Setter for the modloader and modloader version used by the CurseForge modpack. It's a single string in the
     * manifest.json which contains the modloader and the version of said modloader, for example <code>"id": "forge-36.1.4"</code>
     * @param id Receives the id of the CurseForge modpack containing information about the modloader
     * and the version of said modloader.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * String containing information about the CurseForge modpack's used modloader and the modloader version.
     * @return String. Returns the String with information about the modloader and modloader version used by the
     * CurseForge modpack.
     */
    @Override
    public String toString() {
        return id;
    }
}