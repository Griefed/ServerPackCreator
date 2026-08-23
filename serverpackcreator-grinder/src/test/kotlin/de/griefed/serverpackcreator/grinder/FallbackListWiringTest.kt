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
 * Keeps `/as-properties` connected to the lists it is supposed to publish.
 *
 * `ReportServerTest` hands the endpoint a lambda of its own, so it proves the *rendering* and nothing about
 * production: the endpoint could be wired to an empty list, or to the regex variants, and every test would stay
 * green while every polling client silently received nothing. `main` cannot be executed to check — it builds an
 * `ApiWrapper` and a Docker client — so the join is asserted against its source, exactly as
 * [ReportBindWiringTest] does for `SPC_GRINDER_HOST`. Audit iteration 17, M3.
 */
internal class FallbackListWiringTest {

    @Test
    fun theEndpointIsFedSpcsOwnLists() {
        val body = grinderMainBody()

        Assertions.assertTrue(
            body.contains("fallbackLists ="),
            "main() no longer passes fallbackLists to the ReportServer — /as-properties would serve the " +
                "grinder's findings on top of an empty list, and every polling client would quietly lose the " +
                "shipped entries"
        )
        Assertions.assertTrue(
            body.contains("apiWrapper.apiProperties.clientsideMods"),
            "the published clientside list must come from SPC's own property, not a copy that can go stale"
        )
        Assertions.assertTrue(
            body.contains("apiWrapper.apiProperties.modsWhitelist"),
            "the whitelist must be passed through, or the endpoint silently freezes a client's whitelist " +
                "the moment they point at it instead of the repository URL"
        )
    }

    /**
     * The lists must be read *per request*. Captured once at startup, a daemon that runs for weeks would keep
     * serving whatever SPC happened to hold at boot, which is the opposite of the point.
     */
    @Test
    fun theListsAreReadThroughALambdaRatherThanCapturedAtStartup() {
        val wiring = grinderMainBody().substringAfter("fallbackLists =").substringBefore(").start()")

        Assertions.assertTrue(
            wiring.contains("{") && wiring.contains("FallbackLists("),
            "fallbackLists must be a lambda constructing FallbackLists on each call, was: ${wiring.trim()}"
        )
    }
}
