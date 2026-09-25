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
package de.griefed.serverpackcreator.plugin.servertest.gui

import de.griefed.serverpackcreator.plugin.servertest.core.PackVariables

/**
 * Notes the console adds of its own accord, at the moment they are needed.
 *
 * Separate from [ConsolePane] because this is the part worth pinning: *when* a hint fires is a decision, and
 * the pane around it is rendering.
 *
 * @author Griefed
 */
object ConsoleHints {

    /**
     * What the script prints immediately before it blocks on `read -n 1 -s -r -p "Press any key to continue"`.
     *
     * Verified against a real NeoForge boot on 2026-09-25: the server stopped cleanly, the script printed
     * this line, and then produced **nothing further** while waiting for a keypress — because bash writes a
     * `read -p` prompt only when standard input is a terminal, and this plugin gives it a pipe. Without a
     * hint at that moment the session is indistinguishable from a hang.
     */
    const val EXIT_LINE = "Exiting..."

    /** The hint shown when the script is waiting on that invisible prompt. */
    const val PRESS_ENTER_HINT =
        "[ServerPackCreator] The script is now waiting for a keypress it cannot show a prompt for " +
                "(WAIT_FOR_USER_INPUT=true). Press Enter in the box below to finish it."

    /**
     * The note to append after [line], or `null` when none is needed.
     *
     * Gated on the pack's own `WAIT_FOR_USER_INPUT`, so a pack that does not wait never sees the hint even
     * if something else prints the same words.
     */
    fun after(line: String, variables: PackVariables): String? =
        if (variables.waitsForUserInput && line.trim() == EXIT_LINE) PRESS_ENTER_HINT else null
}
