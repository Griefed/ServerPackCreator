/* Copyright (C) 2025 Griefed
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
package de.griefed.serverpackcreator.clientside

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Pins the heuristic that turns a project's published file-names into the version-less list-stems the
 * clientside-only list matches with `startsWith`.
 */
internal class FilenameStemDeriverTest {

    /** Several versions of one mod collapse to the leading stem via the common prefix. */
    @Test
    fun derivesStemFromMultipleVersions() {
        Assertions.assertEquals(
            "jei-",
            FilenameStemDeriver.deriveStem(
                listOf("jei-1.20.1-15.2.0.27.jar", "jei-1.19.2-11.6.0.1018.jar")
            )
        )
    }

    /** An embedded `mc<version>` token must not leak into the stem. */
    @Test
    fun keepsLoaderTokenButDropsEmbeddedMinecraftVersion() {
        Assertions.assertEquals(
            "sodium-fabric-",
            FilenameStemDeriver.deriveStem(
                listOf("sodium-fabric-mc1.20.1-0.5.3.jar", "sodium-fabric-mc1.19.4-0.4.10.jar")
            )
        )
    }

    /** With a single file-name the trailing version is stripped but the separator is kept. */
    @Test
    fun stripsTrailingVersionFromSingleFileNameKeepingSeparator() {
        Assertions.assertEquals("rubidium-", FilenameStemDeriver.deriveStem(listOf("rubidium-0.7.1.jar")))
    }

    /** A name whose version directly abuts it (no separator) yields a separator-less stem. */
    @Test
    fun handlesSeparatorlessVersion() {
        Assertions.assertEquals("totaldarkness", FilenameStemDeriver.deriveStem(listOf("totaldarkness1.2.jar")))
    }

    /** Underscore separators are preserved just like dashes. */
    @Test
    fun preservesUnderscoreSeparator() {
        Assertions.assertEquals(
            "rubidium_extras-",
            FilenameStemDeriver.deriveStem(
                listOf("rubidium_extras-1.0.jar", "rubidium_extras-1.1.jar")
            )
        )
    }

    /** Empty input yields no stem rather than throwing. */
    @Test
    fun returnsNullForNoFiles() {
        Assertions.assertNull(FilenameStemDeriver.deriveStem(emptyList()))
    }

    /** Per-loader grouping produces one stem per loader. */
    @Test
    fun derivesOneStemPerLoader() {
        val stems = FilenameStemDeriver.deriveStems(
            mapOf(
                "Fabric" to listOf("sodium-fabric-mc1.20.1-0.5.3.jar"),
                "Forge" to listOf("reforgium-1.20.1-1.0.jar", "reforgium-1.19.2-0.9.jar")
            )
        )
        Assertions.assertEquals("sodium-fabric-", stems["Fabric"])
        Assertions.assertEquals("reforgium-", stems["Forge"])
    }
}
