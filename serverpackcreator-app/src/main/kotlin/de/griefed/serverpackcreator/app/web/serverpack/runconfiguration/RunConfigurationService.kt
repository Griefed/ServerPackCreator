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

import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.app.web.serverpack.customizing.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

@Service
class RunConfigurationService @Autowired constructor(
    private val runConfigurationRepository: RunConfigurationRepository,
    private val apiProperties: ApiProperties,
    private val clientModRepository: ClientModRepository,
    private val whitelistedModRepository: WhitelistedModRepository,
    private val startArgumentRepository: StartArgumentRepository
) {
    private val spaces : Regex = "\\s+".toRegex()
    private val commaSpace: String = ", "
    private val comma: String = ","
    private val space: String = " "

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

        if (startArgs.isNotBlank()) {
            for (argument in startArgs.replace(spaces, space).split(space)) {
                config.startArgs.add(StartArgument(argument))
            }
        } else {
            config.startArgs.addAll(apiProperties.aikarsFlags.replace(spaces, space).split(space).map { StartArgument(it) })
        }
        for (i in 0 until config.startArgs.size) {
            if (startArgumentRepository.findByArgument(config.startArgs[i].argument).isPresent) {
                config.startArgs[i] = startArgumentRepository.findByArgument(config.startArgs[i].argument).get()
            } else {
                config.startArgs[i] = startArgumentRepository.save(config.startArgs[i])
            }
        }

        if (clientMods.isNotBlank()) {
            for (mod in clientMods.replace(commaSpace, comma).split(comma)) {
                config.clientMods.add(ClientMod(mod))
            }
        } else {
            config.clientMods.addAll(apiProperties.clientSideMods().map { ClientMod(it) })
        }
        for (i in 0 until config.clientMods.size) {
            if (clientModRepository.findByMod(config.clientMods[i].mod).isPresent) {
                config.clientMods[i] = clientModRepository.findByMod(config.clientMods[i].mod).get()
            } else {
                config.clientMods[i] = clientModRepository.save(config.clientMods[i])
            }
        }

        if (whitelistedMods.isNotBlank()) {
            for (mod in whitelistedMods.replace(commaSpace, comma).split(comma)) {
                config.whitelistedMods.add(WhitelistedMod(mod))
            }
        } else {
            config.whitelistedMods.addAll(apiProperties.whitelistedMods().map { WhitelistedMod(it) })
        }
        for (i in 0 until config.whitelistedMods.size) {
            if (whitelistedModRepository.findByMod(config.whitelistedMods[i].mod).isPresent) {
                config.whitelistedMods[i] =
                    whitelistedModRepository.findByMod(config.whitelistedMods[i].mod).get()
            } else {
                config.whitelistedMods[i] = whitelistedModRepository.save(config.whitelistedMods[i])
            }
        }

        return save(config)
    }

    fun save(runConfiguration: RunConfiguration): RunConfiguration {
        val fromRepo =
            runConfigurationRepository.findByMinecraftVersionAndModloaderAndModloaderVersionAndStartArgsInAndClientModsInAndWhitelistedModsIn(
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

    fun save(
        minecraftVersion: String,
        modloader: String,
        modloaderVersion: String,
        startArgs: MutableList<StartArgument>,
        clientMods: MutableList<ClientMod>,
        whitelistedMods: MutableList<WhitelistedMod>
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

    fun load(id: String): Optional<RunConfiguration> {
        return runConfigurationRepository.findById(id)
    }

    fun load(
        minecraftVersion: String,
        modloader: String,
        modloaderVersion: String,
        startArgs: MutableList<StartArgument>,
        clientMods: MutableList<ClientMod>,
        whitelistedMods: MutableList<WhitelistedMod>
    ): Optional<RunConfiguration> {
        return runConfigurationRepository.findByMinecraftVersionAndModloaderAndModloaderVersionAndStartArgsInAndClientModsInAndWhitelistedModsIn(
            minecraftVersion = minecraftVersion,
            modloader = modloader,
            modloaderVersion = modloaderVersion,
            startArgs = startArgs,
            clientMods = clientMods,
            whitelistedMods = whitelistedMods
        )
    }

    fun loadAll(): List<RunConfiguration> {
        return runConfigurationRepository.findAll()
    }
}