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
package de.griefed.serverpackcreator.grinder.container

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.time.Duration

/**
 * Integration test for the one piece no unit test can cover: [DockerJavaContainerEngine] against a
 * **live Docker daemon**. Gated behind `GRINDER_DOCKER_IT=1` so it never runs in a daemon-less CI; it
 * needs the `busybox:latest` image present (`docker pull busybox`). Exercises the full path —
 * create → start → stream logs → ready-detect/stop (or natural exit) → inspect exit code → remove —
 * under the production hardening defaults (`--network none`, read-only rootfs, dropped caps, non-root).
 */
@EnabledIfEnvironmentVariable(named = "GRINDER_DOCKER_IT", matches = "1")
internal class DockerJavaContainerEngineIT {

    private val engine = DockerJavaContainerEngine()

    private fun busyboxSpec(script: String) = ContainerSpec(
        image = "busybox:latest",
        command = listOf("sh", "-c", script),
        workingDir = "/",
        mounts = emptyList()
    )

    @Test
    fun capturesConsoleAndNonZeroExitFromARealContainer() {
        val output = engine.run(
            busyboxSpec("echo hello-from-container; echo crashing-now; exit 3"),
            readyPattern = Regex("this-never-appears"),
            timeout = Duration.ofSeconds(30)
        )

        Assertions.assertTrue(output.lines.any { it.contains("hello-from-container") }, "stdout must be captured: ${output.lines}")
        Assertions.assertEquals(3, output.exitCode, "the container's non-zero exit must be read back")
        Assertions.assertFalse(output.timedOut)
    }

    /**
     * Seeing the ready line must end the run, rather than letting the container live out its sleep.
     *
     * "Did not wait out the sleep" is asserted by what the container *printed*, not by how long the test
     * took. The clock answers a different question on every host: this bound was 30s, failed run 726 at
     * 38s, was raised to 90s, and failed run 834 at 101s — three reds, no defect, because what it actually
     * measured was how loaded the daemon was. [SLEPT_THROUGH] can only appear if the shell got past
     * `sleep`, so the marker's absence discriminates between the two *outcomes* where the clock
     * discriminated between two machines.
     *
     * Worth stating what that gives up, because it is not nothing: a daemon that honoured the stop but took
     * a minute to do it now passes. That was never this test's subject — [closeSignalsAContainerBeforeKillingIt]
     * owns the stop path — and it is the only thing the old bound caught that `timedOut` did not.
     */
    @Test
    fun detectsReadyLineAndStopsALongRunningContainerPromptly() {
        val output = engine.run(
            busyboxSpec("echo 'Done (2.5s)! For help, type help'; sleep 120; echo $SLEPT_THROUGH"),
            readyPattern = Regex("""Done \([^)]*\)! For help"""),
            timeout = Duration.ofSeconds(60)
        )

        Assertions.assertTrue(output.lines.any { it.contains("For help") }, "ready line must be captured: ${output.lines}")
        Assertions.assertFalse(output.timedOut, "ready was seen, so this is not a timeout")
        Assertions.assertTrue(
            output.lines.none { it.contains(SLEPT_THROUGH) },
            "the run must end on the ready line; reaching '$SLEPT_THROUGH' means it waited out the " +
                "container's 120s sleep instead: ${output.lines}"
        )
    }

