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
package de.griefed.serverpackcreator.api.config

import de.griefed.serverpackcreator.api.ApiWrapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Save/load round-trip coverage for [PackConfig]. The existing `scriptSettingsTest` round-trips
 * scalars, script-settings and plugin-configs from a file fixture; this class additionally pins the
 * inclusions round-trip (source/destination/filters), the blank-entry filtering in the mod-list
 * setters, and the `.conf`-extension append in `save`.
 */
internal class PackConfigRoundTripTest {

    private val apiProperties =
        ApiWrapper.api(File("src/test/resources/serverpackcreator.properties")).apiProperties

    /**
     * A fully-populated configuration survives a save/load cycle with all scalar fields, mod-lists,
     * script-settings and — crucially — inclusion-specifications (including destination and both
     * filters) intact.
     */
    @Test
    fun roundTripPreservesScalarsModListsAndInclusions(@TempDir tempDir: File) {
        val original = PackConfig()
        original.modpackDir = "some/modpack/dir"
        original.minecraftVersion = "1.20.1"
        original.modloader = "Forge"
        original.modloaderVersion = "47.2.0"
        original.javaArgs = "-Xmx4G"
        original.serverPackSuffix = "-roundtrip"
        original.serverIconPath = "icon.png"
        original.serverPropertiesPath = "server.properties"
        original.isServerIconInclusionDesired = true
        original.isServerPropertiesInclusionDesired = false
        original.isZipCreationDesired = true
        original.setClientMods(mutableListOf("ClientMod-"))
        original.setModsWhitelist(mutableListOf("Keep-"))
        original.setScriptSettings(hashMapOf("SPC_CUSTOM_SPC" to "value"))
        val inclusion = InclusionSpecification("mods", "mods", "keep.*", "drop.*")
        original.setInclusions(arrayListOf(inclusion))

        original.save(File(tempDir, "roundtrip.conf"), apiProperties)
        val reloaded = PackConfig(File(tempDir, "roundtrip.conf"))

        Assertions.assertEquals(original.modpackDir, reloaded.modpackDir)
        Assertions.assertEquals(original.minecraftVersion, reloaded.minecraftVersion)
        Assertions.assertEquals(original.modloader, reloaded.modloader)
        Assertions.assertEquals(original.modloaderVersion, reloaded.modloaderVersion)
        Assertions.assertEquals(original.javaArgs, reloaded.javaArgs)
        Assertions.assertEquals(original.serverPackSuffix, reloaded.serverPackSuffix)
        Assertions.assertEquals(original.serverIconPath, reloaded.serverIconPath)
        Assertions.assertEquals(original.serverPropertiesPath, reloaded.serverPropertiesPath)
        Assertions.assertEquals(original.isServerIconInclusionDesired, reloaded.isServerIconInclusionDesired)
        Assertions.assertEquals(original.isServerPropertiesInclusionDesired, reloaded.isServerPropertiesInclusionDesired)
        Assertions.assertEquals(original.isZipCreationDesired, reloaded.isZipCreationDesired)
        Assertions.assertEquals(original.clientMods, reloaded.clientMods)
        Assertions.assertEquals(original.modsWhitelist, reloaded.modsWhitelist)
        Assertions.assertEquals("value", reloaded.scriptSettings["SPC_CUSTOM_SPC"])
        Assertions.assertEquals(1, reloaded.inclusions.size)
        Assertions.assertEquals(inclusion, reloaded.inclusions.first(), "Inclusion incl. destination/filters must round-trip")
    }

    /**
     * The mod-list setters drop blank and whitespace-only entries.
     */
    @Test
    fun modListSettersDropBlankEntries() {
        val packConfig = PackConfig()
        packConfig.setClientMods(mutableListOf("real-", "", "   ", "another-"))
        packConfig.setModsWhitelist(mutableListOf("keep-", " "))

        Assertions.assertEquals(listOf("real-", "another-"), packConfig.clientMods)
        Assertions.assertEquals(listOf("keep-"), packConfig.modsWhitelist)
    }

    /**
     * Saving to a destination without a `.conf` extension appends it.
     */
    @Test
    fun saveAppendsConfExtensionWhenMissing(@TempDir tempDir: File) {
        val packConfig = PackConfig()
        packConfig.modpackDir = "x"

        packConfig.save(File(tempDir, "no-extension"), apiProperties)

        Assertions.assertTrue(File(tempDir, "no-extension.conf").isFile, "A .conf extension must be appended")
    }
}
