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
import java.util.concurrent.ConcurrentHashMap

/**
 * Display-independent state-logic for the config-editor so it can be unit-tested without a display. The Swing
 * view stays dumb: it reads the editor's field-values into a [PackConfig], asks this view-model whether
 * anything changed, which Java-version is required, whether a modloader-server can be downloaded or what the
 * pack is called, and updates its widgets accordingly.
 */
class ConfigEditorViewModel(
    /** Minecraft/modloader version metadata. */
    private val versionMeta: VersionMeta,
    /** Used to read a modpack's launcher-manifest for its name. */
    private val configurationHandler: ConfigurationHandler,
    /** Used to check whether a modloader-server installer is downloadable. */
    private val serverPackHandler: ServerPackHandler
) {

    /**
     * Version-triples already known to have a downloadable server installer.
     *
     * Only successes are remembered — see [isServerDownloadable]. Concurrent because the editor's
     * periodic check walks the open tabs on a `parallelStream`, so several threads can ask at once.
     */
    private val downloadableTriples = ConcurrentHashMap.newKeySet<Triple<String, String, String>>()

    /**
     * The last pack-name resolved per modpack directory, together with the manifest fingerprint it was
     * resolved from. Concurrent for the same reason as [downloadableTriples].
     */
    private val packNames = ConcurrentHashMap<String, Pair<String, String>>()

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
     * Whether a modloader-server installer can be downloaded for this version-triple, asking the
     * network at most once per triple that answers yes.
     *
     * Answering costs an HTTP request against the modloader's maven, and the only caller is the
     * editor's 500 ms debounce — restarted by a change in *any* field, for *every* open tab — so
     * without a memo, pausing while typing sent a request per tab. The answer depends on nothing but
     * the three versions, so re-asking about a triple is waste by construction.
     *
     * **Successes are cached, failures are not, and the asymmetry is deliberate.** A published
     * installer does not disappear, so a `true` stays true; a `false` may only mean the network
     * blinked, and remembering that would leave the editor insisting "server unavailable" until the
     * application is restarted.
     */
    fun isServerDownloadable(minecraftVersion: String, modloader: String, modloaderVersion: String): Boolean {
        val triple = Triple(minecraftVersion, modloader, modloaderVersion)
        if (downloadableTriples.contains(triple)) {
            return true
        }
        val downloadable = serverPackHandler.serverDownloadable(minecraftVersion, modloader, modloaderVersion)
        if (downloadable) {
            downloadableTriples.add(triple)
        }
        return downloadable
    }

    /**
     * The display-name for the modpack in [modpackDirectory]: whatever its launcher-manifest declares,
     * falling back to the directory's own name. Re-read only when one of those manifests changes.
     *
     * Resolves the same way the editor's title always did — the manifest read sets the name on the
     * throwaway [PackConfig] it is handed, and either that or the returned name wins over the
     * directory name.
     *
     * The memo matters because reading is not cheap: `checkManifests` parses the launcher manifest into
     * a JSON tree, and a real CurseForge `minecraftinstance.json` is multi-megabyte. Measured against
     * this repo's own 2,715,835-byte fixture, that parse is **4.70 ms** where the fingerprint below is
     * **0.021 ms** — 221x. The latency saved per tick is modest; the garbage avoided is not, since the
     * old path allocated and discarded a whole tree of a 2.7 MB document on every keystroke-pause, per
     * open tab.
     */
    fun packName(modpackDirectory: String): String {
        val fingerprint = manifestFingerprint(modpackDirectory)
        packNames[modpackDirectory]?.let { (cachedFingerprint, cachedName) ->
            if (cachedFingerprint == fingerprint) {
                return cachedName
            }
        }
        val probe = PackConfig()
        val declared = configurationHandler.checkManifests(modpackDirectory, probe)
        val name = probe.name ?: declared ?: File(modpackDirectory).name
        packNames[modpackDirectory] = fingerprint to name
        return name
    }

    /**
     * A cheap summary of the state of every launcher-manifest that could describe [modpackDirectory]:
     * each candidate's existence, size and modification time.
     *
     * Six `stat` calls, against parsing megabytes of JSON. The candidates come from
     * [ConfigurationHandler.manifestCandidates] rather than being listed here, so this cannot drift
     * from the files actually consulted — a drifted list would make the memo miss real edits.
     */
    private fun manifestFingerprint(modpackDirectory: String): String =
        configurationHandler.manifestCandidates(modpackDirectory)
            .joinToString("|") { "${it.path}:${it.length()}:${it.lastModified()}" }
}
