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

import de.griefed.serverpackcreator.ApplicationProperties;
import de.griefed.serverpackcreator.ConfigurationHandler;
import de.griefed.serverpackcreator.spring.NotificationResponse;
import de.griefed.serverpackcreator.spring.task.TaskSubmitter;
import de.griefed.serverpackcreator.utilities.ConfigUtilities;
import de.griefed.serverpackcreator.versionmeta.VersionMeta;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service class for backend tasks related to storing ZIP-archives uploaded through
 * {@link ZipController}.
 *
 * @author Griefed
 */
@Service
public class ZipService {

  private static final Logger LOG = LogManager.getLogger(ZipService.class);

  private final ApplicationProperties APPLICATIONPROPERTIES;
  private final TaskSubmitter TASKSUBMITTER;
  private final ConfigurationHandler CONFIGURATIONHANDLER;
  private final NotificationResponse NOTIFICATIONRESPONSE;
  private final VersionMeta VERSIONMETA;
  private final ConfigUtilities CONFIGUTILITIES;

  /**
   * Constructor responsible for DI.
   *
   * @param injectedApplicationProperties Instance of {@link ApplicationProperties}.
   * @param injectedTaskSubmitter         Instance of {@link TaskSubmitter}.
   * @param injectedConfigurationHandler  Instance of {@link ConfigurationHandler}.
   * @param injectedNotificationResponse  Instance of {@link NotificationResponse}.
   * @param injectedVersionMeta           Instance of {@link VersionMeta}.
   * @param injectedConfigUtilities       Instance of {@link ConfigUtilities}.
   * @author Griefed
   */
  @Autowired
  public ZipService(
      ApplicationProperties injectedApplicationProperties,
      TaskSubmitter injectedTaskSubmitter,
      ConfigurationHandler injectedConfigurationHandler,
      NotificationResponse injectedNotificationResponse,
      VersionMeta injectedVersionMeta,
      ConfigUtilities injectedConfigUtilities) {

    APPLICATIONPROPERTIES = injectedApplicationProperties;
    TASKSUBMITTER = injectedTaskSubmitter;
    CONFIGURATIONHANDLER = injectedConfigurationHandler;
    NOTIFICATIONRESPONSE = injectedNotificationResponse;
    VERSIONMETA = injectedVersionMeta;
    CONFIGUTILITIES = injectedConfigUtilities;
  }

  /**
   * Store an uploaded ZIP-archive to disk.
   *
   * @param uploadedFile The file which was uploaded which you want to store on disk.
   * @return The path to the saved file.
   * @throws IOException If an I/O error occurs writing to or creating the file.
   * @author Griefed
   */
  protected Path saveUploadedFile(final MultipartFile uploadedFile) throws IOException {

    Path zipPath = Paths.get(
        APPLICATIONPROPERTIES.modpacksDirectory() + File.separator
            + uploadedFile.getOriginalFilename());

    // Does a archive with the same name already exist?
    if (zipPath.toFile().isFile()) {

      int incrementation = 0;

      String substring = zipPath.toString().substring(0, zipPath.toString().length() - 4);

      while (new File(substring + "_" + incrementation + ".zip").isFile()) {
        incrementation++;
      }

      zipPath = Paths.get(substring + "_" + incrementation + ".zip");
    }

    uploadedFile.transferTo(zipPath);

    return zipPath;
  }

  /**
   * Submit a task for the generation of a server pack from a ZIP-archive.
   *
   * @param zipGenerationProperties String containing all information required to generate a server
   *                                pack from a ZIP-archive. See
   *                                {@link ZipController#requestGenerationFromZip(String, String,
   *                                String, String, String)}.
   * @return {@code true} if the task was submitted.
   * @author Griefed
   */
  protected String submitGenerationTask(String zipGenerationProperties) {

    String[] parameters = zipGenerationProperties.split("&");

    // Check if the requested ZIP-archive exists.
    if (!parameters[0].substring(parameters[0].length() - 4).equalsIgnoreCase(".zip")
        || !new File(APPLICATIONPROPERTIES.modpacksDirectory(), parameters[0]).isFile()) {

      LOG.info("ZIP-archive " + APPLICATIONPROPERTIES.modpacksDirectory() + "/" + parameters[0]
          + " not found.");

      return NOTIFICATIONRESPONSE.zipResponse(
          "ZIP-archive not found.", 5000, "error", "negative", parameters[0], false);
    }

    // Check the Minecraft version
    if (!VERSIONMETA.minecraft().checkMinecraftVersion(parameters[2])) {
      LOG.info("Minecraft version " + parameters[2] + " incorrect.");

      return NOTIFICATIONRESPONSE.zipResponse(
          "Incorrect Minecraft version: " + parameters[2], 5000, "error", "negative", null, false);
    }

    // Check the modloader version
    if (CONFIGURATIONHANDLER.checkModloader(parameters[3])) {

      // Check Forge
      if (CONFIGUTILITIES.getModLoaderCase(parameters[3]).equals("Forge")) {

        if (!VERSIONMETA.forge().checkForgeAndMinecraftVersion(parameters[2], parameters[4])) {
          LOG.info(
              parameters[3] + " version " + parameters[2] + "-" + parameters[4] + " incorrect.");
          return NOTIFICATIONRESPONSE.zipResponse(
              "Incorrect Forge version: " + parameters[2], 5000, "error", "negative", null, false);
        }

        // Check Fabric
      } else {

        if (!VERSIONMETA.fabric().isVersionValid(parameters[4])) {
          LOG.info(parameters[3] + " version " + parameters[4] + " incorrect.");
          return NOTIFICATIONRESPONSE.zipResponse(
              "Incorrect Fabric version: " + parameters[4], 5000, "error", "negative", null, false);
        }
      }

    } else {
      LOG.info("Modloader " + parameters[3] + " incorrect.");
      return NOTIFICATIONRESPONSE.zipResponse(
          "Modloader incorrect: " + parameters[3], 5000, "error", "negative", null, false);
    }

    TASKSUBMITTER.generateZip(zipGenerationProperties);

    return NOTIFICATIONRESPONSE.zipResponse(
        "Request submitted. Check the downloads section later on. Keep a look out for :"
            + parameters[0]
            + " in the File Disk Name column.",
        7000,
        "info",
        "positive",
        parameters[0],
        true);
  }
}
