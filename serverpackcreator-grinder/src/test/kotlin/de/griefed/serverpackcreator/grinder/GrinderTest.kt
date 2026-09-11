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

import de.griefed.serverpackcreator.clientside.DeclaredSupport
import de.griefed.serverpackcreator.clientside.JarScan
import de.griefed.serverpackcreator.clientside.Verdict
import de.griefed.serverpackcreator.grinder.report.VerdictField
import de.griefed.serverpackcreator.grinder.report.InMemoryVerdictStore
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

/**
 * Pins the grind orchestration with a fake [CandidateVerifier] (no containers): one verdict recorded
 * per loader, projects with a *fresh* verdict skipped while *stale* ones are re-verified, a thrown
 * verification swallowed (not propagated), and the pool draining every candidate across workers
 * in the pool's order (round-robin by platform — pinned in `GrindPoolOrderTest`). Each grind also reports its
 * [GrindOutcome], and the pool counts the verified ones —
 * the daemon paces itself on that count, so it is part of the contract, not a convenience.
 */
internal class GrinderTest {

    private fun candidate(slug: String, popularity: Long = 1) =
        GrindCandidate("https://modrinth.com/mod/$slug", slug, popularity, ModPlatforms.MODRINTH)

    /**
     * One row per Minecraft version-line, each naming the loader that line was ground under. The report's
     * rows are lines, so this counts them rather than counting loaders — which is how the same project came
     * to be reported three times about one era.
     */
    @Test
    fun recordsOneVerdictPerMinecraftLineFromTheReport() {
        val store = InMemoryVerdictStore()
        val verifier = CandidateVerifier { c ->
            clientsideReport(
                c.slug,
                listOf(
                    loaderVerdict(
                        "NeoForge", "${c.slug}-", Verdict.CONFIRMED,
                        minecraftLine = "1.21", minecraftVersion = "1.21.1"
                    ),
                    loaderVerdict(
                        "Forge", "${c.slug}-", Verdict.INCONCLUSIVE,
                        minecraftLine = "1.12", minecraftVersion = "1.12.2"
                    )
                )
            )
        }
        val outcome = Grinder(verifier, store).grind(candidate("jei"))

        Assertions.assertEquals(GrindOutcome.VERIFIED, outcome)
        Assertions.assertEquals(
            setOf(
                Triple("1.21", "NeoForge", Verdict.CONFIRMED),
                Triple("1.12", "Forge", Verdict.INCONCLUSIVE)
            ),
            store.all().map { Triple(it.minecraftLine, it.loader, it.verdict) }.toSet()
        )
        Assertions.assertEquals("jei-", store.all().first { it.minecraftLine == "1.21" }.suggestedEntry)
    }

    /**
     * The clientside engine already decides both sidenesses and the jar scan, and the CLI report prints
     * all three — but they stopped at [de.griefed.serverpackcreator.clientside.LoaderVerdict] and never
     * reached the store, so the grinder's own report could not show what the platform declared versus what
     * the jar declared. Those two facts are what a human needs to judge a verdict.
     */
    @Test
    fun recordsTheDeclaredSidenessAndTheJarScanBehindTheVerdict() {
        val store = InMemoryVerdictStore()
        val verifier = CandidateVerifier { c ->
            clientsideReport(
                c.slug,
                listOf(
                    loaderVerdict(
                        "Fabric", "${c.slug}-", Verdict.CONFIRMED,
                        declaredClientSide = DeclaredSupport.REQUIRED,
                        declaredServerSide = DeclaredSupport.UNSUPPORTED,
                        jarScan = JarScan.CLIENT,
                        bootedLoader = "Quilt",
                        sampleFile = "jei-1.21.1-fabric-19.21.0.247.jar"
                    )
                )
            )
        }

        Grinder(verifier, store).grind(candidate("jei"))

        val recorded = store.all().single()
        // Asked through the column rather than the field, so the assertion needs no name for it: what
        // matters is that the report's `Filename` cell ends up carrying the artifact that was sampled.
        // `LoaderVerdict.sampleFile` holds it and `Grinder.grind`'s hand-written mapping dropped it, which
        // is the same 18-field copy `claude-docs/ANALYSIS-AUDIT.md` flagged as asserted five fields deep.
        Assertions.assertEquals(
            "jei-1.21.1-fabric-19.21.0.247.jar", VerdictField.FILENAME.text(recorded),
            "the Filename column has to name the artifact a maintainer would download"
        )
        Assertions.assertEquals(DeclaredSupport.REQUIRED, recorded.declaredClientSide)
        Assertions.assertEquals(DeclaredSupport.UNSUPPORTED, recorded.declaredServerSide)
        Assertions.assertEquals(JarScan.CLIENT, recorded.jarScan)
        Assertions.assertEquals(
            "Quilt", recorded.bootedLoader,
            "the loader that actually booted may differ from the loader the verdict is about"
        )
    }

