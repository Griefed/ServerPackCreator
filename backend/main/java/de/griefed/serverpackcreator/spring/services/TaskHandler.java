/* Copyright (C) 2021  Griefed
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
package de.griefed.serverpackcreator.spring.services;

import de.griefed.serverpackcreator.ConfigurationHandler;
import de.griefed.serverpackcreator.ServerPackHandler;
import de.griefed.serverpackcreator.spring.models.ServerPack;
import de.griefed.serverpackcreator.spring.models.Task;
import org.apache.commons.lang.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.Date;

/**
 *
 * @author Griefed
 */
@Service
public class TaskHandler {

    private static final Logger LOG = LogManager.getLogger(TaskHandler.class);

    private final ConfigurationHandler CONFIGURATIONHANDLER;
    private final ServerPackHandler SERVERPACKHANDLER;
    private final ServerPackService SERVERPACKSERVICE;
    private final TaskReceiver TASKRECEIVER;
    private final StopWatch STOPWATCH;

    /**
     *
     * @author Griefed
     * @param injectedConfigurationHandler
     * @param injectedServerPackHandler
     * @param injectedServerPackService
     */
    @Autowired
    public TaskHandler(ConfigurationHandler injectedConfigurationHandler, ServerPackHandler injectedServerPackHandler, ServerPackService injectedServerPackService, TaskReceiver injectedTaskReceiver) {
        this.CONFIGURATIONHANDLER = injectedConfigurationHandler;
        this.SERVERPACKHANDLER = injectedServerPackHandler;
        this.SERVERPACKSERVICE = injectedServerPackService;
        this.TASKRECEIVER = injectedTaskReceiver;
        this.STOPWATCH = new StopWatch();
    }

    /**
     *
     * @author Griefed
     * @param task
     */
    @JmsListener(destination = "tasks.background", selector = "type = 'scan'")
    public void handleScan(Task task) {
        LOG.info("Executing task: " + task);
        try {

            if (task instanceof Task.ScanCurseProject) {

                LOG.info("Instance of ScanCurseProject " + task.uniqueId());

                String[] project = ((Task.ScanCurseProject) task).getProjectIDAndFileID().split(",");
                int projectID = Integer.parseInt(project[0]);
                int fileID = Integer.parseInt(project[1]);

                ServerPack serverPack = new ServerPack();

                try {
                    if (!SERVERPACKSERVICE.findByProjectIDAndFileID(projectID, fileID).isPresent() && CONFIGURATIONHANDLER.checkCurseForge(projectID + "," + fileID, serverPack)) {

                        serverPack.setModpackDir(projectID + "," + fileID);
                        serverPack.setStatus("Queued");
                        SERVERPACKSERVICE.insert(serverPack);
                        TASKRECEIVER.generateCurseProject(projectID + "," + fileID);

                    } else if (SERVERPACKSERVICE.findByProjectIDAndFileID(projectID, fileID).isPresent()) {

                        serverPack = SERVERPACKSERVICE.findByProjectIDAndFileID(projectID, fileID).get();

                        if (serverPack.getStatus().equals("Generating") && (new Timestamp(new Date().getTime()).getTime() - serverPack.getLastModified().getTime()) >= 1800000  && CONFIGURATIONHANDLER.checkCurseForge(projectID + "," + fileID, serverPack)) {
                            serverPack.setModpackDir(projectID + "," + fileID);
                            serverPack.setStatus("Queued");
                            SERVERPACKSERVICE.updateServerPackByProjectIDAndFileID(projectID, fileID, serverPack);
                            TASKRECEIVER.generateCurseProject(projectID + "," + fileID);
                        }

                    }
                } catch (Exception ex) {
                    LOG.error("An error occurred submitting the task for generation for " + projectID + ", " + fileID, ex);
                    if (SERVERPACKSERVICE.findByProjectIDAndFileID(projectID, fileID).isPresent()) {
                        SERVERPACKSERVICE.deleteByProjectIDAndFileID(projectID, fileID);
                    }
                }

            } else {

                LOG.info("This is not the queue you are looking for: " + task.uniqueId());
            }

        } catch (Exception ex) {
            LOG.error("Error submitting generationTask", ex);
        }

    }

    /**
     *
     * @author Griefed
     * @param task
     */
    @JmsListener(destination = "tasks.background", selector = "type = 'generation'")
    public void handleGeneration(Task task) {
        LOG.info("Executing task: " + task);
        try {

            if (task instanceof Task.GenerateCurseProject) {

                LOG.info("Instance of GenerateCurseProject " + task.uniqueId());

                String[] project = ((Task.GenerateCurseProject) task).getProjectIDAndFileID().split(",");
                int projectID = Integer.parseInt(project[0]);
                int fileID = Integer.parseInt(project[1]);

                ServerPack serverPack = SERVERPACKSERVICE.findByProjectIDAndFileID(projectID, fileID).get();

                serverPack.setStatus("Generating");

                SERVERPACKSERVICE.updateServerPackByID(serverPack.getId(), serverPack);

                serverPack.setModpackDir(projectID + "," + fileID);

                ServerPack pack = null;

                STOPWATCH.reset();
                STOPWATCH.start();

                try {

                    pack = SERVERPACKHANDLER.run(serverPack);

                    if (pack!=null) {
                        SERVERPACKSERVICE.updateServerPackByID(pack.getId(), pack);
                    }

                } catch (Exception ex) {

                    LOG.error("An error occurred generating the server pack for " + projectID + ", " + fileID, ex);
                    if (SERVERPACKSERVICE.findByProjectIDAndFileID(projectID, fileID).isPresent()) {
                        SERVERPACKSERVICE.deleteServerPack(serverPack.getId());
                    }

                } finally {
                    STOPWATCH.stop();
                    LOG.info("Generation took " + STOPWATCH);
                    STOPWATCH.reset();
                }

            } else {

                LOG.info("This is not the queue you are looking for: " + task.uniqueId());
            }

        } catch (Exception ex) {
            LOG.error("Error generating server pack", ex);
        }

    }
}
