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
package de.griefed.serverpackcreator.grinder.loader

/**
 * Orders a loader's known builds for [CachedLoaderVersions]' step-down.
 *
 * SPC's manifests list versions **ascending**, verified against the shipped Quilt and Fabric manifests
 * (Quilt runs `0.16.0-beta.1` … `0.31.0-beta.3`, Fabric `0.1.0.48` … `0.19.5`), while the step-down walks
 * from the newest downwards — so the list is reversed. Pure, because choosing which build to try next is a
 * decision worth pinning and the surrounding wiring needs an `ApiWrapper`.
 *
 * @author Griefed
 */
object LoaderStepDown {

    /**
     * [versionsAscending] newest-first, ready for the first-not-on-cooldown walk.
     *
     * Deliberately no other policy: no filtering of pre-releases, no de-duplication, no cap. The caller
     * already stops at the first build that is not refusing to install, so a longer list costs nothing, and
     * a filter here could empty a line whose every build is a pre-release — which is the state Quilt's line
     * is in at its head, and exactly when the step-down is needed most.
     */
    fun newestFirst(versionsAscending: List<String>): List<String> = versionsAscending.asReversed()
}
