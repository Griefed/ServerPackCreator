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
package de.griefed.serverpackcreator.plugin.servertest.core

import com.electronwill.nightconfig.core.CommentedConfig
import com.electronwill.nightconfig.core.file.FileNotFoundAction
import com.electronwill.nightconfig.toml.TomlParser
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * Pins the plugin's settings against the `config.toml` this module actually ships.
 *
 * Parsed by the same TOML parser `ApiPlugins` uses, and read from the shipped file rather than from a
 * literal, so a key renamed in one place and not the other fails here rather than at a user's next launch.
 * That matters more than usual: `ApiPlugins.extractPluginConfigs` extracts the shipped file only when the
 * user's copy does not exist, so a rename orphans a saved setting instead of migrating it.
 */
internal class ServerTestSettingsTest {

    /** The `config.toml` this module ships, parsed the way ServerPackCreator parses it. */
    private fun shippedConfig(): CommentedConfig = TomlParser().parse(
        File("src/main/resources/config.toml"), FileNotFoundAction.THROW_ERROR, StandardCharsets.UTF_8
    )

    /**
     * Every key the code reads is actually present in the shipped file.
     *
     * Asserted by **presence**, not by the value that comes back, and that distinction is the whole point.
     * Each shipped default deliberately equals the code's own fallback, so reading a renamed key returns the
     * fallback and looks exactly like reading the right one — a first version of this guard asserted only
     * the values and stayed green with `portRangeStart` renamed to `portRangeStartMUTATED`. Presence is the
     * only assertion that can tell a live key from a dead one.
     */
    @Test
    fun theShippedConfigCarriesEveryKeyTheCodeReads() {
        val config = shippedConfig()

        for (key in listOf(
            ServerTestSettings.PORT_RANGE_START_KEY,
            ServerTestSettings.PORT_RANGE_END_KEY,
            ServerTestSettings.CONSOLE_SCROLLBACK_KEY
        )) {
            Assertions.assertTrue(
                config.contains(key),
                "The shipped config.toml has no '$key'. ApiPlugins extracts that file only when the user has " +
                        "no copy, so a key the code reads but the file omits silently falls back forever."
            )
        }
    }

    /** The shipped defaults are the documented ones, read through the accessors users' settings go through. */
    @Test
    fun theShippedDefaultsAreTheDocumentedOnes() {
        val settings = ServerTestSettings(shippedConfig())

        Assertions.assertEquals(PortAllocator.DEFAULT_RANGE_START, settings.portRangeStart)
        Assertions.assertEquals(PortAllocator.DEFAULT_RANGE_END, settings.portRangeEnd)
        Assertions.assertEquals(ServerTestSettings.DEFAULT_CONSOLE_SCROLLBACK, settings.consoleScrollback)
    }

    /**
     * A hand-edited value of the wrong type falls back rather than throwing. These are read while the tab is
     * being built, and `ApiPlugins` calls `getTab` outside its own try-block.
     */
    @Test
    fun aDamagedValueFallsBackToTheDefault() {
        val config = shippedConfig()
        config.set<Any>(ServerTestSettings.PORT_RANGE_START_KEY, "twenty-five thousand")
        config.remove<Any>(ServerTestSettings.CONSOLE_SCROLLBACK_KEY)

        val settings = ServerTestSettings(config)

        Assertions.assertEquals(PortAllocator.DEFAULT_RANGE_START, settings.portRangeStart)
        Assertions.assertEquals(ServerTestSettings.DEFAULT_CONSOLE_SCROLLBACK, settings.consoleScrollback)
    }

    /** A scrollback of zero would read as the server producing no output at all, so it is floored at one. */
    @Test
    fun scrollbackIsNeverZero() {
        val config = shippedConfig()
        config.set<Any>(ServerTestSettings.CONSOLE_SCROLLBACK_KEY, 0)

        Assertions.assertEquals(1, ServerTestSettings(config).consoleScrollback)
    }
}
