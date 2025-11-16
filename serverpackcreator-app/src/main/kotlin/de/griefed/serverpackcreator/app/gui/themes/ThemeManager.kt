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
package de.griefed.serverpackcreator.app.gui.themes

import com.formdev.flatlaf.*
import com.formdev.flatlaf.extras.FlatAnimatedLafChange
import com.formdev.flatlaf.intellijthemes.FlatAllIJThemes
import com.formdev.flatlaf.intellijthemes.FlatDarkPurpleIJTheme
import com.formdev.flatlaf.themes.FlatMacDarkLaf
import com.formdev.flatlaf.themes.FlatMacLightLaf
import com.formdev.flatlaf.util.StringUtils
import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.api.utilities.common.JarUtilities
import de.griefed.serverpackcreator.api.utilities.common.create
import de.griefed.serverpackcreator.app.gui.GuiProps
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.awt.Frame
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import javax.swing.SwingUtilities
import javax.swing.UIManager


/**
 * Theme manager of ServerPackCreator, providing a list of available themes to change them from other places.
 * Heavily inspired by https://github.com/JFormDesigner/FlatLaf/blob/main/flatlaf-demo/src/main/java/com/formdev/flatlaf/demo/intellijthemes/IJThemesManager.java
 *
 * @author Griefed
 */
class ThemeManager(private val apiWrapper: ApiWrapper, private val guiProps: GuiProps) {

    private val log by lazy { cachedLoggerOf(this.javaClass) }
    private val themeRegex = ".*\\.(properties|txt)".toRegex()
    private val lastModifiedMap: HashMap<File, Long> = HashMap()
    val themesDir: File = File(apiWrapper.apiProperties.homeDirectory, "themes").absoluteFile
    val themes = mutableListOf<ThemeInfo>()

    init {
        themesDir.create(createFileOrDir = true, asDirectory = true)
        provideExamples()
        reloadThemes()
    }

    /**
     * @author Griefed
     */
    fun reloadThemes() {
        themes.clear()
        loadFlatLafThemes()
        loadThemesFromDirectory()
        loadIJThemes()
    }

    /**
     * @author Griefed
     */
    private fun provideExamples() {
        try {
            var themesPrefix = "BOOT-INF/classes"
            if (apiWrapper.apiProperties.isExe()) {
                themesPrefix = ""
                //source = "de/griefed/resources/manifests"
            }
            JarUtilities.copyFolderFromJar(
                this.javaClass,
                "de/griefed/resources/gui/themes",
                themesDir.absolutePath,
                themesPrefix,
                themeRegex,
                apiWrapper.apiProperties.tempDirectory
            )
        } catch (ex: IOException) {
            log.error("Error copying \"/de/griefed/resources/gui/themes\" from the JAR-file.", ex)
        }
    }

    /**
     * Heavily inspired by https://github.com/JFormDesigner/FlatLaf/blob/main/flatlaf-demo/src/main/java/com/formdev/flatlaf/demo/intellijthemes/IJThemesManager.java
     *
     * @author Griefed
     */
    private fun loadFlatLafThemes() {
        themes.add(
            ThemeInfo(
                "FlatLaf Light",
                null,
                null,
                FlatLightLaf::class.java.name
            )
        )

        themes.add(
            ThemeInfo(
                "FlatLaf Dark",
                null,
                null,
                FlatDarkLaf::class.java.name
            )
        )

        themes.add(
            ThemeInfo(
                "FlatLaf IntelliJ", null, null, FlatIntelliJLaf::class.java.name
            )
        )
        themes.add(
            ThemeInfo(
                "FlatLaf Darcula",
                null,
                null,
                FlatDarculaLaf::class.java.name
            )
        )

        themes.add(
            ThemeInfo(
                "FlatLaf macOS Light", null, null, FlatMacLightLaf::class.java.name
            )
        )
        themes.add(
            ThemeInfo(
                "FlatLaf macOS Dark", null, null, FlatMacDarkLaf::class.java.name
            )
        )
    }

    /**
     * @author Griefed
     */
    private fun loadIJThemes() {
        for (theme in FlatAllIJThemes.INFOS) {
            themes.add(
                ThemeInfo(
                    theme.name,
                    null,
                    null,
                    theme.className
                )
            )
        }
    }

    /**
     * Heavily inspired by https://github.com/JFormDesigner/FlatLaf/blob/main/flatlaf-demo/src/main/java/com/formdev/flatlaf/demo/intellijthemes/IJThemesManager.java
     *
     * @author Griefed
     */
    private fun loadThemesFromDirectory() {
        // get current working directory
        val themeFiles = themesDir.listFiles { _: File?, name: String ->
            name.endsWith(".theme.json")
                    || name.endsWith(".properties")
        } ?: return
        lastModifiedMap.clear()
        lastModifiedMap[themesDir] = themesDir.lastModified()
        for (f in themeFiles) {
            val fname = f.name
            val name: String = if (fname.endsWith(".properties")) {
                StringUtils.removeTrailing(fname, ".properties")
            } else {
                StringUtils.removeTrailing(fname, ".theme.json")
            }
            themes.add(ThemeInfo(name, null, f, null))
            lastModifiedMap[f] = f.lastModified()
        }
    }

    /**
     * Set the theme without animations.
     * Heavily inspired by https://github.com/JFormDesigner/FlatLaf/blob/main/flatlaf-demo/src/main/java/com/formdev/flatlaf/demo/intellijthemes/IJThemesManager.java
     *
     * @author Griefed
     */
    fun updateLookAndFeel(themeName: String) {
        val themeInfo = try {
            getThemeInfo(themeName)!!
        } catch (ex: NullPointerException) {
            val catch = FlatDarkPurpleIJTheme().name
            log.error("No theme with the name $themeName found. Defaulting to $catch")
            getThemeInfo(catch)!!
        }
        if (themeInfo.lafClassName != null) {
            if (themeInfo.lafClassName == UIManager.getLookAndFeel().javaClass.name) {
                log.info("Theme ${themeInfo.lafClassName} already active.")
                return
            }
            try {
                UIManager.setLookAndFeel(themeInfo.lafClassName)
            } catch (ex: Exception) {
                log.error("Couldn't load theme class ${themeInfo.lafClassName}", ex)
            }
        } else if (themeInfo.themeFile != null) {
            try {
                if (themeInfo.themeFile.name.endsWith(".properties")) {
                    FlatLaf.setup(FlatPropertiesLaf(themeInfo.name, themeInfo.themeFile))
                } else {
                    FlatLaf.setup(IntelliJTheme.createLaf(FileInputStream(themeInfo.themeFile)))
                }
            } catch (ex: Exception) {
                log.error("Couldn't load theme file ${themeInfo.themeFile}", ex)
            }
        }
        FlatLaf.updateUI()
    }

    /**
     * Set the theme
     *
     * @author Griefed
     */
    fun setTheme(themeInfo: ThemeInfo) {
        FlatAnimatedLafChange.showSnapshot()
        updateLookAndFeel(themeInfo.name)
        guiProps.updateThemeRelatedComponents()
        for (frame in Frame.getFrames()) {
            SwingUtilities.updateComponentTreeUI(frame)
        }
        guiProps.theme = themeInfo.name
        FlatAnimatedLafChange.hideSnapshotWithAnimation()
    }

    /**
     * @author Griefed
     */
    fun getThemeInfo(themeInfoName: String) : ThemeInfo? {
        for (theme in themes) {
            if (theme.name == themeInfoName) {
                return theme
            }
        }
        return null
    }
}