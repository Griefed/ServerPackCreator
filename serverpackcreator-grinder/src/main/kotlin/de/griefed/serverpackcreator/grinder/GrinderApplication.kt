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

import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.grinder.container.DockerJavaContainerEngine
import de.griefed.serverpackcreator.grinder.loader.*
import de.griefed.serverpackcreator.grinder.report.JsonVerdictStore
import de.griefed.serverpackcreator.grinder.report.ReportServer
import de.griefed.serverpackcreator.grinder.source.*
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * The fire-and-forget entry point. Wires the real chain — a [CatalogCrawler] over the available candidate
 * sources (or project URLs from the command line) → [GrindPool] over a [Grinder] backed by the
 * [ContainerCandidateVerifier] and a restart-safe [JsonVerdictStore] — and serves the live table/CSV via
 * [ReportServer]. Configured by environment variables so the daemon needs no flags.
 *
 * With no arguments it crawls: every pass takes the next slice of each platform's catalog, so left running
 * it works its way through the whole catalog and then keeps it current, resuming mid-catalog after a restart
 * ([JsonCursorStore]) and pacing itself on how much work each pass found ([GrindPacing]). Pass project URLs
 * instead to grind a fixed set once (handy for an end-to-end verification).
 *
 * @author Griefed
 */
object GrinderApplication {
    private val log by lazy { cachedLoggerOf(GrinderApplication::class.java) }

    /**
     * Entry point. With project URLs as [args] it grinds exactly those once and exits; with none it runs
     * continuously, taking the next catalogue slice each pass and persisting verdicts and crawl position after every
     * step so a restart resumes mid-catalogue. Everything else is read from the environment — see README §5.
     */
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

        claimSpcPreferencesNode()
        log.info("Using Preferences node '${ApiProperties.resolvePreferencesNode()}' for SPC settings.")

        // Point SPC at a specific config when given (reproducible runs), else one inside our own home -- never
        // ApiWrapper.api()'s relative default, see resolveSpcPropertiesFile.
        val apiWrapper = ApiWrapper.api(resolveSpcPropertiesFile(System.getenv("SPC_GRINDER_SPC_PROPERTIES"), base))
        val engine = DockerJavaContainerEngine()
        // Authoritative Minecraft -> required-Java from SPC's own metadata; gates selection to the image's JDKs.
        val imageJava = ImageJavaRuntimes.from(apiWrapper.versionMeta.minecraft)
        val installer = DockerLoaderInstaller(engine, image, ApiVanillaPackGenerator(apiWrapper, File(workDir, "install")), imageJava)
        // A cached install is a product of the start-script templates that built it, so record which ones those
        // were. Read per call rather than once: SPC resolves its templates from the then-current home, and the
        // daemon's home can be re-resolved while it runs.
        val cache = LoaderCache(cacheRoot, installer, templateProvenance = {
            TemplateProvenance.digestOf(
                apiWrapper.apiProperties.defaultStartScriptTemplates().values.map { File(it) }
            )
        })
        val verifier = ContainerCandidateVerifier(apiWrapper, cache, engine, image, imageJava, File(workDir, "verify"))
        // A run killed mid-boot leaves a staged pack that no per-candidate reap will ever come for, so sweep what
        // we inherited before adding to it. Safe here and only here: nothing is in flight yet.
        BootWorkspaceReaper(File(workDir, "verify")).reapAll().let { reclaimed ->
            if (reclaimed > 0) {
                log.info("Reclaimed ${reclaimed / 1_048_576} MiB of staging left behind by a previous run.")
            }
        }
        val store = JsonVerdictStore(storeFile)
        // Live activity record, so `/status` can answer "what is it doing right now?" (the verdict table only
        // ever answers "what has it found?").
        val status = GrinderStatus()
        val reverifyTtl = Duration.ofDays(env("SPC_GRINDER_REVERIFY_TTL_DAYS", "30").toLong())
        val grinder = Grinder(verifier, store, reverifyTtl, status = status)

