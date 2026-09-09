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

import com.electronwill.nightconfig.core.CommentedConfig
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.plugins.swinggui.ExtensionTab
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import de.griefed.serverpackcreator.plugin.grinder.core.FetchResult
import de.griefed.serverpackcreator.plugin.grinder.core.GrinderClient
import de.griefed.serverpackcreator.plugin.grinder.core.GrinderVerdict
import de.griefed.serverpackcreator.plugin.grinder.core.SelectionAttribution
import de.griefed.serverpackcreator.plugin.grinder.core.SelectionPane
import de.griefed.serverpackcreator.plugin.grinder.core.SelectionStore
import java.awt.BorderLayout
import java.io.File
import java.util.Optional
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTabbedPane
import javax.swing.SwingWorker
import javax.swing.Timer

/**
 * The plugin's single tab in ServerPackCreator's window, holding the four panes the feature is made of:
 * Confirmed, Other Verdicts, Dashboard and Settings.
 *
 * A `TabExtension` yields exactly one tab, so the four are a nested [JTabbedPane]. They share one
 * [SelectionStore] over the `CommentedConfig` ServerPackCreator owns — the same instance
 * `GrinderPreGenExtension` reads, which is what lets a tick apply to a generation started moments later
 * without a save, while [saveConfiguration] is what carries it across a restart.
 *
 * All network work happens on a [SwingWorker] and is applied on the event dispatch thread. No coroutines:
 * a plugin cannot reach ServerPackCreator's lifecycle-cancelled scopes, and an unowned `GlobalScope` is
 * the anti-pattern this project spent a sprint removing from its own GUI.
 *
 * @author Griefed
 */
