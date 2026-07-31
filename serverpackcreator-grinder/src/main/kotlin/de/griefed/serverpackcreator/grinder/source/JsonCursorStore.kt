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
package de.griefed.serverpackcreator.grinder.source

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap

/**
 * File-backed [CursorStore] that survives restarts, mirroring
 * [de.griefed.serverpackcreator.grinder.report.JsonVerdictStore]'s approach: positions are loaded from
 * [file] on construction and the whole (tiny) document is rewritten on every [store] via
 * temp-then-atomic-move, so a crash mid-write cannot truncate it. This is what lets a months-long crawl
 * resume mid-catalog instead of restarting at the most-downloaded mods after every restart.
 *
 * A corrupt or unreadable file is logged and treated as empty: losing the position costs one extra sweep
 * (whose fresh verdicts are skipped anyway), whereas refusing to start costs the whole service.
 *
 * @param file The JSON document backing the store (its parent directories are created on first write).
 * @author Griefed
 */
class JsonCursorStore(private val file: File) : CursorStore {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

    private val mapper = jacksonObjectMapper()

    private val cursors = ConcurrentHashMap<String, CatalogCursor>()

    init {
        load()
    }

    override fun cursor(source: String): CatalogCursor = cursors[source] ?: CatalogCursor.START

    @Synchronized
    override fun store(source: String, cursor: CatalogCursor) {
        cursors[source] = cursor
        persist()
    }

    /** Populate from the backing file if it exists; a read failure leaves every catalog at its start (logged). */
    private fun load() {
        if (!file.isFile) {
            return
        }
        runCatching { mapper.readValue<Map<String, CatalogCursor>>(file) }
            .onSuccess { cursors.putAll(it) }
            .onFailure { log.warn("Could not read crawl cursors ${file.absolutePath}; starting at the top: ${it.message}") }
    }

    /** Write the whole document, then move it over [file], so an interrupted write cannot corrupt it. */
    private fun persist() {
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, "${file.name}.tmp")
        mapper.writerWithDefaultPrettyPrinter().writeValue(tmp, cursors.toSortedMap())
        try {
            Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
