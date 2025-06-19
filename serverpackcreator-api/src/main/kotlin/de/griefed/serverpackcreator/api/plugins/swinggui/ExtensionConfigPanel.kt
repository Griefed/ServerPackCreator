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
package de.griefed.serverpackcreator.api.plugins.swinggui

import com.electronwill.nightconfig.core.CommentedConfig
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.util.*
import javax.swing.JPanel

/**
 * Template ConfigPanel for use in {@link ConfigPanelExtension} extensions.
 *
 * @param versionMeta           Instance of [VersionMeta] so you can work with available Minecraft, Forge, Fabric,
 * LegacyFabric and Quilt versions.
 * @param apiProperties         Instance of [Properties] The current configuration of ServerPackCreator, like the
 * default list of clientside-only mods, the server pack directory etc.
 * @param utilities             Instance of [Utilities] commonly used across ServerPackCreator.
 * @param serverPackConfigTab   Instance of [ServerPackConfigTab] to give you access to the various fields inside it,
 * like the modpack directory, selected Minecraft, modloader and modloader versions, etc.
 * @param pluginConfig          Plugin specific configuration conveniently provided by ServerPackCreator. This is the
 * global configuration of the plugin which provides the ConfigPanelExtension to ServerPackCreator.
 * @param extensionName         The name the titled border of this ConfigPanel will get.
 * @param pluginID              The ID of the plugin providing this extension implementation. The pluginID determines
 * which extension specific configurations are provided to this panel, and how they are stored in a given serverpackcreator.conf.
 * @author Griefed
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
abstract class ExtensionConfigPanel protected constructor(
    protected val versionMeta: VersionMeta,
    protected val apiProperties: ApiProperties,
    protected val utilities: Utilities,
    protected val serverPackConfigTab: ServerPackConfigTab,
    protected var pluginConfig: Optional<CommentedConfig>,
    val extensionName: String,
    val pluginID: String
) : JPanel() {
    protected val log by lazy { cachedLoggerOf(this.javaClass) }
    protected val pluginsLog: Logger = LogManager.getLogger("AddonsLogger")
    val serverPackExtensionConfig: ArrayList<CommentedConfig> = ArrayList<CommentedConfig>(100)

    /**
     * Retrieve this extensions' server pack specific configuration. When no configuration with configs
     * for this extension has been loaded yet, the returned list is empty. Fill it with life!
     *
     * @return Config list to be used in subsequent server pack generation runs, by various other
     * extensions.
     * @author Griefed
     */
    abstract fun serverPackExtensionConfig(): ArrayList<CommentedConfig>

    /**
     * Pass the extension configuration to the configuration panel, so it can then, in turn, load the
     * available configurations and make them editable, if so desired.
     *
     * @param serverPackExtensionConfig The list of extension configurations to pass to the
     * configuration panel.
     * @author Griefed
     */
    abstract fun setServerPackExtensionConfig(
        serverPackExtensionConfig: ArrayList<CommentedConfig>
    )

    /**
     * Clear the interface, or in other words, reset this extensions config panel UI. If your Config
     * Panel Extensions has no elements you wish to reset, then simply overwrite this method with an
     * empty method body.
     *
     * The `clear()`-method is called when the owning `TabCreateServerPack.clearInterface()`-method is called.
     */
    abstract fun clear()
}