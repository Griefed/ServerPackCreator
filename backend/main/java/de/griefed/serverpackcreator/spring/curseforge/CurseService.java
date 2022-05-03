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
package de.griefed.serverpackcreator.spring.curseforge;

import com.therandomlabs.curseapi.CurseException;
import de.griefed.serverpackcreator.ApplicationProperties;
import de.griefed.serverpackcreator.ConfigurationHandler;
import de.griefed.serverpackcreator.curseforge.InvalidFileException;
import de.griefed.serverpackcreator.curseforge.InvalidModpackException;
import de.griefed.serverpackcreator.spring.NotificationResponse;
import de.griefed.serverpackcreator.spring.serverpack.ServerPackModel;
import de.griefed.serverpackcreator.spring.serverpack.ServerPackService;
import de.griefed.serverpackcreator.spring.task.TaskSubmitter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * CurseForge service for working with, retrieving, sending, generating, handling everything related to server packs/modpacks
 * which make use of CurseForge or are made using CurseForge and their API.
 * @author Griefed
 */
@Service
public class CurseService {

    private static final Logger LOG = LogManager.getLogger(CurseService.class);

    private final ConfigurationHandler CONFIGURATIONHANDLER;
    private final NotificationResponse CURSERESPONSEMODEL;
    private final ApplicationProperties APPLICATIONPROPERTIES;
    private final ServerPackService SERVERPACKSERVICE;
    private final TaskSubmitter TASKSUBMITTER;

    /**
     * Constructor responsible for our DI.
     * @author Griefed
     * @param injectedConfigurationHandler Instance of {@link ConfigurationHandler}.
     * @param injectedNotificationResponse Instance of {@link NotificationResponse}.
     * @param injectedApplicationProperties Instance of {@link ApplicationProperties}.
     * @param injectedServerPackService Instance of {@link ServerPackService}.
     * @param injectedTaskSubmitter Instance of {@link TaskSubmitter}.
     */
    @Autowired
    public CurseService(ConfigurationHandler injectedConfigurationHandler, NotificationResponse injectedNotificationResponse, ApplicationProperties injectedApplicationProperties, ServerPackService injectedServerPackService, TaskSubmitter injectedTaskSubmitter) {
        this.CONFIGURATIONHANDLER = injectedConfigurationHandler;
        this.CURSERESPONSEMODEL = injectedNotificationResponse;
        this.APPLICATIONPROPERTIES = injectedApplicationProperties;
        this.SERVERPACKSERVICE = injectedServerPackService;
        this.TASKSUBMITTER = injectedTaskSubmitter;
    }

