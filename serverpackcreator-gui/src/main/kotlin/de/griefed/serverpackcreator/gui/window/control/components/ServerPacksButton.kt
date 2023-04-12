package de.griefed.serverpackcreator.gui.window.control.components

import Gui
import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.components.BalloonTipButton
import java.awt.event.ActionListener

/**
 * Button which opens the directory containing generated server packs in the users file-explorer.
 *
 * @author Griefed
 */
class ServerPacksButton(guiProps: GuiProps, openPacksAction: ActionListener) : BalloonTipButton(
    Gui.createserverpack_gui_buttonserverpacks.toString(),
    guiProps.packsIcon,
    Gui.createserverpack_gui_buttonserverpacks_tip.toString(),
    guiProps,
    openPacksAction
)