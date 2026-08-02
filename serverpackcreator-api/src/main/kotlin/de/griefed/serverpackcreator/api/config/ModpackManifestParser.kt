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
package de.griefed.serverpackcreator.api.config

import Translations
import com.fasterxml.jackson.databind.JsonNode
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import java.io.IOException
import java.net.URI
import java.util.*

/**
 * Parser deriving PackConfig-values from the manifests of various launchers: CurseForge
 * (manifest.json, minecraftinstance.json), Modrinth (modrinth.index.json), ATLauncher
 * (instance.json), GDLauncher (config.json, instance.json) and MultiMC/Prism (mmc-pack.json,
 * instance.cfg), including modloader-name normalization. Extracted from ConfigurationHandler
 * (refactor Phase 1c); ConfigurationHandler remains the facade through which consumers access
 * these parsers.
 */
class ModpackManifestParser(
    private val apiProperties: ApiProperties,
    private val utilities: Utilities
) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
    private val forge = SupportedModloaders.forge
    private val neoForge = SupportedModloaders.neoForge
    private val fabric = SupportedModloaders.fabric
    private val quilt = SupportedModloaders.quilt
    private val legacyFabric = SupportedModloaders.legacyFabric

    /**
     * Check whether various manifests from various launchers exist and use them to update our
     * ConfigurationModel and pack name.
     *
     * @param destination The destination in which the manifests are.
     * @param packConfig The ConfigurationModel to update.
     * @param configCheck Collection of encountered errors, if any, for convenient result-checks.
     * @return The name of the modpack currently being checked. `null` if the name could not be
     * acquired.
     * @author Griefed
     */
    fun checkManifests(destination: String, packConfig: PackConfig, configCheck: ConfigCheck = ConfigCheck()): String? {
        var packName: String? = null
        val curseManifest = File(destination, "manifest.json")
        val curseMinecraftInstance = File(destination, "minecraftinstance.json")
        val atLauncherInstance = File(destination, "instance.json")
        val gdLauncherInstance = File(File(destination).parentFile,"instance.json")
        val mmcPrismPack = File(File(destination).parentFile, "mmc-pack.json")
        val mmcPrismInstance = File(File(destination).parentFile, "instance.cfg")
        when {
            curseMinecraftInstance.exists() -> {
                // Check minecraftinstance.json usually created by Overwolf's CurseForge launcher.
                // Check misc/curseforge/minecraftinstance.json in the repo
                try {
                    updateConfigModelFromMinecraftInstance(packConfig, curseMinecraftInstance)
                    packName = if (packConfig.name != null) {
                        packConfig.name!!
                    } else {
                        updatePackName(packConfig, "name")
                    }
                } catch (ex: IOException) {
                    log.error("Error parsing minecraftinstance.json from ZIP-file.", ex)
                    configCheck.modpackErrors.add(Translations.configuration_log_error_zip_instance.toString())
                }
            }

            curseManifest.exists() -> {
                // Check manifest.json usually created by Overwolf's CurseForge launcher.
                // Check misc/curseforge/manifest.json in the repo
                try {
                    updateConfigModelFromCurseManifest(packConfig, curseManifest)
                    packName = updatePackName(packConfig, "name")
                } catch (ex: IOException) {
                    log.error("Error parsing CurseForge manifest.json from ZIP-file.", ex)
                    configCheck.modpackErrors.add(Translations.configuration_log_error_zip_manifest.toString())
                }
            }

            atLauncherInstance.exists() -> {
                // Check instance.json usually created by ATLauncher
                // Check misc/atlauncher/instance.json in the repo
                try {
                    updateConfigModelFromATLauncherInstance(packConfig, File(destination, "instance.json"))
                    packName = updatePackName(packConfig, "launcher", "name")
                } catch (ex: IOException) {
                    log.error("Error parsing config.json from ZIP-file.", ex)
                    configCheck.modpackErrors.add(Translations.configuration_log_error_zip_config.toString())
                }
            }


            gdLauncherInstance.exists() -> {
                // Check the instance.json usually created by new versions of GDLauncher, in the parent folder.
                try {
                    updateConfigModelFromGDInstanceJson(packConfig, gdLauncherInstance)
                    packName = updatePackName(packConfig, "loader", "sourceName")
                } catch (ex: IOException) {
                    log.error("Error parsing config.json from ZIP-file.", ex)
                    configCheck.modpackErrors.add(Translations.configuration_log_error_zip_config.toString())
                }
            }

            mmcPrismPack.exists() -> {
                // Check mmc-pack.json usually created by MultiMC or Prism Launcher
                try {
                    updateConfigModelFromMMCPack(packConfig, mmcPrismPack)
                } catch (ex: IOException) {
                    log.error("Error parsing mmc-pack.json from ZIP-file.", ex)
                    configCheck.modpackErrors.add(Translations.configuration_log_error_zip_mmcpack.toString())
                }
                try {
                    if (mmcPrismInstance.exists()) {
                        packName = updateDestinationFromInstanceCfg(mmcPrismInstance)
                        packConfig.name = packName
                    }
                } catch (ex: IOException) {
                    log.error("Couldn't read instance.cfg.", ex)
                }
            }
        }
        return packName
    }

    /**
     * **`manifest.json`**
     *
     * Update the given ConfigurationModel with values gathered from the downloaded CurseForge
     * modpack. A manifest.json-file is usually created when a modpack is exported through launchers
     * like Overwolf's CurseForge or GDLauncher.
     *
     * @param packConfig An instance containing a configuration for a modpack from which to
     * create a server pack.
     * @param manifest           The CurseForge manifest.json-file of the modpack to read.
     * @author Griefed
     */
    @Throws(IOException::class)
    fun updateConfigModelFromCurseManifest(packConfig: PackConfig, manifest: File) {
        packConfig.modpackJson = utilities.jsonUtilities.getJson(manifest)
        val minecraft = packConfig.modpackJson!!.get("minecraft")
        val modloaders = minecraft.get("modLoaders").get(0)
        val id = modloaders.get("id").asText()
        val modloaderAndVersion: List<String> = id.split("-")
        packConfig.minecraftVersion = minecraft.get("version").asText()
        packConfig.modloader = modloaderAndVersion[0]
        packConfig.modloaderVersion = modloaderAndVersion[1]
        packConfig.name = packConfig.modpackJson!!.get("name").asText()
    }

    /**
     * Acquire the modpacks name from the JSON previously acquired and stored in the
     * ConfigurationModel.
     *
     * @param packConfig The ConfigurationModel containing the JsonNode from which to acquire
     * the modpacks name.
     * @param childNodes         The child nodes, in order, which contain the requested packname.
     * @return The new name of the modpack.
     * @author Griefed
     */
    fun updatePackName(packConfig: PackConfig, vararg childNodes: String) = try {
        val modpackDir = apiProperties.modpacksDirectory.toString()
        val packName = packConfig.modpackJson?.let {
            utilities.jsonUtilities.getNestedText(
                it, *childNodes
            )
        }
        @Suppress("IfThenToElvis")
        if (packName != null) {
            packName
        } else {
            File(modpackDir).name
        }
    } catch (npe: NullPointerException) {
        null
    }

    /**
     * **`minecraftinstance.json`**
     *
     * Update the given ConfigurationModel with values gathered from the minecraftinstance.json of
     * the modpack. A minecraftinstance.json is usually created by Overwolf's CurseForge launcher.
     *
     * @param packConfig An instance containing a configuration for a modpack from which to
     * create a server pack.
     * @param minecraftInstance  The minecraftinstance.json-file of the modpack to read.
     * @author Griefed
     */
    @Throws(IOException::class)
    fun updateConfigModelFromMinecraftInstance(packConfig: PackConfig, minecraftInstance: File) {
        packConfig.modpackJson = utilities.jsonUtilities.getJson(minecraftInstance)
        val json = packConfig.modpackJson!!
        val base = json.get("baseModLoader")
        val modloader = base.get("name").asText().split("-")[0]
        packConfig.modloader = getModLoaderCase(modloader)
        //even Fabric, Quilt, and NeoForge have the modloader version under this JSON tag
        packConfig.modloaderVersion = base.get("forgeVersion").asText()
        packConfig.minecraftVersion = base.get("minecraftVersion").asText()
        val urlPath = arrayOf("installedModpack", "thumbnailUrl")
        val namePath = arrayOf("name")
        try {
            getAndSetIcon(json, packConfig, urlPath, namePath)
        } catch (_: NullPointerException) {
            // The manifest declares no icon URL/name, so there is no icon to download; leave the
            // server-icon path unset and continue parsing the rest of the manifest.
        } catch (ex: Exception) {
            log.error("Error acquiring icon.", ex)
        }
        packConfig.name = packConfig.modpackJson!!.get("name").asText()
        packConfig.projectID = packConfig.modpackJson!!.get("projectID").asText()
        packConfig.versionID = packConfig.modpackJson!!.get("fileID").asText()
        packConfig.source = ModpackSource.CURSEFORGE
    }

    /**
     * **`modrinth.index.json`**
     *
     * Update the given ConfigurationModel with values gathered from a Modrinth `modrinth.index.json`-manifest.
     *
     * @param packConfig The model to update.
     * @param manifest           The manifest file.
     * @author Griefed
     */
    @Throws(IOException::class)
    fun updateConfigModelFromModrinthManifest(packConfig: PackConfig, manifest: File) {
        packConfig.modpackJson = utilities.jsonUtilities.getJson(manifest)
        val dependencies = packConfig.modpackJson!!.get("dependencies")
        packConfig.minecraftVersion = dependencies.get("minecraft").asText()
        val iterator: Iterator<Map.Entry<String, JsonNode>> = dependencies.fields()
        while (iterator.hasNext()) {
            val (key, value) = iterator.next()
            when (key) {
                "fabric-loader" -> {
                    packConfig.modloader = "Fabric"
                    packConfig.modloaderVersion = value.asText()
                }

                "quilt-loader" -> {
                    packConfig.modloader = "Quilt"
                    packConfig.modloaderVersion = value.asText()
                }

                "forge" -> {
                    packConfig.modloader = "Forge"
                    packConfig.modloaderVersion = value.asText()
                }

                "neoforge" -> {
                    packConfig.modloader = "NeoForge"
                    packConfig.modloaderVersion = value.asText()
                }
            }
        }
    }

    /**
     * **`instance.json`**
     *
     * Update the given ConfigurationModel with values gathered from a ATLauncher manifest.
     *
     * @param packConfig The model to update.
     * @param manifest           The manifest file.
     * @author Griefed
     */
    @Throws(IOException::class)
    fun updateConfigModelFromATLauncherInstance(packConfig: PackConfig, manifest: File) {
        packConfig.modpackJson = utilities.jsonUtilities.getJson(manifest)
        val json = packConfig.modpackJson!!
        packConfig.minecraftVersion = json.get("id").asText()
        val launcher = json.get("launcher")
        val loaderVersion = launcher.get("loaderVersion")
        packConfig.modloader = loaderVersion.get("type").asText()
        packConfig.modloaderVersion = loaderVersion.get("version").asText()
        val urlPath = arrayOf("launcher", "curseForgeProject", "logo", "thumbnailUrl")
        val namePath = arrayOf("launcher", "name")
        try {
            getAndSetIcon(json, packConfig, urlPath, namePath)
        } catch (_: NullPointerException) {
            // The manifest declares no icon URL/name, so there is no icon to download; leave the
            // server-icon path unset and continue parsing the rest of the manifest.
        } catch (ex: Exception) {
            log.error("Error acquiring icon.", ex)
        }
        packConfig.name = packConfig.modpackJson!!.get("launcher").get("name").asText()
        try {
            packConfig.projectID = packConfig.modpackJson!!.get("launcher").get("curseForgeProject").get("id").asText()
            packConfig.versionID = packConfig.modpackJson!!.get("curseForgeFile").get("id").asText()
            packConfig.source = ModpackSource.CURSEFORGE
        } catch (ex: Exception) {
            log.error("Error acquiring modpack-source details. Please report this to ServerPackCreator in GitHub.", ex)
        }
    }

    @Throws(NullPointerException::class)
    private fun getAndSetIcon(json: JsonNode, packConfig: PackConfig, urlPath: Array<String>, namePath: Array<String>) {
        val iconUrl = URI(utilities.jsonUtilities.getNestedText(json, *urlPath)).toURL()
        val iconName = utilities.jsonUtilities.getNestedText(json, *namePath) + ".png"
        val iconFile = File(apiProperties.iconsDirectory.absolutePath, iconName)
        if (utilities.webUtilities.downloadFile(iconFile, iconUrl)) {
            packConfig.serverIconPath = iconFile.absolutePath
        }
    }

    /**
     * **`config.json`**
     *
     * Update the given ConfigurationModel with values gathered from the modpacks config.json. A
     * config.json is usually created by GDLauncher.
     *
     * @param packConfig An instance containing a configuration for a modpack from which to
     * create a server pack.
     * @param config             The config.json-file of the modpack to read.
     * @author Griefed
     */
    @Throws(IOException::class)
    fun updateConfigModelFromConfigJson(packConfig: PackConfig, config: File) {
        packConfig.modpackJson = utilities.jsonUtilities.getJson(config)
        val loader = packConfig.modpackJson!!.get("loader")
        packConfig.modloader = getModLoaderCase(loader.get("loaderType").asText())
        packConfig.minecraftVersion = loader.get("mcVersion").asText()
        packConfig.modloaderVersion =
            loader.get("loaderVersion").asText().replace("${packConfig.minecraftVersion}-", "")
    }

    /**
     * **`parentDirectory/instance.json`**
     *
     * Update the given PackConfig with values gathered from the modpacks instance.json. An
     * instance.json is usually created by GDLauncher and located in the modpacks parent directory of the data-directory.
     *
     * @param packConfig An instance containing a configuration for a modpack from which to
     * create a server pack.
     * @param instance             The instance.json-file of the modpack to read.
     */
    @Throws(IOException::class)
    fun updateConfigModelFromGDInstanceJson(packConfig: PackConfig, instance: File) {
        packConfig.modpackJson = utilities.jsonUtilities.getJson(instance)
        val version = packConfig.modpackJson!!.get("game_configuration").get("version")
        packConfig.modloader = version.get("modloaders")[0].get("type").asText()
        packConfig.minecraftVersion = version.get("release").asText()
        packConfig.modloaderVersion = version.get("modloaders")[0].get("version").asText().replace("${packConfig.minecraftVersion}-","")
        packConfig.name = packConfig.modpackJson!!.get("name").asText()
        packConfig.projectID = packConfig.modpackJson!!.get("modpack").get("project_id").asInt().toString()
        packConfig.versionID = packConfig.modpackJson!!.get("modpack").get("file_id").asInt().toString()
        val source = packConfig.modpackJson!!.get("modpack").get("platform").asText()
        packConfig.source = if (source == "Curseforge") {
            ModpackSource.CURSEFORGE
        } else {
            ModpackSource.MODRINTH
        }
    }

    /**
     * **`mmc-pack.json`**
     *
     *
     * Update the given ConfigurationModel with values gathered from the modpacks mmc-pack.json. A
     * mmc-pack.json is usually created by the MultiMC launcher.
     *
     * @param packConfig An instance containing a configuration for a modpack from which to
     * create a server pack.
     * @param mmcPack            The config.json-file of the modpack to read.
     * @author Griefed
     */
    @Throws(IOException::class)
    fun updateConfigModelFromMMCPack(packConfig: PackConfig, mmcPack: File) {
        packConfig.modpackJson = utilities.jsonUtilities.getJson(mmcPack)
        val components = packConfig.modpackJson!!.get("components")
        for (jsonNode in components) {
            val version = jsonNode.get("version").asText()
            when (jsonNode.get("uid").asText()) {
                "net.minecraft" -> packConfig.minecraftVersion = version
                "net.minecraftforge" -> {
                    packConfig.modloader = "Forge"
                    packConfig.modloaderVersion = version
                }

                "net.fabricmc.fabric-loader" -> {
                    packConfig.modloader = "Fabric"
                    packConfig.modloaderVersion = version
                }

                "org.quiltmc.quilt-loader" -> {
                    packConfig.modloader = "Quilt"
                    packConfig.modloaderVersion = version
                }

                "net.neoforged" -> {
                    packConfig.modloader = "NeoForge"
                    packConfig.modloaderVersion = version
                }
            }
        }
    }

    /**
     * **`instance.cfg`**
     *
     * Acquire the name of the modpack/instance of a MultiMC modpack from the modpacks
     * instance.cfg, which is usually created by the MultiMC launcher.
     *
     * @param instanceCfg The config.json-file of the modpack to read.
     * @return The instance name.
     * @author Griefed
     */
    @Throws(IOException::class)
    fun updateDestinationFromInstanceCfg(instanceCfg: File): String {
        var name: String
        instanceCfg.inputStream().use {
            val properties = Properties()
            properties.load(it)
            name = properties.getProperty("name", null)
        }
        return name
    }

    /**
     * Ensures the modloader is normalized to first letter upper case and rest lower case. Basically
     * allows the user to input Forge or Fabric in any combination of upper- and lowercase and
     * ServerPackCreator will still be able to work with the users input.
     *
     * @param modloader Modloader String-representation to normalize.
     * @return A normalized String of the specified modloader.
     * @author Griefed
     */
    fun getModLoaderCase(modloader: String) = when {
        // Most specific names first: "neoforge" contains "forge" and "legacyfabric" contains
        // "fabric", so checking NeoForge before Forge and LegacyFabric before Fabric prevents
        // misdetection of the more specific loader as the generic one.
        modloader.lowercase().matches(neoForge) || modloader.lowercase().contains("neoforge") -> {
            "NeoForge"
        }
        modloader.lowercase().matches(forge) || modloader.lowercase().contains("forge") -> {
            "Forge"
        }
        modloader.lowercase().matches(legacyFabric) || modloader.lowercase().contains("legacyfabric") -> {
            "LegacyFabric"
        }
        modloader.lowercase().matches(fabric) || modloader.lowercase().contains("fabric") -> {
            "Fabric"
        }
        modloader.lowercase().matches(quilt) || modloader.lowercase().contains("quilt") -> {
            "Quilt"
        }
        else -> {
            log.warn { "No suitable modloader found. Defaulting to Forge." }
            "Forge"
        }
    }
}
