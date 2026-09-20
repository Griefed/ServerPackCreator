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

import de.griefed.serverpackcreator.api.config.InclusionSpecification
import de.griefed.serverpackcreator.api.config.PackConfig
import de.griefed.serverpackcreator.api.serverpack.ServerPackManifest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.*
import java.util.zip.ZipFile

/**
 * What an update must guarantee for a server pack somebody is actually running a server out of.
 *
 * The companion to [ServerPackUpdateTest], which pins the mechanism as it behaves; this class states
 * the promises the feature has to keep before it can stop calling itself experimental. A server run
 * straight out of a generated pack writes a world, an `ops.json` and a tuned `server.properties`
 * into that same directory, and regenerating the pack must not take any of them — nor hand them to
 * anyone the resulting ZIP-archive is shared with.
 */
internal class ServerPackUpdateSafetyTest {
    private val api = ApiWrapper.api(File("build/resources/test/serverpackcreator.properties"))
    private val apiProperties = api.apiProperties
    private val serverPackHandler = api.serverPackHandler

    private var originalOverwrite = false
    private var originalUpdating = false

    /** Remembers the global toggles so a test may set them freely. */
    @BeforeEach
    fun snapshotToggles() {
        originalOverwrite = apiProperties.isServerPacksOverwriteEnabled
        originalUpdating = apiProperties.isUpdatingServerPacksEnabled
    }

    /** Restores the global toggles, so one test's settings cannot leak into the next. */
    @AfterEach
    fun restoreToggles() {
        apiProperties.isServerPacksOverwriteEnabled = originalOverwrite
        apiProperties.isUpdatingServerPacksEnabled = originalUpdating
    }

    /** Writes [content] to `relative` beneath [root], creating parent directories, and returns the file. */
    private fun write(root: File, relative: String, content: String): File {
        val file = File(root, relative)
        file.parentFile.mkdirs()
        file.writeText(content)
        return file
    }

    /** Lays out a minimal modpack — one mod, one config file, one nested config directory. */
    private fun modpack(tempDir: File): File {
        val modpackDir = File(tempDir, "modpack")
        write(modpackDir, "mods/alpha.jar", "alpha v1")
        write(modpackDir, "config/settings.cfg", "setting=1")
        write(modpackDir, "config/alpha/alpha.cfg", "alpha=1")
        return modpackDir
    }

    /** A [PackConfig] copying [sources] out of [modpackDir] into [destination], with no extras. */
    private fun packConfig(
        modpackDir: File,
        destination: File,
        vararg sources: String = arrayOf("mods", "config")
    ): PackConfig {
        val packConfig = PackConfig()
        packConfig.modpackDir = modpackDir.absolutePath
        packConfig.minecraftVersion = "1.16.5"
        packConfig.modloader = "Forge"
        packConfig.modloaderVersion = "36.1.2"
        packConfig.isServerIconInclusionDesired = false
        packConfig.isServerPropertiesInclusionDesired = false
        packConfig.isZipCreationDesired = false
        packConfig.customDestination = Optional.of(destination)
        for (source in sources) {
            packConfig.inclusions.add(InclusionSpecification(source))
        }
        return packConfig
    }

    /** Reads the `manifest.json` ServerPackCreator wrote into [destination]. */
    private fun manifestOf(destination: File): ServerPackManifest =
        api.utilities.jsonUtilities.objectMapper
            .readValue(File(destination, "manifest.json"), ServerPackManifest::class.java)

    /** Puts the traces of a server having been run out of [destination] into it. */
    private fun asIfAServerHadRun(destination: File) {
        write(destination, "world/level.dat", "a world worth keeping")
        write(destination, "world/region/r.0.0.mca", "chunks")
        write(destination, "ops.json", """[{"name":"Griefed"}]""")
        write(destination, "eula.txt", "eula=true")
    }

    /**
     * Updating must win over overwriting. `isServerPacksOverwriteEnabled` defaults to `true`, so a
     * user who ticks "update" and nothing else gets both toggles on — and today the cleanup wins,
     * empties the destination, and the manifest the update needs is gone before it is read.
     */
    @Test
    fun updatingWinsOverOverwriting(@TempDir tempDir: File) {
        val modpackDir = modpack(tempDir)
        val destination = File(tempDir, "pack")

        apiProperties.isServerPacksOverwriteEnabled = true
        apiProperties.isUpdatingServerPacksEnabled = false
        serverPackHandler.run(packConfig(modpackDir, destination))

        asIfAServerHadRun(destination)
        apiProperties.isUpdatingServerPacksEnabled = true
        serverPackHandler.run(packConfig(modpackDir, destination))

        val level = File(destination, "world/level.dat")
        Assertions.assertTrue(
            level.isFile,
            "An update must not empty the destination, whatever the overwrite-toggle says"
        )
        Assertions.assertEquals("a world worth keeping", level.readText(), "and must not touch the world it finds")
        Assertions.assertTrue(File(destination, "ops.json").isFile, "ops.json must survive")
        Assertions.assertTrue(File(destination, "mods/alpha.jar").isFile, "and the pack must still be generated")
    }