        // ONE shutdown hook, registered before any boot can start so it covers the one-shot path too and
        // its ordering is unambiguous: stop pulling new candidates, then release containers whose run was
        // interrupted (the engine's per-run `finally` never executes when the JVM is torn down mid-boot).
        val running = AtomicBoolean(true)
        val activePool = AtomicReference<GrindPool?>(null)
        val mainThread = Thread.currentThread()
        Runtime.getRuntime().addShutdownHook(Thread {
            log.info("Shutdown requested — stopping the grind loop.")
            running.set(false)
            activePool.get()?.requestStop()
            runCatching { engine.close() }
                .onFailure { log.warn("Could not clean up in-flight containers: ${it.message}") }
            mainThread.interrupt()
        })

        val cursorFile = File(env("SPC_GRINDER_CURSORS", File(base, "cursors.json").path))
            .apply { parentFile?.mkdirs() }
        val cursorStore = JsonCursorStore(cursorFile)
        val server = ReportServer(store, port, status = status, cursors = cursorStore, cacheRoot = cacheRoot).start()
        log.info(
            "Report:  http://localhost:${server.port}/    CSV: http://localhost:${server.port}/export.csv" +
                "    live status: http://localhost:${server.port}/status"
        )

        if (args.isNotEmpty()) {
            // One-shot: grind a fixed set of project URLs (handy for an end-to-end verification), then
            // hold the report open. The re-verify TTL still applies, so re-running skips fresh verdicts.
            val candidates = args.map { GrindCandidate(it, slugFromUrl(it), 0, ModPlatforms.ofUrl(it)) }
            log.info("One-shot run: grinding ${candidates.size} candidate(s) with $workers worker(s)...")
            GrindPool(grinder, workers).grindAll(candidates) // one-shot: no crawl cursor to advance
            log.info("Grind complete: ${store.all().size} verdict(s). Report stays up at http://localhost:${server.port}/ — Ctrl-C to exit.")
            // Park until the shutdown hook interrupts us. Catching the interrupt is the point: the hook calls
            // `mainThread.interrupt()`, and letting that escape printed a bare `Exception in thread "main"
            // java.lang.InterruptedException` over an otherwise clean Ctrl-C — the same defect that was fixed
            // inside `GrindPool.grindAll` for the continuous path.
            runCatching { CountDownLatch(1).await() }
                .onFailure { log.info("Report server stopped.") }
            return
        }

        // Continuous fire-and-forget: each pass takes the *next* slice of every platform's catalog and
        // grinds it, so coverage keeps extending instead of re-checking the same most-downloaded projects.
        // The crawl position is persisted per platform, so a restart resumes mid-catalog; once a source runs
        // out the crawler wraps to the top and the re-verify TTL decides what actually gets re-ground, which
        // is how evolving mods, new loader versions and newly-supported Minecraft releases get picked up.
        // Verdicts persist after every record, so a restart never redoes finished work.
        // Sources: Modrinth always (keyless); CurseForge only when its API key is set (mirrors clientside's
        // supportedPlatforms). GrindPool orders each batch round-robin across platforms, so neither starves.
        val batchSize = env("SPC_GRINDER_BATCH", "25").toInt()
        val curseForgeKey = System.getenv("CURSEFORGE_API_KEY")?.takeIf { it.isNotBlank() }
        val sources = buildList<CandidateSource> {
            add(ModrinthCandidateSource())
            if (curseForgeKey != null) {
                add(CurseForgeCandidateSource(curseForgeKey))
            }
        }
        val crawler = CatalogCrawler(sources, cursorStore, batchSize)
        val cacheRetention = Duration.ofDays(env("SPC_GRINDER_CACHE_TTL_DAYS", "7").toLong())
        val betweenSweeps = Duration.ofSeconds(env("SPC_GRINDER_INTERVAL", "21600").toLong())
        val whileCrawling = Duration.ofSeconds(env("SPC_GRINDER_SCAN_DELAY", "15").toLong())
        val sourceNames = if (curseForgeKey != null) "Modrinth + CurseForge" else "Modrinth (no CURSEFORGE_API_KEY)"
        log.info(
            "Continuous mode: sources=$sourceNames, batch $batchSize/pass, re-verify TTL ${reverifyTtl.toDays()}d, " +
                "${betweenSweeps.toSeconds()}s between completed sweeps, ${whileCrawling.toSeconds()}s while scanning ahead, " +
                "$workers worker(s), cache retention ${cacheRetention.toDays()}d, cursors=$cursorFile."
        )

