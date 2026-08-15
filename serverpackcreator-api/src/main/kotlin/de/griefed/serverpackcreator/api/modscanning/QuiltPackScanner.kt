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
package de.griefed.serverpackcreator.api.modscanning

import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File

/**
 * The scanner for a Quilt pack, which is not one descriptor format but two: Quilt runs Fabric mods,
 * so a jar in a Quilt pack may carry a `quilt.mod.json`, a `fabric.mod.json`, or both.
 *
 * The directory is therefore scanned twice and the two verdicts merged per jar, with **CLIENT
 * winning**. That asymmetry is deliberate: a scan that could not read a jar falls back to SERVER, so
 * a SERVER verdict is only meaningful when it came from a descriptor the scanner actually read,
 * while a CLIENT verdict can only come from a descriptor that said so.
 *
 * The Quilt entry is the one kept whenever the two agree, so a mod carrying both descriptors is
 * reported with the id and dependencies Quilt declares.
 *
 * @param quiltScanner  Reads `quilt.mod.json`.
 * @param fabricScanner Reads `fabric.mod.json`.
 */
class QuiltPackScanner(
    private val quiltScanner: QuiltScanner,
    private val fabricScanner: FabricScanner
) : ModJarScanner {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

    override fun scan(jarFiles: Collection<File>): List<ScannedMod> {
        val quiltScan = quiltScanner.scan(jarFiles).toMutableList()
        val fabricScan = fabricScanner.scan(jarFiles)

        for (index in quiltScan.indices) {
            val fabricVerdict = fabricScan.find { it.file == quiltScan[index].file } ?: continue
            if (quiltScan[index].sideness == Sideness.SERVER && fabricVerdict.sideness == Sideness.CLIENT) {
                log.info(
                    "${fabricVerdict.file.name} Quilt-scan yielded sideness SERVER, but Fabric-scan " +
                            "yielded CLIENT. Using Fabric-scan result instead."
                )
                quiltScan[index] = fabricVerdict
            }
        }

        return quiltScan
    }
}
