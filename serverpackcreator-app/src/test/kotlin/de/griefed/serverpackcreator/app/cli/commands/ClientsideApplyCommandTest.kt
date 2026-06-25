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
package de.griefed.serverpackcreator.app.cli.commands

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * End-to-end check that the apply-command reads a report-JSON and inserts its suggested entries (with
 * the project-link as the documenting comment) into both fallback-list files.
 */
internal class ClientsideApplyCommandTest {

    @Test
    fun appliesSuggestedEntriesToBothFiles(@TempDir tempDir: File) {
        val report = File(tempDir, "report.json").apply {
            writeText(
                """
                {
                  "projectUrl": "https://modrinth.com/mod/banana",
                  "suggestedEntries": ["banana-"]
                }
                """.trimIndent()
            )
        }
        val generationConfig = File(tempDir, "GenerationConfig.kt").apply {
            writeText(
                """
                class GenerationConfig {
                    private var fallbackMods = TreeSet(
                        listOf(
                            "apple-",                       //https://a
                            "mango-"
                        )
                    )
                }
                """.trimIndent()
            )
        }
        val properties = File(tempDir, "spc.properties").apply {
            writeText(
                "de.griefed.serverpackcreator.configuration.fallbackmodslist=\\\n" +
                    "    apple-,\\\n" +
                    "    mango-\n"
            )
        }

        ClientsideApplyCommand().apply(report, generationConfig, properties)

        val kotlin = generationConfig.readText()
        Assertions.assertTrue(kotlin.contains("\"banana-\","))
        Assertions.assertTrue(kotlin.contains("//https://modrinth.com/mod/banana"))
        // sorted between apple and mango
        Assertions.assertTrue(kotlin.indexOf("apple-") < kotlin.indexOf("banana-"))
        Assertions.assertTrue(kotlin.indexOf("banana-") < kotlin.indexOf("mango-"))

        Assertions.assertTrue(properties.readText().contains("    banana-,\\"))
    }
}
