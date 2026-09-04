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

import de.griefed.serverpackcreator.clientside.Declaration
import de.griefed.serverpackcreator.clientside.Verdict
import de.griefed.serverpackcreator.grinder.GrindVerdict
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

    /** Render as the server does: through the same selection the live table and the CSV export both run. */
    private fun pageOf(verdicts: List<GrindVerdict>, rawQuery: String? = null) =
        VerdictSelection.select(verdicts, VerdictQuery.parse(QueryParams.parse(rawQuery), VerdictQuery.DEFAULT_PAGE_SIZE))


    @Test
    fun rendersSortableHeadersAndADataRow() {
        val html = VerdictReportRenderer.toHtml(
            pageOf(listOf(grindVerdict("jei", "Forge", suggestedEntry = "jei-", verdict = Verdict.CONFIRMED)))
        )
        Assertions.assertTrue(html.contains("<table"), "needs a table")
        Assertions.assertTrue(html.contains("""href="/?sort=name"""), "a header must link its sorted view")
        Assertions.assertFalse(html.contains("function sortBy("), "sorting is server-side now, not a DOM sort")
        Assertions.assertTrue(html.contains(">jei<"), "the project name")
        Assertions.assertTrue(html.contains(">jei-<"), "the clientside-list name-pattern column")
        Assertions.assertTrue(html.contains(">CONFIRMED<"), "the verdict")
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
            pageOf(listOf(grindVerdict("jei", "Forge", verifiedAt = Instant.parse("2026-08-23T19:41:13Z"))))
        )
        Assertions.assertTrue(html.contains(">Scanned (UTC)<"), "the column needs a header")
        Assertions.assertTrue(html.contains(">2026/08/23<"), "the scan date must be in the row: $html")
    }

    /** A date early in the year must stay zero-padded, or the column sorts as text in the wrong order. */
    @Test
    fun zeroPadsTheScanDate() {
        val html = VerdictReportRenderer.toHtml(
            pageOf(listOf(grindVerdict("jei", "Forge", verifiedAt = Instant.parse("2026-01-05T00:00:00Z"))))
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
            pageOf(listOf(grindVerdict("jei", "Forge"), grindVerdict("sodium", "Fabric")))
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
        val html = VerdictReportRenderer.toHtml(pageOf(listOf(grindVerdict("jei", "Forge", suggestedEntry = "jei-"))))

        listOf("/export.csv", "/status", "/as-properties", "/boot-logs").forEach { endpoint ->
            Assertions.assertTrue(
                html.contains("""href="$endpoint"""),
                "the overview must offer $endpoint; it was only ever in a startup log line"
            )
        }
    }

    /**
     * Logs are offered only where some are actually kept, so the table never points at a 404 — which is why
     * the renderer asks a lookup per row instead of trusting a field that a deleted file would strand.
     *
     * A row now carries *many*: the console, the server's own logs and its crash reports, once per attempt.
     * They go behind a `<details>` disclosure so a heavily re-checked row cannot dominate the table, and the
     * link labels drop the tuple prefix every name in the cell shares — what differs is the attempt and the
     * artifact, which is what a reader is choosing between.
     */
    @Test
    fun onlyARowWithKeptLogsGetsLinks() {
        val crashed = grindVerdict("creativecore", "Fabric", verdict = Verdict.CONFIRMED)
        val clean = grindVerdict("jei", "Forge", verdict = Verdict.ERROR)
        val kept = listOf(
            "Modrinth-creativecore-Fabric~Fabric_0.19.3_mc26.2~console.log",
            "Modrinth-creativecore-Fabric~Fabric_0.19.3_mc26.2~logs-latest.log"
        )

        val html = VerdictReportRenderer.toHtml(pageOf(listOf(crashed, clean))) { verdict ->
            if (verdict.slug == "creativecore") kept else emptyList()
        }

        Assertions.assertEquals(2, Regex("/boot-log\\?name=").findAll(html).count(), "one link per kept artifact, and no more")
        Assertions.assertTrue(html.contains("<details><summary>2 log(s)</summary>"), "collapsed, so a re-checked row stays readable")
        Assertions.assertTrue(
            html.contains(">Fabric_0.19.3_mc26.2~console.log<"),
            "the label drops the tuple prefix every name in the cell shares"
        )
        Assertions.assertTrue(html.contains("&mdash;"), "a row with nothing kept says so rather than linking a 404")
    }

    @Test
    fun theDownloadButtonLinksTheFilteredCsvExport() {
        val html = VerdictReportRenderer.toHtml(
            pageOf(listOf(grindVerdict("jei", "Forge", suggestedEntry = "jei-")), "f.verdict=HIGH&sort=name")
        )

        Assertions.assertTrue(html.contains("/export.csv?"), "the button links the export rather than embedding it")
        Assertions.assertTrue(html.contains("f.verdict=HIGH"), "carrying the current filter")
        Assertions.assertTrue(html.contains("sort=name"), "and the current sort")
        // Pins the REMOVAL. An embedded copy would silently disagree with a filtered export, and dropping it
        // also stops the page putting mod-supplied text inside a <script> block at all.
        Assertions.assertFalse(html.contains("function downloadCsv("), "no embedded-CSV download hook")
        Assertions.assertFalse(html.contains("const CSV ="), "no embedded CSV literal")
    }

    @Test
    fun escapesModSuppliedStringsToPreventInjection() {
        val html = VerdictReportRenderer.toHtml(
            pageOf(listOf(grindVerdict("x", "Forge", detail = "<script>alert(1)</script>")))
        )
        Assertions.assertFalse(html.contains("<script>alert(1)</script>"), "raw markup must not survive into a table cell")
        Assertions.assertTrue(html.contains("&lt;script&gt;alert(1)&lt;/script&gt;"), "it must be HTML-escaped")
    }

    /**
     * **The guard counting cannot give you.** `everyHeaderHasACellBeneathIt` proves the *number* of cells
     * matches the number of headers, which is exactly what an off-by-one preserves: drop a column and add
     * another and every cell past the gap silently shows its neighbour's data, with the count still right.
     * This gives each field a distinct sentinel and asserts cell *i* carries column *i*'s.
     *
     * It lands green rather than red on purpose: it is a characterization guard over correct-but-fragile
     * code, put in place to protect the restructure that follows. "Red first" applies to tests for new
     * behaviour, which this is not.
     */
    @Test
    fun everyColumnRendersTheValueItsHeaderNames() {
        val verdict = grindVerdict(
            slug = "SENTINELNAME",
            loader = "SENTINELLOADER",
            suggestedEntry = "SENTINELPATTERN",
            projectUrl = "https://example.invalid/SENTINELPROJECT",
            detail = "SENTINELDETAIL", verdict = Verdict.CONFIRMED).copy(
            declared = Declaration.SERVER,
            firedRule = "SENTINELRULE",
            stagedDependencies = listOf("SENTINELDEP"),
            decidedBy = "SENTINELDECISION"
        )

        val row = VerdictReportRenderer.toHtml(pageOf(listOf(verdict))) { listOf("SENTINELLOG") }
            .substringAfter("<tbody").substringAfter("<tr>").substringBefore("</tr>")
        val cells = row.split("</td>").dropLast(1)

        val expected = listOf(
            "SENTINELNAME", "SENTINELPROJECT", "SENTINELPATTERN", "CONFIRMED", "SERVER", "SENTINELLOADER",
            "Modrinth", "not recorded", "not recorded",
            "SENTINELDETAIL", "SENTINELRULE", "SENTINELDECISION", "SENTINELDEP", "1970", "SENTINELLOG"
        )
        Assertions.assertEquals(expected.size, cells.size, "one sentinel per column; got ${cells.size} cells")
        expected.forEachIndexed { index, sentinel ->
            Assertions.assertTrue(
                cells[index].contains(sentinel),
                "cell $index should carry '$sentinel' but was: ${cells[index]}"
            )
        }
    }

    /**
     * The HTML table and the CSV are two renderings of one column list, and they have drifted before — the
     * CSV header carried seven fields while the table carried eight for a long time. This pins that they
     * describe the same number of data columns, so the divergence cannot silently re-open.
     */
    @Test
    fun theCsvAndTheTableAgreeOnTheirDataColumns() {
        val html = VerdictReportRenderer.toHtml(pageOf(listOf(grindVerdict("jei", "Forge"))))
        val headerCount = Regex("<th[ >]").findAll(html).count()
        val csvColumnCount = VerdictCsvExporter.toCsv(emptyList()).split(",").size

        Assertions.assertEquals(
            headerCount - 1, csvColumnCount,
            "the table has exactly one column the CSV does not: Logs, which is a set of links rather than a value"
        )
    }
}
