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

import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File

/**
 * Reclaims the per-attempt staging space once a candidate's verdicts are in, keeping only the boot logs.
 *
 * Staging leaves a full server pack (with the overlaid loader libraries) plus the mod jars it downloaded under
 * `<work>/boot/<slug>-<loader>` and `<work>/verify/<slug>-<loader>`. Those directories were only ever deleted when
 * the *same* `(slug, loader)` pair was attempted again, which for a catalog sweep is never — every attempt was a
 * new pair, so the work tree grew without bound: **98 GB across 1750 attempt directories, ~23 GB/h**, measured
 * live on 2026-07-30. The packs are fully reproducible from the cache and the source; the `boot.log` is not, and a
 * verdict's detail is read from it, so that one file stays.
 *
 * @param workDirectory The grinder's scratch root — the same one [ContainerCandidateVerifier] stages under.
 * @author Griefed
 */
class BootWorkspaceReaper(private val workDirectory: File) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

    /**
     * Reclaim the staging of one finished candidate, across every loader it was attempted on. Scoped to the given
     * slug on purpose: workers run in parallel, and deleting another candidate's directory would pull the server
     * out from under a container that is still booting it.
     *
     * @return Bytes freed, for the caller to log.
     */
    fun reap(slug: String): Long = reapMatching("candidate '$slug'") { it.attemptSlug() == slug }

    /**
     * Sweep every attempt directory found, for use at startup. A run killed mid-boot leaves staging that no
     * [reap] call will ever come for, so without this each crash leaks a pack permanently. Safe only when nothing
     * is in flight — at startup, nothing is.
     *
     * @return Bytes freed, for the caller to log.
     */
    fun reapAll(): Long = reapMatching("previous run") { true }

    /** Apply [selects] to every attempt directory in both staging roots and strip the matches down to their logs. */
    private fun reapMatching(what: String, selects: (File) -> Boolean): Long {
        val reclaimed = STAGING_ROOTS
            .map { File(workDirectory, it) }
            .flatMap { root -> root.listFiles()?.filter { it.isDirectory && selects(it) } ?: emptyList() }
            .sumOf { stripToLogs(it) }
        if (reclaimed > 0) {
            log.debug("Reclaimed ${reclaimed / 1_048_576} MiB of staging for $what.")
        }
        return reclaimed
    }

    /**
     * Delete everything in [attemptDir] except the boot log, removing the directory itself when no log was kept
     * (the downloaded-jar scratch has none). Failures are logged and swallowed — reclaiming disk must never fail a
     * grind, and whatever survives is retried by the next sweep.
     */
    private fun stripToLogs(attemptDir: File): Long {
        var reclaimed = 0L
        for (entry in attemptDir.listFiles() ?: emptyArray()) {
            if (entry.name == KEPT_LOG) {
                continue
            }
            val size = entry.sizeRecursively()
            if (entry.deleteRecursively()) {
                reclaimed += size
            } else {
                log.warn("Could not reclaim ${entry.absolutePath}; it will be retried on the next sweep.")
            }
        }
        if (attemptDir.listFiles()?.isEmpty() == true) {
            attemptDir.delete()
        }
        return reclaimed
    }

    /**
     * The candidate slug an attempt directory belongs to. Directories are named `<slug>-<loader>`, and slugs nest
     * (`jei` vs `jei-extras`), so the loader suffix is cut off rather than the slug prefix-matched — a prefix match
     * would reap a different, possibly in-flight, project.
     */
    private fun File.attemptSlug(): String = name.substringBeforeLast('-')

    /** Total size of a file, or of every file beneath a directory. */
    private fun File.sizeRecursively(): Long =
        if (isDirectory) walkTopDown().filter { it.isFile }.sumOf { it.length() } else length()

    companion object {
        /** The two staging roots, relative to the work directory: boot packs and downloaded mod jars. */
        private val STAGING_ROOTS = listOf("boot", "verify")

        /** The one file worth keeping: a verdict's detail is read from the boot console. */
        const val KEPT_LOG = "boot.log"
    }
}
