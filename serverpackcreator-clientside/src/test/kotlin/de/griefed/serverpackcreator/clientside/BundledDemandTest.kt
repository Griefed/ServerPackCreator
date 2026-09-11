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
 * Pins [BundledJars.requirementsIn] and [BundledJars.minecraftDemandsIn] — what a **bundled** jar demands,
 * as opposed to what it provides.
 *
 * `idsIn` and `versionsIn` answer *"is this dependency already inside the jar, and which build?"*. These
 * answer the question nothing was asking: **a bundled library is on the classpath exactly like a staged one,
 * so its own demands bind exactly like a staged one's.** Two live failures, both read off the public
 * grinder's consoles on 2026-09-11:
 *
 * - `Modrinth/highlight` declares `depends: { "resourcefullib": "*" }` and ships
 *   `META-INF/jars/resourcefullib-fabric-26.2-5.0.3.jar`, so the requirement was rightly dropped — nothing
 *   needed downloading. The bundled jar's own descriptor declares `depends: { "fabric-api": "*" }`, which
 *   nothing read, so Fabric API was never staged and the boot died with *"Resourceful Lib requires any
 *   version of fabric-api, which is missing"* — charged to `highlight`. Verified by opening the published
 *   jar.
 * - `quilted-fabric-api-11.0.0-alpha.3+0.102.0-1.21.jar` bundles `qsl_base-10.0.0-alpha.1+1.21.jar`, which
 *   pins `minecraft [1.21, 1.21]` — exactly, not a line. Staged into a Minecraft **1.21.1** pack it refuses
 *   the whole pack, while QFAPI's own top-level descriptor says nothing that would have predicted it. Four
 *   published rows.
 *
 * @author Griefed
 */
internal class BundledDemandTest {

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

    /** A jar declaring [nested] (path to that jar's descriptor body) in its own descriptor. */
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

    /** **The `highlight` shape**, verbatim from the published jar. */
    @Test
    fun aBundledJarsOwnRequirementIsReported(@TempDir directory: File) {
        val host = jarWith(
            directory, "hightlight-26.2-4.2.0.jar",
            """"id":"highlight","depends":{"resourcefullib":"*","minecraft":">=26.2"},""" +
                """"jars":[{"file":"META-INF/jars/resourcefullib-fabric-26.2-5.0.3.jar"}]""",
            mapOf(
                "META-INF/jars/resourcefullib-fabric-26.2-5.0.3.jar" to
                    """"id":"resourcefullib","version":"5.0.3","depends":{"fabric-api":"*","minecraft":">=26.2"}"""
            )
        )

        Assertions.assertEquals(
            listOf("fabric-api" to "*"),
            BundledJars.requirementsIn(host).map { it.modID to it.versionConstraint },
            "the bundled library's own demand is what nothing was reading"
        )
    }

    /**
     * `minecraft` is answered by [BundledJars.minecraftDemandsIn] instead, because the consequence differs:
     * an unmet *mod* dependency is staged, while a bundled jar built for another Minecraft can only be
     * answered by dropping the jar that carries it.
     */
    @Test
    fun theGameItselfIsNotReportedAsSomethingToStage(@TempDir directory: File) {
        val host = jarWith(
            directory, "host.jar",
            """"id":"host","jars":[{"file":"META-INF/jars/lib.jar"}]""",
            mapOf("META-INF/jars/lib.jar" to """"id":"lib","depends":{"minecraft":"1.21"}""")
        )

        Assertions.assertEquals(emptyList<String>(), BundledJars.requirementsIn(host).map { it.modID })
        Assertions.assertEquals(mapOf("lib" to "1.21"), BundledJars.minecraftDemandsIn(host))
    }

