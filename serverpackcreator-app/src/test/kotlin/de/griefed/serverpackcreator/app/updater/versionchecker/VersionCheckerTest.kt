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

        /** Newest overall when pre-releases count, newest plain release otherwise. */
        override fun latestVersion(checkForPreRelease: Boolean): String =
            if (checkForPreRelease) versions.first() else versions.first { !it.contains("-") }

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
     * Characterizes a quirk rather than endorsing it: `isPreReleaseNewer` compares only the number
     * after the dot and is blind to the channel, while `isUpdateAvailable` consults beta before alpha.
     *
     * So an alpha user is offered a *beta* whenever the beta's number happens to be higher —
     * `alpha.2` gets `beta.3` — and is offered nothing at all when it is not, even though a newer
     * alpha exists: `alpha.5` is left on `up_to_date` with `beta.3` and `alpha.5` both published,
     * because `3 > 5` fails for the beta and `5 > 5` fails for its own channel.
     *
     * Arguably a beta is a reasonable offer to an alpha user, but "reasonable" is not what decides it
     * here — the numeric accident is. Pinned so a restructuring of these chains cannot change it
     * silently; changing it deliberately is a separate decision.
     */
    @Test
    fun anAlphaIsOfferedABetaOrNothingDependingOnTheNumbersAlone() {
        val checker = checker()
        Assertions.assertEquals("3.1.0-beta.3", checker.updateFor("3.1.0-alpha.2", true))
        Assertions.assertEquals("up_to_date", checker.updateFor("3.1.0-alpha.5", true))
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
