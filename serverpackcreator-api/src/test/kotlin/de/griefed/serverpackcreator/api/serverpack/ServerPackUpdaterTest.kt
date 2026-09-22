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
package de.griefed.serverpackcreator.api.serverpack

import de.griefed.serverpackcreator.api.ApiWrapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * [ServerPackUpdater] on its own, without generating anything. The end-to-end guards in
 * `ServerPackUpdateSafetyTest` show the mechanism doing its job; these reach the answers a
 * generation cannot easily produce — a manifest written on the other operating system, a path
 * pointing outside the pack, a manifest that will not parse.
 */
internal class ServerPackUpdaterTest {
    private val api = ApiWrapper.api(File("build/resources/test/serverpackcreator.properties"))
    private val apiProperties = api.apiProperties
    private val updater = ServerPackUpdater(apiProperties, api.utilities.jsonUtilities.objectMapper)

    private var originalUpdating = false

    /** Remembers the update-toggle so a test may set it freely. */
    @BeforeEach
    fun snapshotToggle() {
        originalUpdating = apiProperties.isUpdatingServerPacksEnabled
    }

    /** Restores the update-toggle, so one test's setting cannot leak into the next. */
    @AfterEach
    fun restoreToggle() {
        apiProperties.isUpdatingServerPacksEnabled = originalUpdating
    }

    /** Writes [content] to `relative` beneath [root], creating parent directories, and returns the file. */
    private fun write(root: File, relative: String, content: String): File {
        val file = File(root, relative)
        file.parentFile.mkdirs()
        file.writeText(content)
        return file
    }

    /** Writes a manifest listing [entries] into [serverPack], as a previous run would have. */
    private fun manifest(serverPack: File, vararg entries: String) {
        serverPack.mkdirs()
        ServerPackManifest(entries.toList(), "1.16.5", "Forge", "36.1.2")
            .writeToFile(serverPack, api.utilities.jsonUtilities.objectMapper)
    }

    /** A protected entry protects itself and everything beneath it, but not a name that merely starts alike. */
    @Test
    fun protectionCoversAnEntryAndItsContents() {
        Assertions.assertTrue(updater.protects("world"), "The entry itself")
        Assertions.assertTrue(updater.protects("world/level.dat"), "A file inside it")
        Assertions.assertTrue(updater.protects("world/region/r.0.0.mca"), "However deep")
        Assertions.assertTrue(updater.protects("ops.json"), "A protected file")
        Assertions.assertFalse(updater.protects("worldly-mod.jar"), "A name that only shares a prefix")
        Assertions.assertFalse(updater.protects("mods/alpha.jar"), "An ordinary pack-file")
        Assertions.assertFalse(updater.protects(""), "Nothing at all")
    }

    /**
     * A manifest written on Windows carries backslashes and a pack may be updated on another machine
     * than it was generated on, so separators and case must not decide whether a world is protected.
     */
    @Test
    fun protectionIgnoresSeparatorStyleAndCase() {
        Assertions.assertTrue(updater.protects("world\\level.dat"), "Backslashes as written on Windows")
        Assertions.assertTrue(updater.protects("/world/level.dat"), "A leading separator")
        Assertions.assertTrue(updater.protects("World/Level.dat"), "A different spelling of the same path")
        Assertions.assertTrue(updater.protects("OPS.JSON"), "And of a protected file")
    }

    /** Protection can be widened, and a widened entry behaves exactly like a shipped one. */
    @Test
    fun anAddedPathIsProtectedLikeAShippedOne() {
        val original = apiProperties.updateProtectedPaths
        try {
            apiProperties.updateProtectedPaths = java.util.TreeSet(original + "plugins")
            Assertions.assertTrue(updater.protects("plugins/LuckPerms.jar"), "A path the user added")
            Assertions.assertTrue(updater.protects("world"), "without losing the shipped ones")
        } finally {
            apiProperties.updateProtectedPaths = original
        }
    }

    /** Updating is only possible where a previous run left the record of what it produced. */
    @Test
    fun aRunIsAnUpdateOnlyWithBothTheToggleAndAManifest(@TempDir tempDir: File) {
        val serverPack = File(tempDir, "pack")
        serverPack.mkdirs()

        apiProperties.isUpdatingServerPacksEnabled = true
        Assertions.assertFalse(updater.isUpdateRun(serverPack), "No manifest means nothing to update")

        manifest(serverPack, "mods/alpha.jar")
        Assertions.assertTrue(updater.isUpdateRun(serverPack), "Toggle and manifest together")

        apiProperties.isUpdatingServerPacksEnabled = false
        Assertions.assertFalse(updater.isUpdateRun(serverPack), "The toggle still decides")
    }

