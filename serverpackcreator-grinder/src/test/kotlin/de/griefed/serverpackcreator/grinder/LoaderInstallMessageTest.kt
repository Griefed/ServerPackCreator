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

/**
 * What the operator is told when a loader install is unavailable.
 *
 * The old message was `No cached loader install for Forge 61.2.1 / Minecraft 1.21.11`, and it described the
 * one thing that had **not** happened. `LoaderCache.ensureInstalled` installs on a miss; it returns `null`
 * only when the install *failed*, or when the tuple is on cooldown after failing recently. So a reader
 * reasonably concluded the grinder was refusing to install something it could have installed, and went
 * looking for a missing feature that was already there. Reported by Griefed 2026-08-30.
 *
 * The reason matters twice over: this string becomes the verdict's detail, so it is what the report shows
 * for every candidate that wanted the tuple — and the cooldown path logs at DEBUG, so at default levels the
 * verdict is the *only* place the cause appears.
 */
internal class LoaderInstallMessageTest {

    @Test
    fun aFailedInstallSaysItFailedRatherThanThatNothingWasCached() {
        val message = ContainerCandidateVerifier.installUnavailableMessage(
            "Forge", "61.2.1", "1.21.11", onCooldown = false
        )

        Assertions.assertTrue(
            message.contains("failed", ignoreCase = true),
            "the message must say the install failed; it was attempted. Was: $message"
        )
        Assertions.assertFalse(
            message.contains("No cached loader install"),
            "a cache miss is what triggers an install, so it cannot be the reason one is unavailable. Was: $message"
        )
        Assertions.assertTrue(
            message.contains("Forge 61.2.1") && message.contains("1.21.11"),
            "the tuple must still be named — it is how an operator finds the install's own log line. Was: $message"
        )
    }

    /**
     * The cooldown is the case that reads worst in a log: nothing is attempted, nothing is logged above
     * DEBUG, and every candidate wanting the tuple is scored INCONCLUSIVE. The verdict has to carry it.
     */
    @Test
    fun aCooldownSaysSoAndSaysItIsTemporary() {
        val message = ContainerCandidateVerifier.installUnavailableMessage(
            "NeoForge", "21.1.247", "1.21.1", onCooldown = true
        )

        Assertions.assertTrue(
            message.contains("cooldown", ignoreCase = true),
            "a suppressed retry must say so rather than looking like a fresh failure. Was: $message"
        )
        Assertions.assertTrue(
            message.contains("NeoForge 21.1.247"),
            "the tuple must be named. Was: $message"
        )
    }

    /** The two cases must not read identically, or the distinction buys nothing. */
    @Test
    fun theTwoCasesAreDistinguishable() {
        val failed = ContainerCandidateVerifier.installUnavailableMessage("Forge", "61.2.1", "1.21.11", false)
        val cooling = ContainerCandidateVerifier.installUnavailableMessage("Forge", "61.2.1", "1.21.11", true)

        Assertions.assertNotEquals(failed, cooling)
    }
}
