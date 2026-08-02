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
package de.griefed.serverpackcreator.app.gui.window.configs

import de.griefed.serverpackcreator.api.config.PackConfig
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta

/**
 * Display-independent state-logic for the config-editor, extracted from the Swing-coupled
 * ConfigEditor (refactor Phase 2) so it can be unit-tested without a display. The Swing view
 * stays dumb: it reads the editor's field-values into a [PackConfig], asks this view-model
 * whether anything changed or which Java-version is required, and updates its widgets
 * accordingly.
 */
class ConfigEditorViewModel(private val versionMeta: VersionMeta) {

    /**
     * Whether the [current] configuration has unsaved changes relative to the [lastSaved] one.
     * A never-saved configuration (null [lastSaved]) always counts as changed. The comparison
     * is intentionally scoped to the generation-relevant fields the editor tracks — pack-name,
     * project/version-IDs and extension-configs are deliberately excluded.
     */
    fun hasUnsavedChanges(current: PackConfig, lastSaved: PackConfig?): Boolean {
        if (lastSaved == null) {
            return true
        }
        return current.clientMods != lastSaved.clientMods
                || current.modsWhitelist != lastSaved.modsWhitelist
                || current.inclusions != lastSaved.inclusions
                || current.javaArgs != lastSaved.javaArgs
                || current.minecraftVersion != lastSaved.minecraftVersion
                || current.modloader != lastSaved.modloader
                || current.modloaderVersion != lastSaved.modloaderVersion
                || current.modpackDir != lastSaved.modpackDir
                || current.scriptSettings != lastSaved.scriptSettings
                || current.serverIconPath != lastSaved.serverIconPath
                || current.serverPropertiesPath != lastSaved.serverPropertiesPath
                || current.serverPackSuffix != lastSaved.serverPackSuffix
                || current.isServerIconInclusionDesired != lastSaved.isServerIconInclusionDesired
                || current.isServerPropertiesInclusionDesired != lastSaved.isServerPropertiesInclusionDesired
                || current.isZipCreationDesired != lastSaved.isZipCreationDesired
    }

    /**
     * The Java-version required to run a server for the given [minecraftVersion], or "?" when no
     * server or no Java-requirement is known for it.
     */
    fun requiredJavaVersion(minecraftVersion: String): String {
        val version = versionMeta.minecraft.requiredJavaVersion(minecraftVersion)
        return if (version.isPresent) {
            version.get()
        } else {
            "?"
        }
    }
}
