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
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

/**
 * Pins the pre-boot gate: **a jar is never booted on a runtime it does not claim to support.**
 *
 * A platform's declared loader/Minecraft sets are what an author ticked, and the selector trusts them
 * absolutely — it boots the newest Minecraft in the set and, where one file claims two loaders, whichever
 * the platform listed first. Measured against the live grinder on 2026-08-31 that produced
 * `DamageVignette-2.0.2-**forge**+mc1.20.jar` on a NeoForge boot and `create_ltab` on Minecraft 1.20.6 with
 * older mappings; both died as crashes and were scored as sideness evidence.
 *
 * The jar's own descriptor is the authority the platform metadata is not.
 */
internal class JarSelfDeclarationTest {

    /** A jar whose entries carry real content, for the checks that read a descriptor rather than list it. */
    private fun jarWithContent(dir: File, name: String, vararg entries: Pair<String, String>): File =
        File(dir, name).also { file ->
            JarOutputStream(file.outputStream()).use { out ->
                entries.forEach { (path, body) ->
                    out.putNextEntry(JarEntry(path))
                    out.write(body.toByteArray())
                    out.closeEntry()
                }
            }
        }

    private fun jar(dir: File, name: String, vararg entries: String): File =
        File(dir, name).also { file ->
            JarOutputStream(file.outputStream()).use { out ->
                entries.forEach { entry ->
                    out.putNextEntry(JarEntry(entry))
                    out.write("{}".toByteArray())
                    out.closeEntry()
                }
            }
        }

    /**
     * The answer is read **at a Minecraft version**, because which file a loader reads has moved twice.
     * `1.21.1` is used here as a version past both boundaries, where each descriptor names exactly one
     * loader; the eras themselves are pinned by the three guards further down.
     */
    @Test
    fun aJarNamesTheLoadersWhoseDescriptorsItCarries(@TempDir dir: File) {
        fun declared(name: String, vararg entries: String) =
            JarSelfDeclaration.declaredLoaders(jar(dir, name, *entries), "1.21.1")

        Assertions.assertEquals(setOf("Fabric"), declared("f.jar", "fabric.mod.json"))
        Assertions.assertEquals(setOf("Quilt"), declared("q.jar", "quilt.mod.json"))
        Assertions.assertEquals(setOf("Forge"), declared("fo.jar", "META-INF/mods.toml"))
        Assertions.assertEquals(setOf("NeoForge"), declared("n.jar", "META-INF/neoforge.mods.toml"))
        Assertions.assertEquals(
            setOf("Forge", "NeoForge"),
            declared("both.jar", "META-INF/mods.toml", "META-INF/neoforge.mods.toml"),
            "a jar really can carry both, and then either boot is legitimate"
        )
    }

    /**
     * **Two loaders from one file is a different fact from two loaders from two files**, and only the
     * version tells them apart. Before Minecraft 1.20.5 a lone `mods.toml` names Forge *and* NeoForge
     * because both read it — the presence of that file distinguishes nothing — while after 1.20.5 the same
     * jar names Forge alone.
     */
    @Test
    fun oneDescriptorCanNameTwoLoadersBeforeTheRename(@TempDir dir: File) {
        val modsTomlOnly = jar(dir, "ambiguous.jar", "META-INF/mods.toml")

        Assertions.assertEquals(
            setOf("Forge", "NeoForge"), JarSelfDeclaration.declaredLoaders(modsTomlOnly, "1.20.4"),
            "both loaders read mods.toml on 1.20.4, so it cannot say which the jar is for"
        )
        Assertions.assertEquals(
            setOf("Forge"), JarSelfDeclaration.declaredLoaders(modsTomlOnly, "1.20.6"),
            "from 1.20.5 NeoForge has its own descriptor, so mods.toml names Forge and nothing else"
        )
    }

    /** And the legacy Forge descriptors name Forge only while Forge is the loader that reads them. */
    @Test
    fun theLegacyForgeDescriptorsNameForgeBeforeMinecraft113(@TempDir dir: File) {
        Assertions.assertEquals(
            setOf("Forge"),
            JarSelfDeclaration.declaredLoaders(jar(dir, "legacy.jar", "mcmod.info"), "1.12.2")
        )
        Assertions.assertEquals(
            emptySet<String>(),
            JarSelfDeclaration.declaredLoaders(jar(dir, "legacy.jar", "mcmod.info"), "1.20.1"),
            "from 1.13 nothing reads mcmod.info, so it is not evidence of any loader"
        )
    }

