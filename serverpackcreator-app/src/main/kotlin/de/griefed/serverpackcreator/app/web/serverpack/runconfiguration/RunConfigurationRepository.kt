/* Copyright (C) 2024  Griefed
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

import de.griefed.serverpackcreator.app.web.serverpack.customizing.ClientMod
import de.griefed.serverpackcreator.app.web.serverpack.customizing.RunConfiguration
import de.griefed.serverpackcreator.app.web.serverpack.customizing.StartArgument
import de.griefed.serverpackcreator.app.web.serverpack.customizing.WhitelistedMod
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface RunConfigurationRepository : MongoRepository<RunConfiguration, String> {
    // lol, dat method name
    @Suppress("SpringDataRepositoryMethodParametersInspection")
    fun findByMinecraftVersionAndModloaderAndModloaderVersionAndStartArgsInAndClientModsInAndWhitelistedModsIn(
        minecraftVersion: String,
        modloader: String,
        modloaderVersion: String,
        startArgs: MutableList<StartArgument>,
        clientMods: MutableList<ClientMod>,
        whitelistedMods: MutableList<WhitelistedMod>
    ): Optional<RunConfiguration>

    fun findAllByMinecraftVersion(minecraftVersion: String): List<RunConfiguration>
    fun findAllByModloader(modloader: String): List<RunConfiguration>
    fun findAllByModloaderVersion(modloaderVersion: String): List<RunConfiguration>
    fun findAllByModloaderAndModloaderVersion(modloader: String, modloaderVersion: String): List<RunConfiguration>
}