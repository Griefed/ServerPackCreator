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
package de.griefed.serverpackcreator.grinder.loader

import de.griefed.serverpackcreator.api.utilities.common.orNullLogged
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.io.Writer

/**
 * The install console as it streams, written line by line into [file] so an operator can follow a minutes-long
 * install and keep its output if the grinder is killed. Writing it is bookkeeping: a failure never fails the install.
 */
internal class LiveInstallLog(
    private val file: File,
    /** Opens [file] for writing; a parameter only so a failing writer can be executed in a test. */
    open: (File) -> Writer = { it.bufferedWriter() }
) : Closeable {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
    private val writer: Writer? = orNullLogged(
        log.delegate, "Could not open the live install console ${file.absolutePath}; the install runs without it."
    ) { open(file) }

    /** Set by the first failed write, so a broken console is reported once rather than once per line. */
    private var writeFailed = false

    /** Append and flush one console [line]. */
    fun append(line: String) {
        val target = writer ?: return
        try {
            target.appendLine(line)
            target.flush()
        } catch (ex: IOException) {
            if (!writeFailed) {
                writeFailed = true
                log.warn(
                    "Could not write a line to the live install console ${file.absolutePath}; " +
                        "later lines may be missing.",
                    ex
                )
            }
        }
    }

    override fun close() {
        orNullLogged(log.delegate, "Could not close the live install console ${file.absolutePath}.") { writer?.close() }
    }
}