    /**
     * The live NeoForge failure: a Forge jar, because one platform file claimed both loaders.
     *
     * **Asked at 1.21.1, where it can be asked.** This guard used to run at 1.20.4, and could not keep that
     * version once the eras were measured: Forge and NeoForge both read `mods.toml` until Minecraft 1.20.5,
     * so there the descriptor is *ambiguous* rather than contradictory, and refusing on it condemned 13
     * genuine NeoForge jars. The `DamageVignette` shape is still caught wherever the descriptors differ.
     *
     * **What the gate gives up in the 1.20.2–1.20.4 band, and why that is affordable.** A jar that really is
     * Forge-only, ticked NeoForge, now reaches a container and dies on
     * `Missing language javafml version [46,)` — which `BootLogClassifier`'s `runtimeMismatchMarkers` already
     * scores INCONCLUSIVE, *not* as sideness evidence. So the cost is one wasted boot in the false case, and
     * the gain is a real verdict in the thirteen true ones. A cheap catch was traded for correctness, not for
     * a wrong verdict — which is also what this object's fail-toward-accept design already prescribes when
     * the evidence cannot distinguish.
     */
    @Test
    fun aForgeJarIsRefusedForANeoForgeBoot(@TempDir dir: File) {
        val contradiction = JarSelfDeclaration.contradiction(
            jar(dir, "damagevignette-forge.jar", "META-INF/mods.toml"),
            loader = "NeoForge", minecraftVersion = "1.21.1", minecraftConstraint = null
        )

        Assertions.assertNotNull(contradiction)
        Assertions.assertTrue(contradiction!!.contains("NeoForge"), "the reason has to name the boot: $contradiction")
    }

    /** The live Fabric/Quilt failure: a jar whose mappings belong to another Minecraft. */
    @Test
    fun aJarIsRefusedForAMinecraftItDoesNotDeclare(@TempDir dir: File) {
        val contradiction = JarSelfDeclaration.contradiction(
            jar(dir, "create_ltab.jar", "fabric.mod.json"),
            loader = "Fabric", minecraftVersion = "1.20.6", minecraftConstraint = "1.20.1"
        )

        Assertions.assertNotNull(contradiction)
        Assertions.assertTrue(contradiction!!.contains("1.20.6") && contradiction.contains("1.20.1"), contradiction)
    }

    /** Quilt deliberately runs Fabric mods, so a Fabric descriptor is not a contradiction on a Quilt boot. */
    @Test
    fun aQuiltBootAcceptsAFabricJar(@TempDir dir: File) {
        Assertions.assertNull(
            JarSelfDeclaration.contradiction(
                jar(dir, "fabriconly.jar", "fabric.mod.json"),
                loader = "Quilt", minecraftVersion = "1.21.1", minecraftConstraint = null
            )
        )
    }

    /** The reverse does not hold: Fabric cannot load a Quilt-only mod, and pretending otherwise wastes a boot. */
    @Test
    fun aFabricBootDoesNotAcceptAQuiltOnlyJar(@TempDir dir: File) {
        Assertions.assertNotNull(
            JarSelfDeclaration.contradiction(
                jar(dir, "quiltonly.jar", "quilt.mod.json"),
                loader = "Fabric", minecraftVersion = "1.21.1", minecraftConstraint = null
            )
        )
    }

    @Test
    fun aMatchingLoaderAndMinecraftAreAccepted(@TempDir dir: File) {
        Assertions.assertNull(
            JarSelfDeclaration.contradiction(
                jar(dir, "good.jar", "fabric.mod.json"),
                loader = "Fabric", minecraftVersion = "1.20.1", minecraftConstraint = ">=1.20"
            )
        )
    }

