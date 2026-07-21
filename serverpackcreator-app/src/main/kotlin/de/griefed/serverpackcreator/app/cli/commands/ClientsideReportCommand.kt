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
package de.griefed.serverpackcreator.app.cli.commands

import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.clientside.*
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import picocli.CommandLine
import picocli.shell.jline3.PicocliCommands.ClearScreen
import java.io.File

/**
 * Produces the clientside-verification report for a Modrinth/CurseForge project-link and prints it as
 * a ready-to-post Markdown comment (with an embedded machine-readable JSON block). This is the entry
 * point the verify-workflow runs; the workflow merely posts the stdout as the issue-comment.
 *
 * The CurseForge API-key is read from the `CURSEFORGE_API_KEY` environment-variable; it is only
 * required for CurseForge links.
 *
 * @param apiWrapper Provides the mod-scanner and download utilities.
 * @author Griefed
 */
@CommandLine.Command(
    name = "clientsidereport", mixinStandardHelpOptions = true,
    description = [
        "Assess whether the mod behind a Modrinth/CurseForge link is clientside-only.",
        "Prints a Markdown report for the maintainer to review."
    ],
    subcommands = [ClearScreen::class, CommandLine.HelpCommand::class]
)
class ClientsideReportCommand(private val apiWrapper: ApiWrapper = ApiWrapper.api()) : Command {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

    /** The CurseForge/Modrinth project-link, supplied interactively via the option below. */
    @CommandLine.Option(
        names = ["-u", "--url"],
        description = ["The CurseForge or Modrinth project-link from the issue."],
        required = true
    )
    private var url: String? = null

    /** Optional file to write the Markdown to instead of stdout (keeps it free of log-output). */
    @CommandLine.Option(
        names = ["-o", "--output"],
        description = ["Write the Markdown report to this file instead of stdout."],
        required = false
    )
    private var output: String? = null

    override fun run() {
        report(url ?: return, output?.let { File(it) })
    }

    /**
     * Build the Markdown report for [projectUrl] and emit it to [outputFile] (or stdout when `null`).
     * Failures are rendered as a short Markdown note rather than thrown, so the workflow always has a
     * comment to post.
     */
    fun report(projectUrl: String, outputFile: File? = null) {
        val markdown = try {
            val verifier = ClientsideVerifier(
                platforms = supportedPlatforms(),
                metadataScanner = MetadataScanner(apiWrapper.modScanner),
                jarDownloader = HttpJarDownloader(apiWrapper.webUtilities),
                workDirectory = File(apiWrapper.apiProperties.homeDirectory, "work/clientside-verify")
            )
            ClientsideReportRenderer.renderMarkdown(verifier.report(projectUrl))
        } catch (ex: Exception) {
            log.error("Clientside report failed for $projectUrl", ex)
            "${ClientsideReportRenderer.MARKER}\n## 🤖 Clientside-mod verification failed\n\n" +
                    "Could not assess `$projectUrl`: ${ex.message}\n"
        }
        if (outputFile != null) {
            outputFile.parentFile?.mkdirs()
            outputFile.writeText(markdown)
            log.info("Clientside report written to ${outputFile.absolutePath}")
        } else {
            println(markdown)
        }
    }
}