    /**
     * A verdict recorded before those columns existed has no answer for them, which is a *different*
     * statement from CurseForge publishing no sideness at all — and CurseForge publishes none for every
     * one of its rows. Collapsing the two onto `UNKNOWN` would make the ~870 legacy rows indistinguishable
     * from every CurseForge row, which is exactly the confusion the column exists to dispel.
     */
    @Test
    fun aVerdictWithoutRecordedSidenessIsNullRatherThanUnknown() {
        Assertions.assertNull(grindVerdict("jei", "Forge").declaredClientSide)
        Assertions.assertNull(grindVerdict("jei", "Forge").jarScan)
    }

    /**
     * **A dependency blamed for a candidate's crash is queued for its own verification.**
     *
     * This is the mechanism that makes attribution safe. The blame itself is a string match over a console
     * and deliberately never moves a verdict, so the suspicion has to be settled some other way — by
     * grinding the dependency alone and seeing whether it crashes by itself. Without the queueing the
     * annotation would be a note nobody ever acts on.
     */
    @Test
    fun aDependencyBlamedForACrashIsQueuedForItsOwnVerification() {
        val store = InMemoryVerdictStore()
        val queued = mutableListOf<GrindCandidate>()
        val requeue = object : de.griefed.serverpackcreator.grinder.source.RequeueStore {
            override fun add(candidates: Collection<GrindCandidate>): Int {
                queued.addAll(candidates); return candidates.size
            }

            override fun drain(): List<GrindCandidate> = emptyList()
            override fun pending(): Int = queued.size
        }
        val verifier = CandidateVerifier { c ->
            clientsideReport(
                c.slug,
                listOf(
                    loaderVerdict("Forge", "${c.slug}-", Verdict.CONFIRMED)
                        .copy(blamedDependencyUrl = "https://modrinth.com/mod/benbenlaw-core")
                )
            )
        }

        Grinder(verifier, store, requeue = requeue).grind(candidate("strawberrymod"))

        Assertions.assertEquals(
            listOf("https://modrinth.com/mod/benbenlaw-core"), queued.map { it.projectUrl },
            "the blamed dependency must be queued so the question is answered by grinding it"
        )
        Assertions.assertEquals(ModPlatforms.MODRINTH, queued.single().platform, "resolved from its own URL")
    }

    /** Nothing blamed means nothing queued — the common case must not put work on the lane. */
    @Test
    fun aCandidateWithNoBlamedDependencyQueuesNothing() {
        val queued = mutableListOf<GrindCandidate>()
        val requeue = object : de.griefed.serverpackcreator.grinder.source.RequeueStore {
            override fun add(candidates: Collection<GrindCandidate>): Int {
                queued.addAll(candidates); return candidates.size
            }

            override fun drain(): List<GrindCandidate> = emptyList()
            override fun pending(): Int = 0
        }
        val verifier = CandidateVerifier { c ->
            clientsideReport(c.slug, listOf(loaderVerdict("Forge", "${c.slug}-", Verdict.ERROR)))
        }

        Grinder(verifier, InMemoryVerdictStore(), requeue = requeue).grind(candidate("jei"))

        Assertions.assertTrue(queued.isEmpty())
    }

