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

import de.griefed.serverpackcreator.api.plugins.serverpackhandler.PreGenExtension
import de.griefed.serverpackcreator.api.plugins.swinggui.TabExtension
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry
import javax.swing.JTabbedPane

/**
 * Pins that an extension belongs to **one** plugin.
 *
 * [ApiPlugins.getAllExtensionsOfPlugin] takes a plugin and is called once per plugin, so if it answers
 * with every plugin's extensions the callers multiply: `addTabExtensionTabs` adds each tab once per
 * *installed plugin*, and `runPreGenExtensions` runs each extension that many times. With a single
 * plugin installed — which is all this repository shipped until the grinder plugin — the defect is
 * invisible, which is why it survived: one plugin times one plugin is one.
 *
 * Reproduced 2026-09-06 by running ServerPackCreator with the example and grinder plugins side by side:
 * the tab strip read `Grinder | Tetris | Grinder | Tetris`.
 *
 * A second plugin is therefore the whole point of this test, and it is built here rather than checked in
 * — a second *fixture* jar would have to be maintained, and the example plugin is already the one that
 * implements every extension point. The clone is the same bytes under a different id.
 */
class ExtensionScopingTest {

    private val apiPlugins: ApiPlugins =
        ApiWrapper.api(File("build/resources/test/serverpackcreator.properties")).apiPlugins

    private val pluginsDir = File("tests/plugins")
    private val clone = File(pluginsDir, "extension-scoping-clone.jar")

    @BeforeEach
    fun installTwoPlugins() {
        runCatching { File("src/test/resources/testresources/plugins").copyRecursively(pluginsDir, true) }
        val original = pluginsDir.listFiles { file -> file.name.endsWith(".jar") && file != clone }
            ?.firstOrNull()
            ?: error("No plugin jar to clone; the example plugin should have been copied into $pluginsDir.")
        cloneUnderNewId(original, clone, CLONE_ID)
        apiPlugins.loadPlugins()
        apiPlugins.startPlugins()
    }

    @AfterEach
    fun removeTheClone() {
        // Left behind it would follow every other test in this JVM into its own plugins directory.
        apiPlugins.plugins.firstOrNull { it.pluginId == CLONE_ID }?.let { runCatching { apiPlugins.unloadPlugin(CLONE_ID) } }
        clone.delete()
    }

    /**
     * Two plugins, each providing one `TabExtension`, must yield one tab each. Before the fix this
     * produced four: the outer loop runs per plugin and the lookup ignored which plugin it was asked
     * about.
     */
    @Test
    fun addsOneTabPerTabExtensionRatherThanOnePerPluginSquared() {
        Assertions.assertEquals(
            2, apiPlugins.plugins.size,
            "this guard is meaningless with fewer than two plugins installed"
        )

        val pane = JTabbedPane()
        apiPlugins.addTabExtensionTabs(pane)

        Assertions.assertEquals(2, pane.tabCount, "each plugin's tab must be added exactly once")
    }

    /** The lookup answers for the plugin it was handed, not for every plugin the manager knows. */
    @Test
    fun answersOnlyWithTheGivenPluginsExtensions() {
        for (plugin in apiPlugins.plugins) {
            Assertions.assertEquals(
                1, apiPlugins.getAllExtensionsOfPlugin(plugin, TabExtension::class.java).size,
                "${plugin.pluginId} was handed another plugin's TabExtension"
            )
            Assertions.assertEquals(
                1, apiPlugins.getAllExtensionsOfPlugin(plugin, PreGenExtension::class.java).size,
                "${plugin.pluginId} was handed another plugin's PreGenExtension"
            )
        }
    }

    /**
     * Copy [source] to [target], rewriting the plugin id in both places that carry it: the jar manifest,
     * which is what pf4j's descriptor finder reads, and `plugin.toml`, which `ServerPackCreatorPlugin`
     * reads for its own fields. Rewriting only one leaves a plugin whose two identities disagree.
     */
    private fun cloneUnderNewId(source: File, target: File, newId: String) {
        JarFile(source).use { jar ->
            val manifest = java.util.jar.Manifest(jar.manifest).apply {
                mainAttributes.putValue("Plugin-Id", newId)
            }
            JarOutputStream(target.outputStream(), manifest).use { out ->
                for (entry in jar.entries()) {
                    if (entry.name == "META-INF/MANIFEST.MF") {
                        continue
                    }
                    out.putNextEntry(ZipEntry(entry.name))
                    if (entry.name == "plugin.toml") {
                        val rewritten = jar.getInputStream(entry).use { it.readBytes() }
                            .decodeToString()
                            .replace(Regex("""(?m)^id\s*=\s*".*"$"""), """id = "$newId"""")
                        out.write(rewritten.toByteArray())
                    } else {
                        jar.getInputStream(entry).use { it.copyTo(out) }
                    }
                    out.closeEntry()
                }
            }
        }
    }

    private companion object {
        const val CLONE_ID = "extension-scoping-clone"
    }
}
