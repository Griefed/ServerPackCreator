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
 * Sets the `variables.txt` levers an **unattended** server boot needs, plus the EULA — both confirmed
 * required by the loader-install spike against `default_template.sh`:
 *  - `eula.txt` = `eula=true` (else an interactive EULA prompt),
 *  - `WAIT_FOR_USER_INPUT=false` (else the script blocks on a `read`),
 *  - `JAVA` pointed at the bundled per-Minecraft JDK (so no Java download),
 *  - `SERVERSTARTERJAR_FORCE_FETCH=false` for **offline** (cached) boots, else Forge/NeoForge
 *    re-download `server.jar` — fatal under `--network none`. The install boot leaves it on.
 *
 * @author Griefed
 */
object PackVariables {

    /** Set (or append) `key=value` in [content], line-anchored and idempotent. */
    fun set(content: String, key: String, value: String): String {
        val line = Regex("(?m)^${Regex.escape(key)}=.*$")
        return if (line.containsMatchIn(content)) {
            line.replace(content) { "$key=$value" }
        } else {
            content.trimEnd('\n') + "\n$key=$value\n"
        }
    }

    /**
     * Apply the unattended-boot levers to the pack at [packDir], pointing `$JAVA` at the already
     * resolved [javaPath] (the caller picks the bundled JDK via [ImageJavaRuntimes]). Pass
     * [offline] = `true` for a cached `--network none` boot (disables the ServerStarterJar re-fetch),
     * `false` for the one-off install boot that still needs network.
     */
    fun prepareUnattended(packDir: File, javaPath: String, offline: Boolean) {
        File(packDir, "eula.txt").writeText("eula=true\n")
        val variables = File(packDir, "variables.txt")
        if (!variables.isFile) {
            return
        }
        var text = variables.readText()
        text = set(text, "WAIT_FOR_USER_INPUT", "false")
        text = set(text, "JAVA", javaPath)
        if (offline) {
            text = set(text, "SERVERSTARTERJAR_FORCE_FETCH", "false")
        }
        variables.writeText(text)
    }
}
