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

import de.griefed.serverpackcreator.clientside.boot.AttemptDirectory
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File

/**
 * Reclaims the per-attempt staging space once a candidate's verdicts are in.
 *
 * Staging leaves a full server pack (with the overlaid loader libraries) plus the mod jars it downloaded under
 * `<work>/boot/<attempt>` and `<work>/verify/<attempt>`. Those directories are only ever deleted when the *same*
 * attempt is staged again, which for a catalog sweep is never — every attempt is a new tuple, so without this the
 * work tree grows without bound, measured live at **98 GB across 1750 attempt directories, ~23 GB/h**.
 *
 * **Nothing is kept here, the console included.** Retention belongs to the
 * [de.griefed.serverpackcreator.grinder.report.BootLogStore], which copies the consoles worth keeping *during*
 * the boots and holds them to a byte budget; a second copy here is both redundant and unbounded, because a
 * directory holding a kept file is never empty and so is never removed.
 *
 * @author Griefed
 */
class BootWorkspaceReaper(
    /** The grinder's scratch root — the same one [ContainerCandidateVerifier] stages under. */
    private val workDirectory: File
) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

    /**
     * Reclaim the staging of one finished candidate, across every loader it was attempted on. Scoped to the given
     * `(platform, slug)` on purpose: workers run in parallel, and deleting another candidate's directory would pull
     * the server out from under a container that is still booting it.
     *
     * **The platform is half of the scope, not decoration.** The same slug on Modrinth and on CurseForge is two
     * candidates — verdict freshness is keyed that way for the same reason — and they are ground concurrently.
     * Reaping by slug alone deletes the other platform's pack mid-boot, which surfaces as an exit-127 boot, or as
     * the same project reading SURVIVED on one platform and CRASHED on the other. Returns the bytes
     * freed, for the caller to log.
     */
    fun reap(platform: String, slug: String): Long {
        val owner = AttemptDirectory.ownerKey(platform, slug)
        return reapMatching("candidate '$platform/$slug'") { AttemptDirectory.ownerOf(it.name) == owner }
    }

    /**
     * Sweep every attempt directory found, for use at startup. A run killed mid-boot leaves staging that no
     * [reap] call will ever come for, so without this each crash leaks a pack permanently. Safe only when nothing
     * is in flight — at startup, nothing is. Returns the bytes freed, for the caller to log.
     */
    fun reapAll(): Long = reapMatching("previous run") { true }

    /** Apply [selects] to every attempt directory in both staging roots and reclaim the matches entirely. */
    private fun reapMatching(what: String, selects: (File) -> Boolean): Long {
        val reclaimed = STAGING_ROOTS
            .map { File(workDirectory, it) }
            .flatMap { root -> root.listFiles()?.filter { it.isDirectory && selects(it) } ?: emptyList() }
            .sumOf { reclaim(it) }
        if (reclaimed > 0) {
            log.debug("Reclaimed ${reclaimed / 1_048_576} MiB of staging for $what.")
        }
        return reclaimed
    }

    /**
     * Delete [attemptDir] and everything in it, returning the bytes known to have gone. Failures are logged and
     * swallowed — reclaiming disk must never fail a grind, and whatever survives is retried by the next sweep.
     *
     * Entry by entry rather than one [deleteRecursively] on the directory, so an entry that cannot be removed
     * contributes nothing to the total instead of making the whole attempt's bytes unreportable.
     */
    private fun reclaim(attemptDir: File): Long {
        var reclaimed = 0L
        for (entry in attemptDir.listFiles() ?: emptyArray()) {
            val size = entry.sizeRecursively()
            if (entry.deleteRecursively()) {
                reclaimed += size
            } else {
                log.warn("Could not reclaim ${entry.absolutePath}; it will be retried on the next sweep.")
            }
        }
        // The directory itself goes too. One surviving inode per attempt IS the unbounded growth this class
        // exists to prevent: 255,591 of them had accumulated on the live daemon by 2026-11-09, and startup
        // walks that tree before the report server can bind.
        if (attemptDir.listFiles()?.isEmpty() == true && !attemptDir.delete()) {
            log.warn("Could not remove ${attemptDir.absolutePath}; it will be retried on the next sweep.")
        }
        return reclaimed
    }

    /** Total size of a file, or of every file beneath a directory. */
    private fun File.sizeRecursively(): Long =
        if (isDirectory) walkTopDown().filter { it.isFile }.sumOf { it.length() } else length()

    /** The staging-directory layout constants the reaper matches on. */

    companion object {
        /** The two staging roots, relative to the work directory: boot packs and downloaded mod jars. */
        private val STAGING_ROOTS = listOf("boot", "verify")
    }
}
