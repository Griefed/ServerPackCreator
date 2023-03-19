# Module serverpackcreator-gui

# Package de.serverpackcreator.gui

The Swing-GUI for ServerPackCreator. The GUI itself holds little to no actual ServerPackCreator
logic, but instead focuses on accessing the underlying logic to allow a user to configure and
generate a server pack. This package creates the complete GUI which can then be used by a given
user.

# Package de.serverpackcreator.gui.filebrowser

A custom filebrowser with the goal of quickly and easily changing key-values for a selected server
pack configuration. Originally by Andrew Thompson, [Stackexchange.com](https://codereview.stackexchange.com/questions/4446/file-browser-gui),
converted to Kotlin and edited/changed to fit the needs of ServerPackCreator.
