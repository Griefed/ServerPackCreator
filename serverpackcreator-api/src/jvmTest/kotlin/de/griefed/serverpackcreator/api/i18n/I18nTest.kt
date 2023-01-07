package de.griefed.serverpackcreator.api.i18n

import Api
import de.comahe.i18n4k.Locale
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class I18nTest {
    @Test
    fun localeTest() {
        Assertions.assertEquals("English (Great Britain)", Api.localeUnlocalizedName.toString(Locale("en_GB")))
    }
}