    /**
     * **The safety property, and the whole design.** A gate that refuses on doubt turns a gap in descriptor
     * coverage into a catalog-wide mass-INCONCLUSIVE event — the same shape `VersionConstraint`'s own doc
     * warns about, and the same shape a `LoaderSupportMemory` once produced by marking Fabric unusable for
     * 22 Minecraft versions inside minutes. Only a *positive, readable* contradiction may refuse.
     */
    @Test
    fun everythingUnreadableIsAccepted(@TempDir dir: File) {
        val unreadable = listOf(
            jar(dir, "nodescriptor.jar", "META-INF/MANIFEST.MF"),
            jar(dir, "empty.jar"),
            File(dir, "absent.jar"),
            File(dir, "notazip.jar").apply { writeText("I am not a zip archive") },
            File(dir, "adirectory.jar").apply { mkdirs() }
        )

        for (candidate in unreadable) {
            for (loader in listOf("Fabric", "Forge", "NeoForge", "Quilt", "LegacyFabric", "Whatever")) {
                Assertions.assertNull(
                    JarSelfDeclaration.contradiction(candidate, loader, "1.20.1", null),
                    "${candidate.name} on $loader must be booted, not refused — doubt is not a contradiction"
                )
            }
        }
    }

    /** A constraint nobody can parse is doubt, not contradiction — same rule, on the other input. */
    @Test
    fun anUnreadableMinecraftConstraintIsAccepted(@TempDir dir: File) {
        val jar = jar(dir, "odd.jar", "fabric.mod.json")
        val nonsense = listOf("", "   ", "*", "whatever", "not-a-version", "${'$'}{version}", "[", "]", ">=", "~")

        for (constraint in nonsense) {
            Assertions.assertNull(
                JarSelfDeclaration.contradiction(jar, "Fabric", "1.20.1", constraint),
                "'$constraint' is unreadable, so it must accept rather than refuse"
            )
        }
    }

    /** An unknown loader is doubt about *our* vocabulary, not about the jar. */
    @Test
    fun anUnrecognisedLoaderIsAccepted(@TempDir dir: File) {
        Assertions.assertNull(
            JarSelfDeclaration.contradiction(
                jar(dir, "f.jar", "fabric.mod.json"), loader = "SomeNewLoader",
                minecraftVersion = "1.20.1", minecraftConstraint = null
            )
        )
    }

    /**
     * **NeoForge on Minecraft 1.20.1 *is* Forge, so a Forge jar is not a contradiction there.** NeoForge
     * 20.1.x forked Forge 47 and kept the `net.minecraftforge` packages, `javafml` and `META-INF/mods.toml`;
     * the package rename landed with 1.20.2, and from there the two are separate ecosystems. 1.20.1 is
     * therefore the whole band, not the start of one.
     *
     * The live false positive this pins: `Mantle-1.20.1-1.11.117.jar` is ticked Forge **and** NeoForge on
     * CurseForge and carries only `META-INF/mods.toml`, so the gate refused it as "not a NeoForge mod" and
     * the project published an `ERROR` row for a loader that runs it perfectly well — while the very same
     * file booted to a ready-line under Forge minutes earlier.
     */
    @Test
    fun aNeoForgeBootOnMinecraft1201AcceptsAForgeJar(@TempDir dir: File) {
        Assertions.assertNull(
            JarSelfDeclaration.contradiction(
                jar(dir, "Mantle-1.20.1-1.11.117.jar", "META-INF/mods.toml"),
                loader = "NeoForge", minecraftVersion = "1.20.1", minecraftConstraint = null
            )
        )
    }

