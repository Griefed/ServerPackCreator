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
package de.griefed.serverpackcreator.web.serverpack

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * RestController for everything server pack related, like downloads.<br></br> All requests are in
 * `/api/v1/packs`.
 *
 * @author Griefed
 */
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/api/v1/packs")
@Suppress("unused")
class ServerPackController @Autowired constructor(private val serverPackService: ServerPackService) {
    /**
     * GET request for downloading a server pack by the id in the database.
     *
     * @param id The id of the server pack in the database.
     * @return Gives the requester the requested file as a download, if it was found.
     * @author Griefed
     */
    @GetMapping(value = ["/download/{id}"], produces = ["application/zip"])
    fun downloadServerPack(@PathVariable id: Int): ResponseEntity<Resource?> {
        return serverPackService.downloadServerPackById(id)
    }

    /**
     * GET request for retrieving a list of all available server packs.
     *
     * @return A list of all available server packs on this instance.
     * @author Griefed
     */
    @get:GetMapping("all")
    val allServerPacks: ResponseEntity<List<ServerPackModel>>
        get() = ResponseEntity.ok().header("Content-Type", "application/json").body(
            serverPackService.getServerPacks()
        )


    /**
     * GET request for voting whether a server pack works or not.
     *
     * @param voting The vote, consisting of the id of the server pack and whether the vote should be
     * incremented or decremented. Example `42,up` or `23,down`.
     * @return ResponseEntity OK/BadRequest/NotFound
     * @author Griefed
     */
    @GetMapping("vote/{voting}")
    fun voteForServerPack(@PathVariable("voting") voting: String): ResponseEntity<Any> {
        return serverPackService.voteForServerPack(voting)
    }
}