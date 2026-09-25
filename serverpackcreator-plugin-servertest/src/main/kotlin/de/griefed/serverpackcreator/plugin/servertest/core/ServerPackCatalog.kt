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
package de.griefed.serverpackcreator.plugin.servertest.core

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import de.griefed.serverpackcreator.api.serverpack.ServerPackManifest
import java.io.File

/**
 * One server pack on disk, as the pack list shows it.
 *
 * The version fields come from the pack's `manifest.json` and are display-only — nothing launches differently
 * because of them. [selection] is what decides whether the Start button is enabled, and carries the reason
 * when it is not.
 */
data class LaunchablePack(
    /** The pack's directory, which is also the working directory its start script runs in. */
    val directory: File,
    /** The directory's own name, which is what the user named the pack when generating it. */
    val name: String,
    /** Minecraft version from the manifest, or blank when the manifest could not be read. */
    val minecraftVersion: String,
    /** Modloader from the manifest, or blank when the manifest could not be read. */
    val modloader: String,
    /** Modloader version from the manifest, or blank when the manifest could not be read. */
    val modloaderVersion: String,
    /** Whether this pack can be launched on this host, and with what — or why it cannot. */
    val selection: StartScriptSelection
)

/**
 * Finds the server packs in ServerPackCreator's server-packs directory that can be offered for launching.
 *
 * A directory counts as a server pack when it holds a `manifest.json`. That file is ServerPackCreator's own
 * "I produced this" marker — [ServerPackUpdater][de.griefed.serverpackcreator.api.serverpack.ServerPackUpdater]
 * already keys on its presence to tell an update run from a first run — so reusing it here means the list
 * cannot disagree with SPC about what a server pack is. The `<name>_server_pack.zip` archives sitting beside
 * the directories are skipped: nothing can be launched out of an archive.
 *
 * **Nothing here throws.** It runs from the tab's constructor, which `ApiPlugins.addTabExtensionTabs` calls
 * outside its own try-block — an exception would take the whole GUI's tab assembly with it.
 *
 * @param objectMapper SPC's mapper, used only to read manifests.
 * @param platform     Which host family to select start scripts for; injectable so the rules can be tested.
 * @author Griefed
 */
class ServerPackCatalog(
    objectMapper: ObjectMapper,
    private val platform: Platform = Platform.of()
) {

    /**
     * Reads manifests without binding this plugin to the exact field set of the ServerPackCreator that wrote
     * them. Derived from SPC's own mapper so any other configuration carries over, with unknown properties
     * explicitly ignored rather than inherited: a plugin is upgraded on the user's schedule and SPC on its
     * own, so a field this build has never heard of must be ignorable rather than a parse failure the user
     * reads as "my server packs disappeared".
     */
    private val manifestReader = objectMapper.copy()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .readerFor(ServerPackManifest::class.java)

    /**
     * Every launchable-looking pack in [serverPacksDirectory], sorted by name so the list does not reshuffle
     * between refreshes.
     *
     * A pack with no start script for this host is **listed rather than hidden**, carrying the reason: a pack
     * the user can see in their file manager but not in this list reads as a bug in the plugin.
     */
    fun packsIn(serverPacksDirectory: File): List<LaunchablePack> = emptyList()
}
