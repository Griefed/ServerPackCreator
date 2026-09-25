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
package de.griefed.serverpackcreator.plugin.servertest.core

import java.io.File

/**
 * The two families of host this plugin runs on, used only to pick a sensible **default** script.
 *
 * It decides nothing else. Which script actually runs is the user's choice, because the plugin guessing
 * wrong is the one failure that leaves somebody unable to start a pack at all.
 */
enum class Platform {
    /** Windows, where a pack is launched through its batch shim by default. */
    WINDOWS,

    /** Linux and macOS — anything that is not Windows, where `bash start.sh` is the default. */
    POSIX;

    companion object {
        /**
         * The family [osName] belongs to. Defaults to the running host, and takes the name as an argument so
         * both families can be exercised on either one.
         */
        fun of(osName: String = System.getProperty("os.name") ?: ""): Platform =
            if (osName.lowercase().contains("win")) WINDOWS else POSIX
    }
}

/**
 * One of the four start scripts ServerPackCreator writes into every server pack, and how to run it.
 *
 * Every pack ships all four, so which to use is a question about the *host* — and one the user answers,
 * not the plugin. Offering all four is the point: a guess that lands on a script the user's machine cannot
 * run leaves them unable to start anything, with no way to say otherwise.
 *
 * @author Griefed
 */
enum class StartScriptKind(
    /** The file inside the server pack, exactly as ServerPackCreator names it. */
    val fileName: String,

    /** What the dropdown shows: the file, plus who it is for. */
    val label: String,

    /** The argv to spawn. The script is named relatively, so the working directory decides which pack. */
    val command: List<String>
) {
    /** The bash script, and the default everywhere that is not Windows. */
    SH("start.sh", "start.sh — Linux / macOS (bash)", listOf("bash", "start.sh")),

    /**
     * The Windows batch shim, and the default there. Its entire job is to run `start.ps1` without the user
     * having to change their execution policy, which is why the pack's own `HOW-TO-RUN.md` prefers it.
     */
    BAT("start.bat", "start.bat — Windows (recommended)", listOf("cmd", "/c", "start.bat")),

    /**
     * PowerShell directly, for a Windows user who would rather skip the shim — or whose pack was generated
     * without one. `-NoProfile` because a profile that writes to the console would land in the server log,
     * and `-ExecutionPolicy Bypass` because that is the restriction the shim exists to work around.
     */
    PS1(
        "start.ps1",
        "start.ps1 — Windows (PowerShell directly)",
        listOf("powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", "start.ps1")
    ),

    /** The fish script, for users of that shell. Never a default: fish is a deliberate install, bash is not. */
    FISH("start.fish", "start.fish — fish shell", listOf("fish", "start.fish"));

    companion object {
        /**
         * The script pre-selected for [platform]: the batch shim on Windows, bash everywhere else.
         *
         * A *default*, not a decision — the user may pick any of the four, including one this host has no
         * interpreter for. That launch fails on the console with the interpreter's own error, which is a
         * better outcome than a plugin that quietly refuses to offer the script somebody needs.
         */
        fun defaultFor(platform: Platform = Platform.of()): StartScriptKind = when (platform) {
            Platform.WINDOWS -> BAT
            Platform.POSIX -> SH
        }
    }
}

/**
 * Whether the chosen script is actually in the pack, and what to run if it is.
 *
 * A sealed pair rather than a nullable script plus a nullable reason: exactly one of the two is always
 * meaningful, and a type that says so cannot be read the wrong way round.
 */
sealed interface StartScriptSelection {

    /** The pack has the chosen script. [command] is the argv, relative to the pack directory. */
    data class Available(
        /** The script that will be run, as an absolute file — for the UI to name and for a final check. */
        val script: File,
        /** The argv to spawn, with the script named relatively so the working directory decides which pack. */
        val command: List<String>
    ) : StartScriptSelection

    /** The pack does not carry the chosen script. [reason] names the file that is missing. */
    data class Missing(
        /** Why nothing can be launched, in words the pack list can show a user. */
        val reason: String
    ) : StartScriptSelection
}

/**
 * Answers whether a given pack can be launched with a given script.
 *
 * Pure: the scripts a pack carries are read once, when the pack is discovered, so changing the dropdown
 * re-decides every row without touching the disk again.
 *
 * @author Griefed
 */
object StartScriptSelector {

    /**
     * Whether [pack] can be launched with [kind], and with what.
     *
     * Keyed on what the catalog found when it read the directory rather than on a fresh check, so every row
     * in the list answers from the same reading.
     */
    fun selectFor(pack: LaunchablePack, kind: StartScriptKind): StartScriptSelection =
        if (kind in pack.scriptsPresent) {
            StartScriptSelection.Available(File(pack.directory, kind.fileName), kind.command)
        } else {
            StartScriptSelection.Missing(
                "This server pack has no ${kind.fileName}. Pick another start script, or regenerate the " +
                        "pack with that template enabled."
            )
        }

    /**
     * Which of the four scripts [packDirectory] actually carries.
     *
     * `isFile` rather than `exists`, so a directory that happens to be named `start.sh` is reported absent
     * instead of failing at spawn time with a shell error.
     */
    fun scriptsIn(packDirectory: File): Set<StartScriptKind> =
        StartScriptKind.entries.filterTo(mutableSetOf()) { File(packDirectory, it.fileName).isFile }
}
