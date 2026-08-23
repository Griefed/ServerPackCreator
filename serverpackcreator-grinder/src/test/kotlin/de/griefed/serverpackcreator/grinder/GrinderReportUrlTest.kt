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
package de.griefed.serverpackcreator.grinder

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.net.URI

/**
 * The report URL the daemon logs at startup has to be one an operator can paste into a browser.
 *
 * It is the only place the address is ever handed to a human — the journal is where they go looking for it —
 * and two of the four addresses a bind can legitimately take do not survive naive interpolation: the wildcard
 * is not a destination, and an IPv6 literal needs brackets or the URL will not parse at all.
 */
internal class GrinderReportUrlTest {

    /** Every URL logged must parse and carry a host, whatever the bind address was. */
    @Test
    fun everyBindAddressYieldsAParsableUrl() {
        for (bind in listOf("127.0.0.1", "172.19.0.1", "0.0.0.0", "::1", "::", "fe80::1")) {
            val url = GrinderApplication.reportUrl(bind, 8757)
            val parsed = URI.create(url)
            Assertions.assertEquals("http", parsed.scheme, "not a usable URL for bind '$bind': $url")
            Assertions.assertEquals(8757, parsed.port, "port lost for bind '$bind': $url")
            Assertions.assertFalse(parsed.host.isNullOrBlank(), "no host parsed for bind '$bind': $url")
        }
    }

    /**
     * A wildcard bind means "every interface", which is not somewhere a browser can go. The report is
     * reachable on loopback in that case, so that is what the operator gets told.
     */
    @Test
    fun aWildcardBindIsReportedAsLoopback() {
        Assertions.assertEquals("http://127.0.0.1:8757", GrinderApplication.reportUrl("0.0.0.0", 8757))
        Assertions.assertEquals("http://[::1]:8757", GrinderApplication.reportUrl("::", 8757))
    }

    /** IPv6 literals are bracketed, per RFC 3986 — unbracketed, the port is unparsable. */
    @Test
    fun ipv6LiteralsAreBracketed() {
        Assertions.assertEquals("http://[::1]:8757", GrinderApplication.reportUrl("::1", 8757))
        Assertions.assertEquals("http://[fe80::1]:9090", GrinderApplication.reportUrl("fe80::1", 9090))
    }

    /** The ordinary cases are untouched: a concrete IPv4 address is already exactly what it should be. */
    @Test
    fun concreteIpv4AddressesAreLeftAlone() {
        Assertions.assertEquals("http://127.0.0.1:8757", GrinderApplication.reportUrl("127.0.0.1", 8757))
        Assertions.assertEquals("http://172.19.0.1:9090", GrinderApplication.reportUrl("172.19.0.1", 9090))
    }
}
