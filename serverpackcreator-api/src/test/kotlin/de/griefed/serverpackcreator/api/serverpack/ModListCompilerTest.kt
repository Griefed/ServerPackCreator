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
package de.griefed.serverpackcreator.api.serverpack

import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.api.config.ExclusionFilter
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Direct unit tests for [ModListCompiler]. The class is the most branch-dense, least-covered unit
 * in the generation pipeline (clientside-only mod exclusion, whitelist overriding, per-loader and
 * per-Minecraft-version scanner selection), so it is exercised here in isolation rather than only
 * indirectly through full server pack generation.
 *
 * The mod-exclusion-filter tests run against a controlled, throwaway mods-directory with
 * auto-discovery switched off, isolating the user-specified exclusion logic. The scanner-selection
 * tests run with auto-discovery on against the real fixture modpacks; the fixture jars carry no
 * scannable metadata, so no mod is auto-excluded — what is asserted is that the correct scanner
 * branch is reached without error for every loader/version combination.
 */
internal class ModListCompilerTest {

    private val api = ApiWrapper.api(File("build/resources/test/serverpackcreator.properties"))
    private val apiProperties = api.apiProperties
    private val modListCompiler = ModListCompiler(apiProperties, api.modScanner)

    /**
     * The exclusion-toggles live on the shared, singleton [ApiWrapper] graph, so their original
     * values are snapshotted before each test and restored afterwards to keep the suite isolated.
     */
    private var originalAutoExclude = false
    private var originalFilter = ExclusionFilter.START

    @BeforeEach
    fun snapshotToggles() {
        originalAutoExclude = apiProperties.isAutoExcludingModsEnabled
        originalFilter = apiProperties.exclusionFilter
    }

    @AfterEach
    fun restoreToggles() {
        apiProperties.isAutoExcludingModsEnabled = originalAutoExclude
        apiProperties.exclusionFilter = originalFilter
    }

    /**
     * Creates a throwaway mods-directory containing exactly the three named jars and returns it.
     */
    private fun modsDirWith(parent: File, vararg modNames: String): File {
        val modsDir = File(parent, "mods")
        modsDir.mkdirs()
        for (name in modNames) {
            File(modsDir, name).writeText("dummy")
        }
        return modsDir
    }

    /**
     * Writes a real (openable) jar holding a single `fabric.mod.json` into [modsDir], so the scanner
     * has something to read. [dependencies] become the descriptor's `depends` block.
     */
    private fun fabricJar(
        modsDir: File,
        jarName: String,
        modId: String,
        environment: String,
        vararg dependencies: String
    ): File {
        val depends = dependencies.joinToString(",") { """"$it":"*"""" }
        return jarContaining(
            modsDir, jarName, "fabric.mod.json",
            """{"schemaVersion":1,"id":"$modId","version":"1.0.0",""" +
                    """"environment":"$environment","depends":{$depends}}"""
        )
    }

    /** The Quilt counterpart of [fabricJar]: a real jar holding a single `quilt.mod.json`. */
    private fun quiltJar(modsDir: File, jarName: String, modId: String, environment: String): File =
        jarContaining(
            modsDir, jarName, "quilt.mod.json",
            """{"schema_version":1,"quilt_loader":{"id":"$modId","version":"1.0.0"},""" +
                    """"minecraft":{"environment":"$environment"}}"""
        )

    /**
     * Writes a real (openable) jar holding a modern Forge `META-INF/mods.toml` into [modsDir]. The
     * mod's sideness comes from the side it demands of the platform, so [side] is put on a
     * `minecraft` dependency — which is what `ForgeTomlScanner` reads as the mod's own side.
     */
    private fun forgeTomlJar(modsDir: File, jarName: String, modId: String, side: String): File =
        jarContaining(
            modsDir, jarName, "META-INF/mods.toml",
            """
            modLoader="javafml"
            loaderVersion="[40,)"
            license="MIT"
            [[mods]]
            modId="$modId"
            version="1.0.0"
            [[dependencies.$modId]]
            modId="minecraft"
            mandatory=true
            versionRange="[1.16.5,)"
            ordering="NONE"
            side="$side"
            """.trimIndent()
        )

    /** Writes a real (openable) jar into [modsDir] holding exactly [entryPath] with [content]. */
    private fun jarContaining(modsDir: File, jarName: String, entryPath: String, content: String): File {
        val jar = File(modsDir, jarName)
        ZipOutputStream(jar.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry(entryPath))
            zip.write(content.toByteArray())
            zip.closeEntry()
        }
        return jar
    }

