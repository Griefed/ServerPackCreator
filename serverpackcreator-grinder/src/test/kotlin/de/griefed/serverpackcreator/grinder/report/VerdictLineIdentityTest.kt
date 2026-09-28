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
import de.griefed.serverpackcreator.grinder.grindVerdict
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Pins that a verdict's dedup identity is its **Minecraft version-line**, not its modloader.
 *
 * A project is ground once per line under whichever loader that line publishes for, so one loader routinely
 * holds several of a project's rows — `CurseForge/aether` is NeoForge on both 1.21 and 1.20. Keyed on the
 * loader, the second of those *overwrites* the first, and the store silently reports one era's evidence as
 * the whole project's.
 *
 * The legacy half matters as much: the deployed store holds tens of thousands of loader-keyed rows, and they
 * have to age out per project rather than all at once or on a schedule nobody controls.
 *
 * @author Griefed
 */
internal class VerdictLineIdentityTest {

    /** **The collision.** Same project, same loader, two Minecraft lines — two rows, not one. */
    @Test
    fun twoMinecraftLinesOfOneLoaderAreTwoRows() {
        val store = InMemoryVerdictStore()

        store.record(grindVerdict("aether", "NeoForge", minecraftLine = "1.21", minecraftVersion = "1.21.1"))
        store.record(grindVerdict("aether", "NeoForge", minecraftLine = "1.20", minecraftVersion = "1.20.1"))

        Assertions.assertEquals(
            listOf("1.20", "1.21"), store.all().mapNotNull { it.minecraftLine }.sorted(),
            "keyed on the loader, the 1.20 row would have overwritten the 1.21 row"
        )
    }

    /** Re-grinding one line replaces that line's row and leaves the project's others alone. */
    @Test
    fun reGrindingOneLineReplacesOnlyThatLine() {
        val store = InMemoryVerdictStore()
        store.record(grindVerdict("aether", "NeoForge", minecraftLine = "1.21", detail = "first"))
        store.record(grindVerdict("aether", "Forge", minecraftLine = "1.12", detail = "untouched"))

        store.record(grindVerdict("aether", "NeoForge", minecraftLine = "1.21", detail = "second"))

        Assertions.assertEquals(
            mapOf("1.21" to "second", "1.12" to "untouched"),
            store.all().associate { it.minecraftLine to it.detail }
        )
    }

    /**
     * A project's loader may change between grinds — a line that had only Forge gains a NeoForge build, and
     * the priority order then picks it. The row is the *line*, so the new grind replaces the old one instead
     * of leaving a Forge row nothing will ever come back to.
     */
    @Test
    fun aLineThatChangesLoaderKeepsOneRow() {
        val store = InMemoryVerdictStore()
        store.record(grindVerdict("aether", "Forge", minecraftLine = "1.20", detail = "before"))

        store.record(grindVerdict("aether", "NeoForge", minecraftLine = "1.20", detail = "after"))

        Assertions.assertEquals(
            listOf("NeoForge" to "after"), store.all().map { it.loader to it.detail },
            "keyed on the loader this would strand the Forge row for ever"
        )
    }

    /**
     * **The migration.** A store written before the axis moved holds one row per loader. Recording the first
     * line-keyed row for a project drops that project's loader-keyed rows — per project, as each is
     * re-ground, so nothing is thrown away before it has been replaced.
     */
    @Test
    fun aLineRowSupersedesThatProjectsLegacyLoaderRows() {
        val store = InMemoryVerdictStore()
        store.record(grindVerdict("aether", "Fabric", minecraftLine = null, minecraftVersion = null))
        store.record(grindVerdict("aether", "Forge", minecraftLine = null, minecraftVersion = null))
        store.record(grindVerdict("jei", "Forge", minecraftLine = null, minecraftVersion = null))

        store.record(grindVerdict("aether", "NeoForge", minecraftLine = "1.21", detail = "re-ground"))

        Assertions.assertEquals(
            listOf("aether" to "1.21", "jei" to null),
            store.all().map { it.slug to it.minecraftLine }.sortedBy { it.first },
            "this project's legacy rows go; another project's are untouched until it is re-ground"
        )
    }

    /** Legacy rows of a project nothing re-ground stay, evidence and all, until the TTL brings it round. */
    @Test
    fun anUntouchedProjectKeepsItsLegacyRows() {
        val store = InMemoryVerdictStore()
        store.record(grindVerdict("jei", "Forge", minecraftLine = null, verdict = Verdict.CONFIRMED))
        store.record(grindVerdict("jei", "Fabric", minecraftLine = null, verdict = Verdict.CONFIRMED))

        Assertions.assertEquals(2, store.all().size)
    }

    /** The same contract in the persistent store, across a reopen — the two must not drift. */
    @Test
    fun theJsonStoreKeysOnTheLineToo(@TempDir dir: File) {
        val file = File(dir, "verdicts.json")
        JsonVerdictStore(file).apply {
            record(grindVerdict("aether", "NeoForge", minecraftLine = "1.21"))
            record(grindVerdict("aether", "NeoForge", minecraftLine = "1.20"))
            flush()
        }

        Assertions.assertEquals(
            listOf("1.20", "1.21"),
            JsonVerdictStore(file).all().mapNotNull { it.minecraftLine }.sorted()
        )
    }

    /** And supersedes a legacy row across a reopen, which is where the deployed store actually is. */
    @Test
    fun theJsonStoreSupersedesLegacyRowsAcrossAReopen(@TempDir dir: File) {
        val file = File(dir, "verdicts.json")
        JsonVerdictStore(file).apply {
            record(grindVerdict("aether", "Fabric", minecraftLine = null))
            flush()
        }

        JsonVerdictStore(file).apply {
            record(grindVerdict("aether", "NeoForge", minecraftLine = "1.21"))
            flush()
        }

        Assertions.assertEquals(
            listOf("1.21"), JsonVerdictStore(file).all().map { it.minecraftLine ?: "legacy" }
        )
    }
}
