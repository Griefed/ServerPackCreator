/* Copyright (C) 2025 Griefed
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
package de.griefed.serverpackcreator.app.web.versions

import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.util.MimeTypeUtils
import org.springframework.web.bind.annotation.*

/**
 * RestController for acquiring all available Minecraft, Forge, Fabric and Fabric Installer
 * versions.
 *
 * @author Griefed
 */
@Suppress("unused")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/api/v2/versions")
class VersionsController @Autowired constructor(private val versionMeta: VersionMeta) {

    /**
     * Get a list of all modloader versions that are not forge.
     *
     * @return Returns a list of all available Fabric versions.
     * @author Griefed
     */
    @GetMapping("/all", produces = ["application/json"])
    fun availableVersions(): ResponseEntity<VersionMetaResponse> {
        val versionMetaResponse = VersionMetaResponse(
            minecraft = versionMeta.minecraft.clientReleases().map { it.version },
            fabric = versionMeta.fabric.loaderVersions().reversed(),
            legacyFabric = versionMeta.legacyFabric.loaderVersions(),
            quilt = versionMeta.quilt.loaderVersions().reversed(),
            forge = versionMeta.forge.getForgeMeta(),
            neoForge = versionMeta.neoForge.getNeoForgeMeta()
        )
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)
            .body(versionMetaResponse)
    }

    /**
     * Get a list of all available Minecraft versions.
     *
     * @return Returns a list of all available Minecraft verions.
     * @author Griefed
     */
    @GetMapping("/minecraft", produces = ["application/json"])
    fun availableMinecraftVersions(): ResponseEntity<List<String>> {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)
            .body(versionMeta.minecraft.clientReleases().map { it.version })
    }

    /**
     * Get a list of all available Forge versions for a specific Minecraft version.
     *
     * @param minecraftVersion The Minecraft version you want to get a list of Forge versions for.
     * @return Returns a list of all available Forge versions for the specified Minecraft version.
     * @author Griefed
     */
    @GetMapping("/forge/{minecraftversion}", produces = ["application/json"])
    fun availableForgeVersionsForMinecraftVersion(@PathVariable("minecraftversion") minecraftVersion: String): ResponseEntity<List<String>> {
        val versions = versionMeta.forge.supportedForgeVersions(minecraftVersion)
        return if (versions.isPresent) {
            ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)
                .body(versions.get())
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Get a list of all available NeoForge versions.
     *
     * @return Returns a list of all available Fabric versions.
     * @author Griefed
     */
    @GetMapping("/forge", produces = ["application/json"])
    fun availableForgeVersions(): ResponseEntity<HashMap<String, List<String>>> {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)
            .body(versionMeta.forge.getForgeMeta())
    }

    /**
     * Get a list of all available NeoForge versions.
     *
     * @return Returns a list of all available Fabric versions.
     * @author Griefed
     */
    @GetMapping("/neoforge", produces = ["application/json"])
    fun availableNeoForgeVersions(): ResponseEntity<HashMap<String,List<String>>> {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)
            .body(versionMeta.neoForge.getNeoForgeMeta())
    }

    /**
     * Get a list of all available NeoForge versions.
     *
     * @return Returns a list of all available Fabric versions.
     * @author Griefed
     */
    @GetMapping("/neoforge/{minecraftversion}", produces = ["application/json"])
    fun availableNeoForgeVersionsForMinecraftVersion(@PathVariable("minecraftversion") minecraftVersion: String): ResponseEntity<List<String>> {
        val versions = versionMeta.neoForge.supportedNeoForgeVersions(minecraftVersion)
        return if (versions.isPresent) {
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)
                .body(versions.get())
        } else {
            ResponseEntity.notFound().build()
        }

    }

    /**
     * Get a list of all available Fabric versions.
     *
     * @return Returns a list of all available Fabric versions.
     * @author Griefed
     */
    @GetMapping("/fabric", produces = ["application/json"])
    fun availableFabricVersions(): ResponseEntity<List<String>> {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)
            .body(versionMeta.fabric.loaderVersions().reversed())
    }

    /**
     * Get a list of all available Fabric versions.
     *
     * @return Returns a list of all available Fabric versions.
     * @author Griefed
     */
    @GetMapping("/legacyfabric", produces = ["application/json"])
    fun availableLegacyFabricVersions(): ResponseEntity<List<String>> {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)
            .body(versionMeta.legacyFabric.loaderVersions())
    }

    /**
     * Get a list of all available Fabric versions.
     *
     * @return Returns a list of all available Fabric versions.
     * @author Griefed
     */
    @GetMapping("/quilt", produces = ["application/json"])
    fun availableQuiltVersions(): ResponseEntity<List<String>> {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)
            .body(versionMeta.quilt.loaderVersions().reversed())
    }
}