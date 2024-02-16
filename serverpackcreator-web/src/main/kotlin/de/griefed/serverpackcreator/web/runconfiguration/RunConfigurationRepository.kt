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
package de.griefed.serverpackcreator.web.runconfiguration

import de.griefed.serverpackcreator.web.data.ClientMod
import de.griefed.serverpackcreator.web.data.RunConfiguration
import de.griefed.serverpackcreator.web.data.StartArgument
import de.griefed.serverpackcreator.web.data.WhitelistedMod
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface RunConfigurationRepository : JpaRepository<RunConfiguration, Int> {
    // lol, dat method name
    fun findByMinecraftVersionAndModloaderAndModloaderVersionAndStartArgsInAndClientModsInAndWhitelistedModsIn(
        minecraftVersion: String,
        modloader: String,
        modloaderVersion: String,
        startArgs: Collection<StartArgument>,
        clientMods: Collection<ClientMod>,
        whitelistedMods: Collection<WhitelistedMod>
    ): Optional<RunConfiguration>

    fun findAllByMinecraftVersion(minecraftVersion: String): List<RunConfiguration>
    fun findAllByModloader(modloader: String): List<RunConfiguration>
    fun findAllByModloaderVersion(modloaderVersion: String): List<RunConfiguration>
    fun findAllByModloaderAndModloaderVersion(modloader: String, modloaderVersion: String): List<RunConfiguration>
}