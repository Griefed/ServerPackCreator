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
import de.griefed.serverpackcreator.api.versionmeta.minecraft.MinecraftMeta
import de.griefed.serverpackcreator.clientside.boot.BootArtifactWriter
import de.griefed.serverpackcreator.clientside.boot.BootVerifier
import de.griefed.serverpackcreator.clientside.boot.ConsoleTail
import de.griefed.serverpackcreator.clientside.boot.HostJavaSelection
import de.griefed.serverpackcreator.clientside.boot.ImageJavaRuntimes
import de.griefed.serverpackcreator.clientside.ClientsideReportRenderer
import de.griefed.serverpackcreator.clientside.ClientsideVerifier
import de.griefed.serverpackcreator.clientside.platform.HttpJarDownloader
import de.griefed.serverpackcreator.clientside.version.LoaderVersionResolver
import de.griefed.serverpackcreator.clientside.metadata.MetadataScanner
import de.griefed.serverpackcreator.clientside.platform.supportedPlatforms
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import picocli.CommandLine
import picocli.shell.jline3.PicocliCommands.ClearScreen
import java.io.File

/**
 * Like [ClientsideReportCommand] but adds the **server-boot** signal: for each target it generates a
 * server pack with the candidate mod force-included and boots it, so a mod whose crash a rule recognises
 * is `CONFIRMED` even when its metadata claims it is server-safe.
 *
 * A target is one Minecraft version-line, under the first loader that line has a bootable build for --
 * `BootCandidateSelector.pickGrindTargets`, not one boot per loader: sideness is a property of a build,
 * and builds differ far more across Minecraft eras than across loaders of one era.
 *
 * A distribution-locked CurseForge file (`allowModDistribution=false`) is **not** fetched: such a file
 * publishes no download URL. Those projects report `LOCKED` and are verified from Modrinth instead.
 *
 * The CurseForge API-key comes from `CurseForgeApiKey` (CurseForge links only); the booted servers never
 * inherit it. Heavier and slower than `clientsidereport` — it installs and boots a server per
 * target.
 *
 * [apiWrapper] — Provides generation, version-meta, scanner and download utilities.
 *
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

    /** Assess a project with a real server boot on top of the metadata — the expensive pass, and the only one whose crash is decisive. */
    override fun run() {
        verify(url ?: return, output?.let { File(it) })
    }

    /**
     * Build the metadata+boot report for [projectUrl] and emit it to [outputFile] (or stdout when
     * `null`). Failures render as a short Markdown note rather than throwing, so the workflow always
     * has a comment to post.
     *
     * **Distribution-locked CurseForge files cannot be verified here**, because they publish no download
     * URL — see `HttpJarDownloader`. Such a project is verified from Modrinth instead.
     */
    fun verify(projectUrl: String, outputFile: File? = null) {
        val workDirectory = File(apiWrapper.apiProperties.homeDirectory, "work/clientside-verify")
        val bootDirectory = File(workDirectory, "boot")
        // Beside `boot/`, never inside it: `stageBootPack` deletes and re-creates `boot/<attemptName>`
        // before every attempt, so evidence kept in there lives until the next boot and no longer.
        val artifactWriter = BootArtifactWriter(File(workDirectory, "boot-evidence"))
        val markdown = try {
            run {
                val httpDownloader = HttpJarDownloader(apiWrapper.webUtilities)
                val platforms = supportedPlatforms()
                val packJava = packJavaSelection(
                    apiWrapper.versionMeta.minecraft,
                    System.getenv(),
                    Runtime.version().feature(),
                    File(System.getProperty("java.home"))
                )
                val verifier = ClientsideVerifier(
                    platforms = platforms,
                    metadataScanner = MetadataScanner(apiWrapper.modScanner),
                    jarDownloader = httpDownloader,
                    workDirectory = workDirectory,
                    bootVerifierFactory = { platform ->
                        BootVerifier(
                            apiWrapper = apiWrapper,
                            platform = platform,
                            httpDownloader = httpDownloader,
                            loaderVersionPolicy = LoaderVersionResolver(apiWrapper.versionMeta),
                            workDirectory = bootDirectory,
                            // A dependency this platform cannot supply may exist on the other one, and the
                            // staged file is just a jar. Empty when no CurseForge key is configured.
                            alternatePlatforms = platforms.filter { it !== platform },
                            // A missing JDK becomes an ERROR naming the version, where booting on the wrong
                            // Java ends in start.sh's Jabba prompt. No `minecraftAcceptable` gate, unlike the
                            // grinder: a host lacking a JDK should say so in the report, not drop the line.
                            packPostProcessor = packJava,
                            // Per attempt, because this is the only moment a re-check's pack still exists.
                            // A thrown sink is logged and ignored by `BootVerifier`, so a full disk cannot
                            // cost a verdict that already ran.
                            bootArtifactSink = { staged, outcome -> artifactWriter.keep(staged, outcome) }
                        )
                    }
                )
                val report = verifier.report(projectUrl)
                ClientsideReportRenderer.renderMarkdown(report) { target ->
                    target.minecraftLine
                        ?.let { line -> BootVerifier.decisiveConsole(bootDirectory, report.platform, report.slug, target.loader, line) }
                        ?.takeIf { it.isFile }
                        ?.let { console -> ConsoleTail.lastLines(console, ClientsideReportRenderer.MAX_CONSOLE_LINES) }
                }
            }
        } catch (ex: Exception) {
            log.error("Clientside verification failed for $projectUrl", ex)
            ClientsideReportRenderer.renderFailure(projectUrl, ex.message)
        }
        if (outputFile != null) {
            outputFile.parentFile?.mkdirs()
            outputFile.writeText(markdown)
            log.info("Clientside verification report written to ${outputFile.absolutePath}")
        } else {
            println(markdown)
        }
    }

    companion object {
        /**
         * The post-processor every boot of this verb applies: it points the staged pack at the JDK its Minecraft
         * version requires, chosen from the homes [environment] advertises as `JAVA_HOME_<major>_<arch>` plus the
         * JVM running SPC, so a host that advertises none still boots every version its own Java serves.
         */
        internal fun packJavaSelection(
            minecraftMeta: MinecraftMeta,
            environment: Map<String, String>,
            runningJavaMajor: Int,
            runningJavaHome: File
        ): HostJavaSelection {
            val javaHomes = mapOf(runningJavaMajor to runningJavaHome) + ImageJavaRuntimes.javaHomesIn(environment)
            return HostJavaSelection(ImageJavaRuntimes.installed(minecraftMeta, javaHomes))
        }
    }
}
