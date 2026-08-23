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
import de.griefed.serverpackcreator.grinder.GrindCandidate
import de.griefed.serverpackcreator.grinder.ModPlatforms
import de.griefed.serverpackcreator.grinder.GrindVerdict
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Instant

/**
 * The **immediate re-grind queue**: projects that jump ahead of the catalog crawl and are re-verified
 * regardless of how fresh their verdict is.
 *
 * **Why the crawl and the TTL are not enough.** Together they answer "when does a project come round again?"
 * with *eventually, at the re-verify TTL* — the right answer when a mod changes, and the wrong one when the
 * defect is in this engine. Three landed on 2026-08-23 alone (a source jar becoming a list-entry, a crash
 * re-check that never left the crashing combination's neighbourhood, two platform runs of one slug sharing a
 * staging directory), and each invalidated verdicts that were already published. Waiting out a 30-day TTL
 * means publishing a known-wrong clientside entry for a month.
 *
 * A queued grind is **forced** past the freshness check by the caller — see `Grinder.grind`'s `force`. That
 * is not an optional detail: verdicts get queued because they are wrong, and a wrong verdict is usually a
 * recent one, so an unforced drain would turn straight into `SKIPPED_FRESH`.
 *
 * @author Griefed
 */
interface RequeueStore {
    /**
     * Queue [candidates], skipping any already waiting, and return how many were newly added.
     *
     * Identity is the platform plus the project's own id where one is known, falling back to the slug: a slug
     * is a mutable display name, so a project queued under its old name and again under its new one is one
     * re-grind, while the same slug on the other platform is a different project and queues separately.
     */
    fun add(candidates: Collection<GrindCandidate>): Int

    /** Take everything waiting and clear the queue, so a pass grinds each entry once rather than forever. */
    fun drain(): List<GrindCandidate>

    /** How many entries are waiting — for `/status`, so a backlog is visible rather than inferred. */
    fun pending(): Int
}

/**
 * File-backed [RequeueStore]. On disk because **the tool that queues and the daemon that drains are different
 * processes**: an operator queues work against a service that is already running, so an in-memory queue would
 * be empty in the one process that matters. A corrupt or half-written file reads as empty and is logged,
 * never thrown — a hand-edited queue must not stop the service starting.
 *
 * Writes replace the file atomically where the filesystem allows it, so a daemon draining while an operator
 * queues sees one state or the other and never a truncated document. The two processes are not otherwise
 * synchronised: a queue-and-drain landing in the same instant can lose or repeat an entry, and both are
 * harmless — a repeat is one extra re-verification, and a loss is re-queued by running the command again.
 *
 * @param file The JSON document backing the queue; its parent directory is created on first write.
 * @author Griefed
 */
class JsonRequeueStore(private val file: File) : RequeueStore {

    private val log by lazy { cachedLoggerOf(this.javaClass) }
    private val mapper = jacksonObjectMapper()

    @Synchronized
    override fun add(candidates: Collection<GrindCandidate>): Int {
        val waiting = read()
        val known = waiting.mapTo(HashSet()) { it.requeueKey() }
        val fresh = candidates.filter { known.add(it.requeueKey()) }
        if (fresh.isNotEmpty()) {
            write(waiting + fresh)
        }
        return fresh.size
    }

    @Synchronized
    override fun drain(): List<GrindCandidate> {
        val waiting = read()
        if (waiting.isNotEmpty()) {
            write(emptyList())
        }
        return waiting
    }

    @Synchronized
    override fun pending(): Int = read().size

    /** Read the queue, treating anything unreadable as empty so a bad file cannot stop a grind. */
    private fun read(): List<GrindCandidate> {
        if (!file.isFile) {
            return emptyList()
        }
        return runCatching { mapper.readValue<List<GrindCandidate>>(file) }
            .getOrElse {
                log.warn("Could not read the re-grind queue ${file.absolutePath}; treating it as empty: ${it.message}")
                emptyList()
            }
    }

    /** Persist [candidates], replacing the file atomically where the filesystem supports it. */
    private fun write(candidates: List<GrindCandidate>) {
        runCatching {
            file.parentFile?.mkdirs()
            val temporary = File.createTempFile("requeue", ".json", file.parentFile)
            temporary.writeText(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(candidates))
            try {
                Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (unsupported: AtomicMoveNotSupportedException) {
                log.debug("Atomic replace unavailable for ${file.absolutePath}: ${unsupported.message}")
                Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        }.onFailure { log.warn("Could not persist the re-grind queue ${file.absolutePath}: ${it.message}") }
    }

    /** Platform plus the project's stable id, or its slug when the id is unknown. */
    private fun GrindCandidate.requeueKey(): String = "$platform/${projectId ?: slug}"
}

/**
 * Selects what to queue, expressed over the verdict store rather than assembled by hand.
 *
 * @author Griefed
 */
object RequeueSelection {
    /**
     * Split [links] into candidates that can actually be ground and the links no platform resolves.
     *
     * **Rejecting here is the point.** `ModPlatforms.ofUrl` answers `Unknown` for anything that is neither
     * Modrinth nor CurseForge, and queueing that reports a cheerful success before failing hours later inside
     * the daemon, where `ClientsideVerifier.report` throws "No supported platform" into a log nobody is
     * reading. A typo belongs to the command that read it — the only moment somebody is watching.
     */
    fun fromLinks(links: Collection<String>): Pair<List<GrindCandidate>, List<String>> {
        val (resolvable, rejected) = links.partition { ModPlatforms.ofUrl(it) != ModPlatforms.UNKNOWN }
        return resolvable.map {
            GrindCandidate(it, slugFromUrl(it), 0, ModPlatforms.ofUrl(it))
        } to rejected
    }

    /**
     * Best-effort project-slug from a URL: the last path segment, query stripped. A display name for the log
     * and the report — identity is the platform's own id where one is known.
     */
    private fun slugFromUrl(url: String): String =
        url.substringBefore('?').trimEnd('/').substringAfterLast('/').ifBlank { url }

    /**
     * Every project whose verdict was recorded **before** [instant], as one candidate each.
     *
     * This is the recurring shape and the reason the selector exists: a defect is found in the engine, and
     * everything verified before the fix landed is suspect. Naming the moment is both precise and auditable —
     * an operator can say "re-grind everything from before the fix" without listing hundreds of projects, and
     * a reader of the log can tell exactly which population was re-verified and why.
     *
     * One candidate per *project*, not per verdict row: a project carries one verdict per loader and a
     * re-grind re-verifies all of them, so queueing per row would boot the same pack several times.
     * Popularity is zero because the queue is not ordered by it — it is ordered by "we know this is wrong".
     */
    fun verifiedBefore(verdicts: Collection<GrindVerdict>, instant: Instant): List<GrindCandidate> =
        verdicts.filter { it.verifiedAt.isBefore(instant) }
            .groupBy { it.platform to (it.projectId ?: it.slug) }
            .map { (_, rows) ->
                val newest = rows.maxBy { it.verifiedAt }
                GrindCandidate(newest.projectUrl, newest.slug, 0, newest.platform, newest.projectId)
            }
}
