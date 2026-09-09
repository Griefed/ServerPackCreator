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
 * Pins **who a prevented grind is blamed on**, which is the one thing [Verdict.ERROR] promises and was not
 * keeping.
 *
 * Its own contract reads *"An operator's problem, never evidence about the mod"* — and it was carrying three
 * unrelated things: the host being broken, CurseForge withholding a download, and a loader/Minecraft
 * combination nothing upstream ever published for. Only the first is an operator's problem. The other two
 * are permanent, actionable by nobody, and mixed into the one bucket an operator is expected to read and
 * fix, which is what makes the bucket unreadable.
 *
 * **Measured on `grinder.serverpackcreator.de`, 2026-09-09**, over its 53 `ERROR` rows:
 *
 * | Cause | Rows |
 * |---|---|
 * | The mod's own file is distribution-locked (`corail-tombstone`, `entityculling`, `not-enough-animations`, `skin-layers-3d`, `structory`) | 15 |
 * | A required dependency is distribution-locked (`better-combat-by-daedelus`) | 2 |
 * | Upstream published nothing for the loader and Minecraft being booted | ~14 |
 * | The jar carries only another loader's descriptor — the author mis-ticked the web form | 4 |
 * | Genuinely ours | the rest |
 *
 * These guards deliberately assert what a prevented grind is **not**, because that is the whole claim they
 * can make without naming the verdicts that replace it — and it stays the claim worth guarding afterwards:
 * whatever the vocabulary grows into, a CurseForge opt-out must never be filed as ServerPackCreator's
 * failure. `PreventionCauseVerdictTest` is where the positive answers live.
 *
 * @author Griefed
 */
internal class PreventedGrindBlameTest {

    /** A refusal exactly as staging reports it: no container ran, and the detail says why. */
    private fun refusedWith(detail: String) = BootVerifier.BootOutcome(
        BootResult.INCONCLUSIVE, null, detail, stagingPrevented = true
    )

    private fun verdictFor(detail: String, jarScan: JarScan = JarScan.ERROR) = ClientsideVerifier.verdictOf(
        serverSide = DeclaredSupport.UNKNOWN,
        clientSide = DeclaredSupport.UNKNOWN,
        jarScan = jarScan,
        bootOutcome = refusedWith(detail),
        bootAttempted = true
    ).verdict

    /**
     * `corail-tombstone`, `entityculling`, `not-enough-animations`, `skin-layers-3d` and `structory`: the
     * author set `allowModDistribution=false`, so CurseForge publishes no download URL and there is nothing
     * to fetch. Nothing about that is ours, and no operator can fix it.
     */
    @Test
    fun aDistributionLockedFileIsNotOurFailure() {
        Assertions.assertNotEquals(
            Verdict.ERROR,
            verdictFor(
                "Could not download tombstone-forge-26.2-9.9.3.jar: the file is distribution-locked " +
                    "(allowModDistribution=false), so CurseForge publishes no download URL for it.",
                jarScan = JarScan.DEFERRED
            ),
            "a CurseForge opt-out is CurseForge's decision, not a defect in ServerPackCreator"
        )
    }

    /**
     * `better-combat-by-daedelus` on Fabric and NeoForge: the mod itself is obtainable and
     * `player-animation-library` is not, so the pack can never be assembled. Same cause, one level down.
     */
    @Test
    fun aDistributionLockedDependencyIsNotOurFailureEither() {
        Assertions.assertNotEquals(
            Verdict.ERROR,
            verdictFor(
                "Required dependency unavailable for Fabric / Minecraft 26.2: player-animation-library " +
                    "(distribution-locked on CurseForge). Not booting."
            ),
            "the pack cannot be assembled and nobody involved can change that"
        )
    }

    /**
     * `cobblemon-additions` on Fabric 1.21.11 needs `cobblemon`, whose newest Fabric build is 1.21.1;
     * `shatterbyte-lib` on Quilt 1.21.1 needs QSL, whose last release is Minecraft 1.21 and which is
     * discontinued. Neither is a grind that failed — it is a grind that was never possible.
     */
    @Test
    fun anUpstreamGapIsNotOurFailure() {
        Assertions.assertNotEquals(
            Verdict.ERROR,
            verdictFor(
                "Required dependency unavailable for Fabric / Minecraft 1.21.11: cobblemon (nothing " +
                    "published for this loader and Minecraft version). Not booting."
            ),
            "a dependency nobody ever published is not a host problem and not evidence about the mod"
        )
    }

    /**
     * And the host's own trouble stays exactly where it was, or the split has achieved nothing: `ERROR` has
     * to keep meaning "somebody can go and fix this".
     */
    @Test
    fun theHostsOwnTroubleIsStillAnError() {
        Assertions.assertEquals(
            Verdict.ERROR,
            verdictFor("Server-pack generation failed for Fabric 26.2."),
            "generation is ours, and an operator reads this column to find out what broke"
        )
        Assertions.assertEquals(
            Verdict.ERROR,
            verdictFor("Pack post-processing failed: no such loader install in the cache"),
            "a broken loader cache is the missing-runtime-image shape and must stay visible"
        )
    }
}
