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

/**
 * Decides whether a mod file's version satisfies the constraint a dependant declared for it.
 *
 * Two grammars, because the loaders disagree and `ModDependency.versionConstraint` keeps each descriptor's
 * text verbatim so they can be told apart: **Fabric and Quilt** write npm-style ranges (`>=0.92.0`,
 * `^1.2.0`, `~1.2.0`, `1.20.x`), **Forge and NeoForge** write Maven ranges (`[47,)`, `(,3.0]`).
 *
 * **Unreadable input accepts, and that is the whole safety design.** A constraint this cannot parse — a
 * grammar nobody anticipated, a version string that is not a version — must never cause a *refusal*,
 * because a refusal is indistinguishable from the dependency being genuinely unsatisfiable. Getting that
 * backwards would turn a gap in grammar coverage into a catalog-wide mass-INCONCLUSIVE event. The worst a
 * gap can do here is fail to *narrow* a choice that would otherwise have been made anyway.
 *
 * @author Griefed
 */
object VersionConstraint {

    /** Comparators an npm-style clause may open with, longest first so `>=` is read before `>`. */
    private val comparators = listOf(">=", "<=", ">", "<", "=")

    /**
     * Whether [text] can be read as a version at all, i.e. holds at least one digit.
     *
     * **This is the guard that keeps "unreadable accepts" honest**, and it is easy to leave out: [numbersOf]
     * maps a component with no digits to `0`, so without this check a clause like `whatever` compares equal
     * to `0.0.0` and *refuses* every real version — the exact direction the class doc forbids. Found by
     * `anythingUnreadableAccepts` rather than by inspection.
     */
    private fun looksLikeVersion(text: String) = text.any { it.isDigit() }

    /**
     * Whether [version] satisfies [constraint]. A `null`, blank, wildcard or unparseable [constraint]
     * accepts, as does a `null` or blank [version] — see the class doc for why that direction is deliberate.
     */
    fun satisfies(version: String?, constraint: String?): Boolean {
        val candidate = version?.trim().orEmpty()
        val declared = constraint?.trim().orEmpty()
        if (candidate.isEmpty() || declared.isEmpty() || declared == "*") {
            return true
        }
        return runCatching { matches(candidate, declared) }.getOrDefault(true)
    }

    /** Split on `||` first: a disjunction holds when any alternative does. */
    private fun matches(version: String, constraint: String): Boolean =
        constraint.split("||").any { alternative -> conjunctionHolds(version, alternative.trim()) }

    /** Within an alternative, space-separated clauses are a conjunction — every one has to hold. */
    private fun conjunctionHolds(version: String, alternative: String): Boolean {
        if (alternative.isEmpty() || alternative == "*") {
            return true
        }
        if (alternative.startsWith("[") || alternative.startsWith("(")) {
            return mavenRangeHolds(version, alternative)
        }
        return alternative.split(" ").filter { it.isNotBlank() }.all { clauseHolds(version, it) }
    }

    /**
     * One npm-style clause. `~` pins the minor and `^` the major, which is what the shorthands mean in the
     * ranges Fabric and Quilt mods actually publish; a bare version is an exact match.
     */
    private fun clauseHolds(version: String, clause: String): Boolean {
        val comparator = comparators.firstOrNull { clause.startsWith(it) }
        if (comparator != null) {
            val bound = clause.removePrefix(comparator).trim()
            if (!looksLikeVersion(bound)) {
                return true
            }
            val order = compareVersions(version, bound)
            return when (comparator) {
                ">=" -> order >= 0
                "<=" -> order <= 0
                ">" -> order > 0
                "<" -> order < 0
                else -> order == 0
            }
        }
        if (clause.startsWith("~") || clause.startsWith("^")) {
            val bound = clause.drop(1).trim()
            if (!looksLikeVersion(bound)) {
                return true
            }
            if (compareVersions(version, bound) < 0) {
                return false
            }
            // `~1.2.0` allows anything up to 1.3.0; `^1.2.0` up to 2.0.0. Both are "at least the bound,
            // below the next release of the component the shorthand pins".
            val pinned = numbersOf(bound)
            val ceiling = when {
                clause.startsWith("~") && pinned.size >= 2 -> listOf(pinned[0], pinned[1] + 1)
                else -> listOf(pinned.firstOrNull()?.plus(1) ?: return true)
            }
            return compareNumbers(numbersOf(version), ceiling) < 0
        }
        // A trailing `.x` / `.*` pins the components before it, e.g. `1.20.x`. The prefix has to be
        // version-shaped in its own right: a bare `.x` pins nothing, and without this check it would refuse
        // everything — the same hole `looksLikeVersion` closes below, reached by a different branch.
        if (clause.endsWith(".x") || clause.endsWith(".*")) {
            val prefix = clause.dropLast(2)
            if (!looksLikeVersion(prefix)) {
                return true
            }
            return version == prefix || version.startsWith("$prefix.")
        }
        // A bare clause that is not version-shaped is not a constraint this understands, so it accepts.
        return !looksLikeVersion(clause) || compareVersions(version, clause) == 0
    }

    /**
     * A Maven range: `[` / `]` are inclusive bounds, `(` / `)` exclusive, and either side may be empty for
     * an open end. `[47,)` — at least 47, no upper bound — is the shape most Forge descriptors use.
     */
    private fun mavenRangeHolds(version: String, range: String): Boolean {
        val lowerInclusive = range.startsWith("[")
        val upperInclusive = range.endsWith("]")
        val body = range.drop(1).dropLast(1)
        if (!looksLikeVersion(body)) {
            return true
        }
        // A range without a comma is a single pinned version, e.g. `[1.20.1]`.
        if (!body.contains(",")) {
            return compareVersions(version, body.trim()) == 0
        }
        val lower = body.substringBefore(",").trim()
        val upper = body.substringAfter(",").trim()
        if (lower.isNotEmpty()) {
            val order = compareVersions(version, lower)
            if (if (lowerInclusive) order < 0 else order <= 0) {
                return false
            }
        }
        if (upper.isNotEmpty()) {
            val order = compareVersions(version, upper)
            if (if (upperInclusive) order > 0 else order >= 0) {
                return false
            }
        }
        return true
    }

    /** Compare two version strings component-wise, ignoring build metadata after `+` or `-`. */
    private fun compareVersions(left: String, right: String): Int =
        compareNumbers(numbersOf(left), numbersOf(right))

    /**
     * The numeric components of a version, with build metadata dropped — `0.92.2+1.20.1` is `[0, 92, 2]`.
     * Fabric API publishes exactly that shape, so ignoring the suffix is not an edge case here.
     */
    private fun numbersOf(version: String): List<Int> =
        version.substringBefore("+").substringBefore("-")
            .split(".")
            .map { component -> component.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }

    /** Compare component lists numerically, treating a missing component as zero so depths may differ. */
    private fun compareNumbers(left: List<Int>, right: List<Int>): Int {
        for (index in 0 until maxOf(left.size, right.size)) {
            val order = (left.getOrElse(index) { 0 }).compareTo(right.getOrElse(index) { 0 })
            if (order != 0) {
                return order
            }
        }
        return 0
    }
}
