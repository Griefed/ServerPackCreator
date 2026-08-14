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
package de.griefed.serverpackcreator.clientside

import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File

/**
 * Orchestrates the Phase-1 (metadata-only) clientside verdict: pick the hosting platform for the
 * issue-link, resolve its files, derive a list-entry per loader, and combine the platform-declared
 * sideness with SPC's jar metadata scan into a per-loader [Confidence]. The boot signal is added in
 * Phase 2; until then a clientside-looking mod tops out at [Confidence.MEDIUM].
 *
 * @param platforms          The supported hosting platforms, tried in order via [ModPlatform.handles].
 * @param metadataScanner    Reads declared sideness out of a downloaded jar.
 * @param jarDownloader      Fetches freely-distributable files for scanning (locked files are deferred).
 * @param workDirectory      Scratch directory for downloaded jars.
 * @param bootVerifierFactory When non-null, builds a [BootVerifier] for the resolved platform to add
 *                            the server-boot signal (Phase 2); when null, the report is metadata-only.
 * @author Griefed
 */
class ClientsideVerifier(
    private val platforms: List<ModPlatform>,
    private val metadataScanner: MetadataScanner,
    private val jarDownloader: JarDownloader,
    private val workDirectory: File,
    private val bootVerifierFactory: ((ModPlatform) -> BootVerifier)? = null
) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

    /**
     * Build the metadata-only report for [projectUrl].
     *
     * @throws IllegalArgumentException if no supported platform recognizes the link.
     */
    fun report(projectUrl: String): ClientsideReport {
        val platform = platforms.firstOrNull { it.handles(projectUrl) }
            ?: throw IllegalArgumentException("No supported platform (Modrinth/CurseForge) for '$projectUrl'.")
        val project = platform.resolve(projectUrl)
        val bootVerifier = bootVerifierFactory?.invoke(platform)

        val perLoader = project.loaders.map { loader -> verdictFor(project, loader, bootVerifier) }
        val suggestedEntries = perLoader.mapNotNull { it.suggestedEntry }.distinct().sorted()

        return ClientsideReport(
            platform = project.platform,
            slug = project.slug,
            projectUrl = project.projectUrl,
            phase = if (bootVerifier != null) "metadata + server-boot" else "metadata-only",
            suggestedEntries = suggestedEntries,
            perLoader = perLoader,
            fileNames = project.fileNames.sorted()
        )
    }

    /** Compute the verdict for a single [loader] of the resolved [project], optionally booting it. */
    private fun verdictFor(project: ProjectFiles, loader: String, bootVerifier: BootVerifier?): LoaderVerdict {
        val loaderFiles = project.files.filter { loader in it.loaders }
        val stem = FilenameStemDeriver.deriveStem(loaderFiles.map { it.fileName })
        val sample = loaderFiles.firstOrNull()

        val jarScan = when {
            sample == null -> JarScan.ERROR
            sample.locked -> JarScan.DEFERRED
            else -> scanSample(sample, loader, project)
        }

        val bootOutcome = bootVerifier?.let { verifier ->
            runCatching { verifier.verify(project, loader) }
                .onFailure { log.warn("Boot-test for $loader failed: ${it.message}") }
                .getOrNull()
        }

        val (confidence, note) = aggregate(project.serverSide, jarScan, bootOutcome?.result)
        return LoaderVerdict(
            loader = loader,
            suggestedEntry = stem,
            declaredClientSide = project.clientSide,
            declaredServerSide = project.serverSide,
            jarScan = jarScan,
            bootResult = bootOutcome?.result,
            bootCrashExcerpt = bootOutcome?.crashExcerpt,
            confidence = confidence,
            sampleFile = sample?.fileName,
            note = listOfNotNull(note, bootOutcome?.detail).joinToString(" ").ifBlank { null }
        )
    }

    /** Download a sample file and read its declared sideness, degrading to [JarScan.ERROR] on failure. */
    private fun scanSample(sample: ModFile, loader: String, project: ProjectFiles): JarScan {
        val minecraftVersion = sample.minecraftVersions.maxOrNull() ?: ""
        val jar = jarDownloader.download(sample, File(workDirectory, "${project.slug}-$loader"))
        if (jar == null) {
            log.warn("Could not download ${sample.fileName} for $loader; jar-scan unavailable.")
            return JarScan.ERROR
        }
        return when (metadataScanner.scan(jar, loader, minecraftVersion)) {
            MetadataScanner.Result.CLIENT -> JarScan.CLIENT
            MetadataScanner.Result.SERVER_OR_BOTH -> JarScan.SERVER_OR_BOTH
            MetadataScanner.Result.ERROR -> JarScan.ERROR
        }
    }

    /**
     * Combine the platform-declared server-side support, the jar-scan and (when run) the boot-result
     * into a confidence. A crash is the strongest single signal — it promotes any metadata to
     * [Confidence.HIGH], including the "declares server/both yet crashes" lie the metadata can't
     * catch. Without a crash, a client-leaning metadata signal is [Confidence.MEDIUM], a clear
     * server/both is [Confidence.LOW], and everything unknown/deferred is [Confidence.INCONCLUSIVE].
     */
    private fun aggregate(serverSide: DeclaredSupport, jarScan: JarScan, bootResult: BootResult?): Pair<Confidence, String?> {
        val declaresClient = serverSide == DeclaredSupport.UNSUPPORTED
        val declaresServer = serverSide == DeclaredSupport.REQUIRED
        val jarClient = jarScan == JarScan.CLIENT
        val jarServer = jarScan == JarScan.SERVER_OR_BOTH
        val metadataClient = declaresClient || jarClient
        val metadataServer = declaresServer || jarServer

        val note = when {
            declaresClient && jarServer -> "Platform marks server unsupported but the jar declares server/both."
            declaresServer && jarClient -> "Platform marks server required but the jar declares client-only."
            bootResult == BootResult.CRASHED && metadataServer -> "Declared server/both but the server crashed — a strong clientside signal."
            jarScan == JarScan.DEFERRED && bootResult == null -> "Distribution-locked file; jar-scan deferred to the boot-phase."
            else -> null
        }

        // A crash is decisive regardless of declaration; otherwise fall back to the metadata signal,
        // which boot can confirm but (short of a crash) not overturn.
        val confidence = when {
            bootResult == BootResult.CRASHED -> Confidence.HIGH
            metadataClient -> Confidence.MEDIUM
            metadataServer -> Confidence.LOW
            else -> Confidence.INCONCLUSIVE
        }
        return confidence to note
    }
}
