/* Copyright (C) 2023  Griefed
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
package de.griefed.serverpackcreator.api

import de.griefed.serverpackcreator.api.utilities.File

/**
 * Check any given [PackConfig] for errors and, if so desired, add them to a passed
 * list of errors, so you may display them in a GUI, CLI or website. The most important method is
 * [checkConfiguration] and all of its variants which will check
 * your passed configuration model for errors, indicating whether it is safe to use for further
 * operations. Running your model through the checks also ensures that the default script settings
 * are present and set according to your pack's environment.
 *
 * @author Griefed
 */
expect class ConfigurationHandler {
    fun checkConfiguration(
        configFile: File, packConfig: PackConfig, encounteredErrors: MutableList<String>, quietCheck: Boolean
    ): Boolean

    fun checkConfiguration(packConfig: PackConfig, encounteredErrors: MutableList<String>, quietCheck: Boolean): Boolean
    fun sanitizeLinks(packConfig: PackConfig)
    fun checkIconAndProperties(iconOrPropertiesPath: String): Boolean
    fun isDir(packConfig: PackConfig, encounteredErrors: MutableList<String>): Boolean
    fun isZip(packConfig: PackConfig, encounteredErrors: MutableList<String>): Boolean
    fun checkModloaderVersion(
        modloader: String, modloaderVersion: String, minecraftVersion: String, encounteredErrors: MutableList<String>
    ): Boolean

    fun ensureScriptSettingsDefaults(packConfig: PackConfig)
    fun checkInclusions(
        inclusions: MutableList<InclusionSpecification>,
        modpackDir: String,
        encounteredErrors: MutableList<String> = ArrayList(5),
        printLog: Boolean = true
    ): Boolean

    fun checkZipArchive(pathToZip: String, encounteredErrors: MutableList<String>): Boolean
    fun unzipDestination(destination: String): String
    fun suggestCopyDirs(modpackDir: String): ArrayList<InclusionSpecification>
    fun checkManifests(destination: String, packConfig: PackConfig, encounteredErrors: MutableList<String> = mutableListOf()): String?
    fun checkServerPacksForIncrement(source: String, destination: String): String
    fun printConfigurationModel(
        modpackDirectory: String,
        clientsideMods: List<String>,
        inclusions: List<InclusionSpecification>,
        installServer: Boolean,
        minecraftVer: String,
        modloader: String,
        modloaderVersion: String,
        includeIcon: Boolean,
        includeProperties: Boolean,
        includeZip: Boolean,
        javaArgs: String,
        serverPackSuffix: String,
        serverIconPath: String,
        serverPropertiesPath: String,
        scriptSettings: HashMap<String, String>
    )

    fun checkRegex(
        modpackDir: String, entry: String, exclusion: Boolean, encounteredErrors: MutableList<String>
    ): Boolean

    fun getDirectoriesInModpackZipBaseDirectory(zipFile: File): List<String>
    fun updateConfigModelFromCurseManifest(packConfig: PackConfig, manifest: File)
    fun updatePackName(packConfig: PackConfig, vararg childNodes: String): String?
    fun updateConfigModelFromMinecraftInstance(packConfig: PackConfig, minecraftInstance: File)
    fun updateConfigModelFromModrinthManifest(packConfig: PackConfig, manifest: File)
    fun updateConfigModelFromATLauncherInstance(packConfig: PackConfig, manifest: File)
    fun updateConfigModelFromConfigJson(packConfig: PackConfig, config: File)
    fun updateConfigModelFromMMCPack(packConfig: PackConfig, mmcPack: File)
    fun updateDestinationFromInstanceCfg(instanceCfg: File): String
    fun exclusionRegexCheck(modpackDir: String, entry: String, encounteredErrors: MutableList<String>): Boolean
    fun inclusionRegexCheck(modpackDir: String, entry: String, encounteredErrors: MutableList<String>): Boolean
    fun countRegexMatches(source: File, regex: Regex)
    fun checkModpackDir(modpackDir: String, encounteredErrors: MutableList<String>, printLog: Boolean): Boolean
    fun getAllFilesAndDirectoriesInModpackZip(zipFile: File): List<String>
    fun getDirectoriesInModpackZip(zipFile: File): List<String>
    fun getFilesInModpackZip(zipFile: File): List<String>
}