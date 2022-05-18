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

import de.griefed.serverpackcreator.ConfigurationHandler;
import de.griefed.serverpackcreator.spring.NotificationResponse;
import de.griefed.serverpackcreator.spring.task.TaskSubmitter;
import de.griefed.serverpackcreator.utilities.ConfigUtilities;
import de.griefed.serverpackcreator.utilities.commonutilities.Utilities;
import de.griefed.serverpackcreator.versionmeta.VersionMeta;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Service class for backend tasks related to storing ZIP-archives uploaded through {@link ZipController}.
 * @author Griefed
 */
@Service
public class ZipService {

    private static final Logger LOG = LogManager.getLogger(ZipService.class);

    private final TaskSubmitter TASKSUBMITTER;
    private final ConfigurationHandler CONFIGURATIONHANDLER;
    private final Utilities UTILITIES;
    private final NotificationResponse NOTIFICATIONRESPONSE;
    private final VersionMeta VERSIONMETA;
    private final ConfigUtilities CONFIGUTILITIES;

    /**
     * Constructor responsible for DI.
     * @author Griefed
     * @param injectedTaskSubmitter Instance of {@link TaskSubmitter}.
     * @param injectedConfigurationHandler Instance of {@link ConfigurationHandler}.
     * @param injectedUtilities Instance of {@link Utilities}.
     * @param injectedNotificationResponse Instance of {@link NotificationResponse}.
     * @param injectedVersionMeta Instance of {@link VersionMeta}.
     * @param injectedConfigUtilities Instance of {@link ConfigUtilities}.
     */
    @Autowired
    public ZipService(TaskSubmitter injectedTaskSubmitter, ConfigurationHandler injectedConfigurationHandler,
                      Utilities injectedUtilities, NotificationResponse injectedNotificationResponse,
                      VersionMeta injectedVersionMeta, ConfigUtilities injectedConfigUtilities) {

        this.TASKSUBMITTER = injectedTaskSubmitter;
        this.CONFIGURATIONHANDLER = injectedConfigurationHandler;
        this.UTILITIES = injectedUtilities;
        this.NOTIFICATIONRESPONSE = injectedNotificationResponse;
        this.VERSIONMETA = injectedVersionMeta;
        this.CONFIGUTILITIES = injectedConfigUtilities;
    }

    /**
     * Store an uploaded ZIP-archive to disk.
     * @author Griefed
     * @param uploadedFile {@link File} The file which was uploaded which you want to store on disk.
     * @return {@link Path} The path to the saved file.
     * @throws IOException If an I/O error occurs writing to or creating the file.
     */
    protected Path saveUploadedFile(final MultipartFile uploadedFile) throws IOException {
        final byte[] fileBytes = uploadedFile.getBytes();

        Path zipPath = Paths.get("work/modpacks/" + uploadedFile.getOriginalFilename());

        // Does a archive with the same name already exist?
        if (zipPath.toFile().isFile()) {

            int incrementation = 0;

            String substring = zipPath.toString()
                    .replace("\\", "/")
                    .substring(
                            0,
                            zipPath.toString()
                                    .replace("\\", "/")
                                    .length() - 4
                    );

            while (new File(substring + "_" + incrementation + ".zip").isFile()) {
                incrementation++;
            }

            zipPath = Paths.get(substring + "_" + incrementation + ".zip");

        }

        Files.write(zipPath,fileBytes);

        return zipPath;
    }

    /**
     * Submit a task for the generation of a server pack from a ZIP-archive.
     * @author Griefed
     * @param zipGenerationProperties {@link String} containing all information required to generate a server pack from
     * a ZIP-archive. See {@link ZipController#requestGenerationFromZip(String, String, String, String, String)}.
     * @return {@link Boolean} Returns true if the task was submitted.
     */
    protected String submitGenerationTask(String zipGenerationProperties) {

        String[] parameters = zipGenerationProperties.split("&");

        // Check if the requested ZIP-archive exists.
        if (!parameters[0].substring(parameters[0].length() - 4).equalsIgnoreCase(".zip") ||
                !new File("work/modpacks/" + parameters[0]).isFile()) {

            LOG.info("ZIP-archive work/modpacks/"+ parameters[0] + " not found.");

            return NOTIFICATIONRESPONSE.zipResponse(
                    "ZIP-archive not found.",
                    5000,
                    "error",
                    "negative",
                    parameters[0],
                    false
            );
        }

        // Check the Minecraft version
        if (!VERSIONMETA.minecraft().checkMinecraftVersion(parameters[2])) {
            LOG.info("Minecraft version " + parameters[2] + " incorrect.");

            return NOTIFICATIONRESPONSE.zipResponse(
                    "Incorrect Minecraft version: " + parameters[2],
                    5000,
                    "error",
                    "negative",
                    null,
                    false
            );
        }

        // Check the modloader version
        if (CONFIGURATIONHANDLER.checkModloader(parameters[3])) {

            // Check Forge
            if (CONFIGUTILITIES.getModLoaderCase(parameters[3]).equals("Forge")) {

                if (!VERSIONMETA.forge().checkForgeAndMinecraftVersion(parameters[2],parameters[4])) {
                    LOG.info(parameters[3] + " version " + parameters[2] + "-" + parameters[4] + " incorrect.");
                    return NOTIFICATIONRESPONSE.zipResponse(
                            "Incorrect Forge version: " + parameters[2],
                            5000,
                            "error",
                            "negative",
                            null,
                            false
                    );
                }

            // Check Fabric
            } else {

                if (!VERSIONMETA.fabric().checkFabricVersion(parameters[4])) {
                    LOG.info(parameters[3] + " version " + parameters[4] + " incorrect.");
                    return NOTIFICATIONRESPONSE.zipResponse(
                            "Incorrect Fabric version: " + parameters[4],
                            5000,
                            "error",
                            "negative",
                            null,
                            false
                    );
                }
            }

        } else {
            LOG.info("Modloader " + parameters[3] + " incorrect.");
            return NOTIFICATIONRESPONSE.zipResponse(
                    "Modloader incorrect: " + parameters[3],
                    5000,
                    "error",
                    "negative",
                    null,
                    false
            );
        }

        TASKSUBMITTER.generateZip(zipGenerationProperties);

        return NOTIFICATIONRESPONSE.zipResponse(
                "Request submitted. Check the downloads section later on. Keep a look out for :" + parameters[0] + " in the File Disk Name column.",
                7000,
                "info",
                "positive",
                parameters[0],
                true
        );
    }
}
