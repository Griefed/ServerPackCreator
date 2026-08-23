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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Pins the core-count → CFS-quota arithmetic, which is the whole of `SPC_GRINDER_CPUS`.
 *
 * Worth its own guard because it fails *silently* in both directions: a quota computed against the wrong
 * period throttles a boot to a tenth of what the operator asked for (boots then crawl and hit the 15-minute
 * budget, scored INCONCLUSIVE — indistinguishable from a mod that hangs), and a value the daemon rejects
 * fails the whole run at container-create time rather than at the knob that caused it.
 */
internal class ContainerResourcesTest {

    /** The operator's unit is cores, docker's is microseconds per period — one has to convert to the other. */
    @Test
    fun coresBecomeAQuotaAgainstTheDeclaredPeriod() {
        val twoCores = ContainerResources.forCpus(2.0)

        Assertions.assertEquals(2 * twoCores.cpuPeriod, twoCores.cpuQuota, "2 cores must be twice the period")
        Assertions.assertEquals(twoCores.cpuPeriod / 2, ContainerResources.forCpus(0.5).cpuQuota, "half a core is half a period")
        Assertions.assertEquals(
            (7 * twoCores.cpuPeriod) / 2, ContainerResources.forCpus(3.5).cpuQuota,
            "a fractional count must survive the conversion — 3.5 cores is not 3"
        )
    }

    /**
     * The period must be stated, not inherited. `cpu.cfs_period_us` defaults to 100ms today, but a quota is
     * meaningless without the period it divides, and nothing in this codebase would notice a daemon or kernel
     * that shipped a different one.
     */
    @Test
    fun theQuotaPeriodIsDeclaredRatherThanAssumed() {
        Assertions.assertEquals(100_000L, ContainerResources().cpuPeriod, "the CFS period must be pinned at 100ms")
    }

    /** The knob must not change what an existing install gets: the default stays the two cores it was. */
    @Test
    fun theShippedDefaultIsExactlyTwoCores() {
        Assertions.assertEquals(ContainerResources().cpuQuota, ContainerResources.forCpus(2.0).cpuQuota)
        Assertions.assertEquals(200_000L, ContainerResources().cpuQuota, "the shipped default is ~2 cores")
    }

    /**
     * Zero is the deliberate escape hatch (docker reads quota `0` as "no limit"), matching how
     * `SPC_GRINDER_CACHE_TTL_DAYS=0` means "never". A negative count is an operator error, not a hatch.
     */
    @Test
    fun zeroMeansUncappedAndNegativeIsRejected() {
        Assertions.assertEquals(0L, ContainerResources.forCpus(0.0).cpuQuota, "0 cores must mean an unset quota")
        Assertions.assertThrows(IllegalArgumentException::class.java) { ContainerResources.forCpus(-1.0) }
    }

    /**
     * Below 1ms per period the daemon refuses the container outright ("CPU cfs quota can not be less than
     * 1ms"), so a tiny value has to be raised here — at the knob — rather than surfacing as every boot
     * failing to start.
     */
    @Test
    fun aQuotaTooSmallForTheDaemonIsRaisedToItsMinimum() {
        Assertions.assertEquals(1_000L, ContainerResources.forCpus(0.0001).cpuQuota)
    }

    /**
     * The floor above must be decided by what the operator *asked for*, not by what the arithmetic produced.
     *
     * A count small enough to round to a 0µs quota is still a request for a cap, and quota `0` is docker's
     * "no limit" — so reading the computed value as the uncapped sentinel turns the smallest possible cap
     * into none at all, in the one direction a hardening knob must never fail. Verified against the daemon:
     * `--cpu-quota=0 --cpu-period=100000` reports `max 100000` in the container's own cgroup.
     */
    @Test
    fun aCapTooSmallToRoundIsStillACapAndNotUncapped() {
        Assertions.assertEquals(
            1_000L, ContainerResources.forCpus(0.000001).cpuQuota,
            "a positive core count must never produce the unset quota that means uncapped"
        )
    }

    /**
     * `Infinity` and `NaN` both survive `String.toDouble()`, so the knob can be handed either. Rejected at
     * the conversion, because the alternatives are silent: infinity rounds to `Long.MAX_VALUE` (a quota so
     * large it means uncapped), and NaN rounds to 0 (uncapped outright).
     */
    @Test
    fun aNonFiniteCountIsRejectedRatherThanRoundedIntoNonsense() {
        Assertions.assertThrows(IllegalArgumentException::class.java) { ContainerResources.forCpus(Double.POSITIVE_INFINITY) }
        Assertions.assertThrows(IllegalArgumentException::class.java) { ContainerResources.forCpus(Double.NaN) }
    }
}
