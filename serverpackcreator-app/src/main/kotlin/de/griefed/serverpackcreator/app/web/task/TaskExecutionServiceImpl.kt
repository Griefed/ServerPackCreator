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
package de.griefed.serverpackcreator.app.web.task

import de.griefed.serverpackcreator.api.config.ConfigurationHandler
import de.griefed.serverpackcreator.api.serverpack.ServerPackHandler
import de.griefed.serverpackcreator.api.utilities.common.deleteQuietly
import de.griefed.serverpackcreator.app.web.modpack.ModPack
import de.griefed.serverpackcreator.app.web.modpack.ModPackService
import de.griefed.serverpackcreator.app.web.modpack.ModPackStatus
import de.griefed.serverpackcreator.app.web.serverpack.ServerPack
import de.griefed.serverpackcreator.app.web.serverpack.ServerPackService
import de.griefed.serverpackcreator.app.web.storage.StorageException
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingDeque

@Service
class TaskExecutionServiceImpl @Autowired constructor(
    private val modpackService: ModPackService,
    private val serverPackService: ServerPackService,
    private val configurationHandler: ConfigurationHandler,
    private val serverPackHandler: ServerPackHandler,
    private val eventService: EventService
) :
    TaskExecutionService {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
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
                        log.info("Processing Next Task from Queue")
                        val taskDetail = blockingQueue.take()
                        processTask(taskDetail)
                    } else {
                        Thread.sleep(1000)
                    }
                } catch (e: InterruptedException) {
                    log.error("There was an error while processing ", e)
                    Thread.currentThread().interrupt()
                }
            }
        }
        thread.name = "GenerationThread"
        thread.start()
        log.info("Worker Thread ${thread.name} initiated successfully")
    }

    /**
     * Method to Submit Tasks in Queue
     */
    override fun submitTaskInQueue(taskDetail: TaskDetail) {
        eventService.submit(
            taskDetail.modpack.id,
            taskDetail.serverPack?.id,
            taskDetail.modpack.status,
            "Submitted task to queue."
        )
        blockingQueue.add(taskDetail)
    }

    private fun processTask(taskDetail: TaskDetail) {
        log.info("Running on Thread ${Thread.currentThread().name}")
        when (taskDetail.modpack.status) {
            ModPackStatus.QUEUED -> checkModpack(taskDetail)
            ModPackStatus.CHECKED -> generateFromZip(taskDetail)
            ModPackStatus.GENERATED, ModPackStatus.ERROR -> finishing(taskDetail)
            else -> log.error("${taskDetail.modpack.status} does not merit unique processing.")
        }
    }

    private fun finishing(taskDetail: TaskDetail) {
        if (taskDetail.modPackFile != null) {
            eventService.submit(
                taskDetail.modpack.id,
                taskDetail.serverPack?.id,
                taskDetail.modpack.status,
                "Syncing ModPack to database."
            )
        }
        if (taskDetail.serverPack != null && taskDetail.serverPackFile != null) {
            eventService.submit(
                taskDetail.modpack.id,
                taskDetail.serverPack?.id,
                taskDetail.modpack.status,
                "Syncing ServerPack to database."
            )
        }
        Runtime.getRuntime().gc()
        log.info("Remaining tasks in queue: ${getQueueSize()}")
    }

    private fun checkModpack(taskDetail: TaskDetail) {
        log.info("Performing Modpack check for modpack : ${taskDetail.modpack.id}")
        val zipFile = modpackService.getModPackArchive(taskDetail.modpack)
        if (zipFile.isEmpty) {
            throw StorageException("ModPack-file for ${taskDetail.modpack.id} not found.")
        }
        taskDetail.modpack.status = ModPackStatus.CHECKING
        eventService.submit(
            taskDetail.modpack.id,
            taskDetail.serverPack?.id,
            taskDetail.modpack.status,
            "Checking ModPack for errors."
        )
        modpackService.saveModpack(taskDetail.modpack)
        val packConfig = modpackService.getPackConfigForModpack(taskDetail.modpack, taskDetail.runConfiguration!!)
        taskDetail.modPackFile = File(packConfig.modpackDir)
        val check = configurationHandler.checkConfiguration(packConfig)
        if (check.allChecksPassed) {
            taskDetail.modpack.status = ModPackStatus.CHECKED
            taskDetail.packConfig = packConfig
            if (packConfig.projectID != null && packConfig.versionID != null) {
                taskDetail.modpack.projectID = packConfig.projectID!!
                taskDetail.modpack.versionID = packConfig.versionID!!
                taskDetail.modpack.source = packConfig.source
            }
            eventService.submit(
                taskDetail.modpack.id,
                taskDetail.serverPack?.id,
                taskDetail.modpack.status,
                "ModPack checks passed."
            )
        } else {
            taskDetail.modpack.status = ModPackStatus.ERROR
            eventService.submit(
                taskDetail.modpack.id,
                taskDetail.serverPack?.id,
                taskDetail.modpack.status,
                "ModPack checks not passed.",
                check.encounteredErrors
            )
        }
        modpackService.saveModpack(taskDetail.modpack)
        submitTaskInQueue(taskDetail)
    }

    private fun generateFromZip(taskDetail: TaskDetail) {
        log.info("Server Pack will be generated from uploaded, zipped, modpack : ${taskDetail.modpack.id}")
        taskDetail.modpack.status = ModPackStatus.GENERATING
        eventService.submit(
            taskDetail.modpack.id,
            taskDetail.serverPack?.id,
            taskDetail.modpack.status,
            "Generating ServerPack."
        )
        modpackService.saveModpack(taskDetail.modpack)
        if (taskDetail.packConfig == null && taskDetail.runConfiguration != null) {
            taskDetail.packConfig = modpackService
                .getPackConfigForModpack(taskDetail.modpack, taskDetail.runConfiguration!!)
        }
        val generation = serverPackHandler.run(taskDetail.packConfig!!)
        if (generation.success) {
            val serverPackZipOld = generation.serverPackZip.get().absoluteFile
            val savedFile = serverPackService.storeServerPackFile(serverPackZipOld)
            val serverPack = ServerPack(
                savedFile.size,
                taskDetail.runConfiguration,
                savedFile.id,
                serverPackZipOld.absolutePath,
                savedFile.sha256,
                taskDetail.modpack.id!!
            )

            serverPackService.saveServerPack(serverPack)
            taskDetail.modpack.serverPacks.addLast(serverPack)
            taskDetail.modpack.status = ModPackStatus.GENERATED
            eventService.submit(
                taskDetail.modpack.id,
                taskDetail.serverPack?.id,
                taskDetail.modpack.status,
                "Generated ServerPack."
            )
            taskDetail.serverPack = serverPack
            taskDetail.serverPackFile = savedFile.file.toFile()
            generation.serverPack.deleteQuietly()
            File(taskDetail.packConfig!!.modpackDir).deleteQuietly()
        } else {
            taskDetail.modpack.status = ModPackStatus.ERROR
            eventService.submit(
                taskDetail.modpack.id,
                taskDetail.serverPack?.id,
                taskDetail.modpack.status,
                "Error generating ServerPack. Contact your admin for details."
            )
        }
        modpackService.saveModpack(taskDetail.modpack)
        submitTaskInQueue(taskDetail)
        log.info("Server Pack generated.")
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