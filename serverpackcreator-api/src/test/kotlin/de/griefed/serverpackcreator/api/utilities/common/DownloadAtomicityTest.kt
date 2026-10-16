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
package de.griefed.serverpackcreator.api.utilities.common

import de.griefed.serverpackcreator.api.ApiProperties
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Pins that a download either replaces its destination completely or leaves it alone.
 *
 * Every caller reads the returned `true` as *I now have this file*: `ForgeMeta.installerFor` and its three
 * siblings hand the destination straight back as an installer, `HttpJarDownloader` returns it as a mod jar
 * to be staged, and `MinecraftServer` parses it. A partially written file satisfies `isFile`, so a killed
 * process could leave one and have it served from cache as a complete artifact ever after — the same shape
 * as the truncated version manifest that ended every grinder start.
 */
internal class DownloadAtomicityTest {

    /** An [ApiProperties] whose network timeouts are short, so nothing here can hang the suite. */
    private fun timedProperties(): ApiProperties {
        val apiProperties = mockk<ApiProperties>(relaxed = true)
        every { apiProperties.networkConnectTimeout } returns 2_000
        every { apiProperties.networkReadTimeout } returns 2_000
        every { apiProperties.networkDownloadReadTimeout } returns 2_000
        return apiProperties
    }

    /**
     * Pins that a download which could not be performed leaves the file that was already there.
     *
     * The destination was created, then truncated, then deleted when the source turned out to be
     * unreachable — so a re-download of something the host already held destroyed the good copy and left
     * nothing. A `file:` URL to a path that does not exist fails exactly as an unreachable host does.
     */
    @Test
    fun aFailedDownloadLeavesAnExistingFileIntact(@TempDir tempDir: File) {
        val destination = File(tempDir, "forge-installer.jar")
        val original = "the copy this host already had"
        destination.writeText(original)
        val absentSource = File(tempDir, "nothing-here.jar")

        val downloaded = WebUtilities(timedProperties()).downloadFile(destination, absentSource.toURI().toURL())

        Assertions.assertFalse(downloaded, "A download that did not happen must not report success")
        Assertions.assertEquals(
            original, destination.readText(),
            "A failed download must not destroy the copy the host already had"
        )
    }

    /**
     * Pins that a failed download still reports failure when a file was already present.
     *
     * The counterweight to the guard above: now that the destination survives, `isFile` is true after a
     * failure, so a return value derived from the file's existence would answer "downloaded" for a download
     * that never happened — and every caller listed on this class would treat a stale artifact as a fresh one.
     */
    @Test
    fun aFailedDownloadReportsFailureEvenThoughTheOldFileSurvives(@TempDir tempDir: File) {
        val destination = File(tempDir, "forge-installer.jar")
        destination.writeText("stale")

        val downloaded = WebUtilities(timedProperties())
            .downloadFile(destination, File(tempDir, "absent.jar").toURI().toURL())

        Assertions.assertFalse(downloaded, "The answer is about this call, not about whether a file exists")
        Assertions.assertTrue(destination.isFile, "...and the old file is still there, which is the point")
    }

    /** Pins that a completed download leaves the destination and nothing beside it. */
    @Test
    fun aSuccessfulDownloadLeavesNoStagingFileBehind(@TempDir tempDir: File) {
        val source = File(tempDir, "source").apply { mkdirs() }.let { File(it, "payload.jar") }
        source.writeText("payload")
        val target = File(tempDir, "target").apply { mkdirs() }
        val destination = File(target, "forge-installer.jar")

        Assertions.assertTrue(
            WebUtilities(timedProperties()).downloadFile(destination, source.toURI().toURL()),
            "precondition: the download succeeds"
        )

        Assertions.assertEquals("payload", destination.readText())
        Assertions.assertArrayEquals(
            arrayOf("forge-installer.jar"), target.list()?.sortedArray(),
            "A completed download must leave the artifact and no staging beside it"
        )
    }

    /** Pins the same after a failure — staging that outlives its attempt accumulates one file per try. */
    @Test
    fun aFailedDownloadLeavesNoStagingFileBehind(@TempDir tempDir: File) {
        val target = File(tempDir, "target").apply { mkdirs() }
        val destination = File(target, "forge-installer.jar")

        WebUtilities(timedProperties()).downloadFile(destination, File(tempDir, "absent.jar").toURI().toURL())

        Assertions.assertArrayEquals(
            emptyArray<String>(), target.list()?.sortedArray(),
            "A failed download must leave nothing at all where there was nothing before"
        )
    }
}
