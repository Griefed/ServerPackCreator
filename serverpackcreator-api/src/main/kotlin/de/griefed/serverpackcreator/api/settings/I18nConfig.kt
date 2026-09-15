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

import de.comahe.i18n4k.Locale
import de.comahe.i18n4k.config.I18n4kConfigDefault
import de.comahe.i18n4k.toTag
import de.griefed.serverpackcreator.api.PropertyStore
import org.apache.logging.log4j.kotlin.cachedLoggerOf

/**
 * Settings-group for ServerPackCreators language: parses the stored language-property into a
 * locale, propagates every change to the given i18n4k-configuration, and persists locale-changes
 * via the injected [saveToDisk]-callback. Extracted from ApiProperties (refactor Phase 1b);
 * ApiProperties remains the facade through which consumers access these values.
 */
class I18nConfig(
    private val store: PropertyStore,
    private val i18n4kConfig: I18n4kConfigDefault,
    private val saveToDisk: () -> Unit
) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

    /** Property key for the configured locale. */

    companion object {
        /**
         * Property-key holding the language-tag used by ServerPackCreator.
         */
        const val LANGUAGE_KEY = "de.griefed.serverpackcreator.language"
    }

    /**
     * Language used by ServerPackCreator; reading parses the stored tag (language, language_REGION
     * or language_REGION_variant) and propagates it to the i18n4k-configuration.
     */
    var language = Locale("en", "GB")
        get() {
            val prop = store.properties.getProperty(LANGUAGE_KEY)
            val lang = if (prop.contains("_")) {
                val split = prop.split("_")
                if (split.size == 3) {
                    Locale(split[0], split[1], split[2])
                } else {
                    Locale(split[0], split[1])
                }
            } else {
                Locale(prop)
            }
            field = lang
            i18n4kConfig.locale = field
            return field
        }
        set(value) {
            store.properties.setProperty(LANGUAGE_KEY, value.toTag())
            i18n4kConfig.locale = value
            field = value
            log.info("Language set to: ${field.displayLanguage} (${field.toTag()}).")
        }

    /**
     * Changes the locale and persists it via the injected save-callback, ensuring every
     * subsequent start of ServerPackCreator uses said locale.
     */
    fun changeLocale(locale: Locale) {
        language = locale
        saveToDisk()
        log.info("Changed locale to $language")
    }
}
