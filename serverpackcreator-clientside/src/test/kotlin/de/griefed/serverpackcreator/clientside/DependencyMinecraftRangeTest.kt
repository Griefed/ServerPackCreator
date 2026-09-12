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

import de.griefed.serverpackcreator.api.ApiWrapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

/**
 * Pins that a staged **dependency** whose own descriptor excludes the Minecraft version being booted is
 * dropped before a container is spent on it.
 *
 * `refuseForSelfDeclaration` asks this of the **candidate** and answers a disagreement by re-selecting a
 * version the jar accepts. Nothing asked it of the dependencies. `DependencyBacktrack.conflicts` matches
 * *mod-id → version* requirements and never looks at `ScannedMod.minecraftConstraint`, even though
 * `dependencyToDemote` already holds a `ScannedMod` for every staged jar and that field is on it.
 *
 * **Why it matters now.** Until 2026-09-09 the *exact-Minecraft* rule in `pickDependencyFile` was the
 * protection in this dimension: a dependency was never staged for another version, so its descriptor could
 * not disagree about one. The patch-version fallback deliberately relaxed that, which leaves the dimension
 * ungated — a `cobblemon` Fabric 1.21.1 build now stages into a 1.21.11 pack, the loader refuses the pack at
 * runtime, and the **candidate** wears the `INCONCLUSIVE`, which overwrites a decisive verdict in the store.
 * It cannot reach a false `CONFIRMED` (a wrong-Minecraft library produces none of the four decisive rungs),
 * so the cost is a wasted boot and a downgraded verdict rather than a wrong publication.
 *
 * The same gate closes the identical exposure in the **cross-loader** and **untagged** fallbacks, both of
 * which predate the patch fallback and can also stage a jar built against another Minecraft.
 *
 * Read from `FabricFamilyScanner`: `minecraftConstraint` is populated for Fabric (`depends.minecraft`),
 * Quilt (`quilt_loader.depends`' `minecraft` entry) and Forge/NeoForge (`readMinecraftConstraint`), so the
 * gate is not loader-specific.
 *
 * @author Griefed
 */
internal class DependencyMinecraftRangeTest {

    private val apiWrapper = ApiWrapper.api(File("build/resources/test/serverpackcreator.properties"))
    private val resolver = LoaderVersionResolver(apiWrapper.versionMeta)

    /** A real Fabric-capable server release from the cached metadata, so the test stays version-agnostic. */
    private val fabricRelease = apiWrapper.versionMeta.minecraft.serverReleases()
        .map { it.minecraftVersion }
        .first { resolver.latest("Fabric", it) != null }

    /** Selection passes and generation does not, so staging is exercised and no server is ever launched. */
    private val unbootableLoaderVersion = object : LoaderVersionPolicy {
        override fun preferredVersion(loader: String, minecraftVersion: String) = "0.0.0-no-such-build"
        override fun latestVersion(loader: String, minecraftVersion: String) = "0.0.0-no-such-build"
    }

    /**
     * A range that certainly excludes [fabricRelease], asserted rather than assumed — a constraint
     * `VersionConstraint` could not read would accept everything and let these guards pass for the wrong
     * reason, which is the failure mode this module has already paid for twice.
     */
    private val excludingRange = "~1.16.5"

    private fun fabricFile(fileName: String, dependencies: List<String> = emptyList()) =
        ModFile(fileName, setOf("Fabric"), setOf(fabricRelease), "https://cdn/$fileName", null, dependencies)

    /** The mod under test, needing the library by platform ref. */
    private val candidate = ProjectFiles(
        platform = "Modrinth", slug = "some-mod", projectUrl = "https://modrinth.com/mod/some-mod",
        clientSide = DeclaredSupport.REQUIRED, serverSide = DeclaredSupport.REQUIRED,
        files = listOf(fabricFile("some-mod-1.0.0.jar", listOf("some-lib")))
    )

    /** Newest first, exactly as both platforms answer, so the wrong-Minecraft build is picked first. */
    private val library = ProjectFiles(
        platform = "Modrinth", slug = "some-lib", projectUrl = "https://modrinth.com/mod/some-lib",
        clientSide = DeclaredSupport.UNKNOWN, serverSide = DeclaredSupport.UNKNOWN,
        files = listOf(fabricFile("some-lib-2.0.0.jar"), fabricFile("some-lib-1.0.0.jar"))
    )

