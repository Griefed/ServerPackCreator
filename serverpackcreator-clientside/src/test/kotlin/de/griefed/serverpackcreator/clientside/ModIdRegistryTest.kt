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

/**
 * Pins the bridge between the two vocabularies a dependency is spelled in.
 *
 * A jar manifest names `fabric`; the platform wants a Modrinth slug (`fabric-api`) or a CurseForge numeric
 * id (`306612`). Without a mapping, the single most-depended-on mod in the ecosystem cannot be resolved
 * from a manifest at all.
 *
 * **The table is deliberately tiny.** The platform-declared dependency path already resolves everything the
 * platform itself knows, so this exists only for ids the platform metadata omits. A large guessed table
 * would be un-pinned data that goes stale silently — the failure class this repository documents at length.
 */
internal class ModIdRegistryTest {

    @Test
    fun mapsFabricApiOnBothPlatforms() {
        Assertions.assertEquals("fabric-api", KnownModIds.refFor("fabric", "Modrinth"))
        Assertions.assertEquals("306612", KnownModIds.refFor("fabric", "CurseForge"))
    }

    /** `fabric-api` is how a Quilt descriptor and some Fabric ones spell the same project. */
    @Test
    fun theApisOtherSpellingMapsToTheSameProject() {
        Assertions.assertEquals(
            KnownModIds.refFor("fabric", "Modrinth"),
            KnownModIds.refFor("fabric-api", "Modrinth")
        )
    }

    /**
     * On Modrinth an unknown id is *tried* as a slug, because Modrinth resolves a project by slug or id and
     * most mod ids are their slug. That costs one lookup that may miss, which is far cheaper than not
     * resolving a dependency at all.
     */
    @Test
    fun anUnknownIdIsTriedAsAModrinthSlug() {
        Assertions.assertEquals("cloth-config", KnownModIds.refFor("cloth-config", "Modrinth"))
    }

    /**
     * **CurseForge gets a guess too, now that a guess cannot refuse a boot** (Griefed's call, 2026-09-06).
     *
     * It used to get none, and the reasoning was sound at the time: CurseForge addresses projects by numeric
     * id, so a mod id is never a valid ref, and a mapped-then-unstageable id refused the boot — which made
     * *being almost resolvable worse than being unknown*, the `xaerolib` trap. The cost was that every
     * manifest-declared dependency of a CurseForge candidate was unresolvable unless it was one of four
     * hardcoded aliases: `modtweaker` never staged `mtlib`, which CurseForge publishes under that exact slug.
     *
     * What changed is the refusal split, not the guess: it now keys on **how the ref was arrived at** rather
     * than on how far it got. A [ModIdMapping.Guess] never refuses at any stage, so guessing costs at most a
     * lookup that misses — and CurseForge's search endpoint resolves a slug to the numeric id its other
     * routes need, which `CurseForgePlatform.resolve` was already doing.
     */
    @Test
    fun anUnknownIdIsGuessedOnBothPlatforms() {
        Assertions.assertEquals("cloth-config", KnownModIds.refFor("cloth-config", "Modrinth"))
        Assertions.assertEquals("cloth-config", KnownModIds.refFor("cloth-config", "CurseForge"))
    }

    /**
     * The distinction the refusal split reads. An alias is a project we *know* the id names — a failure to
     * honour it is a real gap and may refuse a boot. A guess is an optimistic slug that may name nothing,
     * or something else entirely, so it must never refuse.
     */
    @Test
    fun anAliasIsConfidentAndAGuessIsNot() {
        Assertions.assertEquals(ModIdMapping.Alias("306612"), KnownModIds.mappingFor("fabric", "CurseForge"))
        Assertions.assertEquals(ModIdMapping.Alias("fabric-api"), KnownModIds.mappingFor("fabric", "Modrinth"))
        Assertions.assertEquals(ModIdMapping.Alias("qsl"), KnownModIds.mappingFor("quilt_resource_loader", "Modrinth"))

        Assertions.assertEquals(ModIdMapping.Guess("mtlib"), KnownModIds.mappingFor("mtlib", "CurseForge"))
        Assertions.assertEquals(ModIdMapping.Guess("cloth-config"), KnownModIds.mappingFor("cloth-config", "Modrinth"))
    }

