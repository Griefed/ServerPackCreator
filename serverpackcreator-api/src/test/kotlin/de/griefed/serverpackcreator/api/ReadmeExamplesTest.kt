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
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Keeps [README.md](../../../../../../../README.md)'s code examples honest.
 *
 * The guide teaches roughly a dozen snippets. Prose cannot be compiled, so the same calls live here and the
 * **Kotlin compiler is the gate**: rename a member, change a return type or a parameter list, and this file
 * stops building — which is exactly what did *not* happen when `ModScanner.scan` began returning a
 * `ScanResult` instead of a file list, leaving the README teaching code that could never compile.
 *
 * Two deliberate design points:
 *  - The expensive, side-effecting snippets (`ApiWrapper.api()`, `serverPackHandler.run`) sit in functions
 *    that are **never invoked**. Compiling them is the whole point; running them would boot the API, write
 *    to the home directory and generate a pack, which is not this test's job.
 *  - Everything cheap and side-effect-free is additionally *executed* below, so the shapes the README
 *    describes (a `ScanResult` with two lists, an `Optional` required-Java) are checked, not just typed.
 *
 * When you change the README's Kotlin, change it here too — and vice versa.
 */
internal class ReadmeExamplesTest {

    private val api = ApiWrapper.api(File("src/test/resources/serverpackcreator.properties"))

    /**
     * README §2 *Quickstart* — compile-only: it would create a home directory and generate a server pack.
     * Mirrors the snippet call-for-call so a signature change breaks the build.
     */
    @Suppress("unused")
    private fun quickstartCompiles() {
        val api = ApiWrapper.api()

        val config = PackConfig().apply {
            modpackDir = "/path/to/modpack"
            minecraftVersion = "1.20.1"
            modloader = "Forge"
            modloaderVersion = "47.2.0"
            inclusions.addAll(api.configurationHandler.suggestInclusions(modpackDir))
        }

        val check = api.configurationHandler.checkConfiguration(config)
        if (!check.allChecksPassed) {
            check.encounteredErrors.forEach { println("error: $it") }
            return
        }

        val generation = api.serverPackHandler.run(config)
        if (generation.success) {
            println("Server pack: ${generation.serverPack}")
            generation.serverPackZip.ifPresent { println("ZIP: $it") }
        } else {
            generation.errors.forEach { println("failed: $it") }
        }
    }

    /** README §3 *Composition root* + §9 *Settings* — compile-only: `api()` would initialise the API. */
    @Suppress("unused")
    private fun apiWrapperAndSettingsCompile() {
        ApiWrapper.api()
        ApiWrapper.api(File("/etc/spc/serverpackcreator.properties"))
        ApiWrapper.api(File("…"), runSetup = false)

        val props = api.apiProperties
        props.homeDirectory
        props.serverPacksDirectory
        props.serverFilesDirectory
        props.isAutoExcludingModsEnabled = false
        props.startScriptTemplates
        props.javaScriptTemplates
        props.defaultStartScriptTemplates()
    }

    /** README §4 *PackConfig* + §5 *Validating* — compile-only: it reads and writes config files. */
    @Suppress("unused")
    private fun packConfigAndValidationCompile() {
        val config = PackConfig(File("serverpackcreator.conf"))
        config.save(File("serverpackcreator.conf"), api.apiProperties)

        InclusionSpecification(
            source = "mods",
            destination = null,
            inclusionFilter = null,
            exclusionFilter = null
        )

        val fromFile = PackConfig()
        api.configurationHandler.checkConfiguration(File("serverpackcreator.conf"), fromFile)
    }

    /**
     * README §8 *Scanning mods* — executed, because it is cheap and needs no network. Pins the shape the
     * README documents: `scan` takes a `Collection<File>` and returns a `ScanResult` carrying `exclusions`
     * and `dependencies`. An empty input is enough to hold the contract; the scanners themselves are covered
     * by `ModScannerTest`.
     */
    @Test
    fun scanReturnsAScanResultWithExclusionsAndDependencies() {
        val result = api.modScanner.fabricScanner.scan(emptyList())

        Assertions.assertTrue(result.exclusions.isEmpty(), "nothing to exclude from an empty scan")
        Assertions.assertTrue(result.dependencies.isEmpty(), "nothing depends on anything in an empty scan")

        // The README's usage — kept compiling *and* running.
        result.exclusions.forEach { println("${it.modId} -> ${it.excludedMod.name}") }
        result.dependencies.forEach { println("needed: ${it.identifier}") }
    }

    /**
     * README §7 *Version metadata* — executed against the cached manifests, so it is offline. Pins the
     * accessor names and, for Fabric/Quilt/LegacyFabric, the point the README makes: `latestLoader()` always
     * answers, so support is a *separate* question.
     */
    @Test
    fun versionMetadataAccessorsBehaveAsDocumented() {
        val meta = api.versionMeta

        Assertions.assertTrue(meta.minecraft.serverReleases().isNotEmpty())
        Assertions.assertTrue(meta.minecraft.requiredJavaVersion("1.20.1").isPresent)

        Assertions.assertTrue(meta.forge.newestForgeVersion("1.20.1").isPresent)
        Assertions.assertNotNull(meta.fabric.latestLoader())

        // The documented trap: a loader version exists regardless, support does not.
        Assertions.assertTrue(meta.fabric.isMinecraftSupported("1.20.1"))
        Assertions.assertTrue(meta.quilt.isMinecraftSupported("1.20.1"))
        Assertions.assertTrue(meta.legacyFabric.isMinecraftSupported("1.12.2"))
        Assertions.assertFalse(meta.legacyFabric.isMinecraftSupported("1.20.1"), "LegacyFabric is a pre-1.14 loader")
    }
}
