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
import de.griefed.serverpackcreator.clientside.verdict.ClientsideReport
import de.griefed.serverpackcreator.clientside.verdict.GrindTargetVerdict
import de.griefed.serverpackcreator.clientside.verdict.Verdict

/**
 * Renders a [ClientsideReport] into the Markdown body posted as the issue-comment. The output is
 * self-contained — the verify-workflow posts it verbatim — and carries a hidden marker (so the
 * comment can be updated in place) plus a hidden JSON block (so the acceptance-workflow can read the
 * suggested entries back without re-deriving them), and always fits into one comment.
 *
 * @author Griefed
 */
object ClientsideReportRenderer {

    /** Hidden HTML-comment marker identifying the sticky report-comment for in-place updates. */
    const val MARKER = "<!-- serverpackcreator-clientside-report -->"

    /**
     * The most this renderer writes. GitHub refuses a comment body over 65,536 characters, and a refused report
     * reaches nobody; the difference is room for the evidence block the workflow inserts.
     */
    const val MAX_COMMENT_CHARS = 60_000

    /** A console line is cut to this length, so one runaway line cannot take a boot's whole share. */
    const val MAX_CONSOLE_LINE_CHARS = 400

    /** Where `-verifyclientside` keeps every attempt's evidence, as the boot-log section tells the reader. */
    const val CLI_EVIDENCE_NOTE = "Every attempt's full console, with the server's own logs and crash reports, is kept " +
        "under `work/clientside-verify/` in ServerPackCreator's home."

    /** The most console lines shown for one boot, however much room the comment has. */
    const val MAX_CONSOLE_LINES = 200

    /** A note is cut to this length once the comment has to shed its crash excerpts to fit. */
    const val MAX_NOTE_CHARS = 500

    /** Room kept for the line counting the notes that did not fit. */
    private const val OMITTED_NOTES_LINE_CHARS = 80

    /** Pretty-printing JSON writer for the embedded machine-readable report-block. */
    private val jsonWriter = jacksonObjectMapper().writerWithDefaultPrettyPrinter()

    /** A terminal escape sequence, which a server console uses for colour and a comment can only print as noise. */
    private val TERMINAL_ESCAPE = Regex("\u001B\\[[0-?]*[ -/]*[@-~]|\u001B")

    /**
     * Build the full Markdown comment for [report], with the end of each target's server console from
     * [consoleOf] — `null` or empty where none was recorded — and [evidenceNote] saying where the rest is kept.
     *
     * The consoles share whatever room the rest of the comment leaves under [MAX_COMMENT_CHARS]. A report too
     * large on its own sheds, in order, its file-name list and the data block's excerpts and notes, then the crash
     * excerpts above the table (the consoles show the same text), then the length of its notes -- never the
     * table or anything the acceptance workflow reads.
     */
    fun renderMarkdown(
        report: ClientsideReport,
        evidenceNote: String = CLI_EVIDENCE_NOTE,
        consoleOf: (GrindTargetVerdict) -> List<String>? = { null }
    ): String {
        val fullHead = head(report, withExcerpts = true, noteBudget = Int.MAX_VALUE)
        val fullTail = tail(report, compact = false)
        val tail = if (fullHead.length + fullTail.length <= MAX_COMMENT_CHARS) fullTail else tail(report, compact = true)
        val room = MAX_COMMENT_CHARS - tail.length
        val head = fullHead.takeIf { it.length <= room }
            ?: head(report, withExcerpts = false, noteBudget = Int.MAX_VALUE).takeIf { it.length <= room }
            ?: head(report, withExcerpts = false, noteBudget = room - head(report, withExcerpts = false, noteBudget = 0).length)
        return head + bootLogs(report, consoleOf, evidenceNote, MAX_COMMENT_CHARS - head.length - tail.length) + tail
    }

    /**
     * The comment for a verification that produced no report: the [MARKER], so it still replaces the sticky comment,
     * and [reason]. The CLI's report verbs and the grinder's `--clientside-report` all write it.
     */
    fun renderFailure(projectUrl: String, reason: String?): String =
        "$MARKER\n## 🤖 Clientside-mod verification failed\n\nCould not assess `$projectUrl`: $reason\n"

