/* Copyright (C) 2023  Griefed
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
package de.griefed.serverpackcreator.web.zip

import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.ConfigurationHandler
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import de.griefed.serverpackcreator.web.NotificationResponse
import de.griefed.serverpackcreator.web.task.TaskSubmitter
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Service class for backend tasks related to storing ZIP-archives uploaded through
 * [ZipController].
 *
 * @author Griefed
 */
@Service
class ZipService @Autowired constructor(
    private val apiProperties: ApiProperties,
    private val taskSubmitter: TaskSubmitter,
    private val configurationHandler: ConfigurationHandler,
    private val notificationResponse: NotificationResponse,
    private val versionMeta: VersionMeta
) {
    private val log = cachedLoggerOf(this.javaClass)

    /**
     * Store an uploaded ZIP-archive to disk.
     *
     * @param uploadedFile The file which was uploaded which you want to store on disk.
     * @return The path to the saved file.
     * @throws IOException If an I/O error occurs writing to or creating the file.
     * @author Griefed
     */
    @Throws(IOException::class)
    fun saveUploadedFile(uploadedFile: MultipartFile): Path {
        var zipPath: Path = File(apiProperties.modpacksDirectory, uploadedFile.originalFilename!!).toPath()

        // Does a archive with the same name already exist?
        if (zipPath.toFile().isFile) {
            var incrementation = 0
            val substring = zipPath.toString().substring(0, zipPath.toString().length - 4)
            while (File("${substring}_$incrementation.zip").isFile) {
                incrementation++
            }
            zipPath = Paths.get("${substring}_$incrementation.zip")
        }
        uploadedFile.transferTo(zipPath)
        return zipPath
    }

    /**
     * Submit a task for the generation of a server pack from a ZIP-archive.
     *
     * @param zipGenerationProperties String containing all information required to generate a server
     * pack from a ZIP-archive. See
     * [ZipController.requestGenerationFromZip].
     * @return `true` if the task was submitted.
     * @author Griefed
     */
    fun submitGenerationTask(zipGenerationProperties: String): String {
        val parameters = zipGenerationProperties.split("&".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        // Check if the requested ZIP-archive exists.
        if (!parameters[0].substring(parameters[0].length - 4).equals(".zip", ignoreCase = true)
            || !File(apiProperties.modpacksDirectory, parameters[0]).isFile
        ) {
            log.info(
                "ZIP-archive ${apiProperties.modpacksDirectory}/${parameters[0]} not found."
            )
            return notificationResponse.zipResponse(
                "ZIP-archive not found.", 5000, "error", "negative", parameters[0], false
            )
        }

        // Check the Minecraft version
        if (!versionMeta.minecraft.isMinecraftVersionAvailable(parameters[2])) {
            log.info("Minecraft version ${parameters[2]} incorrect.")
            return notificationResponse.zipResponse(
                "Incorrect Minecraft version: ${parameters[2]}", 5000, "error", "negative", null, false
            )
        }

        // Check the modloader version
        if (configurationHandler.checkModloader(parameters[3]).modloaderChecksPassed) {

            // Check Forge
            if (configurationHandler.getModLoaderCase(parameters[3]) == "Forge") {
                if (!versionMeta.forge.isForgeAndMinecraftCombinationValid(parameters[2], parameters[4])) {
                    log.info("${parameters[3]} version ${parameters[2]}-${parameters[4]} incorrect.")
                    return notificationResponse.zipResponse(
                        "Incorrect Forge version: ${parameters[2]}", 5000, "error", "negative", null, false
                    )
                }

                // Check Fabric
            } else {
                if (!versionMeta.fabric.isVersionValid(parameters[4])) {
                    log.info("${parameters[3]} version ${parameters[4]} incorrect.")
                    return notificationResponse.zipResponse(
                        "Incorrect Fabric version: ${parameters[4]}", 5000, "error", "negative", null, false
                    )
                }
            }
        } else {
            log.info("Modloader ${parameters[3]} incorrect.")
            return notificationResponse.zipResponse(
                "Modloader incorrect: ${parameters[3]}", 5000, "error", "negative", null, false
            )
        }
        taskSubmitter.generateZip(zipGenerationProperties)
        return notificationResponse.zipResponse(
            "Request submitted. Check the downloads section later on. Keep a look out for \"${parameters[0]}\" in the File Disk Name column.",
            7000,
            "info",
            "positive",
            parameters[0],
            true
        )
    }
}