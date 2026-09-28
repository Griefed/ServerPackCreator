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

import de.griefed.serverpackcreator.api.config.InclusionSpecification
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
 * End-to-end behaviour of regenerating a server pack *over an existing one*, driven through
 * [de.griefed.serverpackcreator.api.serverpack.ServerPackHandler.run] with the two toggles that
 * govern it: `isServerPacksOverwriteEnabled` and `isUpdatingServerPacksEnabled`.
 *
 * Every test builds its own throwaway modpack under a [TempDir] rather than reusing the shared
 * `forge_tests` fixture, because the point of an update is what happens when the *modpack changes
 * between two runs* — a file added, removed or edited — which a read-only fixture cannot express.
 */
internal class ServerPackUpdateTest {
    private val api = ApiWrapper.api(File("build/resources/test/serverpackcreator.properties"))
    private val apiProperties = api.apiProperties
    private val serverPackHandler = api.serverPackHandler

    private var originalOverwrite = false
    private var originalUpdating = false

    /** Remembers the global toggles so a test may set them freely. */
    @BeforeEach
    fun snapshotToggles() {
        originalOverwrite = apiProperties.isServerPacksOverwriteEnabled
        originalUpdating = apiProperties.isUpdatingServerPacksEnabled
    }

    /** Restores the global toggles, so one test's settings cannot leak into the next. */
    @AfterEach
    fun restoreToggles() {
        apiProperties.isServerPacksOverwriteEnabled = originalOverwrite
        apiProperties.isUpdatingServerPacksEnabled = originalUpdating
    }

    /**
     * Writes [content] to `relative` beneath [root], creating parent directories, and returns the
     * file. The one-liner every fixture in this class is built from.
     */
    private fun write(root: File, relative: String, content: String): File {
        val file = File(root, relative)
        file.parentFile.mkdirs()
        file.writeText(content)
        return file
    }

    /**
     * Lays out a minimal but realistic modpack — a mod, a config file and a nested config
     * directory — under `<tempDir>/modpack`, and returns it.
     */
    private fun modpack(tempDir: File): File {
        val modpackDir = File(tempDir, "modpack")
        write(modpackDir, "mods/alpha.jar", "alpha v1")
        write(modpackDir, "config/settings.cfg", "setting=1")
        write(modpackDir, "config/alpha/alpha.cfg", "alpha=1")
        return modpackDir
    }

    /**
     * A [PackConfig] copying [sources] out of [modpackDir] into [destination]. Icon, properties and
     * ZIP are off by default so a test opts into exactly the parts of the pack it asserts on,
     * keeping runs fast and failures unambiguous.
     */
    private fun packConfig(
        modpackDir: File,
        destination: File,
        vararg sources: String = arrayOf("mods", "config")
    ): PackConfig {
        val packConfig = PackConfig()
        packConfig.modpackDir = modpackDir.absolutePath
        packConfig.minecraftVersion = "1.16.5"
        packConfig.modloader = "Forge"
        packConfig.modloaderVersion = "36.1.2"
        packConfig.isServerIconInclusionDesired = false
        packConfig.isServerPropertiesInclusionDesired = false
        packConfig.isZipCreationDesired = false
        packConfig.customDestination = Optional.of(destination)
        for (source in sources) {
            packConfig.inclusions.add(InclusionSpecification(source))
        }
        return packConfig
    }

    /** Reads the `manifest.json` ServerPackCreator wrote into [destination]. */
    private fun manifestOf(destination: File): ServerPackManifest =
        api.utilities.jsonUtilities.objectMapper
            .readValue(File(destination, "manifest.json"), ServerPackManifest::class.java)

    /**
     * The update-combination as it is documented today: overwrite off, updating on. A file the
     * previous run produced, which the modpack no longer contains, is removed on the next run.
     */
    @Test
    fun updateRemovesAFileTheModpackNoLongerContains(@TempDir tempDir: File) {
        val modpackDir = modpack(tempDir)
        val destination = File(tempDir, "pack")
        val dropped = write(modpackDir, "mods/dropped.jar", "dropped v1")

        apiProperties.isServerPacksOverwriteEnabled = false
        apiProperties.isUpdatingServerPacksEnabled = true
        serverPackHandler.run(packConfig(modpackDir, destination))
        Assertions.assertTrue(File(destination, "mods/dropped.jar").isFile, "First run must produce the mod")

        dropped.delete()
        serverPackHandler.run(packConfig(modpackDir, destination))

        Assertions.assertFalse(
            File(destination, "mods/dropped.jar").exists(),
            "A mod dropped from the modpack must not survive an update"
        )
        Assertions.assertTrue(File(destination, "mods/alpha.jar").isFile, "The remaining mod must still be there")
    }

