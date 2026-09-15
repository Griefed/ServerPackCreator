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
 * Tests for [RunConfigurationListMigration]'s rewrite of a stored run-configuration.
 *
 * The rewrite is **join-free**, which is the whole reason this migration is cheap and safe: a stored
 * entry is a `DBRef` whose `$id` *is* the value, because the referenced documents held nothing but
 * their own `@MongoId`. So `{$ref: "clientMod", $id: "OptiFine"}` becomes `"OptiFine"` without reading
 * the referenced collection at all.
 *
 * Only the per-document transformation is exercised here — no Mongo. The surrounding component is a
 * loop over a collection and a save, which needs a live database to say anything about.
 */
internal class RunConfigurationListMigrationTest {

    private val migration = RunConfigurationListMigration()

    /** A stored document holding the three list fields as given. */
    private fun storedConfig(startArgs: Any?, clientMods: Any?, whitelistedMods: Any?): Document {
        val document = Document()
        document["_id"] = "config-1"
        document["minecraftVersion"] = "1.20.1"
        if (startArgs != null) document["startArgs"] = startArgs
        if (clientMods != null) document["clientMods"] = clientMods
        if (whitelistedMods != null) document["whitelistedMods"] = whitelistedMods
        return document
    }

    /**
     * Pins the rewrite: every `DBRef` element becomes the string it was keyed by, in order.
     */
    @Test
    fun dbRefElementsBecomeTheirIds() {
        val document = storedConfig(
            startArgs = listOf(DBRef("startArgument", "-Xmx4G"), DBRef("startArgument", "-Xms4G")),
            clientMods = listOf(DBRef("clientMod", "OptiFine"), DBRef("clientMod", "JourneyMap")),
            whitelistedMods = listOf(DBRef("whitelistedMod", "jei"))
        )

        Assertions.assertTrue(migration.needsRewrite(document), "precondition: this document is in the old shape")
        val rewritten = migration.rewrite(document)

        Assertions.assertEquals(listOf("-Xmx4G", "-Xms4G"), rewritten["startArgs"])
        Assertions.assertEquals(listOf("OptiFine", "JourneyMap"), rewritten["clientMods"])
        Assertions.assertEquals(listOf("jei"), rewritten["whitelistedMods"])
        Assertions.assertEquals("1.20.1", rewritten["minecraftVersion"], "untouched fields must survive")
        Assertions.assertEquals("config-1", rewritten["_id"])
    }

    /**
     * Pins idempotency: a document already holding plain strings is reported as needing nothing, and
     * rewriting it anyway changes nothing. A migration that runs on every startup must be safe to
     * re-run.
     */
    @Test
    fun anAlreadyMigratedDocumentIsLeftAlone() {
        val document = storedConfig(
            startArgs = listOf("-Xmx4G"),
            clientMods = listOf("OptiFine"),
            whitelistedMods = listOf("jei")
        )

        Assertions.assertFalse(migration.needsRewrite(document), "New-shape documents need no rewrite")
        val rewritten = migration.rewrite(document)
        Assertions.assertEquals(listOf("-Xmx4G"), rewritten["startArgs"])
        Assertions.assertEquals(listOf("OptiFine"), rewritten["clientMods"])
        Assertions.assertEquals(listOf("jei"), rewritten["whitelistedMods"])
    }

    /**
     * Pins that a mixed document — half rewritten, as an interrupted migration would leave it — is
     * completed rather than corrupted.
     */
    @Test
    fun aPartiallyMigratedDocumentIsCompleted() {
        val document = storedConfig(
            startArgs = listOf("-Xmx4G"),
            clientMods = listOf(DBRef("clientMod", "OptiFine")),
            whitelistedMods = listOf("jei")
        )

        Assertions.assertTrue(migration.needsRewrite(document), "One old-shape list is enough to need a rewrite")
        val rewritten = migration.rewrite(document)
        Assertions.assertEquals(listOf("-Xmx4G"), rewritten["startArgs"])
        Assertions.assertEquals(listOf("OptiFine"), rewritten["clientMods"])
    }

    /**
     * Pins that absent and empty lists are handled: a configuration that never had a list, or had an
     * empty one, must not gain junk or trip the rewrite.
     */
    @Test
    fun absentAndEmptyListsAreHandled() {
        val empty = storedConfig(startArgs = emptyList<Any>(), clientMods = null, whitelistedMods = null)

        Assertions.assertFalse(migration.needsRewrite(empty), "Nothing to rewrite in empty or absent lists")
        val rewritten = migration.rewrite(empty)
        Assertions.assertEquals(emptyList<String>(), rewritten["startArgs"])
        Assertions.assertNull(rewritten["clientMods"], "An absent list must not be invented")
    }

    /**
     * Pins that a fresh install is a no-op: nothing in the collection is in the old shape, so no
     * document is reported as needing a rewrite.
     */
    @Test
    fun aFreshInstallNeedsNoRewrite() {
        Assertions.assertFalse(migration.needsRewrite(Document()), "An empty document needs no rewrite")
    }
}
