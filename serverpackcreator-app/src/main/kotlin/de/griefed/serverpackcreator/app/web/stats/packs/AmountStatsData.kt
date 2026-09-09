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
package de.griefed.serverpackcreator.app.web.stats.packs

/**
 * The dashboard's plain counts: how much has been stored, and which versions and loaders it is spread across.
 * 
 * The three maps are value-to-count, not lists, so the SPA can render a distribution without counting client-side.
 */
data class AmountStatsData(
    /** How many modpacks are stored. */
    val modPacks: Int,
    /** How many server packs are stored. */
    val serverPacks: Int,
    /** How many distinct run configurations exist — lower than [serverPacks] whenever configurations were reused. */
    val runConfigurations: Int,
    /** Minecraft version to how many server packs use it. */
    val minecraftVersions: HashMap<String, Int>,
    /** Modloader to how many server packs use it. */
    val modloaders: HashMap<String, Int>,
    /** Modloader build to how many server packs use it. */
    val modloaderVersions: HashMap<String, Int>
)
