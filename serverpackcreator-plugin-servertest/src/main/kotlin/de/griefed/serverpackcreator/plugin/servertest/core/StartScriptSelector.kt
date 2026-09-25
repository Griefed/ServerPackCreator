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
 * The two families of host this plugin launches on, which is as fine a distinction as the choice needs.
 *
 * A pack ships a script per shell, but only the shell family decides the argv: everything that is not
 * Windows runs the same `bash start.sh`, because bash is present on every Linux and macOS installation.
 */
enum class Platform {
    /** Windows, where the pack is launched through its batch shim rather than a shell. */
    WINDOWS,

    /** Linux and macOS — anything that has bash, which is the launch vehicle for both. */
    POSIX;

    companion object {
        /**
         * The family [osName] belongs to. Defaults to the running host, and takes the name as an argument so
         * the selector's rules can be exercised for both families on either one.
         */
        fun of(osName: String = System.getProperty("os.name") ?: ""): Platform =
            if (osName.lowercase().contains("win")) WINDOWS else POSIX
    }
}

/**
 * Whether a pack can be launched on a given platform, and with what.
 *
 * A sealed pair rather than a nullable script plus a nullable reason: exactly one of the two is always
 * meaningful, and a type that says so cannot be read the wrong way round.
 */
sealed interface StartScriptSelection {

    /** The pack has a script for this platform. [command] is the argv, relative to the pack directory. */
    data class Available(
        /** The script that will be run, as an absolute file — for existence checks and for the UI to name. */
        val script: File,
        /** The argv to spawn, with the script named relatively so the working directory decides which pack. */
        val command: List<String>
    ) : StartScriptSelection

    /** The pack has no script this platform can run. [reason] names the file that was wanted. */
    data class Missing(
        /** Why nothing can be launched, in words the pack list can show a user. */
        val reason: String
    ) : StartScriptSelection
}

/**
 * Picks the script a generated server pack is launched with, and the argv to launch it.
 *
 * The answers come from the pack's own `HOW-TO-RUN.md` rather than from taste: `bash start.sh` on Linux and
 * macOS, `start.bat` on Windows — the shipped shim whose only job is to run `start.ps1` without the user
 * changing their ExecutionPolicy.
 *
 * @author Griefed
 */
object StartScriptSelector {

    /**
     * The script and argv for [packDirectory] on [platform], or why there is none.
     *
     * Checks the file is a regular file rather than merely present, so a directory that happens to be named
     * `start.sh` is reported as missing instead of failing at spawn time with a shell error.
     */
    fun selectFor(packDirectory: File, platform: Platform = Platform.of()): StartScriptSelection =
        StartScriptSelection.Missing("Start-script selection is not implemented yet.")
}
