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
package de.griefed.serverpackcreator.spring.services;

import de.griefed.serverpackcreator.ConfigurationHandler;
import de.griefed.serverpackcreator.ServerPackHandler;
import de.griefed.serverpackcreator.spring.models.GenerateCurseProject;
import de.griefed.serverpackcreator.spring.models.ScanCurseProject;
import de.griefed.serverpackcreator.spring.models.ServerPack;
import de.griefed.serverpackcreator.spring.models.Task;
import org.apache.commons.lang.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * <a href="https://dev.to/gotson/how-to-implement-a-task-queue-using-apache-artemis-and-spring-boot-2mme">How to implement a task queue using Apache Artemis and Spring Boot</a><br>
 * Huge Thank You to <a href="https://github.com/gotson">Gauthier</a> for writing the above guide on how to implement a JMS. Without it this implementation of Artemis
 * would have either taken way longer or never happened at all. I managed to translate their Kotlin-code to Java and make
 * the necessary changes to fully implement it in ServerPackCreator.<br>
 * TaskHandler class which determines what to do with all message in our JMS, depending on their task type and content of
 * the message.
 * @author Griefed
 */
@Service
public class TaskHandler {

    private static final Logger LOG = LogManager.getLogger(TaskHandler.class);

    private final ConfigurationHandler CONFIGURATIONHANDLER;
    private final ServerPackHandler SERVERPACKHANDLER;
    private final ServerPackService SERVERPACKSERVICE;
    private final TaskSubmitter TASKSUBMITTER;
    private final StopWatch STOPWATCH;

    /**
     * Constructor responsible for our DI.
     * @author Griefed
     * @param injectedConfigurationHandler Instance of {@link ConfigurationHandler}.
     * @param injectedServerPackHandler Instance of {@link ServerPackHandler}.
     * @param injectedServerPackService Instance of {@link ServerPackService}.
     * @param injectedTaskSubmitter Instance of {@link TaskSubmitter}.
     */
    @Autowired
    public TaskHandler(ConfigurationHandler injectedConfigurationHandler, ServerPackHandler injectedServerPackHandler, ServerPackService injectedServerPackService, TaskSubmitter injectedTaskSubmitter) {
        this.CONFIGURATIONHANDLER = injectedConfigurationHandler;
        this.SERVERPACKHANDLER = injectedServerPackHandler;
        this.SERVERPACKSERVICE = injectedServerPackService;
        this.TASKSUBMITTER = injectedTaskSubmitter;
        this.STOPWATCH = new StopWatch();
    }

    /**
     * {@link JmsListener} listening to the destination <code>tasks.background</code> and selector <code>type = 'scan'</code>, so only task
     * that match the <code>scan</code>-type are worked with in this method.<br>
     * If a task is received that matches this type, the CurseForge project and file ID of said task is checked for validity.
     * If the combination is found valid, either a new entry is saved to the database or an already existing one updated,
     * if the existing one has the status <code>Generating</code> and <code>lastModified</code> is bigger than 30 minutes.
     * In either case, a {@link GenerateCurseProject}-task is sent which will then generate a server pack from the CurseForge project
     * and file ID combination.
     * @author Griefed
     * @param task The task for which to check the CurseForge project and file ID, as well as status.
     */
    @JmsListener(destination = "tasks.background", selector = "type = 'scan'")
    public void handleScan(Task task) {
        LOG.info("Executing task: " + task);
        try {

            if (task instanceof ScanCurseProject) {

                LOG.info("Instance of ScanCurseProject " + task.uniqueId());

                String[] project = ((ScanCurseProject) task).getProjectIDAndFileID().split(",");
                int projectID = Integer.parseInt(project[0]);
                int fileID = Integer.parseInt(project[1]);

                ServerPack serverPack = new ServerPack();

                try {

                    List<String> encounteredErrors = new ArrayList<>(100);

                    if (!SERVERPACKSERVICE.findByProjectIDAndFileID(projectID, fileID).isPresent() && CONFIGURATIONHANDLER.checkCurseForge(projectID + "," + fileID, serverPack, encounteredErrors)) {

                        serverPack.setModpackDir(projectID + "," + fileID);
                        serverPack.setStatus("Queued");
                        SERVERPACKSERVICE.insert(serverPack);
                        TASKSUBMITTER.generateCurseProject(projectID + "," + fileID);

                    } else if (SERVERPACKSERVICE.findByProjectIDAndFileID(projectID, fileID).isPresent()) {

                        serverPack = SERVERPACKSERVICE.findByProjectIDAndFileID(projectID, fileID).get();

                        if (serverPack.getStatus().equals("Generating") && (new Timestamp(new Date().getTime()).getTime() - serverPack.getLastModified().getTime()) >= 1800000  && CONFIGURATIONHANDLER.checkCurseForge(projectID + "," + fileID, serverPack, encounteredErrors)) {
                            serverPack.setModpackDir(projectID + "," + fileID);
                            serverPack.setStatus("Queued");
                            SERVERPACKSERVICE.updateServerPackByProjectIDAndFileID(projectID, fileID, serverPack);
                            TASKSUBMITTER.generateCurseProject(projectID + "," + fileID);
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
     * {@link JmsListener} listening to the destination <code>tasks.background</code> and selector <code>type = 'generation'</code>, so only task
     * that match the <code>generation</code>-type are worked with in this method.<br>
     * If a task is received that matches this type, the CurseForge project and file ID of said task is used to retrieve the
     * relevant entry in the database. Said entry in the database is then used to generate our server pack with and subsequently updated
     * with new values gathered in {@link ServerPackHandler#run(ServerPack)}. If at any point an error occurs during the creation of a server pack,
     * the database entry is deleted completely, so we prevent dead entries in the database. Downside being that the request must
     * check whether the server pack is available for download and resubmit, if it isn't.
     * @author Griefed
     * @param task The task with which to generate a server pack from a CurseForge project and file ID.
     */
    @JmsListener(destination = "tasks.background", selector = "type = 'generation'")
    public void handleGeneration(Task task) {
        LOG.info("Executing task: " + task);
        try {

            if (task instanceof GenerateCurseProject) {

                LOG.info("Instance of GenerateCurseProject " + task.uniqueId());

                String[] project = ((GenerateCurseProject) task).getProjectIDAndFileID().split(",");
                int projectID = Integer.parseInt(project[0]);
                int fileID = Integer.parseInt(project[1]);

                ServerPack serverPack = SERVERPACKSERVICE.findByProjectIDAndFileID(projectID, fileID).get();

                serverPack.setStatus("Generating");
                serverPack.setDownloads(0);
                serverPack.setConfirmedWorking(0);

                SERVERPACKSERVICE.updateServerPackByID(serverPack.getId(), serverPack);

                serverPack.setModpackDir(projectID + "," + fileID);

                ServerPack pack = null;

                STOPWATCH.reset();
                STOPWATCH.start();

                try {

                    CONFIGURATIONHANDLER.checkConfiguration(serverPack, true, true);

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