    /**
     * The ZIP-archive is what a pack-author uploads for other people to download. Anything the
     * operator's own server wrote into the pack — their world, their ops-list — must stay out of it.
     */
    @Test
    fun theArchiveOfAnUpdateLeavesOutWhatTheServerWrote(@TempDir tempDir: File) {
        val modpackDir = modpack(tempDir)
        val destination = File(tempDir, "pack")

        apiProperties.isServerPacksOverwriteEnabled = false
        apiProperties.isUpdatingServerPacksEnabled = true
        serverPackHandler.run(packConfig(modpackDir, destination))

        asIfAServerHadRun(destination)
        val zipping = packConfig(modpackDir, destination)
        zipping.isZipCreationDesired = true
        zipping.isServerPropertiesInclusionDesired = true
        serverPackHandler.run(zipping)

        val archive = File(destination.absolutePath + "_server_pack.zip")
        Assertions.assertTrue(archive.isFile, "An archive must be produced")
        val entries = ZipFile(archive).use { zip -> zip.entries().toList().map { it.name } }

        Assertions.assertTrue(
            entries.none { it.startsWith("world/") || it == "world" },
            "The operator's world must not be shipped in the archive, got $entries"
        )
        Assertions.assertTrue(entries.none { it == "ops.json" }, "Nor their ops-list")
        Assertions.assertTrue(entries.any { it == "mods/alpha.jar" }, "The pack itself must still be archived")
        // Protected does not mean absent: these two are part of every server pack, and a downloaded
        // archive without them is one whose start scripts have nothing to read.
        Assertions.assertTrue(entries.any { it == "variables.txt" }, "variables.txt must be archived, got $entries")
        Assertions.assertTrue(entries.any { it == "server.properties" }, "and so must server.properties")
        Assertions.assertTrue(entries.any { it.startsWith("start.") }, "and the start scripts")
    }

    /**
     * `server.properties` is where the operator sets their MOTD, difficulty, port and — the one that
     * decides which world the server loads at all — `level-name`. An update must leave the copy in a
     * pack it is updating alone.
     */
    @Test
    fun anUpdateKeepsTheOperatorsServerProperties(@TempDir tempDir: File) {
        val modpackDir = modpack(tempDir)
        val destination = File(tempDir, "pack")

        apiProperties.isServerPacksOverwriteEnabled = false
        apiProperties.isUpdatingServerPacksEnabled = true
        val withProperties = packConfig(modpackDir, destination)
        withProperties.isServerPropertiesInclusionDesired = true
        serverPackHandler.run(withProperties)

        val properties = write(destination, "server.properties", "motd=a tuned server\nlevel-name=survival\n")
        serverPackHandler.run(withProperties)

        Assertions.assertEquals(
            "motd=a tuned server\nlevel-name=survival\n",
            properties.readText(),
            "An update must not revert the operator's server.properties"
        )
    }

    /**
     * `variables.txt` carries the operator's Java path and memory settings. Same promise as
     * `server.properties`: written on a first generation, left alone on an update.
     */
    @Test
    fun anUpdateKeepsTheOperatorsVariables(@TempDir tempDir: File) {
        val modpackDir = modpack(tempDir)
        val destination = File(tempDir, "pack")

        apiProperties.isServerPacksOverwriteEnabled = false
        apiProperties.isUpdatingServerPacksEnabled = true
        serverPackHandler.run(packConfig(modpackDir, destination))
        Assertions.assertTrue(File(destination, "variables.txt").isFile, "A first run must write variables.txt")

        val variables = write(destination, "variables.txt", "JAVA=/opt/jdk/bin/java\nJAVA_ARGS=-Xmx16G\n")
        serverPackHandler.run(packConfig(modpackDir, destination))

        Assertions.assertEquals(
            "JAVA=/opt/jdk/bin/java\nJAVA_ARGS=-Xmx16G\n",
            variables.readText(),
            "An update must not revert the operator's variables.txt"
        )
    }

