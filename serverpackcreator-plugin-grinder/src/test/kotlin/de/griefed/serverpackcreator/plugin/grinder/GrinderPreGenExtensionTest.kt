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
package de.griefed.serverpackcreator.plugin.grinder

import com.electronwill.nightconfig.core.CommentedConfig
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.config.PackConfig
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import de.griefed.serverpackcreator.plugin.grinder.core.SelectionPane
import de.griefed.serverpackcreator.plugin.grinder.core.SelectionStore
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.Optional

/**
 * Pins the extension point itself, not a helper standing in for it.
 *
 * This is where the plugin actually changes a server pack: `ServerPackHandler.run` calls
 * `ApiPlugins.runPreGenExtensions(packConfig, …)` and then reads `packConfig.clientMods` to compile the
 * mod list, so mutating that list here is what excludes a mod. Because the hook is inside
 * `ServerPackHandler.run`, it fires for the GUI, the CLI and the web backend alike — the selection is
 * read out of the plugin configuration, which means a headless run honours ticks made in the GUI.
 *
 * `versionMeta`, `utilities` and `apiProperties` are mocked because this extension genuinely does not
 * touch them; mocking them is what keeps the guard on the real `run` signature instead of on a private
 * helper that the real signature might one day stop calling.
 */
internal class GrinderPreGenExtensionTest {

    private val versionMeta = mockk<VersionMeta>(relaxed = true)
    private val utilities = mockk<Utilities>(relaxed = true)
    private val apiProperties = mockk<ApiProperties>(relaxed = true)

    /** Run the extension over a pack config, with the plugin configured as [configure] says. */
    private fun runWith(
        packConfig: PackConfig,
        pluginConfig: Optional<CommentedConfig> = Optional.of(CommentedConfig.inMemory()),
        configure: SelectionStore.() -> Unit = {}
    ) {
        pluginConfig.ifPresent { SelectionStore(it).configure() }
        GrinderPreGenExtension().run(
            versionMeta, utilities, apiProperties, packConfig, "/tmp/does-not-matter", pluginConfig, ArrayList()
        )
    }

    /** The point of the feature: what was ticked is excluded, on top of what the user already had. */
    @Test
    fun addsTheSelectionToTheClientsideExclusionList() {
        val packConfig = PackConfig().apply { setClientMods(mutableListOf("my-own-entry")) }

        runWith(packConfig) {
            setSelected(SelectionPane.CONFIRMED, listOf("creativecore-"))
            setSelected(SelectionPane.OTHER, listOf("bookshelf-"))
        }

        Assertions.assertEquals(listOf("my-own-entry", "creativecore-", "bookshelf-"), packConfig.clientMods)
    }

    /**
     * A user who never opens the plugin's tab must generate exactly the pack they generated before
     * installing it. Nothing ticked, nothing touched.
     */
    @Test
    fun leavesTheListAloneWhenNothingIsSelected() {
        val packConfig = PackConfig().apply { setClientMods(mutableListOf("my-own-entry")) }
        runWith(packConfig)
        Assertions.assertEquals(listOf("my-own-entry"), packConfig.clientMods)
    }

    /**
     * A plugin whose configuration failed to parse still has to let generation finish. `ApiPlugins`
     * hands an empty Optional when it could not read the file, and an exclusion the user cannot see is
     * not worth aborting a server pack over.
     */
    @Test
    fun doesNothingWhenNoConfigurationWasProvided() {
        val packConfig = PackConfig().apply { setClientMods(mutableListOf("my-own-entry")) }
        runWith(packConfig, pluginConfig = Optional.empty())
        Assertions.assertEquals(listOf("my-own-entry"), packConfig.clientMods)
    }

    /**
     * Generation happens repeatedly in one session, against the same live plugin config. The list must
     * not grow by a copy of the selection each time — the same list is shown back to the user in the GUI
     * field, and this is how that field would end up thousands of lines long.
     */
    @Test
    fun isIdempotentAcrossRepeatedGenerations() {
        val packConfig = PackConfig().apply { setClientMods(mutableListOf("my-own-entry")) }
        val pluginConfig = Optional.of(CommentedConfig.inMemory())

        repeat(3) {
            runWith(packConfig, pluginConfig) { setSelected(SelectionPane.CONFIRMED, listOf("creativecore-")) }
        }

        Assertions.assertEquals(listOf("my-own-entry", "creativecore-"), packConfig.clientMods)
    }

    /** The extension identifies itself, since ServerPackCreator logs and keys extensions by these. */
    @Test
    fun identifiesItself() {
        val extension = GrinderPreGenExtension()
        Assertions.assertTrue(extension.extensionId.isNotBlank())
        Assertions.assertTrue(extension.name.isNotBlank())
        Assertions.assertTrue(extension.description.isNotBlank())
        Assertions.assertTrue(extension.author.isNotBlank())
        Assertions.assertTrue(extension.version.isNotBlank())
    }
}
