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

import de.griefed.serverpackcreator.app.web.modpack.ModPackStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.Optional

/**
 * Characterization tests for [EventService.submit]'s error de-duplication, which had no coverage:
 * the controller test mocks this service away entirely.
 *
 * What is pinned is the *outcome* — which entries the stored event ends up holding, and which of
 * them reach `save` — deliberately **not** how many times the repository is queried to get there.
 * The number of lookups is an implementation detail, and pinning it would turn any future change of
 * that shape into a red test for no reason.
 */
internal class EventServiceTest {

    private val errorRepository: ErrorRepository = mockk()
    private val queueEventRepository: QueueEventRepository = mockk()
    private val eventService = EventService(errorRepository, queueEventRepository)

    /** Captures the event handed to the queue repository, which is the only observable result. */
    private fun submitCapturing(errors: List<String>?): QueueEvent {
        val captured = slot<QueueEvent>()
        every { queueEventRepository.save(capture(captured)) } answers { captured.captured }
        eventService.submit("modpack-1", "serverpack-1", ModPackStatus.QUEUED, "a message", errors)
        return captured.captured
    }

    /**
     * An error string the repository already knows must come back as the *stored* entry rather than
     * the freshly-constructed one, so the event references the existing document instead of
     * duplicating it.
     */
    @Test
    fun aKnownErrorIsReplacedByTheStoredEntry() {
        val stored = ErrorEntry("already known")
        every { errorRepository.findByError("already known") } returns Optional.of(stored)

        val event = submitCapturing(listOf("already known"))

        Assertions.assertSame(stored, event.errors.single())
        verify(exactly = 0) { errorRepository.save(any()) }
    }

    /** An unknown error is saved, and it is the saved entry that lands on the event. */
    @Test
    fun anUnknownErrorIsSavedAndTheSavedEntryIsKept() {
        val persisted = ErrorEntry("brand new")
        every { errorRepository.findByError("brand new") } returns Optional.empty()
        every { errorRepository.save(any()) } returns persisted

        val event = submitCapturing(listOf("brand new"))

        Assertions.assertSame(persisted, event.errors.single())
        verify(exactly = 1) { errorRepository.save(any()) }
    }

    /** A mixed batch keeps input order, reusing the known entry and saving only the unknown one. */
    @Test
    fun aMixedBatchReusesTheKnownEntryAndSavesOnlyTheUnknownOne() {
        val stored = ErrorEntry("known")
        val persisted = ErrorEntry("unknown")
        every { errorRepository.findByError("known") } returns Optional.of(stored)
        every { errorRepository.findByError("unknown") } returns Optional.empty()
        every { errorRepository.save(any()) } returns persisted

        val event = submitCapturing(listOf("known", "unknown"))

        Assertions.assertEquals(listOf(stored, persisted), event.errors)
        verify(exactly = 1) { errorRepository.save(any()) }
    }

    /** With no errors the error repository is never touched, and the event still gets stored. */
    @Test
    fun anEventWithoutErrorsNeverTouchesTheErrorRepository() {
        val event = submitCapturing(null)

        Assertions.assertTrue(event.errors.isEmpty())
        Assertions.assertEquals("a message", event.message)
        verify(exactly = 0) { errorRepository.findByError(any()) }
        verify(exactly = 0) { errorRepository.save(any()) }
        verify(exactly = 1) { queueEventRepository.save(any()) }
    }

    /** An empty list is treated like no errors at all, not like a batch of zero. */
    @Test
    fun anEmptyErrorListIsTreatedAsNoErrors() {
        val event = submitCapturing(emptyList())

        Assertions.assertTrue(event.errors.isEmpty())
        verify(exactly = 0) { errorRepository.findByError(any()) }
    }
}
