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
package de.griefed.serverpackcreator.api.settings

import de.griefed.serverpackcreator.api.PropertyStore
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File
import java.util.Properties
import java.util.TreeSet

/**
 * The clientside fallback list is written three times: `GenerationConfig`'s `fallbackMods`, the shipped
 * `serverpackcreator.properties`, and the Docker image's default copy. Nothing generates one from another, and a
 * copy that drifts fails silently -- a mod one of them lacks is simply shipped into server packs -- so all three
 * are held to one set.
 */
internal class FallbackModsCopiesTest {

    private fun listIn(propertiesFile: File): TreeSet<String> {
        Assertions.assertTrue(propertiesFile.isFile, "not found: ${propertiesFile.absolutePath}")
        val properties = Properties().apply { propertiesFile.reader().use { load(it) } }
        val value = properties.getProperty(GenerationConfig.FALLBACK_MODS_LIST_KEY)
        Assertions.assertNotNull(value, "${propertiesFile.path} has no ${GenerationConfig.FALLBACK_MODS_LIST_KEY}")
        return value.split(",").filter { it.isNotEmpty() }.toCollection(TreeSet())
    }

    private val compiledDefault: TreeSet<String> = TreeSet(GenerationConfig(PropertyStore()).clientsideMods)

    @Test
    fun theShippedPropertiesListTheCompiledDefault() {
        Assertions.assertEquals(compiledDefault, listIn(File("src/main/resources/serverpackcreator.properties")))
    }

    @Test
    fun theDockerDefaultsListTheCompiledDefault() {
        val docker = listIn(File("../docker/root/defaults/serverpackcreator.properties"))
        Assertions.assertEquals(
            emptyList<String>() to emptyList<String>(),
            (compiledDefault - docker).toList() to (docker - compiledDefault).toList(),
            "(missing from the Docker copy) to (only in the Docker copy)"
        )
    }
}