    /**
     * The shutdown drain: a boot abandoned by JVM teardown never reaches [DockerJavaContainerEngine.run]'s
     * `finally`, so [DockerJavaContainerEngine.close] must remove whatever is still in flight. Simulated by
     * starting a long-running container on another thread and closing the engine while it runs — observed
     * once for real, when a `SIGTERM` mid-boot left a Minecraft server container behind.
     */
    @Test
    fun closeRemovesAContainerLeftRunningByAnAbandonedRun() {
        val drainEngine = DockerJavaContainerEngine()
        val booting = Thread {
            runCatching {
                drainEngine.run(busyboxSpec("echo booting; sleep 300"), Regex("this-never-appears"), Duration.ofMinutes(5))
            }
        }.apply { isDaemon = true; start() }

        // Wait for the container to actually exist before pulling the rug out.
        val deadline = System.currentTimeMillis() + FIXTURE_DAEMON_BUDGET_MILLIS
        var running = runningContainersOf(drainEngine)
        while (running == 0 && System.currentTimeMillis() < deadline) {
            Thread.sleep(500)
            running = runningContainersOf(drainEngine)
        }
        Assertions.assertTrue(running > 0, "the probe container should be running before close()")

        drainEngine.close()
        booting.interrupt()

        // close() force-removes, so the sleeper must be gone almost immediately.
        val goneBy = System.currentTimeMillis() + FIXTURE_DAEMON_BUDGET_MILLIS
        while (runningContainersOf(drainEngine) > 0 && System.currentTimeMillis() < goneBy) {
            Thread.sleep(500)
        }
        Assertions.assertEquals(0, runningContainersOf(drainEngine), "close() must leave none of its containers running")
    }

    /**
     * The CPU cap must land in the *kernel's* view, not merely in the request we sent.
     *
     * Read from inside the container, because that is the only place the answer is authoritative: docker echoing
     * back a `HostConfig` proves the field was transmitted, and nothing more. A quota is meaningless without the
     * period it divides, and this is the guard for sending both — with the period omitted the cap silently
     * becomes whatever the daemon's default period makes it.
     */
    @Test
    fun theCpuCapReachesTheKernelWithItsPeriod() {
        // A *non-default* period on purpose: at the kernel's own 100ms, a container created without the period
        // being sent at all would report the right numbers anyway, and the guard would have no teeth.
        val requested = ContainerResources.forCpus(1.5, ContainerResources(cpuPeriod = 50_000))
        // cgroup v2 states both numbers in one file ("150000 100000"); v1 splits them. Try v2, fall back.
        val spec = busyboxSpec("cat /sys/fs/cgroup/cpu.max 2>/dev/null || cat /sys/fs/cgroup/cpu/cpu.cfs_quota_us /sys/fs/cgroup/cpu/cpu.cfs_period_us")
            .copy(resources = requested)

        val output = engine.run(spec, Regex("this-never-appears"), Duration.ofSeconds(30))
        val reported = output.lines.joinToString(" ").split(Regex("\\s+")).mapNotNull { it.toLongOrNull() }

        Assertions.assertTrue(
            reported.containsAll(listOf(requested.cpuQuota, requested.cpuPeriod)),
            "the container's own cgroup must show quota ${requested.cpuQuota} and period ${requested.cpuPeriod}, " +
                "saw: ${output.lines}"
        )
    }

    /**
     * How many containers [engine] has *running*, by instance label.
     *
     * Was a count of every `busybox … sleep 300` on the daemon, which is a description of the fixture rather
     * than of ownership — and matched the identical container a concurrent job was running.
     */
    private fun runningContainersOf(engine: DockerJavaContainerEngine): Int =
        DockerJavaContainerEngine.defaultClient().listContainersCmd().withShowAll(false)
            .withLabelFilter(mapOf(DockerJavaContainerEngine.INSTANCE_LABEL to engine.instanceId))
            .exec().size

