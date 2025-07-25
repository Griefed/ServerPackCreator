/* Copyright (C) 2024  Griefed
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

import de.griefed.serverpackcreator.api.utilities.common.concatenate

/**
 * Conveniently access all different check-types, whether they passed and which errors, if any, were encountered.
 *
 * @author Griefed
 */
class ConfigCheck {

    /**
     * List of errors encountered during the config checks of the configuration.
     */
    val configErrors: MutableList<String> = mutableListOf()

    /**
     * Whether the config checks passed. Only true if all checks passed.
     */
    val configChecksPassed: Boolean
        get() {
            return configErrors.isEmpty()
        }

    /**
     * List of errors encountered during the modpack checks of the configuration.
     */
    val modpackErrors: MutableList<String> = mutableListOf()

    /***
     * Whether the modpack checks passed. Only true if all checks passed.
     */
    val modpackChecksPassed: Boolean
        get() {
            return modpackErrors.isEmpty()
        }

    /**
     * List of errors encountered during the inclusion checks of the configuration.
     */
    val inclusionErrors: MutableList<String> = mutableListOf()

    /**
     * Whether the inclusion checks passed. Only true if all checks passed.
     */
    val inclusionsChecksPassed: Boolean
        get() {
            return inclusionErrors.isEmpty()
        }

    /**
     * List of errors encountered during the Minecraft-version checks of the configuration.
     */
    val minecraftVersionErrors: MutableList<String> = mutableListOf()

    /**
     * Whether the Minecraft-version checks passed. Only true if all checks passed.
     */
    val minecraftVersionChecksPassed: Boolean
        get() {
            return minecraftVersionErrors.isEmpty()
        }

    /**
     * List of errors encountered during the modloader checks of the configuration.
     */
    val modloaderErrors: MutableList<String> = mutableListOf()

    /**
     * Whether the modloader checks passed. Only true if all checks passed.
     */
    val modloaderChecksPassed: Boolean
        get() {
            return modloaderErrors.isEmpty()
        }

    /**
     * List of errors encountered during the modloader-version checks of the configuration.
     */
    val modloaderVersionErrors: MutableList<String> = mutableListOf()

    /**
     * Whether the modloader-version checks passed. Only true if all checks passed.
     */
    val modloaderVersionChecksPassed: Boolean
        get() {
            return modloaderVersionErrors.isEmpty()
        }

    /**
     * List of errors encountered during the server-icon checks of the configuration.
     */
    val serverIconErrors: MutableList<String> = mutableListOf()

    /**
     * Whether the server-icon checks passed. Only true if all checks passed.
     */
    val serverIconChecksPassed: Boolean
        get() {
            return serverIconErrors.isEmpty()
        }

    /**
     * List of errors encountered during the server.properties checks of the configuration.
     */
    val serverPropertiesErrors: MutableList<String> = mutableListOf()

    /**
     * Whether the server.properties checks passed. Only true if all checks passed.
     */
    val serverPropertiesChecksPassed: Boolean
        get() {
            return serverPropertiesErrors.isEmpty()
        }

    /**
     * List of errors encountered in plugin-provided checks of the configuration.
     */
    val pluginsErrors: MutableList<String> = mutableListOf()

    /**
     * Whether all plugin-provided checks passed. Only true if all passed.
     */
    @Suppress("unused")
    val pluginsChecksPassed: Boolean
        get() {
            return pluginsErrors.isEmpty()
        }

    /**
     * List of errors which didn't fit any of the other categories.
     */
    val otherErrors: MutableList<String> = mutableListOf()
    val otherChecksPassed: Boolean
        get() {
            return otherErrors.isEmpty()
        }

    /**
     * List of all errors encountered during the check of this configuration.
     */
    val encounteredErrors: MutableList<String>
        get() {
            return concatenate(
                configErrors,
                modpackErrors,
                inclusionErrors,
                minecraftVersionErrors,
                modloaderErrors,
                modloaderVersionErrors,
                serverIconErrors,
                serverPropertiesErrors,
                pluginsErrors,
                otherErrors
            ).toMutableList()
        }

    /**
     * Whether all checks were passed for this configuration. Only true if every single check, including those of any
     * installed plugins, passed.
     */
    val allChecksPassed: Boolean
        get() {
            return encounteredErrors.isEmpty()
        }

    /**
     * Merge this check with the given [check] into one.
     *
     * Note that no checks are performed for already existing errors. Therefor, duplicate entries are possible.
     *
     * @author Griefed
     */
    fun and(check: ConfigCheck): ConfigCheck {
        configErrors.addAll(check.configErrors)
        modpackErrors.addAll(check.modpackErrors)
        inclusionErrors.addAll(check.inclusionErrors)
        minecraftVersionErrors.addAll(check.minecraftVersionErrors)
        modloaderErrors.addAll(check.modloaderErrors)
        modloaderVersionErrors.addAll(check.modloaderVersionErrors)
        serverIconErrors.addAll(check.serverIconErrors)
        serverPropertiesErrors.addAll(check.serverPropertiesErrors)
        pluginsErrors.addAll(check.pluginsErrors)
        otherErrors.addAll(check.otherErrors)
        return this
    }
}