    /** Nothing to try stays nothing to try — a blank id, or a platform the registry has never heard of. */
    @Test
    fun nothingToTryIsItsOwnAnswer() {
        Assertions.assertEquals(ModIdMapping.None, KnownModIds.mappingFor("   ", "Modrinth"))
        Assertions.assertEquals(ModIdMapping.None, KnownModIds.mappingFor("mtlib", "SomeFuturePlatform"))
        Assertions.assertNull(KnownModIds.refFor("mtlib", "SomeFuturePlatform"))
    }

    /** `refFor` is the ref of whatever `mappingFor` decided — one rule, read two ways. */
    @Test
    fun refForIsTheMappingsRef() {
        for (id in listOf("fabric", "quilt_resource_loader", "mtlib", "cloth-config", "   ")) {
            for (platform in listOf("Modrinth", "CurseForge")) {
                Assertions.assertEquals(
                    KnownModIds.mappingFor(id, platform).ref, KnownModIds.refFor(id, platform), "$id / $platform"
                )
            }
        }
    }

    /**
     * **QFAPI is why this registry exists rather than a slug guess.** Neither platform addresses it by
     * anything resembling `quilted_fabric_api`: Modrinth calls it `qsl` and CurseForge `634179`. The
     * Modrinth slug-fallback would miss it outright, so without the alias a Quilt mod's core dependency is
     * unresolvable. Verified against both live APIs on 2026-08-29.
     */
    @Test
    fun quiltedFabricApiMapsToQslOnBothPlatforms() {
        Assertions.assertEquals("qsl", KnownModIds.refFor("quilted_fabric_api", "Modrinth"))
        Assertions.assertEquals("634179", KnownModIds.refFor("quilted_fabric_api", "CurseForge"))
        Assertions.assertEquals(
            KnownModIds.refFor("quilted_fabric_api", "Modrinth"), KnownModIds.refFor("qsl", "Modrinth"),
            "the project's own slug must resolve to itself, since a descriptor may use either name"
        )
    }

    /** Ids are matched case-insensitively; descriptors are hand-written and inconsistent about it. */
    @Test
    fun idsAreMatchedCaseInsensitively() {
        Assertions.assertEquals("fabric-api", KnownModIds.refFor("Fabric", "Modrinth"))
    }

    /** A blank id maps nowhere rather than to a lookup that would resolve something arbitrary. */
    @Test
    fun aBlankIdMapsNowhere() {
        Assertions.assertNull(KnownModIds.refFor("   ", "Modrinth"))
    }

    /**
     * **Fabric API is one project published as ~45 modules, and a mod depends on the modules.** A descriptor
     * says `fabric-resource-loader-v0`; neither platform has a project by that name. Modrinth's slug guess
     * 404s and CurseForge refuses to guess at all, so the single most common dependency in the Fabric
     * ecosystem went unstaged — the mod then booted without it, its loader refused the pack, and the
     * *candidate* wore an INCONCLUSIVE for a dependency the harness never supplied.
     *
     * Reported live 2026-09-01 for `fabric-resource-loader-v*`, `fabric-block-getter-api-v*` and
     * `fabric-rendering-fluids-v*`, and it is the same shape as the Quilt solver failure recorded earlier:
     * `fabric-resource-loader-v0 versions [*] (0 valid options, 0 invalid options)`.
     */
    @Test
    fun fabricApiModulesResolveToFabricApiOnBothPlatforms() {
        val modules = listOf(
            "fabric-resource-loader-v0", "fabric-block-getter-api-v2", "fabric-rendering-fluids-v1",
            "fabric-networking-api-v1", "fabric-command-api-v2", "fabric-registry-sync-v0",
            "fabric-loot-api-v3", "fabric-api-lookup-api-v1", "fabric-transitive-access-wideners-v1"
        )

        for (module in modules) {
            Assertions.assertEquals("fabric-api", KnownModIds.refFor(module, "Modrinth"), module)
            Assertions.assertEquals("306612", KnownModIds.refFor(module, "CurseForge"), module)
        }
    }

