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

import de.griefed.serverpackcreator.app.web.modpack.ModPackStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service

@Service
class EventService @Autowired constructor(
    private val errorRepository: ErrorRepository,
    private val queueEventRepository: QueueEventRepository
) {

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
            for (i in 0 until event.errors.size) {
                if (errorRepository.findByError(event.errors[i].error).isPresent) {
                    event.errors[i] = errorRepository.findByError(event.errors[i].error).get()
                } else {
                    event.errors[i] = errorRepository.save(event.errors[i])
                }
            }
        }
        queueEventRepository.save(event)
    }

    fun loadAll(sort: Sort = Sort.by(Sort.Direction.DESC, "timestamp")): MutableList<QueueEvent> {
        return queueEventRepository.findAll(sort)
    }

    fun loadAll(sizedPage: PageRequest, sort: Sort = Sort.by(Sort.Direction.DESC, "dateCreated")) : Page<QueueEvent> {
        return queueEventRepository.findAll(sizedPage.withSort(sort))
    }

    fun loadAllByModPackId(modPackId: String): MutableList<QueueEvent> {
        return queueEventRepository.findAllByModPackId(modPackId)
    }

    fun loadAllByServerPackId(serverPackId: String): MutableList<QueueEvent> {
        return queueEventRepository.findAllByServerPackId(serverPackId)
    }

    fun loadAllByStatus(status: ModPackStatus): MutableList<QueueEvent> {
        return queueEventRepository.findAllByStatus(status)
    }
}