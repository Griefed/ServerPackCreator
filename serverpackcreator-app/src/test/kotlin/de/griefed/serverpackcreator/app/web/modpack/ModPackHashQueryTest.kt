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
package de.griefed.serverpackcreator.app.web.modpack

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.data.repository.query.parser.PartTree
import java.util.Optional

/**
 * Guards that the upload duplicate-check survives two stored modpacks sharing a hash.
 *
 * The scan this replaced threw `StorageException` on the **first** match and so tolerated any number of
 * duplicates. A derived query declared to return `Optional<T>` does not: Spring Data raises
 * `IncorrectResultSizeDataAccessException` when more than one document matches, and `ModPackController`
 * catches only `StorageException` — so a duplicate pair turns every later upload of that hash into a 500
 * rather than the intended error response. Duplicates are reachable through a race between two concurrent
 * uploads of one file, and through any database predating the check.
 *
 * The `First` keyword is what prevents it, by limiting the query server-side. Asserted through Spring
 * Data's own `PartTree` — the parser that turns the method name into a query — rather than by matching the
 * name against a string, because the name is only a symptom of the property that matters.
 */
internal class ModPackHashQueryTest {

    /** The repository's hash finder, whatever it ends up being called. */
    private fun hashFinder() = ModPackRepository::class.java.methods
        .filter { it.name.contains("Sha256") }
        .also { Assertions.assertEquals(1, it.size, "Expected exactly one hash finder, found: $it") }
        .single()

    /**
     * Pins that the hash lookup can never see more than one result, so it cannot raise
     * `IncorrectResultSizeDataAccessException` however many duplicates the collection holds.
     */
    @Test
    fun theHashLookupIsLimitedToOneResult() {
        val finder = hashFinder()
        val query = PartTree(finder.name, ModPack::class.java)

        Assertions.assertTrue(
            query.isLimiting,
            "${finder.name} must limit its result set — an unlimited derived query returning Optional " +
                    "throws IncorrectResultSizeDataAccessException on a duplicate hash"
        )
        Assertions.assertEquals(
            1, query.maxResults,
            "${finder.name} must be limited to exactly one result"
        )
    }

    /**
     * Pins that the finder still hands back an `Optional`, since "no duplicate" has to stay expressible.
     * A limit alone would be satisfied by a `List`-returning finder, which every caller would then have to
     * unpack.
     */
    @Test
    fun theHashLookupStillReportsAbsence() {
        Assertions.assertEquals(
            Optional::class.java, hashFinder().returnType,
            "The hash finder reports absence with an empty Optional"
        )
    }
}
