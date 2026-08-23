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

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.model.*
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient
import de.griefed.serverpackcreator.clientside.SuspendAwareDeadline
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The production [ContainerEngine]: drives the Docker Engine API (via docker-java + the zerodep
 * transport) to create a hardened container from a [ContainerSpec], stream its console while watching
 * for the ready-line, stop it once ready or timed out, read its exit code, and force-remove it.
 *
 * Not unit-tested — it needs a live daemon — which is exactly why the orchestration that *can* be
 * tested lives in [ContainerServerRunner] behind the [ContainerEngine] seam. This class is the thin,
 * imperative docker-java translation only.
 *
 * @param client The docker-java client; defaults to one built from the ambient Docker environment.
 * @param shutdownGrace How long a container gets to exit on its own during [close] before it is killed.
 *                      A parameter so a test can shorten it; production always takes [SHUTDOWN_GRACE].
 * @author Griefed
 */
class DockerJavaContainerEngine(
    private val client: DockerClient = defaultClient(),
    private val shutdownGrace: Duration = SHUTDOWN_GRACE
) : ContainerEngine {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

    /**
     * Containers currently owned by this engine. [run]'s `finally` removes a container on the normal
     * path, but that block never executes if the JVM is torn down mid-boot — which is exactly what a
     * `SIGTERM` to the daemon does — leaking a running Minecraft server. [close] force-removes whatever
     * is still tracked, so shutdown cleans up after in-flight work.
     */
    private val liveContainers: MutableSet<String> = Collections.newSetFromMap(ConcurrentHashMap())

    /**
     * Set by [close] before it sweeps, so a container cannot be created behind it.
     *
     * `GrindPool.requestStop` only stops a worker taking a *new candidate*, and once shutdown hooks are running
     * the JVM no longer waits for worker threads — so a worker between its loader install and its mod boot could
     * start a container after the sweep and have it outlive the process.
     */
    private val closed = AtomicBoolean(false)

    override fun run(spec: ContainerSpec, readyPattern: Regex, timeout: Duration, onLine: (String) -> Unit): ContainerRunOutput {
        check(!closed.get()) { "The container engine is shutting down; refusing to start a new container." }
        val containerId = client.createContainerCmd(spec.image)
            .withHostConfig(hostConfigFor(spec))
            .withCmd(spec.command)
            .withWorkingDir(spec.workingDir)
            .withUser(spec.user)
            // Stamped so a container that outlives its JVM can still be identified. Nothing else can find it:
            // it has no name, no autoremove, and the tracking set above dies with the process.
            .withLabels(mapOf(OWNER_LABEL to "1"))
            .exec()
            .id
        liveContainers.add(containerId)
        // Re-checked after the add, not only before the create: close() may have swept in between, and then
        // this container is in nobody's list. Either close() sees it here, or this sees close().
        if (closed.get()) {
            runCatching { client.removeContainerCmd(containerId).withForce(true).exec() }
            liveContainers.remove(containerId)
            throw IllegalStateException("The container engine shut down while this container was being created.")
        }

        val lines = Collections.synchronizedList(ArrayList<String>())
        val ready = AtomicBoolean(false)
        try {
            client.startContainerCmd(containerId).exec()
            val logStream = client.logContainerCmd(containerId)
                .withStdOut(true)
                .withStdErr(true)
                .withFollowStream(true)
                .withTailAll()
                .exec(object : ResultCallback.Adapter<Frame>() {
                    override fun onNext(frame: Frame) {
                        for (line in String(frame.payload, Charsets.UTF_8).split("\n")) {
                            if (line.isEmpty()) continue
                            lines.add(line)
                            // Hand the line on at once; a throwing sink must never break the boot stream.
                            runCatching { onLine(line) }
                            if (readyPattern.containsMatchIn(line)) {
                                ready.set(true)
                            }
                        }
                    }
                })

            // The budget must not be spent while the host is asleep. A suspend freezes the container mid-boot, and a
            // wall-clock deadline then expires on a server that never got the time — measured 2026-07-31, a laptop
            // idle-sleeping in ~16-minute cycles produced 19 of 153 verdicts reading `timed out`, several of them
            // `SURVIVED (timed out)` whose console showed the server reaching ready seconds after launch. Each
            // suspended interval is added back to the deadline, so the timeout means "the boot had this long and did
            // not make it" rather than "this much clock passed".
            val deadline = SuspendAwareDeadline(timeout, POLL_INTERVAL_MILLIS) { gapMillis ->
                log.warn(
                    "The host appears to have suspended for ~${gapMillis / 1000}s while booting; that time is not counted " +
                        "against the boot's ${timeout.toMinutes()}-minute budget. Keep the machine awake for a sweep " +
                        "(e.g. `caffeinate -ims`) — a boot interrupted this way learns nothing either way."
                )
            }
            while (isRunning(containerId) && !ready.get() && deadline.hasTimeLeft()) {
                Thread.sleep(POLL_INTERVAL_MILLIS)
                deadline.tick()
            }
            val timedOut = !ready.get() && !deadline.hasTimeLeft()

            // A server that became ready stays up by design, so stop it; classification keys on the
            // captured lines + exit code, never on liveness. Kill if a graceful stop fails.
            if (isRunning(containerId)) {
                runCatching { client.stopContainerCmd(containerId).withTimeout(10).exec() }
                    .onFailure { runCatching { client.killContainerCmd(containerId).exec() } }
            }
            runCatching { logStream.close() }

            return ContainerRunOutput(ArrayList(lines), exitCodeOf(containerId), timedOut)
        } finally {
            runCatching { client.removeContainerCmd(containerId).withForce(true).exec() }
                .onFailure { failure ->
                    // On shutdown `close()` has already removed it, so this is the expected 404 rather than a
                    // problem -- and a WARN here is indistinguishable in the journal from a removal that really
                    // did fail, which is exactly the noise that makes a real one hard to spot.
                    if (closed.get()) {
                        log.debug("Container $containerId was already removed by the shutdown sweep.")
                    } else {
                        log.warn("Could not remove container $containerId: ${failure.message}")
                    }
                }
            liveContainers.remove(containerId)
        }
    }

    /**
     * Force-remove every container this engine still owns. Called on shutdown so a boot interrupted by a
     * `SIGTERM` cannot leave a Minecraft server running — [run]'s `finally` is skipped when the JVM dies
     * mid-boot. Safe to call repeatedly and never throws: a container that already vanished is fine.
     */
    override fun close() {
        closed.set(true)
        val abandoned = liveContainers.toList()
        if (abandoned.isEmpty()) {
            return
        }
        log.info(
            "Stopping ${abandoned.size} container(s) abandoned by an interrupted run — " +
                "${shutdownGrace.seconds}s to exit on their own, then killed."
        )
        // Concurrently, because the grace window is per container: ten workers stopped one after another would
        // be ten times the window, and would blow through the unit's TimeoutStopSec into the SIGKILL this whole
        // path exists to avoid.
        val stoppers = Executors.newFixedThreadPool(minOf(abandoned.size, MAX_PARALLEL_STOPS))
        try {
            abandoned.map { containerId -> stoppers.submit { stopThenRemove(containerId) } }
                .forEach { pending -> runCatching { pending.get() } }
        } finally {
            stoppers.shutdownNow()
        }
    }

    /**
     * Ask one container to exit, then remove it. `docker stop` with a timeout is SIGTERM followed by the
     * daemon's own SIGKILL once the window passes, which is what gives a Minecraft server the chance to save
     * its world — going straight to `remove --force`, as this used to, is a SIGKILL to PID 1 with no warning.
     */
    private fun stopThenRemove(containerId: String) {
        runCatching { client.stopContainerCmd(containerId).withTimeout(shutdownGrace.seconds.toInt()).exec() }
            .onFailure { log.debug("Container $containerId did not stop cleanly: ${it.message}") }
        runCatching { client.removeContainerCmd(containerId).withForce(true).exec() }
            .onFailure { log.warn("Could not remove abandoned container $containerId: ${it.message}") }
        liveContainers.remove(containerId)
    }

    /**
     * Remove every container carrying [OWNER_LABEL], which at startup can only be an orphan of a previous
     * process — this engine has started none yet.
     *
     * **LANDMINE: this assumes one grinder per Docker daemon.** The label says "a grinder made this", not
     * "*this* grinder made this", so a second instance sharing the daemon would have its in-flight boots
     * removed by the first one's startup. The shipped unit is a singleton service, which is what makes the
     * simple label safe; anything else needs a per-instance label first.
     */
    override fun reapOrphans(): Int {
        val orphans = runCatching {
            client.listContainersCmd().withShowAll(true)
                .withLabelFilter(mapOf(OWNER_LABEL to "1"))
                .exec()
        }.getOrElse {
            log.warn("Could not list containers to reap orphans: ${it.message}")
            return 0
        }
        if (orphans.isEmpty()) {
            return 0
        }
        log.info("Reaping ${orphans.size} container(s) left behind by a previous run — a kill, not a clean stop.")
        return orphans.count { orphan ->
            runCatching { client.removeContainerCmd(orphan.id).withForce(true).exec() }
                .onFailure { log.warn("Could not reap orphaned container ${orphan.id}: ${it.message}") }
                .isSuccess
        }
    }

    /** Translate the platform-agnostic [spec] into a docker-java [HostConfig] with the hardening on. */
    private fun hostConfigFor(spec: ContainerSpec): HostConfig {
        val hostConfig = HostConfig.newHostConfig()
            .withNetworkMode(spec.networkMode)
            .withMemory(spec.resources.memoryBytes)
            .withCpuQuota(spec.resources.cpuQuota)
            .withPidsLimit(spec.resources.pidsLimit)
            .withReadonlyRootfs(spec.readonlyRootfs)
            .withBinds(spec.mounts.map { Bind(it.hostPath, Volume(it.containerPath), if (it.readOnly) AccessMode.ro else AccessMode.rw) })
        if (spec.dropAllCapabilities) {
            hostConfig.withCapDrop(Capability.ALL)
        }
        if (spec.noNewPrivileges) {
            hostConfig.withSecurityOpts(listOf("no-new-privileges"))
        }
        if (spec.tmpfsMounts.isNotEmpty()) {
            hostConfig.withTmpFs(spec.tmpfsMounts.associateWith { "rw" })
        }
        return hostConfig
    }

    /** Whether the container is currently running, defaulting to false if it can no longer be found. */
    private fun isRunning(containerId: String): Boolean =
        runCatching { client.inspectContainerCmd(containerId).exec().state.running == true }.getOrDefault(false)

    /** The container's exit code, or `null` if it is unavailable (still running / not found). */
    private fun exitCodeOf(containerId: String): Int? =
        runCatching { client.inspectContainerCmd(containerId).exec().state.exitCodeLong?.toInt() }.getOrNull()

    /** Poll interval, the suspend-gap threshold and the ambient-environment Docker client factory. */

    companion object {
        /** How often the boot's liveness and ready-state are polled. */
        internal const val POLL_INTERVAL_MILLIS = 500L

        /**
         * Docker label every container this engine creates carries, so one that outlives its JVM can still be
         * found. Without it an orphan is indistinguishable from any other container on the host.
         */
        const val OWNER_LABEL = "de.griefed.serverpackcreator.grinder"

        /** Cap on concurrent stop requests during shutdown, so a large worker count cannot flood the daemon. */
        private const val MAX_PARALLEL_STOPS = 8

        /** Build a [DockerClient] from the ambient Docker environment (DOCKER_HOST, TLS settings, …). */
        fun defaultClient(): DockerClient {
            val config = DefaultDockerClientConfig.createDefaultConfigBuilder().build()
            val httpClient = ZerodepDockerHttpClient.Builder()
                .dockerHost(config.dockerHost)
                .sslConfig(config.sslConfig)
                .build()
            return DockerClientImpl.getInstance(config, httpClient)
        }
    }
}
