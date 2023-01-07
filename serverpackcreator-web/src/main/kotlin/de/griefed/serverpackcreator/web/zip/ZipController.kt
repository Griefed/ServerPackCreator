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

import com.google.common.net.HttpHeaders
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.ConfigurationHandler
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.utilities.common.deleteQuietly
import de.griefed.serverpackcreator.web.NotificationResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.IOException

/**
 * RestController responsible for handling ZIP-archive uploads and server pack generation from the
 * very same.
 *
 * @author Griefed
 */
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/api/v1/zip")
@Suppress("unused")
class ZipController @Autowired constructor(
    private val zipService: ZipService,
    private val configurationHandler: ConfigurationHandler,
    private val notificationResponse: NotificationResponse,
    private val apiProperties: ApiProperties,
    private val utilities: Utilities
) {

    /**
     * Upload a file and check whether it is a ServerPackCreator valid ZIP-archive.
     *
     * @param file The file uploaded to ServerPackCreator.
     * @return A list on encountered errors, if any.
     * @throws IOException if an errors occurred saving or reading the file.
     * @author Griefed
     */
    @PostMapping("/upload")
    @Throws(IOException::class)
    fun handleFileUpload(@RequestParam("file") file: MultipartFile): ResponseEntity<String> {
        val encounteredErrors: List<String> = ArrayList(5)
        val pathToZip = zipService.saveUploadedFile(file)
        if (configurationHandler.checkZipArchive(
                pathToZip.toAbsolutePath().toString(), encounteredErrors.toMutableList()
            )
        ) {
            File(pathToZip.toString()).deleteQuietly()
            return ResponseEntity.badRequest()
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .body(
                    notificationResponse.zipResponse(
                        encounteredErrors,
                        10000,
                        "error",
                        "negative",
                        file.originalFilename!!,
                        false
                    )
                )
        }
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .body(
                notificationResponse.zipResponse(
                    "ZIP-file checks passed. You may press Submit. :)",
                    5000,
                    "info",
                    "positive",
                    pathToZip.toFile().name,
                    true
                )
            )
    }

    /**
     * Request the generation of a server pack from a previously uploaded ZIP-archive, which passed
     * validation checks, and from a barebones configuration, including:<br></br> `clientMods`<br></br>
     * `minecraftVersion`<br></br> `modLoader`<br></br> `modLoaderVersion`<br></br>
     *
     * @param zipName          The name of the previously uploaded ZIP-archive.
     * @param clientMods       A comma separated list of clientside-only mods to exclude from the
     * server pack.
     * @param minecraftVersion The Minecraft version the modpack, and therefor the server pack, uses.
     * @param modLoader        The modloader the modpack, and therefor the server pack, uses.
     * @param modLoaderVersion The modloader version the modpack, and therefor the server pack, uses.
     * @return Notification message with information about the result.
     * @author Griefed
     */
    @GetMapping("/{zipName}&{clientMods}&{minecraftVersion}&{modLoader}&{modLoaderVersion}")
    fun requestGenerationFromZip(
        @PathVariable("zipName") zipName: String,
        @PathVariable("clientMods") clientMods: String,
        @PathVariable("minecraftVersion") minecraftVersion: String,
        @PathVariable("modLoader") modLoader: String,
        @PathVariable("modLoaderVersion") modLoaderVersion: String
    ): ResponseEntity<String> {
        var mods = clientMods
        if (mods.isEmpty()) {
            mods = utilities.stringUtilities.buildString(apiProperties.clientSideMods())
        }
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .body(
                zipService.submitGenerationTask(
                    zipName
                            + "&"
                            + mods
                            + "&"
                            + minecraftVersion
                            + "&"
                            + modLoader
                            + "&"
                            + modLoaderVersion
                )
            )
    }
}