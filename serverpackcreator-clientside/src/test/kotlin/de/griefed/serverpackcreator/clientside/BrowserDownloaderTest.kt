/* Copyright (C) 2025 Griefed
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
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Covers the [BrowserDownloader] behaviour that does **not** require launching Chromium (the live
 * download is integration-only). A file with no page-URL must short-circuit to `null` without ever
 * starting the browser.
 */
internal class BrowserDownloaderTest {

    @Test
    fun returnsNullWithoutLaunchingBrowserWhenNoPageUrl(@TempDir tempDir: File) {
        val file = ModFile("mod.jar", setOf("Forge"), setOf("1.20.1"), downloadUrl = null, pageUrl = null, requiredDependencies = emptyList())
        // requestDelayMillis = 0 so the test does not sleep; the early return precedes any browser use.
        BrowserDownloader(requestDelayMillis = 0).use { downloader ->
            Assertions.assertNull(downloader.download(file, tempDir))
        }
    }
}
