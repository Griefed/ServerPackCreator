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
import de.griefed.serverpackcreator.api.modscanning.Sideness
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.collections.filter

/**
 * Keeps [README.md](../../../../../../../README.md)'s code examples honest, and pins the shape of the
 * neighbouring API surface an embedder reaches for next.
 *
 * Prose cannot be compiled, so the README's calls live here too and the **Kotlin compiler is the gate**:
 * rename a member, change a return type or a parameter list, and this file stops building — which is exactly
 * what did *not* happen when `ModScanner.scan` began returning a scan result instead of a file list, leaving
 * the README teaching code that could never compile.
 *
 * Three deliberate design points:
 *  - The expensive, side-effecting snippets (`ApiWrapper.api()`, `serverPackHandler.run`) sit in functions
 *    that are **never invoked**. Compiling them is the whole point; running them would boot the API, write
 *    to the home directory and generate a pack, which is not this test's job.
 *  - Everything cheap and side-effect-free is additionally *executed* below, so the shapes are checked, not
 *    just typed.
 *  - The README currently carries only two Kotlin snippets, both under *§6 API → Example*: initialising
 *    `ApiWrapper`, and check-then-generate. The mod-scanning and version-metadata cases below are **not** in
 *    the README; they guard the adjacent surface those snippets lead an embedder into, and are marked as such
 *    rather than pretending to mirror a section. If the README grows a snippet, add it here too.
 */
internal class ReadmeExamplesTest {

    private val api = ApiWrapper.api(File("build/resources/test/serverpackcreator.properties"))

    /**
     * README *§6 API → Example* — compile-only: it would create a home directory and generate a server pack.
     * Follows the README's check-then-generate snippet, with the config built inline rather than read from a
     * file, so a signature change on any call it makes breaks the build.
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

    /**
     * The `ApiWrapper.api(...)` overloads — the README's first snippet — plus the settings an embedder reads
     * next, which the README does not show. Compile-only: `api()` would initialise the API.
     */
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

    /**
     * `PackConfig` construction, saving and validation. Not a README snippet; it guards the surface the
     * README's example depends on. Compile-only: it reads and writes config files.
     */
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
     * Mod scanning — not a README snippet; this guards the exported shape of `scan`, which an embedder
     * reaches for straight after the README's two examples. Executed, because it is cheap and needs no
     * network: `scan` takes a `Collection<File>` and returns one [ScannedMod] per input, each carrying a
     * [Sideness] and its dependencies. An empty input is enough to hold the contract; the scanners
     * themselves are covered by `ModScannerTest` and `ModScannerSidenessTest`.
     */
    @Test
    fun scanReturnsScannedModsCarryingSidenessAndDependencies() {
        val result = api.modScanner.fabricScanner.scan(emptyList())

        Assertions.assertTrue(result.none { it.sideness == Sideness.CLIENT }, "nothing to exclude from an empty scan")
        Assertions.assertTrue(result.map { it.dependencies }.flatten().isEmpty(), "nothing depends on anything in an empty scan")

        // Typical embedder usage — kept compiling *and* running.
        result.filter { it.sideness == Sideness.CLIENT }.forEach { println("${it.modID} -> ${it.file.name}") }
        result.map { it.dependencies }.flatten().forEach { println("needed: ${it.modID}") }
    }

    /**
     * Version metadata — not a README snippet either; same rationale as the scanning case above.
     * Executed against the cached manifests, so it is offline. Pins the
     * accessor names and, for Fabric/Quilt/LegacyFabric, that `latestLoader()` always
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
