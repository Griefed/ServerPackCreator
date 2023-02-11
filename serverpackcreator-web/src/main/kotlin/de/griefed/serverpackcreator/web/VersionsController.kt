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
package de.griefed.serverpackcreator.web

import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
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
@RequestMapping("/api/v1/versions")
class VersionsController @Autowired constructor(
    private val versionMeta: VersionMeta,
    private val utilities: Utilities
) {

    /**
     * Get a list of all available Minecraft versions.
     *
     * @return Returns a list of all available Minecraft verions.
     * @author Griefed
     */
    @get:GetMapping(value = ["/minecraft"])
    val availableMinecraftVersions: ResponseEntity<String>
        get() = ResponseEntity.ok()
            .header("Content-Type", "application/json")
            .body(
                ("{\"minecraft\":"
                        + utilities.listUtilities
                    .encapsulateListElements(
                        versionMeta.minecraft.releaseVersionsDescending()
                    )
                        ) + "}"
            )

    /**
     * Get a list of all available Forge versions for a specific Minecraft version.
     *
     * @param minecraftVersion The Minecraft version you want to get a list of Forge versions for.
     * @return Returns a list of all available Forge versions for the specified Minecraft version.
     * @author Griefed
     */
    @GetMapping(value = ["/forge/{minecraftversion}"])
    fun getAvailableForgeVersions(
        @PathVariable("minecraftversion") minecraftVersion: String?
    ): ResponseEntity<String> {
        return if (versionMeta.forge.supportedForgeVersionsAscending(minecraftVersion!!).isPresent) {
            ResponseEntity.ok()
                .header("Content-Type", "application/json")
                .body(
                    ("{\"forge\":"
                            + utilities.listUtilities
                        .encapsulateListElements(
                            versionMeta
                                .forge
                                .supportedForgeVersionsAscending(minecraftVersion)
                                .get()
                        )
                            ) + "}"
                )
        } else {
            ResponseEntity.ok().header("Content-Type", "application/json").body("{\"forge\":[]}")
        }
    }

    /**
     * Get a list of all available Fabric versions.
     *
     * @return Returns a list of all available Fabric versions.
     * @author Griefed
     */
    @get:GetMapping(value = ["/fabric"])
    val availableFabricVersions: ResponseEntity<String>
        get() = ResponseEntity.ok()
            .header("Content-Type", "application/json")
            .body(
                ("{\"fabric\":"
                        + utilities.listUtilities
                    .encapsulateListElements(
                        versionMeta.fabric.loaderVersionsListDescending()
                    )
                        ) + "}"
            )

    /**
     * Get the Latest Fabric Installer and Release Fabric installer versions as a JSON object.
     *
     * @return Returns the Latest Fabric Installer and Release Fabric Installer as a JSON object.
     * @author Griefed
     */
    @get:GetMapping(value = ["/fabric/installer"], produces = ["application/json"])
    val availableFabricInstallerVersions: ResponseEntity<String>
        get() = ResponseEntity.ok()
            .header("Content-Type", "application/json")
            .body(
                (("{"
                        + "\"release\":\""
                        + versionMeta.fabric.releaseInstaller()
                        ) + "\","
                        + "\"latest\":\""
                        + versionMeta.fabric.releaseInstaller()
                        ) + "\""
                        + "}"
            )

    /**
     * Get a list of all available Fabric versions.
     *
     * @return Returns a list of all available Fabric versions.
     * @author Griefed
     */
    @get:GetMapping(value = ["/legacyfabric"])
    val availableLegacyFabricVersions: ResponseEntity<String>
        get() = ResponseEntity.ok()
            .header("Content-Type", "application/json")
            .body(
                ("{\"fabric\":"
                        + utilities.listUtilities
                    .encapsulateListElements(
                        versionMeta.legacyFabric.loaderVersionsListDescending()
                    )
                        ) + "}"
            )

    /**
     * Get the Latest Fabric Installer and Release Fabric installer versions as a JSON object.
     *
     * @return Returns the Latest Fabric Installer and Release Fabric Installer as a JSON object.
     * @author Griefed
     */
    @get:GetMapping(value = ["/legacyfabric/installer"], produces = ["application/json"])
    val availableLegacyFabricInstallerVersions: ResponseEntity<String>
        get() = ResponseEntity.ok()
            .header("Content-Type", "application/json")
            .body(
                (("{"
                        + "\"release\":\""
                        + versionMeta.legacyFabric.releaseInstaller()
                        ) + "\","
                        + "\"latest\":\""
                        + versionMeta.legacyFabric.releaseInstaller()
                        ) + "\""
                        + "}"
            )

    /**
     * Get a list of all available Fabric versions.
     *
     * @return Returns a list of all available Fabric versions.
     * @author Griefed
     */
    @get:GetMapping(value = ["/quilt"])
    val availableQuiltVersions: ResponseEntity<String>
        get() = ResponseEntity.ok()
            .header("Content-Type", "application/json")
            .body(
                ("{\"quilt\":"
                        + utilities.listUtilities
                    .encapsulateListElements(
                        versionMeta.quilt.loaderVersionsListDescending()
                    )
                        ) + "}"
            )

    /**
     * Get the Latest Fabric Installer and Release Fabric installer versions as a JSON object.
     *
     * @return Returns the Latest Fabric Installer and Release Fabric Installer as a JSON object.
     * @author Griefed
     */
    @get:GetMapping(value = ["/quilt/installer"], produces = ["application/json"])
    val availableQuiltInstallerVersions: ResponseEntity<String>
        get() = ResponseEntity.ok()
            .header("Content-Type", "application/json")
            .body(
                (("{"
                        + "\"release\":\""
                        + versionMeta.quilt.releaseInstaller()
                        ) + "\","
                        + "\"latest\":\""
                        + versionMeta.quilt.releaseInstaller()
                        ) + "\""
                        + "}"
            )
}