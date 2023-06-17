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
package de.griefed.serverpackcreator.gui.window.menu.about

import Gui
import de.griefed.serverpackcreator.api.utilities.common.WebUtilities
import java.net.URI
import javax.swing.JMenuItem

/**
 * Menu item for opening the How-To section in Griefed's official Wiki in the users browser.
 *
 * @author Griefed
 */
class WikiHowToItem(private val webUtilities: WebUtilities) :
    JMenuItem(Gui.menubar_gui_menuitem_wiki_howto.toString()) {
    private val howto = URI.create("https://wiki.griefed.de/en/Documentation/ServerPackCreator/ServerPackCreator-HowTo")

    init {
        this.addActionListener { openHowTo() }
    }

    /**
     * @author Griefed
     */
    private fun openHowTo() {
        webUtilities.openLinkInBrowser(howto)
    }
}