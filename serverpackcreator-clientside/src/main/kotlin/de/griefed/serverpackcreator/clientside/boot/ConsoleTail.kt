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
package de.griefed.serverpackcreator.clientside.boot

import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

/**
 * The end of a server console, read by seeking: a boot can log for its whole budget, and the report shows only its
 * last lines.
 */
object ConsoleTail {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

    /** How much of a console's end is read: room for the report's lines, a bounded heap cost whatever the file's size. */
    const val WINDOW_BYTES = 1_048_576

    /**
     * The last [maxLines] complete lines within the final [windowBytes] of [console], or `null` when it cannot be
     * read — a missing console is something the report can say, an exception would cost the report.
     */
    fun lastLines(console: File, maxLines: Int, windowBytes: Int = WINDOW_BYTES): List<String>? = try {
        RandomAccessFile(console, "r").use { handle ->
            val start = maxOf(0L, handle.length() - windowBytes)
            val window = ByteArray((handle.length() - start).toInt())
            handle.seek(start)
            handle.readFully(window)
            val lines = String(window, Charsets.UTF_8).lines().dropLastWhile { it.isEmpty() }
            // A window that does not start the file starts mid-line, and that cut line is not the console's.
            (if (start > 0) lines.drop(1) else lines).takeLast(maxLines)
        }
    } catch (ex: IOException) {
        log.warn("Could not read the console ${console.absolutePath}", ex)
        null
    }
}
