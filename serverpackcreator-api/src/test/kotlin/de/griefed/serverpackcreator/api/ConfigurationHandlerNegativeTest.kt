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
package de.griefed.serverpackcreator.api

import de.griefed.serverpackcreator.api.config.ConfigCheck
import de.griefed.serverpackcreator.api.config.PackConfig
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Negative-path coverage for [de.griefed.serverpackcreator.api.config.ConfigurationHandler.checkConfiguration].
 * The existing handler tests assert the all-pass case and a few config-file negatives; this class
 * targets the per-category failure branches that were previously unexercised: fallback-list
 * injection for empty mod-lists, missing server-icon/properties, a modpack that is neither a
 * directory nor a ZIP, the unparsable-config-file catch, and the quiet-check print path.
 */
internal class ConfigurationHandlerNegativeTest {
    private val apiProperties =
        ApiWrapper.api(File("build/resources/test/serverpackcreator.properties")).apiProperties
    private val configurationHandler =
        ApiWrapper.api(File("build/resources/test/serverpackcreator.properties")).configurationHandler

    /**
     * When the configuration carries no clientside-only mods and no whitelist, the handler must
     * inject the configured fallback lists into the model.
     */
    @Test
    fun emptyClientModsAndWhitelistReceiveFallbackLists() {
        val packConfig = PackConfig()
        Assertions.assertTrue(packConfig.clientMods.isEmpty())
        Assertions.assertTrue(packConfig.modsWhitelist.isEmpty())

        configurationHandler.checkConfiguration(packConfig, ConfigCheck())

        Assertions.assertTrue(packConfig.clientMods.isNotEmpty(), "Empty clientside list must be filled from the fallback")
        Assertions.assertTrue(packConfig.modsWhitelist.isNotEmpty(), "Empty whitelist must be filled from the fallback")
    }

    /**
     * A server-icon path pointing at a non-existent file must fail the server-icon check.
     */
    @Test
    fun nonexistentServerIconIsReported(@TempDir tempDir: File) {
        val packConfig = PackConfig()
        packConfig.serverIconPath = File(tempDir, "does-not-exist.png").absolutePath
        val check = configurationHandler.checkConfiguration(packConfig, ConfigCheck())

        Assertions.assertFalse(check.serverIconChecksPassed)
        Assertions.assertTrue(check.serverIconErrors.isNotEmpty())
    }

    /**
     * A server.properties path pointing at a non-existent file must fail the server-properties check.
     */
    @Test
    fun nonexistentServerPropertiesIsReported(@TempDir tempDir: File) {
        val packConfig = PackConfig()
        packConfig.serverPropertiesPath = File(tempDir, "does-not-exist.properties").absolutePath
        val check = configurationHandler.checkConfiguration(packConfig, ConfigCheck())

        Assertions.assertFalse(check.serverPropertiesChecksPassed)
        Assertions.assertTrue(check.serverPropertiesErrors.isNotEmpty())
    }

    /**
     * A modpack directory that is neither an existing directory nor a ZIP-archive must fail the
     * modpack check.
     */
    @Test
    fun modpackThatIsNeitherDirectoryNorZipIsReported(@TempDir tempDir: File) {
        val packConfig = PackConfig()
        packConfig.modpackDir = File(tempDir, "ghost-modpack").absolutePath
        val check = configurationHandler.checkConfiguration(packConfig, ConfigCheck())

        Assertions.assertFalse(check.modpackChecksPassed)
        Assertions.assertTrue(check.modpackErrors.isNotEmpty())
    }

    /**
     * An unparsable configuration file must be caught and reported as a config error rather than
     * propagating an exception.
     */
    @Test
    fun unparsableConfigFileIsReported(@TempDir tempDir: File) {
        val brokenConf = File(tempDir, "broken.conf")
        brokenConf.writeText("= = = not valid toml = = =")
        val check = configurationHandler.checkConfiguration(brokenConf, PackConfig(), ConfigCheck())

        Assertions.assertFalse(check.configChecksPassed)
        Assertions.assertTrue(check.configErrors.isNotEmpty())
    }

    /**
     * The quiet-check path additionally prints the configuration model; a valid configuration must
     * still pass all checks when run with quietCheck enabled.
     */
    @Test
    fun quietCheckPrintsModelAndStillPasses() {
        val check = configurationHandler.checkConfiguration(
            File("src/test/resources/testresources/spcconfs/serverpackcreator.conf"),
            PackConfig(),
            ConfigCheck(),
            quietCheck = true
        )
        Assertions.assertTrue(check.allChecksPassed)
    }
}
