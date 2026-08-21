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
package de.griefed.serverpackcreator.app.gui.window.configs.components

import de.griefed.serverpackcreator.app.gui.GuiProps
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import javax.swing.JTextArea

/**
 * Tests for [SuggestionProvider]'s suggestion source.
 *
 * The autocomplete list is re-read and re-parsed on **every keystroke**, on the EDT: the provider's
 * document-listener fires per document event and walks the whole list. For the clientside-mods field
 * that list is the ~550-entry fallback, so every character typed cost a property read, a `split(",")`
 * and 550 sorted inserts into a fresh `TreeSet` before a single suggestion could be shown.
 *
 * Only the suggestion *source* is exercised here — no popup is shown, so this stays headless. The
 * provider is constructed against a real `JTextArea` because it registers listeners on one; nothing
 * is realised on screen.
 */
internal class SuggestionProviderTest {

    /** A comma-separated autocomplete property standing in for the shipped clientside-mods list. */
    private val entries = (1..550).map { "mod-$it" }.joinToString(",")

    private val guiProps = mockk<GuiProps>()

    /** A provider reading `autocomplete.clientmods`, backed by [entries]. */
    private fun provider(): SuggestionProvider {
        every { guiProps.getGuiProperty("autocomplete.clientmods") } returns entries
        every { guiProps.getGuiProperty("autocomplete.limit") } returns "10"
        return SuggestionProvider(guiProps, JTextArea(), "clientmods")
    }

    /**
     * Pins that an unchanged autocomplete property is parsed **once**, however often it is queried.
     *
     * Asserted by identity, because reuse is otherwise invisible: [SuggestionProvider.allSuggestions]
     * owes its callers a fresh copy, so equal-but-distinct sets would be returned either way. The same
     * instance coming back proves the `split(",")` and the 550 sorted inserts did not run again.
     *
     * The property read itself is deliberately *not* cached — hence `exactly = 20`, one per query. It is
     * a map lookup, and keying the memo on the raw value is what lets a saved suggestion-list be picked
     * up immediately without a change-listener. Rebuilding the set was the cost, not reading the string.
     */
    @Test
    fun anUnchangedSuggestionListIsParsedOnce() {
        val suggestionProvider = provider()
        val first = suggestionProvider.parsedSuggestions()
        repeat(19) {
            Assertions.assertSame(
                first,
                suggestionProvider.parsedSuggestions(),
                "An unchanged property must not be re-parsed"
            )
        }
        Assertions.assertEquals(550, first.size)
        verify(exactly = 20) { guiProps.getGuiProperty("autocomplete.clientmods") }
    }

    /**
     * Pins that a changed property is picked up. The memo is keyed on the raw property value, so
     * saving new suggestions — which every caller of `allSuggestions()` does, via
     * `storeGuiProperty` — must be visible immediately.
     */
    @Test
    fun aChangedSuggestionListIsPickedUp() {
        val suggestionProvider = provider()
        Assertions.assertEquals(550, suggestionProvider.allSuggestions().size)
        every { guiProps.getGuiProperty("autocomplete.clientmods") } returns "only-one"
        Assertions.assertEquals(setOf("only-one"), suggestionProvider.allSuggestions())
    }

    /**
     * Pins that the returned set stays **caller-owned**.
     *
     * Every production caller mutates it and persists the result — `ConfigEditor.saveSuggestions`
     * adds the current field value, `InclusionsEditor.saveSuggestions` adds and `removeIf`s. Handing
     * out a shared cached instance would let those mutations corrupt the cache and accumulate across
     * calls, so caching must cache the *parse*, never the instance.
     */
    @Test
    fun eachCallerGetsItsOwnMutableSet() {
        val suggestionProvider = provider()
        val first = suggestionProvider.allSuggestions()
        first.add("added-by-caller")
        first.removeIf { it == "mod-1" }
        val second = suggestionProvider.allSuggestions()
        Assertions.assertFalse(second.contains("added-by-caller"), "A caller's addition must not leak into the source")
        Assertions.assertTrue(second.contains("mod-1"), "A caller's removal must not leak into the source")
        Assertions.assertEquals(550, second.size)
    }

    /**
     * Pins that an absent property yields no suggestions rather than the literal string "null" —
     * the source reads the property as a nullable and the old code compared its `toString()`.
     */
    @Test
    fun anAbsentPropertyYieldsNoSuggestions() {
        every { guiProps.getGuiProperty("autocomplete.missing") } returns null
        val suggestionProvider = SuggestionProvider(guiProps, JTextArea(), "missing")
        Assertions.assertTrue(suggestionProvider.allSuggestions().isEmpty())
    }
}
