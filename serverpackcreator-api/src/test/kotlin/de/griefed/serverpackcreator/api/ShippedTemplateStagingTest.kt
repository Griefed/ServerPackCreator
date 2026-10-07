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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Guards what `ApiWrapper.stageOne()` actually writes into the user's `server_files` directory.
 *
 * Staging is the one step every single launch performs, for every user, before anything else, so a resource
 * that is named but not shipped is written into every home that ever exists. Nothing else notices:
 * `PathsConfigTest` asserts what the *path* is, never that anything is behind it, and a template nothing
 * reads back - no `bat` template is registered for Java installers - can sit there empty indefinitely.
 *
 * An empty template is the symptom worth pinning rather than any one file name. Whatever is staged, it
 * came out of the jar, and nothing in the jar is empty - so a zero-length file there means a resource
 * that could not be read, whichever one it is.
 *
 * @author Griefed
 */
internal class ShippedTemplateStagingTest {

    /** Booting the wrapper is what runs `stageOne()`; the fixture has to be created by the test itself. */
    private val api = ApiWrapper.api(File("build/resources/test/serverpackcreator.properties"))

    /** Nothing staged into `server_files` may be empty, because nothing in the jar is. */
    @Test
    fun stagingWritesNoEmptyFile() {
        val staged = api.apiProperties.serverFilesDirectory.listFiles()?.filter { it.isFile } ?: emptyList()

        Assertions.assertTrue(staged.isNotEmpty(), "nothing was staged at all - the fixture is wrong, not the code")
        val empty = staged.filter { it.length() == 0L }
        Assertions.assertTrue(
            empty.isEmpty(),
            "these staged files are empty, so their jar resource could not be read: ${empty.map { it.name }}"
        )
    }
}