class GrinderTab(
    versionMeta: VersionMeta,
    apiProperties: ApiProperties,
    utilities: Utilities,
    pluginConfig: Optional<CommentedConfig>,
    configFile: Optional<File>
) : ExtensionTab(versionMeta, apiProperties, utilities, pluginConfig, configFile) {

    private val settings = pluginConfig.map { SelectionStore(it) }.orElseGet { SelectionStore(CommentedConfig.inMemory()) }
    private val client = GrinderClient(utilities.jsonUtilities.objectMapper)

    private val confirmedPane = VerdictListPane(onSelectionChanged = ::onSelectionChanged)
    private val otherPane = VerdictListPane(warning = RISK_WARNING, onSelectionChanged = ::onSelectionChanged)
    private val dashboardPane = DashboardPane()
    private val status = JLabel(" ")

    /**
     * Polls `/status` for the Dashboard. A Swing [Timer] fires on the event dispatch thread, so the tick
     * only *starts* the worker that does the request — the request itself never runs on the EDT.
     */
    private val dashboardTimer = Timer(settings.refreshIntervalSeconds * 1_000) { refreshDashboard() }

    init {
        layout = BorderLayout()
        add(header(), BorderLayout.NORTH)
        add(tabs(), BorderLayout.CENTER)

        showStoredSelection()
        if (settings.resolvedUrl != null) {
            refreshVerdicts()
            dashboardTimer.start()
        } else {
            status.text = "No grinder configured — set one in the Settings tab."
        }
    }

    /**
     * Stop polling when the tab leaves the window. Nothing else would: a Swing [Timer] holds a reference
     * to its listener and keeps firing for the life of the JVM, so an unattended ServerPackCreator would
     * ask its grinder for `/status` every few seconds forever. Not the `GlobalScope` anti-pattern this
     * project removed from `-app`, but the same shape — a repeating task with no owner.
     */
    override fun removeNotify() {
        dashboardTimer.stop()
        super.removeNotify()
    }

    /** Resume polling if the tab is added back, so the Dashboard is live whenever it can be seen. */
    override fun addNotify() {
        super.addNotify()
        if (settings.resolvedUrl != null && !dashboardTimer.isRunning) {
            dashboardTimer.start()
        }
    }

    /** The refresh control and the one status line the whole tab reports through. */
    private fun header() = JPanel(BorderLayout()).apply {
        add(JButton("Refresh from grinder").apply { addActionListener { refreshVerdicts() } }, BorderLayout.WEST)
        add(status, BorderLayout.CENTER)
    }

    /** The four panes, in the order the feature reads: proven findings first, configuration last. */
    private fun tabs() = JTabbedPane().apply {
        addTab("Confirmed", confirmedPane)
        setToolTipTextAt(0, "Mods the grinder proved clientside-only by crashing a server with them installed.")
        addTab("Other Verdicts", otherPane)
        setToolTipTextAt(1, "Everything the grinder did NOT prove. Excluding these may break your server.")
        addTab("Dashboard", dashboardPane)
        setToolTipTextAt(2, "What the grinder is doing right now.")
        addTab("Settings", settingsPane())
        setToolTipTextAt(3, "Which grinder to read, and how often to poll it.")
    }

    /** The settings pane, wired to persist through this tab and to reload everything once it has. */
    private fun settingsPane() = SettingsPane(
        initialUrl = settings.grinderUrl,
        initialInterval = settings.refreshIntervalSeconds,
        onSave = { url, interval ->
            settings.grinderUrl = url
            settings.refreshIntervalSeconds = interval
            saveConfiguration()
            dashboardTimer.delay = settings.refreshIntervalSeconds * 1_000
            if (settings.resolvedUrl != null) {
                refreshVerdicts()
                dashboardTimer.restart()
            } else {
                dashboardTimer.stop()
                status.text = "No grinder configured — set one in the Settings tab."
            }
        },
        onTest = { url, report ->
            // The typed address, not the stored one: the point of the button is to check before saving.
            inBackground({ client.fetchStatus(url) }) { result ->
                when (result) {
                    is FetchResult.Ok -> report(true, "Reached the grinder: ${result.value.path("verdicts")} verdicts.")
                    is FetchResult.Failed -> report(false, result.reason)
                }
            }
        }
    )

    /** Fetch every verdict and hand the two lists their rows. */
    private fun refreshVerdicts() {
        val target = settings.resolvedUrl
        if (target == null) {
            status.text = "No grinder configured — set one in the Settings tab."
            return
        }
        status.text = "Reading $target…"
        inBackground({ client.fetchVerdicts(target) }) { result ->
            when (result) {
                is FetchResult.Ok -> distribute(result.value)
                is FetchResult.Failed -> status.text = result.reason
            }
        }
    }

    /** Poll `/status` once and hand it to the Dashboard. */
    private fun refreshDashboard() {
        val target = settings.resolvedUrl ?: return
        inBackground({ client.fetchStatus(target) }) { result ->
            when (result) {
                is FetchResult.Ok -> dashboardPane.show(result.value)
                is FetchResult.Failed -> dashboardPane.showFailure(result.reason)
            }
        }
    }

    /**
     * Split [verdicts] across the two panes: proven findings on the left, everything else behind the
     * warning. The split is this interface's statement about risk — generation excludes whatever is
     * ticked in either.
     */
    private fun distribute(verdicts: List<GrinderVerdict>) {
        val (confirmed, other) = verdicts.partition { it.isConfirmed }
        confirmedPane.setVerdicts(confirmed)
        otherPane.setVerdicts(other)
        showStoredSelection()
        status.text = "${confirmed.size} confirmed, ${other.size} other verdicts."
    }

    /** Show what is saved as ticked in both panes, without either treating it as a user edit. */
    private fun showStoredSelection() {
        val selected = settings.allSelected()
        confirmedPane.showSelection(selected)
        otherPane.showSelection(selected)
    }

    /**
     * Persist a user's tick.
     *
     * The panes share one selection set, so which key an entry is written under has to be worked out
     * rather than known. [SelectionAttribution] owns that rule and is pinned separately — it is pure, and
     * it was wrong for as long as it lived here untested.
     */
    private fun onSelectionChanged(selected: Set<String>) {
        val attributed = SelectionAttribution.split(
            selected = selected,
            shownInConfirmed = confirmedPane.shownEntries(),
            shownInOther = otherPane.shownEntries(),
            storedConfirmed = settings.selected(SelectionPane.CONFIRMED),
            storedOther = settings.selected(SelectionPane.OTHER)
        )
        settings.setSelected(SelectionPane.CONFIRMED, attributed.confirmed)
        settings.setSelected(SelectionPane.OTHER, attributed.other)
        saveConfiguration()

        // Both panes render one set, so a tick in either has to be mirrored into the other's model.
        confirmedPane.showSelection(selected)
        otherPane.showSelection(selected)
    }

    /**
     * Run [work] off the event dispatch thread and hand its result to [onDone] back on it.
     *
     * A plain [SwingWorker] rather than a coroutine: a plugin has no access to ServerPackCreator's
     * lifecycle-cancelled scopes, and `GlobalScope` is exactly the anti-pattern this project removed
     * from its own GUI. `get()` cannot throw here — [GrinderClient] answers with a result rather than an
     * exception — but it is guarded anyway, since an interrupt during shutdown would otherwise surface
     * as a dialog nobody can act on.
     */
    private fun <T> inBackground(work: () -> T, onDone: (T) -> Unit) {
        object : SwingWorker<T, Unit>() {
            override fun doInBackground(): T = work()

            override fun done() {
                runCatching { get() }
                    .onSuccess(onDone)
                    .onFailure { log.error("Grinder request failed unexpectedly.", it) }
            }
        }.execute()
    }

    private companion object {
        /** The warning above the unproven list. Its wording is the point — it is what the user is agreeing to. */
        const val RISK_WARNING =
            "USE AT YOUR OWN RISK! Entries in these lists may very well be server mods. " +
                    "Excluding them might break compatibility between your modpack client and a server."
    }
}

