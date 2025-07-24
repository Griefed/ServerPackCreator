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
package de.griefed.serverpackcreator.app.web.serverpack

import de.griefed.serverpackcreator.app.web.modpack.ModPackService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.util.MimeTypeUtils
import org.springframework.web.bind.annotation.*

/**
 * RestController for everything server pack related, like downloads.<br></br> All requests are in
 * `/api/v2/serverpacks`.
 *
 * @author Griefed
 */
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/api/v2/serverpacks")
class ServerPackController @Autowired constructor(
    private val serverPackService: ServerPackService,
    private val modpackService: ModPackService
) {

    /**
     * Download a server pack by its ID.
     *
     * @param id The id of the server pack.
     * @return Gives the requester the requested file as a download, if it was found.
     * @author Griefed
     */
    @GetMapping(value = ["/download/{id:[0-9a-zA-Z]+}"], produces = ["application/zip"])
    @ResponseBody
    fun downloadServerPack(@PathVariable id: String): ResponseEntity<Resource> {
        val serverPack = serverPackService.getServerPack(id)
        return if (serverPack.isPresent) {
            val archive = serverPackService.getServerPackArchive(serverPack.get())
            if (archive.isEmpty) {
                ResponseEntity.notFound().build()
            } else {
                serverPackService.updateDownloadStats(id)
                ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/zip"))
                    .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"${serverPack.get().fileName}_server_pack.zip\""
                    )
                    .body(ByteArrayResource(archive.get().readBytes()))
            }
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Download a server pack by the modpack and run-configuration ID.
     *
     * @param modPackId The id of the modpack from which the server pack was generated.
     * @param runConfigurationId The ID of the run-configuration with which the server pack was generated.
     * @return Gives the requester the requested file as a download, if it was found.
     * @author Griefed
     */
    @GetMapping(value = ["/download/{modPackId:[0-9]+}&{runConfigurationId:[0-9]+}"], produces = ["application/zip"])
    @ResponseBody
    fun downloadServerPack(
        @PathVariable modPackId: String,
        @PathVariable runConfigurationId: String
    ): ResponseEntity<Resource> {
        val modpack = modpackService.getModpack(modPackId)
        if (modpack.isEmpty) {
            return ResponseEntity.notFound().build()
        }
        var serverpack: ServerPack? = null
        for (pack in modpack.get().serverPacks) {
            if (pack.runConfiguration!!.id == runConfigurationId) {
                serverpack = pack
            }
        }
        if (serverpack == null) {
            return ResponseEntity.notFound().build()
        }
        return downloadServerPack(serverpack.id!!)
    }

    /**
     * Retrieve a list of all available server packs.
     *
     * @author Griefed
     */
    @GetMapping("/all", produces = ["application/json"])
    @ResponseBody
    fun getAllServerPacks(): ResponseEntity<List<ServerPack>> {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE).body(
            serverPackService.getServerPacks()
        )
    }

    @GetMapping("/allpaginated", produces = ["application/json"])
    @ResponseBody
    fun getAllServerPacksPaginated(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "100") size: Int
    ): ResponseEntity<List<ServerPack>> {
        val serverPacks = mutableListOf<ServerPack>()
        val sizedPage = PageRequest.of(page, size)
        val pageServerPacks = serverPackService.getServerPacks(sizedPage)
        serverPacks.addAll(pageServerPacks.content)
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE).body(
            serverPacks
        )
    }

    @GetMapping("/{id:[0-9a-zA-Z]+}", produces = ["application/json"])
    @ResponseBody
    fun getServerPack(@PathVariable id: String): ResponseEntity<ServerPack> {
        return if (serverPackService.getServerPack(id).isPresent) {
            ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE).body(
                serverPackService.getServerPack(id).get()
            )
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * GET request for voting whether a server pack works or not.
     *
     * @param id   The id of the server pack to vote for.
     * @param vote The vote, consisting of the id of the server pack and whether the vote should be
     * incremented or decremented. Example `42,up` or `23,down`.
     * @return ResponseEntity OK/BadRequest/NotFound
     * @author Griefed
     */
    @GetMapping("/vote/{id:[0-9a-zA-Z]+}&{vote}")
    @ResponseBody
    fun voteForServerPack(@PathVariable("id") id: String, @PathVariable("vote") vote: String): ResponseEntity<Any> {
        return serverPackService.voteForServerPack(id, vote)
    }
}