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
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import java.util.concurrent.CountDownLatch

/**
 * The fire-and-forget entry point. Wires the real chain — [ModrinthCandidateSource] (or project URLs
 * from the command line) → [GrindPool] over a [Grinder] backed by the [ContainerCandidateVerifier] and
 * a restart-safe [JsonVerdictStore] — and serves the live table/CSV via [ReportServer]. Configured by
 * environment variables so the daemon needs no flags; pass project URLs as args to grind a fixed set
 * (handy for an end-to-end verification), or none to pull the top Modrinth mods by downloads.
 *
 * @author Griefed
 */
object GrinderApplication {
    private val log by lazy { cachedLoggerOf(GrinderApplication::class.java) }

    @JvmStatic
    fun main(args: Array<String>) {
        val base = File(System.getProperty("user.home"), ".spc-grinder")
        val image = env("SPC_GRINDER_IMAGE", "spc-grinder-runtime:latest")
        val workDir = File(env("SPC_GRINDER_WORK", File(base, "work").path)).apply { mkdirs() }
        val cacheRoot = File(env("SPC_GRINDER_CACHE", File(base, "cache").path)).apply { mkdirs() }
        val storeFile = File(env("SPC_GRINDER_STORE", File(base, "verdicts.json").path)).apply { parentFile?.mkdirs() }
        val port = env("SPC_GRINDER_PORT", "8757").toInt()
        val workers = env("SPC_GRINDER_WORKERS", "2").toInt()

        log.info("Grinder starting — image=$image work=$workDir cache=$cacheRoot store=$storeFile port=$port workers=$workers")

        // Point SPC at a specific home/config when given (reproducible runs), else its default.
        val apiWrapper = System.getenv("SPC_GRINDER_SPC_PROPERTIES")?.takeIf { it.isNotBlank() }
            ?.let { ApiWrapper.api(File(it)) }
            ?: ApiWrapper.api()
        val engine = DockerJavaContainerEngine()
        // Authoritative Minecraft -> required-Java from SPC's own metadata; gates selection to the image's JDKs.
        val imageJava = ImageJavaRuntimes.from(apiWrapper.versionMeta.minecraft)
        val installer = DockerLoaderInstaller(engine, image, ApiVanillaPackGenerator(apiWrapper, File(workDir, "install")), imageJava)
        val cache = LoaderCache(cacheRoot, installer)
        val verifier = ContainerCandidateVerifier(apiWrapper, cache, engine, image, imageJava, File(workDir, "verify"))
        val store = JsonVerdictStore(storeFile)
        val grinder = Grinder(verifier, store)

        val server = ReportServer(store, port).start()
        log.info("Report:  http://localhost:${server.port}/    CSV: http://localhost:${server.port}/export.csv")

        val candidates = if (args.isNotEmpty()) {
            args.map { GrindCandidate(it, slugFromUrl(it), 0) }
        } else {
            val limit = env("SPC_GRINDER_MODRINTH_LIMIT", "25").toInt()
            log.info("No project URLs given — pulling the top $limit Modrinth mods by downloads.")
            ModrinthCandidateSource().candidates(limit)
        }

        log.info("Grinding ${candidates.size} candidate(s) with $workers worker(s)...")
        GrindPool(grinder, workers).grindAll(candidates)
        log.info("Grind complete: ${store.all().size} verdict(s). Report stays up at http://localhost:${server.port}/ — Ctrl-C to exit.")
        CountDownLatch(1).await() // keep the report server alive
    }

    /** Read [key] from the environment, falling back to [default] when unset or blank. */
    private fun env(key: String, default: String): String = System.getenv(key)?.takeIf { it.isNotBlank() } ?: default

    /** Best-effort project-slug from a URL (last path segment) — used only for the skip-already-done check. */
    private fun slugFromUrl(url: String): String = url.substringBefore('?').trimEnd('/').substringAfterLast('/').ifBlank { url }
}
