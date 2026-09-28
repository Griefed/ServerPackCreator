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
package de.griefed.serverpackcreator.plugin.grinder.core

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Pins the one rule for reading an operator-typed grinder address. Both the Settings pane and the HTTP
 * client resolve the configured URL through [GrinderUrl], so what the pane calls valid and what the
 * client actually requests cannot drift — the failure where a field says "saved" and every fetch then
 * 404s against a subtly different address.
 */
internal class GrinderUrlTest {

    /**
     * The address an operator types after reading the grinder's README, which prints
     * `http://localhost:8757/` with a trailing slash. Paths are built by appending `/verdicts.json`, so
     * a surviving trailing slash would request `//verdicts.json`.
     */
    @Test
    fun stripsTrailingSlashesAndSurroundingWhitespace() {
        Assertions.assertEquals("http://localhost:8757", GrinderUrl.normalise("http://localhost:8757/"))
        Assertions.assertEquals("http://localhost:8757", GrinderUrl.normalise("  http://localhost:8757///  "))
        Assertions.assertEquals("http://localhost:8757", GrinderUrl.normalise("http://localhost:8757"))
    }

    /**
     * `localhost:8757` is what an operator copies out of the log line the grinder prints. Left alone it
     * parses as the scheme `localhost`, so it is assumed to be http rather than rejected — the daemon
     * binds plain http by default, and refusing the most likely input teaches nothing.
     */
    @Test
    fun assumesHttpWhenNoSchemeWasTyped() {
        Assertions.assertEquals("http://localhost:8757", GrinderUrl.normalise("localhost:8757"))
        Assertions.assertEquals("http://grinder.example.com", GrinderUrl.normalise("grinder.example.com"))
    }

    /** A configured https grinder behind a reverse proxy keeps its scheme. */
    @Test
    fun preservesAnExplicitHttpsScheme() {
        Assertions.assertEquals("https://grinder.example.com", GrinderUrl.normalise("https://grinder.example.com/"))
    }

    /**
     * Unusable input resolves to `null` rather than to a string that fails later: an empty setting is the
     * shipped default and means "idle", and a scheme the HTTP client cannot speak must be refused where
     * the operator can still see the field, not at the first fetch.
     */
    @Test
    fun rejectsWhatCannotBeRequested() {
        Assertions.assertNull(GrinderUrl.normalise(null))
        Assertions.assertNull(GrinderUrl.normalise(""))
        Assertions.assertNull(GrinderUrl.normalise("   "))
        Assertions.assertNull(GrinderUrl.normalise("ftp://grinder.example.com"))
        Assertions.assertNull(GrinderUrl.normalise("file:///etc/passwd"))
        Assertions.assertNull(GrinderUrl.normalise("http://"))
    }

    /** The endpoints are derived from the base, so that derivation is the thing to pin, not each caller. */
    @Test
    fun derivesTheEndpointsFromTheBase() {
        Assertions.assertEquals("http://localhost:8757/verdicts.json", GrinderUrl.verdicts("http://localhost:8757"))
        Assertions.assertEquals("http://localhost:8757/status", GrinderUrl.status("http://localhost:8757"))
    }
}
