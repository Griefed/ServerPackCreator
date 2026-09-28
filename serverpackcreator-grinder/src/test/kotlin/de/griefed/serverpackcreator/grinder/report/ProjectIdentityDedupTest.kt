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
package de.griefed.serverpackcreator.grinder.report

import de.griefed.serverpackcreator.clientside.Verdict
import de.griefed.serverpackcreator.grinder.GrindVerdict
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Pins that a verdict's identity is the *project*, not its current slug.
 *
 * The store keyed on `platform + slug + loader`, and a slug is a mutable display name: a project that renames
 * itself is a new row, its old verdicts linger under the old name, and the catalogue crawl grinds it again as if
 * unseen. Harmless while the store is scratch data; not harmless once it is a dataset anyone reads, since the same
 * mod then appears twice with possibly opposite confidences and nothing says which is current.
 *
 * Platform stays part of the identity — `jei` exists on Modrinth *and* CurseForge, and those are different projects.
 *
 * **Legacy entries matter as much as the fix.** 860 verdicts were recorded before ids existed and carry none, so
 * the id is nullable and dedup falls back to the slug when it is absent. That alone would leave a project with two
 * rows — one legacy keyed by slug, one new keyed by id — so recording an identified verdict must *supersede* the
 * unidentified row for the same slug. That is the migration: it happens as projects are re-ground, with no schema
 * step and no rewrite of a live store.
 */
internal class ProjectIdentityDedupTest {

    private fun verdict(
        slug: String,
        projectId: String? = null,
        platform: String = "Modrinth",
        loader: String = "Fabric",
        verdict: Verdict = Verdict.CLEAR,
        verifiedAt: Instant = Instant.parse("2026-07-31T12:00:00Z")
    ) = GrindVerdict(
        platform = platform,
        slug = slug,
        projectUrl = "https://modrinth.com/mod/$slug",
        loader = loader,
        suggestedEntry = null,
        verdict = verdict,
        detail = "test",
        verifiedAt = verifiedAt,
        projectId = projectId
    )

    /** The defect: a renamed project must replace its own verdict, not become a second one. */
    @Test
    fun aRenamedProjectReplacesItsVerdictInsteadOfDuplicating() {
        val store = InMemoryVerdictStore()

        store.record(verdict(slug = "old-name", projectId = "AANobbMI", verdict = Verdict.ERROR))
        store.record(verdict(slug = "new-name", projectId = "AANobbMI", verdict = Verdict.CONFIRMED))

        Assertions.assertEquals(
            1, store.all().size,
            "the same project on the same loader must hold one verdict however often it renames itself"
        )
        Assertions.assertEquals(Verdict.CONFIRMED, store.all().single().verdict, "the newer verdict wins")
        Assertions.assertEquals("new-name", store.all().single().slug, "and it carries the current slug")
    }

    /** A renamed project must also count as *seen*, or the crawl re-grinds it under its new name. */
    @Test
    fun aRenamedProjectCountsAsAlreadyGround() {
        val store = InMemoryVerdictStore()
        store.record(verdict(slug = "old-name", projectId = "AANobbMI"))

        Assertions.assertTrue(
            store.hasVerdictFor("Modrinth", "new-name", "AANobbMI"),
            "asked by project id, a renamed project is already ground — otherwise every rename costs a full re-grind"
        )
        Assertions.assertNotNull(
            store.newestVerification("Modrinth", "new-name", "AANobbMI"),
            "and its freshness must be found, so the TTL skip still applies after a rename"
        )
    }

    /** Recording an identified verdict supersedes the pre-id row for that slug: the migration, done in place. */
    @Test
    fun anIdentifiedVerdictSupersedesTheLegacyRowForTheSameSlug() {
        val store = InMemoryVerdictStore()

        store.record(verdict(slug = "balm", projectId = null, verdict = Verdict.ERROR))
        store.record(verdict(slug = "balm", projectId = "MQ4RtcVI", verdict = Verdict.INCONCLUSIVE))

        Assertions.assertEquals(
            1, store.all().size,
            "a legacy slug-keyed row and its identified replacement are the same project, not two"
        )
        Assertions.assertEquals("MQ4RtcVI", store.all().single().projectId, "the identified verdict is the one kept")
    }

    /** Without ids on either side the old behaviour must hold exactly — 860 stored verdicts depend on it. */
    @Test
    fun verdictsWithoutIdsStillDedupBySlug() {
        val store = InMemoryVerdictStore()

        store.record(verdict(slug = "jei", verdict = Verdict.ERROR))
        store.record(verdict(slug = "jei", verdict = Verdict.CONFIRMED))

        Assertions.assertEquals(1, store.all().size, "slug fallback must still dedup")
        Assertions.assertEquals(Verdict.CONFIRMED, store.all().single().verdict)
        Assertions.assertTrue(store.hasVerdictFor("Modrinth", "jei"), "and still be found without an id")
    }

    /** Platform remains part of the identity, and different projects that share a slug stay separate. */
    @Test
    fun theSameSlugOnDifferentPlatformsStaysSeparate() {
        val store = InMemoryVerdictStore()

        store.record(verdict(slug = "jei", projectId = "u6dRKJwZ", platform = "Modrinth"))
        store.record(verdict(slug = "jei", projectId = "238222", platform = "CurseForge"))

        Assertions.assertEquals(2, store.all().size, "same name, different platforms, different projects")
        Assertions.assertTrue(
            store.hasVerdictFor("Modrinth", "jei", "u6dRKJwZ"),
            "Modrinth's project is found under Modrinth's id"
        )
        Assertions.assertTrue(
            store.hasVerdictFor("CurseForge", "jei", "238222"),
            "CurseForge's project is found under CurseForge's id"
        )
        Assertions.assertFalse(
            store.hasVerdictFor("CurseForge", "jei", "u6dRKJwZ"),
            "and one platform's id must not match the other's project, even though the slug is identical"
        )
    }

    /** Loaders stay separate rows for one project — the per-loader verdict is the unit of the report. */
    @Test
    fun oneProjectKeepsOneVerdictPerLoader() {
        val store = InMemoryVerdictStore()

        store.record(verdict(slug = "balm", projectId = "MQ4RtcVI", loader = "Fabric"))
        store.record(verdict(slug = "balm", projectId = "MQ4RtcVI", loader = "Forge"))

        Assertions.assertEquals(2, store.all().size, "a project is verified per loader and reported per loader")
    }
}
