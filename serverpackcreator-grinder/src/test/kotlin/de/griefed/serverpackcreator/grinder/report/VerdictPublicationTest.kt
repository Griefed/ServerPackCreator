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
import de.griefed.serverpackcreator.grinder.grindVerdict
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Pins what the redesigned verdict actually publishes, which is the only part of this engine users see.
 *
 * `/as-properties` feeds an SPC instance's `fallback.updateurl`, so a row reaching it becomes a mod excluded
 * from real server packs. That makes it the highest-stakes output here, and the gate is now exactly one
 * thing: **[Verdict.CONFIRMED]**.
 *
 * The old gate was `Verdict.CONFIRMED` *and* a separate check that the deciding rung was decisive — two
 * conditions because HIGH alone was reachable from the bare exit-code rung, which means only "exited
 * non-zero, nothing recognised why". Measured against the live daemon, 27 of 43 published HIGH verdicts
 * rested on no decisive evidence at all. Under the redesign that second condition is structural rather than
 * a guard bolted on top: `verdictOf` only ever reaches CONFIRMED from a decisive rung, so CONFIRMED *means*
 * decisive and the gate needs to ask once.
 *
 * **A metadata declaration no longer publishes anything**, which is the deliberate narrowing: a mod is
 * excluded because a boot's console proved it, never because the mod said so about itself.
 */
internal class VerdictPublicationTest {

    /**
     * The renderer merges the shipped list and an operator whitelist with the ground findings; both are
     * empty here so the assertions speak only about what the *verdicts* contributed.
     */
    private fun render(verdicts: Collection<GrindVerdict>) =
        FallbackPropertiesRenderer.render(emptyList(), emptyList(), verdicts)


    /** Only a confirmation is published; every other verdict is withheld. */
    @Test
    fun onlyConfirmedIsPublished() {
        val rendered = render(
            listOf(
                grindVerdict("confirmed-mod", "Forge", verdict = Verdict.CONFIRMED),
                grindVerdict("clear-mod", "Forge", verdict = Verdict.CLEAR),
                grindVerdict("error-mod", "Forge", verdict = Verdict.ERROR),
                grindVerdict("unclear-mod", "Forge", verdict = Verdict.INCONCLUSIVE)
            )
        )

        Assertions.assertTrue(rendered.contains("confirmed-mod-"), rendered)
        listOf("clear-mod-", "error-mod-", "unclear-mod-").forEach {
            Assertions.assertFalse(rendered.contains(it), "$it must not be published:\n$rendered")
        }
    }

    /**
     * **An ERROR must never publish, and this is the guard that matters most.** ERROR means the grind could
     * not be performed — no runtime image, a staging refusal, a failed download. During the
     * missing-runtime-image outage every candidate produced exactly this shape, and a gate that leaked it
     * would exclude mods from users' packs on the strength of a broken Docker host.
     */
    @Test
    fun anErrorNeverPublishesHoweverManyThereAre() {
        val rendered = render((1..20).map { grindVerdict("mod$it", "Forge", verdict = Verdict.ERROR) })

        (1..20).forEach {
            Assertions.assertFalse(rendered.contains("mod$it-"), "an outage published mod$it:\n$rendered")
        }
    }

    /**
     * Retention is asked of the verdict itself rather than re-derived, so the artifacts and the outcome that
     * justified keeping them cannot drift apart. Only a clean boot has nothing worth reading.
     */
    @Test
    fun everyVerdictButClearKeepsItsLogs() {
        Assertions.assertTrue(Verdict.CONFIRMED.keepsLogs, "a published exclusion has to stay auditable")
        Assertions.assertTrue(Verdict.ERROR.keepsLogs, "an admin reads these to find what broke")
        Assertions.assertTrue(Verdict.INCONCLUSIVE.keepsLogs, "the next rule is extracted from these")
        Assertions.assertFalse(Verdict.CLEAR.keepsLogs, "a clean boot has nothing to investigate")
    }

    /**
     * A verdict stored by the old schema carries no `verdict` field. It must load as
     * [Verdict.INCONCLUSIVE] — kept, so nothing is lost and the re-verify TTL will re-grind it, but
     * publishing nothing in the meantime.
     *
     * That is the "start clean" decision expressed without deleting anything: the old `Confidence` scale has
     * no honest mapping onto the new verdicts, so no old row may be treated as evidence, and every one of
     * them is re-earned by a real boot rather than translated.
     */
    @Test
    fun aVerdictFromTheOldSchemaPublishesNothingUntilItIsReGround() {
        val legacy = grindVerdict("legacy-mod", "Forge")

        Assertions.assertEquals(
            Verdict.INCONCLUSIVE, legacy.verdict,
            "an unmigrated row must default to the verdict that claims nothing"
        )
        Assertions.assertFalse(
            render(listOf(legacy)).contains("legacy-mod-"),
            "an old-scale row must not publish on the strength of a confidence this engine no longer means"
        )
    }
}
