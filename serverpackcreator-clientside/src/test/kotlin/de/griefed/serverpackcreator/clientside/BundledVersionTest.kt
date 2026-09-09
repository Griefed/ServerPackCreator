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
package de.griefed.serverpackcreator.clientside

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

/**
 * Pins [BundledJars.versionsIn] — the half of the jar-in-jar reader that answers *which build* of a bundled
 * mod is on the classpath, rather than merely whether one is.
 *
 * **Why it needs its own guards.** `versionsIn` was added with only an end-to-end test behind it
 * (`NestedDependencyConflictTest`), which exercises the happy path and nothing else. The two rules that make
 * it *safe* are its ambiguity rules, and mutating either to keep the first value seen passed the whole suite
 * — flagged as A-2 in `claude-docs/ANALYSIS-AUDIT.md`, 2026-09-08. They matter because a wrong version here
 * manufactures a demotion, which is how the 47 published `ERROR` verdicts of 2026-09-07 came about.
 *
 * @author Griefed
 */
internal class BundledVersionTest {

    /** The bytes of a one-descriptor jar, to be written as an entry of another jar. */
    private fun nestedJarBytes(body: String, descriptorName: String = "fabric.mod.json"): ByteArray {
        val buffer = ByteArrayOutputStream()
        JarOutputStream(buffer).use { out ->
            out.putNextEntry(JarEntry(descriptorName))
            out.write("""{"schemaVersion":1,$body}""".toByteArray())
            out.closeEntry()
        }
        return buffer.toByteArray()
    }

    /**
     * A jar declaring [nested] (path to descriptor body) in its own descriptor.
     *
     * @param outerBody      The host jar's descriptor body, which must declare the nested paths.
     * @param nested         Nested jar path to that jar's descriptor body.
     * @param descriptorName `fabric.mod.json` or `quilt.mod.json`, for both the host and the nested jars.
     */
    private fun jarWith(
        directory: File,
        fileName: String,
        outerBody: String,
        nested: Map<String, String>,
        descriptorName: String = "fabric.mod.json"
    ): File {
        directory.mkdirs()
        return File(directory, fileName).also { jar ->
            JarOutputStream(jar.outputStream()).use { out ->
                out.putNextEntry(JarEntry(descriptorName))
                out.write("""{"schemaVersion":1,$outerBody}""".toByteArray())
                out.closeEntry()
                nested.forEach { (path, body) ->
                    out.putNextEntry(JarEntry(path))
                    out.write(nestedJarBytes(body, descriptorName))
                    out.closeEntry()
                }
            }
        }
    }