    /**
     * `systemctl stop` must *signal* a container, not shoot it. `close` therefore issues a `docker stop` with a
     * bounded grace window before force-removing, so a Minecraft server gets its chance to save and exit — the
     * previous behaviour went straight to `remove --force`, which is a SIGKILL to PID 1 and loses the world save
     * of an in-flight boot.
     *
     * Asserted on the container's own exit: a shell trapping TERM writes its marker and exits 0 only if the
     * signal actually arrived.
     */
    @Test
    fun closeSignalsAContainerBeforeKillingIt() {
        // A short window: this asserts that the signal is *sent and honoured*, not how long production waits.
        val signalEngine = DockerJavaContainerEngine(shutdownGrace = Duration.ofSeconds(5))
        val sawSignal = java.util.concurrent.atomic.AtomicBoolean(false)
        val booting = Thread {
            runCatching {
                signalEngine.run(
                    busyboxSpec("trap 'echo GRACEFUL-TERM; exit 0' TERM; echo ready-to-be-stopped; while true; do sleep 1; done"),
                    Regex("this-never-appears"),
                    Duration.ofMinutes(5)
                ) { line -> if (line.contains("GRACEFUL-TERM")) sawSignal.set(true) }
            }
        }.apply { isDaemon = true; start() }

        waitForContainerOf(signalEngine)
        signalEngine.close()
        booting.join(FIXTURE_DAEMON_BUDGET_MILLIS)

        Assertions.assertTrue(sawSignal.get(), "the container must receive SIGTERM and get to run its handler before removal")
        // What `close` promises is that nothing of this engine's is left *running*, not that nothing is left at
        // all: when the grace window expires it logs "abandoning them so shutdown can finish ... reaped on the
        // next start" and returns with the container still listed. `containersOf` is withShowAll(true), so the
        // old form asserted removal -- a promise close does not make -- and sampled it instantly. Run 719 is
        // what that costs: close hit its 5s window, logged the documented abandonment, and this failed
        // "nothing may be left running after close ==> expected: <true> but was: <false>" on a daemon doing
        // exactly what it was told.
        Assertions.assertEquals(0, awaitNothingRunning(signalEngine), "nothing may be left running after close")
    }

    /**
     * The teardown race: `requestStop` only stops a worker taking a *new candidate*, and once shutdown hooks are
     * running the JVM no longer waits for worker threads — so a worker between its loader install and its mod
     * boot could create a container *after* `close` had already swept, and that one was never removed.
     */
    @Test
    fun refusesToCreateAContainerOnceClosed() {
        val closedEngine = DockerJavaContainerEngine()
        closedEngine.close()

        Assertions.assertThrows(IllegalStateException::class.java) {
            closedEngine.run(busyboxSpec("echo should-never-start"), Regex("x"), Duration.ofSeconds(30))
        }
        Assertions.assertTrue(containersOf(closedEngine).isEmpty(), "a refused run must leave nothing behind")
    }

    /**
     * The SIGKILL case, which no in-process hook can cover: systemd kills the JVM before `close` finishes and the
     * containers keep running, parented by the docker daemon rather than the unit's cgroup. They carry a label so
     * the next start can find and remove them — without one, an orphan survives every restart forever.
     *
     * **This is also the one test here that cannot be scoped, and the reason `grinder-container-it.yml` holds a
     * repository-wide lock.** Reaping is global by definition — an orphan is a container whose maker is gone, and
     * the daemon cannot say who is gone — so this removes every grinder container on the machine, including a
     * concurrent job's live ones. Its *assertions* are scoped to the orphan it made; the side effect is not.
     */
    @Test
    fun reapsALabelledOrphanLeftByAPreviousProcess() {
        val orphanEngine = DockerJavaContainerEngine()
        Thread {
            runCatching {
                orphanEngine.run(busyboxSpec("echo orphan-alive; sleep 300"), Regex("this-never-appears"), Duration.ofMinutes(5))
            }
        }.apply { isDaemon = true; start() }
        waitForContainerOf(orphanEngine)
        // Forget the container the way a killed JVM does: the tracking set dies with the process, the container
        // does not. A fresh engine is exactly what the next `systemctl start` brings up.
        Assertions.assertEquals(1, runningContainersOf(orphanEngine), "test setup: the orphan must be running")

        // The return value is deliberately not asserted here: reaping is global, so the count includes
        // whatever else the machine had orphaned, and `>= 1` would pass against a reap that took fifty
        // containers — which is the scenario that breaks a concurrent job. What this test owns is whether
        // *its* orphan went. `ContainerOwnershipIT` pins the count where a known foreign container makes
        // it exact enough to mean something.
        DockerJavaContainerEngine().reapOrphans()

        Assertions.assertTrue(awaitGone(orphanEngine).isEmpty(), "the orphan may not survive the reap")
        // The engine that made the orphan is still open, and its worker is still polling a container that no
        // longer exists. Close it here rather than leaving the only test in this file that does not tidy up.
        orphanEngine.close()
    }

