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
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Pins the file behind the learned id-to-project map, which is what makes it worth learning at all: a
 * daemon that forgets on restart re-pays every probe download it has ever made.
 *
 * Mirrors `JsonCursorStore`'s contract deliberately, because the failure modes are the same and the
 * daemon's answer to them has to be: an unreadable document is logged and treated as empty rather than
 * refusing to start, and every write goes temp-then-atomic-move so a crash mid-write cannot truncate it.
 *
 * @author Griefed
 */
internal class JsonLearnedModIdsTest {

    /** The point of the file: what one run proved, the next run starts with. */
    @Test
    fun whatOneRunLearnsTheNextRunStartsWith(@TempDir home: File) {
        val file = File(home, "learned-mod-ids.json")

        JsonLearnedModIds(file).ids.learn("Modrinth", "yacl", setOf("yet_another_config_lib_v3"))

        Assertions.assertTrue(file.isFile, "learning something new is what triggers the write")
        Assertions.assertEquals(
            "yacl",
            JsonLearnedModIds(file).ids.refFor("yet_another_config_lib_v3", "Modrinth"),
            "a fresh instance over the same file knows what the first one proved"
        )
    }

    /** A first start has no file, and that is not an error to report or a reason to fail. */
    @Test
    fun aMissingFileIsAnEmptyMap(@TempDir home: File) {
        val store = JsonLearnedModIds(File(home, "learned-mod-ids.json"))

        Assertions.assertNull(store.ids.refFor("anything", "Modrinth"))
    }

    /**
     * **A corrupt document must not stop the daemon.** Everything in this file is re-derivable by grinding;
     * refusing to start over it would trade a cheap loss for a total one — the same call `JsonCursorStore`
     * makes about a crawl position.
     */
    @Test
    fun aCorruptFileIsTreatedAsEmptyAndStillUsable(@TempDir home: File) {
        val file = File(home, "learned-mod-ids.json").apply { writeText("{not json at all") }

        val store = JsonLearnedModIds(file)
        Assertions.assertNull(store.ids.refFor("anything", "Modrinth"))

        store.ids.learn("Modrinth", "yacl", setOf("yet_another_config_lib_v3"))
        Assertions.assertEquals(
            "yacl",
            JsonLearnedModIds(file).ids.refFor("yet_another_config_lib_v3", "Modrinth"),
            "and the next thing learned repairs the file"
        )
    }

    /** Two platforms keep their own vocabularies across the round trip, as they do in memory. */
    @Test
    fun eachPlatformKeepsItsOwnRefs(@TempDir home: File) {
        val file = File(home, "learned-mod-ids.json")
        val store = JsonLearnedModIds(file)

        store.ids.learn("Modrinth", "yacl", setOf("yet_another_config_lib_v3"))
        store.ids.learn("CurseForge", "667299", setOf("yet_another_config_lib_v3"))

        val reloaded = JsonLearnedModIds(file).ids
        Assertions.assertEquals("yacl", reloaded.refFor("yet_another_config_lib_v3", "Modrinth"))
        Assertions.assertEquals("667299", reloaded.refFor("yet_another_config_lib_v3", "CurseForge"))
    }

    /** No temp file is left behind, or a directory listing slowly fills with them. */
    @Test
    fun theWriteLeavesNoTemporaryBehind(@TempDir home: File) {
        val file = File(home, "learned-mod-ids.json")

        JsonLearnedModIds(file).ids.learn("Modrinth", "yacl", setOf("yet_another_config_lib_v3"))

        Assertions.assertEquals(
            listOf("learned-mod-ids.json"),
            home.listFiles()?.map { it.name }?.sorted(),
            "temp-then-move must leave only the document"
        )
    }

    /** Re-stating something already known writes nothing: the document did not change. */
    @Test
    fun reLearningTheSameThingDoesNotRewriteTheFile(@TempDir home: File) {
        val file = File(home, "learned-mod-ids.json")
        val store = JsonLearnedModIds(file)
        store.ids.learn("Modrinth", "yacl", setOf("yet_another_config_lib_v3"))
        val writtenAt = file.lastModified()
        file.setLastModified(writtenAt - 10_000)

        store.ids.learn("Modrinth", "yacl", setOf("yet_another_config_lib_v3"))

        Assertions.assertEquals(
            writtenAt - 10_000, file.lastModified(),
            "a staged dependency re-declares its id on every candidate that uses it; that is not news"
        )
    }
}
