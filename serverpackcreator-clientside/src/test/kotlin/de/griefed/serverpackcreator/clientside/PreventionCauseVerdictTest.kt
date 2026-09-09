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
 * Pins that a grind nobody could have performed is **not** reported as our own failure.
 *
 * [Verdict.ERROR]'s own contract is *"An operator's problem, never evidence about the mod"*, and it was
 * carrying three unrelated things: the host being broken, CurseForge withholding a download, and a
 * loader/Minecraft combination nothing upstream ever published for. Only the first is an operator's
 * problem. The other two are permanent, actionable by nobody, and mixed into the one bucket an operator is
 * expected to read and fix — so the bucket cannot be read.
 *
 * **Measured on `grinder.serverpackcreator.de`, 2026-09-09**, over its 53 `ERROR` rows:
 *
 * | Cause | Rows | Belongs in |
 * |---|---|---|
 * | The mod's own file is distribution-locked (`corail-tombstone`, `entityculling`, `not-enough-animations`, `skin-layers-3d`, `structory`) | 15 | [Verdict.LOCKED] |
 * | A required dependency is distribution-locked (`better-combat-by-daedelus`) | 2 | [Verdict.LOCKED] |
 * | Upstream published nothing for the loader and Minecraft being booted | ~14 | [Verdict.UNVERIFIABLE] |
 * | The jar carries only another loader's descriptor — the author mis-ticked the web form | 4 | [Verdict.UNVERIFIABLE] |
 *
 * `LOCKED` is separate from `UNVERIFIABLE` rather than folded into it because a distribution opt-out is a
 * *named, identifiable* fact with a URL behind it, and one an author can reverse; "nothing published for
 * this combination" is neither. Both keep the property that matters: they are not `ERROR`, so what remains
 * in `ERROR` is what somebody can go and fix.
 *
 * @author Griefed
 */
internal class PreventionCauseVerdictTest {

    private fun prevented(cause: PreventionCause) = VerdictPolicy.decide(
        staging = StagingOutcome.Prevented("whatever the report says", cause),
        boot = null,
        confirmedByRule = null
    )

    /** A CurseForge opt-out is CurseForge's decision, and the verdict says so. */
    @Test
    fun aDistributionOptOutIsLocked() {
        Assertions.assertEquals(Verdict.LOCKED, prevented(PreventionCause.DISTRIBUTION_LOCKED))
    }

    /** So is a combination nothing upstream published for — nobody's defect, and nothing about the mod. */
    @Test
    fun anUpstreamGapIsUnverifiable() {
        Assertions.assertEquals(Verdict.UNVERIFIABLE, prevented(PreventionCause.UPSTREAM_UNAVAILABLE))
    }

    /** And what is left in ERROR is what an operator can actually act on. */
    @Test
    fun ourOwnFailureIsStillAnError() {
        Assertions.assertEquals(Verdict.ERROR, prevented(PreventionCause.HOST))
    }

    /**
     * A prevented grind is still decided **before** any rule is consulted, whatever prevented it: nothing
     * ran, so no console existed for a rule to have matched, and a confirmation arriving alongside one is a
     * caller bug that must not be laundered into evidence.
     */
    @Test
    fun aPreventedGrindIsNeverConfirmedByARule() {
        PreventionCause.entries.forEach { cause ->
            Assertions.assertNotEquals(
                Verdict.CONFIRMED,
                VerdictPolicy.decide(
                    staging = StagingOutcome.Prevented("nothing ran", cause),
                    boot = BootResult.CRASHED,
                    confirmedByRule = "some-rule"
                ),
                "$cause ran no container, so nothing can have confirmed anything"
            )
        }
    }

    /** A staged grind that never reported a boot is the host's problem and stays exactly as it was. */
    @Test
    fun aContainerThatNeverStartedIsStillAnError() {
        Assertions.assertEquals(
            Verdict.ERROR,
            VerdictPolicy.decide(StagingOutcome.Staged, boot = null, confirmedByRule = null)
        )
    }