    private val platform = object : ModPlatform {
        override val name: String = "Modrinth"
        override fun handles(projectUrl: String): Boolean = true
        override fun resolve(projectUrl: String): ProjectFiles = candidate
        override fun resolveDependency(nativeRef: String, minecraftVersion: String?): ProjectFiles? =
            library.takeIf { nativeRef == "some-lib" }
    }

    /** The bytes of a one-descriptor jar, to be written as an entry of another jar. */
    private fun nestedJarBytes(body: String): ByteArray {
        val buffer = ByteArrayOutputStream()
        JarOutputStream(buffer).use { out ->
            out.putNextEntry(JarEntry("fabric.mod.json"))
            out.write("""{"schemaVersion":1,$body}""".toByteArray())
            out.closeEntry()
        }
        return buffer.toByteArray()
    }

    /**
     * Writes the real jar the scanners will read, so nothing here is mocked below the descriptor.
     *
     * [nested] maps a file name to the `path to descriptor body` of a jar bundled inside it, which is how
     * the jar-in-jar case is expressed: the host declares the path and ships the jar.
     */
    private fun downloaderFor(
        bodies: Map<String, String>,
        nested: Map<String, Pair<String, String>> = emptyMap()
    ) = JarDownloader { file, targetDirectory ->
        val body = bodies[file.fileName] ?: return@JarDownloader null
        targetDirectory.mkdirs()
        File(targetDirectory, file.fileName).also { jar ->
            JarOutputStream(jar.outputStream()).use { out ->
                out.putNextEntry(JarEntry("fabric.mod.json"))
                out.write("""{"schemaVersion":1,$body}""".toByteArray())
                out.closeEntry()
                nested[file.fileName]?.let { (path, nestedBody) ->
                    out.putNextEntry(JarEntry(path))
                    out.write(nestedJarBytes(nestedBody))
                    out.closeEntry()
                }
            }
        }
    }

    private fun stagedMods(workDir: File): List<String> =
        File(workDir, AttemptDirectory.nameFor("Modrinth", "some-mod", "Fabric", BootCandidateSelector.minecraftLine(fabricRelease)) + "/modpack/mods")
            .listFiles()?.map { it.name }?.sorted() ?: emptyList()

    /** Stage the pack and return what ended up in `mods/`. */
    private fun stage(
        bodies: Map<String, String>,
        workDir: File,
        nested: Map<String, Pair<String, String>> = emptyMap()
    ): List<String> {
        BootVerifier(
            apiWrapper = apiWrapper,
            platform = platform,
            httpDownloader = downloaderFor(bodies, nested),
            loaderVersionPolicy = unbootableLoaderVersion,
            workDirectory = workDir
        ).prepareBootPack(candidate, "Fabric")
        return stagedMods(workDir)
    }

    /** The precondition every guard below rests on: the range really does exclude the pack's Minecraft. */
    @Test
    fun theFixtureRangeReallyExcludesThePacksMinecraft() {
        Assertions.assertFalse(
            VersionConstraint.satisfies(fabricRelease, excludingRange),
            "'$excludingRange' must exclude $fabricRelease, or these guards pass for the wrong reason"
        )
    }

    /**
     * **The whole point.** The newest library build declares a Minecraft this pack is not, so the loader
     * would refuse the pack and the candidate would wear the verdict. Staging drops it a build instead.
     */
    @Test
    fun aDependencyWhoseDescriptorExcludesThePacksMinecraftIsDemoted(@TempDir workDir: File) {
        val staged = stage(
            mapOf(
                "some-mod-1.0.0.jar" to """"id":"somemod","depends":{"some-lib":"*"}""",
                "some-lib-2.0.0.jar" to """"id":"some-lib","depends":{"minecraft":"$excludingRange"}""",
                "some-lib-1.0.0.jar" to """"id":"some-lib""""
            ),
            workDir
        )

        Assertions.assertEquals(
            listOf("some-lib-1.0.0.jar", "some-mod-1.0.0.jar"), staged,
            "the 2.0.0 build declares Minecraft '$excludingRange' and cannot load in a $fabricRelease pack"
        )
    }

