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
import de.griefed.serverpackcreator.clientside.BootVerifier
import de.griefed.serverpackcreator.clientside.BrowserDownloader
import de.griefed.serverpackcreator.clientside.ClientsideReport
import de.griefed.serverpackcreator.clientside.ClientsideVerifier
import de.griefed.serverpackcreator.clientside.HttpJarDownloader
import de.griefed.serverpackcreator.clientside.LoaderVersionResolver
import de.griefed.serverpackcreator.clientside.MetadataScanner
import de.griefed.serverpackcreator.clientside.supportedPlatforms
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import java.time.Duration

/**
 * The production [CandidateVerifier]: drives the clientside `ClientsideVerifier` (metadata + boot)
 * with the boot wired to run in an **isolated container** instead of a host process. The decisive
 * piece is the `packPostProcessor` hook — between staging and boot it ensures the loader is installed
 * (once per tuple, with network), overlays that cached install layer into the pack, and sets the
 * offline-boot levers, so the actual mod-boot runs under `--network none`.
 *
 * Integration-only: a live daemon, the runtime image and a real `ApiWrapper` are required, so this is
 * exercised end-to-end, not unit-tested; the pieces it composes are individually tested.
 *
 * @param apiWrapper      SPC resolution + generation + version metadata.
 * @param loaderCache     The per-tuple install cache (its installer boots-with-network on a miss).
 * @param containerEngine The container runtime the mod-boots use.
 * @param runtimeImage    The image carrying the JDKs + SPC's shell tooling.
 * @param imageJava       The image's supported-Java gate + per-version JDK resolution.
 * @param workDirectory   Scratch root for verification + boot staging.
 * @param bootTimeout     Per-boot budget.
 * @param resources       CPU/memory/pid caps per mod-boot container.
 * @param curseForgeApiKey CurseForge key (CF resolution); Modrinth needs none.
 * @author Griefed
 */
class ContainerCandidateVerifier(
    private val apiWrapper: ApiWrapper,
    private val loaderCache: LoaderCache,
    private val containerEngine: ContainerEngine,
    private val runtimeImage: String,
    private val imageJava: ImageJavaRuntimes,
    private val workDirectory: File,
    private val bootTimeout: Duration = Duration.ofMinutes(15),
    private val resources: ContainerResources = ContainerResources(),
    private val curseForgeApiKey: String? = System.getenv("CURSEFORGE_API_KEY")
) : CandidateVerifier {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

    override fun verify(candidate: GrindCandidate): ClientsideReport {
        val httpDownloader = HttpJarDownloader(apiWrapper.webUtilities)
        // The browser is only launched for distribution-locked CurseForge files; disposed after the run.
        return BrowserDownloader().use { browserDownloader ->
            ClientsideVerifier(
                platforms = supportedPlatforms(curseForgeApiKey),
                metadataScanner = MetadataScanner(apiWrapper.modScanner),
                jarDownloader = httpDownloader,
                workDirectory = File(workDirectory, "verify"),
                bootVerifierFactory = { platform ->
                    BootVerifier(
                        apiWrapper = apiWrapper,
                        platform = platform,
                        httpDownloader = httpDownloader,
                        browserDownloader = browserDownloader,
                        loaderVersionResolver = LoaderVersionResolver(apiWrapper.versionMeta),
                        workDirectory = File(workDirectory, "boot"),
                        serverRunner = ContainerServerRunner(containerEngine, runtimeImage, resources),
                        packPostProcessor = ::overlayLoaderInstall,
                        minecraftAcceptable = imageJava::supports,
                        bootTimeout = bootTimeout
                    )
                }
            ).report(candidate.projectUrl)
        }
    }

    /**
     * The cache-overlay hook: ensure the loader is installed for the pack's tuple, copy that install
     * layer into the staged pack, then set the offline-boot levers. A missing install throws — the
     * [BootVerifier] reports it INCONCLUSIVE rather than booting a pack that would need network.
     */
    private fun overlayLoaderInstall(pack: BootVerifier.Prepared.Ready) {
        val javaPath = imageJava.javaPath(pack.minecraftVersion)
            ?: throw IllegalStateException("No bundled JDK for Minecraft ${pack.minecraftVersion}")
        val base = loaderCache.ensureInstalled(pack.loader, pack.loaderVersion, pack.minecraftVersion)
            ?: throw IllegalStateException("No cached loader install for ${pack.loader} ${pack.loaderVersion} / Minecraft ${pack.minecraftVersion}")
        copyInstallLayer(base, pack.serverPack)
        PackVariables.prepareUnattended(pack.serverPack, javaPath, offline = true)
    }

    /** Copy the cached install layer from [base] into [pack], skipping the cache's completion marker. */
    private fun copyInstallLayer(base: File, pack: File) {
        base.walkTopDown().filter { it.isFile && it.name != LoaderCache.MARKER }.forEach { file ->
            val destination = File(pack, file.relativeTo(base).path)
            destination.parentFile?.mkdirs()
            file.copyTo(destination, overwrite = true)
        }
    }
}
