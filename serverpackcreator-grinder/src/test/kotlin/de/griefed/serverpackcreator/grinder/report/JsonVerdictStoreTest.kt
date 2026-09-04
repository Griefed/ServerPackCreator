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
package de.griefed.serverpackcreator.grinder.report

import de.griefed.serverpackcreator.clientside.Verdict
import de.griefed.serverpackcreator.grinder.ModPlatforms.MODRINTH
import de.griefed.serverpackcreator.grinder.grindVerdict
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.Instant

/**
 * Pins the file-backed store: verdicts survive a "restart" (a fresh instance over the same file reloads
 * them), re-verify replaces across reloads, and a corrupt file degrades to empty rather than throwing.
 */
internal class JsonVerdictStoreTest {

    @Test
    fun verdictsSurviveAReopen(@TempDir dir: File) {
        val file = File(dir, "verdicts.json")
        JsonVerdictStore(file).apply {
            record(grindVerdict("jei", "Forge", verdict = Verdict.CONFIRMED))
            record(grindVerdict("sodium", "Fabric", verdict = Verdict.INCONCLUSIVE))
        }

        val reopened = JsonVerdictStore(file)
        Assertions.assertEquals(2, reopened.all().size)
        Assertions.assertTrue(reopened.hasVerdictFor(MODRINTH, "jei"))
        Assertions.assertEquals(Verdict.CONFIRMED, reopened.all().first { it.slug == "jei" }.verdict)
    }

    @Test
    fun reVerifyReplacesAcrossAReopen(@TempDir dir: File) {
        val file = File(dir, "verdicts.json")
        JsonVerdictStore(file).record(grindVerdict("jei", "Forge", verdict = Verdict.CONFIRMED))
        JsonVerdictStore(file).record(grindVerdict("jei", "Forge", verdict = Verdict.CONFIRMED))

        val reopened = JsonVerdictStore(file)
        Assertions.assertEquals(1, reopened.all().size)
        Assertions.assertEquals(Verdict.CONFIRMED, reopened.all().single().verdict)
    }

    @Test
    fun aCorruptFileDegradesToEmpty(@TempDir dir: File) {
        val file = File(dir, "verdicts.json").apply { writeText("{ this is not valid json") }

        val store = JsonVerdictStore(file)
        Assertions.assertTrue(store.all().isEmpty())
        // ...and the store is still usable (recovers by overwriting on the next record).
        store.record(grindVerdict("jei", "Forge", verdict = Verdict.CONFIRMED))
        Assertions.assertEquals(1, JsonVerdictStore(file).all().size)
    }

    /**
     * A store written by a *newer* build carries fields this build has never heard of. Jackson's default
     * is to fail the whole read on one, which routes a perfectly good store down the corrupt path — and
     * from there the next [JsonVerdictStore.record] rewrites the file from an empty map. A downgrade must
     * cost nothing but the unknown fields.
     */
    @Test
    fun aStoreWrittenByANewerBuildStillLoads(@TempDir dir: File) {
        val file = File(dir, "verdicts.json").apply { writeText(verdictJson(extra = ""","aFieldFromTheFuture":"whatever"""")) }

        val store = JsonVerdictStore(file)

        Assertions.assertEquals(1, store.all().size, "an unknown field must not discard the verdict")
        Assertions.assertEquals(Verdict.CONFIRMED, store.all().single().verdict)
    }

    /**
     * The store degrading to empty is survivable; the store being *destroyed* is not. Persisting happens
     * on the very next [JsonVerdictStore.record], so an unreadable file has to be copied aside before
     * anything can overwrite it — otherwise one bad byte costs a multi-day grind.
     */
    @Test
    fun anUnreadableStoreIsPreservedRatherThanOverwritten(@TempDir dir: File) {
        val original = "{ this is not valid json"
        val file = File(dir, "verdicts.json").apply { writeText(original) }

        JsonVerdictStore(file).record(grindVerdict("jei", "Forge", verdict = Verdict.CONFIRMED))

        val preserved = dir.listFiles().orEmpty().filter { it.name.startsWith("verdicts.json.unreadable-") }
        Assertions.assertEquals(1, preserved.size, "the unreadable store must be kept aside, not silently destroyed")
        Assertions.assertEquals(original, preserved.single().readText())
    }

    /**
     * One row this build cannot make sense of — a verdict constant added later, say — must cost that
     * row and nothing else. Reading the document as a whole makes every row hostage to the worst one.
     */
    @Test
    fun oneUnreadableRowDoesNotDiscardTheOthers(@TempDir dir: File) {
        val file = File(dir, "verdicts.json").apply {
            writeText("[" + verdictBody("jei") + "," + verdictBody("sodium", verdict = "CERTAIN_FROM_THE_FUTURE") + "]")
        }

        val store = JsonVerdictStore(file)

        Assertions.assertEquals(1, store.all().size, "the readable row must survive its neighbour")
        Assertions.assertEquals("jei", store.all().single().slug)
        Assertions.assertTrue(
            dir.listFiles().orEmpty().any { it.name.startsWith("verdicts.json.unreadable-") },
            "a partially-read store must be preserved too — the next record() drops the skipped rows"
        )
    }

    /** One persisted verdict, as an older build would have written it, plus any [extra] trailing fields. */
    private fun verdictJson(extra: String = "") = "[" + verdictBody("jei", extra = extra) + "]"

    private fun verdictBody(slug: String, verdict: String = "CONFIRMED", extra: String = "") = """
        {"platform":"Modrinth","slug":"$slug","projectUrl":"https://modrinth.com/mod/$slug",
         "loader":"Forge","suggestedEntry":"$slug-","verdict":"$verdict","detail":"",
         "verifiedAt":"2026-02-01T00:00:00Z"$extra}
    """.trimIndent()

    @Test
    fun newestVerificationSurvivesAReopen(@TempDir dir: File) {
        val file = File(dir, "verdicts.json")
        val when1 = Instant.parse("2026-02-01T00:00:00Z")
        val when2 = Instant.parse("2026-05-01T00:00:00Z")
        JsonVerdictStore(file).apply {
            record(grindVerdict("jei", "Forge", verifiedAt = when1))
            record(grindVerdict("jei", "NeoForge", verifiedAt = when2))
        }

        Assertions.assertEquals(when2, JsonVerdictStore(file).newestVerification(MODRINTH, "jei"))
    }

    @Test
    fun createsTheBackingFileAndItsParentOnFirstRecord(@TempDir dir: File) {
        val file = File(dir, "nested/sub/verdicts.json")
        Assertions.assertFalse(file.exists())

        JsonVerdictStore(file).record(grindVerdict("jei", "Forge", verdict = Verdict.CONFIRMED))

        Assertions.assertTrue(file.isFile, "backing file (and parents) must be created")
    }
}
