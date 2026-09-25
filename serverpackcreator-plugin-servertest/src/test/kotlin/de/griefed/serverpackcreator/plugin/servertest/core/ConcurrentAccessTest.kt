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
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Pins that the two units reached from more than one thread really are safe there.
 *
 * Both carry `@Synchronized`, and every other guard in this module calls them from one thread, so the
 * annotations were claims rather than facts. In the running plugin a port is taken on the event dispatch
 * thread and released from whichever session's reader thread noticed its process exit, and the registry is
 * read and written from both.
 */
internal class ConcurrentAccessTest {

    /** Run [work] on [threads] threads released together, so they genuinely overlap. */
    private fun <T> inParallel(threads: Int, work: (Int) -> T): List<T> {
        val start = CountDownLatch(1)
        val done = CountDownLatch(threads)
        val results = Collections.synchronizedList(mutableListOf<T>())
        repeat(threads) { index ->
            Thread {
                start.await()
                runCatching { work(index) }.onSuccess { results.add(it) }
                done.countDown()
            }.apply { isDaemon = true }.start()
        }
        start.countDown()
        Assertions.assertTrue(done.await(30, TimeUnit.SECONDS), "Parallel work did not finish in time.")
        return results.toList()
    }

    /**
     * Ten threads asking at once get ten different ports.
     *
     * A duplicate here is two servers told to bind the same socket, so the second fails to start with an
     * error pointing at the wrong thing entirely.
     */
    @Test
    fun concurrentAllocationsNeverCollide() {
        val allocator = PortAllocator(32000, 32009, isFree = { true })

        val ports = inParallel(10) { allocator.allocate() }.filterNotNull()

        Assertions.assertEquals(10, ports.size, "Every thread should have been given a port.")
        Assertions.assertEquals(10, ports.distinct().size, "Two threads were handed the same port: $ports")
    }

    /**
     * Only one of ten simultaneous Start presses may win the same pack.
     *
     * Weaker than its neighbour, and worth saying so: dropping `@Synchronized` from `register` leaves this
     * green — measured — because the window between the containment check and the put is a few
     * instructions and ten threads rarely land inside it. It pins the contract, not the annotation. The
     * allocator's guard above *does* catch its annotation being removed, so the two are not equivalent
     * evidence.
     */
    @Test
    fun onlyOneConcurrentRegistrationWinsAPack(@TempDir packDir: File) {
        val registry = SessionRegistry()

        val accepted = inParallel(10) {
            registry.register(packDir, ServerSession(packDir, listOf("bash", "start.sh"), {}, {}, {}))
        }.count { it }

        Assertions.assertEquals(1, accepted, "Exactly one registration may win; two servers share one world.")
        Assertions.assertEquals(1, registry.runningCount())
    }
}
