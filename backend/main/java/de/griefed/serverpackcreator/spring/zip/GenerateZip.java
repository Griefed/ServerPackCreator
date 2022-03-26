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
package de.griefed.serverpackcreator.spring.zip;

import de.griefed.serverpackcreator.spring.task.Task;

/**
 * Task for generating a server pack from a modpack ZIP-archive.
 * @author Griefed
 */
public class GenerateZip extends Task {

    private final String zipGenerationProperties;

    /**
     * Create a message with a task for a Zip generation.
     * @author Griefed
     * @param zipGenerationProperties {@link String} The concatenated String which contains all information with which a server pack will be
     * generated from. See {@link ZipController#requestGenerationFromZip(String, String, String, String, String, boolean)}.
     */
    public GenerateZip(String zipGenerationProperties) {
        this.zipGenerationProperties = zipGenerationProperties;
    }

    /**
     * Getter for the concatenated String which contains all information with which a server pack will be
     * generated from. See {@link ZipController#requestGenerationFromZip(String, String, String, String, String, boolean)}.
     * @author Griefed
     * @return {@link String}. The String from which to generate a server pack.
     */
    public String getZipGenerationProperties() { return zipGenerationProperties; }

    /**
     * Getter for the unique id of the submitted task.
     * @author Griefed
     * @return String. Returns the unique id of the submitted task.
     */
    @Override
    public String uniqueId() {
        return "GENERATE_ZIP_" + zipGenerationProperties;
    }
}
