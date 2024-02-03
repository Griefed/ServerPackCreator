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
package de.griefed.serverpackcreator.web.modpack

import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.ConfigurationHandler
import de.griefed.serverpackcreator.api.PackConfig
import de.griefed.serverpackcreator.web.data.ModPack
import de.griefed.serverpackcreator.web.data.ModPackView
import de.griefed.serverpackcreator.web.data.RunConfiguration
import de.griefed.serverpackcreator.web.storage.StorageSystem
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.util.*

@Service
class ModpackService @Autowired constructor(
    private val modpackRepository: ModpackRepository,
    private val configurationHandler: ConfigurationHandler,
    apiProperties: ApiProperties,
) {
    private val rootLocation: Path = apiProperties.modpacksDirectory.toPath()
    private val storage: StorageSystem = StorageSystem(rootLocation)

    fun saveZipModpack(file: MultipartFile): ModPack {
        val modpack = ModPack()
        modpack.status = ModpackStatus.QUEUED
        modpack.name = file.originalFilename ?: file.name
        modpack.size = file.size.toDouble().div(1048576.0).toInt()
        modpack.source = ModpackSource.ZIP
        val storePair = storage.storeOnFilesystem(file).get()
        modpack.fileID = storePair.first
        modpack.sha256 = storePair.second
        return modpackRepository.save(modpack)
    }

    fun saveModpack(modpack: ModPack): ModPack {
        return modpackRepository.save(modpack)
    }

    fun getModpack(id: Int): Optional<ModPack> {
        return modpackRepository.findById(id)
    }

    fun getModpackView(id: Int): Optional<ModPackView> {
        return modpackRepository.findProjectedById(id)
    }

    fun getModpacks(): List<ModPackView> {
        return modpackRepository.findAllProjectedBy(Sort.by(Sort.Direction.DESC, "dateCreated"))
    }

    fun getPackConfigForModpack(modpack: ModPack, runConfiguration: RunConfiguration): PackConfig {
        val packConfig = PackConfig()
        packConfig.modpackDir = rootLocation.resolve("${modpack.fileID}.zip").normalize().toFile().absolutePath
        packConfig.setClientMods(runConfiguration.clientMods.map { it.mod }.toMutableList())
        packConfig.setModsWhitelist(runConfiguration.whitelistedMods.map { it.mod }.toMutableList())
        if (modpack.status == ModpackStatus.GENERATING) {
            packConfig.inclusions.addAll(configurationHandler.suggestInclusions(packConfig.modpackDir))
        }
        packConfig.minecraftVersion = runConfiguration.minecraftVersion
        packConfig.modloader = runConfiguration.modloader
        packConfig.modloaderVersion = runConfiguration.modloaderVersion
        packConfig.javaArgs = runConfiguration.startArgs.joinToString(" ") { it.argument }
        packConfig.isZipCreationDesired = true
        return packConfig
    }

    fun deleteModpack(id: Int) {
        val modpack = modpackRepository.findById(id)
        if (modpack.isPresent) {
            modpackRepository.deleteById(id)
            if (modpack.get().fileID != null) {
                storage.delete(modpack.get().fileID!!)
            }
        }
    }

    /**
     * Retrieve the archive of a previously uploaded modpack.
     *
     * @param modPack The modpack for which to retrieve the archive.
     * @return The modpack-zip, wrapped in an [Optional]
     * @throws IOException If an I/O error occurs writing to or creating the file.
     * @throws IllegalArgumentException If the modpack doesn't have data to export.
     * @author Griefed
     */
    @Throws(IOException::class, IllegalArgumentException::class)
    fun getModPackArchive(modPack: ModPack): Optional<File> {
        return storage.load(modPack.fileID!!)
    }

    fun getModPackArchive(fileID: Long): Optional<File> {
        return storage.load(fileID)
    }
}