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
package de.griefed.serverpackcreator.app

import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.settings.PathsConfig
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.prefs.Preferences

/**
 * Pins that the app writes the home directory where `-api` will read it back from.
 *
 * The two used to be able to disagree: four call-sites in this module hard-coded the node name while `-api` resolves it
 * through [ApiProperties.resolvePreferencesNode], so a host claiming its own node — the grinder daemon, a test JVM —
 * would have had the app store a home that `-api` never looked at. That is the same class of mismatch that once let a
 * test suite relocate a running daemon's home directory.
 *
 * Each test points the resolver at a scratch node so the developer's real settings are never touched.
 */
internal class HomeDirectoryPreferenceTest {

    private var previousNode: String? = null
    private val scratchNodeName = "ServerPackCreator-test-HomeDirectoryPreference"

    @BeforeEach
    fun useAScratchNode() {
        previousNode = System.getProperty(ApiProperties.PREFERENCES_NODE_PROPERTY)
        System.setProperty(ApiProperties.PREFERENCES_NODE_PROPERTY, scratchNodeName)
        Preferences.userRoot().node(scratchNodeName).clear()
    }

    @AfterEach
    fun restoreTheBuildsNode() {
        Preferences.userRoot().node(scratchNodeName).removeNode()
        previousNode
            ?.let { System.setProperty(ApiProperties.PREFERENCES_NODE_PROPERTY, it) }
            ?: System.clearProperty(ApiProperties.PREFERENCES_NODE_PROPERTY)
    }

    /** What the app stores must be readable under the node `-api` resolves, and under the key `-api` uses. */
    @Test
    fun theStoredHomeLandsWhereTheApiWillReadIt() {
        HomeDirectoryPreference.store("/tmp/spc-home")

        Assertions.assertEquals(
            "/tmp/spc-home",
            Preferences.userRoot().node(scratchNodeName).get(PathsConfig.HOME_DIRECTORY_KEY, null),
            "the app must write into the resolved node under the key -api reads, or the two silently disagree"
        )
        Assertions.assertEquals("/tmp/spc-home", HomeDirectoryPreference.stored())
    }

    /** An installation that has never had a home set reports none, rather than an empty string. */
    @Test
    fun anUnsetHomeReadsAsNull() {
        Assertions.assertNull(HomeDirectoryPreference.stored())
    }

    /**
     * The node is resolved per call, not captured once, so a host that claims its own node still gets its own home.
     * Without this the app would keep writing to whichever node happened to be current when the class loaded.
     */
    @Test
    fun theNodeFollowsTheResolverRatherThanBeingCapturedOnce() {
        HomeDirectoryPreference.store("/tmp/first-node-home")

        val otherNodeName = "$scratchNodeName-other"
        System.setProperty(ApiProperties.PREFERENCES_NODE_PROPERTY, otherNodeName)
        try {
            Assertions.assertNull(
                HomeDirectoryPreference.stored(),
                "switching the resolved node must switch which home is seen"
            )
            HomeDirectoryPreference.store("/tmp/second-node-home")
            Assertions.assertEquals("/tmp/second-node-home", HomeDirectoryPreference.stored())
        } finally {
            Preferences.userRoot().node(otherNodeName).removeNode()
            System.setProperty(ApiProperties.PREFERENCES_NODE_PROPERTY, scratchNodeName)
        }

        // The first node still holds its own value — the two are genuinely separate.
        Assertions.assertEquals("/tmp/first-node-home", HomeDirectoryPreference.stored())
    }
}