    /**
     * **The version suffix moves, which is why this is a rule and not a list.** Today's Fabric API ships
     * `fabric-resource-loader-v1` and `fabric-block-getter-api-v2`, but the corpus is full of older mods
     * declaring `-v0` and `-v1` — ids that exist in no current source tree. A table snapshotted from the
     * repository would therefore be wrong for exactly the historical mods this grinder spends its time on.
     */
    @Test
    fun everyApiVersionOfAModuleResolves() {
        for (version in 0..3) {
            Assertions.assertEquals(
                "fabric-api", KnownModIds.refFor("fabric-resource-loader-v${version}", "Modrinth"),
                "v${version} of a module is still Fabric API"
            )
        }
    }

    /** The two modules that carry no version suffix at all are still Fabric API. */
    @Test
    fun theUnversionedModulesAreRecognisedToo() {
        Assertions.assertEquals("fabric-api", KnownModIds.refFor("fabric-api-base", "Modrinth"))
        Assertions.assertEquals("fabric-api", KnownModIds.refFor("fabric-renderer-indigo", "Modrinth"))
    }

    /**
     * **The collision that makes a bare pattern wrong.** lucko's permissions library declares
     * `fabric-permissions-api-v0` (plural), which matches the module shape exactly while being a separate
     * project — verified against its `fabric.mod.json` on 2026-09-01. Fabric API's own module is
     * `fabric-permission-api-v1` (singular), one character apart, and must still resolve. Claiming lucko's
     * for Fabric API would stage the wrong jar and report a dependency the mod never declared.
     */
    @Test
    fun aThirdPartyModuleShapedIdIsNotClaimedForFabricApi() {
        Assertions.assertNotEquals(
            "fabric-api", KnownModIds.refFor("fabric-permissions-api-v0", "Modrinth"),
            "lucko's permissions library is not Fabric API"
        )
        Assertions.assertNotEquals(
            "306612", KnownModIds.refFor("fabric-permissions-api-v0", "CurseForge"),
            "and it must not be claimed for Fabric API on CurseForge either"
        )
        Assertions.assertEquals(
            ModIdMapping.Guess("fabric-permissions-api-v0"),
            KnownModIds.mappingFor("fabric-permissions-api-v0", "CurseForge"),
            "it is its own project, so it is a guess at its own slug and never refuses"
        )
        Assertions.assertEquals(
            "fabric-api", KnownModIds.refFor("fabric-permission-api-v1", "Modrinth"),
            "Fabric API's own singular module is one character away and must still resolve"
        )
    }

    /**
     * **QSL is the Quilt mirror of the Fabric API case, and it was left open by the Fabric fix.** Quilt
     * Standard Libraries ships as ~33 modules and a Quilt descriptor depends on the modules, not on the
     * project: `quilt_resource_loader`, `quilt_networking`, `quilt_registry`. Neither platform publishes
     * them, so each fell through to a Modrinth slug guess that 404s and to nothing at all on CurseForge —
     * the same unstaged-dependency failure `fabric-resource-loader-v0` produced.
     *
     * Verified 2026-09-01 by reading all 47 `quilt.mod.json` files in `QuiltMC/quilt-standard-libraries`
     * (branch 1.21.5) and collecting what they declare in `depends`: 33 distinct `quilt_*` ids, e.g.
     * `quilt_resource_loader_testmod` depends on `["quilt_loader", "quilt_resource_loader"]`.
     *
     * **The shape differs from Fabric's and the rule must not be copied.** QSL ids are underscored and carry
     * **no** API-version suffix, so the `-v<digits>` rule matches none of them.
     */
    @Test
    fun qslModulesResolveToQslOnBothPlatforms() {
        val modules = listOf(
            "quilt_resource_loader", "quilt_networking", "quilt_registry", "quilt_lifecycle_events",
            "quilt_item_extensions", "quilt_block_extensions", "quilt_crash_info", "quilt_screen",
            "quilt_datafixerupper", "quilt_registry_entry_attachment"
        )

        for (module in modules) {
            Assertions.assertEquals("qsl", KnownModIds.refFor(module, "Modrinth"), module)
            Assertions.assertEquals("634179", KnownModIds.refFor(module, "CurseForge"), module)
        }
    }

