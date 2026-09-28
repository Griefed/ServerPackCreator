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
 * Pins that a **resolved dependency knows its own name**, through the real platform code.
 *
 * Reported twice. `unsatisfiedLabel` was written to name a resolved project by its slug, and
 * `DependencyLabelTest` proves it does — by handing it a `ProjectFiles` built in the test with the slug
 * already correct. Production never builds one of those: **both** platforms' `resolveDependency` passed
 * `nativeRef` into the `slug` parameter positionally, so the label resolved the project, read back the ref
 * it started from, and printed it. The fix to the labeller was a no-op for the case that actually fires.
 *
 * Measured on the live daemon, 2026-09-04, both rows `ERROR` on CurseForge:
 *
 *  - `architectury-api` — *"Required dependency unavailable for Quilt / Minecraft 1.20.4: **306612**"*
 *    (Fabric API)
 *  - `waystones` — *"Required dependency unavailable for Forge / Minecraft 1.21.11: **531761**"* (Balm)
 *
 * **The lesson is the test boundary, not the bug.** A unit test that constructs the value under test
 * cannot see a producer that constructs it wrongly — the same shape as the loader step-down whose pin
 * injected the versions it was meant to prove were fetched. So these drive the real `resolveDependency`
 * with canned JSON, and the last one asserts the composition end to end: it is the only arrangement in
 * which a positional-argument slip in either platform fails a test.
 */
internal class DependencySlugTest {

    /** CurseForge answers `/mods/{id}` with the project (slug included) and `/mods/{id}/files` with files. */
    private val curseForge = CurseForgePlatform(
        "test-key",
        HttpFetcher { url, _ ->
            when {
                url.contains("/files") -> """{"data": []}"""
                url.contains("/mods/306612") -> """
                    {"data": {"id": 306612, "slug": "fabric-api",
                              "links": {"websiteUrl": "https://www.curseforge.com/minecraft/mc-mods/fabric-api"}}}
                """.trimIndent()
                else -> throw IllegalStateException("unexpected url $url")
            }
        }
    )

    /** Modrinth answers `/project/{ref}` with the project and `/project/{ref}/version` with versions. */
    private val modrinth = ModrinthPlatform(
        HttpFetcher { url, _ ->
            when {
                url.endsWith("/version") -> "[]"
                url.contains("/project/P7dR8mSH") -> """{"slug": "fabric-api", "client_side": "required"}"""
                else -> throw IllegalStateException("unexpected url $url")
            }
        }
    )

    /** CurseForge already fetches the project for its `websiteUrl`; the slug is in the same response. */
    @Test
    fun curseForgeNamesAResolvedDependencyByItsSlug() {
        Assertions.assertEquals("fabric-api", curseForge.resolveDependency("306612")?.slug)
    }

    /** Modrinth's `project_id` is opaque base62, so the ref is never a name a reader recognises. */
    @Test
    fun modrinthNamesAResolvedDependencyByItsSlug() {
        Assertions.assertEquals("fabric-api", modrinth.resolveDependency("P7dR8mSH")?.slug)
    }

    /**
     * **The composition, which is what actually broke.** Neither the labeller nor the platform is wrong in
     * isolation; the defect only exists where one feeds the other.
     */
    @Test
    fun theRefusalNamesTheModAcrossBothPlatforms() {
        listOf(
            Triple(curseForge, "306612", "CurseForge"),
            Triple(modrinth, "P7dR8mSH", "Modrinth")
        ).forEach { (platform, ref, platformName) ->
            val label = BootVerifier.unsatisfiedLabel(ref, platform.resolveDependency(ref), platformName)

            Assertions.assertEquals("fabric-api", label, "$platformName must name the mod, not its id")
            Assertions.assertFalse(label.contains(ref), "the raw ref must not survive into the refusal: $label")
        }
    }

    /**
     * A project the platform cannot resolve still keeps the ref — it is all there is — and still says where
     * to look it up. The fix must not turn a diagnosable refusal into a nameless one.
     */
    @Test
    fun anUnresolvableRefStillReportsTheRefAndItsPlatform() {
        val label = BootVerifier.unsatisfiedLabel("306612", null, "CurseForge")

        Assertions.assertTrue(label.contains("306612") && label.contains("CurseForge"), label)
    }
}
