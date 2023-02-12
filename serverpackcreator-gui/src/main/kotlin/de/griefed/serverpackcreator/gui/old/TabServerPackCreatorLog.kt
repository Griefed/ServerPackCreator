/* Copyright (C) 2023  Griefed
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
package de.griefed.serverpackcreator.gui.old

import de.griefed.serverpackcreator.gui.old.utilities.JComponentTailer
import org.apache.commons.io.input.Tailer
import org.apache.commons.io.input.TailerListener
import org.apache.commons.io.input.TailerListenerAdapter
import java.io.File

/**
 * This class creates the tab which display the latest serverpackcreator.log tailer.
 *
 * @author Griefed
 */
class TabServerPackCreatorLog(
    tooltip: String,
    logsDirectory: File
) : JComponentTailer(tooltip) {

    init {
        createTailer(logsDirectory)
    }

    /**
     * @author Griefed
     */
    override fun createTailer(logsDirectory: File) {
        class MyTailerListener : TailerListenerAdapter() {
            override fun handle(line: String) {
                if (line.contains("Checking entered configuration.")) {
                    textArea.text = ""
                }
                if (!line.contains("DEBUG")) {
                    textArea.append(
                        line.trimIndent() + "\n"
                    )
                }
            }
        }

        val tailerListener: TailerListener = MyTailerListener()
        val tailer = Tailer(
            File(logsDirectory, "serverpackcreator.log"), tailerListener,
            200
        )
        val thread = Thread(tailer)
        thread.isDaemon = true
        thread.start()
    }
}