    /**
     * With auto-discovery disabled and no user-specified clientside mods, every jar in the
     * directory must be kept and nothing must be reported as disabled.
     */
    @Test
    fun noExclusionsKeepsEveryMod(@TempDir tempDir: File) {
        apiProperties.isAutoExcludingModsEnabled = false
        val modsDir = modsDirWith(tempDir, "clientmod.jar", "servermod.jar", "library.jar")

        val (included, disabled) = modListCompiler.compileModList(
            modsDir.absolutePath, emptyList(), emptyList(), "1.20.1", "Forge"
        )

        Assertions.assertEquals(
            setOf("clientmod.jar", "servermod.jar", "library.jar"),
            included.map { mod -> mod.name }.toSet()
        )
        Assertions.assertTrue(disabled.isEmpty(), "Nothing should be excluded; got ${disabled.map { it.name }}")
    }

    /**
     * Each [ExclusionFilter] mode must exclude exactly the mod matched by its entry and keep the
     * rest, with the excluded mod reported in the second (disabled) list. Each entry is crafted to
     * match only "clientmod.jar" among {clientmod, servermod, library}.jar.
     */
    @Test
    fun exclusionFilterModesExcludeMatchedMod(@TempDir tempDir: File) {
        val cases = mapOf(
            ExclusionFilter.START to "clientmod",
            ExclusionFilter.END to "tmod.jar",
            ExclusionFilter.CONTAIN to "client",
            ExclusionFilter.REGEX to "clientmod\\.jar",
            ExclusionFilter.EITHER to "clientmod"
        )
        apiProperties.isAutoExcludingModsEnabled = false
        for ((filter, entry) in cases) {
            apiProperties.exclusionFilter = filter
            val modsDir = modsDirWith(File(tempDir, filter.name), "clientmod.jar", "servermod.jar", "library.jar")

            val (included, disabled) = modListCompiler.compileModList(
                modsDir.absolutePath, listOf(entry), emptyList(), "1.20.1", "Forge"
            )

            val includedNames = included.map { mod -> mod.name }
            Assertions.assertFalse(includedNames.contains("clientmod.jar"), "$filter/$entry must exclude clientmod.jar")
            Assertions.assertTrue(includedNames.contains("servermod.jar"), "$filter/$entry must keep servermod.jar")
            Assertions.assertTrue(includedNames.contains("library.jar"), "$filter/$entry must keep library.jar")
            Assertions.assertTrue(
                disabled.any { mod -> mod.name == "clientmod.jar" },
                "$filter/$entry: excluded mod must be reported in the disabled list; got ${disabled.map { it.name }}"
            )
        }
    }

    /**
     * A whitelisted mod must stay in the server pack even when it also matches a clientside-only
     * exclusion entry.
     */
    @Test
    fun whitelistOverridesUserExclusion(@TempDir tempDir: File) {
        apiProperties.isAutoExcludingModsEnabled = false
        apiProperties.exclusionFilter = ExclusionFilter.CONTAIN
        val modsDir = modsDirWith(tempDir, "clientmod.jar", "servermod.jar")

        val (included, disabled) = modListCompiler.compileModList(
            modsDir.absolutePath, listOf("client"), listOf("clientmod"), "1.20.1", "Forge"
        )

        Assertions.assertTrue(
            included.map { mod -> mod.name }.contains("clientmod.jar"),
            "Whitelisted mod must be kept despite matching the exclusion entry"
        )
        Assertions.assertTrue(disabled.isEmpty(), "Whitelisted mod must not be reported as disabled")
    }

