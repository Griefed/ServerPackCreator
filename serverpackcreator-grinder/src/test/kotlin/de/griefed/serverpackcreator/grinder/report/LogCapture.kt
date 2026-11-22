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
package de.griefed.serverpackcreator.grinder.report

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.Logger
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.config.Property
import java.util.Collections

/** Records what [owner]'s logger writes while [block] runs, for guards asserting that a failure reached the log. */
internal fun <T> capturingLogsOf(owner: Class<*>, block: (CapturedLog) -> T): T {
    val logger = LogManager.getLogger(owner) as Logger
    val appender = CapturedLog().apply { start() }
    logger.addAppender(appender)
    try {
        return block(appender)
    } finally {
        logger.removeAppender(appender)
        appender.stop()
    }
}

/** The events one [capturingLogsOf] call recorded. */
internal class CapturedLog : AbstractAppender("capture", null, null, true, Property.EMPTY_ARRAY) {
    private val events: MutableList<LogEvent> = Collections.synchronizedList(mutableListOf())

    override fun append(event: LogEvent) {
        events.add(event.toImmutable())
    }

    /** Whether an event at [level] carrying an exception was recorded. */
    fun hasThrowableAt(level: Level): Boolean = synchronized(events) { events.any { it.level == level && it.thrown != null } }
}
