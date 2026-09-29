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
package de.griefed.serverpackcreator.api

import com.electronwill.nightconfig.core.CommentedConfig
import de.griefed.serverpackcreator.api.plugins.swinggui.ExtensionTab
import de.griefed.serverpackcreator.api.plugins.swinggui.TabExtension
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File
import java.util.Optional
import javax.swing.Icon
import javax.swing.JPanel
import javax.swing.JTabbedPane

/**
 * Pins that one misbehaving tab extension costs only its own tab.
 *
 * ServerPackCreator builds its window by handing the tabbed pane to every installed plugin in turn. Every
 * value taken from a `TabExtension` on that path is third-party code — `name`, `getTab`, `title`, `icon`
 * and `tooltip` alike — and only `addTab` was ever guarded. A plugin whose tab constructor threw
 * therefore did not lose *its* tab: it escaped into `MainPanel`'s initialiser and took the whole window's
 * tab assembly with it, including every plugin registered after it and ServerPackCreator's own layout.
 *
 * The failure needs no malice to reach. A plugin built against an older API and loaded into a newer one
 * throws `NoClassDefFoundError` from exactly here, which is why an `Error` is pinned alongside the
 * exceptions rather than treated as unreachable.
 *
 * Exercised through [ApiPlugins.addTabExtensionTab] rather than through a plugin jar, because no
 * installed plugin throws on purpose and a fixture jar that did would have to be built and maintained to
 * assert one branch. That `addTabExtensionTabs` routes through it is covered by `ExtensionScopingTest`,
 * which counts the tabs real plugins produce.
 */
class TabExtensionFailureContainmentTest {

    private val apiPlugins: ApiPlugins =
        ApiWrapper.api(File("build/resources/test/serverpackcreator.properties")).apiPlugins

    /** A tab extension that works, so a failing neighbour can be shown not to have cost it anything. */
    private class WellBehavedTab(private val label: String) : TabExtension {
        override fun getTab(
            versionMeta: VersionMeta,
            apiProperties: ApiProperties,
            utilities: Utilities,
            pluginConfig: Optional<CommentedConfig>,
            configFile: Optional<File>
        ): ExtensionTab = object : ExtensionTab(versionMeta, apiProperties, utilities, pluginConfig, configFile) {}

        override val icon: Icon? = null
        override val title = label
        override val tooltip = "tooltip"
        override val name = label
        override val description = "description"
        override val author = "test"
        override val version = "1.0.0"
        override val extensionId = label
    }

    /**
     * A tab extension that throws from whichever member the test names.
     *
     * Every member is a candidate because every one of them is plugin code on the registration path; the
     * failing member is chosen per test so each is pinned separately rather than as a group.
     */
    private class ThrowingTab(private val failing: String, private val thrown: Throwable) : TabExtension {
        private fun failIf(member: String) {
            if (member == failing) {
                throw thrown
            }
        }

        override fun getTab(
            versionMeta: VersionMeta,
            apiProperties: ApiProperties,
            utilities: Utilities,
            pluginConfig: Optional<CommentedConfig>,
            configFile: Optional<File>
        ): ExtensionTab {
            failIf("getTab")
            return object : ExtensionTab(versionMeta, apiProperties, utilities, pluginConfig, configFile) {}
        }

        override val icon: Icon? get() = null.also { failIf("icon") }
        override val title: String get() = "throwing".also { failIf("title") }
        override val tooltip: String get() = "tooltip".also { failIf("tooltip") }
        override val name: String get() = "throwing".also { failIf("name") }
        override val description = "description"
        override val author = "test"
        override val version = "1.0.0"
        override val extensionId = "throwing"
    }

    /** Add [extension] to [pane], failing the test if anything escapes into the caller. */
    private fun register(extension: TabExtension, pane: JTabbedPane) {
        try {
            apiPlugins.addTabExtensionTab(extension, "test-plugin", pane)
        } catch (throwable: Throwable) {
            Assertions.fail<Unit>(
                "A misbehaving tab extension escaped into GUI assembly: $throwable. " +
                        "Every plugin registered after it loses its tab, and so does ServerPackCreator's own layout."
            )
        }
    }

    /** The headline case: a plugin whose tab constructor throws must cost only its own tab. */
    @Test
    fun aTabFactoryThatThrowsDoesNotEscape() {
        val pane = JTabbedPane()

        register(ThrowingTab("getTab", IllegalStateException("the tab's constructor blew up")), pane)

        Assertions.assertEquals(0, pane.tabCount, "The failing extension must not have added a tab.")
    }

    /**
     * An `Error` is contained too. A plugin compiled against an older ServerPackCreator throws
     * `NoClassDefFoundError` from its tab constructor, which is a realistic way for a user to meet this.
     */
    @Test
    fun aTabFactoryThatThrowsAnErrorDoesNotEscape() {
        val pane = JTabbedPane()

        register(ThrowingTab("getTab", NoClassDefFoundError("de/griefed/removed/Type")), pane)

        Assertions.assertEquals(0, pane.tabCount)
    }

    /**
     * The labels are plugin code as much as the factory is, and they are read *before* the tab is built —
     * `name` for the log line, the rest for the pane. A throw from any of them reaches the same caller.
     */
    @Test
    fun aLabelThatThrowsDoesNotEscape() {
        for (member in listOf("name", "title", "tooltip", "icon")) {
            val pane = JTabbedPane()

            register(ThrowingTab(member, IllegalStateException("$member blew up")), pane)

            Assertions.assertEquals(0, pane.tabCount, "A throwing '$member' must not leave a half-added tab.")
        }
    }

    /**
     * The point of containing it: the plugins around the failure keep their tabs.
     *
     * Asserted by title rather than by count, so a guard cannot pass on two tabs that are the wrong ones.
     */
    @Test
    fun aFailingExtensionCostsOnlyItsOwnTab() {
        val pane = JTabbedPane()

        register(WellBehavedTab("before"), pane)
        register(ThrowingTab("getTab", IllegalStateException("blew up")), pane)
        register(WellBehavedTab("after"), pane)

        Assertions.assertEquals(2, pane.tabCount)
        Assertions.assertEquals("before", pane.getTitleAt(0))
        Assertions.assertEquals("after", pane.getTitleAt(1), "A later plugin must not lose its tab to an earlier one.")
    }

    /** A well-behaved extension is still added, so the containment cannot be "never add anything". */
    @Test
    fun aWellBehavedExtensionIsStillAdded() {
        val pane = JTabbedPane()

        register(WellBehavedTab("fine"), pane)

        Assertions.assertEquals(1, pane.tabCount)
        Assertions.assertEquals("fine", pane.getTitleAt(0))
    }
}
