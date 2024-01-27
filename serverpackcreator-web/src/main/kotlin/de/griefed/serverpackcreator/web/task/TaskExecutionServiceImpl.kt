/* Copyright (C) 2024  Griefed
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

import de.griefed.serverpackcreator.api.ConfigurationHandler
import de.griefed.serverpackcreator.api.ServerPackHandler
import de.griefed.serverpackcreator.api.utilities.common.deleteQuietly
import de.griefed.serverpackcreator.api.utilities.common.size
import de.griefed.serverpackcreator.web.data.ModPack
import de.griefed.serverpackcreator.web.data.ServerPack
import de.griefed.serverpackcreator.web.modpack.ModpackService
import de.griefed.serverpackcreator.web.modpack.ModpackSource
import de.griefed.serverpackcreator.web.modpack.ModpackStatus
import de.griefed.serverpackcreator.web.serverpack.ServerPackService
import de.griefed.serverpackcreator.web.storage.StorageException
import org.apache.logging.log4j.kotlin.KotlinLogger
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import org.bouncycastle.util.encoders.Hex
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingDeque

@Service
class TaskExecutionServiceImpl @Autowired constructor(
    private val modpackService: ModpackService,
    private val serverPackService: ServerPackService,
    private val configurationHandler: ConfigurationHandler,
    private val serverPackHandler: ServerPackHandler,
    private val messageDigestInstance: MessageDigest,
    private val eventService: EventService
) :
    TaskExecutionService {
    private val logger: KotlinLogger = cachedLoggerOf(this.javaClass)
    private val blockingQueue: BlockingQueue<TaskDetail> = LinkedBlockingDeque()

    init {
        initiateThread()
    }

    /**
     * Single Thread on Which Tasks will be performed
     */
    private fun initiateThread() {
        val thread = Thread {
            while (true) {
                try {
                    if (!blockingQueue.isEmpty()) {
                        logger.info("Processing Next Task from Queue")
                        val taskDetail = blockingQueue.take()
                        processTask(taskDetail)
                    } else {
                        Thread.sleep(1000)
                    }
                } catch (e: InterruptedException) {
                    logger.error("There was an error while processing ", e)
                    Thread.currentThread().interrupt()
                }
            }
        }
        thread.name = "GenerationThread"
        thread.start()
        logger.info("Worker Thread ${thread.name} initiated successfully")
    }

    /**
     * Method to Submit Tasks in Queue
     */
    override fun submitTaskInQueue(taskDetail: TaskDetail) {
        eventService.submit(taskDetail.modpack.id, taskDetail.serverPack?.id, taskDetail.modpack.status, "Submitted task to queue.")
        blockingQueue.add(taskDetail)
    }

    private fun processTask(taskDetail: TaskDetail) {
        logger.info("Running on Thread ${Thread.currentThread().name}")
        when (taskDetail.modpack.status) {
            ModpackStatus.QUEUED -> checkModpack(taskDetail)
            ModpackStatus.CHECKED -> {
                if (taskDetail.modpack.source == ModpackSource.ZIP) {
                    generateFromZip(taskDetail)
                } else {
                    generateFromModrinth(taskDetail.modpack)
                }
            }

            ModpackStatus.GENERATED, ModpackStatus.ERROR -> finishing(taskDetail)
            else -> logger.error("${taskDetail.modpack.status} does not merit unique processing.")
        }
    }

    private fun finishing(taskDetail: TaskDetail) {
        if (taskDetail.modPackFile != null) {
            eventService.submit(taskDetail.modpack.id, taskDetail.serverPack?.id, taskDetail.modpack.status, "Syncing ModPack to database.")
        }
        if (taskDetail.serverPack != null && taskDetail.serverPackFile != null) {
            eventService.submit(taskDetail.modpack.id, taskDetail.serverPack?.id, taskDetail.modpack.status, "Syncing ServerPack to database.")
        }
        Runtime.getRuntime().gc()
        logger.info("Remaining tasks in queue: ${getQueueSize()}")
    }

    private fun checkModpack(taskDetail: TaskDetail) {
        logger.info("Performing Modpack check for modpack : ${taskDetail.modpack.id}")
        val zipFile = modpackService.getModPackArchive(taskDetail.modpack)
        if (zipFile.isEmpty) {
            throw StorageException("ModPack-file for ${taskDetail.modpack.id} not found.")
        }
        taskDetail.modpack.status = ModpackStatus.CHECKING
        eventService.submit(taskDetail.modpack.id, taskDetail.serverPack?.id, taskDetail.modpack.status, "Checking ModPack for errors.")
        modpackService.saveModpack(taskDetail.modpack)
        val packConfig = modpackService.getPackConfigForModpack(taskDetail.modpack, taskDetail.runConfiguration!!)
        taskDetail.modPackFile = File(packConfig.modpackDir)
        val check = configurationHandler.checkConfiguration(packConfig)
        if (check.allChecksPassed) {
            taskDetail.modpack.status = ModpackStatus.CHECKED
            taskDetail.packConfig = packConfig
            eventService.submit(taskDetail.modpack.id, taskDetail.serverPack?.id, taskDetail.modpack.status, "ModPack checks passed.")
        } else {
            taskDetail.modpack.status = ModpackStatus.ERROR
            eventService.submit(taskDetail.modpack.id, taskDetail.serverPack?.id, taskDetail.modpack.status, "ModPack checks not passed.", check.encounteredErrors)
        }
        modpackService.saveModpack(taskDetail.modpack)
        submitTaskInQueue(taskDetail)
    }

    private fun generateFromModrinth(modpack: ModPack) {
        logger.info("Server Pack will be generated from Modrinth modpack : ${modpack.id}")
        logger.warn("Modrinth API will be available in Milestone 6.")
        /*logger.info("Server Pack generated.")*/
    }

    private fun generateFromZip(taskDetail: TaskDetail) {
        logger.info("Server Pack will be generated from uploaded, zipped, modpack : ${taskDetail.modpack.id}")
        taskDetail.modpack.status = ModpackStatus.GENERATING
        eventService.submit(taskDetail.modpack.id, taskDetail.serverPack?.id, taskDetail.modpack.status, "Generating ServerPack.")
        modpackService.saveModpack(taskDetail.modpack)
        if (taskDetail.packConfig == null && taskDetail.runConfiguration != null) {
            taskDetail.packConfig =
                modpackService.getPackConfigForModpack(taskDetail.modpack, taskDetail.runConfiguration!!)
        }
        if (serverPackHandler.run(taskDetail.packConfig!!)) {
            val destination = serverPackHandler.getServerPackDestination(taskDetail.packConfig!!)
            val serverPackZip = File("${destination}_server_pack.zip").absoluteFile
            val serverPack = ServerPack()
            serverPack.size = serverPackZip.size().div(1048576.0).toInt()
            serverPack.runConfiguration = taskDetail.runConfiguration
            serverPack.fileID = serverPackZip.name.replace("_server_pack.zip","").toLong()
            serverPack.sha256 = String(Hex.encode(messageDigestInstance.digest(serverPackZip.readBytes())))
            serverPackService.saveServerPack(serverPack)
            taskDetail.modpack.serverPacks.addLast(serverPack)
            taskDetail.modpack.status = ModpackStatus.GENERATED
            eventService.submit(taskDetail.modpack.id, taskDetail.serverPack?.id, taskDetail.modpack.status, "Generated ServerPack.")
            taskDetail.serverPack = serverPack
            taskDetail.serverPackFile = serverPackZip
            //logger.info("Storing server pack : ${serverPack.id}")
            //serverPackService.storeGeneration(serverPackZip, serverPack)
            File(destination).deleteQuietly()
            File(taskDetail.packConfig!!.modpackDir).deleteQuietly()
        } else {
            taskDetail.modpack.status = ModpackStatus.ERROR
            eventService.submit(taskDetail.modpack.id, taskDetail.serverPack?.id, taskDetail.modpack.status, "Error generating ServerPack. Contact your admin for details.")
        }
        modpackService.saveModpack(taskDetail.modpack)
        submitTaskInQueue(taskDetail)
        logger.info("Server Pack generated.")
    }

    /**
     * Method to get Queue Size
     */
    override fun getQueueSize(): Int {
        return blockingQueue.size
    }

    /**
     * Method to clear all tasks from queue
     */
    override fun clearQueue(): String {
        val size = getQueueSize()
        blockingQueue.clear()
        return "Cleared Queue. It had total tasks : $size"
    }

    /**
     * Method to remove a Task from Queue
     */
    override fun removeTaskForModpack(modpack: ModPack): String {
        val taskList = blockingQueue.stream().filter { task: TaskDetail ->
            task.modpack == modpack
        }.toList().toSet()
        blockingQueue.removeAll(taskList)
        return "Total ${taskList.size} removed from Queue."
    }

    /**
     * Method to get all Task details present in Queue
     */
    override fun getQueueDetails(): List<TaskDetail> {
        return blockingQueue.stream().toList()
    }
}