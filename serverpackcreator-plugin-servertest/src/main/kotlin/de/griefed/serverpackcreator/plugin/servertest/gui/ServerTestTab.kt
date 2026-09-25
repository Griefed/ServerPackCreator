/* Copyright (C) 2026 Griefed
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
package de.griefed.serverpackcreator.plugin.servertest.gui

import com.electronwill.nightconfig.core.CommentedConfig
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.plugins.swinggui.ExtensionTab
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import java.awt.BorderLayout
import java.io.File
import java.util.Optional
import javax.swing.JTabbedPane

/**
 * The plugin's single tab in ServerPackCreator's window.
 *
 * A `TabExtension` yields exactly one tab, so the pack list and one console per running server are a nested
 * [JTabbedPane]: the list is always index 0, and starting a pack appends a console beside it.
 *
 * **This constructor must not throw.** `ApiPlugins.addTabExtensionTabs` calls `getTab` *outside* the
 * try-block that guards the rest of tab registration, so an exception here escapes into GUI assembly rather
 * than being logged as a plugin error. Everything that can fail — an unreadable server-packs directory, a
 * malformed manifest — is reported inside the pane instead.
 *
 * Threading is a plain `SwingWorker` and daemon reader threads, deliberately: a plugin cannot reach
 * ServerPackCreator's lifecycle-cancelled `ComponentCoroutineScope`, and an unowned `GlobalScope` is the
 * anti-pattern this project spent a sprint removing from its own GUI.
 *
 * @author Griefed
 */
class ServerTestTab(
    versionMeta: VersionMeta,
    apiProperties: ApiProperties,
    utilities: Utilities,
    pluginConfig: Optional<CommentedConfig>,
    configFile: Optional<File>
) : ExtensionTab(versionMeta, apiProperties, utilities, pluginConfig, configFile) {

    /** The pack list plus one console per running server; the list is always the first tab. */
    private val panes = JTabbedPane()

    init {
        layout = BorderLayout()
        add(panes, BorderLayout.CENTER)
    }
}
