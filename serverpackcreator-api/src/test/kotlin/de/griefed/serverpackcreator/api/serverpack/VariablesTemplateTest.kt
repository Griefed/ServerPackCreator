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
package de.griefed.serverpackcreator.api.serverpack

import de.griefed.serverpackcreator.api.ApiWrapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Pins the shipped `variables.txt` template that server-pack generation fills in.
 *
 * The content used to be a 91-line Kotlin string literal, so correcting a comment an operator reads meant rebuilding
 * the API. It now lives beside the start-script templates in `server_files`, which introduces one risk worth guarding:
 * generation reads a file on disk, so a missing or unreadable template must not silently produce a server pack with no
 * `variables.txt` — the file every setting in a pack is configured through.
 */
internal class VariablesTemplateTest {

    private val apiWrapper = ApiWrapper.api(File("build/resources/test/serverpackcreator.properties"))

    /** Every placeholder generation substitutes must exist in the shipped template, or a pack ships raw markers. */
    @Test
    fun theShippedTemplateCarriesThePlaceholdersGenerationSubstitutes() {
        val template = javaClass.getResourceAsStream("/de/griefed/resources/server_files/variables.txt")
            ?.bufferedReader()?.use { it.readText() }
            ?: Assertions.fail("variables.txt is not bundled in the jar")

        for (placeholder in listOf(
            "SPC_MINECRAFT_VERSION_SPC",
            "SPC_MODLOADER_SPC",
            "SPC_MODLOADER_VERSION_SPC",
            "SPC_JAVA_SPC",
            "SPC_JAVA_ARGS_SPC",
            "SPC_USE_SSJ_SPC",
            "SPC_SSJ_FORGE_ARGS_SPC",
            "SPC_CLEANUP_SPC"
        )) {
            Assertions.assertTrue(
                template.contains(placeholder),
                "the shipped variables.txt must carry $placeholder, or generated packs ship the raw marker"
            )
        }
    }

    /**
     * With no template on disk the bundled copy is used, so the API works before `ApiWrapper.setup()` has populated a
     * home directory — and a template the user deletes mid-run cannot produce an empty `variables.txt`.
     */
    @Test
    fun aMissingTemplateFallsBackToTheBundledCopy() {
        val template = apiWrapper.apiProperties.defaultVariablesTemplate
        val backup = if (template.isFile) template.readText() else null
        try {
            template.delete()
            Assertions.assertFalse(template.isFile, "precondition: the template is absent")

            val content = apiWrapper.serverPackHandler.variables

            Assertions.assertTrue(content.isNotBlank(), "a missing template must not yield empty variables.txt content")
            Assertions.assertTrue(
                content.contains("SPC_MINECRAFT_VERSION_SPC"),
                "the fallback must be the real template, placeholders included"
            )
        } finally {
            backup?.let { template.writeText(it) }
        }
    }

    /** What is on disk is what generation uses — that is the whole point of moving it out of the source. */
    @Test
    fun theTemplateOnDiskIsWhatGenerationReads() {
        val template = apiWrapper.apiProperties.defaultVariablesTemplate
        val backup = if (template.isFile) template.readText() else null
        try {
            template.parentFile.mkdirs()
            template.writeText("# edited by the operator\nMINECRAFT_VERSION=SPC_MINECRAFT_VERSION_SPC\n")

            val content = apiWrapper.serverPackHandler.variables

            Assertions.assertTrue(
                content.contains("# edited by the operator"),
                "generation must read the file on disk, not a compiled-in copy"
            )
        } finally {
            backup?.let { template.writeText(it) }
        }
    }
}
