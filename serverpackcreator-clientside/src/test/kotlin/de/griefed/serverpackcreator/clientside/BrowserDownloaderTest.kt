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

import com.microsoft.playwright.options.WaitUntilState
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

    /**
     * The download-flow's own navigation is expected to be aborted: CurseForge answers `/download` with a
     * file transfer, and Chromium aborts a navigation that turns into one. Playwright surfaces that as a
     * thrown `net::ERR_ABORTED`, which used to escape the `waitForDownload` callback and tear the wait down
     * — discarding a download that had in fact started. Observed live 2026-08-23 on `bwncr-neoforge`,
     * `tombstone-neoforge` and `Structory`, all stacked at `_FrameSession._navigate`.
     */
    @Test
    fun treatsAnAbortedNavigationAsTheDownloadStarting() {
        Assertions.assertTrue(
            BrowserDownloader.isDownloadAbort(RuntimeException("Error { message='net::ERR_ABORTED; maybe frame was detached?' }")),
            "net::ERR_ABORTED is how a navigation-turned-download reports itself"
        )
        Assertions.assertTrue(
            BrowserDownloader.isDownloadAbort(RuntimeException("Download is starting")),
            "Playwright also words the same event this way"
        )
    }

    @Test
    fun doesNotSwallowANavigationThatFailedForARealReason() {
        Assertions.assertFalse(
            BrowserDownloader.isDownloadAbort(RuntimeException("net::ERR_NAME_NOT_RESOLVED at https://www.curseforge.com/")),
            "a genuine navigation failure must not be mistaken for a download"
        )
        Assertions.assertFalse(
            BrowserDownloader.isDownloadAbort(RuntimeException("Timeout 30000ms exceeded")),
            "a timeout is not a download"
        )
        Assertions.assertFalse(BrowserDownloader.isDownloadAbort(RuntimeException(null as String?)))
    }


    /**
     * The other two behaviours changed alongside the abort fix, which had no guard at all until audit iteration
     * 17 (M2). Both are decided host-side, before any browser exists, so they are pinned by *building* the
     * options rather than by reading the source for them.
     *
     * `load` is the default and the wrong one here: a CurseForge project page keeps fetching ads and trackers
     * long after it is usable, so waiting for it spends the whole budget on third parties. And every timeout in
     * the 2026-08-23 run was exactly `Timeout 30000ms exceeded` — Playwright's untouched default, never a
     * page-specific budget.
     */
    @Test
    fun waitsForTheDomRatherThanForEveryAdOnThePage() {
        val options = BrowserDownloader(requestDelayMillis = 0, navigationTimeoutMillis = 45_000.0).navigationOptions()

        Assertions.assertEquals(WaitUntilState.DOMCONTENTLOADED, options.waitUntil)
        Assertions.assertEquals(45_000.0, options.timeout)
    }

    @Test
    fun givesTheDownloadItselfTheSameBudgetAsTheNavigation() {
        val downloader = BrowserDownloader(requestDelayMillis = 0, navigationTimeoutMillis = 45_000.0)

        Assertions.assertEquals(45_000.0, downloader.downloadOptions().timeout)
    }

    @Test
    fun defaultsToMoreThanPlaywrightsThirtySeconds() {
        Assertions.assertTrue(
            (BrowserDownloader().navigationOptions().timeout ?: 0.0) > 30_000.0,
            "30s is the default that timed out on every locked CurseForge file"
        )
    }

}
