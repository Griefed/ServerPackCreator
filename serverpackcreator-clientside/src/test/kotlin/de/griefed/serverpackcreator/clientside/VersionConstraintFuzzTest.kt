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
 * Sweeps malformed constraints against real versions, asserting **not one of them refuses**.
 *
 * `VersionConstraintTest.anythingUnreadableAccepts` states the rule with a handful of hand-picked cases,
 * and it already caught one hole — `numbersOf` maps a digit-less component to `0`, so a bare clause like
 * `whatever` compared equal to `0.0.0` and refused every real version. A handful of examples is a thin net
 * for a rule whose whole point is covering input nobody anticipated, so this sweeps the shapes a parser
 * usually breaks on: empty brackets, dangling comparators, half-written ranges, disjunctions with a junk
 * arm, and text with no digits at all.
 *
 * A refusal here is not a cosmetic failure: it is indistinguishable from the dependency being genuinely
 * unsatisfiable, so a gap in grammar coverage would present as a catalog-wide mass-INCONCLUSIVE event.
 */
internal class VersionConstraintFuzzTest {

    private val versions = listOf("1.0.0", "0.92.2+1.20.1", "47", "1.21.8", "15.2.0.27", "v2.1")

    private val unreadable = listOf(
        "", "   ", "whatever nonsense this is", "[malformed", "malformed]", "(", ")", "[]", "()",
        ">=", "<", "~", "^", "||", ">=   ", "[,]", "[,)", "(,)", ">=abc", "~xyz", "^none",
        "not a version at all", "garbage || more garbage", ".x", "..", "-", "+", "@",
        "verylongtextwithnodigits", "[abc,def]"
    )

    @Test
    fun noMalformedConstraintEverRefuses() {
        val refusals = versions.flatMap { version ->
            unreadable.filterNot { VersionConstraint.satisfies(version, it) }.map { version to it }
        }

        Assertions.assertTrue(
            refusals.isEmpty(),
            "an unreadable constraint must accept, never refuse — these did not: $refusals"
        )
    }

    /** A junk arm in a disjunction must not poison an arm that is perfectly readable, in either position. */
    @Test
    fun aJunkArmDoesNotPoisonAReadableOne() {
        Assertions.assertTrue(VersionConstraint.satisfies("1.0.0", "garbage || >=1.0.0"))
        Assertions.assertTrue(VersionConstraint.satisfies("1.0.0", ">=1.0.0 || garbage"))
    }

    /** And the rule must not become "accept everything": readable constraints still have to bite. */
    @Test
    fun readableConstraintsStillRefuseWhenTheyShould() {
        Assertions.assertFalse(VersionConstraint.satisfies("0.91.0", ">=0.92.0"))
        Assertions.assertFalse(VersionConstraint.satisfies("46", "[47,)"))
        Assertions.assertFalse(VersionConstraint.satisfies("2.0", "[1.0,2.0)"))
    }
}
