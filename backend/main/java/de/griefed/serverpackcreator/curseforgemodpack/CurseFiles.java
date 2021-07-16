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
 * <strong>Table of methods</strong><br>
 * {@link #getProjectID()}<br>
 * {@link #setProjectID(String)}<br>
 * {@link #getFileID()}<br>
 * {@link #setFileID(String)}<br>
 * {@link #toString()}<p>
 * Retrieves information about a CurseForge Minecraft modpack by using {@linkplain com.fasterxml.jackson.databind} JSON parsing.
 * This class retrieves the projectIDs and fileIDs a modpack acquired from CurseForge depends on. These can be mods,
 * resource packs, worlds etc. etc.
 */
public class CurseFiles {
    /**
     * Ignore unknown values/object. We only want to gather specific information and disregard the rest. Setting this
     * property allows us setup the class to only gather the information we want, so we don't have to worry about any
     * additions to the source-data being made, which would otherwise cause an "Unknown property" exception.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private String projectID;
    private String fileID;

    /**
     * Getter for a projectID of a dependency in the CurseForge modpack.
     * @return String. Returns a projectID of a dependency in the modpack acquired from CurseForge.
     */
    public String getProjectID() {
        return projectID;
    }

    /**
     * Setter for a projectID of a dependency in the CurseForge modpack.
     * @param projectID Receives the projectID of a dependency in the modpack acquired from CurseForge.
     */
    public void setProjectID(String projectID) {
        this.projectID = projectID;
    }

    /**
     * Getter for a fileID of a dependency in the CurseForge modpack.
     * @return String. Returns a fileID of a dependency in the modpack acquired from CurseForge.
     */
    public String getFileID() {
        return fileID;
    }

    /**
     * Setter for a fileID of a dependency in the CurseForge modpack.
     * @param fileID Receives the fileID of a dependency in the modpack acquired from CurseForge.
     */
    public void setFileID(String fileID) {
        this.fileID = fileID;
    }

    /**
     * A comma separated combination of a dependency projectID and fileID of a CurseForge modpack.
     * @return String. Returns a comma separated projectID,fileID combination of a dependency of a modpack acquired from
     * CurseForge.
     */
    @Override
    public String toString() {
        return String.format("%s,%s",projectID,fileID);
    }
}