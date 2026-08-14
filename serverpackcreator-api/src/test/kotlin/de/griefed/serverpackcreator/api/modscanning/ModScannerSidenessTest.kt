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
}