    /**
     * **Once NeoForge has a descriptor of its own, a `mods.toml`-only jar is refused again.**
     *
     * This test used to include 1.20.2 and 1.20.4, and that was wrong: it conflated two different NeoForge
     * changes. The **package** rename (`net.minecraftforge` → `net.neoforged`) landed with 1.20.2, which is
     * what ends *jar parity* and is why `LoaderCompatibility` stays one version wide. The **descriptor**
     * rename landed with 1.20.5 — `META-INF/mods.toml` → `META-INF/neoforge.mods.toml`. Between them, both
     * loaders read the same file, so its presence is *ambiguous* rather than contradictory, and a gate whose
     * whole design is "refuse only on a positive, readable contradiction" must not refuse on it.
     *
     * Measured against the live Modrinth API on 2026-09-10, on two independent mods:
     *
     * | Minecraft | `architectury-api` | `jei` |
     * |---|---|---|
     * | 1.20.2 | `META-INF/mods.toml` | — |
     * | 1.20.4 | `META-INF/mods.toml` | `META-INF/mods.toml` |
     * | 1.20.6 | `META-INF/neoforge.mods.toml` | `META-INF/neoforge.mods.toml` |
     * | 1.21.1 | `META-INF/neoforge.mods.toml` | `META-INF/neoforge.mods.toml` |
     *
     * `-api`'s `ModScanner` had this right all along (`NEOFORGE_TOML_MINIMUM_MINECRAFT = "1.20.5"`), and
     * `serverpackcreator-api/CLAUDE.md` says so in words — this object simply held a second, version-blind
     * copy of the same knowledge.
     */
    @Test
    fun aNeoForgeBootRefusesAForgeJarOnceTheDescriptorsDiverge(@TempDir dir: File) {
        val forgeOnly = jar(dir, "forgeonly.jar", "META-INF/mods.toml")

        for (minecraftVersion in listOf("1.20.6", "1.21.1", "26.2")) {
            Assertions.assertNotNull(
                JarSelfDeclaration.contradiction(forgeOnly, "NeoForge", minecraftVersion, null),
                "NeoForge $minecraftVersion reads neoforge.mods.toml, so a mods.toml-only jar is not one"
            )
        }
    }

    /**
     * **The 13 rows this batch was opened for.** Every jar the live grinder refused this way carries *only*
     * `META-INF/mods.toml` at Minecraft 1.20.2 or 1.20.4 — including files whose own names say `neoforge`:
     * `botarium-neoforge-1.20.4-3.2.1.jar`, `decorative_blocks-NeoForge-1.20.4-5.0.2.jar`,
     * `emitrades-neoforge-1.3.0+mc1.20.4.jar`, `majrusz-library-neoforge-1.20.2-6.0.1.jar`,
     * `rebornstorage-1.20.4-5.1.2-neoforge.jar`. They are not mis-ticks; that *is* NeoForge's descriptor
     * for that Minecraft range.
     *
     * Note what this does **not** claim: NeoForge 1.20.4 cannot load a genuinely Forge-built jar (the
     * packages diverged at 1.20.2). It claims only that the descriptor cannot tell the two apart there, so
     * the gate has no evidence and must fall through to the boot. A jar that really is Forge-only then fails
     * on `Missing language javafml version`, which `runtimeMismatchMarkers` already scores as INCONCLUSIVE
     * rather than as sideness.
     */
    @Test
    fun aNeoForgeBootAcceptsAModsTomlJarBeforeTheDescriptorRename(@TempDir dir: File) {
        val neoForge1204 = jar(dir, "botarium-neoforge-1.20.4-3.2.1.jar", "META-INF/mods.toml")

        for (minecraftVersion in listOf("1.20.2", "1.20.3", "1.20.4")) {
            Assertions.assertNull(
                JarSelfDeclaration.contradiction(neoForge1204, "NeoForge", minecraftVersion, null),
                "NeoForge $minecraftVersion reads mods.toml, so carrying one is not evidence against it"
            )
        }
    }

    /**
     * **A pre-1.13 Forge mod declares itself in `mcmod.info`, which the gate could not see** — so a jar that
     * also ships a Fabric descriptor looked Fabric-only and its Forge boot was refused.
     *
     * Read from the live file: `SkyHanni-6.0.0-mc1.8.9.jar` carries `mcmod.info` **and** `fabric.mod.json`,
     * and neither `META-INF/mods.toml` nor `META-INF/fml_cache_annotation.json`. The gate fails *open* for a
     * jar that declares nothing at all, so it is the *visible* Fabric descriptor that flipped this one from
     * fail-open to fail-closed.
     *
     * Both legacy paths count: `mcmod.info` is what an author writes, and
     * `META-INF/fml_cache_annotation.json` is what `-api`'s `ForgeAnnotationScanner` reads.
     */
    @Test
    fun aLegacyForgeJarIsNotMistakenForFabricOnly(@TempDir dir: File) {
        val skyhanni = jar(dir, "SkyHanni-6.0.0-mc1.8.9.jar", "mcmod.info", "fabric.mod.json")
        val annotated = jar(dir, "old-forge.jar", "META-INF/fml_cache_annotation.json", "fabric.mod.json")

        Assertions.assertNull(
            JarSelfDeclaration.contradiction(skyhanni, "Forge", "1.8.9", null),
            "mcmod.info is the Forge descriptor before 1.13, so this jar does declare Forge"
        )
        Assertions.assertNull(
            JarSelfDeclaration.contradiction(annotated, "Forge", "1.12.2", null),
            "the FML annotation cache is the other legacy Forge descriptor"
        )
        Assertions.assertNotNull(
            JarSelfDeclaration.contradiction(skyhanni, "Forge", "1.20.1", null),
            "from 1.13 Forge reads mods.toml, so a legacy descriptor is no longer evidence of Forge"
        )
    }

