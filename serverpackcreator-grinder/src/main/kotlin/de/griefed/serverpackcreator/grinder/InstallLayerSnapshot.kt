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

import java.io.File

/**
 * Captures the **loader-install layer** of a booted pack for the [LoaderCache]: the files an install
 * produced that are mod-independent and reusable across every mod sharing the tuple — `libraries/`
 * (the bulk), plus the loader's jars/scripts (`server.jar`/`forge.jar`/`*-launcher.jar`/`run.sh`/…).
 *
 * Derived from the loader-install spike (see the module CLAUDE.md): the install layer is
 * loader-specific, so a **denylist** is used rather than a per-loader include-list — snapshot every
 * post-boot file that did not exist pre-boot and is not runtime state. (SPC's `CLEANUP` variable was
 * rejected as the manifest: it misses `forge.jar`, `minecraft_server.*`, `quilt-server-launch.jar`,
 * `versions/`, `.fabric/`, `.cache/`, …)
 *
 * @author Griefed
 */
object InstallLayerSnapshot {

    /** Top-level names that are server **runtime state**, created at boot and never cached. */
    private val runtimeNames = setOf(
        "logs", "crash-reports",
        "ops.json", "whitelist.json", "banned-ips.json", "banned-players.json", "usercache.json",
        "eula.txt", ".previousrun", "README.txt", ".DS_Store"
    )

    /** Whether the pack-relative [relativePath] is runtime state (excluded from the snapshot). */
    private fun isRuntimeState(relativePath: String): Boolean {
        val top = relativePath.substringBefore('/')
        return top in runtimeNames ||
            top.startsWith("world") ||      // world, world_nether, world_the_end
            top.startsWith("hs_err_pid")    // JVM crash dumps
    }

    /** Relative paths of every file in [directory] — the pre-boot baseline to diff against. */
    fun relativeFilePaths(directory: File): Set<String> =
        directory.walkTopDown().filter { it.isFile }.map { it.relativeTo(directory).path }.toSet()

    /**
     * Copy the install layer from [packDir] into [targetDir]: every file that is **not** in
     * [preBootRelativePaths] and **not** runtime state, preserving its relative path. Returns the
     * number of files copied (callers treat 0 — or a missing `libraries/` — as a failed install).
     */
    fun copyInstallLayer(packDir: File, preBootRelativePaths: Set<String>, targetDir: File): Int {
        var copied = 0
        packDir.walkTopDown().filter { it.isFile }.forEach { file ->
            val relativePath = file.relativeTo(packDir).path
            if (relativePath in preBootRelativePaths || isRuntimeState(relativePath)) {
                return@forEach
            }
            val destination = File(targetDir, relativePath)
            destination.parentFile?.mkdirs()
            file.copyTo(destination, overwrite = true)
            copied++
        }
        return copied
    }
}