    /**
     * The manifest is the record of what ServerPackCreator produced, and the prune deletes exactly
     * what it lists. Everything provisioned alongside the modpack-files therefore belongs in it —
     * otherwise the record is incomplete and those files can never be cleaned up.
     */
    @Test
    fun theManifestAlsoRecordsWhatWasProvisionedAlongsideTheModpack(@TempDir tempDir: File) {
        val modpackDir = modpack(tempDir)
        val destination = File(tempDir, "pack")

        apiProperties.isServerPacksOverwriteEnabled = true
        apiProperties.isUpdatingServerPacksEnabled = false
        val complete = packConfig(modpackDir, destination)
        complete.isServerIconInclusionDesired = true
        complete.isServerPropertiesInclusionDesired = true
        serverPackHandler.run(complete)

        val listed = manifestOf(destination).files.map { it.replace('\\', '/') }
        Assertions.assertTrue(listed.contains("server.properties"), "server.properties belongs in the manifest, got $listed")
        Assertions.assertTrue(listed.contains("server-icon.png"), "so does the server-icon")
        Assertions.assertTrue(listed.contains("variables.txt"), "so does variables.txt")
        Assertions.assertTrue(listed.contains("HOW-TO-RUN.md"), "so does HOW-TO-RUN.md")
        Assertions.assertTrue(listed.any { it.startsWith("start.") }, "and so do the start scripts")
    }

    /**
     * The consequence of a complete manifest: untick "include the server-icon" and the icon a
     * previous run put there is gone after the next update, instead of lingering forever.
     */
    @Test
    fun anIconNoLongerWantedIsRemovedByTheNextUpdate(@TempDir tempDir: File) {
        val modpackDir = modpack(tempDir)
        val destination = File(tempDir, "pack")

        apiProperties.isServerPacksOverwriteEnabled = false
        apiProperties.isUpdatingServerPacksEnabled = true
        val withIcon = packConfig(modpackDir, destination)
        withIcon.isServerIconInclusionDesired = true
        serverPackHandler.run(withIcon)
        Assertions.assertTrue(File(destination, "server-icon.png").isFile, "A first run must produce the icon")

        serverPackHandler.run(packConfig(modpackDir, destination))

        Assertions.assertFalse(
            File(destination, "server-icon.png").exists(),
            "An icon that is no longer wanted must be removed by the update that stops producing it"
        )
    }

    /**
     * A directory the update empties is a directory the modpack no longer has. Leaving it behind
     * means the pack never converges on what the modpack actually contains.
     */
    @Test
    fun aDirectoryEmptiedByAnUpdateIsRemoved(@TempDir tempDir: File) {
        val modpackDir = modpack(tempDir)
        val destination = File(tempDir, "pack")

        apiProperties.isServerPacksOverwriteEnabled = false
        apiProperties.isUpdatingServerPacksEnabled = true
        serverPackHandler.run(packConfig(modpackDir, destination))
        Assertions.assertTrue(File(destination, "config/alpha").isDirectory, "First run must produce the directory")

        File(modpackDir, "config/alpha").deleteRecursively()
        serverPackHandler.run(packConfig(modpackDir, destination))

        Assertions.assertFalse(
            File(destination, "config/alpha").exists(),
            "A directory the update emptied must not be left behind"
        )
    }

    /**
     * A world that came *from* the modpack is in the manifest, so the prune would delete it and the
     * copy would put the pristine one back — silently throwing away everything played since. The
     * pack directory is a live server's directory, and a world in it is the operator's, wherever it
     * originally came from.
     */
    @Test
    fun aWorldShippedInTheModpackIsNotRevertedByAnUpdate(@TempDir tempDir: File) {
        val modpackDir = modpack(tempDir)
        write(modpackDir, "saves/adventure/level.dat", "the pristine world")
        val destination = File(tempDir, "pack")
        val shippingTheWorld = packConfig(modpackDir, destination)
        shippingTheWorld.inclusions.add(InclusionSpecification("saves/adventure", "world"))

        apiProperties.isServerPacksOverwriteEnabled = false
        apiProperties.isUpdatingServerPacksEnabled = true
        serverPackHandler.run(shippingTheWorld)
        Assertions.assertTrue(File(destination, "world/level.dat").isFile, "The first run must ship the world")

        write(destination, "world/level.dat", "a hundred hours later")
        serverPackHandler.run(shippingTheWorld)

        Assertions.assertEquals(
            "a hundred hours later",
            File(destination, "world/level.dat").readText(),
            "A world in the pack belongs to whoever has been playing it, not to the modpack"
        )
    }

