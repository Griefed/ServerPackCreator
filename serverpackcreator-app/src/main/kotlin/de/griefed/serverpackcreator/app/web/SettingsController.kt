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
package de.griefed.serverpackcreator.app.web

import com.fasterxml.jackson.annotation.JsonProperty
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

    /** The settings snapshot the SPA reads on load. */
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

    /**
     * Everything `/api/v2/settings` answers with, in one response body.
     * 
     * A flat snapshot rather than a live view: it is built per request from `ApiProperties`, so a setting changed in
     * the GUI is picked up by the next call without the web layer holding any state of its own.
     */
    @Suppress("unused")
    class Settings(
        /** The shipped clientside-mod list, which the SPA offers as the default exclusions. */
        val clientsideMods: List<String>,
        /** The shipped whitelist — entries the clientside list must never exclude. */
        val whitelistMods: List<String>,
        /** The loaders SPC can generate for, so the SPA need not hard-code them. */
        val supportedModloaders: List<String>,
        /** ServerPackCreator's own version, shown in the SPA's footer. */
        val version: String,
        /** Whether this is a locally built artefact (version `dev`) rather than a release. */
        val devBuild: Boolean,
        /** Modpack directories a server pack takes by default. */
        val directoriesToInclude: List<String>,
        /** Modpack directories left out by default. */
        val directoriesToExclude: List<String>,
        /** Files excluded from the ZIP archive specifically, which is a separate list from the pack contents. */
        val zipArchiveExclusions: List<String>,
        /** How exclusion entries are matched (start, end, contains, regex, either). */
        val exclusionFilter: ExclusionFilter,
        // The JsonProperty-annotations keep the "is"-prefix in the JSON: Jackson would otherwise
        // strip it from Boolean-getters, breaking the frontend which reads the prefixed names.
        /** Whether the ZIP-archive exclusions are applied at all. */
        @get:JsonProperty("isZipFileExclusionEnabled")
        val isZipFileExclusionEnabled: Boolean,
        /** Whether SPC scans jars and drops the clientside ones automatically. */
        @get:JsonProperty("isAutoExcludingModsEnabled")
        val isAutoExcludingModsEnabled: Boolean,
        /** Whether snapshots and pre-releases are offered as Minecraft versions. */
        @get:JsonProperty("isMinecraftPreReleasesAvailabilityEnabled")
        val isMinecraftPreReleasesAvailabilityEnabled: Boolean,
        /** The Aikar's-flags string offered as a one-click JVM argument set. */
        val aikarsFlags: String,
        /** The configured locale, so the SPA can match SPC's own language. */
        val language: String
    )
}