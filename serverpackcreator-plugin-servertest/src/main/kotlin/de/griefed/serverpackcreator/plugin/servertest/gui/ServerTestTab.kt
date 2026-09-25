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

import com.electronwill.nightconfig.core.CommentedConfig
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.plugins.swinggui.ExtensionTab
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import de.griefed.serverpackcreator.plugin.servertest.core.LaunchablePack
import de.griefed.serverpackcreator.plugin.servertest.core.PackVariables
import de.griefed.serverpackcreator.plugin.servertest.core.GenerationNotifier
import de.griefed.serverpackcreator.plugin.servertest.core.LaunchOutcome
import de.griefed.serverpackcreator.plugin.servertest.core.PortAllocator
import de.griefed.serverpackcreator.plugin.servertest.core.ServerLauncher
import de.griefed.serverpackcreator.plugin.servertest.core.ServerPackCatalog
import de.griefed.serverpackcreator.plugin.servertest.core.ServerTestSettings
import de.griefed.serverpackcreator.plugin.servertest.core.SessionRegistry
import de.griefed.serverpackcreator.plugin.servertest.core.SessionState
import de.griefed.serverpackcreator.plugin.servertest.core.Subscription
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.Insets
import java.io.File
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTabbedPane
import javax.swing.SwingUtilities

/**
 * The plugin's single tab: the pack list, plus one console per running server.
 *
 * A `TabExtension` yields exactly one tab, so these are a nested [JTabbedPane] — the list is always index 0
 * and starting a pack appends a console beside it.
 *
 * **This constructor must not throw.** `ApiPlugins.addTabExtensionTabs` calls `getTab` *outside* the
 * try-block guarding the rest of tab registration, so an exception here escapes into GUI assembly instead of
 * being logged as a plugin error. Everything that can fail is reported in the pane.
 *
 * Threading: no coroutines. A plugin cannot reach ServerPackCreator's lifecycle-cancelled
 * `ComponentCoroutineScope`, and an unowned `GlobalScope` is the anti-pattern this project spent a sprint
 * removing from its own GUI. A session's console arrives on its reader thread and every pane marshals to the
 * event dispatch thread itself.
 *
 * @author Griefed
 */
