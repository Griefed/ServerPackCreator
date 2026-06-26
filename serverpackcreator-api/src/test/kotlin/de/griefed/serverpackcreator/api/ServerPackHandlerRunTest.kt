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

import de.griefed.serverpackcreator.api.config.PackConfig
import de.griefed.serverpackcreator.api.serverpack.ServerPackManifest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.*

/**
 * Covers the configuration-toggle permutations of
 * [de.griefed.serverpackcreator.api.serverpack.ServerPackHandler.run] that the per-loader e2e tests
 * do not: custom destination, ZIP-creation disabled, icon/properties inclusion disabled, the
 * written manifest's contents, and the update-existing-server-pack pruning of stale files. Each run
 * is redirected into a per-test temporary directory via [PackConfig.customDestination] so the shared
 * server-packs directory is not polluted and the tests stay isolated.
 */
internal class ServerPackHandlerRunTest {
    private val api = ApiWrapper.api(File("src/test/resources/serverpackcreator.properties"))
    private val apiProperties = api.apiProperties
    private val configurationHandler = api.configurationHandler
    private val serverPackHandler = api.serverPackHandler

    private var originalOverwrite = false
    private var originalUpdating = false

    @BeforeEach
    fun snapshotToggles() {
        originalOverwrite = apiProperties.isServerPacksOverwriteEnabled
        originalUpdating = apiProperties.isUpdatingServerPacksEnabled
    }

    @AfterEach
    fun restoreToggles() {
        apiProperties.isServerPacksOverwriteEnabled = originalOverwrite
        apiProperties.isUpdatingServerPacksEnabled = originalUpdating
    }

    /**
     * Builds a fully-checked Forge configuration whose server pack will be generated into
     * [destination].
     */
    private fun forgeConfigInto(destination: File): PackConfig {
        val packConfig = PackConfig()
        configurationHandler.checkConfiguration(
            File("src/test/resources/testresources/spcconfs/serverpackcreator.conf"),
            packConfig
        )
        packConfig.customDestination = Optional.of(destination)
        return packConfig
    }

    /**
     * A custom destination is honored: the server pack is generated at exactly that path.
     */
    @Test
    fun customDestinationIsHonored(@TempDir tempDir: File) {
        val destination = File(tempDir, "custom-pack")
        val packConfig = forgeConfigInto(destination)

        val generation = serverPackHandler.run(packConfig)

        Assertions.assertTrue(generation.success)
        Assertions.assertEquals(destination.absolutePath, generation.serverPack.absolutePath)
        Assertions.assertTrue(File(destination, "mods").isDirectory)
        Assertions.assertTrue(File(destination, "config").isDirectory)
    }

    /**
     * With ZIP-creation disabled, no ZIP-archive is produced, yet the local start scripts are still
     * created.
     */
    @Test
    fun zipCreationDisabledProducesNoZip(@TempDir tempDir: File) {
        val destination = File(tempDir, "no-zip-pack")
        val packConfig = forgeConfigInto(destination)
        packConfig.isZipCreationDesired = false

        val generation = serverPackHandler.run(packConfig)

        Assertions.assertTrue(generation.serverPackZip.isEmpty, "No ZIP must be reported")
        Assertions.assertFalse(File(destination.absolutePath + "_server_pack.zip").exists(), "No ZIP file must exist")
        Assertions.assertTrue(File(destination, "start.sh").isFile, "Local start scripts must still be created")
    }

    /**
     * With icon- and properties-inclusion disabled, neither file is copied into the server pack.
     */
    @Test
    fun iconAndPropertiesInclusionDisabledOmitsBothFiles(@TempDir tempDir: File) {
        val destination = File(tempDir, "no-extras-pack")
        val packConfig = forgeConfigInto(destination)
        packConfig.isServerIconInclusionDesired = false
        packConfig.isServerPropertiesInclusionDesired = false

        serverPackHandler.run(packConfig)

        Assertions.assertFalse(File(destination, "server-icon.png").exists(), "Icon must not be included")
        Assertions.assertFalse(File(destination, "server.properties").exists(), "Properties must not be included")
    }

    /**
     * The written manifest lists relative file paths and records the Minecraft- and modloader
     * versions of the configuration.
     */
    @Test
    fun manifestRecordsRelativeFilesAndVersions(@TempDir tempDir: File) {
        val destination = File(tempDir, "manifest-pack")
        val packConfig = forgeConfigInto(destination)

        serverPackHandler.run(packConfig)

        val manifestFile = File(destination, "manifest.json")
        Assertions.assertTrue(manifestFile.isFile, "A manifest.json must be written")
        val manifest = api.utilities.jsonUtilities.objectMapper.readValue(manifestFile, ServerPackManifest::class.java)

        Assertions.assertEquals(packConfig.minecraftVersion, manifest.minecraftVersion)
        Assertions.assertEquals(packConfig.modloader, manifest.modloader)
        Assertions.assertEquals(packConfig.modloaderVersion, manifest.modloaderVersion)
        Assertions.assertTrue(manifest.files.isNotEmpty(), "The manifest must list the included files")
        Assertions.assertTrue(
            manifest.files.none { it.startsWith(destination.absolutePath) || it.startsWith(File.separator) },
            "Manifest entries must be relative, not absolute"
        )
    }

    /**
     * With overwrite disabled and updating enabled, files listed in a pre-existing manifest but no
     * longer part of the modpack are pruned from the server pack on regeneration.
     */
    @Test
    fun updatingServerPacksPrunesStaleFiles(@TempDir tempDir: File) {
        apiProperties.isServerPacksOverwriteEnabled = false
        apiProperties.isUpdatingServerPacksEnabled = true
        val destination = File(tempDir, "update-pack")
        destination.mkdirs()
        val staleFile = File(destination, "stale-leftover.txt")
        staleFile.writeText("from a previous generation")
        ServerPackManifest(listOf("stale-leftover.txt"), "1.16.5", "Forge", "40.2.4")
            .writeToFile(destination, api.utilities.jsonUtilities.objectMapper)

        val packConfig = forgeConfigInto(destination)
        serverPackHandler.run(packConfig)

        Assertions.assertFalse(staleFile.exists(), "A stale file from the old manifest must be pruned on update")
        Assertions.assertTrue(File(destination, "mods").isDirectory, "The fresh server pack must still be generated")
    }
}
