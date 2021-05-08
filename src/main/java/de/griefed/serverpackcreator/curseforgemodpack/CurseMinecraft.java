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

import java.util.List;

/**
 * <strong>Table of methods</strong><br>
 * {@link #getVersion()}<br>
 * {@link #setVersion(String)}<br>
 * {@link #getModLoaders()}<br>
 * {@link #setModLoaders(List)}<br>
 * {@link #toString()}<p>
 * Retrieves information about a CurseForge Minecraft modpack by using {@linkplain com.fasterxml.jackson.databind} JSON parsing.
 * This class retrieves the Minecraft version of a modpack.
 */
public class CurseMinecraft {
    /**
     * Ignore unknown values/object. We only want to gather specific information and disregard the rest. Setting this
     * property allows us setup the class to only gather the information we want, so we don't have to worry about any
     * additions to the source-data being made, which would otherwise cause an "Unknown property" exception.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private String version;
    private List<CurseModLoaders> modLoaders;

    /**
     * Getter for the Minecraft version used by the CurseForge modpack.
     * @return String. Returns the Minecraft version used by the modpack acquired from CurseForge.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Setter for the Minecraft version used by the CurseForge modpack.
     * @param version Receives the Minecraft version of the CurseForge modpack.
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Getter for modloader related information using {@link CurseModLoaders}.
     * @return List CurseModLoaders. Returns an instance of CurseModLoaders.class with information about the modloader
     * and modloader version used by the CurseForge modpack.
     */
    public List<CurseModLoaders> getModLoaders() {
        return modLoaders;
    }

    /**
     * Setter for modloader related information using {@link CurseModLoaders}.
     * @param modLoaders Receives an instance of CurseModLoaders.class.
     */
    public void setModLoaders(List<CurseModLoaders> modLoaders) {
        this.modLoaders = modLoaders;
    }

    /**
     * String containing information about the Minecraft version, modloader and modloader version used by the CurseForge
     * modpack.
     * @return String. Returns a String with the Minecraft version, used modloader and modloader version of a modpack
     * acquired from CurseForge.
     */
    @Override
    public String toString() {
        return String.format("%s,%s",version,modLoaders);
    }
}