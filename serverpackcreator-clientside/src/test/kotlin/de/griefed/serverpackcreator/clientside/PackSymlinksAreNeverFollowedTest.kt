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
package de.griefed.serverpackcreator.clientside

import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.clientside.boot.AttemptDirectory
import de.griefed.serverpackcreator.clientside.boot.BootArtifacts
import de.griefed.serverpackcreator.clientside.boot.BootVerifier
import de.griefed.serverpackcreator.clientside.boot.ModWritableTree
import de.griefed.serverpackcreator.clientside.platform.DeclaredSupport
import de.griefed.serverpackcreator.clientside.platform.JarDownloader
import de.griefed.serverpackcreator.clientside.platform.ModFile
import de.griefed.serverpackcreator.clientside.platform.ModPlatform
import de.griefed.serverpackcreator.clientside.platform.ProjectFiles
import de.griefed.serverpackcreator.clientside.selection.BootCandidateSelector
import de.griefed.serverpackcreator.clientside.verdict.PreventionCause
import de.griefed.serverpackcreator.clientside.version.LoaderVersionPolicy
import de.griefed.serverpackcreator.clientside.version.LoaderVersionResolver
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files

/**
 * Pins that host-side code never follows a symbolic link out of a booted server pack. A booted mod writes
 * freely into its pack — the grinder bind-mounts it read-write, the CLI boots it as a host process — so a
 * link it plants there is attacker-controlled: followed by collection it exfiltrates any file the verifier
 * can read into the boot evidence (which the grinder serves unauthenticated), followed by a recursive delete
 * it wipes a directory outside the pack.
 */
internal class PackSymlinksAreNeverFollowedTest {

    /** A host file the verifier can read and a mod must never reach, outside every pack. */
    private fun secretOutside(dir: File): File =
        File(dir, "outside/id_ed25519").apply { parentFile.mkdirs(); writeText(SECRET) }

    /** Plant [link] → [target] the way a mod would, skipping where the OS refuses symbolic links. */
    private fun plant(link: File, target: File) {
        link.parentFile.mkdirs()
        val planted = runCatching { Files.createSymbolicLink(link.toPath(), target.toPath()) }
        Assumptions.assumeTrue(planted.isSuccess, "this OS refuses symbolic links: ${planted.exceptionOrNull()}")
    }

    private fun packWithARegularLog(dir: File): File = File(dir, "serverpack").apply {
        File(this, "logs").mkdirs()
        File(this, "logs/latest.log").writeText("latest log body")
    }

    @Test
    fun aSymlinkedLogFilePointingOutsideThePackIsNotCollected(@TempDir dir: File) {
        val pack = packWithARegularLog(dir)
        plant(File(pack, "logs/stolen.log"), secretOutside(dir))

        val artifacts = BootArtifacts.collect(pack, console = null)

        Assertions.assertEquals(
            "latest log body", artifacts.firstOrNull { it.name == "logs-latest.log" }?.content,
            "positive control: a regular log is still collected — got ${artifacts.map { it.name }}"
        )
        Assertions.assertTrue(
            artifacts.none { it.name == "logs-stolen.log" },
            "a symlinked log must not be collected — got ${artifacts.map { it.name }}"
        )
        Assertions.assertTrue(
            artifacts.none { it.content.contains(SECRET) },
            "the link's target content must appear in no artifact"
        )
        Assertions.assertTrue(
            artifacts.first { it.name == BootArtifacts.INDEX_NAME }.content.contains("logs/stolen.log"),
            "the refused link is named in the index, since it is evidence a mod tried something"
        )
    }

    @Test
    fun aSymlinkedEvidenceDirectoryPointingOutsideThePackIsNotDescended(@TempDir dir: File) {
        val pack = packWithARegularLog(dir)
        plant(File(pack, "crash-reports"), secretOutside(dir).parentFile)

        val artifacts = BootArtifacts.collect(pack, console = null)

        Assertions.assertEquals(
            "latest log body", artifacts.firstOrNull { it.name == "logs-latest.log" }?.content,
            "positive control: a regular log is still collected — got ${artifacts.map { it.name }}"
        )
        Assertions.assertTrue(
            artifacts.none { it.name.startsWith("crash-reports-") },
            "a symlinked evidence directory must not be descended — got ${artifacts.map { it.name }}"
        )
        Assertions.assertTrue(artifacts.none { it.content.contains(SECRET) }, "the outside file must not be read")
    }

    @Test
    fun theStagingWipeLeavesAFileInASymlinkedOutsideDirectoryIntact(@TempDir dir: File) {
        val workDir = File(dir, "work")
        val project = lockedCandidate()
        val attemptDir = File(
            workDir,
            AttemptDirectory.nameFor(project.platform, project.slug, "Fabric", BootCandidateSelector.minecraftLine(fabricRelease))
        )
        File(attemptDir, "serverpack/logs/latest.log").apply { parentFile.mkdirs(); writeText("previous boot") }
        val secret = secretOutside(dir)
        plant(File(attemptDir, "serverpack/world"), secret.parentFile)

        verifierFor(workDir).prepareBootPack(project, "Fabric")

        Assertions.assertFalse(
            File(attemptDir, "serverpack/logs/latest.log").exists(),
            "positive control: the wipe ran and removed the previous attempt's pack"
        )
        Assertions.assertTrue(secret.isFile, "the wipe followed the link and deleted a file outside the pack")
        Assertions.assertEquals(SECRET, secret.readText())
    }

