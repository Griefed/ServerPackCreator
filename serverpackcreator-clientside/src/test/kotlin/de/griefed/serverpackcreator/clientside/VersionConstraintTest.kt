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
 * Pins matching a mod file's version against the constraint its dependant declared.
 *
 * Two grammars have to be read, because the loaders disagree: Fabric and Quilt write npm-style ranges
 * (`>=0.92.0`, `^2.0.0`, `1.20.x`), Forge and NeoForge write Maven ranges (`[47,)`, `(,3.0]`). The
 * constraint is carried verbatim from the descriptor precisely so this can tell them apart.
 *
 * **The load-bearing rule is the last one: an unparseable constraint ACCEPTS.** A constraint this parser
 * cannot read must never cause a refusal, or a gap in the grammar coverage turns into a mass-INCONCLUSIVE
 * event across the catalog — the parser failing would be indistinguishable from every mod's dependencies
 * being unsatisfiable.
 */
internal class VersionConstraintTest {

    private fun assertSatisfies(version: String, constraint: String) = Assertions.assertTrue(
        VersionConstraint.satisfies(version, constraint), "$version should satisfy '$constraint'"
    )

    private fun assertViolates(version: String, constraint: String) = Assertions.assertFalse(
        VersionConstraint.satisfies(version, constraint), "$version should NOT satisfy '$constraint'"
    )

    @Test
    fun readsFabricStyleComparators() {
        assertSatisfies("0.92.2", ">=0.92.0")
        assertSatisfies("0.92.0", ">=0.92.0")
        assertViolates("0.91.9", ">=0.92.0")
        assertSatisfies("0.93.0", ">0.92.0")
        assertViolates("0.92.0", ">0.92.0")
        assertSatisfies("1.0.0", "<=1.0.0")
        assertViolates("1.0.1", "<=1.0.0")
        assertSatisfies("0.9.9", "<1.0.0")
    }

    @Test
    fun anExactVersionMatchesOnlyItself() {
        assertSatisfies("1.2.3", "1.2.3")
        assertViolates("1.2.4", "1.2.3")
    }

    @Test
    fun wildcardsAcceptAnything() {
        assertSatisfies("0.92.2+1.20.1", "*")
        assertSatisfies("anything at all", "*")
    }

    /** Fabric's `~` (same minor) and `^` (same major) are the two most common shorthands in the wild. */
    @Test
    fun readsTildeAndCaretShorthands() {
        assertSatisfies("1.2.9", "~1.2.0")
        assertViolates("1.3.0", "~1.2.0")
        assertSatisfies("1.9.0", "^1.2.0")
        assertViolates("2.0.0", "^1.2.0")
    }

    /** Maven ranges, which is what a Forge or NeoForge `versionRange` states. */
    @Test
    fun readsMavenRanges() {
        assertSatisfies("47.1.0", "[47,)")
        assertViolates("46.9.9", "[47,)")
        assertSatisfies("1.5", "[1.0,2.0)")
        assertViolates("2.0", "[1.0,2.0)")
        assertSatisfies("2.0", "[1.0,2.0]")
        assertSatisfies("2.9", "(,3.0]")
        assertViolates("3.1", "(,3.0]")
    }

