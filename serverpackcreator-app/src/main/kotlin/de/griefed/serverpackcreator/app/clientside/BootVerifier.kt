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
package de.griefed.serverpackcreator.app.clientside

import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.api.config.PackConfig
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The production-near half of the signal: for one loader it force-includes the candidate mod (and its
 * required dependencies) into a freshly generated server pack and boots it, watching for a crash.
 *
 * This is the only signal that catches a mod that *declares* server/both yet actually crashes a
 * server — the metadata can't, because the declaration itself is the lie. Reuses SPC's own
 * generation + the ServerStarterJar (which self-installs the loader server on first run), so no
 * loader-installer machinery is reinvented here.
 *
 * @param apiWrapper            Generation + config + version-meta + properties.
 * @param platform             The hosting platform, for recursive dependency resolution.
 * @param httpDownloader       Downloads freely-distributable files.
 * @param browserDownloader    Downloads distribution-locked files (headless browser).
 * @param loaderVersionResolver Picks the loader-version to install.
 * @param workDirectory        Scratch root for the synthetic modpack and generated server pack.
 * @param bootTimeout          Budget for install + boot before declaring the run inconclusive.
 * @author Griefed
 */
class BootVerifier(
    private val apiWrapper: ApiWrapper,
    private val platform: ModPlatform,
    private val httpDownloader: JarDownloader,
    private val browserDownloader: JarDownloader,
    private val loaderVersionResolver: LoaderVersionResolver,
    private val workDirectory: File,
    private val bootTimeout: Duration = Duration.ofMinutes(12)
) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

    /** Maximum dependency-graph depth to resolve, guarding against cycles/runaway graphs. */
    private val maxDependencyDepth = 4

    /** The vanilla server's ready-line, watched while streaming the boot. */
    private val readyLine = Regex("""Done \([^)]*\)! For help""")

    /**
     * Result of a single boot-attempt: the verdict, the captured log-file, a human-readable note, and
     * (on a crash) the excerpt of the console-output around the failure for in-comment analysis.
     */
    data class BootOutcome(
        val result: BootResult,
        val logFile: File?,
        val detail: String,
        val crashExcerpt: String? = null
    )

    /**
     * Boot the [project]'s newest file for [loader] (with its required dependencies) and classify the
     * outcome. Any preparation failure (no bootable loader/MC combo, download or generation failure)
     * is reported as [BootResult.INCONCLUSIVE] rather than thrown.
     */
    fun verify(project: ProjectFiles, loader: String): BootOutcome {
        val candidate = BootCandidateSelector.pickBootableCandidate(project.files, loader) { minecraftVersion ->
            loaderVersionResolver.latest(loader, minecraftVersion) != null
        } ?: return BootOutcome(BootResult.INCONCLUSIVE, null, "No bootable file/Minecraft/loader combination for $loader.")
        val (mainFile, minecraftVersion) = candidate
        val loaderVersion = loaderVersionResolver.latest(loader, minecraftVersion)
            ?: return BootOutcome(BootResult.INCONCLUSIVE, null, "No $loader version for Minecraft $minecraftVersion.")

        val attemptDir = File(workDirectory, "${project.slug}-$loader").apply { deleteRecursively() }
        val modsDir = File(attemptDir, "modpack/mods").apply { mkdirs() }

        if (!downloadWithDependencies(mainFile, loader, minecraftVersion, modsDir, mutableSetOf(), 0)) {
            return BootOutcome(BootResult.INCONCLUSIVE, null, "Could not download ${mainFile.fileName} (or a dependency).")
        }

        val serverPack = generateServerPack(File(attemptDir, "modpack"), File(attemptDir, "serverpack"), minecraftVersion, loader, loaderVersion)
            ?: return BootOutcome(BootResult.INCONCLUSIVE, null, "Server-pack generation failed for $loader $minecraftVersion.")

        return boot(serverPack, File(attemptDir, "boot.log"), minecraftVersion, loader, loaderVersion)
    }

    /**
     * Download [file] and, recursively up to [maxDependencyDepth], its required dependencies into
     * [modsDir]. Locked files go through the [browserDownloader], everything else through the
     * [httpDownloader]. Returns false only if the main file itself could not be obtained; a missing
     * dependency is logged but does not abort (the boot may still be meaningful).
     */
    private fun downloadWithDependencies(
        file: ModFile,
        loader: String,
        minecraftVersion: String,
        modsDir: File,
        visited: MutableSet<String>,
        depth: Int
    ): Boolean {
        if (selectDownloader(file, httpDownloader, browserDownloader).download(file, modsDir) == null) {
            return false
        }
        if (depth >= maxDependencyDepth) {
            return true
        }
        for (dependencyRef in file.requiredDependencies) {
            if (!visited.add(dependencyRef)) {
                continue
            }
            val dependencyProject = platform.resolveDependency(dependencyRef) ?: continue
            val dependencyFile = BootCandidateSelector.pickDependencyFile(dependencyProject.files, loader, minecraftVersion)
            if (dependencyFile == null) {
                log.warn("No $loader file for dependency '$dependencyRef'; booting without it.")
                continue
            }
            downloadWithDependencies(dependencyFile, loader, minecraftVersion, modsDir, visited, depth + 1)
        }
        return true
    }

    /**
     * Generate a self-installing server pack from the synthetic [modpackDir] with mod auto-exclusion
     * disabled (so the candidate mod is kept). Returns the server-pack directory, or `null` on a
     * failed config-check or generation.
     */
    private fun generateServerPack(
        modpackDir: File,
        destination: File,
        minecraftVersion: String,
        loader: String,
        loaderVersion: String
    ): File? {
        // The candidate mod must survive generation, so turn off the scanner-driven exclusion and
        // start from an empty clientside-list for this dedicated verification run.
        apiWrapper.apiProperties.isAutoExcludingModsEnabled = false

        val packConfig = PackConfig()
        packConfig.modpackDir = modpackDir.absolutePath
        packConfig.minecraftVersion = minecraftVersion
        packConfig.modloader = loader
        packConfig.modloaderVersion = loaderVersion
        packConfig.clientMods.clear()
        packConfig.customDestination = Optional.of(destination)

        val check = apiWrapper.configurationHandler.checkConfiguration(packConfig)
        if (!check.allChecksPassed) {
            log.warn("Config-check failed for $loader $minecraftVersion: ${check.encounteredErrors}")
            return null
        }
        val generation = apiWrapper.serverPackHandler.run(packConfig)
        if (!generation.success) {
            log.warn("Generation failed for $loader $minecraftVersion: ${generation.errors}")
            return null
        }
        return generation.serverPack
    }

    /**
     * Boot the generated [serverPack] via its `start.sh`, streaming the console to [logFile] while
     * watching for the ready-line, then classify. The process is force-killed once ready or once the
     * [bootTimeout] elapses.
     */
    private fun boot(serverPack: File, logFile: File, minecraftVersion: String, loader: String, loaderVersion: String): BootOutcome {
        val startScript = File(serverPack, "start.sh")
        if (!startScript.isFile) {
            return BootOutcome(BootResult.INCONCLUSIVE, null, "No start.sh in the generated server pack.")
        }
        File(serverPack, "eula.txt").writeText("eula=true\n")
        startScript.setExecutable(true)

        log.info("Booting $loader $loaderVersion (Minecraft $minecraftVersion) server pack at ${serverPack.absolutePath}")
        val process = ProcessBuilder("bash", startScript.name)
            .directory(serverPack)
            .redirectErrorStream(true)
            .redirectInput(ProcessBuilder.Redirect.from(File("/dev/null")))
            .start()

        val lines = ArrayList<String>()
        val ready = AtomicBoolean(false)
        val readerThread = Thread {
            process.inputStream.bufferedReader().useLines { sequence ->
                for (line in sequence) {
                    synchronized(lines) { lines.add(line) }
                    if (readyLine.containsMatchIn(line)) {
                        ready.set(true)
                    }
                }
            }
        }.apply { isDaemon = true; start() }

        val deadline = System.currentTimeMillis() + bootTimeout.toMillis()
        while (process.isAlive && !ready.get() && System.currentTimeMillis() < deadline) {
            Thread.sleep(500)
        }
        val timedOut = !ready.get() && System.currentTimeMillis() >= deadline

        if (process.isAlive) {
            process.destroyForcibly()
            process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS)
        }
        readerThread.join(5_000)
        val exitCode = if (process.isAlive) null else runCatching { process.exitValue() }.getOrNull()

        val captured = synchronized(lines) { ArrayList(lines) }
        logFile.writeText(captured.joinToString("\n"))
        val result = BootLogClassifier.classify(captured, exitCode, timedOut)
        val crashExcerpt = if (result == BootResult.CRASHED) BootLogExcerpt.crashExcerpt(captured) else null
        return BootOutcome(result, logFile, "$loader $loaderVersion / Minecraft $minecraftVersion → $result", crashExcerpt)
    }
}
