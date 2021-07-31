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
 * {@link #getMinecraft()}<br>
 * {@link #setMinecraft(List)}<br>
 * {@link #getName()}<br>
 * {@link #setName(String)}<br>
 * {@link #getVersion()}<br>
 * {@link #setVersion(String)}<br>
 * {@link #getAuthor()}<br>
 * {@link #setAuthor(String)}<br>
 * {@link #getFiles()}<br>
 * {@link #setFiles(List)}<br>
 * {@link #toString()}<p>
 * Retrieve information about a CurseForge Minecraft modpack by using {@linkplain com.fasterxml.jackson.databind} JSON parsing.
 * This class retrieves the name, version and author of a modpack.
 * @author Griefed
 */
public class CurseModpack {
    /**
     * Ignore unknown values/object. We only want to gather specific information and disregard the rest. Setting this
     * property allows us to set up the class to only gather the information we want, so we don't have to worry about any
     * additions to the source-data being made, which would otherwise cause an "Unknown property" exception.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private List<CurseMinecraft> minecraft;
    private String name;
    private String version;
    private String author;
    private List<CurseFiles> files;

    /**
     * Getter for Minecraft related information using {@link CurseMinecraft}.
     * @author Griefed
     * @return List CurseMinecraft. Returns an instance of {@link CurseMinecraft} with information related to Minecraft.
     */
    public List<CurseMinecraft> getMinecraft() {
        return minecraft;
    }

    /**
     * Setter for Minecraft related information using {@link CurseMinecraft}
     * @author Griefed
     * @param newMinecraft Receives an instance of the CurseMinecraft.class.
     */
    public void setMinecraft(List<CurseMinecraft> newMinecraft) {
        this.minecraft = newMinecraft;
    }

    /**
     * Getter for the name of the CurseForge modpack.
     * @author Griefed
     * @return String. Returns the name of modpack acquired from CurseForge.
     */
    public String getName() {
        return name;
    }

    /**
     * Setter for the name of the CurseForge modpack.
     * @author Griefed
     * @param newName Receives the name of the CurseForge modpack.
     */
    public void setName(String newName) {
        this.name = newName;
    }

    /**
     * Getter for the version of the CurseForge modpack.
     * @author Griefed
     * @return String. Returns the version of the modpack acquired from CurseForge.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Setter for the version of the CurseForge modpack.
     * @author Griefed
     * @param newVersion Receives the version of the modpack acquired from CurseForge.
     */
    public void setVersion(String newVersion) {
        this.version = newVersion;
    }

    /**
     * Getter for the author of the CurseForge modpack.
     * @author Griefed
     * @return String. Returns the author of the modpack acquired from CurseForge.
     */
    public String getAuthor() {
        return author;
    }

    /**
     * Setter for the author of the CurseForge modpack.
     * @author Griefed
     * @param newAuthor Receives the author of the modpack acquired from CurseForge.
     */
    public void setAuthor(String newAuthor) {
        this.author = newAuthor;
    }

    /**
     * Getter for the files on which the CurseForge modpack depends by using {@link CurseFiles}.
     * @author Griefed
     * @return List CurseFiles. Returns an instance of {@link CurseFiles} with information related to the files included
     * in the CurseForge modpack.
     */
    public List<CurseFiles> getFiles() {
        return files;
    }

    /**
     * Setter for the CurseForge modpack files on which it depends.
     * @author Griefed
     * @param newFiles Receives an instance of {@link CurseFiles} with information about the files on which the modpack depends.
     */
    public void setFiles(List<CurseFiles> newFiles) {
        this.files = newFiles;
    }

    /**
     * String containing information about the CurseForge modpack. Included are {@link #getMinecraft()},
     * {@link #getName()}, {@link #getVersion()}, {@link #getAuthor()}, {@link #getFiles()}.
     * @author Griefed
     * @return String. Returns String with information about the CurseForge modpack.
     */
    @Override
    public String toString() {
        return String.format(
                "Modpack details: Version & Modloader: %s Name: %s Version: %s Author: %s Files: %s",
                getMinecraft(), getName(), getVersion(), getAuthor(), getFiles()
        );
    }
}