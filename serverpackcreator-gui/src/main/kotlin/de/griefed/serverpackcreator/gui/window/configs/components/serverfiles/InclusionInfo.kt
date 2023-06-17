package de.griefed.serverpackcreator.gui.window.configs.components.serverfiles

import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.window.configs.components.StatusIcon

class InclusionInfo(guiProps: GuiProps) : StatusIcon(
    guiProps,
    "Regex-expression by which to determine files and directories to include from the specified source."
)