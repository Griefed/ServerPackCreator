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

import de.griefed.serverpackcreator.grinder.ModPlatforms
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Pins the durable home for the console of a boot that **crashed** — the one artefact a HIGH verdict
 * cannot be re-derived without.
 *
 * The staging console is not that home. `BootWorkspaceReaper` keeps one `boot.log` per attempt
 * directory, but staging *wipes and re-creates* that directory, so the next re-grind of the same
 * `(platform, slug, loader)` destroys the evidence for the verdict that is still published. A crash is
 * the only outcome that reaches HIGH, and the usual cause — a server reaching for a client-only class —
 * is legible only from the console, so it has to survive the sweep that produced it.
 */
internal class CrashLogStoreTest {

    @TempDir
    lateinit var directory: File

    private fun store() = CrashLogStore(directory)

    /**
     * A staged console, as the boot verifier leaves it — written **outside** the store, because that is where
     * a real one lives: under `<work>/boot/<attempt>/boot.log`, in the staging this store exists to rescue it
     * from.
     */
    private fun console(text: String): File =
        File(directory.parentFile, "staged-boot-${text.hashCode()}.log").apply { writeText(text) }

    /** The round trip a report link makes: keep it, get a name, read the same bytes back by that name. */
    @Test
    fun keepsACrashConsoleAndReadsItBackByName() {
        val crash = console("java.lang.NoClassDefFoundError: net/minecraft/client/Minecraft\n\tat com.example…")

        val name = store().keep(ModPlatforms.MODRINTH, "creativecore", "Fabric", crash)

        Assertions.assertNotNull(name)
        Assertions.assertEquals(crash.readText(), store().read(name!!))
        Assertions.assertEquals(name, store().nameFor(ModPlatforms.MODRINTH, "creativecore", "Fabric"))
    }

    /** The same slug on the other platform is a different project, so it must not overwrite the first. */
    @Test
    fun theSameSlugOnAnotherPlatformIsADifferentLog() {
        val modrinth = store().keep(ModPlatforms.MODRINTH, "creativecore", "Fabric", console("modrinth crash"))
        val curseForge = store().keep(ModPlatforms.CURSEFORGE, "creativecore", "Fabric", console("curseforge crash"))

        Assertions.assertNotEquals(modrinth, curseForge)
        Assertions.assertEquals("modrinth crash", store().read(modrinth!!))
        Assertions.assertEquals("curseforge crash", store().read(curseForge!!))
    }

    /** Nothing kept means no link to offer, which is what keeps the report from pointing at a 404. */
    @Test
    fun anUnknownTupleHasNoName() {
        Assertions.assertNull(store().nameFor(ModPlatforms.MODRINTH, "never-ground", "Forge"))
    }

    /**
     * A re-grind of the same project replaces its log rather than adding one, so the store is bounded by the
     * number of distinct crashing `(platform, slug, loader)` tuples instead of by how long the daemon runs.
     */
    @Test
    fun reGrindingATupleReplacesItsLogRatherThanAccumulating() {
        store().keep(ModPlatforms.MODRINTH, "creativecore", "Fabric", console("first pass"))
        store().keep(ModPlatforms.MODRINTH, "creativecore", "Fabric", console("second pass"))

        Assertions.assertEquals(listOf("Modrinth-creativecore-Fabric.log"), store().list())
        Assertions.assertEquals("second pass", store().read("Modrinth-creativecore-Fabric.log"))
    }

    /**
     * **This store is served over HTTP by name, so a name is untrusted input.** A traversal must read
     * nothing at all rather than any file the daemon's user can open — the grinder's report binds loopback
     * by default but is explicitly documented as something an operator may reverse-proxy.
     */
    @Test
    fun aNameThatEscapesTheStoreReadsNothing() {
        val secret = File(directory.parentFile, "secret.txt").apply { writeText("not yours") }
        store().keep(ModPlatforms.MODRINTH, "creativecore", "Fabric", console("real log"))

        listOf("../${secret.name}", "..%2Fsecret.txt", "sub/dir.log", "/etc/passwd", "", "..").forEach { name ->
            Assertions.assertNull(store().read(name), "'$name' must not resolve to anything")
        }
        Assertions.assertEquals("real log", store().read("Modrinth-creativecore-Fabric.log"), "and the real one still reads")
    }

    /**
     * A mod can spew megabytes before it dies. The crash is at the *end*, so an oversized console is kept
     * from its tail with the truncation said out loud — a silently shortened log is a log nobody can trust.
     */
    @Test
    fun anOversizedConsoleIsKeptFromItsTailAndSaysSo() {
        val tail = "java.lang.NoClassDefFoundError: net/minecraft/client/Minecraft"
        val huge = console("x".repeat(CrashLogStore.MAX_BYTES + 4096) + "\n" + tail)

        val name = store().keep(ModPlatforms.MODRINTH, "spewy", "Forge", huge)!!
        val kept = store().read(name)!!

        Assertions.assertTrue(kept.endsWith(tail), "the crash is at the end — that is the half worth keeping")
        Assertions.assertTrue(kept.contains("truncated"), "a shortened log must say it was shortened")
        Assertions.assertTrue(kept.length <= CrashLogStore.MAX_BYTES + 512, "kept ${kept.length} bytes")
    }

    /** Reclamation must never fail a grind, so an unwritable store degrades to "no log" rather than throwing. */
    @Test
    fun anUnwritableStoreYieldsNoNameInsteadOfThrowing() {
        val asFile = File(directory, "not-a-directory").apply { writeText("in the way") }

        Assertions.assertNull(CrashLogStore(asFile).keep(ModPlatforms.MODRINTH, "jei", "Forge", console("x")))
    }
}
