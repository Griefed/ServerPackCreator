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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service

/**
 * Records and reads the queue's progress events — the stream the SPA polls to show what is happening to an
 * upload. Append-only: an event is a record of a moment, never updated.
 */
@Service
class EventService @Autowired constructor(
    private val errorRepository: ErrorRepository,
    private val queueEventRepository: QueueEventRepository
) {

    /** Record one event against a modpack, and optionally a server pack, with the status and message to show. */
    fun submit(
        modPackId: String?,
        serverPackId: String?,
        status: ModPackStatus?,
        message: String,
        errors: List<String>? = null
    ) {
        val event = QueueEvent()
        event.modPackId = modPackId
        event.serverPackId = serverPackId
        event.status = status
        event.message = message
        if (!errors.isNullOrEmpty()) {
            for (error in errors) {
                event.errors.add(ErrorEntry(error))
            }
            for (i in event.errors.indices) {
                // One lookup, reused: the stored entry when this error is already known, a freshly
                // saved one otherwise.
                val stored = errorRepository.findByError(event.errors[i].error)
                event.errors[i] = stored.orElseGet { errorRepository.save(event.errors[i]) }
            }
        }
        queueEventRepository.save(event)
    }

    /** Every event, newest first. */
    fun loadAll(sort: Sort = Sort.by(Sort.Direction.DESC, "timestamp")): MutableList<QueueEvent> {
        return queueEventRepository.findAll(sort)
    }

    /** One page of events, as a `Page` so the caller learns the total. */
    fun loadAll(sizedPage: PageRequest, sort: Sort = Sort.by(Sort.Direction.DESC, "dateCreated")) : Page<QueueEvent> {
        return queueEventRepository.findAll(sizedPage.withSort(sort))
    }

    /** Every event for one modpack — the history of a single upload. */
    fun loadAllByModPackId(modPackId: String): MutableList<QueueEvent> {
        return queueEventRepository.findAllByModPackId(modPackId)
    }

    /** Every event for one server pack. */
    fun loadAllByServerPackId(serverPackId: String): MutableList<QueueEvent> {
        return queueEventRepository.findAllByServerPackId(serverPackId)
    }

    /** Every event that reported a given status, for finding what failed. */
    fun loadAllByStatus(status: ModPackStatus): MutableList<QueueEvent> {
        return queueEventRepository.findAllByStatus(status)
    }
}