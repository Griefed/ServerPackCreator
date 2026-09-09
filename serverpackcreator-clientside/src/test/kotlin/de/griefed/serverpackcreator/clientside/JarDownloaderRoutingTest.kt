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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Pins that a file is routed to the browser-downloader only when distribution-locked (no
 * `downloadUrl`), and to the HTTP-downloader otherwise — without launching a real browser.
 */
/**
 * Pins that there is exactly **one** download route, and that a distribution-locked file is reported rather
 * than worked around.
 *
 * Until 2026-09-02 a locked CurseForge file (`allowModDistribution=false`, so the API returns no
 * `downloadUrl`) was routed to a headless-browser downloader that drove the website instead. That existed
 * only to get around the author's opt-out of third-party distribution, it cost **192.9 MB** of bundled node
 * binaries in every shipped artifact — ~70% of the 274.7 MB app jar — and by the end it did not work at all,
 * because CurseForge is behind a Cloudflare challenge the browser does not clear. Griefed's call: remove it.
 *
 * What still works is everything that was never blocked: any CurseForge file the API gives a URL for, and
 * all of Modrinth.
 */
internal class JarDownloaderRoutingTest {

    private val http = JarDownloader { _, _ -> File("http") }

    private fun file(downloadUrl: String?) =
        ModFile("mod.jar", setOf("Forge"), setOf("1.20.1"), downloadUrl, "page", emptyList())

    /** A file the platform will serve is downloaded, exactly as before. */
    @Test
    fun aDownloadableFileIsFetchedOverHttp() {
        Assertions.assertEquals(File("http"), http.download(file("https://cdn/mod.jar"), File("target")))
    }

    /**
     * **A locked file is still *recognised*, and there is no longer anywhere to route it.** The flag stays
     * useful — it is what lets a refusal explain itself — but the browser that used to consume it is gone,
     * asserted by its absence from the classpath rather than by trusting the deletion.
     */
    @Test
    fun aLockedFileIsRecognisedAndHasNoBrowserFallback() {
        Assertions.assertTrue(file(downloadUrl = null).locked, "no downloadUrl still means distribution-locked")

        Assertions.assertThrows(ClassNotFoundException::class.java, {
            Class.forName("de.griefed.serverpackcreator.clientside.BrowserDownloader")
        }, "the headless-browser downloader must be gone, not merely unused")
    }

    /**
     * There is no `selectDownloader` any more — one route means nothing to select between.
     *
     * The file now declares no top-level function at all, so Kotlin emits **no** `JarDownloaderKt` facade;
     * the class being absent is therefore a stronger result than an empty method list, and both count.
     */
    @Test
    fun noRoutingHelperSurvives() {
        val facade = runCatching {
            Class.forName("de.griefed.serverpackcreator.clientside.JarDownloaderKt")
        }.getOrNull()

        val router = facade?.declaredMethods?.map { it.name }?.filter {
            it.contains("selectDownloader", ignoreCase = true)
        }
        Assertions.assertTrue(
            router.isNullOrEmpty(),
            "a router with one route is dead weight, but it is still here: $router"
        )
    }
}
