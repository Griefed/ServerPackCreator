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
package de.griefed.serverpackcreator.api

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Pins which `Preferences` node ServerPackCreator stores its home directory in.
 *
 * The node used to be the hard-coded `ServerPackCreator`, i.e. **one machine-wide, per-user node shared by every
 * SPC process on the account** — GUI, web backend, every module's test suite and the grinder daemon. Since
 * `PathsConfig.homeDirectory` re-reads that preference on *every* access and writes back what it resolved, a test
 * suite starting up relocated a *running* daemon's home into its own scratch directory and then deleted it. What an
 * operator saw was every boot failing on a missing `server_files/server-icon.png`, recorded as metadata-only
 * verdicts — indistinguishable from "these mods were never bootable" (measured 2026-07-30: 30+ polluted verdicts).
 * The same collision runs the other way: running the suites moved a developer's own GUI installation's home.
 *
 * An overridable node name lets each of those live in its own node, which is what makes the collision impossible
 * rather than merely unlikely.
 */
internal class PreferencesNodeTest {

    /** Default behaviour must be unchanged — existing installations keep reading their own settings. */
    @Test
    fun defaultsToTheSharedNodeWhenNothingOverridesIt() {
        Assertions.assertEquals(
            "ServerPackCreator",
            ApiProperties.resolvePreferencesNode(property = null, environment = null),
            "changing the default would orphan every existing installation's stored settings"
        )
    }

    /** The system property is the in-process lever: a host can set it before building an `ApiWrapper`. */
    @Test
    fun aSystemPropertyOverridesTheDefault() {
        Assertions.assertEquals(
            "ServerPackCreator-grinder",
            ApiProperties.resolvePreferencesNode(property = "ServerPackCreator-grinder", environment = null)
        )
    }

    /** The environment variable serves launchers that cannot set a system property (scripts, containers). */
    @Test
    fun anEnvironmentVariableOverridesTheDefault() {
        Assertions.assertEquals(
            "ServerPackCreator-ci",
            ApiProperties.resolvePreferencesNode(property = null, environment = "ServerPackCreator-ci")
        )
    }

    /** Both set: the in-process property wins, since it is the more specific, deliberately-set one. */
    @Test
    fun theSystemPropertyWinsOverTheEnvironmentVariable() {
        Assertions.assertEquals(
            "from-property",
            ApiProperties.resolvePreferencesNode(property = "from-property", environment = "from-environment")
        )
    }

    /**
     * A blank override is a misconfiguration, not a request for the root node: `Preferences.userRoot().node("")`
     * returns the *root*, which would scatter SPC's keys across a node shared with every other Java application.
     */
    @Test
    fun blankOverridesFallBackToTheDefaultInsteadOfTheRootNode() {
        Assertions.assertEquals("ServerPackCreator", ApiProperties.resolvePreferencesNode(property = "   ", environment = null))
        Assertions.assertEquals("ServerPackCreator", ApiProperties.resolvePreferencesNode(property = "", environment = ""))
        Assertions.assertEquals(
            "ServerPackCreator-env",
            ApiProperties.resolvePreferencesNode(property = " ", environment = "ServerPackCreator-env"),
            "a blank property must not mask a usable environment value"
        )
    }

    /**
     * The test suites themselves must not run on the shared node — that is the bug this exists to prevent, and the
     * build sets the property for every test task. Asserting it here means a lost build-script line fails a test
     * instead of silently moving a developer's GUI home again.
     */
    @Test
    fun theTestSuiteItselfRunsOnAnIsolatedNode() {
        val node = ApiProperties.resolvePreferencesNode()
        Assertions.assertNotEquals(
            "ServerPackCreator",
            node,
            "this suite is running on the shared node — the build must set ${ApiProperties.PREFERENCES_NODE_PROPERTY}"
        )
        Assertions.assertTrue(node.startsWith("ServerPackCreator"), "an isolated node should still be recognisable, was '$node'")
    }
}
