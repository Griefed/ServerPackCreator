package de.griefed.example.kotlin.configcheck

import com.electronwill.nightconfig.core.CommentedConfig
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.config.ConfigCheck
import de.griefed.serverpackcreator.api.config.PackConfig
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.*

/**
 * Tests for the example [ConfigurationCheck] extension. Doubles as documentation-by-example of
 * how a plugin author can unit-test a [de.griefed.serverpackcreator.api.plugins.configurationhandler.ConfigCheckExtension]
 * without booting all of ServerPackCreator: the version-meta, api-properties and utilities are
 * passed but unused by this extension, so they are relaxed mocks.
 */
internal class ConfigurationCheckTest {
    private val configurationCheck = ConfigurationCheck()
    private val versionMeta = mockk<VersionMeta>(relaxed = true)
    private val apiProperties = mockk<ApiProperties>(relaxed = true)
    private val utilities = mockk<Utilities>(relaxed = true)

    /**
     * Builds a pack-specific config carrying the given text and extension-id, mirroring what
     * ServerPackCreator passes to the extension at runtime.
     */
    private fun packSpecificConfig(text: String, extension: String = "configcheckexample"): CommentedConfig {
        val config = CommentedConfig.inMemory()
        config.set<String>("text", text)
        config.set<String>("extension", extension)
        return config
    }

    /**
     * Runs the extension with the given pack-specific configs.
     */
    private fun runCheck(packSpecificConfigs: ArrayList<CommentedConfig>, configCheck: ConfigCheck): Boolean {
        val packConfig = PackConfig()
        packConfig.minecraftVersion = "1.20.1"
        return configurationCheck.runCheck(
            versionMeta,
            apiProperties,
            utilities,
            packConfig,
            configCheck,
            Optional.empty(),
            packSpecificConfigs
        )
    }

    /**
     * Pins that a pack-specific config with non-empty text reports an error and records it on the
     * config-check's plugin-errors.
     */
    @Test
    fun nonEmptyTextReportsError() {
        val configCheck = ConfigCheck()
        val result = runCheck(arrayListOf(packSpecificConfig("something is wrong")), configCheck)
        Assertions.assertTrue(result, "Non-empty text must report an error")
        Assertions.assertTrue(configCheck.pluginsErrors.any { error -> error.contains("configcheckexample") })
    }

    /**
     * Pins that without any pack-specific configs the extension reports no error.
     */
    @Test
    fun noPackSpecificConfigsReportNoError() {
        val configCheck = ConfigCheck()
        val result = runCheck(arrayListOf(), configCheck)
        Assertions.assertFalse(result)
        Assertions.assertTrue(configCheck.pluginsErrors.isEmpty())
    }

    /**
     * Pins the extension's self-describing metadata, which ServerPackCreator uses to route
     * pack-specific configuration to the right extension.
     */
    @Test
    fun extensionMetadataIsStable() {
        Assertions.assertEquals("configcheckexample", configurationCheck.extensionId)
        Assertions.assertEquals("Example Configuration Check Extension", configurationCheck.name)
        Assertions.assertEquals("Griefed", configurationCheck.author)
        Assertions.assertEquals("0.0.1-SNAPSHOT", configurationCheck.version)
    }
}
