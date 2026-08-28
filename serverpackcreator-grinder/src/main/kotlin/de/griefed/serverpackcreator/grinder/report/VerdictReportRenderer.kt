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

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.griefed.serverpackcreator.clientside.AttemptDirectory
import de.griefed.serverpackcreator.clientside.Confidence
import de.griefed.serverpackcreator.grinder.GrindVerdict
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Renders the verdicts as a single **self-contained HTML page**: a table with click-to-sort columns
 * (vanilla JS, no framework) and a "Download CSV" button backed by the embedded [VerdictCsvExporter]
 * output. Self-contained on purpose — the standalone grinder has no web stack, so [ReportServer] just
 * hands this string back over the JDK HTTP server; it also works opened straight from disk.
 *
 * @author Griefed
 */
object VerdictReportRenderer {

    /** Column headers, in the order the rows below emit their cells. */
    private val columns =
        listOf("Name", "Project", "Name-pattern", "Confidence", "Loader", "Detail", "Logs", "Scanned (UTC)")

    /** Default order: strongest clientside signal first, then by name — matches the CSV export. */
    private val confidenceRank = mapOf(
        Confidence.HIGH to 0, Confidence.MEDIUM to 1, Confidence.LOW to 2, Confidence.INCONCLUSIVE to 3
    )

    /**
     * Build the full HTML document for [verdicts].
     *
     * [logNames] answers, per verdict, every kept artifact to link — the console, the server's own logs and
     * its crash reports, once per boot attempt. It is a *lookup* rather than a field on [GrindVerdict] on
     * purpose: the files live on disk under [BootLogStore], so asking at render time means a link appears
     * exactly when a file is there, and one removed by hand cannot strand the table pointing at a 404.
     * Defaults to "no logs anywhere", which keeps the page renderable — and openable straight from disk —
     * with no store wired at all.
     *
     * **Ask it for the rows being rendered, never for the whole store**: it is one directory listing per
     * row, so applying it before paging would cost a lookup per verdict on every page load.
     */
    fun toHtml(verdicts: List<GrindVerdict>, logNames: (GrindVerdict) -> List<String> = { emptyList() }): String {
        val ordered = verdicts.sortedWith(
            compareBy({ confidenceRank[it.confidence] ?: Int.MAX_VALUE }, { it.slug }, { it.loader })
        )
        val headerCells = columns.mapIndexed { index, name -> """<th onclick="sortBy($index)">${esc(name)}</th>""" }.joinToString("")
        val bodyRows = ordered.joinToString("\n") { rowHtml(it, logNames(it)) }
        // jackson yields a valid JS string literal (quotes/newlines escaped); additionally escape
        // <, > and & to their \uXXXX form so a mod-supplied "</script>" can't break out of the script
        // block (jackson does not escape these by default).
        val csvLiteral = jacksonObjectMapper().writeValueAsString(VerdictCsvExporter.toCsv(verdicts))
            .replace("<", "\\u003c")
            .replace(">", "\\u003e")
            .replace("&", "\\u0026")

        return """
            <!doctype html>
            <html lang="en">
            <head>
              <meta charset="utf-8">
              <title>ServerPackCreator — suspected clientside mods</title>
              <link rel="icon" type="image/png" href="/favicon.png">
              <style>
                body { font-family: system-ui, sans-serif; margin: 1.5rem; }
                table { border-collapse: collapse; width: 100%; }
                th, td { border: 1px solid #ccc; padding: 4px 8px; text-align: left; }
                th { cursor: pointer; background: #f3f3f3; user-select: none; }
                tr:nth-child(even) td { background: #fafafa; }
                .toolbar { display: flex; flex-wrap: wrap; gap: .5rem; align-items: center; margin-bottom: 1rem; }
                .toolbar button, .toolbar .btn {
                  font: inherit; padding: .35rem .75rem; border: 1px solid #bbb; border-radius: 4px;
                  background: #f3f3f3; color: inherit; text-decoration: none; cursor: pointer;
                }
                .toolbar button:hover, .toolbar .btn:hover { background: #e6e6e6; }
              </style>
            </head>
            <body>
              <h1>Suspected clientside mods (${ordered.size})</h1>
              <nav class="toolbar">
                <button onclick="downloadCsv()">Download CSV</button>
                <a class="btn" href="/export.csv">CSV endpoint</a>
                <a class="btn" href="/status">Live status</a>
                <a class="btn" href="/as-properties">Fallback list</a>
                <a class="btn" href="/boot-logs">Boot logs</a>
              </nav>
              <table id="verdicts">
                <thead><tr>$headerCells</tr></thead>
                <tbody>
            $bodyRows
                </tbody>
              </table>
              <script>
                const CSV = $csvLiteral;
                function downloadCsv() {
                  const url = URL.createObjectURL(new Blob([CSV], { type: "text/csv" }));
                  const a = document.createElement("a");
                  a.href = url; a.download = "clientside-mods.csv"; a.click();
                  URL.revokeObjectURL(url);
                }
                function sortBy(col) {
                  const tbody = document.querySelector("#verdicts tbody");
                  const rows = Array.from(tbody.rows);
                  const dir = tbody.dataset.sortCol == col && tbody.dataset.sortDir == "asc" ? "desc" : "asc";
                  rows.sort((a, b) => {
                    const x = a.cells[col].innerText, y = b.cells[col].innerText;
                    return (dir == "asc" ? 1 : -1) * x.localeCompare(y, undefined, { numeric: true });
                  });
                  rows.forEach(r => tbody.appendChild(r));
                  tbody.dataset.sortCol = col; tbody.dataset.sortDir = dir;
                }
              </script>
            </body>
            </html>
        """.trimIndent()
    }

