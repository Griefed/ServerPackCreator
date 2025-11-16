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
 * full license can be found at https:github.com/Griefed/ServerPackCreator/blob/main/LICENSE
 */
package de.griefed.serverpackcreator.app.gui.window.logs

import de.griefed.serverpackcreator.app.gui.window.logs.components.ScrollDirection
import de.griefed.serverpackcreator.app.gui.window.logs.components.SmartScroller
import de.griefed.serverpackcreator.app.gui.window.logs.components.ViewPortPosition
import org.apache.commons.io.input.Tailer
import org.apache.commons.io.input.TailerListenerAdapter
import java.io.File
import java.time.Duration
import javax.swing.JScrollPane
import javax.swing.JTextArea

/**
 * Log tailer which writes changes to a scrollable panel.
 *
 * @author Griefed
 */
abstract class LogTailer : JScrollPane() {
    protected var textArea: JTextArea = JTextArea()
    private var counter = 0

    init {
        textArea.isEditable = false
        viewport.view = textArea
        horizontalScrollBarPolicy = HORIZONTAL_SCROLLBAR_ALWAYS
        verticalScrollBarPolicy = VERTICAL_SCROLLBAR_ALWAYS
        SmartScroller(this, ScrollDirection.VERTICAL, ViewPortPosition.END)
    }

    /**
     * Create a tailer for the specified [logFile] to keep an eye on said file.
     *
     * @author Griefed
     */
    fun createTailer(logFile: File) {
        val listener = object : TailerListenerAdapter() {
            override fun handle(line: String) {
                textArea.append("$line\n")
                if (counter >= 2000) {
                    textArea.document.remove(
                        0,
                        textArea.document.defaultRootElement.getElement(0).endOffset
                    )
                } else {
                    counter++
                }
            }
        }
        val builder = Tailer.builder()
        builder.file = logFile
        builder.setTailerListener(listener)
        builder.setDelayDuration(Duration.ofMillis(250L))
        builder.setStartThread(true)
        builder.get()
    }
}