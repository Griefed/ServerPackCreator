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

import de.griefed.serverpackcreator.clientside.Verdict
import de.griefed.serverpackcreator.grinder.GrindVerdict
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.Duration
import java.time.Instant

/**
 * Coalesced persistence: `record()` buffers, and the file is written on an interval instead of once per
 * verdict.
 *
 * Measured 2026-08-29 (`StoreWriteBenchTest`, B35): `persist()` serialises the whole store, so a `record()`
 * costs 18–25 ms at 1 k rows, 74–83 ms at 10 k and 787–1050 ms at 100 k. It is `@Synchronized` on the grind
 * worker's thread, and the deployed store held 38 258 verdicts at ~4.3 verdicts/second — so every worker was
 * serialising behind a multi-megabyte rewrite per verdict. Dropping the pretty-printer was measured and is
 * *not* enough (707 ms → 361 ms at 100 k); only writing less often is.
 *
 * The durability trade is deliberate and bounded: at most one interval of verdicts can be lost to a hard
 * kill, and a lost verdict is re-derived by the re-verify TTL. Nothing about the file format, the
 * pretty-printing or the atomic move changes.
 */
internal class CoalescedVerdictWritesTest {

    private fun verdict(slug: String) = GrindVerdict(
        "Modrinth", slug, "https://modrinth.com/mod/$slug", "Forge", "$slug-",
        "Forge 47.2.0 / Minecraft 1.20.1 -> SURVIVED", Instant.parse("2026-08-29T00:00:00Z")
    )

    /** Reload through a plain write-through store, which is what a restart actually does. */
    private fun reload(file: File) = JsonVerdictStore(file).all().map { it.slug }.toSet()

    /**
     * The point of the change: a buffered store must not touch the disk per verdict. Asserted on the file
     * never appearing at all, which is unambiguous — a size or mtime comparison would race the flusher.
     */
    @Test
    fun aCoalescedStoreDoesNotWriteOnEveryRecord(@TempDir dir: File) {
        val file = File(dir, "verdicts.json")
        JsonVerdictStore(file, flushInterval = Duration.ofHours(1)).use { store ->
            store.record(verdict("alpha"))
            store.record(verdict("beta"))

            Assertions.assertFalse(
                file.exists(),
                "a coalesced store rewrote the whole file per record(), which is the cost this exists to remove"
            )
            Assertions.assertEquals(
                setOf("alpha", "beta"), store.all().map { it.slug }.toSet(),
                "buffered verdicts must still be visible in memory — the report reads through all()"
            )
        }
    }

    /** An explicit flush writes everything buffered since the last one. */
    @Test
    fun flushWritesEverythingBuffered(@TempDir dir: File) {
        val file = File(dir, "verdicts.json")
        JsonVerdictStore(file, flushInterval = Duration.ofHours(1)).use { store ->
            store.record(verdict("alpha"))
            store.record(verdict("beta"))
            store.flush()

            Assertions.assertEquals(setOf("alpha", "beta"), reload(file))
        }
    }

    /**
     * The data-loss guard. `close()` is what the shutdown hook calls, and without a flush there every verdict
     * ground since the last tick would be lost on a normal, orderly stop.
     */
    @Test
    fun closeFlushesWhatIsStillBuffered(@TempDir dir: File) {
        val file = File(dir, "verdicts.json")
        val store = JsonVerdictStore(file, flushInterval = Duration.ofHours(1))
        store.record(verdict("alpha"))
        store.close()

        Assertions.assertEquals(
            setOf("alpha"), reload(file),
            "a verdict recorded before an orderly shutdown must survive it"
        )
    }

    /** The interval must actually fire on its own — nobody calls flush() during a grind. */
    @Test
    fun theScheduledFlusherWritesWithoutBeingAsked(@TempDir dir: File) {
        val file = File(dir, "verdicts.json")
        JsonVerdictStore(file, flushInterval = Duration.ofMillis(50)).use { store ->
            store.record(verdict("alpha"))

            val deadline = System.currentTimeMillis() + 10_000
            while (System.currentTimeMillis() < deadline && !file.exists()) {
                Thread.sleep(25)
            }
            Assertions.assertEquals(
                setOf("alpha"), reload(file),
                "the scheduled flusher never wrote; a crash would then lose everything since startup"
            )
        }
    }

    /**
     * The default is unchanged, deliberately. Coalescing is opt-in at the composition root so that every
     * existing caller — and every existing test — keeps the write-through durability it was written against.
     */
    @Test
    fun aWriteThroughStoreStillPersistsImmediately(@TempDir dir: File) {
        val file = File(dir, "verdicts.json")
        JsonVerdictStore(file).record(verdict("alpha"))

        Assertions.assertEquals(
            setOf("alpha"), reload(file),
            "the default store must still write on every record()"
        )
    }
}
