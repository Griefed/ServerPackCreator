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
package de.griefed.serverpackcreator.grinder.report

import de.griefed.serverpackcreator.clientside.Confidence
import de.griefed.serverpackcreator.grinder.grindVerdict
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Pins the HTML report: clickable column headers (the sort hook), a data row carrying the
 * name-pattern + a project link, the embedded CSV behind the download button, and HTML-escaping of
 * mod-supplied strings so a malicious name/detail can't inject markup.
 */
internal class VerdictReportRendererTest {

    @Test
    fun rendersSortableHeadersAndADataRow() {
        val html = VerdictReportRenderer.toHtml(
            listOf(grindVerdict("jei", "Forge", confidence = Confidence.HIGH, suggestedEntry = "jei-"))
        )
        Assertions.assertTrue(html.contains("<table"), "needs a table")
        Assertions.assertTrue(html.contains("""onclick="sortBy(0)""""), "headers must be click-to-sort")
        Assertions.assertTrue(html.contains("function sortBy("), "needs the sort script")
        Assertions.assertTrue(html.contains(">jei<"), "the project name")
        Assertions.assertTrue(html.contains(">jei-<"), "the clientside-list name-pattern column")
        Assertions.assertTrue(html.contains(">HIGH<"), "the confidence")
        Assertions.assertTrue(html.contains("""href="https://modrinth.com/mod/jei""""), "a link to the project")
    }

    /**
     * When the mod was scanned, as `YEAR/MM/DD`.
     *
     * The store has carried `verifiedAt` all along — it is what the re-verify TTL compares against — but the
     * overview never showed it, so a reader could not tell a verdict reached minutes ago from one reached weeks
     * ago on a loader build long since superseded. Rendered in **UTC** so the same store reads the same on any
     * host, and zero-padded so the column sorts correctly as text under the table's own sort.
     */
    @Test
    fun showsWhenTheModWasScanned() {
        val html = VerdictReportRenderer.toHtml(
            listOf(grindVerdict("jei", "Forge", verifiedAt = Instant.parse("2026-08-23T19:41:13Z")))
        )
        Assertions.assertTrue(html.contains(">Scanned (UTC)<"), "the column needs a header")
        Assertions.assertTrue(html.contains(">2026/08/23<"), "the scan date must be in the row: $html")
    }

    /** A date early in the year must stay zero-padded, or the column sorts as text in the wrong order. */
    @Test
    fun zeroPadsTheScanDate() {
        val html = VerdictReportRenderer.toHtml(
            listOf(grindVerdict("jei", "Forge", verifiedAt = Instant.parse("2026-01-05T00:00:00Z")))
        )
        Assertions.assertTrue(html.contains(">2026/01/05<"), "expected a padded date: $html")
    }

    /**
     * Every header must have a cell under it.
     *
     * `columns` and `rowHtml`'s cell list are two hand-maintained lists that have to stay the same length, and
     * nothing checked it. Add a header without its cell (or the reverse) and the table still renders: every
     * column past the gap shows the neighbouring column's data, and `sortBy(index)` — wired from the header's
     * position — sorts by the wrong one. No existing guard notices, because each of them looks for one value
     * somewhere in the page. This branch incremented both lists, which is exactly when the two drift.
     *
     * Counted off the rendered page rather than the two lists, so it pins the consequence and not the source.
     */
    @Test
    fun everyHeaderHasACellBeneathIt() {
        val html = VerdictReportRenderer.toHtml(
            listOf(grindVerdict("jei", "Forge"), grindVerdict("sodium", "Fabric"))
        )
        val headers = Regex("<th[ >]").findAll(html).count()
        val bodyRows = html.substringAfter("<tbody>").substringBefore("</tbody>").trim().lines()

        Assertions.assertEquals(2, bodyRows.size, "test setup: one row per verdict")
        bodyRows.forEach { row ->
            Assertions.assertEquals(
                headers,
                Regex("<td[ >]").findAll(row).count(),
                "a row must carry exactly one cell per header ($headers): $row"
            )
        }
    }

    /**
     * The overview is the only page an operator ever opens, and the daemon's other endpoints were reachable
     * only from a log line printed at startup. Each one now has a button beside "Download CSV".
     */
    @Test
    fun linksEveryEndpointBesideTheDownloadButton() {
        val html = VerdictReportRenderer.toHtml(listOf(grindVerdict("jei", "Forge", suggestedEntry = "jei-")))

        listOf("/export.csv", "/status", "/as-properties", "/crash-logs").forEach { endpoint ->
            Assertions.assertTrue(
                html.contains("""href="$endpoint""""),
                "the overview must offer $endpoint; it was only ever in a startup log line"
            )
        }
    }

    /**
     * A crash log is offered only where one is actually kept, so the table never points at a 404 — which is
     * why the renderer asks a lookup per row instead of trusting a field that a deleted file would strand.
     */
    @Test
    fun onlyARowWithAKeptCrashLogGetsALink() {
        val crashed = grindVerdict("creativecore", "Fabric", confidence = Confidence.HIGH)
        val clean = grindVerdict("jei", "Forge", confidence = Confidence.LOW)

        val html = VerdictReportRenderer.toHtml(listOf(crashed, clean)) { verdict ->
            "Modrinth-creativecore-Fabric.log".takeIf { verdict.slug == "creativecore" }
        }

        Assertions.assertTrue(
            html.contains("""href="/crash-log?name=Modrinth-creativecore-Fabric.log""""),
            "the crashing row must link its console"
        )
        Assertions.assertEquals(1, Regex("/crash-log\\?name=").findAll(html).count(), "and only that row")
    }

    @Test
    fun embedsTheCsvForTheDownloadButton() {
        val html = VerdictReportRenderer.toHtml(listOf(grindVerdict("jei", "Forge", suggestedEntry = "jei-")))
        Assertions.assertTrue(html.contains("function downloadCsv("), "needs the CSV download hook")
        // The CSV is embedded as a JS string literal; its header and the name-pattern must be present.
        Assertions.assertTrue(html.contains("Name,Project,NamePattern,Confidence,Loader,Detail,Scanned"), "embedded CSV header")
        Assertions.assertTrue(html.contains("jei-"), "embedded CSV row")
    }

    @Test
    fun escapesModSuppliedStringsToPreventInjection() {
        val html = VerdictReportRenderer.toHtml(
            listOf(grindVerdict("x", "Forge", detail = "<script>alert(1)</script>"))
        )
        Assertions.assertFalse(html.contains("<script>alert(1)</script>"), "raw markup must not survive into a table cell")
        Assertions.assertTrue(html.contains("&lt;script&gt;alert(1)&lt;/script&gt;"), "it must be HTML-escaped")
    }
}
