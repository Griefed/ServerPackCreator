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
import java.time.Duration
import java.time.Instant

/**
 * Pins learning-from-the-abort. The grinder always boots the **newest** Minecraft a mod supports, so a loader
 * that *claims* support for the newest version but cannot actually build it wastes one boot per mod, forever.
 * Measured on 2026-07-30: Fabric's meta lists Minecraft `26.1.2` and returns a placeholder `0.0.0` intermediary,
 * `Meta.isMinecraftSupported` therefore says yes, and `start.sh` then aborts with *"Fabric is not available for
 * Minecraft 26.1.2"* — 103 wasted boots in a morning. Trusting the console over the metadata is the fix.
 */
internal class LoaderSupportMemoryTest {

    private var now = Instant.parse("2026-07-30T12:00:00Z")

    private fun memory(retention: Duration = Duration.ofHours(24)) = LoaderSupportMemory(retention) { now }

    @Test
    fun everythingIsUsableUntilProvenOtherwise() {
        Assertions.assertTrue(memory().isUsable("Fabric", "26.1.2"), "no experience yet ⇒ try it")
    }

    @Test
    fun aProvenUnbootableCombinationStopsBeingSelected() {
        val memory = memory()
        memory.rememberUnbootable("Fabric", "26.1.2", "start.sh aborted")

        Assertions.assertFalse(memory.isUsable("Fabric", "26.1.2"))
        Assertions.assertTrue(memory.isUsable("Fabric", "1.21.1"), "only that Minecraft version is affected")
        Assertions.assertTrue(memory.isUsable("NeoForge", "26.1.2"), "only that loader is affected")
    }

    /** It expires: a loader that publishes support later must get another chance without a restart. */
    @Test
    fun theMemoryExpiresSoUpstreamCanCatchUp() {
        val memory = memory(retention = Duration.ofHours(6))
        memory.rememberUnbootable("Fabric", "26.1.2", "start.sh aborted")

        now = now.plus(Duration.ofHours(5))
        Assertions.assertFalse(memory.isUsable("Fabric", "26.1.2"), "still within the retention window")

        now = now.plus(Duration.ofHours(2))
        Assertions.assertTrue(memory.isUsable("Fabric", "26.1.2"), "retention passed ⇒ try again")
    }

    @Test
    fun remembersEveryFailingCombinationSeparately() {
        val memory = memory()
        memory.rememberUnbootable("Fabric", "26.2", "abort")
        memory.rememberUnbootable("NeoForge", "1.21.1", "installer 404")

        Assertions.assertEquals(setOf("Fabric/26.2", "NeoForge/1.21.1"), memory.knownUnbootable())
    }

    /** Re-recording the same combination is harmless (and stays quiet — the warning is once per combination). */
    @Test
    fun recordingTheSameCombinationTwiceIsIdempotent() {
        val memory = memory()
        memory.rememberUnbootable("Fabric", "26.2", "first")
        memory.rememberUnbootable("Fabric", "26.2", "second")

        Assertions.assertEquals(setOf("Fabric/26.2"), memory.knownUnbootable())
        Assertions.assertFalse(memory.isUsable("Fabric", "26.2"))
    }
}
