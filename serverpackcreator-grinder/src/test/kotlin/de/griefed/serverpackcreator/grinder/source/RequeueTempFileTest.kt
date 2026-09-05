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

import de.griefed.serverpackcreator.grinder.GrindCandidate
import de.griefed.serverpackcreator.grinder.ModPlatforms
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Pins that a failed queue write leaves no scratch behind.
 *
 * `write` creates a temp file, fills it, then moves it over the queue — and wraps the lot in `runCatching`,
 * because a queueing problem must never stop a grind. The swallow is right; what was missing is that a
 * failure between "created" and "moved" left the temp file where it fell. Unlike `JsonVerdictStore`, which
 * writes to a fixed `<name>.tmp` and therefore overwrites its own debris, this one asks for a **fresh random
 * name every call**, so repeated failures accumulate one file each, forever, in a directory the reaper is
 * deliberately not entitled to touch (the queue is operator-authored state, not scratch).
 *
 * The failure is provoked the only way that is portable: make the destination a **directory**, so the move
 * cannot replace it. No permission games, no root-only setup, and it fails identically everywhere.
 */
internal class RequeueTempFileTest {

    private fun candidate(slug: String) =
        GrindCandidate("https://modrinth.com/mod/$slug", slug, 0, ModPlatforms.MODRINTH)

    /** Everything in the queue directory that is not the queue itself. */
    private fun strayFiles(directory: File, queue: File): List<String> =
        directory.listFiles().orEmpty().filter { it != queue }.map { it.name }

    /** **The defect.** A write that cannot complete must not leave its scratch file behind. */
    @Test
    fun aFailedWriteLeavesNoTemporaryFile(@TempDir directory: File) {
        // A directory where the queue file belongs: the move can never replace it, so `write` always fails.
        val queue = File(directory, "requeue.json").apply { mkdirs() }
        val store = JsonRequeueStore(queue)

        repeat(3) { attempt -> store.add(listOf(candidate("mod$attempt"))) }

        Assertions.assertEquals(
            emptyList<String>(), strayFiles(directory, queue),
            "each failed write left its temp file behind, in a directory nothing ever cleans"
        )
    }

    /** A failing queue must still not throw — that contract is why the debris went unnoticed. */
    @Test
    fun aFailedWriteStillDoesNotThrow(@TempDir directory: File) {
        val queue = File(directory, "requeue.json").apply { mkdirs() }

        Assertions.assertDoesNotThrow { JsonRequeueStore(queue).add(listOf(candidate("jei"))) }
    }

    /** The successful path is unchanged: the queue is written and no scratch survives it. */
    @Test
    fun aSuccessfulWriteLeavesOnlyTheQueue(@TempDir directory: File) {
        val queue = File(directory, "requeue.json")
        val store = JsonRequeueStore(queue)

        Assertions.assertEquals(1, store.add(listOf(candidate("jei"))))
        Assertions.assertEquals(1, store.pending())
        Assertions.assertEquals(emptyList<String>(), strayFiles(directory, queue))
    }
}
