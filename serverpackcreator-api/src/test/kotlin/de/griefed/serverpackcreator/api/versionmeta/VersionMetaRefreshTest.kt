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
package de.griefed.serverpackcreator.api.versionmeta

import de.griefed.serverpackcreator.api.ApiWrapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Pins the contract change B31 makes: [VersionMeta] no longer refreshes its manifests while it is being
 * constructed.
 *
 * The metas are built from the manifest *files*, and `ApiWrapper.setup()` has already seeded every one of
 * them from the jar — so there is working version data before any request is made. That is what lets the
 * refresh move off the blocking startup path. What must not be lost is that the data is usable *immediately*,
 * and that a caller who needs upstream-fresh data can still get it.
 */
internal class VersionMetaRefreshTest {

    /**
     * Pins that the seeded manifests are usable the moment construction returns — no waiting, no network.
     *
     * This is the half the whole change rests on: if the metas were empty until a refresh landed, moving the
     * refresh off the startup path would break every caller instead of speeding it up.
     */
    @Test
    fun theSeededManifestsAreUsableBeforeAnyRefresh() {
        val versionMeta = ApiWrapper.api().versionMeta

        Assertions.assertTrue(
            versionMeta.minecraft.allVersions().isNotEmpty(),
            "The jar-seeded Minecraft manifest must be parsed and usable without awaiting a refresh"
        )
        Assertions.assertTrue(
            versionMeta.fabric.loaderVersions().isNotEmpty(),
            "Same for the loader manifests — construction must not depend on the network"
        )
    }

    /**
     * Pins that the refresh is awaitable and reports completion, which is how the GUI's version dropdowns
     * and `ConfigurationHandler.checkConfiguration` avoid showing or rejecting a just-released version.
     */
    @Test
    fun theBackgroundRefreshCanBeAwaited() {
        val versionMeta = ApiWrapper.api().versionMeta

        Assertions.assertTrue(
            versionMeta.awaitManifestRefresh(),
            "The refresh must finish within its timeout and say so"
        )
        Assertions.assertTrue(
            versionMeta.awaitManifestRefresh(),
            "Awaiting again must return immediately and still report completion — callers await freely"
        )
    }
}
