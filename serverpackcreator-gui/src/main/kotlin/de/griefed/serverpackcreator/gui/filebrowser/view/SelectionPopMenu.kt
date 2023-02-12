package de.griefed.serverpackcreator.gui.filebrowser.view

import de.griefed.serverpackcreator.gui.filebrowser.controller.action.ModpackDirectoryAction
import de.griefed.serverpackcreator.gui.filebrowser.controller.action.OpenAction
import de.griefed.serverpackcreator.gui.filebrowser.controller.action.ServerIconAction
import de.griefed.serverpackcreator.gui.filebrowser.controller.action.ServerPropertiesAction
import de.griefed.serverpackcreator.gui.window.configs.ConfigsTab
import java.awt.Component
import java.awt.event.MouseAdapter
import java.io.File
import javax.swing.JPopupMenu
import javax.swing.JSeparator

open class SelectionPopMenu(configsTab: ConfigsTab) : MouseAdapter() {
    private val menu: JPopupMenu = JPopupMenu()
    private val directory = ModpackDirectoryAction(configsTab)
    private val icon = ServerIconAction(configsTab)
    private val properties = ServerPropertiesAction(configsTab)
    private val open = OpenAction()
    private val imageRegex: Regex = ".*\\.(png|jpg|jpeg|bmp)".toRegex()
    private val props: String = "properties"

    init {
        menu.add(open)
        menu.add(JSeparator())
        menu.add(directory)
        menu.add(icon)
        menu.add(properties)
    }

    fun show(invoker: Component?, x: Int, y: Int, file: File) {
        setVisibilities(file)
        menu.show(invoker, x, y)
    }

    private fun setVisibilities(file: File) {
        open.setFile(file)
        if (file.isDirectory) {
            directory.setDirectory(file)
            directory.isEnabled = true
            icon.isEnabled = false
            properties.isEnabled = false
            return
        }
        if (file.isFile && file.name.lowercase().matches(imageRegex)) {
            icon.setIcon(file)
            directory.isEnabled = false
            icon.isEnabled = true
            properties.isEnabled = false
            return
        }
        if (file.isFile && file.name.lowercase().endsWith(props)) {
            properties.setProperties(file)
            directory.isEnabled = false
            icon.isEnabled = false
            properties.isEnabled = true
        }
    }
}