    /**
     * Already-disabled mod files keep their name; an excluded enabled mod is renamed with a
     * trailing ".disabled" by the file-gatherer — here we only pin that the compiler reports the
     * excluded file in the second list with its original name for the gatherer to rename.
     */
    @Test
    fun excludedModsAreReportedSeparately(@TempDir tempDir: File) {
        apiProperties.isAutoExcludingModsEnabled = false
        apiProperties.exclusionFilter = ExclusionFilter.START
        val modsDir = modsDirWith(tempDir, "clientmod.jar")

        val (included, disabled) = modListCompiler.compileModList(
            modsDir.absolutePath, listOf("clientmod"), emptyList(), "1.20.1", "Forge"
        )

        Assertions.assertTrue(included.isEmpty())
        Assertions.assertEquals(listOf("clientmod.jar"), disabled.map { mod -> mod.name })
    }

    /**
     * With auto-discovery enabled, every loader/Minecraft-version combination must reach its
     * scanner branch (Forge annotation vs toml at the MC 1.12/1.13 boundary, NeoForge dedicated vs
     * Forge scanner at the 1.20.5 boundary, Fabric, LegacyFabric, Quilt) without error. Regardless
     * of how many mods each scanner flags as clientside, the returned lists must together partition
     * every jar in the directory exactly once (included + disabled == total).
     */
    @Test
    fun autoDiscoveryReachesScannerBranchPerLoader() {
        // fixture, Minecraft version, modloader — one per arm of the scanner-selection `when`.
        val cases = listOf(
            Triple("forge_tests", "1.12.2", "Forge"),            // Forge annotation scanner (MC minor <= 12)
            Triple("forge_tests", "1.16.5", "Forge"),            // Forge toml scanner (MC minor > 12)
            Triple("neoforge_tests", "1.20.1", "NeoForge"),      // NeoForge < 1.20.5 -> Forge toml scanner
            Triple("neoforge_tests", "1.21.1", "NeoForge"),      // NeoForge >= 1.20.5 -> NeoForge scanner
            Triple("fabric_tests", "1.20.1", "Fabric"),          // Fabric scanner
            Triple("legacyfabric_tests", "1.12.2", "LegacyFabric"), // Fabric scanner via LegacyFabric arm
            Triple("quilt_tests", "1.20.1", "Quilt")             // Fabric + Quilt scanner union
        )
        apiProperties.isAutoExcludingModsEnabled = true
        for ((fixture, minecraftVersion, modloader) in cases) {
            val modsDir = File("src/test/resources/$fixture/mods")
            val totalJars = modsDir.listFiles { file -> file.extension == "jar" || file.extension == "disabled" }!!.size

            val (included, disabled) = modListCompiler.compileModList(
                modsDir.absolutePath, emptyList(), emptyList(), minecraftVersion, modloader
            )

            Assertions.assertEquals(
                totalJars, included.size + disabled.size,
                "$modloader/$minecraftVersion: every jar must be partitioned into included or disabled exactly once"
            )
        }
    }

    /**
     * Forge's scanner is chosen by Minecraft *era*, and Minecraft has two versioning schemes
     * (`1.x.y` and the newer `YY.x.y`), so the choice must not be made from the minor component
     * alone: `26.2`'s minor is `2`, which reads as the 1.2 era and would send a modern pack to the
     * annotation scanner meant for 1.12-and-older.
     *
     * The failure is silent — the annotation scanner finds no `fml_cache_annotation.json` in a
     * modern jar, falls back to the never-drop-a-jar default of SERVER, and auto-exclusion quietly
     * stops working — so this pins the *outcome*: a jar whose `mods.toml` declares CLIENT must be
     * auto-excluded on both schemes, and its SERVER counterpart must survive on both.
     */
    @Test
    fun forgeScannerSelectionSpansBothMinecraftVersioningSchemes(@TempDir tempDir: File) {
        apiProperties.isAutoExcludingModsEnabled = true

        for (minecraftVersion in listOf("1.20.1", "26.2")) {
            val modsDir = File(tempDir, minecraftVersion).also { it.mkdirs() }
            forgeTomlJar(modsDir, "clientonly.jar", "clientonlymod", "CLIENT")
            forgeTomlJar(modsDir, "serverside.jar", "serversidemod", "BOTH")

            val (included, disabled) = modListCompiler.compileModList(
                modsDir.absolutePath, emptyList(), emptyList(), minecraftVersion, "Forge"
            )

            Assertions.assertEquals(
                listOf("clientonly.jar"), disabled.map { it.name },
                "Minecraft $minecraftVersion: the CLIENT-declaring mods.toml jar must be auto-excluded"
            )
            Assertions.assertEquals(
                listOf("serverside.jar"), included.map { it.name },
                "Minecraft $minecraftVersion: the BOTH-declaring mods.toml jar must be kept"
            )
        }
    }

