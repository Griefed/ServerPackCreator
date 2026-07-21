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
package de.griefed.serverpackcreator.grinder

import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.api.config.PackConfig
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import java.util.Optional

/**
 * Generates a **mod-less** server pack for a `(loader, loaderVersion, minecraftVersion)` tuple — the
 * input to the one-off install boot whose result the [LoaderCache] snapshots. Empty mods on purpose:
 * the install layer (`libraries/` + loader jars) depends only on the tuple, not on any mod.
 *
 * @author Griefed
 */
fun interface VanillaPackGenerator {
    /** Generate the empty pack into a fresh directory, or `null` if config-check / generation failed. */
    fun generate(loader: String, loaderVersion: String, minecraftVersion: String): File?
}

/**
 * [VanillaPackGenerator] backed by SPC's own [ApiWrapper] generation — the same path
 * `BootVerifier.prepareBootPack` uses, minus any mod. Integration-only (a real `ApiWrapper`, with its
 * cached version metadata, is required); not unit-tested.
 *
 * @param apiWrapper     SPC generation + config + version metadata.
 * @param workDirectory  Scratch root; each tuple gets a fresh sub-directory.
 * @author Griefed
 */
class ApiVanillaPackGenerator(
    private val apiWrapper: ApiWrapper,
    private val workDirectory: File
) : VanillaPackGenerator {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

    override fun generate(loader: String, loaderVersion: String, minecraftVersion: String): File? {
        val tupleDir = File(workDirectory, sanitize("$minecraftVersion-$loader-$loaderVersion")).apply { deleteRecursively() }
        val modpack = File(tupleDir, "modpack").apply { File(this, "mods").mkdirs() }
        // SPC refuses to generate an "empty" pack, so give the (mod-less) modpack a minimal includable
        // directory. The install only needs start.sh + the loader; this placeholder is harmless and is
        // excluded from the install-layer snapshot (it's a pre-boot file).
        File(modpack, "config").mkdirs()
        File(modpack, "config/.spc-grinder-keep").writeText("Placeholder so the vanilla install pack is not empty.\n")

        // Keep generation faithful to the boot path: no scanner-driven exclusion, empty clientside-list.
        apiWrapper.apiProperties.isAutoExcludingModsEnabled = false
        val packConfig = PackConfig().apply {
            modpackDir = modpack.absolutePath
            this.minecraftVersion = minecraftVersion
            modloader = loader
            modloaderVersion = loaderVersion
            clientMods.clear()
            inclusions.clear()
            inclusions.addAll(apiWrapper.configurationHandler.suggestInclusions(modpack.absolutePath))
            customDestination = Optional.of(File(tupleDir, "serverpack"))
        }

        val check = apiWrapper.configurationHandler.checkConfiguration(packConfig)
        if (!check.allChecksPassed) {
            log.warn("Vanilla config-check failed for $loader $loaderVersion / $minecraftVersion: ${check.encounteredErrors}")
            return null
        }
        val generation = apiWrapper.serverPackHandler.run(packConfig)
        if (!generation.success) {
            log.warn("Vanilla generation failed for $loader $loaderVersion / $minecraftVersion: ${generation.errors}")
            return null
        }
        return generation.serverPack
    }

    private fun sanitize(token: String): String = token.replace(Regex("[^A-Za-z0-9._-]"), "_")
}
