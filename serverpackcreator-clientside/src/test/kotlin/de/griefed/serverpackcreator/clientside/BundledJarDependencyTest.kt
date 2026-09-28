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

import de.griefed.serverpackcreator.api.modscanning.ModDependency
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

/**
 * Pins that **a dependency the candidate already carries inside itself is never fetched and never missing**.
 *
 * Reported live: `xaeros-world-map` was refused with *"Required dependency unavailable for Quilt /
 * Minecraft 26.2: xaerolib"*. Its Quilt/26.2 jar declares `depends: { "xaerolib": ">=1.0" }` **and ships it**
 * — `"jars": [{ "file": "META-INF/jars/xaerolib-fabric-26.2-1.7.1.jar" }]`, whose own descriptor reads
 * `id: xaerolib, version: 1.7.1`. Fabric and Quilt Loader load nested jars, so the requirement was satisfied
 * before anything was downloaded.
 *
 * **The near-miss is what made it fatal.** A Modrinth project `xaerolib` genuinely exists, so the manifest id
 * *mapped* — but it publishes 13 versions, **none** tagged Quilt and **none** tagged 26.2, so nothing could
 * be staged. A mapped-then-unstageable id lands in `unsatisfied`, which refuses; had the project not existed
 * at all it would have landed in `unmapped`, which does not. The mod was refused for a library it was
 * carrying.
 *
 * **Not one mod's quirk.** Jar-in-jar is ordinary: sampled the same day, `sodium` bundles **9** nested jars
 * and `modmenu` 1. Any bundled library that also exists as a thinly-tagged standalone project reproduces
 * this, and every occurrence costs an INCONCLUSIVE that overwrites whatever the store held.
 *
 * **Bundled wins unconditionally** (Griefed's call): the author shipped that exact build, so fetching a
 * different version of the same id is how a conflict gets manufactured and then blamed on the mod.
 */
internal class BundledJarDependencyTest {

    /** A nested mod jar carrying its own Fabric descriptor. */
    private fun nestedJar(dir: File, name: String, id: String, provides: List<String> = emptyList()): File =
        File(dir, name).also { jar ->
            JarOutputStream(jar.outputStream()).use { out ->
                out.putNextEntry(JarEntry("fabric.mod.json"))
                val providesJson = provides.joinToString(",") { "\"$it\"" }
                out.write("""{"schemaVersion":1,"id":"$id","version":"1.0.0","provides":[$providesJson]}""".toByteArray())
                out.closeEntry()
            }
        }

    /** A candidate jar whose descriptor declares [bundled] under `jars`, with those jars really inside it. */
    private fun candidateWithBundled(dir: File, vararg bundled: Pair<String, File>): File =
        File(dir, "candidate.jar").also { jar ->
            JarOutputStream(jar.outputStream()).use { out ->
                out.putNextEntry(JarEntry("fabric.mod.json"))
                val jarsJson = bundled.joinToString(",") { """{"file":"${it.first}"}""" }
                out.write(
                    """{"schemaVersion":1,"id":"candidate","version":"1.0.0","jars":[$jarsJson]}""".toByteArray()
                )
                out.closeEntry()
                bundled.forEach { (path, source) ->
                    out.putNextEntry(JarEntry(path))
                    out.write(source.readBytes())
                    out.closeEntry()
                }
            }
        }

    /** The `xaeros-world-map` shape, verbatim. */
    @Test
    fun aDependencyTheCandidateShipsIsNotStageable(@TempDir dir: File) {
        val nested = nestedJar(dir, "xaerolib.jar", id = "xaerolib")
        val candidate = candidateWithBundled(dir, "META-INF/jars/xaerolib-fabric-26.2-1.7.1.jar" to nested)

        val stageable = BootVerifier.stageableRequirements(
            listOf(ModDependency("xaerolib"), ModDependency("fabric-api")),
            bundledIds = BundledJars.idsIn(candidate)
        )

        Assertions.assertEquals(
            listOf("fabric-api"), stageable.map { it.modID },
            "xaerolib is inside the candidate; only the genuinely external dependency may be staged"
        )
    }

    /** The ids come from the nested jars' own descriptors, not from guessing at their file names. */
    @Test
    fun theBundledIdsAreReadFromTheNestedDescriptors(@TempDir dir: File) {
        val nested = nestedJar(dir, "lib.jar", id = "xaerolib")
        val candidate = candidateWithBundled(dir, "META-INF/jars/some-unrelated-file-name.jar" to nested)

        Assertions.assertEquals(setOf("xaerolib"), BundledJars.idsIn(candidate))
    }

