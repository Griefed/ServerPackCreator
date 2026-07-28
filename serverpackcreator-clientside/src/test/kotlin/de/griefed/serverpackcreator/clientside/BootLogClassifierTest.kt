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
    fun loaderNotAvailableForMinecraftIsInconclusiveNotCrashed() {
        // Verbatim from a grinder e2e: Fabric has no build for a brand-new Minecraft, so start.sh
        // aborts before the mod is ever loaded. Scoring this CRASHED was a false clientside HIGH.
        val lines = listOf(
            "Detected 26.2. - Java 25",
            "Running Fabric checks and setup...",
            "Fabric is not available for Minecraft 26.2, Fabric 0.19.3."
        )
        Assertions.assertEquals(BootResult.INCONCLUSIVE, BootLogClassifier.classify(lines, 1, timedOut = false))
    }

    @Test
    fun loaderLauncherDownloadFailureIsInconclusive() {
        val lines = listOf(
            "Running Quilt checks and setup...",
            "quilt-server-launch.jar not found. Maybe the Quilt servers are having trouble. Please try again in a couple of minutes and check your internet connection."
        )
        Assertions.assertEquals(BootResult.INCONCLUSIVE, BootLogClassifier.classify(lines, 1, timedOut = false))
    }

    @Test
    fun environmentSetupAbortsAreInconclusive() {
        // A spread of the pre-launch crashServer failures — none are the mod's fault.
        listOf(
            "Something went wrong during the server installation. Please try again in a couple of minutes and check your internet connection.",
            "Java installation failed. Couldn't find /opt/java-25/bin/java.",
            "User did not agree to Mojang's EULA. Entered: no."
        ).forEach { marker ->
            Assertions.assertEquals(
                BootResult.INCONCLUSIVE,
                BootLogClassifier.classify(listOf(marker), exitCode = 1, timedOut = false),
                "setup abort must not be a crash: $marker"
            )
        }
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
