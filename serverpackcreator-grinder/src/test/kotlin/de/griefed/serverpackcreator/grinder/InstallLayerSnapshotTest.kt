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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Pins the install-layer snapshot: copy what a boot *added* (loader jars, `libraries/`, run-scripts,
 * `versions/`/`.fabric/`) into the cache, while skipping pre-existing pack files and runtime state
 * (world, logs, the `*.json` server files, `eula.txt`, crash dumps). Models the real spike layout.
 */
internal class InstallLayerSnapshotTest {

    private fun write(root: File, relativePath: String, body: String = "x") {
        File(root, relativePath).apply { parentFile?.mkdirs() }.writeText(body)
    }

    @Test
    fun copiesOnlyTheAddedNonRuntimeFiles(@TempDir tmp: File) {
        val pack = File(tmp, "pack").apply { mkdirs() }
        // Pre-boot pack files (must NOT be copied — the per-candidate pack provides its own):
        write(pack, "start.sh"); write(pack, "variables.txt"); write(pack, "config/a.toml")
        val preBoot = InstallLayerSnapshot.relativeFilePaths(pack)

        // Post-boot install layer (SSJ + Fabric/Quilt shapes) — MUST be copied:
        write(pack, "libraries/net/foo/foo.jar"); write(pack, "server.jar"); write(pack, "run.sh")
        write(pack, "forge.jar"); write(pack, "versions/1.20.6/server.jar"); write(pack, ".fabric/cache.bin")
        // Runtime state — must NOT be copied:
        write(pack, "world/level.dat"); write(pack, "logs/latest.log"); write(pack, "ops.json")
        write(pack, "eula.txt"); write(pack, "crash-reports/c.txt"); write(pack, "hs_err_pid42.log")

        val target = File(tmp, "cache")
        val copied = InstallLayerSnapshot.copyInstallLayer(pack, preBoot, target)

        val landed = InstallLayerSnapshot.relativeFilePaths(target).map { it.replace(File.separatorChar, '/') }.toSet()
        Assertions.assertEquals(
            setOf("libraries/net/foo/foo.jar", "server.jar", "run.sh", "forge.jar", "versions/1.20.6/server.jar", ".fabric/cache.bin"),
            landed
        )
        Assertions.assertEquals(6, copied)
        // Spot-check the exclusions explicitly:
        Assertions.assertFalse(File(target, "world/level.dat").exists())
        Assertions.assertFalse(File(target, "start.sh").exists(), "pre-boot pack files stay out of the cache")
        Assertions.assertFalse(File(target, "eula.txt").exists())
    }

    @Test
    fun anEmptyDiffCopiesNothing(@TempDir tmp: File) {
        val pack = File(tmp, "pack").apply { mkdirs() }
        write(pack, "start.sh"); write(pack, "libraries/x.jar")
        val all = InstallLayerSnapshot.relativeFilePaths(pack) // pretend everything already existed

        val copied = InstallLayerSnapshot.copyInstallLayer(pack, all, File(tmp, "cache"))

        Assertions.assertEquals(0, copied)
    }
}
