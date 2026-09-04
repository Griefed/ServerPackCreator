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
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Pins that a project's scratch space is owned by `(platform, slug, loader)` and not by `(slug, loader)`.
 *
 * **Why it is not merely tidy.** Staging *wipes* the directory it is about to use, and the grinder's reaper
 * deletes it again once a candidate's verdicts are in. The same slug on Modrinth and on CurseForge is two
 * candidates — the grinder says so itself, keying verdict freshness on `(platform, slug)` — and its workers
 * run them in parallel. Sharing one directory therefore lets one candidate delete the server pack out from
 * under a container the other is still booting.
 *
 * Observed 2026-08-23 on `creativecore`, whose two platform runs finished 71 seconds apart: NeoForge 26.2.0.66
 * on Minecraft 26.2 SURVIVED for one platform and CRASHED (exit 1) for the other, same loader build, same
 * Minecraft, same mod; a Fabric boot exited **127** (a shell that could not find the command it was told to
 * run); and two re-checks came back INCONCLUSIVE on files another run had booted to a ready-line.
 */
internal class AttemptStagingIsolationTest {

    private val apiWrapper = ApiWrapper.api(File("build/resources/test/serverpackcreator.properties"))

    /** A platform answering for one host with a single Fabric file, under the slug both platforms share. */
    private fun platform(name: String, host: String) = object : ModPlatform {
        // The same [name] the resolved ProjectFiles carry: a candidate's platform and its verdict's platform
        // must agree exactly, which is what this fixture exists to exercise.
        override val name: String = name
        override fun handles(projectUrl: String): Boolean = projectUrl.contains(host, ignoreCase = true)
        override fun resolve(projectUrl: String): ProjectFiles = ProjectFiles(
            platform = name,
            slug = "creativecore",
            projectUrl = projectUrl,
            clientSide = DeclaredSupport.UNKNOWN,
            serverSide = DeclaredSupport.UNKNOWN,
            files = listOf(
                ModFile(
                    "CreativeCore_FABRIC_v2.14.16_mc1.20.1.jar",
                    setOf("Fabric"),
                    setOf("1.20.1"),
                    "https://cdn/creativecore.jar",
                    null,
                    emptyList()
                )
            )
        )

        override fun resolveDependency(nativeRef: String, minecraftVersion: String?): ProjectFiles? = null
    }

    /**
     * The jar-scan download directory, which `ClientsideVerifier` hands the downloader. Executed rather than
     * inspected: the downloader records the directory it was actually given and reports failure, which is as
     * far as a metadata-only report needs to go.
     */
    @Test
    fun theJarScanOfTwoPlatformsSharingASlugDownloadsIntoSeparateDirectories(@TempDir workDir: File) {
        val handed = mutableListOf<File>()
        val recordingDownloader = JarDownloader { _, targetDirectory ->
            handed.add(targetDirectory)
            null
        }
        fun verifier() = ClientsideVerifier(
            platforms = listOf(platform("Modrinth", "modrinth.com"), platform("CurseForge", "curseforge.com")),
            metadataScanner = MetadataScanner(apiWrapper.modScanner),
            jarDownloader = recordingDownloader,
            workDirectory = workDir
        )

        verifier().report("https://modrinth.com/mod/creativecore")
        verifier().report("https://www.curseforge.com/minecraft/mc-mods/creativecore")

        Assertions.assertEquals(
            listOf(
                AttemptDirectory.nameFor("Modrinth", "creativecore", "Fabric"),
                AttemptDirectory.nameFor("CurseForge", "creativecore", "Fabric")
            ),
            handed.map { it.name },
            "the same slug on two platforms must not share one scratch directory"
        )
    }

    /**
     * A directory name has to be readable back to the candidate that owns it, because the grinder's reaper
     * decides what to delete from the name alone. Round-tripped through both halves of the helper so the
     * producer and the consumer cannot drift into disagreeing.
     */
    @Test
    fun anAttemptDirectoryNamesTheCandidateThatOwnsIt() {
        val modrinth = AttemptDirectory.nameFor("Modrinth", "creativecore", "Fabric")
        val curseForge = AttemptDirectory.nameFor("CurseForge", "creativecore", "Fabric")

        Assertions.assertEquals(AttemptDirectory.ownerKey("Modrinth", "creativecore"), AttemptDirectory.ownerOf(modrinth))
        Assertions.assertEquals(AttemptDirectory.ownerKey("CurseForge", "creativecore"), AttemptDirectory.ownerOf(curseForge))
        Assertions.assertNotEquals(AttemptDirectory.ownerOf(modrinth), AttemptDirectory.ownerOf(curseForge))
    }

    /**
     * Slugs nest and so do the names built from them: `creativecore` must not read as the owner of
     * `creativecore-extras`, or reaping one would delete the other mid-boot — the same hazard the reaper
     * already guards for a bare slug, restated for the qualified name.
     */
    @Test
    fun aSlugThatPrefixesAnotherOwnsADifferentDirectory() {
        Assertions.assertNotEquals(
            AttemptDirectory.ownerOf(AttemptDirectory.nameFor("Modrinth", "creativecore", "Fabric")),
            AttemptDirectory.ownerOf(AttemptDirectory.nameFor("Modrinth", "creativecore-extras", "Fabric"))
        )
    }
}
