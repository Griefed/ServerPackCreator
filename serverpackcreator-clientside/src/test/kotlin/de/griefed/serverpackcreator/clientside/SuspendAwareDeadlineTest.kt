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
import java.time.Duration

/**
 * Pins the boot budget against a host that goes to sleep mid-boot.
 *
 * A boot deadline measured on the wall clock expires on a server that never got the time. Measured 2026-07-31: a
 * laptop idle-sleeping in ~16-minute cycles produced 19 of 153 verdicts reading `timed out`, several of them
 * `SURVIVED (timed out)` whose console showed the server reaching ready seconds after launch — i.e. the verdict was
 * about the host's sleep, not about the mod. So a suspended interval is added back: the timeout must mean "the boot
 * had this long and did not make it", never "this much clock passed".
 *
 * The clock is injected, which is the whole point of this being a separate unit — the two real callers
 * (`HostProcessServerRunner` here, `DockerJavaContainerEngine` in the grinder) are integration-shaped and cannot
 * be made to sleep for a test.
 */
internal class SuspendAwareDeadlineTest {

    /** A clock the test moves by hand, standing in for `System.currentTimeMillis`. */
    private class FakeClock(var millis: Long = 1_000_000L) : () -> Long {
        override fun invoke(): Long = millis
        fun advance(by: Long) { millis += by }
    }

    private val poll = 500L

    /** Ordinary polling must consume the budget: without a suspend, the deadline arrives on schedule. */
    @Test
    fun ordinaryPollingSpendsTheBudget() {
        val clock = FakeClock()
        val deadline = SuspendAwareDeadline(Duration.ofMinutes(2), poll, clock)

        Assertions.assertTrue(deadline.hasTimeLeft(), "the budget starts unspent")

        // 2 minutes of normal polling, in poll-sized steps.
        repeat(240) {
            clock.advance(poll)
            deadline.tick()
        }

        Assertions.assertFalse(
            deadline.hasTimeLeft(),
            "two minutes of real polling must exhaust a two-minute budget — otherwise nothing ever times out"
        )
    }

    /** A suspend is given back, so the boot keeps the budget it never got to use. */
    @Test
    fun aSuspendedIntervalIsAddedBackToTheBudget() {
        val clock = FakeClock()
        val suspends = mutableListOf<Long>()
        val deadline = SuspendAwareDeadline(Duration.ofMinutes(2), poll, clock) { suspends.add(it) }

        // A minute of honest polling, then the host sleeps for 16 minutes, then polling resumes.
        repeat(120) { clock.advance(poll); deadline.tick() }
        Assertions.assertTrue(deadline.hasTimeLeft(), "only a minute of the two-minute budget is gone")

        clock.advance(Duration.ofMinutes(16).toMillis())
        deadline.tick()

        Assertions.assertEquals(1, suspends.size, "the suspend must be reported once, so an operator can see why")
        Assertions.assertTrue(
            deadline.hasTimeLeft(),
            "the 16 minutes the host was asleep must not count against the boot — this is the whole defect"
        )

        // The remaining minute of genuine polling still runs out.
        repeat(120) { clock.advance(poll); deadline.tick() }
        Assertions.assertFalse(deadline.hasTimeLeft(), "the budget is spent once the boot has actually had its time")
    }

    /**
     * The threshold must not fire on ordinary slowness. Scheduling jitter, a starved host or a long GC pause can
     * cost seconds; nothing short of a suspend costs a minute between two 500 ms polls. Under-detecting is the safe
     * direction — it only means a suspended boot still times out, which is the behaviour being replaced.
     */
    @Test
    fun ordinarySlownessIsNotMistakenForASuspend() {
        val clock = FakeClock()
        val suspends = mutableListOf<Long>()
        val deadline = SuspendAwareDeadline(Duration.ofMinutes(2), poll, clock) { suspends.add(it) }

        for (jitter in listOf(600L, 2_000L, 10_000L, 30_000L)) {
            clock.advance(jitter)
            deadline.tick()
        }

        Assertions.assertTrue(suspends.isEmpty(), "gaps under the floor are slowness, not sleep: $suspends")

        clock.advance(SuspendAwareDeadline.SUSPEND_GAP_FLOOR_MILLIS)
        deadline.tick()
        Assertions.assertEquals(1, suspends.size, "a gap at the floor is a suspend")
    }

    /** A poll interval so long that the floor would misjudge it scales the threshold instead. */
    @Test
    fun theThresholdScalesWithAnUnusuallyLongPollInterval() {
        val slowPoll = Duration.ofSeconds(10).toMillis()
        Assertions.assertFalse(
            SuspendAwareDeadline.isSuspendGap(SuspendAwareDeadline.SUSPEND_GAP_FLOOR_MILLIS, slowPoll),
            "with a 10s poll, a 60s gap is only six polls — not evidence of sleep"
        )
        Assertions.assertTrue(
            SuspendAwareDeadline.isSuspendGap(slowPoll * 30, slowPoll),
            "thirty missed polls is sleep regardless of the floor"
        )
    }
}
