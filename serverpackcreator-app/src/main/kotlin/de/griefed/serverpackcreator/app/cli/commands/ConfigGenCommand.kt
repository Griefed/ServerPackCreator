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
package de.griefed.serverpackcreator.app.cli.commands

import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.api.utilities.common.JarUtilities
import de.griefed.serverpackcreator.app.Mode
import de.griefed.serverpackcreator.app.cli.ConfigurationEditor
import org.xml.sax.SAXException
import picocli.CommandLine
import picocli.shell.jline3.PicocliCommands.ClearScreen
import java.io.IOException
import javax.xml.parsers.ParserConfigurationException

@CommandLine.Command(
    name = "cgen", mixinStandardHelpOptions = true,
    description = ["Interactively generate a new server pack configuration."],
    subcommands = [ClearScreen::class, CommandLine.HelpCommand::class]
)
class ConfigGenCommand(
    private val apiWrapper: ApiWrapper = ApiWrapper.api(),
    private val configurationEditor: ConfigurationEditor = ConfigurationEditor(
        apiWrapper.configurationHandler,
        apiWrapper.apiProperties,
        apiWrapper.versionMeta)
) : Command {

    override fun run() {
        createDefaultConfig()
        runConfigurationEditor()
    }

    /**
     * Run in [Mode.CGEN] and allow the user to load, edit and create a
     * `serverpackcreator.conf`-file using the CLI.
     *
     * @throws IOException                  When the [de.griefed.serverpackcreator.api.versionmeta.VersionMeta] had to be instantiated, but
     * an error occurred during the parsing of a manifest.
     * @throws ParserConfigurationException When the [de.griefed.serverpackcreator.api.versionmeta.VersionMeta] had to be instantiated, but
     * an error occurred during the parsing of a manifest.
     * @throws SAXException                 When the [de.griefed.serverpackcreator.api.versionmeta.VersionMeta] had to be instantiated, but
     * an error occurred during the parsing of a manifest.
     * @author Griefed
     */
    @Throws(IOException::class, ParserConfigurationException::class, SAXException::class)
    private fun runConfigurationEditor() {
        configurationEditor.continuedRunOptions()
    }

    /**
     * Check whether a `serverpackcreator.conf`-file exists. If it doesn't exist, and we are not
     * running in [Mode.CLI] or [Mode.CGEN], create an unconfigured default one which can
     * then be loaded into the GUI.
     *
     * @return `true` if a `serverpackcreator.conf`-file was created.
     * @author Griefed
     */
    private fun createDefaultConfig() {
        if (!apiWrapper.apiProperties.defaultConfig.exists()) {
            JarUtilities.copyFileFromJar(
                "de/griefed/resources/${apiWrapper.apiProperties.defaultConfig.name}",
                apiWrapper.apiProperties.defaultConfig, this.javaClass
            )
        }
    }

}