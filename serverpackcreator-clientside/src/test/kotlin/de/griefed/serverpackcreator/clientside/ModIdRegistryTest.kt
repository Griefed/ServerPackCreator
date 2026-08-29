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
        Assertions.assertEquals("fabric-api", KnownModIds.refFor("fabric", ModPlatforms.MODRINTH))
        Assertions.assertEquals("306612", KnownModIds.refFor("fabric", ModPlatforms.CURSEFORGE))
    }

    /** `fabric-api` is how a Quilt descriptor and some Fabric ones spell the same project. */
    @Test
    fun theApisOtherSpellingMapsToTheSameProject() {
        Assertions.assertEquals(
            KnownModIds.refFor("fabric", ModPlatforms.MODRINTH),
            KnownModIds.refFor("fabric-api", ModPlatforms.MODRINTH)
        )
    }

    /**
     * On Modrinth an unknown id is *tried* as a slug, because Modrinth resolves a project by slug or id and
     * most mod ids are their slug. That costs one lookup that may miss, which is far cheaper than not
     * resolving a dependency at all.
     */
    @Test
    fun anUnknownIdIsTriedAsAModrinthSlug() {
        Assertions.assertEquals("cloth-config", KnownModIds.refFor("cloth-config", ModPlatforms.MODRINTH))
    }

    /**
     * **CurseForge gets no guess.** It addresses projects by numeric id, so a mod id is never a valid ref;
     * guessing would mean spending the API key's quota on a search that cannot be verified from the id
     * alone. An unmappable id is reported rather than fabricated — see the refusal split.
     */
    @Test
    fun anUnknownIdIsNotGuessedOnCurseForge() {
        Assertions.assertNull(KnownModIds.refFor("cloth-config", ModPlatforms.CURSEFORGE))
    }

    /** Ids are matched case-insensitively; descriptors are hand-written and inconsistent about it. */
    @Test
    fun idsAreMatchedCaseInsensitively() {
        Assertions.assertEquals("fabric-api", KnownModIds.refFor("Fabric", ModPlatforms.MODRINTH))
    }

    /** A blank id maps nowhere rather than to a lookup that would resolve something arbitrary. */
    @Test
    fun aBlankIdMapsNowhere() {
        Assertions.assertNull(KnownModIds.refFor("   ", ModPlatforms.MODRINTH))
    }
}
