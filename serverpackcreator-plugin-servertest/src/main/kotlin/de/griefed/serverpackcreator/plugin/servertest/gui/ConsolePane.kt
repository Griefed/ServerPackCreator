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
import de.griefed.serverpackcreator.plugin.servertest.core.SessionState
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.SwingUtilities

/**
 * One running server: its console, a line into its standard input, and the address to connect a client to.
 *
 * The input field is the feature, not a convenience. `start.sh` blocks on `read` three times before a server
 * even exists — a spaces-in-path confirmation, Mojang's EULA and a "press any key" on exit — and the server
 * reads its own commands from the same pipe afterwards. Typing `I agree` and later `stop` both go through
 * here.
 *
 * Every callback from [ServerSession] arrives on its reader thread, so everything that touches Swing is
 * marshalled with [SwingUtilities.invokeLater]. ServerPackCreator's own `LogTailer` appends from the tailer's
 * thread instead; that is not a pattern to copy.
 *
 * @param session    The server this pane drives.
 * @param port       The port the server was given, shown as the address to connect to.
 * @param variables  The pack's `RESTART` / `WAIT_FOR_USER_INPUT` settings, warned about up front.
 * @param scrollback How many console lines to keep on screen.
 * @author Griefed
 */
class ConsolePane(
    private val session: ServerSession,
    port: Int,
    private val variables: PackVariables,
    private val scrollback: Int
) : JPanel(BorderLayout()) {

    /**
     * The console. Plain text, never markup — a `JTextArea` parses no HTML, unlike a label.
     *
     * Wrapped, unlike ServerPackCreator's own log panes, which scroll horizontally instead. Two reasons this
     * surface differs: the notes this pane writes are prose and were being cut off mid-sentence at the pane's
     * width, and a crash report is the thing a user comes here to read — hunting for a horizontal scrollbar
     * to finish a stack-trace line is worse than a wrapped one. Wrapped on word boundaries so a path or a mod
     * name stays readable across the break.
     */
    private val console = JTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
    }

    private val consoleScroller = JScrollPane(console)

    /** The line the user types into the server's standard input. */
    private val input = JTextField()

    /** What the session is doing, in one phrase. */
    private val status = PlainTextRendering.label("Starting…")

    /** Asks the server to shut down the way an operator would. */
    private val stopButton = JButton("Stop (send \"stop\")")

    /** Force-kills the server and everything it started, for one that will not stop. */
    private val killButton = JButton("Force stop")

    /** Where a client connects. Held so it can be copied without retyping. */
    private val connectAddress = "localhost:$port"

    init {
        add(header(), BorderLayout.NORTH)
        add(consoleScroller, BorderLayout.CENTER)
        add(inputRow(), BorderLayout.SOUTH)

        input.addActionListener { sendTypedLine() }
        stopButton.addActionListener { session.stop() }
        killButton.addActionListener { session.kill() }

        for (note in openingNotes()) {
            append(note)
        }
    }

    /**
     * The notes shown before the first line of output.
     *
     * Every one of them describes something that otherwise reads as the plugin being broken: a prompt the
     * user must answer, a stop that does not stop, or a script that goes silent and waits.
     */
    private fun openingNotes(): List<String> = buildList {
        add("[ServerPackCreator] Connect a client to $connectAddress once the server reports it is ready.")
        add(
            "[ServerPackCreator] Mojang's EULA is not accepted for you. If this pack has not been started " +
                    "before, the script will ask — type  I agree  below and press Enter. You will be asked " +
                    "again for every pack that has no eula.txt yet."
        )
        if (variables.waitsForUserInput) {
            add(
                "[ServerPackCreator] This pack has WAIT_FOR_USER_INPUT=true, so the script ends by waiting " +
                        "for a keypress. It prints no prompt when it is not attached to a terminal, so if it " +
                        "goes quiet after \"Exiting...\", press Enter below to finish it."
            )
        }
        if (variables.restartsAutomatically) {
            add(
                "[ServerPackCreator] This pack has RESTART=true, so the script relaunches the server five " +
                        "seconds after it stops. Use Force stop to end the session itself."
            )
        }
        add("")
    }

    /** The connection address, the session's state, and the two stop controls. */
    private fun header(): JPanel = JPanel(BorderLayout()).apply {
        border = BorderFactory.createEmptyBorder(4, 4, 4, 4)

        add(
            JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                add(PlainTextRendering.label("Connect to:  $connectAddress"))
                add(status)
            },
            BorderLayout.WEST
        )
        add(
            JPanel(FlowLayout(FlowLayout.RIGHT)).apply {
                add(JButton("Copy address").apply { addActionListener { copyAddress() } })
                add(stopButton)
                add(killButton)
            },
            BorderLayout.EAST
        )
    }

    /** The standard-input line, labelled so it is obvious it talks to the server rather than filtering. */
    private fun inputRow(): JPanel = JPanel(BorderLayout()).apply {
        border = BorderFactory.createEmptyBorder(0, 4, 4, 4)
        add(PlainTextRendering.label("Send to server:  "), BorderLayout.WEST)
        add(input, BorderLayout.CENTER)
        add(JButton("Send").apply { addActionListener { sendTypedLine() } }, BorderLayout.EAST)
    }

    /** Put the connection address on the clipboard, so it can be pasted into a client's server list. */
    private fun copyAddress() {
        runCatching {
            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(connectAddress), null)
        }
    }

    /**
     * Send what is typed, echoing it into the console.
     *
     * Echoed because the server does not: a bare `stop` typed into a field that then clears itself gives no
     * evidence anything was sent. An empty line is still sent — `read -n 1` wants exactly that.
     */
    private fun sendTypedLine() {
        val line = input.text
        input.text = ""
        if (session.send(line)) {
            append("> $line")
        } else {
            append("[ServerPackCreator] Not sent — this server is no longer running.")
        }
    }

    /**
     * Append [line] to the console, plus any note it calls for, from any thread.
     *
     * The note is appended here rather than by the caller so the two always arrive together and in order.
     */
    fun appendLine(line: String) {
        append(line)
        ConsoleHints.after(line, variables)?.let(::append)
    }

    /** Put one line on screen, trimming to [scrollback] and following the tail. */
    private fun append(line: String) {
        SwingUtilities.invokeLater {
            val scrollBar = consoleScroller.verticalScrollBar
            // Decided BEFORE the append: afterwards the maximum has already grown and every position looks
            // scrolled-up, which would stop the console following the tail from its very first line.
            val wasAtBottom = scrollBar.value + scrollBar.visibleAmount >= scrollBar.maximum - AT_BOTTOM_SLACK

            console.append(line + "\n")
            trimToScrollback()

            if (wasAtBottom) {
                console.caretPosition = console.document.length
            }
        }
    }

    /** Drop the oldest lines once the console is longer than [scrollback], so a long run stays bounded. */
    private fun trimToScrollback() {
        val root = console.document.defaultRootElement
        val excess = root.elementCount - scrollback
        if (excess > 0) {
            runCatching { console.document.remove(0, root.getElement(excess - 1).endOffset) }
        }
    }

    /** Reflect a session state change in the header and the controls, from any thread. */
    fun showState(state: SessionState) {
        SwingUtilities.invokeLater {
            status.text = when (state) {
                SessionState.Starting -> "Starting — installing the modloader and booting."
                SessionState.Ready -> "Ready — connect a client to $connectAddress."
                SessionState.Stopping -> "Stopping…"
                is SessionState.Exited -> "Stopped. The script exited with ${state.exitCode ?: "an unknown status"}."
            }
            val alive = state !is SessionState.Exited
            stopButton.isEnabled = alive
            killButton.isEnabled = alive
            input.isEnabled = alive
        }
    }

    companion object {
        /**
         * How many pixels short of the bottom still counts as "at the bottom".
         *
         * Without slack the console stops following as soon as a partial line leaves the scrollbar a pixel
         * off its maximum, which reads as the server having gone quiet.
         */
        const val AT_BOTTOM_SLACK = 16
    }
}
