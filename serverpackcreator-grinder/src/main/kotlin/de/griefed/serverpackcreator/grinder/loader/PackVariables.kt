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

import java.io.File

/**
 * Sets the `variables.txt` levers an **unattended** server boot needs, plus the EULA — both confirmed
 * required by the loader-install spike against `default_template.sh`:
 *  - `eula.txt` = `eula=true` (else an interactive EULA prompt),
 *  - `WAIT_FOR_USER_INPUT=false` (else the script blocks on a `read`),
 *  - `JAVA` pointed at the bundled per-Minecraft JDK (so no Java download),
 *  - `SERVERSTARTERJAR_FORCE_FETCH=false` for **offline** (cached) boots, else Forge/NeoForge
 *    re-download `server.jar` — fatal under `--network none`. The install boot leaves it on,
 *  - `USE_SSJ=false` so **Forge** launches from the installer's own argfile (see [prepareUnattended]).
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
     *
     * [installerJavaPath], when given, is written as `JAVA_INSTALLER` — the JDK the templates use for
     * modloader *installers* that need a newer Java than the server. Without it, Quilt cannot install on
     * an older Minecraft (its installer requires Java 17+ while e.g. 1.16.1 runs on Java 8). Harmless for
     * every other loader: the templates fall back to `JAVA` and only Quilt's install consults it.
     *
     * **`USE_SSJ=false` is set on every pack, and it is only Forge that reads it.** The ServerStarterJar cannot
     * launch a Forge install whose module path it has to synthesise a boot layer for — Forge's
     * `SecureModuleClassLoader` resolves a read module's configuration against its *direct* parents only, so
     * `java.base` is not found and it throws `Could not find parent layer for module` before FML exists. That is
     * the incompatibility `HELP.md` records for Minecraft 1.20.2/1.20.3, and the knob is a pack author's escape
     * hatch — which an unattended grinder has no way to reach for, so it takes the hatch by default and every
     * Forge boot launches the way `run.sh` does. The trade-off worth stating: the grinder therefore exercises the
     * argfile path rather than the one a default user pack takes, and the starter-jar path is covered by
     * `ScriptTemplateMatrixIT` instead. NeoForge is unaffected — `setupNeoForge` never consults the knob.
     */
    fun prepareUnattended(packDir: File, javaPath: String, offline: Boolean, installerJavaPath: String? = null) {
        File(packDir, "eula.txt").writeText("eula=true\n")
        val variables = File(packDir, "variables.txt")
        if (!variables.isFile) {
            return
        }
        var text = variables.readText()
        text = set(text, "WAIT_FOR_USER_INPUT", "false")
        text = set(text, "JAVA", javaPath)
        // Both boots, install and mod: the install layer that gets cached has to be the one the offline boot
        // then launches from.
        text = set(text, "USE_SSJ", "false")
        if (installerJavaPath != null) {
            text = set(text, "JAVA_INSTALLER", installerJavaPath)
        }
        if (offline) {
            text = set(text, "SERVERSTARTERJAR_FORCE_FETCH", "false")
        }
        variables.writeText(text)
    }
}
