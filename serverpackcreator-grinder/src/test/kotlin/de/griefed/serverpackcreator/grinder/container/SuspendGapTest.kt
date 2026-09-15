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
package de.griefed.serverpackcreator.grinder.container

import de.griefed.serverpackcreator.clientside.SuspendAwareDeadline

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.Duration

/**
 * Pins the host-suspend detection that keeps a boot's time budget honest.
 *
 * The boot deadline is wall-clock, so a host that suspends mid-boot spends the budget on a frozen container and the
 * run is written off as a timeout even though the server never got the time. Measured 2026-07-31: a laptop
 * idle-sleeping in ~16-minute cycles produced **19 of 153 verdicts** reading `timed out`, several of them
 * `SURVIVED (timed out)` — the console showed the server reaching its ready line seconds after launch, and the wake
 * times in `pmset -g log` lined up with the grinder's log gaps to the second.
 *
 * Only the threshold is unit-testable; the surrounding [DockerJavaContainerEngine] needs a live daemon, which is
 * exactly why the decision was extracted.
 */
internal class SuspendGapTest {

    private val poll = DockerJavaContainerEngine.POLL_INTERVAL_MILLIS

    /** Ordinary polling, jitter and even multi-second stalls are not suspends. */
    @Test
    fun normalPollingIsNotMistakenForASuspend() {
        for (gap in listOf(0L, poll, poll * 2, 5_000L, 30_000L, 59_999L)) {
            Assertions.assertFalse(
                SuspendAwareDeadline.isSuspendGap(gap, poll),
                "a ${gap}ms gap between polls is load or jitter, not a suspend — treating it as one would hand a " +
                    "genuinely slow boot extra budget it should not get"
            )
        }
    }

    /** A real standby cycle is minutes long and unmistakable. */
    @Test
    fun aStandbyCycleIsDetected() {
        Assertions.assertTrue(SuspendAwareDeadline.isSuspendGap(60_000L, poll), "one minute is the floor")
        // The measured overnight cycle: ~16 minutes asleep between wakes.
        Assertions.assertTrue(SuspendAwareDeadline.isSuspendGap(Duration.ofMinutes(16).toMillis(), poll))
        Assertions.assertTrue(SuspendAwareDeadline.isSuspendGap(Duration.ofHours(8).toMillis(), poll))
    }

    /**
     * The threshold scales with the poll interval but never drops below the floor, so raising the interval cannot make
     * detection hair-trigger and lowering it cannot make an ordinary stall look like a suspend.
     */
    @Test
    fun theThresholdNeverFallsBelowItsFloor() {
        // A tiny poll interval must not lower the bar: 30 x 10ms is 300ms, far too little to mean "asleep".
        Assertions.assertFalse(SuspendAwareDeadline.isSuspendGap(1_000L, pollIntervalMillis = 10L))
        Assertions.assertTrue(SuspendAwareDeadline.isSuspendGap(60_000L, pollIntervalMillis = 10L))

        // A very large poll interval scales the bar up rather than down.
        Assertions.assertFalse(SuspendAwareDeadline.isSuspendGap(60_000L, pollIntervalMillis = 10_000L))
        Assertions.assertTrue(SuspendAwareDeadline.isSuspendGap(300_000L, pollIntervalMillis = 10_000L))
    }

    /**
     * Detection is deliberately conservative: when in doubt it reports *no* suspend, which merely preserves the old
     * behaviour (a suspended boot times out) rather than granting extra budget to a boot that is simply slow.
     */
    @Test
    fun theFloorSitsWellAboveAnyPlausibleStall() {
        Assertions.assertTrue(
            SuspendAwareDeadline.SUSPEND_GAP_FLOOR_MILLIS >= 60_000L,
            "a floor below a minute risks calling a starved host's stall a suspend"
        )
    }
}
