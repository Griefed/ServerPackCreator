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
package de.griefed.serverpackcreator.app.gui.tips

import Translations
import de.griefed.serverpackcreator.app.gui.GuiProps
import de.griefed.serverpackcreator.app.gui.utilities.getAspectRatioScaledInstance
import io.ktor.util.reflect.*
import tokyo.northside.swing.TipOfTheDay
import tokyo.northside.swing.plaf.DefaultTipOfTheDayUI
import java.awt.*
import java.awt.event.*
import java.util.*
import javax.swing.*
import javax.swing.plaf.BorderUIResource
import javax.swing.plaf.FontUIResource
import javax.swing.plaf.basic.BasicHTML

/**
 * Custom UI for the tip of the day dialog to assure it always resembles the current theme.
 *
 * @author Griefed
 */
class CustomTipOfTheDayUI(tipOfTheDay: TipOfTheDay, private val guiProps: GuiProps) :
    DefaultTipOfTheDayUI(tipOfTheDay) {

    private val tipImage = JLabel()
    private val preferredDimension = Dimension(650,350)
    private val defaultSize = Dimension(650,350)

    override fun installDefaults(defaults: UIDefaults) {
        super.installDefaults(defaults)
        tipPane.background = UIManager.getColor("Panel.background")
        tipPane.foreground = UIManager.getColor("Panel.foreground")
        tipPane.font = guiProps.font
        tipFont = guiProps.font.deriveFont(Font.BOLD)
        UIManager.put("TipOfTheDay.dialogTitle", Translations.tips_title.toString())
        UIManager.put("TipOfTheDay.showOnStartupText", Translations.tips_show.toString())
    }

    override fun installComponents(defaults: UIDefaults) {
        super.installComponents()
        val tipIcon = JLabel(Translations.tips_know.toString())
        tipIcon.icon = guiProps.fancyInfoIcon
        tipIcon.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
        tipIcon.font = guiProps.font.deriveFont(Font.BOLD, (guiProps.fontSize + 5).toFloat())
        tipPane.add("North", tipIcon)
    }

    override fun addUIDefaults(defaults: UIDefaults) {
        super.addUIDefaults(defaults)
        val font = guiProps.font.deriveFont(Font.BOLD, (guiProps.fontSize + 3).toFloat())
        val tipOfTheDayFont = guiProps.font
        defaults["TipOfTheDay.font"] = tipOfTheDayFont
        defaults["TipOfTheDay.tipFont"] = FontUIResource(font)
        defaults["TipOfTheDay.background"] = UIManager.getColor("Panel.background")
        defaults["TipOfTheDay.border"] =
            BorderUIResource(BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor")))
    }

    /**
     * Customized dialog based on [tokyo.northside.swing.plaf.BasicTipOfTheDayUI.createDialog]
     *
     * @author Griefed
     */
    override fun createDialog(parentComponent: Component, choice: TipOfTheDay.ShowOnStartupChoice): JDialog {
        val title = Translations.tips_title.toString()

        val dialog: JDialog

        val window: Window = parentComponent as? Window ?: SwingUtilities.getWindowAncestor(
            parentComponent
        )

        dialog = if (window is Frame) {
            JDialog(window, title, true)
        } else {
            JDialog(window as Dialog, title, true)
        }

        dialog.contentPane.layout = BorderLayout(10, 10)
        dialog.contentPane.add(tipPane, BorderLayout.CENTER)
        (dialog.contentPane as JComponent).border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

        // tip control
        val controls = JPanel(BorderLayout())
        dialog.add("South", controls)

        val showOnStartupBox = JCheckBox(Translations.tips_show.toString(), choice.isShowingOnStartup)
        controls.add(showOnStartupBox, BorderLayout.CENTER)

        val buttons = JPanel(GridLayout(1, 2, 9, 0))
        controls.add(buttons, BorderLayout.LINE_END)

        val previousTipButton = JButton(Translations.tips_previous.toString())
        buttons.add(previousTipButton)
        previousTipButton.addActionListener(PreviousTipAction())

        val nextTipButton = JButton(Translations.tips_next.toString())
        buttons.add(nextTipButton)
        nextTipButton.addActionListener(NextTipAction())

        val closeButton = JButton(Translations.tips_close.toString())
        buttons.add(closeButton)

        closeButton.addActionListener {
            if (dialog.isDisplayable) {
                dialog.isVisible = false
            }
            choice.isShowingOnStartup = showOnStartupBox.isSelected
        }

        dialog.rootPane.setDefaultButton(closeButton)

        dialog.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                choice.isShowingOnStartup = showOnStartupBox.isSelected
            }
        })

        val closeAction = ActionListener { _: ActionEvent? ->
            if (dialog.isDisplayable) {
                dialog.isVisible = false
            }
        }

        (dialog.contentPane as JComponent).registerKeyboardAction(
            closeAction,
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        )

        dialog.isResizable = false
        dialog.pack()
        dialog.setLocationRelativeTo(parentComponent)
        updatePreferredSize()
        dialog.size = preferredDimension
        dialog.preferredSize = preferredDimension
        return dialog
    }

    /**
     * Customized tip-update based on [tokyo.northside.swing.plaf.BasicTipOfTheDayUI.showCurrentTip]
     *
     * @author Griefed
     */
    override fun showCurrentTip() {
        if (currentTipComponent != null) {
            tipArea.remove(currentTipComponent)
        }

        val currentTip = tipPane.currentTip
        if (currentTip == -1) {
            val label = JLabel()
            label.isOpaque = true
            label.setBackground(UIManager.getColor("TextArea.background"))
            currentTipComponent = label
            tipArea.add("Center", currentTipComponent)
            updateImage(currentTip)
            return
        }

        // tip does not fall in current tip range
        if (tipPane.model == null || tipPane.model.tipCount == 0 || currentTip < 0 && currentTip >= tipPane.model.tipCount) {
            currentTipComponent = JLabel()
        } else {
            val tip = tipPane.model.getTipAt(currentTip)
            val tipObject = tip.tip
            val tipScroll = JScrollPane()
            tipScroll.border = null
            tipScroll.isOpaque = false
            tipScroll.viewport.isOpaque = false
            tipScroll.border = null
            val text = tipObject?.toString() ?: ""
            val editor = JEditorPane("text/html", text)
            editor.setFont(tipPane.font)
            BasicHTML.updateRenderer(editor, text)
            editor.isEditable = false
            editor.border = null
            editor.margin = null
            editor.isOpaque = false
            editor.setFont(tipFont)
            tipScroll.viewport.view = editor

            currentTipComponent = tipScroll
        }

        updateImage(currentTip)

        tipArea.add("Center", currentTipComponent)
        tipArea.revalidate()
        tipArea.repaint()
    }

    private fun updateImage(currentTip: Int) {
        if ((tipPane.model.getTipAt(currentTip) as CustomTip).getImage() != null) {
            tipImage.icon = (tipPane.model.getTipAt(currentTip) as CustomTip).getImage()!!.getAspectRatioScaledInstance(800)
            tipArea.add("East",tipImage)
        } else {
            tipImage.icon = null
            tipArea.remove(tipImage)
        }
    }

    private fun updatePreferredSize() {
        if (tipImage.icon == null) {
            preferredDimension.width = defaultSize.width
            preferredDimension.height = defaultSize.height
        } else {
            if (defaultSize.width <= (defaultSize.width + tipImage.icon.iconWidth)) {
                preferredDimension.width = defaultSize.width + tipImage.icon.iconWidth
            }
            if (defaultSize.height <= ((defaultSize.height / 2) + tipImage.icon.iconHeight)) {
                preferredDimension.height = (defaultSize.height / 2) + tipImage.icon.iconHeight
            }
        }
        try {
            var parent = tipArea.parent
            while(!parent.instanceOf(JDialog::class)) {
                parent = parent.parent
            }
            parent.size = preferredDimension
            parent.preferredSize = preferredDimension
        } catch (_: NullPointerException) {}
    }

    private fun updateViewedTips() {
        val newViewed = TreeSet(guiProps.viewedTips)
        newViewed.add(tipPane.currentTip)
        guiProps.viewedTips = newViewed
    }

    /**
     * See [tokyo.northside.swing.plaf.BasicTipOfTheDayUI.PreviousTipAction]
     * @author Frederic Lavigne
     * @author Hiroshi Miura
     */
    inner class PreviousTipAction : AbstractAction("previousTip") {
        override fun actionPerformed(e: ActionEvent) {
            tipPane.previousTip()
            updateViewedTips()
            updatePreferredSize()
        }

        override fun isEnabled(): Boolean {
            return tipPane.isEnabled
        }
    }

    /**
     * See [tokyo.northside.swing.plaf.BasicTipOfTheDayUI.NextTipAction]
     * @author Frederic Lavigne
     * @author Hiroshi Miura
     */
    inner class NextTipAction : AbstractAction("nextTip") {
        override fun actionPerformed(e: ActionEvent) {
            tipPane.nextTip()
            updateViewedTips()
            updatePreferredSize()
        }

        override fun isEnabled(): Boolean {
            return tipPane.isEnabled
        }
    }
}