    @Test
    fun skipsProjectsWithAFreshVerdict() {
        val now = Instant.parse("2026-06-01T00:00:00Z")
        val store = InMemoryVerdictStore()
        store.record(grindVerdict("jei", "Forge", verifiedAt = now.minus(Duration.ofDays(5))))
        val calls = AtomicInteger(0)
        val verifier = CandidateVerifier { c -> calls.incrementAndGet(); clientsideReport(c.slug, listOf(loaderVerdict("Forge", "jei-", Verdict.CONFIRMED))) }

        val outcome = Grinder(verifier, store, reverifyTtl = Duration.ofDays(30), clock = { now }).grind(candidate("jei"))

        Assertions.assertEquals(GrindOutcome.SKIPPED_FRESH, outcome)
        Assertions.assertEquals(0, calls.get(), "a verdict younger than the TTL must not be re-verified")
        Assertions.assertEquals(1, store.all().size)
    }

    /**
     * **A forced grind ignores freshness.** That is the whole point of the immediate re-grind queue: a
     * verdict is queued precisely *because* it is wrong, and it is almost always recent — the defects that
     * invalidate verdicts are found by reading verdicts that were just produced. Without this the queue
     * would drain into `SKIPPED_FRESH` and do nothing at all.
     */
    @Test
    fun aForcedGrindReVerifiesEvenAFreshVerdict() {
        val now = Instant.parse("2026-06-01T00:00:00Z")
        val store = InMemoryVerdictStore()
        store.record(grindVerdict("creativecore", "Fabric", verifiedAt = now.minus(Duration.ofMinutes(5))))
        val calls = AtomicInteger(0)
        val verifier = CandidateVerifier { c ->
            calls.incrementAndGet()
            clientsideReport(c.slug, listOf(loaderVerdict("Fabric", "CreativeCore_FABRIC_", Verdict.ERROR)))
        }
        val grinder = Grinder(verifier, store, reverifyTtl = Duration.ofDays(30), clock = { now })

        Assertions.assertEquals(GrindOutcome.SKIPPED_FRESH, grinder.grind(candidate("creativecore")))
        Assertions.assertEquals(GrindOutcome.VERIFIED, grinder.grind(candidate("creativecore"), force = true))
        Assertions.assertEquals(1, calls.get(), "only the forced grind may re-verify")
        Assertions.assertEquals(
            Verdict.ERROR,
            store.all().single { it.loader == "Fabric" }.verdict,
            "the re-grind replaces the verdict it was queued to correct"
        )
    }

    /** The pool carries the force through to every candidate, or a queued batch would drain into nothing. */
    @Test
    fun theForcedFlagReachesEveryCandidateInAPooledBatch() {
        val now = Instant.parse("2026-06-01T00:00:00Z")
        val store = InMemoryVerdictStore()
        listOf("creativecore", "jei").forEach {
            store.record(grindVerdict(it, "Forge", verifiedAt = now.minus(Duration.ofMinutes(5))))
        }
        val calls = AtomicInteger(0)
        val verifier = CandidateVerifier { c ->
            calls.incrementAndGet()
            clientsideReport(c.slug, listOf(loaderVerdict("Forge", "${c.slug}-", Verdict.ERROR)))
        }
        val grinder = Grinder(verifier, store, reverifyTtl = Duration.ofDays(30), clock = { now })

        val pass = GrindPool(grinder, 2).grindAll(listOf(candidate("creativecore"), candidate("jei")), force = true)

        Assertions.assertEquals(2, pass.verified)
        Assertions.assertEquals(2, calls.get())
    }

    /**
     * The freshness check is per-platform: a fresh Modrinth verdict for `jei` must not stop CurseForge's
     * `jei` — a different project that happens to share a slug — from being ground.
     */
    @Test
    fun aFreshVerdictOnOnePlatformDoesNotSkipTheSameSlugOnAnother() {
        val now = Instant.parse("2026-06-01T00:00:00Z")
        val store = InMemoryVerdictStore()
        store.record(grindVerdict("jei", "Forge", platform = ModPlatforms.MODRINTH, verifiedAt = now.minus(Duration.ofDays(1))))
        val ground = Collections.synchronizedList(mutableListOf<String>())
        val verifier = CandidateVerifier { c ->
            ground.add(c.platform)
            clientsideReport(c.slug, listOf(loaderVerdict("Forge", "jei-", Verdict.CONFIRMED)), platform = c.platform)
        }
        val grinder = Grinder(verifier, store, reverifyTtl = Duration.ofDays(30), clock = { now })

        grinder.grind(GrindCandidate("https://modrinth.com/mod/jei", "jei", 1, ModPlatforms.MODRINTH))
        grinder.grind(GrindCandidate("https://www.curseforge.com/minecraft/mc-mods/jei", "jei", 1, ModPlatforms.CURSEFORGE))

        Assertions.assertEquals(listOf(ModPlatforms.CURSEFORGE), ground, "only the CurseForge project was due")
        Assertions.assertEquals(
            setOf(ModPlatforms.MODRINTH, ModPlatforms.CURSEFORGE),
            store.all().map { it.platform }.toSet(),
            "both platforms' jei coexist in the store"
        )
    }

