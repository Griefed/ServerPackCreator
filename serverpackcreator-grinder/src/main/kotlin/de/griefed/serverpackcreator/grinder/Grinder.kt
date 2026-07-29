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
import java.util.concurrent.ConcurrentLinkedQueue

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
 * @author Griefed
 */
class Grinder(
    private val verifier: CandidateVerifier,
    private val store: VerdictStore,
    private val reverifyTtl: Duration = Duration.ofDays(30),
    private val clock: () -> Instant = Instant::now
) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

    /** Verify [candidate] (unless a fresh verdict exists) and record its per-loader verdicts. */
    fun grind(candidate: GrindCandidate) {
        // Freshness is per (platform, slug): the same slug on Modrinth and CurseForge is two projects.
        val lastVerified = store.newestVerification(candidate.platform, candidate.slug)
        if (lastVerified != null && Duration.between(lastVerified, clock()) < reverifyTtl) {
            return
        }
        val report = runCatching { verifier.verify(candidate) }
            .onFailure { log.warn("Verification failed for ${candidate.projectUrl}: ${it.message}") }
            .getOrNull() ?: return
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
                    verifiedAt = now
                )
            )
        }
    }
}

/**
 * Drains a batch of candidates across a fixed pool of worker threads, so multiple servers boot in
 * parallel (the throughput lever — sequential grinding would never finish a catalog). Candidates are
 * processed most-popular-first; each worker pulls the next from a shared queue until it drains.
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
    init {
        require(workerCount >= 1) { "workerCount must be at least 1, was $workerCount" }
    }

    /** Process every candidate in [candidates] (popularity-first), returning once all are done. */
    fun grindAll(candidates: Collection<GrindCandidate>) {
        val queue = ConcurrentLinkedQueue(candidates.sortedByDescending { it.popularity })
        val workers = (1..workerCount).map {
            Thread {
                while (true) {
                    val candidate = queue.poll() ?: break
                    grinder.grind(candidate)
                }
            }.apply { name = "grind-worker-$it"; start() }
        }
        workers.forEach { it.join() }
    }
}
