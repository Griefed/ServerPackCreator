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
package de.griefed.serverpackcreator.api.settings

import de.griefed.serverpackcreator.api.PropertyStore
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.IOException
import java.net.URI
import java.net.URL
import java.util.*

/**
 * Settings-group for update- and release-tracking: the URL from which the fallback mod-lists are
 * refreshed, the pre-release version-check flag, the old-version used for migrations, and the
 * fallback-list update itself. Saving to disk is delegated to the injected [saveToDisk]-callback,
 * keeping persistence-orchestration with ApiProperties. Extracted from ApiProperties (refactor
 * Phase 1b); ApiProperties remains the facade through which consumers access these values.
 */
class UpdateConfig(
    private val store: PropertyStore,
    private val generationConfig: GenerationConfig,
    private val saveToDisk: () -> Unit
) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

    companion object {
        /**
         * URL of the main-repository properties-file from which fallback-lists are refreshed
         * when no other URL is configured.
         */
        const val FALLBACK_UPDATE_URL =
            "https://raw.githubusercontent.com/Griefed/ServerPackCreator/main/serverpackcreator-api/src/main/resources/serverpackcreator.properties"

        /**
         * Property-key holding the URL from which fallback-lists are refreshed.
         */
        const val UPDATE_URL_KEY = "de.griefed.serverpackcreator.configuration.fallback.updateurl"

        /**
         * Property-key toggling version-checks against pre-releases.
         */
        const val VERSION_CHECK_PRERELEASE_KEY = "de.griefed.serverpackcreator.versioncheck.prerelease"

        /**
         * Property-key holding the previously used ServerPackCreator-version, for migrations.
         */
        const val OLD_VERSION_KEY = "de.griefed.serverpackcreator.version.old"
    }

    /**
     * Fallback-value for version-checks against pre-releases.
     */
    val fallbackCheckingForPreReleasesEnabled = false

    /**
     * The URL from which a .properties-file is read during updating of the fallback
     * clientside-mods list.
     */
    var updateUrl: URL = URI(FALLBACK_UPDATE_URL).toURL()
        get() {
            field = URI(store.acquire(UPDATE_URL_KEY, FALLBACK_UPDATE_URL)).toURL()
            return field
        }
        set(value) {
            store.define(UPDATE_URL_KEY, value.toString())
            field = value
        }

    /**
     * Whether the search for available pre-releases is enabled during version-checks.
     */
    var isCheckingForPreReleasesEnabled = fallbackCheckingForPreReleasesEnabled
        get() {
            field = store.getBool(VERSION_CHECK_PRERELEASE_KEY, fallbackCheckingForPreReleasesEnabled)
            return field
        }
        set(value) {
            store.setBool(VERSION_CHECK_PRERELEASE_KEY, value)
            field = value
            log.info("Checking for pre-releases set to $field.")
        }

    /**
     * Whether the fallback-lists for clientside-mods and whitelisted mods have been updated by
     * the last [updateFallback]-run.
     */
    var fallbackUpdated: Boolean = false
        private set

    /**
     * Updates the fallback clientside-mod-list and whitelist from the configured [updateUrl],
     * replacing the stored lists when the remote ones differ, and saving to disk via the
     * injected callback when anything changed.
     */
    fun updateFallback(): Boolean {
        var remoteProperties: Properties? = null
        try {
            updateUrl.openStream().use {
                remoteProperties = Properties()
                remoteProperties!!.load(it)
            }
        } catch (e: IOException) {
            log.debug("GitHub could not be reached.", e)
        }
        fallbackUpdated = false
        if (remoteProperties != null) {
            val newBlacklist = remoteProperties!!.getProperty(GenerationConfig.FALLBACK_MODS_LIST_KEY)
            val currentBlacklist = store.properties.getProperty(GenerationConfig.FALLBACK_MODS_LIST_KEY)
            if (newBlacklist != null && currentBlacklist != newBlacklist) {
                store.define(GenerationConfig.FALLBACK_MODS_LIST_KEY, newBlacklist)
                generationConfig.clientsideMods.clear()
                generationConfig.clientsideMods.addAll(newBlacklist.split(","))
                log.info("The fallback-list for clientside only mods has been updated to: ${generationConfig.clientsideMods}")
                fallbackUpdated = true
            }

            val newWhitelist = remoteProperties!!.getProperty(GenerationConfig.MODS_WHITELIST_KEY)
            val currentWhitelist = store.properties.getProperty(GenerationConfig.MODS_WHITELIST_KEY)
            if (newWhitelist != null && currentWhitelist != newWhitelist) {
                store.define(GenerationConfig.MODS_WHITELIST_KEY, newWhitelist)
                generationConfig.modsWhitelist.clear()
                generationConfig.modsWhitelist.addAll(newWhitelist.split(","))
                log.info("The fallback-list for whitelisted mods has been updated to: ${generationConfig.modsWhitelist}")
                fallbackUpdated = true
            }
        }
        if (fallbackUpdated) {
            saveToDisk()
        }
        return fallbackUpdated
    }

    /**
     * Stores the old ServerPackCreator-version used to perform necessary migrations between the
     * old and the current version, and saves to disk.
     */
    fun setOldVersion(version: String) {
        store.define(OLD_VERSION_KEY, version)
        saveToDisk()
    }

    /**
     * The old ServerPackCreator-version used before updating; empty on a first run.
     */
    fun oldVersion(): String = store.properties.getProperty(OLD_VERSION_KEY, "")
}
