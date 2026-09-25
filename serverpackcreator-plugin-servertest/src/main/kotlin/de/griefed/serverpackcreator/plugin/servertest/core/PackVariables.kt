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
 * The two `variables.txt` settings that change what a user sees on the console, read without interpreting
 * the rest of the file.
 *
 * Neither changes how the pack is launched — they change what the *console* does, in ways that look like a
 * hang if nobody says so beforehand. Read line-anchored rather than parsed: `variables.txt` is sourced by
 * bash and parsed by hand in PowerShell, and this needs two values out of it, not a model of it.
 *
 * @author Griefed
 */
data class PackVariables(
    /**
     * `RESTART=true`, which wraps the server in a loop inside the script. A `stop` then returns the user to
     * the script's own five-second countdown rather than ending the session.
     */
    val restartsAutomatically: Boolean,

    /**
     * `WAIT_FOR_USER_INPUT=true`, which ends the script on `read -n 1 -s -r -p "Press any key to continue"`.
     *
     * The trap worth warning about: **bash writes a `-p` prompt only when standard input is a terminal.**
     * Over the pipe this plugin uses, the script produces no output at all at that point and looks hung, so
     * the console pane has to say that pressing Enter will finish it.
     */
    val waitsForUserInput: Boolean
) {

    companion object {
        /** The file these are read from, written into every generated pack beside its start scripts. */
        const val FILE_NAME = "variables.txt"

        /** Loops the server inside the script instead of exiting when it stops. */
        const val RESTART_KEY = "RESTART"

        /** Ends the script on a keypress the user cannot see the prompt for. */
        const val WAIT_FOR_USER_INPUT_KEY = "WAIT_FOR_USER_INPUT"

        /**
         * Read [packDirectory]'s variables. A pack with no readable `variables.txt` reports neither setting:
         * its start script will refuse to run for that reason anyway, and inventing a warning about a file
         * that is not there would only obscure the real message.
         */
        fun read(packDirectory: File): PackVariables {
            val contents = runCatching { File(packDirectory, FILE_NAME).readText() }.getOrNull().orEmpty()
            return PackVariables(
                restartsAutomatically = isTrue(contents, RESTART_KEY),
                waitsForUserInput = isTrue(contents, WAIT_FOR_USER_INPUT_KEY)
            )
        }

        /** Whether [key] is assigned a value meaning true, ignoring surrounding quotes and case. */
        private fun isTrue(contents: String, key: String): Boolean =
            Regex("^[ \\t]*${Regex.escape(key)}[ \\t]*=(.*)$", RegexOption.MULTILINE)
                .findAll(contents)
                .lastOrNull()
                ?.groupValues
                ?.get(1)
                ?.trim()
                ?.trim('"', '\'')
                .equals("true", ignoreCase = true)
    }
}
