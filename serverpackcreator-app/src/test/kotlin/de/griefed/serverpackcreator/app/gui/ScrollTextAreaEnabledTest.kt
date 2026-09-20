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
package de.griefed.serverpackcreator.app.gui

import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.app.gui.components.ScrollTextArea
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File
import javax.swing.JScrollPane

/**
 * Disabling a settings widget has to reach the thing the user actually types into.
 *
 * Swing does not cascade `setEnabled` from a `JScrollPane` to its view, so a caller greying out a
 * [ScrollTextArea] used to grey out nothing at all: the text stayed bright and stayed editable. The
 * global settings panel relies on this to show that the protected-paths list is consulted only while
 * updating is enabled.
 */
internal class ScrollTextAreaEnabledTest {
    private val api = ApiWrapper.api(File("build/resources/test/serverpackcreator.properties"))

    /** The wrapped text area follows the scroll pane's enabled state, in both directions. */
    @Test
    fun disablingTheScrollPaneDisablesTheTextAreaInsideIt() {
        val area = ScrollTextArea("world, ops.json", "Protected From Updates", GuiProps(api.apiProperties))
        val inner = (area as JScrollPane).viewport.view

        Assertions.assertTrue(area.isEnabled, "A fresh widget is enabled")
        Assertions.assertTrue(inner.isEnabled, "and so is what the user types into")

        area.isEnabled = false
        Assertions.assertFalse(area.isEnabled, "Disabling reaches the scroll pane")
        Assertions.assertFalse(inner.isEnabled, "and the text area inside it, which is the visible part")

        area.isEnabled = true
        Assertions.assertTrue(inner.isEnabled, "Re-enabling reaches it again")
    }
}
