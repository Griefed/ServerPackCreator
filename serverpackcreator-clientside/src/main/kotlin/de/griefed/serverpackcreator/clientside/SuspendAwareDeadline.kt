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

import java.time.Duration

/**
 * A boot timeout that is not spent while the host is asleep.
 *
 * A plain wall-clock deadline expires on a server that never got the time: suspend the machine mid-boot and the
 * boot is scored as having timed out when in truth it was frozen. This tracks the gap between successive polls and
 * adds any interval too large to be anything but a suspend back onto the deadline, so the timeout means *"the boot
 * had this long and did not make it"* rather than *"this much clock passed"*.
 *
 * Used by both boot paths — `HostProcessServerRunner` here and the grinder's `DockerJavaContainerEngine` — which
 * are integration-shaped and untestable directly; keeping the arithmetic here, with an injected [now], is what
 * makes the threshold verifiable at all.
 *
 * Not thread-safe: one instance belongs to one boot loop.
 *
 * @param timeout             The budget the boot is given, excluding any time the host spends suspended.
 * @param pollIntervalMillis  How often the caller polls, which sets what counts as an impossible gap.
 * @param now                 Millisecond clock, injected so tests can move it by hand.
 * @param onSuspendDetected   Called once per detected suspend with its length in millis, for operator-facing
 *                            logging. Defaults to doing nothing so a caller that does not care stays simple.
 * @author Griefed
 */
class SuspendAwareDeadline(
    timeout: Duration,
    private val pollIntervalMillis: Long,
    private val now: () -> Long = System::currentTimeMillis,
    private val onSuspendDetected: (Long) -> Unit = {}
) {
    private var deadlineMillis: Long = now() + timeout.toMillis()
    private var lastTickMillis: Long = now()

    /** Whether budget remains. Checked as the boot loop's condition, exactly like a wall-clock comparison. */
    fun hasTimeLeft(): Boolean = now() < deadlineMillis

    /**
     * Records one completed poll. Any gap since the previous tick that is too large to be mere slowness is treated
     * as the host having slept and is handed back to the deadline, reporting it through [onSuspendDetected].
     */
    fun tick() {
        val currentMillis = now()
        val gapMillis = currentMillis - lastTickMillis
        if (isSuspendGap(gapMillis, pollIntervalMillis)) {
            deadlineMillis += gapMillis
            onSuspendDetected(gapMillis)
        }
        lastTickMillis = currentMillis
    }

    /**
     * The suspend-detection threshold, kept out of the class so both boot paths — the host process runner and the
     * grinder's container engine — are measured against the same number rather than each picking one.
     */
    companion object {
        /**
         * Smallest wall-clock gap between two polls that is read as the host having suspended rather than merely
         * being busy. Generous on purpose: scheduling jitter, a starved container host or a long GC pause can cost
         * seconds, but nothing short of a suspend costs a minute between two 500 ms polls. Under-detecting is the
         * safe direction — it only means a suspended boot still times out, which is the behaviour this replaces.
         */
        const val SUSPEND_GAP_FLOOR_MILLIS = 60_000L

        /**
         * True when [gapMillis] between two polls is too large to be anything but the host having been asleep.
         * Scales with [pollIntervalMillis] so an unusually slow poll is not misjudged by the flat floor.
         */
        fun isSuspendGap(gapMillis: Long, pollIntervalMillis: Long): Boolean =
            gapMillis >= maxOf(SUSPEND_GAP_FLOOR_MILLIS, pollIntervalMillis * 30)
    }
}
