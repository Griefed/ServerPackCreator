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

import com.electronwill.nightconfig.toml.TomlParser
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Pins that loading plugins is a **step**, not a side effect of building the object that manages them.
 *
 * **The bug this closes.** A plugin's code runs while `ApiPlugins` is being constructed, and `ApiPlugins`
 * is constructed inside `ApiWrapper.apiPlugins`' lazy initialiser, which `stageThree` touches *first* —
 * before `serverPackHandler` exists. The example plugin's `init` calls `ApiWrapper.api()`, so:
 *
 *  1. `ApiWrapper.api()` builds a wrapper; the companion's field is assigned only when the constructor
 *     *returns*, and `@Synchronized` is re-entrant on the same thread, so it is still `null` throughout.
 *  2. `setup()` -> `stageThree()` -> `apiPlugins` -> pf4j -> `Example.init` -> `ApiWrapper.api()`.
 *  3. `api` is still `null`, so a **second** wrapper is built, which loads the plugins again, which…
 *
 * `StackOverflowError`, with the plugin's own log lines repeated once per level — which is exactly how
 * Griefed reported it: "the log-output for the example plugin a gazillion times".
 *
 * **Why the api suite never caught it.** Tests share a JVM, and the first test class to call
 * `ApiWrapper.api()` did so before anything copied a plugin jar into `tests/plugins`. By the time
 * `ExtensionScopingTest` installs one and loads it by hand, the singleton is long since published, so the
 * re-entrant call returns it and the recursion never starts. The defect needs a populated plugins
 * directory at *first* startup, which is every real CLI run and no test.
 *
 * Two halves are fixed and only the first is cleanly pinnable here — a suite sharing a JVM cannot
 * un-create a singleton, so the end-to-end proof is a live CLI run recorded in the commit message.
 *
 * @author Griefed
 */
internal class PluginLoadingOrderTest {

    companion object {
        private val pluginsDir = File("tests/plugins")

        /** The same install the scoping test performs, so this class does not depend on running after it. */
        @JvmStatic
        @BeforeAll
        fun installTheExamplePlugin() {
            runCatching { File("src/test/resources/testresources/plugins").copyRecursively(pluginsDir, true) }
        }
    }

    private val api = ApiWrapper.api(File("build/resources/test/serverpackcreator.properties"))

    /**
     * **The fix, stated as a rule:** constructing the manager reads no jars and runs nobody's code. Until
     * that is true there is no way to have the rest of the API built before a plugin can reach into it,
     * because the construction *is* the loading.
     */
    @Test
    fun constructingTheManagerLoadsNothing() {
        val plugins = ApiPlugins(TomlParser(), api.apiProperties, api.versionMeta, api.utilities)

        try {
            Assertions.assertTrue(
                plugins.plugins.isEmpty(),
                "constructing the manager must not run plugin code: ${plugins.plugins.map { it.pluginId }}"
            )

            plugins.loadAndStart()

            Assertions.assertTrue(
                plugins.plugins.isNotEmpty(),
                "and the explicit step must still load what is installed in ${pluginsDir.absolutePath}"
            )
        } finally {
            runCatching { plugins.stopPlugins() }
            runCatching { plugins.unloadPlugins() }
        }
    }

    /**
     * The wrapper still ends up with its plugins, which is the half that must not regress: moving the work
     * out of a constructor is only safe if something calls the step.
     */
    @Test
    fun theWrapperStillEndsUpWithItsPluginsLoaded() {
        api.setup()

        Assertions.assertTrue(
            api.apiPlugins.plugins.isNotEmpty(),
            "stageThree owns the loading now, and must actually do it"
        )
    }

    /**
     * **And it happens last.** `serverPackHandler` is what the example plugin reaches for, so it has to
     * exist before any plugin runs — otherwise its lazy initialiser is entered *from inside*
     * `apiPlugins`' own lazy initialiser, and Kotlin's `SynchronizedLazyImpl` re-enters rather than
     * blocking: the initialiser simply runs again, loading the plugins again.
     *
     * Asserted from the outside the only way a test can: after a setup, both are live and the plugin that
     * demanded one during the other's construction did not blow the stack.
     */
    @Test
    fun theHandlersAPluginReachesForExistBeforePluginsRun() {
        api.setup()

        Assertions.assertNotNull(api.serverPackHandler, "the plugin's init calls this")
        Assertions.assertNotNull(api.configurationHandler, "and this")
        Assertions.assertTrue(api.apiPlugins.plugins.isNotEmpty(), "with the plugins loaded around them")
    }
}
