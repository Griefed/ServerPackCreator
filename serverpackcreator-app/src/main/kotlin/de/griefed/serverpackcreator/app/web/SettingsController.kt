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
package de.griefed.serverpackcreator.app.web

import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.config.ExclusionFilter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.util.MimeTypeUtils
import org.springframework.web.bind.annotation.*

/**
 * RestController for acquiring the configuration of this ServerPackCreator instance.
 *
 * @author Griefed
 */
@Suppress("unused")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/api/v2/settings")
class SettingsController @Autowired constructor(
    private val apiProperties: ApiProperties
) {

    @GetMapping("/current", produces = ["application/json"])
    @ResponseBody
    fun getProperties(): ResponseEntity<Settings> {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)
            .body(Settings(
                clientsideMods = apiProperties.clientSideMods(),
                whitelistMods = apiProperties.whitelistedMods(),
                supportedModloaders = apiProperties.supportedModloaders.toList(),
                version = apiProperties.apiVersion,
                devBuild = apiProperties.devBuild,
                directoriesToInclude = apiProperties.directoriesToInclude.toList(),
                directoriesToExclude = apiProperties.directoriesToExclude.toList(),
                zipArchiveExclusions = apiProperties.zipArchiveExclusions.toList(),
                exclusionFilter = apiProperties.exclusionFilter,
                isZipFileExclusionEnabled = apiProperties.isZipFileExclusionEnabled,
                isAutoExcludingModsEnabled = apiProperties.isAutoExcludingModsEnabled,
                isMinecraftPreReleasesAvailabilityEnabled = apiProperties.isMinecraftPreReleasesAvailabilityEnabled,
                aikarsFlags = apiProperties.aikarsFlags,
                language = apiProperties.language.toString()
            ))
    }

    @Suppress("unused")
    inner class Settings(
        val clientsideMods: List<String>,
        val whitelistMods: List<String>,
        val supportedModloaders: List<String>,
        val version: String,
        val devBuild: Boolean,
        val directoriesToInclude: List<String>,
        val directoriesToExclude: List<String>,
        val zipArchiveExclusions: List<String>,
        val exclusionFilter: ExclusionFilter,
        val isZipFileExclusionEnabled: Boolean,
        val isAutoExcludingModsEnabled: Boolean,
        val isMinecraftPreReleasesAvailabilityEnabled: Boolean,
        val aikarsFlags: String,
        val language: String
    )
}