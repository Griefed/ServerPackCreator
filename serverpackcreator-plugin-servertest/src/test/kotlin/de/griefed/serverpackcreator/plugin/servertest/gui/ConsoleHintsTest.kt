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
import de.griefed.serverpackcreator.plugin.servertest.core.ServerSession
import org.junit.jupiter.api.io.TempDir
import java.io.File
import javax.swing.JTextArea
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Pins the one note the console adds while a session is running.
 *
 * It exists because of something observed rather than imagined: a real NeoForge boot on 2026-09-25 reached
 * ready, took `stop`, saved its worlds, printed `Exiting...` — and then went completely silent for three
 * minutes until the run was abandoned. The script was blocked on
 * `read -n 1 -s -r -p "Press any key to continue"`, and bash writes a `read -p` prompt **only when standard
 * input is a terminal**. Over this plugin's pipe there is no prompt at all, so a finished server is
 * indistinguishable from a hung one.
 */
internal class ConsoleHintsTest {

    private val waiting = PackVariables(restartsAutomatically = false, waitsForUserInput = true)
    private val notWaiting = PackVariables(restartsAutomatically = false, waitsForUserInput = false)

    /** The exit line from a pack that waits is what the hint is for. */
    @Test
    fun hintsWhenAWaitingPackReachesItsInvisiblePrompt() {
        Assertions.assertEquals(ConsoleHints.PRESS_ENTER_HINT, ConsoleHints.after(ConsoleHints.EXIT_LINE, waiting))
    }

    /** Leading or trailing whitespace on the line must not lose the hint. */
    @Test
    fun toleratesSurroundingWhitespaceOnTheExitLine() {
        Assertions.assertEquals(ConsoleHints.PRESS_ENTER_HINT, ConsoleHints.after("  Exiting...  ", waiting))
    }

    /**
     * A pack that does not wait never gets the hint, even on the same line — the script exits straight away
     * there, and telling the user to press Enter would be advice for a prompt that never comes.
     */
    @Test
    fun staysSilentForAPackThatDoesNotWait() {
        Assertions.assertNull(ConsoleHints.after(ConsoleHints.EXIT_LINE, notWaiting))
    }

    /** Ordinary output gets no note, including a line that merely contains the word. */
    @Test
    fun staysSilentForEveryOtherLine() {
        Assertions.assertNull(ConsoleHints.after("Done (5.862s)! For help, type \"help\"", waiting))
        Assertions.assertNull(ConsoleHints.after("[Server thread/INFO]: Exiting... the nether portal", waiting))
        Assertions.assertNull(ConsoleHints.after("", waiting))
    }

    /**
     * The console wraps its lines.
     *
     * Two properties, pinned because the defect they fix was real and invisible to every other guard: the
     * notes this pane writes are prose, and at the pane's width one of them ended mid-sentence behind a
     * horizontal scrollbar nobody would think to drag. Found by rendering the pane and looking at it.
     */
    @Test
    fun theConsoleWrapsSoLongNotesStayReadable(@TempDir packDir: File) {
        val pane = ConsolePane(
            session = ServerSession(packDir, listOf("true"), {}, {}, {}),
            port = 25565,
            variables = waiting,
            scrollback = 100
        )

        val console = findTextArea(pane)
        Assertions.assertNotNull(console, "The pane must contain the console text area.")
        Assertions.assertTrue(console!!.lineWrap, "Long notes are cut off without wrapping.")
        Assertions.assertTrue(console.wrapStyleWord, "Wrapping mid-word makes a path or a mod name unreadable.")
    }

    private fun findTextArea(container: java.awt.Container): JTextArea? {
        for (child in container.components) {
            if (child is JTextArea) return child
            if (child is java.awt.Container) findTextArea(child)?.let { return it }
        }
        return null
    }
}
