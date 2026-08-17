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

import de.griefed.serverpackcreator.api.config.ConfigurationHandler
import de.griefed.serverpackcreator.api.config.PackConfig
import de.griefed.serverpackcreator.api.serverpack.ServerPackHandler
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import java.io.File

/**
 * Display-independent state-logic for the config-editor, extracted from the Swing-coupled
 * ConfigEditor (refactor Phase 2) so it can be unit-tested without a display. The Swing view
 * stays dumb: it reads the editor's field-values into a [PackConfig], asks this view-model
 * whether anything changed, which Java-version is required, whether a modloader-server can be
 * downloaded or what the pack is called, and updates its widgets accordingly.
 *
 * @param versionMeta Minecraft/modloader version metadata.
 * @param configurationHandler Used to read a modpack's launcher-manifest for its name.
 * @param serverPackHandler Used to check whether a modloader-server installer is downloadable.
 */
class ConfigEditorViewModel(
    private val versionMeta: VersionMeta,
    private val configurationHandler: ConfigurationHandler,
    private val serverPackHandler: ServerPackHandler
) {

    /**
     * Whether the [current] configuration has unsaved changes relative to the [lastSaved] one.
     * A never-saved configuration (null [lastSaved]) always counts as changed. The comparison
     * is intentionally scoped to the generation-relevant fields the editor tracks — pack-name,
     * project/version-IDs and extension-configs are deliberately excluded.
     */
    fun hasUnsavedChanges(current: PackConfig, lastSaved: PackConfig?): Boolean {
        return lastSaved == null
                || current.clientMods != lastSaved.clientMods
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
    fun requiredJavaVersion(minecraftVersion: String): String =
        versionMeta.minecraft.requiredJavaVersion(minecraftVersion).orElse("?")

    /**
     * Whether a modloader-server installer can be downloaded for this version-triple.
     *
     * Reached from the editor's periodic validation, which is why it lives here rather than being
     * called inline: the answer depends on nothing but the three versions, so it is a pure question
     * about a tuple even though answering it costs a network request.
     */
    fun isServerDownloadable(minecraftVersion: String, modloader: String, modloaderVersion: String): Boolean =
        serverPackHandler.serverDownloadable(minecraftVersion, modloader, modloaderVersion)

    /**
     * The display-name for the modpack in [modpackDirectory]: whatever its launcher-manifest declares,
     * falling back to the directory's own name.
     *
     * Resolves the same way the editor's title always did — the manifest read sets the name on the
     * throwaway [PackConfig] it is handed, and either that or the returned name wins over the
     * directory name.
     */
    fun packName(modpackDirectory: String): String {
        val probe = PackConfig()
        val declared = configurationHandler.checkManifests(modpackDirectory, probe)
        return probe.name ?: declared ?: File(modpackDirectory).name
    }
}
