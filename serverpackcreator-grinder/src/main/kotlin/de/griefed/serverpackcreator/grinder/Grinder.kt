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

import de.griefed.serverpackcreator.grinder.report.VerdictStore
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.time.Instant
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Grinds one candidate: skip it while its verdict is still *fresh*, otherwise run the
 * [CandidateVerifier] and record one [GrindVerdict] per loader in the [store]. A thrown verification is
 * logged and dropped (the candidate stays un-verified, to be retried on a later pass) rather than
 * sinking the worker. "Fresh" = a verdict younger than [reverifyTtl]; anything unseen or older is
 * (re-)ground, so a continuous grind re-checks projects as mods, loader versions and Minecraft support
 * evolve, without redoing fresh work every pass.
 *
 * @param verifier    The boot pipeline (faked in tests; container-backed in production).
 * @param store       Where verdicts accumulate.
 * @param reverifyTtl How long a verdict stays fresh before the project is re-ground.
 * @param clock       Supplies the verdict timestamp and the freshness "now" (injectable for tests).
 * @param status      Live activity record for the report server's `/status`, updated around each candidate.
 *                    Optional so the orchestration stays testable without it.
 * @author Griefed
 */
class Grinder(
    private val verifier: CandidateVerifier,
    private val store: VerdictStore,
    private val reverifyTtl: Duration = Duration.ofDays(30),
    private val clock: () -> Instant = Instant::now,
    private val status: GrinderStatus? = null
) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

    /**
     * Verify [candidate] (unless a fresh verdict exists), record its per-loader verdicts and report what
     * happened — the daemon paces itself on how much real work a pass did (see [GrindPacing]).
     */
    fun grind(candidate: GrindCandidate): GrindOutcome {
        // Freshness is per (platform, slug): the same slug on Modrinth and CurseForge is two projects.
        val lastVerified = store.newestVerification(candidate.platform, candidate.slug, candidate.projectId)
        if (lastVerified != null && Duration.between(lastVerified, clock()) < reverifyTtl) {
            // Deliberately not INFO: a pass can skip dozens of fresh projects in microseconds, and logging each
            // would bury the one line that matters — the candidate actually being worked on.
            log.debug("Skipping ${candidate.platform}/${candidate.slug}: verdict still fresh.")
            return GrindOutcome.SKIPPED_FRESH
        }
        // One readable line per candidate actually being ground, so `tail -f` answers "what is it doing?"
        // without decoding pack paths. The thread name in the log pattern says which worker.
        log.info("Grinding ${candidate.platform}/${candidate.slug} — ${candidate.projectUrl}")
        status?.beginCandidate(candidate)
        val startedAt = clock()
        val report = runCatching { verifier.verify(candidate) }
            .onFailure { log.warn("Verification failed for ${candidate.projectUrl}: ${it.message}") }
            .getOrNull()
        if (report == null) {
            log.warn("Done ${candidate.platform}/${candidate.slug} → FAILED after ${Duration.between(startedAt, clock()).seconds}s")
            status?.endCandidate(GrindOutcome.FAILED)
            return GrindOutcome.FAILED
        }
        if (report.platform != candidate.platform) {
            // Recording uses the resolved report's platform, while the skip-check above uses the
            // candidate's. If a source ever labels a project differently from the platform that resolves
            // it, the two keys never meet and the project is re-ground every pass — so make it loud.
            log.warn(
                "Platform mismatch for ${candidate.projectUrl}: candidate says '${candidate.platform}', " +
                    "resolved report says '${report.platform}'. It will be re-verified every pass until they agree."
            )
        }
        val now = clock()
        for (verdict in report.perLoader) {
            store.record(
                GrindVerdict(
                    platform = report.platform,
                    slug = report.slug,
                    projectUrl = report.projectUrl,
                    loader = verdict.loader,
                    suggestedEntry = verdict.suggestedEntry,
                    confidence = verdict.confidence,
                    detail = verdict.note ?: "",
                    verifiedAt = now,
                    // Identity comes from the candidate, not the report: the report echoes the slug, which is the
                    // mutable name this exists to stop depending on.
                    projectId = candidate.projectId
                )
            )
        }
        // Report the boot result alongside the confidence: a verdict reached *without* a boot is a much weaker
        // claim than one that booted, and only the log can tell them apart afterwards.
        log.info(
            "Done ${candidate.platform}/${candidate.slug} → " +
                report.perLoader
                    .joinToString(", ") { "${it.loader}=${it.confidence}(boot:${it.bootResult ?: "none"})" }
                    .ifEmpty { "no loader verdicts" } +
                " after ${Duration.between(startedAt, clock()).seconds}s"
        )
        status?.endCandidate(GrindOutcome.VERIFIED)
        return GrindOutcome.VERIFIED
    }
}

/**
 * What one [Grinder.grind] call did. Distinguishing *skipped because fresh* from *attempted and failed* is
 * what lets the daemon pace itself: only [VERIFIED] counts as progress, so a pass that found nothing due —
 * or one where everything failed — waits instead of racing the crawl position onward.
 *
 * @author Griefed
 */
enum class GrindOutcome {
    /** The candidate was verified and its per-loader verdicts recorded. */
    VERIFIED,

    /** Verification was attempted but threw; nothing was recorded and the project stays due. */
    FAILED,

    /** The project's verdict is still younger than the re-verify TTL, so nothing was done. */
    SKIPPED_FRESH
}

