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
import de.griefed.serverpackcreator.api.settings.PathsConfig
import de.griefed.serverpackcreator.grinder.container.ContainerResources
import de.griefed.serverpackcreator.grinder.container.ContainerUser
import de.griefed.serverpackcreator.grinder.container.SHUTDOWN_GRACE
import de.griefed.serverpackcreator.grinder.container.DockerJavaContainerEngine
import de.griefed.serverpackcreator.grinder.loader.*
import de.griefed.serverpackcreator.grinder.report.CrashLogStore
import de.griefed.serverpackcreator.grinder.report.FallbackLists
import de.griefed.serverpackcreator.grinder.report.JsonVerdictStore
import de.griefed.serverpackcreator.grinder.report.VerdictStore
import de.griefed.serverpackcreator.grinder.report.ReportServer
import de.griefed.serverpackcreator.grinder.source.*
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import java.time.Duration
import java.time.Instant
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
     * Entry point. `--requeue <url>…` and `--requeue-before <instant>` add work to the immediate re-grind
     * queue and exit without grinding, so they can be run against a service that is already up. With project
     * URLs as [args] it grinds exactly those once and exits; with none it runs
     * continuously, taking the next catalogue slice each pass and persisting verdicts and crawl position after every
     * step so a restart resumes mid-catalogue. Everything else is read from the environment — see README §5.
     */
    @JvmStatic
    fun main(args: Array<String>) {
        // Created here rather than lazily: it is handed to SPC as its home directory below, and SPC's own
        // writability check runs before anything of ours would have created it.
        val base = File(env("SPC_GRINDER_HOME", File(System.getProperty("user.home"), ".spc-grinder").path))
            .absoluteFile.apply { mkdirs() }
        // Queue-and-exit, BEFORE the two claims below and before anything expensive is built. This process runs
        // *against a daemon that is already up*, so it must not claim the preferences node, re-pin SPC's home,
        // or touch Docker, the loader cache and the report port -- the service owns all of those, and the claims
        // are remembered for every later run. It writes to stdout rather than the log for the same reason: the
        // first `log` use in this process would construct the very ApiProperties the claims exist to control.
        // Answered here rather than over HTTP because the report server is unauthenticated by design, and a
        // write endpoint on it would let anyone who can reach the page schedule unbounded container work.
        if (args.isNotEmpty() && args.first().startsWith("--requeue")) {
            val storePath = File(env("SPC_GRINDER_STORE", File(base, "verdicts.json").path))
            val queuePath = File(env("SPC_GRINDER_REQUEUE", File(base, "requeue.json").path))
                .apply { parentFile?.mkdirs() }
            enqueueAndExit(args, JsonRequeueStore(queuePath), JsonVerdictStore(storePath))
            return
        }
        // Both claims BEFORE anything touches `log`: ApiProperties is registered as log4j's ConfigurationFactory,
        // so the first log statement in this process constructs one, and whatever that one resolves is what the
        // daemon runs on -- and gets remembered in the Preferences node for every run after it.
        claimSpcPreferencesNode()
        pinSpcHomeDirectory(base)

        val image = env("SPC_GRINDER_IMAGE", "spc-grinder-runtime:latest")
        val workDir = File(env("SPC_GRINDER_WORK", File(base, "work").path)).apply { mkdirs() }
        val cacheRoot = File(env("SPC_GRINDER_CACHE", File(base, "cache").path)).apply { mkdirs() }
        val storeFile = File(env("SPC_GRINDER_STORE", File(base, "verdicts.json").path)).apply { parentFile?.mkdirs() }
        // The immediate re-grind lane. Lives beside the verdict store rather than under `work/`: it is state an
        // operator authored, not scratch the reaper may reclaim.
        val requeueFile = File(env("SPC_GRINDER_REQUEUE", File(base, "requeue.json").path)).apply { parentFile?.mkdirs() }
        val requeue = JsonRequeueStore(requeueFile)
        val port = env("SPC_GRINDER_PORT", "8757").toInt()
        // Loopback by default: the report is unauthenticated, so reaching it from anywhere else -- a reverse
        // proxy in a container dials the host over the bridge gateway, never 127.0.0.1 -- is a deliberate act.
        val bindHost = env("SPC_GRINDER_HOST", "127.0.0.1")
        val workers = env("SPC_GRINDER_WORKERS", "2").toInt()
        // Cores per container, not per host: `workers * cpus` is what the grinder can actually occupy, and the
        // default pair (2 workers x 2 cores) is what every install has been running on. Worth a knob because a
        // grinder normally shares its box -- and because the cap the code shipped with was unreachable from the
        // outside. Do not tune it below ~1 core: world generation is single-thread-bound, and a boot throttled
        // past its 15-minute budget is scored INCONCLUSIVE, which reads as a hanging mod rather than as a
        // starved host. 0 disables the cap.
        val containerCpus = env("SPC_GRINDER_CPUS", "2").toDouble()
        // Memory per container, and the one knob here that is genuinely dangerous to touch: the packs the
        // grinder builds leave `javaArgs` empty, so the JVM sizes the server's heap from this cgroup limit
        // (measured at 25%, so 3 GiB gives a 768 MiB heap), and it is also what the README's worker-sizing
        // advice divides by. Lower it and boots die of a heap too small for a modded server; raise it without
        // dropping workers and the host over-subscribes and OOM-kills them. Both are scored INCONCLUSIVE,
        // i.e. they look like mods that hang. Default 3 GiB -- see README §5's warning.
        val containerMemoryGiB = env("SPC_GRINDER_MEMORY_GIB", "3").toDouble()
        val containerResources = ContainerResources.forLimits(containerCpus, containerMemoryGiB)
        // The image declares USER 1000:1000, which is only right while the daemon itself is uid 1000. Every
        // container bind-mounts a directory this process created, so it has to run as that directory's owner --
        // otherwise every write inside the pack is refused, and the boot dies on a missing @argfile far from
        // the actual cause. Logged below so the identity is visible without reproducing the failure.
        // Read here rather than inside ContainerUser so the entry point stays the one place environment is
        // consulted -- which is also what keeps the README table and the systemd unit honest, since both guards
        // scan this file for the names it reads.
        val containerUser = ContainerUser.forDirectory(workDir, System.getenv("SPC_GRINDER_CONTAINER_USER"))

        log.info(
            "Grinder starting — home=$base image=$image work=$workDir cache=$cacheRoot store=$storeFile " +
                "bind=$bindHost port=$port workers=$workers containerUser=$containerUser " +
                "cpus=${containerResources.cpuCapDescription()} memory=${containerResources.memoryCapDescription()}"
        )

        log.info("Using Preferences node '${ApiProperties.resolvePreferencesNode()}' for SPC settings.")

        // Point SPC at a specific config when given (reproducible runs), else one inside our own home -- never
        // ApiWrapper.api()'s relative default, see resolveSpcPropertiesFile.
        val apiWrapper = ApiWrapper.api(resolveSpcPropertiesFile(System.getenv("SPC_GRINDER_SPC_PROPERTIES"), base))
        val engine = DockerJavaContainerEngine()
        // Authoritative Minecraft -> required-Java from SPC's own metadata; gates selection to the image's JDKs.
        val imageJava = ImageJavaRuntimes.from(apiWrapper.versionMeta.minecraft)
        val installer = DockerLoaderInstaller(
            engine, image, ApiVanillaPackGenerator(apiWrapper, File(workDir, "install")), imageJava,
            resources = containerResources, containerUser = containerUser
        )
        // A cached install is a product of the start-script templates that built it, so record which ones those
        // were. Read per call rather than once: SPC resolves its templates from the then-current home, and the
        // daemon's home can be re-resolved while it runs.
        val cache = LoaderCache(cacheRoot, installer, templateProvenance = {
            TemplateProvenance.digestOf(
                apiWrapper.apiProperties.defaultStartScriptTemplates().values.map { File(it) }
            )
        })
        // Lives under the daemon's home rather than under `work/`, deliberately: everything below `work/` is
        // scratch the reaper is entitled to reclaim, and the console of a crashed boot is the one artefact a
        // HIGH verdict cannot be re-derived without. Bounded by the number of distinct crashing tuples, since
        // a re-grind replaces a project's log rather than adding one.
        val crashLogs = CrashLogStore(File(base, "crash-logs"))
        val verifier = ContainerCandidateVerifier(
            apiWrapper, cache, engine, image, imageJava, File(workDir, "verify"),
            resources = containerResources, containerUser = containerUser, crashLogs = crashLogs
        )
        // Containers first: a JVM that was SIGKILLed (systemd's TimeoutStopSec expiring mid-cleanup) leaves them
        // running, parented by the docker daemon rather than this unit's control group, so nothing else on the
        // host will ever collect them. Safe here and only here, for the same reason as the staging sweep below:
        // nothing of ours is in flight yet, so anything wearing our label is by definition inherited.
        engine.reapOrphans().let { reaped ->
            if (reaped > 0) {
                log.info("Reaped $reaped container(s) left running by a previous, killed run.")
            }
        }
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
            log.info("Shutdown requested — ${SHUTDOWN_GRACE.seconds}s for containers and workers to quit, then killed.")
            // ONE window for the whole shutdown, not one per half: the containers and the workers are the same
            // stop as far as an operator and systemd are concerned, and spending a full window on each would
            // double the worst case past what the unit's TimeoutStopSec is sized for.
            val deadline = System.currentTimeMillis() + SHUTDOWN_GRACE.toMillis()
            running.set(false)
            // Read once: `main` replaces this per pass, and reading it twice could signal one pool and wait on
            // another.
            val pool = activePool.get()
            // No new candidates, before anything else: cheap, and it means a worker finishing right now does not
            // start another one while the rest of this runs.
            pool?.requestStop()
            // Containers first, and this is the ordering that matters. `close()` marks the engine closed before
            // it sweeps, so a worker cannot create a container behind it; it then asks each container to exit
            // (SIGTERM, killed after the window) which is also what unblocks the workers waiting on them.
            runCatching { engine.close() }
                .onFailure { log.warn("Could not clean up in-flight containers: ${it.message}") }
            // Whatever is left of the window goes to the workers -- which is usually most of it, since stopping
            // containers is what frees them. A worker that does not come back is abandoned (the JVM exits either
            // way), but say so: it means work was still running at exit.
            // Floored, not clamped to zero: a container that ignores SIGTERM can eat the whole window, and
            // handing the workers 0ms means the interrupt they were just sent cannot possibly be observed --
            // the "did not stop" warning would then be guaranteed rather than informative.
            val remaining = maxOf(WORKER_STOP_FLOOR, Duration.ofMillis(deadline - System.currentTimeMillis()))
            if (pool?.awaitStop(remaining) == false) {
                log.warn("A worker did not stop within ${SHUTDOWN_GRACE.seconds}s; exiting anyway.")
            }
            mainThread.interrupt()
        })

        val cursorFile = File(env("SPC_GRINDER_CURSORS", File(base, "cursors.json").path))
            .apply { parentFile?.mkdirs() }
        val cursorStore = JsonCursorStore(cursorFile)
        val server = ReportServer(
            store, port, host = bindHost, status = status, cursors = cursorStore, cacheRoot = cacheRoot,
            // Read per request, not captured once: SPC refreshes these from its own update-URL while the
            // daemon runs, and /as-properties must publish what this instance holds now.
            fallbackLists = {
                FallbackLists(
                    clientsideMods = apiWrapper.apiProperties.clientsideMods.toList(),
                    whitelist = apiWrapper.apiProperties.modsWhitelist.toList()
                )
            },
            crashLogs = crashLogs,
            requeue = requeue
        ).start()
        val reportUrl = reportUrl(bindHost, server.port)
        log.info("Report:  $reportUrl/    CSV: $reportUrl/export.csv    live status: $reportUrl/status")
        log.info("Crash consoles of boots that died: $reportUrl/crash-logs (also linked per row in the report)")
        log.info("Fallback list for SPC instances (set as their fallback.updateurl): $reportUrl/as-properties")

        if (args.isNotEmpty()) {
            // One-shot: grind a fixed set of project URLs (handy for an end-to-end verification), then
            // hold the report open. The re-verify TTL still applies, so re-running skips fresh verdicts.
            val candidates = args.map { GrindCandidate(it, slugFromUrl(it), 0, ModPlatforms.ofUrl(it)) }
            log.info("One-shot run: grinding ${candidates.size} candidate(s) with $workers worker(s)...")
            // Registered like the continuous path's pool: activePool is the only handle the shutdown hook has,
            // and without it Ctrl-C here signalled and awaited nothing -- both calls no-opping through a null.
            GrindPool(grinder, workers).also { activePool.set(it) }
                .grindAll(candidates) // one-shot: no crawl cursor to advance
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
            // The immediate lane first, and forced: these are projects somebody decided are wrong, and a
            // wrong verdict is usually a recent one, so an unforced drain would skip every one as fresh.
            // Ground before the catalog slice so a re-grind lands in minutes rather than at the next TTL.
            val requeued = requeue.drain()
            if (requeued.isNotEmpty()) {
                log.info("Pass #$pass: re-grinding ${requeued.size} requested candidate(s) ahead of the crawl...")
                GrindPool(grinder, workers).also { activePool.set(it) }.grindAll(requeued, force = true)
            }
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
     * The base URL to print for the report, given the address it was bound to. The bound host rather than a
     * hardcoded "localhost": under a non-default bind that URL is one the operator cannot reach, and the
     * journal is where they go looking for it.
     *
     * Two of the shapes a bind address can take do not survive plain interpolation. A wildcard means "every
     * interface" and is not a destination a browser can resolve, so it is reported as the loopback the report
     * is certainly answering on; and an IPv6 literal needs the brackets of RFC 3986, without which the port is
     * silently parsed as -1 rather than rejected.
     */
    internal fun reportUrl(bindHost: String, port: Int): String {
        val reachable = when (bindHost) {
            "0.0.0.0" -> "127.0.0.1"
            "::", "0:0:0:0:0:0:0:0" -> "::1"
            else -> bindHost
        }
        // startsWith("["): the JDK binds a bracketed literal quite happily, so an operator may have written
        // one, and bracketing it again yields http://[[::1]]:8757.
        val literal = if (reachable.contains(':') && !reachable.startsWith("[")) "[$reachable]" else reachable
        return "http://$literal:$port"
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

    /**
     * Names SPC's home directory for it, so it is the daemon's own base rather than something SPC resolves on its
     * own. Left to resolve one, a source build — which every locally built artifact is — falls back to the process
     * working directory, and a service manager starts a unit in `/`: every write SPC performs then fails, starting
     * with `log4j2.xml`, and the daemon dies before reaching Docker. Set as a system property, which outranks the
     * stored preference without replacing it, so an operator's own `-D` is left alone and a bad value remembered
     * from an earlier run is overridden rather than inherited.
     */
    internal fun pinSpcHomeDirectory(home: File) {
        if (System.getProperty(PathsConfig.HOME_DIRECTORY_KEY).isNullOrBlank()) {
            System.setProperty(PathsConfig.HOME_DIRECTORY_KEY, home.absolutePath)
        }
    }

    /** Read [key] from the environment, falling back to [default] when unset or blank. */
    /**
     * Least time the workers get to notice their interrupt, however long the containers took. Small enough that
     * the worst case (grace + this) stays far inside the unit's stop timeout.
     */
    private val WORKER_STOP_FLOOR: Duration = Duration.ofSeconds(1)

    private fun env(key: String, default: String): String = System.getenv(key)?.takeIf { it.isNotBlank() } ?: default

    /**
     * Handle `--requeue …` and `--requeue-before …`, print what was queued, and return without grinding.
     *
     * Two selectors, because two things actually happen. `--requeue <url>…` is a named handful — a report a
     * user disputed, a project whose verdict looks wrong. `--requeue-before <instant>` is the recurring one:
     * a defect is found in the engine and *everything verified before the fix* is suspect, which is a
     * population nobody should have to list by hand. Both are additive and idempotent — queueing an entry
     * that is already waiting changes nothing.
     *
     * The running daemon picks the queue up at the start of its next pass. This process deliberately builds
     * nothing else: no Docker, no loader cache, no report port, because a service is already holding those.
     *
     * **It writes to stdout, never to `log`, and that is load-bearing rather than stylistic.** `ApiProperties`
     * is registered as log4j's `ConfigurationFactory`, so the first log statement in a process constructs one
     * and whatever it resolves is remembered in SPC's Preferences node for every later run — which is exactly
     * what [claimSpcPreferencesNode] and [pinSpcHomeDirectory] exist to control, and which this path runs
     * *before*. An operator running `--requeue` would otherwise re-pin the home of the service it is queueing
     * work for. Stdout is also simply what a queue-and-exit command should produce.
     */
    private fun enqueueAndExit(args: Array<String>, requeue: RequeueStore, store: VerdictStore) {
        val verb = args.first()
        val rest = args.drop(1)
        val candidates = when (verb) {
            "--requeue" -> rest.map { GrindCandidate(it, slugFromUrl(it), 0, ModPlatforms.ofUrl(it)) }

            "--requeue-before" -> {
                val instant = rest.firstOrNull()?.let { runCatching { Instant.parse(it) }.getOrNull() }
                if (instant == null) {
                    println("--requeue-before needs an ISO-8601 instant, e.g. --requeue-before 2026-08-23T18:00:00Z")
                    return
                }
                RequeueSelection.verifiedBefore(store.all(), instant)
            }

            else -> {
                println("Unknown verb '$verb'. Use --requeue <url>… or --requeue-before <ISO-8601 instant>.")
                return
            }
        }
        if (candidates.isEmpty()) {
            println("Nothing to re-grind: the selection matched no project.")
            return
        }
        val added = requeue.add(candidates)
        println(
            "Queued $added of ${candidates.size} project(s) for immediate re-grinding " +
                "(${candidates.size - added} already waiting); ${requeue.pending()} now pending.\n" +
                "A running daemon takes them at the start of its next pass; a stopped one on its next start."
        )
    }

    /** Best-effort project-slug from a URL (last path segment) — used only for the skip-already-done check. */
    private fun slugFromUrl(url: String): String = url.substringBefore('?').trimEnd('/').substringAfterLast('/').ifBlank { url }
}
