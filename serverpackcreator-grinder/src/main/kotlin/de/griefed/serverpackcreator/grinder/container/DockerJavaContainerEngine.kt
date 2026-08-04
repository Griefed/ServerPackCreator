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
 * @author Griefed
 */
class DockerJavaContainerEngine(
    private val client: DockerClient = defaultClient()
) : ContainerEngine {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

    /**
     * Containers currently owned by this engine. [run]'s `finally` removes a container on the normal
     * path, but that block never executes if the JVM is torn down mid-boot — which is exactly what a
     * `SIGTERM` to the daemon does — leaking a running Minecraft server. [close] force-removes whatever
     * is still tracked, so shutdown cleans up after in-flight work.
     */
    private val liveContainers: MutableSet<String> = Collections.newSetFromMap(ConcurrentHashMap())

    override fun run(spec: ContainerSpec, readyPattern: Regex, timeout: Duration, onLine: (String) -> Unit): ContainerRunOutput {
        val containerId = client.createContainerCmd(spec.image)
            .withHostConfig(hostConfigFor(spec))
            .withCmd(spec.command)
            .withWorkingDir(spec.workingDir)
            .withUser(spec.user)
            .exec()
            .id
        liveContainers.add(containerId)

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
                .onFailure { log.warn("Could not remove container $containerId: ${it.message}") }
            liveContainers.remove(containerId)
        }
    }

    /**
     * Force-remove every container this engine still owns. Called on shutdown so a boot interrupted by a
     * `SIGTERM` cannot leave a Minecraft server running — [run]'s `finally` is skipped when the JVM dies
     * mid-boot. Safe to call repeatedly and never throws: a container that already vanished is fine.
     */
    override fun close() {
        val abandoned = liveContainers.toList()
        if (abandoned.isEmpty()) {
            return
        }
        log.info("Removing ${abandoned.size} container(s) abandoned by an interrupted run.")
        for (containerId in abandoned) {
            runCatching { client.removeContainerCmd(containerId).withForce(true).exec() }
                .onFailure { log.warn("Could not remove abandoned container $containerId: ${it.message}") }
            liveContainers.remove(containerId)
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
