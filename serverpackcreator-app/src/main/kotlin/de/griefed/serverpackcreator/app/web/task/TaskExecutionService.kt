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

import de.griefed.serverpackcreator.app.web.modpack.ModPack


/**
 * The generation queue, behind an interface so the web layer neither knows nor cares that it is backed by a
 * single worker. Submitting is fire-and-forget: progress comes back as `QueueEvent`s, not as a return value.
 */
interface TaskExecutionService {
    /** Queue one generation. Returns immediately; watch the events for what happens next. */
    fun submitTaskInQueue(taskDetail: TaskDetail)

    /** How many tasks are waiting, for the dashboard. */
    fun getQueueSize(): Int

    /** Drop every waiting task, returning what was done as a message for the operator. */
    fun clearQueue(): String

    /** Drop the waiting task for one modpack — used when that modpack is deleted underneath it. */
    fun removeTaskForModpack(modpack: ModPack): String

    /** What is currently queued, so an operator can see the backlog rather than just its size. */
    fun getQueueDetails(): List<TaskDetail>
}