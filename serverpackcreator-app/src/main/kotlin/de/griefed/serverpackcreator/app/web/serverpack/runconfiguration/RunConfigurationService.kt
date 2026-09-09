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

import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.app.web.serverpack.customizing.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

/**
 * Stores and reuses run configurations. **The reuse is the point:** an incoming configuration identical to a
 * stored one is answered with the stored one, so two users asking for the same pack share it.
 */
@Service
class RunConfigurationService @Autowired constructor(
    private val runConfigurationRepository: RunConfigurationRepository,
    private val apiProperties: ApiProperties
) {
    private val spaces : Regex = "\\s+".toRegex()
    private val commaSpace: String = ", "
    private val comma: String = ","
    private val space: String = " "

    /** Build an entity from what a request supplied, filling in the shipped defaults for anything it left out. */
    fun createRunConfig(
        minecraftVersion: String,
        modloader: String,
        modloaderVersion: String,
        startArgs: String,
        clientMods: String,
        whitelistedMods: String
    ): RunConfiguration {
        val config = RunConfiguration()
        config.minecraftVersion = minecraftVersion
        config.modloader = modloader
        config.modloaderVersion = modloaderVersion

        // No per-entry lookup-or-store any more: these are plain strings embedded in the document, so
        // there is nothing to resolve. Each list used to cost one findBy per entry plus a save per miss
        // -- on the default clientside list that is ~550 sequential round-trips to build one config.
        if (startArgs.isNotBlank()) {
            config.startArgs.addAll(startArgs.replace(spaces, space).split(space))
        } else {
            config.startArgs.addAll(apiProperties.aikarsFlags.replace(spaces, space).split(space))
        }

        if (clientMods.isNotBlank()) {
            config.clientMods.addAll(clientMods.replace(commaSpace, comma).split(comma))
        } else {
            config.clientMods.addAll(apiProperties.clientSideMods())
        }

        if (whitelistedMods.isNotBlank()) {
            config.whitelistedMods.addAll(whitelistedMods.replace(commaSpace, comma).split(comma))
        } else {
            config.whitelistedMods.addAll(apiProperties.whitelistedMods())
        }

        return save(config)
    }

    /** Store a configuration, returning the existing one when an exact match is already stored. */
    fun save(runConfiguration: RunConfiguration): RunConfiguration {
        val fromRepo =
            runConfigurationRepository.findByMinecraftVersionAndModloaderAndModloaderVersionAndStartArgsAndClientModsAndWhitelistedMods(
                minecraftVersion = runConfiguration.minecraftVersion,
                modloader = runConfiguration.modloader,
                modloaderVersion = runConfiguration.modloaderVersion,
                startArgs = runConfiguration.startArgs,
                clientMods = runConfiguration.clientMods,
                whitelistedMods = runConfiguration.whitelistedMods
            )
        return if (fromRepo.isPresent) {
            fromRepo.get()
        } else {
            runConfigurationRepository.save(runConfiguration)
        }
    }

    /** Store a configuration assembled from its parts, otherwise as the overload above. */
    fun save(
        minecraftVersion: String,
        modloader: String,
        modloaderVersion: String,
        startArgs: MutableList<String>,
        clientMods: MutableList<String>,
        whitelistedMods: MutableList<String>
    ): RunConfiguration {
        return save(
            RunConfiguration(
            minecraftVersion = minecraftVersion,
            modloader = modloader,
            modloaderVersion = modloaderVersion,
            startArgs = startArgs,
            clientMods = clientMods,
            whitelistedMods = whitelistedMods
        )
        )
    }

    /** One configuration by id. */
    fun load(id: String): Optional<RunConfiguration> {
        return runConfigurationRepository.findById(id)
    }

    /** Find a configuration by its *contents* rather than its id — the exact-match lookup the reuse depends on. */
    fun load(
        minecraftVersion: String,
        modloader: String,
        modloaderVersion: String,
        startArgs: MutableList<String>,
        clientMods: MutableList<String>,
        whitelistedMods: MutableList<String>
    ): Optional<RunConfiguration> {
        return runConfigurationRepository.findByMinecraftVersionAndModloaderAndModloaderVersionAndStartArgsAndClientModsAndWhitelistedMods(
            minecraftVersion = minecraftVersion,
            modloader = modloader,
            modloaderVersion = modloaderVersion,
            startArgs = startArgs,
            clientMods = clientMods,
            whitelistedMods = whitelistedMods
        )
    }

    /** Every stored configuration. */
    fun loadAll(): List<RunConfiguration> {
        return runConfigurationRepository.findAll()
    }
}