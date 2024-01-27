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
package de.griefed.serverpackcreator.app

import de.comahe.i18n4k.Locale
import java.awt.GraphicsEnvironment
import java.io.File

/**
 * The Commandline Parser checks the passed commandline arguments to determine the mode to run in.
 *
 * CLI arguments are checked in order of their priority, which you can find in [Mode].
 *
 * After the mode has been determined, you can acquire it with [CommandlineParser.mode].
 *
 *
 * If a specific language was passed using the `-lang`-argument, you can get it with [CommandlineParser.language].
 *
 * If ServerPackCreator was run with the `--setup`-argument specifying a `properties`-file to load into
 * [de.griefed.serverpackcreator.api.ApiProperties], you can get said file via
 * [CommandlineParser.propertiesFile]. Values are loaded from the specified file and subsequently stored in the local
 * `serverpackcreator.properties`-file inside ServerPackCreators home-directory.
 *
 * @author Griefed
 */
open class CommandlineParser(args: Array<String>) {
    var mode: Mode = Mode.GUI
    var language: Locale = Locale("en_GB")
    var propertiesFile: File = File("serverpackcreator.properties")

    init {
        val argsList = args.toList()

        /*
        * Check whether a language locale was specified by the user.
        * If none was specified, set LANG to null so the Optional returns false for isPresent(),
        * thus allowing us to use the locale set in the ApplicationProperties later on.
        */
        language = if (argsList.contains(Mode.LANG.argument())
            && argsList.size >= argsList.indexOf(Mode.LANG.argument()) + 1
        ) {
            Locale(argsList[argsList.indexOf(Mode.LANG.argument()) + 1])
        } else {
            Locale("en_GB")
        }
        run {
            /*
            * Check whether the user wanted us to print the help-text.
            */
            if (argsList.any { entry -> entry.contains(Mode.HELP.argument()) }) {
                mode = Mode.HELP
                return@run
            }

            /*
            * Check whether the user wants to check for update availability.
            */
            if (argsList.any { entry -> entry.contains(Mode.UPDATE.argument()) }) {
                mode = Mode.UPDATE
                return@run
            }

            /*
            * Check whether the user wants to generate a new serverpackcreator.conf from the commandline.
            */
            if (argsList.any { entry -> entry.contains(Mode.CGEN.argument()) }) {
                mode = Mode.CGEN
                return@run
            }

            /*
            * Check whether the user wants to run in commandline-mode or whether a GUI would not be supported.
            */
            if (argsList.any { entry -> entry.contains(Mode.CLI.argument()) }) {
                mode = Mode.CLI
                return@run
            }

            /*
            * Check whether the user wants ServerPackCreator to run as a webservice.
            */
            if (argsList.any { entry -> entry.contains(Mode.WEB.argument()) }) {
                mode = Mode.WEB
                return@run
            }

            /*
            * Check whether the user wants to use ServerPackCreators GUI.
            */
            if (argsList.any { entry -> entry.contains(Mode.GUI.argument()) } && !GraphicsEnvironment.isHeadless()) {
                mode = Mode.GUI
                return@run
            }

            /*
            * Check whether the user wants to set up and prepare the environment for subsequent runs.
            */
            if (argsList.any { entry -> entry.contains(Mode.SETUP.argument()) }) {
                val setupPos = argsList.indexOf(Mode.SETUP.argument()) + 1
                val setupArg = argsList[setupPos]
                val setupFile = File(setupArg)
                if (argsList.size > 1 && setupFile.isFile) {
                    propertiesFile = setupFile
                }
                mode = Mode.SETUP
                return@run
            }

            /*
            * Last but not least, failsafe-check whether a GUI would be supported.
            */
            if (!GraphicsEnvironment.isHeadless()) {
                mode = Mode.GUI
                return@run
            } else {
                mode = Mode.CLI
                return@run
            }
        }
    }
}