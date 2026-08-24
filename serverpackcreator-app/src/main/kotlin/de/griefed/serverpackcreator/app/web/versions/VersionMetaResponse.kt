/* Copyright (C) 2026 Griefed
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
package de.griefed.serverpackcreator.app.web.versions

/**
 * Everything the SPA needs to populate its version pickers, in one response.
 * 
 * Note the two shapes: Fabric-family loaders are a flat list because one loader build serves every Minecraft
 * version, while Forge and NeoForge are keyed *by* Minecraft version because their builds are version-specific.
 */
@Suppress("unused")
class VersionMetaResponse(
    /** Minecraft versions SPC can generate for, newest first. */
    val minecraft: List<String>,
    /** Fabric loader versions, which apply across Minecraft versions. */
    val fabric: List<String>,
    /** LegacyFabric loader versions. */
    val legacyFabric: List<String>,
    /** Quilt loader versions. */
    val quilt: List<String>,
    /** Minecraft version to the Forge builds available for it. */
    val forge: HashMap<String, List<String>>,
    /** Minecraft version to the NeoForge builds available for it. */
    val neoForge: HashMap<String,List<String>>
)