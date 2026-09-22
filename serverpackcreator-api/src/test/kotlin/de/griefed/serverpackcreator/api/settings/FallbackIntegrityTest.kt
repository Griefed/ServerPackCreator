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
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.*

/**
 * A shipped default must survive the setting it is the default for.
 *
 * Every collection-valued property in [GenerationConfig] is declared as `var x = fallbackX`, which
 * makes the property and its fallback the *same* `TreeSet`, and every setter does
 * `field.clear(); field.addAll(value)`. Configuring one therefore used to overwrite the constant it
 * is supposed to be able to fall back to — which is exactly what the GUI's four "reset to default"
 * buttons read, so pressing reset after a save restored the user's own values.
 */
internal class FallbackIntegrityTest {

    /** Every list-setting, its setter, and the fallback that must be unaffected by it. */
    private fun assertFallbackSurvives(
        name: String,
        set: (GenerationConfig, TreeSet<String>) -> Unit,
        fallback: (GenerationConfig) -> TreeSet<String>
    ) {
        val config = GenerationConfig(PropertyStore())
        val shipped = TreeSet(fallback(config))
        Assertions.assertTrue(shipped.isNotEmpty(), "$name must ship a non-empty default to be worth guarding")

        set(config, TreeSet(listOf("something-the-user-chose")))

        Assertions.assertEquals(
            shipped,
            fallback(config),
            "Configuring $name must not overwrite the default it falls back to"
        )
    }

    /** The five path-lists a user can configure, each against its own fallback. */
    @Test
    fun configuringAListLeavesItsShippedDefaultIntact() {
        assertFallbackSurvives("zipArchiveExclusions", { c, v -> c.zipArchiveExclusions = v }) { it.fallbackZipExclusions }
        assertFallbackSurvives("directoriesToInclude", { c, v -> c.directoriesToInclude = v }) { it.fallbackDirectoriesInclusion }
        assertFallbackSurvives("directoriesToExclude", { c, v -> c.directoriesToExclude = v }) { it.fallbackDirectoriesExclusion }
        assertFallbackSurvives("preInstallCleanupFiles", { c, v -> c.preInstallCleanupFiles = v }) { it.fallbackPreInstallCleanupFiles }
        assertFallbackSurvives("postInstallCleanupFiles", { c, v -> c.postInstallCleanupFiles = v }) { it.fallbackPostInstallCleanupFiles }
        assertFallbackSurvives("updateProtectedPaths", { c, v -> c.updateProtectedPaths = v }) { it.fallbackUpdateProtectedPaths }
    }

    /** A setting and its fallback must not be the same object in the first place. */
    @Test
    fun aSettingIsNeverTheSameObjectAsItsFallback() {
        val config = GenerationConfig(PropertyStore())
        Assertions.assertNotSame(config.fallbackZipExclusions, config.zipArchiveExclusions)
        Assertions.assertNotSame(config.fallbackDirectoriesInclusion, config.directoriesToInclude)
        Assertions.assertNotSame(config.fallbackDirectoriesExclusion, config.directoriesToExclude)
        Assertions.assertNotSame(config.fallbackPreInstallCleanupFiles, config.preInstallCleanupFiles)
        Assertions.assertNotSame(config.fallbackPostInstallCleanupFiles, config.postInstallCleanupFiles)
        Assertions.assertNotSame(config.fallbackUpdateProtectedPaths, config.updateProtectedPaths)
    }
}