    /**
     * The local `variables.txt` carries the operator's configured Java path; the archived one
     * deliberately does not. A *first* generation must still produce the local one even with
     * updating enabled -- there is no previous pack to preserve anything from, and the file the
     * zipped-variant step wrote moments earlier is this run's own output, not the operator's.
     */
    @Test
    fun aFirstGenerationWritesTheLocalVariablesEvenWhenUpdatingIsEnabled(@TempDir tempDir: File) {
        val modpackDir = modpack(tempDir)
        val destination = File(tempDir, "pack")
        val packConfig = packConfig(modpackDir, destination)
        packConfig.isZipCreationDesired = true
        packConfig.scriptSettings["SPC_JAVA_SPC"] = "/opt/a-very-distinctive-jdk/bin/java"

        apiProperties.isServerPacksOverwriteEnabled = true
        apiProperties.isUpdatingServerPacksEnabled = true
        serverPackHandler.run(packConfig)

        Assertions.assertTrue(
            File(destination, "variables.txt").readText().contains("a-very-distinctive-jdk"),
            "A first generation must write the local variables.txt, whatever the update-toggle says"
        )
    }

    /**
     * Lazy mode copies the whole modpack with no exceptions, which is exactly why an update must
     * still keep its hands off a protected path: the modpack ships a world, the operator has been
     * playing it, and "no exceptions" must not mean "including the save you cannot get back".
     */
    @Test
    fun lazyModeStillRespectsProtectionOnAnUpdate(@TempDir tempDir: File) {
        val modpackDir = modpack(tempDir)
        write(modpackDir, "world/level.dat", "the pristine world")
        val destination = File(tempDir, "pack")

        val lazily = packConfig(modpackDir, destination)
        lazily.inclusions.clear()
        lazily.inclusions.add(InclusionSpecification("lazy_mode"))

        apiProperties.isServerPacksOverwriteEnabled = false
        apiProperties.isUpdatingServerPacksEnabled = true
        serverPackHandler.run(lazily)
        Assertions.assertTrue(File(destination, "world/level.dat").isFile, "The first run must ship the world")

        write(destination, "world/level.dat", "a hundred hours later")
        serverPackHandler.run(lazily)

        Assertions.assertEquals(
            "a hundred hours later",
            File(destination, "world/level.dat").readText(),
            "Lazy mode must not overwrite a protected path on an update"
        )
    }

    /**
     * Lazy mode must also report what it copied, so the manifest is a record of the pack rather
     * than of the handful of files provisioned beside it -- otherwise an update of a lazily
     * generated pack has nothing to prune against and the pack never converges on its modpack.
     */
    @Test
    fun lazyModeRecordsWhatItCopied(@TempDir tempDir: File) {
        val modpackDir = modpack(tempDir)
        val destination = File(tempDir, "pack")
        val lazily = packConfig(modpackDir, destination)
        lazily.inclusions.clear()
        lazily.inclusions.add(InclusionSpecification("lazy_mode"))

        apiProperties.isServerPacksOverwriteEnabled = true
        apiProperties.isUpdatingServerPacksEnabled = false
        serverPackHandler.run(lazily)

        val listed = manifestOf(destination).files.map { it.replace('\\', '/') }
        Assertions.assertTrue(
            listed.contains("mods/alpha.jar"),
            "A lazily copied file belongs in the manifest, got $listed"
        )
    }

    /**
     * A run that copied nothing is a broken run, not an empty modpack. Pruning against its result
     * would delete the entire pack and leave a server that cannot start, so a run that produced no
     * files must prune nothing at all.
     */
    @Test
    fun aRunThatProducedNothingPrunesNothing(@TempDir tempDir: File) {
        val modpackDir = modpack(tempDir)
        val destination = File(tempDir, "pack")

        apiProperties.isServerPacksOverwriteEnabled = false
        apiProperties.isUpdatingServerPacksEnabled = true
        serverPackHandler.run(packConfig(modpackDir, destination))
        Assertions.assertTrue(File(destination, "mods/alpha.jar").isFile, "First run must produce the pack")

        modpackDir.deleteRecursively()
        serverPackHandler.run(packConfig(modpackDir, destination))

        Assertions.assertTrue(
            File(destination, "mods/alpha.jar").isFile,
            "A run that produced nothing must not gut the pack it was updating"
        )
        Assertions.assertTrue(File(destination, "config/settings.cfg").isFile, "nor any other part of it")
    }
}