    /**
     * Check a passed CurseForge project and file ID combination. Checks whether a server pack is already for the given
     * combination and then checks their status and returns it wrapped in a {@link NotificationResponse}. If a server pack is not
     * available for the given combination, a scan task is sent to {@link TaskSubmitter#scanCurseProject(String)} with the
     * given CurseForge project and file ID combination.
     * @author Griefed
     * @param projectID Integer. CurseForge projectID.
     * @param fileID Integer. CurseForge fileID.
     * @throws CurseException Exception thrown if an error occurs during the acquisition of information from CurseForge.
     * @return String. Returns a {@link NotificationResponse} with some information about the requested project and file, as well as the status, which is either of <code>Status 0: Already exists</code>, <code>Status 1: OK, generating</code> or <code>Status 2: Error occurred</code>.
     */
    public String createFromCurseModpack(int projectID, int fileID) throws CurseException {

        if (SERVERPACKSERVICE.findByProjectIDAndFileID(projectID, fileID).isPresent()) {

            ServerPackModel pack = SERVERPACKSERVICE.findByProjectIDAndFileID(projectID, fileID).get();

            if (pack.getStatus().equals("Available")) {
                return CURSERESPONSEMODEL.curseResponse(
                        pack.getProjectName(),
                        0,
                        "The modpack you requested a server pack for has already been generated. Check the downloads-section!",
                        5000,
                        "info",
                        "info"
                );
            }

            if (pack.getStatus().equals("Queued")) {
                return CURSERESPONSEMODEL.curseResponse(
                        pack.getProjectName(),
                        1,
                        "The modpack you requested a server pack for has already been queued!",
                        5000,
                        "info",
                        "info"
                );
            }

            if (pack.getStatus().equals("Generating") && (new Timestamp(new Date().getTime()).getTime() - pack.getLastModified().getTime()) < 1800000) {
                return CURSERESPONSEMODEL.curseResponse(
                        pack.getProjectName(),
                        1,
                        "The modpack you requested a server pack for is currently being generated!",
                        5000,
                        "info",
                        "info"
                );
            }

        }

        try {

            ServerPackModel serverPackModel = new ServerPackModel();
            List<String> encounteredErrors = new ArrayList<>(100);

            if (CONFIGURATIONHANDLER.checkCurseForge(projectID + "," + fileID, serverPackModel, encounteredErrors)) {

                TASKSUBMITTER.scanCurseProject(projectID + "," + fileID);

                return CURSERESPONSEMODEL.curseResponse(
                        serverPackModel.getProjectName(),
                        1,
                        "Queued! Come back later and check the downloads-section to see whether your server pack" +
                                " for " + serverPackModel.getProjectName() + " is ready for download!",
                        7000,
                        "done",
                        "positive"
                );

            } else {



                return CURSERESPONSEMODEL.curseResponse(
                        projectID + "," + fileID,
                        2,
                        "Project or file could not be found!",
                        3000,
                        "error",
                        "negative"
                );

            }

        } catch (InvalidModpackException ex) {
            LOG.error("Couldn't generate server pack for project: " + projectID + "," + fileID, ex);
            return CURSERESPONSEMODEL.curseResponse(
                    projectID,
                    2,
                    "The specified project is not a Minecraft modpack!",
                    3000,
                    "error",
                    "negative"
            );

        } catch (InvalidFileException ex) {
            LOG.error("The specified file is not a file of project: " + projectID + "," + fileID, ex);
            return CURSERESPONSEMODEL.curseResponse(
                    projectID,
                    2,
                    "The specified file could not be found for this project!",
                    3000,
                    "error",
                    "negative"
            );

        } catch (CurseException ex) {
            LOG.error("The specified project does not exist: " + projectID + "," + fileID, ex);
            return CURSERESPONSEMODEL.curseResponse(
                    projectID,
                    2,
                    "The specified project does not exist!",
                    3000,
                    "error",
                    "negative"
            );
        }
    }

    /**
     * Regenerates server pack for the given CurseForge project and file ID.<br>
     * Requires <code>de.griefed.serverpackcreator.spring.cursecontroller.regenerate.enabled=true</code>, otherwise a message is returned
     * telling the requester that regeneration is disabled on this instance of ServerPackCreator.
     * @author Griefed
     * @param modpack CurseForge projectID and fileID combination.
     * @return String. Returns a {@link NotificationResponse#curseResponse(String, int, String, int, String, String)} telling the requester the status of their request.
     */
    public String regenerateFromCurseModpack(String modpack) {

        if (APPLICATIONPROPERTIES.getCurseControllerRegenerationEnabled()) {

            if (SERVERPACKSERVICE.findByProjectIDAndFileID(Integer.parseInt(modpack.split(",")[0]), Integer.parseInt(modpack.split(",")[1])).isPresent()) {

                ServerPackModel serverPackModel = SERVERPACKSERVICE.findByProjectIDAndFileID(Integer.parseInt(modpack.split(",")[0]), Integer.parseInt(modpack.split(",")[1])).get();
                serverPackModel.setModpackDir(modpack);
                serverPackModel.setStatus("Queued");
                SERVERPACKSERVICE.updateServerPackByID(serverPackModel.getId(), serverPackModel);
                TASKSUBMITTER.generateCurseProject(modpack);

                return CURSERESPONSEMODEL.curseResponse(
                        modpack,
                        1,
                        "Regenerating server pack.",
                        3000,
                        "done",
                        "positive"
                );

            } else {

                return CURSERESPONSEMODEL.curseResponse(
                        modpack,
                        2,
                        "Server pack can not be regenerated. It doesn't exist.",
                        4000,
                        "info",
                        "warning"
                );

            }

        } else {

            return CURSERESPONSEMODEL.curseResponse(
                    modpack,
                    2,
                    "Regeneration is disabled on this instance!",
                    4000,
                    "info",
                    "warning"
            );
        }

    }
}
