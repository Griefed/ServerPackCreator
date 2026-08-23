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

import java.time.Duration

/**
 * Where a server pack is bind-mounted inside a grinder container — and every such container's working
 * directory, since the pack's `start` script expects to run from the pack root. Single source of truth
 * for the mount point: the boot runner, the loader installer and the template matrix must all agree, or
 * a pack would be mounted somewhere its script isn't looking.
 */
const val PACK_MOUNT = "/srv/pack"

/**
 * How long anything the grinder is tearing down gets to exit on its own before it is killed.
 *
 * Applies to both halves of a shutdown, because both are on the same clock: the container is asked to stop
 * with this as its `docker stop` timeout (SIGTERM, then the daemon's own SIGKILL), and the workers get the
 * same window to come back from whatever they were doing. It has to stay comfortably below the unit's
 * `TimeoutStopSec`, or systemd's SIGKILL lands *during* the cleanup that exists to prevent orphans.
 */
val SHUTDOWN_GRACE: Duration = Duration.ofSeconds(15)

/**
 * How many containers are asked to stop at once during shutdown.
 *
 * The window in [SHUTDOWN_GRACE] is *per container*, so anything below the number in flight turns one window
 * into several: at a cap of 8 and ten workers, the container phase alone was 30 seconds and the workers were
 * left with none of the shared budget. A `docker stop` is an HTTP call that spends its time waiting, and
 * concurrent boots are memory-bound at roughly twenty, so a cap well above any real worker count costs nothing
 * and makes the single window the documentation promises actually true.
 */
const val MAX_PARALLEL_STOPS = 64

/**
 * CPU / memory / pid caps applied to every boot container, so one fat modpack can't exhaust the host
 * and a runaway can't peg every core. Defaults are sized for a single Minecraft server boot.
 *
 * @param memoryBytes Hard memory limit (`--memory`); the server's heap must fit inside this.
 * @param cpuQuota    CFS CPU quota in microseconds per the default 100ms period (200_000 = ~2 cores).
 * @param pidsLimit   Maximum process/thread count (`--pids-limit`), guarding against fork-bombs.
 * @author Griefed
 */
data class ContainerResources(
    val memoryBytes: Long = 3L * 1024 * 1024 * 1024,
    val cpuQuota: Long = 200_000,
    val pidsLimit: Long = 512
)

/**
 * A host-path → container-path bind mount.
 *
 * @param hostPath      Absolute path on the host (the generated server pack, or a cached loader tree).
 * @param containerPath Mount point inside the container.
 * @param readOnly      Whether the container may write through the mount.
 * @author Griefed
 */
data class BindMount(val hostPath: String, val containerPath: String, val readOnly: Boolean)

/**
 * Everything needed to launch one isolated boot container. The defaults are the security posture for
 * running an **untrusted** mod: no network (no exfiltration / phone-home / lateral movement), a
 * read-only root filesystem with only an explicit tmpfs writable, every Linux capability dropped, no
 * privilege escalation, and a non-root user. The Docker socket is never mounted.
 *
 * @param image            The runtime image (a JRE + the ServerStarterJar + a fixed entrypoint).
 * @param command          The command to run inside the container (e.g. `bash start.sh`).
 * @param workingDir       Working directory inside the container (where the pack is mounted).
 * @param mounts           Bind mounts (at minimum the generated server pack).
 * @param resources        CPU/memory/pid caps.
 * @param networkMode      Docker network mode; `none` for an untrusted boot.
 * @param readonlyRootfs   Whether the root filesystem is read-only.
 * @param dropAllCapabilities Whether to drop all Linux capabilities.
 * @param noNewPrivileges  Whether to forbid privilege escalation (`no-new-privileges`).
 * @param user             The `uid:gid` to run as (non-root). The default matches the image's own `USER`;
 *                         callers that bind-mount a host directory pass the host owner (see `ContainerUser`).
 * @param tmpfsMounts      Writable tmpfs mount points, needed because the rootfs is read-only.
 * @author Griefed
 */
data class ContainerSpec(
    val image: String,
    val command: List<String>,
    val workingDir: String,
    val mounts: List<BindMount>,
    val resources: ContainerResources = ContainerResources(),
    val networkMode: String = "none",
    val readonlyRootfs: Boolean = true,
    val dropAllCapabilities: Boolean = true,
    val noNewPrivileges: Boolean = true,
    val user: String = "1000:1000",
    val tmpfsMounts: List<String> = listOf("/tmp")
)

/**
 * Raw result of a container run, before any clientside classification: the captured console [lines],
 * the container [exitCode] (`null` if it had to be force-killed) and whether the budget [timedOut]
 * before the ready-line appeared. Mirrors what `de.griefed.serverpackcreator.clientside.RunResult.Completed`
 * needs, so the runner can map straight across and reuse the existing `BootLogClassifier`.
 *
 * @author Griefed
 */
data class ContainerRunOutput(
    /** The container's combined stdout+stderr, in order. What the classifier reads — the console decides, not the exit code. */
    val lines: List<String>,
    /** The container's exit status, or `null` when it could not be determined (killed, or inspect failed). */
    val exitCode: Int?,
    /** Whether the boot ran out of its budget rather than finishing. Suspended host time is excluded; see `SuspendAwareDeadline`. */
    val timedOut: Boolean
)

/**
 * Thin, mockable boundary over the container runtime. An implementation creates + starts a container
 * from a [ContainerSpec], streams its combined console while watching for [run]'s ready pattern, stops
 * it once ready or that call's timeout elapses, and **always removes it** — returning the captured
 * lines + exit status.
 *
 * Keeping the runtime behind this seam (the same pattern as the clientside module's `HttpFetcher`) lets
 * [ContainerServerRunner]'s orchestration be unit-tested with a fake, while the real
 * [DockerJavaContainerEngine] is exercised only against a live daemon.
 *
 * @author Griefed
 */
interface ContainerEngine : AutoCloseable {
    /**
     * Run [spec] to a terminal state, stopping once [readyPattern] is seen or [timeout] elapses, and
     * return its captured output. Implementations must remove the container before returning.
     *
     * [onLine] receives each console line **as it is streamed**, so the caller can persist a boot log live
     * rather than only once the container has exited — without it, a hung boot is undiagnosable until its
     * timeout fires. Implementations must still return every line in [ContainerRunOutput]; the sink is
     * additive, and a throwing sink must not break the run.
     */
    fun run(spec: ContainerSpec, readyPattern: Regex, timeout: Duration, onLine: (String) -> Unit = {}): ContainerRunOutput

    /**
     * Release whatever [run] could not clean up itself — the per-run removal is skipped when the JVM is
     * torn down mid-boot, which would leave a container (and a Minecraft server) running. Part of the seam
     * rather than one implementation, so *any* engine can be drained on shutdown and the contract above
     * ("always removes it") holds even on an interrupted run. Must be idempotent and must not throw; the
     * default is a no-op for engines with nothing to release (e.g. test fakes).
     */
    override fun close() {
        // Nothing to release by default.
    }

    /**
     * Remove containers this engine's *previous* process left behind, returning how many went.
     *
     * Distinct from [close], which cleans up after the process it runs in. Containers are children of the
     * container daemon, not of the unit's control group, so a JVM killed outright — systemd's SIGKILL once
     * `TimeoutStopSec` expires — leaves them running with nothing to tidy them. Called at startup, this is the
     * only thing that ever collects them. Default no-op for engines with no such notion (test fakes).
     */
    fun reapOrphans(): Int = 0
}
