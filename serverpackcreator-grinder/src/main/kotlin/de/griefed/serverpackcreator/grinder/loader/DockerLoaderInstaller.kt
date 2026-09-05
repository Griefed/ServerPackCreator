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
package de.griefed.serverpackcreator.grinder.loader

import de.griefed.serverpackcreator.grinder.container.*
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import java.time.Duration

/** Name of the live install console, written beside the generated pack so it outlives a failed install. */
const val INSTALL_LOG = "install.log"

/**
 * The production [LoaderInstaller]: generates a mod-less pack for the tuple, boots it **once with
 * network** in the runtime container so SPC's `start.sh` installs the loader + Minecraft server +
 * libraries, then snapshots the [InstallLayerSnapshot] into the [LoaderCache] target. Subsequent
 * mod-boots overlay that cached layer and run offline (`--network none`).
 *
 * The install boot is the **only** place network is allowed; it keeps the `ServerStarterJar` re-fetch
 * on (`offline = false`) because it must download `server.jar`. Integration-only — needs a live daemon,
 * the runtime image, and a real `ApiWrapper` behind the generator — so it is not unit-tested; the
 * error-prone pieces it leans on ([InstallLayerSnapshot], [PackVariables], [ImageJavaRuntimes]) are.
 *
 * @param engine         The container runtime.
 * @param image          The runtime image (must carry the JDKs + SPC's shell tooling).
 * @param packGenerator  Produces the mod-less pack for the tuple.
 * @param imageJava      Resolves the bundled JDK for the tuple's Minecraft version.
 * @param installTimeout Budget for the install boot (downloads + first server start).
 * @param resources      CPU/memory/pid caps for the install container.
 * @param containerUser  The `uid:gid` the install runs as; must own the generated pack, or the installer
 *                       cannot save `server.jar` and the install produces no library layer.
 * @author Griefed
 */
class DockerLoaderInstaller(
    private val engine: ContainerEngine,
    private val image: String,
    private val packGenerator: VanillaPackGenerator,
    private val imageJava: ImageJavaRuntimes,
    private val installTimeout: Duration = Duration.ofMinutes(20),
    private val resources: ContainerResources = ContainerResources(),
    private val containerUser: String = ContainerUser.IMAGE_DEFAULT
) : LoaderInstaller {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

    /** Live install console, written into the tuple's cache directory as bookkeeping (see the run below). */
    private val installLogName = INSTALL_LOG

    /** Watched in the install console: once the server is ready, the loader+libraries are fully installed. */
    private val readyLine = Regex("""Done \([^)]*\)! For help""")

    override fun install(target: File, loader: String, loaderVersion: String, minecraftVersion: String): Boolean {
        val pack = packGenerator.generate(loader, loaderVersion, minecraftVersion)
        if (pack == null) {
            log.warn("Vanilla pack generation failed for $loader $loaderVersion / Minecraft $minecraftVersion.")
            return false
        }
        val javaPath = imageJava.javaPath(minecraftVersion)
        if (javaPath == null) {
            log.warn("No bundled JDK for Minecraft $minecraftVersion — cannot install $loader $loaderVersion offline.")
            pack.deleteRecursively()
            return false
        }
        try {
            val preBoot = InstallLayerSnapshot.relativeFilePaths(pack)
            // Unattended boot, but the install still needs network + the ServerStarterJar fetch.
            PackVariables.prepareUnattended(pack, javaPath, offline = false, installerJavaPath = imageJava.installerJavaPathFor(minecraftVersion))

            val spec = ContainerSpec(
                image = image,
                command = listOf("bash", "start.sh"),
                workingDir = PACK_MOUNT,
                mounts = listOf(BindMount(pack.absolutePath, PACK_MOUNT, readOnly = false)),
                resources = resources,
                networkMode = "bridge", // the ONLY networked boot — downloads loader + MC server + libraries
                user = containerUser
            )
            // Stream the install console live into the cache dir, next to the completion marker. This is the
            // slowest phase of a cold grind (minutes of library downloads), so it is the one an operator most
            // needs to watch, and unlike output only returned at the end it survives a kill. Named as cache
            // bookkeeping (leading dot) and so it goes with the tuple when eviction removes it.
            // Deliberately NOT inside `target`: LoaderCache wipes the cache directory when an install fails, which
            // would delete the console exactly when it is the only evidence of *why* it failed. The generated
            // pack's tuple directory survives until that tuple is regenerated.
            val installLog = File(pack.parentFile ?: target, installLogName)
            log.info("Installing $loader $loaderVersion / Minecraft $minecraftVersion — live console: ${installLog.absolutePath}")
            val liveLog = runCatching { installLog.bufferedWriter() }.getOrNull()
            val output = try {
                engine.run(spec, readyLine, installTimeout) { line ->
                    runCatching {
                        liveLog?.appendLine(line)
                        liveLog?.flush()
                    }
                }
            } finally {
                runCatching { liveLog?.close() }
            }

            val copied = InstallLayerSnapshot.copyInstallLayer(pack, preBoot, target)
            val installed = copied > 0 && File(target, "libraries").isDirectory
            if (!installed) {
                // The diagnosis scans the WHOLE console; the quoted tail below does not. An unwritable mount
                // refuses the first writes and the script fails twenty lines later, so the tail alone described
                // a consequence and sent three rounds of diagnosis after the wrong subsystem.
                val diagnosis = InstallFailureDiagnosis.of(output.lines)
                log.warn(
                    "Install produced no library layer for $loader $loaderVersion / Minecraft $minecraftVersion " +
                        "(copied=$copied, exitCode=${output.exitCode}, timedOut=${output.timedOut})." +
                        (diagnosis?.let { " Cause: $it" } ?: "") +
                        " Full console: ${installLog.absolutePath}. Last container output:\n" +
                        output.lines.takeLast(25).joinToString("\n")
                )
            }
            return installed
        } finally {
            pack.deleteRecursively()
        }
    }
}
