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
package de.griefed.serverpackcreator.grinder.source

import de.griefed.serverpackcreator.clientside.Confidence
import de.griefed.serverpackcreator.grinder.GrindCandidate
import de.griefed.serverpackcreator.grinder.ModPlatforms
import de.griefed.serverpackcreator.grinder.grindVerdict
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.Instant

/**
 * Pins the **immediate re-grind queue**: the jump-the-crawl lane for projects whose stored verdict is known
 * to be wrong.
 *
 * It exists because the catalog crawl and the re-verify TTL together answer "when does a project come round
 * again?" with *eventually, at TTL*, and that is the wrong answer when a defect is found in the engine
 * itself. Three landed on 2026-08-23 alone — a source jar becoming a list-entry, a crash re-check that never
 * left the crashing combination's neighbourhood, and two platform runs of one slug sharing a staging
 * directory. Each invalidated verdicts already published, and none of them was fixable by waiting.
 *
 * Persisted, because the daemon is fire-and-forget under systemd and an operator queueing work must not have
 * to keep a process alive for it to survive.
 */
internal class RequeueStoreTest {

    @TempDir
    lateinit var home: File

    private fun store() = JsonRequeueStore(File(home, "requeue.json"))

    private fun candidate(slug: String, platform: String = ModPlatforms.MODRINTH, projectId: String? = null) =
        GrindCandidate("https://modrinth.com/mod/$slug", slug, 0, platform, projectId)

    /** The whole contract: what goes in comes out once, and the queue is empty afterwards. */
    @Test
    fun drainsWhatWasQueuedExactlyOnce() {
        val store = store()
        store.add(listOf(candidate("creativecore"), candidate("jei")))

        Assertions.assertEquals(
            listOf("creativecore", "jei"),
            store.drain().map { it.slug }.sorted()
        )
        Assertions.assertTrue(store.drain().isEmpty(), "a drained queue is empty — work must not be repeated forever")
        Assertions.assertEquals(0, store.pending())
    }

    /**
     * The queueing tool and the daemon are **different processes** — an operator runs the one against a
     * service already running the other — so the queue only works if it is on disk.
     */
    @Test
    fun aQueuedEntrySurvivesTheProcessThatQueuedIt() {
        store().add(listOf(candidate("creativecore")))

        Assertions.assertEquals(listOf("creativecore"), store().drain().map { it.slug })
    }

    /** Queueing the same project twice is one re-grind, not two: the second would boot the same thing again. */
    @Test
    fun theSameProjectQueuedTwiceIsQueuedOnce() {
        val store = store()
        Assertions.assertEquals(1, store.add(listOf(candidate("creativecore", projectId = "OsZiaDHq"))))
        Assertions.assertEquals(0, store.add(listOf(candidate("creativecore", projectId = "OsZiaDHq"))))

        Assertions.assertEquals(1, store.drain().size)
    }

    /**
     * A slug is a mutable display name, so identity is the platform's own id where one is known — a project
     * queued under its old slug and again under its new one is still one re-grind.
     */
    @Test
    fun identityIsTheProjectIdNotTheSlug() {
        val store = store()
        store.add(listOf(candidate("creativecore", projectId = "OsZiaDHq")))
        store.add(listOf(candidate("creative-core", projectId = "OsZiaDHq")))

        Assertions.assertEquals(1, store.drain().size)
    }

    /** The same slug on the other platform is a different project, so it queues separately. */
    @Test
    fun theSameSlugOnAnotherPlatformQueuesSeparately() {
        val store = store()
        store.add(listOf(candidate("creativecore"), candidate("creativecore", platform = ModPlatforms.CURSEFORGE)))

        Assertions.assertEquals(2, store.drain().size)
    }

    /** A hand-edited or half-written queue must not stop the daemon starting; it starts empty and says so. */
    @Test
    fun anUnreadableQueueReadsAsEmptyRatherThanThrowing() {
        File(home, "requeue.json").writeText("{ this is not json")

        Assertions.assertTrue(store().drain().isEmpty())
    }

    /** Nothing queued is the normal case, and it must cost nothing and create nothing. */
    @Test
    fun anAbsentQueueIsEmpty() {
        Assertions.assertTrue(store().drain().isEmpty())
        Assertions.assertFalse(File(home, "requeue.json").exists(), "reading an empty queue must not create it")
    }

    /**
     * **The selector for "a defect invalidated everything we knew before the fix".** That is the recurring
     * shape — an engine bug is found, and every verdict produced before it landed is suspect — so it is
     * expressed once, over the store, rather than as a list somebody assembles by hand each time.
     *
     * One candidate per *project*, not per verdict row: a project holds one verdict per loader, and
     * re-grinding it re-verifies every loader anyway.
     */
    @Test
    fun verdictsOlderThanAGivenInstantBecomeOneCandidatePerProject() {
        val fix = Instant.parse("2026-08-23T12:00:00Z")
        val before = fix.minusSeconds(3600)
        val after = fix.plusSeconds(3600)
        val verdicts = listOf(
            grindVerdict("creativecore", "Fabric", verifiedAt = before),
            grindVerdict("creativecore", "NeoForge", verifiedAt = before),
            grindVerdict("creativecore", "Fabric", platform = ModPlatforms.CURSEFORGE, verifiedAt = before),
            grindVerdict("jei", "Forge", confidence = Confidence.LOW, verifiedAt = after)
        )

        val queued = RequeueSelection.verifiedBefore(verdicts, fix)

        Assertions.assertEquals(
            listOf(ModPlatforms.CURSEFORGE to "creativecore", ModPlatforms.MODRINTH to "creativecore"),
            queued.map { it.platform to it.slug }.sortedBy { it.first },
            "both platforms' creativecore is suspect; the verdict recorded after the fix is not"
        )
    }
}
