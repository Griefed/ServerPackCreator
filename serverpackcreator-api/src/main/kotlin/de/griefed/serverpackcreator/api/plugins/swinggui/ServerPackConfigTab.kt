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
package de.griefed.serverpackcreator.api.plugins.swinggui

import de.griefed.serverpackcreator.api.config.InclusionSpecification
import de.griefed.serverpackcreator.api.config.PackConfig
import java.io.File

/**
 * Force every server pack configuration tab to provide a certain set of methods. Said set of methods give plugins
 * access to various configurable values, such as
 * * the modpack directory
 * * the list of clientside-only mods
 * * the list of files and folders to include or exclude
 * and more.
 *
 * **What every accessor here reads and writes is the tab's *live* state — what the user currently sees — not the
 * last saved configuration.** That is the contract the individual methods rely on rather than repeating:
 *
 * * a getter reflects unsaved edits, so two calls around a user interaction can legitimately differ;
 * * a setter mutates the tab exactly as typing into it would, which means SPC's own validation and status icons
 *   react to it — call [validateInputFields] if you need that to happen at a point of your choosing;
 * * nothing here persists anything. [getCurrentConfiguration] builds a `PackConfig` from the live state on demand,
 *   and [saveCurrentConfiguration] is the only method that reaches disk.
 *
 * The paired `getX`/`getXList` accessors are the same data in two shapes: the string as it appears in the field,
 * and the parsed entries. Prefer the list form for logic — the string form's delimiters are a UI detail.
 *
 * @author Griefed
 */
@Suppress("unused")
interface ServerPackConfigTab {
    /** Replace the clientside-mod exclusion list, as if the user had typed it into the tab. */
    fun setClientSideMods(entries: MutableList<String>)

    /** Replace the whitelist — entries here survive a clientside-exclusion match. */
    fun setWhitelist(entries: MutableList<String>)

    /** Replace the inclusion specifications: what gets copied into the server pack, with optional filters. */
    fun setInclusions(entries: MutableList<InclusionSpecification>)

    /** Tick or untick "include the server icon". */
    fun setIconInclusionTicked(ticked: Boolean)

    /** Replace the JVM arguments written into the generated start scripts. */
    fun setJavaArguments(javaArguments: String)

    /** Select the Minecraft version. Changing it can invalidate the selected modloader version. */
    fun setMinecraftVersion(version: String)

    /** Select the modloader. Unrecognised names are ignored rather than rejected — see `PackConfig.modloader`. */
    fun setModloader(modloader: String)

    /** Select the modloader version, meaningful only together with the Minecraft version and modloader. */
    fun setModloaderVersion(version: String)

    /** Set the modpack directory the server pack is generated from. */
    fun setModpackDirectory(directory: String)

    /** Tick or untick "include server.properties". */
    fun setPropertiesInclusionTicked(ticked: Boolean)

    /** Replace the start-script placeholder values, keyed by their `SPC_..._SPC` marker. */
    fun setScriptVariables(variables: HashMap<String, String>)

    /** Set the server icon to include, or empty for the shipped default. */
    fun setServerIconPath(path: String)

    /** Set the suffix appended to the generated pack's directory name, so variants can coexist. */
    fun setServerPackSuffix(suffix: String)

    /** Set the `server.properties` to include, or empty for the shipped default. */
    fun setServerPropertiesPath(path: String)

    /** Tick or untick "also create a ZIP archive" — the artifact a web-frontend user downloads. */
    fun setZipArchiveCreationTicked(ticked: Boolean)

    /** The clientside-mod list as the single string shown in the field. Use [getClientSideModsList] for entries. */
    fun getClientSideMods(): String

    /** The clientside-mod list parsed into entries — the list form of [getClientSideMods]. */
    fun getClientSideModsList(): MutableList<String>

    /** The whitelist as the single string shown in the field. Use [getWhitelistList] for entries. */
    fun getWhitelist(): String

    /** The whitelist parsed into entries — the list form of [getWhitelist]. */
    fun getWhitelistList(): MutableList<String>

    /**
     * The inclusion specifications currently configured. Order is meaningful: entries are applied in sequence, so a
     * later one can copy over what an earlier one placed.
     */
    fun getInclusions(): List<InclusionSpecification>

    /**
     * The tab's current state as a [PackConfig], built on demand and **not** persisted. Use
     * [saveCurrentConfiguration] when the configuration should also reach disk.
     */
    fun getCurrentConfiguration(): PackConfig

    /** Write the tab's current configuration to disk and return the file it was written to. */
    fun saveCurrentConfiguration(): File

    /** The JVM arguments currently configured, as written into the generated start scripts. */
    fun getJavaArguments(): String

    /** The selected Minecraft version, which determines loader-version validity and the required Java. */
    fun getMinecraftVersion(): String

    /** The selected modloader in SPC's canonical spelling (`Forge`, `NeoForge`, `Fabric`, `Quilt`, `LegacyFabric`). */
    fun getModloader(): String

    /** The selected modloader version. Only meaningful together with [getModloader] and [getMinecraftVersion]. */
    fun getModloaderVersion(): String

    /** The configured modpack directory — the source the server pack is generated from. */
    fun getModpackDirectory(): String

    /**
     * The start-script placeholder values, keyed by their `SPC_..._SPC` marker. Keys absent here fall back to
     * `PackConfig.defaultScriptValues`, so an empty map is normal rather than a missing configuration.
     */
    fun getScriptSettings(): HashMap<String, String>

    /** The configured server-icon path, or empty when the shipped default is used. */
    fun getServerIconPath(): String

    /** The configured suffix, appended to the generated directory's name so variants of one modpack can coexist. */
    fun getServerPackSuffix(): String

    /** The configured `server.properties` path, or empty when the shipped default is used. */
    fun getServerPropertiesPath(): String

    /**
     * Whether Mojang publishes a server for the selected Minecraft version. Not every version has one, and a pack
     * for a version without a server cannot be generated — so this gates generation rather than describing the UI.
     */
    fun isMinecraftServerAvailable(): Boolean

    /** Whether "include the server icon" is ticked. */
    fun isServerIconInclusionTicked(): Boolean

    /** Whether "include server.properties" is ticked. */
    fun isServerPropertiesInclusionTicked(): Boolean

    /** Whether "also create a ZIP archive" is ticked — the archive is what a web-frontend user downloads. */
    fun isZipArchiveCreationTicked(): Boolean

    /** Empty the start-script placeholder table, leaving generation to fall back on the shipped defaults. */
    fun clearScriptVariables()

    /** Replace the JVM arguments with Aikar's recommended flags — a convenience the GUI offers as one click. */
    fun setAikarsFlagsAsJavaArguments()

    /** Re-run the tab's validation and refresh its status icons, as an edit to a field would. */
    fun validateInputFields()

    /**
     * Mojang's *declared* required Java major for the selected Minecraft version — the same authority the start
     * scripts and the grinder use, rather than a version-derived guess.
     */
    fun acquireRequiredJavaVersion(): String
}
