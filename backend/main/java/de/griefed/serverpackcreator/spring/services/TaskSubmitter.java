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
 * <a href="https://dev.to/gotson/how-to-implement-a-task-queue-using-apache-artemis-and-spring-boot-2mme">How to implement a task queue using Apache Artemis and Spring Boot</a><br>
 * Huge Thank You to <a href="https://github.com/gotson">Gauthier</a> for writing the above guide on how to implement a JMS. Without it this implementation of Artemis
 * would have either taken way longer or never happened at all. I managed to translate their Kotlin-code to Java and make
 * the necessary changes to fully implement it in ServerPackCreator.<br>
 * Class responsible for submitting tasks to our JMS.
 * @author Griefed
 */
@Service
public class TaskSubmitter {

    private static final Logger LOG = LogManager.getLogger(TaskSubmitter.class);

    private JmsTemplate jmsTemplate;
    private final ApplicationProperties APPLICATIONPROPERTIES;

    /**
     * Constructor responsible for our DI.
     * @author Griefed
     * @param injectedJmsTemplate Instance of {@link JmsTemplate}.
     * @param injectedApplicationProperties Instance of {@link ApplicationProperties}.
     */
    @Autowired
    public TaskSubmitter(JmsTemplate injectedJmsTemplate, ApplicationProperties injectedApplicationProperties) {
        this.jmsTemplate = injectedJmsTemplate;
        this.APPLICATIONPROPERTIES = injectedApplicationProperties;
    }

    /**
     * Submit a task for scanning a CurseForge project and file id combination.
     * @author Griefed
     * @param projectIDAndFileID The CurseForge project and file id combination
     */
    public void scanCurseProject(String projectIDAndFileID) {
        submitScan(new ScanCurseProject(projectIDAndFileID));
    }

    /**
     * Submit a task for the generation of a server pack from a CurseForge project and file id combination.
     * @author Griefed
     * @param projectIDAndFileID The CurseForge project and file id combination.
     */
    public void generateCurseProject(String projectIDAndFileID) {
        submitGeneration(new GenerateCurseProject(projectIDAndFileID));
    }

    /**
     * Convert and send a scan-task to our JMS. Set the <code>type</code> to <code>scan</code> and the <code>unique id</code>
     * to tasks unique id which contains the CurseForge project and file id combination.
     * @author Griefed
     * @param task The task to be submitted to the scan-queue.
     */
    private void submitScan(Task task) {
        LOG.info("Submitting scan " + task);
        LOG.debug("UniqueID is: " + task.uniqueId());
        jmsTemplate.convertAndSend("tasks.background", task, message -> {
            message.setStringProperty("type", "scan");
            message.setStringProperty("unique_id", task.uniqueId());
            return message;
        });
    }

    /**
     * Convert and send a generation-task to our JMS. Set the <code>type</code> to <code>generation</code> and the <code>unique id</code>
     * to tasks unique id which contains the CurseForge project and file id combination.
     * @author Griefed
     * @param task The task to be submitted to the generation-queue.
     */
    private void submitGeneration(Task task) {
        LOG.info("Submitting generation " + task);
        LOG.debug("UniqueID is: " + task.uniqueId());
        jmsTemplate.convertAndSend("tasks.background", task, message -> {
            message.setStringProperty("type", "generation");
            message.setStringProperty("unique_id", task.uniqueId());
            return message;
        });
    }
}
