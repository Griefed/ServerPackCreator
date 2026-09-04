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
 * Pins that a refused boot names its missing dependencies in words a human can act on.
 *
 * Reported live: `architectury-api`, `enchantment-descriptions` and `waystones` refused with "weird strings"
 * as the missing dependency. They are **platform refs** — `unsatisfied.add(dependencyRef)` records whatever
 * `ModFile.requiredDependencies` holds, which for Modrinth is the opaque base62 `project_id` and for
 * CurseForge a bare numeric id.
 *
 * Measured against the live API on 2026-09-04:
 *
 *  - `enchantment-descriptions` requires `uy4Cnpcm` and `aaRl8GiW` — **bookshelf-lib** and **prickle**
 *  - `waystones` requires `bi4iCmsw` and `MBAkmtvl` — **shogi** and **balm**
 *
 * **The sharpest demonstration is `waystones`**: its own `neoforge.mods.toml` declares `balm` and `shogi` in
 * plain words, and the manifest path reports them that way — while the platform path reports the very same
 * two mods as `MBAkmtvl` and `bi4iCmsw`. One refusal, two vocabularies, one of them unreadable.
 *
 * Resolving the ref also **improves deduplication**, which is the part that is not merely cosmetic:
 * `unsatisfied` is a `Set<String>`, so `MBAkmtvl` and `balm` are two entries today and one entry once both
 * sides speak slugs.
 */
internal class DependencyLabelTest {

    private fun project(slug: String) = ProjectFiles(
        platform = "Modrinth",
        slug = slug,
        projectUrl = "https://modrinth.com/mod/$slug",
        clientSide = DeclaredSupport.UNKNOWN,
        serverSide = DeclaredSupport.UNKNOWN,
        files = emptyList()
    )

    /** A ref we resolved is reported by its slug — the name the author and the operator both use. */
    @Test
    fun aResolvedRefIsNamedByItsSlug() {
        Assertions.assertEquals(
            "balm",
            BootVerifier.unsatisfiedLabel("MBAkmtvl", project("balm"), "Modrinth")
        )
    }

    /**
     * A ref we could **not** resolve keeps the ref — it is all we have — but says what it is, so a reader
     * does not mistake an opaque id for a mod name that simply looks strange.
     */
    @Test
    fun anUnresolvedRefSaysWhatItIs() {
        val label = BootVerifier.unsatisfiedLabel("ok4syTXP", null, "Modrinth")

        Assertions.assertTrue(label.contains("ok4syTXP"), label)
        Assertions.assertTrue(label.contains("Modrinth"), "the reader has to know where to look it up: $label")
    }

    /**
     * The platform and manifest halves must agree, or the same missing mod is reported twice under two
     * names. `waystones` is exactly that case.
     */
    @Test
    fun theSameMissingModIsReportedOnceAcrossBothHalves() {
        val unsatisfied = sortedSetOf<String>()
        // The platform half, resolved.
        unsatisfied.add(BootVerifier.unsatisfiedLabel("MBAkmtvl", project("balm"), "Modrinth"))
        // The manifest half, which has always been readable.
        unsatisfied.add("balm")

        Assertions.assertEquals(
            listOf("balm"), unsatisfied.toList(),
            "one missing mod must be one entry, however it was discovered"
        )
    }

    /** End to end: the refusal an operator reads names mods, not ids. */
    @Test
    fun theRefusalNamesModsNotIds() {
        val refusal = BootVerifier.refuseForMissingDependencies(
            setOf(
                BootVerifier.unsatisfiedLabel("uy4Cnpcm", project("bookshelf-lib"), "Modrinth"),
                BootVerifier.unsatisfiedLabel("aaRl8GiW", project("prickle"), "Modrinth")
            ),
            "NeoForge", "1.21.11"
        )

        val detail = refusal?.detail.orEmpty()
        Assertions.assertTrue(detail.contains("bookshelf-lib") && detail.contains("prickle"), detail)
        Assertions.assertFalse(detail.contains("uy4Cnpcm"), "the opaque id must not reach the report: $detail")
    }
}