    /**
     * A mod the scanner auto-detects as clientside-only must be rescued and kept in the server pack
     * when it is whitelisted. The detected mod is discovered dynamically (first run with an empty
     * whitelist) so the assertion does not depend on specific fixture jar names.
     */
    @Test
    fun whitelistRescuesAutoDiscoveredClientsideMod() {
        apiProperties.isAutoExcludingModsEnabled = true
        apiProperties.exclusionFilter = ExclusionFilter.CONTAIN
        val modsDir = File("src/test/resources/forge_tests/mods").absolutePath

        val (_, autoExcluded) = modListCompiler.compileModList(modsDir, emptyList(), emptyList(), "1.16.5", "Forge")
        Assertions.assertTrue(
            autoExcluded.isNotEmpty(),
            "Auto-discovery must detect at least one clientside mod in forge_tests for this test to mean anything. " +
                    "An empty list here means auto-exclusion stopped working, not that the fixture is unsuitable."
        )
        val rescuedName = autoExcluded.first().name

        val (included, disabled) = modListCompiler.compileModList(modsDir, emptyList(), listOf(rescuedName), "1.16.5", "Forge")

        Assertions.assertTrue(
            included.any { mod -> mod.name == rescuedName },
            "Whitelisted mod '$rescuedName' must be rescued from auto-exclusion"
        )
        Assertions.assertFalse(
            disabled.any { mod -> mod.name == rescuedName },
            "Whitelisted mod '$rescuedName' must not be reported as disabled"
        )
    }

    /**
     * With auto-discovery on and **no** user-specified clientside mods, a mod the scanner judged
     * clientside must stay disabled. This is the case auto-detection exists for, and the one the
     * user-exclusion pass is most likely to undo: that pass walks every scanned mod again, so an
     * `else` branch that re-enables everything it did not itself match silently defeats the whole
     * feature while leaving the suite green.
     *
     * `aaaaa.jar` is asserted clientside at the scanner level by
     * [de.griefed.serverpackcreator.api.modscanning.ModScannerTest.tomlTest]; `ddddd.jar` is
     * asserted not clientside there. Pinning both directions keeps this honest if the fixture changes.
     */
    @Test
    fun autoDetectedClientsideModsStayDisabledWithoutUserExclusions() {
        apiProperties.isAutoExcludingModsEnabled = true
        apiProperties.exclusionFilter = ExclusionFilter.CONTAIN
        val modsDir = File("src/test/resources/forge_tests/mods").absolutePath

        val (included, disabled) = modListCompiler.compileModList(
            modsDir, emptyList(), emptyList(), "1.16.5", "Forge"
        )
        val includedNames = included.map { mod -> mod.name }
        val disabledNames = disabled.map { mod -> mod.name }

        Assertions.assertTrue(
            disabledNames.contains("aaaaa.jar"),
            "Scanner-detected clientside mod must remain disabled with no user exclusions; disabled=$disabledNames"
        )
        Assertions.assertFalse(
            includedNames.contains("aaaaa.jar"),
            "Scanner-detected clientside mod must not be included; included=$includedNames"
        )
        Assertions.assertTrue(
            includedNames.contains("ddddd.jar"),
            "A mod the scanner judged server-side must still be included; included=$includedNames"
        )
    }

