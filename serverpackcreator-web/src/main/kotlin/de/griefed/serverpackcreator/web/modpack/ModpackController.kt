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

import de.griefed.serverpackcreator.web.data.ModPackView
import de.griefed.serverpackcreator.web.data.ServerPack
import de.griefed.serverpackcreator.web.data.ZipResponse
import de.griefed.serverpackcreator.web.runconfiguration.RunConfigurationService
import de.griefed.serverpackcreator.web.storage.StorageException
import de.griefed.serverpackcreator.web.task.TaskDetail
import de.griefed.serverpackcreator.web.task.TaskExecutionServiceImpl
import org.apache.logging.log4j.kotlin.KotlinLogger
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.util.MimeTypeUtils
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/api/v2/modpacks")
class ModpackController @Autowired constructor(
    private val modpackService: ModpackService,
    private val runConfigurationService: RunConfigurationService,
    private val taskExecutionServiceImpl: TaskExecutionServiceImpl
) {
    private val logger: KotlinLogger = cachedLoggerOf(this.javaClass)

    @GetMapping("/download/{id:[0-9]+}", produces = ["application/zip"])
    @ResponseBody
    fun downloadModpack(@PathVariable id: Int): ResponseEntity<Resource> {
        val modpack = modpackService.getModpack(id)
        if (modpack.isEmpty) {
            return ResponseEntity.notFound().build()
        }
        val modpackArchive = modpackService.getModPackArchive(modpack.get())
        if (modpackArchive.isEmpty) {
            return ResponseEntity.notFound().build()
        }
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/zip"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${modpack.get().name}\"")
            .body(ByteArrayResource(modpackArchive.get().readBytes()))
    }

    @PostMapping("/upload", produces = ["application/json"])
    @ResponseBody
    fun uploadModPack(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("minecraftVersion") minecraftVersion: String,
        @RequestParam("modloader") modloader: String,
        @RequestParam("modloaderVersion") modloaderVersion: String,
        @RequestParam("startArgs") startArgs: String,
        @RequestParam("clientMods") clientMods: String,
        @RequestParam("whiteListMods") whiteListMods: String
    ): ResponseEntity<ZipResponse> {
        var zipResponse: ZipResponse
        if (file.size == 0L ||
            file.bytes.isEmpty() ||
            minecraftVersion.isEmpty() ||
            modloader.isEmpty() ||
            modloaderVersion.isEmpty()
        ) {
            zipResponse = ZipResponse(
                message = "Invalid ZIP-file, Minecraft version, Modloader or Modloader version configuration.",
                success = false,
                modPackId = null,
                runConfigId = null,
                serverPackId = null,
                status = ModpackStatus.ERROR
            )
            return ResponseEntity.badRequest().header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)
                .body(zipResponse)
        }
        val runConfig = runConfigurationService.createRunConfig(
            minecraftVersion, modloader, modloaderVersion, startArgs, clientMods, whiteListMods
        )
        try {
            val modpack = modpackService.saveZipModpack(file)
            val taskDetail = TaskDetail(modpack)
            taskDetail.runConfiguration = runConfig
            taskExecutionServiceImpl.submitTaskInQueue(taskDetail)
            zipResponse = ZipResponse(
                message = "File is being stored and will be queued for checks.",
                success = true,
                modPackId = modpack.id,
                runConfigId = taskDetail.runConfiguration?.id,
                serverPackId = null,
                status = ModpackStatus.QUEUED
            )
            return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)
                .body(zipResponse)
        } catch (ex: StorageException) {
            zipResponse = ZipResponse(
                message = ex.message!!,
                success = false,
                modPackId = ex.id,
                runConfigId = runConfig.id,
                serverPackId = null,
                status = ModpackStatus.ERROR
            )
            return ResponseEntity.badRequest().header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)
                .body(zipResponse)
        }
    }

    @PostMapping("/generate", produces = ["application/json"])
    @ResponseBody
    fun requestGeneration(
        @RequestParam("modPackID") modPackID: Int,
        @RequestParam("minecraftVersion") minecraftVersion: String,
        @RequestParam("modloader") modloader: String,
        @RequestParam("modloaderVersion") modloaderVersion: String,
        @RequestParam("startArgs") startArgs: String,
        @RequestParam("clientMods") clientMods: String,
        @RequestParam("whiteListMods") whiteListMods: String
    ): ResponseEntity<ZipResponse> {
        val modpack = modpackService.getModpack(modPackID)
        if (modpack.isEmpty) {
            return ResponseEntity.badRequest().header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)
                .body(
                    ZipResponse(
                        message = "Modpack not found.",
                        success = false,
                        modPackId = modPackID,
                        runConfigId = null,
                        serverPackId = null,
                        status = ModpackStatus.ERROR
                    )
                )
        }
        val taskDetail = TaskDetail(modpack.get())
        taskDetail.modpack.status = ModpackStatus.QUEUED
        taskDetail.runConfiguration = runConfigurationService.createRunConfig(
            minecraftVersion, modloader, modloaderVersion, startArgs, clientMods, whiteListMods
        )
        var serverpack: ServerPack? = null
        for (pack in modpack.get().serverPacks) {
            if (pack.runConfiguration!!.id == taskDetail.runConfiguration!!.id) {
                serverpack = pack
            }
        }
        if (serverpack != null) {
            return ResponseEntity.badRequest().header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)
                .body(
                    ZipResponse(
                        message = "Server Pack already exists for the requested ModPack and RunConfiguration.",
                        success = false,
                        modPackId = modPackID,
                        runConfigId = taskDetail.runConfiguration!!.id,
                        serverPackId = serverpack.id,
                        status = ModpackStatus.GENERATED
                    )
                )
        }
        taskExecutionServiceImpl.submitTaskInQueue(taskDetail)
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)
            .body(
                ZipResponse(
                    message = "Generation of ServerPack, from existing ModPack, with different config, queued.",
                    success = true,
                    modPackId = modPackID,
                    runConfigId = taskDetail.runConfiguration?.id,
                    serverPackId = null,
                    status = ModpackStatus.QUEUED
                )
            )
    }

    @GetMapping("/all", produces = ["application/json"])
    @ResponseBody
    fun getAllModPacks(): ResponseEntity<List<ModPackView>> {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE).body(
            modpackService.getModpacks()
        )
    }

    @GetMapping("/{id:[0-9]+}", produces = ["application/json"])
    @ResponseBody
    fun getModpack(@PathVariable id: Int): ResponseEntity<ModPackView> {
        return if (modpackService.getModpackView(id).isPresent) {
            ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE).body(
                modpackService.getModpackView(id).get()
            )
        } else {
            ResponseEntity.notFound().header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE).build()
        }
    }
}