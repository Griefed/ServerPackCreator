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
package de.griefed.serverpackcreator.app.web.migration

import com.mongodb.DBRef
import org.bson.Document
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Tests for [RunConfigurationListMigrationRunner]'s **safety decisions**, which is the half that can lose
 * a user's data if it is wrong.
 *
 * `RunConfigurationListMigrationTest` covers the per-document transformation; this covers the ordering and
 * failure handling around it: rewrite before dropping, never drop when nothing was rewritten, and keep the
 * application starting when the database will not cooperate. An audit found the runner untested — the
 * component that mutates persisted data was the only substantial one on the whole performance stack with no
 * coverage, precisely because its collaborator was `MongoTemplate` and therefore unobservable.
 */
internal class RunConfigurationListMigrationRunnerTest {

    /**
     * An in-memory [MigrationStore] recording what the runner asked of it, in order.
     *
     * @param failReplace Throw on the first `replace`, to model a database refusing a write mid-pass.
     * @param failDropOf Throw when this collection is dropped, to model one drop failing.
     */
    private class RecordingStore(
        private val documents: MutableList<Document>,
        private val existing: MutableSet<String> = mutableSetOf("startArgument", "clientMod", "whitelistedMod"),
        private val failReplace: Boolean = false,
        private val failDropOf: String? = null
    ) : MigrationStore {
        /** Every call, in order, so a test can assert that the rewrite preceded the drops. */
        val calls = mutableListOf<String>()

        /** Documents as they stand after the runner has finished. */
        val stored: List<Document> get() = documents

        override fun findAll(collection: String): List<Document> {
            calls.add("findAll($collection)")
            return documents.toList()
        }

        override fun replace(collection: String, id: Any?, replacement: Document) {
            calls.add("replace($id)")
            if (failReplace) {
                throw IllegalStateException("the database refused the write")
            }
            documents.replaceAll { if (it["_id"] == id) replacement else it }
        }

        override fun exists(collection: String) = existing.contains(collection)

        override fun drop(collection: String) {
            calls.add("drop($collection)")
            if (collection == failDropOf) {
                throw IllegalStateException("cannot drop $collection")
            }
            existing.remove(collection)
        }
    }

    /** A stored configuration in the old shape, with its lists as `DBRef`s. */
    private fun oldShape(id: String) = Document(
        mapOf(
            "_id" to id,
            "minecraftVersion" to "1.20.1",
            "startArgs" to listOf(DBRef("startArgument", "-Xmx4G")),
            "clientMods" to listOf(DBRef("clientMod", "OptiFine")),
            "whitelistedMods" to listOf(DBRef("whitelistedMod", "jei"))
        )
    )

    /** A stored configuration already migrated, with plain strings. */
    private fun newShape(id: String) = Document(
        mapOf(
            "_id" to id,
            "minecraftVersion" to "1.20.1",
            "startArgs" to listOf("-Xmx4G"),
            "clientMods" to listOf("OptiFine"),
            "whitelistedMods" to listOf("jei")
        )
    )

    private fun runnerOver(store: MigrationStore) =
        RunConfigurationListMigrationRunner(store, RunConfigurationListMigration())

    /**
     * Pins the load-bearing ordering: **every** rewrite happens before **any** drop.
     *
     * If a drop ran first the referenced ids would be gone and the configurations unrecoverable, since the
     * id is the only place the value is stored. Asserted on call order, not just on the end state, because
     * the end state looks identical either way.
     */
    @Test
    fun everyRewriteHappensBeforeAnyDrop() {
        val store = RecordingStore(mutableListOf(oldShape("a"), oldShape("b")))
        runnerOver(store).migrate()

        val firstDrop = store.calls.indexOfFirst { it.startsWith("drop(") }
        val lastReplace = store.calls.indexOfLast { it.startsWith("replace(") }
        Assertions.assertTrue(firstDrop > 0, "the orphaned collections must be dropped: ${store.calls}")
        Assertions.assertTrue(
            lastReplace < firstDrop,
            "every rewrite must precede every drop, was ${store.calls}"
        )
        Assertions.assertEquals(listOf("OptiFine"), store.stored.first()["clientMods"])
    }

    /**
     * Pins that a database with nothing to migrate is left completely alone — no writes, and crucially no
     * drops. A fresh install must not have collections removed on the strength of an empty pass.
     */
    @Test
    fun anAlreadyMigratedDatabaseIsNotTouched() {
        val store = RecordingStore(mutableListOf(newShape("a"), newShape("b")))
        runnerOver(store).migrate()

        Assertions.assertEquals(listOf("findAll(runConfiguration)"), store.calls)
    }

    /** Pins the same for an empty collection: a fresh install drops nothing. */
    @Test
    fun aFreshInstallDropsNothing() {
        val store = RecordingStore(mutableListOf())
        runnerOver(store).migrate()

        Assertions.assertTrue(store.calls.none { it.startsWith("drop(") }, "was ${store.calls}")
    }

    /**
     * Pins that a write failure mid-pass does **not** drop the orphaned collections.
     *
     * This is the case that would destroy data: a half-rewritten database whose referenced ids were then
     * deleted could not be finished on the next start.
     */
    @Test
    fun aFailedRewriteLeavesTheOrphanedCollectionsAlone() {
        val store = RecordingStore(mutableListOf(oldShape("a")), failReplace = true)

        Assertions.assertDoesNotThrow { runnerOver(store).migrate() }

        Assertions.assertTrue(
            store.calls.none { it.startsWith("drop(") },
            "a failed rewrite must not drop anything, was ${store.calls}"
        )
        Assertions.assertTrue(store.exists("clientMod"), "the referenced ids must survive a failed pass")
    }

    /**
     * Pins that one stubborn collection does not abort the others. Leaving an unused collection behind is
     * harmless; failing the migration over it is not.
     */
    @Test
    fun oneFailedDropDoesNotStopTheRest() {
        val store = RecordingStore(mutableListOf(oldShape("a")), failDropOf = "clientMod")

        Assertions.assertDoesNotThrow { runnerOver(store).migrate() }

        Assertions.assertEquals(
            listOf("startArgument", "clientMod", "whitelistedMod"),
            store.calls.filter { it.startsWith("drop(") }.map { it.removePrefix("drop(").removeSuffix(")") },
            "all three drops must be attempted"
        )
        Assertions.assertFalse(store.exists("whitelistedMod"), "the drop after the failing one must still run")
    }

    /**
     * Pins that an unreachable database does not stop the application from starting — the migration runs on
     * `ApplicationReadyEvent`, and throwing there would take the boot down with it.
     */
    @Test
    fun anUnreachableDatabaseDoesNotFailStartup() {
        val unreachable = object : MigrationStore {
            override fun findAll(collection: String): List<Document> = throw IllegalStateException("no database")
            override fun replace(collection: String, id: Any?, replacement: Document) = Unit
            override fun exists(collection: String) = false
            override fun drop(collection: String) = Unit
        }

        Assertions.assertDoesNotThrow { runnerOver(unreachable).migrate() }
    }

    /**
     * Pins that a mixed database — some documents migrated, some not, as an interrupted run leaves it — is
     * completed, and only the stale ones are written.
     */
    @Test
    fun onlyTheDocumentsStillInTheOldShapeAreRewritten() {
        val store = RecordingStore(mutableListOf(newShape("a"), oldShape("b"), newShape("c")))
        runnerOver(store).migrate()

        Assertions.assertEquals(
            listOf("replace(b)"),
            store.calls.filter { it.startsWith("replace(") },
            "only the stale document may be written, was ${store.calls}"
        )
    }
}