/**
 * Drains a batch of candidates across a fixed pool of worker threads, so multiple servers boot in
 * parallel (the throughput lever — sequential grinding would never finish a catalog). Candidates are
 * ordered round-robin across platforms, each platform most-downloaded first (see [interleaveByPlatform]);
 * each worker pulls the next from a shared queue until it drains.
 * Parallelism should be sized to the host (≈ RAM / per-boot-memory), since each in-flight grind holds
 * a booting container.
 *
 * @param grinder     Grinds a single candidate.
 * @param workerCount Number of parallel workers (and therefore concurrent container boots).
 * @author Griefed
 */
class GrindPool(
    private val grinder: Grinder,
    private val workerCount: Int
) {
    /** Set by [requestStop]; workers finish their current candidate and then stop taking new ones. */
    private val stopRequested = AtomicBoolean(false)

    init {
        require(workerCount >= 1) { "workerCount must be at least 1, was $workerCount" }
    }

    /**
     * Ask the workers to stop after their current candidate — the queue is abandoned, nothing is
     * cancelled mid-grind. Used by the daemon's shutdown hook so a pass ends promptly instead of draining
     * a whole popularity-ranked batch; the in-flight boot is torn down separately by closing the
     * container engine.
     */
    fun requestStop() {
        stopRequested.set(true)
    }

    /**
     * Process every candidate in [candidates] — round-robin across platforms, each platform most-downloaded
     * first ([interleaveByPlatform]) — returning once all are done, or early if [requestStop] is called or the
     * calling thread is interrupted (the daemon's shutdown path).
     *
     * The returned [GrindPass] reports **which candidates were reached** as well as how many were verified.
     * Reached matters as much as verified: the crawl cursor may only advance past candidates something actually
     * got to, so an abandoned pass has to be able to say what it never touched (see `CatalogCrawler.commit`).
     * A candidate counts as reached only once [Grinder.grind] has *returned* for it, so one still being ground
     * while the JVM tears down is deliberately not reported — it gets handed out again next time.
     * `verified` stays the pacing measure (see [GrindPacing]); skipped-as-fresh and failed do not count there.
     */
    fun grindAll(candidates: Collection<GrindCandidate>): GrindPass {
        val queue = ConcurrentLinkedQueue(interleaveByPlatform(candidates))
        val verified = AtomicInteger(0)
        val reached = ConcurrentHashMap.newKeySet<GrindCandidate>()
        val workers = (1..workerCount).map {
            Thread {
                while (!stopRequested.get()) {
                    val candidate = queue.poll() ?: break
                    val outcome = grinder.grind(candidate)
                    reached.add(candidate)
                    if (outcome == GrindOutcome.VERIFIED) {
                        verified.incrementAndGet()
                    }
                }
            }.apply { name = "grind-worker-$it"; start() }
        }
        try {
            workers.forEach { it.join() }
        } catch (_: InterruptedException) {
            // The daemon's shutdown hook interrupts the thread that is parked here. Abandon the rest of the
            // batch instead of letting the interrupt escape as an uncaught exception (which killed the
            // process outright on SIGTERM mid-pass), and hand the flag back so the caller sees the shutdown.
            // Workers still finish their current candidate; their in-flight containers are torn down
            // separately by closing the container engine.
            requestStop()
            Thread.currentThread().interrupt()
        }
        return GrindPass(reached, verified.get())
    }

    /**
     * Order a batch **round-robin across platforms**, each platform most-downloaded first — one CurseForge, one
     * Modrinth, one CurseForge, and so on, with a platform that runs out simply dropping out of the rotation.
     *
     * Sorting the whole batch by `popularity` instead starves a platform. Measured live on 2026-07-30:
     * CurseForge's counts run several times Modrinth's for equivalent mods (`jei` 602 M vs `fabric-api` 218 M),
     * so every CurseForge candidate outranked every Modrinth one and a two-hour pass produced 108 CurseForge
     * verdicts and **zero** Modrinth ones — indefinitely, for any interruption shorter than a full pass.
     *
     * The counts are not comparable in the first place: CurseForge counts file downloads across every version,
     * Modrinth counts differently, so ranking them against each other was never meaningful — it just silently
     * promoted one platform. This keeps the comparison that *is* meaningful (within a platform) and drops the one
     * that is not. Platform order in the rotation is alphabetical, purely so a pass is reproducible.
     */
    private fun interleaveByPlatform(candidates: Collection<GrindCandidate>): List<GrindCandidate> {
        val perPlatform = candidates
            .groupBy { it.platform }
            .toSortedMap()
            .map { (_, ofPlatform) -> ArrayDeque(ofPlatform.sortedByDescending { it.popularity }) }
        val ordered = ArrayList<GrindCandidate>(candidates.size)
        while (ordered.size < candidates.size) {
            for (platformQueue in perPlatform) {
                platformQueue.removeFirstOrNull()?.let { ordered.add(it) }
            }
        }
        return ordered
    }
}

/**
 * What one pass of [GrindPool.grindAll] achieved: the candidates it **reached** (ground to any outcome — verified,
 * skipped as fresh, or attempted and failed) and how many of those were verified. Two different questions, which
 * is why both are reported: the daemon paces itself on [verified], while the crawl cursor may only advance past
 * [reached].
 *
 * @author Griefed
 */
data class GrindPass(
    /** Every candidate this pass actually reached, used to commit each source's cursor no further than the work done. */
    val reached: Set<GrindCandidate>,
    /** How many reached a verdict. Failures are excluded deliberately — pacing on failures races the cursor. */
    val verified: Int
)
