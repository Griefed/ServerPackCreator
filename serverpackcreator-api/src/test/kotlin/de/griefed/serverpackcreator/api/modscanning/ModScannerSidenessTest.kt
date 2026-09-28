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
package de.griefed.serverpackcreator.api.modscanning

import de.griefed.serverpackcreator.api.ApiWrapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Sideness and dependency pins for the mod scanners, driven by descriptors written inline rather
 * than by the committed fixture jars.
 *
 * The fixtures under `src/test/resources/<loader>_tests` are real-world captures and stay that way — their
 * value is the messiness only a real mod produces. What they cannot express is the *absence* of a
 * field: every fabric and quilt descriptor among them declares an `environment`, so the default a
 * scanner falls back to when one is missing had no coverage at all. That default is a decision, not
 * an accident — a mod that does not declare its side is assumed server-side so it is never dropped
 * from a pack — and it regressed once already.
 *
 * Each case therefore builds a real jar in a [TempDir] containing exactly one descriptor, so the
 * JSON under test is visible in the diff and no binary enters the repository.
 */
internal class ModScannerSidenessTest {

    private val modScanner = ApiWrapper.api(File("build/resources/test/serverpackcreator.properties")).modScanner

    /**
     * Writes a real (openable) jar into [directory] containing exactly one entry at [entryPath] with
     * [content], and returns it. A genuine archive is required because the scanners open these with
     * `JarFile`; a plain text file with a `.jar` name exercises the failure branch instead.
     */
    private fun jarContaining(directory: File, jarName: String, entryPath: String, content: String): File {
        val jar = File(directory, jarName)
        ZipOutputStream(jar.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry(entryPath))
            zip.write(content.toByteArray())
            zip.closeEntry()
        }
        return jar
    }

    /** A `fabric.mod.json` declaring [modId], with an `environment` line only when [environment] is given. */
    private fun fabricDescriptor(modId: String, environment: String?) = buildString {
        append("""{"schemaVersion":1,"id":"$modId","version":"1.0.0"""")
        if (environment != null) {
            append(""","environment":"$environment"""")
        }
        append("}")
    }

    /** A `quilt.mod.json` declaring [modId], with a `minecraft.environment` block only when [environment] is given. */
    private fun quiltDescriptor(modId: String, environment: String?) = buildString {
        append("""{"schema_version":1,"quilt_loader":{"id":"$modId","version":"1.0.0"}""")
        if (environment != null) {
            append(""","minecraft":{"environment":"$environment"}""")
        }
        append("}")
    }

    /**
     * A `fabric.mod.json` that declares no `environment` at all must be judged SERVER. Dropping such
     * a mod from a server pack breaks it, so the absent field has to mean "keep", not "unknown".
     */
    @Test
    fun fabricModWithoutAnEnvironmentIsServerSide(@TempDir tempDir: File) {
        val jar = jarContaining(tempDir, "no-env.jar", "fabric.mod.json", fabricDescriptor("noenv", null))

        val scanned = modScanner.fabricScanner.scan(listOf(jar)).single()

        Assertions.assertEquals(Sideness.SERVER, scanned.sideness, "A Fabric mod declaring no environment must be SERVER")
        Assertions.assertEquals("noenv", scanned.modID, "The declared id must be read even with no environment block")
    }

    /** The two `environment` values a Fabric descriptor can carry, pinned so the default above reads as a default. */
    @Test
    fun fabricEnvironmentDecidesSideness(@TempDir tempDir: File) {
        val clientJar = jarContaining(tempDir, "client.jar", "fabric.mod.json", fabricDescriptor("clientmod", "client"))
        val bothJar = jarContaining(tempDir, "both.jar", "fabric.mod.json", fabricDescriptor("bothmod", "*"))

        val scanned = modScanner.fabricScanner.scan(listOf(clientJar, bothJar)).associateBy { it.modID }

        Assertions.assertEquals(Sideness.CLIENT, scanned.getValue("clientmod").sideness, "environment=client must be CLIENT")
        Assertions.assertEquals(Sideness.SERVER, scanned.getValue("bothmod").sideness, "environment=* must be SERVER")
    }

    /**
     * A `quilt.mod.json` with no `minecraft.environment` must be judged SERVER, exactly as the Fabric
     * scanner does for the same omission.
     *
     * This regressed: the rewrite left the Quilt scanner's catch-block adding nothing to its list of
     * sidenesses, and an empty list falls through to CLIENT — so a Quilt mod that simply did not
     * declare an environment was excluded from the pack. Fixed in bf226c2ac, unpinned until now.
     */
    @Test
    fun quiltModWithoutAnEnvironmentIsServerSide(@TempDir tempDir: File) {
        val jar = jarContaining(tempDir, "no-env.jar", "quilt.mod.json", quiltDescriptor("quiltnoenv", null))

        val scanned = modScanner.quiltScanner.scan(listOf(jar)).single()

        Assertions.assertEquals(Sideness.SERVER, scanned.sideness, "A Quilt mod declaring no environment must be SERVER")
        Assertions.assertEquals("quiltnoenv", scanned.modID, "The declared quilt_loader id must be read")
    }

    /** The Quilt counterpart of [fabricEnvironmentDecidesSideness], so its default is pinned as a default too. */
    @Test
    fun quiltEnvironmentDecidesSideness(@TempDir tempDir: File) {
        val clientJar = jarContaining(tempDir, "client.jar", "quilt.mod.json", quiltDescriptor("quiltclient", "client"))
        val bothJar = jarContaining(tempDir, "both.jar", "quilt.mod.json", quiltDescriptor("quiltboth", "*"))

        val scanned = modScanner.quiltScanner.scan(listOf(clientJar, bothJar)).associateBy { it.modID }

        Assertions.assertEquals(Sideness.CLIENT, scanned.getValue("quiltclient").sideness, "environment=client must be CLIENT")
        Assertions.assertEquals(Sideness.SERVER, scanned.getValue("quiltboth").sideness, "environment=* must be SERVER")
    }

    /**
     * Declared Fabric dependencies must be recorded, and the platform itself must not be: the loader,
     * Java and Minecraft are always present on a server, so treating them as mod dependencies would
     * make every mod look like it depends on something that needs keeping.
     *
     * `fabric-api-base` is included on purpose. The exclusion regex is an exact match, not a prefix,
     * so a real mod whose id merely *starts with* `fabric` must survive it — pinned here because a
     * regex loosened to `fabric.*` would silently drop genuine dependencies and still look right.
     */
    @Test
    fun fabricDependenciesAreRecordedWithoutThePlatform(@TempDir tempDir: File) {
        val descriptor = """
            {"schemaVersion":1,"id":"withdeps","version":"1.0.0","environment":"*",
             "depends":{"creativecore":"*","fabricloader":">=0.14","minecraft":"1.20.1",
                        "java":">=17","fabric":"*","fabric-api-base":"*"}}
        """.trimIndent()
        val jar = jarContaining(tempDir, "withdeps.jar", "fabric.mod.json", descriptor)

        val dependencies = modScanner.fabricScanner.scan(listOf(jar)).single().dependencies.map { it.modID }

        Assertions.assertEquals(
            listOf("creativecore", "fabric", "fabric-api-base"), dependencies,
            "The platform is `fabricloader`; `fabric` is Fabric API, a mod the server needs; got $dependencies"
        )
    }

    /**
     * **`fabric` is Fabric API, not the platform.** The exclusion list conflated them, and its own doc
     * comment says it holds "ids that are the platform rather than a mod" — `fabricloader` is the platform,
     * while `fabric` is the single most-depended-on *mod* in the ecosystem and is genuinely required on a
     * server by the mods that declare it. Dropping it meant Fabric API could never be reported as the
     * dependency it is, and could never be rescued back into a pack that had disabled it.
     */
    @Test
    fun fabricApiIsADependencyAndCarriesItsVersionRange(@TempDir tempDir: File) {
        val descriptor = """
            {"schemaVersion":1,"id":"needsapi","version":"1.0.0","environment":"*",
             "depends":{"fabric":">=0.92.0","fabricloader":">=0.14","minecraft":"1.20.1","java":">=17"}}
        """.trimIndent()
        val jar = jarContaining(tempDir, "needsapi.jar", "fabric.mod.json", descriptor)

        val dependency = modScanner.fabricScanner.scan(listOf(jar)).single().dependencies.single()

        Assertions.assertEquals("fabric", dependency.modID)
        Assertions.assertEquals(">=0.92.0", dependency.versionConstraint, "the declared range must be carried verbatim")
    }

    /**
     * The Quilt half of the same bug, and the one that matters for a Quilt mod: `quilt_loader` is the
     * platform, but `quilted_fabric_api` is QFAPI — Quilt's port of Fabric API, and just as much a mod the
     * server needs.
     *
     * **`quilt_base` is a mod too, and used to be excluded as though it were the platform.** It is QSL's
     * base module, shipped by QFAPI: `library/core/qsl_base` in `QuiltMC/quilt-standard-libraries`, whose
     * own `quilt_base_testmod` declares `["quilt_loader", "quilt_base"]` (read 2026-09-01, branch 1.21.5).
     * Excluding it was the exact mistake this test's Fabric counterpart exists to prevent — `fabric` (the
     * API) is not excluded there, only `fabricloader` — so Quilt now matches: loader out, modules in.
     */
    @Test
    fun quiltedFabricApiIsADependencyRatherThanThePlatform(@TempDir tempDir: File) {
        val descriptor = """
            {"schema_version":1,
             "quilt_loader":{"id":"needsqfapi","version":"1.0.0",
               "depends":[{"id":"quilted_fabric_api","versions":">=7.0.0"},"quilt_base","minecraft","java"]},
             "minecraft":{"environment":"*"}}
        """.trimIndent()
        val jar = jarContaining(tempDir, "needsqfapi.jar", "quilt.mod.json", descriptor)

        val dependencies = modScanner.quiltScanner.scan(listOf(jar)).single().dependencies

        Assertions.assertEquals(
            listOf("quilted_fabric_api", "quilt_base"), dependencies.map { it.modID },
            "QFAPI and QSL's base module are both mods; only quilt_loader/minecraft/java are the platform"
        )
        val qfapi = dependencies.first { it.modID == "quilted_fabric_api" }
        Assertions.assertEquals(">=7.0.0", qfapi.versionConstraint)
    }

    /**
     * The line the exclusion list actually draws, stated on its own so it cannot be inferred from a fixture
     * that happens to list one of each. `quilt_loader` is the runtime; everything QSL ships is a mod that a
     * server pack has to keep, and a jar providing it must stay rescuable.
     */
    @Test
    fun onlyTheQuiltRuntimeIsExcludedFromDependencies(@TempDir tempDir: File) {
        val descriptor = """
            {"schema_version":1,
             "quilt_loader":{"id":"qsluser","version":"1.0.0",
               "depends":["quilt_loader","minecraft","java","quilt_base","quilt_resource_loader"]},
             "minecraft":{"environment":"*"}}
        """.trimIndent()
        val jar = jarContaining(tempDir, "qsluser.jar", "quilt.mod.json", descriptor)

        Assertions.assertEquals(
            listOf("quilt_base", "quilt_resource_loader"),
            modScanner.quiltScanner.scan(listOf(jar)).single().dependencies.map { it.modID }
        )
    }

    /** Forge and NeoForge state a `versionRange` per dependency; it has to survive the scan too. */
    @Test
    fun forgeDependenciesCarryTheirDeclaredVersionRange(@TempDir tempDir: File) {
        val toml = """
            modLoader="javafml"
            loaderVersion="[40,)"
            license="MIT"
            [[mods]]
            modId="needsjei"
            [[dependencies.needsjei]]
            modId="minecraft"
            side="BOTH"
            versionRange="[1.20.1]"
            [[dependencies.needsjei]]
            modId="jei"
            side="BOTH"
            versionRange="[15.2.0.27,)"
        """.trimIndent()
        val jar = jarContaining(tempDir, "needsjei.jar", "META-INF/mods.toml", toml)

        val dependency = modScanner.forgeTomlScanner.scan(listOf(jar)).single().dependencies.single()

        Assertions.assertEquals("jei", dependency.modID)
        Assertions.assertEquals("[15.2.0.27,)", dependency.versionConstraint)
    }

    /** A dependency that states no range keeps a null constraint rather than an invented one. */
    @Test
    fun aBareQuiltDependencyCarriesNoConstraint(@TempDir tempDir: File) {
        val descriptor = """
            {"schema_version":1,
             "quilt_loader":{"id":"bare","version":"1.0.0","depends":["cloth-config2"]},
             "minecraft":{"environment":"*"}}
        """.trimIndent()
        val jar = jarContaining(tempDir, "bare.jar", "quilt.mod.json", descriptor)

        Assertions.assertNull(modScanner.quiltScanner.scan(listOf(jar)).single().dependencies.single().versionConstraint)
    }

    /**
     * The Quilt counterpart. A `quilt_loader.depends` entry is either a bare string or an object
     * carrying an `id`, and both forms must be read — the fixture only exercises one of them.
     */
    @Test
    fun quiltDependenciesAreRecordedInBothDeclarationForms(@TempDir tempDir: File) {
        val descriptor = """
            {"schema_version":1,
             "quilt_loader":{"id":"quiltdeps","version":"1.0.0",
               "depends":["cloth-config2",{"id":"jei","versions":"*"},"quilt_base","minecraft","java"]},
             "minecraft":{"environment":"*"}}
        """.trimIndent()
        val jar = jarContaining(tempDir, "quiltdeps.jar", "quilt.mod.json", descriptor)

        val dependencies = modScanner.quiltScanner.scan(listOf(jar)).single().dependencies.map { it.modID }

        Assertions.assertEquals(
            listOf("cloth-config2", "jei", "quilt_base"), dependencies,
            "Both the string and the object form must be read, minus the platform; got $dependencies"
        )
        Assertions.assertEquals(
            listOf(null, "*", null),
            modScanner.quiltScanner.scan(listOf(jar)).single().dependencies.map { it.versionConstraint },
            "the object form states a range and the bare strings do not"
        )
    }

    /**
     * Forge declares no sideness of its own — a mod's side is inferred from the `side` it demands of
     * the *platform* dependency (`minecraft`/`forge`/`neoforge`). A mod that asks for `minecraft` on
     * `CLIENT` is a client mod; anything else keeps it on the server. Non-platform dependencies are
     * recorded instead of consumed.
     *
     * This inference is the least obvious rule in the scanners and had no direct test: the fixture
     * assertions only state which jars come out clientside, not why.
     */
    @Test
    fun forgeSidenessComesFromThePlatformDependencySide(@TempDir tempDir: File) {
        fun modsToml(modId: String, minecraftSide: String) = """
            modLoader="javafml"
            loaderVersion="[40,)"
            license="MIT"

            [[mods]]
            modId="$modId"
            version="1.0.0"

            [[dependencies.$modId]]
            modId="minecraft"
            mandatory=true
            versionRange="[1.19,)"
            side="$minecraftSide"

            [[dependencies.$modId]]
            modId="jei"
            mandatory=true
            versionRange="[1.0,)"
            side="BOTH"
        """.trimIndent()

        val clientJar = jarContaining(tempDir, "clientside.jar", "META-INF/mods.toml", modsToml("clientmod", "CLIENT"))
        val bothJar = jarContaining(tempDir, "bothside.jar", "META-INF/mods.toml", modsToml("bothmod", "BOTH"))

        val scanned = modScanner.forgeTomlScanner.scan(listOf(clientJar, bothJar)).associateBy { it.modID }

        Assertions.assertEquals(
            Sideness.CLIENT, scanned.getValue("clientmod").sideness,
            "A mod demanding minecraft on CLIENT must be clientside"
        )
        Assertions.assertEquals(
            Sideness.SERVER, scanned.getValue("bothmod").sideness,
            "A mod demanding minecraft on BOTH must be kept on the server"
        )
        Assertions.assertEquals(
            listOf("jei"), scanned.getValue("bothmod").dependencies.map { it.modID },
            "The non-platform dependency must be recorded rather than consumed as a sideness signal"
        )
    }

    /**
     * A jar the scanner cannot read at all — a truncated download, a non-archive with a `.jar` name —
     * must still come back as SERVER, so an unreadable file is kept rather than silently dropped from
     * the pack, and must carry the filename as its id.
     *
     * The id matters beyond diagnostics: it is what downstream matching joins on. It was `"N/A"` for
     * every unreadable jar until 2ec5ff202, which meant any two of them compared equal and matched
     * each other in the dependency lookup. The filename is not a real mod id, and nothing in the type
     * says so, which is why it is pinned here.
     */
    @Test
    fun anUnreadableJarIsServerSideAndCarriesItsFilenameAsId(@TempDir tempDir: File) {
        val notAnArchive = File(tempDir, "brokenmod.jar").apply { writeText("this is not a zip") }

        val scanned = modScanner.fabricScanner.scan(listOf(notAnArchive)).single()

        Assertions.assertEquals(Sideness.SERVER, scanned.sideness, "An unreadable jar must be kept, i.e. SERVER")
        Assertions.assertEquals(
            "brokenmod", scanned.modID,
            "An unreadable jar must fall back to its filename as id, not to a shared placeholder"
        )
    }

    /**
     * A scan must return one entry per input file, whatever the outcome. The compiler builds its
     * include-list solely from what the scanners hand back, so a jar dropped mid-scan is a jar
     * missing from the server pack — and a jar entered twice is one that can land in both the
     * included and the disabled list.
     */
    @Test
    fun everyJarYieldsExactlyOneEntryWhateverTheOutcome(@TempDir tempDir: File) {
        val readable = jarContaining(tempDir, "readable.jar", "fabric.mod.json", fabricDescriptor("readable", "*"))
        val noDescriptor = jarContaining(tempDir, "nodescriptor.jar", "META-INF/MANIFEST.MF", "Manifest-Version: 1.0\n")
        val unreadable = File(tempDir, "unreadable.jar").apply { writeText("not a zip") }
        val input = listOf(readable, noDescriptor, unreadable)

        val scanned = modScanner.fabricScanner.scan(input)

        Assertions.assertEquals(
            input.map { it.name }.sorted(), scanned.map { it.file.name }.sorted(),
            "The scan must return exactly one entry per input jar"
        )
        Assertions.assertEquals(
            Sideness.SERVER, scanned.single { it.file.name == "nodescriptor.jar" }.sideness,
            "A readable jar carrying no fabric.mod.json must be kept, i.e. SERVER"
        )
    }

    /**
     * A jar carrying nothing for this scanner must be distinguishable from a jar that failed to be
     * read, because that distinction is what decides whether the operator sees a DEBUG line or an
     * ERROR with a stack trace.
     *
     * It is the common case, not the exception: every scanner is handed the whole mods-directory, and
     * a Quilt pack is deliberately scanned by both the Quilt and the Fabric scanner, so one of the two
     * finds nothing in every single-format jar. A corrupt archive must still be loud.
     */
    @Test
    fun anAbsentDescriptorIsDistinguishedFromAnUnreadableJar(@TempDir tempDir: File) {
        val wrongLoader = jarContaining(tempDir, "fabriconly.jar", "fabric.mod.json", fabricDescriptor("fabriconly", "*"))
        val corrupt = File(tempDir, "corrupt.jar").apply { writeText("not a zip") }

        Assertions.assertThrows(MissingDescriptorException::class.java, {
            modScanner.forgeTomlScanner.read(wrongLoader)
        }, "A jar with no mods.toml must report the descriptor as missing, not as a failure")

        Assertions.assertFalse(
            runCatching { modScanner.forgeTomlScanner.read(corrupt) }
                .exceptionOrNull() is MissingDescriptorException,
            "A corrupt archive is a real failure and must NOT be classed as a missing descriptor"
        )
    }

    /**
     * A `mods.toml` may legitimately declare no `[[dependencies]]` block at all, and such a mod must
     * still be read normally — its declared id kept, its verdict SERVER.
     *
     * The absent block used to be raised as a `ScanningException` from the middle of the read, which
     * aborted the whole thing and fell back to the unreadable-jar defaults: the *filename* as the id.
     * The verdict was unaffected (a mod with no dependencies has no clientside signal, so SERVER is
     * the only possible answer either way), which is why nothing broke — but it discarded a mod id
     * that had been read successfully, and it logged an ERROR for an entirely ordinary descriptor.
     */
    @Test
    fun aForgeModWithoutADependenciesBlockKeepsItsDeclaredId(@TempDir tempDir: File) {
        val modsToml = """
            modLoader="javafml"
            loaderVersion="[40,)"
            license="MIT"

            [[mods]]
            modId="lonelymod"
            version="1.0.0"
        """.trimIndent()
        val jar = jarContaining(tempDir, "lonelymod-1.0.0.jar", "META-INF/mods.toml", modsToml)

        val scanned = modScanner.forgeTomlScanner.scan(listOf(jar)).single()

        Assertions.assertEquals(
            "lonelymod", scanned.modID,
            "The declared modId must survive a descriptor that names no dependencies"
        )
        Assertions.assertEquals(Sideness.SERVER, scanned.sideness, "No dependencies means no clientside signal")
        Assertions.assertTrue(scanned.dependencies.isEmpty(), "No dependencies were declared")
    }
    /**
     * Fabric's `provides` block, read verbatim.
     *
     * Verified against the real artifact: Fabric API **0.92.11+1.20.1** declares `"id": "fabric-api"` and
     * `"provides": ["fabric"]`, so a mod writing `depends: {"fabric": "*"}` is satisfied by a jar that calls
     * itself something else. Anything matching a dependency to a mod by id alone — `ModListCompiler`'s
     * rescue above all — needs the alias or it compares "fabric" to "fabric-api" and misses.
     *
     * The newest builds have dropped the block, so its absence must stay normal rather than an error.
     */
    @Test
    fun fabricProvidesIsRecordedAsAnAlias(@TempDir tempDir: File) {
        val jar = jarContaining(
            tempDir, "fabric-api.jar", "fabric.mod.json",
            """{"schemaVersion":1,"id":"fabric-api","version":"0.92.11","environment":"*","provides":["fabric"]}"""
        )

        Assertions.assertEquals(listOf("fabric"), modScanner.fabricScanner.scan(listOf(jar)).single().provides)
    }

    /** A descriptor without the block answers to its own id only — the common case, and not an error. */
    @Test
    fun aFabricModWithoutProvidesCarriesNoAliases(@TempDir tempDir: File) {
        val jar = jarContaining(
            tempDir, "plain.jar", "fabric.mod.json",
            """{"schemaVersion":1,"id":"plain","version":"1.0.0","environment":"*"}"""
        )

        Assertions.assertTrue(modScanner.fabricScanner.scan(listOf(jar)).single().provides.isEmpty())
    }

    /**
     * Quilt nests `provides` under `quilt_loader` and allows both entry shapes its `depends` block does —
     * a bare string, or an object carrying an `id`. Both occur, so both are read; asserted together
     * because reading only one shape fails silently, yielding a plausible empty list rather than an error.
     */
    @Test
    fun quiltProvidesIsReadInBothOfItsEntryShapes(@TempDir tempDir: File) {
        val jar = jarContaining(
            tempDir, "qfapi.jar", "quilt.mod.json",
            """{"schema_version":1,
                "quilt_loader":{"id":"quilted_fabric_api","version":"1.0.0",
                                "provides":["fabric",{"id":"fabric-api","version":"*"}]},
                "minecraft":{"environment":"*"}}"""
        )

        Assertions.assertEquals(
            listOf("fabric", "fabric-api"), modScanner.quiltScanner.scan(listOf(jar)).single().provides,
            "both the bare-string and the object entry shapes must be read"
        )
    }

    /**
     * **A Fabric-only jar booted under Quilt must not lose its declared dependencies.**
     *
     * Quilt deliberately runs Fabric mods, and most do not ship a `quilt.mod.json` at all. The Quilt scanner
     * then finds no descriptor, `DescriptorScanner` flattens that to a default entry — filename as id,
     * `SERVER`, **empty dependencies** — and the merge only prefers the Fabric result when the two disagree
     * about *sideness*. Two SERVER verdicts agree, so the empty entry wins and everything the Fabric manifest
     * declared is discarded.
     *
     * Reported 2026-08-31 from the live grinder: `bookshelf` on Quilt 0.31.0-beta.3 / Minecraft 1.21.1 died
     * with `Bookshelf requires any version of fabric-api, which is missing!` — the dependency was never
     * resolved because the scan never reported it. The same loss reaches real generation, where
     * `ModListCompiler`'s dependency rescue would fail to keep Fabric API in a Quilt pack that needs it.
     */
    @Test
    fun aFabricOnlyJarKeepsItsDependenciesWhenScannedForQuilt(@TempDir tempDir: File) {
        val descriptor = """
            {"schemaVersion":1,"id":"bookshelf","version":"1.0.0","environment":"*",
             "depends":{"fabric-api":"*","minecraft":"~1.21.1"}}
        """.trimIndent()
        val jar = jarContaining(tempDir, "bookshelf-fabric.jar", "fabric.mod.json", descriptor)

        val scanned = modScanner.quiltPackScanner.scan(listOf(jar)).single()

        Assertions.assertEquals("bookshelf", scanned.modID, "the Fabric descriptor's id, not the file name")
        Assertions.assertEquals(
            listOf("fabric-api"), scanned.dependencies.map { it.modID },
            "a Quilt pack scan must keep what the Fabric manifest declared; got ${scanned.dependencies}"
        )
        Assertions.assertEquals("~1.21.1", scanned.minecraftConstraint)
    }
}
