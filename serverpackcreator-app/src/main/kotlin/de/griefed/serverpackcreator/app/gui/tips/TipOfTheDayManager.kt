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
import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import tokyo.northside.swing.TipOfTheDay
import tokyo.northside.swing.tips.DefaultTipOfTheDayModel
import java.util.*
import javax.swing.JFrame
import kotlin.reflect.full.memberProperties

/**
 * Tip of the day manager which creates and stores all tips ready for display.
 *
 * @author Griefed
 */
class TipOfTheDayManager(private val mainFrame: JFrame, private val guiProps: GuiProps) {

    private val log by lazy { cachedLoggerOf(this.javaClass) }
    private val showOnStartupChoice = ShowOnStartup()
    private val tipOfTheDayModel = DefaultTipOfTheDayModel()

    init {
        val tipEntries = Translations::class.memberProperties.filter { it.name.matches("tip_\\d+_name".toRegex()) }
        for (tipEntry in tipEntries) {
            val tipNumber = tipEntry.name
                .replace("tip_", "")
                .replace("_name", "")
            val tipName = Translations.getEntryByKey("tip.${tipNumber}.name").toString()
            val tipContent = Translations.getEntryByKey("tip.${tipNumber}.content").toString()
            tipOfTheDayModel.add(CustomTip(tipName, tipContent,"/de/griefed/resources/gui/tip$tipNumber.png"))
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun showTipOfTheDay() {
        GlobalScope.launch(Dispatchers.Swing, CoroutineStart.DEFAULT) {
            var random = (0..<tipOfTheDayModel.tipCount).random()
            if (guiProps.viewedTips.size == tipOfTheDayModel.tipCount) {
                random = 0
            } else {
                while (guiProps.viewedTips.contains(random)) {
                    random = (0..<tipOfTheDayModel.tipCount).random()
                }
            }
            log.debug("Tip to render: $random")
            val newViewed = TreeSet(guiProps.viewedTips)
            newViewed.add(random)
            guiProps.viewedTips = newViewed
            val tipOfTheDay = TipOfTheDay(tipOfTheDayModel, false)
            tipOfTheDay.currentTip = random
            val customUI = CustomTipOfTheDayUI(tipOfTheDay, guiProps)
            tipOfTheDay.setUI(customUI)
            val dialog = tipOfTheDay.ui.createDialog(mainFrame, showOnStartupChoice)
            dialog.pack()
            dialog.isVisible = true
            dialog.dispose()
        }
    }

    inner class ShowOnStartup : TipOfTheDay.ShowOnStartupChoice {
        override fun setShowingOnStartup(showOnStartup: Boolean) {
            guiProps.showTipOnStartup = showOnStartup
        }

        override fun isShowingOnStartup(): Boolean {
            return guiProps.showTipOnStartup
        }
    }
}