    /** And a legacy Forge descriptor still says nothing about Quilt — the concession is loader-specific. */
    @Test
    fun aLegacyForgeJarIsStillRefusedForQuilt(@TempDir dir: File) {
        Assertions.assertNotNull(
            JarSelfDeclaration.contradiction(
                jar(dir, "SkyHanni-6.0.0-mc1.8.9.jar", "mcmod.info"),
                loader = "Quilt", minecraftVersion = "1.8.9", minecraftConstraint = null
            )
        )
    }

    /**
     * And the concession is one-way, like every other entry: Forge never gained the ability to read
     * `META-INF/neoforge.mods.toml`, so a NeoForge-only jar is refused on a Forge boot at 1.20.1 too.
     */
    @Test
    fun aForgeBootDoesNotAcceptANeoForgeOnlyJarOnMinecraft1201(@TempDir dir: File) {
        Assertions.assertNotNull(
            JarSelfDeclaration.contradiction(
                jar(dir, "neoforgeonly.jar", "META-INF/neoforge.mods.toml"),
                loader = "Forge", minecraftVersion = "1.20.1", minecraftConstraint = null
            )
        )
    }

    /**
     * A Sinytra Connector **placeholder** names itself in its own `mods.toml`, and that marker is the only
     * thing separating it from a genuine multi-loader jar carrying both descriptors.
     *
     * The shape is `continuity-3.0.0+1.20.1.forge.jar`'s, read from the live file: a stub `mods.toml`
     * carrying `[properties] "connector:placeholder" = true` and version-less dependency entries, beside
     * the `fabric.mod.json` that holds the actual mod.
     */
    @Test
    fun aConnectorPlaceholderNamesItselfInItsModsToml(@TempDir dir: File) {
        val placeholder = jarWithContent(
            dir, "continuity.forge.jar",
            "META-INF/mods.toml" to """
                modLoader = "javafml"
                [properties]
                "connector:placeholder" = true
                [[mods]]
                modId = "continuity"
            """.trimIndent(),
            "fabric.mod.json" to """{"id":"continuity","environment":"client"}"""
        )

        Assertions.assertTrue(JarSelfDeclaration.isConnectorPlaceholder(placeholder))
    }

    /** Everything else is not one — including a real multi-loader jar, which carries both descriptors too. */
    @Test
    fun anythingWithoutTheMarkerIsNotAConnectorPlaceholder(@TempDir dir: File) {
        val notPlaceholders = listOf(
            jarWithContent(
                dir, "multiloader.jar",
                "META-INF/mods.toml" to """
                    modLoader = "javafml"
                    [[mods]]
                    modId = "multiloader"
                """.trimIndent(),
                "fabric.mod.json" to """{"id":"multiloader"}"""
            ),
            jarWithContent(dir, "fabriconly.jar", "fabric.mod.json" to """{"id":"fabriconly"}"""),
            jarWithContent(dir, "unparseable.jar", "META-INF/mods.toml" to "this is not toml ]["),
            jar(dir, "empty.jar"),
            File(dir, "absent.jar")
        )

        for (candidate in notPlaceholders) {
            Assertions.assertFalse(
                JarSelfDeclaration.isConnectorPlaceholder(candidate),
                "${candidate.name} carries no placeholder marker"
            )
        }
    }
}
