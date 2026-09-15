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
package de.griefed.serverpackcreator.grinder.loader

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * **An exception's `message` is not a diagnosis, and sometimes it is not even a string.**
 *
 * From the live daemon, 2026-09-03, two consecutive lines of the same outage:
 *
 * ```
 * Loader install threw for NeoForge 26.2.0.26-beta / Minecraft 26.2: Status 404: {"message":"No such image: …"}
 * Loader install threw for NeoForge 21.1.23 / Minecraft 1.21.1: null
 * ```
 *
 * The second one is the whole problem: `${it.message}` on a throwable that carries none prints `null`, so the
 * operator is told a tuple failed and nothing whatsoever about why — not even the exception's type, which is
 * free. Naming the class costs nothing and is the difference between "some NPE in our own staging" and "the
 * daemon refused us".
 */
internal class LoaderInstallThrowMessageTest {

    @Test
    fun aThrowableWithNoMessageIsNamedByItsType() {
        val message = LoaderCache.installThrewMessage(
            "NeoForge", "21.1.23", "1.21.1", NullPointerException()
        )

        Assertions.assertTrue(
            message.contains("NullPointerException"),
            "a throwable with no message must still be identified by type, or the line says nothing at all. " +
                "Was: $message"
        )
        Assertions.assertFalse(
            message.endsWith("null"),
            "the live daemon printed a line ending in a bare 'null'; that is the defect. Was: $message"
        )
    }

    @Test
    fun aThrowableWithAMessageKeepsBothTypeAndMessage() {
        val message = LoaderCache.installThrewMessage(
            "NeoForge", "26.2.0.26-beta", "26.2",
            IllegalStateException("Status 404: {\"message\":\"No such image: spc-grinder-runtime:latest\"}")
        )

        Assertions.assertTrue(message.contains("IllegalStateException"), "the type is what says whose fault it is. Was: $message")
        Assertions.assertTrue(message.contains("No such image"), "the message is the detail; it must survive. Was: $message")
    }

    /** The tuple is how an operator finds the tuple's own install log, so it stays in every variant. */
    @Test
    fun theTupleIsAlwaysNamed() {
        val message = LoaderCache.installThrewMessage("Fabric", "0.19.3", "26.1", RuntimeException())

        Assertions.assertTrue(
            message.contains("Fabric 0.19.3") && message.contains("26.1"),
            "Was: $message"
        )
    }
}
