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
package de.griefed.serverpackcreator.api.versionmeta.fabric

import java.util.*

/**
 * Fabric loader details
 *
 * @author Griefed
 */
class FabricDetails internal constructor() {
    /**
     * Fabric loader ID, in the format of `fabric-loader-FABRIC_VERSION-MINECRAFT_VERSION `.
     *
     * @return The Fabric loader ID for the requested Minecraft and Fabric versions.
     * @author Griefed
     */
    val id: String? = null

    /**
     * The Minecraft version from which this Fabric loader version inherits from.
     *
     * @return The Minecraft version of this Fabric loader.
     * @author Griefed
     */
    val inheritsFrom: String? = null

    /**
     * The date at which this loader was released.
     *
     * @return The release date of this Fabric loader.
     * @author Griefed
     */
    val releaseTime: Date? = null

    /**
     * Probably the same as [.getReleaseTime]. Not sure. It's a field in the JSON you receive
     * from Fabric. - Griefed.
     *
     * @return The date of this Fabric loader.
     * @author Griefed
     */
    val time: Date? = null

    /**
     * The release type of this Fabric loader.
     *
     * @return Release type of this Fabric loader.
     * @author Griefed
     */
    val type: String? = null

    /**
     * The Main class of this Fabric loader.
     *
     * @return Main class.
     * @author Griefed
     */
    val mainClass: String? = null

    /**
     * [FabricArguments] used by this Fabric loader.
     *
     * @return Arguments of this Fabric loader.
     * @author Griefed
     */
    val arguments: FabricArguments? = null

    /**
     * [FabricLibrary]-list used by this Fabric loader.
     *
     * @return Library-list used by this Fabric loader.
     * @author Griefed
     */
    val libraries: List<FabricLibrary>? = null
}