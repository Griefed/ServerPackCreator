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
package de.griefed.serverpackcreator.api.versionmeta

import com.fasterxml.jackson.databind.ObjectMapper
import de.griefed.serverpackcreator.api.utilities.common.JsonUtilities
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.utilities.common.WebUtilities
import de.griefed.serverpackcreator.api.utilities.common.XmlUtilities
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Pins that a manifest which can no longer be parsed is repaired rather than left to kill every start.
 *
 * Reported from a live grinder host: `forge-manifest.json` had been truncated mid-write, and because the
 * jar only seeds a manifest that does **not** exist, every subsequent start re-read the same broken file
 * and died in `ForgeLoader.update`. Nothing in the process could recover from it, and the stack trace
 * named neither the file nor a remedy.
 */
internal class ManifestIntegrityTest {

    /** A [ManifestIntegrity] over real parsers — the question is whether a file parses, so nothing is mocked. */
    private fun integrity() = ManifestIntegrity(
        Utilities(
            WebUtilities(mockk(relaxed = true)),
            JsonUtilities(ObjectMapper()),
            XmlUtilities(DocumentBuilderFactory.newInstance())
        )
    )

    /** The first [bytes] bytes of the manifest the jar ships under [name] — i.e. a plausible partial write. */
    private fun truncatedShippedManifest(name: String, bytes: Int): String =
        javaClass.getResourceAsStream("/de/griefed/resources/manifests/$name")!!
            .use { it.readBytes() }
            .decodeToString()
            .take(bytes)

    /**
     * Pins the reported failure: a truncated JSON manifest is replaced by the copy in the jar, so the very
     * next parse succeeds.
     */
    @Test
    fun aTruncatedJsonManifestIsRestoredFromTheJar(@TempDir tempDir: File) {
        val manifest = File(tempDir, "forge-manifest.json")
        manifest.writeText(truncatedShippedManifest("forge-manifest.json", 90_000))

        val restored = integrity().restoreIfUnreadable(manifest)

        Assertions.assertTrue(restored, "A manifest that cannot be parsed must be reported as restored")
        Assertions.assertDoesNotThrow({ ObjectMapper().readTree(manifest) }, "The restored manifest must parse")
    }

    /** Same for the XML manifests — the loader metadata is maven-metadata, not JSON. */
    @Test
    fun aTruncatedXmlManifestIsRestoredFromTheJar(@TempDir tempDir: File) {
        val manifest = File(tempDir, "neoforge-manifest-new.xml")
        manifest.writeText(truncatedShippedManifest("neoforge-manifest-new.xml", 40_000))

        val restored = integrity().restoreIfUnreadable(manifest)

        Assertions.assertTrue(restored, "A truncated maven-metadata must be restored too")
        Assertions.assertDoesNotThrow(
            { DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(manifest) },
            "The restored manifest must parse"
        )
    }

    /**
     * Pins that an intact manifest is left exactly as it is.
     *
     * Without this the repair would silently throw away every refresh the host had collected, replacing an
     * up-to-date manifest with the shipped snapshot on every single start.
     */
    @Test
    fun anIntactManifestIsLeftAlone(@TempDir tempDir: File) {
        val manifest = File(tempDir, "forge-manifest.json")
        val content = """{"1.21.1":["1.21.1-52.1.0"]}"""
        manifest.writeText(content)

        val restored = integrity().restoreIfUnreadable(manifest)

        Assertions.assertFalse(restored, "A manifest that parses must not be reported as restored")
        Assertions.assertEquals(content, manifest.readText(), "A parseable manifest must not be touched")
    }

    /**
     * Pins that a broken manifest the jar does not carry is deleted rather than left in place.
     *
     * The per-version `mcserver/<version>.json` files are the real case: they are fetched on demand, so
     * removing a broken one costs exactly one request, while keeping it costs the same failure for ever.
     */
    @Test
    fun aBrokenManifestWithNoShippedCopyIsRemoved(@TempDir tempDir: File) {
        val manifest = File(tempDir, "no-such-manifest.json")
        manifest.writeText("""{"versions":[""")

        val restored = integrity().restoreIfUnreadable(manifest)

        Assertions.assertTrue(restored, "Removing the broken file is a repair and must be reported as one")
        Assertions.assertFalse(manifest.exists(), "A broken manifest with no shipped copy must not survive")
    }

    /**
     * Pins that the ETag sidecar goes with the manifest it described.
     *
     * The sidecar earns a `304`, which is a promise that the local copy is already the upstream one. Leaving
     * it beside a replaced manifest would suppress the very refresh the repair exists to make possible —
     * [ManifestUpdater] does verify the recorded byte length, but a repair must not be left depending on two
     * files happening to differ in size.
     */
    @Test
    fun theEtagSidecarIsDiscardedWithTheManifest(@TempDir tempDir: File) {
        val manifest = File(tempDir, "forge-manifest.json")
        manifest.writeText(truncatedShippedManifest("forge-manifest.json", 90_000))
        val sidecar = File(tempDir, "forge-manifest.json.etag")
        sidecar.writeText("W/\"abc\"\n${manifest.length()}")

        integrity().restoreIfUnreadable(manifest)

        Assertions.assertFalse(sidecar.exists(), "An ETag describing bytes that are gone must not survive them")
    }

    /**
     * Pins that a repaired manifest asks for the whole upstream copy at the next check.
     *
     * A restored manifest is the *shipped snapshot*, which is older than what the host had — and the refresh
     * asks conditionally, against the local file's modification time. Freshly written, that is "now", so the
     * four upstreams that honour `If-Modified-Since` would answer `304` and leave the host on the snapshot
     * until upstream happened to publish something. A modification time of zero is what
     * `HttpURLConnection` reads as "do not ask conditionally" — see
     * `ManifestUpdaterTest.aManifestWithNoModificationTimeIsFetchedUnconditionally`, which pins that half.
     */
    @Test
    fun aRestoredManifestAsksForTheFullUpstreamCopyNextTime(@TempDir tempDir: File) {
        val manifest = File(tempDir, "forge-manifest.json")
        manifest.writeText(truncatedShippedManifest("forge-manifest.json", 90_000))

        integrity().restoreIfUnreadable(manifest)

        Assertions.assertEquals(
            0L, manifest.lastModified(),
            "A restored snapshot must not be mistaken for an up-to-date copy by the conditional request"
        )
    }
}
