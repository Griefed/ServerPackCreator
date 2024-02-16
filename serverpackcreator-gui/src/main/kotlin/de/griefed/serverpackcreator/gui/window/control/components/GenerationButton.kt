/* Copyright (C) 2024  Griefed
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
package de.griefed.serverpackcreator.gui.window.control.components

import Translations
import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.components.BalloonTipButton
import java.awt.event.ActionListener

/**
 * Generation-Button which, upon pressing it, generates a server pack with the config acquired from the currently active
 * tab in the config-tabs tab.
 *
 * @author Griefed
 */
class GenerationButton(guiProps: GuiProps, generationAction: ActionListener) : BalloonTipButton(
    Translations.createserverpack_gui_buttongenerateserverpack.toString(),
    guiProps.genIcon,
    Translations.createserverpack_gui_buttongenerateserverpack_tip.toString(),
    guiProps,
    generationAction
)