    /** **The QSL shape**: an exact pin one patch release away from the pack, invisible from the host jar. */
    @Test
    fun aBundledJarsExactMinecraftPinIsReported(@TempDir directory: File) {
        val host = jarWith(
            directory, "quilted-fabric-api-11.0.0-alpha.3+0.102.0-1.21.jar",
            """"id":"quilted_fabric_api","jars":[{"file":"META-INF/jars/qsl_base-10.0.0-alpha.1+1.21.jar"}]""",
            mapOf(
                "META-INF/jars/qsl_base-10.0.0-alpha.1+1.21.jar" to
                    """"id":"quilt_base","version":"10.0.0-alpha.1","depends":{"minecraft":"[1.21, 1.21]"}"""
            )
        )

        val pinned = BundledJars.minecraftDemandsIn(host)

        Assertions.assertEquals(mapOf("quilt_base" to "[1.21, 1.21]"), pinned)
        Assertions.assertFalse(
            VersionConstraint.satisfies("1.21.1", pinned.getValue("quilt_base")),
            "and the pin really does exclude the version the pack booted -- otherwise this fixture would " +
                "pass for the wrong reason"
        )
    }

    /** Quilt spells both blocks differently, and a bundled Quilt library is as ordinary as a Fabric one. */
    @Test
    fun quiltsOwnSpellingIsRead(@TempDir directory: File) {
        val host = jarWith(
            directory, "host.jar",
            """"quilt_loader":{"id":"host","jars":["META-INF/jars/lib.jar"]}""",
            mapOf(
                "META-INF/jars/lib.jar" to
                    """"quilt_loader":{"id":"lib","version":"1.0","depends":[{"id":"qsl","versions":">=7.0"},""" +
                        """{"id":"minecraft","versions":"1.21"}]}"""
            ),
            descriptorName = "quilt.mod.json"
        )

        Assertions.assertEquals(
            listOf("qsl" to ">=7.0"),
            BundledJars.requirementsIn(host).map { it.modID to it.versionConstraint }
        )
        Assertions.assertEquals(mapOf("lib" to "1.21"), BundledJars.minecraftDemandsIn(host))
    }

    /**
     * A range that is not a plain string — Quilt permits an object, Fabric an array of alternatives — yields
     * no opinion rather than a guess, which `VersionConstraint` then accepts. Failing toward accepting is
     * this module's standing rule: a range we cannot read must never manufacture a refusal.
     */
    @Test
    fun aRangeThatIsNotAPlainStringYieldsNoOpinion(@TempDir directory: File) {
        val host = jarWith(
            directory, "host.jar",
            """"id":"host","jars":[{"file":"META-INF/jars/lib.jar"}]""",
            mapOf("META-INF/jars/lib.jar" to """"id":"lib","depends":{"other":["1.0","2.0"]}""")
        )

        Assertions.assertEquals(
            listOf("other" to null),
            BundledJars.requirementsIn(host).map { it.modID to it.versionConstraint }
        )
    }

    /** A jar bundling nothing demands nothing; nothing is invented for one that cannot be read. */
    @Test
    fun ajarThatBundlesNothingDemandsNothing(@TempDir directory: File) {
        val plain = jarWith(directory, "plain.jar", """"id":"plain"""", emptyMap())
        val unreadable = File(directory, "broken.jar").apply { writeText("not a jar") }

        Assertions.assertEquals(emptyList<Any>(), BundledJars.requirementsIn(plain))
        Assertions.assertEquals(emptyMap<String, String>(), BundledJars.minecraftDemandsIn(plain))
        Assertions.assertEquals(emptyList<Any>(), BundledJars.requirementsIn(unreadable))
        Assertions.assertEquals(emptyMap<String, String>(), BundledJars.minecraftDemandsIn(unreadable))
    }

    /**
     * **Only jars the descriptor declares count**, the same restraint [BundledJars.idsIn] keeps. Fabric
     * loads the jars its descriptor lists; a stray file under `META-INF/jars/` is not on the classpath, and
     * staging what it demands would fetch a library nothing will load.
     */
    @Test
    fun anUndeclaredNestedJarDemandsNothing(@TempDir directory: File) {
        val host = jarWith(
            directory, "host.jar",
            """"id":"host"""",
            mapOf("META-INF/jars/stray.jar" to """"id":"stray","depends":{"ghost":"*"}""")
        )

        Assertions.assertEquals(emptyList<Any>(), BundledJars.requirementsIn(host))
    }
}
