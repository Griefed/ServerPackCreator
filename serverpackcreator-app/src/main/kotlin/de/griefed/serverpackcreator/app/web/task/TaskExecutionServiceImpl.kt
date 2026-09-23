/* Copyright (C) 2026 Griefed
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

/**
 * The generation queue's one worker: a blocking queue drained by a single thread, so two generations never run
 * at once. That is deliberate — concurrent generations would race in the same server-packs directory.
 * 
 * Progress is reported by writing `QueueEvent`s as each stage completes, which is the only way a caller learns
 * what happened to a fire-and-forget submission.
 */
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
     * Start the single worker that drains the queue. It blocks on [BlockingQueue.take] rather than
     * polling, and every task is run through [runTask], because nothing a task throws may be allowed
     * to end this loop — it is the only one there is.
     */
    private fun initiateThread() {
        val thread = Thread {
            while (!Thread.currentThread().isInterrupted) {
                val taskDetail = try {
                    blockingQueue.take()
                } catch (interruption: InterruptedException) {
                    log.info("Generation queue interrupted. Shutting the worker down.", interruption)
                    Thread.currentThread().interrupt()
                    break
                }
                log.info("Processing Next Task from Queue")
                runTask(taskDetail)
            }
            log.warn("Worker Thread ${Thread.currentThread().name} has stopped.")
        }
        thread.name = "GenerationThread"
        thread.start()
        log.info("Worker Thread ${thread.name} initiated successfully")
    }

    /**
     * Run one task, surviving whatever it throws. `checkModpack` raises a StorageException for a
     * missing archive, and generation can fail in any number of ways; before this, any of them ended
     * the worker for the lifetime of the process and left every later upload stuck in QUEUED.
     */
    private fun runTask(taskDetail: TaskDetail) {
        try {
            processTask(taskDetail)
        } catch (failure: Throwable) {
            log.error("Task for modpack ${taskDetail.modpack.id} failed and was abandoned.", failure)
            reportFailure(taskDetail, failure)
        }
    }

    /**
     * Record an abandoned task as an ERROR on the modpack and as a [QueueEvent], so the pack does not
     * sit in CHECKING forever with nothing saying why. Its own failures are swallowed deliberately: an
     * unreachable database here would otherwise kill the worker, which is the very defect this exists
     * to remove.
     */
    private fun reportFailure(taskDetail: TaskDetail, failure: Throwable) {
        try {
            taskDetail.modpack.status = ModPackStatus.ERROR
            modpackService.saveModpack(taskDetail.modpack)
            eventService.submit(
                taskDetail.modpack.id,
                taskDetail.serverPack?.id,
                ModPackStatus.ERROR,
                "Processing failed and was abandoned.",
                listOf(failure.message ?: failure.javaClass.simpleName)
            )
        } catch (reportingFailure: Throwable) {
            log.error("Could not record the failure of modpack ${taskDetail.modpack.id}.", reportingFailure)
        }
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