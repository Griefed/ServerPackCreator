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
package de.griefed.serverpackcreator.plugin.grinder.core

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Pins the merge that decides what a server pack ends up excluding. It is a pure function over two lists
 * precisely so it can be pinned this hard: everything around it — an HTTP fetch, a Swing table, a pf4j
 * extension point — is scenery, and this is the only place where getting it wrong silently removes mods
 * from somebody's server pack or silently fails to.
 */
internal class ClientsideEntryInjectorTest {

    /** The ordinary case: the user's own list first, the ticked entries appended after it. */
    @Test
    fun appendsTheSelectionAfterWhatTheUserConfigured() {
        Assertions.assertEquals(
            listOf("my-own-entry", "another-of-mine", "creativecore-", "jei-"),
            ClientsideEntryInjector.inject(
                existing = listOf("my-own-entry", "another-of-mine"),
                selected = listOf("creativecore-", "jei-")
            )
        )
    }

    /**
     * Order is preserved rather than sorted. The exclusion list is applied in sequence and a user's own
     * ordering is a thing they can reason about; re-sorting it would be a change this plugin has no
     * business making to a list it only meant to add to.
     */
    @Test
    fun leavesTheExistingOrderAlone() {
        val existing = listOf("zzz-", "aaa-", "mmm-")
        Assertions.assertEquals(existing, ClientsideEntryInjector.inject(existing, emptyList()).take(3))
    }

    /**
     * An entry the user already configured is not added twice, and the comparison ignores case because
     * SPC's own matching does. A duplicate is not merely untidy — the exclusion list is shown back to the
     * user in the GUI field, and growing it by a copy of itself on every generation is how that field
     * ends up thousands of lines long.
     */
    @Test
    fun doesNotAddWhatIsAlreadyThere() {
        Assertions.assertEquals(
            listOf("CreativeCore-", "jei-"),
            ClientsideEntryInjector.inject(
                existing = listOf("CreativeCore-"),
                selected = listOf("creativecore-", "jei-")
            )
        )
    }

    /** A selection repeating itself contributes one entry, whatever case its duplicates arrived in. */
    @Test
    fun collapsesDuplicatesWithinTheSelection() {
        Assertions.assertEquals(
            listOf("creativecore-"),
            ClientsideEntryInjector.inject(emptyList(), listOf("creativecore-", "CREATIVECORE-", "creativecore-"))
        )
    }

    /**
     * Nothing ticked means nothing changes. This is the state every user is in until they use the plugin,
     * so it is the case that must not surprise anyone.
     */
    @Test
    fun isANoOpWhenNothingIsSelected() {
        val existing = listOf("my-own-entry")
        Assertions.assertEquals(existing, ClientsideEntryInjector.inject(existing, emptySet()))
    }

    /**
     * Blank entries never reach the list. An empty string matches every mod name under SPC's
     * `startsWith` and `contains` filters, so one slipping through would exclude the entire mods
     * directory — refused here as well as in [SelectionStore], because this function is also reachable
     * from a hand-edited config.
     */
    @Test
    fun refusesBlankEntriesFromEitherSide() {
        Assertions.assertEquals(
            listOf("mine-", "creativecore-"),
            ClientsideEntryInjector.inject(
                existing = listOf("mine-", "", "   "),
                selected = listOf("creativecore-", "  ")
            )
        )
    }

    /** Entries are trimmed, so a stray space in a config file does not create a near-duplicate. */
    @Test
    fun trimsBeforeComparing() {
        Assertions.assertEquals(
            listOf("creativecore-"),
            ClientsideEntryInjector.inject(existing = listOf("  creativecore-  "), selected = listOf("creativecore-"))
        )
    }
}
