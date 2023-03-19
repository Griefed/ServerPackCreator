package de.griefed.serverpackcreator.gui.window.control.components

import Gui
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
    Gui.createserverpack_gui_buttongenerateserverpack.toString(),
    guiProps.genIcon,
    Gui.createserverpack_gui_buttongenerateserverpack_tip.toString(),
    guiProps,
    generationAction
)