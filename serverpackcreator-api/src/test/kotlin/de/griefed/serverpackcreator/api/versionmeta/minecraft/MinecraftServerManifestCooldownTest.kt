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
package de.griefed.serverpackcreator.api.versionmeta.minecraft

import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.api.versionmeta.Type
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.Duration
import java.time.Instant

/**
 * Pins that a failed server-manifest download is not retried on every lookup.
 *
 * Nothing remembered a failure: `setServerJson()` re-downloads whenever the manifest file is absent, and a failed
 * `downloadFile` **deletes** the partial file and returns false, so the next call is in exactly the same position.
 * `getServer` then evaluates `url().isPresent && javaVersion().isPresent`, and both go through that path — so one
 * `requiredJavaVersion` lookup on an unfetchable version costs two attempts.
 *
 * That lookup is hot: `ImageJavaRuntimes.requiredJavaMajor` reaches it from `supportFor`, `javaPath` *and*
 * `installerJavaPathFor` — per candidate in the grinder, per cell in the template matrix, per version selection in
 * the GUI. And every attempt is loud: `WebUtilities.downloadFile` logs the failure at **ERROR with a stack trace**
 * before returning false, so an unreachable manifest floods the log in proportion to catalogue size.
 *
 * A cooldown rather than a permanent memory, deliberately: the grinder runs for days, so a transient network
 * failure must not poison a version for the life of the process. Same shape and default as
 * `LoaderCache.failureCooldown`, which exists for the identical reason.
 */
internal class MinecraftServerManifestCooldownTest {

    private val apiWrapper = ApiWrapper.api(File("build/resources/test/serverpackcreator.properties"))

    /** A version id no real manifest uses, so this test cannot collide with the cached metadata. */
    private val probeVersion = "b23-cooldown-probe"

    /** Where `MinecraftServer` will look for the manifest — removed again afterwards. */
    private val manifestFile: File
        get() = File(apiWrapper.apiProperties.minecraftServerManifestsDirectory, "$probeVersion.json")

    @AfterEach
    fun removeProbeManifest() {
        manifestFile.delete()
    }

    /** A manifest carrying the two fields `url()` and `javaVersion()` read. */
    private fun manifestJson(javaMajor: Int) = """
        {
          "downloads": { "server": { "url": "https://example.invalid/server.jar" } },
          "javaVersion": { "component": "java-runtime-probe", "majorVersion": $javaMajor }
        }
    """.trimIndent()

    /**
     * A failed download is not retried while the cooldown holds, and *is* retried once it lapses.
     *
     * The source is a `file:` URL whose target does not exist yet, so the first attempt fails exactly as an
     * unreachable host would. Creating the target afterwards is what makes the retry observable: if the download is
     * gated the lookup stays empty, and if it is not, the value appears immediately.
     */
    @Test
    fun aFailedManifestDownloadIsNotRetriedUntilTheCooldownLapses(@TempDir tempDir: File) {
        manifestFile.delete()
        val source = File(tempDir, "$probeVersion.json")
        var now = Instant.parse("2026-07-31T12:00:00Z")
        val server = MinecraftServer(
            minecraftVersion = probeVersion,
            releaseType = Type.RELEASE,
            serverUrl = source.toURI().toURL(),
            utilities = apiWrapper.utilities,
            apiProperties = apiWrapper.apiProperties,
            downloadCooldown = Duration.ofHours(1),
            clock = { now }
        )

        Assertions.assertFalse(
            server.javaVersion().isPresent,
            "precondition: with no manifest and an unfetchable source there is nothing to report"
        )

        // The manifest becomes fetchable, but the cooldown has not lapsed.
        source.writeText(manifestJson(21))

        Assertions.assertFalse(
            server.javaVersion().isPresent,
            "the download must not be retried while the cooldown holds — this is the retry storm: every lookup " +
                "re-attempts, and each attempt logs an ERROR with a stack trace from WebUtilities"
        )
        Assertions.assertFalse(
            server.url().isPresent,
            "url() shares the same path, which is why one getServer() lookup used to cost two attempts"
        )

        now = now.plus(Duration.ofHours(1)).plusSeconds(1)

        Assertions.assertEquals(
            21.toByte(),
            server.javaVersion().orElse(null),
            "once the cooldown lapses the download must be attempted again — a cooldown, not a permanent memory, " +
                "because the grinder runs for days and a transient failure must not poison a version for its whole life"
        )
    }

    /**
     * A manifest that is already on disk is read regardless of any cooldown.
     *
     * The cooldown gates the *network attempt*, not the lookup: re-reading a file that is already there costs
     * nothing and cannot fail for the same reason, so gating it would turn a cheap hit into a false "unknown" —
     * which is precisely the answer that made the newest Minecraft versions vanish from the template matrix.
     */
    @Test
    fun anAlreadyPresentManifestIsReadEvenWhileTheCooldownHolds(@TempDir tempDir: File) {
        val source = File(tempDir, "$probeVersion.json")
        var now = Instant.parse("2026-07-31T12:00:00Z")
        val server = MinecraftServer(
            minecraftVersion = probeVersion,
            releaseType = Type.RELEASE,
            serverUrl = source.toURI().toURL(),
            utilities = apiWrapper.utilities,
            apiProperties = apiWrapper.apiProperties,
            downloadCooldown = Duration.ofHours(1),
            clock = { now }
        )

        manifestFile.delete()
        Assertions.assertFalse(server.javaVersion().isPresent, "precondition: the first attempt fails")

        // Whatever put it there -- another process, a manual copy, a previous run -- it needs no download.
        manifestFile.parentFile.mkdirs()
        manifestFile.writeText(manifestJson(17))

        Assertions.assertEquals(
            17.toByte(),
            server.javaVersion().orElse(null),
            "a manifest already on disk must be read even inside the cooldown; the cooldown gates downloads only"
        )
    }
}
