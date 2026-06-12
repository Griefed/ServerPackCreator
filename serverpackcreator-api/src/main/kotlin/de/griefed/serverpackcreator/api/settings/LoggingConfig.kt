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

/**
 * Settings-group for ServerPackCreators log-level: stores the level uppercased and applies every
 * change via the injected [applyLogLevel]-callback. The log4j-XML machinery itself stays with
 * ApiProperties, which acts as log4j's ConfigurationFactory. Extracted from ApiProperties
 * (refactor Phase 1b); ApiProperties remains the facade through which consumers access this
 * value.
 */
class LoggingConfig(
    private val store: PropertyStore,
    private val applyLogLevel: (String) -> Unit
) {

    companion object {
        /**
         * Property-key holding ServerPackCreators log-level.
         */
        const val LOG_LEVEL_KEY = "de.griefed.serverpackcreator.loglevel"
    }

    /**
     * Log-level of ServerPackCreator, always uppercase; setting a level stores and applies it.
     */
    var logLevel = "INFO"
        get() {
            field = store.acquire(LOG_LEVEL_KEY, "INFO").uppercase()
            return field
        }
        set(value) {
            field = value.uppercase()
            store.define(LOG_LEVEL_KEY, field)
            applyLogLevel(field)
        }
}
