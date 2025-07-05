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
package de.griefed.serverpackcreator.app.web.modpack

import de.griefed.serverpackcreator.app.web.serverpack.ServerPack
import de.griefed.serverpackcreator.app.web.serverpack.runconfiguration.RunConfigurationService
import de.griefed.serverpackcreator.app.web.storage.StorageException
import de.griefed.serverpackcreator.app.web.task.TaskDetail
import de.griefed.serverpackcreator.app.web.task.TaskExecutionServiceImpl
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.util.MimeTypeUtils
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/api/v2/modpacks")
class ModPackController @Autowired constructor(
    private val modpackService: ModPackService,
    private val runConfigurationService: RunConfigurationService,
    private val taskExecutionServiceImpl: TaskExecutionServiceImpl
) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

    @GetMapping("/download/{id:[0-9a-zA-Z]+}", produces = ["application/zip"])
    @ResponseBody
    fun downloadModpack(@PathVariable id: String): ResponseEntity<Resource> {
        val modpack = modpackService.getModpack(id)
        if (modpack.isEmpty) {
            log.warn("Modpack with ID $id not found")
            return ResponseEntity.notFound().build()
        }
        val modpackArchive = modpackService.getModPackArchive(modpack.get())
        if (modpackArchive.isEmpty) {
            log.warn("No archive found for modpack with ID $id")
            return ResponseEntity.notFound().build()
        }
        modpackService.updateDownloadStats(modpack.get())
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
                status = ModPackStatus.ERROR
            )
            return ResponseEntity.badRequest().header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)
                .body(zipResponse)
        }
        val runConfig = runConfigurationService.createRunConfig(
            minecraftVersion, modloader, modloaderVersion, startArgs, clientMods, whiteListMods
        )
        try {
            val modpack = modpackService.saveUploadedFile(file)
            val taskDetail = TaskDetail(modpack)
            taskDetail.runConfiguration = runConfig
            taskExecutionServiceImpl.submitTaskInQueue(taskDetail)
            zipResponse = ZipResponse(
                message = "File is being stored and will be queued for checks.",
                success = true,
                modPackId = modpack.id,
                runConfigId = taskDetail.runConfiguration?.id,
                serverPackId = null,
                status = ModPackStatus.QUEUED
            )
            return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)
                .body(zipResponse)
        } catch (ex: StorageException) {
            zipResponse = ZipResponse(
                message = ex.message!!,
                success = false,
                modPackId = ex.id.toString(),
                runConfigId = runConfig.id,
                serverPackId = null,
                status = ModPackStatus.ERROR
            )
            return ResponseEntity.badRequest().header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)
                .body(zipResponse)
        }
    }

    @PostMapping("/generate", produces = ["application/json"])
    @ResponseBody
    fun requestGeneration(
        @RequestParam("modPackID") modPackID: String,
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
                        status = ModPackStatus.ERROR
                    )
                )
        }
        val taskDetail = TaskDetail(modpack.get())
        taskDetail.modpack.status = ModPackStatus.QUEUED
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
                        status = ModPackStatus.GENERATED
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
                    status = ModPackStatus.QUEUED
                )
            )
    }

    @GetMapping("/all", produces = ["application/json"])
    @ResponseBody
    fun getAllModPacks(): ResponseEntity<List<ModPack>> {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE).body(
            modpackService.getModpacks()
        )
    }

    @GetMapping("/allpaginated", produces = ["application/json"])
    @ResponseBody
    fun getAllModPacksPaginated(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "100") size: Int
    ): ResponseEntity<List<ModPack>> {
        val modpacks = mutableListOf<ModPack>()
        val sizedPage = PageRequest.of(page, size)
        val pageModpacks = modpackService.getModpacks(sizedPage)
        modpacks.addAll(pageModpacks.content)
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE).body(
            modpacks
        )
    }

    @GetMapping("/{id:[0-9a-zA-Z]+}", produces = ["application/json"])
    @ResponseBody
    fun getModpack(@PathVariable id: String): ResponseEntity<ModPack> {
        val pack = modpackService.getModpack(id)
        return if (pack.isPresent) {
            ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE).body(
                pack.get()
            )
        } else {
            ResponseEntity.notFound().header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE).build()
        }
    }

    @GetMapping("byserverpack/{id:[0-9a-zA-Z]+}", produces = ["application/json"])
    @ResponseBody
    fun getModPackByServerPack(@PathVariable id: String): ResponseEntity<ModPack> {
        val pack = modpackService.getByServerPack(id)
        return if (pack.isPresent) {
            ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE).body(
                pack.get()
            )
        } else {
            ResponseEntity.notFound().header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE).build()
        }
    }
}