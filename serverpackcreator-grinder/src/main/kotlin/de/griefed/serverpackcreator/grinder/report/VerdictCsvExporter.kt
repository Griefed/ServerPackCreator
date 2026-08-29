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
    private val header = listOf("Name", "Project", "NamePattern", "Confidence", "Loader", "Detail", "Rule", "Dependencies", "Scanned")

    /** Confidence ordering for the default sort: strongest clientside signal first. */
    private val confidenceRank = mapOf(
        de.griefed.serverpackcreator.clientside.Confidence.HIGH to 0,
        de.griefed.serverpackcreator.clientside.Confidence.MEDIUM to 1,
        de.griefed.serverpackcreator.clientside.Confidence.LOW to 2,
        de.griefed.serverpackcreator.clientside.Confidence.INCONCLUSIVE to 3
    )

    /** Render [verdicts] as a CSV document (header + one row per verdict), with proper escaping. */
    fun toCsv(verdicts: List<GrindVerdict>): String {
        val ordered = verdicts.sortedWith(
            compareBy({ confidenceRank[it.confidence] ?: Int.MAX_VALUE }, { it.slug }, { it.loader })
        )
        val rows = ordered.map { verdict ->
            listOf(
                verdict.slug,
                verdict.projectUrl,
                verdict.suggestedEntry ?: "",
                verdict.confidence.name,
                verdict.loader,
                verdict.detail,
            // Machine-readable, so "how many verdicts did this rule decide?" is a question the CSV answers.
            verdict.firedRule ?: "",
            // Published jar names, so a verdict can be reproduced with the exact pack that produced it.
            verdict.stagedDependencies.joinToString(" "),
                ScanDate.of(verdict.verifiedAt)
            )
        }
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
