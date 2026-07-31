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
package de.griefed.serverpackcreator.api.utilities

import me.cortex.jarscanner.Main
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.nio.file.Path

/**
 * Various methods to perform security-related scans, such as Nekodetector.
 *
 * @author Griefed
 */
class SecurityScans {

    /** Zip-slip and archive-safety checks applied before an upload is trusted. */

    companion object {
        /** Logger for rejected archives, so a refused upload leaves a trace of *why*. */
        val log by lazy { cachedLoggerOf(SecurityScans::class.java) }

        /**
         * Uses MCRcortex's nekodetector to detect files infected by the fractureiser malware.
         * The code can be found at [MCRcortex/nekodetector](https://github.com/MCRcortex/nekodetector)
         *
         * Initially provided via a plugin, available at [Griefed/spc-nekodetector-plugin](https://github.com/Griefed/spc-nekodetector-plugin)
         * @author Griefed
         */
        fun scanUsingNekodetector(destination: Path) : List<String> {
            val results = mutableListOf<String>()
            try {
                log.info("Scanning $destination for infections using Nekodetector...")
                val output: (String) -> String = { "" }
                val run = Main.run(Runtime.getRuntime().availableProcessors(), destination, true, output)
                if (run.stage1Detections.isNotEmpty() || run.stage2Detections.isNotEmpty()) {
                    results.add("Nekodetector infections found! Remove these mods, perform a virus-scan and report the mods on the platform you got them from!")
                }
                if (run.stage1Detections.isNotEmpty()) {
                    results.add("Stage 1 infections:")
                    for (entry in run.stage1Detections) {
                        results.add(entry)
                    }
                }
                if (run.stage2Detections.isNotEmpty()) {
                    results.add("Stage 2 infections:")
                    for (entry in run.stage2Detections) {
                        results.add(entry)
                    }
                }
            } catch (ex: Exception) {
                log.error("Error during Nekodetector scan.", ex)
            }
            return results
        }
    }
}