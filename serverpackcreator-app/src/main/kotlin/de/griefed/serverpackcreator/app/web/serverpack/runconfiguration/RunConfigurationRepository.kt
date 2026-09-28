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
package de.griefed.serverpackcreator.app.web.serverpack.runconfiguration

import de.griefed.serverpackcreator.app.web.serverpack.customizing.RunConfiguration
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.util.*

/** Stored run configurations, with the exact-match lookup the reuse depends on plus the filters the stats use. */
@Suppress("unused")
@Repository
interface RunConfigurationRepository : MongoRepository<RunConfiguration, String> {
    // lol, dat method name
    /**
     * The duplicate lookup: a configuration matching *every* field, lists included.
     * 
     * **LANDMINE — do not write `…In` in this method name.** `In` derives to "contains any of", not "equals", so an
     * earlier version of this query matched a stored configuration that shared a *single* mod with the incoming one
     * and handed back somebody else's server pack. The list parameters must compare as whole arrays.
     */
    @Suppress("SpringDataRepositoryMethodParametersInspection")
    fun findByMinecraftVersionAndModloaderAndModloaderVersionAndStartArgsAndClientModsAndWhitelistedMods(
        minecraftVersion: String,
        modloader: String,
        modloaderVersion: String,
        startArgs: MutableList<String>,
        clientMods: MutableList<String>,
        whitelistedMods: MutableList<String>
    ): Optional<RunConfiguration>

    /** Every configuration for one Minecraft version. */
    fun findAllByMinecraftVersion(minecraftVersion: String): List<RunConfiguration>
    /** Every configuration for one modloader. */
    fun findAllByModloader(modloader: String): List<RunConfiguration>
    /** Every configuration for one modloader build. */
    fun findAllByModloaderVersion(modloaderVersion: String): List<RunConfiguration>
    /** Every configuration for one loader *and* build pair. */
    fun findAllByModloaderAndModloaderVersion(modloader: String, modloaderVersion: String): List<RunConfiguration>
}