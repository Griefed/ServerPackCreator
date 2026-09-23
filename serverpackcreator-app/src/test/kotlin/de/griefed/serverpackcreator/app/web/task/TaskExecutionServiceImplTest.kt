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

import de.griefed.serverpackcreator.api.config.ConfigCheck
import de.griefed.serverpackcreator.api.config.ConfigurationHandler
import de.griefed.serverpackcreator.api.config.PackConfig
import de.griefed.serverpackcreator.api.serverpack.ServerPackHandler
import de.griefed.serverpackcreator.app.web.assignEntityId
import de.griefed.serverpackcreator.app.web.modpack.ModPack
import de.griefed.serverpackcreator.app.web.modpack.ModPackService
import de.griefed.serverpackcreator.app.web.modpack.ModPackStatus
import de.griefed.serverpackcreator.app.web.serverpack.ServerPackService
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.Optional
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Guards the one worker thread behind the generation queue.
 *
 * The queue is drained by a single `while (true)` loop, so anything that escapes it takes the whole
 * webservice's generation capability down until the process restarts — silently, with no event and no
 * status change. That is what these assert against.
 */
class TaskExecutionServiceImplTest {

    @TempDir
    lateinit var tempDir: Path

    private val modpackService: ModPackService = mockk()
    private val serverPackService: ServerPackService = mockk()
    private val configurationHandler: ConfigurationHandler = mockk()
    private val serverPackHandler: ServerPackHandler = mockk()
    private val eventService: EventService = mockk()

    /** A queued [ModPack] carrying [id]. */
    private fun queuedModPack(id: String): ModPack = ModPack().apply {
        assignEntityId(this, id)
        fileID = id
        status = ModPackStatus.QUEUED
    }

    @Test
    fun aTaskThatThrowsDoesNotStopTheQueueFromDrainingTheNextOne() {
        val poisoned = queuedModPack("poisonedPack")
        val following = queuedModPack("followingPack")

        // The poisoned task reproduces the real throw: checkModpack raises StorageException the moment
        // the archive a queued modpack names is not on disk.
        every { modpackService.getModPackArchive(poisoned) } returns Optional.empty()

        val archive = tempDir.resolve("followingPack.zip").toFile().apply { writeText("pack") }
        every { modpackService.getModPackArchive(following) } returns Optional.of(archive)
        every { modpackService.saveModpack(any()) } returnsArgument 0
        every { modpackService.getPackConfigForModpack(any(), any()) } returns PackConfig()
        // A failing check keeps the task from cascading into an actual generation.
        every { configurationHandler.checkConfiguration(any<PackConfig>()) } returns
                ConfigCheck().apply { modpackErrors.add("checks not passed") }

        val followingWasChecked = CountDownLatch(1)
        justRun { eventService.submit(any(), any(), any(), any(), any()) }
        every {
            eventService.submit("followingPack", any(), ModPackStatus.CHECKING, any(), any())
        } answers { followingWasChecked.countDown() }

        val queue = TaskExecutionServiceImpl(
            modpackService, serverPackService, configurationHandler, serverPackHandler, eventService
        )
        queue.submitTaskInQueue(TaskDetail(poisoned).apply { runConfiguration = mockk(relaxed = true) })
        queue.submitTaskInQueue(TaskDetail(following).apply { runConfiguration = mockk(relaxed = true) })

        Assertions.assertTrue(
            followingWasChecked.await(10, TimeUnit.SECONDS),
            "The task queued behind a throwing one was never processed — the worker thread died with it."
        )
    }

    @Test
    fun aTaskThatThrowsIsReportedAsAnErrorRatherThanVanishing() {
        val poisoned = queuedModPack("poisonedPack")
        every { modpackService.getModPackArchive(poisoned) } returns Optional.empty()
        every { modpackService.saveModpack(any()) } returnsArgument 0

        val failureWasReported = CountDownLatch(1)
        justRun { eventService.submit(any(), any(), any(), any(), any()) }
        every {
            eventService.submit("poisonedPack", any(), ModPackStatus.ERROR, any(), any())
        } answers { failureWasReported.countDown() }

        val queue = TaskExecutionServiceImpl(
            modpackService, serverPackService, configurationHandler, serverPackHandler, eventService
        )
        queue.submitTaskInQueue(TaskDetail(poisoned).apply { runConfiguration = mockk(relaxed = true) })

        Assertions.assertTrue(
            failureWasReported.await(10, TimeUnit.SECONDS),
            "A task that threw left no ERROR event, so nothing downstream can tell it failed."
        )
    }
}