    /**
     * A container's own hostname must resolve, even with no network.
     *
     * `--network none` gives the daemon no address to map, so it writes no `<ip> <hostname>` line into
     * `/etc/hosts` — the line every *networked* container gets. `getaddrinfo` on the container's own name then
     * fails, and the first thing a Minecraft server does is ask for it: log4j calls
     * `InetAddress.getLocalHost()` while configuring itself, so every boot opened with three
     * `UnknownHostException: <container-id>: Temporary failure in name resolution` stacktraces before any mod
     * was touched.
     *
     * Asserted through `wget`, which calls the same `getaddrinfo` the JVM does, against a port nothing listens
     * on: a resolved name reaches the connect and is refused, an unresolved one never gets that far and reports
     * a bad address. Reading `/etc/hosts` would only show that a line was written, not that the resolver uses it.
     */
    @Test
    fun theContainersOwnHostnameResolvesWithoutANetwork() {
        val output = engine.run(
            busyboxSpec("""wget -q -T 1 -O - "http://${'$'}(hostname):1/" 2>&1"""),
            readyPattern = Regex("this-never-appears"),
            timeout = Duration.ofSeconds(30)
        )
        val console = output.lines.joinToString("\n")

        Assertions.assertFalse(
            console.contains("bad address"),
            "the container's own hostname must resolve — the boot's first log4j call is getLocalHost(): $console"
        )
        Assertions.assertTrue(
            console.contains("Connection refused"),
            "resolution must get as far as a connect (refused, since nothing listens): $console"
        )
    }

    /**
     * A boot must be able to execute a native library it extracted into `/tmp`, while the rest of that mount's
     * hardening stays on.
     *
     * Docker mounts a `--tmpfs` `nosuid,nodev,noexec` by default, and the rootfs is read-only, so anything that
     * writes a `.so` and maps it executable fails — which is what JNA does, and what Minecraft's own `oshi`
     * system-report probes need. Measured with the production posture otherwise unchanged (no network, read-only
     * rootfs, all caps dropped, no-new-privileges):
     *
     * | `/tmp` | JNA loading its native library |
     * |---|---|
     * | `rw` | `UnsatisfiedLinkError: /tmp/jna….tmp: failed to map segment from shared object` |
     * | `rw,exec` | `JNA-OK pointerSize=8` |
     *
     * That failure reaches a boot console as `NoClassDefFoundError: Could not initialize class
     * com.sun.jna.Native` (seen in `Modrinth-polytone-NeoForge.log`), and a mod needing JNA *at load time* would
     * therefore die for the environment and arrive at the classifier looking like a crash.
     *
     * Asserted by **executing** a binary out of `/tmp` rather than by reading the mount flags, because the flag
     * is the mechanism and running the file is the promise. The flags are then checked for what must *not* have
     * been given away: `nosuid` and `nodev` stay, and only `noexec` goes.
     */
    @Test
    fun aBootCanExecuteFromItsTmpfsWhileKeepingTheRestOfItsHardening() {
        val output = engine.run(
            busyboxSpec("cp /bin/busybox /tmp/echo && /tmp/echo EXEC-FROM-TMPFS-WORKS; grep ' /tmp ' /proc/mounts"),
            readyPattern = Regex("this-never-appears"),
            timeout = Duration.ofSeconds(30)
        )
        val console = output.lines.joinToString("\n")

        Assertions.assertTrue(
            console.contains("EXEC-FROM-TMPFS-WORKS"),
            "a boot must be able to run a native library it unpacked into /tmp: $console"
        )
        val tmpMount = output.lines.firstOrNull { it.contains(" /tmp ") }
            ?: Assertions.fail("no /tmp mount line in: $console")
        Assertions.assertFalse(tmpMount.contains("noexec"), "noexec is what this grants away: $tmpMount")
        Assertions.assertTrue(tmpMount.contains("nosuid"), "nosuid must NOT be given away with it: $tmpMount")
        Assertions.assertTrue(tmpMount.contains("nodev"), "nodev must NOT be given away with it: $tmpMount")
    }

