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
 * Pins the four-state verdict the redesigned result-system publishes, and above all **the distinction the
 * old one could not make**: a grind that was *prevented* is not a grind that *learned nothing*.
 *
 * The old scheme reported both as `INCONCLUSIVE`, and this project has paid for that conflation twice.
 * When `spc-grinder-runtime:latest` went missing from the Docker daemon, every candidate published
 * INCONCLUSIVE "about a boot that never happened", overwriting decisive HIGH verdicts the 30-day TTL would
 * otherwise have left alone. The `advancement-plaques` and JEI refusals did the same thing one candidate at
 * a time. In every case the engine knew perfectly well that nothing had run — it simply had no verdict that
 * could say so.
 *
 * [Verdict.ERROR] is that verdict. It means *the grind could not be performed*: no runtime image, a refusal
 * before the container started, a download that failed, a pack that would not generate. It is an operator's
 * problem, its logs are kept for an admin to read, and it must never be mistaken for evidence about the mod.
 *
 * [Verdict.INCONCLUSIVE] keeps its honest meaning: the boot **ran** and did something unexpected — a
 * non-zero exit, a crash, a timeout, a kill — with nothing confirming why. Its logs are kept too, because
 * that log is the raw material a new rule gets written from.
 *
 * [Verdict.CONFIRMED] is reached only by a rule match: the mod is exclusion-worthy. [Verdict.CLEAR] is the
 * other decisive outcome — the server booted, nothing matched, so the mod is proven server-safe. Keeping
 * CLEAR separate from INCONCLUSIVE is what preserves the difference between "we proved it is fine" and "we
 * learned nothing", which a single bucket destroys.
 */
internal class VerdictPolicyTest {

    /**
     * The missing-runtime-image outage, and the shape of every staging refusal since. Nothing ran, so no
     * statement about the mod is possible and the verdict must say so rather than imply a failed boot.
     */
    @Test
    fun aGrindThatCouldNotRunIsAnError() {
        Assertions.assertEquals(
            Verdict.ERROR,
            VerdictPolicy.decide(
                staging = StagingOutcome.Prevented("No such image: spc-grinder-runtime:latest"),
                boot = null,
                confirmedByRule = null
            ),
            "a boot that never happened is an operator problem, not evidence about the mod"
        )
    }

    /** A rule match is the only route to CONFIRMED — the mod is exclusion-worthy. */
    @Test
    fun aRuleMatchConfirms() {
        val verdict = VerdictPolicy.decide(
            staging = StagingOutcome.Staged,
            boot = BootObservation.Crashed(exitCode = 1),
            confirmedByRule = "fml-invalid-dist"
        )

        Assertions.assertEquals(Verdict.CONFIRMED, verdict)
    }

    /**
     * **The ladder, preserved.** An excuse-rule fires on a crash that proves nothing about sideness — a
     * missing dependency, a sandboxed network call. It must land on INCONCLUSIVE, never CONFIRMED, or the
     * engine converts host trouble into a clientside verdict. This is the inversion a flat
     * "matches a rule means exclusion-worthy" reading would introduce.
     */
    @Test
    fun aCrashWithNoConfirmingRuleIsInconclusiveNotConfirmed() {
        val verdict = VerdictPolicy.decide(
            staging = StagingOutcome.Staged,
            boot = BootObservation.Crashed(exitCode = 1),
            confirmedByRule = null
        )

        Assertions.assertEquals(
            Verdict.INCONCLUSIVE, verdict,
            "a crash on its own is not evidence of sideness — only a rule makes it so"
        )
    }

    /** A clean boot with nothing matched is the positive result: proven server-safe. */
    @Test
    fun aCleanBootThatMatchesNothingIsClear() {
        Assertions.assertEquals(
            Verdict.CLEAR,
            VerdictPolicy.decide(StagingOutcome.Staged, BootObservation.Survived, confirmedByRule = null)
        )
    }

    /**
     * A timeout or a kill ran the container but proved nothing. It is INCONCLUSIVE rather than ERROR: the
     * grind *was* performed, and the log is worth keeping because a rule may yet be extracted from it.
     */
    @Test
    fun aTimeoutIsInconclusiveBecauseTheGrindDidRun() {
        Assertions.assertEquals(
            Verdict.INCONCLUSIVE,
            VerdictPolicy.decide(StagingOutcome.Staged, BootObservation.TimedOut, confirmedByRule = null)
        )
    }

    /**
     * Staging succeeded but the boot was never observed — the container itself failed to start. Nothing
     * ran, so this is an operator problem however late it surfaced.
     */
    @Test
    fun aStagedGrindWithNoObservationIsAnError() {
        Assertions.assertEquals(
            Verdict.ERROR,
            VerdictPolicy.decide(StagingOutcome.Staged, boot = null, confirmedByRule = null)
        )
    }

    /**
     * A rule may not rescue a grind that never ran. If nothing booted there is no console for a rule to
     * have matched, so a confirmation arriving alongside a prevented grind is a bug in the caller, and the
     * policy must not launder it into evidence.
     */
    @Test
    fun aRuleCannotConfirmAGrindThatNeverRan() {
        Assertions.assertEquals(
            Verdict.ERROR,
            VerdictPolicy.decide(
                staging = StagingOutcome.Prevented("download failed"),
                boot = null,
                confirmedByRule = "fml-invalid-dist"
            ),
            "nothing ran, so there was no console to match — ERROR outranks a stray confirmation"
        )
    }

    /**
     * Every verdict that keeps logs says so itself, so the reaper needs no second opinion.
     *
     * **Only CLEAR discards.** ERROR and INCONCLUSIVE keep logs because that was the requirement — an admin
     * reads the first, a rule is extracted from the second. CONFIRMED keeps them too, which the requirement
     * did not ask for and which is deliberate: a confirmation is what publishes a mod to the fallback list,
     * the highest-stakes output this engine has, and *a verdict that cannot name its own evidence cannot be
     * audited*. The rule id says which rule fired; only the console says what it fired on. A clean boot is
     * the one outcome with nothing to investigate.
     */
    @Test
    fun everyVerdictButClearKeepsItsLogs() {
        Assertions.assertTrue(Verdict.ERROR.keepsLogs, "an admin has to be able to read why a grind failed")
        Assertions.assertTrue(Verdict.INCONCLUSIVE.keepsLogs, "the log is what a new rule is extracted from")
        Assertions.assertTrue(Verdict.CONFIRMED.keepsLogs, "a published exclusion has to remain auditable")
        Assertions.assertFalse(Verdict.CLEAR.keepsLogs, "a clean boot has nothing to investigate")
    }
}
