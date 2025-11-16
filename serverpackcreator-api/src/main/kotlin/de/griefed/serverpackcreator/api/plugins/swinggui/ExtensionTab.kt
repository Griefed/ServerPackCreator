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
@file:Suppress("MemberVisibilityCanBePrivate")

package de.griefed.serverpackcreator.api.plugins.swinggui

import com.electronwill.nightconfig.core.CommentedConfig
import com.electronwill.nightconfig.core.io.WritingMode
import com.electronwill.nightconfig.toml.TomlFormat
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.io.File
import java.util.*
import javax.swing.JPanel
import javax.swing.SwingUtilities

/**
 * Class to extend from if you want to add your own tabs to the ServerPackCreator GUI.
 *
 * @param versionMeta           Instance of [VersionMeta] so you can work with available Minecraft, Forge, Fabric,
 * LegacyFabric and Quilt versions.
 * @param apiProperties Instance of [ApiProperties] The current configuration of ServerPackCreator,
 * like the default list of clientside-only mods, the server pack directory etc.
 * @param utilities             Instance of [Utilities] commonly used across ServerPackCreator.
 * @param pluginConfig          Plugin specific configuration conveniently provided by ServerPackCreator. This is the
 * global configuration of the plugin which provides the ConfigPanelExtension to ServerPackCreator.
 * @param configFile            The config-file corresponding to the ID of the plugin, wrapped in an Optional.
 */
@Suppress("unused")
abstract class ExtensionTab protected constructor(
    protected val versionMeta: VersionMeta,
    protected val apiProperties: ApiProperties,
    protected val utilities: Utilities,
    protected val pluginConfig: Optional<CommentedConfig>,
    protected val configFile: Optional<File>
) : JPanel() {
    protected val log: Logger = LogManager.getLogger("AddonsLogger")

    /**
     * Save the current configuration of this plugin to the overlying plugins config-file. Requires your
     * plugin to register a global configuration-file. If your plugin does not provide a
     * configuration-file, this method will not store or save anything. No file will be created. Your
     * plugin **MUST** provide the initial configuration-file.
     *
     * @author Griefed
     */
    protected fun saveConfiguration() {
        SwingUtilities.invokeLater {
            if (pluginConfig.isPresent && configFile.isPresent) {
                val tomlWriter = TomlFormat.instance().createWriter()
                tomlWriter.write(
                    pluginConfig.get(), configFile.get(), WritingMode.REPLACE,
                    Charsets.UTF_8
                )
                log.info("Configuration saved.")
            } else {
                log.info("No configuration or configuration file available.")
            }
        }
    }
}