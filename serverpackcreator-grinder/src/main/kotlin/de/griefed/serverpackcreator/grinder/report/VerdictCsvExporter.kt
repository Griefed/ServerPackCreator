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

import de.griefed.serverpackcreator.grinder.GrindVerdict

/**
 * Renders verdicts as RFC-4180 CSV — the export behind the web table's "download CSV" action. Columns
 * mirror the table: project name (slug), link, the clientside-list name-pattern, the confidence,
 * which loader produced it, and the UTC date it was scanned on (see [ScanDate]). Rows are sorted
 * highest-confidence-then-name so the strongest clientside candidates lead; the web table re-sorts
 * client-side, this is just a deterministic default.
 *
 * @author Griefed
 */
object VerdictCsvExporter {

    /** The header row; also documents the column order callers (and the table) rely on. */
    private val header = VerdictField.entries.map { it.csvHeader }

    /**
     * Render [verdicts] as a CSV document (header + one row per verdict), with proper escaping.
     *
     * Rows are **not** re-sorted here when the caller has already ordered them: `/export.csv` runs the same
     * [VerdictSelection] the table does, so re-sorting would silently discard the reader's chosen order and
     * hand them a file that disagrees with the page it came from. An unordered call still gets the
     * highest-confidence-first default, which is what a direct `/export.csv` has always produced.
     */
    fun toCsv(verdicts: List<GrindVerdict>, preOrdered: Boolean = false): String {
        val ordered = if (preOrdered) {
            verdicts
        } else {
            // The table's own ordering, through the same key rather than a second rank table kept in step
            // by hand -- the two used to declare confidence order separately.
            verdicts.sortedWith(
                compareBy<GrindVerdict> { VerdictField.VERDICT.sortKey(it) }
                    .thenBy { it.slug }
                    // Newest era first inside a project: the line a pack is most likely being built on
                    // leads, and the loader stays the last tie-break because a legacy row carries no line.
                    .thenByDescending { VerdictField.MINECRAFT.sortKey(it) }
                    .thenBy { it.loader }
            )
        }
        // Cells come from the same VerdictField list the table renders from, so the two cannot describe
        // different columns -- the drift that had the CSV carrying seven fields against the table's eight.
        val rows = ordered.map { verdict -> VerdictField.entries.map { it.text(verdict) } }
        return (listOf(header) + rows).joinToString("\n") { fields -> fields.joinToString(",") { escape(it) } }
    }

    /**
     * Escape one field per RFC-4180: wrap in double-quotes when it contains a comma, quote, CR or LF,
     * doubling any embedded quote. Plain fields are emitted verbatim.
     */
    private fun escape(field: String): String =
        if (field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else {
            field
        }
}
