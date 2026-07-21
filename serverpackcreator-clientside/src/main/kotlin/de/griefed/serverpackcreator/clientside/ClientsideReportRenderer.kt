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

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

/**
 * Renders a [ClientsideReport] into the Markdown body posted as the issue-comment. The output is
 * self-contained — the verify-workflow posts it verbatim — and carries a hidden marker (so the
 * comment can be updated in place) plus a hidden JSON block (so the acceptance-workflow can read the
 * suggested entries back without re-deriving them).
 *
 * @author Griefed
 */
object ClientsideReportRenderer {

    /** Hidden HTML-comment marker identifying the sticky report-comment for in-place updates. */
    const val MARKER = "<!-- serverpackcreator-clientside-report -->"

    /** Pretty-printing JSON writer for the embedded machine-readable report-block. */
    private val jsonWriter = jacksonObjectMapper().writerWithDefaultPrettyPrinter()

    /** Build the full Markdown comment for [report]. */
    fun renderMarkdown(report: ClientsideReport): String {
        val builder = StringBuilder()
        builder.appendLine(MARKER)
        builder.appendLine("## 🤖 Clientside-mod verification — `${report.slug}` (${report.platform})")
        builder.appendLine()
        builder.appendLine("Automated assessment (**${report.phase}**). A maintainer should review the evidence below, correct the entries if needed, then add the `accepted` label to open a PR.")
        builder.appendLine()

        builder.appendLine("### Suggested clientside-list entries")
        if (report.suggestedEntries.isEmpty()) {
            builder.appendLine("_None could be derived — see the file-names below and add an entry manually._")
        } else {
            builder.appendLine("```")
            report.suggestedEntries.forEach { builder.appendLine(it) }
            builder.appendLine("```")
        }
        builder.appendLine()

        builder.appendLine("### Per-loader confidence")
        builder.appendLine("| Loader | Suggested entry | Declared (client/server) | Jar scan | Boot | Confidence |")
        builder.appendLine("|---|---|---|---|---|---|")
        for (verdict in report.perLoader) {
            builder.appendLine(
                "| ${verdict.loader} | ${code(verdict.suggestedEntry)} | " +
                        "${verdict.declaredClientSide} / ${verdict.declaredServerSide} | " +
                        "${verdict.jarScan} | ${verdict.bootResult ?: "—"} | ${badge(verdict.confidence)} |"
            )
        }
        builder.appendLine()

        // Surface the crash-output inline so a maintainer can judge *why* the server died — a crash
        // raises confidence but is no proof of clientside-only-ness.
        for (verdict in report.perLoader) {
            val excerpt = verdict.bootCrashExcerpt ?: continue
            builder.appendLine("<details><summary>💥 ${verdict.loader} server crash output</summary>")
            builder.appendLine()
            builder.appendLine("```")
            builder.appendLine(excerpt)
            builder.appendLine("```")
            builder.appendLine("</details>")
            builder.appendLine()
        }

        val notes = report.perLoader.mapNotNull { it.note }.distinct()
        if (notes.isNotEmpty()) {
            builder.appendLine("> [!NOTE]")
            notes.forEach { builder.appendLine("> - $it") }
            builder.appendLine()
        }

        builder.appendLine("<details><summary>All ${report.fileNames.size} published file-names</summary>")
        builder.appendLine()
        builder.appendLine("```")
        report.fileNames.forEach { builder.appendLine(it) }
        builder.appendLine("```")
        builder.appendLine("</details>")
        builder.appendLine()

        builder.appendLine("<!-- clientside-report-data")
        builder.appendLine(jsonWriter.writeValueAsString(report))
        builder.appendLine("-->")
        return builder.toString()
    }

    /** Wrap a non-null entry in inline-code, or render an em-dash for a missing one. */
    private fun code(value: String?): String = if (value.isNullOrBlank()) "—" else "`$value`"

    /** Decorate a confidence with an emoji so a maintainer can triage at a glance. */
    private fun badge(confidence: Confidence): String = when (confidence) {
        Confidence.HIGH -> "🟢 **HIGH**"
        Confidence.MEDIUM -> "🟡 MEDIUM"
        Confidence.LOW -> "🔴 LOW"
        Confidence.INCONCLUSIVE -> "⚪ INCONCLUSIVE"
    }
}
