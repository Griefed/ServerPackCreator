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
package de.griefed.serverpackcreator.gui

import de.griefed.serverpackcreator.api.utilities.ReticulatingSplines
import de.griefed.serverpackcreator.gui.utilities.BackgroundPanel
import de.griefed.serverpackcreator.gui.utilities.ImageUtilities
import de.griefed.serverpackcreator.gui.utilities.getScaledInstance
import java.awt.Color
import java.awt.Font
import java.awt.Graphics
import java.awt.image.BufferedImage
import java.io.IOException
import java.util.*
import javax.swing.*
import javax.swing.plaf.basic.BasicProgressBarUI
import kotlin.math.roundToInt
import kotlin.system.exitProcess

/**
 * Splash screen shown before opening the main GUI and indicating that SPC is busy/running.
 *
 * @author Griefed
 */
class ServerPackCreatorSplash(version: String?) {
    private val reticulatingSplines: ReticulatingSplines = ReticulatingSplines()
    private val splash: JWindow
    private val text: JLabel = JLabel(reticulatingSplines.reticulate())
    private val bar: JProgressBar = JProgressBar()
    private val resourcePrefix = "/de/griefed/resources/gui"

    init {
        val background = ImageUtilities.imageIconFromResourceStream(
            this.javaClass,
            "$resourcePrefix/splashscreen${Random().nextInt(3)}.png"
        )
        val bufferedImage = BufferedImage(
            background.iconWidth,
            background.iconHeight,
            BufferedImage.TYPE_INT_ARGB
        )
        val graphics: Graphics = bufferedImage.createGraphics()
        background.paintIcon(null, graphics, 0, 0)
        graphics.dispose()
        val secondary = Color(192, 255, 238)
        val primary = Color(50, 83, 88)
        splash = JWindow()
        splash.contentPane = BackgroundPanel(bufferedImage, BackgroundPanel.ACTUAL, 0.0f, 0.0f)
        splash.contentPane.layout = null
        splash.setSize(background.iconWidth, background.iconHeight)
        splash.setLocationRelativeTo(null)
        splash.contentPane.background = secondary

        // Construct and prepare mem progress text
        text.font = Font("arial", Font.BOLD, 20)
        text.horizontalAlignment = SwingConstants.CENTER
        text.setBounds(
            0,
            Math.floorDiv(background.iconHeight, 2) + 20,
            background.iconWidth,
            40
        )
        text.foreground = secondary
        splash.add(text)

        // Construct and add progress bar
        val offsetInPercent = 20f
        bar.setBounds(
            (background.iconWidth / 100f * offsetInPercent).roundToInt(),
            Math.floorDiv(background.iconHeight, 2),
            (background.iconWidth / 100f * (100f - offsetInPercent * 2)).roundToInt(),
            20
        )
        bar.alignmentY = 0.0f
        bar.isBorderPainted = true
        bar.isStringPainted = true
        bar.background = Color.WHITE
        bar.setUI(
            object : BasicProgressBarUI() {
                // Text-colour when the bar IS covering the loading-text
                override fun getSelectionForeground(): Color {
                    return secondary
                }

                // Text-colour when bar is NOT covering the loading-text
                override fun getSelectionBackground(): Color {
                    return primary
                }
            })
        bar.foreground = primary
        bar.value = 0
        splash.add(bar)

        // Construct and add version label
        val versionLabel = JLabel(version)
        versionLabel.font = Font("arial", Font.BOLD, 15)
        versionLabel.setBounds(
            15,
            background.iconHeight - 40,
            background.iconWidth,
            40
        )
        versionLabel.foreground = secondary
        splash.add(versionLabel)

        // Construct and add sum luv
        val someLuv = JLabel("By Griefed")
        someLuv.font = Font("arial", Font.BOLD, 15)
        someLuv.setBounds(
            background.iconWidth - 100,
            background.iconHeight - 40,
            background.iconWidth,
            40
        )
        someLuv.foreground = secondary
        splash.add(someLuv)

        // Construct and add close button in case SPC was not meant to be started, or hangs
        val exit = JButton()
        val buttonSize = 16
        try {
            exit.icon = ImageUtilities.imageIconFromResourceStream(this.javaClass, "$resourcePrefix/error.png")
                .getScaledInstance(buttonSize, buttonSize)
        } catch (e: IOException) {
            throw RuntimeException("Could not instantiate exit-icon.", e)
        }
        exit.setBounds(
            background.iconWidth - buttonSize, 0, buttonSize, buttonSize
        )
        exit.addActionListener { exit() }
        exit.isContentAreaFilled = false
        exit.isOpaque = false
        exit.border = null
        exit.isBorderPainted = false
        splash.add(exit)
        splash.setIconImage(ImageUtilities.imageFromResourceStream(this.javaClass, "$resourcePrefix/app.png"))
        splash.isVisible = true
    }

    private fun exit() {
        close()
        exitProcess(0)
    }

    fun close() {
        splash.dispose()
    }

    fun update(progress: Int) {
        text.text = reticulatingSplines.reticulate()
        text.horizontalAlignment = SwingConstants.CENTER
        bar.value = progress
    }
}