    /**
     * A **comma-separated union of ranges**, which Maven's own specification documents and Forge and
     * NeoForge therefore accept in a `versionRange`. Every element is an alternative: the constraint holds
     * if any one of them does.
     *
     * Found on the public grinder 2026-09-06, in the ERROR rows. `distanthorizons` declares
     * `[1.20.3],[1.20.4]` and was refused for a **1.20.4** pack; `mru` declares `26.2,26.3` and was refused
     * for a **26.2** pack. Executed against the parser, both constraints refused *every* version they list,
     * and Maven's own documented example `(,1.0],[1.2,)` refused everything — the union was never
     * satisfiable at all.
     *
     * The cause is one comma doing two jobs: inside a single bracketed range it separates lower from upper,
     * and between ranges it separates alternatives. `mavenRangeHolds` assumed the first reading always, so
     * `[1.20.3],[1.20.4]` parsed as one range from `1.20.3]` to `[1.20.4` — and `numbersOf("[1.20.4")` maps
     * the bracketed component to `0`, making the upper bound `0.20.4`.
     *
     * This fails **safe** — a refused boot publishes nothing — so no wrong exclusion reached anyone. What it
     * costs is coverage: those mods are never boot-verified, and the ERROR they produce reads as a statement
     * about the mod rather than about the parser.
     */
    @Test
    fun readsCommaSeparatedUnionsOfRanges() {
        // The two observed on the live instance.
        assertSatisfies("1.20.4", "[1.20.3],[1.20.4]")
        assertSatisfies("1.20.3", "[1.20.3],[1.20.4]")
        assertViolates("1.20.5", "[1.20.3],[1.20.4]")
        assertSatisfies("26.2", "26.2,26.3")
        assertSatisfies("26.3", "26.2,26.3")
        assertViolates("26.4", "26.2,26.3")

        // Maven's own documented union example: everything at or below 1.0, or at or above 1.2.
        assertSatisfies("1.20.4", "(,1.0],[1.2,)")
        assertSatisfies("0.9", "(,1.0],[1.2,)")
        assertViolates("1.1", "(,1.0],[1.2,)")

        // A comma *inside* one range still separates its bounds — the reading that was already correct.
        assertSatisfies("1.20.1", "[1.20,1.20.2)")
        assertViolates("1.20.4", "[1.20,1.20.2)")
    }

    /** A space-separated conjunction has to hold on both sides; a `||` disjunction on either. */
    @Test
    fun readsConjunctionsAndDisjunctions() {
        assertSatisfies("1.5.0", ">=1.0.0 <2.0.0")
        assertViolates("2.1.0", ">=1.0.0 <2.0.0")
        assertSatisfies("3.0.0", ">=1.0.0 <2.0.0 || >=3.0.0")
    }

    /** Real-world versions carry build metadata; comparing must not trip over the `+`. */
    @Test
    fun ignoresBuildMetadataWhenComparing() {
        assertSatisfies("0.92.2+1.20.1", ">=0.92.0")
        assertViolates("0.91.0+1.20.1", ">=0.92.0")
    }

    /** Numeric components compare numerically, not as text — otherwise 10 sorts below 9. */
    @Test
    fun comparesComponentsNumericallyRatherThanAsText() {
        assertSatisfies("0.10.0", ">=0.9.0")
        assertSatisfies("47.10", "[47.9,)")
    }

    /** Versions of different depth still compare: a missing component reads as zero. */
    @Test
    fun aShorterVersionIsPaddedRatherThanRejected() {
        assertSatisfies("1.2", ">=1.2.0")
        assertViolates("1.2", ">1.2.0")
    }

    /**
     * **The rule that keeps a parser gap from becoming a catalog-wide outage.** Anything this cannot read —
     * an empty constraint, a grammar nobody anticipated, a null — accepts, so the worst a coverage gap can
     * do is fail to *narrow* a choice. Refusing instead would look exactly like every dependency being
     * unsatisfiable.
     */
    @Test
    fun anythingUnreadableAccepts() {
        assertSatisfies("1.0.0", "")
        assertSatisfies("1.0.0", "   ")
        assertSatisfies("1.0.0", "whatever nonsense this is")
        assertSatisfies("1.0.0", "[malformed")
        Assertions.assertTrue(VersionConstraint.satisfies("1.0.0", null))
    }

    /** A version we do not know cannot be judged, so it is accepted for the same reason. */
    @Test
    fun anUnknownVersionAccepts() {
        Assertions.assertTrue(VersionConstraint.satisfies(null, ">=1.0.0"))
        Assertions.assertTrue(VersionConstraint.satisfies("", ">=1.0.0"))
    }
}