    /**
     * An update refreshes a file whose content changed in the modpack — the copy runs with
     * overwriting enabled even though the overwrite-toggle itself is off.
     */
    @Test
    fun updateRefreshesAChangedFile(@TempDir tempDir: File) {
        val modpackDir = modpack(tempDir)
        val destination = File(tempDir, "pack")

        apiProperties.isServerPacksOverwriteEnabled = false
        apiProperties.isUpdatingServerPacksEnabled = true
        serverPackHandler.run(packConfig(modpackDir, destination))

        write(modpackDir, "config/settings.cfg", "setting=2")
        serverPackHandler.run(packConfig(modpackDir, destination))

        Assertions.assertEquals(
            "setting=2",
            File(destination, "config/settings.cfg").readText(),
            "An update must refresh a file that changed in the modpack"
        )
    }

    /**
     * Files nobody but the server itself produced — a world, `ops.json` — are absent from the
     * manifest and therefore survive an update untouched. This is the property the whole feature
     * exists for.
     */
    @Test
    fun updateKeepsFilesTheServerItselfCreated(@TempDir tempDir: File) {
        val modpackDir = modpack(tempDir)
        val destination = File(tempDir, "pack")

        apiProperties.isServerPacksOverwriteEnabled = false
        apiProperties.isUpdatingServerPacksEnabled = true
        serverPackHandler.run(packConfig(modpackDir, destination))

        val level = write(destination, "world/level.dat", "a world worth keeping")
        val ops = write(destination, "ops.json", "[]")
        serverPackHandler.run(packConfig(modpackDir, destination))

        Assertions.assertEquals("a world worth keeping", level.readText(), "The world must survive an update")
        Assertions.assertTrue(ops.exists(), "ops.json must survive an update")
    }

    /**
     * Overwrite is destructive by design: it empties the destination before generating, which takes
     * anything the server created with it. Pinned so the difference to an update is unmistakable,
     * and so a change to the default is a deliberate, visible one.
     */
    @Test
    fun overwriteEmptiesTheDestination(@TempDir tempDir: File) {
        val modpackDir = modpack(tempDir)
        val destination = File(tempDir, "pack")

        apiProperties.isServerPacksOverwriteEnabled = true
        apiProperties.isUpdatingServerPacksEnabled = false
        serverPackHandler.run(packConfig(modpackDir, destination))

        val level = write(destination, "world/level.dat", "a world about to be lost")
        serverPackHandler.run(packConfig(modpackDir, destination))

        Assertions.assertFalse(level.exists(), "Overwrite deletes everything in the destination, world included")
        Assertions.assertTrue(File(destination, "mods/alpha.jar").isFile, "and regenerates the pack")
    }

    /**
     * With both toggles off nothing is deleted and nothing is overwritten: the run is purely
     * additive. That is what the setting promises, and it is also why a mod renamed by its version
     * ends up in the pack *twice* — pinned here because a user choosing this combination to protect
     * their world needs the consequence to be a documented one, not a surprise.
     */
    @Test
    fun neitherToggleMeansPurelyAdditive(@TempDir tempDir: File) {
        val modpackDir = modpack(tempDir)
        val destination = File(tempDir, "pack")

        apiProperties.isServerPacksOverwriteEnabled = false
        apiProperties.isUpdatingServerPacksEnabled = false
        serverPackHandler.run(packConfig(modpackDir, destination))

        write(modpackDir, "config/settings.cfg", "setting=2")
        File(modpackDir, "mods/alpha.jar").renameTo(File(modpackDir, "mods/alpha-2.jar"))
        serverPackHandler.run(packConfig(modpackDir, destination))

        Assertions.assertEquals(
            "setting=1",
            File(destination, "config/settings.cfg").readText(),
            "Without either toggle an existing file is never refreshed"
        )
        Assertions.assertTrue(File(destination, "mods/alpha.jar").isFile, "The superseded mod stays")
        Assertions.assertTrue(File(destination, "mods/alpha-2.jar").isFile, "and the new one lands beside it")
    }

    /**
     * The manifest records what this run produced, as paths relative to the server pack, together
     * with the versions the pack was built for — the record every later update reads back.
     */
    @Test
    fun manifestRecordsWhatTheRunProduced(@TempDir tempDir: File) {
        val modpackDir = modpack(tempDir)
        val destination = File(tempDir, "pack")

        apiProperties.isServerPacksOverwriteEnabled = true
        apiProperties.isUpdatingServerPacksEnabled = false
        serverPackHandler.run(packConfig(modpackDir, destination))

        val manifest = manifestOf(destination)
        Assertions.assertEquals("1.16.5", manifest.minecraftVersion)
        Assertions.assertEquals("Forge", manifest.modloader)
        Assertions.assertEquals("36.1.2", manifest.modloaderVersion)
        Assertions.assertTrue(
            manifest.files.map { it.replace('\\', '/') }.contains("mods/alpha.jar"),
            "Copied mods must be listed relative to the pack, got ${manifest.files}"
        )
        Assertions.assertTrue(
            manifest.files.none { it.startsWith(destination.absolutePath) },
            "Manifest entries must never be absolute"
        )
    }
}
