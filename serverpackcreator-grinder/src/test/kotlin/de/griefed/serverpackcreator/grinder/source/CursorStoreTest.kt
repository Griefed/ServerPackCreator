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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

/**
 * Pins the crawl-position stores. The in-memory one is the test double and defines the contract (unknown
 * source ⇒ start of catalog, sweep count carried alongside the offset); the JSON one adds the property the
 * whole unattended-crawl design rests on — **the position survives a restart**, so a multi-week crawl
 * resumes where it stopped instead of starting over at the most-downloaded mods.
 */
internal class CursorStoreTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun anUnknownSourceStartsAtTheTopOfTheCatalog() {
        val store = InMemoryCursorStore()

        Assertions.assertEquals(CatalogCursor(offset = 0, sweeps = 0), store.cursor("Modrinth"))
    }

    @Test
    fun storesAndReadsBackACursorPerSource() {
        val store = InMemoryCursorStore()
        store.store("Modrinth", CatalogCursor(offset = 2_500, sweeps = 1))
        store.store("CurseForge", CatalogCursor(offset = 400, sweeps = 3))

        Assertions.assertEquals(CatalogCursor(2_500, 1), store.cursor("Modrinth"))
        Assertions.assertEquals(CatalogCursor(400, 3), store.cursor("CurseForge"))
        Assertions.assertEquals(CatalogCursor(0, 0), store.cursor("Unknown"))
    }

    @Test
    fun theJsonStoreSurvivesAReopen() {
        val file = File(tempDir.toFile(), "cursors.json")
        JsonCursorStore(file).store("Modrinth", CatalogCursor(offset = 12_300, sweeps = 2))

        val reopened = JsonCursorStore(file)

        Assertions.assertEquals(CatalogCursor(12_300, 2), reopened.cursor("Modrinth"))
        Assertions.assertTrue(file.isFile, "the cursor file must be written eagerly, not on shutdown")
    }

    @Test
    fun theJsonStoreCreatesItsFileAndParentDirectories() {
        val file = File(tempDir.toFile(), "nested/deeper/cursors.json")
        JsonCursorStore(file).store("Modrinth", CatalogCursor(offset = 50, sweeps = 0))

        Assertions.assertTrue(file.isFile, "expected ${file.absolutePath} to exist")
    }

    /** A later write must replace the earlier position, not accumulate rows. */
    @Test
    fun theJsonStoreReplacesAPositionRatherThanAppending() {
        val file = File(tempDir.toFile(), "cursors.json")
        val store = JsonCursorStore(file)
        store.store("Modrinth", CatalogCursor(offset = 100, sweeps = 0))
        store.store("Modrinth", CatalogCursor(offset = 200, sweeps = 0))

        Assertions.assertEquals(CatalogCursor(200, 0), JsonCursorStore(file).cursor("Modrinth"))
    }

    /**
     * A corrupt cursor file must not stop the service: losing the crawl position costs a re-sweep (cheap —
     * fresh verdicts are skipped), while crashing on start costs the whole daemon.
     */
    @Test
    fun aCorruptFileStartsFromTheTopInsteadOfCrashing() {
        val file = File(tempDir.toFile(), "cursors.json").apply { writeText("{not json at all") }

        val store = JsonCursorStore(file)

        Assertions.assertEquals(CatalogCursor(0, 0), store.cursor("Modrinth"))
    }

    /** A partitioned source's traversal token has to survive a restart too, or its crawl restarts at the top. */
    @Test
    fun theJsonStoreRoundTripsAPartitionToken() {
        val file = File(tempDir.toFile(), "cursors.json")
        JsonCursorStore(file).store("CurseForge", CatalogCursor(offset = 350, sweeps = 1, partition = "1.20.1|1|desc"))

        Assertions.assertEquals(
            CatalogCursor(offset = 350, sweeps = 1, partition = "1.20.1|1|desc"),
            JsonCursorStore(file).cursor("CurseForge")
        )
    }

    /**
     * A cursor file written before partitioning existed has no `partition` key. It must load as "start of the
     * plan" rather than failing the whole file, which would silently reset every platform's crawl position.
     */
    @Test
    fun aCursorFileWithoutAPartitionKeyStillLoads() {
        val file = File(tempDir.toFile(), "cursors.json").apply {
            writeText("""{"Modrinth":{"offset":12300,"sweeps":2}}""")
        }

        val store = JsonCursorStore(file)

        Assertions.assertEquals(CatalogCursor(offset = 12_300, sweeps = 2, partition = null), store.cursor("Modrinth"))
    }

    @Test
    fun aNegativeOffsetOrSweepCountIsRejected() {
        Assertions.assertThrows(IllegalArgumentException::class.java) { CatalogCursor(offset = -1, sweeps = 0) }
        Assertions.assertThrows(IllegalArgumentException::class.java) { CatalogCursor(offset = 0, sweeps = -1) }
    }
}
