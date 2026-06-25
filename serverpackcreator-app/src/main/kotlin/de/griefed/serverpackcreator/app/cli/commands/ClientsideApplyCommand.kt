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

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.griefed.serverpackcreator.app.clientside.ClientsideListEditor
import de.griefed.serverpackcreator.app.clientside.ClientsideListEditor.Entry
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import picocli.CommandLine
import picocli.shell.jline3.PicocliCommands.ClearScreen
import java.io.File

/**
 * Applies the approved clientside-list entries from a verification-report (the JSON the report-comment
 * embeds) to the two files that ship the official fallback-list, so the acceptance-workflow can open
 * a ready-to-review PR. Pure source-editing via [ClientsideListEditor] — no API/network needed.
 *
 * @author Griefed
 */
@CommandLine.Command(
    name = "clientsideapply", mixinStandardHelpOptions = true,
    description = [
        "Add the suggested entries from a clientside-report JSON to the official fallback-list files.",
        "Used by the acceptance-workflow to prepare a PR."
    ],
    subcommands = [ClearScreen::class, CommandLine.HelpCommand::class]
)
class ClientsideApplyCommand : Command {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
    private val objectMapper = jacksonObjectMapper()

    private val defaultGenerationConfig =
        "serverpackcreator-api/src/main/kotlin/de/griefed/serverpackcreator/api/settings/GenerationConfig.kt"
    private val defaultProperties =
        "serverpackcreator-api/src/main/resources/serverpackcreator.properties"

    /** The report-JSON (the block the verify-workflow embeds in its comment), supplied via option. */
    @CommandLine.Option(
        names = ["-r", "--report"],
        description = ["Path to the clientside-report JSON to read the suggested entries from."],
        required = true
    )
    private var report: String? = null

    @CommandLine.Option(
        names = ["--generation-config"],
        description = ["Path to GenerationConfig.kt (defaults to the in-repo location)."],
        required = false
    )
    private var generationConfig: String? = null

    @CommandLine.Option(
        names = ["--properties"],
        description = ["Path to serverpackcreator.properties (defaults to the in-repo location)."],
        required = false
    )
    private var properties: String? = null

    override fun run() {
        apply(File(report ?: return), generationConfig?.let { File(it) }, properties?.let { File(it) })
    }

    /**
     * Read the suggested entries (and the project-link used as their documenting comment) from
     * [reportFile] and insert any new ones into the fallback-list files, defaulting to their in-repo
     * locations when [generationConfigFile]/[propertiesFile] are not given.
     */
    fun apply(reportFile: File, generationConfigFile: File? = null, propertiesFile: File? = null) {
        val report = objectMapper.readTree(reportFile.readText())
        val projectUrl = report.path("projectUrl").asText(null)
        val entries = report.path("suggestedEntries").map { Entry(it.asText(), projectUrl) }
        if (entries.isEmpty()) {
            println("No suggested entries in ${reportFile.name}; nothing to apply.")
            return
        }

        editFile(generationConfigFile ?: File(defaultGenerationConfig)) { ClientsideListEditor.addToKotlin(it, entries) }
        editFile(propertiesFile ?: File(defaultProperties)) { ClientsideListEditor.addToProperties(it, entries) }
        println("Applied ${entries.size} entr${if (entries.size == 1) "y" else "ies"}: ${entries.joinToString(", ") { it.value }}")
    }

    /** Read [file], run [transform], and write back only when the content actually changed. */
    private fun editFile(file: File, transform: (String) -> String) {
        if (!file.isFile) {
            log.warn("${file.absolutePath} not found; skipping.")
            return
        }
        val original = file.readText()
        val updated = transform(original)
        if (updated != original) {
            file.writeText(updated)
            log.info("Updated ${file.absolutePath}")
        } else {
            log.info("No changes for ${file.absolutePath} (entries already present?).")
        }
    }
}
