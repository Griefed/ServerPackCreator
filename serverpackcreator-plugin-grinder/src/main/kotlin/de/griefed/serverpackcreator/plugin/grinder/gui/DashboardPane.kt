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
package de.griefed.serverpackcreator.plugin.grinder.gui

import com.fasterxml.jackson.databind.JsonNode
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Font
import java.awt.GridLayout
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.SwingConstants

/**
 * A native rendering of the grinder's `/dashboard`, built from the `/status` document that page polls
 * for itself.
 *
 * It reads `/status` rather than displaying `/dashboard` because that page is an HTML shell whose
 * numbers arrive from JavaScript, and Swing's HTML renderer executes none. The fields shown here are the
 * ones the daemon's `StatusDashboardRenderer.READ_FIELDS` names, so the two views agree on what a
 * status document contains.
 *
 * Every field is read defensively. This pane is pointed at a daemon the user upgrades independently, so
 * a missing or reshaped field renders as an em dash rather than emptying the tab.
 *
 * @author Griefed
 */
class DashboardPane : JPanel(BorderLayout(0, 8)) {

    private val statusLine = JLabel("Not connected.")
    private val cards = JPanel(GridLayout(0, 4, 8, 8))
    private val workers = JPanel().apply { layout = BoxLayout(this, BoxLayout.Y_AXIS) }
    private val crawl = JPanel().apply { layout = BoxLayout(this, BoxLayout.Y_AXIS) }
    private val rulesAndCache = JPanel().apply { layout = BoxLayout(this, BoxLayout.Y_AXIS) }

    init {
        border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        add(statusLine, BorderLayout.NORTH)

        val body = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(cards)
            add(section("Workers", workers))
            add(section("Catalogue crawl", crawl))
            add(section("Rules and cache", rulesAndCache))
        }
        add(JScrollPane(body), BorderLayout.CENTER)
    }

    /** Render [status], the parsed `/status` document, replacing whatever was shown. */
    fun show(status: JsonNode) {
        statusLine.text = "Connected."
        statusLine.foreground = null

        val activity = status.path("activity")
        cards.removeAll()
        listOf(
            "Verdicts" to text(status.path("verdicts")),
            "Re-grind queue" to text(status.path("requeued")),
            "Pass" to "${text(activity.path("pass"))} (${text(activity.path("passCandidates"))} candidates)",
            "Pass running" to duration(activity.path("passRunningSeconds")),
            "Verified" to text(activity.path("verified")),
            "Failed" to text(activity.path("failed")),
            "Skipped (fresh)" to text(activity.path("skippedFresh")),
            "Uptime" to duration(activity.path("uptimeSeconds")),
            "Started" to text(activity.path("startedAt"))
        ).forEach { (label, value) -> cards.add(card(label, value)) }

        workers.removeAll()
        val workerRows = activity.path("workers")
        if (workerRows.isArray && !workerRows.isEmpty) {
            // The daemon's WorkerSnapshot: worker, platform, slug, projectUrl, busySeconds. Rendered in
            // the same order the daemon's own dashboard table uses, so the two read alike.
            workerRows.forEach { worker ->
                workers.add(
                    JLabel(
                        "${text(worker.path("worker"))} — ${text(worker.path("platform"))}/" +
                                "${text(worker.path("slug"))}, busy ${duration(worker.path("busySeconds"))}"
                    )
                )
            }
        } else {
            workers.add(JLabel("No worker is currently busy."))
        }

        crawl.removeAll()
        val crawlNode = status.path("crawl")
        if (crawlNode.isObject && !crawlNode.isEmpty) {
            crawlNode.properties().forEach { (platform, cursor) ->
                crawl.add(
                    JLabel(
                        "$platform — offset ${text(cursor.path("offset"))}, " +
                                "sweeps ${text(cursor.path("sweeps"))}, partition ${text(cursor.path("partition"))}"
                    )
                )
            }
        } else {
            crawl.add(JLabel("No crawl cursors reported."))
        }

        rulesAndCache.removeAll()
        val rules = status.path("bootRules")
        rulesAndCache.add(
            JLabel(
                "Boot rules: ${text(rules.path("ruleCount"))} from ${text(rules.path("source"))}, " +
                        "undecided → ${text(rules.path("undecidedVerdict"))}"
            )
        )
        // Rule errors are the reason /status reports them at all: the daemon keeps its last good rule
        // file when a save breaks it, so a typo would otherwise disable an operator's rules in silence.
        val errors = rules.path("errors")
        if (errors.isArray && !errors.isEmpty) {
            errors.forEach { error ->
                rulesAndCache.add(JLabel(error.asText()).apply { foreground = ERROR_COLOUR })
            }
        }
        val cache = status.path("loaderCache")
        rulesAndCache.add(
            JLabel("Loader cache: ${text(cache.path("installedTuples"))} tuples at ${text(cache.path("path"))}")
        )

        revalidate()
        repaint()
    }

    /** Report [reason] in place of a reading, leaving the last numbers visible rather than blanking them. */
    fun showFailure(reason: String) {
        statusLine.text = reason
        statusLine.foreground = ERROR_COLOUR
    }

    /** A labelled block with a heading, so the sections read as sections rather than as a run of labels. */
    private fun section(title: String, content: JPanel) = JPanel(BorderLayout()).apply {
        border = BorderFactory.createTitledBorder(title)
        add(content, BorderLayout.CENTER)
    }

    /** One statistic: its name above its value, the same pairing the daemon's own dashboard uses. */
    private fun card(label: String, value: String) = JPanel(BorderLayout()).apply {
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(6, 8, 6, 8)
        )
        add(JLabel(label, SwingConstants.CENTER), BorderLayout.NORTH)
        add(
            JLabel(value, SwingConstants.CENTER).apply { font = font.deriveFont(Font.BOLD, font.size + 4f) },
            BorderLayout.CENTER
        )
    }

    /** A value fit to display: an em dash for anything absent, so a gap reads as a gap. */
    private fun text(node: JsonNode): String =
        if (node.isMissingNode || node.isNull) "—" else node.asText().ifBlank { "—" }

    /**
     * A span of seconds as something a human reads — "4h 12m" rather than "15134". Same rendering the
     * daemon's dashboard does in JavaScript, for the same reason.
     */
    private fun duration(node: JsonNode): String {
        if (!node.isNumber) {
            return "—"
        }
        val total = node.asLong()
        val days = total / 86_400
        val hours = (total % 86_400) / 3_600
        val minutes = (total % 3_600) / 60
        val seconds = total % 60
        return when {
            days > 0 -> "${days}d ${hours}h ${minutes}m"
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }

    private companion object {
        /** A red that stays legible on both the light and the dark look-and-feels ServerPackCreator ships. */
        val ERROR_COLOUR: Color = Color(0xC0, 0x39, 0x2B)
    }
}
