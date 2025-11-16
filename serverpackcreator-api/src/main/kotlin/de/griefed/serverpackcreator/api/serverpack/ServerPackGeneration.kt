/* Copyright (C) 2025 Griefed
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
package de.griefed.serverpackcreator.api.serverpack

import de.griefed.serverpackcreator.api.config.PackConfig
import java.io.File
import java.util.*

/**
 * Returned by [de.griefed.serverpackcreator.api.serverpack.ServerPackHandler.run]. Contains information about the
 * generated server pack, or the server pack which was attempted to be generated.
 *
 * @author Griefed
 */
class ServerPackGeneration(
    /**
     * The generated server pack as a file. This usually points towards a directory on the file-system.
     */
    val serverPack: File,

    /**
     * List of errors encountered during server pack generation.
     */
    val errors: List<String>,

    /**
     * The server pack ZIP-archive, if one was generated. [Optional] for ease of use, as not every server pack generation
     * also creates a ZIP-archive.
     */
    val serverPackZip: Optional<File>,

    /**
     * The configuration used in the generation of this server pack.
     */
    val packConfig: PackConfig,

    /**
     * A list of all files in the server pack. These are absolute files, mind you. If you need relative-files, iterate
     * through this list and remove the path to the server pack from each entry to receive a list of relative paths.
     */
    val files: List<File>
) {
    /**
     * Whether the generation was successful.
     */
    val success: Boolean
        get() {
            return errors.isEmpty()
        }
}