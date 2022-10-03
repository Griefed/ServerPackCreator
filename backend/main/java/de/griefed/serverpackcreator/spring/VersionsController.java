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

import de.griefed.serverpackcreator.utilities.common.Utilities;
import de.griefed.serverpackcreator.versionmeta.VersionMeta;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * RestController for acquiring all available Minecraft, Forge, Fabric and Fabric Installer
 * versions.
 *
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
   *
   * @param injectedVersionMeta Instance of {@link VersionMeta} for version acquisition.
   * @param injectedUtilities   Instance of {@link Utilities}.
   * @author Griefed
   */
  @Autowired
  public VersionsController(VersionMeta injectedVersionMeta,
                            Utilities injectedUtilities) {
    this.VERSIONMETA = injectedVersionMeta;
    this.UTILITIES = injectedUtilities;
  }

  /**
   * Get a list of all available Minecraft versions.
   *
   * @return Returns a list of all available Minecraft verions.
   * @author Griefed
   */
  @GetMapping(value = "/minecraft")
  public ResponseEntity<String> getAvailableMinecraftVersions() {

    return ResponseEntity.ok()
                         .header("Content-Type", "application/json")
                         .body(
                             "{\"minecraft\":"
                                 + UTILITIES.ListUtils()
                                            .encapsulateListElements(
                                                VERSIONMETA.minecraft().releaseVersionsDescending())
                                 + "}");
  }

  /**
   * Get a list of all available Forge versions for a specific Minecraft version.
   *
   * @param minecraftVersion The Minecraft version you want to get a list of Forge versions for.
   * @return Returns a list of all available Forge versions for the specified Minecraft version.
   * @author Griefed
   */
  @GetMapping(value = "/forge/{minecraftversion}")
  public ResponseEntity<String> getAvailableForgeVersions(
      @PathVariable("minecraftversion") String minecraftVersion) {

    if (VERSIONMETA.forge().availableForgeVersionsDescending(minecraftVersion).isPresent()) {

      return ResponseEntity.ok()
                           .header("Content-Type", "application/json")
                           .body(
                               "{\"forge\":"
                                   + UTILITIES.ListUtils()
                                              .encapsulateListElements(
                                                  VERSIONMETA
                                                      .forge()
                                                      .availableForgeVersionsDescending(
                                                          minecraftVersion)
                                                      .get())
                                   + "}");

    } else {

      return ResponseEntity.ok().header("Content-Type", "application/json").body("{\"forge\":[]}");
    }
  }

  /**
   * Get a list of all available Fabric versions.
   *
   * @return Returns a list of all available Fabric versions.
   * @author Griefed
   */
  @GetMapping(value = "/fabric")
  public ResponseEntity<String> getAvailableFabricVersions() {

    return ResponseEntity.ok()
                         .header("Content-Type", "application/json")
                         .body(
                             "{\"fabric\":"
                                 + UTILITIES.ListUtils()
                                            .encapsulateListElements(
                                                VERSIONMETA.fabric().loaderVersionsListDescending())
                                 + "}");
  }

  /**
   * Get the Latest Fabric Installer and Release Fabric installer versions as a JSON object.
   *
   * @return String, JSON. Returns the Latest Fabric Installer and Release Fabric Installer as a
   * JSON object.
   * @author Griefed
   */
  @GetMapping(value = "/fabric/installer", produces = "application/json")
  public ResponseEntity<String> getAvailableFabricInstallerVersions() {

    return ResponseEntity.ok()
                         .header("Content-Type", "application/json")
                         .body(
                             "{"
                                 + "\"release\":\""
                                 + VERSIONMETA.fabric().releaseInstaller()
                                 + "\","
                                 + "\"latest\":\""
                                 + VERSIONMETA.fabric().releaseInstaller()
                                 + "\""
                                 + "}");
  }

  /**
   * Get a list of all available Fabric versions.
   *
   * @return Returns a list of all available Fabric versions.
   * @author Griefed
   */
  @GetMapping(value = "/quilt")
  public ResponseEntity<String> getAvailableQuiltVersions() {

    return ResponseEntity.ok()
                         .header("Content-Type", "application/json")
                         .body(
                             "{\"quilt\":"
                                 + UTILITIES.ListUtils()
                                            .encapsulateListElements(
                                                VERSIONMETA.quilt().loaderVersionsListDescending())
                                 + "}");
  }

  /**
   * Get the Latest Fabric Installer and Release Fabric installer versions as a JSON object.
   *
   * @return String, JSON. Returns the Latest Fabric Installer and Release Fabric Installer as a
   * JSON object.
   * @author Griefed
   */
  @GetMapping(value = "/quilt/installer", produces = "application/json")
  public ResponseEntity<String> getAvailableQuiltInstallerVersions() {

    return ResponseEntity.ok()
                         .header("Content-Type", "application/json")
                         .body(
                             "{"
                                 + "\"release\":\""
                                 + VERSIONMETA.quilt().releaseInstaller()
                                 + "\","
                                 + "\"latest\":\""
                                 + VERSIONMETA.quilt().releaseInstaller()
                                 + "\""
                                 + "}");
  }
}
