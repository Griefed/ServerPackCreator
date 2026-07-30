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
 * Pins the narrow predicate that feeds [LoaderSupportMemory]. It must fire for "the loader has no build for this
 * Minecraft" and for nothing else: the other pre-launch aborts (Java, EULA, variables) say nothing about loader
 * support, and marking a combination unbootable on their account would stop selecting a perfectly good one.
 */
internal class BootLogClassifierLoaderUnavailableTest {

    /** The real line, verbatim from a live boot on 2026-07-30. */
    @Test
    fun recognisesTheLoaderUnavailableAbort() {
        Assertions.assertTrue(
            BootLogClassifier.loaderUnavailable(
                listOf(
                    "Detected 26.1.2 - Java 25",
                    "Running Fabric checks and setup...",
                    "Fabric is not available for Minecraft 26.1.2, Fabric 0.19.3."
                )
            )
        )
    }

    @Test
    fun doesNotFireOnOtherPreLaunchAborts() {
        listOf(
            listOf("Could not download the launcher jar.", "Exiting..."),
            listOf("You need to agree to the EULA before running the server."),
            listOf("Java version could not be determined, aborting."),
            listOf("[Server thread/INFO]: Done (4.2s)! For help, type \"help\"")
        ).forEach { console ->
            Assertions.assertFalse(BootLogClassifier.loaderUnavailable(console), "should not fire on: $console")
        }
    }

    @Test
    fun anEmptyConsoleSaysNothing() {
        Assertions.assertFalse(BootLogClassifier.loaderUnavailable(emptyList()))
    }
}
