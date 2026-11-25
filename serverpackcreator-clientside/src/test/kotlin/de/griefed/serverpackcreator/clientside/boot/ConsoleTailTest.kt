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
package de.griefed.serverpackcreator.clientside.boot

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Pins the console tail the report shows. A mod can spam its log for a whole boot budget, so the tail is read from
 * the end of the file within a bounded window, and a console that cannot be read is no console rather than an
 * exception that would cost the whole report.
 */
internal class ConsoleTailTest {

    @Test
    fun aShortConsoleIsReadWhole(@TempDir directory: File) {
        val console = File(directory, "boot.log").apply { writeText("one\ntwo\nthree\n") }

        Assertions.assertEquals(listOf("one", "two", "three"), ConsoleTail.lastLines(console, 200))
    }

    @Test
    fun aLongConsoleYieldsExactlyItsLastLines(@TempDir directory: File) {
        val console = File(directory, "boot.log")
        console.printWriter().use { writer -> (1..50_000).forEach { writer.println("[Server thread/INFO]: line $it") } }

        val tail = ConsoleTail.lastLines(console, 200)

        Assertions.assertEquals((49_801..50_000).map { "[Server thread/INFO]: line $it" }, tail)
    }

    /** Only the window at the end is read: lines that do not fit it are not returned, and nor is a cut first line. */
    @Test
    fun onlyTheWindowAtTheEndIsRead(@TempDir directory: File) {
        val console = File(directory, "boot.log")
        console.printWriter().use { writer -> repeat(2_000) { writer.println("x".repeat(1_000)) }; writer.println("last") }

        val tail = ConsoleTail.lastLines(console, 200, windowBytes = 10_000)

        Assertions.assertEquals("last", tail?.last())
        Assertions.assertTrue(tail.orEmpty().sumOf { it.length + 1 } <= 10_000, "${tail?.size} lines")
        Assertions.assertTrue(tail.orEmpty().all { it == "last" || it.length == 1_000 }, "no partial first line")
    }

    @Test
    fun anUnreadableConsoleIsNoConsole(@TempDir directory: File) {
        Assertions.assertNull(ConsoleTail.lastLines(directory, 200))
        Assertions.assertNull(ConsoleTail.lastLines(File(directory, "absent.log"), 200))
    }
}
