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

import de.griefed.serverpackcreator.clientside.Confidence
import de.griefed.serverpackcreator.grinder.grindVerdict
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Pins the in-memory verdict store: a re-verified `(slug, loader)` replaces rather than duplicates,
 * distinct loaders of one project coexist, and [VerdictStore.hasVerdictFor] drives the skip-already-done
 * check.
 */
internal class VerdictStoreTest {

    @Test
    fun reVerifyingSameProjectAndLoaderReplacesTheVerdict() {
        val store = InMemoryVerdictStore()
        store.record(grindVerdict("jei", "Forge", confidence = Confidence.MEDIUM))
        store.record(grindVerdict("jei", "Forge", confidence = Confidence.HIGH))

        Assertions.assertEquals(1, store.all().size)
        Assertions.assertEquals(Confidence.HIGH, store.all().single().confidence)
    }

    @Test
    fun distinctLoadersOfOneProjectCoexist() {
        val store = InMemoryVerdictStore()
        store.record(grindVerdict("sodium", "Fabric"))
        store.record(grindVerdict("sodium", "Quilt"))

        Assertions.assertEquals(2, store.all().size)
        Assertions.assertTrue(store.hasVerdictFor("sodium"))
    }

    @Test
    fun hasVerdictForIsFalseUntilSomethingIsRecorded() {
        val store = InMemoryVerdictStore()
        Assertions.assertFalse(store.hasVerdictFor("rubidium"))
        store.record(grindVerdict("rubidium", "Forge"))
        Assertions.assertTrue(store.hasVerdictFor("rubidium"))
        Assertions.assertFalse(store.hasVerdictFor("something-else"))
    }
}