    /**
     * Protection guards what is already there; it does not forbid creating it. Without that a first
     * generation could never ship a world or a `server.properties` at all.
     */
    @Test
    fun protectionAppliesOnlyToAFileThatIsActuallyThere(@TempDir tempDir: File) {
        val serverPack = File(tempDir, "pack")
        manifest(serverPack, "mods/alpha.jar")
        apiProperties.isUpdatingServerPacksEnabled = true

        Assertions.assertFalse(
            updater.preserves(serverPack, "server.properties"),
            "A protected file that does not exist yet must still be written"
        )
        write(serverPack, "server.properties", "motd=mine")
        Assertions.assertTrue(
            updater.preserves(serverPack, "server.properties"),
            "and must be left alone once it does"
        )
    }

    /** Relative paths are recorded `/`-separated, whatever the platform spells them as. */
    @Test
    fun relativePathsAreRecordedWithForwardSlashes(@TempDir tempDir: File) {
        val serverPack = File(tempDir, "pack")
        val nested = write(serverPack, "config/alpha/alpha.cfg", "alpha=1")

        Assertions.assertEquals("config/alpha/alpha.cfg", updater.relativize(serverPack, nested))
    }

    /**
     * A destination that resolved outside the pack is a mis-resolved destination. Recording it would
     * put a `../`-path into the manifest, which the next update would then happily delete from
     * somewhere else entirely.
     */
    @Test
    fun aPathOutsideTheServerPackIsNotRecorded(@TempDir tempDir: File) {
        val serverPack = File(tempDir, "pack")
        serverPack.mkdirs()
        val elsewhere = write(tempDir, "somewhere-else/important.txt", "not ours")

        Assertions.assertNull(updater.relativize(serverPack, elsewhere), "Outside the pack, so not recorded")
        Assertions.assertNull(updater.relativize(serverPack, serverPack), "And neither is the pack itself")
    }

    /** The prune removes what the previous run produced and this one did not, and nothing else. */
    @Test
    fun theOldManifestDecidesWhatIsRemoved(@TempDir tempDir: File) {
        val serverPack = File(tempDir, "pack")
        manifest(serverPack, "mods/alpha.jar", "mods/dropped.jar", "world/level.dat", "ops.json")
        val kept = write(serverPack, "mods/alpha.jar", "alpha")
        val dropped = write(serverPack, "mods/dropped.jar", "dropped")
        val world = write(serverPack, "world/level.dat", "a world")
        val ops = write(serverPack, "ops.json", "[]")
        val theirs = write(serverPack, "notes.txt", "a file nobody asked us about")

        updater.prune(serverPack, setOf("mods/alpha.jar"))

        Assertions.assertTrue(kept.isFile, "A file this run produced again stays")
        Assertions.assertFalse(dropped.exists(), "A file it did not produce goes")
        Assertions.assertTrue(world.isFile, "A protected file stays even though it is in the manifest")
        Assertions.assertTrue(ops.isFile, "as does a protected file of the pack's own")
        Assertions.assertTrue(theirs.isFile, "and anything the manifest never mentioned is not ours to delete")
    }

    /** A directory the prune empties goes with its contents; one still holding something stays. */
    @Test
    fun directoriesGoOnlyOnceTheyAreEmpty(@TempDir tempDir: File) {
        val serverPack = File(tempDir, "pack")
        manifest(
            serverPack,
            "config", "config/keep.cfg",
            "config/gone", "config/gone/nested", "config/gone/nested/deep.cfg", "config/gone/shallow.cfg"
        )
        write(serverPack, "config/keep.cfg", "keep")
        write(serverPack, "config/gone/nested/deep.cfg", "deep")
        write(serverPack, "config/gone/shallow.cfg", "shallow")

        updater.prune(serverPack, setOf("config", "config/keep.cfg"))

        Assertions.assertTrue(File(serverPack, "config/keep.cfg").isFile, "The kept file stays")
        Assertions.assertTrue(File(serverPack, "config").isDirectory, "so its directory stays")
        Assertions.assertFalse(File(serverPack, "config/gone").exists(), "The emptied directory goes")
        Assertions.assertFalse(
            File(serverPack, "config/gone/nested").exists(),
            "including the nested one, which is why they are removed deepest first"
        )
    }

    /**
     * A pack generated on Windows carries a manifest whose entries are `mods\alpha.jar`. Updating it
     * on Linux must still resolve them, or nothing the previous run produced can ever be pruned and
     * the pack silently stops converging on its modpack.
     */
    @Test
    fun aManifestWrittenOnTheOtherPlatformStillPrunes(@TempDir tempDir: File) {
        val serverPack = File(tempDir, "pack")
        manifest(serverPack, "mods\\alpha.jar", "mods\\dropped.jar", "config\\gone", "config\\gone\\some.cfg")
        val kept = write(serverPack, "mods/alpha.jar", "alpha")
        val dropped = write(serverPack, "mods/dropped.jar", "dropped")
        write(serverPack, "config/gone/some.cfg", "stale")

        // What this run produced, spelled the way this platform spells it.
        updater.prune(serverPack, setOf("mods/alpha.jar"))

        Assertions.assertTrue(kept.isFile, "A backslash entry this run produced again must be recognised and kept")
        Assertions.assertFalse(dropped.exists(), "and one it did not must be pruned")
        Assertions.assertFalse(File(serverPack, "config/gone").exists(), "including the directory it emptied")
    }

