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

import com.microsoft.playwright.*
import com.microsoft.playwright.options.WaitUntilState
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File

/**
 * Downloads distribution-locked CurseForge files (`allowModDistribution=false`, no API
 * `downloadUrl`) by driving the project's website download-flow with a headless Chromium through
 * Playwright — the only way to obtain these files, as the API deliberately withholds them and the
 * CDN 403s direct requests. The browser is launched **lazily** (only when a locked file is actually
 * encountered) and reused; call [close] to dispose it.
 *
 * Scope guard: files are fetched transiently for local scan/boot and discarded, never re-hosted —
 * equivalent to a maintainer manually downloading the mod to test it. An honest User-Agent and a
 * small inter-request delay keep the access respectful.
 *
 * @param requestDelayMillis    Pause before each download to avoid hammering CurseForge.
 * @param navigationTimeoutMillis Budget for each navigation and for the download itself. Playwright's own
 *                                default is 30s, which a Cloudflare-fronted, ad-laden project page routinely
 *                                exceeds — every timeout observed on 2026-08-23 was exactly `30000ms`.
 * @author Griefed
 */
class BrowserDownloader(
    private val requestDelayMillis: Long = 2_000,
    private val navigationTimeoutMillis: Double = 60_000.0
) : JarDownloader, AutoCloseable {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
    private val userAgent =
        "Mozilla/5.0 (compatible; ServerPackCreator clientside-verification; +https://github.com/Griefed/ServerPackCreator)"

    private var playwright: Playwright? = null
    private var browser: Browser? = null

    /**
     * Fetch [modFile] into [targetDirectory] through the browser. Returns the saved jar, or `null`
     * when the file has no [ModFile.pageUrl] (nothing to drive) or the download could not be
     * captured. Never throws — a failure degrades to a deferred/unknown signal upstream.
     */
    override fun download(modFile: ModFile, targetDirectory: File): File? {
        val pageUrl = modFile.pageUrl ?: return null
        return try {
            Thread.sleep(requestDelayMillis)
            targetDirectory.mkdirs()
            val destination = File(targetDirectory, modFile.fileName)
            newPage().use { page ->
                // DOMCONTENTLOADED, not the default `load`: a CurseForge project page keeps fetching ads and
                // trackers long after it is usable, and waiting for `load` turns a working page into a timeout.
                page.navigate(pageUrl, navigationOptions())
                // CurseForge auto-initiates the file-download on the `/download` page; navigating
                // there inside waitForDownload captures the resulting transfer.
                val download: Download = page.waitForDownload(downloadOptions()) {
                    // The navigation is *expected* to be aborted -- it turns into a file transfer, and Chromium
                    // cancels a navigation that becomes a download. Letting that throw escape tore down the wait
                    // and discarded the very download it had just triggered. Anything else is a real failure and
                    // must still propagate.
                    runCatching {
                        page.navigate("$pageUrl/download", navigationOptions())
                    }.onFailure { navigationFailure ->
                        if (!isDownloadAbort(navigationFailure)) {
                            throw navigationFailure
                        }
                    }
                }
                download.saveAs(destination.toPath())
            }
            if (destination.isFile) destination else null
        } catch (ex: Exception) {
            log.warn("Browser-download of ${modFile.fileName} from $pageUrl failed: ${ex.message}")
            null
        }
    }

    /**
     * How both navigations are performed: wait for `DOMCONTENTLOADED`, never Playwright's default `load`.
     *
     * A CurseForge project page keeps fetching ads and trackers long after it is usable, so waiting for `load`
     * spends the entire budget on third parties and reports a page that works as a timeout. Extracted rather
     * than inlined so the choice is testable without a browser — it is made here, on the host, before Chromium
     * is involved at all.
     */
    internal fun navigationOptions(): Page.NavigateOptions = Page.NavigateOptions()
        .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
        .setTimeout(navigationTimeoutMillis)

    /** The download's own budget, kept equal to the navigation's so one slow page cannot be half-timed-out. */
    internal fun downloadOptions(): Page.WaitForDownloadOptions =
        Page.WaitForDownloadOptions().setTimeout(navigationTimeoutMillis)

    /** Open a fresh download-enabled page on the lazily-launched browser. */
    private fun newPage(): Page {
        val launched = browser ?: launchBrowser().also { browser = it }
        val context = launched.newContext(
            Browser.NewContextOptions().setUserAgent(userAgent).setAcceptDownloads(true)
        )
        return context.newPage()
    }

    /** Create the Playwright runtime and a headless Chromium on first use. */
    private fun launchBrowser(): Browser {
        val runtime = playwright ?: Playwright.create().also { playwright = it }
        log.info("Launching headless Chromium for distribution-locked downloads...")
        return runtime.chromium().launch(BrowserType.LaunchOptions().setHeadless(true))
    }

    companion object {
        /**
         * Whether [failure] is Chromium cancelling a navigation because it became a download, rather than a
         * navigation that genuinely failed. Both wordings Playwright uses for the event are recognised; a
         * timeout or a DNS failure is not one of them, and must keep failing the download.
         */
        @JvmStatic
        fun isDownloadAbort(failure: Throwable): Boolean {
            val message = failure.message ?: return false
            return message.contains("net::ERR_ABORTED", ignoreCase = true) ||
                message.contains("Download is starting", ignoreCase = true)
        }
    }

    /** Dispose the browser and Playwright runtime if they were started. */
    override fun close() {
        try {
            browser?.close()
            playwright?.close()
        } catch (ex: Exception) {
            log.debug("Error closing Playwright: ${ex.message}")
        } finally {
            browser = null
            playwright = null
        }
    }
}