    /**
     * The Quilt arm scans the same directory twice — once per descriptor format — and merges the two
     * result sets. The merge must be keyed on the jar, not on the declared mod id: a jar can declare
     * *different* ids in `quilt.mod.json` and `fabric.mod.json` (the fixture's `aaaaa.jar` declares
     * `ok_zoomer` and `ok_zoomer-pmw`), and a jar carrying only one descriptor falls back to a
     * synthesised id for the scan that failed. Keying on the id therefore fails to match exactly the
     * entries being merged, yielding two entries for one file.
     *
     * Two entries for one file can carry different sideness, which puts the same jar in *both*
     * returned lists. The partition check in [autoDiscoveryReachesScannerBranchPerLoader] cannot
     * catch that — each list is de-duplicated separately before being returned — so disjointness is
     * asserted explicitly here.
     */
    @Test
    fun quiltArmReturnsEachJarExactlyOnce() {
        apiProperties.isAutoExcludingModsEnabled = true
        apiProperties.exclusionFilter = ExclusionFilter.CONTAIN
        val modsDir = File("src/test/resources/quilt_tests/mods")

        val (included, disabled) = modListCompiler.compileModList(
            modsDir.absolutePath, emptyList(), emptyList(), "1.20.1", "Quilt"
        )
        val includedNames = included.map { mod -> mod.name }
        val disabledNames = disabled.map { mod -> mod.name }

        val inBoth = includedNames.intersect(disabledNames.toSet())
        Assertions.assertTrue(
            inBoth.isEmpty(),
            "A jar must not be both included and disabled; both=$inBoth, included=$includedNames, disabled=$disabledNames"
        )
        Assertions.assertEquals(
            includedNames.size, includedNames.distinct().size,
            "Included list must hold no duplicate jar; got $includedNames"
        )
        Assertions.assertEquals(
            disabledNames.size, disabledNames.distinct().size,
            "Disabled list must hold no duplicate jar; got $disabledNames"
        )
        Assertions.assertEquals(
            modsDir.listFiles { file -> file.extension == "jar" }!!.size,
            includedNames.size + disabledNames.size,
            "Every jar must appear exactly once across the two lists"
        )
    }

    /**
     * An unrecognised modloader must still yield every mod, not an empty pack.
     *
     * The scanner-selection `when` has an arm per supported loader and no `else`, and the
     * include-list is built solely from what a scanner returned — so a loader string matching no arm
     * leaves nothing scanned and returns two empty lists. Before the modscan rewrite the list was
     * seeded with every file and exclusions were removed from it, so the same input returned every
     * jar.
     *
     * This is reachable without any embedder doing something exotic: [PackConfig.modloader]'s setter
     * silently ignores a value it does not recognise, leaving the field at its initial empty string,
     * and that empty string reaches this `when`. An over-full pack is something a user can fix; a
     * silently empty one looks like the tool did nothing.
     */
    @Test
    fun unrecognisedModloaderStillYieldsEveryMod(@TempDir tempDir: File) {
        apiProperties.isAutoExcludingModsEnabled = true
        val modsDir = modsDirWith(tempDir, "alpha.jar", "beta.jar", "gamma.jar")

        val (included, disabled) = modListCompiler.compileModList(
            modsDir.absolutePath, emptyList(), emptyList(), "1.20.1", "NotAModloader"
        )

        Assertions.assertEquals(
            setOf("alpha.jar", "beta.jar", "gamma.jar"), included.map { mod -> mod.name }.toSet(),
            "An unrecognised modloader must fall back to including every mod, not to an empty pack"
        )
        Assertions.assertTrue(
            disabled.isEmpty(),
            "Nothing can be judged clientside without a scanner; got ${disabled.map { it.name }}"
        )
    }