    /**
     * The containers [engine] has on the daemon — **its own**, by instance label.
     *
     * Scoped deliberately. Asking for every container with the owner label asks about the whole machine, and
     * on 2026-09-27 the whole machine had a second `test.yml` job on it: runs 954 and 956 reached this class
     * seven seconds apart and each failed a *different* test of it. A test may only assert about what it did.
     */
    private fun containersOf(engine: DockerJavaContainerEngine): List<String> =
        DockerJavaContainerEngine.defaultClient().listContainersCmd().withShowAll(true)
            .withLabelFilter(mapOf(DockerJavaContainerEngine.INSTANCE_LABEL to engine.instanceId))
            .exec().map { it.id }

    /**
     * Block until [engine]'s own container is actually up, so a test never pulls the rug before there is one.
     *
     * Budgeted by [FIXTURE_DAEMON_BUDGET_MILLIS], which is deliberately far larger than any healthy
     * daemon needs: this runs on a runner that shares its Docker daemon with whatever else CI is doing, and
     * when the wait expires the failure surfaces as the *next* assertion — "test setup: the orphan must be
     * running ==> expected 1 but was 0" (run 681) — which reads as a verdict about reaping rather than as a
     * slow daemon. A fixture that gives up too early does not fail, it misattributes.
     */
    private fun waitForContainerOf(engine: DockerJavaContainerEngine) {
        val until = System.currentTimeMillis() + FIXTURE_DAEMON_BUDGET_MILLIS
        while (System.currentTimeMillis() < until && runningContainersOf(engine) == 0) {
            Thread.sleep(200)
        }
        // Loudly, because the doc above is right about what silence costs: an expiry used to surface as the
        // *next* assertion and read as a verdict about reaping. `runningContainersOf`, not `containersOf`:
        // the latter is withShowAll(true) and so answers the moment `run` has *created* the container,
        // several statements before `startContainerCmd`, which is a state no assertion here is about.
        Assertions.assertNotEquals(
            0,
            runningContainersOf(engine),
            "the fixture never got this engine's container running within " +
                "${FIXTURE_DAEMON_BUDGET_MILLIS / 1000}s; the daemon was too slow, which is not what any " +
                "test in this class is about"
        )
    }

    /**
     * Wait for [engine] to have nothing running, which is what `close` promises -- not "nothing left at all".
     *
     * `close` gives the whole sweep one grace window and then interrupts its own stoppers so shutdown cannot
     * hang, logging that it is abandoning what is left for the next start to reap. A busy daemon misses that
     * window routinely, and the container is stopped a moment later. Mirrors `ContainerOwnershipIT`.
     */
    private fun awaitNothingRunning(engine: DockerJavaContainerEngine): Int {
        val until = System.currentTimeMillis() + FIXTURE_DAEMON_BUDGET_MILLIS
        var running = runningContainersOf(engine)
        while (running > 0 && System.currentTimeMillis() < until) {
            Thread.sleep(250)
            running = runningContainersOf(engine)
        }
        return running
    }

    /**
     * Wait for [engine]'s containers to be gone entirely, for the one assertion that really is about removal.
     *
     * `removeContainerCmd` returns before the daemon has finished: run 719 logged
     * `409: removal of container ... is already in progress` against a container a reap had just taken, so a
     * removal that succeeded can still be listed for a moment afterwards.
     */
    private fun awaitGone(engine: DockerJavaContainerEngine): List<String> {
        val until = System.currentTimeMillis() + FIXTURE_DAEMON_BUDGET_MILLIS
        var present = containersOf(engine)
        while (present.isNotEmpty() && System.currentTimeMillis() < until) {
            Thread.sleep(250)
            present = containersOf(engine)
        }
        return present
    }

    private companion object {
        /**
         * Printed by the sleeper only if its `sleep` ran to completion, which a run that stopped on ready
         * can never reach. Shares no substring with any other line the container emits, so
         * `lines.none { it.contains(...) }` cannot match something else and quietly pass.
         */
        const val SLEPT_THROUGH = "SLEPT-THROUGH-THE-READY-LINE"
    }
}
