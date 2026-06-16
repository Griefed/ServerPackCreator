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
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import org.apache.logging.log4j.kotlin.cachedLoggerOf

/**
 * Validator for the modloader-concern of a server pack configuration: whether a supported
 * modloader was specified and whether the modloader-version exists for the given
 * Minecraft-version, verified against the version-meta. Extracted from ConfigurationHandler
 * (refactor Phase 1c); ConfigurationHandler remains the facade through which consumers access
 * these checks.
 */
class ModloaderValidator(private val versionMeta: VersionMeta) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
    private val forge = SupportedModloaders.forge
    private val neoForge = SupportedModloaders.neoForge
    private val fabric = SupportedModloaders.fabric
    private val quilt = SupportedModloaders.quilt
    private val legacyFabric = SupportedModloaders.legacyFabric

    /**
     * Checks whether either Forge or Fabric were specified as the modloader.
     *
     * @param modloader Check as case-insensitive for Forge or Fabric.
     * @return `true` if the specified modloader is either Forge or Fabric. False if neither.
     * @author Griefed
     */
    fun checkModloader(modloader: String, configCheck: ConfigCheck = ConfigCheck()): ConfigCheck {
        if (!modloader.lowercase().matches(forge)
            && !modloader.lowercase().matches(neoForge)
            && !modloader.lowercase().matches(fabric)
            && !modloader.lowercase().matches(quilt)
            && !modloader.lowercase().matches(legacyFabric)
        ) {
            configCheck.modloaderErrors.add(Translations.configuration_log_error_checkmodloader.toString())
            log.error("Invalid modloader specified. Modloader must be either Forge, NeoForge, Fabric or Quilt.")
        }
        return configCheck
    }

    /**
     * Check the given Minecraft and modloader versions for the specified modloader.
     *
     * @param modloader        The passed modloader which determines whether the check for Forge or
     * Fabric is called.
     * @param modloaderVersion The version of the modloader which is checked against the corresponding
     * modloader's manifest.
     * @param minecraftVersion The version of Minecraft used for checking the Forge version.
     * @return `true` if the specified modloader version was found in the corresponding
     * manifest.
     * @author Griefed
     */
    fun checkModloaderVersion(
        modloader: String, modloaderVersion: String, minecraftVersion: String, configCheck: ConfigCheck = ConfigCheck()
    ): ConfigCheck {
        when (modloader) {
            "Forge" -> if (!versionMeta.forge.isForgeAndMinecraftCombinationValid(minecraftVersion, modloaderVersion)) {
                configCheck.modloaderVersionErrors.add(
                    Translations.configuration_log_error_checkmodloaderandversion(
                        minecraftVersion, modloader, modloaderVersion
                    )
                )
            }

            "NeoForge" -> if (!versionMeta.neoForge.isNeoForgeAndMinecraftCombinationValid(minecraftVersion,modloaderVersion)) {
                configCheck.modloaderVersionErrors.add(
                    Translations.configuration_log_error_checkmodloaderandversion(
                        minecraftVersion, modloader, modloaderVersion
                    )
                )
            }

            "Fabric" -> if (!versionMeta.fabric.isVersionValid(modloaderVersion)
                || !versionMeta.fabric.getLoaderDetails(minecraftVersion,modloaderVersion).isPresent) {
                configCheck.modloaderVersionErrors.add(
                    Translations.configuration_log_error_checkmodloaderandversion(
                        minecraftVersion, modloader, modloaderVersion
                    )
                )
            }

            "Quilt" -> if (!versionMeta.quilt.isVersionValid(modloaderVersion)
                || !versionMeta.fabric.isMinecraftSupported(minecraftVersion)) {
                configCheck.modloaderVersionErrors.add(
                    Translations.configuration_log_error_checkmodloaderandversion(
                        minecraftVersion, modloader, modloaderVersion
                    )
                )
            }

            "LegacyFabric" -> if (!versionMeta.legacyFabric.isVersionValid(modloaderVersion)
                || !versionMeta.legacyFabric.isMinecraftSupported(minecraftVersion)) {
                configCheck.modloaderVersionErrors.add(
                    Translations.configuration_log_error_checkmodloaderandversion(
                        minecraftVersion, modloader, modloaderVersion
                    )
                )
            }

            else -> {
                log.error("Specified incorrect modloader version. Please check your modpack for the correct version and enter again.")
                configCheck.modloaderVersionErrors.add("Specified incorrect modloader version. Please check your modpack for the correct version and enter again.")
            }
        }
        return configCheck
    }
}
