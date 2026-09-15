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
package de.griefed.serverpackcreator.app.updater.versionchecker

import de.griefed.serverpackcreator.api.utilities.common.Comparison
import de.griefed.serverpackcreator.api.utilities.common.SemanticVersionComparator
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.Optional

/**
 * Characterization tests for [VersionChecker]'s pre-release comparison, which had no coverage at all.
 *
 * The class is abstract and its only data source is [VersionChecker.allVersions], so the whole
 * alpha/beta path can be exercised offline by supplying a canned version list — no repository, no
 * network. What is pinned here is *current* behaviour, including the parts that are merely
 * incidental, so a later restructuring of these boolean chains has something to be measured against.
 *
 * Version comparison is the project's documented silent-failure category: a wrong branch yields a
 * plausible version rather than an error, so it gets pinned before it gets touched.
 */
internal class VersionCheckerTest {

    /**
     * A [VersionChecker] over a fixed version list. [latestVersion] and [getDownloadUrl] are the
     * abstract repository-specific hooks; they answer from the same list so nothing reaches a network.
     */
    private class FakeVersionChecker(private val versions: List<String>) : VersionChecker() {
        override fun allVersions(): List<String> = versions

        override fun refresh(): VersionChecker {
            setAllVersions()
            return this
        }

        /**
         * Newest overall when pre-releases count, newest plain release otherwise — *computed*, not
         * taken from the list order, so a fixture may be given in any order. That matters: the real
         * `allVersions()` comes from a repository API whose ordering is not guaranteed, and code that
         * silently depends on it is exactly what these tests are here to catch.
         */
        override fun latestVersion(checkForPreRelease: Boolean): String {
            val candidates = if (checkForPreRelease) versions else versions.filter { !it.contains("-") }
            return candidates.reduce { newest, candidate ->
                if (SemanticVersionComparator.compareSemantics(newest, candidate, Comparison.NEW)) candidate else newest
            }
        }

        override fun getDownloadUrl(version: String) = "https://example.invalid/$version"

        override fun setRepository() = Unit

        override fun check(currentVersion: String, checkForPreReleases: Boolean): Optional<Update> =
            Optional.empty()

        /** Exposes the protected decision under test. */
        fun updateFor(currentVersion: String, checkForPreReleases: Boolean): String? =
            isUpdateAvailable(currentVersion, checkForPreReleases)
    }

    /** Newest first, mixing a plain release with two beta and two alpha pre-releases of the next one. */
    private fun checker() = FakeVersionChecker(
        listOf("3.1.0-beta.3", "3.1.0-beta.1", "3.1.0-alpha.5", "3.1.0-alpha.2", "3.0.0")
    ).refresh() as FakeVersionChecker

    /** The newest beta is picked by pre-release number, not by list order. */
    @Test
    fun theLatestBetaIsTheHighestNumberedOne() {
        Assertions.assertEquals("3.1.0-beta.3", checker().updateFor("3.1.0-beta.1", true))
    }

    /**
     * A beta supersedes an alpha of the same version, whatever the two numbers happen to be.
     *
     * The comparison used to look only at the number after the dot, so the offer an alpha user got
     * depended on a numeric accident: `alpha.2` was offered `beta.3` (3 > 2) while `alpha.5` was
     * offered nothing at all (3 > 5 fails for the beta, 5 > 5 for its own channel) even with both
     * `beta.3` and `alpha.5` published. Channel first, number only as the tie-break.
     */
    @Test
    fun anAlphaIsOfferedTheBetaOfTheSameVersionWhateverTheNumbers() {
        val checker = checker()
        Assertions.assertEquals("3.1.0-beta.3", checker.updateFor("3.1.0-alpha.2", true))
        Assertions.assertEquals("3.1.0-beta.3", checker.updateFor("3.1.0-alpha.5", true))
    }

    /**
     * The newest pre-release is the one with the newest *version*, not the highest pre-release
     * number. `latestBeta`/`latestAlpha` required a candidate to be both semantically newer-or-equal
     * **and** higher-numbered, so a newer version restarting its count — `3.2.0-beta.1` after
     * `3.1.0-beta.3` — lost to the older one and was never offered at all.
     */
    @Test
    fun aNewerVersionsPreReleaseWinsOverAHigherNumberedOlderOne() {
        // Deliberately oldest-first: `latestBeta` walked the list keeping a candidate only if it was
        // BOTH newer-or-equal and higher-numbered, so whether it found the right answer depended on
        // the order the repository happened to return.
        //
        // 3.1.0-beta.1 is old enough that the beta branch fires and hands back latestBeta() directly,
        // which is what makes the wrong answer reach the user instead of being masked by the
        // fall-through to latestVersion().
        val checker = FakeVersionChecker(
            listOf("3.0.0", "3.1.0-beta.1", "3.1.0-beta.3", "3.2.0-beta.1")
        ).refresh() as FakeVersionChecker

        Assertions.assertEquals("3.2.0-beta.1", checker.updateFor("3.1.0-beta.1", true))
    }

    /** Sitting on the newest beta means there is nothing newer to offer. */
    @Test
    fun theNewestPreReleaseIsUpToDate() {
        Assertions.assertEquals("up_to_date", checker().updateFor("3.1.0-beta.3", true))
    }

    /**
     * A beta is never offered an alpha of the same version. The guard is explicit in
     * `isNewAlphaAvailable`, and without it the newest alpha would look like an update to a beta that
     * is already ahead of it.
     */
    @Test
    fun aBetaIsNotOfferedAnAlphaOfTheSameVersion() {
        Assertions.assertEquals("up_to_date", checker().updateFor("3.1.0-beta.3", true))
        Assertions.assertNotEquals("3.1.0-alpha.5", checker().updateFor("3.1.0-beta.1", true))
    }

    /** With pre-releases switched off, neither channel is consulted at all. */
    @Test
    fun preReleasesAreIgnoredWhenNotAskedFor() {
        val checker = checker()
        Assertions.assertEquals("up_to_date", checker.updateFor("3.0.0", false))
        Assertions.assertEquals("up_to_date", checker.updateFor("3.1.0-beta.1", false))
    }

    /** An older plain release is offered the newest plain release. */
    @Test
    fun anOlderReleaseIsOfferedTheNewestRelease() {
        val checker = FakeVersionChecker(listOf("3.0.0", "2.0.0")).refresh() as FakeVersionChecker
        Assertions.assertEquals("3.0.0", checker.updateFor("2.0.0", false))
    }

    /** A repository holding no pre-releases at all must not break the pre-release path. */
    @Test
    fun aRepositoryWithoutPreReleasesIsHandled() {
        val checker = FakeVersionChecker(listOf("3.0.0", "2.0.0")).refresh() as FakeVersionChecker
        Assertions.assertEquals("3.0.0", checker.updateFor("2.0.0", true))
        Assertions.assertEquals("up_to_date", checker.updateFor("3.0.0", true))
    }
}
