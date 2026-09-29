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
package de.griefed.serverpackcreator.plugin.servertest.core

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.net.ServerSocket
import java.util.Random

/**
 * Pins that a running server is given a port nothing else holds.
 *
 * Two halves, and both are needed. Most of it is exercised through an injected predicate, so the *scan* can
 * be pinned exactly — which port is tried first, what happens when one is busy, what happens when they all
 * are. The last test uses the real bind test against a really-held socket, because whether a port is free is
 * a question only the operating system can answer and a fake one answers it by construction.
 */
internal class PortAllocatorTest {

    /** A [Random] whose first `nextInt` is fixed, so the scan's starting offset is known rather than guessed. */
    private fun randomStartingAt(offset: Int) = object : Random() {
        override fun nextInt(bound: Int): Int = offset % bound
    }

    /** The scan starts where the RNG points, which is what "a randomly chosen port" has to mean. */
    @Test
    fun startsTheScanWhereTheRandomSourcePoints() {
        val allocator = PortAllocator(30000, 30009, randomStartingAt(4), isFree = { true })

        Assertions.assertEquals(30004, allocator.allocate())
    }

    /** A busy port is stepped over rather than handed out. */
    @Test
    fun skipsAPortThatIsNotBindable() {
        val busy = setOf(30004, 30005)
        val allocator = PortAllocator(30000, 30009, randomStartingAt(4), isFree = { it !in busy })

        Assertions.assertEquals(30006, allocator.allocate())
    }

    /** Two servers must never be handed the same port, even though both bind-test as free beforehand. */
    @Test
    fun neverHandsOutAPortItHasAlreadyAllocated() {
        val allocator = PortAllocator(30000, 30002, randomStartingAt(0), isFree = { true })

        val handedOut = listOf(allocator.allocate(), allocator.allocate(), allocator.allocate())

        Assertions.assertEquals(setOf(30000, 30001, 30002), handedOut.toSet())
        Assertions.assertEquals(3, handedOut.distinct().size, "A port was handed out twice: $handedOut")
    }

    /** The scan wraps, so a starting offset near the end still reaches the ports below it. */
    @Test
    fun wrapsAroundTheEndOfTheRange() {
        val allocator = PortAllocator(30000, 30002, randomStartingAt(2), isFree = { it != 30002 })

        Assertions.assertEquals(30000, allocator.allocate())
    }

    /** A stopped server's port becomes available again; otherwise a long session would exhaust the range. */
    @Test
    fun aReleasedPortCanBeAllocatedAgain() {
        val allocator = PortAllocator(30000, 30000, randomStartingAt(0), isFree = { true })
        val first = allocator.allocate()
        Assertions.assertEquals(30000, first)
        Assertions.assertNull(allocator.allocate(), "The only port in the range is taken.")

        allocator.release(first!!)

        Assertions.assertEquals(30000, allocator.allocate())
    }

    /** Nothing free means `null`, so the caller can say so rather than launch a server that cannot bind. */
    @Test
    fun yieldsNullWhenEveryPortInTheRangeIsBusy() {
        val allocator = PortAllocator(30000, 30009, randomStartingAt(0), isFree = { false })

        Assertions.assertNull(allocator.allocate())
    }

    /**
     * A configured range is clamped, not rejected. These numbers come from a hand-editable `config.toml` and
     * the allocator is built while the tab is, so a typo must not take the GUI's tab assembly down — and
     * binding below 1024 needs privileges no launch should require.
     */
    @Test
    fun clampsAConfiguredRangeIntoUsablePorts() {
        Assertions.assertEquals(
            PortAllocator.LOWEST_UNPRIVILEGED_PORT,
            PortAllocator(80, 30000).range.first,
            "A privileged port must be clamped up, not honoured."
        )
        Assertions.assertTrue(
            PortAllocator(30000, 10).range.last >= 30000,
            "An inverted range must not produce an empty one."
        )
        Assertions.assertEquals(PortAllocator.HIGHEST_PORT, PortAllocator(30000, 99999).range.last)
    }

    /**
     * The real bind test, against a really-held socket.
     *
     * Everything above runs on an injected predicate, which is exactly why this one cannot: whether a port is
     * free is a question only the operating system answers, and the default predicate is the one shipped. A
     * mock agrees with itself.
     */
    @Test
    fun theRealBindTestRefusesAPortSomethingIsListeningOn() {
        ServerSocket(0).use { held ->
            val heldPort = held.localPort

            Assertions.assertFalse(
                PortAllocator.isPortBindable(heldPort),
                "Port $heldPort is being listened on by this very test, so it is not bindable."
            )
            Assertions.assertEquals(
                heldPort,
                PortAllocator(heldPort, heldPort, randomStartingAt(0), isFree = { true }).allocate(),
                "Sanity check: the range really is the single held port, so the refusal above is the bind test's."
            )
            Assertions.assertNull(
                PortAllocator(heldPort, heldPort).allocate(),
                "With the real bind test, a range of only the held port has nothing to hand out."
            )
        }
    }
}