    /**
     * The Logs cell: a `<details>` disclosure over every artifact kept for this verdict's tuple, collapsed
     * so a row with forty logs does not dominate the table. Native HTML, so it needs no JavaScript and still
     * works in a page opened straight off disk.
     *
     * The link label drops the tuple prefix every name in the cell shares, leaving the attempt and the
     * artifact — which is the part that differs and the part a reader is choosing between.
     */
    private fun logsCell(verdict: GrindVerdict, logNames: List<String>): String {
        if (logNames.isEmpty()) {
            return "&mdash;"
        }
        val prefix = AttemptDirectory.nameFor(verdict.platform, verdict.slug, verdict.loader) +
            BootLogStore.ATTEMPT_SEPARATOR
        val links = logNames.sorted().joinToString("") { name ->
            val label = name.removePrefix(prefix)
            """<a href="/boot-log?name=${esc(urlEncode(name))}">${esc(label)}</a><br>"""
        }
        return "<details><summary>${logNames.size} log(s)</summary>$links</details>"
    }

    /**
     * One table row; the Name links to the project, the crash-console cell links the kept log when
     * [crashLogName] names one, the last cell says when the mod was scanned, and every cell is HTML-escaped.
     *
     * The crash console is the cell that answers *why* a HIGH was reached — most often a server loading a mod
     * that reaches for a client-only class — which the Detail column can only summarise.
     */
    private fun rowHtml(verdict: GrindVerdict, logNames: List<String>): String {
        val logs = logsCell(verdict, logNames)
        val cells = listOf(
            esc(verdict.slug),
            """<a href="${esc(verdict.projectUrl)}" rel="noopener noreferrer">${esc(verdict.projectUrl)}</a>""",
            esc(verdict.suggestedEntry ?: ""),
            esc(verdict.confidence.name),
            esc(verdict.loader),
            esc(verdict.detail),
            logs,
            // Last, because it is the one column whose width never changes — and the sort works on it as text.
            // Escaped like every other cell even though a `yyyy/MM/dd` string cannot contain markup: the
            // uniformity is what makes the *next* cell safe to add, and one exception is a trap for whoever
            // adds it.
            esc(ScanDate.of(verdict.verifiedAt))
        )
        return "<tr>" + cells.joinToString("") { "<td>$it</td>" } + "</tr>"
    }

    /**
     * Percent-encode a crash-log name for the `?name=` query. Names are built from a platform, a slug and a
     * loader, and a slug is whatever the platform allows — so this must not assume the name is already safe
     * for a URL, even though the store refuses anything with a separator when it reads it back.
     */
    private fun urlEncode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8)

    /** Escape a value for HTML text/attribute context so mod-supplied strings can't break the page. */
    private fun esc(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
}
