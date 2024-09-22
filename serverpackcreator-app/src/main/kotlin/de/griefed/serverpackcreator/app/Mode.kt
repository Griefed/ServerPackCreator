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

/**
 * Available mods of ServerPackCreator and their respective CLI-arguments required to be
 * activated/used.
 *
 * @author Griefed
 */
enum class Mode(private val argument: String) {
    /**
     * **Priority 0**
     *
     *
     * Print ServerPackCreators help to commandline.
     */
    HELP("-help"),

    /**
     * **Priority 1**
     *
     *
     * Check whether a newer version of ServerPackCreator is available.
     */
    UPDATE("-update"),

    /**
     * **Priority 2**
     *
     *
     * Run the generation of a basic server pack config from a given modpack.
     */
    CGEN("-cgen"),

    /**
     * **Priority 3**
     *
     *
     * Run ServerPackCreator from the commandline and generate a server pack from a specific server pack config.
     */
    CONFIG("-config"),

    /**
     * **Priority 3.1**
     *
     *
     * Generate the server pack from the config specified in [CONFIG] in a specific location.
     * This argument requires [CONFIG] being present, too.
     */
    DESTINATION("--destination"),

    /**
     * **Priority 4**
     *
     *
     * Run ServerPackCreator in commandline-mode. If no graphical environment is supported, this is
     * the default ServerPackCreator will enter, even when starting ServerPackCreator with no extra
     * arguments at all.
     */
    CLI("-cli"),

    /**
     * **Priority 5**
     *
     *
     * Run ServerPackCreator as a webservice.
     */
    WEB("-web"),

    /**
     * **Priority 6**
     *
     *
     * Run ServerPackCreator with our GUI. If a graphical environment is supported, this is the
     * default ServerPackCreator will enter, even when starting ServerPackCreator with no extra
     * arguments at all.
     */
    GUI("-gui"),

    /**
     * **Priority 7**
     *
     *
     * Set up and prepare the environment for subsequent runs of ServerPackCreator. This will
     * create/copy all files needed for ServerPackCreator to function properly from inside its
     * JAR-file and setup everything else, too.
     *
     *
     * The `--setup`-argument also allows a user to specify a `properties`-file to load
     * into [de.griefed.serverpackcreator.api.ApiProperties]. Values are loaded from the specified file and
     * subsequently stored in the local `serverpackcreator.properties`-file inside
     * ServerPackCreators home-directory.
     */
    SETUP("--setup"),

    /**
     * When specified, this will override any and all configurations of any and all properties which specify a home-directory
     * for ServerPackCreator. This argument can be used for the initial run of ServerPackCreator, or to change the
     * home-directory later on.
     */
    HOME("--home"),

    /**
     * **Priority 8**
     *
     *
     * Exit ServerPackCreator.
     */
    EXIT("exit"),

    /**
     * Used when the user wants to change the language of ServerPackCreator.
     */
    LANG("-lang");

    /**
     * Textual representation of this mode or argument.
     *
     * @return Textual representation of this mode.
     * @author Griefed
     */
    fun argument(): String {
        return argument
    }
}