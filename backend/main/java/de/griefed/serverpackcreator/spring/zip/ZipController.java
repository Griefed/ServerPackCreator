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
package de.griefed.serverpackcreator.spring.zip;

import com.google.common.net.HttpHeaders;
import de.griefed.serverpackcreator.ApplicationProperties;
import de.griefed.serverpackcreator.ConfigurationHandler;
import de.griefed.serverpackcreator.spring.NotificationResponse;
import de.griefed.serverpackcreator.utilities.common.Utilities;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * RestController responsible for handling ZIP-archive uploads and server pack generation from the
 * very same.
 *
 * @author Griefed
 */
@RestController
@CrossOrigin(origins = {"*"})
@RequestMapping("/api/v1/zip")
public class ZipController {

  private final ZipService ZIPSERVICE;
  private final ConfigurationHandler CONFIGURATIONHANDLER;
  private final NotificationResponse NOTIFICATIONRESPONSE;
  private final ApplicationProperties APPLICATIONPROPERTIES;
  private final Utilities UTILITIES;

  /**
   * Constructor responsible for DI.
   *
   * @param injectedZipService            Instance of {@link ZipService}.
   * @param injectedConfigurationHandler  Instance of {@link ConfigurationHandler}.
   * @param injectedNotificationResponse  Instance of {@link NotificationResponse}.
   * @param injectedApplicationProperties Instance of {@link ApplicationProperties}.
   * @param injectedUtilities             Instance of {@link Utilities}.
   * @author Griefed
   */
  @Autowired
  public ZipController(
      ZipService injectedZipService,
      ConfigurationHandler injectedConfigurationHandler,
      NotificationResponse injectedNotificationResponse,
      ApplicationProperties injectedApplicationProperties,
      Utilities injectedUtilities) {

    this.ZIPSERVICE = injectedZipService;
    this.CONFIGURATIONHANDLER = injectedConfigurationHandler;
    this.NOTIFICATIONRESPONSE = injectedNotificationResponse;
    this.APPLICATIONPROPERTIES = injectedApplicationProperties;
    this.UTILITIES = injectedUtilities;
  }

  /**
   * Upload a file and check whether it is a ServerPackCreator valid ZIP-archive.
   *
   * @param file The file uploaded to ServerPackCreator.
   * @return A list on encountered errors, if any.
   * @throws IOException if an errors occurred saving or reading the file.
   * @author Griefed
   */
  @PostMapping("/upload")
  public ResponseEntity<String> handleFileUpload(@RequestParam("file") final MultipartFile file)
      throws IOException {
    List<String> encounteredErrors = new ArrayList<>();

    Path pathToZip = ZIPSERVICE.saveUploadedFile(file);

    if (CONFIGURATIONHANDLER.checkZipArchive(
        Paths.get(pathToZip.toString().replace("\\", "/")), encounteredErrors)) {

      FileUtils.deleteQuietly(new File(pathToZip.toString()));

      return ResponseEntity.badRequest()
          .header(HttpHeaders.CONTENT_TYPE, "application/json")
          .body(
              NOTIFICATIONRESPONSE.zipResponse(
                  encounteredErrors,
                  10000,
                  "error",
                  "negative",
                  file.getOriginalFilename(),
                  false));
    }

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_TYPE, "application/json")
        .body(
            NOTIFICATIONRESPONSE.zipResponse(
                "ZIP-file checks passed. You may press Submit. :)",
                5000,
                "info",
                "positive",
                pathToZip.toFile().getName(),
                true));
  }

  /**
   * Request the generation of a server pack from a previously uploaded ZIP-archive, which passed
   * validation checks, and from a barebones configuration, including:<br>
   * <code>clientMods</code><br>
   * <code>minecraftVersion</code><br>
   * <code>modLoader</code><br>
   * <code>modLoaderVersion</code><br>
   *
   * @param zipName          The name of the previously uploaded ZIP-archive.
   * @param clientMods       A comma separated list of clientside-only mods to exclude from the
   *                         server pack.
   * @param minecraftVersion The Minecraft version the modpack, and therefor the server pack, uses.
   * @param modLoader        The modloader the modpack, and therefor the server pack, uses.
   * @param modLoaderVersion The modloader version the modpack, and therefor the server pack, uses.
   * @return Notification message with information about the result.
   * @author Griefed
   */
  @GetMapping("/{zipName}&{clientMods}&{minecraftVersion}&{modLoader}&{modLoaderVersion}")
  public ResponseEntity<String> requestGenerationFromZip(
      @PathVariable("zipName") String zipName,
      @PathVariable("clientMods") String clientMods,
      @PathVariable("minecraftVersion") String minecraftVersion,
      @PathVariable("modLoader") String modLoader,
      @PathVariable("modLoaderVersion") String modLoaderVersion) {

    if (clientMods.length() == 0) {
      clientMods = UTILITIES.StringUtils().buildString(APPLICATIONPROPERTIES.getListFallbackMods());
    }

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_TYPE, "application/json")
        .body(
            ZIPSERVICE.submitGenerationTask(
                zipName
                    + "&"
                    + clientMods
                    + "&"
                    + minecraftVersion
                    + "&"
                    + modLoader
                    + "&"
                    + modLoaderVersion));
  }
}