    /**
     * A mod can make part of its pack unremovable (a directory without write permission keeps its links), and
     * staging into what survives would write through them. Manufactured with a permission root ignores, so the
     * precondition is asserted and the test skips rather than passes where it did not hold.
     */
    @Test
    fun stagingRefusesWhenThePreviousAttemptCannotBeCleared(@TempDir dir: File) {
        val workDir = File(dir, "work")
        val project = lockedCandidate()
        val attemptDir = File(
            workDir,
            AttemptDirectory.nameFor(project.platform, project.slug, "Fabric", BootCandidateSelector.minecraftLine(fabricRelease))
        )
        val sealed = File(attemptDir, "serverpack/sealed").apply { mkdirs() }
        plant(File(sealed, "mods"), File(dir, "outside").apply { mkdirs() })
        Assertions.assertTrue(sealed.setWritable(false), "could not clear write permission on the fixture")
        try {
            val prepared = verifierFor(workDir).prepareBootPack(project, "Fabric")

            Assumptions.assumeTrue(
                Files.isSymbolicLink(File(sealed, "mods").toPath()),
                "the unwritable directory's link was deleted anyway (running as root?)"
            )
            Assertions.assertTrue(
                prepared is BootVerifier.Prepared.Failed &&
                    prepared.detail.contains("Could not clear the previous attempt's staging") &&
                    prepared.cause == PreventionCause.HOST,
                "staging must refuse, blamed on the host, rather than stage into a tree that kept a link: $prepared"
            )
        } finally {
            sealed.setWritable(true)
        }
    }

    @Test
    fun aRecursiveDeleteRemovesALinkAsALinkAndReportsSuccess(@TempDir dir: File) {
        val tree = File(dir, "tree").apply { File(this, "nested").mkdirs() }
        File(tree, "nested/file.txt").writeText("gone")
        val secret = secretOutside(dir)
        plant(File(tree, "nested/link-to-dir"), secret.parentFile)
        plant(File(tree, "link-to-file"), secret)

        Assertions.assertTrue(ModWritableTree.deleteRecursively(tree), "everything inside the tree was deletable")

        Assertions.assertFalse(tree.exists(), "the tree itself is gone")
        Assertions.assertEquals(SECRET, secret.readText(), "neither link's target was touched")
    }

    @Test
    fun theSizeOfATreeCountsNothingBehindALink(@TempDir dir: File) {
        val tree = File(dir, "tree").apply { mkdirs() }
        File(tree, "own.bin").writeBytes(ByteArray(100))
        val secret = secretOutside(dir)
        plant(File(tree, "link-to-dir"), secret.parentFile)
        plant(File(tree, "loop"), tree)

        Assertions.assertEquals(100L, ModWritableTree.sizeOf(tree))
    }

    private val apiWrapper = ApiWrapper.api(File("build/resources/test/serverpackcreator.properties"))
    private val resolver = LoaderVersionResolver(apiWrapper.versionMeta)

    private val fabricRelease = apiWrapper.versionMeta.minecraft.serverReleases()
        .map { it.minecraftVersion }
        .first { resolver.latest("Fabric", it) != null }

    /** Locked, so staging wipes the attempt directory and then refuses before anything is generated. */
    private fun lockedCandidate() = ProjectFiles(
        "CurseForge", "symlink-planter", "https://www.curseforge.com/minecraft/mc-mods/symlink-planter",
        DeclaredSupport.UNKNOWN, DeclaredSupport.UNKNOWN,
        listOf(ModFile("planter-1.0.jar", setOf("Fabric"), setOf(fabricRelease), null, null, emptyList()))
    )

    private fun verifierFor(workDir: File) = BootVerifier(
        apiWrapper = apiWrapper,
        platform = object : ModPlatform {
            override val name: String = "CurseForge"
            override fun handles(projectUrl: String): Boolean = true
            override fun resolve(projectUrl: String): ProjectFiles = error("not used")
            override fun resolveDependency(nativeRef: String, minecraftVersion: String?): ProjectFiles? = null
        },
        httpDownloader = JarDownloader { _, _ -> null },
        loaderVersionPolicy = object : LoaderVersionPolicy {
            override fun preferredVersion(loader: String, minecraftVersion: String) = "0.0.0-no-such-build"
            override fun latestVersion(loader: String, minecraftVersion: String) = "0.0.0-no-such-build"
        },
        workDirectory = workDir
    )

    private companion object {
        const val SECRET = "-----BEGIN OPENSSH PRIVATE KEY----- not really"
    }
}
