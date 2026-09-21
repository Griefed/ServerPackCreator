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
import java.net.http.HttpClient

/**
 * Pins that default-constructed [JdkHttpFetcher]s share **one** [HttpClient].
 *
 * Not a style preference — the JDK's own documentation names the alternative as the anti-pattern:
 * *"Creating a new client for each operation, though possible, will usually prevent reusing such
 * connections."* Each client carries its own connection pool and its own `SelectorManager` thread, so one
 * per fetcher means every candidate re-does the TLS handshake to Modrinth/CurseForge that the previous one
 * had already paid for, and leaves a thread behind until the client is collected.
 *
 * Measured on the live daemon on 2026-09-21: **384 clients created in 2.9 hours, 28 still alive** — because
 * `ContainerCandidateVerifier.verifyStaged` calls `supportedPlatforms()` per candidate, and each call builds
 * a `ModrinthPlatform` and a `CurseForgePlatform` that each default-construct a fetcher.
 *
 * Sharing is safe because neither platform puts credentials on the client: the CurseForge API key travels as
 * an `x-api-key` **request header** through `HttpFetcher.get(url, headers)`, never as client state, and
 * `HttpClient` is documented immutable and thread-safe.
 *
 * @author Griefed
 */
internal class JdkHttpFetcherClientSharingTest {

    /**
     * The defect itself: two fetchers built the default way must be pointing at the same client. Identity is
     * the assertion because identity is precisely the property — anything weaker would stay green against a
     * fresh client per instance.
     */
    @Test
    fun defaultFetchersShareOneClient() {
        Assertions.assertSame(
            JdkHttpFetcher().client,
            JdkHttpFetcher().client,
            "each default-constructed JdkHttpFetcher built its own HttpClient - that is one connection pool " +
                "and one SelectorManager thread per fetcher, and the JDK names it an anti-pattern"
        )
    }

    /**
     * The same statement at the scale it actually bites: a run of fetchers must collapse to a single distinct
     * client. Twenty-five stands in for a pass's worth of candidates, and counting distinct identities is what
     * turns "they are equal" into "there is only one".
     */
    @Test
    fun manyFetchersCollapseToOneClient() {
        val distinct = (1..25).map { JdkHttpFetcher().client }
            .distinctBy { System.identityHashCode(it) }
        Assertions.assertEquals(
            1,
            distinct.size,
            "25 default-constructed fetchers produced ${distinct.size} distinct HttpClients"
        )
    }

    /**
     * The sharing must not swallow an explicit choice: a caller that supplies its own client — every test that
     * fakes the transport, and anything that ever needs a different timeout — still gets exactly that one.
     */
    @Test
    fun anExplicitlySuppliedClientIsStillHonoured() {
        val own = HttpClient.newHttpClient()
        Assertions.assertSame(own, JdkHttpFetcher(own).client, "an explicitly supplied client was ignored")
    }
}
