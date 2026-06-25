/* Copyright (C) 2025 Griefed
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
package de.griefed.serverpackcreator.app.clientside

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Pins how a finished server-boot is read from its console-output and exit-state, including the
 * deliberate asymmetry that only a crash (not a clean boot) is a strong clientside signal.
 */
internal class BootLogClassifierTest {

    @Test
    fun readyLineMeansSurvivedEvenWhenLaterKilled() {
        val lines = listOf(
            "[12:00:01] [Server thread/INFO]: Preparing level \"world\"",
            "[12:00:21] [Server thread/INFO]: Done (21.473s)! For help, type \"help\""
        )
        // exit is non-zero because we force-kill a server that reached ready — SURVIVED still wins.
        Assertions.assertEquals(BootResult.SURVIVED, BootLogClassifier.classify(lines, 137, timedOut = false))
    }

    @Test
    fun nonZeroExitWithoutReadyMeansCrashed() {
        val lines = listOf(
            "[12:00:03] [Server thread/ERROR]: Encountered an unexpected exception",
            "java.lang.NoClassDefFoundError: net/minecraft/client/Minecraft"
        )
        Assertions.assertEquals(BootResult.CRASHED, BootLogClassifier.classify(lines, 1, timedOut = false))
    }

    @Test
    fun timeoutWithoutReadyIsInconclusive() {
        Assertions.assertEquals(
            BootResult.INCONCLUSIVE,
            BootLogClassifier.classify(listOf("still installing..."), exitCode = null, timedOut = true)
        )
    }

    @Test
    fun cleanZeroExitWithoutReadyIsInconclusive() {
        Assertions.assertEquals(
            BootResult.INCONCLUSIVE,
            BootLogClassifier.classify(listOf("stopping..."), exitCode = 0, timedOut = false)
        )
    }

    @Test
    fun crashExcerptStartsAtFirstErrorMarker() {
        val lines = listOf(
            "[12:00:01] [Server thread/INFO]: Loading mods",
            "[12:00:02] [Server thread/INFO]: Loading examplemod",
            "[12:00:03] [Server thread/ERROR]: Encountered an unexpected exception",
            "java.lang.NoClassDefFoundError: net/minecraft/client/Minecraft",
            "\tat examplemod.ClientThing.<init>(ClientThing.java:10)"
        )
        val excerpt = BootLogExcerpt.crashExcerpt(lines)!!
        Assertions.assertTrue(excerpt.startsWith("[12:00:03] [Server thread/ERROR]"))
        Assertions.assertTrue(excerpt.contains("NoClassDefFoundError"))
        Assertions.assertFalse(excerpt.contains("Loading mods"))
    }

    @Test
    fun crashExcerptFallsBackToTailWhenNoMarker() {
        val lines = (1..100).map { "line $it" }
        val excerpt = BootLogExcerpt.crashExcerpt(lines, maxLines = 10)!!
        Assertions.assertTrue(excerpt.contains("line 91"))
        Assertions.assertFalse(excerpt.contains("line 90"))
    }

    @Test
    fun crashExcerptIsNullForEmptyLog() {
        Assertions.assertNull(BootLogExcerpt.crashExcerpt(emptyList()))
    }
}
