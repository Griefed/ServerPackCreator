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
import de.griefed.serverpackcreator.spring.repositories.ServerPackRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

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
    private final ServerPackRepository SERVERPACKREPOSITORY;
    private final TaskReceiver TASKRECEIVER;

    /**
     *
     * @author Griefed
     * @param injectedConfigurationHandler
     * @param injectedServerPackHandler
     * @param injectedServerPackService
     */
    @Autowired
    public TaskHandler(ConfigurationHandler injectedConfigurationHandler, ServerPackHandler injectedServerPackHandler, ServerPackService injectedServerPackService, ServerPackRepository injectedServerPackRepository, TaskReceiver injectedTaskReceiver) {
        this.CONFIGURATIONHANDLER = injectedConfigurationHandler;
        this.SERVERPACKHANDLER = injectedServerPackHandler;
        this.SERVERPACKSERVICE = injectedServerPackService;
        this.SERVERPACKREPOSITORY = injectedServerPackRepository;
        this.TASKRECEIVER = injectedTaskReceiver;
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

                if (!SERVERPACKREPOSITORY.findByProjectIDAndFileID(projectID, fileID).isPresent() && CONFIGURATIONHANDLER.checkCurseForge(projectID + "," + fileID, serverPack)) {

                    serverPack.setModpackDir(projectID + "," + fileID);
                    serverPack.setStatus("Queued");
                    SERVERPACKSERVICE.insert(serverPack);
                    TASKRECEIVER.generateCurseProject(projectID + "," + fileID);

                }

            } else if (task instanceof Task.GenerateCurseProject) {

                LOG.info("GenerateCurseProject should not be in this handler. " + task.uniqueId());

            } else {

                LOG.info("Not an instance of any known tasks: " + task.toString());
            }

        } catch (Exception ex) {
            LOG.error("Error generating server pack", ex);
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

            if (task instanceof Task.ScanCurseProject) {

                LOG.info("ScanCurseProject should not be in this handler. " + task.uniqueId());

            } else if (task instanceof Task.GenerateCurseProject) {

                LOG.info("Instance of GenerateCurseProject " + task.uniqueId());

                String[] project = ((Task.GenerateCurseProject) task).getProjectIDAndFileID().split(",");
                int projectID = Integer.parseInt(project[0]);
                int fileID = Integer.parseInt(project[1]);

                ServerPack serverPack = SERVERPACKREPOSITORY.findByProjectIDAndFileID(projectID, fileID).get();

                serverPack.setModpackDir(projectID + "," + fileID);

                ServerPack pack = SERVERPACKHANDLER.run(serverPack);

                SERVERPACKSERVICE.updateServerPackByID(pack.getId(), pack);



            } else {

                LOG.info("Not an instance of any known tasks: " + task.toString());
            }

        } catch (Exception ex) {
            LOG.error("Error generating server pack", ex);
        }

    }
}
