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
package de.griefed.serverpackcreator.grinder

import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.clientside.ClientsideReportRenderer
import de.griefed.serverpackcreator.clientside.boot.ImageJavaRuntimes
import de.griefed.serverpackcreator.clientside.platform.CurseForgeApiKey
import de.griefed.serverpackcreator.clientside.verdict.ClientsideReport
import de.griefed.serverpackcreator.grinder.container.ContainerEngine
import de.griefed.serverpackcreator.grinder.container.ContainerResources
import de.griefed.serverpackcreator.grinder.container.ContainerUser
import de.griefed.serverpackcreator.grinder.container.DockerJavaContainerEngine
import de.griefed.serverpackcreator.grinder.container.RuntimeImagePreflight
import de.griefed.serverpackcreator.grinder.loader.ApiVanillaPackGenerator
import de.griefed.serverpackcreator.grinder.loader.DockerLoaderInstaller
import de.griefed.serverpackcreator.grinder.loader.LoaderCache
import de.griefed.serverpackcreator.grinder.loader.TemplateProvenance
import de.griefed.serverpackcreator.grinder.report.BootLogStore
import de.griefed.serverpackcreator.grinder.source.RequeueSelection
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import java.time.Duration

/** A verified project and each target's decisive console tail, keyed by `(loader, minecraftLine)`. */
data class ConsoledReport(
    val report: ClientsideReport,
    val consoles: Map<Pair<String, String>, List<String>>
)

/**
 * `--clientside-report <url> --output <file>`: verify one project the way the daemon grinds it — downloads on the
 * host, the loader installed from a vanilla pack, the mod booted in the hardened `--network none` container — and
 * write the clientside-mod report `-verifyclientside` writes, then exit. Nothing is recorded: no verdict store, no
 * crawl cursor, no report server.
 *
 * [verifier] is built only after the runtime image is known to exist, because building it boots an `ApiWrapper`.
 * Every failure still writes a report, the renderer's failure comment, so the issue is told what happened.
 */
class ClientsideReportRun(
    private val engine: ContainerEngine,
    private val image: String,
    private val verifier: () -> (GrindCandidate) -> ConsoledReport
) {
    private val log by lazy { cachedLoggerOf(ClientsideReportRun::class.java) }

    /** Write [projectUrl]'s report to [output]; `0` when the project was verified, `1` when only a failure note was written. */
    fun run(projectUrl: String, output: File): Int {
        val (markdown, exitCode) = try {
            render(projectUrl) to 0
        } catch (ex: Exception) {
            log.error("Clientside verification failed for $projectUrl", ex)
            ClientsideReportRenderer.renderFailure(projectUrl, ex.message) to 1
        }
        output.absoluteFile.parentFile?.mkdirs()
        output.writeText(markdown)
        log.info("Clientside verification report written to ${output.absolutePath}")
        return exitCode
    }

    private fun render(projectUrl: String): String {
        val candidate = RequeueSelection.fromLinks(listOf(projectUrl)).first.singleOrNull()
            ?: throw IllegalArgumentException("Not a Modrinth or CurseForge project link.")
        RuntimeImagePreflight.refusalFor(engine, image)?.let { throw IllegalStateException(it) }
        val verified = verifier()(candidate)
        return ClientsideReportRenderer.renderMarkdown(verified.report, EVIDENCE_NOTE) { target ->
            target.minecraftLine?.let { line -> verified.consoles[target.loader to line] }
        }
    }

    /** The command line this mode takes, and its composition from the daemon's configuration. */
    companion object {
        /** The flag selecting this mode; it must be the first argument. */
        const val FLAG = "--clientside-report"

        /** How to call this mode, printed when the arguments do not parse. */
        const val USAGE = "Usage: $FLAG <Modrinth or CurseForge project link> --output <report file>"

        /** The project link and output file from [args], or `null` when they are not exactly `FLAG <url> --output <file>`. */
        fun parse(args: Array<String>): Pair<String, File>? {
            if (args.size != 4 || args[0] != FLAG || args[2] != "--output" || args[1].isBlank() || args[3].isBlank()) {
                return null
            }
            return args[1] to File(args[3])
        }

        /**
         * Compose the run from [config] the way `GrinderApplication.main` composes the daemon, minus everything that
         * records or serves, and run it. The caller has already claimed SPC's Preferences node and pinned its home.
         *
         * Two differences from the daemon are deliberate: no `reapOrphans`, because on a host that also runs the
         * daemon it would stop the daemon's containers; and the CLI's boot budget and Minecraft-line policy, so the
         * report reads like `-verifyclientside`'s. The engine is closed by a shutdown hook, which `exitProcess` and a
         * cancelled job's SIGTERM both run, so no container outlives the process.
         */
        internal fun execute(config: GrinderConfiguration, home: File, projectUrl: String, output: File): Int {
            val workDir = config.work.apply { mkdirs() }
            val cacheRoot = config.cache.apply { mkdirs() }
            val resources = ContainerResources.forLimits(config.containerCpus, config.containerMemoryGiB)
            val containerUser = ContainerUser.forDirectory(workDir, config.containerUser)
            val engine = DockerJavaContainerEngine()
            Runtime.getRuntime().addShutdownHook(Thread { runCatching { engine.close() } })
            return ClientsideReportRun(engine, config.image) {
                val apiWrapper = ApiWrapper.api(GrinderApplication.resolveSpcPropertiesFile(config.spcProperties, home))
                val imageJava = ImageJavaRuntimes.from(apiWrapper.versionMeta.minecraft)
                val installer = DockerLoaderInstaller(
                    engine, config.image, ApiVanillaPackGenerator(apiWrapper, File(workDir, "install")), imageJava,
                    resources = resources, containerUser = containerUser
                )
                val cache = LoaderCache(cacheRoot, installer, templateProvenance = {
                    TemplateProvenance.digestOf(apiWrapper.apiProperties.defaultStartScriptTemplates().values.map { File(it) })
                })
                ContainerCandidateVerifier(
                    apiWrapper, cache, engine, config.image, imageJava, File(workDir, "verify"),
                    bootTimeout = CLI_BOOT_TIMEOUT,
                    resources = resources,
                    curseForgeApiKey = CurseForgeApiKey.fromEnvironment,
                    containerUser = containerUser,
                    crashLogs = BootLogStore(config.bootLogs, config.bootLogBudgetBytes),
                    keepUnbootableLines = true
                )::verifyWithConsoles
            }.run(projectUrl, output)
        }

        /**
         * Where the evidence behind the shown consoles is, for the issue reader: the boot-log store, which
         * `BootArtifacts.worthKeeping` fills and the workflow uploads as the evidence block's **All logs** archive.
         */
        const val EVIDENCE_NOTE = "Every attempt that did not survive keeps its full console, with the server's own " +
            "logs and crash reports, in the **All logs** archive linked under Evidence below."

        /** `BootVerifier`'s default budget, which `-verifyclientside` boots with. */
        private val CLI_BOOT_TIMEOUT: Duration = Duration.ofMinutes(12)
    }
}
