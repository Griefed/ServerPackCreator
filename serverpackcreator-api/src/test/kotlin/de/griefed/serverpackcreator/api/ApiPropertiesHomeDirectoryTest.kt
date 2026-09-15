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
package de.griefed.serverpackcreator.api

import de.griefed.serverpackcreator.api.settings.PathsConfig
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Pins what happens when SPC's home directory is not somewhere it can write.
 *
 * The construction sequence made this the worst kind of failure. `ApiProperties.init` writes `log4j2.xml` into the
 * home inside a `try`/`catch` that only *prints* the failure, and then reads that same file unguarded to stamp the
 * log level in — so an unwritable home surfaced as `java.io.FileNotFoundException: /log4j2.xml (No such file or
 * directory)` out of `setLoggingLevel`, an exception that names a file nobody configured, blames the wrong
 * operation, and killed the process. Reproduced 2026-08-22 by running the installed grinder distribution from `/`,
 * as `systemd` does for a unit without `WorkingDirectory=`.
 *
 * The home here is an existing *regular file*: the resolver cannot turn that into a directory, and neither can
 * root, so the pin means the same thing for every user id the suite might run as.
 */
internal class ApiPropertiesHomeDirectoryTest {

    /** Saved so [restoreHomeDirectoryProperty] can put the build's scratch home back for later test classes. */
    private var homeDirectoryProperty: String? = null

    /** Remembers the build's home `-D` before a test replaces it with an unusable one. */
    @BeforeEach
    fun rememberHomeDirectoryProperty() {
        homeDirectoryProperty = System.getProperty(PathsConfig.HOME_DIRECTORY_KEY)
    }

    /** Restores the build's home `-D`, so a test here cannot send another class's writes into a bogus home. */
    @AfterEach
    fun restoreHomeDirectoryProperty() {
        homeDirectoryProperty
            ?.let { System.setProperty(PathsConfig.HOME_DIRECTORY_KEY, it) }
            ?: System.clearProperty(PathsConfig.HOME_DIRECTORY_KEY)
    }

    /**
     * An unusable home must fail with a message an operator can act on: the path, and how to point SPC elsewhere.
     */
    @Test
    fun anUnusableHomeDirectoryFailsWithAnActionableError(@TempDir tempDir: File) {
        val notADirectory = File(tempDir, "home-that-is-a-file").apply { writeText("in the way") }
        System.setProperty(PathsConfig.HOME_DIRECTORY_KEY, notADirectory.absolutePath)

        val thrown = Assertions.assertThrows(IllegalStateException::class.java) {
            ApiProperties(File(tempDir, "serverpackcreator.properties"))
        }

        val message = thrown.message ?: ""
        Assertions.assertTrue(
            message.contains(notADirectory.absolutePath),
            "the error must name the home it could not use, or the operator has nothing to go on: $message"
        )
        Assertions.assertTrue(
            message.contains(PathsConfig.HOME_DIRECTORY_KEY),
            "the error must name the override that fixes it (${PathsConfig.HOME_DIRECTORY_KEY}): $message"
        )
    }
}
