/* Copyright (C) 2023  Griefed
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
package de.griefed.serverpackcreator.web.task

import de.griefed.serverpackcreator.web.zip.GenerateZip
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jms.core.JmsTemplate
import org.springframework.stereotype.Service

/**
 * [How
 * to implement a task queue using Apache Artemis and Spring Boot](https://dev.to/gotson/how-to-implement-a-task-queue-using-apache-artemis-and-spring-boot-2mme)<br></br> Huge Thank You to [Gauthier](https://github.com/gotson) for writing the above guide on how to implement a
 * JMS. Without it this implementation of Artemis would have either taken way longer or never
 * happened at all. I managed to translate their Kotlin-code to Java and make the necessary changes
 * to fully implement it in ServerPackCreator.<br></br> Class responsible for submitting tasks to our
 * JMS.
 *
 * @author Griefed
 */
@Service
@Suppress("unused")
class TaskSubmitter @Autowired constructor(private val jmsTemplate: JmsTemplate) {
    private val log = cachedLoggerOf(this.javaClass)

    /**
     * Submit a task for the generation of a server pack from a ZIP-archive.
     *
     * @param zipGenerationProperties containing all information required to generate a server pack
     * from a ZIP-archive. See
     * [de.griefed.serverpackcreator.web.zip.ZipController.requestGenerationFromZip].
     * @author Griefed
     */
    fun generateZip(zipGenerationProperties: String) {
        log.debug("Sending ZIP generate task: $zipGenerationProperties")
        submitGeneration(GenerateZip(zipGenerationProperties))
    }

    /**
     * Convert and send a generation-task to our JMS. Set the `type` to `generation ` and
     * the `unique id` to tasks unique id which contains the CurseForge project and file id
     * combination.
     *
     * @param task The task to be submitted to the generation-queue.
     * @author Griefed
     */
    private fun submitGeneration(task: Task) {
        log.info("Submitting generation $task")
        log.debug("UniqueID is: ${task.uniqueId()}")
        jmsTemplate.convertAndSend(
            "tasks.background",
            task
        ) { message ->
            message.setStringProperty("type", "generation")
            message.setStringProperty("unique_id", task.uniqueId())
            message
        }
    }

    /**
     * Convert and send a scan-task to our JMS. Set the `type` to `scan` and the
     * `unique id` to tasks unique id which contains the CurseForge project and file id
     * combination.
     *
     * @param task The task to be submitted to the scan-queue.
     * @author Griefed
     */
    private fun submitScan(task: Task) {
        log.info("Submitting scan $task")
        log.debug("UniqueID is: ${task.uniqueId()}")
        jmsTemplate.convertAndSend(
            "tasks.background",
            task
        ) { message ->
            message.setStringProperty("type", "scan")
            message.setStringProperty("unique_id", task.uniqueId())
            message
        }
    }
}