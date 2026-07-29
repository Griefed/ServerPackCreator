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

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import de.griefed.serverpackcreator.grinder.GrindVerdict
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap

/**
 * File-backed [VerdictStore] that survives restarts: verdicts are loaded from [file] on construction
 * and re-persisted on every [record], so a multi-day fire-and-forget grind resumes where it left off
 * instead of re-booting everything. Keyed by [verdictKey] (platform + slug + loader) like the in-memory
 * store — shared so the two key schemes cannot drift — so a re-verified triple replaces rather than
 * duplicates, while the same slug on another platform keeps its own row. Existing stores need no
 * migration: keys are derived from fields every persisted verdict already carries. A corrupt/unreadable
 * file is logged and treated as empty rather than crashing the service.
 *
 * @param file The JSON document backing the store (its parent directory is created on first write).
 * @author Griefed
 */
class JsonVerdictStore(private val file: File) : VerdictStore {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

    private val mapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    private val verdicts = ConcurrentHashMap<String, GrindVerdict>()

    init {
        load()
    }

    @Synchronized
    override fun record(verdict: GrindVerdict) {
        verdicts[verdictKey(verdict.platform, verdict.slug, verdict.loader)] = verdict
        persist()
    }

    override fun all(): List<GrindVerdict> = verdicts.values.toList()


    /** Populate from the backing file if it exists; a read failure leaves the store empty (logged). */
    private fun load() {
        if (!file.isFile) {
            return
        }
        runCatching { mapper.readValue<List<GrindVerdict>>(file) }
            .onSuccess { stored -> stored.forEach { verdicts[verdictKey(it.platform, it.slug, it.loader)] = it } }
            .onFailure { log.warn("Could not read verdict store ${file.absolutePath}; starting empty: ${it.message}") }
    }

    /**
     * Write the whole store, then move it over [file], so a crash mid-write can never leave a truncated
     * (and therefore unreadable) store. Falls back to a plain replace where atomic moves are unsupported.
     */
    private fun persist() {
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, "${file.name}.tmp")
        mapper.writerWithDefaultPrettyPrinter().writeValue(tmp, verdicts.values.sortedWith(compareBy({ it.slug }, { it.loader })))
        try {
            Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
