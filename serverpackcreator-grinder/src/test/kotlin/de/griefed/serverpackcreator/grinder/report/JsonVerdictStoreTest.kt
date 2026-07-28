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

import de.griefed.serverpackcreator.clientside.Confidence
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
            record(grindVerdict("jei", "Forge", confidence = Confidence.HIGH))
            record(grindVerdict("sodium", "Fabric", confidence = Confidence.MEDIUM))
        }

        val reopened = JsonVerdictStore(file)
        Assertions.assertEquals(2, reopened.all().size)
        Assertions.assertTrue(reopened.hasVerdictFor("jei"))
        Assertions.assertEquals(Confidence.HIGH, reopened.all().first { it.slug == "jei" }.confidence)
    }

    @Test
    fun reVerifyReplacesAcrossAReopen(@TempDir dir: File) {
        val file = File(dir, "verdicts.json")
        JsonVerdictStore(file).record(grindVerdict("jei", "Forge", confidence = Confidence.LOW))
        JsonVerdictStore(file).record(grindVerdict("jei", "Forge", confidence = Confidence.HIGH))

        val reopened = JsonVerdictStore(file)
        Assertions.assertEquals(1, reopened.all().size)
        Assertions.assertEquals(Confidence.HIGH, reopened.all().single().confidence)
    }

    @Test
    fun aCorruptFileDegradesToEmpty(@TempDir dir: File) {
        val file = File(dir, "verdicts.json").apply { writeText("{ this is not valid json") }

        val store = JsonVerdictStore(file)
        Assertions.assertTrue(store.all().isEmpty())
        // ...and the store is still usable (recovers by overwriting on the next record).
        store.record(grindVerdict("jei", "Forge"))
        Assertions.assertEquals(1, JsonVerdictStore(file).all().size)
    }

    @Test
    fun newestVerificationSurvivesAReopen(@TempDir dir: File) {
        val file = File(dir, "verdicts.json")
        val when1 = Instant.parse("2026-02-01T00:00:00Z")
        val when2 = Instant.parse("2026-05-01T00:00:00Z")
        JsonVerdictStore(file).apply {
            record(grindVerdict("jei", "Forge", verifiedAt = when1))
            record(grindVerdict("jei", "NeoForge", verifiedAt = when2))
        }

        Assertions.assertEquals(when2, JsonVerdictStore(file).newestVerification("jei"))
    }

    @Test
    fun createsTheBackingFileAndItsParentOnFirstRecord(@TempDir dir: File) {
        val file = File(dir, "nested/sub/verdicts.json")
        Assertions.assertFalse(file.exists())

        JsonVerdictStore(file).record(grindVerdict("jei", "Forge"))

        Assertions.assertTrue(file.isFile, "backing file (and parents) must be created")
    }
}