    /** The ordinary case: the nested descriptor's own `version` is what the classpath will hold. */
    @Test
    fun aNestedModReportsTheVersionItsOwnDescriptorStates(@TempDir directory: File) {
        val host = jarWith(
            directory, "ponderjs-2.2.0.jar",
            """"id":"ponderjs","version":"2.2.0","jars":[{"file":"META-INF/jars/ponder.jar"}]""",
            mapOf("META-INF/jars/ponder.jar" to """"id":"ponder","version":"1.0.64"""")
        )

        Assertions.assertEquals(mapOf("ponder" to "1.0.64"), BundledJars.versionsIn(host))
    }

    /** A `provides` alias answers to the same version, since one jar really does supply both names. */
    @Test
    fun aProvidedAliasCarriesTheSameVersion(@TempDir directory: File) {
        val host = jarWith(
            directory, "host.jar",
            """"id":"host","jars":[{"file":"META-INF/jars/fabric-api.jar"}]""",
            mapOf(
                "META-INF/jars/fabric-api.jar" to
                    """"id":"fabric-api","version":"0.97.8","provides":["fabric"]"""
            )
        )

        Assertions.assertEquals(
            mapOf("fabric-api" to "0.97.8", "fabric" to "0.97.8"),
            BundledJars.versionsIn(host)
        )
    }

    /**
     * **The documented split.** A nested mod stating no version still counts as *present* — so staging keeps
     * skipping its download — but contributes no version for anything to be compared against.
     */
    @Test
    fun aNestedModWithoutAVersionIsStillPresentButUnversioned(@TempDir directory: File) {
        val host = jarWith(
            directory, "host.jar",
            """"id":"host","jars":[{"file":"META-INF/jars/xaerolib.jar"}]""",
            mapOf("META-INF/jars/xaerolib.jar" to """"id":"xaerolib"""")
        )

        Assertions.assertEquals(setOf("xaerolib"), BundledJars.idsIn(host), "presence is unaffected")
        Assertions.assertEquals(emptyMap<String, String>(), BundledJars.versionsIn(host), "no version to state")
    }

    /**
     * **Ambiguity contributes nothing.** Which of two bundled copies a loader picks is its own resolution
     * behaviour, and this module fails toward proceeding: no opinion costs a missed conflict, while a wrong
     * one manufactures a demotion.
     */
    @Test
    fun oneIdBundledAtTwoVersionsIsDropped(@TempDir directory: File) {
        val host = jarWith(
            directory, "host.jar",
            """"id":"host","jars":[{"file":"META-INF/jars/a.jar"},{"file":"META-INF/jars/b.jar"}]""",
            mapOf(
                "META-INF/jars/a.jar" to """"id":"ponder","version":"1.0.64"""",
                "META-INF/jars/b.jar" to """"id":"ponder","version":"1.0.90""""
            )
        )

        Assertions.assertEquals(
            emptyMap<String, String>(), BundledJars.versionsIn(host),
            "two versions of one id is not an opinion this can hold"
        )
    }

    /** The same id at the same version twice is not ambiguous — it is one answer, stated twice. */
    @Test
    fun oneIdBundledTwiceAtTheSameVersionIsKept(@TempDir directory: File) {
        val host = jarWith(
            directory, "host.jar",
            """"id":"host","jars":[{"file":"META-INF/jars/a.jar"},{"file":"META-INF/jars/b.jar"}]""",
            mapOf(
                "META-INF/jars/a.jar" to """"id":"ponder","version":"1.0.64"""",
                "META-INF/jars/b.jar" to """"id":"ponder","version":"1.0.64""""
            )
        )

        Assertions.assertEquals(mapOf("ponder" to "1.0.64"), BundledJars.versionsIn(host))
    }

    /** Quilt spells both the nesting and the version one level down, and must read identically. */
    @Test
    fun theQuiltSpellingIsReadToo(@TempDir directory: File) {
        val host = jarWith(
            directory, "host.jar",
            """"quilt_loader":{"id":"host","version":"1.0.0","jars":["META-INF/jars/qsl.jar"]}""",
            mapOf("META-INF/jars/qsl.jar" to """"quilt_loader":{"id":"qsl","version":"7.0.0"}"""),
            descriptorName = "quilt.mod.json"
        )

        Assertions.assertEquals(mapOf("qsl" to "7.0.0"), BundledJars.versionsIn(host))
    }

    /** Unreadable input yields nothing, for the same reason [BundledJars.idsIn] does: staging must not die. */
    @Test
    fun somethingThatIsNotAJarYieldsNothing(@TempDir directory: File) {
        val notAJar = File(directory, "broken.jar").apply { writeText("not a zip at all") }

        Assertions.assertEquals(emptyMap<String, String>(), BundledJars.versionsIn(notAJar))
    }

    /** A jar that declares no nested jars at all is the common case and must cost nothing. */
    @Test
    fun aJarBundlingNothingYieldsNothing(@TempDir directory: File) {
        val plain = jarWith(directory, "plain.jar", """"id":"plain","version":"1.0.0"""", emptyMap())

        Assertions.assertEquals(emptyMap<String, String>(), BundledJars.versionsIn(plain))
    }

    /**
     * **The same rule one level up**, across a pack rather than within a jar: two staged mods each bundling
     * a different build of one library is not an answer either.
     *
     * Asserted on `BootVerifier.nestedVersions` directly rather than through staging, because the
     * end-to-end route would depend on `File.listFiles()` order to decide *which* wrong version a broken
     * fold happened to keep — a guard that fails only sometimes is worse than none.
     */
    @Test
    fun oneIdBundledDifferentlyByTwoStagedJarsIsDropped(@TempDir directory: File) {
        val first = jarWith(
            directory, "ponderjs.jar",
            """"id":"ponderjs","jars":[{"file":"META-INF/jars/ponder.jar"}]""",
            mapOf("META-INF/jars/ponder.jar" to """"id":"ponder","version":"1.0.64"""")
        )
        val second = jarWith(
            directory, "ponderlib.jar",
            """"id":"ponderlib","jars":[{"file":"META-INF/jars/ponder.jar"}]""",
            mapOf("META-INF/jars/ponder.jar" to """"id":"ponder","version":"1.0.90"""")
        )

        Assertions.assertEquals(
            emptyMap<String, String>(),
            BootVerifier.nestedVersions(listOf(first, second)),
            "the pack holds two Ponders and nothing here knows which one the loader will load"
        )
    }

    /** And two jars bundling the *same* build agree, so the pack does hold that version. */
    @Test
    fun twoStagedJarsBundlingTheSameBuildAgree(@TempDir directory: File) {
        val body = """"id":"ponder","version":"1.0.64""""
        val first = jarWith(
            directory, "ponderjs.jar",
            """"id":"ponderjs","jars":[{"file":"META-INF/jars/ponder.jar"}]""",
            mapOf("META-INF/jars/ponder.jar" to body)
        )
        val second = jarWith(
            directory, "ponderlib.jar",
            """"id":"ponderlib","jars":[{"file":"META-INF/jars/ponder.jar"}]""",
            mapOf("META-INF/jars/ponder.jar" to body)
        )

        Assertions.assertEquals(
            mapOf("ponder" to "1.0.64"),
            BootVerifier.nestedVersions(listOf(first, second))
        )
    }
}
