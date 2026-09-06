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

import com.electronwill.nightconfig.core.CommentedConfig
import com.electronwill.nightconfig.core.file.FileNotFoundAction
import com.electronwill.nightconfig.toml.TomlParser
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * Pins the plugin's state as it actually lives: inside the `CommentedConfig` ServerPackCreator owns.
 * The tab writes it and the pre-generation extension reads it, and `ApiPlugins` hands **the same
 * instance** to both — so a tick made in the GUI must be visible to a generation started seconds later,
 * without a save and without a restart. That sharing is the mechanism the whole feature rests on, so
 * these guards work against a real config object rather than a stand-in for one.
 */
internal class SelectionStoreTest {

    /**
     * The config as ServerPackCreator would hand it over on a first run: parsed from the very
     * `config.toml` this module ships, by the same TOML parser `ApiPlugins.registerPluginConfig` uses.
     * Parsing the shipped file rather than a literal is deliberate — it makes the defaults part of what
     * is pinned, so a key renamed in one place and not the other is a failure here.
     */
    private fun shippedConfig(): CommentedConfig =
        TomlParser().parse(
            File("src/main/resources/config.toml"), FileNotFoundAction.THROW_ERROR, StandardCharsets.UTF_8
        )

    /** Out of the box the plugin is idle: no address, nothing ticked, nothing excluded. */
    @Test
    fun startsIdleOnTheShippedDefaults() {
        val store = SelectionStore(shippedConfig())
        Assertions.assertEquals("", store.grinderUrl)
        Assertions.assertNull(store.resolvedUrl, "an empty address must not resolve to something requestable")
        Assertions.assertTrue(store.selected(SelectionPane.CONFIRMED).isEmpty())
        Assertions.assertTrue(store.selected(SelectionPane.OTHER).isEmpty())
        Assertions.assertTrue(store.allSelected().isEmpty())
        Assertions.assertEquals(5, store.refreshIntervalSeconds)
    }

    /** What the Settings pane writes is what the client later reads, through the shared config object. */
    @Test
    fun roundTripsTheAddressThroughTheConfig() {
        val config = shippedConfig()
        SelectionStore(config).grinderUrl = "http://localhost:8757/"

        val reader = SelectionStore(config)
        Assertions.assertEquals("http://localhost:8757/", reader.grinderUrl, "the field keeps what was typed")
        Assertions.assertEquals(
            "http://localhost:8757", reader.resolvedUrl,
            "what is requested goes through the one URL rule, so the pane and the client cannot disagree"
        )
    }

    /** The two panes are separate lists on purpose: one is proven findings, the other is at-your-own-risk. */
    @Test
    fun keepsTheTwoPanesSelectionsApartButExcludesBoth() {
        val config = shippedConfig()
        val store = SelectionStore(config)
        store.setSelected(SelectionPane.CONFIRMED, listOf("creativecore-", "jei-"))
        store.setSelected(SelectionPane.OTHER, listOf("bookshelf-"))

        val reloaded = SelectionStore(config)
        Assertions.assertEquals(setOf("creativecore-", "jei-"), reloaded.selected(SelectionPane.CONFIRMED))
        Assertions.assertEquals(setOf("bookshelf-"), reloaded.selected(SelectionPane.OTHER))
        Assertions.assertEquals(
            setOf("creativecore-", "jei-", "bookshelf-"), reloaded.allSelected(),
            "generation excludes what was ticked anywhere; the split is a UI distinction, not a behavioural one"
        )
    }

    /**
     * A selection is authoritative on its own. An entry the grinder no longer reports — the crawl moved
     * on, the store was reset, the daemon is simply down — stays ticked, because the alternative is that
     * a mod the user deliberately excluded silently reappears in their next server pack. The plugin never
     * prunes; only the user unticks.
     */
    @Test
    fun keepsAnEntryTheGrinderNoLongerReports() {
        val config = shippedConfig()
        SelectionStore(config).setSelected(SelectionPane.CONFIRMED, listOf("a-mod-that-vanished-"))

        // A later session that reaches no grinder at all still knows what to exclude.
        val offline = SelectionStore(config)
        Assertions.assertEquals(setOf("a-mod-that-vanished-"), offline.allSelected())
    }

    /** Unticking removes exactly one entry and leaves the rest of the list alone. */
    @Test
    fun replacesTheStoredListWholesale() {
        val config = shippedConfig()
        val store = SelectionStore(config)
        store.setSelected(SelectionPane.CONFIRMED, listOf("creativecore-", "jei-"))
        store.setSelected(SelectionPane.CONFIRMED, listOf("jei-"))

        Assertions.assertEquals(setOf("jei-"), SelectionStore(config).selected(SelectionPane.CONFIRMED))
    }

    /**
     * Blank entries are dropped on the way in. An empty exclusion entry matches every mod name under
     * `startsWith`/`contains`, so one stored by accident would empty a server pack's mods directory.
     */
    @Test
    fun refusesToStoreABlankEntry() {
        val config = shippedConfig()
        SelectionStore(config).setSelected(SelectionPane.CONFIRMED, listOf("creativecore-", "", "   "))
        Assertions.assertEquals(setOf("creativecore-"), SelectionStore(config).selected(SelectionPane.CONFIRMED))
    }

    /**
     * A hand-edited config is still a config. A missing key, or one holding the wrong type, must read as
     * the default rather than throw — this object is constructed on the generation path, where an
     * exception aborts somebody's server pack over a typo in a settings file.
     */
    @Test
    fun readsDefaultsOutOfADamagedConfig() {
        val damaged = CommentedConfig.inMemory().apply {
            set<Any>("grinderUrl", 42)
            set<Any>("selectedConfirmed", "not-a-list")
            set<Any>("refreshIntervalSeconds", "soon")
        }
        val store = SelectionStore(damaged)
        Assertions.assertEquals("", store.grinderUrl)
        Assertions.assertTrue(store.allSelected().isEmpty())
        Assertions.assertEquals(5, store.refreshIntervalSeconds)
    }

    /** The poll interval is bounded: zero would busy-loop the daemon, and a negative Timer delay throws. */
    @Test
    fun boundsThePollInterval() {
        val config = shippedConfig()
        val store = SelectionStore(config)

        store.refreshIntervalSeconds = 0
        Assertions.assertEquals(1, SelectionStore(config).refreshIntervalSeconds)

        store.refreshIntervalSeconds = 9_999
        Assertions.assertEquals(3_600, SelectionStore(config).refreshIntervalSeconds)

        // The side that matters most: javax.swing.Timer rejects a non-positive delay outright, so a
        // negative surviving the clamp is a thrown IllegalArgumentException in the tab's constructor.
        store.refreshIntervalSeconds = -30
        Assertions.assertEquals(1, SelectionStore(config).refreshIntervalSeconds)
    }
}