    /**
     * Neither verdict keeps artifacts, because neither has any: no container ran, so there is no console to
     * read and nothing for a future rule to be written from.
     */
    @Test
    fun neitherNewVerdictKeepsLogs() {
        Assertions.assertFalse(Verdict.LOCKED.keepsLogs, "there is no download, so there is no boot to read")
        Assertions.assertFalse(Verdict.UNVERIFIABLE.keepsLogs, "nothing ran; the detail is the whole story")
    }

    // --- which unmet dependency produces which cause ---------------------------------------------------

    /** One locked dependency makes the whole refusal a distribution opt-out. */
    @Test
    fun aLockedDependencyMakesTheRefusalLocked() {
        Assertions.assertEquals(
            PreventionCause.DISTRIBUTION_LOCKED,
            BootVerifier.preventionCauseFor(mapOf("player-animation-library" to UnmetReason.DISTRIBUTION_LOCKED))
        )
    }

    /** A dependency upstream never published is unverifiable, whether the ref resolved or not. */
    @Test
    fun anUnpublishedDependencyMakesTheRefusalUnverifiable() {
        Assertions.assertEquals(
            PreventionCause.UPSTREAM_UNAVAILABLE,
            BootVerifier.preventionCauseFor(mapOf("cobblemon" to UnmetReason.NO_USABLE_FILE))
        )
        Assertions.assertEquals(
            PreventionCause.UPSTREAM_UNAVAILABLE,
            BootVerifier.preventionCauseFor(mapOf("LNytGWDc" to UnmetReason.UNRESOLVED))
        )
    }

    /**
     * **Our own failures win the fold**, because they are the only ones anybody can act on. A refusal
     * mixing a transient download failure with a permanent upstream gap has to reach the operator who can
     * retry the download; filing it as unverifiable would hide the one half that is fixable.
     */
    @Test
    fun ourOwnFailureOutranksEveryOtherCause() {
        Assertions.assertEquals(
            PreventionCause.HOST,
            BootVerifier.preventionCauseFor(
                mapOf(
                    "balm" to UnmetReason.DOWNLOAD_FAILED,
                    "cobblemon" to UnmetReason.NO_USABLE_FILE,
                    "player-animation-library" to UnmetReason.DISTRIBUTION_LOCKED
                )
            ),
            "a failed download is retryable and must not be filed behind two permanent facts"
        )
        Assertions.assertEquals(
            PreventionCause.HOST,
            BootVerifier.preventionCauseFor(mapOf("yacl" to UnmetReason.DROPPED_BY_BACKTRACK)),
            "the backtrack dropped those builds itself; that is our doing"
        )
    }

    /**
     * **An empty set is our problem, not an exception.**
     *
     * `first {}` over the causes present throws `NoSuchElementException` for an empty map, and the only
     * caller guards it — which is exactly the shape this module has paid for before: `UnmetReason.explain`
     * returned `null` for a value no caller could produce, and two log sites would have printed the literal
     * `null` after some later edit. A helper whose name reads total has to be total.
     *
     * `HOST` is the answer for the same reason it is every prevention default: the loud, actionable reading
     * is the safe one when nothing said otherwise.
     */
    @Test
    fun anEmptyUnmetSetIsOurProblem() {
        Assertions.assertEquals(
            PreventionCause.HOST,
            BootVerifier.preventionCauseFor(emptyMap()),
            "a fold over nothing must answer, not throw"
        )
    }

    /**
     * And a locked dependency outranks a merely-unpublished one, because it is the half a reader can chase:
     * it names a project, a file and an author's decision, where the other names an absence.
     */
    @Test
    fun aNamedOptOutOutranksAnAbsence() {
        Assertions.assertEquals(
            PreventionCause.DISTRIBUTION_LOCKED,
            BootVerifier.preventionCauseFor(
                mapOf(
                    "cobblemon" to UnmetReason.NO_USABLE_FILE,
                    "player-animation-library" to UnmetReason.DISTRIBUTION_LOCKED
                )
            )
        )
    }
}
