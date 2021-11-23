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

import de.griefed.serverpackcreator.ApplicationProperties;
import de.griefed.serverpackcreator.spring.models.GenerateCurseProject;
import de.griefed.serverpackcreator.spring.models.ScanCurseProject;
import de.griefed.serverpackcreator.spring.models.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

/**
 *
 * @author Griefed
 */
@Service
public class TaskReceiver {

    private static final Logger LOG = LogManager.getLogger(TaskReceiver.class);

    private JmsTemplate jmsTemplate;
    private final ApplicationProperties APPLICATIONPROPERTIES;

    /**
     *
     * @author Griefed
     * @param injectedJmsTemplate
     */
    @Autowired
    public TaskReceiver(JmsTemplate injectedJmsTemplate, ApplicationProperties injectedApplicationProperties) {
        this.jmsTemplate = injectedJmsTemplate;
        this.APPLICATIONPROPERTIES = injectedApplicationProperties;
    }

    /**
     *
     * @author Griefed
     * @param projectIDAndFileID
     */
    public void scanCurseProject(String projectIDAndFileID) {
        submitScan(new ScanCurseProject(projectIDAndFileID));
    }

    /**
     *
     * @author Griefed
     * @param projectIDAndFileID
     */
    public void generateCurseProject(String projectIDAndFileID) {
        submitGeneration(new GenerateCurseProject(projectIDAndFileID));
    }

    /**
     *
     * @author Griefed
     * @param task
     */
    void submitScan(Task task) {
        LOG.info("Submitting scan " + task);
        LOG.debug("UniqueID is: " + task.uniqueId());
        jmsTemplate.convertAndSend("tasks.background", task, message -> {
            message.setStringProperty("type", "scan");
            message.setStringProperty("unique_id", task.uniqueId());
            return message;
        });
    }

    /**
     *
     * @author Griefed
     * @param task
     */
    void submitGeneration(Task task) {
        LOG.info("Submitting generation " + task);
        LOG.debug("UniqueID is: " + task.uniqueId());
        jmsTemplate.convertAndSend("tasks.background", task, message -> {
            message.setStringProperty("type", "generation");
            message.setStringProperty("unique_id", task.uniqueId());
            return message;
        });
    }
}
