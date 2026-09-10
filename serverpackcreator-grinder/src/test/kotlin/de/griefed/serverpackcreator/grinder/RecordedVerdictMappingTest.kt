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
package de.griefed.serverpackcreator.grinder

import de.griefed.serverpackcreator.clientside.BootDecision
import de.griefed.serverpackcreator.clientside.Declaration
import de.griefed.serverpackcreator.clientside.DeclaredSupport
import de.griefed.serverpackcreator.clientside.JarScan
import de.griefed.serverpackcreator.clientside.Verdict
import de.griefed.serverpackcreator.grinder.report.InMemoryVerdictStore
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Pins that `Grinder.grind` carries **every** field from a `LoaderVerdict` to the `GrindVerdict` it records.
 *
 * That mapping is eighteen fields assigned by hand. Before this, `GrinderTest` — the only test that drives
 * `grind` and inspects what was stored — asserted five of them (`bootedLoader`, `declaredClientSide`,
 * `declaredServerSide`, `jarScan`, `suggestedEntry`). Every report, CSV, query and filter test builds its
 * `GrindVerdict` through the `grindVerdict(...)` fixture, so none of them can see a producer filling a field
 * wrongly — the same boundary that let a dependency-label fix pass its tests while both platforms fed the
 * labeller the wrong `slug`.
 *
 * The unasserted fields were the ones that matter most: **`verdict`** is what `/as-properties` gates
 * publication on, `declared`, `firedRule` and `decidedBy` are what make a published exclusion auditable, and
 * `filenamePattern` and `detail` are report columns. Writing `filenamePattern = verdict.suggestedEntry`, or
 * swapping `declared` for `declaredServerSide`, would have left the whole suite green.
 *
 * Every field gets a **distinct** sentinel, which is the point: equal values cannot detect a swap. The two
 * `DeclaredSupport` fields take different constants for exactly that reason, and the enums are chosen so no
 * two carry the same name. Green when written — the mapping was correct — so it is mutation-verified rather
 * than trusted: swapping any pair of assignments in `Grinder.grind` fails it.
 */
internal class RecordedVerdictMappingTest {

    private val recordedAt = Instant.parse("2026-09-05T12:00:00Z")

    /** One loader verdict with a distinct sentinel in every field the mapping copies. */
    private val sentinelVerdict = loaderVerdict(
        loader = "SENTINEL_LOADER",
        suggestedEntry = "SENTINEL_ENTRY",
        verdict = Verdict.CONFIRMED,
        note = "SENTINEL_DETAIL",
        declaredClientSide = DeclaredSupport.REQUIRED,
        declaredServerSide = DeclaredSupport.UNSUPPORTED,
        jarScan = JarScan.CLIENT,
        bootedLoader = "SENTINEL_BOOTED",
        // Sentinelled here since 2026-09-10. This guard exists to prove every field the mapping copies
        // arrives, and it used to sentinel the *derived stem* instead -- which round-tripped fine while
        // `sampleFile`, the field the report actually needs, was dropped and left `null` in the fixture.
        sampleFile = "SENTINEL_FILENAME"
    ).copy(
        declared = Declaration.SERVER,
        firedRule = "SENTINEL_RULE",
        decidedBy = BootDecision.FML_INVALID_DIST,
        stagedDependencies = listOf("SENTINEL_DEPENDENCY")
    )

    /** Grind one candidate whose every field is a sentinel, and hand back what was stored. */
    private fun recorded(): GrindVerdict {
        val store = InMemoryVerdictStore()
        val verifier = CandidateVerifier {
            clientsideReport(
                slug = "sentinel-slug",
                perLoader = listOf(sentinelVerdict),
                platform = "SENTINEL_PLATFORM",
                projectUrl = "https://modrinth.com/mod/SENTINEL_URL"
            )
        }
        val candidate = GrindCandidate(
            "https://modrinth.com/mod/SENTINEL_URL", "sentinel-slug", 0, "SENTINEL_PLATFORM", "SENTINEL_ID"
        )
        Grinder(verifier, store, clock = { recordedAt }).grind(candidate)
        return store.all().single()
    }

    /** Identity: where the row lives in the store, and what it links to. */
    @Test
    fun theIdentityFieldsAreCarried() {
        val row = recorded()

        Assertions.assertEquals("SENTINEL_PLATFORM", row.platform)
        Assertions.assertEquals("sentinel-slug", row.slug)
        Assertions.assertEquals("https://modrinth.com/mod/SENTINEL_URL", row.projectUrl)
        Assertions.assertEquals("SENTINEL_LOADER", row.loader)
        // Identity comes from the candidate, not the report: the report echoes the slug, which is the
        // mutable display name this field exists to stop depending on.
        Assertions.assertEquals("SENTINEL_ID", row.projectId)
        Assertions.assertEquals(recordedAt, row.verifiedAt)
    }

    /** The broad published stem and the sampled artifact's own name, which must not be confused. */
    @Test
    fun bothPatternsAreCarriedAndNotSwapped() {
        val row = recorded()

        Assertions.assertEquals("SENTINEL_ENTRY", row.suggestedEntry, "the broad, published stem")
        Assertions.assertEquals("SENTINEL_FILENAME", row.fileName, "the sampled artifact's own file name")
    }

    /** **The verdict itself** — the field the publication gate reads. */
    @Test
    fun theVerdictAndItsDeclarationAreCarried() {
        val row = recorded()

        Assertions.assertEquals(Verdict.CONFIRMED, row.verdict)
        Assertions.assertEquals(Declaration.SERVER, row.declared)
        Assertions.assertEquals("SENTINEL_DETAIL", row.detail)
    }

    /** The evidence behind the verdict, without which a published exclusion cannot be audited. */
    @Test
    fun theEvidenceFieldsAreCarried() {
        val row = recorded()

        Assertions.assertEquals(DeclaredSupport.REQUIRED, row.declaredClientSide)
        Assertions.assertEquals(DeclaredSupport.UNSUPPORTED, row.declaredServerSide, "must not mirror the client side")
        Assertions.assertEquals(JarScan.CLIENT, row.jarScan)
        Assertions.assertEquals("SENTINEL_BOOTED", row.bootedLoader)
        Assertions.assertEquals("SENTINEL_RULE", row.firedRule)
        Assertions.assertEquals(BootDecision.FML_INVALID_DIST.name, row.decidedBy)
        Assertions.assertEquals(listOf("SENTINEL_DEPENDENCY"), row.stagedDependencies)
    }

    /** Nothing was left behind: every sentinel appears somewhere in the recorded row. */
    @Test
    fun everySentinelSurvivesTheMapping() {
        val rendered = recorded().toString()

        listOf(
            "SENTINEL_PLATFORM", "sentinel-slug", "SENTINEL_URL", "SENTINEL_LOADER", "SENTINEL_ID",
            "SENTINEL_ENTRY", "SENTINEL_FILENAME", "SENTINEL_DETAIL", "SENTINEL_BOOTED", "SENTINEL_RULE",
            "SENTINEL_DEPENDENCY"
        ).forEach { sentinel ->
            Assertions.assertTrue(rendered.contains(sentinel), "'$sentinel' was dropped by the mapping: $rendered")
        }
    }
}