    @Test
    fun reVerifiesAProjectWhoseVerdictIsStale() {
        val now = Instant.parse("2026-06-01T00:00:00Z")
        val store = InMemoryVerdictStore()
        store.record(grindVerdict("jei", "Forge", verifiedAt = now.minus(Duration.ofDays(40))))
        val calls = AtomicInteger(0)
        val verifier = CandidateVerifier { c -> calls.incrementAndGet(); clientsideReport(c.slug, listOf(loaderVerdict("Forge", "jei-", Verdict.CONFIRMED))) }

        Grinder(verifier, store, reverifyTtl = Duration.ofDays(30), clock = { now }).grind(candidate("jei"))

        Assertions.assertEquals(1, calls.get(), "a verdict older than the TTL must be re-verified")
        Assertions.assertEquals(Verdict.CONFIRMED, store.all().single { it.loader == "Forge" }.verdict)
    }

    @Test
    fun swallowsAThrownVerificationWithoutRecordingOrPropagating() {
        val store = InMemoryVerdictStore()
        val verifier = CandidateVerifier { throw IllegalStateException("boot host exploded") }

        // Must not throw — a bad candidate cannot sink the worker.
        val outcome = Grinder(verifier, store).grind(candidate("doomed"))

        Assertions.assertEquals(GrindOutcome.FAILED, outcome, "a failure is not progress — the daemon throttles on it")
        Assertions.assertTrue(store.all().isEmpty())
    }

    @Test
    fun poolDrainsEveryCandidateAcrossWorkers() {
        val store = InMemoryVerdictStore()
        val verifier = CandidateVerifier { c ->
            Thread.sleep(5)
            clientsideReport(c.slug, listOf(loaderVerdict("Forge", "${c.slug}-", Verdict.CONFIRMED)))
        }
        val candidates = (1..30).map { candidate("mod$it", it.toLong()) }

        val pass = GrindPool(Grinder(verifier, store), workerCount = 4).grindAll(candidates)

        Assertions.assertEquals(30, pass.verified, "every candidate was verified")
        Assertions.assertEquals(candidates.toSet(), pass.reached, "and every candidate was reached")
        Assertions.assertEquals(30, store.all().size)
        Assertions.assertEquals((1..30).map { "mod$it" }.toSet(), store.all().map { it.slug }.toSet())
    }

    @Test
    fun poolProcessesMostPopularFirst() {
        val processed = Collections.synchronizedList(mutableListOf<String>())
        val verifier = CandidateVerifier { c ->
            processed.add(c.slug)
            clientsideReport(c.slug, listOf(loaderVerdict("Forge", "${c.slug}-", Verdict.CONFIRMED)))
        }
        val candidates = listOf(candidate("low", 10), candidate("high", 30), candidate("mid", 20))

        // Single worker => deterministic order, so the popularity ranking is observable.
        GrindPool(Grinder(verifier, InMemoryVerdictStore()), workerCount = 1).grindAll(candidates)

        Assertions.assertEquals(listOf("high", "mid", "low"), processed)
    }

