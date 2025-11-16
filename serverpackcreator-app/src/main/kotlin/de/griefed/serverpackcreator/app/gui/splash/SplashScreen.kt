/* Copyright (C) 2025 Griefed
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
package de.griefed.serverpackcreator.app.gui.splash

import javax.swing.JWindow

/**
 * Splashscreen to display during initialization of ServerPackCreator.
 *
 * @author Griefed
 */
class SplashScreen(version: String) {
    private val splash = JWindow()
    private val text: Reticulation
    private val bar: Progress

    init {
        val props = SplashProps()
        val splashes = Splashes()
        val image = splashes.getRandomSplash
        text = Reticulation(image.iconWidth, image.iconHeight, props)
        bar = Progress(image.iconWidth, image.iconHeight, props)

        splash.contentPane = BackgroundPanel(image.image, BackgroundPanel.ACTUAL, 0.0f, 0.0f)
        splash.contentPane.layout = null
        splash.setSize(image.iconWidth, image.iconHeight)
        splash.setLocationRelativeTo(null)
        splash.contentPane.background = props.secondaryColour
        splash.add(text)
        splash.add(bar)
        splash.add(Version(image.iconWidth, image.iconHeight, version, props.secondaryColour))
        splash.add(By(image.iconWidth, image.iconHeight, props.secondaryColour))
        splash.add(ExitButton(image.iconWidth))
        splash.setIconImage(props.appIcon)
        splash.isVisible = true
    }

    /**
     * @author Griefed
     */
    fun close() {
        splash.dispose()
    }

    /**
     * @author Griefed
     */
    fun update(progress: Int) {
        text.reticulate()
        bar.value = progress
    }
}