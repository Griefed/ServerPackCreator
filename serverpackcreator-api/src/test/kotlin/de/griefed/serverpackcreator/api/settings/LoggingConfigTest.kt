package de.griefed.serverpackcreator.api.settings

import de.griefed.serverpackcreator.api.PropertyStore
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Tests for [LoggingConfig], the log-level settings-group extracted from ApiProperties in
 * refactor Phase 1b. The log4j-XML machinery stays in ApiProperties and is represented here by
 * the injected apply-callback.
 */
internal class LoggingConfigTest {

    /**
     * Pins that the log-level defaults to INFO, is always reported uppercase, and that setting
     * a level stores it uppercased and applies it via the callback.
     */
    @Test
    fun logLevelDefaultsToInfoAndAppliesUppercased() {
        val store = PropertyStore()
        val appliedLevels = mutableListOf<String>()
        val loggingConfig = LoggingConfig(store) { level -> appliedLevels.add(level) }
        Assertions.assertEquals("INFO", loggingConfig.logLevel)

        loggingConfig.logLevel = "debug"
        Assertions.assertEquals("DEBUG", loggingConfig.logLevel)
        Assertions.assertEquals("DEBUG", store.properties.getProperty(LoggingConfig.LOG_LEVEL_KEY))
        Assertions.assertEquals(listOf("DEBUG"), appliedLevels)
    }
}
