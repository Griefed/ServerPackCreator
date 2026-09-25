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
package de.griefed.serverpackcreator.plugin.servertest.gui

import de.griefed.serverpackcreator.plugin.servertest.core.LaunchablePack
import de.griefed.serverpackcreator.plugin.servertest.core.StartScriptSelection
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.awt.Container
import java.io.File
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JTabbedPane
import javax.swing.JTable

/**
 * Pins that no text originating outside this plugin is ever rendered as markup.
 *
 * Swing installs an HTML view for any string beginning with `<html>`, and its HTML subset **loads remote
 * images** — so one `<html><img src="http://…">` in the wrong place makes a user's ServerPackCreator issue
 * a request nobody asked for. The input here is a server-pack **directory name**, which the user creates
 * and which an imported modpack can influence, plus the version strings in a hand-editable `manifest.json`.
 *
 * Written after an audit found the module's existing mitigation **inert**: the table registered an
 * HTML-disabled renderer under `String::class.java` while `AbstractTableModel` answered `Object` for every
 * column, so `JTable` never consulted it. That is exactly why these guards assert on what Swing
 * *resolved* — the client property on the component it actually rendered — rather than on the
 * registration having been made. A guard that checks the call was issued would have stayed green through
 * the entire defect.
 */
internal class HtmlProofingTest {

    /** A pack whose every user-controlled field is an injection attempt. */
    private val hostilePack = LaunchablePack(
        directory = File("/packs/evil"),
        name = HOSTILE,
        minecraftVersion = HOSTILE,
        modloader = HOSTILE,
        modloaderVersion = HOSTILE,
        selection = StartScriptSelection.Available(File("start.sh"), listOf("bash", "start.sh"))
    )

    /** Whether Swing decided [component] is markup, which is the only thing that matters here. */
    private fun rendersAsHtml(component: JComponent): Boolean = component.getClientProperty("html") != null

    /** Every `JLabel` anywhere under [container], however deeply the look and feel nested it. */
    private fun labelsUnder(container: Container): List<JLabel> = buildList {
        for (child in container.components) {
            if (child is JLabel) add(child)
            if (child is Container) addAll(labelsUnder(child))
        }
    }

    /**
     * The model must declare its columns as `String`, or the renderer registered for `String` is dead code.
     *
     * `AbstractTableModel.getColumnClass` answers `Object` unless overridden, and `JTable` resolves its
     * renderer by that answer. This is the assertion that would have caught the original defect.
     */
    @Test
    fun theModelDeclaresItsColumnsAsStrings() {
        val model = PackTableModel()
        for (column in PackTableModel.COLUMNS.indices) {
            Assertions.assertEquals(
                String::class.java,
                model.getColumnClass(column),
                "Column $column must be declared String, or JTable never reaches the HTML-disabled renderer."
            )
        }
    }

    /** What the table actually renders for a hostile pack name must be literal text. */
    @Test
    fun theTableRendersAHostilePackNameLiterally() {
        val pane = PackListPane({}, {}).apply { show(listOf(PackRow(hostilePack, null, running = false))) }
        val table = labelsOrTable(pane)

        for (column in PackTableModel.COLUMNS.indices) {
            val renderer = table.getCellRenderer(0, column)
            val rendered = table.prepareRenderer(renderer, 0, column) as JComponent
            Assertions.assertFalse(
                rendersAsHtml(rendered),
                "Column $column rendered a pack's own name as HTML. Swing's HTML subset fetches remote images."
            )
        }
    }

    /**
     * A dialog message must be handed to `JOptionPane` as a component, not as a string.
     *
     * The contrast is the evidence: the same hostile text is markup as a `String` and literal as a
     * [PlainTextRendering.label], so a green result here cannot be an accident of the text.
     */
    @Test
    fun aDialogMessageIsLiteralOnlyWhenItIsAComponent() {
        val asString = labelsUnder(JOptionPane(HOSTILE, JOptionPane.WARNING_MESSAGE)).count { rendersAsHtml(it) }
        val asLabel = labelsUnder(JOptionPane(PlainTextRendering.label(HOSTILE), JOptionPane.WARNING_MESSAGE))
            .count { rendersAsHtml(it) }

        Assertions.assertEquals(1, asString, "Sanity check: a bare String really is parsed as markup here.")
        Assertions.assertEquals(0, asLabel, "A PlainTextRendering label must stay literal inside a dialog.")
    }

    /**
     * A tab titled with a pack's own name must carry an HTML-disabled component rather than a bare title.
     *
     * `BasicTabbedPaneUI` keeps its own HTML views for titles, so a pack name passed as the title string is
     * a third renderer reached by the same input. `setTabComponentAt` is how `MainPanel` labels its own
     * tabs, so this is the house idiom rather than a workaround.
     */
    @Test
    fun aTabTitledWithAPackNameCarriesALiteralComponent() {
        val panes = JTabbedPane()
        panes.addTab(hostilePack.name, javax.swing.JPanel())
        panes.setTabComponentAt(0, PlainTextRendering.label(hostilePack.name))

        val component = panes.getTabComponentAt(0)
        Assertions.assertNotNull(component, "A tab showing a pack's name must carry its own component.")
        Assertions.assertFalse(rendersAsHtml(component as JComponent), "The tab label must be literal text.")
    }

    /**
     * No view raises a dialog for itself, so the proofing in [Dialogs] cannot be bypassed a line at a time.
     *
     * Structural rather than behavioural on purpose. The two guards above pin the *contract* — that a
     * component message stays literal where a string does not — but neither can reach the call site, which
     * lives on a tab that needs a live `ApiWrapper` to construct. What can be checked cheaply is that there
     * is exactly one call site, and this is the same shape `KDocAttachmentTest` uses to pin a defect by its
     * own structure rather than through a symptom.
     */
    @Test
    fun noViewRaisesADialogWithoutGoingThroughDialogs() {
        val guiSources = File("src/main/kotlin/de/griefed/serverpackcreator/plugin/servertest/gui")
            .listFiles { file -> file.extension == "kt" }
            ?.toList()
            .orEmpty()
        Assertions.assertTrue(guiSources.isNotEmpty(), "Expected to find the plugin's gui sources on disk.")

        val offenders = guiSources
            .filter { it.name != "Dialogs.kt" }
            .filter { withoutComments(it.readText()).contains("JOptionPane") }
            .map { it.name }

        Assertions.assertEquals(
            emptyList<String>(),
            offenders,
            "These views reach for JOptionPane directly. A String message there is parsed as markup; " +
                    "route them through Dialogs, which wraps the text in an HTML-disabled component."
        )
    }

    /**
     * [source] with its comments removed, so the guard above reads code rather than prose.
     *
     * Needed because the first version flagged `ServerTestTab` for the sentence in a doc comment
     * explaining why it does *not* call `JOptionPane` — a guard that fires on an explanation of its own
     * rule is worse than none, because the obvious way to silence it is to delete the explanation.
     */
    private fun withoutComments(source: String): String = source
        .replace(Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL), "")
        .replace(Regex("""//.*"""), "")

    /** Dig the table out of the pane, which wraps it in a scroll pane. */
    private fun labelsOrTable(pane: PackListPane): JTable =
        requireNotNull(findTable(pane)) { "PackListPane must contain a JTable." }

    private fun findTable(container: Container): JTable? {
        for (child in container.components) {
            if (child is JTable) return child
            if (child is Container) findTable(child)?.let { return it }
        }
        return null
    }

    private companion object {
        /** The shape every one of these guards is defending against. */
        const val HOSTILE = "<html><img src=\"http://example.invalid/pixel.png\">"
    }
}
