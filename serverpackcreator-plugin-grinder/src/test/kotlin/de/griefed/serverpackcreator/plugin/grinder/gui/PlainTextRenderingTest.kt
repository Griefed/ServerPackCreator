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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import javax.swing.JLabel
import javax.swing.JTable

/**
 * Pins that text this plugin did not write is never rendered as HTML.
 *
 * `JLabel` and `DefaultTableCellRenderer` — the `JTable` default, itself a `JLabel` — install an HTML
 * view whenever the string they are given starts with `<html>`. Everything this plugin displays is
 * attacker-influenced: a verdict's `slug` and `detail` come from mod metadata the grinder scraped off a
 * platform, the `/status` worker and rule-error strings come from the daemon, and that daemon's report
 * server carries **no authentication**. Swing's HTML subset loads remote images, so
 * `<html><img src="http://…">` in a mod name would turn a user's ServerPackCreator window into an
 * outbound request they never made.
 *
 * The grinder's own web report was hardened against exactly this class of input — its `CLAUDE.md`
 * records that it no longer puts mod-supplied text inside a `<script>` block at all — so a Swing surface
 * that interprets the same strings reintroduces a defect this project already paid to remove.
 *
 * Swing sets the `"html"` client property to the parsed view when it decides a string is HTML, and
 * leaves it absent otherwise. That property is therefore the observable: it distinguishes *parsed* from
 * *displayed literally* without needing a screen, so these run headless.
 */
internal class PlainTextRenderingTest {

    /** What a hostile mod name or `/status` string would look like. */
    private val hostile = """<html><b>totally fine</b><img src="http://example.invalid/pixel.png"></html>"""

    /** A label built for grinder text refuses to parse it, however HTML-shaped it is. */
    @Test
    fun aLabelForGrinderTextNeverParsesHtml() {
        val label = PlainTextRendering.label(hostile)

        Assertions.assertNull(
            label.getClientProperty("html"),
            "Swing parsed the string as HTML; an <img> in it would be fetched from the network"
        )
        Assertions.assertEquals(hostile, label.text, "the text itself must survive, shown literally")
    }

    /** And it keeps working as an ordinary label for text nobody is attacking with. */
    @Test
    fun aLabelStillShowsOrdinaryText() {
        Assertions.assertEquals("Modrinth — offset 12", PlainTextRendering.label("Modrinth — offset 12").text)
    }

    /**
     * The table is the bigger surface: every verdict row carries four grinder-supplied strings, and the
     * default renderer would parse each one.
     */
    @Test
    fun aTableCellNeverParsesHtml() {
        val table = JTable(arrayOf(arrayOf<Any>(hostile)), arrayOf<Any>("Detail"))
        val renderer = PlainTextRendering.tableCellRenderer()

        val component = renderer.getTableCellRendererComponent(table, hostile, false, false, 0, 0)

        Assertions.assertNull(
            (component as JLabel).getClientProperty("html"),
            "the table parsed a grinder-supplied cell as HTML"
        )
        Assertions.assertEquals(hostile, component.text)
    }

    /**
     * The guard is only meaningful if Swing would otherwise have parsed it — otherwise it asserts a
     * property that is absent for an unrelated reason. This is the control: the same string through an
     * ordinary `JLabel` **is** parsed.
     */
    @Test
    fun swingWouldOtherwiseHaveParsedIt() {
        Assertions.assertNotNull(
            JLabel(hostile).getClientProperty("html"),
            "if this is null, the guards above prove nothing and the observable has changed"
        )
    }
}
