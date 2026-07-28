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
import com.github.dockerjava.api.model.AccessMode
import com.github.dockerjava.api.model.Bind
import com.github.dockerjava.api.model.Capability
import com.github.dockerjava.api.model.Frame
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.Volume
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.time.Duration
import java.util.Collections
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

    override fun run(spec: ContainerSpec, readyPattern: Regex, timeout: Duration): ContainerRunOutput {
        val containerId = client.createContainerCmd(spec.image)
            .withHostConfig(hostConfigFor(spec))
            .withCmd(spec.command)
            .withWorkingDir(spec.workingDir)
            .withUser(spec.user)
            .exec()
            .id

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
                            if (readyPattern.containsMatchIn(line)) {
                                ready.set(true)
                            }
                        }
                    }
                })

            val deadline = System.currentTimeMillis() + timeout.toMillis()
            while (isRunning(containerId) && !ready.get() && System.currentTimeMillis() < deadline) {
                Thread.sleep(500)
            }
            val timedOut = !ready.get() && System.currentTimeMillis() >= deadline

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

    companion object {
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