    /**
     * Everything above the boot logs: the marker, the entries, the table, crash excerpts and notes. The notes take at
     * most [noteBudget] characters; below [Int.MAX_VALUE] each is cut to [MAX_NOTE_CHARS] and those that do not fit
     * are counted instead of shown.
     */
    private fun head(report: ClientsideReport, withExcerpts: Boolean, noteBudget: Int): String {
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

        builder.appendLine("### Confidence per Minecraft version")
        builder.appendLine("| Minecraft | Loader | Suggested entry | Declared (client/server) | Jar scan | Boot | Verdict |")
        builder.appendLine("|---|---|---|---|---|---|---|")
        for (verdict in report.perTarget) {
            builder.appendLine(
                "| ${minecraftOf(verdict)} | ${verdict.loader} | ${code(verdict.suggestedEntry)} | " +
                        "${verdict.declaredClientSide} / ${verdict.declaredServerSide} | " +
                        "${verdict.jarScan} | ${bootCell(verdict)} | ${badge(verdict.verdict)} |"
            )
        }
        builder.appendLine()

        // Surface the crash-output inline so a maintainer can judge *why* the server died — a crash
        // raises confidence but is no proof of clientside-only-ness.
        val crashed = report.perTarget.filter { it.bootCrashExcerpt != null }
        if (withExcerpts) {
            for (verdict in crashed) {
                val excerpt = verdict.bootCrashExcerpt.orEmpty().lines().map(::printable)
                val fence = fenceFor(excerpt)
                builder.appendLine("<details><summary>💥 ${verdict.loader} · Minecraft ${minecraftOf(verdict)} server crash output</summary>")
                builder.appendLine()
                builder.appendLine(fence)
                excerpt.forEach { builder.appendLine(it) }
                builder.appendLine(fence)
                builder.appendLine("</details>")
                builder.appendLine()
            }
        } else if (crashed.isNotEmpty()) {
            builder.appendLine(
                "_The ${crashed.size} crash excerpts are left out to fit the comment's size limit; " +
                        "each crash's console is below._"
            )
            builder.appendLine()
        }

        builder.append(notes(report.perTarget.mapNotNull { it.note }.distinct(), noteBudget))
        return builder.toString()
    }

    /** The notes block within [budget] characters; see [head]. */
    private fun notes(notes: List<String>, budget: Int): String {
        if (notes.isEmpty()) {
            return ""
        }
        val unbounded = budget == Int.MAX_VALUE
        val builder = StringBuilder("> [!NOTE]\n")
        var shown = 0
        for (note in notes) {
            val text = if (unbounded || note.length <= MAX_NOTE_CHARS) note else note.take(MAX_NOTE_CHARS) + "…"
            val line = "> - $text\n"
            if (!unbounded && builder.length + line.length + OMITTED_NOTES_LINE_CHARS > budget) {
                break
            }
            builder.append(line)
            shown++
        }
        if (shown < notes.size) {
            builder.append("> - _${notes.size - shown} more notes left out to fit the comment's size limit._\n")
        }
        return builder.append("\n").toString()
    }

    /**
     * The file-name list and the data block, which stays last. [compact] leaves out what only makes a report
     * large — the file names, and the crash excerpts and notes the head shows — and keeps every field
     * `-clientsideapply` reads.
     */
    private fun tail(report: ClientsideReport, compact: Boolean): String {
        val builder = StringBuilder()
        if (compact) {
            builder.appendLine(
                "_The ${report.fileNames.size} published file-names are left out to fit the comment's size limit; " +
                        "the project page lists them._"
            )
        } else {
            builder.appendLine("<details><summary>All ${report.fileNames.size} published file-names</summary>")
            builder.appendLine()
            builder.appendLine("```")
            report.fileNames.forEach { builder.appendLine(it) }
            builder.appendLine("```")
            builder.appendLine("</details>")
        }
        builder.appendLine()

        val data = if (compact) {
            report.copy(fileNames = emptyList(), perTarget = report.perTarget.map { it.copy(bootCrashExcerpt = null, note = null) })
        } else {
            report
        }
        builder.appendLine("<!-- clientside-report-data")
        // `<` and `>` occur in JSON only inside strings, so escaping them keeps the meaning and stops any value from
        // closing this HTML comment (`-->`) or opening a second block.
        builder.appendLine(jsonWriter.writeValueAsString(data).replace("<", "\\u003c").replace(">", "\\u003e"))
        builder.appendLine("-->")
        return builder.toString()
    }

    /**
     * One collapsed console per target that has one, and a line naming each that has none, within [budget]
     * characters. Absent when no target was booted, which is every metadata-only report.
     *
     * The budget is shared by water-filling: the shortest consoles take what they need first and leave the rest
     * to the long ones, so one chatty boot cannot crowd out another.
     */
    private fun bootLogs(
        report: ClientsideReport,
        consoleOf: (GrindTargetVerdict) -> List<String>?,
        evidenceNote: String,
        budget: Int
    ): String {
        if (report.perTarget.none { it.bootResult != null }) {
            return ""
        }
        val consoles = report.perTarget.map { target -> consoleOf(target).orEmpty().map(::printable) }
        val header = "### 🪵 Boot logs\n\nThe end of each server's console — the output its verdict was read from. " +
                "$evidenceNote\n\n"
        val missing = report.perTarget.indices.filter { consoles[it].isEmpty() }
            .joinToString("") { "- ${label(report.perTarget[it])} — no server console was recorded\n" }
        val withConsole = report.perTarget.indices
            .filter { consoles[it].isNotEmpty() }
            .sortedBy { consoleBlock(report.perTarget[it], consoles[it], minOf(MAX_CONSOLE_LINES, consoles[it].size)).length }

        if (budget < header.length + missing.length + 1) {
            return ""
        }
        val blocks = arrayOfNulls<String>(report.perTarget.size)
        var remaining = budget - header.length - missing.length - 1
        withConsole.forEachIndexed { position, index ->
            val block = fittingBlock(report.perTarget[index], consoles[index], remaining / (withConsole.size - position))
            blocks[index] = block
            remaining -= block.length
        }
        return header + blocks.filterNotNull().joinToString("") + missing + "\n"
    }