class ServerTestTab(
    versionMeta: VersionMeta,
    apiProperties: ApiProperties,
    utilities: Utilities,
    pluginConfig: Optional<CommentedConfig>,
    configFile: Optional<File>
) : ExtensionTab(versionMeta, apiProperties, utilities, pluginConfig, configFile) {

    private val settings = ServerTestSettings(pluginConfig.orElseGet { CommentedConfig.inMemory() })
    private val catalog = ServerPackCatalog(utilities.jsonUtilities.objectMapper)
    private val allocator = PortAllocator(settings.portRangeStart, settings.portRangeEnd)
    private val registry = SessionRegistry()
    private val launcher = ServerLauncher(allocator, registry)

    /** The pack list plus one console per running server; the list is always the first tab. */
    private val panes = JTabbedPane()

    private val packList = PackListPane(onStart = ::launch, onRefresh = ::refreshPackList)

    /**
     * Consoles by pack directory, so a line arriving from a session can find the pane it belongs to.
     *
     * Concurrent, and that is not caution: the entry is written here on the event dispatch thread and read
     * on **each session's own reader thread**, which is where `ServerSession` documents its callbacks
     * arrive. A plain `HashMap` read while another thread is resizing it is undefined behaviour.
     */
    private val consoles = ConcurrentHashMap<File, ConsolePane>()

    /**
     * The last state reported per pack, kept after the session ends so the list can still say how a run
     * finished. Whether a pack is *running* is asked of the registry, never inferred from this.
     */
    private val lastStates = mutableMapOf<File, SessionState>()

    /**
     * Live while the tab is in the window, so a pack generated in the Configs tab shows up here by itself.
     *
     * Held so it can be cancelled: [GenerationNotifier] keeps whatever it is given, and a tab that has left
     * the window would otherwise be kept alive refreshing a list nobody can see.
     */
    private var generationSubscription: Subscription? = null

    init {
        layout = BorderLayout()
        panes.addTab("Server Packs", packList)
        add(panes, BorderLayout.CENTER)

        registry.installShutdownHook()
        refreshPackList()
    }

    /**
     * Start listening for generated server packs when the tab joins the window.
     *
     * Paired with [removeNotify] rather than subscribed once in the constructor, which is the same shape
     * the grinder plugin's dashboard timer uses: a subscription with no owner outlives whatever it was
     * meant to serve.
     */
    override fun addNotify() {
        super.addNotify()
        if (generationSubscription == null) {
            generationSubscription = GenerationNotifier.subscribe { onPackGenerated() }
        }
    }

    /** Stop listening when the tab leaves the window. Nothing else would. */
    override fun removeNotify() {
        generationSubscription?.cancel()
        generationSubscription = null
        super.removeNotify()
    }

    /**
     * A server pack was just generated somewhere in ServerPackCreator, so re-read the directory.
     *
     * The generated pack's path is deliberately ignored: re-reading gives the new row the same
     * `manifest.json` every other row came from, where trusting the path would make one row's facts arrive
     * by a different route than the rest. Marshalled because generation runs on its own dispatcher, never
     * on the event dispatch thread.
     */
    private fun onPackGenerated() {
        SwingUtilities.invokeLater { refreshPackList() }
    }

    /** Re-read the server-packs directory and redraw the list with each pack's current state. */
    private fun refreshPackList() {
        val packs = catalog.packsIn(apiProperties.serverPacksDirectory)
        packList.show(packs.map { PackRow(it, lastStates[it.directory], registry.isRunning(it.directory)) })
    }

    /**
     * Launch [pack] and open a console for it, or say why not.
     *
     * The sequence this used to contain — take a port, borrow `server.properties`, register before
     * starting, give everything back exactly once — now lives in [ServerLauncher], where it is reachable
     * headless. What is left here is the part that is genuinely a view's job: put a console on screen,
     * and start the session only once there is somewhere for its output to go.
     */
    private fun launch(pack: LaunchablePack) {
        when (val outcome = launcher.launch(
            pack = pack,
            onLine = { line -> consoles[pack.directory]?.appendLine(line) },
            onState = { state -> onSessionState(pack, state) },
            onClosed = { SwingUtilities.invokeLater { refreshPackList() } }
        )) {
            is LaunchOutcome.Refused -> warn(outcome.reason)

            is LaunchOutcome.Started -> {
                val console = ConsolePane(
                    session = outcome.session,
                    port = outcome.port,
                    variables = PackVariables.read(pack.directory),
                    scrollback = settings.consoleScrollback
                )
                consoles[pack.directory] = console
                lastStates[pack.directory] = SessionState.Starting

                addConsoleTab(pack, console)
                refreshPackList()

                // Started last, deliberately: the session streams to `consoles[pack.directory]`, so a
                // process spawned before that entry exists would drop its first lines on the floor --
                // which, for a pack that fails immediately, is the only output there would ever be.
                outcome.session.start()
            }
        }
    }

    /**
     * Add [console] as a sub-tab for [pack] and select it.
     *
     * The title is set as a **component** rather than as a string. `BasicTabbedPaneUI` keeps HTML views for
     * tab titles, so a pack name handed over as a title is a third renderer reached by the same
     * user-controlled text the table and the dialogs already had to be proofed against. `setTabComponentAt`
     * is how `MainPanel` labels ServerPackCreator's own tabs, so this is the house idiom.
     */
    private fun addConsoleTab(pack: LaunchablePack, console: ConsolePane) {
        panes.addTab(pack.name, console)
        panes.setTabComponentAt(panes.tabCount - 1, consoleTabLabel(pack, console))
        panes.selectedComponent = console
    }

    /**
     * The tab's own label: the pack's name, rendered literally, with a close control beside it.
     *
     * Without a way to close one, a console is kept for the life of the application — its `JTextArea`
     * holding up to `consoleScrollback` lines — for every pack ever launched, and the maps behind it grow
     * with it. Closing a *running* server's tab is refused rather than silently killing it: the tab is the
     * only place that server can be stopped from, so taking it away would strand the process.
     */
    private fun consoleTabLabel(pack: LaunchablePack, console: ConsolePane): JPanel =
        JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
            isOpaque = false
            add(PlainTextRendering.label(pack.name))
            add(
                JButton("\u00d7").apply {
                    toolTipText = "Close this console"
                    isFocusable = false
                    margin = Insets(0, 4, 0, 0)
                    isBorderPainted = false
                    isContentAreaFilled = false
                    addActionListener { closeConsole(pack, console) }
                }
            )
        }

    /**
     * Drop [pack]'s console, unless its server is still running.
     *
     * The state the list shows is dropped with it: a pack whose console has been closed has no run to
     * report any more, so it goes back to reading "Ready to launch" rather than keeping a stale exit code
     * next to a tab that is no longer there.
     */
    private fun closeConsole(pack: LaunchablePack, console: ConsolePane) {
        if (registry.isRunning(pack.directory)) {
            warn("${pack.name} is still running. Stop it first — this tab is the only place you can.")
            return
        }
        panes.remove(console)
        consoles.remove(pack.directory)
        lastStates.remove(pack.directory)
        refreshPackList()
    }

    /** Push a state change to the pack's console and to the list, on the event dispatch thread. */
    private fun onSessionState(pack: LaunchablePack, state: SessionState) {
        consoles[pack.directory]?.showState(state)
        SwingUtilities.invokeLater {
            lastStates[pack.directory] = state
            refreshPackList()
        }
    }

    /**
     * Say something the user needs to act on.
     *
     * Goes through [Dialogs] rather than `JOptionPane` directly, because every message here names a server
     * pack — whose directory name the user chose — and a raw string reaches a renderer that would parse it
     * as markup.
     */
    private fun warn(message: String) {
        log.warn(message)
        Dialogs.warn(this, message)
    }
}
