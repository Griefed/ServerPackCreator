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
package de.griefed.serverpackcreator.gui.tips

import Translations
import de.griefed.serverpackcreator.gui.GuiProps
import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import tokyo.northside.tipoftheday.TipOfTheDay
import tokyo.northside.tipoftheday.tips.DefaultTipOfTheDayModel
import java.util.*
import javax.swing.JFrame
import kotlin.reflect.full.memberProperties

/**
 * Tip of the day manager which creates and stores all tips ready for display.
 *
 * @author Griefed
 */
class TipOfTheDayManager(private val mainFrame: JFrame, private val guiProps: GuiProps) {

    private val showOnStartupChoice = ShowOnStartup()
    private val tipOfTheDayModel = DefaultTipOfTheDayModel()

    init {
        val memberProperties = Translations::class.memberProperties.filter { it.name.matches("tip_\\d+_name".toRegex()) }
        for (memberProp in memberProperties) {
            val number = memberProp.name
                .replace("tip_", "")
                .replace("_name", "")
            val name = memberProp.get(Translations).toString()
            val value = Translations::class.memberProperties
                .find { it.name == "tip_${number}_content" }
                ?.get(Translations)?.toString()
                ?: "Could not retrieve tip for ${memberProp.name}."
            tipOfTheDayModel.add(CustomTip(name, value,"/de/griefed/resources/gui/tip$number.png"))
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun showTipOfTheDay() {
        GlobalScope.launch(Dispatchers.Swing, CoroutineStart.UNDISPATCHED) {
            var random = (0..<tipOfTheDayModel.tipCount).random()
            if (guiProps.viewedTips.size == tipOfTheDayModel.tipCount) {
                random = 0
            } else {
                while (guiProps.viewedTips.contains(random)) {
                    random = (0..<tipOfTheDayModel.tipCount).random()
                }
            }
            val newViewed = TreeSet(guiProps.viewedTips)
            newViewed.add(random)
            guiProps.viewedTips = newViewed
            val tipOfTheDay = TipOfTheDay(tipOfTheDayModel)
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