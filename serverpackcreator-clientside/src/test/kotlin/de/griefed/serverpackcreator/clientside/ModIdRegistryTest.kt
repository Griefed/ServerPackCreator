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
     * **CurseForge gets no guess.** It addresses projects by numeric id, so a mod id is never a valid ref;
     * guessing would mean spending the API key's quota on a search that cannot be verified from the id
     * alone. An unmappable id is reported rather than fabricated — see the refusal split.
     */
    @Test
    fun anUnknownIdIsNotGuessedOnCurseForge() {
        Assertions.assertNull(KnownModIds.refFor("cloth-config", "CurseForge"))
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
        Assertions.assertNull(
            KnownModIds.refFor("fabric-permissions-api-v0", "CurseForge"),
            "and it must not be fabricated on CurseForge either"
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
        Assertions.assertNull(KnownModIds.refFor("quilt_loader", "CurseForge"))
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
        Assertions.assertNull(KnownModIds.refFor("fabric-language-kotlin", "CurseForge"))
    }

    /**
     * A mod id no alias covers is offered to **CurseForge** as a candidate slug, exactly as it already is
     * to Modrinth — the platform then decides whether such a project exists.
     *
     * Until 2026-09-06 this returned `null` for CurseForge, so *every* manifest-declared dependency of a
     * CurseForge candidate was unmappable unless it happened to be one of the four hardcoded aliases.
     * Reported from the live grinder: `modtweaker` on Forge/1.12.2 refused for `mtlib`, which is published
     * on CurseForge under exactly that slug. Executed against the registry at the time, `mtlib`,
     * `crafttweaker`, `jei`, `athena`, `flywheel` and `xaerolib` all returned `null` for CurseForge and
     * their own id for Modrinth.
     *
     * The asymmetry had a real cause — CurseForge addresses projects by numeric id, which cannot be
     * guessed — but the conclusion did not follow: its search endpoint takes a slug, and
     * `CurseForgePlatform.resolve` was already using it. Returning the id here is the same optimistic guess
     * Modrinth gets, and a guess that misses still resolves to nothing rather than to something wrong.
     */
    @Test
    fun offersAnUnknownModIdAsASlugToBothPlatforms() {
        for (id in listOf("mtlib", "crafttweaker", "jei", "athena", "flywheel", "xaerolib")) {
            Assertions.assertEquals(id, KnownModIds.refFor(id, "CurseForge"), "CurseForge ref for '$id'")
            Assertions.assertEquals(id, KnownModIds.refFor(id, "Modrinth"), "Modrinth ref for '$id'")
        }
    }

    /** An alias still wins over the guess, on both platforms — that is the whole point of having one. */
    @Test
    fun stillPrefersAnAliasOverTheGuess() {
        Assertions.assertEquals("306612", KnownModIds.refFor("fabric", "CurseForge"))
        Assertions.assertEquals("fabric-api", KnownModIds.refFor("fabric", "Modrinth"))
        Assertions.assertEquals("634179", KnownModIds.refFor("quilt_base", "CurseForge"))
        Assertions.assertEquals("qsl", KnownModIds.refFor("quilt_base", "Modrinth"))
    }

    /** An unknown platform is still nothing; the guess is per-platform, not a blanket pass-through. */
    @Test
    fun offersNothingForAPlatformItDoesNotKnow() {
        Assertions.assertNull(KnownModIds.refFor("mtlib", "SomeFuturePlatform"))
    }
}