    /** A nested jar's `provides` aliases count too — a dependant may name any of them. */
    @Test
    fun aNestedJarsProvidesAliasesAreBundledToo(@TempDir dir: File) {
        val nested = nestedJar(dir, "api.jar", id = "fabric-api", provides = listOf("fabric"))
        val candidate = candidateWithBundled(dir, "META-INF/jars/fabric-api.jar" to nested)

        Assertions.assertEquals(setOf("fabric-api", "fabric"), BundledJars.idsIn(candidate))
    }

    /** A dependency the candidate does *not* ship is still required, or this would hide real failures. */
    @Test
    fun aDependencyThatIsNotBundledIsStillRequired(@TempDir dir: File) {
        val nested = nestedJar(dir, "xaerolib.jar", id = "xaerolib")
        val candidate = candidateWithBundled(dir, "META-INF/jars/xaerolib.jar" to nested)

        val stageable = BootVerifier.stageableRequirements(
            listOf(ModDependency("cloth-config")),
            bundledIds = BundledJars.idsIn(candidate)
        )

        Assertions.assertEquals(listOf("cloth-config"), stageable.map { it.modID })
    }

    /** A jar bundling nothing behaves exactly as before. */
    @Test
    fun aCandidateWithNoNestedJarsChangesNothing(@TempDir dir: File) {
        val plain = File(dir, "plain.jar").also { jar ->
            JarOutputStream(jar.outputStream()).use { out ->
                out.putNextEntry(JarEntry("fabric.mod.json"))
                out.write("""{"schemaVersion":1,"id":"plain","version":"1.0.0"}""".toByteArray())
                out.closeEntry()
            }
        }

        Assertions.assertTrue(BundledJars.idsIn(plain).isEmpty())
        Assertions.assertEquals(
            listOf("cloth-config"),
            BootVerifier.stageableRequirements(listOf(ModDependency("cloth-config")), bundledIds = emptySet())
                .map { it.modID }
        )
    }

    /**
     * Quilt declares its nested jars under `quilt_loader.jars`. The reader has to know both shapes, since a
     * Quilt candidate is exactly the case that reported this.
     */
    @Test
    fun theQuiltDescriptorShapeIsReadToo(@TempDir dir: File) {
        val nested = nestedJar(dir, "qlib.jar", id = "xaerolib")
        val candidate = File(dir, "quilted.jar").also { jar ->
            JarOutputStream(jar.outputStream()).use { out ->
                out.putNextEntry(JarEntry("quilt.mod.json"))
                out.write(
                    """{"schema_version":1,"quilt_loader":{"id":"candidate","version":"1.0.0",
                       "jars":["META-INF/jars/qlib.jar"]}}""".trimIndent().toByteArray()
                )
                out.closeEntry()
                out.putNextEntry(JarEntry("META-INF/jars/qlib.jar"))
                out.write(nested.readBytes())
                out.closeEntry()
            }
        }

        Assertions.assertEquals(setOf("xaerolib"), BundledJars.idsIn(candidate))
    }

    /**
     * **Only what the descriptor declares.** A jar sitting in `META-INF/jars/` that the descriptor does not
     * list is not loaded by Fabric, so claiming it as bundled would skip staging something genuinely needed
     * and produce a failure to blame on the mod.
     */
    @Test
    fun anUndeclaredNestedJarIsNotTreatedAsBundled(@TempDir dir: File) {
        val nested = nestedJar(dir, "sneaky.jar", id = "sneakylib")
        val candidate = File(dir, "undeclared.jar").also { jar ->
            JarOutputStream(jar.outputStream()).use { out ->
                out.putNextEntry(JarEntry("fabric.mod.json"))
                out.write("""{"schemaVersion":1,"id":"candidate","version":"1.0.0"}""".toByteArray())
                out.closeEntry()
                out.putNextEntry(JarEntry("META-INF/jars/sneaky.jar"))
                out.write(nested.readBytes())
                out.closeEntry()
            }
        }

        Assertions.assertTrue(
            BundledJars.idsIn(candidate).isEmpty(),
            "the loader reads the declared list; anything else is not on the classpath"
        )
    }

    /** An unreadable jar yields nothing rather than throwing — staging must not die on a malformed file. */
    @Test
    fun anUnreadableJarYieldsNoBundledIds(@TempDir dir: File) {
        val notAJar = File(dir, "broken.jar").also { it.writeText("this is not a zip") }

        Assertions.assertTrue(BundledJars.idsIn(notAJar).isEmpty())
    }
}
