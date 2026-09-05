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

import de.griefed.serverpackcreator.clientside.AttemptDirectory
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
internal object VerdictReportRenderer {

    /**
     * Build the full HTML document for [page].
     *
     * [logLinks] answers, per verdict, every kept artifact to link. It is a *lookup* rather than a field on
     * [GrindVerdict] on purpose: the files live on disk under [BootLogStore], so asking at render time means
     * a link appears exactly when a file is there, and one removed by hand cannot strand a 404.
     *
     * **Ask it for the rows being rendered, never for the whole store** — it is one directory listing per
     * row, so applying it before paging would cost a lookup per verdict on every page load.
     */
    fun toHtml(page: VerdictPage, logLinks: (GrindVerdict) -> List<String> = { emptyList() }): String {
        val query = page.query
        val headerCells = VerdictField.entries.joinToString("") { field ->
            headerCell(SortKey.Column(field), field.header, query)
        } + headerCell(SortKey.Logs, SortKey.Logs.HEADER, query)
        val filterCells = VerdictField.entries.joinToString("") { field -> filterCell(field, page) } +
            """<td></td>"""
        val bodyRows = page.rows.joinToString("\n") { rowHtml(it, logLinks(it)) }
        val hidden = buildString {
            query.sort?.let { append("""<input type="hidden" name="sort" value="${esc(it.param)}">""") }
            if (query.descending) append("""<input type="hidden" name="dir" value="desc">""")
            query.size?.takeIf { it != VerdictQuery.DEFAULT_PAGE_SIZE }
                ?.let { append("""<input type="hidden" name="size" value="$it">""") }
                ?: run { if (query.size == null) append("""<input type="hidden" name="size" value="all">""") }
        }

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
                th, td { border: 1px solid #ccc; padding: 4px 8px; text-align: left; vertical-align: top; }
                th { background: #f3f3f3; user-select: none; }
                th a { color: inherit; text-decoration: none; }
                tr:nth-child(even) td { background: #fafafa; }
                .filters td { background: #fff; padding: 2px 4px; }
                .filters input, .filters select { font: inherit; width: 100%; box-sizing: border-box; }
                .toolbar { display: flex; flex-wrap: wrap; gap: .5rem; align-items: center; margin-bottom: 1rem; }
                .toolbar button, .toolbar .btn {
                  font: inherit; padding: .35rem .75rem; border: 1px solid #bbb; border-radius: 4px;
                  background: #f3f3f3; color: inherit; text-decoration: none; cursor: pointer;
                }
                .toolbar button:hover, .toolbar .btn:hover { background: #e6e6e6; }
                .pager { margin-top: 1rem; display: flex; gap: .5rem; align-items: center; flex-wrap: wrap; }
              </style>
            </head>
            <body>
              <h1>Suspected clientside mods (${page.matched} of ${page.total})</h1>
              <nav class="toolbar">
                <a class="btn" href="/export.csv${csvQuery(query)}">Download CSV (${page.matched} rows)</a>
                <a class="btn" href="/dashboard">Live status</a>
                <a class="btn" href="/status">Status JSON</a>
                <a class="btn" href="/as-properties">Fallback list</a>
                <a class="btn" href="/boot-logs">Boot logs</a>
              </nav>
              <form method="get" action="/">
                $hidden
                <nav class="toolbar">
                  <input type="search" name="q" placeholder="Search every column"
                         value="${esc(query.search ?: "")}">
                  <button type="submit">Apply</button>
                  <a class="btn" href="/">Clear</a>
                  ${sizeSelect(page)}
                </nav>
                <table id="verdicts">
                  <thead>
                    <tr>$headerCells</tr>
                    <tr class="filters">$filterCells</tr>
                  </thead>
                  <tbody>
            $bodyRows
                  </tbody>
                </table>
              </form>
              ${pager(page)}
            </body>
            </html>
        """.trimIndent()
    }

    /**
     * One sortable `<th>`: a header linking to this same view ordered by its column, toggling direction when
     * it is already the sort.
     *
     * Takes the [key] and its [header] rather than a [VerdictField], because **Logs** is sortable without
     * being one — it is rendered from a directory listing, not from the verdict.
     *
     * **The page resets to 1**: keeping it would land the reader on page 40 of a different ordering, which
     * is not where they were.
     */
    private fun headerCell(key: SortKey, header: String, query: VerdictQuery): String {
        val descending = query.sort == key && !query.descending
        val target = query.copy(sort = key, descending = descending, page = 1)
        val marker = if (query.sort == key) (if (query.descending) " ▾" else " ▴") else ""
        return """<th><a href="/${esc(target.toQueryString())}">${esc(header)}$marker</a></th>"""
    }

    /**
     * One filter control per column: a `<select>` of the values actually present for a CHOICE column, a
     * text box for the rest. Plain form controls, so filtering needs **no JavaScript at all** — submitting
     * *is* the URL update, which is what makes every view bookmarkable by construction.
     */
    private fun filterCell(field: VerdictField, page: VerdictPage): String {
        val current = page.query.filters[field]?.firstOrNull().orEmpty()
        val name = "f.${field.param}"
        if (field.filter == FilterKind.TEXT) {
            return """<td><input type="text" name="${esc(name)}" value="${esc(current)}"></td>"""
        }
        val options = page.choices[field].orEmpty().joinToString("") { value ->
            val selected = if (value.equals(current, ignoreCase = true)) " selected" else ""
            """<option value="${esc(value)}"$selected>${esc(value)}</option>"""
        }
        return """<td><select name="${esc(name)}"><option value="">(any)</option>$options</select></td>"""
    }

    /** The page-size chooser, offering only sizes this result count justifies plus the one in force. */
    private fun sizeSelect(page: VerdictPage): String {
        val options = VerdictQuery.offeredSizes(page.matched, page.query.size).joinToString("") { size ->
            val value = size?.toString() ?: "all"
            val selected = if (size == page.query.size) " selected" else ""
            """<option value="$value"$selected>$value</option>"""
        }
        return """<label>Rows <select name="size" onchange="this.form.submit()">$options</select></label>"""
    }

    /** Previous/next links plus the position, all carrying the rest of the query. */
    private fun pager(page: VerdictPage): String {
        if (page.pages <= 1) {
            return ""
        }
        val previous = if (page.page > 1) {
            """<a class="btn" href="/${esc(page.query.copy(page = page.page - 1).toQueryString())}">← Previous</a>"""
        } else {
            ""
        }
        val next = if (page.page < page.pages) {
            """<a class="btn" href="/${esc(page.query.copy(page = page.page + 1).toQueryString())}">Next →</a>"""
        } else {
            ""
        }
        return """<nav class="pager">$previous<span>Page ${page.page} of ${page.pages}</span>$next</nav>"""
    }

    /**
     * The CSV link's query: the current filters and sort, but **every** matching row rather than this page.
     * A reader downloading "the filtered set" means all of it, and stating that in the URL is clearer than
     * making `/export.csv` special-case a missing size.
     */
    private fun csvQuery(query: VerdictQuery): String =
        esc(query.copy(page = 1, size = null).toQueryString())

    /**
     * One table row. Cells come from the same [VerdictField] list the headers do, so a column cannot be
     * added to one and forgotten in the other — the drift `everyHeaderHasACellBeneathIt` had to exist for.
     */
    private fun rowHtml(verdict: GrindVerdict, logNames: List<String>): String {
        val cells = VerdictField.entries.map { field ->
            if (field == VerdictField.PROJECT) {
                """<a href="${esc(verdict.projectUrl)}" rel="noopener noreferrer">${esc(verdict.projectUrl)}</a>"""
            } else {
                esc(field.text(verdict))
            }
        } + logsCell(verdict, logNames)
        return "<tr>" + cells.joinToString("") { "<td>$it</td>" } + "</tr>"
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

    private fun esc(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")

    /** Percent-encode a value bound for a query string, before it is HTML-escaped for the attribute. */
    private fun urlEncode(value: String): String =
        java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8)
}
