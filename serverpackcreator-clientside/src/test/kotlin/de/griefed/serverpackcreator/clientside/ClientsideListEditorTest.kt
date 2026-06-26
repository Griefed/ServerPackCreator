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

import de.griefed.serverpackcreator.clientside.ClientsideListEditor.Entry
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Pins the source-edits that the acceptance-workflow turns into a PR: inserting entries into the
 * `GenerationConfig.kt` `fallbackMods` block and the `serverpackcreator.properties` list, in sorted
 * position, skipping duplicates and keeping the list syntactically valid when appending at the end.
 */
internal class ClientsideListEditorTest {

    private val kotlin = """
        class GenerationConfig {
            private var fallbackMods = TreeSet(
                listOf(
                    "apple-",                       //https://a
                    "mango-",                       //https://m
                    "zucchini-"
                )
            )
        }
    """.trimIndent()

    private val properties =
        "de.griefed.serverpackcreator.configuration.fallbackmodslist=\\\n" +
            "    apple-,\\\n" +
            "    mango-,\\\n" +
            "    zucchini-\n" +
            "other.key=value\n"

    @Test
    fun insertsKotlinEntryInSortedPositionWithComment() {
        val result = ClientsideListEditor.addToKotlin(kotlin, listOf(Entry("banana-", "https://b")))
        val entries = result.lines().mapNotNull { line ->
            Regex(""""([^"]*)"""").find(line.trim().takeIf { it.startsWith("\"") } ?: "")?.groupValues?.get(1)
        }
        Assertions.assertEquals(listOf("apple-", "banana-", "mango-", "zucchini-"), entries)
        Assertions.assertTrue(result.contains("\"banana-\","))
        Assertions.assertTrue(result.contains("//https://b"))
    }

    @Test
    fun skipsKotlinDuplicate() {
        val result = ClientsideListEditor.addToKotlin(kotlin, listOf(Entry("mango-", "https://dup")))
        Assertions.assertEquals(1, Regex("\"mango-\"").findAll(result).count())
    }

    @Test
    fun appendingKotlinEntryGivesPreviousLastEntryAComma() {
        val result = ClientsideListEditor.addToKotlin(kotlin, listOf(Entry("zzz-", "https://z")))
        Assertions.assertTrue(result.contains("\"zucchini-\","), "previously-last entry must gain a comma")
        Assertions.assertTrue(result.contains("\"zzz-\","))
    }

    @Test
    fun insertsPropertiesEntryInSortedPosition() {
        val result = ClientsideListEditor.addToProperties(properties, listOf(Entry("banana-", null)))
        val listLines = result.lines().filter { it.startsWith("    ") }.map { it.trim().removeSuffix("\\").trim().removeSuffix(",") }
        Assertions.assertEquals(listOf("apple-", "banana-", "mango-", "zucchini-"), listLines)
        Assertions.assertTrue(result.contains("    banana-,\\"))
    }

    @Test
    fun appendingPropertiesEntryGivesPreviousLastABackslashButNotTheNewLast() {
        val result = ClientsideListEditor.addToProperties(properties, listOf(Entry("zzz-", null)))
        Assertions.assertTrue(result.contains("    zucchini-,\\"), "previously-last entry must gain a continuation")
        // The new last entry must NOT carry a `,\` continuation that would bleed into the next property.
        Assertions.assertTrue(result.contains("    zzz-\n"), "new last entry must have no trailing continuation")
        Assertions.assertFalse(result.contains("    zzz-,\\"))
        Assertions.assertTrue(result.trimEnd().endsWith("other.key=value"))
    }
}
