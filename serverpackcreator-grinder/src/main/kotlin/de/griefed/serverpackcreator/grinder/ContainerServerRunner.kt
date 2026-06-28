/* Copyright (C) 2025 Griefed
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

import de.griefed.serverpackcreator.clientside.RunResult
import de.griefed.serverpackcreator.clientside.ServerRunner
import java.io.File
import java.time.Duration

/**
 * The grinder's [ServerRunner]: boots a prepared server pack inside an **isolated, network-less
 * container** instead of as a host process, so an untrusted mod can't touch the host and many boots can
 * run in parallel. Drops into `BootVerifier` exactly where `HostProcessServerRunner` would, and feeds
 * the same `BootLogClassifier` (classification stays with the caller via `BootVerifier.outcomeFor`).
 *
 * The container interaction is delegated to a [ContainerEngine], so this class — the host-side staging
 * (start-script check, eula, spec assembly with the hardening defaults) and the result mapping — is
 * unit-testable with a fake engine; only the real [DockerJavaContainerEngine] needs a live daemon.
 *
 * @param engine    The container runtime boundary.
 * @param image     The runtime image the pack is booted in.
 * @param resources CPU/memory/pid caps per boot.
 * @author Griefed
 */
class ContainerServerRunner(
    private val engine: ContainerEngine,
    private val image: String,
    private val resources: ContainerResources = ContainerResources()
) : ServerRunner {

    /** The vanilla server's ready-line, watched in the container's streamed console to stop early. */
    private val readyLine = Regex("""Done \([^)]*\)! For help""")

    /**
     * Mount the [serverPack] into a hardened container and boot it via its `start.sh`, mapping the
     * container's raw output onto a [RunResult]. A pack without a start-script is [RunResult.NotStarted]
     * (never launched), matching the host runner so both report the same "cannot launch" contract.
     */
    override fun run(serverPack: File, timeout: Duration): RunResult {
        val startScript = File(serverPack, "start.sh")
        if (!startScript.isFile) {
            return RunResult.NotStarted("No start.sh in the generated server pack.")
        }
        File(serverPack, "eula.txt").writeText("eula=true\n")

        val spec = ContainerSpec(
            image = image,
            command = listOf("bash", "start.sh"),
            workingDir = PACK_MOUNT,
            mounts = listOf(BindMount(serverPack.absolutePath, PACK_MOUNT, readOnly = false)),
            resources = resources
        )
        val output = engine.run(spec, readyLine, timeout)
        return RunResult.Completed(output.lines, output.exitCode, output.timedOut)
    }

    companion object {
        /** Where the generated server pack is bind-mounted (and the container's working directory). */
        const val PACK_MOUNT = "/srv/pack"
    }
}
