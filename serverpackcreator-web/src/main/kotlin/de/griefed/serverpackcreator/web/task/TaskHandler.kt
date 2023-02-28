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

import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.ConfigurationHandler
import de.griefed.serverpackcreator.api.ServerPackHandler
import de.griefed.serverpackcreator.api.utilities.SimpleStopWatch
import de.griefed.serverpackcreator.api.utilities.common.deleteQuietly
import de.griefed.serverpackcreator.api.utilities.common.size
import de.griefed.serverpackcreator.web.serverpack.ServerPackModel
import de.griefed.serverpackcreator.web.serverpack.ServerPackService
import de.griefed.serverpackcreator.web.zip.GenerateZip
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jms.annotation.JmsListener
import org.springframework.stereotype.Service
import java.io.File

/**
 * [How
 * to implement a task queue using Apache Artemis and Spring Boot](https://dev.to/gotson/how-to-implement-a-task-queue-using-apache-artemis-and-spring-boot-2mme)<br></br> Huge Thank You to [Gauthier](https://github.com/gotson) for writing the above guide on how to implement a
 * JMS. Without it this implementation of Artemis would have either taken way longer or never
 * happened at all. I managed to translate their Kotlin-code to Java and make the necessary changes
 * to fully implement it in ServerPackCreator.<br></br> TaskHandler class which determines what to do
 * with all message in our JMS, depending on their task type and content of the message.
 *
 * @author Griefed
 */
@Service
@Suppress("unused")
class TaskHandler @Autowired constructor(
    private val apiProperties: ApiProperties,
    private val configurationHandler: ConfigurationHandler,
    private val serverPackHandler: ServerPackHandler,
    private val serverPackService: ServerPackService
) {
    private val log = cachedLoggerOf(this.javaClass)
    private val simpleStopWatch: SimpleStopWatch = SimpleStopWatch()

    /**
     * [JmsListener] listening to the destination `tasks.background` and selector
     * `type = 'scan'`, so only task that match the `scan`-type are worked with in this
     * method.<br></br> If a task is received that matches this type, the CurseForge project and file ID of
     * said task is checked for validity. If the combination is found valid, either a new entry is
     * saved to the database or an already existing one updated, if the existing one has the status
     * `Generating` and `lastModified` is bigger than 30 minutes.
     *
     * @param task The task for which to check the CurseForge project and file ID, as well as status.
     * @author Griefed
     */
    @JmsListener(destination = "tasks.background", selector = "type = 'scan'")
    fun handleScan(task: Task) {
        log.info("Executing task: $task")
    }

    /**
     * [JmsListener] listening to the destination `tasks.background` and selector
     * `type = 'generation'`, so only task that match the `generation`-type are worked
     * with in this method.<br></br> If a task is received that matches this type, the generation of a new
     * server pack is started.
     *
     * @param task The task with which to generate a server pack from a CurseForge project and file
     * ID.
     * @author Griefed
     */
    @JmsListener(destination = "tasks.background", selector = "type = 'generation'")
    fun handleGeneration(task: Task) {
        log.info("Executing task: $task")
        if (task is GenerateZip) {
            log.info("Instance of GenerateZip: ${task.uniqueId()}")
            val parameters: List<String> = task.zipGenerationProperties.split("&")
            val encounteredErrors: MutableList<String> = ArrayList(100)
            val serverPackModel = ServerPackModel()
            serverPackModel.status = "Generating"
            serverPackModel.downloads = 0
            serverPackModel.confirmedWorking = 0
            serverPackModel.fileDiskName = parameters[0]
            serverPackModel.modpackDir = File(apiProperties.modpacksDirectory, parameters[0]).absolutePath
            serverPackModel.minecraftVersion = parameters[2]
            serverPackModel.modloader = parameters[3]
            serverPackModel.modloaderVersion = parameters[4]
            serverPackModel.setClientMods(parameters[1].split(",").dropLastWhile { it.isEmpty() } as ArrayList<String>)
            serverPackModel.isServerInstallationDesired = false
            try {
                simpleStopWatch.start()
                if (!configurationHandler.checkConfiguration(serverPackModel, encounteredErrors, false)) {
                    serverPackModel.fileName = File(serverPackModel.modpackDir).name
                    serverPackService.insert(serverPackModel)
                    serverPackHandler.run(serverPackModel)
                    val destination = serverPackHandler.getServerPackDestination(serverPackModel)
                    serverPackModel.size =
                        File(destination + "_server_pack.zip").size().div(1048576.0)
                    serverPackModel.path = destination + "_server_pack.zip"
                    serverPackModel.status = "Available"
                    serverPackService.updateServerPackByID(serverPackModel.id, serverPackModel)
                } else {
                    log.error("Configuration check for ZIP-archive ${parameters[0]} failed.")
                    if (encounteredErrors.isNotEmpty()) {
                        log.error("Encountered errors: ")
                        for (error in encounteredErrors) {
                            log.error(error)
                        }
                    }
                }
            } catch (ex: Exception) {
                log.error("An error occurred generating the server pack for ZIP-archive: ${parameters[0]}", ex)
                serverPackService.deleteServerPack(serverPackModel.id)
                if (encounteredErrors.isNotEmpty()) {
                    log.error("Encountered errors: ")
                    for (error in encounteredErrors) {
                        log.error(error)
                    }
                }
            } finally {
                File(apiProperties.modpacksDirectory, parameters[0]).deleteQuietly()
                log.info("Generation took ${simpleStopWatch.stop().getTime()}")
            }
        } else {
            log.info("This is not the queue you are looking for: ${task.uniqueId()}")
        }
    }
}