    /**
     * **A bundled jar's pin counts as the jar that carries it**, because a jar-in-jar library is on the
     * classpath exactly like a staged one — while the host's own descriptor may say nothing at all.
     *
     * Measured live 2026-09-11, four published rows:
     * `quilted-fabric-api-11.0.0-alpha.3+0.102.0-1.21.jar` bundles `qsl_base-10.0.0-alpha.1+1.21.jar`, which
     * pins `minecraft [1.21, 1.21]` exactly. Staged into a Minecraft 1.21.1 pack it refused the whole pack
     * with *"Quilt Base API requires version [1.21, 1.21] of minecraft"*, and the candidate wore the
     * INCONCLUSIVE. Nothing read the nested descriptor, so nothing could have predicted it.
     */
    @Test
    fun aDependencyBundlingAJarThatExcludesThePacksMinecraftIsDemoted(@TempDir workDir: File) {
        val staged = stage(
            mapOf(
                "some-mod-1.0.0.jar" to """"id":"somemod","depends":{"some-lib":"*"}""",
                "some-lib-2.0.0.jar" to
                    """"id":"some-lib","jars":[{"file":"META-INF/jars/inner.jar"}]""",
                "some-lib-1.0.0.jar" to """"id":"some-lib""""
            ),
            workDir,
            nested = mapOf(
                "some-lib-2.0.0.jar" to
                    ("META-INF/jars/inner.jar" to """"id":"inner","depends":{"minecraft":"$excludingRange"}""")
            )
        )

        Assertions.assertEquals(
            listOf("some-lib-1.0.0.jar", "some-mod-1.0.0.jar"), staged,
            "the 2.0.0 build says nothing itself, and the jar it ships cannot load in a $fabricRelease pack"
        )
    }

    /**
     * A dependency that declares no range at all is left exactly where it was: silence is not a
     * disagreement, and a gate that refused on it would drop most of the ecosystem.
     */
    @Test
    fun aDependencyDeclaringNoMinecraftRangeIsLeftAlone(@TempDir workDir: File) {
        val staged = stage(
            mapOf(
                "some-mod-1.0.0.jar" to """"id":"somemod","depends":{"some-lib":"*"}""",
                "some-lib-2.0.0.jar" to """"id":"some-lib"""",
                "some-lib-1.0.0.jar" to """"id":"some-lib""""
            ),
            workDir
        )

        Assertions.assertEquals(
            listOf("some-lib-2.0.0.jar", "some-mod-1.0.0.jar"), staged,
            "nothing contradicts anything, so the newest build stays"
        )
    }

    /**
     * And a range the parser cannot read accepts, like every other constraint in this module: a grammar gap
     * must never become a mass-demotion, which is the direction `VersionConstraint` fails in by design.
     */
    @Test
    fun anUnreadableMinecraftRangeIsLeftAlone(@TempDir workDir: File) {
        val staged = stage(
            mapOf(
                "some-mod-1.0.0.jar" to """"id":"somemod","depends":{"some-lib":"*"}""",
                "some-lib-2.0.0.jar" to """"id":"some-lib","depends":{"minecraft":"whatever-this-is"}""",
                "some-lib-1.0.0.jar" to """"id":"some-lib""""
            ),
            workDir
        )

        Assertions.assertEquals(
            listOf("some-lib-2.0.0.jar", "some-mod-1.0.0.jar"), staged,
            "an unreadable range is not evidence of a mismatch"
        )
    }

    /**
     * The **candidate**'s own range is not a demotion. It is the subject of the experiment, and
     * `refuseForSelfDeclaration` plus `reselectOnMinecraftContradiction` already own that disagreement by
     * re-selecting a version the jar accepts — demoting it here would verify a different mod, and dropping
     * the *dependency* over it would be blaming the wrong jar.
     */
    @Test
    fun theCandidatesOwnRangeNeverDemotesADependency(@TempDir workDir: File) {
        val staged = stage(
            mapOf(
                "some-mod-1.0.0.jar" to
                    """"id":"somemod","depends":{"some-lib":"*","minecraft":"$excludingRange"}""",
                "some-lib-2.0.0.jar" to """"id":"some-lib"""",
                "some-lib-1.0.0.jar" to """"id":"some-lib""""
            ),
            workDir
        )

        Assertions.assertFalse(
            staged.contains("some-lib-1.0.0.jar"),
            "the candidate declared the mismatch; the library must not be demoted for it ($staged)"
        )
    }
}
