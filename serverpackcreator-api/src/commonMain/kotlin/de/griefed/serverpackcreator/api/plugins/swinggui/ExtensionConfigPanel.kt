/*
 * Copyright (C) 2023 Griefed.
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

import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.utilities.CommentedConfig
import de.griefed.serverpackcreator.api.utilities.Optional
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta

/**
 * TODO write expectation docs
 */
expect abstract class ExtensionConfigPanel protected constructor(
    versionMeta: VersionMeta,
    apiProperties: ApiProperties,
    utilities: Utilities,
    serverPackConfigTab: ServerPackConfigTab,
    pluginConfig: Optional<CommentedConfig>,
    extensionName: String,
    pluginID: String
) {

    val serverPackExtensionConfig: ArrayList<CommentedConfig>

    /**
     * Retrieve this extensions server pack specific configuration. When no configuration with configs
     * for this extension has been loaded yet, the returned list is empty. Fill it with life!
     *
     * @return Config list to be used in subsequent server pack generation runs, by various other
     * extensions.
     * @author Griefed
     */
    abstract fun serverPackExtensionConfig(): ArrayList<CommentedConfig>

    /**
     * Pass the extension configuration to the configuration panel so it can then, in turn, load the
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