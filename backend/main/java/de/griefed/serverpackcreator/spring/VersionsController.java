/* Copyright (C) 2022  Griefed
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
package de.griefed.serverpackcreator.spring;

import de.griefed.serverpackcreator.utilities.commonutilities.Utilities;
import de.griefed.serverpackcreator.versionmeta.VersionMeta;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * RestController for acquiring all available Minecraft, Forge, Fabric and Fabric Installer versions.
 * @author Griefed
 */
@RestController
@CrossOrigin(origins = {"*"})
@RequestMapping("/api/v1/versions")
public class VersionsController {

    private final VersionMeta VERSIONMETA;
    private final Utilities UTILITIES;

    /**
     * Constructor for DI.
     * @author Griefed
     * @param injectedVersionMeta Instance of {@link VersionMeta} for version acquisition.
     * @param injectedUtilities Instance of {@link Utilities}.
     */
    @Autowired
    public VersionsController(VersionMeta injectedVersionMeta, Utilities injectedUtilities) {
        this.VERSIONMETA = injectedVersionMeta;
        this.UTILITIES = injectedUtilities;
    }

    /**
     * Get a list of all available Minecraft versions.
     * @author Griefed
     * @return String List. Returns a list of all available Minecraft verions.
     */
    @GetMapping(value = "/minecraft")
    public ResponseEntity<String> getAvailableMinecraftVersions() {

        return ResponseEntity
                .ok()
                .header(
                "Content-Type",
                "application/json"
                ).body(
                        "{\"minecraft\":" +
                        UTILITIES.ListUtils().encapsulateListElements(
                                VERSIONMETA.minecraft().releaseVersionsDescending()
                        ) + "}"
                );
    }

    /**
     * Get a list of all available Forge versions for a specific Minecraft version.
     * @author Griefed
     * @param minecraftVersion String. The Minecraft version you want to get a list of Forge versions for.
     * @return String List. Returns a list of all available Forge versions for the specified Minecraft version.
     */
    @GetMapping(value = "/forge/{minecraftversion}")
    public ResponseEntity<String> getAvailableForgeVersions(@PathVariable("minecraftversion") String minecraftVersion) {

        if (VERSIONMETA.forge().availableForgeVersionsDescending(minecraftVersion).isPresent()) {

            return ResponseEntity
                    .ok()
                    .header(
                            "Content-Type",
                            "application/json"
                    ).body(
                            "{\"forge\":" +
                                    UTILITIES.ListUtils().encapsulateListElements(
                                            VERSIONMETA.forge().availableForgeVersionsDescending(minecraftVersion).get()
                                    ) +
                                    "}"
                    );

        } else {

            return ResponseEntity
                    .ok()
                    .header(
                            "Content-Type",
                            "application/json"
                    ).body(
                            "{\"forge\":[]}"
                    );
        }
    }

    /**
     * Get a list of all available Fabric versions.
     * @author Griefed
     * @return String List. Returns a list of all available Fabric versions.
     */
    @GetMapping(value= "/fabric")
    public ResponseEntity<String> getAvailableFabricVersions() {

        return ResponseEntity
                .ok()
                .header(
                        "Content-Type",
                        "application/json"
                ).body(
                        "{\"fabric\":" +
                                UTILITIES.ListUtils().encapsulateListElements(VERSIONMETA.fabric().loaderVersionsDescending()
                        ) +
                                "}"
                );
    }

    /**
     * Get the Latest Fabric Installer and Release Fabric installer versions as a JSON object.
     * @author Griefed
     * @return String, JSON. Returns the Latest Fabric Installer and Release Fabric Installer as a JSON object.
     */
    @GetMapping(value = "/fabric/installer", produces = "application/json")
    public ResponseEntity<String> getAvailableFabricInstallerVersions() {

        return ResponseEntity
                .ok()
                .header(
                        "Content-Type",
                        "application/json"
                ).body(
                        "{" +
                        "\"release\":\"" + VERSIONMETA.fabric().releaseInstallerVersion() + "\"," +
                        "\"latest\":\"" + VERSIONMETA.fabric().releaseInstallerVersion() + "\"" +
                        "}"
                );
    }
}
