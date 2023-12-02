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

import de.griefed.serverpackcreator.api.utilities.common.concatenate

/**
 * Conveniently access all different check-types, whether they passed and which errors, if any, were encountered.
 *
 * @author Griefed
 */
class ConfigCheck {

    val configErrors: MutableList<String> = mutableListOf()
    val configChecksPassed: Boolean
        get() {
            return configErrors.isEmpty()
        }

    val modpackErrors: MutableList<String> = mutableListOf()
    val modpackChecksPassed: Boolean
        get() {
            return modpackErrors.isEmpty()
        }

    val inclusionErrors: MutableList<String> = mutableListOf()
    val inclusionsChecksPassed: Boolean
        get() {
            return inclusionErrors.isEmpty()
        }

    val minecraftVersionErrors: MutableList<String> = mutableListOf()
    val minecraftVersionChecksPassed: Boolean
        get() {
            return minecraftVersionErrors.isEmpty()
        }

    val modloaderErrors: MutableList<String> = mutableListOf()
    val modloaderChecksPassed: Boolean
        get() {
            return modloaderErrors.isEmpty()
        }

    val modloaderVersionErrors: MutableList<String> = mutableListOf()
    val modloaderVersionChecksPassed: Boolean
        get() {
            return modloaderVersionErrors.isEmpty()
        }

    val serverIconErrors: MutableList<String> = mutableListOf()
    val serverIconChecksPassed: Boolean
        get() {
            return serverIconErrors.isEmpty()
        }

    val serverPropertiesErrors: MutableList<String> = mutableListOf()
    val serverPropertiesChecksPassed: Boolean
        get() {
            return serverPropertiesErrors.isEmpty()
        }

    val pluginsErrors: MutableList<String> = mutableListOf()
    val pluginsChecksPassed: Boolean
        get() {
            return pluginsErrors.isEmpty()
        }

    val otherErrors: MutableList<String> = mutableListOf()
    val otherChecksPassed: Boolean
        get() {
            return otherErrors.isEmpty()
        }

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