    /** The largest tail of [lines] whose block fits into [maxChars], or a line saying it was left out. */
    private fun fittingBlock(target: GrindTargetVerdict, lines: List<String>, maxChars: Int): String {
        var shown = minOf(MAX_CONSOLE_LINES, lines.size)
        while (shown > 0) {
            val block = consoleBlock(target, lines, shown)
            if (block.length <= maxChars) {
                return block
            }
            shown--
        }
        return "- ${label(target)} — console left out to fit the comment's size limit\n"
    }

    /** A collapsed block holding the last [shown] of [lines]. */
    private fun consoleBlock(target: GrindTargetVerdict, lines: List<String>, shown: Int): String {
        val tail = lines.takeLast(shown)
        val fence = fenceFor(tail)
        val extent = if (shown == lines.size) "all $shown console lines" else "last $shown of ${lines.size} console lines"
        return buildString {
            appendLine("<details><summary>📜 ${label(target)} — ${bootCell(target)} · $extent</summary>")
            appendLine()
            appendLine("${fence}text")
            tail.forEach(::appendLine)
            appendLine(fence)
            appendLine("</details>")
            appendLine()
        }
    }

    /** A console line as a comment can show it: no terminal escapes or carriage returns, and cut to length. */
    private fun printable(line: String): String {
        val plain = line.replace(TERMINAL_ESCAPE, "").replace("\r", "")
        return if (plain.length > MAX_CONSOLE_LINE_CHARS) plain.take(MAX_CONSOLE_LINE_CHARS) + "…" else plain
    }

    /** A code fence longer than any backtick run in [lines], so none of them can close it. */
    private fun fenceFor(lines: List<String>): String =
        "`".repeat(maxOf(3, (lines.maxOfOrNull { longestBacktickRun(it) } ?: 0) + 1))

    private fun longestBacktickRun(line: String): Int = Regex("`+").findAll(line).maxOfOrNull { it.value.length } ?: 0

    /** A row's name in prose: its loader and the Minecraft version it booted. */
    private fun label(target: GrindTargetVerdict): String = "${target.loader} · Minecraft ${minecraftOf(target)}"

    /**
     * The `Boot` cell for one row: the result, and — when a *different* loader produced it — which one.
     *
     * The other-version crash re-check samples across loaders, so a verdict can be decided by a boot that ran
     * under another loader. That is honest evidence about the mod and misleading evidence about the loader, so
     * the cell has to say `SURVIVED (via NeoForge)` rather than let a row claim a boot it never had. A staging
     * refusal also carries a result (`INCONCLUSIVE`) while no server ran; only a real boot has a classifier rung
     * in [GrindTargetVerdict.decidedBy], so its absence is what says "not booted".
     */
    private fun bootCell(verdict: GrindTargetVerdict): String {
        val result = verdict.bootResult ?: return "—"
        if (verdict.decidedBy == null) {
            return "not booted"
        }
        val via = verdict.bootedLoader?.takeIf { it != verdict.loader } ?: return result.toString()
        return "$result (via $via)"
    }

    /** The Minecraft version a row booted, its version-line when no exact version was chosen, or an em-dash. */
    private fun minecraftOf(verdict: GrindTargetVerdict): String = verdict.minecraftVersion ?: verdict.minecraftLine ?: "—"

    /** Wrap a non-null entry in inline-code, or render an em-dash for a missing one. */
    private fun code(value: String?): String = if (value.isNullOrBlank()) "—" else "`$value`"

    /**
     * Decorate a verdict with an emoji so a maintainer can triage at a glance.
     *
     * CONFIRMED leads because it is what the report is read for; ERROR is marked as the operator's problem
     * it is, so a broken host does not read as a page of suspicious mods.
     */
    private fun badge(verdict: Verdict): String = when (verdict) {
        Verdict.CONFIRMED -> "🟢 **CONFIRMED**"
        Verdict.INCONCLUSIVE -> "⚪ INCONCLUSIVE"
        Verdict.ERROR -> "🛠 ERROR"
        Verdict.CLEAR -> "🔵 CLEAR"
        Verdict.LOCKED -> "🔒 LOCKED"
        Verdict.UNVERIFIABLE -> "🚫 UNVERIFIABLE"
    }
}