    /**
     * A mod the scanner judged clientside must be rescued when something kept on the server depends
     * on it. Excluding a dependency produces a pack that installs and then dies on load, which is
     * worse than shipping one mod too many — so a dependency wins over a clientside verdict.
     *
     * This could not fire until now: the rescue additionally required the *disabled* mod to be
     * `Sideness.SERVER`, but a mod auto-disabled by a scanner is `CLIENT` by construction, so the
     * protection never reached the population it was written for. Removing that clause is what this
     * pins.
     */
    @Test
    fun aClientsideModDependedOnByAServerModIsRescued(@TempDir tempDir: File) {
        apiProperties.isAutoExcludingModsEnabled = true
        val modsDir = File(tempDir, "mods").apply { mkdirs() }
        fabricJar(modsDir, "servermod.jar", "servermod", "*", "clientlib")
        fabricJar(modsDir, "clientlib.jar", "clientlib", "client")

        val (included, disabled) = modListCompiler.compileModList(
            modsDir.absolutePath, emptyList(), emptyList(), "1.20.1", "Fabric"
        )

        Assertions.assertTrue(
            included.map { mod -> mod.name }.contains("clientlib.jar"),
            "A clientside mod that a server mod depends on must be kept; included=${included.map { it.name }}"
        )
        Assertions.assertTrue(
            disabled.isEmpty(),
            "The rescued dependency must not also be reported as disabled; got ${disabled.map { it.name }}"
        )
    }

    /**
     * The majority of a real Quilt pack is Fabric mods carrying no `quilt.mod.json`, and the
     * committed `quilt_tests` fixture contains no such jar. This covers it: a fabric-only mod must
     * get the verdict from the Fabric scan — the Quilt scan cannot read it and falls back to SERVER —
     * and must still appear exactly once.
     *
     * Guards the removal of the copy-loop that used to sit at the end of the Quilt arm. That loop
     * cannot fire (both scanners return one entry per input file, so the lookup always matches), and
     * the case it looks like it handles is this one, which the sideness-merge above it covers.
     */
    @Test
    fun theQuiltArmTakesTheFabricVerdictForAFabricOnlyJar(@TempDir tempDir: File) {
        apiProperties.isAutoExcludingModsEnabled = true
        val modsDir = File(tempDir, "mods").apply { mkdirs() }
        quiltJar(modsDir, "quiltmod.jar", "quiltmod", "*")
        fabricJar(modsDir, "fabriconly.jar", "fabriconly", "client")

        val (included, disabled) = modListCompiler.compileModList(
            modsDir.absolutePath, emptyList(), emptyList(), "1.20.1", "Quilt"
        )

        Assertions.assertEquals(
            listOf("fabriconly.jar"), disabled.map { mod -> mod.name },
            "A fabric-only clientside mod must be disabled on the Quilt arm"
        )
        Assertions.assertEquals(
            listOf("quiltmod.jar"), included.map { mod -> mod.name },
            "The quilt-only server mod must be kept"
        )
    }

    /**
     * The rescue is transitive, which is what the surrounding `while` loop exists for: rescuing one
     * mod puts its own dependencies in play, and those may themselves sit in the disabled list. A
     * single pass would keep `deeplib` excluded and still look like it had done its job.
     */
    @Test
    fun theDependencyRescueFollowsAChain(@TempDir tempDir: File) {
        apiProperties.isAutoExcludingModsEnabled = true
        val modsDir = File(tempDir, "mods").apply { mkdirs() }
        fabricJar(modsDir, "servermod.jar", "servermod", "*", "midlib")
        fabricJar(modsDir, "midlib.jar", "midlib", "client", "deeplib")
        fabricJar(modsDir, "deeplib.jar", "deeplib", "client")

        val (included, disabled) = modListCompiler.compileModList(
            modsDir.absolutePath, emptyList(), emptyList(), "1.20.1", "Fabric"
        )

        Assertions.assertEquals(
            setOf("servermod.jar", "midlib.jar", "deeplib.jar"), included.map { mod -> mod.name }.toSet(),
            "The whole dependency chain must be rescued, not just its first link"
        )
        Assertions.assertTrue(disabled.isEmpty(), "Nothing in the chain may stay disabled; got ${disabled.map { it.name }}")
    }
}
