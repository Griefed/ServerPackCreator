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
package de.griefed.serverpackcreator.clientside.platform

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Pins how a host hands this process the CurseForge key. A key in a file is read once and the file deleted, so a
 * server booted afterwards finds it neither in any process environment nor on disk.
 */
internal class CurseForgeApiKeyTest {

    @Test
    fun aKeyFileIsReadAndThenDeleted(@TempDir directory: File) {
        val keyFile = File(directory, "key").apply { writeText("the-key\n") }

        val key = CurseForgeApiKey.from(mapOf(CurseForgeApiKey.FILE_VARIABLE to keyFile.absolutePath))

        Assertions.assertEquals("the-key", key)
        Assertions.assertFalse(keyFile.exists())
    }

    @Test
    fun theKeyFileOutranksTheVariable(@TempDir directory: File) {
        val keyFile = File(directory, "key").apply { writeText("from-file") }

        val key = CurseForgeApiKey.from(
            mapOf(CurseForgeApiKey.FILE_VARIABLE to keyFile.absolutePath, CurseForgeApiKey.VARIABLE to "from-variable")
        )

        Assertions.assertEquals("from-file", key)
    }

    @Test
    fun withoutAFileTheVariableIsTheKey() {
        Assertions.assertEquals(
            "from-variable",
            CurseForgeApiKey.from(mapOf(CurseForgeApiKey.VARIABLE to "from-variable"))
        )
    }

    @Test
    fun aBlankOrMissingKeyIsNoKey(@TempDir directory: File) {
        val blank = File(directory, "blank").apply { writeText("  \n") }
        val absent = File(directory, "absent")

        Assertions.assertNull(CurseForgeApiKey.from(mapOf(CurseForgeApiKey.FILE_VARIABLE to blank.absolutePath)))
        Assertions.assertNull(CurseForgeApiKey.from(mapOf(CurseForgeApiKey.FILE_VARIABLE to absent.absolutePath)))
        Assertions.assertNull(CurseForgeApiKey.from(emptyMap()))
    }
}
