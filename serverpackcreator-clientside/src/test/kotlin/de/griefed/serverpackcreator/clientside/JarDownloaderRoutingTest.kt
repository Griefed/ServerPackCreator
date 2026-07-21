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
internal class JarDownloaderRoutingTest {

    private val http = JarDownloader { _, _ -> File("http") }
    private val browser = JarDownloader { _, _ -> File("browser") }

    private fun file(downloadUrl: String?) =
        ModFile("mod.jar", setOf("Forge"), setOf("1.20.1"), downloadUrl, "page", emptyList())

    @Test
    fun lockedFileGoesToBrowser() {
        Assertions.assertSame(browser, selectDownloader(file(downloadUrl = null), http, browser))
    }

    @Test
    fun downloadableFileGoesToHttp() {
        Assertions.assertSame(http, selectDownloader(file(downloadUrl = "https://cdn/mod.jar"), http, browser))
    }
}