    /**
     * `quilt_loader` is Quilt Loader, not a QSL module — the runtime provides it, and `BootVerifier`'s
     * `environmentProvidedIds` already drops it before staging. Mapping it to QSL would be wrong even
     * though nothing downstream would notice today, and "nothing notices" is not a reason to record a
     * false fact in a lookup table other code is entitled to trust.
     */
    @Test
    fun theQuiltLoaderItselfIsNotAQslModule() {
        Assertions.assertNotEquals("qsl", KnownModIds.refFor("quilt_loader", "Modrinth"))
        Assertions.assertNotEquals("634179", KnownModIds.refFor("quilt_loader", "CurseForge"))
    }

    /** The two families do not bleed into one another: a QSL id is never Fabric API, nor the reverse. */
    @Test
    fun theFabricAndQuiltFamiliesStaySeparate() {
        Assertions.assertEquals("fabric-api", KnownModIds.refFor("fabric-resource-loader-v0", "Modrinth"))
        Assertions.assertEquals("qsl", KnownModIds.refFor("quilt_resource_loader", "Modrinth"))
    }

    /**
     * A `fabric-` prefix alone proves nothing: `fabric-language-kotlin` is its own project, and its id has
     * no module version suffix. It keeps the plain Modrinth slug guess, which is correct — that *is* its slug.
     */
    @Test
    fun aFabricPrefixedProjectIsNotAModule() {
        Assertions.assertEquals("fabric-language-kotlin", KnownModIds.refFor("fabric-language-kotlin", "Modrinth"))
        Assertions.assertNotEquals("306612", KnownModIds.refFor("fabric-language-kotlin", "CurseForge"))
    }

    /**
     * **YACL's mod id carries its major version, and its slug does not.** `yet_another_config_lib_v3` is
     * what a descriptor names; both platforms publish the project as `yacl`, so the optimistic
     * slug-guess resolves to nothing on either.
     *
     * Observed twice on the live daemon before being added, which is the bar this table sets:
     * `Modrinth/do-a-barrel-roll` on Fabric / Minecraft 26.2 booted without it and the loader refused the
     * pack with *"requires any version of yet_another_config_lib_v3, which is missing"*, and the earlier
     * `zoomify` backtrack case reached the same project by the platform ref instead.
     *
     * **Modrinth marks it `optional` for do-a-barrel-roll while the jar declares it under `depends`**, so
     * the platform half filters it out (correctly — that filter exists) and the manifest half is the only
     * route left. That is exactly the gap an alias closes.
     *
     * A shape rule rather than a single entry, for the reason the Fabric API modules have one: the suffix
     * tracks the library's major version and has already moved once (`_v2` -> `_v3`), so a literal entry
     * would go stale at the next major and take the same debugging session to find again.
     */
    @Test
    fun yetAnotherConfigLibResolvesToYaclOnBothPlatforms() {
        Assertions.assertEquals(
            ModIdMapping.Alias("yacl"),
            KnownModIds.mappingFor("yet_another_config_lib_v3", "Modrinth")
        )
        Assertions.assertEquals(
            ModIdMapping.Alias("667299"),
            KnownModIds.mappingFor("yet_another_config_lib_v3", "CurseForge")
        )
        Assertions.assertEquals(
            ModIdMapping.Alias("yacl"),
            KnownModIds.mappingFor("yet_another_config_lib_v2", "Modrinth"),
            "the suffix is the library's major version and has moved before"
        )
    }

    /** And the shape stays narrow: a different library with a versioned id is still only a guess. */
    @Test
    fun anotherLibraryWithAVersionedIdIsNotClaimedForYacl() {
        Assertions.assertEquals(
            ModIdMapping.Guess("some_other_config_lib_v3"),
            KnownModIds.mappingFor("some_other_config_lib_v3", "Modrinth")
        )
    }
}
