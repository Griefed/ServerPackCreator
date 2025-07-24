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

import de.griefed.serverpackcreator.app.web.serverpack.customizing.RunConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.util.MimeTypeUtils
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/api/v2/runconfigs")
class RunConfigurationController @Autowired constructor(
    private val runConfigurationService: RunConfigurationService
) {

    @GetMapping("/all", produces = ["application/json"])
    @ResponseBody
    fun getAllRUnConfigurations(): ResponseEntity<List<RunConfiguration>> {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE).body(
            runConfigurationService.loadAll()
        )
    }

    @GetMapping("/{id:[0-9a-zA-Z]+}", produces = ["application/json"])
    @ResponseBody
    fun getModpack(@PathVariable id: String): ResponseEntity<RunConfiguration> {
        return if (runConfigurationService.load(id).isPresent) {
            ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE).body(
                runConfigurationService.load(id).get()
            )
        } else {
            ResponseEntity.notFound().build()
        }
    }
}