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
package de.griefed.serverpackcreator.web.task

import de.griefed.serverpackcreator.web.data.QueueEvent
import de.griefed.serverpackcreator.web.modpack.ModpackStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.util.MimeTypeUtils
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/api/v2/events")
class EventController @Autowired constructor(private val eventService: EventService) {

    @GetMapping("/all", produces = ["application/json"])
    @ResponseBody
    fun getEvents(): ResponseEntity<List<QueueEvent>> {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)
            .body(eventService.loadAll())
    }

    @GetMapping("/modpack/{id:[0-9]+}", produces = ["application/json"])
    @ResponseBody
    fun getModPackEvents(@PathVariable id: Int): ResponseEntity<List<QueueEvent>> {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)
            .body(eventService.loadAllByModPackId(id))
    }

    @GetMapping("/serverpack/{id:[0-9]+}", produces = ["application/json"])
    @ResponseBody
    fun getServerPackEvents(@PathVariable id: Int): ResponseEntity<List<QueueEvent>> {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)
            .body(eventService.loadAllByServerPackId(id))
    }

    @GetMapping("/status/{status:[A-Z]+}", produces = ["application/json"])
    @ResponseBody
    fun getStatusEvents(@PathVariable status: ModpackStatus): ResponseEntity<List<QueueEvent>> {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)
            .body(eventService.loadAllByStatus(status))
    }
}