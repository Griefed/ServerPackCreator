package de.griefed.serverpackcreator.api.settings

import de.comahe.i18n4k.config.I18n4kConfigDefault
import de.griefed.serverpackcreator.api.PropertyStore
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Tests for [I18nConfig], the language settings-group extracted from ApiProperties in refactor
 * Phase 1b. Pins locale-parsing from the stored property, propagation to the i18n4k-config, and
 * the save-triggering locale-change.
 */
internal class I18nConfigTest {

    /**
     * Pins that the stored language-property is parsed into a locale (language_REGION form) and
     * propagated to the i18n4k-configuration.
     */
    @Test
    fun languageParsesStoredPropertyAndPropagatesToI18n4k() {
        val store = PropertyStore()
        val i18n4kConfig = I18n4kConfigDefault()
        val i18nConfig = I18nConfig(store, i18n4kConfig) {}
        store.define(I18nConfig.LANGUAGE_KEY, "de_DE")
        val locale = i18nConfig.language
        Assertions.assertEquals("de", locale.language)
        Assertions.assertEquals("DE", locale.country)
        Assertions.assertEquals(locale, i18n4kConfig.locale)
    }

    /**
     * Pins that assigning a language stores its tag and updates the i18n4k-configuration.
     */
    @Test
    fun languageSetterStoresTagAndUpdatesI18n4k() {
        val store = PropertyStore()
        val i18n4kConfig = I18n4kConfigDefault()
        val i18nConfig = I18nConfig(store, i18n4kConfig) {}
        i18nConfig.language = de.comahe.i18n4k.Locale("en", "GB")
        Assertions.assertEquals("en_GB", store.properties.getProperty(I18nConfig.LANGUAGE_KEY))
        Assertions.assertEquals("en", i18n4kConfig.locale.language)
    }

    /**
     * Pins that changing the locale stores the new language and triggers a save-to-disk.
     */
    @Test
    fun changeLocaleStoresLanguageAndSaves() {
        val store = PropertyStore()
        var saves = 0
        val i18nConfig = I18nConfig(store, I18n4kConfigDefault()) { saves++ }
        i18nConfig.changeLocale(de.comahe.i18n4k.Locale("fr", "FR"))
        Assertions.assertEquals("fr_FR", store.properties.getProperty(I18nConfig.LANGUAGE_KEY))
        Assertions.assertEquals(1, saves)
    }
}
