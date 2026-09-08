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
package de.griefed.serverpackcreator.grinder

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import de.griefed.serverpackcreator.clientside.LearnedModIds
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * File-backed [LearnedModIds], so what one run proves about a mod id the next run starts with.
 *
 * The map bridges the two vocabularies staging speaks — a descriptor names a mod **id**, a platform serves
 * a **ref** — and it is built from jars the grinder downloads anyway. Without a file behind it a restart
 * re-pays every probe download it has ever made, which is the one cost that route was designed to avoid.
 *
 * **Mirrors [de.griefed.serverpackcreator.grinder.source.JsonCursorStore] on purpose**, because the failure
 * modes are the same: loaded on construction, rewritten whole via temp-then-atomic-move so a crash mid-write
 * cannot truncate it, and an unreadable document logged and treated as empty rather than refusing to start.
 * Everything here is re-derivable by grinding, so losing it costs downloads while refusing to boot the
 * service costs everything.
 *
 * **Written only when something is genuinely new.** Every staged dependency re-declares its own id on every
 * candidate that uses it; persisting those would be a write per staged jar for an unchanged document, which
 * is why [LearnedModIds] announces news rather than every `learn`.
 *
 * It lives in the base package rather than `report` or `source` because it is neither a verdict nor a crawl
 * position — it is composition-level state the daemon owns and hands to the engine.
 *
 * @param file The JSON document backing the map; its parent directories are created on first write.
 * @author Griefed
 */
class JsonLearnedModIds(private val file: File) {

    private val log by lazy { cachedLoggerOf(this.javaClass) }

    private val mapper = jacksonObjectMapper()

    /** The map itself: seeded from [file] and writing back whenever it learns something new. */
    val ids = LearnedModIds(onLearned = ::persist).apply { restore(load()) }

    /** Read the document, or an empty map when it is absent (a first start) or unreadable (logged). */
    private fun load(): Map<String, Map<String, String>> {
        if (!file.isFile) {
            return emptyMap()
        }
        return runCatching { mapper.readValue<Map<String, Map<String, String>>>(file) }
            .onFailure {
                log.warn(
                    "Could not read learned mod ids ${file.absolutePath}; starting with none, " +
                        "which costs downloads rather than correctness: ${it.message}"
                )
            }
            .getOrDefault(emptyMap())
    }

    /**
     * Write the whole document, then move it over [file], so an interrupted write cannot corrupt it.
     *
     * Failures are logged and swallowed: this runs from a grind worker that has just learned something, and
     * an unwritable disk must cost the memory of it rather than the boot in progress.
     */
    @Synchronized
    private fun persist() {
        runCatching {
            file.parentFile?.mkdirs()
            val tmp = File(file.parentFile, "${file.name}.tmp")
            mapper.writerWithDefaultPrettyPrinter().writeValue(tmp, ids.snapshot().toSortedMap())
            try {
                Files.move(
                    tmp.toPath(), file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        }.onFailure { log.warn("Could not write learned mod ids ${file.absolutePath}: ${it.message}") }
    }
}
