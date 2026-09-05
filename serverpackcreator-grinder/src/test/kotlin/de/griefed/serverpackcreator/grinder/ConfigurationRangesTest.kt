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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Pins that a knob set to a value the daemon **cannot use** falls back to its default, exactly as an
 * unparseable one already did.
 *
 * `from` documents that nothing here throws — "a typo in a unit file should not stop a service that has
 * verdicts to serve" — and `"abc"` honoured that. `"0"` did not: it parses perfectly and is simply
 * unusable, so it travelled onward to whatever consumed it.
 *
 * The consequences were not uniform, which is what made this worth closing in one place:
 *
 *  - `SPC_GRINDER_WORKERS=0` reached `GrindPool`'s `require`, which `GrindLoop` constructs **inside the
 *    pass loop** — so the daemon started, bound the report port, logged a healthy startup line, and only
 *    then died on a message naming `workerCount` rather than the variable the operator set. Under
 *    `Restart=on-failure` that is a restart loop wearing the shape of a crash.
 *  - `SPC_GRINDER_INTERVAL=-1` throws nothing at all: `pauseAfterPass` hands back a negative duration, the
 *    wake-up instant is already in the past, and the loop paces itself by not pausing — a silent hot loop
 *    over the catalogue, which is the worse failure of the two because nothing reports it.
 *
 * Coercion rather than rejection is the deliberate reading of that "never throws" contract: the value the
 * daemon actually used is on the startup line either way, so an operator who set nonsense sees the default
 * being used rather than a dead unit.
 */
internal class ConfigurationRangesTest {

    /** Read the configuration with [overrides] applied over an otherwise empty environment. */
    private fun configWith(vararg overrides: Pair<String, String>): GrinderConfiguration {
        val environment = overrides.toMap()
        return GrinderConfiguration.from { environment[it] }
    }

    private val defaults = GrinderConfiguration.from { null }

    /** The reported case: zero workers is a pool that cannot be built. */
    @Test
    fun zeroWorkersFallsBackToTheDefault() {
        Assertions.assertEquals(defaults.workers, configWith("SPC_GRINDER_WORKERS" to "0").workers)
        Assertions.assertEquals(defaults.workers, configWith("SPC_GRINDER_WORKERS" to "-4").workers)
    }

    /** A batch of zero is a crawl that hands out nothing, and trips `CatalogCrawler`'s own guard. */
    @Test
    fun zeroBatchFallsBackToTheDefault() {
        Assertions.assertEquals(defaults.batch, configWith("SPC_GRINDER_BATCH" to "0").batch)
    }

    /** A negative pause is not a short pause; it is no pause, and the loop spins. */
    @Test
    fun negativePausesFallBackToTheirDefaults() {
        Assertions.assertEquals(defaults.betweenSweeps, configWith("SPC_GRINDER_INTERVAL" to "-1").betweenSweeps)
        Assertions.assertEquals(defaults.whileCrawling, configWith("SPC_GRINDER_SCAN_DELAY" to "-30").whileCrawling)
    }

    /** A negative TTL makes every verdict permanently stale, so the grinder re-grinds instead of covering. */
    @Test
    fun negativeRetentionsFallBackToTheirDefaults() {
        Assertions.assertEquals(defaults.reverifyTtl, configWith("SPC_GRINDER_REVERIFY_TTL_DAYS" to "-1").reverifyTtl)
        Assertions.assertEquals(defaults.cacheTtl, configWith("SPC_GRINDER_CACHE_TTL_DAYS" to "-1").cacheTtl)
    }

    /** A port outside the TCP range cannot be bound; 0 stays legal because it means "any free port". */
    @Test
    fun anUnbindablePortFallsBackButZeroIsKept() {
        Assertions.assertEquals(defaults.port, configWith("SPC_GRINDER_PORT" to "70000").port)
        Assertions.assertEquals(defaults.port, configWith("SPC_GRINDER_PORT" to "-1").port)
        Assertions.assertEquals(0, configWith("SPC_GRINDER_PORT" to "0").port, "0 means: pick a free port")
    }

    /** Negative container limits reach `ContainerResources`' own `require`; 0 legitimately means uncapped. */
    @Test
    fun negativeContainerLimitsFallBackButZeroMeansUncapped() {
        Assertions.assertEquals(defaults.containerCpus, configWith("SPC_GRINDER_CPUS" to "-2").containerCpus)
        Assertions.assertEquals(
            defaults.containerMemoryGiB, configWith("SPC_GRINDER_MEMORY_GIB" to "-1").containerMemoryGiB
        )
        Assertions.assertEquals(0.0, configWith("SPC_GRINDER_CPUS" to "0").containerCpus, "0 is uncapped")
    }

    /** A negative log budget would keep nothing; zero is a legitimate "keep no artifacts". */
    @Test
    fun aNegativeLogBudgetFallsBackButZeroIsKept() {
        Assertions.assertEquals(
            defaults.bootLogBudgetBytes, configWith("SPC_GRINDER_BOOT_LOG_BUDGET_MIB" to "-5").bootLogBudgetBytes
        )
        Assertions.assertEquals(0L, configWith("SPC_GRINDER_BOOT_LOG_BUDGET_MIB" to "0").bootLogBudgetBytes)
    }

    /** A negative flush interval already means write-through, which is a real choice and stays untouched. */
    @Test
    fun theFlushIntervalKeepsItsZeroAndNegativeMeanings() {
        Assertions.assertTrue(configWith("SPC_GRINDER_STORE_FLUSH_SECONDS" to "0").storeFlush.isZero)
    }

    /** Unchanged: an unparseable value still falls back, and a valid one is still honoured verbatim. */
    @Test
    fun parseableAndUsableValuesAreUntouched() {
        Assertions.assertEquals(8, configWith("SPC_GRINDER_WORKERS" to "8").workers)
        Assertions.assertEquals(defaults.workers, configWith("SPC_GRINDER_WORKERS" to "abc").workers)
    }
}