    /**
     * An entry that is not relative to the pack must not reach outside it. `File(pack, "/etc/x")`
     * resolves to `pack/etc/x` on Unix rather than to `/etc/x`, so this is about the pack staying the
     * only thing a prune can touch — whatever a manifest claims.
     */
    @Test
    fun anAbsolutePathInAnOldManifestCannotReachOutsideThePack(@TempDir tempDir: File) {
        val serverPack = File(tempDir, "pack")
        val outsider = write(tempDir, "not-ours/precious.txt", "belongs to somebody else")
        manifest(serverPack, outsider.absolutePath, "mods/dropped.jar")
        val dropped = write(serverPack, "mods/dropped.jar", "dropped")

        updater.prune(serverPack, setOf("mods/alpha.jar"))

        Assertions.assertTrue(outsider.isFile, "A prune must never delete anything outside the server pack")
        Assertions.assertFalse(dropped.exists(), "while still pruning what is inside it")
    }

    /**
     * Protection is by path segment, not by string prefix. `world` must not protect `worlds/` or
     * `world_backup/`, or a modpack with a directory named after the save would become unprunable.
     */
    @Test
    fun protectionStopsAtTheSegmentBoundaryWhenPruning(@TempDir tempDir: File) {
        val serverPack = File(tempDir, "pack")
        manifest(serverPack, "world/level.dat", "worlds/old.dat", "world_backup/old.dat", "worldly.jar")
        val protectedWorld = write(serverPack, "world/level.dat", "the operator's")
        val plural = write(serverPack, "worlds/old.dat", "not the world")
        val backup = write(serverPack, "world_backup/old.dat", "not the world either")
        val jar = write(serverPack, "worldly.jar", "a mod")

        updater.prune(serverPack, setOf("mods/alpha.jar"))

        Assertions.assertTrue(protectedWorld.isFile, "world/ is protected")
        Assertions.assertFalse(plural.exists(), "worlds/ is not")
        Assertions.assertFalse(backup.exists(), "nor is world_backup/")
        Assertions.assertFalse(jar.exists(), "nor is a file whose name merely starts the same way")
    }

    /**
     * A manifest that cannot be *read* is the same answer as one that cannot be parsed: no manifest,
     * and therefore no prune. Deleting on the strength of a file we failed to open is the one outcome
     * that is never acceptable.
     */
    @Test
    fun anUnreadableManifestPrunesNothingEither(@TempDir tempDir: File) {
        val serverPack = File(tempDir, "pack")
        manifest(serverPack, "mods/dropped.jar")
        val existing = write(serverPack, "mods/dropped.jar", "dropped")
        val manifestFile = ServerPackManifest.inside(serverPack)

        Assumptions.assumeTrue(manifestFile.setReadable(false), "This filesystem must honour setReadable(false)")
        Assumptions.assumeFalse(manifestFile.canRead(), "and the process must not be able to read it anyway (not root)")
        try {
            Assertions.assertNull(updater.readManifest(serverPack), "An unreadable manifest reads as none")
            updater.prune(serverPack, setOf("mods/alpha.jar"))
            Assertions.assertTrue(existing.isFile, "and therefore nothing is pruned")
        } finally {
            manifestFile.setReadable(true)
        }
    }

    /** Pruning against nothing would empty the pack, so it refuses. */
    @Test
    fun aPruneWithNothingProducedRemovesNothing(@TempDir tempDir: File) {
        val serverPack = File(tempDir, "pack")
        manifest(serverPack, "mods/alpha.jar")
        val existing = write(serverPack, "mods/alpha.jar", "alpha")

        updater.prune(serverPack, emptySet())

        Assertions.assertTrue(existing.isFile, "A prune with nothing to compare against must delete nothing")
    }

    /**
     * A manifest that will not parse is reported and then treated as no manifest. Guessing would
     * mean deleting files on the strength of a file we just failed to understand.
     */
    @Test
    fun anUnreadableManifestPrunesNothing(@TempDir tempDir: File) {
        val serverPack = File(tempDir, "pack")
        serverPack.mkdirs()
        write(serverPack, ServerPackManifest.FILE_NAME, "{ this is not json")
        val existing = write(serverPack, "mods/alpha.jar", "alpha")

        Assertions.assertNull(updater.readManifest(serverPack), "An unreadable manifest reads as none")
        updater.prune(serverPack, setOf("mods/beta.jar"))

        Assertions.assertTrue(existing.isFile, "and therefore nothing is pruned")
    }
}