        var pass = 0
        while (running.get()) {
            pass++
            val batch = crawler.nextBatch()
            log.info("Pass #$pass: grinding ${batch.candidates.size} candidate(s)...")
            status.beginPass(pass, batch.candidates.size)
            val pool = GrindPool(grinder, workers).also { activePool.set(it) }
            val pass = pool.grindAll(batch.candidates)
            val verified = pass.verified
            activePool.set(null)
            // Advance the crawl only past what was actually ground. An interrupted pass re-hands the rest next
            // time instead of skipping those projects until the next full sweep, weeks or months away.
            crawler.commit(batch, pass.reached)
            log.info("Pass #$pass complete: $verified verified, ${store.all().size} verdict(s) total.")
            // Bound the loader cache by time. Each tuple costs ~150 MB and loaders keep shipping builds, so an
            // unattended sweep would grow it without limit; a tuple still being booted is stamped as used on
            // every cache hit, so only genuinely idle ones go.
            val evicted = cache.evictUnusedSince(cacheRetention)
            if (evicted > 0) {
                log.info("Evicted $evicted loader install(s) unused for over ${cacheRetention.toDays()}d.")
            }
            if (!running.get()) {
                break
            }
            // Wait only when there is nothing to get on with — a fixed sleep per pass would cap how fast the
            // catalog can be swept, which is the difference between covering it in weeks and never.
            val pause = GrindPacing.pauseAfterPass(verified, batch.sweepCompleted, betweenSweeps, whileCrawling)
            if (pause.isZero) {
                continue
            }
            try {
                Thread.sleep(pause.toMillis())
            } catch (_: InterruptedException) {
                break // shutdown requested during the inter-pass wait
            }
        }
        log.info("Grinder stopped after $pass pass(es).")
    }

    /**
     * Where the daemon's SPC settings file lives: [explicitPath] when the operator named one, otherwise
     * `serverpackcreator.properties` inside the daemon's own [home]. Always absolute.
     *
     * Absoluteness is the point, not tidiness. `ApiProperties`' default is the *relative*
     * `File("serverpackcreator.properties")`, and `PropertyStore` keeps every file it loads in
     * `trackedPropertyFiles` and writes to all of them on every save — so that default made the daemon create and
     * rewrite a settings file in whatever directory it was launched from. Started from a checkout, it dropped one
     * into the repository root on every start. The daemon already derives `work`, `cache` and the verdict store
     * from its home; its settings belong there too, and then where it was started from stops mattering.
     */
    internal fun resolveSpcPropertiesFile(explicitPath: String?, home: File): File =
        explicitPath?.takeIf { it.isNotBlank() }?.let { File(it).absoluteFile }
            ?: File(home, "serverpackcreator.properties").absoluteFile

    /**
     * Claims the daemon its own SPC `Preferences` node, so the home directory it runs on cannot be moved by another
     * SPC process on this account (a test suite did exactly that mid-run: it relocated the home into its own scratch
     * directory, deleted it, and every boot then failed on a missing `server-icon.png` and was recorded as a
     * metadata-only verdict). Only claims one when the operator has not chosen a node themselves.
     */
    internal fun claimSpcPreferencesNode() {
        if (System.getProperty(ApiProperties.PREFERENCES_NODE_PROPERTY).isNullOrBlank() &&
            System.getenv(ApiProperties.PREFERENCES_NODE_ENV).isNullOrBlank()
        ) {
            System.setProperty(ApiProperties.PREFERENCES_NODE_PROPERTY, "${ApiProperties.DEFAULT_PREFERENCES_NODE}-grinder")
        }
    }

    /** Read [key] from the environment, falling back to [default] when unset or blank. */
    private fun env(key: String, default: String): String = System.getenv(key)?.takeIf { it.isNotBlank() } ?: default

    /** Best-effort project-slug from a URL (last path segment) — used only for the skip-already-done check. */
    private fun slugFromUrl(url: String): String = url.substringBefore('?').trimEnd('/').substringAfterLast('/').ifBlank { url }
}
