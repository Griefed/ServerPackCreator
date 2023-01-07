# Module serverpackcreator-gui

# Package de.serverpackcreator.gui

The Swing-GUI for ServerPackCreator. The GUI itself holds little to no actual ServerPackCreator
logic, but instead focuses on accessing the underlying logic to allow a user to configure and
generate a server pack. This package creates the complete GUI which can then be used by a given
user. Entrypoint is [de.griefed.serverpackcreator.swing.ServerPackCreatorWindow.mainGUI].

# Package de.serverpackcreator.gui.utilities

Utilities for creating our GUI. Provides classes for creating out scroll panes for displaying
logging, a background panel which repeats a given image in both x and y directions, and the base
class from wich a log tailer can be created.

# Package de.serverpackcreator.gui.themes

ServerPackCreator GUI themes. Mainly light and dark theme. Nothing fancy going on here.