    /**
     * A stop request abandons the rest of the batch instead of draining it, so the daemon's shutdown ends
     * the current pass promptly. The candidate in flight is *not* cancelled — it finishes — which is why
     * the in-flight container is torn down separately by closing the engine.
     */
    @Test
    fun requestStopAbandonsTheRestOfTheBatch() {
        val processed = Collections.synchronizedList(mutableListOf<String>())
        lateinit var pool: GrindPool
        val verifier = CandidateVerifier { c ->
            processed.add(c.slug)
            pool.requestStop() // ask to stop while the very first candidate is still being ground
            clientsideReport(c.slug, listOf(loaderVerdict("Forge", "${c.slug}-", Verdict.CONFIRMED)))
        }
        pool = GrindPool(Grinder(verifier, InMemoryVerdictStore()), workerCount = 1)

        val pass = pool.grindAll((1..20).map { candidate("mod$it", it.toLong()) })

        Assertions.assertEquals(
            setOf(processed.single()), pass.reached.map { it.slug }.toSet(),
            "the abandoned batch reports exactly the one candidate it got to"
        )
        Assertions.assertEquals(1, processed.size, "only the in-flight candidate completes; the queue is dropped")
    }

    /**
     * The pacing input: only verified candidates count. A pass made up of fresh skips and failures reports
     * zero, which is what makes the daemon wait instead of racing its crawl position through the catalog.
     */
    @Test
    fun poolCountsOnlyTheCandidatesItActuallyVerified() {
        val now = Instant.parse("2026-06-01T00:00:00Z")
        val store = InMemoryVerdictStore()
        store.record(grindVerdict("fresh", "Forge", verifiedAt = now.minus(Duration.ofDays(1))))
        val verifier = CandidateVerifier { c ->
            if (c.slug == "doomed") throw IllegalStateException("boot host exploded")
            clientsideReport(c.slug, listOf(loaderVerdict("Forge", "${c.slug}-", Verdict.CONFIRMED)))
        }
        val grinder = Grinder(verifier, store, reverifyTtl = Duration.ofDays(30), clock = { now })

        val candidates = listOf(candidate("fresh"), candidate("doomed"), candidate("due"), candidate("alsoDue"))
        val pass = GrindPool(grinder, workerCount = 2).grindAll(candidates)

        Assertions.assertEquals(2, pass.verified, "one fresh skip and one failure are not work")
        Assertions.assertEquals(
            candidates.toSet(), pass.reached,
            "a fresh skip and a failure are still *reached* — the crawl may advance past them, or a poison " +
                "candidate would stall the sweep forever"
        )
    }

    /**
     * The daemon's shutdown hook interrupts the main thread, which is normally parked in `grindAll` joining
     * its workers. An interrupted join must end the pass, **not** escape as an uncaught `InterruptedException`
     * (observed killing the process with a bare `Exception in thread "main"` on `SIGTERM` mid-pass). The
     * interrupt flag is handed back to the caller so the daemon loop still sees the shutdown.
     */
    @Test
    fun anInterruptedPassStopsInsteadOfThrowing() {
        val firstStarted = CountDownLatch(1)
        val verifier = CandidateVerifier { c ->
            firstStarted.countDown()
            Thread.sleep(50)
            clientsideReport(c.slug, listOf(loaderVerdict("Forge", "${c.slug}-", Verdict.CONFIRMED)))
        }
        val pool = GrindPool(Grinder(verifier, InMemoryVerdictStore()), workerCount = 1)
        val mainThread = Thread.currentThread()
        Thread { firstStarted.await(); mainThread.interrupt() }.apply { isDaemon = true; start() }

        // Must return rather than throw, even though the caller is interrupted mid-join.
        val pass = pool.grindAll((1..40).map { candidate("mod$it", it.toLong()) })

        Assertions.assertTrue(Thread.interrupted(), "the caller's interrupt flag must be restored (and is cleared here)")
        Assertions.assertTrue(pass.verified < 40, "the pass was abandoned, not drained; verified=${pass.verified}")
        Assertions.assertTrue(
            pass.reached.size < 40,
            "an abandoned pass must report the candidates it never reached, so the crawl does not skip them"
        )
    }

    @Test
    fun poolRejectsANonPositiveWorkerCount() {
        val grinder = Grinder({ clientsideReport("x", emptyList()) }, InMemoryVerdictStore())
        assertThrows<IllegalArgumentException> { GrindPool(grinder, workerCount = 0) }
    }
}
