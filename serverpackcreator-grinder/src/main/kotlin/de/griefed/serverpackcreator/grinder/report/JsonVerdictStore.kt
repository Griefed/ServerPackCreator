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

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
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
 * file is logged and treated as empty rather than crashing the service — and **copied aside first**,
 * because [record] persists the whole map immediately, so anything unread would otherwise be overwritten
 * by whatever survived. Unknown fields and unreadable individual rows are tolerated for the same reason:
 * a store written by a newer build must survive a downgrade.
 *
 * @param file The JSON document backing the store (its parent directory is created on first write).
 * @author Griefed
 */
class JsonVerdictStore(private val file: File) : VerdictStore {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

    // Unknown properties are tolerated so a store written by a *newer* build stays readable after a
    // downgrade. Failing on one would route a perfectly good store down the corrupt path, and from there
    // the next record() rewrites the file from an empty map -- losing every verdict over a field name.
    private val mapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

    private val verdicts = ConcurrentHashMap<String, GrindVerdict>()

    init {
        load()
    }

    @Synchronized
    override fun record(verdict: GrindVerdict) {
        // Drop the id-less row for this project first, so an identified verdict replaces it rather than
        // sitting beside it. Without this a project ground before ids existed would hold two rows for good.
        supersededLegacyKey(verdict)?.let { verdicts.remove(it) }
        verdicts[verdict.identityKey()] = verdict
        persist()
    }

    override fun all(): List<GrindVerdict> = verdicts.values.toList()


    /**
     * Populate from the backing file if it exists. Read **row by row** rather than as one document, so a
     * single verdict this build cannot make sense of -- a confidence constant added by a later version,
     * say -- costs that row instead of every row. Whatever could not be read is preserved by
     * [preserveUnreadable] first, because [persist] runs on the very next [record] and would otherwise
     * overwrite the evidence with what survived.
     */
    private fun load() {
        if (!file.isFile) {
            return
        }
        val elements = runCatching { mapper.readValue<List<JsonNode>>(file) }.getOrElse { failure ->
            log.warn("Could not read verdict store ${file.absolutePath}; starting empty: ${failure.message}")
            preserveUnreadable()
            return
        }

        var skipped = 0
        for (element in elements) {
            runCatching { mapper.treeToValue(element, GrindVerdict::class.java) }
                .onSuccess { verdicts[it.identityKey()] = it }
                .onFailure { failure ->
                    skipped++
                    log.warn("Skipping an unreadable verdict in ${file.absolutePath}: ${failure.message}")
                }
        }
        if (skipped > 0) {
            log.warn("$skipped of ${elements.size} verdicts could not be read; the rest were kept.")
            preserveUnreadable()
        }
    }

    /**
     * Copy the backing file aside before anything overwrites it, naming the copy after the moment it was
     * rescued. A **copy** rather than a move: the store must still be where the operator expects it, and
     * the rescued bytes are what a later build (or a human) needs to recover the rows this one dropped.
     */
    private fun preserveUnreadable() {
        val backup = File(file.parentFile, "${file.name}.unreadable-${System.currentTimeMillis()}")
        runCatching { Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING) }
            .onSuccess { log.warn("Preserved the unreadable verdict store at ${backup.absolutePath}") }
            .onFailure { log.error("Could not preserve the unreadable verdict store: ${it.message}") }
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
