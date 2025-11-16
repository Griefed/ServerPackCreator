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
package de.griefed.serverpackcreator.app.web.stats.packs

import de.griefed.serverpackcreator.app.web.modpack.ModPackRepository
import de.griefed.serverpackcreator.app.web.serverpack.ServerPackRepository
import de.griefed.serverpackcreator.app.web.serverpack.runconfiguration.RunConfigurationRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AmountStatsService @Autowired constructor(
    private val serverPackRepository: ServerPackRepository,
    private val modpackRepository: ModPackRepository,
    private val runConfigurationRepository: RunConfigurationRepository
) {
    val stats: AmountStatsData
        get() {
            val modloaders = hashMapOf<String, Int>()
            val minecraftVersions = HashMap<String, Int>()
            val modloaderVersions = HashMap<String, Int>()

            for (serverPack in serverPackRepository.findAll()) {
                val runConfig = serverPack.runConfiguration!!
                if (!minecraftVersions.containsKey(runConfig.minecraftVersion)) {
                    minecraftVersions[runConfig.minecraftVersion] = 1
                } else {
                    minecraftVersions[runConfig.minecraftVersion] = minecraftVersions[runConfig.minecraftVersion]!! + 1
                }

                if (!modloaders.containsKey(runConfig.modloader)) {
                    modloaders[runConfig.modloader] = 1
                } else {
                    modloaders[runConfig.modloader] = modloaders[runConfig.modloader]!! + 1
                }

                val modloaderVersion = "${runConfig.modloader}-${runConfig.modloaderVersion}"
                if (!modloaderVersions.containsKey(modloaderVersion)) {
                    modloaderVersions[modloaderVersion] = 1
                } else {
                    modloaderVersions[modloaderVersion] = modloaderVersions[modloaderVersion]!! + 1
                }
            }

            return AmountStatsData(
                modpackRepository.findAll().size,
                serverPackRepository.findAll().size,
                runConfigurationRepository.findAll().size,
                minecraftVersions,
                modloaders,
                modloaderVersions
            )
        }
}