/* Copyright (C) 2026 Griefed
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

/**
 * Read-only access to stored run configurations.
 * 
 * **The response body is this entity, on a versioned path**, so its field shape is part of the published API —
 * changing it means changing the SPA's types and the published description in the same commit.
 */
@Suppress("unused")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/api/v2/runconfigs")
class RunConfigurationController @Autowired constructor(
    private val runConfigurationService: RunConfigurationService
) {

    /** Every stored run configuration. (The typo in the name is part of the shipped API surface.) */
    @GetMapping("/all", produces = ["application/json"])
    @ResponseBody
    fun getAllRUnConfigurations(): ResponseEntity<List<RunConfiguration>> {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE).body(
            runConfigurationService.loadAll()
        )
    }

    /** One run configuration by id. Named for the SPA's call site rather than for what it returns. */
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