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
 * Like [ClientsideReportCommand] but adds the **server-boot** signal: for each loader it generates a
 * server pack with the candidate mod force-included and boots it, so a mod that crashes a server is
 * flagged with HIGH confidence even when its metadata claims it is server-safe. Distribution-locked
 * CurseForge files are fetched via an embedded headless browser.
 *
 * The CurseForge API-key is read from the `CURSEFORGE_API_KEY` environment-variable (CurseForge
 * links only). Heavier and slower than `clientsidereport` — it installs and boots a server per
 * loader.
 *
 * @param apiWrapper Provides generation, version-meta, scanner and download utilities.
 * @author Griefed
 */
@CommandLine.Command(
    name = "verifyclientside", mixinStandardHelpOptions = true,
    description = [
        "Assess whether a Modrinth/CurseForge mod is clientside-only, including a server-boot test.",
        "Prints a Markdown report for the maintainer to review."
    ],
    subcommands = [ClearScreen::class, CommandLine.HelpCommand::class]
)
class VerifyClientsideCommand(private val apiWrapper: ApiWrapper = ApiWrapper.api()) : Command {
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
        verify(url ?: return, output?.let { File(it) })
    }

    /**
     * Build the metadata+boot report for [projectUrl] and emit it to [outputFile] (or stdout when
     * `null`). Failures render as a short Markdown note rather than throwing, so the workflow always
     * has a comment to post. The headless browser is disposed once the run completes.
     */
    fun verify(projectUrl: String, outputFile: File? = null) {
        val workDirectory = File(apiWrapper.apiProperties.homeDirectory, "work/clientside-verify")
        val markdown = try {
            BrowserDownloader().use { browserDownloader ->
                val httpDownloader = HttpJarDownloader(apiWrapper.webUtilities)
                val verifier = ClientsideVerifier(
                    platforms = supportedPlatforms(),
                    metadataScanner = MetadataScanner(apiWrapper.modScanner),
                    jarDownloader = httpDownloader,
                    workDirectory = workDirectory,
                    bootVerifierFactory = { platform ->
                        BootVerifier(
                            apiWrapper = apiWrapper,
                            platform = platform,
                            httpDownloader = httpDownloader,
                            browserDownloader = browserDownloader,
                            loaderVersionPolicy = LoaderVersionResolver(apiWrapper.versionMeta),
                            workDirectory = File(workDirectory, "boot")
                        )
                    }
                )
                ClientsideReportRenderer.renderMarkdown(verifier.report(projectUrl))
            }
        } catch (ex: Exception) {
            log.error("Clientside verification failed for $projectUrl", ex)
            "${ClientsideReportRenderer.MARKER}\n## 🤖 Clientside-mod verification failed\n\n" +
                    "Could not assess `$projectUrl`: ${ex.message}\n"
        }
        if (outputFile != null) {
            outputFile.parentFile?.mkdirs()
            outputFile.writeText(markdown)
            log.info("Clientside verification report written to ${outputFile.absolutePath}")
        } else {
            println(markdown)
        }
    }
}
